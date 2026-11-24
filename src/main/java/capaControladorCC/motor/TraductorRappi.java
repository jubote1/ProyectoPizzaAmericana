package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaModeloCC.motor.MotorSolicitud;

/**
 * Traductor de Pedidos de RAPPI hacia el Nuevo Motor de Pedidos (pos_central).
 *
 * Convierte el arreglo de items, subitems, cargos y descuentos enviado por el webhook
 * de Rappi a la estructura unificada MotorSolicitud:
 *   1. Mapea productos y especialidades usando homologacion_producto_rappi.
 *   2. Traduce a IDs unificados del POS mediante pos_central.equivalencia_producto.
 *   3. Soporta pizzas con especialidad simple, mitades y combos.
 *   4. Identifica bebidas, acompañantes y domicilios de plataforma.
 *   5. Aplica descuentos asumidos por la tienda de forma transparente.
 */
public class TraductorRappi {

	public static final int PRODUCTO_DOMICILIO_POS = 412; // Valor del domicilio plataforma

	private static final Logger logger = Logger.getLogger("log_file");

	private final CatalogoPos catalogo;

	// Claves de homologacion en memoria
	private final Map<String, HomologacionRappiInfo> homologacionRappi = new HashMap<>();
	private final Map<String, Integer> eqProducto = new HashMap<>();
	private final Map<String, Integer> eqEspecialidad = new HashMap<>();
	private final Map<Integer, Integer> espDirecta = new HashMap<>();
	private final Map<String, Integer> eqExcepcion = new HashMap<>();
	private final Map<Integer, Integer> eqSabor = new HashMap<>();
	private final Map<String, Integer> skusEcommerce = new HashMap<>();
	private final Map<String, Integer> saborLiquidoCentral = new HashMap<>();

	public static class HomologacionRappiInfo {
		public final int idProducto;
		public final int idEspecialidad;
		public final int idExcepcion;
		public final double precio;

		public HomologacionRappiInfo(int idProducto, int idEspecialidad, int idExcepcion, double precio) {
			this.idProducto = idProducto;
			this.idEspecialidad = idEspecialidad;
			this.idExcepcion = idExcepcion;
			this.precio = precio;
		}
	}

	public TraductorRappi(Connection con, CatalogoPos catalogo) throws Exception {
		this.catalogo = catalogo;
		cargarTablas(con);
	}

	private void cargarTablas(Connection con) throws Exception {
		// 1. Homologacion Rappi
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select nombre, idproducto, idespecialidad, idexcepcion, precio from pizzaamericana.homologacion_producto_rappi")) {
			while (rs.next()) {
				String nombre = rs.getString(1);
				if (nombre != null) {
					HomologacionRappiInfo info = new HomologacionRappiInfo(
							rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5));
					homologacionRappi.put(normalizar(nombre), info);
				}
			}
		}

		// 2. Equivalencia producto hacia pos_central
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select tipo, idproducto_central, idespecialidad_central, idsabor_central, "
						+ "idexcepcion_central, idproducto_pos from pos_central.equivalencia_producto")) {
			while (rs.next()) {
				String tipo = rs.getString(1);
				int pos = rs.getInt(6);
				if ("PRODUCTO".equals(tipo)) {
					eqProducto.put(String.valueOf(rs.getInt(2)), pos);
				} else if ("ESPECIALIDAD".equals(tipo)) {
					eqEspecialidad.put(rs.getInt(2) + "|" + rs.getInt(3), pos);
					if (!espDirecta.containsKey(rs.getInt(3))) {
						espDirecta.put(rs.getInt(3), pos);
					}
				} else if ("EXCEPCION".equals(tipo)) {
					eqExcepcion.put(rs.getInt(2) + "|" + rs.getInt(5), pos);
				} else if ("SABOR".equals(tipo)) {
					eqSabor.put(rs.getInt(4), pos);
				}
			}
		}

		// 3. Homologacion SKUs virtual
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select tipo, sku, idinterno from pizzaamericana.homologacion_producto_pedidovirtual")) {
			while (rs.next()) {
				String clave = rs.getString(1) + "|" + normalizar(rs.getString(2));
				if (!skusEcommerce.containsKey(clave)) {
					skusEcommerce.put(clave, rs.getInt(3));
				}
			}
		}

		// 4. Sabores de liquidos
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select descripcion, idproducto from pizzaamericana.sabor_x_tipo_liquido")) {
			while (rs.next()) {
				String desc = rs.getString(1);
				if (desc != null) {
					saborLiquidoCentral.put(normalizar(desc), rs.getInt(2));
				}
			}
		}
	}

	public static String normalizar(String t) {
		if (t == null) return "";
		String sinAcentos = java.text.Normalizer.normalize(t.trim(), java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return sinAcentos.replaceAll("\\s+", " ").toLowerCase();
	}

	/**
	 * Traduce un pedido de Rappi a MotorSolicitud.
	 */
	public MotorSolicitud traducir(
			JSONArray detalleOrdenArray,
			JSONObject jsonOrder,
			int idTienda,
			int idCliente,
			int idTipoPedido,
			String programado,
			String horaProgramado,
			double descuentoPropio,
			double tarifaServicio,
			double propina,
			String marketPlace) {

		MotorSolicitud solicitud = new MotorSolicitud();
		solicitud.contexto.canal = "RAPPI";
		solicitud.contexto.idtienda = idTienda;
		solicitud.contexto.idcliente = idCliente;
		solicitud.contexto.idtipopedido = idTipoPedido > 0 ? idTipoPedido : 1;
		solicitud.contexto.esDomicilio = (idTipoPedido == 1);
		solicitud.contexto.programado = "S".equalsIgnoreCase(programado);
		solicitud.contexto.horaProgramado = (horaProgramado != null) ? horaProgramado : "";
		solicitud.contexto.fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

		// 1. Manejo del domicilio
		if ("S".equalsIgnoreCase(marketPlace) || solicitud.contexto.esDomicilio) {
			double costoEnvio = 4500.0;
			try {
				if (jsonOrder != null && jsonOrder.get("totals") != null) {
					JSONObject totals = (JSONObject) jsonOrder.get("totals");
					if (totals.get("charges") != null) {
						JSONObject charges = (JSONObject) totals.get("charges");
						if (charges.get("shipping") != null) {
							costoEnvio = ((Number) charges.get("shipping")).doubleValue();
						}
					}
				}
			} catch (Exception ignorada) {
			}

			solicitud.domicilio = new MotorSolicitud.Domicilio();
			solicitud.domicilio.idproducto = PRODUCTO_DOMICILIO_POS;
			solicitud.domicilio.valor = costoEnvio;
		}

		// 2. Manejo de descuentos asumidos por la tienda
		if (descuentoPropio > 0) {
			solicitud.descuentos = new MotorSolicitud.Descuentos();
			solicitud.descuentos.descuentoManual = descuentoPropio;
			solicitud.descuentos.motivoDescuento = "DESCUENTO ASUMIDO TIENDA RAPPI";
		}

		if (detalleOrdenArray == null) {
			return solicitud;
		}

		// 3. Procesamiento de items
		for (int i = 0; i < detalleOrdenArray.size(); i++) {
			JSONObject item = (JSONObject) detalleOrdenArray.get(i);
			procesarItemRappi(item, solicitud);
		}

		return solicitud;
	}

	private void procesarItemRappi(JSONObject item, MotorSolicitud solicitud) {
		String nombre = (String) item.get("name");
		if (nombre == null || nombre.trim().isEmpty()) {
			return;
		}
		nombre = nombre.trim();
		String nombreNorm = normalizar(nombre);

		double cantidad = 1.0;
		try {
			Number q = (Number) item.get("quantity");
			if (q != null) cantidad = q.doubleValue();
		} catch (Exception e) {
		}

		// 1. Buscar en homologacion_producto_rappi
		HomologacionRappiInfo hom = homologacionRappi.get(nombreNorm);
		int idProdCentral = (hom != null) ? hom.idProducto : 0;
		int idEspCentral = (hom != null) ? hom.idEspecialidad : 0;
		int idExcCentral = (hom != null) ? hom.idExcepcion : 0;

		// 2. Resolver producto en pos_central
		int idProductoPos = 0;
		if (idExcCentral > 0 && eqExcepcion.containsKey(idProdCentral + "|" + idExcCentral)) {
			idProductoPos = eqExcepcion.get(idProdCentral + "|" + idExcCentral);
		} else if (idProdCentral > 0 && eqProducto.containsKey(String.valueOf(idProdCentral))) {
			idProductoPos = eqProducto.get(String.valueOf(idProdCentral));
		}

		// 3. Si no se encontro directo, intentar por tabla de SKUs
		if (idProductoPos == 0) {
			Integer idSku = skusEcommerce.get("P|" + nombreNorm);
			if (idSku != null && eqProducto.containsKey(String.valueOf(idSku))) {
				idProductoPos = eqProducto.get(String.valueOf(idSku));
				idProdCentral = idSku;
			}
		}

		if (idProductoPos == 0) {
			solicitud.avisosTraduccion.add("PRODUCTO_RAPPI_NO_HOMOLOGADO: " + nombre);
			return;
		}

		MotorSolicitud.Linea linea = new MotorSolicitud.Linea();
		linea.idproducto = idProductoPos;
		linea.cantidad = cantidad;
		linea.observacion = item.get("comments") != null ? item.get("comments").toString() : "";

		// 4. Si tiene especialidad fija
		if (idEspCentral > 0) {
			int idEspPos = resolverEspecialidadPos(idProdCentral, idEspCentral);
			if (idEspPos > 0) {
				MotorSolicitud.Seleccion sel = new MotorSolicitud.Seleccion();
				sel.idproductoOpcion = idEspPos;
				sel.mitad = 0;
				sel.cantidad = 1.0;
				linea.selecciones.add(sel);
			}
		}

		// 5. Procesar subitems (mitades, gaseosas, acompañantes)
		JSONArray subitems = (JSONArray) item.get("subitems");
		if (subitems != null) {
			int mitadIdx = 1;
			for (int j = 0; j < subitems.size(); j++) {
				JSONObject sub = (JSONObject) subitems.get(j);
				String subNombre = (String) sub.get("name");
				if (subNombre == null) continue;
				String subNorm = normalizar(subNombre);

				double subPrecio = 0.0;
				try {
					Number pNum = (Number) sub.get("price");
					if (pNum != null) subPrecio = pNum.doubleValue();
				} catch (Exception ignorada) {
				}

				// A. Verificar si es especialidad (combo o mitad y mitad)
				if (idEspCentral == -1 || linea.selecciones.isEmpty()) {
					HomologacionRappiInfo homEsp = homologacionRappi.get(subNorm);
					int espC = (homEsp != null && homEsp.idEspecialidad > 0) ? homEsp.idEspecialidad : 0;
					if (espC > 0) {
						int idEspPos = resolverEspecialidadPos(idProdCentral, espC);
						if (idEspPos > 0) {
							MotorSolicitud.Seleccion sel = new MotorSolicitud.Seleccion();
							sel.idproductoOpcion = idEspPos;
							sel.mitad = mitadIdx <= 2 ? mitadIdx : 0;
							sel.cantidad = (mitadIdx <= 2) ? 0.5 : 1.0;
							linea.selecciones.add(sel);
							mitadIdx++;
							continue;
						}
					}
				}

				// B. Verificar si es bebida / gaseosa
				Integer idGasCentral = saborLiquidoCentral.get(subNorm);
				if (idGasCentral != null && idGasCentral > 0) {
					Integer idGasPos = eqProducto.get(String.valueOf(idGasCentral));
					if (idGasPos != null && idGasPos > 0) {
						if (subPrecio > 0) {
							// Bebida adicional pagada
							MotorSolicitud.Linea lineaGas = new MotorSolicitud.Linea();
							lineaGas.idproducto = idGasPos;
							lineaGas.cantidad = cantidad;
							solicitud.lineas.add(lineaGas);
						} else {
							// Bebida incluida en combo
							MotorSolicitud.Seleccion sel = new MotorSolicitud.Seleccion();
							sel.idproductoOpcion = idGasPos;
							sel.mitad = 0;
							sel.cantidad = 1.0;
							linea.selecciones.add(sel);
						}
						continue;
					}
				}

				// C. Verificar si es acompañante
				String cat = (String) sub.get("categoryDescription");
				if ("Acompañante".equalsIgnoreCase(cat) || subPrecio > 0) {
					HomologacionRappiInfo homAcom = homologacionRappi.get(subNorm);
					int idAcomC = (homAcom != null) ? homAcom.idProducto : 0;
					Integer idAcomPos = eqProducto.get(String.valueOf(idAcomC));
					if (idAcomPos != null && idAcomPos > 0) {
						MotorSolicitud.Linea lineaAcom = new MotorSolicitud.Linea();
						lineaAcom.idproducto = idAcomPos;
						lineaAcom.cantidad = cantidad;
						solicitud.lineas.add(lineaAcom);
						continue;
					}
				}
			}
		}

		solicitud.lineas.add(linea);
	}

	private int resolverEspecialidadPos(int idProdCentral, int idEspCentral) {
		String clave = idProdCentral + "|" + idEspCentral;
		if (eqEspecialidad.containsKey(clave)) {
			return eqEspecialidad.get(clave);
		}
		if (espDirecta.containsKey(idEspCentral)) {
			return espDirecta.get(idEspCentral);
		}
		return 0;
	}
}

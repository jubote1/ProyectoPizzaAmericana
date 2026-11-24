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
 * Traductor de Pedidos de DiDi Food hacia el Nuevo Motor de Pedidos (pos_central).
 *
 * Convierte el arreglo de order_items, sub_item_list y cargos enviado por el webhook
 * de DiDi a la estructura unificada MotorSolicitud:
 *   1. Mapea pizzas por mitades (ej. "Pizzeta por Mitades", "Mediana 2 Sabores") nativamente
 *      con mitad 1 y mitad 2 en selecciones.
 *   2. Mapea productos y especialidades usando homologacion_producto_rappi y pos_central.equivalencia_producto.
 *   3. Identifica bebidas incluidas y adicionales (precios en centavos divididos por 100).
 *   4. Identifica domicilios de marketplace (tipoDelivery != 1).
 *   5. Aplica descuentos asumidos por la tienda de forma transparente.
 */
public class TraductorDidi {

	public static final int PRODUCTO_DOMICILIO_POS = 412; // Valor del domicilio plataforma

	private static final Logger logger = Logger.getLogger("log_file");

	private final CatalogoPos catalogo;

	// Claves de homologacion en memoria
	private final Map<String, HomologacionDidiInfo> homologacionDidi = new HashMap<>();
	private final Map<String, Integer> eqProducto = new HashMap<>();
	private final Map<String, Integer> eqEspecialidad = new HashMap<>();
	private final Map<Integer, Integer> espDirecta = new HashMap<>();
	private final Map<String, Integer> eqExcepcion = new HashMap<>();
	private final Map<Integer, Integer> eqSabor = new HashMap<>();
	private final Map<String, Integer> skusEcommerce = new HashMap<>();
	private final Map<String, Integer> saborLiquidoCentral = new HashMap<>();

	public static class HomologacionDidiInfo {
		public final int idProducto;
		public final int idEspecialidad;
		public final int idExcepcion;
		public final double precio;

		public HomologacionDidiInfo(int idProducto, int idEspecialidad, int idExcepcion, double precio) {
			this.idProducto = idProducto;
			this.idEspecialidad = idEspecialidad;
			this.idExcepcion = idExcepcion;
			this.precio = precio;
		}
	}

	public TraductorDidi(Connection con, CatalogoPos catalogo) throws Exception {
		this.catalogo = catalogo;
		cargarTablas(con);
	}

	private void cargarTablas(Connection con) throws Exception {
		// 1. Homologacion base compartida con agregadores
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select nombre, idproducto, idespecialidad, idexcepcion, precio from pizzaamericana.homologacion_producto_rappi")) {
			while (rs.next()) {
				String nombre = rs.getString(1);
				if (nombre != null) {
					HomologacionDidiInfo info = new HomologacionDidiInfo(
							rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5));
					homologacionDidi.put(normalizar(nombre), info);
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
	 * Traduce un pedido de DiDi a MotorSolicitud.
	 */
	public MotorSolicitud traducir(
			JSONArray detalleOrdenArray,
			JSONObject jsonOrderInfo,
			int idTienda,
			int idCliente,
			int idTipoPedido,
			String programado,
			String horaProgramado,
			double descuentoPropio,
			long tipoDelivery) {

		MotorSolicitud solicitud = new MotorSolicitud();
		solicitud.contexto.canal = "DIDI";
		solicitud.contexto.idtienda = idTienda;
		solicitud.contexto.idcliente = idCliente;
		solicitud.contexto.idtipopedido = idTipoPedido > 0 ? idTipoPedido : 1;
		solicitud.contexto.esDomicilio = (tipoDelivery != 1);
		solicitud.contexto.programado = "S".equalsIgnoreCase(programado);
		solicitud.contexto.horaProgramado = (horaProgramado != null) ? horaProgramado : "";
		solicitud.contexto.fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

		// 1. Manejo del domicilio si DiDi no lo lleva directo
		if (tipoDelivery != 1) {
			solicitud.domicilio = new MotorSolicitud.Domicilio();
			solicitud.domicilio.idproducto = PRODUCTO_DOMICILIO_POS;
			solicitud.domicilio.valor = 4500.0;
		}

		// 2. Manejo de descuentos asumidos por la tienda
		if (descuentoPropio > 0) {
			solicitud.descuentos = new MotorSolicitud.Descuentos();
			solicitud.descuentos.descuentoManual = descuentoPropio;
			solicitud.descuentos.motivoDescuento = "DESCUENTO ASUMIDO TIENDA DIDI";
		}

		if (detalleOrdenArray == null) {
			return solicitud;
		}

		// 3. Procesamiento de items
		for (int i = 0; i < detalleOrdenArray.size(); i++) {
			JSONObject item = (JSONObject) detalleOrdenArray.get(i);
			procesarItemDidi(item, solicitud);
		}

		return solicitud;
	}

	private void procesarItemDidi(JSONObject item, MotorSolicitud solicitud) {
		String nombre = (String) item.get("name");
		if (nombre == null || nombre.trim().isEmpty()) {
			return;
		}
		nombre = nombre.trim();
		String nombreNorm = normalizar(nombre);

		double cantidad = 1.0;
		try {
			Number a = (Number) item.get("amount");
			if (a != null) cantidad = a.doubleValue();
		} catch (Exception e) {
		}

		// 1. Buscar en homologacion
		HomologacionDidiInfo hom = homologacionDidi.get(nombreNorm);
		int idProdCentral = (hom != null) ? hom.idProducto : 0;
		int idEspCentral = (hom != null) ? hom.idEspecialidad : 0;
		int idExcCentral = (hom != null) ? hom.idExcepcion : 0;

		JSONArray subItems = (JSONArray) item.get("sub_item_list");

		// 2. Si no se encontro o es pizza por mitades, buscar especialidad combinada o inferir tamaño
		if (idProdCentral == 0 && subItems != null) {
			for (int j = 0; j < subItems.size(); j++) {
				JSONObject sub = (JSONObject) subItems.get(j);
				String subNombre = (String) sub.get("name");
				if (subNombre != null) {
					HomologacionDidiInfo homMitad = homologacionDidi.get(normalizar(nombre + " " + subNombre));
					if (homMitad != null && homMitad.idProducto > 0) {
						idProdCentral = homMitad.idProducto;
						idEspCentral = homMitad.idEspecialidad;
						break;
					}
				}
			}
		}

		// 3. Si aun no tiene id central, deducir de palabras clave de tamaño
		if (idProdCentral == 0) {
			if (nombreNorm.contains("pizzeta") || nombreNorm.contains("pequena") || nombreNorm.contains("pequeña")) {
				idProdCentral = 325; // Pequeña / Pizzeta central
			} else if (nombreNorm.contains("mediana")) {
				idProdCentral = 324; // Mediana central
			} else if (nombreNorm.contains("extra grande") || nombreNorm.contains("extragrande")) {
				idProdCentral = 322; // Extra grande central
			} else if (nombreNorm.contains("grande")) {
				idProdCentral = 323; // Grande central
			}
		}

		// 4. Resolver en pos_central
		int idProductoPos = 0;
		if (idExcCentral > 0 && eqExcepcion.containsKey(idProdCentral + "|" + idExcCentral)) {
			idProductoPos = eqExcepcion.get(idProdCentral + "|" + idExcCentral);
		} else if (idProdCentral > 0 && eqProducto.containsKey(String.valueOf(idProdCentral))) {
			idProductoPos = eqProducto.get(String.valueOf(idProdCentral));
		}

		if (idProductoPos == 0) {
			solicitud.avisosTraduccion.add("PRODUCTO_DIDI_NO_HOMOLOGADO: " + nombre);
			return;
		}

		MotorSolicitud.Linea linea = new MotorSolicitud.Linea();
		linea.idproducto = idProductoPos;
		linea.cantidad = cantidad;
		linea.observacion = item.get("remark") != null ? item.get("remark").toString() : "";

		// 5. Si tiene especialidad fija
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

		// 6. Procesar sub_item_list (mitades, bebidas, acompañantes)
		if (subItems != null) {
			int mitadIdx = 1;
			for (int j = 0; j < subItems.size(); j++) {
				JSONObject sub = (JSONObject) subItems.get(j);
				String subNombre = (String) sub.get("name");
				if (subNombre == null) continue;
				String subNorm = normalizar(subNombre);

				double subPrecio = 0.0;
				try {
					Number pNum = (Number) sub.get("total_price");
					if (pNum != null) subPrecio = pNum.doubleValue() / 100.0; // En DiDi viene multiplicado por 100
				} catch (Exception ignorada) {
				}

				// A. Verificar si es especialidad / mitad
				HomologacionDidiInfo homEsp = homologacionDidi.get(subNorm);
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

				// B. Verificar si es bebida
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
							// Bebida incluida
							MotorSolicitud.Seleccion sel = new MotorSolicitud.Seleccion();
							sel.idproductoOpcion = idGasPos;
							sel.mitad = 0;
							sel.cantidad = 1.0;
							linea.selecciones.add(sel);
						}
						continue;
					}
				}

				// C. Verificar si es acompañante / producto adicional
				if (subPrecio > 0) {
					HomologacionDidiInfo homAcom = homologacionDidi.get(subNorm);
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

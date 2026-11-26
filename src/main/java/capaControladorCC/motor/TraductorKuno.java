package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaModeloCC.motor.MotorSolicitud;

/**
 * Traductor de Pedidos de Tienda Virtual Kuno / APP hacia el Motor de Pedidos (pos_central).
 *
 * Convierte el arreglo de items y opciones enviado por GloriaFood / Ecommerce Kuno / App movil
 * a la estructura de contrato MotorSolicitud, utilizando las tablas de equivalencia:
 *   1. homologacion_producto_pedidovirtual: mapea el SKU del ecommerce a idproducto o idespecialidad del central.
 *   2. pos_central.equivalencia_producto: mapea los ids del central a los ids nativos de pos_central.producto.
 */
public class TraductorKuno {

	public static final int PRODUCTO_DOMICILIO_POS = 412;

	private final CatalogoPos catalogo;
	private final Map<String, Integer> skus = new HashMap<String, Integer>();
	private final Map<String, Integer> eqProducto = new HashMap<String, Integer>();
	private final Map<String, Integer> eqEspecialidad = new HashMap<String, Integer>();
	private final Map<String, Integer> eqExcepcion = new HashMap<String, Integer>();
	private final Map<Integer, Integer> eqSabor = new HashMap<Integer, Integer>();

	public TraductorKuno(Connection con, CatalogoPos catalogo) throws Exception {
		this.catalogo = catalogo;
		cargarTablasHomologacion(con);
	}

	private void cargarTablasHomologacion(Connection con) throws Exception {
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select tipo, sku, idinterno from pizzaamericana.homologacion_producto_pedidovirtual")) {
			while (rs.next()) {
				String clave = rs.getString(1) + "|" + normalizar(rs.getString(2));
				if (!skus.containsKey(clave)) {
					skus.put(clave, rs.getInt(3));
				}
			}
		}

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
				} else if ("EXCEPCION".equals(tipo)) {
					eqExcepcion.put(rs.getInt(2) + "|" + rs.getInt(5), pos);
				} else if ("SABOR".equals(tipo)) {
					eqSabor.put(rs.getInt(4), pos);
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
	 * Traduce la orden de Kuno a MotorSolicitud.
	 */
	public MotorSolicitud traducir(JSONArray jsonItemsArray, int idTienda, int idCliente, int idTipoPedido,
			String canal, boolean esProgramado, String horaProgramado) {

		MotorSolicitud solicitud = new MotorSolicitud();
		solicitud.contexto.canal = (canal != null && !canal.trim().isEmpty()) ? canal.toUpperCase() : "KUNO";
		solicitud.contexto.idtienda = idTienda;
		solicitud.contexto.idcliente = idCliente;
		solicitud.contexto.idtipopedido = idTipoPedido > 0 ? idTipoPedido : 1;
		solicitud.contexto.esDomicilio = (idTipoPedido == 1);
		solicitud.contexto.programado = esProgramado;
		solicitud.contexto.horaProgramado = (horaProgramado != null) ? horaProgramado : "";
		solicitud.contexto.fechaHora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

		if (jsonItemsArray == null) {
			return solicitud;
		}

		for (int i = 0; i < jsonItemsArray.size(); i++) {
			JSONObject item = (JSONObject) jsonItemsArray.get(i);
			procesarItem(item, solicitud);
		}

		return solicitud;
	}

	private void procesarItem(JSONObject item, MotorSolicitud solicitud) {
		String sku = (String) item.get("name");
		if (sku == null) {
			return;
		}
		sku = sku.trim();

		// 1. Manejo del costo de domicilio
		if ("DELIVERY_FEE".equalsIgnoreCase(sku) || sku.toLowerCase().contains("domicilio")) {
			long precioDomi = 0;
			try {
				precioDomi = (Long) item.get("price");
			} catch (Exception e) {
				try {
					precioDomi = (Long) item.get("total_item_price");
				} catch (Exception ex2) {
				}
			}
			solicitud.domicilio = new MotorSolicitud.Domicilio();
			solicitud.domicilio.idproducto = PRODUCTO_DOMICILIO_POS;
			solicitud.domicilio.valor = precioDomi;
			return;
		}

		// 2. Extraer cantidad
		double cantidad = 1.0;
		try {
			Long q = (Long) item.get("quantity");
			if (q != null) cantidad = q.doubleValue();
		} catch (Exception e) {
		}

		// 3. Mapear producto base
		int idProductoCentral = obtenerIdCentral("P", sku);
		int idProductoPos = 0;
		if (idProductoCentral > 0 && eqProducto.containsKey(String.valueOf(idProductoCentral))) {
			idProductoPos = eqProducto.get(String.valueOf(idProductoCentral));
		}

		// Si no se encuentra directo por P, intentar como producto genérico o excepción
		if (idProductoPos == 0) {
			Integer idExc = skus.get("EX|" + normalizar(sku));
			if (idExc == null) {
				idExc = skus.get("X|" + normalizar(sku));
			}
			if (idExc != null && idExc > 0) {
				for (Map.Entry<String, Integer> entry : eqExcepcion.entrySet()) {
					if (entry.getKey().endsWith("|" + idExc)) {
						idProductoPos = entry.getValue();
						try {
							String[] partes = entry.getKey().split("\\|");
							idProductoCentral = Integer.parseInt(partes[0]);
						} catch (Exception ignorada) {
						}
						break;
					}
				}
			}
		}

		if (idProductoPos == 0) {
			solicitud.avisosTraduccion.add("PRODUCTO_NO_HOMOLOGADO: " + sku);
			return;
		}

		MotorSolicitud.Linea linea = new MotorSolicitud.Linea();
		linea.idproducto = idProductoPos;
		linea.cantidad = cantidad;

		// 4. Procesar Opciones (sabores, mitades, bebidas, adiciones, etc.)
		JSONArray options = (JSONArray) item.get("options");
		if (options != null) {
			for (int j = 0; j < options.size(); j++) {
				JSONObject opt = (JSONObject) options.get(j);
				procesarOpcion(opt, linea, idProductoCentral, solicitud);
			}
		}

		solicitud.lineas.add(linea);
	}

	private void procesarOpcion(JSONObject opt, MotorSolicitud.Linea linea, int idProductoBaseCentral, MotorSolicitud solicitud) {
		String grupo = (String) opt.get("group_name");
		String nombre = (String) opt.get("name");
		if (nombre == null) return;
		nombre = nombre.trim();
		String grupoNorm = (grupo != null) ? normalizar(grupo) : "";

		// Ignorar selector de tamaño ya implícito en el producto base
		if (grupoNorm.contains("tamano") || grupoNorm.contains("size")) {
			return;
		}

		// Determinar si es Mitad 1 o Mitad 2
		int mitad = 0;
		if (grupoNorm.contains("mitad 1") || grupoNorm.contains("primera mitad") || grupoNorm.contains("half 1")) {
			mitad = 1;
		} else if (grupoNorm.contains("mitad 2") || grupoNorm.contains("segunda mitad") || grupoNorm.contains("half 2")) {
			mitad = 2;
		}

		// Buscar especialidad / sabor
		int idEspCentral = obtenerIdCentral("E", nombre);
		int idOpcionPos = 0;

		if (idEspCentral > 0) {
			String clave = idProductoBaseCentral + "|" + idEspCentral;
			if (eqEspecialidad.containsKey(clave)) {
				idOpcionPos = eqEspecialidad.get(clave);
			} else {
				// Buscar cualquier mapeo de esta especialidad
				for (Map.Entry<String, Integer> entry : eqEspecialidad.entrySet()) {
					if (entry.getKey().endsWith("|" + idEspCentral)) {
						idOpcionPos = entry.getValue();
						break;
					}
				}
			}
		}

		// Si no es especialidad, probar como adición, sabor o bebida (P, T, A)
		if (idOpcionPos == 0) {
			int idAdiCentral = obtenerIdCentral("P", nombre);
			if (idAdiCentral == 0) idAdiCentral = obtenerIdCentral("T", nombre);
			if (idAdiCentral == 0) idAdiCentral = obtenerIdCentral("A", nombre);
			if (idAdiCentral > 0 && eqProducto.containsKey(String.valueOf(idAdiCentral))) {
				idOpcionPos = eqProducto.get(String.valueOf(idAdiCentral));
			}
		}

		if (idOpcionPos > 0) {
			MotorSolicitud.Seleccion sel = new MotorSolicitud.Seleccion();
			sel.idproductoOpcion = idOpcionPos;
			sel.mitad = mitad;
			sel.cantidad = (mitad > 0) ? 0.5 : 1.0;
			linea.selecciones.add(sel);
		} else {
			solicitud.avisosTraduccion.add("OPCION_NO_HOMOLOGADA: " + nombre + " (" + grupo + ")");
		}
	}

	private int obtenerIdCentral(String tipo, String texto) {
		if (texto == null || texto.trim().isEmpty()) return 0;
		Integer id = skus.get(tipo + "|" + normalizar(texto));
		return id != null ? id : 0;
	}
}

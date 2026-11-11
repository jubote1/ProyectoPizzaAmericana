package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import conexionCC.ConexionBaseDatos;

/**
 * Endpoint API REST para consulta del Catalogo Unificado del POS (pos_central)
 * para frontends (Contact Center, Tienda Virtual, Agente de Voz).
 *
 * Rutas: /api/motor/catalogo y /MotorCatalogo
 * Metodos: GET, OPTIONS
 * Parametros:
 *   - accion=menu: Lista de categorias (multimenus) y productos configurados en cada cuadrícula.
 *   - accion=producto&id=...: Ficha del producto con preguntas forzadas, opciones y precios.
 *   - accion=tiendas: Lista de puntos de venta activos con su tipo de POS.
 */
@WebServlet({ "/api/motor/catalogo", "/MotorCatalogo" })
public class MotorCatalogo extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Gson GSON = new Gson();

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();

		String accion = request.getParameter("accion");
		if (accion == null || accion.trim().isEmpty()) {
			accion = "menu";
		}

		try (Connection con = new ConexionBaseDatos().obtenerConexionBDPrincipal()) {
			if ("menu".equalsIgnoreCase(accion)) {
				out.write(obtenerMenuCompleto(con));
			} else if ("producto".equalsIgnoreCase(accion)) {
				String idStr = request.getParameter("id");
				int idProducto = (idStr != null && !idStr.trim().isEmpty()) ? Integer.parseInt(idStr.trim()) : 0;
				out.write(obtenerDetalleProducto(con, idProducto));
			} else if ("tiendas".equalsIgnoreCase(accion)) {
				out.write(obtenerTiendas(con));
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"error\":\"Accion no soportada: " + accion + "\"}");
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"error\":\"Error consultando catalogo: " + t.getMessage() + "\"}");
		}
	}

	private String obtenerMenuCompleto(Connection con) throws Exception {
		// 1. Obtener categorias (multimenus)
		String sqlMulti = "select idmultimenu, nombre from pos_central.nombre_multimenu order by idmultimenu";
		List<Map<String, Object>> listaMulti = new ArrayList<Map<String, Object>>();
		Map<Integer, Map<String, Object>> mapaMulti = new HashMap<Integer, Map<String, Object>>();

		try (PreparedStatement ps = con.prepareStatement(sqlMulti); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				Map<String, Object> m = new HashMap<String, Object>();
				int id = rs.getInt("idmultimenu");
				m.put("idmultimenu", id);
				m.put("nombre", rs.getString("nombre"));
				m.put("items", new ArrayList<Map<String, Object>>());
				listaMulti.add(m);
				mapaMulti.put(id, m);
			}
		}

		// 2. Obtener productos configurados en la cuadricula de cada multimenu
		String sqlItems = "select cm.multimenu, cm.menu, cm.fila, cm.columna, cm.idproducto, "
				+ "p.descripcion, p.tipo_producto as tipo, p.precio1, p.precio_pila, p.promocion "
				+ "from pos_central.configuracion_menu cm "
				+ "join pos_central.producto p on p.idproducto = cm.idproducto "
				+ "where p.no_venta = 0 or p.no_venta = '0' or p.no_venta = 0x30 "
				+ "order by cm.multimenu, cm.menu, cm.fila, cm.columna";

		try (PreparedStatement ps = con.prepareStatement(sqlItems); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				int idMulti = rs.getInt("multimenu");
				Map<String, Object> multi = mapaMulti.get(idMulti);
				if (multi != null) {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> items = (List<Map<String, Object>>) multi.get("items");
					Map<String, Object> item = new HashMap<String, Object>();
					item.put("idproducto", rs.getInt("idproducto"));
					item.put("descripcion", rs.getString("descripcion"));
					item.put("tipo", rs.getString("tipo"));
					item.put("precio", rs.getDouble("precio1"));
					item.put("precioPila", rs.getInt("precio_pila"));
					item.put("promocion", rs.getString("promocion"));
					item.put("menu", rs.getInt("menu"));
					item.put("fila", rs.getInt("fila"));
					item.put("columna", rs.getInt("columna"));
					items.add(item);
				}
			}
		}

		Map<String, Object> resp = new HashMap<String, Object>();
		resp.put("multimenus", listaMulti);
		return GSON.toJson(resp);
	}

	private String obtenerDetalleProducto(Connection con, int idProducto) throws Exception {
		if (idProducto <= 0) {
			return "{\"error\":\"idproducto invalido\"}";
		}

		Map<String, Object> resp = new HashMap<String, Object>();

		// Datos basicos del producto
		String sqlP = "select idproducto, descripcion, tipo_producto as tipo, precio1, precio_pila, id_producto_depende, precio_puntos, promocion, "
				+ "idpreguntaforzada1, idpreguntaforzada2, idpreguntaforzada3, idpreguntaforzada4, idpreguntaforzada5 "
				+ "from pos_central.producto where idproducto = ?";
		int[] idPreguntas = new int[5];

		try (PreparedStatement ps = con.prepareStatement(sqlP)) {
			ps.setInt(1, idProducto);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return "{\"error\":\"Producto no encontrado\"}";
				}
				resp.put("idproducto", rs.getInt("idproducto"));
				resp.put("descripcion", rs.getString("descripcion"));
				resp.put("tipo", rs.getString("tipo"));
				resp.put("precio1", rs.getDouble("precio1"));
				resp.put("precioPila", rs.getInt("precio_pila"));
				resp.put("idProductoDepende", rs.getInt("id_producto_depende"));
				resp.put("precioPuntos", rs.getDouble("precio_puntos"));
				resp.put("promocion", rs.getString("promocion"));

				for (int i = 0; i < 5; i++) {
					idPreguntas[i] = rs.getInt("idpreguntaforzada" + (i + 1));
				}
			}
		}

		// Productos incluidos por defecto (ej. salsas $0)
		String sqlInc = "select pi.idproductoincluye, p.descripcion, pi.precio as columna_precio "
				+ "from pos_central.producto_incluido pi "
				+ "join pos_central.producto p on p.idproducto = pi.idproductoincluye "
				+ "where pi.idproductoincluido = ?";
		List<Map<String, Object>> incluidos = new ArrayList<Map<String, Object>>();
		try (PreparedStatement ps = con.prepareStatement(sqlInc)) {
			ps.setInt(1, idProducto);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> inc = new HashMap<String, Object>();
					inc.put("idproducto", rs.getInt("idproductoincluye"));
					inc.put("descripcion", rs.getString("descripcion"));
					inc.put("columnaPrecio", rs.getString("columna_precio"));
					incluidos.add(inc);
				}
			}
		}
		resp.put("incluidos", incluidos);

		// Preguntas forzadas con sus opciones
		List<Map<String, Object>> preguntas = new ArrayList<Map<String, Object>>();
		for (int idPregunta : idPreguntas) {
			if (idPregunta <= 0) {
				continue;
			}
			String sqlPreg = "select idpregunta, titulopregunta, descripcion, obliga_eleccion, "
					+ "numero_minimo_eleccion, numero_maximo_eleccion, permite_dividir "
					+ "from pos_central.pregunta where idpregunta = ?";
			Map<String, Object> preg = new HashMap<String, Object>();
			try (PreparedStatement ps = con.prepareStatement(sqlPreg)) {
				ps.setInt(1, idPregunta);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						preg.put("idpregunta", rs.getInt("idpregunta"));
						preg.put("titulo", rs.getString("titulopregunta"));
						preg.put("descripcion", rs.getString("descripcion"));
						preg.put("obligaEleccion", rs.getInt("obliga_eleccion"));
						preg.put("minimo", rs.getInt("numero_minimo_eleccion"));
						preg.put("maximo", rs.getInt("numero_maximo_eleccion"));
						preg.put("permiteDividir", rs.getInt("permite_dividir"));
					}
				}
			}

			// Opciones de esta pregunta
			String sqlOp = "select ef.idproducto, p.descripcion, ef.precio as col_precio, "
					+ "p.precio1, p.precio2, p.precio3, p.precio4, p.precio5, p.precio6, p.precio7, p.precio8, p.precio9, p.precio10 "
					+ "from pos_central.eleccion_forzada ef "
					+ "join pos_central.producto p on p.idproducto = ef.idproducto "
					+ "where ef.idpregunta = ? and ef.estado = 1 and (p.no_venta = 0 or p.no_venta = '0' or p.no_venta = 0x30) "
					+ "order by ef.ideleccion_forzada, p.descripcion";
			List<Map<String, Object>> opciones = new ArrayList<Map<String, Object>>();
			try (PreparedStatement ps = con.prepareStatement(sqlOp)) {
				ps.setInt(1, idPregunta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Map<String, Object> op = new HashMap<String, Object>();
						op.put("idproducto", rs.getInt("idproducto"));
						op.put("descripcion", rs.getString("descripcion"));
						String colPrecio = rs.getString("col_precio");
						if (colPrecio == null || colPrecio.trim().isEmpty()) {
							colPrecio = "precio1";
						} else {
							colPrecio = colPrecio.trim().toLowerCase();
						}
						double valorPrecio = 0.0;
						try {
							valorPrecio = rs.getDouble(colPrecio);
						} catch (Exception ex) {
							valorPrecio = rs.getDouble("precio1");
						}
						op.put("precio", valorPrecio);
						opciones.add(op);
					}
				}
			}
			preg.put("opciones", opciones);
			preguntas.add(preg);
		}
		resp.put("preguntas", preguntas);

		return GSON.toJson(resp);
	}

	private String obtenerTiendas(Connection con) throws Exception {
		String sql = "select idtienda, nombre, url, dsn, pos, hosbd from pizzaamericana.tienda where funcional = 'S' order by idtienda";
		List<Map<String, Object>> tiendas = new ArrayList<Map<String, Object>>();
		try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				Map<String, Object> t = new HashMap<String, Object>();
				t.put("idtienda", rs.getInt("idtienda"));
				t.put("nombre", rs.getString("nombre"));
				t.put("url", rs.getString("url"));
				t.put("dsn", rs.getString("dsn"));
				t.put("pos", rs.getInt("pos"));
				t.put("host", rs.getString("hosbd"));
				tiendas.add(t);
			}
		}
		Map<String, Object> resp = new HashMap<String, Object>();
		resp.put("tiendas", tiendas);
		return GSON.toJson(resp);
	}

	private void configurarCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
	}
}

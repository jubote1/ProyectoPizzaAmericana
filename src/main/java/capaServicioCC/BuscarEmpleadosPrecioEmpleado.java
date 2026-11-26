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
 * Servicio para busqueda reactiva de empleados de nomina activos (general.empleado).
 * Permite identificar al beneficiario del precio empleado en TomaPedidosPOS.
 *
 * Rutas: /BuscarEmpleadosPrecioEmpleado, /api/precio-empleado/empleados
 */
@WebServlet({ "/BuscarEmpleadosPrecioEmpleado", "/api/precio-empleado/empleados" })
public class BuscarEmpleadosPrecioEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Gson GSON = new Gson();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");

		String query = request.getParameter("q");
		if (query == null || query.trim().length() < 2) {
			response.getWriter().write("[]");
			return;
		}

		String termino = "%" + query.trim() + "%";
		List<Map<String, Object>> lista = new ArrayList<Map<String, Object>>();

		ConexionBaseDatos cnx = new ConexionBaseDatos();
		try (Connection con = cnx.obtenerConexionBDGeneral()) {
			String sql = "SELECT id, nombre, nombre_largo, IFNULL(email, '') as email "
					+ "FROM general.empleado "
					+ "WHERE activo = 1 "
					+ "  AND (nombre LIKE ? OR nombre_largo LIKE ?) "
					+ "ORDER BY nombre_largo LIMIT 25";

			try (PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setString(1, termino);
				ps.setString(2, termino);

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int id = rs.getInt("id");
						String cedula = rs.getString("nombre");
						String nombreLargo = rs.getString("nombre_largo");
						String email = rs.getString("email").trim();

						Map<String, Object> emp = new HashMap<String, Object>();
						emp.put("id", id);
						emp.put("cedula", cedula);
						emp.put("nombre", nombreLargo);
						emp.put("email", email);
						emp.put("emailEnmascarado", enmascararEmail(email));
						emp.put("tieneEmail", !email.isEmpty() && email.contains("@"));

						lista.add(emp);
					}
				}
			}
		} catch (Throwable t) {
			System.err.println("Error en BuscarEmpleadosPrecioEmpleado: " + t.getMessage());
		}

		PrintWriter out = response.getWriter();
		out.write(GSON.toJson(lista));
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	public static String enmascararEmail(String email) {
		if (email == null || email.trim().isEmpty() || !email.contains("@")) {
			return "Sin correo registrado";
		}
		String[] partes = email.split("@");
		String usuario = partes[0];
		String dominio = partes.length > 1 ? partes[1] : "";

		if (usuario.length() <= 2) {
			return usuario.charAt(0) + "***@" + dominio;
		}
		int len = usuario.length();
		String prefix = usuario.substring(0, Math.min(2, len));
		String suffix = (len > 3) ? usuario.substring(len - 2) : "";
		return prefix + "***" + suffix + "@" + dominio;
	}
}

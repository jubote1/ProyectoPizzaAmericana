package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.EmpleadoEncuestaDAO;

/**
 * Servicio para consultar los empleados activos asociados a una tienda.
 * Ruta: /ProyectoPizzaAmericana/ObtenerEmpleadosTiendaAPP
 * Recibe: idtienda (int)
 * Devuelve: JSON Array [{ "id": 123, "nombre": "Nombre Apellido", "cargo": "Cargo" }, ...]
 */
@WebServlet("/ObtenerEmpleadosTiendaAPP")
public class ObtenerEmpleadosTiendaAPP extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ObtenerEmpleadosTiendaAPP() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}

		ArrayList<JSONObject> empleados = EmpleadoEncuestaDAO.obtenerEmpleadosPorTienda(idTienda);
		JSONArray respuesta = new JSONArray();
		if (empleados != null) {
			for (JSONObject emp : empleados) {
				respuesta.add(emp);
			}
		}

		PrintWriter out = response.getWriter();
		out.write(respuesta.toJSONString());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}


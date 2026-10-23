package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utilidadesCC.AccesoCRM;
import capaControladorCC.Persona360Ctrl;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca AccesoCRM.puede, AccesoCRM.negado.
 *
 * Ruta: /ProyectoPizzaAmericana/BuscarPersonaCRM   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): texto (string).
 * Devuelve: application/json; charset=UTF-8; claves: apellido, busco_por, celular, email, error,
 * idpersona, mensaje, nombre, pedidos, personas, ultimo.
 * Lee: cliente, crm.persona, pedido, usuario.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha BuscarPersonaCRM.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/BuscarPersonaCRM")
public class BuscarPersonaCRM extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		//El CRM entrega datos personales de 450 mil personas. Esconder la
		//opcion del menu no protege nada: esta URL se puede escribir a mano.
		if (!AccesoCRM.puede(request)) {
			PrintWriter negado = response.getWriter();
			negado.write(AccesoCRM.negado());
			return;
		}
		String respuesta = Persona360Ctrl.buscar(request.getParameter("texto"));
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

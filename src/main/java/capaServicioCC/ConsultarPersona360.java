package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import utilidadesCC.AccesoCRM;
import capaControladorCC.Persona360Ctrl;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca AccesoCRM.puede, AccesoCRM.negado.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarPersona360   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpersona (long).
 * Devuelve: application/json; charset=UTF-8; claves: apellido, asunto, canal, cancelado, caras,
 * celular, codigo, comentario, cuando, destino, direccion, email, ....
 * Lee: cliente, crm.envio_brevo, crm.persona, crm.stage_cliente_tienda, crm.v_persona_360,
 * estado_pqrs, oferta, oferta_cliente, pedido, pizzaamericana.plantilla_brevo.
 * Sistemas externos: Brevo.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarPersona360.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarPersona360")
public class ConsultarPersona360 extends HttpServlet {
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
		Logger logger = Logger.getLogger("log_file");
		long idPersona = 0;
		try {
			idPersona = Long.parseLong(request.getParameter("idpersona"));
		} catch (Exception e) {
			idPersona = 0;
		}
		logger.info("consultar persona 360 idpersona=" + idPersona);
		String respuesta = Persona360Ctrl.consultar(idPersona);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

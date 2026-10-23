package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.SegmentacionPersonaCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca AccesoCRM.puede, AccesoCRM.negado.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarSegmentacionPersona   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; charset=UTF-8; claves: activo, apellido, autorizados, calculado_en,
 * celular, color, con_correo, descripcion, dias_activo, dias_entre_pedidos, dias_riesgo,
 * dias_sin_comprar, ....
 * Lee: apellido, celular, crm.parametro_segmento, crm.persona_resumen, crm.segmento_definicion,
 * crm.segmento_regla, dias_entre_pedidos, dias_sin_comprar, email, nombre.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarSegmentacionPersona.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarSegmentacionPersona")
public class ConsultarSegmentacionPersona extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		//Datos personales de 450 mil personas. Esconder la opcion del menu no
		//protege nada: esta URL se escribe a mano.
		if (!AccesoCRM.puede(request)) {
			PrintWriter negado = response.getWriter();
			negado.write(AccesoCRM.negado());
			return;
		}
		String respuesta = SegmentacionPersonaCtrl.consultar(request);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

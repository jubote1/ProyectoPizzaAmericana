package capaServicioCC;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CorreoPQRS;


/**
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca
 * CorreoPQRS.correoEncuestaPqrs.
 *
 * Ruta: /ProyectoPizzaAmericana/CorreoEncuestaPqrs   Metodos: POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): cliente (string), correo (string), telefono (string).
 * Devuelve: cuerpo vacio; claves: mail.smtp.host, mail.smtp.ssl.protocols, message, success.
 * Lee: parametros, parametros_correo.
 * Escribe: solicitudpqrs (UPDATE).
 * Sistemas externos: Correo SMTP, Gmail.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CorreoEncuestaPqrs.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CorreoEncuestaPqrs")

public class CorreoEncuestaPqrs extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public CorreoEncuestaPqrs() {
		super();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    request.setCharacterEncoding("UTF-8");
	    response.setCharacterEncoding("UTF-8");
	    CorreoPQRS respuestaPqrs = new CorreoPQRS();

	    String cliente = request.getParameter("cliente");
	    String correo = request.getParameter("correo");
	    String telefono = request.getParameter("telefono");
	    int idpqrs = parseIntSafe(request, "idpqrs");
	    
	    String respuesta = respuestaPqrs.correoEncuestaPqrs(correo, cliente,telefono,idpqrs).toString() ;
	    response.getWriter().write(respuesta);
	}

	// Métodos auxiliares de parseo seguro
	private int parseIntSafe(HttpServletRequest request, String paramName) {
		try {
			return Integer.parseInt(request.getParameter(paramName));
		} catch (Exception e) {
			return 0;
		}
	}


}

package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;


import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import capaControladorCC.CorreoPQRS;


@WebServlet("/CorreoRespuestaPqrs")
public class CorreoRespuestaPqrs extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public CorreoRespuestaPqrs() {
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
	    String contenido = request.getParameter("contenido");
	    int idpqrs = parseIntSafe(request, "idpqrs");

	    String respuesta = respuestaPqrs.enviarRespuesta(correo, cliente, idpqrs, contenido).toString() ;
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

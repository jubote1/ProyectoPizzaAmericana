package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.SegmentacionPersonaCtrl;
import capaDAOCC.SegmentacionPersonaDAO;
import utilidadesCC.AccesoCRM;

/**
 * Servlet implementation class DescargarSegmentacionPersona
 * Entrega en CSV la lista de personas que cumplen los filtros del CRM.
 *
 * QUEDA ESCRITO QUIEN DESCARGO Y QUE
 *
 * Esto saca de la empresa un archivo con nombre, celular y correo de miles de
 * personas. No se bloquea -para eso esta la pantalla- pero si queda en el log
 * quien lo pidio, con que filtros y cuantas filas se llevo. Si manana aparece
 * una base de datos nuestra donde no debe, esa linea es la unica forma de
 * saber por donde salio.
 */
@WebServlet("/DescargarSegmentacionPersona")
public class DescargarSegmentacionPersona extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!AccesoCRM.puede(request)) {
			response.setContentType("application/json; charset=UTF-8");
			PrintWriter negado = response.getWriter();
			negado.write(AccesoCRM.negado());
			return;
		}

		SegmentacionPersonaDAO.Filtro filtro = SegmentacionPersonaCtrl.filtroDe(request);
		ArrayList<SegmentacionPersonaDAO.Fila> filas = SegmentacionPersonaDAO.paraDescargar(filtro);

		Logger.getLogger("log_file").info("CRM descarga de segmentacion: usuario="
				+ AccesoCRM.usuarioEnSesion(request)
				+ " filas=" + filas.size()
				+ " segmentos=[" + texto(request.getParameter("segmentos")) + "]"
				+ " tienda=" + texto(request.getParameter("idtienda"))
				+ " canal=" + texto(request.getParameter("canal"))
				+ " pedidosmin=" + texto(request.getParameter("pedidosmin"))
				+ " valormin=" + texto(request.getParameter("valormin"))
				+ " diasmin=" + texto(request.getParameter("diasmin"))
				+ " diasmax=" + texto(request.getParameter("diasmax"))
				+ " concorreo=" + texto(request.getParameter("concorreo"))
				+ " autorizados=" + texto(request.getParameter("autorizados")));

		String nombre = "segmentacion_"
				+ new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".csv";
		response.setContentType("text/csv; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + nombre + "\"");
		PrintWriter out = response.getWriter();
		out.write(SegmentacionPersonaCtrl.csv(filas));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private static String texto(String s) {
		return (s == null ? "" : s);
	}
}

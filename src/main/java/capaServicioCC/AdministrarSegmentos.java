package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.DefinicionSegmentoCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Las definiciones de segmento: listar, probar, guardar, borrar y clasificar.
 *
 * Van las cinco en un solo servlet, separadas por el parametro accion, porque
 * son la misma pantalla y comparten la validacion de acceso. Cinco URL para
 * cinco botones serian cinco sitios donde olvidar esa validacion.
 *
 * ESTE SERVICIO ESCRIBE, Y ESO CAMBIA A QUIEN LE RESPONDE
 *
 * Los demas del CRM leen. Este cambia como se clasifican 450 mil personas, y
 * clasificar de nuevo toca la tabla que el CRM esta leyendo en ese momento. Por
 * eso ademas de exigir acceso al CRM -AccesoCRM.puede- las acciones que
 * escriben solo entran por POST: un GET se deja en un enlace, en un correo o en
 * el historial del navegador, y nadie debe poder reclasificar a media empresa
 * con un clic prestado.
 *
 * Servlet implementation class AdministrarSegmentos
 */
@WebServlet("/AdministrarSegmentos")
public class AdministrarSegmentos extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public AdministrarSegmentos() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, false);
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, true);
	}

	private void atender(final HttpServletRequest request, final HttpServletResponse response,
			final boolean esPost) throws IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		String accion = request.getParameter("accion");
		if (accion == null) {
			accion = "listar";
		}
		accion = accion.trim().toLowerCase();

		try {
			if ("listar".equals(accion)) {
				out.write(DefinicionSegmentoCtrl.listar());
			} else if ("probar".equals(accion)) {
				//Probar no escribe nada, pero recorre las 450 mil filas, asi que
				//tambien va por POST para que no quede en el historial.
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.probar(request) : soloPost());
			} else if ("guardar".equals(accion)) {
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.guardar(request) : soloPost());
			} else if ("borrar".equals(accion)) {
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.borrar(request) : soloPost());
			} else if ("clasificar".equals(accion)) {
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.clasificar(request) : soloPost());
			} else {
				out.write("{\"error\":\"Accion desconocida.\"}");
			}
		} catch (final Exception e) {
			//El mensaje del error NO se le devuelve al navegador: puede traer el
			//SQL o el nombre de una tabla. Va al log, que es donde se mira.
			System.out.println("AdministrarSegmentos: " + e);
			out.write("{\"error\":\"No se pudo completar la operacion. Revise el log.\"}");
		}
	}

	private boolean exigirPost(final boolean esPost) {
		return (esPost);
	}

	private String soloPost() {
		return ("{\"error\":\"Esta operacion solo se acepta por POST.\"}");
	}
}

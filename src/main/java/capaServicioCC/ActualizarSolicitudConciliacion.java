package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Guarda la resolucion de una diferencia de conciliacion.
 *
 * No existia. La pantalla de conciliacion era de solo lectura, asi que el
 * estado de una diferencia se cambiaba a mano en la base de datos. Al
 * 2026-09-15 habia 122 diferencias PENDIENTE por $5.852.232, la mas vieja de
 * hace 422 dias: nadie las estaba cerrando porque no habia con que.
 *
 * Se toma el usuario de la SESION y no de un parametro: quien resolvio una
 * diferencia de plata no puede ser algo que el navegador diga de si mismo.
 */
@WebServlet("/ActualizarSolicitudConciliacion")
public class ActualizarSolicitudConciliacion extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ActualizarSolicitudConciliacion() {
		super();
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		//El usuario sale de la sesion. Si no hay sesion no se guarda nada: es un
		//cambio sobre plata y tiene que quedar a nombre de alguien.
		final javax.servlet.http.HttpSession sesion = request.getSession(false);
		final Usuario enSesion = (sesion == null) ? null : (Usuario) sesion.getAttribute("usuario");
		if (enSesion == null || enSesion.getNombreLargo() == null
				|| enSesion.getNombreLargo().trim().length() == 0) {
			out.write("{\"respuesta\":\"SINSESION\"}");
			return;
		}

		int idSolicitud = 0;
		double valorFinal = 0;
		try {
			idSolicitud = Integer.parseInt(request.getParameter("idsolicitud"));
			//Antes esto borraba TODOS los puntos, asi que "1377.5" quedaba en
			//13775: diez veces mas. La regla vive ahora en
			//utilidadesCC.ValorDigitado, la misma que usa el ingreso de la
			//solicitud, porque resuelta aparte salio distinta en cada lado.
			//Aca el vacio SI vale: una solicitud pendiente todavia no tiene
			//valor final.
			final Double leido = utilidadesCC.ValorDigitado.leer(
					request.getParameter("valorfinal"), true);
			if (leido == null) {
				out.write("{\"respuesta\":\"DATOSMALOS\"}");
				return;
			}
			valorFinal = leido.doubleValue();
		} catch (final Exception e) {
			out.write("{\"respuesta\":\"DATOSMALOS\"}");
			return;
		}
		if (idSolicitud <= 0) {
			out.write("{\"respuesta\":\"DATOSMALOS\"}");
			return;
		}

		final String estado = request.getParameter("estado") == null ? ""
				: request.getParameter("estado").trim().toUpperCase();
		if (!"PENDIENTE".equals(estado) && !"PROCESADO".equals(estado)) {
			out.write("{\"respuesta\":\"ESTADOMALO\"}");
			return;
		}

		final String observacion = request.getParameter("observacion") == null ? ""
				: request.getParameter("observacion").trim();
		//Cerrar una diferencia sin decir que se hizo con ella deja el registro
		//inservible para auditar despues.
		if ("PROCESADO".equals(estado) && observacion.length() < 10) {
			out.write("{\"respuesta\":\"FALTAOBSERVACION\"}");
			return;
		}

		out.write(PedidoCtrl.actualizarSolicitudConciliacion(idSolicitud, valorFinal, estado,
				observacion, enSesion.getNombreLargo()));
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}
}

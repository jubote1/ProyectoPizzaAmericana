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
 * Servicio que modifica datos de Pagos virtuales y conciliacion: invoca ValorDigitado.leer,
 * PedidoCtrl.actualizarSolicitudConciliacion.
 *
 * Ruta: /ProyectoPizzaAmericana/ActualizarSolicitudConciliacion   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idsolicitud (integer), valorfinal (string), estado (string), observacion
 * (string).
 * Devuelve: application/json; charset=UTF-8; claves: respuesta.
 * Escribe: solicitud_conciliacion (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ActualizarSolicitudConciliacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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

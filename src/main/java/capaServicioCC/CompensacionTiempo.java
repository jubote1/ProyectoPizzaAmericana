package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CompensacionTiempoCtrl;
import capaControladorCC.EnvioPublicidadCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Compensaciones por tiempo: los pedidos a domicilio que se demoraron, y la oferta de disculpa que se
 * les manda a quienes se escojan.
 *
 * ACCIONES
 *
 *   consultar  GET   fecha (aaaa-mm-dd, anterior a hoy), minutos, idtienda (0 = todas)
 *                    Los pedidos que se demoraron al menos esos minutos, con a quien se le puede escribir.
 *   ofertas    GET   Las ofertas que se pueden mandar (personales, con codigo y caducidad).
 *   vistaprevia GET  idoferta, fecha. El correo de disculpa con datos de ejemplo.
 *   enviar     POST  fecha, minutos, idoferta, pedidos (lista "idtienda:idpedido" separada por coma).
 *                    Manda el correo de disculpa con la oferta. Es lo unico que escribe.
 *
 * Acceso: el mismo del CRM (usuario.acceso_crm). Esta pantalla le escribe a clientes con una oferta que
 * cuesta plata, asi que la valida el servidor y no solo el menu. El usuario sale de la sesion.
 */
@WebServlet("/CompensacionTiempo")
public class CompensacionTiempo extends HttpServlet {
	private static final long serialVersionUID = 1L;

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
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		final String accion = texto(request.getParameter("accion"));

		if ("consultar".equals(accion)) {
			out.write(CompensacionTiempoCtrl.consultar(texto(request.getParameter("fecha")),
					entero(request.getParameter("minutos")), entero(request.getParameter("idtienda")),
					"S".equals(texto(request.getParameter("soloexceso")))));

		} else if ("resumen".equals(accion)) {
			out.write(CompensacionTiempoCtrl.resumen(texto(request.getParameter("fecha")),
					entero(request.getParameter("dias")), entero(request.getParameter("minutos"))));

		} else if ("ofertas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.ofertas());

		} else if ("vistaprevia".equals(accion)) {
			out.write(CompensacionTiempoCtrl.vistaPrevia(entero(request.getParameter("idoferta")),
					texto(request.getParameter("fecha"))));

		} else if ("enviar".equals(accion)) {
			if (!esPost) {
				//Manda correos con una oferta: un enlace o un rastreador no lo puede disparar.
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				out.write("{\"error\":\"Use POST para enviar.\"}");
				return;
			}
			final ArrayList<String> seleccion = new ArrayList<String>();
			final String lista = texto(request.getParameter("pedidos"));
			if (lista.length() > 0) {
				final String[] partes = lista.split(",");
				for (int i = 0; i < partes.length; i++) {
					if (partes[i].trim().length() > 0) {
						seleccion.add(partes[i].trim());
					}
				}
			}
			out.write(CompensacionTiempoCtrl.enviar(texto(request.getParameter("fecha")),
					entero(request.getParameter("minutos")),
					"S".equals(texto(request.getParameter("soloexceso"))),
					entero(request.getParameter("idoferta")), seleccion, AccesoCRM.usuarioEnSesion(request)));

		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	private static String texto(final String v) {
		return (v == null ? "" : v.trim());
	}

	private static int entero(final String v) {
		try {
			return (Integer.parseInt(texto(v)));
		} catch (final Exception e) {
			return (0);
		}
	}
}

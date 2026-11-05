package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import capaControladorCC.ConsultaPuntosClienteCtrl;

/**
 * Los puntos de un cliente, para el POS de la tienda.
 *
 * POR QUE EXISTE OTRO SERVICIO Y NO SE USA ConsultarPuntosCliente
 *
 * Aquel exige sesion HTTP y un usuario del central en la sesion. El POS es una
 * aplicacion Swing que no tiene sesion web: llama servicios sueltos por HTTP
 * contra tienda.urlcontact, igual que ConfirmarRedencionPuntos. Por eso este.
 *
 * NO SE DUPLICA NADA
 *
 * Toda la logica -las 837 lineas de ConsultaPuntosClienteDAO- se queda donde
 * esta: aqui solo se llama a ConsultaPuntosClienteCtrl y se recorta la
 * respuesta. Si manana cambia como se calculan los puntos, cambia en un solo
 * lado y las dos pantallas lo heredan.
 *
 * QUE SE RECORTA Y POR QUE
 *
 * La ficha completa trae el historial de compras por mes, por tienda, el
 * comportamiento y hasta 200 movimientos. En el mostrador eso no se lee -el
 * cliente esta al frente esperando- y es informacion personal viajando por un
 * servicio que hoy no pide credenciales. Se mandan solo las cuatro cosas que
 * sirven en caja:
 *
 *   ficha         quien es y si esta en el programa
 *   totales       el saldo y lo que se le vence
 *   vencimientos  cuando se le vence y cuanto
 *   catalogo      que premios alcanza con lo que tiene
 *   bonos         los bonos vivos, que tambien puede usar ya
 *
 * OJO, PENDIENTE DE SEGURIDAD
 *
 * Este servicio NO valida quien llama, igual que los demas que consume el POS
 * (ConfirmarRedencionPuntos y compania). Es el estandar que hay hoy, no es que
 * se haya decidido asi. Como aqui viajan datos de personas, convendria exigirle
 * el token por tienda de homologacion_tienda_token, que ya existe y hoy solo
 * usa Rappi. Queda dicho a proposito para que no pase inadvertido.
 *
 * Recibe: accion (BUSCAR o FICHA), q (correo o celular) o correo.
 * Devuelve JSON con resultado OK o ERROR.
 */
@WebServlet("/ConsultarPuntosClienteTienda")
public class ConsultarPuntosClienteTienda extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Lo que se le quita a la ficha antes de mandarla a la tienda. */
	private static final String[] FUERA = { "movimientos", "portienda", "pormes", "comportamiento" };

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		try {
			final String accion = texto(request.getParameter("accion")).toUpperCase();

			if ("BUSCAR".equals(accion)) {
				final String q = texto(request.getParameter("q")).trim();
				if (q.length() < 4 || q.length() > 80) {
					out.write(error("Escriba el correo o el celular del cliente."));
					return;
				}
				out.write(ConsultaPuntosClienteCtrl.buscar(q).toJSONString());
				return;
			}

			if ("FICHA".equals(accion)) {
				final String correo = texto(request.getParameter("correo")).trim().toLowerCase();
				if (correo.length() < 5 || correo.length() > 50 || correo.indexOf('@') < 0) {
					out.write(error("El correo no es valido."));
					return;
				}
				//Queda en el log quien pregunto por quien, igual que en la pantalla
				//del central. La tienda manda su nombre; si no lo manda, se dice.
				final String tienda = texto(request.getParameter("tienda")).trim();
				Logger.getLogger("log_file").info("ConsultarPuntosClienteTienda: "
						+ (tienda.length() == 0 ? "tienda sin identificar" : tienda) + " consulto " + correo);

				final JSONObject ficha = ConsultaPuntosClienteCtrl.ficha(correo);
				for (int i = 0; i < FUERA.length; i++) {
					ficha.remove(FUERA[i]);
				}
				out.write(ficha.toJSONString());
				return;
			}

			out.write(error("Accion desconocida."));

		} catch (final Exception e) {
			//El detalle va al log, no al POS: un stack trace en pantalla de caja
			//no le sirve a nadie y puede decir de mas.
			Logger.getLogger("log_file").error("ConsultarPuntosClienteTienda: " + e.toString());
			out.write(error("No se pudo consultar los puntos."));
		} finally {
			out.flush();
			out.close();
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@SuppressWarnings("unchecked")
	private static String error(final String mensaje) {
		final JSONObject o = new JSONObject();
		o.put("resultado", "ERROR");
		o.put("mensaje", mensaje);
		return (o.toJSONString());
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}
}

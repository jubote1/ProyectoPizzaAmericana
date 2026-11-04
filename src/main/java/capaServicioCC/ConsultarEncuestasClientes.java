package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.EncuestaClienteCtrl;
import capaControladorCC.RestriccionTiendaCtrl;
import capaModeloCC.Usuario;

/**
 * Reporte de las encuestas de satisfaccion que responden los clientes (link por WhatsApp,
 * QR de la mesa y QR de la factura): invoca EncuestaClienteCtrl.consultar.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarEncuestasClientes   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: exige sesion; el administrador de punto de venta solo ve su tienda.
 * Recibe (query/form): idtienda (0 = todas), fechainicial (aaaa-mm-dd), fechafinal (aaaa-mm-dd).
 * Devuelve: application/json; claves: resultado, totales, preguntas, semanas, tipos, dias,
 * palabras, detalle, tienda; o resultado = ERROR con mensaje.
 * Lee: encuesta_servicio, pregunta_servicio, log_encuesta_servicio, tienda.
 *
 * A diferencia de otros reportes, este exige sesion: trae los comentarios de los clientes.
 */
@WebServlet("/ConsultarEncuestasClientes")
public class ConsultarEncuestasClientes extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarEncuestasClientes() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			//Sin sesion no se entrega nada: son opiniones de clientes.
			final HttpSession sesion = request.getSession(false);
			if (sesion == null || !(sesion.getAttribute("usuario") instanceof Usuario)) {
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Sesion no valida. Vuelva a ingresar.\"}");
				return;
			}
			//Un administrador de punto de venta solo puede ver su propia tienda, pida lo que pida.
			final int idTienda = RestriccionTiendaCtrl.tiendaForzada(request, entero(request.getParameter("idtienda")));
			if (idTienda < 0) {
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Todavia no podemos saber de cual punto de venta es usted: "
						+ "no hay horas suyas registradas la semana pasada. Avisele a Gestion Humana o a Sistemas.\"}");
				return;
			}
			final String fechaInicial = texto(request.getParameter("fechainicial"));
			final String fechaFinal = texto(request.getParameter("fechafinal"));
			if (fechaInicial.length() == 0 || fechaFinal.length() == 0) {
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"fechainicial y fechafinal son obligatorias.\"}");
				return;
			}
			out.write(new EncuestaClienteCtrl().consultar(idTienda, fechaInicial, fechaFinal));
		} catch (final Exception e) {
			System.out.println("ConsultarEncuestasClientes: " + e.toString());
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Error consultando el reporte.\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private int entero(final String valor) {
		try {
			return (Integer.parseInt(valor == null ? "0" : valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}

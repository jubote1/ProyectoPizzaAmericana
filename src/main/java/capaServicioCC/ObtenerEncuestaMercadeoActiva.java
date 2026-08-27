package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EncuestaMercadeoCtrl;

/**
 * Servicio que retorna en formato JSON las preguntas de encuesta de mercadeo
 * vigentes el dia de hoy para el canal indicado, con sus opciones de respuesta.
 *
 * El POS lo invoca al finalizar un pedido. Ademas de traer la configuracion, esta
 * llamada le sirve como prueba de conectividad: si falla o retorna el arreglo de
 * preguntas vacio, el POS no muestra nada y el pedido sigue su curso normal.
 *
 * Parametros:
 *   espuntoventa  S para pedidos de punto de venta, N para domicilio. Por defecto S.
 *
 * Servlet implementation class ObtenerEncuestaMercadeoActiva
 */
@WebServlet("/ObtenerEncuestaMercadeoActiva")
public class ObtenerEncuestaMercadeoActiva extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ObtenerEncuestaMercadeoActiva() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final String parametroPV = request.getParameter("espuntoventa");
		// Por defecto se asume punto de venta, que es el unico canal habilitado hoy
		final boolean esPuntoVenta = parametroPV == null || !parametroPV.equalsIgnoreCase("N");
		final EncuestaMercadeoCtrl encCtrl = new EncuestaMercadeoCtrl();
		final String respuesta = encCtrl.obtenerPreguntasVigentes(esPuntoVenta);
		final PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

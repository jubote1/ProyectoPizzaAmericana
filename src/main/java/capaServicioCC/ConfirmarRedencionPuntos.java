package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.FidelizacionCtrl;

/**
 * Confirma una redencion que habia quedado RESERVADA.
 *
 * La llama el POS cuando el pedido quedo finalizado de verdad. Mientras la
 * redencion esta RESERVADA se considera provisional, y si el pedido nunca se
 * finaliza el proceso de barrido la reversa sola.
 *
 * Parametros: idredencion, usuario
 */
@WebServlet("/ConfirmarRedencionPuntos")
public class ConfirmarRedencionPuntos extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConfirmarRedencionPuntos() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		try {
			int idRedencion = 0;
			try {
				idRedencion = Integer.parseInt(request.getParameter("idredencion"));
			} catch (Exception e) {
				idRedencion = 0;
			}
			String usuario = request.getParameter("usuario");
			if (usuario == null) {
				usuario = "";
			}
			if (idRedencion <= 0) {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Falta el idredencion\"}");
				return;
			}
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			out.write(fidCtrl.confirmarRedencionPuntos(idRedencion, usuario));
		} catch (Exception e) {
			System.out.println("ConfirmarRedencionPuntos: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico confirmando la redencion\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

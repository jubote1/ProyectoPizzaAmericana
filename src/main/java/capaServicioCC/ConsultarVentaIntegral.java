package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.VentaIntegralCtrl;

/**
 * Servlet implementation class ConsultarVentaIntegral
 * Consulta el cierre semanal de Venta Integral (ya calculado por el proceso
 * de Servicios) para un rango de semanas y una tienda o todas. Recibe las
 * fechas en dd/mm/yyyy, igual que ConsultarVentasEmpresariales.
 */
@WebServlet("/ConsultarVentaIntegral")
public class ConsultarVentaIntegral extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConsultarVentaIntegral() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		String fechaInicial = request.getParameter("fechainicial");
		String fechaFinal = request.getParameter("fechafinal");
		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}
		String fechaInicialISO = fechaInicial.substring(6, 10) + "-" + fechaInicial.substring(3, 5) + "-"
				+ fechaInicial.substring(0, 2);
		String fechaFinalISO = fechaFinal.substring(6, 10) + "-" + fechaFinal.substring(3, 5) + "-"
				+ fechaFinal.substring(0, 2);
		logger.info("consultar venta integral idtienda " + idTienda + " entre " + fechaInicialISO + " y "
				+ fechaFinalISO);
		VentaIntegralCtrl ventaIntegralCtrl = new VentaIntegralCtrl();
		String respuesta = ventaIntegralCtrl.consultarResumenVentaIntegral(idTienda, fechaInicialISO, fechaFinalISO);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}

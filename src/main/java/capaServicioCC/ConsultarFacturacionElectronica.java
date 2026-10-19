package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.FacturacionElectronicaCtrl;

/**
 * El reporte mensual de facturacion electronica por tienda: facturado, notas credito y neto.
 *
 *   ConsultarFacturacionElectronica?mes=2026-09              -> JSON
 *   ConsultarFacturacionElectronica?mes=2026-09&formato=csv  -> CSV de todos los documentos del mes
 */
@WebServlet("/ConsultarFacturacionElectronica")
public class ConsultarFacturacionElectronica extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		final String mes = request.getParameter("mes");
		if ("csv".equalsIgnoreCase(request.getParameter("formato"))) {
			response.setContentType("text/csv; charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"facturacion_"
					+ (mes != null && mes.matches("^[0-9]{4}-[0-9]{2}$") ? mes : "mes") + ".csv\"");
			final PrintWriter out = response.getWriter();
			// La marca de orden de bytes para que Excel respete los acentos.
			out.write('﻿');
			out.write(FacturacionElectronicaCtrl.csv(mes));
			return;
		}
		response.setContentType("application/json; charset=UTF-8");
		final PrintWriter out = response.getWriter();
		out.write(FacturacionElectronicaCtrl.consultar(mes));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

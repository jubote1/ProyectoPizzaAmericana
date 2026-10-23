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
 * Servicio de consulta de Facturacion electronica: invoca FacturacionElectronicaCtrl.csv,
 * FacturacionElectronicaCtrl.consultar.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarFacturacionElectronica   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): mes (string), formato (string).
 * Devuelve: text/csv; charset=UTF-8; claves: base, cantidad, dias_con_datos, dias_del_mes, error,
 * factura, factura_de_otro_mes, facturas, facturas_no_aceptadas, fecha, fecha_factura, hora, ....
 * Lee: factura_electronica_generada, nota_credito_electronica_generada, pedido, replica_log,
 * tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarFacturacionElectronica.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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

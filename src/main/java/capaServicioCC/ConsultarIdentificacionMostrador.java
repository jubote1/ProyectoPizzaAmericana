package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.IdentificacionMostradorCtrl;

/**
 * Servicio de consulta de Reportes y estadisticas: invoca
 * IdentificacionMostradorCtrl.consultarIdentificacionMostrador.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarIdentificacionMostrador   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), idtienda (integer).
 * Devuelve: texto; claves: autpreguntadas, autsi, cajero, cajeros, filas, identificados, idtienda,
 * minimopedidoscajero, pedidos, porcentaje, senegaron, senego, ....
 * Lee: cliente, cliente_autorizacion_datos, information_schema.tables, pedido, pedido_sin_cliente,
 * tienda, tipo_pedido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarIdentificacionMostrador.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarIdentificacionMostrador")
public class ConsultarIdentificacionMostrador extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConsultarIdentificacionMostrador() {
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
		logger.info("identificacion mostrador idtienda " + idTienda + " entre " + fechaInicialISO + " y "
				+ fechaFinalISO);
		long arranco = System.currentTimeMillis();
		IdentificacionMostradorCtrl ctrl = new IdentificacionMostradorCtrl();
		String respuesta = ctrl.consultarIdentificacionMostrador(idTienda, fechaInicialISO, fechaFinalISO);
		logger.info("identificacion mostrador tardo " + (System.currentTimeMillis() - arranco) + " ms");
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}

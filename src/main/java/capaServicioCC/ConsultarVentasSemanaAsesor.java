package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;
/**
 * Servicio de consulta de Reportes y estadisticas: invoca PedidoCtrl.consultarVentasSemanaAsesor.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarVentasSemanaAsesor   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), asesor (string).
 * Devuelve: texto; claves: ventatotal.
 * Lee: pedido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarVentasSemanaAsesor.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarVentasSemanaAsesor")
public class ConsultarVentasSemanaAsesor extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarVentasSemanaAsesor() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio se encarga de recibir como par�metros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos par�metros, se invoca en la capa controlador al m�todo ConsultaIntegradaPedidos.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
        String fechaInicial, fechaFinal, asesor;
        fechaInicial = request.getParameter("fechainicial");
        fechaFinal = request.getParameter("fechafinal");
        asesor = request.getParameter("asesor");
        PedidoCtrl consultaVentas = new PedidoCtrl();
        String respuestas = consultaVentas.consultarVentasSemanaAsesor(fechaInicial,fechaFinal, asesor);
        PrintWriter out = response.getWriter();
        //Comentamos resultado de la consulta debido a que consultas grandes pueden generar mucha informaci�n
        //logger.debug(respuestaConsulta);
		out.write(respuestas);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

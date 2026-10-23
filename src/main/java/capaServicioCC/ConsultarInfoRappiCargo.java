package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Plataformas (Rappi, DiDi, Kuno): invoca
 * PedidoCtrl.consultarInfoRappiCargo.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarInfoRappiCargo   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedidotienda (integer), idtienda (integer).
 * Devuelve: application/json;charset=UTF-8; claves: fechacancelacion, fechaentrega, idpedido,
 * idpedidologistico, urlseguimiento.
 * Lee: pedido, tercerizado_domicilio_evento.
 * Sistemas externos: Rappi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarInfoRappiCargo.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarInfoRappiCargo")
public class ConsultarInfoRappiCargo extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarInfoRappiCargo() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * M�todo GET del Servicio que recibe como par�metroel idpedido del sistema Contact Center y el numPedidoPixel el cual es el 
	 * n�mero de pedido Pixel que retorno la inserci�n, se invoca en al capa controlador el m�todo actualizarEstadoNumeroPedidoPixel.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				response.setContentType("application/json;charset=UTF-8");
				request.setCharacterEncoding("UTF-8");
				int idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
				int idTienda = Integer.parseInt(request.getParameter("idtienda"));
				PedidoCtrl PedidoCtrl = new PedidoCtrl();
		        String respuesta = PedidoCtrl.consultarInfoRappiCargo(idPedidoTienda, idTienda);
		        System.out.println(respuesta);
		        PrintWriter out = response.getWriter();
				out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

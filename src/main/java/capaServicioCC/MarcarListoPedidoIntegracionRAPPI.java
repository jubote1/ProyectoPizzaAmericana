package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;

/**
 * Servicio de consulta de Plataformas (Rappi, DiDi, Kuno): invoca
 * PedidoCtrl.marcarListoPedidoIntegracionRAPPI.
 *
 * Ruta: /ProyectoPizzaAmericana/MarcarListoPedidoIntegracionRAPPI   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idordencomercio (long).
 * Devuelve: texto; claves: respuesta.
 * Lee: integracion_crm.
 * Sistemas externos: Rappi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha MarcarListoPedidoIntegracionRAPPI.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/MarcarListoPedidoIntegracionRAPPI")
public class MarcarListoPedidoIntegracionRAPPI extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MarcarListoPedidoIntegracionRAPPI() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocar� el m�todo de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		long idOrdenComercio = 0;
		try
		{
			idOrdenComercio = Long.parseLong(request.getParameter("idordencomercio"));
		}catch(Exception e)
		{
			idOrdenComercio = 0;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.marcarListoPedidoIntegracionRAPPI(idOrdenComercio);
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

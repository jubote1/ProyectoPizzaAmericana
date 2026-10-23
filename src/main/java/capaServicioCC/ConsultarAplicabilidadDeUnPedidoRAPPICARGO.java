package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;
/**
 * Dice si un pedido cumple las condiciones para entregarse con Rappi Cargo (domicilio tercerizado)
 * y si su pago virtual ya esta confirmado.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarAplicabilidadDeUnPedidoRAPPICARGO   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer), idtienda (integer), numposheader (integer).
 * Devuelve: application/json;charset=UTF-8; claves: direccionCliente, distanciaKm,
 * fechapagovirtual, formapago, idCliente, idformapago, latitudCliente, latitudTienda,
 * longitudCliente, longitudTienda, mensaje, nombreTienda, ....
 * Lee: cliente, forma_pago, parametros, pedido, pedido_forma_pago, producto, tienda.
 * Sistemas externos: Rappi.
 *
 * Trampas y riesgos conocidos:
 * - Si no encuentra el pedido, responde
 *   {"resultado":false,"validacionDistancia":false,"mensaje":"No se encontro informacion del
 *   pedido en el servidor principal."}.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarAplicabilidadDeUnPedidoRAPPICARGO.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarAplicabilidadDeUnPedidoRAPPICARGO")
public class ConsultarAplicabilidadDeUnPedidoRAPPICARGO extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarAplicabilidadDeUnPedidoRAPPICARGO() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio se encarga de recibir como par�metros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos par�metros, se invoca en la capa controlador al m�todo ConsultaIntegradaPedidos.
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Logger logger = Logger.getLogger("log_file");
        PedidoCtrl consultapedido = new PedidoCtrl();

		int idpedido = 0;
		int idtienda = 0;
		int numposheader = 0;

		try {
		    if (request.getParameter("idpedido") != null) {
		        idpedido = Integer.parseInt(request.getParameter("idpedido"));
		    }
		} catch (Exception e) {
		    idpedido = 0;
		}

		try {
		    if (request.getParameter("idtienda") != null) {
		    	idtienda = Integer.parseInt(request.getParameter("idtienda"));
		    }
		} catch (Exception e) {
			idtienda = 0;
		}
		
		try {
		    if (request.getParameter("numposheader") != null) {
		    	numposheader = Integer.parseInt(request.getParameter("numposheader"));
		    }
		} catch (Exception e) {
			numposheader = 0;
		}
		

        String respuestaConsulta =
                consultapedido.consultarAplicabilidadPedidoRAPPICARGO(idpedido, numposheader, idtienda);

        PrintWriter out = response.getWriter();
        out.write(respuestaConsulta);
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

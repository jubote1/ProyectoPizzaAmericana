package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.FidelizacionCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.CodigoRedencionPuntos;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class GetCliente
 * Servicio que se encarga de consultar todos los registros que tiene asociado un cliente en la tabla de clientes, dando 
 * como par�metro un tel�fono determinado.
 */
@WebServlet("/RealizarRedencionPuntos")
public class RealizarRedencionPuntos extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RealizarRedencionPuntos() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro del tel�fono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el tel�fono indicado.
	 * Lo anterior invocando el m�todo obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			String codigo = request.getParameter("codigo");
			String correo = request.getParameter("correo");
			int idTienda = 0;
			int idPedido = 0;
			try {
				idTienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				idTienda = 0;
			}
			try {
				idPedido = Integer.parseInt(request.getParameter("idpedido"));
			}catch(Exception e)
			{
				idPedido = 0;
			}
			double puntosRedimir = Double.parseDouble(request.getParameter("puntosredimir"));

			/*
			 * Trazabilidad de quien redime. Antes no quedaba registro y no se podia
			 * saber quien habia sacado el producto.
			 *
			 * Los dos canales se resuelven distinto a proposito:
			 *
			 * CC  El contact center llega con sesion HTTP iniciada, asi que el usuario
			 *     se toma de la sesion. Es la fuente confiable: un parametro se puede
			 *     alterar desde el navegador, la sesion no. Para una revision de
			 *     fraude esa diferencia importa.
			 * POS El punto de venta no tiene sesion, asi que lo envia como parametro.
			 *     Menos blindado, pero el riesgo real es un cajero usando la pantalla,
			 *     no alguien fabricando peticiones HTTP.
			 */
			String usuario = "";
			String origen = "";
			HttpSession sesion = request.getSession(false);
			if (sesion != null && sesion.getAttribute("usuario") != null) {
				Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
				usuario = usuarioSesion.getNombreUsuario();
				origen = "CC";
			} else {
				usuario = request.getParameter("usuario");
				if (usuario == null) {
					usuario = "";
				}
				origen = "POS";
			}

			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			/*
			 * reservar es opcional a proposito. El POS que esta hoy en produccion
			 * no lo envia, y sin el la redencion queda CONFIRMADA de inmediato,
			 * exactamente como se comportaba antes. Cuando se actualice el POS
			 * empezara a enviar reservar=S para que los puntos queden RESERVADA y
			 * solo se confirmen si el pedido llega a finalizarse. Asi el central
			 * puede salir hoy sin tener que esperar a las tiendas.
			 */
			boolean reservar = "S".equalsIgnoreCase(request.getParameter("reservar"));

			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			String respuesta = fidCtrl.realizarRedencionPuntos(codigo, correo, puntosRedimir, idTienda, idPedido,
					usuario, origen, reservar);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

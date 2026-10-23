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
 * Guarda en el cliente el codigo de miembro que le asigno SALESmanago o la tienda.
 *
 * Ruta: /ProyectoPizzaAmericana/ActualizarMemcode   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): membercode (integer), idcliente (integer).
 * Devuelve: application/json;charset=UTF-8; claves: resultado.
 * Escribe: cliente (UPDATE).
 *
 * Trampas y riesgos conocidos:
 * - Cualquiera puede cambiar el memcode de un cliente. Un memcode mayor que 0 con idcliente 0
 *   hace que InsertarClienteEncabezadoPedido no cree nada (ver grupo B).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ActualizarMemcode.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ActualizarMemcode")
public class ActualizarMemcode extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActualizarMemcode() {
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
				int membercode;
				int idcliente;
				try
				{
					membercode = Integer.parseInt(request.getParameter("membercode"));
				}catch(Exception e)
				{
					membercode = 0;
				}
				try
				{
					idcliente = Integer.parseInt(request.getParameter("idcliente"));
				}catch(Exception e)
				{
					idcliente = 0;
				}
				
				PedidoCtrl PedidoCtrl = new PedidoCtrl();
		        String respuesta = PedidoCtrl.actualizarMemcode(membercode, idcliente);
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

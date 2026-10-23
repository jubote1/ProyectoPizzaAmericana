package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import capaControladorCC.EmpleadoCtrl;
import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.EmpleadoVale;;

/**
 * Dice si un pedido ya tiene un producto dado.
 *
 * Ruta: /ProyectoPizzaAmericana/ValidarExistenciaProductoPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer), idproducto (integer).
 * Devuelve: texto; claves: respuesta.
 * Lee: detalle_pedido, pedido.
 *
 * Trampas y riesgos conocidos:
 * - Responde false tambien cuando falla la consulta o cuando los ids no son validos: no
 *   distingue "no lo tiene" de "no se pudo saber".
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ValidarExistenciaProductoPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ValidarExistenciaProductoPedido")
public class ValidarExistenciaProductoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ValidarExistenciaProductoPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como par�metro el iddetallepedido al cual se le realiz� la adicion, adicionalmente se 
	 * env�a el iddetallepedido relacionado a la adicion, la adici�n relacionada a la especialidad 1 o la especialidad 2 y 
	 * las cantidades. Lo anterior invocando al m�todo InsertarDetalleAdicion de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				String fecha = "";
				int idProducto = 0;
				int idPedido = 0;
		        try
		        {
		        	idPedido = Integer.parseInt(request.getParameter("idpedido"));
		        }catch(Exception e)
		        {
		        	idPedido = 0;
		        }
		        try
		        {
		        	idProducto = Integer.parseInt(request.getParameter("idproducto"));
		        }catch(Exception e)
		        {
		        	idProducto = 0;
		        }
		        PedidoCtrl pedCtrl = new PedidoCtrl();
		        String respuesta = pedCtrl.validarExistenciaProductoPedido(idPedido, idProducto);
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

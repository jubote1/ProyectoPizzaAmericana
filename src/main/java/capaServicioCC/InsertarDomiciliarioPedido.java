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
import capaModeloCC.DomiciliarioPedido;

/**
 * Servlet implementation class InsertarDespachoTienda
 */
@WebServlet("/InsertarDomiciliarioPedido")
/**
 * Servicio que modifica datos de Domiciliarios y enrutamiento: invoca
 * PedidoCtrl.insertarDomiciliarioPedido.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarDomiciliarioPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): fecha (string), idtienda (integer), idusuario (integer), cantidad
 * (integer).
 * Devuelve: application/json.
 * Escribe: domiciliario_pedido (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarDomiciliarioPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
public class InsertarDomiciliarioPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarDomiciliarioPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		Logger logger = Logger.getLogger("log_file");
		String fecha = request.getParameter("fecha");
        int idTienda;
        try
        {
        	idTienda = Integer.parseInt(request.getParameter("idtienda"));
        	
        }catch(Exception e)
        {
        	logger.error(e.toString());
        	idTienda = 0;
        }
        int idUsuario;
        try
        {
        	idUsuario = Integer.parseInt(request.getParameter("idusuario"));
        	
        }catch(Exception e)
        {
        	logger.error(e.toString());
        	idUsuario = 0;
        }
        int cantidad;
        try
        {
        	cantidad = Integer.parseInt(request.getParameter("cantidad"));
        	
        }catch(Exception e)
        {
        	logger.error(e.toString());
        	cantidad = 0;
        }
        PedidoCtrl pedCtrl = new PedidoCtrl();
        DomiciliarioPedido domPed = new DomiciliarioPedido(idUsuario,fecha, idTienda, cantidad);
        pedCtrl.insertarDomiciliarioPedido(domPed);
        String respuesta = "OK";
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

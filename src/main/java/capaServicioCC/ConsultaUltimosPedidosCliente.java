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
 * Lista los 3 ultimos pedidos de una direccion de cliente (idcliente) con tienda, valor, estado,
 * forma de pago y si ya llegaron a la tienda.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultaUltimosPedidosCliente   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idcliente (string).
 * Devuelve: texto; claves: cliente, direccion, enviadopixel, estadoenviotienda, estadopedido,
 * fechainsercion, formapago, idcliente, idformapago, idpedido, idtienda, numposheader, ....
 * Lee: cliente, estado_pedido, forma_pago, pedido, pedido_forma_pago, tienda.
 *
 * Trampas y riesgos conocidos:
 * - Expone la URL interna de la tienda y la cadena stringpixel completa del pedido.
 * - Devuelve como maximo 3 pedidos (order by fechapedido desc limit 3): ordena por fecha, no por
 *   hora, y si dos pedidos son del mismo dia no hay un criterio estable.
 * - Une con pedido_forma_pago: un pedido que todavia no tiene forma de pago registrada no
 *   aparece.
 * - Mira una sola direccion (idcliente), no todos los pedidos del telefono.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultaUltimosPedidosCliente.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultaUltimosPedidosCliente")
public class ConsultaUltimosPedidosCliente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultaUltimosPedidosCliente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio se encarga de recibir como parámetros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos parámetros, se invoca en la capa controlador al método ConsultaIntegradaPedidos.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		String strIdCliente = request.getParameter("idcliente");
		int idCliente = 0;
        try
        {
        	idCliente = Integer.parseInt(strIdCliente);
        }catch(Exception e)
        {
        	idCliente = 0;
        }
        logger.info("Llamado a servicio con parámetros idCliente " + idCliente);
        PedidoCtrl consultapedido = new PedidoCtrl();
        String respuestaConsulta = consultapedido.ConsultaUltimosPedidosCliente(idCliente);
        PrintWriter out = response.getWriter();
        //Comentamos resultado de la consulta debido a que consultas grandes pueden generar mucha información
        //logger.debug(respuestaConsulta);
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

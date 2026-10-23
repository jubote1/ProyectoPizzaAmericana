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
 * Servicio de consulta de Plataformas (Rappi, DiDi, Kuno): invoca
 * PedidoCtrl.ConsultaIntegradaPedidosDomicom.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultaIntegradaPedidosDomicom   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), tienda (string), numeropedido
 * (integer), estado (integer), estadotienda (integer), idplataforma (integer), integracion
 * (integer).
 * Devuelve: texto; claves: aceptadorappi, cliente, direccion, enviadopixel, estadoenviotienda,
 * estadopedido, fechafinalizacion, fechainsercion, fechapagovirtual, fechapedido, formapago,
 * idcliente, ....
 * Lee: cliente, estado_pedido, forma_pago, marcacion_pedido, pedido, pedido_forma_pago, tienda.
 * Sistemas externos: Rappi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultaIntegradaPedidosDomicom.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultaIntegradaPedidosDomicom")
public class ConsultaIntegradaPedidosDomicom extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultaIntegradaPedidosDomicom() {
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
		String fechainicial = request.getParameter("fechainicial");
        String fechafinal = request.getParameter("fechafinal");
        String tienda = request.getParameter("tienda");
        int idEstadoPedido = 0;
        int enviadoPixel = 0;
        int numeropedido;
        int idPlataforma = 0;
        int integracion = 0;
        try
        {
        	numeropedido = Integer.parseInt(request.getParameter("numeropedido"));
        }catch(Exception e)
        {
        	numeropedido= 0;
        }
        try
        {
        	idEstadoPedido = Integer.parseInt(request.getParameter("estado"));
        }catch(Exception e)
        {
        	idEstadoPedido = 0;
        }
        try
        {
        	enviadoPixel = Integer.parseInt(request.getParameter("estadotienda"));
        }catch(Exception e)
        {
        	enviadoPixel = -1;
        }
        try
        {
        	idPlataforma = Integer.parseInt(request.getParameter("idplataforma"));
        }catch(Exception e)
        {
        	idPlataforma = 0;
        }
        try
        {
        	integracion = Integer.parseInt(request.getParameter("integracion"));
        }catch(Exception e)
        {
        	integracion = 0;
        }
        logger.info("Llamado a servicio con parámetros fechainicial " + fechainicial + " fechafinal " + fechafinal + " tienda "+ tienda + "numeropedido " + numeropedido);
        PedidoCtrl consultapedido = new PedidoCtrl();
        String respuestaConsulta = consultapedido.ConsultaIntegradaPedidosDomicom(fechainicial, fechafinal, tienda, numeropedido, idEstadoPedido, enviadoPixel, idPlataforma, integracion);
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

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
 * Devuelve los renglones de un pedido ya creado, con nombres legibles.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDetallePedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): numeropedido (integer).
 * Devuelve: texto; claves: adicion, cantidad, especialidad1, especialidad2, excepcion,
 * iddetallepedido, liquido, modespecialidad1, modespecialidad2, nombreproducto, observacion,
 * valortotal, ....
 * Lee: detalle_pedido, especialidad, excepcion_precio, sabor_x_tipo_liquido.
 *
 * Trampas y riesgos conocidos:
 * - Devuelve las adiciones, los productos incluidos y las bebidas como renglones sueltos, sin
 *   indicar a que pizza pertenecen (no trae idproducto, ni tipo, ni el vinculo con el padre).
 * - Cualquiera que conozca un numero de pedido puede ver su contenido (sin sesion).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDetallePedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDetallePedido")
public class ConsultarDetallePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarDetallePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro un numeropedido y con base en este retorna la información, de un pedido.
	 * Se invoca en la capa controlador el método  ConsultarDetallePedido, el cual retorna en formato JSON la información.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		int numeropedido;
        try
        {
        	numeropedido = Integer.parseInt(request.getParameter("numeropedido"));
        }catch(Exception e)
        {
        	numeropedido= 0;
        }
        logger.info("Llamado a servicio con parámetros numeropedido " + numeropedido);
        PedidoCtrl consultapedido = new PedidoCtrl();
        String respuestaConsulta = consultapedido.ConsultarDetallePedido(numeropedido);
        logger.debug(respuestaConsulta);
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

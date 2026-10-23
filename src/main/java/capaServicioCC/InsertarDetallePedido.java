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
 * Inserta un renglon en el pedido: una pizza, una gaseosa, una adicion, un producto incluido o
 * cualquier otro producto.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarDetallePedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idproducto (integer), idpedido (integer), especialidad1 (string),
 * especialidad2 (string), cantidad (number), valorunitario (number), valortotal (number), adicion
 * (string), observacion (string), modespecialidad1 (string), modespecialidad2 (string),
 * idsabortipoliquido (integer), idexcepcion (integer).
 * Devuelve: texto; claves: iddetallepedido.
 * Escribe: detalle_pedido (INSERT).
 *
 * Trampas y riesgos conocidos:
 * - SEGURIDAD y dinero: quien llame puede fijar el precio que quiera (por ejemplo 0) y la
 *   promocion que quiera; el servidor lo graba.
 * - Los numeros obligatorios se leen sin try/catch: si falta uno, el servlet lanza la excepcion
 *   y Tomcat responde HTTP 500.
 * - adicion y observacion son texto libre concatenado en el SQL: una comilla simple en la
 *   observacion rompe la insercion (devuelve 0) y abre la puerta a inyeccion.
 * - No es idempotente: repetir la llamada duplica el renglon.
 * - Se puede agregar a cualquier idpedido, incluso de un pedido ya finalizado o ajeno.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarDetallePedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarDetallePedido")
public class InsertarDetallePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarDetallePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetros el idproducto qeu se va a agregar, el idpedido al cual estará relacionado
	 * el detalle de pedido, se ingresa la información del detalle pedido, como las especialidades, la cantidad, observaciones, excepciones de precio etc.
	 * 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idproducto = Integer.parseInt(request.getParameter("idproducto"));
        int idpedido = Integer.parseInt(request.getParameter("idpedido"));
        String  especialidad1;
        String  especialidad2;
        String modespecialidad1;
        String modespecialidad2;
        especialidad1 = request.getParameter("especialidad1");
        especialidad2 = request.getParameter("especialidad2");
        double cantidad =  Double.parseDouble(request.getParameter("cantidad"));
        double valorUnitario = Double.parseDouble(request.getParameter("valorunitario"));
        double valorTotal = Double.parseDouble(request.getParameter("valortotal"));
        String adicion = request.getParameter("adicion");
        String observacion = request.getParameter("observacion");
        modespecialidad1 = request.getParameter("modespecialidad1");
        modespecialidad2 = request.getParameter("modespecialidad2");
        int idsabortipoliquido;
        try
        {
        	idsabortipoliquido = Integer.parseInt(request.getParameter("idsabortipoliquido"));
        }catch(Exception e)
        {
        	idsabortipoliquido = 0;
        }
        
        int idexcepcion;
        try
        {
        	idexcepcion = Integer.parseInt(request.getParameter("idexcepcion"));
        }catch(Exception e)
        {
        	idexcepcion = 0;
        }
        PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.InsertarDetallePedido(idproducto, idpedido,cantidad, especialidad1, especialidad2, valorUnitario, valorTotal,adicion, observacion, idsabortipoliquido, idexcepcion, modespecialidad1, modespecialidad2);
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

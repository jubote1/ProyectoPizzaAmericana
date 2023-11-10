package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;
import capaControladorCC.TiendaCtrl;

/**
 * Servlet implementation class ConsultarBloqueosTienda
 * Servicio que implementa los métodos para soportar el CRUD de la entidad Tienda.
 */
@WebServlet("/ConsultarPedidosPendientesRAPPI")
public class ConsultarPedidosPendientesRAPPI extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarPedidosPendientesRAPPI() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibe como parámetro principal el idoperacion con base en lo siguiente: 
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la operación seleccionada se 
	 * solicitarán ciertos parámetros y posteriormente se invocará al método correspondiente en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		//Operación idoperacion 1 insertar 3 Eliminar  4 Consultar
		HttpSession sesion = request.getSession();
		response.addHeader("Access-Control-Allow-Origin", "*");
		String respuesta="";
		PedidoCtrl pedCtrl = new PedidoCtrl();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String fechaActual = "";
		Date datFechaActual = new Date();
		fechaActual = dateFormat.format(datFechaActual);
		respuesta = pedCtrl.consultarPedidosPendientesRAPPI(fechaActual);
		//System.out.println(respuesta);
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

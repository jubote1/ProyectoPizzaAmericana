package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.EncuestaCtrl;
import capaModeloCC.EmpleadoEncuesta;


/**
 * Servlet implementation class FinalizarPedido
 * Servicio que se encarga de cerrar un pedido, totalizar el valor del pedido,y cambiar el estado del pedido a finalizado.
 * 
 */
@WebServlet("/InsertarEncabezadoEncuestaTiendasAPP")
public class InsertarEncabezadoEncuestaTiendasAPP extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarEncabezadoEncuestaTiendasAPP() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocar� el m�todo de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		int  idTienda = 0;
		try
		{
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		}catch(Exception e)
		{
			idTienda = 1;
		}
		int idempleado = Integer.parseInt(request.getParameter("idempleado"));
		int idencuesta = Integer.parseInt(request.getParameter("idencuesta"));
		EmpleadoEncuesta empEncuesta = new EmpleadoEncuesta(0,idempleado,0,idencuesta,"",idTienda);
		EncuestaCtrl encCtrl = new EncuestaCtrl();
		String respuesta = encCtrl.insertarEmpleadoEncuesta(empEncuesta);
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

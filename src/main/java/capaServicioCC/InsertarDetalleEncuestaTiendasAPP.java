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
import capaModeloCC.EmpleadoEncuestaDetalle;


/**
 * Servlet implementation class FinalizarPedido
 * Servicio que se encarga de cerrar un pedido, totalizar el valor del pedido,y cambiar el estado del pedido a finalizado.
 * 
 */
@WebServlet("/InsertarDetalleEncuestaTiendasAPP")
public class InsertarDetalleEncuestaTiendasAPP extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarDetalleEncuestaTiendasAPP() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocará el método de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		int  idEmpleadoEncuesta = 0;
		try
		{
			idEmpleadoEncuesta = Integer.parseInt(request.getParameter("idempleadoencuesta"));
		}catch(Exception e)
		{
			idEmpleadoEncuesta = 1;
		}
		int  idEncuestaDetalle = 0;
		try
		{
			idEncuestaDetalle = Integer.parseInt(request.getParameter("idencuestadetalle"));
		}catch(Exception e)
		{
			idEncuestaDetalle = 1;
		}
		
		String valorSeleccionado = request.getParameter("valor");
		String observacionAdicional = request.getParameter("observacionadi");
		EmpleadoEncuestaDetalle empEncuestaDetalle = new EmpleadoEncuestaDetalle(0,idEmpleadoEncuesta,idEncuestaDetalle,"","",  valorSeleccionado);
		empEncuestaDetalle.setObservacionAdicional(observacionAdicional);
		EncuestaCtrl encCtrl = new EncuestaCtrl();
		String respuesta = encCtrl.insertarEmpleadoEncuestaDetalle(empEncuestaDetalle);
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

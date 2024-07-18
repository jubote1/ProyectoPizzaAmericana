package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;

/**
 * Servlet implementation class InsertarDetallePedido
 * Servicio que es consumido en la inserción del pedido, cada vez que se agrega un detalle al pedido, para lo cual recibe 
 * la información clave para la inserción.
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
		HttpSession sesion = request.getSession();
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

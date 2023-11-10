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
 * Servlet implementation class FinalizarPedido
 * Servicio que se encarga de cerrar un pedido, totalizar el valor del pedido,y cambiar el estado del pedido a finalizado.
 * 
 */
@WebServlet("/FinalizarPedido")
public class FinalizarPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FinalizarPedido() {
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
		int idpedido = Integer.parseInt(request.getParameter("idpedido"));
		int idformapago;
		int idcliente;
		int insertado;
		double valorformapago;
		double valortotal;
		double tiempopedido;
		double descuento = 0;
		String validaDir;
		String motivoDescuento = "";
		String programado = request.getParameter("programado");
		if(programado == null)
		{
			programado = "";
		}
        String esProgramado = "N";
        if(programado.equals(new String("AHORA")))
        {
        	esProgramado = "N";
        }else if(programado.equals(new String("")))
        {
        	esProgramado = "N";
        }else
        {
        	esProgramado = "S";
        }
		validaDir = request.getParameter("validadir");
		motivoDescuento = request.getParameter("motivodescuento");
		try
		{
			idformapago = Integer.parseInt(request.getParameter("idformapago"));
		}catch(Exception e)
		{
			idformapago = 0;
		}
		try{
			valorformapago = Double.parseDouble(request.getParameter("valorformapago"));
		}catch(Exception e)
		{
			valorformapago = 0;
		}
		try{
			valortotal = Double.parseDouble(request.getParameter("valortotal"));
		}catch(Exception e)
		{
			valortotal = 0;
		}
		try{
			idcliente = Integer.parseInt(request.getParameter("idcliente"));
		}catch(Exception e)
		{
			idcliente = 0;
		}
		try{
			insertado = Integer.parseInt(request.getParameter("insertado"));
		}catch(Exception e)
		{
			insertado = 0;
		}
		try{
			tiempopedido = Double.parseDouble(request.getParameter("tiempopedido"));
		}catch(Exception e)
		{
			tiempopedido = 0;
		}
		try{
			descuento = Double.parseDouble(request.getParameter("descuento"));
		}catch(Exception e)
		{
			descuento = 0;
		}
		//Validamos si es tiendaKuno
		String tiendaKuno = request.getParameter("tiendakuno");
		if(tiendaKuno == null)
		{
			tiendaKuno = "";
		}
		if(tiendaKuno.equals(new String("S")))
		{
			validaDir = "N";
		}
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.FinalizarPedido(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, tiempopedido, validaDir, descuento, motivoDescuento, esProgramado, programado);
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

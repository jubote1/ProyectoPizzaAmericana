package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class InsertarEncabezadoPedido
 * Servicio que se encarga de insertar el encabezado de un pedido, se retorna en formato JSON el dato esperado que 
 * corresponde al idpedido.
 */
@WebServlet("/InsertarEncabezadoPedido")
public class InsertarEncabezadoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarEncabezadoPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro la información más básica de un pedido como lo son, tinda, idcliente, 
	 * fechapedido, con estos tres hace una inserción del encabezado pedido y retorna en formato JSON el idpedido que se
	 * manejará para el pedido. Esta labor se realiza invocando el método InsertarEncabezadoPedido de la capa Pedido
	 * Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		//Capturamos la información de la sesion del usuario que está ejecutando la inserción
		Usuario usuario = (Usuario) sesion.getAttribute("usuario");
		String resultado ="" ;
		String user = "" ;
		//Al no existir el usuario logueado es posible que produza una excepcion
		try
		{
			user = usuario.getNombreUsuario();
			
			
		}catch(Exception e)
		{
			
			
		}
		String tienda = request.getParameter("tienda");
        int idcliente = Integer.parseInt(request.getParameter("idcliente"));
        String fechaPedido = request.getParameter("fechapedido");
        PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String programado = request.getParameter("programado");
        String esProgramado = "N";
        if(programado.equals(new String("AHORA")))
        {
        	esProgramado = "N";
        }else if(programado == null || programado.equals(new String("")))
        {
        	esProgramado = "N";
        }else
        {
        	esProgramado = "S";
        }
        int idTipoPedido = 1;
        try
        {
        	idTipoPedido = Integer.parseInt(request.getParameter("idtipopedido"));
        }catch(Exception e)
        {
        	idTipoPedido = 1;
        }
        String respuesta = PedidoCtrl.InsertarEncabezadoPedido(tienda,idcliente, fechaPedido, user, esProgramado, programado, idTipoPedido);
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

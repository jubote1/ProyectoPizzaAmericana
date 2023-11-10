package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.AutenticacionCtrl;
import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class InsertarClienteEncabezadoPedido
 * Este servicio se encarga de pasar como parámetro todo los valores del cliente y con base en estos el sistema
 * o actualiza o crea un nuevo cliente, esta labor se realiza en la inserción del encabezado de un pedido
 */
@WebServlet("/InsertarClienteEncabezadoPedido")
public class InsertarClienteEncabezadoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarClienteEncabezadoPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro todos los valores de un cliente, telefono, nombres, apellidos etc, con estos 
	 * valores y mediante la invocación al método InsertarClientePedidoEncabezado de la capa Cliente Controlador, se
	 * realiza la actualización o inserción del cliente. 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
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
		//Final de captura del usuario
		String telefono = request.getParameter("telefono");
        String nombres = request.getParameter("nombres");
        String apellidos = request.getParameter("apellidos");
        String nombreCompania = request.getParameter("nombreCompania");
        String direccion = request.getParameter("direccion");
        String zona = request.getParameter("zona");
        String observacion = request.getParameter("observacion");
        String tienda = request.getParameter("tienda");
        String municipio = request.getParameter("municipio");
        String fechaPedido = request.getParameter("fechapedido");
        int idnomenclatura = 0;
        String numNomenclatura = request.getParameter("numnomenclatura1");
        String numNomenclatura2 = request.getParameter("numnomenclatura2");
        String num3 = request.getParameter("num3");
        String telefonoCelular = request.getParameter("telefonocelular");
        String email = request.getParameter("email");
        String politicaDatos = request.getParameter("politicadatos");
        String programado = request.getParameter("programado");
        String fechaNacimiento = request.getParameter("fechanacimiento");
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
        float latitud;
        float longitud;
        double distanciaTienda;
        int idCliente;
        int memcode;
        try{
        	latitud = Float.parseFloat(request.getParameter("latitud"));
        }catch(Exception e)
        {
        	latitud = 0;
        }
        try{
        	longitud = Float.parseFloat(request.getParameter("longitud"));
        }catch(Exception e)
        {
        	longitud = 0;
        }
        try{
        	
        	distanciaTienda = Double.parseDouble(request.getParameter("distanciatienda"));
        }catch(Exception e)
        {
        	distanciaTienda = 0;
        }
        try{
        	memcode = Integer.parseInt(request.getParameter("memcode"));
        }catch(Exception e)
        {
        	memcode = 0;
        }
        try{
        	idCliente = Integer.parseInt(request.getParameter("idcliente"));
        }catch(Exception e)
        {
        	idCliente = 0;
        }
        try{
        	idnomenclatura = Integer.parseInt(request.getParameter("idnomenclatura"));
        }catch(Exception e)
        {
        	idnomenclatura = 0;
        }
        int idTipoPedido = 1;
        try
        {
        	idTipoPedido = Integer.parseInt(request.getParameter("idtipopedido"));
        }catch(Exception e)
        {
        	idTipoPedido = 1;
        }
        ClienteCtrl ClienCtrl = new ClienteCtrl();
        int idcliente = ClienCtrl.InsertarClientePedidoEncabezado(idCliente,telefono, nombres, apellidos, nombreCompania,  direccion, municipio, latitud, longitud, distanciaTienda, zona, observacion, tienda, memcode, idnomenclatura, numNomenclatura, numNomenclatura2, num3, telefonoCelular, email, politicaDatos, fechaNacimiento);
        String respuesta = "";
        PedidoCtrl PedidoCtrl = new PedidoCtrl();
	    respuesta = PedidoCtrl.InsertarEncabezadoPedido(tienda,idcliente,fechaPedido, user, esProgramado, programado, idTipoPedido);
        
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

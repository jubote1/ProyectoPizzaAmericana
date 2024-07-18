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

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class ActualizarCliente
 */
@WebServlet("/ActualizarCliente")
public class ActualizarCliente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActualizarCliente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession();
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
        String telefonoCelular = request.getParameter("telefonocelular");
        String email = request.getParameter("email");
        String clienteSinIden = request.getParameter("clientesiniden");
        String emailFact = request.getParameter("emailfact");
        int idTipoPersona = 0;
        String identificacion = request.getParameter("identificacion");
        String politicaDatos = request.getParameter("politicadatos");
        float latitud;
        float longitud;
        double distanciaTienda;
        int idCliente;
        int memcode;
        int idnomenclatura = 0;
        String numNomenclatura = request.getParameter("numnomenclatura1");
        String numNomenclatura2 = request.getParameter("numnomenclatura2");
        String num3 = request.getParameter("num3");
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
        try
        {
        	idTipoPersona = Integer.parseInt(request.getParameter("idtipopersona"));
        }catch(Exception e)
        {
        	idTipoPersona = 1;
        }
        String fechaNacimiento = request.getParameter("fechanacimiento");
        ClienteCtrl ClienCtrl = new ClienteCtrl();
        String respuesta  = ClienCtrl.InsertarClientePedidoEncabezadoJSON(idCliente,telefono, nombres, apellidos, nombreCompania,  direccion, municipio, latitud, longitud, distanciaTienda, zona, observacion, tienda, memcode, idnomenclatura, numNomenclatura, numNomenclatura2, num3, telefonoCelular, email, politicaDatos,fechaNacimiento, clienteSinIden, emailFact, idTipoPersona, identificacion);
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

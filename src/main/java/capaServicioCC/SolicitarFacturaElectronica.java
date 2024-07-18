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
import capaModeloCC.SolicitudFactura;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class ActualizarCliente
 */
@WebServlet("/SolicitarFacturaElectronica")
public class SolicitarFacturaElectronica extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SolicitarFacturaElectronica() {
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
		int idPedidoContact;
		int idPedidoTienda;
		double valor = 0;
		String nit = request.getParameter("nit");
		String correo = request.getParameter("correo");
		String empresa = request.getParameter("empresa");
		if(empresa.length() > 150)
		{
			empresa = empresa.substring(0,150);
		}
		String telefono = request.getParameter("telefono");
		String fechaPedido = request.getParameter("fechapedido");
        try{
        	idPedidoContact = Integer.parseInt(request.getParameter("idpedidocontact"));
        }catch(Exception e)
        {
        	idPedidoContact = 0;
        }
        try{
        	idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
        }catch(Exception e)
        {
        	idPedidoTienda = 0;
        }
        try{
        	valor = Double.parseDouble(request.getParameter("valor"));
        }catch(Exception e)
        {
        	valor = 0;
        }
        String usuario = request.getParameter("usuario");
        if(usuario == null)
        {
        	usuario = "";
        }
        SolicitudFactura solFactura = new SolicitudFactura(0,idPedidoContact, idPedidoTienda, valor, nit, correo, empresa, telefono, fechaPedido,"","", usuario);
        PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta  = pedCtrl.insertarSolicitudFactura(solFactura);
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

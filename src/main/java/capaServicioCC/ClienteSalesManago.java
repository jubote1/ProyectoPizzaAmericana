package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.Cliente;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca Cliente.setNombres, Cliente.setApellidos.
 *
 * Ruta: /ProyectoPizzaAmericana/ClienteSalesManago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): nombres (string), apellidos (string), direccion (string), telefonocelular
 * (string), email (string), isAdd (boolean).
 * Devuelve: application/json;charset=UTF-8; claves: address, apiKey, clientId, contact, country,
 * doubleOptInLanguage, email, externalId, forceOptIn, idcontacto, mensaje, name, ....
 * Lee: integracion_crm.
 * Sistemas externos: SALESmanago.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ClienteSalesManago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ClienteSalesManago")
public class ClienteSalesManago extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
     * @see HttpServlet#HttpServlet()
     */
    public ClienteSalesManago() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
		//	HttpSession sesion = request.getSession();
			request.setCharacterEncoding("UTF-8");
	        String nombres = request.getParameter("nombres");
	        String apellidos = request.getParameter("apellidos");
	        String direccion = request.getParameter("direccion");
	        String telefonoCelular = request.getParameter("telefonocelular");
	        String email = request.getParameter("email");
	        boolean isAdd = Boolean.parseBoolean(request.getParameter("isAdd"));
        
	        Cliente cliente = new Cliente();
	        cliente.setNombres(nombres);
	        cliente.setApellidos(apellidos);
	        cliente.setDireccion(direccion);
	        cliente.setTelefonoCelular(telefonoCelular);
	        cliente.setEmail(email);
	        
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json;charset=UTF-8");
			PedidoCtrl pedCtrl = new PedidoCtrl();
			String respuesta = pedCtrl.ClienteSalesManago(cliente,isAdd);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

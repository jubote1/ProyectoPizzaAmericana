package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PromocionesCtrl;
import capaModeloCC.Oferta;
import capaModeloCC.OfertaCliente;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class CRUDExcepcionPrecio
 * Servicio que implementa los servicios que responden al CRUD de la entidad Excepcion Precio.
 */
@WebServlet("/CRUDOfertaCliente")
public class CRUDOfertaCliente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDOfertaCliente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se implementa el CRUD para la entidad Excepción Precio, se recibe como parámetro principal el idoperacion
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, con base en el idoperacion se pediran el resto de parámetros.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 Actualizar uso promoción 3 Eliminar  4 Consultar promociones cliente 6 Consultar promociones cliente vigentes
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		response.addHeader("Access-Control-Allow-Origin", "*");
		String idoperacion = request.getParameter("idoperacion");
		PromocionesCtrl PromoCtrl = new PromocionesCtrl();
		System.out.println("estoy en servicio de ofertas clientes");
		int operacion;
		String respuesta="";
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		if (operacion ==1)
		{
			int idOferta = 0;
			try{
				idOferta = Integer.parseInt(request.getParameter("idoferta"));
			}catch(Exception e){
				idOferta = 0;
			}
			int idCliente = 0;
			try{
				idCliente = Integer.parseInt(request.getParameter("idcliente"));
			}catch(Exception e){
				idCliente = 0;
			}
			int PQRS = 0;
			try{
				PQRS = Integer.parseInt(request.getParameter("pqrs"));
			}catch(Exception e){
				PQRS = 0;
			}
			String observacion = request.getParameter("observacion");
			//Capturamos la información de la sesion del usuario que está ejecutando la inserción
			Usuario usuario = (Usuario) sesion.getAttribute("usuario");
			String usuarioIngreso = usuario.getNombreUsuario();
			OfertaCliente ofer = new OfertaCliente(0,idOferta, idCliente, "", PQRS, "", "", observacion, usuarioIngreso);
			respuesta = PromoCtrl.insertarOfertaCliente(ofer);
		}else if (operacion ==2)
		{
			int idOfertaCliente = Integer.parseInt(request.getParameter("idofertacliente"));
			//Capturamos la información de la sesion del usuario que está ejecutando la inserción
			Usuario usuario = (Usuario) sesion.getAttribute("usuario");
			String usuarioUso = usuario.getNombreUsuario();
			respuesta = PromoCtrl.actualizarUsoOferta(idOfertaCliente, usuarioUso, 0, 0);
		}else if (operacion ==3 )
		{
			int idOfertaClienteEli = Integer.parseInt(request.getParameter("idofertacliente"));
			respuesta = PromoCtrl.eliminarOfertaCliente(idOfertaClienteEli);
		}else if (operacion == 4)
		{
			int idOfertaClienteCon = Integer.parseInt(request.getParameter("idofertacliente"));
			respuesta = PromoCtrl.retornarOfertaCliente(idOfertaClienteCon);
		}else if(operacion == 5)
		{
			int idCliente = 0;
			try{
				idCliente = Integer.parseInt(request.getParameter("idcliente"));
			}catch(Exception e){
				idCliente = 0;
			}
			respuesta = PromoCtrl.obtenerOfertasClienteGrid(idCliente);
		}else if(operacion == 6)
		{
			int idCliente = 0;
			try{
				idCliente = Integer.parseInt(request.getParameter("idcliente"));
			}catch(Exception e){
				idCliente = 0;
			}
			respuesta = PromoCtrl.obtenerOfertasVigenteCliente(idCliente);
		}
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

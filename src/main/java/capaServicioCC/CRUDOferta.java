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

/**
 * Servlet implementation class CRUDExcepcionPrecio
 * Servicio que implementa los servicios que responden al CRUD de la entidad Excepcion Precio.
 */
@WebServlet("/CRUDOferta")
public class CRUDOferta extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDOferta() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se implementa el CRUD para la entidad Excepción Precio, se recibe como parámetro principal el idoperacion
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, con base en el idoperacion se pediran el resto de parámetros.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		response.addHeader("Access-Control-Allow-Origin", "*");
		String idoperacion = request.getParameter("idoperacion");
		PromocionesCtrl PromoCtrl = new PromocionesCtrl();
		int operacion;
		String respuesta="";
		System.out.println("operacion " + idoperacion) ;
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		if (operacion ==1)
		{
			String nombreOferta = request.getParameter("nombreoferta");
			int idExcepcion = 0;
			try{
				idExcepcion = Integer.parseInt(request.getParameter("idexcepcion"));
			}catch(Exception e){
				idExcepcion = 0;
			}
			Oferta ofer = new Oferta(0,nombreOferta, idExcepcion);
			respuesta = PromoCtrl.insertarOferta(ofer);
		}else if (operacion ==2)
		{
			int idOferta = Integer.parseInt(request.getParameter("idoferta"));
			String nombreOferta = request.getParameter("nombreoferta");
			int idExcepcion = 0;
			try{
				idExcepcion = Integer.parseInt(request.getParameter("idexcepcion"));
			}catch(Exception e){
				idExcepcion = 0;
			}
			Oferta ofer = new Oferta(idOferta,nombreOferta, idExcepcion);
			respuesta = PromoCtrl.editarOferta(ofer);
		}else if (operacion ==3 )
		{
			int idOfertaEli = Integer.parseInt(request.getParameter("idoferta"));
			respuesta = PromoCtrl.eliminarOferta(idOfertaEli);
		}else if (operacion == 4)
		{
			int idOfertaCon = Integer.parseInt(request.getParameter("idoferta"));
			respuesta = PromoCtrl.retornarOferta(idOfertaCon);
		}else if(operacion == 5)
		{
			respuesta = PromoCtrl.obtenerOfertasGrid();
		}else if(operacion == 6)
		{
			respuesta = PromoCtrl.obtenerOfertasGrid();
		}else if(operacion == 7)
		{
			respuesta = PromoCtrl.obtenerOfertasGridContact();
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

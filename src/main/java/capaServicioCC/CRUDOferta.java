package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
			Oferta ofer = CRUDOferta.leerOferta(request, 0);
			respuesta = PromoCtrl.insertarOferta(ofer);
		}else if (operacion ==2)
		{
			int idOferta = 0;
			try{
				idOferta = Integer.parseInt(request.getParameter("idoferta"));
			}catch(Exception e){
				idOferta = 0;
			}
			Oferta ofer = CRUDOferta.leerOferta(request, idOferta);
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
		}else if(operacion == 8)
		{
			//La pantalla de administracion: TODAS las ofertas, tambien las
			//deshabilitadas, porque para volver a habilitar una hay que verla.
			respuesta = PromoCtrl.obtenerOfertasAdministracion();
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


	/**
	 * Arma la oferta con TODO lo que mando la pantalla.
	 *
	 * Antes el servlet solo leia el nombre y la excepcion de precio; los otros
	 * veintiun parametros de la tabla oferta tocaba ponerlos a mano en la base de
	 * datos. Este metodo los lee todos, y lo usan por igual el crear y el editar
	 * para que no se puedan desincronizar.
	 *
	 * Lo que no venga se queda en el valor por defecto que el DAO le ponga; no se
	 * inventa nada aca.
	 */
	private static Oferta leerOferta(HttpServletRequest request, int idOferta)
	{
		Oferta ofer = new Oferta(idOferta, CRUDOferta.texto(request, "nombreoferta"),
				CRUDOferta.entero(request, "idexcepcion"));
		ofer.setCodigoPromocional(CRUDOferta.texto(request, "codigopromocional"));
		ofer.setDescuentoFijoPorcentaje(CRUDOferta.decimal(request, "descuentofijoporcentaje"));
		ofer.setDescuentoPorcentajeFuturo(CRUDOferta.decimal(request, "descuentoporcentajefuturo"));
		ofer.setDescuentoFijoValor(CRUDOferta.decimal(request, "descuentofijovalor"));
		ofer.setMensaje1(CRUDOferta.texto(request, "mensaje1"));
		ofer.setMensaje2(CRUDOferta.texto(request, "mensaje2"));
		ofer.setDiasCaducidad(CRUDOferta.entero(request, "diascaducidad"));
		ofer.setTipoCaducidad(CRUDOferta.texto(request, "tipocaducidad"));
		ofer.setControlaHora(CRUDOferta.texto(request, "controlahora"));
		ofer.setHoraInicio(CRUDOferta.texto(request, "horainicio"));
		ofer.setHoraFin(CRUDOferta.texto(request, "horafin"));
		ofer.setTipoOferta(CRUDOferta.texto(request, "tipooferta"));
		ofer.setFechaDesde(CRUDOferta.texto(request, "fechadesde"));
		ofer.setFechaHasta(CRUDOferta.texto(request, "fechahasta"));
		ofer.setCodigoGeneral(CRUDOferta.texto(request, "codigogeneral"));
		ofer.setContact(CRUDOferta.texto(request, "contact"));
		ofer.setRedParcial(CRUDOferta.texto(request, "redparcial"));
		ofer.setReintegro(CRUDOferta.texto(request, "reintegro"));
		ofer.setHabilitado(CRUDOferta.texto(request, "habilitado"));
		return (ofer);
	}

	/** Un parametro de texto, nunca nulo. */
	private static String texto(HttpServletRequest request, String nombre)
	{
		String valor = request.getParameter(nombre);
		return ((valor == null) ? "" : valor.trim());
	}

	/** Un parametro entero; 0 si no vino o no es un numero. */
	private static int entero(HttpServletRequest request, String nombre)
	{
		try
		{
			return (Integer.parseInt(CRUDOferta.texto(request, nombre)));
		}
		catch (Exception e)
		{
			return (0);
		}
	}

	/** Un parametro con decimales; 0 si no vino o no es un numero. */
	private static double decimal(HttpServletRequest request, String nombre)
	{
		try
		{
			return (Double.parseDouble(CRUDOferta.texto(request, nombre)));
		}
		catch (Exception e)
		{
			return (0);
		}
	}
}

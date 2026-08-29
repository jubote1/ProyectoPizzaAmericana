package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.SolicitudConciliacion;;

/**
 * Servlet implementation class InsertarEspecialidad
 * M�todo que se encarga recibir los par�metros para la inserci�n de una especialidad.
 */
@WebServlet("/InsertarSolicitudConciliacion")
public class InsertarSolicitudConciliacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarSolicitudConciliacion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los par�metros para llamar al m�todo insertarEspecialidad de la capa Parametros controlador,
	 * e insertar la especialidad.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		String fecha = request.getParameter("fecha");
		SimpleDateFormat dateFormatOrigen = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat dateFormatDestino = new SimpleDateFormat("yyyy-MM-dd");
		Date datFecha = new Date();
		try {
			datFecha = dateFormatOrigen.parse(fecha);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String fechaFinal = dateFormatDestino.format(datFecha);
		
		String origen = request.getParameter("origen");
		int idTienda;
		try{
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		}catch(Exception e)
		{
			idTienda = 0;
		}
		int idPedidoTienda;
		try{
			idPedidoTienda = Integer.parseInt(request.getParameter("numpedido"));
		}catch(Exception e)
		{
			idPedidoTienda = 0;
		}
		String descripcion = request.getParameter("descripcion");
		String categoria = request.getParameter("categoria");
		double valorAnalizar;
		try{
			valorAnalizar = Double.parseDouble(request.getParameter("valoranalizar"));
		}catch(Exception e)
		{
			valorAnalizar = 0;
		}
		String telefono = request.getParameter("telefono");
		PedidoCtrl pedCtrl = new PedidoCtrl();
		SolicitudConciliacion solicitud = new SolicitudConciliacion(fechaFinal, origen, descripcion, idTienda, categoria,
				valorAnalizar, telefono, idPedidoTienda);
		String respuesta = pedCtrl.insertarSolicitudConciliacion(solicitud);
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

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
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.SolicitudConciliacion;;

/**
 * Servlet implementation class InsertarEspecialidad
 * M�todo que se encarga recibir los par�metros para la inserci�n de una especialidad.
 */
@WebServlet("/ConsultarSolicitudConciliacion")
public class ConsultarSolicitudConciliacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarSolicitudConciliacion() {
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
		HttpSession sesion = request.getSession();
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
		fecha = dateFormatDestino.format(datFecha);
		int idTienda;
		try{
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		}catch(Exception e)
		{
			idTienda = 0;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.consultarSolicitudConciliacion(idTienda, fecha);
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

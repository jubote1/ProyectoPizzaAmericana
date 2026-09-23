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
		//Antes esto era un Double.parseDouble suelto cuyo catch dejaba el valor
		//en CERO. O sea que escribir "150.000" no daba error: guardaba 150, mil
		//veces menos, y escribir "150,000" guardaba cero. Ninguno de los dos
		//avisaba. En la tabla quedaron seis solicitudes asi -135,25 por
		//135.250; 457,5 por 457.500- y las seis siguen PENDIENTE.
		//
		//La regla de lectura vive en utilidadesCC.ValorDigitado, junto con la
		//del valor final, porque resuelta aparte salio distinta en cada lado.
		//Si no se entiende NO se guarda: es preferible que la pantalla diga
		//"revise el valor" a que la solicitud nazca con una cifra que no es.
		Double valorLeido = utilidadesCC.ValorDigitado.leer(request.getParameter("valoranalizar"), false);
		if (valorLeido == null || valorLeido.doubleValue() <= 0) {
			PrintWriter outMal = response.getWriter();
			outMal.write("{\"respuesta\":false,\"error\":\"VALORMALO\"}");
			return;
		}
		double valorAnalizar = valorLeido.doubleValue();
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

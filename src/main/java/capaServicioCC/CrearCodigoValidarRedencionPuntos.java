package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.FidelizacionCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.CodigoRedencionPuntos;;

/**
 * Servlet implementation class GetCliente
 * Servicio que se encarga de consultar todos los registros que tiene asociado un cliente en la tabla de clientes, dando 
 * como par�metro un tel�fono determinado.
 */
@WebServlet("/CrearCodigoValidarRedencionPuntos")
public class CrearCodigoValidarRedencionPuntos extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CrearCodigoValidarRedencionPuntos() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro del tel�fono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el tel�fono indicado.
	 * Lo anterior invocando el m�todo obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			String correo = request.getParameter("correo");
			String fechaSistema = request.getParameter("fechasistema");
			double puntosRedimir = Double.parseDouble(request.getParameter("puntosredimir"));
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			CodigoRedencionPuntos codRed = new CodigoRedencionPuntos();
			codRed.setCorreo(correo);
			codRed.setFechaSistema(fechaSistema);
			codRed.setPuntos(puntosRedimir);
			String respuesta = fidCtrl.crearCodigoValidarRedencionPuntos(codRed);
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

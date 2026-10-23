package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;;

/**
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca
 * PedidoCtrl.insertarSolicitudCumple.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarSolicitudCumple   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): fecha (string), idpedido (integer), idtienda (integer).
 * Devuelve: texto; claves: idSolicitudCumple.
 * Escribe: solicitud_cumple (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarSolicitudCumple.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarSolicitudCumple")
public class InsertarSolicitudCumple extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarSolicitudCumple() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como par�metro el iddetallepedido al cual se le realiz� la adicion, adicionalmente se 
	 * env�a el iddetallepedido relacionado a la adicion, la adici�n relacionada a la especialidad 1 o la especialidad 2 y 
	 * las cantidades. Lo anterior invocando al m�todo InsertarDetalleAdicion de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				String fecha = "";
				int idpedido = 0;
				int idtienda = 0;
				fecha = request.getParameter("fecha");
		        try
		        {
		        	idpedido = Integer.parseInt(request.getParameter("idpedido"));
		        }catch(Exception e)
		        {
		        	idpedido = 0;
		        }
		        try
		        {
		        	idtienda = Integer.parseInt(request.getParameter("idtienda"));
		        }catch(Exception e)
		        {
		        	idtienda = 0;
		        }
		        PedidoCtrl pedidoCtrl = new PedidoCtrl();
		        String respuesta = pedidoCtrl.insertarSolicitudCumple(fecha,idtienda, idpedido);
		        System.out.println(respuesta);
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

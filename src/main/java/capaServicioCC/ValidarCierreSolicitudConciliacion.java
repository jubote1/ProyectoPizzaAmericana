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
 * Servicio de consulta de Pagos virtuales y conciliacion: invoca
 * PedidoCtrl.validarCierreSolicitudConciliacion.
 *
 * Ruta: /ProyectoPizzaAmericana/ValidarCierreSolicitudConciliacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer).
 * Devuelve: texto; claves: respuesta.
 * Lee: solicitud_conciliacion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ValidarCierreSolicitudConciliacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ValidarCierreSolicitudConciliacion")
public class ValidarCierreSolicitudConciliacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ValidarCierreSolicitudConciliacion() {
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
		int idTienda;
		try{
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		}catch(Exception e)
		{
			idTienda = 0;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.validarCierreSolicitudConciliacion(idTienda);
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

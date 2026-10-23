package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.TiendaCtrl;

/**
 * Servicio de consulta de Tiendas y operacion: invoca TiendaCtrl.consultarBloqueosTienda.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarBloqueosTienda   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (string), fecha (string).
 * Devuelve: texto; claves: accion, aprobado, fecha_accion, idlogbloqueo, motivo, observacion,
 * tienda.
 * Lee: log_bloqueo_tienda, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarBloqueosTienda.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarBloqueosTienda")
public class ConsultarBloqueosTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarBloqueosTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibe como parámetro principal el idoperacion con base en lo siguiente: 
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la operación seleccionada se 
	 * solicitarán ciertos parámetros y posteriormente se invocará al método correspondiente en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		//Operación idoperacion 1 insertar 3 Eliminar  4 Consultar
		response.addHeader("Access-Control-Allow-Origin", "*");
		String stridTienda = request.getParameter("idtienda");
		int idTienda;
		String respuesta="";
		TiendaCtrl tiendaCtrl = new TiendaCtrl();
		try
		{
			idTienda = Integer.parseInt(stridTienda);
		}catch(Exception e){
			idTienda = 0;
		}
		String fecha = request.getParameter("fecha");
		respuesta = tiendaCtrl.consultarBloqueosTienda(idTienda, fecha);
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

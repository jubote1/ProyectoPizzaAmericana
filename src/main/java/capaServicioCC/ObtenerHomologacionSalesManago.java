package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.HomologacionSalesManago;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca PedidoCtrl.obtenerHomologacionSalesManago.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerHomologacionSalesManago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): tamano (string), name (string), especialidad (string).
 * Devuelve: texto; claves: id, nombre.
 * Lee: homologacion_sales_manago.
 * Sistemas externos: SALESmanago.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerHomologacionSalesManago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerHomologacionSalesManago")
public class ObtenerHomologacionSalesManago extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerHomologacionSalesManago() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		String tamano = request.getParameter("tamano");
		if(tamano == null)
		{
			tamano = "";
		}
		String name = request.getParameter("name");
		if(name == null)
		{
			name = "";
		}
		String especialidad = request.getParameter("especialidad");
		if(especialidad == null)
		{
			especialidad = "";
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		HomologacionSalesManago homol = new HomologacionSalesManago(tamano,name,especialidad);
        String respuesta = pedCtrl.obtenerHomologacionSalesManago(homol);
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

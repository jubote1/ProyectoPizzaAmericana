package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Ofertas y fidelizacion (administracion): invoca
 * PedidoCtrl.obtenerPromociones.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerPromocion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: texto; claves: descripcion, idpromocion.
 * Lee: promocion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerPromocion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerPromocion")
public class ObtenerPromocion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerPromocion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta = pedCtrl.obtenerPromociones();
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

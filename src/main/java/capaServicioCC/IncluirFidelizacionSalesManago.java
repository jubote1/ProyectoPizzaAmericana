package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Envia a SALESmanago (la plataforma de marketing) los datos de un cliente nuevo para el programa
 * de fidelizacion.
 *
 * Ruta: /ProyectoPizzaAmericana/IncluirFidelizacionSalesManago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): nombres (string), apellidos (string), direccion (string), telefonocelular
 * (string), email (string).
 * Devuelve: texto.
 * Lee: parametros.
 * Sistemas externos: SALESmanago.
 *
 * Trampas y riesgos conocidos:
 * - Si el tercero no responde, el resultado depende de getResponseFromUrl: la pantalla no valida
 *   si funciono.
 * - Entrega datos personales a un tercero (hay que cubrirlo en la autorizacion de datos).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha IncluirFidelizacionSalesManago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/IncluirFidelizacionSalesManago")
public class IncluirFidelizacionSalesManago extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public IncluirFidelizacionSalesManago() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
        String nombres = request.getParameter("nombres");
        String apellidos = request.getParameter("apellidos");
        String direccion = request.getParameter("direccion");
        String telefonoCelular = request.getParameter("telefonocelular");
        String email = request.getParameter("email");
        PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta  = pedCtrl.incluirFidelizacionSalesManago(nombres, apellidos, direccion,  telefonoCelular, email, true);
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

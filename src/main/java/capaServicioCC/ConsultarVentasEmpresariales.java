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
import capaModeloCC.SolicitudFactura;
import capaModeloCC.Usuario;

/**
 * Servicio de consulta de Reportes y estadisticas: invoca
 * PedidoCtrl.ConsultarPedidosVentasCorporativas.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarVentasEmpresariales   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string).
 * Devuelve: texto; claves: asesor, cliente, fecha, idpedido, idpedidotienda, nombrecompania,
 * valor.
 * Lee: cliente, estado_pedido, forma_pago, pedido, pedido_forma_pago, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarVentasEmpresariales.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarVentasEmpresariales")
public class ConsultarVentasEmpresariales extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarVentasEmpresariales() {
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
		String fechaInicial = request.getParameter("fechainicial");
		String fechaFinal = request.getParameter("fechafinal");
        PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta  = pedCtrl.ConsultarPedidosVentasCorporativas(fechaInicial, fechaFinal);
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

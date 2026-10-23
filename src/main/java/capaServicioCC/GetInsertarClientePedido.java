package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;

/**
 * Servicio que modifica datos de Pedidos: invoca ClienteCtrl.InsertarClientePedido.
 *
 * Ruta: /ProyectoPizzaAmericana/GetInsertarClientePedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): telefono (string), nombres (string), direccion (string), zona (string),
 * observacion (string), tienda (string).
 * Devuelve: texto; claves: idcliente, resultado.
 * Lee: cliente, municipio, nomenclatura_direccion, tienda.
 * Escribe: cliente (INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetInsertarClientePedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetInsertarClientePedido")
public class GetInsertarClientePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetInsertarClientePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los parámetros de cliente y realiza la utilización de método en capa Cliente Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		String telefono = request.getParameter("telefono");
        String nombres = request.getParameter("nombres");
        String direccion = request.getParameter("direccion");
        String zona = request.getParameter("zona");
        String observacion = request.getParameter("observacion");
        String tienda = request.getParameter("tienda");
        ClienteCtrl ClienCtrl = new ClienteCtrl();
        String respuesta = ClienCtrl.InsertarClientePedido(telefono, nombres, direccion, zona, observacion, tienda);
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

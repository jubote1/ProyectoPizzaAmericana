package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Devuelve la alerta que el negocio le haya puesto a un telefono (positiva o negativa), por
 * ejemplo un cliente problematico o uno especial.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerClienteAlerta   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): telefono (string).
 * Devuelve: application/json; claves: mensaje, telefono, tipo_alerta.
 * Lee: cliente_alerta.
 *
 * Trampas y riesgos conocidos:
 * - Si el telefono tiene varias alertas solo se ve la primera.
 * - SQL concatenado, sin sesion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerClienteAlerta.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerClienteAlerta")
public class ObtenerClienteAlerta extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerClienteAlerta() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe par�metros se encarga de retornar en formato JSON todos los productos parametrizados en el
	 * sistema invocando al m�todo obtenerTodosProductos de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			String telefono;
			try
			{
				telefono = request.getParameter("telefono");
			}catch(Exception e)
			{
				System.out.println(e.toString());
				telefono = "";
			}
			ClienteCtrl clienteCtrl = new ClienteCtrl();
			String respuesta = clienteCtrl.obtenerClienteAlerta(telefono);
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

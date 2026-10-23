package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;;

/**
 * Cuenta los pedidos que ese telefono ya tiene hoy en estado 2 (finalizado en el central).
 *
 * Ruta: /ProyectoPizzaAmericana/ValidarTelefono   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): telefono (string).
 * Devuelve: application/json; claves: cantidad.
 * Lee: cliente, pedido.
 *
 * Trampas y riesgos conocidos:
 * - El nombre engana: no valida el telefono, cuenta pedidos del dia.
 * - SQL concatenado, sin sesion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ValidarTelefono.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ValidarTelefono")
public class ValidarTelefono extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ValidarTelefono() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro del teléfono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el teléfono indicado.
	 * Lo anterior invocando el método obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			String tel = request.getParameter("telefono");
			System.out.println(tel);
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ClienteCtrl ClienCtrl = new ClienteCtrl();
			String respuesta = ClienCtrl.validarTelefono(tel);
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

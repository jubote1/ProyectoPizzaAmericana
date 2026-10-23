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
 * Busca los clientes (direcciones) que tienen el telefono dado.
 *
 * Ruta: /ProyectoPizzaAmericana/GetCliente   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): telefono (string).
 * Devuelve: application/json;charset=UTF-8; claves: apellido, clientesiniden, correo, direccion,
 * distanciatienda, email, emailfact, fechanacimiento, idCliente, identificacion, idnomenclatura,
 * idtipopersona, ....
 * Lee: cliente, municipio, nomenclatura_direccion, tienda.
 *
 * Trampas y riesgos conocidos:
 * - El SQL concatena el telefono: es inyectable y no pide sesion.
 * - Devuelve datos personales (nombre, direccion, correo, identificacion) a cualquiera que
 *   conozca un telefono.
 * - Cualquier error se registra y se devuelve [], igual que cuando no hay cliente: no se
 *   distingue "no existe" de "fallo".
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetCliente.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetCliente")
public class GetCliente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetCliente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro del tel�fono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el tel�fono indicado.
	 * Lo anterior invocando el m�todo obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			request.setCharacterEncoding("UTF-8");
			String tel = request.getParameter("telefono");
			System.out.println(tel);
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json;charset=UTF-8");
			ClienteCtrl ClienCtrl = new ClienteCtrl();
			String respuesta = ClienCtrl.obtenerCliente(tel);
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

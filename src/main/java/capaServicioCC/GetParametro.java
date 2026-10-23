package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.ParametrosCtrl;;

/**
 * Devuelve el valor de un parametro de configuracion de la tabla parametros (base general).
 *
 * Ruta: /ProyectoPizzaAmericana/GetParametro   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): parametro (string).
 * Devuelve: application/json; claves: valornumerico, valortexto.
 * Lee: parametros.
 *
 * Trampas y riesgos conocidos:
 * - SEGURIDAD, grave: el SQL se arma concatenando el parametro (... where valorparametro = '" +
 *   valorParametro + "'), asi que es inyectable, y el servicio no pide sesion. Quien llegue al
 *   central puede leer cualquier parametro, incluidas las claves privadas de Wompi que
 *   pedidos.js carga en el navegador (WOMPIPRODUCCIONPRI).
 * - El controlador imprime en el log del servidor el JSON devuelto (System.out.println), asi que
 *   los secretos quedan tambien en el log de Tomcat.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetParametro.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetParametro")
public class GetParametro extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetParametro() {
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
			String parametro = request.getParameter("parametro");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ParametrosCtrl parCtrl = new ParametrosCtrl();
			String respuesta = parCtrl.obtenerParametro(parametro);
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

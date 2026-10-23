package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.FidelizacionCtrl;
import capaControladorCC.PedidoCtrl;;

/**
 * Dice cuantos puntos vigentes tiene un cliente de fidelizacion, buscandolo por su correo.
 *
 * Ruta: /ProyectoPizzaAmericana/ValidarRedencionPuntos   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): correo (string).
 * Devuelve: application/json; claves: puntos, respuesta.
 * Lee: cliente_fidelizacion.
 *
 * Trampas y riesgos conocidos:
 * - La identidad del programa es el correo, no el telefono: un cliente sin correo no tiene
 *   puntos.
 * - Cualquiera que sepa un correo puede consultar los puntos (sin sesion).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ValidarRedencionPuntos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ValidarRedencionPuntos")
public class ValidarRedencionPuntos extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ValidarRedencionPuntos() {
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
			String correo = request.getParameter("correo");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			String respuesta = fidCtrl.validarRedencionPuntos(correo);
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

package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PromocionesCtrl;;

/**
 * Dice si un codigo promocional existe, esta vigente y se puede usar.
 *
 * Ruta: /ProyectoPizzaAmericana/GetValidarCodigoPromocional   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): codigopromocional (string).
 * Devuelve: application/json; claves: codigopromocional, fechacaducidad, idcliente, idoferta,
 * idofertacliente, ingresooferta, mensaje, observacion, pqrs, redparcial, respuesta, saldo, ....
 * Lee: oferta, oferta_cliente.
 *
 * Trampas y riesgos conocidos:
 * - El freno por intentos es por IP y vive en la memoria del servidor: se pierde al reiniciar y,
 *   detras de un proxy, todas las personas comparten IP.
 * - No valida que el codigo corresponda al cliente que esta en la llamada: un codigo personal de
 *   otro cliente es valido para quien lo conozca.
 * - El codigo se compara con consultas preparadas (no inyectable).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetValidarCodigoPromocional.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetValidarCodigoPromocional")
public class GetValidarCodigoPromocional extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetValidarCodigoPromocional() {
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
			String codigoPromocional = request.getParameter("codigopromocional");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			//Este servicio no pide sesion, asi que se frena a quien prueba codigos al azar.
			final String origen = request.getRemoteAddr() + "|legado";
			if(utilidadesCC.LimitadorIntentos.bloqueado(origen))
			{
				response.getWriter().write("{\"respuesta\":\"NOK\",\"mensaje\":\"Demasiados intentos.\"}");
				return;
			}
			PromocionesCtrl promoCtrl = new PromocionesCtrl();
			String respuesta = promoCtrl.retornarOfertaCodigoPromocional(codigoPromocional);
			if(respuesta.contains("\"respuesta\":\"NOK\""))
			{
				utilidadesCC.LimitadorIntentos.registrarFallo(origen);
			}
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

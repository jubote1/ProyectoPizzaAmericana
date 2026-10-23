package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PromocionesCtrl;
import capaModeloCC.Usuario;;

/**
 * Devuelve los datos de una oferta del cliente a partir de su identificador (idofertacliente), por
 * ejemplo la que el operario escogio de la lista de ofertas pendientes.
 *
 * Ruta: /ProyectoPizzaAmericana/GetCodigoPromocional   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idofertacliente (string).
 * Devuelve: application/json; claves: codigopromocion, idcliente, idoferta, idofertacliente,
 * ingresooferta, nombrecliente, nombreoferta, observacion, pqrs, redparcial, saldo, usooferta,
 * ....
 * Lee: cliente, oferta, oferta_cliente.
 *
 * Trampas y riesgos conocidos:
 * - Sin sesion: cualquiera que adivine un idofertacliente (numero consecutivo) obtiene el codigo
 *   promocional de otra persona.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetCodigoPromocional.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetCodigoPromocional")
public class GetCodigoPromocional extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetCodigoPromocional() {
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
			String strIdOfertaCliente = request.getParameter("idofertacliente");
			int idOfertaCliente = 0;
			try
			{
				idOfertaCliente = Integer.parseInt(strIdOfertaCliente);
			}catch(Exception e)
			{
				idOfertaCliente = 0;
			}
			//Capturamos la información de la sesion del usuario que está ejecutando la inserción
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PromocionesCtrl promoCtrl = new PromocionesCtrl();
			String respuesta = promoCtrl.retornarOfertaCliente(idOfertaCliente);
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

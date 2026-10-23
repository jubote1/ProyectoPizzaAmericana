package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;;

/**
 * Despues de que alguien creo el link de pago en Wompi, avisa al cliente (correo y, segun el
 * canal, WhatsApp) y guarda el link en el pedido.
 *
 * Ruta: /ProyectoPizzaAmericana/RealizarNotificacionWompi   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idcliente (integer), idlink (string), linkpago (string), idformapago
 * (integer), idpedido (integer).
 * Devuelve: application/json; claves: mail.smtp.host, mail.smtp.ssl.protocols, respuesta.
 * Lee: cliente, crm.correo_rebotado, forma_pago, integracion_crm, municipio,
 * nomenclatura_direccion, parametros, parametros_correo, pedido_pago_virtual, tienda.
 * Escribe: cliente (UPDATE), pedido (UPDATE), pedido_idlink (INSERT), pedido_pago_virtual
 * (INSERT).
 * Sistemas externos: Brevo, Correo SMTP, Gmail, Kommo (CRM), WhatsApp, Wompi.
 *
 * Trampas y riesgos conocidos:
 * - Responde OK aunque no se haya podido enviar el correo: el detalle queda solo en
 *   pedido_pago_virtual.observacion.
 * - Quien llama puede inventar un linkpago (cualquier URL) y el sistema se lo enviara al
 *   cliente. No verifica el link contra Wompi.
 * - En esta pantalla el link lo crea el navegador con la clave privada de Wompi; las otras vias
 *   (bot, tienda virtual) lo crean en el servidor.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RealizarNotificacionWompi.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RealizarNotificacionWompi")
public class RealizarNotificacionWompi extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RealizarNotificacionWompi() {
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
			String idLink = "";
			int idCliente = 0;
			String linkPago = "";
			int idFormaPago = 0;
			int idPedido = 0;
			try
			{
				idCliente = Integer.parseInt(request.getParameter("idcliente"));
			}catch(Exception e)
			{
				idCliente = 0;
			}
			idLink = request.getParameter("idlink");
			linkPago = request.getParameter("linkpago");
			try
			{
				idFormaPago = Integer.parseInt(request.getParameter("idformapago"));
			}catch(Exception e)
			{
				idFormaPago = 0;
			}
			try
			{
				idPedido = Integer.parseInt(request.getParameter("idpedido"));
			}catch(Exception e)
			{
				idPedido = 0;
			}
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl pedCtrl = new PedidoCtrl();
			String respuesta = pedCtrl.realizarNotificacionWompi(idLink, idCliente, linkPago, idFormaPago, idPedido);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
			
		}catch(Exception e){
			System.out.println(e.getMessage() + e.toString() +  e.getStackTrace());
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

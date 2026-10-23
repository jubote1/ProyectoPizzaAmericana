package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.TiendaCtrl;
/**
 * Servicio que modifica datos de Plataformas (Rappi, DiDi, Kuno): invoca
 * PedidoCtrl.insertarPedidoTiendaVirtualKuno.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarPedidoTiendaVirtualKuno   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera authorization.
 * Devuelve: application/json; claves: amount_in_cents, collect_shipping, currency, description,
 * expires_at, mail.smtp.host, mail.smtp.ssl.protocols, mensaje, name, redirect_url, respuesta,
 * single_use, ....
 * Lee: adicion_detalle_pedido, auth_kuno, cliente, descuentos_generales, detalle_pedido,
 * especialidad_excepcion, excepcion_precio, forma_pago, homologacion_forpag_pedidovirtual,
 * homologacion_producto_pedidovirtual.
 * Escribe: cliente (INSERT/UPDATE), detalle_pedido (INSERT), log_pedido_virtual_kuno (INSERT),
 * pedido (INSERT/UPDATE), pedido_idlink (INSERT), pedido_pago_virtual (INSERT),
 * pedido_tienda_virtual (INSERT/UPDATE).
 * Sistemas externos: ArcGIS, Brevo, Correo SMTP, DiDi, GloriaFood/Kuno, Gmail, Google Maps, Kommo
 * (CRM), WhatsApp, Wompi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarPedidoTiendaVirtualKuno.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarPedidoTiendaVirtualKuno")
public class InsertarPedidoTiendaVirtualKuno extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarPedidoTiendaVirtualKuno() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros dado que no filtra la información, simplemente retorna en formato JSON 
	 * las tiendas o puntos de venta parametrizados en el sistema, invocando el método obtenerTiendas de la capa Tienda Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			//Devolveremos según documentación de WOMPI un JSON vacío
			response.setContentType("application/json");
			//Recuperamos el valor enviado en el body, el cual no tiene ninguna maración
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			String dataJSON = sb.toString();
			PedidoCtrl pedidoCtrl = new PedidoCtrl();
			//Tomaremos el Authorization
			String authHeader = request.getHeader("authorization");          
			String auth = "HTTP Authorization header:";
			if (authHeader == null) {            
			        authHeader = auth +" No authorization header";      
			} else {            
			        //Se deja el authHeader solito
			}
			
			String respuesta = pedidoCtrl.insertarPedidoTiendaVirtualKuno(dataJSON, authHeader);
			//String respuesta = TienCtrl.obtenerTiendas();
			PrintWriter out = response.getWriter();
			//out.write(respuesta);
			response.setStatus(200);
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

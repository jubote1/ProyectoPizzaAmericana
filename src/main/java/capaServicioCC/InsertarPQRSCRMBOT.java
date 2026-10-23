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
 * Crea una solicitud PQRS (peticion, queja, reclamo o sugerencia) con lo que el cliente conto al
 * bot.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarPQRSCRMBOT   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera authorization.
 * Devuelve: application/json; claves: idSolicitudPQRS, mail.smtp.host, mail.smtp.ssl.protocols,
 * message, registroComentarios, success.
 * Lee: cliente, integracion_crm, log_pedido_crmbot, municipio, nomenclatura_direccion, parametros,
 * parametros_correo, pedido, pqrs_comentario, tienda.
 * Escribe: log_pedido_crmbot (INSERT/UPDATE), pqrs_comentario (DELETE/INSERT/UPDATE),
 * solicitudpqrs (INSERT).
 * Sistemas externos: Correo SMTP, GloriaFood/Kuno, Gmail, Kommo (CRM).
 *
 * Trampas y riesgos conocidos:
 * - No valida la cabecera Authorization.
 * - El anti-duplicado depende de la ventana de minutos y del numero de lead: otro lead del mismo
 *   cliente crea otra PQRS.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarPQRSCRMBOT.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarPQRSCRMBOT")
public class InsertarPQRSCRMBOT extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarPQRSCRMBOT() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe par�metros dado que no filtra la informaci�n, simplemente retorna en formato JSON 
	 * las tiendas o puntos de venta parametrizados en el sistema, invocando el m�todo obtenerTiendas de la capa Tienda Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			request.setCharacterEncoding("UTF-8");
			//Devolveremos seg�n documentaci�n de WOMPI un JSON vac�o
			response.setContentType("application/json");
			//Recuperamos el valor enviado en el body, el cual no tiene ninguna maraci�n
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			String data = sb.toString();
			PedidoCtrl pedidoCtrl = new PedidoCtrl();
			//Tomaremos el Authorization
			String authHeader = request.getHeader("authorization");          
			String auth = "HTTP Authorization header:";
			if (authHeader == null) {            
			        authHeader = auth +" No authorization header";      
			} else {            
			        //Se deja el authHeader solito
			}
			
			String respuesta = pedidoCtrl.insertarPQRSCRMBOT(data, authHeader); 
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

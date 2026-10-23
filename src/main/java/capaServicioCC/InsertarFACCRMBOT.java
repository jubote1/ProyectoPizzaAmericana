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
 * Registra una solicitud de factura electronica con los datos de facturacion del lead.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarFACCRMBOT   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera authorization.
 * Devuelve: application/json; claves: idsolicitud, mail.smtp.host, mail.smtp.ssl.protocols.
 * Lee: integracion_crm, parametros, parametros_correo.
 * Escribe: log_pedido_crmbot (INSERT/UPDATE), solicitud_factura (INSERT).
 * Sistemas externos: Correo SMTP, GloriaFood/Kuno, Gmail, Kommo (CRM).
 *
 * Trampas y riesgos conocidos:
 * - No valida la cabecera Authorization.
 * - Los datos fiscales (NIT, correo) viajan por Kommo y se guardan como llegan.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarFACCRMBOT.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarFACCRMBOT")
public class InsertarFACCRMBOT extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarFACCRMBOT() {
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
			
			String respuesta = pedidoCtrl.insertarFACCRMBOT(data, authHeader); 
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

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
import capaControladorCC.PedidoMrTamalCtrl;
import capaControladorCC.TiendaCtrl;
/**
 * Servicio que modifica datos de Pagos virtuales y conciliacion: invoca
 * PedidoMrTamalCtrl.capturarEventoPagoWompiMrTamal.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerRespuestaWompiMrTamal   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion.
 * Devuelve: application/json; claves: mail.smtp.host, mail.smtp.ssl.protocols, respuesta.
 * Lee: pedido_link_wompi, tienda_franquicia_wompi.
 * Escribe: log_evento_wompi (INSERT).
 * Sistemas externos: Correo SMTP, Gmail, Wompi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerRespuestaWompiMrTamal.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerRespuestaWompiMrTamal")
public class ObtenerRespuestaWompiMrTamal extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerRespuestaWompiMrTamal() {
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
			PedidoMrTamalCtrl pedidoCtrl = new PedidoMrTamalCtrl();
			String respuesta = pedidoCtrl.capturarEventoPagoWompiMrTamal(dataJSON);
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

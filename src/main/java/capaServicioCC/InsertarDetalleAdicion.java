package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;

/**
 * Une un renglon de adicion con el renglon de la pizza a la que pertenece y reparte la cantidad
 * entre las mitades.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarDetalleAdicion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): iddetallepedidopadre (integer), iddetallepedidoadicion (integer),
 * idespecialidad1 (integer), idespecialidad2 (integer), cantidad1 (number), cantidad2 (number).
 * Devuelve: texto; claves: idadicion.
 *
 * Trampas y riesgos conocidos:
 * - Los dos ids obligatorios se leen sin try/catch: si faltan, HTTP 500.
 * - No tiene efecto sobre el precio: el valor de la adicion ya esta en su propio renglon.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarDetalleAdicion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarDetalleAdicion")
public class InsertarDetalleAdicion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarDetalleAdicion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro el iddetallepedido al cual se le realizó la adicion, adicionalmente se 
	 * envía el iddetallepedido relacionado a la adicion, la adición relacionada a la especialidad 1 o la especialidad 2 y 
	 * las cantidades. Lo anterior invocando al método InsertarDetalleAdicion de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				int iddetallepedidopadre = Integer.parseInt(request.getParameter("iddetallepedidopadre"));
		        int iddetallepedidoadicion = Integer.parseInt(request.getParameter("iddetallepedidoadicion"));
		        int  idespecialidad1;
		        int  idespecialidad2;
		        try
		        {
		        	idespecialidad1 = Integer.parseInt(request.getParameter("idespecialidad1"));
		        }catch(Exception e)
		        {
		        	idespecialidad1 = 0;
		        }
		        try
		        {
		        	idespecialidad2 = Integer.parseInt(request.getParameter("idespecialidad2"));
		        }catch(Exception e)
		        {
		        	idespecialidad2 = 0;
		        }
		        double cantidad1;
		        double cantidad2;
		        try
		        {
		        	cantidad1 =  Double.parseDouble(request.getParameter("cantidad1"));
		        }catch(Exception e)
		        {
		        	cantidad1 = 0;
		        }
		        try
		        {
		        	cantidad2 =  Double.parseDouble(request.getParameter("cantidad2"));
		        }catch(Exception e)
		        {
		        	cantidad2 = 0;
		        }
		        logger.info("Llamado a servicio InsertarDetalleAdicion con parámetros iddetallepedidopadre: "
		        		+ iddetallepedidopadre + " iddetallepedidoadicion:  " + iddetallepedidoadicion + " idespecialidad1: " + idespecialidad1
		        		+ " idespecialidad2: " + idespecialidad2 + " cantidad1: " + cantidad1 + " cantidad2: " + cantidad2 );
		        PedidoCtrl PedidoCtrl = new PedidoCtrl();
		        String respuesta = PedidoCtrl.InsertarDetalleAdicion(iddetallepedidopadre, iddetallepedidoadicion,idespecialidad1, idespecialidad2, cantidad1, cantidad2);
		        System.out.println(respuesta);
		        PrintWriter out = response.getWriter();
				out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

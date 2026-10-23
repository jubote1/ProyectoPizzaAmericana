package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import capaControladorCC.EmpleadoCtrl;
import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.EmpleadoVale;;

/**
 * Servicio que modifica datos de Tiendas y operacion: invoca EmpleadoCtrl.insertarEmpleadoVale.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarEmpleadoVale   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): fecha (string), descuadre (string), idempleado (integer), idegreso
 * (integer), valor (number).
 * Devuelve: texto; claves: idempleadovale.
 * Escribe: empleado_vale (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarEmpleadoVale.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarEmpleadoVale")
public class InsertarEmpleadoVale extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarEmpleadoVale() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como par�metro el iddetallepedido al cual se le realiz� la adicion, adicionalmente se 
	 * env�a el iddetallepedido relacionado a la adicion, la adici�n relacionada a la especialidad 1 o la especialidad 2 y 
	 * las cantidades. Lo anterior invocando al m�todo InsertarDetalleAdicion de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				String fecha = "";
				int idempleado = 0;
				int idEgreso = 0;
				double  valor = 0;
				fecha = request.getParameter("fecha");
				String descuadre = request.getParameter("descuadre");
				if(descuadre == null)
				{
					descuadre = "N";
				}
				if(descuadre.equals(new String("")))
				{
					descuadre = "N";
				}
		        try
		        {
		        	idempleado = Integer.parseInt(request.getParameter("idempleado"));
		        }catch(Exception e)
		        {
		        	idempleado = 0;
		        }
		        try
		        {
		        	idEgreso = Integer.parseInt(request.getParameter("idegreso"));
		        }catch(Exception e)
		        {
		        	idEgreso = 0;
		        }
		        try
		        {
		        	valor = Double.parseDouble(request.getParameter("valor"));
		        }catch(Exception e)
		        {
		        	valor = 0;
		        }
		        EmpleadoCtrl empCtrl = new EmpleadoCtrl();
		        String respuesta = empCtrl.insertarEmpleadoVale(new EmpleadoVale(0,idempleado, fecha, valor, descuadre,idEgreso));
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

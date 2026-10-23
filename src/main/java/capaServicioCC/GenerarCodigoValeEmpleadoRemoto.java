package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.PedidoPrecioEmpleado;
import capaModeloCC.Usuario;

/**
 * Servicio que modifica datos de Tiendas y operacion: invoca
 * PedidoCtrl.generarCodigoValeEmpleadoRemoto.
 *
 * Ruta: /ProyectoPizzaAmericana/GenerarCodigoValeEmpleadoRemoto   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idempleado (integer), fecha (string).
 * Devuelve: texto; claves: codigo.
 * Lee: empleado_remoto_vale.
 * Escribe: empleado_remoto_vale (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GenerarCodigoValeEmpleadoRemoto.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GenerarCodigoValeEmpleadoRemoto")
public class GenerarCodigoValeEmpleadoRemoto extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GenerarCodigoValeEmpleadoRemoto() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro la información más básica de un pedido como lo son, tinda, idcliente, 
	 * fechapedido, con estos tres hace una inserción del encabezado pedido y retorna en formato JSON el idpedido que se
	 * manejará para el pedido. Esta labor se realiza invocando el método InsertarEncabezadoPedido de la capa Pedido
	 * Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		//Capturamos la información de la sesion del usuario que está ejecutando la inserción
		int idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
		String fecha = request.getParameter("fecha");		
		PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta = pedCtrl.generarCodigoValeEmpleadoRemoto(idEmpleado,fecha);
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

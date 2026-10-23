	package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * Lista todas las promociones (excepciones de precio) habilitadas, con sus reglas.
 *
 * Ruta: /ProyectoPizzaAmericana/getExcepcionesPrecio   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: cantidadingredientes, controlacantidadingredientes,
 * controlaespecialidades, descripcion, domingo, horafinal, horainicial, idexcepcion, idproducto,
 * idproductodepende, jueves, lunes, ....
 * Lee: excepcion_precio.
 *
 * Trampas y riesgos conocidos:
 * - Las validaciones de dia, hora, codigo, oferta y dependencia se hacen solo en el navegador:
 *   el servidor no vuelve a comprobarlas al insertar el detalle.
 * - La hora de la promocion se compara con el reloj del navegador, no con el del servidor.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha getExcepcionesPrecio.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/getExcepcionesPrecio")
public class getExcepcionesPrecio extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public getExcepcionesPrecio() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio no toma ningún filtro, se retornan todas las excepciones de precio de producto en formato JSON
	 * invocando al método obtenerTodosExcepciones() de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.obtenerTodosExcepciones();
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

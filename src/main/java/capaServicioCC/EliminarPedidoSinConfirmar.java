package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Descarta un pedido completo: es el boton de reiniciar de la pantalla.
 *
 * Ruta: /ProyectoPizzaAmericana/EliminarPedidoSinConfirmar   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: texto; claves: respuesta.
 * Lee: adicion_detalle_pedido, detalle_pedido, modificador_detalle_pedido, pedido.
 * Escribe: adicion_detalle_pedido (DELETE), detalle_pedido (DELETE), log_reiniciar_pedido
 * (INSERT), modificador_detalle_pedido (DELETE), pedido (DELETE).
 *
 * Trampas y riesgos conocidos:
 * - GRAVE: a pesar del nombre, NO verifica que el pedido este sin confirmar. Se puede borrar un
 *   pedido ya finalizado, pagado o enviado a la tienda con solo conocer su numero. No necesita
 *   sesion.
 * - Son cinco sentencias sin transaccion: si alguna falla queda un pedido a medias.
 * - No borra pedido_forma_pago ni otras tablas relacionadas: podrian quedar filas huerfanas (si
 *   la base no tiene borrado en cascada).
 * - Sin el parametro, HTTP 500.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha EliminarPedidoSinConfirmar.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/EliminarPedidoSinConfirmar")
public class EliminarPedidoSinConfirmar extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EliminarPedidoSinConfirmar() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio solicitará el idpedido, con esta informción se encargará de eliminar la información del pedido que no se ha confirmado.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		Usuario usuario = (Usuario) sesion.getAttribute("usuario");
		String user = "" ;
		//Al no existir el usuario logueado es posible que produza una excepcion
		try
		{
			user = usuario.getNombreUsuario();
			
			
		}catch(Exception e)
		{
			
			
		}
		int idpedido = Integer.parseInt(request.getParameter("idpedido"));
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.eliminarPedidoSinConfirmar(idpedido, user);
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

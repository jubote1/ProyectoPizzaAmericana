package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;

/**
 * Servicio que modifica datos de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.adicionarComentarioPQRS.
 *
 * Ruta: /ProyectoPizzaAmericana/AdicionarComentarioPQRS   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): pqrs (integer), comentario (string).
 * Devuelve: application/json;charset=UTF-8; claves: resultado.
 * Lee: parametros, parametros_correo.
 * Escribe: solicitudpqrs (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha AdicionarComentarioPQRS.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/AdicionarComentarioPQRS")
public class AdicionarComentarioPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AdicionarComentarioPQRS() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocar� el m�todo de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		int PQRS = 0;
		try
		{
			PQRS = Integer.parseInt(request.getParameter("pqrs"));
		}catch(Exception e)
		{
			PQRS = 0;
		}
		String comentario = "";
		comentario = request.getParameter("comentario");
		
		SolicitudPQRSCtrl solPQRSCtrl = new SolicitudPQRSCtrl();
		String respuesta = solPQRSCtrl.adicionarComentarioPQRS(PQRS, comentario);
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

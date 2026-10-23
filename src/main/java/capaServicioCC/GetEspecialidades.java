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
 * Lista las especialidades de pizza (Hawaiana, Pollo Champinon...) que se pueden elegir.
 *
 * Ruta: /ProyectoPizzaAmericana/GetEspecialidades   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idexcepcion (integer), idproducto (integer).
 * Devuelve: application/json; claves: abreviatura, idespecialidad, nombre.
 * Lee: controla_especialidades, especialidad.
 *
 * Trampas y riesgos conocidos:
 * - Si se mandan los dos filtros a la vez no hay consulta definida: falla en silencio y devuelve
 *   [].
 * - No filtra por tienda: una especialidad que una tienda no pueda hacer sale igual.
 * - No devuelve el recargo de la especialidad (eso va en ObtenerExcepcionEspecialidad).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetEspecialidades.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetEspecialidades")
public class GetEspecialidades extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetEspecialidades() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio no toma ningún parámetro y retorna todas las especialidades en formato JSON, invocando al método
	 * obtenerEspecialidades() de la capa Controlador Pedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			int idExcepcion = 0;
			try {
				idExcepcion = Integer.parseInt(request.getParameter("idexcepcion"));
			} catch (Exception e) {
				idExcepcion = 0;
			}
			int idProducto = 0;
			try {
				idProducto = Integer.parseInt(request.getParameter("idproducto"));
			} catch (Exception e) {
				idProducto = 0;
			}
			String respuesta = PedidoCtrl.obtenerEspecialidades(idExcepcion, idProducto);
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

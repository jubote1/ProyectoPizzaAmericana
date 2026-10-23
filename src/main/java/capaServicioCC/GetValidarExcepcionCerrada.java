package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PromocionesCtrl;;

/**
 * Verifica el codigo que el cliente da para una promocion cerrada (la que no es de oferta
 * abierta).
 *
 * Ruta: /ProyectoPizzaAmericana/GetValidarExcepcionCerrada   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): codigo (string), idexcepcion (integer).
 * Devuelve: application/json; claves: nombre.
 * Lee: excepcion_cerrada.
 *
 * Trampas y riesgos conocidos:
 * - El codigo se concatena en el SQL (inyectable). Si distingue o no mayusculas depende de la
 *   collation de la base (no verificado).
 * - Un idexcepcion no numerico cae en el catch y la respuesta sale vacia (cuerpo vacio, no
 *   {"nombre":""}).
 * - No hay limite de intentos: se pueden probar codigos sin freno.
 * - La validacion solo la pide el navegador; al insertar el detalle el servidor no comprueba que
 *   el codigo se haya validado.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetValidarExcepcionCerrada.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetValidarExcepcionCerrada")
public class GetValidarExcepcionCerrada extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetValidarExcepcionCerrada() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro del teléfono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el teléfono indicado.
	 * Lo anterior invocando el método obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			String codigo = request.getParameter("codigo");
			int idExcepcion = Integer.parseInt(request.getParameter("idexcepcion"));
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PromocionesCtrl promoCtrl = new PromocionesCtrl();
			String respuesta = promoCtrl.validarExcepcionCerrada(codigo, idExcepcion);
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

package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;

/**
 * Devuelve una direccion de cliente por su identificador, con los datos para mostrar y editar.
 *
 * Ruta: /ProyectoPizzaAmericana/GetClientePorID   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idcliente (integer).
 * Devuelve: application/json; claves: direccion, distanciatienda, email, idcliente, idtienda,
 * latitud, longitud, nombrecliente, nombremunicipio, nombretienda, nomenclatura, num3, ....
 * Lee: cliente, municipio, nomenclatura_direccion, tienda.
 *
 * Trampas y riesgos conocidos:
 * - Un idcliente ausente o no numerico cae en el catch y la respuesta sale vacia (cuerpo vacio,
 *   no []).
 * - Si el cliente no existe devuelve un objeto con valores nulos o en cero, no un error.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetClientePorID.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetClientePorID")
public class GetClientePorID extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetClientePorID() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * EL servicio toma como parámetro el idcliente y con base en esto retorna en formato JSON el cliente asociado al id
	 * pasado como parámetro, lo anterior invocando el método obtenerClienteporID(idCliente) en la capa controlador 
	 * Cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			int idCliente = Integer.parseInt(request.getParameter("idcliente"));
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ClienteCtrl ClienCtrl = new ClienteCtrl();
			String respuesta = ClienCtrl.obtenerClienteporID(idCliente);
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

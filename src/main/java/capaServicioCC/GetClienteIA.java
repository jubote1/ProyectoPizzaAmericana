package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;;

/**
 * Igual que GetCliente, pero solo devuelve las direcciones que se han usado en pedidos del ultimo
 * ano y como maximo 2.
 *
 * Ruta: /ProyectoPizzaAmericana/GetClienteIA   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): telefono (string).
 * Devuelve: application/json; claves: apellido, clientesiniden, correo, direccion,
 * distanciatienda, email, emailfact, fechanacimiento, idCliente, identificacion, idnomenclatura,
 * idtipopersona, ....
 * Lee: cliente, municipio, nomenclatura_direccion, pedido, tienda.
 *
 * Trampas y riesgos conocidos:
 * - Un cliente real cuya ultima compra fue hace mas de un ano sale como "no existe" y el agente
 *   podria crearlo duplicado.
 * - Mismas observaciones de seguridad que GetCliente.
 * - Ninguna pagina ni sistema del repositorio lo llama: se usa desde fuera (por ejemplo el bot o
 *   una automatizacion), asi que no se puede retirar sin preguntar.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetClienteIA.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetClienteIA")
public class GetClienteIA extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetClienteIA() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro del tel�fono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el tel�fono indicado.
	 * Lo anterior invocando el m�todo obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			String tel = request.getParameter("telefono");
			System.out.println(tel);
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ClienteCtrl ClienCtrl = new ClienteCtrl();
			String respuesta = ClienCtrl.obtenerClienteIA(tel);
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

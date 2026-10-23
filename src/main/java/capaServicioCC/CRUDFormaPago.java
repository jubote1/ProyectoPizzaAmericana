package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;

/**
 * Lista las formas de pago.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDFormaPago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idoperacion (string), nombre (string), tipoformapago (string), idformapago
 * (integer), tipopago (string).
 * Operaciones (operacion): 1, 2, 3, 4, 5, 6.
 * Devuelve: texto; claves: idformapago, mensajecorreo, mensajetexto, nombre, tipoformapago,
 * virtual.
 * Lee: forma_pago.
 *
 * Trampas y riesgos conocidos:
 * - Un tipopago distinto de T o PV deja la consulta vacia y el servicio responde [] sin avisar.
 * - Por lo que se ve en el codigo: la forma 1 es efectivo (la que admite "devuelta") y la 4 es
 *   pago virtual (Wompi). La 2 tambien se envia sin esperar pago, pero en este codigo no se ve
 *   su nombre.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDFormaPago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDFormaPago")
public class CRUDFormaPago extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDFormaPago() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibirá como parámetro principal el idoperacion con base en los siguientes valores 
	 *  idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la id operacion se pedirán los otros parámetros y se invocará el método en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		response.addHeader("Access-Control-Allow-Origin", "*");		
				response.addHeader("Access-Control-Allow-Origin", "*");
				String idoperacion = request.getParameter("idoperacion");
				ParametrosCtrl ParametrosCtrl = new ParametrosCtrl();
				int operacion;
				String respuesta="";
				try
				{
					operacion = Integer.parseInt(idoperacion);
				}catch(Exception e){
					operacion = 0;
				}
				if (operacion ==1)
				{
					String nombre = request.getParameter("nombre");
					String tipoformapago = request.getParameter("tipoformapago");
					//respuesta = ParametrosCtrl.insertarEstadoPedido(descripcion);
				}else if (operacion ==2)
				{
					int idformapagoedit= Integer.parseInt(request.getParameter("idformapago"));
					String nombreedit= request.getParameter("nombre");
					String tipoformaedit= request.getParameter("tipoformapago");
					//respuesta = ParametrosCtrl.editarEstadoPedido(idestedit, descedit);
				}else if (operacion ==3 )
				{
					int idformaeli = Integer.parseInt(request.getParameter("idformapago"));
					//respuesta = ParametrosCtrl.eliminarEstadoPedido(idesteli);
				}else if (operacion == 4)
				{
					int idformacon = Integer.parseInt(request.getParameter("idformapago"));
					//respuesta = ParametrosCtrl.retornarEstadoPedido(idestcon);
				}else if(operacion == 5)
				{
					String tipoPago = request.getParameter("tipopago");
					respuesta = ParametrosCtrl.retornarFormasPago(tipoPago);
				}else if(operacion == 6)
				{
					String tipoPago = request.getParameter("tipopago");
					respuesta = ParametrosCtrl.retornarFormasPagoContact(tipoPago);
				}
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

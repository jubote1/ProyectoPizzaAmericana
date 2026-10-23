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
 * Servicio con 6 operaciones que se escogen con el parametro operacion.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDProductoNoExistente   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Sin validacion propia.
 * Recibe (query/form): idoperacion (string), idtienda (integer), idproducto (integer).
 * Operaciones (operacion): 1, 2, 3, 4, 5, 6.
 * Devuelve: texto; claves: idproducto, idtienda, insertado, mail.smtp.host,
 * mail.smtp.ssl.protocols, producto, resultado, tienda.
 * Lee: parametros, parametros_correo, producto, producto_no_existente, tienda.
 * Escribe: log_bloqueo_producto_tienda (INSERT), producto_no_existente (DELETE/INSERT/UPDATE).
 * Sistemas externos: Correo SMTP, Gmail.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDProductoNoExistente.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDProductoNoExistente")
public class CRUDProductoNoExistente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDProductoNoExistente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se soportan las operaciones para el CRUD de la entidad Producto, se recibe como parámetro principal
	 * el idoperación de acuerdo a los siguientes valores 1 insertar 2 editar 3 Eliminar  4 Consultar, 
	 * con base en la operación se solicitarán el resto de parámetros y se invocará el método correspondiente en la capa controladora.
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
					int idtienda;
					try
					{
						idtienda = Integer.parseInt(request.getParameter("idtienda"));
					}catch(Exception e)
					{
						System.out.println(e.toString());
						idtienda = 0;
					}
					
					int idproducto;
					try
					{
						idproducto = Integer.parseInt(request.getParameter("idproducto"));
					}catch(Exception e)
					{
						idproducto = 0;
					}
					
					
					respuesta = ParametrosCtrl.insertaProductoNoExistente(idtienda, idproducto);
				}else if (operacion ==2)
				{
					
					int idtienda;
					try
					{
						idtienda = Integer.parseInt(request.getParameter("idtienda"));
					}catch(Exception e)
					{
						idtienda = 0;
					}
					
					int idproducto;
					try
					{
						idproducto = Integer.parseInt(request.getParameter("idproducto"));
					}catch(Exception e)
					{
						idproducto = 0;
					}
					respuesta = ParametrosCtrl.editarProductoNoExistente(idtienda, idproducto);
				}else if (operacion ==3 )
				{
					int idtienda;
					try
					{
						idtienda = Integer.parseInt(request.getParameter("idtienda"));
					}catch(Exception e)
					{
						idtienda = 0;
					}
					
					int idproducto;
					try
					{
						idproducto = Integer.parseInt(request.getParameter("idproducto"));
					}catch(Exception e)
					{
						idproducto = 0;
					}
					respuesta = ParametrosCtrl.eliminarProductoNoExistente(idtienda, idproducto);
				}else if (operacion == 4)
				{
					int idtienda;
					try
					{
						idtienda = Integer.parseInt(request.getParameter("idtienda"));
					}catch(Exception e)
					{
						idtienda = 0;
					}
					
					int idproducto;
					try
					{
						idproducto = Integer.parseInt(request.getParameter("idproducto"));
					}catch(Exception e)
					{
						idproducto = 0;
					}
					respuesta = ParametrosCtrl.retornarProductoNoExistente(idtienda, idproducto);
				}else if(operacion == 5)
				{
					respuesta = ParametrosCtrl.retornarProductosNoExistentes();
				}else if(operacion == 6)
				{
					System.out.println("hola pase");
					respuesta = ParametrosCtrl.retornarProductosNoExistentesGrid();
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

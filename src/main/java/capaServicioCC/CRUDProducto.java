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
 * Ruta: /ProyectoPizzaAmericana/CRUDProducto   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Sin validacion propia.
 * Recibe (query/form): idoperacion (string), idreceta (integer), nombre (string), descripcion
 * (string), impuesto (number), tipo (string), preciogeneral (number), incluye_liquido (string),
 * idtipoliquido (integer), habilitado (string), idproducto (integer).
 * Operaciones (operacion): 1, 2, 3, 4, 5, 6.
 * Devuelve: texto; claves: descripcion, habilitado, idproducto, idreceta, idtipo_liquido,
 * impuesto, incluye_liquido, nombre, nombreliquido, nombrereceta, preciogeneral, resultado, ....
 * Lee: producto, receta, tipo_liquido.
 * Escribe: producto (DELETE/INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDProducto.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDProducto")
public class CRUDProducto extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDProducto() {
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
				System.out.println("operacion " + idoperacion) ;
				try
				{
					operacion = Integer.parseInt(idoperacion);
				}catch(Exception e){
					operacion = 0;
				}
				if (operacion ==1)
				{
					int idreceta;
					try
					{
						idreceta = Integer.parseInt(request.getParameter("idreceta"));
					}catch(Exception e)
					{
						idreceta = 0;
					}
					
					String nombre = request.getParameter("nombre");
					String descripcion = request.getParameter("descripcion");
					float impuesto = Float.parseFloat(request.getParameter("impuesto"));
					String tipo = request.getParameter("tipo");
					double preciogeneral = Double.parseDouble(request.getParameter("preciogeneral"));
					String incluye_liquido = request.getParameter("incluye_liquido");
					int idtipoliquido = Integer.parseInt(request.getParameter("idtipoliquido"));
					String habilitado = request.getParameter("habilitado");
					respuesta = ParametrosCtrl.insertaProducto(idreceta, nombre, descripcion, impuesto, tipo, preciogeneral, incluye_liquido, idtipoliquido,habilitado);
				}else if (operacion ==2)
				{
					int idproducto= Integer.parseInt(request.getParameter("idproducto"));
					int idreceta;
					try
					{
						idreceta = Integer.parseInt(request.getParameter("idreceta"));
					}catch(Exception e)
					{
						idreceta = 0;
					}
					String nombre = request.getParameter("nombre");
					String descripcion = request.getParameter("descripcion");
					float impuesto = Float.parseFloat(request.getParameter("impuesto"));
					String tipo = request.getParameter("tipo");
					double preciogeneral = Double.parseDouble(request.getParameter("preciogeneral"));
					String incluye_liquido = request.getParameter("incluye_liquido");
					int idtipoliquido = Integer.parseInt(request.getParameter("idtipoliquido"));
					String habilitado = request.getParameter("habilitado");
					respuesta = ParametrosCtrl.editarProducto(idproducto, idreceta, nombre, descripcion, impuesto, tipo, preciogeneral, incluye_liquido, idtipoliquido,habilitado);
				}else if (operacion ==3 )
				{
					int idproducto = Integer.parseInt(request.getParameter("idproducto"));
					respuesta = ParametrosCtrl.eliminarProducto(idproducto);
				}else if (operacion == 4)
				{
					int idprocon = Integer.parseInt(request.getParameter("idproducto"));
					respuesta = ParametrosCtrl.retornarProducto(idprocon);
				}else if(operacion == 5)
				{
					respuesta = ParametrosCtrl.retornarProductos();
				}else if(operacion == 6)
				{
					respuesta = ParametrosCtrl.retornarProductosGrid();
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

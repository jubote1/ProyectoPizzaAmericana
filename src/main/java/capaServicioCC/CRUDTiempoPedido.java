package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;
import capaModeloCC.Usuario;

/**
 * Gestiona el tiempo de entrega prometido de cada tienda.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDTiempoPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idoperacion (string), nuevotiempo (integer), idtienda (integer).
 * Operaciones (operacion): 1, 2, 3.
 * Devuelve: texto; claves: estado, idtienda, mail.smtp.host, mail.smtp.ssl.protocols, resultado,
 * tiempopedido, tienda.
 * Lee: parametros, parametros_correo, tiempo_pedido_tienda, tienda, tienda_bloqueada.
 * Escribe: log_tiempo_tienda (INSERT), tiempo_pedido_tienda (UPDATE).
 * Sistemas externos: Correo SMTP, Gmail.
 *
 * Trampas y riesgos conocidos:
 * - SEGURIDAD: cambiar el tiempo de una tienda es un GET sin validar sesion.
 * - La respuesta de 1 mezcla HTML con JSON.
 * - El 100 para tienda bloqueada es un valor magico: no se distingue de una tienda que de verdad
 *   tarda 100 minutos.
 * - En 3, una tienda sin fila en tiempo_pedido_tienda devuelve 0.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDTiempoPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDTiempoPedido")
public class CRUDTiempoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDTiempoPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibir� como par�metro principal el idoperacion con base en los siguientes valores 
	 *  idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la id operacion se pedir�n los otros par�metros y se invocar� el m�todo en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operaci�n idoperacion 1 actualizar 2 consultar
		response.addHeader("Access-Control-Allow-Origin", "*");		
		HttpSession sesion = request.getSession();
		//Capturamos la informaci�n de la sesion del usuario que est� ejecutando la inserci�n
		Usuario usuario = (Usuario) sesion.getAttribute("usuario");
		String user = "" ;
		//Al no existir el usuario logueado es posible que produza una excepcion
		try
		{
			user = usuario.getNombreUsuario();
		}catch(Exception e)
		{
		
		}
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
			int nuevotiempo = Integer.parseInt(request.getParameter("nuevotiempo"));
			int idtienda = Integer.parseInt(request.getParameter("idtienda"));
			respuesta = ParametrosCtrl.actualizarTiempoPedido(nuevotiempo,idtienda, user);
		}else if (operacion ==2)
		{
			respuesta = ParametrosCtrl.retornarTiempoPedido();
		}else if (operacion ==3)
		{
			int idtienda = Integer.parseInt(request.getParameter("idtienda"));
			respuesta = ParametrosCtrl.retornarTiempoPedidoTienda(idtienda);
		}
		//System.out.println(respuesta);
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

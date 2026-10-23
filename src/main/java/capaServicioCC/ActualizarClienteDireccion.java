package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Servicio que modifica datos de Direccion y cobertura (administracion): invoca
 * ClienteCtrl.actualizarClienteDireccion.
 *
 * Ruta: /ProyectoPizzaAmericana/ActualizarClienteDireccion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): direccion (string), zona (string), observacion (string), municipio
 * (string), numnomenclatura1 (string), numnomenclatura2 (string), num3 (string), latitud (number),
 * longitud (number), idcliente (integer), idnomenclatura (integer).
 * Devuelve: application/json;charset=UTF-8; claves: respuesta.
 * Lee: municipio.
 * Escribe: cliente (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ActualizarClienteDireccion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ActualizarClienteDireccion")
public class ActualizarClienteDireccion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActualizarClienteDireccion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		Logger logger = Logger.getLogger("log_file");
		request.setCharacterEncoding("UTF-8");
		//Final de captura del usuario
        String direccion = request.getParameter("direccion");
        String zona = request.getParameter("zona");
        String observacion = request.getParameter("observacion");
        String municipio = request.getParameter("municipio");
        float latitud;
        float longitud;
        int idCliente;
        int memcode;
        int idnomenclatura = 0;
        String numNomenclatura = request.getParameter("numnomenclatura1");
        String numNomenclatura2 = request.getParameter("numnomenclatura2");
        String num3 = request.getParameter("num3");
        try{
        	latitud = Float.parseFloat(request.getParameter("latitud"));
        }catch(Exception e)
        {
        	latitud = 0;
        }
        try{
        	longitud = Float.parseFloat(request.getParameter("longitud"));
        }catch(Exception e)
        {
        	longitud = 0;
        }
        try{
        	idCliente = Integer.parseInt(request.getParameter("idcliente"));
        }catch(Exception e)
        {
        	idCliente = 0;
        }
        try{
        	idnomenclatura = Integer.parseInt(request.getParameter("idnomenclatura"));
        }catch(Exception e)
        {
        	idnomenclatura = 0;
        }
        ClienteCtrl ClienCtrl = new ClienteCtrl();
        String respuesta  = ClienCtrl.actualizarClienteDireccion(idCliente, direccion, municipio, latitud, longitud, zona, observacion, idnomenclatura, numNomenclatura, numNomenclatura2, num3);
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

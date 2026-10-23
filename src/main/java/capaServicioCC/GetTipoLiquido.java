package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Catalogo y productos (administracion): invoca
 * ParametrosCtrl.ObtenerTiposLiquido.
 *
 * Ruta: /ProyectoPizzaAmericana/GetTipoLiquido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: capacidad, idtipo_liquido, nombre.
 * Lee: tipo_liquido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetTipoLiquido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetTipoLiquido")
public class GetTipoLiquido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetTipoLiquido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que no recibe parámetros e invoca al método ObtenerTiposLiquido de la capa Parametros Controlador, retorna
	 * en formato JSON los tipos liquidos parametrizados en el sistema.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ParametrosCtrl ParametrosCtrl = new ParametrosCtrl();
			String respuesta = ParametrosCtrl.ObtenerTiposLiquido();
			PrintWriter out = response.getWriter();
			System.out.println(respuesta);
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

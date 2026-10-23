package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;;

/**
 * Servicio de consulta de Direccion y cobertura (administracion): invoca
 * ParametrosCtrl.ConsultaDirFueraZona.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDirsFueraZona   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), municipio (string).
 * Devuelve: texto; claves: apellido, direccion, fecha_ingreso, id, idcliente, latitud, longitud,
 * municipio, nombre, telefono.
 * Lee: direccion_fuera_zona, pedido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDirsFueraZona.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDirsFueraZona")
public class ConsultarDirsFueraZona extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarDirsFueraZona() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los parámetros para llamar al método insertarEspecialidad de la capa Parametros controlador,
	 * e insertar la especialidad.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		String fechaInicial = request.getParameter("fechainicial");
		String fechaFinal = request.getParameter("fechafinal");
		String municipio = request.getParameter("municipio");
		ParametrosCtrl Parametros = new ParametrosCtrl();
		String respuesta = Parametros.ConsultaDirFueraZona(fechaInicial, fechaFinal, municipio);
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

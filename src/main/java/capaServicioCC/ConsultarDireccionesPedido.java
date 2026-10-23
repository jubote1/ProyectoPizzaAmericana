package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;;

/**
 * Lista las direcciones de los pedidos de un rango de fecha y hora para pintarlas en el mapa de
 * zonas.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDireccionesPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), idmunicipio (string), idtienda
 * (string), horaini (string), horafin (string).
 * Devuelve: texto; claves: apellido, direccion, fechapedido, idcliente, idpedido, latitud,
 * longitud, municipio, nombre, telefono, valor.
 * Lee: cliente, municipio, pedido, tmp_pedidos_poligono.
 * Escribe: tmp_pedidos_poligono (DELETE).
 *
 * Trampas y riesgos conocidos:
 * - Entrega nombre, telefono y direccion de clientes sin sesion.
 * - La tabla temporal es unica: dos consultas a la vez pueden pisarse.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDireccionesPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDireccionesPedido")
public class ConsultarDireccionesPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarDireccionesPedido() {
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
		String idMunicipio = request.getParameter("idmunicipio");
		String idTienda = request.getParameter("idtienda");
		String horaIni ="";
		String horaFin = "";
		try
		{
			horaIni = request.getParameter("horaini");
			if(horaIni.length() == 0 || horaIni.equals(new String("null")) )
			{
				horaIni = "";
			}
		}catch(Exception e)
		{
			horaIni = "";
		}
		
		try
		{
			horaFin = request.getParameter("horafin");
			if(horaFin.length() == 0 || horaFin.equals(new String("null")))
			{
				horaFin = "";
			}
		}catch(Exception e)
		{
			horaFin = "";
		}
		System.out.println("HORA INI Y FIN " + horaIni + horaFin);
		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.ConsultarDireccionesPedido(fechaInicial, fechaFinal, idMunicipio, idTienda, horaIni, horaFin);
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

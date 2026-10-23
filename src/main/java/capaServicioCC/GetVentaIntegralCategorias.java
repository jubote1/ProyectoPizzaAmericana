package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.VentaIntegralCtrl;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * VentaIntegralCtrl.listarCategorias.
 *
 * Ruta: /ProyectoPizzaAmericana/GetVentaIntegralCategorias   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: abreviatura, activo, excluyeanuladostienda,
 * filtroestaciontienda, idcategoria, itemscontactcenter, itemstienda, medicioncc, mediciontienda,
 * nombre, orden, tipodato.
 * Lee: venta_integral_categoria, venta_integral_categoria_item.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetVentaIntegralCategorias.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetVentaIntegralCategorias")
public class GetVentaIntegralCategorias extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public GetVentaIntegralCategorias() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			VentaIntegralCtrl ventaIntegralCtrl = new VentaIntegralCtrl();
			String respuesta = ventaIntegralCtrl.listarCategorias();
			PrintWriter out = response.getWriter();
			out.write(respuesta);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}

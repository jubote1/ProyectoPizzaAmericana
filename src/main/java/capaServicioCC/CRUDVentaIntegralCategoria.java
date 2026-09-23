package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.VentaIntegralCtrl;

/**
 * Servlet implementation class CRUDVentaIntegralCategoria
 * CRUD del catalogo de categorias de Venta Integral, mismo patron que
 * CRUDEspecialidad: idoperacion 1 insertar 2 editar 3 eliminar 4 consultar.
 */
@WebServlet("/CRUDVentaIntegralCategoria")
public class CRUDVentaIntegralCategoria extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public CRUDVentaIntegralCategoria() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		String idoperacion = request.getParameter("idoperacion");
		VentaIntegralCtrl ventaIntegralCtrl = new VentaIntegralCtrl();
		int operacion;
		String respuesta = "";
		try {
			operacion = Integer.parseInt(idoperacion);
		} catch (Exception e) {
			operacion = 0;
		}
		if (operacion == 1) {
			String nombre = request.getParameter("nombre");
			String abreviatura = request.getParameter("abreviatura");
			String tipodato = request.getParameter("tipodato");
			String mediciontienda = request.getParameter("mediciontienda");
			String excluyeanuladostienda = request.getParameter("excluyeanuladostienda");
			String filtroestaciontienda = request.getParameter("filtroestaciontienda");
			String medicioncc = request.getParameter("medicioncc");
			int orden = parsearEntero(request.getParameter("orden"));
			String itemstienda = request.getParameter("itemstienda");
			String itemscontactcenter = request.getParameter("itemscontactcenter");
			logger.info("insertar categoria venta integral " + nombre);
			respuesta = ventaIntegralCtrl.insertarCategoria(nombre, abreviatura, tipodato, mediciontienda,
					excluyeanuladostienda, filtroestaciontienda, medicioncc, orden, itemstienda, itemscontactcenter);
		} else if (operacion == 2) {
			int idcategoria = parsearEntero(request.getParameter("idcategoria"));
			String nombre = request.getParameter("nombre");
			String abreviatura = request.getParameter("abreviatura");
			String tipodato = request.getParameter("tipodato");
			String mediciontienda = request.getParameter("mediciontienda");
			String excluyeanuladostienda = request.getParameter("excluyeanuladostienda");
			String filtroestaciontienda = request.getParameter("filtroestaciontienda");
			String medicioncc = request.getParameter("medicioncc");
			int orden = parsearEntero(request.getParameter("orden"));
			String itemstienda = request.getParameter("itemstienda");
			String itemscontactcenter = request.getParameter("itemscontactcenter");
			logger.info("editar categoria venta integral " + idcategoria);
			respuesta = ventaIntegralCtrl.editarCategoria(idcategoria, nombre, abreviatura, tipodato, mediciontienda,
					excluyeanuladostienda, filtroestaciontienda, medicioncc, orden, itemstienda, itemscontactcenter);
		} else if (operacion == 3) {
			int idcategoria = parsearEntero(request.getParameter("idcategoria"));
			respuesta = ventaIntegralCtrl.eliminarCategoria(idcategoria);
		} else if (operacion == 4) {
			int idcategoria = parsearEntero(request.getParameter("idcategoria"));
			respuesta = ventaIntegralCtrl.retornarCategoria(idcategoria);
		}
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	private int parsearEntero(String valor) {
		try {
			return (Integer.parseInt(valor));
		} catch (Exception e) {
			return (0);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}

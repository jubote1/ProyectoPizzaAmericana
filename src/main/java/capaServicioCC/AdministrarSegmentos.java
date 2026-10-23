package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.DefinicionSegmentoCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servicio que modifica datos de Bot y CRM (Kommo): invoca AccesoCRM.puede, AccesoCRM.negado.
 *
 * Ruta: /ProyectoPizzaAmericana/AdministrarSegmentos   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): accion (string).
 * Devuelve: application/json; charset=UTF-8; claves: activo, ayuda, calculado_en, campo, campos,
 * color, condicion, contactables, cumplen, descripcion, error, etiqueta, ....
 * Lee: crm.parametro_segmento, crm.persona_resumen, crm.segmento_campo, crm.segmento_definicion,
 * crm.segmento_regla, usuario.
 * Escribe: crm.segmento_definicion (DELETE/INSERT/UPDATE), crm.segmento_regla (DELETE/INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha AdministrarSegmentos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/AdministrarSegmentos")
public class AdministrarSegmentos extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public AdministrarSegmentos() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, false);
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, true);
	}

	private void atender(final HttpServletRequest request, final HttpServletResponse response,
			final boolean esPost) throws IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		String accion = request.getParameter("accion");
		if (accion == null) {
			accion = "listar";
		}
		accion = accion.trim().toLowerCase();

		try {
			if ("listar".equals(accion)) {
				out.write(DefinicionSegmentoCtrl.listar());
			} else if ("probar".equals(accion)) {
				//Probar no escribe nada, pero recorre las 450 mil filas, asi que
				//tambien va por POST para que no quede en el historial.
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.probar(request) : soloPost());
			} else if ("guardar".equals(accion)) {
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.guardar(request) : soloPost());
			} else if ("borrar".equals(accion)) {
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.borrar(request) : soloPost());
			} else if ("clasificar".equals(accion)) {
				out.write(exigirPost(esPost) ? DefinicionSegmentoCtrl.clasificar(request) : soloPost());
			} else {
				out.write("{\"error\":\"Accion desconocida.\"}");
			}
		} catch (final Exception e) {
			//El mensaje del error NO se le devuelve al navegador: puede traer el
			//SQL o el nombre de una tabla. Va al log, que es donde se mira.
			System.out.println("AdministrarSegmentos: " + e);
			out.write("{\"error\":\"No se pudo completar la operacion. Revise el log.\"}");
		}
	}

	private boolean exigirPost(final boolean esPost) {
		return (esPost);
	}

	private String soloPost() {
		return ("{\"error\":\"Esta operacion solo se acepta por POST.\"}");
	}
}

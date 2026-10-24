package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.GastoAreaCtrl;
import capaDAOCC.GastoAreaDAO;

/**
 * Gestiona los gastos de las areas que se reparten entre las tiendas: publicidad,
 * administrativa, logistica y contact center.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDGastoArea   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Exige sesion de usuario.
 * Operaciones (idoperacion): 1 guardar, 2 eliminar, 4 listar, 5 areas, 6 resumen del mes,
 * 7 guardar el valor de un mes de un gasto variable.
 * Devuelve: application/json.
 * Lee y escribe: gasto_area, gasto_area_mes.
 *
 * Estos valores terminan repartidos entre las once tiendas y entran al costo de cada
 * una, asi que no se tocan sin sesion.
 *
 * Trampas y riesgos conocidos:
 * - El mes importa en toda consulta: un gasto fijo solo cuenta si estaba vigente ese
 *   mes, y un variable vale lo que se haya digitado PARA ese mes. Sin anio y mes, la
 *   respuesta no significa nada.
 * - El reparto por tienda que devuelve la operacion 6 es el PAREJO, solo de referencia
 *   para cuadrar contra el Excel. El reparto real va por participacion en la venta y lo
 *   hace la pantalla de porcentaje, que es la que conoce las ventas del periodo.
 *
 * Ver sql/gasto_area.sql.
 */
@WebServlet("/CRUDGastoArea")
public class CRUDGastoArea extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public CRUDGastoArea() {
		super();
	}

	private int entero(final HttpServletRequest request, final String nombre) {
		try {
			return Integer.parseInt(request.getParameter(nombre));
		} catch (final Exception e) {
			return 0;
		}
	}

	private double real(final HttpServletRequest request, final String nombre) {
		try {
			return Double.parseDouble(request.getParameter(nombre));
		} catch (final Exception e) {
			return 0;
		}
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String valor = request.getParameter(nombre);
		return valor == null ? "" : valor.trim();
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion\"}");
			return;
		}
		final String usuario = String.valueOf(sesion.getAttribute("usuario"));

		final int operacion = this.entero(request, "idoperacion");
		final GastoAreaCtrl ctrl = new GastoAreaCtrl();
		try {
			if (operacion == 1) {
				final GastoAreaDAO.Gasto g = new GastoAreaDAO.Gasto();
				g.idGasto = this.entero(request, "idgasto");
				g.area = this.texto(request, "area");
				g.concepto = this.texto(request, "concepto");
				g.tipo = GastoAreaDAO.TIPO_VARIABLE.equalsIgnoreCase(this.texto(request, "tipo"))
						? GastoAreaDAO.TIPO_VARIABLE
						: GastoAreaDAO.TIPO_FIJO;
				g.valorMes = this.real(request, "valormes");
				g.vigenciaDesde = this.texto(request, "vigenciadesde");
				g.vigenciaHasta = this.texto(request, "vigenciahasta");
				g.activo = !"N".equalsIgnoreCase(this.texto(request, "activo"));
				g.usuario = usuario;
				out.write(ctrl.guardar(g));
			} else if (operacion == 2) {
				out.write(ctrl.eliminar(this.entero(request, "idgasto")));
			} else if (operacion == 4) {
				out.write(ctrl.listar(this.texto(request, "area"), this.entero(request, "anio"),
						this.entero(request, "mes")));
			} else if (operacion == 5) {
				out.write(ctrl.listarAreas());
			} else if (operacion == 6) {
				out.write(ctrl.resumen(this.texto(request, "area"), this.entero(request, "anio"),
						this.entero(request, "mes"), this.entero(request, "semanas"),
						this.entero(request, "tiendas")));
			} else if (operacion == 7) {
				out.write(ctrl.guardarValorDelMes(this.entero(request, "idgasto"), this.entero(request, "anio"),
						this.entero(request, "mes"), this.real(request, "valor"), usuario));
			} else {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Operacion no reconocida\"}");
			}
		} catch (final Exception e) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error procesando la solicitud\"}");
			System.out.println("CRUDGastoArea: " + e.toString());
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}
}

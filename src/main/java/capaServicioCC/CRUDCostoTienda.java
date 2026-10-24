package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.CostoTiendaCtrl;

/**
 * El costo de las tiendas por concepto y por semana: lo que hoy es la pestana
 * PORCENTAJE del libro de Excel.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDCostoTienda   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Exige sesion de usuario.
 * Operaciones (idoperacion): 1 matriz del rango, 2 conceptos, 3 guardar un digitado.
 * Devuelve: application/json.
 * Lee: datamart.egreso_historico_tienda, datamart.cierre_datafono_pos,
 *      datamart.venta_semanal_tienda, inventarioamericana.gasto_empleado_temporal,
 *      pizzaamericana.concepto_costo, pizzaamericana.concepto_costo_tipo_egreso,
 *      general.parametros.
 * Escribe: pizzaamericana.costo_tienda_digitado.
 *
 * Esto es el costo de operar cada tienda, asi que no sale sin sesion.
 *
 * Trampas y riesgos conocidos:
 * - La semana se identifica por su DOMINGO, igual que venta_semanal_tienda y
 *   gasto_empleado_temporal. Un rango que no caiga en domingos igual funciona -el
 *   rango se recorta a las semanas con venta- pero las fechas que vuelven son domingos.
 * - El rango se recorta a las semanas que tengan venta registrada. Un rango sin
 *   ninguna devuelve respuesta N, no una matriz vacia.
 * - Si la tarifa COMISIONDATAFONO esta en cero, la comision de datafonos NO viene en
 *   la matriz y datafonoparametrizado llega en 'N'. La pantalla tiene que decirlo:
 *   un cero ahi se lee como "no hubo comision" y lo que pasa es que falta la tarifa.
 * - Un tipo de egreso que no este en concepto_costo_tipo_egreso no entra al costo. En
 *   septiembre de 2026 eso dejaba por fuera $7,7 de $35,2 millones, a proposito.
 *
 * Ver sql/costo_tienda.sql.
 */
@WebServlet("/CRUDCostoTienda")
public class CRUDCostoTienda extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public CRUDCostoTienda() {
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
		final CostoTiendaCtrl ctrl = new CostoTiendaCtrl();
		try {
			if (operacion == 1) {
				final String desde = this.texto(request, "desde");
				final String hasta = this.texto(request, "hasta");
				if (desde.length() < 10 || hasta.length() < 10) {
					out.write("{\"respuesta\":\"N\",\"mensaje\":\"Faltan las fechas del rango.\"}");
					return;
				}
				out.write(ctrl.matriz(desde, hasta, this.entero(request, "idtienda")));
			} else if (operacion == 2) {
				out.write(ctrl.listarConceptos());
			} else if (operacion == 3) {
				out.write(ctrl.guardarDigitado(this.entero(request, "idtienda"),
						this.entero(request, "idconcepto"), this.entero(request, "anio"),
						this.entero(request, "mes"), this.real(request, "valor"), usuario));
			} else {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Operacion no reconocida\"}");
			}
		} catch (final Exception e) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error procesando la solicitud\"}");
			System.out.println("CRUDCostoTienda: " + e.toString());
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}
}

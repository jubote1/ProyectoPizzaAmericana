package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CompensacionTiempoCtrl;
import capaControladorCC.EnvioPublicidadCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servicio que modifica datos de Sin clasificar (revisar a mano): invoca AccesoCRM.puede,
 * AccesoCRM.negado.
 *
 * Ruta: /ProyectoPizzaAmericana/CompensacionTiempo   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): accion (string), fecha (string), minutos (string), idtienda (string),
 * soloexceso (string), dias (string), idoferta (string), pedidos (string).
 * Devuelve: application/json; charset=UTF-8; claves: asunto, cliente, desde, dias, email,
 * emitidos, enviadas, error, exceso, general, hasta, hora_entrega, ....
 * Lee: cliente, crm.campana, crm.campana_destinatario, crm.campana_envio, crm.compensacion_tiempo,
 * crm.persona_resumen, crm.stage_cliente_tienda, datamart.despacho_real,
 * datamart.despacho_real_det, datamart.pedido.
 * Escribe: crm.campana (INSERT), crm.campana_destinatario (INSERT/UPDATE), crm.campana_envio
 * (INSERT/UPDATE), crm.compensacion_tiempo (INSERT), oferta_bitacora (INSERT), oferta_cliente
 * (INSERT/UPDATE), pizzaamericana.cliente (UPDATE).
 * Sistemas externos: Correo SMTP, Gmail.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CompensacionTiempo.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CompensacionTiempo")
public class CompensacionTiempo extends HttpServlet {
	private static final long serialVersionUID = 1L;

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
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		final String accion = texto(request.getParameter("accion"));

		if ("consultar".equals(accion)) {
			out.write(CompensacionTiempoCtrl.consultar(texto(request.getParameter("fecha")),
					entero(request.getParameter("minutos")), entero(request.getParameter("idtienda")),
					"S".equals(texto(request.getParameter("soloexceso")))));

		} else if ("resumen".equals(accion)) {
			out.write(CompensacionTiempoCtrl.resumen(texto(request.getParameter("fecha")),
					entero(request.getParameter("dias")), entero(request.getParameter("minutos"))));

		} else if ("ofertas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.ofertas());

		} else if ("vistaprevia".equals(accion)) {
			out.write(CompensacionTiempoCtrl.vistaPrevia(entero(request.getParameter("idoferta")),
					texto(request.getParameter("fecha"))));

		} else if ("enviar".equals(accion)) {
			if (!esPost) {
				//Manda correos con una oferta: un enlace o un rastreador no lo puede disparar.
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				out.write("{\"error\":\"Use POST para enviar.\"}");
				return;
			}
			final ArrayList<String> seleccion = new ArrayList<String>();
			final String lista = texto(request.getParameter("pedidos"));
			if (lista.length() > 0) {
				final String[] partes = lista.split(",");
				for (int i = 0; i < partes.length; i++) {
					if (partes[i].trim().length() > 0) {
						seleccion.add(partes[i].trim());
					}
				}
			}
			out.write(CompensacionTiempoCtrl.enviar(texto(request.getParameter("fecha")),
					entero(request.getParameter("minutos")),
					"S".equals(texto(request.getParameter("soloexceso"))),
					entero(request.getParameter("idoferta")), seleccion, AccesoCRM.usuarioEnSesion(request)));

		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	private static String texto(final String v) {
		return (v == null ? "" : v.trim());
	}

	private static int entero(final String v) {
		try {
			return (Integer.parseInt(texto(v)));
		} catch (final Exception e) {
			return (0);
		}
	}
}

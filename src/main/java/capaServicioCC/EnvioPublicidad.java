package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EnvioPublicidadCtrl;
import capaControladorCC.SegmentacionPersonaCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servlet implementation class EnvioPublicidad
 *
 * Todas las acciones de la pantalla de Envio de Publicidad, en un solo sitio y
 * distinguidas por el parametro "accion".
 *
 * Van juntas y no en siete servlets porque comparten exactamente la misma
 * guarda -acceso al CRM- y el mismo armado de filtro. Partirlas obligaria a
 * repetir esa guarda siete veces, y una guarda repetida siete veces es una
 * guarda que algun dia se olvida en la octava.
 */
@WebServlet("/EnvioPublicidad")
public class EnvioPublicidad extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		final PrintWriter out = response.getWriter();

		//Esta pantalla no solo LEE datos personales: los usa para escribirle a
		//la gente. Con mas razon se valida en el servidor y no solo en el menu.
		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		final String accion = request.getParameter("accion") == null
				? "" : request.getParameter("accion").trim();

		if ("alcance".equals(accion)) {
			out.write(EnvioPublicidadCtrl.alcance(SegmentacionPersonaCtrl.filtroDe(request),
					extraDe(request)));

		} else if ("enviar".equals(accion)) {
			out.write(EnvioPublicidadCtrl.crearYEnviar(
					largo(request.getParameter("idcampana")),
					request.getParameter("nombre"),
					request.getParameter("canal"),
					entero(request.getParameter("idplantilla")),
					request.getParameter("asunto"),
					request.getParameter("cuerpo"),
					entero(request.getParameter("tope")),
					resumenDeFiltros(request),
					SegmentacionPersonaCtrl.filtroDe(request),
					extraDe(request),
					AccesoCRM.usuarioEnSesion(request)));

		} else if ("campanas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.campanas());

		} else if ("avance".equals(accion)) {
			out.write(EnvioPublicidadCtrl.avance(largo(request.getParameter("idenvio"))));

		} else if ("ultimas".equals(accion)) {
			final int cuantas = entero(request.getParameter("cuantas"));
			out.write(EnvioPublicidadCtrl.ultimas(cuantas > 0 ? cuantas : 20));

		} else if ("resultado".equals(accion)) {
			//Se puede medir una tanda sola o la campana completa. Son dos
			//preguntas distintas: "como le fue al envio del 24" y "sirve el
			//COMBO FUTBOLERO".
			final int horas = entero(request.getParameter("horas"));
			final long idCampana = largo(request.getParameter("idcampana"));
			final long idEnvio = largo(request.getParameter("idenvio"));
			final boolean porCampana = idEnvio == 0 && idCampana > 0;
			out.write(EnvioPublicidadCtrl.resultado(porCampana ? idCampana : idEnvio,
					horas > 0 ? horas : 24, porCampana));

		} else if ("probar".equals(accion)) {
			out.write(EnvioPublicidadCtrl.probar(
					request.getParameter("canal"),
					request.getParameter("destino"),
					entero(request.getParameter("idplantilla")),
					request.getParameter("asunto"),
					request.getParameter("cuerpo")));

		} else if ("tiendas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.tiendas());

		} else if ("detener".equals(accion)) {
			out.write(EnvioPublicidadCtrl.detener());

		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Guarda como texto el filtro con el que se armo la campana.
	 *
	 * No es adorno: dentro de tres meses, viendo que una campana funciono, lo
	 * primero que alguien va a querer saber es a quien se le mando. Sin esto la
	 * respuesta se perdio.
	 */
	private String resumenDeFiltros(final HttpServletRequest request) {
		final StringBuilder t = new StringBuilder();
		final String[] campos = { "segmentos", "idtienda", "pedidosmin", "valormin",
				"diasmin", "diasmax", "canal", "concorreo", "autorizados", "universo",
				"diassinpublicidad", "tiposcliente", "productos" };
		for (int i = 0; i < campos.length; i++) {
			final String valor = request.getParameter(campos[i]);
			if (valor != null && valor.trim().length() > 0) {
				if (t.length() > 0) {
					t.append(" | ");
				}
				t.append(campos[i]).append("=").append(valor.trim());
			}
		}
		return (t.length() == 0 ? "todo el CRM" : t.toString());
	}

	/**
	 * Los filtros que venian de la pantalla anterior.
	 *
	 * Van aparte del filtro de segmento porque salen a buscar a cliente y a
	 * pedido, mientras el otro se resuelve dentro del resumen por persona.
	 * Tenerlos separados deja a la vista cuales son los caros.
	 */
	private capaDAOCC.CampanaDAO.FiltroExtra extraDe(final HttpServletRequest request) {
		final capaDAOCC.CampanaDAO.FiltroExtra e = new capaDAOCC.CampanaDAO.FiltroExtra();
		e.diasSinPublicidad = entero(request.getParameter("diassinpublicidad"));
		e.excluirPlataformas = "S".equals(request.getParameter("excluirplataformas"));
		e.pedidosMax = entero(request.getParameter("pedidosmax"));
		e.puntosMin = entero(request.getParameter("puntosmin"));
		e.soloMiembrosClub = "S".equals(request.getParameter("soloclub"));
		e.correoContiene = texto(request.getParameter("correo"));
		e.compraDesde = texto(request.getParameter("comprodesde"));
		e.compraHasta = texto(request.getParameter("comprohasta"));
		llenarTexto(e.tiposCliente, request.getParameter("tiposcliente"));
		llenarEntero(e.productos, request.getParameter("productos"));
		llenarEntero(e.especialidades, request.getParameter("especialidades"));
		llenarEntero(e.excepciones, request.getParameter("promociones"));
		return (e);
	}

	/** Las listas llegan separadas por coma, como en el resto de la pantalla. */
	private void llenarEntero(final java.util.ArrayList<Integer> destino, final String valor) {
		if (valor == null || valor.trim().length() == 0) {
			return;
		}
		final String[] partes = valor.split(",");
		for (int i = 0; i < partes.length; i++) {
			final int n = entero(partes[i]);
			if (n > 0) {
				destino.add(Integer.valueOf(n));
			}
		}
	}

	private void llenarTexto(final java.util.ArrayList<String> destino, final String valor) {
		if (valor == null || valor.trim().length() == 0) {
			return;
		}
		final String[] partes = valor.split(",");
		for (int i = 0; i < partes.length; i++) {
			if (partes[i].trim().length() > 0) {
				destino.add(partes[i].trim());
			}
		}
	}

	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}

	private int entero(final String valor) {
		try {
			return (Integer.parseInt(valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	private long largo(final String valor) {
		try {
			return (Long.parseLong(valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}
}

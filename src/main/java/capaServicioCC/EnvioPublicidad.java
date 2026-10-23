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
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca AccesoCRM.puede,
 * AccesoCRM.negado.
 *
 * Ruta: /ProyectoPizzaAmericana/EnvioPublicidad   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): accion (string), idcampana (string), nombre (string), canalenvio (string),
 * idplantilla (string), asunto (string), cuerpo (string), tope (string), esperados (string),
 * idoferta (string), programar_para (string), idprogramacion (string), idenvio (string), cuantas
 * (string), horas (string), destino (string).
 * Devuelve: application/json; charset=UTF-8; claves: advertencia, anulados, asunto, campanas,
 * canal, claras, codigos, compraron, con_celular, con_correo, consecutivo, creada_en, ....
 * Lee: cliente, crm.campana, crm.campana_destinatario, crm.campana_envio, crm.compensacion_tiempo,
 * crm.persona_resumen, crm.segmento_definicion, integracion_crm, oferta, oferta_cliente.
 * Escribe: crm.campana (INSERT/UPDATE), crm.campana_destinatario (INSERT/UPDATE),
 * crm.campana_envio (INSERT/UPDATE), crm.campana_programada (UPDATE), oferta_bitacora (INSERT),
 * oferta_cliente (INSERT/UPDATE), pizzaamericana.cliente (UPDATE).
 * Sistemas externos: Brevo, Correo SMTP, Gmail, Rappi, WhatsApp.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha EnvioPublicidad.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/EnvioPublicidad")
public class EnvioPublicidad extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, false);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
					//canalenvio y no canal: "canal" es POR DONDE COMPRA, que lee
					//filtroDe. Cuando los dos se llamaban igual, el de envio le
					//borraba el filtro de mostrador y la campana salia a todos.
					request.getParameter("canalenvio"),
					entero(request.getParameter("idplantilla")),
					request.getParameter("asunto"),
					request.getParameter("cuerpo"),
					entero(request.getParameter("tope")),
					entero(request.getParameter("esperados")),
					resumenDeFiltros(request),
					SegmentacionPersonaCtrl.filtroDe(request),
					extraDe(request),
					AccesoCRM.usuarioEnSesion(request),
					entero(request.getParameter("idoferta"))));

		} else if ("programar".equals(accion)) {
			if (!esPost) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				out.write("{\"error\":\"Use POST para programar un envio.\"}");
				return;
			}
			out.write(EnvioPublicidadCtrl.programar(SegmentacionPersonaCtrl.parametrosDe(request),
					request.getParameter("programar_para"),
					AccesoCRM.usuarioEnSesion(request)));

		} else if ("programadas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.programadas());

		} else if ("cancelarprogramada".equals(accion)) {
			if (!esPost) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				out.write("{\"error\":\"Use POST para cancelar un envio programado.\"}");
				return;
			}
			out.write(EnvioPublicidadCtrl.cancelarProgramada(largo(request.getParameter("idprogramacion")),
					AccesoCRM.usuarioEnSesion(request)));

		} else if ("anularcodigos".equals(accion)) {
			out.write(EnvioPublicidadCtrl.anularCodigos(largo(request.getParameter("idenvio")),
					AccesoCRM.usuarioEnSesion(request)));

		} else if ("ofertas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.ofertas());

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
					request.getParameter("canalenvio"),
					request.getParameter("destino"),
					entero(request.getParameter("idplantilla")),
					request.getParameter("asunto"),
					request.getParameter("cuerpo")));

		} else if ("tiendas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.tiendas());

		} else if ("familias".equals(accion)) {
			out.write(EnvioPublicidadCtrl.familias());

		} else if ("segmentos".equals(accion)) {
			out.write(EnvioPublicidadCtrl.segmentos());

		} else if ("detener".equals(accion)) {
			out.write(EnvioPublicidadCtrl.detener());

		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	/**
	 * Guarda como texto el filtro con el que se armo la campana.
	 *
	 * No es adorno: dentro de tres meses, viendo que una campana funciono, lo
	 * primero que alguien va a querer saber es a quien se le mando. Sin esto la
	 * respuesta se perdio. El armado vive en el controlador para que un envio
	 * programado lo reuse tal cual.
	 */
	private String resumenDeFiltros(final HttpServletRequest request) {
		return (EnvioPublicidadCtrl.resumenDeFiltros(SegmentacionPersonaCtrl.parametrosDe(request)));
	}

	/**
	 * Los filtros que venian de la pantalla anterior.
	 *
	 * Van aparte del filtro de segmento porque salen a buscar a cliente y a
	 * pedido, mientras el otro se resuelve dentro del resumen por persona.
	 * Tenerlos separados deja a la vista cuales son los caros.
	 */
	private capaDAOCC.CampanaDAO.FiltroExtra extraDe(final HttpServletRequest request) {
		return (EnvioPublicidadCtrl.extraDe(SegmentacionPersonaCtrl.parametrosDe(request)));
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

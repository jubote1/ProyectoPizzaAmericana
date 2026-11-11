package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import capaControladorCC.motor.CatalogoPos;
import capaControladorCC.motor.MotorPedidoV1;
import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;
import conexionCC.ConexionBaseDatos;

/**
 * Endpoint API REST para cotizacion del Motor de Pedido de Pizza Americana (Etapa 2 / Etapa 3).
 *
 * Permite cotizar pedidos en tiempo real para cualquier canal (Contact Center, WhatsApp BOT,
 * nueva Tienda Virtual y Agente de Voz), aplicando el catalogo unificado pos_central,
 * productos incluidos, preguntas forzadas, horarios y descuentos automaticos.
 *
 * Rutas: /api/motor/cotizar y /MotorCotizar
 * Metodos: POST (cotizacion), OPTIONS (CORS preflight).
 */
@WebServlet({ "/api/motor/cotizar", "/MotorCotizar" })
public class MotorCotizar extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Gson GSON = new Gson();
	private static final Object LOCK = new Object();
	private static final long VIGENCIA_CATALOGO_MS = 10 * 60_000L; // 10 minutos

	private static volatile CatalogoPos catalogoCache;
	private static volatile long catalogoCargadoEn = 0L;

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.write("{\"servicio\":\"MotorCotizar\",\"version\":\"" + MotorPedidoV1.VERSION
				+ "\",\"metodo\":\"POST requerido para cotizar\"}");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");

		PrintWriter out = response.getWriter();

		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			String linea;
			while ((linea = reader.readLine()) != null) {
				sb.append(linea);
			}
		}

		String jsonEntrada = sb.toString().trim();
		if (jsonEntrada.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			MotorResultado errorRes = new MotorResultado();
			errorRes.estado = MotorResultado.ERROR;
			errorRes.advertencias.add(new MotorResultado.Advertencia("CUERPO_VACIO",
					"Se requiere un JSON con la estructura MotorSolicitud en el cuerpo de la peticion.", 0));
			out.write(GSON.toJson(errorRes));
			return;
		}

		MotorSolicitud solicitud;
		try {
			solicitud = GSON.fromJson(jsonEntrada, MotorSolicitud.class);
			if (solicitud == null) {
				throw new IllegalArgumentException("JSON no corresponde a MotorSolicitud.");
			}
		} catch (Exception ex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			MotorResultado errorRes = new MotorResultado();
			errorRes.estado = MotorResultado.ERROR;
			errorRes.advertencias.add(new MotorResultado.Advertencia("JSON_INVALIDO",
					"Error al deserializar MotorSolicitud: " + ex.getMessage(), 0));
			out.write(GSON.toJson(errorRes));
			return;
		}

		// Fecha y hora por defecto si no vienen en el contexto
		if (solicitud.contexto == null) {
			solicitud.contexto = new MotorSolicitud.Contexto();
		}
		if (solicitud.contexto.fechaHora == null || solicitud.contexto.fechaHora.trim().isEmpty()) {
			solicitud.contexto.fechaHora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(new java.util.Date());
		}

		try {
			CatalogoPos cat = obtenerCatalogo();
			MotorPedidoV1 motor = new MotorPedidoV1(cat);
			MotorResultado resultado = motor.cotizar(solicitud);
			response.setStatus(HttpServletResponse.SC_OK);
			out.write(GSON.toJson(resultado));
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			MotorResultado errorRes = new MotorResultado();
			errorRes.estado = MotorResultado.ERROR;
			errorRes.advertencias.add(new MotorResultado.Advertencia("ERROR_MOTOR",
					"Error interno en motor de cotizacion: " + t.getMessage(), 0));
			out.write(GSON.toJson(errorRes));
		}
	}

	private CatalogoPos obtenerCatalogo() throws Exception {
		long ahora = System.currentTimeMillis();
		if (catalogoCache != null && (ahora - catalogoCargadoEn < VIGENCIA_CATALOGO_MS)) {
			return catalogoCache;
		}
		synchronized (LOCK) {
			if (catalogoCache != null && (ahora - catalogoCargadoEn < VIGENCIA_CATALOGO_MS)) {
				return catalogoCache;
			}
			try (Connection con = new ConexionBaseDatos().obtenerConexionBDPrincipal()) {
				catalogoCache = CatalogoPos.cargar(con);
				catalogoCargadoEn = System.currentTimeMillis();
				return catalogoCache;
			}
		}
	}

	private void configurarCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
	}
}

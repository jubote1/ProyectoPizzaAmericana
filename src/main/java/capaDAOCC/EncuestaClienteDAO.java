package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * DAO de las encuestas de satisfaccion que responden los CLIENTES (la pagina
 * encuesta-satisfaccion-directa de pizzaamericana.co: link que se envia por WhatsApp,
 * QR de las mesas y QR de la factura). Trabaja sobre pizzaamericana.encuesta_servicio,
 * pregunta_servicio y log_encuesta_servicio.
 *
 * Una fila de encuesta_servicio es UNA respuesta a UNA pregunta; la encuesta completa
 * es el conjunto de filas de un mismo (idtienda, idpedido). Las preguntas y su tipo
 * (valoracion, calificacion, cerrada, abierta) salen de pregunta_servicio: si se
 * configura una pregunta nueva el reporte la recoge sin tocar codigo.
 *
 * Lectura solamente. Se usa PreparedStatement porque las respuestas abiertas son texto
 * libre del cliente.
 */
public class EncuestaClienteDAO {

	/** Una pregunta configurada en pregunta_servicio. */
	public static class Pregunta {
		public int idPregunta;
		public String titulo = "";
		public String descripcion = "";
		/** valoracion, calificacion, cerrada o abierta. */
		public String tipo = "";
	}

	/** Una respuesta a una pregunta dentro de una encuesta. */
	public static class Respuesta {
		public int idTienda;
		public int idPedido;
		public int idPregunta;
		public String respuesta = "";
		public String tipoAtencion = "";
		public Timestamp fecha;
	}

	/** Cuantas encuestas se enviaron por link y cuantas se contestaron. */
	public static class Envios {
		public int enviadas = 0;
		public int contestadas = 0;
	}

	public static ArrayList<Pregunta> obtenerPreguntas() {
		final ArrayList<Pregunta> preguntas = new ArrayList<Pregunta>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = con1
					.prepareStatement("select idpregunta, titulo, descripcion, tipo from pregunta_servicio order by idpregunta");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Pregunta p = new Pregunta();
				p.idPregunta = rs.getInt("idpregunta");
				p.titulo = texto(rs.getString("titulo"));
				p.descripcion = texto(rs.getString("descripcion"));
				p.tipo = texto(rs.getString("tipo")).toLowerCase();
				preguntas.add(p);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("EncuestaClienteDAO.obtenerPreguntas: " + e.toString());
			cerrar(con1);
		}
		return (preguntas);
	}

	/**
	 * Las respuestas de las encuestas creadas entre dos fechas.
	 *
	 * @param desde       inclusive, aaaa-mm-dd
	 * @param hastaExclusivo el dia SIGUIENTE al ultimo que se quiere ver, aaaa-mm-dd
	 * @param idTienda    0 = todas las tiendas
	 */
	public static ArrayList<Respuesta> obtenerRespuestas(final String desde, final String hastaExclusivo,
			final int idTienda) {
		final ArrayList<Respuesta> respuestas = new ArrayList<Respuesta>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = con1.prepareStatement(
					"select idtienda, idpedido, idpregunta, respuesta, tipo_atencion, fecha_creacion "
							+ "from encuesta_servicio where fecha_creacion >= ? and fecha_creacion < ? "
							+ (idTienda > 0 ? "and idtienda = ? " : "") + "order by fecha_creacion");
			ps.setString(1, desde);
			ps.setString(2, hastaExclusivo);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Respuesta r = new Respuesta();
				r.idTienda = rs.getInt("idtienda");
				r.idPedido = rs.getInt("idpedido");
				r.idPregunta = rs.getInt("idpregunta");
				r.respuesta = texto(rs.getString("respuesta"));
				r.tipoAtencion = texto(rs.getString("tipo_atencion"));
				r.fecha = rs.getTimestamp("fecha_creacion");
				respuestas.add(r);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("EncuestaClienteDAO.obtenerRespuestas: " + e.toString());
			cerrar(con1);
		}
		return (respuestas);
	}

	/**
	 * Cuantas encuestas se enviaron por link en el periodo y cuantas se contestaron.
	 * Las que entran por el QR de la mesa o de la factura no pasan por este log: por eso
	 * pueden ser mas las encuestas respondidas que las contestadas del envio.
	 */
	public static Envios obtenerEnvios(final String desde, final String hastaExclusivo, final int idTienda) {
		final Envios envios = new Envios();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = con1.prepareStatement(
					"select count(*) enviadas, ifnull(sum(contestado = 'S'), 0) contestadas "
							+ "from log_encuesta_servicio where hora_envio >= ? and hora_envio < ? "
							+ (idTienda > 0 ? "and idtienda = ?" : ""));
			ps.setString(1, desde);
			ps.setString(2, hastaExclusivo);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				envios.enviadas = rs.getInt("enviadas");
				envios.contestadas = rs.getInt("contestadas");
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("EncuestaClienteDAO.obtenerEnvios: " + e.toString());
			cerrar(con1);
		}
		return (envios);
	}

	/**
	 * Nombre de pila de los clientes a quienes se les envio la encuesta, por
	 * "idtienda-idpedido". Se busca desde 45 dias antes del periodo porque el cliente
	 * puede contestar semanas despues del envio. Solo el nombre de pila: el telefono y el
	 * apellido no salen en el reporte.
	 */
	public static HashMap<String, String> obtenerNombres(final String desde, final String hastaExclusivo,
			final int idTienda) {
		final HashMap<String, String> nombres = new HashMap<String, String>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = con1.prepareStatement(
					"select idtienda, numero_pedido, nombre_cliente from log_encuesta_servicio "
							+ "where hora_envio >= date_sub(?, interval 45 day) and hora_envio < ? "
							+ (idTienda > 0 ? "and idtienda = ?" : ""));
			ps.setString(1, desde);
			ps.setString(2, hastaExclusivo);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final String completo = texto(rs.getString("nombre_cliente")).trim();
				if (completo.length() == 0) {
					continue;
				}
				final String pila = completo.split("\\s+")[0];
				nombres.put(rs.getInt("idtienda") + "-" + rs.getInt("numero_pedido"), pila);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("EncuestaClienteDAO.obtenerNombres: " + e.toString());
			cerrar(con1);
		}
		return (nombres);
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}

	private static void cerrar(final Connection con1) {
		try {
			if (con1 != null) {
				con1.close();
			}
		} catch (final Exception e) {
			//sin accion: ya se registro el error original
		}
	}
}

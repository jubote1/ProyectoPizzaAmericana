package capaControladorCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.CodigoPromoDAO;
import conexionCC.ConexionBaseDatos;

/**
 * Las reglas de uso de una oferta (las que valida el servicio CodigoPromocional al redimir un codigo) y la
 * bitacora de quien cambio que en una oferta.
 *
 * REGLAS
 *
 *   monto_minimo      total minimo del pedido para poder usarla
 *   tope_descuento    maximo descuento en pesos por uso (0 = sin tope)
 *   tiendas           idtienda separados por coma; vacio = todas
 *   aplica_a          T todos, M solo mostrador, D solo domicilio
 *   max_usos_cliente  veces que un mismo cliente puede redimirla (0 = sin limite)
 *   max_emision       codigos que se pueden emitir en total (0 = sin limite)
 *
 * BITACORA
 *
 * Antes cualquiera con acceso a la pantalla podia cambiar el descuento de una oferta viva o deshabilitarla y no
 * quedaba rastro de quien ni cuando. Cada crear, editar, eliminar y cambio de reglas deja una fila con lo que
 * cambio, campo por campo.
 */
public class OfertaReglasCtrl {

	private static final String[] CAMPOS_REGLAS = { "monto_minimo", "tope_descuento", "tiendas", "aplica_a",
			"max_usos_cliente", "max_emision" };

	// los campos de la oferta que se comparan en la bitacora
	private static final String[] CAMPOS_OFERTA = { "nombre_oferta", "habilitado", "tipo_oferta",
			"codigo_promocional", "descuento_fijo_porcentaje", "descuento_fijo_valor", "red_parcial",
			"dias_caducidad", "fecha_desde", "fecha_hasta", "controla_hora", "hora_inicio", "hora_fin",
			"codigo_general", "monto_minimo", "tope_descuento", "tiendas", "aplica_a", "max_usos_cliente",
			"max_emision" };

	private OfertaReglasCtrl() {
	}

	/** Foto de los campos de una oferta, para comparar antes y despues. Vacia si no existe o si falla. */
	public static Map<String, String> foto(final int idOferta) {
		final Map<String, String> m = new LinkedHashMap<String, String>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("select * from oferta where idoferta = ?")) {
				ps.setInt(1, idOferta);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						for (final String c : CAMPOS_OFERTA) {
							try {
								final String v = rs.getString(c);
								m.put(c, v == null ? "" : v.trim());
							} catch (final Exception sinColumna) {
								// la columna no existe todavia (falta correr el SQL): no se compara
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("OfertaReglasCtrl.foto: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return m;
	}

	/** "campo: antes -> despues; ..." con lo que cambio. Vacio si no cambio nada. */
	public static String diferencias(final Map<String, String> antes, final Map<String, String> despues) {
		final StringBuilder sb = new StringBuilder();
		for (final Map.Entry<String, String> e : despues.entrySet()) {
			final String a = antes.containsKey(e.getKey()) ? antes.get(e.getKey()) : "";
			if (!a.equals(e.getValue())) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append(e.getKey()).append(": ").append(a.length() == 0 ? "(vacio)" : a).append(" -> ")
						.append(e.getValue().length() == 0 ? "(vacio)" : e.getValue());
			}
		}
		return sb.toString();
	}

	/** Deja constancia de una accion sobre una oferta. Nunca lanza: la bitacora no puede tumbar el guardado. */
	public static void registrar(final int idOferta, final String usuario, final String accion, final String detalle) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			CodigoPromoDAO.bitacora(cn, idOferta, usuario == null ? "desconocido" : usuario, accion, detalle);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("OfertaReglasCtrl.registrar: " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	/** El id de la oferta recien creada con ese nombre, o 0. */
	public static int ultimaConNombre(final String nombre) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement(
					"select max(idoferta) from oferta where nombre_oferta = ?")) {
				ps.setString(1, nombre == null ? "" : nombre.trim());
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next() ? rs.getInt(1) : 0;
				}
			}
		} catch (final Exception e) {
			return 0;
		} finally {
			cerrar(cn);
		}
	}

	@SuppressWarnings("unchecked")
	public static String obtener(final int idOferta) {
		final JSONObject o = new JSONObject();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("select monto_minimo, tope_descuento, tiendas, aplica_a,"
					+ " max_usos_cliente, max_emision from oferta where idoferta = ?")) {
				ps.setInt(1, idOferta);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						o.put("error", "La oferta no existe.");
						return o.toJSONString();
					}
					o.put("monto_minimo", rs.getDouble(1));
					o.put("tope_descuento", rs.getDouble(2));
					o.put("tiendas", rs.getString(3) == null ? "" : rs.getString(3));
					o.put("aplica_a", rs.getString(4) == null ? "T" : rs.getString(4));
					o.put("max_usos_cliente", rs.getInt(5));
					o.put("max_emision", rs.getInt(6));
				}
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("OfertaReglasCtrl.obtener: " + e.toString());
			o.put("error", "Las reglas todavia no estan disponibles (falta actualizar la base de datos).");
		} finally {
			cerrar(cn);
		}
		return o.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public static String guardar(final int idOferta, final double montoMinimo, final double topeDescuento,
			final String tiendas, final String aplicaA, final int maxUsosCliente, final int maxEmision,
			final String usuario) {
		final JSONObject o = new JSONObject();
		if (montoMinimo < 0 || topeDescuento < 0 || maxUsosCliente < 0 || maxEmision < 0) {
			o.put("error", "Los valores no pueden ser negativos.");
			return o.toJSONString();
		}
		final String aplica = aplicaA == null ? "T" : aplicaA.trim().toUpperCase();
		if (!aplica.equals("T") && !aplica.equals("M") && !aplica.equals("D")) {
			o.put("error", "\"Aplica a\" debe ser todos, mostrador o domicilio.");
			return o.toJSONString();
		}
		// las tiendas son ids separados por coma; cualquier otra cosa se rechaza en vez de guardarla
		final StringBuilder limpias = new StringBuilder();
		final String crudas = tiendas == null ? "" : tiendas.trim();
		if (crudas.length() > 0) {
			for (final String parte : crudas.split(",")) {
				final String p = parte.trim();
				if (!p.matches("[0-9]{1,4}")) {
					o.put("error", "Las tiendas van como numeros separados por coma, por ejemplo 1,3,7.");
					return o.toJSONString();
				}
				if (limpias.length() > 0) {
					limpias.append(",");
				}
				limpias.append(Integer.parseInt(p));
			}
		}
		if (limpias.length() > 200) {
			o.put("error", "Son demasiadas tiendas para guardar.");
			return o.toJSONString();
		}

		final Map<String, String> antes = foto(idOferta);
		if (antes.isEmpty()) {
			o.put("error", "La oferta no existe.");
			return o.toJSONString();
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("update oferta set monto_minimo = ?, tope_descuento = ?,"
					+ " tiendas = ?, aplica_a = ?, max_usos_cliente = ?, max_emision = ? where idoferta = ?")) {
				ps.setDouble(1, montoMinimo);
				ps.setDouble(2, topeDescuento);
				if (limpias.length() == 0) {
					ps.setNull(3, java.sql.Types.VARCHAR);
				} else {
					ps.setString(3, limpias.toString());
				}
				ps.setString(4, aplica);
				ps.setInt(5, maxUsosCliente);
				ps.setInt(6, maxEmision);
				ps.setInt(7, idOferta);
				ps.executeUpdate();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("OfertaReglasCtrl.guardar: " + e.toString());
			o.put("error", "No se pudieron guardar las reglas.");
			return o.toJSONString();
		} finally {
			cerrar(cn);
		}
		final String cambios = diferencias(antes, foto(idOferta));
		if (cambios.length() > 0) {
			registrar(idOferta, usuario, "REGLAS", cambios);
		}
		o.put("mensaje", "Reglas guardadas.");
		return o.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public static String bitacora(final int idOferta) {
		final JSONArray lista = new JSONArray();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("select fecha_real, usuario, accion, detalle"
					+ " from oferta_bitacora where idoferta = ? order by idbitacora desc limit 50")) {
				ps.setInt(1, idOferta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final JSONObject f = new JSONObject();
						f.put("fecha", rs.getString(1) == null ? "" : rs.getString(1).substring(0, 16));
						f.put("usuario", rs.getString(2) == null ? "" : rs.getString(2));
						f.put("accion", rs.getString(3));
						f.put("detalle", rs.getString(4) == null ? "" : rs.getString(4));
						lista.add(f);
					}
				}
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("OfertaReglasCtrl.bitacora: " + e.toString());
		} finally {
			cerrar(cn);
		}
		final JSONObject r = new JSONObject();
		r.put("cambios", lista);
		return r.toJSONString();
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			// nada que hacer
		}
	}
}

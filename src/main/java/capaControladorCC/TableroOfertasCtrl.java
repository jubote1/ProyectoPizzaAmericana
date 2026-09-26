package capaControladorCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import conexionCC.ConexionBaseDatos;

/**
 * El tablero de control de ofertas y codigos: cuanto se emitio, cuanto se uso, cuanto se descontio y donde, y
 * las alertas de lo que no deberia pasar.
 *
 * Antes no habia forma de saber esto sin escribir SQL: log_redencion_codigo no tenia fecha, tienda ni pedido, y
 * 395 de sus 834 registros eran del usuario "caja" compartido.
 *
 * LO QUE SE MIDE VIENE DEL LOG NUEVO. Los usos anteriores a la migracion no tienen fecha, asi que no entran en
 * la ventana de dias: el tablero cuenta desde que se despliega el control, no hacia atras.
 */
public class TableroOfertasCtrl {

	private TableroOfertasCtrl() {
	}

	private static final String USO_VALIDO = "coalesce(l.estado,'OK') in ('OK','EXCESO')";

	@SuppressWarnings("unchecked")
	public static String tablero(final int dias) {
		final int ventana = dias <= 0 ? 30 : Math.min(dias, 365);
		final JSONObject r = new JSONObject();
		r.put("dias", ventana);
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			// 1) Cada oferta: cuanto se ha emitido y como esta hoy (de toda su vida)
			final JSONArray ofertas = new JSONArray();
			final java.util.HashMap<Integer, JSONObject> porId = new java.util.HashMap<Integer, JSONObject>();
			try (PreparedStatement ps = cn.prepareStatement("select o.idoferta, o.nombre_oferta, o.habilitado,"
					+ " count(oc.idofertacliente) as emitidos,"
					+ " coalesce(sum(oc.utilizada = 'S'),0) as usados,"
					+ " coalesce(sum(oc.anulada = 'S'),0) as anulados,"
					+ " coalesce(sum(oc.utilizada = 'N' and oc.anulada = 'N' and oc.fecha_caducidad < curdate()),0) as vencidos,"
					+ " coalesce(sum(oc.utilizada = 'N' and oc.anulada = 'N' and (oc.fecha_caducidad is null"
					+ "     or oc.fecha_caducidad >= curdate())),0) as vigentes"
					+ " from oferta o left join oferta_cliente oc on oc.idoferta = o.idoferta"
					+ " group by o.idoferta, o.nombre_oferta, o.habilitado"
					+ " order by emitidos desc, o.idoferta desc limit 60");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final JSONObject o = new JSONObject();
					o.put("idoferta", rs.getInt("idoferta"));
					o.put("nombre", rs.getString("nombre_oferta"));
					o.put("habilitada", "S".equals(rs.getString("habilitado")));
					o.put("emitidos", rs.getInt("emitidos"));
					o.put("usados", rs.getInt("usados"));
					o.put("anulados", rs.getInt("anulados"));
					o.put("vencidos", rs.getInt("vencidos"));
					o.put("vigentes", rs.getInt("vigentes"));
					o.put("usos_ventana", 0);
					o.put("descontado", 0.0);
					o.put("ventas", 0.0);
					ofertas.add(o);
					porId.put(Integer.valueOf(rs.getInt("idoferta")), o);
				}
			}

			// 2) Lo que se uso en la ventana, por oferta
			try (PreparedStatement ps = cn.prepareStatement("select oc.idoferta, count(*) as usos,"
					+ " coalesce(sum(l.descuento),0) as descontado, coalesce(sum(pe.total_neto),0) as ventas"
					+ " from log_redencion_codigo l join oferta_cliente oc on oc.idofertacliente = l.idofertacliente"
					+ " left join pedido pe on pe.idpedido = l.idpedido and pe.idtienda = l.idtienda"
					+ " where l.fecha_real >= date_sub(now(), interval ? day) and " + USO_VALIDO
					+ " group by oc.idoferta")) {
				ps.setInt(1, ventana);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final JSONObject o = porId.get(Integer.valueOf(rs.getInt("idoferta")));
						if (o != null) {
							o.put("usos_ventana", rs.getInt("usos"));
							o.put("descontado", rs.getDouble("descontado"));
							o.put("ventas", rs.getDouble("ventas"));
						}
					}
				}
			}
			r.put("ofertas", ofertas);

			// 3) Por tienda
			final JSONArray tiendas = new JSONArray();
			try (PreparedStatement ps = cn.prepareStatement("select l.idtienda, t.nombre, count(*) as usos,"
					+ " coalesce(sum(l.descuento),0) as descontado"
					+ " from log_redencion_codigo l left join tienda t on t.idtienda = l.idtienda"
					+ " where l.fecha_real >= date_sub(now(), interval ? day) and " + USO_VALIDO
					+ " group by l.idtienda, t.nombre order by usos desc")) {
				ps.setInt(1, ventana);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final JSONObject t = new JSONObject();
						t.put("idtienda", rs.getInt("idtienda"));
						t.put("nombre", rs.getString("nombre") == null ? "(sin dato)" : rs.getString("nombre"));
						t.put("usos", rs.getInt("usos"));
						t.put("descontado", rs.getDouble("descontado"));
						tiendas.add(t);
					}
				}
			}
			r.put("tiendas", tiendas);

			// 4) Alertas: lo que no deberia pasar
			final JSONArray alertas = new JSONArray();
			alerta(cn, alertas, "ALTA", "Descuento mayor al autorizado",
					"Usos donde la tienda aplico MAS descuento del que el central habia calculado al reservar el codigo.",
					"select count(*) from log_redencion_codigo l where l.estado = 'EXCESO' and l.fecha_real >= date_sub(now(), interval ? day)",
					ventana);
			alerta(cn, alertas, "ALTA", "Codigo usado en dos pedidos a la vez",
					"Dos pedidos intentaron consumir el mismo codigo; uno quedo como conflicto para revisar.",
					"select count(*) from log_redencion_codigo l where l.estado = 'CONFLICTO' and l.fecha_real >= date_sub(now(), interval ? day)",
					ventana);
			alerta(cn, alertas, "MEDIA", "Usos por el servicio viejo",
					"El POS sin actualizar sigue redimiendo por el camino anterior, que no reserva ni valida reglas. Deberia bajar a cero cuando todas las tiendas tengan la version nueva.",
					"select count(*) from log_redencion_codigo l where l.origen = 'LEGADO' and l.fecha_real >= date_sub(now(), interval ? day)",
					ventana);
			alerta(cn, alertas, "MEDIA", "Usos a nombre de \"caja\"",
					"El usuario compartido no dice quien redimio el codigo. Se va a inhabilitar.",
					"select count(*) from log_redencion_codigo l where l.usuario_uso = 'caja' and l.fecha_real >= date_sub(now(), interval ? day)",
					ventana);
			alerta(cn, alertas, "MEDIA", "Codigos usados con descuento en cero",
					"Se marco un codigo como usado pero no descontó nada.",
					"select count(*) from log_redencion_codigo l where l.descuento = 0 and l.fecha_real >= date_sub(now(), interval ? day)",
					ventana);
			alerta(cn, alertas, "BAJA", "Reservas que nunca se confirmaron",
					"Codigos apartados por una tienda hace mas de un dia que ni se usaron ni se devolvieron: el pedido no se finalizo o la confirmacion no llego.",
					"select count(*) from oferta_cliente where reserva_token is not null and utilizada = 'N'"
							+ " and reservada_hasta < date_sub(now(), interval 1 day) and ? > 0",
					ventana);
			r.put("alertas", alertas);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("TableroOfertasCtrl.tablero: " + e.toString());
			final JSONObject err = new JSONObject();
			err.put("error", "El tablero todavia no esta disponible: falta actualizar la base de datos (ofertas_control_codigos.sql).");
			return err.toJSONString();
		} finally {
			try {
				if (cn != null) {
					cn.close();
				}
			} catch (final Exception e2) {
				// nada que hacer
			}
		}
		return r.toJSONString();
	}

	@SuppressWarnings("unchecked")
	private static void alerta(final Connection cn, final JSONArray destino, final String nivel, final String titulo,
			final String explicacion, final String sql, final int dias) throws Exception {
		try (PreparedStatement ps = cn.prepareStatement(sql)) {
			ps.setInt(1, dias);
			try (ResultSet rs = ps.executeQuery()) {
				final int n = rs.next() ? rs.getInt(1) : 0;
				final JSONObject a = new JSONObject();
				a.put("nivel", nivel);
				a.put("titulo", titulo);
				a.put("explicacion", explicacion);
				a.put("cantidad", n);
				destino.add(a);
			}
		}
	}
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * De que equipo es hincha un cliente, deducido de cuando compro el Combo Futbolero.
 *
 * LA REGLA: si compro el combo un dia en que jugaba UNICAMENTE Nacional, es hincha de
 * Nacional; si jugaba unicamente Medellin, del Medellin. Los dias de clasico no dicen
 * nada -se compra igual sea cual sea el equipo- y se dejan por fuera; en la tabla de
 * partidos un clasico son dos filas de la misma fecha, asi que basta con descartar las
 * fechas que tienen los dos equipos.
 *
 * Y si un mismo cliente aparece comprando en dias de los dos equipos, TAMPOCO se marca:
 * o va con quien lo invite, o en esa casa hay hinchas de ambos. Marcarlo con el que mas
 * veces aparezca seria inventar.
 *
 * Solo se usan partidos con confirmado = 'S'. Una fecha mal puesta no deja un hueco:
 * deja a un hincha marcado al reves, que es peor.
 */
public class HinchaFutbolDAO {

	/** El producto del datamart; el combo vive ahi con este id. */
	public static final int PRODUCTO_FUTBOLERO = 444;

	public static final String MARCA_NACIONAL = "HINCHA_NACIONAL";
	public static final String MARCA_MEDELLIN = "HINCHA_MEDELLIN";

	/** Un cliente y el equipo que le corresponde. */
	public static class Candidato {
		public String telefono = "";
		public String equipo = "";
	}

	public static class Resumen {
		public int diasNacional = 0;
		public int diasMedellin = 0;
		public int diasClasico = 0;
		public int candidatos = 0;
		public int ambosEquipos = 0;
		public int marcadosNacional = 0;
		public int marcadosMedellin = 0;
		public int yaEstaban = 0;
		public String error = "";
	}

	/**
	 * Los clientes que se pueden clasificar en el rango, con su equipo.
	 *
	 * Se cruza el datamart -que es donde esta el detalle del pedido de las once tiendas-
	 * con el pedido del central, que es el unico que tiene el telefono del cliente. Ese
	 * puente es (idtienda, numposheader, fechapedido); se pierde unos pedidos, los que
	 * no alcanzaron a subir, y eso es preferible a adivinar el cliente.
	 */
	public static ArrayList<Candidato> candidatos(final String desde, final String hasta,
			final Resumen resumen) {

		final ArrayList<Candidato> lista = new ArrayList<Candidato>();
		final String sql =
				"SELECT tel.telefono,"
				+ "  SUM(dia.equipo = 'NACIONAL') AS dias_nacional,"
				+ "  SUM(dia.equipo = 'MEDELLIN') AS dias_medellin"
				+ " FROM ("
				//Las fechas que sirven: las que tienen UN solo equipo confirmado.
				+ "   SELECT fecha, MIN(equipo) AS equipo FROM partido_futbol"
				+ "    WHERE confirmado = 'S' AND fecha BETWEEN ? AND ?"
				+ "    GROUP BY fecha HAVING COUNT(DISTINCT equipo) = 1"
				+ " ) dia"
				+ " JOIN datamart.pedido p ON p.fechapedido = dia.fecha AND p.idmotivoanulacion IS NULL"
				+ " JOIN datamart.detalle_pedido dp ON dp.idpedidotienda = p.idpedidotienda"
				+ "   AND dp.idtienda = p.idtienda AND dp.idproducto = ?"
				+ " JOIN pedido pc ON pc.numposheader = p.idpedidotienda AND pc.idtienda = p.idtienda"
				+ "   AND pc.fechapedido = p.fechapedido AND pc.idcliente > 1"
				+ " JOIN ("
				+ "   SELECT idcliente, REGEXP_REPLACE(IFNULL(NULLIF(TRIM(telefono_celular),''),"
				+ "          IFNULL(telefono,'')), '[^0-9]', '') AS telefono FROM cliente"
				+ " ) tel ON tel.idcliente = pc.idcliente"
				+ " WHERE CHAR_LENGTH(tel.telefono) >= 7"
				+ " GROUP BY tel.telefono";

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			contarDias(con1, desde, hasta, resumen);

			final PreparedStatement pst = con1.prepareStatement(sql);
			pst.setString(1, desde);
			pst.setString(2, hasta);
			pst.setInt(3, PRODUCTO_FUTBOLERO);
			final ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				final int nacional = rs.getInt("dias_nacional");
				final int medellin = rs.getInt("dias_medellin");
				if (nacional > 0 && medellin > 0) {
					//Compro en dias de los dos: no se puede decir de cual es.
					resumen.ambosEquipos++;
					continue;
				}
				final Candidato c = new Candidato();
				c.telefono = rs.getString("telefono");
				c.equipo = (nacional > 0 ? "NACIONAL" : "MEDELLIN");
				lista.add(c);
			}
			rs.close();
			pst.close();
			resumen.candidatos = lista.size();
		} catch (final Exception e) {
			resumen.error = "No se pudieron calcular los hinchas: " + e.getMessage();
			Logger.getLogger("log_file").error(resumen.error);
		} finally {
			cerrar(con1);
		}
		return (lista);
	}

	/** Cuantos dias utiles y cuantos clasicos hay en el rango, para poder explicarlo. */
	private static void contarDias(final Connection con1, final String desde, final String hasta,
			final Resumen resumen) throws Exception {
		final PreparedStatement pst = con1.prepareStatement(
				"SELECT COUNT(DISTINCT equipo) AS equipos, MIN(equipo) AS equipo"
				+ " FROM partido_futbol WHERE confirmado = 'S' AND fecha BETWEEN ? AND ?"
				+ " GROUP BY fecha");
		pst.setString(1, desde);
		pst.setString(2, hasta);
		final ResultSet rs = pst.executeQuery();
		while (rs.next()) {
			if (rs.getInt("equipos") > 1) {
				resumen.diasClasico++;
			} else if ("NACIONAL".equals(rs.getString("equipo"))) {
				resumen.diasNacional++;
			} else {
				resumen.diasMedellin++;
			}
		}
		rs.close();
		pst.close();
	}

	/**
	 * Marca a los candidatos. No toca a quien ya tenga una marca de hinchada: si alguien
	 * la corrigio a mano, el proceso no se la puede volver a cambiar.
	 */
	public static void marcar(final ArrayList<Candidato> candidatos, final String usuario,
			final Resumen resumen) {

		if (candidatos.isEmpty()) {
			return;
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final LinkedHashMap<String, String> yaMarcados = marcasExistentes(con1);

			final PreparedStatement ins = con1.prepareStatement(
					"INSERT INTO cliente_marca (telefono, idtipo, fecha_marca, usuario) VALUES (?, ?, NOW(), ?)");
			for (final Candidato c : candidatos) {
				if (yaMarcados.containsKey(c.telefono)) {
					resumen.yaEstaban++;
					continue;
				}
				final String idtipo = ("NACIONAL".equals(c.equipo) ? MARCA_NACIONAL : MARCA_MEDELLIN);
				ins.setString(1, c.telefono);
				ins.setString(2, idtipo);
				ins.setString(3, usuario);
				try {
					ins.executeUpdate();
					if (MARCA_NACIONAL.equals(idtipo)) {
						resumen.marcadosNacional++;
					} else {
						resumen.marcadosMedellin++;
					}
				} catch (final Exception e) {
					//Un telefono que no se pudo marcar no detiene a los demas.
					Logger.getLogger("log_file").error("No se pudo marcar " + c.telefono + ": " + e.getMessage());
				}
			}
			ins.close();
		} catch (final Exception e) {
			resumen.error = "No se pudieron guardar las marcas: " + e.getMessage();
			Logger.getLogger("log_file").error(resumen.error);
		} finally {
			cerrar(con1);
		}
	}

	/** Telefonos que ya tienen una marca de hinchada, con cual. */
	private static LinkedHashMap<String, String> marcasExistentes(final Connection con1) throws Exception {
		final LinkedHashMap<String, String> mapa = new LinkedHashMap<String, String>();
		final PreparedStatement pst = con1.prepareStatement(
				"SELECT telefono, idtipo FROM cliente_marca WHERE idtipo IN (?, ?)");
		pst.setString(1, MARCA_NACIONAL);
		pst.setString(2, MARCA_MEDELLIN);
		final ResultSet rs = pst.executeQuery();
		while (rs.next()) {
			mapa.put(rs.getString("telefono"), rs.getString("idtipo"));
		}
		rs.close();
		pst.close();
		return (mapa);
	}

	private static void cerrar(final Connection con1) {
		try {
			if (con1 != null) {
				con1.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("HinchaFutbolDAO, cerrando: " + e.getMessage());
		}
	}
}

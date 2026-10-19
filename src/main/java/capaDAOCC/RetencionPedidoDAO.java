package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Retener un pedido: "existe, pero no lo manden todavia".
 *
 * POR QUE EXISTE
 *
 * Dos cosas distintas necesitan lo mismo. Editar un pedido que aun no ha salido
 * -si no se retiene, la red de seguridad lo empuja a cocina a los 2 o 5 minutos
 * mientras alguien lo esta corrigiendo-, y tomar un pedido para una fecha
 * futura -que tiene que quedarse quieto hasta su hora-.
 *
 * EL ESTADO ES UN 3 EN enviadopixel
 *
 * Las cuatro consultas de envio de Servicios preguntan por un valor EXACTO: 0
 * las tres de la red de seguridad, 2 la de pago virtual. Un 3 les queda
 * invisible sin tocar ninguna de las cuatro, que son cadenas de SQL armadas a
 * mano en cuatro archivos distintos. Lo que no hay que acordarse de hacer no se
 * puede olvidar.
 *
 * El boton Reenviar de las ocho pantallas si hubo que taparlo aparte, porque no
 * mira enviadopixel sino que pide turno: la condicion esta en EnvioTiendaDAO,
 * que es el unico paso por el que pasan las once entradas.
 *
 * SIEMPRE CON FECHA DE VENCIMIENTO
 *
 * retener() exige un plazo. Sin el, cerrar el navegador a mitad de una edicion
 * deja el pedido trancado para siempre y toca sacarlo a mano de la base. Es la
 * misma leccion del turno de EnvioTiendaDAO, que tambien se vence solo.
 *
 * NO RETIENE LO QUE YA SALIO
 *
 * retener() no toca un pedido que ya tiene numero en la tienda. Retenerlo no lo
 * traeria de vuelta de la cocina -alla ya esta impreso- y si dejaria el estado
 * mintiendo. Devuelve false y quien llame decide que decirle a la persona.
 */
public class RetencionPedidoDAO {

	/** El valor de enviadopixel que significa "retenido". Ver el comentario de arriba. */
	public static final int RETENIDO = 3;

	/** Se esta editando. Lo libera quien guarda o cancela, o el vencimiento. */
	public static final String POR_EDICION = "EDICION";

	/** Es para una fecha futura. Lo libera el proceso que mira la hora. */
	public static final String POR_PROGRAMADO = "PROGRAMADO";

	/** Lo que se sabe de un pedido retenido. */
	public static class Estado {
		public boolean retenido = false;
		public String motivo = "";
		public String por = "";
		public String hasta = "";
		/** El valor al que hay que devolverlo al liberar. */
		public int enviadoAntes = 0;
	}

	/**
	 * Retiene el pedido. Devuelve false si no se pudo, y entonces NO se retuvo.
	 *
	 * Va en un solo UPDATE con condiciones, como el turno de envio: si dos
	 * personas abren el mismo pedido a editar al tiempo, la base decide y solo
	 * una se lleva la fila. Un SELECT y despues un UPDATE las dejaria entrar a
	 * las dos.
	 *
	 * Se guarda el enviadopixel anterior en retenido_motivo? No: se guarda
	 * aparte, porque el unico valor del que se puede retener es 0 o 2 y al
	 * liberar hay que devolverlo a donde estaba. Ver liberar().
	 *
	 * @param minutos cuanto dura la retencion antes de soltarse sola.
	 */
	public static boolean retener(final int idPedido, final String motivo, final String usuario,
			final int minutos) {
		if (idPedido <= 0 || minutos <= 0) {
			return (false);
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			/*
			 * Solo se puede retener lo que NO ha llegado a la tienda y no esta
			 * ya retenido por otro. numposheader en cero es la misma condicion
			 * con la que EnvioTiendaDAO decide si algo ya esta en cocina.
			 */
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido"
					+ "   SET enviadopixel_antes = IFNULL(enviadopixel,0),"
					+ "       enviadopixel = " + RETENIDO + ","
					+ "       retenido_motivo = ?, retenido_por = ?,"
					+ "       retenido_hasta = NOW() + INTERVAL ? MINUTE"
					+ " WHERE idpedido = ?"
					+ "   AND IFNULL(numposheader,0) = 0"
					+ "   AND IFNULL(enviadopixel,0) <> " + RETENIDO
					+ "   AND IFNULL(enviadopixel,0) <> 1");
			ps.setString(1, motivo);
			ps.setString(2, usuario == null ? "" : usuario);
			ps.setInt(3, minutos);
			ps.setInt(4, idPedido);
			final int filas = ps.executeUpdate();
			ps.close();
			return (filas == 1);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("RetencionPedidoDAO.retener pedido " + idPedido
					+ ": " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Suelta el pedido y lo devuelve al estado en que estaba.
	 *
	 * Devolverlo a 0 a ciegas estaria mal: un pedido de pago virtual estaba en
	 * 2, y ponerlo en 0 lo haria salir a cocina sin que nadie hubiera pagado.
	 * Por eso se guarda el valor anterior al retener.
	 */
	public static boolean liberar(final int idPedido) {
		if (idPedido <= 0) {
			return (false);
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido"
					+ "   SET enviadopixel = IFNULL(enviadopixel_antes,0),"
					+ "       enviadopixel_antes = NULL,"
					+ "       retenido_motivo = NULL, retenido_por = NULL, retenido_hasta = NULL"
					+ " WHERE idpedido = ? AND IFNULL(enviadopixel,0) = " + RETENIDO);
			ps.setInt(1, idPedido);
			final int filas = ps.executeUpdate();
			ps.close();
			return (filas == 1);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("RetencionPedidoDAO.liberar pedido " + idPedido
					+ ": " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Alarga la retencion de una edicion que sigue abierta.
	 *
	 * La pantalla lo llama cada tanto mientras la persona trabaja. Asi el plazo
	 * puede ser corto -unos minutos- sin que a nadie se le suelte el pedido por
	 * estar escribiendo, y una pestania olvidada si se suelta sola.
	 */
	public static boolean renovar(final int idPedido, final String usuario, final int minutos) {
		if (idPedido <= 0 || minutos <= 0) {
			return (false);
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			//Solo renueva quien la tomo: si se vencio y la cogio otro, este se
			//entera porque le dicen que no.
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido SET retenido_hasta = NOW() + INTERVAL ? MINUTE"
					+ " WHERE idpedido = ? AND IFNULL(enviadopixel,0) = " + RETENIDO
					+ "   AND IFNULL(retenido_por,'') = ?");
			ps.setInt(1, minutos);
			ps.setInt(2, idPedido);
			ps.setString(3, usuario == null ? "" : usuario);
			final int filas = ps.executeUpdate();
			ps.close();
			return (filas == 1);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("RetencionPedidoDAO.renovar pedido " + idPedido
					+ ": " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	/** Como esta el pedido, para pintar la pantalla. */
	public static Estado consultar(final int idPedido) {
		final Estado e = new Estado();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT IFNULL(enviadopixel,0) enviadopixel, IFNULL(enviadopixel_antes,0) antes,"
					+ " IFNULL(retenido_motivo,'') motivo, IFNULL(retenido_por,'') por,"
					+ " retenido_hasta FROM pedido WHERE idpedido = ?");
			ps.setInt(1, idPedido);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				e.retenido = rs.getInt("enviadopixel") == RETENIDO;
				e.motivo = rs.getString("motivo");
				e.por = rs.getString("por");
				e.hasta = rs.getString("retenido_hasta") == null ? "" : rs.getString("retenido_hasta");
				e.enviadoAntes = rs.getInt("antes");
			}
			rs.close();
			ps.close();
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("RetencionPedidoDAO.consultar pedido " + idPedido
					+ ": " + ex.toString());
		} finally {
			cerrar(cn);
		}
		return (e);
	}

	/**
	 * Suelta las EDICIONES vencidas. Devuelve cuantas solto.
	 *
	 * Solo las de edicion: una retencion de PROGRAMADO vencida significa que al
	 * pedido le llego la hora, y eso lo despacha su propio proceso, que ademas
	 * tiene que mirar si la tienda esta abierta. Soltarlo aqui lo mandaria a
	 * cocina de una, que es justo lo que no se quiere.
	 */
	public static int soltarEdicionesVencidas() {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido"
					+ "   SET enviadopixel = IFNULL(enviadopixel_antes,0),"
					+ "       enviadopixel_antes = NULL,"
					+ "       retenido_motivo = NULL, retenido_por = NULL, retenido_hasta = NULL"
					+ " WHERE IFNULL(enviadopixel,0) = " + RETENIDO
					+ "   AND retenido_motivo = '" + POR_EDICION + "'"
					+ "   AND retenido_hasta IS NOT NULL AND retenido_hasta < NOW()");
			final int filas = ps.executeUpdate();
			ps.close();
			if (filas > 0) {
				Logger.getLogger("log_file").info("RetencionPedidoDAO: se soltaron " + filas
						+ " ediciones vencidas");
			}
			return (filas);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("RetencionPedidoDAO.soltarEdicionesVencidas: "
					+ e.toString());
			return (0);
		} finally {
			cerrar(cn);
		}
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("RetencionPedidoDAO: no cerro la conexion, "
					+ e.toString());
		}
	}
}

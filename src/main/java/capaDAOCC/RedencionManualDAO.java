package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * La redencion manual de puntos: cuando la tienda no pudo hacerla y la hace
 * administracion.
 *
 * POR QUE NO SE REUSA ClienteFidelizacionDAO.redimirPuntosClienteFidelizacion
 *
 * Ese metodo resta a ciegas:
 *
 *     update cliente_fidelizacion set puntos_vigentes = puntos_vigentes - X
 *
 * Nadie revisa antes si el cliente tiene esos puntos, asi que redimir 200 a
 * quien tiene 50 lo deja en -150 y el sistema no dice nada. En el POS el riesgo
 * es limitado porque la pantalla valida antes de llamar; aca lo digita una
 * persona contra un saldo que leyo hace un rato, y entre que lo leyo y le da al
 * boton el cliente pudo haber redimido en la tienda.
 *
 * Por eso {@link #redimirConSaldo} pone la condicion DENTRO del update: si el
 * saldo ya no alcanza, no actualiza ninguna fila y se sabe. Es una sola
 * sentencia, asi que no hay ventana entre leer y escribir.
 *
 * Este archivo es nuevo a proposito y no toca ClienteFidelizacionDAO, que
 * Servicios compila por referencia de codigo fuente: cambiarlo obligaria a
 * desplegar los dos al tiempo.
 */
public class RedencionManualDAO {

	/** Origen con que queda marcada la redencion. La columna es char(3). */
	public static final String ORIGEN = "ADM";

	/** Lo que hay que saberse del cliente antes de tocarle los puntos. */
	public static class Cliente {
		public boolean existe;
		public String correo = "";
		public String nombre = "";
		public String activo = "";
		public double puntos;
	}

	/**
	 * El cliente del plan de fidelizacion, con su nombre y su saldo.
	 *
	 * El nombre no esta en cliente_fidelizacion -esa tabla solo tiene el correo-
	 * asi que se busca en cliente. Puede no aparecer: hay correos del plan que
	 * no tienen ficha. Eso no impide la redencion, solo hace que el correo
	 * salude sin nombre.
	 */
	public static Cliente consultar(final String correo) {
		final Logger logger = Logger.getLogger("log_file");
		final Cliente cliente = new Cliente();
		cliente.correo = (correo == null) ? "" : correo.trim();
		if (cliente.correo.length() == 0) {
			return (cliente);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();

			final PreparedStatement ps = cn.prepareStatement(
					"SELECT activo, puntos_vigentes FROM cliente_fidelizacion WHERE correo = ?");
			ps.setString(1, cliente.correo);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				cliente.existe = true;
				cliente.activo = rs.getString("activo");
				cliente.puntos = rs.getDouble("puntos_vigentes");
			}
			rs.close();
			ps.close();

			if (cliente.existe) {
				//El mismo correo puede estar en varias fichas. Se toma la mas
				//reciente con nombre, que es la que el cliente esta usando.
				final PreparedStatement psn = cn.prepareStatement(
						"SELECT nombre, apellido FROM cliente"
						+ " WHERE email = ? AND nombre IS NOT NULL AND nombre <> ''"
						+ " ORDER BY idcliente DESC LIMIT 1");
				psn.setString(1, cliente.correo);
				final ResultSet rsn = psn.executeQuery();
				if (rsn.next()) {
					final String nombre = rsn.getString("nombre") == null ? "" : rsn.getString("nombre").trim();
					final String apellido = rsn.getString("apellido") == null ? "" : rsn.getString("apellido").trim();
					cliente.nombre = (nombre + " " + apellido).trim();
				}
				rsn.close();
				psn.close();
			}
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.consultar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (cliente);
	}

	/**
	 * Descuenta los puntos solo si el cliente los tiene.
	 *
	 * La condicion del saldo va dentro del UPDATE, no en un SELECT previo: entre
	 * un SELECT y un UPDATE cabe una redencion hecha en la tienda, y el cliente
	 * terminaria con saldo negativo sin que nadie se entere.
	 *
	 * @return el saldo que le queda, o -1 si no se pudo porque ya no alcanzaba
	 */
	public static double redimirConSaldo(final String correo, final double puntos) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		double restante = -1;
		try {
			cn = con.obtenerConexionBDPrincipal();

			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE cliente_fidelizacion SET puntos_vigentes = puntos_vigentes - ?"
					+ " WHERE correo = ? AND puntos_vigentes >= ?");
			ps.setDouble(1, puntos);
			ps.setString(2, correo);
			ps.setDouble(3, puntos);
			final int filas = ps.executeUpdate();
			ps.close();

			if (filas == 1) {
				final PreparedStatement psl = cn.prepareStatement(
						"SELECT puntos_vigentes FROM cliente_fidelizacion WHERE correo = ?");
				psl.setString(1, correo);
				final ResultSet rsl = psl.executeQuery();
				if (rsl.next()) {
					restante = rsl.getDouble(1);
				}
				rsl.close();
				psl.close();
			} else {
				logger.warn("RedencionManualDAO: no alcanzo el saldo para redimir " + puntos
						+ " de [" + correo + "]");
			}
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.redimirConSaldo: " + e.toString());
			restante = -1;
		} finally {
			cerrar(cn);
		}
		return (restante);
	}

	/**
	 * Deja el registro de la redencion.
	 *
	 * Guarda el motivo, que en una redencion manual es lo mas importante: es el
	 * unico lugar donde queda escrito por que administracion le movio los puntos
	 * a un cliente. La tienda queda en idtienda para saber donde se entrego el
	 * producto, y el pedido va en cero porque no hay pedido: si lo hubiera, la
	 * redencion se habria podido hacer por el camino normal.
	 *
	 * @return el id de la redencion, o 0 si no quedo registrada
	 */
	public static int registrar(final String correo, final double puntos, final int idTienda,
			final String usuario, final String motivo) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		int idRedencion = 0;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO fidelizacion_redencion"
					+ " (correo, puntos_redimidos, idtienda, idpedidotienda, usuario, origen,"
					+ "  estado, fecha_estado, usuario_estado, motivo)"
					+ " VALUES (?, ?, ?, 0, ?, ?, 'CONFIRMADA', NOW(), ?, ?)",
					java.sql.Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, correo);
			ps.setDouble(2, puntos);
			ps.setInt(3, idTienda);
			ps.setString(4, recortar(usuario, 20));
			ps.setString(5, ORIGEN);
			ps.setString(6, recortar(usuario, 20));
			ps.setString(7, recortar(motivo, 200));
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				idRedencion = rs.getInt(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.registrar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (idRedencion);
	}

	/**
	 * Devuelve los puntos de una redencion que quedo registrada pero cuyo
	 * proceso no se pudo terminar.
	 *
	 * Solo se usa cuando el registro falla despues de haber descontado: sin esto
	 * el cliente quedaria sin los puntos y sin constancia de a donde fueron.
	 */
	public static boolean devolver(final String correo, final double puntos) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		boolean listo = false;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE cliente_fidelizacion SET puntos_vigentes = puntos_vigentes + ?"
					+ " WHERE correo = ?");
			ps.setDouble(1, puntos);
			ps.setString(2, correo);
			listo = (ps.executeUpdate() == 1);
			ps.close();
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.devolver: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (listo);
	}

	/**
	 * Si el usuario de la sesion es administrador.
	 *
	 * Se pregunta a la base y no al objeto de la sesion porque ese objeto no
	 * trae la marca, y sobre todo porque asi a quien le quiten el permiso deja
	 * de poder mover puntos en el siguiente clic y no cuando se le venza la
	 * sesion.
	 *
	 * Falla CERRADO: si la consulta no se puede hacer, no se autoriza.
	 */
	public static boolean esAdministrador(final int idUsuario) {
		if (idUsuario <= 0) {
			return (false);
		}
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		boolean puede = false;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT administrador FROM usuario WHERE id = ? AND activo = 1");
			ps.setInt(1, idUsuario);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				puede = "S".equalsIgnoreCase(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.esAdministrador: " + e.toString());
			puede = false;
		} finally {
			cerrar(cn);
		}
		return (puede);
	}

	/** Las tiendas, para el combo de donde se entrego el producto. */
	public static java.util.ArrayList<String[]> obtenerTiendas() {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final java.util.ArrayList<String[]> tiendas = new java.util.ArrayList<String[]>();
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idtienda, nombre FROM tienda ORDER BY nombre");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				tiendas.add(new String[] { String.valueOf(rs.getInt("idtienda")), rs.getString("nombre") });
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.obtenerTiendas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (tiendas);
	}

	private static String recortar(final String texto, final int largo) {
		final String limpio = (texto == null) ? "" : texto.trim();
		return (limpio.length() <= largo ? limpio : limpio.substring(0, largo));
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("RedencionManualDAO: cerrando conexion " + e.toString());
		}
	}
}

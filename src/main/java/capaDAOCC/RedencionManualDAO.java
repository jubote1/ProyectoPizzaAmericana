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
 * ESTE ARCHIVO YA NO DESCUENTA PUNTOS, Y ESO ES UNA CORRECCION
 *
 * La primera version tenia su propio camino: restaba el saldo con la condicion
 * dentro del UPDATE y guardaba el registro. Resolvia bien el saldo negativo,
 * pero dejaba a medias lo mas importante: NO repartia el debito entre las
 * acumulaciones de fidelizacion_transaccion.
 *
 * La consecuencia no se ve de inmediato. El saldo del cliente baja, pero sus
 * acumulaciones siguen diciendo que esos puntos estan disponibles, y de ahi
 * salen dos cosas: el correo que le avisa al cliente que se le vencen puntos
 * -le avisaria de puntos que ya no tiene- y la reversa, que no sabria a que
 * acumulacion devolverlos.
 *
 * Aparecio mirando un saldo en negativo: cinco clientes tenian el saldo por
 * debajo de su propio detalle, exactamente por el monto de las redenciones
 * manuales que se les habian hecho.
 *
 * Ahora la redencion la hace FidelizacionRedencionDAO.ejecutarRedencion, el
 * mismo camino de las redenciones normales, que reparte el debito, escribe el
 * detalle para que la reversa sea exacta, valida el saldo con las filas
 * bloqueadas y aborta si las acumulaciones no cubren.
 *
 * Aqui queda lo que es propio de la redencion manual: quien puede hacerla, a
 * quien se le hace, y el motivo -que las redenciones normales no tienen-.
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
	 * Guarda el motivo de una redencion manual.
	 *
	 * Es lo unico que FidelizacionRedencionDAO.ejecutarRedencion no guarda,
	 * porque las redenciones normales no tienen motivo. Va aparte y despues: la
	 * redencion ya quedo hecha y correcta, y si esto fallara solo quedaria sin
	 * la explicacion escrita.
	 */
	public static boolean guardarMotivo(final int idRedencion, final String motivo) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		boolean listo = false;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE fidelizacion_redencion SET motivo = ? WHERE idredencion = ?");
			ps.setString(1, recortar(motivo, 200));
			ps.setInt(2, idRedencion);
			listo = (ps.executeUpdate() > 0);
			ps.close();
		} catch (final Exception e) {
			logger.error("RedencionManualDAO.guardarMotivo: " + e.toString());
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

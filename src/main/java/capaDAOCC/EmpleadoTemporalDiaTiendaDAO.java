package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El reflejo, en general.empleado_temporal_dia_tienda, del ingreso y la salida de los domiciliarios temporales
 * que cada tienda guarda en su base local. Ver sql/2026_10_07_01_general_empleado_temporal_dia_tienda.sql.
 *
 * LA REGLA DE LA VERSION. El POS manda, en cada envio, el estado COMPLETO de la fila y la hora (milisegundos)
 * en que lo genero. Aqui solo se aplica si es igual o mas nuevo que lo que ya hay: un reintento atrasado nunca
 * pisa un estado mas reciente. Sin eso, un ingreso que tarda en llegar podia "reabrir" a alguien cuya salida ya
 * se habia registrado.
 *
 * Se hace con un UPDATE condicionado y, si no toco ninguna fila, un INSERT IGNORE: sin ON DUPLICATE KEY UPDATE
 * porque la forma con VALUES() quedo obsoleta en MySQL 8 y llena el log de advertencias.
 */
public class EmpleadoTemporalDiaTiendaDAO {

	private static final Logger logger = Logger.getLogger("log_file");

	/** Una fila tal como la manda la tienda. */
	public static class Registro {
		public int idTienda;
		public int idInterno;
		public int id;
		public String identificacion = "";
		public String nombre = "";
		public String telefono = "";
		public String empresa = "";
		public Integer idEmpresa;
		public String fechaSistema = "";
		public String horaIngreso = "";
		public String horaSalida = "";
		public String observacion = "";
		public boolean anulado;
		public long version;
	}

	public static final int APLICADO = 1;
	/** Llego un estado mas viejo que el que ya se tiene: se descarta, no es un error. */
	public static final int DESCARTADO_POR_VIEJO = 0;
	public static final int ERROR = -1;

	/** La clave con que la app del domiciliario reporta su ubicacion: los ultimos 6 digitos de la cedula. */
	public static String claveDe(final String identificacion) {
		final String c = identificacion == null ? "" : identificacion.trim();
		return c.length() > 6 ? c.substring(c.length() - 6) : c;
	}

	/** Inserta o actualiza la fila (idtienda, idinterno), sin permitir que un estado viejo pise a uno nuevo. */
	public static int registrar(final Registro r) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDGeneral();
		if (cn == null) {
			return ERROR;
		}
		try {
			final String anulado = r.anulado ? "S" : "N";
			final String clave = claveDe(r.identificacion);
			int tocadas;
			try (PreparedStatement ps = cn.prepareStatement(
					"UPDATE empleado_temporal_dia_tienda SET id = ?, identificacion = ?, clave_dom = ?, nombre = ?, "
							+ "telefono = ?, empresa = ?, idempresa = ?, fecha_sistema = ?, horaingreso = ?, horasalida = ?, "
							+ "observacion = ?, anulado = ?, version = ? "
							+ "WHERE idtienda = ? AND idinterno = ? AND version <= ?")) {
				int i = 1;
				ps.setInt(i++, r.id);
				ps.setString(i++, r.identificacion);
				ps.setString(i++, clave);
				ps.setString(i++, r.nombre);
				ps.setString(i++, r.telefono);
				ps.setString(i++, r.empresa);
				if (r.idEmpresa == null) {
					ps.setNull(i++, java.sql.Types.INTEGER);
				} else {
					ps.setInt(i++, r.idEmpresa.intValue());
				}
				ps.setString(i++, r.fechaSistema);
				ps.setString(i++, r.horaIngreso);
				ps.setString(i++, r.horaSalida);
				ps.setString(i++, r.observacion);
				ps.setString(i++, anulado);
				ps.setLong(i++, r.version);
				ps.setInt(i++, r.idTienda);
				ps.setInt(i++, r.idInterno);
				ps.setLong(i++, r.version);
				tocadas = ps.executeUpdate();
			}
			if (tocadas > 0) {
				cn.close();
				return APLICADO;
			}
			// No habia fila, o la que habia es mas nueva. INSERT IGNORE distingue las dos cosas: inserta si no
			// existia y no hace nada si ya estaba.
			int insertadas;
			try (PreparedStatement ps = cn.prepareStatement(
					"INSERT IGNORE INTO empleado_temporal_dia_tienda (idtienda, idinterno, id, identificacion, clave_dom, "
							+ "nombre, telefono, empresa, idempresa, fecha_sistema, horaingreso, horasalida, observacion, "
							+ "anulado, version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
				int i = 1;
				ps.setInt(i++, r.idTienda);
				ps.setInt(i++, r.idInterno);
				ps.setInt(i++, r.id);
				ps.setString(i++, r.identificacion);
				ps.setString(i++, clave);
				ps.setString(i++, r.nombre);
				ps.setString(i++, r.telefono);
				ps.setString(i++, r.empresa);
				if (r.idEmpresa == null) {
					ps.setNull(i++, java.sql.Types.INTEGER);
				} else {
					ps.setInt(i++, r.idEmpresa.intValue());
				}
				ps.setString(i++, r.fechaSistema);
				ps.setString(i++, r.horaIngreso);
				ps.setString(i++, r.horaSalida);
				ps.setString(i++, r.observacion);
				ps.setString(i++, anulado);
				ps.setLong(i++, r.version);
				insertadas = ps.executeUpdate();
			}
			cn.close();
			return insertadas > 0 ? APLICADO : DESCARTADO_POR_VIEJO;
		} catch (final Exception e) {
			logger.error("EmpleadoTemporalDiaTiendaDAO.registrar tienda " + r.idTienda + " idinterno " + r.idInterno
					+ ": " + e.toString());
			try {
				cn.close();
			} catch (final Exception e1) {
				// Ya estaba cerrada.
			}
			return ERROR;
		}
	}
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Solicitudes de devolucion de puntos que pasan por aprobacion.
 *
 * Cuando se anula un pedido que ya estaba finalizado, o se le quita el
 * producto de redencion, los puntos no se devuelven solos. Queda una
 * solicitud que alguien aprueba o rechaza desde la pantalla de revision. Es
 * a proposito: si la devolucion fuera automatica se podria pedir con puntos,
 * recibir el producto y luego anular para recuperarlos.
 *
 * El pedido que nunca se finalizo es distinto y no pasa por aqui. Ahi no hubo
 * entrega, asi que la reversa es directa.
 */
public class FidelizacionReversaSolicitudDAO {

	public static final String ESTADO_PENDIENTE = "PENDIENTE";

	public static final String ESTADO_APROBADA = "APROBADA";

	public static final String ESTADO_RECHAZADA = "RECHAZADA";

	public static final String MOTIVO_ANULACION = "ANULACION";

	public static final String MOTIVO_PRODUCTO = "PRODUCTO_ELIMINADO";

	public static final String MOTIVO_NO_FINALIZADO = "NO_FINALIZADO";

	public static class Solicitud {
		public int idSolicitud = 0;

		public int idRedencion = 0;

		public String correo = "";

		public int idTienda = 0;

		public String nombreTienda = "";

		public int idPedidoTienda = 0;

		public double puntosSolicitados = 0;

		public String motivo = "";

		public String observacion = "";

		public String usuarioSolicita = "";

		public String origen = "";

		public String fechaSolicitud = "";

		public String estado = "";

		public String usuarioRevisa = "";

		public String fechaRevision = "";

		public String observacionRevision = "";

		public double puntosRedimidos = 0;

		public double puntosReversados = 0;

		public String estadoRedencion = "";
	}

	/**
	 * Crea la solicitud. Si ya hay una PENDIENTE para la misma redencion y el
	 * mismo motivo se devuelve esa, para que dos clics del cajero no generen dos
	 * solicitudes que despues devuelvan los puntos dos veces.
	 *
	 * @return el id de la solicitud, o 0 si no se pudo crear
	 */
	public static int crearSolicitud(int idRedencion, String correo, int idTienda, int idPedidoTienda,
			double puntosSolicitados, String motivo, String observacion, String usuarioSolicita, String origen) {
		Logger logger = Logger.getLogger("log_file");
		int idSolicitud = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pstDup = con1.prepareStatement("select idsolicitud from fidelizacion_reversa_solicitud"
					+ " where idredencion = ? and motivo = ? and estado = ? limit 1");
			pstDup.setInt(1, idRedencion);
			pstDup.setString(2, motivo == null ? "" : motivo);
			pstDup.setString(3, ESTADO_PENDIENTE);
			ResultSet rsDup = pstDup.executeQuery();
			if (rsDup.next()) {
				idSolicitud = rsDup.getInt(1);
			}
			rsDup.close();
			pstDup.close();
			if (idSolicitud > 0) {
				con1.close();
				logger.info("crearSolicitud: ya existia la solicitud " + idSolicitud + " pendiente para la redencion "
						+ idRedencion + " con motivo " + motivo);
				return (idSolicitud);
			}

			PreparedStatement pst = con1.prepareStatement("insert into fidelizacion_reversa_solicitud (idredencion,"
					+ " correo, idtienda, idpedidotienda, puntos_solicitados, motivo, observacion, usuario_solicita,"
					+ " origen) values (?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
			pst.setInt(1, idRedencion);
			pst.setString(2, correo == null ? "" : correo);
			pst.setInt(3, idTienda);
			pst.setInt(4, idPedidoTienda);
			pst.setDouble(5, puntosSolicitados);
			pst.setString(6, motivo == null ? "" : motivo);
			pst.setString(7, observacion == null ? "" : observacion);
			pst.setString(8, usuarioSolicita == null ? "" : usuarioSolicita);
			pst.setString(9, origen == null ? "" : origen);
			pst.executeUpdate();
			ResultSet rsLlave = pst.getGeneratedKeys();
			if (rsLlave.next()) {
				idSolicitud = rsLlave.getInt(1);
			}
			rsLlave.close();
			pst.close();
			con1.close();
			logger.info("crearSolicitud " + idSolicitud + " redencion=" + idRedencion + " puntos=" + puntosSolicitados
					+ " motivo=" + motivo + " usuario=" + usuarioSolicita + " origen=" + origen);
		} catch (Exception e) {
			logger.error("crearSolicitud: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (idSolicitud);
	}

	/**
	 * Lista las solicitudes para la pantalla de revision.
	 *
	 * @param estado      PENDIENTE, APROBADA, RECHAZADA o vacio para todas
	 * @param fechaDesde  formato aaaa-mm-dd, o vacio
	 * @param fechaHasta  formato aaaa-mm-dd, o vacio
	 */
	public static ArrayList<Solicitud> obtenerSolicitudes(String estado, String fechaDesde, String fechaHasta) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Solicitud> solicitudes = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("select s.*, t.nombre as nombretienda, r.puntos_redimidos, r.puntos_reversados,");
			sql.append(" r.estado as estadoredencion from fidelizacion_reversa_solicitud s");
			sql.append(" left join tienda t on t.idtienda = s.idtienda");
			sql.append(" left join fidelizacion_redencion r on r.idredencion = s.idredencion where 1 = 1");
			if (estado != null && !estado.trim().equals("")) {
				sql.append(" and s.estado = ?");
			}
			if (fechaDesde != null && !fechaDesde.trim().equals("")) {
				sql.append(" and DATE(s.fecha_solicitud) >= ?");
			}
			if (fechaHasta != null && !fechaHasta.trim().equals("")) {
				sql.append(" and DATE(s.fecha_solicitud) <= ?");
			}
			sql.append(" order by s.estado = 'PENDIENTE' desc, s.fecha_solicitud desc");

			PreparedStatement pst = con1.prepareStatement(sql.toString());
			int p = 1;
			if (estado != null && !estado.trim().equals("")) {
				pst.setString(p++, estado.trim());
			}
			if (fechaDesde != null && !fechaDesde.trim().equals("")) {
				pst.setString(p++, fechaDesde.trim());
			}
			if (fechaHasta != null && !fechaHasta.trim().equals("")) {
				pst.setString(p++, fechaHasta.trim());
			}
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				Solicitud s = new Solicitud();
				s.idSolicitud = rs.getInt("idsolicitud");
				s.idRedencion = rs.getInt("idredencion");
				s.correo = rs.getString("correo") == null ? "" : rs.getString("correo");
				s.idTienda = rs.getInt("idtienda");
				s.nombreTienda = rs.getString("nombretienda") == null ? "" : rs.getString("nombretienda");
				s.idPedidoTienda = rs.getInt("idpedidotienda");
				s.puntosSolicitados = rs.getDouble("puntos_solicitados");
				s.motivo = rs.getString("motivo") == null ? "" : rs.getString("motivo");
				s.observacion = rs.getString("observacion") == null ? "" : rs.getString("observacion");
				s.usuarioSolicita = rs.getString("usuario_solicita") == null ? "" : rs.getString("usuario_solicita");
				s.origen = rs.getString("origen") == null ? "" : rs.getString("origen");
				s.fechaSolicitud = rs.getString("fecha_solicitud") == null ? "" : rs.getString("fecha_solicitud");
				s.estado = rs.getString("estado") == null ? "" : rs.getString("estado");
				s.usuarioRevisa = rs.getString("usuario_revisa") == null ? "" : rs.getString("usuario_revisa");
				s.fechaRevision = rs.getString("fecha_revision") == null ? "" : rs.getString("fecha_revision");
				s.observacionRevision = rs.getString("observacion_revision") == null ? ""
						: rs.getString("observacion_revision");
				s.puntosRedimidos = rs.getDouble("puntos_redimidos");
				s.puntosReversados = rs.getDouble("puntos_reversados");
				s.estadoRedencion = rs.getString("estadoredencion") == null ? "" : rs.getString("estadoredencion");
				solicitudes.add(s);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerSolicitudes: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (solicitudes);
	}

	/**
	 * Aprueba o rechaza una solicitud. Si se aprueba, la devolucion de los
	 * puntos y el cambio de estado de la solicitud van en la misma transaccion:
	 * no puede quedar una solicitud aprobada sin puntos devueltos, ni puntos
	 * devueltos sin constancia de quien lo aprobo.
	 *
	 * @return "OK" si se resolvio, o el motivo por el cual no
	 */
	public static String resolverSolicitud(int idSolicitud, boolean aprobar, String usuarioRevisa,
			String observacionRevision) {
		Logger logger = Logger.getLogger("log_file");
		String respuesta = "";
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return ("Sin conexion a la base de datos");
		}
		try {
			con1.setAutoCommit(false);

			int idRedencion = 0;
			double puntosSolicitados = 0;
			String motivo = "";
			String estadoActual = "";
			boolean existe = false;
			PreparedStatement pst = con1.prepareStatement("select idredencion, puntos_solicitados, motivo, estado"
					+ " from fidelizacion_reversa_solicitud where idsolicitud = ? for update");
			pst.setInt(1, idSolicitud);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				existe = true;
				idRedencion = rs.getInt("idredencion");
				puntosSolicitados = rs.getDouble("puntos_solicitados");
				motivo = rs.getString("motivo");
				estadoActual = rs.getString("estado");
			}
			rs.close();
			pst.close();

			if (!existe) {
				con1.rollback();
				return ("La solicitud no existe");
			}
			if (!ESTADO_PENDIENTE.equals(estadoActual)) {
				con1.rollback();
				return ("La solicitud ya fue " + estadoActual.toLowerCase() + ", no se puede volver a resolver");
			}

			if (aprobar) {
				boolean reversada = FidelizacionRedencionDAO.reversarRedencion(idRedencion, puntosSolicitados,
						usuarioRevisa, "Solicitud " + idSolicitud + " " + motivo, con1);
				if (!reversada) {
					con1.rollback();
					return ("No se pudo devolver los puntos; revise si la redencion ya estaba reversada");
				}
			}

			PreparedStatement pstUpd = con1.prepareStatement("update fidelizacion_reversa_solicitud set estado = ?,"
					+ " usuario_revisa = ?, fecha_revision = now(), observacion_revision = ? where idsolicitud = ?");
			pstUpd.setString(1, aprobar ? ESTADO_APROBADA : ESTADO_RECHAZADA);
			pstUpd.setString(2, usuarioRevisa == null ? "" : usuarioRevisa);
			pstUpd.setString(3, observacionRevision == null ? "" : observacionRevision);
			pstUpd.setInt(4, idSolicitud);
			pstUpd.executeUpdate();
			pstUpd.close();

			con1.commit();
			respuesta = "OK";
			logger.info("resolverSolicitud " + idSolicitud + " aprobar=" + aprobar + " redencion=" + idRedencion
					+ " puntos=" + puntosSolicitados + " usuario=" + usuarioRevisa);
		} catch (Exception e) {
			logger.error("resolverSolicitud: " + e.toString());
			respuesta = "Error procesando la solicitud: " + e.toString();
			try {
				con1.rollback();
			} catch (Exception e1) {
			}
		} finally {
			try {
				con1.setAutoCommit(true);
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (respuesta);
	}

	/** Cuantas solicitudes estan esperando revision, para el indicador del menu. */
	public static int contarPendientes() {
		Logger logger = Logger.getLogger("log_file");
		int pendientes = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1.prepareStatement(
					"select COUNT(*) from fidelizacion_reversa_solicitud where estado = ?");
			pst.setString(1, ESTADO_PENDIENTE);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				pendientes = rs.getInt(1);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("contarPendientes: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (pendientes);
	}
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Cola de revision para Servicio al Cliente cuando un pedido con la campana
 * 15 minutos aplicada no salio a tiempo. La fila la crea el job de deteccion
 * en Servicios (conexion directa a esta misma tabla); este DAO es para listar
 * y resolver desde la pantalla del central.
 *
 * Al aprobar, el sistema NO mueve dinero: solo deja el caso aprobado con el
 * monto ya calculado (valor_a_devolver). La devolucion real la ejecuta
 * Servicio al Cliente por fuera, igual que el resto del ecosistema de pagos.
 */
public class Campana15MinIncumplimientoDAO {

	public static final String ESTADO_PENDIENTE = "PENDIENTE";

	public static final String ESTADO_APROBADA = "APROBADA";

	public static final String ESTADO_RECHAZADA = "RECHAZADA";

	public static class Incumplimiento {
		public int idSolicitud = 0;

		public int idPedidoTienda = 0;

		public int idTienda = 0;

		public String nombreTienda = "";

		public String fechaHoraInicio = "";

		public String fechaDeteccion = "";

		public double valorBasePizza = 0;

		public double retencionAplicada = 0;

		public double valorADevolver = 0;

		public String estado = "";

		public String usuarioRevisa = "";

		public String fechaRevision = "";

		public String observacionRevision = "";
	}

	public static ArrayList<Incumplimiento> obtener(String estado, String fechaDesde, String fechaHasta) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<Incumplimiento> lista = new ArrayList<>();
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("select i.*, t.nombre as nombretienda from campana_15min_incumplimiento i");
			sql.append(" left join tienda t on t.idtienda = i.idtienda where 1 = 1");
			if (estado != null && !estado.trim().equals("")) {
				sql.append(" and i.estado = ?");
			}
			if (fechaDesde != null && !fechaDesde.trim().equals("")) {
				sql.append(" and DATE(i.fecha_deteccion) >= ?");
			}
			if (fechaHasta != null && !fechaHasta.trim().equals("")) {
				sql.append(" and DATE(i.fecha_deteccion) <= ?");
			}
			sql.append(" order by i.estado = 'PENDIENTE' desc, i.fecha_deteccion desc");

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
				Incumplimiento i = new Incumplimiento();
				i.idSolicitud = rs.getInt("idsolicitud");
				i.idPedidoTienda = rs.getInt("idpedidotienda");
				i.idTienda = rs.getInt("idtienda");
				i.nombreTienda = rs.getString("nombretienda") == null ? "" : rs.getString("nombretienda");
				i.fechaHoraInicio = rs.getString("fecha_hora_inicio");
				i.fechaDeteccion = rs.getString("fecha_deteccion");
				i.valorBasePizza = rs.getDouble("valor_base_pizza");
				i.retencionAplicada = rs.getDouble("retencion_aplicada");
				i.valorADevolver = rs.getDouble("valor_a_devolver");
				i.estado = rs.getString("estado") == null ? "" : rs.getString("estado");
				i.usuarioRevisa = rs.getString("usuario_revisa") == null ? "" : rs.getString("usuario_revisa");
				i.fechaRevision = rs.getString("fecha_revision") == null ? "" : rs.getString("fecha_revision");
				i.observacionRevision = rs.getString("observacion_revision") == null ? ""
						: rs.getString("observacion_revision");
				lista.add(i);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtener: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (lista);
	}

	/** Aprueba o rechaza. Nunca mueve dinero, solo deja el estado y quien decidio. */
	public static String resolver(int idSolicitud, boolean aprobar, String usuarioRevisa, String observacionRevision) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return ("Sin conexion a la base de datos");
		}
		String respuesta = "";
		try {
			con1.setAutoCommit(false);

			String estadoActual = "";
			boolean existe = false;
			PreparedStatement pst = con1.prepareStatement(
					"select estado from campana_15min_incumplimiento where idsolicitud = ? for update");
			pst.setInt(1, idSolicitud);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				existe = true;
				estadoActual = rs.getString("estado");
			}
			rs.close();
			pst.close();

			if (!existe) {
				con1.rollback();
				return ("El caso no existe");
			}
			if (!ESTADO_PENDIENTE.equals(estadoActual)) {
				con1.rollback();
				return ("El caso ya fue " + estadoActual.toLowerCase() + ", no se puede volver a resolver");
			}

			PreparedStatement pstUpd = con1.prepareStatement("update campana_15min_incumplimiento set estado = ?,"
					+ " usuario_revisa = ?, fecha_revision = now(), observacion_revision = ? where idsolicitud = ?");
			pstUpd.setString(1, aprobar ? ESTADO_APROBADA : ESTADO_RECHAZADA);
			pstUpd.setString(2, usuarioRevisa == null ? "" : usuarioRevisa);
			pstUpd.setString(3, observacionRevision == null ? "" : observacionRevision);
			pstUpd.setInt(4, idSolicitud);
			pstUpd.executeUpdate();
			pstUpd.close();

			con1.commit();
			respuesta = "OK";
			logger.info("resolver incumplimiento15min " + idSolicitud + " aprobar=" + aprobar + " usuario="
					+ usuarioRevisa);
		} catch (Exception e) {
			logger.error("resolver: " + e.toString());
			respuesta = "Error procesando el caso: " + e.toString();
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

	public static int contarPendientes() {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int pendientes = 0;
		try {
			PreparedStatement pst = con1
					.prepareStatement("select COUNT(*) from campana_15min_incumplimiento where estado = ?");
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

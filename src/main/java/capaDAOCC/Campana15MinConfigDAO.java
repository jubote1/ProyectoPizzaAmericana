package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Configuracion de la campana "15 minutos o gratis" (fechas, dias, horario,
 * mensajes, retencion por medio de pago virtual). El POS la lee por servicio
 * y nunca bloquea la venta si no hay respuesta o no hay campana activa.
 */
public class Campana15MinConfigDAO {

	public static class Config {
		public int idCampana = 0;

		public String nombre = "";

		public boolean activo = false;

		public String mensajeOperario = "";

		public String mensajeFactura = "";

		public String fechaDesde = "";

		public String fechaHasta = "";

		public String diasSemana = "SSSSSSS";

		public String horaDesde = "";

		public String horaHasta = "";

		public int minutosPromesa = 15;

		public double porcentajeRetencionMedioVirtual = 5;
	}

	private static Config mapear(ResultSet rs) throws Exception {
		Config c = new Config();
		c.idCampana = rs.getInt("idcampana");
		c.nombre = rs.getString("nombre") == null ? "" : rs.getString("nombre");
		c.activo = "S".equals(rs.getString("activo"));
		c.mensajeOperario = rs.getString("mensaje_operario") == null ? "" : rs.getString("mensaje_operario");
		c.mensajeFactura = rs.getString("mensaje_factura") == null ? "" : rs.getString("mensaje_factura");
		c.fechaDesde = rs.getString("fecha_desde") == null ? "" : rs.getString("fecha_desde");
		c.fechaHasta = rs.getString("fecha_hasta") == null ? "" : rs.getString("fecha_hasta");
		c.diasSemana = rs.getString("dias_semana") == null ? "SSSSSSS" : rs.getString("dias_semana");
		c.horaDesde = rs.getString("hora_desde") == null ? "" : rs.getString("hora_desde");
		c.horaHasta = rs.getString("hora_hasta") == null ? "" : rs.getString("hora_hasta");
		c.minutosPromesa = rs.getInt("minutos_promesa");
		c.porcentajeRetencionMedioVirtual = rs.getDouble("porcentaje_retencion_medio_virtual");
		return (c);
	}

	/**
	 * La campana activa AHORA MISMO: activo='S', fecha y dia de la semana y hora
	 * dentro de ventana. Es la unica consulta que le importa al POS; si no
	 * devuelve nada, el flujo del pedido sigue normal.
	 */
	public static Config obtenerActivaAhora() {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Config resultado = null;
		try {
			// dias_semana es 'LMMJVSD' en el orden lunes..domingo, char 'S'/'N' en la
			// posicion del dia de hoy. DAYOFWEEK de MySQL da 1=domingo..7=sabado, se
			// traduce a 1=lunes..7=domingo restando y con modulo.
			String sql = "select * from campana_15min_config where activo = 'S'"
					+ " and (fecha_desde is null or fecha_desde <= curdate())"
					+ " and (fecha_hasta is null or fecha_hasta >= curdate())"
					+ " and (hora_desde is null or hora_desde <= curtime())"
					+ " and (hora_hasta is null or hora_hasta >= curtime())"
					+ " and substring(dias_semana, ((dayofweek(curdate()) + 5) mod 7) + 1, 1) = 'S'"
					+ " order by idcampana desc limit 1";
			PreparedStatement pst = con1.prepareStatement(sql);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				resultado = mapear(rs);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerActivaAhora: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (resultado);
	}

	public static ArrayList<Config> obtenerTodas() {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<Config> lista = new ArrayList<>();
		try {
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery("select * from campana_15min_config order by idcampana desc");
			while (rs.next()) {
				lista.add(mapear(rs));
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerTodas: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (lista);
	}

	public static Config obtenerPorId(int idCampana) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Config resultado = null;
		try {
			PreparedStatement pst = con1.prepareStatement("select * from campana_15min_config where idcampana = ?");
			pst.setInt(1, idCampana);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				resultado = mapear(rs);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerPorId: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (resultado);
	}

	public static String guardar(Config c) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try {
			if (c.idCampana <= 0) {
				PreparedStatement pst = con1.prepareStatement("insert into campana_15min_config (nombre, activo,"
						+ " mensaje_operario, mensaje_factura, fecha_desde, fecha_hasta, dias_semana, hora_desde,"
						+ " hora_hasta, minutos_promesa, porcentaje_retencion_medio_virtual)"
						+ " values (?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
				llenarParametros(pst, c);
				pst.executeUpdate();
				ResultSet rs = pst.getGeneratedKeys();
				if (rs.next()) {
					c.idCampana = rs.getInt(1);
				}
				rs.close();
				pst.close();
			} else {
				PreparedStatement pst = con1.prepareStatement("update campana_15min_config set nombre = ?,"
						+ " activo = ?, mensaje_operario = ?, mensaje_factura = ?, fecha_desde = ?, fecha_hasta = ?,"
						+ " dias_semana = ?, hora_desde = ?, hora_hasta = ?, minutos_promesa = ?,"
						+ " porcentaje_retencion_medio_virtual = ? where idcampana = ?");
				llenarParametros(pst, c);
				pst.setInt(12, c.idCampana);
				pst.executeUpdate();
				pst.close();
			}
			con1.close();
			resultado = "exitoso";
		} catch (Exception e) {
			logger.error("guardar: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			resultado = "error";
		}
		return (resultado);
	}

	private static void llenarParametros(PreparedStatement pst, Config c) throws Exception {
		pst.setString(1, c.nombre);
		pst.setString(2, c.activo ? "S" : "N");
		pst.setString(3, c.mensajeOperario);
		pst.setString(4, c.mensajeFactura);
		if (c.fechaDesde == null || c.fechaDesde.trim().equals("")) {
			pst.setNull(5, java.sql.Types.DATE);
		} else {
			pst.setString(5, c.fechaDesde);
		}
		if (c.fechaHasta == null || c.fechaHasta.trim().equals("")) {
			pst.setNull(6, java.sql.Types.DATE);
		} else {
			pst.setString(6, c.fechaHasta);
		}
		pst.setString(7, c.diasSemana == null || c.diasSemana.length() != 7 ? "SSSSSSS" : c.diasSemana);
		if (c.horaDesde == null || c.horaDesde.trim().equals("")) {
			pst.setNull(8, java.sql.Types.TIME);
		} else {
			pst.setString(8, c.horaDesde);
		}
		if (c.horaHasta == null || c.horaHasta.trim().equals("")) {
			pst.setNull(9, java.sql.Types.TIME);
		} else {
			pst.setString(9, c.horaHasta);
		}
		pst.setInt(10, c.minutosPromesa <= 0 ? 15 : c.minutosPromesa);
		pst.setDouble(11, c.porcentajeRetencionMedioVirtual);
	}
}

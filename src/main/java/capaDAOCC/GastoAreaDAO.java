package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Los gastos de las areas que se reparten entre las tiendas: publicidad,
 * administrativa, logistica y contact center.
 *
 * Hoy esto vive en el libro GASTOS_FIJOS_TIENDAS_JULIO.xlsx y de ahi alguien lo copia
 * a mano a la pestana PORCENTAJE. Ninguno de los valores de esa pestana tiene formula.
 *
 * DOS CLASES DE GASTO
 *
 *   'F' FIJO CON VIGENCIA: vale lo mismo cada mes mientras este vigente. El valor esta
 *       en gasto_area.valor_mes.
 *   'V' VARIABLE: cambia mes a mes. El valor NO esta en gasto_area sino en
 *       gasto_area_mes, una fila por mes.
 *
 * El variable va en otra tabla a proposito: si se guardara encima del anterior, al
 * recalcular un periodo viejo saldrian las cifras de hoy. Un gasto que ya se repartio
 * entre las tiendas no puede cambiar porque alguien digite el mes siguiente.
 *
 * Ver sql/gasto_area.sql.
 */
public class GastoAreaDAO {

	private static final Logger LOG = Logger.getLogger("log_file");

	public static final String TIPO_FIJO = "F";

	public static final String TIPO_VARIABLE = "V";

	/** Un gasto del area, con el valor que le corresponde al mes que se este mirando. */
	public static class Gasto {
		public int idGasto = 0;
		public String area = "";
		public String concepto = "";
		public String tipo = TIPO_FIJO;
		/** Lo que vale al mes. Para los fijos sale de valor_mes; para los variables, del mes. */
		public double valorMes = 0;
		public String vigenciaDesde = "";
		/** Vacio = sigue vigente, sin fecha de fin. */
		public String vigenciaHasta = "";
		public boolean activo = true;
		public String usuario = "";
		/** Solo para los variables: si ese mes ya tiene valor digitado. */
		public boolean tieneValorDelMes = false;

		public boolean esVariable() {
			return TIPO_VARIABLE.equals(this.tipo);
		}
	}

	/**
	 * Los gastos de un area, con el valor del mes que se pida. Trae tambien los
	 * inactivos y los que no esten vigentes: esta pantalla es de configuracion, y lo
	 * que no se ve no se puede corregir.
	 *
	 * Para los variables, si ese mes no se ha digitado el valor llega en 0 y
	 * tieneValorDelMes en false, que no es lo mismo que un mes que de verdad valio 0.
	 */
	public static ArrayList<Gasto> listar(final String area, final int anio, final int mes) {
		final ArrayList<Gasto> gastos = new ArrayList<Gasto>();
		final String consulta = "select g.idgasto, g.area, g.concepto, g.tipo, g.valor_mes,"
				+ " g.vigencia_desde, g.vigencia_hasta, g.activo, g.usuario,"
				+ " m.valor as valor_del_mes, (m.idgasto is not null) as tiene_mes"
				+ " from gasto_area g"
				+ " left join gasto_area_mes m on m.idgasto = g.idgasto and m.anio = ? and m.mes = ?"
				+ " where g.area = ? order by g.tipo, g.concepto";
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final PreparedStatement ps = con1.prepareStatement(consulta);
			ps.setInt(1, anio);
			ps.setInt(2, mes);
			ps.setString(3, area);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Gasto g = new Gasto();
				g.idGasto = rs.getInt("idgasto");
				g.area = texto(rs.getString("area"));
				g.concepto = texto(rs.getString("concepto"));
				g.tipo = texto(rs.getString("tipo"));
				g.vigenciaDesde = texto(rs.getString("vigencia_desde"));
				g.vigenciaHasta = texto(rs.getString("vigencia_hasta"));
				g.activo = !"N".equalsIgnoreCase(texto(rs.getString("activo")));
				g.usuario = texto(rs.getString("usuario"));
				g.tieneValorDelMes = rs.getBoolean("tiene_mes");
				g.valorMes = g.esVariable() ? rs.getDouble("valor_del_mes") : rs.getDouble("valor_mes");
				gastos.add(g);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("GastoAreaDAO.listar: " + e.toString());
		} finally {
			cerrar(con1);
		}
		return gastos;
	}

	/**
	 * Lo que cuesta un area en un mes: los fijos vigentes ese mes mas el valor digitado
	 * de los variables. Un gasto cuenta si estaba vigente el PRIMER dia del mes o
	 * empezo dentro de el; se compara contra el ultimo dia para no dejar por fuera al
	 * que arranco a mitad.
	 */
	public static double totalDelMes(final String area, final int anio, final int mes) {
		double total = 0;
		final String consulta = "select ifnull(sum("
				+ "  case when g.tipo = 'V' then ifnull(m.valor, 0) else g.valor_mes end"
				+ "), 0)"
				+ " from gasto_area g"
				+ " left join gasto_area_mes m on m.idgasto = g.idgasto and m.anio = ? and m.mes = ?"
				+ " where g.area = ? and g.activo = 'S'"
				+ "   and g.vigencia_desde <= last_day(concat(?, '-', lpad(?, 2, '0'), '-01'))"
				+ "   and (g.vigencia_hasta is null"
				+ "        or g.vigencia_hasta >= concat(?, '-', lpad(?, 2, '0'), '-01'))";
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final PreparedStatement ps = con1.prepareStatement(consulta);
			ps.setInt(1, anio);
			ps.setInt(2, mes);
			ps.setString(3, area);
			ps.setInt(4, anio);
			ps.setInt(5, mes);
			ps.setInt(6, anio);
			ps.setInt(7, mes);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				total = rs.getDouble(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("GastoAreaDAO.totalDelMes: " + e.toString());
		} finally {
			cerrar(con1);
		}
		return total;
	}

	/** Inserta el gasto y devuelve su id, o 0 si no se pudo. */
	public static int insertar(final Gasto g) {
		int idNuevo = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final PreparedStatement ps = con1.prepareStatement("insert into gasto_area"
					+ " (area, concepto, tipo, valor_mes, vigencia_desde, vigencia_hasta, activo, usuario)"
					+ " values (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			llenar(ps, g);
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				idNuevo = rs.getInt(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("GastoAreaDAO.insertar: " + e.toString());
		} finally {
			cerrar(con1);
		}
		return idNuevo;
	}

	public static boolean actualizar(final Gasto g) {
		boolean exito = false;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final PreparedStatement ps = con1.prepareStatement("update gasto_area set area = ?, concepto = ?,"
					+ " tipo = ?, valor_mes = ?, vigencia_desde = ?, vigencia_hasta = ?, activo = ?, usuario = ?"
					+ " where idgasto = ?");
			llenar(ps, g);
			ps.setInt(9, g.idGasto);
			exito = ps.executeUpdate() >= 1;
			ps.close();
		} catch (final Exception e) {
			LOG.error("GastoAreaDAO.actualizar: " + e.toString());
		} finally {
			cerrar(con1);
		}
		return exito;
	}

	/**
	 * Borra el gasto y sus valores por mes.
	 *
	 * Borrar un gasto cambia hacia atras lo que costo cada periodo. Para sacar de
	 * circulacion algo que ya corrio, lo correcto es ponerle vigencia_hasta o
	 * desactivarlo, no borrarlo.
	 */
	public static boolean eliminar(final int idGasto) {
		boolean exito = false;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final PreparedStatement psMes = con1.prepareStatement("delete from gasto_area_mes where idgasto = ?");
			psMes.setInt(1, idGasto);
			psMes.executeUpdate();
			psMes.close();
			final PreparedStatement ps = con1.prepareStatement("delete from gasto_area where idgasto = ?");
			ps.setInt(1, idGasto);
			exito = ps.executeUpdate() >= 1;
			ps.close();
		} catch (final Exception e) {
			LOG.error("GastoAreaDAO.eliminar: " + e.toString());
		} finally {
			cerrar(con1);
		}
		return exito;
	}

	/** Guarda lo que valio un gasto variable en un mes. */
	public static boolean guardarValorDelMes(final int idGasto, final int anio, final int mes, final double valor,
			final String usuario) {
		boolean exito = false;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final PreparedStatement ps = con1.prepareStatement("insert into gasto_area_mes"
					+ " (idgasto, anio, mes, valor, usuario) values (?, ?, ?, ?, ?)"
					+ " on duplicate key update valor = ?, usuario = ?");
			ps.setInt(1, idGasto);
			ps.setInt(2, anio);
			ps.setInt(3, mes);
			ps.setDouble(4, valor);
			ps.setString(5, usuario == null ? "" : usuario);
			ps.setDouble(6, valor);
			ps.setString(7, usuario == null ? "" : usuario);
			exito = ps.executeUpdate() >= 1;
			ps.close();
		} catch (final Exception e) {
			LOG.error("GastoAreaDAO.guardarValorDelMes: " + e.toString());
		} finally {
			cerrar(con1);
		}
		return exito;
	}

	private static void llenar(final PreparedStatement ps, final Gasto g) throws java.sql.SQLException {
		ps.setString(1, g.area);
		ps.setString(2, g.concepto);
		ps.setString(3, g.tipo);
		ps.setDouble(4, g.esVariable() ? 0 : g.valorMes);
		ps.setString(5, g.vigenciaDesde);
		if (g.vigenciaHasta == null || g.vigenciaHasta.trim().length() == 0) {
			ps.setNull(6, java.sql.Types.DATE);
		} else {
			ps.setString(6, g.vigenciaHasta);
		}
		ps.setString(7, g.activo ? "S" : "N");
		ps.setString(8, g.usuario == null ? "" : g.usuario);
	}

	private static String texto(final String s) {
		return s == null ? "" : s;
	}

	private static void cerrar(final Connection con1) {
		try {
			if (con1 != null) {
				con1.close();
			}
		} catch (final Exception e) {
		}
	}
}

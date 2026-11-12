package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Los resultados de las preguntas que el huellero del POS le hace a los
 * empleados al dar ingreso.
 *
 * Todo vive en el esquema general: pregunta_empleado, opcion_respuesta_pregunta,
 * opcion_respuesta y respuesta_empleado (ver sql/preguntas_empleado_biometria.sql),
 * junto con empleado y tipo_empleado, asi que una sola conexion alcanza.
 *
 * SI ACERTO O NO
 *
 * respuesta_empleado.correcta guarda el resultado AL MOMENTO de responder. Las
 * filas viejas, de antes de que existiera la columna, la tienen en NULL; para
 * esas se recurre a opcion_respuesta_pregunta. Por eso todo usa
 * coalesce(r.correcta, orp.correcta): con el guardado manda el guardado, sin el
 * se calcula. Editar mas tarde cual era la opcion correcta no cambia el
 * historico de lo que ya se contesto.
 *
 * Si la consulta falla -lo normal es que falte correr el script de las
 * columnas- el mensaje sube hasta la pantalla en vez de mostrar una tabla vacia
 * que se lee como "nadie ha respondido".
 */
public class PreguntaEmpleadoResultadoDAO {

	/** Un empleado activo con lo que ha respondido en el rango. */
	public static class ResumenEmpleado {
		public int id;
		public String nombre = "";
		public String nombreLargo = "";
		public String cargo = "";
		public int respondidas;
		public int correctas;
		public String ultima = "";
	}

	/** Una pregunta con cuantas veces la contesto un empleado y cuantas acerto. */
	public static class PorPregunta {
		public int idPregunta;
		public String descripcion = "";
		public String respuestaCorrecta = "";
		public int veces;
		public int correctas;
	}

	/** Una respuesta puntual. */
	public static class Respuesta {
		public String fecha = "";
		public String pregunta = "";
		public String respuesta = "";
		public String respuestaCorrecta = "";
		public boolean correcta;
		public int idTienda;
	}

	/**
	 * Los empleados ACTIVOS, respondan o no en el rango: quien no ha contestado
	 * nada tiene que verse como tal, no desaparecer de la lista.
	 *
	 * @param desde yyyy-MM-dd, incluido
	 * @param hasta yyyy-MM-dd, incluido
	 * @throws Exception si la consulta falla; quien llama decide que decirle al usuario
	 */
	public static ArrayList<ResumenEmpleado> obtenerResumen(final String desde, final String hasta) throws Exception {
		final ArrayList<ResumenEmpleado> lista = new ArrayList<ResumenEmpleado>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			final String sql = "select e.id, e.nombre, e.nombre_largo, t.descripcion as cargo, "
					+ "count(r.idpregunta) as respondidas, "
					+ "coalesce(sum(coalesce(r.correcta, orp.correcta)), 0) as correctas, "
					+ "date_format(max(r.fecha_respuesta), '%Y-%m-%d %H:%i') as ultima "
					+ "from empleado e "
					+ "left join tipo_empleado t on t.idtipoempleado = e.idtipoempleado "
					+ "left join respuesta_empleado r on r.idempleado = e.id "
					+ "  and r.fecha_respuesta >= ? and r.fecha_respuesta < date_add(?, interval 1 day) "
					+ "left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta "
					+ "  and orp.idopcion = r.idopcion "
					+ "where e.activo = '1' "
					+ "group by e.id, e.nombre, e.nombre_largo, t.descripcion "
					+ "order by e.nombre_largo";
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setString(1, desde);
			ps.setString(2, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final ResumenEmpleado e = new ResumenEmpleado();
				e.id = rs.getInt("id");
				e.nombre = texto(rs.getString("nombre"));
				e.nombreLargo = texto(rs.getString("nombre_largo"));
				e.cargo = texto(rs.getString("cargo"));
				e.respondidas = rs.getInt("respondidas");
				e.correctas = rs.getInt("correctas");
				e.ultima = texto(rs.getString("ultima"));
				lista.add(e);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PreguntaEmpleadoResultadoDAO.obtenerResumen: " + e.toString());
			throw e;
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Por pregunta: cuantas veces la contesto ese empleado en el rango y cuantas acerto. */
	public static ArrayList<PorPregunta> obtenerPorPregunta(final int idEmpleado, final String desde,
			final String hasta) throws Exception {
		final ArrayList<PorPregunta> lista = new ArrayList<PorPregunta>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			final String sql = "select p.id, p.descripcion, count(*) as veces, "
					+ "coalesce(sum(coalesce(r.correcta, orp.correcta)), 0) as correctas, "
					+ "(select oc.contenido from opcion_respuesta_pregunta x "
					+ "   join opcion_respuesta oc on oc.id = x.idopcion "
					+ "  where x.idpregunta = p.id and x.correcta = 1 limit 1) as respuesta_correcta "
					+ "from respuesta_empleado r "
					+ "join pregunta_empleado p on p.id = r.idpregunta "
					+ "left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta "
					+ "  and orp.idopcion = r.idopcion "
					+ "where r.idempleado = ? and r.fecha_respuesta >= ? "
					+ "  and r.fecha_respuesta < date_add(?, interval 1 day) "
					+ "group by p.id, p.descripcion order by p.descripcion";
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setInt(1, idEmpleado);
			ps.setString(2, desde);
			ps.setString(3, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final PorPregunta p = new PorPregunta();
				p.idPregunta = rs.getInt("id");
				p.descripcion = texto(rs.getString("descripcion"));
				p.respuestaCorrecta = texto(rs.getString("respuesta_correcta"));
				p.veces = rs.getInt("veces");
				p.correctas = rs.getInt("correctas");
				lista.add(p);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PreguntaEmpleadoResultadoDAO.obtenerPorPregunta: " + e.toString());
			throw e;
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Cada respuesta del empleado en el rango, la mas reciente primero. Tope de 500. */
	public static ArrayList<Respuesta> obtenerRespuestas(final int idEmpleado, final String desde,
			final String hasta) throws Exception {
		final ArrayList<Respuesta> lista = new ArrayList<Respuesta>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			final String sql = "select date_format(r.fecha_respuesta, '%Y-%m-%d %H:%i') as fecha, "
					+ "p.descripcion as pregunta, o.contenido as respuesta, "
					+ "coalesce(r.correcta, orp.correcta, 0) as correcta, r.idtienda, "
					+ "(select oc.contenido from opcion_respuesta_pregunta x "
					+ "   join opcion_respuesta oc on oc.id = x.idopcion "
					+ "  where x.idpregunta = p.id and x.correcta = 1 limit 1) as respuesta_correcta "
					+ "from respuesta_empleado r "
					+ "join pregunta_empleado p on p.id = r.idpregunta "
					+ "left join opcion_respuesta o on o.id = r.idopcion "
					+ "left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta "
					+ "  and orp.idopcion = r.idopcion "
					+ "where r.idempleado = ? and r.fecha_respuesta >= ? "
					+ "  and r.fecha_respuesta < date_add(?, interval 1 day) "
					+ "order by r.fecha_respuesta desc limit 500";
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setInt(1, idEmpleado);
			ps.setString(2, desde);
			ps.setString(3, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Respuesta r = new Respuesta();
				r.fecha = texto(rs.getString("fecha"));
				r.pregunta = texto(rs.getString("pregunta"));
				r.respuesta = texto(rs.getString("respuesta"));
				r.respuestaCorrecta = texto(rs.getString("respuesta_correcta"));
				r.correcta = rs.getInt("correcta") == 1;
				r.idTienda = rs.getInt("idtienda");
				lista.add(r);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PreguntaEmpleadoResultadoDAO.obtenerRespuestas: " + e.toString());
			throw e;
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// =======================================================================
	// Reporte de UN empleado en un rango (pantalla ReporteEmpleadoPreguntas)
	// =======================================================================

	public static class Empleado {
		public int id;
		public int idTipo;
		public String nombre = "";
		public String nombreLargo = "";
		public String cargo = "";
		public boolean activo;
	}

	/** Lo global del rango: cuantas contesto, cuantas acerto, cuantas preguntas distintas y desde/hasta cuando. */
	public static class Totales {
		public int respondidas;
		public int correctas;
		public int distintas;
		public String primera = "";
		public String ultima = "";
	}

	/** Una semana (lunes a domingo) con lo que contesto. */
	public static class Semana {
		public String inicio = "";
		public int respondidas;
		public int correctas;
	}

	/** Contra quien compararlo: su cargo y toda la empresa, en el mismo rango. */
	public static class Comparativo {
		public int cargoEmpleados;
		public int cargoRespondidas;
		public int cargoCorrectas;
		public int empresaRespondidas;
		public int empresaCorrectas;
	}

	/** Una pregunta vigente para su cargo que no contesto en el rango. */
	public static class Pendiente {
		public int idPregunta;
		public String descripcion = "";
		/** Cuantas veces la ha contestado en toda su historia (0 = nunca la ha visto). */
		public int vecesHistorico;
	}

	/** El empleado (activo o no), o null si no existe. */
	public static Empleado obtenerEmpleado(final int idEmpleado) throws Exception {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select e.id, e.nombre, e.nombre_largo, e.idtipoempleado, e.activo, t.descripcion as cargo "
							+ "from empleado e left join tipo_empleado t on t.idtipoempleado = e.idtipoempleado where e.id = ?")) {
				ps.setInt(1, idEmpleado);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return (null);
					}
					final Empleado e = new Empleado();
					e.id = rs.getInt("id");
					e.idTipo = rs.getInt("idtipoempleado");
					e.nombre = texto(rs.getString("nombre"));
					e.nombreLargo = texto(rs.getString("nombre_largo"));
					e.cargo = texto(rs.getString("cargo"));
					e.activo = "1".equals(rs.getString("activo"));
					return (e);
				}
			}
		} finally {
			cerrar(cn);
		}
	}

	public static Totales obtenerTotales(final int idEmpleado, final String desde, final String hasta) throws Exception {
		final Totales t = new Totales();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select count(*) as respondidas, coalesce(sum(coalesce(r.correcta, orp.correcta)), 0) as correctas, "
							+ "count(distinct r.idpregunta) as distintas, "
							+ "date_format(min(r.fecha_respuesta), '%Y-%m-%d %H:%i') as primera, "
							+ "date_format(max(r.fecha_respuesta), '%Y-%m-%d %H:%i') as ultima "
							+ "from respuesta_empleado r "
							+ "left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta and orp.idopcion = r.idopcion "
							+ "where r.idempleado = ? and r.fecha_respuesta >= ? and r.fecha_respuesta < date_add(?, interval 1 day)")) {
				ps.setInt(1, idEmpleado);
				ps.setString(2, desde);
				ps.setString(3, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						t.respondidas = rs.getInt("respondidas");
						t.correctas = rs.getInt("correctas");
						t.distintas = rs.getInt("distintas");
						t.primera = texto(rs.getString("primera"));
						t.ultima = texto(rs.getString("ultima"));
					}
				}
			}
		} finally {
			cerrar(cn);
		}
		return (t);
	}

	/** Por semana (la semana empieza el lunes), de la mas antigua a la mas reciente. */
	public static ArrayList<Semana> obtenerSemanas(final int idEmpleado, final String desde, final String hasta)
			throws Exception {
		final ArrayList<Semana> lista = new ArrayList<Semana>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select date_format(date_sub(date(r.fecha_respuesta), interval weekday(r.fecha_respuesta) day), '%Y-%m-%d') as inicio, "
							+ "count(*) as respondidas, coalesce(sum(coalesce(r.correcta, orp.correcta)), 0) as correctas "
							+ "from respuesta_empleado r "
							+ "left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta and orp.idopcion = r.idopcion "
							+ "where r.idempleado = ? and r.fecha_respuesta >= ? and r.fecha_respuesta < date_add(?, interval 1 day) "
							+ "group by inicio order by inicio")) {
				ps.setInt(1, idEmpleado);
				ps.setString(2, desde);
				ps.setString(3, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final Semana s = new Semana();
						s.inicio = texto(rs.getString("inicio"));
						s.respondidas = rs.getInt("respondidas");
						s.correctas = rs.getInt("correctas");
						lista.add(s);
					}
				}
			}
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Lo mismo del rango para los empleados del mismo cargo y para toda la empresa. */
	public static Comparativo obtenerComparativo(final int idTipo, final String desde, final String hasta) throws Exception {
		final Comparativo c = new Comparativo();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select count(*) as respondidas, coalesce(sum(coalesce(r.correcta, orp.correcta)), 0) as correctas, "
							+ "coalesce(sum(e.idtipoempleado = ?), 0) as respondidas_cargo, "
							+ "coalesce(sum(case when e.idtipoempleado = ? then coalesce(r.correcta, orp.correcta) else 0 end), 0) as correctas_cargo, "
							+ "count(distinct case when e.idtipoempleado = ? then r.idempleado end) as empleados_cargo "
							+ "from respuesta_empleado r join empleado e on e.id = r.idempleado "
							+ "left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta and orp.idopcion = r.idopcion "
							+ "where r.fecha_respuesta >= ? and r.fecha_respuesta < date_add(?, interval 1 day)")) {
				ps.setInt(1, idTipo);
				ps.setInt(2, idTipo);
				ps.setInt(3, idTipo);
				ps.setString(4, desde);
				ps.setString(5, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						c.empresaRespondidas = rs.getInt("respondidas");
						c.empresaCorrectas = rs.getInt("correctas");
						c.cargoRespondidas = rs.getInt("respondidas_cargo");
						c.cargoCorrectas = rs.getInt("correctas_cargo");
						c.cargoEmpleados = rs.getInt("empleados_cargo");
					}
				}
			}
		} finally {
			cerrar(cn);
		}
		return (c);
	}

	/** Las preguntas vigentes HOY para su cargo (o para todos) que no contesto en el rango. */
	public static ArrayList<Pendiente> obtenerPendientes(final int idEmpleado, final int idTipo, final String desde,
			final String hasta) throws Exception {
		final ArrayList<Pendiente> lista = new ArrayList<Pendiente>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select p.id, p.descripcion, "
							+ "(select count(*) from respuesta_empleado h where h.idempleado = ? and h.idpregunta = p.id) as historico "
							+ "from pregunta_empleado p "
							+ "where p.activo = 1 and current_date between p.fecha_inicio and p.fecha_final "
							+ "  and exists (select 1 from pregunta_empleado_tipo t where t.idpregunta = p.id and t.idtipoempleado in (?, 0)) "
							+ "  and not exists (select 1 from respuesta_empleado r where r.idempleado = ? and r.idpregunta = p.id "
							+ "      and r.fecha_respuesta >= ? and r.fecha_respuesta < date_add(?, interval 1 day)) "
							+ "order by historico, p.id")) {
				ps.setInt(1, idEmpleado);
				ps.setInt(2, idTipo);
				ps.setInt(3, idEmpleado);
				ps.setString(4, desde);
				ps.setString(5, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final Pendiente pe = new Pendiente();
						pe.idPregunta = rs.getInt("id");
						pe.descripcion = texto(rs.getString("descripcion"));
						pe.vecesHistorico = rs.getInt("historico");
						lista.add(pe);
					}
				}
			}
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PreguntaEmpleadoResultadoDAO: no cerro la conexion, " + e.toString());
		}
	}
}

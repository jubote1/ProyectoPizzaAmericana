package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Banco de preguntas que el huellero del POS le hace al empleado al dar ingreso: administracion desde el central.
 *
 * Todo vive en el esquema general, las mismas tablas que ya lee el huellero (EmpleadoBiometriaDAO en el POS):
 *   pregunta_empleado         la pregunta, su vigencia y si esta activa
 *   pregunta_empleado_tipo    a que cargos (tipo_empleado) se le hace; idtipoempleado = 0 significa TODOS los cargos
 *   opcion_respuesta          catalogo de textos de opcion (se reutiliza por texto, nunca se edita)
 *   opcion_respuesta_pregunta las opciones de cada pregunta y cuales son correctas
 *   respuesta_empleado        lo que respondio cada empleado
 *
 * Reglas que se respetan a proposito:
 *  - Las preguntas NO se borran, se inactivan: lo que ya respondieron los empleados tiene que seguir explicandose.
 *  - Una opcion que ya se contesto y se quita de la pregunta sigue mostrandose en el historial (respuesta_empleado
 *    guarda si acerto AL MOMENTO de responder, y el texto vive en el catalogo).
 *  - "Todos los cargos" se guarda como la fila 0 MAS una fila por cada cargo que existe hoy: el huellero que todavia no
 *    conoce el 0 sigue funcionando con las filas explicitas, y el que si lo conoce cubre tambien los cargos que se creen luego.
 */
public class BancoPreguntasEmpleadoDAO {

	public static class Cargo {
		public int id;
		public String nombre = "";
	}

	public static class Opcion {
		public int idOpcion;
		public String texto = "";
		public boolean correcta;
	}

	public static class Pregunta {
		public int id;
		public String descripcion = "";
		public String fechaInicio = "";
		public String fechaFinal = "";
		public boolean activa;
		public boolean todos;
		public List<Integer> cargos = new ArrayList<Integer>();
		public List<Opcion> opciones = new ArrayList<Opcion>();
		public int respuestas;
		public int aciertos;
		public int empleados;
	}

	/** Los cargos que existen, para escoger a quien le aplica cada pregunta. */
	public static List<Cargo> listarCargos() throws Exception {
		final List<Cargo> lista = new ArrayList<Cargo>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select idtipoempleado, descripcion from tipo_empleado order by descripcion");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final Cargo c = new Cargo();
					c.id = rs.getInt(1);
					c.nombre = rs.getString(2) == null ? "" : rs.getString(2);
					lista.add(c);
				}
			}
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * Todas las preguntas con sus cargos, opciones y cuanto se han respondido.
	 *
	 * @param estado "todas", "vigentes" (activas y dentro de fechas), "inactivas" o "vencidas"
	 */
	public static List<Pregunta> listar(final String estado) throws Exception {
		final Map<Integer, Pregunta> porId = new LinkedHashMap<Integer, Pregunta>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			final StringBuilder donde = new StringBuilder(" where 1 = 1");
			if ("vigentes".equals(estado)) {
				donde.append(" and p.activo = 1 and current_date between p.fecha_inicio and p.fecha_final");
			} else if ("inactivas".equals(estado)) {
				donde.append(" and p.activo = 0");
			} else if ("vencidas".equals(estado)) {
				donde.append(" and p.fecha_final < current_date");
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select p.id, p.descripcion, date_format(p.fecha_inicio, '%Y-%m-%d') as fi, "
							+ "date_format(p.fecha_final, '%Y-%m-%d') as ff, p.activo, "
							+ "(select count(*) from respuesta_empleado r where r.idpregunta = p.id) as respuestas, "
							+ "(select count(distinct r.idempleado) from respuesta_empleado r where r.idpregunta = p.id) as empleados, "
							+ "(select coalesce(sum(coalesce(r.correcta, orp.correcta)), 0) from respuesta_empleado r "
							+ "   left join opcion_respuesta_pregunta orp on orp.idpregunta = r.idpregunta and orp.idopcion = r.idopcion "
							+ "  where r.idpregunta = p.id) as aciertos "
							+ "from pregunta_empleado p" + donde + " order by p.activo desc, p.id desc");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final Pregunta p = new Pregunta();
					p.id = rs.getInt("id");
					p.descripcion = rs.getString("descripcion") == null ? "" : rs.getString("descripcion");
					p.fechaInicio = rs.getString("fi") == null ? "" : rs.getString("fi");
					p.fechaFinal = rs.getString("ff") == null ? "" : rs.getString("ff");
					p.activa = rs.getInt("activo") == 1;
					p.respuestas = rs.getInt("respuestas");
					p.empleados = rs.getInt("empleados");
					p.aciertos = rs.getInt("aciertos");
					porId.put(p.id, p);
				}
			}
			if (porId.isEmpty()) {
				return new ArrayList<Pregunta>();
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select idpregunta, idtipoempleado from pregunta_empleado_tipo order by idpregunta, idtipoempleado");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final Pregunta p = porId.get(rs.getInt(1));
					if (p == null) {
						continue;
					}
					final int cargo = rs.getInt(2);
					if (cargo == 0) {
						p.todos = true;
					} else if (!p.cargos.contains(Integer.valueOf(cargo))) {
						p.cargos.add(Integer.valueOf(cargo));
					}
				}
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"select orp.idpregunta, orp.idopcion, o.contenido, orp.correcta from opcion_respuesta_pregunta orp "
							+ "join opcion_respuesta o on o.id = orp.idopcion order by orp.idpregunta, orp.id");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final Pregunta p = porId.get(rs.getInt(1));
					if (p == null) {
						continue;
					}
					final Opcion o = new Opcion();
					o.idOpcion = rs.getInt(2);
					o.texto = rs.getString(3) == null ? "" : rs.getString(3);
					o.correcta = rs.getInt(4) == 1;
					p.opciones.add(o);
				}
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BancoPreguntasEmpleadoDAO.listar: " + e);
			throw e;
		} finally {
			cerrar(cn);
		}
		return new ArrayList<Pregunta>(porId.values());
	}

	/**
	 * Crea (idPregunta = 0) o actualiza una pregunta con sus cargos y opciones, todo en una transaccion.
	 * Las validaciones de contenido las hace el controlador; aqui solo se escribe.
	 *
	 * @return el id de la pregunta
	 */
	public static int guardar(final int idPregunta, final String descripcion, final String fechaInicio,
			final String fechaFinal, final boolean activa, final boolean todos, final List<Integer> cargos,
			final List<Opcion> opciones) throws Exception {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			cn.setAutoCommit(false);
			int id = idPregunta;
			if (id <= 0) {
				try (PreparedStatement ps = cn.prepareStatement(
						"insert into pregunta_empleado (descripcion, fecha_inicio, fecha_final, activo) values (?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS)) {
					ps.setString(1, descripcion);
					ps.setString(2, fechaInicio);
					ps.setString(3, fechaFinal);
					ps.setInt(4, activa ? 1 : 0);
					ps.executeUpdate();
					try (ResultSet k = ps.getGeneratedKeys()) {
						if (!k.next()) {
							throw new Exception("No se obtuvo el id de la pregunta nueva.");
						}
						id = k.getInt(1);
					}
				}
			} else {
				try (PreparedStatement ps = cn.prepareStatement(
						"update pregunta_empleado set descripcion = ?, fecha_inicio = ?, fecha_final = ?, activo = ? where id = ?")) {
					ps.setString(1, descripcion);
					ps.setString(2, fechaInicio);
					ps.setString(3, fechaFinal);
					ps.setInt(4, activa ? 1 : 0);
					ps.setInt(5, id);
					if (ps.executeUpdate() == 0) {
						throw new Exception("La pregunta " + id + " no existe.");
					}
				}
			}

			// Cargos: se reescriben completos.
			try (PreparedStatement ps = cn.prepareStatement("delete from pregunta_empleado_tipo where idpregunta = ?")) {
				ps.setInt(1, id);
				ps.executeUpdate();
			}
			final List<Integer> aInsertar = new ArrayList<Integer>();
			if (todos) {
				aInsertar.add(Integer.valueOf(0));
				try (PreparedStatement ps = cn.prepareStatement("select idtipoempleado from tipo_empleado");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						aInsertar.add(Integer.valueOf(rs.getInt(1)));
					}
				}
			} else {
				aInsertar.addAll(cargos);
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"insert into pregunta_empleado_tipo (idpregunta, idtipoempleado) values (?, ?)")) {
				for (final Integer c : aInsertar) {
					ps.setInt(1, id);
					ps.setInt(2, c.intValue());
					ps.addBatch();
				}
				ps.executeBatch();
			}

			// Opciones: el texto se reutiliza del catalogo; las que ya estaban se conservan (y solo cambia si es correcta).
			final Map<Integer, Boolean> quedan = new HashMap<Integer, Boolean>();
			for (final Opcion o : opciones) {
				final int idOpcion = idDeOpcion(cn, o.texto);
				quedan.put(Integer.valueOf(idOpcion), Boolean.valueOf(o.correcta));
			}
			final Map<Integer, Integer> existentes = new HashMap<Integer, Integer>(); // idopcion -> id de la fila
			try (PreparedStatement ps = cn.prepareStatement(
					"select id, idopcion from opcion_respuesta_pregunta where idpregunta = ?")) {
				ps.setInt(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						existentes.put(Integer.valueOf(rs.getInt(2)), Integer.valueOf(rs.getInt(1)));
					}
				}
			}
			for (final Map.Entry<Integer, Integer> e : existentes.entrySet()) {
				if (!quedan.containsKey(e.getKey())) {
					try (PreparedStatement ps = cn.prepareStatement("delete from opcion_respuesta_pregunta where id = ?")) {
						ps.setInt(1, e.getValue().intValue());
						ps.executeUpdate();
					}
				}
			}
			for (final Map.Entry<Integer, Boolean> e : quedan.entrySet()) {
				final Integer fila = existentes.get(e.getKey());
				if (fila != null) {
					try (PreparedStatement ps = cn.prepareStatement(
							"update opcion_respuesta_pregunta set correcta = ? where id = ?")) {
						ps.setInt(1, e.getValue().booleanValue() ? 1 : 0);
						ps.setInt(2, fila.intValue());
						ps.executeUpdate();
					}
				} else {
					try (PreparedStatement ps = cn.prepareStatement(
							"insert into opcion_respuesta_pregunta (idopcion, idpregunta, correcta) values (?, ?, ?)")) {
						ps.setInt(1, e.getKey().intValue());
						ps.setInt(2, id);
						ps.setInt(3, e.getValue().booleanValue() ? 1 : 0);
						ps.executeUpdate();
					}
				}
			}
			cn.commit();
			return id;
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BancoPreguntasEmpleadoDAO.guardar: " + e);
			if (cn != null) {
				try {
					cn.rollback();
				} catch (final Exception ignorar) {
					// ya se informa el error original
				}
			}
			throw e;
		} finally {
			cerrar(cn);
		}
	}

	/** Activa o inactiva una pregunta. false si no existe. */
	public static boolean cambiarEstado(final int idPregunta, final boolean activa) throws Exception {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			if (cn == null) {
				throw new Exception("No hay conexion a la base general.");
			}
			try (PreparedStatement ps = cn.prepareStatement("update pregunta_empleado set activo = ? where id = ?")) {
				ps.setInt(1, activa ? 1 : 0);
				ps.setInt(2, idPregunta);
				return ps.executeUpdate() > 0;
			}
		} finally {
			cerrar(cn);
		}
	}

	/** El id del texto en el catalogo; si no esta, lo crea. */
	private static int idDeOpcion(final Connection cn, final String texto) throws Exception {
		try (PreparedStatement ps = cn.prepareStatement("select id from opcion_respuesta where contenido = ? order by id limit 1")) {
			ps.setString(1, texto);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			}
		}
		try (PreparedStatement ps = cn.prepareStatement("insert into opcion_respuesta (contenido) values (?)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, texto);
			ps.executeUpdate();
			try (ResultSet k = ps.getGeneratedKeys()) {
				if (k.next()) {
					return k.getInt(1);
				}
			}
		}
		throw new Exception("No se pudo crear la opcion '" + texto + "'.");
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BancoPreguntasEmpleadoDAO: no cerro la conexion, " + e);
		}
	}
}

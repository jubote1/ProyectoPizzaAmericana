package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.OpcionMercadeo;
import capaModeloCC.PreguntaMercadeo;
import conexionCC.ConexionBaseDatos;

/**
 * DAO de la configuracion de preguntas de encuestas de mercadeo.
 * Trabaja sobre las tablas pregunta_mercadeo y opcion_mercadeo de la base de
 * datos pizzaamericana.
 *
 * Nota sobre PreparedStatement: el resto del proyecto arma los SQL por
 * concatenacion con Statement. Aqui se usa PreparedStatement porque los textos
 * de preguntas y opciones los escribe un usuario de mercadeo y un apostrofo
 * romperia la sentencia.
 */
public class PreguntaMercadeoDAO {

	/**
	 * Obtiene las preguntas vigentes el dia de hoy para el canal indicado, con sus
	 * opciones ya cargadas y ordenadas.
	 *
	 * @param esPuntoVenta true para pedidos de punto de venta, false para domicilio
	 * @return lista de preguntas vigentes; vacia si no hay ninguna
	 */
	public static ArrayList<PreguntaMercadeo> obtenerPreguntasVigentes(final boolean esPuntoVenta) {
		final ArrayList<PreguntaMercadeo> preguntas = new ArrayList<PreguntaMercadeo>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String columnaCanal = esPuntoVenta ? "aplica_pv" : "aplica_domicilio";
			final String consulta = "select idpregunta, titulo, descripcion, tipo, fecha_inicio, fecha_fin, orden, "
					+ "obligatoria, aplica_pv, aplica_domicilio, activo, usuario_creacion "
					+ "from pregunta_mercadeo "
					+ "where activo = 'S' and " + columnaCanal + " = 'S' "
					+ "and curdate() between fecha_inicio and fecha_fin "
					+ "order by orden, idpregunta";
			logger.info(consulta);
			final Statement stm = con1.createStatement();
			final ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				final PreguntaMercadeo pregunta = new PreguntaMercadeo();
				pregunta.setIdPregunta(rs.getInt("idpregunta"));
				pregunta.setTitulo(rs.getString("titulo"));
				pregunta.setDescripcion(rs.getString("descripcion"));
				pregunta.setTipo(rs.getString("tipo"));
				pregunta.setFechaInicio(rs.getString("fecha_inicio"));
				pregunta.setFechaFin(rs.getString("fecha_fin"));
				pregunta.setOrden(rs.getInt("orden"));
				pregunta.setObligatoria(rs.getString("obligatoria"));
				pregunta.setAplicaPV(rs.getString("aplica_pv"));
				pregunta.setAplicaDomicilio(rs.getString("aplica_domicilio"));
				pregunta.setActivo(rs.getString("activo"));
				pregunta.setUsuarioCreacion(rs.getString("usuario_creacion"));
				preguntas.add(pregunta);
			}
			rs.close();
			stm.close();
			// Solo las preguntas de opciones necesitan cargar el detalle
			for (final PreguntaMercadeo pregunta : preguntas) {
				if (PreguntaMercadeo.TIPO_OPCIONES.equals(pregunta.getTipo())) {
					pregunta.setOpciones(obtenerOpciones(con1, pregunta.getIdPregunta(), true));
				}
			}
			con1.close();
		} catch (final Exception e) {
			logger.error("obtenerPreguntasVigentes: " + e.toString());
			cerrar(con1);
		}
		return (preguntas);
	}

	/**
	 * Obtiene todas las preguntas configuradas, vigentes o no, para el maestro de
	 * configuracion del POS.
	 *
	 * @return lista completa de preguntas con sus opciones
	 */
	public static ArrayList<PreguntaMercadeo> obtenerTodasLasPreguntas() {
		final ArrayList<PreguntaMercadeo> preguntas = new ArrayList<PreguntaMercadeo>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String consulta = "select idpregunta, titulo, descripcion, tipo, fecha_inicio, fecha_fin, orden, "
					+ "obligatoria, aplica_pv, aplica_domicilio, activo, usuario_creacion "
					+ "from pregunta_mercadeo order by orden, idpregunta";
			logger.info(consulta);
			final Statement stm = con1.createStatement();
			final ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				final PreguntaMercadeo pregunta = new PreguntaMercadeo();
				pregunta.setIdPregunta(rs.getInt("idpregunta"));
				pregunta.setTitulo(rs.getString("titulo"));
				pregunta.setDescripcion(rs.getString("descripcion"));
				pregunta.setTipo(rs.getString("tipo"));
				pregunta.setFechaInicio(rs.getString("fecha_inicio"));
				pregunta.setFechaFin(rs.getString("fecha_fin"));
				pregunta.setOrden(rs.getInt("orden"));
				pregunta.setObligatoria(rs.getString("obligatoria"));
				pregunta.setAplicaPV(rs.getString("aplica_pv"));
				pregunta.setAplicaDomicilio(rs.getString("aplica_domicilio"));
				pregunta.setActivo(rs.getString("activo"));
				pregunta.setUsuarioCreacion(rs.getString("usuario_creacion"));
				preguntas.add(pregunta);
			}
			rs.close();
			stm.close();
			for (final PreguntaMercadeo pregunta : preguntas) {
				pregunta.setOpciones(obtenerOpciones(con1, pregunta.getIdPregunta(), false));
			}
			con1.close();
		} catch (final Exception e) {
			logger.error("obtenerTodasLasPreguntas: " + e.toString());
			cerrar(con1);
		}
		return (preguntas);
	}

	/**
	 * Inserta una pregunta nueva.
	 *
	 * @param pregunta pregunta a insertar
	 * @return el idpregunta generado, o 0 si fallo
	 */
	public static int insertarPregunta(final PreguntaMercadeo pregunta) {
		int idPregunta = 0;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String insert = "insert into pregunta_mercadeo (titulo, descripcion, tipo, fecha_inicio, fecha_fin, "
					+ "orden, obligatoria, aplica_pv, aplica_domicilio, activo, usuario_creacion) "
					+ "values (?,?,?,?,?,?,?,?,?,?,?)";
			logger.info(insert + " titulo=" + pregunta.getTitulo());
			final PreparedStatement pst = con1.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
			pst.setString(1, pregunta.getTitulo());
			pst.setString(2, pregunta.getDescripcion());
			pst.setString(3, pregunta.getTipo());
			pst.setString(4, pregunta.getFechaInicio());
			pst.setString(5, pregunta.getFechaFin());
			pst.setInt(6, pregunta.getOrden());
			pst.setString(7, pregunta.getObligatoria());
			pst.setString(8, pregunta.getAplicaPV());
			pst.setString(9, pregunta.getAplicaDomicilio());
			pst.setString(10, pregunta.getActivo());
			pst.setString(11, pregunta.getUsuarioCreacion());
			pst.executeUpdate();
			final ResultSet rs = pst.getGeneratedKeys();
			if (rs.next()) {
				idPregunta = rs.getInt(1);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("insertarPregunta: " + e.toString());
			cerrar(con1);
		}
		return (idPregunta);
	}

	/**
	 * Actualiza una pregunta existente.
	 *
	 * @param pregunta pregunta con el idpregunta a actualizar
	 * @return true si actualizo
	 */
	public static boolean actualizarPregunta(final PreguntaMercadeo pregunta) {
		boolean resultado = false;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String update = "update pregunta_mercadeo set titulo = ?, descripcion = ?, tipo = ?, "
					+ "fecha_inicio = ?, fecha_fin = ?, orden = ?, obligatoria = ?, aplica_pv = ?, "
					+ "aplica_domicilio = ?, activo = ? where idpregunta = ?";
			logger.info(update + " idpregunta=" + pregunta.getIdPregunta());
			final PreparedStatement pst = con1.prepareStatement(update);
			pst.setString(1, pregunta.getTitulo());
			pst.setString(2, pregunta.getDescripcion());
			pst.setString(3, pregunta.getTipo());
			pst.setString(4, pregunta.getFechaInicio());
			pst.setString(5, pregunta.getFechaFin());
			pst.setInt(6, pregunta.getOrden());
			pst.setString(7, pregunta.getObligatoria());
			pst.setString(8, pregunta.getAplicaPV());
			pst.setString(9, pregunta.getAplicaDomicilio());
			pst.setString(10, pregunta.getActivo());
			pst.setInt(11, pregunta.getIdPregunta());
			resultado = pst.executeUpdate() > 0;
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("actualizarPregunta: " + e.toString());
			cerrar(con1);
		}
		return (resultado);
	}

	/**
	 * Inactiva una pregunta. No se borra fisicamente para no perder la trazabilidad
	 * de las respuestas historicas que la referencian.
	 *
	 * @param idPregunta pregunta a inactivar
	 * @return true si inactivo
	 */
	public static boolean inactivarPregunta(final int idPregunta) {
		boolean resultado = false;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String update = "update pregunta_mercadeo set activo = 'N' where idpregunta = " + idPregunta;
			logger.info(update);
			final Statement stm = con1.createStatement();
			resultado = stm.executeUpdate(update) > 0;
			stm.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("inactivarPregunta: " + e.toString());
			cerrar(con1);
		}
		return (resultado);
	}

	/**
	 * Inserta una opcion de respuesta para una pregunta tipo O.
	 *
	 * @param opcion opcion a insertar
	 * @return el idopcion generado, o 0 si fallo
	 */
	public static int insertarOpcion(final OpcionMercadeo opcion) {
		int idOpcion = 0;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String insert = "insert into opcion_mercadeo (idpregunta, descripcion, orden, activo) "
					+ "values (?,?,?,?)";
			logger.info(insert + " idpregunta=" + opcion.getIdPregunta());
			final PreparedStatement pst = con1.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
			pst.setInt(1, opcion.getIdPregunta());
			pst.setString(2, opcion.getDescripcion());
			pst.setInt(3, opcion.getOrden());
			pst.setString(4, opcion.getActivo());
			pst.executeUpdate();
			final ResultSet rs = pst.getGeneratedKeys();
			if (rs.next()) {
				idOpcion = rs.getInt(1);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("insertarOpcion: " + e.toString());
			cerrar(con1);
		}
		return (idOpcion);
	}

	/**
	 * Actualiza una opcion de respuesta.
	 *
	 * @param opcion opcion con el idopcion a actualizar
	 * @return true si actualizo
	 */
	public static boolean actualizarOpcion(final OpcionMercadeo opcion) {
		boolean resultado = false;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String update = "update opcion_mercadeo set descripcion = ?, orden = ?, activo = ? "
					+ "where idopcion = ?";
			logger.info(update + " idopcion=" + opcion.getIdOpcion());
			final PreparedStatement pst = con1.prepareStatement(update);
			pst.setString(1, opcion.getDescripcion());
			pst.setInt(2, opcion.getOrden());
			pst.setString(3, opcion.getActivo());
			pst.setInt(4, opcion.getIdOpcion());
			resultado = pst.executeUpdate() > 0;
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("actualizarOpcion: " + e.toString());
			cerrar(con1);
		}
		return (resultado);
	}

	/**
	 * Inactiva una opcion. Igual que con las preguntas, no se borra fisicamente
	 * porque las respuestas historicas guardan el idopcion.
	 *
	 * @param idOpcion opcion a inactivar
	 * @return true si inactivo
	 */
	public static boolean inactivarOpcion(final int idOpcion) {
		boolean resultado = false;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String update = "update opcion_mercadeo set activo = 'N' where idopcion = " + idOpcion;
			logger.info(update);
			final Statement stm = con1.createStatement();
			resultado = stm.executeUpdate(update) > 0;
			stm.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("inactivarOpcion: " + e.toString());
			cerrar(con1);
		}
		return (resultado);
	}

	/**
	 * Carga las opciones de una pregunta reutilizando una conexion ya abierta.
	 *
	 * @param con1           conexion abierta
	 * @param idPregunta     pregunta de la cual cargar opciones
	 * @param soloActivas    true para traer unicamente las opciones activas
	 * @return lista de opciones ordenadas
	 */
	private static ArrayList<OpcionMercadeo> obtenerOpciones(final Connection con1, final int idPregunta,
			final boolean soloActivas) throws Exception {
		final ArrayList<OpcionMercadeo> opciones = new ArrayList<OpcionMercadeo>();
		final String filtroActivas = soloActivas ? " and activo = 'S' " : " ";
		final String consulta = "select idopcion, idpregunta, descripcion, orden, activo from opcion_mercadeo "
				+ "where idpregunta = " + idPregunta + filtroActivas + "order by orden, idopcion";
		final Statement stm = con1.createStatement();
		final ResultSet rs = stm.executeQuery(consulta);
		while (rs.next()) {
			final OpcionMercadeo opcion = new OpcionMercadeo();
			opcion.setIdOpcion(rs.getInt("idopcion"));
			opcion.setIdPregunta(rs.getInt("idpregunta"));
			opcion.setDescripcion(rs.getString("descripcion"));
			opcion.setOrden(rs.getInt("orden"));
			opcion.setActivo(rs.getString("activo"));
			opciones.add(opcion);
		}
		rs.close();
		stm.close();
		return (opciones);
	}

	/**
	 * Cierra la conexion ignorando cualquier error, para usar en los bloques catch.
	 *
	 * @param con1 conexion a cerrar, puede venir en null
	 */
	private static void cerrar(final Connection con1) {
		try {
			if (con1 != null) {
				con1.close();
			}
		} catch (final Exception e1) {
			// Se ignora: ya estamos manejando un error previo
		}
	}
}

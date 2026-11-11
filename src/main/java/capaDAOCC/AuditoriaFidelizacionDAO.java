package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Las consultas de la pantalla de auditoria de fidelizacion: quien redimio
 * puntos en un rango, y todo el historial de una persona.
 *
 * POR QUE EXISTE
 *
 * Hay sospecha de que empleados se atribuyan puntos de clientes y los rediman
 * en producto. Desde septiembre de 2026 las dos tablas guardan quien atendio
 * -fidelizacion_redencion.usuario y fidelizacion_transaccion.usuario-, y con
 * eso ya se puede mirar. Antes de esa fecha esas columnas estan vacias y
 * cualquier cuenta sobre ellas sale en ceros: por eso todo lo que las use
 * informa tambien sobre cuantas filas SI tenian el dato.
 *
 * COMO SE RESUELVE EL NOMBRE DE QUIEN ATENDIO
 *
 * La columna usuario guarda dos cosas distintas segun el origen:
 *
 *   POS -> la CEDULA del empleado, que es general.empleado.nombre
 *   CC  -> el LOGIN del central,  que es pizzaamericana.usuario.nombre
 *
 * Por eso los dos LEFT JOIN. Medido el 2026-11-04: resuelve el 98% de las del
 * POS (319 de 324) y el 100% de las del contact center. NO hay que salir a las
 * tiendas para esto, aunque el id del POS venga de alla: general.empleado es
 * el espejo central que alimenta a las once tiendas.
 *
 * Ninguna de las dos llaves tiene duplicados -verificado-, asi que los joins no
 * multiplican filas.
 *
 * LAS DOS SENALES
 *
 *   cliente_es_empleado: el correo de la cuenta de puntos esta en la nomina.
 *   auto_redencion:      ademas, quien proceso la redencion ES esa misma
 *                        persona. Es la firma mas fuerte y la mas facil de
 *                        explicar en un proceso disciplinario.
 */
public class AuditoriaFidelizacionDAO {

	/** Una redencion, ya resuelta a nombres y con las senales calculadas. */
	public static class Redencion {
		public int idRedencion;
		public String fecha = "";
		public String correo = "";
		public double puntos;
		public int idTienda;
		public String tienda = "";
		public int idPedidoTienda;
		public String usuario = "";
		public String usuarioNombre = "";
		public String origen = "";
		public String estado = "";
		public boolean clienteEsEmpleado;
		public String clienteEmpleadoNombre = "";
		public boolean autoRedencion;
	}

	/** Una acumulacion de puntos: un pedido que genero puntos. */
	public static class Acumulacion {
		public String fecha = "";
		public int idTienda;
		public String tienda = "";
		public int idPedidoTienda;
		public double valorNeto;
		public double puntos;
		public double puntosRedimidos;
		public String usuario = "";
		public String usuarioNombre = "";
		public boolean vencidos;
	}

	/** Cuantas veces atendio a esta persona cada quien. La firma de la concentracion. */
	public static class Atencion {
		public String usuario = "";
		public String nombre = "";
		public int pedidos;
		public int tiendas;
		public boolean esElMismoCliente;
		/** true si no es una persona sino un canal (CONTACT, BOT, APP...). */
		public boolean esCanal;
	}

	/** Una persona que acumula puntos, con su senal de concentracion. */
	public static class Acumulador {
		public String correo = "";
		public String nombre = "";
		public int pedidos;
		public double puntos;
		public double valor;
		public int tiendas;
		public int cajerosDistintos;
		/** Pedidos atendidos por una PERSONA (sin canales). Es el denominador honesto. */
		public int pedidosConPersona;
		public String topUsuario = "";
		public String topNombre = "";
		public int topPedidos;
		public boolean esEmpleado;
		public String empleadoNombre = "";
		public int autoRedenciones;
		/** Cuantos dias distintos tuvo MAS DE UN pedido con puntos. */
		public int diasConVarios;
		/** El peor de esos dias: cuantos pedidos junto en una sola fecha. */
		public int maxPedidosDia;
	}

	/** Un dia en que la persona junto mas de un pedido con puntos. */
	public static class DiaCargado {
		public String fecha = "";
		public int pedidos;
		public double puntos;
		public double valor;
		public String tiendas = "";
		public String usuarios = "";
	}

	/**
	 * Los usuarios que NO son una persona sino un canal.
	 *
	 * Existen como filas de general.empleado (Caja 180, CONTACT 181,
	 * TIEVIRTUALKUNO 262, BOT 525) y APP ni siquiera esta ahi. Pesan: 6.169 de
	 * las ~70.000 acumulaciones al 2026-11-04.
	 *
	 * HAY QUE EXCLUIRLOS DE LA CONCENTRACION O EL REPORTE MIENTE. Un cliente
	 * que siempre pide por el contact center sale con 100% de concentracion en
	 * CONTACT, que no significa absolutamente nada: no es que siempre lo
	 * atienda la misma persona, es que siempre usa el mismo canal. Sin este
	 * filtro los primeros puestos de la lista de sospechosos son clientes
	 * perfectamente normales.
	 */
	private static final String NO_PERSONA =
			" ('CONTACT','TIEVIRTUALKUNO','BOT','APP','CAJA','AJUSTE') ";

	/** La condicion SQL que deja solo a las personas de carne y hueso. */
	private static String soloPersonas(final String alias) {
		return (" " + alias + ".usuario <> '' AND UPPER(" + alias + ".usuario) NOT IN " + NO_PERSONA + " ");
	}

	/** Los totales de una persona. */
	public static class ResumenCliente {
		public String correo = "";
		public String nombre = "";
		public String celular = "";
		public String vinculado = "";
		public boolean activo;
		public double puntosVigentes;
		public int acumulaciones;
		public double puntosAcumulados;
		public double valorAcumulado;
		public int redenciones;
		public double puntosRedimidos;
		public int redencionesReversadas;
		/** De cuantas acumulaciones se sabe quien atendio. Sin esto los porcentajes enganan. */
		public int acumulacionesConUsuario;
		public int tiendasDistintas;
		public boolean esEmpleado;
		public String empleadoNombre = "";
		public String empleadoCedula = "";
		public boolean empleadoActivo;
		public int autoRedenciones;
		public int diasConVarios;
		public int maxPedidosDia;
	}

	// ------------------------------------------------------------------
	// El join que resuelve el nombre de quien atendio, reutilizado en varias
	// consultas. Se deja en una constante para que las tres lo hagan igual.
	// ------------------------------------------------------------------
	private static final String JOIN_NOMBRE_USUARIO =
			" LEFT JOIN general.empleado emp ON emp.nombre = %s.usuario "
			+ " LEFT JOIN pizzaamericana.usuario usr ON usr.nombre = %s.usuario ";

	private static final String NOMBRE_USUARIO =
			" IFNULL(NULLIF(emp.nombre_largo,''), IFNULL(usr.nombre_largo,'')) ";

	/**
	 * Las redenciones de un rango de fechas, de la mas reciente a la mas vieja.
	 *
	 * El rango es por dia completo: fechaHasta entra entera, porque
	 * fecha_redencion es timestamp y comparar contra la fecha pelada dejaria
	 * por fuera todo lo del ultimo dia despues de medianoche.
	 */
	public static ArrayList<Redencion> listarRedenciones(final String fechaDesde, final String fechaHasta,
			final int idTienda, final String soloSenal) {

		final ArrayList<Redencion> lista = new ArrayList<Redencion>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT r.idredencion, r.fecha_redencion, r.correo, r.puntos_redimidos,")
			   .append("       r.idtienda, IFNULL(t.nombre,'') AS tienda, r.idpedidotienda,")
			   .append("       r.usuario, ").append(NOMBRE_USUARIO).append(" AS usuario_nombre,")
			   .append("       r.origen, r.estado,")
			   .append("       IF(empc.id IS NULL,'N','S') AS cliente_es_empleado,")
			   .append("       IFNULL(empc.nombre_largo,'') AS cliente_empleado_nombre,")
			   .append("       IF(empc.id IS NOT NULL AND empc.nombre = r.usuario,'S','N') AS auto_redencion")
			   .append("  FROM pizzaamericana.fidelizacion_redencion r")
			   .append("  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = r.idtienda")
			   .append(String.format(JOIN_NOMBRE_USUARIO, "r", "r"))
			   .append("  LEFT JOIN general.empleado empc")
			   .append("    ON LOWER(TRIM(empc.email)) = LOWER(TRIM(r.correo))")
			   .append(" WHERE r.fecha_redencion >= ?")
			   .append("   AND r.fecha_redencion < DATE_ADD(?, INTERVAL 1 DAY)");
			if (idTienda > 0) {
				sql.append(" AND r.idtienda = ").append(idTienda);
			}
			//Los filtros de senal se resuelven en SQL y no en Java para no traer
			//toda la semana cuando lo que se quiere son tres filas.
			if ("EMPLEADO".equals(soloSenal)) {
				sql.append(" AND empc.id IS NOT NULL");
			} else if ("AUTO".equals(soloSenal)) {
				sql.append(" AND empc.id IS NOT NULL AND empc.nombre = r.usuario");
			} else if ("REVERSADA".equals(soloSenal)) {
				sql.append(" AND r.estado = 'REVERSADA'");
			}
			sql.append(" ORDER BY r.fecha_redencion DESC");

			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setString(1, fechaDesde);
			ps.setString(2, fechaHasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Redencion r = new Redencion();
				r.idRedencion = rs.getInt("idredencion");
				r.fecha = texto(rs.getString("fecha_redencion"));
				r.correo = texto(rs.getString("correo"));
				r.puntos = rs.getDouble("puntos_redimidos");
				r.idTienda = rs.getInt("idtienda");
				r.tienda = texto(rs.getString("tienda"));
				r.idPedidoTienda = rs.getInt("idpedidotienda");
				r.usuario = texto(rs.getString("usuario"));
				r.usuarioNombre = texto(rs.getString("usuario_nombre"));
				r.origen = texto(rs.getString("origen"));
				r.estado = texto(rs.getString("estado"));
				r.clienteEsEmpleado = "S".equals(rs.getString("cliente_es_empleado"));
				r.clienteEmpleadoNombre = texto(rs.getString("cliente_empleado_nombre"));
				r.autoRedencion = "S".equals(rs.getString("auto_redencion"));
				lista.add(r);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.listarRedenciones: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Los totales de una persona.
	 *
	 * Va en una sola consulta con subconsultas y no en seis viajes: la pantalla
	 * la llama cada vez que se da clic en una fila, y seis idas a la base por
	 * clic se sienten.
	 */
	public static ResumenCliente resumenCliente(final String correo) {
		final ResumenCliente res = new ResumenCliente();
		res.correo = correo;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String sql =
					"SELECT"
					+ " (SELECT IFNULL(puntos_vigentes,0) FROM pizzaamericana.cliente_fidelizacion WHERE correo = ?) AS vigentes,"
					+ " (SELECT DATE(fecha_vinculacion) FROM pizzaamericana.cliente_fidelizacion WHERE correo = ?) AS vinculado,"
					+ " (SELECT activo FROM pizzaamericana.cliente_fidelizacion WHERE correo = ?) AS activo,"
					+ " (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_transaccion WHERE correo = ?) AS n_acum,"
					+ " (SELECT IFNULL(SUM(puntos),0) FROM pizzaamericana.fidelizacion_transaccion WHERE correo = ?) AS pts_acum,"
					+ " (SELECT IFNULL(SUM(valor_neto),0) FROM pizzaamericana.fidelizacion_transaccion WHERE correo = ?) AS valor_acum,"
					+ " (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_transaccion WHERE correo = ? AND usuario <> '') AS n_acum_usr,"
					+ " (SELECT COUNT(DISTINCT idtienda) FROM pizzaamericana.fidelizacion_transaccion WHERE correo = ?) AS tiendas,"
					+ " (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_redencion WHERE correo = ? AND estado = 'CONFIRMADA') AS n_red,"
					+ " (SELECT IFNULL(SUM(puntos_redimidos),0) FROM pizzaamericana.fidelizacion_redencion WHERE correo = ? AND estado = 'CONFIRMADA') AS pts_red,"
					+ " (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_redencion WHERE correo = ? AND estado = 'REVERSADA') AS n_rev,"
					+ " e.id AS idempleado, IFNULL(e.nombre_largo,'') AS emp_nombre,"
					+ " IFNULL(e.nombre,'') AS emp_cedula, IFNULL(e.activo,0) AS emp_activo,"
					+ " (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_redencion r2"
					+ "   WHERE r2.correo = ? AND r2.estado = 'CONFIRMADA' AND e.id IS NOT NULL"
					+ "     AND r2.usuario = e.nombre) AS auto_red"
					+ " FROM (SELECT 1) AS x"
					+ " LEFT JOIN general.empleado e ON LOWER(TRIM(e.email)) = LOWER(TRIM(?))";

			final PreparedStatement ps = cn.prepareStatement(sql);
			for (int i = 1; i <= 13; i++) {
				ps.setString(i, correo);
			}
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				res.puntosVigentes = rs.getDouble("vigentes");
				res.vinculado = texto(rs.getString("vinculado"));
				res.activo = "S".equals(rs.getString("activo"));
				res.acumulaciones = rs.getInt("n_acum");
				res.puntosAcumulados = rs.getDouble("pts_acum");
				res.valorAcumulado = rs.getDouble("valor_acum");
				res.acumulacionesConUsuario = rs.getInt("n_acum_usr");
				res.tiendasDistintas = rs.getInt("tiendas");
				res.redenciones = rs.getInt("n_red");
				res.puntosRedimidos = rs.getDouble("pts_red");
				res.redencionesReversadas = rs.getInt("n_rev");
				res.empleadoNombre = texto(rs.getString("emp_nombre"));
				res.empleadoCedula = texto(rs.getString("emp_cedula"));
				res.esEmpleado = rs.getInt("idempleado") > 0;
				res.empleadoActivo = rs.getInt("emp_activo") == 1;
				res.autoRedenciones = rs.getInt("auto_red");
			}
			rs.close();
			ps.close();
			nombreYCelular(cn, res);
			diasCargadosResumen(cn, res);
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.resumenCliente: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/**
	 * El nombre y el celular de la persona, de crm.persona_resumen.
	 *
	 * Va aparte y tolera el fallo: si el CRM no respondiera, el resto del
	 * detalle -que es lo que se esta auditando- tiene que salir igual.
	 */
	private static void nombreYCelular(final Connection cn, final ResumenCliente res) {
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT TRIM(CONCAT(IFNULL(nombre,''),' ',IFNULL(apellido,''))) AS nombre, IFNULL(celular,'') AS celular"
					+ " FROM crm.persona_resumen WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))"
					+ " ORDER BY pedidos DESC LIMIT 1");
			ps.setString(1, res.correo);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				res.nombre = texto(rs.getString("nombre"));
				res.celular = texto(rs.getString("celular"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AuditoriaFidelizacionDAO.nombreYCelular: " + e.toString());
		}
	}

	/** Cuantos dias junto mas de un pedido con puntos, y cual fue el peor. */
	private static void diasCargadosResumen(final Connection cn, final ResumenCliente res) {
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT COUNT(*) AS dias, IFNULL(MAX(y.n),0) AS max_dia"
					+ "  FROM (SELECT ft.fecha_transaccion, COUNT(*) AS n"
					+ "          FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "         WHERE ft.correo = ?"
					+ "         GROUP BY ft.fecha_transaccion"
					+ "        HAVING COUNT(*) > 1) y");
			ps.setString(1, res.correo);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				res.diasConVarios = rs.getInt("dias");
				res.maxPedidosDia = rs.getInt("max_dia");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AuditoriaFidelizacionDAO.diasCargadosResumen: " + e.toString());
		}
	}

	/**
	 * Los dias en que la persona junto mas de un pedido con puntos, con el
	 * detalle de en que tiendas y quien se los atendio.
	 *
	 * Una compra de almuerzo y otra de comida el mismo dia existe y es
	 * legitima; por eso esto no condena solo, se muestra para revisar. Lo que
	 * llama la atencion es el patron repetido, y sobre todo cuando los varios
	 * pedidos del dia los tomo la MISMA persona: por eso va la columna de
	 * quienes atendieron.
	 */
	public static ArrayList<DiaCargado> diasConVariosPedidos(final String correo) {
		final ArrayList<DiaCargado> lista = new ArrayList<DiaCargado>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String sql =
					"SELECT ft.fecha_transaccion, COUNT(*) AS pedidos,"
					+ "       SUM(ft.puntos) AS puntos, SUM(ft.valor_neto) AS valor,"
					+ "       GROUP_CONCAT(DISTINCT IFNULL(t.nombre,'?') ORDER BY t.nombre SEPARATOR ', ') AS tiendas,"
					+ "       GROUP_CONCAT(DISTINCT IFNULL(NULLIF(emp.nombre_largo,''),"
					+ "            IFNULL(usr.nombre_largo, NULLIF(ft.usuario,''))) SEPARATOR ', ') AS usuarios"
					+ "  FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = ft.idtienda"
					+ String.format(JOIN_NOMBRE_USUARIO, "ft", "ft")
					+ " WHERE ft.correo = ?"
					+ " GROUP BY ft.fecha_transaccion"
					+ "HAVING COUNT(*) > 1"
					+ " ORDER BY ft.fecha_transaccion DESC"
					+ " LIMIT 100";
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final DiaCargado d = new DiaCargado();
				d.fecha = texto(rs.getString("fecha_transaccion"));
				d.pedidos = rs.getInt("pedidos");
				d.puntos = rs.getDouble("puntos");
				d.valor = rs.getDouble("valor");
				d.tiendas = texto(rs.getString("tiendas"));
				d.usuarios = texto(rs.getString("usuarios"));
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.diasConVariosPedidos: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Las acumulaciones de una persona, de la mas reciente a la mas vieja. */
	public static ArrayList<Acumulacion> acumulacionesCliente(final String correo, final int limite) {
		final ArrayList<Acumulacion> lista = new ArrayList<Acumulacion>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String sql =
					"SELECT ft.fecha_transaccion, ft.idtienda, IFNULL(t.nombre,'') AS tienda,"
					+ "       ft.idpedidotienda, ft.valor_neto, ft.puntos, ft.puntos_redimidos,"
					+ "       ft.usuario, " + NOMBRE_USUARIO + " AS usuario_nombre, ft.vencidos"
					+ "  FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = ft.idtienda"
					+ String.format(JOIN_NOMBRE_USUARIO, "ft", "ft")
					+ " WHERE ft.correo = ?"
					+ " ORDER BY ft.fecha_transaccion DESC, ft.idpedidotienda DESC"
					+ " LIMIT " + (limite > 0 ? limite : 300);
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Acumulacion a = new Acumulacion();
				a.fecha = texto(rs.getString("fecha_transaccion"));
				a.idTienda = rs.getInt("idtienda");
				a.tienda = texto(rs.getString("tienda"));
				a.idPedidoTienda = rs.getInt("idpedidotienda");
				a.valorNeto = rs.getDouble("valor_neto");
				a.puntos = rs.getDouble("puntos");
				a.puntosRedimidos = rs.getDouble("puntos_redimidos");
				a.usuario = texto(rs.getString("usuario"));
				a.usuarioNombre = texto(rs.getString("usuario_nombre"));
				a.vencidos = "S".equals(rs.getString("vencidos"));
				lista.add(a);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.acumulacionesCliente: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Todas las redenciones de una persona, sin filtro de fecha. */
	public static ArrayList<Redencion> redencionesCliente(final String correo, final int limite) {
		final ArrayList<Redencion> lista = new ArrayList<Redencion>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String sql =
					"SELECT r.idredencion, r.fecha_redencion, r.correo, r.puntos_redimidos,"
					+ "       r.idtienda, IFNULL(t.nombre,'') AS tienda, r.idpedidotienda,"
					+ "       r.usuario, " + NOMBRE_USUARIO + " AS usuario_nombre, r.origen, r.estado,"
					+ "       IF(empc.id IS NULL,'N','S') AS cliente_es_empleado,"
					+ "       IFNULL(empc.nombre_largo,'') AS cliente_empleado_nombre,"
					+ "       IF(empc.id IS NOT NULL AND empc.nombre = r.usuario,'S','N') AS auto_redencion"
					+ "  FROM pizzaamericana.fidelizacion_redencion r"
					+ "  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = r.idtienda"
					+ String.format(JOIN_NOMBRE_USUARIO, "r", "r")
					+ "  LEFT JOIN general.empleado empc ON LOWER(TRIM(empc.email)) = LOWER(TRIM(r.correo))"
					+ " WHERE r.correo = ?"
					+ " ORDER BY r.fecha_redencion DESC"
					+ " LIMIT " + (limite > 0 ? limite : 200);
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Redencion r = new Redencion();
				r.idRedencion = rs.getInt("idredencion");
				r.fecha = texto(rs.getString("fecha_redencion"));
				r.correo = texto(rs.getString("correo"));
				r.puntos = rs.getDouble("puntos_redimidos");
				r.idTienda = rs.getInt("idtienda");
				r.tienda = texto(rs.getString("tienda"));
				r.idPedidoTienda = rs.getInt("idpedidotienda");
				r.usuario = texto(rs.getString("usuario"));
				r.usuarioNombre = texto(rs.getString("usuario_nombre"));
				r.origen = texto(rs.getString("origen"));
				r.estado = texto(rs.getString("estado"));
				r.clienteEsEmpleado = "S".equals(rs.getString("cliente_es_empleado"));
				r.clienteEmpleadoNombre = texto(rs.getString("cliente_empleado_nombre"));
				r.autoRedencion = "S".equals(rs.getString("auto_redencion"));
				lista.add(r);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.redencionesCliente: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Quien le ha tomado los pedidos a esta persona y cuantas veces.
	 *
	 * ESTA ES LA CONSULTA QUE IMPORTA. A un cliente frecuente legitimo lo
	 * atiende quien este en la caja ese dia, asi que su atencion se reparte. La
	 * firma del fraude no es comprar mucho -eso tambien lo hace un cliente
	 * fiel-, es que una sola persona aparezca en casi todos los pedidos.
	 *
	 * Solo cuenta las acumulaciones que tienen usuario: las de antes de
	 * septiembre de 2026 no lo traen y meterlas en el denominador diluiria el
	 * porcentaje hasta volverlo inofensivo.
	 */
	public static ArrayList<Atencion> quienLoAtiende(final String correo) {
		final ArrayList<Atencion> lista = new ArrayList<Atencion>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			//Los canales se traen tambien -saber que alguien pide todo por el BOT
			//es util- pero marcados, para que el controlador los deje por fuera
			//del porcentaje de concentracion.
			final String sql =
					"SELECT ft.usuario, " + NOMBRE_USUARIO + " AS usuario_nombre,"
					+ "       COUNT(*) AS pedidos, COUNT(DISTINCT ft.idtienda) AS tiendas,"
					+ "       IF(empc.id IS NOT NULL AND empc.nombre = ft.usuario,'S','N') AS es_el_mismo,"
					+ "       IF(UPPER(ft.usuario) IN " + NO_PERSONA + ",'S','N') AS es_canal"
					+ "  FROM pizzaamericana.fidelizacion_transaccion ft"
					+ String.format(JOIN_NOMBRE_USUARIO, "ft", "ft")
					+ "  LEFT JOIN general.empleado empc ON LOWER(TRIM(empc.email)) = LOWER(TRIM(ft.correo))"
					+ " WHERE ft.correo = ? AND ft.usuario <> ''"
					+ " GROUP BY ft.usuario, usuario_nombre, es_el_mismo, es_canal"
					+ " ORDER BY pedidos DESC";
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Atencion a = new Atencion();
				a.usuario = texto(rs.getString("usuario"));
				a.nombre = texto(rs.getString("usuario_nombre"));
				a.pedidos = rs.getInt("pedidos");
				a.tiendas = rs.getInt("tiendas");
				a.esElMismoCliente = "S".equals(rs.getString("es_el_mismo"));
				a.esCanal = "S".equals(rs.getString("es_canal"));
				lista.add(a);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.quienLoAtiende: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Quien mas acumula puntos en un rango, con la senal de concentracion ya
	 * calculada por la base.
	 *
	 * Es la otra mitad de la auditoria: la lista de redenciones muestra quien
	 * SACA producto, y esta muestra quien esta ACUMULANDO, que es donde empieza
	 * el problema. Una cuenta manipulada se ve aqui antes de que alguien redima
	 * nada.
	 *
	 * Las dos columnas que importan son pedidos_con_persona y top_pedidos: el
	 * porcentaje se calcula sobre los pedidos atendidos por una PERSONA, nunca
	 * sobre el total. Ver NO_PERSONA arriba para por que.
	 *
	 * @param minimoPedidos no vale la pena mirar a quien compro dos veces: con
	 *                      pocos pedidos cualquier porcentaje es ruido.
	 */
	public static ArrayList<Acumulador> topAcumuladores(final String fechaDesde, final String fechaHasta,
			final int idTienda, final int minimoPedidos, final int limite) {

		final ArrayList<Acumulador> lista = new ArrayList<Acumulador>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String filtroTienda = idTienda > 0 ? " AND ft.idtienda = " + idTienda : "";
			final String sql =
					"WITH base AS ("
					+ "  SELECT ft.correo, COUNT(*) AS pedidos, SUM(ft.puntos) AS puntos,"
					+ "         SUM(ft.valor_neto) AS valor, COUNT(DISTINCT ft.idtienda) AS tiendas,"
					+ "         SUM(IF(" + soloPersonas("ft") + ",1,0)) AS con_persona,"
					+ "         COUNT(DISTINCT IF(" + soloPersonas("ft") + ", ft.usuario, NULL)) AS cajeros"
					+ "    FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "   WHERE ft.fecha_transaccion BETWEEN ? AND ?" + filtroTienda
					+ "   GROUP BY ft.correo"
					+ "  HAVING COUNT(*) >= " + (minimoPedidos > 0 ? minimoPedidos : 1)
					+ "), top_cajero AS ("
					+ "  SELECT x.correo, x.usuario, x.n,"
					+ "         ROW_NUMBER() OVER (PARTITION BY x.correo ORDER BY x.n DESC) AS rn"
					+ "    FROM (SELECT ft.correo, ft.usuario, COUNT(*) AS n"
					+ "            FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "           WHERE ft.fecha_transaccion BETWEEN ? AND ?" + filtroTienda
					+ "             AND " + soloPersonas("ft")
					+ "           GROUP BY ft.correo, ft.usuario) x"
					+ "), mismo_dia AS ("
					//Dias en que la persona junto MAS DE UN pedido con puntos. Medido el
					//2026-11-04 sobre 90 dias: pasa en el 1,7% de los dias-persona (228 de
					//13.629), asi que discrimina de verdad. Y de las 12 personas con mas
					//dias asi, 9 estaban en la nomina.
					+ "  SELECT y.correo, COUNT(*) AS dias, MAX(y.n) AS max_dia"
					+ "    FROM (SELECT ft.correo, ft.fecha_transaccion, COUNT(*) AS n"
					+ "            FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "           WHERE ft.fecha_transaccion BETWEEN ? AND ?" + filtroTienda
					+ "           GROUP BY ft.correo, ft.fecha_transaccion"
					+ "          HAVING COUNT(*) > 1) y"
					+ "   GROUP BY y.correo"
					+ ")"
					+ "SELECT b.correo, b.pedidos, b.puntos, b.valor, b.tiendas, b.cajeros, b.con_persona,"
					+ "       IFNULL(md.dias,0) AS dias_varios, IFNULL(md.max_dia,0) AS max_dia,"
					+ "       IFNULL(tc.usuario,'') AS top_usuario, IFNULL(tc.n,0) AS top_pedidos,"
					+ "       IFNULL(NULLIF(emp.nombre_largo,''), IFNULL(usr.nombre_largo,'')) AS top_nombre,"
					+ "       IF(empc.id IS NULL,'N','S') AS es_empleado,"
					+ "       IFNULL(empc.nombre_largo,'') AS empleado_nombre,"
					+ "       (SELECT COUNT(*) FROM pizzaamericana.fidelizacion_redencion r"
					+ "         WHERE r.correo = b.correo AND r.estado = 'CONFIRMADA'"
					+ "           AND empc.id IS NOT NULL AND r.usuario = empc.nombre) AS auto_red,"
					+ "       (SELECT TRIM(CONCAT(IFNULL(pr.nombre,''),' ',IFNULL(pr.apellido,'')))"
					+ "          FROM crm.persona_resumen pr"
					+ "         WHERE LOWER(TRIM(pr.email)) = LOWER(TRIM(b.correo))"
					+ "         ORDER BY pr.pedidos DESC LIMIT 1) AS nombre"
					+ "  FROM base b"
					+ "  LEFT JOIN mismo_dia md ON md.correo = b.correo"
					+ "  LEFT JOIN top_cajero tc ON tc.correo = b.correo AND tc.rn = 1"
					+ "  LEFT JOIN general.empleado emp ON emp.nombre = tc.usuario"
					+ "  LEFT JOIN pizzaamericana.usuario usr ON usr.nombre = tc.usuario"
					+ "  LEFT JOIN general.empleado empc ON LOWER(TRIM(empc.email)) = LOWER(TRIM(b.correo))"
					+ " ORDER BY b.puntos DESC"
					+ " LIMIT " + (limite > 0 ? limite : 200);

			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setString(1, fechaDesde);
			ps.setString(2, fechaHasta);
			ps.setString(3, fechaDesde);
			ps.setString(4, fechaHasta);
			ps.setString(5, fechaDesde);
			ps.setString(6, fechaHasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Acumulador a = new Acumulador();
				a.diasConVarios = rs.getInt("dias_varios");
				a.maxPedidosDia = rs.getInt("max_dia");
				a.correo = texto(rs.getString("correo"));
				a.nombre = texto(rs.getString("nombre"));
				a.pedidos = rs.getInt("pedidos");
				a.puntos = rs.getDouble("puntos");
				a.valor = rs.getDouble("valor");
				a.tiendas = rs.getInt("tiendas");
				a.cajerosDistintos = rs.getInt("cajeros");
				a.pedidosConPersona = rs.getInt("con_persona");
				a.topUsuario = texto(rs.getString("top_usuario"));
				a.topNombre = texto(rs.getString("top_nombre"));
				a.topPedidos = rs.getInt("top_pedidos");
				a.esEmpleado = "S".equals(rs.getString("es_empleado"));
				a.empleadoNombre = texto(rs.getString("empleado_nombre"));
				a.autoRedenciones = rs.getInt("auto_red");
				lista.add(a);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("AuditoriaFidelizacionDAO.topAcumuladores: " + e.toString(), e);
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
			Logger.getLogger("log_file").error("AuditoriaFidelizacionDAO: no cerro la conexion, " + e.toString());
		}
	}
}

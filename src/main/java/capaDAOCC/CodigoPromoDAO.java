package capaDAOCC;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Reserva, confirmacion, liberacion y emision de codigos promocionales.
 *
 * POR QUE EXISTE
 *
 * Antes la tienda validaba el codigo, miraba el descuento configurado y luego le decia al central
 * "marquelo como usado, con este saldo". El central no comprobaba nada: ni que siguiera sin usar,
 * ni que el saldo o el descuento fueran los que le correspondian. Dos tiendas podian redimir el
 * mismo codigo a la vez, y una tienda podia mandar el saldo que quisiera. Aqui la logica esta en el
 * servidor y es ATOMICA:
 *
 *   RESERVAR   valida todas las reglas de la oferta, calcula el descuento y aparta el codigo por
 *              30 minutos (un UPDATE condicionado: gana una sola tienda).
 *   CONFIRMAR  cuando el pedido se finaliza de verdad: consume el codigo y deja el log con pedido,
 *              tienda, usuario y valor.
 *   LIBERAR    si el pedido se cancela o se quita el codigo.
 *
 * Todas las consultas usan parametros: los servicios viejos armaban el SQL concatenando lo que
 * llegaba por la URL, sin autenticacion.
 *
 * Los metodos publicos que reciben un Connection existen para poder probar la logica contra una
 * base de prueba; los otros abren la conexion normal del central.
 */
public class CodigoPromoDAO {

	/** Cuanto tiempo se aparta un codigo mientras se termina el pedido. */
	public static final int MINUTOS_RESERVA = 30;

	private static final String ALFABETO = "23456789BCDFGHJKLMNPQRSTVWXYZ";
	private static final int LARGO_CODIGO = 8;
	private static final SecureRandom AZAR = new SecureRandom();

	/** Lo que se sabe de un codigo, personal (oferta_cliente) o abierto (oferta.codigo_general). */
	public static class Codigo {
		public boolean abierto;
		public int idOfertaCliente;
		public int idOferta;
		public int idCliente;
		public String codigo = "";
		public String nombreOferta = "";
		public String utilizada = "N";
		public String anulada = "N";
		public double saldo;
		/** yyyy-MM-dd o vacio */
		public String fechaCaducidad = "";
		public String reservaToken = "";
		public boolean reservadaVigente;
		public String habilitado = "S";
		public String redParcial = "N";
		public double descuentoPorcentaje;
		public double descuentoValor;
		public String controlaHora = "N";
		public int horaInicio;
		public int horaFin;
		public String fechaDesde = "";
		public String fechaHasta = "";
		public double montoMinimo;
		public double tope;
		public String tiendas = "";
		public String aplicaA = "T";
		public int maxUsosCliente;
	}

	/** Resultado de reservar. */
	public static class Reserva {
		/** OK, NOK (no existe), VEN (vencido o usado), NOAPLICA, EN_USO, DESHAB */
		public String estado = "NOK";
		public String mensaje = "";
		public String token = "";
		public Codigo codigo;
		public double descuento;
		public double saldoPosterior;
		public boolean abierta;
	}

	/** Resultado de confirmar. */
	public static class Confirmacion {
		/** OK, YA (ya estaba confirmado), CONFLICTO, NOEXISTE */
		public String estado = "NOEXISTE";
		public String mensaje = "";
		public double saldoPosterior;
	}

	/** Resultado de emitir un codigo a un cliente. */
	public static class Emision {
		public int idOfertaCliente;
		public String codigo = "";
		public String fechaCaducidad = "";
		public boolean yaExistia;
		public String error = "";
		public double saldo;
	}

	// =======================================================================
	// Busqueda
	// =======================================================================

	private static final String CAMPOS_OFERTA = " o.idoferta, o.nombre_oferta, o.habilitado, o.red_parcial,"
			+ " o.descuento_fijo_porcentaje, o.descuento_fijo_valor, o.controla_hora, o.hora_inicio, o.hora_fin,"
			+ " o.fecha_desde, o.fecha_hasta, o.monto_minimo, o.tope_descuento, o.tiendas, o.aplica_a,"
			+ " o.max_usos_cliente ";

	/** El codigo tal como esta hoy, o null si no existe. Distingue mayusculas solo en lo que la base distinga. */
	public static Codigo buscar(final Connection cn, final String codigo) throws SQLException {
		if (codigo == null || codigo.trim().length() == 0) {
			return null;
		}
		final String limpio = codigo.trim();
		try (PreparedStatement ps = cn.prepareStatement("select oc.idofertacliente, oc.idcliente, oc.codigo_promocion,"
				+ " oc.utilizada, oc.anulada, oc.saldo, oc.fecha_caducidad, oc.reserva_token,"
				+ " (oc.reservada_hasta is not null and oc.reservada_hasta > now()) as reservada_vigente,"
				+ CAMPOS_OFERTA + " from oferta_cliente oc join oferta o on o.idoferta = oc.idoferta"
				+ " where oc.codigo_promocion = ?")) {
			ps.setString(1, limpio);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final Codigo c = new Codigo();
					c.abierto = false;
					c.idOfertaCliente = rs.getInt("idofertacliente");
					c.idCliente = rs.getInt("idcliente");
					c.codigo = rs.getString("codigo_promocion");
					c.utilizada = texto(rs.getString("utilizada"), "N");
					c.anulada = texto(rs.getString("anulada"), "N");
					c.saldo = rs.getDouble("saldo");
					c.fechaCaducidad = texto(rs.getString("fecha_caducidad"), "");
					c.reservaToken = texto(rs.getString("reserva_token"), "");
					c.reservadaVigente = rs.getInt("reservada_vigente") == 1;
					leerOferta(rs, c);
					return c;
				}
			}
		}
		try (PreparedStatement ps = cn.prepareStatement(
				"select" + CAMPOS_OFERTA + "from oferta o where o.codigo_general = ? and o.tipo_oferta = 'A'")) {
			ps.setString(1, limpio);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final Codigo c = new Codigo();
					c.abierto = true;
					c.codigo = limpio;
					leerOferta(rs, c);
					return c;
				}
			}
		}
		return null;
	}

	private static void leerOferta(final ResultSet rs, final Codigo c) throws SQLException {
		c.idOferta = rs.getInt("idoferta");
		c.nombreOferta = texto(rs.getString("nombre_oferta"), "");
		c.habilitado = texto(rs.getString("habilitado"), "S");
		c.redParcial = texto(rs.getString("red_parcial"), "N");
		c.descuentoPorcentaje = rs.getDouble("descuento_fijo_porcentaje");
		c.descuentoValor = rs.getDouble("descuento_fijo_valor");
		c.controlaHora = texto(rs.getString("controla_hora"), "N");
		c.horaInicio = entero(rs.getString("hora_inicio"));
		c.horaFin = entero(rs.getString("hora_fin"));
		c.fechaDesde = texto(rs.getString("fecha_desde"), "");
		c.fechaHasta = texto(rs.getString("fecha_hasta"), "");
		c.montoMinimo = rs.getDouble("monto_minimo");
		c.tope = rs.getDouble("tope_descuento");
		c.tiendas = texto(rs.getString("tiendas"), "");
		c.aplicaA = texto(rs.getString("aplica_a"), "T");
		c.maxUsosCliente = rs.getInt("max_usos_cliente");
	}

	// =======================================================================
	// Reglas y descuento
	// =======================================================================

	/**
	 * Las reglas de la oferta contra este pedido. Devuelve null si se puede usar, o el mensaje de por
	 * que no. Trabaja con la hora y la fecha del SERVIDOR DE BASE DE DATOS, no la del equipo de la
	 * tienda.
	 */
	static String motivoDeRechazo(final Connection cn, final Codigo c, final int idTienda, final double total,
			final boolean esDomicilio) throws SQLException {
		if (!"S".equals(c.habilitado)) {
			return "La oferta esta deshabilitada.";
		}
		try (PreparedStatement ps = cn.prepareStatement("select curdate() as hoy, hour(now()) as hora")) {
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				final String hoy = rs.getString("hoy");
				final int hora = rs.getInt("hora");
				//La caducidad y la vigencia son INCLUSIVAS: el ultimo dia sirve todo el dia.
				if (c.fechaCaducidad.length() > 0 && c.fechaCaducidad.compareTo(hoy) < 0) {
					return "El codigo vencio el " + c.fechaCaducidad + ".";
				}
				//Las fechas desde/hasta de la OFERTA solo mandan en los codigos ABIERTOS. En un codigo personal manda
				//la caducidad del propio codigo: esas fechas dicen hasta cuando se puede ASIGNAR la oferta, y hay ofertas
				//viejas (2021) que se siguen asignando hoy con codigos que vencen dentro de 15 dias. El sistema anterior
				//tampoco las miraba en codigos personales.
				if (c.abierto) {
					if (c.fechaDesde.length() > 0 && c.fechaDesde.compareTo(hoy) > 0) {
						return "La oferta empieza el " + c.fechaDesde + ".";
					}
					if (c.fechaHasta.length() > 0 && c.fechaHasta.compareTo(hoy) < 0) {
						return "La oferta termino el " + c.fechaHasta + ".";
					}
				}
				if ("S".equals(c.controlaHora) && !(hora >= c.horaInicio && hora < c.horaFin)) {
					return "La oferta solo sirve entre las " + c.horaInicio + ":00 y las " + c.horaFin + ":00.";
				}
			}
		}
		if (c.montoMinimo > 0 && total < c.montoMinimo) {
			return "La oferta exige un pedido de minimo $" + formato(c.montoMinimo) + ".";
		}
		if ("M".equals(c.aplicaA) && esDomicilio) {
			return "La oferta solo aplica para pedidos en mostrador.";
		}
		if ("D".equals(c.aplicaA) && !esDomicilio) {
			return "La oferta solo aplica para pedidos a domicilio.";
		}
		if (c.tiendas.trim().length() > 0 && !contiene(c.tiendas, idTienda)) {
			return "La oferta no aplica en esta tienda.";
		}
		if (!c.abierto && c.maxUsosCliente > 0) {
			try (PreparedStatement ps = cn.prepareStatement("select count(*) from oferta_cliente"
					+ " where idoferta = ? and idcliente = ? and utilizada = 'S' and idofertacliente <> ?")) {
				ps.setInt(1, c.idOferta);
				ps.setInt(2, c.idCliente);
				ps.setInt(3, c.idOfertaCliente);
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					if (rs.getInt(1) >= c.maxUsosCliente) {
						return "Este cliente ya uso la oferta el maximo de veces permitido.";
					}
				}
			}
		}
		return null;
	}

	/** El descuento en pesos que le corresponde a este pedido, ya con tope y sin pasar del total. */
	static double calcularDescuento(final Codigo c, final double total) {
		double d;
		if (!c.abierto && "S".equals(c.redParcial) && c.saldo > 0) {
			d = Math.min(c.saldo, total);
		} else if (c.descuentoPorcentaje > 0) {
			d = total * c.descuentoPorcentaje / 100.0;
		} else {
			d = c.descuentoValor;
		}
		if (c.tope > 0) {
			d = Math.min(d, c.tope);
		}
		d = Math.min(d, total);
		return Math.round(d * 100.0) / 100.0;
	}

	// =======================================================================
	// Reservar
	// =======================================================================

	public static Reserva reservar(final String codigo, final int idTienda, final String usuario, final double total,
			final boolean esDomicilio, final String tokenPrevio) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			return reservar(cn, codigo, idTienda, usuario, total, esDomicilio, tokenPrevio);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.reservar: " + e.toString());
			final Reserva r = new Reserva();
			r.estado = "ERROR";
			r.mensaje = "No se pudo validar el codigo, intente de nuevo.";
			return r;
		} finally {
			cerrar(cn);
		}
	}

	public static Reserva reservar(final Connection cn, final String codigo, final int idTienda, final String usuario,
			final double total, final boolean esDomicilio, final String tokenPrevio) throws SQLException {
		final Reserva r = new Reserva();
		final Codigo c = buscar(cn, codigo);
		if (c == null) {
			r.estado = "NOK";
			r.mensaje = "El codigo promocional no existe.";
			return r;
		}
		r.codigo = c;
		if (!c.abierto) {
			if ("S".equals(c.anulada)) {
				r.estado = "VEN";
				r.mensaje = "El codigo fue anulado.";
				return r;
			}
			if ("S".equals(c.utilizada)) {
				r.estado = "VEN";
				r.mensaje = "El codigo ya fue utilizado.";
				return r;
			}
		}
		final String rechazo = motivoDeRechazo(cn, c, idTienda, total, esDomicilio);
		if (rechazo != null) {
			//Vencido y deshabilitado se distinguen del resto: la caja necesita saber si el problema es el
			//codigo o es este pedido.
			r.estado = rechazo.startsWith("El codigo vencio") ? "VEN" : ("La oferta esta deshabilitada.".equals(rechazo) ? "DESHAB" : "NOAPLICA");
			r.mensaje = rechazo;
			return r;
		}
		final double descuento = calcularDescuento(c, total);
		if (descuento <= 0) {
			r.estado = "NOAPLICA";
			r.mensaje = "La oferta no tiene un descuento configurado para este pedido.";
			return r;
		}
		r.descuento = descuento;
		r.saldoPosterior = "S".equals(c.redParcial) && !c.abierto ? Math.max(0, c.saldo - descuento) : 0;
		if (c.abierto) {
			//Los codigos abiertos sirven para todos: no se aparta nada.
			r.abierta = true;
			r.token = "A" + c.idOferta + "." + nuevoToken();
			r.estado = "OK";
			return r;
		}
		final String token = tokenPrevio != null && tokenPrevio.length() > 0 && tokenPrevio.equals(c.reservaToken)
				? tokenPrevio : nuevoToken();
		//El UPDATE condicionado es lo que hace que gane una sola tienda: si otra ya lo aparto, no toca ninguna fila.
		try (PreparedStatement ps = cn.prepareStatement("update oferta_cliente set reservada_hasta = date_add(now(),"
				+ " interval " + MINUTOS_RESERVA + " minute), reserva_token = ?, reserva_tienda = ?,"
				+ " reserva_usuario = ?, reserva_descuento = ?"
				+ " where idofertacliente = ? and utilizada = 'N' and anulada = 'N'"
				+ " and (reservada_hasta is null or reservada_hasta <= now() or reserva_token = ?)")) {
			ps.setString(1, token);
			ps.setInt(2, idTienda);
			ps.setString(3, recortar(usuario, 50));
			ps.setDouble(4, descuento);
			ps.setInt(5, c.idOfertaCliente);
			ps.setString(6, tokenPrevio == null ? "" : tokenPrevio);
			if (ps.executeUpdate() != 1) {
				r.estado = "EN_USO";
				r.mensaje = "El codigo esta siendo usado en otro pedido. Intente de nuevo en unos minutos.";
				r.descuento = 0;
				return r;
			}
		}
		r.token = token;
		r.estado = "OK";
		return r;
	}

	// =======================================================================
	// Confirmar
	// =======================================================================

	public static Confirmacion confirmar(final String token, final int idPedido, final int idTienda,
			final String usuario, final double descuentoAplicado, final String origen) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			return confirmar(cn, token, idPedido, idTienda, usuario, descuentoAplicado, origen);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.confirmar: " + e.toString());
			final Confirmacion c = new Confirmacion();
			c.estado = "ERROR";
			c.mensaje = e.getMessage();
			return c;
		} finally {
			cerrar(cn);
		}
	}

	public static Confirmacion confirmar(final Connection cn, final String token, final int idPedido, final int idTienda,
			final String usuario, final double descuentoAplicado, final String origen) throws SQLException {
		final Confirmacion res = new Confirmacion();
		if (token == null || token.trim().length() == 0) {
			res.mensaje = "Falta el token de la reserva.";
			return res;
		}
		if (token.startsWith("A")) {
			//Codigo abierto: no hay nada que consumir, solo queda el registro.
			final int idOferta = entero(token.substring(1, Math.max(1, token.indexOf('.'))));
			if (yaConfirmado(cn, token, idPedido)) {
				res.estado = "YA";
				return res;
			}
			registrarLog(cn, 0, idOferta, idPedido, idTienda, usuario, descuentoAplicado, 0, origen, "OK", token);
			res.estado = "OK";
			return res;
		}
		cn.setAutoCommit(false);
		try {
			int idOfertaCliente = 0;
			int idOferta = 0;
			String utilizada = "N";
			double saldo = 0;
			double autorizado = 0;
			String redParcial = "N";
			try (PreparedStatement ps = cn.prepareStatement("select oc.idofertacliente, oc.idoferta, oc.utilizada, oc.saldo,"
					+ " oc.reserva_descuento, o.red_parcial from oferta_cliente oc join oferta o on o.idoferta = oc.idoferta"
					+ " where oc.reserva_token = ? for update")) {
				ps.setString(1, token);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						idOfertaCliente = rs.getInt(1);
						idOferta = rs.getInt(2);
						utilizada = texto(rs.getString(3), "N");
						saldo = rs.getDouble(4);
						autorizado = rs.getDouble(5);
						redParcial = texto(rs.getString(6), "N");
					}
				}
			}
			if (idOfertaCliente == 0) {
				//O ya se confirmo (y la reserva se limpio) o se libero: se mira el log antes de dar por perdido.
				cn.rollback();
				cn.setAutoCommit(true);
				if (yaConfirmado(cn, token, idPedido)) {
					res.estado = "YA";
					return res;
				}
				res.estado = "NOEXISTE";
				res.mensaje = "La reserva no existe o ya fue liberada.";
				return res;
			}
			if ("S".equals(utilizada)) {
				//Se consumio por otra via mientras el pedido se terminaba: el pedido ya salio, asi que no se puede
				//deshacer; queda como CONFLICTO para que alguien lo revise.
				registrarLog(cn, idOfertaCliente, idOferta, idPedido, idTienda, usuario, descuentoAplicado, saldo, origen, "CONFLICTO", token);
				cn.commit();
				cn.setAutoCommit(true);
				res.estado = "CONFLICTO";
				res.mensaje = "El codigo ya estaba usado cuando se confirmo el pedido.";
				return res;
			}
			//Un descuento mayor al autorizado se deja pasar -el pedido ya se cobro- pero queda marcado.
			final boolean exceso = descuentoAplicado > autorizado + 1.0;
			double nuevoSaldo = 0;
			boolean agotado = true;
			if ("S".equals(redParcial)) {
				nuevoSaldo = Math.max(0, saldo - descuentoAplicado);
				agotado = nuevoSaldo <= 0;
			}
			try (PreparedStatement ps = cn.prepareStatement("update oferta_cliente set saldo = ?, utilizada = ?,"
					+ " uso_oferta = now(), usuario_uso = ?, reservada_hasta = null, reserva_token = null"
					+ " where idofertacliente = ? and utilizada = 'N'")) {
				ps.setDouble(1, agotado ? 0 : nuevoSaldo);
				ps.setString(2, agotado ? "S" : "N");
				ps.setString(3, recortar(usuario, 20));
				ps.setInt(4, idOfertaCliente);
				ps.executeUpdate();
			}
			registrarLog(cn, idOfertaCliente, idOferta, idPedido, idTienda, usuario, descuentoAplicado, agotado ? 0 : nuevoSaldo,
					origen, exceso ? "EXCESO" : "OK", token);
			cn.commit();
			cn.setAutoCommit(true);
			res.estado = "OK";
			res.saldoPosterior = agotado ? 0 : nuevoSaldo;
			if (exceso) {
				res.mensaje = "El descuento aplicado fue mayor al autorizado.";
			}
			return res;
		} catch (final SQLException e) {
			try {
				cn.rollback();
				cn.setAutoCommit(true);
			} catch (final SQLException e1) {
			}
			throw e;
		}
	}

	private static boolean yaConfirmado(final Connection cn, final String token, final int idPedido) throws SQLException {
		if (idPedido <= 0) {
			return false;
		}
		try (PreparedStatement ps = cn.prepareStatement(
				"select count(*) from log_redencion_codigo where idpedido = ? and estado in ('OK','EXCESO') and token_reserva = ?")) {
			ps.setInt(1, idPedido);
			ps.setString(2, token);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt(1) > 0;
			}
		} catch (final SQLException sinColumna) {
			return false;
		}
	}

	/** El log de una redencion. Nunca lanza por la columna opcional token_reserva: si no esta, se registra sin ella. */
	static void registrarLog(final Connection cn, final int idOfertaCliente, final int idOferta, final int idPedido,
			final int idTienda, final String usuario, final double descuento, final double saldo, final String origen,
			final String estado, final String token) throws SQLException {
		try (PreparedStatement ps = cn.prepareStatement("insert into log_redencion_codigo (idofertacliente, idoferta,"
				+ " descuento, saldo, usuario_uso, fecha_real, idpedido, idtienda, origen, estado, token_reserva)"
				+ " values (?, ?, ?, ?, ?, now(), ?, ?, ?, ?, ?)")) {
			ps.setInt(1, idOfertaCliente);
			ps.setInt(2, idOferta);
			ps.setDouble(3, descuento);
			ps.setDouble(4, saldo);
			ps.setString(5, recortar(usuario, 20));
			ps.setInt(6, idPedido);
			ps.setInt(7, idTienda);
			ps.setString(8, recortar(origen, 10));
			ps.setString(9, estado);
			ps.setString(10, recortar(token, 40));
			ps.executeUpdate();
		}
	}

	// =======================================================================
	// Liberar
	// =======================================================================

	public static boolean liberar(final String token) {
		if (token == null || token.length() == 0 || token.startsWith("A")) {
			return true;
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			return liberar(cn, token);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.liberar: " + e.toString());
			return false;
		} finally {
			cerrar(cn);
		}
	}

	public static boolean liberar(final Connection cn, final String token) throws SQLException {
		try (PreparedStatement ps = cn.prepareStatement("update oferta_cliente set reservada_hasta = null,"
				+ " reserva_token = null, reserva_tienda = null, reserva_usuario = null, reserva_descuento = null"
				+ " where reserva_token = ? and utilizada = 'N'")) {
			ps.setString(1, token);
			return ps.executeUpdate() >= 0;
		}
	}

	// =======================================================================
	// Emitir (envio de publicidad con oferta)
	// =======================================================================

	/**
	 * Le crea a un cliente su propio codigo de la oferta, ligado al envio. Es idempotente por (envio, cliente):
	 * si el envio se reanuda, nadie recibe dos codigos.
	 */
	public static Emision emitir(final Connection cn, final int idOferta, final int idCliente, final long idEnvio,
			final String usuario) throws SQLException {
		final Emision e = new Emision();
		try (PreparedStatement ps = cn.prepareStatement("select idofertacliente, codigo_promocion, fecha_caducidad, saldo"
				+ " from oferta_cliente where idenvio = ? and idcliente = ? and idoferta = ?")) {
			ps.setLong(1, idEnvio);
			ps.setInt(2, idCliente);
			ps.setInt(3, idOferta);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					e.idOfertaCliente = rs.getInt(1);
					e.codigo = rs.getString(2);
					e.fechaCaducidad = texto(rs.getString(3), "");
					e.saldo = rs.getDouble(4);
					e.yaExistia = true;
					return e;
				}
			}
		}
		String tipoOferta = "";
		String habilitado = "";
		String codigoPromocional = "";
		String redParcial = "";
		double valor = 0;
		int dias = 0;
		int maxEmision = 0;
		try (PreparedStatement ps = cn.prepareStatement("select tipo_oferta, habilitado, codigo_promocional, red_parcial,"
				+ " descuento_fijo_valor, dias_caducidad, max_emision from oferta where idoferta = ?")) {
			ps.setInt(1, idOferta);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					e.error = "La oferta no existe.";
					return e;
				}
				tipoOferta = texto(rs.getString(1), "C");
				habilitado = texto(rs.getString(2), "S");
				codigoPromocional = texto(rs.getString(3), "N");
				redParcial = texto(rs.getString(4), "N");
				valor = rs.getDouble(5);
				dias = rs.getInt(6);
				maxEmision = rs.getInt(7);
			}
		}
		if (!"S".equals(habilitado)) {
			e.error = "La oferta esta deshabilitada.";
			return e;
		}
		if (!"C".equals(tipoOferta) || !"S".equals(codigoPromocional)) {
			e.error = "Solo las ofertas personales con codigo promocional se pueden enviar por campana.";
			return e;
		}
		if (dias <= 0) {
			e.error = "La oferta no tiene dias de caducidad: un codigo enviado a miles de clientes tiene que vencer.";
			return e;
		}
		if (maxEmision > 0) {
			try (PreparedStatement ps = cn.prepareStatement("select count(*) from oferta_cliente where idoferta = ?")) {
				ps.setInt(1, idOferta);
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					if (rs.getInt(1) >= maxEmision) {
						e.error = "La oferta llego a su tope de emision (" + maxEmision + " codigos).";
						return e;
					}
				}
			}
		}
		final double saldo = "S".equals(redParcial) && valor > 0 ? valor : 0;
		final String codigo = generarCodigo(cn);
		try (PreparedStatement ps = cn.prepareStatement("insert into oferta_cliente (idoferta, idcliente, observacion, PQRS,"
				+ " codigo_promocion, usuario_ingreso, fecha_caducidad, saldo, cliente, idenvio)"
				+ " values (?, ?, ?, 0, ?, ?, date_add(curdate(), interval ? day), ?, '', ?)", Statement.RETURN_GENERATED_KEYS)) {
			ps.setInt(1, idOferta);
			ps.setInt(2, idCliente);
			ps.setString(3, "Emitido por el envio de publicidad " + idEnvio);
			ps.setString(4, codigo);
			ps.setString(5, recortar(usuario, 20));
			ps.setInt(6, dias);
			ps.setDouble(7, saldo);
			ps.setLong(8, idEnvio);
			ps.executeUpdate();
			try (ResultSet k = ps.getGeneratedKeys()) {
				if (k.next()) {
					e.idOfertaCliente = k.getInt(1);
				}
			}
		}
		try (PreparedStatement ps = cn.prepareStatement("select fecha_caducidad from oferta_cliente where idofertacliente = ?")) {
			ps.setInt(1, e.idOfertaCliente);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					e.fechaCaducidad = texto(rs.getString(1), "");
				}
			}
		}
		e.codigo = codigo;
		e.saldo = saldo;
		return e;
	}

	/** Igual que emitir(cn, ...) pero abriendo y cerrando su propia conexion. */
	public static Emision emitir(final int idOferta, final int idCliente, final long idEnvio, final String usuario) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			return emitir(cn, idOferta, idCliente, idEnvio, usuario);
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.emitir: " + ex.toString());
			final Emision e = new Emision();
			e.error = "No se pudo emitir el codigo: " + ex.getMessage();
			return e;
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Dice, ANTES de abrir la tanda, si la oferta se puede mandar en una campana. "" si se puede, o el motivo.
	 * Son las mismas reglas de emitir: si se dejaran para el primer destinatario, la tanda se abriria, cargaria
	 * toda la gente y fallaria una por una.
	 */
	public static String problemaParaEnviar(final int idOferta) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("select tipo_oferta, habilitado, codigo_promocional,"
					+ " dias_caducidad, max_emision, (select count(*) from oferta_cliente c where c.idoferta = o.idoferta)"
					+ " from oferta o where o.idoferta = ?")) {
				ps.setInt(1, idOferta);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return "La oferta no existe.";
					}
					if (!"S".equals(texto(rs.getString(2), "S"))) {
						return "La oferta esta deshabilitada.";
					}
					if (!"C".equals(texto(rs.getString(1), "C")) || !"S".equals(texto(rs.getString(3), "N"))) {
						return "Solo las ofertas personales con codigo promocional se pueden enviar por campana.";
					}
					if (rs.getInt(4) <= 0) {
						return "La oferta no tiene dias de caducidad: un codigo enviado a muchos clientes tiene que vencer.";
					}
					final int tope = rs.getInt(5);
					if (tope > 0 && rs.getInt(6) >= tope) {
						return "La oferta ya llego a su tope de emision (" + tope + " codigos).";
					}
				}
			}
			return "";
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.problemaParaEnviar: " + ex.toString());
			return "No se pudo revisar la oferta.";
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * El cliente al que se le liga el codigo de una persona del CRM (una persona puede tener varios clientes).
	 * Se prefiere el que tiene ese mismo correo y acepto la politica de datos, y despues el mas reciente.
	 *
	 * @return 0 si la persona no tiene ningun cliente
	 */
	public static int clienteDePersona(final long idPersona, final String correo) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("select idcliente from cliente where idpersona = ?"
					+ " order by (trim(email) = ?) desc, (politica_datos = 'S') desc, idcliente desc limit 1")) {
				ps.setLong(1, idPersona);
				ps.setString(2, correo == null ? "" : correo.trim());
				try (ResultSet rs = ps.executeQuery()) {
					return rs.next() ? rs.getInt(1) : 0;
				}
			}
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.clienteDePersona: " + ex.toString());
			return 0;
		} finally {
			cerrar(cn);
		}
	}

	/** Anula un codigo que todavia no se ha usado (por ejemplo, porque el correo no salio). @return true si quedo anulado */
	public static boolean anularCodigo(final int idOfertaCliente, final String motivo, final String usuario) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			int idOferta = 0;
			try (PreparedStatement ps = cn.prepareStatement("select idoferta from oferta_cliente where idofertacliente = ?")) {
				ps.setInt(1, idOfertaCliente);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						idOferta = rs.getInt(1);
					}
				}
			}
			try (PreparedStatement ps = cn.prepareStatement("update oferta_cliente set anulada = 'S', reservada_hasta = null,"
					+ " reserva_token = null where idofertacliente = ? and utilizada = 'N' and anulada = 'N'")) {
				ps.setInt(1, idOfertaCliente);
				if (ps.executeUpdate() == 0) {
					return false;
				}
			}
			bitacora(cn, idOferta, usuario, "ANULAR_CODIGO", "Codigo " + idOfertaCliente + ": " + motivo);
			return true;
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.anularCodigo: " + ex.toString());
			return false;
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Anula TODOS los codigos sin usar que emitio un envio: sirve cuando se mando la oferta equivocada o con
	 * un error. Los ya usados no se tocan.
	 *
	 * @return cuantos codigos se anularon, o -1 si fallo
	 */
	public static int anularEnvio(final long idEnvio, final String usuario) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			int idOferta = 0;
			try (PreparedStatement ps = cn.prepareStatement("select idoferta from oferta_cliente where idenvio = ? limit 1")) {
				ps.setLong(1, idEnvio);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						idOferta = rs.getInt(1);
					}
				}
			}
			int n;
			try (PreparedStatement ps = cn.prepareStatement("update oferta_cliente set anulada = 'S', reservada_hasta = null,"
					+ " reserva_token = null where idenvio = ? and utilizada = 'N' and anulada = 'N'")) {
				ps.setLong(1, idEnvio);
				n = ps.executeUpdate();
			}
			bitacora(cn, idOferta, usuario, "ANULAR_ENVIO", "Envio " + idEnvio + ": " + n + " codigos sin usar anulados");
			return n;
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.anularEnvio: " + ex.toString());
			return -1;
		} finally {
			cerrar(cn);
		}
	}

	/** Deja constancia de un cambio o una accion sobre una oferta. Nunca lanza: la bitacora no tumba la accion. */
	public static void bitacora(final Connection cn, final int idOferta, final String usuario, final String accion,
			final String detalle) {
		try (PreparedStatement ps = cn.prepareStatement("insert into oferta_bitacora (idoferta, fecha_real, usuario, accion, detalle)"
				+ " values (?, now(), ?, ?, ?)")) {
			ps.setInt(1, idOferta);
			ps.setString(2, recortar(usuario, 50));
			ps.setString(3, recortar(accion, 20));
			ps.setString(4, recortar(detalle, 500));
			ps.executeUpdate();
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.bitacora: " + ex.toString());
		}
	}

	/** Una oferta que se puede escoger para un envio de publicidad. */
	public static class OfertaEnviable {
		public int idOferta;
		public String nombre = "";
		public double valor;
		public int porcentaje;
		public int dias;
		public int maxEmision;
		public int emitidos;
		public String redParcial = "N";
	}

	/** Las ofertas que cumplen lo que exige un envio: personales, habilitadas, con codigo y con caducidad, y vigentes. */
	public static java.util.ArrayList<OfertaEnviable> ofertasEnviables() {
		final java.util.ArrayList<OfertaEnviable> lista = new java.util.ArrayList<OfertaEnviable>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			try (PreparedStatement ps = cn.prepareStatement("select o.idoferta, o.nombre_oferta, o.descuento_fijo_valor,"
					+ " o.descuento_fijo_porcentaje, o.dias_caducidad, o.max_emision, o.red_parcial,"
					+ " (select count(*) from oferta_cliente c where c.idoferta = o.idoferta) as emitidos"
					+ " from oferta o where o.tipo_oferta = 'C' and o.habilitado = 'S' and o.codigo_promocional = 'S'"
					+ " and o.dias_caducidad > 0"
					+ " order by o.idoferta desc");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final OfertaEnviable o = new OfertaEnviable();
					o.idOferta = rs.getInt(1);
					o.nombre = texto(rs.getString(2), "");
					o.valor = rs.getDouble(3);
					o.porcentaje = rs.getInt(4);
					o.dias = rs.getInt(5);
					o.maxEmision = rs.getInt(6);
					o.redParcial = texto(rs.getString(7), "N");
					o.emitidos = rs.getInt(8);
					lista.add(o);
				}
			}
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CodigoPromoDAO.ofertasEnviables: " + ex.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/** Un codigo de 8 caracteres, sin vocales ni caracteres que se confunden (0/O, 1/I), que no exista ya. */
	public static String generarCodigo(final Connection cn) throws SQLException {
		for (int intento = 0; intento < 50; intento++) {
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < LARGO_CODIGO; i++) {
				sb.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
			}
			final String codigo = sb.toString();
			try (PreparedStatement ps = cn.prepareStatement("select count(*) from oferta_cliente where codigo_promocion = ?")) {
				ps.setString(1, codigo);
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					if (rs.getInt(1) == 0) {
						return codigo;
					}
				}
			}
		}
		throw new SQLException("No se pudo generar un codigo unico.");
	}

	// =======================================================================
	// Plomeria
	// =======================================================================

	private static String nuevoToken() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
	}

	private static boolean contiene(final String lista, final int idTienda) {
		for (final String parte : lista.split(",")) {
			if (entero(parte.trim()) == idTienda) {
				return true;
			}
		}
		return false;
	}

	private static String formato(final double v) {
		return String.format("%,.0f", v).replace(',', '.');
	}

	private static String texto(final String s, final String defecto) {
		return s == null ? defecto : s;
	}

	private static int entero(final String s) {
		try {
			return Integer.parseInt(s.trim());
		} catch (final Exception e) {
			return 0;
		}
	}

	private static String recortar(final String s, final int max) {
		if (s == null) {
			return null;
		}
		return s.length() > max ? s.substring(0, max) : s;
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CodigoPromoDAO: no cerro la conexion, " + e.toString());
		}
	}
}

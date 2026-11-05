package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Las consultas de la pantalla "Consulta de puntos cliente", la que usan el
 * contact center y los lideres para atender a un cliente que pregunta por sus
 * puntos.
 *
 * QUE RESPONDE
 *
 * Todo lo que una persona al telefono necesita para contestar sin ir a pedirle
 * nada a sistemas: cuantos puntos tiene, cuales se le vencen y cuando, de donde
 * salieron, en que los gasto, si alguna redencion se le reverso, y como se
 * comporta como cliente.
 *
 * COMO SE CALCULA EL SALDO
 *
 * Se suma sobre fidelizacion_transaccion, que es el libro con un pedido detras de
 * cada fila, y NO se lee cliente_fidelizacion.puntos_vigentes como verdad: ese
 * acumulado se desvia (hoy 13 de 19.179 cuentas). El saldo guardado se trae solo
 * para compararlo y avisar cuando no cuadra.
 *
 *   saldo = puntos - puntos_redimidos - puntos_vencidos   (todas las filas)
 *
 * Verificado el 2026-11-05 contra una cuenta real: 1463,40 - 932,41 - 1,00 = 529,99,
 * igual al saldo guardado.
 *
 * LO QUE ESTA PANTALLA NO MUESTRA, A PROPOSITO
 *
 * Quien atendio cada pedido (el cajero) y las senales de auditoria. Esta
 * pantalla la ve todo el contact center; eso es de la Auditoria de Fidelizacion,
 * que tiene su propio rol. Aqui se muestra el canal (Contact Center, Punto de
 * venta, Administracion), que es lo que el cliente puede reclamar.
 */
public class ConsultaPuntosClienteDAO {

	/** Una persona encontrada por la busqueda. */
	public static class Candidato {
		public String correo = "";
		public String nombre = "";
		public String celular = "";
		public boolean enPrograma;
	}

	/** La ficha de la persona. */
	public static class Ficha {
		public String correo = "";
		public String nombre = "";
		public String celular = "";
		public boolean enPrograma;
		public boolean activo;
		public String vinculado = "";
		public String canal = "";
		public double saldoGuardado;
		public boolean politicaDatos;
		public boolean hayCrm;
		public long idPersona;
	}

	/** Los totales de puntos, calculados sobre el libro de transacciones. */
	public static class Totales {
		public int acumulaciones;
		public double valorCompras;
		public double ganados;
		public double redimidos;
		public double vencidos;
		public double saldo;
		/** Lo que se vence de aqui a 60 dias (la ventana del aviso). Incluye lo de 15 y 30 dias. */
		public double porVencer;
		/** Lo que se vence de aqui a 15 dias. Es lo mas urgente de decirle al cliente. */
		public double porVencer15;
		/** Lo que se vence de aqui a 30 dias. */
		public double porVencer30;
		public int diasAviso = 60;
		public String proximoVencimiento = "";
		public double puntosProximoVencimiento;
		public int redencionesConfirmadas;
		public int redencionesReversadas;
		public String primeraAcumulacion = "";
		public String ultimaAcumulacion = "";
	}

	/** Un grupo de puntos que vence el mismo dia. */
	public static class Vencimiento {
		public String fecha = "";
		public double puntos;
		/** La fecha ya paso y la fila sigue sin marcarse como vencida. */
		public boolean atrasado;
	}

	/** Una linea de la historia: acumula, redime, reversa o vence. */
	public static class Movimiento {
		/** yyyy-MM-dd HH:mm:ss, para poder ordenar. */
		public String momento = "";
		/** ACUMULA, REDIME, REVERSA o VENCE. */
		public String tipo = "";
		public String tienda = "";
		public int idPedidoTienda;
		public double valorNeto;
		/** Con signo: suma (+) o resta (-) al saldo. */
		public double puntos;
		public String canal = "";
		public String estado = "";
		public String detalle = "";
	}

	/** Lo que la persona ha hecho en cada tienda. */
	public static class PorTienda {
		public String tienda = "";
		public int pedidos;
		public double valor;
		public double puntos;
		public String ultima = "";
	}

	/** Lo que la persona ha hecho en cada mes. */
	public static class PorMes {
		public String mes = "";
		public int pedidos;
		public double valor;
		public double puntos;
	}

	/** Lo que dice el CRM de la persona. */
	public static class Comportamiento {
		public boolean hay;
		public String segmento = "";
		public int pedidos;
		public double valor;
		public double ticketPromedio;
		public String primerPedido = "";
		public String ultimoPedido = "";
		public int diasSinComprar;
		public int diasEntrePedidos;
		public int tiendasDistintas;
		public String tiendaHabitual = "";
		public String familiaFavorita = "";
		public int pizzas12m;
	}

	/** Un bono emitido a la persona. */
	public static class Bono {
		public String nombre = "";
		public String codigo = "";
		public double valor;
		public String estado = "";
		public String caducidad = "";
	}

	// ----------------------------------------------------------------------------------------
	// Busqueda
	// ----------------------------------------------------------------------------------------

	/**
	 * Busca por correo o por celular.
	 *
	 * El correo es la llave del programa, asi que si lo escriben se devuelve tal
	 * cual, este o no en el programa: la pantalla le dice a quien atiende que ese
	 * cliente no se ha vinculado, que tambien es una respuesta util.
	 *
	 * El celular se busca en cliente.celular_norm (con indice) y puede dar varios
	 * correos, porque una misma persona tiene fichas en varias tiendas.
	 */
	public static ArrayList<Candidato> buscar(final String consulta) {
		final ArrayList<Candidato> lista = new ArrayList<Candidato>();
		final String q = texto(consulta).trim();
		if (q.length() == 0) {
			return (lista);
		}
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			if (q.indexOf('@') >= 0) {
				final Candidato c = new Candidato();
				c.correo = q.toLowerCase();
				llenarCandidato(cn, c);
				lista.add(c);
			} else {
				String digitos = q.replaceAll("[^0-9]", "");
				if (digitos.length() > 10) {
					digitos = digitos.substring(digitos.length() - 10);
				}
				if (digitos.length() < 7) {
					return (lista);
				}
				final PreparedStatement ps = cn.prepareStatement(
						"SELECT DISTINCT LOWER(TRIM(email)) AS correo FROM pizzaamericana.cliente"
						+ " WHERE celular_norm = ? AND email IS NOT NULL AND TRIM(email) <> ''"
						+ "   AND UPPER(TRIM(email)) <> 'NO TIENE' AND email LIKE '%@%' LIMIT 8");
				ps.setString(1, digitos);
				final ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					final Candidato c = new Candidato();
					c.correo = texto(rs.getString("correo"));
					lista.add(c);
				}
				rs.close();
				ps.close();
				for (int i = 0; i < lista.size(); i++) {
					llenarCandidato(cn, lista.get(i));
				}
			}
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.buscar: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Nombre, celular y si esta en el programa, para mostrar al lado del correo. */
	private static void llenarCandidato(final Connection cn, final Candidato c) {
		try {
			PreparedStatement ps = cn.prepareStatement(
					"SELECT TRIM(CONCAT(IFNULL(nombre,''),' ',IFNULL(apellido,''))) AS nombre, IFNULL(celular,'') AS celular"
					+ "  FROM crm.persona_resumen WHERE LOWER(TRIM(email)) = ? ORDER BY pedidos DESC LIMIT 1");
			ps.setString(1, c.correo);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				c.nombre = texto(rs.getString("nombre"));
				c.celular = texto(rs.getString("celular"));
			}
			rs.close();
			ps.close();
			if (c.nombre.length() == 0) {
				ps = cn.prepareStatement(
						"SELECT TRIM(CONCAT(IFNULL(nombre,''),' ',IFNULL(apellido,''))) AS nombre,"
						+ " IFNULL(telefono_celular,'') AS celular FROM pizzaamericana.cliente"
						+ " WHERE LOWER(TRIM(email)) = ? LIMIT 1");
				ps.setString(1, c.correo);
				rs = ps.executeQuery();
				if (rs.next()) {
					c.nombre = texto(rs.getString("nombre"));
					c.celular = texto(rs.getString("celular"));
				}
				rs.close();
				ps.close();
			}
			ps = cn.prepareStatement("SELECT 1 FROM pizzaamericana.cliente_fidelizacion WHERE correo = ?");
			ps.setString(1, c.correo);
			rs = ps.executeQuery();
			c.enPrograma = rs.next();
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ConsultaPuntosClienteDAO.llenarCandidato: " + e.toString());
		}
	}

	// ----------------------------------------------------------------------------------------
	// La ficha y los totales
	// ----------------------------------------------------------------------------------------

	/** La ficha del cliente en el programa y los datos de contacto del CRM. */
	public static Ficha ficha(final String correo) {
		final Ficha f = new Ficha();
		f.correo = correo;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			PreparedStatement ps = cn.prepareStatement(
					"SELECT activo, IFNULL(puntos_vigentes,0) AS saldo, DATE(fecha_vinculacion) AS vinculado,"
					+ " IFNULL(canal,'') AS canal FROM pizzaamericana.cliente_fidelizacion WHERE correo = ?");
			ps.setString(1, correo);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				f.enPrograma = true;
				f.activo = "S".equals(rs.getString("activo"));
				f.saldoGuardado = rs.getDouble("saldo");
				f.vinculado = texto(rs.getString("vinculado"));
				f.canal = texto(rs.getString("canal"));
			}
			rs.close();
			ps.close();

			ps = cn.prepareStatement(
					"SELECT idpersona, TRIM(CONCAT(IFNULL(nombre,''),' ',IFNULL(apellido,''))) AS nombre,"
					+ " IFNULL(celular,'') AS celular, politica_datos FROM crm.persona_resumen"
					+ " WHERE LOWER(TRIM(email)) = LOWER(TRIM(?)) ORDER BY pedidos DESC LIMIT 1");
			ps.setString(1, correo);
			rs = ps.executeQuery();
			if (rs.next()) {
				f.hayCrm = true;
				f.idPersona = rs.getLong("idpersona");
				f.nombre = texto(rs.getString("nombre"));
				f.celular = texto(rs.getString("celular"));
				f.politicaDatos = "S".equals(rs.getString("politica_datos"));
			}
			rs.close();
			ps.close();
			if (f.nombre.length() == 0) {
				ps = cn.prepareStatement(
						"SELECT TRIM(CONCAT(IFNULL(nombre,''),' ',IFNULL(apellido,''))) AS nombre,"
						+ " IFNULL(telefono_celular,'') AS celular FROM pizzaamericana.cliente"
						+ " WHERE LOWER(TRIM(email)) = LOWER(TRIM(?)) LIMIT 1");
				ps.setString(1, correo);
				rs = ps.executeQuery();
				if (rs.next()) {
					f.nombre = texto(rs.getString("nombre"));
					f.celular = texto(rs.getString("celular"));
				}
				rs.close();
				ps.close();
			}
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.ficha: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (f);
	}

	/**
	 * Los totales de puntos en una sola consulta: la pantalla la pide cada vez que
	 * se busca a alguien y varios viajes a la base por busqueda se sienten.
	 */
	public static Totales totales(final String correo, final int diasAviso) {
		final Totales t = new Totales();
		t.diasAviso = diasAviso;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			PreparedStatement ps = cn.prepareStatement(
					"SELECT COUNT(*) AS n, IFNULL(SUM(valor_neto),0) AS valor, IFNULL(SUM(puntos),0) AS ganados,"
					+ " IFNULL(SUM(puntos_redimidos),0) AS redimidos, IFNULL(SUM(puntos_vencidos),0) AS vencidos,"
					+ " IFNULL(SUM(CASE WHEN vencidos = 'N' THEN puntos - puntos_redimidos ELSE 0 END),0) AS saldo,"
					+ " IFNULL(SUM(CASE WHEN vencidos = 'N' AND puntos > puntos_redimidos"
					+ "   AND fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)"
					+ "   THEN puntos - puntos_redimidos ELSE 0 END),0) AS por_vencer,"
					+ " IFNULL(SUM(CASE WHEN vencidos = 'N' AND puntos > puntos_redimidos"
					+ "   AND fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 15 DAY)"
					+ "   THEN puntos - puntos_redimidos ELSE 0 END),0) AS por_vencer_15,"
					+ " IFNULL(SUM(CASE WHEN vencidos = 'N' AND puntos > puntos_redimidos"
					+ "   AND fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)"
					+ "   THEN puntos - puntos_redimidos ELSE 0 END),0) AS por_vencer_30,"
					+ " MIN(fecha_transaccion) AS primera, MAX(fecha_transaccion) AS ultima"
					+ "  FROM pizzaamericana.fidelizacion_transaccion WHERE correo = ?");
			ps.setInt(1, diasAviso);
			ps.setString(2, correo);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				t.acumulaciones = rs.getInt("n");
				t.valorCompras = rs.getDouble("valor");
				t.ganados = rs.getDouble("ganados");
				t.redimidos = rs.getDouble("redimidos");
				t.vencidos = rs.getDouble("vencidos");
				t.saldo = rs.getDouble("saldo");
				t.porVencer = rs.getDouble("por_vencer");
				t.porVencer15 = rs.getDouble("por_vencer_15");
				t.porVencer30 = rs.getDouble("por_vencer_30");
				t.primeraAcumulacion = texto(rs.getString("primera"));
				t.ultimaAcumulacion = texto(rs.getString("ultima"));
			}
			rs.close();
			ps.close();

			// El proximo vencimiento, con los puntos que caen ESE dia (no todo el saldo).
			ps = cn.prepareStatement(
					"SELECT fecha_vencimiento AS vence, SUM(puntos - puntos_redimidos) AS puntos"
					+ "  FROM pizzaamericana.fidelizacion_transaccion"
					+ " WHERE correo = ? AND vencidos = 'N' AND puntos > puntos_redimidos AND fecha_vencimiento >= CURDATE()"
					+ " GROUP BY fecha_vencimiento HAVING puntos > 0 ORDER BY fecha_vencimiento LIMIT 1");
			ps.setString(1, correo);
			rs = ps.executeQuery();
			if (rs.next()) {
				t.proximoVencimiento = texto(rs.getString("vence"));
				t.puntosProximoVencimiento = rs.getDouble("puntos");
			}
			rs.close();
			ps.close();

			ps = cn.prepareStatement(
					"SELECT SUM(estado = 'CONFIRMADA') AS conf, SUM(estado = 'REVERSADA') AS rev"
					+ "  FROM pizzaamericana.fidelizacion_redencion WHERE correo = ?");
			ps.setString(1, correo);
			rs = ps.executeQuery();
			if (rs.next()) {
				t.redencionesConfirmadas = rs.getInt("conf");
				t.redencionesReversadas = rs.getInt("rev");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.totales: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (t);
	}

	/** Cuando vencen los puntos que le quedan, agrupados por fecha. */
	public static ArrayList<Vencimiento> vencimientos(final String correo) {
		final ArrayList<Vencimiento> lista = new ArrayList<Vencimiento>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT fecha_vencimiento AS vence, SUM(puntos - puntos_redimidos) AS puntos,"
					+ " (fecha_vencimiento < CURDATE()) AS atrasado"
					+ "  FROM pizzaamericana.fidelizacion_transaccion"
					+ " WHERE correo = ? AND vencidos = 'N' AND puntos > puntos_redimidos"
					+ " GROUP BY fecha_vencimiento HAVING puntos > 0 ORDER BY fecha_vencimiento LIMIT 24");
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Vencimiento v = new Vencimiento();
				v.fecha = texto(rs.getString("vence"));
				v.puntos = rs.getDouble("puntos");
				v.atrasado = rs.getInt("atrasado") == 1;
				lista.add(v);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.vencimientos: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// ----------------------------------------------------------------------------------------
	// La historia
	// ----------------------------------------------------------------------------------------

	/**
	 * Todos los movimientos de puntos de la persona, del mas reciente al mas viejo.
	 *
	 * Junta cuatro cosas que viven en dos tablas: lo que se acumulo, lo que se
	 * redimio, lo que se reverso de esas redenciones y lo que se vencio. Una
	 * redencion reversada produce DOS lineas -la redencion y la devolucion-, para
	 * que el cliente pueda ver que los puntos le volvieron.
	 */
	public static ArrayList<Movimiento> movimientos(final String correo, final int limite) {
		final ArrayList<Movimiento> lista = new ArrayList<Movimiento>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			PreparedStatement ps = cn.prepareStatement(
					"SELECT ft.fecha_transaccion, ft.fecha_vencimiento, ft.idpedidotienda, ft.valor_neto, ft.puntos,"
					+ " ft.puntos_redimidos, ft.puntos_vencidos, ft.vencidos, IFNULL(t.nombre,'') AS tienda"
					+ "  FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = ft.idtienda"
					+ " WHERE ft.correo = ? ORDER BY ft.fecha_transaccion DESC, ft.idpedidotienda DESC LIMIT 1000");
			ps.setString(1, correo);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final String fechaCompra = texto(rs.getString("fecha_transaccion"));
				final String tienda = texto(rs.getString("tienda"));
				final int pedido = rs.getInt("idpedidotienda");

				final Movimiento a = new Movimiento();
				a.momento = fechaCompra + " 00:00:00";
				a.tipo = "ACUMULA";
				a.tienda = tienda;
				a.idPedidoTienda = pedido;
				a.valorNeto = rs.getDouble("valor_neto");
				a.puntos = rs.getDouble("puntos");
				a.detalle = "Compra del " + fechaCompra + ". Vence el " + texto(rs.getString("fecha_vencimiento")) + ".";
				lista.add(a);

				final double vencidos = rs.getDouble("puntos_vencidos");
				if (vencidos > 0) {
					final Movimiento v = new Movimiento();
					v.momento = texto(rs.getString("fecha_vencimiento")) + " 00:00:00";
					v.tipo = "VENCE";
					v.tienda = tienda;
					v.idPedidoTienda = pedido;
					v.puntos = -vencidos;
					v.detalle = "Se vencieron puntos de la compra del " + fechaCompra + ".";
					lista.add(v);
				}
			}
			rs.close();
			ps.close();

			ps = cn.prepareStatement(
					"SELECT r.fecha_redencion, r.puntos_redimidos, r.idpedidotienda, IFNULL(r.origen,'') AS origen,"
					+ " IFNULL(r.estado,'') AS estado, IFNULL(r.motivo,'') AS motivo, IFNULL(r.puntos_reversados,0) AS rev,"
					+ " r.fecha_estado, IFNULL(t.nombre,'') AS tienda"
					+ "  FROM pizzaamericana.fidelizacion_redencion r"
					+ "  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = r.idtienda"
					+ " WHERE r.correo = ? ORDER BY r.fecha_redencion DESC LIMIT 500");
			ps.setString(1, correo);
			rs = ps.executeQuery();
			while (rs.next()) {
				final String estado = texto(rs.getString("estado"));
				final String canal = etiquetaCanal(texto(rs.getString("origen")));
				final String tienda = texto(rs.getString("tienda"));
				final int pedido = rs.getInt("idpedidotienda");
				final double puntos = rs.getDouble("puntos_redimidos");

				final Movimiento m = new Movimiento();
				m.momento = momento(rs.getString("fecha_redencion"));
				m.tipo = "REDIME";
				m.tienda = tienda;
				m.idPedidoTienda = pedido;
				m.puntos = -puntos;
				m.canal = canal;
				m.estado = estado;
				m.detalle = "Redencion de puntos" + (pedido > 0 ? " en el pedido " + pedido : "") + ".";
				lista.add(m);

				final double reversados = rs.getDouble("rev");
				if ("REVERSADA".equals(estado) && reversados > 0) {
					final Movimiento d = new Movimiento();
					d.momento = momento(rs.getString("fecha_estado"));
					if (d.momento.length() == 0) {
						d.momento = m.momento;
					}
					d.tipo = "REVERSA";
					d.tienda = tienda;
					d.idPedidoTienda = pedido;
					d.puntos = reversados;
					d.canal = canal;
					d.estado = estado;
					d.detalle = "Se devolvieron los puntos: " + texto(rs.getString("motivo"));
					lista.add(d);
				}
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.movimientos: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		Collections.sort(lista, new Comparator<Movimiento>() {
			public int compare(final Movimiento x, final Movimiento y) {
				return (y.momento.compareTo(x.momento));
			}
		});
		if (limite > 0 && lista.size() > limite) {
			return (new ArrayList<Movimiento>(lista.subList(0, limite)));
		}
		return (lista);
	}

	// ----------------------------------------------------------------------------------------
	// El comportamiento
	// ----------------------------------------------------------------------------------------

	/** Lo que el CRM sabe de la persona: cuanto compra, cada cuanto y que le gusta. */
	public static Comportamiento comportamiento(final String correo) {
		final Comportamiento c = new Comportamiento();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT p.segmento, p.pedidos, p.valor, p.ticket_promedio, p.primer_pedido, p.ultimo_pedido,"
					+ " p.dias_sin_comprar, p.dias_entre_pedidos, p.tiendas_distintas, IFNULL(t.nombre,'') AS tienda,"
					+ " IFNULL(p.familia_favorita,'') AS familia, IFNULL(p.pizzas_12m,0) AS pizzas"
					+ "  FROM crm.persona_resumen p LEFT JOIN pizzaamericana.tienda t ON t.idtienda = p.tienda_habitual"
					+ " WHERE LOWER(TRIM(p.email)) = LOWER(TRIM(?)) ORDER BY p.pedidos DESC LIMIT 1");
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				c.hay = true;
				c.segmento = texto(rs.getString("segmento"));
				c.pedidos = rs.getInt("pedidos");
				c.valor = rs.getDouble("valor");
				c.ticketPromedio = rs.getDouble("ticket_promedio");
				c.primerPedido = texto(rs.getString("primer_pedido"));
				c.ultimoPedido = texto(rs.getString("ultimo_pedido"));
				c.diasSinComprar = rs.getInt("dias_sin_comprar");
				c.diasEntrePedidos = rs.getInt("dias_entre_pedidos");
				c.tiendasDistintas = rs.getInt("tiendas_distintas");
				c.tiendaHabitual = texto(rs.getString("tienda"));
				c.familiaFavorita = texto(rs.getString("familia"));
				c.pizzas12m = rs.getInt("pizzas");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.comportamiento: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (c);
	}

	/** Pedidos con puntos por tienda. */
	public static ArrayList<PorTienda> porTienda(final String correo) {
		final ArrayList<PorTienda> lista = new ArrayList<PorTienda>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT IFNULL(t.nombre,'?') AS tienda, COUNT(*) AS pedidos, SUM(ft.valor_neto) AS valor,"
					+ " SUM(ft.puntos) AS puntos, MAX(ft.fecha_transaccion) AS ultima"
					+ "  FROM pizzaamericana.fidelizacion_transaccion ft"
					+ "  LEFT JOIN pizzaamericana.tienda t ON t.idtienda = ft.idtienda"
					+ " WHERE ft.correo = ? GROUP BY ft.idtienda, t.nombre ORDER BY pedidos DESC");
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final PorTienda p = new PorTienda();
				p.tienda = texto(rs.getString("tienda"));
				p.pedidos = rs.getInt("pedidos");
				p.valor = rs.getDouble("valor");
				p.puntos = rs.getDouble("puntos");
				p.ultima = texto(rs.getString("ultima"));
				lista.add(p);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.porTienda: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Pedidos con puntos por mes, los ultimos 12 meses. */
	public static ArrayList<PorMes> porMes(final String correo) {
		final ArrayList<PorMes> lista = new ArrayList<PorMes>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT DATE_FORMAT(fecha_transaccion,'%Y-%m') AS mes, COUNT(*) AS pedidos,"
					+ " SUM(valor_neto) AS valor, SUM(puntos) AS puntos"
					+ "  FROM pizzaamericana.fidelizacion_transaccion"
					+ " WHERE correo = ? AND fecha_transaccion >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)"
					+ " GROUP BY mes ORDER BY mes");
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final PorMes m = new PorMes();
				m.mes = texto(rs.getString("mes"));
				m.pedidos = rs.getInt("pedidos");
				m.valor = rs.getDouble("valor");
				m.puntos = rs.getDouble("puntos");
				lista.add(m);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("ConsultaPuntosClienteDAO.porMes: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Los bonos de la persona. Va aparte y tolera el fallo: si falla, el resto de
	 * la ficha -que es lo que se pregunto- sale igual.
	 */
	public static ArrayList<Bono> bonos(final long idPersona) {
		final ArrayList<Bono> lista = new ArrayList<Bono>();
		if (idPersona <= 0) {
			return (lista);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT IFNULL(c.nombre,'') AS nombre, IFNULL(b.codigo,'') AS codigo, IFNULL(b.valor,0) AS valor,"
					+ " IFNULL(b.estado,'') AS estado, b.fecha_caducidad AS caducidad"
					+ "  FROM pizzaamericana.bono_emitido b LEFT JOIN pizzaamericana.bono_campana c ON c.idbono = b.idbono"
					+ " WHERE b.idpersona = ? ORDER BY b.calculado_en DESC LIMIT 10");
			ps.setLong(1, idPersona);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Bono b = new Bono();
				b.nombre = texto(rs.getString("nombre"));
				b.codigo = texto(rs.getString("codigo"));
				b.valor = rs.getDouble("valor");
				b.estado = texto(rs.getString("estado"));
				b.caducidad = texto(rs.getString("caducidad"));
				lista.add(b);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ConsultaPuntosClienteDAO.bonos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// ----------------------------------------------------------------------------------------
	// El catalogo de premios
	// ----------------------------------------------------------------------------------------

	/** Un producto que se puede pagar con puntos. */
	public static class Premio {
		public int idProducto;
		public String nombre = "";
		public String tipo = "";
		public double puntos;
	}

	/**
	 * Los productos que se pagan con puntos: los habilitados de la tabla producto
	 * del central que tienen precio_puntos mayor que cero, del mas barato al mas caro.
	 *
	 * OJO: es el catalogo del CENTRAL, el que usa el contact center. El POS de cada
	 * tienda tiene su propia tabla producto con su propia lista de premios, y las dos
	 * no son iguales (al 2026-11-05 el central trae 8 y Itagui 7, con premios distintos).
	 */
	public static ArrayList<Premio> catalogoPuntos() {
		final ArrayList<Premio> lista = new ArrayList<Premio>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idproducto, nombre, IFNULL(tipo,'') AS tipo, precio_puntos FROM pizzaamericana.producto"
					+ " WHERE habilitado = 'S' AND precio_puntos > 0 ORDER BY precio_puntos, nombre");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Premio p = new Premio();
				p.idProducto = rs.getInt("idproducto");
				p.nombre = texto(rs.getString("nombre"));
				p.tipo = texto(rs.getString("tipo"));
				p.puntos = rs.getDouble("precio_puntos");
				lista.add(p);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ConsultaPuntosClienteDAO.catalogoPuntos: " + e.toString(), e);
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// ----------------------------------------------------------------------------------------
	// Permiso
	// ----------------------------------------------------------------------------------------

	/**
	 * Si el usuario tiene, por alguno de sus roles, la pantalla ConsultaPuntosCliente.html.
	 *
	 * Se lee de la base en cada peticion -no de la sesion, que se congela al
	 * ingresar- y sigue la matriz de roles: quien administra la seguridad le da o
	 * le quita la pantalla a un rol y esto responde sin desplegar nada.
	 */
	public static boolean tienePantalla(final int idUsuario) {
		if (idUsuario <= 0) {
			return (false);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT 1 FROM pizzaamericana.usuario_rol ur"
					+ "  JOIN pizzaamericana.rol_pantalla rp ON rp.idrol = ur.idrol"
					+ "  JOIN pizzaamericana.pantalla p ON p.idpantalla = rp.idpantalla"
					+ " WHERE ur.idusuario = ? AND p.url_html = 'ConsultaPuntosCliente.html' LIMIT 1");
			ps.setInt(1, idUsuario);
			final ResultSet rs = ps.executeQuery();
			final boolean si = rs.next();
			rs.close();
			ps.close();
			return (si);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ConsultaPuntosClienteDAO.tienePantalla: " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	// ----------------------------------------------------------------------------------------

	/** El canal tal como se lo explica uno al cliente. */
	private static String etiquetaCanal(final String origen) {
		final String o = origen.toUpperCase();
		if ("CC".equals(o)) {
			return ("Contact Center");
		}
		if ("POS".equals(o)) {
			return ("Punto de venta");
		}
		if ("ADM".equals(o)) {
			return ("Administracion");
		}
		return (origen);
	}

	/** "2026-11-04 17:30:00.0" -> "2026-11-04 17:30:00"; vacio si no hay. */
	private static String momento(final String valor) {
		final String v = texto(valor);
		if (v.length() >= 19) {
			return (v.substring(0, 19));
		}
		return (v);
	}

	/** Que significa el segmento de una persona: el nombre, su explicacion y su color. */
	public static class Segmento {
		public boolean hay;
		public String nombre = "";
		public String descripcion = "";
		public String color = "";
	}

	/**
	 * La definicion del segmento, para poder decir que significa y no solo como
	 * se llama.
	 *
	 * "ORO" no le dice nada a quien esta en la caja; "compra seguido y compro
	 * hace poco" si. La explicacion y el color salen de crm.segmento_definicion,
	 * que es la misma tabla con la que se arman los segmentos: asi, si manana se
	 * cambia una regla o se renombra uno, las pantallas lo siguen solas y no hay
	 * una lista de colores escrita a mano en ninguna parte.
	 *
	 * Se busca por nombre porque es lo unico que trae crm.persona_resumen.
	 */
	public static Segmento segmento(final String nombre) {
		final Segmento s = new Segmento();
		if (nombre == null || nombre.trim().length() == 0) {
			return (s);
		}
		s.nombre = nombre.trim();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT descripcion, color FROM crm.segmento_definicion"
					+ " WHERE UPPER(TRIM(nombre)) = UPPER(TRIM(?)) LIMIT 1");
			ps.setString(1, s.nombre);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				s.hay = true;
				s.descripcion = texto(rs.getString("descripcion"));
				s.color = texto(rs.getString("color"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			//Que no se pueda leer la definicion no puede tumbar la ficha: se
			//queda sin explicacion y ya, el nombre del segmento igual se ve.
			logger.error("ConsultaPuntosClienteDAO.segmento: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (s);
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
			// Nada que hacer: ya se esta cerrando.
		}
	}
}

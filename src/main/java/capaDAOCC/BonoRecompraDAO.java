package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El bono de recompra: compre ahora y le devolvemos un bono para despues.
 *
 * ES AL REVES DE LO QUE HABIA. Hasta ahora el codigo promocional se le asignaba
 * al cliente ANTES de comprar, para que viniera. Aqui el bono se GANA
 * comprando, y por eso no se puede saber a quien darselo hasta que la compra ya
 * ocurrio.
 *
 * QUIEN LEE LAS COMPRAS
 *
 * No esta clase. El central solo ve los pedidos que el mismo tomo -medido el
 * 2026-09-28: tipos 1 y 4-, y todo el mostrador vive en la base de cada tienda.
 * Calcular aqui seria darle el bono unicamente a quien pide a domicilio.
 *
 * Las compras las lee el barrido nocturno de Servicios, que ya entra a las once
 * tiendas, y las deja en bono_pedido. Esta clase trabaja sobre ese libro.
 *
 * CUANDO SE EMITE
 *
 *   No repetible  al CERRAR la ventana. Es la unica forma de cumplir "si compra
 *                 tres veces se suman en un solo bono": emitir apenas cruza el
 *                 minimo dejaria por fuera las compras que vengan despues.
 *   Repetible     cada noche, con los pedidos que todavia no han contado. El
 *                 que sigue comprando se lo sigue ganando.
 */
public class BonoRecompraDAO {

	public static class Campana {
		public int idBono;
		public String nombre = "";
		public int idOferta;
		public String compraDesde = "";
		public String compraHasta = "";
		public double porcentaje;
		public double topeBono;
		public double baseMinima;
		public String productos = "";
		public boolean excluirPromociones = true;
		public boolean repetible = false;
		/** S = se lo gana cualquiera que compre. N = solo a quien se le invito. */
		public boolean abierta = true;
		/** La tanda de Envio de Publicidad con la que se invito. */
		public long idEnvio;
		/** El dia en que se emite. Vacio = al cerrar la ventana. */
		public String fechaEmision = "";
		/** Informativo, para el mensaje. */
		public String redimeDesde = "";
		/** Manda de verdad: de aqui sale la caducidad del codigo. */
		public String redimeHasta = "";
		public String estado = "";
		public boolean emitir = false;
		public boolean avisar = true;
		public String usuario = "";
		public String creadaEn = "";
		public String ultimoCalculoEn = "";
		/** Lo que ya lleva acumulado, para la pantalla. */
		public int personasConPedidos;
		public int personasQueCalifican;
		public int emitidos;
		public double valorEmitido;
	}

	/** Lo que se le calculo a una persona. */
	public static class Emision {
		public int idEmision;
		public long idPersona;
		public int idCliente;
		public int pedidos;
		public double base;
		public double valor;
		public boolean topado;
		public String codigo = "";
		public String fechaCaducidad = "";
		public String estado = "";
		public String detalle = "";
		public String destino = "";
	}

	// =======================================================================
	// La campana
	// =======================================================================

	public static ArrayList<Campana> listar(final boolean soloActivas) {
		final ArrayList<Campana> lista = new ArrayList<Campana>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"select c.*,"
					+ " (select count(distinct p.idpersona) from pizzaamericana.bono_pedido p"
					+ "   where p.idbono = c.idbono) as con_pedidos,"
					+ " (select count(*) from pizzaamericana.bono_emitido e"
					+ "   where e.idbono = c.idbono and e.estado <> 'FALLIDO') as emitidos,"
					+ " (select ifnull(sum(e.valor),0) from pizzaamericana.bono_emitido e"
					+ "   where e.idbono = c.idbono and e.estado <> 'FALLIDO') as valor_emitido"
					+ " from pizzaamericana.bono_campana c"
					+ (soloActivas ? " where c.estado = 'ACTIVA'" : "")
					+ " order by c.idbono desc");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(leer(rs));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO.listar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	public static Campana obtener(final Connection cn, final int idBono) throws SQLException {
		Campana c = null;
		try (PreparedStatement ps = cn.prepareStatement(
				"select c.*, 0 as con_pedidos, 0 as emitidos, 0 as valor_emitido"
				+ " from pizzaamericana.bono_campana c where c.idbono = ?")) {
			ps.setInt(1, idBono);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					c = leer(rs);
				}
			}
		}
		return (c);
	}

	/** Las campanas que el barrido tiene que mirar esta noche. */
	public static ArrayList<Campana> paraCalcular() {
		final ArrayList<Campana> lista = new ArrayList<Campana>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			//Se siguen mirando hasta 30 dias despues de cerrada la ventana: los
			//pedidos de los ultimos dias se leen la noche siguiente, y una
			//tienda que no respondio se lee cuando vuelva.
			final PreparedStatement ps = cn.prepareStatement(
					"select c.*, 0 as con_pedidos, 0 as emitidos, 0 as valor_emitido"
					+ " from pizzaamericana.bono_campana c"
					+ " where c.estado = 'ACTIVA'"
					+ "   and c.compra_desde <= curdate()"
					+ "   and c.compra_hasta >= curdate() - interval 30 day"
					+ " order by c.idbono");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(leer(rs));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO.paraCalcular: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	public static int guardar(final Campana c) {
		int id = c.idBono;
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			if (id > 0) {
				final PreparedStatement ps = cn.prepareStatement(
						"update pizzaamericana.bono_campana set nombre=?, idoferta=?, compra_desde=?,"
						+ " compra_hasta=?, porcentaje=?, tope_bono=?, base_minima=?, productos=?,"
						+ " excluir_promociones=?, repetible=?, estado=?, emitir=?, avisar=?,"
						+ " abierta=?, idenvio=?, fecha_emision=?, redime_desde=?, redime_hasta=?"
						+ " where idbono=?");
				ponerCampos(ps, c);
				ps.setInt(19, id);
				ps.executeUpdate();
				ps.close();
			} else {
				final PreparedStatement ps = cn.prepareStatement(
						"insert into pizzaamericana.bono_campana (nombre, idoferta, compra_desde,"
						+ " compra_hasta, porcentaje, tope_bono, base_minima, productos,"
						+ " excluir_promociones, repetible, estado, emitir, avisar,"
						+ " abierta, idenvio, fecha_emision, redime_desde, redime_hasta, usuario, creada_en)"
						+ " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now())",
						Statement.RETURN_GENERATED_KEYS);
				ponerCampos(ps, c);
				ps.setString(19, c.usuario);
				ps.executeUpdate();
				final ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					id = rs.getInt(1);
				}
				rs.close();
				ps.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO.guardar: " + e.toString());
			return (0);
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	private static void ponerCampos(final PreparedStatement ps, final Campana c) throws SQLException {
		ps.setString(1, c.nombre);
		ps.setInt(2, c.idOferta);
		ps.setString(3, c.compraDesde);
		ps.setString(4, c.compraHasta);
		ps.setDouble(5, c.porcentaje);
		ps.setDouble(6, c.topeBono);
		ps.setDouble(7, c.baseMinima);
		ps.setString(8, c.productos);
		ps.setString(9, c.excluirPromociones ? "S" : "N");
		ps.setString(10, c.repetible ? "S" : "N");
		ps.setString(11, c.estado);
		ps.setString(12, c.emitir ? "S" : "N");
		ps.setString(13, c.avisar ? "S" : "N");
		ps.setString(14, c.abierta ? "S" : "N");
		if (c.idEnvio > 0) {
			ps.setLong(15, c.idEnvio);
		} else {
			ps.setNull(15, java.sql.Types.BIGINT);
		}
		fecha(ps, 16, c.fechaEmision);
		fecha(ps, 17, c.redimeDesde);
		fecha(ps, 18, c.redimeHasta);
	}

	/** Una fecha vacia es NULL, no cadena vacia: MySQL rechaza '' en una columna DATE. */
	private static void fecha(final PreparedStatement ps, final int pos, final String valor)
			throws SQLException {
		if (valor != null && valor.length() >= 10) {
			ps.setString(pos, valor.substring(0, 10));
		} else {
			ps.setNull(pos, java.sql.Types.DATE);
		}
	}

	// =======================================================================
	// El libro de pedidos
	// =======================================================================

	/**
	 * Anota los pedidos de una tienda que cuentan para una campana.
	 *
	 * INSERT IGNORE contra la llave (idbono, idtienda, idpedido): el barrido
	 * corre todas las noches sobre la misma ventana, y sin esto la segunda
	 * noche volveria a sumar lo de la primera y el bono creceria solo. No es un
	 * riesgo que haya que cuidar, es imposible.
	 *
	 * Un pedido que YA conto no se actualiza aunque cambie de valor. Si a un
	 * pedido pagado dentro de un bono se le corrigiera el total, el bono ya
	 * emitido quedaria sin respaldo; se prefiere que el libro diga lo que se
	 * leyo el dia que se leyo.
	 *
	 * @return cuantos pedidos nuevos entraron
	 */
	public static int registrarPedidos(final Connection cn, final int idBono, final int idTienda,
			final ArrayList<long[]> pedidos, final ArrayList<Double> bases,
			final ArrayList<java.sql.Date> fechas) throws SQLException {
		int nuevos = 0;
		try (PreparedStatement ps = cn.prepareStatement(
				"insert ignore into pizzaamericana.bono_pedido"
				+ " (idbono, idtienda, idpedido, idpersona, fecha, base, leido_en)"
				+ " values (?,?,?,?,?,?,now())")) {
			for (int i = 0; i < pedidos.size(); i++) {
				ps.setInt(1, idBono);
				ps.setInt(2, idTienda);
				ps.setLong(3, pedidos.get(i)[0]);
				ps.setLong(4, pedidos.get(i)[1]);
				ps.setDate(5, fechas.get(i));
				ps.setDouble(6, bases.get(i).doubleValue());
				ps.addBatch();
				if ((i + 1) % 500 == 0) {
					final int[] r = ps.executeBatch();
					for (int j = 0; j < r.length; j++) {
						nuevos += r[j] > 0 ? 1 : 0;
					}
				}
			}
			final int[] r = ps.executeBatch();
			for (int j = 0; j < r.length; j++) {
				nuevos += r[j] > 0 ? 1 : 0;
			}
		}
		return (nuevos);
	}

	// =======================================================================
	// Calcular y emitir
	// =======================================================================

	public static class Resultado {
		public int califican;
		public int emitidos;
		public int fallidos;
		public double valor;
		public String aviso = "";
	}

	/**
	 * Cierra las cuentas de una campana: suma, calcula el bono y lo emite.
	 *
	 * Solo emite si la campana lo tiene prendido. Con emitir en N calcula todo
	 * y lo deja en CALCULADO sin tocarle nada al cliente: es el ensayo, para ver
	 * a cuantos les daria y por cuanto antes de repartir plata.
	 */
	public static Resultado cerrar(final Campana c, final String usuario) {
		final Resultado res = new Resultado();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			//CUANDO SE EMITE
			//
			//Si la campana dice un dia, manda ese dia. Es lo que hace cumplir
			//"se redime del lunes al miercoles": antes de emitir no hay codigo,
			//asi que no hay nada que redimir el fin de semana. No se puede
			//lograr validando al redimir, porque en un codigo personal el
			//sistema solo mira la caducidad, nunca una fecha de inicio.
			//
			//Sin dia dicho, se vuelve a la regla vieja: al cerrar la ventana,
			//que es la unica forma de sumar todas las compras en un bono.
			if (c.fechaEmision.length() >= 10) {
				if (hoy(cn).compareTo(c.fechaEmision.substring(0, 10)) < 0) {
					res.aviso = "Todavia no es el dia de emision (" + c.fechaEmision.substring(0, 10)
							+ "). Se sigue acumulando.";
					return (res);
				}
			} else if (!c.repetible && !ventanaCerrada(cn, c.idBono)) {
				res.aviso = "La ventana de compra todavia no cierra. Se sigue acumulando;"
						+ " el bono se emite cuando cierre, para que sume todas las compras.";
				return (res);
			}

			//Una campana por invitacion sin envio no tiene publico: emitirla
			//seria regalarle a todo el mundo, justo lo contrario de lo que se
			//pidio. Se para antes de repartir nada.
			if (!c.abierta && c.idEnvio <= 0) {
				res.aviso = "La campana es por invitacion pero no tiene envio asociado."
						+ " Sin el no se sabe a quien se invito y no se emite nada.";
				return (res);
			}

			final ArrayList<Emision> candidatos = candidatos(cn, c);
			res.califican = candidatos.size();
			if (!c.emitir) {
				res.aviso = "La campana esta en ensayo (emitir en N): se calculo pero no se emitio nada.";
				return (res);
			}

			for (int i = 0; i < candidatos.size(); i++) {
				final Emision e = candidatos.get(i);
				if (emitirUno(cn, c, e, usuario)) {
					res.emitidos++;
					res.valor += e.valor;
				} else {
					res.fallidos++;
				}
			}

			try (PreparedStatement ps = cn.prepareStatement(
					"update pizzaamericana.bono_campana set ultimo_calculo_en = now() where idbono = ?")) {
				ps.setInt(1, c.idBono);
				ps.executeUpdate();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO.cerrar: " + e.toString());
			res.aviso = "Fallo el cierre: " + e.toString();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/**
	 * A cuantos les daria y por cuanto, con lo que ya se acumulo HOY.
	 *
	 * Es la misma cuenta de candidatos() que usa el cierre real, pero sin las
	 * guardas de cuando se emite y sin emitir nada: sirve para ver el avance
	 * cualquier dia, no solo el dia de emision. El numero puede subir si
	 * siguen entrando pedidos antes de que el barrido cierre de verdad.
	 */
	public static Resultado previsualizar(final int idBono) {
		final Resultado res = new Resultado();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final Campana c = obtener(cn, idBono);
			if (c == null) {
				res.aviso = "No existe esa campana.";
				return (res);
			}
			final ArrayList<Emision> candidatos = candidatos(cn, c);
			res.califican = candidatos.size();
			for (int i = 0; i < candidatos.size(); i++) {
				res.valor += candidatos.get(i).valor;
			}
			res.aviso = "Estimado con lo acumulado hasta ahora. No es definitivo: puede"
					+ " subir si siguen entrando pedidos antes del cierre real.";
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO.previsualizar: " + e.toString());
			res.aviso = "No se pudo calcular: " + e.toString();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/** La fecha del servidor, que es la que manda; no la del equipo que llama. */
	private static String hoy(final Connection cn) throws SQLException {
		try (PreparedStatement ps = cn.prepareStatement("select curdate()")) {
			try (ResultSet rs = ps.executeQuery()) {
				return (rs.next() ? rs.getString(1) : "");
			}
		}
	}

	/** Si ya paso el ultimo dia en que una compra contaba. */
	private static boolean ventanaCerrada(final Connection cn, final int idBono) throws SQLException {
		try (PreparedStatement ps = cn.prepareStatement(
				"select compra_hasta < curdate() from pizzaamericana.bono_campana where idbono = ?")) {
			ps.setInt(1, idBono);
			try (ResultSet rs = ps.executeQuery()) {
				return (rs.next() && rs.getBoolean(1));
			}
		}
	}

	/**
	 * Quienes se ganaron bono y por cuanto.
	 *
	 * En una campana no repetible se excluye a quien ya tiene emision. Sus
	 * pedidos se quedan sin pagar en el libro a proposito: nunca contaron, y
	 * decir que contaron inflaria el reporte de lo que se repartio.
	 */
	private static ArrayList<Emision> candidatos(final Connection cn, final Campana c)
			throws SQLException {
		final ArrayList<Emision> lista = new ArrayList<Emision>();
		final StringBuilder sql = new StringBuilder();
		sql.append("select p.idpersona, count(*) as pedidos, sum(p.base) as base")
			.append(" from pizzaamericana.bono_pedido p")
			.append(" where p.idbono = ? and p.idemision is null");
		if (!c.abierta) {
			//Solo los invitados. Y solo a quienes el mensaje les LLEGO: si el
			//correo reboto, esa persona nunca supo que comprando se ganaba algo,
			//asi que no se le puede reclamar ni ella puede reclamar.
			sql.append(" and exists (select 1 from crm.campana_destinatario d")
				.append("              where d.idenvio = ").append(c.idEnvio)
				.append("                and d.idpersona = p.idpersona")
				.append("                and d.estado = 'ENVIADO')");
		}
		if (!c.repetible) {
			sql.append(" and not exists (select 1 from pizzaamericana.bono_emitido e")
				.append("                 where e.idbono = p.idbono and e.idpersona = p.idpersona)");
		}
		sql.append(" group by p.idpersona having sum(p.base) >= ?");

		try (PreparedStatement ps = cn.prepareStatement(sql.toString())) {
			ps.setInt(1, c.idBono);
			ps.setDouble(2, c.baseMinima);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final Emision e = new Emision();
					e.idPersona = rs.getLong("idpersona");
					e.pedidos = rs.getInt("pedidos");
					e.base = rs.getDouble("base");
					//Se redondea a pesos: un bono de $8.433,7 no existe.
					double valor = Math.floor(e.base * c.porcentaje / 100.0);
					if (c.topeBono > 0 && valor > c.topeBono) {
						valor = c.topeBono;
						e.topado = true;
					}
					e.valor = valor;
					if (valor > 0) {
						lista.add(e);
					}
				}
			}
		}
		return (lista);
	}

	/**
	 * Emite el bono de una persona y deja amarrados los pedidos que lo pagaron.
	 *
	 * Todo en una transaccion. Si se emitiera el codigo y fallara el amarre, la
	 * noche siguiente esos mismos pedidos volverian a pagar otro bono.
	 */
	private static boolean emitirUno(final Connection cn, final Campana c, final Emision e,
			final String usuario) {
		try {
			cn.setAutoCommit(false);

			e.idCliente = CodigoPromoDAO.clienteDePersona(e.idPersona, "");
			if (e.idCliente <= 0) {
				guardarEmision(cn, c, e, "FALLIDO", "La persona no tiene una fila de cliente en el central.");
				cn.commit();
				cn.setAutoCommit(true);
				return (false);
			}

			final String concepto = "Bono de recompra " + c.nombre + " (" + e.pedidos
					+ " pedidos, base " + Math.round(e.base) + ")";
			//Si la campana dice hasta cuando se redime, esa es la caducidad. Con
			//los dias de la oferta, emitir un dia mas tarde correria el
			//vencimiento y el mensaje que ya se le mando al cliente -"del 5 al
			//7"- quedaria mintiendo.
			final String vence = c.redimeHasta.length() >= 10 ? c.redimeHasta.substring(0, 10) : null;
			final CodigoPromoDAO.Emision emitida = CodigoPromoDAO.emitirValor(cn, c.idOferta,
					e.idCliente, e.valor, concepto, usuario, vence);
			if (emitida.error != null && emitida.error.length() > 0) {
				guardarEmision(cn, c, e, "FALLIDO", emitida.error);
				cn.commit();
				cn.setAutoCommit(true);
				return (false);
			}
			e.codigo = emitida.codigo;
			e.fechaCaducidad = emitida.fechaCaducidad;

			final int idEmision = guardarEmision(cn, c, e, "EMITIDO", "");
			e.idEmision = idEmision;

			//Los pedidos que pagaron este bono quedan marcados. Es lo que evita
			//que vuelvan a pagar otro manana.
			try (PreparedStatement ps = cn.prepareStatement(
					"update pizzaamericana.bono_pedido set idemision = ?"
					+ " where idbono = ? and idpersona = ? and idemision is null")) {
				ps.setInt(1, idEmision);
				ps.setInt(2, c.idBono);
				ps.setLong(3, e.idPersona);
				ps.executeUpdate();
			}

			//La oferta emitida queda amarrada a la emision, para poder ir de un
			//lado al otro cuando alguien reclame.
			try (PreparedStatement ps = cn.prepareStatement(
					"update pizzaamericana.bono_emitido set idofertacliente = ? where idemision = ?")) {
				ps.setInt(1, emitida.idOfertaCliente);
				ps.setInt(2, idEmision);
				ps.executeUpdate();
			}

			cn.commit();
			cn.setAutoCommit(true);
			return (true);
		} catch (final Exception ex) {
			try {
				cn.rollback();
				cn.setAutoCommit(true);
			} catch (final Exception e2) {
			}
			Logger.getLogger("log_file").error("BonoRecompraDAO.emitirUno persona " + e.idPersona
					+ ": " + ex.toString());
			return (false);
		}
	}

	private static int guardarEmision(final Connection cn, final Campana c, final Emision e,
			final String estado, final String detalle) throws SQLException {
		int secuencia = 1;
		if (c.repetible) {
			try (PreparedStatement ps = cn.prepareStatement(
					"select ifnull(max(secuencia),0) + 1 from pizzaamericana.bono_emitido"
					+ " where idbono = ? and idpersona = ?")) {
				ps.setInt(1, c.idBono);
				ps.setLong(2, e.idPersona);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						secuencia = rs.getInt(1);
					}
				}
			}
		}
		try (PreparedStatement ps = cn.prepareStatement(
				"insert into pizzaamericana.bono_emitido (idbono, idpersona, secuencia, idcliente,"
				+ " pedidos, base, valor, topado, codigo, fecha_caducidad, estado, detalle, calculado_en)"
				+ " values (?,?,?,?,?,?,?,?,?,?,?,?,now())", Statement.RETURN_GENERATED_KEYS)) {
			ps.setInt(1, c.idBono);
			ps.setLong(2, e.idPersona);
			ps.setInt(3, secuencia);
			ps.setInt(4, e.idCliente);
			ps.setInt(5, e.pedidos);
			ps.setDouble(6, e.base);
			ps.setDouble(7, e.valor);
			ps.setString(8, e.topado ? "S" : "N");
			ps.setString(9, e.codigo);
			if (e.fechaCaducidad != null && e.fechaCaducidad.length() >= 10) {
				ps.setString(10, e.fechaCaducidad.substring(0, 10));
			} else {
				ps.setNull(10, java.sql.Types.DATE);
			}
			ps.setString(11, estado);
			ps.setString(12, detalle.length() > 300 ? detalle.substring(0, 300) : detalle);
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return (rs.getInt(1));
				}
			}
		}
		return (0);
	}

	// =======================================================================
	// Para la pantalla
	// =======================================================================

	/** Lo emitido de una campana, para revisar y para atender reclamos. */
	public static ArrayList<Emision> emisiones(final int idBono, final int cuantas) {
		final ArrayList<Emision> lista = new ArrayList<Emision>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"select e.*, ifnull(r.email,'') as correo, ifnull(r.celular,'') as celular"
					+ " from pizzaamericana.bono_emitido e"
					+ " left join crm.persona_resumen r on r.idpersona = e.idpersona"
					+ " where e.idbono = ? order by e.valor desc limit ?");
			ps.setInt(1, idBono);
			ps.setInt(2, cuantas);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Emision e = new Emision();
				e.idEmision = rs.getInt("idemision");
				e.idPersona = rs.getLong("idpersona");
				e.idCliente = rs.getInt("idcliente");
				e.pedidos = rs.getInt("pedidos");
				e.base = rs.getDouble("base");
				e.valor = rs.getDouble("valor");
				e.topado = "S".equals(rs.getString("topado"));
				e.codigo = txt(rs.getString("codigo"));
				e.fechaCaducidad = txt(rs.getString("fecha_caducidad"));
				e.estado = txt(rs.getString("estado"));
				e.detalle = txt(rs.getString("detalle"));
				e.destino = txt(rs.getString("correo"));
				lista.add(e);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO.emisiones: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// =======================================================================
	// Plomeria
	// =======================================================================

	private static Campana leer(final ResultSet rs) throws SQLException {
		final Campana c = new Campana();
		c.idBono = rs.getInt("idbono");
		c.nombre = txt(rs.getString("nombre"));
		c.idOferta = rs.getInt("idoferta");
		c.compraDesde = txt(rs.getString("compra_desde"));
		c.compraHasta = txt(rs.getString("compra_hasta"));
		c.porcentaje = rs.getDouble("porcentaje");
		c.topeBono = rs.getDouble("tope_bono");
		c.baseMinima = rs.getDouble("base_minima");
		c.productos = txt(rs.getString("productos"));
		c.excluirPromociones = "S".equals(rs.getString("excluir_promociones"));
		c.repetible = "S".equals(rs.getString("repetible"));
		c.estado = txt(rs.getString("estado"));
		c.emitir = "S".equals(rs.getString("emitir"));
		c.avisar = "S".equals(rs.getString("avisar"));
		c.abierta = !"N".equals(rs.getString("abierta"));
		c.idEnvio = rs.getLong("idenvio");
		c.fechaEmision = txt(rs.getString("fecha_emision"));
		c.redimeDesde = txt(rs.getString("redime_desde"));
		c.redimeHasta = txt(rs.getString("redime_hasta"));
		c.usuario = txt(rs.getString("usuario"));
		c.creadaEn = txt(rs.getString("creada_en"));
		c.ultimoCalculoEn = txt(rs.getString("ultimo_calculo_en"));
		c.personasConPedidos = rs.getInt("con_pedidos");
		c.emitidos = rs.getInt("emitidos");
		c.valorEmitido = rs.getDouble("valor_emitido");
		return (c);
	}

	private static String txt(final String v) {
		return (v == null ? "" : v);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("BonoRecompraDAO: no cerro la conexion, " + e.toString());
		}
	}
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Compensaciones por tiempo: los pedidos a domicilio que se demoraron, y a quienes de ellos se les
 * pidio disculpas con una oferta.
 *
 * DE DONDE SALE CADA COSA
 *
 * El tiempo REAL de entrega solo existe en la base de cada tienda: despacho_real_det.hora_entrega. El
 * datamart la trae del dia anterior (ServicioReplicaPedidos corre de madrugada), y de ahi lee esta
 * clase, que es lo que deja ver las once tiendas juntas sin llamar a ninguna. Por eso la pantalla solo
 * deja consultar hasta AYER.
 *
 * El tiempo que cuenta es el mismo de la pantalla de desempeno de domiciliarios: desde que entro el
 * pedido (pedido.fechainsercion) hasta que se entrego, contra lo que se le prometio al cliente
 * (pedido.tiempopedido). Se dejan por fuera los anulados y los programados: el cliente de un programado
 * pidio una hora concreta, no "lo mas rapido", y compararlo contra la promesa de un pedido inmediato
 * mezclaria dos promesas distintas.
 *
 * A QUIEN SE LE ESCRIBE
 *
 * El pedido de tienda trae el cliente de ESA tienda (pedido.idcliente). El maestro del CRM sabe a que
 * persona corresponde: crm.stage_cliente_tienda (idtienda, idcliente) -> idpersona, y de ahi
 * crm.persona_resumen tiene el correo y la autorizacion de datos. Un cliente que el maestro todavia no
 * conoce no aparece como "puede recibir": no se adivina a quien escribirle.
 *
 * El ultimo filtro -que no haya rebotado y que no haya dicho "no mas publicidad"- lo aplica al cargar
 * la tanda CampanaDAO.cargarDestinatariosDePersonas, con las mismas reglas de cualquier envio.
 */
public class CompensacionTiempoDAO {

	/** Tope de filas que se devuelven: un dia de las once tiendas no se acerca, y la tabla no es para mas. */
	private static final int MAXIMO_FILAS = 1000;

	/** Tiendas que no hacen domicilios y no se listan como "sin datos". */
	private static final String TIENDAS_NO_OPERATIVAS = "('Bodega','Contact Center','Tienda pruebas')";

	public static class Pedido {
		public int idTienda;
		public String tienda = "";
		public int idPedidoTienda;
		public String fecha = "";
		public String horaPedido = "";
		public String horaEntrega = "";
		public int prometido;
		public int minutos;
		public long idPersona;
		public String cliente = "";
		public String email = "";
		/** Se le puede escribir segun lo que se ve aqui (el filtro final lo hace el envio). */
		public boolean puede;
		/** Si no puede: por que. Si ya fue compensado, dice con cual envio. */
		public String motivo = "";
		public boolean yaCompensado;
		public long idEnvioPrevio;

		public int exceso() {
			return (prometido > 0 ? minutos - prometido : 0);
		}
	}

	public static class Resultado {
		public ArrayList<Pedido> pedidos = new ArrayList<Pedido>();
		public String error = "";
		/** Tiendas que no tienen ese dia en el datamart: para que la lista no parezca completa sin serlo. */
		public ArrayList<String> tiendasSinDatos = new ArrayList<String>();
		/** true si la consulta llego al tope de filas. */
		public boolean truncado;
	}

	private CompensacionTiempoDAO() {
	}

	/**
	 * Los pedidos entregados ese dia que se demoraron al menos esos minutos, de todas las tiendas o de
	 * una.
	 *
	 * @param fecha       aaaa-mm-dd; tiene que ser anterior a hoy
	 * @param minutosMin  minutos totales, de que entro el pedido a que se entrego, a partir de los
	 *                    cuales se lista
	 * @param idTienda    0 para todas
	 */
	public static Resultado listar(final String fecha, final int minutosMin, final int idTienda,
			final boolean soloPasaronPromesa) {
		final Resultado res = new Resultado();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			//El datamart se llena de madrugada con el dia anterior: hoy no hay nada que mirar todavia.
			final PreparedStatement psHoy = cn.prepareStatement("SELECT ? < CURDATE()");
			psHoy.setString(1, fecha);
			final ResultSet rsHoy = psHoy.executeQuery();
			final boolean esPasada = rsHoy.next() && rsHoy.getInt(1) == 1;
			rsHoy.close();
			psHoy.close();
			if (!esPasada) {
				res.error = "Solo se pueden consultar dias anteriores a hoy: el datamart se llena de madrugada "
						+ "con lo de ayer.";
				return (res);
			}

			//Las columnas obligatorias. Si el datamart no las tiene, se dice cual falta en vez de fallar
			//con un error de SQL que no le dice nada a quien opera.
			final String[] obligatorias = { "idpedidotienda", "idcliente", "fechapedido", "fechainsercion",
					"tiempopedido" };
			for (int i = 0; i < obligatorias.length; i++) {
				if (!tieneColumna(cn, "datamart", "pedido", obligatorias[i])) {
					res.error = "El datamart no tiene la columna pedido." + obligatorias[i]
							+ ": sin ella no se puede cruzar el cliente. Revise la replica de pedidos.";
					return (res);
				}
			}
			//Las opcionales: sin ellas se consulta igual, y la pantalla lo avisa.
			final boolean conAnulacion = tieneColumna(cn, "datamart", "pedido", "idmotivoanulacion");
			final boolean conProgramado = tieneColumna(cn, "datamart", "pedido", "programado");

			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT p.idtienda, IFNULL(t.nombre,'') AS tienda, p.idpedidotienda,")
				.append(" DATE_FORMAT(p.fechainsercion,'%H:%i') AS hora_pedido,")
				.append(" DATE_FORMAT(MAX(d.hora_entrega),'%H:%i') AS hora_entrega,")
				.append(" IFNULL(p.tiempopedido,0) AS prometido,")
				.append(" TIMESTAMPDIFF(MINUTE, p.fechainsercion, MAX(d.hora_entrega)) AS minutos,")
				.append(" IFNULL(s.idpersona,0) AS idpersona,")
				.append(" TRIM(CONCAT(IFNULL(s.nombre,''),' ',IFNULL(s.apellido,''))) AS cliente,")
				.append(" IFNULL(TRIM(pr.email),'') AS email, IFNULL(pr.politica_datos,'') AS politica,")
				//Un destinatario cuyo correo FALLO no cuenta como compensado: se puede volver a intentar.
				.append(" IF(cd.estado = 'FALLIDO', NULL, c.idenvio) AS idenvio_previo")
				.append(" FROM datamart.pedido p")
				.append(" JOIN datamart.despacho_real_det d")
				.append("   ON d.id_pedido = p.idpedidotienda AND d.idtienda = p.idtienda")
				.append(" LEFT JOIN pizzaamericana.tienda t ON t.idtienda = p.idtienda")
				.append(" LEFT JOIN crm.stage_cliente_tienda s")
				.append("   ON s.idtienda = p.idtienda AND s.idcliente = p.idcliente")
				.append(" LEFT JOIN crm.persona_resumen pr ON pr.idpersona = s.idpersona")
				.append(" LEFT JOIN crm.compensacion_tiempo c")
				.append("   ON c.idtienda = p.idtienda AND c.idpedidotienda = p.idpedidotienda")
				.append(" LEFT JOIN crm.campana_destinatario cd")
				.append("   ON cd.idenvio = c.idenvio AND cd.idpersona = c.idpersona")
				.append(" WHERE p.fechapedido = ? AND d.hora_entrega IS NOT NULL")
				.append("   AND IFNULL(p.tiempopedido,0) > 0");
			if (conAnulacion) {
				sql.append(" AND p.idmotivoanulacion IS NULL");
			}
			if (conProgramado) {
				sql.append(" AND IFNULL(p.programado,'N') <> 'S'");
			}
			if (idTienda > 0) {
				sql.append(" AND p.idtienda = ?");
			}
			sql.append(" GROUP BY p.idtienda, t.nombre, p.idpedidotienda, p.fechainsercion, p.tiempopedido,")
				.append(" s.idpersona, s.nombre, s.apellido, pr.email, pr.politica_datos, c.idenvio, cd.estado")
				.append(" HAVING minutos >= ?")
				.append(soloPasaronPromesa ? " AND minutos > prometido" : "")
				.append(" ORDER BY minutos DESC, p.idtienda, p.idpedidotienda")
				.append(" LIMIT ").append(MAXIMO_FILAS + 1);

			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			int k = 1;
			ps.setString(k++, fecha);
			if (idTienda > 0) {
				ps.setInt(k++, idTienda);
			}
			ps.setInt(k++, minutosMin);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (res.pedidos.size() >= MAXIMO_FILAS) {
					res.truncado = true;
					break;
				}
				final Pedido p = new Pedido();
				p.idTienda = rs.getInt("idtienda");
				p.tienda = rs.getString("tienda");
				p.idPedidoTienda = rs.getInt("idpedidotienda");
				p.fecha = fecha;
				p.horaPedido = rs.getString("hora_pedido");
				p.horaEntrega = rs.getString("hora_entrega");
				p.prometido = rs.getInt("prometido");
				p.minutos = rs.getInt("minutos");
				p.idPersona = rs.getLong("idpersona");
				p.cliente = rs.getString("cliente");
				p.email = rs.getString("email");
				final long previo = rs.getLong("idenvio_previo");
				final boolean sinPrevio = rs.wasNull();
				final String politica = rs.getString("politica");
				if (!sinPrevio && previo > 0) {
					p.yaCompensado = true;
					p.idEnvioPrevio = previo;
					p.motivo = "Ya se le compenso (envio " + previo + ")";
				} else if (p.idPersona <= 0) {
					p.motivo = "El cliente no esta en el maestro del CRM";
				} else if (p.email.length() == 0 || !p.email.matches(CampanaDAO.PATRON_CORREO)) {
					p.motivo = "No tiene un correo valido";
				} else if (!"S".equals(politica)) {
					p.motivo = "No autorizo el uso de sus datos";
				} else {
					p.puede = true;
				}
				res.pedidos.add(p);
			}
			rs.close();
			ps.close();

			res.tiendasSinDatos = tiendasSinDatos(cn, fecha, idTienda);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CompensacionTiempoDAO.listar: " + e.toString());
			res.error = "No se pudo consultar: " + e.getMessage();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/** Las tiendas operativas que no tienen despachos de ese dia en el datamart. */
	private static ArrayList<String> tiendasSinDatos(final Connection cn, final String fecha, final int idTienda) {
		final ArrayList<String> lista = new ArrayList<String>();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT t.nombre FROM pizzaamericana.tienda t"
					+ " WHERE t.nombre NOT IN " + TIENDAS_NO_OPERATIVAS
					+ (idTienda > 0 ? " AND t.idtienda = ?" : "")
					+ "   AND NOT EXISTS (SELECT 1 FROM datamart.despacho_real r"
					+ "                    WHERE r.idtienda = t.idtienda AND r.fecha = ?)"
					+ " ORDER BY t.nombre");
			int k = 1;
			if (idTienda > 0) {
				ps.setInt(k++, idTienda);
			}
			ps.setString(k++, fecha);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CompensacionTiempoDAO.tiendasSinDatos: " + e.toString());
		}
		return (lista);
	}

	private static boolean tieneColumna(final Connection cn, final String esquema, final String tabla,
			final String columna) throws java.sql.SQLException {
		final PreparedStatement ps = cn.prepareStatement(
				"SELECT COUNT(*) FROM information_schema.columns"
				+ " WHERE table_schema = ? AND table_name = ? AND column_name = ?");
		ps.setString(1, esquema);
		ps.setString(2, tabla);
		ps.setString(3, columna);
		final ResultSet rs = ps.executeQuery();
		final boolean existe = rs.next() && rs.getInt(1) > 0;
		rs.close();
		ps.close();
		return (existe);
	}

	// =======================================================================
	// El libro de compensaciones
	// =======================================================================

	/**
	 * Deja asentado que estos pedidos quedaron en la tanda. Si un pedido ya estaba (su correo anterior
	 * fallo) se vuelve a apuntar a la tanda nueva.
	 *
	 * @param personasEnTanda solo se asientan los pedidos de quien de verdad quedo cargado: quien fue
	 *                        filtrado por consentimiento o por rebote no recibio nada, y marcarlo
	 *                        "compensado" lo sacaria de la lista sin haberle escrito.
	 * @return cuantos pedidos se asentaron
	 */
	public static int registrar(final long idEnvio, final int idOferta, final String usuario,
			final List<Pedido> pedidos, final Set<Long> personasEnTanda) {
		int n = 0;
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO crm.compensacion_tiempo (idtienda, idpedidotienda, fecha_pedido, idpersona,"
					+ " minutos, prometido, idoferta, idenvio, usuario, creado_en)"
					+ " VALUES (?,?,?,?,?,?,?,?,?, NOW())"
					+ " ON DUPLICATE KEY UPDATE idenvio = VALUES(idenvio), idoferta = VALUES(idoferta),"
					+ " minutos = VALUES(minutos), usuario = VALUES(usuario), creado_en = NOW()");
			for (final Pedido p : pedidos) {
				if (!personasEnTanda.contains(Long.valueOf(p.idPersona))) {
					continue;
				}
				ps.setInt(1, p.idTienda);
				ps.setInt(2, p.idPedidoTienda);
				ps.setString(3, p.fecha);
				ps.setLong(4, p.idPersona);
				ps.setInt(5, p.minutos);
				ps.setInt(6, p.prometido);
				ps.setInt(7, idOferta);
				ps.setLong(8, idEnvio);
				ps.setString(9, usuario);
				ps.executeUpdate();
				n++;
			}
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CompensacionTiempoDAO.registrar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (n);
	}

	/** Las personas que de verdad quedaron cargadas en la tanda. */
	public static Set<Long> personasDeEnvio(final long idEnvio) {
		final Set<Long> ids = new HashSet<Long>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idpersona FROM crm.campana_destinatario WHERE idenvio = ?");
			ps.setLong(1, idEnvio);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(Long.valueOf(rs.getLong(1)));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CompensacionTiempoDAO.personasDeEnvio: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (ids);
	}

	/**
	 * La fecha (aaaa-mm-dd) del pedido por el que se le pide disculpas a esta persona en esta tanda, o ""
	 * si esta tanda no es de compensaciones. Es lo que decide, al armar cada correo, si sale el de
	 * disculpa o el de oferta de siempre.
	 */
	public static String fechaPedido(final long idEnvio, final long idPersona) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT DATE_FORMAT(MIN(fecha_pedido),'%Y-%m-%d') FROM crm.compensacion_tiempo"
					+ " WHERE idenvio = ? AND idpersona = ?");
			ps.setLong(1, idEnvio);
			ps.setLong(2, idPersona);
			final ResultSet rs = ps.executeQuery();
			String fecha = "";
			if (rs.next() && rs.getString(1) != null) {
				fecha = rs.getString(1);
			}
			rs.close();
			ps.close();
			return (fecha);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CompensacionTiempoDAO.fechaPedido: " + e.toString());
			return ("");
		} finally {
			cerrar(cn);
		}
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CompensacionTiempoDAO: no cerro la conexion, " + e.toString());
		}
	}
}

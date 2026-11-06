package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * SOMBRA EN VIVO del motor de pedido con el BOT (etapa 2, paquete 2.9).
 *
 * Cuando el BOT confirma un pedido (ConfirmarPedidoCRMBOT), este gancho calcula en silencio lo que habria
 * dado el motor nuevo y lo guarda en pos_central.sombra_vivo, al lado de la cotizacion del flujo viejo.
 *
 * GARANTIAS (el flujo del BOT no debe notar que esto existe):
 *  - Apagado por defecto. Interruptor: general.parametros, MOTORSOMBRABOT, valortexto 'S' o 'N'. Se lee como
 *    maximo cada 30 segundos y se corta solo si no se puede leer (ante la duda, apagado).
 *  - No cambia la respuesta al BOT ni escribe nada fuera de pos_central.sombra_vivo.
 *  - No corre en el hilo del BOT: usa un solo hilo aparte con cola de 50. Si la cola se llena, descarta.
 *  - Nunca lanza excepciones hacia afuera.
 *  - Se apaga solo tras 20 errores seguidos, hasta que se reinicie el servidor, y lo dice en el log.
 *  - Solo LEE el catalogo (pos_central) y las equivalencias; no envia nada a ninguna tienda.
 */
public class MotorSombraBot {

	public static final String PARAMETRO = "MOTORSOMBRABOT";
	private static final long VIGENCIA_INTERRUPTOR_MS = 30_000L;
	private static final long VIGENCIA_CATALOGO_MS = 10 * 60_000L;
	private static final int ERRORES_PARA_APAGAR = 20;
	private static final Gson GSON = new Gson();

	private static final ThreadPoolExecutor EJECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(50), r -> {
				Thread t = new Thread(r, "motor-sombra-bot");
				t.setDaemon(true);
				return t;
			}, new ThreadPoolExecutor.DiscardPolicy());

	private static volatile boolean interruptor = false;
	private static volatile long interruptorLeidoEn = 0L;
	private static final AtomicInteger erroresSeguidos = new AtomicInteger(0);
	private static volatile boolean autoApagado = false;

	private static final Object CANDADO = new Object();
	private static CatalogoPos catalogo;
	private static TraductorBot traductor;
	private static MotorPedidoV1 motor;
	private static long catalogoCargadoEn = 0L;

	private MotorSombraBot() {
	}

	/**
	 * Punto de entrada desde el flujo del BOT. Retorna de inmediato.
	 *
	 * @param leadId         lead de Kommo
	 * @param idLog          fila de log_pedido_crmbot de esta confirmacion
	 * @param infoLead       JSON del lead tal como se guarda en log_pedido_crmbot.info_lead
	 * @param subtotalViejo  subtotalProductos de la cotizacion del flujo viejo (sin domicilio)
	 */
	public static void registrar(final long leadId, final int idLog, final String infoLead, final double subtotalViejo) {
		try {
			if (autoApagado || !interruptorActivo()) {
				return;
			}
			EJECUTOR.execute(new Runnable() {
				@Override
				public void run() {
					Connection con = null;
					try {
						con = new conexionCC.ConexionBaseDatos().obtenerConexionBDPrincipal();
						registrarAhora(con, leadId, idLog, infoLead, subtotalViejo);
						erroresSeguidos.set(0);
					} catch (Throwable t) {
						int n = erroresSeguidos.incrementAndGet();
						System.out.println("MOTOR SOMBRA: error " + n + " seguido(s): " + t);
						if (n >= ERRORES_PARA_APAGAR) {
							autoApagado = true;
							System.out.println("MOTOR SOMBRA: APAGADO SOLO tras " + n + " errores seguidos. Reiniciar el servidor para volver a encenderlo.");
						}
					} finally {
						try {
							if (con != null) {
								con.close();
							}
						} catch (Exception ignorada) {
						}
					}
				}
			});
		} catch (Throwable t) {
			// Nada de lo que pase aqui puede afectar al BOT.
		}
	}

	/** Lee el interruptor (cada 30 segundos como maximo). Ante cualquier problema devuelve falso. */
	static boolean interruptorActivo() {
		long ahora = System.currentTimeMillis();
		if (ahora - interruptorLeidoEn < VIGENCIA_INTERRUPTOR_MS) {
			return interruptor;
		}
		Connection con = null;
		boolean valor = false;
		try {
			con = new conexionCC.ConexionBaseDatos().obtenerConexionBDGeneral();
			try (PreparedStatement ps = con.prepareStatement("select valortexto from parametros where valorparametro = ?")) {
				ps.setString(1, PARAMETRO);
				try (ResultSet rs = ps.executeQuery()) {
					valor = rs.next() && "S".equalsIgnoreCase(String.valueOf(rs.getString(1)).trim());
				}
			}
		} catch (Throwable t) {
			valor = false;
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception ignorada) {
			}
		}
		interruptor = valor;
		interruptorLeidoEn = ahora;
		return valor;
	}

	/** Prepara (o refresca) el catalogo, el traductor y el motor. */
	private static void preparar(Connection con) throws Exception {
		synchronized (CANDADO) {
			if (motor == null || System.currentTimeMillis() - catalogoCargadoEn > VIGENCIA_CATALOGO_MS) {
				CatalogoPos c = CatalogoPos.cargar(con);
				TraductorBot t = new TraductorBot(con, c);
				catalogo = c;
				traductor = t;
				motor = new MotorPedidoV1(c);
				catalogoCargadoEn = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Hace el trabajo, sin interruptor ni hilo: lo usan el gancho (en su hilo) y las pruebas. Escribe una fila en
	 * pos_central.sombra_vivo.
	 */
	public static void registrarAhora(Connection con, long leadId, int idLog, String infoLead, Double subtotalViejo)
			throws Exception {
		long ini = System.currentTimeMillis();
		preparar(con);
		MotorSolicitud sol = traductor.traducir(infoLead, traductor.resolverTienda(infoLead));
		sol.contexto.fechaHora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		MotorResultado res = motor.cotizar(sol);
		double domicilio = 0;
		for (MotorResultado.Linea l : res.lineas) {
			if ("DOMICILIO".equals(l.origen)) {
				domicilio += l.valorTotal;
			}
		}
		Double subMotor = MotorResultado.OK.equals(res.estado) ? Double.valueOf(res.subtotal - domicilio) : null;
		String sql = "insert into pos_central.sombra_vivo (lead_id, idlog_bot, version_motor, estado_motor, avisos_traduccion, "
				+ "subtotal_motor, subtotal_viejo, diferencia_vs_viejo, duracion_ms, solicitud_json, resultado_json) "
				+ "values (?,?,?,?,?,?,?,?,?,?,?)";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setLong(1, leadId);
			ps.setInt(2, idLog);
			ps.setString(3, motor.version());
			ps.setString(4, res.estado);
			ps.setInt(5, sol.avisosTraduccion.size());
			if (subMotor == null) {
				ps.setNull(6, java.sql.Types.DOUBLE);
			} else {
				ps.setDouble(6, subMotor);
			}
			if (subtotalViejo == null) {
				ps.setNull(7, java.sql.Types.DOUBLE);
			} else {
				ps.setDouble(7, subtotalViejo);
			}
			if (subMotor == null || subtotalViejo == null) {
				ps.setNull(8, java.sql.Types.DOUBLE);
			} else {
				ps.setDouble(8, Math.round((subMotor - subtotalViejo) * 100.0) / 100.0);
			}
			ps.setInt(9, (int) (System.currentTimeMillis() - ini));
			ps.setString(10, GSON.toJson(sol));
			ps.setString(11, GSON.toJson(res));
			ps.executeUpdate();
		}
	}

	/** Cuantas filas lleva la sombra y como van contra el flujo viejo. Para el informe. */
	public static String resumenRapido(Connection con) throws Exception {
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select count(*), sum(estado_motor = 'OK'), sum(abs(ifnull(diferencia_vs_viejo, 0)) < 1) "
						+ "from pos_central.sombra_vivo")) {
			rs.next();
			return "sombra_vivo: " + rs.getInt(1) + " filas, " + rs.getInt(2) + " con motor OK, " + rs.getInt(3)
					+ " iguales a la cotizacion vieja";
		}
	}
}

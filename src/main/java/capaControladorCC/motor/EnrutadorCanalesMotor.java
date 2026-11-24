package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;

import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;
import conexionCC.ConexionBaseDatos;

/**
 * Enrutador Multicanal del Motor de Pedidos (Etapa 3 / Piloto).
 *
 * Permite controlar el modo de ejecucion por canal (Kuno, App, Bot, Rappi, DiDi, Contact):
 *  - LEGACY  : Flujo tradicional 100% inalterado (valor por defecto).
 *  - PARALELO: Flujo tradicional procesa y responde de forma sincrona; el Nuevo Motor
 *              se ejecuta de forma ASINCRONA en segundo plano para comparar totales,
 *              lineas y reglas de catalogo sin agregar latencia al webhook/cliente.
 *  - NUEVO   : El Nuevo Motor asume el pedido directamente, con fallback automatico al
 *              flujo tradicional si llega a presentarse cualquier excepcion imprevista.
 *
 * Parametros en general.parametros:
 *  - MODO_MOTOR_GLOBAL : 'LEGACY', 'PARALELO' o 'NUEVO'.
 *  - MODO_MOTOR_<CANAL>: Sobrescritura opcional por canal (ej. MODO_MOTOR_KUNO).
 *  - KUNO_NUEVO_MOTOR  : 'N' (Legacy), 'P' (Paralelo), 'S' (Nuevo) por retrocompatibilidad.
 *  - MOTOR_PILOTO_TIENDAS: Lista de tiendas piloto (ej. '11' o vacio para todas).
 */
public class EnrutadorCanalesMotor {

	private static final Logger logger = Logger.getLogger("log_file");
	private static final Gson GSON = new Gson();
	private static final long VIGENCIA_CACHE_MS = 30_000L; // 30 segundos cache

	public enum ModoMotor {
		LEGACY,
		PARALELO,
		NUEVO
	}

	// Pool de hilos asincrono para ejecucion en paralelo sin demorar la respuesta
	private static final ThreadPoolExecutor EJECUTOR_ASINCRONO = new ThreadPoolExecutor(
			2, 4, 60L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(150),
			r -> {
				Thread t = new Thread(r, "motor-pedido-paralelo");
				t.setDaemon(true);
				return t;
			},
			new ThreadPoolExecutor.DiscardPolicy()
	);

	// Estado cacheado
	private static volatile long ultimaLecturaCache = 0L;
	private static volatile String modoGlobalCache = "LEGACY";
	private static volatile Map<String, String> modosPorCanalCache = new HashMap<String, String>();
	private static volatile Set<Integer> tiendasPilotoCache = new HashSet<Integer>();

	private EnrutadorCanalesMotor() {
	}

	/**
	 * Determina el modo de ejecucion para un canal y tienda dados.
	 */
	public static ModoMotor obtenerModo(Connection con, String canal, int idTienda) {
		refrescarCacheSiEsNecesario(con);

		// 1. Validar si la tienda esta incluida en el piloto
		if (!tiendasPilotoCache.isEmpty() && idTienda > 0 && !tiendasPilotoCache.contains(idTienda)) {
			return ModoMotor.LEGACY;
		}

		// 2. Determinar valor de configuracion (Canal especifico -> Global -> KUNO_NUEVO_MOTOR)
		String canalNorm = canal != null ? canal.trim().toUpperCase() : "GENERAL";
		String valorModo = modosPorCanalCache.get(canalNorm);

		if (valorModo == null || valorModo.trim().isEmpty()) {
			valorModo = modoGlobalCache;
		}

		if (valorModo == null || valorModo.trim().isEmpty()) {
			valorModo = "LEGACY";
		}

		valorModo = valorModo.trim().toUpperCase();
		if ("NUEVO".equals(valorModo) || "S".equals(valorModo)) {
			return ModoMotor.NUEVO;
		} else if ("PARALELO".equals(valorModo) || "P".equals(valorModo) || "AMBOS".equals(valorModo) || "A".equals(valorModo)) {
			return ModoMotor.PARALELO;
		} else {
			return ModoMotor.LEGACY;
		}
	}

	/**
	 * Verifica si el modo nuevo esta activo de forma exclusiva.
	 */
	public static boolean esModoNuevo(Connection con, String canal, int idTienda) {
		return obtenerModo(con, canal, idTienda) == ModoMotor.NUEVO;
	}

	/**
	 * Verifica si debe ejecutarse en paralelo (flujo tradicional + nuevo en sombra).
	 */
	public static boolean esModoParalelo(Connection con, String canal, int idTienda) {
		return obtenerModo(con, canal, idTienda) == ModoMotor.PARALELO;
	}

	/**
	 * Despacha de forma ASINCRONA la evaluacion y registro comparativo del Nuevo Motor.
	 * Se ejecuta en un hilo secundario independiente; si el hilo o la BD fallan,
	 * no interrumpe jamas el flujo principal.
	 */
	public static void registrarParaleloAsincrono(
			final String canal,
			final int idPedidoCentral,
			final String idOrdenComercio,
			final int idTienda,
			final double subtotalViejo,
			final double totalViejo,
			final MotorSolicitud solicitud) {

		if (solicitud == null) {
			return;
		}

		EJECUTOR_ASINCRONO.execute(() -> {
			Connection con = null;
			try {
				con = new ConexionBaseDatos().obtenerConexionBDPrincipal();
				registrarParaleloSincrono(con, canal, idPedidoCentral, idOrdenComercio, idTienda, subtotalViejo, totalViejo, solicitud);
			} catch (Throwable t) {
				logger.error("Error en ejecucion asincrona paralelo nuevo motor: " + t.getMessage(), t);
			} finally {
				if (con != null) {
					try {
						con.close();
					} catch (Exception ignorada) {
					}
				}
			}
		});
	}

	/**
	 * Ejecuta la cotizacion y registro comparativo en sombra dentro de la conexion provista.
	 */
	public static void registrarParaleloSincrono(
			Connection con,
			final String canal,
			final int idPedidoCentral,
			final String idOrdenComercio,
			final int idTienda,
			final double subtotalViejo,
			final double totalViejo,
			final MotorSolicitud solicitud) {

		if (solicitud == null || con == null) {
			return;
		}

		long ini = System.currentTimeMillis();
		try {
			CatalogoPos cat = CatalogoPos.cargar(con);
			MotorPedidoV1 motor = new MotorPedidoV1(cat);
			MotorResultado res = motor.cotizar(solicitud);

			double subMotor = MotorResultado.OK.equals(res.estado) ? res.subtotal : 0.0;
			double totMotor = MotorResultado.OK.equals(res.estado) ? res.total : 0.0;
			double dif = Math.round((totMotor - totalViejo) * 100.0) / 100.0;
			String estadoComp = MotorResultado.OK.equals(res.estado)
					? (Math.abs(dif) < 1.0 ? "OK" : "DIFERENCIA")
					: "ERROR";

			int duracion = (int) (System.currentTimeMillis() - ini);

			String sql = "insert into pos_central.sombra_pedido_canal "
					+ "(canal, idpedido_central, idorden_comercio, idtienda, modo_ejecucion, "
					+ "version_motor, estado_motor, subtotal_viejo, total_viejo, subtotal_motor, "
					+ "total_motor, diferencia, avisos_traduccion, duracion_ms, solicitud_json, resultado_json) "
					+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			try (PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setString(1, canal != null ? canal.toUpperCase() : "GENERAL");
				ps.setInt(2, idPedidoCentral);
				ps.setString(3, idOrdenComercio != null ? idOrdenComercio : "");
				ps.setInt(4, idTienda);
				ps.setString(5, "PARALELO");
				ps.setString(6, motor.version());
				ps.setString(7, estadoComp);
				ps.setDouble(8, subtotalViejo);
				ps.setDouble(9, totalViejo);
				ps.setDouble(10, subMotor);
				ps.setDouble(11, totMotor);
				ps.setDouble(12, dif);
				ps.setInt(13, solicitud.avisosTraduccion != null ? solicitud.avisosTraduccion.size() : 0);
				ps.setInt(14, duracion);
				ps.setString(15, GSON.toJson(solicitud));
				ps.setString(16, GSON.toJson(res));
				ps.executeUpdate();
			}

		} catch (Throwable t) {
			logger.error("Error en ejecucion sincrona sombra nuevo motor: " + t.getMessage(), t);
		}
	}

	/**
	 * Dispara la auditoria en sombra asincrona para pedidos de RAPPI.
	 */
	public static void auditarRappiParalelo(
			final JSONArray detalleOrdenArray,
			final JSONObject jsonOrder,
			final int idTienda,
			final int idCliente,
			final int idTipoPedido,
			final String programado,
			final String horaProgramado,
			final int idPedidoCreado,
			final long idOrdenComercio,
			final double descuentoPropio,
			final double tarifaServicio,
			final double propina,
			final String marketPlace,
			final double totalLegacy) {

		try {
			EJECUTOR_ASINCRONO.execute(() -> {
				Connection con = null;
				try {
					con = new ConexionBaseDatos().obtenerConexionBDPrincipal();
					CatalogoPos cat = CatalogoPos.cargar(con);
					TraductorRappi traductor = new TraductorRappi(con, cat);
					MotorSolicitud sol = traductor.traducir(
							detalleOrdenArray, jsonOrder, idTienda, idCliente, idTipoPedido,
							programado, horaProgramado, descuentoPropio, tarifaServicio, propina, marketPlace);

					registrarParaleloSincrono(con, "RAPPI", idPedidoCreado, String.valueOf(idOrdenComercio), idTienda,
							totalLegacy, totalLegacy, sol);
				} catch (Throwable t) {
					logger.warn("Advertencia en auditarRappiParalelo: " + t.getMessage());
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (Exception ignorada) {
						}
					}
				}
			});
		} catch (Throwable t) {
			// Jamas interrumpir flujo de produccion
		}
	}

	/**
	 * Dispara la auditoria en sombra asincrona para pedidos de DiDi.
	 */
	public static void auditarDidiParalelo(
			final JSONArray detalleOrdenArray,
			final JSONObject jsonOrderInfo,
			final int idTienda,
			final int idCliente,
			final int idTipoPedido,
			final String programado,
			final String horaProgramado,
			final int idPedidoCreado,
			final long idOrdenComercio,
			final double descuentoPropio,
			final long tipoDelivery,
			final double totalLegacy) {

		try {
			EJECUTOR_ASINCRONO.execute(() -> {
				Connection con = null;
				try {
					con = new ConexionBaseDatos().obtenerConexionBDPrincipal();
					CatalogoPos cat = CatalogoPos.cargar(con);
					TraductorDidi traductor = new TraductorDidi(con, cat);
					MotorSolicitud sol = traductor.traducir(
							detalleOrdenArray, jsonOrderInfo, idTienda, idCliente, idTipoPedido,
							programado, horaProgramado, descuentoPropio, tipoDelivery);

					registrarParaleloSincrono(con, "DIDI", idPedidoCreado, String.valueOf(idOrdenComercio), idTienda,
							totalLegacy, totalLegacy, sol);
				} catch (Throwable t) {
					logger.warn("Advertencia en auditarDidiParalelo: " + t.getMessage());
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (Exception ignorada) {
						}
					}
				}
			});
		} catch (Throwable t) {
			// Jamas interrumpir flujo de produccion
		}
	}

	/**
	 * Refresca la configuracion desde general.parametros con TTL de 30 segundos.
	 */
	private static synchronized void refrescarCacheSiEsNecesario(Connection con) {
		long ahora = System.currentTimeMillis();
		if (ahora - ultimaLecturaCache < VIGENCIA_CACHE_MS) {
			return;
		}

		String sql = "select valorparametro, valortexto from general.parametros where valorparametro in ("
				+ "'MODO_MOTOR_GLOBAL', 'KUNO_NUEVO_MOTOR', 'MODO_MOTOR_KUNO', 'MODO_MOTOR_RAPPI', "
				+ "'MODO_MOTOR_DIDI', 'MODO_MOTOR_BOT', 'MOTOR_PILOTO_TIENDAS')";

		boolean cerroConexion = false;
		Connection conexionALeer = con;
		try {
			if (conexionALeer == null || conexionALeer.isClosed()) {
				conexionALeer = new ConexionBaseDatos().obtenerConexionBDPrincipal();
				cerroConexion = true;
			}

			String nuevoGlobal = "LEGACY";
			Map<String, String> nuevosCanales = new HashMap<String, String>();
			Set<Integer> nuevasTiendas = new HashSet<Integer>();

			try (PreparedStatement ps = conexionALeer.prepareStatement(sql);
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String param = rs.getString(1);
					String val = rs.getString(2);
					if (val == null) {
						continue;
					}
					val = val.trim();

					if ("MODO_MOTOR_GLOBAL".equalsIgnoreCase(param)) {
						nuevoGlobal = val;
					} else if ("KUNO_NUEVO_MOTOR".equalsIgnoreCase(param)) {
						// Si KUNO_NUEVO_MOTOR tiene 'S' o 'P', mapear
						if ("S".equalsIgnoreCase(val)) {
							nuevosCanales.put("KUNO", "NUEVO");
							nuevosCanales.put("APP", "NUEVO");
						} else if ("P".equalsIgnoreCase(val)) {
							nuevosCanales.put("KUNO", "PARALELO");
							nuevosCanales.put("APP", "PARALELO");
						}
					} else if (param.startsWith("MODO_MOTOR_")) {
						String c = param.substring("MODO_MOTOR_".length()).toUpperCase();
						nuevosCanales.put(c, val);
					} else if ("MOTOR_PILOTO_TIENDAS".equalsIgnoreCase(param)) {
						if (!val.isEmpty() && !"TODAS".equalsIgnoreCase(val) && !"ALL".equalsIgnoreCase(val)) {
							String[] partes = val.split(",");
							for (String p : partes) {
								try {
									int idT = Integer.parseInt(p.trim());
									if (idT > 0) nuevasTiendas.add(idT);
								} catch (NumberFormatException ignorada) {
								}
							}
						}
					}
				}
			}

			modoGlobalCache = nuevoGlobal;
			modosPorCanalCache = nuevosCanales;
			tiendasPilotoCache = nuevasTiendas;
			ultimaLecturaCache = ahora;

		} catch (Throwable t) {
			logger.warn("No se pudo refrescar cache de EnrutadorCanalesMotor: " + t.getMessage());
		} finally {
			if (cerroConexion && conexionALeer != null) {
				try {
					conexionALeer.close();
				} catch (Exception ignorada) {
				}
			}
		}
	}
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import capaModeloCC.ConocimientoBloqueoTienda;
import conexionCC.ConexionBaseDatos;

/**
 * Capa de Acceso a Datos y Motor Analitico para la tabla de
 * Conocimiento Semanal de Bloqueos de Tienda (`pizzaamericana.conocimiento_bloqueo_tienda`).
 *
 * Analiza el estado operacional de la tienda al momento del bloqueo:
 * - Pedidos y pizzas en proceso ("pixeles")
 * - Domiciliarios activos en turno
 * - Ratio de presion operativa (pedidos / domiciliario)
 * - Venta estimada perdida
 * - Diagnostico de causa raiz y sugerencias predictivas
 */
public class ConocimientoBloqueoDAO {

	private static final Logger logger = Logger.getLogger("log_file");
	private static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Asegura que la tabla de conocimiento exista con su estructura completa e indices.
	 */
	public static void crearTablaSiNoExiste(Connection con) {
		String sql = "CREATE TABLE IF NOT EXISTS pizzaamericana.conocimiento_bloqueo_tienda ("
				+ "  idconocimiento INT AUTO_INCREMENT PRIMARY KEY,"
				+ "  idlogbloqueo INT DEFAULT 0,"
				+ "  idtienda INT NOT NULL,"
				+ "  nombre_tienda VARCHAR(100) NOT NULL,"
				+ "  periodo_semana VARCHAR(50) NOT NULL,"
				+ "  fecha_bloqueo DATETIME NOT NULL,"
				+ "  fecha_desbloqueo DATETIME NULL,"
				+ "  minutos_bloqueo INT NOT NULL DEFAULT 0,"
				+ "  horas_bloqueo DECIMAL(6,2) NOT NULL DEFAULT 0.00,"
				+ "  motivo VARCHAR(255) NULL,"
				+ "  observacion TEXT NULL,"
				+ "  debloqueo_en VARCHAR(50) NULL,"
				+ "  dia_semana VARCHAR(30) NULL,"
				+ "  es_fin_semana TINYINT(1) DEFAULT 0,"
				+ "  franja_horaria VARCHAR(50) NULL,"
				+ "  hora_del_dia INT DEFAULT 0,"
				+ "  pedidos_activos_pixel INT DEFAULT 0,"
				+ "  pizzas_en_proceso INT DEFAULT 0,"
				+ "  domiciliarios_activos INT DEFAULT 0,"
				+ "  ratio_pedidos_domiciliario DECIMAL(6,2) DEFAULT 0.00,"
				+ "  pedidos_domicilio_activos INT DEFAULT 0,"
				+ "  pedidos_mostrador_activos INT DEFAULT 0,"
				+ "  venta_hora_promedio DECIMAL(12,2) DEFAULT 0.00,"
				+ "  venta_estimada_perdida DECIMAL(12,2) DEFAULT 0.00,"
				+ "  patron_detectado VARCHAR(100) NULL,"
				+ "  nivel_criticidad VARCHAR(20) DEFAULT 'MEDIA',"
				+ "  sugerencia_predictiva TEXT NULL,"
				+ "  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
				+ "  KEY idx_periodo (periodo_semana),"
				+ "  KEY idx_tienda_fecha (idtienda, fecha_bloqueo),"
				+ "  KEY idx_patron (patron_detectado)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

		try (Statement stm = con.createStatement()) {
			stm.executeUpdate(sql);
		} catch (Exception e) {
			logger.warn("Aviso creando tabla conocimiento_bloqueo_tienda: " + e.getMessage());
		}
	}

	/**
	 * Analiza el contexto de un bloqueo de tienda, recopila métricas de carga y personal,
	 * genera el diagnóstico predictivo y persiste el registro en la base de datos.
	 */
	public static ConocimientoBloqueoTienda analizarYGuardarBloqueo(int idTienda, String nombreTienda,
			String fechaBloqueoStr, String fechaDesbloqueoStr, long minutosBloqueo, String motivo,
			String observacion, String debloqueoEn, String periodoSemana, int idLogBloqueo) {

		ConocimientoBloqueoTienda c = new ConocimientoBloqueoTienda();
		c.setIdLogBloqueo(idLogBloqueo);
		c.setIdTienda(idTienda);
		c.setNombreTienda(nombreTienda);
		c.setPeriodoSemana(periodoSemana);
		c.setFechaBloqueo(fechaBloqueoStr);
		c.setFechaDesbloqueo(fechaDesbloqueoStr);
		c.setMinutosBloqueo(minutosBloqueo);
		c.setMotivo(motivo);
		c.setObservacion(observacion);
		c.setDebloqueoEn(debloqueoEn);

		Date datBloqueo = null;
		try {
			datBloqueo = SDF_DATETIME.parse(fechaBloqueoStr);
		} catch (Exception e) {
			datBloqueo = new Date();
		}

		// Calendario y franja
		Calendar cal = Calendar.getInstance();
		cal.setTime(datBloqueo);
		int diaSem = cal.get(Calendar.DAY_OF_WEEK);
		int hora = cal.get(Calendar.HOUR_OF_DAY);
		int minuto = cal.get(Calendar.MINUTE);
		c.setHoraDelDia(hora);

		// Dia de la semana en Espanol
		String nombreDia = "";
		switch (diaSem) {
		case Calendar.SUNDAY:
			nombreDia = "Domingo";
			break;
		case Calendar.MONDAY:
			nombreDia = "Lunes";
			break;
		case Calendar.TUESDAY:
			nombreDia = "Martes";
			break;
		case Calendar.WEDNESDAY:
			nombreDia = "Miercoles";
			break;
		case Calendar.THURSDAY:
			nombreDia = "Jueves";
			break;
		case Calendar.FRIDAY:
			nombreDia = "Viernes";
			break;
		case Calendar.SATURDAY:
			nombreDia = "Sabado";
			break;
		default:
			nombreDia = "Desconocido";
			break;
		}
		c.setDiaSemana(nombreDia);
		boolean esFinSem = (diaSem == Calendar.FRIDAY || diaSem == Calendar.SATURDAY || diaSem == Calendar.SUNDAY);
		c.setEsFinSemana(esFinSem);

		// Franja horaria
		double horaFrac = hora + (minuto / 60.0);
		String franja = "HORARIO REGULAR";
		if (horaFrac >= 11.5 && horaFrac < 15.0) {
			franja = "ALMUERZO (Pico Diurno)";
		} else if (horaFrac >= 15.0 && horaFrac < 18.5) {
			franja = "VALLE VESPERTINO";
		} else if (horaFrac >= 18.5 && horaFrac < 22.5) {
			franja = "CENA (Pico Nocturno)";
		} else if (horaFrac >= 22.5 || horaFrac < 2.0) {
			franja = "TRASNOCHE";
		}
		c.setFranjaHoraria(franja);

		ConexionBaseDatos conHelper = new ConexionBaseDatos();
		try (Connection con = conHelper.obtenerConexionBDPrincipal()) {
			crearTablaSiNoExiste(con);

			// 1. Analizar pedidos activos en la ventana de 45 minutos previa al bloqueo
			int pedidosTotal = 0;
			int pedidosDomi = 0;
			int pedidosMost = 0;
			double ventaVentana = 0.0;

			String sqlPed = "SELECT COUNT(1) AS total, "
					+ "       IFNULL(SUM(CASE WHEN idtipopedido = 1 THEN 1 ELSE 0 END), 0) AS domi, "
					+ "       IFNULL(SUM(CASE WHEN idtipopedido != 1 THEN 1 ELSE 0 END), 0) AS mostrador, "
					+ "       IFNULL(SUM(total_neto), 0) AS venta "
					+ "FROM pizzaamericana.pedido "
					+ "WHERE idtienda = ? "
					+ "  AND fechainsercion BETWEEN DATE_SUB(?, INTERVAL 45 MINUTE) AND ?";

			try (PreparedStatement psPed = con.prepareStatement(sqlPed)) {
				psPed.setInt(1, idTienda);
				psPed.setString(2, fechaBloqueoStr);
				psPed.setString(3, fechaBloqueoStr);
				try (ResultSet rs = psPed.executeQuery()) {
					if (rs.next()) {
						pedidosTotal = rs.getInt("total");
						pedidosDomi = rs.getInt("domi");
						pedidosMost = rs.getInt("mostrador");
						ventaVentana = rs.getDouble("venta");
					}
				}
			} catch (Exception exPed) {
				logger.warn("Error consultando pedidos activos para bloqueo tienda " + idTienda + ": " + exPed);
			}

			c.setPedidosActivosPixel(pedidosTotal);
			c.setPedidosDomicilioActivos(pedidosDomi);
			c.setPedidosMostradorActivos(pedidosMost);

			// 2. Pizzas / unidades en proceso en esos pedidos
			int pizzasEnProceso = 0;
			String sqlPizzas = "SELECT IFNULL(SUM(d.cantidad), 0) "
					+ "FROM pizzaamericana.pedido p "
					+ "JOIN pizzaamericana.detalle_pedido d ON d.idpedido = p.idpedido "
					+ "WHERE p.idtienda = ? "
					+ "  AND p.fechainsercion BETWEEN DATE_SUB(?, INTERVAL 45 MINUTE) AND ?";

			try (PreparedStatement psPiz = con.prepareStatement(sqlPizzas)) {
				psPiz.setInt(1, idTienda);
				psPiz.setString(2, fechaBloqueoStr);
				psPiz.setString(3, fechaBloqueoStr);
				try (ResultSet rs = psPiz.executeQuery()) {
					if (rs.next()) {
						pizzasEnProceso = rs.getInt(1);
					}
				}
			} catch (Exception exPiz) {
				logger.warn("Error consultando pizzas en proceso: " + exPiz);
			}
			c.setPizzasEnProceso(pizzasEnProceso > 0 ? pizzasEnProceso : pedidosTotal);

			// 3. Domiciliarios activos en turno
			int domiEventos = 0;
			try (Connection conGen = conHelper.obtenerConexionBDGeneral()) {
				String sqlDomiEv = "SELECT COUNT(DISTINCT e.id) "
						+ "FROM general.empleado_evento e "
						+ "JOIN general.empleado em ON em.id = e.id "
						+ "WHERE e.idtienda = ? "
						+ "  AND e.fecha = DATE(?) "
						+ "  AND (em.idtipoempleado = 3 OR em.nombre_largo LIKE '%Domiciliario%') "
						+ "  AND e.tipo_evento = 'INGRESO' "
						+ "  AND e.fecha_hora_log <= ? "
						+ "  AND NOT EXISTS ( "
						+ "      SELECT 1 FROM general.empleado_evento s "
						+ "      WHERE s.id = e.id AND s.fecha = e.fecha "
						+ "        AND s.tipo_evento = 'SALIDA' "
						+ "        AND s.fecha_hora_log <= ? "
						+ "  )";
				try (PreparedStatement psEv = conGen.prepareStatement(sqlDomiEv)) {
					psEv.setInt(1, idTienda);
					psEv.setString(2, fechaBloqueoStr);
					psEv.setString(3, fechaBloqueoStr);
					psEv.setString(4, fechaBloqueoStr);
					try (ResultSet rs = psEv.executeQuery()) {
						if (rs.next()) {
							domiEventos = rs.getInt(1);
						}
					}
				}
			} catch (Exception exEv) {
				logger.warn("Aviso consultando biometricos domiciliarios: " + exEv);
			}

			// Complementar con domiciliarios con pedidos activos en la ventana
			int domiPedidos = 0;
			String sqlDomiPed = "SELECT COUNT(DISTINCT iddomiciliario) "
					+ "FROM pizzaamericana.pedido "
					+ "WHERE idtienda = ? "
					+ "  AND fechapedido = DATE(?) "
					+ "  AND iddomiciliario > 0 "
					+ "  AND fechainsercion BETWEEN DATE_SUB(?, INTERVAL 2 HOUR) AND DATE_ADD(?, INTERVAL 2 HOUR)";

			try (PreparedStatement psDp = con.prepareStatement(sqlDomiPed)) {
				psDp.setInt(1, idTienda);
				psDp.setString(2, fechaBloqueoStr);
				psDp.setString(3, fechaBloqueoStr);
				psDp.setString(4, fechaBloqueoStr);
				try (ResultSet rs = psDp.executeQuery()) {
					if (rs.next()) {
						domiPedidos = rs.getInt(1);
					}
				}
			} catch (Exception exDp) {
				logger.warn("Aviso consultando domiciliarios por pedidos: " + exDp);
			}

			int domiciliariosTotal = Math.max(domiEventos, domiPedidos);
			c.setDomiciliariosActivos(domiciliariosTotal);

			// 4. Ratio de presion
			double ratio = 0.0;
			int domiParaRatio = domiciliariosTotal > 0 ? domiciliariosTotal : 1;
			if (pedidosDomi > 0) {
				ratio = ((double) pedidosDomi) / domiParaRatio;
			} else if (pedidosTotal > 0) {
				ratio = ((double) pedidosTotal) / domiParaRatio;
			}
			c.setRatioPedidosDomiciliario(Math.round(ratio * 100.0) / 100.0);

			// 5. Venta estimada perdida
			double ventaHora = 0.0;
			if (ventaVentana > 0) {
				ventaHora = (ventaVentana / 45.0) * 60.0;
			} else {
				// Base referencial segun tipo de dia y franja
				ventaHora = esFinSem ? 240000.0 : 130000.0;
			}
			c.setVentaHoraPromedio(Math.round(ventaHora));
			double ventaPerdida = ventaHora * (((double) minutosBloqueo) / 60.0);
			c.setVentaEstimadaPerdida(Math.round(ventaPerdida));

			// 6. Diagnostico Predictivo y Nivel de Criticidad
			clasificarPatronYSugerencia(c);

			// 7. Persistir / Actualizar en pizzaamericana.conocimiento_bloqueo_tienda
			guardarOActualizarEnBD(con, c);

		} catch (Exception e) {
			logger.error("Error analizando y guardando conocimiento de bloqueo tienda " + idTienda + ": " + e);
			System.err.println("Error ConocimientoBloqueoDAO: " + e.getMessage());
		}

		return c;
	}

	/**
	 * Clasifica el patron de bloqueo y genera la recomendacion preventiva accionable.
	 */
	private static void clasificarPatronYSugerencia(ConocimientoBloqueoTienda c) {
		String motLower = c.getMotivo().toLowerCase(Locale.ROOT);
		String obsLower = c.getObservacion().toLowerCase(Locale.ROOT);
		double ratio = c.getRatioPedidosDomiciliario();
		int ped = c.getPedidosActivosPixel();
		int domi = c.getDomiciliariosActivos();
		long min = c.getMinutosBloqueo();

		String patron = "OPERATIVO_GENERAL";
		String sugerencia = "";
		String criticidad = "MEDIA";

		if (motLower.contains("horno") || motLower.contains("energ") || motLower.contains("internet")
				|| obsLower.contains("horno") || obsLower.contains("luz") || obsLower.contains("caido")) {
			patron = "FALLA_TECNICA_INFRAESTRUCTURA";
			sugerencia = "Priorizar revision de contingencia tecnica y mantenimiento preventivo en " + c.getNombreTienda()
					+ " para evitar desconexion forzada.";
			criticidad = min >= 30 ? "ALTA" : "MEDIA";

		} else if (motLower.contains("insumo") || motLower.contains("comida") || obsLower.contains("materia prima")
				|| obsLower.contains("masa") || obsLower.contains("queso")) {
			patron = "DESABASTECIMIENTO_INSUMOS";
			sugerencia = "Ajustar politicas de inventario minimo y horarios de entrega de comisariato hacia "
					+ c.getNombreTienda() + " antes del fin de semana.";
			criticidad = "ALTA";

		} else if (motLower.contains("accident") || motLower.contains("varado") || obsLower.contains("moto")) {
			patron = "INCIDENTE_LOGISTICA_RUTA";
			sugerencia = "Activar relevo inmediato de flota de apoyo o soporte de domiciliario de contingencia zonal cuando se reporte incidente vial.";
			criticidad = "MEDIA";

		} else if (ratio >= 4.0 || (c.getPedidosDomicilioActivos() >= 8 && domi <= 1)) {
			patron = "SATURACION_CRITICA_DOMICILIOS";
			int domiNecesarios = (int) Math.ceil(((double) c.getPedidosDomicilioActivos()) / 2.5);
			int deficit = Math.max(1, domiNecesarios - domi);
			sugerencia = "Reforzar cuadrilla con +" + deficit + " domiciliario(s) en la franja " + c.getFranjaHoraria()
					+ " los " + c.getDiaSemana() + " para mantener ratio < 3.0 ped/domi.";
			criticidad = "CRITICA";

		} else if (domi <= 1 && ped >= 5) {
			patron = "DEFICIT_DOMICILIARIOS";
			sugerencia = "Evitar programar turnos individuales en " + c.getNombreTienda() + " durante franjas con demanda superior a 5 pedidos simultaneos.";
			criticidad = "ALTA";

		} else if (motLower.contains("alta demanda") || ped >= 15) {
			patron = "SOBRECARGA_PICO_DEMANDA";
			sugerencia = "Ajustar tiempo estimado en tomador central (+15 min) y escalonar produccion para amortiguar el pico sin requerir bloqueo total de tienda.";
			criticidad = min >= 30 ? "CRITICA" : "ALTA";

		} else {
			patron = "GESTION_TIEMPOS_OPERACION";
			sugerencia = "Monitorear tiempos de preparacion y despacho para evitar acumular demoras preventivas.";
			criticidad = min >= 45 ? "ALTA" : "MEDIA";
		}

		if (min >= 60 || c.getVentaEstimadaPerdida() >= 400000.0) {
			criticidad = "CRITICA";
		} else if (min < 15 && !"CRITICA".equals(criticidad)) {
			criticidad = "BAJA";
		}

		c.setPatronDetectado(patron);
		c.setNivelCriticidad(criticidad);
		c.setSugerenciaPredictiva(sugerencia);
	}

	/**
	 * Inserta o actualiza el registro en `conocimiento_bloqueo_tienda`.
	 */
	private static void guardarOActualizarEnBD(Connection con, ConocimientoBloqueoTienda c) {
		String sqlCheck = "SELECT idconocimiento FROM pizzaamericana.conocimiento_bloqueo_tienda "
				+ "WHERE idtienda = ? AND fecha_bloqueo = ?";

		int idExistente = 0;
		try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
			psCheck.setInt(1, c.getIdTienda());
			psCheck.setString(2, c.getFechaBloqueo());
			try (ResultSet rs = psCheck.executeQuery()) {
				if (rs.next()) {
					idExistente = rs.getInt(1);
				}
			}
		} catch (Exception ex) {
			logger.warn("Aviso check conocimiento existente: " + ex);
		}

		if (idExistente > 0) {
			c.setIdConocimiento(idExistente);
			String sqlUpd = "UPDATE pizzaamericana.conocimiento_bloqueo_tienda SET "
					+ "fecha_desbloqueo = ?, minutos_bloqueo = ?, horas_bloqueo = ?, motivo = ?, observacion = ?, "
					+ "debloqueo_en = ?, pedidos_activos_pixel = ?, pizzas_en_proceso = ?, domiciliarios_activos = ?, "
					+ "ratio_pedidos_domiciliario = ?, pedidos_domicilio_activos = ?, pedidos_mostrador_activos = ?, "
					+ "venta_hora_promedio = ?, venta_estimada_perdida = ?, patron_detectado = ?, "
					+ "nivel_criticidad = ?, sugerencia_predictiva = ? "
					+ "WHERE idconocimiento = ?";
			try (PreparedStatement psUpd = con.prepareStatement(sqlUpd)) {
				psUpd.setString(1, c.getFechaDesbloqueo() != null && !c.getFechaDesbloqueo().isEmpty() ? c.getFechaDesbloqueo() : null);
				psUpd.setLong(2, c.getMinutosBloqueo());
				psUpd.setDouble(3, c.getHorasBloqueo());
				psUpd.setString(4, c.getMotivo());
				psUpd.setString(5, c.getObservacion());
				psUpd.setString(6, c.getDebloqueoEn());
				psUpd.setInt(7, c.getPedidosActivosPixel());
				psUpd.setInt(8, c.getPizzasEnProceso());
				psUpd.setInt(9, c.getDomiciliariosActivos());
				psUpd.setDouble(10, c.getRatioPedidosDomiciliario());
				psUpd.setInt(11, c.getPedidosDomicilioActivos());
				psUpd.setInt(12, c.getPedidosMostradorActivos());
				psUpd.setDouble(13, c.getVentaHoraPromedio());
				psUpd.setDouble(14, c.getVentaEstimadaPerdida());
				psUpd.setString(15, c.getPatronDetectado());
				psUpd.setString(16, c.getNivelCriticidad());
				psUpd.setString(17, c.getSugerenciaPredictiva());
				psUpd.setInt(18, idExistente);
				psUpd.executeUpdate();
			} catch (Exception exUpd) {
				logger.error("Error actualizando conocimiento_bloqueo_tienda: " + exUpd);
			}
		} else {
			String sqlIns = "INSERT INTO pizzaamericana.conocimiento_bloqueo_tienda ("
					+ "idlogbloqueo, idtienda, nombre_tienda, periodo_semana, fecha_bloqueo, fecha_desbloqueo, "
					+ "minutos_bloqueo, horas_bloqueo, motivo, observacion, debloqueo_en, dia_semana, es_fin_semana, "
					+ "franja_horaria, hora_del_dia, pedidos_activos_pixel, pizzas_en_proceso, domiciliarios_activos, "
					+ "ratio_pedidos_domiciliario, pedidos_domicilio_activos, pedidos_mostrador_activos, "
					+ "venta_hora_promedio, venta_estimada_perdida, patron_detectado, nivel_criticidad, sugerencia_predictiva) "
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			try (PreparedStatement psIns = con.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
				psIns.setInt(1, c.getIdLogBloqueo());
				psIns.setInt(2, c.getIdTienda());
				psIns.setString(3, c.getNombreTienda());
				psIns.setString(4, c.getPeriodoSemana());
				psIns.setString(5, c.getFechaBloqueo());
				psIns.setString(6, c.getFechaDesbloqueo() != null && !c.getFechaDesbloqueo().isEmpty() ? c.getFechaDesbloqueo() : null);
				psIns.setLong(7, c.getMinutosBloqueo());
				psIns.setDouble(8, c.getHorasBloqueo());
				psIns.setString(9, c.getMotivo());
				psIns.setString(10, c.getObservacion());
				psIns.setString(11, c.getDebloqueoEn());
				psIns.setString(12, c.getDiaSemana());
				psIns.setInt(13, c.isEsFinSemana() ? 1 : 0);
				psIns.setString(14, c.getFranjaHoraria());
				psIns.setInt(15, c.getHoraDelDia());
				psIns.setInt(16, c.getPedidosActivosPixel());
				psIns.setInt(17, c.getPizzasEnProceso());
				psIns.setInt(18, c.getDomiciliariosActivos());
				psIns.setDouble(19, c.getRatioPedidosDomiciliario());
				psIns.setInt(20, c.getPedidosDomicilioActivos());
				psIns.setInt(21, c.getPedidosMostradorActivos());
				psIns.setDouble(22, c.getVentaHoraPromedio());
				psIns.setDouble(23, c.getVentaEstimadaPerdida());
				psIns.setString(24, c.getPatronDetectado());
				psIns.setString(25, c.getNivelCriticidad());
				psIns.setString(26, c.getSugerenciaPredictiva());
				psIns.executeUpdate();
				try (ResultSet rsKey = psIns.getGeneratedKeys()) {
					if (rsKey.next()) {
						c.setIdConocimiento(rsKey.getInt(1));
					}
				}
			} catch (Exception exIns) {
				logger.error("Error insertando conocimiento_bloqueo_tienda: " + exIns);
			}
		}
	}

	/**
	 * Recupera el conocimiento de bloqueos almacenado para un periodo semanal.
	 */
	public static List<ConocimientoBloqueoTienda> obtenerPorPeriodo(String periodoSemana) {
		List<ConocimientoBloqueoTienda> lista = new ArrayList<>();
		String sql = "SELECT * FROM pizzaamericana.conocimiento_bloqueo_tienda "
				+ "WHERE periodo_semana = ? ORDER BY minutos_bloqueo DESC, fecha_bloqueo DESC";

		ConexionBaseDatos conHelper = new ConexionBaseDatos();
		try (Connection con = conHelper.obtenerConexionBDPrincipal();
			 PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, periodoSemana);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					ConocimientoBloqueoTienda c = mapearFila(rs);
					lista.add(c);
				}
			}
		} catch (Exception e) {
			logger.error("Error consultando conocimiento_bloqueo_tienda por periodo: " + e);
		}
		return lista;
	}

	private static ConocimientoBloqueoTienda mapearFila(ResultSet rs) throws Exception {
		ConocimientoBloqueoTienda c = new ConocimientoBloqueoTienda();
		c.setIdConocimiento(rs.getInt("idconocimiento"));
		c.setIdLogBloqueo(rs.getInt("idlogbloqueo"));
		c.setIdTienda(rs.getInt("idtienda"));
		c.setNombreTienda(rs.getString("nombre_tienda"));
		c.setPeriodoSemana(rs.getString("periodo_semana"));
		c.setFechaBloqueo(rs.getString("fecha_bloqueo"));
		c.setFechaDesbloqueo(rs.getString("fecha_desbloqueo"));
		c.setMinutosBloqueo(rs.getLong("minutos_bloqueo"));
		c.setHorasBloqueo(rs.getDouble("horas_bloqueo"));
		c.setMotivo(rs.getString("motivo"));
		c.setObservacion(rs.getString("observacion"));
		c.setDebloqueoEn(rs.getString("debloqueo_en"));
		c.setDiaSemana(rs.getString("dia_semana"));
		c.setEsFinSemana(rs.getInt("es_fin_semana") == 1);
		c.setFranjaHoraria(rs.getString("franja_horaria"));
		c.setHoraDelDia(rs.getInt("hora_del_dia"));
		c.setPedidosActivosPixel(rs.getInt("pedidos_activos_pixel"));
		c.setPizzasEnProceso(rs.getInt("pizzas_en_proceso"));
		c.setDomiciliariosActivos(rs.getInt("domiciliarios_activos"));
		c.setRatioPedidosDomiciliario(rs.getDouble("ratio_pedidos_domiciliario"));
		c.setPedidosDomicilioActivos(rs.getInt("pedidos_domicilio_activos"));
		c.setPedidosMostradorActivos(rs.getInt("pedidos_mostrador_activos"));
		c.setVentaHoraPromedio(rs.getDouble("venta_hora_promedio"));
		c.setVentaEstimadaPerdida(rs.getDouble("venta_estimada_perdida"));
		c.setPatronDetectado(rs.getString("patron_detectado"));
		c.setNivelCriticidad(rs.getString("nivel_criticidad"));
		c.setSugerenciaPredictiva(rs.getString("sugerencia_predictiva"));
		return c;
	}
}

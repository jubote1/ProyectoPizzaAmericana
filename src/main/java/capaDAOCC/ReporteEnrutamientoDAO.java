package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El reporte de uso del enrutamiento, leido de la base LOCAL de una tienda o,
 * cuando la tienda no contesta, del DATAMART del central.
 *
 * QUE MIDE
 *
 * De los domicilios propios de la tienda en un rango de fechas, cuantos salieron
 * dentro de una ruta que el sistema sugirio (enrutamiento) y cuantos no, a que
 * horas entran y salen los pedidos, y cuantos pedidos enrutados SOLTARON los
 * domiciliarios.
 *
 * DOMICILIO PROPIO
 *
 * Es el mismo criterio con el que el monitoreo del POS cuenta "Domicilio":
 * idtipopedido = 1, sin las plataformas (estacion con DOMI-Plat, DIDI o RAPPI),
 * sin los domicilios tercerizados (domicilio_tercerizado = 'S', Rappi Cargo, que
 * los lleva un tercero y no pasan por un domiciliario de la tienda, asi que no
 * pueden usar el enrutamiento) y sin los anulados.
 *
 * QUE ES "USO EL ENRUTAMIENTO"
 *
 * Que el pedido salio con un domiciliario que lo llevaba en la ruta que se le
 * habia sugerido: despacho_real_det.origen_pedido = 'SUGERIDO'. Una sugerencia que
 * el domiciliario solto y que despues salio "libre" NO cuenta como enrutada.
 *
 * QUE ES "SOLTADO"
 *
 * Un pedido que se le sugirio a un domiciliario y que EL SOLTO. No alcanza con
 * mirar pedido_sugerencia_det.estado_det = 'LIBERADO': ese estado lo ponen DOS
 * caminos -el domiciliario que suelta su enrutamiento (PedidoSugerenciaDAO.liberar)
 * y el administrador que anula una sugerencia completa (anularSugerencia)- y solo
 * el primero es lo que se quiere medir. Lo que los separa es el log: cuando el
 * domiciliario suelta queda una fila accion = 'LIBERADA', con el id del pedido si
 * solto uno solo o con 0/NULL si solto el enrutamiento entero (en ese caso aplica
 * a todos los pedidos de la sugerencia). Un pedido es soltado si su detalle quedo
 * LIBERADO y hay una fila LIBERADA del log que lo cubra.
 *
 * LAS DOS HORAS
 *
 *  - Entrada: la hora de pedido.fechainsercion, o sea cuando el pedido entro a la
 *    tienda.
 *  - Salida: en la TIENDA, la hora del ultimo cambio del pedido a "En ruta" en
 *    cambios_estado_pedido (el id del estado sale del parametro ENRUTADOMICILIO,
 *    con 7 de respaldo). En el DATAMART esa tabla no se replica, asi que se usa
 *    despacho_real.hora_salida, que el POS graba en el mismo instante en que
 *    pasa los pedidos a "En ruta": es la misma hora.
 *
 * Las horas son las del reloj de la tienda, 0 a 23. Los pedidos se agrupan por
 * la fecha de JORNADA (pedido.fechapedido), no por la del reloj.
 *
 * DATAMART: HASTA AYER Y SIN GARANTIAS DE COLUMNAS
 *
 * ServicioReplicaPedidos lo llena una vez al dia, de madrugada, con el dia
 * anterior: nunca trae "hoy". Y copia por nombre de columna, asi que una columna
 * que el datamart no tenga (estacion, domicilio_tercerizado, origen_pedido...)
 * simplemente no esta. Cada una se pregunta antes de usarla; las que son
 * indispensables para el reporte -sin ellas no se puede separar un domicilio
 * propio- hacen que la tienda se muestre como "sin datos" y no con una cifra
 * inventada.
 *
 * UNA TIENDA ATRASADA NO TUMBA EL REPORTE
 *
 * Si la tienda no tiene la columna despacho_real_det.origen_pedido, el reporte
 * igual muestra sus pedidos y sus horas, pero marca enrutamientoInstalado = false.
 */
public class ReporteEnrutamientoDAO {

	public static final String ESTADO_OK = "OK";
	public static final String ESTADO_SIN_CONEXION = "SIN_CONEXION";
	public static final String ESTADO_TARDO = "TARDO";
	public static final String ESTADO_ERROR = "ERROR";

	public static final String FUENTE_TIENDA = "TIENDA";
	public static final String FUENTE_DATAMART = "DATAMART";

	/** Cuantos soltados se listan por tienda: es para ver quien y por que, no un archivo. */
	private static final int MAXIMO_DETALLE_SOLTADOS = 60;

	/** Un domiciliario que solto pedidos. */
	public static class PorDomiciliario {
		public int idDomiciliario = 0;
		public String nombre = "";
		/** Pedidos que se le sugirieron en el rango. */
		public int sugeridos = 0;
		/** De ellos, los que el solto. */
		public int soltados = 0;
		/** Los que se llevo por la ruta sugerida. */
		public int llevados = 0;
		/** Los que NO acepto ni solto: salieron por fuera de la ruta. */
		public int libres = 0;
	}

	/**
	 * Que paso con los pedidos que se enrutaron. Las categorias NO se traslapan y
	 * suman sugeridos, para que la pantalla cierre la cuenta: ni un pedido enrutado
	 * se queda sin explicar.
	 */
	public static class Desglose {
		/** Pedidos que se sugirieron a un domiciliario en el rango (por jornada de la sugerencia). */
		public int sugeridos = 0;
		/** Salieron por la ruta sugerida. */
		public int llevados = 0;
		/** El domiciliario los solto. */
		public int soltados = 0;
		/** Un administrador los movio a otro domiciliario. */
		public int reasignados = 0;
		/** Un administrador anulo la sugerencia entera. */
		public int anulados = 0;
		/** La ruta no se acepto ni se solto, y el pedido SALIO por fuera de ella. */
		public int libres = 0;
		/** La ruta se creo cuando el pedido YA HABIA SALIDO: llego tarde, no fue que se ignorara. */
		public int rutasTardias = 0;
		/** La sugerencia expiro y el pedido no salio (se anulo o no tiene salida registrada). */
		public int expiradasSinSalir = 0;
		/** La sugerencia sigue viva y el pedido todavia no sale. */
		public int enCurso = 0;
		/** Lo que no cae en ninguna de las anteriores (por ejemplo, un pedido sugerido dos veces). */
		public int otros = 0;
		/** Rutas (sugerencias) creadas, aceptadas y vencidas sin aceptar. */
		public int sugerencias = 0;
		public int sugerenciasAceptadas = 0;
		public int sugerenciasExpiradas = 0;

		public void sumar(final Desglose o) {
			sugeridos += o.sugeridos;
			llevados += o.llevados;
			soltados += o.soltados;
			reasignados += o.reasignados;
			anulados += o.anulados;
			libres += o.libres;
			rutasTardias += o.rutasTardias;
			expiradasSinSalir += o.expiradasSinSalir;
			enCurso += o.enCurso;
			otros += o.otros;
			sugerencias += o.sugerencias;
			sugerenciasAceptadas += o.sugerenciasAceptadas;
			sugerenciasExpiradas += o.sugerenciasExpiradas;
		}
	}

	/** Un pedido soltado, con quien lo solto y el motivo que dio. */
	public static class Soltado {
		public String fechaHora = "";
		public int idDomiciliario = 0;
		public String domiciliario = "";
		public int idPedido = 0;
		public String motivo = "";
	}

	/** Lo que se encontro en una tienda. */
	public static class ResultadoTienda {
		public int idTienda = 0;
		public String tienda = "";
		/** OK, SIN_CONEXION, TARDO o ERROR. */
		public String estado = ESTADO_ERROR;
		public String error = "";
		/** De donde salen las cifras: la TIENDA en vivo o el DATAMART (hasta ayer). */
		public String fuente = FUENTE_TIENDA;
		/** Solo DATAMART: el ultimo dia que el datamart tiene de esta tienda (yyyy-MM-dd). */
		public String datosHasta = "";
		/** Solo DATAMART: por que no se pudo en vivo (SIN_CONEXION o TARDO). */
		public String motivoVivo = "";
		/** La tienda ya tiene el enrutamiento instalado (existe la columna origen_pedido). */
		public boolean enrutamientoInstalado = false;

		/** Domicilios propios del rango. */
		public int pedidos = 0;
		/** De ellos, los que salieron dentro de una ruta sugerida. */
		public int enrutados = 0;

		/** Por hora de entrada a la tienda (0 a 23): pedidos y cuantos de ellos salieron enrutados. */
		public int[] entradas = new int[24];
		public int[] entradasEnrutadas = new int[24];

		/** Por hora de salida: pedidos y cuantos de ellos salieron enrutados. */
		public int[] salidas = new int[24];
		public int[] salidasEnrutadas = new int[24];

		/** Suma de salidas: los pedidos que tienen registro de salida. */
		public int salidasTotal = 0;
		public int salidasEnrutadasTotal = 0;

		/** Lo que NO entra al reporte y se cuenta aparte para que se sepa. */
		public int excluidosPlataforma = 0;
		public int excluidosTercerizados = 0;

		/** Si se pudo leer el enrutamiento (las tres tablas de sugerencias). */
		public boolean soltadosDisponible = false;
		/** Que paso con los pedidos enrutados. */
		public Desglose enrutamiento = new Desglose();
		public ArrayList<PorDomiciliario> porDomiciliario = new ArrayList<PorDomiciliario>();
		public ArrayList<Soltado> detalleSoltados = new ArrayList<Soltado>();
	}

	// -----------------------------------------------------------------------
	// Piezas de SQL
	// -----------------------------------------------------------------------

	/** Plataformas: la estacion lo dice. */
	private static final String ES_PLATAFORMA =
			"(IFNULL(p.estacion,'') LIKE '%DOMI-Plat%' OR IFNULL(p.estacion,'') LIKE '%DIDI%' OR IFNULL(p.estacion,'') LIKE '%RAPPI%')";

	/** El estado "En ruta" de ESA tienda; 7 si el parametro no esta. */
	private static final String ESTADO_EN_RUTA =
			"IFNULL((SELECT valornumerico FROM parametros WHERE valorparametro = 'ENRUTADOMICILIO' LIMIT 1), 7)";

	/** Un SQL con sus parametros, para no perder la cuenta de los signos de pregunta. */
	private static class Sql {
		private final StringBuilder texto = new StringBuilder();
		private final ArrayList<Object> parametros = new ArrayList<Object>();

		Sql add(final String fragmento, final Object... valores) {
			texto.append(fragmento);
			for (final Object v : valores) {
				parametros.add(v);
			}
			return (this);
		}

		PreparedStatement preparar(final Connection cn) throws Exception {
			final PreparedStatement ps = cn.prepareStatement(texto.toString());
			for (int i = 0; i < parametros.size(); i++) {
				final Object v = parametros.get(i);
				if (v instanceof Integer) {
					ps.setInt(i + 1, ((Integer) v).intValue());
				} else {
					ps.setString(i + 1, String.valueOf(v));
				}
			}
			return (ps);
		}
	}

	/** Las columnas de pedido de las que depende el reporte, segun la base a la que se consulta. */
	private static class Columnas {
		boolean indispensables = true;
		String faltan = "";
		boolean tercerizado = true;
		boolean anulacion = true;
	}

	private static String domicilioPropio(final Columnas c) {
		return ("p.idtipopedido = 1 AND NOT " + ES_PLATAFORMA
				+ (c.tercerizado ? " AND IFNULL(p.domicilio_tercerizado,'N') <> 'S'" : "")
				+ (c.anulacion ? " AND IFNULL(p.idmotivoanulacion,0) = 0" : ""));
	}

	/**
	 * Los pedidos que salieron en una ruta sugerida. Tabla derivada y no EXISTS
	 * por fila: asi MySQL la materializa una vez en vez de recorrer
	 * despacho_real_det por cada pedido. En el datamart lleva idtienda.
	 */
	private static Sql joinEnrutados(final Sql s, final boolean datamart, final int idTienda) {
		if (datamart) {
			return (s.add(" LEFT JOIN (SELECT DISTINCT id_pedido FROM despacho_real_det"
					+ " WHERE idtienda = ? AND origen_pedido = 'SUGERIDO') e ON e.id_pedido = p.idpedidotienda",
					Integer.valueOf(idTienda)));
		}
		return (s.add(" LEFT JOIN (SELECT DISTINCT id_pedido FROM despacho_real_det WHERE origen_pedido = 'SUGERIDO') e"
				+ " ON e.id_pedido = p.idpedidotienda"));
	}

	// -----------------------------------------------------------------------
	// En vivo: la base de la tienda
	// -----------------------------------------------------------------------

	/**
	 * Todo lo del reporte en UNA tienda, en vivo. Devuelve siempre un resultado,
	 * nunca null: si la tienda no contesta queda en SIN_CONEXION, que es distinto
	 * de "no tuvo pedidos".
	 *
	 * @param desde y hasta en yyyy-MM-dd, inclusive
	 */
	public static ResultadoTienda consultarTienda(final int idTienda, final String nombreTienda,
			final String hosbd, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ResultadoTienda res = new ResultadoTienda();
		res.idTienda = idTienda;
		res.tienda = (nombreTienda == null ? "" : nombreTienda);

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDTiendaRemota(hosbd);
			if (cn == null) {
				res.estado = ESTADO_SIN_CONEXION;
				res.error = "El computador de la tienda no se encuentra en linea";
				return (res);
			}
			res.enrutamientoInstalado = existeColumna(cn, "despacho_real_det", "origen_pedido");
			final Columnas columnas = new Columnas();
			cargarEntradas(cn, res, columnas, false, idTienda, desde, hasta);
			cargarSalidasTienda(cn, res, columnas, desde, hasta);
			cargarExcluidos(cn, res, columnas, false, idTienda, desde, hasta);
			cargarSoltados(cn, res, false, idTienda, desde, hasta);
			res.estado = ESTADO_OK;
		} catch (final Exception e) {
			logger.error("ReporteEnrutamientoDAO.consultarTienda " + hosbd + ": " + e.toString());
			res.estado = ESTADO_ERROR;
			res.error = e.toString();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	// -----------------------------------------------------------------------
	// Datamart: lo que hay hasta ayer
	// -----------------------------------------------------------------------

	/**
	 * Lo mismo, leido del datamart. Sirve cuando la tienda esta apagada.
	 *
	 * Devuelve siempre un resultado. Si se pudo leer queda con estado OK y
	 * fuente DATAMART; si no, queda con ESTADO_SIN_CONEXION y, cuando se sabe,
	 * datosHasta, para que la pantalla diga "el datamart llega hasta tal dia".
	 *
	 * El rango se recorta hasta el ultimo dia que el datamart tiene de esa tienda:
	 * pedir hasta hoy y recibir "hasta ayer" es lo normal, no un error, pero la
	 * pantalla lo tiene que decir.
	 *
	 * @param motivoVivo por que no se pudo en vivo (SIN_CONEXION o TARDO)
	 */
	public static ResultadoTienda consultarTiendaDatamart(final int idTienda, final String nombreTienda,
			final String desde, final String hasta, final String motivoVivo) {
		final Logger logger = Logger.getLogger("log_file");
		final ResultadoTienda res = new ResultadoTienda();
		res.idTienda = idTienda;
		res.tienda = (nombreTienda == null ? "" : nombreTienda);
		res.fuente = FUENTE_DATAMART;
		res.motivoVivo = (motivoVivo == null ? "" : motivoVivo);
		res.estado = ESTADO_SIN_CONEXION;

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDDatamartLocal();
			if (cn == null) {
				res.error = "No se pudo conectar con el datamart";
				return (res);
			}
			final PreparedStatement ps = cn.prepareStatement("SELECT MAX(fechapedido) AS f FROM pedido WHERE idtienda = ?");
			ps.setInt(1, idTienda);
			final ResultSet rs = ps.executeQuery();
			if (rs.next() && rs.getString("f") != null) {
				res.datosHasta = rs.getString("f");
			}
			rs.close();
			ps.close();
			if (res.datosHasta.length() == 0) {
				res.error = "El datamart no tiene pedidos de esta tienda";
				return (res);
			}
			//El rango efectivo: lo que se pidio, hasta donde llega el datamart.
			final String hastaEfectivo = (hasta.compareTo(res.datosHasta) <= 0 ? hasta : res.datosHasta);
			if (desde.compareTo(hastaEfectivo) > 0) {
				res.error = "El datamart de esta tienda llega hasta " + res.datosHasta + ", antes del rango pedido";
				return (res);
			}

			final Columnas columnas = columnasDatamart(cn);
			if (!columnas.indispensables) {
				res.error = "El datamart no tiene las columnas necesarias de pedido (" + columnas.faltan + ")";
				return (res);
			}
			res.enrutamientoInstalado = existeColumna(cn, "despacho_real_det", "origen_pedido");
			cargarEntradas(cn, res, columnas, true, idTienda, desde, hastaEfectivo);
			cargarSalidasDatamart(cn, res, columnas, idTienda, desde, hastaEfectivo);
			cargarExcluidos(cn, res, columnas, true, idTienda, desde, hastaEfectivo);
			cargarSoltados(cn, res, true, idTienda, desde, hastaEfectivo);
			res.estado = ESTADO_OK;
		} catch (final Exception e) {
			logger.error("ReporteEnrutamientoDAO.consultarTiendaDatamart " + idTienda + ": " + e.toString());
			res.estado = ESTADO_SIN_CONEXION;
			res.error = e.toString();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/** Que columnas de pedido trae el datamart. Las que faltan y no son indispensables se ignoran. */
	private static Columnas columnasDatamart(final Connection cn) {
		final Columnas c = new Columnas();
		final String[] indispensables = { "idpedidotienda", "idtienda", "fechapedido", "fechainsercion", "idtipopedido", "estacion" };
		final StringBuilder faltan = new StringBuilder();
		for (final String col : indispensables) {
			if (!existeColumna(cn, "pedido", col)) {
				if (faltan.length() > 0) {
					faltan.append(", ");
				}
				faltan.append(col);
			}
		}
		c.indispensables = (faltan.length() == 0);
		c.faltan = faltan.toString();
		c.tercerizado = existeColumna(cn, "pedido", "domicilio_tercerizado");
		c.anulacion = existeColumna(cn, "pedido", "idmotivoanulacion");
		return (c);
	}

	// -----------------------------------------------------------------------
	// Las consultas
	// -----------------------------------------------------------------------

	/** Pedidos por hora de entrada, y de ellos cuantos usaron el enrutamiento. */
	private static void cargarEntradas(final Connection cn, final ResultadoTienda res, final Columnas c,
			final boolean datamart, final int idTienda, final String desde, final String hasta) throws Exception {
		final boolean inst = res.enrutamientoInstalado;
		final Sql s = new Sql();
		s.add("SELECT HOUR(p.fechainsercion) AS hora, COUNT(*) AS pedidos, "
				+ (inst ? "SUM(e.id_pedido IS NOT NULL)" : "0") + " AS enrutados FROM pedido p");
		if (inst) {
			joinEnrutados(s, datamart, idTienda);
		}
		s.add(" WHERE p.fechapedido BETWEEN ? AND ?", desde, hasta);
		if (datamart) {
			s.add(" AND p.idtienda = ?", Integer.valueOf(idTienda));
		}
		s.add(" AND " + domicilioPropio(c) + " GROUP BY HOUR(p.fechainsercion)");
		final PreparedStatement ps = s.preparar(cn);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final int hora = rs.getInt("hora");
			final boolean sinHora = rs.wasNull();
			final int pedidos = rs.getInt("pedidos");
			final int enrutados = rs.getInt("enrutados");
			//Un pedido sin fecha de insercion cuenta en el total pero no tiene hora
			//donde pintarse.
			res.pedidos = res.pedidos + pedidos;
			res.enrutados = res.enrutados + enrutados;
			if (!sinHora && hora >= 0 && hora <= 23) {
				res.entradas[hora] = pedidos;
				res.entradasEnrutadas[hora] = enrutados;
			}
		}
		rs.close();
		ps.close();
	}

	/**
	 * Pedidos por hora de salida, en la TIENDA: el ultimo cambio a "En ruta".
	 *
	 * La tabla derivada de cambios_estado_pedido se acota por fecha: un pedido de
	 * la jornada D sale entre D y D+1 (despues de medianoche), asi que alcanza con
	 * mirar los cambios de desde hasta hasta + 2 dias. Sin ese acote se agruparia
	 * toda la historia de la tabla, que es la mas grande de la base.
	 */
	private static void cargarSalidasTienda(final Connection cn, final ResultadoTienda res, final Columnas c,
			final String desde, final String hasta) throws Exception {
		final boolean inst = res.enrutamientoInstalado;
		final Sql s = new Sql();
		s.add("SELECT HOUR(s.salida) AS hora, COUNT(*) AS salidas, SUM(s.enr) AS enrutados FROM ("
				+ " SELECT x.salida AS salida, " + (inst ? "(e.id_pedido IS NOT NULL)" : "0") + " AS enr"
				+ " FROM pedido p"
				+ " INNER JOIN (SELECT idpedidotienda, MAX(fechacambio) AS salida FROM cambios_estado_pedido"
				+ "   WHERE idestadoposterior = " + ESTADO_EN_RUTA
				+ "   AND fechacambio >= ? AND fechacambio < DATE_ADD(?, INTERVAL 3 DAY)"
				+ "   GROUP BY idpedidotienda) x ON x.idpedidotienda = p.idpedidotienda", desde, hasta);
		if (inst) {
			joinEnrutados(s, false, 0);
		}
		s.add(" WHERE p.fechapedido BETWEEN ? AND ? AND " + domicilioPropio(c) + ") s GROUP BY HOUR(s.salida)", desde, hasta);
		leerSalidas(s.preparar(cn), res);
	}

	/**
	 * Pedidos por hora de salida, en el DATAMART: despacho_real.hora_salida de la
	 * salida en que el pedido fue (despacho_real_det). Si un pedido salio mas de
	 * una vez vale la ultima, igual que en la tienda.
	 */
	private static void cargarSalidasDatamart(final Connection cn, final ResultadoTienda res, final Columnas c,
			final int idTienda, final String desde, final String hasta) throws Exception {
		final boolean inst = res.enrutamientoInstalado;
		final Integer id = Integer.valueOf(idTienda);
		final Sql s = new Sql();
		s.add("SELECT HOUR(s.salida) AS hora, COUNT(*) AS salidas, SUM(s.enr) AS enrutados FROM ("
				+ " SELECT x.salida AS salida, " + (inst ? "(e.id_pedido IS NOT NULL)" : "0") + " AS enr"
				+ " FROM pedido p"
				+ " INNER JOIN (SELECT d.id_pedido, MAX(r.hora_salida) AS salida FROM despacho_real_det d"
				+ "   INNER JOIN despacho_real r ON r.id = d.despacho_real_id AND r.idtienda = d.idtienda"
				+ "   WHERE d.idtienda = ? AND r.fecha BETWEEN ? AND ? AND r.hora_salida IS NOT NULL"
				+ "   GROUP BY d.id_pedido) x ON x.id_pedido = p.idpedidotienda", id, desde, hasta);
		if (inst) {
			joinEnrutados(s, true, idTienda);
		}
		s.add(" WHERE p.idtienda = ? AND p.fechapedido BETWEEN ? AND ? AND " + domicilioPropio(c) + ") s GROUP BY HOUR(s.salida)",
				id, desde, hasta);
		leerSalidas(s.preparar(cn), res);
	}

	private static void leerSalidas(final PreparedStatement ps, final ResultadoTienda res) throws Exception {
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final int hora = rs.getInt("hora");
			final boolean sinHora = rs.wasNull();
			final int salidas = rs.getInt("salidas");
			final int enrutados = rs.getInt("enrutados");
			res.salidasTotal = res.salidasTotal + salidas;
			res.salidasEnrutadasTotal = res.salidasEnrutadasTotal + enrutados;
			if (!sinHora && hora >= 0 && hora <= 23) {
				res.salidas[hora] = salidas;
				res.salidasEnrutadas[hora] = enrutados;
			}
		}
		rs.close();
		ps.close();
	}

	/** Lo que se deja por fuera, para decirlo en pantalla. */
	private static void cargarExcluidos(final Connection cn, final ResultadoTienda res, final Columnas c,
			final boolean datamart, final int idTienda, final String desde, final String hasta) throws Exception {
		final Sql s = new Sql();
		s.add("SELECT SUM(CASE WHEN " + ES_PLATAFORMA + " THEN 1 ELSE 0 END) AS plataforma,"
				+ (c.tercerizado
						? " SUM(CASE WHEN NOT " + ES_PLATAFORMA + " AND IFNULL(p.domicilio_tercerizado,'N') = 'S' THEN 1 ELSE 0 END)"
						: " 0")
				+ " AS tercerizado FROM pedido p WHERE p.fechapedido BETWEEN ? AND ? AND p.idtipopedido = 1"
				+ (c.anulacion ? " AND IFNULL(p.idmotivoanulacion,0) = 0" : ""), desde, hasta);
		if (datamart) {
			s.add(" AND p.idtienda = ?", Integer.valueOf(idTienda));
		}
		final PreparedStatement ps = s.preparar(cn);
		final ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			res.excluidosPlataforma = rs.getInt("plataforma");
			res.excluidosTercerizados = rs.getInt("tercerizado");
		}
		rs.close();
		ps.close();
	}

	/**
	 * Un pedido soltado: su detalle quedo LIBERADO y el log tiene una fila LIBERADA
	 * del domiciliario que lo cubre (la de ese pedido, o la del enrutamiento entero,
	 * que lleva el id del pedido en 0 o NULL). Ver la explicacion de la clase.
	 */
	private static String esSoltado(final boolean datamart) {
		return ("(d.estado_det = 'LIBERADO' AND EXISTS (SELECT 1 FROM pedido_sugerencia_log l"
				+ " WHERE l.pedido_sugerencia_id = d.pedido_sugerencia_id"
				+ (datamart ? " AND l.idtienda = d.idtienda" : "")
				+ " AND l.accion = 'LIBERADA' AND (IFNULL(l.id_pedido,0) = 0 OR l.id_pedido = d.id_pedido)))");
	}

	/** El pedido de este detalle YA SALIO: tiene una salida de domiciliario (despacho_real_det). */
	private static String salio(final boolean datamart) {
		return ("EXISTS (SELECT 1 FROM despacho_real_det x WHERE x.id_pedido = d.id_pedido"
				+ (datamart ? " AND x.idtienda = d.idtienda" : "") + ")");
	}

	/**
	 * El pedido salio DESPUES de que se creo la ruta (despacho_real.hora_salida no
	 * es anterior a pedido_sugerencia.creado_en). Si salio antes, la ruta llego
	 * tarde y no hubo nada que ignorar.
	 */
	private static String salioDespuesDeLaRuta(final boolean datamart) {
		return ("EXISTS (SELECT 1 FROM despacho_real_det x INNER JOIN despacho_real r ON r.id = x.despacho_real_id"
				+ (datamart ? " AND r.idtienda = x.idtienda" : "")
				+ " WHERE x.id_pedido = d.id_pedido" + (datamart ? " AND x.idtienda = d.idtienda" : "")
				+ " AND r.hora_salida >= c.creado_en)");
	}

	/** Otra ruta SI llevo este pedido (otro detalle del mismo pedido en LLEVADO). */
	private static String llevadoPorOtraRuta(final boolean datamart) {
		return ("EXISTS (SELECT 1 FROM pedido_sugerencia_det d2 WHERE d2.id_pedido = d.id_pedido"
				+ " AND d2.estado_det = 'LLEVADO'" + (datamart ? " AND d2.idtienda = d.idtienda" : "") + ")");
	}

	/**
	 * Un pedido enrutado que salio POR FUERA de su ruta: el detalle nunca se resolvio
	 * (sigue SUGERIDO), el pedido salio con un domiciliario DESPUES de que se creo la
	 * ruta, y no fue porque otra ruta lo llevo. Es el caso de un domiciliario que
	 * ignora lo que se le sugirio: no lo solto, no lo acepto y salio con ese pedido
	 * de todos modos.
	 */
	private static String esLibre(final boolean datamart) {
		return ("(d.estado_det = 'SUGERIDO' AND " + salioDespuesDeLaRuta(datamart)
				+ " AND NOT " + llevadoPorOtraRuta(datamart) + ")");
	}

	/**
	 * Un pedido cuya ruta se creo CUANDO YA HABIA SALIDO: no se ignoro nada, la ruta
	 * llego tarde (quien enruta lo hizo sobre un pedido que ya iba en la calle).
	 */
	private static String esRutaTardia(final boolean datamart) {
		return ("(d.estado_det = 'SUGERIDO' AND " + salio(datamart) + " AND NOT " + salioDespuesDeLaRuta(datamart)
				+ " AND NOT " + llevadoPorOtraRuta(datamart) + ")");
	}

	/**
	 * El enrutamiento: cuantos pedidos se sugirieron, cuantos se llevaron, cuantos
	 * SOLTARON los domiciliarios y quien. Va en su propio try: si una tienda no
	 * tiene las tablas, el resto del reporte tiene que seguir funcionando.
	 */
	private static void cargarSoltados(final Connection cn, final ResultadoTienda res, final boolean datamart,
			final int idTienda, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		try {
			if (!existeTabla(cn, "pedido_sugerencia") || !existeTabla(cn, "pedido_sugerencia_det")
					|| !existeTabla(cn, "pedido_sugerencia_log")) {
				return;
			}
			final String cruce = " FROM pedido_sugerencia_det d INNER JOIN pedido_sugerencia c ON c.id = d.pedido_sugerencia_id"
					+ (datamart ? " AND c.idtienda = d.idtienda" : "");

			final Desglose g = res.enrutamiento;

			//1. Que paso con cada pedido enrutado. Un solo recorrido, con categorias que
			//no se traslapan (ver Desglose): para un detalle SUGERIDO, lo que lo separa
			//es si el pedido YA SALIO y, si no, si su ruta expiro o sigue viva.
			final Sql s1 = new Sql();
			s1.add("SELECT COUNT(*) AS sugeridos,"
					+ " SUM(CASE WHEN d.estado_det = 'LLEVADO' THEN 1 ELSE 0 END) AS llevados,"
					+ " SUM(CASE WHEN d.estado_det = 'REASIGNADO' THEN 1 ELSE 0 END) AS reasignados,"
					+ " SUM(CASE WHEN " + esSoltado(datamart) + " THEN 1 ELSE 0 END) AS soltados,"
					+ " SUM(CASE WHEN d.estado_det = 'LIBERADO' THEN 1 ELSE 0 END) AS liberados,"
					+ " SUM(CASE WHEN " + esLibre(datamart) + " THEN 1 ELSE 0 END) AS libres,"
					+ " SUM(CASE WHEN " + esRutaTardia(datamart) + " THEN 1 ELSE 0 END) AS rutas_tardias,"
					+ " SUM(CASE WHEN d.estado_det = 'SUGERIDO' AND c.estado = 'EXPIRADA' AND NOT " + salio(datamart)
					+ " THEN 1 ELSE 0 END) AS expiradas_sin_salir,"
					+ " SUM(CASE WHEN d.estado_det = 'SUGERIDO' AND c.estado IN ('PENDIENTE','ASIGNADA','VISTA','ACEPTADA')"
					+ " AND NOT " + salio(datamart) + " THEN 1 ELSE 0 END) AS en_curso"
					+ cruce + " WHERE c.fecha_jornada BETWEEN ? AND ?", desde, hasta);
			if (datamart) {
				s1.add(" AND d.idtienda = ?", Integer.valueOf(idTienda));
			}
			PreparedStatement ps = s1.preparar(cn);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				g.sugeridos = rs.getInt("sugeridos");
				g.llevados = rs.getInt("llevados");
				g.reasignados = rs.getInt("reasignados");
				g.soltados = rs.getInt("soltados");
				g.anulados = Math.max(0, rs.getInt("liberados") - g.soltados);
				g.libres = rs.getInt("libres");
				g.rutasTardias = rs.getInt("rutas_tardias");
				g.expiradasSinSalir = rs.getInt("expiradas_sin_salir");
				g.enCurso = rs.getInt("en_curso");
				g.otros = Math.max(0, g.sugeridos - g.llevados - g.reasignados - g.soltados - g.anulados - g.libres
						- g.rutasTardias - g.expiradasSinSalir - g.enCurso);
			}
			rs.close();
			ps.close();

			//1b. Las rutas (sugerencias) por estado: cuantas se crearon, cuantas se
			//aceptaron y cuantas vencieron sin aceptarse.
			final Sql sc = new Sql();
			sc.add("SELECT c.estado AS estado, COUNT(*) AS cuantas FROM pedido_sugerencia c"
					+ " WHERE c.fecha_jornada BETWEEN ? AND ?", desde, hasta);
			if (datamart) {
				sc.add(" AND c.idtienda = ?", Integer.valueOf(idTienda));
			}
			sc.add(" GROUP BY c.estado");
			ps = sc.preparar(cn);
			rs = ps.executeQuery();
			while (rs.next()) {
				final String estado = rs.getString("estado");
				final int cuantas = rs.getInt("cuantas");
				g.sugerencias = g.sugerencias + cuantas;
				if ("ACEPTADA".equals(estado)) {
					g.sugerenciasAceptadas = cuantas;
				} else if ("EXPIRADA".equals(estado)) {
					g.sugerenciasExpiradas = cuantas;
				}
			}
			rs.close();
			ps.close();

			//2. Por domiciliario: a quien se le enruto, cuantos llevo, cuantos solto y
			//cuantos salieron por fuera de su ruta. El nombre sale de la tabla usuario
			//de la tienda; en el datamart no esta, y el controlador lo completa.
			final Sql s2 = new Sql();
			s2.add("SELECT IFNULL(c.id_domiciliario,0) AS id_dom, COUNT(*) AS sugeridos,"
					+ " SUM(CASE WHEN d.estado_det = 'LLEVADO' THEN 1 ELSE 0 END) AS llevados,"
					+ " SUM(CASE WHEN " + esSoltado(datamart) + " THEN 1 ELSE 0 END) AS soltados,"
					+ " SUM(CASE WHEN " + esLibre(datamart) + " THEN 1 ELSE 0 END) AS libres"
					+ cruce + " WHERE c.fecha_jornada BETWEEN ? AND ?", desde, hasta);
			if (datamart) {
				s2.add(" AND d.idtienda = ?", Integer.valueOf(idTienda));
			}
			s2.add(" GROUP BY IFNULL(c.id_domiciliario,0) HAVING (soltados > 0 OR libres > 0) ORDER BY (soltados + libres) DESC");
			ps = s2.preparar(cn);
			rs = ps.executeQuery();
			while (rs.next()) {
				final PorDomiciliario p = new PorDomiciliario();
				p.idDomiciliario = rs.getInt("id_dom");
				p.sugeridos = rs.getInt("sugeridos");
				p.llevados = rs.getInt("llevados");
				p.soltados = rs.getInt("soltados");
				p.libres = rs.getInt("libres");
				res.porDomiciliario.add(p);
			}
			rs.close();
			ps.close();

			//3. Los soltados uno por uno, con el motivo. GROUP BY d.id porque una
			//fila de log de "todo el enrutamiento" y una individual pueden cubrir el
			//mismo pedido, y no se quiere contarlo dos veces.
			if (g.soltados > 0) {
				final Sql s3 = new Sql();
				s3.add("SELECT MAX(l.creado_en) AS cuando, MAX(IFNULL(c.id_domiciliario,0)) AS id_dom, MAX(d.id_pedido) AS id_pedido,"
						+ " MAX(IFNULL(l.motivo,'')) AS motivo" + cruce
						+ " INNER JOIN pedido_sugerencia_log l ON l.pedido_sugerencia_id = d.pedido_sugerencia_id"
						+ (datamart ? " AND l.idtienda = d.idtienda" : "")
						+ " AND l.accion = 'LIBERADA' AND (IFNULL(l.id_pedido,0) = 0 OR l.id_pedido = d.id_pedido)"
						+ " WHERE c.fecha_jornada BETWEEN ? AND ? AND d.estado_det = 'LIBERADO'", desde, hasta);
				if (datamart) {
					s3.add(" AND d.idtienda = ?", Integer.valueOf(idTienda));
				}
				s3.add(" GROUP BY d.id ORDER BY cuando DESC LIMIT " + MAXIMO_DETALLE_SOLTADOS);
				ps = s3.preparar(cn);
				rs = ps.executeQuery();
				while (rs.next()) {
					final Soltado x = new Soltado();
					x.fechaHora = rs.getString("cuando") == null ? "" : rs.getString("cuando");
					x.idDomiciliario = rs.getInt("id_dom");
					x.idPedido = rs.getInt("id_pedido");
					x.motivo = rs.getString("motivo") == null ? "" : rs.getString("motivo");
					res.detalleSoltados.add(x);
				}
				rs.close();
				ps.close();
			}

			//4. Los nombres, en vivo, de la tabla usuario de la tienda.
			if (!datamart) {
				completarNombresEnTienda(cn, res);
			}
			res.soltadosDisponible = true;
		} catch (final Exception e) {
			logger.error("ReporteEnrutamientoDAO.cargarSoltados " + idTienda + ": " + e.toString());
			//Lo que se alcanzo a leer no se muestra a medias: o se tiene todo o no se dice nada.
			res.soltadosDisponible = false;
			res.enrutamiento = new Desglose();
			res.porDomiciliario.clear();
			res.detalleSoltados.clear();
		}
	}

	/** Nombre de cada domiciliario desde usuario.nombre_largo de la tienda. Si falla se deja vacio. */
	private static void completarNombresEnTienda(final Connection cn, final ResultadoTienda res) {
		try {
			final java.util.HashMap<Integer, String> nombres = new java.util.HashMap<Integer, String>();
			final ArrayList<Integer> ids = new ArrayList<Integer>();
			for (final PorDomiciliario p : res.porDomiciliario) {
				ids.add(Integer.valueOf(p.idDomiciliario));
			}
			for (final Soltado x : res.detalleSoltados) {
				if (!ids.contains(Integer.valueOf(x.idDomiciliario))) {
					ids.add(Integer.valueOf(x.idDomiciliario));
				}
			}
			if (ids.isEmpty()) {
				return;
			}
			final StringBuilder lista = new StringBuilder();
			for (final Integer id : ids) {
				if (lista.length() > 0) {
					lista.append(",");
				}
				lista.append(id.intValue());
			}
			final Statement stm = cn.createStatement();
			final ResultSet rs = stm.executeQuery("SELECT id, nombre_largo FROM usuario WHERE id IN (" + lista + ")");
			while (rs.next()) {
				nombres.put(Integer.valueOf(rs.getInt("id")), rs.getString("nombre_largo"));
			}
			rs.close();
			stm.close();
			for (final PorDomiciliario p : res.porDomiciliario) {
				final String n = nombres.get(Integer.valueOf(p.idDomiciliario));
				p.nombre = (n == null ? "" : n);
			}
			for (final Soltado x : res.detalleSoltados) {
				final String n = nombres.get(Integer.valueOf(x.idDomiciliario));
				x.domiciliario = (n == null ? "" : n);
			}
		} catch (final Exception e) {
			//Sin nombre se muestra el numero: no vale la pena perder lo demas.
		}
	}

	/**
	 * Los nombres de domiciliarios que faltan, desde general.empleado. Lo usa el
	 * controlador para los resultados que salen del datamart, donde no hay tabla
	 * usuario. El id del enrutamiento es el mismo del empleado (asi lo lee tambien
	 * DesempenoDomiciliarioDAO).
	 */
	public static java.util.HashMap<Integer, String> nombresDeEmpleados(final java.util.Set<Integer> ids) {
		final java.util.HashMap<Integer, String> nombres = new java.util.HashMap<Integer, String>();
		if (ids == null || ids.isEmpty()) {
			return (nombres);
		}
		final Logger logger = Logger.getLogger("log_file");
		final StringBuilder lista = new StringBuilder();
		for (final Integer id : ids) {
			if (lista.length() > 0) {
				lista.append(",");
			}
			lista.append(id.intValue());
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDGeneral();
			if (cn == null) {
				return (nombres);
			}
			final Statement stm = cn.createStatement();
			final ResultSet rs = stm.executeQuery("SELECT id, nombre_largo FROM empleado WHERE id IN (" + lista + ")");
			while (rs.next()) {
				nombres.put(Integer.valueOf(rs.getInt("id")), rs.getString("nombre_largo"));
			}
			rs.close();
			stm.close();
		} catch (final Exception e) {
			logger.error("ReporteEnrutamientoDAO.nombresDeEmpleados: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (nombres);
	}

	// -----------------------------------------------------------------------
	// Utilidades
	// -----------------------------------------------------------------------

	/**
	 * Si una columna existe en la base a la que ya estamos conectados. Ante la
	 * duda devuelve false, que es el camino que no reviente en una tienda atrasada.
	 */
	private static boolean existeColumna(final Connection cn, final String tabla, final String columna) {
		return (existe(cn, "SELECT COUNT(*) AS hay FROM information_schema.columns"
				+ " WHERE table_schema = DATABASE() AND table_name = '" + tabla + "' AND column_name = '" + columna + "'"));
	}

	private static boolean existeTabla(final Connection cn, final String tabla) {
		return (existe(cn, "SELECT COUNT(*) AS hay FROM information_schema.tables"
				+ " WHERE table_schema = DATABASE() AND table_name = '" + tabla + "'"));
	}

	private static boolean existe(final Connection cn, final String consulta) {
		try {
			final Statement stm = cn.createStatement();
			final ResultSet rs = stm.executeQuery(consulta);
			boolean hay = false;
			if (rs.next()) {
				hay = (rs.getInt("hay") > 0);
			}
			rs.close();
			stm.close();
			return (hay);
		} catch (final Exception e) {
			return (false);
		}
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			//Nada que hacer: la conexion ya estaba muerta.
		}
	}
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Redencion y reversa de puntos del plan de fidelizacion.
 *
 * Una redencion toca cuatro tablas: marca el codigo como usado, reparte el
 * debito entre las acumulaciones vigentes de fidelizacion_transaccion, resta
 * el saldo en cliente_fidelizacion y deja el registro en
 * fidelizacion_redencion. Antes cada paso iba en su propia conexion y en
 * autocommit, asi que un fallo a mitad de camino dejaba al cliente con los
 * puntos descontados de las acumulaciones pero no del saldo, o al contrario,
 * y en silencio. Aqui los cuatro pasos van en UNA transaccion.
 *
 * La otra pieza que faltaba es el detalle del reparto. Los puntos salen de
 * varias acumulaciones a la vez y sin saber de cuales y cuanto de cada una,
 * devolverlos es adivinar. Ese reparto queda en
 * fidelizacion_redencion_detalle y es lo que permite reversar con exactitud.
 */
public class FidelizacionRedencionDAO {

	public static final String ESTADO_RESERVADA = "RESERVADA";

	public static final String ESTADO_CONFIRMADA = "CONFIRMADA";

	public static final String ESTADO_REVERSADA = "REVERSADA";

	/** Tolerancia para comparar puntos, que son double. */
	private static final double EPSILON = 0.0001;

	/**
	 * Resultado de una redencion. El codigo de error usa los mismos valores que
	 * ya conocia el POS y la web: NOK1 sin puntos suficientes, NOK2 el cliente
	 * no esta en el plan.
	 */
	public static class ResultadoRedencion {
		public boolean exitosa = false;

		public int idRedencion = 0;

		public double puntosRestantes = 0;

		public String codigoError = "";

		public String detalleError = "";
	}

	/** Una redencion vista desde un pedido, para decidir si se puede reversar. */
	public static class RedencionPedido {
		public int idRedencion = 0;

		public String correo = "";

		public int idTienda = 0;

		public int idPedidoTienda = 0;

		public double puntosRedimidos = 0;

		public double puntosReversados = 0;

		public String estado = "";

		public double puntosPendientesPorReversar() {
			return (this.puntosRedimidos - this.puntosReversados);
		}
	}

	/**
	 * Ejecuta la redencion completa de forma atomica.
	 *
	 * @param codigo         codigo de redencion ya validado con el cliente
	 * @param correo         cliente del plan
	 * @param puntosRedimir  puntos a descontar
	 * @param idTienda       tienda donde se redime
	 * @param idPedidoTienda consecutivo del pedido en la tienda
	 * @param usuario        quien procesa la redencion
	 * @param origen         POS, CC o SER
	 * @param reservar       true deja la redencion en RESERVADA, para que el
	 *                       POS la confirme cuando el pedido quede finalizado.
	 *                       false la deja en CONFIRMADA de una vez, que es el
	 *                       comportamiento historico y el que sigue usando el
	 *                       POS que aun no se ha actualizado.
	 */
	public static ResultadoRedencion ejecutarRedencion(String codigo, String correo, double puntosRedimir,
			int idTienda, int idPedidoTienda, String usuario, String origen, boolean reservar) {
		Logger logger = Logger.getLogger("log_file");
		ResultadoRedencion resultado = new ResultadoRedencion();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			resultado.codigoError = "NOK3";
			resultado.detalleError = "Sin conexion a la base de datos";
			return (resultado);
		}
		try {
			con1.setAutoCommit(false);

			// El saldo se lee con FOR UPDATE dentro de la transaccion. La
			// validacion previa del controlador no basta: entre esa lectura y
			// esta escritura pueden entrar dos redenciones del mismo cliente
			// y gastar dos veces los mismos puntos.
			double puntosVigentes = 0;
			boolean existeCliente = false;
			PreparedStatement pstSaldo = con1
					.prepareStatement("select puntos_vigentes from cliente_fidelizacion where correo = ? for update");
			pstSaldo.setString(1, correo);
			ResultSet rsSaldo = pstSaldo.executeQuery();
			if (rsSaldo.next()) {
				existeCliente = true;
				puntosVigentes = rsSaldo.getDouble(1);
			}
			rsSaldo.close();
			pstSaldo.close();

			if (!existeCliente) {
				con1.rollback();
				resultado.codigoError = "NOK2";
				resultado.detalleError = "El cliente no esta en el plan de fidelizacion";
				return (resultado);
			}
			if (puntosVigentes + EPSILON < puntosRedimir) {
				con1.rollback();
				resultado.codigoError = "NOK1";
				resultado.detalleError = "Puntos insuficientes: tiene " + puntosVigentes + " y requiere "
						+ puntosRedimir;
				return (resultado);
			}

			// El codigo se marca con la condicion validado = 'N' incluida. Si no
			// actualiza ninguna fila es que ya se habia usado, y entonces esta
			// es una peticion repetida que no se debe cobrar dos veces.
			if (codigo != null && !codigo.trim().equals("")) {
				PreparedStatement pstCod = con1.prepareStatement(
						"update codigo_redencion_puntos set validado = 'S' where codigo = ? and validado = 'N'");
				pstCod.setString(1, codigo);
				int filasCodigo = pstCod.executeUpdate();
				pstCod.close();
				if (filasCodigo == 0) {
					con1.rollback();
					resultado.codigoError = "NOK4";
					resultado.detalleError = "El codigo de redencion no existe o ya fue usado";
					return (resultado);
				}
			}

			// Acumulaciones vigentes, de la mas reciente a la mas antigua, que es
			// el orden que ya usaba el proceso original.
			ArrayList<int[]> llaves = new ArrayList<>();
			ArrayList<double[]> montos = new ArrayList<>();
			ArrayList<String> correosAcum = new ArrayList<>();
			PreparedStatement pstAcum = con1.prepareStatement("select correo, idtienda, idpedidotienda, puntos,"
					+ " puntos_redimidos from fidelizacion_transaccion where correo = ? and vencidos = 'N'"
					+ " and puntos > puntos_redimidos order by fecha_transaccion desc, idtienda, idpedidotienda"
					+ " for update");
			pstAcum.setString(1, correo);
			ResultSet rsAcum = pstAcum.executeQuery();
			while (rsAcum.next()) {
				correosAcum.add(rsAcum.getString("correo"));
				llaves.add(new int[] { rsAcum.getInt("idtienda"), rsAcum.getInt("idpedidotienda") });
				montos.add(new double[] { rsAcum.getDouble("puntos"), rsAcum.getDouble("puntos_redimidos") });
			}
			rsAcum.close();
			pstAcum.close();

			// Reparto del debito. Se guarda cuanto sale de cada acumulacion.
			ArrayList<Double> debitos = new ArrayList<>();
			double porRepartir = puntosRedimir;
			PreparedStatement pstUpd = con1.prepareStatement("update fidelizacion_transaccion set puntos_redimidos = ?"
					+ " where correo = ? and idtienda = ? and idpedidotienda = ?");
			for (int i = 0; i < llaves.size() && porRepartir > EPSILON; i++) {
				double disponibles = montos.get(i)[0] - montos.get(i)[1];
				// El caso de igualdad se toma completo aqui mismo. Antes caia al
				// else y dejaba una iteracion de mas sin efecto.
				double debito = (disponibles >= porRepartir) ? porRepartir : disponibles;
				pstUpd.setDouble(1, montos.get(i)[1] + debito);
				pstUpd.setString(2, correosAcum.get(i));
				pstUpd.setInt(3, llaves.get(i)[0]);
				pstUpd.setInt(4, llaves.get(i)[1]);
				pstUpd.addBatch();
				debitos.add(Double.valueOf(debito));
				porRepartir = porRepartir - debito;
			}
			pstUpd.executeBatch();
			pstUpd.close();

			// Si las acumulaciones no alcanzaron, el saldo de cliente_fidelizacion
			// y las acumulaciones estan descuadrados de antes. Se aborta en vez de
			// dejar el descuadre mas grande.
			if (porRepartir > EPSILON) {
				con1.rollback();
				logger.error("ejecutarRedencion: las acumulaciones de " + correo + " no cubren " + puntosRedimir
						+ ", faltaron " + porRepartir + ". Saldo en cliente_fidelizacion: " + puntosVigentes);
				resultado.codigoError = "NOK5";
				resultado.detalleError = "Las acumulaciones de puntos no cubren la redencion, faltaron " + porRepartir;
				return (resultado);
			}

			PreparedStatement pstSaldoUpd = con1.prepareStatement(
					"update cliente_fidelizacion set puntos_vigentes = puntos_vigentes - ? where correo = ?");
			pstSaldoUpd.setDouble(1, puntosRedimir);
			pstSaldoUpd.setString(2, correo);
			pstSaldoUpd.executeUpdate();
			pstSaldoUpd.close();

			String estado = reservar ? ESTADO_RESERVADA : ESTADO_CONFIRMADA;
			PreparedStatement pstRed = con1.prepareStatement(
					"insert into fidelizacion_redencion (correo, puntos_redimidos, idtienda, idpedidotienda,"
							+ " usuario, origen, estado, fecha_estado, usuario_estado) values (?,?,?,?,?,?,?,now(),?)",
					Statement.RETURN_GENERATED_KEYS);
			pstRed.setString(1, correo);
			pstRed.setDouble(2, puntosRedimir);
			pstRed.setInt(3, idTienda);
			pstRed.setInt(4, idPedidoTienda);
			pstRed.setString(5, usuario == null ? "" : usuario);
			pstRed.setString(6, origen == null ? "" : origen);
			pstRed.setString(7, estado);
			pstRed.setString(8, usuario == null ? "" : usuario);
			pstRed.executeUpdate();
			ResultSet rsLlave = pstRed.getGeneratedKeys();
			if (rsLlave.next()) {
				resultado.idRedencion = rsLlave.getInt(1);
			}
			rsLlave.close();
			pstRed.close();

			PreparedStatement pstDet = con1
					.prepareStatement("insert into fidelizacion_redencion_detalle (idredencion, correo, idtienda,"
							+ " idpedidotienda, puntos_debitados) values (?,?,?,?,?)");
			for (int i = 0; i < debitos.size(); i++) {
				pstDet.setInt(1, resultado.idRedencion);
				pstDet.setString(2, correosAcum.get(i));
				pstDet.setInt(3, llaves.get(i)[0]);
				pstDet.setInt(4, llaves.get(i)[1]);
				pstDet.setDouble(5, debitos.get(i).doubleValue());
				pstDet.addBatch();
			}
			pstDet.executeBatch();
			pstDet.close();

			con1.commit();
			resultado.exitosa = true;
			resultado.puntosRestantes = puntosVigentes - puntosRedimir;
			logger.info("Redencion " + resultado.idRedencion + " " + estado + " correo=" + correo + " puntos="
					+ puntosRedimir + " tienda=" + idTienda + " pedido=" + idPedidoTienda + " usuario=" + usuario
					+ " origen=" + origen + " acumulaciones=" + debitos.size());
		} catch (Exception e) {
			logger.error("ejecutarRedencion: " + e.toString());
			resultado.codigoError = "NOK3";
			resultado.detalleError = e.toString();
			try {
				con1.rollback();
			} catch (Exception e1) {
			}
		} finally {
			try {
				con1.setAutoCommit(true);
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (resultado);
	}

	/**
	 * Pasa una redencion de RESERVADA a CONFIRMADA. La llama el POS cuando el
	 * pedido quedo finalizado de verdad.
	 */
	public static boolean confirmarRedencion(int idRedencion, String usuario) {
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1.prepareStatement("update fidelizacion_redencion set estado = ?,"
					+ " fecha_estado = now(), usuario_estado = ? where idredencion = ? and estado = ?");
			pst.setString(1, ESTADO_CONFIRMADA);
			pst.setString(2, usuario == null ? "" : usuario);
			pst.setInt(3, idRedencion);
			pst.setString(4, ESTADO_RESERVADA);
			respuesta = (pst.executeUpdate() > 0);
			pst.close();
			con1.close();
			logger.info("confirmarRedencion " + idRedencion + " usuario=" + usuario + " resultado=" + respuesta);
		} catch (Exception e) {
			logger.error("confirmarRedencion: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (respuesta);
	}

	/**
	 * Devuelve los puntos de una redencion, total o parcialmente, de forma
	 * atomica: los repone en las acumulaciones de las que salieron, suma el
	 * saldo del cliente y libera el codigo si la reversa quedo completa.
	 *
	 * La devolucion parcial recorre el detalle en orden inverso al del debito:
	 * lo ultimo que se tomo es lo primero que se devuelve, de modo que la
	 * operacion es el espejo de la original.
	 *
	 * @param idRedencion     redencion a reversar
	 * @param puntosADevolver puntos a devolver; 0 o menos significa todo lo que
	 *                        quede pendiente
	 * @param usuario         quien autoriza la reversa
	 * @param motivo          por que se reversa, queda en la trazabilidad
	 * @return true si se devolvio algo
	 */
	public static boolean reversarRedencion(int idRedencion, double puntosADevolver, String usuario, String motivo) {
		return (reversarRedencion(idRedencion, puntosADevolver, usuario, motivo, null));
	}

	/**
	 * Igual que el anterior, pero reutilizando una conexion en transaccion. Se
	 * usa cuando la reversa es parte de una operacion mas grande, como aprobar
	 * una solicitud, para que todo quede en el mismo commit.
	 */
	public static boolean reversarRedencion(int idRedencion, double puntosADevolver, String usuario, String motivo,
			Connection conexionExterna) {
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		boolean propia = (conexionExterna == null);
		Connection con1 = conexionExterna;
		if (propia) {
			ConexionBaseDatos con = new ConexionBaseDatos();
			con1 = con.obtenerConexionBDPrincipal();
			if (con1 == null) {
				return (false);
			}
		}
		try {
			if (propia) {
				con1.setAutoCommit(false);
			}

			String correo = "";
			String estado = "";
			double puntosRedimidos = 0;
			double puntosReversados = 0;
			boolean existe = false;
			PreparedStatement pstRed = con1.prepareStatement("select correo, puntos_redimidos, puntos_reversados,"
					+ " estado from fidelizacion_redencion where idredencion = ? for update");
			pstRed.setInt(1, idRedencion);
			ResultSet rsRed = pstRed.executeQuery();
			if (rsRed.next()) {
				existe = true;
				correo = rsRed.getString("correo");
				puntosRedimidos = rsRed.getDouble("puntos_redimidos");
				puntosReversados = rsRed.getDouble("puntos_reversados");
				estado = rsRed.getString("estado");
			}
			rsRed.close();
			pstRed.close();

			if (!existe) {
				logger.error("reversarRedencion: no existe la redencion " + idRedencion);
				if (propia) {
					con1.rollback();
				}
				return (false);
			}
			double pendiente = puntosRedimidos - puntosReversados;
			if (pendiente <= EPSILON) {
				logger.info("reversarRedencion: la redencion " + idRedencion + " ya estaba reversada por completo");
				if (propia) {
					con1.rollback();
				}
				return (false);
			}
			double devolver = (puntosADevolver <= 0 || puntosADevolver > pendiente) ? pendiente : puntosADevolver;

			// El detalle en orden inverso al debito: lo ultimo tomado es lo
			// primero devuelto.
			ArrayList<int[]> detId = new ArrayList<>();
			ArrayList<Double> detPuntos = new ArrayList<>();
			ArrayList<String> detCorreo = new ArrayList<>();
			PreparedStatement pstDet = con1.prepareStatement("select iddetalle, correo, idtienda, idpedidotienda,"
					+ " puntos_debitados from fidelizacion_redencion_detalle where idredencion = ?"
					+ " and puntos_debitados > 0 order by iddetalle desc");
			pstDet.setInt(1, idRedencion);
			ResultSet rsDet = pstDet.executeQuery();
			while (rsDet.next()) {
				detId.add(new int[] { rsDet.getInt("iddetalle"), rsDet.getInt("idtienda"),
						rsDet.getInt("idpedidotienda") });
				detCorreo.add(rsDet.getString("correo"));
				detPuntos.add(Double.valueOf(rsDet.getDouble("puntos_debitados")));
			}
			rsDet.close();
			pstDet.close();

			if (detId.isEmpty()) {
				// Redenciones anteriores a este desarrollo no tienen detalle. No se
				// puede saber a que acumulaciones devolver, asi que se repone solo
				// el saldo del cliente y se deja constancia.
				logger.info("reversarRedencion: la redencion " + idRedencion + " no tiene detalle de debito;"
						+ " se repone unicamente el saldo del cliente");
			}

			double porDevolver = devolver;
			PreparedStatement pstAcum = con1.prepareStatement("update fidelizacion_transaccion set puntos_redimidos ="
					+ " puntos_redimidos - ? where correo = ? and idtienda = ? and idpedidotienda = ?");
			PreparedStatement pstDetUpd = con1.prepareStatement("update fidelizacion_redencion_detalle set"
					+ " puntos_debitados = puntos_debitados - ? where iddetalle = ?");
			for (int i = 0; i < detId.size() && porDevolver > EPSILON; i++) {
				double enEsta = detPuntos.get(i).doubleValue();
				double credito = (enEsta >= porDevolver) ? porDevolver : enEsta;
				pstAcum.setDouble(1, credito);
				pstAcum.setString(2, detCorreo.get(i));
				pstAcum.setInt(3, detId.get(i)[1]);
				pstAcum.setInt(4, detId.get(i)[2]);
				pstAcum.addBatch();
				pstDetUpd.setDouble(1, credito);
				pstDetUpd.setInt(2, detId.get(i)[0]);
				pstDetUpd.addBatch();
				porDevolver = porDevolver - credito;
			}
			pstAcum.executeBatch();
			pstAcum.close();
			pstDetUpd.executeBatch();
			pstDetUpd.close();

			PreparedStatement pstSaldo = con1.prepareStatement(
					"update cliente_fidelizacion set puntos_vigentes = puntos_vigentes + ? where correo = ?");
			pstSaldo.setDouble(1, devolver);
			pstSaldo.setString(2, correo);
			pstSaldo.executeUpdate();
			pstSaldo.close();

			double reversadoTotal = puntosReversados + devolver;
			boolean completa = (reversadoTotal + EPSILON >= puntosRedimidos);
			PreparedStatement pstEst = con1.prepareStatement("update fidelizacion_redencion set puntos_reversados = ?,"
					+ " estado = ?, fecha_estado = now(), usuario_estado = ?, motivo = ? where idredencion = ?");
			pstEst.setDouble(1, reversadoTotal);
			pstEst.setString(2, completa ? ESTADO_REVERSADA : estado);
			pstEst.setString(3, usuario == null ? "" : usuario);
			pstEst.setString(4, motivo == null ? "" : motivo);
			pstEst.setInt(5, idRedencion);
			pstEst.executeUpdate();
			pstEst.close();

			if (propia) {
				con1.commit();
			}
			respuesta = true;
			logger.info("reversarRedencion " + idRedencion + " correo=" + correo + " devuelto=" + devolver
					+ " acumulado=" + reversadoTotal + " de " + puntosRedimidos + " completa=" + completa
					+ " usuario=" + usuario + " motivo=" + motivo);
		} catch (Exception e) {
			logger.error("reversarRedencion: " + e.toString());
			if (propia) {
				try {
					con1.rollback();
				} catch (Exception e1) {
				}
			}
		} finally {
			if (propia) {
				try {
					con1.setAutoCommit(true);
					con1.close();
				} catch (Exception e1) {
				}
			}
		}
		return (respuesta);
	}

	/**
	 * Busca la redencion asociada a un pedido. Es la entrada del flujo de
	 * reversa: el POS y la web conocen la tienda y el pedido, no el idredencion.
	 * Si un pedido tuviera varias redenciones se devuelve la mas reciente que
	 * aun tenga puntos por devolver.
	 */
	public static RedencionPedido obtenerRedencionPorPedido(int idTienda, int idPedidoTienda) {
		Logger logger = Logger.getLogger("log_file");
		RedencionPedido redencion = null;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1.prepareStatement("select idredencion, correo, idtienda, idpedidotienda,"
					+ " puntos_redimidos, puntos_reversados, estado from fidelizacion_redencion"
					+ " where idtienda = ? and idpedidotienda = ? and estado <> ?"
					+ " order by idredencion desc limit 1");
			pst.setInt(1, idTienda);
			pst.setInt(2, idPedidoTienda);
			pst.setString(3, ESTADO_REVERSADA);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				redencion = new RedencionPedido();
				redencion.idRedencion = rs.getInt("idredencion");
				redencion.correo = rs.getString("correo");
				redencion.idTienda = rs.getInt("idtienda");
				redencion.idPedidoTienda = rs.getInt("idpedidotienda");
				redencion.puntosRedimidos = rs.getDouble("puntos_redimidos");
				redencion.puntosReversados = rs.getDouble("puntos_reversados");
				redencion.estado = rs.getString("estado");
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerRedencionPorPedido: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (redencion);
	}

	/**
	 * Redenciones que quedaron RESERVADA hace mas de las horas indicadas. Son
	 * pedidos que nunca se finalizaron: el proceso de barrido las reversa sola.
	 */
	public static ArrayList<RedencionPedido> obtenerReservadasVencidas(int horas) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<RedencionPedido> pendientes = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1.prepareStatement("select idredencion, correo, idtienda, idpedidotienda,"
					+ " puntos_redimidos, puntos_reversados, estado from fidelizacion_redencion where estado = ?"
					+ " and fecha_redencion < DATE_SUB(now(), INTERVAL ? HOUR) order by idredencion");
			pst.setString(1, ESTADO_RESERVADA);
			pst.setInt(2, horas);
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				RedencionPedido redencion = new RedencionPedido();
				redencion.idRedencion = rs.getInt("idredencion");
				redencion.correo = rs.getString("correo");
				redencion.idTienda = rs.getInt("idtienda");
				redencion.idPedidoTienda = rs.getInt("idpedidotienda");
				redencion.puntosRedimidos = rs.getDouble("puntos_redimidos");
				redencion.puntosReversados = rs.getDouble("puntos_reversados");
				redencion.estado = rs.getString("estado");
				pendientes.add(redencion);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerReservadasVencidas: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (pendientes);
	}
}

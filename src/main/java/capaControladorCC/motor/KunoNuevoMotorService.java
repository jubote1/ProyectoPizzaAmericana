package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;

import capaDAOCC.TiempoPedidoDAO;
import capaModeloCC.Cliente;
import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;
import conexionCC.ConexionBaseDatos;

/**
 * Servicio de Ejecución del Nuevo Motor para pedidos de Tienda Virtual Kuno y App.
 *
 * Se encarga de:
 *  1. Traducir los items y opciones de Kuno mediante TraductorKuno.
 *  2. Cotizar el pedido con MotorPedidoV1.
 *  3. Crear encabezado central y proyectar en pos_central (ProyectorPedidoLegacy).
 *  4. Despachar directamente a la tienda mediante ConectorTiendaPos si es tienda piloto.
 *  5. Gestionar la auditoría paralela asíncrona cuando el canal está en modo PARALELO.
 */
public class KunoNuevoMotorService {

	private static final Logger logger = Logger.getLogger("log_file");

	public static class ResultadoProceso {
		public boolean exito = false;
		public int idPedido = 0;
		public double total = 0.0;
		public String mensaje = "";
		public MotorResultado motorResultado;
	}

	/**
	 * Procesa un pedido de Kuno por el camino nuevo completo.
	 */
	public static ResultadoProceso procesarPedidoNuevo(
			JSONArray jsonItemsArray,
			int idTienda,
			Cliente cliente,
			String strFechaFinal,
			long idOrdenComercio,
			int idTipoPedido,
			String origenPedido,
			String fuentePedido,
			int idFormaPago,
			String programado,
			String horaProgramado,
			int idEstadoPedido) {

		ResultadoProceso rp = new ResultadoProceso();
		Connection con = null;

		try {
			con = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			// 1. Cargar catálogo y traducir orden
			CatalogoPos cat = CatalogoPos.cargar(con);
			TraductorKuno traductor = new TraductorKuno(con, cat);
			String canal = (fuentePedido != null && fuentePedido.toLowerCase().contains("app")) ? "APP" : "TK";
			boolean esProg = "S".equalsIgnoreCase(programado);

			int idCliente = (cliente != null) ? cliente.getIdcliente() : 0;
			MotorSolicitud solicitud = traductor.traducir(jsonItemsArray, idTienda, idCliente, idTipoPedido, canal, esProg, horaProgramado);

			// 2. Cotizar con el motor
			MotorPedidoV1 motor = new MotorPedidoV1(cat);
			MotorResultado res = motor.cotizar(solicitud);
			rp.motorResultado = res;

			if (!MotorResultado.OK.equals(res.estado)) {
				rp.exito = false;
				rp.mensaje = "Motor rechazo la cotizacion: " + res.estado;
				return rp;
			}

			// 3. Crear encabezado inicial en pizzaamericana.pedido
			String usuarioPedido = (fuentePedido != null && fuentePedido.contains("web")) ? "TIENDA VIRTUAL" : "APP";
			int tiempoPedido = TiempoPedidoDAO.retornarTiempoPedidoTienda(idTienda);
			if (tiempoPedido <= 0) tiempoPedido = 30;

			int idPedidoCentral = 0;
			String sqlIns = "insert into pizzaamericana.pedido (idtienda, idcliente, idestadopedido, fechapedido, "
					+ "usuariopedido, total_bruto, descuento, total_neto, tiempopedido, origen, programado, hora_programado, "
					+ "idtipopedido, idordencomercio) "
					+ "values (?,?,?,current_date(),?,?,?,?,?,?,?,?,?,?)";

			try (PreparedStatement ps = con.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, idTienda);
				ps.setInt(2, idCliente);
				ps.setInt(3, idEstadoPedido > 0 ? idEstadoPedido : 2);
				ps.setString(4, usuarioPedido);
				ps.setDouble(5, res.subtotal);
				ps.setDouble(6, res.descuento);
				ps.setDouble(7, res.total);
				ps.setDouble(8, (double) tiempoPedido);
				ps.setString(9, origenPedido != null ? origenPedido : "TK");
				ps.setString(10, programado != null ? programado : "N");
				ps.setString(11, horaProgramado != null ? horaProgramado : "");
				ps.setInt(12, idTipoPedido > 0 ? idTipoPedido : 1);
				ps.setString(13, String.valueOf(idOrdenComercio));
				ps.executeUpdate();

				try (ResultSet rs = ps.getGeneratedKeys()) {
					if (rs.next()) {
						idPedidoCentral = rs.getInt(1);
					}
				}
			}

			if (idPedidoCentral <= 0) {
				rp.exito = false;
				rp.mensaje = "No se genero el idpedido central.";
				return rp;
			}

			// 4. Registrar forma de pago en pizzaamericana.pedido_forma_pago
			String sqlFP = "insert into pizzaamericana.pedido_forma_pago (idpedido, idforma_pago, valortotal, valorformapago) "
					+ "values (?, ?, ?, ?)";
			try (PreparedStatement psFP = con.prepareStatement(sqlFP)) {
				psFP.setInt(1, idPedidoCentral);
				psFP.setInt(2, idFormaPago > 0 ? idFormaPago : 1);
				psFP.setDouble(3, res.total);
				psFP.setDouble(4, res.total);
				psFP.executeUpdate();
			}

			// 5. Proyectar en pos_central y pizzaamericana.detalle_pedido
			ProyectorPedidoLegacy.proyectarPedido(con, idPedidoCentral, idTienda, idCliente, solicitud, res, usuarioPedido);

			// 6. Si es tienda piloto, despachar directamente por ConectorTiendaPos
			boolean esPiloto = EnrutadorDespachoTienda.esTiendaPiloto(con, idTienda);
			if (esPiloto) {
				ConectorTiendaPos.despachar(con, idPedidoCentral, idTienda, cliente, solicitud, res, idFormaPago,
						res.total, usuarioPedido, (double) tiempoPedido, origenPedido);
			}

			rp.exito = true;
			rp.idPedido = idPedidoCentral;
			rp.total = res.total;
			rp.mensaje = "Pedido procesado exitosamente por Nuevo Motor (#" + idPedidoCentral + ")";
			logger.info("KunoNuevoMotorService: pedido " + idOrdenComercio + " procesado exitosamente como #" + idPedidoCentral);

		} catch (Throwable t) {
			rp.exito = false;
			rp.mensaje = "Error procesando en nuevo motor: " + t.getMessage();
			logger.error("KunoNuevoMotorService fallo: " + t.getMessage(), t);
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (Exception ignorada) {
				}
			}
		}

		return rp;
	}

	/**
	 * Envía a ejecución paralela asíncrona en sombra para comparar resultados sin demorar la respuesta.
	 */
	public static void auditarParalelo(
			final JSONArray jsonItemsArray,
			final int idTienda,
			final int idCliente,
			final int idTipoPedido,
			final String fuentePedido,
			final boolean esProgramado,
			final String horaProgramado,
			final int idPedidoCreado,
			final long idOrdenComercio,
			final double totalLegacy) {

		try {
			final String canal = (fuentePedido != null && fuentePedido.toLowerCase().contains("app")) ? "APP" : "KUNO";
			new Thread(() -> {
				Connection con = null;
				try {
					con = new ConexionBaseDatos().obtenerConexionBDPrincipal();
					CatalogoPos cat = CatalogoPos.cargar(con);
					TraductorKuno traductor = new TraductorKuno(con, cat);
					MotorSolicitud sol = traductor.traducir(jsonItemsArray, idTienda, idCliente, idTipoPedido, canal, esProgramado, horaProgramado);

					EnrutadorCanalesMotor.registrarParaleloAsincrono(
							canal, idPedidoCreado, String.valueOf(idOrdenComercio), idTienda,
							totalLegacy, totalLegacy, sol);
				} catch (Throwable t) {
					logger.warn("Advertencia en auditarParalelo Kuno: " + t.getMessage());
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (Exception ignorada) {
						}
					}
				}
			}, "auditoria-kuno-paralelo").start();
		} catch (Throwable t) {
			// No afecta
		}
	}
}

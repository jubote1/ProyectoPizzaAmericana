package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.PagoTiendaConciliacion;
import conexionCC.ConexionBaseDatos;

/**
 * Consulta en vivo, contra la base local de una tienda, los pagos QR o
 * Datafono registrados ahi -para que quien esta resolviendo una diferencia de
 * conciliacion (ver capaDAOCC.SolicitudConciliacionDAO) pueda cruzarlos a ojo
 * contra lo que reporto la tienda, sin ir a buscarlos a mano.
 *
 * Requiere que el computador de la tienda este encendido: usa
 * conexionCC.ConexionBaseDatos.obtenerConexionBDTiendaRemota(hosbd), que da
 * timeout a los 10 segundos si no hay respuesta.
 */
public class ConciliacionTiendaDAO {

	/** idforma_pago en la tabla local forma_pago. Confirmado con el usuario 2026-09-16. */
	public static final int IDFORMAPAGO_DATAFONO = 2;

	public static final int IDFORMAPAGO_QR = 7;

	/**
	 * @param hosbd      tienda.hosbd de la tienda a consultar
	 * @param origen     "DATAFONO" o "QR" (mismos valores que solicitud_conciliacion.origen)
	 * @param fechaDesde en yyyy-MM-dd
	 * @param fechaHasta en yyyy-MM-dd
	 */
	public static ArrayList<PagoTiendaConciliacion> consultarPagosTienda(String hosbd, String origen,
			String fechaDesde, String fechaHasta) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<PagoTiendaConciliacion> pagos = new ArrayList<>();
		int idFormaPago = "QR".equalsIgnoreCase(origen) ? IDFORMAPAGO_QR : IDFORMAPAGO_DATAFONO;

		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDTiendaRemota(hosbd);
		if (con1 == null) {
			logger.error("ConciliacionTiendaDAO: no se pudo conectar a la tienda " + hosbd);
			return (null);
		}
		try {
			String consulta = "select a.idpedidotienda, a.fechapedido, "
					+ "concat_ws(' ', c.nombre, c.apellido) as nombrecliente, "
					+ "c.telefono, c.telefono_celular, b.valorformapago, a.total_neto, "
					+ "b.datafono, a.idmotivoanulacion, a.estacion "
					+ "from pedido a, pedido_forma_pago b, cliente c "
					+ "where a.idpedidotienda = b.idpedidotienda and a.idcliente = c.idcliente "
					+ "and b.idforma_pago = ? and a.fechapedido >= ? and a.fechapedido <= ? "
					+ "order by a.fechapedido, a.idpedidotienda";
			PreparedStatement ps = con1.prepareStatement(consulta);
			ps.setInt(1, idFormaPago);
			ps.setString(2, fechaDesde);
			ps.setString(3, fechaHasta);
			logger.info(consulta + " [" + idFormaPago + ", " + fechaDesde + ", " + fechaHasta + "] en " + hosbd);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				PagoTiendaConciliacion pago = new PagoTiendaConciliacion();
				pago.setIdPedidoTienda(rs.getInt("idpedidotienda"));
				pago.setFecha(rs.getString("fechapedido"));
				pago.setNombreCliente(rs.getString("nombrecliente"));
				String telCelular = rs.getString("telefono_celular");
				pago.setTelefono((telCelular != null && !telCelular.trim().isEmpty()) ? telCelular
						: rs.getString("telefono"));
				pago.setValorFormaPago(rs.getDouble("valorformapago"));
				pago.setTotalNeto(rs.getDouble("total_neto"));
				pago.setReferenciaDatafono(rs.getString("datafono"));
				pago.setAnulado(rs.getObject("idmotivoanulacion") != null);
				pago.setEstacion(rs.getString("estacion"));
				pagos.add(pago);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (Exception e) {
			logger.error("ConciliacionTiendaDAO.consultarPagosTienda en " + hosbd + ": " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			return (null);
		}
		return (pagos);
	}

}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Registro que deja el POS cuando la campana 15 minutos aplico a un pedido de
 * punto de venta, en el momento exacto en que se envia a cocina. Es la fuente
 * para el job de deteccion de incumplimientos en Servicios y para la hora que
 * se reimprime en la factura si hace falta.
 *
 * Insercion best-effort desde el POS: si esta llamada falla, el pedido queda
 * marcado solo localmente y no entra a la cola central de revision. Riesgo
 * aceptado, es herramienta de soporte, no la venta misma.
 */
public class Campana15MinAplicadoDAO {

	public static boolean registrar(int idPedidoTienda, int idTienda, int idCampana, String fechaHoraInicio,
			double valorBasePizza, String nombreCliente, String celularCliente) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean ok = false;
		try {
			PreparedStatement pst = con1.prepareStatement("insert into campana_15min_aplicado (idpedidotienda,"
					+ " idtienda, idcampana, fecha_hora_inicio, valor_base_pizza, nombre_cliente, celular_cliente)"
					+ " values (?,?,?,?,?,?,?)"
					+ " on duplicate key update fecha_hora_inicio = values(fecha_hora_inicio),"
					+ " valor_base_pizza = values(valor_base_pizza), nombre_cliente = values(nombre_cliente),"
					+ " celular_cliente = values(celular_cliente)");
			pst.setInt(1, idPedidoTienda);
			pst.setInt(2, idTienda);
			pst.setInt(3, idCampana);
			pst.setString(4, fechaHoraInicio);
			pst.setDouble(5, valorBasePizza);
			pst.setString(6, nombreCliente == null ? "" : nombreCliente);
			pst.setString(7, celularCliente == null ? "" : celularCliente);
			pst.executeUpdate();
			pst.close();
			con1.close();
			ok = true;
			logger.info("Campana15MinAplicadoDAO.registrar pedido=" + idPedidoTienda + " tienda=" + idTienda
					+ " inicio=" + fechaHoraInicio + " valor=" + valorBasePizza);
		} catch (Exception e) {
			logger.error("registrar: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (ok);
	}

	/** Completa la forma de pago cuando se cierra el pago del pedido, mas tarde en el flujo. */
	public static boolean registrarFormaPago(int idPedidoTienda, int idTienda, boolean esMedioVirtual) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean ok = false;
		try {
			PreparedStatement pst = con1.prepareStatement("update campana_15min_aplicado set"
					+ " idformapago_virtual = ? where idpedidotienda = ? and idtienda = ?");
			pst.setString(1, esMedioVirtual ? "S" : "N");
			pst.setInt(2, idPedidoTienda);
			pst.setInt(3, idTienda);
			int filas = pst.executeUpdate();
			pst.close();
			con1.close();
			ok = filas > 0;
		} catch (Exception e) {
			logger.error("registrarFormaPago: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (ok);
	}

	public static class Aplicado {
		public int idPedidoTienda = 0;

		public int idTienda = 0;

		public int idCampana = 0;

		public String fechaHoraInicio = "";

		public double valorBasePizza = 0;

		public String idFormaPagoVirtual = "";

		public String estadoCumplido = "";
	}

	/** Pedidos con campana aplicada, vencidos y aun sin evaluar. Lo usa el job de Servicios. */
	public static java.util.ArrayList<Aplicado> obtenerVencidosSinEvaluar(int minutosPromesa) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		java.util.ArrayList<Aplicado> lista = new java.util.ArrayList<>();
		try {
			PreparedStatement pst = con1.prepareStatement("select * from campana_15min_aplicado"
					+ " where estado_cumplido is null and timestampdiff(minute, fecha_hora_inicio, now()) > ?");
			pst.setInt(1, minutosPromesa);
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				Aplicado a = new Aplicado();
				a.idPedidoTienda = rs.getInt("idpedidotienda");
				a.idTienda = rs.getInt("idtienda");
				a.idCampana = rs.getInt("idcampana");
				a.fechaHoraInicio = rs.getString("fecha_hora_inicio");
				a.valorBasePizza = rs.getDouble("valor_base_pizza");
				a.idFormaPagoVirtual = rs.getString("idformapago_virtual") == null ? ""
						: rs.getString("idformapago_virtual");
				a.estadoCumplido = rs.getString("estado_cumplido") == null ? "" : rs.getString("estado_cumplido");
				lista.add(a);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerVencidosSinEvaluar: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (lista);
	}

	public static void marcarEvaluado(int idPedidoTienda, int idTienda, boolean cumplido) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1.prepareStatement(
					"update campana_15min_aplicado set estado_cumplido = ? where idpedidotienda = ? and idtienda = ?");
			pst.setString(1, cumplido ? "S" : "N");
			pst.setInt(2, idPedidoTienda);
			pst.setInt(3, idTienda);
			pst.executeUpdate();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("marcarEvaluado: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
	}
}

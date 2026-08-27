package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.EncuestaMercadeo;
import conexionCC.ConexionBaseDatos;

/**
 * DAO de las respuestas de encuestas de mercadeo tomadas en el POS.
 * Trabaja sobre la tabla encuesta_mercadeo de la base de datos pizzaamericana.
 *
 * Se usa PreparedStatement porque las respuestas abiertas son texto libre digitado
 * por el operador del POS y un apostrofo romperia una sentencia concatenada.
 */
public class EncuestaMercadeoDAO {

	/**
	 * Inserta todas las respuestas de una encuesta en una sola transaccion, de modo
	 * que una encuesta queda completa o no queda.
	 *
	 * @param respuestas respuestas a insertar, una por pregunta contestada
	 * @return cantidad de respuestas insertadas; 0 si fallo o si la lista venia vacia
	 */
	public static int insertarRespuestas(final ArrayList<EncuestaMercadeo> respuestas) {
		int insertadas = 0;
		final Logger logger = Logger.getLogger("log_file");
		if (respuestas == null || respuestas.isEmpty()) {
			return (0);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			con1.setAutoCommit(false);
			final String insert = "insert into encuesta_mercadeo (idpregunta, respuesta, idopcion, idpedidotienda, "
					+ "idtienda, idcliente, idusuario) values (?,?,?,?,?,?,?)";
			logger.info(insert + " cantidad=" + respuestas.size());
			final PreparedStatement pst = con1.prepareStatement(insert);
			for (final EncuestaMercadeo respuesta : respuestas) {
				pst.setInt(1, respuesta.getIdPregunta());
				pst.setString(2, respuesta.getRespuesta());
				pst.setInt(3, respuesta.getIdOpcion());
				pst.setInt(4, respuesta.getIdPedidoTienda());
				pst.setInt(5, respuesta.getIdTienda());
				pst.setInt(6, respuesta.getIdCliente());
				pst.setInt(7, respuesta.getIdUsuario());
				pst.addBatch();
			}
			final int[] resultados = pst.executeBatch();
			con1.commit();
			for (int i = 0; i < resultados.length; i++) {
				if (resultados[i] >= 0 || resultados[i] == PreparedStatement.SUCCESS_NO_INFO) {
					insertadas++;
				}
			}
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("insertarRespuestas: " + e.toString());
			revertir(con1);
			insertadas = 0;
		}
		return (insertadas);
	}

	/**
	 * Indica si un pedido ya tiene encuesta de mercadeo registrada, para no volver a
	 * preguntarle al cliente si el pedido se reabre.
	 *
	 * @param idPedidoTienda consecutivo del pedido en la tienda
	 * @param idTienda       tienda del pedido
	 * @return true si ya existe al menos una respuesta para ese pedido
	 */
	public static boolean existeEncuestaPedido(final int idPedidoTienda, final int idTienda) {
		boolean existe = false;
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final String consulta = "select count(*) from encuesta_mercadeo where idpedidotienda = "
					+ idPedidoTienda + " and idtienda = " + idTienda;
			logger.info(consulta);
			final java.sql.Statement stm = con1.createStatement();
			final java.sql.ResultSet rs = stm.executeQuery(consulta);
			if (rs.next()) {
				existe = rs.getInt(1) > 0;
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("existeEncuestaPedido: " + e.toString());
			cerrar(con1);
		}
		return (existe);
	}

	/**
	 * Revierte la transaccion y cierra, ignorando errores.
	 *
	 * @param con1 conexion a revertir, puede venir en null
	 */
	private static void revertir(final Connection con1) {
		try {
			if (con1 != null) {
				con1.rollback();
				con1.close();
			}
		} catch (final Exception e1) {
			// Se ignora: ya estamos manejando un error previo
		}
	}

	/**
	 * Cierra la conexion ignorando cualquier error, para usar en los bloques catch.
	 *
	 * @param con1 conexion a cerrar, puede venir en null
	 */
	private static void cerrar(final Connection con1) {
		try {
			if (con1 != null) {
				con1.close();
			}
		} catch (final Exception e1) {
			// Se ignora: ya estamos manejando un error previo
		}
	}
}

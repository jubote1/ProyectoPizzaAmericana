package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
	 * Trae el detalle de respuestas para el reporte de mercadeo: una fila por
	 * respuesta, con el nombre de la tienda y el texto de la pregunta ya resueltos.
	 *
	 * @param idTienda     tienda a filtrar; 0 para todas
	 * @param fechaInicial fecha inicial inclusive, formato aaaa-mm-dd
	 * @param fechaFinal   fecha final inclusive, formato aaaa-mm-dd
	 * @param idPregunta   pregunta a filtrar; 0 para todas
	 * @return lista de filas como objetos JSON; vacia si no hay o si fallo
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<org.json.simple.JSONObject> obtenerDetalle(final int idTienda,
			final String fechaInicial, final String fechaFinal, final int idPregunta) {
		final ArrayList<org.json.simple.JSONObject> filas = new ArrayList<org.json.simple.JSONObject>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final StringBuilder consulta = new StringBuilder();
			consulta.append("select em.idencuesta, em.idtienda, ifnull(t.nombre, concat('TIENDA ', em.idtienda)) tienda, ");
			consulta.append("em.idpedidotienda, em.idcliente, em.idusuario, em.idpregunta, pm.titulo, ");
			consulta.append("pm.descripcion pregunta, pm.tipo, em.respuesta, em.idopcion, ");
			consulta.append("date_format(em.fecha_creacion, '%Y-%m-%d %H:%i') fecha ");
			consulta.append("from encuesta_mercadeo em ");
			consulta.append("inner join pregunta_mercadeo pm on pm.idpregunta = em.idpregunta ");
			consulta.append("left join tienda t on t.idtienda = em.idtienda ");
			// Se compara contra el dia siguiente en vez de usar date(fecha_creacion),
			// porque envolver la columna en una funcion impide usar el indice
			consulta.append("where em.fecha_creacion >= ? and em.fecha_creacion < date_add(?, interval 1 day) ");
			if (idTienda > 0) {
				consulta.append("and em.idtienda = ? ");
			}
			if (idPregunta > 0) {
				consulta.append("and em.idpregunta = ? ");
			}
			consulta.append("order by em.fecha_creacion desc, em.idencuesta desc");
			logger.info(consulta.toString());
			final PreparedStatement pst = con1.prepareStatement(consulta.toString());
			int posicion = 1;
			pst.setString(posicion++, fechaInicial);
			pst.setString(posicion++, fechaFinal);
			if (idTienda > 0) {
				pst.setInt(posicion++, idTienda);
			}
			if (idPregunta > 0) {
				pst.setInt(posicion++, idPregunta);
			}
			final ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				final org.json.simple.JSONObject fila = new org.json.simple.JSONObject();
				fila.put("idencuesta", Integer.valueOf(rs.getInt("idencuesta")));
				fila.put("idtienda", Integer.valueOf(rs.getInt("idtienda")));
				fila.put("tienda", rs.getString("tienda"));
				fila.put("idpedidotienda", Integer.valueOf(rs.getInt("idpedidotienda")));
				fila.put("idcliente", Integer.valueOf(rs.getInt("idcliente")));
				fila.put("idusuario", Integer.valueOf(rs.getInt("idusuario")));
				fila.put("idpregunta", Integer.valueOf(rs.getInt("idpregunta")));
				fila.put("titulo", rs.getString("titulo"));
				fila.put("pregunta", rs.getString("pregunta"));
				fila.put("tipo", rs.getString("tipo"));
				fila.put("respuesta", rs.getString("respuesta"));
				fila.put("idopcion", Integer.valueOf(rs.getInt("idopcion")));
				fila.put("fecha", rs.getString("fecha"));
				filas.add(fila);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("obtenerDetalle: " + e.toString());
			cerrar(con1);
		}
		return (filas);
	}

	/**
	 * Trae el resumen agrupado: cuantas veces se dio cada respuesta por pregunta.
	 *
	 * Las preguntas abiertas (tipo A) se agrupan aparte bajo la etiqueta
	 * "(respuestas abiertas)", porque agrupar texto libre no dice nada util: cada
	 * respuesta seria su propio grupo. Para esas hay que mirar el detalle.
	 *
	 * @param idTienda     tienda a filtrar; 0 para todas
	 * @param fechaInicial fecha inicial inclusive, formato aaaa-mm-dd
	 * @param fechaFinal   fecha final inclusive, formato aaaa-mm-dd
	 * @param idPregunta   pregunta a filtrar; 0 para todas
	 * @return lista de grupos como objetos JSON; vacia si no hay o si fallo
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<org.json.simple.JSONObject> obtenerResumen(final int idTienda,
			final String fechaInicial, final String fechaFinal, final int idPregunta) {
		final ArrayList<org.json.simple.JSONObject> filas = new ArrayList<org.json.simple.JSONObject>();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final StringBuilder consulta = new StringBuilder();
			consulta.append("select pm.idpregunta, pm.titulo, pm.descripcion pregunta, pm.tipo, pm.orden, ");
			consulta.append("case when pm.tipo = 'A' then '(respuestas abiertas)' ");
			consulta.append("when pm.tipo = 'B' and em.respuesta = 'S' then 'SI' ");
			consulta.append("when pm.tipo = 'B' and em.respuesta = 'N' then 'NO' ");
			consulta.append("else em.respuesta end etiqueta, ");
			consulta.append("count(*) cantidad ");
			consulta.append("from encuesta_mercadeo em ");
			consulta.append("inner join pregunta_mercadeo pm on pm.idpregunta = em.idpregunta ");
			consulta.append("where em.fecha_creacion >= ? and em.fecha_creacion < date_add(?, interval 1 day) ");
			if (idTienda > 0) {
				consulta.append("and em.idtienda = ? ");
			}
			if (idPregunta > 0) {
				consulta.append("and em.idpregunta = ? ");
			}
			consulta.append("group by pm.idpregunta, pm.titulo, pm.descripcion, pm.tipo, pm.orden, etiqueta ");
			consulta.append("order by pm.orden, pm.idpregunta, cantidad desc");
			logger.info(consulta.toString());
			final PreparedStatement pst = con1.prepareStatement(consulta.toString());
			int posicion = 1;
			pst.setString(posicion++, fechaInicial);
			pst.setString(posicion++, fechaFinal);
			if (idTienda > 0) {
				pst.setInt(posicion++, idTienda);
			}
			if (idPregunta > 0) {
				pst.setInt(posicion++, idPregunta);
			}
			final ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				final org.json.simple.JSONObject fila = new org.json.simple.JSONObject();
				fila.put("idpregunta", Integer.valueOf(rs.getInt("idpregunta")));
				fila.put("titulo", rs.getString("titulo"));
				fila.put("pregunta", rs.getString("pregunta"));
				fila.put("tipo", rs.getString("tipo"));
				fila.put("etiqueta", rs.getString("etiqueta"));
				fila.put("cantidad", Integer.valueOf(rs.getInt("cantidad")));
				filas.add(fila);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("obtenerResumen: " + e.toString());
			cerrar(con1);
		}
		return (filas);
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

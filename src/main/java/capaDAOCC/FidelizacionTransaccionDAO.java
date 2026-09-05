package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FidelizacionTransaccion;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class FidelizacionTransaccionDAO {

	
	public static boolean existeFidelizacionTransaccion(String correo, int idTienda, int idPedidoTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from fidelizacion_transaccion where correo = '"+ correo + "' and idtienda = " + idTienda + " and idpedidotienda = " + idPedidoTienda;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				respuesta = true;
				break;
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(respuesta);
	}
	
	public static boolean insertarFidelizacionTransaccion(FidelizacionTransaccion transaccion , int diasVigencia)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Se usa PreparedStatement porque el correo es texto libre que llega desde
			//el punto de venta. Pegandolo a la cadena, un apostrofo en el correo rompe
			//la consulta y la acumulacion de ese pedido se pierde en silencio.
			String insert = "insert into fidelizacion_transaccion (correo, idtienda, idpedidotienda, valor_neto, puntos, usuario, fecha_vencimiento) values (?,?,?,?,?,?, DATE_ADD(CURDATE(), INTERVAL ? DAY))";
			logger.info(insert + " correo=" + transaccion.getCorreo() + " tienda=" + transaccion.getIdTienda()
					+ " pedido=" + transaccion.getIdPedidoTienda() + " puntos=" + transaccion.getPuntos()
					+ " usuario=" + transaccion.getUsuario());
			PreparedStatement pst = con1.prepareStatement(insert);
			pst.setString(1, transaccion.getCorreo());
			pst.setInt(2, transaccion.getIdTienda());
			pst.setInt(3, transaccion.getIdPedidoTienda());
			pst.setDouble(4, transaccion.getValorNeto());
			pst.setDouble(5, transaccion.getPuntos());
			pst.setString(6, transaccion.getUsuario());
			pst.setInt(7, diasVigencia);
			pst.executeUpdate();
			respuesta = true;
			pst.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(respuesta);
	}
	
	public static ArrayList<FidelizacionTransaccion> obtenerFidelizacionTransacciones(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<FidelizacionTransaccion> transacciones = new ArrayList();
		FidelizacionTransaccion tranTemp;
		int idTienda;
		String tienda;
		int idPedidoTienda;
		String fechaTransaccion;
		double valorNeto;
		double puntos;
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND a.correo = '" + correo + "'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				idTienda = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				idPedidoTienda = rs.getInt("idpedidotienda");
				fechaTransaccion = rs.getString("fecha_transaccion");
				valorNeto = rs.getDouble("valor_neto");
				puntos = rs.getDouble("puntos");
				tranTemp = new FidelizacionTransaccion(correo,idTienda,tienda, idPedidoTienda, fechaTransaccion, valorNeto, puntos);
				transacciones.add(tranTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(transacciones);
	}
	
	
	/**
	 * Método que obtiene la transacciones de fidelización vencidas dados los parámetros.
	 * @return
	 */
	public static ArrayList<FidelizacionTransaccion> obtenerFidelizacionTransaccionesVencimiento()
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<FidelizacionTransaccion> transacciones = new ArrayList();
		FidelizacionTransaccion tranTemp;
		int idTienda;
		String tienda;
		int idPedidoTienda;
		String fechaTransaccion;
		double valorNeto;
		double puntos;
		double puntosRedimidos;
		String correo;
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND a.fecha_vencimiento < CURDATE() AND vencidos = 'N' and puntos > puntos_redimidos";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				correo = rs.getString("correo");
				idTienda = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				idPedidoTienda = rs.getInt("idpedidotienda");
				fechaTransaccion = rs.getString("fecha_transaccion");
				valorNeto = rs.getDouble("valor_neto");
				puntos = rs.getDouble("puntos");
				puntosRedimidos = rs.getDouble("puntos_redimidos");
				tranTemp = new FidelizacionTransaccion(correo,idTienda,tienda, idPedidoTienda, fechaTransaccion, valorNeto, puntos);
				tranTemp.setPuntosRedimidos(puntosRedimidos);
				transacciones.add(tranTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(transacciones);
	}
	
	/**
	 *Marca como vencida una transacción de fidelización 
	 * @param correo
	 * @param idTienda
	 * @param idPedidoTienda
	 * @param puntosVencidos
	 * @return
	 */
	public static boolean vencerFidelizacionTransaccion(String correo,int idTienda, int idPedidoTienda, double puntosVencidos)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "UPDATE fidelizacion_transaccion SET vencidos = 'S' , puntos_vencidos = " + puntosVencidos + " WHERE correo = '" + correo + "' AND idtienda = " + idTienda +  " AND idpedidotienda =" + idPedidoTienda;
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(respuesta);
	}
	
	public static ArrayList<FidelizacionTransaccion> obtenerFidelizacionTransaccionesRedencion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<FidelizacionTransaccion> transacciones = new ArrayList();
		FidelizacionTransaccion tranTemp;
		int idTienda;
		String tienda;
		int idPedidoTienda;
		String fechaTransaccion;
		double valorNeto;
		double puntos;
		double puntosRedimidos;
		double puntosVencidos;
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND vencidos = 'N' and puntos > puntos_redimidos and correo = '" + correo + "' ORDER BY fecha_transaccion desc";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				correo = rs.getString("correo");
				idTienda = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				idPedidoTienda = rs.getInt("idpedidotienda");
				fechaTransaccion = rs.getString("fecha_transaccion");
				valorNeto = rs.getDouble("valor_neto");
				puntos = rs.getDouble("puntos");
				puntosRedimidos = rs.getDouble("puntos_redimidos");
				tranTemp = new FidelizacionTransaccion(correo,idTienda,tienda, idPedidoTienda, fechaTransaccion, valorNeto, puntos);
				tranTemp.setPuntosRedimidos(puntosRedimidos);
				transacciones.add(tranTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(transacciones);
	}
	
	public static boolean actualizarPuntosRedimidosFidelizacionTransaccion(String correo, int idTienda, int idPedidoTienda, double puntosRedimidos)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			PreparedStatement pstmt = con1.prepareStatement(
		             "update fidelizacion_transaccion set puntos_redimidos = ? where correo = ? and idtienda = ? and idpedidotienda = ?");
		    pstmt.setDouble(1, puntosRedimidos);
		    pstmt.setString(2, correo);
		    pstmt.setInt(3, idTienda);
		    pstmt.setInt(4, idPedidoTienda);
		    pstmt.executeUpdate();

			respuesta = true;
			pstmt.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(respuesta);
	}
	

	/**
	 * Un cliente al que hay que avisarle que se le vencen puntos, con el total
	 * que se le vence y la fecha mas proxima.
	 */
	public static class AvisoVencimiento {

		public String correo = "";

		public double puntos = 0;

		public String fechaVence = "";

		public String nombre = "";
	}

	/** Columna de control del aviso temprano. */
	public static final String COLUMNA_AVISO_60 = "fecha_aviso_60";

	/** Columna de control del ultimo recordatorio. */
	public static final String COLUMNA_AVISO_15 = "fecha_aviso_15";

	/**
	 * Clientes a los que todavia no se les ha avisado que se les vencen puntos
	 * dentro de la ventana indicada.
	 *
	 * Se agrupa por correo y no por transaccion a proposito: un cliente puede
	 * tener varias acumulaciones venciendose la misma semana, y no tiene sentido
	 * mandarle tres correos. Se le manda uno con el total.
	 *
	 * La ventana va desde hoy hasta hoy mas los dias indicados, y no exactamente
	 * al dia numero N. Asi, si el proceso no corre una noche, los que quedaron
	 * sin avisar se recuperan a la noche siguiente en vez de perderse.
	 *
	 * El tope existe porque el dia que esto arranque hay casi seis mil clientes
	 * acumulados en la ventana de 60 dias. Sin tope serian seis mil correos de
	 * una sola noche, y Gmail reacciona mal a eso.
	 *
	 * @param dias    tamano de la ventana en dias
	 * @param columna COLUMNA_AVISO_60 o COLUMNA_AVISO_15
	 * @param maximo  cuantos clientes atender esta noche
	 */
	public static ArrayList<AvisoVencimiento> obtenerClientesParaAviso(int dias, String columna, int maximo)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<AvisoVencimiento> avisos = new ArrayList<>();
		if(!COLUMNA_AVISO_60.equals(columna) && !COLUMNA_AVISO_15.equals(columna))
		{
			//El nombre de una columna no puede ir como parametro de un
			//PreparedStatement, asi que se acepta solo de esta lista.
			logger.error("obtenerClientesParaAviso: columna no permitida " + columna);
			return(avisos);
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if(con1 == null)
		{
			return(avisos);
		}
		try
		{
			//El nombre sale en la misma consulta con una subconsulta, no con un viaje
			//aparte por cada cliente. Se toma el de la fila mas reciente porque un
			//mismo correo puede estar repetido en cliente, que es justo el problema
			//que aparecio en el analisis de clientes centralizados.
			String consulta = "select t.correo, SUM(t.puntos - t.puntos_redimidos) as puntos,"
					+ " MIN(t.fecha_vencimiento) as vence,"
					+ " IFNULL((select c.nombre from cliente c where c.email = t.correo"
					+ "          order by c.idcliente desc limit 1),'') as nombre"
					+ " from fidelizacion_transaccion t"
					+ " where t.vencidos = 'N' and t.puntos > t.puntos_redimidos"
					+ " and t.fecha_vencimiento between CURDATE() and DATE_ADD(CURDATE(), INTERVAL ? DAY)"
					+ " and t." + columna + " is null"
					+ " group by t.correo having puntos > 0 order by vence limit ?";
			PreparedStatement pst = con1.prepareStatement(consulta);
			pst.setInt(1, dias);
			pst.setInt(2, maximo);
			ResultSet rs = pst.executeQuery();
			while(rs.next())
			{
				AvisoVencimiento aviso = new AvisoVencimiento();
				aviso.correo = rs.getString("correo");
				aviso.puntos = rs.getDouble("puntos");
				aviso.fechaVence = rs.getString("vence") == null ? "" : rs.getString("vence");
				aviso.nombre = rs.getString("nombre") == null ? "" : rs.getString("nombre");
				avisos.add(aviso);
			}
			rs.close();
			pst.close();
			con1.close();
		}
		catch(Exception e)
		{
			logger.error("obtenerClientesParaAviso: " + e.toString());
			System.out.println("obtenerClientesParaAviso: " + e.toString());
			try { con1.close(); } catch(Exception e1) { }
		}
		return(avisos);
	}

	/**
	 * Deja constancia de que a este cliente ya se le aviso, para que el barrido
	 * de manana no le mande el mismo correo otra vez.
	 *
	 * Se marcan todas sus acumulaciones dentro de la ventana, que son las que se
	 * le resumieron en el correo.
	 */
	public static boolean marcarAvisoEnviado(String correo, int dias, String columna)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		if(!COLUMNA_AVISO_60.equals(columna) && !COLUMNA_AVISO_15.equals(columna))
		{
			logger.error("marcarAvisoEnviado: columna no permitida " + columna);
			return(false);
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if(con1 == null)
		{
			return(false);
		}
		try
		{
			String update = "update fidelizacion_transaccion set " + columna + " = CURDATE()"
					+ " where correo = ? and vencidos = 'N' and puntos > puntos_redimidos"
					+ " and fecha_vencimiento between CURDATE() and DATE_ADD(CURDATE(), INTERVAL ? DAY)"
					+ " and " + columna + " is null";
			PreparedStatement pst = con1.prepareStatement(update);
			pst.setString(1, correo);
			pst.setInt(2, dias);
			respuesta = (pst.executeUpdate() > 0);
			pst.close();
			con1.close();
		}
		catch(Exception e)
		{
			logger.error("marcarAvisoEnviado: " + e.toString());
			try { con1.close(); } catch(Exception e1) { }
		}
		return(respuesta);
	}
}

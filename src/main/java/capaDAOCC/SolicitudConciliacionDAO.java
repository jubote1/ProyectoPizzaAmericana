package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.SolicitudConciliacion;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class SolicitudConciliacionDAO {

	/**
	 * Método que se encarga de insertar la solicitud de conciliación de QR o Datáfono
	 * @param solicitud
	 * @return
	 */
	public static boolean insertarSolicitudConciliacion(SolicitudConciliacion solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitud_conciliacion (fecha,origen,descripcion,idtienda,categoria,valor_analizar,telefono,idpedidotienda) values ('"+ solicitud.getFecha() + "','" + solicitud.getOrigen() + "' , '" + solicitud.getDescripcion() + "' , " + solicitud.getIdTienda() + " , '" + solicitud.getCategoria() + "' , " + solicitud.getValorAnalizar() + " , '" + solicitud.getTelefono() +"' , " + solicitud.getIdPedidoTienda() + ")";
			logger.info(insert);
			stm.executeUpdate(insert);
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
	
	/**
	 * Método que trae la cantidad de solicitudes de conciliación pendientes en los últimos 7 días.
	 * @param idTienda
	 * @return
	 */
	public static int existeSolicitudConciliacion(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int respuesta = 0;
		Calendar calendario = Calendar.getInstance();
		calendario.add(Calendar.DAY_OF_YEAR, -7);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String fecha = dateFormat.format(calendario.getTime());
		try
		{
			Statement stm = con1.createStatement();
			String select = "select count(*) from solicitud_conciliacion where idtienda = " + idTienda + " and fecha >= '" + fecha +"' and estado = 'PENDIENTE'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				respuesta = rs.getInt(1);
			}
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
	
	
	/**
	 * Método que nos retornará en un json un booleano qeu nos indicará si se puede o no cerrar el sistema dado que hay que hay partidas
	 * pendientes de validación
	 * @param idTienda
	 * @return
	 */
	public static boolean validarCierreSolicitudConciliacion(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = true;
		Calendar calendario = Calendar.getInstance();
		calendario.add(Calendar.DAY_OF_YEAR, -7);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String fecha = dateFormat.format(calendario.getTime());
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitud_conciliacion where idtienda = " + idTienda + " and fecha < '" + fecha +"' and estado = 'PENDIENTE'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				respuesta  = false;
			}
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
	
	
	/**
	 * Método que dado una tienda y una fecha de consulta, retorna las solicitudes de conciliación 
	 * @param idTienda
	 * @param fechaConsulta
	 * @return
	 */
	public static ArrayList<SolicitudConciliacion> consultarSolicitudConciliacion(int idTienda, String fechaConsulta)
	{
		ArrayList<SolicitudConciliacion> solicitudes = new ArrayList();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitud_conciliacion where idtienda = " + idTienda + " and fecha >= '" + fechaConsulta +"'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			SolicitudConciliacion solTemp;
			int idSolicitud;
			String fecha;
			String origen;
			String descripcion;
			String categoria;
			double valorAnalizar;
			String estado;
			double valorFinal;
			String telefono;
			int idPedidoTienda;
			while(rs.next())
			{
				idSolicitud = rs.getInt("idsolicitud");
				fecha = rs.getString("fecha");
				origen = rs.getString("origen");
				descripcion = rs.getString("descripcion");
				categoria = rs.getString("categoria");
				valorAnalizar = rs.getDouble("valor_analizar");
				estado = rs.getString("estado");
				valorFinal = rs.getDouble("valor_final");
				telefono = rs.getString("telefono");
				idPedidoTienda = rs.getInt("idpedidotienda");
				solTemp = new SolicitudConciliacion(idSolicitud, fecha, origen, descripcion, idTienda,
						categoria, valorAnalizar, estado, valorFinal, telefono,
						idPedidoTienda);
				solicitudes.add(solTemp);
			}
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
		return(solicitudes);
	}

	
}

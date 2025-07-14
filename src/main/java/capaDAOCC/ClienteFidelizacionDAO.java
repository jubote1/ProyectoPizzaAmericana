package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class ClienteFidelizacionDAO {

	public static ClienteFidelizacion obtenerClienteFidelizacion(String correo)
	{
	    Logger logger = Logger.getLogger("log_file");
	    ClienteFidelizacion cliente = null;
	    ConexionBaseDatos con = new ConexionBaseDatos();

	    try (Connection con1 = con.obtenerConexionBDPrincipal();
	         PreparedStatement pstmt = con1.prepareStatement(
	             "SELECT fecha_vinculacion, activo, puntos_vigentes FROM cliente_fidelizacion WHERE correo = ?")) {

	        pstmt.setString(1, correo);
	        logger.info("Ejecutando consulta para obtener cliente con correo: " + correo);

	        try (ResultSet rs = pstmt.executeQuery()) {
	            if (rs.next()) {
	                String fechaVinculacion = rs.getString("fecha_vinculacion");
	                String activo = rs.getString("activo");
	                double puntosVigentes = rs.getDouble("puntos_vigentes");

	                cliente = new ClienteFidelizacion(correo, fechaVinculacion, activo, puntosVigentes);
	            } else {
	                logger.warn("No se encontró cliente con el correo: " + correo);
	            }
	        }

	    } catch (Exception e) {
	        logger.error("Error al obtener cliente fidelización: " + e.toString());
	    }

	    return cliente;
	}

	
	
	public static boolean existeClienteFidelizacion(String correo) {
	    Logger logger = Logger.getLogger("log_file");
	    boolean respuesta = false;
	    Connection con1 = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        ConexionBaseDatos con = new ConexionBaseDatos();
	        con1 = con.obtenerConexionBDPrincipal();

	        String consulta = "SELECT 1 FROM cliente_fidelizacion WHERE correo = ? AND activo = 'S'";
	        ps = con1.prepareStatement(consulta);
	        ps.setString(1, correo);
	        logger.info("Consulta: " + ps.toString());

	        rs = ps.executeQuery();
	        respuesta = rs.next(); // basta con verificar si hay un resultado
	    } catch (Exception e) {
	        logger.error("Error en existeClienteFidelizacion: " + e.toString());
	    } finally {
	        try { if (rs != null) rs.close(); } catch (Exception ignored) {}
	        try { if (ps != null) ps.close(); } catch (Exception ignored) {}
	        try { if (con1 != null) con1.close(); } catch (Exception ignored) {}
	    }

	    return respuesta;
	}

	
	public static boolean desactivarClienteFidelizacion(String correo)
	{
	    Logger logger = Logger.getLogger("log_file");
	    boolean respuesta = false;
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	

	    try
	    {
	        String update = "UPDATE cliente_fidelizacion SET activo = 'N' WHERE correo = ?";
	        PreparedStatement pstmt = con1.prepareStatement(update);
	        pstmt.setString(1, correo);

	        logger.info("Ejecutando: " + update + " con correo = " + correo);

	        int filasAfectadas = pstmt.executeUpdate();
	        if (filasAfectadas > 0) {
	            respuesta = true;
	        } else {
	            logger.warn("No se encontró ningún cliente con el correo: " + correo);
	        }

	        pstmt.close();
	        con1.close();
	    }
	    catch (Exception e)
	    {
	    	   System.out.println("Error al desactivar cliente: "+e);
	        logger.error("Error al desactivar cliente: " + e.toString());
	        try
	        {
	            if (con1 != null) con1.close();
	        } catch (Exception e1)
	        {
	            logger.error("Error al cerrar la conexión: " + e1.toString());
	        }
	    }
	    
	

	    return respuesta;
	}

	
	public static boolean activarClienteFidelizacion(String correo) {
		Logger logger = Logger.getLogger("log_file");
        String sql = "UPDATE cliente_fidelizacion SET activo = 'S' WHERE correo = ?";
        boolean respuesta = false;

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;
        PreparedStatement stm = null;

        try {
            con1 = con.obtenerConexionBDPrincipal();
            stm = con1.prepareStatement(sql);
            stm.setString(1, correo);

            int filasAfectadas = stm.executeUpdate();
            respuesta = (filasAfectadas > 0); // Si se actualizó al menos una fila, es exitoso

            logger.info("Cliente activado con correo: " + correo + " | Filas afectadas: " + filasAfectadas);
        } catch (SQLException e) {
            logger.error("Error activando cliente: " + correo, e);
        } finally {
            // Cerrar recursos en el orden correcto
            try {
                if (stm != null) stm.close();
                if (con1 != null) con1.close();
            } catch (SQLException e) {
                logger.error("Error cerrando recursos", e);
            }
        }

        return respuesta;
    }
	
	public static double sumarPuntosClienteFidelizacion(String correo, double puntosSumar)
	{
		Logger logger = Logger.getLogger("log_file");
		double puntos = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update cliente_fidelizacion a set a.puntos_vigentes = a.puntos_vigentes + " + puntosSumar +  " where a.correo = '"+ correo + "'";
			logger.info(update);
			stm.executeUpdate(update);
			String select = "select puntos_vigentes from cliente_fidelizacion a where a.correo = '"+ correo + "'";
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				puntos = rs.getDouble(1);
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
		return(puntos);
	}
	
	/**
	 * Método que se encarga de recalcular los puntos disponibles del cliente luego de una redención
	 * @param correo
	 * @param puntosRedimir
	 * @return
	 */
	public static double redimirPuntosClienteFidelizacion(String correo, double puntosRedimir)
	{
		Logger logger = Logger.getLogger("log_file");
		double puntos = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update cliente_fidelizacion a set a.puntos_vigentes = a.puntos_vigentes - " + puntosRedimir +  " where a.correo = '"+ correo + "'";
			logger.info(update);
			stm.executeUpdate(update);
			String select = "select puntos_vigentes from cliente_fidelizacion a where a.correo = '"+ correo + "'";
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				puntos = rs.getDouble(1);
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
		return(puntos);
	}

	public static boolean insertarClienteFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into cliente_fidelizacion (correo) values ('" + correo +"')";
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
	
	public static boolean insertarLogRedencion(String correo, double puntosRedimir)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into fidelizacion_redencion (correo,puntos_redimidos) values ('" + correo + "', " + puntosRedimir +")";
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
	
	public static double obtenerCantidadPuntosCliente(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		double puntosVigentes = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select puntos_vigentes from cliente_fidelizacion where correo = '"+ correo + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				puntosVigentes = rs.getDouble("puntos_vigentes");
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
		return(puntosVigentes);
	}
	
}

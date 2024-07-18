package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.PedidoPrecioEmpleado;
import conexionCC.ConexionBaseDatos;

public class EmpleadoRemotoValeDAO {

	public static void insertarEmpleadoRemotoVale(int idEmpleado, String fecha, String codigo)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into empleado_remoto_vale (idempleado, fecha,codigo) values("
			+ idEmpleado + " ,'" + fecha + "' , '"  + codigo + "')" ;
			logger.info(insert);
			stm.executeUpdate(insert);
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
	}
		
	public static boolean validarExistenciaCodigo(String codigo)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		boolean resultado = false;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from empleado_remoto_vale  where codigo = '" + codigo + "'"; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				resultado = true;
			}
			stm.close();
			rs.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(resultado);
	}
	
}

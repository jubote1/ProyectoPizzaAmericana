package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.EmpleadoVale;
import capaModeloCC.HorarioEmpleado;
import capaConexionPOS.ConexionBaseDatos;
import capaModeloPOS.Egreso;
import capaModeloPOS.EmpleadoEncuesta;

public class EmpleadoValeDAO {
		
	public static int insertarEmpleadoVale(EmpleadoVale empleadoVale)
	{
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneralLocal();
		String insert  = "insert into empleado_vale (idempleado, fecha,valor) values(" + empleadoVale.getIdEmpleado() + ", '" + empleadoVale.getFecha() + "' ," + empleadoVale.getValor() +")" ;
		Statement stm;
		int idEmpleadoVale = 0;
		try
		{
			stm = con1.createStatement();
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEmpleadoVale=rs.getInt(1);
	        }
			stm.close();
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
			System.out.println(e.toString());
			try
			{
				con1.close();
				
			}catch(Exception e1)
			{
				
			}
		}
		return(idEmpleadoVale);
	}
	
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.EmpleadoEncuesta;
import capaConexionPOS.ConexionBaseDatos;


public class EmpleadoEncuestaDAO {
		
	public static ArrayList<String[]> obtenerResultadoEncuesta( String fechaInferior, String fechaSuperior, int idTienda, int idEncuesta)
	{
		ArrayList<String[]> repResultadoEncuesta = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneralLocal();
		String select  = "SELECT a.nombre_largo AS nombre, SUM(observacion)/COUNT(*) AS promedio, (SELECT COUNT(*) FROM empleado_encuesta d WHERE d.id_evaluar = a.id AND d.fecha_ingreso >= '" + fechaInferior + " 00:00:00' AND d.fecha_ingreso <= '" + fechaSuperior + " 23:59:00' AND d.idencuesta = " + idEncuesta + " and d.idtienda = " + idTienda + ") AS cantidad FROM empleado a, empleado_encuesta b, empleado_encuesta_detalle c " + 
				" WHERE a.id = b.id_evaluar AND b.idempleadoencuesta = c.idempleadoencuesta AND b.fecha_ingreso >= '" + fechaInferior + " 00:00:00' AND b.fecha_ingreso <= '" + fechaSuperior +" 23:59:00' AND b.idencuesta = " + idEncuesta + " and b.idtienda = " + idTienda + " GROUP BY a.nombre_largo, cantidad";
		Statement stm;
		System.out.println(select);
		try
		{
			stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(select);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				repResultadoEncuesta.add(fila);
			}
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
		return(repResultadoEncuesta);
	}
	
	public static ArrayList<String[]> obtenerResultadoEncuesta( String fechaInferior, String fechaSuperior, int idEncuesta)
	{
		ArrayList<String[]> repResultadoEncuesta = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneralLocal();
		String select  = "SELECT a.nombre_largo AS nombre, SUM(observacion)/COUNT(*) AS promedio, (SELECT COUNT(*) FROM empleado_encuesta d WHERE d.id_evaluar = a.id AND d.fecha_ingreso >= '" + fechaInferior + " 00:00:00' AND d.fecha_ingreso <= '" + fechaSuperior + " 23:59:00' AND d.idencuesta = " + idEncuesta + ") AS cantidad FROM empleado a, empleado_encuesta b, empleado_encuesta_detalle c " + 
				" WHERE a.id = b.id_evaluar AND b.idempleadoencuesta = c.idempleadoencuesta AND b.fecha_ingreso >= '" + fechaInferior + " 00:00:00' AND b.fecha_ingreso <= '" + fechaSuperior +" 23:59:00' AND b.idencuesta = " + idEncuesta + " GROUP BY a.nombre_largo, cantidad";
		Statement stm;
		System.out.println(select);
		try
		{
			stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(select);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				repResultadoEncuesta.add(fila);
			}
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
		return(repResultadoEncuesta);
	}
	
	public static int insertarEmpleadoEncuesta(EmpleadoEncuesta empEncuesta)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEmpleadoEncuesta = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneralLocal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "";
			int idTienda = 0;

			if ((Integer)empEncuesta.getIdTienda() != null) {
				idTienda = empEncuesta.getIdTienda();
			}

			if(empEncuesta.getIdEvaluar() == 0)
			{
				insert = "insert into empleado_encuesta (id, idencuesta,idtienda) values (" + empEncuesta.getId() + " , " + empEncuesta.getIdEncuesta() + ","+idTienda+")";
			}else
			{
				insert = "insert into empleado_encuesta (id, id_evaluar, idencuesta, idtienda) values (" + empEncuesta.getId() + " , " + empEncuesta.getIdEvaluar() + " , " + empEncuesta.getIdEncuesta() + " , " + empEncuesta.getIdTienda() + ")";
			}
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEmpleadoEncuesta=rs.getInt(1);
	        }
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idEmpleadoEncuesta);
	}
	

}

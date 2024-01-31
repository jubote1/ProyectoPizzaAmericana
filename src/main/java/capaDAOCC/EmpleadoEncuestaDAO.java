package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

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

	
	public static ArrayList<JSONObject> obtenerResultadoEncuestaOperacion(int idtienda ,int idencuesta,String[] rangofecha){
		ArrayList<JSONObject> repResultadoEncuesta = new ArrayList();
		Connection con1 = null;
        Statement statement = null;
        ResultSet resultSet = null;
		
		try {

			ConexionBaseDatos con = new ConexionBaseDatos();
			con1 = con.obtenerConexionBDGeneralLocal();
			String fecha_inicio = rangofecha[0];
			String fecha_final= rangofecha[1];
			String sqlQuer ="SELECT \n"
					+ " 	e.idempleadoencuesta,\n"
					+ "    em.nombre_largo AS nombre_empleado,\n"
					+ "    e.fecha_ingreso,\n"
					+ "    e.idtienda AS tienda_id,\n"
					+ "    l.descripcion AS descripcion_encuesta\n"
					+ "\n"
					+ "FROM \n"
					+ "    empleado_encuesta AS e\n"
					+ "JOIN \n"
					+ "    encuesta_laboral AS l ON l.idencuesta = e.idencuesta\n"
					+ "JOIN \n"
					+ "    empleado AS em ON e.id = em.id\n"
					+ "WHERE \n"
					+ "    e.idencuesta = "+idencuesta+"\n"
					+ "    AND DATE(e.fecha_ingreso) BETWEEN '"+fecha_inicio+"' AND '"+fecha_final+"'\n"
					+ "    AND e.idtienda = "+idtienda+" GROUP BY e.idempleadoencuesta,em.nombre_largo, e.fecha_ingreso, e.idtienda, l.descripcion";
			
	            statement = con1.createStatement();

	            // Ejecutar la consulta SQL
	            resultSet = statement.executeQuery(sqlQuer);

	            // Procesar los resultados
	            while (resultSet.next()) {
	                // Acceder a los valores de las columnas
	            	int idempleadoencuesta = resultSet.getInt("idempleadoencuesta");
	                String nombreEmpleado = resultSet.getString("nombre_empleado");
	                String fechaIngreso = resultSet.getString("fecha_ingreso");
	                int tiendaId = resultSet.getInt("tienda_id");
	                String descripcionEncuesta = resultSet.getString("descripcion_encuesta");
	                JSONObject json = new JSONObject();
	                json.put("idempleadoencuesta", idempleadoencuesta);
	                json.put("nombre_empleado", nombreEmpleado);
	                json.put("fecha_hora", fechaIngreso);
	                json.put("idtienda", tiendaId);
	                json.put("descripcion_encuesta", descripcionEncuesta);
	                repResultadoEncuesta.add(json);

	            }
		} catch (SQLException e) {
			System.out.println(e.toString());
        } finally {
            // Cerrar los objetos de conexión, result set y statement
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (con1 != null) con1.close();
            } catch (SQLException e) {
            	System.out.println(e.toString());
            }
        }

		return repResultadoEncuesta;
	}
	
	
	public static ArrayList<JSONObject> obtenerResultEncuestaOperacionDetalle(int idempleadoencuesta){
		ArrayList<JSONObject> repResultadoEncuesta = new ArrayList();
		Connection con1 = null;
        Statement statement = null;
        ResultSet resultSet = null;
		
		try {

			ConexionBaseDatos con = new ConexionBaseDatos();
			con1 = con.obtenerConexionBDGeneralLocal();

			String sqlQuer ="SELECT \n"
					+ "    el.descripcion AS item,\n"
					+ "    el.tipo_respuesta,\n"
					+ "    el.orden,\n"
					+ "    d.observacion AS respuesta,\n"
					+ "    d.observacion_adi AS observacion,\n"
					+ "    d.respuesta_si,\n"
					+ "    d.respuesta_no\n"
					+ "    \n"
					+ "\n"
					+ "FROM \n"
					+ "    empleado_encuesta_detalle AS d\n"
					+ "JOIN \n"
					+ "    encuesta_laboral_detalle AS el ON d.idencuestadetalle = el.idencuestadetalle\n"
					+ "WHERE \n"
					+ "    d.idempleadoencuesta = "+idempleadoencuesta;
			
	            statement = con1.createStatement();

	            // Ejecutar la consulta SQL
	            resultSet = statement.executeQuery(sqlQuer);

	            // Procesar los resultados
	            while (resultSet.next()) {
	                // Acceder a los valores de las columnas
	                String item = resultSet.getString("item");
	                String tipo_respuesta = resultSet.getString("tipo_respuesta");
	                int orden = resultSet.getInt("orden");
	                String respuesta = resultSet.getString("respuesta");
	                String observacion = resultSet.getString("observacion");
	                String respuesta_si = resultSet.getString("respuesta_si");
	                String respuesta_no = resultSet.getString("respuesta_no");
	                JSONObject json = new JSONObject();
	                
	                json.put("item", item);
	                json.put("tipo_respuesta", tipo_respuesta);
	                json.put("orden", orden);
	                json.put("respuesta", respuesta);
	                json.put("observacion", observacion);
	                json.put("respuesta_si", respuesta_si);
	                json.put("respuesta_no", respuesta_no);
	                repResultadoEncuesta.add(json);

	            }
		} catch (SQLException e) {
			System.out.println(e.toString());
        } finally {
            // Cerrar los objetos de conexión, result set y statement
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (con1 != null) con1.close();
            } catch (SQLException e) {
            	System.out.println(e.toString());
            }
        }

		return repResultadoEncuesta;
	}
}

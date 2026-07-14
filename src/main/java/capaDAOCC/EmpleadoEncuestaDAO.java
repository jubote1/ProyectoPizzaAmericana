package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.EmpleadoEncuesta;
import capaModeloCC.EncuestaServicio;
import capaModeloCC.EncuestaServicio.RespuestaServicio;
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

	
	public static ArrayList<JSONObject> obtenerResultadoEncuestaOperacion(int idtienda ,int idencuesta,String fecha1,String fecha2){
		Logger logger = Logger.getLogger("log_file");
		ArrayList<JSONObject> repResultadoEncuesta = new ArrayList();
		Connection con1 = null;
        Statement statement = null;
        ResultSet resultSet = null;
		
		try {

			ConexionBaseDatos con = new ConexionBaseDatos();
			con1 = con.obtenerConexionBDGeneralLocal();

			String sqlQuer ="SELECT \n"
					+ " 	e.idempleadoencuesta,\n"
					+ "    em.nombre_largo AS nombre_empleado,\n"
					+ "    e.fecha_ingreso,\n"
					+ "    e.idtienda AS tienda_id,\n"
					+ "    l.descripcion AS descripcion_encuesta,\n"
					+ "    e.porcentaje_total\n"
					+ "\n"
					+ "FROM \n"
					+ "    empleado_encuesta AS e\n"
					+ "JOIN \n"
					+ "    encuesta_laboral AS l ON l.idencuesta = e.idencuesta\n"
					+ "JOIN \n"
					+ "    empleado AS em ON e.id = em.id\n"
					+ "WHERE \n"
					+ "    e.idencuesta = "+idencuesta+"\n"
					+ "    AND DATE(e.fecha_ingreso) BETWEEN '"+fecha1+"' AND '"+fecha2+"'\n"
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
	                float porcentaje_total =  resultSet.getFloat("porcentaje_total");
	                String descripcionEncuesta = resultSet.getString("descripcion_encuesta");
	                JSONObject json = new JSONObject();
	                json.put("idempleadoencuesta", idempleadoencuesta);
	                json.put("nombre_empleado", nombreEmpleado);
	                json.put("fecha_hora", fechaIngreso);
	                json.put("idtienda", tiendaId);
	                json.put("descripcion_encuesta", descripcionEncuesta);
	                json.put("porcentaje_total", porcentaje_total);
	                repResultadoEncuesta.add(json);

	            }
		} catch (SQLException e) {
			System.out.println(e.toString());
			logger.error(e.toString());
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
		Logger logger = Logger.getLogger("log_file");
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
					+ "    d.respuesta_no,\n"
					+ "    el.valor_inicial,\n"
					+ "    el.valor_final,\n"
					+ "    el.valor_escala,\n"
					+ "    el.porcentaje\n"
					+ "    \n"
					+ "\n"
					+ "FROM \n"
					+ "    empleado_encuesta_detalle AS d  "
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
	                float valor_inicial = resultSet.getFloat("valor_inicial");
	                float valor_final = resultSet.getFloat("valor_final");
	                float valor_escala = resultSet.getFloat("valor_escala");
	                float porcentaje = resultSet.getFloat("porcentaje");
	                JSONObject json = new JSONObject();
	                
	                json.put("item", item);
	                json.put("tipo_respuesta", tipo_respuesta);
	                json.put("orden", orden);
	                json.put("respuesta", respuesta);
	                json.put("observacion", observacion);
	                json.put("respuesta_si", respuesta_si);
	                json.put("respuesta_no", respuesta_no);
	                json.put("valor_inicial", valor_inicial);
	                json.put("valor_final", valor_final);
	                json.put("valor_escala", valor_escala);
	                json.put("porcentaje", porcentaje);
	                
	                repResultadoEncuesta.add(json);

	            }
		} catch (SQLException e) {
			System.out.println(e.toString());
			logger.error(e.toString());
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
	
	// Método para insertar registros en la tabla encuesta_servicio
	public static boolean insertarEncuestaServicio(EncuestaServicio encuestaservicio) {
	    String sql = "INSERT INTO encuesta_servicio (idpregunta, respuesta, idpedido, tipo_atencion, idtienda) VALUES (?, ?, ?, ?, ?)";
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = null;
	    boolean exito = true;  // 👈 asumimos que todo va bien

	    try {
	        con1 = con.obtenerConexionBDPrincipal();

	        Integer idpedido = encuestaservicio.getIdpedido();
	        String tipo_atencion = encuestaservicio.getTipo_atencion();
	        Integer idtienda = encuestaservicio.getIdtienda();

	        for (RespuestaServicio value : encuestaservicio.getRespuesta()) {
	            Integer idPregunta = value.getIdpregunta();

	            if (idPregunta == null || idPregunta == 0) {
	                System.err.println("No se encontró la pregunta con el título: " + value.getTitulo());
	                exito = false;  // hubo error en esta respuesta
	                continue;
	            }

	            try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
	                pstmt.setInt(1, idPregunta);
	                pstmt.setString(2, value.getRespuesta());
	                pstmt.setInt(3, idpedido);
	                pstmt.setString(4, tipo_atencion);
	                pstmt.setInt(5, idtienda);

	                pstmt.executeUpdate();
	            } catch (SQLException e) {
	                e.printStackTrace();
	                System.err.println("Error al insertar el registro con la pregunta: " + value.getTitulo());
	                exito = false;  // 👈 marcamos que algo falló
	            }
	        }

	    } catch (Exception e) {
	        System.err.println("Error al insertar encuesta: " + e.getMessage());
	        e.printStackTrace();
	        exito = false;  // fallo global
	    } finally {
	        if (con1 != null) {
	            try {
	                con1.close();
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    return exito; // 👈 devolvemos si todo salió bien o no
	}

    
	// Método para insertar registros en la tabla encuesta_servicio
    public static void insertarClienteServicio(EncuestaServicio encuestaservicio) {
        String sql = "INSERT INTO cliente_servicio (nombre_cliente, telefono, idtienda,idpedido) VALUES (?, ?, ?,?)";
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;
    //EJEMPLOO
        try {
            con1 = con.obtenerConexionBDPrincipal();  // Obtener la conexión una vez
            
            Integer idpedido = encuestaservicio.getIdpedido();
            Integer idtienda =encuestaservicio.getIdtienda();
            String  nombre_cliente=encuestaservicio.getNombre_cliente();
            String  telefono =encuestaservicio.getTelefono();
            //Intentamos marcar que la encuesta fue diligenciada
            LogEncuestaServicioDAO.actualizarLlenadoLogEncuestaServicio(idtienda, idpedido);
            
                // Usamos el PreparedStatement dentro del bloque try-with-resources
                try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
                    pstmt.setString(1, nombre_cliente);  
                    pstmt.setString(2, telefono); 
                    pstmt.setInt(3, idtienda);     
                    pstmt.setInt(4, idpedido);     
                    pstmt.executeUpdate();
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error al insertar el cliente que califico el servicio");
                    // Aquí puedes decidir si detener el proceso o continuar con los demás registros
                }
            

        } catch (Exception e) {
            e.printStackTrace();
            // Aquí manejas el error de conexión general si es necesario

        } finally {
            if (con1 != null) {
                try {
                    con1.close(); // Cerramos la conexión
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static List<org.json.JSONObject> obtenerPreguntas_es() throws SQLException {
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDPrincipal(); 
        
    	List<org.json.JSONObject> listPreguntas = new ArrayList<>();
        String sql = "SELECT * FROM pregunta_servicio";

        try (PreparedStatement pstmt = con1.prepareStatement(sql)) {

            try (ResultSet rs = pstmt.executeQuery()) {
            	while (rs.next()) {
            		org.json.JSONObject obj = new org.json.JSONObject();
                   	obj.put("idpregunta", rs.getInt("idpregunta"));
                   	obj.put("titulo", rs.getString("titulo"));
                   	obj.put("descripcion", rs.getString("descripcion"));
                	obj.put("tipo", rs.getString("tipo"));
                                        
                    listPreguntas.add(obj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Relanzamos la excepción para que sea manejada adecuadamente
        }

        return listPreguntas;
    }
    // Método para obtener el ID de una pregunta por su título
    public static Integer obtenerIdPorTitulo(String titulo, Connection con1) throws SQLException {
        String sql = "SELECT idpregunta FROM pregunta_servicio WHERE titulo = ?";
        Integer idPregunta = null;

        try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
            pstmt.setString(1, titulo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    idPregunta = rs.getInt("idpregunta");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Relanzamos la excepción para que sea manejada adecuadamente
        }

        return idPregunta;
    }
    
    public static void main(String[] args) {

    }
    
    /**
     * Método que se encargara de devolver un valor booleano en caso de que ya se tenga una encuesta insertada para el pedido y tienda pasados
     * como parámetro
     * @param idPedido
     * @param idTienda
     * @return
     * @throws SQLException
     */
    public static boolean validarExisteEncuestaPedido(int idPedido, int idTienda) {

        String sql = "SELECT 1 FROM cliente_servicio WHERE idpedido = ? AND idtienda = ? LIMIT 1";

        try (
            Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
            PreparedStatement pstmt = con1.prepareStatement(sql)
        ) {
            pstmt.setInt(1, idPedido);
            pstmt.setInt(2, idTienda);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean existeEncuestaPedido(int idPedido, int idTienda) {

        Logger logger = Logger.getLogger("log_file");

        String sql = "SELECT 1 "
                   + "FROM encuesta_servicio "
                   + "WHERE idpedido = ? "
                   + "AND idtienda = ? "
                   + "LIMIT 1";

        try (
            Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
            PreparedStatement ps = con1.prepareStatement(sql)
        ) {
            ps.setInt(1, idPedido);
            ps.setInt(2, idTienda);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            logger.error("Error validando encuesta existente: " + e.toString());
            return false;
        }
    }
	


}

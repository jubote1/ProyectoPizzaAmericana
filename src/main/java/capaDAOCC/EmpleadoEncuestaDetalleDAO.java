package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.log4j.Logger;

import capaModeloCC.EmpleadoEncuestaDetalle;
import conexionCC.ConexionBaseDatos;


public class EmpleadoEncuestaDetalleDAO {
	
	public static int insertarEmpleadoEncuestaDetalle(EmpleadoEncuestaDetalle empEncuestaDetalle)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEmpleadoEncuestaDetalle = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into empleado_encuesta_detalle (idempleadoencuesta, idencuestadetalle, respuesta_si, respuesta_no, observacion, observacion_adi) values (" + empEncuestaDetalle.getIdEmpleadoEncuesta() + " , " + empEncuestaDetalle.getIdEncuestaDetalle() + " , '" + empEncuestaDetalle.getRespuestaSi() + "' , '" + empEncuestaDetalle.getRespuestaNo() + "' , '" + empEncuestaDetalle.getObservacion() + "' , '" + empEncuestaDetalle.getObservacionAdicional() +"')"; 
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEmpleadoEncuestaDetalle=rs.getInt(1);
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
		return(idEmpleadoEncuestaDetalle);
	}
	

}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.log4j.Logger;

import capaModeloCC.EmpleadoEncuestaDetalle;
import conexionCC.ConexionBaseDatos;


public class EmpleadoEncuestaDetalleDAO {

	public static int insertarEmpleadoEncuestaDetalle(EmpleadoEncuestaDetalle empEncuestaDetalle) {
		Logger logger = Logger.getLogger("log_file");
		int idEmpleadoEncuestaDetalle = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			con1 = con.obtenerConexionBDGeneral();

			String insert = "INSERT INTO empleado_encuesta_detalle "
					+ "(idempleadoencuesta, idencuestadetalle, respuesta_si, respuesta_no, observacion, observacion_adi) "
					+ "VALUES (?, ?, ?, ?, ?, ?)";

			pstmt = con1.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);

			pstmt.setInt(1, empEncuestaDetalle.getIdEmpleadoEncuesta());
			pstmt.setInt(2, empEncuestaDetalle.getIdEncuestaDetalle());
			pstmt.setString(3, empEncuestaDetalle.getRespuestaSi());
			pstmt.setString(4, empEncuestaDetalle.getRespuestaNo());
			pstmt.setString(5, empEncuestaDetalle.getObservacion());
			pstmt.setString(6, empEncuestaDetalle.getObservacionAdicional());

			pstmt.executeUpdate();

			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				idEmpleadoEncuestaDetalle = rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error("Error insertando EmpleadoEncuestaDetalle: " + e.toString());
			System.out.println("Error: " + e.toString());
			return 0;
		} finally {
			try {
				if (rs != null) rs.close();
				if (pstmt != null) pstmt.close();
				if (con1 != null) con1.close();
			} catch (Exception e2) {
				logger.error("Error cerrando recursos: " + e2.toString());
			}
		}

		return idEmpleadoEncuestaDetalle;
	}
}

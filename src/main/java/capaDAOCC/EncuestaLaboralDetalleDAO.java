package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.EncuestaLaboralDetalle;
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
public class EncuestaLaboralDetalleDAO {
	

	public static ArrayList<EncuestaLaboralDetalle> obtenerEncLaboralDetalle(int idEncuesta) {

	    Logger logger = Logger.getLogger("log_file");
	    ArrayList<EncuestaLaboralDetalle> detalles = new ArrayList<>();
	    String sql = "SELECT ed.idencuestadetalle, ed.descripcion, ed.tipo_respuesta, ed.valor_inicial, "
	               + "ed.valor_final, ed.valor_escala, ed.valor_defecto, ed.alertar, ed.valor_alertar, "
	               + "ed.orden, ed.porcentaje, e.dependencia "
	               + "FROM encuesta_laboral_detalle ed "
	               + "INNER JOIN encuesta_laboral e ON e.idencuesta = ed.idencuesta "
	               + "WHERE e.idencuesta = ? "
	               + "ORDER BY ed.orden";

	    try (Connection con = new ConexionBaseDatos().obtenerConexionBDGeneral();
	         PreparedStatement ps = con.prepareStatement(sql)) {

	        ps.setInt(1, idEncuesta);
	        ResultSet rs = ps.executeQuery();

	        while (rs.next()) {

	            int idDetalle = rs.getInt("idencuestadetalle");
	            String descripcion = rs.getString("descripcion");
	            String tipoRespuesta = rs.getString("tipo_respuesta");
	            double valorInicial = rs.getDouble("valor_inicial");
	            double valorFinal = rs.getDouble("valor_final");
	            double valorEscala = rs.getDouble("valor_escala");
	            double valorDefecto = rs.getDouble("valor_defecto");
	            String alertar = rs.getString("alertar");
	            String valorAlertar = rs.getString("valor_alertar");
	            int orden = rs.getInt("orden");
	            float porcentaje = rs.getFloat("porcentaje");
	            String dependencia = rs.getString("dependencia");

	            EncuestaLaboralDetalle detalle = new EncuestaLaboralDetalle(
	                idDetalle, idEncuesta, descripcion, tipoRespuesta,
	                valorInicial, valorFinal, valorEscala, valorDefecto,
	                alertar, valorAlertar, orden, porcentaje
	            );
	            detalle.setDependencia(dependencia);
	            detalles.add(detalle);
	        }

	    } catch (SQLException e) {
	        logger.error("Error al obtener encuesta laboral detalle: " + e.toString());
	    }

	    return detalles;
	}

	


}

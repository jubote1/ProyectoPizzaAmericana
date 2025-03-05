package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.PeriodoNomina;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class PeriodoNominaDAO {
	
	/**
	 * M�todo que se encarga de retornar la informaci�n de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<PeriodoNomina> obtenerPeriodosNomina()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<PeriodoNomina> periodos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from periodo_nomina";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idPeriodo;
			Date fechaInferior;
			Date fechaSuperior;
			PeriodoNomina perTem;
			while(rs.next()){
				idPeriodo = rs.getInt("idperiodo");
				fechaInferior = rs.getDate("fecha_inferior");
				fechaSuperior = rs.getDate("fecha_superior");
				perTem =  new PeriodoNomina(idPeriodo, fechaInferior, fechaSuperior);
				periodos.add(perTem);
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
		return(periodos);
		
	}
	
}

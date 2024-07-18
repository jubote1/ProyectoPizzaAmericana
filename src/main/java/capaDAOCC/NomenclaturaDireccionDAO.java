package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

public class NomenclaturaDireccionDAO {

	public static int obtenerIdNomenclatura(String nomenclatura)
	{
		Logger logger = Logger.getLogger("log_file");
		int idNomenclatura=0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idnomenclatura from nomenclatura_direccion where LOWER(nomenclatura) = '"+ nomenclatura.toLowerCase() + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idNomenclatura = rs.getInt("idnomenclatura");
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
		return(idNomenclatura);
	}
	
}

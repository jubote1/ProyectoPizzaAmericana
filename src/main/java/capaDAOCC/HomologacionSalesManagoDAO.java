package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.HomologacionSalesManago;
import capaModeloCC.TiendaFranquiciaWompi;
import conexionCC.ConexionBaseDatos;

public class HomologacionSalesManagoDAO {
	
	public static HomologacionSalesManago obtenerHomologacionSalesManago(HomologacionSalesManago homol)
	{
		HomologacionSalesManago homolResp = new HomologacionSalesManago();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String id = "";
		String nombre = "";
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "SELECT * FROM homologacion_sales_manago WHERE tamano = '" + homol.getTamano() + "' AND name = '" + homol.getName() + "' AND especialidad = '" + homol.getEspecialidad() + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				id = rs.getString("id");
				nombre = rs.getString("nombre");
				break;
			}
			homolResp = new HomologacionSalesManago(id,nombre,homol.getTamano(), homol.getName(), homol.getEspecialidad());
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
		return(homolResp);
	}

}

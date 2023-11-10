package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.HomologacionProductoRappi;
import capaModeloCC.HomologacionTiendaToken;
import conexionCC.ConexionBaseDatos;

public class HomologacionProductoRappiDAO {
	
	public static HomologacionProductoRappi obtenerHomologacionProductoRappi(String nombre)
	{
		Logger logger = Logger.getLogger("log_file");
		HomologacionProductoRappi homoResp = new HomologacionProductoRappi("",0,0,0,0);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from homologacion_producto_rappi where nombre = '" + nombre +"'"; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idProducto;
			int idEspecialidad;
			int idExcepcion;
			double precio;
			while(rs.next()){
				idProducto = rs.getInt("idproducto");
				idEspecialidad = rs.getInt("idespecialidad");
				idExcepcion = rs.getInt("idexcepcion");
				precio = rs.getDouble("precio");
				homoResp = new HomologacionProductoRappi(nombre,idProducto, idEspecialidad, idExcepcion, precio);
				break;
			}
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(homoResp);
	}
	

}

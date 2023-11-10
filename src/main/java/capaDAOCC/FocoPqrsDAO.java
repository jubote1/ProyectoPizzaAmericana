package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.EstadoPedido;
import capaModeloCC.FocoPqrs;
import capaModeloCC.FormaPago;
import conexionCC.ConexionBaseDatos;

/**
 * Clase que se encarga de implementar los métodos que se encargarán de la interacción de base de datos con la entidad
 * FormaPago
 * @author JuanDavid
 *
 */
public class FocoPqrsDAO {
	
	/**
	 * Método que se encarga de retornar los focos pqrs paramétrizados en el sistema para la radicación de PQRS.
	 * @return Un arrayList con los focosPqrs existentes en base de datos
	 */
	public static ArrayList<FocoPqrs> obtenerFocosPqrs()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<FocoPqrs> focosPqrs = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from foco_pqrs";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idfoco;
			String nombreFoco;
			while(rs.next()){
				idfoco = rs.getInt("idfoco");
				nombreFoco = rs.getString("nombre_foco");
				
				FocoPqrs focoPqrs = new FocoPqrs( idfoco, nombreFoco);
				focosPqrs.add(focoPqrs);
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
		return(focosPqrs);
		
	}
	
	public static FocoPqrs retornarFocoPqrs(int idFoco)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		FocoPqrs foco = new FocoPqrs(0,"");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from  foco_pqrs  where idfoco = " + idFoco; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idfoco = 0;
			String nombreFoco = "";
			while(rs.next()){
				idfoco = rs.getInt("idfoco");
				nombreFoco = rs.getString("nombre_foco");
				break;
			}
			foco = new FocoPqrs(idfoco, nombreFoco);
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
		return(foco);
	}

}

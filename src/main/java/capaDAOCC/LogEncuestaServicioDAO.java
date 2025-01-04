package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import capaModeloCC.LogEncuestaServicio;
import conexionCC.ConexionBaseDatos;

public class LogEncuestaServicioDAO {
	
	
	
	public static boolean insertarLogEncuestaServicio(LogEncuestaServicio encuestaServicio)
	{
		boolean resultado = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_encuesta_servicio (numero_pedido,nombre_cliente,telefono,idtienda) values("+ encuestaServicio.getNumeroPedido() +",'" + encuestaServicio.getNombreCliente() + "','" + encuestaServicio.getTelefono() + "'," + encuestaServicio.getIdTienda() + ")" ;
			logger.info(insert);
			stm.executeUpdate(insert);
			resultado = true;
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
		return(resultado);
	}
	
	public static boolean actualizarLlenadoLogEncuestaServicio(int idTienda, int numeroPedido)
	{
		boolean resultado = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "update log_encuesta_servicio set contestado = 'S', hora_llenado = CURTIME() where idtienda =  " + idTienda + " and numero_pedido = " + numeroPedido;
			logger.info(insert);
			stm.executeUpdate(insert);
			resultado = true;
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
		return(resultado);
	}

}

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
	
	/**
	 * Método que se encargará de indicarnos si se debe o no enviar las encuestas de acuerdo a una definición dada con el cliente y con base
	 * en el número de teléfono, que será nuestra clave principal, dado que el envío del mensaje es por WhatsApp
	 * @param telefono
	 * @return
	 */
	public static boolean validarEncuestaServicio(String telefono)
	{
		boolean resultado = false;
		boolean noHayEncuestas = true;
		ArrayList<LogEncuestaServicio> encuestasServicio = new ArrayList();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT * FROM log_encuesta_servicio WHERE hora_envio >= DATE_SUB(NOW(),INTERVAL 60 DAY) AND telefono = '" + telefono + "'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			int idEncuesta, numeroPedido,idTienda;
			String nombreCliente,horaEnvio,contestado,horaLlenado;
			LogEncuestaServicio logTemp;
			while(rs.next())
			{
				noHayEncuestas = false;
				idEncuesta = rs.getInt("idencuesta");
				numeroPedido = rs.getInt("numero_pedido");
				nombreCliente = rs.getString("nombre_cliente");
				idTienda = rs.getInt("idtienda");
				horaEnvio = rs.getString("hora_envio");
				contestado = rs.getString("contestado");
				horaLlenado = rs.getString("hora_llenado");
				if(horaLlenado == null || horaLlenado.equals(new String("null")))
				{
					horaLlenado = "";
				}
				logTemp = new LogEncuestaServicio(idEncuesta, numeroPedido, nombreCliente, telefono, idTienda,
						horaEnvio, contestado, horaLlenado);
				encuestasServicio.add(logTemp);
			}
			if(noHayEncuestas)
			{
				resultado = true;
			}else
			{
				//Se realiza el recorrido de las encuestas y se saca el porcentaje
				int encuestaContestada = 0;
				int encuestaNoContestada = 0;
				for(int i = 0 ; i < encuestasServicio.size(); i++)
				{
					logTemp = encuestasServicio.get(i);
					if(logTemp.getHoraLlenado().equals(new String("")))
					{
						encuestaNoContestada++;
					}else
					{
						encuestaContestada++;
					}
				}
				//Terminado el conteo de las encuestas contestadas y no contestadas sacamos el porcentaje de las encuestas contestadas sobre el total
				double porEncCon = (encuestaContestada/(encuestaContestada + encuestaNoContestada))*100;
				if(porEncCon >= 50)
				{
					resultado = true;
				}
			}
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

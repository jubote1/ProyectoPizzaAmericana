package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ClienteAlerta;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class ClienteAlertaDAO {
	
		/**
		 * Método que se encarga de retornar dado un teléfono el mensaje guardado para dicho cliente.
		 * @param telefono
		 * @return
		 */
		public static ClienteAlerta obtenerClienteAlerta(String telefono)
		{
			Logger logger = Logger.getLogger("log_file");
			ClienteAlerta clienteAlerta = new ClienteAlerta();
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			String tipoAlerta = "";
			String mensaje = "";
			try
			{
				Statement stm = con1.createStatement();
				String consulta = "select * from cliente_alerta where telefono = '"+ telefono + "'";
				logger.info(consulta);
				ResultSet rs = stm.executeQuery(consulta);
				while(rs.next()){
					tipoAlerta = rs.getString("tipo_alerta");
					mensaje = rs.getString("mensaje");
					break;
				}
				clienteAlerta = new ClienteAlerta(telefono, tipoAlerta, mensaje);
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
			return(clienteAlerta);
		}

}

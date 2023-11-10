package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.DireccionFueraZona;
import capaModeloCC.Pedido;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

public class DireccionFueraZonaDAO {

	
	public static int insertarDirFueraZona(DireccionFueraZona dirFuera)
	{
		Logger logger = Logger.getLogger("log_file");
		int id = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into direccion_fuera_zona (direccion, municipio,idcliente, latitud, longitud,telefono, nombre, apellido) values ('" + dirFuera.getDireccion() + "', '" + dirFuera.getMunicipio() + "' , " + dirFuera.getIdCliente() + " , " + dirFuera.getLatitud() + " , " + dirFuera.getLongitud() + ", '" + dirFuera.getTelefono() + "' , '" + dirFuera.getNombre() + "' , '" + dirFuera.getApellido()  +  "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				id = rs.getInt(1);
				
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
			return(0);
		}
		return(id);
	}
	
	
	public static ArrayList<DireccionFueraZona> ConsultaDirFueraZona(String fechainicial, String fechafinal, String muni)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <DireccionFueraZona> consultaDirs = new ArrayList();
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2) + " 00:00:00";	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2) + " 23:59:00";	
		//Modificamos consulta para incluir el número de pedidos que tiene el cliente, para realizar un control
		//Validamos si el municipio es igual a cero es porque vamos a consultar todos los municipio, sino es así
		// es porque la consulta deberá filtrar por municipio.
		if(muni.equals(new String ("TODOS")))
		{
			consulta = "select a.*, (select count(*) from pedido b where b.idcliente = a.idcliente) as pedidos from direccion_fuera_zona a where a.fecha_ingreso >= '"+ fechaini + "' and a.fecha_ingreso <= '" + fechafin + "'";
		}else
		{
			consulta = "select a.*, (select count(*) from pedido b where b.idcliente = a.idcliente) as pedidos from direccion_fuera_zona a where a.municipio = '" + muni + "'  and a.fecha_ingreso >= '"+ fechaini + "' and a.fecha_ingreso <= '" + fechafin + "'";
		}
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int id = 0;
			String direccion = "";
			String municipio = "";
			int idCliente = 0;
			double latitud = 0;
			double longitud = 0;
			String telefono = "";
			String nombre = "";
			String apellido = "";
			String fechaIngreso ="";
			int pedidos = 0;
			while(rs.next())
			{
				id = rs.getInt("id");
				direccion = rs.getString("direccion");
				municipio = rs.getString("municipio");
				idCliente = rs.getInt("idcliente");
				latitud = rs.getDouble("latitud");
				longitud = rs.getDouble("longitud");
				telefono = rs.getString("telefono");
				nombre = rs.getString("nombre");
				apellido = rs.getString("apellido");
				fechaIngreso = rs.getString("fecha_ingreso");
				pedidos = rs.getInt("pedidos");
				//Luego de tomada la información de la cantidad de pedidos, validamos que los pedidos llevados al cliente seran 0 o 1.
				if (pedidos <= 1)
				{
					DireccionFueraZona dirFuera = new DireccionFueraZona(id, direccion, municipio, idCliente, latitud, longitud, telefono, nombre, apellido,0);
					dirFuera.setFechaIngreso(fechaIngreso);
					consultaDirs.add(dirFuera);
				}
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaDirs);
	}
	
	
}

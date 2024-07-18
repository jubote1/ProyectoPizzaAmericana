package capaDAOCC;

import java.sql.Connection;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import capaModeloCC.Cliente;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que se encarga de todo lo relacionado con clientes y la interacci�n con la base de datos
 * @author JuanDavid
 *
 */
public class ClienteDAO {
	
	/**
	 * 
	 * @param tel Dado el telef�no de un cliente se encarga de retornar en un array list de objetos tipo liente
	 * la informaci�n de los registros que coincidente con dicho tel�fono.
	 * @return ArrayList de tipo cliente con la informaci�n de clientes que coinciden con el tel�fono dado.
	 */
	public static ArrayList<Cliente> obtenerCliente(String tel)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Cliente> clientes = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.zona_tienda, a.telefono_celular, a.email, a.politica_datos, a.fecha_nacimiento, a.facturar_sin, a.email_facturacion, a.idtipopersona, a.identificacion from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura where a.telefono = '" + tel +"' and activo = 1";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int idTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			String zonaTienda;
			String telefonoCelular;
			String email;
			String politicaDatos;
			String fechaNacimiento;
			String clienteSinIden;
			String emailFact;
			int idTipoPersona;
			String identificacion;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				distanciaTienda = rs.getDouble("distancia_tienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				zonaTienda = rs.getString("zona_tienda");
				telefonoCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				fechaNacimiento = rs.getString("fecha_nacimiento");
				Cliente clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clien.setZonaTienda(zonaTienda);
				clien.setTelefonoCelular(telefonoCelular);
				clien.setEmail(email);
				clien.setPoliticaDatos(politicaDatos);
				if(fechaNacimiento == null)
				{
					fechaNacimiento = "";
				}
				clienteSinIden = rs.getString("facturar_sin");
				emailFact = rs.getString("email_facturacion");
				if(emailFact == null)
				{
					emailFact = "";
				}
				idTipoPersona = rs.getInt("idtipopersona");
				identificacion = rs.getString("identificacion");
				if(identificacion == null)
				{
					identificacion = "";
				}
				clien.setFechaNacimiento(fechaNacimiento);
				clien.setClienteSinIden(clienteSinIden);
				clien.setEmailFacturacion(emailFact);
				clien.setIdTipoPersona(idTipoPersona);
				clien.setIdentificacion(identificacion);
				clientes.add(clien);
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
		return(clientes);
		
	}
	
	/**
	 * M�todo que retorna el �ltimo cliente asociado a  un pedido por parte de este cliente
	 * con el fin de dar la informaci�n m�s actualizada posible
	 * @param tel
	 * @return
	 */
	public static Cliente obtenerClienteUltimoPedido(String tel)
	{
		Logger logger = Logger.getLogger("log_file");
		Cliente cliente = new Cliente();
		cliente.setIdcliente(0);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.zona_tienda, a.telefono_celular, a.email, a.politica_datos from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura left outer join pedido e on a.idcliente = e.idcliente where (a.telefono = '" + tel +"' or a.telefono_celular = '" + tel + "') and activo = 1 order by e.idpedido desc";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int idTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			String zonaTienda;
			String telefonoCelular;
			String email;
			String politicaDatos;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				distanciaTienda = rs.getDouble("distancia_tienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				zonaTienda = rs.getString("zona_tienda");
				telefonoCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				cliente = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				cliente.setZonaTienda(zonaTienda);
				cliente.setTelefonoCelular(telefonoCelular);
				cliente.setEmail(email);
				cliente.setPoliticaDatos(politicaDatos);
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
		return(cliente);
		
	}
	
	
	/**
	 * 
	 * @param tel Dado el telef�no de un cliente se encarga de retornar en un array list de objetos tipo liente
	 * la informaci�n de los registros que coincidente con dicho tel�fono.
	 * @return ArrayList de tipo cliente con la informaci�n de clientes que coinciden con el tel�fono dado.
	 */
	public static ArrayList<Cliente> obtenerClienteTodos(String tel)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Cliente> clientes = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.telefono_celular, a.email, a.politica_datos from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura where a.telefono = '" + tel +"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int idTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			int estado;
			String telefonoCelular;
			String email;
			String politicaDatos;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				distanciaTienda = rs.getDouble("distancia_tienda");
				idTienda = rs.getInt("idtienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				estado = rs.getInt("activo");
				telefonoCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				Cliente clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clien.setEstado(estado);
				clien.setTelefonoCelular(telefonoCelular);
				clien.setEmail(email);
				clien.setPoliticaDatos(politicaDatos);
				clientes.add(clien);
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
		return(clientes);
		
	}
	
	/**
	 * M�todo que se encarga de insertar en la base de datos un cliente
	 * @param clienteInsertar Se recibe como par�metro un objeto Modelo Cliente con base en el cual se inserta el cliente.
	 * @return  Se retorna un int con el valor del idcliente insertado en la base de datos.
	 */
	public static int insertarCliente(Cliente clienteInsertar)
	{
		Logger logger = Logger.getLogger("log_file");
		int idClienteInsertado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			String fechaNacimiento = "";
			if(clienteInsertar.getFechaNacimiento()==null)
			{
				clienteInsertar.setFechaNacimiento("");
			}
			if(clienteInsertar.getFechaNacimiento().equals(new String("")))
			{
				fechaNacimiento = null;
			}else
			{
				Date fechaTemporal = new Date();
				DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
				String fechaPedidoFinal ="";
				try
				{
					fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(clienteInsertar.getFechaNacimiento());
					fechaPedidoFinal = formatoFinal.format(fechaTemporal);
					
				}catch(Exception e){
					logger.error(e.toString());
					return(0);
				}
				fechaNacimiento = "'"+fechaPedidoFinal+"'";
			}
			//Controlamos los datos que estamos adicionando para facturación electrónica
			if(clienteInsertar.getClienteSinIden()==null)
			{
				clienteInsertar.setClienteSinIden("S");
			}else if(clienteInsertar.getClienteSinIden().equals(new String("")))
			{
				clienteInsertar.setClienteSinIden("S");
			}
			if(clienteInsertar.getEmailFacturacion()==null)
			{
				clienteInsertar.setEmailFacturacion("");
			}
			if(clienteInsertar.getIdTipoPersona()==0)
			{
				clienteInsertar.setIdTipoPersona(2);
			}
			if(clienteInsertar.getIdentificacion()==null)
			{
				clienteInsertar.setIdentificacion("");
			}
			Statement stm = con1.createStatement();
			String insert = "insert into cliente (idtienda,nombre, apellido, nombrecompania, direccion, zona, telefono, observacion, idmunicipio, latitud, longitud, distancia_tienda, idnomenclatura, num_nomencla1, num_nomencla2, num3, telefono_celular, email, politica_datos, fecha_nacimiento,facturar_sin,email_facturacion,idtipopersona,identificacion) values (" + clienteInsertar.getIdtienda() + ", '" +clienteInsertar.getNombres() + "' , '" + clienteInsertar.getApellidos() + "' , '" + clienteInsertar.getNombreCompania() + "' , '" + clienteInsertar.getDireccion() + "' , '" + clienteInsertar.getZonaDireccion() +"' , '" + clienteInsertar.getTelefono() + "' , '" + clienteInsertar.getObservacion() + "' , " + clienteInsertar.getIdMunicipio() + " , " + clienteInsertar.getLatitud() + " , " + clienteInsertar.getLontitud() + " , " + clienteInsertar.getDistanciaTienda() + " , " + clienteInsertar.getIdnomenclatura() + " , '" + clienteInsertar.getNumNomenclatura() + "' , '" + clienteInsertar.getNumNomenclatura2() + "' ,  '" + clienteInsertar.getNum3() + "' , '" + clienteInsertar.getTelefonoCelular() + "' , '" + clienteInsertar.getEmail() + "' , '" + clienteInsertar.getPoliticaDatos() + "' ," + fechaNacimiento + " , '" + clienteInsertar.getClienteSinIden() +"' , '" + clienteInsertar.getEmailFacturacion() + "' , " + clienteInsertar.getIdTipoPersona() + " , '" + clienteInsertar.getIdentificacion() + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idClienteInsertado=rs.getInt(1);
				
	        }
			stm.close();
			rs.close();
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
		return(idClienteInsertado);
	}
	
	/**
	 * M�todo que se encarga de retonar un objeto Modelo CLiente, con base en un id que env�a como par�metro.
	 * @param id Se recibe un id cliente y con base en este se busca en base de datos.
	 * @return Se retorna un objeto Modelo Cliente con la informaci�n del cliente.
	 */
	public static Cliente obtenerClienteporID(int id)
	{
		Logger logger = Logger.getLogger("log_file");
		Cliente clienteConsultado = new Cliente(); 
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, c.idmunicipio, a.latitud, a.longitud, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.distancia_tienda, a.telefono_celular, a.email, a.politica_datos, a.observacion_virtual, a.email_facturacion, a.facturar_sin, a.idtipopersona, a.identificacion from cliente a JOIN tienda b ON a.idtienda = b.idtienda JOIN municipio c ON a.idmunicipio = c.idmunicipio left join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura  where a.idcliente = " + id +"";
			logger.info(consulta);
			System.out.println(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			int idMunicipio;
			float latitud;
			float longitud;
			int idTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			double distanciaTienda;
			String telefonoCelular;
			String email; 
			String politicaDatos;
			String observacionVirtual;
			String emailFact;
			String clienteSinIden;
			int idTipoPersona;
			String identificacion;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				idMunicipio = rs.getInt("idmunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				distanciaTienda = rs.getDouble("distancia_tienda");
				email = rs.getString("email");
				telefonoCelular = rs.getString("telefono_celular");
				politicaDatos = rs.getString("politica_datos");
				observacionVirtual = rs.getString("observacion_virtual");
				emailFact = rs.getString("email_facturacion");
				clienteSinIden = rs.getString("facturar_sin");
				idTipoPersona = rs.getInt("idtipopersona");
				identificacion = rs.getString("identificacion");
				clienteConsultado = new Cliente( idcliente, telefono, nombreCliente, apellido, nombreCompania, direccion,municipio, latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda,memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clienteConsultado.setIdMunicipio(idMunicipio);
				clienteConsultado.setEmail(email);
				clienteConsultado.setTelefonoCelular(telefonoCelular);
				clienteConsultado.setPoliticaDatos(politicaDatos);
				clienteConsultado.setObservacionVirtual(observacionVirtual);
				clienteConsultado.setEmailFacturacion(emailFact);
				clienteConsultado.setClienteSinIden(clienteSinIden);
				clienteConsultado.setIdTipoPersona(idTipoPersona);
				clienteConsultado.setIdentificacion(identificacion);
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
		return(clienteConsultado);
		
	}
	
	/**
	 * M�todo que busca actualizar la informaci�n de un cliente con base en un objeto Modelo Cliente enviado como par�metro.
	 * @param clienteAct Se env�a como par�metro un tipo de Modelo Cliente con base en el cual se hace la actualizaci�n.
	 * @return Se retorna un valor entero con el valor del id cliente actualizado en el sistema.
	 */
	public static int actualizarCliente(Cliente clienteAct)
	{
		Logger logger = Logger.getLogger("log_file");
		int idClienteActualizado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String adiObserVirtual = "";
		if(clienteAct.getObservacionVirtual() != null)
		{
			if(clienteAct.getObservacionVirtual().length() > 0)
			{
				if(clienteAct.getObservacionVirtual().length() > 3000)
				{
					adiObserVirtual = adiObserVirtual.substring(0, 3000);
				}
				adiObserVirtual = " , observacion_virtual = '" + clienteAct.getObservacionVirtual() + "' ";
			}
		}
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String fechaNacimiento = "";
			if(clienteAct.getFechaNacimiento()==null)
			{
				clienteAct.setFechaNacimiento("");
			}
			if(clienteAct.getFechaNacimiento().equals(new String("")))
			{
				fechaNacimiento = null;
			}else
			{
				Date fechaTemporal = new Date();
				DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
				String fechaPedidoFinal ="";
				try
				{
					fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(clienteAct.getFechaNacimiento());
					fechaPedidoFinal = formatoFinal.format(fechaTemporal);
					
				}catch(Exception e){
					logger.error(e.toString());
					return(0);
				}
				fechaNacimiento = "'"+fechaPedidoFinal+"'";
			}
			//Controlamos los datos que estamos adicionando para facturación electrónica
			if(clienteAct.getClienteSinIden()==null)
			{
				clienteAct.setClienteSinIden("S");
			}else if(clienteAct.getClienteSinIden().equals(new String("")))
			{
				clienteAct.setClienteSinIden("S");
			}
			if(clienteAct.getEmailFacturacion()==null)
			{
				clienteAct.setEmailFacturacion("");
			}
			if(clienteAct.getIdTipoPersona()==0)
			{
				clienteAct.setIdTipoPersona(2);
			}
			if(clienteAct.getIdentificacion()==null)
			{
				clienteAct.setIdentificacion("");
			}
			if(clienteAct.getIdcliente() > 0)
			{
				String update = "update cliente set telefono  = '" + clienteAct.getTelefono() +"' , nombre = '" + clienteAct.getNombres() + "' , direccion = '" + clienteAct.getDireccion() + "' , idmunicipio = " + clienteAct.getIdMunicipio() + " , latitud = " + clienteAct.getLatitud() + " , longitud = " + clienteAct.getLontitud() + " , distancia_tienda = " + clienteAct.getDistanciaTienda()  + " , zona = '" + clienteAct.getZonaDireccion() + "' , observacion = '" + clienteAct.getObservacion() +"', apellido = '" + clienteAct.getApellidos() + "' , nombrecompania = '" + clienteAct.getNombreCompania() + "' , idnomenclatura = " + clienteAct.getIdnomenclatura() + " , num_nomencla1 = '" + clienteAct.getNumNomenclatura() + "' , num_nomencla2 = '" + clienteAct.getNumNomenclatura2() + "' , num3 =  '" + clienteAct.getNum3()  + "' , telefono_celular = '" + clienteAct.getTelefonoCelular() + "' , email = '" + clienteAct.getEmail() + "' , politica_datos= '" + clienteAct.getPoliticaDatos() + "' , idtienda = " + clienteAct.getIdtienda() + adiObserVirtual + " , fecha_nacimiento = " + fechaNacimiento + " , facturar_sin = '" + clienteAct.getClienteSinIden() + "' , email_facturacion = '" + clienteAct.getEmailFacturacion() + "' , idtipopersona = " + clienteAct.getIdTipoPersona() + " , identificacion = '" + clienteAct.getIdentificacion() +"' where idcliente = " + clienteAct.getIdcliente(); 
				logger.info(update);
				stm.executeUpdate(update);
				idClienteActualizado = clienteAct.getIdcliente();
			}else
			{
				logger.info("No se pudo hacer actualizaci�n dado que el idCliente venia en ceros o vac�o");
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
		logger.info("id cliente actualizado" + idClienteActualizado);
		return(idClienteActualizado);
	}
	
	/**
	 * El campo memcode corresponde al consecutivo cliente que tiene en la base de datos de la tienda, como la inserci�n
	 * es asincrona, se requiere que luego de insertado el cliente en nuestro sistema y luego de insertado el pedido en la tienda
	 * donde el cliente nuevo tiene un memcode, venimos y actualizamos el memcode en nuestro sistema.
	 * @param idCliente Se recibe como par�metro el idcliente al cual se le va a actualizar el memcode.
	 * @param memcode Valor de memcode a actualizar.
	 * @return Se retorna un entero con el idcliente actualizado.
	 */
	public static int actualizarClienteMemcode(int idCliente, int memcode)
	{
		Logger logger = Logger.getLogger("log_file");
		int idClienteActualizado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update cliente set memcode = " + memcode + "  where idcliente = " + idCliente; 
			logger.info(update);
			stm.executeUpdate(update);
			idClienteActualizado = idCliente;
			
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
		logger.info("id cliente actualizado" + idClienteActualizado);
		return(idClienteActualizado);
	}
	
	/**
	 * Método que se encarga de enviar un email incorrecto en caso de que sea fallido un envio de correo
	 * @param idCliente
	 * @return
	 */
	public static int marcarCorreoIncorrecto(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		int idClienteActualizado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update cliente set email_correcto = 'N'  where idcliente = " + idCliente; 
			logger.info(update);
			stm.executeUpdate(update);
			idClienteActualizado = idCliente;
			
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
		logger.info("id cliente actualizado" + idClienteActualizado);
		return(idClienteActualizado);
	}
	
	public static ArrayList<Cliente> ObtenerClientesTienda(int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Cliente> clientes = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda,  a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura from cliente a,tienda b, municipio c, nomenclatura_direccion d where a.idnomenclatura = d.idnomenclatura and a.idtienda = b.idtienda and a.idmunicipio = c.idmunicipio and a.idtienda = " + idtienda +" and a.idcliente not in (select b.idcliente from geolocaliza_masivo_tienda b )" ;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int idTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				distanciaTienda = rs.getDouble("distancia_tienda");
				idTienda = rs.getInt("idtienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				Cliente clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode, idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clientes.add(clien);
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
		return(clientes);
		
	}
	
	public static boolean InsertarClienteGeolocalizado(int idcliente,  String direccion, String municipio, int idtiendaanterior, int idtiendaactual)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into geolocaliza_masivo_tienda (idcliente,direccionbuscada,municipio,idtiendaanterior,idtiendaactual) values (" + idcliente + ", '" + direccion + "' , '" + municipio  + "' , " + idtiendaanterior + " , " + idtiendaactual + ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			stm.close();
			rs.close();
			con1.close();
			return(true);
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
				return(false);
			}catch(Exception e1)
			{
				return(false);
			}
			
		}
		
	}
	
	
	/**
	 * M�todo en la capa DAO que se encarga de inactivar un cliente en caso de as� se requiera
	 * @param idCliente se recibe como par�metro el id del cliente, el cual es el identificador �nico.
	 * @return Se retorna un valor boolean que indica si el proceso fue o no exitoso.
	 */
	public static boolean inactivarCliente(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update cliente set activo = 0  where idcliente = " + idCliente; 
			logger.info(update);
			stm.executeUpdate(update);
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
			return(false);
		}
		return(true);
	}
	
	/**
	 * M�todo en la capa DAO que se encarga de activar un cliente en caso de as� se requiera
	 * @param idCliente se recibe como par�metro el id del cliente, el cual es el identificador �nico.
	 * @return Se retorna un valor boolean que indica si el proceso fue o no exitoso.
	 */
	public static boolean activarCliente(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update cliente set activo = 1  where idcliente = " + idCliente; 
			logger.info(update);
			stm.executeUpdate(update);
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
			return(false);
		}
		return(true);
	}
	
	/**
	 * M�todo que se encarga de retornar un cliente con base en el tel�fono y el idtienda, en teor�a esta es una combinaci�n que 
	 * deber�a ser �nica.
	 * @param tel
	 * @param idTienda
	 * @return Se retornar�a un objeto de tipo Cliente con la informaci�n.
	 */
	public static Cliente obtenerCliente(String tel, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		Cliente clien = new Cliente();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, a.idmunicipio, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.zona_tienda, a.telefono_celular, a.email, a.politica_datos from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura where a.telefono = '" + tel +"' and a.idtienda = " + idTienda + " and activo = 1";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			int idMunicipio;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			String zonaTienda;
			String telefonoCelular;
			String email;
			String politicaDatos;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				idMunicipio = rs.getInt("idmunicipio");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				distanciaTienda = rs.getDouble("distancia_tienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				if(numNomenclatura1 == null)
				{
					numNomenclatura1 = "";
				}
				numNomenclatura2 = rs.getString("num_nomencla2");
				if(numNomenclatura2 == null)
				{
					numNomenclatura2 = "";
				}
				num3 = rs.getString("num3");
				if(num3 == null)
				{
					num3 = "";
				}
				nomenclatura = rs.getString("nomenclatura");
				zonaTienda = rs.getString("zona_tienda");
				telefonoCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clien.setIdMunicipio(idMunicipio);
				clien.setZonaTienda(zonaTienda);
				clien.setTelefonoCelular(telefonoCelular);
				clien.setEmail(email);
				clien.setPoliticaDatos(politicaDatos);
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
		return(clien);
		
	}
	
	
	public static Cliente obtenerClienteTiendaVirtual(String tel, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		Cliente clien = new Cliente();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente, a.idmunicipio, b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.zona_tienda, a.telefono_celular, a.email, a.politica_datos from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura where a.telefono = '" + tel +"' and a.idtienda = " + idTienda + " and activo = 1 and a.idcliente not in(select g.idcliente from pedido g where g.fechapedido = CURDATE())";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			int idMunicipio;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			String zonaTienda;
			String telefonoCelular;
			String email;
			String politicaDatos;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				idMunicipio = rs.getInt("idmunicipio");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				distanciaTienda = rs.getDouble("distancia_tienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				if(numNomenclatura1 == null)
				{
					numNomenclatura1 = "";
				}
				numNomenclatura2 = rs.getString("num_nomencla2");
				if(numNomenclatura2 == null)
				{
					numNomenclatura2 = "";
				}
				num3 = rs.getString("num3");
				if(num3 == null)
				{
					num3 = "";
				}
				nomenclatura = rs.getString("nomenclatura");
				zonaTienda = rs.getString("zona_tienda");
				telefonoCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clien.setIdMunicipio(idMunicipio);
				clien.setZonaTienda(zonaTienda);
				clien.setTelefonoCelular(telefonoCelular);
				clien.setEmail(email);
				clien.setPoliticaDatos(politicaDatos);
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
		return(clien);
		
	}
	
	
	public static Cliente obtenerClienteCelular(String telefonoCelular, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		Cliente clien = new Cliente();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente , a.idmunicipio,  b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.zona_tienda, a.telefono_celular, a.email, a.politica_datos from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura where a.telefono_celular = '" + telefonoCelular +"' and a.idtienda = " + idTienda + " and activo = 1";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			int idMunicipio;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			String zonaTienda;
			String email;
			String politicaDatos;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				idMunicipio = rs.getInt("idmunicipio");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				distanciaTienda = rs.getDouble("distancia_tienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				zonaTienda = rs.getString("zona_tienda");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clien.setIdMunicipio(idMunicipio);
				clien.setZonaTienda(zonaTienda);
				clien.setTelefonoCelular(telefonoCelular);
				clien.setEmail(email);
				clien.setPoliticaDatos(politicaDatos);
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
		return(clien);
		
	}
	
	
	public static Cliente obtenerClienteCelularTiendaVirtual(String telefonoCelular, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		Cliente clien = new Cliente();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idcliente , a.idmunicipio,  b.nombre nombreTienda, a.idtienda, a.nombre, a.apellido, a.nombrecompania, a.direccion, a.zona, a.observacion, a.telefono, c.nombre nombremunicipio, a.latitud, a.longitud, a.distancia_tienda, a.memcode, a.idnomenclatura, a.num_nomencla1, a.num_nomencla2, a.num3, d.nomenclatura, a.activo, a.zona_tienda, a.telefono_celular, a.email, a.politica_datos from cliente a left outer join tienda b on a.idtienda = b.idtienda left outer join municipio c on a.idmunicipio = c.idmunicipio left outer join nomenclatura_direccion d on a.idnomenclatura = d.idnomenclatura where a.telefono_celular = '" + telefonoCelular +"' and a.idtienda = " + idTienda + " and activo = 1 and a.idcliente not in(select g.idcliente from pedido g where g.fechapedido = CURDATE())";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idcliente;
			int idMunicipio;
			String nombreTienda;
			String nombreCliente;
			String apellido;
			String nombreCompania;
			String direccion;
			String zona;
			String observacion;
			String telefono;
			String municipio;
			float latitud;
			float longitud;
			double distanciaTienda;
			int memcode;
			int idnomenclatura;
			String numNomenclatura1;
			String numNomenclatura2;
			String num3;
			String nomenclatura;
			String zonaTienda;
			String email;
			String politicaDatos;
			while(rs.next()){
				idcliente = rs.getInt("idcliente");
				idMunicipio = rs.getInt("idmunicipio");
				nombreTienda = rs.getString("nombreTienda");
				nombreCliente = rs.getString("nombre");
				apellido = rs.getString("apellido");
				nombreCompania = rs.getString("nombrecompania");
				direccion = rs.getString("direccion");
				zona = rs.getString("zona");
				observacion = rs.getString("observacion");
				telefono = rs.getString("telefono");
				municipio = rs.getString("nombremunicipio");
				latitud = rs.getFloat("latitud");
				longitud = rs.getFloat("longitud");
				idTienda = rs.getInt("idtienda");
				distanciaTienda = rs.getDouble("distancia_tienda");
				memcode = rs.getInt("memcode");
				idnomenclatura = rs.getInt("idnomenclatura");
				numNomenclatura1 = rs.getString("num_nomencla1");
				numNomenclatura2 = rs.getString("num_nomencla2");
				num3 = rs.getString("num3");
				nomenclatura = rs.getString("nomenclatura");
				zonaTienda = rs.getString("zona_tienda");
				email = rs.getString("email");
				politicaDatos = rs.getString("politica_datos");
				clien = new Cliente( idcliente, telefono, nombreCliente,apellido, nombreCompania, direccion,municipio,latitud, longitud, distanciaTienda, zona, observacion, nombreTienda, idTienda, memcode,idnomenclatura, numNomenclatura1, numNomenclatura2, num3, nomenclatura);
				clien.setIdMunicipio(idMunicipio);
				clien.setZonaTienda(zonaTienda);
				clien.setTelefonoCelular(telefonoCelular);
				clien.setEmail(email);
				clien.setPoliticaDatos(politicaDatos);
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
		return(clien);
		
	}
	
	public static void actualizarClienteCoordenas(int idCliente, float latitud, float longitud)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			if(idCliente > 0)
			{
				String update = "update cliente set latitud  = " + latitud +" , longitud = " + longitud + " where idcliente = " + idCliente; 
				logger.info(update);
				stm.executeUpdate(update);
				System.out.println(update);
			}else
			{
				logger.info("No se pudo hacer actualizaci�n dado que el idCliente venia en ceros o vac�o");
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
	}
	
	public static boolean actualizarClienteDireccion(int idCliente, String direccion, int idMunicipio, float latitud, float longitud, String zona,  String observacion, int idnomenclatura, String numNomenclatura, String numNomenclatura2, String num3)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean retorno = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update cliente set direccion = '" + direccion + "' , idmunicipio = " + idMunicipio + " , latitud = " + latitud + " , longitud = " + longitud + " ,  zona = '" + zona + "' , observacion = '" + observacion +"', idnomenclatura = " + idnomenclatura + " , num_nomencla1 = '" + numNomenclatura + "' , num_nomencla2 = '" + numNomenclatura2 + "' , num3 =  '" + num3  + "'  where idcliente = " + idCliente; 
			logger.info(update);
			stm.executeUpdate(update);
			retorno = true;
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
		return(retorno);
	}
	

}

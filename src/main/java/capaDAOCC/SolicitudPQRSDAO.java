package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import capaModeloCC.FormaPago;
import capaModeloCC.Pedido;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

public class SolicitudPQRSDAO {
	
	/**
	 * M�todo de la capa DAO que se encarga de implementar la inserci�n de la entidad solicitudPQRS, recibiendo como par�metro
	 * un objeto tipo SolicitudPQRS, retornar� como resultado de la inserci�n el idsolicitudPQRS asignado por la base de datos
	 * en base a un campo configurado como autoincrementable en la misma.
	 * @param solicitud Se recibe como par�metro un objeto de la capaModelo SolicitudPQRS
	 * @return Se retorna valor intero con el idSolicitudPQRS asignado por la base de datos.
	 */
	public static int insertarSolicitudPQRS(SolicitudPQRS solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSolicitudPQRSIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaSolicitudFinal ="";
		try
		{
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(solicitud.getFechaSolicitud());
			fechaSolicitudFinal  = formatoFinal.format(fechaTemporal);
			
		}catch(Exception e){
			logger.error(e.toString());
			return(0);
		}
		String insert = "";
		try
		{
			Statement stm = con1.createStatement();
			insert = "insert into solicitudPQRS (fechasolicitud,tiposolicitud,idcliente, idtienda, nombres, apellidos, telefono, direccion, zona, idmunicipio, comentario, idorigen, idfoco, tipo, area_responsable) values ('" + fechaSolicitudFinal + "', '" + solicitud.getTipoSolicitud() + "', " + solicitud.getIdcliente() + " , " + solicitud.getIdtienda() + " , '"+ solicitud.getNombres() + "' , '"+ solicitud.getApellidos() + "' , '" + solicitud.getTelefono() + "' , '" + solicitud.getDireccion() + "' , '" + solicitud.getZona() + "' , " + solicitud.getIdmunicipio() + " , '" + solicitud.getComentario() + "' , " + solicitud.getIdOrigen() + " , " + solicitud.getIdFoco() + " , '" + solicitud.getTipo() + "' , '" + solicitud.getAreaResponsable() + "')" ; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idSolicitudPQRSIns =rs.getInt(1);
				logger.info("Id SolicitudPQRS insertada en bd " + idSolicitudPQRSIns);
	        }
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(insert + " " + e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idSolicitudPQRSIns);
	}
	
	/**
	 * M�todo que se encarga de la actualizaci�n de una PQRS
	 * @param solicitud
	 * @return
	 */
	public static int actualizarSolicitudPQRS(SolicitudPQRS solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSolicitudPQRSIns = solicitud.getIdsolicitud();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaSolicitudFinal ="";
		try
		{
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(solicitud.getFechaSolicitud());
			fechaSolicitudFinal  = formatoFinal.format(fechaTemporal);
			
		}catch(Exception e){
			logger.error(e.toString());
			return(0);
		}
		String update = "";
		try
		{
			Statement stm = con1.createStatement();
			update = "update solicitudPQRS set fechasolicitud ='" + fechaSolicitudFinal + "', tiposolicitud = '" + solicitud.getTipoSolicitud() + "', idcliente =  " + solicitud.getIdcliente() + " , idtienda = " + solicitud.getIdtienda() + " , nombres = '"+ solicitud.getNombres() + "' , apellidos = '"+ solicitud.getApellidos() + "' , telefono = '" + solicitud.getTelefono() + "' , direccion = '" + solicitud.getDireccion() + "' , zona = '" + solicitud.getZona() + "' , idmunicipio = " + solicitud.getIdmunicipio() + " , comentario ='" + solicitud.getComentario() + "' , idorigen = " + solicitud.getIdOrigen() + " , idfoco = " + solicitud.getIdFoco() + " , tipo = '" + solicitud.getTipo() + "' , area_responsable = '" + solicitud.getAreaResponsable() + "' where idsolicitudpqrs =" + idSolicitudPQRSIns; 
			logger.info(update);
			stm.executeUpdate(update);
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(update + " " + e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idSolicitudPQRSIns);
	}
	
	
	/**
	 * M�todo que se encarga desde la base de datos del descarte de la solicitu de PQRS.
	 * @param idSolicitudPQRS
	 * @return
	 */
	public static int descartarSolicitudPQRS(int idSolicitudPQRS)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String delete = "";
		try
		{
			Statement stm = con1.createStatement();
			delete = "delete from solicitudPQRS  where idsolicitudpqrs =" + idSolicitudPQRS; 
			logger.info(delete);
			stm.executeUpdate(delete);
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(delete + " " + e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idSolicitudPQRS);
	}
	
	public static boolean adicionarComentarioPQRS(int idSolicitudPQRS, String comentario)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		comentario = " ///" + fechaTemporal.toString() + " "	+ comentario;
		try
		{
			Statement stm = con1.createStatement();
			String update = "update solicitudPQRS set comentario = concat( comentario , ' " + comentario + "') where idsolicitudPQRS = " + idSolicitudPQRS;
			logger.info(update);
			stm.executeUpdate(update);
			stm.close();
			con1.close();
			respuesta = true;
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(respuesta);
		}
		return(respuesta);
	}
	
	public static ArrayList<SolicitudPQRS> ConsultaIntegradaSolicitudesPQRS(String fechainicial, String fechafinal, String tienda, String tipoSolicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <SolicitudPQRS> consultaSolicitudes = new ArrayList();
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2);	
		if((fechainicial.length()>0) && (fechafinal.length()>0) && (tienda.length()>0))
		{
			if (tienda.equals("TODAS"))
			{
				consulta = "select idsolicitudPQRS, fechasolicitud, tiposolicitud,nombres,apellidos, direccion , telefono,comentario, a.idorigen, b.nombre_origen, a.idmunicipio, a.idtienda, c.nombre_foco, a.tipo, a.area_responsable, (SELECT COUNT(*) FROM solicitudpqrs_imagenes d WHERE  d.idsolicitudPQRS = a.idsolicitudPQRS) AS imagenes  from solicitudPQRS a, origen_pqrs b, foco_pqrs c where a.idorigen = b.idorigen and a.idfoco = c.idfoco and fechasolicitud >=  '" + fechaini +"' and fechasolicitud <= '"+ fechafin +"'" ;
			}else
			{
				consulta = "select idsolicitudPQRS, fechasolicitud, tiposolicitud,nombres,apellidos, direccion , telefono,comentario, a.idorigen, b.nombre_origen, a.idmunicipio, a.idtienda, c.nombre_foco, a.tipo, a.area_responsable, (SELECT COUNT(*) FROM solicitudpqrs_imagenes d WHERE  d.idsolicitudPQRS = a.idsolicitudPQRS) AS imagenes  from solicitudPQRS a, origen_pqrs b, foco_pqrs c where a.idorigen = b.idorigen and a.idfoco = c.idfoco and fechasolicitud >=  '" + fechaini +"' and fechasolicitud <= '"+ fechafin +"' and idtienda = " + tienda;
			}
			if(!tipoSolicitud.equals(new String("")))
			{
				consulta = consulta + " and a.tiposolicitud = '" + tipoSolicitud + "'";
			}
		}
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idsolicitudpqrs = 0;
			String fechasolicitud = "";
			String tiposolicitud = "";
			String nombres = "";
			String apellidos = "";
			String direccion = "";
			String telefono = "";
			String comentario = "";
			int idOrigen = 0;
			String origen = "";
			int idMunicipio = 0;
			int idTienda = 0;
			String nombreFoco = "";
			String tipo = "";
			String areaResponsable = "";
			int imagenes = 0;
			while(rs.next())
			{
				idsolicitudpqrs = rs.getInt("idsolicitudPQRS");
				fechasolicitud = rs.getString("fechasolicitud");
				tiposolicitud = rs.getString("tiposolicitud");
				nombres = rs.getString("nombres");
				apellidos = rs.getString("apellidos");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				comentario = rs.getString("comentario");
				idOrigen = rs.getInt("idorigen");
				origen = rs.getString("nombre_origen");
				idMunicipio = rs.getInt("idmunicipio");
				idTienda = rs.getInt("idtienda");
				nombreFoco = rs.getString("nombre_foco");
				tipo = rs.getString("tipo");
				areaResponsable = rs.getString("area_responsable");
				imagenes = rs.getInt("imagenes");
				SolicitudPQRS cadaSolicitud = new SolicitudPQRS(idsolicitudpqrs, fechasolicitud, tiposolicitud, 0, 0,
						nombres, apellidos, telefono, direccion, "", 0,
						comentario, idOrigen,0, tipo, areaResponsable);
				cadaSolicitud.setOrigen(origen);
				cadaSolicitud.setIdmunicipio(idMunicipio);
				cadaSolicitud.setIdtienda(idTienda);
				cadaSolicitud.setFoco(nombreFoco);
				cadaSolicitud.setImagenes(imagenes);
				consultaSolicitudes.add(cadaSolicitud);
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
		return(consultaSolicitudes);
	}
	
	
	/**
	 * M�todo que se encarga de validar si una PQRS existe o no, retornando esto como un valor booleano
	 * @param PQRS
	 * @param idCliente
	 * @return
	 */
	public static boolean validarPQRS(int PQRS, int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		String consulta = "select * from solicitudPQRS where idCliente = " + idCliente + " and idsolicitudPQRS = " + PQRS;
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			
			while(rs.next())
			{
				respuesta = true;
				break;
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
		return(respuesta);
	}
	
	public static ArrayList<Integer> obtenerPQRSCliente(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		String consulta = "select idsolicitudPQRS from solicitudPQRS where idCliente = " + idCliente;
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<Integer> pqrsCliente = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int pqrs = 0;
			while(rs.next())
			{
				pqrs = rs.getInt("idsolicitudPQRS");
				pqrsCliente.add(Integer.valueOf(pqrs));
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
		return(pqrsCliente);
	}

}

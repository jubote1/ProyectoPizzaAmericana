package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Oferta;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que implementa todos los métodos de acceso a la base de datos para la administración de la entidad Excepcion de Precio.
 * @author JuanDavid
 *
 */
public class OfertaDAO {
	
	/**
	 * Método que se encarga de obtener todas la excepciones de precio parametrizadas en base de datos
	 * @return Retorna un ArrayList con objetos de Modelo Oferta.
	 */
	public static ArrayList<Oferta> obtenerOfertas()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Oferta> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from oferta";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOferta;
			String nombreOferta;
			int idExcepcion;
			Oferta ofertaTemp = new Oferta(0,"",0);
			while(rs.next()){
				idOferta = rs.getInt("idoferta");
				nombreOferta = rs.getString("nombre_oferta");
				idExcepcion = rs.getInt("idexcepcion");
				ofertaTemp = new Oferta(idOferta,nombreOferta, idExcepcion);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
	public static ArrayList<Oferta> obtenerOfertasGrid()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Oferta> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idoferta, a.nombre_oferta, a.idexcepcion, b.descripcion from oferta a left outer join excepcion_precio b on a.idexcepcion = b.idexcepcion ";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOferta;
			String nombreOferta;
			int idExcepcion;
			String nombreExcepcion = "";
			Oferta ofertaTemp = new Oferta(0,"",0);
			while(rs.next()){
				idOferta = rs.getInt("idoferta");
				nombreOferta = rs.getString("nombre_oferta");
				idExcepcion = rs.getInt("idexcepcion");
				nombreExcepcion = rs.getString("descripcion");
				ofertaTemp = new Oferta(idOferta,nombreOferta, idExcepcion);
				ofertaTemp.setNombreExcepcion(nombreExcepcion);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
	
	public static ArrayList<Oferta> obtenerOfertasGridContact()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Oferta> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idoferta, a.nombre_oferta, a.idexcepcion, b.descripcion from oferta a left outer join excepcion_precio b on a.idexcepcion = b.idexcepcion where a.contact = 'S' ";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOferta;
			String nombreOferta;
			int idExcepcion;
			String nombreExcepcion = "";
			Oferta ofertaTemp = new Oferta(0,"",0);
			while(rs.next()){
				idOferta = rs.getInt("idoferta");
				nombreOferta = rs.getString("nombre_oferta");
				idExcepcion = rs.getInt("idexcepcion");
				nombreExcepcion = rs.getString("descripcion");
				ofertaTemp = new Oferta(idOferta,nombreOferta, idExcepcion);
				ofertaTemp.setNombreExcepcion(nombreExcepcion);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
		
	/**
	 * Método que se encarga de realizar la inserción de una ferta con base en la información recibida como 
	 * parámetro.
	 * @param Exc Recibe como parámetro un objeto de Modelo EOferta con base en el cual se realiza la inserción
	 * de la información.
	 * @return Se retorna un número entero con el idoferta retornado en la inserción a la base de datos.
	 */
	public static int insertarOferta(Oferta ofer)
	{
		Logger logger = Logger.getLogger("log_file");
		int idOfertaIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into oferta (nombre_oferta,idexcepcion) values ('" + ofer.getNombreOferta() + "' ," + ofer.getIdExcepcion() +")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idOfertaIns = rs.getInt(1);
				logger.info("idOferta insertado es " + idOfertaIns);
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
		return(idOfertaIns);
	}

	/**
	 * Método qeu se encarga de eliminar una oferta de precio con base en la información enviadad como parámetro.
	 * @param idoferta Recibe como parámetro el idexcepcion que desea ser eliminado.
	 */
	public static void eliminarOferta(int idOferta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from oferta  where idoferta = " + idOferta; 
			logger.info(delete);
			stm.executeUpdate(delete);
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

	/**
	 * Método que se encarga de consultar una oferta con base en el parámetro recibido.
	 * @param idOferta Se recibe como parámetro el idexcepcion que desea ser consultado.
	 * @return Se retorna un objeto Modelo Oferta que contiene la información el excepcion Precio consultada.
	 */
	public static Oferta retornarOferta(int idOferta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Oferta ofertaTemp = new Oferta(0,"",0);
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from  oferta  where idoferta = " + idOferta; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String nombreOferta = "";
			int idExcepcion = 0;
			int diasCaducidad = 0;
			String tipoCaducidad = "";
			double descuentoFijoPorcentaje = 0, descuentoFijoValor = 0;
			String codigoPromocional = "";
			String redParcial = "";
			while(rs.next()){
				nombreOferta = rs.getString("nombre_oferta");
				idExcepcion = rs.getInt("idexcepcion");
				descuentoFijoPorcentaje = rs.getDouble("descuento_fijo_porcentaje");
				descuentoFijoValor = rs.getDouble("descuento_fijo_valor");
				try {
					diasCaducidad = rs.getInt("dias_caducidad");
					
				}catch(Exception e)
				{
					diasCaducidad = 0;
				}
				tipoCaducidad = rs.getString("tipo_caducidad");
				codigoPromocional = rs.getString("codigo_promocional");
				redParcial = rs.getString("red_parcial");
				break;
			}
			ofertaTemp = new Oferta(idOferta, nombreOferta, idExcepcion);
			ofertaTemp.setDiasCaducidad(diasCaducidad);
			ofertaTemp.setTipoCaducidad(tipoCaducidad);
			ofertaTemp.setDescuentoFijoPorcentaje(descuentoFijoPorcentaje);
			ofertaTemp.setDescuentoFijoValor(descuentoFijoValor);
			ofertaTemp.setCodigoPromocional(codigoPromocional);
			ofertaTemp.setRedParcial(redParcial);
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
		return(ofertaTemp);
	}
	
	
	/**
	 * Método que retorna las condiciones de horario de una oferta en caso de que las tenga definidas para validar la vigencia de una oferta
	 * @param idOferta
	 * @return
	 */
	public static Oferta retornarOfertaInfoHora(int idOferta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Oferta ofertaTemp = new Oferta(0,"",0);
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select controla_hora, hora_inicio, hora_fin, red_parcial from oferta  where idoferta = " + idOferta; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String controlaHora = "";
			String horaInicio = "";
			String horaFin = "";
			String redParcial = "";
			while(rs.next()){
				controlaHora = rs.getString("controla_hora");
				horaInicio = rs.getString("hora_inicio");
				horaFin = rs.getString("hora_fin");
				redParcial = rs.getString("red_parcial");
				break;
			}
			ofertaTemp = new Oferta (idOferta, controlaHora, horaInicio, horaFin);
			ofertaTemp.setRedParcial(redParcial);
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
		return(ofertaTemp);
	}

	/**
	 * Método que permite editar una excepción Precio con base en la información enviada como parámetro.
	 * @param Esc Recibe como parámetro un objeto Modelo ExcepcionPrecio con base en el cual se realiza la edición.
	 * @return Retorna un string con el resultado del proceso de edición.
	 */
	public static String editarOferta(Oferta ofertaEdi)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String update = "update oferta set nombre_oferta = '"+ ofertaEdi.getNombreOferta() +"' , idexcepcion =" + ofertaEdi.getIdExcepcion() + "  where idoferta = " + ofertaEdi.getIdOferta(); 
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
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
			resultado = "error";
		}
		return(resultado);
	}
	
	public static boolean manejaCodigoOferta(int idOferta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select codigo_promocional from  oferta  where idoferta = " + idOferta; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String codigoPromocional = "";
			while(rs.next()){
				codigoPromocional = rs.getString("codigo_promocional");
				if(codigoPromocional.equals(new String("S")))
				{
					respuesta = true;
				}else
				{
					respuesta = false;
				}
				break;
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
		}
		return(respuesta);
	}
	
	public static Oferta obtenerOfertaCodigoPromocional(String codigoGeneral)
	{
		Logger logger = Logger.getLogger("log_file");
		Oferta oferta = new Oferta(0,"",0);;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from oferta where codigo_general = '" + codigoGeneral + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOferta;
			String nombreOferta;
			int idExcepcion;
			while(rs.next()){
				idOferta = rs.getInt("idoferta");
				nombreOferta = rs.getString("nombre_oferta");
				idExcepcion = rs.getInt("idexcepcion");
				oferta = new Oferta(idOferta,nombreOferta, idExcepcion);
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
		return(oferta);
		
	}


}

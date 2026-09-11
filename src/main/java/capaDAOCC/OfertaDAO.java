package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Oferta;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que implementa todos los m�todos de acceso a la base de datos para la administraci�n de la entidad Excepcion de Precio.
 * @author JuanDavid
 *
 */
public class OfertaDAO {
	
	/**
	 * M�todo que se encarga de obtener todas la excepciones de precio parametrizadas en base de datos
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
			String consulta = "select * from oferta where habilitado = 'S'";
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
			String consulta = "select a.idoferta, a.nombre_oferta, a.idexcepcion, b.descripcion from oferta a left outer join excepcion_precio b on a.idexcepcion = b.idexcepcion where a.habilitado = 'S' ";
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
			String consulta = "select a.idoferta, a.nombre_oferta, a.idexcepcion, b.descripcion from oferta a left outer join excepcion_precio b on a.idexcepcion = b.idexcepcion where a.contact = 'S' and a.habilitado = 'S'";
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
	
		
	/** Las 22 columnas que se pueden definir desde la pantalla, sin el idoferta. */
	private static final String COLUMNAS_OFERTA =
			"nombre_oferta, idexcepcion, codigo_promocional, descuento_fijo_porcentaje, "
			+ "descuento_porcentaje_futuro, descuento_fijo_valor, mensaje1, mensaje2, "
			+ "dias_caducidad, tipo_caducidad, controla_hora, hora_inicio, hora_fin, "
			+ "tipo_oferta, fecha_desde, fecha_hasta, codigo_general, contact, "
			+ "red_parcial, reintegro, habilitado";

	/**
	 * Pone en el PreparedStatement los 21 valores de la oferta, en el orden de
	 * COLUMNAS_OFERTA. Lo usan el insert y el update, para que no se puedan
	 * desincronizar.
	 *
	 * Las fechas de vigencia van como NULL cuando vienen vacias: una cadena vacia
	 * en una columna date la guarda como 0000-00-00 y despues no hay forma de
	 * distinguir "sin vigencia" de "vigente desde el ano cero".
	 *
	 * @return el siguiente indice libre, para que el update ponga ahi el idoferta
	 */
	private static int ponerValoresOferta(PreparedStatement pstmt, Oferta ofer) throws SQLException {
		int i = 1;
		pstmt.setString(i++, OfertaDAO.texto(ofer.getNombreOferta()));
		pstmt.setInt(i++, ofer.getIdExcepcion());
		pstmt.setString(i++, OfertaDAO.siNo(ofer.getCodigoPromocional()));
		pstmt.setDouble(i++, ofer.getDescuentoFijoPorcentaje());
		pstmt.setDouble(i++, ofer.getDescuentoPorcentajeFuturo());
		pstmt.setDouble(i++, ofer.getDescuentoFijoValor());
		pstmt.setString(i++, OfertaDAO.texto(ofer.getMensaje1()));
		pstmt.setString(i++, OfertaDAO.texto(ofer.getMensaje2()));
		pstmt.setInt(i++, ofer.getDiasCaducidad());
		pstmt.setString(i++, OfertaDAO.conDefecto(ofer.getTipoCaducidad(), "P"));
		pstmt.setString(i++, OfertaDAO.siNo(ofer.getControlaHora()));
		pstmt.setString(i++, OfertaDAO.texto(ofer.getHoraInicio()));
		pstmt.setString(i++, OfertaDAO.texto(ofer.getHoraFin()));
		pstmt.setString(i++, OfertaDAO.conDefecto(ofer.getTipoOferta(), "C"));
		OfertaDAO.ponerFecha(pstmt, i++, ofer.getFechaDesde());
		OfertaDAO.ponerFecha(pstmt, i++, ofer.getFechaHasta());
		pstmt.setString(i++, OfertaDAO.texto(ofer.getCodigoGeneral()));
		pstmt.setString(i++, OfertaDAO.siNo(ofer.getContact()));
		pstmt.setString(i++, OfertaDAO.siNo(ofer.getRedParcial()));
		pstmt.setString(i++, OfertaDAO.siNo(ofer.getReintegro()));
		pstmt.setString(i++, OfertaDAO.conDefecto(ofer.getHabilitado(), "S"));
		return (i);
	}

	/** Cadena vacia en vez de nulo. */
	private static String texto(String valor) {
		return ((valor == null) ? "" : valor.trim());
	}

	/** Las banderas de la tabla son S o N, nunca vacio ni nulo. */
	private static String siNo(String valor) {
		return ("S".equalsIgnoreCase(OfertaDAO.texto(valor)) ? "S" : "N");
	}

	/** Un valor de un solo caracter con su valor por defecto si viene vacio. */
	private static String conDefecto(String valor, String porDefecto) {
		String limpio = OfertaDAO.texto(valor);
		return ((limpio.length() == 0) ? porDefecto : limpio.substring(0, 1).toUpperCase());
	}

	/**
	 * Una fecha de vigencia, o NULL si viene vacia o mal escrita.
	 *
	 * Nunca se guarda cadena vacia: MySQL la convierte en 0000-00-00 y despues no
	 * se puede distinguir de una vigencia de verdad.
	 */
	private static void ponerFecha(PreparedStatement pstmt, int indice, String fecha) throws SQLException {
		String limpia = OfertaDAO.texto(fecha);
		if (limpia.length() < 10) {
			pstmt.setNull(indice, java.sql.Types.DATE);
			return;
		}
		try {
			pstmt.setDate(indice, java.sql.Date.valueOf(limpia.substring(0, 10)));
		} catch (Exception e) {
			pstmt.setNull(indice, java.sql.Types.DATE);
		}
	}

	/**
	 * Crea la oferta con TODA su definicion.
	 *
	 * Antes esto escribia dos columnas -el nombre y la excepcion de precio- y las
	 * otras veintiuna tocaba ponerlas a mano en la base de datos. Ahora entran
	 * todas desde la pantalla.
	 *
	 * Se pasa a PreparedStatement y no por gusto: el nombre de la oferta se
	 * concatenaba dentro del SQL, asi que un nombre con apostrofe -"Promo del mes
	 * de mama's"- rompia la consulta, y cualquier cosa peor tambien entraba.
	 *
	 * @return el idoferta creado, o 0 si fallo
	 */
	public static int insertarOferta(Oferta ofer) {
		Logger logger = Logger.getLogger("log_file");
		int idGenerado = 0;
		String insert = "INSERT INTO oferta (" + OfertaDAO.COLUMNAS_OFERTA + ") "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDPrincipal();
				PreparedStatement pstmt = con1.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
			OfertaDAO.ponerValoresOferta(pstmt, ofer);
			pstmt.executeUpdate();
			try (ResultSet rs = pstmt.getGeneratedKeys()) {
				if (rs.next()) {
					idGenerado = rs.getInt(1);
				}
			}
		} catch (Exception e) {
			logger.error("insertarOferta: " + e.toString());
			System.out.println("insertarOferta: " + e.toString());
		}
		return (idGenerado);
	}

	/**
	 * Actualiza TODA la definicion de la oferta.
	 *
	 * Misma historia que el insert: antes solo cambiaba el nombre y la excepcion.
	 *
	 * @return "exitoso" o el motivo del fallo
	 */
	public static String editarOferta(Oferta ofertaEdi) {
		Logger logger = Logger.getLogger("log_file");
		StringBuilder sets = new StringBuilder();
		String[] columnas = OfertaDAO.COLUMNAS_OFERTA.split(",");
		for (int i = 0; i < columnas.length; i++) {
			if (i > 0) {
				sets.append(", ");
			}
			sets.append(columnas[i].trim()).append(" = ?");
		}
		String update = "UPDATE oferta SET " + sets.toString() + " WHERE idoferta = ?";
		ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDPrincipal();
				PreparedStatement pstmt = con1.prepareStatement(update)) {
			int siguiente = OfertaDAO.ponerValoresOferta(pstmt, ofertaEdi);
			pstmt.setInt(siguiente, ofertaEdi.getIdOferta());
			pstmt.executeUpdate();
			return ("exitoso");
		} catch (Exception e) {
			logger.error("editarOferta " + ofertaEdi.getIdOferta() + ": " + e.toString());
			System.out.println("editarOferta " + ofertaEdi.getIdOferta() + ": " + e.toString());
			return ("No se pudo guardar la oferta: " + e.toString());
		}
	}

	/**
	 * M�todo qeu se encarga de eliminar una oferta de precio con base en la informaci�n enviadad como par�metro.
	 * @param idoferta Recibe como par�metro el idexcepcion que desea ser eliminado.
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
	 * M�todo que se encarga de consultar una oferta con base en el par�metro recibido.
	 * @param idOferta Se recibe como par�metro el idexcepcion que desea ser consultado.
	 * @return Se retorna un objeto Modelo Oferta que contiene la informaci�n el excepcion Precio consultada.
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
			double descuentoFijoPorcentaje = 0, descuentoFijoValor = 0, descuentoPorcentajeFuturo = 0; 
			String codigoPromocional = "";
			String redParcial = "";
			//El resto de la definicion, que antes no se leia: la pantalla de ofertas
			//la necesita completa para poder editarla sin perder nada.
			String mensaje1 = "", mensaje2 = "", controlaHora = "N", horaInicio = "", horaFin = "";
			String tipoOferta = "C", fechaDesde = "", fechaHasta = "", codigoGeneral = "";
			String contact = "N", reintegro = "N", habilitado = "S";
			while(rs.next()){
				nombreOferta = rs.getString("nombre_oferta");
				idExcepcion = rs.getInt("idexcepcion");
				descuentoFijoPorcentaje = rs.getDouble("descuento_fijo_porcentaje");
				descuentoFijoValor = rs.getDouble("descuento_fijo_valor");
				descuentoPorcentajeFuturo = rs.getDouble("descuento_porcentaje_futuro");
				try {
					diasCaducidad = rs.getInt("dias_caducidad");
					
				}catch(Exception e)
				{
					diasCaducidad = 0;
				}
				tipoCaducidad = rs.getString("tipo_caducidad");
				codigoPromocional = rs.getString("codigo_promocional");
				redParcial = rs.getString("red_parcial");
				mensaje1 = OfertaDAO.texto(rs.getString("mensaje1"));
				mensaje2 = OfertaDAO.texto(rs.getString("mensaje2"));
				controlaHora = OfertaDAO.texto(rs.getString("controla_hora"));
				horaInicio = OfertaDAO.texto(rs.getString("hora_inicio"));
				horaFin = OfertaDAO.texto(rs.getString("hora_fin"));
				tipoOferta = OfertaDAO.texto(rs.getString("tipo_oferta"));
				fechaDesde = OfertaDAO.texto(rs.getString("fecha_desde"));
				fechaHasta = OfertaDAO.texto(rs.getString("fecha_hasta"));
				codigoGeneral = OfertaDAO.texto(rs.getString("codigo_general"));
				contact = OfertaDAO.texto(rs.getString("contact"));
				reintegro = OfertaDAO.texto(rs.getString("reintegro"));
				habilitado = OfertaDAO.texto(rs.getString("habilitado"));
				break;
			}
			ofertaTemp = new Oferta(idOferta, nombreOferta, idExcepcion);
			ofertaTemp.setDiasCaducidad(diasCaducidad);
			ofertaTemp.setTipoCaducidad(tipoCaducidad);
			ofertaTemp.setDescuentoFijoPorcentaje(descuentoFijoPorcentaje);
			ofertaTemp.setDescuentoFijoValor(descuentoFijoValor);
			ofertaTemp.setCodigoPromocional(codigoPromocional);
			ofertaTemp.setRedParcial(redParcial);
			ofertaTemp.setDescuentoPorcentajeFuturo(descuentoPorcentajeFuturo);
			ofertaTemp.setMensaje1(mensaje1);
			ofertaTemp.setMensaje2(mensaje2);
			ofertaTemp.setControlaHora(controlaHora);
			ofertaTemp.setHoraInicio(horaInicio);
			ofertaTemp.setHoraFin(horaFin);
			ofertaTemp.setTipoOferta(tipoOferta);
			ofertaTemp.setFechaDesde(fechaDesde);
			ofertaTemp.setFechaHasta(fechaHasta);
			ofertaTemp.setCodigoGeneral(codigoGeneral);
			ofertaTemp.setContact(contact);
			ofertaTemp.setReintegro(reintegro);
			ofertaTemp.setHabilitado(habilitado);
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
	 * M�todo que retorna las condiciones de horario de una oferta en caso de que las tenga definidas para validar la vigencia de una oferta
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



	/**
	 * Todas las ofertas con su definicion completa, para la pantalla que las
	 * administra.
	 *
	 * Es distinto de obtenerOfertasGrid a proposito y NO lo reemplaza: aquel
	 * filtra habilitado = 'S' porque alimenta el desplegable desde el que se le
	 * asigna una oferta a un cliente, y ahi solo deben salir las vigentes. Aqui
	 * salen las 41, porque para volver a habilitar una hay que poder verla.
	 *
	 * Trae tambien cuantas veces se ha asignado y cuantas se ha usado, que es lo
	 * que dice si la oferta esta funcionando o no.
	 */
	public static ArrayList<Oferta> obtenerOfertasAdministracion() {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Oferta> ofertas = new ArrayList<Oferta>();
		String consulta = "SELECT a.*, b.descripcion, "
				+ "  (SELECT COUNT(*) FROM oferta_cliente oc WHERE oc.idoferta = a.idoferta) AS asignadas, "
				+ "  (SELECT COUNT(*) FROM oferta_cliente oc WHERE oc.idoferta = a.idoferta "
				+ "     AND oc.utilizada = 'S') AS usadas "
				+ " FROM oferta a "
				+ " LEFT OUTER JOIN excepcion_precio b ON a.idexcepcion = b.idexcepcion "
				+ " ORDER BY a.habilitado DESC, a.idoferta DESC";
		ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDPrincipal();
				Statement stm = con1.createStatement();
				ResultSet rs = stm.executeQuery(consulta)) {
			while (rs.next()) {
				Oferta ofer = new Oferta(rs.getInt("idoferta"),
						OfertaDAO.texto(rs.getString("nombre_oferta")), rs.getInt("idexcepcion"));
				ofer.setNombreExcepcion(OfertaDAO.texto(rs.getString("descripcion")));
				ofer.setCodigoPromocional(OfertaDAO.texto(rs.getString("codigo_promocional")));
				ofer.setDescuentoFijoPorcentaje(rs.getDouble("descuento_fijo_porcentaje"));
				ofer.setDescuentoPorcentajeFuturo(rs.getDouble("descuento_porcentaje_futuro"));
				ofer.setDescuentoFijoValor(rs.getDouble("descuento_fijo_valor"));
				ofer.setMensaje1(OfertaDAO.texto(rs.getString("mensaje1")));
				ofer.setMensaje2(OfertaDAO.texto(rs.getString("mensaje2")));
				ofer.setDiasCaducidad(rs.getInt("dias_caducidad"));
				ofer.setTipoCaducidad(OfertaDAO.texto(rs.getString("tipo_caducidad")));
				ofer.setControlaHora(OfertaDAO.texto(rs.getString("controla_hora")));
				ofer.setHoraInicio(OfertaDAO.texto(rs.getString("hora_inicio")));
				ofer.setHoraFin(OfertaDAO.texto(rs.getString("hora_fin")));
				ofer.setTipoOferta(OfertaDAO.texto(rs.getString("tipo_oferta")));
				ofer.setFechaDesde(OfertaDAO.texto(rs.getString("fecha_desde")));
				ofer.setFechaHasta(OfertaDAO.texto(rs.getString("fecha_hasta")));
				ofer.setCodigoGeneral(OfertaDAO.texto(rs.getString("codigo_general")));
				ofer.setContact(OfertaDAO.texto(rs.getString("contact")));
				ofer.setRedParcial(OfertaDAO.texto(rs.getString("red_parcial")));
				ofer.setReintegro(OfertaDAO.texto(rs.getString("reintegro")));
				ofer.setHabilitado(OfertaDAO.texto(rs.getString("habilitado")));
				ofer.setAsignadas(rs.getInt("asignadas"));
				ofer.setUsadas(rs.getInt("usadas"));
				ofertas.add(ofer);
			}
		} catch (Exception e) {
			logger.error("obtenerOfertasAdministracion: " + e.toString());
			System.out.println("obtenerOfertasAdministracion: " + e.toString());
		}
		return (ofertas);
	}
}

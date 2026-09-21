package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.SolicitudConciliacion;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class SolicitudConciliacionDAO {

	/**
	 * Método que se encarga de insertar la solicitud de conciliación de QR o Datáfono
	 * @param solicitud
	 * @return
	 */
	public static boolean insertarSolicitudConciliacion(SolicitudConciliacion solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitud_conciliacion (fecha,origen,descripcion,idtienda,categoria,valor_analizar,telefono,idpedidotienda) values ('"+ solicitud.getFecha() + "','" + solicitud.getOrigen() + "' , '" + solicitud.getDescripcion() + "' , " + solicitud.getIdTienda() + " , '" + solicitud.getCategoria() + "' , " + solicitud.getValorAnalizar() + " , '" + solicitud.getTelefono() +"' , " + solicitud.getIdPedidoTienda() + ")";
			logger.info(insert);
			stm.executeUpdate(insert);
			respuesta = true;
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
		return(respuesta);
	}
	
	/**
	 * Método que trae la cantidad de solicitudes de conciliación pendientes en los últimos 7 días.
	 * @param idTienda
	 * @return
	 */
	public static int existeSolicitudConciliacion(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int respuesta = 0;
		Calendar calendario = Calendar.getInstance();
		calendario.add(Calendar.DAY_OF_YEAR, -7);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String fecha = dateFormat.format(calendario.getTime());
		try
		{
			Statement stm = con1.createStatement();
			String select = "select count(*) from solicitud_conciliacion where idtienda = " + idTienda + " and fecha >= '" + fecha +"' and estado = 'PENDIENTE'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				respuesta = rs.getInt(1);
			}
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
		return(respuesta);
	}
	
	
	/**
	 * Método que nos retornará en un json un booleano qeu nos indicará si se puede o no cerrar el sistema dado que hay que hay partidas
	 * pendientes de validación
	 * @param idTienda
	 * @return
	 */
	public static boolean validarCierreSolicitudConciliacion(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = true;
		Calendar calendario = Calendar.getInstance();
		calendario.add(Calendar.DAY_OF_YEAR, -7);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String fecha = dateFormat.format(calendario.getTime());
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitud_conciliacion where idtienda = " + idTienda + " and fecha < '" + fecha +"' and estado = 'PENDIENTE'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				respuesta  = false;
			}
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
		return(respuesta);
	}
	
	
	/**
	 * Método que dado una tienda y una fecha de consulta, retorna las solicitudes de conciliación 
	 * @param idTienda
	 * @param fechaConsulta
	 * @return
	 */
	public static ArrayList<SolicitudConciliacion> consultarSolicitudConciliacion(int idTienda, String fechaConsulta)
	{
		ArrayList<SolicitudConciliacion> solicitudes = new ArrayList();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitud_conciliacion where idtienda = " + idTienda + " and fecha >= '" + fechaConsulta +"'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			SolicitudConciliacion solTemp;
			int idSolicitud;
			String fecha;
			String origen;
			String descripcion;
			String categoria;
			double valorAnalizar;
			String estado;
			double valorFinal;
			String telefono;
			int idPedidoTienda;
			while(rs.next())
			{
				idSolicitud = rs.getInt("idsolicitud");
				fecha = rs.getString("fecha");
				origen = rs.getString("origen");
				descripcion = rs.getString("descripcion");
				categoria = rs.getString("categoria");
				valorAnalizar = rs.getDouble("valor_analizar");
				estado = rs.getString("estado");
				valorFinal = rs.getDouble("valor_final");
				telefono = rs.getString("telefono");
				idPedidoTienda = rs.getInt("idpedidotienda");
				solTemp = new SolicitudConciliacion(idSolicitud, fecha, origen, descripcion, idTienda,
						categoria, valorAnalizar, estado, valorFinal, telefono,
						idPedidoTienda);
				solicitudes.add(solTemp);
			}
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
		return(solicitudes);
	}

	

	/**
	 * Las solicitudes de conciliacion que la pantalla muestra.
	 *
	 * Reemplaza a consultarSolicitudConciliacion, que tenia dos problemas:
	 *
	 * 1. Armaba "where idtienda = N" sin caso para TODAS. La pantalla mandaba
	 *    idtienda=TODAS, el servlet no lo podia convertir y lo dejaba en 0, asi
	 *    que la consulta quedaba "idtienda = 0" y no devolvia NADA. Escoger
	 *    TODAS era garantia de pantalla vacia.
	 * 2. Solo aceptaba fecha desde, sin tope ni filtro de estado, asi que para
	 *    encontrar las pendientes habia que buscarlas a ojo entre las 363.
	 *
	 * Ahora idTienda 0 significa todas, hay rango de fechas y filtro de estado,
	 * y se traen el nombre de la tienda y los dias que lleva la diferencia.
	 *
	 * @param idTienda   0 = todas las tiendas
	 * @param fechaDesde en yyyy-MM-dd
	 * @param fechaHasta en yyyy-MM-dd, o vacio para no poner tope
	 * @param estado     PENDIENTE, PROCESADO, o vacio para todos
	 */
	public static ArrayList<SolicitudConciliacion> consultarSolicitudes(final int idTienda,
			final String fechaDesde, final String fechaHasta, final String estado) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<SolicitudConciliacion> solicitudes = new ArrayList<SolicitudConciliacion>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT s.idsolicitud, s.fecha, s.origen, s.descripcion, s.idtienda,")
					.append(" IFNULL(t.nombre,'') AS nombretienda, s.categoria, s.valor_analizar,")
					.append(" s.estado, s.valor_final, s.telefono, s.idpedidotienda,")
					.append(" DATEDIFF(CURDATE(), s.fecha) AS dias,")
					.append(" IFNULL(s.usuario_procesa,'') AS usuario_procesa,")
					.append(" IFNULL(DATE_FORMAT(s.fecha_procesa,'%Y-%m-%d %H:%i'),'') AS fecha_procesa,")
					.append(" IFNULL(s.observacion_cierre,'') AS observacion_cierre")
					.append(" FROM solicitud_conciliacion s")
					.append(" LEFT JOIN tienda t ON t.idtienda = s.idtienda")
					.append(" WHERE 1 = 1");
			if (fechaDesde != null && fechaDesde.trim().length() > 0) {
				sql.append(" AND s.fecha >= ?");
			}
			if (fechaHasta != null && fechaHasta.trim().length() > 0) {
				sql.append(" AND s.fecha <= ?");
			}
			if (idTienda > 0) {
				sql.append(" AND s.idtienda = ?");
			}
			if (estado != null && estado.trim().length() > 0) {
				sql.append(" AND s.estado = ?");
			}
			//Lo mas viejo primero: es lo que hay que resolver de una.
			sql.append(" ORDER BY s.fecha ASC, s.idsolicitud ASC");

			final PreparedStatement ps = con1.prepareStatement(sql.toString());
			int i = 1;
			if (fechaDesde != null && fechaDesde.trim().length() > 0) {
				ps.setString(i++, fechaDesde);
			}
			if (fechaHasta != null && fechaHasta.trim().length() > 0) {
				ps.setString(i++, fechaHasta);
			}
			if (idTienda > 0) {
				ps.setInt(i++, idTienda);
			}
			if (estado != null && estado.trim().length() > 0) {
				ps.setString(i++, estado);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final SolicitudConciliacion s = new SolicitudConciliacion();
				s.setIdSolicitud(rs.getInt("idsolicitud"));
				s.setFecha(rs.getString("fecha"));
				s.setOrigen(rs.getString("origen"));
				s.setDescripcion(rs.getString("descripcion"));
				s.setIdTienda(rs.getInt("idtienda"));
				s.setNombreTienda(rs.getString("nombretienda"));
				s.setCategoria(rs.getString("categoria"));
				s.setValorAnalizar(rs.getDouble("valor_analizar"));
				s.setEstado(rs.getString("estado"));
				s.setValorFinal(rs.getDouble("valor_final"));
				s.setTelefono(rs.getString("telefono"));
				s.setIdPedidoTienda(rs.getInt("idpedidotienda"));
				s.setDias(rs.getInt("dias"));
				s.setUsuarioProcesa(rs.getString("usuario_procesa"));
				s.setFechaProcesa(rs.getString("fecha_procesa"));
				s.setObservacionCierre(rs.getString("observacion_cierre"));
				solicitudes.add(s);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("consultarSolicitudes: " + e.toString());
			try {
				con1.close();
			} catch (final Exception e1) {
			}
		}
		return (solicitudes);
	}

	/**
	 * Guarda la resolucion de una diferencia.
	 *
	 * No existia: la pantalla era de solo lectura y el estado se cambiaba a mano
	 * en la base. Al 2026-09-15 habia 122 diferencias pendientes por $5.852.232,
	 * la mas vieja de hace 422 dias.
	 *
	 * Queda registrado QUIEN la cerro y cuando. Una diferencia de plata que se
	 * cierra sin dejar rastro no se puede auditar despues.
	 *
	 * @param estado PENDIENTE o PROCESADO
	 * @return true si se actualizo una fila
	 */
	public static boolean actualizarSolicitud(final int idSolicitud, final double valorFinal,
			final String estado, final String observacionCierre, final String usuario) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		boolean actualizo = false;
		try {
			//La fecha y el usuario solo se sellan cuando se procesa. Si se devuelve
			//a PENDIENTE se limpian, para que no quede diciendo que alguien la cerro.
			final String sql = "UPDATE solicitud_conciliacion"
					+ " SET valor_final = ?, estado = ?, observacion_cierre = ?,"
					+ "     usuario_procesa = IF(? = 'PROCESADO', ?, NULL),"
					+ "     fecha_procesa   = IF(? = 'PROCESADO', NOW(), NULL)"
					+ " WHERE idsolicitud = ?";
			final PreparedStatement ps = con1.prepareStatement(sql);
			ps.setDouble(1, valorFinal);
			ps.setString(2, estado);
			ps.setString(3, observacionCierre);
			ps.setString(4, estado);
			ps.setString(5, usuario);
			ps.setString(6, estado);
			ps.setInt(7, idSolicitud);
			actualizo = (ps.executeUpdate() > 0);
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("actualizarSolicitud: " + e.toString());
			try {
				con1.close();
			} catch (final Exception e1) {
			}
		}
		return (actualizo);
	}
}

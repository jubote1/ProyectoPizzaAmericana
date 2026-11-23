package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.TiendaBloqueada;
import conexionCC.ConexionBaseDatos;

/**
 * Clase que se encarga de implementar todo lo relacionado con la base de datos de la entidad tienda Bloqueada.
 * Soporta bloqueo general ('TODOS') y segmentado por canal ('TIENDA_VIRTUAL', 'BOT', 'CALL_CENTER', 'POS').
 * @author JuanDavid / Modificado: Soporte Multi-Canal
 */
public class TiendaBloqueadaDAO {
	
	/**
	 * Método que se encarga de retornar todas las entidades Tiendas Bloqueadas definidas en la base de datos
	 * @return Se retorna un ArrayList con todas las entidades Tiendas Bloqueadas definidas en la base de datos.
	 */
	public static ArrayList<TiendaBloqueada> retornarTiendasBloqueadas()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<TiendaBloqueada> tiendas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Intentamos primero con la columna canal, si no existe caemos al fallback sin canal
			String consulta = "select a.idtienda, a.canal, b.nombre, a.comentario, a.fecha_bloqueo from tienda_bloqueada a, tienda b where a.idtienda = b.idtienda order by a.fecha_bloqueo desc";
			ResultSet rs;
			try {
				rs = stm.executeQuery(consulta);
			} catch (Exception exCol) {
				// Fallback si la columna canal aún no ha sido agregada por migración SQL
				consulta = "select a.idtienda, 'TODOS' as canal, b.nombre, a.comentario, a.fecha_bloqueo from tienda_bloqueada a, tienda b where a.idtienda = b.idtienda order by a.fecha_bloqueo desc";
				rs = stm.executeQuery(consulta);
			}
			logger.info(consulta);
			while(rs.next()){
				int idTienda = rs.getInt("idtienda");
				String canal = "TODOS";
				try {
					canal = rs.getString("canal");
					if (canal == null || canal.trim().isEmpty()) {
						canal = "TODOS";
					}
				} catch (Exception eCanal) {
					canal = "TODOS";
				}
				String nombre = rs.getString("nombre");
				String comentario = rs.getString("comentario");
				String fecha_auditoria = rs.getString("fecha_bloqueo");
				TiendaBloqueada tien = new TiendaBloqueada(idTienda, canal, nombre, comentario, fecha_auditoria);
				tiendas.add(tien);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			try
			{
				if (con1 != null) con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(tiendas);
	}
	
	/**
	 * Método que se encarga de la inserción de una tienda bloqueada para uno o varios canales.
	 * Si tienda.getCanal() contiene canales separados por coma (ej. 'BOT,TIENDA_VIRTUAL'),
	 * inserta cada canal individualmente en tienda_bloqueada y log_bloqueo_tienda.
	 * @param tienda Objeto TiendaBloqueada con idtienda, canal, motivo, comentario, desbloqueoEn.
	 */
	public static void insertarTiendaBloqueada(TiendaBloqueada tienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String canalOriginal = tienda.getCanal();
			if (canalOriginal == null || canalOriginal.trim().isEmpty()) {
				canalOriginal = "TODOS";
			}
			
			String[] canales = canalOriginal.split(",");
			for (String c : canales) {
				String canalIndividual = c.trim().toUpperCase();
				if (canalIndividual.isEmpty()) {
					continue;
				}
				
				// 1. Inserción en log_bloqueo_tienda (intenta con canal, fallback sin canal)
				try {
					String logBloqueo = "insert into log_bloqueo_tienda (idtienda, canal, accion, motivo, observacion, debloqueo_en) values (" 
							+ tienda.getIdtienda() + ", '" + canalIndividual + "', 'BLOQUEO', '" 
							+ escaparSQL(tienda.getMotivo()) + "', '" + escaparSQL(tienda.getComentario()) + "', '" 
							+ escaparSQL(tienda.getDesbloqueoEn()) + "')"; 
					stm.executeUpdate(logBloqueo);
				} catch (Exception exLog) {
					String logBloqueoFallback = "insert into log_bloqueo_tienda (idtienda, accion, motivo, observacion, debloqueo_en) values (" 
							+ tienda.getIdtienda() + ", 'BLOQUEO', '" 
							+ escaparSQL(tienda.getMotivo()) + "', '" + escaparSQL(tienda.getComentario()) + "', '" 
							+ escaparSQL(tienda.getDesbloqueoEn()) + "')"; 
					stm.executeUpdate(logBloqueoFallback);
				}
				
				// 2. Inserción en tienda_bloqueada (REPLACE INTO para permitir múltiples canales o actualizar)
				try {
					String insert = "replace into tienda_bloqueada (idtienda, canal, comentario) values ( " 
							+ tienda.getIdtienda() + ", '" + canalIndividual + "', '" + escaparSQL(tienda.getComentario()) + "' )"; 
					logger.info(insert);
					stm.executeUpdate(insert);
				} catch (Exception exInsert) {
					// Fallback si la tabla no tiene la columna canal aún
					String insertFallback = "replace into tienda_bloqueada (idtienda, comentario) values ( " 
							+ tienda.getIdtienda() + ", '" + escaparSQL(tienda.getComentario()) + "' )"; 
					logger.info(insertFallback);
					stm.executeUpdate(insertFallback);
				}
			}
			
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error("Error al insertar tienda bloqueada: " + e.toString());
			try
			{
				if (con1 != null) con1.close();
			}catch(Exception e1)
			{
			}
		}
	}

	/**
	 * Método que se encarga de la eliminación/desbloqueo de una tienda para un canal específico o general.
	 * @param idtienda Id de la tienda a desbloquear
	 * @param canal Canal a desbloquear ('TODOS' o null desbloquea todos los bloqueos de esa tienda; un canal puntual desbloquea solo ese).
	 */
	public static void eliminarTiendaBloqueada(int idtienda, String canal)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String canalFiltro = (canal == null) ? "" : canal.trim().toUpperCase();
			
			// Registro en auditoría
			try {
				String canalAuditoria = canalFiltro.isEmpty() ? "TODOS" : canalFiltro;
				String logBloqueo = "insert into log_bloqueo_tienda (idtienda, canal, accion) values (" + idtienda + ", '" + canalAuditoria + "', 'DESBLOQUEO')"; 
				stm.executeUpdate(logBloqueo);
			} catch (Exception exLog) {
				String logBloqueoFallback = "insert into log_bloqueo_tienda (idtienda, accion) values (" + idtienda + ", 'DESBLOQUEO')"; 
				stm.executeUpdate(logBloqueoFallback);
			}
			
			// Eliminación en tienda_bloqueada
			String delete;
			if (canalFiltro.isEmpty() || canalFiltro.equals("TODOS") || canalFiltro.equals("ALL")) {
				delete = "delete from tienda_bloqueada where idtienda = " + idtienda; 
			} else {
				delete = "delete from tienda_bloqueada where idtienda = " + idtienda + " and canal = '" + canalFiltro + "'"; 
			}
			logger.info(delete);
			stm.executeUpdate(delete);
			
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error("Error al eliminar tienda bloqueada: " + e.toString());
			try
			{
				if (con1 != null) con1.close();
			}catch(Exception e1)
			{
			}
		}
	}

	/**
	 * Sobrecarga compatible hacia atrás: Desbloquea todos los canales de la tienda.
	 */
	public static void eliminarTiendaBloqueada(int idtienda)
	{
		eliminarTiendaBloqueada(idtienda, "TODOS");
	}

	/**
	 * Valida si una tienda está bloqueada para un canal determinado.
	 * Regla jerárquica: Una tienda se considera bloqueada si tiene un bloqueo 'TODOS'
	 * O si tiene un bloqueo específico para el canal solicitado.
	 * @param idTienda Id de la tienda a evaluar
	 * @param canal Canal consultado (ej. 'TIENDA_VIRTUAL', 'BOT', 'CALL_CENTER', 'POS', o 'TODOS')
	 * @return true si la tienda está bloqueada para ese canal, false si está disponible
	 */
	public static boolean validarTiendaBloqueada(int idTienda, String canal)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String canalCons = (canal == null || canal.trim().isEmpty()) ? "TODOS" : canal.trim().toUpperCase();
			
			String consulta;
			if (canalCons.equals("TODOS")) {
				// Consulta de bloqueo general: solo aplica si está bloqueada con canal 'TODOS'
				consulta = "select 1 from tienda_bloqueada where idtienda = " + idTienda + " and canal = 'TODOS' limit 1";
			} else {
				// Consulta jerárquica: aplica si está bloqueado para TODOS o para este canal específico
				consulta = "select 1 from tienda_bloqueada where idtienda = " + idTienda 
						+ " and (canal = 'TODOS' or canal = '" + canalCons + "') limit 1";
			}
			
			ResultSet rs;
			try {
				rs = stm.executeQuery(consulta);
			} catch (Exception exCol) {
				// Fallback si la columna canal no existe en la base de datos
				consulta = "select 1 from tienda_bloqueada where idtienda = " + idTienda + " limit 1";
				rs = stm.executeQuery(consulta);
			}
			
			if(rs.next()){
				respuesta = true;
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			try
			{
				if (con1 != null) con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(respuesta);
	}
	
	/**
	 * Sobrecarga compatible hacia atrás: Valida si la tienda tiene cualquier bloqueo activo.
	 * @param idTienda Id de la tienda
	 * @return true si la tienda está bloqueada
	 */
	public static boolean validarTiendaBloqueada(int idTienda)
	{
		return validarTiendaBloqueada(idTienda, "TODOS");
	}
	
	private static String escaparSQL(String valor) {
		if (valor == null) return "";
		return valor.replace("'", "''");
	}
	
}

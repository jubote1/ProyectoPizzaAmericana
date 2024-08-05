package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.*;
import org.json.simple.parser.*;

import capaDAOCC.LogBloqueoTiendaDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.LogBloqueoTienda;
import capaModeloCC.Tienda;

public class TiendaCtrl {
	
	public String obtenerTiendas(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendas();
		
		for (Tienda tienda : tiendas) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("id", tienda.getIdTienda());
			cadaViajeJSON.put("nombre", tienda.getNombreTienda());
			cadaViajeJSON.put("destino", tienda.getDsnTienda());
			cadaViajeJSON.put("urltienda", tienda.getUrl());
			cadaViajeJSON.put("pos", tienda.getPos());
			cadaViajeJSON.put("alertarpedidos", tienda.getAlertarPedidos());
			cadaViajeJSON.put("manejazonas", tienda.getManejaZonas());
			listJSON.add(cadaViajeJSON);
		}
		return listJSON.toJSONString();
	}
	
	
	public String obtenerHostTiendas(){
		ArrayList<JSONObject> tiendas = TiendaDAO.obtenerHostTiendas();
		return tiendas.toString();
	}
	
	public static void main(String args[])
	{
		TiendaCtrl tCtrl = new TiendaCtrl();
	    System.out.println(tCtrl.ServicioVerificarConexInternet());
	    

		
	}
	
	public String obtenerTiendasOtroOrden(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendasOtroOrden();
		
		for (Tienda tienda : tiendas) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("id", tienda.getIdTienda());
			cadaViajeJSON.put("nombre", tienda.getNombreTienda());
			cadaViajeJSON.put("destino", tienda.getDsnTienda());
			cadaViajeJSON.put("urltienda", tienda.getUrl());
			cadaViajeJSON.put("pos", tienda.getPos());
			cadaViajeJSON.put("alertarpedidos", tienda.getAlertarPedidos());
			cadaViajeJSON.put("manejazonas", tienda.getManejaZonas());
			listJSON.add(cadaViajeJSON);
		}
		return listJSON.toJSONString();
	}

	public String obtenerTiendasFuncionales(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendasFuncionales();
		
		for (Tienda tienda : tiendas) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("id", tienda.getIdTienda());
			cadaViajeJSON.put("nombre", tienda.getNombreTienda());
			cadaViajeJSON.put("destino", tienda.getDsnTienda());
			cadaViajeJSON.put("urltienda", tienda.getUrl());
			cadaViajeJSON.put("pos", tienda.getPos());
			cadaViajeJSON.put("alertarpedidos", tienda.getAlertarPedidos());
			cadaViajeJSON.put("manejazonas", tienda.getManejaZonas());
			listJSON.add(cadaViajeJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String obtenerUrlTienda(int idtienda){
		JSONArray listJSON = new JSONArray();
		Tienda tienda = TiendaDAO.obtenerUrlTienda(idtienda);
		JSONObject urlJSON = new JSONObject();
		urlJSON.put("urltienda", tienda.getUrl());
		urlJSON.put("dsnodbc", tienda.getDsnTienda());
		urlJSON.put("pos", tienda.getPos());
		listJSON.add(urlJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * M�todo de la capa controlador que recibe los par�metros para la inserci�n de una nueva tienda, invoca la capa DAO y retorna el ID de la nueva tienda creada.
	 * @param nombre Par�metro con el valor de la tienda a insertar.
	 * @param dsn Par�metro con el valor del Data Source Name de la tienda a crear
	 * @return Retorna el valor id de la tienda creada en formato JSON.
	 */
	public String insertarTienda(String nombre, String dsn)
	{
		JSONArray listJSON = new JSONArray();
		Tienda Tie = new Tienda(0,nombre,dsn,"",0, "", "", "");
		int idTieIns = TiendaDAO.insertarTienda(Tie);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idtienda", idTieIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	/**
	 * M�todo de la capa controladora que dado un id tienda, lo retorna en formato JSON luego de invocada la capa DAO.
	 * @param idtienda Par�metro con el valor de idtienda que se desea consultar.
	 * @return Se retorna la entidad tienda consultada en formato JSON.
	 */
	public String retornarTienda(int idtienda)
	{
		JSONArray listJSON = new JSONArray();
		Tienda Tie = TiendaDAO.retornarTienda(idtienda);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idtienda", Tie.getIdTienda());
		ResultadoJSON.put("nombre", Tie.getNombreTienda());
		ResultadoJSON.put("dsn", Tie.getDsnTienda());
		ResultadoJSON.put("alertarpedidos", Tie.getAlertarPedidos());
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	
	/**
	 * M�todo de la capa controlador que retorna en formato JSON las tiendas que existen en la base de datos
	 * @return Retorna en formato JSON las tiendas que existen en la base de datos, invocando la capa DAO.
	 */
	public String retornarTiendas(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendas();
		for (Tienda Tie : tiendas) 
		{
			JSONObject ResultadoJSON = new JSONObject();
			ResultadoJSON.put("idtienda", Tie.getIdTienda());
			ResultadoJSON.put("nombre", Tie.getNombreTienda());
			ResultadoJSON.put("dsn", Tie.getDsnTienda());
			listJSON.add(ResultadoJSON);
		}
		return listJSON.toJSONString();
	}
	
	/**
	 * M�todo de la capa controlador que se encarga de Eliminar una tienda, con base en la id tienda recibida como par�metro.			
	 * @param idtienda Par�metro con base en el cual realiza la eliminaci�n de la tienda.
	 * @return Retorna el resultado del proceso.
	 */
	public String eliminarTienda(int idtienda)
	{
		JSONArray listJSON = new JSONArray();
		TiendaDAO.eliminarTienda(idtienda);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", "exitoso");
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * M�todo en la capa controlador que se encarga de la edici�n de la entidad tienda
	 * @param idtienda Par�metro de la tienda a modificar.
	 * @param nombre NOmbre de la tienda a ser modificado
	 * @param dsn Data source name de la tienda a ser modificado
	 * @return Se retorna el resultado del proceso.
	 */
	public String editarTienda(int idtienda, String nombre, String dsn, String alertarPedidos)
	{
		JSONArray listJSON = new JSONArray();
		Tienda Tie = new Tienda(idtienda,nombre,dsn,"",0, "", alertarPedidos, "");
		String resultado =TiendaDAO.editarTienda(Tie);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", resultado);
		listJSON.add(ResultadoJSON);
		System.out.println(listJSON.toJSONString());
		return listJSON.toJSONString();
	}	
	
	public int realizarHomologacionTiendaVirtual(String tiendaVirtual)
	{
		int idTienda = TiendaDAO.realizarHomologacionTiendaVirtual(tiendaVirtual);
		return(idTienda);
	}
	
	public String consultarBloqueosAprobados(int idTienda, String fecha)
	{
		boolean respuesta = LogBloqueoTiendaDAO.consultarBloqueosAprobados(idTienda, fecha);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", respuesta);
		return(resultadoJSON.toJSONString());
	}
	
	public String aprobarBloqueoTienda(int idLogBloqueo)
	{
		boolean respuesta = LogBloqueoTiendaDAO.aprobarBloqueoTienda(idLogBloqueo);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", respuesta);
		return(resultadoJSON.toJSONString());
	}
	public String consultarBloqueosTienda(int idTienda, String fecha)
	{
		ArrayList<LogBloqueoTienda> respuestas = LogBloqueoTiendaDAO.consultarBloqueosTienda(idTienda, fecha);
		JSONObject temp = new JSONObject();
		JSONArray respuestaFinal = new JSONArray();
		for(LogBloqueoTienda logTemp : respuestas)
		{
			temp = new JSONObject();
			temp.put("idlogbloqueo", logTemp.getIdLogBloqueo());
			temp.put("tienda", logTemp.getTienda());
			temp.put("accion", logTemp.getAccion());
			temp.put("fecha_accion", logTemp.getFechaAccion());
			temp.put("motivo", logTemp.getMotivo());
			temp.put("observacion", logTemp.getObservacion());
			temp.put("aprobado", logTemp.getAprobado());
			respuestaFinal.add(temp);
		}

		return(respuestaFinal.toJSONString());
	}

	public static String formatIP(String ip) {
	    // Verifica si la IP es nula o vacía
	    if (ip == null || ip.isEmpty()) {
	        return ""; // Retorna una cadena vacía en lugar de lanzar una excepción
	    }

	    // Divide la IP en partes usando el punto como delimitador
	    String[] tokens = ip.split("\\.");

	    // Verifica si la IP tiene exactamente 4 partes
	    if (tokens.length != 4) {
	        return ""; // Retorna una cadena vacía si la IP no es válida
	    }

	    // Construye la nueva IP reemplazando el último octeto por 254
	    return String.format("%s.%s.%s.254", tokens[0], tokens[1], tokens[2]);
	}
	
	public String ServicioVerificarConexInternet() {
	    JSONArray respuesta = new JSONArray();
	    ArrayList<JSONObject> tiendas = TiendaDAO.obtenerHostTiendas();
	    
	    for (JSONObject t : tiendas) {
	        String hosbd = (String) t.get("hosbd");
	        String formattedIP = formatIP(hosbd);
	        String funcional = (String) t.get("funcional");

	        // Verifica si la IP formateada no está vacía y el campo funcional es "S"
	        if (!formattedIP.isEmpty() && "S".equals(funcional)) {
	            boolean isReachable = pingHost(formattedIP);
	            JSONObject json = new JSONObject();
	            json.put("nombre", (String) t.get("nombre"));
	            json.put("conexion", isReachable);
	            
	            String mensaje = isReachable ? 
	                "Ping successful to " + hosbd : 
	                "Error pinging " + hosbd;
	            
	            json.put("mensaje", mensaje);
	            respuesta.add(json);
	        }
	    }

	    return respuesta.toJSONString();
	}

	
	public  boolean pingHost(String host) {		
	      boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
	        String cmd = isWindows ? "ping -n 1 " + host : "ping -c 1 " + host;

	        try {
	            Process process = Runtime.getRuntime().exec(cmd);
	            int returnVal = process.waitFor();
	            return returnVal == 0;
	        } catch (Exception e) {
	            return false;
	        }
	}
}

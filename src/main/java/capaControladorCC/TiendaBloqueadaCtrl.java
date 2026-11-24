package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.TiendaBloqueadaDAO;
import capaModeloCC.TiendaBloqueada;

/**
 * Clase de la capa controladora que se encarga de todo lo relacionado con la entidad TiendaBloqueada.
 * Serializa a JSON e interactúa con la capa DAO soportando multi-canal.
 * @author JuanDavid / Modificado: Soporte Multi-Canal
 */
public class TiendaBloqueadaCtrl {
	
	/**
	 * Retorna todas las tiendas bloqueadas en formato JSON, incluyendo el canal afectado.
	 * @return String en formato JSON
	 */
	public String retornarTiendasBloqueadas(){
		JSONArray listJSON = new JSONArray();
		ArrayList<TiendaBloqueada> tiendas = TiendaBloqueadaDAO.retornarTiendasBloqueadas();
		
		for (TiendaBloqueada tienda : tiendas) {
			JSONObject cadaTiendaJSON = new JSONObject();
			cadaTiendaJSON.put("idtienda", tienda.getIdtienda());
			cadaTiendaJSON.put("canal", tienda.getCanal());
			cadaTiendaJSON.put("nombre", tienda.getNombreTienda());
			cadaTiendaJSON.put("comentario", tienda.getComentario());
			cadaTiendaJSON.put("fecha", tienda.getFechaAuditoria());
			listJSON.add(cadaTiendaJSON);
		}
		return listJSON.toJSONString();
	}
	
	/**
	 * Inserta el bloqueo para uno o varios canales.
	 * @param idtienda Identificación de la tienda
	 * @param observacion Observación o comentario adicional
	 * @param motivo Motivo del bloqueo
	 * @param desbloqueoEn Tiempo estimado de desbloqueo
	 * @param canales Cadena de canales separados por coma (ej. 'TODOS', o 'BOT,TIENDA_VIRTUAL')
	 * @return JSON con resultado
	 */
	public String insertarTiendaBloqueada(int idtienda, String observacion, String motivo, String desbloqueoEn, String canales){
		JSONArray listJSON = new JSONArray();
		String canalesStr = (canales == null || canales.trim().isEmpty()) ? "TODOS" : canales.trim();
		TiendaBloqueada tien = new TiendaBloqueada(idtienda, canalesStr, "", observacion, "");
		tien.setMotivo(motivo);
		tien.setDesbloqueoEn(desbloqueoEn);
		TiendaBloqueadaDAO.insertarTiendaBloqueada(tien);
		JSONObject urlJSON = new JSONObject();
		urlJSON.put("resultado", "exitoso");
		listJSON.add(urlJSON);
		return listJSON.toJSONString();
	}

	/**
	 * Sobrecarga compatible con versiones anteriores (bloquea TODOS los canales).
	 */
	public String insertarTiendaBloqueada(int idtienda, String observacion, String motivo, String desbloqueoEn){
		return insertarTiendaBloqueada(idtienda, observacion, motivo, desbloqueoEn, "TODOS");
	}

	/**
	 * Desbloquea una tienda para un canal específico o general.
	 * @param idtienda Identificación de la tienda
	 * @param canal Canal a desbloquear ('TODOS' o puntual)
	 * @return JSON con resultado
	 */
	public String eliminarTiendaBloqueada(int idtienda, String canal){
		JSONArray listJSON = new JSONArray();
		TiendaBloqueadaDAO.eliminarTiendaBloqueada(idtienda, canal);
		JSONObject urlJSON = new JSONObject();
		urlJSON.put("resultado", "exitoso");
		listJSON.add(urlJSON);
		return listJSON.toJSONString();
	}

	/**
	 * Sobrecarga compatible con versiones anteriores (desbloquea todos los canales de la tienda).
	 */
	public String eliminarTiendaBloqueada(int idtienda){
		return eliminarTiendaBloqueada(idtienda, "TODOS");
	}

}

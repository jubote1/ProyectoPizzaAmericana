package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.*;
import org.json.simple.parser.*;

import capaDAOCC.TiendaBloqueadaDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;
import capaModeloCC.TiendaBloqueada;
/**
 * Clase de la capa controladora que se encarga de todo lo relacionado con la entidad TiendaBLoqueada
 * @author JuanDavid
 *
 */
public class TiendaBloqueadaCtrl {
	
	/**
	 * Método que se encarga de retornar todas las tiendas bloqueadas en el sistema desde la capa controladora, es en esta capa
	 * donde se realiza el paso a formato json con las tiendas bloqueadas en el sistema.
	 * @return Se retorna un String en formato JSON con todas las tiendas bloqueadas definidas en el sistema
	 */
	public String retornarTiendasBloqueadas(){
		JSONArray listJSON = new JSONArray();
		ArrayList<TiendaBloqueada> tiendas = TiendaBloqueadaDAO.retornarTiendasBloqueadas();
		
		for (TiendaBloqueada tienda : tiendas) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("idtienda", tienda.getIdtienda());
			cadaViajeJSON.put("nombre", tienda.getNombreTienda());
			cadaViajeJSON.put("comentario", tienda.getComentario());
			cadaViajeJSON.put("fecha", tienda.getFechaAuditoria());
			listJSON.add(cadaViajeJSON);
		}
		return listJSON.toJSONString();
	}
	
	/**
	 * Método en la capa controladora que se encarga de recibir los parámetros para la inserción de una tienda bloqueadas y de hacer
	 * la respectiva comunicación con la capa DAO.
	 * @param idtienda Se recibe la identificacion de la tienda que se pretende bloquear
	 * @param comentario Se recibe un String con un posible comentarios de porque se está realiazando la acción de bloqueo de tienda.
	 * @return Se retorna un String con el resultado del proceso en formato JSON.
	 */
	public String insertarTiendaBloqueada(int idtienda, String observacion, String motivo, String desbloqueoEn){
		JSONArray listJSON = new JSONArray();
		TiendaBloqueada tien = new TiendaBloqueada(idtienda,"", observacion, "");
		tien.setMotivo(motivo);
		tien.setDesbloqueoEn(desbloqueoEn);
		TiendaBloqueadaDAO.insertarTiendaBloqueada(tien);
		JSONObject urlJSON = new JSONObject();
		urlJSON.put("resultado", "exitoso");
		listJSON.add(urlJSON);
		return listJSON.toJSONString();
	}
	/**
	 * Métodoe en la capa controlador que se encarga de eliminar una tienda bloqueada, este método se encarga de comunicarse con el
	 * correspondiente método en la capa DAO.
	 * @param idtienda Se recibe como parámetro la identificacion de la tienda que va a ser eliminada.
	 * @return Se retornar un String en formato JSON con el resultado del proceso.
	 */
	public String eliminarTiendaBloqueada(int idtienda){
		JSONArray listJSON = new JSONArray();
		TiendaBloqueadaDAO.eliminarTiendaBloqueada(idtienda);
		JSONObject urlJSON = new JSONObject();
		urlJSON.put("resultado", "exitoso");
		listJSON.add(urlJSON);
		return listJSON.toJSONString();
	}

}

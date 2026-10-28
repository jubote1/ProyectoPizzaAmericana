package capaControladorCC;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.MenuDAO;
import capaModeloCC.MenuModulo;
import capaModeloCC.Pantalla;

/**
 * Arma el arbol de menu (modulo -> pantallas) que puede ver un usuario
 * logueado, segun sus roles. Reemplaza los 3 archivos Menu.html/MenuAdm.html/
 * MenuPQRS.html estaticos: en vez de elegir entre 3 HTML fijos, el HTML del
 * menu es uno solo y pinta lo que este JSON traiga.
 */
public class MenuCtrl {

	public String obtenerMenuUsuario(int idUsuario) {
		JSONArray modulosJSON = new JSONArray();
		for (MenuModulo modulo : MenuDAO.obtenerMenuParaUsuario(idUsuario)) {
			modulosJSON.add(moduloAJSON(modulo));
		}
		return (modulosJSON.toJSONString());
	}

	/**
	 * Un modulo con sus pantallas y, si es de primer nivel, sus submodulos (que
	 * a su vez traen sus pantallas). Solo hay dos niveles: el menu no es un
	 * arbol libre, es modulo -> submodulo -> pantalla.
	 */
	@SuppressWarnings("unchecked")
	private JSONObject moduloAJSON(MenuModulo modulo) {
		JSONObject moduloJSON = new JSONObject();
		moduloJSON.put("idmodulo", modulo.getIdModulo());
		moduloJSON.put("nombre", modulo.getNombre());
		JSONArray pantallasJSON = new JSONArray();
		for (Pantalla pantalla : modulo.getPantallas()) {
			JSONObject pantallaJSON = new JSONObject();
			pantallaJSON.put("idpantalla", pantalla.getIdPantalla());
			pantallaJSON.put("nombre", pantalla.getNombre());
			pantallaJSON.put("url_html", pantalla.getUrlHtml());
			pantallasJSON.add(pantallaJSON);
		}
		moduloJSON.put("pantallas", pantallasJSON);
		JSONArray submodulosJSON = new JSONArray();
		for (MenuModulo sub : modulo.getSubmodulos()) {
			submodulosJSON.add(moduloAJSON(sub));
		}
		moduloJSON.put("submodulos", submodulosJSON);
		return moduloJSON;
	}

}

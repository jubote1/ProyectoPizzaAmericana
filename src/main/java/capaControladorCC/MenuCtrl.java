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

	@SuppressWarnings("unchecked")
	public String obtenerMenuUsuario(int idUsuario) {
		JSONArray modulosJSON = new JSONArray();
		for (MenuModulo modulo : MenuDAO.obtenerMenuParaUsuario(idUsuario)) {
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
			modulosJSON.add(moduloJSON);
		}
		return (modulosJSON.toJSONString());
	}

}

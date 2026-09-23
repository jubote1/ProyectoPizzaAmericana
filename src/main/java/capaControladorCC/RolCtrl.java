package capaControladorCC;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.PantallaDAO;
import capaDAOCC.RolDAO;
import capaDAOCC.RolPantallaDAO;
import capaDAOCC.UsuarioDAO;
import capaDAOCC.UsuarioRolDAO;
import capaModeloCC.MenuModulo;
import capaModeloCC.Pantalla;
import capaModeloCC.Rol;
import capaModeloCC.Usuario;

/**
 * Administracion de roles, pantallas por rol, y rol(es) por usuario. Mismo
 * patron de armado de JSON que ParametrosCtrl (JSONArray/JSONObject de
 * org.json.simple).
 */
public class RolCtrl {

	// ===================================================================
	// Catalogo de roles
	// ===================================================================

	@SuppressWarnings("unchecked")
	public String listarRoles() {
		JSONArray listJSON = new JSONArray();
		for (Rol rol : RolDAO.listarRoles()) {
			listJSON.add(rolAJSON(rol));
		}
		return (listJSON.toJSONString());
	}

	public String retornarRol(int idRol) {
		JSONArray listJSON = new JSONArray();
		listJSON.add(rolAJSON(RolDAO.retornarRol(idRol)));
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String insertarRol(String nombre, String descripcion) {
		int idRolIns = RolDAO.insertarRol(new Rol(nombre, descripcion));
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("idrol", idRolIns);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String editarRol(int idRol, String nombre, String descripcion) {
		String resultado = RolDAO.editarRol(new Rol(idRol, nombre, descripcion, "S"));
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", resultado);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String eliminarRol(int idRol) {
		String resultado = RolDAO.eliminarRol(idRol);
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", resultado);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private JSONObject rolAJSON(Rol rol) {
		JSONObject rolJSON = new JSONObject();
		rolJSON.put("idrol", rol.getIdRol());
		rolJSON.put("nombre", rol.getNombre());
		rolJSON.put("descripcion", rol.getDescripcion());
		rolJSON.put("activo", rol.getActivo());
		return (rolJSON);
	}

	// ===================================================================
	// Pantallas por modulo (para el checklist de asignacion)
	// ===================================================================

	@SuppressWarnings("unchecked")
	public String listarModulosConPantallas() {
		JSONArray modulosJSON = new JSONArray();
		for (MenuModulo modulo : PantallaDAO.listarModulosConPantallas()) {
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

	// ===================================================================
	// Pantallas asignadas a un rol
	// ===================================================================

	@SuppressWarnings("unchecked")
	public String listarIdsPantallaPorRol(int idRol) {
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		JSONArray idsJSON = new JSONArray();
		for (Integer idPantalla : PantallaDAO.listarIdsPantallaPorRol(idRol)) {
			idsJSON.add(idPantalla);
		}
		resultadoJSON.put("idspantalla", idsJSON);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String guardarPantallasRol(int idRol, String idsPantallaTexto) {
		String resultado = RolPantallaDAO.guardarPantallasRol(idRol, parsearListaEnteros(idsPantallaTexto));
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", resultado);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	// ===================================================================
	// Rol(es) por usuario
	// ===================================================================

	@SuppressWarnings("unchecked")
	public String listarUsuariosConRoles() {
		JSONArray listJSON = new JSONArray();
		List<Usuario> usuarios = UsuarioDAO.obtenerUsuarioActivo();
		for (Usuario usu : usuarios) {
			JSONObject usuarioJSON = new JSONObject();
			usuarioJSON.put("idusuario", usu.getId());
			usuarioJSON.put("nombreusuario", usu.getNombreUsuario());
			usuarioJSON.put("nombrelargo", usu.getNombreLargo());
			JSONArray rolesJSON = new JSONArray();
			for (String nombreRol : UsuarioRolDAO.listarNombresRolPorUsuario(usu.getId())) {
				rolesJSON.add(nombreRol);
			}
			usuarioJSON.put("roles", rolesJSON);
			listJSON.add(usuarioJSON);
		}
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String listarIdsRolPorUsuario(int idUsuario) {
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		JSONArray idsJSON = new JSONArray();
		for (Integer idRol : UsuarioRolDAO.listarIdsRolPorUsuario(idUsuario)) {
			idsJSON.add(idRol);
		}
		resultadoJSON.put("idsrol", idsJSON);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String guardarRolesUsuario(int idUsuario, String idsRolTexto) {
		String resultado = UsuarioRolDAO.guardarRolesUsuario(idUsuario, parsearListaEnteros(idsRolTexto));
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", resultado);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	private ArrayList<Integer> parsearListaEnteros(String texto) {
		ArrayList<Integer> valores = new ArrayList<>();
		if (texto == null) {
			return (valores);
		}
		StringTokenizer tokens = new StringTokenizer(texto, ",; \n\r\t");
		while (tokens.hasMoreTokens()) {
			try {
				valores.add(Integer.parseInt(tokens.nextToken().trim()));
			} catch (Exception e) {
				// Se ignora un token que no sea numero, en vez de tumbar todo el guardado.
			}
		}
		return (valores);
	}

}

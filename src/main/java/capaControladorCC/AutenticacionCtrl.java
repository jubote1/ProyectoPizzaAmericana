package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.AuthKunoDAO;
import capaDAOCC.PermisosGrupoDAO;
import capaDAOCC.UsuarioDAO;
import capaModeloCC.PermisosGrupo;
import capaModeloCC.Usuario;

/**
 * Clase AutenticacionCtrl tiene como objetivo hacer las veces de Controlador para la autenticación de usuarios
 * en el aplicatiov
 * @author Juan David Botero Duque
 * @
 */
public class AutenticacionCtrl {
	
	
	private static AutenticacionCtrl instance;
	
	//singleton controlador
	public static AutenticacionCtrl getInstance(){
		if(instance == null){
			instance = new AutenticacionCtrl();
		}
		return instance;
	}
	
	/**
	 * 
	 * @param usuario El usuario de logueo de la aplicación
	 * @param contrasena Contraseña asociada al usuario que se está logueando
	 * @return Se retona un valor booleano indicando si el usuario y contraseña corresponde con alguien logueado
	 * en al aplicación
	 */
	public boolean autenticarUsuario(String usuario, String contrasena){
		Usuario usu = new Usuario(usuario, contrasena, "");
		boolean resultado = UsuarioDAO.validarUsuario(usu);
		return(resultado);
	}
	
	public String autenticarUsuarioInventario(String usuario, String contrasena){
		capaModeloPOS.Usuario usu = new capaModeloPOS.Usuario(usuario);
		usu.setContrasena(contrasena);
		capaModeloPOS.Usuario usuResp = UsuarioDAO.validarUsuarioInventario(usu);
		ArrayList<PermisosGrupo> permisos = PermisosGrupoDAO.obtenerPermisosGrupo(usuResp.getIdGrupo());
		JSONArray permisosJSON = new JSONArray();
		JSONObject permisoJSONObj = new JSONObject();
		for(PermisosGrupo temp: permisos)
		{
			permisoJSONObj = new JSONObject();
			//permisoJSONObj.put("idpermiso",temp.getIdPermiso());
			//permisoJSONObj.put("idgrupo",temp.getIdGrupo());
			permisoJSONObj.put("nombre_permiso",temp.getNombrePermiso());
			permisosJSON.add(permisoJSONObj);
		}
		JSONObject respuesta = new JSONObject();
		respuesta.put("usuario", usuResp.getNombreUsuario());
		respuesta.put("nombreLargo", usuResp .getNombreLargo());
		respuesta.put("idgrupo", usuResp.getIdGrupo());
		respuesta.put("permisos", permisosJSON.toJSONString());
		return(respuesta.toJSONString());
	}
	
	/**
	 * 
	 * @param usuario Se recibe el usuario de aplicación con el fin de validar si el usuario pasado como parámetro está
	 * o no logueado en la aplicación
	 * @return Se retorna un valor booleano indicando si el usuario se encuentra o no logueado en el aplicativo.
	 */
	public String validarAutenticacion(String usuario)
	{
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		Usuario usu = new Usuario(usuario);
		String resultado = UsuarioDAO.validarAutenticacion(usu);
		if (resultado.equals(new  String ("N")) ){
			Respuesta.put("respuesta", "OK");
    		
		} 
		else if(resultado.equals(new  String ("S"))){
			Respuesta.put("respuesta", "OKA");
    	
		}else if(resultado.equals(new  String ("P"))){
			Respuesta.put("respuesta", "OKP");
		}
		else 
		{
			Respuesta.put("respuesta", "NOK");
		}
		Respuesta.put("nombreusuario", usu.getNombreLargo());
		Respuesta.put("plataforma", usu.getPlataforma());
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public boolean validarAuthKuno(String authHeader)
	{
		boolean respuesta = false;
		ArrayList auths = AuthKunoDAO.obtenerAuths();
		for(int i = 0 ; i < auths.size(); i++)
		{
			String temp = (String)auths.get(i);
			if(temp.equals(authHeader))
			{
				respuesta = true;
				break;
			}
		}
		return(respuesta);
	}

}

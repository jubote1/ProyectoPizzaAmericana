package capaControladorCC;

import org.json.simple.JSONObject;

import capaDAOCC.ClienteFidelizacionDAO;

public class FidelizacionCtrl {
	
	
	public String existeClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean existe = ClienteFidelizacionDAO.existeClienteFidelizacion(correo);
		respuesta.put("respuesta", existe);
		return(respuesta.toJSONString());
	}
	
	public String insertarClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.insertarClienteFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}

}
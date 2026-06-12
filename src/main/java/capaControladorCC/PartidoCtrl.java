package capaControladorCC;

import org.json.simple.JSONObject;

import capaDAOCC.partidoDAO;
import capaModeloCC.Pronostico;

public class PartidoCtrl {
	
	public String obtenerPartidoPublicado() {
	    return partidoDAO.obtenerPartidoPublicado().toString();
	}
	
	
	public String guardarPronostico(Pronostico pronostico) {

	    if (partidoDAO.existePronosticoFactura(
	            pronostico.getNumeroFactura())) {

	        JSONObject respuesta = new JSONObject();

	        respuesta.put("success", false);
	        respuesta.put("message",
	                "La factura ya tiene un pronóstico registrado.");

	        return respuesta.toString();
	    }

	    return partidoDAO.guardarPronostico(pronostico).toString();
	}
}

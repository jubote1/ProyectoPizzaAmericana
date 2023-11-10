package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.*;
import org.json.simple.parser.*;

import capaDAOCC.CoordenadaPoligonoDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.PoligonoDAO;
import capaDAOCC.TiendaDAO;
import capaDAOCC.UbicacionDomiciliarioDAO;
import capaModeloCC.CoordenadaPoligono;
import capaModeloCC.Poligono;
import capaModeloCC.Tienda;

public class GeolocalizacionCtrl implements Runnable {
	
	/*
	 * Método que retorna los poligonos definidos en un JSON
	 */
	Thread hiloInsertarUbicacion;
	String claveDomiciliario;
	int idTienda;
	float latitud;
	float longitud;
	
	public String obtenerPoligonos(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Poligono> poligonos = PoligonoDAO.obtenerPoligonos();
		
		for (Poligono poligono : poligonos) {
			JSONObject cadaPoligonoJSON = new JSONObject();
			cadaPoligonoJSON.put("idpoligono", poligono.getIdPoligono());
			cadaPoligonoJSON.put("nombrepoligono", poligono.getNombrePoligo());
			cadaPoligonoJSON.put("ubicacionmapa", poligono.getUbicacionMapa());
			listJSON.add(cadaPoligonoJSON);
		}
		return listJSON.toJSONString();
	}
	
	
	public String obtenerCoordenadasPoligono(int idPoligono)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList<CoordenadaPoligono> coorPoligonos =  CoordenadaPoligonoDAO.obtenerCoordenadasPoligono(idPoligono);
		for (CoordenadaPoligono coorPoligono : coorPoligonos) {
			JSONObject cadaCoorPoligonoJSON = new JSONObject();
			cadaCoorPoligonoJSON.put("lat", coorPoligono.getLatitud());
			cadaCoorPoligonoJSON.put("lng", coorPoligono.getLongitud());
			listJSON.add(cadaCoorPoligonoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String insertarUbicacionDomiciliario(String claveDomiciliario, int idTienda, float latitud, float longitud)
	{
		JSONObject respuesta = new JSONObject();
		this.claveDomiciliario = claveDomiciliario;
		this.idTienda = idTienda;
		this.latitud = latitud;
		this.longitud = longitud;
		hiloInsertarUbicacion = new Thread(this);
		hiloInsertarUbicacion.start();
		respuesta.put("resultado", "OK");
		return(respuesta.toJSONString());
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		Thread ct = Thread.currentThread();
		//Validamos si el hilo es de impuestos con el fin de poder arrancar el hilo que descuenta de inventarios
		if(ct == hiloInsertarUbicacion) 
		{  
			UbicacionDomiciliarioDAO.insertarUbicacionDomiciliario(claveDomiciliario, idTienda, latitud, longitud);
		}
	}
	
}

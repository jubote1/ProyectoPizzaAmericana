package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.ClienteFidelizacionDAO;
import capaDAOCC.FidelizacionTransaccionDAO;
import capaModeloCC.FidelizacionTransaccion;

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
	

	public String activarClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.activarClienteFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}

	
	
	public String desactivarClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.desactivarClienteFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}


	public String sumarPuntosClienteFidelizacion(String correo, double puntosSumar, int idTienda, int idPedidoTienda, double valorNeto)
	{
		JSONObject respuesta = new JSONObject();
		boolean acumula =  false;
		boolean creaTransaccion = false;
		boolean existe = FidelizacionTransaccionDAO.existeFidelizacionTransaccion(correo, idTienda, idPedidoTienda);
		if(!existe)
		{
			acumula =  ClienteFidelizacionDAO.sumarPuntosClienteFidelizacion(correo, puntosSumar);
			FidelizacionTransaccion transaccion = new FidelizacionTransaccion(correo, idTienda, idPedidoTienda, valorNeto, puntosSumar);
			creaTransaccion = FidelizacionTransaccionDAO.insertarFidelizacionTransaccion(transaccion);
			if(acumula && creaTransaccion)
			{
				respuesta.put("respuesta", true);
			}
		}else
		{
			respuesta.put("respuesta", false);
		}
		
		return(respuesta.toJSONString());
	}

	public String  obtenerFidelizacionTransacciones(String correo)
	{
		JSONArray transacciones = new JSONArray();
		JSONObject transaccion = new JSONObject();
		ArrayList<FidelizacionTransaccion> tranObj = FidelizacionTransaccionDAO.obtenerFidelizacionTransacciones(correo);
		FidelizacionTransaccion tranTemp;
		for(int i = 0; i < tranObj.size(); i++)
		{
			tranTemp = tranObj.get(i);
			transaccion = new JSONObject();
			transaccion.put("idtienda", tranTemp.getIdTienda());
			transaccion.put("tienda", tranTemp.getTienda());
			transaccion.put("idpedidotienda", tranTemp.getIdPedidoTienda());
			transaccion.put("fechatransaccion", tranTemp.getFechaTransaccion());
			transaccion.put("valorneto", tranTemp.getValorNeto());
			transaccion.put("puntos", tranTemp.getPuntos());
			transacciones.add(transaccion);
		}
		return(transacciones.toJSONString());
	}
	
	public String obtenerCantidadPuntosCliente(String correo)
	{
		JSONObject respuesta = new JSONObject();
		double puntos = ClienteFidelizacionDAO.obtenerCantidadPuntosCliente(correo);
		respuesta.put("puntos", puntos);
		return(respuesta.toJSONString());
	}
}

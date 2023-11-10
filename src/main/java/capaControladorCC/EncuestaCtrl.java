package capaControladorCC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.EmpleadoEncuestaDAO;
import capaDAOCC.EmpleadoEncuestaDetalleDAO;
import capaDAOCC.EncuestaLaboralDAO;
import capaDAOCC.EncuestaLaboralDetalleDAO;
import capaModeloCC.EmpleadoEncuesta;
import capaModeloCC.EmpleadoEncuestaDetalle;
import capaModeloCC.EncuestaLaboral;
import capaModeloCC.EncuestaLaboralDetalle;


public class EncuestaCtrl {
	
	
	public EncuestaCtrl()
	{
	}
	
	public String obtenerEncuestaLaboral(int idEncuesta)
	{
		JSONObject respuesta = new JSONObject();
		EncuestaLaboral encLaboral =EncuestaLaboralDAO.obtenerEncuestaLaboral(idEncuesta);
		respuesta.put("idencuesta", encLaboral.getIdEncuesta());
		respuesta.put("codigo", encLaboral.getCodigo());
		respuesta.put("dependencia", encLaboral.getDependencia());
		respuesta.put("descripcion", encLaboral.getDescripcion());
		respuesta.put("Encabezado", encLaboral.getEncabezado());
		respuesta.put("nombreencuesta", encLaboral.getNombreEncuesta());
		respuesta.put("version", encLaboral.getVersion());
		return(respuesta.toJSONString());
	}
	
	
	public String obtenerEncLaboralDetalle(int idEncuesta)
	{
		JSONObject resTemp = new JSONObject();
		JSONArray respuesta = new JSONArray();
		ArrayList<EncuestaLaboralDetalle> encLaboralDetalle =  EncuestaLaboralDetalleDAO.obtenerEncLaboralDetalle(idEncuesta);
		for(EncuestaLaboralDetalle detalle: encLaboralDetalle)
		{
			resTemp = new JSONObject();
			resTemp.put("idencuesta", detalle.getIdEncuesta());
			resTemp.put("alertar", detalle.getAlertar());
			resTemp.put("descripcion", detalle.getDescripcion());
			resTemp.put("idencuestadetalle", detalle.getIdEncuestaDetalle());
			resTemp.put("obligatorio", detalle.getObligatorio());
			resTemp.put("tiporespuesta", detalle.getTipoRespuesta());
			resTemp.put("valoralertar", detalle.getValorAlertar());
			resTemp.put("valordefecto", detalle.getValorDefecto());
			resTemp.put("valorescala", detalle.getValorEscala());
			resTemp.put("valorfinal", detalle.getValorFinal());
			resTemp.put("valorinicial", detalle.getValorInicial());
			respuesta.add(resTemp);
		}
		return(respuesta.toJSONString());
	}
	
	public String insertarEmpleadoEncuestaDetalle(EmpleadoEncuestaDetalle empEncuestaDetalle)
	{
		JSONObject respuesta = new JSONObject();
		int idEmpleadoEncuestaDet = EmpleadoEncuestaDetalleDAO.insertarEmpleadoEncuestaDetalle(empEncuestaDetalle);
		respuesta.put("idempleadoencuestadet",idEmpleadoEncuestaDet);
		return(respuesta.toJSONString());
	}
	
	public String insertarEmpleadoEncuesta(EmpleadoEncuesta empEncuesta)
	{
		int idEmpleadoEncuesta = EmpleadoEncuestaDAO.insertarEmpleadoEncuesta(empEncuesta);
		JSONObject respuesta = new JSONObject();
		respuesta.put("idempleadoencuesta", idEmpleadoEncuesta);
		return(respuesta.toJSONString());
	}	
}

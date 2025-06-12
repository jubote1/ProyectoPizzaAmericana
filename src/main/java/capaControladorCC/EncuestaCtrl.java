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
import java.util.List;
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
import capaModeloCC.EncuestaServicio;


public class EncuestaCtrl {
	
	
	public EncuestaCtrl()
	{
	}
	
	public String obtenerEncuestaLaboral(int idEncuesta, String documento )
	{
		JSONObject respuesta = new JSONObject();
		EncuestaLaboral encLaboral =EncuestaLaboralDAO.obtenerEncuestaLaboral(idEncuesta);
		int idempleado = EncuestaLaboralDAO.obteneridEmpleado(documento);
		respuesta.put("idencuesta", encLaboral.getIdEncuesta());
		respuesta.put("codigo", encLaboral.getCodigo());
		respuesta.put("dependencia", encLaboral.getDependencia());
		respuesta.put("descripcion", encLaboral.getDescripcion());
		respuesta.put("Encabezado", encLaboral.getEncabezado());
		respuesta.put("nombreencuesta", encLaboral.getNombreEncuesta());
		respuesta.put("version", encLaboral.getVersion());
		respuesta.put("idempleado", idempleado);
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
			resTemp.put("tiporespuesta", detalle.getTipoRespuesta());
			resTemp.put("orden", detalle.getOrden());
			resTemp.put("porcentaje", detalle.getPorcentaje());
			resTemp.put("dependencia", detalle.getDependencia());
			respuesta.add(resTemp);
		}
		return(respuesta.toJSONString());
	}
	
	public String insertarEmpleadoEncuestaDetalle(List<org.json.JSONObject> empEncuestaDetalle) {
	    float totalObtenido = 0;
	    float totalEsperado = 0;
	    float porcentajeTransferido = 0;
	    int preguntasValidas = 0;
	    org.json.JSONObject respuesta = new org.json.JSONObject();
	    
	    try {
	        List<EmpleadoEncuestaDetalle> detallesEncuesta = new ArrayList<>();
	        boolean esOperacional = "operacional".equals(empEncuestaDetalle.get(0).optString("dependencia"));

	        // Primer recorrido: construir lista y (si aplica) calcular porcentajeTransferido
	        for (org.json.JSONObject item : empEncuestaDetalle) {
	            String valorStr = item.optString("valor", "");
	            float valor = item.optFloat("valor", 0);
	            float porcentaje = item.optFloat("porcentaje", 0);

	            EmpleadoEncuestaDetalle det = new EmpleadoEncuestaDetalle(
	                0,
	                item.getInt("idempleadoencuesta"),
	                item.getInt("idencuestadetalle"),
	                "", "", valorStr
	            );
	            det.setObservacionAdicional(item.optString("observacionadi", ""));
	            detallesEncuesta.add(det);

	            if (esOperacional && Math.abs(valor) > 0.0001 && porcentaje != 0) {
	                if (valor == -1) {
	                    porcentajeTransferido += porcentaje;
	                } else {
	                    preguntasValidas++;
	                }
	            }
	        }

	        // Segundo recorrido: solo si es operacional
	        if (esOperacional) {
	            for (org.json.JSONObject item : empEncuestaDetalle) {
	                float valor = item.optFloat("valor", 0);
	                if (Math.abs(valor) < 0.0001) continue;

	                float porcentaje = item.optFloat("porcentaje", 0);
	                if (valor == -1) {
	                    porcentaje = 0;
	                } else if (porcentajeTransferido > 0 && preguntasValidas > 0 && porcentaje != 0) {
	                    porcentaje += porcentajeTransferido / preguntasValidas;
	                }

	                float valorFinal = item.optFloat("valorfinal", 0);
	                totalEsperado += (porcentaje * valorFinal) / 100;
	                totalObtenido += (porcentaje * valor) / 100;
	            }
	        }

	        // Calcular porcentaje total
	        float porcentajeTotal = 0;
	        if (totalEsperado > 0) {
	            porcentajeTotal = Math.round((totalObtenido / totalEsperado) * 10000f) / 100f;
	        }

	        respuesta = EmpleadoEncuestaDetalleDAO.insertarEmpleadoEncuestaDetalle(detallesEncuesta, porcentajeTotal);
	    } catch (Exception e) {
	        System.out.println("Error: " + e);
	        respuesta.put("success", false);
	    }

	    return respuesta.toString();
	}


	
	public String insertarEmpleadoEncuesta(EmpleadoEncuesta empEncuesta)
	{
		int idEmpleadoEncuesta = EmpleadoEncuestaDAO.insertarEmpleadoEncuesta(empEncuesta);
		JSONObject respuesta = new JSONObject();
		respuesta.put("idempleadoencuesta", idEmpleadoEncuesta);
		return(respuesta.toJSONString());
	}	
	
	

}

package capaControladorCC;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.EmpleadoEncuestaDAO;
import capaDAOCC.EmpleadoEventoDAO;
import capaDAOCC.EmpleadoValeDAO;
import capaDAOCC.PeriodoNominaDAO;
import capaModeloCC.EmpleadoVale;
import capaModeloCC.HorarioEmpleado;
import capaModeloCC.PeriodoNomina;

public class EmpleadoCtrl {
	

	public String obtenerResultadoEncuesta(int idTienda, int idEncuesta)
	{
		JSONArray listJSON = new JSONArray();
		Date fechaActual = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendarioActual = Calendar.getInstance();
		calendarioActual.setTime(fechaActual);
		int mesActual = calendarioActual.get(Calendar.MONTH) + 1;
		int anoActual = calendarioActual.get(Calendar.YEAR);
		//Creamos las fechas anterior y posterior
		String fechaInferior = Integer.toString(anoActual) + "-" + Integer.toString(mesActual) + "-01";
		String fechaPosterior = Integer.toString(anoActual) + "-" + Integer.toString(mesActual);
		if(mesActual == 1 || mesActual == 3 || mesActual == 5 || mesActual == 7 || mesActual == 8 || mesActual == 10 || mesActual == 12)
		{
			fechaPosterior = fechaPosterior + "-31";
		}else if(mesActual == 2)
		{
			fechaPosterior = fechaPosterior + "-28";
		}else if(mesActual == 4 || mesActual == 6  || mesActual == 9 || mesActual == 11)
		{
			fechaPosterior = fechaPosterior + "-30";
		}
		ArrayList<String[]> resultadoEncuestas = EmpleadoEncuestaDAO.obtenerResultadoEncuesta( fechaInferior, fechaPosterior, idTienda, idEncuesta);
		for(String[] fila: resultadoEncuestas)
		{
			JSONObject cadaJSON = new JSONObject();
			cadaJSON.put("nombre", fila[0]);
			cadaJSON.put("promedio", fila[1]);
			cadaJSON.put("cantidad", fila[2]);
			listJSON.add(cadaJSON);
		}
		return(listJSON.toJSONString());
	}
	
	public ArrayList<String[]> obtenerResultadoEncuestaArreglo(int idTienda, int idEncuesta)
	{
		Date fechaActual = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendarioActual = Calendar.getInstance();
		calendarioActual.setTime(fechaActual);
		int mesActual = calendarioActual.get(Calendar.MONTH) + 1;
		int anoActual = calendarioActual.get(Calendar.YEAR);
		//Creamos las fechas anterior y posterior
		String fechaInferior = Integer.toString(anoActual) + "-" + Integer.toString(mesActual) + "-01";
		String fechaPosterior = Integer.toString(anoActual) + "-" + Integer.toString(mesActual);
		if(mesActual == 1 || mesActual == 3 || mesActual == 5 || mesActual == 7 || mesActual == 8 || mesActual == 10 || mesActual == 12)
		{
			fechaPosterior = fechaPosterior + "-31";
		}else if(mesActual == 2)
		{
			fechaPosterior = fechaPosterior + "-28";
		}else if(mesActual == 4 || mesActual == 6  || mesActual == 9 || mesActual == 11)
		{
			fechaPosterior = fechaPosterior + "-30";
		}
		ArrayList<String[]> resultadoEncuestas = EmpleadoEncuestaDAO.obtenerResultadoEncuesta( fechaInferior, fechaPosterior, idTienda, idEncuesta);
		return(resultadoEncuestas);
	}
	
	public ArrayList<String[]> obtenerResultadoEncuestaArreglo(int idEncuesta)
	{
		Date fechaActual = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendarioActual = Calendar.getInstance();
		calendarioActual.setTime(fechaActual);
		int mesActual = calendarioActual.get(Calendar.MONTH) + 1;
		int anoActual = calendarioActual.get(Calendar.YEAR);
		//Creamos las fechas anterior y posterior
		String fechaInferior = Integer.toString(anoActual) + "-" + Integer.toString(mesActual) + "-01";
		String fechaPosterior = Integer.toString(anoActual) + "-" + Integer.toString(mesActual);
		if(mesActual == 1 || mesActual == 3 || mesActual == 5 || mesActual == 7 || mesActual == 8 || mesActual == 10 || mesActual == 12)
		{
			fechaPosterior = fechaPosterior + "-31";
		}else if(mesActual == 2)
		{
			fechaPosterior = fechaPosterior + "-28";
		}else if(mesActual == 4 || mesActual == 6  || mesActual == 9 || mesActual == 11)
		{
			fechaPosterior = fechaPosterior + "-30";
		}
		ArrayList<String[]> resultadoEncuestas = EmpleadoEncuestaDAO.obtenerResultadoEncuesta( fechaInferior, fechaPosterior, idEncuesta);
		return(resultadoEncuestas);
	}
	
	public String obtenerHorariosAdministradoresDia()
	{
		ArrayList<HorarioEmpleado> respuestas = EmpleadoEventoDAO.obtenerHorariosAdministradoresDia();
		HorarioEmpleado temp;
		JSONObject objJSON;
		JSONArray arrayJSON = new JSONArray();
		for(int i = 0; i < respuestas.size(); i++)
		{
			temp = respuestas.get(i);
			objJSON = new JSONObject();
			objJSON.put("id", temp.getId());
			objJSON.put("nombre", temp.getNombre());
			objJSON.put("tipoevento", temp.getTipoEvento());
			objJSON.put("fechahora", temp.getFechaHora());
			objJSON.put("tienda", temp.getTienda());
			arrayJSON.add(objJSON);
		}
		return(arrayJSON.toJSONString());
	}
	
	
	public String obtenerResultadoEncuestaOperacion(int idtienda, int idencuesta, String fecha1, String fecha2) {
	    ArrayList<JSONObject> obtenerResult = new ArrayList<>();

	    try {
	        obtenerResult = EmpleadoEncuestaDAO.obtenerResultadoEncuestaOperacion(idtienda, idencuesta, fecha1, fecha2);
	        
	        for (int i = 0; i < obtenerResult.size(); i++) {
	            JSONObject json = obtenerResult.get(i);
	            int idempleadoencuesta = (int) json.get("idempleadoencuesta");
	            float total_obtenido = 0;
	            float total_esperado = 0;
	            float p_t = 0;
	            int longitud = 0;
	            
	            ArrayList<JSONObject> obtenerResultDet = EmpleadoEncuestaDAO.obtenerResultEncuestaOperacionDetalle(idempleadoencuesta);
	            
	            for (int j = 0; j < obtenerResultDet.size(); j++) {
	                JSONObject json2 = obtenerResultDet.get(j);
	                float porcentaje = (float) json2.get("porcentaje");
	                float respuesta = 0;
	                String respuestatxt = (String) json2.get("respuesta");
	                
	                if (respuestatxt != null && !respuestatxt.isEmpty()) {
	                    respuesta = Float.valueOf(respuestatxt);
	                }
	                
	                if (respuesta != 0) {
	                    if (porcentaje != 0) {
	                        if (respuesta == -1) {
	                            p_t += porcentaje;
	                        } else {
	                            longitud++;
	                        }
	                    }
	                }
	            }
	            
	            for (int j = 0; j < obtenerResultDet.size(); j++) {
	                JSONObject json2 = obtenerResultDet.get(j);
	                float porcentaje = (float) json2.get("porcentaje");
	                float respuesta = 0;
	                String respuestatxt = (String) json2.get("respuesta");
	                
	                if (respuestatxt != null && !respuestatxt.isEmpty()) {
	                    respuesta = Float.valueOf(respuestatxt);
	                }
	                
	                if (respuesta != 0) {
	                    if (respuesta == -1) {
	                        porcentaje = 0;
	                    } else {
	                        if (p_t > 0 && longitud > 0 && porcentaje != 0) {
	                            porcentaje += p_t / longitud;
	                        }
	                    }
	                    
	                    float valor_final = (float) json2.get("valor_final");
	                    float valor_esperado = (porcentaje * valor_final) / 100;
	                    float valor_obtenido = (porcentaje * respuesta) / 100;
	                    
	                    total_esperado += valor_esperado;
	                    total_obtenido += valor_obtenido;
	                }
	            }
	            
	            double numeroRedondeado = 0;
	            if (total_esperado > 0) {
	                float result_porcentaje = (total_obtenido / total_esperado) * 100;
	                numeroRedondeado = Math.round(result_porcentaje * 100.0) / 100.0;
	            }
	            
	            obtenerResult.get(i).put("porcentaje_total", numeroRedondeado);
	        }
	    } catch (Exception e) {
	        System.out.println(e);
	    }

	    return obtenerResult.toString();
	}

	
	public String obtenerResulEncuestaOperacionDetalle(int idempleadoencuesta){
		
		ArrayList<JSONObject> respuesta = EmpleadoEncuestaDAO.obtenerResultEncuestaOperacionDetalle(idempleadoencuesta);
			
		return respuesta.toString();
	}
	
	/**
	 * Método que se encarga de adicionar un vale empleado.
	 * @param empleadoVale
	 * @return
	 */
	public String insertarEmpleadoVale(EmpleadoVale empleadoVale)
	{
		int idEmpleadoVale = EmpleadoValeDAO.insertarEmpleadoVale(empleadoVale);
		JSONObject respuesta = new JSONObject();
		respuesta.put("idempleadovale", idEmpleadoVale);
		return(respuesta.toJSONString());
	}
	
	/**
	 * Método que se encarga de la eliminación de un vale
	 * @param idEmpleado
	 * @param fecha
	 * @param idEgreso
	 * @return
	 */
	public String eliminarEmpleadoVale(int idEmpleado, String fecha, int idEgreso)
	{
		boolean respuesta = EmpleadoValeDAO.eliminarEmpleadoVale(idEmpleado, fecha, idEgreso);
		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON .put("respuesta", respuesta);
		return(respuestaJSON .toJSONString());
	}
	
	public String validarEmpleadoVale(int idEmpleado, String fechaVale)
	{
		ArrayList<PeriodoNomina> periodos = PeriodoNominaDAO.obtenerPeriodosNomina();
		//Formateamos la fecha del vale
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date dateFechaVale  = new Date();
		try {
			dateFechaVale = dateFormat.parse(fechaVale);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Date fechaInferior = null;
		Date fechaSuperior = null;
		//Debemos identificar según la fecha del vale a que periodo correspondería
		for(PeriodoNomina periodoTemp : periodos)
		{
			if((dateFechaVale.compareTo(periodoTemp.getFechaInferior()) == 0 || dateFechaVale.compareTo(periodoTemp.getFechaInferior()) == 1) && (dateFechaVale.compareTo(periodoTemp.getFechaSuperior()) == 0 || dateFechaVale.compareTo(periodoTemp.getFechaSuperior()) == -1))
			{
				fechaInferior = periodoTemp.getFechaInferior();
				fechaSuperior = periodoTemp.getFechaSuperior();
				break;
			}
		}
		//Posteriormente debemos de validar cuanto vales hay en ese rango de tiempo para el empleado
		int cantidadVales = 0;
		if(fechaInferior != null && fechaSuperior != null)
		{
			cantidadVales = EmpleadoValeDAO.validarEmpleadoValeNoDescuadre(dateFormat.format(fechaInferior), dateFormat.format(fechaSuperior), idEmpleado);
		}
		JSONObject respuesta = new JSONObject();
		if(cantidadVales == 0)
		{
			respuesta.put("respuesta", true);
		}else
		{
			respuesta.put("respuesta", false);
		}
		
		return(respuesta.toJSONString());
	}


	public static void main(String args[])
	{
		EmpleadoCtrl empCtrl = new EmpleadoCtrl();
		empCtrl.validarEmpleadoVale(648, "2025-03-21");
	}
}

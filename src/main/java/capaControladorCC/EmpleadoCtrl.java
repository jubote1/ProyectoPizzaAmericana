package capaControladorCC;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.EmpleadoEncuestaDAO;
import capaDAOCC.EmpleadoEventoDAO;
import capaModeloCC.HorarioEmpleado;

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

}

package capaControladorCC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
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
import capaDAOCC.ParametrosDAO;
import capaDAOCC.RuletaDAO;
import capaModeloCC.EmpleadoEncuesta;
import capaModeloCC.EmpleadoEncuestaDetalle;
import capaModeloCC.EncuestaLaboral;
import capaModeloCC.EncuestaLaboralDetalle;
import capaModeloCC.EncuestaServicio;
import capaSeguridad.TokenRoulette;

public class EncuestaCtrl {
	private static final SecureRandom random = new SecureRandom();

	public EncuestaCtrl() {
		
	}

	public String obtenerEncuestaLaboral(int idEncuesta, String documento) {
		JSONObject respuesta = new JSONObject();
		EncuestaLaboral encLaboral = EncuestaLaboralDAO.obtenerEncuestaLaboral(idEncuesta);
		int idempleado = EncuestaLaboralDAO.obteneridEmpleado(documento);
		respuesta.put("idencuesta", encLaboral.getIdEncuesta());
		respuesta.put("codigo", encLaboral.getCodigo());
		respuesta.put("dependencia", encLaboral.getDependencia());
		respuesta.put("descripcion", encLaboral.getDescripcion());
		respuesta.put("Encabezado", encLaboral.getEncabezado());
		respuesta.put("nombreencuesta", encLaboral.getNombreEncuesta());
		respuesta.put("version", encLaboral.getVersion());
		respuesta.put("idempleado", idempleado);
		return (respuesta.toJSONString());
	}

	public String obtenerEncLaboralDetalle(int idEncuesta) {
		JSONObject resTemp = new JSONObject();
		JSONArray respuesta = new JSONArray();
		ArrayList<EncuestaLaboralDetalle> encLaboralDetalle = EncuestaLaboralDetalleDAO
				.obtenerEncLaboralDetalle(idEncuesta);
		for (EncuestaLaboralDetalle detalle : encLaboralDetalle) {
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
		return (respuesta.toJSONString());
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

			// Primer recorrido: construir lista y (si aplica) calcular
			// porcentajeTransferido
			for (org.json.JSONObject item : empEncuestaDetalle) {
				String valorStr = item.optString("valor", "");
				float valor = item.optFloat("valor", 0);
				float porcentaje = item.optFloat("porcentaje", 0);

				EmpleadoEncuestaDetalle det = new EmpleadoEncuestaDetalle(0, item.getInt("idempleadoencuesta"),
						item.getInt("idencuestadetalle"), "", "", valorStr);
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
					if (Math.abs(valor) < 0.0001)
						continue;

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

	public String insertarEmpleadoEncuesta(EmpleadoEncuesta empEncuesta) {
		int idEmpleadoEncuesta = EmpleadoEncuestaDAO.insertarEmpleadoEncuesta(empEncuesta);
		JSONObject respuesta = new JSONObject();
		respuesta.put("idempleadoencuesta", idEmpleadoEncuesta);
		return (respuesta.toJSONString());
	}

	public String insertarEncuestaServicioWb(EncuestaServicio encuesta) {

	    JSONObject respuesta = new JSONObject();

	    boolean existe = EmpleadoEncuestaDAO.existeEncuestaPedido(
	    	    encuesta.getIdpedido(),
	    	    encuesta.getIdtienda()
	    	);

	    	if (existe) {
	    	    respuesta.put("success", false);
	    	    respuesta.put(
	    	        "message",
	    	        "Ya no es posible realizar esta acción nuevamente: "
	    	            + encuesta.getIdpedido()
	    	            + " - "
	    	            + encuesta.getIdtienda()
	    	    );

	    	    return respuesta.toJSONString();
	    	}

	    	existe = EmpleadoEncuestaDAO.insertarEncuestaServicio(encuesta);

	    if (existe) {

	        List<JSONObject> listaOpcionesPublica = new ArrayList<>();

	        List<JSONObject> listaOpcionesRuleta = RuletaDAO.ListaOpcionesRuleta();

	        for (JSONObject opcion : listaOpcionesRuleta) {

	            JSONObject publica = new JSONObject();

	            publica.put("index", opcion.get("indice"));
	            publica.put("title", opcion.get("titulo"));
	            publica.put("iterations", opcion.get("repeticiones"));

	            listaOpcionesPublica.add(publica);
	        }

	        respuesta.put("roulette_options", listaOpcionesPublica);

	        EmpleadoEncuestaDAO.insertarClienteServicio(encuesta);
	    }

	    respuesta.put("success", existe);

	    respuesta.put(
	        "message",
	        existe
	            ? "Encuesta insertada correctamente"
	            : "Error al insertar la encuesta"
	    );

	    return respuesta.toJSONString();
	}

	public static String resultadoRuleta(EncuestaServicio encuesta) {

	    List<JSONObject> opcionesRuleta = RuletaDAO.ListaOpcionesRuleta();
	    JSONObject respuesta = new JSONObject();

	    if (opcionesRuleta == null || opcionesRuleta.isEmpty()) {
	        respuesta.put("success", false);
	        respuesta.put("message", "No hay opciones disponibles para la ruleta");
	        return respuesta.toJSONString();
	    }

	    try {

	        // ===== PROBABILIDAD DE GANAR =====
	        int probabilidadGanar = ParametrosDAO.retornarValorNumerico("PROBABILIDADGANAR");// %

	        boolean gana = random.nextInt(100) < probabilidadGanar;

	        List<JSONObject> premios = new ArrayList<>();
	        List<JSONObject> sinPremio = new ArrayList<>();

	        for (JSONObject opcion : opcionesRuleta) {

	            int premioVal = opcion.get("premio") != null
	                    ? Integer.parseInt(opcion.get("premio").toString())
	                    : 0;

	            if (premioVal != 0) {
	                premios.add(opcion);
	            } else {
	                sinPremio.add(opcion);
	            }
	        }
	        
	        List<JSONObject> poolFinal;

	        if (gana && !premios.isEmpty()) {
	            poolFinal = premios;
	        } else {
	            poolFinal = sinPremio;
	        }
	        
	        if (poolFinal == null || poolFinal.isEmpty()) {
	            throw new RuntimeException("No hay opciones válidas para el resultado de la ruleta");
	        }

	        int indiceSeleccionado = random.nextInt(poolFinal.size());
	        JSONObject opcionSeleccionada = poolFinal.get(indiceSeleccionado);


	        int idOpcion = opcionSeleccionada.get("idopcion") != null
	                ? Integer.parseInt(opcionSeleccionada.get("idopcion").toString())
	                : 0;

	        int indice = opcionSeleccionada.get("indice") != null
	                ? Integer.parseInt(opcionSeleccionada.get("indice").toString())
	                : 0;

	        int premio = opcionSeleccionada.get("premio") != null
	                ? Integer.parseInt(opcionSeleccionada.get("premio").toString())
	                : 0;

	        int reintento = opcionSeleccionada.get("reintento") != null
	                ? Integer.parseInt(opcionSeleccionada.get("reintento").toString())
	                : 0;

	        String titulo = String.valueOf(opcionSeleccionada.get("titulo"));
	        String descripcion = String.valueOf(opcionSeleccionada.get("descripcion"));

	        Logger.getLogger("Ruleta").info(
	                "Resultado ruleta | opcion=" + idOpcion +
	                " | premio=" + premio +
	                " | titulo=" + titulo
	        );

	        int idregistro;

	        if (reintento == 1) {
	            idregistro = -1;
	        } else {
	           idregistro = RuletaDAO.registrarResultadoRuletaConToken(encuesta, idOpcion, null);
	        }

	        if (idregistro == 0) {
	            respuesta.put("success", false);
	            respuesta.put("message", "La solicitud ya no puede ser procesada.");
	            return respuesta.toJSONString();
	        }

	        respuesta.put("roulette", true);
	        respuesta.put("animation_index", indice);
	        respuesta.put("retry", reintento);
	        respuesta.put("description", descripcion);
	        respuesta.put("title", titulo);
	        respuesta.put("success", true);
	        respuesta.put("option_type", premio);
	        respuesta.put("message", "Resultado registrado exitosamente");

	        return respuesta.toJSONString();

	    } catch (Exception e) {

	        respuesta.put("success", false);
	        respuesta.put("message", "Error interno en el proceso de ruleta: " + e.getMessage());
	        System.out.println("Error: " + e.getMessage());

	        return respuesta.toJSONString();
	    }
	}
	
	
	public static void main(String args[])
	{


	}
	

	

	

}

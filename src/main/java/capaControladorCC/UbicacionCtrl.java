package capaControladorCC;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.QueryParameters.SpatialRelationship;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties.SurfacePlacement;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaDAOCC.ParametrosDAO;
import capaDAOCC.TiendaBloqueadaDAO;
import capaDAOPOS.TiendaDAO;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.Parametro;
import capaModeloCC.Resultado;
import capaModeloCC.Ubicacion;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import utilidadesCC.ControladorEnvioCorreo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class UbicacionCtrl {

	private static String API_KEY_HERE = "";
	private static String serviceUrl = "http://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";
	private static String queryUrl = "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0";

	public static Resultado ubicarDireccionEnTienda(String direccion, String municipio, String barrio,
			String tipo_cliente, String lead) {
		Resultado resultado = new Resultado();
		resultado.setLongitud(0);
		resultado.setLatitud(0);

		try {
			// 🔹 1. Limpiar la dirección
			String direccionLimpia = limpiarDireccion(direccion, municipio, barrio);
			//System.out.println(direccionLimpia);
			// 🔹 2. Inicializar API Key
			inicializarApiKey();

			// 🔹 3. Intentar geocodificar con ArcGIS primero
			JsonObject coords = obtenerCoordenadasDesdeArcGIS(direccionLimpia, municipio);
			if (esCoordenadaValida(coords)) {
				return crearResultadoConCoordenadas(coords, tipo_cliente, lead);
			}

			// System.out.println("Intento 2 con HERE...");
			// 🔹 4. Intentar geocodificar con HERE
			coords = HereGeocode(direccionLimpia, municipio);
			if (esCoordenadaValida(coords)) {
				return crearResultadoConCoordenadas(coords, tipo_cliente, lead);
			}
			// 🔹 5. Ningún proveedor encontró la dirección
			resultado.setSuccess(false);
			resultado.setResultado(
					"No se pudo ubicar la dirección con ArcGIS ni HERE. Verifica la ortografía o intenta con otra referencia cercana.");

		} catch (Exception e) {
			// 🔹 Manejo de errores
			System.err.println("Error al geocodificar: " + e.getMessage());
			e.printStackTrace();
			EnvioCorreo(lead, e.getMessage());

			resultado.setSuccess(false);
			resultado.setResultado("Error interno al intentar ubicar la dirección.");
		}

		return resultado;
	}

	/**
	 * Método auxiliar para crear el objeto Resultado a partir de coordenadas
	 * válidas
	 */
	private static Resultado crearResultadoConCoordenadas(JsonObject coords, String tipo_cliente, String lead) {
		double lat = coords.get("Latitud").getAsDouble();
		double lng = coords.get("Longitud").getAsDouble();
		String address = coords.get("Direccion").getAsString();

		// Crear resultado inicial y consultar zonas
		Resultado resultado = consultarZonas(crearPunto(coords), tipo_cliente, lead, queryUrl);

		// Actualizar coordenadas y dirección
		resultado.setLatitud(lat);
		resultado.setLongitud(lng);
		resultado.setDireccion(address);

		resultado.setSuccess(true);
		return resultado;
	}

	private static boolean esCoordenadaValida(JsonObject coords) {
		if (coords == null)
			return false;
		double lat = coords.get("Latitud").getAsDouble();
		double lng = coords.get("Longitud").getAsDouble();
		return !(lat == 0 && lng == 0);
	}

	private static Point crearPunto(JsonObject coords) {
		double lat = coords.get("Latitud").getAsDouble();
		double lng = coords.get("Longitud").getAsDouble();
		return new Point(lng, lat, SpatialReferences.getWgs84());
	}

	private static void inicializarApiKey() throws Exception {
		Parametro parametro = ParametrosDAO.obtenerParametro("APIARCGIS");
		ArcGISRuntimeEnvironment.setApiKey(parametro.getValorTexto());
		parametro = ParametrosDAO.obtenerParametro("APIHEREMAPS");
		API_KEY_HERE = parametro.getValorTexto();
	}

	private static JsonObject obtenerCoordenadasDesdeArcGIS(String direccion, String municipioPedido) throws Exception {

		JsonObject coords = new JsonObject();
		coords.addProperty("Longitud", 0);
		coords.addProperty("Latitud", 0);
		coords.addProperty("Direccion", "");

		LocatorTask locatorTask = new LocatorTask(serviceUrl);
		GeocodeParameters geocodeParameters = new GeocodeParameters();
		geocodeParameters.setMaxResults(5); // ✅ hasta 5 resultados
		geocodeParameters.setCountryCode("COL");
		geocodeParameters.getResultAttributeNames().add("*");

		ListenableFuture<List<GeocodeResult>> future = locatorTask.geocodeAsync(direccion, geocodeParameters);
		List<GeocodeResult> geocodeResults = future.get();

		if (geocodeResults != null && !geocodeResults.isEmpty()) {
			JsonObject mejor = null;

			for (GeocodeResult geocodeResult : geocodeResults) {
				String address = geocodeResult.getLabel();
				Map<String, Object> atributos = geocodeResult.getAttributes();
				//System.out.println("atributos:" + atributos);
				//System.out.println("➡ Label Arcgis: " + address);

				String municipioArcgis = atributos.containsKey("City") ? atributos.get("City").toString().toLowerCase()
						: "";
				String districtArcgis = atributos.containsKey("District")
						? atributos.get("District").toString().toLowerCase()
						: "";

				String municipioPedidoNorm = normalizarTexto(municipioPedido);
				String municipioArcgisNorm = normalizarTexto(municipioArcgis);
				String districtArcgisNom = normalizarTexto(districtArcgis);

				JsonObject attExtra = new JsonObject();

				attExtra.addProperty("municipioPedido", municipioPedidoNorm);
				attExtra.addProperty("municipioApi", municipioArcgisNorm);
				attExtra.addProperty("districtApi", districtArcgisNom);

				Point displayLocation = geocodeResult.getDisplayLocation();
				 //System.out.println("➡ coord: " + displayLocation.getY() + "," +
				// displayLocation.getX());
				String direccionParaValidar = direccion.replaceAll(",\\s*Antioquia,\\s*Colombia$", "");
				// System.out.println(direccionParaValidar);
				if (esCoincidencia(direccionParaValidar, address, attExtra)) {
					mejor = new JsonObject();

					mejor.addProperty("Longitud", displayLocation.getX());
					mejor.addProperty("Latitud", displayLocation.getY());
					mejor.addProperty("Direccion", address);

					// System.out.println("✔ Dirección aceptada: " + address);
					break; // ✅ nos quedamos con la primera coincidencia válida
				}
			}

			if (mejor != null) {
				return mejor;
			} else {
				coords.addProperty("error", "No se encontraron resultados exactos");
				return coords;
			}
		}

		coords.addProperty("error", "No se encontraron resultados");
		return coords;
	}

	private static Resultado consultarZonas(Point punto, String tipo_cliente, String lead, String queryUrl) {
		Resultado resultado = new Resultado();
		ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(queryUrl);
		QueryParameters query = new QueryParameters();
		query.setGeometry(punto);
		query.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);

		try {
			FeatureQueryResult result = serviceFeatureTable
					.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL).get();

			if (result != null && result.iterator().hasNext()) {
				for (Feature feature : result) {
					Map<String, Object> attributes = feature.getAttributes();
					String nombre = attributes.get("nombre").toString();

					String mensaje = switch (tipo_cliente.toLowerCase()) {
					case "informacion" -> "Tu direccion se encuentra dentro de nuestra cobertura de la tienda " + nombre
							+ ", te invitamos a que sigas con tu pedido. Recuerda que toda la informacion es validada al final por nuestros asesores";
					case "programado" ->
						"Tu direccion se encuentra dentro de la cobertura de nuestras tiendas. El pedido esta siendo programado para la tienda "
								+ nombre;
					default -> "Tu direccion se encuentra dentro de la cobertura de nuestra tienda " + nombre;
					};
					resultado.setResultado(mensaje);
					resultado.setInfoAdicional(nombre);

					int idTienda = capaDAOCC.TiendaDAO.obteneridTienda(nombre);
					resultado.setEstadoTienda(
							TiendaBloqueadaDAO.validarTiendaBloqueada(idTienda) ? "BLOQUEADO" : "DISPONIBLE");
					resultado.setSuccess(true);
					break;
				}
			} else {
				resultado.setResultado(
						"Por el momento tu direccion no se encuentra dentro de la cobertura de domicilio de nuestras tiendas. Te invitamos a que te comuniques a nuestra linea telefonica 604 4444553 o vuelve a intentar.");
				resultado.setSuccess(false);
			}
		} catch (Exception e) {
			System.out.println("Error al realizar la consulta: " + e.getMessage());
			EnvioCorreo(lead, e.getMessage());
			resultado.setSuccess(false);
		}

		return resultado;
	}

	public Ubicacion ubicarDireccionEnTiendaBatch(String direccion, String municipio, String barrio) {
		String direccionLimpia = limpiarDireccion(direccion, municipio, barrio);
		Ubicacion ubicacion = new Ubicacion(0, 0);
		try {
			inicializarApiKey();
			// 🔹 1. Intentar con HERE
			JsonObject coords = obtenerCoordenadasDesdeArcGIS(direccionLimpia, municipio);
			if (esCoordenadaValida(coords)) {
				double lat = coords.get("Latitud").getAsDouble();
				double lng = coords.get("Longitud").getAsDouble();

				ubicacion.setLatitud(lat);
				ubicacion.setLongitud(lng);
				return ubicacion;
			}

			// 🔹 2. Intentar con ArcGIS
			coords = HereGeocode(direccionLimpia, municipio);
			if (esCoordenadaValida(coords)) {
				double lat = coords.get("Latitud").getAsDouble();
				double lng = coords.get("Longitud").getAsDouble();

				ubicacion.setLatitud(lat);
				ubicacion.setLongitud(lng);
				return ubicacion;

			}

		} catch (Exception e) {
			System.out.println("Error al geocodificar: " + e.getMessage());
		}

		return ubicacion;
	}

	public static void EnvioCorreo(String lead, String error) {
		Correo correo = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
		ArrayList correos = new ArrayList();
		correo.setAsunto("TENEMOS PROBLEMA CON LA API ARCGIS");
		String correoEle = "jubote1@gmail.com";
		correos.add(correoEle);
		correos.add("a.desarrollosi@gmail.com");
		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		String mensCorreo = "Se encontro un problema con la invocación de la API ARCGIS  " + error;
		String leadInfo = (lead == null || lead.trim().isEmpty()) ? "SIN LEAD" : lead;
		mensCorreo += "<br> Numero de Lead: " + leadInfo;
		correo.setMensaje(mensCorreo);
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();

	}

	public static void main(String[] args) {
		String direccion = "Calle 41Dsur # 58-19 apartamento 101";
		String Barrio = "Unidad abierta Vegas de Alcalá, Pradito, detrás de la estación de bomberos, bajando las escalas frente al parque infantil";
		String Municipio = " San antonio de prado";
		String txt = direccion + ", " + Municipio + ", " + Barrio;
		Resultado resultado = ubicarDireccionEnTienda(direccion, Municipio, Barrio, "informacion", null);
		System.out.println(resultado.getResultado());

	}

	public static String limpiarDireccion(String direccion, String municipio, String barrio) {
		// 1. Quitar emojis y caracteres raros
		direccion = (direccion == null ? "" : direccion).replaceAll("[^\\p{L}\\p{N}\\s,\\.\\#\\-]", " ").trim();
		barrio = (barrio == null ? "" : barrio).replaceAll("[^\\p{L}\\p{N}\\s,\\.\\#\\-]", " ").trim();
		municipio = (municipio == null ? "" : municipio).replaceAll("[^\\p{L}\\p{N}\\s,\\.\\#\\-]", " ").trim();

		// 2. Cortar en la primera coma
		int comaDir = direccion.indexOf(",");
		if (comaDir > 0)
			direccion = direccion.substring(0, comaDir);

		// 3. Limitar a 12 palabras
		String[] partes = direccion.split("\\s+");
		if (partes.length > 14) {
			direccion = String.join(" ", Arrays.copyOfRange(partes, 0, 14));
		}

		int comaBarrio = barrio.indexOf(",");
		if (comaBarrio > 0)
			barrio = barrio.substring(0, comaBarrio);

		// 4. Concatenar evitando comas vacías
		StringBuilder sb = new StringBuilder();
		sb.append(direccion);
		if (!municipio.isBlank())
			sb.append(", ").append(municipio);
		if (!barrio.isBlank())
			sb.append(", ").append(barrio);
		
		

		String completa = sb.toString();
		
		String[] partesDireccionComp = completa.split("\\s+");
		if (partesDireccionComp.length > 15) {
		    completa = String.join(" ", Arrays.copyOfRange(partesDireccionComp, 0, 15));
		}


		// 5. Quitar tildes y acentos
		completa = Normalizer.normalize(completa, Normalizer.Form.NFD);
		completa = completa.replaceAll("\\p{M}", "");

		// 6. Normalizar bloque después de '#'
		completa = completa.replaceAll("#\\s*(\\d{1,3})\\s*-\\s*(\\d{1,3})\\b", "# $1 $2");
		completa = completa.replaceAll("#\\s*(\\d{2})(\\d{2})\\b", "# $1 $2");
		completa = completa.replaceAll("#\\s*(\\d{3})(\\d{2})\\b", "# $1 $2");
		completa = completa.replace("#", " ");

		// 7. Pasar todo a minúsculas
		completa = completa.toLowerCase();

		// 8. Normalizar abreviaturas
		completa = completa.replaceAll("\\bcrra\\b|\\bcrr\\b|\\bcra\\b|\\bcr\\b", "carrera");
		completa = completa.replaceAll("\\bcll\\b|\\bcalle\\b", "calle");
		completa = completa.replaceAll("\\bav\\b|\\bavenida\\b", "avenida");
		completa = completa.replaceAll("\\bint\\b|\\binterior\\b", "interior");
		completa = completa.replaceAll("\\bapto\\b|\\bap\\b|\\bapartamento\\b", "apartamento");
		completa = completa.replaceAll("\\b(?i)(número|numero|no\\.?|nro\\.?|num\\.?|núm\\.?)\\b", " ");

		// 1. Separar tipo de vía de número
		completa = completa.replaceAll("\\b(calle|carrera|avenida)\\s*(\\d+)", "$1 $2");

		// 2. Separar número + letra (no cardinal)
		completa = completa.replaceAll("(\\d+)([a-df-zA-DF-Z])(?!este|sur|norte|oeste)", "$1 $2");

		// 1. Separar número de letra (no cardinal)
		completa = completa.replaceAll("(\\d+)([A-Za-z])(?!\\s|este|sur|norte|oeste)", "$1 $2");

		// 2. Separar letra de cardinal
		completa = completa.replaceAll("([A-Za-z])(?=(este|sur|norte|oeste))", "$1 ");

		// 4. Normalizar cardinales sueltos
		completa = completa.replaceAll("\\bs\\b", "sur");
		completa = completa.replaceAll("\\bn\\b", "norte");
		completa = completa.replaceAll("\\be\\b", "este");
		completa = completa.replaceAll("\\bo\\b", "oeste");
		
		

		completa += ", Antioquia, Colombia";
		// 10. Quitar espacios múltiples
		completa = completa.replaceAll("\\s+", " ").trim();

		return completa;
	}

	public static JsonObject HereGeocode(String direccion, String municipioPedido) {

		String url = "https://geocode.search.hereapi.com/v1/geocode?q="
				+ java.net.URLEncoder.encode(direccion, java.nio.charset.StandardCharsets.UTF_8)
				+ "&lang=es&in=countryCode:COL&limit=5&apiKey=" + API_KEY_HERE;

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		JsonObject coords = new JsonObject();
		coords.addProperty("Longitud", 0);
		coords.addProperty("Latitud", 0);
		coords.addProperty("Direccion", "");

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

				if (json.has("items") && json.getAsJsonArray("items").size() > 0) {
					JsonObject mejor = null;

					for (JsonElement elem : json.getAsJsonArray("items")) {
						JsonObject item = elem.getAsJsonObject();
						//System.out.println("item:" + item);
						JsonObject addr = item.getAsJsonObject("address");
						String label = addr.get("label").getAsString();
						//System.out.println("➡ Label Here: " + label);

						String municipioHere = addr.has("city") ? addr.get("city").getAsString() : "";
						String districtHere = addr.has("district") ? addr.get("district").getAsString() : "";

						JsonObject attExtra = new JsonObject();

						String municipioPedidoNorm = normalizarTexto(municipioPedido);
						String municipioHereNorm = normalizarTexto(municipioHere);
						String districtHereNom = normalizarTexto(districtHere);

						attExtra.addProperty("municipioPedido", municipioPedidoNorm);
						attExtra.addProperty("municipioApi", municipioHereNorm);
						attExtra.addProperty("districtApi", districtHereNom);

						// System.out.println(
						// "➡ coord: " + pos.get("lat").getAsDouble() + "," +
						// pos.get("lng").getAsDouble());
						String direccionParaValidar = direccion.replaceAll(",\\s*Antioquia,\\s*Colombia$", "");
						// System.out.println(direccionParaValidar);
						if (esCoincidencia(direccionParaValidar, label, attExtra)) {

							mejor = item;
							break;
						}

					}

					if (mejor != null) {
						JsonObject pos = mejor.getAsJsonObject("position");
						JsonObject addr = mejor.getAsJsonObject("address");

						coords.addProperty("Latitud", pos.get("lat").getAsDouble());
						coords.addProperty("Longitud", pos.get("lng").getAsDouble());
						coords.addProperty("Direccion", addr.get("label").getAsString());

						// System.out.println("✔ Match exacto: " + addr.get("label").getAsString());
					} else {
						coords.addProperty("error", "No se encontraron resultados exactos");

					}

				} else {
					coords.addProperty("error", "No se encontraron resultados");
				}
			} else {
				coords.addProperty("error", "Error en la petición geocode: " + response.statusCode());
			}

		} catch (Exception e) {
			System.out.println("No se pudo geocodificar la dirección.");
			e.printStackTrace();
		}

		return coords;
	}

	private static boolean esCoincidencia(String query, String label, JsonObject extra) {
	    String q = normalizarTexto(query);
	    String l = normalizarTexto(label);
	    
	    String m = normalizarTexto(extra.get("municipioPedido").getAsString());
	    String m_a = normalizarTexto(extra.get("municipioApi").getAsString());
	    String d_a = normalizarTexto(extra.get("districtApi").getAsString());

	    if (!m.isEmpty()) {
	        if (!l.contains(m) && !m_a.contains(m) && !d_a.contains(m)) {
	            return false; // ❌ no pertenece al municipio correcto
	        }
	    }

	    String[] palabrasQ = q.split(" ");
	    String[] palabrasL = l.split(" ");

	    int limite = Math.min(5, Math.min(palabrasQ.length, palabrasL.length));
	    int coincidenciasInicio = 0;

	    // ✅ Verificar primeras palabras
	    for (int i = 0; i < limite; i++) {
	        if (palabrasQ[i].equals(palabrasL[i])) {
	            coincidenciasInicio++;
	        } else {
	            break; // si se rompe en el inicio, dejamos de contar
	        }
	    }

	    // Si las 3 primeras palabras seguidas coinciden → aceptar
	    if (coincidenciasInicio >= 3) {
	        return true;
	    }

	    // 🔄 Plan B: coincidencia por porcentaje
	    int total = palabrasQ.length;
	    int match = 0;
	    for (String p : palabrasQ) {
	        if (l.contains(p)) {
	            match++;
	        }
	    }

	    double ratio = (double) match / total;

	    // ⚡ Ajuste: si el query contiene números, exigir más coincidencia
	    boolean tieneNumeros = q.matches(".*\\d+.*");
	    double umbral = tieneNumeros ? 0.8 : 0.65;

	    return ratio >= umbral;
	}


	private static String normalizarTexto(String txt) {
		if (txt == null)
			return "";
		txt = Normalizer.normalize(txt, Normalizer.Form.NFD);
		txt = txt.replaceAll("\\p{M}", ""); // ✅ elimina tildes
		txt = txt.toLowerCase();
		txt = txt.replaceAll("-", " "); // ✅ unificar guiones
		txt = txt.replaceAll("\\s+", " "); // ✅ colapsar espacios
		txt = txt.replaceAll("(?<=\\d)(?=[a-zA-Z])", " ");
		// 8. Normalizar cardinales (si están separados)
		txt = txt.replaceAll("\\b(\\d+)\\s+e\\b", "$1 este");
		txt = txt.replaceAll("\\b(\\d+)\\s+o\\b", "$1 oeste");
		txt = txt.replaceAll("\\b(\\d+)\\s+n\\b", "$1 norte");
		txt = txt.replaceAll("\\b(\\d+)\\s+s\\b", "$1 sur");

		// 9. Unir número y letra si NO son cardinales
		txt = txt.replaceAll("\\b(\\d+)\\s+([a-df-mp-zA-DF-MP-Z]{1,2})\\b", "$1$2");

		return txt.trim();
	}

}
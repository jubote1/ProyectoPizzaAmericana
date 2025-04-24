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
import java.util.concurrent.ExecutionException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class UbicacionCtrl {

	public static Resultado ubicarDireccionEnTienda(String direccion, String tipo_cliente, String lead) {
		Resultado resultado = new Resultado();
		try {
			inicializarApiKeyArcGIS();

			String serviceUrl = "http://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";
			String queryUrl = "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0";

			Point punto = obtenerCoordenadasDesdeArcGIS(direccion, serviceUrl);
			if (punto != null) {
				resultado = consultarZonas(punto, tipo_cliente, lead, queryUrl);
			}

			// Si ArcGIS falla o no tiene cobertura
			if (!resultado.isSuccess()) {
				JsonObject coords = obtenerCoordenadasGMps(direccion);
				double lat = coords.get("Latitud").getAsDouble();
				double lng = coords.get("Longitud").getAsDouble();
				resultado = consultarZonas(new Point(lng, lat, SpatialReferences.getWgs84()), tipo_cliente, lead,
						queryUrl);
			}
			


		} catch (Exception e) {
			System.out.println("Error al geocodificar: " + e.getMessage());
			EnvioCorreo(lead, e.getMessage());
		}
		return resultado;
	}

	private static void inicializarApiKeyArcGIS() throws Exception {
		Parametro parametro = ParametrosDAO.obtenerParametro("APIARCGIS");
		//System.out.println(parametro.getValorTexto());
		//ArcGISRuntimeEnvironment.setApiKey("AAPK8f44b53988ec4457b8d7cebe2d9ca927gC2JymB5EkSC3Gt71rGCqWdnJqkR1hhou3JvG83zGpZm-dnA59DqJiwzOGIeor7t");
		ArcGISRuntimeEnvironment.setApiKey(parametro.getValorTexto());
	}

	private static Point obtenerCoordenadasDesdeArcGIS(String direccion, String serviceUrl) throws Exception {
		LocatorTask locatorTask = new LocatorTask(serviceUrl);
		GeocodeParameters geocodeParameters = new GeocodeParameters();
		geocodeParameters.getResultAttributeNames().add("*");
		geocodeParameters.setMaxResults(1);

		ListenableFuture<List<GeocodeResult>> future = locatorTask.geocodeAsync(direccion, geocodeParameters);
		List<GeocodeResult> geocodeResults = future.get();

		if (geocodeResults != null && !geocodeResults.isEmpty()) {
			GeocodeResult geocodeResult = geocodeResults.get(0);
			Point displayLocation = geocodeResult.getDisplayLocation();
			double latitude = displayLocation.getY();
			double longitude = displayLocation.getX();

			return displayLocation;
		}
		return null;
	}

	private static Resultado consultarZonas(Point punto, String tipo_cliente, String lead, String queryUrl) {
		Resultado resultado = new Resultado();
		ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(queryUrl);
		QueryParameters query = new QueryParameters();
		query.setGeometry(punto);
		query.setSpatialRelationship(QueryParameters.SpatialRelationship.WITHIN);

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

	public Ubicacion ubicarDireccionEnTiendaBatch(String direccion) {
		Ubicacion ubica = new Ubicacion(0, 0);
		try {
			Parametro parametro;
			parametro = ParametrosDAO.obtenerParametro("APIARCGIS");
			System.out.println(parametro.getValorTexto());
			System.out.println("2.1 ANTES DE INICIAR ");
			ArcGISRuntimeEnvironment.setApiKey(parametro.getValorTexto());
			System.out.println("2.2 ANTES DE INICIAR ");
			String serviceUrl = "http://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";
			String queryUrl = "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0";
			LocatorTask locatorTask = new LocatorTask(serviceUrl);
			GeocodeParameters geocodeParameters = new GeocodeParameters();
			geocodeParameters.getResultAttributeNames().add("*");
			geocodeParameters.setMaxResults(1); // Limitamos el número de resultados para obtener solo el más relevante
			ListenableFuture<List<GeocodeResult>> future = locatorTask.geocodeAsync(direccion, geocodeParameters);
			List<GeocodeResult> geocodeResults = future.get();

			if (geocodeResults != null && !geocodeResults.isEmpty()) {
				GeocodeResult geocodeResult = geocodeResults.get(0);
				Point displayLocation = geocodeResult.getDisplayLocation();
				double latitude = displayLocation.getY();
				double longitude = displayLocation.getX();

				ubica.setLatitud(latitude);
				ubica.setLongitud(longitude);

//                ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(queryUrl);
//
//                Point point = new Point(longitude, latitude, SpatialReferences.getWgs84());
//             // Transforma las coordenadas al sistema de coordenadas de la capa de entidades (si es diferente)
//                QueryParameters query = new QueryParameters();
//                query.setGeometry(point); // Establece la geometría de la consulta (en este caso, un punto)
//                query.setSpatialRelationship(QueryParameters.SpatialRelationship.WITHIN);
// 
//               
//                    try {
//                        FeatureQueryResult result = serviceFeatureTable.queryFeaturesAsync(query,ServiceFeatureTable.QueryFeatureFields.LOAD_ALL).get();
//
//                        // Verifica si hay resultados en la consulta
//                        if (result.iterator().hasNext()) {
//                            // El punto se encuentra dentro de al menos uno de los polígonos en la capa
//                            System.out.println("El punto está dentro de al menos uno de los polígonos en la capa.");
//                            Geometry pointGeometry = GeometryEngine.project(point, SpatialReferences.getWgs84());
//
//                            // Itera sobre los resultados de la consulta
//                            for (Feature feature : result) {
//                                // Obten la geometría de la característica (polígono)
//                            	  Geometry polygonGeometry = GeometryEngine.project(feature.getGeometry(), SpatialReferences.getWgs84());
//                            	  Map<String, Object> attributes = feature.getAttributes();
//                            	  Object nombre = attributes.get("nombre");
//                            	  System.out.println(nombre);
//                            	  resultado.setResultado("Tu dirección se encuentra dentro de nuestra cobertura de la tienda " + nombre.toString() + ", te invitamos a que sigas con tu pedido");  
//                            }
//                        
//                        } else {
//                            // El punto no se encuentra dentro de ningún polígono en la capa
//                            System.out.println("El punto no se encuentra dentro de ningún polígono en la capa.");
//                            resultado.setResultado("Por el momento tu dirección no se encuentra dentro de la cobertura de domicilio de nuestras tiendas, te invitamos a que te acerques a nuestro punto de venta más cercano para que puedas realizar tu pedido.");
//                        }
//                    } catch (Exception e) {
//                        System.out.println("Error al realizar la consulta: " + e.getMessage());
//                        //Enviaremos correo para notificar que hay problema con la API
//                        Correo correo = new Correo();
//        				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
//        				ArrayList correos = new ArrayList();
//        				correo.setAsunto("TENEMOS PROBLEMA CON LA API ARCGIS  ");
//        				String correoEle = "jubote1@gmail.com";
//        				correos.add(correoEle);
//        				correo.setContrasena(infoCorreo.getClaveCorreo());
//        				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
//        				correo.setMensaje(" Se tiene prolema con la invocación de la API ARCGIS  " + e.getMessage());
//        				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
//        				contro.enviarCorreo();
//                    }

			} else {
				System.out.println("No se pudo geocodificar la dirección.");
			}
		} catch (Exception e) {
			System.out.println("Error al geocodificar: " + e.getMessage());

		}
		return (ubica);
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
		if (lead != null || !lead.isEmpty()) {
			mensCorreo = mensCorreo + "<br> Numero de Lead: " + lead;
		}
		correo.setMensaje(mensCorreo);
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();

	}

	public static JsonObject obtenerCoordenadasGMps(String direccion) {
		OkHttpClient client = new OkHttpClient();
		JsonObject coords = new JsonObject(); // Por defecto (coordenadas inválidas)
		coords.addProperty("Longitud", 0);
		coords.addProperty("Latitud", 0);
		try {
			Parametro param = ParametrosDAO.obtenerParametro("APIKEYGOOGLEMPS");
			String apiKey = param.getValorTexto();
			String direccionEncoded = URLEncoder.encode(direccion, "UTF-8");
			String url = String.format(
					"https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s&location_type=APPROXIMATE",
					direccionEncoded, apiKey);

			Request request = new Request.Builder().url(url).get().build();

			try (Response response = client.newCall(request).execute()) {
		
				if (!response.isSuccessful()) {
					return coords;
				}

				String jsonResponse = response.body().string();
				JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

				String status = json.get("status").getAsString();
				if (!"OK".equals(status)) {
					return coords;
				}

				JsonArray results = json.getAsJsonArray("results");
				if (results.size() > 0) {
					JsonObject location = results.get(0).getAsJsonObject().get("geometry").getAsJsonObject()
							.get("location").getAsJsonObject();

					double lat = location.get("lat").getAsDouble();
					double lng = location.get("lng").getAsDouble();

					coords.addProperty("Longitud", lng);
					coords.addProperty("Latitud", lat);
				}
			}
		} catch (Exception e) {
			System.out.println("Error con api de google Maps: " + e.getMessage());
		}

		return coords;
	}

	public static void main(String[] args) {
		Resultado resultado = ubicarDireccionEnTienda("Calle 54# 86c-666, calazans, Medellin", "informacion", null);
		System.out.println(resultado.getResultado());
	}
}
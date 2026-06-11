package capaControladorCC;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaDAOCC.ParametrosDAO;
import capaDAOCC.TiendaBloqueadaDAO;
import capaModeloCC.Cliente;
import capaModeloCC.CoberturaRequest;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.Parametro;
import capaModeloCC.Resultado;
import capaModeloCC.Ubicacion;
import utilidadesCC.ControladorEnvioCorreo;
import capaDAOCC.ClienteDAO;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UbicacionCtrl {

	private static String API_KEY_HERE = "";
	private static String API_KEY_ARCGIS = "";
	private static String API_KEY_GOOGLE = "";
	private static String serviceUrl = "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";
	private static String queryUrl = "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0";

	private static final String DEPARTAMENTO = "Antioquia";
	private static final String PAIS = "Colombia";
	private static final String TOKEN_NUMERO_DIRECCION =
	        "\\d+\\s*[a-z]{0,2}(?:\\s*(?:sur|norte|este|oeste))?";

	private static final Pattern PATRON_TIPO_VIA = Pattern
			.compile("\\b(calle|carrera|avenida|diagonal|transversal|circular|autopista)\\b");

	private static final Pattern PATRON_COMPLEMENTO = Pattern.compile(
			"\\b(apartamento|apto|ap|interior|int|torre|bloque|piso|local|oficina|casa|unidad|urbanizacion|urb|edificio|ed)\\b\\s*[a-z0-9\\-]*\\s*[a-z0-9\\-]*");

	
	private static final Pattern PATRON_NOMENCLATURA = Pattern.compile(
	        "\\b(calle|carrera|avenida|diagonal|transversal|circular|autopista)\\s+"
	                + "(" + TOKEN_NUMERO_DIRECCION + ")\\s*"
	                + "(?:#|numero|no|nro|num)?\\s*"
	                + "(" + TOKEN_NUMERO_DIRECCION + ")\\s*(?:-|\\s+)\\s*"
	                + "(" + TOKEN_NUMERO_DIRECCION + ")\\b");

	private static final Pattern PATRON_NOMENCLATURA_CON_CRUCE = Pattern.compile(
	        "\\b(calle|carrera|avenida|diagonal|transversal|circular|autopista)\\s+"
	                + "(" + TOKEN_NUMERO_DIRECCION + ")\\s+"
	                + "(?:calle|carrera|avenida|diagonal|transversal|circular|autopista)\\s+"
	                + "(" + TOKEN_NUMERO_DIRECCION + ")\\s*(?:-|\\s+)\\s*"
	                + "(" + TOKEN_NUMERO_DIRECCION + ")\\b");
	
	private static final String[] MUNICIPIOS_SOPORTADOS = {
	        "medellin", "bello", "itagui", "envigado", "sabaneta", "la estrella", "caldas",
	        "copacabana", "girardota", "barbosa", "rionegro", "marinilla", "la ceja",
	        "guarne", "el retiro", "carmen de viboral", "santuario", "apartado"
	};
	
	/**
	 * Valida la cobertura de una direccion.
	 *
	 * Flujo:
	 * 1. Si llegan coordenadas validas, idTienda y direccionProveedor confiable,
	 *    intenta validar directamente por coordenadas para evitar geocodificacion.
	 * 2. Si las coordenadas no se pueden usar, no coinciden o no dan cobertura,
	 *    continua con el flujo normal: geocodificar la direccion y luego consultar zona.
	 *
	 * Este metodo conserva el flujo anterior cuando solo llega direccion.
	 */
	public static Resultado ubicarDireccionEnTienda(CoberturaRequest coberturaRequest) {

	    String direccion = coberturaRequest.getDireccion();
	    String municipio = coberturaRequest.getMunicipio();
	    String barrio = coberturaRequest.getBarrio();
	    String tipo_cliente = coberturaRequest.getTipoCliente();
	    String lead = coberturaRequest.getLead();
	    Integer idcliente = coberturaRequest.getIdcliente();
	    String telefono = coberturaRequest.getTelefono();
	    String referencia  = "";
	    
	    System.out.println("===== INICIO =====");
	    System.out.println("idcliente=" + idcliente);
	    System.out.println("direccion=" + direccion);
	    System.out.println("telefono=" + telefono);
	  

	    Resultado resultado = new Resultado();
	    resultado.setLongitud(0);
	    resultado.setLatitud(0);

	    try {
	        inicializarApiKey();
	        
	     // Camino rapido: si las coordenadas vienen respaldadas por una direccionProveedor
	     // que coincide con la direccion del cliente, se consulta cobertura sin geocodificar.
	        
	        Cliente cliente = null;
	        String barrioBD   = "";
		    String municipioBD  = "";
		    
	        if (idcliente != null && idcliente > 0) {

	        	Cliente clienteTmp = ClienteDAO.obtenerClienteporID(idcliente);

	        	if (clienteTmp != null && clienteTmp.getIdcliente() > 0) {
	        	    cliente = clienteTmp;
	        	}

	        } else if (telefono != null && !telefono.isEmpty()) {

	            cliente = ClienteDAO.obtenerUltimoClientePorTelefono(telefono);
	        }
	        System.out.println("cliente encontrado=" + (cliente != null));
	        if (cliente != null) {
	        	
	            System.out.println("direccion cliente=" + cliente.getDireccion());
	            System.out.println("latitud cliente=" + cliente.getLatitud());
	            System.out.println("longitud cliente=" + cliente.getLontitud());
	            System.out.println("idtienda cliente=" + cliente.getIdtienda());
	            
	            idcliente = cliente.getIdcliente();

	            coberturaRequest.setDireccionProveedor(cliente.getDireccionProveedor());
	            coberturaRequest.setLatitud((double) cliente.getLatitud());
	            coberturaRequest.setLongitud((double) cliente.getLontitud());
	            coberturaRequest.setIdTienda(cliente.getIdtienda());
	            direccion = cliente.getDireccion();
	            coberturaRequest.setDireccion(direccion);
	            referencia = cliente.getObservacion();
	            barrioBD = cliente.getZonaDireccion();
	            municipioBD = cliente.getMunicipio();
	     
	        }
	        
	        System.out.println("puedeValidarPorCoordenadas="
	                + puedeValidarPorCoordenadas(coberturaRequest));
	        if (idcliente != null && idcliente > 0
	                && puedeValidarPorCoordenadas(coberturaRequest)) { 

	            Resultado resultadoCoordenadas = consultarZonas(
	                    coberturaRequest.getLongitud(),
	                    coberturaRequest.getLatitud(),
	                    tipo_cliente,
	                    lead,
	                    direccion,
	                    queryUrl,
	                    coberturaRequest.getIdTienda()
	            );
	            
	            System.out.println("consultarZonas success="
	                    + resultadoCoordenadas.isSuccess());

	            System.out.println("consultarZonas resultado="
	                    + resultadoCoordenadas.getResultado());

	            resultadoCoordenadas.setLatitud(coberturaRequest.getLatitud());
	            resultadoCoordenadas.setLongitud(coberturaRequest.getLongitud());
	            resultadoCoordenadas.setDireccion(direccion);
	            resultadoCoordenadas.setReferencia(referencia);
	            resultadoCoordenadas.setBarrio(barrioBD);
	            resultadoCoordenadas.setMunicipioOriginal(municipioBD);

	            if (resultadoCoordenadas.isSuccess()) {
	                return resultadoCoordenadas;
	            }
	        }


	        
	     // Flujo normal: se usa cuando no hay coordenadas confiables o cuando la
	     // validacion por coordenadas no confirma cobertura para la tienda esperada.
	        
	        if (direccion != null && !direccion.trim().isEmpty()) {
	        	
	            DireccionNormalizada direccionNormalizada = normalizarDireccionEntrega(direccion, municipio, barrio);

		        JsonObject coords = geocodificarConCandidatos(direccionNormalizada, direccion, municipio, barrio);
		        if (esCoordenadaValida(coords)) {

		            Resultado resultadoCoordenadas = crearResultadoConCoordenadas(
		                    coords,
		                    tipo_cliente,
		                    lead,
		                    direccion
		            );

		            if (idcliente != null
		                    && idcliente > 0
		                    && resultadoCoordenadas.isSuccess()) {

		                new ClienteCtrl().actualizarClienteCoordenadas(
		                        idcliente,
		                        (float) resultadoCoordenadas.getLatitud(),
		                        (float) resultadoCoordenadas.getLongitud(),
		                        resultadoCoordenadas.getDireccion()
		                );
		            }

		            return resultadoCoordenadas;
		        }
		        
		        if (!direccionNormalizada.esPuntual) {
		            resultado.setResultado(
		                    "No pudimos validar la direccion exacta. Por favor confirma la nomenclatura, numero de placa o comparte una referencia mas precisa.");
		        } else {
		            resultado.setResultado(
		                    "No se pudo validar la direccion exacta con los proveedores disponibles. Verifica la nomenclatura, municipio o barrio.");
		        }

	        }else {
	        	
	        	resultado.setResultado("No se proporciono una dirección valida.");
	        }
	        	
	        resultado.setSuccess(false);

	      

	    } catch (Exception e) {
	        System.err.println("Error al geocodificar: " + e.getMessage());
	        e.printStackTrace();
	        EnvioCorreo(direccion, lead, e.getMessage());

	        resultado.setSuccess(false);
	        resultado.setResultado("Error interno al intentar ubicar la direccion.");
	    }

	    return resultado;
	}

	
	private static JsonObject geocodificarConCandidatosSinSugerencias(DireccionNormalizada direccionNormalizada,
	        String direccion, String municipio, String barrio) throws Exception {
		Set<String> candidatos = new LinkedHashSet<>();

		candidatos.addAll(direccionNormalizada.candidatosPuntuales);

		String direccionLegado = limpiarDireccionLegadoMejorada(direccion, municipio, barrio);
		if (!direccionLegado.isBlank()) {
			candidatos.add(direccionLegado);
		}

		String municipioValidacion = direccionNormalizada.municipio == null || direccionNormalizada.municipio.isBlank()
				? municipio
				: direccionNormalizada.municipio;

		for (String candidato : candidatos) {
		    JsonObject coords = obtenerCoordenadasDesdeArcGIS(candidato, municipioValidacion);
		    if (esCoordenadaValida(coords)) return coords;

		    coords = HereGeocode(candidato, municipioValidacion);
		    if (esCoordenadaValida(coords)) return coords;
		}

		JsonObject mejorPistaGoogle = crearCoordsVacias();

		for (String candidato : candidatos) {
		    System.out.println("Probando GOOGLE: " + candidato);

		    JsonObject coords = GoogleGeocode(candidato, municipioValidacion);

		    if (esCoordenadaValida(coords)) {
		        return coords;
		    }

		    if (!mejorPistaGoogle.has("MunicipioSugerido") && coords.has("MunicipioSugerido")) {
		        mejorPistaGoogle = coords;
		    }

		    if (coords.has("error")) {
		        System.out.println("Google fallo: " + coords.get("error").getAsString());
		    }
		}
		
		String candidatoPlaces = direccionNormalizada.direccionGeocoder;

		if (mejorPistaGoogle.has("MunicipioSugerido")) {
		    String municipioSugerido = mejorPistaGoogle.get("MunicipioSugerido").getAsString();

		    DireccionNormalizada direccionConMunicipioSugerido = normalizarDireccionEntrega(
		            direccion,
		            municipioSugerido,
		            barrio
		    );

		    if (direccionConMunicipioSugerido.direccionGeocoder != null
		            && !direccionConMunicipioSugerido.direccionGeocoder.isBlank()) {
		        candidatoPlaces = direccionConMunicipioSugerido.direccionGeocoder;
		    }
		}

		if (candidatoPlaces != null && !candidatoPlaces.isBlank()) {
		    System.out.println("Probando GOOGLE PLACES: " + candidatoPlaces);

		    String municipioPlaces = municipioValidacion;

		    if (mejorPistaGoogle.has("MunicipioSugerido")) {
		        municipioPlaces = mejorPistaGoogle.get("MunicipioSugerido").getAsString();
		    }

		    JsonObject coords = GooglePlacesTextSearch(candidatoPlaces, municipioPlaces);

		    if (esCoordenadaValida(coords)) {
		        return coords;
		    }

		    if (!mejorPistaGoogle.has("MunicipioSugerido") && coords.has("MunicipioSugerido")) {
		        mejorPistaGoogle = coords;
		        System.out.println("Google Places sugirio municipio: "
		                + coords.get("MunicipioSugerido").getAsString()
		                + " por label: "
		                + coords.get("LabelPista").getAsString());
		    }

		    if (coords.has("error")) {
		        System.out.println("Google Places fallo: " + coords.get("error").getAsString());
		    }
		}

		return mejorPistaGoogle;

	}
	
	private static JsonObject geocodificarConCandidatos(DireccionNormalizada direccionNormalizada, String direccion,
	        String municipio, String barrio) throws Exception {

		
		String municipioValidacion = direccionNormalizada.municipio == null || direccionNormalizada.municipio.isBlank()
		            ? municipio
		            : direccionNormalizada.municipio;
		
		String barrioValidacion = direccionNormalizada.barrio == null || direccionNormalizada.barrio.isBlank()
	            ? barrio
	            : direccionNormalizada.barrio;
		
	    JsonObject coords = geocodificarConCandidatosSinSugerencias(direccionNormalizada, direccion, municipio, barrio);
	    coords.addProperty("MunicipioOriginal", municipioValidacion);
	    coords.addProperty("barrio", barrioValidacion);
	    
	    if (esCoordenadaValida(coords)) {
	        return coords;
	    }

	 

	    if (coords.has("MunicipioSugerido")) {
	        String municipioSugerido = coords.get("MunicipioSugerido").getAsString();

	        if (!municipioSugerido.isBlank()
	                && !normalizarTexto(municipioSugerido).equals(normalizarTexto(municipioValidacion))) {

	            System.out.println("Reintentando con municipio sugerido por Google: " + municipioSugerido);

	            DireccionNormalizada reintentada = normalizarDireccionEntrega(direccion, municipioSugerido, barrio);

	            JsonObject coordsReintento = geocodificarConCandidatosSinSugerencias(
	                    reintentada,
	                    direccion,
	                    municipioSugerido,
	                    barrio
	            );

	            if (esCoordenadaValida(coordsReintento)) {
	            	coordsReintento.addProperty("MunicipioOriginal",
	            	        municipioValidacion == null || municipioValidacion.isBlank() ? "" : municipioValidacion);
	            	coordsReintento.addProperty("barrio", barrioValidacion == null || barrioValidacion.isBlank() ? "" : barrioValidacion);
	                coordsReintento.addProperty("MunicipioCorregido", municipioSugerido);
	                return coordsReintento;
	            }

	            JsonObject coordsVarianteMunicipio = geocodificarVariantesLetrasDobles(reintentada, municipioSugerido);
	            if (esCoordenadaValida(coordsVarianteMunicipio)) {
	                coordsVarianteMunicipio.addProperty("MunicipioOriginal", municipioValidacion);
	                coordsVarianteMunicipio.addProperty("barrio", barrioValidacion);
	                coordsVarianteMunicipio.addProperty("MunicipioCorregido", municipioSugerido);
	                coordsVarianteMunicipio.addProperty("DireccionOriginalNormalizada", direccionNormalizada.direccionGeocoder);
	                return coordsVarianteMunicipio;
	            }
	        }
	    }

	    JsonObject coordsVariante = geocodificarVariantesLetrasDobles(direccionNormalizada, municipioValidacion);
	    coordsVariante.addProperty("MunicipioOriginal", municipioValidacion);
	    coordsVariante.addProperty("barrio", barrioValidacion);
	    
	    if (esCoordenadaValida(coordsVariante)) {
	        return coordsVariante;
	    }
	    
	    
	 // AQUI VA EL NUEVO BLOQUE
	    JsonObject pistaSinMunicipio = buscarMunicipioSugeridoSinMunicipio(
	            direccion,
	            municipioValidacion,
	            barrio
	    );

	    if (pistaSinMunicipio.has("MunicipioSugerido")) {
	        String municipioSugerido = pistaSinMunicipio.get("MunicipioSugerido").getAsString();

	        if (!municipioSugerido.isBlank()
	                && !normalizarTexto(municipioSugerido).equals(normalizarTexto(municipioValidacion))) {

	            System.out.println("Reintentando con municipio sugerido sin municipio: " + municipioSugerido);

	            String direccionSinMunicipio = quitarMunicipiosSoportados(direccion);

	            DireccionNormalizada reintentada = normalizarDireccionEntrega(
	                    direccionSinMunicipio,
	                    municipioSugerido,
	                    barrio
	            );

	            JsonObject coordsReintento = geocodificarConCandidatosSinSugerencias(
	                    reintentada,
	                    direccionSinMunicipio,
	                    municipioSugerido,
	                    barrio
	            );

	            if (esCoordenadaValida(coordsReintento)) {
	                coordsReintento.addProperty("MunicipioOriginal",
	                        municipioValidacion == null || municipioValidacion.isBlank()
	                                ? "SIN MUNICIPIO"
	                                : municipioValidacion);
	                coordsReintento.addProperty("MunicipioCorregido", municipioSugerido);
	                coordsReintento.addProperty("CorreccionAplicada", "MUNICIPIO_SUGERIDO_SIN_MUNICIPIO");
	                return coordsReintento;
	            }
	        }
	    }

	    return crearCoordsVacias();
	}
	
	/**
	 * Construye el Resultado final cuando la direccion ya fue geocodificada.
	 *
	 * Recibe las coordenadas devueltas por el proveedor, consulta cobertura con
	 * ese punto y agrega informacion adicional como proveedor, correcciones y
	 * direccion devuelta por la geocodificacion.
	 */
	private static Resultado crearResultadoConCoordenadas(JsonObject coords, String tipo_cliente, String lead ,String direccion) {
		double lat = coords.get("Latitud").getAsDouble();
		double lng = coords.get("Longitud").getAsDouble();
		String address = coords.get("Direccion").getAsString();
		
	

		Resultado resultado = consultarZonas(lng, lat, tipo_cliente, lead, direccion, queryUrl);

		resultado.setLatitud(lat);
		resultado.setLongitud(lng);
		resultado.setDireccion(address);
		
		if (coords.has("Proveedor")) {
		    resultado.setProveedorGeocodificacion(coords.get("Proveedor").getAsString());
		}

		if (coords.has("CorreccionAplicada")) {
		    resultado.setCorreccionAplicada(coords.get("CorreccionAplicada").getAsString());
		}

		if (coords.has("MunicipioOriginal")) {
		    resultado.setMunicipioOriginal(coords.get("MunicipioOriginal").getAsString());
		}

		if (coords.has("MunicipioCorregido")) {
		    resultado.setMunicipioCorregido(coords.get("MunicipioCorregido").getAsString());
		}

		if (coords.has("DireccionCorregida")) {
		    resultado.setDireccionCorregida(coords.get("DireccionCorregida").getAsString());
		}

		if (coords.has("DireccionOriginalNormalizada")) {
		    resultado.setDireccionOriginalNormalizada(coords.get("DireccionOriginalNormalizada").getAsString());
		}
		

		if (coords.has("barrio")) {
		    resultado.setBarrio(coords.get("barrio").getAsString());
		}

		return resultado;
	}

	private static JsonObject crearCoordsVacias() {
		JsonObject coords = new JsonObject();
		coords.addProperty("Longitud", 0);
		coords.addProperty("Latitud", 0);
		coords.addProperty("Direccion", "");
		return coords;
	}

	/**
	 * Valida que el objeto tenga latitud y longitud utilizables.
	 *
	 * Actualmente descarta solo el caso 0,0. Para valores recibidos directamente
	 * del request se recomienda validar tambien rangos geograficos en CoberturaRequest.
	 */
	private static boolean esCoordenadaValida(JsonObject coords) {
		if (coords == null || !coords.has("Latitud") || !coords.has("Longitud")) {
			return false;
		}

		double lat = coords.get("Latitud").getAsDouble();
		double lng = coords.get("Longitud").getAsDouble();
		return !(lat == 0 && lng == 0);
	}

	
	private static void inicializarApiKey() throws Exception {
	    Parametro parametro = ParametrosDAO.obtenerParametro("APIARCGIS");
	    API_KEY_ARCGIS = parametro.getValorTexto();

	    parametro = ParametrosDAO.obtenerParametro("APIHEREMAPS");
	    API_KEY_HERE = parametro.getValorTexto();

	    parametro = ParametrosDAO.obtenerParametro("APIGOOGLE");
	    API_KEY_GOOGLE = parametro.getValorTexto();
	}

	private static JsonObject obtenerCoordenadasDesdeArcGIS(String direccion, String municipioPedido) throws Exception {
	    JsonObject coords = crearCoordsVacias();
	    
	    String url = serviceUrl + "/findAddressCandidates"
	            + "?f=json"
	            + "&singleLine=" + urlEncode(direccion)
	            + "&countryCode=COL"
	            + "&sourceCountry=COL"
	            + "&outFields=*"
	            + "&maxLocations=5"
	            + "&outSR=4326"
	            + "&forStorage=false"
	            + "&token=" + urlEncode(API_KEY_ARCGIS);
	    

	    
	    JsonObject json = ejecutarGetJson(url);

	    if (json.has("error")) {
	        coords.addProperty("error", json.getAsJsonObject("error").toString());
	        return coords;
	    }

	    if (!json.has("candidates") || json.getAsJsonArray("candidates").isEmpty()) {
	        coords.addProperty("error", "No se encontraron resultados");
	        return coords;
	    }

	    for (JsonElement elem : json.getAsJsonArray("candidates")) {
	        JsonObject candidate = elem.getAsJsonObject();

	        String address = candidate.has("address") ? candidate.get("address").getAsString() : "";
	        JsonObject atributos = candidate.has("attributes")
	                ? candidate.getAsJsonObject("attributes")
	                : new JsonObject();

	        String municipioArcgis = obtenerAtributo(atributos, "City");
	        String districtArcgis = obtenerAtributo(atributos, "District");
	        String addrType = obtenerAtributo(atributos, "Addr_type");

	        JsonObject attExtra = new JsonObject();
	        attExtra.addProperty("municipioPedido", normalizarTexto(municipioPedido));
	        attExtra.addProperty("municipioApi", normalizarTexto(municipioArcgis));
	        attExtra.addProperty("districtApi", normalizarTexto(districtArcgis));
	        attExtra.addProperty("tipoResultado", normalizarTexto(addrType));

	        String direccionParaValidar = quitarSufijoPais(direccion);

	        if (esCoincidencia(direccionParaValidar, address, attExtra)
	                && candidate.has("location")) {

	            JsonObject location = candidate.getAsJsonObject("location");

	            JsonObject mejor = new JsonObject();
	            mejor.addProperty("Longitud", location.get("x").getAsDouble());
	            mejor.addProperty("Latitud", location.get("y").getAsDouble());
	            mejor.addProperty("Direccion", address);
	            mejor.addProperty("Proveedor", "ARCGIS");
	            mejor.addProperty("TipoResultado", addrType);
	            return mejor;
	        }
	    }

	    coords.addProperty("error", "No se encontraron resultados exactos");
	    return coords;
	}


	/**
	 * Consulta si un punto cae dentro de alguna zona de cobertura.
	 *
	 * Esta sobrecarga mantiene el comportamiento historico: valida el punto contra
	 * cualquier zona encontrada, sin exigir una tienda especifica.
	 */
	public static Resultado consultarZonas(double longitud, double latitud, String tipo_cliente,
	        String lead, String direccion, String queryUrl) {
	    return consultarZonas(longitud, latitud, tipo_cliente, lead, direccion, queryUrl, null);
	}

	
	/**
	 * Consulta si un punto cae dentro de una zona de cobertura.
	 *
	 * Si idTiendaEsperada viene informado, la zona encontrada debe corresponder
	 * a esa tienda. Si no coincide, se ignora esa zona y se sigue buscando.
	 *
	 * Cuando idTiendaEsperada es null, funciona igual que el flujo anterior.
	 */

	public static Resultado consultarZonas(double longitud, double latitud, String tipo_cliente,
	        String lead, String direccion, String queryUrl, Integer idTiendaEsperada) {

	    Resultado resultado = new Resultado();
	    String tipoCliente = tipo_cliente == null ? "" : tipo_cliente.toLowerCase().trim();

	    try {
	        JsonObject geometry = new JsonObject();
	        geometry.addProperty("x", longitud);
	        geometry.addProperty("y", latitud);

	        JsonObject spatialReference = new JsonObject();
	        spatialReference.addProperty("wkid", 4326);
	        geometry.add("spatialReference", spatialReference);

	        String endpoint = queryUrl.endsWith("/query") ? queryUrl : queryUrl + "/query";

	        String url = endpoint
	                + "?f=json"
	                + "&where=1%3D1"
	                + "&geometry=" + urlEncode(geometry.toString())
	                + "&geometryType=esriGeometryPoint"
	                + "&inSR=4326"
	                + "&spatialRel=esriSpatialRelIntersects"
	                + "&outFields=*"
	                + "&returnGeometry=false"
	                + "&token=" + urlEncode(API_KEY_ARCGIS);

		    System.out.println("endpoint = " + endpoint);
		    System.out.println("longitud = " + longitud);
		    System.out.println("latitud = " + latitud);
		    System.out.println("geometry = " + geometry.toString());
		    System.out.println("url query zonas = " + url);
		    
	        JsonObject json = ejecutarGetJson(url);

	        if (json.has("error")) {
	            throw new Exception(json.getAsJsonObject("error").toString());
	        }

	        if (json.has("features")) {
	            for (JsonElement elem : json.getAsJsonArray("features")) {
	                JsonObject feature = elem.getAsJsonObject();

	                if (!feature.has("attributes")) {
	                    continue;
	                }

	                JsonObject attributes = feature.getAsJsonObject("attributes");
	                String nombre = obtenerAtributo(attributes, "nombre");
	                

	                if (nombre.isBlank()) {
	                    continue;
	                }

	                int idTiendaZona = capaDAOCC.TiendaDAO.obteneridTienda(nombre);

	                if (idTiendaEsperada != null && idTiendaEsperada > 0 && idTiendaZona != idTiendaEsperada) {
	                    continue;
	                }

	                String mensaje = switch (tipoCliente) {
	                    case "informacion" ->
	                            "Tu direccion se encuentra dentro de nuestra cobertura de la tienda " + nombre
	                                    + ", te invitamos a que sigas con tu pedido. Recuerda que toda la informacion es validada al final por nuestros asesores";

	                    case "programado" ->
	                            "Tu direccion se encuentra dentro de la cobertura de nuestras tiendas. El pedido esta siendo programado para la tienda "
	                                    + nombre;

	                    default ->
	                            "Tu direccion se encuentra dentro de la cobertura de nuestra tienda " + nombre;
	                };

	                resultado.setResultado(mensaje);
	                resultado.setInfoAdicional(nombre);
	                resultado.setEstadoTienda(
	                        TiendaBloqueadaDAO.validarTiendaBloqueada(idTiendaZona)
	                                ? "BLOQUEADO"
	                                : "DISPONIBLE"
	                );
	                
	                resultado.setIdtienda(idTiendaZona);
	                resultado.setSuccess(true);
	               

	                return resultado;
	            }
	        }

	        String mensajeSinCobertura = switch (tipoCliente) {
	            case "informacion" ->
	                    "Por el momento tu direccion no se encuentra dentro de la cobertura de domicilio de nuestras tiendas. Te invitamos a que te comuniques a nuestra linea telefonica 604 4444553 o vuelve a intentar.";

	            case "programado" ->
	                    "La direccion no se encuentra dentro de la cobertura disponible para programar el pedido.";

	            default ->
	                    "La direccion no se encuentra dentro de cobertura.";
	        };

	        resultado.setResultado(mensajeSinCobertura);
	        resultado.setSuccess(false);

	    } catch (Exception e) {
	        System.out.println("Error al realizar la consulta: " + e.getMessage());
	        EnvioCorreo(direccion, lead, e.getMessage());
	        resultado.setSuccess(false);
	    }

	    return resultado;
	}

	public Ubicacion ubicarDireccionEnTiendaBatch(String direccion, String municipio, String barrio) {
		Ubicacion ubicacion = new Ubicacion(0, 0);

		try {
			DireccionNormalizada direccionNormalizada = normalizarDireccionEntrega(direccion, municipio, barrio);
			inicializarApiKey();

			JsonObject coords = geocodificarConCandidatos(direccionNormalizada, direccion, municipio, barrio);
			if (esCoordenadaValida(coords)) {
				ubicacion.setLatitud(coords.get("Latitud").getAsDouble());
				ubicacion.setLongitud(coords.get("Longitud").getAsDouble());
				return ubicacion;
			}

		} catch (Exception e) {
			System.out.println("Error al geocodificar: " + e.getMessage());
		}

		return ubicacion;
	}

	public static void EnvioCorreo(String direccion,String  lead, String error) {
		
		Correo correo = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
		ArrayList<String> correos = new ArrayList<String>();

		correo.setAsunto("TENEMOS PROBLEMA CON LA CONSULTA DE LA COBERTURA");

		correos.add("jubote1@gmail.com");
		correos.add("a.desarrollosi@gmail.com");

		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());

		String mensCorreo = "Se encontro un problema al invocar uno de los servicios de Geocodificacion. " + error;

		if (lead != null && !lead.trim().isEmpty()) {
		    mensCorreo += "<br> Numero de Lead: " + lead.trim();
		}
		
		if (direccion != null && !direccion.trim().isEmpty()) {
		    mensCorreo += "<br>Dirección a consultar: " + direccion.trim();
		}

		correo.setMensaje(mensCorreo);

		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
	}

	public static void main(String[] args) {

		CoberturaRequest coberturaRequest = new CoberturaRequest();
		coberturaRequest.setTelefono("3185020068");

		Resultado resultado = ubicarDireccionEnTienda(coberturaRequest);
		System.out.println(resultado.getResultado());
		System.out.println(resultado.getEstadoTienda());
		System.out.println(resultado.getInfoAdicional());
		System.out.println(resultado.isSuccess());
		System.out.println("Latitud: " + resultado.getLatitud());
		System.out.println("Longitud: " + resultado.getLongitud());
		System.out.println("Proveedor: " + resultado.getProveedorGeocodificacion());
		System.out.println("Correccion aplicada: " + resultado.getCorreccionAplicada());
		System.out.println("Municipio original: " + resultado.getMunicipioOriginal());
		System.out.println("Municipio corregido: " + resultado.getMunicipioCorregido());
		System.out.println("Direccion original normalizada: " + resultado.getDireccionOriginalNormalizada());
		System.out.println("Direccion corregida: " + resultado.getDireccionCorregida());
		System.out.println("Direccion proveedor: " + resultado.getDireccion());
		System.out.println("Barrio: " + resultado.getBarrio());
	}


	private static DireccionNormalizada normalizarDireccionEntrega(String direccion, String municipio, String barrio) {
		DireccionNormalizada d = new DireccionNormalizada();
		d.original = direccion == null ? "" : direccion;

		String texto = normalizarTextoBase(direccion);

		Matcher matcherCruce = PATRON_NOMENCLATURA_CON_CRUCE.matcher(texto);
		Matcher matcher = PATRON_NOMENCLATURA.matcher(texto);

		if (matcherCruce.find()) {
		    d.tipoVia = matcherCruce.group(1);
		    d.numeroVia = limpiarNumeroVia(matcherCruce.group(2)).replaceAll("\\s+", "");
		    d.placa = normalizarTokenDireccion(matcherCruce.group(3)).toUpperCase(Locale.ROOT)
		            + "-"
		            + normalizarTokenDireccion(matcherCruce.group(4)).toUpperCase(Locale.ROOT);
		    d.complemento = extraerComplemento(texto);
		    d.municipio = resolverMunicipio(texto, municipio);
		    d.barrio = resolverBarrio(texto, barrio, matcherCruce, d.municipio, d.complemento);
		    d.esPuntual = true;
		} else if (matcher.find()) {
		    d.tipoVia = matcher.group(1);
		    d.numeroVia = limpiarNumeroVia(matcher.group(2)).replaceAll("\\s+", "");
		    d.placa = normalizarTokenDireccion(matcher.group(3)).toUpperCase(Locale.ROOT)
		            + "-"
		            + normalizarTokenDireccion(matcher.group(4)).toUpperCase(Locale.ROOT);
		    d.complemento = extraerComplemento(texto);
		    d.municipio = resolverMunicipio(texto, municipio);
		    d.barrio = resolverBarrio(texto, barrio, matcher, d.municipio, d.complemento);
		    d.esPuntual = true;
		} else {
			d.tipoVia = extraerTipoVia(texto);
			d.numeroVia = extraerNumeroViaBasico(texto, d.tipoVia);
			d.municipio = resolverMunicipio(texto, municipio);
			d.barrio = limpiarCampo(barrio);
			d.complemento = extraerComplemento(texto);
			d.esPuntual = false;
		}

		d.scoreEntrada = calcularScoreEntrada(d);

		if (d.esPuntual) {
			d.direccionGeocoder = construirDireccionGeocoder(d);
			d.direccionEntrega = construirDireccionEntrega(d);
			d.candidatosPuntuales.add(d.direccionGeocoder);

			if (d.barrio != null && !d.barrio.isBlank()) {
				d.candidatosPuntuales.add(construirDireccionGeocoderConBarrio(d));
			}

			d.candidatosPuntuales.add(construirDireccionSinSimboloNumeral(d));
		}

		return d;
	}

	private static String normalizarTextoBase(String txt) {
		if (txt == null) {
			return "";
		}

		txt = txt.replaceAll("[^\\p{L}\\p{N}\\s,\\.\\#\\-]", " ");
		txt = Normalizer.normalize(txt, Normalizer.Form.NFD);
		txt = txt.replaceAll("\\p{M}", "");
		txt = txt.toLowerCase(Locale.ROOT);

		txt = txt.replaceAll("[,\\.]", " ");
		txt = txt.replaceAll("#\\s*", "# ");
		txt = txt.replaceAll("\\s*-\\s*", "-");

		txt = txt.replaceAll("\\b(cl|cll|calle)\\b", "calle");
		txt = txt.replaceAll("\\b(crra|crr|cra|cr|kr|krr|carrera)\\b", "carrera");
		txt = txt.replaceAll("\\b(av|avenida)\\b", "avenida");
		txt = txt.replaceAll("\\b(dg|diag|diagonal)\\b", "diagonal");
		txt = txt.replaceAll("\\b(tv|transv|transversal)\\b", "transversal");
		txt = txt.replaceAll("\\b(int|interior)\\b", "interior");
		txt = txt.replaceAll("\\b(apto|apt|ap|apartamento)\\b", "apartamento");
		txt = txt.replaceAll("\\b(numero|no|nro|num)\\b", " ");

		txt = txt.replaceAll("\\bs\\b", "sur");
		txt = txt.replaceAll("\\bn\\b", "norte");
		txt = txt.replaceAll("\\be\\b", "este");
		txt = txt.replaceAll("\\bo\\b", "oeste");

		txt = txt.replaceAll("\\s+", " ").trim();

		return txt;
	}

	private static String limpiarDireccionLegadoMejorada(String direccion, String municipio, String barrio) {
		direccion = limpiarCampo(direccion);
		barrio = limpiarCampo(barrio);
		municipio = limpiarCampo(municipio);

		int comaDir = direccion.indexOf(",");
		if (comaDir > 0) {
			direccion = direccion.substring(0, comaDir);
		}

		int comaBarrio = barrio.indexOf(",");
		if (comaBarrio > 0) {
			barrio = barrio.substring(0, comaBarrio);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(direccion);

		if (!municipio.isBlank()) {
			sb.append(", ").append(municipio);
		}

		if (!barrio.isBlank()) {
			sb.append(", ").append(barrio);
		}

		String completa = normalizarTextoBase(sb.toString());
		completa = removerComplementosParaGeocoder(completa);

		String[] partes = completa.split("\\s+");
		if (partes.length > 18) {
			completa = String.join(" ", Arrays.copyOfRange(partes, 0, 18));
		}

		completa += ", " + DEPARTAMENTO + ", " + PAIS;
		completa = completa.replaceAll("\\s+", " ").trim();

		return completa;
	}

	private static String limpiarCampo(String valor) {
		if (valor == null) {
			return "";
		}
		return valor.replaceAll("[^\\p{L}\\p{N}\\s,\\.\\#\\-]", " ").replaceAll("\\s+", " ").trim();
	}

	private static String extraerTipoVia(String texto) {
		Matcher matcher = PATRON_TIPO_VIA.matcher(texto);
		return matcher.find() ? matcher.group(1) : "";
	}

	private static String extraerNumeroViaBasico(String texto, String tipoVia) {
		if (tipoVia == null || tipoVia.isBlank()) {
			return "";
		}

		Pattern patron = Pattern.compile("\\b" + Pattern.quote(tipoVia) + "\\s+(\\d+[a-z]?)\\b");
		Matcher matcher = patron.matcher(texto);

		return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "";
	}

	private static String limpiarNumeroVia(String numeroVia) {
		if (numeroVia == null) {
			return "";
		}
		return numeroVia.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
	}

	private static String resolverMunicipio(String texto, String municipio) {
	    String municipioEnTexto = buscarMunicipioEnTexto(texto);

	    if (!municipioEnTexto.isBlank()) {
	        return municipioEnTexto;
	    }

	    String limpio = normalizarTextoBase(municipio);
	    if (!limpio.isBlank()) {
	        return capitalizarPalabras(limpio);
	    }

	    return "";
	}
	
	private static String buscarMunicipioEnTexto(String texto) {
	    String[] municipios = { "medellin", "bello", "itagui", "envigado", "sabaneta", "la estrella", "caldas",
	            "copacabana", "girardota", "barbosa", "rionegro", "marinilla", "la ceja", "guarne", "el retiro",
	            "carmen de viboral", "santuario", "apartado" };

	    for (String m : MUNICIPIOS_SOPORTADOS) {
	        if (texto.matches(".*\\b" + Pattern.quote(m) + "\\b.*")) {
	            return capitalizarPalabras(m);
	        }
	    }

	    return "";
	}

	private static String resolverBarrio(String texto, String barrio, Matcher nomenclatura, String municipio,
			String complemento) {
		String barrioLimpio = limpiarCampo(barrio);
		if (!barrioLimpio.isBlank()) {
			return capitalizarPalabras(normalizarTextoBase(barrioLimpio));
		}

		String restante = texto.substring(nomenclatura.end()).trim();
		restante = removerComplementosParaGeocoder(restante);

		if (municipio != null && !municipio.isBlank()) {
			restante = restante.replaceAll("\\b" + Pattern.quote(normalizarTextoBase(municipio)) + "\\b", " ");
		}

		restante = restante.replaceAll("\\b" + DEPARTAMENTO.toLowerCase(Locale.ROOT) + "\\b", " ");
		restante = restante.replaceAll("\\b" + PAIS.toLowerCase(Locale.ROOT) + "\\b", " ");
		restante = restante.replaceAll("\\s+", " ").trim();

		if (restante.isBlank() || restante.matches(".*\\b(calle|carrera|avenida|diagonal|transversal)\\b.*")) {
			return "";
		}

		String[] palabras = restante.split("\\s+");
		if (palabras.length > 5) {
			restante = String.join(" ", Arrays.copyOfRange(palabras, 0, 5));
		}

		return capitalizarPalabras(restante);
	}

	private static String extraerComplemento(String texto) {
		Matcher matcher = PATRON_COMPLEMENTO.matcher(texto);
		if (matcher.find()) {
			return capitalizarPalabras(matcher.group().trim());
		}
		return "";
	}

	private static String removerComplementosParaGeocoder(String texto) {
		if (texto == null) {
			return "";
		}
		return PATRON_COMPLEMENTO.matcher(texto).replaceAll(" ").replaceAll("\\s+", " ").trim();
	}

	private static int calcularScoreEntrada(DireccionNormalizada d) {
		int score = 0;

		if (d.tipoVia != null && !d.tipoVia.isBlank()) {
			score += 20;
		}
		if (d.numeroVia != null && !d.numeroVia.isBlank()) {
			score += 20;
		}
		if (d.placa != null && !d.placa.isBlank()) {
			score += 30;
		}
		if (d.municipio != null && !d.municipio.isBlank()) {
			score += 20;
		}
		if (d.barrio != null && !d.barrio.isBlank()) {
			score += 10;
		}

		return Math.min(score, 100);
	}

	private static String construirDireccionGeocoder(DireccionNormalizada d) {
		StringBuilder sb = new StringBuilder();
		sb.append(capitalizarPalabras(d.tipoVia)).append(" ").append(d.numeroVia).append(" # ").append(d.placa);

		if (d.municipio != null && !d.municipio.isBlank()) {
			sb.append(", ").append(d.municipio);
		}

		sb.append(", ").append(DEPARTAMENTO).append(", ").append(PAIS);

		return sb.toString();
	}

	private static String construirDireccionGeocoderConBarrio(DireccionNormalizada d) {
		StringBuilder sb = new StringBuilder();
		sb.append(capitalizarPalabras(d.tipoVia)).append(" ").append(d.numeroVia).append(" # ").append(d.placa);

		if (d.barrio != null && !d.barrio.isBlank()) {
			sb.append(", ").append(d.barrio);
		}

		if (d.municipio != null && !d.municipio.isBlank()) {
			sb.append(", ").append(d.municipio);
		}

		sb.append(", ").append(DEPARTAMENTO).append(", ").append(PAIS);

		return sb.toString();
	}

	private static String construirDireccionSinSimboloNumeral(DireccionNormalizada d) {
		String placaSinGuion = d.placa.replace("-", " ");
		StringBuilder sb = new StringBuilder();
		sb.append(capitalizarPalabras(d.tipoVia)).append(" ").append(d.numeroVia).append(" ").append(placaSinGuion);

		if (d.municipio != null && !d.municipio.isBlank()) {
			sb.append(", ").append(d.municipio);
		}

		sb.append(", ").append(DEPARTAMENTO).append(", ").append(PAIS);

		return sb.toString();
	}

	private static String construirDireccionEntrega(DireccionNormalizada d) {
		StringBuilder sb = new StringBuilder();
		sb.append(capitalizarPalabras(d.tipoVia)).append(" ").append(d.numeroVia).append(" # ").append(d.placa);

		if (d.complemento != null && !d.complemento.isBlank()) {
			sb.append(", ").append(d.complemento);
		}

		if (d.barrio != null && !d.barrio.isBlank()) {
			sb.append(", Barrio ").append(d.barrio);
		}

		if (d.municipio != null && !d.municipio.isBlank()) {
			sb.append(", ").append(d.municipio);
		}

		return sb.toString();
	}

	public static JsonObject HereGeocode(String direccion, String municipioPedido) {
		String url = "https://geocode.search.hereapi.com/v1/geocode?q="
				+ java.net.URLEncoder.encode(direccion, java.nio.charset.StandardCharsets.UTF_8)
				+ "&lang=es&in=countryCode:COL&limit=5&apiKey=" + API_KEY_HERE;

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		JsonObject coords = crearCoordsVacias();

		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

				if (json.has("items") && json.getAsJsonArray("items").size() > 0) {
					for (JsonElement elem : json.getAsJsonArray("items")) {
						JsonObject item = elem.getAsJsonObject();

						if (!item.has("address") || !item.has("position")) {
							continue;
						}

						JsonObject addr = item.getAsJsonObject("address");
						String label = addr.has("label") ? addr.get("label").getAsString() : "";

						String municipioHere = addr.has("city") ? addr.get("city").getAsString() : "";
						String districtHere = addr.has("district") ? addr.get("district").getAsString() : "";
						String resultType = item.has("resultType") ? item.get("resultType").getAsString() : "";

						JsonObject attExtra = new JsonObject();
						attExtra.addProperty("municipioPedido", normalizarTexto(municipioPedido));
						attExtra.addProperty("municipioApi", normalizarTexto(municipioHere));
						attExtra.addProperty("districtApi", normalizarTexto(districtHere));
						attExtra.addProperty("tipoResultado", normalizarTexto(resultType));

						String direccionParaValidar = quitarSufijoPais(direccion);

						if (esCoincidencia(direccionParaValidar, label, attExtra)) {
							JsonObject pos = item.getAsJsonObject("position");

							coords.addProperty("Latitud", pos.get("lat").getAsDouble());
							coords.addProperty("Longitud", pos.get("lng").getAsDouble());
							coords.addProperty("Direccion", label);
							coords.addProperty("Proveedor", "HERE");
							coords.addProperty("TipoResultado", resultType);
							return coords;
						}
					}

					coords.addProperty("error", "No se encontraron resultados exactos");
				} else {
					coords.addProperty("error", "No se encontraron resultados");
				}
			} else {
				coords.addProperty("error", "Error en la peticion geocode: " + response.statusCode());
			}

		} catch (Exception e) {
			System.out.println("No se pudo geocodificar la direccion.");
			e.printStackTrace();
		}

		return coords;
	}

	private static boolean esCoincidencia(String query, String label, JsonObject extra) {
		int score = calcularScoreCoincidencia(query, label, extra);
		return score >= 65;
	}

	private static boolean contieneDireccionConNumeral(String query) {
	    String q = normalizarTextoBase(query);
	    return q.matches(".*\\b(calle|carrera|avenida|diagonal|transversal|circular|autopista)\\b.*")
	            && (q.contains("#") || q.matches(".*\\b\\d+\\s*[a-z]{0,2}\\s*-\\s*\\d+\\s*[a-z]{0,2}\\b.*"));
	}
	
	private static int calcularScoreCoincidencia(String query, String label, JsonObject extra) {
		String q = normalizarTexto(removerComplementosParaGeocoder(query));
		String l = normalizarTexto(label);

		NomenclaturaEsperada nomenclatura = extraerNomenclaturaEsperada(query);
		
		if (!nomenclatura.completa && contieneDireccionConNumeral(query)) {
		    System.out.println("Rechazado porque la entrada parece puntual pero no se pudo parsear completa. Query: " + query);
		    return 0;
		}
		if (nomenclatura.completa && !labelContieneNomenclaturaCompleta(label, nomenclatura)) {
			System.out.println("Rechazado por nomenclatura incompleta. Query: " + query + " | Label: " + label);
			return 0;
		}

		String m = normalizarTexto(extra.get("municipioPedido").getAsString());
		String mApi = normalizarTexto(extra.get("municipioApi").getAsString());
		String dApi = normalizarTexto(extra.get("districtApi").getAsString());
		String tipoResultado = extra.has("tipoResultado") ? normalizarTexto(extra.get("tipoResultado").getAsString())
				: "";

		if (!m.isEmpty() && !l.contains(m) && !mApi.contains(m) && !dApi.contains(m)) {
			return 0;
		}

		int score = 0;

		if (!m.isEmpty()) {
			score += 20;
		}

		if (tipoResultado.contains("pointaddress") || tipoResultado.contains("streetaddress")
				|| tipoResultado.contains("subaddress") || tipoResultado.contains("housenumber")) {
			score += 20;
		}

		if (contieneMismoTipoVia(q, l)) {
			score += 15;
		}

		List<String> numerosQ = extraerTokensNumero(q);
		List<String> numerosL = extraerTokensNumero(l);

		int numerosCoincidentes = 0;
		for (String n : numerosQ) {
			if (numerosL.contains(n)) {
				numerosCoincidentes++;
			}
		}

		if (numerosQ.size() >= 3) {
			score += Math.min(numerosCoincidentes * 18, 45);
		} else if (!numerosQ.isEmpty()) {
			score += Math.min(numerosCoincidentes * 15, 30);
		}

		double ratio = calcularRatioPalabras(q, l);
		if (ratio >= 0.80) {
			score += 20;
		} else if (ratio >= 0.65) {
			score += 10;
		}

		return Math.min(score, 100);
	}

	private static boolean contieneMismoTipoVia(String q, String l) {
		String[] tipos = { "calle", "carrera", "avenida", "diagonal", "transversal", "circular", "autopista" };

		for (String tipo : tipos) {
			if (q.contains(tipo) && l.contains(tipo)) {
				return true;
			}
		}

		return false;
	}

	private static List<String> extraerTokensNumero(String texto) {
	    List<String> numeros = new ArrayList<>();

	    Matcher matcher = Pattern
	            .compile("\\b\\d+\\s*(?:este|oeste|norte|sur|[a-z]{1,2})?\\b")
	            .matcher(texto);

	    while (matcher.find()) {
	        numeros.add(normalizarTokenDireccion(matcher.group()));
	    }

	    return numeros;
	}

	private static double calcularRatioPalabras(String query, String label) {
		String[] palabrasQ = query.split(" ");
		int total = 0;
		int match = 0;

		for (String p : palabrasQ) {
			if (p.isBlank() || esPalabraRuido(p)) {
				continue;
			}

			total++;

			if (label.contains(p)) {
				match++;
			}
		}

		if (total == 0) {
			return 0;
		}

		return (double) match / total;
	}

	private static boolean esPalabraRuido(String palabra) {
		return palabra.equals("colombia") || palabra.equals("antioquia") || palabra.equals("barrio")
				|| palabra.equals("apartamento") || palabra.equals("apto") || palabra.equals("ap")
				|| palabra.equals("interior") || palabra.equals("torre") || palabra.equals("piso");
	}

	private static String quitarSufijoPais(String direccion) {
		if (direccion == null) {
			return "";
		}

		return direccion.replaceAll("(?i),?\\s*Antioquia,?\\s*Colombia\\s*$", "").trim();
	}

	private static String normalizarTexto(String txt) {
		if (txt == null) {
			return "";
		}

		txt = Normalizer.normalize(txt, Normalizer.Form.NFD);
		txt = txt.replaceAll("\\p{M}", "");
		txt = txt.toLowerCase(Locale.ROOT);
		txt = txt.replaceAll("[#,.]", " ");
		txt = txt.replaceAll("-", " ");

		txt = txt.replaceAll("\\b(cl|cll)\\b", "calle");
		txt = txt.replaceAll("\\b(crra|crr|cra|cr|kr|krr)\\b", "carrera");
		txt = txt.replaceAll("\\b(av)\\b", "avenida");
		txt = txt.replaceAll("\\b(dg|diag)\\b", "diagonal");
		txt = txt.replaceAll("\\b(tv|transv)\\b", "transversal");

		txt = txt.replaceAll("(?<=\\d)(?=[a-zA-Z])", " ");
		txt = txt.replaceAll("\\b(\\d+)\\s+e\\b", "$1 este");
		txt = txt.replaceAll("\\b(\\d+)\\s+o\\b", "$1 oeste");
		txt = txt.replaceAll("\\b(\\d+)\\s+n\\b", "$1 norte");
		txt = txt.replaceAll("\\b(\\d+)\\s+s\\b", "$1 sur");
		txt = txt.replaceAll("\\b(\\d+)\\s+([a-df-mp-zA-DF-MP-Z]{1,2})\\b", "$1$2");

		txt = txt.replaceAll("\\s+", " ").trim();

		return txt;
	}

	private static String capitalizarPalabras(String texto) {
		if (texto == null || texto.isBlank()) {
			return "";
		}

		String[] partes = texto.toLowerCase(Locale.ROOT).split("\\s+");
		StringBuilder sb = new StringBuilder();

		for (String parte : partes) {
			if (parte.isBlank()) {
				continue;
			}

			if (sb.length() > 0) {
				sb.append(" ");
			}

			sb.append(parte.substring(0, 1).toUpperCase(Locale.ROOT)).append(parte.substring(1));
		}

		return sb.toString();
	}

	private static NomenclaturaEsperada extraerNomenclaturaEsperada(String direccion) {
		NomenclaturaEsperada n = new NomenclaturaEsperada();

		String texto = normalizarTextoBase(quitarSufijoPais(direccion));
		Matcher matcher = PATRON_NOMENCLATURA.matcher(texto);

		if (matcher.find()) {
			n.tipoVia = normalizarTexto(matcher.group(1));
			n.numeroVia = normalizarTokenDireccion(matcher.group(2));
			n.placaInicial = normalizarTokenDireccion(matcher.group(3));
			n.placaFinal = normalizarTokenDireccion(matcher.group(4));
			n.completa = true;
		}

		return n;
	}

	private static String normalizarTokenDireccion(String valor) {
	    if (valor == null) {
	        return "";
	    }

	    String token = normalizarTexto(valor);

	    token = token.replaceAll("\\b(\\d+)\\s*este\\b", "$1e");
	    token = token.replaceAll("\\b(\\d+)\\s*oeste\\b", "$1o");
	    token = token.replaceAll("\\b(\\d+)\\s*norte\\b", "$1n");
	    token = token.replaceAll("\\b(\\d+)\\s*sur\\b", "$1s");

	    return token.replaceAll("\\s+", "");
	}

	private static boolean labelContieneNomenclaturaCompleta(String label, NomenclaturaEsperada n) {
		if (label == null || !n.completa) {
			return false;
		}

		String labelNormalizado = normalizarTexto(label);
		List<String> tokensLabel = extraerTokensNumero(labelNormalizado);

		return tokensLabel.contains(n.numeroVia)
				&& tokensLabel.contains(n.placaInicial)
				&& tokensLabel.contains(n.placaFinal);
	}
	
	private static class DireccionNormalizada {
		String original;
		String direccionGeocoder;
		String direccionEntrega;
		String tipoVia;
		String numeroVia;
		String placa;
		String barrio;
		String municipio;
		String complemento;
		boolean esPuntual;
		int scoreEntrada;
		List<String> candidatosPuntuales = new ArrayList<>();
	}
	
	private static class NomenclaturaEsperada {
		String tipoVia;
		String numeroVia;
		String placaInicial;
		String placaFinal;
		boolean completa;
	}

	private static JsonObject GoogleGeocode(String direccion, String municipioPedido) {
	    JsonObject coords = crearCoordsVacias();

	    String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
	            + java.net.URLEncoder.encode(direccion, java.nio.charset.StandardCharsets.UTF_8)
	            + "&components="
	            + java.net.URLEncoder.encode("country:CO|administrative_area:Antioquia",
	                    java.nio.charset.StandardCharsets.UTF_8)
	            + "&key=" + API_KEY_GOOGLE;

	    HttpClient client = HttpClient.newHttpClient();
	    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

	    try {
	        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

	        if (response.statusCode() != 200) {
	            coords.addProperty("error", "Error Google Geocode: " + response.statusCode());
	            return coords;
	        }

	        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
	        String status = json.has("status") ? json.get("status").getAsString() : "";

	        if (!"OK".equals(status) || !json.has("results") || json.getAsJsonArray("results").isEmpty()) {
	            coords.addProperty("error", "Google sin resultados: " + status);
	            return coords;
	        }

	        for (JsonElement elem : json.getAsJsonArray("results")) {
	            JsonObject item = elem.getAsJsonObject();

	            String label = item.has("formatted_address") ? item.get("formatted_address").getAsString() : "";
	            String resultType = obtenerTiposGoogle(item);

	            JsonObject extra = new JsonObject();
	            extra.addProperty("municipioPedido", normalizarTexto(municipioPedido));
	            extra.addProperty("municipioApi", normalizarTexto(obtenerLocalidadGoogle(item)));
	            extra.addProperty("districtApi", "");
	            extra.addProperty("tipoResultado", normalizarTexto(resultType));

	            String direccionParaValidar = quitarSufijoPais(direccion);

	            if (esCoincidencia(direccionParaValidar, label, extra) && esTipoGoogleAceptable(item)) {
	                JsonObject location = item.getAsJsonObject("geometry").getAsJsonObject("location");

	                coords.addProperty("Latitud", location.get("lat").getAsDouble());
	                coords.addProperty("Longitud", location.get("lng").getAsDouble());
	                coords.addProperty("Direccion", label);
	                coords.addProperty("Proveedor", "GOOGLE");
	                coords.addProperty("TipoResultado", resultType);
	                return coords;
	            }

	            agregarPistaMunicipioSiAplica(coords, direccionParaValidar, label, item);
	        }

	        coords.addProperty("error", "Google no encontro resultado exacto");

	    } catch (Exception e) {
	        coords.addProperty("error", "Excepcion Google Geocode: " + e.getMessage());
	    }

	    return coords;
	}
	
	
	private static void agregarPistaMunicipioSiAplica(JsonObject coords, String query, String label, JsonObject item) {
	    if (coords.has("MunicipioSugerido")) {
	        return;
	    }

	    NomenclaturaEsperada esperada = extraerNomenclaturaEsperada(query);
	    if (!esperada.completa) {
	        return;
	    }

	    String labelNormalizado = normalizarTexto(label);

	    boolean mismaViaPrincipal = labelNormalizado.contains(esperada.numeroVia);
	    boolean mismaViaGeneradora = labelNormalizado.contains(esperada.placaInicial);

	    if (!mismaViaPrincipal || !mismaViaGeneradora) {
	        return;
	    }

	    String municipioSugerido = obtenerLocalidadGoogle(item);
	    if (municipioSugerido.isBlank()) {
	        return;
	    }

	    coords.addProperty("MunicipioSugerido", municipioSugerido);
	    coords.addProperty("LabelPista", label);
	}
	private static String obtenerTiposGoogle(JsonObject item) {
	    if (!item.has("types")) {
	        return "";
	    }

	    List<String> tipos = new ArrayList<>();
	    for (JsonElement tipo : item.getAsJsonArray("types")) {
	        tipos.add(tipo.getAsString());
	    }

	    return String.join(",", tipos);
	}

	private static boolean esTipoGoogleAceptable(JsonObject item) {
	    String tipos = obtenerTiposGoogle(item);
	    String locationType = "";

	    if (item.has("geometry") && item.getAsJsonObject("geometry").has("location_type")) {
	        locationType = item.getAsJsonObject("geometry").get("location_type").getAsString();
	    }

	    boolean tipoPuntual = tipos.contains("street_address")
	            || tipos.contains("premise")
	            || tipos.contains("subpremise");

	    boolean ubicacionConfiable = "ROOFTOP".equals(locationType)
	            || "RANGE_INTERPOLATED".equals(locationType);

	    return tipoPuntual && ubicacionConfiable;
	}

	private static String obtenerLocalidadGoogle(JsonObject item) {
	    String municipioPorComponente = "";

	    if (item.has("address_components")) {
	        for (JsonElement elem : item.getAsJsonArray("address_components")) {
	            JsonObject comp = elem.getAsJsonObject();
	            String nombre = comp.has("long_name") ? comp.get("long_name").getAsString() : "";
	            String tipos = obtenerTiposGoogle(comp);

	            if (esMunicipioSoportado(nombre)) {
	                return capitalizarPalabras(normalizarTextoBase(nombre));
	            }

	            if (tipos.contains("administrative_area_level_2")) {
	                municipioPorComponente = nombre;
	            }
	        }
	    }

	    if (item.has("formatted_address")) {
	        String label = item.get("formatted_address").getAsString();
	        String municipioEnLabel = buscarMunicipioEnTexto(normalizarTextoBase(label));

	        if (!municipioEnLabel.isBlank()) {
	            return municipioEnLabel;
	        }
	    }

	    if (!municipioPorComponente.isBlank() && esMunicipioSoportado(municipioPorComponente)) {
	        return capitalizarPalabras(normalizarTextoBase(municipioPorComponente));
	    }

	    return "";
	}
	
	private static boolean esMunicipioSoportado(String valor) {
	    return !buscarMunicipioEnTexto(normalizarTextoBase(valor)).isBlank();
	}
	private static List<String> generarVariantesLetrasDobles(DireccionNormalizada d) {
	    List<String> variantes = new ArrayList<>();

	    if (d == null || !d.esPuntual || d.placa == null || !d.placa.contains("-")) {
	        return variantes;
	    }

	    String[] placaPartes = d.placa.split("-");
	    if (placaPartes.length != 2) {
	        return variantes;
	    }

	    String numeroVia = d.numeroVia;
	    String placaInicial = placaPartes[0];
	    String placaFinal = placaPartes[1];

	    String numeroViaReducido = reducirLetraDobleFinal(numeroVia);
	    String placaInicialReducida = reducirLetraDobleFinal(placaInicial);
	    String placaFinalReducida = reducirLetraDobleFinal(placaFinal);

	    agregarVarianteSiCambio(variantes, d, numeroViaReducido, placaInicial, placaFinal);
	    agregarVarianteSiCambio(variantes, d, numeroVia, placaInicialReducida, placaFinal);
	    agregarVarianteSiCambio(variantes, d, numeroVia, placaInicial, placaFinalReducida);
	    agregarVarianteSiCambio(variantes, d, numeroViaReducido, placaInicialReducida, placaFinal);
	    agregarVarianteSiCambio(variantes, d, numeroViaReducido, placaInicial, placaFinalReducida);
	    agregarVarianteSiCambio(variantes, d, numeroVia, placaInicialReducida, placaFinalReducida);
	    agregarVarianteSiCambio(variantes, d, numeroViaReducido, placaInicialReducida, placaFinalReducida);

	    return variantes;
	}

	private static void agregarVarianteSiCambio(List<String> variantes, DireccionNormalizada d, String numeroVia,
	        String placaInicial, String placaFinal) {
	    if (numeroVia.equals(d.numeroVia) && (placaInicial + "-" + placaFinal).equals(d.placa)) {
	        return;
	    }

	    String variante = construirDireccionConPartes(d, numeroVia, placaInicial, placaFinal);

	    if (!variantes.contains(variante)) {
	        variantes.add(variante);
	    }
	}

	private static String reducirLetraDobleFinal(String token) {
	    if (token == null) {
	        return "";
	    }

	    return token.replaceAll("(?i)(\\d+)([a-z])\\2$", "$1$2").toUpperCase(Locale.ROOT);
	}

	private static String construirDireccionConPartes(DireccionNormalizada d, String numeroVia, String placaInicial,
	        String placaFinal) {
	    StringBuilder sb = new StringBuilder();

	    sb.append(capitalizarPalabras(d.tipoVia))
	            .append(" ")
	            .append(numeroVia)
	            .append(" # ")
	            .append(placaInicial)
	            .append("-")
	            .append(placaFinal);

	    if (d.municipio != null && !d.municipio.isBlank()) {
	        sb.append(", ").append(d.municipio);
	    }

	    sb.append(", ").append(DEPARTAMENTO).append(", ").append(PAIS);

	    return sb.toString();
	}
	
	private static JsonObject geocodificarVariantesLetrasDobles(DireccionNormalizada direccionNormalizada,
	        String municipioValidacion) throws Exception {

	    List<String> variantes = generarVariantesLetrasDobles(direccionNormalizada);

	    for (String variante : variantes) {
	        System.out.println("Probando variante letras dobles: " + variante);

	        JsonObject coords = obtenerCoordenadasDesdeArcGIS(variante, municipioValidacion);
	        if (esCoordenadaValida(coords)) {
	            coords.addProperty("CorreccionAplicada", "LETRA_DOBLE");
	            coords.addProperty("DireccionOriginalNormalizada", direccionNormalizada.direccionGeocoder);
	            coords.addProperty("DireccionCorregida", variante);
	            return coords;
	        }

	        coords = HereGeocode(variante, municipioValidacion);
	        if (esCoordenadaValida(coords)) {
	            coords.addProperty("CorreccionAplicada", "LETRA_DOBLE");
	            coords.addProperty("DireccionOriginalNormalizada", direccionNormalizada.direccionGeocoder);
	            coords.addProperty("DireccionCorregida", variante);
	            return coords;
	        }
	    }

	    for (String variante : variantes) {
	        JsonObject coords = GoogleGeocode(variante, municipioValidacion);
	        if (esCoordenadaValida(coords)) {
	            coords.addProperty("CorreccionAplicada", "LETRA_DOBLE");
	            coords.addProperty("DireccionOriginalNormalizada", direccionNormalizada.direccionGeocoder);
	            coords.addProperty("DireccionCorregida", variante);
	            return coords;
	        }
	    }

	    return crearCoordsVacias();
	}
	
	private static JsonObject GooglePlacesTextSearch(String direccion, String municipioPedido) {
	    JsonObject coords = crearCoordsVacias();

	    String url = "https://places.googleapis.com/v1/places:searchText";

	    JsonObject body = new JsonObject();
	    body.addProperty("textQuery", direccion);
	    body.addProperty("languageCode", "es");
	    body.addProperty("regionCode", "CO");

	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .header("Content-Type", "application/json")
	            .header("X-Goog-Api-Key", API_KEY_GOOGLE)
	            .header("X-Goog-FieldMask", "places.formattedAddress,places.location,places.displayName,places.types")
	            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
	            .build();

	    try {
	        HttpResponse<String> response = HttpClient.newHttpClient()
	                .send(request, HttpResponse.BodyHandlers.ofString());

	        if (response.statusCode() != 200) {
	            coords.addProperty("error", "Error Google Places: " + response.statusCode() + " - " + response.body());
	            return coords;
	        }

	        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

	        if (!json.has("places") || json.getAsJsonArray("places").isEmpty()) {
	            coords.addProperty("error", "Google Places sin resultados");
	            return coords;
	        }

	        for (JsonElement elem : json.getAsJsonArray("places")) {
	            JsonObject place = elem.getAsJsonObject();

	            String label = place.has("formattedAddress") ? place.get("formattedAddress").getAsString() : "";
	            String nombre = "";

	            if (place.has("displayName")) {
	                JsonObject displayName = place.getAsJsonObject("displayName");
	                nombre = displayName.has("text") ? displayName.get("text").getAsString() : "";
	            }

	            JsonObject extra = new JsonObject();
	            extra.addProperty("municipioPedido", normalizarTexto(municipioPedido));
	            extra.addProperty("municipioApi", normalizarTexto(obtenerMunicipioDesdeTexto(label)));
	            extra.addProperty("districtApi", "");
	            extra.addProperty("tipoResultado", "streetaddress pointaddress premise");

	            String direccionParaValidar = quitarSufijoPais(direccion);

	            if (esCoincidencia(direccionParaValidar, label, extra) && place.has("location")) {
	                JsonObject location = place.getAsJsonObject("location");

	                coords.addProperty("Latitud", location.get("latitude").getAsDouble());
	                coords.addProperty("Longitud", location.get("longitude").getAsDouble());
	                coords.addProperty("Direccion", label);
	                coords.addProperty("Proveedor", "GOOGLE_PLACES");
	                coords.addProperty("TipoResultado", "PLACE_TEXT_SEARCH");

	                if (!nombre.isBlank()) {
	                    coords.addProperty("NombreLugar", nombre);
	                }

	                return coords;
	            }

	            agregarPistaMunicipioDesdeTextoSiAplica(coords, direccionParaValidar, label);
	        }

	        coords.addProperty("error", "Google Places no encontro resultado exacto");

	    } catch (Exception e) {
	        coords.addProperty("error", "Excepcion Google Places: " + e.getMessage());
	    }

	    return coords;
	}
	
	private static String obtenerMunicipioDesdeTexto(String texto) {
	    return buscarMunicipioEnTexto(normalizarTextoBase(texto));
	}

	private static void agregarPistaMunicipioDesdeTextoSiAplica(JsonObject coords, String query, String label) {
	    if (coords.has("MunicipioSugerido")) {
	        return;
	    }

	    NomenclaturaEsperada esperada = extraerNomenclaturaEsperada(query);
	    if (!esperada.completa) {
	        return;
	    }

	    String labelNormalizado = normalizarTexto(label);

	    boolean mismaViaPrincipal = labelNormalizado.contains(esperada.numeroVia);
	    boolean mismaViaGeneradora = labelNormalizado.contains(esperada.placaInicial);

	    if (!mismaViaPrincipal || !mismaViaGeneradora) {
	        return;
	    }

	    String municipioSugerido = obtenerMunicipioDesdeTexto(label);
	    if (municipioSugerido.isBlank()) {
	        return;
	    }

	    coords.addProperty("MunicipioSugerido", municipioSugerido);
	    coords.addProperty("LabelPista", label);
	}
	
	/**
	 * Define si es seguro usar las coordenadas recibidas sin geocodificar.
	 *
	 * No basta con que latitud y longitud sean validas: tambien se exige idTienda
	 * y una direccionProveedor que coincida con la direccion actual del cliente.
	 * Esto evita confiar en coordenadas reales pero asociadas a otra direccion.
	 */
	
	private static boolean puedeValidarPorCoordenadas(CoberturaRequest request) {
		
		
		System.out.println("---** ----"+direccionesCoinciden(request.getDireccion(), request.getDireccionProveedor()));
		System.out.println(request.tieneCoordenadasValidas());
		System.out.println(request.tieneCoordenadasValidas());
		System.out.println(request.tieneIdTiendaValido());
		System.out.println(request.tieneDireccionProveedor());   
		
		
		
	    return request.tieneCoordenadasValidas()
	            && request.tieneIdTiendaValido()
	            && request.tieneDireccionProveedor()
	            && direccionesCoinciden(request.getDireccion(), request.getDireccionProveedor());
	}

	/**
	 * Compara dos direcciones por nomenclatura, no por texto exacto.
	 *
	 * Ejemplo: "Cra 100a # 47e-49" y
	 * "Carrera 100A 47E 49, Medellin, Antioquia" deben considerarse equivalentes.
	 *
	 * Solo compara la base de la direccion: tipo de via, numero de via,
	 * placa inicial y placa final. Complementos como apartamento, torre o barrio
	 * no se usan para decidir cobertura.
	 */
	private static boolean direccionesCoinciden(String direccionCliente, String direccionProveedor) {
	    NomenclaturaEsperada cliente = extraerNomenclaturaEsperada(direccionCliente);
	    NomenclaturaEsperada proveedor = extraerNomenclaturaEsperada(direccionProveedor);

	    if (!cliente.completa || !proveedor.completa) {
	        return false;
	    }

	    return cliente.tipoVia.equals(proveedor.tipoVia)
	            && cliente.numeroVia.equals(proveedor.numeroVia)
	            && cliente.placaInicial.equals(proveedor.placaInicial)
	            && cliente.placaFinal.equals(proveedor.placaFinal);
	}
	
	private static JsonObject ejecutarGetJson(String url) throws Exception {
	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .GET()
	            .build();

	    HttpResponse<String> response = HttpClient.newHttpClient()
	            .send(request, HttpResponse.BodyHandlers.ofString());

	    if (response.statusCode() != 200) {
	        throw new Exception("Error HTTP ArcGIS: " + response.statusCode() + " - " + response.body());
	    }

	    return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private static String urlEncode(String valor) {
	    return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
	}

	private static String obtenerAtributo(JsonObject atributos, String nombre) {
	    if (atributos == null || !atributos.has(nombre) || atributos.get(nombre).isJsonNull()) {
	        return "";
	    }

	    return atributos.get(nombre).getAsString();
	}

	
	private static JsonObject buscarMunicipioSugeridoSinMunicipio(String direccion,
	        String municipioOriginal, String barrio) throws Exception {

	    JsonObject pista = crearCoordsVacias();

	    String direccionSinMunicipio = quitarMunicipiosSoportados(direccion);
	    DireccionNormalizada normalizadaSinMunicipio = normalizarDireccionEntrega(
	            direccionSinMunicipio,
	            "",
	            barrio
	    );

	    if (normalizadaSinMunicipio.direccionGeocoder == null
	            || normalizadaSinMunicipio.direccionGeocoder.isBlank()) {
	        return pista;
	    }

	    System.out.println("Buscando municipio sugerido sin municipio: "
	            + normalizadaSinMunicipio.direccionGeocoder);

	    JsonObject coords = GoogleGeocode(normalizadaSinMunicipio.direccionGeocoder, "");

	    return convertirResultadoEnPistaMunicipio(coords, municipioOriginal);
	}

	private static JsonObject convertirResultadoEnPistaMunicipio(JsonObject coords, String municipioOriginal) {
	    JsonObject pista = crearCoordsVacias();

	    if (!esCoordenadaValida(coords) || !coords.has("Direccion")) {
	        return pista;
	    }

	    String direccionEncontrada = coords.get("Direccion").getAsString();
	    String municipioSugerido = obtenerMunicipioDesdeTexto(direccionEncontrada);

	    if (municipioSugerido.isBlank()) {
	        return pista;
	    }

	    if (!normalizarTexto(municipioOriginal).isBlank()
	            && normalizarTexto(municipioSugerido).equals(normalizarTexto(municipioOriginal))) {
	        return pista;
	    }

	    pista.addProperty("MunicipioSugerido", municipioSugerido);
	    pista.addProperty("LabelPista", direccionEncontrada);

	    if (coords.has("Proveedor")) {
	        pista.addProperty("ProveedorPista", coords.get("Proveedor").getAsString());
	    }

	    return pista;
	}

	private static String quitarMunicipiosSoportados(String direccion) {
	    String texto = normalizarTextoBase(direccion);

	    for (String municipio : MUNICIPIOS_SOPORTADOS) {
	        texto = texto.replaceAll("\\b" + Pattern.quote(municipio) + "\\b", " ");
	    }

	    return texto.replaceAll("\\s+", " ").trim();
	}
	
}
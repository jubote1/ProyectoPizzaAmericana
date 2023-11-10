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

import capaDAOCC.ParametrosDAO;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.Parametro;
import capaModeloCC.Resultado;
import capaModeloCC.Ubicacion;
import utilidadesCC.ControladorEnvioCorreo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;



public class UbicacionCtrl {
	
	public static boolean primeraEjecucion = false;
	
	public Resultado ubicarDireccionEnTienda(String direccion)
	{
		Resultado resultado = new Resultado();
		try {
			Parametro parametro;
//			if(!primeraEjecucion)
//			{
//				parametro =ParametrosDAO.obtenerParametro("LIBRERIAARCGIS");
//				System.out.println(parametro.getValorTexto());
//				ArcGISRuntimeEnvironment.setInstallDirectory(parametro.getValorTexto());
//				primeraEjecucion = true;
//			}
			parametro =ParametrosDAO.obtenerParametro("APIARCGIS");
			System.out.println(parametro.getValorTexto());
			System.out.println("2.1 ANTES DE INICIAR ");
			ArcGISRuntimeEnvironment.setApiKey(parametro.getValorTexto());
			System.out.println("2.2 ANTES DE INICIAR ");
	        String serviceUrl = "http://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";
	        //String address = "Cl. 104 #42b-2 a 42b-30,Medell�n,Antioquia";
	        String queryUrl = "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0";
	
	        LocatorTask locatorTask = new LocatorTask(serviceUrl);
	        GeocodeParameters geocodeParameters = new GeocodeParameters();
	        geocodeParameters.getResultAttributeNames().add("*");
	        geocodeParameters.setMaxResults(1); // Limitamos el n�mero de resultados para obtener solo el m�s relevante
	
	        ListenableFuture<List<GeocodeResult>> future = locatorTask.geocodeAsync(direccion, geocodeParameters);
            List<GeocodeResult> geocodeResults = future.get(); 

            if (geocodeResults != null && !geocodeResults.isEmpty()) {
                GeocodeResult geocodeResult = geocodeResults.get(0);
                Point displayLocation = geocodeResult.getDisplayLocation();
                double latitude = displayLocation.getY();
                double longitude = displayLocation.getX();

                System.out.println("Coordenadas geogr�ficas de la direcci�n:");
                System.out.println("Latitud: " + latitude);
                System.out.println("Longitud: " + longitude);
                
                ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(queryUrl);

                Point point = new Point(longitude, latitude, SpatialReferences.getWgs84());
             // Transforma las coordenadas al sistema de coordenadas de la capa de entidades (si es diferente)
                QueryParameters query = new QueryParameters();
                query.setGeometry(point); // Establece la geometr�a de la consulta (en este caso, un punto)
                query.setSpatialRelationship(QueryParameters.SpatialRelationship.WITHIN);
 
               
                    try {
                        FeatureQueryResult result = serviceFeatureTable.queryFeaturesAsync(query,ServiceFeatureTable.QueryFeatureFields.LOAD_ALL).get();

                        // Verifica si hay resultados en la consulta
                        if (result.iterator().hasNext()) {
                            // El punto se encuentra dentro de al menos uno de los pol�gonos en la capa
                            System.out.println("El punto est� dentro de al menos uno de los pol�gonos en la capa.");
                            Geometry pointGeometry = GeometryEngine.project(point, SpatialReferences.getWgs84());

                            // Itera sobre los resultados de la consulta
                            for (Feature feature : result) {
                                // Obt�n la geometr�a de la caracter�stica (pol�gono)
                            	  Geometry polygonGeometry = GeometryEngine.project(feature.getGeometry(), SpatialReferences.getWgs84());
                            	  Map<String, Object> attributes = feature.getAttributes();
                            	  Object nombre = attributes.get("nombre");
                            	  System.out.println(nombre);
                            	  resultado.setResultado("Tu direcci�n se encuentra dentro de nuestra cobertura de la tienda " + nombre.toString() + ", te invitamos a que sigas con tu pedido");  
                            }
                        
                        } else {
                            // El punto no se encuentra dentro de ning�n pol�gono en la capa
                            System.out.println("El punto no se encuentra dentro de ning�n pol�gono en la capa.");
                            resultado.setResultado("Por el momento tu direcci�n no se encuentra dentro de la cobertura de domicilio de nuestras tiendas, te invitamos a que te acerques a nuestro punto de venta m�s cercano para que puedas realizar tu pedido.");
                        }
                    } catch (Exception e) {
                        System.out.println("Error al realizar la consulta: " + e.getMessage());
                        //Enviaremos correo para notificar que hay problema con la API
                        Correo correo = new Correo();
        				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
        				ArrayList correos = new ArrayList();
        				correo.setAsunto("TENEMOS PROBLEMA CON LA API ARCGIS  ");
        				String correoEle = "jubote1@gmail.com";
        				correos.add(correoEle);
        				correo.setContrasena(infoCorreo.getClaveCorreo());
        				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
        				correo.setMensaje(" Se tiene prolema con la invocaci�n de la API ARCGIS  " + e.getMessage());
        				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
        				contro.enviarCorreo();
                    }
               
                
            } else {
                System.out.println("No se pudo geocodificar la direcci�n.");
                resultado.setResultado("Presentamos problemas encontrando tu direcci�n, recuerda seguir las instrucciones de nuestro BOT de mensajes, para que el sistema pueda validar bien tu direcci�n.");
            }
        } catch (Exception e) {
            System.out.println("Error al geocodificar: " + e.getMessage());
        }
        return(resultado);
	}
	
	public Ubicacion ubicarDireccionEnTiendaBatch(String direccion)
	{
		Ubicacion ubica = new Ubicacion(0,0);
		try {
			Parametro parametro;
			parametro =ParametrosDAO.obtenerParametro("APIARCGIS");
			System.out.println(parametro.getValorTexto());
			System.out.println("2.1 ANTES DE INICIAR ");
			ArcGISRuntimeEnvironment.setApiKey(parametro.getValorTexto());
			System.out.println("2.2 ANTES DE INICIAR ");
	        String serviceUrl = "http://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";
	        String queryUrl = "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0";
	        LocatorTask locatorTask = new LocatorTask(serviceUrl);
	        GeocodeParameters geocodeParameters = new GeocodeParameters();
	        geocodeParameters.getResultAttributeNames().add("*");
	        geocodeParameters.setMaxResults(1); // Limitamos el n�mero de resultados para obtener solo el m�s relevante
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
//                query.setGeometry(point); // Establece la geometr�a de la consulta (en este caso, un punto)
//                query.setSpatialRelationship(QueryParameters.SpatialRelationship.WITHIN);
// 
//               
//                    try {
//                        FeatureQueryResult result = serviceFeatureTable.queryFeaturesAsync(query,ServiceFeatureTable.QueryFeatureFields.LOAD_ALL).get();
//
//                        // Verifica si hay resultados en la consulta
//                        if (result.iterator().hasNext()) {
//                            // El punto se encuentra dentro de al menos uno de los pol�gonos en la capa
//                            System.out.println("El punto est� dentro de al menos uno de los pol�gonos en la capa.");
//                            Geometry pointGeometry = GeometryEngine.project(point, SpatialReferences.getWgs84());
//
//                            // Itera sobre los resultados de la consulta
//                            for (Feature feature : result) {
//                                // Obt�n la geometr�a de la caracter�stica (pol�gono)
//                            	  Geometry polygonGeometry = GeometryEngine.project(feature.getGeometry(), SpatialReferences.getWgs84());
//                            	  Map<String, Object> attributes = feature.getAttributes();
//                            	  Object nombre = attributes.get("nombre");
//                            	  System.out.println(nombre);
//                            	  resultado.setResultado("Tu direcci�n se encuentra dentro de nuestra cobertura de la tienda " + nombre.toString() + ", te invitamos a que sigas con tu pedido");  
//                            }
//                        
//                        } else {
//                            // El punto no se encuentra dentro de ning�n pol�gono en la capa
//                            System.out.println("El punto no se encuentra dentro de ning�n pol�gono en la capa.");
//                            resultado.setResultado("Por el momento tu direcci�n no se encuentra dentro de la cobertura de domicilio de nuestras tiendas, te invitamos a que te acerques a nuestro punto de venta m�s cercano para que puedas realizar tu pedido.");
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
//        				correo.setMensaje(" Se tiene prolema con la invocaci�n de la API ARCGIS  " + e.getMessage());
//        				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
//        				contro.enviarCorreo();
//                    }
               
                
            } else {
                System.out.println("No se pudo geocodificar la direcci�n.");
            }
        } catch (Exception e) {
            System.out.println("Error al geocodificar: " + e.getMessage());
        }
        return(ubica);
	}
	
	public static void main(String[] args)
	{
		Parametro parametro;
		parametro =ParametrosDAO.obtenerParametro("LIBRERIAARCGIS");
		System.out.println(parametro.getValorTexto());
		ArcGISRuntimeEnvironment.setInstallDirectory(parametro.getValorTexto());
	}
}

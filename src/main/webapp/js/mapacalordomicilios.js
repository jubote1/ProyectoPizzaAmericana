/**
 * Mapa de calor de domicilios: reutiliza ConsultarDireccionesDomicilio (solo pedidos de domicilio sin
 * cancelar) y GetTiendas.
 *
 * POR QUE ArcGIS Y NO GOOGLE MAPS
 *
 * La primera version se hizo con google.maps.visualization.HeatmapLayer, y Google la elimino de su API
 * -"no longer available ... as of version 3.65"-, asi que cargar la libreria ya no sirve de nada, sin
 * importar la clave que se use. ArcGIS si tiene un renderer de mapa de calor vigente
 * (esri/renderers/HeatmapRenderer) y ya esta licenciado y en uso en este sistema (Pedidos.html, arcgis.js);
 * se reusa la misma clave y version (4.27) para no meter un tercer proveedor de mapas.
 *
 * ArcGIS pinta el calor sobre un FeatureLayer, no sobre un GraphicsLayer: un GraphicsLayer no acepta
 * renderer. Por eso los puntos se cargan como un FeatureLayer "de cliente" (source en memoria, sin
 * servicio detras), que es la forma soportada de pintar datos que no viven en un Feature Service de Esri.
 */
var mcMapa = null;
var mcView = null;
var mcCapaCalor = null;
var mcArcgisListo = false;

//Misma clave que arcgis.js (Pedidos.html): ya esta licenciada y aprobada para este dominio.
var MC_ARCGIS_API_KEY = "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";

var MC_FeatureLayer = null;
var MC_HeatmapRenderer = null;
var MC_Graphic = null;

$(document).ready(function () {
	mcCargarTiendas();
	mcInitMap();
});

function mcInitMap() {
	require([
		"esri/config",
		"esri/Map",
		"esri/views/MapView",
		"esri/layers/FeatureLayer",
		"esri/renderers/HeatmapRenderer",
		"esri/Graphic"
	], function (esriConfig, EsriMap, MapView, FeatureLayer, HeatmapRenderer, Graphic) {
		esriConfig.apiKey = MC_ARCGIS_API_KEY;
		MC_FeatureLayer = FeatureLayer;
		MC_HeatmapRenderer = HeatmapRenderer;
		MC_Graphic = Graphic;

		mcMapa = new EsriMap({ basemap: "streets-navigation-vector" });
		mcView = new MapView({
			map: mcMapa,
			container: "mapaCalor",
			center: [-75.53611, 6.29139],
			zoom: 12,
			popup: { autoOpenEnabled: false }
		});
		mcArcgisListo = true;
	});
}

function mcCargarTiendas() {
	$.getJSON(server + 'GetTiendas', function (data) {
		var str = '<option value="">Escoja una tienda...</option>';
		for (var i = 0; i < data.length; i++) {
			str += '<option value="' + data[i].id + '">' + data[i].nombre + '</option>';
		}
		$('#selectTiendas').html(str);
	});
}

function mcAviso(texto, clase) {
	$('#mcAviso').html(texto ? ('<div class="alert ' + (clase || 'alert-warning') + '">' + texto + '</div>') : '');
}

function mcConsultar() {
	var idTienda = $('#selectTiendas').val();
	var fechaIni = $('#fechainicial').val();
	var fechaFin = $('#fechafinal').val();
	mcAviso('');

	if (!idTienda) {
		mcAviso('Escoja una tienda.');
		return;
	}
	if (!fechaIni || !fechaFin) {
		mcAviso('Escoja las dos fechas.');
		return;
	}
	if (!mcArcgisListo || !mcView) {
		mcAviso('El mapa todavía no ha cargado, espere un momento e intente de nuevo.');
		return;
	}

	mcAviso('Consultando...', 'alert-info');
	$.getJSON(server + 'ConsultarDireccionesDomicilio', {
		fechainicial: fechaIni,
		fechafinal: fechaFin,
		idtienda: idTienda
	}, function (data) {
		try {
			mcPintarCalor(data);
		} catch (ex) {
			mcAviso('No se pudo pintar el mapa de calor: ' + ex.message, 'alert-danger');
			$('#mcResumen').text('');
		}
	}).fail(function () {
		mcAviso('No se pudo consultar el central.', 'alert-danger');
	});
}

function mcPintarCalor(data) {
	if (mcCapaCalor) {
		mcMapa.remove(mcCapaCalor);
		mcCapaCalor = null;
	}

	var graficos = [];
	var oid = 1;
	for (var i = 0; i < data.length; i++) {
		var lat = parseFloat(data[i].latitud);
		var lng = parseFloat(data[i].longitud);
		if (!isNaN(lat) && !isNaN(lng) && (lat !== 0 || lng !== 0)) {
			graficos.push(new MC_Graphic({
				geometry: { type: "point", longitude: lng, latitude: lat },
				attributes: { ObjectID: oid }
			}));
			oid++;
		}
	}

	if (graficos.length === 0) {
		mcAviso('No hay pedidos de domicilio con coordenadas en ese rango para esa tienda.');
		$('#mcResumen').text('');
		return;
	}

	mcCapaCalor = new MC_FeatureLayer({
		source: graficos,
		objectIdField: "ObjectID",
		fields: [{ name: "ObjectID", type: "oid" }],
		geometryType: "point",
		spatialReference: { wkid: 4326 },
		renderer: new MC_HeatmapRenderer({
			//Radio y stops por defecto de Esri: van bien para decenas o cientos de puntos como este caso.
			colorStops: [
				{ ratio: 0, color: "rgba(0, 0, 0, 0)" },
				{ ratio: 0.2, color: "rgba(0, 0, 255, 0.6)" },
				{ ratio: 0.4, color: "rgba(0, 255, 255, 0.7)" },
				{ ratio: 0.6, color: "rgba(0, 255, 0, 0.8)" },
				{ ratio: 0.8, color: "rgba(255, 255, 0, 0.9)" },
				{ ratio: 1, color: "rgba(255, 0, 0, 1)" }
			],
			radius: 18,
			maxDensity: 0.02,
			minDensity: 0
		})
	});
	mcMapa.add(mcCapaCalor);
	//Sin forzar el zoom: que encuadre solo, segun donde queden los puntos. Un solo punto no tiene
	//"extent" (ancho y alto cero) y goTo fallaria si se le pidiera encuadrar sin zoom; en ese caso se
	//deja el zoom que ya trae el mapa.
	var opcionesGoTo = { target: graficos };
	if (graficos.length === 1) {
		opcionesGoTo.zoom = 15;
	}
	mcView.goTo(opcionesGoTo).catch(function () { });

	mcAviso('');
	$('#mcResumen').html('<b>' + graficos.length + '</b> de ' + data.length
		+ ' pedido(s) de domicilio tienen coordenadas y se ven en el mapa.'
		+ ' Entre más clientes pidan cerca de un mismo punto, más rojo se ve esa zona.');
}

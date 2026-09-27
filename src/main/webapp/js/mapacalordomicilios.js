/**
 * Mapa de calor de domicilios: reutiliza ConsultarDireccionesDomicilio (solo pedidos de domicilio sin
 * cancelar), GetTiendas, y los mismos tiendas.json/poligonos2.json que ya usa Pedidos.html para marcar el
 * punto de la tienda y el contorno de su zona.
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
 *
 * PUNTO Y ZONA DE LA TIENDA
 *
 * No se carga arcgis.js completo -esa pantalla ademas maneja tomar el pedido, arrastrar el marcador del
 * cliente, validar cobertura, etc., nada de lo cual aplica aca-. Se leen los mismos dos JSON estaticos
 * (tiendas.json: el punto de cada tienda; poligonos2.json: el contorno de cada zona) y se dibujan en una
 * capa aparte, SIEMPRE por encima del calor: el contorno va sin relleno para no taparle el color al
 * calor, solo la linea.
 *
 * El nombre de la tienda no se escribe igual en los tres lados -GetTiendas.nombre dice "America" o
 * "Manrique Piloto", tiendas.json dice "La America" o "Piloto", poligonos2.json dice "america_id" o
 * "piloto_id"-, asi que el cruce es por coincidencia de texto (normalizado, sin tildes) en cualquiera de
 * los dos sentidos, no por igualdad exacta. Bodega, Poblado y Medayoung no tienen los tres datos a la vez
 * -Bodega no es una tienda con domicilios, Poblado/Medayoung no tienen servidor local- y sencillamente no
 * se les dibuja lo que falte, sin error.
 */
var mcMapa = null;
var mcView = null;
var mcCapaCalor = null;
var mcCapaTienda = null;
var mcArcgisListo = false;

//Misma clave que arcgis.js (Pedidos.html): ya esta licenciada y aprobada para este dominio.
var MC_ARCGIS_API_KEY = "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";

var MC_FeatureLayer = null;
var MC_HeatmapRenderer = null;
var MC_Graphic = null;
var MC_GraphicsLayer = null;
var MC_SimpleMarkerSymbol = null;

var MC_TIENDAS = null;
var MC_POLIGONOS = null;

$(document).ready(function () {
	mcCargarTiendas();
	mcInitMap();
	mcCargarTiendasJson();
	mcCargarPoligonosJson();

	$('#selectTiendas').on('change', function () {
		mcDibujarTienda($('#selectTiendas option:selected').text());
	});
});

function mcInitMap() {
	require([
		"esri/config",
		"esri/Map",
		"esri/views/MapView",
		"esri/layers/FeatureLayer",
		"esri/layers/GraphicsLayer",
		"esri/renderers/HeatmapRenderer",
		"esri/symbols/SimpleMarkerSymbol",
		"esri/Graphic"
	], function (esriConfig, EsriMap, MapView, FeatureLayer, GraphicsLayer, HeatmapRenderer, SimpleMarkerSymbol, Graphic) {
		esriConfig.apiKey = MC_ARCGIS_API_KEY;
		MC_FeatureLayer = FeatureLayer;
		MC_HeatmapRenderer = HeatmapRenderer;
		MC_Graphic = Graphic;
		MC_GraphicsLayer = GraphicsLayer;
		MC_SimpleMarkerSymbol = SimpleMarkerSymbol;

		mcMapa = new EsriMap({ basemap: "streets-navigation-vector" });
		mcView = new MapView({
			map: mcMapa,
			container: "mapaCalor",
			center: [-75.53611, 6.29139],
			zoom: 12,
			popup: { autoOpenEnabled: false }
		});

		mcCapaTienda = new GraphicsLayer({ id: "mcCapaTienda", title: "Tienda" });
		mcMapa.add(mcCapaTienda);

		mcArcgisListo = true;
		//Si el usuario ya habia escogido tienda mientras ArcGIS terminaba de cargar.
		var nombreEscogido = $('#selectTiendas option:selected').text();
		if ($('#selectTiendas').val()) {
			mcDibujarTienda(nombreEscogido);
		}
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

/** El punto de cada tienda (el mismo tiendas.json de Pedidos.html). */
function mcCargarTiendasJson() {
	$.getJSON('tiendas.json', function (data) {
		MC_TIENDAS = data;
		mcReintentarDibujoInicial();
	}).fail(function () {
		MC_TIENDAS = [];
	});
}

/** El contorno de cada zona (el mismo poligonos2.json de Pedidos.html). */
function mcCargarPoligonosJson() {
	$.getJSON('poligonos2.json', function (data) {
		MC_POLIGONOS = data;
		mcReintentarDibujoInicial();
	}).fail(function () {
		MC_POLIGONOS = [];
	});
}

/** Los tres archivos (mapa, tiendas.json, poligonos2.json) cargan por separado; se dibuja en cuanto esten los tres. */
function mcReintentarDibujoInicial() {
	if (mcArcgisListo && MC_TIENDAS && MC_POLIGONOS && $('#selectTiendas').val()) {
		mcDibujarTienda($('#selectTiendas option:selected').text());
	}
}

/** Sin tildes y en minuscula, para comparar nombres que no se escriben igual en cada archivo. */
function mcNormalizar(texto) {
	return (texto || '').toString()
		.normalize('NFD').replace(/[̀-ͯ]/g, '')
		.toLowerCase()
		.replace(/[^a-z0-9]/g, '');
}

/** true si un nombre "contiene" al otro, en cualquiera de los dos sentidos (p.ej. "america" en "laamerica"). */
function mcCoincide(a, b) {
	if (!a || !b) { return false; }
	return a.indexOf(b) !== -1 || b.indexOf(a) !== -1;
}

/**
 * Pinta el punto de la tienda y el contorno de su zona (o zonas: Bello tiene una zona roja aparte). Si
 * alguno de los dos no se encuentra para esa tienda, se deja sin dibujar -no es un error, ver el
 * comentario de arriba sobre Bodega/Poblado/Medayoung-.
 */
function mcDibujarTienda(nombreTienda) {
	if (!mcArcgisListo || !mcCapaTienda || !MC_TIENDAS || !MC_POLIGONOS) {
		return;
	}
	mcCapaTienda.removeAll();
	if (!nombreTienda) {
		return;
	}
	var normTienda = mcNormalizar(nombreTienda);

	//El punto.
	var tienda = null;
	for (var i = 0; i < MC_TIENDAS.length; i++) {
		if (mcCoincide(mcNormalizar(MC_TIENDAS[i].title), normTienda) && MC_TIENDAS[i].coordinates) {
			tienda = MC_TIENDAS[i];
			break;
		}
	}
	if (tienda) {
		mcCapaTienda.add(new MC_Graphic({
			geometry: { type: "point", longitude: tienda.coordinates.lng, latitude: tienda.coordinates.lat },
			symbol: new MC_SimpleMarkerSymbol({
				style: "circle",
				color: [16, 47, 111, 1],
				size: 16,
				outline: { color: [255, 255, 255, 1], width: 2 }
			}),
			attributes: { tipo: "tienda" },
			popupTemplate: { title: tienda.title, content: "Tienda" }
		}));
	}

	//El o los contornos de zona (Bello tiene una zona roja aparte, ademas de la principal).
	var algunPoligono = false;
	for (var p = 0; p < MC_POLIGONOS.length; p++) {
		var poligono = MC_POLIGONOS[p];
		var idZona = mcNormalizar((poligono.id || '').replace(/_id$/, '').replace(/zonaroja/, ''));
		if (!mcCoincide(idZona, normTienda) || !poligono.coordinates) {
			continue;
		}
		algunPoligono = true;
		var color = poligono.color || [16, 47, 111, 1];
		mcCapaTienda.add(new MC_Graphic({
			geometry: { type: "polygon", rings: poligono.coordinates },
			symbol: {
				type: "simple-fill",
				//Sin relleno: es solo el contorno, para no taparle el color al mapa de calor.
				color: [0, 0, 0, 0],
				outline: { color: [color[0], color[1], color[2], 1], width: 2 }
			}
		}));
	}

	if (tienda || algunPoligono) {
		mcMapa.reorder(mcCapaTienda, mcMapa.layers.length - 1);
	}
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

	mcDibujarTienda($('#selectTiendas option:selected').text());

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
	//Al principio de la lista de capas: el punto y el contorno de la tienda (mcCapaTienda) quedan por
	//encima, y se le hace reorder de todas formas por si acaso.
	mcMapa.add(mcCapaCalor, 0);
	if (mcCapaTienda) {
		mcMapa.reorder(mcCapaTienda, mcMapa.layers.length - 1);
	}
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

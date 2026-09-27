/**
 * Mapa de calor de domicilios: reutiliza ConsultarDireccionesDomicilio (nuevo servicio, solo pedidos de
 * domicilio sin cancelar) y GetTiendas (ya usados por ConsultaPedidosMapa.html). La capa de calor es de
 * la libreria "visualization" de Google Maps, que las pantallas de mapa existentes no cargaban porque
 * pintaban marcador por marcador en vez de una densidad.
 */
var mcMapa = null;
var mcCapaCalor = null;

$(document).ready(function () {
	mcCargarTiendas();
});

function mcInitMap() {
	mcMapa = new google.maps.Map(document.getElementById('mapaCalor'), {
		zoom: 12,
		center: { lat: 6.29139, lng: -75.53611 },
		mapTypeId: google.maps.MapTypeId.ROADMAP
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
	if (!mcMapa) {
		mcAviso('El mapa todavía no ha cargado, espere un momento e intente de nuevo.');
		return;
	}

	mcAviso('Consultando...', 'alert-info');
	$.getJSON(server + 'ConsultarDireccionesDomicilio', {
		fechainicial: fechaIni,
		fechafinal: fechaFin,
		idtienda: idTienda
	}, function (data) {
		if (mcCapaCalor) {
			mcCapaCalor.setMap(null);
		}
		var puntos = [];
		for (var i = 0; i < data.length; i++) {
			var lat = parseFloat(data[i].latitud);
			var lng = parseFloat(data[i].longitud);
			if (!isNaN(lat) && !isNaN(lng) && (lat !== 0 || lng !== 0)) {
				puntos.push(new google.maps.LatLng(lat, lng));
			}
		}
		if (puntos.length === 0) {
			mcAviso('No hay pedidos de domicilio con coordenadas en ese rango para esa tienda.');
			$('#mcResumen').text('');
			return;
		}
		mcCapaCalor = new google.maps.visualization.HeatmapLayer({
			data: puntos,
			map: mcMapa,
			radius: 28
		});
		mcMapa.setCenter(puntos[0]);
		mcAviso('');
		$('#mcResumen').html('<b>' + data.length + '</b> pedido(s) de domicilio con ubicación en el rango escogido.'
			+ ' Entre más clientes pidan cerca de un mismo punto, más rojo se ve esa zona.');
	}).fail(function () {
		mcAviso('No se pudo consultar el central.', 'alert-danger');
	});
}

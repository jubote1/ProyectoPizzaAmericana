/* Tablero de ofertas y codigos. Lee el servicio TableroOfertas. */

function toEscapar(t) {
	if (t === null || t === undefined) { return ''; }
	return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function toMiles(n) {
	return String(Math.round(Number(n) || 0)).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function toPesos(n) { return ('$' + toMiles(n)); }

$(document).ready(function () {
	toCargar();
	$('#t-dias').change(toCargar);
});

function toCargar() {
	$('#t-error').hide();
	$.getJSON(server + 'TableroOfertas', { dias: $('#t-dias').val() }, function (d) {
		if (d.error) {
			$('#t-error').text(d.error).show();
			return;
		}
		toPintarAlertas(d.alertas || []);
		toPintarOfertas(d.ofertas || []);
		toPintarTiendas(d.tiendas || []);
	}).fail(function () {
		$('#t-error').text('No se pudo cargar el tablero. Verifique que tenga la sesi\u00f3n iniciada.').show();
	});
}

function toPintarAlertas(lista) {
	var html = '';
	for (var i = 0; i < lista.length; i++) {
		var a = lista[i];
		html += '<div class="alerta-t ' + toEscapar(a.nivel) + (a.cantidad > 0 ? ' hay' : '') + '">' +
			'<span class="num">' + toMiles(a.cantidad) + '</span><strong>' + toEscapar(a.titulo) + '</strong>' +
			'<div class="expl">' + toEscapar(a.explicacion) + '</div></div>';
	}
	$('#t-alertas').html(html === '' ? 'Sin alertas.' : html);
}

function toPintarOfertas(lista) {
	var cuerpo = $('#t-ofertas tbody').empty();
	if (lista.length === 0) {
		cuerpo.html('<tr><td colspan="9">No hay ofertas.</td></tr>');
		return;
	}
	for (var i = 0; i < lista.length; i++) {
		var o = lista[i];
		cuerpo.append('<tr>' +
			'<td>' + toEscapar(o.nombre) + (o.habilitada ? '' : ' <span class="text-muted">(deshabilitada)</span>') + '</td>' +
			'<td class="num-d">' + toMiles(o.emitidos) + '</td>' +
			'<td class="num-d">' + toMiles(o.usados) + '</td>' +
			'<td class="num-d">' + toMiles(o.vigentes) + '</td>' +
			'<td class="num-d">' + toMiles(o.vencidos) + '</td>' +
			'<td class="num-d">' + toMiles(o.anulados) + '</td>' +
			'<td class="num-d">' + toMiles(o.usos_ventana) + '</td>' +
			'<td class="num-d">' + toPesos(o.descontado) + '</td>' +
			'<td class="num-d">' + toPesos(o.ventas) + '</td></tr>');
	}
}

function toPintarTiendas(lista) {
	var cuerpo = $('#t-tiendas tbody').empty();
	if (lista.length === 0) {
		cuerpo.html('<tr><td colspan="3">Todav\u00eda no hay usos en el per\u00edodo.</td></tr>');
		return;
	}
	for (var i = 0; i < lista.length; i++) {
		var t = lista[i];
		cuerpo.append('<tr><td>' + toEscapar(t.nombre) + '</td><td class="num-d">' + toMiles(t.usos) +
			'</td><td class="num-d">' + toPesos(t.descontado) + '</td></tr>');
	}
}

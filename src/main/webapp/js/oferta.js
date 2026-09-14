/*
 * Pantalla de administracion de ofertas.
 *
 * Antes esta pantalla manejaba dos parametros -el nombre y la excepcion de
 * precio- y los otros veintiuno de la tabla oferta tocaba ponerlos a mano en la
 * base de datos. Ahora entran todos desde aqui.
 *
 * Hay un bloque que explica la oferta en palabras y avisa de las combinaciones
 * que suelen dar problema. Con veintitres parametros es muy facil armar una
 * oferta que en el papel se ve bien y en la tienda no funciona.
 */

var tablaOfertas;

$(document).ready(function () {
	tablaOfertas = $('#grid-oferta').DataTable({
		"order": [],
		"pageLength": 15
	});
	llenarSelectExcepcion();
	pintarOfertas();
	//El resumen se recalcula con cualquier cambio del formulario.
	$('#nombre, #selectExcepcion, #tipooferta, #habilitado, #contact, #descuentofijovalor, '
		+ '#descuentofijoporcentaje, #descuentoporcentajefuturo, #codigopromocional, #redparcial, '
		+ '#reintegro, #codigogeneral, #fechadesde, #fechahasta, #diascaducidad, #tipocaducidad, '
		+ '#controlahora, #horainicio, #horafin, #mensaje1, #mensaje2')
		.on('change keyup', pintarResumen);
	pintarResumen();
});

/** Los valores del formulario, con los nombres que espera el servlet. */
function datosDelFormulario() {
	return {
		idoferta: $('#idoferta').val(),
		nombreoferta: $('#nombre').val(),
		idexcepcion: $('#selectExcepcion').val(),
		tipooferta: $('#tipooferta').val(),
		habilitado: $('#habilitado').val(),
		contact: $('#contact').val(),
		descuentofijovalor: $('#descuentofijovalor').val(),
		descuentofijoporcentaje: $('#descuentofijoporcentaje').val(),
		descuentoporcentajefuturo: $('#descuentoporcentajefuturo').val(),
		codigopromocional: $('#codigopromocional').val(),
		redparcial: $('#redparcial').val(),
		reintegro: $('#reintegro').val(),
		codigogeneral: $('#codigogeneral').val(),
		fechadesde: $('#fechadesde').val(),
		fechahasta: $('#fechahasta').val(),
		diascaducidad: $('#diascaducidad').val(),
		tipocaducidad: $('#tipocaducidad').val(),
		controlahora: $('#controlahora').val(),
		horainicio: $('#horainicio').val(),
		horafin: $('#horafin').val(),
		mensaje1: $('#mensaje1').val(),
		mensaje2: $('#mensaje2').val()
	};
}

function pesos(valor) {
	var n = parseFloat(valor) || 0;
	return '$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 });
}

function textoFecha(iso) {
	if (!iso || iso.length < 10) { return ''; }
	var partes = iso.substring(0, 10).split('-');
	var meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto',
		'septiembre', 'octubre', 'noviembre', 'diciembre'];
	var m = parseInt(partes[1], 10) - 1;
	if (m < 0 || m > 11) { return iso; }
	return parseInt(partes[2], 10) + ' de ' + meses[m] + ' de ' + partes[0];
}

/**
 * La oferta explicada en palabras, y los avisos.
 *
 * El resumen es lo que permite darse cuenta de que la oferta quedo mal antes de
 * guardarla, que con veintitres parametros no es evidente mirando los campos.
 */
function pintarResumen() {
	var d = datosDelFormulario();
	var valor = parseFloat(d.descuentofijovalor) || 0;
	var pct = parseFloat(d.descuentofijoporcentaje) || 0;
	var pctFuturo = parseFloat(d.descuentoporcentajefuturo) || 0;
	var dias = parseInt(d.diascaducidad, 10) || 0;
	var frases = [];

	var beneficio = '';
	if (valor > 0) { beneficio = 'un bono de <strong>' + pesos(valor) + '</strong>'; }
	else if (pct > 0) { beneficio = '<strong>' + pct + '% de descuento</strong>'; }
	else if (pctFuturo > 0) { beneficio = '<strong>' + pctFuturo + '% para una compra futura</strong>'; }

	if (beneficio === '') {
		frases.push('Todav&iacute;a no tiene ning&uacute;n descuento definido.');
	} else {
		var conCodigo = (d.codigopromocional === 'S') ? ' con un c&oacute;digo &uacute;nico' : '';
		frases.push('Se le da al cliente ' + beneficio + conCodigo + '.');
	}

	if (dias > 0) {
		frases.push('Puede usarlo <strong>hasta ' + dias + ' d&iacute;as</strong> despu&eacute;s de que se le asigne.');
	} else if (d.tipocaducidad === 'P') {
		frases.push('<strong>No caduca por d&iacute;as</strong>: los d&iacute;as de caducidad est&aacute;n en cero.');
	}

	if (d.controlahora === 'S') {
		frases.push('Solo entre las <strong>' + (d.horainicio || '0') + ' y las ' + (d.horafin || '0') + ' horas</strong>.');
	}
	if (d.redparcial === 'S' && valor > 0) {
		frases.push('Si gasta menos de ' + pesos(valor) + ', <strong>le queda el saldo</strong> para otra compra.');
	}
	if (d.contact === 'S') {
		frases.push('Solo la puede asignar el <strong>contact center</strong>.');
	}
	if (d.fechahasta) {
		frases.push('Deja de poderse asignar el <strong>' + textoFecha(d.fechahasta) + '</strong>.');
	}
	if (d.habilitado === 'N') {
		frases.push('Est&aacute; <strong>deshabilitada</strong>: queda guardada pero nadie la puede asignar.');
	}

	$('#resumenOferta').html('<p style="margin:0 0 9px;">' + frases.join('</p><p style="margin:0 0 9px;">') + '</p>');
	pintarAvisos(d, valor, pct, pctFuturo, dias);
}

/** Las combinaciones que suelen dar problema en la tienda. */
function pintarAvisos(d, valor, pct, pctFuturo, dias) {
	var avisos = [];
	if (valor > 0 && pct > 0) {
		avisos.push('Tiene valor fijo <strong>y</strong> porcentaje. Revise cu&aacute;l de los dos quiere que aplique, '
			+ 'porque tener los dos suele terminar en un cobro equivocado.');
	}
	if (valor > 0 && d.controlahora === 'S') {
		avisos.push('Es un bono en pesos y solo aplica en una franja horaria. Confirme que la tienda lo sepa, '
			+ 'o el cliente va a llegar fuera de hora con el c&oacute;digo en la mano.');
	}
	if (d.habilitado === 'S' && !d.fechahasta) {
		avisos.push('Est&aacute; habilitada y <strong>sin fecha de fin</strong>: se va a poder asignar indefinidamente.');
	}
	if (dias === 0 && d.tipocaducidad === 'P' && (valor > 0 || pct > 0)) {
		avisos.push('Caduca por d&iacute;as pero los d&iacute;as est&aacute;n en <strong>cero</strong>. '
			+ 'El c&oacute;digo no va a vencer nunca.');
	}
	if (d.codigopromocional === 'N' && !d.codigogeneral) {
		avisos.push('No genera c&oacute;digo por cliente y tampoco tiene c&oacute;digo general. '
			+ 'Revise c&oacute;mo la va a reclamar el cliente.');
	}
	if (d.controlahora === 'S' && d.horainicio === d.horafin) {
		avisos.push('La hora de inicio y la de fin son la misma.');
	}
	if (d.fechadesde && d.fechahasta && d.fechadesde > d.fechahasta) {
		avisos.push('La fecha de inicio es <strong>posterior</strong> a la de fin.');
	}
	if (avisos.length === 0) {
		$('#avisosOferta').html('');
		return;
	}
	var html = '';
	for (var i = 0; i < avisos.length; i++) {
		html += '<div class="aviso-oferta">' + avisos[i] + '</div>';
	}
	$('#avisosOferta').html(html);
}

function llenarSelectExcepcion() {
	//El mismo servicio que usaba la pantalla anterior: devuelve idexcepcion y descripcion.
	$.getJSON(server + 'getExcepcionesPrecio', function (data) {
		var opciones = '<option value="0">Sin excepci&oacute;n de precio</option>';
		for (var i = 0; i < data.length; i++) {
			opciones += '<option value="' + data[i].idexcepcion + '">' + data[i].descripcion + '</option>';
		}
		$('#selectExcepcion').html(opciones);
	});
}

function pintarOfertas() {
	$.getJSON(server + 'CRUDOferta?idoperacion=8', function (data) {
		tablaOfertas.clear();
		for (var i = 0; i < data.length; i++) {
			var o = data[i];
			var descuento = '&mdash;';
			if (parseFloat(o.descuentofijovalor) > 0) { descuento = pesos(o.descuentofijovalor); }
			else if (parseFloat(o.descuentofijoporcentaje) > 0) { descuento = o.descuentofijoporcentaje + '%'; }
			else if (parseFloat(o.descuentoporcentajefuturo) > 0) { descuento = o.descuentoporcentajefuturo + '% futuro'; }
			var caducidad = (parseInt(o.diascaducidad, 10) > 0) ? (o.diascaducidad + ' d&iacute;as') : '&mdash;';
			var horario = (o.controlahora === 'S') ? (o.horainicio + ' a ' + o.horafin + ' h') : '&mdash;';
			var hab = (o.habilitado === 'S')
				? '<span class="sello-of si">S&iacute;</span>'
				: '<span class="sello-of no">No</span>';
			tablaOfertas.row.add([
				o.idoferta,
				escaparTexto(o.nombreoferta),
				escaparTexto(o.nombreexcepcion || ''),
				descuento,
				caducidad,
				horario,
				hab,
				o.asignadas,
				o.usadas,
				'<button class="btn btn-primary btn-xs" onclick="cargarOferta(' + o.idoferta + ')">Abrir</button>'
			]);
		}
		tablaOfertas.draw();
		window.ofertasCargadas = data;
	});
}

/** El nombre lo escribe una persona: se escapa antes de meterlo a la tabla. */
function escaparTexto(valor) {
	if (valor === null || valor === undefined) { return ''; }
	return String(valor).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function cargarOferta(idoferta) {
	var lista = window.ofertasCargadas || [];
	var o = null;
	for (var i = 0; i < lista.length; i++) {
		if (parseInt(lista[i].idoferta, 10) === parseInt(idoferta, 10)) { o = lista[i]; break; }
	}
	if (o === null) { return; }
	$('#idoferta').val(o.idoferta);
	$('#nombre').val(o.nombreoferta);
	$('#selectExcepcion').val(o.idexcepcion);
	$('#tipooferta').val(o.tipooferta || 'C');
	$('#habilitado').val(o.habilitado || 'S');
	$('#contact').val(o.contact || 'N');
	$('#descuentofijovalor').val(o.descuentofijovalor);
	$('#descuentofijoporcentaje').val(o.descuentofijoporcentaje);
	$('#descuentoporcentajefuturo').val(o.descuentoporcentajefuturo);
	$('#codigopromocional').val(o.codigopromocional || 'N');
	$('#redparcial').val(o.redparcial || 'N');
	$('#reintegro').val(o.reintegro || 'N');
	$('#codigogeneral').val(o.codigogeneral || '');
	$('#fechadesde').val((o.fechadesde || '').substring(0, 10));
	$('#fechahasta').val((o.fechahasta || '').substring(0, 10));
	$('#diascaducidad').val(o.diascaducidad);
	$('#tipocaducidad').val(o.tipocaducidad || 'P');
	$('#controlahora').val(o.controlahora || 'N');
	$('#horainicio').val(o.horainicio || 0);
	$('#horafin').val(o.horafin || 0);
	$('#mensaje1').val(o.mensaje1 || '');
	$('#mensaje2').val(o.mensaje2 || '');
	var vencidas = parseInt(o.asignadas, 10) - parseInt(o.usadas, 10);
	$('#comoVaOferta').html(
		'<table class="table table-sm" style="margin:0;font-size:13px;">'
		+ '<tr><td>Asignadas</td><td style="text-align:right;"><strong>' + o.asignadas + '</strong></td></tr>'
		+ '<tr><td>Usadas</td><td style="text-align:right;"><strong>' + o.usadas + '</strong></td></tr>'
		+ '<tr><td>Sin usar</td><td style="text-align:right;"><strong>' + (vencidas < 0 ? 0 : vencidas) + '</strong></td></tr>'
		+ '</table>');
	pintarResumen();
	$('html, body').animate({ scrollTop: 0 }, 200);
}

function limpiarOferta() {
	$('#idoferta').val(0);
	$('#nombre').val('');
	$('#selectExcepcion').val(0);
	$('#tipooferta').val('C');
	$('#habilitado').val('S');
	$('#contact').val('N');
	$('#descuentofijovalor').val(0);
	$('#descuentofijoporcentaje').val(0);
	$('#descuentoporcentajefuturo').val(0);
	$('#codigopromocional').val('S');
	$('#redparcial').val('N');
	$('#reintegro').val('N');
	$('#codigogeneral').val('');
	$('#fechadesde').val('');
	$('#fechahasta').val('');
	$('#diascaducidad').val(0);
	$('#tipocaducidad').val('P');
	$('#controlahora').val('N');
	$('#horainicio').val(0);
	$('#horafin').val(0);
	$('#mensaje1').val('');
	$('#mensaje2').val('');
	$('#comoVaOferta').html('Escoja una oferta de la lista para ver c&oacute;mo viene funcionando.');
	pintarResumen();
}

function guardarOferta() {
	var d = datosDelFormulario();
	if (!d.nombreoferta || d.nombreoferta.trim() === '') {
		$.alert('La oferta necesita un nombre.');
		return;
	}
	if (d.controlahora === 'S') {
		var hi = parseInt(d.horainicio, 10);
		var hf = parseInt(d.horafin, 10);
		if (isNaN(hi) || isNaN(hf) || hi < 0 || hi > 23 || hf < 0 || hf > 23) {
			$.alert('Las horas van de 0 a 23.');
			return;
		}
	}
	//Se manda por POST: los mensajes al cliente pueden traer tildes, comillas y
	//hasta 500 caracteres cada uno, y eso no cabe comodo en la URL.
	var nueva = (parseInt(d.idoferta, 10) === 0);
	d.idoperacion = nueva ? 1 : 2;
	$.ajax({
		url: server + 'CRUDOferta',
		type: 'POST',
		data: d,
		success: function () {
			pintarOfertas();
			$.alert(nueva ? 'Oferta creada.' : 'Oferta actualizada.');
			if (nueva) { limpiarOferta(); }
		},
		error: function () {
			$.alert('No se pudo guardar la oferta. Vuelva a intentarlo.');
		}
	});
}

/*
 * Compensaciones por tiempo.
 *
 * Lista los pedidos a domicilio que se demoraron, deja escoger a quienes se les manda una disculpa y la
 * oferta con la que se compensa.
 *
 * De la pantalla al servidor viaja SOLO que pedidos se escogieron (tienda:pedido) y con que oferta. El
 * servidor vuelve a leer los tiempos, el cliente y el correo al enviar: nada de lo que se ve aqui se toma
 * como cierto.
 *
 * ES5 -var y function-: convive con jQuery 1.11.
 */

var ctPedidos = [];       // lo que devolvio la consulta
var ctSeleccion = {};     // "idtienda:idpedido" -> true
var ctConsulta = null;    // los parametros con los que se consulto: son los que se mandan al enviar
var ctOfertas = [];
var ctReloj = null;

$(document).ready(function () {
	var ayer = new Date();
	ayer.setDate(ayer.getDate() - 1);
	$('#ct-fecha').val(ctFechaIso(ayer)).attr('max', ctFechaIso(ayer));

	ctCargarTiendas();
	ctCargarOfertas();

	$('#ct-buscar').click(ctBuscar);
	$('#ct-todos').click(ctEscogerTodos);
	$('#ct-ninguno').click(ctQuitarSeleccion);
	$('#ct-oferta').change(ctCambioOferta);
	$('#ct-vista').click(ctVistaPrevia);
	$('#ct-enviar').click(ctEnviar);

	//Los criterios cambian: lo que hay en la tabla ya no corresponde a lo que dicen los campos, y
	//enviar contra eso mandaria algo que nadie aprobo.
	$('#ct-fecha, #ct-minutos, #ct-tienda, #ct-soloexceso').on('change keyup', ctInvalidar);
});

// ===========================================================================
// Ayudas
// ===========================================================================

function ctEscapar(t) {
	return ($('<div/>').text(t === null || t === undefined ? '' : t).html());
}

function ctMiles(n) {
	var texto = String(Math.round(Number(n) || 0));
	var salida = '';
	for (var i = 0; i < texto.length; i++) {
		if (i > 0 && (texto.length - i) % 3 === 0) { salida += '.'; }
		salida += texto.charAt(i);
	}
	return (salida);
}

function ctFechaIso(d) {
	function dos(n) { return ((n < 10 ? '0' : '') + n); }
	return (d.getFullYear() + '-' + dos(d.getMonth() + 1) + '-' + dos(d.getDate()));
}

function ctMensaje(clase, texto) {
	$('#ct-mensaje').html('<div class="ct-aviso ' + clase + '">' + texto + '</div>');
}

function ctClave(p) {
	return (p.idtienda + ':' + p.idpedido);
}

// ===========================================================================
// Cargas
// ===========================================================================

function ctCargarTiendas() {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'tiendas' }, function (d) {
		var lista = (d && d.tiendas) ? d.tiendas : [];
		var html = '<option value="0">Todas</option>';
		for (var i = 0; i < lista.length; i++) {
			html += '<option value="' + lista[i].idtienda + '">' + ctEscapar(lista[i].nombre) + '</option>';
		}
		$('#ct-tienda').html(html);
	});
}

function ctCargarOfertas() {
	$.getJSON(server + 'CompensacionTiempo', { accion: 'ofertas' }, function (d) {
		if (d && d.error) { return; }
		ctOfertas = (d && d.ofertas) ? d.ofertas : [];
		var html = '<option value="0">Escoja una oferta...</option>';
		for (var i = 0; i < ctOfertas.length; i++) {
			var o = ctOfertas[i];
			var vale = o.valor > 0 ? ('$' + ctMiles(o.valor)) : (o.porcentaje > 0 ? (o.porcentaje + '%') : '');
			html += '<option value="' + o.idoferta + '">' + ctEscapar(o.nombre) +
				(vale ? ' - ' + vale : '') + '</option>';
		}
		$('#ct-oferta').html(html);
	});
}

function ctOfertaDe(id) {
	for (var i = 0; i < ctOfertas.length; i++) {
		if (String(ctOfertas[i].idoferta) === String(id)) { return (ctOfertas[i]); }
	}
	return (null);
}

function ctCambioOferta() {
	var o = ctOfertaDe($('#ct-oferta').val());
	$('#ct-vista').prop('disabled', !o);
	$('#ct-correo').hide();
	if (o) {
		$('#ct-oferta-nota').html('El c&oacute;digo caduca a los <b>' + o.dias + ' d&iacute;as</b> de enviado' +
			(o.max_emision > 0 ? '. Se han emitido ' + ctMiles(o.emitidos) + ' de ' + ctMiles(o.max_emision) + '.' : '.'));
	}
	ctPintarBotonEnvio();
}

// ===========================================================================
// Buscar
// ===========================================================================

function ctInvalidar() {
	if (ctConsulta === null) { return; }
	ctConsulta = null;
	ctPedidos = [];
	ctSeleccion = {};
	$('#ct-tabla tbody').html('<tr><td colspan="11" class="ct-nota">Cambi&oacute; el filtro: oprima Buscar.</td></tr>');
	$('#ct-resumen').html('');
	$('#ct-sindatos').html('');
	$('#ct-todos, #ct-ninguno').prop('disabled', true);
	ctPintarBotonEnvio();
}

function ctBuscar() {
	var fecha = $('#ct-fecha').val();
	var minutos = parseInt($('#ct-minutos').val(), 10);
	if (!fecha) { ctMensaje('ct-aviso-mal', 'Escoja el d&iacute;a.'); return; }
	if (!(minutos > 0)) { ctMensaje('ct-aviso-mal', 'Ponga a partir de cu&aacute;ntos minutos.'); return; }
	$('#ct-mensaje').html('');

	var datos = {
		accion: 'consultar',
		fecha: fecha,
		minutos: minutos,
		idtienda: $('#ct-tienda').val() || 0,
		soloexceso: $('#ct-soloexceso').is(':checked') ? 'S' : 'N'
	};
	$('#ct-buscar').prop('disabled', true).text('Buscando...');
	$.getJSON(server + 'CompensacionTiempo', datos, function (d) {
		$('#ct-buscar').prop('disabled', false).text('Buscar los pedidos');
		if (d.error) {
			ctMensaje('ct-aviso-mal', ctEscapar(d.error));
			return;
		}
		ctConsulta = { fecha: fecha, minutos: minutos, soloexceso: datos.soloexceso };
		ctPedidos = d.pedidos || [];
		ctSeleccion = {};
		ctPintar(d);
	}).fail(function () {
		$('#ct-buscar').prop('disabled', false).text('Buscar los pedidos');
		ctMensaje('ct-aviso-mal', 'No se pudo consultar. Intente de nuevo.');
	});
}

function ctPintar(d) {
	var resumen =
		'<div class="row">' +
		'<div class="col-xs-4"><div class="ct-cifra"><div class="valor">' + ctMiles(d.total) +
			'</div><div class="rotulo">pedidos</div></div></div>' +
		'<div class="col-xs-4"><div class="ct-cifra"><div class="valor">' + ctMiles(d.pueden_recibir) +
			'</div><div class="rotulo">se les puede escribir</div></div></div>' +
		'<div class="col-xs-4"><div class="ct-cifra"><div class="valor">' + ctMiles(d.ya_compensados) +
			'</div><div class="rotulo">ya compensados</div></div></div>' +
		'</div>';
	if (d.truncado) {
		resumen += '<div class="ct-peligro">Son m&aacute;s de los que caben en la lista: se muestran los ' +
			'que m&aacute;s se demoraron. Suba los minutos para ver el resto.</div>';
	}
	$('#ct-resumen').html(resumen);

	var sin = d.tiendas_sin_datos || [];
	if (sin.length > 0) {
		$('#ct-sindatos').html('<div class="ct-peligro"><b>Sin datos de ese d&iacute;a:</b> ' +
			ctEscapar(sin.join(', ')) + '. Su lista puede estar incompleta: la r&eacute;plica de esa tienda ' +
			'no ha llegado.</div>');
	} else {
		$('#ct-sindatos').html('');
	}

	var cuerpo = $('#ct-tabla tbody');
	cuerpo.empty();
	if (ctPedidos.length === 0) {
		cuerpo.html('<tr><td colspan="11" class="ct-nota">Ning&uacute;n pedido pas&oacute; de ese tiempo ese d&iacute;a. ' +
			'Buena noticia.</td></tr>');
	}
	for (var i = 0; i < ctPedidos.length; i++) {
		var p = ctPedidos[i];
		var fila = $('<tr>');
		var casilla = $('<input type="checkbox">').prop('disabled', !p.puede).data('clave', ctClave(p));
		casilla.change(function () {
			ctSeleccion[$(this).data('clave')] = this.checked;
			ctPintarBotonEnvio();
		});
		if (!p.puede) { fila.addClass('ct-no'); }
		fila.append($('<td>').append(casilla));
		fila.append($('<td>').text(p.tienda));
		fila.append($('<td class="ct-num">').text(p.idpedido));
		fila.append($('<td>').text(p.hora_pedido));
		fila.append($('<td>').text(p.hora_entrega));
		fila.append($('<td class="ct-num">').text(p.prometido));
		fila.append($('<td class="ct-num ct-mucho">').text(p.minutos));
		fila.append($('<td class="ct-num">').text(p.exceso > 0 ? '+' + p.exceso : '0'));
		fila.append($('<td>').text(p.cliente));
		fila.append($('<td>').text(p.email));
		fila.append($('<td>').text(p.puede ? 'Se le puede escribir' : p.motivo));
		cuerpo.append(fila);
	}
	$('#ct-todos, #ct-ninguno').prop('disabled', ctPedidos.length === 0);
	ctPintarBotonEnvio();
}

// ===========================================================================
// Seleccion y envio
// ===========================================================================

function ctEscogerTodos() {
	$('#ct-tabla tbody input[type=checkbox]:not(:disabled)').each(function () {
		this.checked = true;
		ctSeleccion[$(this).data('clave')] = true;
	});
	ctPintarBotonEnvio();
}

function ctQuitarSeleccion() {
	$('#ct-tabla tbody input[type=checkbox]').each(function () { this.checked = false; });
	ctSeleccion = {};
	ctPintarBotonEnvio();
}

function ctEscogidos() {
	var lista = [];
	for (var k in ctSeleccion) {
		if (ctSeleccion.hasOwnProperty(k) && ctSeleccion[k]) { lista.push(k); }
	}
	return (lista);
}

function ctPintarBotonEnvio() {
	var n = ctEscogidos().length;
	var oferta = ctOfertaDe($('#ct-oferta').val());
	var boton = $('#ct-enviar');
	if (ctConsulta === null || n === 0) {
		boton.prop('disabled', true).text('Escoja pedidos y una oferta');
	} else if (!oferta) {
		boton.prop('disabled', true).text('Escoja la oferta (a la izquierda)');
	} else {
		boton.prop('disabled', false)
			.text('Enviar la disculpa a ' + ctMiles(n) + (n === 1 ? ' pedido' : ' pedidos'));
	}
}

function ctVistaPrevia() {
	var id = $('#ct-oferta').val();
	if (!ctOfertaDe(id)) { return; }
	$.getJSON(server + 'CompensacionTiempo',
		{ accion: 'vistaprevia', idoferta: id, fecha: $('#ct-fecha').val() }, function (d) {
		if (d.error) { ctMensaje('ct-aviso-mal', ctEscapar(d.error)); return; }
		$('#ct-correo-asunto').html('<b>Asunto:</b> ' + ctEscapar(d.asunto) +
			'<br><span class="ct-nota">Con datos de ejemplo. El nombre, el c&oacute;digo y el d&iacute;a ' +
			'son los de cada cliente.</span>');
		$('#ct-correo-cuerpo').html(d.html);
		$('#ct-correo').show();
		$('html, body').animate({ scrollTop: $('#ct-correo').offset().top - 20 }, 300);
	});
}

function ctEnviar() {
	var escogidos = ctEscogidos();
	var oferta = ctOfertaDe($('#ct-oferta').val());
	if (ctConsulta === null || escogidos.length === 0 || !oferta) { return; }

	var segundos = 30;
	var texto = 'Va a pedirle disculpas a ' + escogidos.length + ' pedido(s) del ' + ctConsulta.fecha +
		' con la oferta "' + oferta.nombre + '".\n\n' +
		'Cada cliente recibe SU código personal. Una persona con dos pedidos recibe un solo correo.\n' +
		'El correo sale de a uno, cada ' + segundos + ' segundos.\n\n' +
		'Esto no se puede deshacer. Continuar?';
	if (!confirm(texto)) { return; }

	$('#ct-enviar').prop('disabled', true).text('Enviando...');
	$('#ct-aviso-envio').html('');
	$.post(server + 'CompensacionTiempo', {
		accion: 'enviar',
		fecha: ctConsulta.fecha,
		minutos: ctConsulta.minutos,
		soloexceso: ctConsulta.soloexceso,
		idoferta: oferta.idoferta,
		pedidos: escogidos.join(',')
	}, function (d) {
		if (d.error) {
			$('#ct-aviso-envio').html('<div class="ct-aviso ct-aviso-mal">' + ctEscapar(d.error) + '</div>');
			ctPintarBotonEnvio();
			return;
		}
		$('#ct-aviso-envio').html('<div class="ct-aviso ct-aviso-ok">' + ctEscapar(d.mensaje) + '</div>');
		$('#ct-avance').show();
		ctSeguirAvance(d.idenvio);
		//Lo recien enviado ya no es escogible: se vuelve a consultar para que quede "ya compensado".
		ctBuscarSinLimpiarMensajes();
	}, 'json').fail(function () {
		$('#ct-aviso-envio').html('<div class="ct-aviso ct-aviso-mal">No se pudo enviar. Intente de nuevo.</div>');
		ctPintarBotonEnvio();
	});
}

function ctBuscarSinLimpiarMensajes() {
	if (ctConsulta === null) { return; }
	var datos = {
		accion: 'consultar',
		fecha: ctConsulta.fecha,
		minutos: ctConsulta.minutos,
		idtienda: $('#ct-tienda').val() || 0,
		soloexceso: ctConsulta.soloexceso
	};
	$.getJSON(server + 'CompensacionTiempo', datos, function (d) {
		if (d.error) { return; }
		ctPedidos = d.pedidos || [];
		ctSeleccion = {};
		ctPintar(d);
	});
}

function ctSeguirAvance(idEnvio) {
	if (ctReloj) { clearInterval(ctReloj); }
	function mirar() {
		$.getJSON(server + 'EnvioPublicidad', { accion: 'avance', idenvio: idEnvio }, function (d) {
			if (d.error) { return; }
			var hechos = d.enviados + d.fallidos;
			var pct = d.publico > 0 ? Math.round(hechos * 100 / d.publico) : 0;
			$('#ct-barra-int').css('width', pct + '%');
			var texto = ctMiles(d.enviados) + ' enviados de ' + ctMiles(d.publico);
			if (d.fallidos > 0) { texto += ' &middot; ' + ctMiles(d.fallidos) + ' fallidos'; }
			texto += ' &middot; ' + ctEscapar(d.estado) + ' &middot; env&iacute;o ' + idEnvio +
				' (lo ve en Env&iacute;o de Publicidad, pesta&ntilde;a Campa&ntilde;as anteriores)';
			$('#ct-avance-texto').html(texto);
			if (d.estado === 'TERMINADA' || d.estado === 'CANCELADA') {
				clearInterval(ctReloj);
				ctReloj = null;
				ctBuscarSinLimpiarMensajes();
			}
		});
	}
	mirar();
	ctReloj = setInterval(mirar, 5000);
}

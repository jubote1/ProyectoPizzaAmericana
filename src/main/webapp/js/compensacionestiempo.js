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
var ctTolerancia = 10;   // el margen (%) sobre lo prometido; lo manda el servidor

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

	//El resumen de cumplimiento: se pinta solo al abrir (ayer, todas las tiendas) y se refresca cuando
	//cambia el dia, el alcance o los minutos. No depende de que se busque la lista.
	$('#ct-alcance').change(ctResumen);
	$('#ct-fecha').on('change', ctResumen);
	$('#ct-minutos').on('keyup change', ctResumenConEspera);
	$('#ct-res-vertabla').click(function () {
		ctVerTabla = !ctVerTabla;
		ctPintarResumen();
	});
	ctResumen();

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

/* El margen sobre la promesa que decide quien es incumplido (lo manda el servidor, se cambia en parametros). */
function ctPonerTolerancia(t) {
	var n = parseInt(t, 10);
	if (isNaN(n)) { return; }
	ctTolerancia = n;
	$('.ct-tol').text(n);
	$('#ct-tol-ej').text(Math.floor(60 * (100 + n) / 100));
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
	$('#ct-tabla tbody').html('<tr><td colspan="12" class="ct-nota">Cambi&oacute; el filtro: oprima Buscar.</td></tr>');
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
	ctResumen();

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
		ctPonerTolerancia(d.tolerancia);
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
		cuerpo.html('<tr><td colspan="12" class="ct-nota">Ning&uacute;n pedido pas&oacute; de ese tiempo ese d&iacute;a. ' +
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
		fila.append($('<td class="ct-num">').text(p.limite));
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

// ===========================================================================
// Cumplimiento de la promesa de tiempo: general y por tienda
//
// Barras apiladas horizontales, una por tienda: cumplido (verde) e incumplido (rojo) sobre el total de lo
// que se entrego. La peor tienda sale arriba: es la que hay que mirar. Los dos colores son los de ESTADO
// y van siempre con icono y texto, nunca solos. Cada valor tambien esta en la vista de tabla.
// ===========================================================================

var ctResumenDatos = null;
var ctVerTabla = false;
var ctTemporizadorResumen = null;

function ctResumen() {
	var fecha = $('#ct-fecha').val();
	var minutos = parseInt($('#ct-minutos').val(), 10) || 0;
	if (!fecha) { return; }
	$('#ct-resumen-panel').addClass('cargando');
	$.getJSON(server + 'CompensacionTiempo', {
		accion: 'resumen',
		fecha: fecha,
		dias: $('#ct-alcance').val() || 1,
		minutos: minutos
	}, function (d) {
		$('#ct-resumen-panel').removeClass('cargando');
		if (d.error) {
			ctResumenDatos = null;
			$('#ct-res-estado').html('<div class="ct-aviso ct-aviso-mal">' + ctEscapar(d.error) + '</div>');
			$('#ct-res-cuerpo').hide();
			return;
		}
		$('#ct-res-estado').html('');
		$('#ct-res-cuerpo').show();
		ctPonerTolerancia(d.tolerancia);
		ctResumenDatos = d;
		ctPintarResumen();
	}).fail(function () {
		$('#ct-resumen-panel').removeClass('cargando');
		$('#ct-res-estado').html('<div class="ct-aviso ct-aviso-mal">No se pudo consultar el resumen.</div>');
	});
}

/* Para campos que se teclean: no se consulta en cada letra. */
function ctResumenConEspera() {
	if (ctTemporizadorResumen) { clearTimeout(ctTemporizadorResumen); }
	ctTemporizadorResumen = setTimeout(ctResumen, 500);
}

function ctPorcentajeTexto(p) {
	return (String(p).replace('.', ',') + '%');
}

function ctRangoTexto(d) {
	if (d.dias === 1) { return ('el ' + ctFechaCorta(d.hasta)); }
	return ('del ' + ctFechaCorta(d.desde) + ' al ' + ctFechaCorta(d.hasta));
}

function ctFechaCorta(iso) {
	var m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '');
	return (m ? (m[3] + '/' + m[2] + '/' + m[1]) : (iso || ''));
}

function ctPintarResumen() {
	var d = ctResumenDatos;
	if (!d) { return; }
	var g = d.general;

	if (g.total === 0) {
		$('#ct-res-hero').html('<div class="ct-nota" style="font-size:13px;">No hay pedidos entregados y medibles ' +
			ctRangoTexto(d) + '.</div>');
		$('#ct-res-tiles').html('');
		$('#ct-res-tiendas').html('');
		$('#ct-tendencia').html('');
		$('#ct-res-tabla').html('');
		ctAvisoSinDatos(d);
		return;
	}

	// El numero que encabeza la vista: uno solo.
	$('#ct-res-hero').html(
		'<div class="ct-hero">' + ctEscapar(ctPorcentajeTexto(g.porcentaje)) + '</div>' +
		'<div class="ct-hero-rotulo">de los pedidos cumplieron la promesa de tiempo ' + ctEscapar(ctRangoTexto(d)) + '</div>');

	var tiles = [
		{ valor: ctMiles(g.total), rotulo: 'pedidos entregados' },
		{ valor: '✓ ' + ctMiles(g.cumplidos), rotulo: 'cumplidos' },
		{ valor: '✗ ' + ctMiles(g.incumplidos), rotulo: 'incumplidos' },
		{ valor: String(g.promedio).replace('.', ','), rotulo: 'minutos en promedio' }
	];
	if (d.umbral > 0) {
		tiles.push({ valor: ctMiles(g.sobre_umbral), rotulo: 'pasaron de ' + d.umbral + ' min' });
	}
	var htmlTiles = '<div style="display:flex;flex-wrap:wrap;">';
	for (var i = 0; i < tiles.length; i++) {
		htmlTiles += '<div style="flex:1 1 96px;"><div class="ct-cifra"><div class="valor">' +
			ctEscapar(tiles[i].valor) + '</div><div class="rotulo">' + ctEscapar(tiles[i].rotulo) +
			'</div></div></div>';
	}
	htmlTiles += '</div>';
	$('#ct-res-tiles').html(htmlTiles);

	ctPintarTiendas(d);
	ctPintarTendencia(d);
	ctPintarTabla(d);
	ctAvisoSinDatos(d);
	$('#ct-res-tiendas').toggle(!ctVerTabla);
	$('#ct-res-ayuda').toggle(!ctVerTabla);
	$('#ct-tendencia').toggle(!ctVerTabla);
	$('#ct-res-tabla').toggle(ctVerTabla);
	$('#ct-res-vertabla').text(ctVerTabla ? 'Ver la gráfica' : 'Ver como tabla');
}

function ctAvisoSinDatos(d) {
	var sin = d.tiendas_sin_datos || [];
	if (sin.length > 0) {
		$('#ct-res-estado').html('<div class="ct-peligro"><b>Sin datos de ' + ctEscapar(ctFechaCorta(d.hasta)) +
			':</b> ' + ctEscapar(sin.join(', ')) + '. No cuentan en el resumen de ese día: la réplica ' +
			'de esa tienda no ha llegado.</div>');
	}
}

/* Una barra por tienda. Hecha con divs: crece sola con el ancho y cada segmento es su propio blanco de hover. */
function ctPintarTiendas(d) {
	var cont = $('#ct-res-tiendas');
	cont.empty();
	var tiendas = d.tiendas || [];
	for (var i = 0; i < tiendas.length; i++) {
		var t = tiendas[i];
		var pBueno = t.total > 0 ? (t.cumplidos * 100 / t.total) : 0;
		var pMalo = t.total > 0 ? (t.incumplidos * 100 / t.total) : 0;

		var fila = $('<div class="ct-fila" tabindex="0">').data('t', t);
		fila.append($('<div class="ct-fila-nombre">').text(t.nombre).attr('title', t.nombre));
		var barra = $('<div class="ct-fila-barra">');
		if (t.cumplidos > 0) {
			barra.append($('<div class="ct-seg bueno">').css('width', pBueno + '%')
				.toggleClass('ultimo', t.incumplidos === 0));
		}
		if (t.incumplidos > 0) {
			barra.append($('<div class="ct-seg malo ultimo">').css('width', pMalo + '%'));
		}
		fila.append(barra);
		// El valor va al final de la barra, fuera de ella: dentro de un segmento chico no cabria.
		var valor = $('<div class="ct-fila-valor">');
		valor.append($('<b>').text(ctPorcentajeTexto(t.porcentaje)));
		valor.append($('<span>').text(' · ' + ctMiles(t.cumplidos) + ' de ' + ctMiles(t.total)));
		fila.append(valor);

		fila.on('mousemove', ctTooltipTienda).on('mouseleave blur', ctOcultarTooltip)
			.on('focus', ctTooltipTienda)
			.on('click keydown', function (e) {
				if (e.type === 'keydown' && e.which !== 13 && e.which !== 32) { return; }
				e.preventDefault();
				ctIrATienda($(this).data('t'));
			});
		cont.append(fila);
	}
}

/* Clic en una tienda: se filtra la lista de abajo a esa tienda y se busca. */
function ctIrATienda(t) {
	$('#ct-tienda').val(String(t.clave));
	ctInvalidar();
	ctBuscar();
}

function ctTooltipTienda(e) {
	var t = $(this).data('t');
	var tip = $('#ct-tip');
	tip.empty();
	tip.append($('<div class="t-nombre">').text(t.nombre));
	var f1 = $('<div>');
	f1.append($('<span class="t-valor">').text('✓ ' + ctMiles(t.cumplidos)));
	f1.append($('<span class="t-sec">').text(' cumplidos (' + ctPorcentajeTexto(t.porcentaje) + ')'));
	tip.append(f1);
	var f2 = $('<div>');
	f2.append($('<span class="t-valor">').text('✗ ' + ctMiles(t.incumplidos)));
	f2.append($('<span class="t-sec">').text(' incumplidos (' +
		ctPorcentajeTexto(Math.round((100 - t.porcentaje) * 10) / 10) + ')'));
	tip.append(f2);
	tip.append($('<div class="t-sec">').text('Promedio ' + String(t.promedio).replace('.', ',') +
		' min · el más lento ' + t.maximo + ' min'));
	var x = (e && e.clientX !== undefined) ? e.clientX : ($(this).offset().left + 40);
	var y = (e && e.clientY !== undefined) ? e.clientY : ($(this).offset().top - $(window).scrollTop());
	tip.css({ display: 'block', left: Math.min(x + 14, $(window).width() - 280), top: y + 14 });
}

function ctOcultarTooltip() {
	$('#ct-tip').hide();
}

/* La evolucion diaria del % cumplido. Solo cuando el rango son varios dias. Una linea: un solo dato. */
function ctPintarTendencia(d) {
	var dias = d.por_dia || [];
	var cont = $('#ct-tendencia');
	if (d.dias === 1 || dias.length < 2) {
		cont.html('');
		return;
	}
	var W = 640, H = 170, mi = 40, md = 54, ms = 12, mb = 28;
	var iw = W - mi - md, ih = H - ms - mb;
	function x(i) { return (mi + (dias.length === 1 ? iw / 2 : i * iw / (dias.length - 1))); }
	function y(p) { return (ms + ih - (p / 100) * ih); }

	var s = '<div class="ct-leyenda" style="margin-top:0;">Cumplimiento por d&iacute;a (% de pedidos que cumplieron)</div>';
	s += '<svg viewBox="0 0 ' + W + ' ' + H + '" role="img" aria-label="Cumplimiento por d&iacute;a">';
	// Rejilla: tres lineas, solidas y recesivas.
	var marcas = [0, 50, 100];
	for (var m = 0; m < marcas.length; m++) {
		s += '<line x1="' + mi + '" x2="' + (W - md) + '" y1="' + y(marcas[m]) + '" y2="' + y(marcas[m]) +
			'" stroke="#E1E0D9" stroke-width="1"/>';
		s += '<text x="' + (mi - 6) + '" y="' + (y(marcas[m]) + 4) + '" text-anchor="end" font-size="10.5" ' +
			'fill="#898781">' + marcas[m] + '%</text>';
	}
	var puntos = '';
	for (var i = 0; i < dias.length; i++) {
		puntos += (i === 0 ? '' : ' ') + x(i) + ',' + y(dias[i].porcentaje);
	}
	s += '<polyline points="' + puntos + '" fill="none" stroke="#2a78d6" stroke-width="2" ' +
		'stroke-linejoin="round" stroke-linecap="round"/>';
	// El ultimo dia: punto con anillo del color de la superficie y su valor, que es el dato que se lee.
	var u = dias.length - 1;
	s += '<circle cx="' + x(u) + '" cy="' + y(dias[u].porcentaje) + '" r="5" fill="#2a78d6" stroke="#FFFFFF" stroke-width="2"/>';
	s += '<text x="' + (x(u) + 10) + '" y="' + (y(dias[u].porcentaje) + 4) + '" font-size="12" font-weight="700" ' +
		'fill="#14181F">' + ctPorcentajeTexto(dias[u].porcentaje) + '</text>';
	// Fechas: la primera y la ultima.
	s += '<text x="' + x(0) + '" y="' + (H - 8) + '" text-anchor="start" font-size="10.5" fill="#898781">' +
		ctFechaCorta(dias[0].clave).substring(0, 5) + '</text>';
	s += '<text x="' + x(u) + '" y="' + (H - 8) + '" text-anchor="end" font-size="10.5" fill="#898781">' +
		ctFechaCorta(dias[u].clave).substring(0, 5) + '</text>';
	// Blancos de hover: una columna transparente por dia, mas ancha que el punto.
	var ancho = dias.length === 1 ? iw : iw / (dias.length - 1);
	for (var k = 0; k < dias.length; k++) {
		s += '<rect class="ct-dia" data-i="' + k + '" x="' + (x(k) - ancho / 2) + '" y="' + ms + '" width="' +
			ancho + '" height="' + ih + '" fill="transparent" tabindex="0"/>';
	}
	s += '<line id="ct-cruz" x1="0" x2="0" y1="' + ms + '" y2="' + (ms + ih) + '" stroke="#C3C2B7" ' +
		'stroke-width="1" style="display:none;pointer-events:none;"/>';
	s += '</svg>';
	cont.html(s);

	cont.find('.ct-dia').on('mousemove focus', function (e) {
		var di = dias[parseInt($(this).attr('data-i'), 10)];
		var xi = parseFloat($(this).attr('x')) + parseFloat($(this).attr('width')) / 2;
		$('#ct-cruz').attr('x1', xi).attr('x2', xi).show();
		var tip = $('#ct-tip');
		tip.empty();
		tip.append($('<div class="t-nombre">').text(ctFechaCorta(di.clave)));
		var f1 = $('<div>');
		f1.append($('<span class="t-valor">').text(ctPorcentajeTexto(di.porcentaje)));
		f1.append($('<span class="t-sec">').text(' cumplidos'));
		tip.append(f1);
		tip.append($('<div class="t-sec">').text(ctMiles(di.cumplidos) + ' de ' + ctMiles(di.total) +
			' · ' + ctMiles(di.incumplidos) + ' incumplidos'));
		var cx = (e.clientX !== undefined) ? e.clientX : ($(this).offset().left + 20);
		var cy = (e.clientY !== undefined) ? e.clientY : ($(this).offset().top - $(window).scrollTop());
		tip.css({ display: 'block', left: Math.min(cx + 14, $(window).width() - 280), top: cy + 14 });
	}).on('mouseleave blur', function () {
		$('#ct-cruz').hide();
		ctOcultarTooltip();
	});
}

/* La misma informacion, en filas: para quien prefiere leer numeros o necesita copiarlos. */
function ctPintarTabla(d) {
	function fila(a, rotuloClave) {
		return '<tr><td>' + ctEscapar(a.nombre) + '</td>' +
			'<td class="ct-num">' + ctMiles(a.total) + '</td>' +
			'<td class="ct-num">' + ctMiles(a.cumplidos) + '</td>' +
			'<td class="ct-num">' + ctMiles(a.incumplidos) + '</td>' +
			'<td class="ct-num"><b>' + ctEscapar(ctPorcentajeTexto(a.porcentaje)) + '</b></td>' +
			'<td class="ct-num">' + ctEscapar(String(a.promedio).replace('.', ',')) + '</td>' +
			'<td class="ct-num">' + a.maximo + '</td>' +
			(d.umbral > 0 ? '<td class="ct-num">' + ctMiles(a.sobre_umbral) + '</td>' : '') + '</tr>';
	}
	function cabecera(primero) {
		return '<thead><tr><th>' + primero + '</th><th class="ct-num">Entregados</th>' +
			'<th class="ct-num">Cumplidos</th><th class="ct-num">Incumplidos</th>' +
			'<th class="ct-num">% cumplido</th><th class="ct-num">Promedio (min)</th>' +
			'<th class="ct-num">M&aacute;s lento (min)</th>' +
			(d.umbral > 0 ? '<th class="ct-num">Pasaron de ' + d.umbral + ' min</th>' : '') + '</tr></thead>';
	}
	var h = '<table class="table table-bordered table-condensed">' + cabecera('Tienda') + '<tbody>';
	var tiendas = d.tiendas || [];
	for (var i = 0; i < tiendas.length; i++) { h += fila(tiendas[i]); }
	var g = d.general;
	g.nombre = 'TODAS LAS TIENDAS';
	h += '<tr style="font-weight:bold;background:#F4F6F8;">' + fila(g).substring(4);
	h += '</tbody></table>';
	if (d.dias > 1 && (d.por_dia || []).length > 0) {
		h += '<table class="table table-bordered table-condensed" style="margin-top:14px;">' + cabecera('D&iacute;a') + '<tbody>';
		for (var j = 0; j < d.por_dia.length; j++) {
			d.por_dia[j].nombre = ctFechaCorta(d.por_dia[j].clave);
			h += fila(d.por_dia[j]);
		}
		h += '</tbody></table>';
	}
	$('#ct-res-tabla').html(h);
}

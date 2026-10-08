/*
 * Bono de recompra.
 *
 * La pantalla solo define la REGLA. El calculo lo hace el barrido nocturno de
 * Servicios, que es el unico que ve las compras de mostrador. Por eso aqui no
 * hay ningun boton de "calcular ahora": prometeria algo que esta pantalla no
 * puede cumplir.
 *
 * ES5 -var y function-: convive con jQuery 1.11.
 */

var brCampanas = [];
var brSeleccionada = 0;

$(document).ready(function () {
	brCargarOfertas();
	brCargarProductos();
	brCargar();

	brCargarEnvios();

	$('#br-guardar').click(brGuardar);
	$('#br-nueva').click(function () { brLimpiar(); });
	$('#br-refrescar-estimado').click(function () { if (brSeleccionada) { brVerEstimado(brSeleccionada); } });
	$('#br-repetible').change(brNotaRepetible);
	$('#br-abierta').change(brPintarPublico);

	//El texto del mensaje sale de las fechas: si cambian, cambia.
	$('#br-desde,#br-hasta,#br-emision,#br-redesde,#br-rehasta,#br-porcentaje,#br-tope')
		.on('change keyup', brPintarInvitacion);

	brNotaRepetible();
	brPintarPublico();
});

/* El selector del envio solo tiene sentido cuando la campana es por invitacion. */
function brPintarPublico() {
	var porInvitacion = ($('#br-abierta').val() === 'N');
	$('#br-caja-envio').toggle(porInvitacion);
	brPintarInvitacion();
}

/*
 * Arma la frase que hay que decirle al cliente, con las fechas de la campana.
 *
 * No es adorno. El mensaje y lo que el sistema va a hacer TIENEN que decir lo
 * mismo: prometer "del 5 al 7" y que el bono venza el 6 es la clase de error
 * que se descubre con el cliente parado en la caja.
 */
function brPintarInvitacion() {
	var compraD = $('#br-desde').val();
	var compraH = $('#br-hasta').val();
	var emision = $('#br-emision').val();
	var redD = $('#br-redesde').val();
	var redH = $('#br-rehasta').val();
	var pct = $('#br-porcentaje').val();
	var tope = $('#br-tope').val();

	if (!compraD || !redH) { $('#br-mensaje-tipo').hide(); return; }

	var cuando = (compraD === compraH) ? ('el ' + brDia(compraD))
		: ('entre el ' + brDia(compraD) + ' y el ' + brDia(compraH));
	var vigencia = (redD && redD !== redH)
		? ('entre el ' + brDia(redD) + ' y el ' + brDia(redH))
		: ('hasta el ' + brDia(redH));

	var t = 'Has sido seleccionado: todo lo que compres ' + cuando +
		' (sin promociones) te lo devolvemos en un bono del ' + pct + '%';
	if (tope) { t += ', hasta ' + tope; }
	t += '.\n\nTe llega ' + (emision ? ('el ' + brDia(emision)) : 'apenas cierre el plazo') +
		' y lo puedes redimir ' + vigencia + ', en punto de venta o llamando.';

	$('#br-texto-invitacion').val(t);
	$('#br-mensaje-tipo').show();
}

/* "2026-10-05" -> "lunes 5 de octubre". */
function brDia(iso) {
	if (!iso || iso.length < 10) { return (''); }
	var dias = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
	var meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto',
		'septiembre', 'octubre', 'noviembre', 'diciembre'];
	//Se parte la cadena en vez de dejar que Date lea el ISO: leyendolo asi lo
	//toma como UTC y en Colombia muestra el dia anterior.
	var a = Number(iso.substring(0, 4));
	var m = Number(iso.substring(5, 7)) - 1;
	var d = Number(iso.substring(8, 10));
	var f = new Date(a, m, d);
	return (dias[f.getDay()] + ' ' + d + ' de ' + meses[m]);
}

/* Los envios de publicidad, para amarrar el bono al que llevo la invitacion. */
function brCargarEnvios() {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'ultimas', cuantas: 30 }, function (d) {
		var lista = (d && d.campanas) ? d.campanas : [];
		var html = '<option value="">Escoja el env&iacute;o</option>';
		for (var i = 0; i < lista.length; i++) {
			var e = lista[i];
			html += '<option value="' + e.idenvio + '">' + brEscapar(e.nombre) +
				' — ' + String(e.creada_en || '').substring(0, 10) +
				' (' + brMiles(e.enviados) + ' recibieron)</option>';
		}
		$('#br-envio').html(html);
	}).fail(function () {
		$('#br-envio').html('<option value="">No se pudieron cargar los env&iacute;os</option>');
	});
}

// ===========================================================================
// Plomeria
// ===========================================================================

function brEscapar(t) {
	return ($('<div/>').text(t === null || t === undefined ? '' : t).html());
}

function brMiles(n) {
	var texto = String(Math.round(Number(n) || 0));
	var signo = '';
	if (texto.charAt(0) === '-') { signo = '-'; texto = texto.substring(1); }
	var salida = '';
	for (var i = 0; i < texto.length; i++) {
		if (i > 0 && (texto.length - i) % 3 === 0) { salida += '.'; }
		salida += texto.charAt(i);
	}
	return (signo + salida);
}

function brPesos(v) { return ('$' + brMiles(v)); }

function brMensaje(clase, texto) {
	$('#br-mensaje').html('<div class="br-aviso ' + clase + '">' + texto + '</div>');
	if (window.scrollTo) { window.scrollTo(0, 0); }
}

/*
 * Explica en palabras que significa el interruptor, porque "repetible" no dice
 * lo que de verdad cambia: cuando se emite el bono.
 */
function brNotaRepetible() {
	if ($('#br-repetible').is(':checked')) {
		$('#br-nota-repetible').html('Cada noche, quien haya acumulado lo suficiente con pedidos ' +
			'que todav&iacute;a no han contado se gana <b>otro</b> bono. El que sigue comprando se lo ' +
			'sigue ganando.');
	} else {
		$('#br-nota-repetible').html('Un solo bono por persona, y se emite <b>cuando cierre la ' +
			'ventana</b>: es la &uacute;nica forma de sumar todas sus compras en uno solo. Si compra ' +
			'tres veces, el bono sale cuando termine el rango, no en la primera compra.');
	}
}

// ===========================================================================
// Cargas
// ===========================================================================

/*
 * Las ofertas que sirven para esto.
 *
 * Se reusa el servicio del envio de publicidad, que ya devuelve unicamente las
 * que se pueden emitir con codigo. De esas se muestran solo las que admiten
 * SALDO: un bono vale un monto distinto para cada quien, y sin redencion
 * parcial ese monto no se respeta al redimir -quien se gano $8.000 usaria la
 * oferta completa-.
 *
 * El servidor lo vuelve a revisar al guardar. Esto es para no ofrecer en la
 * lista algo que despues va a ser rechazado.
 */
function brCargarOfertas() {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'ofertas' }, function (d) {
		var lista = (d && d.ofertas) ? d.ofertas : [];
		var html = '<option value="">Escoja la oferta</option>';
		var sirven = 0;
		for (var i = 0; i < lista.length; i++) {
			var o = lista[i];
			if (!o.saldo) { continue; }
			sirven++;
			html += '<option value="' + o.idoferta + '">' + brEscapar(o.nombre) +
				' (vence a ' + o.dias + ' días)</option>';
		}
		$('#br-oferta').html(html);
		if (sirven === 0) {
			brMensaje('br-aviso-info', 'No hay ninguna oferta que sirva para un bono. ' +
				'Necesita una oferta <b>personal</b>, con <b>c&oacute;digo promocional</b>, ' +
				'<b>redenci&oacute;n parcial</b> y d&iacute;as de caducidad. ' +
				'Se crea en Ofertas &rarr; Administrar Ofertas.');
		}
	}).fail(function () {
		$('#br-oferta').html('<option value="">No se pudieron cargar las ofertas</option>');
	});
}

function brCargarProductos() {
	$.getJSON(server + 'GetTodosProductos', function (d) {
		var lista = d;
		if (!$.isArray(lista)) {
			for (var k in d) {
				if (d.hasOwnProperty(k) && $.isArray(d[k])) { lista = d[k]; break; }
			}
		}
		if (!$.isArray(lista)) { return; }
		var html = '';
		for (var i = 0; i < lista.length; i++) {
			var id = lista[i].idproducto || lista[i].id;
			var nom = lista[i].descripcion || lista[i].nombre || lista[i].nombreproducto || id;
			if (id) { html += '<option value="' + id + '">' + brEscapar(nom) + '</option>'; }
		}
		$('#br-productos').html(html);
	}).fail(function () {
		//Sin catalogo el filtro queda vacio y cuenta todo. Es el valor por
		//defecto de todas formas.
	});
}

function brCargar() {
	$.getJSON(server + 'BonoRecompra', { accion: 'listar' }, function (d) {
		if (d.error) { brMensaje('br-aviso-mal', brEscapar(d.error)); return; }
		brCampanas = d.campanas || [];
		brPintarTabla();
	}).fail(function () {
		brMensaje('br-aviso-mal', 'No se pudo cargar la lista de campa&ntilde;as.');
	});
}

function brPintarTabla() {
	var cuerpo = $('#br-tabla tbody');
	cuerpo.empty();
	if (brCampanas.length === 0) {
		cuerpo.html('<tr><td colspan="8" class="br-nota">Todav&iacute;a no hay campa&ntilde;as de bono.</td></tr>');
		return;
	}
	for (var i = 0; i < brCampanas.length; i++) {
		var c = brCampanas[i];
		var fila = $('<tr>').css('cursor', 'pointer');
		fila.append($('<td>').text(c.idbono));
		fila.append($('<td>').text(c.nombre));
		fila.append($('<td>').text(String(c.compra_desde).substring(5, 10) + ' a ' +
			String(c.compra_hasta).substring(5, 10)));
		fila.append($('<td class="br-num">').text(c.porcentaje + '%'));
		fila.append($('<td>').text(c.estado + (c.emitir === 'S' ? '' : ' (ensayo)')));
		fila.append($('<td class="br-num">').text(brMiles(c.con_pedidos)));
		fila.append($('<td class="br-num">').text(brMiles(c.emitidos)));
		fila.append($('<td class="br-num">').text(brPesos(c.valor_emitido)));
		fila.data('id', c.idbono);
		fila.click(function () { brSeleccionar($(this).data('id')); });
		cuerpo.append(fila);
	}
}

// ===========================================================================
// El formulario
// ===========================================================================

function brLimpiar() {
	brSeleccionada = 0;
	$('#br-nombre').val('');
	$('#br-oferta').val('');
	$('#br-desde').val('');
	$('#br-hasta').val('');
	$('#br-porcentaje').val('10');
	$('#br-tope').val('');
	$('#br-minima').val('');
	$('#br-productos').val([]);
	$('#br-excluirpromos').prop('checked', true);
	$('#br-repetible').prop('checked', false);
	$('#br-estado').val('BORRADOR');
	$('#br-emision').val('');
	$('#br-redesde').val('');
	$('#br-rehasta').val('');
	$('#br-abierta').val('S');
	$('#br-envio').val('');
	$('#br-emitir').val('N');
	$('#br-caja-emisiones').hide();
	$('#br-caja-estimado').hide();
	$('#br-mensaje').html('');
	brNotaRepetible();
	brPintarPublico();
}

function brSeleccionar(idBono) {
	var c = null;
	for (var i = 0; i < brCampanas.length; i++) {
		if (String(brCampanas[i].idbono) === String(idBono)) { c = brCampanas[i]; break; }
	}
	if (!c) { return; }
	brSeleccionada = c.idbono;
	$('#br-nombre').val(c.nombre);
	$('#br-oferta').val(c.idoferta);
	$('#br-desde').val(String(c.compra_desde).substring(0, 10));
	$('#br-hasta').val(String(c.compra_hasta).substring(0, 10));
	$('#br-porcentaje').val(c.porcentaje);
	$('#br-tope').val(brMiles(c.tope_bono));
	$('#br-minima').val(brMiles(c.base_minima));
	$('#br-productos').val(String(c.productos || '').split(',').filter(function (x) { return (x !== ''); }));
	$('#br-excluirpromos').prop('checked', c.excluir_promociones !== 'N');
	$('#br-repetible').prop('checked', c.repetible === 'S');
	$('#br-estado').val(c.estado);
	$('#br-emision').val(String(c.fecha_emision || '').substring(0, 10));
	$('#br-redesde').val(String(c.redime_desde || '').substring(0, 10));
	$('#br-rehasta').val(String(c.redime_hasta || '').substring(0, 10));
	$('#br-abierta').val(c.abierta === 'N' ? 'N' : 'S');
	$('#br-envio').val(c.idenvio ? String(c.idenvio) : '');
	$('#br-emitir').val(c.emitir);
	brNotaRepetible();
	brPintarPublico();
	brVerEmisiones(c.idbono);
	brVerEstimado(c.idbono);
}

function brGuardar() {
	var productos = $('#br-productos').val();
	var datos = {
		accion: 'guardar',
		idbono: brSeleccionada,
		nombre: $('#br-nombre').val(),
		idoferta: $('#br-oferta').val(),
		compra_desde: $('#br-desde').val(),
		compra_hasta: $('#br-hasta').val(),
		porcentaje: $('#br-porcentaje').val(),
		tope_bono: $('#br-tope').val(),
		base_minima: $('#br-minima').val(),
		productos: (productos && productos.length > 0) ? productos.join(',') : '',
		excluir_promociones: $('#br-excluirpromos').is(':checked') ? 'S' : 'N',
		repetible: $('#br-repetible').is(':checked') ? 'S' : 'N',
		estado: $('#br-estado').val(),
		emitir: $('#br-emitir').val(),
		avisar: 'S',
		abierta: $('#br-abierta').val(),
		idenvio: $('#br-envio').val() || 0,
		fecha_emision: $('#br-emision').val(),
		redime_desde: $('#br-redesde').val(),
		redime_hasta: $('#br-rehasta').val()
	};

	//Prender la emision reparte plata de verdad. Se pregunta con las cifras
	//escritas, para que nadie pueda decir que no sabia.
	if (datos.emitir === 'S' && datos.estado === 'ACTIVA') {
		if (!confirm('Va a dejar la campana "' + datos.nombre + '" EMITIENDO DE VERDAD.\n\n' +
				'A partir de esta noche se van a emitir bonos del ' + datos.porcentaje +
				'% (hasta ' + datos.tope_bono + ') y se les va a avisar a los clientes.\n\n' +
				'Continuar?')) {
			return;
		}
	}

	$('#br-guardar').prop('disabled', true).text('Guardando...');
	$.post(server + 'BonoRecompra', datos, function (d) {
		$('#br-guardar').prop('disabled', false).text('Guardar la campaña');
		if (d.error) { brMensaje('br-aviso-mal', brEscapar(d.error)); return; }
		brSeleccionada = d.idbono;
		var extra = '';
		if (datos.emitir === 'N') {
			extra = ' Est&aacute; en <b>ensayo</b>: el barrido de esta noche va a calcular a ' +
				'cu&aacute;ntos les dar&iacute;a y por cu&aacute;nto, sin emitir nada.';
		} else if (datos.estado !== 'ACTIVA') {
			extra = ' Ojo: el estado no es <b>Activa</b>, as&iacute; que el barrido no la va a mirar.';
		}
		brMensaje('br-aviso-ok', brEscapar(d.mensaje) + extra);
		brCargar();
	}, 'json').fail(function () {
		$('#br-guardar').prop('disabled', false).text('Guardar la campaña');
		brMensaje('br-aviso-mal', 'No se pudo guardar la campa&ntilde;a.');
	});
}

// ===========================================================================
// Como vamos (estimado, sin esperar al cierre real)
// ===========================================================================

function brVerEstimado(idBono) {
	$('#br-caja-estimado').show();
	$('#br-est-personas').text('...');
	$('#br-est-valor').text('...');
	$('#br-est-aviso').text('Calculando...');
	$.getJSON(server + 'BonoRecompra', { accion: 'previsualizar', idbono: idBono },
		function (d) {
			if (d.error) {
				$('#br-est-personas').text('-');
				$('#br-est-valor').text('-');
				$('#br-est-aviso').text(d.error);
				return;
			}
			$('#br-est-personas').text(d.califican);
			$('#br-est-valor').text(brPesos(d.valor));
			$('#br-est-aviso').text(d.aviso || '');
		}).fail(function () {
			$('#br-est-aviso').text('No se pudo calcular el estimado.');
		});
}

// ===========================================================================
// Lo emitido
// ===========================================================================

function brVerEmisiones(idBono) {
	$.getJSON(server + 'BonoRecompra', { accion: 'emisiones', idbono: idBono, cuantas: 200 },
		function (d) {
			var lista = d.emisiones || [];
			var cuerpo = $('#br-emisiones tbody');
			cuerpo.empty();
			if (lista.length === 0) {
				$('#br-caja-emisiones').show();
				cuerpo.html('<tr><td colspan="7" class="br-nota">Todav&iacute;a no se le ha calculado ' +
					'bono a nadie. Lo hace el barrido nocturno.</td></tr>');
				return;
			}
			for (var i = 0; i < lista.length; i++) {
				var e = lista[i];
				var fila = $('<tr>');
				fila.append($('<td>').text(e.destino || ('persona ' + e.idpersona)));
				fila.append($('<td class="br-num">').text(e.pedidos));
				fila.append($('<td class="br-num">').text(brPesos(e.base)));
				fila.append($('<td class="br-num">').text(brPesos(e.valor) +
					(e.topado === 'S' ? ' (tope)' : '')));
				fila.append($('<td>').text(e.codigo || ''));
				fila.append($('<td>').text(String(e.fecha_caducidad || '').substring(0, 10)));
				fila.append($('<td>').text(e.estado + (e.detalle ? ': ' + e.detalle : '')));
				cuerpo.append(fila);
			}
			$('#br-caja-emisiones').show();
		});
}

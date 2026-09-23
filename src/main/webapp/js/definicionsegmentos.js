/**
 * Definicion de segmentos del CRM.
 *
 * La pantalla escribe, y lo que escribe cambia como quedan clasificadas 450
 * mil personas. Por eso todo lo que modifica va por POST, y por eso el boton
 * de probar esta antes que el de guardar y no despues.
 *
 * LO QUE ESTA PANTALLA TIENE QUE DEJAR CLARO
 *
 * 1. Que el orden manda. Un segmento nuevo detras de uno mas amplio no atrapa
 *    a nadie, y desde afuera parece que la herramienta no sirve.
 * 2. Que los numeros de la tabla son del ULTIMO reparto, no de las reglas que
 *    se acaban de escribir. Editar no reclasifica: son dos cosas distintas y
 *    la segunda tarda un minuto.
 *
 * Por eso al guardar aparece el aviso de "todavia no ha clasificado" y no se
 * quita hasta que se clasifique.
 */

var dsCampos = [];
var dsSegmentos = [];
var dsSucio = false;

var DS_OPERADORES = [
	{ op: '>=', texto: 'es mayor o igual a' },
	{ op: '<=', texto: 'es menor o igual a' },
	{ op: '=', texto: 'es igual a' },
	{ op: '>', texto: 'es mayor que' },
	{ op: '<', texto: 'es menor que' },
	{ op: '<>', texto: 'es distinto de' }
];

$(function () {
	$('#ds-nuevo').click(function () { dsAbrir(null); });
	$('#ds-cancelar').click(dsCerrar);
	$('#ds-agregar').click(function () { dsFilaRegla(null); });
	$('#ds-probar').click(dsProbar);
	$('#ds-guardar').click(dsGuardar);
	$('#ds-clasificar').click(dsClasificar);
	$('#ds-cuerpo').on('click', '.ds-editar', function () {
		dsAbrir($(this).data('id'));
	});
	$('#ds-cuerpo').on('click', '.ds-borrar', function () {
		dsBorrar($(this).data('id'), $(this).data('nombre'));
	});
	$('#ds-reglas').on('click', '.ds-quitar', function () {
		$(this).closest('.ds-regla').remove();
		dsNumerarReglas();
	});
	dsCargar();
});

// ==========================================================================
// Cargar y pintar
// ==========================================================================

function dsCargar(alTerminar) {
	$.ajax({
		url: server + 'AdministrarSegmentos',
		data: { accion: 'listar' },
		dataType: 'json',
		type: 'get',
		success: function (d) {
			if (d.error) { dsMensaje(d.error, 'malo'); return; }
			dsCampos = d.campos || [];
			dsSegmentos = d.segmentos || [];
			dsPintar(d);
			if (alTerminar) { alTerminar(); }
		},
		error: function () {
			dsMensaje('No se pudo cargar la lista de segmentos.', 'malo');
		}
	});
}

function dsPintar(d) {
	var c = $('#ds-cuerpo').empty();
	if (!dsSegmentos.length) {
		c.append('<tr><td colspan="5" class="text-center" style="color:#8A919E;padding:24px;">' +
			'No hay ning&uacute;n segmento definido.</td></tr>');
	}
	for (var i = 0; i < dsSegmentos.length; i++) {
		c.append(dsFilaSegmento(dsSegmentos[i]));
	}

	var sc = d.sin_clasificar || 0;
	if (sc > 0) {
		//Esto no es un error, es informacion: si hay gente sin clasificar, a
		//las definiciones les falta un caso. Se dice, no se esconde.
		$('#ds-sinclasificar').html('<b>' + dsMil(sc) + '</b> personas no cumplen ' +
			'ninguna definici&oacute;n y quedaron en <b>sin clasificar</b>. ' +
			'Si eso no era la intenci&oacute;n, a las reglas les falta un caso.');
	} else {
		$('#ds-sinclasificar').html('Todas las personas cumplen alguna definici&oacute;n.');
	}
}

function dsFilaSegmento(s) {
	var apagado = (s.activo !== 'S');
	var tr = $('<tr></tr>');
	if (apagado) { tr.addClass('ds-apagado'); }

	tr.append($('<td class="ds-num"></td>').text(s.orden));

	var color = /^#[0-9A-Fa-f]{6}$/.test(s.color || '') ? s.color : '#8A919E';
	var td = $('<td></td>');
	td.append($('<span class="ds-etiqueta"></span>').css('background-color', color).text(s.nombre));
	if (apagado) {
		td.append('<span class="ds-nota"> (apagado)</span>');
	}
	if (s.descripcion) {
		td.append($('<div class="ds-nota" style="margin-top:3px;"></div>').text(s.descripcion));
	}
	tr.append(td);

	tr.append($('<td class="ds-reglatexto"></td>').html(dsReglasEnTexto(s.reglas)));
	tr.append($('<td class="ds-num"></td>').text(dsMil(s.personas)));

	var acc = $('<td class="text-right"></td>');
	acc.append($('<button class="btn btn-default btn-xs ds-editar">Editar</button>')
		.attr('data-id', s.idsegmento));
	acc.append($('<button class="btn btn-link btn-xs ds-borrar" style="color:#C21C1F;">Borrar</button>')
		.attr('data-id', s.idsegmento).attr('data-nombre', s.nombre));
	tr.append(acc);
	return tr;
}

/** Las reglas en palabras, no en SQL: esta pantalla no es para programadores. */
function dsReglasEnTexto(reglas) {
	if (!reglas || !reglas.length) {
		return '<span style="color:#C21C1F;">sin reglas, no clasifica a nadie</span>';
	}
	var partes = [];
	for (var i = 0; i < reglas.length; i++) {
		partes.push(dsEsc(dsEtiquetaCampo(reglas[i].campo)) + ' ' +
			dsEsc(reglas[i].operador) + ' ' + dsEsc(reglas[i].valor));
	}
	return partes.join('<span class="ds-y"> y </span>');
}

function dsEtiquetaCampo(campo) {
	for (var i = 0; i < dsCampos.length; i++) {
		if (dsCampos[i].campo === campo) { return dsCampos[i].etiqueta; }
	}
	return campo;
}

function dsTipoCampo(campo) {
	for (var i = 0; i < dsCampos.length; i++) {
		if (dsCampos[i].campo === campo) { return dsCampos[i].tipo; }
	}
	return 'NUMERO';
}

// ==========================================================================
// El editor
// ==========================================================================

function dsAbrir(idsegmento) {
	var s = null;
	for (var i = 0; i < dsSegmentos.length; i++) {
		if (dsSegmentos[i].idsegmento === idsegmento) { s = dsSegmentos[i]; }
	}
	$('#ds-ayuda').hide();
	$('#ds-editor').show();
	$('#ds-resultado').empty();

	if (s) {
		$('#ds-editor-titulo').text('Editar ' + s.nombre);
		$('#ds-id').val(s.idsegmento);
		$('#ds-nombre').val(s.nombre);
		$('#ds-orden').val(s.orden);
		$('#ds-descripcion').val(s.descripcion);
		$('#ds-color').val(s.color);
		$('#ds-activo').val(s.activo === 'N' ? 'N' : 'S');
		$('#ds-reglas').empty();
		for (var k = 0; k < s.reglas.length; k++) { dsFilaRegla(s.reglas[k]); }
	} else {
		$('#ds-editor-titulo').text('Nuevo segmento');
		$('#ds-id').val(0);
		$('#ds-nombre').val('');
		//Se propone un orden despues del ultimo, que es lo menos peligroso: un
		//segmento nuevo con orden 10 le quitaria gente a todos los demas sin
		//que nadie lo pidiera.
		$('#ds-orden').val(dsSiguienteOrden());
		$('#ds-descripcion').val('');
		$('#ds-color').val('');
		$('#ds-activo').val('S');
		$('#ds-reglas').empty();
		dsFilaRegla(null);
	}
	dsNumerarReglas();
}

function dsSiguienteOrden() {
	var max = 0;
	for (var i = 0; i < dsSegmentos.length; i++) {
		if (dsSegmentos[i].orden > max) { max = dsSegmentos[i].orden; }
	}
	return max + 10;
}

function dsCerrar() {
	$('#ds-editor').hide();
	$('#ds-ayuda').show();
}

function dsFilaRegla(r) {
	var fila = $('<div class="ds-regla row"></div>');

	var selCampo = $('<select class="form-control input-sm ds-campo-sel"></select>');
	for (var i = 0; i < dsCampos.length; i++) {
		selCampo.append($('<option></option>').attr('value', dsCampos[i].campo)
			.text(dsCampos[i].etiqueta));
	}

	var selOp = $('<select class="form-control input-sm ds-op-sel"></select>');
	for (var k = 0; k < DS_OPERADORES.length; k++) {
		selOp.append($('<option></option>').attr('value', DS_OPERADORES[k].op)
			.text(DS_OPERADORES[k].texto));
	}

	var inpVal = $('<input type="text" class="form-control input-sm ds-val" maxlength="40" />');

	if (r) {
		selCampo.val(r.campo);
		selOp.val(r.operador);
		inpVal.val(r.valor);
	}

	fila.append($('<div class="col-xs-4"></div>').append(selCampo));
	fila.append($('<div class="col-xs-4"></div>').append(selOp));
	fila.append($('<div class="col-xs-3"></div>').append(inpVal));
	fila.append('<div class="col-xs-1" style="padding-left:0;">' +
		'<button class="btn btn-link btn-xs ds-quitar" title="Quitar" ' +
		'style="color:#C21C1F;padding:5px 2px;">&times;</button></div>');

	var ayuda = $('<div class="col-xs-12 ds-nota ds-ayuda-campo" style="margin:1px 0 4px;"></div>');
	fila.append(ayuda);
	selCampo.change(function () { dsPintarAyuda(fila); });
	$('#ds-reglas').append(fila);
	dsPintarAyuda(fila);
	dsNumerarReglas();
}

function dsPintarAyuda(fila) {
	var campo = fila.find('.ds-campo-sel').val();
	var texto = '';
	for (var i = 0; i < dsCampos.length; i++) {
		if (dsCampos[i].campo === campo) { texto = dsCampos[i].ayuda || ''; }
	}
	if (dsTipoCampo(campo) === 'TEXTO') {
		texto = texto || 'Un texto.';
	}
	fila.find('.ds-ayuda-campo').text(texto);
}

/** El "y" entre reglas, para que se vea que se suman y no se escogen. */
function dsNumerarReglas() {
	$('#ds-reglas .ds-regla').each(function (i) {
		$(this).find('.ds-marca').remove();
		if (i > 0) {
			$(this).find('.col-xs-4').first()
				.prepend('<span class="ds-marca ds-y" style="position:absolute;' +
					'left:-2px;top:-13px;">y</span>');
			$(this).find('.col-xs-4').first().css('position', 'relative');
		}
	});
}

function dsReglasDelEditor() {
	var reglas = [];
	$('#ds-reglas .ds-regla').each(function () {
		var campo = $(this).find('.ds-campo-sel').val();
		var op = $(this).find('.ds-op-sel').val();
		var valor = $.trim($(this).find('.ds-val').val() || '');
		if (campo && op && valor !== '') {
			reglas.push({ campo: campo, operador: op, valor: valor });
		}
	});
	return reglas;
}

// ==========================================================================
// Probar, guardar, borrar, clasificar
// ==========================================================================

function dsProbar() {
	var reglas = dsReglasDelEditor();
	if (!reglas.length) {
		$('#ds-resultado').html('<div class="ds-malo">Escriba por lo menos una regla ' +
			'con su valor.</div>');
		return;
	}
	var b = $('#ds-probar').prop('disabled', true).text('Probando...');
	$.ajax({
		url: server + 'AdministrarSegmentos',
		type: 'post',
		dataType: 'json',
		data: {
			accion: 'probar',
			idsegmento: $('#ds-id').val(),
			orden: $('#ds-orden').val(),
			reglas: JSON.stringify(reglas)
		},
		success: function (d) {
			b.prop('disabled', false).text('Probar');
			if (d.error) {
				$('#ds-resultado').html('<div class="ds-malo">' + dsEsc(d.error) + '</div>');
				return;
			}
			dsPintarPrueba(d);
		},
		error: function () {
			b.prop('disabled', false).text('Probar');
			$('#ds-resultado').html('<div class="ds-malo">No se pudo probar.</div>');
		}
	});
}

function dsPintarPrueba(d) {
	var h = '<div class="ds-prueba"><div class="row">' +
		'<div class="col-xs-4"><div class="cifra">' + dsMil(d.cumplen) + '</div>' +
		'<div class="rotulo">cumplen las reglas</div></div>' +
		'<div class="col-xs-4"><div class="cifra" style="color:' +
		(d.quedarian > 0 ? '#16704F' : '#C21C1F') + ';">' + dsMil(d.quedarian) + '</div>' +
		'<div class="rotulo">quedar&iacute;an aqu&iacute;</div></div>' +
		'<div class="col-xs-4"><div class="cifra">' + dsMil(d.contactables) + '</div>' +
		'<div class="rotulo">autorizaron datos</div></div>' +
		'</div>';

	if (d.quedarian === 0 && d.cumplen > 0) {
		//El caso que hay que cazar: las reglas sirven pero un segmento mas
		//prioritario se lleva a todo el mundo. Sin decirlo, uno guarda esto
		//convencido de que funciona y despues no entiende por que sale vacio.
		h += '<div class="ds-malo" style="margin:10px 0 0;">' +
			'Cumplen ' + dsMil(d.cumplen) + ' personas pero <b>ninguna quedar&iacute;a ' +
			'aqu&iacute;</b>: se las lleva un segmento de mayor prioridad. ' +
			'B&aacute;jele el n&uacute;mero de orden o ajuste las reglas.</div>';
	} else if (d.quedarian < d.cumplen) {
		h += '<div class="ds-nota" style="margin-top:8px;">' +
			'De los que cumplen, ' + dsMil(d.cumplen - d.quedarian) + ' ya se los lleva un ' +
			'segmento de mayor prioridad.</div>';
	} else {
		h += '<div class="ds-nota" style="margin-top:8px;">' +
			'Ninguna otra definici&oacute;n les gana.</div>';
	}
	h += '<div class="ds-nota" style="margin-top:6px;">Compras de esta gente: <b>' +
		dsPlata(d.valor) + '</b>.</div></div>';
	$('#ds-resultado').html(h);
}

function dsGuardar() {
	var reglas = dsReglasDelEditor();
	var b = $('#ds-guardar').prop('disabled', true).text('Guardando...');
	$.ajax({
		url: server + 'AdministrarSegmentos',
		type: 'post',
		dataType: 'json',
		data: {
			accion: 'guardar',
			idsegmento: $('#ds-id').val(),
			nombre: $('#ds-nombre').val(),
			descripcion: $('#ds-descripcion').val(),
			orden: $('#ds-orden').val(),
			color: $('#ds-color').val(),
			activo: $('#ds-activo').val(),
			reglas: JSON.stringify(reglas)
		},
		success: function (d) {
			b.prop('disabled', false).text('Guardar');
			if (d.error) {
				$('#ds-resultado').html('<div class="ds-malo">' + dsEsc(d.error) + '</div>');
				return;
			}
			dsSucio = true;
			dsCerrar();
			dsCargar(function () {
				$('#ds-desactualizado').show();
				dsMensaje('Guardado. Para que la gente se mueva de segmento hay que ' +
					'volver a clasificar.', 'bueno');
			});
		},
		error: function () {
			b.prop('disabled', false).text('Guardar');
			$('#ds-resultado').html('<div class="ds-malo">No se pudo guardar.</div>');
		}
	});
}

function dsBorrar(id, nombre) {
	if (!window.confirm('Borrar el segmento ' + nombre + '?\n\n' +
			'La gente que lo tiene no se borra: se acomoda en la siguiente ' +
			'clasificacion. Si no cumple ninguna otra definicion queda en ' +
			'"sin clasificar".')) {
		return;
	}
	$.ajax({
		url: server + 'AdministrarSegmentos',
		type: 'post',
		dataType: 'json',
		data: { accion: 'borrar', idsegmento: id },
		success: function (d) {
			if (d.error) { dsMensaje(d.error, 'malo'); return; }
			dsSucio = true;
			dsCerrar();
			dsCargar(function () {
				$('#ds-desactualizado').show();
				dsMensaje('Segmento borrado. Vuelva a clasificar.', 'bueno');
			});
		},
		error: function () { dsMensaje('No se pudo borrar.', 'malo'); }
	});
}

function dsClasificar() {
	if (!window.confirm('Volver a clasificar a las 450 mil personas con las ' +
			'definiciones de este momento?\n\nTarda alrededor de un minuto. ' +
			'No recalcula los pedidos.')) {
		return;
	}
	var b = $('#ds-clasificar').prop('disabled', true).text('Clasificando...');
	dsMensaje('Clasificando. Esto tarda alrededor de un minuto, no cierre la pantalla.', 'aviso');
	$.ajax({
		url: server + 'AdministrarSegmentos',
		type: 'post',
		dataType: 'json',
		//Un minuto largo. El que viene por defecto lo cortaria a la mitad y la
		//pantalla diria que fallo mientras el procedimiento sigue corriendo.
		timeout: 300000,
		data: { accion: 'clasificar' },
		success: function (d) {
			b.prop('disabled', false).text('Volver a clasificar');
			if (d.error) { dsMensaje(d.error, 'malo'); return; }
			dsSucio = false;
			dsCargar(function () {
				$('#ds-desactualizado').hide();
				dsMensaje('Listo: las personas quedaron repartidas de nuevo, en ' +
					d.segundos + ' segundos.', 'bueno');
			});
		},
		error: function (x, motivo) {
			b.prop('disabled', false).text('Volver a clasificar');
			//Que se corte la espera NO quiere decir que no haya clasificado: el
			//procedimiento sigue en la base de datos. Decir "fallo" seria
			//mentir, y volverlo a oprimir lo correria dos veces.
			//Ojo: este texto pasa por dsEsc, asi que va sin entidades HTML. Un
			//"&oacute;" aqui saldria escrito tal cual en la pantalla.
			dsMensaje(motivo === 'timeout'
				? 'La espera se acabo antes de recibir respuesta. El proceso puede haber '
					+ 'terminado igual: recargue en un minuto y mire los numeros.'
				: 'No se pudo clasificar.', 'malo');
		}
	});
}

// ==========================================================================

function dsMensaje(texto, clase) {
	var css = clase === 'bueno' ? 'ds-bueno' : (clase === 'aviso' ? 'ds-aviso' : 'ds-malo');
	$('#ds-mensaje').html('<div class="' + css + '">' + dsEsc(texto) + '</div>');
	if (clase === 'bueno') {
		window.setTimeout(function () { $('#ds-mensaje').empty(); }, 8000);
	}
}

function dsMil(n) {
	n = Number(n) || 0;
	return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function dsPlata(n) {
	n = Number(n) || 0;
	return '$' + dsMil(Math.round(n));
}

function dsEsc(s) {
	return $('<div></div>').text(s === null || s === undefined ? '' : s).html();
}

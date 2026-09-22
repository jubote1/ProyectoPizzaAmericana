/**
 * Segmentacion de personas del CRM.
 *
 * Lee crm.persona_resumen, que se calcula una vez por noche. Por eso la
 * pantalla dice arriba CUANDO se calculo: quien mira "1.200 activos" tiene que
 * saber que es el corte de anoche y no de este momento, o va a creer que el
 * sistema esta mal cuando el numero no cuadre con un pedido de hace una hora.
 *
 * La cifra que importa es el CONTEO, no la lista. Quien segmenta quiere saber
 * cuanta gente hay en un grupo para decidir si vale la pena una campana; la
 * lista es para revisar unos cuantos y para descargarla. Por eso el conteo va
 * arriba y grande, y la tabla esta paginada.
 */

var sgUltimo = null;
var sgPagina = 1;

$(function () {
	$('#sg-consultar').click(function () { sgPagina = 1; sgConsultar(); });
	$('#sg-limpiar').click(sgLimpiar);
	$('#sg-descargar').click(sgDescargar);
	$('#sg-anterior').click(function () {
		if (sgPagina > 1) { sgPagina--; sgConsultar(); }
	});
	$('#sg-siguiente').click(function () { sgPagina++; sgConsultar(); });
	$('#sg-grid').on('click', 'tbody tr', function () {
		var id = $(this).data('id');
		if (id) {
			window.open('Persona360.html?idpersona=' + id, '_blank');
		}
	});
	//La primera consulta va sin filtros: asi la pantalla abre mostrando el
	//universo completo y se entiende de una que hay 450 mil personas, en vez de
	//abrir vacia y dejar a uno adivinando que escribir.
	sgConsultar();
});

function sgFiltros() {
	var segs = [];
	$('.sg-chk-seg:checked').each(function () { segs.push($(this).val()); });
	return {
		segmentos: segs.join(','),
		idtienda: $('#sg-tienda').val() || 0,
		canal: $('#sg-canal').val(),
		pedidosmin: $('#sg-pedidosmin').val(),
		valormin: $('#sg-valormin').val(),
		diasmin: $('#sg-diasmin').val(),
		diasmax: $('#sg-diasmax').val(),
		concorreo: $('#sg-concorreo').is(':checked') ? 'S' : 'N',
		autorizados: $('#sg-autorizados').is(':checked') ? 'S' : 'N',
		orden: $('#sg-orden').val()
	};
}

function sgConsultar() {
	var datos = sgFiltros();
	datos.pagina = sgPagina;
	datos.porpagina = 50;

	$('#sg-consultar').prop('disabled', true).text('Consultando...');
	$.ajax({
		url: server + 'ConsultarSegmentacionPersona',
		data: datos,
		dataType: 'json',
		type: 'post',
		success: function (d) {
			$('#sg-consultar').prop('disabled', false).text('Consultar');
			if (!d || d.error) {
				sgVacio(d && d.mensaje ? d.mensaje : 'No se pudo consultar.');
				return;
			}
			sgUltimo = d;
			sgPintar(d);
		},
		error: function () {
			$('#sg-consultar').prop('disabled', false).text('Consultar');
			sgVacio('No se pudo consultar. Intente de nuevo.');
		}
	});
}

function sgPintar(d) {
	sgLlenarTiendas(d.tiendas);
	sgLlenarSegmentos(d.segmentos_definidos);

	$('#sg-leyenda').html(
		'Activo es haber comprado en los &uacute;ltimos <b>' + sgEsc(d.dias_activo) +
		'</b> d&iacute;as; en riesgo hasta <b>' + sgEsc(d.dias_riesgo) +
		'</b>; m&aacute;s all&aacute; de ah&iacute;, dormido. ' +
		'Incluye los pedidos de mostrador, que en el central no existen. ' +
		'Calculado el <b>' + sgEsc(d.calculado_en || 'sin calcular') + '</b>.');

	$('#sg-k-personas').text(sgMil(d.personas));
	$('#sg-k-pedidos').text(sgMil(d.pedidos));
	$('#sg-k-valor').text(sgPlata(d.valor));
	$('#sg-k-correo').text(sgMil(d.con_correo));
	$('#sg-k-autorizados').text(sgMil(d.autorizados));

	sgReparto(d.por_segmento);

	var c = $('#sg-cuerpo').empty();
	if (!d.filas || d.filas.length === 0) {
		c.append('<tr><td colspan="12" class="text-center" style="color:#8A919E;padding:24px;">' +
			'Ninguna persona cumple esos filtros.</td></tr>');
	} else {
		for (var i = 0; i < d.filas.length; i++) {
			c.append(sgFila(d.filas[i]));
		}
	}

	var desde = (d.pagina - 1) * d.por_pagina + 1;
	var hasta = desde + (d.filas ? d.filas.length : 0) - 1;
	$('#sg-pagina').html(d.personas === 0 ? '' :
		'Mostrando ' + sgMil(desde) + ' a ' + sgMil(hasta) + ' de ' + sgMil(d.personas));
	$('#sg-anterior').prop('disabled', d.pagina <= 1);
	$('#sg-siguiente').prop('disabled', hasta >= d.personas);
	$('#sg-descargar').prop('disabled', d.personas === 0);

	//Se avisa ANTES de descargar y no despues: un archivo recortado en 50 mil
	//filas no se distingue de uno completo, y quien lo reciba creeria que ese
	//es todo el grupo.
	if (d.personas > d.tope_descarga) {
		$('#sg-avisotope').show().html('Hay <b>' + sgMil(d.personas) +
			'</b> personas y la descarga trae como m&aacute;ximo <b>' + sgMil(d.tope_descarga) +
			'</b>. Afine los filtros si necesita la lista completa.');
	} else {
		$('#sg-avisotope').hide();
	}
}

function sgFila(p) {
	var nombre = ((p.nombre || '') + ' ' + (p.apellido || '')).trim();
	var tr = $('<tr></tr>').data('id', p.idpersona);
	tr.append($('<td></td>').html('<b>' + sgEsc(nombre || '(sin nombre)') + '</b>' +
		(p.tiendas_distintas > 1 ? '<br><span style="font-size:10.5px;color:#8A919E;">' +
			p.tiendas_distintas + ' tiendas</span>' : '')));
	tr.append($('<td></td>').text(p.celular || ''));
	tr.append($('<td></td>').html(p.email ? sgEsc(p.email) :
		'<span style="color:#C0C5CE;">sin correo</span>'));
	tr.append($('<td></td>').html(sgEtiqueta(p.segmento)));
	tr.append($('<td class="sg-num"></td>').text(sgMil(p.pedidos)));
	tr.append($('<td class="sg-num"></td>').text(sgMil(p.pedidos_central)));
	tr.append($('<td class="sg-num"></td>').text(sgMil(p.pedidos_tienda)));
	tr.append($('<td class="sg-num"></td>').text(sgPlata(p.valor)));
	tr.append($('<td class="sg-num"></td>').text(sgPlata(p.ticket)));
	tr.append($('<td></td>').text(p.ultimo_pedido || ''));
	tr.append($('<td class="sg-num"></td>').text(p.dias_sin_comprar < 0 ? '' : sgMil(p.dias_sin_comprar)));
	tr.append($('<td class="sg-num"></td>').text(p.dias_entre_pedidos < 0 ? '' : sgMil(p.dias_entre_pedidos)));
	return (tr);
}

/** El color de cada segmento, tal como quedo definido. Se llena al consultar. */
var sgColores = {};

/**
 * La etiqueta de colores de un segmento.
 *
 * El color sale de la definicion y no de una clase de CSS escrita a mano: si
 * estuviera en el CSS, un segmento nuevo saldria gris hasta que alguien se
 * acordara de tocar la hoja de estilos. Las clases sg-NUEVO y compania siguen
 * ahi como respaldo para los cinco de siempre, por si a alguno le borran el
 * color.
 *
 * El color se usa en la letra y diluido en el fondo, no como fondo lleno: son
 * etiquetas pequenas dentro de una tabla larga, y once fondos fuertes
 * convierten la lista en un semaforo donde ya no se lee nada.
 */
function sgEtiqueta(s) {
	var color = sgColores[s];
	if (color) {
		var r = parseInt(color.substr(1, 2), 16);
		var g = parseInt(color.substr(3, 2), 16);
		var b = parseInt(color.substr(5, 2), 16);
		return ('<span class="sg-etiqueta" style="color:' + color +
			';background-color:rgba(' + r + ',' + g + ',' + b + ',.13);">' +
			sgEsc(s) + '</span>');
	}
	var clase = 'sg-NINGUNO';
	if (s === 'NUEVO') { clase = 'sg-NUEVO'; }
	else if (s === 'ACTIVO') { clase = 'sg-ACTIVO'; }
	else if (s === 'EN RIESGO') { clase = 'sg-RIESGO'; }
	else if (s === 'DORMIDO') { clase = 'sg-DORMIDO'; }
	return ('<span class="sg-etiqueta ' + clase + '">' + sgEsc(s || '-') + '</span>');
}

function sgReparto(lista) {
	if (!lista || lista.length === 0) {
		$('#sg-repartowrap').hide();
		return;
	}
	var mayor = 0;
	for (var i = 0; i < lista.length; i++) {
		if (lista[i].personas > mayor) { mayor = lista[i].personas; }
	}
	var t = $('#sg-reparto').empty();
	for (var j = 0; j < lista.length; j++) {
		var x = lista[j];
		var ancho = mayor === 0 ? 0 : Math.round((x.personas / mayor) * 160);
		t.append('<tr>' +
			'<td style="width:110px;">' + sgEtiqueta(x.segmento) + '</td>' +
			'<td style="width:170px;"><span class="sg-barra" style="width:' + ancho + 'px;"></span></td>' +
			'<td style="width:90px;" class="sg-num"><b>' + sgMil(x.personas) + '</b></td>' +
			'<td class="sg-num" style="color:#6C7482;">' + sgPlata(x.valor) + '</td>' +
			'</tr>');
	}
	$('#sg-repartowrap').show();
}

/**
 * Las tiendas se llenan una sola vez. Volver a armar el select en cada consulta
 * borraria lo que el usuario escogio justo antes de consultar.
 */
function sgLlenarTiendas(lista) {
	var sel = $('#sg-tienda');
	if (!lista || sel.data('llena')) {
		return;
	}
	for (var i = 0; i < lista.length; i++) {
		sel.append($('<option></option>').attr('value', lista[i].idtienda).text(lista[i].nombre));
	}
	sel.data('llena', true);
}

/**
 * Las casillas de segmento se arman con los que existan de verdad.
 *
 * Antes estaban escritas en el HTML, y eso significaba que un segmento creado
 * desde la pantalla de definiciones se podia clasificar pero no filtrar: esta
 * pantalla lo descartaba en silencio. Igual que las tiendas, se llenan una sola
 * vez para no borrar lo que el usuario acaba de marcar.
 *
 * Los apagados tambien se muestran: la gente que los tenia sigue teniendolos
 * hasta la siguiente clasificacion, y hay que poder buscarla.
 */
function sgLlenarSegmentos(lista) {
	var caja = $('#sg-segmentos');
	if (!lista) {
		return;
	}
	//Los colores se refrescan en cada consulta aunque las casillas no: son para
	//pintar la tabla, no un control que el usuario pueda tener a medio llenar.
	for (var c = 0; c < lista.length; c++) {
		if (/^#[0-9A-Fa-f]{6}$/.test(lista[c].color || '')) {
			sgColores[lista[c].nombre] = lista[c].color;
		}
	}
	if (caja.data('llena')) {
		return;
	}
	caja.empty();
	for (var i = 0; i < lista.length; i++) {
		var s = lista[i];
		var etiqueta = $('<label class="sg-seg"></label>');
		etiqueta.append($('<input type="checkbox" class="sg-chk-seg" />').attr('value', s.nombre));
		etiqueta.append(document.createTextNode(
			sgBonito(s.nombre) + (s.activo === 'N' ? ' (apagado)' : '')));
		if (s.descripcion) {
			etiqueta.attr('title', s.descripcion);
		}
		caja.append(etiqueta);
	}
	//Siempre va al final, aunque no sea una definicion: es donde cae quien no
	//cumple ninguna, y es justo a esa gente a la que hay que poder mirarle.
	var sin = $('<label class="sg-seg"></label>');
	sin.append($('<input type="checkbox" class="sg-chk-seg" value="SIN CLASIFICAR" />'));
	sin.append(document.createTextNode('Sin clasificar'));
	caja.append(sin);
	caja.data('llena', true);
}

/** POR RECUPERAR -> Por recuperar. Los nombres se guardan en mayuscula sostenida. */
function sgBonito(nombre) {
	if (!nombre) { return ''; }
	return nombre.charAt(0) + nombre.substring(1).toLowerCase();
}

function sgLimpiar() {
	$('.sg-chk-seg').prop('checked', false);
	$('#sg-tienda').val('0');
	$('#sg-canal').val('TODOS');
	$('#sg-pedidosmin, #sg-valormin, #sg-diasmin, #sg-diasmax').val('');
	$('#sg-concorreo, #sg-autorizados').prop('checked', false);
	$('#sg-orden').val('VALOR');
	sgPagina = 1;
	sgConsultar();
}

function sgDescargar() {
	var f = sgFiltros();
	var partes = [];
	for (var k in f) {
		if (f.hasOwnProperty(k)) {
			partes.push(encodeURIComponent(k) + '=' + encodeURIComponent(f[k]));
		}
	}
	//Se va por la barra de direcciones y no por ajax: es una descarga, y el
	//navegador tiene que recibir el Content-Disposition para abrir el dialogo
	//de guardar. Con ajax el archivo llegaria a una variable y ahi se quedaria.
	window.location = server + 'DescargarSegmentacionPersona?' + partes.join('&');
}

function sgVacio(mensaje) {
	$('#sg-cuerpo').html('<tr><td colspan="12" class="text-center" ' +
		'style="color:#C21C1F;padding:24px;">' + sgEsc(mensaje) + '</td></tr>');
	$('#sg-k-personas, #sg-k-pedidos, #sg-k-valor, #sg-k-correo, #sg-k-autorizados').text('-');
	$('#sg-repartowrap').hide();
	$('#sg-descargar').prop('disabled', true);
}

function sgMil(n) {
	if (n === null || n === undefined || n === '') { return ('0'); }
	var s = String(Math.round(n));
	var negativo = s.charAt(0) === '-';
	if (negativo) { s = s.substring(1); }
	var r = '';
	for (var i = 0; i < s.length; i++) {
		if (i > 0 && (s.length - i) % 3 === 0) { r += '.'; }
		r += s.charAt(i);
	}
	return (negativo ? '-' + r : r);
}

/**
 * Por encima del millon se abrevia: una columna con $41.338.161.802 no se lee,
 * y en una lista lo que importa es comparar, no el peso exacto.
 *
 * El decimal se calcula aparte y no pasando un numero con coma a sgMil: esa
 * funcion redondea a entero, asi que 41.338,2 habria salido 41.338 y la
 * abreviatura no habria servido para comparar dos cifras parecidas.
 */
function sgPlata(n) {
	if (!n) { return ('$0'); }
	if (n >= 1000000) {
		var millones = n / 1000000;
		var enteros = Math.floor(millones);
		var decimo = Math.round((millones - enteros) * 10);
		if (decimo === 10) { enteros = enteros + 1; decimo = 0; }
		return ('$' + sgMil(enteros) + ',' + decimo + ' M');
	}
	return ('$' + sgMil(n));
}

function sgEsc(s) {
	if (s === null || s === undefined) { return (''); }
	return (String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
		.replace(/>/g, '&gt;').replace(/"/g, '&quot;'));
}

/**
 * Tablero de segmentos: para mirar, no para tocar.
 *
 * Responde tres preguntas de una sola ojeada: cuales segmentos hay, que quiere
 * decir cada uno y cuanta gente tiene. Lee del mismo servicio que la pantalla
 * de definiciones -AdministrarSegmentos, accion listar-, que ya trae las
 * reglas, los colores y los conteos; un servicio aparte para lo mismo serian
 * dos sitios donde los numeros pueden terminar diciendo cosas distintas.
 *
 * LAS REGLAS SE MUESTRAN EN PALABRAS, NO EN SQL
 *
 * Este tablero es para mercadeo y para operacion. "Pedidos en total es mayor o
 * igual a 10" se entiende; "pedidos >= 10" obliga a saber que significa esa
 * columna. La definicion escrita a mano va primero y la regla va debajo, porque
 * la regla es la verdad -es lo que la base ejecuta- pero la frase es lo que se
 * lee.
 *
 * DOS CIFRAS POR SEGMENTO, NO UNA
 *
 * Personas y, debajo, a cuantas se les puede escribir. Un segmento de 195 mil
 * personas del que solo 124 mil autorizaron datos no es un grupo de 195 mil
 * para efectos de una campana, y mostrar solo el total invita a prometer
 * alcances que no existen.
 */

var tbDatos = null;

$(function () {
	tbCargar();
	$('#tb-cinta').on('click', '.tb-tramo', function () {
		tbVerLista($(this).data('segmento'));
	});
	$('#tb-tarjetas').on('click', '.tb-ver', function (e) {
		e.preventDefault();
		tbVerLista($(this).data('segmento'));
	});
});

function tbCargar() {
	$.ajax({
		url: server + 'AdministrarSegmentos',
		data: { accion: 'listar' },
		dataType: 'json',
		type: 'get',
		success: function (d) {
			if (d.error) {
				$('#tb-mensaje').html('<div class="tb-malo">' + tbEsc(d.error) + '</div>');
				return;
			}
			tbDatos = d;
			tbPintar(d);
		},
		error: function () {
			$('#tb-mensaje').html('<div class="tb-malo">No se pudo cargar el tablero.</div>');
		}
	});
}

function tbPintar(d) {
	var segs = d.segmentos || [];
	var total = d.total_personas || 0;
	var activos = 0;
	for (var i = 0; i < segs.length; i++) {
		if (segs[i].activo === 'S') { activos++; }
	}

	$('#tb-corte').html('Los n&uacute;meros son del &uacute;ltimo reparto, sobre el c&aacute;lculo del <b>' +
		tbEsc(d.calculado_en || 'sin calcular') + '</b>. ' +
		'Incluye los pedidos de mostrador, que en el central no existen.');

	$('#tb-k-personas').text(tbMil(total));
	$('#tb-k-contactables').text(tbMil(d.total_contactables));
	$('#tb-k-valor').text(tbMillones(d.total_valor));
	$('#tb-k-segmentos').text(tbMil(activos));

	tbCinta(segs, total);
	tbTarjetas(segs, total);

	var sc = d.sin_clasificar || 0;
	$('#tb-pie').html(sc > 0
		? '<b>' + tbMil(sc) + '</b> personas no cumplen ninguna definici&oacute;n y quedaron ' +
			'en <b>sin clasificar</b>. Si eso no era la intenci&oacute;n, a las reglas les ' +
			'falta un caso.'
		: 'Todas las personas de la base cumplen alguna definici&oacute;n.');
}

// ==========================================================================
// La cinta
// ==========================================================================

function tbCinta(segs, total) {
	var cinta = $('#tb-cinta').empty();
	var leyenda = $('#tb-leyenda').empty();
	if (!total) { return; }

	for (var i = 0; i < segs.length; i++) {
		var s = segs[i];
		if (!s.personas) { continue; }
		var pct = (s.personas / total) * 100;
		var color = tbColor(s.color);

		var tramo = $('<span class="tb-tramo"></span>')
			.css({ width: pct.toFixed(3) + '%', backgroundColor: color })
			.attr('data-segmento', s.nombre)
			.attr('title', s.nombre + ': ' + tbMil(s.personas) + ' personas (' +
				tbPct(pct) + ')');
		cinta.append(tramo);

		//La leyenda no sobra aunque la cinta tenga colores: los tramos chicos
		//-FIEL es el 1,2% de la base- quedan de dos pixeles y ahi el color no
		//alcanza a leerse ni pasando el mouse por encima.
		var llave = $('<span class="tb-llave"></span>');
		llave.append($('<span class="tb-punto"></span>').css('background-color', color));
		llave.append(document.createTextNode(s.nombre + '  ' + tbMil(s.personas) +
			'  (' + tbPct(pct) + ')'));
		leyenda.append(llave);
	}
}

// ==========================================================================
// Las tarjetas
// ==========================================================================

function tbTarjetas(segs, total) {
	var caja = $('#tb-tarjetas').empty();
	for (var i = 0; i < segs.length; i++) {
		caja.append(tbTarjeta(segs[i], total));
	}
}

function tbTarjeta(s, total) {
	var color = tbColor(s.color);
	var pct = total ? (s.personas / total) * 100 : 0;
	var pctCont = s.personas ? (s.contactables / s.personas) * 100 : 0;

	var col = $('<div class="col-md-4 col-sm-6"></div>');
	var t = $('<div class="tb-tarjeta"></div>').css('border-left-color', color);
	if (s.activo !== 'S') { t.addClass('tb-apagado'); }

	var cab = $('<div></div>');
	cab.append($('<span class="tb-nombre"></span>').css('color', color).text(s.nombre));
	if (s.activo !== 'S') {
		cab.append($('<span class="tb-nota"></span>').text('  apagado'));
	}
	t.append(cab);

	t.append($('<div class="tb-desc"></div>').text(s.descripcion || 'Sin descripción.'));
	t.append($('<div class="tb-reglas"></div>').html(tbReglas(s.reglas)));

	t.append($('<div class="tb-cifra"></div>').css('color', color).text(tbMil(s.personas)));
	t.append($('<div class="tb-pct"></div>').text('personas · ' + tbPct(pct) + ' de la base'));

	var sub = $('<div class="tb-sub"></div>');
	sub.append(document.createTextNode('Se les puede escribir a ' + tbMil(s.contactables) +
		' (' + tbPct(pctCont) + ')'));
	sub.append($('<div class="tb-barrita"></div>')
		.append($('<div></div>').css({ width: pctCont.toFixed(1) + '%', backgroundColor: color })));
	sub.append($('<div style="margin-top:6px;"></div>')
		.text('Compras: ' + tbMillones(s.valor)));
	t.append(sub);

	t.append($('<div style="margin-top:9px;"></div>').append(
		$('<a href="#" class="tb-ver">Ver la lista</a>').attr('data-segmento', s.nombre)));

	return (col.append(t));
}

/** Las reglas en palabras. La lista de campos viene en la misma respuesta. */
function tbReglas(reglas) {
	if (!reglas || !reglas.length) {
		return '<span style="color:#C21C1F;">sin reglas, no clasifica a nadie</span>';
	}
	var partes = [];
	for (var i = 0; i < reglas.length; i++) {
		partes.push(tbEsc(tbEtiqueta(reglas[i].campo)) + ' ' +
			tbEsc(tbOperador(reglas[i].operador)) + ' <b>' + tbEsc(reglas[i].valor) + '</b>');
	}
	return partes.join('<span class="tb-y"> y </span>');
}

function tbEtiqueta(campo) {
	var campos = (tbDatos && tbDatos.campos) || [];
	for (var i = 0; i < campos.length; i++) {
		if (campos[i].campo === campo) { return campos[i].etiqueta; }
	}
	return campo;
}

function tbOperador(op) {
	if (op === '>=') { return 'es mayor o igual a'; }
	if (op === '<=') { return 'es menor o igual a'; }
	if (op === '=') { return 'es igual a'; }
	if (op === '>') { return 'es mayor que'; }
	if (op === '<') { return 'es menor que'; }
	if (op === '<>') { return 'es distinto de'; }
	return op;
}

// ==========================================================================

/** Se abre en otra pestana para no perder el tablero. */
function tbVerLista(nombre) {
	if (!nombre) { return; }
	window.open('SegmentacionPersona.html?segmento=' + encodeURIComponent(nombre), '_blank');
}

function tbColor(c) {
	return (/^#[0-9A-Fa-f]{6}$/.test(c || '') ? c : '#8A919E');
}

function tbMil(n) {
	n = Number(n) || 0;
	return Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** Un porcentaje con coma: 1.24 -> "1,2%". Los menores a 0,1 no se redondean a 0. */
function tbPct(p) {
	p = Number(p) || 0;
	if (p > 0 && p < 0.1) { return 'menos de 0,1%'; }
	return p.toFixed(1).replace('.', ',') + '%';
}

/**
 * Las compras van en millones y no en pesos sueltos: 18.569.123.456 en una
 * tarjeta es una cifra que nadie lee, y aqui lo que importa es el tamano
 * relativo de un segmento contra otro, no el peso exacto.
 */
function tbMillones(v) {
	v = Number(v) || 0;
	if (v >= 1000000000) {
		return '$' + (v / 1000000000).toFixed(1).replace('.', ',') + ' mil millones';
	}
	if (v >= 1000000) {
		return '$' + tbMil(v / 1000000) + ' millones';
	}
	return '$' + tbMil(v);
}

function tbEsc(s) {
	return $('<div></div>').text(s === null || s === undefined ? '' : s).html();
}

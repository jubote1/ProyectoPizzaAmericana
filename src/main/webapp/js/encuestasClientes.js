/** encuestasClientes.js
 *
 * Reporte de las encuestas de satisfaccion que responden los clientes (link por WhatsApp, QR de la mesa
 * y QR de la factura). Pantalla EncuestasClientes.html, que va dentro de la pestana "Encuestas de Clientes"
 * de Reportes de Mercadeo.
 *
 * Los datos vienen del servicio ConsultarEncuestasClientes, en una sola llamada: resumen por pregunta,
 * comparacion contra la red, patrones y el detalle encuesta por encuesta.
 *
 * Las preguntas NO estan escritas aqui: llegan del servidor segun su configuracion (pregunta_servicio) con su
 * tipo -valoracion, calificacion, cerrada o abierta- y la pantalla pinta cada una segun su tipo. Una pregunta
 * nueva aparece sola. Los textos con tilde van con secuencias \u para que el archivo sea ASCII.
 */

var ecDatos = null;          //la ultima respuesta del servidor
var ecFiltradas = [];        //el detalle con el filtro aplicado
var ecMostradas = 0;         //cuantas filas del detalle se estan mostrando
var EC_PAGINA = 150;
var ecPreguntasTabla = [];   //preguntas que van como columna del detalle (todas menos las abiertas)

$(function () {
	ecCargarTiendas();
	ecRango('mes');
});

// ------------------------------------------------------------------------------------------------
// Filtros
// ------------------------------------------------------------------------------------------------

function ecCargarTiendas() {
	var str = '<option value="0">TODAS LAS TIENDAS</option>';
	$.getJSON(server + 'GetTiendasFuncionales', function (data) {
		for (var i = 0; i < data.length; i++) {
			str += '<option value="' + data[i].id + '">' + data[i].nombre + '</option>';
		}
		$('#selectTiendas').html(str);
		//El administrador de punto de venta queda con su tienda; el servidor igual la fuerza.
		tpRecortarCombo('#selectTiendas', '#avisoTiendaPermitida');
		ecConsultar();
	}).fail(function () {
		$('#selectTiendas').html(str);
		ecConsultar();
	});
}

function ecAFecha(f) {
	var m = f.getMonth() + 1, d = f.getDate();
	return f.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (d < 10 ? '0' : '') + d;
}

function ecRango(tipo) {
	var hoy = new Date();
	var desde, hasta = hoy;
	if (tipo === 'mes') {
		desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
	} else if (tipo === 'mespasado') {
		desde = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
		hasta = new Date(hoy.getFullYear(), hoy.getMonth(), 0);
	} else {
		desde = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - parseInt(tipo, 10) + 1);
	}
	$('#fechainicial').val(ecAFecha(desde));
	$('#fechafinal').val(ecAFecha(hasta));
	//Los rangos rapidos consultan de una vez; la primera vez lo hace la carga de tiendas.
	if (ecDatos !== null) {
		ecConsultar();
	}
}

function ecConsultar() {
	var idtienda = $('#selectTiendas').val() || '0';
	var desde = $('#fechainicial').val();
	var hasta = $('#fechafinal').val();
	if (!desde || !hasta) {
		ecMensaje('Seleccione la fecha inicial y la fecha final.', 'warning');
		return;
	}
	$('#btnconsultar').val('Consultando...').prop('disabled', true);
	ecMensaje('', '');
	$.ajax({
		url: server + 'ConsultarEncuestasClientes',
		dataType: 'json',
		type: 'post',
		data: { 'idtienda': idtienda, 'fechainicial': desde, 'fechafinal': hasta },
		success: function (d) {
			if (!d || d.resultado !== 'OK') {
				$('#ecResultados').hide();
				ecMensaje((d && d.mensaje) || 'No se pudo consultar el reporte.', 'danger');
				return;
			}
			ecDatos = d;
			ecPintar(d);
		},
		error: function () {
			ecMensaje('No hubo respuesta del servidor al consultar el reporte.', 'danger');
		},
		complete: function () {
			$('#btnconsultar').val('Consultar').prop('disabled', false);
		}
	});
}

function ecMensaje(texto, tipo) {
	$('#ecMensaje').html(texto ? '<div class="alert alert-' + tipo + '">' + texto + '</div>' : '');
}

function ecTab(id) {
	$('.ec-tabs li').removeClass('activo');
	$('.ec-tabs li[data-panel="' + id + '"]').addClass('activo');
	$('.ec-panel').removeClass('activo');
	$('#' + id).addClass('activo');
}

// ------------------------------------------------------------------------------------------------
// Utilidades
// ------------------------------------------------------------------------------------------------

function esc(s) {
	return String(s === null || s === undefined ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function num(n, dec) {
	if (n === null || n === undefined) { return '-'; }
	return Number(n).toFixed(dec === undefined ? 1 : dec).replace('.', ',');
}

function miles(n) {
	return String(n).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function sinTildes(s) {
	return String(s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
}

/** Los titulos vienen en minuscula y sin tilde: se les pone tilde y mayuscula inicial para mostrarlos. */
function bonito(texto) {
	var t = String(texto || '').toLowerCase();
	var mapa = {
		'atencion': 'atención', 'satisfaccion': 'satisfacción', 'recomendarias': 'recomendarías',
		'organizacion': 'organización', 'orga.': 'organización', 'calificacion': 'calificación',
		'limpieza y organizacion': 'limpieza y organización', 'sugerencia': 'sugerencia'
	};
	t = t.replace(/[a-zà-ÿ\.]+/g, function (w) { return mapa[w] || w; });
	return t.charAt(0).toUpperCase() + t.slice(1);
}

function esValoracion(p) { return p.tipo === 'valoracion'; }
function esCalificacion(p) { return p.tipo === 'calificacion'; }
function esPuntuable(p) { return esValoracion(p) || esCalificacion(p); }

/** Verde, ambar o rojo segun el promedio y la escala de la pregunta. */
function claseProm(valor, escala) {
	if (valor === null || valor === undefined) { return ''; }
	var r = valor / escala;
	return r >= 0.9 ? 'bien' : (r >= 0.8 ? 'medio' : 'mal');
}

function nivelValoracion(texto) {
	var t = sinTildes(texto);
	if (t === 'muy buena' || t === 'muy bueno' || t === 'excelente') { return 4; }
	if (t === 'buena' || t === 'bueno') { return 3; }
	if (t === 'regular') { return 2; }
	if (t === 'mala' || t === 'malo' || t === 'muy mala' || t === 'muy malo') { return 1; }
	return 0;
}

function fechaCorta(f) {
	// 2026-11-03 16:57 -> 03/11/2026 16:57
	var m = /^(\d{4})-(\d{2})-(\d{2})(.*)$/.exec(f || '');
	return m ? (m[3] + '/' + m[2] + '/' + m[1] + m[4]) : (f || '');
}

// ------------------------------------------------------------------------------------------------
// Pintado general
// ------------------------------------------------------------------------------------------------

function ecPintar(d) {
	$('#ecResultados').show();
	if (d.totales.encuestas === 0) {
		$('#ecTarjetas').html('');
		$('#ecResumen').html('<p class="ec-vacio">No hay encuestas contestadas en este rango para ' + esc(d.tienda) + '. Pruebe con un rango m&aacute;s amplio.</p>');
		$('#ecPalabras,#ecTipos,#ecDias,#ecSemanas').html('');
		$('#detTabla').html('');
		$('#detConteo,#detNota').text('');
		$('#detMas').hide();
		return;
	}
	ecPintarTarjetas(d);
	ecPintarResumen(d);
	ecPintarPatrones(d);
	ecPreparaDetalle(d);
	ecPintarDetalle(true);
}

function ecPrimera(d, tipo, texto) {
	var elegida = null;
	for (var i = 0; i < d.preguntas.length; i++) {
		var p = d.preguntas[i];
		if (p.tipo !== tipo) { continue; }
		if (elegida === null) { elegida = p; }
		if (texto && sinTildes(p.titulo + ' ' + p.descripcion).indexOf(texto) >= 0) { return p; }
	}
	return elegida;
}

function ecTarjeta(k, v, pie, clase) {
	return '<div class="ec-tarjeta"><div class="k">' + k + '</div><div class="v ' + (clase || '') + '">' + v + '</div><div class="p">' + pie + '</div></div>';
}

function ecCompara(valor, red, dec) {
	if (red === undefined || red === null || valor === null || valor === undefined) { return ''; }
	var dif = valor - red;
	var flecha = dif > 0.05 ? '<span class="bien">&#9650; ' + num(Math.abs(dif), dec) + '</span>' : (dif < -0.05 ? '<span class="mal">&#9660; ' + num(Math.abs(dif), dec) + '</span>' : '<span>igual</span>');
	return ' &middot; red ' + num(red, dec) + ' (' + flecha + ')';
}

function ecPintarTarjetas(d) {
	var t = d.totales, h = '';
	var enRed = (d.tienda !== 'Todas las tiendas');
	h += ecTarjeta('Encuestas contestadas', miles(t.encuestas), enRed ? ('de ' + miles(t.encuestasRed) + ' en toda la red') : 'en toda la red');

	var aten = ecPrimera(d, 'valoracion', 'atencion');
	if (aten) {
		h += ecTarjeta(esc(bonito(aten.descripcion)), num(aten.promedio) + ' <small>de 4</small>',
			num(aten.pctBuena) + ' % buena o muy buena' + (aten.red ? ecCompara(aten.promedio, aten.red.promedio, 1) : ''), claseProm(aten.promedio, 4));
	}
	var rec = ecPrimera(d, 'calificacion', 'recomend');
	if (rec) {
		var clase = rec.indice >= 70 ? 'bien' : (rec.indice >= 50 ? 'medio' : 'mal');
		h += ecTarjeta('&Iacute;ndice de recomendaci&oacute;n', num(rec.indice, 0),
			'promedio ' + num(rec.promedio) + ' de 10' + (rec.red ? ecCompara(rec.indice, rec.red.indice, 0).replace(' &middot; red', ' &middot; red') : ''), clase);
	}
	var inc = ecPrimera(d, 'cerrada', 'inconvenient');
	if (inc) {
		h += ecTarjeta('Con inconvenientes', num(inc.pctSi) + ' <small>%</small>',
			miles(t.conInconvenientes) + ' de ' + miles(inc.n) + ' encuestas' + (inc.red ? ' &middot; red ' + num(inc.red.pctSi) + ' %' : ''), inc.pctSi > 10 ? 'mal' : (inc.pctSi > 5 ? 'medio' : 'bien'));
	}
	h += ecTarjeta('Comentarios', miles(t.conComentario), 'con algo m&aacute;s que "no" o "ninguno"');
	h += ecTarjeta('Respuesta a los env&iacute;os', num(t.tasaRespuesta) + ' <small>%</small>',
		miles(t.contestadasDelEnvio) + ' de ' + miles(t.enviadas) + ' enviadas por link; el resto entra por QR');
	$('#ecTarjetas').html(h);
}

// ------------------------------------------------------------------------------------------------
// Resultados: un bloque por pregunta
// ------------------------------------------------------------------------------------------------

function ecFila(etiqueta, porcentaje, cantidad, color, maximo) {
	var ancho = maximo > 0 ? Math.max(0, porcentaje) / maximo * 100 : 0;
	return '<div class="ec-fila"><div class="et">' + esc(etiqueta) + '</div><div class="bar"><i style="width:' + ancho.toFixed(1) + '%;background:' + color + '"></i></div>'
		+ '<div class="cif"><b>' + miles(cantidad) + '</b> &middot; ' + num(porcentaje) + ' %</div></div>';
}

function ecPintarResumen(d) {
	var h = '';
	for (var i = 0; i < d.preguntas.length; i++) {
		var p = d.preguntas[i];
		h += '<div class="ec-bloque"><h4>' + esc(bonito(p.descripcion)) + '</h4>';
		if (esValoracion(p)) {
			h += '<div class="nota">' + miles(p.n) + ' respuestas &middot; promedio <b>' + num(p.promedio) + '</b> de 4 &middot; <b>' + num(p.pctBuena) + ' %</b> buena o muy buena'
				+ (p.red ? ' &middot; en la red: promedio ' + num(p.red.promedio) + ', ' + num(p.red.pctBuena) + ' % buena o muy buena' : '') + '</div>';
			var colores = { '4': '#0b8a4a', '3': '#6fb98f', '2': '#d98c00', '1': '#E42528' };
			for (var j = 0; j < p.distribucion.length; j++) {
				var o = p.distribucion[j];
				h += ecFila(o.etiqueta, o.porcentaje, o.cantidad, colores[o.valor], 100);
			}
		} else if (esCalificacion(p)) {
			var tot = p.n || 1;
			var pp = p.promotores / tot * 100, ps = p.pasivos / tot * 100, pd = p.detractores / tot * 100;
			h += '<div class="nota">' + miles(p.n) + ' respuestas &middot; promedio <b>' + num(p.promedio) + '</b> de 10 &middot; &iacute;ndice de recomendaci&oacute;n <b>' + num(p.indice, 0) + '</b> (promotores menos detractores)'
				+ (p.red ? ' &middot; en la red: promedio ' + num(p.red.promedio) + ', &iacute;ndice ' + num(p.red.indice, 0) : '') + '</div>';
			h += '<div class="ec-nps"><span style="width:' + pp + '%;background:#0b8a4a">' + (pp >= 6 ? num(pp, 0) + ' %' : '') + '</span>'
				+ '<span style="width:' + ps + '%;background:#9aa3b5">' + (ps >= 6 ? num(ps, 0) + ' %' : '') + '</span>'
				+ '<span style="width:' + pd + '%;background:#E42528">' + (pd >= 6 ? num(pd, 0) + ' %' : '') + '</span></div>';
			h += '<div class="ec-leyenda" style="margin-bottom:8px;"><b style="color:#0b8a4a">' + miles(p.promotores) + ' promotores</b> (9 y 10) &middot; ' + miles(p.pasivos) + ' pasivos (7 y 8) &middot; <b style="color:#E42528">' + miles(p.detractores) + ' detractores</b> (1 a 6)</div>';
			var maxCal = 0;
			for (var k = 0; k < p.distribucion.length; k++) { if (p.distribucion[k].porcentaje > maxCal) { maxCal = p.distribucion[k].porcentaje; } }
			for (var m = 0; m < p.distribucion.length; m++) {
				var oc = p.distribucion[m], v = parseInt(oc.valor, 10);
				h += ecFila('Calificaron ' + oc.etiqueta, oc.porcentaje, oc.cantidad, v >= 9 ? '#0b8a4a' : (v >= 7 ? '#9aa3b5' : '#E42528'), maxCal);
			}
		} else if (p.tipo === 'cerrada') {
			var malo = sinTildes(p.titulo + ' ' + p.descripcion).indexOf('inconvenient') >= 0;
			h += '<div class="nota">' + miles(p.n) + ' respuestas' + (p.red ? ' &middot; en la red: ' + num(p.red.pctSi) + ' % dijo que s&iacute;' : '') + '</div>';
			for (var q = 0; q < p.distribucion.length; q++) {
				var oq = p.distribucion[q];
				var si = (oq.valor === 'Si');
				h += ecFila(si ? 'Sí' : 'No', oq.porcentaje, oq.cantidad, malo ? (si ? '#E42528' : '#0b8a4a') : '#102F6F', 100);
			}
		} else {
			h += '<div class="nota">' + miles(p.n) + ' respuestas, de las cuales <b>' + miles(p.conComentario) + '</b> dicen algo &uacute;til.</div>'
				+ '<input type="button" class="btn btn-default btn-sm" value="Ver los comentarios" onclick="ecVerComentarios()">';
		}
		h += '</div>';
	}
	$('#ecResumen').html(h);
}

function ecVerComentarios() {
	$('#detFiltro').val('coment');
	$('#detBuscar').val('');
	ecTab('ecDetalle');
	ecPintarDetalle(true);
}

// ------------------------------------------------------------------------------------------------
// Patrones
// ------------------------------------------------------------------------------------------------

function ecPuntuables(d) {
	var r = [];
	for (var i = 0; i < d.preguntas.length; i++) { if (esPuntuable(d.preguntas[i])) { r.push(d.preguntas[i]); } }
	return r;
}

function ecCelda(p, promedios) {
	var v = promedios ? promedios[String(p.idpregunta)] : undefined;
	if (v === undefined) { return '<td class="n">-</td>'; }
	return '<td class="n ' + claseProm(v, esValoracion(p) ? 4 : 10) + '"><b>' + num(v) + '</b></td>';
}

function ecCabeceraProm(pregs) {
	var h = '';
	for (var i = 0; i < pregs.length; i++) {
		h += '<th class="n">' + esc(bonito(pregs[i].descripcion)) + '<br><span style="font-weight:400">de ' + (esValoracion(pregs[i]) ? 4 : 10) + '</span></th>';
	}
	return h;
}

function ecPintarPatrones(d) {
	var pregs = ecPuntuables(d), i, j;

	// palabras
	var pal = '';
	for (i = 0; i < d.palabras.length; i++) {
		pal += '<a onclick="ecBuscarPalabra(\'' + esc(d.palabras[i].palabra) + '\')">' + esc(d.palabras[i].palabra) + ' <small>' + d.palabras[i].veces + '</small></a>';
	}
	$('#ecPalabras').html(pal || '<p class="ec-vacio">Todav&iacute;a no hay suficientes comentarios para encontrar palabras repetidas.</p>');

	// por tipo de atencion
	var th = '<table class="ec-tabla"><tr><th>Atenci&oacute;n</th><th class="n">Encuestas</th>' + ecCabeceraProm(pregs) + '</tr>';
	for (i = 0; i < d.tipos.length; i++) {
		var ti = d.tipos[i];
		th += '<tr><td>' + esc(ti.tipo.charAt(0).toUpperCase() + ti.tipo.slice(1)) + '</td><td class="n">' + miles(ti.encuestas) + '</td>';
		for (j = 0; j < pregs.length; j++) { th += ecCelda(pregs[j], ti.promedios); }
		th += '</tr>';
	}
	$('#ecTipos').html(th + '</table>');

	// por dia
	var maxDia = 0;
	for (i = 0; i < d.dias.length; i++) { if (d.dias[i].encuestas > maxDia) { maxDia = d.dias[i].encuestas; } }
	var td = '<table class="ec-tabla"><tr><th>D&iacute;a</th><th class="n">Encuestas</th><th style="width:90px"></th>' + ecCabeceraProm(pregs) + '</tr>';
	for (i = 0; i < d.dias.length; i++) {
		var di = d.dias[i];
		td += '<tr><td>' + esc(di.dia) + '</td><td class="n">' + miles(di.encuestas) + '</td><td><div class="bar" style="background:#eceff6;border-radius:4px;height:10px"><i style="display:block;height:10px;border-radius:4px;background:#102F6F;width:' + (maxDia ? di.encuestas / maxDia * 100 : 0).toFixed(0) + '%"></i></div></td>';
		for (j = 0; j < pregs.length; j++) { td += ecCelda(pregs[j], di.promedios); }
		td += '</tr>';
	}
	$('#ecDias').html(td + '</table>');

	// semanas: se marca la mejor y la peor por la primera pregunta de calificacion (o la primera puntuable), solo con 5 o mas encuestas
	var guia = pregs.length ? ecPrimera(d, 'calificacion') || pregs[0] : null;
	var mejor = -1, peor = -1, vMejor = -1, vPeor = 1e9;
	if (guia) {
		for (i = 0; i < d.semanas.length; i++) {
			var vv = d.semanas[i].promedios[String(guia.idpregunta)];
			if (vv === undefined || d.semanas[i].encuestas < 5) { continue; }
			if (vv > vMejor) { vMejor = vv; mejor = i; }
			if (vv < vPeor) { vPeor = vv; peor = i; }
		}
		if (mejor === peor) { mejor = -1; peor = -1; }
	}
	var maxSem = 0;
	for (i = 0; i < d.semanas.length; i++) { if (d.semanas[i].encuestas > maxSem) { maxSem = d.semanas[i].encuestas; } }
	var ts = '<table class="ec-tabla"><tr><th>Semana del</th><th class="n">Encuestas</th><th style="width:140px"></th>' + ecCabeceraProm(pregs) + '</tr>';
	for (i = 0; i < d.semanas.length; i++) {
		var s = d.semanas[i];
		var marca = (i === mejor) ? ' style="background:#eaf6ee"' : ((i === peor) ? ' style="background:#FDECEC"' : '');
		ts += '<tr' + marca + '><td>' + fechaCorta(s.semana) + '</td><td class="n">' + miles(s.encuestas) + '</td><td><div style="background:#eceff6;border-radius:4px;height:10px"><i style="display:block;height:10px;border-radius:4px;background:#102F6F;width:' + (maxSem ? s.encuestas / maxSem * 100 : 0).toFixed(0) + '%"></i></div></td>';
		for (j = 0; j < pregs.length; j++) { ts += ecCelda(pregs[j], s.promedios); }
		ts += '</tr>';
	}
	$('#ecSemanas').html(ts + '</table>');
}

function ecBuscarPalabra(palabra) {
	$('#detFiltro').val('coment');
	$('#detBuscar').val(palabra);
	ecTab('ecDetalle');
	ecPintarDetalle(true);
}

// ------------------------------------------------------------------------------------------------
// Detalle encuesta por encuesta
// ------------------------------------------------------------------------------------------------

function ecPreparaDetalle(d) {
	ecPreguntasTabla = [];
	for (var i = 0; i < d.preguntas.length; i++) {
		if (d.preguntas[i].tipo !== 'abierta') { ecPreguntasTabla.push(d.preguntas[i]); }
	}
	var cab = '<tr><th>Fecha</th>' + (d.tienda === 'Todas las tiendas' ? '<th>Tienda</th>' : '') + '<th class="n">Pedido</th><th>Atenci&oacute;n</th><th>Cliente</th>';
	for (var j = 0; j < ecPreguntasTabla.length; j++) { cab += '<th>' + esc(bonito(ecPreguntasTabla[j].descripcion)) + '</th>'; }
	cab += '<th>Comentario</th></tr>';
	$('#detTabla').data('cabecera', cab);
}

function ecPill(p, texto) {
	if (texto === undefined || texto === null || texto === '') { return '-'; }
	if (esValoracion(p)) {
		var n = nivelValoracion(texto);
		return '<span class="pill v' + (n || 2) + '">' + esc(texto) + '</span>';
	}
	if (esCalificacion(p)) {
		var v = parseInt(texto, 10);
		return '<span class="pill ' + (v >= 9 ? 'v4' : (v >= 7 ? 'v2' : 'v1')) + '">' + esc(texto) + '</span>';
	}
	var si = sinTildes(texto) === 'si';
	return '<span class="pill ' + (si ? 'si' : 'no') + '">' + (si ? 'Sí' : 'No') + '</span>';
}

function ecFiltrarDetalle() {
	var filtro = $('#detFiltro').val();
	var buscar = sinTildes($('#detBuscar').val());
	var r = [];
	for (var i = 0; i < ecDatos.detalle.length; i++) {
		var e = ecDatos.detalle[i];
		if (filtro === 'bajas' && !e.baja) { continue; }
		if (filtro === 'coment' && !e.comentario) { continue; }
		if ((filtro === 'domicilio' || filtro === 'punto de venta') && sinTildes(e.tipo) !== filtro) { continue; }
		if (buscar && sinTildes(e.comentario + ' ' + e.pedido + ' ' + e.cliente).indexOf(buscar) < 0) { continue; }
		r.push(e);
	}
	return r;
}

function ecPintarDetalle(desdeCero) {
	if (!ecDatos) { return; }
	if (desdeCero) {
		ecFiltradas = ecFiltrarDetalle();
		ecMostradas = 0;
		$('#detTabla').html($('#detTabla').data('cabecera') || '');
	}
	var todas = (ecDatos.tienda === 'Todas las tiendas');
	var hasta = Math.min(ecFiltradas.length, ecMostradas + EC_PAGINA);
	var h = '';
	for (var i = ecMostradas; i < hasta; i++) {
		var e = ecFiltradas[i];
		h += '<tr' + (e.baja ? ' class="baja"' : '') + '><td style="white-space:nowrap">' + fechaCorta(e.fecha) + '</td>' + (todas ? '<td>' + esc(e.tienda) + '</td>' : '')
			+ '<td class="n">' + e.pedido + '</td><td>' + esc(e.tipo) + '</td><td>' + esc(e.cliente) + '</td>';
		for (var j = 0; j < ecPreguntasTabla.length; j++) {
			h += '<td>' + ecPill(ecPreguntasTabla[j], e.resp[String(ecPreguntasTabla[j].idpregunta)]) + '</td>';
		}
		h += '<td class="coment">' + esc(e.comentario) + '</td></tr>';
	}
	$('#detTabla').append(h);
	ecMostradas = hasta;
	$('#detConteo').text('Mostrando ' + miles(ecMostradas) + ' de ' + miles(ecFiltradas.length) + ' encuestas');
	$('#detMas').toggle(ecMostradas < ecFiltradas.length);
	var nota = '';
	if (ecDatos.detalle.length >= ecDatos.detalleTope && ecDatos.totales.encuestas > ecDatos.detalleTope) {
		nota = 'El detalle trae las ' + miles(ecDatos.detalleTope) + ' encuestas más recientes del rango; los resultados de arriba cuentan todas.';
	}
	if (ecFiltradas.length === 0) { nota = 'Ninguna encuesta coincide con el filtro.'; }
	$('#detNota').text(nota);
}

function ecMas() {
	ecPintarDetalle(false);
}

// ------------------------------------------------------------------------------------------------
// Exportar
// ------------------------------------------------------------------------------------------------

function ecCsv(v) {
	var s = String(v === null || v === undefined ? '' : v).replace(/"/g, '""');
	return '"' + s + '"';
}

function ecExportar() {
	if (!ecDatos || ecFiltradas.length === 0) { return; }
	var cab = ['Fecha', 'Tienda', 'Pedido', 'Atencion', 'Cliente'];
	for (var j = 0; j < ecPreguntasTabla.length; j++) { cab.push(bonito(ecPreguntasTabla[j].descripcion)); }
	cab.push('Comentario');
	var filas = [cab.map(ecCsv).join(';')];
	for (var i = 0; i < ecFiltradas.length; i++) {
		var e = ecFiltradas[i];
		var f = [fechaCorta(e.fecha), e.tienda, e.pedido, e.tipo, e.cliente];
		for (var k = 0; k < ecPreguntasTabla.length; k++) { f.push(e.resp[String(ecPreguntasTabla[k].idpregunta)] || ''); }
		f.push(e.comentario);
		filas.push(f.map(ecCsv).join(';'));
	}
	var blob = new Blob(['﻿' + filas.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
	var nombre = 'encuestas_clientes_' + sinTildes(ecDatos.tienda).replace(/[^a-z0-9]+/g, '_') + '_' + $('#fechainicial').val() + '_' + $('#fechafinal').val() + '.csv';
	if (window.navigator && window.navigator.msSaveBlob) {
		window.navigator.msSaveBlob(blob, nombre);
		return;
	}
	var a = document.createElement('a');
	a.href = URL.createObjectURL(blob);
	a.download = nombre;
	document.body.appendChild(a);
	a.click();
	document.body.removeChild(a);
}

/** tableroPuntos.js
 *
 * Pantalla TableroPuntos.html: el plan de puntos (Puntos Pizza) en una sola vista.
 *
 *   Resumen    -> afiliados, puntos asignados / vigentes / redimidos / vencidos, por vencer a 15, 30 y
 *                 60 dias, redenciones del periodo, series de 12 meses y reparto por tienda.
 *   Consultas  -> rankings: clientes con mas puntos, clientes y empleados que mas redimen, empleados
 *                 que mas acumulan. Se bajan a CSV desde la pantalla.
 *   Por vencer -> la lista de clientes con puntos por vencerse, con su CSV completo para avisarles.
 *
 * Todo viene de ConsultarTableroPuntos (acciones RESUMEN, RANKING, PORVENCER y CSVPORVENCER). El
 * servidor decide quien puede ver esto; aqui solo se pinta.
 *
 * Los textos con tilde van con secuencias \\u para que el archivo quede en ASCII, igual que en las
 * demas pantallas de reporteria.
 */

var tpDatos = null;          // ultima respuesta del resumen
var tpFilasConsulta = [];    // filas de la consulta mostrada, para el CSV
var tpColsConsulta = [];     // columnas de esa consulta

$(function () {
	tpFechasPorDefecto();
	$('#tpActualizar').click(tpActualizar);
	$('#tp7').click(function () { tpRango(6); });
	$('#tp30').click(function () { tpRango(29); });
	$('#tp90').click(function () { tpRango(89); });
	$('#tpMes').click(function () {
		var h = new Date(), d = new Date(h.getFullYear(), h.getMonth(), 1);
		$('#tpDesde').val(tpIso(d)); $('#tpHasta').val(tpIso(h)); tpActualizar();
	});
	$('#tpMesPas').click(function () {
		var h = new Date(), d = new Date(h.getFullYear(), h.getMonth() - 1, 1), f = new Date(h.getFullYear(), h.getMonth(), 0);
		$('#tpDesde').val(tpIso(d)); $('#tpHasta').val(tpIso(f)); tpActualizar();
	});
	$('#tpTabs a').click(function () {
		var v = $(this).data('vista');
		$('#tpTabs li').removeClass('activo'); $(this).parent().addClass('activo');
		$('.tp-vista').removeClass('activo'); $('.tp-vista[data-vista="' + v + '"]').addClass('activo');
		if (v === 'consultas' && tpFilasConsulta.length === 0) { tpConsultar(); }
		if (v === 'vencer' && $('#tpCuerpoVencer').children().length === 0) { tpVencer(); }
	});
	$('#tpConsultar').click(tpConsultar);
	$('#tpConsulta').change(tpConsultar);
	$('#tpCsvConsulta').click(tpCsvConsulta);
	$('#tpConsultarVencer').click(tpVencer);
	$('#tpDiasVencer').change(tpVencer);
	tpActualizar();
});

// ---------------------------------------------------------------------------
// Auxiliares
// ---------------------------------------------------------------------------

function tpIso(d) {
	var m = d.getMonth() + 1, dd = d.getDate();
	return (d.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (dd < 10 ? '0' : '') + dd);
}
function tpFechasPorDefecto() {
	var h = new Date(), d = new Date(); d.setDate(d.getDate() - 29);
	$('#tpDesde').val(tpIso(d)); $('#tpHasta').val(tpIso(h));
}
function tpRango(dias) {
	var h = new Date(), d = new Date(); d.setDate(d.getDate() - dias);
	$('#tpDesde').val(tpIso(d)); $('#tpHasta').val(tpIso(h)); tpActualizar();
}
function tpMiles(n) {
	var t = String(Math.round(Number(n) || 0)), s = '';
	for (var i = 0; i < t.length; i++) {
		if (i > 0 && (t.length - i) % 3 === 0 && t.charAt(i - 1) !== '-') { s += '.'; }
		s += t.charAt(i);
	}
	return (s);
}
function tpPesos(n) { return ('$' + tpMiles(n)); }
function tpPct(a, b) { return (b > 0 ? (Math.round(a * 1000 / b) / 10).toString().replace('.', ',') + '%' : '0%'); }
function tpEsc(t) { return (String(t == null ? '' : t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')); }
function tpNum(v) { return (Number(v) || 0); }
function tpMensaje(clase, texto) { $('#tpMensaje').html(texto ? '<div class="tp-aviso ' + clase + '">' + texto + '</div>' : ''); }
function tpTienda() { return ($('#tpTienda').val() || '0'); }

function tpPedir(params, ok) {
	$('#tpCargando').show();
	$.getJSON(server + 'ConsultarTableroPuntos', params, function (d) {
		$('#tpCargando').hide();
		if (!d || d.resultado !== 'OK') {
			tpMensaje('roja', tpEsc((d && d.mensaje) ? d.mensaje : 'No se pudo consultar.'));
			return;
		}
		tpMensaje('', '');
		ok(d);
	}).fail(function () {
		$('#tpCargando').hide();
		tpMensaje('roja', 'No se pudo comunicar con el servidor.');
	});
}

function tpTarjeta(k, v, p, clase) {
	return ('<div class="tp-tarjeta ' + (clase || '') + '"><div class="k">' + k + '</div><div class="v">' + v +
		'</div><div class="p">' + (p || '&nbsp;') + '</div></div>');
}

// ---------------------------------------------------------------------------
// Resumen
// ---------------------------------------------------------------------------

function tpActualizar() {
	var p = { accion: 'RESUMEN', fechainicial: $('#tpDesde').val(), fechafinal: $('#tpHasta').val(),
		diasactivo: $('#tpDiasActivo').val() || 90 };
	tpPedir(p, function (d) {
		tpDatos = d;
		//Quien no puede ver a los empleados ni siquiera ve esas dos consultas.
		if (d.puede_empleados === false) { $('#tpConsulta option[value^="EMPLEADOS"]').remove(); }
		tpLlenarTiendas(d.tiendas_lista || []);
		tpPintarResumen(d);
	});
}

function tpLlenarTiendas(lista) {
	var actual = $('#tpTienda').val() || '0';
	var html = '<option value="0">Todas</option>';
	for (var i = 0; i < lista.length; i++) {
		html += '<option value="' + lista[i].idtienda + '">' + tpEsc(lista[i].nombre) + '</option>';
	}
	$('#tpTienda').html(html).val(actual);
}

function tpPintarResumen(d) {
	var af = d.afiliados || {}, lb = d.libro || {}, pv = d.por_vencer || {};
	var asign = tpNum(lb.asignados), vig = tpNum(lb.vigentes), red = tpNum(lb.redimidos), ven = tpNum(lb.vencidos);

	$('#tpTarjClientes').html(
		tpTarjeta('Afiliados', tpMiles(af.total), tpMiles(af.activos) + ' activos &middot; +' + tpMiles(af.nuevos7) + ' en 7 d &middot; +' + tpMiles(af.nuevos30) + ' en 30 d') +
		tpTarjeta('Clientes activos', tpMiles(lb.activos), 'acumularon en los últimos ' + d.dias_activo + ' d &middot; ' + tpPct(tpNum(lb.activos), tpNum(af.total)) + ' de los afiliados') +
		tpTarjeta('Con puntos vigentes', tpMiles(lb.con_saldo), tpPct(tpNum(lb.con_saldo), tpNum(af.total)) + ' de los afiliados') +
		tpTarjeta('Clientes que han acumulado', tpMiles(lb.clientes), 'desde ' + tpEsc(lb.desde))
	);

	var dif = vig - tpNum(af.saldo_cliente);
	$('#tpTarjPuntos').html(
		tpTarjeta('Puntos asignados', tpMiles(asign), 'desde ' + tpEsc(lb.desde) + ' (todo el programa)') +
		tpTarjeta('Puntos vigentes', tpMiles(vig), 'saldo de los clientes según el libro' +
			(Math.abs(dif) > 1 ? '<br>el campo de cada cliente suma ' + tpMiles(af.saldo_cliente) + ' (' + (dif > 0 ? '+' : '') + tpMiles(dif) + ')' : '')) +
		tpTarjeta('Puntos redimidos', tpMiles(red), tpPct(red, asign) + ' de lo asignado') +
		tpTarjeta('Puntos vencidos', tpMiles(ven), tpPct(ven, asign) + ' de lo asignado')
	);

	$('#tpTarjVencer').html(
		tpTarjeta('Se vencen en 15 días', tpMiles(pv.p15), tpMiles(pv.c15) + ' clientes' + tpInvalidos(pv.p15inv), 'alerta') +
		tpTarjeta('Se vencen en 30 días', tpMiles(pv.p30), tpMiles(pv.c30) + ' clientes', 'alerta') +
		tpTarjeta('Se vencen en 60 días', tpMiles(pv.p60), tpMiles(pv.c60) + ' clientes' + tpInvalidos(pv.p60inv), 'alerta') +
		tpTarjeta('Fecha pasada, sin marcar', tpMiles(pv.pasados), tpMiles(pv.cpasados) + ' clientes: el proceso nocturno no los ha marcado', pv.pasados > 0 ? 'roja' : '')
	);

	var conf = tpEstado(d.redenciones_rango, 'CONFIRMADA'), rev = tpEstado(d.redenciones_rango, 'REVERSADA');
	var tot = tpEstado(d.redenciones_total, 'CONFIRMADA');
	$('#tpTarjRedenc').html(
		tpTarjeta('Redenciones', tpMiles(conf.n), tpMiles(conf.clientes) + ' clientes distintos') +
		tpTarjeta('Puntos redimidos', tpMiles(conf.puntos), 'en el período ' + $('#tpDesde').val() + ' a ' + $('#tpHasta').val()) +
		tpTarjeta('Reversadas', tpMiles(rev.n), tpMiles(rev.puntos) + ' puntos devueltos') +
		tpTarjeta('Redenciones, todo el programa', tpMiles(tot.n), tpMiles(tot.puntos) + ' puntos &middot; ' + tpMiles(tot.clientes) + ' clientes')
	);

	// Alertas de calidad de datos: lo que el tablero muestra pero no debe esconder.
	var al = '';
	if (tpNum(lb.vigentes_invalidos) > 0) {
		al += '<div class="tp-aviso">Hay <b>' + tpMiles(lb.vigentes_invalidos) + ' puntos vigentes</b> en <b>' + tpMiles(lb.correos_invalidos) +
			' correos que no son correos</b> (&laquo;no lo da&raquo;, &laquo;no tiene&raquo;&hellip;): los escribe la caja cuando el cliente no da el dato. ' +
			'Nadie los puede avisar ni redimir. Los rankings y las listas de abajo los dejan por fuera.</div>';
	}
	$('#tpAlertas').html(al);

	tpPintarSerie(d.serie || []);
	tpPintarAfiliaciones(d.serie || []);
	tpPintarBarras('#tpVencimientos', d.vencimientos || [], 'tramo', 'puntos', 'clientes',
		{ '1': '0 a 15 días', '2': '16 a 30 días', '3': '31 a 45 días', '4': '46 a 60 días' }, 'ro');
	tpPintarBarras('#tpSaldos', d.saldos || [], 'rango', 'clientes', 'puntos',
		{ '1': '0 a 99', '2': '100 a 149', '3': '150 a 199', '4': '200 a 249', '5': '250 a 299', '6': '300 o más' }, '');
	tpPintarCanales(d.afiliacion_canal || []);
	tpPintarPorTienda(d.por_tienda || []);
}

function tpInvalidos(p) { return (tpNum(p) > 0 ? '<br>' + tpMiles(p) + ' pts en correos inválidos' : ''); }

function tpEstado(lista, estado) {
	for (var i = 0; lista && i < lista.length; i++) {
		if (lista[i].estado === estado) { return ({ n: tpNum(lista[i].n), puntos: tpNum(lista[i].puntos), clientes: tpNum(lista[i].clientes) }); }
	}
	return ({ n: 0, puntos: 0, clientes: 0 });
}

function tpPintarSerie(serie) {
	var max = 1, i;
	for (i = 0; i < serie.length; i++) {
		max = Math.max(max, tpNum(serie[i].asignados), tpNum(serie[i].redimidos), tpNum(serie[i].vencidos));
	}
	var html = '';
	for (i = 0; i < serie.length; i++) {
		var s = serie[i];
		html += '<div class="tp-mes" title="' + tpEsc(s.mes) + ': asignados ' + tpMiles(s.asignados) + ' | redimidos ' + tpMiles(s.redimidos) +
			' | vencidos ' + tpMiles(s.vencidos) + '"><div class="barras">' +
			'<i style="background:#102F6F;height:' + Math.round(tpNum(s.asignados) * 100 / max) + '%"></i>' +
			'<i style="background:#0b8a4a;height:' + Math.round(tpNum(s.redimidos) * 100 / max) + '%"></i>' +
			'<i style="background:#E42528;height:' + Math.round(tpNum(s.vencidos) * 100 / max) + '%"></i>' +
			'</div>' + tpEsc(String(s.mes).substring(2)) + '</div>';
	}
	$('#tpSerie').html(html || '<div class="tp-vacio">Sin datos.</div>');
}

function tpPintarAfiliaciones(serie) {
	var maxA = 1, maxC = 1, i;
	for (i = 0; i < serie.length; i++) { maxA = Math.max(maxA, tpNum(serie[i].afiliaciones)); maxC = Math.max(maxC, tpNum(serie[i].clientes)); }
	var html = '';
	for (i = serie.length - 1; i >= 0; i--) {
		var s = serie[i];
		html += '<div class="tp-fila"><div class="et">' + tpEsc(s.mes) + '</div><div class="bar" style="background:none;height:auto;">' +
			'<div style="background:#eceff6;border-radius:4px;height:8px;margin-bottom:2px;"><i style="display:block;height:8px;border-radius:4px;background:#102F6F;width:' + Math.round(tpNum(s.afiliaciones) * 100 / maxA) + '%"></i></div>' +
			'<div style="background:#eceff6;border-radius:4px;height:8px;"><i style="display:block;height:8px;border-radius:4px;background:#FDC806;width:' + Math.round(tpNum(s.clientes) * 100 / maxC) + '%"></i></div>' +
			'</div><div class="cif">' + tpMiles(s.afiliaciones) + ' afil. &middot; ' + tpMiles(s.clientes) + ' acum.</div></div>';
	}
	$('#tpAfiliaciones').html(html + '<div class="tp-leyenda"><b style="background:#102F6F"></b>afiliaciones nuevas<b style="background:#FDC806"></b>clientes que acumularon</div>');
}

function tpPintarBarras(sel, lista, llave, valor, aux, etiquetas, color) {
	var max = 1, i;
	for (i = 0; i < lista.length; i++) { max = Math.max(max, tpNum(lista[i][valor])); }
	var html = '';
	for (i = 0; i < lista.length; i++) {
		var x = lista[i];
		html += '<div class="tp-fila"><div class="et">' + tpEsc(etiquetas[x[llave]] || x[llave]) + '</div>' +
			'<div class="bar"><i class="' + color + '" style="width:' + Math.round(tpNum(x[valor]) * 100 / max) + '%"></i></div>' +
			'<div class="cif">' + tpMiles(x[valor]) + ' <span style="color:#6C7482">(' + tpMiles(x[aux]) + ')</span></div></div>';
	}
	$(sel).html(html || '<div class="tp-vacio">Sin datos.</div>');
}

function tpPintarCanales(lista) {
	var max = 1, i, total = 0;
	for (i = 0; i < lista.length; i++) { max = Math.max(max, tpNum(lista[i].n)); total += tpNum(lista[i].n); }
	var html = '';
	for (i = 0; i < lista.length; i++) {
		html += '<div class="tp-fila"><div class="et">' + tpEsc(lista[i].canal) + '</div><div class="bar"><i style="width:' +
			Math.round(tpNum(lista[i].n) * 100 / max) + '%"></i></div><div class="cif">' + tpMiles(lista[i].n) + ' <span style="color:#6C7482">(' +
			tpPct(tpNum(lista[i].n), total) + ')</span></div></div>';
	}
	$('#tpCanales').html(html || '<div class="tp-vacio">Sin datos.</div>');
}

function tpPintarPorTienda(lista) {
	lista = lista.slice().sort(function (a, b) { return (tpNum(b.puntos) - tpNum(a.puntos)); });
	var html = '', t = { afil: 0, acum: 0, cli: 0, pts: 0, venta: 0, red: 0, ptsr: 0 };
	for (var i = 0; i < lista.length; i++) {
		var x = lista[i];
		t.afil += tpNum(x.afiliados); t.acum += tpNum(x.acumulaciones); t.cli += tpNum(x.clientes); t.pts += tpNum(x.puntos);
		t.venta += tpNum(x.venta); t.red += tpNum(x.redenciones); t.ptsr += tpNum(x.puntos_redimidos);
		html += '<tr><td>' + tpEsc(x.tienda) + '</td><td class="num">' + tpMiles(x.afiliados) + '</td><td class="num">' + tpMiles(x.acumulaciones) +
			'</td><td class="num">' + tpMiles(x.clientes) + '</td><td class="num">' + tpMiles(x.puntos) + '</td><td class="num">' + tpPesos(x.venta) +
			'</td><td class="num">' + tpMiles(x.redenciones) + '</td><td class="num">' + tpMiles(x.puntos_redimidos) +
			'</td><td class="num">' + tpPct(tpNum(x.puntos_redimidos), tpNum(x.puntos)) + '</td></tr>';
	}
	// "Clientes" no se suma: un mismo cliente acumula en varias tiendas.
	html += '<tr style="font-weight:700;background:#f4f6fb"><td>Total</td><td class="num">' + tpMiles(t.afil) + '</td><td class="num">' + tpMiles(t.acum) +
		'</td><td class="num">&mdash;</td><td class="num">' + tpMiles(t.pts) + '</td><td class="num">' + tpPesos(t.venta) + '</td><td class="num">' +
		tpMiles(t.red) + '</td><td class="num">' + tpMiles(t.ptsr) + '</td><td class="num">' + tpPct(t.ptsr, t.pts) + '</td></tr>';
	$('#tpPorTienda').html(html);
}

// ---------------------------------------------------------------------------
// Consultas
// ---------------------------------------------------------------------------

var TP_CONSULTAS = {
	CLIENTES_SALDO: {
		titulo: 'Clientes con más puntos vigentes',
		nota: 'Es el saldo de hoy: el período no cuenta. Solo correos válidos. Si escogió una tienda, cuenta los puntos acumulados allí.',
		cols: [['nombre', 'Cliente'], ['correo', 'Correo'], ['celular', 'Celular'], ['saldo', 'Puntos vigentes', 'n'],
			['por_vencer60', 'Se vencen en 60 d', 'n'], ['acumulaciones', 'Compras con puntos', 'n'], ['ultima', 'Última acumulación'], ['tiendas', 'Tiendas', 'n']]
	},
	CLIENTES_REDIMEN: {
		titulo: 'Clientes que más redimen',
		nota: 'Redenciones confirmadas dentro del período escogido. Solo correos válidos.',
		cols: [['nombre', 'Cliente'], ['correo', 'Correo'], ['celular', 'Celular'], ['redenciones', 'Redenciones', 'n'],
			['puntos', 'Puntos redimidos', 'n'], ['ultima', 'Última redención'], ['tiendas', 'Tiendas', 'n']]
	},
	EMPLEADOS_REDIMEN: {
		titulo: 'Empleados que más redimen',
		nota: 'Quién procesó la redención. Sin los canales (contact center, bot, app). La columna de usuario solo se llena desde septiembre de 2026. Para revisar un caso a fondo use la Auditoría de Fidelización.',
		cols: [['usuario', 'Usuario'], ['nombre', 'Nombre'], ['redenciones', 'Redenciones', 'n'], ['puntos', 'Puntos', 'n'],
			['clientes', 'Clientes distintos', 'n'], ['reversadas', 'Reversadas', 'n'], ['tiendas', 'Tiendas'], ['ultima', 'Última']]
	},
	EMPLEADOS_ACUMULAN: {
		titulo: 'Empleados que más acumulan puntos',
		nota: 'Quién atendió las compras que sumaron puntos. Sin los canales. Mucho volumen no es señal: un cajero de un turno fuerte acumula mucho. La señal es la concentración en pocos clientes (Auditoría).',
		cols: [['usuario', 'Usuario'], ['nombre', 'Nombre'], ['acumulaciones', 'Compras atendidas', 'n'], ['clientes', 'Clientes distintos', 'n'],
			['puntos', 'Puntos dados', 'n'], ['venta', 'Venta', 'p'], ['tiendas', 'Tiendas']]
	}
};

function tpConsultar() {
	var tipo = $('#tpConsulta').val(), def = TP_CONSULTAS[tipo];
	var p = { accion: 'RANKING', tipo: tipo, fechainicial: $('#tpDesde').val(), fechafinal: $('#tpHasta').val(),
		idtienda: tpTienda(), limite: $('#tpLimite').val() };
	tpPedir(p, function (d) {
		tpFilasConsulta = d.filas || [];
		tpColsConsulta = def.cols;
		$('#tpTituloConsulta').text(def.titulo);
		$('#tpRotuloConsulta').text(def.nota + ' Período: ' + d.desde + ' a ' + d.hasta + '.');
		var cab = '<tr>';
		for (var c = 0; c < def.cols.length; c++) { cab += '<th' + (def.cols[c][2] ? ' class="num"' : '') + '>' + tpEsc(def.cols[c][1]) + '</th>'; }
		$('#tpCabConsulta').html(cab + '</tr>');
		var html = '';
		for (var i = 0; i < tpFilasConsulta.length; i++) {
			var f = tpFilasConsulta[i];
			html += '<tr>';
			for (c = 0; c < def.cols.length; c++) {
				var col = def.cols[c], v = f[col[0]], cel;
				if (col[2] === 'p') { cel = tpPesos(v); } else if (col[2] === 'n') { cel = tpMiles(v); } else { cel = tpEsc(v); }
				if (col[0] === 'correo' && f.es_empleado === 'S') { cel += ' <span class="etq">EMPLEADO</span>'; }
				html += '<td' + (col[2] ? ' class="num"' : '') + '>' + cel + '</td>';
			}
			html += '</tr>';
		}
		$('#tpCuerpoConsulta').html(html || '<tr><td colspan="' + def.cols.length + '" class="tp-vacio">Sin resultados para este período.</td></tr>');
		$('#tpCsvConsulta').prop('disabled', tpFilasConsulta.length === 0);
	});
}

function tpCsvConsulta() {
	if (tpFilasConsulta.length === 0) { return; }
	var lineas = [], i, c, fila;
	fila = [];
	for (c = 0; c < tpColsConsulta.length; c++) { fila.push(tpColsConsulta[c][1]); }
	lineas.push(fila.join(';'));
	for (i = 0; i < tpFilasConsulta.length; i++) {
		fila = [];
		for (c = 0; c < tpColsConsulta.length; c++) {
			var v = tpFilasConsulta[i][tpColsConsulta[c][0]];
			fila.push(String(v == null ? '' : (typeof v === 'number' ? Math.round(v * 100) / 100 : v)).replace(/[;\r\n]+/g, ' '));
		}
		lineas.push(fila.join(';'));
	}
	var blob = new Blob(['﻿' + lineas.join('\n')], { type: 'text/csv;charset=utf-8;' });
	var a = document.createElement('a');
	a.href = URL.createObjectURL(blob);
	a.download = 'puntos_' + $('#tpConsulta').val().toLowerCase() + '.csv';
	document.body.appendChild(a); a.click(); document.body.removeChild(a);
}

// ---------------------------------------------------------------------------
// Por vencer
// ---------------------------------------------------------------------------

function tpVencer() {
	var dias = $('#tpDiasVencer').val();
	$('#tpCsvVencer').attr('href', server + 'ConsultarTableroPuntos?accion=CSVPORVENCER&dias=' + encodeURIComponent(dias) + '&idtienda=' + encodeURIComponent(tpTienda()));
	tpPedir({ accion: 'PORVENCER', dias: dias, idtienda: tpTienda(), limite: 300 }, function (d) {
		var filas = d.filas || [];
		$('#tpTituloVencer').text('Clientes con puntos que se vencen en los próximos ' + dias + ' días');
		$('#tpRotuloVencer').text(filas.length >= 300 ? 'Se muestran los 300 primeros, ordenados por puntos por vencer. El CSV trae todos.' :
			'Son ' + filas.length + ' clientes con correo válido, ordenados por puntos por vencer.');
		var html = '';
		for (var i = 0; i < filas.length; i++) {
			var f = filas[i];
			html += '<tr><td>' + tpEsc(f.nombre) + '</td><td>' + tpEsc(f.correo) + '</td><td>' + tpEsc(f.celular) + '</td><td class="num">' +
				tpMiles(f.por_vencer) + '</td><td>' + tpEsc(f.proxima) + '</td><td class="num">' + tpMiles(f.saldo) + '</td><td>' +
				(f.es_empleado === 'S' ? '<span class="etq">EMPLEADO</span>' : '') + '</td></tr>';
		}
		$('#tpCuerpoVencer').html(html || '<tr><td colspan="7" class="tp-vacio">Nadie tiene puntos por vencerse en ese plazo.</td></tr>');
	});
}

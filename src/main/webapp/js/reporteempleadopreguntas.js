/**
 * Reporte de las preguntas de UN empleado en un rango de fechas.
 *
 * Todo viene calculado del servidor (ConsultarReporteEmpleadoPreguntas); aqui solo se dibuja. Un porcentaje null
 * quiere decir "no contesto nada", que es distinto de 0 % (haber contestado y fallado todo).
 */
var empleadosPorTexto = {};
var ultimoReporte = null;

$(function () {
	var params = parametrosDeLaUrl();
	var hoy = new Date();
	var hace30 = new Date(); hace30.setDate(hoy.getDate() - 30);
	$('#rpHasta').val(params.hasta || fechaISO(hoy));
	$('#rpDesde').val(params.desde || fechaISO(hace30));

	cargarEmpleados(function () {
		if (params.idempleado) {
			for (var t in empleadosPorTexto) { if (String(empleadosPorTexto[t]) === String(params.idempleado)) { $('#rpEmpleado').val(t); } }
			consultar(parseInt(params.idempleado, 10));
		}
	});

	$('#rpConsultar').on('click', function () { consultar(idElegido()); });
	$('#rpEmpleado').on('keypress', function (e) { if (e.which === 13) { consultar(idElegido()); } });
	$('.rp-atajo').on('click', function (e) {
		e.preventDefault();
		var d = new Date(); var h = new Date(); d.setDate(h.getDate() - parseInt($(this).attr('data-dias'), 10));
		$('#rpDesde').val(fechaISO(d)); $('#rpHasta').val(fechaISO(h));
		if (idElegido() > 0) { consultar(idElegido()); }
	});
	$('#rpImprimir').on('click', function () { window.print(); });
	$('#rpCsv').on('click', descargarCsv);
	$('#rpVerTodas').on('click', function (e) { e.preventDefault(); $('#rpTodas').toggle(); });
});

function parametrosDeLaUrl() {
	var r = {};
	var q = window.location.search.replace(/^\?/, '').split('&');
	for (var i = 0; i < q.length; i++) {
		var kv = q[i].split('=');
		if (kv[0]) { r[kv[0]] = decodeURIComponent((kv[1] || '').replace(/\+/g, ' ')); }
	}
	return r;
}

function fechaISO(d) {
	return d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' + ('0' + d.getDate()).slice(-2);
}

function esc(t) {
	if (t === null || t === undefined) { return ''; }
	return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function clase(p) {
	if (p === null || p === undefined) { return 'nada'; }
	if (p >= 80) { return 'bien'; }
	if (p >= 60) { return 'regular'; }
	return 'mal';
}

function colorBarra(p) {
	return { bien: '#2e7d32', regular: '#ef6c00', mal: '#c62828', nada: '#bbb' }[clase(p)];
}

function pc(p) { return (p === null || p === undefined) ? '-' : p + ' %'; }

function avisar(texto, tipo) {
	var c = $('#rpAviso');
	if (!texto) { c.hide(); return; }
	c.removeClass('alert-danger alert-warning alert-info').addClass('alert-' + (tipo || 'danger')).text(texto).show();
}

// ------------------------------------------------------------------ empleados

function cargarEmpleados(despues) {
	$.getJSON(server + 'ConsultarResumenPreguntasEmpleado', { fechadesde: $('#rpDesde').val(), fechahasta: $('#rpHasta').val() }, function (d) {
		if (d.error) { avisar(d.error); return; }
		var lista = d.empleados || [];
		var dl = $('#rpListaEmpleados').empty();
		empleadosPorTexto = {};
		for (var i = 0; i < lista.length; i++) {
			var t = lista[i].nombrelargo + (lista[i].cargo ? ' - ' + lista[i].cargo : '');
			empleadosPorTexto[t] = lista[i].id;
			dl.append($('<option></option>').attr('value', t));
		}
		if (despues) { despues(); }
	}).fail(function () { avisar('No se pudo cargar la lista de empleados.'); });
}

function idElegido() {
	var t = $.trim($('#rpEmpleado').val());
	if (empleadosPorTexto[t]) { return empleadosPorTexto[t]; }
	// Si escribio solo el nombre, se acepta cuando es unico.
	var hallados = [];
	for (var k in empleadosPorTexto) { if (k.toLowerCase().indexOf(t.toLowerCase()) === 0 && t !== '') { hallados.push(empleadosPorTexto[k]); } }
	return hallados.length === 1 ? hallados[0] : 0;
}

// ------------------------------------------------------------------ consulta

function consultar(id) {
	avisar('');
	if (!id) { avisar('Escoja un empleado de la lista.', 'warning'); return; }
	if (!$('#rpDesde').val() || !$('#rpHasta').val() || $('#rpDesde').val() > $('#rpHasta').val()) {
		avisar('Revise el rango de fechas: la fecha inicial no puede ser posterior a la final.', 'warning');
		return;
	}
	$('#rpCargando').show();
	$('#rpReporte').hide();
	$.ajax({
		url: server + 'ConsultarReporteEmpleadoPreguntas',
		dataType: 'json',
		data: { idempleado: id, fechadesde: $('#rpDesde').val(), fechahasta: $('#rpHasta').val() },
		success: function (d) {
			$('#rpCargando').hide();
			if (d.error) { avisar(d.error); return; }
			ultimoReporte = d;
			pintar(d);
		},
		error: function () { $('#rpCargando').hide(); avisar('No se pudo consultar el reporte.'); }
	});
}

// ------------------------------------------------------------------ dibujo

function pintar(d) {
	var k = d.kpis, c = d.comparativo;
	$('#rpNombre').text(d.empleado.nombrelargo || d.empleado.nombre);
	$('#rpCargo').text(d.empleado.cargo || 'Sin cargo');
	$('#rpRango').text('Del ' + d.desde + ' al ' + d.hasta + (d.empleado.activo ? '' : ' (empleado inactivo)'));

	$('#rpResumen').html(resumen(d));

	$('#rpKpis').html(
		kpi(k.respondidas, 'Respondidas', '') +
		kpi(k.correctas, 'Correctas', 'bien') +
		kpi(k.incorrectas, 'Incorrectas', k.incorrectas > 0 ? 'mal' : '') +
		kpi(pc(k.porcentaje), '% de acierto', clase(k.porcentaje)) +
		kpi(k.distintas, 'Preguntas distintas', '') +
		kpi(k.ultima ? k.ultima.substring(0, 10) : '-', 'Última respuesta', ''));

	$('#rpComparativo').html(
		fila('Este empleado', k.porcentaje, k.respondidas + ' resp.') +
		fila('Su cargo (' + esc(d.empleado.cargo) + ', ' + c.cargoempleados + ' empleado(s))', c.cargoporcentaje, c.cargorespondidas + ' resp.') +
		fila('Toda la empresa', c.empresaporcentaje, c.empresarespondidas + ' resp.'));

	var sem = d.semanas || [], h = '', et = '';
	if (sem.length === 0) { h = '<div class="rp-nota" style="padding:30px 0;">No contestó ninguna pregunta en el rango.</div>'; }
	for (var i = 0; i < sem.length; i++) {
		var p = sem[i].porcentaje;
		h += '<div class="rp-sem"><div class="pc ' + clase(p) + '">' + pc(p) + '</div><div class="barra" style="height:' + Math.max(3, Math.round((p || 0) * 1.2)) + 'px;background:' + colorBarra(p) + ';"></div></div>';
		et += '<div>' + esc(sem[i].inicio.substring(5)) + '<br>' + sem[i].respondidas + ' resp.</div>';
	}
	$('#rpSemanas').html('<div class="rp-semanas">' + h + '</div><div class="rp-etiquetas">' + et + '</div>');

	// Falladas
	var rs = d.respuestas || [], fall = [];
	for (var j = 0; j < rs.length; j++) { if (!rs[j].correcta) { fall.push(rs[j]); } }
	if (fall.length === 0) {
		$('#rpFalladas').html('<div class="rp-ok">' + (rs.length === 0 ? 'No hay respuestas en el rango.' : 'No falló ninguna. ¡Todas correctas!') + '</div>');
	} else {
		var t = '<table class="table table-bordered table-condensed rp-tabla"><thead><tr><th>Fecha</th><th>Pregunta</th><th>Contestó</th><th>Era</th></tr></thead><tbody>';
		for (var f = 0; f < fall.length; f++) {
			t += '<tr><td>' + esc(fall[f].fecha) + '</td><td>' + esc(fall[f].pregunta) + '</td><td class="rp-no">' + esc(fall[f].respuesta) + '</td><td class="rp-ok">' + esc(fall[f].respuestacorrecta) + '</td></tr>';
		}
		$('#rpFalladas').html(t + '</tbody></table>');
	}

	// Por pregunta
	var pp = d.porpregunta || [];
	if (pp.length === 0) { $('#rpPorPregunta').html('<div class="rp-nota">Sin respuestas en el rango.</div>'); }
	else {
		var tp = '<table class="table table-bordered table-condensed rp-tabla"><thead><tr><th>Pregunta</th><th>Respuesta correcta</th><th class="text-right">Veces</th><th class="text-right">Correctas</th><th class="text-right">% de acierto</th></tr></thead><tbody>';
		for (var q = 0; q < pp.length; q++) {
			tp += '<tr><td>' + esc(pp[q].pregunta) + '</td><td>' + esc(pp[q].respuestacorrecta) + '</td><td class="text-right">' + pp[q].veces + '</td><td class="text-right">' + pp[q].correctas + '</td><td class="text-right ' + clase(pp[q].porcentaje) + '"><b>' + pc(pp[q].porcentaje) + '</b></td></tr>';
		}
		$('#rpPorPregunta').html(tp + '</tbody></table>');
	}

	// Pendientes
	var pe = d.pendientes || [];
	if (pe.length === 0) { $('#rpPendientes').html('<div class="rp-ok">Contestó todas las preguntas vigentes que le tocan.</div>'); }
	else {
		var tpe = '<table class="table table-bordered table-condensed rp-tabla"><thead><tr><th>Pregunta</th><th class="text-right">Veces que la ha contestado</th></tr></thead><tbody>';
		for (var n = 0; n < pe.length; n++) {
			tpe += '<tr><td>' + esc(pe[n].pregunta) + '</td><td class="text-right">' + (pe[n].vecesHistorico === 0 ? '<b>Nunca</b>' : pe[n].vecesHistorico) + '</td></tr>';
		}
		$('#rpPendientes').html(tpe + '</tbody></table>');
	}

	// Todas
	if (rs.length === 0) { $('#rpTodas').html('<div class="rp-nota">Sin respuestas en el rango.</div>'); }
	else {
		var ta = '<table class="table table-bordered table-condensed rp-tabla"><thead><tr><th>Fecha</th><th>Pregunta</th><th>Contestó</th><th>Resultado</th><th class="text-right">Tienda</th></tr></thead><tbody>';
		for (var a = 0; a < rs.length; a++) {
			ta += '<tr><td>' + esc(rs[a].fecha) + '</td><td>' + esc(rs[a].pregunta) + '</td><td>' + esc(rs[a].respuesta) + '</td><td class="' + (rs[a].correcta ? 'rp-ok' : 'rp-no') + '">' + (rs[a].correcta ? 'Correcta' : 'Incorrecta') + '</td><td class="text-right">' + (rs[a].idtienda || '') + '</td></tr>';
		}
		$('#rpTodas').html(ta + '</tbody></table>');
	}

	$('#rpReporte').show();
}

function kpi(valor, rotulo, cls) {
	return '<div class="rp-kpi"><div class="v ' + cls + '">' + esc(valor) + '</div><div class="r">' + esc(rotulo) + '</div></div>';
}

function fila(etiqueta, p, extra) {
	var ancho = p === null || p === undefined ? 0 : p;
	return '<div class="rp-comp"><span class="etq">' + etiqueta + '</span><span class="pista"><span style="width:' + ancho + '%;background:' + colorBarra(p) + ';"></span></span>'
		+ '<span class="num ' + clase(p) + '">' + pc(p) + '</span> <span class="rp-nota">' + esc(extra) + '</span></div>';
}

/** Una lectura en palabras: lo que se le diria a quien no quiera mirar las tablas. */
function resumen(d) {
	var k = d.kpis, c = d.comparativo, nombre = esc(d.empleado.nombrelargo || d.empleado.nombre);
	if (k.respondidas === 0) {
		return '<b>' + nombre + '</b> no contestó ninguna pregunta entre el ' + d.desde + ' y el ' + d.hasta + '. '
			+ 'Puede ser que no haya ingresado por el huellero en ese rango, o que no le tocaron preguntas.';
	}
	var s = '<b>' + nombre + '</b> contestó <b>' + k.respondidas + '</b> pregunta(s) entre el ' + d.desde + ' y el ' + d.hasta
		+ ' y acertó <b>' + k.correctas + '</b> (<b class="' + clase(k.porcentaje) + '">' + k.porcentaje + ' %</b>).';
	if (c.cargoporcentaje !== null && c.cargoporcentaje !== undefined) {
		var dif = k.porcentaje - c.cargoporcentaje;
		s += ' Frente a su cargo (' + c.cargoporcentaje + ' %) va ' + (dif === 0 ? 'igual' : (dif > 0 ? dif + ' puntos por encima' : (-dif) + ' puntos por debajo')) + '.';
	}
	var pp = d.porpregunta || [], peores = [];
	for (var i = 0; i < pp.length; i++) { if (pp[i].porcentaje !== null && pp[i].porcentaje < 60) { peores.push(pp[i]); } }
	if (peores.length > 0) { s += ' Le cuesta: «' + esc(peores[0].pregunta) + '»' + (peores.length > 1 ? ' y ' + (peores.length - 1) + ' más' : '') + '.'; }
	if ((d.pendientes || []).length > 0) { s += ' Le faltan por ver ' + d.pendientes.length + ' pregunta(s) vigentes de su cargo.'; }
	return s;
}

// ------------------------------------------------------------------ CSV

function descargarCsv() {
	if (!ultimoReporte) { return; }
	var d = ultimoReporte, k = d.kpis, c = d.comparativo;
	var filas = [];
	filas.push(['Reporte de preguntas por empleado']);
	filas.push(['Empleado', d.empleado.nombrelargo, 'Cargo', d.empleado.cargo]);
	filas.push(['Desde', d.desde, 'Hasta', d.hasta]);
	filas.push([]);
	filas.push(['Respondidas', k.respondidas, 'Correctas', k.correctas, 'Incorrectas', k.incorrectas, '% de acierto', k.porcentaje === null ? '' : k.porcentaje]);
	filas.push(['% su cargo', c.cargoporcentaje === null ? '' : c.cargoporcentaje, '% empresa', c.empresaporcentaje === null ? '' : c.empresaporcentaje]);
	filas.push([]);
	filas.push(['Semana (lunes)', 'Respondidas', 'Correctas', '% de acierto']);
	var sem = d.semanas || [];
	for (var i = 0; i < sem.length; i++) { filas.push([sem[i].inicio, sem[i].respondidas, sem[i].correctas, sem[i].porcentaje]); }
	filas.push([]);
	filas.push(['Fecha', 'Pregunta', 'Contesto', 'Respuesta correcta', 'Resultado', 'Tienda']);
	var rs = d.respuestas || [];
	for (var j = 0; j < rs.length; j++) { filas.push([rs[j].fecha, rs[j].pregunta, rs[j].respuesta, rs[j].respuestacorrecta, rs[j].correcta ? 'Correcta' : 'Incorrecta', rs[j].idtienda || '']); }
	// Punto y coma: asi lo abre Excel configurado en espanol sin pasar por el asistente de importacion.
	var texto = filas.map(function (f) { return f.map(function (v) { return '"' + String(v === null || v === undefined ? '' : v).replace(/"/g, '""') + '"'; }).join(';'); }).join('\r\n');
	var blob = new Blob(['﻿' + texto], { type: 'text/csv;charset=utf-8;' });
	var nombre = 'preguntas_' + (d.empleado.nombre || d.empleado.id) + '_' + d.desde + '_' + d.hasta + '.csv';
	if (window.navigator.msSaveOrOpenBlob) { window.navigator.msSaveOrOpenBlob(blob, nombre); return; }
	var a = document.createElement('a');
	a.href = URL.createObjectURL(blob); a.download = nombre;
	document.body.appendChild(a); a.click(); document.body.removeChild(a);
}

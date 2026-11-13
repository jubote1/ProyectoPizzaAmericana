/**
 * Banco de preguntas a empleados (central).
 *
 * La pantalla lista TODAS las preguntas una sola vez y filtra en el navegador (son pocas decenas): el estado de cada
 * una (vigente, programada, vencida, inactiva) se calcula aqui con la fecha de hoy. Nada de lo que se valida aqui es
 * seguridad: el servidor vuelve a validar todo y vuelve a revisar el rol en cada peticion.
 */
var preguntas = [];
var cargos = [];
var filtroEstado = 'todas';
var MAX_OPCIONES = 6;

$(function () {
	cargar();

	$('.bp-filtro').on('click', function () {
		$('.bp-filtro').removeClass('activo');
		$(this).addClass('activo');
		filtroEstado = $(this).attr('data-estado');
		dibujar();
	});
	$('#fCargo').on('change', dibujar);
	$('#fBuscar').on('input', dibujar);
	$('#btnNueva').on('click', function () { abrirEditor(null, 'nueva'); });
	$('#mpAgregarOpcion').on('click', function () { agregarFilaOpcion('', false); previa(); });
	$('#mpGuardar').on('click', guardar);
	$('#mpDescripcion').on('input', function () { contar(); previa(); });
});

// ------------------------------------------------------------------ carga

function cargar() {
	$.getJSON(server + 'BancoPreguntasEmpleado', { estado: 'todas' }, function (data) {
		if (!responde(data)) { return; }
		preguntas = data.preguntas || [];
		cargos = data.cargos || [];
		var combo = $('#fCargo');
		combo.find('option:gt(0)').remove();
		for (var i = 0; i < cargos.length; i++) {
			combo.append($('<option></option>').val(cargos[i].id).text(cargos[i].nombre));
		}
		dibujar();
	}).fail(function () {
		avisar('No se pudo consultar el banco de preguntas.', 'danger');
	});
}

/** Traduce la respuesta del servidor. true si la pantalla puede seguir. */
function responde(data) {
	if (!data) { avisar('El servidor no respondio.', 'danger'); return false; }
	if (data.respuesta === 'OK') { return true; }
	if (data.respuesta === 'NOSESION') {
		avisar('Se cerro la sesion. Vuelva a entrar.', 'danger');
		setTimeout(function () { location.reload(); }, 1500);
		return false;
	}
	if (data.respuesta === 'SINPERMISO') {
		avisar('Su usuario no tiene el rol "Banco de Preguntas a Empleados". Pida que se lo asignen.', 'danger');
		return false;
	}
	avisar(data.detalle ? data.detalle : 'No se pudo completar la operacion.', 'danger');
	return false;
}

function avisar(texto, tipo) {
	var caja = $('#bpAviso');
	if (!texto) { caja.hide(); return; }
	caja.removeClass('alert-success alert-danger alert-warning').addClass('alert-' + (tipo || 'success')).text(texto).show();
	$('html, body').animate({ scrollTop: 0 }, 200);
}

// ------------------------------------------------------------------ estado y dibujo

function hoyTexto() {
	var d = new Date();
	return d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' + ('0' + d.getDate()).slice(-2);
}

/** vigente | programada | vencida | inactiva (la inactiva gana: apagada es apagada). */
function estadoDe(p) {
	var hoy = hoyTexto();
	if (!p.activa) { return 'inactiva'; }
	if (p.fechafinal < hoy) { return 'vencida'; }
	if (p.fechainicio > hoy) { return 'programada'; }
	return 'vigente';
}

var ROTULO_ESTADO = { vigente: 'Vigente', programada: 'Programada', vencida: 'Vencida', inactiva: 'Inactiva' };

function nombreCargo(id) {
	for (var i = 0; i < cargos.length; i++) { if (cargos[i].id === id) { return cargos[i].nombre; } }
	return 'Cargo ' + id;
}

function dibujar() {
	var cargoFiltro = parseInt($('#fCargo').val(), 10) || 0;
	var texto = $.trim($('#fBuscar').val()).toLowerCase();
	var html = '';
	var mostradas = 0;
	var vigentes = 0, respuestas = 0, aciertos = 0;

	for (var i = 0; i < preguntas.length; i++) {
		var p = preguntas[i];
		var est = estadoDe(p);
		if (est === 'vigente') { vigentes++; }
		respuestas += p.respuestas;
		aciertos += p.aciertos;

		if (filtroEstado === 'vigentes' && est !== 'vigente') { continue; }
		if (filtroEstado === 'inactivas' && est !== 'inactiva') { continue; }
		if (filtroEstado === 'vencidas' && est !== 'vencida') { continue; }
		if (cargoFiltro > 0 && !p.todos && $.inArray(cargoFiltro, p.cargos) < 0) { continue; }
		if (texto !== '' && p.descripcion.toLowerCase().indexOf(texto) < 0) { continue; }
		mostradas++;
		html += tarjeta(p, est, i);
	}

	$('#kVigentes').text(vigentes);
	$('#kTotal').text(preguntas.length);
	$('#kRespuestas').text(respuestas);
	$('#kAcierto').text(respuestas > 0 ? Math.round(aciertos * 100 / respuestas) + ' %' : '-');
	$('#bpTarjetas').html(html);
	$('#bpVacio').toggle(mostradas === 0);

	$('#bpTarjetas .bp-editar').on('click', function () { abrirEditor(preguntas[$(this).attr('data-i')], 'editar'); });
	$('#bpTarjetas .bp-duplicar').on('click', function () { abrirEditor(preguntas[$(this).attr('data-i')], 'duplicar'); });
	$('#bpTarjetas .bp-estado-btn').on('click', function () { cambiarEstado(preguntas[$(this).attr('data-i')]); });
}

function tarjeta(p, est, i) {
	var ops = '';
	for (var k = 0; k < p.opciones.length; k++) {
		var o = p.opciones[k];
		ops += '<div class="bp-opcion' + (o.correcta ? ' ok' : '') + '"><span class="marca">' + (o.correcta ? '&#10003;' : '&middot;') + '</span>'
			+ esc(o.texto) + '</div>';
	}
	var chips = '';
	if (p.todos) {
		chips = '<span class="bp-chip todos">Todos los cargos</span>';
	} else {
		for (var c = 0; c < p.cargos.length; c++) { chips += '<span class="bp-chip">' + esc(nombreCargo(p.cargos[c])) + '</span>'; }
	}
	var acierto = p.respuestas > 0 ? Math.round(p.aciertos * 100 / p.respuestas) : 0;
	var stats = p.respuestas > 0
		? p.respuestas + ' respuestas de ' + p.empleados + ' empleado(s) &middot; ' + acierto + ' % de acierto'
			+ '<div class="bp-barra-acierto"><span style="width:' + acierto + '%"></span></div>'
		: 'Todav&iacute;a no la ha contestado nadie.';
	return '<div class="bp-tarjeta ' + est + '">'
		+ '<span class="bp-estado ' + est + '">' + ROTULO_ESTADO[est] + '</span> '
		+ '<span class="bp-meta" style="margin-left:6px;">Del ' + esc(p.fechainicio) + ' al ' + esc(p.fechafinal) + '</span>'
		+ '<div class="bp-pregunta">' + esc(p.descripcion) + '</div>'
		+ ops
		+ '<div style="margin-top:8px;">' + chips + '</div>'
		+ '<div class="bp-meta">' + stats + '</div>'
		+ '<div class="bp-acciones">'
		+ '<button class="btn btn-default btn-xs bp-editar" data-i="' + i + '">Editar</button>'
		+ '<button class="btn btn-default btn-xs bp-duplicar" data-i="' + i + '">Duplicar</button>'
		+ '<button class="btn btn-' + (p.activa ? 'warning' : 'success') + ' btn-xs bp-estado-btn" data-i="' + i + '">'
		+ (p.activa ? 'Inactivar' : 'Activar') + '</button>'
		+ '</div></div>';
}

// ------------------------------------------------------------------ editor

function abrirEditor(p, modo) {
	$('#mpAviso').hide();
	var titulo = modo === 'editar' ? 'Editar pregunta' : (modo === 'duplicar' ? 'Duplicar pregunta' : 'Pregunta nueva');
	$('#mpTitulo').text(titulo);
	$('#mpId').val(modo === 'editar' && p ? p.id : 0);
	$('#mpDescripcion').val(p ? p.descripcion : '');

	var hoy = hoyTexto();
	var enUnAno = (parseInt(hoy.substring(0, 4), 10) + 1) + hoy.substring(4);
	$('#mpDesde').val(p && modo === 'editar' ? p.fechainicio : hoy);
	$('#mpHasta').val(p && modo === 'editar' ? p.fechafinal : enUnAno);
	$('#mpActiva').prop('checked', p && modo === 'editar' ? p.activa : true);

	$('#mpOpciones').empty();
	if (p) {
		for (var k = 0; k < p.opciones.length; k++) { agregarFilaOpcion(p.opciones[k].texto, p.opciones[k].correcta); }
	} else {
		agregarFilaOpcion('', true);
		agregarFilaOpcion('', false);
		agregarFilaOpcion('', false);
	}
	dibujarCargos(p);
	contar();
	previa();
	$('#modalPregunta').modal('show');
}

function dibujarCargos(p) {
	var html = '<label class="todos' + (p && p.todos ? ' sel' : '') + '"><input type="checkbox" id="mpTodos" style="display:none;"' + (p && p.todos ? ' checked' : '')
		+ '> Todos los cargos</label><br>';
	for (var i = 0; i < cargos.length; i++) {
		var marcado = p && !p.todos && $.inArray(cargos[i].id, p.cargos) >= 0;
		html += '<label class="cargo' + (marcado ? ' sel' : '') + '"><input type="checkbox" class="mpCargo" value="' + cargos[i].id
			+ '" style="display:none;"' + (marcado ? ' checked' : '') + '> ' + esc(cargos[i].nombre) + '</label>';
	}
	$('#mpCargos').html(html);
	$('#mpCargos input').on('change', function () {
		$(this).closest('label').toggleClass('sel', $(this).is(':checked'));
		if ($(this).attr('id') === 'mpTodos' && $(this).is(':checked')) {
			// "Todos" cubre a cualquiera: se sueltan los cargos sueltos para que no parezcan una lista aparte.
			$('#mpCargos .mpCargo').prop('checked', false).closest('label').removeClass('sel');
		} else if ($(this).hasClass('mpCargo') && $(this).is(':checked')) {
			$('#mpTodos').prop('checked', false).closest('label').removeClass('sel');
		}
	});
}

function agregarFilaOpcion(texto, correcta) {
	if ($('#mpOpciones .bp-fila-op').length >= MAX_OPCIONES) { return; }
	var fila = $('<div class="bp-fila-op"></div>');
	fila.append($('<input type="text" class="form-control input-sm op-texto" maxlength="100" placeholder="Texto de la opción">').val(texto));
	var etiqueta = $('<label class="bp-correcta"><input type="checkbox" class="op-correcta"> Correcta</label>');
	etiqueta.find('input').prop('checked', !!correcta);
	fila.append(etiqueta);
	fila.append($('<button type="button" class="btn btn-link btn-xs" title="Quitar">&times;</button>').on('click', function () {
		fila.remove();
		previa();
	}));
	fila.find('input').on('input change', previa);
	$('#mpOpciones').append(fila);
}

function contar() {
	var n = $('#mpDescripcion').val().length;
	$('#mpContador').text(n + ' / 500');
}

/** Lo que vera el empleado: el mismo cuadro del huellero, con las opciones como botones. */
function previa() {
	var html = '<div class="t">¡Desafío de Cultura Empresarial!</div>';
	html += '<div class="q">' + (esc($.trim($('#mpDescripcion').val())) || '<span style="color:#aaa">(la pregunta)</span>') + '</div>';
	$('#mpOpciones .op-texto').each(function () {
		var t = $.trim($(this).val());
		if (t !== '') { html += '<span class="o">' + esc(t) + '</span>'; }
	});
	$('#mpPrevia').html(html);
}

function leerEditor() {
	var opciones = [];
	$('#mpOpciones .bp-fila-op').each(function () {
		opciones.push({ texto: $.trim($(this).find('.op-texto').val()), correcta: $(this).find('.op-correcta').is(':checked') });
	});
	var cs = [];
	$('#mpCargos .mpCargo:checked').each(function () { cs.push(parseInt($(this).val(), 10)); });
	return {
		id: parseInt($('#mpId').val(), 10) || 0,
		descripcion: $.trim($('#mpDescripcion').val()),
		fechainicio: $('#mpDesde').val(),
		fechafinal: $('#mpHasta').val(),
		activa: $('#mpActiva').is(':checked'),
		todos: $('#mpTodos').is(':checked'),
		cargos: cs,
		opciones: opciones
	};
}

function guardar() {
	var datos = leerEditor();
	$('#mpGuardar').prop('disabled', true).text('Guardando...');
	$.post(server + 'BancoPreguntasEmpleado', { accion: 'guardar', datos: JSON.stringify(datos) }, function (resp) {
		$('#mpGuardar').prop('disabled', false).text('Guardar pregunta');
		if (!resp || resp.respuesta !== 'OK') {
			if (resp && (resp.respuesta === 'NOSESION' || resp.respuesta === 'SINPERMISO')) { responde(resp); return; }
			$('#mpAviso').text(resp && resp.detalle ? resp.detalle : 'No se pudo guardar.').show();
			return;
		}
		$('#modalPregunta').modal('hide');
		avisar('La pregunta se guardó.', 'success');
		cargar();
	}, 'json').fail(function () {
		$('#mpGuardar').prop('disabled', false).text('Guardar pregunta');
		$('#mpAviso').text('No se pudo comunicar con el servidor.').show();
	});
}

function cambiarEstado(p) {
	var nueva = !p.activa;
	var msg = nueva ? 'Se vuelve a hacer esta pregunta a los empleados (si esta dentro de sus fechas). Continuar?'
		: 'Esta pregunta deja de hacerse. Lo que ya se respondio se conserva. Continuar?';
	if (!window.confirm(msg)) { return; }
	$.post(server + 'BancoPreguntasEmpleado', { accion: 'estado', id: p.id, activa: nueva }, function (resp) {
		if (!responde(resp)) { return; }
		avisar(resp.detalle, 'success');
		cargar();
	}, 'json').fail(function () { avisar('No se pudo comunicar con el servidor.', 'danger'); });
}

function esc(t) {
	if (t === null || t === undefined) { return ''; }
	return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

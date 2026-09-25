/*
 * Resultado de las preguntas que el huellero del POS le hace a los empleados.
 *
 * Todo en ES5 -var y function, nada de arrow ni let-: la pantalla convive con
 * jQuery 1.11. Sin librerias de tablas ni de graficas: el orden por columna y
 * el buscador son de mano, con cien empleados no hace falta mas.
 */

var peEmpleados = [];
var peOrden = { campo: 'nombrelargo', asc: true };
var peElegido = 0;

$(document).ready(function () {

	//Por defecto el ultimo mes: con un ingreso al dia por empleado, es un rango
	//donde ya hay suficientes respuestas para que el porcentaje diga algo.
	var hoy = new Date();
	var hace30 = new Date();
	hace30.setDate(hoy.getDate() - 30);
	$('#peHasta').val(peFechaISO(hoy));
	$('#peDesde').val(peFechaISO(hace30));

	$('#peConsultar').click(function () {
		peConsultar();
	});
	$('#peBuscar').keyup(function () {
		pePintarTabla();
	});
	$('#peSoloConRespuestas').change(function () {
		pePintarTabla();
	});
	$('#peTablaEmpleados thead th').click(function () {
		var campo = $(this).attr('data-orden');
		if (!campo) {
			return;
		}
		if (peOrden.campo === campo) {
			peOrden.asc = !peOrden.asc;
		} else {
			peOrden.campo = campo;
			peOrden.asc = true;
		}
		pePintarTabla();
	});

	peConsultar();
});

function peFechaISO(fecha) {
	var mes = fecha.getMonth() + 1;
	var dia = fecha.getDate();
	return (fecha.getFullYear() + '-' + (mes < 10 ? '0' : '') + mes + '-' + (dia < 10 ? '0' : '') + dia);
}

function peEscapar(texto) {
	return ($('<div/>').text(texto === null || texto === undefined ? '' : texto).html());
}

function peMensaje(tipo, texto) {
	$('#peMensaje').html('<div class="alert alert-' + tipo + '">' + texto + '</div>');
}

/** Clase de color segun el porcentaje; null no es 0. */
function peClase(porcentaje) {
	if (porcentaje === null || porcentaje === undefined) {
		return ('pe-nada');
	}
	if (porcentaje >= 80) {
		return ('pe-bien');
	}
	if (porcentaje >= 60) {
		return ('pe-regular');
	}
	return ('pe-mal');
}

function pePorcentajeTexto(porcentaje) {
	return (porcentaje === null || porcentaje === undefined ? 'Sin respuestas' : porcentaje + '%');
}

function peRangoValido() {
	var desde = $('#peDesde').val();
	var hasta = $('#peHasta').val();
	if (!desde || !hasta) {
		peMensaje('warning', 'Indique el rango de fechas.');
		return (false);
	}
	if (desde > hasta) {
		peMensaje('warning', 'La fecha inicial es posterior a la final.');
		return (false);
	}
	return (true);
}

// ===========================================================================
// Resumen de todos los empleados activos
// ===========================================================================

function peConsultar() {
	if (!peRangoValido()) {
		return;
	}
	$('#peMensaje').html('');
	$('#peResultado').hide();
	$('#peDetalle').hide();
	peElegido = 0;
	$('#peCargando').show();
	$('#peConsultar').prop('disabled', true);

	$.ajax({
		url: server + 'ConsultarResumenPreguntasEmpleado',
		data: { fechadesde: $('#peDesde').val(), fechahasta: $('#peHasta').val() },
		dataType: 'json',
		type: 'get',
		success: function (data) {
			$('#peCargando').hide();
			$('#peConsultar').prop('disabled', false);
			if (data.error) {
				peMensaje('danger', peEscapar(data.error));
				return;
			}
			peEmpleados = data.empleados || [];
			pePintarKpis();
			pePintarTabla();
			$('#peResultado').show();
		},
		error: function () {
			$('#peCargando').hide();
			$('#peConsultar').prop('disabled', false);
			peMensaje('danger', 'No se pudo consultar. Revise el log del servidor.');
		}
	});
}

function pePintarKpis() {
	var conRespuestas = 0;
	var respondidas = 0;
	var correctas = 0;
	for (var i = 0; i < peEmpleados.length; i++) {
		if (peEmpleados[i].respondidas > 0) {
			conRespuestas++;
		}
		respondidas += peEmpleados[i].respondidas;
		correctas += peEmpleados[i].correctas;
	}
	var pct = respondidas > 0 ? Math.round(correctas * 100 / respondidas) : null;
	var html = '';
	html += '<div class="col-md-3"><div class="pe-kpi"><div class="valor">' + peEmpleados.length +
		'</div><div class="rotulo">Empleados activos</div></div></div>';
	html += '<div class="col-md-3"><div class="pe-kpi"><div class="valor">' + conRespuestas +
		'</div><div class="rotulo">Con respuestas en el rango</div></div></div>';
	html += '<div class="col-md-3"><div class="pe-kpi"><div class="valor">' + respondidas +
		'</div><div class="rotulo">Preguntas respondidas</div></div></div>';
	html += '<div class="col-md-3"><div class="pe-kpi"><div class="valor ' + peClase(pct) + '">' +
		pePorcentajeTexto(pct) + '</div><div class="rotulo">Acierto general</div></div></div>';
	$('#peKpis').html(html);
}

function pePintarTabla() {
	var filtro = ($('#peBuscar').val() || '').toLowerCase();
	var soloConRespuestas = $('#peSoloConRespuestas').is(':checked');
	var lista = [];
	for (var i = 0; i < peEmpleados.length; i++) {
		var e = peEmpleados[i];
		if (soloConRespuestas && e.respondidas === 0) {
			continue;
		}
		if (filtro && (e.nombrelargo + ' ' + e.nombre).toLowerCase().indexOf(filtro) < 0) {
			continue;
		}
		lista.push(e);
	}

	var campo = peOrden.campo;
	var sentido = peOrden.asc ? 1 : -1;
	lista.sort(function (a, b) {
		var x = a[campo];
		var y = b[campo];
		//Sin porcentaje va siempre al final, se ordene como se ordene.
		if (campo === 'porcentaje') {
			if (x === null && y === null) { return (0); }
			if (x === null) { return (1); }
			if (y === null) { return (-1); }
		}
		if (typeof x === 'string') {
			return (x.localeCompare(y) * sentido);
		}
		return ((x - y) * sentido);
	});

	var html = '';
	for (var j = 0; j < lista.length; j++) {
		var f = lista[j];
		html += '<tr class="pe-fila' + (f.id === peElegido ? ' pe-elegida' : '') + '" data-id="' + f.id + '">' +
			'<td>' + peEscapar(f.nombrelargo) + ' <span class="pe-nota">(' + peEscapar(f.nombre) + ')</span></td>' +
			'<td>' + peEscapar(f.cargo) + '</td>' +
			'<td class="text-right">' + f.respondidas + '</td>' +
			'<td class="text-right">' + f.correctas + '</td>' +
			'<td class="text-right"><b class="' + peClase(f.porcentaje) + '">' + pePorcentajeTexto(f.porcentaje) + '</b></td>' +
			'<td>' + peEscapar(f.ultima) + '</td>' +
			'</tr>';
	}
	if (lista.length === 0) {
		html = '<tr><td colspan="6" class="text-center pe-nota">No hay empleados para mostrar con ese filtro.</td></tr>';
	}
	$('#peTablaEmpleados tbody').html(html);
	$('#peTablaEmpleados tbody tr.pe-fila').click(function () {
		peElegirEmpleado(parseInt($(this).attr('data-id'), 10));
	});
}

// ===========================================================================
// Detalle de un empleado
// ===========================================================================

function peElegirEmpleado(id) {
	if (!peRangoValido()) {
		return;
	}
	peElegido = id;
	$('#peTablaEmpleados tbody tr').removeClass('pe-elegida');
	$('#peTablaEmpleados tbody tr[data-id="' + id + '"]').addClass('pe-elegida');

	var nombre = '';
	for (var i = 0; i < peEmpleados.length; i++) {
		if (peEmpleados[i].id === id) {
			nombre = peEmpleados[i].nombrelargo;
		}
	}
	$('#peDetalleTitulo').text('Detalle de ' + nombre + ', del ' + $('#peDesde').val() + ' al ' + $('#peHasta').val());
	$('#peDetalleMensaje').html('<div class="alert alert-info">Consultando...</div>');
	$('#peTablaPorPregunta tbody').html('');
	$('#peTablaRespuestas tbody').html('');
	$('#peDetalle').show();

	$.ajax({
		url: server + 'ConsultarDetallePreguntasEmpleado',
		data: { idempleado: id, fechadesde: $('#peDesde').val(), fechahasta: $('#peHasta').val() },
		dataType: 'json',
		type: 'get',
		success: function (data) {
			if (peElegido !== id) {
				return;
			}
			if (data.error) {
				$('#peDetalleMensaje').html('<div class="alert alert-danger">' + peEscapar(data.error) + '</div>');
				return;
			}
			$('#peDetalleMensaje').html('');
			pePintarDetalle(data);
		},
		error: function () {
			$('#peDetalleMensaje').html('<div class="alert alert-danger">No se pudo consultar el detalle.</div>');
		}
	});
}

function pePintarDetalle(d) {
	var porPregunta = d.porpregunta || [];
	var html = '';
	for (var i = 0; i < porPregunta.length; i++) {
		var p = porPregunta[i];
		html += '<tr><td>' + peEscapar(p.pregunta) + '</td>' +
			'<td>' + peEscapar(p.respuestacorrecta) + '</td>' +
			'<td class="text-right">' + p.veces + '</td>' +
			'<td class="text-right">' + p.correctas + '</td>' +
			'<td class="text-right"><b class="' + peClase(p.porcentaje) + '">' + pePorcentajeTexto(p.porcentaje) + '</b></td></tr>';
	}
	if (porPregunta.length === 0) {
		html = '<tr><td colspan="5" class="text-center pe-nota">Este empleado no respondio preguntas en el rango.</td></tr>';
	}
	$('#peTablaPorPregunta tbody').html(html);

	var respuestas = d.respuestas || [];
	html = '';
	for (var j = 0; j < respuestas.length; j++) {
		var r = respuestas[j];
		html += '<tr><td style="white-space:nowrap;">' + peEscapar(r.fecha) + '</td>' +
			'<td>' + peEscapar(r.pregunta) + '</td>' +
			'<td>' + peEscapar(r.respuesta) + '</td>' +
			'<td>' + (r.correcta ? '<b class="pe-bien">Correcta</b>'
				: '<b class="pe-mal">Incorrecta</b> <span class="pe-nota">(era: ' + peEscapar(r.respuestacorrecta) + ')</span>') + '</td>' +
			'<td class="text-right">' + (r.idtienda > 0 ? r.idtienda : '') + '</td></tr>';
	}
	if (respuestas.length === 0) {
		html = '<tr><td colspan="5" class="text-center pe-nota">Sin respuestas.</td></tr>';
	}
	$('#peTablaRespuestas tbody').html(html);
}

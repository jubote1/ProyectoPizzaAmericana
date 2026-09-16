/*
 * Diferencias de conciliacion de QR y datafono.
 *
 * La pantalla era de SOLO LECTURA: se hacia clic en una fila y se llenaba un
 * formulario deshabilitado. No habia forma de cerrar una diferencia desde el
 * sistema, asi que el estado se cambiaba a mano en la base de datos. Al
 * 2026-09-15 habia 122 diferencias PENDIENTE por $5.852.232 y la mas vieja
 * llevaba 422 dias.
 *
 * Y tenia un defecto que la volvia inservible para lo mas comun: al escoger
 * TODAS en el selector de tienda no devolvia NADA. Mandaba idtienda=TODAS, el
 * servlet no lo podia convertir a numero y lo dejaba en 0, asi que la consulta
 * quedaba "where idtienda = 0" -que no es ninguna tienda- y la pantalla decia
 * "No data available in table" sin explicar nada.
 */

var server;
var table;
var solicitudes = [];
var solicitudSel = null;

$(document).ready(function () {
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	table = $('#grid-solicitudes').DataTable({
		"data": [],
		"columns": [
			{ "data": "idsolicitud" },
			{ "data": "fecha" },
			{ "data": "dias", "render": pintarDias },
			{ "data": "nombretienda" },
			{ "data": "idpedidotienda" },
			{ "data": "categoria", "render": pintarCategoria },
			{ "data": "valor_analizar", "render": pintarValor, "className": "cc-num" },
			{ "data": "estado", "render": pintarEstado }
		],
		"order": [],
		"pageLength": 25,
		"lengthMenu": [[25, 50, 100, -1], [25, 50, 100, "Todas"]],
		"language": {
			"emptyTable": "No hay diferencias con ese filtro",
			"zeroRecords": "Ninguna coincide con la busqueda",
			"info": "_START_ a _END_ de _TOTAL_",
			"infoEmpty": "Sin diferencias",
			"infoFiltered": "(de _MAX_)",
			"lengthMenu": "Ver _MENU_",
			"search": "Buscar:",
			"paginate": { "first": "Primera", "last": "Ultima", "next": "Siguiente", "previous": "Anterior" }
		},
		"createdRow": function (fila, datos) {
			$(fila).attr('data-id', datos.idsolicitud);
		}
	});

	$('#grid-solicitudes tbody').on('click', 'tr', function () {
		var datos = table.row(this).data();
		if (datos) {
			seleccionar(datos.idsolicitud);
		}
	});

	//Si cambia el estado a PROCESADO la observacion pasa a ser obligatoria, y se
	//le dice antes de que toque Guardar y no despues.
	$('#selectEstadoDetalle').on('change', function () {
		refrescarBotonGuardar();
	});
	$('#observacion').on('input', refrescarBotonGuardar);

	getListaTiendas();
	setInterval(validarVigenciaLogueo, 600000);
});

/* ============================ PINTADO ============================ */

function pintarDias(dias) {
	if (dias === null || dias === undefined) {
		return '';
	}
	//Un mes es mucho para una diferencia de plata sin resolver.
	if (dias > 30) {
		return '<span class="cc-dias-alto">' + dias + '</span>';
	}
	return dias;
}

function pintarCategoria(categoria) {
	var c = (categoria || '').toUpperCase();
	if (c === 'FALTANTE') {
		return '<span class="cc-etiqueta cc-cat-falta">FALTANTE</span>';
	}
	if (c === 'SOBRANTE') {
		return '<span class="cc-etiqueta cc-cat-sobra">SOBRANTE</span>';
	}
	//Hay una fila con categoria 'PROCESADO', que no es una categoria. Se muestra
	//tal como esta en vez de esconderla: es un dato sucio que hay que corregir.
	return categoria || '';
}

function pintarEstado(estado) {
	var e = (estado || '').toUpperCase();
	if (e === 'PENDIENTE') {
		return '<span class="cc-etiqueta cc-pend">PENDIENTE</span>';
	}
	if (e === 'PROCESADO') {
		return '<span class="cc-etiqueta cc-proc">PROCESADO</span>';
	}
	//'PROCESASO' existe en una fila: error de digitacion que se deja visible.
	return '<span class="cc-etiqueta cc-pend">' + (estado || '') + '</span>';
}

function pintarValor(valor) {
	return pesos(valor);
}

/** 4466736 -> "$ 4.466.736". Sin decimales: no se manejan centavos. */
function pesos(valor) {
	var n = Math.round(Math.abs(Number(valor) || 0));
	var texto = String(n);
	var conPuntos = '';
	for (var i = 0; i < texto.length; i++) {
		if (i > 0 && (texto.length - i) % 3 === 0) {
			conPuntos += '.';
		}
		conPuntos += texto.charAt(i);
	}
	return '$ ' + conPuntos;
}

/* ============================ CONSULTA ============================ */

function consultarSolicitudes() {
	var fecha = $('#fecha').val();
	var fechaHasta = $('#fechahasta').val();
	var idtienda = $('#selectTiendas').val();
	var estado = $('#selectEstado').val();

	if (!fecha) {
		avisar('Escoja la fecha desde.', false);
		return;
	}
	if (!validarFecha(fecha)) {
		avisar('La fecha desde no es una fecha valida (dd/mm/aaaa).', false);
		return;
	}
	if (fechaHasta && !validarFecha(fechaHasta)) {
		avisar('La fecha hasta no es una fecha valida (dd/mm/aaaa).', false);
		return;
	}
	if (fechaHasta && aComparable(fechaHasta) < aComparable(fecha)) {
		avisar('La fecha hasta no puede ser anterior a la fecha desde.', false);
		return;
	}

	ocultarDetalle();
	$.ajax({
		url: server + 'ConsultarSolicitudConciliacion',
		data: { fecha: fecha, fechahasta: fechaHasta, idtienda: idtienda, estado: estado },
		dataType: 'json',
		success: function (datos) {
			solicitudes = (datos && datos.solicitudes) ? datos.solicitudes : [];
			pintarResumen((datos && datos.resumen) ? datos.resumen : {});
			table.clear();
			table.rows.add(solicitudes).draw();
			$('#ultimaconsulta').html('Consultado a las ' + horaActual() + '. '
					+ solicitudes.length + ' diferencia(s).');
			esconderAviso();
		},
		error: function () {
			//Antes un fallo del servicio se veia igual que "no hay datos", y eso
			//fue lo que escondio durante meses que TODAS no funcionaba.
			avisar('No se pudo consultar. Revise la conexion e intente de nuevo.', false);
		}
	});
}

function pintarResumen(r) {
	$('#res-val-faltante').html(pesos(r.val_faltante || 0));
	$('#res-num-faltante').html(r.pend_faltante || 0);
	$('#res-val-sobrante').html(pesos(r.val_sobrante || 0));
	$('#res-num-sobrante').html(r.pend_sobrante || 0);
	$('#res-dias').html(r.dias_mas_vieja ? r.dias_mas_vieja : '—');
	$('#res-total').html(r.total || 0);
}

/* ============================ DETALLE ============================ */

function seleccionar(idSolicitud) {
	var s = buscar(idSolicitud);
	if (!s) {
		return;
	}
	solicitudSel = s;

	$('#grid-solicitudes tbody tr').removeClass('cc-sel');
	$('#grid-solicitudes tbody tr[data-id="' + idSolicitud + '"]').addClass('cc-sel');

	$('#detalle-titulo').html('Diferencia #' + s.idsolicitud);
	$('#fechatransaccion').val(s.fecha);
	$('#origen').val(s.origen);
	//El nombre de la tienda viene del servicio. Antes se tomaba del selector, asi
	//que con TODAS escogido decia "TODAS" como si fuera el nombre de la tienda.
	$('#tienda').val(s.nombretienda);
	$('#numeropedido').val(s.idpedidotienda);
	$('#categoria').val(s.categoria);
	$('#telefono').val(s.telefono);
	$('#valoranalizar').val(pesos(s.valor_analizar));
	//El valor final arranca con el valor a analizar cuando todavia no se ha
	//puesto: es lo que casi siempre va, y ahorra digitarlo.
	$('#valorfinal').val(Number(s.valor_final) > 0 ? Math.round(s.valor_final) : Math.round(s.valor_analizar));
	$('#descripcion').val(s.descripcion);
	$('#observacion').val(s.observacion_cierre || '');
	$('#selectEstadoDetalle').val((s.estado || '').toUpperCase() === 'PROCESADO' ? 'PROCESADO' : 'PENDIENTE');

	if (s.usuario_procesa) {
		$('#quien-proceso').html('Procesada por <b>' + s.usuario_procesa + '</b> el ' + s.fecha_procesa + '.');
	} else {
		$('#quien-proceso').html('');
	}

	$('#detalle-vacio').hide();
	$('#detalle-cuerpo').show();
	esconderAviso();
	refrescarBotonGuardar();
	prepararPanelPagosTienda(s);
}

function ocultarDetalle() {
	solicitudSel = null;
	$('#detalle-cuerpo').hide();
	$('#detalle-vacio').show();
	$('#detalle-titulo').html('Detalle');
	$('#quien-proceso').html('');
	$('#pagos-tienda-fila').hide();
}

/* ==================== PAGOS EN VIVO DE LA TIENDA ==================== */

/** aaaa-mm-dd (como llega de la base) -> dd/mm/aaaa (como usa el datepicker). */
function aFechaPantalla(fechaSql) {
	if (!fechaSql) {
		return '';
	}
	var p = String(fechaSql).split('-');
	if (p.length !== 3) {
		return fechaSql;
	}
	return p[2] + '/' + p[1] + '/' + p[0];
}

/**
 * Al seleccionar una diferencia, se prepara el panel (rango de fechas
 * alrededor de la transaccion) pero no se consulta solo: es una conexion en
 * vivo a la tienda, y disparar diez consultas mientras se navega la lista de
 * diferencias seria lento e innecesario.
 */
function prepararPanelPagosTienda(s) {
	$('#grid-pagos-tienda tbody').empty();
	$('#pt-aviso').hide();
	var fecha = aFechaPantalla(s.fecha);
	$('#pt-fechadesde').val(fecha);
	$('#pt-fechahasta').val(fecha);
	$('#pagos-tienda-fila').show();
}

function avisarPagosTienda(texto, bien) {
	$('#pt-aviso').html(texto)
		.removeClass('cc-aviso-ok cc-aviso-mal')
		.addClass(bien ? 'cc-aviso-ok' : 'cc-aviso-mal')
		.show();
}

function consultarPagosTienda() {
	if (!solicitudSel) {
		return;
	}
	var fechaDesde = $('#pt-fechadesde').val();
	var fechaHasta = $('#pt-fechahasta').val();
	if (!validarFecha(fechaDesde) || !validarFecha(fechaHasta)) {
		avisarPagosTienda('Revise las fechas (dd/mm/aaaa).', false);
		return;
	}
	if (aComparable(fechaHasta) < aComparable(fechaDesde)) {
		avisarPagosTienda('La fecha hasta no puede ser anterior a la fecha desde.', false);
		return;
	}
	$('#grid-pagos-tienda tbody').empty();
	avisarPagosTienda('Conectando con la tienda, puede tardar unos segundos si el computador esta apagado...', true);
	$.ajax({
		url: server + 'ConsultarPagosTiendaConciliacion',
		data: {
			idtienda: solicitudSel.idtienda,
			origen: solicitudSel.origen,
			fechadesde: fechaDesde,
			fechahasta: fechaHasta
		},
		dataType: 'json',
		success: function (datos) {
			if (datos && datos.error) {
				avisarPagosTienda(datos.error, false);
				return;
			}
			esconderAvisoPagosTienda();
			pintarPagosTienda((datos && datos.pagos) ? datos.pagos : []);
		},
		error: function () {
			avisarPagosTienda('No se pudo consultar. Revise la conexion e intente de nuevo.', false);
		}
	});
}

function esconderAvisoPagosTienda() {
	$('#pt-aviso').hide();
}

function pintarPagosTienda(pagos) {
	var cuerpo = $('#grid-pagos-tienda tbody');
	cuerpo.empty();
	if (pagos.length === 0) {
		avisarPagosTienda('La tienda respondio, pero no tiene pagos de este origen en ese rango de fechas.', false);
		return;
	}
	for (var i = 0; i < pagos.length; i++) {
		var p = pagos[i];
		var fila = $('<tr>').css('cursor', 'default');
		fila.append($('<td>').text(p.idpedidotienda));
		fila.append($('<td>').text(p.fecha));
		fila.append($('<td>').text(p.nombrecliente || ''));
		fila.append($('<td>').text(p.telefono || ''));
		fila.append($('<td class="cc-num">').text(pesos(p.valorformapago)));
		fila.append($('<td class="cc-num">').text(pesos(p.totalneto)));
		fila.append($('<td>').text(p.referenciadatafono || ''));
		fila.append($('<td>').text(p.estacion || ''));
		var etAnulado = p.anulado
			? $('<span class="cc-etiqueta cc-cat-falta">').text('Si')
			: $('<span>').text('No');
		fila.append($('<td>').append(etAnulado));
		cuerpo.append(fila);
	}
}

function refrescarBotonGuardar() {
	var estado = $('#selectEstadoDetalle').val();
	var obs = ($('#observacion').val() || '').trim();
	if (estado === 'PROCESADO' && obs.length < 10) {
		$('#btnGuardar').prop('disabled', true)
			.val('Escriba qué se hizo (mín. 10 letras)');
	} else {
		$('#btnGuardar').prop('disabled', false).val('Guardar');
	}
}

function guardarSolicitud() {
	if (!solicitudSel) {
		return;
	}
	var estado = $('#selectEstadoDetalle').val();
	var valorFinal = ($('#valorfinal').val() || '').trim();
	var observacion = ($('#observacion').val() || '').trim();

	if (!/^[0-9.,]+$/.test(valorFinal)) {
		avisar('El valor final debe ser un numero.', false);
		return;
	}
	if (estado === 'PROCESADO' && observacion.length < 10) {
		avisar('Para marcarla como procesada hay que decir qué se hizo con la diferencia.', false);
		return;
	}

	var pregunta = (estado === 'PROCESADO')
		? 'Se va a marcar la diferencia #' + solicitudSel.idsolicitud + ' como PROCESADA por '
			+ pesos(valorFinal) + '.<br><br>Queda registrada a su nombre. ¿Confirma?'
		: 'Se va a guardar la diferencia #' + solicitudSel.idsolicitud
			+ ' dejándola PENDIENTE.<br><br>¿Confirma?';

	$.confirm({
		'title': 'Confirmar',
		'content': pregunta,
		'type': 'dark',
		'typeAnimated': true,
		'buttons': {
			'Si': {
				'class': 'blue',
				'action': function () { enviarGuardado(estado, valorFinal, observacion); }
			},
			'No': { 'class': 'gray', 'action': function () {} }
		}
	});
}

function enviarGuardado(estado, valorFinal, observacion) {
	$.ajax({
		url: server + 'ActualizarSolicitudConciliacion',
		type: 'post',
		data: {
			idsolicitud: solicitudSel.idsolicitud,
			valorfinal: valorFinal,
			estado: estado,
			observacion: observacion
		},
		dataType: 'json',
		success: function (r) {
			var res = (r && r.respuesta) ? r.respuesta : 'ERROR';
			if (res === 'OK') {
				avisar('Diferencia #' + solicitudSel.idsolicitud + ' guardada.', true);
				//Se vuelve a consultar para que el resumen y la lista queden al dia:
				//si se marco como procesada, sale del filtro de pendientes.
				consultarSolicitudes();
				return;
			}
			if (res === 'SINSESION') {
				avisar('Su sesion se vencio. Vuelva a entrar.', false);
				return;
			}
			if (res === 'FALTAOBSERVACION') {
				avisar('Hay que decir qué se hizo con la diferencia.', false);
				return;
			}
			if (res === 'DATOSMALOS' || res === 'ESTADOMALO') {
				avisar('Los datos enviados no son validos.', false);
				return;
			}
			avisar('No se pudo guardar.', false);
		},
		error: function () {
			avisar('No se pudo guardar. Revise la conexion.', false);
		}
	});
}

function buscar(idSolicitud) {
	for (var i = 0; i < solicitudes.length; i++) {
		if (String(solicitudes[i].idsolicitud) === String(idSolicitud)) {
			return solicitudes[i];
		}
	}
	return null;
}

/* ============================ VARIOS ============================ */

function avisar(texto, bien) {
	$('#cc-aviso').html(texto)
		.removeClass('cc-aviso-ok cc-aviso-mal')
		.addClass(bien ? 'cc-aviso-ok' : 'cc-aviso-mal')
		.show();
}

function esconderAviso() {
	$('#cc-aviso').hide();
}

function horaActual() {
	var d = new Date();
	return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
}

/** dd/mm/aaaa, validando de verdad que el dia exista en ese mes. */
function validarFecha(texto) {
	if (!/^\d{2}\/\d{2}\/\d{4}$/.test(texto)) {
		return false;
	}
	var partes = texto.split('/');
	var dia = parseInt(partes[0], 10);
	var mes = parseInt(partes[1], 10);
	var ano = parseInt(partes[2], 10);
	if (mes < 1 || mes > 12 || dia < 1) {
		return false;
	}
	var d = new Date(ano, mes - 1, dia);
	return (d.getFullYear() === ano && d.getMonth() === mes - 1 && d.getDate() === dia);
}

/** dd/mm/aaaa -> aaaammdd, para poder comparar dos fechas como numeros. */
function aComparable(texto) {
	var p = texto.split('/');
	return parseInt(p[2] + p[1] + p[0], 10);
}

function getListaTiendas() {
	$.getJSON(server + 'GetTiendas', function (datos) {
		//TODAS va de primera y con valor 0, que es lo que el servicio entiende
		//como "todas las tiendas". Antes mandaba el texto TODAS, que no es un
		//numero, y la consulta terminaba buscando la tienda 0.
		var html = '<option value="0" selected>TODAS las tiendas</option>';
		for (var i = 0; i < datos.length; i++) {
			html += '<option value="' + datos[i].id + '">' + datos[i].nombre + '</option>';
		}
		$('#selectTiendas').html(html);
	});
}

function validarVigenciaLogueo() {
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		success: function (data) {
			var r = data[0].respuesta;
			if (r !== 'OK' && r !== 'OKA') {
				location.href = server + 'Index.html';
			}
		}
	});
}

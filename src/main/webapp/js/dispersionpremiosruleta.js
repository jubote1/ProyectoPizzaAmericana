/** dispersionpremiosruleta.js
 *
 * Dispersion de premios de la ruleta de encuestas de servicio.
 *
 * Consulta los premios ganados en un rango de fechas y permite dispersarlos: a
 * cada ganador se le asigna la oferta que corresponde a su premio, se le genera
 * el codigo promocional y se le avisa por correo.
 *
 * Los datos vienen de ConsultarPremiosRuleta, que responde el resumen y el
 * detalle en una sola llamada. Se piden juntos a proposito: si se pidieran por
 * separado, entre las dos llamadas alguien podria dispersar y los contadores de
 * arriba no cuadrarian con la tabla de abajo.
 */

var dtPremios;
var ultimoDetalle = [];

$(function () {
	ponerFechasPorDefecto();

	dtPremios = $('#grid-premios').DataTable({
		"aoColumns": [
			{ "mData": "fecha" },
			{ "mData": "tienda" },
			{ "mData": "idpedido" },
			{ "mData": "premio" },
			{ "mData": "nombre_cliente" },
			{ "mData": "correo", "mRender": pintarCorreo },
			{ "mData": "telefono" },
			{ "mData": "estado", "mRender": pintarEstado },
			{ "mData": "codigo_promocion" },
			{ "mData": "fecha_caducidad" },
			{ "mData": "fecha_aviso" },
			{ "mData": "uso_oferta" }
		],
		"order": [[0, "desc"]],
		"language": {
			"emptyTable": "Sin premios para el rango seleccionado",
			"info": "Mostrando _START_ a _END_ de _TOTAL_ premios",
			"infoEmpty": "Sin premios",
			"lengthMenu": "Ver _MENU_ registros",
			"search": "Buscar:",
			"zeroRecords": "Ningun registro coincide con la busqueda",
			"paginate": { "first": "Primero", "last": "Ultimo", "next": "Siguiente", "previous": "Anterior" }
		}
	});
});

/**
 * Abre el manual de la pantalla en una ventana aparte.
 *
 * Se abre en ventana y no en la misma pestaña para que se pueda leer con la
 * pantalla al lado: la ayuda sirve justo cuando se esta a mitad de una consulta y
 * no conviene perderla. Si el navegador bloquea la ventana emergente, se cae a
 * abrirla en una pestaña nueva.
 */
function abrirAyuda() {
	var ventana = window.open(server + 'AyudaDispersionPremiosRuleta.html',
		'ayudaDispersionPremios',
		'width=980,height=760,scrollbars=yes,resizable=yes');
	if (!ventana) {
		window.open(server + 'AyudaDispersionPremiosRuleta.html', '_blank');
		return;
	}
	ventana.focus();
}

/**
 * Deja por defecto el dia de ayer y hoy. La ruleta se juega despues del pedido,
 * asi que lo normal es revisar lo de las ultimas horas.
 */
function ponerFechasPorDefecto() {
	var hoy = new Date();
	var ayer = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - 1);
	$('#fechadesde').val(aTexto(ayer));
	$('#fechahasta').val(aTexto(hoy));
}

/**
 * Convierte una fecha de JavaScript al formato aaaa-mm-dd que espera el servicio.
 */
function aTexto(fecha) {
	var mes = fecha.getMonth() + 1;
	var dia = fecha.getDate();
	return fecha.getFullYear() + '-' + (mes < 10 ? '0' : '') + mes + '-' + (dia < 10 ? '0' : '') + dia;
}

/**
 * Convierte el estado en una etiqueta de color. Si la oferta ya se redimio se
 * muestra REDIMIDO en lugar de DISPERSADO, porque para quien revisa es mas
 * informativo saber que el cliente ya uso el premio.
 */
function pintarEstado(dato, tipo, fila) {
	if (tipo !== 'display') {
		return dato;
	}
	if (fila.utilizada === 'S') {
		return '<span class="etiqueta-estado est-usado">REDIMIDO</span>';
	}
	var clases = {
		'PENDIENTE': 'est-pendiente',
		'DISPERSADO': 'est-dispersado',
		'FALTA AVISO': 'est-faltaaviso',
		'ENTREGADO A MANO': 'est-amano',
		'SIN CORREO': 'est-sincorreo',
		'SIN OFERTA': 'est-sinoferta'
	};
	var clase = clases[dato] || '';
	return '<span class="etiqueta-estado ' + clase + '">' + dato + '</span>';
}

/**
 * Consulta los premios del rango y pinta los contadores y la tabla.
 */
function consultarPremios() {
	var fechadesde = $('#fechadesde').val();
	var fechahasta = $('#fechahasta').val();

	if (!fechadesde || !fechahasta) {
		alert('Debe seleccionar la fecha desde y la fecha hasta.');
		return;
	}

	$('#btnconsultar').val('...').prop('disabled', true);
	$('#btndispersar').prop('disabled', true);
	$('#btnreenviar').prop('disabled', true);

	$.ajax({
		url: server + 'ConsultarPremiosRuleta',
		dataType: 'json',
		type: 'post',
		data: { 'fechadesde': fechadesde, 'fechahasta': fechahasta },
		success: function (data) {
			if (data.resultado !== 'OK') {
				alert(data.mensaje || 'No se pudo consultar los premios.');
				return;
			}
			pintarTotales(data.resumen);
			pintarTabla(data.detalle);
		},
		error: function () {
			alert('No hubo respuesta del servidor al consultar los premios.');
		},
		complete: function () {
			$('#btnconsultar').val('Consultar').prop('disabled', false);
		}
	});
}

/**
 * Pinta las cifras de arriba y decide si el boton de dispersar se habilita.
 * El boton se habilita solo si hay pendientes: sin eso, alguien podria dispersar
 * dos veces por reflejo y quedarse esperando un resultado que no va a pasar nada.
 */
function pintarTotales(resumen) {
	if (!resumen) { return; }
	$('#totPremios').text(resumen.total);
	$('#totPendientes').text(resumen.pendientes);
	$('#totFaltaAviso').text(resumen.falta_aviso);
	$('#totDispersados').text(resumen.dispersados);
	$('#totSinCorreo').text(resumen.sin_correo);
	$('#totSinOferta').text(resumen.sin_oferta);
	$('#panelTotales').show();

	$('#btndispersar').prop('disabled', resumen.pendientes === 0);
	$('#btndispersar').val(resumen.pendientes > 0
		? 'Dispersar ' + resumen.pendientes + ' premios'
		: 'Dispersar premios');

	// El boton de reenviar se habilita solo si hay premios con codigo y sin aviso.
	// Reintentar es seguro cuantas veces sea -no vuelve a asignar ofertas- pero si
	// no hay nada que reenviar, el boton apagado evita el clic que no hace nada.
	$('#btnreenviar').prop('disabled', resumen.falta_aviso === 0);
	$('#btnreenviar').val(resumen.falta_aviso > 0
		? 'Reenviar ' + resumen.falta_aviso + ' correos'
		: 'Reenviar correos');
}

/**
 * Pinta el detalle en la tabla.
 */
function pintarTabla(detalle) {
	ultimoDetalle = detalle || [];
	dtPremios.clear();
	if (ultimoDetalle.length > 0) {
		dtPremios.rows.add(ultimoDetalle);
		$('#contenedorTabla').show();
		$('#mensajeVacio').hide();
	} else {
		$('#contenedorTabla').hide();
		$('#mensajeVacio').show();
	}
	dtPremios.draw();
}

/**
 * Dispersa los premios pendientes del rango consultado.
 *
 * Se pide confirmacion mostrando la cantidad porque la accion no se puede
 * deshacer: una vez asignada la oferta y enviado el correo, el cliente ya tiene
 * su codigo. Y la cantidad importa: en el uso diario son unos pocos, pero la
 * primera vez puede haber cientos represados.
 */
function dispersarPremios() {
	var fechadesde = $('#fechadesde').val();
	var fechahasta = $('#fechahasta').val();
	var pendientes = parseInt($('#totPendientes').text(), 10) || 0;

	if (pendientes === 0) {
		alert('No hay premios pendientes por dispersar en el rango consultado.');
		return;
	}

	var porTanda = Math.min(pendientes, 30);
	var mensaje = 'Hay ' + pendientes + ' premios pendientes del ' + fechadesde
		+ ' al ' + fechahasta + '.\n\n'
		+ 'Se van a dispersar ' + porTanda + ' en esta tanda'
		+ (pendientes > 30 ? ' (el maximo son 30; los demas quedan para la siguiente)' : '')
		+ ', lo que toma unos ' + Math.ceil(porTanda * 2 / 60) + ' minuto(s).\n\n'
		+ 'A cada ganador se le asigna la oferta, se le genera el codigo y se le '
		+ 'envia el correo. Esta accion no se puede deshacer.\n\n¿Continuar?';

	if (!confirm(mensaje)) {
		return;
	}

	$('#btndispersar').prop('disabled', true);
	$('#btnconsultar').prop('disabled', true);
	$('#barraAvance').show();

	$.ajax({
		url: server + 'DispersarPremiosRuleta',
		dataType: 'json',
		type: 'post',
		data: { 'fechadesde': fechadesde, 'fechahasta': fechahasta },
		success: function (data) {
			if (data.resultado !== 'OK') {
				alert(data.mensaje || 'No se pudo dispersar los premios.');
				return;
			}
			var txt = 'Dispersion terminada.\n\n'
				+ 'Premios dispersados: ' + data.dispersados + '\n'
				+ 'Correos enviados: ' + data.correos_enviados + '\n'
				+ 'Clientes creados: ' + data.clientes_creados + '\n'
				+ 'Con novedad: ' + data.con_error;
			if (data.detalle_errores && data.detalle_errores.length > 0) {
				txt += '\n\nNovedades:\n' + data.detalle_errores.join('\n');
			}
			alert(txt);
		},
		error: function () {
			alert('No hubo respuesta del servidor al dispersar. Vuelva a consultar '
				+ 'para ver que alcanzo a quedar dispersado antes de reintentar.');
		},
		complete: function () {
			$('#barraAvance').hide();
			$('#btnconsultar').prop('disabled', false);
			// Se vuelve a consultar siempre, incluso si hubo error: es la unica
			// forma de saber con certeza que quedo dispersado y que no.
			consultarPremios();
		}
	});
}

/**
 * Reintenta el correo de los premios que quedaron con codigo pero sin avisar al
 * cliente, es decir los que salen como FALTA AVISO.
 *
 * No pide confirmacion, a diferencia de dispersar. Dispersar asigna ofertas y no
 * se puede deshacer; esto solo reenvia un correo que ya debia haber salido, y
 * ademas nunca reenvia a quien si lo recibio, porque el servidor solo mira los
 * que tienen la fecha de aviso vacia.
 */
function reenviarCorreos() {
	var fechadesde = $('#fechadesde').val();
	var fechahasta = $('#fechahasta').val();
	var faltantes = parseInt($('#totFaltaAviso').text(), 10) || 0;

	if (faltantes === 0) {
		alert('No hay correos pendientes por reenviar en el rango consultado.');
		return;
	}

	$('#btnreenviar').prop('disabled', true);
	$('#btndispersar').prop('disabled', true);
	$('#btnconsultar').prop('disabled', true);
	$('#barraAvanceValor').text('Reenviando correos, no cierre esta pagina...');
	$('#barraAvance').show();

	$.ajax({
		url: server + 'DispersarPremiosRuleta',
		dataType: 'json',
		type: 'post',
		data: { 'fechadesde': fechadesde, 'fechahasta': fechahasta, 'accion': 'REENVIAR' },
		success: function (data) {
			if (data.resultado !== 'OK') {
				alert(data.mensaje || 'No se pudo reenviar los correos.');
				return;
			}
			var txt = 'Reenvio terminado.\n\n'
				+ 'Correos enviados: ' + data.correos_enviados + '\n'
				+ 'Con novedad: ' + data.con_error;
			if (data.pendientes_sin_procesar > 0) {
				txt += '\nQuedaron sin procesar: ' + data.pendientes_sin_procesar
					+ ' (vuelva a reenviar para despacharlos)';
			}
			if (data.detalle_errores && data.detalle_errores.length > 0) {
				txt += '\n\nNovedades:\n' + data.detalle_errores.join('\n');
			}
			alert(txt);
		},
		error: function () {
			alert('No hubo respuesta del servidor al reenviar. Vuelva a consultar para '
				+ 'ver cuales alcanzaron a salir.');
		},
		complete: function () {
			$('#barraAvance').hide();
			$('#barraAvanceValor').text('Dispersando premios, no cierre esta pagina...');
			$('#btnconsultar').prop('disabled', false);
			consultarPremios();
		}
	});
}

/**
 * Exporta a CSV lo que se consulto. Se exporta el arreglo completo y no lo que
 * la tabla tiene paginado en pantalla.
 */
function exportarPremios() {
	if (!ultimoDetalle || ultimoDetalle.length === 0) {
		alert('No hay datos para exportar.');
		return;
	}

	var columnas = ['fecha', 'tienda', 'idpedido', 'premio', 'nombre_cliente', 'correo',
		'telefono', 'estado', 'codigo_promocion', 'fecha_caducidad', 'fecha_aviso',
		'uso_oferta', 'usuario_dispersion', 'nombre_oferta'];

	var csv = columnas.join(';') + '\n';
	for (var i = 0; i < ultimoDetalle.length; i++) {
		var fila = [];
		for (var j = 0; j < columnas.length; j++) {
			fila.push(limpiarCampo(ultimoDetalle[i][columnas[j]]));
		}
		csv += fila.join(';') + '\n';
	}

	// El BOM es necesario para que Excel abra el archivo con las tildes correctas.
	var blob = new Blob(["﻿" + csv], { type: 'text/csv;charset=utf-8;' });
	var enlace = document.createElement('a');
	enlace.href = URL.createObjectURL(blob);
	enlace.download = 'premios_ruleta_' + $('#fechadesde').val() + '_' + $('#fechahasta').val() + '.csv';
	document.body.appendChild(enlace);
	enlace.click();
	document.body.removeChild(enlace);
}

/**
 * Quita del campo lo que rompe un CSV separado por punto y coma.
 */
function limpiarCampo(valor) {
	if (valor === null || valor === undefined) {
		return '';
	}
	return String(valor).replace(/;/g, ',').replace(/[\r\n]+/g, ' ');
}

/**
 * La celda del correo, con un lapiz para corregirlo.
 *
 * Corregir el correo en la pantalla de Clientes NO cambia nada aqui: la consulta se
 * queda con el correo que el cliente tecleo al jugar apenas tenga una arroba, aunque
 * este mal escrito. Por eso la correccion tiene que poder hacerse en esta pantalla.
 *
 * Solo se ofrece mientras el premio no se haya avisado: una vez salio el correo,
 * cambiar la direccion aqui ya no le cambia nada al cliente y solo confunde.
 */
function pintarCorreo(dato, tipo, fila) {
	if (tipo !== 'display') {
		return dato;
	}
	var texto = dato ? dato : '<span style="color:#E42528">sin correo</span>';
	if (fila.fecha_aviso) {
		return texto;
	}
	return texto + ' <a href="#" class="corregir-correo" data-id="' + fila.idresultado
		+ '" data-correo="' + (dato || '') + '" title="Corregir el correo de este premio"'
		+ ' style="text-decoration:none">&#9998;</a>';
}

/**
 * Pide el correo nuevo y lo guarda.
 *
 * Despues de guardar se vuelve a consultar: asi la fila queda con lo que de verdad
 * quedo en la base, y no con lo que se escribio en la casilla.
 */
function corregirCorreoPremio(idresultado, correoActual) {
	var nuevo = prompt('Correo al que se le debe avisar este premio:', correoActual || '');
	if (nuevo === null) {
		return;
	}
	nuevo = nuevo.replace(/^\s+|\s+$/g, '');
	if (nuevo === '' || nuevo === correoActual) {
		return;
	}
	$.ajax({
		url: server + 'CorregirCorreoPremioRuleta',
		dataType: 'json',
		type: 'post',
		data: { idresultado: idresultado, correo: nuevo },
		success: function (data) {
			alert(data && data.mensaje ? data.mensaje : 'No se pudo guardar el correo.');
			if (data && data.resultado === 'OK') {
				consultarPremios();
			}
		},
		error: function () {
			alert('No se pudo guardar el correo. Intente de nuevo.');
		}
	});
}

$(document).on('click', '.corregir-correo', function (e) {
	e.preventDefault();
	corregirCorreoPremio($(this).attr('data-id'), $(this).attr('data-correo'));
});

/** consultaencuestamercadeo.js
 *
 * Consulta de las respuestas de encuestas de mercadeo tomadas en el POS.
 * Muestra dos vistas de lo mismo: un resumen agrupado por pregunta con barras y
 * porcentajes, y el detalle fila por fila.
 *
 * Los datos vienen del servicio ConsultarEncuestaMercadeo, que responde el resumen
 * y el detalle en una sola llamada para no pegarle dos veces al servidor.
 */

var dtDetalle;
var ultimoDetalle = [];

$(function () {
	cargarTiendas();
	cargarPreguntas();
	ponerFechasPorDefecto();

	dtDetalle = $('#grid-detalle').DataTable({
		"aoColumns": [
			{ "mData": "fecha" },
			{ "mData": "tienda" },
			{ "mData": "idpedidotienda" },
			{ "mData": "idcliente" },
			{ "mData": "pregunta" },
			{ "mData": "respuesta" }
		],
		"order": [[0, "desc"]],
		"language": {
			"emptyTable": "Sin respuestas para los filtros seleccionados",
			"info": "Mostrando _START_ a _END_ de _TOTAL_ respuestas",
			"infoEmpty": "Sin respuestas",
			"lengthMenu": "Ver _MENU_ registros",
			"search": "Buscar:",
			"zeroRecords": "Ningun registro coincide con la busqueda",
			"paginate": { "first": "Primero", "last": "Ultimo", "next": "Siguiente", "previous": "Anterior" }
		}
	});
});

/**
 * Deja por defecto el mes corrido: del primer dia del mes actual hasta hoy.
 */
function ponerFechasPorDefecto() {
	var hoy = new Date();
	var primero = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
	$('#fechainicial').val(aTexto(primero));
	$('#fechafinal').val(aTexto(hoy));
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
 * Llena el combo de tiendas. La opcion TODAS va de primera y con valor 0, que es
 * como el servicio interpreta "sin filtro de tienda".
 */
function cargarTiendas() {
	var str = '<option value="0">TODAS LAS TIENDAS</option>';
	$.getJSON(server + 'GetTiendasFuncionales', function (data) {
		for (var i = 0; i < data.length; i++) {
			str += '<option value="' + data[i].id + '">' + data[i].nombre + '</option>';
		}
		$('#selectTiendas').html(str);
	}).fail(function () {
		$('#selectTiendas').html(str);
	});
}

/**
 * Llena el combo de preguntas con todas las configuradas, vigentes o no, porque el
 * reporte suele mirar periodos pasados donde la pregunta ya vencio.
 */
function cargarPreguntas() {
	var str = '<option value="0">TODAS LAS PREGUNTAS</option>';
	$.getJSON(server + 'CRUDPreguntaMercadeo?accion=LISTAR', function (data) {
		if (data && data.preguntas) {
			for (var i = 0; i < data.preguntas.length; i++) {
				var p = data.preguntas[i];
				str += '<option value="' + p.idpregunta + '">' + p.titulo + ' - ' + p.descripcion + '</option>';
			}
		}
		$('#selectPreguntas').html(str);
	}).fail(function () {
		$('#selectPreguntas').html(str);
	});
}

/**
 * Consulta el reporte con los filtros seleccionados y pinta las dos vistas.
 */
function consultarEncuestas() {
	var idtienda = $('#selectTiendas').val();
	var idpregunta = $('#selectPreguntas').val();
	var fechainicial = $('#fechainicial').val();
	var fechafinal = $('#fechafinal').val();

	if (!fechainicial || !fechafinal) {
		alert('Debe seleccionar la fecha inicial y la fecha final.');
		return;
	}

	$('#btnconsultar').val('...').prop('disabled', true);

	$.ajax({
		url: server + 'ConsultarEncuestaMercadeo',
		dataType: 'json',
		type: 'post',
		data: {
			'idtienda': idtienda,
			'idpregunta': idpregunta,
			'fechainicial': fechainicial,
			'fechafinal': fechafinal
		},
		success: function (data) {
			if (data.resultado !== 'OK') {
				alert(data.mensaje || 'No se pudo consultar el reporte.');
				return;
			}
			pintarTotales(data.totales);
			pintarResumen(data.resumen);
			pintarDetalle(data.detalle);
		},
		error: function () {
			alert('No hubo respuesta del servidor al consultar el reporte.');
		},
		complete: function () {
			$('#btnconsultar').val('Consultar').prop('disabled', false);
		}
	});
}

/**
 * Pinta las dos cifras de arriba.
 */
function pintarTotales(totales) {
	if (!totales) { return; }
	$('#totalEncuestas').text(totales.encuestas);
	$('#totalRespuestas').text(totales.respuestas);
	$('#panelTotales').show();
}

/**
 * Pinta el resumen: un bloque por pregunta, con una barra por cada respuesta y su
 * porcentaje dentro de esa pregunta.
 */
function pintarResumen(resumen) {
	var cont = $('#contenedorResumen');
	cont.empty();

	if (!resumen || resumen.length === 0) {
		cont.html('<p class="sin-datos">No hay respuestas para los filtros seleccionados.</p>');
		return;
	}

	var idActual = null;
	var html = '';
	for (var i = 0; i < resumen.length; i++) {
		var f = resumen[i];
		if (f.idpregunta !== idActual) {
			if (idActual !== null) { html += '</tbody></table></div>'; }
			idActual = f.idpregunta;
			html += '<div class="bloque-pregunta">';
			html += '<h4>' + escapar(f.pregunta) + '</h4>';
			html += '<div class="subtitulo">' + escapar(f.titulo) + ' &middot; ' + nombreTipo(f.tipo) +
					' &middot; ' + f.totalpregunta + ' respuestas</div>';
			html += '<table class="table table-condensed"><tbody>';
		}
		html += '<tr>';
		html += '<td class="col-etiqueta">' + escapar(f.etiqueta) + '</td>';
		html += '<td class="col-barra"><div class="barra-fondo"><div class="barra-valor" style="width:' +
				f.porcentaje + '%"></div></div></td>';
		html += '<td class="col-cifra"><strong>' + f.cantidad + '</strong> <span class="text-muted">(' +
				f.porcentaje + '%)</span></td>';
		html += '</tr>';
	}
	if (idActual !== null) { html += '</tbody></table></div>'; }

	// Las preguntas abiertas se agrupan bajo una sola etiqueta, porque agrupar texto
	// libre daria un grupo por respuesta. El aviso le dice al usuario donde leerlas.
	if (html.indexOf('(respuestas abiertas)') >= 0) {
		html += '<div class="alert alert-info">Las preguntas abiertas se cuentan pero no se agrupan, ' +
				'porque cada respuesta es distinta. Para leerlas, use la pestana Detalle.</div>';
	}
	cont.html(html);
}

/**
 * Carga el detalle en la tabla.
 */
function pintarDetalle(detalle) {
	ultimoDetalle = detalle || [];
	dtDetalle.clear();
	if (ultimoDetalle.length > 0) {
		dtDetalle.rows.add(ultimoDetalle);
	}
	dtDetalle.draw();
}

/**
 * Baja el detalle consultado como archivo CSV, para que mercadeo lo lleve a Excel.
 * Se usa punto y coma como separador, que es lo que espera Excel en espanol.
 */
function exportarDetalle() {
	if (!ultimoDetalle || ultimoDetalle.length === 0) {
		alert('Primero debe consultar. No hay detalle para exportar.');
		return;
	}
	var lineas = ['Fecha;Tienda;Pedido;Cliente;Usuario;Pregunta;Respuesta'];
	for (var i = 0; i < ultimoDetalle.length; i++) {
		var f = ultimoDetalle[i];
		lineas.push([
			celda(f.fecha), celda(f.tienda), f.idpedidotienda, f.idcliente, f.idusuario,
			celda(f.pregunta), celda(f.respuesta)
		].join(';'));
	}
	// El BOM hace que Excel reconozca el UTF-8 y no dane las tildes
	var contenido = '﻿' + lineas.join('\r\n');
	var enlace = document.createElement('a');
	enlace.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(contenido);
	enlace.download = 'encuestas_mercadeo_' + $('#fechainicial').val() + '_a_' + $('#fechafinal').val() + '.csv';
	document.body.appendChild(enlace);
	enlace.click();
	document.body.removeChild(enlace);
}

/**
 * Limpia un valor para que no rompa el CSV: quita los punto y coma y los saltos.
 */
function celda(valor) {
	if (valor === null || valor === undefined) { return ''; }
	return String(valor).replace(/;/g, ',').replace(/(\r\n|\n|\r)/gm, ' ');
}

/**
 * Evita que un texto de pregunta o respuesta con caracteres de HTML dane la pagina.
 */
function escapar(texto) {
	if (texto === null || texto === undefined) { return ''; }
	return String(texto)
		.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/**
 * Traduce el codigo de tipo de pregunta a algo legible.
 */
function nombreTipo(tipo) {
	switch (tipo) {
		case 'B': return 'Si / No';
		case 'O': return 'Opciones';
		case 'A': return 'Abierta';
		default: return tipo;
	}
}

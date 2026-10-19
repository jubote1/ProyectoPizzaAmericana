/*
 * Pantalla de campanas de promocion.
 *
 * Las fechas se muestran en dd/mm/aaaa -el datepicker en espanol- y viajan al
 * servidor en aaaa-mm-dd, que es lo que espera la columna date. La conversion
 * se hace aqui y no alla para no tener dos formatos dando vueltas por el
 * servlet.
 */

var server;
var tiendasDisponibles = [];
var excepcionesDisponibles = [];
var productosDeLaCampana = [];

var NOMBRES_DIA = ['', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado', 'Domingo'];

$(document).ready(function() {
	validarVigenciaLogueo();
	pintarDias();
	cargarTiendas();
	cargarExcepciones();
	cargarCampanas();
});

function validarVigenciaLogueo()
{
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(data){
			if(data[0].respuesta != 'OK' && data[0].respuesta != 'OKA'){
				location.href = server + "Index.html";
			}
		}
	});
}

function escapar(texto)
{
	return $('<div>').text(texto === null || texto === undefined ? '' : texto).html();
}

/* aaaa-mm-dd -> dd/mm/aaaa. Vacio si no viene nada. */
function aPantalla(fechaIso)
{
	if (!fechaIso || fechaIso.length < 10) { return ''; }
	return fechaIso.substring(8, 10) + '/' + fechaIso.substring(5, 7) + '/' + fechaIso.substring(0, 4);
}

/* dd/mm/aaaa -> aaaa-mm-dd. Vacio si no viene nada. */
function aServidor(fechaTexto)
{
	if (!fechaTexto || fechaTexto.length < 10) { return ''; }
	var p = fechaTexto.split('/');
	if (p.length != 3) { return ''; }
	return p[2] + '-' + p[1] + '-' + p[0];
}

function pintarDias()
{
	var html = '';
	for (var d = 1; d <= 7; d++) {
		html += '<label class="cp-dia"><input type="checkbox" class="chk-dia" value="' + d + '"> '
			+ NOMBRES_DIA[d] + '</label>';
	}
	$('#lista-dias').html(html);
}

function cargarTiendas()
{
	$.getJSON(server + 'GetTiendas', function(data){
		tiendasDisponibles = data;
		var html = '';
		for (var i = 0; i < data.length; i++) {
			html += '<label class="cp-dia"><input type="checkbox" class="chk-tienda" value="'
				+ data[i].id + '"> ' + escapar(data[i].nombre) + '</label>';
		}
		$('#lista-tiendas').html(html);
	});
}

function cargarExcepciones()
{
	$.getJSON(server + 'CRUDCampanaPromo?idoperacion=8', function(data){
		excepcionesDisponibles = data;
		var html = '<option value="0">Sin excepcion</option>';
		for (var i = 0; i < data.length; i++) {
			html += '<option value="' + data[i].idexcepcion + '" data-producto="' + data[i].idproducto + '">'
				+ escapar(data[i].descripcion) + ' - ' + escapar(data[i].producto)
				+ ' ($' + Math.round(data[i].precio) + ')</option>';
		}
		$('#p-excepcion').html(html);
	});
}

function cargarCampanas()
{
	$.getJSON(server + 'CRUDCampanaPromo?idoperacion=4', function(data){
		if (!data || data.length === 0) {
			$('#tabla-campanas').html('<p class="cp-pie">Todavia no hay campanas.</p>');
			return;
		}
		var html = '<table class="table"><thead><tr><th>Campana</th><th>Estado</th><th>Desde</th>'
			+ '<th>Hasta</th><th>Exige correo</th></tr></thead><tbody>';
		for (var i = 0; i < data.length; i++) {
			var c = data[i];
			html += '<tr onclick="abrirCampana(' + c.idcampana + ')">'
				+ '<td>' + escapar(c.nombre) + '</td>'
				+ '<td><span class="cp-chip ' + (c.activo === 'S' ? 'cp-si">Activa' : 'cp-no">Apagada') + '</span></td>'
				+ '<td>' + aPantalla(c.fechadesde) + '</td>'
				+ '<td>' + aPantalla(c.fechahasta) + '</td>'
				+ '<td>' + (c.exigecorreo === 'S' ? 'Si' : 'No') + '</td>'
				+ '</tr>';
		}
		$('#tabla-campanas').html(html + '</tbody></table>');
	});
}

function nuevaCampana()
{
	$('#idcampana').val(0);
	$('#titulo-form').text('Nueva campana');
	$('#nombre').val('');
	$('#fechadesde').val('');
	$('#fechahasta').val('');
	$('#pregunta').val('');
	$('#idoferta').val(0);
	$('#activo').prop('checked', false);
	$('#exigecorreo').prop('checked', true);
	$('.chk-tienda').prop('checked', false);
	$('.chk-dia').prop('checked', false);
	productosDeLaCampana = [];
	pintarProductos();
	$('#btn-eliminar').hide();
	$('#caja-marcados').hide();
	$('#caja-form').show();
}

function abrirCampana(idCampana)
{
	$.getJSON(server + 'CRUDCampanaPromo?idoperacion=6&idcampana=' + idCampana, function(c){
		if (c.respuesta === 'NOK') {
			bootbox.alert(c.detalle);
			return;
		}
		$('#idcampana').val(c.idcampana);
		$('#titulo-form').text('Campana: ' + c.nombre);
		$('#nombre').val(c.nombre);
		$('#fechadesde').val(aPantalla(c.fechadesde));
		$('#fechahasta').val(aPantalla(c.fechahasta));
		$('#pregunta').val(c.pregunta);
		$('#idoferta').val(c.idoferta);
		$('#activo').prop('checked', c.activo === 'S');
		$('#exigecorreo').prop('checked', c.exigecorreo === 'S');

		$('.chk-tienda').prop('checked', false);
		for (var i = 0; i < c.tiendas.length; i++) {
			$('.chk-tienda[value="' + c.tiendas[i] + '"]').prop('checked', true);
		}
		$('.chk-dia').prop('checked', false);
		for (var j = 0; j < c.dias.length; j++) {
			$('.chk-dia[value="' + c.dias[j] + '"]').prop('checked', true);
		}

		productosDeLaCampana = [];
		for (var k = 0; k < c.productos.length; k++) {
			productosDeLaCampana.push({
				ambito: c.productos[k].ambito,
				idproducto: c.productos[k].idproducto,
				idexcepcion: c.productos[k].idexcepcion,
				nombre: c.productos[k].nombre
			});
		}
		pintarProductos();
		pintarMarcados(c.marcados);
		$('#btn-eliminar').show();
		$('#caja-form').show();
		$('html, body').animate({ scrollTop: $('#caja-form').offset().top - 20 }, 200);
	});
}

function pintarMarcados(marcados)
{
	if (!marcados || marcados.length === 0) {
		$('#caja-marcados').hide();
		return;
	}
	var html = '<table class="table"><thead><tr><th>Respuesta</th><th>Estado</th><th>Pedidos</th>'
		+ '</tr></thead><tbody>';
	for (var i = 0; i < marcados.length; i++) {
		html += '<tr><td>' + (marcados[i].solicito === 'S' ? 'Si la quiso' : 'No la quiso') + '</td><td>'
			+ escapar(marcados[i].estado) + '</td><td>' + marcados[i].cuantos + '</td></tr>';
	}
	$('#tabla-marcados').html(html + '</tbody></table>');
	$('#caja-marcados').show();
}

function cambioAmbito()
{
	if ($('#p-ambito').val() === 'CONTACTCENTER') {
		$('#col-excepcion').show();
	} else {
		$('#col-excepcion').hide();
		$('#p-excepcion').val(0);
	}
}

function agregarProducto()
{
	var ambito = $('#p-ambito').val();
	var idExcepcion = 0;
	var idProducto = parseInt($('#p-producto').val(), 10);

	if (ambito === 'CONTACTCENTER') {
		idExcepcion = parseInt($('#p-excepcion').val(), 10) || 0;
		// La excepcion ya sabe sobre que producto va; se toma de ahi para que no
		// se pueda guardar una pareja que no existe.
		if (idExcepcion > 0) {
			idProducto = parseInt($('#p-excepcion option:selected').attr('data-producto'), 10);
		}
	}
	if (!idProducto || idProducto <= 0) {
		bootbox.alert('Escriba el idproducto, o escoja una excepcion.');
		return;
	}
	for (var i = 0; i < productosDeLaCampana.length; i++) {
		var p = productosDeLaCampana[i];
		if (p.ambito === ambito && p.idproducto === idProducto && p.idexcepcion === idExcepcion) {
			bootbox.alert('Ese ya esta en la lista.');
			return;
		}
	}
	// Se consulta el nombre antes de agregarlo: el usuario tiene que ver QUE
	// acaba de escribir, no un numero suelto. Un id equivocado aqui es una
	// promocion que nunca dispara y nadie sabe por que.
	var tiendas = $('.chk-tienda:checked').map(function(){ return this.value; }).get().join(',');
	$.getJSON(server + 'CRUDCampanaPromo?idoperacion=7&ambito=' + ambito + '&idproducto=' + idProducto
			+ '&idexcepcion=' + idExcepcion + '&tiendas=' + tiendas, function(r){
		productosDeLaCampana.push({
			ambito: ambito, idproducto: idProducto, idexcepcion: idExcepcion, nombre: r.nombre
		});
		pintarProductos();
		$('#p-producto').val('');
	});
}

function quitarProducto(indice)
{
	productosDeLaCampana.splice(indice, 1);
	pintarProductos();
}

function pintarProductos()
{
	if (productosDeLaCampana.length === 0) {
		$('#tabla-productos').html('<p class="cp-pie">Sin productos. Una campana activa necesita al menos uno.</p>');
		return;
	}
	var html = '<table class="table"><thead><tr><th>Catalogo</th><th>Producto</th><th>Excepcion</th>'
		+ '<th>Que es</th><th></th></tr></thead><tbody>';
	for (var i = 0; i < productosDeLaCampana.length; i++) {
		var p = productosDeLaCampana[i];
		var malo = p.nombre && (p.nombre.indexOf('(no ') === 0 || p.nombre.indexOf('(la ') === 0);
		html += '<tr><td>' + (p.ambito === 'TIENDA' ? 'Tienda' : 'Central') + '</td>'
			+ '<td>' + p.idproducto + '</td>'
			+ '<td>' + (p.idexcepcion > 0 ? p.idexcepcion : '-') + '</td>'
			+ '<td class="' + (malo ? 'cp-nombre-malo' : 'cp-nombre-resuelto') + '">' + escapar(p.nombre) + '</td>'
			+ '<td><button class="btn btn-xs btn-default" onclick="quitarProducto(' + i + ')">quitar</button></td>'
			+ '</tr>';
	}
	$('#tabla-productos').html(html + '</tbody></table>');
}

function guardarCampana()
{
	var tiendas = $('.chk-tienda:checked').map(function(){ return this.value; }).get().join(',');
	var dias = $('.chk-dia:checked').map(function(){ return this.value; }).get().join(',');
	var productos = productosDeLaCampana.map(function(p){
		return p.ambito + ':' + p.idproducto + ':' + p.idexcepcion;
	}).join(',');

	$.getJSON(server + 'CRUDCampanaPromo', {
		idoperacion: 1,
		idcampana: $('#idcampana').val(),
		nombre: $('#nombre').val(),
		activo: $('#activo').is(':checked') ? 'S' : 'N',
		fechadesde: aServidor($('#fechadesde').val()),
		fechahasta: aServidor($('#fechahasta').val()),
		pregunta: $('#pregunta').val(),
		idoferta: $('#idoferta').val(),
		exigecorreo: $('#exigecorreo').is(':checked') ? 'S' : 'N',
		tiendas: tiendas,
		dias: dias,
		productos: productos
	}, function(r){
		if (r.respuesta === 'OK') {
			bootbox.alert('Campana guardada.');
			cerrarForm();
			cargarCampanas();
		} else {
			bootbox.alert(r.detalle || 'No se pudo guardar.');
		}
	});
}

function eliminarCampana()
{
	var idCampana = $('#idcampana').val();
	if (!idCampana || idCampana == 0) { return; }
	bootbox.confirm('Se borra la campana y su configuracion. Lo que ya quedo marcado NO se borra. Seguro?',
		function(ok){
			if (!ok) { return; }
			$.getJSON(server + 'CRUDCampanaPromo?idoperacion=2&idcampana=' + idCampana, function(r){
				if (r.respuesta === 'OK') {
					cerrarForm();
					cargarCampanas();
				} else {
					bootbox.alert(r.detalle || 'No se pudo eliminar.');
				}
			});
		});
}

function cerrarForm()
{
	$('#caja-form').hide();
}

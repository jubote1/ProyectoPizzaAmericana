var server;

$(document).ready(function() {

	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	cargarTiendas();
	setInterval('validarVigenciaLogueo()',600000);
});

function validarVigenciaLogueo()
{
	var respuesta ='';
	$.ajax({
	   	url: server + 'ValidarUsuarioAplicacion',
	   	dataType: 'json',
	   	type: 'post',
	   	async: false,
	   	success: function(data){
			    respuesta =  data[0].respuesta;
		}
	});
	switch(respuesta)
	{
		case 'OK':
				break;
		case 'OKA':
				break;
		default:
				location.href = server +"Index.html";
		    	break;
	}
}

function cargarTiendas()
{
	$.getJSON(server + 'GetTiendas', function(data){
		var combo = $('#idtienda');
		for(var i = 0; i < data.length; i++){
			combo.append($('<option>', { value: data[i].id, text: data[i].nombre }));
		}
	});
}

function consultar()
{
	var fechaInicial = $('#fechainicial').val();
	var fechaFinal = $('#fechafinal').val();
	var idTienda = $('#idtienda').val();
	if(fechaInicial == '' || fechaFinal == '')
	{
		bootbox.alert('Seleccione la fecha inicial y la fecha final.');
		return;
	}
	$.getJSON(server + 'ConsultarVentaIntegral?fechainicial=' + fechaInicial + '&fechafinal=' + fechaFinal
			+ '&idtienda=' + idTienda, function(data){
		pintarResumen(data);
		pintarTarjetas(data, fechaInicial, fechaFinal);
		pintarAlerta(data.alertatiendassindatos);
	});
}

function calcularSemanas(fechaInicial, fechaFinal)
{
	// Las fechas llegan en dd/mm/yyyy (formato del datepicker en espanol).
	var partesIni = fechaInicial.split('/');
	var partesFin = fechaFinal.split('/');
	var ini = new Date(partesIni[2], partesIni[1] - 1, partesIni[0]);
	var fin = new Date(partesFin[2], partesFin[1] - 1, partesFin[0]);
	var dias = Math.round((fin - ini) / (1000 * 60 * 60 * 24));
	return (dias >= 0 ? Math.floor(dias / 7) + 1 : 0);
}

function formatearNumero(valor)
{
	return (Math.round(valor * 100) / 100).toLocaleString('es-CO');
}

function pintarTarjetas(data, fechaInicial, fechaFinal)
{
	$('#res-semanas').text(calcularSemanas(fechaInicial, fechaFinal));
	$('#res-tiendas').text(data.filas.length);
	$('#res-total').text(formatearNumero(data.grantotal.total));

	if (data.filas.length > 0) {
		var sumaCv = 0;
		for (var i = 0; i < data.filas.length; i++) { sumaCv += Number(data.filas[i].coeficientevariacion); }
		var cvPromedio = sumaCv / data.filas.length;
		$('#res-cv').text(cvPromedio.toFixed(3));
		$('#tarjeta-cv').removeClass('vi-bien vi-alerta').addClass(cvPromedio > 0.35 ? 'vi-alerta' : (cvPromedio <= 0.15 ? 'vi-bien' : ''));
	} else {
		$('#res-cv').text('—');
		$('#tarjeta-cv').removeClass('vi-bien vi-alerta');
	}
}

function pintarAlerta(tiendasSinDatos)
{
	if (tiendasSinDatos && tiendasSinDatos.length > 0) {
		$('#alerta-sin-datos-texto').text(tiendasSinDatos.join(', '));
		$('#alerta-sin-datos').show();
	} else {
		$('#alerta-sin-datos').hide();
	}
}

/**
 * Pinta el resumen tipo correo: una fila por tienda (ya vienen ordenadas de
 * mayor a menor Total desde el servidor), columnas = categorias activas,
 * mas Total y CV; luego la fila de Contact Center y la de Total Red.
 */
function colorCv(cv)
{
	if (cv > 0.35) { return '#C21C1F'; }
	if (cv > 0.15) { return '#8a6d3b'; }
	return '#1B8A4B';
}

function construirFilaHtml(nombre, porcategoria, categorias, total, cv, mostrarCv, claseExtra)
{
	var html = '<tr class="' + (claseExtra || '') + '"><td>' + nombre + '</td>';
	for (var i = 0; i < categorias.length; i++) {
		var valor = porcategoria[categorias[i].idcategoria] || 0;
		html += '<td>' + formatearNumero(valor) + '</td>';
	}
	html += '<td>' + formatearNumero(total) + '</td>';
	if (mostrarCv) {
		html += '<td style="color:' + colorCv(cv) + ';font-weight:bold">' + Number(cv).toFixed(3) + '</td>';
	} else {
		html += '<td>&mdash;</td>';
	}
	html += '</tr>';
	return html;
}

function pintarResumen(data)
{
	var categorias = data.categorias;
	var contenedor = $('#resumen-container');

	if (data.filas.length == 0 && data.contactcenter.total == 0) {
		contenedor.html('<p style="text-align:center;color:#777;padding:20px 0">Sin datos para el rango o la tienda seleccionada. Recuerde que solo se muestran semanas ya cerradas por el proceso de Servicios.</p>');
		return;
	}

	var thead = '<thead><tr><th>Tienda</th>';
	for (var i = 0; i < categorias.length; i++) {
		thead += '<th>' + categorias[i].nombre + '</th>';
	}
	thead += '<th>Total</th><th>CV</th></tr></thead>';

	var tbody = '<tbody>';
	for (var f = 0; f < data.filas.length; f++) {
		var fila = data.filas[f];
		tbody += construirFilaHtml(fila.nombretienda, fila.porcategoria, categorias, fila.total,
				fila.coeficientevariacion, fila.total > 0, '');
	}
	tbody += construirFilaHtml('Contact Center', data.contactcenter.porcategoria, categorias,
			data.contactcenter.total, 0, false, 'vi-fila-cc');
	tbody += construirFilaHtml('TOTAL RED', data.grantotal.porcategoria, categorias, data.grantotal.total, 0,
			false, 'vi-fila-total');
	tbody += '</tbody>';

	contenedor.html('<table class="table table-condensed">' + thead + tbody + '</table>');
}

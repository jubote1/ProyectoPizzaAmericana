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
		pintarDetalle(data.detalle);
		pintarDispersion(data.dispersion);
		pintarTarjetas(data.detalle, data.dispersion, fechaInicial, fechaFinal);
	});
}

function pintarTarjetas(detalle, dispersion, fechaInicial, fechaFinal)
{
	var semanas = calcularSemanas(fechaInicial, fechaFinal);
	$('#res-semanas').text(semanas);

	var tiendas = {};
	var total = 0;
	for (var i = 0; i < detalle.length; i++) {
		tiendas[detalle[i].nombretienda] = true;
		total += Number(detalle[i].cantidadtotal);
	}
	$('#res-tiendas').text(Object.keys(tiendas).length);
	$('#res-total').text(formatearNumero(total));

	if (dispersion.length > 0) {
		var sumaCv = 0;
		for (var j = 0; j < dispersion.length; j++) { sumaCv += Number(dispersion[j].coeficientevariacion); }
		var cvPromedio = sumaCv / dispersion.length;
		$('#res-cv').text(cvPromedio.toFixed(3));
		$('#tarjeta-cv').removeClass('vi-bien vi-alerta').addClass(cvPromedio > 0.35 ? 'vi-alerta' : (cvPromedio <= 0.15 ? 'vi-bien' : ''));
	} else {
		$('#res-cv').text('—');
		$('#tarjeta-cv').removeClass('vi-bien vi-alerta');
	}
}

function calcularSemanas(fechaInicial, fechaFinal)
{
	// Las fechas llegan en dd/mm/yyyy (formato del datepicker en español).
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

function pintarDetalle(filas)
{
	var cuerpo = $('#grid-detalle tbody');
	cuerpo.empty();
	for(var i = 0; i < filas.length; i++){
		var f = filas[i];
		cuerpo.append('<tr>'
			+ '<td>' + f.nombretienda + '</td>'
			+ '<td>' + f.nombrecategoria + '</td>'
			+ '<td>' + f.cantidadtienda + '</td>'
			+ '<td>' + f.cantidadcontactcenter + '</td>'
			+ '<td>' + f.cantidadtotal + '</td>'
			+ '<td>' + Number(f.indicereddecategoria).toFixed(2) + '</td>'
			+ '</tr>');
	}
	if(filas.length == 0){
		cuerpo.append('<tr><td colspan="6" style="text-align:center;color:#777">Sin datos para el rango o la tienda seleccionada. Recuerde que solo se muestran semanas ya cerradas por el proceso de Servicios.</td></tr>');
	}
}

function pintarDispersion(filas)
{
	var cuerpo = $('#grid-dispersion tbody');
	cuerpo.empty();
	filas.sort(function(a, b){ return a.coeficientevariacion - b.coeficientevariacion; });
	for(var i = 0; i < filas.length; i++){
		var f = filas[i];
		var clase = 'vi-n1';
		if(f.coeficientevariacion > 0.35){
			clase = 'vi-n3';
		}else if(f.coeficientevariacion > 0.15){
			clase = 'vi-n2';
		}
		cuerpo.append('<tr class="' + clase + '">'
			+ '<td>' + f.nombretienda + '</td>'
			+ '<td>' + f.categoriasevaluadas + '</td>'
			+ '<td class="vi-cv">' + Number(f.coeficientevariacion).toFixed(3) + '</td>'
			+ '</tr>');
	}
	if(filas.length == 0){
		cuerpo.append('<tr><td colspan="3" style="text-align:center;color:#777">Sin datos suficientes para calcular el indicador.</td></tr>');
	}
}

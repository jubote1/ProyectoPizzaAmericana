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
	});
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
		var color = '#3c763d';
		if(f.coeficientevariacion > 0.35){
			color = '#a94442';
		}else if(f.coeficientevariacion > 0.15){
			color = '#8a6d3b';
		}
		cuerpo.append('<tr>'
			+ '<td>' + f.nombretienda + '</td>'
			+ '<td>' + f.categoriasevaluadas + '</td>'
			+ '<td style="color:' + color + ';font-weight:bold">' + Number(f.coeficientevariacion).toFixed(3) + '</td>'
			+ '</tr>');
	}
	if(filas.length == 0){
		cuerpo.append('<tr><td colspan="3" style="text-align:center;color:#777">Sin datos suficientes para calcular el indicador.</td></tr>');
	}
}

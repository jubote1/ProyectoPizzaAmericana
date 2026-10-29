/*
 * Identificacion del cliente en el mostrador, por tienda y por persona.
 *
 * Las funciones van prefijadas con "im" a proposito: esta pantalla puede
 * terminar conviviendo con otras en la misma pagina, y ventaIntegral.js y
 * consultaencuestamercadeo.js ya chocan entre ellos (los dos definen
 * cargarTiendas y pintarResumen). Mejor no sumar al problema.
 */

var server;

$(document).ready(function() {
	imValidarVigenciaLogueo();
	imCargarTiendas();
	imFechasPorDefecto();
});

function imValidarVigenciaLogueo()
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

function imCargarTiendas()
{
	$.getJSON(server + 'GetTiendas', function(data){
		var combo = $('#idtienda');
		for(var i = 0; i < data.length; i++){
			combo.append($('<option>', { value: data[i].id, text: data[i].nombre }));
		}
		tpRecortarCombo('#idtienda', '#avisoTiendaPermitida');
	});
}

/** Arranca con los ultimos siete dias terminados ayer: el mismo rango del correo. */
function imFechasPorDefecto()
{
	var hasta = new Date();
	hasta.setDate(hasta.getDate() - 1);
	var desde = new Date(hasta.getTime());
	desde.setDate(desde.getDate() - 6);
	$('#fechainicial').val(imATexto(desde));
	$('#fechafinal').val(imATexto(hasta));
}

function imATexto(fecha)
{
	var dia = ('0' + fecha.getDate()).slice(-2);
	var mes = ('0' + (fecha.getMonth() + 1)).slice(-2);
	return (dia + '/' + mes + '/' + fecha.getFullYear());
}

function imConsultar()
{
	var fechaInicial = $('#fechainicial').val();
	var fechaFinal = $('#fechafinal').val();
	var idTienda = $('#idtienda').val();
	if(fechaInicial == '' || fechaFinal == '')
	{
		bootbox.alert('Seleccione la fecha inicial y la fecha final.');
		return;
	}
	$('#im-calculando').show();
	$('#im-boton-consultar').prop('disabled', true);
	$.getJSON(server + 'ConsultarIdentificacionMostrador?fechainicial=' + fechaInicial + '&fechafinal=' + fechaFinal
			+ '&idtienda=' + idTienda, function(data){
		imPintarTarjetas(data);
		imPintarTabla(data);
		imPintarSinDatos(data.tiendassindatos);
		$('#im-minimo').text(data.minimopedidoscajero);
	}).fail(function(){
		//Sin esto el aviso de "calculando" se quedaria puesto y la pantalla
		//pareceria colgada.
		bootbox.alert('No se pudo consultar. Si el rango es largo puede haberse agotado el tiempo de espera; pruebe con menos dias.');
	}).always(function(){
		$('#im-calculando').hide();
		$('#im-boton-consultar').prop('disabled', false);
	});
}

function imNumero(valor)
{
	return (Number(valor) || 0).toLocaleString('es-CO');
}

function imColorPct(pct)
{
	if (pct >= 60) { return '#1B8A4B'; }
	if (pct >= 40) { return '#8a6d3b'; }
	return '#C21C1F';
}

function imPintarTarjetas(data)
{
	$('#im-res-pedidos').text(imNumero(data.totalpedidos));
	$('#im-res-identificados').text(imNumero(data.totalidentificados));
	$('#im-res-pedidos-pie').text(data.totalporcionessolas > 0
		? 'en el rango; no cuentan ' + imNumero(data.totalporcionessolas) + ' que son solo porciones'
		: 'en el rango');
	$('#im-res-tiendas').text(data.filas.length);
	if (data.totalpedidos > 0) {
		$('#im-res-pct').text(data.totalporcentaje + '%');
		$('#im-tarjeta-pct').removeClass('im-bien im-alerta')
			.addClass(data.totalporcentaje >= 60 ? 'im-bien' : (data.totalporcentaje < 40 ? 'im-alerta' : ''));
	} else {
		$('#im-res-pct').text('—');
		$('#im-tarjeta-pct').removeClass('im-bien im-alerta');
	}
}

function imPintarSinDatos(tiendas)
{
	if (tiendas && tiendas.length > 0) {
		$('#im-sin-datos-texto').text(tiendas.join(', '));
		$('#im-sin-datos').show();
	} else {
		$('#im-sin-datos').hide();
	}
}

function imEscapar(texto)
{
	return $('<div>').text(texto === null || texto === undefined ? '' : texto).html();
}

function imPintarTabla(data)
{
	if (!data.filas || data.filas.length === 0) {
		$('#tabla-container').html('<p class="im-pie">No hay pedidos de Punto de Venta en el rango.</p>');
		return;
	}
	var html = '<table class="table">';
	html += '<thead><tr><th>Tienda</th><th>Pedidos</th><th>Identificados</th><th>%</th>'
		+ '<th>Se negaron</th><th>Sin preguntar</th><th>Autorizacion SI / preguntadas</th></tr></thead><tbody>';
	for (var i = 0; i < data.filas.length; i++) {
		var f = data.filas[i];
		html += '<tr class="im-fila-tienda" data-fila="' + i + '">'
			+ '<td><span class="im-flecha">&#9654;</span> ' + imEscapar(f.tienda) + '</td>'
			+ '<td>' + imNumero(f.pedidos) + '</td>'
			+ '<td>' + imNumero(f.identificados) + '</td>'
			+ '<td style="color:' + imColorPct(f.porcentaje) + ';font-weight:bold">' + f.porcentaje + '%</td>'
			+ '<td>' + imNumero(f.senegaron) + '</td>'
			+ '<td>' + imNumero(f.sinpreguntar) + '</td>'
			+ '<td>' + imNumero(f.autsi) + ' / ' + imNumero(f.autpreguntadas) + '</td>'
			+ '</tr>';
		html += '<tr class="im-cajeros" id="im-cajeros-' + i + '" style="display:none"><td colspan="7">'
			+ imTablaCajeros(f.cajeros) + '</td></tr>';
	}
	html += '</tbody><tfoot><tr class="im-total"><td>Total red</td><td>' + imNumero(data.totalpedidos) + '</td><td>'
		+ imNumero(data.totalidentificados) + '</td><td>' + data.totalporcentaje
		+ '%</td><td></td><td></td><td></td></tr></tfoot></table>';
	$('#tabla-container').html(html);

	$('.im-fila-tienda').on('click', function(){
		var detalle = $('#im-cajeros-' + $(this).data('fila'));
		detalle.toggle();
		$(this).find('.im-flecha').html(detalle.is(':visible') ? '&#9660;' : '&#9654;');
	});
}

function imTablaCajeros(cajeros)
{
	if (!cajeros || cajeros.length === 0) {
		return ('<span class="im-pie">Nadie alcanzo el minimo de pedidos para listarse.</span>');
	}
	var html = '<table class="table"><thead><tr><th>Quien atendio</th><th>Pedidos</th><th>Identificados</th>'
		+ '<th>%</th><th>Se nego</th><th>Sin preguntar</th></tr></thead><tbody>';
	for (var i = 0; i < cajeros.length; i++) {
		var c = cajeros[i];
		html += '<tr><td>' + imEscapar(c.cajero) + '</td><td>' + imNumero(c.pedidos) + '</td><td>'
			+ imNumero(c.identificados) + '</td>'
			+ '<td style="color:' + imColorPct(c.porcentaje) + ';font-weight:bold">' + c.porcentaje + '%</td>'
			+ '<td>' + imNumero(c.senego) + '</td><td>' + imNumero(c.sinpreguntar) + '</td></tr>';
	}
	return (html + '</tbody></table>');
}

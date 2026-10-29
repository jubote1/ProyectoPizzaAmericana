/**
 * Operacion de domiciliarios de un dia en TODAS las tiendas.
 *
 * Prefijo orr para no chocar con reporteOperacionDomiciliarios.js (rod): las dos
 * pantallas son parientes y en algun momento alguien las va a abrir en la misma pagina.
 *
 * Sin frameworks ni CDN, como el resto del central: tiene que abrir igual en la red de
 * una tienda que no llegue a internet.
 */

$(document).ready(function () {
	orrFechaPorDefecto();
	$('#orr-consultar').on('click', orrConsultar);
	$('#orr-anterior').on('click', function (e) { e.preventDefault(); orrCorrerDia(-1); });
	$('#orr-siguiente').on('click', function (e) { e.preventDefault(); orrCorrerDia(1); });
});

function orrFechaPorDefecto() {
	//Arranca en AYER: es el ultimo dia cerrado, el que sale del datamart y responde rapido.
	var ayer = new Date();
	ayer.setDate(ayer.getDate() - 1);
	$('#fecha').val(orrATexto(ayer));
}

function orrATexto(fecha) {
	var dd = ('0' + fecha.getDate()).slice(-2);
	var mm = ('0' + (fecha.getMonth() + 1)).slice(-2);
	return (dd + '/' + mm + '/' + fecha.getFullYear());
}

function orrADate(texto) {
	var p = (texto || '').split('/');
	if (p.length !== 3) {
		return (null);
	}
	return (new Date(parseInt(p[2], 10), parseInt(p[1], 10) - 1, parseInt(p[0], 10)));
}

function orrCorrerDia(dias) {
	var f = orrADate($('#fecha').val());
	if (f === null) {
		return;
	}
	f.setDate(f.getDate() + dias);
	$('#fecha').val(orrATexto(f));
	orrConsultar();
}

function orrConsultar() {
	var fecha = $('#fecha').val();
	if (orrADate(fecha) === null) {
		bootbox.alert('Escriba el d&iacute;a como dd/mm/aaaa.');
		return;
	}
	$('#orr-mensaje').html('');
	$('#orr-resultado').hide();
	$('#orr-calculando').show();
	$.ajax({
		url: server + 'ConsultarOperacionRed',
		dataType: 'json',
		type: 'get',
		data: { fecha: fecha },
		success: function (d) {
			$('#orr-calculando').hide();
			if (!d || d.error) {
				orrMensaje('error', d && d.error ? orrEscapar(d.error) : 'No se pudo consultar.');
				return;
			}
			orrPintar(d);
			$('#orr-resultado').show();
		},
		error: function () {
			$('#orr-calculando').hide();
			orrMensaje('error', 'No se pudo consultar. Intente de nuevo.');
		}
	});
}

function orrMensaje(tipo, html) {
	$('#orr-mensaje').html('<div class="rod-aviso rod-aviso-' + tipo + '">' + html + '</div>');
}

function orrEscapar(texto) {
	return ($('<div/>').text(texto === null || texto === undefined ? '' : texto).html());
}

/** Un numero con coma decimal, o una raya cuando no hay dato. */
function orrNum(valor, decimales) {
	if (valor === null || valor === undefined) {
		return ('<span class="rod-gris">&mdash;</span>');
	}
	var n = Number(valor);
	if (isNaN(n)) {
		return ('<span class="rod-gris">&mdash;</span>');
	}
	return (n.toFixed(decimales === undefined ? 1 : decimales).replace('.', ','));
}

function orrEntero(valor) {
	return (valor === null || valor === undefined ? 0 : Number(valor));
}

/** El color del cumplimiento: verde arriba de 90, naranja de 80 a 90, rojo debajo. */
function orrClasePorcentaje(p) {
	if (p === null || p === undefined) {
		return ('rod-gris');
	}
	if (p >= 90) {
		return ('rod-verde');
	}
	if (p >= 80) {
		return ('rod-naranja');
	}
	return ('rod-rojo');
}

function orrPintar(d) {
	orrAvisos(d);
	orrTarjetas(d.total || {}, (d.tiendas || []).length);
	orrTabla(d.tiendas || [], d.total || {});
}

function orrAvisos(d) {
	var html = '';
	if (d.datos_hasta) {
		html += '<div class="rod-aviso rod-aviso-info">D&iacute;a cerrado: los datos salen del <b>datamart</b> '
			+ '(llega hasta el ' + orrEscapar(d.datos_hasta) + ').</div>';
	}
	var sin = d.sin_respuesta || [];
	if (sin.length > 0) {
		//Una tienda apagada NO entra al Total Red. Si no se dice, el total se lee como si
		//esa tienda hubiera trabajado y le hubiera ido mal.
		var nombres = [];
		for (var i = 0; i < sin.length; i++) {
			nombres.push('<b>' + orrEscapar(sin[i].tienda) + '</b>');
		}
		html += '<div class="rod-aviso rod-aviso-alerta">No contestaron ' + nombres.join(', ')
			+ '. <b>No entran en el Total Red</b>: no es que no hayan trabajado, es que no se pudieron leer.</div>';
	}
	$('#orr-avisos').html(html);
}

function orrTarjetas(t, cuantasTiendas) {
	var tarjetas = [
		{ rotulo: 'Tiendas', cifra: cuantasTiendas, pie: 'contestaron' },
		{ rotulo: 'Domiciliarios', cifra: orrEntero(t.domiciliarios), pie: orrEntero(t.con_pedidos) + ' llevaron pedidos' },
		{ rotulo: 'Pedidos llevados', cifra: orrEntero(t.pedidos), pie: orrEntero(t.salidas) + ' salidas' },
		{ rotulo: 'A tiempo',
		  cifra: '<span class="' + orrClasePorcentaje(t.porcentaje) + '">' + orrNum(t.porcentaje) + '%</span>',
		  pie: orrEntero(t.a_tiempo) + ' de ' + orrEntero(t.medibles) + ' medidos' },
		{ rotulo: 'En ruta', cifra: orrNum(t.prom_ruta) + ' <small>min</small>',
		  pie: 'promedio de salir a entregar' },
		{ rotulo: 'Regreso', cifra: orrNum(t.prom_regreso) + ' <small>min</small>',
		  pie: orrEntero(t.regresos_demorados) + ' demorados' }
	];
	var html = '';
	for (var i = 0; i < tarjetas.length; i++) {
		html += '<div class="col-sm-2"><div class="rod-tarjeta">'
			+ '<div class="rod-rotulo">' + tarjetas[i].rotulo + '</div>'
			+ '<div class="rod-cifra">' + tarjetas[i].cifra + '</div>'
			+ '<div class="rod-pie">' + tarjetas[i].pie + '</div>'
			+ '</div></div>';
	}
	$('#orr-tarjetas').html(html);
}

function orrTabla(filas, total) {
	//El maximo manda el ancho de las barritas: sirven para comparar entre tiendas, no
	//contra una escala absoluta.
	var maxPedidos = 1;
	for (var i = 0; i < filas.length; i++) {
		if (orrEntero(filas[i].pedidos) > maxPedidos) {
			maxPedidos = orrEntero(filas[i].pedidos);
		}
	}
	//De la que peor cumplio a la mejor: lo que hay que mirar queda arriba.
	filas.sort(function (a, b) {
		var pa = (a.porcentaje === null || a.porcentaje === undefined) ? 999 : a.porcentaje;
		var pb = (b.porcentaje === null || b.porcentaje === undefined) ? 999 : b.porcentaje;
		return (pa - pb);
	});

	var html = '<thead><tr><th>Tienda</th><th>Domis</th><th>Salidas</th><th>Pedidos</th><th>Por salida</th>'
		+ '<th>A tiempo</th><th>Tarde</th><th>En ruta</th><th>Regreso</th><th>Regresos tarde</th>'
		+ '<th>Sin dato</th></tr></thead><tbody>';
	for (var k = 0; k < filas.length; k++) {
		var f = filas[k];
		var pedidos = orrEntero(f.pedidos);
		var salidas = orrEntero(f.salidas);
		var ancho = Math.round(pedidos * 100 / maxPedidos);
		html += '<tr>'
			+ '<td>' + orrEscapar(f.tienda) + '</td>'
			+ '<td>' + orrEntero(f.domiciliarios) + '</td>'
			+ '<td>' + salidas + '</td>'
			+ '<td>' + pedidos + '<span class="orr-barra" style="width:' + ancho + '%"></span></td>'
			+ '<td>' + orrNum(salidas > 0 ? pedidos / salidas : null) + '</td>'
			+ '<td class="' + orrClasePorcentaje(f.porcentaje) + '">' + orrNum(f.porcentaje) + '%</td>'
			+ '<td>' + orrEntero(f.demorados) + '</td>'
			+ '<td>' + orrNum(f.prom_ruta) + '</td>'
			+ '<td>' + orrNum(f.prom_regreso) + '</td>'
			+ '<td>' + orrEntero(f.regresos_demorados) + '</td>'
			+ '<td class="rod-gris">' + orrEntero(f.regresos_sin_dato) + '</td>'
			+ '</tr>';
	}
	var totSalidas = orrEntero(total.salidas);
	var totPedidos = orrEntero(total.pedidos);
	html += '<tr class="rod-total">'
		+ '<td>Total Red</td>'
		+ '<td>' + orrEntero(total.domiciliarios) + '</td>'
		+ '<td>' + totSalidas + '</td>'
		+ '<td>' + totPedidos + '</td>'
		+ '<td>' + orrNum(totSalidas > 0 ? totPedidos / totSalidas : null) + '</td>'
		+ '<td class="' + orrClasePorcentaje(total.porcentaje) + '">' + orrNum(total.porcentaje) + '%</td>'
		+ '<td>' + orrEntero(total.demorados) + '</td>'
		+ '<td>' + orrNum(total.prom_ruta) + '</td>'
		+ '<td>' + orrNum(total.prom_regreso) + '</td>'
		+ '<td>' + orrEntero(total.regresos_demorados) + '</td>'
		+ '<td class="rod-gris">' + orrEntero(total.regresos_sin_dato) + '</td>'
		+ '</tr></tbody>';
	$('#orr-tabla').html(html);
}

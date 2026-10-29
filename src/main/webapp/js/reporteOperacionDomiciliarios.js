/*
 * Reporte de operacion de domiciliarios diaria.
 *
 * Que domiciliarios trabajaron en una tienda un dia, cuantos pedidos llevaron,
 * cuanto se demoraron en ruta y regresando, y cuantos pedidos salieron por hora.
 * Los pedidos que llegaron tarde se parten entre domiciliarios internos y
 * externos. Las definiciones (tiempo en ruta, regreso, externo, tarde) estan en
 * capaDAOCC.OperacionDomiciliariosDAO y se resumen al final de la pantalla.
 *
 * Las funciones llevan el prefijo "rod" a proposito: otras pantallas del central
 * definen cargarTiendas y pintarResumen con el mismo nombre y se pisan entre si.
 *
 * Las graficas son SVG escrito a mano, sin librerias externas: la pantalla tiene
 * que abrir igual en una red de tienda que no llegue a un CDN.
 */

var server;

/** El dia operativo arranca hacia las 10 de la manana y termina de madrugada. */
var ROD_ORDEN_HORAS = [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

var rodDatos = null;
/** Que domiciliario esta abierto en el detalle; 0 = ninguno. */
var rodDomiSel = 0;
/** TODOS, INTERNOS o EXTERNOS: que pedidos tarde se listan. */
var rodFiltroTarde = 'TODOS';

$(document).ready(function() {
	rodValidarVigenciaLogueo();
	rodCargarTiendas();
	rodFechaPorDefecto();
	$('#fecha').on('keydown', function(e) {
		if (e.which === 13) {
			rodConsultar();
		}
	});
});

function rodValidarVigenciaLogueo()
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

function rodCargarTiendas()
{
	$.getJSON(server + 'GetTiendas', function(data){
		var combo = $('#idtienda');
		for(var i = 0; i < data.length; i++){
			combo.append($('<option>', { value: data[i].id, text: data[i].nombre }));
		}
		//Se acuerda de la ultima tienda: un administrador casi siempre mira la misma.
		try {
			var guardada = window.localStorage.getItem('rodTienda');
			if (guardada && combo.find('option[value="' + guardada + '"]').length > 0) {
				combo.val(guardada);
			}
		} catch (e) {
			//Sin almacenamiento local la pantalla funciona igual.
		}
		tpRecortarCombo('#idtienda', '#avisoTiendaPermitida');
	});
}

function rodFechaPorDefecto()
{
	$('#fecha').val(rodATexto(new Date()));
}

function rodATexto(fecha)
{
	var dd = ('0' + fecha.getDate()).slice(-2);
	var mm = ('0' + (fecha.getMonth() + 1)).slice(-2);
	return (dd + '/' + mm + '/' + fecha.getFullYear());
}

/** De dd/mm/yyyy a un Date; null si no es una fecha. */
function rodAFecha(texto)
{
	var m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec($.trim(texto || ''));
	if (!m) {
		return null;
	}
	return new Date(parseInt(m[3], 10), parseInt(m[2], 10) - 1, parseInt(m[1], 10));
}

function rodMoverDia(delta)
{
	var f = rodAFecha($('#fecha').val());
	if (f === null) {
		f = new Date();
	}
	f.setDate(f.getDate() + delta);
	$('#fecha').val(rodATexto(f));
	if (parseInt($('#idtienda').val(), 10) > 0) {
		rodConsultar();
	}
}

// ===========================================================================
// Consulta
// ===========================================================================

function rodConsultar()
{
	var idTienda = parseInt($('#idtienda').val(), 10);
	var fecha = $.trim($('#fecha').val());
	if (!(idTienda > 0)) {
		bootbox.alert('Escoja la tienda.');
		return;
	}
	if (rodAFecha(fecha) === null) {
		bootbox.alert('Escriba el día como dd/mm/aaaa.');
		return;
	}
	try {
		window.localStorage.setItem('rodTienda', String(idTienda));
	} catch (e) {
		//Nada: es solo una comodidad.
	}
	$('#rod-boton-consultar').prop('disabled', true);
	$('#rod-calculando').show();
	$('#rod-resultado').hide();
	$('#rod-avisos').empty();
	$.ajax({
		url: server + 'ConsultarOperacionDomiciliarios',
		dataType: 'json',
		type: 'post',
		data: { idtienda: idTienda, fecha: fecha },
		success: function(data) {
			if (data.error) {
				rodAviso('error', rodEscapar(data.error));
				return;
			}
			rodDatos = data;
			rodDomiSel = 0;
			rodFiltroTarde = 'TODOS';
			rodPintarTodo();
		},
		error: function() {
			rodAviso('error', 'No hubo respuesta del servidor. Verifique la conexión e intente de nuevo.');
		},
		complete: function() {
			$('#rod-boton-consultar').prop('disabled', false);
			$('#rod-calculando').hide();
		}
	});
}

function rodAviso(tipo, html)
{
	$('#rod-avisos').append('<div class="rod-aviso rod-aviso-' + tipo + '">' + html + '</div>');
}

// ===========================================================================
// Auxiliares de formato
// ===========================================================================

function rodEscapar(texto)
{
	return $('<div>').text(texto === null || texto === undefined ? '' : texto).html();
}

/** Un numero con coma decimal, o una raya si no hay dato. */
function rodNum(valor, decimales)
{
	if (valor === null || valor === undefined) {
		return '&mdash;';
	}
	var d = (decimales === undefined) ? 1 : decimales;
	var t = Number(valor).toFixed(d);
	return t.replace('.', ',');
}

function rodEntero(valor)
{
	return (valor === null || valor === undefined) ? '&mdash;' : String(valor);
}

function rodPct(valor)
{
	return (valor === null || valor === undefined) ? '&mdash;' : (rodNum(valor, 1) + '%');
}

function rodHoras(minutos)
{
	if (minutos === null || minutos === undefined) {
		return '&mdash;';
	}
	var h = Math.floor(minutos / 60);
	var m = minutos % 60;
	return h + ' h ' + ('0' + m).slice(-2) + ' min';
}

function rodHoraTexto(h)
{
	return ('0' + h).slice(-2) + ':00';
}

function rodChip(externo)
{
	return externo
		? '<span class="rod-chip rod-chip-ext">EXTERNO</span>'
		: '<span class="rod-chip rod-chip-int">INTERNO</span>';
}

/** Verde si va bien, naranja si regular, rojo si mal: el mismo corte que usan los otros reportes. */
function rodClasePct(pct)
{
	if (pct === null || pct === undefined) {
		return 'rod-gris';
	}
	if (pct >= 85) {
		return 'rod-verde';
	}
	if (pct >= 65) {
		return 'rod-naranja';
	}
	return 'rod-rojo';
}

// ===========================================================================
// Pintar
// ===========================================================================

function rodPintarTodo()
{
	var d = rodDatos;
	$('#rod-avisos').empty();
	$('#rod-alerta').text(d.alerta_regreso);
	$('#rod-tope').text(d.tope_regreso);

	if (d.fuente === 'DATAMART') {
		rodAviso('info', 'D&iacute;a cerrado: los datos salen del <b>datamart</b>' + (d.datos_hasta ? ' (llega hasta el ' + rodEscapar(d.datos_hasta) + ')' : '') + '.');
	} else {
		rodAviso('info', 'Datos <b>en vivo</b> de la tienda ' + rodEscapar(d.tienda) + '. Lo de hoy puede cambiar mientras se trabaja.');
	}
	if (d.resumen.domiciliarios === 0) {
		rodAviso('alerta', 'Ese d&iacute;a no hay domiciliarios ni salidas registradas en ' + rodEscapar(d.tienda) + '.');
	}
	if (d.resumen.sin_entrega > 0) {
		rodAviso('alerta', 'Hay <b>' + d.resumen.sin_entrega + '</b> pedido(s) que salieron a ruta y no tienen hora de entrega marcada; no entran en los tiempos.');
	}

	rodPintarTarjetas();
	rodPintarComparacion();
	rodPintarTarde();
	rodPintarDomiciliarios();
	rodPintarDetalle();
	rodPintarHoras();
	rodPintarMatriz();
	$('#rod-resultado').show();
}

function rodTarjeta(rotulo, cifra, pie, clase)
{
	return '<div class="col-md-2 col-sm-4"><div class="rod-tarjeta"><div class="rod-rotulo">' + rotulo
		+ '</div><div class="rod-cifra ' + (clase || '') + '">' + cifra + '</div><div class="rod-pie">' + pie + '</div></div></div>';
}

function rodPintarTarjetas()
{
	var r = rodDatos.resumen;
	var html = '';
	html += rodTarjeta('Domiciliarios', r.domiciliarios,
		r.internos.domiciliarios + ' internos &middot; ' + r.externos.domiciliarios + ' externos' + (r.sin_pedidos > 0 ? '<br>' + r.sin_pedidos + ' sin pedidos' : ''));
	html += rodTarjeta('Pedidos llevados', r.pedidos, r.salidas + ' salidas &middot; ' + rodNum(r.pedidos_por_salida) + ' por salida');
	html += rodTarjeta('A tiempo', rodPct(r.porcentaje), r.a_tiempo + ' de ' + r.medibles + ' medidos' + (r.programados > 0 ? '<br>' + r.programados + ' programados sin medir' : ''), rodClasePct(r.porcentaje));
	html += rodTarjeta('Tarde', r.demorados, 'de ' + r.medibles + ' pedidos medidos', r.demorados > 0 ? 'rod-rojo' : '');
	html += rodTarjeta('En ruta', rodNum(r.prom_ruta) + ' <span style="font-size:14px">min</span>', 'promedio de salir a entregar');
	html += rodTarjeta('Regreso', rodNum(r.prom_regreso) + ' <span style="font-size:14px">min</span>',
		r.regresos_demorados + ' demorados de ' + r.regresos_validos + (r.regresos_sin_dato > 0 ? '<br>' + r.regresos_sin_dato + ' sin dato' : ''));
	$('#rod-tarjetas').html(html);
}

function rodBloqueGrupo(titulo, clase, g, totalTarde)
{
	var parte = (totalTarde > 0) ? Math.round(g.demorados * 100 / totalTarde) : 0;
	var html = '<div class="rod-grupo ' + clase + '"><h5>' + titulo + '</h5>';
	html += '<div class="rod-gran ' + (g.demorados > 0 ? 'rod-rojo' : 'rod-verde') + '">' + g.demorados + ' <span style="font-size:14px;font-weight:normal">pedidos tarde</span></div>';
	html += '<div class="rod-pie">' + (totalTarde > 0 ? ('son el ' + parte + '% de todos los pedidos tarde') : 'ning&uacute;n pedido tarde ese d&iacute;a') + '</div>';
	html += '<dl>';
	html += '<dt>Domiciliarios</dt><dd>' + g.domiciliarios + (g.con_pedidos !== g.domiciliarios ? ' (' + g.con_pedidos + ' con pedidos)' : '') + '</dd>';
	html += '<dt>Pedidos llevados</dt><dd>' + g.pedidos + ' en ' + g.salidas + ' salidas</dd>';
	html += '<dt>Pedidos medidos</dt><dd>' + g.medibles + '</dd>';
	html += '<dt>A tiempo</dt><dd><span class="' + rodClasePct(g.porcentaje) + '">' + rodPct(g.porcentaje) + '</span> (' + g.a_tiempo + ' de ' + g.medibles + ')</dd>';
	html += '<dt>Tarde sobre medidos</dt><dd>' + (g.medibles > 0 ? rodNum(g.demorados * 100 / g.medibles) + '%' : '&mdash;') + '</dd>';
	html += '<dt>Tiempo en ruta</dt><dd>' + rodNum(g.prom_ruta) + ' min</dd>';
	html += '<dt>Regreso</dt><dd>' + rodNum(g.prom_regreso) + ' min' + (g.regresos_demorados > 0 ? ' &middot; ' + g.regresos_demorados + ' demorados' : '') + '</dd>';
	html += '</dl></div>';
	return html;
}

function rodPintarComparacion()
{
	var r = rodDatos.resumen;
	var total = r.demorados;
	var pInt = (total > 0) ? Math.round(r.internos.demorados * 100 / total) : 0;
	var pExt = (total > 0) ? (100 - pInt) : 0;
	var html = '<div class="row"><div class="col-md-6" style="margin-bottom:12px">' + rodBloqueGrupo('INTERNOS &middot; de n&oacute;mina', 'rod-grupo-int', r.internos, total) + '</div>'
		+ '<div class="col-md-6" style="margin-bottom:12px">' + rodBloqueGrupo('EXTERNOS &middot; temporales', 'rod-grupo-ext', r.externos, total) + '</div></div>';
	if (total > 0) {
		html += '<div class="rod-pie"><b>' + total + '</b> pedidos llegaron tarde: <b>' + r.internos.demorados + '</b> los llevaron internos y <b>' + r.externos.demorados + '</b> externos.</div>';
		html += '<div class="rod-reparto">'
			+ (r.internos.demorados > 0 ? '<span style="width:' + pInt + '%;background:#E42528">Internos ' + r.internos.demorados + '</span>' : '')
			+ (r.externos.demorados > 0 ? '<span style="width:' + pExt + '%;background:#C46A00">Externos ' + r.externos.demorados + '</span>' : '')
			+ '</div>';
	}
	$('#rod-comparacion').html(html);
}

function rodPintarTarde()
{
	var d = rodDatos;
	var lista = d.pedidos_tarde || [];
	var nInt = 0, nExt = 0;
	for (var i = 0; i < lista.length; i++) {
		if (lista[i].externo) { nExt++; } else { nInt++; }
	}
	var filtros = [['TODOS', 'Todos (' + lista.length + ')'], ['INTERNOS', 'Internos (' + nInt + ')'], ['EXTERNOS', 'Externos (' + nExt + ')']];
	var f = '';
	for (var k = 0; k < filtros.length; k++) {
		f += '<a href="#" data-f="' + filtros[k][0] + '" class="' + (rodFiltroTarde === filtros[k][0] ? 'rod-activo' : '') + '">' + filtros[k][1] + '</a>';
	}
	$('#rod-filtro-tarde').html(f);
	$('#rod-filtro-tarde a').on('click', function(e) {
		e.preventDefault();
		rodFiltroTarde = $(this).attr('data-f');
		rodPintarTarde();
	});

	if (lista.length === 0) {
		$('#rod-tarde').html('<div class="rod-aviso rod-aviso-info">Ning&uacute;n pedido llev&oacute; m&aacute;s del tiempo prometido ese d&iacute;a.</div>');
		return;
	}
	var html = '<table class="rod-tabla"><thead><tr><th>Pedido</th><th class="rod-izq">Domiciliario</th><th class="rod-izq">Tipo</th><th class="rod-izq">Empresa</th>'
		+ '<th>Entr&oacute;</th><th>Sali&oacute;</th><th>Entreg&oacute;</th><th>Promesa</th><th>Total</th><th>Tarde por</th><th>Espera en tienda</th><th>En ruta</th></tr></thead><tbody>';
	var mostrados = 0;
	for (var j = 0; j < lista.length; j++) {
		var t = lista[j];
		if ((rodFiltroTarde === 'INTERNOS' && t.externo) || (rodFiltroTarde === 'EXTERNOS' && !t.externo)) {
			continue;
		}
		mostrados++;
		html += '<tr><td>' + t.id_pedido + '</td><td class="rod-izq">' + rodEscapar(t.domiciliario) + '</td><td class="rod-izq">' + rodChip(t.externo) + '</td>'
			+ '<td class="rod-izq">' + rodEscapar(t.empresa) + '</td>'
			+ '<td>' + rodEscapar(t.hora_entrada) + '</td><td>' + rodEscapar(t.hora_salida) + '</td><td>' + rodEscapar(t.hora_entrega) + '</td>'
			+ '<td>' + t.prometido + ' min</td><td>' + t.min_total + ' min</td><td class="rod-rojo">+' + t.exceso + ' min</td>'
			+ '<td>' + (t.min_espera === null ? '&mdash;' : t.min_espera + ' min') + '</td><td>' + (t.min_ruta === null ? '&mdash;' : t.min_ruta + ' min') + '</td></tr>';
	}
	html += '</tbody></table>';
	$('#rod-tarde').html(mostrados > 0 ? html : '<div class="rod-aviso rod-aviso-info">Ning&uacute;n pedido tarde en este grupo.</div>');
}

function rodPintarDomiciliarios()
{
	var d = rodDatos;
	var html = '<table class="rod-tabla"><thead><tr><th>Domiciliario</th><th class="rod-izq">Tipo</th><th class="rod-izq">Empresa</th><th>Ingreso &ndash; salida</th><th>En la tienda</th>'
		+ '<th>Salidas</th><th>Pedidos</th><th>Pedidos por hora</th><th>En ruta</th><th>Regreso</th><th>A tiempo</th><th>Tarde</th><th title="Regresos sin marcar, de m&aacute;s de 120 min o negativos: no entran al promedio">Sin dato</th></tr></thead><tbody>';
	for (var i = 0; i < d.domiciliarios.length; i++) {
		var o = d.domiciliarios[i];
		var presencia = (o.ingreso || o.salida) ? (rodEscapar(o.ingreso || '?') + ' &ndash; ' + rodEscapar(o.salida || '?')) : '&mdash;';
		html += '<tr class="rod-clic' + (o.pedidos === 0 ? ' rod-sin' : '') + (rodDomiSel === o.id ? ' rod-sel' : '') + '" data-id="' + o.id + '">'
			+ '<td><b>' + rodEscapar(o.nombre) + '</b></td><td class="rod-izq">' + rodChip(o.tipo === 'EXTERNO') + '</td><td class="rod-izq">' + rodEscapar(o.empresa) + '</td>'
			+ '<td>' + presencia + '</td><td>' + rodHoras(o.minutos_turno) + '</td>'
			+ '<td>' + o.salidas + '</td><td><b>' + o.pedidos + '</b></td><td>' + rodNum(o.pedidos_por_hora) + '</td>'
			+ '<td>' + rodNum(o.prom_ruta) + ' min</td><td>' + rodNum(o.prom_regreso) + ' min</td>'
			+ '<td><span class="' + rodClasePct(o.porcentaje) + '">' + rodPct(o.porcentaje) + '</span></td>'
			+ '<td>' + (o.demorados > 0 ? '<span class="rod-rojo">' + o.demorados + '</span>' : o.demorados) + '</td>'
			+ '<td>' + (o.regresos_sin_dato > 0 ? o.regresos_sin_dato : '') + '</td></tr>';
	}
	var r = d.resumen;
	html += '<tr class="rod-total"><td>Total</td><td></td><td></td><td></td><td>' + rodHoras(Math.round(r.horas_en_tienda * 60)) + '</td>'
		+ '<td>' + r.salidas + '</td><td>' + r.pedidos + '</td><td>' + rodNum(r.horas_en_tienda > 0 ? r.pedidos / r.horas_en_tienda : null) + '</td>'
		+ '<td>' + rodNum(r.prom_ruta) + ' min</td><td>' + rodNum(r.prom_regreso) + ' min</td>'
		+ '<td>' + rodPct(r.porcentaje) + '</td><td>' + r.demorados + '</td><td>' + r.regresos_sin_dato + '</td></tr>';
	html += '</tbody></table>';
	$('#rod-domiciliarios').html(html);
	$('#rod-domiciliarios tr.rod-clic').on('click', function() {
		var id = parseInt($(this).attr('data-id'), 10);
		rodDomiSel = (rodDomiSel === id) ? 0 : id;
		rodPintarDomiciliarios();
		rodPintarDetalle();
	});
}

function rodPintarDetalle()
{
	var cont = $('#rod-detalle-domi');
	if (rodDomiSel === 0) {
		cont.empty();
		return;
	}
	var d = rodDatos;
	var domi = null;
	for (var i = 0; i < d.domiciliarios.length; i++) {
		if (d.domiciliarios[i].id === rodDomiSel) {
			domi = d.domiciliarios[i];
		}
	}
	if (domi === null) {
		cont.empty();
		return;
	}
	var html = '<div class="rod-detalle"><h5>' + rodEscapar(domi.nombre) + ' ' + rodChip(domi.tipo === 'EXTERNO') + (domi.empresa ? ' <span class="rod-gris" style="font-weight:normal">' + rodEscapar(domi.empresa) + '</span>' : '') + '</h5>';
	var salidas = [];
	for (var s = 0; s < d.salidas.length; s++) {
		if (d.salidas[s].id_domiciliario === rodDomiSel) {
			salidas.push(d.salidas[s]);
		}
	}
	if (salidas.length === 0) {
		html += '<div class="rod-pie">Marc&oacute; ingreso pero no sali&oacute; con ning&uacute;n pedido ese d&iacute;a.</div></div>';
		cont.html(html);
		return;
	}
	html += '<div class="rod-envuelve"><table class="rod-tabla"><thead><tr><th>Salida</th><th>Regres&oacute;</th><th>Pedidos</th><th>Salida &rarr; &uacute;ltima entrega</th><th>Regreso</th><th class="rod-izq">Detalle del regreso</th></tr></thead><tbody>';
	for (var k = 0; k < salidas.length; k++) {
		var sa = salidas[k];
		html += '<tr class="rod-salida"><td>' + rodEscapar(sa.hora_salida) + '</td><td>' + rodEscapar(sa.hora_regreso || '&mdash;') + '</td><td>' + sa.pedidos.length + '</td>'
			+ '<td>' + (sa.min_hasta_ultima === null ? '&mdash;' : sa.min_hasta_ultima + ' min') + '</td>'
			+ '<td>' + (sa.min_regreso === null ? '&mdash;' : '<span class="' + (sa.min_regreso > d.alerta_regreso ? 'rod-rojo' : '') + '">' + sa.min_regreso + ' min</span>') + '</td>'
			+ '<td class="rod-izq">' + rodTextoRegreso(sa.estado_regreso) + '</td></tr>';
		for (var p = 0; p < sa.pedidos.length; p++) {
			var pe = sa.pedidos[p];
			var estado;
			if (!pe.entregado) {
				estado = '<span class="rod-gris">sin hora de entrega</span>';
			} else if (pe.programado) {
				estado = '<span class="rod-gris">programado</span>';
			} else if (!pe.medible) {
				estado = '<span class="rod-gris">sin medir</span>';
			} else if (pe.a_tiempo) {
				estado = '<span class="rod-verde">a tiempo</span>';
			} else {
				estado = '<span class="rod-rojo">tarde</span>';
			}
			html += '<tr class="rod-sub"><td class="rod-izq" colspan="2">&nbsp;&nbsp;&nbsp;Pedido ' + pe.id_pedido + ' &middot; entr&oacute; ' + rodEscapar(pe.hora_entrada || '?') + ' &middot; entreg&oacute; ' + rodEscapar(pe.hora_entrega || '?') + '</td>'
				+ '<td>' + (pe.min_total === null ? '' : 'total ' + pe.min_total + ' / ' + pe.prometido + ' min') + '</td>'
				+ '<td>espera ' + (pe.min_espera === null ? '&mdash;' : pe.min_espera + ' min') + '</td>'
				+ '<td>en ruta ' + (pe.min_ruta === null ? '&mdash;' : pe.min_ruta + ' min') + '</td>'
				+ '<td class="rod-izq">' + estado + '</td></tr>';
		}
	}
	html += '</tbody></table></div></div>';
	cont.html(html);
}

function rodTextoRegreso(estado)
{
	switch (estado) {
		case 'OK': return '';
		case 'SINREGRESO': return '<span class="rod-gris">no marc&oacute; el regreso</span>';
		case 'SINENTREGA': return '<span class="rod-gris">sin entrega contra qu&eacute; medirlo</span>';
		case 'NEGATIVO': return '<span class="rod-gris">regreso marcado antes de la entrega</span>';
		case 'SINMARCAR': return '<span class="rod-gris">regreso de m&aacute;s de ' + rodDatos.tope_regreso + ' min: no se marc&oacute;</span>';
		default: return '';
	}
}

// ===========================================================================
// Por hora
// ===========================================================================

function rodPintarHoras()
{
	var horas = rodDatos.por_hora;
	var max = 1;
	for (var i = 0; i < horas.length; i++) {
		if (horas[i].pedidos > max) {
			max = horas[i].pedidos;
		}
	}
	var ancho = 920, alto = 250, mIzq = 36, mInf = 40, mSup = 16;
	var anchoUtil = ancho - mIzq - 8;
	var altoUtil = alto - mInf - mSup;
	var paso = anchoUtil / horas.length;
	var barra = Math.max(8, paso - 8);
	var svg = '<svg class="rod-svg" viewBox="0 0 ' + ancho + ' ' + alto + '" role="img" aria-label="Pedidos que salieron por hora">';
	//Lineas guia
	var divisiones = Math.min(max, 4);
	for (var g = 0; g <= divisiones; g++) {
		var valor = Math.round(max * g / divisiones);
		var y = mSup + altoUtil - (altoUtil * valor / max);
		svg += '<line x1="' + mIzq + '" x2="' + (ancho - 4) + '" y1="' + y + '" y2="' + y + '" stroke="#E2E6EC" stroke-width="1"/>'
			+ '<text x="' + (mIzq - 6) + '" y="' + (y + 4) + '" text-anchor="end" font-size="11" fill="#6C7482">' + valor + '</text>';
	}
	for (var k = 0; k < horas.length; k++) {
		var h = horas[k];
		var x = mIzq + paso * k + (paso - barra) / 2;
		var tardeInt = h.tarde_internos, tardeExt = h.tarde_externos;
		var base = h.pedidos - tardeInt - tardeExt;
		var yBase = mSup + altoUtil;
		var hBase = altoUtil * base / max;
		var hInt = altoUtil * tardeInt / max;
		var hExt = altoUtil * tardeExt / max;
		var titulo = rodHoraTexto(h.hora) + ': ' + h.pedidos + ' pedidos, ' + h.salidas + ' salidas, ' + h.domiciliarios + ' domiciliarios'
			+ (h.medibles > 0 ? ', tarde ' + (tardeInt + tardeExt) + ' de ' + h.medibles + ' medidos (internos ' + tardeInt + ', externos ' + tardeExt + ')' : '');
		svg += '<g><title>' + rodEscapar(titulo) + '</title>';
		if (h.pedidos > 0) {
			svg += '<rect x="' + x + '" y="' + (yBase - hBase) + '" width="' + barra + '" height="' + hBase + '" fill="#102F6F" rx="2"/>';
			svg += '<rect x="' + x + '" y="' + (yBase - hBase - hInt) + '" width="' + barra + '" height="' + hInt + '" fill="#E42528"/>';
			svg += '<rect x="' + x + '" y="' + (yBase - hBase - hInt - hExt) + '" width="' + barra + '" height="' + hExt + '" fill="#C46A00"/>';
			svg += '<text x="' + (x + barra / 2) + '" y="' + (yBase - hBase - hInt - hExt - 4) + '" text-anchor="middle" font-size="11" font-weight="bold" fill="#1F2430">' + h.pedidos + '</text>';
		}
		svg += '<text x="' + (x + barra / 2) + '" y="' + (alto - mInf + 16) + '" text-anchor="middle" font-size="11" fill="#4a5164">' + ('0' + h.hora).slice(-2) + '</text>';
		svg += '</g>';
	}
	svg += '<text x="' + (mIzq + anchoUtil / 2) + '" y="' + (alto - 6) + '" text-anchor="middle" font-size="11" fill="#6C7482">Hora del reloj de la tienda</text>';
	svg += '</svg>';
	$('#rod-g-horas').html(svg);

	var html = '<table class="rod-tabla"><thead><tr><th>Hora</th><th>Pedidos que salieron</th><th>Salidas</th><th>Domiciliarios</th><th>En ruta</th><th>A tiempo</th><th>Tarde internos</th><th>Tarde externos</th></tr></thead><tbody>';
	var hayFilas = false;
	for (var j = 0; j < horas.length; j++) {
		var o = horas[j];
		if (o.pedidos === 0 && o.salidas === 0) {
			continue;
		}
		hayFilas = true;
		var pct = (o.medibles > 0) ? (o.a_tiempo * 100 / o.medibles) : null;
		html += '<tr><td>' + rodHoraTexto(o.hora) + '</td><td><b>' + o.pedidos + '</b></td><td>' + o.salidas + '</td><td>' + o.domiciliarios + '</td>'
			+ '<td>' + rodNum(o.prom_ruta) + ' min</td><td><span class="' + rodClasePct(pct) + '">' + rodPct(pct) + '</span></td>'
			+ '<td>' + (o.tarde_internos > 0 ? '<span class="rod-rojo">' + o.tarde_internos + '</span>' : o.tarde_internos) + '</td>'
			+ '<td>' + (o.tarde_externos > 0 ? '<span class="rod-naranja">' + o.tarde_externos + '</span>' : o.tarde_externos) + '</td></tr>';
	}
	html += '</tbody></table>';
	$('#rod-horas').html(hayFilas ? html : '');
}

/** Quien salio en cada hora: domiciliarios en filas, horas con salidas en columnas. */
function rodPintarMatriz()
{
	var d = rodDatos;
	var cuenta = {};
	var porHora = {};
	var maximo = 0;
	for (var s = 0; s < d.salidas.length; s++) {
		var sa = d.salidas[s];
		var h = sa.hora_del_dia;
		var clave = sa.id_domiciliario + '_' + h;
		cuenta[clave] = (cuenta[clave] || 0) + sa.pedidos.length;
		porHora[h] = true;
		if (cuenta[clave] > maximo) {
			maximo = cuenta[clave];
		}
	}
	var columnas = [];
	for (var i = 0; i < ROD_ORDEN_HORAS.length; i++) {
		if (porHora[ROD_ORDEN_HORAS[i]]) {
			columnas.push(ROD_ORDEN_HORAS[i]);
		}
	}
	if (columnas.length === 0) {
		$('#rod-matriz').html('<div class="rod-aviso rod-aviso-info">No hay salidas para mostrar.</div>');
		return;
	}
	var html = '<table class="rod-tabla"><thead><tr><th>Domiciliario</th>';
	for (var c = 0; c < columnas.length; c++) {
		html += '<th style="text-align:center">' + ('0' + columnas[c]).slice(-2) + '</th>';
	}
	html += '<th>Total</th></tr></thead><tbody>';
	for (var f = 0; f < d.domiciliarios.length; f++) {
		var o = d.domiciliarios[f];
		if (o.pedidos === 0) {
			continue;
		}
		html += '<tr><td><b>' + rodEscapar(o.nombre) + '</b> ' + rodChip(o.tipo === 'EXTERNO') + '</td>';
		for (var c2 = 0; c2 < columnas.length; c2++) {
			var n = cuenta[o.id + '_' + columnas[c2]] || 0;
			var intensidad = (maximo > 0) ? (n / maximo) : 0;
			var fondo = (n > 0) ? 'rgba(16,47,111,' + (0.15 + 0.75 * intensidad).toFixed(2) + ')' : '#F4F5F8';
			var texto = (n > 0 && intensidad > 0.5) ? '#FFFFFF' : '#1F2430';
			html += '<td class="rod-celda" style="background:' + fondo + ';color:' + texto + '">' + (n > 0 ? n : '') + '</td>';
		}
		html += '<td><b>' + o.pedidos + '</b></td></tr>';
	}
	html += '</tbody></table>';
	$('#rod-matriz').html(html);
}

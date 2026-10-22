/*
 * Reporte de uso del enrutamiento.
 *
 * Que porcentaje de los domicilios propios de cada tienda salen dentro de una
 * ruta sugerida, y a que horas entran y salen los pedidos. Las definiciones
 * exactas (que es "domicilio propio", que es "uso el enrutamiento", de donde sale
 * cada hora) estan en capaDAOCC.ReporteEnrutamientoDAO y se resumen al final de
 * la pantalla.
 *
 * Las funciones llevan el prefijo "rte" a proposito: otras pantallas del central
 * definen cargarTiendas y pintarResumen con el mismo nombre y se pisan entre si.
 *
 * Las graficas son SVG escrito a mano, sin librerias externas: la pantalla tiene
 * que abrir igual en una red de tienda que no llegue a un CDN.
 */

var server;

/** El dia operativo arranca hacia las 10 de la manana y termina de madrugada. */
var RTE_ORDEN_HORAS = [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

var rteDatos = null;
/** 0 = toda la red; si no, el idtienda escogido. */
var rteSeleccion = 0;

$(document).ready(function() {
	rteValidarVigenciaLogueo();
	rteCargarTiendas();
	rteFechasPorDefecto();
});

function rteValidarVigenciaLogueo()
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

function rteCargarTiendas()
{
	$.getJSON(server + 'GetTiendas', function(data){
		var combo = $('#idtienda');
		for(var i = 0; i < data.length; i++){
			combo.append($('<option>', { value: data[i].id, text: data[i].nombre }));
		}
	});
}

/** Arranca con la ultima semana, contando hoy: se quiere ver como van, no solo como iban. */
function rteFechasPorDefecto()
{
	var hasta = new Date();
	var desde = new Date(hasta.getTime());
	desde.setDate(desde.getDate() - 6);
	$('#fechainicial').val(rteATexto(desde));
	$('#fechafinal').val(rteATexto(hasta));
}

function rteATexto(fecha)
{
	var dia = ('0' + fecha.getDate()).slice(-2);
	var mes = ('0' + (fecha.getMonth() + 1)).slice(-2);
	return (dia + '/' + mes + '/' + fecha.getFullYear());
}

function rteConsultar()
{
	var fechaInicial = $('#fechainicial').val();
	var fechaFinal = $('#fechafinal').val();
	var idTienda = $('#idtienda').val();
	if(fechaInicial == '' || fechaFinal == '')
	{
		bootbox.alert('Seleccione la fecha inicial y la fecha final.');
		return;
	}
	$('#rte-calculando').show();
	$('#rte-boton-consultar').prop('disabled', true);
	$.getJSON(server + 'ConsultarReporteEnrutamiento?fechainicial=' + fechaInicial + '&fechafinal=' + fechaFinal
			+ '&idtienda=' + idTienda, function(data){
		if(data.error){
			bootbox.alert(rteEscapar(data.error));
			return;
		}
		rteDatos = data;
		rteSeleccion = 0;
		$('#rte-resultado').show();
		rtePintarTodo();
	}).fail(function(){
		//Sin esto el aviso de "consultando" se quedaria puesto y la pantalla
		//pareceria colgada.
		bootbox.alert('No se pudo consultar. Si el rango es largo puede haberse agotado el tiempo de espera; pruebe con menos d&iacute;as.');
	}).always(function(){
		$('#rte-calculando').hide();
		$('#rte-boton-consultar').prop('disabled', false);
	});
}

// ===========================================================================
// Utilidades
// ===========================================================================

function rteNumero(valor)
{
	return (Number(valor) || 0).toLocaleString('es-CO');
}

function rtePct(valor)
{
	return ((Math.round((Number(valor) || 0) * 10) / 10).toFixed(1).replace('.', ',') + '%');
}

function rteEscapar(texto)
{
	return $('<div>').text(texto === null || texto === undefined ? '' : texto).html();
}

function rteHH(hora)
{
	return (('0' + hora).slice(-2) + ':00');
}

/** Porcentaje que no miente: con total cero no hay porcentaje. */
function rteDe(parte, total)
{
	return (total > 0 ? (parte * 100.0 / total) : 0);
}

/** Una tienda cuenta para el porcentaje de la red si contesto y tiene el enrutamiento. */
function rteCuenta(t)
{
	return (t.estado == 'OK' && t.instalado);
}

function rteTienda(id)
{
	if(!rteDatos){ return null; }
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		if(rteDatos.tiendas[i].idtienda == id){ return rteDatos.tiendas[i]; }
	}
	return null;
}

// ===========================================================================
// Pintar
// ===========================================================================

function rtePintarTodo()
{
	rtePintarAvisos();
	rtePintarCabecera();
	rtePintarTiendas();
	rtePintarHoras();
	rtePintarDetalle();
}

/** Lo que el que mira tiene que saber antes de leer una cifra. */
function rtePintarAvisos()
{
	var html = '';
	var sinConexion = [], tardaron = [], conError = [], sinInstalar = [];
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		var t = rteDatos.tiendas[i];
		if(t.estado == 'SIN_CONEXION'){ sinConexion.push(t.tienda); }
		else if(t.estado == 'TARDO'){ tardaron.push(t.tienda); }
		else if(t.estado == 'ERROR'){ conError.push(t.tienda); }
		else if(!t.instalado){ sinInstalar.push(t.tienda); }
	}
	if(sinConexion.length > 0){
		html += '<div class="rte-aviso rte-aviso-alerta"><b>Sin conexi&oacute;n:</b> el computador de '
			+ rteEscapar(sinConexion.join(', ')) + ' no se encuentra en l&iacute;nea. No entra a las cifras ni se da por cero.</div>';
	}
	if(tardaron.length > 0){
		html += '<div class="rte-aviso rte-aviso-alerta"><b>No alcanz&oacute; a responder:</b> '
			+ rteEscapar(tardaron.join(', ')) + '. Pruebe con un rango m&aacute;s corto.</div>';
	}
	if(conError.length > 0){
		html += '<div class="rte-aviso rte-aviso-alerta"><b>Respondi&oacute; con un error:</b> '
			+ rteEscapar(conError.join(', ')) + '.</div>';
	}
	if(sinInstalar.length > 0){
		html += '<div class="rte-aviso rte-aviso-info"><b>Sin el enrutamiento instalado:</b> '
			+ rteEscapar(sinInstalar.join(', ')) + '. Se muestran sus pedidos, pero no entran al porcentaje de uso: '
			+ '&quot;no puede usarlo&quot; no es lo mismo que &quot;no lo usa&quot;.</div>';
	}
	$('#rte-avisos').html(html);
}

function rteDonut(pct, hayDatos)
{
	var r = 78;
	var c = 2 * Math.PI * r;
	var lleno = hayDatos ? c * pct / 100.0 : 0;
	var texto = hayDatos ? rtePct(pct) : '&mdash;';
	return ('<svg viewBox="0 0 200 200" width="200" height="200" role="img" aria-label="'
		+ (hayDatos ? rtePct(pct) + ' de los domicilios propios usaron el enrutamiento' : 'Sin datos') + '">'
		+ '<circle cx="100" cy="100" r="' + r + '" fill="none" stroke="#E3E8F1" stroke-width="26"/>'
		+ '<circle cx="100" cy="100" r="' + r + '" fill="none" stroke="#102F6F" stroke-width="26" '
		+ 'stroke-dasharray="' + lleno + ' ' + (c - lleno) + '" transform="rotate(-90 100 100)"/>'
		+ '<text x="100" y="104" text-anchor="middle" font-size="36" font-weight="700" fill="#102F6F">' + texto + '</text>'
		+ '<text x="100" y="126" text-anchor="middle" font-size="12" fill="#6C7482">usaron el enrutamiento</text>'
		+ '</svg>');
}

/** La cifra grande y las tarjetas, de la red o de la tienda escogida. */
function rtePintarCabecera()
{
	var d = rteSeleccion == 0 ? rteDatos.red : rteTienda(rteSeleccion);
	var hayDatos = true;
	var titulo = 'Toda la red';
	if(rteSeleccion != 0){
		titulo = d.tienda;
		hayDatos = rteCuenta(d) && d.pedidos > 0;
	} else {
		hayDatos = d.pedidos > 0;
	}
	var pct = hayDatos ? d.porcentaje : 0;
	var excluidos = d.excluidos_plataforma + d.excluidos_tercerizados;
	var html = '<div class="row">'
		+ '<div class="col-md-4"><div class="rte-tarjeta rte-hero"><div class="rte-rotulo">' + rteEscapar(titulo) + '</div>'
		+ rteDonut(pct, hayDatos) + '</div></div>'
		+ '<div class="col-md-8"><div class="row">'
		+ rteTarjeta('Domicilios propios', rteNumero(d.pedidos), 'en el rango, sin plataformas ni tercerizados', '')
		+ rteTarjeta('Con enrutamiento', rteNumero(d.enrutados), 'salieron en una ruta sugerida', 'rte-azul')
		+ rteTarjeta('Sin enrutamiento', rteNumero(d.no_enrutados), 'salieron sin ruta sugerida', '')
		+ rteTarjeta('Salidas registradas', rteNumero(d.salidas), rteNumero(d.sin_salida) + ' sin registro de salida', '')
		+ '</div>'
		+ '<p class="rte-pie">No entran al reporte: <b>' + rteNumero(d.excluidos_plataforma) + '</b> pedidos de plataformas '
		+ '(DiDi, Rappi y similares) y <b>' + rteNumero(d.excluidos_tercerizados) + '</b> domicilios tercerizados (Rappi Cargo)'
		+ (excluidos > 0 ? ', que lleva un tercero y no pueden usar el enrutamiento.' : '.') + '</p>'
		+ '</div></div>';
	$('#rte-cabecera').html(html);
}

function rteTarjeta(rotulo, cifra, pie, clase)
{
	return ('<div class="col-sm-6"><div class="rte-tarjeta ' + clase + '"><div class="rte-rotulo">' + rotulo + '</div>'
		+ '<div class="rte-cifra">' + cifra + '</div><div class="rte-pie">' + pie + '</div></div></div>');
}

/** Una barra por tienda: lo que se ve de un vistazo es quien lo usa y quien no. */
function rtePintarTiendas()
{
	var conDatos = [], sinUso = [], apagadas = [];
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		var t = rteDatos.tiendas[i];
		if(t.estado != 'OK'){ apagadas.push(t); }
		else if(!t.instalado){ sinUso.push(t); }
		else { conDatos.push(t); }
	}
	conDatos.sort(function(a, b){ return b.porcentaje - a.porcentaje; });
	var html = '';
	for(var j = 0; j < conDatos.length; j++){
		var f = conDatos[j];
		var sel = (rteSeleccion == f.idtienda) ? ' rte-fila-sel' : '';
		html += '<div class="rte-fila' + sel + '" data-id="' + f.idtienda + '" tabindex="0" role="button" '
			+ 'aria-label="Ver las horas de ' + rteEscapar(f.tienda) + '">'
			+ '<div class="rte-nombre">' + rteEscapar(f.tienda) + '</div>'
			+ '<div class="rte-pista" title="' + rteNumero(f.enrutados) + ' de ' + rteNumero(f.pedidos) + ' domicilios"><span class="rte-lleno" style="width:'
			+ Math.max(f.porcentaje, f.pedidos > 0 && f.enrutados > 0 ? 0.6 : 0) + '%"></span></div>'
			+ '<div class="rte-cifras"><b>' + (f.pedidos > 0 ? rtePct(f.porcentaje) : '&mdash;') + '</b>'
			+ '<span>' + rteNumero(f.enrutados) + ' de ' + rteNumero(f.pedidos) + '</span></div></div>';
	}
	for(var k = 0; k < sinUso.length; k++){
		var s = sinUso[k];
		html += '<div class="rte-fila rte-fila-gris" data-id="' + s.idtienda + '" tabindex="0" role="button">'
			+ '<div class="rte-nombre">' + rteEscapar(s.tienda) + '</div>'
			+ '<div class="rte-mensaje">Enrutamiento a&uacute;n no instalado en esta tienda &middot; ' + rteNumero(s.pedidos) + ' domicilios</div></div>';
	}
	for(var m = 0; m < apagadas.length; m++){
		var a = apagadas[m];
		var msg = 'El computador no se encuentra en l&iacute;nea';
		if(a.estado == 'TARDO'){ msg = 'No alcanz&oacute; a responder; pruebe con un rango m&aacute;s corto'; }
		else if(a.estado == 'ERROR'){ msg = 'La tienda respondi&oacute; con un error'; }
		html += '<div class="rte-fila rte-fila-apagada"><div class="rte-nombre">' + rteEscapar(a.tienda) + '</div>'
			+ '<div class="rte-mensaje"><span class="rte-punto-rojo"></span> Sin conexi&oacute;n &middot; ' + msg + '</div></div>';
	}
	if(html == ''){
		html = '<p class="rte-pie">No hay tiendas para mostrar.</p>';
	}
	$('#rte-tiendas').html(html);
	$('#rte-tiendas .rte-fila[data-id]').off('click keypress').on('click keypress', function(ev){
		if(ev.type == 'keypress' && ev.which != 13 && ev.which != 32){ return; }
		rteEscoger($(this).data('id'));
	});
}

function rteEscoger(id)
{
	rteSeleccion = (rteSeleccion == id ? 0 : id);
	rtePintarCabecera();
	rtePintarTiendas();
	rtePintarHoras();
}

/** Las horas de la red o de la tienda escogida. */
function rteHoras()
{
	return (rteSeleccion == 0 ? rteDatos.red.horas : rteTienda(rteSeleccion).horas);
}

/** Las 24 horas en orden operativo (10:00 a 09:00), sin los extremos vacios. */
function rteSecuencia(horas)
{
	var seq = [];
	for(var i = 0; i < RTE_ORDEN_HORAS.length; i++){ seq.push(horas[RTE_ORDEN_HORAS[i]]); }
	var ini = 0, fin = seq.length - 1;
	while(ini < fin && seq[ini].entradas == 0 && seq[ini].salidas == 0){ ini++; }
	while(fin > ini && seq[fin].entradas == 0 && seq[fin].salidas == 0){ fin--; }
	return seq.slice(ini, fin + 1);
}

function rtePintarHoras()
{
	var titulo = rteSeleccion == 0 ? 'toda la red' : rteEscapar(rteTienda(rteSeleccion).tienda);
	$('#rte-titulo-horas').html('Por hora del d&iacute;a &mdash; ' + titulo);
	if(rteSeleccion != 0){
		$('#rte-volver').show();
	} else {
		$('#rte-volver').hide();
	}
	var horas = rteHoras();
	var seq = rteSecuencia(horas);
	$('#rte-ideas').html(rteIdeas(horas));
	$('#rte-g-entradas').html(rteGraficaHoras(seq, 'entradas', 'entradas_enrutadas', 'Pedidos que entraron a la tienda, por hora'));
	$('#rte-g-salidas').html(rteGraficaHoras(seq, 'salidas', 'salidas_enrutadas', 'Pedidos que salieron a ruta, por hora'));
	$('#rte-calor').html(rteCalor(seq));
}

// ===========================================================================
// Las graficas
// ===========================================================================

/** Un tope de escala "redondo": 1, 2, 5 o 10 por una potencia de diez. */
function rteTopeEscala(maximo)
{
	if(maximo <= 4){ return 4; }
	var potencia = Math.pow(10, Math.floor(Math.log(maximo) / Math.LN10));
	var pasos = [1, 2, 4, 5, 8, 10];
	for(var i = 0; i < pasos.length; i++){
		if(maximo <= pasos[i] * potencia){ return pasos[i] * potencia; }
	}
	return 10 * potencia;
}

/**
 * Columnas apiladas por hora: abajo, en azul, lo que uso el enrutamiento; arriba,
 * en gris, el resto. Cada columna dice su cifra y, al pasar el mouse, el detalle.
 */
function rteGraficaHoras(seq, campoTotal, campoEnr, aria)
{
	var W = 760, H = 270, mi = 44, md = 12, ms = 18, mb = 34;
	var n = seq.length;
	var maximo = 0;
	for(var i = 0; i < n; i++){ maximo = Math.max(maximo, seq[i][campoTotal]); }
	if(maximo == 0){
		return '<p class="rte-pie">No hay pedidos con esta hora en el rango.</p>';
	}
	var tope = rteTopeEscala(maximo);
	var alto = H - ms - mb;
	var ancho = (W - mi - md) / n;
	var barra = Math.min(ancho * 0.7, 34);
	var s = '<svg viewBox="0 0 ' + W + ' ' + H + '" class="rte-svg" role="img" aria-label="' + aria + '">';
	for(var k = 0; k <= 4; k++){
		var y = ms + alto * (1 - k / 4);
		s += '<line x1="' + mi + '" x2="' + (W - md) + '" y1="' + y + '" y2="' + y + '" stroke="#E3E8F1"/>'
			+ '<text x="' + (mi - 7) + '" y="' + (y + 4) + '" text-anchor="end" font-size="10" fill="#6C7482">'
			+ Math.round(tope * k / 4) + '</text>';
	}
	for(var j = 0; j < n; j++){
		var d = seq[j];
		var total = d[campoTotal];
		var enr = d[campoEnr];
		var x = mi + ancho * j + (ancho - barra) / 2;
		var hT = alto * total / tope;
		var hE = alto * enr / tope;
		var base = H - mb;
		var detalle = rteHH(d.hora) + ' &middot; ' + rteNumero(total) + ' pedidos &middot; '
			+ rteNumero(enr) + ' con enrutamiento (' + (total > 0 ? rtePct(rteDe(enr, total)) : '0,0%') + ')';
		s += '<g><title>' + detalle + '</title>';
		if(total - enr > 0){
			s += '<rect x="' + x + '" y="' + (base - hT) + '" width="' + barra + '" height="' + (hT - hE) + '" fill="#C9D3E6" rx="3"/>';
		}
		if(enr > 0){
			s += '<rect x="' + x + '" y="' + (base - hE) + '" width="' + barra + '" height="' + hE + '" fill="#102F6F" rx="3"/>';
		}
		if(total > 0 && ancho >= 22){
			s += '<text x="' + (x + barra / 2) + '" y="' + (base - hT - 4) + '" text-anchor="middle" font-size="10" '
				+ 'font-weight="700" fill="#1F2430">' + rteNumero(total) + '</text>';
		}
		s += '<text x="' + (x + barra / 2) + '" y="' + (H - mb + 15) + '" text-anchor="middle" font-size="10.5" fill="#4a5164">'
			+ ('0' + d.hora).slice(-2) + '</text></g>';
	}
	s += '<line x1="' + mi + '" x2="' + (W - md) + '" y1="' + (H - mb) + '" y2="' + (H - mb) + '" stroke="#9aa3b8"/>';
	s += '</svg>';
	return s;
}

/** Del claro al oscuro segun el porcentaje: un solo color, para no inventar un "bueno" y un "malo". */
function rteColorUso(pct)
{
	var t = Math.max(0, Math.min(100, pct)) / 100.0;
	var a = [238, 242, 249], b = [16, 47, 111];
	var c = [];
	for(var i = 0; i < 3; i++){ c.push(Math.round(a[i] + (b[i] - a[i]) * t)); }
	return 'rgb(' + c.join(',') + ')';
}

/** Una celda por hora con el porcentaje de salidas que usaron el enrutamiento. */
function rteCalor(seq)
{
	var html = '<div class="rte-calor">';
	for(var i = 0; i < seq.length; i++){
		var d = seq[i];
		if(d.salidas == 0){
			html += '<div class="rte-celda rte-celda-vacia"><span class="rte-celda-hora">' + ('0' + d.hora).slice(-2)
				+ '</span><span class="rte-celda-pct">&ndash;</span><span class="rte-celda-n">0</span></div>';
			continue;
		}
		var pct = rteDe(d.salidas_enrutadas, d.salidas);
		var fondo = rteColorUso(pct);
		var texto = pct >= 50 ? '#FFFFFF' : '#102F6F';
		html += '<div class="rte-celda" style="background:' + fondo + ';color:' + texto + '" title="'
			+ rteHH(d.hora) + ' &middot; ' + rteNumero(d.salidas) + ' salidas &middot; ' + rteNumero(d.salidas_enrutadas)
			+ ' con enrutamiento"><span class="rte-celda-hora">' + ('0' + d.hora).slice(-2) + '</span>'
			+ '<span class="rte-celda-pct">' + Math.round(pct) + '%</span>'
			+ '<span class="rte-celda-n">' + rteNumero(d.salidas) + '</span></div>';
	}
	return (html + '</div>');
}

/** Lo que dicen las horas, en frases, para quien no quiere leer una grafica. */
function rteIdeas(horas)
{
	var totalSalidas = 0;
	var i;
	for(i = 0; i < 24; i++){ totalSalidas += horas[i].salidas; }
	if(totalSalidas == 0){
		return '';
	}
	var conSalida = [];
	for(i = 0; i < 24; i++){ if(horas[i].salidas > 0){ conSalida.push(horas[i]); } }
	conSalida.sort(function(a, b){ return b.salidas - a.salidas; });
	var pico = conSalida[0];
	var items = [];
	items.push('La hora con m&aacute;s salidas es la de las <b>' + rteHH(pico.hora) + '</b>: ' + rteNumero(pico.salidas)
		+ ' pedidos (' + rtePct(rteDe(pico.salidas, totalSalidas)) + ' de todas las salidas), y en ella el enrutamiento se us&oacute; en '
		+ rtePct(rteDe(pico.salidas_enrutadas, pico.salidas)) + '.');
	var top = conSalida.slice(0, 3);
	if(top.length >= 2){
		var sumaS = 0, sumaE = 0, nombres = [];
		for(i = 0; i < top.length; i++){
			sumaS += top[i].salidas;
			sumaE += top[i].salidas_enrutadas;
			nombres.push(rteHH(top[i].hora));
		}
		items.push('En las ' + top.length + ' horas de m&aacute;s salida (' + nombres.join(', ') + ') sale el <b>'
			+ rtePct(rteDe(sumaS, totalSalidas)) + '</b> de los domicilios, y el enrutamiento se us&oacute; en <b>'
			+ rtePct(rteDe(sumaE, sumaS)) + '</b> de ellos.');
	}
	//Mejor y peor uso, solo entre las horas con volumen: con tres pedidos un
	//porcentaje no dice nada.
	var conVolumen = [];
	for(i = 0; i < conSalida.length; i++){
		if(conSalida[i].salidas >= Math.max(5, totalSalidas * 0.04)){ conVolumen.push(conSalida[i]); }
	}
	if(conVolumen.length >= 2){
		var mejor = conVolumen[0], peor = conVolumen[0];
		for(i = 1; i < conVolumen.length; i++){
			if(rteDe(conVolumen[i].salidas_enrutadas, conVolumen[i].salidas) > rteDe(mejor.salidas_enrutadas, mejor.salidas)){ mejor = conVolumen[i]; }
			if(rteDe(conVolumen[i].salidas_enrutadas, conVolumen[i].salidas) < rteDe(peor.salidas_enrutadas, peor.salidas)){ peor = conVolumen[i]; }
		}
		items.push('Entre las horas con volumen, donde <b>m&aacute;s</b> se usa es a las ' + rteHH(mejor.hora) + ' ('
			+ rtePct(rteDe(mejor.salidas_enrutadas, mejor.salidas)) + ') y donde <b>menos</b>, a las ' + rteHH(peor.hora) + ' ('
			+ rtePct(rteDe(peor.salidas_enrutadas, peor.salidas)) + ').');
	}
	var html = '<ul class="rte-ideas">';
	for(i = 0; i < items.length; i++){ html += '<li>' + items[i] + '</li>'; }
	return (html + '</ul>');
}

// ===========================================================================
// El detalle
// ===========================================================================

function rtePintarDetalle()
{
	var html = '<table class="table"><thead><tr><th>Tienda</th><th>Domicilios propios</th><th>Con enrutamiento</th>'
		+ '<th>Sin enrutamiento</th><th>% con enrutamiento</th><th>Salidas registradas</th><th>Sin salida registrada</th></tr></thead><tbody>';
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		var t = rteDatos.tiendas[i];
		if(t.estado != 'OK'){
			html += '<tr class="rte-det-gris"><td>' + rteEscapar(t.tienda) + '</td><td colspan="6">'
				+ (t.estado == 'SIN_CONEXION' ? 'El computador de la tienda no se encuentra en l&iacute;nea'
					: (t.estado == 'TARDO' ? 'No alcanz&oacute; a responder' : 'Respondi&oacute; con un error')) + '</td></tr>';
			continue;
		}
		html += '<tr><td>' + rteEscapar(t.tienda) + '</td><td>' + rteNumero(t.pedidos) + '</td><td>'
			+ (t.instalado ? rteNumero(t.enrutados) : '&mdash;') + '</td><td>'
			+ (t.instalado ? rteNumero(t.no_enrutados) : '&mdash;') + '</td><td><b>'
			+ (t.instalado && t.pedidos > 0 ? rtePct(t.porcentaje) : '&mdash;') + '</b>'
			+ (t.instalado ? '' : ' <span class="rte-pie">sin instalar</span>') + '</td><td>'
			+ rteNumero(t.salidas) + '</td><td>' + rteNumero(t.sin_salida) + '</td></tr>';
	}
	var r = rteDatos.red;
	html += '</tbody><tfoot><tr class="rte-total"><td>Total (tiendas que cuentan: ' + r.tiendas_en_la_cuenta + ')</td><td>'
		+ rteNumero(r.pedidos) + '</td><td>' + rteNumero(r.enrutados) + '</td><td>' + rteNumero(r.no_enrutados) + '</td><td>'
		+ (r.pedidos > 0 ? rtePct(r.porcentaje) : '&mdash;') + '</td><td>' + rteNumero(r.salidas) + '</td><td>'
		+ rteNumero(r.sin_salida) + '</td></tr></tfoot></table>';
	$('#rte-detalle').html(html);
}

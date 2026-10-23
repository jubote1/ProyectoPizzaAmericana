/*
 * Reporte de uso del enrutamiento.
 *
 * Que porcentaje de los domicilios propios de cada tienda salen dentro de una
 * ruta sugerida, a que horas entran y salen los pedidos, y cuantos pedidos
 * enrutados SOLTARON los domiciliarios. Las definiciones exactas (que es
 * "domicilio propio", que es "uso el enrutamiento", que es "soltado", de donde
 * sale cada hora) estan en capaDAOCC.ReporteEnrutamientoDAO y se resumen al final
 * de la pantalla.
 *
 * Una tienda que no contesta se lee del datamart, que llega hasta ayer. Esas
 * tiendas salen marcadas, y una casilla deja escoger si se suman o no a las
 * cifras de la red: sumarlas mezcla "hasta hoy" con "hasta ayer".
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
/** Si la red suma tambien las tiendas leidas del datamart. */
var rteIncluirDatamart = true;

$(document).ready(function() {
	rteValidarVigenciaLogueo();
	rteCargarTiendas();
	rteFechasPorDefecto();
	//La casilla vive dentro de un aviso que se vuelve a pintar; por eso el evento
	//se cuelga del documento y no de la casilla.
	$(document).on('change', '#rte-incluir-dm', function(){
		rteIncluirDatamart = $(this).is(':checked');
		rtePintarTodo();
	});
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
		rteIncluirDatamart = true;
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

/** De yyyy-MM-dd (o con hora) a dd/MM/yyyy. */
function rteFecha(texto)
{
	if(!texto || texto.length < 10){ return ''; }
	return (texto.substring(8, 10) + '/' + texto.substring(5, 7) + '/' + texto.substring(0, 4));
}

/** dd/MM HH:mm, para el detalle de soltados. */
function rteFechaHora(texto)
{
	if(!texto || texto.length < 16){ return rteEscapar(texto); }
	return (texto.substring(8, 10) + '/' + texto.substring(5, 7) + ' ' + texto.substring(11, 16));
}

/** Porcentaje que no miente: con total cero no hay porcentaje. */
function rteDe(parte, total)
{
	return (total > 0 ? (parte * 100.0 / total) : 0);
}

function rteEsDatamart(t)
{
	return (t.fuente == 'DATAMART' && t.estado == 'OK');
}

/** Una tienda cuenta para el porcentaje de la red si contesto y tiene el enrutamiento. */
function rteCuenta(t)
{
	return (t.estado == 'OK' && t.instalado && (!rteEsDatamart(t) || rteIncluirDatamart));
}

/** Los totales de la red: con o sin las tiendas leidas del datamart, segun la casilla. */
function rteRed()
{
	return ((rteDatos.hay_datamart && rteIncluirDatamart) ? rteDatos.red_con_datamart : rteDatos.red);
}

function rteTienda(id)
{
	if(!rteDatos){ return null; }
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		if(rteDatos.tiendas[i].idtienda == id){ return rteDatos.tiendas[i]; }
	}
	return null;
}

/** La etiqueta que dice que las cifras de una tienda vienen del datamart. */
function rteEtiquetaDatamart(t)
{
	return ('<span class="rte-tag" title="El computador de la tienda no contest&oacute;: estas cifras salen del datamart, que se llena de madrugada con el d&iacute;a anterior y no incluye hoy.">Datamart &middot; hasta '
		+ rteFecha(t.datos_hasta).substring(0, 5) + '</span>');
}

// ===========================================================================
// Pintar
// ===========================================================================

function rtePintarTodo()
{
	rtePintarAvisos();
	rtePintarCabecera();
	rtePintarTiendas();
	rtePintarSoltados();
	rtePintarHoras();
	rtePintarDetalle();
}

/** Lo que el que mira tiene que saber antes de leer una cifra. */
function rtePintarAvisos()
{
	var html = '';
	var sinConexion = [], tardaron = [], conError = [], sinInstalar = [], datamart = [], notaDatamart = [];
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		var t = rteDatos.tiendas[i];
		if(rteEsDatamart(t)){
			datamart.push(rteEscapar(t.tienda) + ' (hasta el ' + rteFecha(t.datos_hasta) + ')');
		}
		else if(t.estado == 'SIN_CONEXION'){
			sinConexion.push(rteEscapar(t.tienda));
			if(t.datos_hasta){ notaDatamart.push('El datamart de ' + rteEscapar(t.tienda) + ' llega hasta el ' + rteFecha(t.datos_hasta) + ', antes del rango pedido.'); }
		}
		else if(t.estado == 'TARDO'){ tardaron.push(rteEscapar(t.tienda)); }
		else if(t.estado == 'ERROR'){ conError.push(rteEscapar(t.tienda)); }
		else if(!t.instalado){ sinInstalar.push(rteEscapar(t.tienda)); }
	}
	if(sinConexion.length > 0){
		html += '<div class="rte-aviso rte-aviso-alerta"><b>Sin conexi&oacute;n:</b> el computador de '
			+ sinConexion.join(', ') + ' no se encuentra en l&iacute;nea y el datamart no tiene datos para el rango. ' + notaDatamart.join(' ')
			+ ' No entra a las cifras ni se da por cero.</div>';
	}
	if(tardaron.length > 0){
		html += '<div class="rte-aviso rte-aviso-alerta"><b>No alcanz&oacute; a responder:</b> '
			+ tardaron.join(', ') + '. Pruebe con un rango m&aacute;s corto.</div>';
	}
	if(conError.length > 0){
		html += '<div class="rte-aviso rte-aviso-alerta"><b>Respondi&oacute; con un error:</b> '
			+ conError.join(', ') + '.</div>';
	}
	if(datamart.length > 0){
		html += '<div class="rte-aviso rte-aviso-info"><b>Le&iacute;do del datamart:</b> el computador de '
			+ datamart.join('; ') + ' no contest&oacute;, as&iacute; que se muestran sus cifras del datamart. Esa informaci&oacute;n llega hasta ayer: '
			+ '<b>no incluye hoy</b>.<br><label style="margin:6px 0 0;font-weight:normal"><input type="checkbox" id="rte-incluir-dm"'
			+ (rteIncluirDatamart ? ' checked' : '') + '> Sumar esas tiendas a las cifras de la red '
			+ '(si se suman, la red mezcla datos hasta hoy con datos hasta ayer).</label></div>';
	}
	if(sinInstalar.length > 0){
		html += '<div class="rte-aviso rte-aviso-info"><b>Sin el enrutamiento instalado:</b> '
			+ sinInstalar.join(', ') + '. Se muestran sus pedidos, pero no entran al porcentaje de uso: '
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
	var d = rteSeleccion == 0 ? rteRed() : rteTienda(rteSeleccion);
	var hayDatos = true;
	var titulo = 'Toda la red';
	var nota = '';
	if(rteSeleccion != 0){
		titulo = d.tienda;
		hayDatos = d.estado == 'OK' && d.instalado && d.pedidos > 0;
		if(rteEsDatamart(d)){
			nota = '<div class="rte-pie" style="margin-top:4px">' + rteEtiquetaDatamart(d) + '</div>';
		}
	} else {
		hayDatos = d.pedidos > 0;
		if(rteDatos.hay_datamart && rteIncluirDatamart){
			nota = '<div class="rte-pie" style="margin-top:4px">Incluye tiendas del datamart (hasta ayer)</div>';
		}
	}
	var pct = hayDatos ? d.porcentaje : 0;
	var excluidos = d.excluidos_plataforma + d.excluidos_tercerizados;
	var html = '<div class="row">'
		+ '<div class="col-md-4"><div class="rte-tarjeta rte-hero"><div class="rte-rotulo">' + rteEscapar(titulo) + '</div>'
		+ rteDonut(pct, hayDatos) + nota + '</div></div>'
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
		var dm = rteEsDatamart(f);
		var tenue = (dm && !rteIncluirDatamart) ? ' rte-fila-tenue' : '';
		html += '<div class="rte-fila' + sel + tenue + '" data-id="' + f.idtienda + '" tabindex="0" role="button" '
			+ 'aria-label="Ver las horas de ' + rteEscapar(f.tienda) + '">'
			+ '<div class="rte-nombre">' + rteEscapar(f.tienda) + (dm ? '<br>' + rteEtiquetaDatamart(f) : '') + '</div>'
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
	rtePintarSoltados();
	rtePintarHoras();
}

// ===========================================================================
// Los pedidos soltados
// ===========================================================================

/**
 * Que paso con los pedidos que el sistema le enruto a un domiciliario. Las
 * categorias no se traslapan y suman lo enrutado: llevados por la ruta, salieron
 * LIBRES (la ruta no se acepto y el pedido salio por fuera), soltados por el
 * domiciliario, movidos o anulados por un administrador, y lo que expiro sin
 * salir o sigue en curso.
 *
 * Lo esperado es que casi todo sea "llevado". Lo que NO se lleva por la ruta se
 * divide en dos: el que el domiciliario suelta y el que ignora (sale libre sin
 * aceptar ni soltar). El segundo no deja huella en el POS mas que en este reporte.
 */
var RTE_COLOR_LLEVADOS = '#102F6F';
var RTE_COLOR_SOLTADOS = '#E42528';
var RTE_COLOR_LIBRES = '#E8890C';
var RTE_COLOR_LIBRES_SIN_DUENO = '#F2B05E';
var RTE_COLOR_LIBRES_ASIGNADO = '#F7D4A3';
var RTE_COLOR_ADMIN = '#9AA3B8';
var RTE_COLOR_TARDIAS = '#7B6CA8';
var RTE_COLOR_PENDIENTES = '#D5DAE5';

/** Las cinco porciones de lo enrutado, en el orden en que se pintan. */
function rteSegmentos(g)
{
	return [
		{ clave: 'llevados', nombre: 'Llevados por la ruta', valor: g.llevados, color: RTE_COLOR_LLEVADOS },
		{ clave: 'libres_otro', nombre: 'Salieron con otro domiciliario', valor: (g.libres_otro || 0), color: RTE_COLOR_LIBRES },
		{ clave: 'libres_sin_dueno', nombre: 'Salieron libres: ruta sin due&ntilde;o (turno de llegada)', valor: (g.libres_sin_dueno || 0), color: RTE_COLOR_LIBRES_SIN_DUENO },
		{ clave: 'libres_asignado', nombre: 'Salieron con el asignado sin aceptar la ruta', valor: (g.libres_asignado || 0), color: RTE_COLOR_LIBRES_ASIGNADO },
		{ clave: 'soltados', nombre: 'Soltados por el domiciliario', valor: g.soltados, color: RTE_COLOR_SOLTADOS },
		{ clave: 'tardias', nombre: 'Ruta creada cuando el pedido ya habia salido', valor: (g.rutas_tardias || 0), color: RTE_COLOR_TARDIAS },
		{ clave: 'admin', nombre: 'Movidos o anulados por un administrador', valor: g.reasignados + g.anulados, color: RTE_COLOR_ADMIN },
		{ clave: 'pendientes', nombre: 'Expiraron sin salir o siguen en curso', valor: g.expiradas_sin_salir + g.en_curso + g.otros, color: RTE_COLOR_PENDIENTES }
	];
}

/** Una barra apilada: cada porcion mide lo que pesa dentro de lo enrutado. */
function rteBarraDesglose(g)
{
	var segs = rteSegmentos(g);
	var total = g.sugeridos;
	var html = '<div class="rte-pista rte-apilada" role="img" aria-label="Que paso con los pedidos enrutados">';
	for(var i = 0; i < segs.length; i++){
		if(segs[i].valor > 0 && total > 0){
			html += '<span title="' + segs[i].nombre + ': ' + rteNumero(segs[i].valor) + ' (' + rtePct(rteDe(segs[i].valor, total)) + ')" style="width:'
				+ (segs[i].valor * 100.0 / total) + '%;background:' + segs[i].color + '"></span>';
		}
	}
	return (html + '</div>');
}

function rteLeyendaDesglose(g)
{
	var segs = rteSegmentos(g);
	var html = '<div class="rte-leyenda">';
	for(var i = 0; i < segs.length; i++){
		html += '<span class="rte-leyenda-item"><i style="background:' + segs[i].color + '"></i>' + segs[i].nombre + ': <b>' + rteNumero(segs[i].valor) + '</b></span>';
	}
	return (html + '</div>');
}

function rtePintarSoltados()
{
	var titulo = rteSeleccion == 0 ? 'toda la red' : rteEscapar(rteTienda(rteSeleccion).tienda);
	$('#rte-titulo-soltados').html('Qu&eacute; pas&oacute; con los pedidos enrutados &mdash; ' + titulo);
	var s = (rteSeleccion == 0 ? rteRed().soltados : rteTienda(rteSeleccion).soltados);
	if(!s || !s.disponible){
		$('#rte-soltados').html('<p class="rte-pie">No se pudo leer el enrutamiento de ' + (rteSeleccion == 0 ? 'ninguna tienda' : 'esta tienda')
			+ ' en el rango (las tablas de sugerencias no est&aacute;n disponibles).</p>');
		return;
	}
	var noCumplido = s.soltados + s.libres;
	var html = '<div class="row">'
		+ rteTarjeta4('Pedidos enrutados', rteNumero(s.sugeridos), 'sugeridos a un domiciliario en el rango', '')
		+ rteTarjeta4('Llevados por la ruta', rteNumero(s.llevados), rtePct(s.porcentaje_llevados) + ' de los enrutados', 'rte-azul')
		+ rteTarjeta4('Salieron libres', rteNumero(s.libres), s.libres > 0
			? rteNumero(s.libres_otro || 0) + ' con otro domiciliario &middot; ' + rteNumero(s.libres_sin_dueno || 0) + ' sin due&ntilde;o &middot; ' + rteNumero(s.libres_asignado || 0) + ' con el asignado'
			: (s.sugeridos > 0 ? rtePct(rteDe(s.libres, s.sugeridos)) + ' de los enrutados' : '&nbsp;'), s.libres > 0 ? 'rte-naranja' : 'rte-verde')
		+ rteTarjeta4('Soltados', rteNumero(s.soltados), s.sugeridos > 0 ? rtePct(s.porcentaje) + ' de los enrutados' : '&nbsp;', s.soltados > 0 ? 'rte-rojo' : 'rte-verde')
		+ '</div>';

	//La cuenta completa, para que nada quede sin explicar.
	html += rteBarraDesglose(s) + rteLeyendaDesglose(s);
	html += '<p class="rte-pie" style="margin-top:8px">Rutas creadas: <b>' + rteNumero(s.sugerencias) + '</b> &middot; aceptadas: <b>' + rteNumero(s.sugerencias_aceptadas)
		+ '</b> (' + rtePct(s.porcentaje_aceptacion) + ') &middot; vencidas sin aceptar: <b>' + rteNumero(s.sugerencias_expiradas) + '</b>.'
		+ ' Movidos por un administrador: <b>' + rteNumero(s.reasignados) + '</b> &middot; anulados por un administrador: <b>' + rteNumero(s.anulados)
		+ '</b> &middot; ruta creada tarde: <b>' + rteNumero(s.rutas_tardias || 0) + '</b> &middot; expiraron sin salir: <b>' + rteNumero(s.expiradas_sin_salir) + '</b> &middot; en curso: <b>' + rteNumero(s.en_curso) + '</b>.</p>';

	if(noCumplido == 0){
		html += '<p class="rte-bien">Ning&uacute;n pedido enrutado se solt&oacute; ni sali&oacute; por fuera de su ruta en el rango.</p>';
	} else {
		html += '<p class="rte-pie"><b>Salieron libres:</b> la ruta ya exist&iacute;a, nadie la acept&oacute; ni la solt&oacute;, y el pedido sali&oacute; igual por fuera de ella. '
			+ 'Se separan en tres: <b>con otro domiciliario</b> (la ruta ten&iacute;a due&ntilde;o y el pedido se fue con alguien m&aacute;s), '
			+ '<b>ruta sin due&ntilde;o</b> (era por turno de llegada y nadie la hab&iacute;a tomado) y <b>con el asignado</b> (sali&oacute; con el domiciliario de la ruta, que nunca la acept&oacute;). '
			+ '<b>Soltados:</b> el domiciliario solt&oacute; el pedido y volvi&oacute; a quedar libre para cualquiera. '
			+ 'En los dos casos la ruta no se cumpli&oacute;; lo esperado es que ambos sean cero.</p>';
	}

	//Por tienda: solo en la vista de toda la red. Una barra apilada por tienda, del
	//tamano de lo que esa tienda enruto.
	if(rteSeleccion == 0){
		var filas = [];
		for(var i = 0; i < rteDatos.tiendas.length; i++){
			var t = rteDatos.tiendas[i];
			if(t.estado == 'OK' && t.soltados.disponible && (!rteEsDatamart(t) || rteIncluirDatamart)){ filas.push(t); }
		}
		filas.sort(function(a, b){ return b.soltados.porcentaje_no_cumplido - a.soltados.porcentaje_no_cumplido || b.soltados.sugeridos - a.soltados.sugeridos; });
		if(filas.length > 0){
			html += '<h5 class="rte-subseccion">Por tienda: qu&eacute; tanto se cumple la ruta</h5>';
			for(var k = 0; k < filas.length; k++){
				var f = filas[k];
				var sf = f.soltados;
				html += '<div class="rte-fila" data-id="' + f.idtienda + '" tabindex="0" role="button">'
					+ '<div class="rte-nombre">' + rteEscapar(f.tienda) + (rteEsDatamart(f) ? '<br>' + rteEtiquetaDatamart(f) : '') + '</div>'
					+ rteBarraDesglose(sf)
					+ '<div class="rte-cifras"><b>' + (sf.sugeridos > 0 ? rtePct(sf.porcentaje_llevados) : '&mdash;') + '</b>'
					+ '<span>' + (sf.sugeridos > 0 ? rteNumero(sf.llevados) + ' de ' + rteNumero(sf.sugeridos) + ' llevados' : 'sin enrutados') + '</span></div></div>';
			}
			html += '<p class="rte-pie">Cada barra es lo que esa tienda enrut&oacute;. Azul: llevado por la ruta; naranjas: sali&oacute; libre (oscuro: con otro domiciliario; medio: ruta sin due&ntilde;o; claro: con el asignado sin aceptar); rojo: soltado; morado: la ruta se cre&oacute; cuando el pedido ya hab&iacute;a salido; gris: lo movi&oacute; o anul&oacute; un administrador; claro: expir&oacute; sin salir o sigue en curso.</p>';
		}
	}

	//Quien no cumplio la ruta.
	if(s.por_domiciliario && s.por_domiciliario.length > 0){
		html += '<h5 class="rte-subseccion">Qui&eacute;n no llev&oacute; lo que se le enrut&oacute;</h5><div class="table-responsive"><table class="table rte-tabla"><thead><tr>'
			+ '<th>Domiciliario</th><th>Tienda</th><th>Enrutados</th><th>Llevados</th><th>Salieron libres</th><th>De ellos, con otro domiciliario</th><th>Soltados</th><th>% no cumplido</th></tr></thead><tbody>';
		for(var m = 0; m < s.por_domiciliario.length; m++){
			var p = s.por_domiciliario[m];
			html += '<tr><td>' + rteEscapar(p.domiciliario) + '</td><td>' + rteEscapar(p.tienda) + '</td><td>' + rteNumero(p.sugeridos)
				+ '</td><td>' + rteNumero(p.llevados) + '</td><td>' + (p.libres > 0 ? '<b class="rte-txt-naranja">' + rteNumero(p.libres) + '</b>' : '0')
				+ '</td><td>' + rteNumero(p.libres_otro || 0)
				+ '</td><td>' + (p.soltados > 0 ? '<b class="rte-txt-rojo">' + rteNumero(p.soltados) + '</b>' : '0')
				+ '</td><td><b>' + rtePct(p.porcentaje_no_cumplido) + '</b></td></tr>';
		}
		html += '</tbody></table></div>';
	}

	//Los pedidos soltados, con el motivo que dio cada domiciliario.
	if(s.detalle && s.detalle.length > 0){
		html += '<details class="rte-detalle"><summary>Ver los pedidos soltados y el motivo que dio cada domiciliario</summary>'
			+ '<div class="table-responsive"><table class="table rte-tabla"><thead><tr><th>Cu&aacute;ndo</th><th>Tienda</th><th>Domiciliario</th>'
			+ '<th>Pedido</th><th>Motivo</th></tr></thead><tbody>';
		for(var n = 0; n < s.detalle.length; n++){
			var x = s.detalle[n];
			html += '<tr><td>' + rteFechaHora(x.fecha_hora) + '</td><td>' + rteEscapar(x.tienda) + '</td><td>' + rteEscapar(x.domiciliario)
				+ '</td><td>' + rteNumero(x.idpedido) + '</td><td style="text-align:left">' + (x.motivo ? rteEscapar(x.motivo) : '<span class="rte-pie">sin motivo</span>') + '</td></tr>';
		}
		html += '</tbody></table></div></details>';
	}
	$('#rte-soltados').html(html);
	$('#rte-soltados .rte-fila[data-id]').off('click keypress').on('click keypress', function(ev){
		if(ev.type == 'keypress' && ev.which != 13 && ev.which != 32){ return; }
		rteEscoger($(this).data('id'));
	});
}

function rteTarjeta4(rotulo, cifra, pie, clase)
{
	return ('<div class="col-sm-6 col-lg-3"><div class="rte-tarjeta ' + clase + '"><div class="rte-rotulo">' + rotulo + '</div>'
		+ '<div class="rte-cifra">' + cifra + '</div><div class="rte-pie">' + pie + '</div></div></div>');
}

// ===========================================================================
// Las horas
// ===========================================================================

/** Las horas de la red o de la tienda escogida. */
function rteHoras()
{
	return (rteSeleccion == 0 ? rteRed().horas : rteTienda(rteSeleccion).horas);
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
	var html = '<table class="table"><thead><tr><th>Tienda</th><th>Fuente</th><th>Domicilios propios</th><th>Con enrutamiento</th>'
		+ '<th>Sin enrutamiento</th><th>% con enrutamiento</th><th>Soltados / libres</th><th>Salidas registradas</th><th>Sin salida registrada</th></tr></thead><tbody>';
	for(var i = 0; i < rteDatos.tiendas.length; i++){
		var t = rteDatos.tiendas[i];
		if(t.estado != 'OK'){
			html += '<tr class="rte-det-gris"><td>' + rteEscapar(t.tienda) + '</td><td colspan="8">'
				+ (t.estado == 'SIN_CONEXION' ? 'El computador de la tienda no se encuentra en l&iacute;nea'
					: (t.estado == 'TARDO' ? 'No alcanz&oacute; a responder' : 'Respondi&oacute; con un error')) + '</td></tr>';
			continue;
		}
		var dm = rteEsDatamart(t);
		html += '<tr><td>' + rteEscapar(t.tienda) + '</td><td style="text-align:left">'
			+ (dm ? 'Datamart, hasta ' + rteFecha(t.datos_hasta).substring(0, 5) : 'En vivo') + '</td><td>' + rteNumero(t.pedidos) + '</td><td>'
			+ (t.instalado ? rteNumero(t.enrutados) : '&mdash;') + '</td><td>'
			+ (t.instalado ? rteNumero(t.no_enrutados) : '&mdash;') + '</td><td><b>'
			+ (t.instalado && t.pedidos > 0 ? rtePct(t.porcentaje) : '&mdash;') + '</b>'
			+ (t.instalado ? '' : ' <span class="rte-pie">sin instalar</span>') + '</td><td>'
			+ (t.soltados.disponible ? rteNumero(t.soltados.soltados) + ' / ' + rteNumero(t.soltados.libres) : '&mdash;') + '</td><td>'
			+ rteNumero(t.salidas) + '</td><td>' + rteNumero(t.sin_salida) + '</td></tr>';
	}
	var r = rteRed();
	html += '</tbody><tfoot><tr class="rte-total"><td>Total (tiendas que cuentan: ' + r.tiendas_en_la_cuenta + ')</td><td></td><td>'
		+ rteNumero(r.pedidos) + '</td><td>' + rteNumero(r.enrutados) + '</td><td>' + rteNumero(r.no_enrutados) + '</td><td>'
		+ (r.pedidos > 0 ? rtePct(r.porcentaje) : '&mdash;') + '</td><td>'
		+ (r.soltados && r.soltados.disponible ? rteNumero(r.soltados.soltados) + ' / ' + rteNumero(r.soltados.libres) : '&mdash;') + '</td><td>' + rteNumero(r.salidas) + '</td><td>'
		+ rteNumero(r.sin_salida) + '</td></tr></tfoot></table>';
	$('#rte-detalle').html(html);
}

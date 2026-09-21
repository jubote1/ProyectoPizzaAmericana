/*
 * Tablero de desempeno de un domiciliario.
 *
 * Las graficas van dibujadas a mano en SVG. El proyecto no trae ninguna
 * libreria de graficas -se revisaron los 93 archivos de js/- y traer una de un
 * CDN pondria la pantalla a depender de internet para dibujar cuatro barras.
 *
 * Todo en ES5 -var y function, nada de arrow ni let-: la pantalla convive con
 * jQuery 1.11 y con los equipos de las tiendas.
 */

var ddDatos = null;

$(document).ready(function () {

	//Por defecto el ultimo mes: es el rango en que un domiciliario ya tiene
	//suficientes pedidos para que los promedios digan algo.
	var hoy = new Date();
	var hace30 = new Date();
	hace30.setDate(hoy.getDate() - 30);
	$('#ddHasta').val(ddFechaISO(hoy));
	$('#ddDesde').val(ddFechaISO(hace30));

	ddCargarDomiciliarios();

	$('#ddConsultar').click(function () {
		ddConsultar();
	});
});

function ddFechaISO(fecha) {
	var mes = fecha.getMonth() + 1;
	var dia = fecha.getDate();
	return (fecha.getFullYear() + '-' + (mes < 10 ? '0' : '') + mes + '-' + (dia < 10 ? '0' : '') + dia);
}

function ddEscapar(texto) {
	return ($('<div/>').text(texto === null || texto === undefined ? '' : texto).html());
}

function ddCargarDomiciliarios() {
	$.ajax({
		url: server + 'ConsultarDomiciliariosActivos',
		dataType: 'json',
		type: 'get',
		success: function (data) {
			var lista = data.domiciliarios || [];
			var html = '<option value="">Seleccione un domiciliario</option>';
			for (var i = 0; i < lista.length; i++) {
				html += '<option value="' + lista[i].id + '">' + ddEscapar(lista[i].nombrelargo) +
					' (' + ddEscapar(lista[i].nombre) + ')</option>';
			}
			$('#ddDomiciliario').html(html);
			if (lista.length === 0) {
				ddMensaje('warning', 'No hay empleados activos marcados como domiciliarios.');
			}
		},
		error: function () {
			$('#ddDomiciliario').html('<option value="">No se pudo cargar la lista</option>');
			ddMensaje('danger', 'No se pudo cargar la lista de domiciliarios.');
		}
	});
}

function ddMensaje(tipo, texto) {
	$('#ddMensaje').html('<div class="alert alert-' + tipo + '">' + texto + '</div>');
}

function ddConsultar() {
	var idEmpleado = $('#ddDomiciliario').val();
	var desde = $('#ddDesde').val();
	var hasta = $('#ddHasta').val();

	if (!idEmpleado) {
		ddMensaje('warning', 'Seleccione un domiciliario.');
		return;
	}
	if (!desde || !hasta) {
		ddMensaje('warning', 'Indique el rango de fechas.');
		return;
	}
	if (desde > hasta) {
		ddMensaje('warning', 'La fecha inicial es posterior a la final.');
		return;
	}

	$('#ddMensaje').html('');
	$('#ddResultado').hide();
	$('#ddCargando').show();
	$('#ddConsultar').prop('disabled', true);

	$.ajax({
		url: server + 'ConsultarDesempenoDomiciliario',
		data: { idempleado: idEmpleado, fechadesde: desde, fechahasta: hasta },
		dataType: 'json',
		type: 'get',
		success: function (data) {
			$('#ddCargando').hide();
			$('#ddConsultar').prop('disabled', false);
			if (data.error) {
				ddMensaje('danger', ddEscapar(data.error));
				return;
			}
			ddDatos = data;
			ddPintar(data);
		},
		error: function () {
			$('#ddCargando').hide();
			$('#ddConsultar').prop('disabled', false);
			ddMensaje('danger', 'No se pudo consultar el desempeño. Revise el log del servidor.');
		}
	});
}

// ===========================================================================
// Pintar
// ===========================================================================

function ddPintar(d) {
	var r = d.resumen;
	var g = d.regreso;

	$('#ddCabecera').html(
		'<h3 style="margin-top:0;">' + ddEscapar(d.domiciliario.nombrelargo) + '</h3>' +
		'<p class="dd-nota" style="font-size:12px;">' + ddEscapar(d.domiciliario.tipo) +
		' &nbsp;|&nbsp; usuario ' + ddEscapar(d.domiciliario.nombre) +
		' &nbsp;|&nbsp; del ' + ddEscapar(d.desde) + ' al ' + ddEscapar(d.hasta) +
		' &nbsp;|&nbsp; ' + d.tiendas_consultadas + ' tiendas consultadas</p>');

	//Una tienda que no contesto no es una tienda sin pedidos. Si no se dice,
	//el domiciliario aparece con menos trabajo del que hizo.
	var sin = d.tiendas_sin_respuesta || [];
	if (sin.length > 0) {
		var nombres = [];
		for (var i = 0; i < sin.length; i++) {
			nombres.push(ddEscapar(sin[i]));
		}
		$('#ddSinRespuesta').html('<div class="alert alert-warning" style="padding:8px 12px;">' +
			'<b>Ojo:</b> no contestaron ' + nombres.join(', ') + '. Si el domiciliario trabajo ahi, ' +
			'esos pedidos NO estan contados abajo.</div>');
	} else {
		$('#ddSinRespuesta').html('');
	}

	if (r.pedidos === 0) {
		$('#ddKpis').html('<div class="col-md-12"><div class="alert alert-info" style="margin:0;">' +
			'No hay entregas registradas para este domiciliario en el rango.</div></div>');
		$('#ddResultado').show();
		$('#ddGrafPromesa,#ddGrafRegreso,#ddGrafDias,#ddEnrutamiento,#ddCalidad').html('');
		$('#ddTablaTiendas tbody,#ddTablaPeores tbody').html('');
		return;
	}

	$('#ddKpis').html(
		ddKpi(r.pedidos, 'Pedidos entregados', r.salidas + ' salidas', '') +
		ddKpi(r.porcentaje + '%', 'Entregas a tiempo', r.a_tiempo + ' de ' + r.medibles,
			ddColorPorcentaje(r.porcentaje, 90, 75)) +
		ddKpi(r.promedio_total, 'Minutos promedio', 'mediana ' + r.mediana_total + ' | desde que entra el pedido', '') +
		ddKpi(r.promedio_calle, 'Minutos en calle', 'mediana ' + r.mediana_calle + ' | esto si es suyo', '') +
		ddKpi(r.pedidos_por_salida, 'Pedidos por salida', 'cuantos lleva junto', '') +
		ddKpi(g.mediana, 'Regreso (mediana)', 'promedio ' + g.promedio + ' min', '') +
		ddKpi(g.porcentaje + '%', 'Regresos a tiempo', 'hasta ' + g.alerta_minutos + ' min | ' +
			g.a_tiempo + ' de ' + g.medibles, ddColorPorcentaje(g.porcentaje, 90, 75)));

	$('#ddGrafPromesa').html(ddBarras(d.distribucion_promesa, 'entregas'));
	$('#ddGrafRegreso').html(ddBarras(d.distribucion_regreso, 'salidas'));
	$('#ddGrafDias').html(ddLineaDias(d.por_dia));

	ddPintarTiendas(d.por_tienda);
	ddPintarEnrutamiento(d.enrutamiento);
	ddPintarCalidad(g, r);
	ddPintarPeores(d.peores);

	$('#ddResultado').show();
}

function ddKpi(valor, rotulo, apoyo, clase) {
	return ('<div class="col-md-3 col-sm-6"><div class="dd-kpi">' +
		'<div class="valor ' + clase + '">' + valor + '</div>' +
		'<div class="rotulo">' + rotulo + '</div>' +
		'<div class="apoyo">' + apoyo + '</div></div></div>');
}

function ddColorPorcentaje(valor, bueno, regular) {
	if (valor >= bueno) { return ('dd-bien'); }
	if (valor >= regular) { return ('dd-regular'); }
	return ('dd-mal');
}

// ===========================================================================
// Las graficas
// ===========================================================================

/* Barras horizontales: los rotulos son textos largos y verticales no caben. */
function ddBarras(datos, unidad) {
	if (!datos || datos.length === 0) { return (''); }

	var anchoRotulo = 130;
	var anchoBarra = 330;
	var alto = 26;
	var total = 0;
	var maximo = 0;
	var i;
	for (i = 0; i < datos.length; i++) {
		total += datos[i].valor;
		if (datos[i].valor > maximo) { maximo = datos[i].valor; }
	}
	if (maximo === 0) { maximo = 1; }

	var altoTotal = datos.length * alto + 20;
	var svg = '<svg viewBox="0 0 520 ' + altoTotal + '" width="100%" height="' + altoTotal +
		'" preserveAspectRatio="xMinYMin meet">';
	for (i = 0; i < datos.length; i++) {
		var y = i * alto + 6;
		var ancho = Math.round(datos[i].valor * anchoBarra / maximo);
		var color = datos[i].bueno ? '#4caf50' : '#e53935';
		var pct = total > 0 ? Math.round(datos[i].valor * 1000 / total) / 10 : 0;
		svg += '<text x="0" y="' + (y + 13) + '" font-size="11" fill="#444">' +
			ddEscapar(datos[i].rango) + '</text>';
		svg += '<rect x="' + anchoRotulo + '" y="' + y + '" width="' + Math.max(ancho, 1) +
			'" height="' + (alto - 8) + '" fill="' + color + '" opacity="0.85"/>';
		svg += '<text x="' + (anchoRotulo + Math.max(ancho, 1) + 6) + '" y="' + (y + 13) +
			'" font-size="11" fill="#666">' + datos[i].valor + ' (' + pct + '%)</text>';
	}
	svg += '<text x="0" y="' + (altoTotal - 2) + '" font-size="10" fill="#999">' +
		total + ' ' + unidad + ' medibles</text>';
	svg += '</svg>';
	return (svg);
}

/*
 * Un dia por barra, con la linea del porcentaje a tiempo encima. Las dos cosas
 * juntas porque por separado enganan: un dia con 2 pedidos y 100% no dice lo
 * mismo que uno con 30 pedidos y 100%.
 */
function ddLineaDias(dias) {
	if (!dias || dias.length === 0) { return ('<p class="dd-nota">Sin dias con entregas.</p>'); }

	var ancho = 1000;
	var alto = 220;
	var margenIzq = 35;
	var margenDer = 40;
	var margenSup = 15;
	var margenInf = 45;
	var util = ancho - margenIzq - margenDer;
	var utilAlto = alto - margenSup - margenInf;
	var i;

	var maxPedidos = 1;
	for (i = 0; i < dias.length; i++) {
		if (dias[i].pedidos > maxPedidos) { maxPedidos = dias[i].pedidos; }
	}

	var paso = util / dias.length;
	var anchoBarra = Math.max(Math.min(paso - 4, 30), 2);

	var svg = '<svg viewBox="0 0 ' + ancho + ' ' + alto + '" width="100%" height="' + alto +
		'" preserveAspectRatio="xMinYMin meet">';

	//Rejilla del porcentaje.
	var marcas = [0, 25, 50, 75, 100];
	for (i = 0; i < marcas.length; i++) {
		var yl = margenSup + utilAlto - (marcas[i] / 100) * utilAlto;
		svg += '<line x1="' + margenIzq + '" y1="' + yl + '" x2="' + (ancho - margenDer) + '" y2="' + yl +
			'" stroke="#eee" stroke-width="1"/>';
		svg += '<text x="' + (ancho - margenDer + 4) + '" y="' + (yl + 4) +
			'" font-size="10" fill="#999">' + marcas[i] + '%</text>';
	}

	var puntos = '';
	for (i = 0; i < dias.length; i++) {
		var x = margenIzq + i * paso + (paso - anchoBarra) / 2;
		var h = (dias[i].pedidos / maxPedidos) * utilAlto;
		svg += '<rect x="' + x + '" y="' + (margenSup + utilAlto - h) + '" width="' + anchoBarra +
			'" height="' + h + '" fill="#102F6F" opacity="0.25"/>';

		var cx = margenIzq + i * paso + paso / 2;
		var cy = margenSup + utilAlto - (dias[i].porcentaje / 100) * utilAlto;
		puntos += (i === 0 ? 'M' : 'L') + cx + ' ' + cy + ' ';
		svg += '<circle cx="' + cx + '" cy="' + cy + '" r="2.5" fill="#E42528"><title>' +
			ddEscapar(dias[i].fecha) + ': ' + dias[i].a_tiempo + ' de ' + dias[i].medibles +
			' a tiempo, ' + dias[i].porcentaje + '%, promedio en calle ' + dias[i].promedio_calle +
			' min</title></circle>';
	}
	svg += '<path d="' + puntos + '" fill="none" stroke="#E42528" stroke-width="1.5"/>';

	//Solo algunas fechas: con treinta dias los rotulos se montan.
	var cada = Math.ceil(dias.length / 12);
	for (i = 0; i < dias.length; i++) {
		if (i % cada !== 0) { continue; }
		var xt = margenIzq + i * paso + paso / 2;
		svg += '<text x="' + xt + '" y="' + (alto - margenInf + 18) + '" font-size="9" fill="#777" ' +
			'text-anchor="end" transform="rotate(-45 ' + xt + ' ' + (alto - margenInf + 18) + ')">' +
			ddEscapar(dias[i].fecha.substring(5)) + '</text>';
	}

	svg += '<text x="' + margenIzq + '" y="' + (alto - 4) + '" font-size="10" fill="#999">' +
		'Barras: pedidos entregados. Linea roja: porcentaje a tiempo.</text>';
	svg += '</svg>';
	return (svg);
}

// ===========================================================================
// Las tablas
// ===========================================================================

function ddPintarTiendas(tiendas) {
	var html = '';
	for (var i = 0; i < tiendas.length; i++) {
		var t = tiendas[i];
		var r = t.resumen;
		var g = t.regreso;
		html += '<tr>' +
			'<td>' + ddEscapar(t.tienda) + '</td>' +
			'<td class="text-right">' + r.pedidos + '</td>' +
			'<td class="text-right">' + r.salidas + '</td>' +
			'<td class="text-right">' + r.pedidos_por_salida + '</td>' +
			'<td class="text-right ' + ddColorPorcentaje(r.porcentaje, 90, 75) + '">' +
				r.porcentaje + '% <span class="apoyo">(' + r.a_tiempo + '/' + r.medibles + ')</span></td>' +
			'<td class="text-right">' + r.promedio_total + '</td>' +
			'<td class="text-right">' + r.promedio_calle + '</td>' +
			'<td class="text-right">' + g.mediana + '</td>' +
			'<td class="text-right ' + ddColorPorcentaje(g.porcentaje, 90, 75) + '">' + g.porcentaje + '%</td>' +
			'</tr>';
	}
	$('#ddTablaTiendas tbody').html(html);
}

function ddPintarEnrutamiento(e) {
	if (e.sugerencias === 0 && e.salidas_sugeridas === 0) {
		$('#ddEnrutamiento').html('<p class="dd-nota" style="font-size:12px;">' +
			'A este domiciliario no le llego ninguna sugerencia de enrutamiento en el rango. ' +
			'El enrutamiento se desplego el 10 de septiembre y todavia se esta usando poco: ' +
			'al 17 de septiembre habia 42 sugerencias en total, y solo en Bello, Pilarica y ' +
			'San Antonio.</p>');
		return;
	}
	var llevados = e.pedidos_llevados + e.pedidos_reasignados + e.pedidos_sin_llevar;
	$('#ddEnrutamiento').html(
		'<table class="table table-condensed" style="margin-bottom:6px;">' +
		ddFila('Sugerencias que le llegaron', e.sugerencias) +
		ddFila('&nbsp;&nbsp;Aceptadas', e.aceptadas) +
		ddFila('&nbsp;&nbsp;Rechazadas', e.rechazadas) +
		ddFila('&nbsp;&nbsp;Expiradas sin responder', e.expiradas) +
		ddFila('&nbsp;&nbsp;Todavia pendientes', e.pendientes) +
		ddFila('Veces que le movieron un pedido ya sugerido', e.reasignaciones) +
		ddFila('Pedidos sugeridos que si llevo', e.pedidos_llevados + ' de ' + llevados) +
		ddFila('Pedidos que le pasaron a otro', e.pedidos_reasignados) +
		ddFila('Salidas que nacieron de una sugerencia', e.salidas_sugeridas) +
		'</table>');
}

function ddFila(rotulo, valor) {
	return ('<tr><td style="font-size:12px;">' + rotulo + '</td>' +
		'<td class="text-right" style="font-size:12px;"><b>' + valor + '</b></td></tr>');
}

/*
 * Lo que no se pudo medir, a la vista. Si esto se esconde, los promedios de
 * arriba parecen mas solidos de lo que son.
 */
function ddPintarCalidad(g, r) {
	var perdidas = g.negativos + g.sin_marcar + g.sin_regreso + g.sin_entrega;
	var pct = g.salidas > 0 ? Math.round(perdidas * 1000 / g.salidas) / 10 : 0;
	var html = '<table class="table table-condensed" style="margin-bottom:6px;">' +
		ddFila('Salidas en el rango', g.salidas) +
		ddFila('Con regreso medible', g.medibles) +
		ddFila('Regreso marcado ANTES de la ultima entrega', g.negativos) +
		ddFila('Regreso a mas de ' + g.tope_minutos + ' min (no lo marcaron)', g.sin_marcar) +
		ddFila('Sin hora de regreso', g.sin_regreso) +
		ddFila('Con regreso pero sin entrega contra que medir', g.sin_entrega) +
		ddFila('Entregas sin tiempo prometido', r.sin_medir) +
		'</table>';
	if (pct >= 10) {
		html += '<div class="alert alert-warning" style="padding:8px 12px;font-size:12px;margin-bottom:0;">' +
			'El ' + pct + '% de las salidas no se pudo medir. El promedio de regreso sale solo de las ' +
			g.medibles + ' que si se pudieron.</div>';
	}
	$('#ddCalidad').html(html);
}

function ddPintarPeores(peores) {
	var html = '';
	for (var i = 0; i < peores.length; i++) {
		var p = peores[i];
		html += '<tr>' +
			'<td>' + ddEscapar(p.tienda) + '</td>' +
			'<td>' + ddEscapar(p.fecha) + '</td>' +
			'<td class="text-right">' + p.idpedido + '</td>' +
			'<td class="text-right">' + p.prometido + '</td>' +
			'<td class="text-right">' + p.minutos + '</td>' +
			'<td class="text-right dd-mal"><b>+' + p.exceso + '</b></td>' +
			'<td class="text-right">' + p.calle + '</td>' +
			'</tr>';
	}
	if (html === '') {
		html = '<tr><td colspan="7" class="dd-nota">Ninguna entrega se paso del tiempo prometido.</td></tr>';
	}
	$('#ddTablaPeores tbody').html(html);
}

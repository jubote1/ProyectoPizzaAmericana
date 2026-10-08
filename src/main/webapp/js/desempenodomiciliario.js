/*
 * Tablero de desempeno de un domiciliario.
 *
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

	$('#ddBtnExcel').click(function () {
		ddExportarExcel();
	});

	$(document).on('input change', '#ddHorasReales, .sc-meta', function () {
		ddRecalcularScorecard();
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
			ddMensaje('danger', 'No se pudo consultar el desempe\u00f1o. Revise el log del servidor.');
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

	ddPintarScorecard(d);
	$('#ddBtnExcel').show();
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

	var anchoRotulo = 160;
	var anchoBarra = 320;
	var alto = 36;
	var total = 0;
	var maximo = 0;
	var i;
	for (i = 0; i < datos.length; i++) {
		total += datos[i].valor;
		if (datos[i].valor > maximo) { maximo = datos[i].valor; }
	}
	if (maximo === 0) { maximo = 1; }

	var altoTotal = datos.length * alto + 32;
	var svg = '<svg viewBox="0 0 620 ' + altoTotal + '" width="100%" height="' + altoTotal +
		'" preserveAspectRatio="xMinYMin meet" style="font-family: inherit;">';

	for (i = 0; i < datos.length; i++) {
		var y = i * alto + 6;
		var ancho = Math.round(datos[i].valor * anchoBarra / maximo);
		var color = datos[i].bueno ? '#10b981' : '#f43f5e';
		var pct = total > 0 ? Math.round(datos[i].valor * 1000 / total) / 10 : 0;

		// Rotulo izquierdo con letra clara y legible
		svg += '<text x="0" y="' + (y + 17) + '" font-size="13" font-weight="600" fill="#334155">' +
			ddEscapar(datos[i].rango) + '</text>';

		// Barra de fondo gris suave
		svg += '<rect x="' + anchoRotulo + '" y="' + y + '" width="' + anchoBarra +
			'" height="' + (alto - 12) + '" rx="6" ry="6" fill="#f1f5f9"/>';

		// Barra de valor rellena moderna
		if (datos[i].valor > 0) {
			svg += '<rect x="' + anchoRotulo + '" y="' + y + '" width="' + Math.max(ancho, 8) +
				'" height="' + (alto - 12) + '" rx="6" ry="6" fill="' + color + '"/>';
		}

		// Valor numerico y porcentaje a la derecha
		svg += '<text x="' + (anchoRotulo + anchoBarra + 14) + '" y="' + (y + 17) +
			'" font-size="13" font-weight="700" fill="#0f172a">' + datos[i].valor +
			' <tspan font-weight="500" fill="#64748b">(' + pct + '%)</tspan></text>';
	}

	// Pie de grafico
	svg += '<text x="0" y="' + (altoTotal - 4) + '" font-size="12" font-weight="600" fill="#64748b">' +
		total + ' ' + unidad + ' medibles</text>';
	svg += '</svg>';
	return (svg);
}

/*
 * Un dia por barra, con la linea del porcentaje a tiempo encima.
 */
function ddLineaDias(dias) {
	if (!dias || dias.length === 0) { return ('<p class="dd-nota">Sin dias con entregas.</p>'); }

	var ancho = 1000;
	var alto = 250;
	var margenIzq = 45;
	var margenDer = 50;
	var margenSup = 42;
	var margenInf = 45;
	var util = ancho - margenIzq - margenDer;
	var utilAlto = alto - margenSup - margenInf;
	var i;

	var maxPedidos = 1;
	for (i = 0; i < dias.length; i++) {
		if (dias[i].pedidos > maxPedidos) { maxPedidos = dias[i].pedidos; }
	}

	var paso = util / dias.length;
	var anchoBarra = Math.max(Math.min(paso - 6, 32), 4);

	var svg = '<svg viewBox="0 0 ' + ancho + ' ' + alto + '" width="100%" height="' + alto +
		'" preserveAspectRatio="xMinYMin meet" style="font-family: inherit;">';

	// Leyenda superior elegante
	svg += '<g transform="translate(' + margenIzq + ', 14)">';
	svg += '<rect x="0" y="0" width="14" height="12" rx="3" ry="3" fill="#6366f1" opacity="0.45"/>';
	svg += '<text x="20" y="10" font-size="12" font-weight="600" fill="#475569">Pedidos entregados (barras)</text>';
	svg += '<line x1="220" y1="6" x2="245" y2="6" stroke="#e11d48" stroke-width="3" stroke-linecap="round"/>';
	svg += '<circle cx="232" cy="6" r="4" fill="#e11d48"/>';
	svg += '<text x="255" y="10" font-size="12" font-weight="600" fill="#475569">% Entregas a tiempo (meta: 90%)</text>';
	svg += '</g>';

	// Rejilla del porcentaje
	var marcas = [0, 25, 50, 75, 100];
	for (i = 0; i < marcas.length; i++) {
		var yl = margenSup + utilAlto - (marcas[i] / 100) * utilAlto;
		svg += '<line x1="' + margenIzq + '" y1="' + yl + '" x2="' + (ancho - margenDer) + '" y2="' + yl +
			'" stroke="#e2e8f0" stroke-width="1" stroke-dasharray="' + (marcas[i] === 0 ? 'none' : '4 4') + '"/>';
		svg += '<text x="' + (ancho - margenDer + 8) + '" y="' + (yl + 4) +
			'" font-size="12" font-weight="600" fill="#64748b">' + marcas[i] + '%</text>';
	}

	// Linea guia de la meta 90%
	var y90 = margenSup + utilAlto - (90 / 100) * utilAlto;
	svg += '<line x1="' + margenIzq + '" y1="' + y90 + '" x2="' + (ancho - margenDer) + '" y2="' + y90 +
		'" stroke="#059669" stroke-width="1.5" stroke-dasharray="3 3" opacity="0.6"/>';

	var puntos = '';
	for (i = 0; i < dias.length; i++) {
		var x = margenIzq + i * paso + (paso - anchoBarra) / 2;
		var h = Math.max((dias[i].pedidos / maxPedidos) * utilAlto, 3);
		svg += '<rect x="' + x + '" y="' + (margenSup + utilAlto - h) + '" width="' + anchoBarra +
			'" height="' + h + '" fill="#6366f1" opacity="0.45" rx="4" ry="4"><title>' +
			ddEscapar(dias[i].fecha) + ': ' + dias[i].pedidos + ' pedidos entregados</title></rect>';

		var cx = margenIzq + i * paso + paso / 2;
		var cy = margenSup + utilAlto - (dias[i].porcentaje / 100) * utilAlto;
		puntos += (i === 0 ? 'M' : 'L') + cx + ' ' + cy + ' ';
	}

	svg += '<path d="' + puntos + '" fill="none" stroke="#e11d48" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>';

	for (i = 0; i < dias.length; i++) {
		var cx = margenIzq + i * paso + paso / 2;
		var cy = margenSup + utilAlto - (dias[i].porcentaje / 100) * utilAlto;
		svg += '<circle cx="' + cx + '" cy="' + cy + '" r="5" fill="#e11d48" stroke="#ffffff" stroke-width="2.5"><title>' +
			ddEscapar(dias[i].fecha) + ': ' + dias[i].a_tiempo + ' de ' + dias[i].medibles +
			' a tiempo (' + dias[i].porcentaje + '%), promedio en calle ' + dias[i].promedio_calle +
			' min</title></circle>';
	}

	var cada = Math.ceil(dias.length / 14);
	for (i = 0; i < dias.length; i++) {
		if (i % cada !== 0) { continue; }
		var xt = margenIzq + i * paso + paso / 2;
		svg += '<text x="' + xt + '" y="' + (alto - margenInf + 22) + '" font-size="12" font-weight="600" fill="#475569" ' +
			'text-anchor="end" transform="rotate(-35 ' + xt + ' ' + (alto - margenInf + 22) + ')">' +
			ddEscapar(dias[i].fecha.substring(5)) + '</text>';
	}

	svg += '</svg>';
	return (svg);
}

// ===========================================================================
// Las tablas
// ===========================================================================

function ddPintarTiendas(tiendas) {
	var html = '\u2014';
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
// Linea guia de la meta 90%
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
		ddFila('Pedidos programados entregados (cumplieron; no se miden en tiempo)', r.programados) +
		'</table>';
	if (pct >= 10) {
		html += '<div class="alert alert-warning" style="padding:8px 12px;font-size:12px;margin-bottom:0;">' +
			'El ' + pct + '% de las salidas no se pudo medir. El promedio de regreso sale solo de las ' +
			g.medibles + ' que si se pudieron.</div>';
	}
	$('#ddCalidad').html(html);
}

function ddPintarPeores(peores) {
	var html = '\u2014';
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
// Linea guia de la meta 90%
	}
	$('#ddTablaPeores tbody').html(html);
}


// ===========================================================================
// Evaluacion de Desempeno y Liquidacion (Gerencia)
// ===========================================================================

function ddFormatoDinero(n) {
	return (Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.'));
}

function ddPintarScorecard(d) {
	var horas = (d.horas_trabajadas !== undefined && d.horas_trabajadas !== null) ? d.horas_trabajadas : 0;
	$('#ddHorasReales').val(horas > 0 ? horas : '');
	ddRecalcularScorecard();
}

function ddRecalcularScorecard() {
	if (!ddDatos || !ddDatos.resumen) {
		return;
	}
	var r = ddDatos.resumen;
	var g = ddDatos.regreso;
	var horas = parseFloat($('#ddHorasReales').val()) || 0;
	var pedidos = r.pedidos || 0;
	var salidas = r.salidas || 0;

	// Actualizar columnas informativas de horas y pedidos
	var textoHoras = horas > 0 ? (horas.toFixed(1) + ' h') : '0.0 h';
	var textoPedidos = pedidos + ' pedidos';
	$('.sc-horas').text(textoHoras);
	$('.sc-pedidos').text(textoPedidos);
	$('#scDetalleSalidas').text(pedidos + ' ped / ' + salidas + ' sal');

	// 1. Productividad por hora (Peso 30%)
	var prodVal = horas > 0 ? (pedidos / horas) : 0;
	$('#scResProductividad').text(prodVal > 0 ? (prodVal.toFixed(2) + ' ped/h') : '0.00 ped/h');
	var metaProd = parseFloat($('#metaProd').val()) || 2.0;
	var cumpProd = metaProd > 0 ? (prodVal / metaProd) * 100 : 0;
	$('#scCumpProductividad').text(cumpProd.toFixed(2) + '%');
	var ptsProd = (cumpProd / 100) * 30;
	$('#scPtsProductividad').text(ptsProd.toFixed(2));

	// 2. Entregas a tiempo (Peso 35%)
	var entregasPct = r.porcentaje || 0;
	$('#scResEntregas').text(entregasPct.toFixed(2) + '%');
	var metaEntregas = parseFloat($('#metaEntregas').val()) || 90.0;
	var cumpEntregas = metaEntregas > 0 ? (entregasPct / metaEntregas) * 100 : 0;
	$('#scCumpEntregas').text(cumpEntregas.toFixed(2) + '%');
	var ptsEntregas = (cumpEntregas / 100) * 35;
	$('#scPtsEntregas').text(ptsEntregas.toFixed(2));

	// 3. Regresos a tiempo (Peso 20% - Tope 100%)
	var regresosPct = g.porcentaje || 0;
	$('#scResRegresos').text(regresosPct.toFixed(2) + '%');
	var metaRegresos = parseFloat($('#metaRegresos').val()) || 90.0;
	var cumpRegresosRaw = metaRegresos > 0 ? (regresosPct / metaRegresos) * 100 : 0;
	var cumpRegresos = Math.min(cumpRegresosRaw, 100);
	$('#scCumpRegresos').text(cumpRegresos.toFixed(2) + '%');
	var ptsRegresos = (cumpRegresos / 100) * 20;
	$('#scPtsRegresos').text(ptsRegresos.toFixed(2));

	// 4. Pedidos por salida (Peso 15% - Tope 100%)
	var salidasRatio = r.pedidos_por_salida || 0;
	$('#scResSalidas').text(salidasRatio.toFixed(2) + ' ped/sal');
	var metaSalidas = parseFloat($('#metaSalidas').val()) || 1.0;
	var cumpSalidasRaw = metaSalidas > 0 ? (salidasRatio / metaSalidas) * 100 : 0;
	var cumpSalidas = Math.min(cumpSalidasRaw, 100);
	$('#scCumpSalidas').text(cumpSalidas.toFixed(2) + '%');
	var ptsSalidas = (cumpSalidas / 100) * 15;
	$('#scPtsSalidas').text(ptsSalidas.toFixed(2));

	// Totales
	var totalPuntos = ptsProd + ptsEntregas + ptsRegresos + ptsSalidas;
	var cumplimientoGlobal = totalPuntos;

	$('#scCumpTotal').text(cumplimientoGlobal.toFixed(2) + '%');
	$('#scPtsTotal').text(totalPuntos.toFixed(2) + ' / 100');
	$('#scTotalPuntos').text(totalPuntos.toFixed(2));
	$('#scCumplimientoGlobal').text(cumplimientoGlobal.toFixed(2) + '%');

	// Color del cumplimiento
	if (cumplimientoGlobal >= 80) {
		$('#scCumplimientoGlobal').css('color', '#2e7d32');
	} else if (cumplimientoGlobal >= 51) {
		$('#scCumplimientoGlobal').css('color', '#ef6c00');
	} else {
		$('#scCumplimientoGlobal').css('color', '#c62828');
	}

	// Escala de Gerencia:
	// $2.500: Cumplimiento 0% al 50%
	// $2.700: Cumplimiento 51% al 79%
	// $3.000: Cumplimiento 80% al 100%
	// $3.500: Cumplimiento >= 120%
	$('.sc-tier-badge').removeClass('active');
	var tarifa = 2500;
	var rangoTexto = '\u2014';

	if (cumplimientoGlobal >= 120) {
		tarifa = 3500;
		$('#scTier4').addClass('active');
		rangoTexto = 'Nivel Sobresaliente (\u2265 120%)';
	} else if (cumplimientoGlobal >= 80) {
		tarifa = 3000;
		$('#scTier3').addClass('active');
		rangoTexto = 'Nivel \u00d3ptimo (80% - 100%)';
	} else if (cumplimientoGlobal >= 51) {
		tarifa = 2700;
		$('#scTier2').addClass('active');
		rangoTexto = 'Nivel Medio (51% - 79%)';
	} else {
		tarifa = 2500;
		$('#scTier1').addClass('active');
		rangoTexto = 'Nivel B\u00e1sico (0% - 50%)';
	}

	$('#scRangoTexto').text(rangoTexto);
	$('#scTarifaPedido').text('$' + ddFormatoDinero(tarifa));

	var totalLiquidacion = pedidos * tarifa;
	$('#scTotalLiquidacion').text('$' + ddFormatoDinero(totalLiquidacion));
	$('#scSubtotalLiquidacion').text(pedidos + ' pedidos \u00d7 $' + ddFormatoDinero(tarifa));
}

// ===========================================================================
// Exportacion a Excel
// ===========================================================================

function ddExportarExcel() {
	if (!ddDatos || !ddDatos.domiciliario) {
		alert('Primero debe consultar el desempe\u00f1o de un domiciliario.');
		return;
	}

	var domi = ddDatos.domiciliario;
	var r = ddDatos.resumen;
	var g = ddDatos.regreso;
	var horas = parseFloat($('#ddHorasReales').val()) || 0;

	if (typeof ExcelJS !== 'undefined') {
		var wb = new ExcelJS.Workbook();
		wb.creator = 'Pizza Americana';
		wb.created = new Date();
		var ws = wb.addWorksheet('Desempe\u00f1o y Liquidaci\u00f3n');

		ws.columns = [
			{ width: 32 },
			{ width: 18 },
			{ width: 22 },
			{ width: 22 },
			{ width: 18 },
			{ width: 18 },
			{ width: 14 },
			{ width: 20 }
		];

		// Titulo Banner
		ws.mergeCells('A1:H1');
		var cellTit = ws.getCell('A1');
		cellTit.value = 'PIZZA AMERICANA - EVALUACI\u00d3N DE DESEMPE\u00d1O Y LIQUIDACI\u00d3N';
		cellTit.font = { name: 'Arial', size: 14, bold: true, color: { argb: 'FFFFFFFF' } };
		cellTit.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF102F6F' } };
		cellTit.alignment = { horizontal: 'center', vertical: 'middle' };
		ws.getRow(1).height = 32;

		// Metadata
		ws.addRow([]);
		ws.addRow(['Domiciliario:', domi.nombrelargo + ' (' + domi.nombre + ')', '\u2014', 'Rango Evaluado:', 'Del ' + ddDatos.desde + ' al ' + ddDatos.hasta]);
		ws.addRow(['Tipo Repartidor:', domi.tipo, '\u2014', 'Horas Reales:', horas + ' h']);
		ws.addRow(['Total Pedidos Entregados:', r.pedidos + ' entregas', '\u2014', 'Total Salidas:', r.salidas + ' salidas']);
		ws.addRow([]);

		ws.getCell('A3').font = { bold: true };
		ws.getCell('D3').font = { bold: true };
		ws.getCell('A4').font = { bold: true };
		ws.getCell('D4').font = { bold: true };
		ws.getCell('A5').font = { bold: true };
		ws.getCell('D5').font = { bold: true };

		// Encabezado Scorecard
		var headRow = ws.addRow(['Indicador', 'Horas reales', 'Pedidos entregados', 'Resultado obtenido', 'Meta', '% Cumplimiento', 'Peso', 'Puntos obtenidos']);
		headRow.font = { bold: true, color: { argb: 'FFFFFFFF' } };
		headRow.alignment = { horizontal: 'center', vertical: 'middle' };
		headRow.eachCell(function (cell) {
			cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF102F6F' } };
			cell.border = { top: { style: 'thin' }, left: { style: 'thin' }, bottom: { style: 'thin' }, right: { style: 'thin' } };
		});
		headRow.height = 24;

		// Filas Scorecard
		var row1 = ws.addRow(['Productividad por hora', horas + ' h', r.pedidos + ' pedidos', $('#scResProductividad').text(), $('#metaProd').val() + ' ped/h', $('#scCumpProductividad').text(), '30%', $('#scPtsProductividad').text()]);
		var row2 = ws.addRow(['Entregas a tiempo', horas + ' h', r.pedidos + ' pedidos', $('#scResEntregas').text(), $('#metaEntregas').val() + '%', $('#scCumpEntregas').text(), '35%', $('#scPtsEntregas').text()]);
		var row3 = ws.addRow(['Regresos a tiempo', horas + ' h', '\u2014', $('#scResRegresos').text(), $('#metaRegresos').val() + '%', $('#scCumpRegresos').text(), '20%', $('#scPtsRegresos').text()]);
		var row4 = ws.addRow(['Pedidos por salida', horas + ' h', r.pedidos + ' ped / ' + r.salidas + ' sal', $('#scResSalidas').text(), $('#metaSalidas').val() + ' ped/sal', $('#scCumpSalidas').text(), '15%', $('#scPtsSalidas').text()]);

		var rowTotal = ws.addRow(['TOTAL EVALUACI\u00d3N', horas + ' h', r.pedidos + ' pedidos', '\u2014', '\u2014', $('#scCumpTotal').text(), '100%', $('#scPtsTotal').text()]);
		rowTotal.font = { bold: true };

		[row1, row2, row3, row4, rowTotal].forEach(function (row) {
			row.eachCell(function (cell, colNum) {
				cell.border = { top: { style: 'thin', color: { argb: 'FFCCCCCC' } }, left: { style: 'thin', color: { argb: 'FFCCCCCC' } }, bottom: { style: 'thin', color: { argb: 'FFCCCCCC' } }, right: { style: 'thin', color: { argb: 'FFCCCCCC' } } };
				if (colNum >= 2) {
					cell.alignment = { horizontal: 'center' };
				}
			});
		});

		ws.addRow([]);

		// Resumen Liquidacion
		ws.addRow(['RESUMEN DE LIQUIDACI\u00d3N Y PAGO (ESCALA DE GERENCIA)']);
		ws.lastRow.font = { bold: true, size: 12, color: { argb: 'FF102F6F' } };
		ws.addRow(['Puntaje Total Obtenido:', $('#scTotalPuntos').text() + ' / 100']);
		ws.addRow(['Nivel de Cumplimiento:', $('#scCumplimientoGlobal').text() + ' (' + $('#scRangoTexto').text() + ')']);
		ws.addRow(['Tarifa por Pedido Asignada:', $('#scTarifaPedido').text()]);
		ws.addRow(['Total Liquidaci\u00f3n Sugerida:', $('#scTotalLiquidacion').text() + ' (' + $('#scSubtotalLiquidacion').text() + ')']);
		ws.lastRow.font = { bold: true, color: { argb: 'FF00796B' }, size: 12 };

		ws.addRow([]);

		// Desglose por Tienda
		var headTiendas = ws.addRow(['Desglose por Tienda', 'Entregas', 'Salidas', 'Pedidos / salida', 'A tiempo', 'Demorados', '% A tiempo']);
		headTiendas.font = { bold: true, color: { argb: 'FFFFFFFF' } };
		headTiendas.eachCell(function (cell) {
			cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF333333' } };
			cell.border = { top: { style: 'thin' }, left: { style: 'thin' }, bottom: { style: 'thin' }, right: { style: 'thin' } };
		});

		var porTienda = ddDatos.por_tienda || [];
		porTienda.forEach(function (t) {
			var rTienda = ws.addRow([t.tienda, t.entregas, t.salidas, t.pedidos_por_salida, t.a_tiempo, t.tarde, t.porcentaje + '%']);
			rTienda.eachCell(function (cell, colNum) {
				cell.border = { top: { style: 'thin', color: { argb: 'FFCCCCCC' } }, left: { style: 'thin', color: { argb: 'FFCCCCCC' } }, bottom: { style: 'thin', color: { argb: 'FFCCCCCC' } }, right: { style: 'thin', color: { argb: 'FFCCCCCC' } } };
				if (colNum >= 2) {
					cell.alignment = { horizontal: 'center' };
				}
			});
		});

		wb.xlsx.writeBuffer().then(function (buffer) {
			var blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
			var fileName = 'Desempeno_' + domi.nombrelargo.replace(/\s+/g, '_') + '_' + ddDatos.desde + '_al_' + ddDatos.hasta + '.xlsx';
			saveAs(blob, fileName);
		});
	} else {
		// Fallback CSV
		ddExportarCSV();
	}
}

function ddExportarCSV() {
	var domi = ddDatos.domiciliario;
	var r = ddDatos.resumen;
	var horas = parseFloat($('#ddHorasReales').val()) || 0;

	var csv = '\uFEFF';
	csv += 'PIZZA AMERICANA - EVALUACION DE DESEMPENO Y LIQUIDACION\r\n\r\n';
	csv += 'Domiciliario;' + domi.nombrelargo + ' (' + domi.nombre + ')\r\n';
	csv += 'Rango Evaluado;Del ' + ddDatos.desde + ' al ' + ddDatos.hasta + '\r\n';
	csv += 'Horas Reales;' + horas + ' h\r\n';
	csv += 'Total Pedidos;' + r.pedidos + '\r\n';
	csv += 'Total Salidas;' + r.salidas + '\r\n\r\n';

	csv += 'Indicador;Horas reales;Pedidos entregados;Resultado obtenido;Meta;% Cumplimiento;Peso;Puntos obtenidos\r\n';
	csv += 'Productividad por hora;' + horas + ' h;' + r.pedidos + ' pedidos;' + $('#scResProductividad').text() + ';' + $('#metaProd').val() + ' ped/h;' + $('#scCumpProductividad').text() + ';30%;' + $('#scPtsProductividad').text() + '\r\n';
	csv += 'Entregas a tiempo;' + horas + ' h;' + r.pedidos + ' pedidos;' + $('#scResEntregas').text() + ';' + $('#metaEntregas').val() + '%;' + $('#scCumpEntregas').text() + ';35%;' + $('#scPtsEntregas').text() + '\r\n';
	csv += 'Regresos a tiempo;' + horas + ' h;\u2014;' + $('#scResRegresos').text() + ';' + $('#metaRegresos').val() + '%;' + $('#scCumpRegresos').text() + ';20%;' + $('#scPtsRegresos').text() + '\r\n';
	csv += 'Pedidos por salida;' + horas + ' h;' + r.pedidos + ' ped / ' + r.salidas + ' sal;' + $('#scResSalidas').text() + ';' + $('#metaSalidas').val() + ' ped/sal;' + $('#scCumpSalidas').text() + ';15%;' + $('#scPtsSalidas').text() + '\r\n';
	csv += 'TOTAL EVALUACION;' + horas + ' h;' + r.pedidos + ' pedidos;\u2014;\u2014;' + $('#scCumpTotal').text() + ';100%;' + $('#scPtsTotal').text() + '\r\n\r\n';

	csv += 'LIQUIDACION SUGERIDA\r\n';
	csv += 'Puntaje Total;' + $('#scTotalPuntos').text() + ' / 100\r\n';
	csv += 'Cumplimiento Global;' + $('#scCumplimientoGlobal').text() + ' (' + $('#scRangoTexto').text() + ')\r\n';
	csv += 'Tarifa por Domicilio;' + $('#scTarifaPedido').text() + '\r\n';
	csv += 'Total a Liquidar;' + $('#scTotalLiquidacion').text() + ' (' + $('#scSubtotalLiquidacion').text() + ')\r\n';

	var blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
	var fileName = 'Desempeno_' + domi.nombrelargo.replace(/\s+/g, '_') + '_' + ddDatos.desde + '_al_' + ddDatos.hasta + '.csv';
	saveAs(blob, fileName);
}

/*
 * Costo por tienda y por concepto: lo que hoy es la pestana PORCENTAJE del Excel.
 *
 * Dos vistas sobre los mismos datos:
 *   - UNA TIENDA: los trece conceptos en las filas y las semanas en las columnas. Es
 *     el detalle semanal, con el total y el porcentaje de la venta del periodo.
 *   - TODAS: las tiendas en las filas y los conceptos en las columnas, en porcentaje.
 *     Es la pestana PORCENTAJE completa, para comparar las once de un vistazo.
 *
 * EL PORCENTAJE VA SOBRE EL TOTAL DEL PERIODO, NO SEMANA POR SEMANA
 *
 * Los egresos se pagan por lotes: en septiembre DOMICILIOS fue 8,8 millones una semana,
 * 0,2 la siguiente y 8,5 la otra. Un porcentaje semanal de eso no dice nada. El detalle
 * semanal se muestra -es lo que se pidio- pero el porcentaje sale del periodo completo.
 */

var datos = null;

function pesos(valor) {
	var n = Math.round(Number(valor) || 0);
	var signo = n < 0 ? '-' : '';
	return signo + '$ ' + Math.abs(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/* Un conteo no lleva signo de pesos: no es plata y no se suma con lo de arriba. */
function numero(valor) {
	return Math.round(Number(valor) || 0).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function pct(valor) {
	return (Number(valor) || 0).toFixed(2).replace('.', ',') + '%';
}

function escapar(texto) {
	return String(texto === null || texto === undefined ? '' : texto)
		.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/*
 * El datepicker de la aplicacion escribe dd/mm/aaaa y el servicio pide aaaa-mm-dd.
 * Si se mandara dd/mm/aaaa el BETWEEN no revienta: devuelve cero filas, y la pantalla
 * quedaria vacia sin decir por que. El servicio tambien lo valida, por si acaso.
 */
function aISO(texto) {
	var t = (texto || '').trim();
	var m = t.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
	if (m) {
		return m[3] + '-' + m[2] + '-' + m[1];
	}
	if (/^\d{4}-\d{2}-\d{2}$/.test(t)) {
		return t;
	}
	return '';
}

function aDDMM(iso) {
	var m = (iso || '').match(/^(\d{4})-(\d{2})-(\d{2})$/);
	return m ? m[3] + '/' + m[2] + '/' + m[1] : iso;
}

function claseOrigen(origen) {
	switch (origen) {
		case 'EGRESO':   return 'ct-egreso';
		case 'TEMPORAL': return 'ct-temporal';
		case 'SEMANAL':  return 'ct-datafono';
		case 'AREA':     return 'ct-area';
		case 'MIXTO':    return 'ct-mixto';
		default:         return 'ct-digitado';
	}
}

$(document).ready(function () {
	// Arranca con las ultimas cuatro semanas completas, que es como se mira siempre.
	var hoy = new Date();
	var hasta = new Date(hoy.getTime());
	hasta.setDate(hasta.getDate() - hasta.getDay()); // el domingo que acaba de pasar
	var desde = new Date(hasta.getTime());
	desde.setDate(desde.getDate() - 21);
	$('#desde').val(aDDMM(aTexto(desde)));
	$('#hasta').val(aDDMM(aTexto(hasta)));
	cargar();
});

function aTexto(fecha) {
	var m = fecha.getMonth() + 1;
	var d = fecha.getDate();
	return fecha.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (d < 10 ? '0' : '') + d;
}

function cargar() {
	var desde = aISO($('#desde').val());
	var hasta = aISO($('#hasta').val());
	if (!desde || !hasta) {
		bootbox.alert('Las fechas tienen que ir en dd/mm/aaaa.');
		return;
	}
	if (desde > hasta) {
		bootbox.alert('El rango empieza despu&eacute;s de terminar.');
		return;
	}
	cerrarDetalle();
	$('#tabla').html('<p class="ct-pie">Consultando&hellip;</p>');
	$.getJSON(server + 'CRUDCostoTienda?idoperacion=1&desde=' + desde + '&hasta=' + hasta
			+ '&idtienda=0', function (d) {
		if (!d || d.respuesta !== 'S') {
			datos = null;
			$('#avisos').html('');
			$('#tabla').html('<p class="ct-pie">' + escapar(d && d.mensaje
				? d.mensaje : 'No se pudo consultar.') + '</p>');
			return;
		}
		datos = d;
		pintarSelectorTiendas();
		pintarAvisos();
		pintar();
	});
}

function pintarSelectorTiendas() {
	var actual = $('#tienda').val();
	var str = '<option value="0">Todas las tiendas (comparativo en %)</option>';
	for (var i = 0; i < datos.tiendas.length; i++) {
		str += '<option value="' + datos.tiendas[i].idtienda + '">'
			+ escapar(datos.tiendas[i].nombre) + '</option>';
	}
	$('#tienda').html(str);
	if (actual) {
		$('#tienda').val(actual);
	}
}

function pintarAvisos() {
	var avisos = '';
	var semanas = datos.semanas.length;
	avisos += '<div class="ct-aviso">El periodo tiene <b>' + semanas + ' semana'
		+ (semanas === 1 ? '' : 's') + '</b> con venta: '
		+ escapar(aDDMM(datos.semanas[0])) + ' a ' + escapar(aDDMM(datos.semanas[semanas - 1]))
		+ '. Los egresos se pagan por lotes, no parejo cada semana, as&iacute; que el '
		+ '<b>porcentaje va sobre el total del periodo</b> y no semana por semana.</div>';
	$('#avisos').html(avisos);
}

function pintar() {
	if (!datos) {
		return;
	}
	if (Number($('#tienda').val() || 0) === 0) {
		pintarComparativo();
	} else {
		pintarUnaTienda(Number($('#tienda').val()));
	}
}

/* Una tienda: conceptos en las filas, semanas en las columnas. */
function pintarUnaTienda(idtienda) {
	var nombre = '';
	for (var i = 0; i < datos.tiendas.length; i++) {
		if (datos.tiendas[i].idtienda === idtienda) {
			nombre = datos.tiendas[i].nombre;
		}
	}
	$('#titulo-tabla').html('Costo por concepto &middot; ' + escapar(nombre));

	// clave concepto|semana -> valor, y la venta y los domicilios de cada semana.
	var celdas = {};
	var ventaSemana = {};
	var domiSemana = {};
	var ventaTotal = 0;
	var domiTotal = 0;
	for (var f = 0; f < datos.filas.length; f++) {
		var fila = datos.filas[f];
		if (fila.idtienda !== idtienda) {
			continue;
		}
		celdas[fila.idconcepto + '|' + fila.semana] = (celdas[fila.idconcepto + '|' + fila.semana] || 0)
			+ fila.valor;
		ventaSemana[fila.semana] = fila.venta;
		domiSemana[fila.semana] = fila.domicilios;
	}
	for (var s = 0; s < datos.semanas.length; s++) {
		ventaTotal += Number(ventaSemana[datos.semanas[s]] || 0);
		domiTotal += Number(domiSemana[datos.semanas[s]] || 0);
	}

	var str = '<table><thead><tr><th>Concepto</th><th>Origen</th>';
	for (var k = 0; k < datos.semanas.length; k++) {
		str += '<th class="ct-num">' + escapar(aDDMM(datos.semanas[k])) + '</th>';
	}
	str += '<th class="ct-num">Total</th><th class="ct-num">% venta</th>'
		+ '<th class="ct-num" title="Solo para los conceptos que dependen del domicilio">'
		+ 'Por domicilio</th></tr></thead><tbody>';

	var granTotal = 0;
	var faltantes = 0;
	for (var c = 0; c < datos.conceptos.length; c++) {
		var concepto = datos.conceptos[c];
		var totalConcepto = 0;
		var celdasHtml = '';
		for (var w = 0; w < datos.semanas.length; w++) {
			var v = Number(celdas[concepto.idconcepto + '|' + datos.semanas[w]] || 0);
			totalConcepto += v;
			celdasHtml += '<td class="ct-num' + (v === 0 ? ' ct-cero' : '') + '">'
				+ (v === 0 ? '&mdash;' : pesos(v)) + '</td>';
		}
		granTotal += totalConcepto;
		// Un concepto sin un solo peso no se muestra en cero: cero se lee como "esto no
		// costo nada", y lo que pasa casi siempre es que falta la fuente. Se dice cual.
		var vacio = totalConcepto === 0;
		if (vacio) {
			faltantes++;
		}
		str += '<tr>'
			+ '<td><span class="ct-clic" onclick="verDetalle(' + idtienda + ','
				+ concepto.idconcepto + ')">' + escapar(concepto.nombre) + '</span></td>'
			+ '<td><span class="ct-chip ' + claseOrigen(concepto.origen) + '">'
				+ escapar(concepto.origen) + '</span></td>'
			+ celdasHtml
			+ '<td class="ct-num">' + (vacio ? '<span class="ct-cero">' + porQueVacio(concepto.origen)
				+ '</span>' : '<b>' + pesos(totalConcepto) + '</b>') + '</td>'
			+ '<td class="ct-num">' + (vacio || ventaTotal <= 0 ? '&mdash;'
				: pct(totalConcepto * 100 / ventaTotal)) + '</td>'
			// Solo tiene sentido en lo que se gasta POR llevar un pedido. Dividir la
			// publicidad entre los domicilios daria un numero, pero no significaria nada.
			+ '<td class="ct-num">' + (!vacio && domiTotal > 0 && porDomicilio(concepto.idconcepto)
				? pesos(totalConcepto / domiTotal) : '&mdash;') + '</td></tr>';
	}

	// La venta va de ultima porque es el denominador, no un costo mas.
	var ventaHtml = '';
	for (var x = 0; x < datos.semanas.length; x++) {
		ventaHtml += '<td class="ct-num">' + pesos(ventaSemana[datos.semanas[x]] || 0) + '</td>';
	}
	str += '<tr class="ct-total"><td>TOTAL COSTO'
		+ (faltantes > 0 ? ' <span class="ct-chip ct-digitado">parcial</span>' : '') + '</td><td></td>';
	for (var y = 0; y < datos.semanas.length; y++) {
		var tv = 0;
		for (var z = 0; z < datos.conceptos.length; z++) {
			tv += Number(celdas[datos.conceptos[z].idconcepto + '|' + datos.semanas[y]] || 0);
		}
		str += '<td class="ct-num">' + pesos(tv) + '</td>';
	}
	str += '<td class="ct-num">' + pesos(granTotal) + '</td><td class="ct-num">'
		+ (ventaTotal > 0 ? pct(granTotal * 100 / ventaTotal) : '&mdash;') + '</td>'
		+ '<td class="ct-num">&mdash;</td></tr>';
	str += '<tr class="ct-total"><td>VENTA</td><td></td>' + ventaHtml
		+ '<td class="ct-num">' + pesos(ventaTotal) + '</td><td class="ct-num">100,00%</td>'
		+ '<td class="ct-num">&mdash;</td></tr>';
	// El conteo no es plata y va de ultimo, para que nadie lo sume con lo de arriba.
	var domiHtml = '';
	for (var w2 = 0; w2 < datos.semanas.length; w2++) {
		domiHtml += '<td class="ct-num">' + numero(domiSemana[datos.semanas[w2]] || 0) + '</td>';
	}
	str += '<tr class="ct-total"><td>DOMICILIOS (cantidad)</td><td></td>' + domiHtml
		+ '<td class="ct-num">' + numero(domiTotal) + '</td><td class="ct-num">&mdash;</td>'
		+ '<td class="ct-num">&mdash;</td></tr>';
	str += '</tbody></table>';
	// El total de abajo no es el costo de la tienda mientras falten conceptos, y hay
	// que decirlo donde se lee el numero, no solo arriba.
	if (faltantes > 0) {
		str += '<p class="ct-pie" style="margin-top:8px"><b>El total est&aacute; incompleto:</b> '
			+ faltantes + ' de los ' + datos.conceptos.length + ' conceptos todav&iacute;a no tienen '
			+ 'valor en este periodo. El costo real de la tienda es mayor que el que se ve aqu&iacute;.</p>';
	}
	$('#tabla').html(str);
}

/*
 * Que conceptos se pueden dividir entre los domicilios.
 *
 * Solo los que se gastan POR llevar un pedido: el rodamiento del domiciliario de la
 * tienda y lo que cobra la empresa temporal. Dividir la publicidad o el arriendo entre
 * los domicilios tambien daria un numero, y no significaria nada.
 */
function porDomicilio(idconcepto) {
	return idconcepto === 5 || idconcepto === 8;
}

/* Por que un concepto sale sin un solo peso. Un cero pelado no lo explica. */
function porQueVacio(origen) {
	switch (origen) {
		case 'AREA':     return 'pendiente';
		case 'DIGITADO': return 'sin digitar';
		case 'MIXTO':    return 'sin digitar';
		case 'SEMANAL':  return 'sin liquidar';
		default:         return 'sin movimientos';
	}
}

/* Todas las tiendas: tiendas en las filas, conceptos en las columnas, en porcentaje. */
function pintarComparativo() {
	$('#titulo-tabla').html('Comparativo de las tiendas, en % de su venta');

	// clave tienda|concepto -> valor, y la venta total de cada tienda.
	var celdas = {};
	var ventaTienda = {};
	for (var f = 0; f < datos.filas.length; f++) {
		var fila = datos.filas[f];
		var clave = fila.idtienda + '|' + fila.idconcepto;
		celdas[clave] = (celdas[clave] || 0) + fila.valor;
	}
	// La venta se suma una sola vez por tienda y semana: en filas viene repetida por
	// cada concepto, y sumarla tal cual la multiplicaria por el numero de conceptos.
	var vistas = {};
	for (var g = 0; g < datos.filas.length; g++) {
		var fl = datos.filas[g];
		var k = fl.idtienda + '|' + fl.semana;
		if (vistas[k]) {
			continue;
		}
		vistas[k] = true;
		ventaTienda[fl.idtienda] = (ventaTienda[fl.idtienda] || 0) + Number(fl.venta || 0);
	}

	var str = '<table><thead><tr><th>Tienda</th>';
	for (var c = 0; c < datos.conceptos.length; c++) {
		str += '<th class="ct-num" title="' + escapar(datos.conceptos[c].nombre) + '">'
			+ abreviar(datos.conceptos[c].nombre) + '</th>';
	}
	str += '<th class="ct-num">TOTAL</th><th class="ct-num">Venta</th></tr></thead><tbody>';

	for (var t = 0; t < datos.tiendas.length; t++) {
		var idt = datos.tiendas[t].idtienda;
		var venta = Number(ventaTienda[idt] || 0);
		var total = 0;
		var fila = '';
		for (var x = 0; x < datos.conceptos.length; x++) {
			var v = Number(celdas[idt + '|' + datos.conceptos[x].idconcepto] || 0);
			total += v;
			fila += '<td class="ct-num' + (v === 0 ? ' ct-cero' : '') + '">'
				+ (v === 0 ? '&mdash;'
					: '<span class="ct-clic" onclick="verDetalle(' + idt + ','
						+ datos.conceptos[x].idconcepto + ')">'
						+ (venta > 0 ? pct(v * 100 / venta) : pesos(v)) + '</span>')
				+ '</td>';
		}
		str += '<tr><td>' + escapar(datos.tiendas[t].nombre) + '</td>' + fila
			+ '<td class="ct-num"><b>' + (venta > 0 ? pct(total * 100 / venta) : '&mdash;') + '</b></td>'
			+ '<td class="ct-num">' + pesos(venta) + '</td></tr>';
	}
	str += '</tbody></table>';
	// Las columnas con raya en TODAS las tiendas son conceptos que todavia no tienen
	// fuente. Mientras existan, el TOTAL no es el costo de la tienda.
	var sinFuente = [];
	for (var c2 = 0; c2 < datos.conceptos.length; c2++) {
		var hay = false;
		for (var t2 = 0; t2 < datos.tiendas.length; t2++) {
			if (Number(celdas[datos.tiendas[t2].idtienda + '|' + datos.conceptos[c2].idconcepto] || 0) !== 0) {
				hay = true;
				break;
			}
		}
		if (!hay) {
			sinFuente.push(datos.conceptos[c2].nombre);
		}
	}
	var pie = '<p class="ct-pie" style="margin-top:8px">Cada celda es el concepto como % de la venta '
		+ 'de esa tienda en el periodo. Oprima una para ver los movimientos que la sostienen, o '
		+ 'escoja una tienda arriba para ver el detalle semana por semana.</p>';
	if (sinFuente.length > 0) {
		pie += '<p class="ct-pie"><b>El TOTAL est&aacute; incompleto.</b> Estos '
			+ sinFuente.length + ' conceptos no tienen valor en ninguna tienda todav&iacute;a: '
			+ escapar(sinFuente.join(', ')) + '. El costo real es mayor que el que se ve aqu&iacute;.</p>';
	}
	$('#tabla').html(str + pie);
}

/*
 * Los encabezados completos no caben en trece columnas.
 *
 * Lo que vuelve lleva entidades HTML y NO pasa por escapar(): son constantes de aqui,
 * no datos. Si se escaparan, el & se volveria &amp; y en pantalla saldria "N&oacute;mina".
 */
function abreviar(nombre) {
	var cortes = {
		'COMISION VIRTUAL': 'Com. virtual',
		'NOMINA PUNTO VENTA Y TEMPORAL': 'N&oacute;mina PV',
		'VARIOS PUNTO VENTA': 'Varios PV',
		'PREMIOS': 'Premios',
		'DOMICILIOS': 'Domicilios',
		'MISCELANEA': 'Miscel&aacute;nea',
		'CUOTA PUNTO VENTA': 'Cuota PV',
		'MENSAJERIA EXTERNA': 'Mensajer&iacute;a',
		'CUOTA ADMINISTRATIVA': 'Cuota adm.',
		'CUOTA LOGISTICA Y PRODUCCION': 'Cuota log.',
		'NOMINA CONTACT': 'N&oacute;m. contact',
		'PUBLICIDAD Y MERCADEO': 'Publicidad',
		'COMISION DATAFONOS': 'Com. dat&aacute;fono'
	};
	return cortes[nombre] || nombre;
}

function verDetalle(idtienda, idconcepto) {
	var desde = aISO($('#desde').val());
	var hasta = aISO($('#hasta').val());
	var nombreConcepto = '';
	for (var c = 0; c < datos.conceptos.length; c++) {
		if (datos.conceptos[c].idconcepto === idconcepto) {
			nombreConcepto = datos.conceptos[c].nombre;
		}
	}
	var nombreTienda = '';
	for (var t = 0; t < datos.tiendas.length; t++) {
		if (datos.tiendas[t].idtienda === idtienda) {
			nombreTienda = datos.tiendas[t].nombre;
		}
	}
	$('#titulo-detalle').html(escapar(nombreConcepto) + ' &middot; ' + escapar(nombreTienda));
	$('#detalle').html('<p class="ct-pie">Consultando&hellip;</p>');
	$('#caja-detalle').show();

	$.getJSON(server + 'CRUDCostoTienda?idoperacion=4&desde=' + desde + '&hasta=' + hasta
			+ '&idtienda=' + idtienda + '&idconcepto=' + idconcepto, function (d) {
		if (!d || d.respuesta !== 'S') {
			$('#detalle').html('<p class="ct-pie">' + escapar(d && d.mensaje
				? d.mensaje : 'No se pudo consultar el detalle.') + '</p>');
			return;
		}
		if (!d.renglones || d.renglones.length === 0) {
			$('#detalle').html('<p class="ct-pie">Este concepto no tiene movimientos en el periodo.'
				+ (d.origen === 'DIGITADO'
					? ' Es un concepto que se digita por tienda y mes, y nadie lo ha digitado.'
					: '') + '</p>');
			return;
		}
		var str = '';
		if (d.totalaproximado === 'S') {
			str += '<div class="ct-aviso">Estos turnos se recalculan con la tarifa vigente hoy. '
				+ 'El total de la tabla de arriba lo escribi&oacute; el proceso semanal con las '
				+ 'tarifas que reg&iacute;an ese d&iacute;a, as&iacute; que pueden no dar iguales.</div>';
		}
		if (d.digitadoesdelmes === 'S') {
			str += '<div class="ct-aviso">Lo digitado es del <b>mes completo</b>. Si el rango corta '
				+ 'un mes, arriba sale solo la parte de las semanas consultadas.</div>';
		}
		str += '<table><thead><tr><th>Fecha</th><th>Qu&eacute; fue</th><th>Referencia</th>'
			+ '<th class="ct-num">Valor</th></tr></thead><tbody>';
		for (var i = 0; i < d.renglones.length; i++) {
			var r = d.renglones[i];
			str += '<tr><td>' + escapar(aDDMM(r.fecha)) + '</td>'
				+ '<td>' + escapar(r.descripcion) + '</td>'
				+ '<td>' + escapar(r.referencia) + '</td>'
				+ '<td class="ct-num">' + pesos(r.valor) + '</td></tr>';
		}
		str += '<tr class="ct-total"><td colspan="3">TOTAL (' + d.renglones.length
			+ ' movimiento' + (d.renglones.length === 1 ? '' : 's') + ')</td>'
			+ '<td class="ct-num">' + pesos(d.total) + '</td></tr>';
		str += '</tbody></table>';
		$('#detalle').html(str);
	});
}

function cerrarDetalle() {
	$('#caja-detalle').hide();
}

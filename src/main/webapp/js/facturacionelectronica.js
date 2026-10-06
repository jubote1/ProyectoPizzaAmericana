/*
 * Reporte mensual de facturacion electronica: facturado, notas credito y neto por tienda.
 *
 * Todo en ES5 -var y function, nada de arrow ni let-: la pantalla convive con jQuery 1.11.
 */

$(document).ready(function () {

	//Por defecto el mes pasado: es el que se concilia con la contabilidad.
	var hoy = new Date();
	var anterior = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
	$('#feMes').val(feMesISO(anterior));

	$('#feConsultar').click(feConsultar);
	$('#feCsv').click(function () {
		var mes = $('#feMes').val();
		if (!mes) {
			$('#feMensaje').html('<div class="alert alert-warning">Elija un mes.</div>');
			return;
		}
		window.location = server + 'ConsultarFacturacionElectronica?formato=csv&mes=' + encodeURIComponent(mes);
	});
});

function feMesISO(fecha) {
	var mes = fecha.getMonth() + 1;
	return fecha.getFullYear() + '-' + (mes < 10 ? '0' : '') + mes;
}

function feEscapar(texto) {
	return ($('<div/>').text(texto === null || texto === undefined ? '' : texto).html());
}

function fePesos(valor) {
	var n = Math.round(Number(valor) || 0);
	var texto = Math.abs(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	return (n < 0 ? '-$ ' : '$ ') + texto;
}

function feConsultar() {
	var mes = $('#feMes').val();
	$('#feMensaje').empty();
	if (!mes) {
		$('#feMensaje').html('<div class="alert alert-warning">Elija un mes.</div>');
		return;
	}
	$('#feResultado').hide();
	$('#feCargando').show();
	$.ajax({
		url: server + 'ConsultarFacturacionElectronica',
		dataType: 'json',
		type: 'get',
		data: { mes: mes },
		success: function (d) {
			$('#feCargando').hide();
			if (d.error) {
				$('#feMensaje').html('<div class="alert alert-danger">' + feEscapar(d.error) + '</div>');
				return;
			}
			fePintar(d);
		},
		error: function () {
			$('#feCargando').hide();
			$('#feMensaje').html('<div class="alert alert-danger">No se pudo consultar el reporte.</div>');
		}
	});
}

function feKpi(valor, rotulo, apoyo, clase) {
	return '<div class="col-md-3 col-sm-6"><div class="fe-kpi"><div class="valor ' + (clase || '') + '">' + valor +
		'</div><div class="rotulo">' + rotulo + '</div><div class="apoyo">' + (apoyo || '&nbsp;') + '</div></div></div>';
}

function fePintar(d) {
	var t = d.totales;

	//Aviso de cobertura: un mes que parece completo y no lo es seria peor que no tener reporte.
	var aviso = '';
	if (d.tiendas_sin_cobertura_completa > 0) {
		aviso = '<div class="alert alert-warning">La réplica al datamart no tiene copiados todos los días del mes en ' +
			d.tiendas_sin_cobertura_completa + ' tienda(s) (columna Alertas). Las cifras de esas tiendas pueden estar ' +
			'incompletas: corra la réplica histórica para traer los días que faltan y vuelva a consultar.</div>';
	}
	var sinValor = t.facturas.sin_valor + t.notas.sin_valor;
	if (sinValor > 0) {
		aviso += '<div class="alert alert-warning">' + sinValor + ' documento(s) no tienen valor guardado ni pedido en el ' +
			'datamart para valorarlos: no están sumados.</div>';
	}
	$('#feAviso').html(aviso);

	$('#feKpis').html(
		feKpi(fePesos(t.facturas.total), 'Facturado', t.facturas.cantidad + ' factura(s)') +
		feKpi((t.notas.total > 0 ? '-' : '') + fePesos(t.notas.total), 'Notas crédito', t.notas.cantidad + ' nota(s)', 'fe-menos') +
		feKpi(fePesos(t.neto_total), 'Neto facturado', 'con impuestos', 'fe-neto') +
		feKpi(fePesos(t.neto_base), 'Neto sin impuestos', 'impuesto neto ' + fePesos(t.neto_impuesto))
	);

	var cuerpo = '';
	for (var i = 0; i < d.tiendas.length; i++) {
		var f = d.tiendas[i];
		var alertas = [];
		if (f.dias_con_datos < d.dias_del_mes) {
			alertas.push('réplica ' + f.dias_con_datos + '/' + d.dias_del_mes + ' días');
		}
		if (f.facturas_no_aceptadas > 0) {
			alertas.push(f.facturas_no_aceptadas + ' factura(s) no aceptada(s) por la DIAN');
		}
		if (f.notas_no_aceptadas > 0) {
			alertas.push(f.notas_no_aceptadas + ' nota(s) no aceptada(s)');
		}
		if (f.facturas.sin_valor + f.notas.sin_valor > 0) {
			alertas.push((f.facturas.sin_valor + f.notas.sin_valor) + ' sin valor');
		}
		cuerpo += '<tr><td>' + feEscapar(f.nombre) + '</td>' +
			'<td class="fe-der">' + f.facturas.cantidad + '</td>' +
			'<td class="fe-der">' + fePesos(f.facturas.total) + '</td>' +
			'<td class="fe-der">' + f.notas.cantidad + '</td>' +
			'<td class="fe-der ' + (f.notas.total > 0 ? 'fe-menos' : '') + '">' + (f.notas.total > 0 ? '-' : '') + fePesos(f.notas.total) + '</td>' +
			'<td class="fe-der fe-neto">' + fePesos(f.neto_total) + '</td>' +
			'<td class="fe-der">' + fePesos(f.neto_impuesto) + '</td>' +
			'<td class="fe-alerta">' + feEscapar(alertas.join(' · ')) + '</td></tr>';
	}
	$('#feTabla tbody').html(cuerpo);
	$('#feTabla tfoot').html('<tr><td>TOTAL</td>' +
		'<td class="fe-der">' + t.facturas.cantidad + '</td>' +
		'<td class="fe-der">' + fePesos(t.facturas.total) + '</td>' +
		'<td class="fe-der">' + t.notas.cantidad + '</td>' +
		'<td class="fe-der fe-menos">' + (t.notas.total > 0 ? '-' : '') + fePesos(t.notas.total) + '</td>' +
		'<td class="fe-der">' + fePesos(t.neto_total) + '</td>' +
		'<td class="fe-der">' + fePesos(t.neto_impuesto) + '</td><td></td></tr>');

	var filas = '';
	for (var j = 0; j < d.notas_detalle.length; j++) {
		var n = d.notas_detalle[j];
		filas += '<tr' + (n.factura_de_otro_mes ? ' style="color:#ef6c00;font-weight:bold;"' : '') + '>' +
			'<td>' + feEscapar(n.fecha) + '</td>' +
			'<td>' + feEscapar(n.tienda) + '</td>' +
			'<td>' + feEscapar(n.nota) + '</td>' +
			'<td>' + feEscapar(n.factura) + '</td>' +
			'<td>' + feEscapar(n.fecha_factura || 'no está en el datamart') + '</td>' +
			'<td>' + n.pedido + '</td>' +
			'<td>' + feEscapar(n.motivo) + '</td>' +
			'<td class="fe-der">' + fePesos(n.total) + '</td></tr>';
	}
	if (filas === '') {
		filas = '<tr><td colspan="8" class="text-center text-muted">No hubo notas crédito en el mes.</td></tr>';
	}
	$('#feNotas tbody').html(filas);

	$('#feResultado').show();
}

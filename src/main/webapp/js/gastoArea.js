/*
 * Pantalla de gastos por area: publicidad, administrativa, logistica y contact center.
 *
 * El mes manda en todo. Un gasto fijo solo cuenta si estaba vigente ese mes, y un
 * gasto variable vale lo que se haya digitado PARA ese mes. Por eso al cambiar el mes
 * se vuelve a cargar la tabla entera y no solo el resumen.
 *
 * El valor de un gasto variable se edita en la misma tabla, en la columna del mes que
 * se este mirando. Se guarda en gasto_area_mes, no encima del gasto, para que un mes
 * ya repartido entre las tiendas no cambie porque alguien digite el siguiente.
 */

var MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
             'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

function pesos(valor) {
	var n = Math.round(Number(valor) || 0);
	return '$ ' + n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function escapar(texto) {
	return String(texto === null || texto === undefined ? '' : texto)
		.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

$(document).ready(function () {
	var hoy = new Date();
	$('#anio').val(hoy.getFullYear());
	var opcionesMes = '';
	for (var i = 0; i < 12; i++) {
		opcionesMes += '<option value="' + (i + 1) + '"' + (i === hoy.getMonth() ? ' selected' : '') + '>'
			+ MESES[i] + '</option>';
	}
	$('#mes').html(opcionesMes);

	$.getJSON(server + 'CRUDGastoArea?idoperacion=5', function (data) {
		var str = '';
		for (var i = 0; i < data.length; i++) {
			str += '<option value="' + data[i].area + '">' + data[i].area + '</option>';
		}
		$('#area').html(str);
		cargar();
	});
});

function periodo() {
	return 'area=' + encodeURIComponent($('#area').val())
		+ '&anio=' + $('#anio').val() + '&mes=' + $('#mes').val();
}

function cargar() {
	cerrarForm();
	cargarResumen();
	$.getJSON(server + 'CRUDGastoArea?idoperacion=4&' + periodo(), function (data) {
		pintarTabla(data);
	});
}

function cargarResumen() {
	var url = server + 'CRUDGastoArea?idoperacion=6&' + periodo()
		+ '&semanas=' + ($('#semanas').val() || 4) + '&tiendas=' + ($('#tiendas').val() || 11);
	$.getJSON(url, function (d) {
		$('#resumen').html(
			'<div><div class="k">Total del mes</div><div class="v">' + pesos(d.total) + '</div></div>'
			+ '<div><div class="k">Por semana (' + d.semanas + ')</div><div class="v">' + pesos(d.totalsemanal) + '</div></div>'
			+ '<div><div class="k">Por tienda y semana (' + d.tiendas + ')</div><div class="v">'
			+ pesos(d.totalsemanalportienda) + '</div></div>');
	});
}

function pintarTabla(data) {
	var nombreMes = MESES[Number($('#mes').val()) - 1];
	var str = '<table><thead><tr>'
		+ '<th>Concepto</th><th>Clase</th><th>Vigencia</th>'
		+ '<th class="ga-num">Valor de ' + nombreMes + '</th>'
		+ '<th>Estado</th><th></th></tr></thead><tbody>';
	if (!data || data.length === 0) {
		str += '<tr><td colspan="6">Esta &aacute;rea todav&iacute;a no tiene gastos registrados.</td></tr>';
	}
	var total = 0;
	for (var i = 0; i < data.length; i++) {
		var g = data[i];
		var esVariable = (g.tipo === 'V');
		var vigente = (g.vigente === 'S');
		if (vigente) {
			total += Number(g.valormes) || 0;
		}
		var vigencia = escapar(g.vigenciadesde)
			+ (g.vigenciahasta ? ' a ' + escapar(g.vigenciahasta) : ' en adelante');

		var celdaValor;
		if (esVariable) {
			// El variable se digita aqui mismo, para el mes que se este mirando.
			celdaValor = '<input type="number" min="0" step="1" style="width:140px; text-align:right"'
				+ ' value="' + (Number(g.valormes) || 0) + '"'
				+ ' onchange="guardarValorMes(' + g.idgasto + ', this.value)" />'
				+ (g.tienevalordelmes === 'S' ? '' : ' <span class="ga-chip ga-ojo">sin digitar</span>');
		} else {
			celdaValor = pesos(g.valormes);
		}

		var estado;
		if (g.activo !== 'S') {
			estado = '<span class="ga-chip ga-no">inactivo</span>';
		} else if (!vigente) {
			estado = '<span class="ga-chip ga-ojo">fuera de vigencia</span>';
		} else {
			estado = '<span class="ga-chip ga-si">cuenta este mes</span>';
		}

		str += '<tr>'
			+ '<td>' + escapar(g.concepto) + '</td>'
			+ '<td>' + (esVariable ? 'Variable' : 'Fijo') + '</td>'
			+ '<td>' + vigencia + '</td>'
			+ '<td class="ga-num">' + celdaValor + '</td>'
			+ '<td>' + estado + '</td>'
			+ '<td><button class="btn btn-default btn-xs" onclick="editarGasto(' + g.idgasto + ')">Editar</button> '
			+ '<button class="btn btn-default btn-xs" onclick="eliminarGasto(' + g.idgasto + ', \''
			+ escapar(g.concepto).replace(/'/g, "\\'") + '\')">Eliminar</button></td>'
			+ '</tr>';
	}
	str += '</tbody></table>';
	$('#tabla-gastos').html(str);
	window.gastosCargados = data;
}

function nuevoGasto() {
	$('#titulo-form').html('Nuevo gasto');
	$('#idgasto').val(0);
	$('#concepto').val('');
	$('#tipo').val('F');
	$('#valormes').val(0);
	$('#activo').val('S');
	$('#vigenciadesde').val('');
	$('#vigenciahasta').val('');
	pintarTipo();
	$('#caja-form').show();
}

function editarGasto(idgasto) {
	var lista = window.gastosCargados || [];
	for (var i = 0; i < lista.length; i++) {
		if (Number(lista[i].idgasto) === Number(idgasto)) {
			var g = lista[i];
			$('#titulo-form').html('Editar: ' + escapar(g.concepto));
			$('#idgasto').val(g.idgasto);
			$('#concepto').val(g.concepto);
			$('#tipo').val(g.tipo);
			// Un variable no tiene valor mensual propio: el de la tabla es el del mes.
			$('#valormes').val(g.tipo === 'V' ? 0 : (Number(g.valormes) || 0));
			$('#activo').val(g.activo);
			$('#vigenciadesde').val(g.vigenciadesde);
			$('#vigenciahasta').val(g.vigenciahasta);
			pintarTipo();
			$('#caja-form').show();
			return;
		}
	}
}

function pintarTipo() {
	var esVariable = ($('#tipo').val() === 'V');
	$('#caja-valor').toggle(!esVariable);
	$('#pie-variable').toggle(esVariable);
}

function cerrarForm() {
	$('#caja-form').hide();
}

function guardarGasto() {
	var datos = 'idoperacion=1'
		+ '&idgasto=' + $('#idgasto').val()
		+ '&area=' + encodeURIComponent($('#area').val())
		+ '&concepto=' + encodeURIComponent($('#concepto').val())
		+ '&tipo=' + $('#tipo').val()
		+ '&valormes=' + (Number($('#valormes').val()) || 0)
		+ '&vigenciadesde=' + encodeURIComponent($('#vigenciadesde').val())
		+ '&vigenciahasta=' + encodeURIComponent($('#vigenciahasta').val())
		+ '&activo=' + $('#activo').val();
	$.getJSON(server + 'CRUDGastoArea?' + datos, function (d) {
		if (d.respuesta === 'S') {
			cargar();
		} else {
			bootbox.alert(d.mensaje || 'No se pudo guardar el gasto.');
		}
	});
}

function eliminarGasto(idgasto, concepto) {
	bootbox.confirm('¿Eliminar "' + concepto + '"?<br><br>'
		+ 'Eliminarlo cambia hacia atr&aacute;s lo que cost&oacute; cada periodo. Si el gasto ya dej&oacute; de '
		+ 'correr pero exist&iacute;a antes, es mejor ponerle una fecha de fin en la vigencia.',
		function (ok) {
			if (!ok) {
				return;
			}
			$.getJSON(server + 'CRUDGastoArea?idoperacion=2&idgasto=' + idgasto, function (d) {
				if (d.respuesta === 'S') {
					cargar();
				} else {
					bootbox.alert(d.mensaje || 'No se pudo eliminar el gasto.');
				}
			});
		});
}

function guardarValorMes(idgasto, valor) {
	var datos = 'idoperacion=7&idgasto=' + idgasto
		+ '&anio=' + $('#anio').val() + '&mes=' + $('#mes').val()
		+ '&valor=' + (Number(valor) || 0);
	$.getJSON(server + 'CRUDGastoArea?' + datos, function (d) {
		if (d.respuesta === 'S') {
			cargar();
		} else {
			bootbox.alert(d.mensaje || 'No se pudo guardar el valor del mes.');
		}
	});
}

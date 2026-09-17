/*
 * Ingreso de una solicitud de conciliacion, con el panel que muestra lo que
 * tiene la tienda para poder contrastar sin salir de la pantalla.
 *
 * El panel reusa el servicio ConsultarPagosTiendaConciliacion, el mismo que ya
 * usa la pantalla de consulta de solicitudes. Consulta la base LOCAL de la
 * tienda, asi que necesita el computador de la tienda encendido y puede
 * tardar unos segundos.
 *
 * Todo en ES5 -var y function-: la pantalla carga con jQuery 1.11.
 */

var server;
var tiendas;
var administrador = "N";

$(document).ready(function () {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	//Marcamos si es administrador para tomar ciertas acciones
	if (respuesta == 'OKA') {
		administrador = 'S';
	} else if (respuesta == 'OK') {
		administrador = 'N';
	}

	//Aviso en vivo de lo que se va a guardar. Es la defensa principal contra el
	//punto y la coma: el usuario ve el numero antes de oprimir el boton.
	$('#valoranalizar').on('input blur', function () {
		mostrarAyudaValor($(this).val());
	});

	//Si cambia la tienda o el origen, lo que se veia en el panel ya no
	//corresponde: se limpia en vez de dejar una tabla que engana.
	$('#selectTiendas, #selectOrigen').change(function () {
		limpiarPagos();
	});

	//El usuario que toca las fechas del panel manda; deja de seguir a la fecha
	//de la transaccion.
	$('#pagos-desde, #pagos-hasta').change(function () {
		$('#pagos-desde').data('auto', false);
	});
});

$(function () {
	getListaTiendas();
	setInterval('validarVigenciaLogueo()', 600000);
});

function validarVigenciaLogueo() {
	var resp = '';
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function (data) {
			resp = data[0].respuesta;
		}
	});
	switch (resp) {
		case 'OK':
			break;
		case 'OKA':
			break;
		case 'OKP':
			break;
		default:
			location.href = server + "Index.html";
			break;
	}
}

function getListaTiendas() {
	$.getJSON(server + 'GetTiendas', function (data) {
		tiendas = data;
		var str = '<option value=""></option>';
		for (var i = 0; i < data.length; i++) {
			var cadaTienda = data[i];
			str += '<option value="' + cadaTienda.nombre + '" id ="' + cadaTienda.id + '">' + cadaTienda.nombre + '</option>';
		}
		$('#selectTiendas').html(str);
	});
}

// ===========================================================================
// El valor
// ===========================================================================

/*
 * Misma convencion que el servidor: punto para los miles, coma para los
 * decimales. Un punto solo es ambiguo -"150.000" son ciento cincuenta mil pero
 * "1377.5" son mil trescientos setenta y siete con cinco-, y se decide por
 * cuantos digitos quedan detras: tres son miles, uno o dos son decimales.
 *
 * Esto es un espejo de leerValor en InsertarSolicitudConciliacion. Si alguna
 * de las dos cambia, la otra tambien: aqui solo sirve para avisar, quien
 * decide de verdad es el servidor.
 */
function normalizarValor(texto) {
	if (texto === null || texto === undefined) {
		return (null);
	}
	var limpio = String(texto).replace(/\$/g, '').replace(/\s/g, '');
	if (limpio === '') {
		return (null);
	}
	if (limpio.indexOf(',') >= 0) {
		limpio = limpio.replace(/\./g, '').replace(',', '.');
	} else {
		var primerPunto = limpio.indexOf('.');
		var ultimoPunto = limpio.lastIndexOf('.');
		var digitosFinales = limpio.length - ultimoPunto - 1;
		if (primerPunto >= 0 && primerPunto === ultimoPunto && digitosFinales > 0 && digitosFinales <= 2) {
			//Un solo punto con uno o dos digitos detras: es decimal, se deja.
		} else {
			limpio = limpio.replace(/\./g, '');
		}
	}
	if (!/^[0-9]+(\.[0-9]+)?$/.test(limpio)) {
		return (null);
	}
	var numero = parseFloat(limpio);
	if (isNaN(numero) || numero <= 0) {
		return (null);
	}
	return (numero);
}

/* De 150000 a "150.000", que es como se lee aca. */
function pesos(valor) {
	var numero = Number(valor) || 0;
	var entero = Math.floor(Math.abs(numero));
	var decimales = Math.round((Math.abs(numero) - entero) * 100);
	var texto = String(entero);
	var conPuntos = '';
	for (var i = 0; i < texto.length; i++) {
		if (i > 0 && (texto.length - i) % 3 === 0) {
			conPuntos += '.';
		}
		conPuntos += texto.charAt(i);
	}
	if (decimales > 0) {
		conPuntos += ',' + (decimales < 10 ? '0' : '') + decimales;
	}
	return ('$' + conPuntos);
}

function mostrarAyudaValor(texto) {
	var ayuda = $('#ayuda-valor');
	var campo = $('#valoranalizar');
	if (texto === null || String(texto).replace(/\s/g, '') === '') {
		ayuda.removeClass('cc-ayuda-mal cc-ayuda-ok')
			.html('El punto separa los miles y la coma los decimales.');
		campo.removeClass('cc-error');
		return;
	}
	var valor = normalizarValor(texto);
	if (valor === null) {
		ayuda.removeClass('cc-ayuda-ok').addClass('cc-ayuda-mal')
			.html('No se entiende ese valor. Escríbalo como 150.000 o 150.000,50');
		campo.addClass('cc-error');
		return;
	}
	ayuda.removeClass('cc-ayuda-mal').addClass('cc-ayuda-ok')
		.html('Se va a guardar ' + pesos(valor));
	campo.removeClass('cc-error');
}

// ===========================================================================
// Guardar la solicitud
// ===========================================================================

function avisar(id, texto, clase) {
	$('#' + id).removeClass('cc-aviso-ok cc-aviso-mal cc-aviso-info')
		.addClass(clase).html(texto).show();
}

function esconder(id) {
	$('#' + id).hide();
}

function solicitarConciliacion() {
	var fechaTran = $("#fechatransaccion").val();
	var origen = $("#selectOrigen").val();
	var tienda = $("#selectTiendas").val();
	var idtienda = $("#selectTiendas option:selected").attr('id');
	var numpedido = $("#numeropedido").val();
	var descripcion = $("#descripcion").val();
	var categoria = $("#selectCategoria").val();
	var telefono = $("#telefono").val();

	if (fechaTran === '' || fechaTran === null) {
		avisar('aviso-solicitud', 'La fecha de transacción no puede estar vacía.', 'cc-aviso-mal');
		return;
	}
	if (!existeFecha(fechaTran)) {
		avisar('aviso-solicitud', 'La fecha de transacción no es correcta.', 'cc-aviso-mal');
		return;
	}
	if (tienda === '' || tienda === null) {
		avisar('aviso-solicitud', 'Falta la tienda.', 'cc-aviso-mal');
		return;
	}
	if (origen === '' || origen === null) {
		avisar('aviso-solicitud', 'Falta el origen de la transacción.', 'cc-aviso-mal');
		return;
	}
	if (numpedido === '' || numpedido === null) {
		avisar('aviso-solicitud', 'Falta el número de pedido.', 'cc-aviso-mal');
		return;
	}
	if (descripcion === '' || descripcion === null) {
		avisar('aviso-solicitud', 'Falta la descripción.', 'cc-aviso-mal');
		return;
	}
	if (categoria === '' || categoria === null) {
		avisar('aviso-solicitud', 'Falta la categoría.', 'cc-aviso-mal');
		return;
	}
	if (telefono === '' || telefono === null) {
		avisar('aviso-solicitud', 'Falta el teléfono del cliente.', 'cc-aviso-mal');
		return;
	}

	//El valor se manda ya normalizado -punto decimal, sin separador de miles-
	//para que no dependa de como lo escribieron. El servidor igual lo vuelve a
	//leer con la misma regla: esta pantalla no es la unica puerta.
	var valor = normalizarValor($("#valoranalizar").val());
	if (valor === null) {
		avisar('aviso-solicitud',
			'Revise el valor a analizar. Escríbalo como 150.000 o 150.000,50', 'cc-aviso-mal');
		$('#valoranalizar').addClass('cc-error').focus();
		return;
	}

	$('#btn-solicitar').prop('disabled', true);
	avisar('aviso-solicitud', 'Guardando...', 'cc-aviso-info');

	$.ajax({
		url: server + 'InsertarSolicitudConciliacion',
		data: {
			fecha: fechaTran,
			idtienda: idtienda,
			numpedido: numpedido,
			origen: origen,
			descripcion: descripcion,
			categoria: categoria,
			valoranalizar: valor,
			telefono: telefono
		},
		dataType: 'json',
		success: function (data2) {
			$('#btn-solicitar').prop('disabled', false);
			if (data2 && data2.error === 'VALORMALO') {
				avisar('aviso-solicitud',
					'El servidor no entendió el valor. Escríbalo como 150.000', 'cc-aviso-mal');
				return;
			}
			if (data2 && data2.respuesta) {
				esconder('aviso-solicitud');
				$.alert('Se ha insertado correctamente la solicitud de Conciliación por ' +
					pesos(valor) + '.');
				limpiarFormulario();
			} else {
				avisar('aviso-solicitud', 'No se pudo guardar la solicitud.', 'cc-aviso-mal');
			}
		},
		error: function () {
			$('#btn-solicitar').prop('disabled', false);
			avisar('aviso-solicitud', 'No se pudo guardar. Revise la conexión e intente de nuevo.',
				'cc-aviso-mal');
		}
	});
}

// ===========================================================================
// Lo que tiene la tienda
// ===========================================================================

function limpiarPagos() {
	$('#grid-pagos tbody').empty();
	$('#pagos-resumen').html('');
	esconder('aviso-pagos');
	$('#pagos-vacio').show();
}

function consultarPagosTienda() {
	var idtienda = $("#selectTiendas option:selected").attr('id');
	var origen = $("#selectOrigen").val();
	var desde = $('#pagos-desde').val();
	var hasta = $('#pagos-hasta').val();

	if (!idtienda) {
		avisar('aviso-pagos', 'Escoja primero la tienda, a la izquierda.', 'cc-aviso-mal');
		return;
	}
	if (origen === '' || origen === null) {
		avisar('aviso-pagos', 'Escoja el origen (QR o DATAFONO), a la izquierda.', 'cc-aviso-mal');
		return;
	}
	if (!validarFecha(desde) || !validarFecha(hasta)) {
		avisar('aviso-pagos', 'Revise las fechas del panel.', 'cc-aviso-mal');
		return;
	}
	if (aComparable(hasta) < aComparable(desde)) {
		avisar('aviso-pagos', 'La fecha hasta no puede ser anterior a la fecha desde.', 'cc-aviso-mal');
		return;
	}

	$('#grid-pagos tbody').empty();
	$('#pagos-vacio').hide();
	$('#pagos-resumen').html('');
	$('#btn-pagos').prop('disabled', true);
	avisar('aviso-pagos',
		'Conectando con el computador de la tienda. Si está apagado puede tardar unos segundos...',
		'cc-aviso-info');

	$.ajax({
		url: server + 'ConsultarPagosTiendaConciliacion',
		data: { idtienda: idtienda, origen: origen, fechadesde: desde, fechahasta: hasta },
		dataType: 'json',
		success: function (datos) {
			$('#btn-pagos').prop('disabled', false);
			if (datos && datos.error) {
				avisar('aviso-pagos', datos.error, 'cc-aviso-mal');
				$('#pagos-vacio').show();
				return;
			}
			pintarPagos((datos && datos.pagos) ? datos.pagos : []);
		},
		error: function () {
			$('#btn-pagos').prop('disabled', false);
			avisar('aviso-pagos', 'No se pudo consultar. Revise la conexión e intente de nuevo.',
				'cc-aviso-mal');
			$('#pagos-vacio').show();
		}
	});
}

function pintarPagos(pagos) {
	var cuerpo = $('#grid-pagos tbody');
	cuerpo.empty();

	if (pagos.length === 0) {
		avisar('aviso-pagos',
			'La tienda respondió, pero no tiene pagos de ese origen en esas fechas.', 'cc-aviso-ok');
		$('#pagos-vacio').show();
		return;
	}
	esconder('aviso-pagos');

	var suma = 0;
	var anulados = 0;
	for (var i = 0; i < pagos.length; i++) {
		var p = pagos[i];
		if (!p.anulado) {
			suma += Number(p.valorformapago) || 0;
		} else {
			anulados++;
		}
		var fila = $('<tr>');
		fila.append($('<td>').text(p.idpedidotienda));
		fila.append($('<td>').text(p.fecha));
		fila.append($('<td>').text(p.nombrecliente || ''));
		fila.append($('<td>').text(p.telefono || ''));
		fila.append($('<td class="cc-num">').text(pesos(p.valorformapago)));
		fila.append($('<td class="cc-num">').text(pesos(p.totalneto)));
		fila.append($('<td>').text(p.referenciadatafono || ''));
		fila.append($('<td>').append(p.anulado
			? $('<span class="cc-etiqueta cc-cat-falta">').text('Sí')
			: $('<span class="cc-etiqueta cc-cat-sobra">').text('No')));
		//El dato viaja en la fila para no volver a buscarlo en el arreglo.
		fila.data('pago', p);
		fila.click(function () {
			copiarPago($(this));
		});
		cuerpo.append(fila);
	}

	//El total va sin los anulados: sumarlos daria una cifra que no entro a caja.
	$('#pagos-resumen').html(pagos.length + ' pagos, ' + pesos(suma) +
		' sin contar anulados' + (anulados > 0 ? ' (' + anulados + ' anulados)' : '') + '.');
}

/* Copia el pago al formulario, que es para lo que sirve el panel. */
function copiarPago(fila) {
	var p = fila.data('pago');
	$('#grid-pagos tbody tr').removeClass('cc-sel');
	fila.addClass('cc-sel');

	$('#numeropedido').val(p.idpedidotienda);
	if (p.telefono) {
		$('#telefono').val(p.telefono);
	}
	//Se copia el valor de la forma de pago y no el total del pedido: la
	//diferencia que se concilia es la del pago, no la de la venta.
	var valor = Number(p.valorformapago) || 0;
	$('#valoranalizar').val(pesos(valor).replace('$', ''));
	mostrarAyudaValor($('#valoranalizar').val());

	avisar('aviso-solicitud', 'Se copió el pedido ' + p.idpedidotienda + ' por ' + pesos(valor) +
		'. Revise la categoría y escriba la descripción.', 'cc-aviso-info');
}

// ===========================================================================
// Fechas
// ===========================================================================

function validarFecha(texto) {
	if (!texto) {
		return (false);
	}
	if (!/^[0-9]{2}\/[0-9]{2}\/[0-9]{4}$/.test(texto)) {
		return (false);
	}
	return (existeFecha(texto));
}

function aComparable(texto) {
	var p = texto.split('/');
	return (parseInt(p[2] + p[1] + p[0], 10));
}

function existeFecha(fecha) {
	var fechaf = fecha.split("/");
	var day = fechaf[0];
	var month = fechaf[1];
	var year = fechaf[2];
	var date = new Date(year, month, '0');
	if ((day - 0) > (date.getDate() - 0)) {
		return (false);
	}
	return (true);
}

function limpiarFormulario() {
	$('#selectOrigen').val("");
	$('#selectTiendas').val('');
	$('#numeropedido').val('');
	$('#descripcion').val('');
	$('#selectCategoria').val('');
	$('#valoranalizar').val('');
	$('#telefono').val('');
	mostrarAyudaValor('');
	limpiarPagos();
}

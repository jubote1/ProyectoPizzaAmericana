/**
 * Adicionar puntos a un cliente.
 *
 * La pantalla muestra las tres cifras -saldo, lo que se adiciona, con cuanto queda- desde que se digitan los
 * puntos, y no solo despues de aplicar: quien le mueve el saldo a otra persona tiene que ver el resultado antes de
 * que sea irreversible.
 *
 * Nada de lo que valida este archivo es seguridad: el servidor vuelve a validar todo, incluido que quien pide
 * tenga el rol. Esto es para que el error se vea antes de mandarlo.
 */

var clienteActual = null;
var MOTIVO_MINIMO = 15;

$(function () {
	$('#btnConsultar').on('click', consultar);
	$('#correo').on('keypress', function (e) { if (e.which === 13) { consultar(); } });
	$('#puntos').on('input', calcular);
	$('#motivo').on('input', contarMotivo);
	$('#btnAdicionar').on('click', confirmar);
	//Cambiar el correo invalida lo consultado: si no, se podria adicionar creyendo que es otro cliente.
	$('#correo').on('input', limpiar);
});

function limpiar() {
	clienteActual = null;
	$('#datosCliente').hide();
	$('#bloqueAjuste').hide();
	$('#panelResultado').hide();
}

function contarMotivo() {
	var n = $.trim($('#motivo').val()).length;
	$('#contadorMotivo').text('(' + n + ' de minimo ' + MOTIVO_MINIMO + ')');
}

function consultar() {
	var correo = $.trim($('#correo').val());
	if (correo === '') {
		avisar('Escriba el correo del cliente.', 'mal');
		return;
	}
	$('#btnConsultar').prop('disabled', true);
	$.getJSON('AdicionarPuntosCliente', { correo: correo }, function (data) {
		$('#btnConsultar').prop('disabled', false);
		if (!responde(data)) { return; }

		clienteActual = data;
		$('#nombreCliente').text(data.nombre && data.nombre !== '' ? data.nombre : '(la ficha no tiene nombre)');
		$('#estadoCliente').html(data.activo === 'S'
			? '<span class="etiqueta act-si">Activo</span>'
			: '<span class="etiqueta act-no">Inactivo en el plan</span>');
		$('#saldoActual').text(formatear(data.puntos));
		$('#datosCliente').show();
		$('#panelResultado').hide();
		$('#notaVigencia').text('Los puntos que se adicionen valdran ' + data.diasvigencia + ' dias desde hoy.');
		$('#notaTope').text('Máximo ' + formatear(data.tope) + ' por operación.');

		//Las cifras se reinician aca, antes de cualquier salida temprana: no pueden quedar con los
		//valores del cliente anterior.
		$('#puntos').val('');
		$('#seAdicionan').text('0');
		$('#leQuedan').text(formatear(data.puntos));

		if (data.activo !== 'S') {
			avisar('El cliente esta inactivo en el plan. Hay que activarlo antes de adicionarle puntos.', 'ojo');
			$('#bloqueAjuste').hide();
			return;
		}
		avisar('', '');
		$('#bloqueAjuste').show();
		contarMotivo();
		$('#puntos').val('').focus();
		calcular();
	}).fail(function () {
		$('#btnConsultar').prop('disabled', false);
		avisar('No se pudo consultar el cliente.', 'mal');
	});
}

function calcular() {
	if (!clienteActual) { return; }
	var puntos = numero($('#puntos').val());
	if (puntos === null) {
		$('#seAdicionan').text('0');
		$('#leQuedan').text(formatear(clienteActual.puntos));
		return;
	}
	$('#seAdicionan').text(formatear(puntos));
	$('#leQuedan').text(formatear(clienteActual.puntos + puntos));
}

function confirmar() {
	if (!clienteActual) { return; }
	var puntos = numero($('#puntos').val());
	var motivo = $.trim($('#motivo').val());

	if (puntos === null || puntos <= 0) {
		avisar('Escriba cuantos puntos se van a adicionar.', 'mal');
		$('#puntos').focus();
		return;
	}
	if (puntos > clienteActual.tope) {
		avisar('Son ' + formatear(puntos) + ' puntos y el tope por operacion es ' + formatear(clienteActual.tope) + '.', 'mal');
		$('#puntos').focus();
		return;
	}
	if (motivo.length < MOTIVO_MINIMO) {
		avisar('Escriba el motivo (minimo ' + MOTIVO_MINIMO + ' caracteres). Es lo que queda registrado y lo que lee el cliente.', 'mal');
		$('#motivo').focus();
		return;
	}

	var texto = 'Se le van a ADICIONAR ' + formatear(puntos) + ' puntos a ' + clienteActual.correo + '.\n\n'
		+ 'Saldo actual:  ' + formatear(clienteActual.puntos) + '\n'
		+ 'Le quedaran:   ' + formatear(clienteActual.puntos + puntos) + '\n'
		+ 'Vigencia:      ' + clienteActual.diasvigencia + ' dias desde hoy\n'
		+ 'Motivo:        ' + motivo + '\n'
		+ '\nAl cliente le va a llegar un correo con el motivo.\n\n'
		+ 'Esto no se puede deshacer desde esta pantalla. Continuar?';
	if (!confirm(texto)) { return; }
	adicionar(puntos, motivo);
}

function adicionar(puntos, motivo) {
	$('#btnAdicionar').prop('disabled', true).html('Procesando...');
	$.post('AdicionarPuntosCliente', {
		correo: clienteActual.correo,
		puntos: puntos,
		motivo: motivo
	}, function (data) {
		$('#btnAdicionar').prop('disabled', false).html('<i class="fas fa-plus"></i> Adicionar y avisar al cliente');
		if (!responde(data)) { return; }

		//El correo se informa aparte del ajuste: el ajuste pudo quedar bien y el correo no haber salido,
		//y entonces hay que avisarle al cliente por otro lado.
		avisar(data.detalle, data.correoenviado ? 'ok' : 'ojo');

		var html = '<p>Ajuste <strong>#' + data.idajuste + '</strong> a nombre de <strong>'
			+ escapar(clienteActual.correo) + '</strong>.</p>'
			+ '<div class="cifras">'
			+ '  <div class="cifra suma"><div class="r">Se adicionaron</div><div class="v">' + formatear(data.puntos) + '</div></div>'
			+ '  <div class="cifra queda"><div class="r">Saldo ahora</div><div class="v">' + formatear(data.saldo) + '</div></div>'
			+ '</div>'
			+ '<p class="nota">Vigentes hasta el <strong>' + escapar(data.vencen) + '</strong> (' + data.diasvigencia + ' d&iacute;as). '
			+ (data.correoenviado
				? 'Se le envi&oacute; el correo al cliente.'
				: '<strong>El correo no sali&oacute;.</strong> El ajuste s&iacute; qued&oacute; hecho; hay que avisarle al cliente por otro medio.')
			+ '</p>';
		$('#detalleResultado').html(html);
		$('#panelResultado').show();

		//Se vuelve a consultar para que el saldo en pantalla sea el real, no el calculado.
		$('#puntos').val('');
		$('#motivo').val('');
		consultar();
	}, 'json').fail(function () {
		$('#btnAdicionar').prop('disabled', false).html('<i class="fas fa-plus"></i> Adicionar y avisar al cliente');
		avisar('No se pudo procesar el ajuste. Consulte el cliente antes de volver a intentar, para no sumarle puntos dos veces.', 'mal');
	});
}

/** Traduce la respuesta del servidor. true si la pantalla puede seguir. */
function responde(data) {
	if (!data) {
		avisar('El servidor no respondio.', 'mal');
		return (false);
	}
	if (data.respuesta === 'OK') {
		return (true);
	}
	if (data.respuesta === 'NOSESION') {
		avisar('Se cerro la sesion. Vuelva a entrar.', 'mal');
		setTimeout(function () { location.reload(); }, 1500);
		return (false);
	}
	if (data.respuesta === 'SINPERMISO') {
		avisar('Su usuario no tiene permiso para esta pantalla.', 'mal');
		$('#bloqueAjuste').hide();
		return (false);
	}
	avisar(data.detalle ? data.detalle : 'No se pudo completar la operacion.', 'mal');
	return (false);
}

function avisar(texto, tipo) {
	var caja = $('#aviso');
	if (!texto) {
		caja.hide().text('');
		return;
	}
	caja.removeClass('aviso-ok aviso-mal aviso-ojo');
	caja.addClass(tipo === 'ok' ? 'aviso-ok' : (tipo === 'ojo' ? 'aviso-ojo' : 'aviso-mal'));
	caja.text(texto).show();
	$('html, body').animate({ scrollTop: 0 }, 200);
}

/** Puntos con separador de miles. Sin decimales cuando son enteros. */
function formatear(valor) {
	if (valor === null || valor === undefined || isNaN(valor)) { return ('0'); }
	var redondeado = Math.round(valor * 100) / 100;
	var entero = Math.floor(Math.abs(redondeado));
	var texto = entero.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	var decimales = Math.round((Math.abs(redondeado) - entero) * 100);
	if (decimales > 0) {
		texto += ',' + (decimales < 10 ? '0' + decimales : decimales);
	}
	return ((redondeado < 0 ? '-' : '') + texto);
}

/** Lee un numero digitado, aceptando coma o punto. null si no se entiende (nunca se convierte en cero). */
function numero(texto) {
	if (texto === null || texto === undefined) { return (null); }
	var limpio = String(texto).trim().replace(/\s/g, '');
	if (limpio === '') { return (null); }
	var coma = limpio.lastIndexOf(',');
	var punto = limpio.lastIndexOf('.');
	if (coma >= 0 && coma > punto) {
		limpio = limpio.replace(/\./g, '').replace(',', '.');
	} else {
		limpio = limpio.replace(/,/g, '');
	}
	if (!/^\d+(\.\d+)?$/.test(limpio)) { return (null); }
	var valor = parseFloat(limpio);
	return (isNaN(valor) ? null : valor);
}

function escapar(texto) {
	if (texto === null || texto === undefined) { return (''); }
	return (String(texto).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'));
}

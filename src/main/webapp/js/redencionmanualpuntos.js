/**
 * Redencion manual de puntos.
 *
 * La pantalla muestra las tres cifras -saldo, lo que se descuenta, lo que
 * queda- desde que se digitan los puntos, y no solo despues de aplicar. Quien
 * esta moviendo el saldo de otra persona tiene que poder ver el resultado antes
 * de que sea irreversible.
 *
 * Nada de lo que valida este archivo es seguridad: el servidor vuelve a validar
 * todo, incluido que quien pide sea administrador. Esto es para que el error se
 * vea antes de mandarlo, no para permitir nada.
 */

var clienteActual = null;

$(function () {
	cargarTiendas();
	$('#btnConsultar').on('click', consultar);
	$('#correo').on('keypress', function (e) { if (e.which === 13) { consultar(); } });
	$('#puntos').on('input', calcular);
	$('#btnRedimir').on('click', confirmar);

	//Cambiar el correo invalida lo consultado: si no, se podria redimir sobre el
	//saldo de un cliente y creer que es el de otro.
	$('#correo').on('input', limpiar);
});

function cargarTiendas() {
	$.getJSON('RedencionManualPuntos', { que: 'tiendas' }, function (data) {
		if (!data || data.respuesta !== 'OK') { return; }
		var html = '<option value="0">(no aplica)</option>';
		for (var i = 0; i < data.tiendas.length; i++) {
			html += '<option value="' + data.tiendas[i].idtienda + '">'
				+ escapar(data.tiendas[i].nombre) + '</option>';
		}
		$('#tienda').html(html);
	});
}

function limpiar() {
	clienteActual = null;
	$('#datosCliente').hide();
	$('#bloqueRedencion').hide();
	$('#panelResultado').hide();
}

function consultar() {
	var correo = $.trim($('#correo').val());
	if (correo === '') {
		avisar('Escriba el correo del cliente.', 'mal');
		return;
	}
	$('#btnConsultar').prop('disabled', true);
	$.getJSON('RedencionManualPuntos', { que: 'consultar', correo: correo }, function (data) {
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

		if (data.activo !== 'S') {
			avisar('El cliente esta inactivo en el plan. Hay que activarlo antes de redimir.', 'ojo');
			$('#bloqueRedencion').hide();
			return;
		}
		if (data.puntos <= 0) {
			avisar('El cliente no tiene puntos para redimir.', 'ojo');
			$('#bloqueRedencion').hide();
			return;
		}
		avisar('', '');
		$('#bloqueRedencion').show();
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
		$('#seDescuentan').text('0');
		$('#leQuedan').text(formatear(clienteActual.puntos));
		$('#cajaQueda').removeClass('malo').addClass('queda');
		return;
	}
	var queda = clienteActual.puntos - puntos;
	$('#seDescuentan').text(formatear(puntos));
	$('#leQuedan').text(formatear(queda));
	//El saldo negativo se pinta en rojo desde que se digita. El servidor no lo
	//va a dejar pasar, pero es mejor verlo mientras todavia se esta mirando la
	//celda que descubrirlo en un mensaje de error.
	if (queda < 0) {
		$('#cajaQueda').removeClass('queda').addClass('malo');
	} else {
		$('#cajaQueda').removeClass('malo').addClass('queda');
	}
}

function confirmar() {
	if (!clienteActual) { return; }

	var puntos = numero($('#puntos').val());
	var motivo = $.trim($('#motivo').val());
	var idTienda = $('#tienda').val();
	var tienda = (idTienda === '0') ? '' : $('#tienda option:selected').text();

	if (puntos === null || puntos <= 0) {
		avisar('Escriba cuantos puntos se van a redimir.', 'mal');
		$('#puntos').focus();
		return;
	}
	if (puntos > clienteActual.puntos) {
		avisar('El cliente tiene ' + formatear(clienteActual.puntos) + ' puntos y se intentan redimir '
			+ formatear(puntos) + '.', 'mal');
		$('#puntos').focus();
		return;
	}
	if (motivo.length < 5) {
		avisar('Escriba el motivo. Es lo que queda registrado y lo que lee el cliente.', 'mal');
		$('#motivo').focus();
		return;
	}

	var queda = clienteActual.puntos - puntos;
	var texto = 'Se le van a descontar ' + formatear(puntos) + ' puntos a ' + clienteActual.correo + '.\n\n'
		+ 'Saldo actual: ' + formatear(clienteActual.puntos) + '\n'
		+ 'Le quedan:    ' + formatear(queda) + '\n'
		+ 'Motivo:       ' + motivo + '\n'
		+ (tienda === '' ? '' : 'Tienda:       ' + tienda + '\n')
		+ '\nAl cliente le va a llegar un correo contandole el movimiento.\n\n'
		+ 'Esto no se puede deshacer desde esta pantalla. Continuar?';

	if (!confirm(texto)) { return; }
	redimir(puntos, idTienda, tienda, motivo);
}

function redimir(puntos, idTienda, tienda, motivo) {
	$('#btnRedimir').prop('disabled', true).html('Procesando...');
	$.post('RedencionManualPuntos', {
		correo: clienteActual.correo,
		puntos: puntos,
		idtienda: idTienda,
		tienda: tienda,
		motivo: motivo
	}, function (data) {
		$('#btnRedimir').prop('disabled', false).html('<i class="fas fa-check"></i> Redimir y avisar al cliente');
		if (!responde(data)) { return; }

		//El correo se informa aparte del resultado de la redencion. Son dos
		//cosas distintas: la redencion pudo quedar bien y el correo no haber
		//salido, y en ese caso hay que avisarle al cliente por otro lado.
		var claseAviso = data.correoenviado ? 'ok' : 'ojo';
		avisar(data.detalle, claseAviso);

		var html = '<p>Redenci&oacute;n <strong>#' + data.idredencion + '</strong> '
			+ 'a nombre de <strong>' + escapar(clienteActual.correo) + '</strong>.</p>'
			+ '<div class="cifras">'
			+ '  <div class="cifra resta"><div class="r">Se descontaron</div><div class="v">'
			+ formatear(data.puntos) + '</div></div>'
			+ '  <div class="cifra queda"><div class="r">Saldo que le queda</div><div class="v">'
			+ formatear(data.saldo) + '</div></div>'
			+ '</div>'
			+ '<p class="nota">' + (data.correoenviado
				? 'Se le envi&oacute; el correo al cliente.'
				: '<strong>El correo no sali&oacute;.</strong> La redenci&oacute;n s&iacute; qued&oacute; '
				+ 'hecha; hay que avisarle al cliente por otro medio.') + '</p>';
		$('#detalleResultado').html(html);
		$('#panelResultado').show();

		//Se vuelve a consultar para que el saldo en pantalla sea el real y no el
		//que se calculo. Si alguien redime dos veces por equivocacion, la
		//segunda tiene que arrancar del saldo de verdad.
		$('#puntos').val('');
		$('#motivo').val('');
		consultar();
	}, 'json').fail(function () {
		$('#btnRedimir').prop('disabled', false).html('<i class="fas fa-check"></i> Redimir y avisar al cliente');
		avisar('No se pudo procesar la redencion. Consulte el cliente antes de volver a intentar, '
			+ 'para no descontarle dos veces.', 'mal');
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
		avisar('Su usuario no es administrador. Esta pantalla solo la pueden usar administradores.', 'mal');
		$('#bloqueRedencion').hide();
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

/**
 * Lee un numero digitado, aceptando coma o punto.
 * Devuelve null cuando no se entiende; convertirlo en cero es como se cuelan
 * las redenciones de nada que igual le mandan un correo al cliente.
 */
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
	return (String(texto).replace(/&/g, '&amp;').replace(/</g, '&lt;')
		.replace(/>/g, '&gt;').replace(/"/g, '&quot;'));
}

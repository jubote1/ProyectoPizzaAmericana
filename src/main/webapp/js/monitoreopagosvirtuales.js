/*
 * Monitoreo de pagos virtuales.
 *
 * Que cambio y por que:
 *
 * - La pantalla mostraba solo los pedidos que en ESE segundo estaban esperando
 *   el pago, de hoy. Un pedido se veia durante los cincuenta minutos de espera
 *   y despues desaparecia para siempre -pagado o perdido-, y despues de
 *   medianoche quedaba vacia. Ahora se consulta por rango de fechas y se ve el
 *   desenlace de cada uno.
 *
 * - No habia columna de estado, que es el nombre de la pantalla. Un pedido de
 *   treinta minutos podia ser uno al que le rechazaron la tarjeta dos veces,
 *   uno que nunca recibio el link, o uno que ya pago: se veian iguales. El
 *   estado sale ahora de los eventos que devuelve Wompi, que ya se venian
 *   guardando y nunca se leian.
 *
 * - La clave PRIVADA de Wompi se bajaba al navegador para poder crear el link
 *   desde aqui. Ya no: recrear el link es una llamada al servidor.
 *
 * - El boton de reenviar preguntaba por un campo #idlink que no existe en el
 *   HTML, asi que la validacion nunca detenia nada y al cliente le podia llegar
 *   un link roto.
 */

var server;
var table;
var dtTraza;

/** Lo ultimo que devolvio el servidor, para poder filtrar sin volver a consultar. */
var datosPedidos = [];

/** El pedido seleccionado. Se conserva entre refrescos. */
var pedidoSel = null;

var filtroActual = 'TODOS';

/** Cada cuanto se vuelve a consultar solo, en milisegundos. */
var MILIS_REFRESCO = 60000;

$(document).ready(function() {

	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	$("#fechainicial").datepicker($.datepicker.regional["es"]);
	$("#fechainicial").datepicker('setDate', new Date());
	$("#fechafinal").datepicker($.datepicker.regional["es"]);
	$("#fechafinal").datepicker('setDate', new Date());

	table = $('#grid-pedidos').DataTable({
		"columns": [
			{ "data": "idpedido" },
			{ "data": "tienda" },
			{ "data": "estado", "render": function(dato, tipo, fila) {
					if (tipo !== 'display') { return dato; }
					return '<span class="pv-estado">' + dato + '</span>';
				} },
			{ "data": "minutos" },
			{ "data": "nombre" },
			{ "data": "telefonocelular" },
			{ "data": "telefono" },
			{ "data": "email" },
			{ "data": "totalneto", "render": function(dato, tipo) {
					if (tipo !== 'display') { return dato; }
					return pesos(dato);
				} },
			{ "data": "eventos" },
			{ "data": "avisos" },
			{ "data": "gestiones" },
			{ "data": "fechainsercion" },
			{ "data": "origen" },
			{ "data": "idlink" }
		],
		"order": [[ 3, "desc" ]],
		"pageLength": 25,
		"language": { "emptyTable": "No hay pagos virtuales en el periodo consultado." },
		"createdRow": function(fila, datos) {
			$(fila).addClass('pv-n' + datos.nivel);
			if (pedidoSel != null && datos.idpedido == pedidoSel.idpedido) {
				$(fila).addClass('pv-sel');
			}
		}
	});

	dtTraza = $('#grid-traza').DataTable({
		"columns": [
			{ "data": "fechahora" },
			{ "data": "fuente", "render": function(dato, tipo) {
					if (tipo !== 'display') { return dato; }
					return '<span class="pv-' + dato + '">' + dato + '</span>';
				} },
			{ "data": "detalle" },
			{ "data": "estado" }
		],
		"order": [],
		"paging": false,
		"searching": false,
		"info": false,
		"language": { "emptyTable": "Sin movimientos registrados." }
	});

	//Delegado sobre el tbody: las filas se vuelven a crear en cada refresco.
	$('#grid-pedidos tbody').on('click', 'tr', function() {
		var datos = table.row(this).data();
		if (datos) {
			seleccionarPedido(datos);
		}
	});

	refrescarPagosVirtuales();
	setInterval(validarVigenciaLogueo, 600000);
	setInterval(refrescoAutomatico, MILIS_REFRESCO);
});


/*
 * Consulta y pintado
 */

function refrescarPagosVirtuales() {
	var desde = $("#fechainicial").val();
	var hasta = $("#fechafinal").val();
	$.getJSON(server + 'ObtenerPedidosMonitoreoPagoVirtual?fechaini=' + encodeURIComponent(desde)
			+ '&fechafin=' + encodeURIComponent(hasta), function(datos) {
		datosPedidos = datos.pedidos || [];
		pintarResumen(datos.resumen || {});
		pintarTabla();
		$("#ultimaconsulta").html('Consultado a las ' + horaActual() + '. '
				+ datosPedidos.length + ' pago(s) virtual(es) en el periodo.');
		//Si el pedido que estaba abierto sigue en la lista, se refresca su ficha:
		//puede haber pagado o haberle llegado un evento mientras se miraba.
		if (pedidoSel != null) {
			var vigente = buscarPedido(pedidoSel.idpedido);
			if (vigente != null) {
				pedidoSel = vigente;
				pintarFichaPedido(vigente);
			}
		}
	});
}

/**
 * El refresco solo no puede pisarle el trabajo a quien esta atendiendo: si
 * desmarco la casilla, o esta escribiendo la observacion, no se refresca.
 */
function refrescoAutomatico() {
	if (!$("#autorefresco").is(':checked')) {
		return;
	}
	if ($("#observacion").is(':focus') || $("#correoenvio").is(':focus')) {
		return;
	}
	refrescarPagosVirtuales();
}

function pintarResumen(r) {
	$("#res-total").html(r.total || 0);
	$("#res-pagados").html(r.pagados || 0);
	$("#res-sinpagar").html(r.sinpagar || 0);
	$("#res-cancelados").html(r.cancelados || 0);
	$("#res-poratender").html(r.poratender || 0);
	$("#res-conrechazo").html(r.conrechazo || 0);
	$("#res-valorpagado").html(pesos(r.valorpagado) + ' recibidos');
	$("#res-valorriesgo").html(pesos(r.valorenriesgo) + ' en riesgo');
	$("#res-valorperdido").html(pesos(r.valorperdido) + ' perdidos');
	//Los tiempos salen de los parametros, no de un numero escrito aqui: si se
	//cambian, la pantalla dice los nuevos.
	$("#res-tiempos").html('Se recuerda a los ' + (r.minutosaviso || 0)
			+ ' min y se cancela a los ' + (r.minutoscancela || 0) + ' min');
}

function pintarTabla() {
	table.clear();
	var filas = [];
	for (var i = 0; i < datosPedidos.length; i++) {
		if (pasaFiltro(datosPedidos[i])) {
			filas.push(datosPedidos[i]);
		}
	}
	//Un solo draw al final. Antes se llamaba draw() por cada fila, o sea que la
	//tabla se repintaba entera tantas veces como pedidos hubiera.
	table.rows.add(filas).draw();
}

/**
 * Los filtros son los que usa quien atiende, no los estados tecnicos:
 * "para atender ya" es lo que hay que salvar en este momento.
 */
function pasaFiltro(pedido) {
	switch (filtroActual) {
		case 'ATENDER':  return (pedido.nivel == 4);
		case 'SINPAGAR': return (pedido.nivel != 1 && pedido.nivel != 5);
		case 'RECHAZO':  return (pedido.rechazos > 0);
		case 'PERDIDOS': return (pedido.nivel == 5);
		case 'PAGADOS':  return (pedido.nivel == 1);
		default:         return true;
	}
}

function filtrar(cual) {
	filtroActual = cual;
	$(".pv-filtros .btn").removeClass('btn-primary').addClass('btn-default');
	$("#f-" + cual).removeClass('btn-default').addClass('btn-primary');
	pintarTabla();
}

function buscarPedido(idPedido) {
	for (var i = 0; i < datosPedidos.length; i++) {
		if (datosPedidos[i].idpedido == idPedido) {
			return datosPedidos[i];
		}
	}
	return null;
}


/*
 * El pedido seleccionado
 */

function seleccionarPedido(datos) {
	pedidoSel = datos;
	$("#grid-pedidos tbody tr").removeClass('pv-sel');
	pintarFichaPedido(datos);
	cargarTraza(datos.idpedido);
	//Se vuelve a marcar la fila despues de pintar, porque pintarFichaPedido no
	//toca la tabla pero cargarTraza si puede tardar.
	table.rows().every(function() {
		if (this.data().idpedido == datos.idpedido) {
			$(this.node()).addClass('pv-sel');
		}
	});
}

function pintarFichaPedido(p) {
	$("#pv-pedido-titulo").html('Pedido <b>#' + p.idpedido + '</b> &nbsp;&middot;&nbsp; ' + p.tienda
			+ ' &nbsp;&middot;&nbsp; ' + pesos(p.totalneto)
			+ ' &nbsp;&middot;&nbsp; <span class="pv-estado">' + p.estado + '</span>');
	$("#pv-pedido-detalle").html(p.nombre + ' &nbsp;&middot;&nbsp; cel ' + (p.telefonocelular || '-')
			+ ' &nbsp;&middot;&nbsp; tel ' + (p.telefono || '-')
			+ '<br/>Tomado ' + p.fechainsercion + ' (' + p.minutos + ' min)'
			+ (p.fechapago ? ' &nbsp;&middot;&nbsp; pagado ' + p.fechapago : '')
			+ '<br/>Link: ' + (p.idlink ? p.idlink : '<b>sin link</b>'));

	$("#correoenvio").val(p.email || '').prop('disabled', false);
	$("#observacion").prop('disabled', false);
	$("#obsGestion").prop('disabled', false);
	$("#reenviarCorreo").prop('disabled', false);

	//Un pedido ya pagado no se vuelve a notificar ni se le recrea el link: seria
	//pedirle al cliente que pague algo que ya pago.
	var pagado = (p.fechapago && p.fechapago.length > 0);
	$("#reenviarNotificacion").prop('disabled', pagado || !p.idlink);
	$("#recrearLink").prop('disabled', pagado);
	$("#reenviarCorreo").prop('disabled', pagado || !p.idlink);
}

function cargarTraza(idPedido) {
	$.getJSON(server + 'ConsultarTrazaPagoVirtual?idpedido=' + idPedido, function(filas) {
		dtTraza.clear();
		dtTraza.rows.add(filas || []).draw();
	});
}

function limpiarSeleccion() {
	pedidoSel = null;
	$("#grid-pedidos tbody tr").removeClass('pv-sel');
	$("#pv-pedido-titulo").html('Seleccione un pedido de la lista.');
	$("#pv-pedido-detalle").html('');
	$("#correoenvio").val('').prop('disabled', true);
	$("#observacion").val('').prop('disabled', true);
	$("#obsGestion").prop('disabled', true);
	$("#reenviarNotificacion").prop('disabled', true);
	$("#reenviarCorreo").prop('disabled', true);
	$("#recrearLink").prop('disabled', true);
	dtTraza.clear().draw();
}


/*
 * Acciones sobre el pedido
 */

function reenviarNotificacion() {
	if (pedidoSel == null) { return; }
	//La validacion mira el link del pedido y no un campo del formulario. La
	//version anterior preguntaba por #idlink, que no existe en el HTML: nunca
	//detenia nada y el cliente recibia la direccion de pago vacia.
	if (!pedidoSel.idlink) {
		$.alert("Este pedido no tiene link de pago. Primero hay que recrearlo.");
		return;
	}
	$.confirm({
		'title': 'Reenviar el link de pago',
		'content': 'Se le vuelve a enviar el link del pedido #' + pedidoSel.idpedido
				+ ' a ' + pedidoSel.nombre + '.<br>Se envia mensaje de texto y correo, si tiene.',
		'type': 'dark',
		'typeAnimated': true,
		'buttons': {
			'Si': { 'class': 'blue', 'action': function() {
					var p = pedidoSel;
					$.getJSON(server + 'RealizarNotificacionWompi?idlink=' + p.idlink
							+ '&idcliente=' + p.idcliente
							+ '&linkpago=' + encodeURIComponent('https://checkout.wompi.co/l/' + p.idlink)
							+ '&idformapago=' + p.idformapago + '&idpedido=' + p.idpedido, function() {
						$.alert("Se reenvio la notificacion del pedido #" + p.idpedido + ".");
						cargarTraza(p.idpedido);
					});
				} },
			'No': { 'class': 'gray', 'action': function() {} }
		}
	});
}


/**
 * Reenvia el correo, con la opcion de mandarlo a otra direccion.
 *
 * Si el correo escrito es distinto al que tiene la ficha, se pregunta si ademas
 * hay que dejarselo al cliente. No se guarda solo: un correo dictado por
 * telefono se entiende mal con frecuencia, y el que esta en la ficha puede ser
 * el bueno.
 */
function reenviarCorreo() {
	if (pedidoSel == null) { return; }
	var p = pedidoSel;
	var destino = $.trim($("#correoenvio").val());
	if (destino.length == 0) {
		$.alert("Escriba el correo al que se le va a enviar el link.");
		return;
	}
	if (!correoValido(destino)) {
		$.alert("El correo '" + destino + "' no parece valido. Revise antes de enviarlo.");
		return;
	}
	var actual = $.trim(p.email || '');
	var esOtro = (destino.toLowerCase() !== actual.toLowerCase());

	if (!esOtro) {
		enviarCorreoLink(p, '', 'N');
		return;
	}
	$.confirm({
		'title': 'El correo es distinto al de la ficha',
		'content': 'En la ficha del cliente esta <b>' + (actual.length > 0 ? actual : 'sin correo')
				+ '</b> y se va a enviar a <b>' + destino + '</b>.'
				+ '<br><br>Desea ademas dejar este correo en la ficha del cliente?',
		'type': 'dark',
		'typeAnimated': true,
		'buttons': {
			'Enviar y actualizar': { 'class': 'blue', 'action': function() {
					enviarCorreoLink(p, destino, 'S');
				} },
			'Solo enviar': { 'class': 'green', 'action': function() {
					enviarCorreoLink(p, destino, 'N');
				} },
			'Cancelar': { 'class': 'gray', 'action': function() {} }
		}
	});
}

function enviarCorreoLink(p, correo, actualizar) {
	$("#reenviarCorreo").prop('disabled', true);
	$.getJSON(server + 'ReenviarCorreoLinkPago?idpedido=' + p.idpedido
			+ '&correo=' + encodeURIComponent(correo)
			+ '&actualizar=' + actualizar, function(resp) {
		$("#reenviarCorreo").prop('disabled', false);
		$.alert(resp.mensaje);
		if (resp.resultado == 'OK') {
			cargarTraza(p.idpedido);
			if (resp.actualizado) {
				//Que la reja muestre el correo nuevo sin tener que volver a consultar.
				p.email = $.trim($("#correoenvio").val());
				pintarTabla();
			}
		}
	}).fail(function() {
		$("#reenviarCorreo").prop('disabled', false);
		$.alert("No se pudo reenviar el correo. Revise la sesion y vuelva a intentar.");
	});
}

/**
 * Recrea el link de pago.
 *
 * Es una llamada al servidor. Antes se armaba aqui mismo el POST a Wompi con la
 * clave PRIVADA bajada al navegador, y con el monto tomado de la reja.
 */
function recrearLink() {
	if (pedidoSel == null) { return; }
	$.confirm({
		'title': 'Recrear el link de pago',
		'content': 'Se crea un link nuevo para el pedido #' + pedidoSel.idpedido
				+ ' y se le envia al cliente.<br>El link anterior ya no se debe usar.',
		'type': 'dark',
		'typeAnimated': true,
		'buttons': {
			'Si': { 'class': 'blue', 'action': function() {
					var p = pedidoSel;
					$("#recrearLink").prop('disabled', true);
					$.getJSON(server + 'RecrearLinkPagoWompi?idpedido=' + p.idpedido, function(resp) {
						$("#recrearLink").prop('disabled', false);
						$.alert(resp.mensaje);
						if (resp.resultado == 'OK') {
							p.idlink = resp.idlink;
							pintarFichaPedido(p);
							pintarTabla();
							cargarTraza(p.idpedido);
						}
					}).fail(function() {
						$("#recrearLink").prop('disabled', false);
						$.alert("No se pudo recrear el link. Revise la sesion y vuelva a intentar.");
					});
				} },
			'No': { 'class': 'gray', 'action': function() {} }
		}
	});
}


function obsGestionLink() {
	if (pedidoSel == null) { return; }
	var observacion = $.trim($("#observacion").val());
	if (observacion.length == 0) {
		$.alert("Escriba que se hizo con el pedido antes de guardar.");
		return;
	}
	var p = pedidoSel;
	$.getJSON(server + 'IngresarObsGestionLink?idpedido=' + p.idpedido
			+ '&observacion=' + encodeURIComponent(observacion), function(resp) {
		if (resp && resp.resultado == 'ERROR') {
			$.alert(resp.mensaje);
			return;
		}
		$("#observacion").val("");
		//La gestion ya no se escribe a ciegas: aparece de una vez en la historia
		//del pedido, con su hora y con el usuario que la dejo.
		cargarTraza(p.idpedido);
		p.gestiones = (p.gestiones || 0) + 1;
		pintarTabla();
	});
}


/*
 * Utilidades
 */

function validarVigenciaLogueo() {
	var respuesta = '';
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(data) {
			respuesta = data[0].respuesta;
		}
	});
	switch (respuesta) {
		case 'OK':
			break;
		case 'OKA':
			break;
		default:
			location.href = server + "Index.html";
			break;
	}
}

/** Revision minima: una arroba, un punto despues y sin espacios. */
function correoValido(correo) {
	var valor = $.trim(correo);
	var arroba = valor.indexOf('@');
	var punto = valor.lastIndexOf('.');
	return (valor.length >= 6 && arroba > 0 && punto > arroba + 1 && punto < valor.length - 1
			&& valor.indexOf(' ') < 0 && valor.indexOf('@', arroba + 1) < 0);
}

function pesos(valor) {
	var numero = parseFloat(valor);
	if (isNaN(numero)) {
		return '$0';
	}
	return '$' + Math.round(numero).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function horaActual() {
	var d = new Date();
	return dosDigitos(d.getHours()) + ':' + dosDigitos(d.getMinutes()) + ':' + dosDigitos(d.getSeconds());
}

function dosDigitos(n) {
	return (n < 10 ? '0' + n : '' + n);
}

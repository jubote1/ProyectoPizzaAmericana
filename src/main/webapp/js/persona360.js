/*
 * Vista 360 de una persona.
 *
 * Todo se consulta por idpersona. La diferencia con CRMClientesFull, que
 * trabaja por idcliente, es de fondo: el cliente es una fila y la persona es
 * quien compra. Juan Santa esta en 28 filas del central y 36 de tienda; por
 * fila se ve un pedazo de su historia, por persona se ve toda.
 *
 * ES5 -var y function-: la pantalla carga con jQuery 1.11.
 */

var p3Tiendas = {};

$(document).ready(function () {

	//Los nombres de tienda para no mostrar numeros pelados.
	$.getJSON(server + 'GetTiendas', function (datos) {
		for (var i = 0; i < datos.length; i++) {
			p3Tiendas[datos[i].id] = datos[i].nombre;
		}
	});

	$('#p3-buscar').click(function () {
		p3Buscar();
	});
	$('#p3-texto').keypress(function (e) {
		if (e.which === 13) {
			p3Buscar();
		}
	});

	//Se puede entrar directo a una persona desde otra pantalla.
	var id = p3Parametro('idpersona');
	if (id) {
		p3Cargar(id);
	}
});

function p3Parametro(nombre) {
	var partes = window.location.search.substring(1).split('&');
	for (var i = 0; i < partes.length; i++) {
		var par = partes[i].split('=');
		if (par[0] === nombre) {
			return (decodeURIComponent(par[1] || ''));
		}
	}
	return ('');
}

function p3Escapar(texto) {
	return ($('<div/>').text(texto === null || texto === undefined ? '' : texto).html());
}

function p3Pesos(valor) {
	var n = Math.round(Math.abs(Number(valor) || 0));
	var texto = String(n);
	var salida = '';
	for (var i = 0; i < texto.length; i++) {
		if (i > 0 && (texto.length - i) % 3 === 0) {
			salida += '.';
		}
		salida += texto.charAt(i);
	}
	return ('$' + salida);
}

function p3Tienda(id) {
	return (p3Tiendas[id] ? p3Tiendas[id] : 'Tienda ' + id);
}

function p3Mensaje(tipo, texto) {
	$('#p3-mensaje').html('<div class="alert alert-' + tipo + '" style="margin-bottom:0;">' +
		texto + '</div>');
}

// ===========================================================================
// Buscar
// ===========================================================================

function p3Buscar() {
	var texto = $('#p3-texto').val();
	if (!texto || texto.length < 3) {
		p3Mensaje('warning', 'Escriba al menos tres caracteres.');
		return;
	}
	$('#p3-mensaje').html('');
	$('#p3-resultado').hide();
	$('#p3-busqueda').hide();
	$('#p3-buscar').prop('disabled', true);

	$.ajax({
		url: server + 'BuscarPersonaCRM',
		data: { texto: texto },
		dataType: 'json',
		success: function (d) {
			$('#p3-buscar').prop('disabled', false);
			if (d.error) {
				p3Mensaje('warning', p3Escapar(d.error));
				return;
			}
			var lista = d.personas || [];

			if (d.busco_por === 'celular no valido') {
				p3Mensaje('warning', 'Eso no parece un celular colombiano. El maestro solo ' +
					'guarda celulares de diez d&iacute;gitos que empiecen por 3; los dem&aacute;s n&uacute;meros ' +
					'no sirven de llave y no se adivinan.');
				return;
			}
			if (lista.length === 0) {
				p3Mensaje('info', 'No se encontr&oacute; ninguna persona buscando por ' +
					p3Escapar(d.busco_por) + '.');
				return;
			}
			//Si es una sola, se abre de una: obligar a hacer clic en el unico
			//resultado es un paso que no aporta.
			if (lista.length === 1) {
				p3Cargar(lista[0].idpersona);
				return;
			}
			p3PintarBusqueda(lista, d.busco_por);
		},
		error: function () {
			$('#p3-buscar').prop('disabled', false);
			p3Mensaje('danger', 'No se pudo buscar. Revise la conexi&oacute;n.');
		}
	});
}

function p3PintarBusqueda(lista, porDonde) {
	var cuerpo = $('#grid-busqueda tbody');
	cuerpo.empty();
	for (var i = 0; i < lista.length; i++) {
		var p = lista[i];
		var fila = $('<tr>');
		fila.append($('<td>').text((p.nombre + ' ' + p.apellido).replace(/\s+/g, ' ').trim()));
		fila.append($('<td>').text(p.celular || ''));
		fila.append($('<td>').text(p.email || ''));
		fila.append($('<td class="p3-num">').text(p.pedidos));
		fila.append($('<td>').text(p.ultimo || ''));
		fila.data('id', p.idpersona);
		fila.click(function () {
			p3Cargar($(this).data('id'));
		});
		cuerpo.append(fila);
	}
	p3Mensaje('info', lista.length + ' personas encontradas buscando por ' +
		p3Escapar(porDonde) + '. Haga clic en una.');
	$('#p3-busqueda').show();
}

// ===========================================================================
// El detalle
// ===========================================================================

function p3Cargar(idPersona) {
	$('#p3-mensaje').html('');
	$('#p3-busqueda').hide();
	$.ajax({
		url: server + 'ConsultarPersona360',
		data: { idpersona: idPersona },
		dataType: 'json',
		success: function (d) {
			if (d.error) {
				p3Mensaje('danger', p3Escapar(d.error));
				return;
			}
			p3Pintar(d);
		},
		error: function () {
			p3Mensaje('danger', 'No se pudo consultar la persona.');
		}
	});
}

function p3Pintar(d) {
	var p = d.persona;

	$('#p3-nombre').text((p.nombre + ' ' + p.apellido).replace(/\s+/g, ' ').trim() || 'Sin nombre');

	var politica = (p.politica_datos === 'S')
		? '<span class="p3-etiqueta p3-si">Autoriz&oacute; datos</span>'
		: '<span class="p3-etiqueta p3-no">No autoriz&oacute; datos</span>';

	$('#p3-identidad').html(
		'<div class="row">' +
		'<div class="col-sm-6 p3-dato" style="margin-bottom:8px;"><b>Celular</b>' +
			p3Escapar(p.celular || 'sin celular') + '</div>' +
		'<div class="col-sm-6 p3-dato" style="margin-bottom:8px;"><b>Correo</b>' +
			p3Escapar(p.email || 'sin correo') + '</div>' +
		'<div class="col-sm-6 p3-dato" style="margin-bottom:8px;"><b>Tienda habitual</b>' +
			p3Escapar(p.tienda_habitual ? p3Tienda(p.tienda_habitual) : 'sin pedidos') + '</div>' +
		'<div class="col-sm-6 p3-dato" style="margin-bottom:8px;"><b>Persona</b>' +
			p.idpersona + ' &middot; origen ' + p3Escapar(p.origen || '-') + '</div>' +
		'</div>' + politica);

	$('#p3-kpis').html(
		p3Kpi(p.pedidos, 'Pedidos') +
		p3Kpi(p3Pesos(p.valor_comprado), 'Comprado') +
		p3Kpi(p3Pesos(p.ticket_promedio), 'Ticket promedio') +
		p3Kpi(p.puntos, 'Puntos') +
		p3Kpi(p.ofertas_usadas + ' / ' + p.ofertas_asignadas, 'Ofertas usadas') +
		p3Kpi(p.pqrs, 'PQRS') +
		p3Kpi(p.encuestas, 'Encuestas') +
		p3Kpi(p.giros_ruleta, 'Giros de ruleta'));

	//Lo que hace entender para que sirve el maestro.
	var filas = p.filas_central + p.filas_tienda;
	if (filas > 1) {
		$('#p3-caras-aviso').html('<div class="alert alert-info" style="margin:12px 0 0;padding:9px 12px;' +
			'font-size:12.5px;">Esta persona est&aacute; repartida en <b>' + filas + ' filas de cliente</b> &mdash; ' +
			p.filas_central + ' en el central y ' + p.filas_tienda + ' en ' + p.tiendas +
			' tienda' + (p.tiendas === 1 ? '' : 's') + '. Consultada por fila se ver&iacute;a solo un pedazo ' +
			'de esta historia.</div>');
	} else {
		$('#p3-caras-aviso').html('');
	}

	if (p.primer_pedido) {
		$('#p3-caras-aviso').append('<p class="p3-nota">Compra desde el ' +
			p3Escapar(p.primer_pedido) + ' y la &uacute;ltima vez fue el ' +
			p3Escapar(p.ultimo_pedido) +
			(p.pedidos_cancelados > 0 ? '. Tiene ' + p.pedidos_cancelados + ' pedidos cancelados' : '') +
			'.</p>');
	}

	p3PintarPedidos(d.pedidos || []);
	p3PintarOfertas(d.ofertas || []);
	p3PintarPqrs(d.pqrs || []);
	p3PintarEnvios(d.envios || []);
	p3PintarCaras(d.caras || []);

	$('#p3-resultado').show();
	$('html, body').animate({ scrollTop: 0 }, 200);
}

function p3Kpi(valor, rotulo) {
	return ('<div class="col-sm-3 col-xs-6"><div class="p3-kpi">' +
		'<div class="valor">' + valor + '</div>' +
		'<div class="rotulo">' + rotulo + '</div></div></div>');
}

function p3Vacio(columnas, texto) {
	return ('<tr><td colspan="' + columnas + '" class="p3-nota">' + texto + '</td></tr>');
}

function p3PintarPedidos(pedidos) {
	var cuerpo = $('#grid-pedidos tbody');
	cuerpo.empty();
	if (pedidos.length === 0) {
		cuerpo.html(p3Vacio(5, 'Sin pedidos registrados.'));
		return;
	}
	for (var i = 0; i < pedidos.length; i++) {
		var pe = pedidos[i];
		var fila = $('<tr>');
		fila.append($('<td>').text(pe.fecha));
		fila.append($('<td>').text(p3Tienda(pe.idtienda)));
		fila.append($('<td>').text(pe.tipo || ''));
		fila.append($('<td class="p3-num">').text(p3Pesos(pe.valor)));
		fila.append($('<td>').append(pe.cancelado
			? $('<span class="p3-etiqueta p3-no">').text('Cancelado')
			: $('<span class="p3-etiqueta p3-si">').text('Entregado')));
		cuerpo.append(fila);
	}
}

function p3PintarOfertas(ofertas) {
	var cuerpo = $('#grid-ofertas tbody');
	cuerpo.empty();
	if (ofertas.length === 0) {
		cuerpo.html(p3Vacio(3, 'Nunca se le ha asignado una oferta.'));
		return;
	}
	for (var i = 0; i < ofertas.length; i++) {
		var o = ofertas[i];
		var fila = $('<tr>');
		fila.append($('<td>').text(o.oferta || o.codigo || 'Oferta'));
		fila.append($('<td>').text(String(o.ingreso || '').substring(0, 10)));
		fila.append($('<td>').append(o.utilizada === 'S'
			? $('<span class="p3-etiqueta p3-si">').text(String(o.uso || 'S&iacute;').substring(0, 10))
			: $('<span class="p3-etiqueta p3-neutro">').text('No')));
		cuerpo.append(fila);
	}
}

function p3PintarPqrs(pqrs) {
	var cuerpo = $('#grid-pqrs tbody');
	cuerpo.empty();
	if (pqrs.length === 0) {
		cuerpo.html(p3Vacio(5, 'Nunca ha puesto una PQRS.'));
		return;
	}
	for (var i = 0; i < pqrs.length; i++) {
		var q = pqrs[i];
		var fila = $('<tr>');
		fila.append($('<td>').text(String(q.fecha || '').substring(0, 10)));
		fila.append($('<td>').text(p3Tienda(q.idtienda)));
		fila.append($('<td>').text(q.tipo || ''));
		fila.append($('<td>').text(q.estado || ''));
		fila.append($('<td>').text(q.comentario || ''));
		cuerpo.append(fila);
	}
}

/**
 * Los envios de Brevo.
 *
 * El mensaje de "no le hemos enviado nada" dice ademas desde cuando se guarda.
 * Sin eso, un cliente de hace anos que si recibio promociones apareceria como
 * si nunca le hubieramos escrito, y quien mire la pantalla lo creeria.
 */
function p3PintarEnvios(envios) {
	var cuerpo = $('#grid-envios tbody');
	cuerpo.empty();
	if (envios.length === 0) {
		cuerpo.html(p3Vacio(6, 'No se le ha enviado nada desde que se registra (22 de septiembre de 2026).'));
		return;
	}
	for (var i = 0; i < envios.length; i++) {
		var e = envios[i];
		var fila = $('<tr>');
		fila.append($('<td>').text(String(e.cuando || '').substring(0, 16)));
		fila.append($('<td>').text(e.canal === 'W' ? 'WhatsApp' : 'Correo'));
		fila.append($('<td>').text(e.plantilla || (e.idplantilla ? '#' + e.idplantilla : '')));
		fila.append($('<td>').text(e.asunto || ''));
		fila.append($('<td>').text(e.destino || ''));
		//El resultado es si BREVO lo acepto, no si llego ni si lo leyeron. Eso
		//Brevo lo sabe y nosotros no, y prometerlo aca seria mentir.
		fila.append($('<td>').html(e.resultado === 'ERROR'
			? '<span class="p3-etiqueta p3-no">No salio</span>'
			: '<span class="p3-etiqueta p3-si">Aceptado</span>'));
		cuerpo.append(fila);
	}
}

function p3PintarCaras(caras) {
	var cuerpo = $('#grid-caras tbody');
	cuerpo.empty();
	if (caras.length === 0) {
		cuerpo.html(p3Vacio(7, 'Sin filas de cliente.'));
		return;
	}
	for (var i = 0; i < caras.length; i++) {
		var c = caras[i];
		var fila = $('<tr>');
		fila.append($('<td>').text(c.origen));
		fila.append($('<td>').text(c.idcliente));
		fila.append($('<td>').text(c.idtienda ? p3Tienda(c.idtienda) : ''));
		fila.append($('<td>').text((c.nombre + ' ' + c.apellido).replace(/\s+/g, ' ').trim()));
		fila.append($('<td>').text(c.celular || c.telefono || ''));
		fila.append($('<td>').text(c.email || ''));
		fila.append($('<td>').text(c.direccion || ''));
		cuerpo.append(fila);
	}
}

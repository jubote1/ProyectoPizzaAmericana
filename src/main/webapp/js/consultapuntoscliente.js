/*
 * Consulta de puntos del cliente: la pantalla del contact center y de los lideres para atender
 * a un cliente que pregunta por sus puntos.
 *
 * Todo en ES5 -var y function, nada de arrow ni let-: la pantalla convive con jQuery 1.11.
 * Habla con un solo servicio, ConsultarPuntosCliente (BUSCAR y FICHA), que exige sesion y
 * revisa el permiso en la base en cada peticion.
 */

var cpMovimientos = [];
var cpTipoActivo = 'TODOS';

$(document).ready(function () {

	$('#cpBuscar').click(cpBuscar);
	$('#cpLimpiar').click(cpLimpiar);
	$('#cpBusqueda').keydown(function (e) {
		if (e.which === 13) {
			cpBuscar();
		}
	});
	$('#cpTabs a').click(function () {
		$('#cpTabs li').removeClass('activo');
		$(this).parent().addClass('activo');
		$('.cp-panel').removeClass('activo');
		$('#' + $(this).attr('data-panel')).addClass('activo');
	});
	$('.cp-chip').click(function () {
		$('.cp-chip').removeClass('activo');
		$(this).addClass('activo');
		cpTipoActivo = $(this).attr('data-tipo');
		cpPintarMovimientos();
	});
	$('#cpFiltroTexto').keyup(cpPintarMovimientos);

	//La sesion se vence: se revisa cada 10 minutos y, si ya no vale, se manda al ingreso.
	setInterval(cpValidarSesion, 600000);

	$('#cpBusqueda').focus();
});

function cpValidarSesion() {
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		success: function (data) {
			var r = data[0].respuesta;
			if (r !== 'OK' && r !== 'OKA' && r !== 'OKP') {
				location.href = server + 'Index.html';
			}
		}
	});
}

// ---------------------------------------------------------------------------------------------
// Formatos
// ---------------------------------------------------------------------------------------------

function cpEscapar(texto) {
	return ($('<div/>').text(texto === null || texto === undefined ? '' : texto).html());
}

/** 1463.4 -> "1.463,4". Los puntos llevan hasta dos decimales y no se rellenan con ceros. */
function cpPuntos(valor) {
	var n = Math.round((Number(valor) || 0) * 100) / 100;
	var partes = Math.abs(n).toString().split('.');
	var entero = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	return ((n < 0 ? '-' : '') + entero + (partes.length > 1 ? ',' + partes[1] : ''));
}

function cpPesos(valor) {
	var n = Math.round(Number(valor) || 0);
	return ((n < 0 ? '-$ ' : '$ ') + Math.abs(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.'));
}

/** "2026-11-04" o "2026-11-04 17:30:00" -> "04/11/2026" (con hora si la trae y no es medianoche). */
function cpFecha(texto) {
	if (!texto || texto.length < 10) {
		return ('');
	}
	var f = texto.substring(8, 10) + '/' + texto.substring(5, 7) + '/' + texto.substring(0, 4);
	if (texto.length >= 16 && texto.substring(11, 16) !== '00:00') {
		f += ' ' + texto.substring(11, 16);
	}
	return (f);
}

/** Dias que faltan para una fecha yyyy-MM-dd. Negativo si ya paso. */
function cpDiasPara(fecha) {
	if (!fecha || fecha.length < 10) {
		return (null);
	}
	var d = new Date(Number(fecha.substring(0, 4)), Number(fecha.substring(5, 7)) - 1, Number(fecha.substring(8, 10)));
	var hoy = new Date();
	hoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
	return (Math.round((d.getTime() - hoy.getTime()) / 86400000));
}

function cpMesTexto(aaaamm) {
	var nombres = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
	return (nombres[Number(aaaamm.substring(5, 7)) - 1] + ' ' + aaaamm.substring(0, 4));
}

// ---------------------------------------------------------------------------------------------
// Busqueda
// ---------------------------------------------------------------------------------------------

function cpLimpiar() {
	$('#cpBusqueda').val('').focus();
	$('#cpMensaje').empty();
	$('#cpCandidatos').empty();
	$('#cpResultado').hide();
}

function cpMensaje(html, clase) {
	$('#cpMensaje').html('<div class="cp-aviso ' + (clase || '') + '">' + html + '</div>');
}

function cpBuscar() {
	var q = $.trim($('#cpBusqueda').val());
	$('#cpMensaje').empty();
	$('#cpCandidatos').empty();
	$('#cpResultado').hide();
	if (q.length < 4) {
		cpMensaje('Escriba el correo o el celular del cliente.');
		return;
	}
	$('#cpCargando').show();
	$.ajax({
		url: server + 'ConsultarPuntosCliente',
		dataType: 'json',
		type: 'get',
		data: { accion: 'BUSCAR', q: q },
		success: function (d) {
			$('#cpCargando').hide();
			if (d.resultado !== 'OK') {
				cpMensaje(cpEscapar(d.mensaje), 'rojo');
				return;
			}
			var c = d.candidatos || [];
			if (c.length === 0) {
				cpMensaje('No se encontró ningún cliente con ese dato. Revise que el celular tenga 10 dígitos o que el correo esté bien escrito.');
				return;
			}
			if (c.length === 1) {
				cpFicha(c[0].correo);
				return;
			}
			cpMensaje('Hay ' + c.length + ' correos con ese celular. Escoja el del cliente:');
			var html = '';
			for (var i = 0; i < c.length; i++) {
				html += '<div class="cp-cand" data-correo="' + cpEscapar(c[i].correo) + '"><div><div class="n">' +
					cpEscapar(c[i].nombre || '(sin nombre)') + '</div><div class="s">' + cpEscapar(c[i].correo) +
					'</div></div><div>' + (c[i].enprograma ? '<span class="etq etq-ok">En el programa</span>' : '<span class="etq etq-no">No vinculado</span>') +
					'</div></div>';
			}
			$('#cpCandidatos').html(html);
			$('.cp-cand').click(function () {
				cpFicha($(this).attr('data-correo'));
			});
		},
		error: function () {
			$('#cpCargando').hide();
			cpMensaje('No se pudo consultar. Intente de nuevo.', 'rojo');
		}
	});
}

// ---------------------------------------------------------------------------------------------
// La ficha
// ---------------------------------------------------------------------------------------------

function cpFicha(correo) {
	$('#cpCandidatos').empty();
	$('#cpMensaje').empty();
	$('#cpCargando').show();
	$.ajax({
		url: server + 'ConsultarPuntosCliente',
		dataType: 'json',
		type: 'get',
		data: { accion: 'FICHA', correo: correo },
		success: function (d) {
			$('#cpCargando').hide();
			if (d.resultado !== 'OK') {
				cpMensaje(cpEscapar(d.mensaje), 'rojo');
				return;
			}
			cpPintar(d);
		},
		error: function () {
			$('#cpCargando').hide();
			cpMensaje('No se pudo consultar. Intente de nuevo.', 'rojo');
		}
	});
}

function cpPintar(d) {
	var f = d.ficha;
	var t = d.totales;
	var c = d.comportamiento;

	$('#cpNombre').text(f.nombre || '(sin nombre en el CRM)');
	var datos = [];
	datos.push('<b>' + cpEscapar(f.correo) + '</b>');
	if (f.celular) {
		datos.push('Celular <b>' + cpEscapar(f.celular) + '</b>');
	}
	if (f.vinculado) {
		datos.push('En el programa desde <b>' + cpFecha(f.vinculado) + '</b>');
	}
	$('#cpDatos').html(datos.join(' &nbsp;·&nbsp; '));
	var estado = '';
	if (!f.enprograma) {
		estado = '<span class="etq etq-no">No vinculado al programa de puntos</span>';
	} else if (f.activo) {
		estado = '<span class="etq etq-ok">En el programa · Activo</span>';
	} else {
		estado = '<span class="etq etq-no">En el programa · Inactivo</span>';
	}
	// El segmento, con su color y su explicacion. "ORO" solo no le dice nada a
	// quien atiende; "compra seguido y compro hace poco" si. El color y el texto
	// salen de crm.segmento_definicion, no escritos aqui: si manana se renombra
	// un segmento o se cambia su color, esta pantalla lo sigue sola.
	var seg = d.segmento || {};
	var nombreSeg = seg.nombre || (c.hay ? c.segmento : '');
	if (nombreSeg) {
		var estilo = seg.color ? ' style="background:' + cpEscapar(seg.color) + ';color:#fff;"' : '';
		estado += '<span class="etq etq-seg"' + estilo + ' title="' + cpEscapar(seg.descripcion || '') + '">'
			+ cpEscapar(nombreSeg) + '</span>';
		if (seg.descripcion) {
			estado += '<div class="cp-segdesc">' + cpEscapar(seg.descripcion) + '</div>';
		}
	}
	$('#cpEstado').html(estado);

	// Avisos: lo que quien atiende tiene que saber antes de contestar.
	var avisos = '';
	if (!f.enprograma) {
		avisos += '<div class="cp-aviso">Este cliente no está vinculado al programa de puntos' +
			(t.acumulaciones > 0 ? ', pero tiene movimientos registrados.' : ': no acumula puntos.') + '</div>';
	} else if (!f.activo) {
		avisos += '<div class="cp-aviso rojo">La cuenta de puntos está <b>inactiva</b>: no acumula ni redime hasta que se active.</div>';
	}
	if (f.enprograma && !t.saldocuadra) {
		avisos += '<div class="cp-aviso rojo">El saldo guardado en la ficha (<b>' + cpPuntos(f.saldoguardado) +
			'</b>) no coincide con el que sale de los movimientos (<b>' + cpPuntos(t.saldo) +
			'</b>). Dígale al cliente el de los movimientos y avise a sistemas para corregir la ficha.</div>';
	}
	if (t.porvencer15 > 0) {
		avisos += '<div class="cp-aviso rojo"><b>Urgente, dígaselo al cliente:</b> se le vencen <b>' + cpPuntos(t.porvencer15) +
			' puntos en los próximos 15 días</b>' +
			(t.proximovencimiento ? ' (el primero el ' + cpFecha(t.proximovencimiento) + ')' : '') + '.</div>';
	} else if (t.porvencer > 0) {
		avisos += '<div class="cp-aviso"><b>Aviso para el cliente:</b> tiene <b>' + cpPuntos(t.porvencer) +
			' puntos que se le vencen en los próximos ' + t.diasaviso + ' días</b>' +
			(t.proximovencimiento ? ' (el primero el ' + cpFecha(t.proximovencimiento) + ')' : '') + '.</div>';
	}
	$('#cpAvisos').html(avisos);

	var proximo = t.proximovencimiento
		? 'Próximo: ' + cpFecha(t.proximovencimiento) + ' (' + cpPuntos(t.puntosproximovencimiento) + ')'
		: 'Nada por vencer';
	$('#cpTarjetas').html(
		cpTarjeta('Puntos disponibles', cpPuntos(t.saldo), 'saldo hoy', 'grande') +
		cpTarjetaVence(t, proximo) +
		cpTarjeta('Ganados en total', cpPuntos(t.ganados), t.acumulaciones + ' compra(s) · ' + cpPesos(t.valorcompras), '') +
		cpTarjeta('Redimidos', cpPuntos(t.redimidos), t.redencionesconfirmadas + ' redención(es)' +
			(t.redencionesreversadas > 0 ? ' · ' + t.redencionesreversadas + ' reversada(s)' : ''), '') +
		cpTarjeta('Vencidos', cpPuntos(t.vencidos), 'ya no se pueden usar', '')
	);

	// Movimientos
	cpMovimientos = d.movimientos || [];
	cpTipoActivo = 'TODOS';
	$('.cp-chip').removeClass('activo');
	$('.cp-chip[data-tipo="TODOS"]').addClass('activo');
	$('#cpFiltroTexto').val('');
	cpPintarMovimientos();

	cpPintarVencimientos(d.vencimientos || []);
	cpPintarComportamiento(c, d.portienda || [], d.pormes || []);
	cpPintarBonos(d.bonos || []);
	cpPintarPremios(d.catalogo || [], t.saldo);

	// Siempre se abre en Movimientos.
	$('#cpTabs li').removeClass('activo');
	$('#cpTabs li:first').addClass('activo');
	$('.cp-panel').removeClass('activo');
	$('#cpPMov').addClass('activo');
	$('#cpResultado').show();
}

function cpTarjeta(titulo, valor, apoyo, clase) {
	return ('<div class="cp-tarjeta ' + (clase || '') + '"><div class="k">' + titulo + '</div><div class="v">' + valor +
		'</div><div class="p">' + cpEscapar(apoyo) + '</div></div>');
}

/**
 * La tarjeta de lo que se vence: 15, 30 y 60 dias. Cada ventana incluye a la anterior
 * ("en 30 dias" son TODOS los que caen de hoy a 30 dias, no solo los de la segunda quincena).
 */
function cpTarjetaVence(t, proximo) {
	var clase = t.porvencer15 > 0 ? 'urgente' : (t.porvencer > 0 ? 'alerta' : '');
	return ('<div class="cp-tarjeta ' + clase + '"><div class="k">Por vencer</div>' +
		'<div class="cp-vence"><span>En 15 días</span><b class="' + (t.porvencer15 > 0 ? 'menos' : '') + '">' + cpPuntos(t.porvencer15) + '</b></div>' +
		'<div class="cp-vence"><span>En 30 días</span><b>' + cpPuntos(t.porvencer30) + '</b></div>' +
		'<div class="cp-vence"><span>En ' + t.diasaviso + ' días</span><b>' + cpPuntos(t.porvencer) + '</b></div>' +
		'<div class="p">' + cpEscapar(proximo) + '</div></div>');
}

function cpEtiquetaTipo(tipo) {
	switch (tipo) {
		case 'ACUMULA': return ('<span class="etq etq-acu">Acumuló</span>');
		case 'REDIME': return ('<span class="etq etq-red">Redimió</span>');
		case 'REVERSA': return ('<span class="etq etq-rev">Devolución</span>');
		case 'VENCE': return ('<span class="etq etq-ven">Se venció</span>');
	}
	return (cpEscapar(tipo));
}

function cpPintarMovimientos() {
	var texto = $.trim($('#cpFiltroTexto').val()).toLowerCase();
	var html = '';
	var n = 0;
	for (var i = 0; i < cpMovimientos.length; i++) {
		var m = cpMovimientos[i];
		if (cpTipoActivo !== 'TODOS' && m.tipo !== cpTipoActivo) {
			continue;
		}
		if (texto !== '' && (m.tienda + ' ' + m.pedido).toLowerCase().indexOf(texto) === -1) {
			continue;
		}
		n++;
		var signo = m.puntos > 0 ? '+' : '';
		html += '<tr><td>' + cpFecha(m.momento) + '</td>' +
			'<td>' + cpEtiquetaTipo(m.tipo) + '</td>' +
			'<td>' + cpEscapar(m.tienda) + '</td>' +
			'<td class="num">' + (m.pedido > 0 ? m.pedido : '') + '</td>' +
			'<td class="num">' + (m.valor > 0 ? cpPesos(m.valor) : '') + '</td>' +
			'<td class="num ' + (m.puntos >= 0 ? 'mas' : 'menos') + '">' + signo + cpPuntos(m.puntos) + '</td>' +
			'<td>' + cpEscapar(m.canal) + (m.estado === 'REVERSADA' ? ' <span class="etq etq-rev">Reversada</span>' : '') + '</td>' +
			'<td>' + cpEscapar(m.detalle) + '</td></tr>';
	}
	if (html === '') {
		html = '<tr><td colspan="8" class="cp-vacio">No hay movimientos para mostrar.</td></tr>';
	}
	$('#cpMovimientos').html(html);
	$('#cpConteo').text(n + ' de ' + cpMovimientos.length + ' movimiento(s)');
}

function cpPintarVencimientos(lista) {
	var html = '';
	for (var i = 0; i < lista.length; i++) {
		var v = lista[i];
		var dias = cpDiasPara(v.fecha);
		var faltan = '';
		if (v.atrasado) {
			faltan = '<span class="etq etq-ven">Fecha ya pasada: pendiente de marcar como vencido</span>';
		} else if (dias !== null) {
			faltan = dias === 0 ? 'Hoy' : (dias === 1 ? 'Mañana' : dias + ' días');
			if (dias <= 60) {
				faltan = '<b class="menos">' + faltan + '</b>';
			}
		}
		html += '<tr><td>' + cpFecha(v.fecha) + '</td><td class="num">' + cpPuntos(v.puntos) + '</td><td>' + faltan + '</td></tr>';
	}
	if (html === '') {
		html = '<tr><td colspan="3" class="cp-vacio">No tiene puntos por vencer.</td></tr>';
	}
	$('#cpVencimientos').html(html);
}

function cpDato(rotulo, valor) {
	return ('<div class="cp-dato"><span>' + rotulo + '</span><span>' + valor + '</span></div>');
}

function cpPintarComportamiento(c, porTienda, porMes) {
	var html = '';
	if (!c.hay) {
		html = '<div class="cp-vacio">El CRM todavía no tiene el resumen de este cliente.</div>';
	} else {
		html += cpDato('Segmento', cpEscapar(c.segmento || '-'));
		html += cpDato('Pedidos en total', c.pedidos);
		html += cpDato('Compras en total', cpPesos(c.valor));
		html += cpDato('Ticket promedio', cpPesos(c.ticketpromedio));
		html += cpDato('Primer pedido', cpFecha(c.primerpedido) || '-');
		html += cpDato('Último pedido', (cpFecha(c.ultimopedido) || '-') + (c.ultimopedido ? ' (hace ' + c.diassincomprar + ' días)' : ''));
		html += cpDato('Cada cuánto compra', c.diasentrepedidos > 0 ? 'cada ' + c.diasentrepedidos + ' días' : '-');
		html += cpDato('Tienda habitual', cpEscapar(c.tiendahabitual || '-'));
		html += cpDato('Tiendas distintas', c.tiendasdistintas);
		html += cpDato('Pizzas en 12 meses', c.pizzas12m);
		if (c.familiafavorita) {
			html += cpDato('Lo que más pide', cpEscapar(c.familiafavorita));
		}
	}
	$('#cpComportamiento').html(html);

	var maxT = 0;
	var i;
	for (i = 0; i < porTienda.length; i++) {
		if (porTienda[i].pedidos > maxT) { maxT = porTienda[i].pedidos; }
	}
	html = '';
	for (i = 0; i < porTienda.length; i++) {
		var pt = porTienda[i];
		html += '<div class="cp-fila"><div class="et" title="' + cpEscapar(pt.tienda) + '">' + cpEscapar(pt.tienda) +
			'</div><div class="bar"><i style="width:' + Math.round(pt.pedidos * 100 / (maxT || 1)) + '%"></i></div><div class="cif">' +
			pt.pedidos + ' pedido(s) · ' + cpPuntos(pt.puntos) + ' pts</div></div>';
	}
	$('#cpPorTienda').html(html || '<div class="cp-vacio">Sin pedidos con puntos.</div>');

	var maxM = 0;
	for (i = 0; i < porMes.length; i++) {
		if (porMes[i].pedidos > maxM) { maxM = porMes[i].pedidos; }
	}
	html = '';
	for (i = 0; i < porMes.length; i++) {
		var pm = porMes[i];
		html += '<div class="cp-fila"><div class="et">' + cpMesTexto(pm.mes) + '</div><div class="bar"><i style="width:' +
			Math.round(pm.pedidos * 100 / (maxM || 1)) + '%"></i></div><div class="cif">' + pm.pedidos + ' pedido(s) · ' +
			cpPesos(pm.valor) + '</div></div>';
	}
	$('#cpPorMes').html(html || '<div class="cp-vacio">Sin pedidos con puntos en el último año.</div>');
}

/** Los premios con puntos y, frente al saldo de este cliente, cuales ya le alcanzan. */
function cpPintarPremios(lista, saldo) {
	var html = '';
	var alcanza = 0;
	for (var i = 0; i < lista.length; i++) {
		var p = lista[i];
		var estado;
		if (saldo >= p.puntos) {
			alcanza++;
			estado = '<span class="etq etq-ok">Le alcanza</span>';
		} else {
			estado = 'Le faltan <b>' + cpPuntos(p.puntos - saldo) + '</b> puntos';
		}
		html += '<tr><td>' + cpEscapar(p.nombre) + '</td><td>' + cpEscapar(p.tipo) + '</td><td class="num"><b>' +
			cpPuntos(p.puntos) + '</b></td><td>' + estado + '</td></tr>';
	}
	if (html === '') {
		html = '<tr><td colspan="4" class="cp-vacio">No hay productos con puntos en el catálogo.</td></tr>';
	}
	$('#cpPremios').html(html);
	$('#cpResumenPremios').html(lista.length === 0 ? 'No hay premios configurados.' :
		'Con sus <b>' + cpPuntos(saldo) + '</b> puntos, este cliente puede pedir <b>' + alcanza + ' de ' + lista.length + '</b> premios.');
}

function cpPintarBonos(lista) {
	var html = '';
	for (var i = 0; i < lista.length; i++) {
		var b = lista[i];
		html += '<tr><td>' + cpEscapar(b.nombre) + '</td><td>' + cpEscapar(b.codigo) + '</td><td class="num">' + cpPesos(b.valor) +
			'</td><td>' + cpEscapar(b.estado) + '</td><td>' + cpFecha(b.caducidad) + '</td></tr>';
	}
	if (html === '') {
		html = '<tr><td colspan="5" class="cp-vacio">Este cliente no tiene bonos.</td></tr>';
	}
	$('#cpBonos').html(html);
}

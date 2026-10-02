/**
 * Administracion del catalogo de promociones del reporte diario.
 *
 * La pantalla existe para que agregar una promocion deje de ser una tarea de
 * programador. El reporte viejo las traia quemadas en el codigo y por eso se
 * desactualizo: siete de las diez que vigilaba no vendian nada, y las tres mas
 * vendidas de la compania no aparecian.
 *
 * Por eso lo que mas cuida este archivo es la BUSQUEDA DE PRODUCTOS: nadie se
 * sabe los ids de memoria, y un producto equivocado hace que el reporte mida
 * algo que no es sin que nada avise. Por eso al lado de cada producto va lo que
 * vendio el ultimo mes, y se marca el que ya pertenece a otra promocion.
 */

var promociones = [];
var promoAbierta = 0;

$(function () {
	cargar();
	$('#btnCrear').on('click', crear);
	$('#nuevoNombre').on('keypress', function (e) { if (e.which === 13) { crear(); } });
});

function cargar() {
	$.getJSON('AdministrarPromocionesReporte', { que: 'listar' }, function (data) {
		if (!responde(data)) { return; }
		promociones = data.promociones || [];
		pintar();
	}).fail(function () {
		avisar('No se pudo cargar el catalogo.', false);
	});
}

function pintar() {
	if (promociones.length === 0) {
		$('#lista').html('<p class="nota">Todavia no hay promociones en el catalogo.</p>');
		return;
	}
	var html = '';
	for (var i = 0; i < promociones.length; i++) {
		var p = promociones[i];
		var apagada = (p.activo !== 'S');
		html += '<div class="promo' + (apagada ? ' apagada' : '') + '" data-id="' + p.idpromo + '">'
			+ '  <div class="promo-cab" data-id="' + p.idpromo + '">'
			+ '    <span class="promo-nombre">' + escapar(p.nombre) + '</span> '
			+ (p.plataforma === 'S'
				? '<span class="etiqueta et-plataforma">plataforma</span>'
				: '<span class="etiqueta et-propia">propia</span>')
			+ (apagada ? ' <span class="etiqueta et-apagada">apagada</span>' : '')
			+ '    <span class="nota" style="float:right;">' + p.productos.length
			+ ' producto' + (p.productos.length === 1 ? '' : 's')
			+ '&nbsp;&nbsp;<i class="fas fa-chevron-down"></i></span>'
			+ '  </div>'
			+ '  <div class="promo-cuerpo" id="cuerpo' + p.idpromo + '">' + cuerpo(p) + '</div>'
			+ '</div>';
	}
	$('#lista').html(html);

	$('.promo-cab').on('click', function () {
		var id = $(this).data('id');
		var caja = $('#cuerpo' + id);
		if (caja.is(':visible')) {
			caja.slideUp(120);
			promoAbierta = 0;
		} else {
			$('.promo-cuerpo').slideUp(120);
			caja.slideDown(120);
			promoAbierta = id;
		}
	});
	conectarBotones();
}

function cuerpo(p) {
	var html = '<div class="row"><div class="col-sm-5">'
		+ '<label class="rotulo-pr">Nombre</label>'
		+ '<input type="text" class="form-control f-nombre" data-id="' + p.idpromo + '" value="'
		+ escapar(p.nombre) + '" maxlength="80"></div>'
		+ '<div class="col-sm-3"><label class="rotulo-pr">Tipo</label>'
		+ '<select class="form-control f-tipo" data-id="' + p.idpromo + '">'
		+ '<option value="N"' + (p.plataforma !== 'S' ? ' selected' : '') + '>Propia</option>'
		+ '<option value="S"' + (p.plataforma === 'S' ? ' selected' : '') + '>De plataforma</option>'
		+ '</select></div>'
		+ '<div class="col-sm-2"><label class="rotulo-pr">Orden</label>'
		+ '<input type="text" class="form-control f-orden" data-id="' + p.idpromo + '" value="'
		+ p.orden + '" style="text-align:right;"></div>'
		+ '<div class="col-sm-2"><label class="rotulo-pr">En el reporte</label>'
		+ '<select class="form-control f-activo" data-id="' + p.idpromo + '">'
		+ '<option value="S"' + (p.activo === 'S' ? ' selected' : '') + '>Si</option>'
		+ '<option value="N"' + (p.activo !== 'S' ? ' selected' : '') + '>No</option>'
		+ '</select></div></div>'
		+ '<div style="margin-top:10px;"><button class="btn btn-primary btn-sm b-guardar" data-id="'
		+ p.idpromo + '">Guardar cambios</button></div>';

	html += '<hr><label class="rotulo-pr">Productos que la componen</label>';
	if (p.productos.length === 0) {
		html += '<p class="nota">Sin productos. <strong>Una promocion sin productos no mide nada</strong>'
			+ ' y va a salir siempre en cero.</p>';
	} else {
		for (var j = 0; j < p.productos.length; j++) {
			var pr = p.productos[j];
			html += '<div class="prod">'
				+ '<span class="id">' + pr.idproducto + '</span>&nbsp; '
				+ escapar(pr.descripcion)
				+ '&nbsp;&nbsp;'
				+ (pr.vendidos > 0
					? '<span class="vend">' + numero(pr.vendidos) + ' el ultimo mes</span>'
					: '<span class="cero">sin ventas el ultimo mes</span>')
				+ '<button class="btn btn-default btn-xs b-quitar" style="float:right;" data-promo='
				+ p.idpromo + ' data-prod=' + pr.idproducto + '>Quitar</button>'
				+ '</div>';
		}
	}

	html += '<hr><label class="rotulo-pr">Agregar un producto</label>'
		+ '<div class="row"><div class="col-sm-9">'
		+ '<input type="text" class="form-control f-buscar" data-id="' + p.idpromo + '"'
		+ ' placeholder="Escriba parte del nombre, por ejemplo: insuperable" autocomplete="off"></div>'
		+ '<div class="col-sm-3"><button class="btn btn-default btn-block b-buscar" data-id="'
		+ p.idpromo + '">Buscar</button></div></div>'
		+ '<div class="resultado" id="res' + p.idpromo + '" style="display:none;"></div>';
	return (html);
}

function conectarBotones() {
	$('.b-guardar').off('click').on('click', function (e) {
		e.stopPropagation();
		guardar($(this).data('id'));
	});
	$('.b-quitar').off('click').on('click', function (e) {
		e.stopPropagation();
		quitar($(this).data('promo'), $(this).data('prod'));
	});
	$('.b-buscar').off('click').on('click', function (e) {
		e.stopPropagation();
		buscar($(this).data('id'));
	});
	$('.f-buscar').off('keypress').on('keypress', function (e) {
		if (e.which === 13) { e.preventDefault(); buscar($(this).data('id')); }
	});
	//Que un clic dentro del formulario no cierre el panel.
	$('.promo-cuerpo').off('click').on('click', function (e) { e.stopPropagation(); });
}

function crear() {
	var nombre = $.trim($('#nuevoNombre').val());
	if (nombre.length < 3) {
		avisar('Escriba el nombre de la promocion.', false);
		return;
	}
	$.post('AdministrarPromocionesReporte', {
		que: 'crear', nombre: nombre,
		plataforma: $('#nuevoTipo').val(),
		orden: $.trim($('#nuevoOrden').val())
	}, function (data) {
		if (!responde(data)) { return; }
		$('#nuevoNombre').val('');
		avisar('Promocion creada. Ahora agreguele sus productos: sin productos no mide nada.', true);
		cargar();
	}, 'json').fail(function () { avisar('No se pudo crear.', false); });
}

function guardar(idpromo) {
	$.post('AdministrarPromocionesReporte', {
		que: 'actualizar',
		idpromo: idpromo,
		nombre: $.trim($('.f-nombre[data-id="' + idpromo + '"]').val()),
		plataforma: $('.f-tipo[data-id="' + idpromo + '"]').val(),
		activo: $('.f-activo[data-id="' + idpromo + '"]').val(),
		orden: $.trim($('.f-orden[data-id="' + idpromo + '"]').val())
	}, function (data) {
		if (!responde(data)) { return; }
		avisar('Guardado.', true);
		cargar();
	}, 'json').fail(function () { avisar('No se pudo guardar.', false); });
}

function buscar(idpromo) {
	var texto = $.trim($('.f-buscar[data-id="' + idpromo + '"]').val());
	if (texto.length < 2) {
		avisar('Escriba al menos dos letras para buscar.', false);
		return;
	}
	$.getJSON('AdministrarPromocionesReporte', { que: 'buscar', texto: texto }, function (data) {
		if (!responde(data)) { return; }
		var caja = $('#res' + idpromo);
		if (!data.productos || data.productos.length === 0) {
			caja.html('<div class="fila nota">Ningun producto se llama asi. '
				+ 'Puede que el nombre en la tienda sea distinto al del aviso.</div>').show();
			return;
		}
		var html = '';
		for (var i = 0; i < data.productos.length; i++) {
			var p = data.productos[i];
			html += '<div class="fila">'
				+ '<span class="id" style="color:#888;font-size:11.5px;">' + p.idproducto + '</span>&nbsp; '
				+ escapar(p.descripcion) + '&nbsp;&nbsp;'
				+ (p.vendidos > 0
					? '<span class="vend">' + numero(p.vendidos) + ' el ultimo mes</span>'
					: '<span class="cero">sin ventas el ultimo mes</span>')
				+ (p.yaen
					? '<br><span class="ocupado">Ya esta en &quot;' + escapar(p.yaen) + '&quot;</span>'
					: '<button class="btn btn-primary btn-xs b-agregar" style="float:right;" data-promo='
						+ idpromo + ' data-prod=' + p.idproducto + '>Agregar</button>')
				+ '</div>';
		}
		caja.html(html).show();
		$('.b-agregar').off('click').on('click', function (e) {
			e.stopPropagation();
			agregar($(this).data('promo'), $(this).data('prod'));
		});
	}).fail(function () { avisar('No se pudo buscar. Puede que ninguna tienda este respondiendo.', false); });
}

function agregar(idpromo, idproducto) {
	$.post('AdministrarPromocionesReporte', {
		que: 'agregar', idpromo: idpromo, idproducto: idproducto
	}, function (data) {
		if (!responde(data)) { return; }
		avisar('Producto agregado.', true);
		cargar();
	}, 'json').fail(function () { avisar('No se pudo agregar.', false); });
}

function quitar(idpromo, idproducto) {
	if (!confirm('Quitar el producto ' + idproducto + ' de esta promocion?\n\n'
		+ 'Lo que ya esta guardado en la historia no se borra: desde manana deja de contarse.')) {
		return;
	}
	$.post('AdministrarPromocionesReporte', {
		que: 'quitar', idpromo: idpromo, idproducto: idproducto
	}, function (data) {
		if (!responde(data)) { return; }
		avisar('Producto quitado.', true);
		cargar();
	}, 'json').fail(function () { avisar('No se pudo quitar.', false); });
}

function responde(data) {
	if (!data) {
		avisar('El servidor no respondio.', false);
		return (false);
	}
	if (data.respuesta === 'OK') { return (true); }
	if (data.respuesta === 'NOSESION') {
		avisar('Se cerro la sesion. Vuelva a entrar.', false);
		setTimeout(function () { location.reload(); }, 1500);
		return (false);
	}
	avisar(data.detalle ? data.detalle : 'No se pudo completar la operacion.', false);
	return (false);
}

function avisar(texto, bueno) {
	var caja = $('#aviso');
	caja.removeClass('aviso-ok aviso-mal').addClass(bueno ? 'aviso-ok' : 'aviso-mal')
		.text(texto).show();
	$('html, body').animate({ scrollTop: 0 }, 200);
	if (bueno) {
		setTimeout(function () { caja.fadeOut(400); }, 3500);
	}
}

/** Medias unidades existen: una pizza mitad y mitad cuenta 0,5. */
function numero(valor) {
	if (valor === null || valor === undefined || isNaN(valor)) { return ('0'); }
	var redondeado = Math.round(valor * 10) / 10;
	return (redondeado === Math.floor(redondeado)
		? String(Math.floor(redondeado))
		: String(redondeado).replace('.', ','));
}

function escapar(texto) {
	if (texto === null || texto === undefined) { return (''); }
	return (String(texto).replace(/&/g, '&amp;').replace(/</g, '&lt;')
		.replace(/>/g, '&gt;').replace(/"/g, '&quot;'));
}

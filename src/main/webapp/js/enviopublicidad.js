/*
 * Envio de publicidad.
 *
 * Reemplaza a "Segmentacion Cliente" en lo que esa hacia mal: agrupaba por
 * correo, asi que quien tenia dos correos recibia dos veces y las 43.177
 * personas que compran sin correo no existian. Aqui el publico son PERSONAS, y
 * el canal decide por donde se les llega.
 *
 * ES5 -var y function-: convive con jQuery 1.11.
 */

var epCanal = 'C';
var epAlcance = null;
var epCampana = 0;
var epReloj = null;

$(document).ready(function () {

	epCargarTiendas();
	epCargarSegmentos();
	epCargarPlantillas();
	epCargarHistorial();
	epCargarCatalogos();

	$('#ep-masfiltros').click(function (e) {
		e.preventDefault();
		$('#ep-avanzados').slideToggle();
	});

	$('#ep-canales').on('click', '.ep-canal', function () {
		$('#ep-canales .ep-canal').removeClass('sel');
		$(this).addClass('sel');
		epCanal = $(this).data('canal');
		epPintarCanal();
	});

	$('#ep-universo').change(function () {
		//El universo completo y los filtros se pelean: si estan los dos, nadie
		//sabe cual mando. Se apaga uno.
		$('#ep-filtros').toggle(!this.checked);
		epLimpiarAlcance();
	});

	$('#ep-contar').click(epContar);
	$('#ep-enviar').click(epEnviar);
	$('#ep-probar').click(epProbar);
	$('#ep-detener').click(epDetener);

	//Cualquier cambio de filtro invalida la cuenta: no se puede enviar contra
	//un numero que ya no corresponde a lo que esta en pantalla.
	$('#ep-filtros').on('change', 'input,select', epLimpiarAlcance);

	epPintarCanal();
});

// ===========================================================================
// Plomeria
// ===========================================================================

function epEscapar(t) {
	return ($('<div/>').text(t === null || t === undefined ? '' : t).html());
}

function epMiles(n) {
	var texto = String(Math.round(Number(n) || 0));
	var salida = '';
	for (var i = 0; i < texto.length; i++) {
		if (i > 0 && (texto.length - i) % 3 === 0) { salida += '.'; }
		salida += texto.charAt(i);
	}
	return (salida);
}

function epPesos(v) { return ('$' + epMiles(v)); }

function epMensaje(clase, texto) {
	$('#ep-mensaje').html('<div class="ep-aviso ' + clase + '">' + texto + '</div>');
}

function epNombreCanal(c) {
	if (c === 'W') { return ('WhatsApp por Brevo'); }
	if (c === 'D') { return ('Correo directo'); }
	return ('Correo por Brevo');
}

/* Los filtros tal como los espera el servlet. */
function epFiltro() {
	var d = { accion: 'alcance' };
	if ($('#ep-universo').is(':checked')) {
		d.universo = 'S';
		return (d);
	}
	var segmentos = $('#ep-segmentos').val();
	if (segmentos && segmentos.length > 0) { d.segmentos = segmentos.join(','); }
	if ($('#ep-tienda').val()) { d.idtienda = $('#ep-tienda').val(); }
	if ($('#ep-pedidosmin').val()) { d.pedidosmin = $('#ep-pedidosmin').val(); }
	if ($('#ep-valormin').val()) { d.valormin = $('#ep-valormin').val(); }
	if ($('#ep-diasmin').val()) { d.diasmin = $('#ep-diasmin').val(); }
	if ($('#ep-diasmax').val()) { d.diasmax = $('#ep-diasmax').val(); }
	d.canal = $('#ep-canalventa').val();

	//Los que venian de la pantalla anterior.
	if ($('#ep-diassinpublicidad').val()) { d.diassinpublicidad = $('#ep-diassinpublicidad').val(); }
	if ($('#ep-excluirplataformas').is(':checked')) { d.excluirplataformas = 'S'; }
	if ($('#ep-pedidosmax').val()) { d.pedidosmax = $('#ep-pedidosmax').val(); }
	if ($('#ep-puntosmin').val()) { d.puntosmin = $('#ep-puntosmin').val(); }
	if ($('#ep-soloclub').is(':checked')) { d.soloclub = 'S'; }
	if ($('#ep-correo').val()) { d.correo = $('#ep-correo').val(); }
	if ($('#ep-comprodesde').val()) { d.comprodesde = $('#ep-comprodesde').val(); }
	if ($('#ep-comprohasta').val()) { d.comprohasta = $('#ep-comprohasta').val(); }
	epLista(d, 'tiposcliente', '#ep-tiposcliente');
	epLista(d, 'productos', '#ep-productos');
	epLista(d, 'especialidades', '#ep-especialidades');
	epLista(d, 'promociones', '#ep-promociones');
	return (d);
}

/* Las listas van separadas por coma, como en el resto de la pantalla. */
function epLista(destino, campo, selector) {
	var v = $(selector).val();
	if (v && v.length > 0) { destino[campo] = v.join(','); }
}

// ===========================================================================
// Cargas iniciales
// ===========================================================================

function epCargarTiendas() {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'tiendas' }, function (d) {
		var html = '<option value="">Todas</option>';
		var lista = (d && d.tiendas) ? d.tiendas : [];
		for (var i = 0; i < lista.length; i++) {
			html += '<option value="' + lista[i].idtienda + '">' + epEscapar(lista[i].nombre) + '</option>';
		}
		$('#ep-tienda').html(html);
	}).fail(function () {
		$('#ep-tienda').html('<option value="">Todas</option>');
	});
}

function epCargarSegmentos() {
	//Los segmentos salen de la misma definicion que usa la pantalla de
	//segmentacion: no se inventan aqui.
	var fijos = ['NUEVO', 'ACTIVO', 'FIEL', 'EN RIESGO', 'POR RECUPERAR', 'PERDIDO', 'UNICA COMPRA'];
	var html = '';
	for (var i = 0; i < fijos.length; i++) {
		html += '<option value="' + fijos[i] + '">' + fijos[i] + '</option>';
	}
	$('#ep-segmentos').html(html);
}

/*
 * Productos, especialidades y promociones para los filtros avanzados.
 *
 * Cada carga va por su lado y falla sola: si un catalogo no responde, ese
 * filtro queda vacio pero la pantalla sirve igual. Son filtros opcionales y no
 * pueden tumbar el envio.
 */
function epCargarCatalogos() {
	epLlenarSelect('#ep-productos', 'GetTodosProductos',
		['idproducto', 'id'], ['descripcion', 'nombre', 'nombreproducto']);
	epLlenarSelect('#ep-especialidades', 'GetEspecialidades?idexcepcion=0&idproducto=0',
		['idespecialidad', 'id'], ['descripcion', 'nombre']);
	epLlenarSelect('#ep-promociones', 'getExcepcionesPrecio',
		['idexcepcion', 'id'], ['descripcion', 'nombre', 'nombre_excepcion']);
}

/*
 * Llena un desplegable de un servicio que no siempre devuelve lo mismo.
 *
 * Los servicios viejos del central responden a veces un arreglo pelado y a
 * veces un objeto con la lista adentro, y los nombres de campo cambian entre
 * uno y otro. En vez de averiguar cada caso, se prueban los nombres posibles:
 * es codigo de pantalla, no vale la pena tocar seis servicios que ya andan.
 */
function epLlenarSelect(selector, url, campoId, campoNombre) {
	$.getJSON(server + url, function (d) {
		var lista = d;
		if (!$.isArray(lista)) {
			for (var k in d) {
				if (d.hasOwnProperty(k) && $.isArray(d[k])) { lista = d[k]; break; }
			}
		}
		if (!$.isArray(lista)) { return; }
		var html = '';
		for (var i = 0; i < lista.length; i++) {
			var id = epPrimero(lista[i], campoId);
			var nom = epPrimero(lista[i], campoNombre);
			if (id) { html += '<option value="' + id + '">' + epEscapar(nom || id) + '</option>'; }
		}
		$(selector).html(html);
	}).fail(function () {
		//Sin catalogo, el filtro queda vacio. No se avisa: es opcional.
	});
}

function epPrimero(obj, nombres) {
	for (var i = 0; i < nombres.length; i++) {
		if (obj[nombres[i]] !== undefined && obj[nombres[i]] !== null) { return (obj[nombres[i]]); }
	}
	return ('');
}

function epCargarPlantillas() {
	$.getJSON(server + 'ObtenerPlantillaBrevo', function (d) {
		var lista = d && d.plantillas ? d.plantillas : (d && d.length ? d : []);
		var html = '<option value="">Escoja la plantilla</option>';
		for (var i = 0; i < lista.length; i++) {
			var p = lista[i];
			var id = p.id || p.idplantilla || p.templateId;
			var nom = p.nombre || p.name || ('Plantilla ' + id);
			html += '<option value="' + id + '">' + epEscapar(nom) + '</option>';
		}
		$('#ep-plantilla').html(html);
	}).fail(function () {
		$('#ep-plantilla').html('<option value="">No se pudieron cargar</option>');
	});
}

// ===========================================================================
// El canal
// ===========================================================================

function epPintarCanal() {
	var directo = (epCanal === 'D');
	$('#ep-caja-plantilla').toggle(!directo);
	$('#ep-caja-cuerpo').toggle(directo);
	epPintarAlcance();
}

// ===========================================================================
// Contar el publico
// ===========================================================================

function epLimpiarAlcance() {
	epAlcance = null;
	$('#ep-enviar').prop('disabled', true);
	$('#ep-cifras').html('<p class="ep-nota" style="margin:0;">El filtro cambi&oacute;. ' +
		'Vuelva a contar antes de enviar.</p>');
	$('#ep-excluidos').html('');
}

function epContar() {
	$('#ep-mensaje').html('');
	$('#ep-contar').prop('disabled', true).text('Contando...');
	$.getJSON(server + 'EnvioPublicidad', epFiltro(), function (d) {
		$('#ep-contar').prop('disabled', false).text('Contar a cuántos le llega');
		if (d.error) { epMensaje('ep-aviso-mal', epEscapar(d.error)); return; }
		epAlcance = d;
		epPintarAlcance();
	}).fail(function () {
		$('#ep-contar').prop('disabled', false).text('Contar a cuántos le llega');
		epMensaje('ep-aviso-mal', 'No se pudo contar el p&uacute;blico.');
	});
}

function epPintarAlcance() {
	if (!epAlcance) { return; }
	var d = epAlcance;

	//Cuanta gente alcanza ESTE canal. Es el numero que importa: por correo no
	//se le llega a quien no tiene correo, por mas que cumpla el filtro.
	var alcanza = (epCanal === 'W') ? d.con_celular : d.con_correo;
	var tope = d.tope_directo || 50;
	if (epCanal === 'D' && alcanza > tope) { alcanza = tope; }

	$('#ep-det-c').html(epMiles(d.con_correo) + ' con correo');
	$('#ep-det-w').html(epMiles(d.con_celular) + ' con celular');
	$('#ep-det-d').html('m&aacute;ximo ' + tope + ', uno cada ' + (d.segundos_directo || 30) + ' segundos');

	$('#ep-cifras').html(
		'<div class="ep-cifra"><div class="valor">' + epMiles(alcanza) + '</div>' +
		'<div class="rotulo">personas por ' + epNombreCanal(epCanal) + '</div></div>' +
		'<p class="ep-nota" style="text-align:center;margin:0;">' +
		'El filtro encontr&oacute; ' + epMiles(d.personas) + ' personas en total.</p>');

	var extra = '';
	if (d.sin_consentimiento > 0) {
		extra += '<div class="ep-cifra ep-excluidos"><div class="valor">' +
			epMiles(d.sin_consentimiento) + '</div>' +
			'<div class="rotulo">excluidas: no autorizaron</div></div>';
	}
	if (epCanal === 'D') {
		var minutos = Math.round(alcanza * (d.segundos_directo || 30) / 60);
		extra += '<div class="ep-peligro">El correo directo sale de la cuenta de la empresa. ' +
			'Se manda de a uno cada ' + (d.segundos_directo || 30) + ' segundos para no quemar la ' +
			'reputaci&oacute;n del dominio: estos ' + alcanza + ' se demoran unos <b>' + minutos +
			' minutos</b>. Lo hace el servidor, as&iacute; que puede cerrar esta pantalla.</div>';
	} else if (alcanza > 20000) {
		extra += '<div class="ep-peligro">Le va a escribir a m&aacute;s de ' + epMiles(alcanza) +
			' personas. Revise que el filtro sea el que quiere antes de enviar.</div>';
	}
	$('#ep-excluidos').html(extra);

	$('#ep-enviar').prop('disabled', alcanza === 0)
		.text(alcanza === 0 ? 'No hay a quién enviarle'
			: 'Enviar a ' + epMiles(alcanza) + ' personas');
}

// ===========================================================================
// Probar
// ===========================================================================

function epProbar() {
	var destino = $('#ep-prueba').val();
	if (!destino) { epMensaje('ep-aviso-mal', 'Escriba a d&oacute;nde quiere la prueba.'); return; }
	$('#ep-probar').prop('disabled', true).text('Enviando...');
	$.getJSON(server + 'EnvioPublicidad', {
		accion: 'probar', canal: epCanal, destino: destino,
		idplantilla: $('#ep-plantilla').val(), asunto: $('#ep-asunto').val(),
		cuerpo: $('#ep-cuerpo').val()
	}, function (d) {
		$('#ep-probar').prop('disabled', false).text('Probar');
		if (d.error) { epMensaje('ep-aviso-mal', epEscapar(d.error)); return; }
		epMensaje(d.exito ? 'ep-aviso-ok' : 'ep-aviso-mal',
			d.exito ? 'Prueba enviada. Rev&iacute;sela antes de disparar la campa&ntilde;a.'
				: 'No sali&oacute;: ' + epEscapar(d.detalle));
	}).fail(function () {
		$('#ep-probar').prop('disabled', false).text('Probar');
		epMensaje('ep-aviso-mal', 'No se pudo enviar la prueba.');
	});
}

// ===========================================================================
// Enviar
// ===========================================================================

function epEnviar() {
	if (!epAlcance) { epMensaje('ep-aviso-mal', 'Cuente el p&uacute;blico primero.'); return; }
	var nombre = $('#ep-nombre').val();
	if (!nombre) { epMensaje('ep-aviso-mal', 'P&oacute;ngale nombre a la campa&ntilde;a.'); return; }

	var alcanza = (epCanal === 'W') ? epAlcance.con_celular : epAlcance.con_correo;
	if (epCanal === 'D' && alcanza > (epAlcance.tope_directo || 50)) {
		alcanza = epAlcance.tope_directo || 50;
	}

	//Confirmacion con el numero escrito: que nadie pueda decir que no sabia a
	//cuanta gente le estaba escribiendo.
	if (!confirm('Va a enviar "' + nombre + '" por ' + epNombreCanal(epCanal) +
			' a ' + epMiles(alcanza) + ' personas.\n\nEsto no se puede deshacer. Continuar?')) {
		return;
	}

	var datos = epFiltro();
	datos.accion = 'enviar';
	datos.nombre = nombre;
	datos.canal = epCanal;
	datos.idplantilla = $('#ep-plantilla').val();
	datos.asunto = $('#ep-asunto').val();
	datos.cuerpo = $('#ep-cuerpo').val();

	$('#ep-enviar').prop('disabled', true).text('Enviando...');
	$.post(server + 'EnvioPublicidad', datos, function (d) {
		if (d.error) {
			epMensaje('ep-aviso-mal', epEscapar(d.error));
			$('#ep-enviar').prop('disabled', false);
			epPintarAlcance();
			return;
		}
		epCampana = d.idcampana;
		epMensaje('ep-aviso-ok', epEscapar(d.mensaje));
		$('#ep-avance').show();
		$('#ep-detener').toggle(epCanal === 'D');
		epSeguirAvance();
		epCargarHistorial();
	}, 'json').fail(function () {
		epMensaje('ep-aviso-mal', 'No se pudo lanzar la campa&ntilde;a.');
		$('#ep-enviar').prop('disabled', false);
		epPintarAlcance();
	});
}

function epSeguirAvance() {
	if (epReloj) { clearInterval(epReloj); }
	epReloj = setInterval(function () {
		$.getJSON(server + 'EnvioPublicidad', { accion: 'avance', idcampana: epCampana },
			function (d) {
				if (d.error) { return; }
				var hechos = d.enviados + d.fallidos;
				var pct = d.publico > 0 ? Math.round(hechos * 100 / d.publico) : 0;
				$('#ep-barra-int').css('width', pct + '%');
				var texto = epMiles(d.enviados) + ' enviados de ' + epMiles(d.publico);
				if (d.fallidos > 0) { texto += ' &middot; ' + epMiles(d.fallidos) + ' fallidos'; }
				if (d.minutos_restantes) { texto += ' &middot; faltan unos ' + d.minutos_restantes + ' min'; }
				texto += ' &middot; ' + d.estado;
				$('#ep-avance-texto').html(texto);

				if (d.estado === 'TERMINADA' || d.estado === 'CANCELADA') {
					clearInterval(epReloj);
					epReloj = null;
					$('#ep-detener').hide();
					$('#ep-enviar').prop('disabled', false);
					epPintarAlcance();
					epCargarHistorial();
				}
			});
	}, 5000);
}

function epDetener() {
	if (!confirm('Detener el envío? Lo que ya salió no se puede devolver.')) { return; }
	$.getJSON(server + 'EnvioPublicidad', { accion: 'detener' }, function () {
		epMensaje('ep-aviso-info', 'Env&iacute;o detenido.');
	});
}

// ===========================================================================
// Historial y resultado
// ===========================================================================

function epCargarHistorial() {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'ultimas', cuantas: 25 }, function (d) {
		var lista = d.campanas || [];
		var cuerpo = $('#ep-tabla tbody');
		cuerpo.empty();
		if (lista.length === 0) {
			cuerpo.html('<tr><td colspan="8" class="ep-nota">Todav&iacute;a no hay campa&ntilde;as.</td></tr>');
			return;
		}
		for (var i = 0; i < lista.length; i++) {
			var c = lista[i];
			var fila = $('<tr>').css('cursor', 'pointer');
			fila.append($('<td>').text(c.idcampana));
			fila.append($('<td>').text(c.nombre));
			fila.append($('<td>').text(epNombreCanal(c.canal)));
			fila.append($('<td>').text(c.estado));
			fila.append($('<td class="ep-num">').text(epMiles(c.publico)));
			fila.append($('<td class="ep-num">').text(epMiles(c.enviados)));
			fila.append($('<td class="ep-num">').text(epMiles(c.fallidos)));
			fila.append($('<td>').text(String(c.creada_en || '').substring(0, 16)));
			fila.data('id', c.idcampana);
			fila.click(function () { epVerResultado($(this).data('id')); });
			cuerpo.append(fila);
		}
	});
}

function epVerResultado(idCampana) {
	$('#ep-resultado').html('<p class="ep-nota">Midiendo...</p>');
	$.getJSON(server + 'EnvioPublicidad',
		{ accion: 'resultado', idcampana: idCampana, horas: 24 }, function (d) {
			if (d.error) { $('#ep-resultado').html(''); return; }
			$('#ep-resultado').html(
				'<div class="ep-panel" style="margin-top:14px;">' +
				'<div class="ep-titulo">Campa&ntilde;a ' + idCampana + ', primeras ' + d.horas + ' horas</div>' +
				'<div class="row">' +
				'<div class="col-md-3"><div class="ep-cifra"><div class="valor">' +
					epMiles(d.enviados) + '</div><div class="rotulo">recibieron</div></div></div>' +
				'<div class="col-md-3"><div class="ep-cifra"><div class="valor">' +
					epMiles(d.compraron) + '</div><div class="rotulo">compraron despu&eacute;s</div></div></div>' +
				'<div class="col-md-3"><div class="ep-cifra"><div class="valor">' +
					d.porcentaje + '%</div><div class="rotulo">de los que recibieron</div></div></div>' +
				'<div class="col-md-3"><div class="ep-cifra"><div class="valor">' +
					epPesos(d.valor) + '</div><div class="rotulo">vendido</div></div></div>' +
				'</div>' +
				'<p class="ep-nota">' + epEscapar(d.advertencia) + '</p></div>');
		});
}

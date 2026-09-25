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
var epEnvio = 0;
var epReloj = null;
var epCampanas = [];

$(document).ready(function () {

	epCargarTiendas();
	epCargarSegmentos();
	epCargarPlantillas();
	epCargarCampanas();
	epCargarHistorial();
	epCargarCatalogos();

	//Crear campana nueva o volver a la lista. Son dos modos y no dos campos
	//sueltos: con los dos a la vista, nadie sabe cual manda.
	$('#ep-nuevacampana').click(function (e) {
		e.preventDefault();
		epModoNueva(true);
	});
	$('#ep-volvercampana').click(function (e) {
		e.preventDefault();
		epModoNueva(false);
	});

	$('#ep-campana').change(epTraerCampana);

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

	//El tope no cambia el publico, solo cuantos salen hoy: no invalida la
	//cuenta, se repinta y ya.
	$('#ep-tope').on('change keyup', epPintarAlcance);

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
	//OJO: "canal" aqui es POR DONDE COMPRA -mostrador o domicilio-, que es lo
	//que espera SegmentacionPersonaCtrl.filtroDe. El canal por el que sale la
	//campana va aparte, como canalenvio. Tuvieron el mismo nombre y el de envio
	//le pisaba el valor a este: el 2026-09-25 la pantalla conto 53 personas de
	//mostrador y mando a 3.135, porque al enviar el filtro de mostrador se
	//perdia.
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

/*
 * Los segmentos salen de crm.segmento_definicion, no de una lista escrita aqui.
 *
 * Estaban escritos a mano y la lista quedo vieja: faltaban ORO -el mejor
 * publico que hay- y CASI PERDIDO, y sobraba UNICA COMPRA, que no existe. Un
 * segmento que falta en el filtro no da error: simplemente no se puede escoger,
 * y nadie se entera. Por eso se leen.
 */
function epCargarSegmentos() {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'segmentos' }, function (d) {
		var lista = (d && d.segmentos) ? d.segmentos : [];
		var html = '';
		for (var i = 0; i < lista.length; i++) {
			html += '<option value="' + epEscapar(lista[i]) + '">' + epEscapar(lista[i]) + '</option>';
		}
		$('#ep-segmentos').html(html);
	}).fail(function () {
		//Sin la lista no se puede segmentar. Se dice, en vez de dejar un
		//desplegable vacio que parece un error de la pantalla.
		$('#ep-segmentos').html('');
		epMensaje('ep-aviso-mal', 'No se pudo cargar la lista de segmentos. ' +
			'Puede enviar marcando <b>Todo el CRM</b>, o recargue la pantalla.');
	});
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

/*
 * El maestro de campanas, para el desplegable.
 *
 * De paso trae los dias de descanso por defecto -el parametro
 * PUBLICIDADDIASMINIMOS- y lo deja puesto en el filtro. Que venga puesto es lo
 * que hace que el caso normal -no volverle a escribir al que acaba de recibir-
 * no dependa de que alguien se acuerde de escribirlo.
 */
function epCargarCampanas(seleccionar) {
	$.getJSON(server + 'EnvioPublicidad', { accion: 'campanas' }, function (d) {
		epCampanas = (d && d.campanas) ? d.campanas : [];
		var html = '<option value="">-- escoja una campa&ntilde;a --</option>';
		for (var i = 0; i < epCampanas.length; i++) {
			var c = epCampanas[i];
			var detalle = c.envios > 0
				? (' (' + c.envios + (c.envios === 1 ? ' env&iacute;o' : ' env&iacute;os') + ')')
				: ' (sin enviar)';
			html += '<option value="' + c.idcampana + '">' + epEscapar(c.nombre) + detalle + '</option>';
		}
		$('#ep-campana').html(html);

		if ($('#ep-diassinpublicidad').val() === '') {
			$('#ep-diassinpublicidad').val(d && d.dias_defecto ? d.dias_defecto : 30);
		}
		//Recien creada una campana, se deja escogida: la tanda de manana sale
		//sin volver a escribir el nombre, que es de lo que se trata.
		if (seleccionar) {
			epModoNueva(false);
			$('#ep-campana').val(String(seleccionar));
			epTraerCampana();
		} else if (epCampanas.length === 0) {
			//Sin ninguna campana todavia, se arranca directo en modo nueva: no
			//tiene sentido mostrar una lista vacia y pedir que escoja de ella.
			epModoNueva(true);
		}
	}).fail(function () {
		$('#ep-campana').html('<option value="">No se pudieron cargar</option>');
		epModoNueva(true);
	});
}

/* Alterna entre escoger del maestro y crear una campana nueva. */
function epModoNueva(nueva) {
	$('#ep-caja-nombre').toggle(nueva);
	$('#ep-campana').closest('.ep-campo').toggle(!nueva);
	if (nueva) {
		$('#ep-campana').val('');
		$('#ep-nombre').focus();
	} else {
		$('#ep-nombre').val('');
	}
}

/*
 * Al escoger una campana, vuelve puesto lo de la vez pasada.
 *
 * Es lo que hace que reusar sea un clic y no volver a armarlo todo. Los filtros
 * NO se restauran: quedaron guardados como texto para poder leerlos, pero
 * reconstruirlos a medias seria peor que no hacerlo, porque nadie sabria cuales
 * quedaron puestos de verdad.
 */
function epTraerCampana() {
	var id = $('#ep-campana').val();
	if (!id) { return; }
	var c = null;
	for (var i = 0; i < epCampanas.length; i++) {
		if (String(epCampanas[i].idcampana) === String(id)) { c = epCampanas[i]; break; }
	}
	if (!c) { return; }

	if (c.canal) {
		epCanal = c.canal;
		$('#ep-canales .ep-canal').removeClass('sel');
		$('#ep-canales .ep-canal[data-canal="' + c.canal + '"]').addClass('sel');
		epPintarCanal();
	}
	if (c.idplantilla) { $('#ep-plantilla').val(c.idplantilla); }
	if (c.asunto) { $('#ep-asunto').val(c.asunto); }
	if (c.cuerpo) { $('#ep-cuerpo').val(c.cuerpo); }
	if (c.tope) { $('#ep-tope').val(c.tope); }
	if (c.dias_sin_publicidad) { $('#ep-diassinpublicidad').val(c.dias_sin_publicidad); }

	var nota = 'Es la tanda n&uacute;mero ' + (Number(c.envios) + 1) + ' de esta campa&ntilde;a.';
	if (c.filtros) {
		nota += ' La vez pasada se mand&oacute; con: <i>' + epEscapar(c.filtros) + '</i>.';
	}
	$('#ep-campana-nota').html(nota + ' <a href="#" id="ep-nuevacampana2">Crear otra campa&ntilde;a</a>.');
	$('#ep-nuevacampana2').click(function (e) { e.preventDefault(); epModoNueva(true); });

	//El publico cambia con el canal, asi que la cuenta anterior ya no vale.
	epLimpiarAlcance();
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
	var alcanzables = (epCanal === 'W') ? d.con_celular : d.con_correo;
	var topeDirecto = d.tope_directo || 50;
	var alcanza = alcanzables;
	if (epCanal === 'D' && alcanza > topeDirecto) { alcanza = topeDirecto; }

	//El tope de la tanda: cuantos salen HOY de todos los que califican.
	var tope = parseInt($('#ep-tope').val(), 10);
	if (!(tope > 0)) { tope = 0; }
	if (epCanal === 'D' && (tope === 0 || tope > topeDirecto)) { tope = topeDirecto; }
	var quedan = 0;
	if (tope > 0 && alcanza > tope) {
		quedan = alcanza - tope;
		alcanza = tope;
	}

	$('#ep-det-c').html(epMiles(d.con_correo) + ' con correo');
	$('#ep-det-w').html(epMiles(d.con_celular) + ' con celular');
	$('#ep-det-d').html('m&aacute;ximo ' + topeDirecto + ', uno cada ' + (d.segundos_directo || 30) + ' segundos');

	var pie = 'El filtro encontr&oacute; ' + epMiles(d.personas) + ' personas en total.';
	if (quedan > 0) {
		//Se dice cuantos quedan para la proxima porque es justo lo que hace que
		//partir el envio en tandas se entienda: no es que se pierdan, es que
		//les toca despues.
		pie += '<br><b>' + epMiles(quedan) + '</b> quedan para las pr&oacute;ximas tandas.';
	}

	$('#ep-cifras').html(
		'<div class="ep-cifra"><div class="valor">' + epMiles(alcanza) + '</div>' +
		'<div class="rotulo">personas por ' + epNombreCanal(epCanal) + '</div></div>' +
		'<p class="ep-nota" style="text-align:center;margin:0;">' + pie + '</p>');

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
		accion: 'probar', canalenvio: epCanal, destino: destino,
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

	var idCampana = $('#ep-campana').val();
	var nombre = $('#ep-nombre').val();
	if (!idCampana && !nombre) {
		epMensaje('ep-aviso-mal',
			'Escoja una campa&ntilde;a o p&oacute;ngale nombre a la nueva.');
		return;
	}
	var comoSeLlama = nombre || $('#ep-campana option:selected').text();

	var alcanza = (epCanal === 'W') ? epAlcance.con_celular : epAlcance.con_correo;
	var topeDirecto = epAlcance.tope_directo || 50;
	if (epCanal === 'D' && alcanza > topeDirecto) { alcanza = topeDirecto; }

	var tope = parseInt($('#ep-tope').val(), 10);
	if (!(tope > 0)) { tope = 0; }
	if (epCanal === 'D' && (tope === 0 || tope > topeDirecto)) { tope = topeDirecto; }
	if (tope > 0 && alcanza > tope) { alcanza = tope; }

	//Confirmacion con el numero escrito: que nadie pueda decir que no sabia a
	//cuanta gente le estaba escribiendo.
	if (!confirm('Va a enviar "' + comoSeLlama + '" por ' + epNombreCanal(epCanal) +
			' a ' + epMiles(alcanza) + ' personas.\n\nEsto no se puede deshacer. Continuar?')) {
		return;
	}

	var datos = epFiltro();
	datos.accion = 'enviar';
	datos.idcampana = idCampana || 0;
	datos.nombre = nombre || '';
	datos.canalenvio = epCanal;
	//Cuantos dijo la pantalla. El servidor NO envia si al cargar le salen mas:
	//es la red contra que un filtro se pierda por el camino y el envio se vaya
	//a mucha mas gente de la que uno aprobo.
	datos.esperados = alcanza;
	datos.tope = tope;
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
		epEnvio = d.idenvio;
		epMensaje('ep-aviso-ok', epEscapar(d.mensaje));
		$('#ep-avance').show();
		$('#ep-detener').toggle(epCanal === 'D');
		epSeguirAvance();
		epCargarHistorial();
		//La campana recien creada tiene que aparecer en el desplegable para
		//poder mandarle la segunda tanda manana sin volver a escribir el nombre.
		epCargarCampanas(d.idcampana);
	}, 'json').fail(function () {
		epMensaje('ep-aviso-mal', 'No se pudo lanzar la campa&ntilde;a.');
		$('#ep-enviar').prop('disabled', false);
		epPintarAlcance();
	});
}

function epSeguirAvance() {
	if (epReloj) { clearInterval(epReloj); }
	epReloj = setInterval(function () {
		$.getJSON(server + 'EnvioPublicidad', { accion: 'avance', idenvio: epEnvio },
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
			cuerpo.html('<tr><td colspan="9" class="ep-nota">Todav&iacute;a no hay env&iacute;os.</td></tr>');
			return;
		}
		for (var i = 0; i < lista.length; i++) {
			var c = lista[i];
			var fila = $('<tr>').css('cursor', 'pointer');
			fila.append($('<td>').text(c.idenvio));

			//El nombre de la campana lleva a la medicion de TODAS sus tandas;
			//el resto del renglon, solo a la de esta.
			var enlace = $('<a href="#">').text(c.nombre).data('idc', c.idcampana);
			enlace.click(function (e) {
				e.preventDefault();
				e.stopPropagation();
				epVerResultado(0, $(this).data('idc'));
			});
			fila.append($('<td>').append(enlace));

			fila.append($('<td class="ep-num">').text(c.consecutivo));
			fila.append($('<td>').text(epNombreCanal(c.canal)));
			fila.append($('<td>').text(c.estado));
			fila.append($('<td class="ep-num">').text(epMiles(c.publico)));
			fila.append($('<td class="ep-num">').text(epMiles(c.enviados)));
			fila.append($('<td class="ep-num">').text(epMiles(c.fallidos)));
			fila.append($('<td>').text(String(c.creada_en || '').substring(0, 16)));
			fila.data('id', c.idenvio);
			fila.click(function () { epVerResultado($(this).data('id'), 0); });
			cuerpo.append(fila);
		}
	});
}

/* Con idEnvio mide una tanda; con idCampana, la campana completa. */
function epVerResultado(idEnvio, idCampana) {
	$('#ep-resultado').html('<p class="ep-nota">Midiendo...</p>');
	var titulo = idEnvio
		? ('Env&iacute;o ' + idEnvio)
		: ('Campa&ntilde;a completa, todas sus tandas');
	$.getJSON(server + 'EnvioPublicidad',
		{ accion: 'resultado', idenvio: idEnvio || 0, idcampana: idCampana || 0, horas: 24 },
		function (d) {
			if (d.error) { $('#ep-resultado').html(''); return; }
			$('#ep-resultado').html(
				'<div class="ep-panel" style="margin-top:14px;">' +
				'<div class="ep-titulo">' + titulo + ', primeras ' + d.horas + ' horas</div>' +
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

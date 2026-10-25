/**
 * Marcas de cliente (pantalla de administradores).
 *
 * Todo lo que valida este archivo es comodidad: el servidor (CRUDClienteMarca) vuelve a
 * validar todo, incluido que quien llama sea administrador. Los textos con tildes van como
 * escapes unicode para no depender de la codificacion con la que se guarde el archivo.
 */

var mcTipos = [];          // todos los habilitadores (activos o no)
var mcFichaActual = null;  // lo que se esta editando en la pestana "Marcar un cliente"

$(function () {
	cargarTipos(function () { /* nada: la lista ya se pinto */ });

	$('#btnBuscar').on('click', buscar);
	$('#telefono').on('keypress', function (e) { if (e.which === 13) { buscar(); } });
	//Cambiar el telefono invalida lo cargado: si no, se podria guardar en un telefono lo que se cargo de otro.
	$('#telefono').on('input', function () { mcFichaActual = null; $('#ficha').hide(); });
	$('#texto').on('input', function () { $('#cuenta').text($('#texto').val().length); });
	$('#btnGuardar').on('click', guardar);
	$('#btnQuitar').on('click', quitar);
	$('#btnHistorial').on('click', verHistorial);
	$('#btnListar').on('click', listar);
	$('#filtro').on('keypress', function (e) { if (e.which === 13) { listar(); } });
	$('#tab-lista').on('shown.bs.tab', listar);

	$('#btnNuevoTipo').on('click', function () { abrirFormTipo(null); });
	$('#btnCancelarTipo').on('click', function () { $('#formTipo').hide(); });
	$('#btnGuardarTipo').on('click', guardarTipo);
});

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

function esc(t) {
	return String(t === null || t === undefined ? '' : t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function avisar(texto, tipo) {
	var clase = tipo === 'ok' ? 'aviso-ok' : (tipo === 'ojo' ? 'aviso-ojo' : 'aviso-mal');
	$('#aviso').attr('class', 'aviso ' + clase).text(texto).show();
	$('html, body').animate({ scrollTop: 0 }, 150);
	if (tipo === 'ok') { setTimeout(function () { $('#aviso').fadeOut(); }, 6000); }
}

function soloDigitos(t) { return String(t || '').replace(/[^0-9]/g, ''); }

/** Llama al servicio. Las llamadas que cambian datos van por POST (el servidor lo exige). */
function llamar(operacion, datos, esPost, siOk) {
	$.ajax({
		url: 'CRUDClienteMarca',
		type: esPost ? 'POST' : 'GET',
		dataType: 'json',
		data: $.extend({ idoperacion: operacion }, datos || {}),
		cache: false,
		success: function (r) {
			if (!r) { avisar('El servidor no respondió.', 'mal'); return; }
			if (r.respuesta === 'NOSESION') { location.href = 'Index.html'; return; }
			if (r.respuesta === 'SINPERMISO') {
				$('#bloqueSinPermiso').show();
				$('#pestanas, .tab-content').hide();
				return;
			}
			if (r.respuesta !== 'OK') { avisar(r.detalle || 'No se pudo completar la operación.', 'mal'); return; }
			siOk(r);
		},
		error: function (xhr) { avisar('No se pudo comunicar con el servidor (HTTP ' + xhr.status + ').', 'mal'); }
	});
}

function tipoPorId(id) {
	for (var i = 0; i < mcTipos.length; i++) { if (mcTipos[i].id === id) { return mcTipos[i]; } }
	return null;
}

function claseGravedad(sev) { return sev === 'N' ? 'chip-neg' : (sev === 'P' ? 'chip-pos' : 'chip-inf'); }
function nombreGravedad(sev) { return sev === 'N' ? 'Negativa' : (sev === 'P' ? 'Positiva' : 'Informativa'); }

/** Las etiquetas de lo que hace un habilitador (sin su nombre). */
function chipsEfecto(t) {
	var h = '';
	if (t.bloquea_cargo === 'S') { h += '<span class="chip chip-cargo">bloquea Rappi Cargo</span>'; }
	if (t.activo === 'N') { h += '<span class="chip chip-off">inactivo</span>'; }
	return h;
}

/** La etiqueta de color de un habilitador, con su nombre. */
function chip(t) {
	return '<span class="chip ' + claseGravedad(t.severidad) + '">' + esc(t.nombre) + '</span>' + chipsEfecto(t);
}

// ---------------------------------------------------------------------------
// Habilitadores (catalogo)
// ---------------------------------------------------------------------------

function cargarTipos(despues) {
	llamar(5, {}, false, function (r) {
		mcTipos = r.tipos;
		pintarTipos();
		var opciones = '<option value="">(todos)</option>';
		for (var i = 0; i < mcTipos.length; i++) { opciones += '<option value="' + esc(mcTipos[i].id) + '">' + esc(mcTipos[i].nombre) + '</option>'; }
		$('#filtroTipo').html(opciones);
		despues();
	});
}

function pintarTipos() {
	var html = '';
	for (var i = 0; i < mcTipos.length; i++) {
		var t = mcTipos[i];
		html += '<tr><td>' + t.orden + '</td><td><b>' + esc(t.nombre) + '</b></td><td>' + esc(t.descripcion) + '</td>'
			+ '<td><span class="chip ' + claseGravedad(t.severidad) + '" style="margin-left:0">' + nombreGravedad(t.severidad) + '</span></td>'
			+ '<td>' + (t.bloquea_cargo === 'S' ? '<span class="chip chip-cargo" style="margin-left:0">Bloquea Rappi Cargo</span>' : 'Solo avisa') + '</td>'
			+ '<td>' + (t.activo === 'S' ? 'Activo' : '<span class="chip chip-off" style="margin-left:0">Inactivo</span>') + '</td>'
			+ '<td><button class="btn btn-sm btn-outline-primary" data-editar="' + esc(t.id) + '">Editar</button></td></tr>';
	}
	$('#tablaTipos tbody').html(html);
	$('#tablaTipos [data-editar]').on('click', function () { abrirFormTipo(tipoPorId($(this).attr('data-editar'))); });
}

function abrirFormTipo(t) {
	$('#tituloTipo').text(t ? 'Editar habilitador' : 'Nuevo habilitador');
	$('#tipoId').val(t ? t.id : '');
	$('#tipoNombre').val(t ? t.nombre : '');
	$('#tipoDescripcion').val(t ? t.descripcion : '');
	$('#tipoSeveridad').val(t ? t.severidad : 'I');
	$('#tipoOrden').val(t ? t.orden : (mcTipos.length + 1) * 10);
	$('#tipoBloquea').prop('checked', t ? t.bloquea_cargo === 'S' : false);
	$('#tipoActivo').prop('checked', t ? t.activo === 'S' : true);
	$('#formTipo').show();
	$('#tipoNombre').focus();
}

function guardarTipo() {
	var nombre = $.trim($('#tipoNombre').val());
	if (nombre.length < 3) { avisar('El nombre del habilitador debe tener al menos 3 letras.', 'mal'); return; }
	llamar(6, {
		idtipo: $('#tipoId').val(),
		nombre: nombre,
		descripcion: $.trim($('#tipoDescripcion').val()),
		severidad: $('#tipoSeveridad').val(),
		bloquea_cargo: $('#tipoBloquea').is(':checked') ? 'S' : 'N',
		activo: $('#tipoActivo').is(':checked') ? 'S' : 'N',
		orden: $('#tipoOrden').val()
	}, true, function () {
		avisar('Habilitador guardado.', 'ok');
		$('#formTipo').hide();
		cargarTipos(function () {
			//Si hay un cliente abierto, se repinta para que vea el cambio.
			if (mcFichaActual) { buscar(); }
		});
	});
}

// ---------------------------------------------------------------------------
// Marcar un cliente
// ---------------------------------------------------------------------------

function buscar() {
	var tel = soloDigitos($('#telefono').val());
	if (tel.length < 7) { avisar('Escriba un teléfono válido (mínimo 7 dígitos).', 'mal'); return; }
	$('#telefono').val(tel);
	$('#btnBuscar').prop('disabled', true);
	llamar(1, { telefono: tel }, false, function (r) {
		$('#btnBuscar').prop('disabled', false);
		mcFichaActual = r;
		pintarFicha(r);
	});
	//Si la llamada falla, el boton se vuelve a habilitar con el siguiente intento.
	setTimeout(function () { $('#btnBuscar').prop('disabled', false); }, 4000);
}

function pintarFicha(r) {
	var personas = '';
	for (var i = 0; i < r.personas.length; i++) {
		personas += '<div class="persona"><b>' + esc(r.personas[i].nombre || '(sin nombre)') + '</b> <small>&middot; ' + esc(r.personas[i].direccion) + '</small></div>';
	}
	if (personas === '') {
		personas = '<div class="persona"><small>No hay un cliente con ese teléfono en el sistema. Igual se puede marcar: la marca queda guardada para cuando pida.</small></div>';
	}
	$('#personas').html(personas);

	var marcados = {};
	for (var m = 0; m < r.marcas.length; m++) { marcados[r.marcas[m].id] = true; }

	var html = '';
	for (var k = 0; k < mcTipos.length; k++) {
		var t = mcTipos[k];
		//Un habilitador inactivo solo aparece si el cliente ya lo tiene (para poder quitarselo).
		if (t.activo === 'N' && !marcados[t.id]) { continue; }
		var prendido = !!marcados[t.id];
		html += '<label class="hab' + (prendido ? ' prendido' : '') + '"><input type="checkbox" data-tipo="' + esc(t.id) + '"' + (prendido ? ' checked' : '') + '>'
			+ '<span class="nom">' + esc(t.nombre) + '</span>' + chipsEfecto(t)
			+ '<div class="des">' + esc(t.descripcion) + '</div></label>';
	}
	if (html === '') { html = '<div class="persona"><small>No hay habilitadores activos. Cree uno en la pestaña Habilitadores.</small></div>'; }
	$('#habilitadores').html(html);
	$('#habilitadores input').on('change', function () { $(this).closest('.hab').toggleClass('prendido', $(this).is(':checked')); });

	$('#texto').val(r.texto);
	$('#cuenta').text(r.texto.length);
	$('#quienToco').text(r.existe && r.fecha_modifica ? 'Último cambio: ' + r.fecha_modifica + (r.usuario_modifica ? ' por ' + r.usuario_modifica : '') + '.' : (r.existe ? '' : 'Este teléfono no tiene marcas todavía.'));
	$('#historial').hide().empty();
	$('#ficha').show();
}

function guardar() {
	if (!mcFichaActual) { return; }
	var ids = [];
	$('#habilitadores input:checked').each(function () { ids.push($(this).attr('data-tipo')); });
	var texto = $.trim($('#texto').val());
	if (ids.length === 0 && texto === '') {
		avisar('No hay nada que guardar: prenda un habilitador o escriba una nota. Para borrar las marcas use «Quitar todas las marcas».', 'ojo');
		return;
	}
	$('#btnGuardar').prop('disabled', true);
	llamar(2, { telefono: mcFichaActual.telefono, texto: texto, tipos: ids.join(',') }, true, function () {
		$('#btnGuardar').prop('disabled', false);
		avisar('Guardado. Desde ahora el cliente se ve marcado al tomar un pedido.', 'ok');
		buscar();
	});
	setTimeout(function () { $('#btnGuardar').prop('disabled', false); }, 4000);
}

function quitar() {
	if (!mcFichaActual) { return; }
	var tel = mcFichaActual.telefono;
	$.confirm({
		title: '¿Quitar todas las marcas?',
		content: 'Se borran la nota y todos los habilitadores del teléfono <b>' + esc(tel) + '</b>. Queda anotado en el historial.',
		type: 'red',
		buttons: {
			'Quitar': { btnClass: 'btn-red', action: function () {
				llamar(3, { telefono: tel }, true, function (r) {
					avisar(r.habia ? 'Se quitaron las marcas del cliente.' : 'Ese teléfono no tenía marcas.', 'ok');
					buscar();
				});
			} },
			'Cancelar': function () { }
		}
	});
}

function verHistorial() {
	if (!mcFichaActual) { return; }
	llamar(7, { telefono: mcFichaActual.telefono }, false, function (r) {
		if (r.eventos.length === 0) { $('#historial').html('<div class="sub-mc">Este teléfono no tiene historial.</div>').show(); return; }
		var h = '<div class="table-responsive"><table class="table table-sm tabla-mc"><thead><tr><th>Cuándo</th><th>Quién</th><th>Qué</th><th>Detalle</th></tr></thead><tbody>';
		for (var i = 0; i < r.eventos.length; i++) {
			var e = r.eventos[i];
			h += '<tr><td>' + esc(e.fecha) + '</td><td>' + esc(e.usuario) + '</td><td>' + esc(e.accion) + '</td><td class="texto-corto">' + esc(e.detalle) + '</td></tr>';
		}
		$('#historial').html(h + '</tbody></table></div>').show();
	});
}

// ---------------------------------------------------------------------------
// Clientes marcados
// ---------------------------------------------------------------------------

function listar() {
	llamar(4, { filtro: $.trim($('#filtro').val()), idtipo: $('#filtroTipo').val() }, false, function (r) {
		var html = '';
		for (var i = 0; i < r.clientes.length; i++) {
			var c = r.clientes[i];
			var chips = '';
			var ids = c.marcas ? c.marcas.split(',') : [];
			for (var j = 0; j < ids.length; j++) { var t = tipoPorId(ids[j]); if (t) { chips += chip(t); } }
			var nota = c.texto.length > 180 ? c.texto.substring(0, 180) + '...' : c.texto;
			html += '<tr><td><b>' + esc(c.telefono) + '</b></td><td>' + (chips || '<small>(solo nota)</small>') + '</td>'
				+ '<td class="texto-corto">' + esc(nota) + '</td><td><small>' + esc(c.fecha) + (c.usuario ? '<br>' + esc(c.usuario) : '') + '</small></td>'
				+ '<td><button class="btn btn-sm btn-outline-primary" data-abrir="' + esc(c.telefono) + '">Abrir</button></td></tr>';
		}
		$('#tablaClientes tbody').html(html);
		$('#vacioLista').toggle(r.clientes.length === 0);
		$('#tablaClientes [data-abrir]').on('click', function () {
			$('#telefono').val($(this).attr('data-abrir'));
			$('#tab-marcar').tab('show');
			buscar();
		});
	});
}

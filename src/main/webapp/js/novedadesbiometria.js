/** novedadesbiometria.js
 *
 * Revision de las novedades de biometria que reportan las tiendas.
 *
 * El flujo es en dos pasos a proposito. La tienda solo REPORTA: la pantalla del
 * POS es abierta y cualquiera puede decir que olvido marcar, asi que eso no
 * cambia nada por si solo. Aqui el supervisor revisa, y al abrir una novedad se
 * carga la jornada COMPLETA del dia de ese empleado, no solo el registro que la
 * novedad senala: para saber si falta una salida o si una hora esta mal hay que
 * ver el dia entero.
 *
 * Cada accion se aplica por separado -una llamada por cambio- porque cada una es
 * atomica y deja su propia fila en el log. Si una falla, las anteriores ya
 * quedaron y se sabe exactamente donde se quedo.
 */

var dtNovedades;
var ultimoDetalle = [];
var novedadActual = null;
var jornadaActual = [];

$(function () {
	ponerFechasPorDefecto();
	$('#usuarioSesion').val(nombreusuario ? nombreusuario : usuario);
	$('#btnConsultar').on('click', consultar);
	$('#btnAgregar').on('click', agregarEvento);
	$('#btnAtendida').on('click', function () { cerrarNovedad('ATENDIDA'); });
	$('#btnRechazar').on('click', function () { cerrarNovedad('RECHAZADA'); });
	$('#btnCerrarCaja').on('click', function () { $('#cajaRevision').hide(); novedadActual = null; });
	consultar();
});

/** Por defecto los ultimos siete dias: las novedades se reportan al dia siguiente. */
function ponerFechasPorDefecto() {
	var hoy = new Date();
	var hace = new Date();
	hace.setDate(hoy.getDate() - 7);
	$('#fechaHasta').val(aTexto(hoy));
	$('#fechaDesde').val(aTexto(hace));
}

function aTexto(fecha) {
	var mes = ('0' + (fecha.getMonth() + 1)).slice(-2);
	var dia = ('0' + fecha.getDate()).slice(-2);
	return (fecha.getFullYear() + '-' + mes + '-' + dia);
}

function consultar() {
	var desde = $('#fechaDesde').val();
	var hasta = $('#fechaHasta').val();
	if (!desde || !hasta) {
		$.alert('Indique la fecha desde y la fecha hasta.');
		return;
	}
	$('#cajaRevision').hide();
	novedadActual = null;
	$.ajax({
		url: server + 'ConsultarNovedadesBiometria',
		data: { fechadesde: desde, fechahasta: hasta, estado: $('#estado').val() },
		dataType: 'json',
		type: 'get',
		success: function (data) {
			if (data.resultado !== 'OK') {
				$.alert(data.mensaje);
				return;
			}
			$('#totNovedades').text(data.resumen.total);
			$('#totReportadas').text(data.resumen.reportadas);
			$('#totAtendidas').text(data.resumen.atendidas);
			$('#totRechazadas').text(data.resumen.rechazadas);
			ultimoDetalle = data.detalle;
			pintarNovedades(data.detalle);
		},
		error: function () {
			$.alert('No se pudo consultar las novedades.');
		}
	});
}

function pintarNovedades(detalle) {
	if (dtNovedades) {
		dtNovedades.destroy();
		dtNovedades = null;
	}
	var cuerpo = $('#grid-novedades tbody');
	cuerpo.empty();
	$('#sinNovedades').toggle(detalle.length === 0);
	$('#grid-novedades').toggle(detalle.length > 0);
	for (var i = 0; i < detalle.length; i++) {
		var n = detalle[i];
		var registro = n.eventotipo ? (n.eventotipo + ' ' + soloHora(n.eventohora)) : '—';
		var horaReal = n.horareportada ? soloHora(n.horareportada) : '—';
		var boton = '<button class="btn btn-primary btn-xs" onclick="revisar(' + n.idnovedad + ')">Revisar</button>';
		cuerpo.append('<tr>'
			+ '<td>' + n.idnovedad + '</td>'
			+ '<td>' + escapar(n.id + ' - ' + n.nombre) + '</td>'
			+ '<td>' + escapar(n.fecha) + '</td>'
			+ '<td>' + escapar(n.tiponovedad) + '</td>'
			+ '<td>' + escapar(registro) + '</td>'
			+ '<td>' + escapar(horaReal) + '</td>'
			+ '<td>' + escapar(n.observacion) + '</td>'
			+ '<td>' + escapar(n.reportadopor) + '</td>'
			+ '<td>' + etiquetaEstado(n.estado) + '</td>'
			+ '<td>' + n.cambios + '</td>'
			+ '<td>' + boton + '</td>'
			+ '</tr>');
	}
	if (detalle.length > 0) {
		dtNovedades = $('#grid-novedades').DataTable({
			paging: true, pageLength: 25, searching: true, ordering: true, order: []
		});
	}
}

function etiquetaEstado(estado) {
	var clase = 'est-reportada';
	if (estado === 'ATENDIDA') { clase = 'est-atendida'; }
	if (estado === 'RECHAZADA') { clase = 'est-rechazada'; }
	return ('<span class="etiqueta-estado ' + clase + '">' + escapar(estado) + '</span>');
}

/** Abre la revision de una novedad y carga la jornada completa de ese dia. */
function revisar(idNovedad) {
	novedadActual = null;
	for (var i = 0; i < ultimoDetalle.length; i++) {
		if (ultimoDetalle[i].idnovedad === idNovedad) {
			novedadActual = ultimoDetalle[i];
			break;
		}
	}
	if (!novedadActual) {
		$.alert('No se encontró la novedad. Vuelva a consultar.');
		return;
	}
	$('#tituloRevision').text('Novedad ' + novedadActual.idnovedad + ' · '
		+ novedadActual.id + ' - ' + novedadActual.nombre + ' · ' + novedadActual.fecha);
	var texto = '<strong>' + escapar(novedadActual.tiponovedad) + '</strong><br>'
		+ escapar(novedadActual.observacion) + '<br>'
		+ '<span style="color:#666;">Reportó ' + escapar(novedadActual.reportadopor)
		+ ' el ' + escapar(novedadActual.fechareporte);
	if (novedadActual.eventotipo) {
		texto += ' · señaló el registro ' + escapar(novedadActual.eventotipo) + ' de las '
			+ escapar(soloHora(novedadActual.eventohora));
	}
	if (novedadActual.horareportada) {
		texto += ' · dice que la hora real fue ' + escapar(soloHora(novedadActual.horareportada));
	}
	texto += '</span>';
	if (novedadActual.estado !== 'REPORTADA') {
		texto += '<br><span style="color:#E42528;font-weight:700;">Esta novedad ya fue '
			+ escapar(novedadActual.estado) + ' por ' + escapar(novedadActual.resueltopor) + '.</span>';
	}
	$('#bloqueNovedad').html(texto);
	$('#nuevaHora').val('');
	$('#observacion').val('');
	$('#cajaRevision').show();
	cargarJornada();
}

function cargarJornada() {
	$.ajax({
		url: server + 'ConsultarEventosBiometria',
		data: { id: novedadActual.id, fecha: novedadActual.fecha },
		dataType: 'json',
		type: 'get',
		success: function (data) {
			if (data.resultado !== 'OK') {
				$.alert(data.mensaje);
				return;
			}
			jornadaActual = data.detalle;
			pintarJornada(data.detalle);
		},
		error: function () {
			$.alert('No se pudo cargar la jornada del empleado.');
		}
	});
}

function pintarJornada(detalle) {
	var cuerpo = $('#grid-jornada tbody');
	cuerpo.empty();
	$('#sinJornada').toggle(detalle.length === 0);
	$('#grid-jornada').toggle(detalle.length > 0);
	for (var i = 0; i < detalle.length; i++) {
		var e = detalle[i];
		//La marca 'N' quiere decir que esa hora la puso una persona y no el huellero.
		var huellero = (e.usobiometria === 'S')
			? 'Sí'
			: '<span class="aviso-manual">Manual</span>';
		cuerpo.append('<tr>'
			+ '<td><strong>' + escapar(e.tipo) + '</strong></td>'
			+ '<td><input type="time" step="1" class="form-control hora-editable" id="hora' + i
				+ '" value="' + escapar(soloHora(e.fechahora)) + '" /></td>'
			+ '<td>' + huellero + '</td>'
			+ '<td>'
			+ '<button class="btn btn-primary btn-xs" onclick="guardarHora(' + i + ')">Guardar hora</button> '
			+ '<button class="btn btn-danger btn-xs" onclick="eliminarEvento(' + i + ')">Eliminar</button>'
			+ '</td></tr>');
	}
}

/** Valida lo que exige el servicio antes de gastar una llamada. */
function comunesValidos() {
	if (!novedadActual) {
		$.alert('Abra primero una novedad.');
		return (false);
	}
	if ($('#observacion').val().trim().length < 10) {
		$.alert('La observación es obligatoria y debe tener al menos 10 caracteres. '
			+ 'Queda en el log como la razón del cambio.');
		return (false);
	}
	return (true);
}

function guardarHora(indice) {
	if (!comunesValidos()) { return; }
	var evento = jornadaActual[indice];
	var horaNueva = $('#hora' + indice).val();
	if (!horaNueva) {
		$.alert('Indique la hora.');
		return;
	}
	if (completar(horaNueva) === soloHora(evento.fechahora)) {
		$.alert('La hora es la misma que ya estaba. No hay nada que cambiar.');
		return;
	}
	enviarCambio({
		accion: 'MODIFICA',
		antestipo: evento.tipo,
		anteshora: evento.fechahora,
		despuestipo: evento.tipo,
		despueshora: novedadActual.fecha + ' ' + completar(horaNueva)
	}, 'Hora corregida.');
}

function eliminarEvento(indice) {
	if (!comunesValidos()) { return; }
	var evento = jornadaActual[indice];
	$.confirm({
		title: 'Eliminar el registro',
		content: 'Se va a eliminar el ' + evento.tipo + ' de las ' + soloHora(evento.fechahora)
			+ '. Queda en el log, pero desde esta pantalla no se puede deshacer.',
		buttons: {
			eliminar: {
				btnClass: 'btn-danger',
				action: function () {
					enviarCambio({
						accion: 'ELIMINA',
						antestipo: evento.tipo,
						anteshora: evento.fechahora,
						despuestipo: '',
						despueshora: ''
					}, 'Registro eliminado.');
				}
			},
			cancelar: function () { }
		}
	});
}

function agregarEvento() {
	if (!comunesValidos()) { return; }
	var hora = $('#nuevaHora').val();
	if (!hora) {
		$.alert('Indique la hora del registro que falta.');
		return;
	}
	enviarCambio({
		accion: 'AGREGA',
		antestipo: '',
		anteshora: '',
		despuestipo: $('#nuevoTipo').val(),
		despueshora: novedadActual.fecha + ' ' + completar(hora)
	}, 'Registro agregado.');
}

function enviarCambio(datos, mensajeOk) {
	datos.idnovedad = novedadActual.idnovedad;
	datos.id = novedadActual.id;
	datos.idtienda = novedadActual.idtienda;
	datos.fecha = novedadActual.fecha;
	datos.usuario = $('#usuarioSesion').val();
	datos.observacion = $('#observacion').val().trim();
	$.ajax({
		url: server + 'GuardarCambioBiometria',
		data: datos,
		dataType: 'json',
		type: 'post',
		success: function (data) {
			if (data.resultado !== 'OK') {
				$.alert(data.mensaje);
				return;
			}
			$.alert(mensajeOk + ' ' + data.mensaje);
			$('#nuevaHora').val('');
			cargarJornada();
			consultarSilencioso();
		},
		error: function () {
			$.alert('No se pudo aplicar el cambio.');
		}
	});
}

function cerrarNovedad(estado) {
	if (!comunesValidos()) { return; }
	var titulo = (estado === 'ATENDIDA') ? 'Marcar como atendida' : 'Rechazar la novedad';
	var texto = (estado === 'ATENDIDA')
		? 'La novedad queda cerrada. Se puede cerrar sin haber hecho cambios, si al revisar la jornada ya estaba bien.'
		: 'La novedad queda rechazada y no se aplica ningún cambio.';
	$.confirm({
		title: titulo,
		content: texto,
		buttons: {
			confirmar: {
				btnClass: (estado === 'ATENDIDA') ? 'btn-primary' : 'btn-danger',
				action: function () {
					$.ajax({
						url: server + 'CerrarNovedadBiometria',
						data: {
							idnovedad: novedadActual.idnovedad,
							estado: estado,
							usuario: $('#usuarioSesion').val(),
							observacion: $('#observacion').val().trim()
						},
						dataType: 'json',
						type: 'post',
						success: function (data) {
							if (data.resultado !== 'OK') {
								$.alert(data.mensaje);
								return;
							}
							$.alert(data.mensaje);
							$('#cajaRevision').hide();
							novedadActual = null;
							consultar();
						},
						error: function () {
							$.alert('No se pudo cerrar la novedad.');
						}
					});
				}
			},
			cancelar: function () { }
		}
	});
}

/** Refresca los contadores sin cerrar la caja de revision que esta abierta. */
function consultarSilencioso() {
	$.ajax({
		url: server + 'ConsultarNovedadesBiometria',
		data: {
			fechadesde: $('#fechaDesde').val(),
			fechahasta: $('#fechaHasta').val(),
			estado: $('#estado').val()
		},
		dataType: 'json',
		type: 'get',
		success: function (data) {
			if (data.resultado !== 'OK') { return; }
			$('#totNovedades').text(data.resumen.total);
			$('#totReportadas').text(data.resumen.reportadas);
			$('#totAtendidas').text(data.resumen.atendidas);
			$('#totRechazadas').text(data.resumen.rechazadas);
			ultimoDetalle = data.detalle;
		}
	});
}

/** De 'aaaa-mm-dd hh:mm:ss' saca solo 'hh:mm:ss'. */
function soloHora(fechaHora) {
	if (!fechaHora) { return (''); }
	var texto = String(fechaHora);
	if (texto.length >= 19) { return (texto.substring(11, 19)); }
	if (texto.length >= 16) { return (texto.substring(11, 16) + ':00'); }
	return (texto);
}

/** El input de hora puede devolver hh:mm sin segundos. */
function completar(hora) {
	if (!hora) { return (''); }
	if (hora.length === 5) { return (hora + ':00'); }
	return (hora);
}

/** Los textos vienen de lo que escribio una persona en una tienda. */
function escapar(texto) {
	if (texto === null || texto === undefined) { return (''); }
	return (String(texto)
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&#39;'));
}

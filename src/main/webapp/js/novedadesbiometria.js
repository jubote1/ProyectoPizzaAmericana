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

/* La franja de madrugada donde se confunde la tarde con la manana. Fuera de
   ella no hay ambiguedad: nadie escribe 23:10 queriendo decir las once de la
   manana. Los mismos valores estan en el POS, en VentSegNovedadBiometria. */
var MADRUGADA_DESDE = 1;
var MADRUGADA_HASTA = 6;
/* Horas de distancia a la referencia desde las cuales la hora se ve rara. */
var BRECHA_SOSPECHOSA = 10;
/* Horas de distancia por debajo de las cuales la hora se ve bien. */
var BRECHA_PLAUSIBLE = 6;

$(function () {
	ponerFechasPorDefecto();
	$('#usuarioSesion').val(nombreusuario ? nombreusuario : usuario);
	$('#btnConsultar').on('click', consultar);
	$('#btnAgregar').on('click', agregarEvento);
	$('#btnAtendida').on('click', function () { cerrarNovedad('ATENDIDA'); });
	$('#btnRechazar').on('click', function () { cerrarNovedad('RECHAZADA'); });
	//Al cerrar hay que devolver la pantalla a la tabla: si no, la caja desaparece
	//y uno queda abajo del todo mirando el vacio, sin saber que paso.
	$('#btnCerrarCaja').on('click', function () {
		$('#cajaRevision').hide();
		novedadActual = null;
		traerALaVista('#grid-novedades');
	});
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
			+ '<td>' + escapar(n.reportadopor) + marcaIdentificacion(n.reportabiometria) + '</td>'
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
		+ '<span style="color:#666;">Reportó ' + escapar(novedadActual.reportadopor) + marcaIdentificacion(novedadActual.reportabiometria)
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
	//La caja de revision NO es un modal: es un div que vive debajo de la tabla,
	//en el flujo normal de la pagina. Con doce novedades en pantalla se abre muy
	//por debajo de lo que se alcanza a ver, y desde la silla el boton Revisar
	//parece que no hiciera nada. Hay que traerla a la vista.
	traerALaVista('#cajaRevision');
	cargarJornada();
}

/**
 * Lleva la pantalla hasta un bloque que se acaba de mostrar.
 *
 * Se usa animate y no scrollTop directo para que se vea el recorrido: si la
 * pagina salta de golpe, quien hizo clic no entiende que lo movieron y busca
 * donde estaba. El desplazamiento de 20 pixeles deja el borde superior
 * despegado del filo de la ventana.
 */
function traerALaVista(selector) {
	var caja = $(selector);
	if (caja.length === 0) {
		return;
	}
	$('html, body').animate({ scrollTop: caja.offset().top - 20 }, 350);
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
	//La referencia es la marca que se esta corrigiendo: una correccion normal la
	//mueve un par de horas, no medio dia.
	confirmarSiPareceTarde(novedadActual.fecha + ' ' + completar(horaNueva), evento.fechahora, function () {
		enviarCambio({
			accion: 'MODIFICA',
			antestipo: evento.tipo,
			anteshora: evento.fechahora,
			despuestipo: evento.tipo,
			despueshora: novedadActual.fecha + ' ' + completar(horaNueva)
		}, 'Hora corregida.');
	});
}

/**
 * Pregunta cuando la hora parece escrita de madrugada queriendo decir la tarde.
 *
 * EL ERROR QUE ATRAPA
 *
 * En Colombia se habla en doce horas. Quien reporta oye "entro a las 5:54" y
 * tiene que traducirlo a 17:54 de cabeza. Casi siempre acierta, pero de las 41
 * novedades que ya existen, TRES quedaron corridas medio dia: todas decian una
 * hora de tarde en la explicacion y una de madrugada en el campo.
 *
 * LAS TRES CONDICIONES, Y POR QUE SON TRES
 *
 * 1. La hora cae en la franja de madrugada. Fuera de ahi no hay ambiguedad.
 * 2. Queda lejisimos de la hora de referencia.
 * 3. Sumarle doce la vuelve plausible. Esta es la que hace la diferencia: sin
 *    ella, una salida olvidada a las 23:10 sobre un ingreso de las 13:55 -nueve
 *    horas, perfectamente normal- tambien saltaria.
 *
 * Probado contra las 41 novedades existentes: avisa en exactamente las tres
 * malas y en ninguna de las buenas.
 *
 * No corrige sola: puede ser un turno de madrugada de verdad. Pregunta.
 */
function confirmarSiPareceTarde(horaNueva, horaReferencia, alSeguir) {
	var tarde = pareceDeLaTarde(horaNueva, horaReferencia);
	if (!tarde) {
		alSeguir();
		return;
	}
	$.confirm({
		title: 'Revise la hora',
		content: 'La hora que escribió queda a las <b>' + escapar(horaNueva.substring(0, 5))
			+ ' de la MADRUGADA</b>, y la referencia es de las '
			+ escapar(horaReferencia.substring(0, 5)) + '.<br><br>'
			+ 'Si quiso decir las <b>' + escapar(tarde.substring(0, 5))
			+ ' de la TARDE</b>, cancele y corríjala.',
		buttons: {
			siEsMadrugada: {
				text: 'Sí, fue de madrugada',
				btnClass: 'btn-warning',
				action: function () { alSeguir(); }
			},
			cancelar: {
				text: 'Cancelar y corregir',
				action: function () { }
			}
		}
	});
}

/**
 * La misma hora pero doce horas despues, cuando todo apunta a que eso fue lo
 * que se quiso decir. Null cuando la hora se ve bien como esta.
 *
 * Las dos horas llegan como 'hh:mm:ss' del mismo dia, que es como las maneja
 * esta pantalla.
 */
function pareceDeLaTarde(fechaHoraNueva, fechaHoraReferencia) {
	var nueva = minutosDe(fechaHoraNueva);
	var referencia = minutosDe(fechaHoraReferencia);
	if (nueva === null || referencia === null) {
		return (null);
	}
	//La hora se lee del texto y NO de la aritmetica: minutosDe devuelve minutos
	//absolutos de epoca, y sacarle el resto de 1440 daria la hora en UTC, que en
	//Colombia esta cinco horas corrida.
	var hora = parseInt(String(fechaHoraNueva).substring(11, 13), 10);
	if (isNaN(hora) || hora < MADRUGADA_DESDE || hora > MADRUGADA_HASTA) {
		return (null);
	}
	if (Math.abs(nueva - referencia) <= BRECHA_SOSPECHOSA * 60) {
		return (null);
	}
	var enLaTarde = nueva + 12 * 60;
	if (Math.abs(enLaTarde - referencia) >= BRECHA_PLAUSIBLE * 60) {
		return (null);
	}
	var minuto = parseInt(String(fechaHoraNueva).substring(14, 16), 10);
	return (dos(hora + 12) + ':' + dos(isNaN(minuto) ? 0 : minuto) + ':00');
}

/**
 * Minutos absolutos de un 'aaaa-mm-dd hh:mm:ss', contando el dia.
 *
 * Tiene que ser con la fecha y no solo con la hora: una SALIDA real a las 02:30
 * del dia siguiente esta a siete horas y media de un INGRESO de las 19:00, pero
 * comparando solo horas pareceria estar a dieciseis y media, y la guarda
 * saltaria sobre un turno de noche perfectamente normal.
 */
function minutosDe(fechaHora) {
	var texto = String(fechaHora || '');
	if (texto.length < 16) {
		return (null);
	}
	var fecha = texto.substring(0, 10).split('-');
	var hm = texto.substring(11, 16).split(':');
	if (fecha.length !== 3 || hm.length !== 2) {
		return (null);
	}
	var d = new Date(parseInt(fecha[0], 10), parseInt(fecha[1], 10) - 1, parseInt(fecha[2], 10),
			parseInt(hm[0], 10), parseInt(hm[1], 10), 0, 0);
	if (isNaN(d.getTime())) {
		return (null);
	}
	return (Math.round(d.getTime() / 60000));
}

function dos(n) {
	return (n < 10 ? '0' + n : '' + n);
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
	//Al AGREGAR no hay una marca que se este corrigiendo, asi que la referencia
	//es la marca mas cercana de la jornada. Si el dia no tiene ninguna -que es
	//justo cuando se agrega la primera- no hay contra que comparar y se sigue
	//sin preguntar: inventar una referencia seria peor que no tenerla.
	var cuando = novedadActual.fecha + ' ' + completar(hora);
	confirmarSiPareceTarde(cuando, marcaMasCercana(cuando), function () {
		enviarCambio({
			accion: 'AGREGA',
			antestipo: '',
			anteshora: '',
			despuestipo: $('#nuevoTipo').val(),
			despueshora: novedadActual.fecha + ' ' + completar(hora)
		}, 'Registro agregado.');
	});
}

/** La marca de la jornada mas cercana a una fecha y hora dada, o ''. */
function marcaMasCercana(fechaHora) {
	var minutos = minutosDe(fechaHora);
	if (minutos === null || !jornadaActual || jornadaActual.length === 0) {
		return ('');
	}
	var mejor = '';
	var menor = -1;
	for (var i = 0; i < jornadaActual.length; i++) {
		var otra = jornadaActual[i].fechahora;
		var m = minutosDe(otra);
		if (m === null) { continue; }
		var distancia = Math.abs(m - minutos);
		if (menor < 0 || distancia < menor) {
			menor = distancia;
			mejor = otra;
		}
	}
	return (mejor);
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
							traerALaVista('#grid-novedades');
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

/**
 * Como se identifico quien reporto: S huella, N clave rapida, vacio = novedad anterior a que el POS
 * pidiera identificarse (el nombre lo escribio la persona a mano).
 */
function marcaIdentificacion(marca) {
	if (marca === 'S') {
		return (' <span title="Se identificó con la huella" style="color:#2e7d32;font-weight:bold;">&#10004; huella</span>');
	}
	if (marca === 'N') {
		return (' <span title="Se identificó con la clave rápida, no con huella" style="color:#ef6c00;font-weight:bold;">clave</span>');
	}
	return (' <span title="Nombre escrito a mano, sin identificación" style="color:#999;">(sin identificar)</span>');
}

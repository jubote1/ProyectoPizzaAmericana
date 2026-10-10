/*
 * PEDIDOS QUE QUEDARON A MEDIO TOMAR
 *
 * POR QUE EXISTE ESTE ARCHIVO
 *
 * ConsultaPedidosEnCurso.html ya estaba en el menu -en Menu.html y en
 * MenuAdm.html- y referenciaba este archivo, pero el archivo NO existia. O sea
 * que la pantalla abria y no hacia nada.
 *
 * Eso importa porque ReportePedidosPendientes manda un correo diciendo
 * "ATENCION PEDIDOS QUE SE ESTAN TOMANDO HACE MAS DE 7 MINUTOS", la persona
 * entra por el menu a revisarlo, y se encuentra una pantalla muerta.
 *
 * QUE PEDIDO ES ESTE
 *
 * idestadopedido = 1 (En curso) con enviadopixel = 0. Son pedidos que se
 * empezaron a tomar y nunca se finalizaron: tienen productos, cliente,
 * direccion y telefono, pero NO tienen forma de pago, y por eso total_neto
 * esta en cero.
 *
 * NINGUN PROCESO LOS MANDA, NI LO INTENTA. Las cuatro consultas de envio de
 * Servicios piden idestadopedido = 2 y se unen a pedido_forma_pago. Un pedido
 * a medio tomar no entra en ninguna. Medido sobre 7 dias: 29 de los 33 pedidos
 * atascados son de estos, llevan 71 horas en promedio y el peor 152.
 *
 * DOS ACCIONES, Y LA REALISTA ES LA SEGUNDA
 *
 * Terminar sirve cuando el pedido tiene minutos y el cliente todavia esta al
 * telefono. Descartar es lo que se va a usar en la practica: de los 29
 * medidos, ninguno tenia menos de una hora.
 */

var server;
var table;
var dtdetalle;
var idPedido = 0;
var idCliente = 0;
var idTienda = 0;
var administrador = "N";

// Edicion del contenido: si la persona esta editando el pedido (ya lo tiene retenido), el temporizador que
// alarga la retencion y el catalogo de la tienda del pedido.
var editando = false;
var temporizadorRenovacion = null;
var catalogoEdicion = [];

/* Cuanto dura la retencion mientras alguien trabaja el pedido. Ver
 * capaDAOCC.RetencionPedidoDAO: si la persona se va, se suelta sola. */
var MINUTOS_RETENCION = 10;

$(document).ready(function () {

	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length
			- ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	//El mismo marcador que usan las pantallas hermanas. "respuesta" la deja
	//puesta el include de validacion de sesion.
	if (typeof respuesta !== 'undefined') {
		administrador = (respuesta == 'OKA') ? 'S' : 'N';
	}

	dtdetalle = $('#grid-detallepedido').DataTable({
		"aoColumns": [
			{ "mData": "iddetallepedido" },
			{ "mData": "nombreproducto" },
			{ "mData": "cantidad" },
			{ "mData": "especialidad1" },
			{ "mData": "modespecialidad1" },
			{ "mData": "especialidad2" },
			{ "mData": "modespecialidad2" },
			{ "mData": "valorunitario" },
			{ "mData": "valortotal" },
			{ "mData": "adicion" },
			{ "mData": "observacion" },
			{ "mData": "liquido" },
			{ "mData": "excepcion" }
		]
	});

	table = $('#grid-encabezadopedido').DataTable({
		//El orden TIENE que ser el mismo de los <th> del HTML. Si se agrega
		//una columna alla, hay que agregarla aca o la tabla se corre entera.
		"aoColumns": [
			{ "mData": "idpedido" },
			{ "mData": "tienda" },
			{ "mData": "fechainsercion" },
			{ "mData": "cliente" },
			{ "mData": "direccion" },
			{ "mData": "telefono" },
			{ "mData": "totalneto" },
			{ "mData": "estadopedido" },
			{ "mData": "usuariopedido" },
			{ "mData": "numposheader" },
			{ "mData": "enviadopixel" },
			{ "mData": "estadoenviotienda" },
			{ "mData": "formapago" },
			{ "mData": "tiempopedido" },
			{ "mData": "accion" },
			{ "mData": "idtienda", "visible": false },
			{ "mData": "urltienda", "visible": false },
			{ "mData": "stringpixel", "visible": false }
		],
		//Lo mas viejo primero: es lo que lleva mas tiempo colgado.
		"order": [[2, "asc"]]
	});

	$('#grid-encabezadopedido tbody').on('click', 'tr', function () {
		var datos = table.row(this).data();
		if (!datos) {
			return;
		}
		seleccionarPedido(datos);
	});

	//Las fechas NO se llenan aqui: el selector de fechas de la pagina
	//(ConsultaPedidosEnCurso.html, al final) las pone en dd/mm/aaaa, con la
	//semana que va corriendo como rango. Antes esto las dejaba en aaaa-mm-dd,
	//y el servidor no las entendia; ademas se veian en dos formatos distintos
	//segun se hubieran escrito o escogido en el calendario.

	//Esta pantalla es la de los que quedaron a medio tomar: se abre con ese
	//filtro puesto, aunque se pueda cambiar.
	$('#selectEstado').val('1');
	$('#selectEstadoTienda').val('0');

	cargarTiendas();
	botones(false);
});

/*
 * El servicio ConsultaIntegradaPedidosEnCurso recibe la tienda por su NOMBRE,
 * o la palabra TODAS: asi lo hacen todas las pantallas hermanas
 * (consultapedidos.js y compania). Cuando esta lista mandaba el id -y "0" para
 * todas- el servidor buscaba una tienda llamada "0", no la encontraba, y
 * filtraba por idtienda = 0: la consulta volvia siempre vacia.
 *
 * GetTiendas devuelve una lista de {id, nombre}; el id viene en "id", no en
 * "idtienda".
 */
function cargarTiendas() {
	$.getJSON(server + 'GetTiendas', function (datos) {
		var lista = (datos && datos.tiendas) ? datos.tiendas : datos;
		var html = '';
		for (var i = 0; i < lista.length; i++) {
			html += '<option value="' + lista[i].nombre + '">'
				+ lista[i].nombre + '</option>';
		}
		html += '<option value="TODAS" selected>TODAS</option>';
		$('#selectTiendas').html(html);
	});
}

/*
 * El servidor lee las fechas como dd/MM/yyyy (el formato del selector de
 * fechas de la pagina). Si por cualquier razon el campo trae aaaa-mm-dd, se
 * convierte antes de enviar: con ese formato la consulta fallaba entera.
 */
function fechaParaServidor(texto) {
	var t = $.trim(texto || '');
	var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(t);
	return m ? (m[3] + '/' + m[2] + '/' + m[1]) : t;
}

/* Prende o apaga los botones que actuan sobre un pedido. */
function botones(hay) {
	$('#editarPedido').attr('disabled', !hay);
	$('#terminarPedido').attr('disabled', !hay);
	$('#cancelarPedido').attr('disabled', !hay);
}

function consultarPedido() {
	var fechaini = fechaParaServidor($('#fechainicial').val());
	var fechafin = fechaParaServidor($('#fechafinal').val());

	if (!fechaini || !fechafin) {
		alert('Las dos fechas son obligatorias.');
		return;
	}

	$.ajax({
		url: server + 'ConsultaIntegradaPedidosEnCurso',
		type: 'GET',
		dataType: 'json',
		data: {
			fechainicial: fechaini,
			fechafinal: fechafin,
			tienda: $('#selectTiendas').val(),
			numeropedido: $('#numeropedido').val(),
			estado: $('#selectEstado').val(),
			estadotienda: $('#selectEstadoTienda').val()
		},
		success: function (datos) {
			var lista = (datos && datos.pedidos) ? datos.pedidos : datos;
			if (!lista || lista.length === 0) {
				table.clear().draw();
				limpiar();
				alert('No hay pedidos a medio tomar en ese rango. Eso es buena noticia.');
				return;
			}
			//La antiguedad es lo primero que hay que ver: decide si el pedido
			//se rescata o se descarta. El servidor no la manda, se calcula aca.
			for (var i = 0; i < lista.length; i++) {
				lista[i].accion = antiguedad(lista[i].fechainsercion);
			}
			table.clear();
			table.rows.add(lista).draw();
			limpiar();
		},
		error: function () {
			alert('No se pudieron consultar los pedidos. Intente de nuevo.');
		}
	});
}

/*
 * Cuanto lleva colgado, en palabras.
 *
 * Va en texto y no en minutos pelados porque "3 dias" se entiende de un
 * vistazo y "4.320" hay que dividirlo mentalmente.
 */
function antiguedad(fechaInsercion) {
	if (!fechaInsercion) {
		return ('');
	}
	var f = new Date(String(fechaInsercion).replace(' ', 'T'));
	if (isNaN(f.getTime())) {
		return ('');
	}
	var minutos = Math.floor((new Date().getTime() - f.getTime()) / 60000);
	if (minutos < 60) {
		return (minutos + ' min');
	}
	if (minutos < 1440) {
		return (Math.floor(minutos / 60) + ' h');
	}
	return (Math.floor(minutos / 1440) + ' dias');
}

function seleccionarPedido(datos) {
	idPedido = datos.idpedido;
	idCliente = datos.idcliente;
	idTienda = datos.idtienda;

	$('#NumPedido').val(datos.idpedido);
	$('#Cliente').val(datos.cliente);
	$('#estadopedido').val(datos.estadopedido);
	$('#estadotienda').val(datos.estadoenviotienda);
	$('#numpedidotienda').val(datos.numposheader);
	$('#totalpedido').val(datos.totalneto);
	$('#formapago').val(datos.formapago);
	$('#tiempopedido').val(datos.tiempopedido);
	$('#fechafinalizacion').val(datos.fechafinalizacion);
	$('#fechapagovirtual').val(datos.fechapagovirtual);
	$('#idlink').val(datos.idlink);

	$('#telefono').val(datos.telefono);
	$('#direccion').val(datos.direccion);
	$('#tienda').val(datos.tienda);

	cargarDetalle(datos.idpedido);
	botones(true);
}

function cargarDetalle(numero) {
	$.ajax({
		url: server + 'ConsultarDetallePedido',
		type: 'GET',
		dataType: 'json',
		data: { numeropedido: numero },
		success: function (datos) {
			var lista = (datos && datos.detallepedido) ? datos.detallepedido : datos;
			dtdetalle.clear();
			if (lista && lista.length > 0) {
				dtdetalle.rows.add(lista);
			}
			dtdetalle.draw();
		},
		error: function () {
			dtdetalle.clear().draw();
		}
	});
}

function limpiar() {
	detenerRenovacion();
	editando = false;
	idPedido = 0;
	idCliente = 0;
	idTienda = 0;
	$('#NumPedido, #Cliente, #estadopedido, #estadotienda, #numpedidotienda').val('');
	$('#totalpedido, #formapago, #tiempopedido, #fechafinalizacion').val('');
	$('#fechapagovirtual, #idlink, #telefono, #direccion, #tienda').val('');
	dtdetalle.clear().draw();
	botones(false);
}

/*
 * DESCARTAR
 *
 * Es la accion que de verdad se va a usar. Un pedido de tres dias no se
 * rescata: el cliente colgo hace rato. Lo que hace falta es cerrarlo para que
 * deje de aparecer en el correo de alerta todos los dias.
 *
 * Se confirma mostrando la antiguedad, porque descartar uno de 8 minutos -que
 * si se podia rescatar- no tiene vuelta atras.
 */
function cancelarPedido() {
	if (idPedido <= 0) {
		alert('Escoja primero un pedido de la lista.');
		return;
	}
	var datos = table.row('.selected').data();
	var cuanto = datos ? datos.accion : '';
	var aviso = 'Va a descartar el pedido ' + idPedido
		+ (cuanto ? ' (lleva ' + cuanto + ' sin terminar)' : '')
		+ '.\n\nNo se puede deshacer. Si el cliente todavia esta al telefono,'
		+ ' use Terminar en vez de descartarlo.\n\nContinua?';
	if (!confirm(aviso)) {
		return;
	}
	$.ajax({
		url: server + 'CancelarPedido',
		type: 'GET',
		data: { idpedido: idPedido },
		success: function () {
			alert('Pedido ' + idPedido + ' descartado.');
			consultarPedido();
		},
		error: function () {
			alert('No se pudo descartar el pedido. Intente de nuevo.');
		}
	});
}

/*
 * TERMINAR
 *
 * Lo unico que le falta al pedido es la forma de pago: ya tiene productos,
 * cliente y direccion. Terminar es escogerla, finalizar y dejar que salga por
 * el camino de siempre.
 *
 * Se retiene ANTES de abrir el dialogo. Si no, mientras la persona escoge la
 * forma de pago, el pedido pasa a estado 2 y la red de seguridad se lo lleva a
 * cocina a los 5 minutos a medio terminar. Ver RetencionPedidoDAO.
 */
function terminarPedido() {
	if (idPedido <= 0) {
		alert('Escoja primero un pedido de la lista.');
		return;
	}
	// Si viene de editar, el pedido ya esta retenido por esta persona: retener otra vez fallaria contra si mismo.
	if (editando) {
		abrirFormasPago();
		return;
	}
	$.ajax({
		url: server + 'TerminarPedidoEnCurso',
		type: 'GET',
		dataType: 'json',
		data: { accion: 'retener', idpedido: idPedido, minutos: MINUTOS_RETENCION },
		success: function (r) {
			if (!r || r.ok !== 'S') {
				alert(r && r.mensaje ? r.mensaje : 'No se pudo tomar el pedido para terminarlo.');
				return;
			}
			abrirFormasPago();
		},
		error: function () {
			alert('No se pudo tomar el pedido para terminarlo.');
		}
	});
}

function abrirFormasPago() {
	//El mismo endpoint que usa ModificarGeneralPedido. idoperacion=5 lista, y
	//tipopago=T las trae todas.
	$.getJSON(server + 'CRUDFormaPago?idoperacion=5&tipopago=T', function (datos) {
		var lista = (datos && datos.formapago) ? datos.formapago : datos;
		var html = '';
		for (var i = 0; i < lista.length; i++) {
			html += '<option value="' + lista[i].idformapago + '">'
				+ lista[i].nombre + '</option>';
		}
		$('#selectFormaPagoTerminar').html(html);
		$('#valorPagoTerminar').val('');
		$('#modalTerminar').modal('show');
	}).fail(function () {
		//Si no se pudo abrir el dialogo hay que soltar el pedido: dejarlo
		//retenido sin que nadie lo este trabajando es peor que no haber
		//empezado.
		editando = false;
		soltar();
		alert('No se pudieron cargar las formas de pago.');
	});
}

function confirmarTerminar() {
	var idFormaPago = $('#selectFormaPagoTerminar').val();
	var valor = $('#valorPagoTerminar').val();
	if (!idFormaPago) {
		alert('Escoja la forma de pago.');
		return;
	}
	$.ajax({
		url: server + 'TerminarPedidoEnCurso',
		type: 'GET',
		dataType: 'json',
		data: {
			accion: 'terminar',
			idpedido: idPedido,
			idcliente: idCliente,
			idformapago: idFormaPago,
			valorformapago: valor
		},
		success: function (r) {
			editando = false;
			$('#modalTerminar').modal('hide');
			if (r && r.ok === 'S') {
				alert('Pedido ' + idPedido + ' terminado y enviado a la tienda.');
			} else {
				alert(r && r.mensaje ? r.mensaje : 'No se pudo terminar el pedido.');
			}
			consultarPedido();
		},
		error: function () {
			editando = false;
			$('#modalTerminar').modal('hide');
			soltar();
			alert('No se pudo terminar el pedido. Quedo como estaba.');
		}
	});
}

/* Suelta la retencion sin terminar. Se llama al cancelar el dialogo. */
function soltar() {
	if (idPedido <= 0) {
		return;
	}
	$.ajax({
		url: server + 'TerminarPedidoEnCurso',
		type: 'GET',
		data: { accion: 'soltar', idpedido: idPedido }
	});
}

function cancelarTerminar() {
	editando = false;
	soltar();
	$('#modalTerminar').modal('hide');
}

function realizarOtraConsulta() {
	limpiar();
	table.clear().draw();
}

/*
 * EDITAR LOS PRODUCTOS DEL PEDIDO
 *
 * Es la misma pantalla: despues de escoger un pedido de la lista, "Editar productos" lo retiene -el mismo
 * mecanismo de Terminar: mientras se edita, nadie mas lo toca y la red de seguridad no se lo lleva a la tienda a
 * medio cambiar- y abre un dialogo con lo que tiene, para quitar un producto o agregar otro.
 *
 * El navegador no calcula ni manda ningun valor. Manda que producto, cuantos y con que especialidades; el precio
 * lo calcula el servidor con las reglas que ya usa el bot de WhatsApp. Ver capaServicioCC.EditarPedidoEnCurso.
 *
 * Lo que NO hace: adiciones, quitar ingredientes ni promociones. Esas se arman en la pantalla de tomar pedidos.
 */

/** Cada cuanto se alarga la retencion mientras el dialogo esta abierto: menos de los 10 minutos que dura. */
var MS_RENOVACION = 4 * 60 * 1000;

function escapar(texto) {
	return $('<div/>').text(texto === null || texto === undefined ? '' : texto).html();
}

function pesos(valor) {
	var n = Math.round(Number(valor) || 0);
	return '$ ' + n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function mensajeEdicion(texto, tipo) {
	if (!texto) {
		$('#editMensaje').empty();
		return;
	}
	$('#editMensaje').html('<div class="alert alert-' + (tipo || 'danger') + '" style="padding:8px 12px;">'
		+ escapar(texto) + '</div>');
}

function editarPedido() {
	if (idPedido <= 0) {
		alert('Escoja primero un pedido de la lista.');
		return;
	}
	$.ajax({
		url: server + 'TerminarPedidoEnCurso',
		type: 'GET',
		dataType: 'json',
		data: { accion: 'retener', idpedido: idPedido, minutos: MINUTOS_RETENCION },
		success: function (r) {
			if (!r || r.ok !== 'S') {
				alert(r && r.mensaje ? r.mensaje : 'No se pudo tomar el pedido para editarlo.');
				return;
			}
			editando = true;
			$('#editNumPedido').text('#' + idPedido);
			mensajeEdicion('');
			$('#modalEditar').modal('show');
			iniciarRenovacion();
			cargarLineasEdicion();
			cargarCatalogoEdicion();
		},
		error: function () {
			alert('No se pudo tomar el pedido para editarlo.');
		}
	});
}

function iniciarRenovacion() {
	detenerRenovacion();
	temporizadorRenovacion = setInterval(function () {
		$.ajax({
			url: server + 'EditarPedidoEnCurso',
			type: 'POST',
			dataType: 'json',
			data: { accion: 'renovar', idpedido: idPedido },
			success: function (r) {
				if (!r || r.ok !== 'S') {
					mensajeEdicion(r && r.mensaje ? r.mensaje : 'Se perdio la edicion de este pedido.', 'danger');
				}
			}
		});
	}, MS_RENOVACION);
}

function detenerRenovacion() {
	if (temporizadorRenovacion !== null) {
		clearInterval(temporizadorRenovacion);
		temporizadorRenovacion = null;
	}
}

function cargarLineasEdicion() {
	$.ajax({
		url: server + 'EditarPedidoEnCurso',
		type: 'GET',
		dataType: 'json',
		data: { accion: 'lineas', idpedido: idPedido },
		success: function (r) {
			pintarLineasEdicion(r);
		},
		error: function () {
			mensajeEdicion('No se pudo cargar el detalle del pedido.', 'danger');
		}
	});
}

function pintarLineasEdicion(r) {
	if (!r || r.ok !== 'S') {
		mensajeEdicion(r && r.mensaje ? r.mensaje : 'No se pudo actualizar el detalle.', 'danger');
		return;
	}
	var html = '';
	var lineas = r.lineas || [];
	for (var i = 0; i < lineas.length; i++) {
		var l = lineas[i];
		var detalle = '';
		if (l.especialidad1) {
			detalle += l.especialidad1 + (l.especialidad2 ? ' / ' + l.especialidad2 : '');
		}
		if (l.adicion) {
			detalle += (detalle ? ' - ' : '') + l.adicion;
		}
		if (l.observacion && l.observacion.indexOf('Producto Incluido-') !== 0) {
			detalle += (detalle ? ' - ' : '') + l.observacion;
		}
		var esHija = l.eshija === true;
		html += '<tr' + (esHija ? ' class="text-muted"' : '') + '>'
			+ '<td>' + (esHija ? '&nbsp;&nbsp;&rarr; ' : '') + escapar(l.producto)
			+ (detalle ? '<br><small>' + escapar(detalle) + '</small>' : '') + '</td>'
			+ '<td style="text-align:right;">' + l.cantidad + '</td>'
			+ '<td style="text-align:right;">' + pesos(l.valortotal) + '</td>'
			+ '<td style="text-align:right;">'
			+ (esHija ? '' : '<button type="button" class="btn btn-danger btn-xs" onclick="quitarLinea('
				+ l.iddetalle + ')">Quitar</button>')
			+ '</td></tr>';
	}
	if (html === '') {
		html = '<tr><td colspan="4" class="text-center text-muted">El pedido no tiene productos.</td></tr>';
	}
	$('#tablaEditar tbody').html(html);
	$('#editTotal').text(pesos(r.total));
}

function quitarLinea(idDetalle) {
	if (!confirm('Quitar este producto del pedido ' + idPedido + '?')) {
		return;
	}
	$.ajax({
		url: server + 'EditarPedidoEnCurso',
		type: 'POST',
		dataType: 'json',
		data: { accion: 'quitar', idpedido: idPedido, iddetalle: idDetalle },
		success: function (r) {
			if (r && r.ok === 'S') {
				mensajeEdicion('');
			}
			pintarLineasEdicion(r);
		},
		error: function () {
			mensajeEdicion('No se pudo quitar el producto. Intente de nuevo.', 'danger');
		}
	});
}

function cargarCatalogoEdicion() {
	$.ajax({
		url: server + 'EditarPedidoEnCurso',
		type: 'GET',
		dataType: 'json',
		data: { accion: 'catalogo', idpedido: idPedido },
		success: function (r) {
			if (!r || r.ok !== 'S') {
				mensajeEdicion(r && r.mensaje ? r.mensaje : 'No se pudo cargar el catalogo.', 'danger');
				return;
			}
			catalogoEdicion = r.productos || [];
			// Agrupado por tipo (PIZZA, GASEOSA...): son cientos de productos y en una lista plana no se encuentran.
			var grupos = {};
			var orden = [];
			for (var i = 0; i < catalogoEdicion.length; i++) {
				var p = catalogoEdicion[i];
				var tipo = p.tipo || 'OTROS';
				if (!grupos[tipo]) {
					grupos[tipo] = [];
					orden.push(tipo);
				}
				grupos[tipo].push(p);
			}
			var html = '<option value="0">Escoja un producto...</option>';
			for (var g = 0; g < orden.length; g++) {
				html += '<optgroup label="' + escapar(orden[g]) + '">';
				var lista = grupos[orden[g]];
				for (var k = 0; k < lista.length; k++) {
					html += '<option value="' + lista[k].idproducto + '">' + escapar(lista[k].nombre)
						+ ' - ' + pesos(lista[k].precio) + '</option>';
				}
				html += '</optgroup>';
			}
			$('#editProducto').html(html);
			cambioProducto();
		},
		error: function () {
			mensajeEdicion('No se pudo cargar el catalogo de la tienda.', 'danger');
		}
	});
}

/* Al escoger un producto se piden solo las opciones que ese producto tiene: especialidades y sabor de bebida. */
function cambioProducto() {
	var idProducto = parseInt($('#editProducto').val(), 10) || 0;
	$('#editFilaEspecialidades').hide();
	$('#editFilaSabor').hide();
	$('#editEspecialidad1, #editEspecialidad2, #editSabor').empty();
	if (idProducto <= 0) {
		return;
	}
	var producto = null;
	for (var i = 0; i < catalogoEdicion.length; i++) {
		if (catalogoEdicion[i].idproducto === idProducto) {
			producto = catalogoEdicion[i];
			break;
		}
	}
	if (!producto) {
		return;
	}
	var pideEspecialidad = String(producto.controlaespecialidades).toUpperCase() === 'S';
	var pideSabor = String(producto.incluyeliquido).toUpperCase() === 'S';
	if (!pideEspecialidad && !pideSabor) {
		return;
	}
	$.ajax({
		url: server + 'EditarPedidoEnCurso',
		type: 'GET',
		dataType: 'json',
		data: { accion: 'opciones', idpedido: idPedido, idproducto: idProducto },
		success: function (r) {
			if (!r || r.ok !== 'S') {
				mensajeEdicion(r && r.mensaje ? r.mensaje : 'No se pudieron cargar las opciones.', 'danger');
				return;
			}
			if (pideEspecialidad) {
				var esp = '<option value="0">Escoja...</option>';
				var esp2 = '<option value="0">Sin segunda mitad</option>';
				var lista = r.especialidades || [];
				for (var j = 0; j < lista.length; j++) {
					var etiqueta = escapar(lista[j].nombre) + (lista[j].adicional > 0 ? ' (+ ' + pesos(lista[j].adicional) + ')' : '');
					esp += '<option value="' + lista[j].idespecialidad + '">' + etiqueta + '</option>';
					esp2 += '<option value="' + lista[j].idespecialidad + '">' + etiqueta + '</option>';
				}
				$('#editEspecialidad1').html(esp);
				$('#editEspecialidad2').html(esp2);
				$('#editFilaEspecialidades').show();
			}
			if (pideSabor) {
				var sab = '<option value="0">Escoja el sabor...</option>';
				var sabores = r.sabores || [];
				for (var s = 0; s < sabores.length; s++) {
					sab += '<option value="' + sabores[s].idsabor + '">' + escapar(sabores[s].nombre)
						+ (sabores[s].adicional > 0 ? ' (+ ' + pesos(sabores[s].adicional) + ')' : '') + '</option>';
				}
				$('#editSabor').html(sab);
				$('#editFilaSabor').show();
			}
		},
		error: function () {
			mensajeEdicion('No se pudieron cargar las opciones del producto.', 'danger');
		}
	});
}

function agregarProducto() {
	var idProducto = parseInt($('#editProducto').val(), 10) || 0;
	if (idProducto <= 0) {
		mensajeEdicion('Escoja el producto que quiere agregar.', 'warning');
		return;
	}
	var cantidad = parseInt($('#editCantidad').val(), 10) || 0;
	if (cantidad < 1 || cantidad > 20) {
		mensajeEdicion('La cantidad debe estar entre 1 y 20.', 'warning');
		return;
	}
	$('#btnAgregarProducto').attr('disabled', true);
	$.ajax({
		url: server + 'EditarPedidoEnCurso',
		type: 'POST',
		dataType: 'json',
		data: {
			accion: 'agregar',
			idpedido: idPedido,
			idproducto: idProducto,
			cantidad: cantidad,
			idespecialidad1: $('#editEspecialidad1').val() || 0,
			idespecialidad2: $('#editEspecialidad2').val() || 0,
			idsabor: $('#editSabor').val() || 0,
			observacion: $('#editObservacion').val()
		},
		success: function (r) {
			$('#btnAgregarProducto').attr('disabled', false);
			if (r && r.ok === 'S') {
				mensajeEdicion('Producto agregado.', 'success');
				$('#editProducto').val('0');
				$('#editCantidad').val(1);
				$('#editObservacion').val('');
				cambioProducto();
			}
			pintarLineasEdicion(r);
		},
		error: function () {
			$('#btnAgregarProducto').attr('disabled', false);
			mensajeEdicion('No se pudo agregar el producto. Intente de nuevo.', 'danger');
		}
	});
}

/* Cierra el dialogo y suelta el pedido tal como quedo. El detalle de la pantalla se refresca con lo nuevo. */
function cerrarEdicion() {
	detenerRenovacion();
	soltar();
	editando = false;
	$('#modalEditar').modal('hide');
	cargarDetalle(idPedido);
}

/* Pasa de editar a terminar sin soltar el pedido en medio: la retencion sigue siendo de esta persona. */
function terminarDesdeEdicion() {
	detenerRenovacion();
	// Bootstrap 3 no apila dos dialogos bien: el de formas de pago se abre cuando este termine de cerrarse.
	$('#modalEditar').one('hidden.bs.modal', function () {
		cargarDetalle(idPedido);
		terminarPedido();
	});
	$('#modalEditar').modal('hide');
}

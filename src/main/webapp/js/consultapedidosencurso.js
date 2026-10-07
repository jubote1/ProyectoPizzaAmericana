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

	//Por defecto, la semana que va corriendo: un pedido colgado de hace mas de
	//una semana ya no se rescata, y pedir un rango mas grande solo hace lenta
	//la consulta.
	var hoy = new Date();
	var hace7 = new Date(hoy.getTime() - 7 * 24 * 60 * 60 * 1000);
	$('#fechainicial').val(aISO(hace7));
	$('#fechafinal').val(aISO(hoy));

	//Esta pantalla es la de los que quedaron a medio tomar: se abre con ese
	//filtro puesto, aunque se pueda cambiar.
	$('#selectEstado').val('1');
	$('#selectEstadoTienda').val('0');

	cargarTiendas();
	botones(false);
});

function aISO(f) {
	var m = f.getMonth() + 1;
	var d = f.getDate();
	return (f.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (d < 10 ? '0' : '') + d);
}

function cargarTiendas() {
	$.getJSON(server + 'GetTiendas', function (datos) {
		var lista = (datos && datos.tiendas) ? datos.tiendas : datos;
		var html = '<option value="0">Todas</option>';
		for (var i = 0; i < lista.length; i++) {
			html += '<option value="' + lista[i].idtienda + '">'
				+ lista[i].nombre + '</option>';
		}
		$('#selectTiendas').html(html);
	});
}

/* Prende o apaga los botones que actuan sobre un pedido. */
function botones(hay) {
	$('#terminarPedido').attr('disabled', !hay);
	$('#cancelarPedido').attr('disabled', !hay);
}

function consultarPedido() {
	var fechaini = $('#fechainicial').val();
	var fechafin = $('#fechafinal').val();

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
			$('#modalTerminar').modal('hide');
			if (r && r.ok === 'S') {
				alert('Pedido ' + idPedido + ' terminado y enviado a la tienda.');
			} else {
				alert(r && r.mensaje ? r.mensaje : 'No se pudo terminar el pedido.');
			}
			consultarPedido();
		},
		error: function () {
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
	soltar();
	$('#modalTerminar').modal('hide');
}

function realizarOtraConsulta() {
	limpiar();
	table.clear().draw();
}

var server;
var tablePorTienda;
var tablePedidosCargo;
var tableAsignacionesCanceladas;

$(document).ready(function() {

	const loc = window.location;
	const pathParts = loc.pathname.split('/');
	const baseFolder = "ProyectoPizzaAmericana";
	const index = pathParts.indexOf(baseFolder);
	server = `${loc.origin}/${pathParts.slice(1, index + 1).join("/")}/`;

	let respuesta = '';
	let usuario = '';

	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'POST',
		async: false,
		success: function(data) {
			respuesta = data[0]?.respuesta || '';
			usuario = data[0]?.nombreusuario || '';
		},
		error: function() {
			console.error("Error al validar el usuario.");
			location.href = server + "Index.html";
		}
	});

	switch (respuesta) {
		case 'OK':
			$('#cargarMenu').load(server + "Menu.html", function() {
				$('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
			});
			break;
		case 'OKA':
			$('#cargarMenu').load(server + "MenuAdm.html", function() {
				$('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
			});
			break;
		case 'OKP':
			$('#cargarMenu').load(server + "MenuPQRS.html", function() {
				$('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
			});
			break;
		default:
			location.href = server + "Index.html";
			return;
	}
	$('#usuarioenpantalla').text(usuario);

	inicializarTablas();
	inicializarFechasPorDefecto();
	getListaTiendas();
});

function inicializarFechasPorDefecto() {
	const hoy = new Date();
	const haceUnaSemana = new Date();
	haceUnaSemana.setDate(hoy.getDate() - 7);
	$("#fechadesde").datepicker('setDate', haceUnaSemana);
	$("#fechahasta").datepicker('setDate', hoy);
}

function getListaTiendas() {
	$.getJSON(server + 'GetTiendasFuncionales', function(data) {
		var str = '<option value="0">TODAS</option>';
		for (var i = 0; i < data.length; i++) {
			str += '<option value="' + data[i].id + '">' + data[i].nombre + '</option>';
		}
		$('#selectTiendas').html(str).val('0');
		// Con las tiendas listas y las fechas por defecto ya puestas, se
		// consulta una vez sola para que el dashboard no abra vacio.
		consultarDashboard();
	});
}

function inicializarTablas() {
	tablePorTienda = $('#grid-por-tienda').DataTable({
		"aoColumns": [
			{ "mData": "nombretienda" },
			{ "mData": "total" },
			{ "mData": "cumplidos" },
			{ "mData": "fueradetiempo" },
			{ "mData": "cancelados" },
			{ "mData": "sincierre" },
			{ "mData": "porcentajecumplimiento", "render": renderPorcentaje },
			{ "mData": "minutospromediototal", "render": renderMinutos }
		],
		"order": [[6, 'desc']],
		"paging": false,
		"searching": false,
		"info": false
	});

	tablePedidosCargo = $('#grid-pedidos-cargo').DataTable({
		"aoColumns": [
			{ "mData": "idpedido" },
			{ "mData": "nombretienda" },
			{ "mData": "numposheader" },
			{ "mData": "fechapedido" },
			{ "mData": "fechainsercion", "render": renderFechaHora },
			{ "mData": "fechaentregado", "render": renderFechaHora },
			{ "mData": "estado", "render": renderEstado },
			{ "mData": "minutostotal", "render": renderMinutos }
		],
		"order": [[3, 'desc']]
	});

	tableAsignacionesCanceladas = $('#grid-asignaciones-canceladas').DataTable({
		"aoColumns": [
			{ "mData": "idpedido" },
			{ "mData": "nombretienda" },
			{ "mData": "numposheader" },
			{ "mData": "fechapedido" },
			{ "mData": "proveedor" },
			{ "mData": "ultimoestadocargo" },
			{ "mData": "fechaasignacion", "render": renderFechaHora },
			{ "mData": "fechacancelacioncargo", "render": renderFechaHora }
		],
		"order": [[3, 'desc']]
	});
}

function renderFechaHora(data) {
	if (!data) {
		return '-';
	}
	return data.substring(0, 16);
}

function renderMinutos(data) {
	if (data === null || data === undefined) {
		return '-';
	}
	const minutos = Math.round(data);
	const horas = Math.floor(minutos / 60);
	const resto = minutos % 60;
	return horas > 0 ? (horas + 'h ' + resto + 'm') : (resto + ' min');
}

function renderEstado(data) {
	var clase = 'cg-estado-sincierre';
	if (data === 'CUMPLIDO') {
		clase = 'cg-estado-cumplido';
	} else if (data === 'FUERA DE TIEMPO') {
		clase = 'cg-estado-fueradetiempo';
	} else if (data === 'CANCELADO') {
		clase = 'cg-estado-cancelado';
	}
	return '<span class="cg-estado ' + clase + '">' + data + '</span>';
}

function renderPorcentaje(data) {
	const pct = data === null || data === undefined ? 0 : data;
	var claseFranja = '';
	if (pct < 70) {
		claseFranja = 'cg-pct-baja';
	} else if (pct < 90) {
		claseFranja = 'cg-pct-media';
	}
	return '<div class="cg-pct-wrap ' + claseFranja + '">'
		+ '<div class="cg-pct-bar"><span style="width:' + Math.min(pct, 100) + '%"></span></div>'
		+ '<div class="cg-pct-texto">' + pct.toFixed(0) + '%</div>'
		+ '</div>';
}

function consultarDashboard() {
	const fechaDesde = $("#fechadesde").val();
	const fechaHasta = $("#fechahasta").val();
	const idTienda = $("#selectTiendas").val();

	if (!fechaDesde || !fechaHasta) {
		alert('Escoja la fecha desde y la fecha hasta.');
		return;
	}

	$.getJSON(server + 'ConsultarDashboardCargo?idtienda=' + idTienda + '&fechadesde=' + fechaDesde
		+ '&fechahasta=' + fechaHasta, function(datos) {
			pintarKPIs(datos.resumen);
			pintarPorTienda(datos.portienda);
			pintarPedidos(datos.pedidos);
			pintarAsignacionesCanceladas(datos.asignacionescanceladas);
		}).fail(function() {
			alert('No se pudo consultar el Dashboard Cargo.');
		});
}

function pintarKPIs(resumen) {
	$('#kpiTotal').text(resumen.total);
	$('#kpiCumplidos').text(resumen.cumplidos);
	$('#kpiFueraDeTiempo').text(resumen.fueradetiempo);
	$('#kpiCancelados').text(resumen.cancelados);
	$('#kpiSinCierre').text(resumen.sincierre);
	$('#kpiCumplimiento').html(resumen.porcentajecumplimiento.toFixed(0) + '<small>%</small>');
	$('#kpiTiempoTotal').text(renderMinutos(resumen.minutospromediototal));
	$('#kpiAsigCanceladas').text(resumen.totalasignacionescanceladas);

	const metaTexto = '<i class="fas fa-bullseye"></i> Meta: entregar en ' + resumen.umbralminutos + ' minutos o menos';
	$('#lblMetaMinutos').html(metaTexto);
	$('#lblMetaMinutosPedidos').html(metaTexto);
}

function pintarPorTienda(filas) {
	tablePorTienda.clear();
	for (var i = 0; i < filas.length; i++) {
		tablePorTienda.row.add(filas[i]);
	}
	tablePorTienda.draw();
}

function pintarPedidos(filas) {
	tablePedidosCargo.clear();
	for (var i = 0; i < filas.length; i++) {
		tablePedidosCargo.row.add(filas[i]);
	}
	tablePedidosCargo.draw();
}

function pintarAsignacionesCanceladas(filas) {
	tableAsignacionesCanceladas.clear();
	for (var i = 0; i < filas.length; i++) {
		tableAsignacionesCanceladas.row.add(filas[i]);
	}
	tableAsignacionesCanceladas.draw();
}

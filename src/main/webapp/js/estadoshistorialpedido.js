/**
 * Script de Gestión y Rastreo Integral de Pedidos en Tienda
 * Proyecto Pizza Americana
 */

var server;
var tiendas;
var urlTienda = "";
var dtestadopedido;

$(document).ready(function() {
	// Obtenemos el contexto base del servidor
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	// Inicializamos el DataTable de estados con formato amigable en español
	dtestadopedido = $('#grid-estadospedido').DataTable({
		"language": {
			"emptyTable": "No se registran cambios de estado para este pedido",
			"info": "Mostrando _START_ a _END_ de _TOTAL_ estados",
			"infoEmpty": "Mostrando 0 estados",
			"paginate": {
				"first": "Primero",
				"last": "Último",
				"next": "Siguiente",
				"previous": "Anterior"
			}
		},
		"order": [[0, "asc"]],
		"paging": false,
		"info": false,
		"searching": false,
		"columns": [
			{ 
				"data": "fechacambio",
				"render": function(data) {
					return '<i class="glyphicon glyphicon-calendar" style="color:#64748B; margin-right:5px;"></i><strong>' + (data || '--') + '</strong>';
				}
			},
			{ 
				"data": "estadoanterior",
				"render": function(data) {
					return formatearBadgeEstado(data);
				}
			},
			{ 
				"data": "estadoposterior",
				"render": function(data) {
					return formatearBadgeEstado(data);
				}
			}
		]
	});
});

$(function() {
	// Carga inicial del catálogo de tiendas
	getListaTiendas();
	// Validar vigencia de sesión cada 10 minutos
	setInterval('validarVigenciaLogueo()', 600000);
});

function validarVigenciaLogueo() {
	var respuesta = '';
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(data) {
			respuesta = data[0].respuesta;
		}
	});
	if (respuesta !== 'OK' && respuesta !== 'OKA') {
		location.href = server + "Index.html";
	}
}

// Carga el listado de tiendas activas en el select
function getListaTiendas() {
	$.getJSON(server + 'GetTiendas', function(data) {
		tiendas = data;
		var str = '<option value="">-- Seleccione una Tienda --</option>';
		for (var i = 0; i < data.length; i++) {
			var cadaTienda = data[i];
			str += '<option value="' + cadaTienda.nombre + '" id="' + cadaTienda.id + '">' + cadaTienda.nombre + '</option>';
		}
		$('#selectTiendas').html(str);
	});
}

// Formateador de moneda colombiana ($ XX.XXX)
function formatearMoneda(val) {
	var num = Number(val) || 0;
	return '$ ' + num.toLocaleString('es-CO');
}

// Formateador visual con badges para los nombres de estado
function formatearBadgeEstado(nombreEstado) {
	if (!nombreEstado || nombreEstado.trim() === '') {
		return '<span style="color:#94A3B8;">(Inicio)</span>';
	}
	var estadoUpper = nombreEstado.toUpperCase().trim();
	var clase = "badge-estado-default";

	if (estadoUpper.indexOf("ENTREGADO") !== -1 || estadoUpper.indexOf("FINALIZADO") !== -1) {
		clase = "badge-estado-entregado";
	} else if (estadoUpper.indexOf("REPARTO") !== -1 || estadoUpper.indexOf("RUTA") !== -1 || estadoUpper.indexOf("DESPACHADO") !== -1) {
		clase = "badge-estado-reparto";
	} else if (estadoUpper.indexOf("COCINA") !== -1 || estadoUpper.indexOf("HORNEADO") !== -1 || estadoUpper.indexOf("PREPARACION") !== -1) {
		clase = "badge-estado-cocina";
	} else if (estadoUpper.indexOf("ANULADO") !== -1 || estadoUpper.indexOf("CANCELADO") !== -1) {
		clase = "badge-estado-anulado";
	}

	return '<span class="badge-estado ' + clase + '">' + nombreEstado + '</span>';
}

/**
 * Consulta Integral del Pedido:
 * 1. Detalle del pedido y comanda (productos, cliente, valores, pagos).
 * 2. Historial de estados en vivo en la tienda.
 * 3. Datos del domiciliario y tiempo de logística.
 */
function consultarHistorialEstadosPedido() {
	var tiendaId = $('#selectTiendas option:selected').attr('id');
	var tiendaNombre = $('#selectTiendas').val();
	var numeroPedido = $('#numeropedido').val();

	if (!tiendaId && !tiendaNombre) {
		alert('Por favor seleccione una tienda de la lista.');
		$('#selectTiendas').focus();
		return;
	}
	if (!numeroPedido || numeroPedido.trim() === '') {
		alert('Debe ingresar el número de pedido a consultar.');
		$('#numeropedido').focus();
		return;
	}

	numeroPedido = numeroPedido.trim();
	var idTiendaParam = tiendaId ? tiendaId : tiendaNombre;

	// Estados visuales de interfaz
	$('#panelResultados').hide();
	$('#panelSinDatos').hide();
	$('#panelCargando').fadeIn(200);
	$('#btnBuscarPedido').prop('disabled', true);

	var detalleEncontrado = false;
	var datosDetalle = null;

	// =========================================================================
	// PASO 1: Consultar detalle completo (comanda, cliente, valores, pagos)
	// =========================================================================
	$.ajax({
		url: server + 'ConsultarDetallePedidoTienda?idtienda=' + encodeURIComponent(idTiendaParam) + '&idpedidotienda=' + encodeURIComponent(numeroPedido),
		dataType: 'json',
		type: 'GET',
		async: false,
		success: function(resp) {
			if (resp && resp.exito) {
				detalleEncontrado = true;
				datosDetalle = resp;
				renderizarDetallePedido(resp);
			}
		},
		error: function(err) {
			console.warn("No fue posible obtener detalle de productos desde ConsultarDetallePedidoTienda", err);
		}
	});

	// =========================================================================
	// PASO 2: Consultar URL de la tienda para estados y domiciliario
	// =========================================================================
	var estadosEncontrados = false;
	urlTienda = "";

	$.ajax({
		url: server + 'ObtenerUrlTienda?idtienda=' + idTiendaParam,
		dataType: 'json',
		async: false,
		success: function(data2) {
			if (data2 && data2.length > 0 && data2[0].urltienda) {
				urlTienda = data2[0].urltienda;
			}
		}
	});

	if (urlTienda && urlTienda.trim() !== "") {
		// 2.1 Historial de estados del pedido
		$.ajax({
			url: urlTienda + 'ConsultarHistorialEstadosPedidoTienda?idpedidotienda=' + numeroPedido,
			dataType: 'json',
			async: false,
			timeout: 5000,
			success: function(data3) {
				dtestadopedido.clear().draw();
				if (data3 && data3.length > 0) {
					estadosEncontrados = true;
					for (var i = 0; i < data3.length; i++) {
						if (i === 0) {
							$('#usologistica').val(data3[i].logistica || '');
							$('#valDomiLogistica').text(data3[i].logistica ? data3[i].logistica : 'No');
						}
						dtestadopedido.row.add(data3[i]);
					}
					dtestadopedido.draw();
					$('#badgeNumEstados').text(data3.length + ' cambios');

					// Actualizar el KPI con el estado más reciente
					var ultimoEstado = data3[data3.length - 1].estadoposterior;
					if (ultimoEstado) {
						$('#kpiEstadoActual').html(formatearBadgeEstado(ultimoEstado));
					}
				} else {
					$('#badgeNumEstados').text('0 cambios');
				}
			},
			error: function() {
				console.warn("No hubo respuesta del servicio ConsultarHistorialEstadosPedidoTienda");
			}
		});

		// 2.2 Domiciliario que llevó el pedido y tiempo
		$.ajax({
			url: urlTienda + 'ConsultarDomiciliarioPedidoTienda?idpedidotienda=' + numeroPedido,
			dataType: 'json',
			async: false,
			timeout: 5000,
			success: function(data4) {
				if (data4) {
					var nombreDomi = data4.domiciliario || 'Sin asignar';
					var empresaDomi = data4.empresa || 'Americana';
					var telDomi = data4.teldomiciliario || '--';
					var tiempoMin = Number(data4.tiempopedido) || 0;

					// Inputs legacy
					$('#domiciliario').val(nombreDomi);
					$('#empresa').val(empresaDomi);
					$('#teldomiciliario').val(telDomi);
					$('#tiempopedido').val(tiempoMin);

					// Campos modernos
					$('#valDomiNombre').text(nombreDomi);
					$('#valDomiEmpresa').text(empresaDomi);
					if (telDomi && telDomi !== '--') {
						$('#valDomiTelefono').html('<a href="tel:' + telDomi + '"><i class="glyphicon glyphicon-phone"></i> ' + telDomi + '</a>');
					} else {
						$('#valDomiTelefono').text('--');
					}
					$('#valDomiTiempo').text(tiempoMin + ' min');
					$('#kpiTiempoPedido').text(tiempoMin + ' min');

					// Alerta si supera los 90 minutos
					if (tiempoMin >= 90) {
						$('#reportar').val("SI");
						$('#valDomiReportar').html('<span class="label label-danger"><i class="glyphicon glyphicon-warning-sign"></i> SI (Demora excesiva)</span>');
						$('#badgeReportar').attr('class', 'badge').css('background', '#E42528').text('⚠️ REPORTAR');
						$('#alertaReportarDemora').fadeIn(150);
						$('#boxKpiTiempo').removeClass('kpi-amber').addClass('kpi-red');
					} else {
						$('#reportar').val("NO");
						$('#valDomiReportar').html('<span class="label label-success">NO</span>');
						$('#badgeReportar').attr('class', 'badge').css('background', '#166534').text('TIEMPO NORMAL');
						$('#alertaReportarDemora').hide();
						$('#boxKpiTiempo').removeClass('kpi-red').addClass('kpi-amber');
					}
				}
			},
			error: function() {
				console.warn("No hubo respuesta del servicio ConsultarDomiciliarioPedidoTienda");
			}
		});
	}

	// Ocultar cargador y habilitar botón
	$('#panelCargando').hide();
	$('#btnBuscarPedido').prop('disabled', false);

	// Decidir qué mostrar
	if (detalleEncontrado || estadosEncontrados) {
		$('#panelResultados').fadeIn(250);
	} else {
		$('#msgSinDatos').text('No se encontró el pedido #' + numeroPedido + ' en ' + ($('#selectTiendas option:selected').text() || 'la tienda seleccionada') + '.');
		$('#panelSinDatos').fadeIn(250);
	}
}

/**
 * Pinta toda la información del cliente, comanda de productos y totales financieros
 */
function renderizarDetallePedido(data) {
	// 1. Datos del Cliente y Ubicación
	var cli = data.cliente || {};
	$('#valClienteNombre').text(cli.nombre || 'Cliente Tienda');

	if (cli.telefono && cli.telefono.trim() !== '') {
		$('#valClienteTelefono').html('<a href="tel:' + cli.telefono + '"><i class="glyphicon glyphicon-phone"></i> ' + cli.telefono + '</a>');
	} else {
		$('#valClienteTelefono').text('No registrado');
	}

	$('#valClienteDireccion').text(cli.direccion || 'Entrega en punto de venta / Mostrador');
	$('#valClienteZona').text(cli.zona || 'Centro');
	$('#valPedidoFecha').text(data.fechapedido || data.fechainsercion || '--');
	$('#valPedidoUsuario').text(data.usuariopedido || 'Sistema');

	// Observaciones
	if (data.observacion && data.observacion.trim() !== '') {
		$('#valPedidoObservacion').text(data.observacion);
		$('#seccionObsPedido').show();
	} else {
		$('#seccionObsPedido').hide();
	}

	if (data.obs_domiciliario && data.obs_domiciliario.trim() !== '') {
		$('#valPedidoObsDomi').text(data.obs_domiciliario);
		$('#seccionObsDomi').show();
	} else {
		$('#seccionObsDomi').hide();
	}

	// Badge Fuente
	if (data.fuente === "TIENDA_LOCAL") {
		$('#badgeFuente').css('background', '#102F6F').html('<i class="glyphicon glyphicon-flash"></i> En Vivo (Tienda)');
	} else {
		$('#badgeFuente').css('background', '#64748B').html('<i class="glyphicon glyphicon-hdd"></i> Respaldo Central');
	}

	// 2. KPIs
	$('#kpiTotalNeto').text(formatearMoneda(data.total_neto));
	$('#kpiTipoPedido').text(data.tipopedido || 'Domicilio');
	if (!$('#kpiEstadoActual').text() || $('#kpiEstadoActual').text() === '--') {
		$('#kpiEstadoActual').html(formatearBadgeEstado('Registrado'));
	}

	// 3. COMANDA / DETALLE DE PRODUCTOS QUE PIDIÓ
	var prods = data.productos || [];
	var tbody = $('#tbodyDetalleProductos');
	tbody.empty();

	if (prods.length === 0) {
		tbody.html('<tr><td colspan="4" class="text-center" style="padding: 20px; color: #94A3B8;">No se encontraron renglones de producto para este pedido.</td></tr>');
		$('#badgeNumItems').text('0 productos');
	} else {
		$('#badgeNumItems').text(prods.length + ' ítems');
		var totalItemsContados = 0;

		for (var i = 0; i < prods.length; i++) {
			var item = prods[i];
			totalItemsContados += Number(item.cantidad) || 0;

			var tr = $('<tr></tr>');

			// Columna Cantidad
			tr.append('<td style="text-align: center;"><span class="badge-cant">' + item.cantidad + '</span></td>');

			// Columna Descripción + Tamaño + Sabores/Observación
			var colDesc = $('<td></td>');
			var nombreHtml = '<span class="item-nombre">' + item.descripcion + '</span>';
			if (item.tamano && item.tamano.trim() !== '') {
				nombreHtml += '<span class="item-tamano">' + item.tamano + '</span>';
			}
			colDesc.append(nombreHtml);

			if (item.observacion && item.observacion.trim() !== '') {
				colDesc.append('<div class="item-sabores"><i class="glyphicon glyphicon-info-sign" style="color:#0284C7; margin-right:4px;"></i>' + item.observacion + '</div>');
			}
			tr.append(colDesc);

			// Columna Valor Unitario
			tr.append('<td style="text-align: right; font-weight: 500; color: #475569;">' + formatearMoneda(item.valorunitario) + '</td>');

			// Columna Subtotal
			tr.append('<td style="text-align: right; font-weight: 700; color: #0F172A;">' + formatearMoneda(item.valortotal) + '</td>');

			tbody.append(tr);
		}
	}

	// 4. Totales Financieros
	$('#valTotalBruto').text(formatearMoneda(data.total_bruto));
	$('#valTotalImpuesto').text(formatearMoneda(data.impuesto));
	$('#valTotalNetoFinal').text(formatearMoneda(data.total_neto));

	// 5. Formas de Pago
	var pagos = data.formas_pago || [];
	var contenedorPagos = $('#contenedorFormasPago');
	contenedorPagos.empty();

	if (pagos.length === 0) {
		contenedorPagos.html('<span class="tag-pago"><i class="glyphicon glyphicon-ok-sign"></i> No especificadas</span>');
	} else {
		for (var p = 0; p < pagos.length; p++) {
			var pago = pagos[p];
			contenedorPagos.append(
				'<span class="tag-pago"><i class="glyphicon glyphicon-ok-sign"></i> ' + 
				(pago.forma_pago || 'Pago') + ': ' + formatearMoneda(pago.valor) + 
				'</span>'
			);
		}
	}
}

// Limpia el formulario y oculta los resultados
function limpiarBusqueda() {
	$('#numeropedido').val('');
	$('#domiciliario').val('');
	$('#empresa').val('');
	$('#usologistica').val('');
	$('#teldomiciliario').val('');
	$('#tiempopedido').val('');
	$('#reportar').val('');

	if (dtestadopedido) {
		dtestadopedido.clear().draw();
	}
	$('#tbodyDetalleProductos').empty();
	$('#contenedorFormasPago').empty();
	$('#alertaReportarDemora').hide();
	$('#panelResultados').hide();
	$('#panelSinDatos').hide();
	$('#numeropedido').focus();
}

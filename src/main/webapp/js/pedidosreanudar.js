/*
 * RETOMAR UN PEDIDO A MEDIO TOMAR EN LA PANTALLA DE TOMAR PEDIDOS
 *
 * Pedidos.html?reanudar=NNN abre el pedido NNN -uno que quedo en curso: tiene cliente y productos pero nunca se
 * finalizo- en la misma pantalla con la que se toma un pedido, para seguir agregandole productos con TODO lo que
 * esa pantalla soporta (especialidades por mitad, adiciones, "con" y "sin", excepciones de precio, productos
 * incluidos, ofertas, puntos, descuentos, domicilio) y finalizarlo por el camino de siempre.
 *
 * POR QUE UN ARCHIVO APARTE Y NO DENTRO DE pedidos.js
 *
 * pedidos.js es la pantalla por la que se toman todos los pedidos y tiene mas de 7.000 lineas. Aqui no se cambia
 * ninguna de sus funciones: este archivo solo se activa si la direccion trae ?reanudar=, y lo unico que hace es
 * dejar en esa pantalla el MISMO estado en que quedaria si la persona hubiera buscado al cliente, escogido la
 * tienda y agregado los productos que el pedido ya tiene. Sin ese parametro no hace nada.
 *
 * QUE HACE, EN ORDEN
 *
 *  1. Pide al servidor el pedido (EditarPedidoEnCurso?accion=reanudar). El servidor lo RETIENE a nombre de la
 *     sesion y se niega si lo trabaja otra persona o si ya esta en la tienda.
 *  2. Busca al cliente por su telefono y lo selecciona como si se hubiera hecho clic en la lista de clientes:
 *     eso es lo que llena los datos, la direccion y carga el catalogo de la tienda.
 *  3. Deja puestos el tipo de pedido, la tienda y el numero de pedido, y bloquea lo que la pantalla bloquea
 *     despues de crear el encabezado (tienda, tipo de pedido, limpiar).
 *  4. Carga en la grilla lo que el pedido ya tiene y recalcula el total con el servidor.
 *  5. Renueva la retencion cada 4 minutos mientras la pantalla siga con ese pedido.
 *
 * Al finalizar, FinalizarPedido suelta la retencion (solo la de esta misma sesion).
 */
(function () {
	'use strict';

	function parametro(nombre) {
		var m = new RegExp('[?&]' + nombre + '=([^&#]*)').exec(window.location.search);
		return m ? decodeURIComponent(m[1].replace(/\+/g, ' ')) : '';
	}

	var idReanudar = parseInt(parametro('reanudar'), 10) || 0;
	if (idReanudar <= 0) {
		return; // Pantalla normal: este archivo no hace nada.
	}

	var MS_RENOVACION = 4 * 60 * 1000;
	var MS_ESPERA_MAXIMA = 25000;
	var temporizador = null;

	function escapar(texto) {
		return $('<div/>').text(texto === null || texto === undefined ? '' : texto).html();
	}

	function aviso(texto) {
		$('#avisoReanudar').remove();
		var $a = $('<div id="avisoReanudar" class="alert alert-info" role="alert"'
			+ ' style="margin:8px 15px;font-size:15px;"></div>');
		$a.html(texto);
		var $contenedor = $('.container-fluid').first();
		if ($contenedor.length === 0) {
			$contenedor = $('body');
		}
		$contenedor.prepend($a);
	}

	function salir(mensaje) {
		alert(mensaje);
		window.location.href = 'ConsultaPedidosEnCurso.html';
	}

	function soltarYSalir(mensaje) {
		$.ajax({
			url: server + 'TerminarPedidoEnCurso',
			type: 'GET',
			data: { accion: 'soltar', idpedido: idReanudar },
			complete: function () {
				salir(mensaje);
			}
		});
	}

	$(function () {
		var inicio = new Date().getTime();

		// pedidos.js carga tiendas, tipos de pedido y tiendas bloqueadas de forma asincrona: se espera a que
		// esten, porque sin ellos no se puede escoger la tienda ni el tipo de pedido.
		function listo() {
			return typeof tiendas !== 'undefined' && tiendas
				&& typeof tiposPedido !== 'undefined' && tiposPedido
				&& typeof tiendasBloqueadas !== 'undefined' && tiendasBloqueadas
				&& $('#selectTiendas option').length > 0
				&& $('#selectTipoPedido option').length > 0;
		}

		function esperar() {
			if (listo()) {
				pedirPedido();
				return;
			}
			if (new Date().getTime() - inicio > MS_ESPERA_MAXIMA) {
				salir('La pantalla no termino de cargar sus listas. Vuelva a intentarlo.');
				return;
			}
			setTimeout(esperar, 300);
		}

		aviso('Retomando el pedido <strong>' + idReanudar + '</strong>...');
		esperar();
	});

	function pedirPedido() {
		$.ajax({
			url: server + 'EditarPedidoEnCurso',
			type: 'GET',
			dataType: 'json',
			data: { accion: 'reanudar', idpedido: idReanudar },
			success: function (r) {
				if (!r || r.ok !== 'S') {
					salir(r && r.mensaje ? r.mensaje : 'No se pudo tomar el pedido para editarlo.');
					return;
				}
				ponerPedido(r);
			},
			error: function (xhr) {
				if (xhr && xhr.status === 401) {
					salir('Su sesion no esta abierta. Ingrese de nuevo.');
				} else {
					salir('No se pudo tomar el pedido para editarlo.');
				}
			}
		});
	}

	function ponerPedido(p) {
		// La tienda del pedido no puede estar bloqueada: es la misma regla de cuando se escoge una tienda.
		for (var i = 0; i < tiendasBloqueadas.length; i++) {
			if (String(tiendasBloqueadas[i].idtienda) === String(p.idtienda)) {
				soltarYSalir('La tienda de este pedido esta deshabilitada en este momento: '
					+ tiendasBloqueadas[i].comentario);
				return;
			}
		}

		var telefono = $.trim(p.telefono || '');
		if (telefono === '') {
			soltarYSalir('El cliente de este pedido no tiene telefono registrado; no se puede retomar aqui.');
			return;
		}
		$('#telcelular').val(telefono);
		$('#telefono').val(telefono);

		// Se busca al cliente igual que cuando se digita el telefono, y se selecciona la fila de ESTE cliente:
		// el clic en la fila es lo que llena el formulario y carga el catalogo de la tienda.
		$.getJSON(server + 'GetCliente?telefono=' + encodeURIComponent(telefono), function (clientes) {
			var dt = $('#grid-clientes').DataTable();
			dt.clear();
			var fila = -1;
			for (var k = 0; k < clientes.length; k++) {
				dt.row.add(clientes[k]);
				if (String(clientes[k].idCliente) === String(p.idcliente)) {
					fila = k;
				}
			}
			dt.draw();
			if (fila < 0) {
				soltarYSalir('No se encontro al cliente de este pedido (puede estar inactivo). Revise el cliente primero.');
				return;
			}
			// Despues de draw() las filas pueden haberse reordenado: se busca la del cliente por su dato.
			var nodo = null;
			dt.rows().nodes().each(function (n) {
				var d = dt.row(n).data();
				if (d && String(d.idCliente) === String(p.idcliente)) {
					nodo = n;
				}
			});
			$(nodo).trigger('click');
			continuar(p);
		}).fail(function () {
			soltarYSalir('No se pudo consultar al cliente de este pedido.');
		});
	}

	function continuar(p) {
		// El clic en el cliente escoge la tienda que el cliente tiene asignada y carga ESE catalogo. Si el
		// pedido es de otra tienda, manda la del pedido: es donde se va a hacer.
		var $tienda = $('#selectTiendas');
		var idTiendaActual = $tienda.find('option:selected').attr('id');
		if (String(idTiendaActual) !== String(p.idtienda)) {
			$tienda.find('option').each(function () {
				if (String($(this).attr('id')) === String(p.idtienda)) {
					$tienda.val($(this).val());
				}
			});
			getProductosTienda(p.idtienda);
			obtenerHomologacionProductoGaseosa(p.idtienda);
		}

		if (parseInt(p.idtipopedido, 10) > 0) {
			$('#selectTipoPedido option').each(function () {
				if (String($(this).attr('id')) === String(p.idtipopedido)) {
					$('#selectTipoPedido').val($(this).val());
				}
			});
			$('#selectTipoPedido').trigger('change');
		}

		if (p.programado === 'S' && p.horaprogramado) {
			$('#selectHoraPedido option').each(function () {
				if ($(this).val() === p.horaprogramado) {
					$('#selectHoraPedido').val(p.horaprogramado);
				}
			});
		}

		// El estado en que queda la pantalla tras crear el encabezado del pedido (agregarEncabezadoPedido).
		idPedido = p.idpedido;
		idEstadoPedido = 1;
		insertado = 0;
		$('#NumPedido').val(idPedido);
		$('#IdCliente').val(p.idcliente);
		$('#estadopedido').val('En curso');
		$('#selectTiendas').attr('disabled', true);
		$('#selectTipoPedido').attr('disabled', true);
		$('#limpiar').attr('disabled', true);
		$('#transferircliente').attr('disabled', true);

		cargarLineas(p.lineas || []);
		recalcularTotal();
		iniciarRenovacion();

		aviso('Retomando el pedido <strong>' + idPedido + '</strong> de <strong>' + escapar(p.tienda)
			+ '</strong>. Ya tiene los productos de abajo: agregue o quite lo que haga falta y finalice como un'
			+ ' pedido normal. El pedido queda reservado a su nombre mientras trabaja en el.');
	}

	/* Las mismas filas que arma pedidos.js al agregar un producto, para que Eliminar y Duplicar funcionen igual. */
	function cargarLineas(lineas) {
		var grilla = $('#grid-pedido').DataTable();
		grilla.clear();
		var principales = 0;
		for (var i = 0; i < lineas.length; i++) {
			var l = lineas[i];
			var tipo = String(l.tipo || '').toUpperCase();
			var esPizza = tipo === 'PIZZA';
			var esHija = tipo === 'ADICION' || tipo.indexOf('MODIFICADOR') === 0
				|| String(l.observacion || '').indexOf('Producto Incluido-') === 0;
			if (!esHija) {
				principales++;
			}
			grilla.row.add({
				iddetallepedido: l.iddetallepedido,
				numitem: principales,
				pizza: esPizza ? l.nombre : '--',
				otroprod: esPizza ? '--' : l.nombre,
				cantidad: l.cantidad,
				especialidad1: l.especialidad1 || '--',
				especialidad2: l.especialidad2 || '--',
				liquido: '--',
				adicion: l.adicion || '--',
				observacion: l.observacion || '',
				valorunitario: l.valorunitario,
				valortotal: l.valortotal,
				accion: '<button type="button" class="btn btn-danger btn-xs" onclick="eliminarDetallePedido('
					+ l.iddetallepedido + ')"><i class="fas fa-trash-alt fa-2x"></i></button>',
				accion2: esHija ? '' : '<button type="button" class="btn btn-warning btn-xs"'
					+ ' onclick="duplicarDetallePedido(' + l.iddetallepedido
					+ ')"><i class="fas fa-copy fa-2x"></i></button>'
			});
		}
		grilla.draw();
		// contadorItem arranca en 1 y pasa al siguiente numero por cada producto principal agregado.
		contadorItem = principales + 1;
	}

	/* El total sale del servidor, igual que cuando se elimina una linea: nunca se suma en el navegador. */
	function recalcularTotal() {
		$.getJSON(server + 'ObtenerTotalPedido?idpedido=' + idReanudar, function (d) {
			totalpedidogeneral = d[0].valortotal;
			validarDescuentoOferta();
		});
	}

	function iniciarRenovacion() {
		if (temporizador !== null) {
			clearInterval(temporizador);
		}
		temporizador = setInterval(function () {
			// Si la pantalla ya termino con este pedido (se finalizo o se reinicio) no hay nada que renovar.
			if (idPedido !== idReanudar) {
				clearInterval(temporizador);
				temporizador = null;
				return;
			}
			$.ajax({
				url: server + 'EditarPedidoEnCurso',
				type: 'POST',
				dataType: 'json',
				data: { accion: 'renovar', idpedido: idReanudar },
				success: function (r) {
					if (!r || r.ok !== 'S') {
						aviso('<strong>Atencion:</strong> se perdio la reserva del pedido ' + idReanudar
							+ '. Otra persona pudo haberlo tomado. Revise antes de finalizar.');
					}
				}
			});
		}, MS_RENOVACION);
	}
})();

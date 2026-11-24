var server;
var table;

$(document).ready(function() {

	// Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
	table = $('#grid-tiendasBloqueadas').DataTable({
		"aoColumns": [
			{ "mData": "idtienda" },
			{ "mData": "nombre" },
			{ "mData": "canalBadge" },
			{ "mData": "comentario" },
			{ "mData": "fecha" },
			{
				"mData": "accion",
				className: "center",
				defaultContent: ''
			}
		],
		"language": {
			"url": "//cdn.datatables.net/plug-ins/1.10.19/i18n/Spanish.json"
		}
	});
  	
	getListaTiendas();
	pintarTiendasBloqueadas();
	setInterval('validarVigenciaLogueo()', 600000);
});

function toggleSeleccionarTodos(master) {
	var checked = $(master).is(':checked');
	$('.chk-canal').prop('checked', checked);
}

function actualizarEstadoTodos() {
	var totalCanales = $('.chk-canal').length;
	var seleccionados = $('.chk-canal:checked').length;
	$('#chkCanalTodos').prop('checked', totalCanales === seleccionados);
}

function validarVigenciaLogueo()
{
	var respuesta ='';
	$.ajax({ 
		url: server + 'ValidarUsuarioAplicacion', 
		dataType: 'json',
		type: 'post', 
		async: false, 
		success: function(data){
			respuesta = data[0].respuesta;		
		} 
	});
	switch(respuesta)
	{
		case 'OK':
		case 'OKA':
			break;	
		default:
			location.href = server + "Index.html";
			break;
	}
}

function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		var str = '<option value="">-- Seleccione una tienda --</option>';
		for(var i = 0; i < data.length; i++){
			var cadaTienda = data[i];
			str += '<option value="' + cadaTienda.nombre + '" id="' + cadaTienda.id + '">' + cadaTienda.nombre + '</option>';
		}
		$('#tienda').html(str);
		$("#tienda").val('');
	});
}

function obtenerCanalesSeleccionados() {
	if ($('#chkCanalTodos').is(':checked')) {
		return "TODOS";
	}
	var canales = [];
	$('.chk-canal:checked').each(function() {
		canales.push($(this).val());
	});
	return canales.join(',');
}

function guardarTiendaBloqueada()
{
	var idtien = $("#tienda option:selected").attr('id');
	if (!idtien || idtien === "" || idtien === "0") {
		alert("Por favor seleccione una tienda.");
		return;
	}

	var canales = obtenerCanalesSeleccionados();
	if (!canales || canales.trim() === "") {
		alert("Por favor seleccione al menos un canal a bloquear.");
		return;
	}

	var observacion = $('#observacion').val();
	var motivo = encodeURIComponent($("#selectMotivo").val());
	var desbloqueoEn = encodeURIComponent($("#selectDesbloqueoEn").val());
	var canalesEncoded = encodeURIComponent(canales);
	
	$.ajax({ 
		url: server + 'CRUDTiendaBloqueada?idoperacion=1&idtienda=' + idtien + "&observacion=" + encodeURIComponent(observacion) + "&motivo=" + motivo + "&desbloqueoen=" + desbloqueoEn + "&canales=" + canalesEncoded, 
		dataType: 'json', 
		async: false, 
		success: function(data){ 
			
			// Solo si el bloqueo afecta a TODOS o al POS se notifica a la tienda física
			if (canales === "TODOS" || canales.indexOf("POS") !== -1) {
				$.ajax({ 
					url: server + 'ObtenerUrlTienda?idtienda=' + idtien, 
					dataType: 'json',
					type: 'get', 
					async: false, 
					success: function(data3)
					{
						if (data3 && data3.length > 0 && data3[0].urltienda) {
							var urlTienda = data3[0].urltienda;
							$.getJSON(urlTienda + 'BloquearTienda', function(data){
								console.log('Se bloqueo la tienda en el punto de venta local');
							}).fail(function() {
								console.log('No se pudo conectar con el POS local, pero el bloqueo en Central quedo registrado.');
							});
						}
					} 
				});
				alert('Se bloqueo la tienda en el Contact Center y en el punto de venta');
			} else {
				alert('Se bloqueo la tienda exitosamente para los canales seleccionados (' + canales + ')');
			}
		} 
	});

	// Resetear formulario
	$('#tienda').val('');
	$('#observacion').val('');
	$('#chkCanalTodos').prop('checked', true);
	$('.chk-canal').prop('checked', true);
	$('#addData').modal('hide');
	pintarTiendasBloqueadas();
}

function formatearBadgeCanal(canal) {
	if (!canal || canal.toUpperCase() === "TODOS") {
		return '<span class="label label-danger" style="font-size:11px; padding:3px 8px;"><i class="glyphicon glyphicon-ban-circle"></i> TODOS LOS CANALES</span>';
	}
	var c = canal.toUpperCase();
	switch(c) {
		case "BOT":
			return '<span class="label label-primary" style="font-size:11px; padding:3px 8px; background-color:#0275d8;"><i class="glyphicon glyphicon-comment"></i> BOT WHATSAPP</span>';
		case "TIENDA_VIRTUAL":
			return '<span class="label label-success" style="font-size:11px; padding:3px 8px; background-color:#5cb85c;"><i class="glyphicon glyphicon-globe"></i> TIENDA VIRTUAL</span>';
		case "CALL_CENTER":
			return '<span class="label label-warning" style="font-size:11px; padding:3px 8px; background-color:#f0ad4e;"><i class="glyphicon glyphicon-earphone"></i> CALL CENTER</span>';
		case "POS":
			return '<span class="label label-default" style="font-size:11px; padding:3px 8px; background-color:#777;"><i class="glyphicon glyphicon-credit-card"></i> POS (MOSTRADOR)</span>';
		default:
			return '<span class="label label-info" style="font-size:11px; padding:3px 8px;">' + canal + '</span>';
	}
}

function eliminarTiendaBloqueada(idtienda, canal, nombreTienda)
{
	canal = canal || "TODOS";
	nombreTienda = nombreTienda || ("Tienda " + idtienda);
	var mensajeConfirm = 'Desea confirmar el desbloqueo de <strong>' + nombreTienda + '</strong> para el canal: <strong>' + canal + '</strong>?';

	$.confirm({
		'title': 'Confirmación Desbloqueo de Tienda',
		'content': mensajeConfirm,
		'buttons': {
			'Si': {
				'class': 'blue',
				'action': function(){
					$.ajax({ 
						url: server + 'CRUDTiendaBloqueada?idoperacion=3&idtienda=' + idtienda + '&canal=' + encodeURIComponent(canal), 
						dataType: 'json', 
						async: false, 
						success: function(data){ 
							// Si el desbloqueo afecta a TODOS o al POS, notifica a la tienda física
							if (canal === "TODOS" || canal.indexOf("POS") !== -1) {
								$.ajax({ 
									url: server + 'ObtenerUrlTienda?idtienda=' + idtienda, 
									dataType: 'json',
									type: 'get', 
									async: false, 
									success: function(data3)
									{
										if (data3 && data3.length > 0 && data3[0].urltienda) {
											var urlTienda = data3[0].urltienda;
											$.getJSON(urlTienda + 'DesbloquearTienda', function(data){
												console.log('Se desbloqueo la tienda en el punto de venta local');
											}).fail(function() {
												console.log('No se pudo conectar con el POS local.');
											});
										}
									} 
								});
								alert('Se desbloqueo la tienda en el Contact Center y en el punto de venta');
							} else {
								alert('Se desbloqueo la tienda para el canal ' + canal);
							}
							
							pintarTiendasBloqueadas();
						} 
					});
				}
			},
			'No': {
				'class': 'gray',
				'action': function(){}
			}
		}
	});
}

function pintarTiendasBloqueadas()
{
	$.getJSON(server + 'CRUDTiendaBloqueada?idoperacion=5', function(data1){
		table.clear().draw();
		if (!data1) return;
		for(var i = 0; i < data1.length; i++){
			var cada = data1[i];
			var canal = cada.canal || "TODOS";
			var nombreEscapado = (cada.nombre || '').replace(/'/g, "\\'");
			var btnAccion = '<button type="button" class="btn btn-danger btn-xs" onclick="eliminarTiendaBloqueada(' 
				+ cada.idtienda + ', \'' + canal + '\', \'' + nombreEscapado + '\')">'
				+ '<i class="glyphicon glyphicon-remove"></i> Desbloquear</button>';
				
			table.row.add({
				"idtienda": cada.idtienda, 
				"nombre": cada.nombre,
				"canalBadge": formatearBadgeCanal(canal),
				"comentario": cada.comentario,
				"fecha": cada.fecha,  
				"accion": btnAccion
			}).draw(false);
		}
	});
}

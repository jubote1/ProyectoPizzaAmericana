
// Definición de variables
var loc = window.location;
var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
var server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
var respuesta = '';
var usuario = "";
var tiendas;
var table;
var productos;
var memcode = 0;
var idCliente = 0;
var dtconsultasPQRS;
var idSolicitudPQRS;
var imgs;
var fulImgBox;
var fulImg;
var div;
var datosReporte = [];
var fecha_inicial = "";
var fecha_final = "";
// Validar usuario
$.ajax({
	url: server + 'ValidarUsuarioAplicacion',
	dataType: 'json',
	type: 'post',
	async: false,
	success: function(data) {
		respuesta = data[0].respuesta;
		usuario = data[0].nombreusuario;
	}
});


switch (respuesta) {
	case 'OK':
		$('#cargarMenu').load("Menu.html");
		break;

	case 'OKA':
		$('#cargarMenu').load("MenuAdm.html", function () {

			$('#cargarMenu').find('#usuariologin').html(usuario);
		});
		break;

	case 'OKP':
		$('#cargarMenu').load("MenuPQRS.html");
		break;

	default:
		location.href = server + "Index.html";
		break;
}



const opcionesFecha = {
	dateFormat: "d/m/Y",
	locale: "es"
};

flatpickr("#fechainicial, #fechafinal", opcionesFecha);

const picker = flatpickr("#fecha", opcionesFecha);

var g = $("#file-1").fileinput({
	theme: 'fa5',
	uploadUrl: 'http://172.19.0.25:4200/service_upload.php',
	showRemove: false,
	showUpload: false,
	showCancel: false,
	allowedFileExtensions: ['jpg', 'png', 'gif', 'pdf', 'jpeg'],
	overwriteInitial: false,
	maxFilesNum: 10,
	showCaption: false,
	browseClass: "btn btn-danger",
	uploadAsync: true,
	browseLabel: "",
	browseIcon: "<i class='fa fa-plus'></i>",
	fileActionSettings: {
		showUpload: false,
		showZoom: false,
	},
	slugCallback: function(filename) {
		return filename.replace('(', '_').replace(']', '_');
	}

});




// A continuación  la ejecucion luego de cargada la pagina
$(document).ready(function() {
	fulImgBox = document.getElementById("fulImgBox");
	fulImg = document.getElementById("fulImg");
	div = document.getElementById("img-gallery");



	dtconsultasPQRS = $('#grid-consultaPQRS').DataTable({
		"aoColumns": [
			{ "mData": "idconsultaPQRS" },
			{ "mData": "fechasolicitud" },
			{ "mData": "tiposolicitud" },
			{ "mData": "cliente" },
			{ "mData": "direccion" },
			{ "mData": "telefono" },
			{ "mData": "comentario", "visible": false },
			{ "mData": "municipio" },
			{ "mData": "tienda" },
			{ "mData": "nombreorigen" },
			{ "mData": "nombrefoco" },
			{ "mData": "accion" },
			{ "mData": "tipo", "visible": false },
			{ "mData": "arearesponsable", "visible": false },
			{ "mData": "imagenes", "visible": false },


		],
		"fnRowCallback": function(nRow, aData, iDisplayIndex) {
			if (aData.imagenes > 0) {
				$(nRow).css('background-color', '#d8ffcf');
			} else {
			}
		}

	});

	//validamos el contenido del campo fecha del pedido y el evento que lo controlará
	//Se invoca servicio para traerse la información de los productos disponibles en el sistema
	// En resumen se invocan todos servicios que se encargan de llenar la data del formulario.
	getListaTiendas();
	getListaMunicipios();
	getListaOrigenes();
	getListaFocos();
	setInterval('validarVigenciaLogueo()', 600000);

	//Colocamos acción al DataTable en caso de dar clic sobre el DATATABLE
	$('#grid-consultaPQRS tbody').on('click', 'tr', function() {
		datos = table.row(this).data();
		$('#idSolicitudPQRS').val(datos.idconsultaPQRS);
		idSolicitudPQRS = datos.idconsultaPQRS;
		var fechaPQRS = new Date(datos.fechasolicitud + " 12:00:00 GMT-0500");
		picker.setDate(fechaPQRS);
		$("#selectSolicitudpqrs").val(datos.tiposolicitud);
		$("#selectMunicipio").val(datos.municipio);
		$('#telefono').val(datos.telefono);
		$('#nombres').val(datos.nombres);
		$('#apellidos').val(datos.apellidos);
		$('#direccion').val(datos.direccion);
		$("#selectTiendaspqrs").val(datos.tienda);
		$("#selectOrigen").val(datos.nombreorigen);
		$("#selectFoco").val(datos.nombrefoco);
		$('#comentariosVista').val(datos.comentario);
		$('#tipo').val(datos.tipo);
		$('#selectAreaResponsable').val(datos.arearesponsable);
		$('#selectPorcentajeDesc').val(datos.porcentajeDescuento);
		$('#idpedidotienda').val(datos.idpedidotienda);
		$('#idpedidoredencion').val(datos.idpedidoredencion);
		$('#valorPedido').val(datos.valorPedido);
		$('#valorDescuento').val(datos.valorDescuento);
		$('#descuentoRedimido').prop('checked', datos.descuentoRedimido);


		//Posteriormente hacemos la consulta para las imagenes de la pqrs
		$.getJSON(server + 'ConsultarSolicitudPQRSImagenes?idsolicitudpqrs=' + datos.idconsultaPQRS, function(data1) {
			//recibimos respueta que es un json con los nombres de todas las imagenes
			imagenes = data1;
			imgs = new Array(imagenes.length);
			for (var i = 0; i < imagenes.length; i++) {
				var cadaResp = imagenes[i];
				imgs[i] = cadaResp.rutaimagen;
			}
			//Una vez cargadas todas las imagenes realizamos la carga de las mismas
			agregarImagen();
		});
	});



});


function validarVigenciaLogueo() {
	var d = new Date();

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
	switch (respuesta) {
		case 'OK':
			break;
		case 'OKA':
			break;
		default:
			location.href = server + "Index.html";
			break;
	}

}

// Método que se encarga luego de introducido un teléfono en el campo de teléfono del cliente llamar al servicio


// Método que invoca el servicio para listar las tiendas donde se pondrán tomar domicilios.
function getListaTiendas() {
	$.getJSON(server + 'GetTiendas', function(data) {
		tiendas = data;
		var str = '';
		let placeholder = `<option value="">Seleccionar...</option>`;
		for (var i = 0; i < data.length; i++) {
			var cadaTienda = data[i];
			str += '<option value="' + cadaTienda.nombre + '" id ="' + cadaTienda.id + '">' + cadaTienda.nombre + '</option>';
		}
		$('#selectTiendaspqrs').html(placeholder + str);
		str += '<option value="' + 'TODAS' + '" id ="' + 'TODAS' + '"  selected>' + 'TODAS' + '</option>';
		$('#selectTiendas').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		//$("#selectTiendas").val('');
	});
}




function existeFecha(fecha) {
	var fechaf = fecha.split("/");
	var day = fechaf[0];
	var month = fechaf[1];
	var year = fechaf[2];
	var date = new Date(year, month, '0');
	if ((day - 0) > (date.getDate() - 0)) {
		return false;
	}
	return true;
}

function validarFechas(date1, date2) {

	var fechaSolIni = new Date();
	var fechaSolFin = new Date();
	var fecha1 = date1.split("/");
	var fecha2 = date2.split("/");
	fechaSolIni.setFullYear(fecha1[2], fecha1[1] - 1, fecha1[0]);
	fechaSolFin.setFullYear(fecha2[2], fecha2[1] - 1, fecha2[0]);

	if (fechaSolIni <= fechaSolFin) {
		return true;
	}
	else {
		return false;
	}
}




function consultarPQRS() {

	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var tienda = $("#selectTiendas option:selected").attr('id');
	var valida = ValidacionesDatos();
	var tipoSolicitud = $("#selectSolicitud option:selected").val()
	if (tipoSolicitud == "todos") {
		tipoSolicitud = "";
	}
  
	var filtrodescuentoRed = document.getElementById("filtrodescuentoRed").checked;

	if (valida != 1) {
		return;
	}
	// Si pasa a este punto es porque paso las validaciones
	if ($.fn.dataTable.isDataTable('#grid-consultaPQRS')) {
		table = $('#grid-consultaPQRS').DataTable();

	}
	$.getJSON(server + 'ConsultaIntegradaSolicitudesPQRS?fechainicial=' + fechaini + "&fechafinal=" + fechafin + "&tienda=" + tienda + "&tiposolicitud=" + tipoSolicitud + "&descuentoredimido=" + filtrodescuentoRed, function(data1) {
	
		datosReporte = data1;
		fecha_inicial = fechaini;
		fecha_final = fechafin;
		table.clear().draw();
		for (var i = 0; i < data1.length; i++) {
			table.row.add({
				"idconsultaPQRS": data1[i].idconsultaPQRS,
				"fechasolicitud": data1[i].fechasolicitud,
				"tiposolicitud": data1[i].tiposolicitud,
				"cliente": data1[i].cliente,
				"direccion": data1[i].direccion,
				"telefono": data1[i].telefono,
				"comentario": data1[i].comentario,
				"municipio": data1[i].municipio,
				"tienda": data1[i].tienda,
				"nombreorigen": data1[i].nombreorigen,
				"nombrefoco": data1[i].nombrefoco,
				"accion": '<button type="button" class="btn btn-default btn-xs" onclick="mostrarModalObservacion(' + data1[i].idconsultaPQRS + ')"><i class="fas fa-cart-plus fa-2x"></i></button>',
				"tipo": data1[i].tipo,
				"arearesponsable": data1[i].arearesponsable,
				"imagenes": data1[i].imagenes,
				"idpedidotienda": data1[i].idpedidotienda,
				"idpedidoredencion": data1[i].idpedidoredencion,
				"valorPedido": data1[i].valorPedido,
				"valorDescuento": data1[i].valorDescuento,
				"porcentajeDescuento": data1[i].porcentajeDescuento,
				"nombres": data1[i].nombres,
				"apellidos": data1[i].apellidos,
				"descuentoRedimido": data1[i].descuentoRedimido
			}).draw();
		}
	});
	limpiarConsultaPQRS();

}

function validarNumero(input) {
	input.value = input.value.replace(/\D/g, ''); // Solo dígitos
}


function calcularDescuento() {
	const valorPedido = parseFloat(document.getElementById('valorPedido').value);
	const porcentaje = parseFloat(document.getElementById('selectPorcentajeDesc').value);
	const inputDescuento = document.getElementById('valorDescuento');

	if (isNaN(valorPedido)) {
		inputDescuento.value = '';
		return;
	}

	const descuento = valorPedido * (porcentaje / 100);
	inputDescuento.value = Math.round(descuento);
}

function ValidacionesDatos() {
	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var tienda = $("#selectTiendas option:selected").attr('id');
	if (fechaini == '' || fechaini == null) {
		Swal.fire({
			icon: 'warning',
			text: 'La fecha inicial debe ser diferente a vacía'
		});

		return;
	}

	if (fechafin == '' || fechafin == null) {
		Swal.fire({
			icon: 'warning',
			text: 'La fecha final debe ser diferente a vacía'
		});

		return;
	}
	if (existeFecha(fechaini)) {
	}
	else {
		Swal.fire({
			icon: 'error',
			text: 'La fecha inicial no es correcta'
		});
		return;
	}

	if (existeFecha(fechafin)) {
	}
	else {
		Swal.fire({
			icon: 'error',
			text: 'La fecha final no es correcta'
		});

		return;
	}
	if (validarFechas(fechaini, fechafin)) {
	}
	else {
		Swal.fire({
			icon: 'warning',
			text: 'La fecha inicial es mayor a la fecha final, favor corregir'
		});

		return;
	}

	if (tienda == '' || tienda == null) {
		Swal.fire({
			icon: 'error',
			text: 'La tienda no puede estar vacía'
		});
		return;
	}

	return (1);
}


function mostrarModalObservacion(idSolPQRS) {
	idSolicitudPQRS = idSolPQRS;
	$('#adicionarObservacion').modal('show');
}


function guardarObservacionPQRS() {
	var comentariosAd = encodeURIComponent($('#comentarios').val());
	if (comentariosAd == '') {
		Swal.fire({
			icon: 'warning',
			text: 'El comentario a adicionar debe tener algún texto.'
		});

		return;
	}
	var controlador = false;
	$.ajax({
		url: server + 'AdicionarComentarioPQRS?pqrs=' + idSolicitudPQRS + "&comentario=" + comentariosAd,
		dataType: 'json',
		async: false,
		success: function(data) {
			var resultado = data;
			if (resultado[0].resultado == 'OK') {
				Swal.fire({
					icon: 'success',
					text: 'Se ha insertado exitosamente el comentario.'
				});

				controlador = true;
			} else {
				Swal.fire({
					icon: 'error',
					text: 'Se tuvo un ERROR al insertar el comentario'
				});

			}
		}
	});
	if (controlador == true) {
		$('#comentarios').val('');
		idSolicitudPQRS = 0;
		$('#adicionarObservacion').modal('hide');
		consultarPQRS();
	}

}


function limpiarConsultaPQRS() {
	$('#idSolicitudPQRS').val('');
	$('#fecha').val('');
	$('#tipoSolicitud').val('');
	$('#telefono').val('');
	$('#nombres').val('');
	$('#direccion').val('');
	$('#municipio').val('');
	$("#tienda").val('');
	$("#selectOrigen").val('');
	$('#comentariosVista').val('');
	$('#descuentoRedimido').prop('checked', false);
	$(' #selectMunicipio, #selectAreaResponsable, #selectPorcentajeDesc').prop('selectedIndex', 0);
	$(' #apellidos, #zona, #valorPedido, #idpedidotienda, #idpedidoredencion , #valorDescuento,#selectSolicitudpqrs, #selectFoco,#selectTiendaspqrs').val("");
}

function agregarImagen() {

	$('#img-gallery').html('');
	imgs.forEach(item => {
		elemento = null;
		//validamos si el archivo es pdf
		if (item.includes('.pdf')) {
			elemento = document.createElement('div');
			embed = document.createElement('embed');
			a = document.createElement('a');
			src = "http://172.19.0.25:4200/imagenes/" + item
			embed.src = src;
			embed.width = '320px';
			embed.height = '350px';
			a.href = src
			a.innerHTML = "<br><strong>Ver documento completo</strong>"
			a.target = "_blank"
			embed.style.display = 'block';
			embed.className = "file"
			elemento.appendChild(embed)
			elemento.appendChild(a);

		} else {
			elemento = document.createElement('img');
			elemento.className = "file"
			src = "http://172.19.0.25:4200/imagenes/" + item
			elemento.src = src;
			elemento.onclick = function() { openFulImg(this.src) };

		}

		div.appendChild(elemento);
	});
}


function openFulImg(reference) {
	fulImgBox.style.display = "flex";
	fulImg.src = reference
}

function closeImg() {
	fulImgBox.style.display = "none";
}

//Método que invoca el servicio para obtener lista de municipios parametrizados en el sistema
function getListaMunicipios() {

	$.getJSON(server + 'CRUDMunicipio?idoperacion=5', function(data) {

		var str = '';
		for (var i = 0; i < data.length; i++) {
			var cadaMunicipio = data[i];
			str += '<option value="' + cadaMunicipio.nombre + '" id ="' + cadaMunicipio.idmunicipio + '">' + cadaMunicipio.nombre + '</option>';
		}
		$('#selectMunicipio').html(str);
	});

}


function getListaOrigenes() {
	$.getJSON(server + 'CRUDOrigenPqrs?idoperacion=5', function(data) {
		var origenes = data;
		var str = '';
		let placeholder = `<option value="">Seleccionar...</option>`;
		for (var i = 0; i < data.length; i++) {
			var cadaOrigen = data[i];
			str += '<option value="' + cadaOrigen.nombreorigen + '" id ="' + cadaOrigen.idorigen + '">' + cadaOrigen.nombreorigen + '</option>';
		}
		$('#selectOrigen').html(placeholder + str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectOrigen").val('');
	});
}

function getListaFocos() {
	$.getJSON(server + 'CRUDFocoPqrs?idoperacion=5', function(data) {
		var str = '';
		let placeholder = `<option value="">Seleccionar...</option>`;
		for (var i = 0; i < data.length; i++) {
			var cadaFoco = data[i];
			str += '<option value="' + cadaFoco.nombrefoco + '" id ="' + cadaFoco.idfoco + '">' + cadaFoco.nombrefoco + '</option>';
		}
		$('#selectFoco').html(placeholder + str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectFoco").val('');
	});
}

function AdicionarImagenesPQRS() {
	if (idSolicitudPQRS == 0) {
		Swal.fire({
			icon: 'warning',
			text: 'Debe haber una PQRS seleccionada para agregar las imagenes o archivos.'
		});

	} else {
		var filestack = $('#file-1').fileinput('getFileList');
		const fd = new FormData();

		//Se guarda un array con las imagenes dentro de los datos del formulario
		filestack.forEach(element => {
			fd.append('files[]', element);
		});
		$.ajax(
			{
				url: 'http://172.19.0.25:4200/service_upload.php',
				method: 'POST',
				data: fd,
				dataType: "json",
				cache: false,
				contentType: false,
				processData: false,
				async: false,
				success: function(resp) {
					console.log(resp);
					//En este punto deberemos insertar las imágenes
					for (var i = 0; i < resp.length; i++) {
						var cadaResp = resp[i];
						console.log(cadaResp.name);
						$.ajax({
							url: server + 'InsertarSolicitudPQRSImagenes?idsolicitudpqrs=' + idSolicitudPQRS + '&rutaimagen=' + cadaResp.name,
							dataType: 'json',
							type: 'post',
							async: false,
							success: function(data) {

							}
						});
						$('#file-1').fileinput('reset');
					}
					refrescarImagenes();
				}
			});
	}
}

function refrescarImagenes() {
	$.getJSON(server + 'ConsultarSolicitudPQRSImagenes?idsolicitudpqrs=' + idSolicitudPQRS, function(data1) {
		//recibimos respueta que es un json con los nombres de todas las imagenes
		imagenes = data1;
		imgs = new Array(imagenes.length);
		for (var i = 0; i < imagenes.length; i++) {
			var cadaResp = imagenes[i];
			imgs[i] = cadaResp.rutaimagen;
		}
		//Una vez cargadas todas las imagenes realizamos la carga de las mismas
		agregarImagen();
	});
}

function EditarPQRS() {
	if (idSolicitudPQRS == 0) {
		Swal.fire({
			icon: 'warning',
			text: 'No se ha seleccionado ninguna PQRS para Modificar'
		});

	} else {
		var fechaSolicitud = $("#fecha").val();
		var tipoSolicitud = $("#selectSolicitudpqrs option:selected").val();
		//idCliente
		var tempTienda = $("#selectTiendaspqrs option:selected").attr('id');
		var idOrigen = $("#selectOrigen option:selected").attr('id');
		var idFoco = $("#selectFoco option:selected").attr('id');
		var nombresEncode = $("#nombres").val();
		var apellidosEncode = $("#apellidos").val();
		var tel = $("#telefono").val();
		var direccionEncode = $("#direccion").val();
		var zonaEncode = $("#zona").val();
		var tempMunicipio = $("#selectMunicipio option:selected").attr('id');
		var comentarioEncode = $("#comentariosVista").val();
		var tipo = $("#selectTipo option:selected").val();
		var areaResponsable = $("#selectAreaResponsable option:selected").val();
		var idpedidotienda = $("#idpedidotienda").val();
		var idpedidoredencion = $("#idpedidoredencion").val();
		var valorPedido = $("#valorPedido").val();
		var valorDescuento = $("#valorDescuento").val();
		var porcentajeDescuento = $("#selectPorcentajeDesc option:selected").val();
		var descuentoRedimido = document.getElementById("descuentoRedimido").checked;

		Swal.fire({
			title: 'Confirmacion Actualización',
			text: 'Desea confirmar la actualización de la Solicitud PQRS.',
			icon: 'warning',
			showCancelButton: true,
			confirmButtonText: 'Sí',
			cancelButtonText: 'No',
			confirmButtonColor: 'blue', // Personaliza el color del botón de "Sí"
			cancelButtonColor: 'gray', // Personaliza el color del botón de "No"
		}).then((result) => {
			if (result.isConfirmed) {
				$.ajax(
					{
						url: server + 'ActualizarSolicitudPQRS',
						dataType: 'json',
						type: 'post',
						data:
						{
							'fechasolicitud': fechaSolicitud,
							'tiposolicitud': tipoSolicitud,
							'idcliente': idCliente,
							'idtienda': tempTienda,
							'nombres': nombresEncode,
							'apellidos': apellidosEncode,
							'telefono': tel,
							'direccion': direccionEncode,
							'zona': zonaEncode,
							'idmunicipio': tempMunicipio,
							'comentario': comentarioEncode,
							'idorigen': idOrigen,
							'idfoco': idFoco,
							'tipo': tipo,
							'arearesponsable': areaResponsable,
							'idsolicitudpqrs': idSolicitudPQRS,
							'idpedidotienda': idpedidotienda,
							'idpedidoredencion': idpedidoredencion,
							'valorPedido': valorPedido,
							'valorDescuento': valorDescuento,
							'porcentajeDescuento': porcentajeDescuento,
							'descuentoRedimido': descuentoRedimido
						},
						async: false,
						success: function(data1) {
							var respuesta = data1[0];
							if (respuesta.idSolicitudPQRS > 0) {
								Swal.fire({
									icon: 'success',
									text: 'Se ha actualizado correctamente la solicitud PQRS número  ' + respuesta.idSolicitudPQRS
								});

								limpiarPQRS()
							}
						}
					});
			}
		});


	}
}

function DescartarPQRS() {
	if (idSolicitudPQRS == 0) {
		Swal.fire({
			icon: 'error',
			text: 'No se ha seleccionado ninguna PQRS para Descartar'
		});
	} else {

		Swal.fire({
			title: 'Confirmacion Descartar',
			text: 'Desea confirmar que se va a descartar de la Solicitud PQRS.',
			icon: 'warning',
			showCancelButton: true,
			confirmButtonText: 'Sí',
			cancelButtonText: 'No',
			confirmButtonColor: 'blue', // Personaliza el color del botón de "Sí"
			cancelButtonColor: 'gray', // Personaliza el color del botón de "No"
		}).then((result) => {
			if (result.isConfirmed) {
				$.ajax(
					{
						url: server + 'DescartarSolicitudPQRS',
						dataType: 'json',
						type: 'post',
						data:
						{
							'idsolicitudpqrs': idSolicitudPQRS
						},
						async: false,
						success: function(data1) {
							var respuesta = data1[0];
							if (respuesta.idSolicitudPQRS > 0) {
								Swal.fire({
									icon: 'success',
									text: 'Se ha descartado correctamente la solicitud PQRS número  ' + respuesta.idSolicitudPQRS
								});

								limpiarPQRS()
							}
						}
					});
			}
		});

	}
}


function limpiarPQRS() {
	// Limpiar campos de texto y numéricos
	$(' #selectMunicipio, #selectAreaResponsable, #selectPorcentajeDesc').prop('selectedIndex', 0);
	$('#telefono, #nombres, #apellidos, #direccion, #zona, #comentariosVista, #valorPedido, #idpedidotienda,#idpedidoredencion, #valorDescuento').val("");
	// Reiniciar selects a la primera opción
	$('#selectTiendaspqrs, #selectOrigen , #selectFoco , #selectMunicipio, #selectSolicitudpqrs, #selectAreaResponsable').val("");

	$('#descuentoRedimido').prop('checked', false);

	// Limpiar DataTable si existe
	if ($.fn.dataTable.isDataTable('#grid-consultaPQRS')) {
		const table = $('#grid-consultaPQRS').DataTable();
		table.clear().draw();
	}

}

async function generarReporte() {
	const filtrados = datosReporte.filter(d => d.valorDescuento > 0);
	if (filtrados.length === 0) {
		Swal.fire({
			icon: 'warning',
			text: "No hay datos para generar el reporte."
		});
		return;
	}

	const agrupados = {};
	filtrados.forEach(d => {
		const tienda = d.tienda || 'Sin Tienda';
		if (!agrupados[tienda]) agrupados[tienda] = [];
		agrupados[tienda].push(d);
	});

	const workbook = new ExcelJS.Workbook();
	const worksheet = workbook.addWorksheet("Reporte");

	let fila = 1;
	let totalGlobal = 0;
	let totalGlobalRedimido = 0;
	let totalGlobalRedimidos = 0;
	let totalGlobalRegistros = 0;

	Object.entries(agrupados).forEach(([tienda, registros]) => {
		worksheet.mergeCells(`A${fila}:G${fila}`);
		const celdaTitulo = worksheet.getCell(`A${fila}`);
		celdaTitulo.value = `Tienda:  ${tienda}`;
		celdaTitulo.font = { bold: true, size: 12, color: { argb: 'FFFFFFFF' } };
		celdaTitulo.fill = {
			type: 'pattern',
			pattern: 'solid',
			fgColor: { argb: '0d2273' }
		};
		celdaTitulo.alignment = { horizontal: 'center', vertical: 'middle' };
		fila++;

		const headers = ['#', 'Factura', 'Valor Pedido', 'Porcentaje Descuento', 'Valor Descuento', 'Redimido', 'Factura Redención'];
		headers.forEach((h, i) => {
			const cell = worksheet.getCell(fila, i + 1);
			cell.value = h;
			cell.font = { bold: true };
			cell.fill = {
				type: 'pattern',
				pattern: 'solid',
				fgColor: { argb: 'FFBDD7EE' }
			};
			cell.border = { top: { style: 'thin' }, left: { style: 'thin' }, bottom: { style: 'thin' }, right: { style: 'thin' } };
			cell.alignment = { horizontal: 'center' };
		});
		fila++;

		let subtotal = 0;
		let subtotalRedimido = 0;
		let totalRedimidos = 0;

		registros.forEach(r => {
			const redimido = r.descuentoRedimido === true;
			const valores = [
				r.idconsultaPQRS,
				r.idpedidotienda,
				r.valorPedido,
				r.porcentajeDescuento > 0 ? r.porcentajeDescuento + '%' : 'No Aplica',
				r.valorDescuento,
				redimido ? 'Sí' : 'No',
				r.idpedidoredencion > 0 ? r.idpedidoredencion : "No existe"
			];

			valores.forEach((val, i) => {
				const cell = worksheet.getCell(fila, i + 1);
				cell.value = val;
				cell.border = { top: { style: 'thin' }, left: { style: 'thin' }, bottom: { style: 'thin' }, right: { style: 'thin' } };
				cell.alignment = { horizontal: 'center' };
				
				// Aquí aplicamos el color amarillo muy claro si redimido es true
							if (redimido) {
								cell.fill = {
									type: 'pattern',
									pattern: 'solid',
									fgColor: { argb: 'FFFFF9C4' } // Amarillo muy claro
								};
							}
			});

			subtotal += r.valorDescuento;
			if (redimido) {
				subtotalRedimido += r.valorDescuento;
				totalRedimidos += 1;
			}
			fila++;

			totalGlobal += r.valorDescuento;
			if (redimido) {
				totalGlobalRedimido += r.valorDescuento;
				totalGlobalRedimidos += 1;
			}
			totalGlobalRegistros += 1;
		});

		const totalRegistros = registros.length;
		const porcentajeRedencion = totalRegistros > 0 ? (totalRedimidos / totalRegistros) * 100 : 0;

		fila++; // Fila vacía

		const totales = [
			['TOTAL TIENDA', subtotal],
			['TOTAL REDIMIDO', subtotalRedimido],
			['PORCENTAJE REDENCIÓN (%)', porcentajeRedencion.toFixed(2)]
		];

		totales.forEach(([label, valor]) => {
			const cellLabel = worksheet.getCell(fila, 4);
			const cellValor = worksheet.getCell(fila, 5);
			cellLabel.value = label;
			cellLabel.font = { bold: true };
			cellValor.value = valor;
			cellValor.font = { bold: true };
			cellValor.alignment = { horizontal: 'center' };
			
			
			fila++;
		});

		fila++; // Separador
	});

	// Resumen global al lado (columna I)
	const resumenGlobal = [
		['Total General Tiendas', totalGlobal],
		['Total General Redimido', totalGlobalRedimido],
		['Porcentaje Redención Global (%)', totalGlobalRegistros > 0 ? ((totalGlobalRedimidos / totalGlobalRegistros) * 100).toFixed(2) : '0.00']
	];

	let filaResumen = 6; // Puedes ajustar si quieres que empiece más abajo
	resumenGlobal.forEach(([label, valor]) => {
		const celdaLabel = worksheet.getCell(filaResumen, 9); // Columna I
		const celdaValor = worksheet.getCell(filaResumen, 10); // Columna J

		celdaLabel.value = label;
		celdaLabel.font = { bold: true, size :12 , color: { argb: 'FFFFFFFF' } };
		celdaLabel.fill = {
			type: 'pattern',
			pattern: 'solid',
			fgColor: { argb: '0d2273' } // Mismo fondo que encabezados
		};
		celdaLabel.border = {
			top: { style: 'thin' },
			left: { style: 'thin' },
			bottom: { style: 'thin' },
			right: { style: 'thin' }
		};


		celdaValor.value = valor;
		celdaValor.font = { bold: true, color: { argb: 'FF000000' } };
		celdaValor.fill = {
			type: 'pattern',
			pattern: 'solid',
			fgColor: { argb: 'FFFFFFFF' }
		};
		celdaValor.border = {
			top: { style: 'thin' },
			left: { style: 'thin' },
			bottom: { style: 'thin' },
			right: { style: 'thin' }
		};


		celdaLabel.alignment = { horizontal: 'left', vertical: 'middle' };
		celdaValor.alignment = { horizontal: 'center', vertical: 'middle' };

		filaResumen++;
	});


	worksheet.columns.forEach(col => {
		let maxLength = 0;
		col.eachCell({ includeEmpty: true }, cell => {
			const val = cell.value ? cell.value.toString() : '';
			maxLength = Math.max(maxLength, val.length);
		});
		col.width = maxLength + 2;
	});
	worksheet.getColumn(8).width = 10;


    const buffer = await workbook.xlsx.writeBuffer();
	const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
	saveAs(blob, "PQRS_Descuentos_" + fecha_inicial + "_Al_" + fecha_final + ".xlsx");
}











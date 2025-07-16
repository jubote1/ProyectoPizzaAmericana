
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
const historialContainer = document.getElementById("historialComentarios");
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
		$('#cargarMenu').load("MenuAdm.html", function() {

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
			{ "mData": "tienda" },
			{ "mData": "nombreorigen" },
			{ "mData": "nombrefoco" },
			{
				"mData": "nombreEstado",
				"render": function(data, type, row) {

					return data ? data : "Ninguno";
				}
			},
			{
				"mData": null,
				"sTitle": "Archivo",
				"orderable": false,
				"searchable": false,
				"render": function(data, type, row) {
					if (row.imagenes > 0) {
						return '<center><i class="fa fa-check text-success" title="Tiene imágenes"></i></center>';
					} else {
						return '<center><i class="fa fa-times text-danger" title="Sin imágenes"></i></center>';
					}
				}
			},
			{ "mData": "tipo", "visible": false },
			{ "mData": "arearesponsable", "visible": false },
			{ "mData": "imagenes", "visible": false }


		],
		"fnRowCallback": function(nRow, aData, iDisplayIndex) {
			// Remueve cualquier clase previa de color
			$(nRow).removeClass('resaltar-amarillo resaltar-azul resaltar-naranja resaltar-verde');

			switch (aData.idestado) {
				case 1:
					$(nRow).addClass('resaltar-amarillo');
					break;
				case 2:
					$(nRow).addClass('resaltar-azul');
					break;
				case 3:
					$(nRow).addClass('resaltar-naranja');
					break;
				case 4:
					$(nRow).addClass('resaltar-verde');
					break;
				default:
					// Si es 0, null, undefined u otro valor no esperado, no aplicar color
					break;
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
	getUsuariosActivos();
	getEstadoPqrs();
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
		$('#selectTipo').val(datos.tipo);
		$('#selectAreaResponsable').val(datos.arearesponsable);
		$('#selectPorcentajeDesc').val(datos.porcentajeDescuento);
		$('#idpedidotienda').val(datos.idpedidotienda);
		$('#idpedidoredencion').val(datos.idpedidoredencion);
		$('#valorPedido').val(datos.valorPedido);
		$('#valorDescuento').val(datos.valorDescuento);
		$('#descuentoRedimido').prop('checked', datos.descuentoRedimido);
		$('#selectEstado').val(datos.idestado);
		if(datos.idestado == 3)
		{
			$('#escalar').attr('disabled', false);
		}else
		{
			$('#escalar').attr('disabled', true);
		}
		seleccionarOpcionSeguro('#selectUsuarioRegistro', datos.idusuarioRegistro);
		seleccionarOpcionSeguro('#selectUsuarioRedencion', datos.idusuarioRedencion);

		['#selectEstado', '#selectUsuarioRegistro', '#selectUsuarioRedencion'].forEach(id => {
		  const $select = $(id);
		  if ($select.val() === "0") {
		    $select.addClass("placeholder");
		  } else {
		    $select.removeClass("placeholder");
		  }
		});

		
		
		historialContainer.innerHTML = '';
		const listaComentarios = datos.listaComentarios;


		// Cargar comentarios iniciales
		Object.keys(listaComentarios).forEach(fecha => {
			listaComentarios[fecha].forEach(com => {
				agregarComentarioVisual(fecha, com.id, com.comentario);
			});
		});



		//Posteriormente hacemos la consulta para las imagenes de la pqrs
		$.getJSON(server + 'ConsultarSolicitudPQRSImagenes?idsolicitudpqrs=' + datos.idconsultaPQRS, function(data1) {
			//recibimos respueta que es un json con los nombres de todas las imagenes
			imagenes = data1;
			imgs = new Array(imagenes.length);
			for (var i = 0; i < imagenes.length; i++) {
				var cadaResp = imagenes[i];
				imgs[i] = cadaResp
			}
			//Una vez cargadas todas las imagenes realizamos la carga de las mismas
			agregarImagen();
		});
	});



});

function seleccionarOpcionSeguro(selector, valor) {
	const $select = $(selector);
	if ($select.find(`option[value="${valor}"]`).length > 0) {
		$select.val(valor);
	} else {
		$select.val('0');
	}
}



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



function escalarPQRS()
{
	$('#modalescalarpqrs').modal('show');
}


function consultarPQRS() {

	$('#escalar').attr('disabled', true);
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
				"municipio": data1[i].municipio,
				"tienda": data1[i].tienda,
				"nombreorigen": data1[i].nombreorigen,
				"nombrefoco": data1[i].nombrefoco,
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
				"descuentoRedimido": data1[i].descuentoRedimido,
				"listaComentarios": data1[i].listaComentarios,
				"idusuarioRegistro": data1[i].idusuarioRegistro,
				"idusuarioRedencion": data1[i].idusuarioRedencion,
				'idestado': data1[i].idestado,
				'nombreEstado': data1[i].nombreEstado
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
	const fechaini = $("#fechainicial").val();
	const fechafin = $("#fechafinal").val();
	const tienda = $("#selectTiendas option:selected").attr('id');

	// Validar campos vacíos
	if (!fechaini) {
		return mostrarAlerta('warning', 'La fecha inicial debe ser diferente a vacía');
	}
	if (!fechafin) {
		return mostrarAlerta('warning', 'La fecha final debe ser diferente a vacía');
	}
	if (!tienda) {
		return mostrarAlerta('error', 'La tienda no puede estar vacía');
	}

	// Validar formato de fechas
	if (!existeFecha(fechaini)) {
		return mostrarAlerta('error', 'La fecha inicial no es correcta');
	}
	if (!existeFecha(fechafin)) {
		return mostrarAlerta('error', 'La fecha final no es correcta');
	}

	// Validar rango de fechas
	if (!validarFechas(fechaini, fechafin)) {
		return mostrarAlerta('warning', 'La fecha inicial es mayor a la fecha final, favor corregir');
	}

	return 1; // Todo está correcto
}




function mostrarAlerta(icono, mensaje) {
	Swal.fire({ icon: icono, text: mensaje });
	return; // Para cortar la ejecución del flujo
}



function ValidarDatosActualizados() {

	const errores = [];
	var telefono = document.getElementById("telefono").value;
	var nombres = document.getElementById("nombres").value;
	var direccion = document.getElementById("direccion").value;
	// Validaciones de campos simples
	validarCampoVacio(telefono, "Debe ingresar un teléfono de contacto.");
	if (telefono && !/^\d+$/.test(telefono)) {
		errores.push(`El valor "${telefono}" no es un número válido.`);
	}

	validarCampoVacio(nombres, "Debe ingresar los nombres del cliente.");
	validarCampoVacio(direccion, "Debe ingresar la dirección del cliente.");
	validarCampoVacio($("#selectTiendaspqrs").val(), "Debe seleccionar una tienda.");
	validarCampoVacio($("#selectOrigen").val(), "Debe seleccionar el origen de la PQRS.");
	validarCampoVacio($("#selectFoco").val(), "Debe seleccionar el foco de la PQRS.");
	validarCampoVacio($("#selectMunicipio").val(), "Debe seleccionar el municipio.");
	validarCampoVacio($("#selectSolicitudpqrs").val(), "Debe seleccionar el tipo de solicitud.");
	validarCampoVacio($("#selectAreaResponsable").val(), "Debe seleccionar area responsable.");
	validarCampoVacio($("#selectTipo").val(), "Debe seleccionar tipo de queja.");

	if ($("#selectUsuarioRegistro").val() === "0") {
		errores.push("Debe seleccionar el usuario que registra la PQRS.");
	}

	if ($("#selectEstado").val() === "0") {
		errores.push("Debe seleccionar un estado");
	}

	const comentariosTextArea = historialContainer.querySelectorAll("textarea");
	if (comentariosTextArea.length === 0) {
		errores.push("Debe ingresar un comentario.");
	}

	// Mostrar errores si existen
	if (errores.length > 0) {
		mostrarErrores(errores);
		return;
	}

	return 1;

	// Función auxiliar para validar campos vacíos
	function validarCampoVacio(valor, mensaje) {
		if (!valor) errores.push(mensaje);
	}

	// Función para mostrar errores con SweetAlert
	function mostrarErrores(listaErrores) {
		Swal.fire({
			icon: 'warning',
			title: 'Faltan datos requeridos',
			html: `<ul style="text-align:left;">${listaErrores.map(e => `<li>${e}</li>`).join('')}</ul>`,
			confirmButtonText: 'Entendido',
			confirmButtonColor: 'blue'
		});
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
	$('#descuentoRedimido').prop('checked', false);
	$(' #selectMunicipio, #selectAreaResponsable, #selectPorcentajeDesc,#selectSolicitudpqrs,#selectUsuarioRegistro,#selectUsuarioRedencion,#selectEstado').prop('selectedIndex', 0);
	$(' #apellidos, #zona, #valorPedido, #idpedidotienda, #idpedidoredencion , #valorDescuento, #selectFoco,#selectTiendaspqrs').val("");
	historialContainer.innerHTML = '';

}

function agregarImagen() {
	$('#img-gallery').html('');
	imgs.forEach(item => {
		let container = document.createElement('div');
		container.className = 'img-container';

		let trashButton = document.createElement('button');
		trashButton.className = 'btn-trash';
		trashButton.type = 'button';
		trashButton.title = 'Eliminar archivo';
		trashButton.innerHTML = '<i class="fa fa-trash"></i>';

		trashButton.onclick = function() {
			Swal.fire({
				title: '¿Está seguro?',
				text: "Esta acción eliminará el archivo.",
				icon: 'warning',
				showCancelButton: true,
				confirmButtonText: 'Sí, eliminar',
				cancelButtonText: 'Cancelar'
			}).then((result) => {
				if (result.isConfirmed) {
					EliminarImg(item);
				}
			});
		};

		let elemento;
		let src = "http://172.19.0.25:4200/imagenes/" + item.rutaimagen;

		if (item.rutaimagen.includes('.pdf')) {
			elemento = document.createElement('div');

			let embed = document.createElement('embed');
			embed.src = src;
			embed.width = '320px';
			embed.height = '350px';
			embed.style.display = 'block';
			embed.className = 'file';

			let a = document.createElement('a');
			a.href = src;
			a.target = "_blank";
			a.innerHTML = "<br><strong>Ver documento completo</strong>";

			elemento.appendChild(embed);
			elemento.appendChild(a);
		} else {
			elemento = document.createElement('img');
			elemento.src = src;
			elemento.className = 'file';
			elemento.onclick = function() {
				openFulImg(this.src);
			};
		}

		container.appendChild(trashButton);
		container.appendChild(elemento);
		document.getElementById('img-gallery').appendChild(container);
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
		let placeholder = `<option value="" disabled selected hidden>Seleccione una opción</option>`;
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
		let placeholder = `<option value="" disabled selected hidden>Seleccione una opción</option>`;
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

					//En este punto deberemos insertar las imágenes
					for (var i = 0; i < resp.length; i++) {
						var cadaResp = resp[i];

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
function EliminarImg(item) {
	$.ajax({
		url: 'http://172.19.0.25:4200/delete_upload.php',
		method: 'POST',
		data: JSON.stringify({ filename: item.rutaimagen }),
		dataType: "json",
		contentType: "application/json",
		processData: false,
		cache: false,
		async: false,
		success: function(resp) {
			if (resp.status === "error") {
				Swal.fire({
					icon: 'error',
					text: resp.message
				});
			}

			$.ajax({
				url: server + 'EliminarSolicitudPQRSImagenes?idimagen=' + item.idimagen,
				type: 'POST',
				dataType: 'json',
				async: false,
				success: function(data) {
					if (data.respuesta) {
						const mensaje = (resp.status === "error")
							? "Se eliminó el registro del archivo en la base de datos, pero no se pudo eliminar el archivo físico."
							: "Se eliminó correctamente el archivo.";

						Swal.fire({
							icon: (resp.status === "error") ? 'error' : 'success',
							text: mensaje
						});
					} else {
						Swal.fire({
							icon: 'error',
							text: "No se pudo eliminar el registro del archivo en la base de datos."
						});
					}
				}
			});

			$('#file-1').fileinput('reset');
			refrescarImagenes();
		},
		error: function(xhr, status, error) {
			console.error("Error al eliminar el archivo:", error);
			Swal.fire({
				icon: 'error',
				text: 'Error en la conexión o al procesar la solicitud.'
			});
		}
	});
}





function refrescarImagenes() {
	$.getJSON(server + 'ConsultarSolicitudPQRSImagenes?idsolicitudpqrs=' + idSolicitudPQRS, function(data1) {
		//recibimos respueta que es un json con los nombres de todas las imagenes
		imagenes = data1;
		imgs = new Array(imagenes.length);
		for (var i = 0; i < imagenes.length; i++) {
			var cadaResp = imagenes[i];
			imgs[i] = cadaResp
		}
		//Una vez cargadas todas las imagenes realizamos la carga de las mismas
		agregarImagen();
	});
}

function EditarPQRS() {

	var valido = ValidarDatosActualizados();

	if (valido != 1) {
		return
	}
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
		var tipo = $("#selectTipo option:selected").val();
		var areaResponsable = $("#selectAreaResponsable option:selected").val();
		var idpedidotienda = $("#idpedidotienda").val();
		var idpedidoredencion = $("#idpedidoredencion").val();
		var valorPedido = $("#valorPedido").val();
		var valorDescuento = $("#valorDescuento").val();
		var porcentajeDescuento = $("#selectPorcentajeDesc option:selected").val();
		var descuentoRedimido = document.getElementById("descuentoRedimido").checked;
		var idusuarioRegistro = $("#selectUsuarioRegistro option:selected").val();
		var idusuarioRedencion = $("#selectUsuarioRedencion option:selected").val();
		var idestado = $("#selectEstado option:selected").val();

		const comentarios = historialContainer.querySelectorAll("textarea");


		const listaComentarios = [];

		comentarios.forEach(textarea => {
			const id = parseInt(textarea.getAttribute("data-id") || "0");
			const fecha = textarea.getAttribute("data-fecha");
			const estado = textarea.getAttribute("data-estado");
			const texto = textarea.value.trim();

			if (!texto) return; // Saltar vacíos

			if (estado == "false" && id == "0") {
				return
			}

			listaComentarios.push({ id, comentario: texto, fecha, idSolicitud: idSolicitudPQRS, estado });
		});


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
							'descuentoRedimido': descuentoRedimido,
							'listaComentarios': JSON.stringify(listaComentarios),
							'idusuarioRegistro': idusuarioRegistro,
							'idusuarioRedencion': idusuarioRedencion,
							'idestado': idestado

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
	//$('#selectTipo,#selectMunicipio, #selectAreaResponsable, #selectPorcentajeDesc,#selectSolicitudpqrs,#selectUsuarioRegistro,#selectUsuarioRedencion,#idestado').prop('selectedIndex', 0);
	$('#telefono, #nombres, #apellidos, #direccion, #zona, #valorPedido, #idpedidotienda,#idpedidoredencion, #valorDescuento , #idSolicitudPQRS,#fecha').val("");
	// Reiniciar selects a la primera opción
	$('#selectTiendaspqrs, #selectOrigen , #selectFoco').val("");
	$('select').each(function () {
	  $(this).prop('selectedIndex', 0);

	  if (this.value === "0") {
	    this.classList.add("placeholder");
	  } else {
	    this.classList.remove("placeholder");
	  }
	});

	$('#descuentoRedimido').prop('checked', false);
	historialContainer.innerHTML = '';
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
		celdaLabel.font = { bold: true, size: 12, color: { argb: 'FFFFFFFF' } };
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




function crearGrupoFecha(fecha) {
	const grupo = document.createElement("div");
	grupo.className = "grupo-fecha mb-3";
	grupo.setAttribute("data-fecha", fecha);
	grupo.innerHTML = `
        <h6 class="mb-2">${fecha}</h6>
        <div class="comentarios"></div>
    `;
	historialContainer.appendChild(grupo);
	return grupo.querySelector('.comentarios');
}

function agregarComentarioVisual(fecha, id, texto) {
	let grupo = document.querySelector(`.grupo-fecha[data-fecha='${fecha}'] .comentarios`);
	if (!grupo) {
		grupo = crearGrupoFecha(fecha);
	}

	// Crear contenedor
	const wrapper = document.createElement("div");
	wrapper.className = "position-relative mb-2 comentario-item";

	// Crear textarea
	const textarea = document.createElement("textarea");
	textarea.className = "form-control";
	textarea.rows = 2;
	textarea.value = texto;
	textarea.setAttribute("data-id", id);
	textarea.setAttribute("data-fecha", fecha);
	textarea.setAttribute("data-estado", "true"); // por defecto activo

	// Crear icono eliminar
	const iconoEliminar = document.createElement("span");
	iconoEliminar.innerHTML = "🗑️";
	iconoEliminar.className = "position-absolute";
	iconoEliminar.style.top = "5px";
	iconoEliminar.style.right = "5px";
	iconoEliminar.style.cursor = "pointer";
	iconoEliminar.style.fontSize = "0.8rem";
	iconoEliminar.style.color = "red";
	iconoEliminar.title = "Eliminar comentario";
	iconoEliminar.addEventListener("click", () => {
		const idComentario = parseInt(textarea.getAttribute("data-id") || "0");

		if (idComentario === 0) {
			// Si es nuevo (no guardado en BD), eliminar completamente
			wrapper.remove();
		} else {
			// Si ya existe en BD, marcar como eliminado y ocultar visualmente
			textarea.setAttribute("data-estado", "false");
			textarea.classList.add("d-none");
			iconoEliminar.remove();
		}

		const visibles = grupo.querySelectorAll("textarea:not(.d-none)");
		if (visibles.length === 0) {
			const contenedorGrupo = grupo.closest(".grupo-fecha");
			if (idComentario === 0) {
				// Si todos eran nuevos, eliminar completamente el grupo del DOM
				if (contenedorGrupo) contenedorGrupo.remove();
			} else {
				// Si eran existentes, solo ocultar visualmente
				if (contenedorGrupo) contenedorGrupo.style.display = "none";
			}
		}
	});


	wrapper.appendChild(textarea);
	wrapper.appendChild(iconoEliminar);
	grupo.appendChild(wrapper);
}


// Acción para nuevo comentario
document.getElementById("btnAgregarComentario").addEventListener("click", () => {
	const nuevoTexto = document.getElementById("nuevoComentario").value.trim();
	if (!nuevoTexto) return;

	const hoy = new Date().toISOString().split("T")[0]; // formato YYYY-MM-DD
	agregarComentarioVisual(hoy, 0, nuevoTexto);

	document.getElementById("nuevoComentario").value = "";
});


document.querySelectorAll('select').forEach(select => {
  select.addEventListener("change", () => {
    if (select.value === "0") {
      select.classList.add("placeholder");
    } else {
      select.classList.remove("placeholder");
    }
  });
});

function getUsuariosActivos() {

	fetch(server + 'ObtenerUsuariosActivos')
		.then(res => res.json())
		.then(usuarios => {
			const selectUsuRegistro = document.getElementById("selectUsuarioRegistro");
			const selectUsuRedencion = document.getElementById("selectUsuarioRedencion");
			// Limpiar el select antes de llenarlo
			selectUsuRegistro.innerHTML = '';
			selectUsuRedencion.innerHTML = '';

			// Opción por defecto para select de registro
			const optionDefaultRegistro = document.createElement("option");
			optionDefaultRegistro.textContent = "Seleccionar...";
			optionDefaultRegistro.value = 0;
			selectUsuRegistro.appendChild(optionDefaultRegistro);

			// Opción por defecto para select de redención
			const optionDefaultRedencion = document.createElement("option");
			optionDefaultRedencion.textContent = "Seleccionar...";
			optionDefaultRedencion.value = 0;
			selectUsuRedencion.appendChild(optionDefaultRedencion);

			// Llenar con usuarios activos
			if (usuarios) {
				usuarios.forEach(usuario => {
					// Para select de registro
					const optionRegistro = document.createElement("option");
					optionRegistro.value = usuario.id;
					optionRegistro.textContent = usuario.nombreLargo;
					selectUsuRegistro.appendChild(optionRegistro);

					// Para select de redención
					const optionRedencion = document.createElement("option");
					optionRedencion.value = usuario.id;
					optionRedencion.textContent = usuario.nombreLargo;
					selectUsuRedencion.appendChild(optionRedencion);
				});
			} else {
				Swal.fire({
					icon: 'error',
					text: 'No se encontraron usuarios'
				});

			}
	
			
			if (selectUsuRegistro.value === "0") {
					  selectUsuRegistro.classList.add("placeholder");
					}
					
					if (selectUsuRedencion.value === "0") {
					  selectUsuRedencion.classList.add("placeholder");
					}


		})
		.catch(err => {
			console.error("Error al cargar usuarios activos:", err);
			Swal.fire({
				icon: 'error',
				text: 'No se pudieron cargar los usuarios activos.'
			});
		});
}




function getEstadoPqrs() {
	fetch(server + 'ObtenerEstadoPqrs')
		.then(res => res.json())
		.then(estados => {
			const selectEstado = document.getElementById("selectEstado");
			selectEstado.innerHTML = '';

			// Opción por defecto
			const optionDefault = new Option("Seleccionar...", 0);
			selectEstado.appendChild(optionDefault);
			if (estados) {
				estados.forEach(estado => {
					const option = new Option(estado.descripcion, estado.idestado);
					selectEstado.appendChild(option);
				});


			} else {
				Swal.fire({
					icon: 'error',
					text: 'No se encontraron los estados de Pqrs'
				});

			}
			
			if (selectEstado.value === "0") {
						  selectEstado.classList.add("placeholder");
						} 


		}).catch(err => {
			console.error("Error al cargar los estados de Pqrs:", err);
			Swal.fire({
				icon: 'error',
				text: 'No se pudieron cargar los estados de Pqrs.'
			});
		});
}





// Inicializar datepicker

flatpickr("#fecha", {
	dateFormat: "d/m/Y",
	locale: "es",
	defaultDate: "today",   // Fecha actual
	onChange: function(selectedDates, dateStr, instance) {
		if (!existeFecha(dateStr)) {
			Swal.fire({
				icon: 'error',
				title: 'Fecha incorrecta',
				text: 'La fecha de la solicitud no es correcta'
			});
			instance.setDate(new Date());
			return;
		}

		if (selectedDates.length > 0) {
			const fecha = selectedDates[0];
			const esValida = validarFechaNoMayorAHoy(fecha);
			if (!esValida) {
				Swal.fire({
					icon: 'error',
					title: 'Fecha incorrecta',
					text: 'La fecha de la solicitud no puede ser futura, favor corregir'
				});
				instance.setDate(new Date());
			}
		}
	}
});
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
			// Este callback se ejecuta después de que MenuAdm.html se ha cargado
			console.log("holi: "+usuario)
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



// Configuración de fileinput
$("#file-1").fileinput({
	theme: 'fa5',
	uploadUrl: 'http://172.19.0.25:4200/service_upload.php',
	showRemove: true,
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
		showZoom: false
	},
	slugCallback: filename => filename.replace('(', '_').replace(']', '_')
});

// Al cargar la página
$(document).ready(function() {

	getListaTiendas();
	getListaMunicipios();
	getListaOrigenes();
	getListaFocos();
	setInterval(validarVigenciaLogueo, 600000);

	const table = $('#grid-clientes').DataTable({
		responsive: true,
		autoWidth: false,
		lengthChange: true, // muestra el selector de cantidad de registros
		language: {
			search: "Buscar:",
			lengthMenu: "Mostrar _MENU_ registros por página",
			zeroRecords: "No se encontraron resultados",
			info: "Mostrando _START_ a _END_ de _TOTAL_ registros",
			infoEmpty: "Mostrando 0 registros",
			infoFiltered: "(filtrado de _MAX_ registros totales)",
			paginate: {
				previous: "Anterior",
				next: "Siguiente"
			}
		},
		columns: [
			{ data: "idCliente" },
			{ data: "tienda" },
			{ data: "nombre" },
			{ data: "apellido" },
			{ data: "nombrecompania" },
			{ data: "direccion" },
			{ data: "zona" },
			{ data: "observacion" },
			{ data: "telefono" },
			{ data: "memcode", visible: false }
		]
	});

	$('#grid-clientes tbody').on('click', 'tr', function() {
		const datos = table.row(this).data();
		$('#nombres').val(datos.nombre);
		$('#apellidos').val(datos.apellido);
		$('#direccion').val(datos.direccion);
		$('#zona').val(datos.zona);
		$('#comentarios').val(datos.observacion);
		$("#selectTiendas").val(datos.tienda);
		$("#selectMunicipio").val(datos.municipio || ""); // asegúrate de tener esta columna

		// Opcional si los campos existen
		$('#nombreCompania').val(datos.nombrecompania || "");
		$('#observacionDir').val(datos.observacion || "");

		memcode = datos.memcode;
		idCliente = datos.idCliente;
	});
});

// Validar vigencia de sesión
function validarVigenciaLogueo() {
	$.ajax({
		url: server + 'ValidarUsuarioAplicacion',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(data) {
			let respuesta = data[0].respuesta;
			if (!['OK', 'OKA', 'OKP'].includes(respuesta)) {
				location.href = server + "Index.html";
			}
		}
	});
}

// Validar número de teléfono
function validarTelefono() {
	let tel = $("#telefono").val();
	if (tel.length < 10) {
		Swal.fire({
			icon: 'error',
			title: 'Teléfono incorrecta',
			text: "Teléfono tiene longitud menor a 10"
		});
	}
	if (!/^[0-9]*$/.test(tel)) {
		Swal.fire({
			icon: 'error',
			title: 'Teléfono incorrecta',
			text: `El valor ${tel} no es un número`
		});
	}


	if ($.fn.dataTable.isDataTable('#grid-clientes')) {
		table = $('#grid-clientes').DataTable();
	}

	$.getJSON(server + 'GetCliente?telefono=' + tel, function(data) {
		table.clear().draw();
		data.forEach(cliente => table.row.add(cliente).draw());
	});
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

function getListaTiendas() {
	$.getJSON(server + 'GetTiendas', function(data) {
		let placeholder = `<option value="">Seleccionar...</option>`;
		let str = data.map(t => `<option value="${t.nombre}" id="${t.id}">${t.nombre}</option>`).join('');
		$('#selectTiendas').html(placeholder + str).val('');
	});
}

function getListaOrigenes() {
	let placeholder = `<option value="">Seleccionar...</option>`;
	$.getJSON(server + 'CRUDOrigenPqrs?idoperacion=5', function(data) {
		let str = data.map(o => `<option value="${o.nombreorigen}" id="${o.idorigen}">${o.nombreorigen}</option>`).join('');
		$('#selectOrigen').html(placeholder + str).val('');
	});
}

function getListaFocos() {
	let placeholder = `<option value="">Seleccionar...</option>`;
	$.getJSON(server + 'CRUDFocoPqrs?idoperacion=5', function(data) {
		let str = data.map(f => `<option value="${f.nombrefoco}" id="${f.idfoco}">${f.nombrefoco}</option>`).join('');
		$('#selectFoco').html(placeholder + str).val('');
	});
}

function getListaMunicipios() {
	$.getJSON(server + 'CRUDMunicipio?idoperacion=5', function(data) {
		let str = data.map(m => `<option value="${m.nombre}" id="${m.idmunicipio}">${m.nombre}</option>`).join('');
		$('#selectMunicipio').html(str);
	});
}

// Limpiar cliente
function limpiarSeleccionCliente() {
	// Limpiar campos de texto y numéricos
	$('#telefono, #nombres, #apellidos, #direccion, #zona, #comentarios, #valorPedido, #idpedidotienda, #idpedidoredencion ,#valorDescuento').val("");

	// Reiniciar selects a la primera opción
	$('#selectTiendas, #selectMunicipio, #selectTipo, #selectAreaResponsable, #selectPorcentajeDesc').prop('selectedIndex', 0);
	$('#descuentoRedimido').prop('checked',false);
	// Reiniciar valor del cliente
	idCliente = 0;

	// Limpiar DataTable si existe
	if ($.fn.dataTable.isDataTable('#grid-clientes')) {
		const table = $('#grid-clientes').DataTable();
		table.clear().draw();
	}
}


// Validar fecha
function existeFecha(fecha) {
	let [day, month, year] = fecha.split("/");
	let date = new Date(year, month, 0);
	return (parseInt(day) <= date.getDate());
}

function validarFechaNoMayorAHoy(fechaIngresada) {
	const hoy = new Date();
	const fechaHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());

	// Convertir a fecha sin hora para la comparación
	const f = new Date(fechaIngresada.getFullYear(), fechaIngresada.getMonth(), fechaIngresada.getDate());

	return f <= fechaHoy;
}



// Confirmar PQRS
function ConfirmarPQRS() {
	if (ValidacionesDatos() !== 1) return;

	let data = {
		fechasolicitud: $("#fecha").val(),
		tiposolicitud: $("#selectSolicitud").val(),
		idcliente: idCliente,
		idtienda: $("#selectTiendas option:selected").attr('id'),
		idorigen: $("#selectOrigen option:selected").attr('id'),
		idfoco: $("#selectFoco option:selected").attr('id'),
		nombre: $("#nombres").val(),
		apellido: $("#apellidos").val(),
		telefono: $("#telefono").val(),
		direccion: $("#direccion").val(),
		zona: $("#zona").val(),
		idmunicipio: $("#selectMunicipio option:selected").attr('id'),
		comentario: $("#comentarios").val(),
		tipo: $("#selectTipo").val(),
		arearesponsable: $("#selectAreaResponsable").val(),
		idpedidotienda: $("#idpedidotienda").val(),
		idpedidoredencion: $("#idpedidoredencion").val(),
		valorPedido: $("#valorPedido").val(),
		valorDescuento: $("#valorDescuento").val(),
		porcentajeDescuento: $("#selectPorcentajeDesc").val(),
		descuentoRedimido: document.getElementById("descuentoRedimido").checked
	};
	console.log("datos a insertar:");
	console.log(data);
	Swal.fire({
		title: 'Confirmación de Solicitud PQRS',
		text: '¿Desea confirmar la inserción de la solicitud PQRS?',
		icon: 'warning',
		showCancelButton: true,
		confirmButtonText: 'Sí',
		cancelButtonText: 'No',
		confirmButtonColor: 'blue', // Personaliza el color del botón de "Sí"
		cancelButtonColor: 'gray', // Personaliza el color del botón de "No"
	}).then((result) => {
		if (result.isConfirmed) {
			// Si el usuario hace clic en "Sí"
			$.ajax({
				url: server + 'InsertarSolicitudPQRS',
				dataType: 'json',
				type: 'post',
				data: data,
				success: function(resp) {
					if (resp[0].idSolicitudPQRS != 0) {
						Swal.fire({
							icon: 'success',
							title: 'Éxito',
							text: 'Solicitud ingresada correctamente'
						});
						limpiarSeleccionCliente();
					} else {
						Swal.fire({
							icon: 'error',
							title: 'Error',
							text: "Error al insertar" 
						});
					}
				}
			});
		}
	});

}


function ValidacionesDatos() {
	let errores = [];

	if (!telefono.value) {
		errores.push("Debe ingresar un teléfono de contacto.");
	} else if (!/^\d+$/.test(telefono.value)) {
		errores.push(`El valor "${telefono.value}" no es un número válido.`);
	}

	if (!nombres.value) errores.push("Debe ingresar los nombres del cliente.");
	if (!direccion.value) errores.push("Debe ingresar la dirección del cliente.");
	if (!$("#selectTiendas").val()) errores.push("Debe seleccionar una tienda.");
	if (!$("#selectOrigen").val()) errores.push("Debe seleccionar el origen de la PQRS.");
	if (!$("#selectFoco").val()) errores.push("Debe seleccionar el foco de la PQRS.");
	if (!$("#selectMunicipio").val()) errores.push("Debe seleccionar el municipio.");
	if (!comentarios.value) errores.push("Debe ingresar un comentario.");

	if (errores.length > 0) {
		Swal.fire({
			icon: 'warning',
			title: 'Faltan datos requeridos',
			html: `<ul style="text-align:left;">${errores.map(e => `<li>${e}</li>`).join('')}</ul>`,
			confirmButtonText: 'Entendido',
			confirmButtonColor: 'blue'
						
		});
		return;
	}

	return 1;
}

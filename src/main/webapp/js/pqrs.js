// Inicializar datepicker
const historialContainer = document.getElementById("historialComentarios");
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
	getUsuariosActivos();
	getEstadoPqrs();
	getListaMotivoPrioridad();
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
		let placeholder = `<option value="" disabled selected hidden>Seleccione una opción</option>`;
		let str = data.map(t => `<option value="${t.nombre}" id="${t.id}">${t.nombre}</option>`).join('');
		$('#selectTiendas').html(placeholder + str).val('');
	});
}

function getListaOrigenes() {
	let placeholder = `<option value="" disabled selected hidden>Seleccione una opción</option>`;
	$.getJSON(server + 'CRUDOrigenPqrs?idoperacion=5', function(data) {
		let str = data.map(o => `<option value="${o.nombreorigen}" id="${o.idorigen}">${o.nombreorigen}</option>`).join('');
		$('#selectOrigen').html(placeholder + str).val('');
	});
}

function getListaFocos() {
	let placeholder = `<option value="" disabled selected hidden>Seleccione una opción</option>`;
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

let motivos = [];
let prioridades = [];

function getListaMotivoPrioridad() {
    $.getJSON(server + 'MotivoPrioridadPqrs', function (data) {
        if (data && data.motivo_pqrs && data.prioridad_pqrs) {

            motivos =data.motivo_pqrs
            prioridades = data.prioridad_pqrs
			
			const $selectPrioridad = $('#selectPrioridad');
			      $selectPrioridad.empty().append('<option value="" disabled selected hidden>Seleccione una prioridad</option>');

			      if (prioridades) {
			          prioridades.forEach(p => {
			              $selectPrioridad.append(
			                  $('<option>', { value: p.idprioridad, text: p.descripcion })
			              );
			          });
			      }

				
			
        }
    });
}


// Al cambiar el tipo de solicitud, llenar motivos
$('#selectSolicitud').on('change', function () {
    const tipoSeleccionado = $(this).val();
    const $selectMotivo = $('#selectMotivo');
    $selectMotivo.empty().append('<option value="" disabled selected hidden>Seleccione un motivo</option>');
	const $selectPrioridad = $('#selectPrioridad');
	$selectPrioridad.val(""); 
    motivos
        .filter(m => m.tiposolicitud.toLowerCase() === tipoSeleccionado.toLowerCase())
        .forEach(m => {
            $selectMotivo.append(
                $('<option>', { value: m.idmotivo, text: m.descripcion })
                    .attr('data-prioridad-id', m.idprioridad) // opcional, si quieres guardar relación
            );
        });

});

// Al cambiar el motivo, llenar prioridad
$('#selectMotivo').on('change', function () {
	const $opcionSeleccionada = $(this).find('option:selected'); // opción seleccionada
	const prioridadId = $opcionSeleccionada.data('prioridad-id'); // accede al data attribute
	console.log("prioridadId:", prioridadId);
    const $selectPrioridad = $('#selectPrioridad');
	$selectPrioridad.val(prioridadId); // ← Establece el valor usando jQuery
});

// Limpiar cliente
function limpiarSeleccionCliente() {
	// Limpiar campos de texto y numéricos
	$('#telefono, #nombres, #apellidos, #direccion, #zona, #valorPedido, #idpedidotienda, #idpedidoredencion ,#valorDescuento').val("");

	// Reiniciar selects a la primera opción
	//$('#selectFoco, #selectOrigen , #selectTiendas, #selectMunicipio, #selectTipo, #selectAreaResponsable, #selectPorcentajeDesc,#selectUsuarioRegistro,#selectUsuarioRedencion,#selectEstado,#selectSolicitud').prop('selectedIndex', 0);
	$('select').each(function () {
	  $(this).prop('selectedIndex', 0);

	  if (this.value === "0") {
	    this.classList.add("placeholder");
	  } else {
	    this.classList.remove("placeholder");
	  }
	});
	$('#descuentoRedimido, #ccVinculado').prop('checked', false);
	// Reiniciar valor del cliente
	idCliente = 0;
	historialContainer.innerHTML = '';

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




function ConfirmarPQRS() {

	if (ValidacionesDatos() !== 1) return;

	const comentarios = historialContainer.querySelectorAll("textarea");
	const listaComentarios = [];

	comentarios.forEach(textarea => {
		const id = parseInt(textarea.getAttribute("data-id") || "0");
		const fecha = textarea.getAttribute("data-fecha");
		const estado = textarea.getAttribute("data-estado");
		const texto = textarea.value.trim();

		if (!texto || (estado == "false" && id === 0)) return;

		listaComentarios.push({ id, comentario: texto, fecha, idSolicitud: 0, estado });
	});

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
		tipo: $("#selectTipo").val(),
		arearesponsable: $("#selectAreaResponsable").val(),
		idpedidotienda: $("#idpedidotienda").val(),
		idpedidoredencion: $("#idpedidoredencion").val(),
		valorPedido: $("#valorPedido").val(),
		valorDescuento: $("#valorDescuento").val(),
		porcentajeDescuento: $("#selectPorcentajeDesc").val(),
		descuentoRedimido: document.getElementById("descuentoRedimido").checked,
		listaComentarios: JSON.stringify(listaComentarios),
		idusuarioRegistro: $("#selectUsuarioRegistro").val(),
		idusuarioRedencion: $("#selectUsuarioRedencion").val(),
		idestado: $("#selectEstado").val(),
		idprioridad: $("#selectPrioridad").val(),
		idmotivo: $("#selectMotivo").val(),
		ccVinculado: document.getElementById("ccVinculado").checked
		
	};

	Swal.fire({
		title: 'Confirmación de Solicitud PQRS',
		text: '¿Desea confirmar la inserción de la solicitud PQRS?',
		icon: 'warning',
		showCancelButton: true,
		confirmButtonText: 'Sí',
		cancelButtonText: 'No',
		confirmButtonColor: 'blue',
		cancelButtonColor: 'gray',
	}).then((result) => {
		if (!result.isConfirmed) return;

		$.ajax({
			url: server + 'InsertarSolicitudPQRS',
			dataType: 'json',
			type: 'post',
			data: data,
			contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
			success: function(resp) {
				const idSolicitud = resp[0]?.idSolicitudPQRS || 0;

				if (idSolicitud === 0) {
					return Swal.fire({
						icon: 'error',
						title: 'Error',
						text: "Error al insertar la solicitud."
					});
				}

				const filestack = $('#file-1').fileinput('getFileList');
				const fd = new FormData();
				const erroresImagenes = [];
				console.log(filestack)
				if (filestack.length === 0) {
					// No hay imágenes: mostrar éxito y limpiar
					Swal.fire({
						icon: 'success',
						title: 'Éxito',
						text: 'Solicitud ingresada correctamente.'
					});
					limpiarSeleccionCliente();
					return;
				}


				filestack.forEach(file => fd.append('files[]', file));

				$.ajax({
					url: 'http://172.19.0.25:4200/service_upload.php',
					method: 'POST',
					data: fd,
					dataType: "json",
					cache: false,
					contentType: false,
					processData: false,
					async: false,
					success: function(uploadResp) {
						let total = uploadResp.length;
						let procesados = 0;

						uploadResp.forEach((imgResp, index) => {
							if (!imgResp.name) {
								erroresImagenes.push(`Archivo ${index + 1}: No se recibió nombre del archivo.`);
								procesados++;
								if (procesados === total) mostrarResultadoFinal();
								return;
							}

							$.ajax({
								url: `${server}InsertarSolicitudPQRSImagenes?idsolicitudpqrs=${idSolicitud}&rutaimagen=${imgResp.name}`,
								dataType: 'json',
								type: 'post',
								async: false,
								error: function() {
									erroresImagenes.push(`Error al insertar imagen "${imgResp.name}".`);
								},
								complete: function() {
									procesados++;
									if (procesados === total) mostrarResultadoFinal();
								}
							});
						});

						function mostrarResultadoFinal() {
							$('#file-1').fileinput('reset');

							if (erroresImagenes.length > 0) {
								Swal.fire({
									icon: 'warning',
									title: 'Solicitud registrada con advertencias',
									html: 'La solicitud fue registrada, pero algunas imágenes tuvieron errores:<br><ul>' +
										erroresImagenes.map(e => `<li>${e}</li>`).join('') +
										'</ul>'
								});
							} else {
								Swal.fire({
									icon: 'success',
									title: 'Éxito',
									text: 'Solicitud ingresada correctamente con todas las imágenes.'
								});
							}

							limpiarSeleccionCliente();
						}
					},
					error: function() {
						Swal.fire({
							icon: 'error',
							title: 'Error',
							text: 'Error al subir las imágenes.'
						});
					}
				});
			},
			error: function() {
				Swal.fire({
					icon: 'error',
					title: 'Error',
					text: "Fallo la conexión con el servidor."
				});
			}
		});
	});
}



function ValidacionesDatos() {
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
	validarCampoVacio($("#selectTiendas").val(), "Debe seleccionar una tienda.");
	validarCampoVacio($("#selectOrigen").val(), "Debe seleccionar el origen de la PQRS.");
	validarCampoVacio($("#selectFoco").val(), "Debe seleccionar el foco de la PQRS.");
	validarCampoVacio($("#selectMunicipio").val(), "Debe seleccionar el municipio.");
	validarCampoVacio($("#selectSolicitud").val(), "Debe seleccionar el tipo de solicitud.");
	validarCampoVacio($("#selectAreaResponsable").val(), "Debe seleccionar area responsable.");
	validarCampoVacio($("#selectTipo").val(), "Debe seleccionar tipo de queja.");
	validarCampoVacio($("#selectMotivo").val(), "Debe seleccionar un motivo");
	validarCampoVacio($("#selectPrioridad").val(), "Debe seleccionar una prioridad.");


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
		wrapper.remove();
		const visibles = grupo.querySelectorAll("textarea:not(.d-none)");
		if (visibles.length === 0) {
			const contenedorGrupo = grupo.closest(".grupo-fecha");
			if (contenedorGrupo) contenedorGrupo.remove();
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

			selectUsuRegistro.innerHTML = '';
			selectUsuRedencion.innerHTML = '';

			// Opción por defecto
			const optionDefault = new Option("Seleccionar...", 0);
			selectUsuRegistro.appendChild(optionDefault.cloneNode(true));
			selectUsuRedencion.appendChild(optionDefault.cloneNode(true));
			if (usuarios) {
				usuarios.forEach(usuario => {
					const option = new Option(usuario.nombreLargo, usuario.id);
					selectUsuRegistro.appendChild(option.cloneNode(true));
					selectUsuRedencion.appendChild(option.cloneNode(true));
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


		}).catch(err => {
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



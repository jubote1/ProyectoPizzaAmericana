
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
var dtEscalamientoPQRS;
var idSolicitudPQRS;
var idEscalamientoCon;
var imgs;
var fulImgBox;
var fulImg;
var div;
var datosReporte = [];
var fecha_inicial = "";
var fecha_final = "";
var validarCorreo = true;
var idestadoPqrs = 0;
var fecha_hora_registro = "";
var fecha_hora_cierre = "";
var envioEncuestaPqrs = false;
var AreasEscalamiento = [];
let motivos = [];
let prioridades = [];
const inputCorreo = document.getElementById('correo');
const errorDiv = document.getElementById('errorCorreo');

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


	dtEscalamientoPQRS = $('#grid-escalamientoPQRS').DataTable({
		"aoColumns": [
			{ "mData": "idescalamiento" },
			{ "mData": "idsolicitudpqrs" },
			{ "mData": "arearesponsable" },
			{ "mData": "fechaescalamiento" },
			{ "mData": "fecharesolucion" },
			{ "mData": "solucionado" }
		]
	});

	dtconsultasPQRS = $('#grid-consultaPQRS').DataTable({
		"aoColumns": [
			{ "mData": "idconsultaPQRS" },
			{
				"mData": null,
				"sTitle": "Prioridad",
				"render": function(data, type, row) {
					var p = null;
					prioridades.forEach(item => {
						if(item.idprioridad == row.idprioridad){
						p =item;	
						}
						
					});
					if (!p) return "N/A";

					return `
				<span style="display: inline-flex; align-items: center; gap: 0.4em;">
					<span style="width: 10px; height: 10px; background-color: ${p.color}; border-radius: 50%; display: inline-block;"></span>
					${p.descripcion}
				</span>
			`;
				}
			},
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
	getListaMotivoPrioridad();
	getAreasEscaladas();
	setInterval('validarVigenciaLogueo()', 600000);

	//Colocamos acción al DataTable en caso de dar clic sobre el DATATABLE
	$('#grid-consultaPQRS tbody').on('click', 'tr', function() {

		limpiarPQRS();
		datos = table.row(this).data();
		$('#idSolicitudPQRS').val(datos.idconsultaPQRS);
		idSolicitudPQRS = datos.idconsultaPQRS;
		var fechaPQRS = new Date(datos.fechasolicitud + " 12:00:00 GMT-0500");
		picker.setDate(fechaPQRS);
		$("#selectSolicitudpqrs").val(datos.tiposolicitud).trigger("change");
		$("#selectMunicipio").val(datos.municipio);
		$('#telefono').val(datos.telefono);
		$('#nombres').val(datos.nombres);
		$('#correo').val(datos.correo);
		document.getElementById('correo').dispatchEvent(new Event('input'));
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
		$('#observacionAns').val(datos.observacion_ans);

		idestadoPqrs = datos.idestado;

		$('#zona').val(datos.zona);
		$('#selectPrioridad').val(datos.idprioridad === 0 ? "" : datos.idprioridad);
		$('#selectMotivo').val(datos.idmotivo === 0 ? "" : datos.idmotivo);
		$('#ccVinculado').prop('checked', datos.ccVinculado);
		fecha_hora_registro = datos.fecha_hora_registro || "";
		fecha_hora_cierre = datos.fecha_hora_cierre || "";
		envioEncuestaPqrs = datos.envio_encuesta || false;

		if (datos.idestado == 3) {
			$('#escalar').attr('disabled', false);
		} else {
			$('#escalar').attr('disabled', true);
		}

		seleccionarOpcionSeguro('#selectUsuarioRegistro', datos.idusuarioRegistro);
		seleccionarOpcionSeguro('#selectUsuarioRedencion', datos.idusuarioRedencion);

		['#selectEstado', '#selectUsuarioRegistro', '#selectUsuarioRedencion', "#selectPorcentajeDesc"].forEach(id => {
			const $select = $(id);
			$select.removeClass("placeholder");
			if ($select.val() === "0") {
				$select.addClass("placeholder");
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
		
		var escalamientos = [];
		//Hacemos consulta para llenar los escalamientos
		$.getJSON(server + 'ConsultarEscalamientoPQRS?idsolicitudpqrs=' + datos.idconsultaPQRS, function(data2) {
			dtEscalamientoPQRS.clear().draw();
			escalamientos = data2;
			for (var i = 0; i < data2.length; i++) {
				dtEscalamientoPQRS.row.add({
					"idescalamiento": data2[i].idescalamiento,
					"idsolicitudpqrs": data2[i].idsolicitudpqrs,
					"arearesponsable": data2[i].arearesponsable,
					"fechaescalamiento": data2[i].fechaescalamiento,
					"fecharesolucion": data2[i].fecharesolucion,
					"solucionado": data2[i].solucionado
				}).draw();
			}
			

			const resultado = calcularANSConFallback(
			  escalamientos,
			  AreasEscalamiento,
			  datos.idprioridad,
			  fecha_hora_registro,
			  fecha_hora_cierre,
			  idestadoPqrs
			);
			
			 let colorANS = 'FFFFFFFF'; // blanco por defecto
			if(resultado != null && resultado.colorANS){
					colorANS = resultado.colorANS;
							     
		     }
			 let colorCSS = colorANS.substring(2); // Esto elimina los dos primeros caracteres  
			 $("#horas_transc").html("<strong>Horas hábiles transcurridas:</strong> "+resultado.horasRedondeadas); 
			 $("#estado_ans").html("<strong>Estado ANS:</strong> " + resultado.estadoANS);
			 $("#estado_ans").css({
			   "background-color": `#${colorCSS}`
			 });
		});

	});

	//Click en Grid de Escalamientos
	$('#grid-escalamientoPQRS tbody').on('click', 'tr', function() {

		datos = dtEscalamientoPQRS.row(this).data();
		idEscalamientoCon = datos.idescalamiento;
		if(datos.solucionado == 'NO')
		{
			$('#finalizarescalar').attr('disabled', false);
		}
		else
		{
			$('#finalizarescalar').attr('disabled', true);
		}
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
		strTiendas = '<option value="' + 'TODAS' + '" id ="' + 'TODAS' + '"  selected>' + 'TODAS' + '</option>';
		$('#selectTiendas').html(strTiendas + str);
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



function escalarPQRS() {
	$('#modalescalarpqrs').modal('show');
	$("#titulo_escalamiento").text("Área para Escalar - #" + idSolicitudPQRS);
}

function realizarEscalamientoPQRS() {

	var areaResponsable = encodeURIComponent($("#selectAreaResponsableEscalar option:selected").val());
	if (areaResponsable != "" && idSolicitudPQRS > 0) {
		$.getJSON(server + 'InsertarEscalamientoPQRS?idsolicitudpqrs=' + idSolicitudPQRS + "&arearesponsable=" + areaResponsable, function(data) {
			if (data.resultado == 'OK') {
				mostrarAlerta('success', 'Se realizó el escalamiento de la PQRS al área ' + areaResponsable);
				$('#modalescalarpqrs').modal('hide');

			} else {
				mostrarAlerta('error', 'No se pudo realizar el escalamiento validar con el área de tecnología');
			}

		});
	}
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

	if ($.fn.dataTable.isDataTable('#grid-escalamientoPQRS')) {
		dtEscalamientoPQRS = $('#grid-escalamientoPQRS').DataTable();

	}
	limpiarPQRS();
	$.getJSON(
	  server + 'ConsultaIntegradaSolicitudesPQRS?fechainicial=' + fechaini +
	  "&fechafinal=" + fechafin +
	  "&tienda=" + tienda +
	  "&tiposolicitud=" + tipoSolicitud +
	  "&descuentoredimido=" + filtrodescuentoRed,
	  function(data1) {

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
	        "idestado": data1[i].idestado,
	        "nombreEstado": data1[i].nombreEstado,
	        "idmotivo": data1[i].idmotivo,
	        "idprioridad": data1[i].idprioridad,
	        "zona": data1[i].zona,
	        "ccVinculado": data1[i].ccVinculado,
	        "correo": data1[i].correo,
	        "fecha_hora_registro": data1[i].fecha_hora_registro,
	        "fecha_hora_cierre": data1[i].fecha_hora_cierre,
	        "envio_encuesta": data1[i].envio_encuesta,
			"observacion_ans": data1[i].observacion_ans
	      }).draw();
	    }
	  }
	)
	.fail(function(jqXHR, textStatus, errorThrown) {
	  console.error("Error en la consulta:", textStatus, errorThrown);
	  alert("Ocurrió un error al obtener las solicitudes PQRS. Intente de nuevo.");
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
	Swal.fire({ icon: icono, text: mensaje, customClass: { icon: 'swal-icon-small' } });
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
	validarCampoVacio($("#selectMotivo").val(), "Debe seleccionar un motivo");
	validarCampoVacio($("#selectPrioridad").val(), "Debe seleccionar una prioridad.");

	if ($("#selectUsuarioRegistro").val() === "0") {
		errores.push("Debe seleccionar el usuario que registra la PQRS.");
	}

	if ($("#selectEstado").val() === "0") {
		errores.push("Debe seleccionar un estado");
	}

	if (!validarCorreo) {
		errores.push("El correo ingresado no es valido.");
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



inputCorreo.addEventListener('input', () => {
	const valor = inputCorreo.value.trim();
	const esValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);

	if (valor === '') {
		inputCorreo.classList.remove('is-valid', 'is-invalid');
		errorDiv.style.display = 'none';
		validarCorreo = true; // o false, según si lo consideras válido vacío
		return;
	}

	if (esValido) {
		inputCorreo.classList.add('is-valid');
		inputCorreo.classList.remove('is-invalid');
		errorDiv.style.display = 'none';
		validarCorreo = true;
	} else {
		inputCorreo.classList.add('is-invalid');
		inputCorreo.classList.remove('is-valid');
		errorDiv.style.display = 'block';
		validarCorreo = false;
	}
});

/*function limpiarConsultaPQRS() {

	$('#idSolicitudPQRS').val('');
	$('#fecha').val('');
	$('#selectSolicitud').val('');
	$('#telefono').val('');
	$('#nombres').val('');
	$('#direccion').val('');
	$("#selectOrigen").val('');
	$('#descuentoRedimido').prop('checked', false);
	$('#selectTiendas, #selectMunicipio, #selectAreaResponsable, #selectPorcentajeDesc,#selectSolicitudpqrs,#selectUsuarioRegistro,#selectUsuarioRedencion,#selectEstado').prop('selectedIndex', 0);
	$(' #apellidos, #zona, #valorPedido, #idpedidotienda, #idpedidoredencion , #valorDescuento, #selectFoco,#selectTiendaspqrs,#selectMotivo,#selectPrioridad').val("");
	historialContainer.innerHTML = '';
	
}
*/
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



function getListaMotivoPrioridad() {
	$.getJSON(server + 'MotivoPrioridadPqrs', function(data) {
		if (data && data.motivo_pqrs && data.prioridad_pqrs) {

			motivos = data.motivo_pqrs
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
$('#selectSolicitudpqrs').on('change', function() {
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
$('#selectMotivo').on('change', function() {
	const $opcionSeleccionada = $(this).find('option:selected'); // opción seleccionada
	const prioridadId = $opcionSeleccionada.data('prioridad-id'); // accede al data attribute

	const $selectPrioridad = $('#selectPrioridad');
	$selectPrioridad.val(prioridadId); // ← Establece el valor usando jQuery
});


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
	if (ValidarDatosActualizados() != 1) return;

	if (idSolicitudPQRS == 0) {
		return Swal.fire({ icon: 'warning', text: 'No se ha seleccionado ninguna PQRS para Modificar' });
	}

	// 1️⃣ Recolectar datos del formulario
	const datos = {
		fechasolicitud: $("#fecha").val(),
		tiposolicitud: $("#selectSolicitudpqrs").val(),
		idcliente: idCliente,
		idtienda: $("#selectTiendaspqrs option:selected").attr('id'),
		nombres: $("#nombres").val(),
		apellidos: $("#apellidos").val(),
		telefono: $("#telefono").val(),
		direccion: $("#direccion").val(),
		zona: $("#zona").val(),
		idmunicipio: $("#selectMunicipio option:selected").attr('id'),
		idorigen: $("#selectOrigen option:selected").attr('id'),
		idfoco: $("#selectFoco option:selected").attr('id'),
		tipo: $("#selectTipo").val(),
		arearesponsable: $("#selectAreaResponsable").val(),
		idsolicitudpqrs: idSolicitudPQRS,
		idpedidotienda: $("#idpedidotienda").val(),
		idpedidoredencion: $("#idpedidoredencion").val(),
		valorPedido: $("#valorPedido").val(),
		valorDescuento: $("#valorDescuento").val(),
		porcentajeDescuento: $("#selectPorcentajeDesc").val(),
		descuentoRedimido: document.getElementById("descuentoRedimido").checked,
		listaComentarios: JSON.stringify(obtenerListaComentarios()),
		idusuarioRegistro: $("#selectUsuarioRegistro").val(),
		idusuarioRedencion: $("#selectUsuarioRedencion").val(),
		idestado: $("#selectEstado").val(),
		idmotivo: $("#selectMotivo").val(),
		idprioridad: $("#selectPrioridad").val(),
		ccVinculado: document.getElementById("ccVinculado").checked,
		correo: $("#correo").val(),
		envio_encuesta: envioEncuestaPqrs,
		observacion_ans:  $("#observacionAns").val(),
		cambio_fecha_cierre: false
	};

	// 2️⃣ Confirmación
	Swal.fire({
		title: 'Confirmación Actualización',
		text: '¿Desea confirmar la actualización de la Solicitud PQRS?',
		icon: 'warning',
		showCancelButton: true,
		confirmButtonText: 'Sí',
		cancelButtonText: 'No',
		confirmButtonColor: 'blue',
		cancelButtonColor: 'gray'
	}).then(result => {
		if (!result.isConfirmed) return;
		procesarActualizacion(datos);
	});
}

// 📌 Función para obtener lista de comentarios
function obtenerListaComentarios() {
	const comentarios = historialContainer.querySelectorAll("textarea");
	const lista = [];

	comentarios.forEach(textarea => {
		const id = parseInt(textarea.getAttribute("data-id") || "0");
		const fecha = textarea.getAttribute("data-fecha");
		const estado = textarea.getAttribute("data-estado");
		const texto = textarea.value.trim();

		if (texto && !(estado == "false" && id == 0)) {
			lista.push({ id, comentario: texto, fecha, idSolicitud: idSolicitudPQRS, estado });
		}
	});

	return lista;
}

// 📌 Función para procesar la actualización con AJAX
async function procesarActualizacion(datos) {
	// Validar fecha de cierre antes de enviar
	if (datos.idestado == 4) {
		if (fecha_hora_cierre) {
			// Ya existía fecha de cierre → preguntar
			const decision = await Swal.fire({
				html: `<b>La PQRS ya tiene una fecha de cierre</b><br>Actualmente está registrada con fecha <b>${fecha_hora_cierre}</b>.<br><br>
					¿Desea reemplazarla con la fecha y hora actual?`,
				icon: 'question',
				customClass: { icon: 'swal-icon-small' },
				showCancelButton: true,
				confirmButtonText: 'Sí, reemplazar',
				cancelButtonText: 'No, conservar',
				allowOutsideClick: false, // 👈 impide cerrar clickeando afuera
				allowEscapeKey: false     // 👈 impide cerrar con ESC
			});

			if (decision.isConfirmed) {
				datos.cambio_fecha_cierre = true; // Había fecha previa y se reemplaza
			} 
		} 
	}

	// Aquí ya tienes decisión, ahora sí mostrar "Actualizando..."
	Swal.fire({
		title: 'Actualizando...',
		text: 'Por favor espere un momento',
		allowOutsideClick: false,
		didOpen: () => Swal.showLoading()
	});

	let iconoFinal = 'success';
	let mensajeFinal = '';

	try {
		const response = await $.ajax({
			url: server + 'ActualizarSolicitudPQRS',
			dataType: 'json',
			type: 'POST',
			data: datos
		});

		const respuesta = response?.[0];

		if (respuesta?.idSolicitudPQRS > 0) {
			mensajeFinal = `✅ Se ha actualizado correctamente la solicitud PQRS número ${respuesta.idSolicitudPQRS}.`;

			/*if (datos.idestado == 4) {
				if (!envioEncuestaPqrs) {
					if (respuesta.telefono_valido) {
						if (respuesta.envioEncuesta) {
							mensajeFinal += "<br>✅ La encuesta de satisfacción fue enviada correctamente.";
							if (!respuesta.estadoEncuesta) {
								mensajeFinal += "<br>⚠️ El estado del envío de la encuesta podría no haberse actualizado correctamente.";
							}
						} else {
							mensajeFinal += "<br>⚠️ No se pudo enviar la encuesta de satisfacción.";
							iconoFinal = 'warning';
						}
					} else {
						mensajeFinal += "<br>⚠️ Teléfono inválido, no se envió la encuesta.";
						iconoFinal = 'warning';
					}
				} else {
					mensajeFinal += "<br>✅ El envío de la encuesta ya había sido registrado anteriormente.";
				}
			}*/
		} else {
			mensajeFinal = "No se pudo actualizar la PQRS. La respuesta no contiene un ID válido.";
			iconoFinal = 'error';
		}
	} catch (error) {
		mensajeFinal = '❌ Ocurrió un error inesperado: ' + error.message;
		iconoFinal = 'error';
	} finally {
		Swal.close();
		Swal.fire({
			icon: iconoFinal,
			title: 'Resultado',
			html: mensajeFinal,
			customClass: { icon: 'swal-icon-small' }
		});

		limpiarPQRS();
		if ($.fn.dataTable.isDataTable('#grid-consultaPQRS')) {
			$('#grid-consultaPQRS').DataTable().clear().draw();
		}
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

								limpiarPQRS();
								if ($.fn.dataTable.isDataTable('#grid-consultaPQRS')) {
									const table = $('#grid-consultaPQRS').DataTable();
									table.clear().draw();
								}
							}
						}
					});
			}
		});

	}
}


function limpiarPQRS() {
	// Si todos los inputs/selects están dentro de un formulario, lo reseteamos:
	const form = document.querySelector("#formPQRS"); // <-- pon el id real de tu <form>
	if (form) form.reset();

	// Limpiar campos específicos que no se limpian con reset
	historialContainer.innerHTML = '';
	$("#img-gallery").html('');
	$("#horas_transc").html(""); 
	$("#estado_ans").html("").css("background-color", "#FFFFFFFF");

	// Resetear variables globales
	idSolicitudPQRS = 0;
	idestadoPqrs = 0;
	fecha_hora_registro = "";
	fecha_hora_cierre = "";
	envioEncuestaPqrs = false;

	// Quitar validaciones visuales de correo
	inputCorreo.classList.remove('is-invalid', 'is-valid');

	// Reiniciar checkboxes
	$('#descuentoRedimido, #ccVinculado').prop('checked', false);

	// Asegurar que los selects con "placeholder" se marquen correctamente
	$('select').each(function() {
		if (this.value === "0") {
			this.classList.add("placeholder");
		} else {
			this.classList.remove("placeholder");
		}
	});
}



async function generarReporteDes() {
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
		const grupo = d.ccVinculado === true ? 'Vinculado Contact Center' : (d.tienda || 'Sin Tienda');
		if (!agrupados[grupo]) agrupados[grupo] = [];
		agrupados[grupo].push(d);
	});


	const workbook = new ExcelJS.Workbook();
	const worksheet = workbook.addWorksheet("Reporte");

	let fila = 1;
	let totalGlobal = 0;
	let totalGlobalRedimido = 0;
	let totalGlobalRedimidos = 0;
	let totalGlobalRegistros = 0;

	Object.entries(agrupados).forEach(([tienda, registros]) => {
		worksheet.mergeCells(`A${fila}:H${fila}`);
		const celdaTitulo = worksheet.getCell(`A${fila}`);
		celdaTitulo.value = tienda === 'Vinculado Contact Center' ? tienda : `Tienda:  ${tienda}`;
		celdaTitulo.font = { bold: true, size: 12, color: { argb: 'FFFFFFFF' } };
		celdaTitulo.fill = {
			type: 'pattern',
			pattern: 'solid',
			fgColor: { argb: '0d2273' }
		};
		celdaTitulo.alignment = { horizontal: 'center', vertical: 'middle' };
		fila++;

		const headers = ['#', 'Factura', 'Teléfono', 'Valor Pedido', 'Porcentaje Descuento', 'Valor Descuento', 'Redimido', 'Factura Redención'];
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
				r.telefono || 'No disponible',
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
		const celdaLabel = worksheet.getCell(filaResumen, 10); // Columna I
		const celdaValor = worksheet.getCell(filaResumen, 11); // Columna J

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
	worksheet.getColumn(9).width = 10;


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
    return grupo; // devolver el div completo
}

function agregarComentarioVisual(fecha, id, texto) {
    // Obtener todos los grupos de la misma fecha
    let grupos = Array.from(document.querySelectorAll(`.grupo-fecha[data-fecha='${fecha}']`));

    // Filtrar grupos que tengan al menos un comentario activo
    let grupoActivo = grupos.find(g => 
        Array.from(g.querySelectorAll("textarea")).some(c => 
            c.dataset.estado === "true" && c.dataset.fecha === fecha
        )
    );
    // Si no hay grupo activo, crear uno nuevo
    if (!grupoActivo) {
        grupoActivo = crearGrupoFecha(fecha);
    } else {
        grupoActivo.style.display = "block";
    }
    const grupoComentarios = grupoActivo.querySelector(".comentarios");

    // Crear contenedor del comentario
    const wrapper = document.createElement("div");
    wrapper.className = "position-relative mb-2 comentario-item";

    // Crear textarea
    const textarea = document.createElement("textarea");
    textarea.className = "form-control";
    textarea.rows = 2;
    textarea.value = texto;
    textarea.dataset.id = id;
    textarea.dataset.fecha = fecha;
    textarea.dataset.estado = "true";

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
        textarea.dataset.estado = "false";
        textarea.classList.add("d-none");
        iconoEliminar.remove();

        // Si no quedan comentarios activos en el grupo, ocultarlo
        const activos = grupoComentarios.querySelectorAll("textarea[data-estado='true']");
        if (activos.length === 0) {
            grupoActivo.style.display = "none";
        }
    });

    wrapper.appendChild(textarea);
    wrapper.appendChild(iconoEliminar);
    grupoComentarios.appendChild(wrapper);
}



// Acción para nuevo comentario
document.getElementById("btnAgregarComentario").addEventListener("click", () => {
	try{
		console.log()
		const nuevoTexto = document.getElementById("nuevoComentario").value.trim();
		if (!nuevoTexto) return;

		const hoy = new Date();
		const hoyLocal = hoy.getFullYear() + "-" +
		                 String(hoy.getMonth() + 1).padStart(2, "0") + "-" +
		                 String(hoy.getDate()).padStart(2, "0");

		agregarComentarioVisual(hoyLocal, 0, nuevoTexto);

		document.getElementById("nuevoComentario").value = "";
		
	}catch(error){
		console.log(error);
	}

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


function getAreasEscaladas() {
	fetch(server + 'ObtenerAreasEscalamiento')
		.then(res => res.json())
		.then(areas => {
			const selectAreaResponsableEscalar = document.getElementById("selectAreaResponsableEscalar");
			const selectAreaResponsable = document.getElementById("selectAreaResponsable");

			selectAreaResponsableEscalar.innerHTML = '';
			selectAreaResponsable.innerHTML = '';

			// Opción por defecto para el primer select
			const optionDefault1 = document.createElement("option");
			optionDefault1.value = "";
			optionDefault1.textContent = "Seleccione una opción";
			optionDefault1.disabled = true;
			optionDefault1.selected = true;
			optionDefault1.hidden = true;

			// Opción por defecto para el segundo select
			const optionDefault2 = document.createElement("option");
			optionDefault2.value = "";
			optionDefault2.textContent = "Seleccione una opción";
			optionDefault2.disabled = true;
			optionDefault2.selected = true;
			optionDefault2.hidden = true;

			selectAreaResponsableEscalar.appendChild(optionDefault1);
			selectAreaResponsable.appendChild(optionDefault2);

			if (areas && areas.length) {
				AreasEscalamiento = areas;

				areas.forEach(area => {
					const option1 = new Option(area.area, area.area);
					const option2 = new Option(area.area, area.area);
					selectAreaResponsableEscalar.appendChild(option1);
					selectAreaResponsable.appendChild(option2);
				});
			} else {
				Swal.fire({
					icon: 'error',
					text: 'No se encontraron las áreas de PQRS'
				});
			}
		})
		.catch(err => {
			console.error("Error al cargar las áreas de PQRS:", err);
			Swal.fire({
				icon: 'error',
				text: 'No se pudieron cargar las áreas de PQRS.'
			});
		});
}


function abrirModalRespuesta() {
	var idSolicitudPQRS = document.getElementById("idSolicitudPQRS").value;

	if (!idSolicitudPQRS || idSolicitudPQRS === "0") {
		Swal.fire({
			icon: 'warning',
			text: 'Por favor, seleccione una solicitud antes de continuar.',
			customClass: {
				icon: 'swal-icon-small'
			}
		});
		return;
	}
	document.getElementById('idpqrs_envio').value = idSolicitudPQRS;
	document.getElementById('cliente_envio').value = $("#nombres").val();;
	document.getElementById('correo_envio').value = $("#correo").val();;

	// Limpiar contenido del correo
	document.getElementById('contenidoCorreo').value = '';


	$('#modalEnvioRespuesta').modal('show');
}

const inputCorreoEnvio = document.getElementById('correo_envio');
const errorCorreoDiv = document.getElementById('errorCorreoEnv');
let validarCorreoEnv = false;

// Evento cuando el usuario escribe en el input
inputCorreoEnvio.addEventListener('input', () => {
	const valor = inputCorreoEnvio.value.trim();

	// Expresión regular simple para validar correo
	const esValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);

	if (valor === '') {
		inputCorreoEnvio.classList.add('is-invalid');
		inputCorreoEnvio.classList.remove('is-valid');
		errorCorreoDiv.style.display = 'block';
		errorCorreoDiv.textContent = 'El correo no puede estar vacío.';
		validarCorreoEnv = false;
		return;
	}

	if (esValido) {
		inputCorreoEnvio.classList.remove('is-invalid');
		inputCorreoEnvio.classList.add('is-valid');
		errorCorreoDiv.style.display = 'none';
		validarCorreoEnv = true;
	} else {
		inputCorreoEnvio.classList.add('is-invalid');
		inputCorreoEnvio.classList.remove('is-valid');
		errorCorreoDiv.style.display = 'block';
		errorCorreoDiv.textContent = 'El correo no tiene un formato válido.';
		validarCorreoEnv = false;
	}
});


document.getElementById('btnEnviarCorreo').addEventListener('click', function() {
	// Recolectar datos
	const idpqrs = document.getElementById('idpqrs_envio').value;
	const cliente = document.getElementById('cliente_envio').value;
	const correo = document.getElementById('correo_envio').value;
	let contenido = document.getElementById('contenidoCorreo').value;

	if (!correo || !contenido || !cliente) {
		Swal.fire({
			icon: 'warning',
			text: 'Por favor completa todos los campos antes de enviar.',
			customClass: {
				icon: 'swal-icon-small'
			}
		});
		return;
	}

	inputCorreoEnvio.dispatchEvent(new Event('input'));

	if (!validarCorreoEnv) {
		Swal.fire({
			icon: 'warning',
			text: 'Correo inválido',
			customClass: {
				icon: 'swal-icon-small'
			}
		});
		return;
	}

	// Escapar HTML peligroso para evitar inyección
	contenido = contenido
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;");

	// Convertir **negrita** a <strong>
	contenido = contenido.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");

	// 🔽 ELIMINA saltos de línea no deseados (como los que aparecen por copiar desde Word o PDF)
	contenido = contenido.replace(/([^\.\n])\n(?=[^\n])/g, '$1 ');

	// 🔽 Luego sí convierte los saltos de línea reales a <br>
	contenido = contenido.replace(/\n/g, "<br>");




	Swal.fire({
		title: '¿Estás seguro?',
		text: "Se enviará un correo al cliente con la respuesta PQRS.",
		icon: 'question',
		showCancelButton: true,
		confirmButtonText: 'Sí, enviar',
		cancelButtonText: 'Cancelar'
	}).then((result) => {
		if (result.isConfirmed) {
			// Mostrar spinner
			Swal.showLoading();

			fetch(server + "CorreoRespuestaPqrs", {
				method: "POST",
				headers: {
					"Content-Type": "application/x-www-form-urlencoded"
				},
				body: new URLSearchParams({
					cliente,
					correo,
					idpqrs,
					contenido
				})
			})
				.then(response => response.json())
				.then(data => {
					Swal.close();
					if (data.success) {
						Swal.fire({
							icon: 'success',
							text: 'Éxito',
							customClass: {
								icon: 'swal-icon-small'
							}
						});
						$('#modalEnvioRespuesta').modal('hide');
					} else {
						Swal.fire({
							icon: 'error',
							text: "Error: " + data.message,
							customClass: {
								icon: 'swal-icon-small'
							}
						});

					}
				})
				.catch(error => {
					Swal.close();
					console.error("Error en la solicitud:", error);
					Swal.fire({
						icon: 'error',
						text: "No se pudo enviar el correo. Verifica la consola.",
						customClass: {
							icon: 'swal-icon-small'
						}
					});
				});

		}
	});
});


document.getElementById('btnEnviarEncuestaS').addEventListener('click', function() {
	const idSolicitudPQRS = document.getElementById("idSolicitudPQRS").value;

	if (!idSolicitudPQRS || idSolicitudPQRS === "0") {
		Swal.fire({
			icon: 'warning',
			text: 'Por favor, seleccione una solicitud antes de continuar.',
			customClass: { icon: 'swal-icon-small' }
		});
		return;
	}

	let mensajesAdvertencia = [];

	if (envioEncuestaPqrs) {
		mensajesAdvertencia.push("⚠️ La encuesta ya fue enviada anteriormente.");
	}

	if (idestadoPqrs != 4) {
		mensajesAdvertencia.push("⚠️ La solicitud no está cerrada.");
	}

	if (mensajesAdvertencia.length > 0) {
		Swal.fire({
			html: mensajesAdvertencia.join("<br>") + "<br><br>¿Desea enviar la encuesta igualmente?",
			icon: 'warning',
			showCancelButton: true,
			confirmButtonText: 'Sí, enviar',
			cancelButtonText: 'Cancelar',
			customClass: { icon: 'swal-icon-small' }
		}).then((result) => {
			if (result.isConfirmed) {
				enviarEncuesta();
			}
		});
	} else {
		enviarEncuesta();
	}

	function enviarEncuesta() {
		const idpqrs = idSolicitudPQRS;
		const cliente = document.getElementById("nombres").value;
		const correo = document.getElementById("correo").value;
		const telefonoInput = document.getElementById("telefono").value;

		Swal.fire({
			title: 'Encuesta de satisfacción',
			input: 'text',
			inputLabel: 'Teléfono del cliente',
			inputPlaceholder: 'Ingrese un número...',
			inputValue: telefonoInput,
			showCancelButton: true,
			confirmButtonText: 'Enviar',
			cancelButtonText: 'Cancelar',
			inputValidator: (value) => {
				if (!value) {
					return '¡Debes ingresar un número de celular!';
				}
				const regexCelularColombia = /^3\d{9}$/;
				if (!regexCelularColombia.test(value)) {
					return '¡Número inválido! Debe empezar por 3 y tener 10 dígitos.';
				}
				return null;
			}
		}).then((result) => {
			if (result.isConfirmed) {
				const telefono = result.value;
				Swal.showLoading();

				fetch(server + "CorreoEncuestaPqrs", {
					method: "POST",
					headers: {
						"Content-Type": "application/x-www-form-urlencoded"
					},
					body: new URLSearchParams({
						cliente,
						correo,
						idpqrs,
						telefono
					})
				})
					.then(response => response.json())
					.then(data => {
						Swal.close();
						if (data.success) {
							Swal.fire({
								icon: 'success',
								text: '¡Encuesta enviada con éxito!',
								customClass: { icon: 'swal-icon-small' }
							});
						} else {
							Swal.fire({
								icon: 'error',
								text: "Error: " + data.message,
								customClass: { icon: 'swal-icon-small' }
							});
						}
					})
					.catch(error => {
						Swal.close();
						console.error("Error en la solicitud:", error);
						Swal.fire({
							icon: 'error',
							text: "No se pudo enviar el correo. Verifica la consola.",
							customClass: { icon: 'swal-icon-small' }
						});
					});
			}
		});
	}
});



function finalizarEscalar()
{
	$.getJSON(server + 'FinalizarEscalamientoPQRS?idescalamiento=' + idEscalamientoCon, function(data) {
		if(data.respuesta)
		{
			mostrarAlerta('success','Se finalizó el escalamiento al área.');
			//Hacemos consulta para llenar los escalamientos
			consultarEscalamientoDataTable(idSolicitudPQRS);
		}
	});

}

function consultarEscalamientoDataTable(idSolicitudCon)
{
	//Hacemos consulta para llenar los escalamientos
	$.getJSON(server + 'ConsultarEscalamientoPQRS?idsolicitudpqrs=' + idSolicitudCon, function(data2) {

		var escalamientos = data2;
		dtEscalamientoPQRS.clear().draw();
		for (var i = 0; i < data2.length; i++) {
			dtEscalamientoPQRS.row.add({
			"idescalamiento": data2[i].idescalamiento,
			"idsolicitudpqrs": data2[i].idsolicitudpqrs,
			"arearesponsable": data2[i].arearesponsable,
			"fechaescalamiento": data2[i].fechaescalamiento,
			"fecharesolucion": data2[i].fecharesolucion,
			"solucionado": data2[i].solucionado
		}).draw();
		}
	});
}


// ======================
// Configuración de festivos (YYYY-MM-DD)
const festivos = []; // ejemplo

// ======================
// Funciones auxiliares
// ======================
function parseFecha(fechaStr) {
  if (!fechaStr) return null;
  if (fechaStr instanceof Date) return fechaStr;
  if (typeof fechaStr !== "string") return null;
  return new Date(fechaStr.replace(" ", "T"));
}

function esNoHabil(fecha) {
  if (!(fecha instanceof Date) || isNaN(fecha)) return false;
  const dia = fecha.getDay();
  const fechaStr = fecha.toISOString().split("T")[0];
  return dia === 0 || dia === 6 || festivos.includes(fechaStr);
}

// ======================
// Calcular horas hábiles entre fechas
// ======================
function calcularHorasHabiles(fechaInicio, fechaFin, inicioHorario, finHorario) {
  let inicioActual = fechaInicio instanceof Date ? fechaInicio : parseFecha(fechaInicio);
  const fin = fechaFin instanceof Date ? fechaFin : parseFecha(fechaFin);
  let totalHoras = 0;

  if (!inicioActual || !fin) return 0;

  while (inicioActual < fin) {
    if (!esNoHabil(inicioActual)) {
      const fechaStr = inicioActual.toISOString().split("T")[0];
      const inicioDia = parseFecha(`${fechaStr}T${inicioHorario}`);
      const finDia = parseFecha(`${fechaStr}T${finHorario}`);

      // En el último día, usar fin absoluto si es antes de fin de jornada
      const tramoFinDia = (fin.toDateString() === inicioActual.toDateString()) ? fin : finDia;

      const tramoInicio = inicioActual < inicioDia ? inicioDia : inicioActual;
      const tramoFin = tramoFinDia < finDia ? tramoFinDia : finDia;

      if (tramoFin > tramoInicio) {
        totalHoras += (tramoFin - tramoInicio) / (1000 * 60 * 60);
      }
    }

    // Pasar al siguiente día a las 00:00
	inicioActual.setDate(inicioActual.getDate() + 1);
	inicioActual.setHours(parseInt(inicioHorario.split(":")[0]), parseInt(inicioHorario.split(":")[1]), 0, 0);
  }

  return totalHoras;
}

// ======================
// Calcular ANS con fallback si no hay cierre
// ======================
function calcularANSConFallback(escalamientos, areas, idPrioridad, fechaRegistro, fechaCierre, idEstadoPQRS) {
  const prioridad = prioridades.find(p => p.idprioridad === idPrioridad);
  let horasTotales = 0;

  // Si no hay fecha de cierre, usar la fecha actual
  const fechaCierreReal = fechaCierre && fechaCierre.trim() !== ""
      ? parseFecha(fechaCierre)
      : new Date();

  const fechaRegistroDate = parseFecha(fechaRegistro);
  const fechaCierreDate = fechaCierreReal;

  if (!fechaRegistroDate || isNaN(fechaRegistroDate)) {
    return { horasTotales: 0, estadoANS: "Error: fecha de registro inválida" };
  }
  if (!fechaCierreDate || isNaN(fechaCierreDate)) {
    return { horasTotales: 0, estadoANS: "Error: fecha de cierre inválida" };
  }

  // Validar fechas de escalamiento
  for (let esc of escalamientos) {
    const fechaEscalamientoDate = parseFecha(esc.fechaescalamiento);
    if (!fechaEscalamientoDate || isNaN(fechaEscalamientoDate)) {
      return { horasTotales: 0, estadoANS: `Error: fecha de escalamiento inválida (${esc.arearesponsable})` };
    }
    if (esc.fecharesolucion) {
      const fechaResolucionDate = parseFecha(esc.fecharesolucion);
      if (!fechaResolucionDate || isNaN(fechaResolucionDate)) {
        return { horasTotales: 0, estadoANS: `Error: fecha de resolución inválida (${esc.arearesponsable})` };
      }
    }
  }

  if (escalamientos.length > 0) {
	  // Ordenar por fecha de escalamiento
	  const escOrdenados = [...escalamientos].sort(
	    (a, b) => parseFecha(a.fechaescalamiento) - parseFecha(b.fechaescalamiento)
	  );

	  // 👉 Tramo inicial: desde fechaRegistro hasta el primer escalamiento con "contact"
	  const primeraFechaEscalamiento = parseFecha(escOrdenados[0].fechaescalamiento);
	  if (escOrdenados[0].arearesponsable !== "contact" && primeraFechaEscalamiento > fechaRegistroDate) {
	    const areaContact = areas.find(a => a.area === "contact");
	    horasTotales += calcularHorasHabiles(
	      fechaRegistroDate,
	      primeraFechaEscalamiento,
	      areaContact.inicio_horario,
	      areaContact.final_horario
	    );
	  }

	  
	// Calcular cada tramo de escalamiento normalmente
	escOrdenados.forEach(esc => {
	  const area = areas.find(a => a.area === esc.arearesponsable) || areas.find(a => a.area === "contact");
	  const finEscalamientoDate = esc.fecharesolucion && esc.fecharesolucion.trim() !== ""
	    ? parseFecha(esc.fecharesolucion)
	    : fechaCierreDate;

	  horasTotales += calcularHorasHabiles(
	    parseFecha(esc.fechaescalamiento),
	    finEscalamientoDate,
	    area.inicio_horario,
	    area.final_horario
	  );
	});


	// 👉 Tramo pendiente en "contact" si último escalamiento no cubre cierre
	const ultimaFechaResolucion = parseFecha(escOrdenados[escOrdenados.length - 1].fecharesolucion) || fechaRegistroDate;
	if (fechaCierreDate > ultimaFechaResolucion) {
	  const areaContact = areas.find(a => a.area === "contact");
	  horasTotales += calcularHorasHabiles(
	    ultimaFechaResolucion,
	    fechaCierreDate,
	    areaContact.inicio_horario,
	    areaContact.final_horario
	  );
	}


  } else {
    // No escalada → todo con contact
    const area = areas.find(a => a.area === "contact");
    horasTotales += calcularHorasHabiles(
        fechaRegistroDate,
        fechaCierreDate,
        area.inicio_horario,
        area.final_horario
    );
  }

  // ======================
  // Determinar estado ANS
  // ======================
  let estadoANS;
  let colorANS;

  // Lógica de asignación de color y estado basada en el tiempo
  if (horasTotales < prioridad.t_resp_min) {
      colorANS = 'FFC6EFCE'; // verde
      if (idEstadoPQRS !== 4) {
          estadoANS = "Actualmente no se le ha dado cierre, y aún no ha alcanzado el tiempo mínimo de respuesta";
      } else {
          estadoANS = "Resuelta antes del tiempo";
      }
  } else if (horasTotales > prioridad.t_resp_max) {
      colorANS = 'FFFFC7CE'; // rojo
      if (idEstadoPQRS !== 4) {
          estadoANS = "Actualmente no se le ha dado cierre y ya se superó el tiempo máximo de respuesta";
      } else {
          estadoANS = "Resuelta fuera del tiempo";
      }
  } else {
      colorANS = 'FFFFEB9C'; // amarillo
      if (idEstadoPQRS !== 4) {
          estadoANS = "Actualmente no se le ha dado cierre, pero se encuentra dentro del tiempo de respuesta";
      } else {
          estadoANS = "Resuelta dentro del tiempo";
      }
  }

  
  const horasRedondeadas = Math.round(horasTotales);

  return { horasRedondeadas, estadoANS , colorANS};
}


async function generarReporteANS() {
	console.log(prioridades);
    const todosDatos = dtconsultasPQRS.rows().data().toArray();

    if (todosDatos.length === 0) {
        Swal.fire({ icon: 'warning', text: "No hay datos para generar el reporte de la ANS." });
        return;
    }

    // Obtener todos los IDs de las PQRS
    const idsPQRS = todosDatos.map(d => d.idconsultaPQRS).join(',');

    // Llamar al servicio que devuelve todos los escalamientos de estos IDs
    let escalamientos = [];

    try {
        const response = await fetch(`${server}ConsultarEscalamientoPQRS?idsolicitudes=${idsPQRS}`);
        escalamientos = await response.json(); // Array de todos los escalamientos
    } catch (error) {
        console.error("Error al obtener los escalamientos:", error);
        Swal.fire({ icon: 'error', text: "No se pudieron obtener los escalamientos." });
        return;
    }

    // Crear workbook y worksheet
    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet(`Reporte ANS`);

    // =========================
    // 🔹 Encabezado principal
    // =========================
    const hoyTexto = new Date().toLocaleString("es-CO", {
        day: "2-digit", month: "2-digit", year: "numeric",
        hour: "2-digit", minute: "2-digit"
    });

	// 🔹 Fila 1 → título general
	worksheet.mergeCells("A1:I1");
	worksheet.getCell("A1").value = `Reporte ANS (${fecha_inicial} - ${fecha_final}) - Generado: ${hoyTexto}`;
	worksheet.getCell("A1").alignment = { horizontal: "center" };
	worksheet.getCell("A1").font = { size: 12, bold: true };

	// 🔹 Fila 2 → encabezados de columnas
	worksheet.addRow([
	    "ID PQRS",
	    "Fecha Inicio",
	    "Fecha Cierre",
	    "Horas Hábiles Transcurridas",
	    "Estado ANS",
	    "Estado PQRS",
		"Escalado"
	]);

	// Dar formato a la fila 2
	worksheet.getRow(2).eachCell(cell => {
	    cell.fill = {
	        type: 'pattern',
	        pattern: 'solid',
	        fgColor: { argb: 'FF4472C4' } // azul
	    };
	    cell.font = { bold: true, color: { argb: 'FFFFFFFF' } };
	    cell.alignment = { horizontal: 'left', vertical: 'middle' };
	    cell.border = {
	        top: { style: 'thin' },
	        left: { style: 'thin' },
	        bottom: { style: 'thin' },
	        right: { style: 'thin' }
	    };
	});
	// =========================
	// 🔹 Definir columnas principales
	// =========================
	worksheet.columns = [
	    { key: "id" },
	    { key: "fechaInicio" },
	    { key: "fechaCierre" },
	    { key: "horas" },
	    { key: "estadoANS" },
	    { key: "estadoPQRS" },
	    { key: "prioridad" },       
	    { key: "estadoEscalado" },   
	    { key: "observacionANS" }   // 👈 nueva columna
	];

	// 🔹 Encabezados de columnas
	worksheet.getRow(2).values = [
	    "ID PQRS",
	    "Fecha Inicio",
	    "Fecha Cierre",
	    "Horas Hábiles Transcurridas",
	    "Estado ANS",
	    "Estado PQRS",
	    "Prioridad",
	    "Escalado",
	    "Observación ANS"   // 👈 nuevo encabezado
	];
	worksheet.getRow(2).eachCell(cell => {
	    cell.fill = {
	        type: 'pattern',
	        pattern: 'solid',
	        fgColor: { argb: 'FF4472C4' } // azul
	    };
	    cell.font = { bold: true, color: { argb: 'FFFFFFFF' } };
	    cell.alignment = { horizontal: 'center', vertical: 'middle' };
	    cell.border = {
	        top: { style: 'thin' },
	        left: { style: 'thin' },
	        bottom: { style: 'thin' },
	        right: { style: 'thin' }
	    };
	});
	// =========================
	// 🔹 Llenado de datos
	// =========================
	todosDatos.forEach(d => {
	    const escParaEstaPQRS = escalamientos.filter(e => e.idsolicitudpqrs === d.idconsultaPQRS);

	    const resultado = calcularANSConFallback(
	        escParaEstaPQRS,
	        AreasEscalamiento,
	        d.idprioridad,
	        d.fecha_hora_registro,
	        d.fecha_hora_cierre,
	        d.idestado
	    );

	    const estadoPQRS = d.idestado == 4 ? "Cerrada" : "Abierta";

	    let fechaCierreTexto;
	    if (d.fecha_hora_cierre && d.fecha_hora_cierre.trim() !== "") {
	        fechaCierreTexto = d.fecha_hora_cierre;
	    } else {
	        fechaCierreTexto = "Sin fecha de cierre (se cuenta desde la creación del reporte)";
	    }

	    // 👇 Prioridad
	    const prioridadObj = prioridades.find(p => p.idprioridad === d.idprioridad);
	    const prioridadDesc = prioridadObj ? prioridadObj.descripcion : "N/A";

	    // 👇 Escalado
	    let escaladoTexto = "NO";
	    if (escParaEstaPQRS.length > 0) {
	        escaladoTexto = escParaEstaPQRS.map(e => e.arearesponsable).join(", ");
	    }

		const row = worksheet.addRow({
		    id: d.idconsultaPQRS,
		    fechaInicio: d.fecha_hora_registro,
		    fechaCierre: fechaCierreTexto,
		    horas: resultado.horasRedondeadas,
		    estadoANS: resultado.estadoANS,
		    estadoPQRS: estadoPQRS,
		    prioridad: prioridadDesc,
		    estadoEscalado: escaladoTexto,
		    observacionANS: d.observacion_ans ?? ""   // 👈 se agrega
		});

	    // =========================
	    // 🔹 Colores según estado ANS
	    // =========================
	    let colorANS = 'FFFFFFFF'; // blanco por defecto
	    if (resultado.colorANS) {
	        colorANS = resultado.colorANS;
	    }

	    row.eachCell(cell => {
	        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: colorANS } };
	        cell.border = {
	            top: { style: 'thin' },
	            left: { style: 'thin' },
	            bottom: { style: 'thin' },
	            right: { style: 'thin' }
	        };
	        cell.alignment = { wrapText: true, vertical: 'middle', horizontal: 'center' };
	    });
	});

	// =========================
	// 🔹 Tabla de Prioridades (con espacio)
	// =========================
	worksheet.mergeCells("K1:M1"); // 👈 ahora empieza en K
	worksheet.getCell("K1").value = "Tipos de Prioridades";
	worksheet.getCell("K1").alignment = { horizontal: "center" };
	worksheet.getCell("K1").font = { size: 12, bold: true };

	worksheet.getRow(2).getCell("K").value = "Prioridad";
	worksheet.getRow(2).getCell("L").value = "Tiempo Mín (h)";
	worksheet.getRow(2).getCell("M").value = "Tiempo Máx (h)";

	["K", "L", "M"].forEach(col => {
	    const cell = worksheet.getRow(2).getCell(col);
	    cell.fill = {
	        type: 'pattern',
	        pattern: 'solid',
	        fgColor: { argb: 'FF305496' }
	    };
	    cell.font = { bold: true, color: { argb: 'FFFFFFFF' } };
	    cell.alignment = { horizontal: 'center', vertical: 'middle' };
	    cell.border = {
	        top: { style: 'thin' },
	        left: { style: 'thin' },
	        bottom: { style: 'thin' },
	        right: { style: 'thin' }
	    };
	});


	let rowIndex = 3;
	prioridades.forEach(p => {
	    worksheet.getCell(`K${rowIndex}`).value = p.descripcion;
	    worksheet.getCell(`L${rowIndex}`).value = p.t_resp_min;
	    worksheet.getCell(`M${rowIndex}`).value = p.t_resp_max;

	    ["K", "L", "M"].forEach(col => {
	        const cell = worksheet.getCell(`${col}${rowIndex}`);
			cell.fill = { 
			    type: 'pattern', 
			    pattern: 'solid', 
			    fgColor: { argb: 'FFFFFFFF' } // blanco
			};
	        cell.border = {
	            top: { style: 'thin' },
	            left: { style: 'thin' },
	            bottom: { style: 'thin' },
	            right: { style: 'thin' }
	        };
	        cell.alignment = { horizontal: 'center', vertical: 'middle' };
	    });

	    rowIndex++;
	});

	// =========================
	// 🔹 Tabla Estados ANS (debajo de prioridades)
	// =========================
	rowIndex += 2;
	worksheet.mergeCells(`K${rowIndex}:L${rowIndex}`);
	worksheet.getCell(`K${rowIndex}`).value = "Estados ANS";
	worksheet.getCell(`K${rowIndex}`).alignment = { horizontal: "center" };
	worksheet.getCell(`K${rowIndex}`).font = { size: 12, bold: true };

	rowIndex++;
	worksheet.getCell(`K${rowIndex}`).value = "Antes de tiempo";
	worksheet.getCell(`L${rowIndex}`).value = "Verde";
	["K","L"].forEach(col => {
	    worksheet.getCell(`${col}${rowIndex}`).border = {
	        top: { style: 'thin' },
	        left: { style: 'thin' },
	        bottom: { style: 'thin' },
	        right: { style: 'thin' }
	    };
	});
	worksheet.getCell(`L${rowIndex}`).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFC6EFCE' } };

	rowIndex++;
	worksheet.getCell(`K${rowIndex}`).value = "En el tiempo";
	worksheet.getCell(`L${rowIndex}`).value = "Amarillo";
	["K","L"].forEach(col => {
	    worksheet.getCell(`${col}${rowIndex}`).border = {
	        top: { style: 'thin' },
	        left: { style: 'thin' },
	        bottom: { style: 'thin' },
	        right: { style: 'thin' }
	    };
	});
	worksheet.getCell(`L${rowIndex}`).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFEB9C' } };

	rowIndex++;
	worksheet.getCell(`K${rowIndex}`).value = "Fuera de tiempo";
	worksheet.getCell(`L${rowIndex}`).value = "Rojo";
	["K","L"].forEach(col => {
	    worksheet.getCell(`${col}${rowIndex}`).border = {
	        top: { style: 'thin' },
	        left: { style: 'thin' },
	        bottom: { style: 'thin' },
	        right: { style: 'thin' }
	    };
	});
	worksheet.getCell(`L${rowIndex}`).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFC7CE' } };


    // =========================
	// Ajustar ancho de columnas
	worksheet.columns.forEach((column, index) => {
	    let maxLength = 0;

	    // Tomar en cuenta los datos
	    column.eachCell({ includeEmpty: true }, cell => {
	        if (cell.row <= 2) return; // ignorar fila título y encabezado
	        const cellValue = cell.value ? cell.value.toString() : "";
	        maxLength = Math.max(maxLength, cellValue.length);
	    });

	    // También considerar el encabezado de la columna
	    const header = worksheet.getRow(2).getCell(index + 1).value;
	    if (header) {
	        maxLength = Math.max(maxLength, header.toString().length);
	    }

	    // Aplicar ancho
	    column.width = Math.min(50, Math.floor(maxLength * 1.2) + 2);
	});

	worksheet.getColumn('J').width = 8;  // Escalado
    // =========================
    // 🔹 Generar y descargar archivo
    // =========================
    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    saveAs(blob, `Reporte_ANS_${fecha_inicial}_al_${fecha_final}.xlsx`);

    Swal.fire({ icon: 'success', text: "Reporte de ANS generado correctamente." });
}



async function GenerarExcelConsultaPqrs() {
  $('#btnExportarExcel').on('click', async function () {

    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet('PQRS');

    // --- Definir campos (claves) y encabezados visibles (nombres bonitos) ---
    const campos = [
      "idconsultaPQRS", "fechasolicitud", "tiposolicitud", "cliente", "direccion",
      "telefono", "municipio", "tienda", "nombreorigen", "nombrefoco",
      "tipo", "idpedidotienda", "nombres", "apellidos", "listaComentarios",
      "nombreEstado", "idmotivo", "idprioridad", "zona", "correo"
    ];
	


    const encabezados = [
      "ID Consulta", "Fecha Solicitud", "Tipo Solicitud", "Cliente", "Dirección",
      "Teléfono", "Municipio", "Tienda", "Origen", "Foco",
      "Tipo", "ID Pedido Tienda", "Nombres", "Apellidos", "Comentarios",
      "Estado", "Motivo", "Prioridad", "Zona", "Correo"
    ];

    // --- Agregar encabezados personalizados ---
    worksheet.addRow(encabezados);

	
	const totalRows = dtconsultasPQRS.rows({ search: 'applied' }).count();
	if (totalRows === 0) {
		mostrarAlerta();    mostrarAlerta("warning", "No se encontraron datos para realizar la descarga.") 
		 return;
		}
    // --- Recorrer los datos del DataTable ---
    dtconsultasPQRS.rows({ search: 'applied' }).every(function () {
      const data = this.data();
      const fila = [];

      campos.forEach(campo => {

        let valor = data[campo];
		
		if (campo === "idprioridad") {
		  const prioridadObj = prioridades.find(p => p.idprioridad === data.idprioridad);
		  valor = prioridadObj ? prioridadObj.descripcion : "N/A";
		}
		
		if(campo === "idmotivo"){
			const motivoObj = motivos.find(p => p.idmotivo === data.idmotivo);
			valor = motivoObj ? motivoObj.descripcion : "N/A";
			
		}

        // Procesar comentarios (listaComentarios)
        if (campo === "listaComentarios" && typeof valor === 'object' && valor !== null) {
          try {
            const partes = [];
            for (const fecha in valor) {
              const lista = valor[fecha];
              if (Array.isArray(lista)) {
                for (const item of lista) {
                  const comentario = item?.comentario?.trim();
                  if (comentario) partes.push(`(${fecha}) ${comentario}`);
                }
              }
            }
            valor = partes.join('\n');
          } catch (error) {
            console.error("Error procesando listaComentarios:", error);
            valor = '';
          }
        }

        // Eliminar HTML si hay
        if (typeof valor === 'string') valor = valor.replace(/<[^>]*>?/gm, '').trim();

        // Convertir undefined o null en vacío
        if (valor === undefined || valor === null) valor = '';

        fila.push(valor);
      });

      worksheet.addRow(fila);
    });

    // --- Estilos de columnas (autoancho + ajuste de texto) ---
    worksheet.columns.forEach(col => {
      let maxLength = 0;
      col.eachCell({ includeEmpty: true }, cell => {
        const cellValue = cell.value ? cell.value.toString() : '';
        if (cellValue.length > maxLength) maxLength = cellValue.length;
      });
      col.width = Math.min(maxLength + 2, 150); // límite de ancho
    });

    // --- Estilos de filas ---
    worksheet.eachRow((row, rowNumber) => {
      row.eachCell(cell => {
        cell.alignment = {
          vertical: 'top',
          horizontal: 'left',
          wrapText: true
        };
      });

      // Encabezado con estilo especial
      if (rowNumber === 1) {
        row.eachCell(cell => {
          cell.font = { bold: true, color: { argb: 'FF000000' } };
          cell.alignment = { horizontal: 'center', vertical: 'middle' };
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: { argb: 'FFCCE5FF' }
          };
          cell.border = {
            top: { style: 'thin', color: { argb: 'FFAAAAAA' } },
            left: { style: 'thin', color: { argb: 'FFAAAAAA' } },
            bottom: { style: 'thin', color: { argb: 'FFAAAAAA' } },
            right: { style: 'thin', color: { argb: 'FFAAAAAA' } }
          };
        });
      }
    });

    // --- Descargar archivo ---
    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    });
    saveAs(blob, `PQRS_${fecha_inicial}_al_${fecha_final}.xlsx`);
  });
}

GenerarExcelConsultaPqrs();



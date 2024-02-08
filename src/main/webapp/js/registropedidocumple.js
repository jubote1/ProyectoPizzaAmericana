	

// Se definen las variables globales.
var server;
var tiendas;
var table;
var productos;
var memcode = 0;
var idCliente = 0;
//Configuramos el campo fileinput 
var g = $("#file-1").fileinput({
    theme: 'fa5',
    uploadUrl: 'http://172.19.0.25:4200/service_upload.php',
    showRemove: false,
    showUpload: false,
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
    slugCallback: function (filename) {
        return filename.replace('(', '_').replace(']', '_');
    }

});


// A continuación  la ejecucion luego de cargada la pagina
$(document).ready(function() {

			

//validamos el contenido del campo fecha del pedido y el evento que lo controlará
$("#fecha").change(function(){
    var fechaped = $("#fecha").val();
    if(existeFecha(fechaped))
	{
	}
	else
	{
		alert ('La fecha de la solicitud no es correcta');
		$("#fecha").datepicker('setDate', new Date());
		return;
	}
});


	//Se invoca servicio para traerse la información de los productos disponibles en el sistema
	// En resumen se invocan todos servicios que se encargan de llenar la data del formulario.
	getListaTiendas();
	setInterval('validarVigenciaLogueo()',600000);
	// Llevamos a cero los campos cálculos de los totales
	// Se define evento para campo valor a devolver.

	} );


function validarVigenciaLogueo()
{
	var d = new Date();
	
	var respuesta ='';
	$.ajax({ 
	   	url: server + 'ValidarUsuarioAplicacion', 
	   	dataType: 'json',
	   	type: 'post', 
	   	async: false, 
	   	success: function(data){
			    respuesta =  data[0].respuesta;		
		} 
	});
	switch(respuesta)
	{
		case 'OK':
				break;
		case 'OKA':
				break;
		case 'OKP':
				break;
		default:
				location.href = server +"Index.html";
		    	break;
	}
		    		
}



// Método que invoca el servicio para listar las tiendas donde se pondrán tomar domicilios.
function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTiendas').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectTiendas").val('');
	});
}

function ConfirmarSolicitud()
{
	var valida = ValidacionesDatos();
	if (valida != 1)
	{
		return;
	}
	var fecha = $("#fecha").val();
	var tempTienda =  $("#selectTiendas option:selected").attr('id');
	var idPedido = $("#idpedido").val();
	var tel = $("#telefono").val();
	$.confirm(
	{
		'title'		: 'Confirmacion Solicitud de Beneficio de Cumpleaños',
		'content'	: 'Desea confirmar la inserción de la Solicitud de Beneficio de cumpleaños?' ,
		'buttons'	: 
		{
			'Si'	: 
			{
				'class'	: 'blue',
				'action': function()
				{
					$.ajax(
					{
		    				url: server + 'InsertarSolicitudCumple' , 
		    				dataType: 'json', 
		    				type: 'post', 
							data: 
							{'fecha' : fecha,
							 'idtienda' : tempTienda,
							 'idpedido' : idPedido
							}, 
		    				async: false, 
			    			success: function(data1)
			    			{
								var respuesta = data1[0];
								if(respuesta.idSolicitudCumple > 0)
		    					{
		    						alert('Se ha insertado correctamente la solicitud de Beneficio Cliente Cumpleaños número  ' + respuesta.idSolicitudCumple);
		    						var filestack = $('#file-1').fileinput('getFileList');
								    const fd = new FormData();

								    //Se guarda un array con las imagenes dentro de los datos del formulario
								    filestack.forEach(element => 
								    {
								        fd.append('files[]', element);
								    });

								  //se llama al servicio php para subir las imagenes.
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
								        success: function(resp) 
								        {
								        	console.log(resp);
								            //En este punto deberemos insertar las imágenes
								            for(var i = 0; i < resp.length;i++)
								            {
													var cadaResp  = resp[i];
													console.log(cadaResp.name);
													$.ajax({ 
													   	url: server + 'InsertarSolicitudCumpleImagenes?idsolicitudcumple=' + respuesta.idSolicitudCumple + '&rutaimagen=' + cadaResp.name, 
													   	dataType: 'json',
													   	type: 'post', 
													   	async: false, 
													   	success: function(data){
															    
														}
													});
													$('#file-1').fileinput('reset');
											}
											
																												
								        }
		    					 	});
		    						//antes del final
		    						$('#idpedido').val('');
									$("#selectTiendas").val('');
									$.alert('Se ha ingresado satisfactoriamente la Solicitud de beneficio Cumpleaños.');
		    					}
							}
					});
				}
			},
			'No'	: 
			{
				'class'	: 'gray',
				'action': function()
				{

				}	// Nothing to do in this case. You can as well omit the action property.
			}
		}
	});
}

function ValidacionesDatos()
{


	var tien = $("#selectTiendas option:selected").val();
	if (tien == '' || tien == null || tien == undefined)
	{
		alert ('Debe ingresar la tienda del cliente');
		return;
	}
	
	var idPedido = $("#idpedido").val();
	if (idPedido == '' || idPedido == null || idPedido == undefined)
	{
		alert ('Debe ingresar el número del pedido con el cual se redimió el beneficio de cumpleaños');
		return;
	}

	//Revisamos que por lo menos se tenga adicionada una imagen
	var filestack = $('#file-1').fileinput('getFileList');
	var hayImagen = false;
    //Se guarda un array con las imagenes dentro de los datos del formulario
    filestack.forEach(element => 
    {
        hayImagen = true;
    });
    if(!hayImagen)
    {
    	alert ('Debe agregar por lo menos una imagen a la solicitud');
		return;
    }

	return(1);
}

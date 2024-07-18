	

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

	if(validarFechaMenorActual(fechaped))
	{
	}
	else
	{
		alert ('La fecha de la solicitud es menor a la fecha actual, favor corregir');
		$("#fecha").datepicker('setDate', new Date());
		return;
	}
});


	//Se invoca servicio para traerse la información de los productos disponibles en el sistema
	// En resumen se invocan todos servicios que se encargan de llenar la data del formulario.
	getListaTiendas();
	getListaMunicipios();
	getListaOrigenes();
	getListaFocos();
	setInterval('validarVigenciaLogueo()',600000);
	// Llevamos a cero los campos cálculos de los totales
	// Se define evento para campo valor a devolver.
	
	// Se realiza la creación del DATATABLE DE CLIENTES
    table = $('#grid-clientes').DataTable( {
    		"aoColumns": [
            { "mData": "idCliente" },
            { "mData": "tienda" },
            { "mData": "nombre" },
            { "mData": "apellido" },
            { "mData": "nombrecompania" },
            { "mData": "direccion" },
            { "mData": "zona" },
            { "mData": "observacion" },
            { "mData": "telefono" },
            { "mData": "memcode"  , "visible": false }
        ]
    	} );

    
    $('#grid-clientes tbody').on('click', 'tr', function () {
        datos = table.row( this ).data();
        //alert( 'Diste clic en  '+datos.nombre+'\'s row' );
        $('#nombres').val(datos.nombre);
        $('#apellidos').val(datos.apellido);
        $('#nombreCompania').val(datos.nombrecompania);
        $('#direccion').val(datos.direccion);
        $('#zona').val(datos.zona);
        $('#observacionDir').val(datos.observacion);
        $("#selectTiendas").val(datos.tienda);
        // Para evitar que modifiquen la tienda
        $("#selectMunicipio").val(datos.municipio);
        memcode = datos.memcode;
        idCliente = datos.idCliente;
        var municipio = datos.municipio;
        var dirbuscar = datos.direccion + " " + municipio.toLowerCase();
        
     } );
 	

  	

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

// Método que se encarga luego de introducido un teléfono en el campo de teléfono del cliente llamar al servicio
function validarTelefono(){

	// Validamos el tema de la longitud del pedido
	var lngTel = $("#telefono").val().length;
	if (lngTel < 10)
	{
		alert("Tenga precaución el teléfono tiene una lontitud menor a 10");
	}

	if (!/^([0-9])*$/.test($("#telefono").val()))
	{
      alert("El valor " + $("#telefono").val() + " no es un número");
	}
	// Validamos que sean solo números
	if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) {
    	table = $('#grid-clientes').DataTable();
    }
	
	$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaCliente  = data1[i];
				table.row.add(data1[i]).draw();
			}
		});
		
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

function getListaOrigenes(){
	$.getJSON(server + 'CRUDOrigenPqrs?idoperacion=5', function(data){
		var origenes = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaOrigen  = data[i];
			str +='<option value="'+ cadaOrigen.nombreorigen +'" id ="'+ cadaOrigen.idorigen +'">' + cadaOrigen.nombreorigen +'</option>';
		}
		$('#selectOrigen').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectOrigen").val('');
	});
}

function getListaFocos(){
	$.getJSON(server + 'CRUDFocoPqrs?idoperacion=5', function(data){
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaFoco  = data[i];
			str +='<option value="'+ cadaFoco.nombrefoco +'" id ="'+ cadaFoco.idfoco +'">' + cadaFoco.nombrefoco +'</option>';
		}
		$('#selectFoco').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectFoco").val('');
	});
}


//Método que invoca el servicio para obtener lista de municipios parametrizados en el sistema
function getListaMunicipios(){

	$.getJSON(server + 'CRUDMunicipio?idoperacion=5', function(data){
		
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaMunicipio  = data[i];
			str +='<option value="'+ cadaMunicipio.nombre +'" id ="'+ cadaMunicipio.idmunicipio +'">' + cadaMunicipio.nombre +'</option>';
		}
		$('#selectMunicipio').html(str);
	});

}





function limpiarSeleccionCliente()
{
		$('#telefono').val("");
		$('#nombres').val("");
        $('#apellidos').val("");
        $('#direccion').val("");
        $('#zona').val("");
        $("#comentarios").val('');
        $("#selectTiendas").val("");
        $("#selectMunicipio").val("");
        $("#selectTipo").val("externa");
        $("#selectAreaResponsable").val("tienda");
        idCliente = 0;
        if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) 
        {
			table = $('#grid-clientes').DataTable();
			table.clear().draw();
		}
}

function existeFecha(fecha){
      var fechaf = fecha.split("/");
      var day = fechaf[0];
      var month = fechaf[1];
      var year = fechaf[2];
      var date = new Date(year,month,'0');
      if((day-0)>(date.getDate()-0)){
            return false;
      }
      return true;
}

function validarFechaMenorActual(date1){
      	var hoy = new Date();
		var dd = hoy.getDate();
		var mm = hoy.getMonth()+1; //January is 0!

		var yyyy = hoy.getFullYear();
		if(dd<10){
		    dd='0'+dd;
		} 
		if(mm<10){
		    mm='0'+mm;
		} 
		var date2 = dd+'/'+mm+'/'+yyyy;
      var fechaPedido = new Date();
      var fechaActual = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaPedido.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechaActual.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      
      if (fechaPedido >= fechaActual)
      {
        return true;
   	  }
      else
      {
        return false;
      }
}

function ConfirmarPQRS()
{
	var valida = ValidacionesDatos();
	if (valida != 1)
	{
		return;
	}
	var fechaSolicitud = $("#fecha").val();
	var tipoSolicitud = $("#selectSolicitud option:selected").val();
	//idCliente
	var tempTienda =  $("#selectTiendas option:selected").attr('id');
	var idOrigen = $("#selectOrigen option:selected").attr('id');
	var idFoco = $("#selectFoco option:selected").attr('id');
	var nombresEncode = $("#nombres").val();
	var apellidosEncode = $("#apellidos").val();
	var tel = $("#telefono").val();
	var direccionEncode = $("#direccion").val();
	var zonaEncode = $("#zona").val(); 
	var tempMunicipio = $("#selectMunicipio option:selected").attr('id');
	var comentarioEncode = $("#comentarios").val();
	var tipo = $("#selectTipo option:selected").val();
	var areaResponsable = $("#selectAreaResponsable option:selected").val();
	$.confirm(
	{
		'title'		: 'Confirmacion SolicitudPQRS',
		'content'	: 'Desea confirmar la inserción de la Solicitud PQRS.' ,
		'buttons'	: 
		{
			'Si'	: 
			{
				'class'	: 'blue',
				'action': function()
				{
					$.ajax(
					{
		    				url: server + 'InsertarSolicitudPQRS' , 
		    				dataType: 'json', 
		    				type: 'post', 
							data: 
							{'fechasolicitud' : fechaSolicitud,
							 'tiposolicitud' : tipoSolicitud,
							 'idcliente' : idCliente,
							 'idtienda' : tempTienda,
							 'nombres' : nombresEncode,
							 'apellidos' : apellidosEncode,
							 'telefono' : tel,
							 'direccion' : direccionEncode,
							 'zona' : zonaEncode,
							 'idmunicipio' : tempMunicipio,
							 'comentario' : comentarioEncode,
							 'idorigen' : idOrigen,
							 'idfoco' : idFoco,
							 'tipo' : tipo,
							 'arearesponsable' : areaResponsable
							}, 
		    				async: false, 
			    			success: function(data1)
			    			{
								var respuesta = data1[0];
								if(respuesta.idSolicitudPQRS > 0)
		    					{
		    						alert('Se ha insertado correctamente la solicitud PQRS número  ' + respuesta.idSolicitudPQRS);
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
													   	url: server + 'InsertarSolicitudPQRSImagenes?idsolicitudpqrs=' + respuesta.idSolicitudPQRS + '&rutaimagen=' + cadaResp.name, 
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
		    						$('#telefono').val('');
									$('#nombres').val('');
									$('#apellidos').val('');
									$('#direccion').val('');
									$('#zona').val('');
									$("#selectTiendas").val('');
									$("#selectOrigen").val('');
									$("#selectFoco").val('');
									$("#selectMunicipio").val(1);
									$("#comentarios").val('');
									$("#selectTipo").val("externa");
					    			$("#selectAreaResponsable").val("tienda");
									idCliente = 0;
									if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) 
									{
										table = $('#grid-clientes').DataTable();
										table.clear().draw();
									}
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
	//validamos campo de telefono
	var tele =  telefono.value;
	if (tele == '' || tele == null)
	{
		alert ('Debe ingresar un telefono de contacto para el cliente');
		return;
	}


	if (!/^([0-9])*$/.test(tele))
	{
      alert("El valor " + tele + " no es un número");
      return;
	}

	//validamos campo Nombres
	var nomb = nombres.value;
	if (nomb == '' || nomb == null)
	{
		alert ('Debe ingresar los nombres del Cliente');
		return;
	}
		//validamos campo Direccion
	var dir = direccion.value;
	if (dir == '' || dir == null)
	{
		alert ('Debe ingresar la dirección del cliente');
		return;
	}
	var tien = $("#selectTiendas option:selected").val();
	if (tien == '' || tien == null || tien == undefined)
	{
		alert ('Debe ingresar la tienda del cliente PQRS');
		return;
	}
	//Validamos la selección del origen de PQRS
	var origen = $("#selectOrigen option:selected").val();
	if (origen == '' || origen == null || origen == undefined)
	{
		alert ('Debe ingresar el origen de la PQRS');
		return;
	}
	//Validamos la selección del foco de PQRS
	var foco = $("#selectFoco option:selected").val();
	if (foco == '' || foco == null || foco == undefined)
	{
		alert ('Debe ingresar el foco de la PQRS');
		return;
	}
	var muni = $("#selectMunicipio option:selected").val();
	if (muni == '' || muni == null|| muni == undefined)
	{
		alert ('Debe ingresar el Municipio del Cliente del PQRS');
		return;
	}
	
	var coment = comentarios.value;
	if (coment == '' || coment == null || coment == undefined)
	{
		alert ('Debe ingresar el comentario del PQRS');
		return;
	}

	return(1);
}

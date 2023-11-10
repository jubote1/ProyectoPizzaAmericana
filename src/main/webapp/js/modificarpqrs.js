	

// Se definen las variables globales.
var server;
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
fulImgBox = document.getElementById("fulImgBox");
fulImg = document.getElementById("fulImg");
div = document.getElementById("img-gallery");
	


dtconsultasPQRS = $('#grid-consultaPQRS').DataTable( {
    		"aoColumns": [
    		{ "mData": "idconsultaPQRS" },
            { "mData": "fechasolicitud" },
            { "mData": "tiposolicitud" },
            { "mData": "cliente" },
            { "mData": "direccion" },
            { "mData": "telefono" },
            { "mData": "comentario" , "visible": false },
            { "mData": "municipio" },
            { "mData": "tienda" },
            { "mData": "nombreorigen" },
            { "mData": "nombrefoco" },
            { "mData": "accion" },
            { "mData": "tipo"  , "visible": false},
            { "mData": "arearesponsable" , "visible": false },
            { "mData": "imagenes" , "visible": false }
            ],
                	"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                if(aData.imagenes > 0 )
                {
                	$(nRow).css('background-color', '#008000');
                }else
                {
                }
    		}

    	} );			

//validamos el contenido del campo fecha del pedido y el evento que lo controlará
	//Se invoca servicio para traerse la información de los productos disponibles en el sistema
	// En resumen se invocan todos servicios que se encargan de llenar la data del formulario.
	getListaTiendas();
	getListaMunicipios();
	getListaOrigenes();
	getListaFocos();
	setInterval('validarVigenciaLogueo()',600000);

//Colocamos acción al DataTable en caso de dar clic sobre el DATATABLE
$('#grid-consultaPQRS tbody').on('click', 'tr', function () {
        datos = table.row( this ).data();
        $('#idSolicitudPQRS').val(datos.idconsultaPQRS);
        idSolicitudPQRS = datos.idconsultaPQRS;
        var fechaPQRS = new Date(datos.fechasolicitud + " 12:00:00 GMT-0500");
        $("#fecha").datepicker('setDate', fechaPQRS);
        $("#selectSolicitudpqrs").val(datos.tiposolicitud);
        $("#selectMunicipio").val(datos.municipio);
        $('#telefono').val(datos.telefono);
        $('#nombres').val(datos.cliente);
        $('#direccion').val(datos.direccion);
        $("#selectTiendaspqrs").val(datos.tienda);
        $("#selectOrigen").val(datos.nombreorigen);
        $("#selectFoco").val(datos.nombrefoco);
        $('#comentariosVista').val(datos.comentario);
        $('#tipo').val(datos.tipo);
        $('#selectAreaResponsable').val(datos.arearesponsable);
        //Posteriormente hacemos la consulta para las imagenes de la pqrs
		$.getJSON(server + 'ConsultarSolicitudPQRSImagenes?idsolicitudpqrs=' + datos.idconsultaPQRS , function(data1){
			//recibimos respueta que es un json con los nombres de todas las imagenes
			imagenes = data1;
			imgs = new Array(imagenes.length);
			for(var i = 0; i < imagenes.length;i++){
				var cadaResp  = imagenes[i];
				imgs[i] = cadaResp.rutaimagen;
			}
			//Una vez cargadas todas las imagenes realizamos la carga de las mismas
			agregarImagen();
		});
     });



	});


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
		default:
				location.href = server +"Index.html";
		    	break;
	}
		    		
}

// Método que se encarga luego de introducido un teléfono en el campo de teléfono del cliente llamar al servicio


// Método que invoca el servicio para listar las tiendas donde se pondrán tomar domicilios.
function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTiendaspqrs').html(str);
		str +='<option value="'+ 'TODAS' +'" id ="'+ 'TODAS' +'"  selected>' + 'TODAS' +'</option>';
		$('#selectTiendas').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		//$("#selectTiendas").val('');
	});
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

function validarFechas(date1, date2){
     
	  var fechaSolIni = new Date();
      var fechaSolFin = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaSolIni.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechaSolFin.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      
      if (fechaSolIni <= fechaSolFin)
      {
        return true;
   	  }
      else
      {
        return false;
      }
}




function consultarPQRS() 
{

	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var tienda = $("#selectTiendas option:selected").attr('id');
	var valida = ValidacionesDatos();
	var tipoSolicitud = $("#selectSolicitud option:selected").val()
	if(tipoSolicitud == "todos")
	{
		tipoSolicitud = "";
	}
	if (valida != 1)
	{
		return;
	}
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-consultaPQRS' ) ) {
    		table = $('#grid-consultaPQRS').DataTable();
    }
	$.getJSON(server + 'ConsultaIntegradaSolicitudesPQRS?fechainicial=' + fechaini +"&fechafinal=" + fechafin + "&tienda=" + tienda + "&tiposolicitud=" + tipoSolicitud, function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
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
			                    "accion": '<button type="button" class="btn btn-default btn-xs" onclick="mostrarModalObservacion(' +data1[i].idconsultaPQRS + ')"><i class="fas fa-cart-plus fa-2x"></i></button>',
			                    "tipo": data1[i].tipo,
			                    "arearesponsable": data1[i].arearesponsable,
			                    "imagenes": data1[i].imagenes
			                }).draw();
							}
					});
	limpiarConsultaPQRS();

}


function ValidacionesDatos()
{
	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var tienda = $("#selectTiendas option:selected").attr('id');
	if(fechaini == '' || fechaini == null)
	{
		alert ('La fecha inicial debe ser diferente a vacía');
		return;
	}

	if(fechafin == '' || fechafin == null)
	{
		alert ('La fecha final debe ser diferente a vacía');
		return;
	}
	if(existeFecha(fechaini))
	{
	}
	else
	{
		alert ('La fecha inicial no es correcta');
		return;
	}

	if(existeFecha(fechafin))
	{
	}
	else
	{
		alert ('La fecha final no es correcta');
		return;
	}
	if(validarFechas(fechaini, fechafin))
	{
	}
	else
	{
		alert ('La fecha inicial es mayor a la fecha final, favor corregir');
		return;
	}

	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}

	return(1);
}


function mostrarModalObservacion(idSolPQRS)
{
	idSolicitudPQRS = idSolPQRS;
	$('#adicionarObservacion').modal('show');
}


function guardarObservacionPQRS()
{
	var comentariosAd = encodeURIComponent($('#comentarios').val());
	if(comentariosAd == '')
	{
		$.alert('El comentario a adicionar debe tener algún texto.' );
		return;
	}
	var controlador = false;
	$.ajax({ 
    				url: server + 'AdicionarComentarioPQRS?pqrs=' + idSolicitudPQRS + "&comentario=" + comentariosAd, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var resultado = data;
						if(resultado[0].resultado == 'OK')
						{
							$.alert('Se ha insertado exitosamente el comentario.' );
							controlador = true;
						}else
						{
							$.alert('Se tuvo un ERROR al insertar el comentario' );
						}
					} 
		});
	if(controlador == true)
	{
		$('#comentarios').val('');
		idSolicitudPQRS = 0;
		$('#adicionarObservacion').modal('hide');
		consultarPQRS();
	}
	
}


function limpiarConsultaPQRS()
{
	    $('#idSolicitudPQRS').val('');
        $('#fecha').val('');
        $('#tipoSolicitud').val('');
        $('#telefono').val('');
        $('#nombres').val('');
        $('#direccion').val('');
        $('#municipio').val('');
        $("#tienda").val('');
        $("#origen").val('');
        $('#comentariosVista').val('');
}

function agregarImagen() {
 
 	$('#img-gallery').html('');
    imgs.forEach(item => {
        elemento = null;
		//validamos si el archivo es pdf
        if(item.includes('.pdf')){
            elemento =document.createElement('div');
            embed = document.createElement('embed');
            a =document.createElement('a');
            src = "http://172.19.0.25:4200/imagenes/" + item
            embed.src = src;
            embed.width = '320px';
            embed.height = '350px';
            a.href = src
            a.innerHTML =  "<br><strong>Ver documento completo</strong>"
            a.target="_blank"
            embed.style.display ='block';
            embed.className = "file"
            elemento.appendChild(embed)
            elemento.appendChild(a);
           
        }else{
            elemento = document.createElement('img');
            elemento.className = "file"
            src = "http://172.19.0.25:4200/imagenes/" + item
            elemento.src = src;
            elemento.onclick = function () { openFulImg(this.src) };

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

function AdicionarImagenesPQRS()
{
	if(idSolicitudPQRS == 0)
	{
		alert ('Debe haber una PQRS seleccionada para agregar las imagenes o archivos.');
	}else
	{
		var filestack = $('#file-1').fileinput('getFileList');
	    const fd = new FormData();

	    //Se guarda un array con las imagenes dentro de los datos del formulario
	    filestack.forEach(element => 
	    {
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
	        success: function(resp) 
	        {
	        	console.log(resp);
	            //En este punto deberemos insertar las imágenes
	            for(var i = 0; i < resp.length;i++)
	            {
						var cadaResp  = resp[i];
						console.log(cadaResp.name);
						$.ajax({ 
						   	url: server + 'InsertarSolicitudPQRSImagenes?idsolicitudpqrs=' + idSolicitudPQRS + '&rutaimagen=' + cadaResp.name, 
						   	dataType: 'json',
						   	type: 'post', 
						   	async: false, 
						   	success: function(data){
								    
							}
						});
						$('#file-1').fileinput('reset');
				}
				refrescarImagenes();																
	        }
	 	});
	}
}

function refrescarImagenes()
{
	$.getJSON(server + 'ConsultarSolicitudPQRSImagenes?idsolicitudpqrs=' + idSolicitudPQRS , function(data1){
			//recibimos respueta que es un json con los nombres de todas las imagenes
			imagenes = data1;
			imgs = new Array(imagenes.length);
			for(var i = 0; i < imagenes.length;i++){
				var cadaResp  = imagenes[i];
				imgs[i] = cadaResp.rutaimagen;
			}
			//Una vez cargadas todas las imagenes realizamos la carga de las mismas
			agregarImagen();
		});
}

function EditarPQRS()
{
	if(idSolicitudPQRS == 0)
	{
		alert ('No se ha seleccionado ninguna PQRS para Modificar');
	}else
	{
		var fechaSolicitud = $("#fecha").val();
		var tipoSolicitud = $("#selectSolicitudpqrs option:selected").val();
		//idCliente
		var tempTienda =  $("#selectTiendaspqrs option:selected").attr('id');
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
		$.confirm(
		{
			'title'		: 'Confirmacion Actualización SolicitudPQRS',
			'content'	: 'Desea confirmar la actualización de la Solicitud PQRS.' ,
			'buttons'	: 
			{
				'Si'	: 
				{
					'class'	: 'blue',
					'action': function()
					{
						$.ajax(
						{
			    				url: server + 'ActualizarSolicitudPQRS' , 
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
								 'arearesponsable' : areaResponsable,
								 'idsolicitudpqrs' : idSolicitudPQRS
								}, 
			    				async: false, 
				    			success: function(data1)
				    			{
									var respuesta = data1[0];
									if(respuesta.idSolicitudPQRS > 0)
			    					{
			    						alert('Se ha actualizado correctamente la solicitud PQRS número  ' + respuesta.idSolicitudPQRS);
			    						limpiarPQRS()
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
}

function DescartarPQRS()
{
	if(idSolicitudPQRS == 0)
	{
		alert ('No se ha seleccionado ninguna PQRS para Descartar');
	}else
	{
		$.confirm(
		{
			'title'		: 'Confirmacion Descartar SolicitudPQRS',
			'content'	: 'Desea confirmar que se va a descartar de la Solicitud PQRS.' ,
			'buttons'	: 
			{
				'Si'	: 
				{
					'class'	: 'blue',
					'action': function()
					{
						$.ajax(
						{
			    				url: server + 'DescartarSolicitudPQRS' , 
			    				dataType: 'json', 
			    				type: 'post', 
								data: 
								{
								 'idsolicitudpqrs' : idSolicitudPQRS
								}, 
			    				async: false, 
				    			success: function(data1)
				    			{
									var respuesta = data1[0];
									if(respuesta.idSolicitudPQRS > 0)
			    					{
			    						alert('Se ha descartado correctamente la solicitud PQRS número  ' + respuesta.idSolicitudPQRS);
			    						limpiarPQRS()
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
}


function limpiarPQRS()
{
	//antes del final
	$('#telefono').val('');
	$('#nombres').val('');
	$('#apellidos').val('');
	$('#direccion').val('');
	$('#zona').val('');
	$("#selectTiendaspqrs").val('');
	$("#selectOrigen").val('');
	$("#selectFoco").val('');
	$("#selectMunicipio").val(1);
	$("#comentariosVista").val('');
	$("#selectTipopqrs").val("externa");
	$("#selectAreaResponsable").val("tienda");
	if ( $.fn.dataTable.isDataTable( '#grid-consultaPQRS' ) ) 
	{
		table = $('#grid-consultaPQRS').DataTable();
		table.clear().draw();
	}
}
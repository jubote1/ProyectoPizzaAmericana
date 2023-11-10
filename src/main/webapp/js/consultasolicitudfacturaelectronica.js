	

var server;
var tiendas;
var table;
var tabledetalle;
var dtsolicitudes;
var idSolicitud = 0;
var administrador = "N";
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
var imgs;
var fulImgBox;  
var fulImg;
var div;


$(document).ready(function() {

	fulImgBox = document.getElementById("fulImgBox");
	fulImg = document.getElementById("fulImg");
	div = document.getElementById("img-gallery");
	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
	//Marcamos si es administrador para tomar ciertas acciones
	if(respuesta == 'OKA')
    {
        administrador = 'S';
    }else if(respuesta == 'OK')
    {
        administrador = 'N';
    }

	//Lo primero que realizaremos es validar si está logueado

	//Llenamos arreglo con los productos
	
	//cargarMapa();
	//Cargamos los productos tipo Pizza para el menu inicial
	
	//Final cargue productos pizza


    dtsolicitudes = $('#grid-solicitudfactura').DataTable( {
    		"aoColumns": 
    		[
    		{ "mData": "idsolicitud" },
            { "mData": "idpedidotienda" },
            { "mData": "idpedidocontact" },
            { "mData": "valor" },
            { "mData": "nit" },
            { "mData": "correo" },
            { "mData": "empresa" },
            { "mData": "telefono" },
            { "mData": "fechapedido" },
            { "mData": "fechasolicitud" },
            { "mData": "usuario" },
            { "mData": "estado" },
            { "mData": "accion" }
        	]
    	} );

   
    $('#grid-solicitudfactura').on('click', 'tr', function () {
        //Clareamos las imagenes para cargarlas
        $('#file-1').fileinput('reset');
    	var datos = table.row( this ).data();
    	idSolicitud = datos.idsolicitud;
    	$.getJSON(server + 'ConsultarSolicitudFacturaImagenes?idsolicitud=' + datos.idsolicitud , function(data1){
			//recibimos respueta que es un json con los nombres de todas las imagenes
			imagenes = data1;
			if(imagenes.length == 0)
			{
				alert ('La solicitud de factura no tiene imágenes anexas');
			}
			imgs = new Array(imagenes.length);
			for(var i = 0; i < imagenes.length;i++){
				var cadaResp  = imagenes[i];
				imgs[i] = cadaResp.rutaimagen;
			}
			//Una vez cargadas todas las imagenes realizamos la carga de las mismas
			agregarImagen();
		});
     } );
});


$(function(){
	
	setInterval('validarVigenciaLogueo()',600000);
	
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
		case 'OKP':
				break;	
		default:
				location.href = server +"Index.html";
		    	break;
	}
		    		
}


function consultarSolicitudes() 
{
	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var estado = $("#selectEstado").val();
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
	if(validarFechaMenorActual(fechaini, fechafin))
	{
	}
	else
	{
		alert ('La fecha inicial es mayor a la fecha final, favor corregir');
		return;
	}
	//Incluimos validación de cantidad de días de la consulta
	if(validarDiferenciaFechas(fechaini, fechafin))
	{
	}
	else
	{
		alert ('La diferencia entre la fecha Inicial y Final no puede ser mayor a 3 días');
		return;
	}
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-solicitudfactura' ) ) {
    		table = $('#grid-solicitudfactura').DataTable();
    }
	$.getJSON(server + 'ConsultarSolicitudesFacturaElectronica?fechainicial=' + fechaini +"&fechafinal=" + fechafin + "&estado=" + estado, function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								table.row.add({
									"idsolicitud": data1[i].idsolicitud,
									"idpedidotienda": data1[i].idpedidotienda,
									"idpedidocontact": data1[i].idpedidocontact, 
									"valor": data1[i].valor,
									"nit": data1[i].nit,
									"correo": data1[i].correo,
									"empresa": data1[i].empresa,
									"telefono": data1[i].telefono,
									"fechapedido": data1[i].fechapedido,
									"fechasolicitud": data1[i].fechasolicitud,
									"usuario": data1[i].usuario,
									"estado": data1[i].estado,
									"accion":'<input type="button" class="btn btn-default btn-xs" onclick="procesarSolicitudFactura(' +data1[i].idsolicitud + ')" value="Procesar Solicitud"></button>'
								}).draw();
							}
							
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

function validarFechaMenorActual(date1, date2){
      var fechaini = new Date();
      var fechafin = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaini.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechafin.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      
      if (fechaini > fechafin)
        return false;
      else
        return true;
}

function validarDiferenciaFechas(date1, date2){
      var fechaini = new Date();
      var fechafin = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaini.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechafin.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      var diferencia = fechafin - fechaini;
      var dias = diferencia/(1000*60*60*24);
      if(dias > 3)
      {
      	return(false);
      }else
      {
      	return(true);
      }
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

function anexarImagenesSolicitud()
{
	if(idSolicitud == 0)
	{
		alert ('Debe haber una solicitud seleccionada para agregar las imagenes o archivos para generación de la factura');
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
						   	url: server + 'InsertarSolicitudFacturaImagenes?idsolicitud=' + idSolicitud + '&rutaimagen=' + cadaResp.name, 
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

function procesarSolicitudFactura(solicitudProcesar)
{

	$.confirm({
			'title'		: 'Confirmacion Procesar solicitud Factura',
			'content'	: 'Desea confirmar que se va a marcar como procesada la Solicitud de Factura electrónica # ' + solicitudProcesar + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
						table.clear().draw();
						$.ajax({ 
						   	url: server + 'ProcesarSolicitudFactura?idsolicitud=' + solicitudProcesar, 
						   	dataType: 'json',
						   	type: 'post', 
						   	async: false, 
						   	success: function(data){
						   		consultarSolicitudes();
							}
						});
					}
				},
				'No'	: {
					'class'	: 'gray',
					'action': function(){}	// Nothing to do in this case. You can as well omit the action property.
				}
			}
		});
}

function refrescarImagenes()
{
	$.getJSON(server + 'ConsultarSolicitudFacturaImagenes?idsolicitud=' + idSolicitud , function(data1)
	{
		//recibimos respuesta que es un json con los nombres de todas las imagenes
		imagenes = data1;
		if(imagenes.length == 0)
		{
			alert ('La solicitud de factura no tiene imágenes anexas');
		}
		imgs = new Array(imagenes.length);
		for(var i = 0; i < imagenes.length;i++){
			var cadaResp  = imagenes[i];
			imgs[i] = cadaResp.rutaimagen;
		}
		//Una vez cargadas todas las imagenes realizamos la carga de las mismas
		agregarImagen();
	});
}
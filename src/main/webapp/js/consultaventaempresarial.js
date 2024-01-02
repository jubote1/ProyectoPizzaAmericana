	

var server;
var tiendas;
var table;
var tabledetalle;
var dtventas;
var dtresumen;
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


    dtventas = $('#grid-ventaempresarial').DataTable( {
    		"aoColumns": 
    		[
    		{ "mData": "idpedido" },
            { "mData": "idpedidotienda" },
            { "mData": "valor" },
            { "mData": "cliente" },
            { "mData": "nombrecompania" },
            { "mData": "fecha" },
            { "mData": "asesor" }
        	]
    	} );

    dtresumen = $('#grid-resumenventa').DataTable( {
    		"aoColumns": 
    		[
    		{ "mData": "asesor" },
            { "mData": "totalventa" },
            { "mData": "comision" }
        	]
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


function consultar() 
{
	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
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

	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-ventaempresarial' ) ) {
    		table = $('#grid-ventaempresarial').DataTable();
    }
	$.getJSON(server + 'ConsultarVentasEmpresariales?fechainicial=' + fechaini +"&fechafinal=" + fechafin, function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								table.row.add({
									"idpedido": data1[i].idpedido,
									"idpedidotienda": data1[i].idpedidotienda,
									"valor": data1[i].valor,
									"cliente": data1[i].cliente,
									"nombrecompania": data1[i].nombrecompania,
									"fecha": data1[i].fecha,
									"asesor": data1[i].asesor
								}).draw();
							}
							
					});

	var table2;
	if ( $.fn.dataTable.isDataTable( '#grid-resumenventa' ) ) {
    		table2 = $('#grid-resumenventa').DataTable();
    }
	$.getJSON(server + 'ConsultarResumenVentasEmpresariales?fechainicial=' + fechaini +"&fechafinal=" + fechafin, function(data2){
	                		
	                		table2.clear().draw();
							for(var i = 0; i < data2.length;i++){
								table2.row.add({
									"asesor": data2[i].asesor,
									"totalventa": data2[i].totalventa,
									"comision": data2[i].comision
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
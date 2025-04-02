	

var server;
var tiendas;
var table;
var productos;
var excepciones;
var idPedido = 0;
var idPedidoTienda = 0;
var valorPago = 0;
var idTienda = 0;
var fechaPedido = "";
var tienda = "";
var idCliente = 0;
var idEstadoPedido = 0;
var longitud = 0;
var latitud = 0;
var urlTienda ="";
var idformapago = 0;
var totalpedido;
var valorformapago;
var stringPixel;
var administrador = "N";
//Debemos traer la información de WOMPI
var wompiClavePublica = "";
var wompiClavePrivada = "";
var wompiAmbiente = "";
var wompiEndPoint = "";
var dtSolicitudes;


$(document).ready(function() {

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

 

	} );


$(function(){

	dtSolicitudes = $('#grid-solicitudes').DataTable( {
    		"aoColumns": [
    		{ "mData": "idsolicitud" },
            { "mData": "fecha" },
            { "mData": "origen" },
            { "mData": "idpedidotienda" },
            { "mData": "estado" },
            { "mData": "categoria" },
            { "mData": "valor_analizar" },
            { "mData": "valor_final" },
            { "mData": "telefono" },
            { "mData": "descripcion" }  
        ]
    	} );

	$('#grid-solicitudes').on('click', 'tr', function () {
		datosSolicitud = table.row( this ).data();
		$('#fechatransaccion').val(datosSolicitud.fecha);
		$('#origen').val(datosSolicitud.origen);
		$('#tienda').val($("#selectTiendas").val());
		$('#numeropedido').val(datosSolicitud.idpedidotienda);
		$('#categoria').val(datosSolicitud.categoria);
		$('#valoranalizar').val(datosSolicitud.valor_analizar);
		$('#telefono').val(datosSolicitud.telefono);
		$('#descripcion').val(datosSolicitud.descripcion);
		$('#valorfinal').val(datosSolicitud.valor_final);
	});
	
	getListaTiendas();
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

function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		str +='<option value="'+ 'TODAS' +'" id ="'+ 'TODAS' +'">' + 'TODAS' +'</option>';
		$('#selectTiendas').html(str);
	});
}




function consultarSolicitudes() 
{

	var fecha = $("#fecha").val();
	var tienda = $("#selectTiendas").val();
	var idtienda = $("#selectTiendas option:selected").attr('id');
	if(fecha == '' || fecha == null)
	{
		alert ('La fecha debe ser diferente a vacía');
		return;
	}
	if(existeFecha(fecha))
	{
	}
	else
	{
		alert ('La fecha no es correcta');
		return;
	}

	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-solicitudes' ) ) {
    		table = $('#grid-solicitudes').DataTable();
    }
	$.ajax({ 
                 url: server + 'ConsultarSolicitudConciliacion?fecha=' + fecha  + "&idtienda=" + idtienda , 
                 dataType: 'json', 
                 async: false, 
                 success: function(data2){ 
                    table.clear().draw();
					for(var i = 0; i < data2.length;i++){
						var cadaSolicitud  = data2[i];
						table.row.add(data2[i]).draw();
					}
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



function validarFechaPedido()
{
	var fecha = new Date()
	var dia = fecha.getDate();
	if(dia < 10)
	{
		dia = "0" + dia;
	}
	var mes = fecha.getMonth()+1;
	if(mes < 10)
	{
		mes = "0" + mes;
	}
	var fechaActual = fecha.getFullYear()+"-"+mes+"-" + dia;
	console.log(fechaActual);
	if(fechaActual == fechaPedido)
	{
		return(true);
	}else
	{
		return(false);
	}
}


function formatearFecha(date) {
    var d = new Date(date),
        month = '' + (d.getMonth() + 1),
        day = '' + d.getDate(),
        year = d.getFullYear();

    if (month.length < 2) 
        month = '0' + month;
    if (day.length < 2) 
        day = '0' + day;

    return [year, month, day].join('-');
}

function limpiarFormulario()
{
		$('#selectOrigen').val("");
        $('#selectTiendas').val('');
        $('#numeropedido').val('');
        $('#descripcion').val('');
        $('#selectCategoria').val('');
        $('#valoranalizar').val('');
        $('#telefono').val('');
}
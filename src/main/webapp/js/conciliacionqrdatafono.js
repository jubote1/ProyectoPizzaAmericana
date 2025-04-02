	

var server;
var tiendas;
var table;
var tabledetalle;
var dtpedido;
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




function solicitarConciliacion() 
{

	var fechaTran = $("#fechatransaccion").val();
	var origen = $("#selectOrigen").val();
	var tienda = $("#selectTiendas").val();
	var idtienda = $("#selectTiendas option:selected").attr('id');
	var numpedido = $("#numeropedido").val();
	var descripcion = encodeURIComponent($("#descripcion").val());
	var categoria = $("#selectCategoria").val();
	var valorAnalizar = $("#valoranalizar").val();
	var telefono = $("#telefono").val();
	if(fechaTran == '' || fechaTran == null)
	{
		alert ('La fecha Transaccion debe ser diferente a vacía');
		return;
	}
	if(existeFecha(fechaTran))
	{
	}
	else
	{
		alert ('La fecha de Transacción no es correcta');
		return;
	}

	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}

	if (origen == '' || origen == null)
	{

		alert ('El origen de la transacción no puede estar vacía');
		return;
	}

	if (numpedido == '' || numpedido == null)
	{

		alert ('El Numero Pedido no puede estar vacía');
		return;
	}
	if (descripcion == '' || descripcion == null)
	{

		alert ('La Descripcion no puede estar vacía');
		return;
	}
	if (categoria == '' || categoria == null)
	{

		alert ('La Categoria no puede estar vacía');
		return;
	}
	if (valorAnalizar == '' || valorAnalizar == null)
	{

		alert ('El valor a analizar no puede estar vacía');
		return;
	}
	if (telefono == '' || telefono == null)
	{

		alert ('El Telefono no puede estar vacía');
		return;
	}
	console.log(server + 'InsertarSolicitudConciliacion?fecha=' + fechaTran  + "&idtienda=" + idtienda +  "&numpedido=" + numpedido + "&origen=" + origen +"&descripcion=" + descripcion +"&categoria=" + categoria +"&valoranalizar=" + valorAnalizar + "&telefono=" + telefono);
	$.ajax({ 
                 url: server + 'InsertarSolicitudConciliacion?fecha=' + fechaTran  + "&idtienda=" + idtienda +  "&numpedido=" + numpedido + "&origen=" + origen +"&descripcion=" + descripcion +"&categoria=" + categoria +"&valoranalizar=" + valorAnalizar + "&telefono=" + telefono, 
                 dataType: 'json', 
                 async: false, 
                 success: function(data2){ 
                    console.log(data2);
                    resultado = data2;
                    if(resultado.respuesta)
                    {
                    	$.alert('Se ha insertado correctamente la solicitud de Conciliación.');
                    	limpiarFormulario();
                    }
                    console.log(resultado.respuesta);
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
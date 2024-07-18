	

var server;
var tiendas;
var table;
var table2;
var dtcancelaciones;
var dtpedidos;
var administrador = "N";
var imgs;
var fulImgBox;  
var fulImg;
var div;


$(document).ready(function() {

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


    dtcancelaciones = $('#grid-cancelaciones').DataTable( {
    		"aoColumns": 
    		[
    		{ "mData": "idpedido" },
    		{ "mData": "idpedidotienda" },
            { "mData": "idordencomercio" },
            { "mData": "fechaingreso" },
            { "mData": "fechadomiciliario" },
            { "mData": "fechaentregado" },
            { "mData": "fechacancelacion" }
        	]
    	} );

    dtpedidos = $('#grid-pedidos').DataTable( {
    		"aoColumns": 
    		[
    		{ "mData": "idpedido" },
    		{ "mData": "idpedidotienda" },
            { "mData": "idordencomercio" },
            { "mData": "fechaingreso" },
            { "mData": "tiempopedido" , "visible": false },
            { "mData": "fechadomiciliario" },
            { "mData": "tiemposalidadomiciliario" },
            { "mData": "fechaentregado" },
            { "mData": "tiempodomiciliopedido" },
            { "mData": "fechacancelacion" , "visible": false }
        	],
        	"order": [[ 0, "desc" ]],
        	"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
        		if(aData.fechaentregado != '')
        		{
        			$(nRow).css('background-color', '#008000');
        		}else if(aData.fechaentregado == '' && aData.fechadomiciliario == '' && aData.fechacancelacion == '' && aData.tiempopedido > 20  && aData.tiempopedido < 40)
        		{
        			$(nRow).css('background-color', '#FFFF00');
        		}else if(aData.fechaentregado == '' && aData.fechadomiciliario == '' && aData.fechacancelacion == ''  && aData.tiempopedido > 40)
        		{
        			$(nRow).css('background-color', '#FF0000');
        		}else if(aData.fechacancelacion != '')
        		{
        			$(nRow).css('background-color', '#FF0000');
        		}
    		}


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


function consultarCancelaciones() 
{
	var fechaini = $("#fechainicial").val();
	if(fechaini == '' || fechaini == null)
	{
		alert ('La fecha inicial debe ser diferente a vacía');
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
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-cancelaciones' ) ) {
    		table = $('#grid-cancelaciones').DataTable();
    }
	$.getJSON(server + 'ObtenerPedidosCancelados?fechainicial=' + fechaini, function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								table.row.add({
									"idpedido": data1[i].idpedido,
									"idpedidotienda": data1[i].idpedidotienda,
									"idordencomercio": data1[i].idordencomercio,
									"fechaingreso": data1[i].fechaingreso,
									"fechadomiciliario": data1[i].fechadomiciliario,
									"fechaentregado": data1[i].fechaentregado, 
									"fechacancelacion": data1[i].fechacancelacion
								}).draw();
							}
							
					});
	if ( $.fn.dataTable.isDataTable( '#grid-pedidos' ) ) {
    		table2 = $('#grid-pedidos').DataTable();
    }
	$.getJSON(server + 'ObtenerPedidosPlataformaMonitoreo?fechainicial=' + fechaini, function(data2){
	                		
	                		table2.clear().draw();
							for(var i = 0; i < data2.length;i++){
								table2.row.add({
									"idpedido": data2[i].idpedido,
									"idpedidotienda": data2[i].idpedidotienda,
									"idordencomercio": data2[i].idordencomercio,
									"fechaingreso": data2[i].fechaingreso,
									"tiempopedido": data2[i].tiempopedido,
									"fechadomiciliario": data2[i].fechadomiciliario,
									"tiemposalidadomiciliario": data2[i].tiemposalidadomiciliario,
									"fechaentregado": data2[i].fechaentregado,
									"tiempodomiciliopedido": data2[i].tiempodomiciliopedido,
									"fechacancelacion": data2[i].fechacancelacion
								}).draw();
							}
							table2
							    .page.len(-1) // set the length to -1
							    .draw();
							
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

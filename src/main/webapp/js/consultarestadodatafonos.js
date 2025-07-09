	

var server;
var tiendas;
var dtDatafono;
var dtPedidoDatafono;
var table1;
var table2;

var urlTienda ="";

$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

	//Definimos el datatable en el cual recibiremos la información de los estados pedido de la tineda
	//Definimos los colores de la fila de acuerdo al valor del campo estatus.
    dtDatafono = $('#grid-datafono').DataTable( {
    		"aoColumns": [
    		{ "mData": "datafono" }
        	]
    	} );

    dtPedidoDatafono = $('#grid-pedidodatafono').DataTable( {
    		"aoColumns": [
    		{ "mData": "idpedido" },	
    		{ "mData": "datafono" }
        	]
    	} );

    
	} );


$(function(){
	
	// Al momento del cargue de la página se consultan las tiendas y con estas se llena el seledt correspondiente.
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
		default:
				location.href = server +"Index.html";
		    	break;
	}
		    		
}
// Función que consume el servicio para obtener las tiendas y adicionalmente se encarga de pintarlas en un select html.
function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTiendas').html(str);
	});
}




//Método para consultar los estados de pedidos de las tiendas, toma la tienda seleccionada
//Consume un servicio para obtener la url de la tienda y el dsn de la tienda, posteriormente invoca el servicio en el sistema
// de la tienda y se encarga del resultado recibido en formato json, formatearlo en un datatable para su presentación.
function consultarEstadoDatafonos() 
{

	var tienda= $('#selectTiendas option:selected').attr('id');
	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}
	//
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-datafono' ) ) {
    		table1 = $('#grid-datafono').DataTable();
    }
    if ( $.fn.dataTable.isDataTable( '#grid-pedidodatafono' ) ) {
    		table2 = $('#grid-pedidodatafono').DataTable();
    }
    $.ajax({ 
	    				url: server + 'ObtenerUrlTienda?idtienda=' + tienda, 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data2){ 
	    					console.log(data2);
							urlTienda = data2[0].urltienda;
	                		dsnodbc = data2[0].dsnodbc;
	                		pos = data2[0].pos;
								
						} 
						});
    //
	$.getJSON(urlTienda + 'ConsultarEstadoDatafonos' , function(data1){
	                		
	                		table1.clear().draw();
	                		var datafonos;
	                		var datafonosUsados;
	                		try {
	                			datafonos = JSON.parse(data1.datafonos);
	                		}catch(error)
	                		{
	                			datafonos = data1.datafonos;
	                		}
	                		try{
	                			datafonosUsados = JSON.parse(data1.datafonosusados);
	                		}catch(error2)
	                		{
	                			datafonosUsados = data1.datafonosusados;
	                		}
							for(var i = 0; i < datafonos.length;i++){
								table1.row.add(datafonos[i]).draw();
							}

							table2.clear().draw();

							for(var i = 0; i < datafonosUsados.length;i++){
								table2.row.add(datafonosUsados[i]).draw();
							}
							
							$('#cantpedsinsalirdat').val(data1.cantpeddatinsalir);
					});


}

	

var server;
var tiendas;
var urlTienda ="";
var dtestadopedido ;

$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	dtestadopedido = $('#grid-estadospedido').DataTable( {
    		"aoColumns": [
    		{ "mData": "fechacambio" },
            { "mData": "estadoanterior" },
            { "mData": "estadoposterior" }
        ]} );
  
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
function consultarHistorialEstadosPedido() 
{

	var tienda= $('#selectTiendas option:selected').attr('id');
	var numeroPedido = $('#numeropedido').val();
	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}
	if (numeroPedido == '' || numeroPedido == null)
	{

		alert ('Se debe ingresar un numero de pedido');
		return;
	}
	var table;
	if ( $.fn.dataTable.isDataTable( '#grid-estadospedido' ) ) {
    		table = $('#grid-estadospedido').DataTable();
    }
    // servicio para obtener info tienda
    
    $.ajax({ 
	    				url: server + 'ObtenerUrlTienda?idtienda=' + tienda, 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data2){ 
							urlTienda = data2[0].urltienda;
						} 
			});

    $.ajax({ 
	    				url: urlTienda + 'ConsultarHistorialEstadosPedidoTienda?idpedidotienda=' + numeroPedido, 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data3){ 
							table.clear().draw();     		
							for(var i = 0; i < data3.length;i++){
								if(i == 0)
								{
									$('#usologistica').val(data3[i].logistica);
								}
								table.row.add(data3[i]).draw();
							}
							//Consultaremos el domiciliario que llevo el pedido
							$.ajax({ 
					    				url: urlTienda + 'ConsultarDomiciliarioPedidoTienda?idpedidotienda=' + numeroPedido, 
					    				dataType: 'json', 
					    				async: false, 
					    				success: function(data4){ 
											var nombreDomi = data4.domiciliario;
											$('#domiciliario').val(nombreDomi);
											$('#empresa').val(data4.empresa);
										}
								  });
						},
						error: function(XMLHttpRequest, textStatus, errorThrown) { 
                    		alert("No hay comunicación con la tienda, posiblemente el computador esta apagado.");
                    		return;
                		}
			});
    //
}

function limpiarBusqueda()
{
	$('#domiciliario').val("");
	$('#numeropedido').val("");
	dtestadopedido.clear().draw();
	$("#selectTiendas").val("");
}

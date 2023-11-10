	

var server;
var tiendas;
var dtestadopedido ;

var urlTienda ="";

$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

	//Definimos el datatable en el cual recibiremos la información de los estados pedido de la tineda
	//Definimos los colores de la fila de acuerdo al valor del campo estatus.
    dtestadopedido = $('#grid-pedidostienda').DataTable( {
    		"aoColumns": [
    		{ "mData": "domiciliario" },
            { "mData": "estatus" },
            { "mData": "transaccion" },
            { "mData": "horapedido" },
            { "mData": "tiempoenruta" },
            { "mData": "tiempototal" },
            { "mData": "tomadordepedido" },
            { "mData": "direccion" },
            { "mData": "telefono" },
            { "mData": "nombrecompleto" },
            { "mData": "formapago" }
        ],
                	"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
        		switch(aData.estatus){
		            case "Esperando":
		                $(nRow).css('background-color', '#FFCD3A')
		                break;
		            case "En Cocina":
		                $(nRow).css('background-color', '#CD853F')
		                break;
		            case "Programado":
		                $(nRow).css('background-color', '#FF0000')
		                break;
		            case "Finalizado":
		                $(nRow).css('background-color', '#9EA1F6')
		                break;
		            case "En Ruta":
		                $(nRow).css('background-color', '#36EE6E')
		                break;
		            
		        }
    		}

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
function consultarEstadosPedido() 
{

	var tienda= $('#selectTiendas option:selected').attr('id');
	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}
	//
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-pedidostienda' ) ) {
    		table = $('#grid-pedidostienda').DataTable();
    }
    var dsnodbc;
    var pos;
    // servicio para obtener info tienda
    
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
	$.getJSON(urlTienda + 'ConsultarEstadosPedidoTienda?dsnodbc=' + dsnodbc + "&pos=" + pos , function(data1){
	                		
	                		console.log(data1);
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								table.row.add(data1[i]).draw();
							}
							
					});


}

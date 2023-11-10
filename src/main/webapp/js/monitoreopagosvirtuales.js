	

var server;
var tiendas;
var dtestadopedido ;
var table;
var idPedido;
var idLink;
var tienda;
var idFormaPago;
var idCliente;
var totalNeto;
var dtLogWompi;
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
	

	//Definimos el datatable en el cual recibiremos la información de los estados pedido de la tineda
	//Definimos los colores de la fila de acuerdo al valor del campo estatus.
    table = $('#grid-pedidos').DataTable( {
    		"aoColumns": [
    		{ "mData": "idpedido" },
            { "mData": "tienda" },
            { "mData": "nombre" },
            { "mData": "telefono" },
            { "mData": "telefonocelular" },
            { "mData": "email" },
            { "mData": "totalneto" },
            { "mData": "idlink" },
            { "mData": "fechainsercion" },
            { "mData": "minutos" },
            { "mData": "idcliente"  , "visible": false },
            { "mData": "idformapago"  , "visible": false }
        ],
                	"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                if(aData.minutos <= 12)
                {
                	$(nRow).css('background-color', '#008000');
                }else
                {
                	$(nRow).css('background-color', '#FF0000');
                }
    		}

    	} );

    
	} );

	dtLogWompi = $('#grid-detalleWompi').DataTable( {
    		"aoColumns": [
    		{ "mData": "idlink" },
            { "mData": "fechahora" },
            { "mData": "evento" },
            { "mData": "estado" }
        ]
    	} );

	$('#grid-pedidos').on('click', 'tr', function () {
        datospedido = table.row( this ).data();
        idPedido = datospedido.idpedido;
        idLink = datospedido.idlink;
        totalNeto = datospedido.totalneto;
        tienda = datospedido.tienda;
        idFormaPago = datospedido.idformapago;
        idCliente = datospedido.idcliente;
        $('#numeropedido').val(idPedido);
        $('#recrearLink').attr('disabled', false);
        $('#reenviarNotificacion').attr('disabled', false);
        if ( $.fn.dataTable.isDataTable( '#grid-detalleWompi' ) ) {
    		dtLogWompi = $('#grid-detalleWompi').DataTable();
    	}
        $.getJSON(server + 'ConsultarLogEventoWompi?idlink=' + idLink, function(data1){
	                		dtLogWompi.clear().draw();
	                		for(var i = 0; i < data1.length;i++){
								dtLogWompi.row.add(data1[i]).draw();
							}
	                		
							
					});
        if(datospedido.minutos > 30)
        {
        	$('#obsGestion').attr('disabled', false);
        	console.log("intentando activar el boton");
        }
    });


$(function(){
	refrescarPagosVirtuales();
	obtenerParametrosWOMPI();
	setInterval('validarVigenciaLogueo()',600000);
	setInterval('refrescarPagosVirtuales()',60000);
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


//Método para consultar los estados de pedidos de las tiendas, toma la tienda seleccionada
//Consume un servicio para obtener la url de la tienda y el dsn de la tienda, posteriormente invoca el servicio en el sistema
// de la tienda y se encarga del resultado recibido en formato json, formatearlo en un datatable para su presentación.
function refrescarPagosVirtuales() 
{

	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-pedidos' ) ) {
    		table = $('#grid-pedidos').DataTable();
    }
	$.getJSON(server + 'ObtenerPedidosMonitoreoPagoVirtual' , function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								table.row.add(data1[i]).draw();
							}
							
					});


}


function reenviarNotificacion()
{
	if($('#idlink').val() != '')
	{
		$.confirm({
			'title'		: 'Confirmacion de Reenvío de información de Link de Pagos',
			'content'	: 'Desea confirmar que se envíe de nuevo la información para el pago del Pedido Número ' + idPedido + '?<br> Se enviaría mensaje de texto y correo en caso de tenerlo.',
			'type': 'dark',
				'typeAnimated': true,
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
						enviarNotificacionWompi(idLink, idCliente, "https://checkout.wompi.co/l/"+idLink, idFormaPago, idPedido);
					}
				},
				'No'	: {
					'class'	: 'gray',
					'action': function(){}	// Nothing to do in this case. You can as well omit the action property.
				}
			}
		});
	}else
	{
		$.alert("CUIDADO! No hay idLink generado para enviar, por lo cual se debería primero recrear el Link de Pagos, por lo tanto no se ha enviado ninguna información.");
	}
	
}

function recrearLink()
{
    $.confirm({
		'title'		: 'Confirmacion de Recreación de Link de Pagos',
		'content'	: 'Desea confirmar que se recree un nuevo Link de Pagos para el Pedido Número ' + idPedido + '<br> El anterior Link ya no deberá ser usado',
		'type': 'dark',
			'typeAnimated': true,
		'buttons'	: {
			'Si'	: {
				'class'	: 'blue',
				'action': function()
				{
					//Como habrá uso de WOMPI traemos las variables
					//Generamos el JSON con los valores para consumir el servicio
			        var dateExp = new Date();
			        dateExp.setDate(dateExp.getDate() + 1);
			        var expMes = dateExp.getMonth()+1;
			        if(expMes < 10)
			        {
			            expMes = "0" + expMes;
			        }
			        var expDia = dateExp.getDate();
			        if(expDia  < 10)
			        {
			            expDia = "0" + expDia;
			        }
			        var strFechaExp = dateExp.getFullYear() + "-" + expMes + "-" + expDia;
			        var jsonLinkPago = '{' +
			              '"amount_in_cents": ' + (totalNeto*100) +','+
			              '"currency": "COP",' + 
			              '"name": "Pizza Americana ' + tienda.toUpperCase() + ' ",'+
			              '"description": "Pedido #' + idPedido + '",'+
			              '"expires_at": "' + strFechaExp + 'T23:00:00.000Z",'+
			              '"redirect_url": "https://pizzaamericana.co/wompi/",'+
			              '"single_use": true,'+
			              '"sku": "' + idPedido + '",'+
			              '"collect_shipping": false'+
			            '}';
			        //Lanzamos la creación de la transacción
			        $.ajax({
			                url: wompiEndPoint + "payment_links",
			                headers: {'Authorization': "Bearer " + wompiClavePrivada},
			                dataType: 'json', 
			                type: 'post',
			                data: jsonLinkPago, 
			                contentType: "application/json; charset=utf-8",
			                async: false, 
			                success: function(dataLink){
			                    //Recuperaremos el valor del id para el envío del link al cliente
			                    var idLink = dataLink.data.id;
			                    $('#idlink').val(idLink);
			                    $('#linkparapago').val('https://checkout.wompi.co/l/' + idLink);
			                    //Vamos a refrescar el ikLink en el campo de texto
			                    enviarNotificacionWompi(idLink, idCliente, "https://checkout.wompi.co/l/"+idLink, idFormaPago, idPedido);
			                },
			                error: function(dataLinkError){
			                    alert('SE PRODUJO UN ERROR');
			                    console.log(dataLinkError);
			                    //process the JSON data etc
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

function obtenerParametrosWOMPI()
{

    $.getJSON(server + 'GetParametro?parametro=WOMPIAMBIENTE' , function(data2){
        wompiAmbiente = data2.valortexto;
        if(wompiAmbiente == 'P')
        {
            $.getJSON(server + 'GetParametro?parametro=WOMPIPRODUCCIONPUB' , function(data3){
                wompiClavePublica = data3.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPIPRODUCCIONPRI' , function(data4){
                wompiClavePrivada = data4.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPIENDPOINTP' , function(data5){
                wompiEndPoint = data5.valortexto;
            });




        }else if(wompiAmbiente == 'C')
        {
            $.getJSON(server + 'GetParametro?parametro=WOMPISANDBOXPUB' , function(data3){
                wompiClavePublica = data3.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPISANDBOXPRI' , function(data4){
                wompiClavePrivada = data4.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPIENDPOINTC' , function(data5){
                wompiEndPoint = data5.valortexto;
            });
        }
    });

}

function enviarNotificacionWompi(idLink, idCliNoti, linkPago, idFormaPago, idPed)
{
    $.getJSON(server + 'RealizarNotificacionWompi?idlink='+ idLink +'&idcliente='+ idCliNoti + '&linkpago=' + linkPago + '&idformapago=' + idFormaPago + '&idpedido=' + idPed , function(data1){
        var respuesta = data1[0].respuesta;
    });
}

function obsGestionLink()
{
	var observacion =  encodeURIComponent($("#observacion").val());
	$.getJSON(server + 'IngresarObsGestionLink?idpedido='+ idPedido +'&observacion='+ observacion , function(data1){
        $("#observacion").val("");
        $.alert("Se ha ingresado la observación de la situación del pedido # " + idPedido);
    });
}
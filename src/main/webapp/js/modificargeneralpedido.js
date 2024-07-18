	

var server;
var tiendas;
var table;
var tabledetalle;
var dtpedido;
var productos;
var excepciones;
var idPedido = 0;
var idTienda = 0;
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
//Informacion del pedido actual
var pedido;
var formas;
var idFormaPagoActual = 0;
var totalPedido = 0;
var telCelular = "";
var email = "";

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
	getFormasPago();
	setInterval('validarVigenciaLogueo()',600000);
	
});

//Método que invoca servicio para obtener las formas de pago parametrizados en el sistema.
function getFormasPago(){
	$.getJSON(server + 'CRUDFormaPago?idoperacion=5&tipopago=T', function(data){
		formas = data;
		var str = '';
		str +='<option value="'+ 'SINCAMBIO' +'" id ="SINCAMBIO">' + 'SIN CAMBIO' +'</option>';
		for(var i = 0; i < data.length;i++){
			var cadaForma  = data[i];
			str +='<option value="'+ cadaForma.idformapago +'" id ="'+ cadaForma.idformapago +'">' + cadaForma.nombre +'</option>';
		}
		$('#selectformapago').html(str);
		
	});
}


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



function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		str +='<option value="'+ 'SINCAMBIO' +'" id ="SINCAMBIO">' + 'SIN CAMBIO' +'</option>';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTiendasNueva').html(str);
	});
}



function consultarPedido() 
{

	var numpedido = $("#numeropedido").val();
	if (numpedido== '' || numpedido == null)
	{

		$.alert('Debe ingresar un número de pedido para realizar el cambio.');
		return;
	}
	$.getJSON(server + 'ConsultaPedido?numeropedido=' + numpedido , function(data1){	
		if(data1.idpedido == 0)
		{
			$.alert('El pedido o no existe o no es de la fecha actual.');
		}else if(data1.idpedido > 0)
		{
			//Antes de realizar todo el despliegue validamos que los estados del pedido sean los que permiten
			//cambiar
			var errorEstado = "";
			if(data1.estadopedido.trim() == "Finalizado")
			{
				
			}else
			{
				errorEstado = 'El estado del pedido debe ser finalizado. ';
			}
			if(data1.enviadopixel == 1)
			{
				errorEstado = errorEstado + 'Error el pedido ya fue enviado a Tienda o fue marcado como enviado a Tienda.';
			}
			if(errorEstado != "")
			{
				$.alert(errorEstado);
				return;
			}
			pedido = data1;
			//Aqui tendremos la lógica para mostrar la información de la consulta del pedido
			//Habilitamos el botón para poder cambiar la información
			$('#numeropedido').attr('disabled', true);
			$('#consultapedido').attr('disabled', true);
			$('#realizarotraconsulta').attr('disabled', false);
			$('#cambioPedido').attr('disabled', false);
			$('#cambioPedidoformapago').attr('disabled', false);
			$('#estadopedido').val(data1.estadopedido);
			$('#estadoenviotienda').val(data1.estadoenviotienda);
			for(var i = 0; i < tiendas.length;i++)
			{
				var cadaTienda = tiendas[i];
				if(cadaTienda.id == data1.idtienda)
				{
					$('#tiendaactual').val(cadaTienda.nombre);

				}
			}
		}
		//Aniado a esta consulta viene la recuperación de la forma de pago
        $.getJSON(server + 'ObtenerFormaPagoPedido?idpedido=' + numpedido, function(data2){
            		var respuesta = data2[0];
					$('#totalpedido').val(data2[0].valortotal);
					$('#totalpedidonuevo').val(data2[0].valortotal);
            		$('#valorpago').val(data2[0].valorformapago);
            		var valorDevolver =  data2[0].valorformapago - data2[0].valortotal;
            		$('#valordevolver').val(valorDevolver);
            		$('#formapago').val(data2[0].nombre);
            		$('#descuento').val(data2[0].descuento);
            		idFormaPagoActual = data2[0].idformapago;
					totalPedido = data2[0].valortotal;
					//Recuperemos la información del cliente para poder verificación si tenemos celular y correo electrónico
			        $.getJSON(server + 'GetClientePorID?idcliente=' + pedido.idcliente, function(data3){
				                		
			        		telCelular = data3[0].telefonocelular;
			        		email = data3[0].email;
					});
					
			});



	});
         		


}

function realizarOtraConsulta()
{
	limpiezaPantalla();
}

function realizarCambioPedido()
{
	var numpedido = $("#numeropedido").val();
	if (numpedido== '' || numpedido == null)
	{

		$.alert('Debe ingresar un número de pedido para realizar el cambio.');
		return;
	}
	//Validamos el cambio de la tienda 
	var idTiendaCambio = $("#selectTiendasNueva option:selected").attr('id');
	//Revisamos si esta seleccionado la opción SINCAMBIO
	if(idTiendaCambio == 'SINCAMBIO')
	{
		$.alert('No seleccionaste una tienda diferente a la actual.');
		return;
	}
	//Revisamos la validación si la tienda seleccionada es igual a la tienda actual
	if(idTiendaCambio == pedido.idtienda)
	{
		$.alert('No seleccionaste una tienda diferente a la actual.');
		return;
	}

	//Consumiremos servicio para los cambios del pedido
	$.getJSON(server + 'RealizarCambiosPedido?idpedido=' + numpedido+"&idtienda=" + idTiendaCambio + "&idcliente=" + pedido.idcliente , function(data2){
		if(data2.resultado == 'OK')
		{
			$.alert('El cambio ha sido satisfactorio.');
		}else
		{
			$.alert('ERROR en el cambio no pudo ser realizado.');
		}
		limpiezaPantalla();
	});

	//Realizamos la limpieza de la pantalla
	
}

function limpiezaPantalla()
{
	$("#numeropedido").val('');
	$('#estadopedido').val('');
	$('#estadoenviotienda').val('');
	$('#tiendaactual').val('');
	$('#consultapedido').attr('disabled', false);
	$('#realizarotraconsulta').attr('disabled', true);
	$('#cambioPedido').attr('disabled', true);
	$('#cambioPedidoformapago').attr('disabled', true);
	$('#numeropedido').attr('disabled', false);
	idFormaPagoActual = 0;
	totalPedido = 0;
	telCelular =  "";
	email = "";
	//Debemos de limpiar los campos de la forma de pago
	$('#totalpedido').val('');
	$('#valorpago').val('');
	$('#valordevolver').val('');
	$('#formapago').val('');
	$('#descuento').val('');
	$('#totalpedidonuevo').val('');
	$('#valorpagonuevo').val('');
	$('#valordevolvernuevo').val('');
	//Inicializamos las selecciones de los select
	$("#selectTiendasNueva").val('SINCAMBIO');
	$("#selectformapago").val('SINCAMBIO');
	$('input:radio[name=requiereDevuelta]')[1].checked = true;
}


//Se deja la acción al momento de seleccionar la forma de pago
$("#selectformapago").change(function(){
    	//Tomamos el idformapago seleccionado
    	var idformapago =  $("#selectformapago").val();
    	var tipoForma = "";
    	for(var i = 0; i < formas.length;i++){
    		var cadaForma  = formas[i];
    		if(idformapago == cadaForma.idformapago)
			{
                //Realizmos primero la validacion de si se tienen los datos para una forma de pago virtual
                if(cadaForma.virtual == 'S')
                {

                    if(telCelular == '')
                    {
                        $("#selectformapago").val(1);
                        $.alert("CUIDADO! Si la forma de pago es virtual debe tener diligenciado el teléfono celular y no lo tiene.");
                        return;
                    }
                }
				if(cadaForma.tipoformapago == 'TRANSFERENCIA' || cadaForma.tipoformapago == 'PAGO-ONLINE')
				{
					$("#valorpagonuevo").val($("#totalpedido").val());
					$("#valordevolvernuevo").val("0");
					$('input:radio[name=requiereDevuelta]')[0].checked = true;
					$('#valorpagonuevo').attr('disabled', true);
				}
				else
				{
					$('#valorpagonuevo').attr('disabled', false);
				}
				break;
			}
			
		}
    });


$("input[name=requiereDevuelta]:radio").click(function() { 

 		if($(this).val() == 'completo') {
 			$("#valorpagonuevo").val($("#totalpedidonuevo").val());
			$('#valorpagonuevo').attr('disabled', true);
			
 		}else  if($(this).val() == 'devuelta') {
 			$('#valorpagonuevo').attr('disabled', false);
 			$("#valorpagonuevo").val('0');
 		}
 		$("#valordevolvernuevo").val($("#valorpagonuevo").val() - $("#totalpedidonuevo").val() );
 	});

$("#valorpagonuevo").change(function(){
			$("#valordevolvernuevo").val($("#valorpagonuevo").val() - $("#totalpedidonuevo").val() );
            
	});


function realizarCambioFormaPago()
{
	var numpedido = $("#numeropedido").val();
	if (numpedido== '' || numpedido == null)
	{

		$.alert('Debe ingresar un número de pedido para realizar el cambio.');
		return;
	}
	//Validamos el cambio de la forma de pago
	var idFormaPagoCambio = $("#selectformapago option:selected").attr('id');
	//Revisamos si esta seleccionado la opción SINCAMBIO
	if(idFormaPagoCambio == 'SINCAMBIO')
	{
		$.alert('No seleccionaste una forma de pago diferente a la actual.');
		return;
	}
	//Revisamos la validación si la tienda seleccionada es igual a la tienda actual
	if(idFormaPagoCambio == idFormaPagoActual)
	{
		$.alert('No seleccionaste una forma de pago diferente a la actual.');
		return;
	}
	//Pasadas estas validaciones ya deberemos de validar la completitud de los datos
	var valordevolver =  $("#valordevolvernuevo").val();
	if(valordevolver < 0)
	{
		$.alert("El valor de pago debe ser mínimo el valor total");
	}
	//A partir de aca realizamos la actualización de la forma de pago nueva
	var idformapagoNueva =  $("#selectformapago").val();
	var valorformapagoNueva =  $("#valorpagonuevo").val();

	//A partir de este punto vamos  a verificar si el origen es forma de pago virtual y si el destino es forma de pago virtual
	var virtualOrigen = false;
	var virtualDestino = false;
	//Revisión del origen
	for(var i = 0; i < formas.length;i++)
	{
		var cadaForma  = formas[i];
		if(cadaForma.idformapago == idFormaPagoActual)
		{
			if(cadaForma.virtual == 'S')
			{
				virtualOrigen = true;
				break;
			}
		}
	}
	//Revisión del destino
	for(var i = 0; i < formas.length;i++)
	{
		var cadaForma  = formas[i];
		if(cadaForma.idformapago == idformapagoNueva)
		{
			if(cadaForma.virtual == 'S')
			{
				virtualDestino = true;
				//Realizamos la validación de si va a ser pagoVirtual deberemos de tener por lo menos celular.
				if(telCelular == '' || telCelular == null)
				{
					$.alert('Si desea pasar a forma de pago virtual, el cliente debe tener celular para enviar allí el mensaje de texto con el link de pago');
					return;
				}
				break;
			}
		}
	}

	//En este punto vamos a realizar la actualización de la forma de pago Nueva.
	$.getJSON(server + 'ActualizarFormaPago?idpedido=' + numpedido+"&idformapagonueva=" + idformapagoNueva + "&valorformapagonueva=" + valorformapagoNueva + "&valortotal=" + totalPedido + "&virtualorigen=" + virtualOrigen + "&virtualdestino=" + virtualDestino , function(data2){
		if(data2.resultado == 'OK')
		{
			var mensajeResultado = 'El cambio ha sido satisfactorio.'
			if(virtualDestino == true)
			{
				mensajeResultado = mensajeResultado + " Como el destino fue pago virtual, recuerde que debe de ir a pedidos registrados, consultar el pedido y recrear el link. Recuerda que el pedido si es de tienda virtual ya no lo verás en dicha consulta.";
			}
			$.alert(mensajeResultado);
		}else
		{
			$.alert('ERROR en el cambio no pudo ser realizado.');
		}
		limpiezaPantalla();
	});
}

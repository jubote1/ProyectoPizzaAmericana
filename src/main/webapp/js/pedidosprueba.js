	

var server;
var tiendas;
var table;
var dtpedido;
var productos;
var excepciones;
var idPedido = 0;
var idCliente = 0;
var idEstadoPedido = 0;



$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

	//Llenamos arreglo con los productos
	getTodosProductos();
	
	//Cargamos los productos tipo Pizza para el menu inicial
	
	//Final cargue productos pizza


    table = $('#grid-clientes').DataTable( {
    		"aoColumns": [
            { "mData": "idCliente" },
            { "mData": "tienda" },
            { "mData": "nombre" },
            { "mData": "direccion" },
            { "mData": "zona" },
            { "mData": "observacion" },
            { "mData": "telefono" }
        ]
    	} );

    dtpedido = $('#grid-pedido').DataTable( {
    		"aoColumns": [
    		{ "mData": "iddetallepedido" },
            { "mData": "pizza" },
            { "mData": "otroprod" },
            { "mData": "cantidad" },
            { "mData": "especialidad1" },
            { "mData": "especialidad2" },
            { "mData": "liquido" },
            { "mData": "adicion" },
            { "mData": "observacion" },
            { "mData": "valorunitario" },
            { "mData": "valortotal" }
        ]
    	} );

     
    $('#grid-clientes tbody').on('click', 'tr', function () {
        datos = table.row( this ).data();
        //alert( 'Diste clic en  '+datos.nombre+'\'s row' );
        $('#nombres').val(datos.nombre);
        $('#direccion').val(datos.direccion);
        $('#zona').val(datos.zona);
        $('#observacionDir').val(datos.observacion);
        $("#selectTiendas").val(datos.tienda);
     } );
 	

 	$("input[name=formapizza]:radio").click(function() { 
                if($(this).val() == 'mitad') {
                		
                		if($("input[name=tamanoPizza]:radio").is(':checked')) {
                				getEspecilidadesMitad();
                        	}
                        	else
                        	{
                        		alert("Debe seleccionar el tamaño de la pizza");
                        		$("input[name=formapizza]:radio").attr('checked', false);
                        		
                        	}	
                } else if($(this).val() == 'entera') {
                		if($("input[name=tamanoPizza]:radio").is(':checked')) {
                				getEspecilidadesEntera();
                        	}
                        	else
                        	{
                        		alert("Debe seleccionar el tamaño de la pizza");
                        		$("input[name=formapizza]:radio").attr('checked', false);
                        	}	

                } 
        });
 	
     	//Pintar las adiciones
		$("input[name=adicion]:radio").click(function() { 
	                if($(this).val() == 'siadicion') {
	                		
	                		if($("input[name=adicion]:radio").is(':checked')) {
	                			getAdicionProductos();	
	                				
	                        }
	                        else
	                        {
	                        	str='';
	                        	$('#pintarAdiciones').html(str);
	                        		
	                        }	
	                }else{
	                	str='';
	                    $('#pintarAdiciones').html(str);
	                }
	               
	        });

	} );



$(function(){
	console.log('hola');
	getListaTiendas();
	getExcepcionesPrecios();
	//valores por defecto
	//$('#adicion option[value="noadicio"]').attr('selected','selected');
	$('input:radio[name=adicion]:nth(1)').attr('checked',true);
	
});


function validarTelefono(){

	if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) {
    	table = $('#grid-clientes').DataTable();
    }
	
	$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaCliente  = data1[i];
				table.row.add(data1[i]).draw();
			}
		});
		
}


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

function getExcepcionesPrecios(){
	$.getJSON(server + 'getExcepcionesPrecio', function(data){
		excepciones = data;
		var str = '';
		str += '<option value="vacio">Sin Excepcion</option>';
		for(var i = 0; i < excepciones.length;i++){
			var cadaExcepcion  = data[i];
			str +='<option value="'+ cadaExcepcion.idexcepcion +'" id ="' + cadaExcepcion.idproducto +'">' + cadaExcepcion.descripcion +'</option>';
		}
		$('#selectExcepcion').html(str);

	});
	var selExcepcion;
	var idSelExcepcion;
	var selCodigoProducto;
	$('#selectExcepcion').bind('change', function(){
		selExcepcion = $(this).val();
		idSelExcepcion = $(this).children(":selected").attr("id");
		selCodigoProducto = $("input:radio[name=tamanoPizza]:checked").val();
		if (selCodigoProducto != 'otros')
		{

			if (selCodigoProducto != idSelExcepcion)
			{
				alert("La excepción no está relacionada con el producto seleccionado, por favor corrija su elección");
				$("#selectExcepcion").val("vacio");
				return;
			}
			else
			{
				$.getJSON(server + 'GetSaboresLiquidoExcepcion?idExcepcion=' + selExcepcion, function(data1){
                		console.log(data1);
                		var strGas='';
                		var strGas = '<h2>Gaseosa</h2>';
						for(var i = 0; i < data1.length;i++){
							var cadaLiq  = data1[i];
							strGas +='<div class="row"> ';
							strGas +='<div class="col-md-3"> ';
							strGas += '<div class="input-group">';
							strGas += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaLiq.idSaborTipoLiquido + '" id="' + cadaLiq.descripcionLiquido + '" name="liquido">' + cadaLiq.descripcionLiquido + '-' + cadaLiq.descripcionSabor +'</label>';
							strGas += '</div> </div> </div>';
						}
                		$('#frmgaseosas').html(strGas);
				});
			}
		}
		else
		{

			var selOtrosProductos = $("input:radio[name=otros]:checked").val();
			if(selOtrosProductos == '')
			{
				alert("Antes de seleccionar la Excepción de precio, debe terminar de seleccionar el producto");
				$("#selectExcepcion").val("vacio");
				return;
			}
			else
			{
				if (selOtrosProductos != idSelExcepcion)
				{
					alert("La excepción no está relacionada con el producto seleccionado, por favor corrija su elección");
					$("#selectExcepcion").val("vacio");
					return;
				}
				else
				{
					$.getJSON(server + 'GetSaboresLiquidoExcepcion?idExcepcion=' + selExcepcion, function(data1){
	                		console.log(data1);
	                		var strGas='';
	                		var strGas = '<h2>Gaseosa</h2>';
							for(var i = 0; i < data1.length;i++){
								var cadaLiq  = data1[i];
								strGas +='<div class="row"> ';
								strGas +='<div class="col-md-3"> ';
								strGas += '<div class="input-group">';
								strGas += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaLiq.idSaborTipoLiquido + '" id="' + cadaLiq.descripcionLiquido + '" name="liquido">' + cadaLiq.descripcionLiquido + '-' + cadaLiq.descripcionSabor +'</label>';
								strGas += '</div> </div> </div>';
							}
	                		$('#frmgaseosas').html(strGas);
					});
				}
			}
		}
		//console.log(selExcepcion + " " + idSelExcepcion);
	});
}

function getEspecilidadesMitad(){
	$.getJSON(server + 'GetEspecialidades', function(data){
		especialidades = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaEspe  = data[i];
			str +='<div class="row"> ';
			str +='<div class="col-md-2"> ';
			str += '<div class="input-group">';
			str += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaEspe.idespecialidad + '" id="' + cadaEspe.idespecialidad + '" name="mitad1">' + cadaEspe.abreviatura +'</label>';
			str += '</div> </div>';
			str += '<div class="col-md-2 col-md-offset-2">';
			str += '<div class="input-group">';
			str += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaEspe.idespecialidad + '" id="' + cadaEspe.idespecialidad + '" name="mitad2">' + cadaEspe.abreviatura +'</label>';
			str += '</div> </div> </div>';

		}
		$('#especialidades').html(str);
	});


}

function getEspecilidadesEntera(){

	$.getJSON(server + 'GetEspecialidades', function(data){
		especialidades = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaEspe  = data[i];
			str +='<div class="row"> ';
			str +='<div class="col-md-2"> ';
			str += '<div class="input-group">';
			str += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaEspe.idespecialidad + '" id="' + cadaEspe.abreviatura + '" name="mitad1">' + cadaEspe.abreviatura +'</label>';
			str += '</div> </div> </div>';

		}
		$('#especialidades').html(str);
	});
	
}

function getTodosProductos()
{
	$.getJSON(server + 'GetTodosProductos', function(data){
		productos = data;
		console.log(data);
		getPizzas();
	});
}

function getPizzas()
{

	var strPizza = '';
	var contadorFila = 1;
	for(var i = 0; i < productos.length;i++){
			var cadaProdu  = productos[i];
			if (cadaProdu.tipo == "PIZZA" )
			{
				if (contadorFila == 1)
				{
					strPizza +='<div class="row"> ';
					strPizza +='<div class="col-md-3"> ';
					strPizza += '<div class="input-group">';
					strPizza += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaProdu.idproducto + '" id="' + cadaProdu.idproducto + '" name="tamanoPizza">' + cadaProdu.nombre +'</label>';
					strPizza += '</div> </div>';
					
				}
				if (contadorFila == 2)
				{
					strPizza +='<div class="col-md-3 col-md-offset-3">';
					strPizza += '<div class="input-group">';
					strPizza += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaProdu.idproducto + '" id="' + cadaProdu.idproducto + '" name="tamanoPizza">' + cadaProdu.nombre +'</label>';
					strPizza += '</div> </div> </div>';
				}
				contadorFila = contadorFila + 1;
				if (contadorFila == 3)
				{
					contadorFila = 1;
				}
			}
			
	}
	strPizza += '<div class="row">' ;
	strPizza += '<div class="col-md-2">';
	strPizza += '<div class="input-group">';			  
	strPizza += ' <label><input type="radio" aria-label="..." value="otros" id="otros" name="tamanoPizza">Otros Productos</label>';					    
	strPizza += '</div> </div> </div>';					       
	$('#FormTamanoPizza').html(strPizza);
	$("input[name=tamanoPizza]:radio").click(function() { 
				var tamanoPizza = $("input:radio[name=tamanoPizza]:checked").val();
				
                if($(this).val() == 'otros') {
                		console.log('punto 2');
                		if($("input[name=tamanoPizza]:radio").is(':checked')) {
                				getOtrosProductos();
                				var str='';
                				$('#especialidades').html(str);
                				$('#frmgaseosas').html(str);
                        	}
                        	else
                        	{
                        		str='';
                        		$('#otrosproductos').html(str);
                        		
                        	}	
                }
                else
                {
                	//if($(this).val() == 'extragrande' || $(this).val() == 'grande'  || $(this).val() == 'mediana' || $(this).val() == 'pizzeta')
                	$('#otrosproductos').html('');
                	$.getJSON(server + 'GetSaboresLiquidoProducto?idProducto=' + tamanoPizza, function(data1){
                		console.log(data1);
                		var strGas='';
                		var strGas = '<h2>Gaseosa</h2>';
						for(var i = 0; i < data1.length;i++){
							var cadaLiq  = data1[i];
							strGas +='<div class="row"> ';
							strGas +='<div class="col-md-2"> ';
							strGas += '<div class="input-group">';
							strGas += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaLiq.idSaborTipoLiquido + '" id="' + cadaLiq.descripcionLiquido + '" name="liquido">' + cadaLiq.descripcionLiquido + '-' + cadaLiq.descripcionSabor +'</label>';
							strGas += '</div> </div> </div>';
						}
                		$('#frmgaseosas').html(strGas);
					});
					

                } 
        });
}

function getOtrosProductos()
{
	var str = '';
	for(var i = 0; i < productos.length;i++){
			var cadaProdu  = productos[i];
			if (cadaProdu.tipo == "OTROS" )
			{
				str +='<div class="row"> ';
				str +='<div class="col-md-2"> ';
				str += '<div class="input-group">';
				str += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaProdu.idproducto +'" id="' + cadaProdu.idproducto + '" name="otros">' + cadaProdu.nombre +'</label>';
				str += '</div> </div> </div>';
			}
		}
		$('#otrosproductos').html(str);
		$("input[name=otros]:radio").click(function() { 
			var idProductoOtros = $("input:radio[name=otros]:checked").val();
			$.getJSON(server + 'GetSaboresLiquidoProducto?idProducto=' + idProductoOtros, function(data1){
                		console.log(data1);
                		var strGas='';
                		var strGas = '<h2>Gaseosa</h2>';
						for(var i = 0; i < data1.length;i++){
							var cadaLiq  = data1[i];
							strGas +='<div class="row"> ';
							strGas +='<div class="col-md-3"> ';
							strGas += '<div class="input-group">';
							strGas += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaLiq.idSaborTipoLiquido + '" id="' + cadaLiq.descripcionLiquido + '" name="liquido">' + cadaLiq.descripcionLiquido + '-' + cadaLiq.descripcionSabor +'</label>';
							strGas += '</div> </div> </div>';
						}
                		$('#frmgaseosas').html(strGas);
			});
		});
}

function getAdicionProductos()
{
	var str = '';
	for(var i = 0; i < productos.length;i++){
			var cadaAdicion  = productos[i];
			if (cadaAdicion.tipo == "ADICION" )
			{
				str +='<div class="row"> ';
				str +='<div class="col-md-2"> ';
				str += '<label><input type="checkbox"' + '  value="'+ cadaAdicion.idproducto + '-' + cadaAdicion.nombre +'" name="' + cadaAdicion.idproducto + '">' + cadaAdicion.nombre +'</label>';
				str += '</div> </div>';
			}
		}
		
		$('#pintarAdiciones').html(str);
}

/*function getOtrosProductos()
{
	$.getJSON(server + 'GetOtrosProductos', function(data){
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaProdu  = data[i];
			str +='<div class="row"> ';
			str +='<div class="col-md-2"> ';
			str += '<div class="input-group">';
			str += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaProdu.idproducto + '-' + cadaProdu.nombre +' " id="' + cadaProdu.idproducto + '" name="otros">' + cadaProdu.nombre +'</label>';
			str += '</div> </div> </div>';

		}
		$('#otrosproductos').html(str);
	});
}*/

//Procedimiento Ejecutado con todas las validaciones necesarias previo a realizar la insercion del  pedido en la base de datos
function ConfirmarPedido()
{
	
	console.log("ojo antes de");
	$.confirm({
			'title'		: 'Confirmacion de Creación de Pedido',
			'content'	: 'Desea confirmar el Pedido Número' + idPedido + '<br />El Pedido pasará a estado  Finalizado?',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'FinalizarPedido?idpedido=' + idPedido , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								
							} 
						});
						console.log("despues de");


					}
				},
				'No'	: {
					'class'	: 'gray',
					'action': function(){}	// Nothing to do in this case. You can as well omit the action property.
				}
			}
		});
		var strClr = "";
	    $('#otrosproductos').html(strClr);
	    $('#especialidades').html(strClr);
	    $('#pintarAdiciones').html(strClr);
	    $('input:text[name=cantidad]').val('');
	    //Se reinician valores por defecto
	    $('input:radio[name=adicion]:nth(1)').attr('checked',true);
	    $('input:radio[name=tamanoPizza]').attr('checked',false);
	    $('input:radio[name=formapizza]').attr('checked',false);
	    $('input:radio[name=liquido]').attr('checked',false);
	    $("#selectExcepcion").val('vacio');
	    $('#observacionDir').val('');
	    $('#observacion').val('');
	    //Adicional al clareo de todo el pedido
	    $("#NumPedido").val('');
		$("#IdCliente").val('');
		$("#estadopedido").val('');
		$('#nombres').val('');
        $('#direccion').val('');
        $('#zona').val('');
        $('#observacionDir').val('');
        $("#selectTiendas").val('');

    
}

function ValidacionesDatos()
{
	//validamos campo de telefono
	var tele =  telefono.value;
	if (tele == '' || tele == null)
	{
		alert ('Debe ingresar un telefono para el cliente');
		return;
	}
	//validamos campo Nombres
	var nomb = nombres.value;
	if (nomb == '' || nomb == null)
	{
		alert ('Debe ingresar los nombres del Cliente');
		return;
	}
		//validamos campo Direccion
	var dir = direccion.value;
	if (dir == '' || dir == null)
	{
		alert ('Debe ingresar la dirección del cliente');
		return;
	}
	var tien = $("#selectTiendas option:selected").val();
	if (tien == '' || tien == null)
	{
		alert ('Debe ingresar la tienda del pedido');
		return;
	}
	var tamapizza = $("input:radio[name=tamanoPizza]:checked").val();
	if (tamapizza == '' || tamapizza == null)
	{
		alert ('Debe seleccionar entre Pizza u otros productos');
		return;
	}
	if (tamapizza == 'otros'){
		var otros = $("input:radio[name=otros]:checked").val();
		if (otros == '' || otros == null)
		{
			alert ('Usted seleccionó otros productos, por favor seleccione un producto de la categoría otros productos');
			return;	
		}
	}else{

		if ($("input:radio[name=formapizza]:checked").val() == 'mitad'){
			var especial1 =  $("input:radio[name=mitad1]:checked").val();
			if (especial1 == '' || especial1 == null)
			{
				alert ('Usted seleccionó Pizza mitad/mitad, por favor seleccione el sabor de la primera mitad');
				return;	
			}
			var especial2 =  $("input:radio[name=mitad2]:checked").val();
			if (especial2 == '' || especial2 == null)
			{
				alert ('Usted seleccionó Pizza mitad/mitad, por favor seleccione el sabor de la segunda mitad');
				return;	
			}
		}
		if ($("input:radio[name=formapizza]:checked").val() == 'entera'){
		
			var especial1 =  $("input:radio[name=mitad1]:checked").val();
			if (especial1 == '' || especial1 == null)
			{
				alert ('Usted seleccionó Pizza de un solo sabor, por favor seleccione el sabor de la Pizza');
				return;	
			}
		}

	}
	
	var cant  = cantidad.value;
	if (cant == '' || cant == null)
		{
			alert ('Por favor seleccione la cantidad del producto que desea ordenar');
			return;	
		}
		return(1);
}


function agregarProducto()
{
	var valida = ValidacionesDatos();
	if (valida != 1)
	{
		return;
	}
		var tempTienda =  $("#selectTiendas option:selected").val();
	if (idPedido == 0)
	{
		var respuesta;
		//Realizar el llamado síncrono
		//$.ajax({ 
    	//			url: server + 'GetInsertarClientePedido?telefono=' + telefono.value + "&nombres=" + nombres.value +  "&direccion=" + direccion.value + "&tienda=" +  tempTienda +  "&zona=" + zona.value + "&observacion=", 
    	//			dataType: 'json', 
    	//			async: false, 
    	//			success: function(data){ 
		//				respuesta = data[0];
		//				idCliente = respuesta.idcliente;
		//			} 
		//});

		// Se realiza inserción del encabezado del pedido
		
		//Se hizo neceasari hacerlo sincrono pues la insercion del detalle pedido, depende de la inserción del encabezado pedido
		
		/*$.getJSON(server + 'InsertarClienteEncabezadoPedido?telefono=' + telefono.value + "&nombres=" + nombres.value +  "&direccion=" + direccion.value + "&tienda=" +  tempTienda +  "&zona=" + zona.value + "&observacion="  , function(data){
		respuesta = data[0];
		idPedido = respuesta.idpedido;
		idCliente = respuesta.idcliente;
		console.log("id pedido " + idPedido + " id cliente " + idCliente);
		});*/

		$.ajax({ 
    				url: server + 'InsertarClienteEncabezadoPedido?telefono=' + telefono.value + "&nombres=" + nombres.value +  "&direccion=" + direccion.value + "&tienda=" +  tempTienda +  "&zona=" + zona.value + "&observacion=" + observacionDir.value, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						respuesta = data[0];
						idCliente = respuesta.idcliente;
						idPedido = respuesta.idpedido;
						idEstadoPedido = respuesta.idestadopedido;
						$("#estadopedido").val(respuesta.descripcionestadopedido);
											} 
		});
		$("#NumPedido").val(idPedido);
		$("#IdCliente").val(idCliente);
		
	}
	var tamanoPizza;
	var pizza;
	var otroProd;
	var cantidad;
	var especialidad1;
	var especial1;
	var especialidad2;
	var especial2;
	var codprod;
	var valorunitario = 0;
	var valortotal = 0;
	var observacion = '';
	var liquido = '';
	var idsabortipoliquido= 0;
	observacion = $("textarea#observacion").val();
	// Datatable que se manejará para el pedido
	table = $('#grid-pedido').DataTable();
	var excepcionPrecio;
	var adiciones = '';
	var adicion = '';
	var idDetallePedido = 0;
	// Manejo de las adiciones por producto
	if($("input:radio[name=adicion]:checked").val()=='noadicion')
	{
		adiciones = '--';
	}
	else
	{
		for(var i = 0; i < productos.length;i++)
		{
			var cadaProdu  = productos[i];
			if (cadaProdu.tipo == "ADICION" )
			{
				
				//console.log($("#"+cadaProdu.idproducto).is(':checked'));
				if ($('input:checkbox[name='+cadaProdu.idproducto+']').is(':checked'))
				{
					//console.log(cadaProdu.nombre);
					pizza = '--';
					codprod = cadaProdu.idproducto;
					cantidad = 1;
					especialidad1 = '--';
					especial1 = '--';
					especialidad2 = '--';
					especial2 = '--';
					liquido = '--';
					valorunitario = cadaProdu.preciogeneral;
					valortotal= cadaProdu.preciogeneral;
					otroProd = cadaProdu.idproducto+'-'+cadaProdu.nombre;
					adicion = cadaProdu.idproducto + '-' + cadaProdu.nombre;
					adiciones = adiciones + cadaProdu.idproducto + '-' + cadaProdu.nombre + '/';
					//Primero deberíamos hacer la validación que se pueda insertar en la tabla  de base de datos y de allí si lanzar 
					
					$.ajax({ 
    				url: server + 'InsertarDetallePedido?idproducto=' + codprod + "&idpedido=" + idPedido +"&especialidad1=" + especialidad1 + "&cantidad=" + cantidad +"&especialidad2=" + especialidad2 + "&valorunitario=" + valorunitario + "&valortotal=" + valortotal + "&adicion=" + adicion + "&observacion=" + observacion + "&idsabortipoliquido=" + idsabortipoliquido + "&idexcepcion=" + excepcionPrecio, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						respuesta = data[0];
						idDetallePedido = respuesta.iddetallepedido;
						console.log("id detalle pedido " +  idDetallePedido);
					} 
					});

					if (idDetallePedido > 0)
					{
						table.row.add( {
							"iddetallepedido": idDetallePedido,
					        "pizza":       pizza,
					        "otroprod":   otroProd,
					        "cantidad":    cantidad,
					        "especialidad1": especial1,
					        "especialidad2": especial2,
					        "liquido": liquido,
					        "observacion": observacion,
					        "adicion": '--',
					        "valorunitario": valorunitario,
					        "valortotal": valortotal
					    } ).draw();
					}else
					{
						alert("Se presentó error en la inserción del Detalle de Pedido");
						return;
					}
				}
			}
		}
	}
	//excepcionPrecio = $("#selectExcepcion option:selected" ).text();

	excepcionPrecio = $("#selectExcepcion option:selected").val();


	tamanoPizza = $("input:radio[name=tamanoPizza]:checked").val();
	cantidad = $('input:text[name=cantidad]').val();	
	if (tamanoPizza == 'otros'){
		pizza = '--';
		especialidad1 = '--';
		especial1 = '--';
		especialidad2 = '--';
		especial2 = '--';
		var tempVal = $("input:radio[name=liquido]:checked").val();
		if ( tempVal > 0 ) {
  			liquido = $("input:radio[name=liquido]:checked").val() +"-" + $("input:radio[name=liquido]:checked").attr('id');
  			idsabortipoliquido = $("input:radio[name=liquido]:checked").val();
		}
		otroProd = $("input:radio[name=otros]:checked").val();
		if (otroProd == null){
			alert ("Debe Seleccionar el producto diferente a pizza");
			return;
		}
		codprod = $("input:radio[name=otros]:checked").attr('id');
		for(var i = 0; i < productos.length;i++){
			var cadaProdu  = productos[i];
			if (cadaProdu.idproducto == codprod )
			{
				valorunitario = cadaProdu.preciogeneral;
				valortotal = valorunitario * cantidad;
			}
		}
		if (excepcionPrecio != 'vacio')
		{
			for(var j = 0; j < excepciones.length; j++)
			{
				var cadaExcepcion = excepciones[j];
				if(cadaExcepcion.idexcepcion == excepcionPrecio)
				{
					if(cadaExcepcion.idproducto == codprod )
					{
						valorunitario = cadaExcepcion.precio;
						valortotal = valorunitario * cantidad;
					}
					else{
						alert("La Excepcion de precio no esta asociada al producto seleccionado por favor corrija su elección");
						return;
					}
				}
			}
		}
	}else{
			
			pizza = tamanoPizza.toLowerCase();
			
			for(var i = 0; i < productos.length;i++){
				var cadaProdu  = productos[i];
				if (cadaProdu.idproducto == pizza )
				{
					codprod = cadaProdu.idproducto;
					pizza = codprod + "-" + cadaProdu.nombre;
					valorunitario = cadaProdu.preciogeneral;
					valortotal = valorunitario * cantidad;
					break;
				}
			}
			//
			if (excepcionPrecio != 'vacio')
			{
				for(var j = 0; j < excepciones.length; j++)
				{
					var cadaExcepcion = excepciones[j];
					if(cadaExcepcion.idexcepcion == excepcionPrecio)
					{
						if(cadaExcepcion.idproducto == codprod )
						{
							valorunitario = cadaExcepcion.precio;
							valortotal = valorunitario * cantidad;
						}
						else{
							alert("La Excepcion de precio no esta asociada al producto seleccionado por favor corrija su elección");
							return;
						}
					}
				}
			}
			//
			otroProd = '--';
			if ($("input:radio[name=formapizza]:checked").val() == 'mitad'){
				especialidad1 =  $("input:radio[name=mitad1]:checked").val();
				especial1 = $("input:radio[name=mitad1]:checked").val() + "-" + $("input:radio[name=mitad1]:checked").attr('id');
				especialidad2 =  $("input:radio[name=mitad2]:checked").val();
				especial2 = $("input:radio[name=mitad2]:checked").val() + "-" +$("input:radio[name=mitad2]:checked").attr('id');
				if(especialidad1 =='' || especialidad2==''){
					alert("Debe seleccionar las dos especialidades");
					return;
				}
			}else if ($("input:radio[name=formapizza]:checked").val() == 'entera'){
					especialidad1 =  $("input:radio[name=mitad1]:checked").val();
					especial1 = $("input:radio[name=mitad1]:checked").val() + "-" + $("input:radio[name=mitad1]:checked").attr('id');
					especialidad2 = "--";
					especial2 = '--';
					if (especialidad1 ==''){
						alert('Debe seleccionar especialidad de la Pizza');
					}
			}
	}
	
		if (cantidad == 0  || cantidad ==''){
			alert ('Debe ingresar la cantidad del Producto que va a ordenar');
		}
	var idDetallePedido = 0;
	var tempVal = $("input:radio[name=liquido]:checked").val();
	if ( tempVal > 0 ) {
  			liquido = $("input:radio[name=liquido]:checked").val() +"-" + $("input:radio[name=liquido]:checked").attr('id');
  			idsabortipoliquido = $("input:radio[name=liquido]:checked").val();
	}
	
	$.ajax({ 
    				url: server + 'InsertarDetallePedido?idproducto=' + codprod + "&idpedido=" + idPedido +"&especialidad1=" + especialidad1 +  "&cantidad=" + cantidad + "&especialidad2=" + especialidad2 + "&valorunitario=" + valorunitario + "&valortotal=" + valortotal + "&adicion=" + adiciones + "&observacion=" + observacion+"&idsabortipoliquido=" +idsabortipoliquido +"&idexcepcion=" + excepcionPrecio, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						respuesta = data[0];
						idDetallePedido = respuesta.iddetallepedido;
						console.log("id detalle pedido " +  idDetallePedido);
					} 
		});

	if (idDetallePedido > 0 )
	{
		table.row.add( {
			"iddetallepedido": idDetallePedido,
	        "pizza":       pizza,
	        "otroprod":   otroProd,
	        "cantidad":    cantidad,
	        "especialidad1": especial1,
	        "especialidad2": especial2,
	        "liquido": liquido,
	        "observacion": observacion,
	        "adicion": adiciones,
	        "valorunitario": valorunitario,
	        "valortotal": valortotal
	    } ).draw();
	    //Se requiere clarear la información de cada item
	    var strClr = "";
	    $('#otrosproductos').html(strClr);
	    $('#especialidades').html(strClr);
	    $('#pintarAdiciones').html(strClr);
	    $('input:text[name=cantidad]').val('');
	    //Se reinician valores por defecto
	    $('input:radio[name=adicion]:nth(1)').attr('checked',true);
	    $('input:radio[name=tamanoPizza]').attr('checked',false);
	    $('input:radio[name=formapizza]').attr('checked',false);
	    $('input:radio[name=liquido]').attr('checked',false);
	    $("#selectExcepcion").val('vacio');
	    $('#observacionDir').val('');
	    $('#observacion').val('');
	}else
	{
		alert("Se presentó error en la inserción del Detalle de Pedido");
	}

// PARA LOS BOTONES DE CAMBIAR CANTIDADES




}



function changeQuantity(qty) 
{
	var resultado = Number($('#cantidad').val())+Number(qty);
	if (resultado < 0 )
	{
			alert("La Cantidad no puede ser menor a cero");
	}
	else
	{


			$('#cantidad').val(Number($('#cantidad').val())+Number(qty));
	}
    //document.cart_quantity['quantity'].value = Number(document.cart_quantity['quantity'].value)+Number(qty);
    
}






	

// Se definen las variables globales.
var server;



// A continuación  la ejecucion luego de cargada la pagina
$(document).ready(function() {
	// Se obtienen la lista de tiendas
	getListaTiendas();
	setInterval('validarVigenciaLogueo()',600000);
	//setInterval('validarEstadoLogin()',300000);
	// Si se cambia el select de tiendas deberemos de recuperar el tiempo
	$("#selectTiendas").change(function(){
    	var tiempoactual;
		var idtien = $("#selectTiendas option:selected").attr('id');
		$.ajax({ 
			url: server + 'CRUDTiempoPedido?idoperacion=3&idtienda=' + idtien , 
			dataType: 'json',
			type: 'get', 
			async: false, 
			success: function(data2){
				tiempoactual = data2.tiempopedido;
				var valorLabel;
				if (tiempoactual <= 50)
			   	{
			   		valorLabel =  '<h1 style="color:#55FF33;">'+ tiempoactual + " MINUTOS" + '</h1>';
			   	}else if (tiempoactual > 50  & tiempoactual <= 80)
			   	{
			   		valorLabel =  '<h1 style="color:#AFFF33;">'+ tiempoactual + " MINUTOS" + '</h1>';
			   	}else if (tiempoactual > 80)
			   	{
			   		valorLabel =  '<h1 style="color:#FF5533;">'+ tiempoactual + " MINUTOS" + '</h1>';
			   	}
		    	$('#tiempoactual').html(valorLabel);			    
			} 
		});
    });

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

function validarCedulaEvento()
{
	var cedula = $('#cedula').val();
	if(cedula == '')
	{
		alert('La cédula no puede estar vacío');
		return;
	}
	$.ajax({ 
		url: server + 'ValidarIngresoSalidaEmpleado?cedula=' + cedula , 
		dataType: 'json',
		type: 'get', 
		async: false, 
		success: function(data3)
		{
			urlTienda = data3[0].urltienda;
			pos = data3[0].pos;
			$.getJSON(urlTienda + 'CRUDTiempoPedido?idoperacion=1&nuevotiempo='+nuevotiempo+"&pos=" + pos, function(data){
				alert('Se ha realizado actualización del tiempo en el sistema de Contact Center y en la tienda correspondiente');
			});
			
		} 
	});
}

function RegistrarEvento()
{

	var idtien = $("#selectTiendas option:selected").attr('id');
	var nuevotiempo = $('#nuevotiempo').val();
	$.ajax({ 
				url: server + 'CRUDTiempoPedido?idoperacion=1&nuevotiempo='+ nuevotiempo + "&idtienda=" + idtien , 
				dataType: 'json',
				type: 'get', 
				async: false, 
				success: function(data2){
					var respuesta = data2.resultado;
					if(respuesta == true)	
					{
						var valorLabel;
						if (nuevotiempo <= 50)
						{
							valorLabel =  '<h1 style="color:#55FF33;">'+ nuevotiempo + " MINUTOS" +'</h1>';
						}else if (nuevotiempo > 50  & nuevotiempo <= 80)
						{
							valorLabel =  '<h1 style="color:#AFFF33;">'+ nuevotiempo + " MINUTOS" +'</h1>';
						}else if (nuevotiempo > 80)
						{
							valorLabel =  '<h1 style="color:#FF5533;">'+ nuevotiempo + " MINUTOS" +'</h1>';
						}
						$('#tiempoactual').html(valorLabel);
						$('#nuevotiempo').val('');			
					}
					//Realizamos consumo en tienda para actualizar tiempo
					var urlTienda = "";
					var pos;
					$.ajax({ 
								url: server + 'ObtenerUrlTienda?idtienda=' + idtien , 
								dataType: 'json',
								type: 'get', 
								async: false, 
								success: function(data3)
								{
									urlTienda = data3[0].urltienda;
									pos = data3[0].pos;
									$.getJSON(urlTienda + 'CRUDTiempoPedido?idoperacion=1&nuevotiempo='+nuevotiempo+"&pos=" + pos, function(data){
										alert('Se ha realizado actualización del tiempo en el sistema de Contact Center y en la tienda correspondiente');
									});
									
								} 
							});
				} 
			});
}


function validarEstadoLogin()
{
			var getImport; 
			var getContent;
			//Obtenemos el valor de la variable server, con base en la URL digitada
			var loc = window.location;
			var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
			server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
			var respuesta ='';
			$.ajax({ 
		    	url: server + 'ValidarUsuarioAplicacion', 
		    	dataType: 'text',
		    	type: 'post', 
		    	async: false, 
		    	success: function(data){
						    respuesta =  data;		
				} 
			});
			switch(respuesta)
		    		{
		    			case 'OK':
		    				getImport = document.querySelector('#Menu-file');
		    				break;
		    			case 'OKA':
		    				getImport = document.querySelector('#MenuAdm-file');
		    				break;	
		    			default:
		    				location.href = server +"Index.html";
		    				break;
		    		}
		    getContent = getImport.import.querySelector('#menuprincipal');
			document.body.appendChild(document.importNode(getContent, true));
}
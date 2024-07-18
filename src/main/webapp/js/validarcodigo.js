	

// Se definen las variables globales.
var server;



// A continuación  la ejecucion luego de cargada la pagina
$(document).ready(function() {
	// Se obtienen la lista de tiendas
	setInterval('validarVigenciaLogueo()',600000);
	//Deshabilitamos el botón de usar código promocional
	$('#usarcodigo').attr('disabled', true);


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


function validarCodigo()
{
	//Realizamos un limpiado de los campos
	limpiarCampos();
	//Capturamos el código promocional que vamos a validar
	var codigoPromocional = $('#codigopromocional').val();
	$.ajax({ 
				url: server + 'GetValidarCodigoPromocional?codigopromocional='+ codigoPromocional , 
				dataType: 'json',
				type: 'get', 
				async: false, 
				success: function(data2)
				{
					if(data2.respuesta == 'OK')
					{
						if(data2.utilizado == 'N')
						{
							$('#estadocodigo').val('CÓDIGO PROMOCIONAL DISPONIBLE');
							$('#usarcodigo').attr('disabled', false);

						} else if(data2.utilizado == 'S')
						{
							$('#estadocodigo').val('CÓDIGO PROMOCIONAL YA UTILIZADO');
							$('#usarcodigo').attr('disabled', true);
						}
						$('#ingresooferta').val(data2.ingresooferta);
						$('#usooferta').val(data2.usooferta);
						$('#fechacaducidad').val(data2.fechacaducidad);
						
					}
					else if(data2.respuesta == 'NOK')
					{
						$('#estadocodigo').val('CÓDIGO PROMOCIONAL NO EXISTE');
						$('#usarcodigo').attr('disabled', true);
					}else if(data2.respuesta == 'VEN')
					{
						$('#estadocodigo').val('CÓDIGO PROMOCIONAL ESTA VENCIDO');
						$('#ingresooferta').val(data2.ingresooferta);
						$('#usooferta').val(data2.usooferta);
						$('#fechacaducidad').val(data2.fechacaducidad);
						$('#usarcodigo').attr('disabled', true);
					}
				} 
			});
}

function limpiarCampos()
{
	$('#estadocodigo').val('');
	$('#ingresooferta').val('');
	$('#usooferta').val('');
									
}

function usarCodigo()
{
	//Capturamos el código promocional que vamos a validar
	var codigoPromocional = $('#codigopromocional').val();
	$.ajax({ 
				url: server + 'GetValidarCodigoPromocional?codigopromocional='+ codigoPromocional, 
				dataType: 'json',
				type: 'get', 
				async: false, 
				success: function(data2)
				{
					if(data2.respuesta == 'OK')
					{
						if(data2.utilizado == 'N')
						{
							//En este punto realizaríamos la inserción del uso de la oferta
							$.ajax({ 
								url: server + 'GetUsarCodigoPromocional?idofertacliente='+ data2.idofertacliente , 
								dataType: 'json',
								type: 'get', 
								async: false, 
								success: function(data3)
								{
									if(data3[0].resultado == 'exitoso')
									{
										$.alert('Se ingresó correctamente el uso del código promocional');
										$('#codigopromocional').val('');
										$('#estadocodigo').val('');
										$('#ingresooferta').val('');
										$('#usooferta').val('');
										$('#usarcodigo').attr('disabled', true);
									
									}else
									{
										$.alert('Se TUVO ERROR ingresando el uso del código promocional');
									}
								} 
							});

						} else if(data2.utilizado == 'S')
						{
							
						}
						
						
					}
					else if(data2.respuesta == 'NOK')
					{
						$('#estadocodigo').val('CÓDIGO PROMOCIONAL NO EXISTE');
						
					}
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
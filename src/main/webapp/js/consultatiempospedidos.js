	

// Se definen las variables globales.
var server;



// A continuación  la ejecucion luego de cargada la pagina
$(document).ready(function() {

	//setInterval('validarEstadoLogin()',300000);
	obtenerTiempos();
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

function obtenerTiempos()
{
	$.ajax({ 
		    	url: server + 'CRUDTiempoPedido?idoperacion=2', 
		    	dataType: 'json',
		    	type: 'get', 
		    	async: false, 
		    	success: function(data){
		    					var str = "";
		    					var tiempoactual = 0;
		    					var tienda = "";
		    					str += '<table class="table table-bordered">';
		    					str += '<tbody>';
								for(var i = 0; i < data.length;i++)
								{
									
									str += '<tr>';
									tienda = data[i].tienda;
									tiempoactual = data[i].tiempopedido;
									str += '<td class="active"><h2>Tiempo Actual '+ tienda +' </h1> </td>';
									str += '<td><label>';
									if (tiempoactual <= 50)
							    	{
							    		str +=  '<h2 style="color:#55FF33;">'+ tiempoactual + " MINUTOS" + '</h2>';
							    	}else if (tiempoactual > 50  & tiempoactual <= 80)
							    	{
							    		str +=  '<h2 style="color:#AFFF33;">'+ tiempoactual + " MINUTOS" + '</h2>';
							    	}else if (tiempoactual > 80)
							    	{
							    		str +=  '<h2 style="color:#FF5533;">'+ tiempoactual + " MINUTOS" + '</h2>';
							    	}
							    	str += '</label></td>';
							    	str += '<td><h2>' +  data[i].estado + '</h2></td>';
							    	str += '</tr>';
								}
								str += '</tbody>';
								str += '</table>';
								$('#tiempos').html(str);

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
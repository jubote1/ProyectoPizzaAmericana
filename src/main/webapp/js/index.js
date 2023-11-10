/** index.js **/


function autenticar()
{
	var usuario =  $('#txtUsuario').val();
	var password =  $('#txtPassword').val();
	// 'GetIngresarAplicacion?txtUsuario=' + usuario + "&txtPassword=" + password
	$.ajax({ 
	    				url: server + 'GetIngresarAplicacion', 
	    				dataType: 'text',
	    				type: 'post', 
	    				data: {'txtUsuario' : usuario , 'txtPassword' : password }, 
	    				async: false, 
	    				success: function(data){ 
	    						if(data == 'OK')
	    						{
	    							location.href = server + "Pedidos.html";
	    						}
	    						else
	    						{
	    							alert(data);
	    							$('#txtPassword').val('');
	    						}
							} 
						});
}


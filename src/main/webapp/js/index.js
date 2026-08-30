/** index.js **/


/**
 * Valida las credenciales contra GetIngresarAplicacion y entra al sistema.
 *
 * El servicio responde el texto 'OK' cuando la autenticacion es correcta; en
 * cualquier otro caso responde el motivo, que se le muestra al usuario.
 */
function autenticar()
{
	var usuario =  $('#txtUsuario').val();
	var password =  $('#txtPassword').val();

	if(!usuario || !password)
	{
		mostrarError('Debe digitar el usuario y la contrasena.');
		return;
	}

	bloquearBoton(true);

	$.ajax({
		url: server + 'GetIngresarAplicacion',
		dataType: 'text',
		type: 'post',
		data: {'txtUsuario' : usuario , 'txtPassword' : password },
		success: function(data){
				if(data == 'OK')
				{
					location.href = server + "Pedidos.html";
				}
				else
				{
					mostrarError(data);
					$('#txtPassword').val('');
					$('#txtPassword').focus();
				}
			},
		error: function(){
				mostrarError('No hubo respuesta del servidor. Verifique la conexion e intente de nuevo.');
			},
		complete: function(){
				bloquearBoton(false);
			}
	});
}

/**
 * Muestra el mensaje en la tarjeta. Si la pagina no tiene el contenedor, cae al
 * alert de siempre, para no perder el aviso.
 *
 * @param mensaje texto a mostrar
 */
function mostrarError(mensaje)
{
	var contenedor = $('#mensajeError');
	if(contenedor.length === 0)
	{
		alert(mensaje);
		return;
	}
	contenedor.text(mensaje);
	contenedor.show();
}

/**
 * Evita el doble clic mientras la peticion esta en curso.
 *
 * @param bloquear true para deshabilitar el boton
 */
function bloquearBoton(bloquear)
{
	var boton = $('#btnIngresar');
	if(boton.length === 0)
	{
		return;
	}
	boton.prop('disabled', bloquear);
	boton.text(bloquear ? 'Ingresando...' : 'Ingresar');
}

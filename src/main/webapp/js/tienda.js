var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-tiendas').DataTable( {
    		"aoColumns": [
            { "mData": "idtienda" },
            { "mData": "dsn" },
            { "mData": "nombre" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<button type="button" class="btn btn-default btn-xs" onclick="eliminarTienda()">Eliminar</button> <button type="button" class="btn btn-default btn-xs" onclick="EditarTienda()">Editar</button>'
            }
        ]
    	} );
  	  	


  	  	//
  	  	$('#userForm')
        .bootstrapValidator({
            framework: 'bootstrap',
            icon: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            fields: {
                nombreedit: {
                    validators: {
                        notEmpty: {
                            message: 'El nombre de la tienda es requerido'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'El nombre de la tienda debe contener solo letras'
                        }
                    }
                },
                dsnedit: {
                    validators: {
                        notEmpty: {
                            message: 'El dsn de la tienda es requerida'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'El dsn de la tienda debe contener solo letras'
                        }
                    }
                }
            }
        })
        
    	//
		pintarTiendas();
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

function guardarTienda()
{
	var nombre = $('#nombre').val();
	var dsn= $('#dsn').val();
	var idtienda;
	$.getJSON(server + 'CRUDTienda?idoperacion=1&nombre=' + nombre + "&dsn=" + dsn, function(data){
		var respuesta = data[0];
		idtienda = respuesta.idtienda;
				
	});
}

function eliminarTienda(idtienda)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación Tienda',
			'content'	: 'Desea confirmar la eliminación de la Tienda ' + idtienda + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDTienda?idoperacion=3&idtienda=' + idtienda , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-tiendas' ) ) {
							    	table = $('#grid-tiendas').DataTable();
							    }
								
								pintarTiendas();

								//

								
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

function pintarTiendas()
{
	$.getJSON(server + 'CRUDTienda?idoperacion=6' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaTienda  = data1[i];
				table.row.add({
					"idtienda": data1[i].idtienda, 
					"dsn": data1[i].dsn,
					"nombre": data1[i].nombre, 
					"accion":'<button type="button" class="btn btn-default btn-xs" onclick="eliminarTienda(' +data1[i].idtienda+ ')"><i class="fas fa-eraser fa-2x"></i></button> <button type="button" onclick="editarTienda('+data1[i].idtienda +')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idtienda + '">Edición</button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarTienda(idtienda)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDTienda?idoperacion=4&idtienda=' + idtienda, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						$('#userForm')
				                .find('[name="idtiendaedit"]').val(respuesta.idtienda).end()
				                .find('[name="nombreedit"]').val(respuesta.nombre).end()
				                .find('[name="dsnedit"]').val(respuesta.dsn).end()
				                if(respuesta.alertarpedidos == 'S')
				                {
				                	$('#alertarpedidos').prop('checked', true);
				                }else
				                {
				                	$('#alertarpedidos').prop('checked', false);
				                }

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Tienda',
				                    message: $('#userForm'),
				                    show: false // We will show it manually later
				                })
				                .on('shown.bs.modal', function() {
				                    $('#userForm')
				                        .show()                             // Show the login form
				                        .bootstrapValidator('resetForm'); // Reset form
				                })
				                .on('hide.bs.modal', function(e) {
				                    // Bootbox will remove the modal (including the body which contains the login form)
				                    // after hiding the modal
				                    // Therefor, we need to backup the form
				                    $('#userForm').hide().appendTo('body');
				                })
				                .modal('show');
					} 
		});

		//
}

function confirmarEditarTienda()
{
	
           
            
                var idtienda =  $('input:text[name=idtiendaedit]').val();
                var nombre = $('input:text[name=nombreedit]').val();
                var dsn = $('input:text[name=dsnedit]').val();
                var alertarPedidos = "";
                if($('#alertarpedidos').is(':checked'))
                {
                	alertarPedidos = "S";
                }
                else
                {
                	alertarPedidos = "N";
                }
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDTienda?idoperacion=2&idtienda='+ idtienda+'&nombre=' + nombre + "&dsn=" + dsn+ "&alertarpedidos=" + alertarPedidos, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarTiendas();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('La tienda ha sido actualizada');
    				} 
			}); 
            //
            

        

}
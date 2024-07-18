var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-estadopedido').DataTable( {
    		"aoColumns": [
            { "mData": "idestadopedido" },
            { "mData": "descripcion" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarEstadoPedido()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarEstadoPedido()" value="Editar"></button>'
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
                descripcionedit: {
                    validators: {
                        notEmpty: {
                            message: 'La descripción del Estado es requerido'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'El nombre del Estado Pedido debe contener solo letras'
                        }
                    }
                }
            }
        })
        
    	//
		pintarEstadosPedidos();
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

function guardarEstadoPedido()
{
	var descripcion = $('#descripcion').val();
	alert("ojo" +  $('#descripcion').val());
	var idestadopedido;
	$.getJSON(server + 'CRUDEstadoPedido?idoperacion=1&descripcion=' + descripcion, function(data){
		var respuesta = data[0];
		idestaopedido = respuesta.idestadopedido;
				
	});
}

function eliminarEstadoPedido(idestadopedido)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación Estado Pedido',
			'content'	: 'Desea confirmar la eliminación del Estado Pedido ' + idestadopedido + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDEstadoPedido?idoperacion=3&idestadopedido=' + idestadopedido , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-estadopedido' ) ) {
							    	table = $('#grid-estadopedido').DataTable();
							    }
								
								pintarEstadosPedidos();

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

function pintarEstadosPedidos()
{
	$.getJSON(server + 'CRUDEstadoPedido?idoperacion=5' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaEstado  = data1[i];
				table.row.add({
					"idestadopedido": data1[i].idestadopedido, 
					"descripcion": data1[i].descripcion,
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarEstadoPedido(' +data1[i].idestadopedido + ')" value="Eliminar"></button> <input type="button" onclick="editarEstadoPedido('+data1[i].idestadopedido +')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idestadopedido + '" value="Edición"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarEstadoPedido(idestadopedido)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDEstadoPedido?idoperacion=4&idestadopedido=' + idestadopedido, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
									            
				            $('#userForm')
				                .find('[name="idestadopedidoedit"]').val(respuesta.idestadopedido).end()
				                .find('[name="descripcionedit"]').val(respuesta.descripcion).end()
				                			                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Estado Pedido',
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

function confirmarEditarEstadoPedido()
{
	
           
            
                var idestadopedido =  $('input:text[name=idestadopedidoedit]').val();
                var descripcion= $('input:text[name=descripcionedit]').val();
                // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDEstadoPedido?idoperacion=2&idestadopedido='+ idestadopedido+'&descripcion=' + descripcion , 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarEstadosPedidos();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('El Estado Pedido ha sido actualizada');
    				} 
			}); 
            //
            

        

}
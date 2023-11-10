var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

		//Lo primero que realizaremos es validar si está logueado
			
	    table = $('#grid-tipoLiquido').DataTable( {
    		"aoColumns": [
            { "mData": "idtipoliquido" },
            { "mData": "nombre" },
            { "mData": "capacidad" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarTipoLiquido()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarTipoLiquido()" value="Editar"></button>'
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
                            message: 'El nombre del tipo del liquido es requerido'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'El nombre del tipo del liquido debe contener solo letras'
                        }
                    }
                },
                capacidadedit: {
                    validators: {
                        notEmpty: {
                            message: 'La capacidad del tipo de liquido es requerida'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'La capacidad del tipo de liquido debe contener solo letras'
                        }
                    }
                }
            }
        })
        
    	//
		pintarTipoLiquido();
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

function guardarTipoLiquido()
{
	var nombre = $('#nombre').val();
	var capacidad = $('#capacidad').val();
	var idtipoliquido;
	$.getJSON(server + 'CRUDTipoLiquido?idoperacion=1&nombre=' + nombre + "&capacidad=" + capacidad, function(data){
		var respuesta = data[0];
		idtipoliquido = respuesta.idtipoliquido;
				
	});
}

function eliminarTipoLiquido(idtipoliquido)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación Tipo Liquido',
			'content'	: 'Desea confirmar la eliminación del Tipo Liquido ' + idtipoliquido + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDTipoLiquido?idoperacion=3&idtipoliquido=' + idtipoliquido , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-tipoLiquido' ) ) {
							    	table = $('#grid-tipoLiquido').DataTable();
							    }
								
								pintarTipoLiquido();

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

function pintarTipoLiquido()
{
	$.getJSON(server + 'CRUDTipoLiquido?idoperacion=6' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaTipo  = data1[i];
				table.row.add({
					"idtipoliquido": data1[i].idtipoliquido, 
					"nombre": data1[i].nombre, 
					"capacidad": data1[i].capacidad,
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarTipoLiquido(' +data1[i].idtipoliquido + ')" value="Eliminar"></button> <input type="button" onclick="editarTipoLiquido('+data1[i].idtipoliquido +')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idtipoliquido + '" value="Edición"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarTipoLiquido(idtipoliquido)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDTipoLiquido?idoperacion=4&idtipoliquido=' + idtipoliquido, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						
				            $('#userForm')
				                .find('[name="idtipoliquidoedit"]').val(respuesta.idtipoliquido).end()
				                .find('[name="nombreedit"]').val(respuesta.nombre).end()
				                .find('[name="capacidadedit"]').val(respuesta.capacidad).end()
				                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Tipo Liquido',
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

function confirmarEditarTipoLiquido()
{
	
           
            
                var idtipoliquido =  $('input:text[name=idtipoliquidoedit]').val();
                var nombre = $('input:text[name=nombreedit]').val();
                var capacidad = $('input:text[name=capacidadedit]').val();
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDTipoLiquido?idoperacion=2&idtipoliquido='+ idtipoliquido+'&nombre=' + nombre + "&capacidad=" + capacidad, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarTipoLiquido();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('El tipo Liquido ha sido actualizada');
    				} 
			}); 
            //
            

        

}
var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-especialidades').DataTable( {
    		"aoColumns": [
            { "mData": "idespecialidad" },
            { "mData": "abreviatura" },
            { "mData": "nombre" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarEspecialidad()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarEspecialidad()" value="Editar"></button>'
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
                            message: 'El nombre de la especialidad es requerido'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'El nombre de la especialidad debe contener solo letras'
                        }
                    }
                },
                abreviaturaedit: {
                    validators: {
                        notEmpty: {
                            message: 'La abreviatura de la especialidad es requerida'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'La abreviatura de la especialidad debe contener solo letras'
                        }
                    }
                }
            }
        })
        
    	//
		pintarEspecialidades();
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

function guardarEspecialidad()
{
	var nombre = $('#nombre').val();
	var abreviatura = $('#abreviatura').val();
	var idespecialidad;
	$.getJSON(server + 'InsertarEspecialidad?nombre=' + nombre + "&abreviatura=" + abreviatura, function(data){
		var respuesta = data[0];
		idespecialidad = respuesta.idespecialidad;
				
	});
}

function eliminarEspecialidad(idespecialidad)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación Especialidad',
			'content'	: 'Desea confirmar la eliminación de la Especialidad ' + idespecialidad + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDEspecialidad?idoperacion=3&idespecialidad=' + idespecialidad , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-especialidades' ) ) {
							    	table = $('#grid-especialidades').DataTable();
							    }
								
								pintarEspecialidades();

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

function pintarEspecialidades()
{
	$.getJSON(server + 'GetEspecialidades' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaEspecialidad  = data1[i];
				table.row.add({
					"idespecialidad": data1[i].idespecialidad, 
					"abreviatura": data1[i].abreviatura,
					"nombre": data1[i].nombre, 
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarEspecialidad(' +data1[i].idespecialidad + ')" value="Eliminar"></button> <input type="button" onclick="editarEspecialidad('+data1[i].idespecialidad +')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idespecialidad + '" value="Edición"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarEspecialidad(idespecialidad)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDEspecialidad?idoperacion=4&idespecialidad=' + idespecialidad, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						console.log(respuesta.idespecialidad);
				            console.log(respuesta.nombre);
				            $('#userForm')
				                .find('[name="idespecialidadedit"]').val(respuesta.idespecialidad).end()
				                .find('[name="nombreedit"]').val(respuesta.nombre).end()
				                .find('[name="abreviaturaedit"]').val(respuesta.abreviatura).end()
				                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Especialidad',
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

function confirmarEditarEspecialidad()
{
	
           
            
                var idespecialidad =  $('input:text[name=idespecialidadedit]').val();
                var nombre = $('input:text[name=nombreedit]').val();
                var abreviatura = $('input:text[name=abreviaturaedit]').val();
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDEspecialidad?idoperacion=2&idespecialidad='+ idespecialidad+'&nombre=' + nombre + "&abreviatura=" + abreviatura, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarEspecialidades();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('La especialiad ha sido actualizada');
    				} 
			}); 
            //
            

        

}
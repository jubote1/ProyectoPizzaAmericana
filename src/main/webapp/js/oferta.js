var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
		//Lo primero que realizaremos es validar si está logueado
		
		
	    table = $('#grid-oferta').DataTable( {
    		"aoColumns": [
            { "mData": "idoferta" },
            { "mData": "nombre" },
            { "mData": "idexcepcion" },
            { "mData": "nombreexcepcion" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<button type="button" class="btn btn-default btn-xs" onclick="eliminarOferta()">Eliminar</button> <button type="button" class="btn btn-default btn-xs" onclick="EditarOferta()">Editar</button>'
            }
        ]
    	} );
  	  	//Pintar las ofertas que se encuentran en el sistema		
        pintarOfertas();
        // Llenar select de las excepciones de precio
        llenarSelectExcepciones();
        setInterval('validarVigenciaLogueo()',600000);
  	  	
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
                            message: 'El nombre de la oferta es requerido'
                        }
                    }
                }
            }
        }) 
         
        
    	//
		
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

function guardarOferta()
{
	
    
	var nombreoferta = encodeURIComponent($('#nombre').val());
	var idexcepcion = $('#selectExcepcion').val();

	$.getJSON(server + 'CRUDOferta?idoperacion=1&idexcepcion=' + idexcepcion + "&nombreoferta=" + nombreoferta, function(data){
		var respuesta = data[0];
		idoferta= respuesta.idoferta;
		$('#nombre').val('');
		$('#selectExcepcion').val('');
		$('#addData').modal('hide');
		pintarOfertas();
	});
	
}

function eliminarOferta(idoferta)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación de la Oferta',
			'content'	: 'Desea confirmar la eliminación de la Oferta ' + idoferta+ '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDOferta?idoperacion=3&idoferta=' + idoferta , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-oferta' ) ) {
							    	table = $('#grid-oferta').DataTable();
							    }
								
								pintarOfertas();

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

function llenarSelectExcepciones()
{
	
	$.ajax({ 
	    				url: server + 'getExcepcionesPrecio' , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data1)
	    					{ 
								var str = '';
								for(var i = 0; i < data1.length;i++){
									str +='<option value="'+ data1[i].idexcepcion +'" id ="'+ data1[i].idexcepcion +'">' + data1[i].descripcion + '</option>';
								}
								str += '<option value="0" id ="0"></option>';
								$('#selectExcepcion').html(str);
								$('#selectExcepcionedit').html(str);
													
							} 
						});

	
}


function pintarOfertas()
{
	$.getJSON(server + 'CRUDOferta?idoperacion=6'  , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaExcepcion = data1[i];
				table.row.add({
					"idoferta": data1[i].idoferta, 
					"nombre": data1[i].nombreoferta,
					"idexcepcion": data1[i].idexcepcion, 
					"nombreexcepcion": data1[i].nombreexcepcion, 
					"accion":'<button type="button" class="btn btn-default btn-xs" onclick="eliminarOferta(' +data1[i].idoferta + ')"><i class="fas fa-eraser fa-2x"></i>Eliminar</button> <button type="button" onclick="editarOferta('+data1[i].idoferta+')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idoferta + '"><i class="fas fa-pizza-slice fa">Edicion</button>' + '<button type="button" onclick="enviarMensaje('+data1[i].idoferta+')" class="btn btn-success btn-xs" ' + 'data-id="' + data1[i].idoferta + '">Enviar Mensaje</button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}


//Se deberá consumir un servicio para el envío de los mensajes de texto pendientes
function enviarMensaje(idOferta)
{

	$.confirm({
		'title'		: 'Confirmacion de Envío Mensajes',
		'content'	: 'Desea confirmar el envío de mensajes de la Oferta ' + idOferta+ '.',
		'buttons'	: {
			'Si'	: {
				'class'	: 'blue',
				'action': function(){
					$.ajax({ 
		    				url: server + 'GetEnviarMensajesOferta?idoferta='+ idOferta , 
		    				dataType: 'json', 
		    				async: false, 
		    				success: function(data){
		    					if(data.respuesta == 'exitoso')
		    					{
		    						$.alert('Los mensajes de Texto se han enviado Correctamente. ' + 'Se han enviado ' + data.mensajes + ' mensajes.');
		    					}else
		    					{
		    						$.alert('Se tuvieron inconvenientes para el envío de los mensajes de Texto');
		    					}
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

function editarOferta(idoferta)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDOferta?idoperacion=4&idoferta=' + idoferta, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						$('#userForm')
				                .find('[name="idofertaedit"]').val(respuesta.idoferta).end()
				                .find('[name="selectExcepcionedit"]').val(respuesta.idexcepcion).end()
				                .find('[name="nombreedit"]').val(respuesta.nombreoferta).end()
				                	                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Oferta',
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

function confirmarEditarOferta()
{
	
           
            
                var idoferta =  $('input:text[name=idofertaedit]').val();
                var nombreoferta = $('input:text[name=nombreedit]').val();
                var idexcepcion =  $('#selectExcepcionedit').val();
               
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDOferta?idoperacion=2&idoferta='+ idoferta+'&nombreoferta=' + nombreoferta + "&idexcepcion=" + idexcepcion , 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarOfertas();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('La oferta ha sido actualizada');
    				} 
			}); 
            //
            

        

}
var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-saborTipoLiquido').DataTable( {
    		"aoColumns": [
            { "mData": "idsabortipoliquido" },
            { "mData": "descripcion" },
            { "mData": "idtipoliquido"  , "visible": false},
            { "mData": "nombreliquido" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarSaborTipoLiquido()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarSaborTipoLiquido()" value="Editar"></button>'
            }
        ]
    	} );
  	  	
	    
		
        pintarSaborTipoLiquido();

        // Llenar select de productos y de liquidos
        llenarSelectTipoLiquidos();
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
                descripcionedit: {
                    validators: {
                        notEmpty: {
                            message: 'La descripción del sabor por tipo de liquido es requerida'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'La descripción del sabor por tipo de liquido debe contener solo letras'
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

function guardarSaborTipoLiquido()
{
	var descripcion =  $('#descripcion').val();
	var tipoliquido = $('#selectTipoLiquido').val(); 
	var idsabortipoliquido;
	$.getJSON(server + 'CRUDSaborTipoLiquido?idoperacion=1&'  + "&descripcion=" + descripcion + "&idtipoliquido=" + tipoliquido, function(data){
		var respuesta = data[0];
		idsabortipoliquido = respuesta.idsabortipoliquido;
				
	});
	pintarSaborTipoLiquido();
}

function eliminarSaborTipoLiquido(idsabortipoliquido)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación del Sabor por tipo de liquido',
			'content'	: 'Desea confirmar la eliminación del sabor por tipo de liquido ' + idsabortipoliquido+ '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDSaborTipoLiquido?idoperacion=3&idsabortipoliquido=' + idsabortipoliquido , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-saborTipoLiquido' ) ) {
							    	table = $('#grid-sabortipoLiquido').DataTable();
							    }
								
								pintarSaborTipoLiquido();

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

function llenarSelectTipoLiquidos()
{
	
	$.ajax({ 
	    				url: server + 'GetTipoLiquido' , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data2)
	    					{ 
								var str = '';
								str += '<option value="0" id ="0"></option>';
								for(var i = 0; i < data2.length;i++){
									str +='<option value="'+ data2[i].idtipo_liquido +'" id ="'+ data2[i].idtipo_liquido +'">' + data2[i].nombre + '-' + data2[i].capacidad + '</option>';
								}
								$('#selectTipoLiquido').html(str);
								$('#selectTipoLiquidoedit').html(str);	
								console.log(str);		
							} 

						});

}

function pintarSaborTipoLiquido()
{
	$.getJSON(server + 'CRUDSaborTipoLiquido?idoperacion=6'  , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaSabor = data1[i];
				table.row.add({
					"idsabortipoliquido": data1[i].idsabortipoliquido, 
					"descripcion": data1[i].descripcion, 
					"idtipoliquido": data1[i].idtipoliquido,  
					"nombreliquido": data1[i].nombreliquido, 
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarSaborTipoLiquido(' +data1[i].idsabortipoliquido + ')" value="Eliminar"></button> <input type="button" onclick="editarSaborTipoLiquido('+data1[i].idsabortipoliquido+')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idsabortipoliquido + '" value="Edición"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarSaborTipoLiquido(idsabortipoliquido)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDSaborTipoLiquido?idoperacion=4&idsabortipoliquido=' + idsabortipoliquido, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						$('#userForm')
				                .find('[name="idsabortipoliquidoedit"]').val(respuesta.idsabortipoliquido).end()
				                .find('[name="descripcionedit"]').val(respuesta.descripcion).end()
				                .find('[name="selectTipoLiquidoedit"]').val(respuesta.idtipoliquido).end()
				                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Sabor por tipo de liquido',
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

function confirmarEditarSaborTipoLiquido()
{
	
           
            
                var idsabortipoliquido =  $('input:text[name=idsabortipoliquidoedit]').val();
                var descripcion = $('input:text[name=descripcionedit]').val();
                var idtipoliquido = $('#selectTipoLiquidoedit').val();
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDSaborTipoLiquido?idoperacion=2&idsabortipoliquido='+ idsabortipoliquido + "&descripcion=" + descripcion + "&idtipoliquido=" + idtipoliquido, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarSaborTipoLiquido();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('El sabor Tipo Liquido ha sido actualizada');
    				} 
			}); 
            //
            

        

}
var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-productos').DataTable( {
    		"aoColumns": [
            { "mData": "idproducto" },
            { "mData": "idreceta" , "visible": false },
            { "mData": "nombrereceta" },
            { "mData": "nombre" },
            { "mData": "descripcion" },
            { "mData": "impuesto" },
            { "mData": "tipo" },
            { "mData": "preciogeneral" },
            { "mData": "incluyeliquido" },
            { "mData": "idtipoliquido"  , "visible": false},
            { "mData": "nombreliquido" },
            { "mData": "habilitado" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarProducto()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarProducto()" value="Editar"></button>'
            }
        ]
    	} );
  	  	
	    
		
        pintarProductos();

        // Llenar select de productos y de liquidos
        llenarSelectRecetas();
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
                preciogeneraledit: {
                    validators: {
                        notEmpty: {
                            message: 'El precio general para la excepción es requerido'
                        },
                        regexp: {
                            regexp: /^[0-9]+$/,
                            message: 'El precio general de la Excepción debe ser numérico'
                        }
                    }
                },
                impuestoedit: {
                    validators: {
                        notEmpty: {
                            message: 'El impuesto para el producto es requerido'
                        },
                        regexp: {
                            regexp: /^[0-9]+$/,
                            message: 'El impuesto debe ser numérico'
                        }
                    }
                },
                descripcionedit: {
                    validators: {
                        notEmpty: {
                            message: 'La descripción de la excepción es requerida'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'La descripción de la excepción debe contener solo letras'
                        }
                    }
                },
                nombreedit: {
                    validators: {
                        notEmpty: {
                            message: 'El nombre del producto es requerido'
                        },
                        regexp: {
                            regexp: /^[a-zA-Z\s]+$/,
                            message: 'El nombre del producto debe contener solo letras'
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

function guardarProducto()
{
	var idreceta = $('#selectRecetas').val();
	var nombre = $('#nombre').val();
	var descripcion =  $('#descripcion').val();
	var impuesto = $('#impuesto').val();
	var tipo = $('#tipo').val();
	var preciogeneral = $('#preciogeneral').val();
	var incluye_liquido = $('#incluyeLiquido').val();
	var tipoliquido = $('#selectTipoLiquido').val();
	var idproducto;
	var habilitado = '';
	if($("#checkHabilitado").is(':checked')) {  
            habilitado = 'S'; 
        } else {  
            habilitado = 'N';
        } 
	$.getJSON(server + 'CRUDProducto?idoperacion=1&idreceta=' + idreceta + "&nombre=" + nombre + "&descripcion=" + descripcion + "&impuesto=" + impuesto + "&tipo=" +tipo + "&preciogeneral=" +preciogeneral+ "&incluye_liquido=" + incluye_liquido + "&idtipoliquido=" + tipoliquido+ "&habilitado=" + habilitado, function(data){
		var respuesta = data[0];
		idproducto = respuesta.idproducto;
				
	});
	pintarProductos();
}

function eliminarProducto(idproducto)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación del Producto',
			'content'	: 'Desea confirmar la eliminación del Producto ' + idproducto+ '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDProducto?idoperacion=3&idproducto=' + idproducto, 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//

								if ( $.fn.dataTable.isDataTable( '#grid-productos' ) ) {
							    	table = $('#productos').DataTable();
							    }
								
								pintarProductos();

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

function llenarSelectRecetas()
{
	//Pendiente de implementar método
	/*
	$.ajax({ 
	    				url: server + 'GetTodosProductos' , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data1)
	    					{ 
								var str = '';
								for(var i = 0; i < data1.length;i++){
									str +='<option value="'+ data1[i].idproducto +'" id ="'+ data1[i].idproducto +'">' + data1[i].nombre + '-' + data1[i].descripcion + '</option>';
								}
								$('#selectProductos').html(str);
								$('#selectProductosedit').html(str);
													
							} 
						});
	*/
	
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

function pintarProductos()
{
	$.getJSON(server + 'CRUDProducto?idoperacion=6'  , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaProducto = data1[i];
				table.row.add({
					"idproducto": data1[i].idproducto, 
					"idreceta": data1[i].idreceta,
					"nombrereceta": data1[i].nombrereceta, 
					"nombre": data1[i].nombre, 
					"descripcion": data1[i].descripcion,
					"impuesto": data1[i].impuesto, 
					"tipo": data1[i].tipo,
					"preciogeneral": data1[i].preciogeneral,
					"incluyeliquido": data1[i].incluye_liquido,
					"idtipoliquido": data1[i].idtipo_liquido,  
					"nombreliquido": data1[i].nombreliquido,
					"habilitado": data1[i].habilitado, 
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarProducto(' +data1[i].idproducto + ')" value="Eliminar"></button> <input type="button" onclick="editarProducto('+data1[i].idproducto+')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idproducto + '" value="Edición"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarProducto(idproducto)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDProducto?idoperacion=4&idproducto=' + idproducto, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						
						$('#userForm')
				                .find('[name="idproductoedit"]').val(respuesta.idproducto).end()
				                .find('[name="selectRecetasedit"]').val(respuesta.idreceta).end()
				                .find('[name="nombreedit"]').val(respuesta.nombre).end()
				                .find('[name="descripcionedit"]').val(respuesta.descripcion).end()
				                .find('[name="impuestoedit"]').val(respuesta.impuesto).end()
				                .find('[name="tipoedit"]').val(respuesta.tipo).end()
				                .find('[name="preciogeneraledit"]').val(respuesta.preciogeneral).end()
				                .find('[name="incluyeLiquidoedit"]').val(respuesta.incluye_liquido).end()
				                .find('[name="selectTipoLiquidoedit"]').val(respuesta.idtipoliquido).end()
				                			                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Producto',
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
				                if(respuesta.habilitado == 'S')
				                {
				                	$('#checkHabilitadoedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkHabilitadoedit').prop('checked',false);
				                }
					} 
		});

		//
}

function confirmarEditarProducto()
{
	
           
            
                var idproducto =  $('input:text[name=idproductoedit]').val();
                var idreceta = $('#selectRecetasedit').val();
                var nombre = $('input:text[name=nombreedit]').val();
                var descripcion = $('input:text[name=descripcionedit]').val();
                var impuesto = $('input:text[name=impuestoedit]').val();
                var tipo = $('#tipoedit').val();
                var preciogeneral = $('input:text[name=preciogeneraledit]').val();
                var incluye_liquido = $('#incluyeLiquidoedit').val();
                var idtipoliquido = $('#selectTipoLiquidoedit').val();
                var habilitado = '';
				if($("#checkHabilitadoedit").is(':checked')) {  
			            habilitado = 'S'; 
			        } else {  
			            habilitado = 'N';
			        } 
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDProducto?idoperacion=2&idproducto='+ idproducto+'&idreceta=' + idreceta + "&nombre=" + nombre + "&descripcion=" + descripcion +"&impuesto=" + impuesto + "&tipo=" + tipo + "&preciogeneral=" + preciogeneral + "&incluye_liquido=" + incluye_liquido + "&idtipoliquido=" + idtipoliquido+ "&habilitado=" + habilitado, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarProductos();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('Wl producto ha sido actualizada');
    				} 
			}); 
            //
            

        

}
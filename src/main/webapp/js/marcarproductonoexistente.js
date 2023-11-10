var server;
var table;
var productos;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-productosnoexistentes').DataTable( {
    		"aoColumns": [
            { "mData": "idtienda" },
            { "mData": "tienda" },
            { "mData": "idproducto" },
            { "mData": "producto" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarProductoNoExistente()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarProductoNoExistente()" value="Editar"></button>'
            }
        ]
    	} );
  	  	
	    
		
        pintarProductosNoExistentes();

        // Llenar select de productos y de liquidos
        getListaTiendas();
        getTodosProductos();
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

function guardarProductoNoExistente()
{
	
	var idtienda = $('#selectTienda').val(); 
	var idproducto = $('#selectProducto').val(); 
	$.getJSON(server + 'CRUDProductoNoExistente?idoperacion=1&idproducto=' + idproducto + "&idtienda=" + idtienda , function(data){
		$('#addData').modal('hide');
		$('#selectTienda').val('');
		$('#selectProducto').val('');
		pintarProductosNoExistentes();
				
	});

}

function eliminarProductoNoExistente(idtienda,idproducto)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación del Producto No Disponible',
			'content'	: 'Desea confirmar la eliminación del Producto No Disponible.' ,
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDProductoNoExistente?idoperacion=3&idproducto=' + idproducto + '&idtienda=' + idtienda, 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								pintarProductosNoExistentes();

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

function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.id +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTienda').html(str);
		$('#selectTiendaedit').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectTienda").val('');
		$("#selectTiendaedit").val('');
	});
}


function getTodosProductos()
{
	$.getJSON(server + 'GetTodosProductos', function(data){
		productos = data;
		var str='';
		for(var i = 0; i < data.length;i++){
			var cadaProducto  = data[i];
			str +='<option value="'+ cadaProducto.idproducto +'" id ="'+ cadaProducto.idproducto +'">' + cadaProducto.nombre +'</option>';
		}
		$('#selectProducto').html(str);
		$('#selectProductoedit').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectProducto").val('');
		$("#selectProductoedit").val('');
	});
}



function pintarProductosNoExistentes()
{
	$.getJSON(server + 'CRUDProductoNoExistente?idoperacion=6'  , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaProducto = data1[i];
				table.row.add({
					"idtienda": data1[i].idtienda, 
					"tienda": data1[i].tienda,
					"idproducto": data1[i].idproducto, 
					"producto": data1[i].producto, 
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarProductoNoExistente(' +data1[i].idtienda + ' , ' + data1[i].idproducto + ')" value="Eliminar"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarProductoNoExistente(idtienda, idproducto)
{
	$("#selectTiendaedit").val(idtienda);
	$("#selectProductoedit").val(idproducto);
}

function editarProductoNoExistente(idtienda, idproducto)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDProductoNoExistente?idoperacion=4&idtienda=' + idtienda  + 'idproducto=' + idproducto , 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						$('#userForm')
				                .find('[name="selectTiendaedit"]').val(respuesta.idtienda).end()
				                .find('[name="selectProductoedit"]').val(respuesta.idproducto).end()
				                

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Producto No Existente',
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

function confirmarEditarProductoNoExistente()
{
	
           
            
                var idtienda = $('#selectTiendaedit').val();
                var idproducto = $('#selectProductoedit').val();
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDProductoNoExistente?idoperacion=2&idproducto='+ idproducto+'&idtienda=' + idtienda , 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarProductosNoExistentes();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('El producto No Disponible ha sido actualizada');
    				} 
			}); 
            //
            

        

}
var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	

		//Lo primero que realizaremos es validar si está logueado
	
		
	    table = $('#grid-tiendasBloqueadas').DataTable( {
    		"aoColumns": [
            { "mData": "idtienda" },
            { "mData": "nombre" },
            { "mData": "comentario" },
            { "mData": "fecha" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarTiendaBloqueada()" value="Eliminar">'
            }
        ]
    	} );
  	  	
	     
    	//
    	getListaTiendas()
		pintarTiendasBloqueadas();
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

function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#tienda').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#tienda").val('');
	});
}

function guardarTiendaBloqueada()
{
	var idtien = $("#tienda option:selected").attr('id');
	var observacion = $('#observacion').val();
	var motivo = encodeURIComponent($("#selectMotivo").val());
	var desbloqueoEn = encodeURIComponent($("#selectDesbloqueoEn").val());
	
	
	$.ajax({ 
	    		url: server + 'CRUDTiendaBloqueada?idoperacion=1&idtienda=' + idtien + "&observacion=" + observacion + "&motivo=" + motivo + "&desbloqueoen=" + desbloqueoEn, 
	    		dataType: 'json', 
	    		async: false, 
	    		success: function(data){ 
					
					var urlTienda = "";
					$.ajax({ 
								url: server + 'ObtenerUrlTienda?idtienda=' + idtien , 
								dataType: 'json',
								type: 'get', 
								async: false, 
								success: function(data3)
								{
									var urlTienda = data3[0].urltienda;
									$.getJSON(urlTienda + 'BloquearTienda', function(data){
										alert('Se bloqueo la tienda en el contact Center y en el punto de venta');
									});
									
								} 
							});					

				} 
			});
	$('#tienda').val('');
	$('#comentario').val('');
	$('#addData').modal('hide');
	pintarTiendasBloqueadas()

}



function eliminarTiendaBloqueada(idtienda)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación Tienda Bloqueada',
			'content'	: 'Desea confirmar la eliminación de la Tienda  Bloqueada ' + idtienda + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDTiendaBloqueada?idoperacion=3&idtienda=' + idtienda , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//
								var urlTienda = "";
								$.ajax({ 
									url: server + 'ObtenerUrlTienda?idtienda=' + idtienda , 
									dataType: 'json',
									type: 'get', 
									async: false, 
									success: function(data3)
									{
										var urlTienda = data3[0].urltienda;
										$.getJSON(urlTienda + 'DesbloquearTienda', function(data){
											alert('Se desbloqueo la tienda en el contact Center y en el punto de venta');
										});
										
									} 
								});			

								if ( $.fn.dataTable.isDataTable( '#grid-tiendasBloqueadas' ) ) {
							    	table = $('#grid-tiendasBloqueadas').DataTable();
							    }
								
								pintarTiendasBloqueadas();

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

function pintarTiendasBloqueadas()
{
	$.getJSON(server + 'CRUDTiendaBloqueada?idoperacion=5' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaTienda  = data1[i];
				table.row.add({
					"idtienda": data1[i].idtienda, 
					"nombre": data1[i].nombre,
					"comentario": data1[i].comentario,
					"fecha": data1[i].fecha,  
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarTiendaBloqueada(' +data1[i].idtienda+ ')" value="Eliminar">'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}



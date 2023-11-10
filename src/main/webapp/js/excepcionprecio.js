var server;
var table;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
		//Lo primero que realizaremos es validar si está logueado
		
		
	    table = $('#grid-excepcionPrecio').DataTable( {
    		"aoColumns": [
            { "mData": "idexcepcion" },
            { "mData": "idproducto" , "visible": false },
            { "mData": "nombreproducto" },
            { "mData": "precio" },
            { "mData": "descripcion" },
            { "mData": "incluyeliquido" },
            { "mData": "idtipoliquido"  , "visible": false},
            { "mData": "nombreliquido" },
            { "mData": "habilitado" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarExcepcion()" value="Eliminar"></button> <input type="button" class="btn btn-default btn-xs" onclick="EditarExcepcion()" value="Editar"></button>'
            }
        ]
    	} );
  	  	
	    
		
        pintarExcepciones();
        //Llenamos los selects de las horas
        getLlenarHoras();
        // Llenar select de productos y de liquidos
        llenarSelectProductos();
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
                precioedit: {
                    validators: {
                        notEmpty: {
                            message: 'El precio para la excepción es requerido'
                        },
                        regexp: {
                            regexp: /^[0-9]+$/,
                            message: 'El precio de la Excepción debe ser numérico'
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

function guardarExcepcionPrecio()
{
	var idproducto = $('#selectProductos').val();
	var precio = $('#precio').val();
	var descripcion =  $('#descripcion').val();
	var incluye_liquido = $('#incluyeLiquido').val();
	var tipoliquido = $('#selectTipoLiquido').val(); 
	var idexcepcion;
	var habilitado = '';
	var lunes = '';
	var martes = '';
	var miercoles = '';
	var jueves = '';
	var viernes = '';
	var sabado = '';
	var domingo = '';
	if($("#checkHabilitado").is(':checked')) {  
            habilitado = 'S'; 
        } else {  
            habilitado = 'N';
        } 
    if($("#checkLunes").is(':checked')) 
    {  
            lunes = 'S'; 
    } else
    {  
            lunes = 'N';
    } 
    if($("#checkMartes").is(':checked')) 
    {  
            martes = 'S'; 
    } else
    {  
            martes = 'N';
    } 
    if($("#checkMiercoles").is(':checked')) 
    {  
            miercoles = 'S'; 
    } else
    {  
            miercoles = 'N';
    } 
    if($("#checkJueves").is(':checked')) 
    {  
            jueves = 'S'; 
    } else
    {  
            jueves = 'N';
    } 
    if($("#checkViernes").is(':checked')) 
    {  
            viernes = 'S'; 
    } else
    {  
            viernes = 'N';
    } 
    if($("#checkSabado").is(':checked')) 
    {  
            sabado = 'S'; 
    } else
    {  
            sabado = 'N';
    } 
    if($("#checkDomingo").is(':checked')) 
    {  
            domingo = 'S'; 
    } else
    {  
            domingo = 'N';
    } 
    //Tratamos los valores de hora Inicial y final de la excepcion
    var horaInicial = $('#selectHoraInicial').val();
    var horaFinal = $('#selectHoraFinal').val();
    if(horaInicial == null || horaInicial == '' || horaInicial == '0')
    {
    	horaInicial = '';
    }
    if(horaFinal == null || horaFinal == '' || horaFinal == '0')
    {
    	horaFinal = '';
    }
	$.getJSON(server + 'CRUDExcepcionPrecio?idoperacion=1&idproducto=' + idproducto + "&precio=" + precio + "&descripcion=" + descripcion + "&incluye_liquido=" + incluye_liquido + "&idtipoliquido=" + tipoliquido + "&habilitado=" + habilitado + "&horainicial=" + horaInicial + "&horafinal=" + horaFinal + "&lunes=" + lunes + "&martes=" + martes + "&miercoles=" + miercoles + "&jueves=" + jueves + "&viernes=" + viernes + "&sabado=" + sabado + "&domingo=" + domingo, function(data){
		var respuesta = data[0];
		idexcepcion = respuesta.idexcepcion;
		pintarExcepciones();
		$('#selectProductos').val('');
		$('#precio').val('');
		$('#descripcion').val('');
		$('#incluyeLiquido').val('S');
		$('#selectTipoLiquido').val('');
		$('#selectHoraInicial').val('');
		$('#selectHoraFinal').val('');
		$('#addData').modal('hide');		
	});
	
}

function eliminarExcepcion(idexcepcion)
{
	/*var table = $('#grid-especialidades').DataTable();
	var datos = table.row( this ).data();
	console.log(datos);
	idespecialidad = datos.idespecialidad;
	nombre = datos.nombre;*/
	$.confirm({
			'title'		: 'Confirmacion Eliminación de la Excepción de Precio',
			'content'	: 'Desea confirmar la eliminación de la Excepción ' + idexcepcion+ '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
					
						var resultado
						$.ajax({ 
	    				url: server + 'CRUDExcepcionPrecio?idoperacion=3&idexcepcion=' + idexcepcion , 
	    				dataType: 'json', 
	    				async: false, 
	    				success: function(data){ 
								resultado = data[0];
								//
								if(resultado.resultado == 'exitoso')
								{
									$.alert('Se realizó la eliminación de la Excepción de precio.');
									if ( $.fn.dataTable.isDataTable( '#grid-excepcionPrecio' ) ) 
									{
							    		table = $('#grid-excepcionPrecio').DataTable();
							    	}
									pintarExcepciones();
								}else if(resultado.resultado == 'error')
								{
									$.alert('No se pudo realizar la eliminación de la Excepción de Precio.');
								}
								

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

function llenarSelectProductos()
{
	
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

function pintarExcepciones()
{
	$.getJSON(server + 'CRUDExcepcionPrecio?idoperacion=6'  , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaExcepcion = data1[i];
				table.row.add({
					"idexcepcion": data1[i].idexcepcion, 
					"idproducto": data1[i].idproducto,
					"nombreproducto": data1[i].nombreproducto, 
					"precio": data1[i].precio, 
					"descripcion": data1[i].descripcion, 
					"incluyeliquido": data1[i].incluye_liquido,
					"idtipoliquido": data1[i].idtipoliquido,  
					"nombreliquido": data1[i].nombreliquido,
					"habilitado": data1[i].habilitado,  
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarExcepcion(' +data1[i].idexcepcion + ')" value="Eliminar"></button> <input type="button" onclick="editarExcepcion('+data1[i].idexcepcion+')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idexcepcion + '" value="Edición"></button>'
				}).draw();
				//table.row.add(data1[i]).draw();
			}
		});
}

function editarExcepcion(idexcepcion)
{
	//
				
	// Get the record's ID via attribute
	
		$.ajax({ 
    				url: server + 'CRUDExcepcionPrecio?idoperacion=4&idexcepcion=' + idexcepcion, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){ 
						var respuesta = data[0];
						console.log(data);
						console.log(respuesta.horainicial + respuesta.horafinal);
				            $('#userForm')
				                .find('[name="idexcepcionedit"]').val(respuesta.idexcepcion).end()
				                .find('[name="selectProductosedit"]').val(respuesta.idproducto).end()
				                .find('[name="precioedit"]').val(respuesta.precio).end()
				                .find('[name="descripcionedit"]').val(respuesta.descripcion).end()
				                .find('[name="incluyeLiquidoedit"]').val(respuesta.incluye_liquido).end()
				                .find('[name="selectTipoLiquidoedit"]').val(respuesta.idtipoliquido).end()
				                .find('[name="selectHoraInicialedit"]').val(respuesta.horainicial).end()
				                .find('[name="selectHoraFinaledit"]').val(respuesta.horafinal).end()

				            // Show the dialog
				            bootbox
				                .dialog({
				                    title: 'Editar Excepción',
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
				                if(respuesta.lunes == 'S')
				                {
				                	$('#checkLunesedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkLunesedit').prop('checked',false);
				                }
				                if(respuesta.martes == 'S')
				                {
				                	$('#checkMartesedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkMartesedit').prop('checked',false);
				                }
				                if(respuesta.miercoles == 'S')
				                {
				                	$('#checkMiercolesedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkMiercolesedit').prop('checked',false);
				                }
				                if(respuesta.jueves == 'S')
				                {
				                	$('#checkJuevesedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkJuevesedit').prop('checked',false);
				                }
				                if(respuesta.viernes == 'S')
				                {
				                	$('#checkViernesedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkViernesedit').prop('checked',false);
				                }
				                if(respuesta.sabado == 'S')
				                {
				                	$('#checkSabadoedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkSabadoedit').prop('checked',false);
				                }
				                if(respuesta.domingo == 'S')
				                {
				                	$('#checkDomingoedit').prop('checked',true);
				                }
				                else
				                {
				                	$('#checkDomingoedit').prop('checked',false);
				                }
				                $("#selectProductosedit").val(respuesta.idproducto);
				                $("#selectTipoLiquidoedit").val(respuesta.idtipoliquido);
				                $("#incluyeLiquidoedit").val(respuesta.incluye_liquido);
				                $("#selectHoraInicialedit").val(respuesta.horainicial);
				                $("#selectHoraFinaledit").val(respuesta.horafinal);
					} 
		});

		//
}

function confirmarEditarExcepcion()
{
	
           
            
                var idexcepcion =  $('input:text[name=idexcepcionedit]').val();
                var precio = $('input:text[name=precioedit]').val();
                var descripcion = $('input:text[name=descripcionedit]').val();
                var idproducto =  $('#selectProductosedit').val();
                var incluye_liquido = $('#incluyeLiquidoedit').val();
                var idtipoliquido = $('#selectTipoLiquidoedit').val();
                var habilitado = '';
                var lunes = '';
                var martes = '';
                var miercoles = '';
                var jueves = '';
                var viernes = '';
                var sabado = '';
                var domingo = '';
                if($("#checkHabilitadoedit").is(':checked')) 
                {  
			            habilitado = 'S'; 
			    } else {  
			            habilitado = 'N';
			    }
			    if($("#checkLunesedit").is(':checked')) 
                {  
			            lunes = 'S'; 
			    } else {  
			            lunes = 'N';
			    }
			    if($("#checkMartesedit").is(':checked')) 
                {  
			            martes = 'S'; 
			    } else {  
			            martes = 'N';
			    }
			    if($("#checkMiercolesedit").is(':checked')) 
                {  
			            miercoles = 'S'; 
			    } else {  
			            miercoles = 'N';
			    }
			    if($("#checkJuevesedit").is(':checked')) 
                {  
			            jueves = 'S'; 
			    } else {  
			            jueves = 'N';
			    }
			    if($("#checkViernesedit").is(':checked')) 
                {  
			            viernes = 'S'; 
			    } else {  
			            viernes = 'N';
			    }
			    if($("#checkSabadoedit").is(':checked')) 
                {  
			            sabado = 'S'; 
			    } else {  
			            sabado = 'N';
			    }
			    if($("#checkDomingoedit").is(':checked')) 
                {  
			            domingo = 'S'; 
			    } else {  
			            domingo = 'N';
			    } 
			    var horaInicial = $('#selectHoraInicialedit').val();
			    var horaFinal = $('#selectHoraFinaledit').val();
			    if(horaInicial == null || horaInicial == '' || horaInicial == '0')
			    {
			    	horaInicial = '';
			    }
			    if(horaFinal == null || horaFinal == '' || horaFinal == '0')
			    {
			    	horaFinal = '';
			    }
            // The url and method might be different in your application
            $.ajax({ 
    				url: server + 'CRUDExcepcionPrecio?idoperacion=2&idexcepcion='+ idexcepcion+'&precio=' + precio + "&descripcion=" + descripcion + "&idproducto=" + idproducto + "&incluye_liquido=" + incluye_liquido + "&idtipoliquido=" + idtipoliquido + "&habilitado=" + habilitado + "&horainicial=" + horaInicial + "&horafinal=" + horaFinal + "&lunes=" + lunes + "&martes=" + martes + "&miercoles=" + miercoles + "&jueves=" + jueves + "&viernes=" + viernes + "&sabado=" + sabado + "&domingo=" + domingo, 
    				dataType: 'json', 
    				async: false, 
    				success: function(data){
    					pintarExcepciones();
               			 // Hide the dialog
                		$('#userForm').parents('.bootbox').modal('hide');

                		// You can inform the user that the data is updated successfully
                		// by highlighting the row or showing a message box
                		bootbox.alert('La excepción ha sido actualizada');
    				} 
			}); 
            //
            

        

}

function getLlenarHoras(){
	var str = '';
	str +='<option value="12:00" id ="12:00">12:00</option>';
	str +='<option value="12:30" id ="12:30">12:30</option>';
	str +='<option value="13:00" id ="13:00">13:00</option>';
	str +='<option value="13:30" id ="13:30">13:30</option>';
	str +='<option value="14:00" id ="14:00">14:00</option>';
	str +='<option value="14:30" id ="14:30">14:30</option>';
	str +='<option value="15:00" id ="15:00">15:00</option>';
	str +='<option value="15:30" id ="15:30">15:30</option>';
	str +='<option value="16:00" id ="16:00">16:00</option>';
	str +='<option value="16:30" id ="16:30">16:40</option>';
	str +='<option value="17:00" id ="17:00">17:00</option>';
	str +='<option value="17:30" id ="17:30">17:30</option>';
	str +='<option value="18:00" id ="18:00">18:00</option>';
	str +='<option value="18:30" id ="18:30">18:30</option>';
	str +='<option value="19:00" id ="19:00">19:00</option>';
	str +='<option value="19:30" id ="19:30">19:30</option>';
	str +='<option value="20:00" id ="20:00">20:00</option>';
	str +='<option value="20:30" id ="20:30">20:30</option>';
	str +='<option value="21:00" id ="21:00">21:00</option>';
	str +='<option value="21:30" id ="21:30">21:30</option>';
	str +='<option value="22:00" id ="22:00">22:00</option>';
	str +='<option value="22:30" id ="22:30">22:30</option>';
	str +='<option value="23:00" id ="23:00">23:00</option>';
	str +='<option value="0" id ="0"></option>';
	$('#selectHoraInicial').html(str);
	$('#selectHoraInicialedit').html(str);
	$('#selectHoraFinal').html(str);
	$('#selectHoraFinaledit').html(str);
	$("#selectHoraInicial").val('');
	$("#selectHoraInicialedit").val('');
	$("#selectHoraFinal").val('');
	$("#selectHoraFinaledit").val('');
}
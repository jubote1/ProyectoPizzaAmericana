	

// Se definen las variables globales.
var server;
var tiendas;
var table;
var dtpedido;
var dtEncabezadoPedido;
var dtDetallePedido;
var productos;
var excepciones;
var memcode = 0;
var insertado = 0;
var idCliente = 0;
var idEstadoPedido = 0;
var longitud = 0;
var latitud = 0;
var marcadorAdiciones = 0;
var marcadorAdicionales = 0;
var marcadorGaseosas = 0;
var marcardorProductoCon = 0;
var marcardorProductoSin = 0;
var canvas;
var ctx;
var totalpedido = 0;
var radioEsp1Ant= 0;
var radioEsp1 = 0;
var radioEsp2Ant= 0;
var radioEsp2 = 0;
var contadorItem = 1;
var controlaCantidadIngredientes = "N";
var cantidadIngredientes = 0;
var excepcionSeleccionada = null;
var gaseosaHomologadaTienda;
var formas;
var tableOfertaCliente;


// A continuación  la ejecucion luego de cargada la pagina
$(document).ready(function() 
{
   	getListaTiendas();
	getListaMunicipios();
	getListaNomenclaturas();
    llenarSelectOferta();
	setInterval('validarVigenciaLogueo()',600000);
	$('#descDireccion').attr('disabled', true);
	// Llevamos a cero los campos cálculos de los totales
	$('#ultimospedidos').attr('disabled', true);
	// Se define evento para campo valor a devolver.
	// Se realiza la creación del DATATABLE DE CLIENTES
    table = $('#grid-clientes').DataTable( {
    		"aoColumns": [
            { "mData": "idCliente" },
            { "mData": "tienda" },
            { "mData": "nombre" },
            { "mData": "apellido" },
            { "mData": "nombrecompania" },
            { "mData": "direccion" },
            { "mData": "zona" },
            { "mData": "observacion" },
            { "mData": "telefono" },
            { "mData": "telefonocelular" , "visible": false },
            { "mData": "email" , "visible": false },
            { "mData": "politicadatos" , "visible": false },
            { "mData": "estado" },
            { "mData": "municipio" , "visible": false },
            { "mData": "latitud" , "visible": false },
            { "mData": "longitud" , "visible": false },
            { "mData": "memcode"  , "visible": false },
            { "mData": "idnomenclatura"  , "visible": false },
            { "mData": "numnomenclatura1"  , "visible": false },
            { "mData": "numnomenclatura2"  , "visible": false },
            { "mData": "num3"  , "visible": false },
            { "mData": "nomenclatura"  , "visible": false },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="inactivarCliente()" value="Inactivar"></button>'
            }
        ]
    	} );

    tableOfertaCliente = $('#grid-ofertascliente').DataTable( {
            "aoColumns": [
            { "mData": "idofertacliente" },
            { "mData": "idcliente", "visible": false },
            { "mData": "idoferta", "visible": false },
            { "mData": "nombreoferta" },
            { "mData": "utilizada" },
            { "mData": "pqrs" },
            { "mData": "ingresooferta" },
            { "mData": "usooferta" },
            { "mData": "observacion" },
            {
                "mData": "accion",
                className: "center",
                defaultContent: '<input type="button" class="btn btn-default btn-xs" onclick="eliminarOfertaCliente()" value="Eliminar"></button>'
            }
        ]
        } );

    //Inicializamos los DataTables para historia de pedidos cliente
    dtDetallePedido = $('#grid-detallepedido').DataTable( {
            "aoColumns": [
            { "mData": "iddetallepedido" },
            { "mData": "nombreproducto" },
            { "mData": "cantidad" },
            { "mData": "especialidad1" },
            { "mData": "especialidad2" },
            { "mData": "valorunitario" , "visible": false},
            { "mData": "valortotal" },
            { "mData": "adicion" },
            { "mData": "observacion" },
            { "mData": "liquido" },
            { "mData": "excepcion" }
            
            
        ]
        } );

    dtEncabezadoPedido = $('#grid-encabezadopedido').DataTable( {
            "aoColumns": [
            { "mData": "idpedido" },
            { "mData": "tienda" },
            { "mData": "fechainsercion" },
            { "mData": "cliente" },
            { "mData": "direccion" },
            { "mData": "telefono" },
            { "mData": "totalneto" , "visible": false},
            { "mData": "estadopedido" , "visible": false },
            { "mData": "usuariopedido" , "visible": false },
            { "mData": "enviadopixel"  , "visible": false },
            { "mData": "estadoenviotienda" , "visible": false },
            { "mData": "numposheader" , "visible": false  },
            { "mData": "formapago" , "visible": false  },
            { "mData": "tiempopedido" , "visible": false },
            { "mData": "idtienda", "visible": false },
            { "mData": "urltienda", "visible": false },
            { "mData": "stringpixel", "visible": false }
        ]
        } );

     



    $('#grid-clientes tbody').on('click', 'tr', function() 
    {
        datos = table.row( this ).data();
        $("#selectTiendas").val(datos.tienda);
        var idtien = $("#selectTiendas option:selected").attr('id');
        //alert( 'Diste clic en  '+datos.nombre+'\'s row' );
        if(idtien != undefined)
        {
            $('#selectTiendas').attr('disabled', true);
        }
        $('#nombres').val(datos.nombre);
        $('#apellidos').val(datos.apellido);
        $('#nombreCompania').val(datos.nombrecompania);
        $('#direccion').val(datos.direccion);
        $('#zona').val(datos.zona);
        $('#observacionDir').val(datos.observacion);
        // Para evitar que modifiquen la tienda
        $("#selectNomenclaturas").val(datos.nomenclatura);
        $("#selectMunicipio").val(datos.municipio);
        $('#numNomen').val(datos.numnomenclatura1);
        $('#numNomen2').val(datos.numnomenclatura2);
        $('#num3').val(datos.num3);
        $('#telcelular').val(datos.telefonocelular);
        $('#email').val(datos.email);
        if(datos.politicadatos == 'S')
        {
            $('#politicadatos').prop('checked', true);
        }else
        {
            $('#politicadatos').prop('checked', false);
        }
        var selMunicipio = $("#selectMunicipio").val();
        if (selMunicipio == '' || selMunicipio == null || selMunicipio == 'null')
        {
        	selMunicipio = '';
        }
        $('#descDireccion').val($("#selectNomenclaturas").val() +  " "  + $("#numNomen").val() + " # " + $("#numNomen2").val() + " - " + $("#num3").val() + " " + selMunicipio);
        memcode = datos.memcode;
        idCliente = datos.idCliente;
        //Se invoca servicio para traer las promociones del cliente seleccionado
        pintarOfertasCliente(idCliente);
        $('#agregarOferta').attr('disabled', false);
        var municipio = datos.municipio;
        //Traemos los valores de la latitud y longitud 
        latitud = datos.latitud;
        longitud = datos.longitud;
        //En este punto luego de buscar la direccion del cliente, buscaremos si este tiene pedidos
        $('#ultimospedidos').attr('disabled', false);

    });
    $('#agregarOferta').attr('disabled', true);
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



function descripcionDireccion()
{
	var selMunicipio = $("#selectMunicipio").val();
    if (selMunicipio == '' || selMunicipio == null || selMunicipio == 'null')
    {
        selMunicipio = '';
    }
	$('#descDireccion').val($("#selectNomenclaturas").val() +  " "  + $("#numNomen").val() + " # " + $("#numNomen2").val() + " - " + $("#num3").val() + " " + selMunicipio);	
}

// Método que se encarga luego de introducido un teléfono en el campo de teléfono del cliente llamar al servicio
function validarTelefono(){

	//Incluimos validación del logueo dado que es un punto crítico para retomar los pedidos
	validarVigenciaLogueo();
	// Validamos el tema de la longitud del pedido
	var lngTel = $("#telefono").val().length;
	if (lngTel < 10)
	{
		alert("Tenga precaución el teléfono tiene una lontitud menor a 10");
	}else if(lngTel == 10)
    {
        if($("#telefono").val().substr(0,1) == '3')
        {
            $("#telcelular").val($("#telefono").val());
        }
    }

	if (!/^([0-9])*$/.test($("#telefono").val()))
	{
      alert("El valor " + $("#telefono").val() + " no es un número");
	}
	// Validamos que sean solo números
	if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) {
    	table = $('#grid-clientes').DataTable();
    }
	
	$.getJSON(server + 'GetClienteTodos?telefono=' + telefono.value, function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaCliente  = data1[i];
				//table.row.add(data1[i]).draw();
                var estado;
                if(data1[i].estado == 0)
                {
                    estado = "INACTIVO";
                }else{
                    estado = "ACTIVO";
                }
                table.row.add({
                    "idCliente": data1[i].idCliente, 
                    "tienda": data1[i].tienda,
                    "nombre": data1[i].nombre,
                    "apellido": data1[i].apellido, 
                    "nombrecompania": data1[i].nombrecompania,
                    "direccion": data1[i].direccion, 
                    "zona": data1[i].zona,
                    "observacion": data1[i].observacion,
                    "telefono": data1[i].telefono,
                    "telefonocelular": data1[i].telefonocelular,
                    "email": data1[i].email,
                    "politicadatos": data1[i].politicadatos,
                    "estado": estado,
                    "municipio": data1[i].municipio,    
                    "latitud": data1[i].latitud, 
                    "longitud": data1[i].longitud,
                    "memcode": data1[i].memcode, 
                    "idnomenclatura": data1[i].idnomenclatura,  
                    "numnomenclatura1": data1[i].numnomenclatura1,  
                    "numnomenclatura2": data1[i].numnomenclatura2,  
                    "num3": data1[i].num3,  
                    "nomenclatura": data1[i].nomenclatura,   
                    "accion":'<input type="button" class="btn btn-default btn-xs" onclick="inactivarCliente(' +data1[i].idCliente + ')" value="Inactivar"></button> <input type="button" onclick="activarCliente('+data1[i].idCliente+')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idCliente + '" value="Activar"></button>'
                }).draw();


			}
		});
		
}





// Método que invoca el servicio para listar las tiendas donde se pondrán tomar domicilios.
function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTiendas').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectTiendas").val('');
	});
}

function getListaNomenclaturas(){
	$.getJSON(server + 'GetNomenclaturasDireccion', function(data){
		var nomenclaturas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaNomen = data[i];
			str +='<option value="'+ cadaNomen.nomenclatura +'" id ="'+ cadaNomen.idnomenclatura +'">' + cadaNomen.nomenclatura +'</option>';
		}
		$('#selectNomenclaturas').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectNomenclaturas").val('');
	});
}

//Método que invoca el servicio para obtener lista de municipios parametrizados en el sistema
function getListaMunicipios(){

	$.getJSON(server + 'CRUDMunicipio?idoperacion=5', function(data){
		
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaMunicipio  = data[i];
			str +='<option value="'+ cadaMunicipio.nombre +'" id ="'+ cadaMunicipio.idmunicipio +'">' + cadaMunicipio.nombre +'</option>';
		}
		$('#selectMunicipio').html(str);
		$("#selectMunicipio").val('');
	});

}



//Métodos para obtener todos los productos y con esto se realizan todas las validaciones de producto dentro de la capa de presentación.


function actualizarCliente()
{
    var valida;
    if(idCliente > 0)
    {
        valida = ValidacionesDatos();
    }else
    {
        if($('#creacionligera').is(':checked'))
        {
            valida = ValidacionesLigerasDatos();
        }else
        {
            valida = ValidacionesDatos();
        }
        
    }
    
    if (!valida)
    {
        return;
    }
	var tempTienda =  $("#selectTiendas option:selected").val();
    //Variable en la que alojamos el id de la tienda
    var telef = telefono.value;
    var tienda = $("#selectTiendas option:selected").attr('id');
    var idMunicipio = $("#selectMunicipio option:selected").attr('id');
    var tempMunicipio = $("#selectMunicipio option:selected").val();
    var idClienteTemporal;
    var nombresEncode = encodeURIComponent(nombres.value);
    var apellidosEncode = encodeURIComponent(apellidos.value);
    var nombreCompaniaEncode = encodeURIComponent(nombreCompania.value);;
    var direccionEncode;
    var validaDir = 'S';
    var dirTemp = $("#selectNomenclaturas").val() +  " "  + $("#numNomen").val() + " # " + $("#numNomen2").val() + " - " + $("#num3").val();
    if($('#validaDir').is(':checked'))
    {
        direccionEncode = encodeURIComponent($("#selectNomenclaturas").val() +  " "  + $("#numNomen").val() + " # " + $("#numNomen2").val() + " - " + $("#num3").val());
    }else
    {
        validaDir = 'N';
        direccionEncode = encodeURIComponent(direccion.value);
    }
    var zonaEncode = encodeURIComponent(zona.value); 
    var observacionDirEncode = encodeURIComponent(observacionDir.value);
    var nomenclatura = $("#selectNomenclaturas option:selected").attr('id');
    var numNomenclatura1 = encodeURIComponent($("#numNomen").val());
    var numNomenclatura2 = encodeURIComponent($("#numNomen2").val());
    var num3 = encodeURIComponent($("#num3").val());
    var telCelular = $("#telcelular").val();
    var email = $("#email").val();
    var politicaDatos = "N";
    if($('#politicadatos').is(':checked'))
    {
        politicaDatos = "S";
    }
    $.confirm({
                'title'     : 'Confirmacion de Creación/Actualización de Cliente',
                'content'   : 'Desea confirmar la creación/actualización del cliente ' + nombres.value + " " + apellidos.value +
                '. Con la siguiente información: <br>' +
                'DIRECCION ' +  dirTemp  + '<br>' +
                'MUNICIPIO ' + tempMunicipio + '<br>' +
                'NOMBRE COMPAÑIA ' + nombreCompania.value + '<br>' +
                'TIENDA' +  '<h1>' + tempTienda + '</h1> <br>',
                'type': 'dark',
                'typeAnimated': true,
                'buttons'   : {
                    'Si'    : {
                        'class' : 'blue',
                        'action': function()
                        {
                            //Ejecutamos la actualización
                            $.ajax({ 
                                url: server + 'ActualizarCliente?telefono=' + telefono.value + "&nombres=" + nombresEncode + "&apellidos=" + apellidosEncode + "&nombreCompania=" + nombreCompaniaEncode +  "&direccion="  + direccionEncode  + "&tienda=" + tempTienda +  "&zona=" + zonaEncode + "&observacion=" + observacionDirEncode + "&municipio=" + tempMunicipio + "&longitud=" + longitud + "&latitud=" + latitud + "&memcode=" + memcode + "&idcliente=" + idCliente + "&idnomenclatura=" + nomenclatura + "&numnomenclatura1=" + numNomenclatura1 + "&numnomenclatura2=" + numNomenclatura2 +  "&num3=" + num3 + "&telefonocelular=" + telCelular + "&email=" + email + "&politicadatos=" + politicaDatos, 
                                dataType: 'json', 
                                async: false, 
                                success: function(data){ 
                                    idClienteTemporal = data[0].idcliente;
                                    console.log("CLIENTE/CREADO ACTUALIZADO " + idClienteTemporal);
                                    var urlTienda;
                                    var pos;
                                    var dsnodbc;
                                    if (tempTienda != undefined)
                                    {
                                         $.ajax({ 
                                             url: server + 'ObtenerUrlTienda?idtienda=' + tienda, 
                                             dataType: 'json', 
                                             async: false, 
                                             success: function(data2){ 
                                                console.log(data2);
                                                urlTienda = data2[0].urltienda;
                                                pos = data2[0].pos;
                                                dsnodbc = data2[0].dsnodbc;
                                            } 
                                        });
                                        //Se consume el servicio de la tienda para la actualización creación del cliente
                                         $.ajax({ 
                                             url: urlTienda + 'ActualizarCliente?telefono=' + telef + "&nombres=" + nombresEncode + "&apellidos=" + apellidosEncode + "&nombreCompania=" + nombreCompaniaEncode +  "&direccion="  + direccionEncode  + "&tienda=" + tempTienda +  "&zona=" + zonaEncode + "&observacion=" + observacionDirEncode + "&municipio=" + tempMunicipio + "&longitud=" + longitud + "&latitud=" + latitud + "&memcode=" + memcode + "&idcliente=" + idCliente + "&idnomenclatura=" + nomenclatura + "&numnomenclatura1=" + numNomenclatura1 + "&numnomenclatura2=" + numNomenclatura2 +  "&num3=" + num3 + "&dsnodbc=" + dsnodbc + "&pos=" + pos + "&idmunicipio=" + idMunicipio + "&idtienda=" + tienda, 
                                            dataType: 'json', 
                                            async: false, 
                                            success: function(data1){
                                                idClienteTienda = data1[0].idclitienda;
                                                $.getJSON(server + 'ActualizarMemcode?membercode=' + idClienteTienda + "&idcliente=" + idClienteTemporal, function(data3){
                                                    //console.log(data3);
                                                 });
                                                $.alert('El cliente fue Creado/Actualizado correctamente.');

                                            }
                                        });
                                     }
                                     limpiarSeleccionCliente();

                                } 
                            });   
                        }
                    },
                    'No'    : {
                        'class' : 'gray',
                        'action': function(){}  // Nothing to do in this case. You can as well omit the action property.
                    }
                }
            });
    

}



function limpiarSeleccionCliente()
{
		$('#telefono').val("");
        $('#telcelular').val('');
        $('#email').val('');
        $('#politicadatos').prop('checked', false);
        $('#creacionligera').prop('checked', false);
		$('#nombres').val("");
        $('#apellidos').val("");
        $('#nombreCompania').val("");
        $('#direccion').val("");
        $('#zona').val("");
        $('#observacionDir').val("");
        $("#selectTiendas").val("");
        $("#selectNomenclaturas").val('');
        $('#numNomen').val("");
        $('#numNomen2').val("");
        $('#num3').val("");
        $('#descDireccion').val("");
        //reiniciamos el arreglo de productos
        //Volvemos  a habiliar el select de tiendas si es qúe está deshabilitado
        $('#selectTiendas').attr('disabled', false);
        $("#selectMunicipio").val("");
        $('#validaDir').prop('checked', true);
        $('#ultimospedidos').attr('disabled', true);
        $('#agregarOferta').attr('disabled', true);
        //$("#validaDir input[type=checkbox]").prop('checked', true);
        memcode = 0;
        idCliente = 0;
        if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) 
        {
			table = $('#grid-clientes').DataTable();
			table.clear().draw();
		}
}


function consultarUltimosPedidos()
{
		limpiarModalOtrosPedidos();
    	$('#grid-encabezadopedido').on('click', 'tr', function () {
    	var datosPedidoClick = dtEncabezadoPedido.row( this ).data();
        $('#NumPedidoClick').val(datosPedidoClick.idpedido);
        $('#ClienteClick').val(datosPedidoClick.cliente);
        $('#estadopedidoClick').val(datosPedidoClick.estadopedido);
        var tempEstadoPedidoPixel = datosPedidoClick.enviadopixel;
        if (tempEstadoPedidoPixel == 0)
        {
        	$('#estadotiendaClick').val("PENDIENTE TIENDA");
        	$("#estadotiendaClick").attr("disabled", true).css("background-color","#FF0000");
        }
        else
        {
        	$('#estadotiendaClick').val("ENVIADO A TIENDA");
        	$("#estadotiendaClick").attr("disabled", true).css("background-color","#00FF00");
        }
        $('#numpedidotiendaClick').val(datosPedidoClick.numposheader);
        // La idea es tomar el id pedido seleccionado y con esto ir a buscar la información.
        
        $.getJSON(server + 'GetClientePorID?idcliente=' + datosPedidoClick.idcliente, function(data1){
	                		
	                		$('#telefonoClick').val(data1[0].telefono);
	                		$('#nombresClick').val(data1[0].nombrecliente);
	                		$('#direccionClick').val(data1[0].direccion);
	                		$('#municipioClick').val(data1[0].nombremunicipio);
	                		$('#zonaClick').val(data1[0].zona);
	                		$('#observacionDirClick').val(data1[0].observacion);
	                		$('#tiendaClick').val(data1[0].nombretienda);

							
					});
        if ( $.fn.dataTable.isDataTable( '#grid-detallepedido' ) ) {
    		dtDetallePedido = $('#grid-detallepedido').DataTable();
    	}
        $.getJSON(server + 'ConsultarDetallePedido?numeropedido=' + datosPedidoClick.idpedido, function(data1){
	                		dtDetallePedido.clear().draw();
	                		for(var i = 0; i < data1.length;i++){
								dtDetallePedido.row.add(data1[i]).draw();
							}
	                		
							
					});

        //Obtenemos la forma de pago
        $.getJSON(server + 'ObtenerFormaPagoPedido?idpedido=' + datosPedidoClick.idpedido, function(data2){
	                		$('#totalpedidoClick').val(data2[0].valortotal);
	                		$('#valorpagoClick').val(data2[0].valorformapago);
	                		var valorDevolver =  data2[0].valorformapago - data2[0].valortotal;
	                		$('#valordevolverClick').val(valorDevolver);
	                		$('#formapagoClick').val(data2[0].nombre);
	                });
     

     } );
	$.getJSON(server + 'ConsultaUltimosPedidosCliente?idcliente=' + idCliente, function(data1){
	                		
	                		dtEncabezadoPedido.clear().draw();
							for(var i = 0; i < data1.length;i++){
								dtEncabezadoPedido.row.add(data1[i]).draw();
							}
							
					});
	$('div').click( function( e ) {
		e.stopPropagation();
	});
	$('#ultimosPedidosCliente').modal('show');
}

//Método que se encarga de limpiar la información de los pedidos clientes cada vez que se da sobre el botón consultar
function limpiarModalOtrosPedidos()
{
	$('#NumPedidoClick').val('');
    $('#ClienteClick').val('');
    $('#estadopedidoClick').val('');
    $('#estadotiendaClick').val('');
    $('#numpedidotiendaClick').val('');
    $('#telefonoClick').val('');
	$('#nombresClick').val('');
	$('#direccionClick').val('');
	$('#municipioClick').val('');
	$('#zonaClick').val('');
	$('#observacionDirClick').val('');
	$('#tiendaClick').val('');
	dtDetallePedido.clear().draw();
	$('#totalpedidoClick').val('');
	$('#valorpagoClick').val('');
	$('#valordevolverClick').val('');
	$('#formapagoClick').val('');
}

function initMap() {
        

        
        var map = new google.maps.Map(document.getElementById("mapas"), {
          zoom: 13,
          scrollwheel: false,
          center: {lat: 6.29139, lng: -75.53611}
        });

       
      }



// Evento para cuando se da  CLICK EN EL BOTÓN BUSCAR
function buscarMapaDigitado() {

    // Obtenemos la dirección y la asignamos a una variable
    var direccion = $('#direccion').val();
    var municipio = $("#selectMunicipio").val();
    municipio = municipio.toLowerCase();
    direccion = direccion + " " + municipio + " Antioquia Colombia";
    var resultado;
    
    $.ajax({ 
                        url:'https://maps.googleapis.com/maps/api/geocode/json?components=administrative_area:Medellin|country:Colombia&address=' + direccion +'&key=AIzaSyCRtUQ2WV0L2gMnb9DKiFn1PTHJQLH3suA' , 
                        dataType: 'json', 
                        async: false, 
                        success: function(data){ 
                                resultado = data;
                                
                            } 
                        });
    // Creamos el Objeto Geocoder
    var geocoder = new google.maps.Geocoder();
    // Hacemos la petición indicando la dirección e invocamos la función
    // geocodeResult enviando todo el resultado obtenido
    geocoder.geocode({ 'address': direccion}, geocodeResult);
    //geocodeResult(resultado.results,resultado.status);
}

// Método para el nuevo esquema de direcciones
function buscarMapaDigitado1() {

    //Se valida si es el esquema viejo de direcciones
    if($("#selectNomenclaturas").val() == '' || $("#selectNomenclaturas").val() == null || $("#numNomen").val() == '' || $("#numNomen").val() == null || $("#numNomen2").val() == '' || $("#numNomen2").val() == null )
        {
            if(($('#direccion').val() != null) && ($('#direccion').val() != '') && ($("#selectMunicipio").val() != null) && ($("#selectMunicipio").val() != ''))
            {
                buscarMapaDigitado();
            }
            return;
        }

        
    // Obtenemos la dirección y la asignamos a una variable
    var direccion = $("#selectNomenclaturas").val() +  " "  + $("#numNomen").val() + " # " + $("#numNomen2").val() ;
    var municipio = $("#selectMunicipio").val();
    if(municipio == '' || municipio== null)
    {
        $.alert('Debe ingresar el municipio para buscar la dirección.');
        return;
    }
    municipio = municipio.toLowerCase();
    direccion = direccion + " " + municipio + " Antioquia Colombia";
    var resultado;
    
    $.ajax({ 
                        url:'https://maps.googleapis.com/maps/api/geocode/json?components=administrative_area:Medellin|country:Colombia&address=' + direccion +'&key=AIzaSyCRtUQ2WV0L2gMnb9DKiFn1PTHJQLH3suA' , 
                        dataType: 'json', 
                        async: false, 
                        success: function(data){ 
                                resultado = data;
                                
                            } 
                        });
    // Creamos el Objeto Geocoder
    var geocoder = new google.maps.Geocoder();
    // Hacemos la petición indicando la dirección e invocamos la función
    // geocodeResult enviando todo el resultado obtenido
    geocoder.geocode({ 'address': direccion}, geocodeResult);
    //geocodeResult(resultado.results,resultado.status);
}

//Georeferenciación de la dirección

function buscarMapa(dir) {

    // Obtenemos la dirección y la asignamos a una variable
    var direccion = dir + " Antioquia Colombia" ; 
    var resultado;
    
    $.ajax({ 
                        url:'https://maps.googleapis.com/maps/api/geocode/json?components=administrative_area:Medellin|country:Colombia&address=' + direccion +'&key=AIzaSyCRtUQ2WV0L2gMnb9DKiFn1PTHJQLH3suA' , 
                        dataType: 'json', 
                        async: false, 
                        success: function(data){ 
                                resultado = data;
                                
                            } 
                        });
    // Creamos el Objeto Geocoder
    var geocoder = new google.maps.Geocoder();
    // Hacemos la petición indicando la dirección e invocamos la función
    // geocodeResult enviando todo el resultado obtenido
    geocoder.geocode({ 'address': direccion}, geocodeResult);
    //geocodeResult(resultado.results,resultado.status);
}

function geocodeResult(results, status) {
    // Verificamos el estatus
    if (status == 'OK') {
        // Si hay resultados encontrados, centramos y repintamos el mapa
        // esto para eliminar cualquier pin antes puesto
        var mapOptions = {
            center: results[0].geometry.location,
            mapTypeId: google.maps.MapTypeId.ROADMAP
        };
        longitud = results[0].geometry.location.lng();
        latitud = results[0].geometry.location.lat();
        map = new google.maps.Map($("#mapas").get(0), mapOptions);
        // fitBounds acercará el mapa con el zoom adecuado de acuerdo a lo buscado
        map.fitBounds(results[0].geometry.viewport);
        // Dibujamos un marcador con la ubicación del primer resultado obtenido
        //url: 'https://raw.githubusercontent.com/Andres-FA/KMLZonasDeReparto/master/ZonasDeRepartoTotales.kml',
        var ctaLayer = new google.maps.KmlLayer({
          url: 'https://raw.githubusercontent.com/Andres-FA/KMLZonasDeReparto/master/PizzaAmericana-ZonasDeRepartoTotales-Ver_03.kml',
          map: map,
          scrollwheel: false,
          zoom: 17
        });
        
        var markerOptions = { position: results[0].geometry.location }
        var marker = new google.maps.Marker(markerOptions);
        marker.setMap(map);
        //Luego de la ubicación en el mapa trataremos de ejecutar una función asincrona para ubicar dentro del mapa y ubicar la tienda
        ubicarTienda(latitud , longitud , map);
        
    } else {
        // En caso de no haber resultados o que haya ocurrido un error
        // lanzamos un mensaje con el error
        alert("La Geolocalización no tuvo éxito debido a: " + status);
    }
}

function geocodeSinServicio(lat, long) 
{
    longitud = long;
    latitud = lat;
    var map = new google.maps.Map($("#mapas").get(0), {
        zoom: 7,
        center: new google.maps.LatLng(6.22339, -75.6281),
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });

    var ctaLayer = new google.maps.KmlLayer({
        url: 'https://raw.githubusercontent.com/Andres-FA/KMLZonasDeReparto/master/PizzaAmericana-ZonasDeRepartoTotales-Ver_02.kml',
        map: map,
        scrollwheel: false,
        zoom: 17
    });
    var infowindow = new google.maps.InfoWindow();
    var marker, i;
    marker = new google.maps.Marker({
        position: new google.maps.LatLng(latitud, longitud),
        map: map,
        icon: 'http://maps.google.com/mapfiles/ms/icons/green-dot.png'
    });
}

function llenarSelectOferta()
{
    
    $.ajax({ 
                        url: server + 'CRUDOferta'+'?idoperacion=5' , 
                        dataType: 'json', 
                        async: false, 
                        success: function(data1)
                            { 
                                var str = '';
                                for(var i = 0; i < data1.length;i++){
                                    str +='<option value="'+ data1[i].idoferta +'" id ="'+ data1[i].idoferta +'">' + data1[i].nombreoferta + '</option>';
                                }
                                $('#selectOferta').html(str);
                                                                                    
                            } 
                        });

    
}

function guardarOfertaCliente()
{
    
    var resultado = validarPQRS();
    if(resultado == 'ERROR')
    {
        return;
    }
    var valPQRS = $("#PQRSOferta").val();
    var PQRS;
    if(valPQRS == '')
    {
        PQRS = 0;
    }else
    {
        PQRS = valPQRS;
    }
    var idoferta = $('#selectOferta').val();
    var observacionOferta = $('#observacionOferta').val();


     $.ajax({ 
                    url: server + 'CRUDOfertaCliente?idoperacion=1&idcliente=' + idCliente + "&idoferta=" + idoferta + "&observacion=" + observacionOferta + "&pqrs=" + PQRS , 
                    dataType: 'json', 
                    async: false, 
                    success: function(data){
                            var respuesta = data[0];
                            var idofertacliente = respuesta.idoferta;
                    } 
            }); 

    $('#selectOferta').val('');
    $("#PQRSOferta").val('');
    $('#observacionOferta').val('');
    $('#addData').modal('hide');
    pintarOfertasCliente(idCliente);
}

function pintarOfertasCliente(idcli)
{
    $.getJSON(server + 'CRUDOfertaCliente?idoperacion=5&idcliente=' + idcli, function(data1){
            tableOfertaCliente.clear().draw();
            for(var i = 0; i < data1.length;i++){
                tableOfertaCliente.row.add({
                    "idofertacliente": data1[i].idofertacliente, 
                    "idcliente": data1[i].idcliente,
                    "idoferta": data1[i].idoferta,
                    "nombreoferta": data1[i].nombreoferta, 
                    "utilizada": data1[i].utilizada,
                    "pqrs": data1[i].pqrs,
                    "ingresooferta": data1[i].ingresooferta, 
                    "usooferta": data1[i].usooferta,
                    "observacion": data1[i].observacion, 
                    "accion":'<button type="button" class="btn btn-default btn-xs" onclick="eliminarOfertaCliente(' +data1[i].idofertacliente + ')"><i class="fas fa-trash-alt fa-2x"></i>Eliminar</button> <button type="button" onclick="usarOferta('+data1[i].idofertacliente+')" class="btn btn-default btn-xs editButton" ' + 'data-id="' + data1[i].idofertacliente + '"><i class="fas fa-check fa-x"></i>Usar Oferta</button>'
                }).draw();
            }
        });
   
}


function usarOferta(idofertacliente)
{
    
           
            
            // The url and method might be different in your application
            $.ajax({ 
                    url: server + 'CRUDOfertaCliente?idoperacion=2&idofertacliente='+ idofertacliente  , 
                    dataType: 'json', 
                    async: false, 
                    success: function(data){
                        pintarOfertasCliente(idCliente);
                         // Hide the dialog
                                              
                    } 
            }); 
            //
            

        

}

function eliminarOfertaCliente(idofertacliente)
{
    /*var table = $('#grid-especialidades').DataTable();
    var datos = table.row( this ).data();
    console.log(datos);
    idespecialidad = datos.idespecialidad;
    nombre = datos.nombre;*/
    $.confirm({
            'title'     : 'Confirmacion Eliminación de la Oferta Cliente',
            'content'   : 'Desea confirmar la eliminación de la Oferta Cliente ' + idofertacliente+ '.',
            'buttons'   : {
                'Si'    : {
                    'class' : 'blue',
                    'action': function(){
                    
                        var resultado
                        $.ajax({ 
                        url: server + 'CRUDOfertaCliente?idoperacion=3&idofertacliente=' + idofertacliente , 
                        dataType: 'json', 
                        async: false, 
                        success: function(data){ 
                                resultado = data[0];
                                //

                                if ( $.fn.dataTable.isDataTable( '#grid-ofertascliente' ) ) {
                                    tableOfertaCliente = $('#grid-ofertascliente').DataTable();
                                }
                                
                                pintarOfertasCliente(idCliente);

                                //

                                
                            } 
                        });
                        


                    }
                },
                'No'    : {
                    'class' : 'gray',
                    'action': function(){}  // Nothing to do in this case. You can as well omit the action property.
                }
            }
        });
}

function inactivarCliente(idCliente)
{
    $.getJSON(server + 'InactivarCliente?idcliente=' + idCliente, function(data){
        if(data[0].resultado == "EXITOSO")
        {
            $.alert('El cliente fue inactivado Correctamente.');
            //Vuelve y se pinta la información
            validarTelefono();
        }

                
    });

}

function activarCliente(idCliente)
{
    $.getJSON(server + 'ActivarCliente?idcliente=' + idCliente, function(data){
        if(data[0].resultado == "EXITOSO")
        {
            $.alert('El cliente fue activado Correctamente.');
            //Vuelve y se pinta la información
            validarTelefono();
        }     
    });
}

function ValidacionesDatos()
{
    //validamos campo de telefono
    var tele =  telefono.value;
    if (tele == '' || tele == null)
    {
        alert ('Debe ingresar un telefono para el cliente');
        return;
    }


    if (!/^([0-9])*$/.test(tele))
    {
      alert("El valor " + tele + " no es un número");
      return;
    }

    //validamos campo Nombres
    var nomb = nombres.value;
    if (nomb == '' || nomb == null)
    {
        alert ('Debe ingresar los nombres del Cliente');
        return;
    }
        //validamos campo Direccion
    var dir = direccion.value;
    //if (dir == '' || dir == null)
    //{
    //  alert ('Debe ingresar la dirección del cliente');
    //  return;
    //}
    var nomenclatura = $("#selectNomenclaturas option:selected").val();
    var numNomen1 =  $("#numNomen").val();
    var numNomen2 =  $("#numNomen2").val();
    var num3 = $("#num3").val();
    if($('#validaDir').is(':checked'))
    {
        if (nomenclatura == '' || nomenclatura == null || numNomen1== '' || numNomen1 == null || numNomen1 == 'null' || numNomen2== '' || numNomen2 == null || numNomen2  == 'null' || num3== '' || num3 == null || num3 == 'null')
        {
            alert ('Faltan datos de la dirección, por favor completarlos');
            return;
        }
    }
    else
    {
        if (dir == '' || dir == null)
        {
            alert ('Aunque tiene definido no validar dirección, debe por lo menos ingresar la indicación del sitio');
            return;
        }
    }
    var tien = $("#selectTiendas option:selected").val();
    if (tien == '' || tien == null || tien == undefined)
    {
        alert ('Debe ingresar la tienda del pedido');
        return;
    }
    var muni = $("#selectMunicipio option:selected").val();
    if (muni == '' || muni == null|| muni == undefined)
    {
        alert ('Debe ingresar el Municipio del Cliente');
        return;
    }
    return(true);
}

function validarPQRS(){

    //Incluimos validación del logueo dado que es un punto crítico para retomar los pedidos
    validarVigenciaLogueo();
    // Validamos el tema de la longitud del pedido
    var valPQRS = $("#PQRSOferta").val();
    var resultado = "";
    if (valPQRS.trim() == '')
    {

    }else
    {
        if (!/^([0-9])*$/.test($("#PQRSOferta").val()))
        {
          $.alert("El valor " + $("#PQRSOferta").val() + " no es un número");
          return("ERROR");
        }
    }
    //Si no hay PQRS es decir es vacía no debería validarse
    
    if (valPQRS.trim() == '')
    {
        resultado = 'OK';
    }else
    {
        $.ajax({ 
                            url:server + 'ValidarPQRS?pqrs=' + valPQRS + "&idcliente=" + idCliente, 
                            dataType: 'json', 
                            async: false, 
                            success: function(data1){ 
                                var respuesta = data1[0];
                                if(respuesta.resultado == 'NOK')
                                {
                                    $.alert("Por favor validar la PQRS, pues o no existe o no pertenece a este cliente");
                                    resultado = "ERROR";
                                }
                                else
                                {
                                    resultado = "OK";
                                }
                            } 
                            });
    }
    return(resultado);

}

function ValidacionesLigerasDatos()
{
    //validamos campo de telefono
    var tele =  telefono.value;
    if (tele == '' || tele == null)
    {
        alert ('Debe ingresar un telefono para el cliente');
        return;
    }


    if (!/^([0-9])*$/.test(tele))
    {
      alert("El valor " + tele + " no es un número");
      return;
    }

    //validamos campo Nombres
    var nomb = nombres.value;
    if (nomb == '' || nomb == null)
    {
        alert ('Debe ingresar los nombres del Cliente');
        return;
    }
    return(true);
}
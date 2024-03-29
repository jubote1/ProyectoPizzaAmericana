	

var server;
var tiendas;
var table;
var tabledetalle;
var dtpedido;
var productos;
var excepciones;
var idPedido = 0;
var idOrdenComercio = 0;
var idTienda = 0;
var fechaPedido = "";
var tienda = "";
var idCliente = 0;
var idEstadoPedido = 0;
var longitud = 0;
var latitud = 0;
var urlTienda ="";
var idformapago = 0;
var totalpedido;
var valorformapago;
var stringPixel;
var administrador = "N";
//Debemos traer la información de WOMPI
var wompiClavePublica = "";
var wompiClavePrivada = "";
var wompiAmbiente = "";
var wompiEndPoint = "";
//Variable para almacenar la fecha en que se insertó el pedido
var fechaInsercion = "";

$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
	//Marcamos si es administrador para tomar ciertas acciones
	if(respuesta == 'OKA')
    {
        administrador = 'S';
    }else if(respuesta == 'OK')
    {
        administrador = 'N';
    }

    getListaMunicipios();
	getListaNomenclaturas();

	//Lo primero que realizaremos es validar si está logueado

	//Llenamos arreglo con los productos
	
	//cargarMapa();
	//Cargamos los productos tipo Pizza para el menu inicial
	
	//Final cargue productos pizza


    tabledetalle = $('#grid-detallepedido').DataTable( {
    		"aoColumns": [
    		{ "mData": "iddetallepedido" },
            { "mData": "nombreproducto" },
            { "mData": "cantidad" },
            { "mData": "especialidad1" },
            { "mData": "modespecialidad1" },
            { "mData": "especialidad2" },
            { "mData": "modespecialidad2" },
            { "mData": "valorunitario" },
            { "mData": "valortotal" },
            { "mData": "adicion" },
            { "mData": "observacion" },
            { "mData": "liquido" },
            { "mData": "excepcion" }
            
            
        ]
    	} );

    dtpedido = $('#grid-encabezadopedido').DataTable( {
    		"aoColumns": [
    		{ "mData": "idpedido" },
    		{ "mData": "idordencomercio" },
            { "mData": "tienda" },
            { "mData": "fechainsercion" },
            { "mData": "cliente" },
            { "mData": "direccion" },
            { "mData": "telefono" },
            { "mData": "totalneto" },
            { "mData": "estadopedido" },
            { "mData": "usuariopedido" },
            { "mData": "enviadopixel"  , "visible": false },
            { "mData": "estadoenviotienda" },
            { "mData": "numposheader"  },
            { "mData": "formapago"  },
            { "mData": "tiempopedido"  },
            { "mData": "idtienda", "visible": false },
            { "mData": "urltienda", "visible": false },
            { "mData": "stringpixel", "visible": false },
            { "mData": "idlink", "visible": false },
            { "mData": "fechafinalizacion", "visible": false },
            { "mData": "fechapagovirtual", "visible": false },
            { "mData": "grupovirtual", "visible": false },
            { "mData": "programado", "visible": false },
            { "mData": "horaprogramado", "visible": false },
            { "mData": "idtipopedido", "visible": false },
            { "mData": "usuarioreenvio", "visible": false }
        ],
        	"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
        		if(aData.grupovirtual == '1')
        		{
        			$(nRow).css('background-color', '#808080');
        		}else if(aData.grupovirtual == '2')
        		{
        			$(nRow).css('background-color', '#008080');
        		}
        		else if(aData.grupovirtual == '3')
        		{
        			$(nRow).css('background-color', '#FFE333');
        		}
        		$(nRow).children().each(function(index, td) { 
					if (index == 12) 
					{
						if(aData.formapago == 'Tienda Virtual-PAYU')
        				{
							$(td).css("background-color", "#FF974D");
						}else if(aData.formapago == 'Pago Virtual')
        				{
							$(td).css("background-color", "#00FF00");
						}
					}
				});

    		}
    	} );

     
    $('#grid-encabezadopedido').on('click', 'tr', function () {
        datospedido = table.row( this ).data();
        //alert( 'Diste clic en  '+datos.nombre+'\'s row' );
        //$('#nombres').val(datos.nombre);
        idPedido = datospedido.idpedido;
        idOrdenComercio = datospedido.idordencomercio;
        idCliente = datospedido.idcliente;
        urlTienda = datospedido.urltienda;
        stringPixel = datospedido.stringpixel;
        idTienda = datospedido.idtienda;
        tienda = datospedido.tienda;
        fechaPedido = datospedido.fechapedido;
        $('#NumPedido').val(idPedido);
        $('#Cliente').val(datospedido.cliente);
        $('#estadopedido').val(datospedido.estadopedido);
        var tempEstadoPedidoPixel = datospedido.enviadopixel;
        if (((tempEstadoPedidoPixel == 0) ||(tempEstadoPedidoPixel == 2)) && datospedido.estadopedido == 'Finalizado')
        {
        	$('#estadotienda').val("PENDIENTE TIENDA");
        	$("#estadotienda").attr("disabled", true).css("background-color","#FF0000");
        	$('#reenviarPedido').attr('disabled', false);
        	if(administrador == 'S')
		    {
		        $('#marcarPedido').attr('disabled', false);
		    }
        }
        else
        {
        	$('#estadotienda').val("ENVIADO A TIENDA");
        	$("#estadotienda").attr("disabled", true).css("background-color","#00FF00");
        	$('#reenviarPedido').attr('disabled', true);
        	$('#marcarPedido').attr('disabled', true);
        }
        if((administrador == 'S') && (datospedido.estadopedido == 'En curso'))
	    {
	        $('#cancelarPedido').attr('disabled', false);
	    }else
	    {
	    	$('#cancelarPedido').attr('disabled', true);
	    }
        $('#numpedidotienda').val(datospedido.numposheader);
        $('#idlink').val(datospedido.idlink);
        $('#linkparapago').val('https://checkout.wompi.co/l/'+datospedido.idlink);
        $('#fechafinalizacion').val(datospedido.fechafinalizacion);
        $('#fechapagovirtual').val(datospedido.fechapagovirtual);
        //Fijamos los valores de prográmación de pedido y tipo de pedido
        if(datospedido.programado == "S")
        {
        	$('#programado').val("SI");
        	$("#programado").attr("disabled", true).css("background-color","#FF0000");
        }else
        {
        	$('#programado').val("NO");
        	$("#programado").attr("disabled", true).css("background-color","#00FF00");
        }
        $('#horaprogramado').val(datospedido.horaprogramado);
        //Incluir validación del tema de pedido programado
		if($('#programado').val() == 'SI')
		{
			//Debemos de validar si el pedido fue programado entre las 0 horas y las 10 horas del día en cuyo caso
			// es mejor validar
			//Extraemos la hora 
			var horaStr = fechaInsercion.substr(11, 2);
			var hora = parseInt(horaStr, 10);
			if(hora <= 10)
			{
				$.alert('CUIDADO!! Es un pedido programado por la APP-Tienda Virtual y adicionalmente fue programado en la siguiente hora ' + fechaInsercion + ' se debe verificar antes de enviar a la tienda.');
			}else
			{
				$.alert('CUIDADO!! Es un pedido programado por la APP-Tienda Virtual.');
			}
		}
        if(datospedido.idtipopedido == "2")
        {
        	$('#tipopedido').val("RECOGER EN TIENDA");
        }else
        {
        	$('#tipopedido').val("DOMICILIO");
        }
        $('#usuarioreenvio').val(datospedido.usuarioreenvio);
        // La idea es tomar el id pedido seleccionado y con esto ir a buscar la información.
        
        $.getJSON(server + 'GetClientePorID?idcliente=' + datospedido.idcliente, function(data1){
	                		
	                		$('#telefono').val(data1[0].telefono);
	                		$('#telcelular').val(data1[0].telefonocelular);
	                		$('#email').val(data1[0].email);
	                		$('#nombres').val(data1[0].nombrecliente);
	                		$('#direccion').val(data1[0].direccion);
	                		$('#municipio').val(data1[0].nombremunicipio);
	                		$('#zona').val(data1[0].zona);
	                		$('#observacionDir').val(data1[0].observacion);
	                		$('#tienda').val(data1[0].nombretienda);
	                		$('#observacionVirtual').val(data1[0].observacionvirtual);
	                		latitud = data1[0].latitud;
	                		longitud = data1[0].longitud;
	                		//MÉTODO PARA UBICAR LAS COORDENADAS DEL CLIENTE
        					ubicarClienteExistente(latitud, longitud);
	                		if(data1[0].numnomenclatura1 == "0")
	                		{
	                			$.alert('El cliente tiene problemas con la dirección, por favor consultarlo y arreglar esta situación antes de enviar el pedido.');
	                		}
	                		//Actualizamos información de bloque de dirección posible para cambiar
	                		$('#direccionact').val(data1[0].direccion);
					        $("#selectNomenclaturas").val(data1[0].nomenclatura);
					        $("#selectMunicipio").val(data1[0].nombremunicipio);
					        $('#numNomen').val(data1[0].numnomenclatura1);
					        $('#numNomen2').val(data1[0].numnomenclatura2);
					        $('#num3').val(data1[0].num3);
					        $('#zonaact').val(data1[0].zona);
	                		$('#observacionDiract').val(data1[0].observacion);

							
					});
        if ( $.fn.dataTable.isDataTable( '#grid-detallepedido' ) ) {
    		tabledetalle = $('#grid-detallepedido').DataTable();
    	}
        $.getJSON(server + 'ConsultarDetallePedido?numeropedido=' + idPedido, function(data1){
	                		tabledetalle.clear().draw();
	                		for(var i = 0; i < data1.length;i++){
								tabledetalle.row.add(data1[i]).draw();
							}
	                		
							
					});

        //Obtenemos la forma de pago
        $.getJSON(server + 'ObtenerFormaPagoPedido?idpedido=' + idPedido, function(data2){
	                		var respuesta = data2[0];
							$('#totalpedido').val(data2[0].valortotal);
	                		$('#valorpago').val(data2[0].valorformapago);
	                		var valorDevolver =  data2[0].valorformapago - data2[0].valortotal;
	                		$('#valordevolver').val(valorDevolver);
	                		$('#formapago').val(data2[0].nombre);
	                		$('#descuento').val(data2[0].descuento);
	                		idformapago = data2[0].idformapago;
							totalpedido = data2[0].valortotal;
							valorformapago = data2[0].valorformapago;
							if(respuesta.virtual == 'S')
							{
								$('#reenviarNotificacion').attr('disabled', false);
								$('#recrearLink').attr('disabled', false);
								obtenerParametrosWOMPI();
							}else
							{
								$('#reenviarNotificacion').attr('disabled', true);
								$('#recrearLink').attr('disabled', true);
							}
							
					});


        //Obtenemos las marcaciones del Pedido
        $.getJSON(server + 'ObtenerMarcacionesPedido?idpedido=' + idPedido, function(data2){
	                		var respuesta = data2;
	                		var str = '<h1>Marcaciones Pedido</h1>';
        					str += '<table class="table table-bordered">';
							str += '<tbody>';
	                		for(var i = 0; i < respuesta.length;i++)
							{
								var cadaMarcacion  = respuesta[i];
								str +='<tr> ';
								str +='<td> ';
								str += '<label>Marcacion<input type="text" aria-label="..."' + '  value="'+ cadaMarcacion.nombremarcacion + '" id="' + cadaMarcacion.idmarcacion + '" disabled></label>';
								str += '</td>';
								str +='<td> ';
								str += '<label>Observacion<input type="text" ' + '" id="txtObsMarcacion' + cadaMarcacion.idmarcacion + '" value="'+ cadaMarcacion.observacion + '" name= "txtObsMarcacion' + cadaMarcacion.idmarcacion +'" maxlength="50" disabled></label>';
								str += '</td>';
					            str +='<td> ';
					            str += '<label>Descuento<input type="text" ' + ' id="txtDescuento' + cadaMarcacion.idmarcacion + '" name= "txtDescuento'  + cadaMarcacion.idmarcacion +'" maxlength="50" value="'+ cadaMarcacion.descuento + '" disabled></label>';
					            str += '</td>';
					            str +='<td> ';
					            str += '<label>Motivo<input type="text" ' + '" id="txtMotivo' + cadaMarcacion.idmarcacion + '" name= "txtMotivo'  + cadaMarcacion.idmarcacion +'" maxlength="50" value="'+ cadaMarcacion.motivo + '"" disabled>'  +'</label>';
					            str += '</td>';
								str += '</tr>';
							}
							$('#marcacionesPedido').html(str);
					});
    

     } );
 	

 	
 	
     

	} );


$(function(){
	
	getListaTiendas();
	getExcepcionesPrecios();
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

function validarTelefono(){

	if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) {
    	table = $('#grid-clientes').DataTable();
    }
	
	$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaCliente  = data1[i];
				table.row.add(data1[i]).draw();
			}
		});
		
}


function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		str +='<option value="'+ 'TODAS' +'" id ="'+ 'TODAS' +'">' + 'TODAS' +'</option>';
		$('#selectTiendas').html(str);
	});
}



function getExcepcionesPrecios(){
	$.getJSON(server + 'getExcepcionesPrecio', function(data){
		excepciones = data;
		var str = '';
		str += '<option value="vacio">Sin Excepcion</option>';
		for(var i = 0; i < excepciones.length;i++){
			var cadaExcepcion  = data[i];
			str +='<option value="'+ cadaExcepcion.idexcepcion +'" id ="' + cadaExcepcion.idproducto +'">' + cadaExcepcion.descripcion +'</option>';
		}
		$('#selectExcepcion').html(str);

	});
	var selExcepcion;
	var idSelExcepcion;
	var selCodigoProducto;
	$('#selectExcepcion').bind('change', function(){
		selExcepcion = $(this).val();
		idSelExcepcion = $(this).children(":selected").attr("id");
		selCodigoProducto = $("input:radio[name=tamanoPizza]:checked").val();
		if (selCodigoProducto != 'otros')
		{

			if (selCodigoProducto != idSelExcepcion)
			{
				alert("La excepción no está relacionada con el producto seleccionado, por favor corrija su elección");
				$("#selectExcepcion").val("vacio");
				return;
			}
			else
			{
				$.getJSON(server + 'GetSaboresLiquidoExcepcion?idExcepcion=' + selExcepcion, function(data1){
                		var strGas='';
                		var strGas = '<h2>Gaseosa</h2>';
                		strGas += '<table class="table table-bordered">';
                		strGas += '<tbody>';
						for(var i = 0; i < data1.length;i++){
							var cadaLiq  = data1[i];
							strGas +='<tr> ';
							strGas +='<td> ';
							strGas += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaLiq.idSaborTipoLiquido + '" id="' + cadaLiq.descripcionLiquido + '" name="liquido">' + cadaLiq.descripcionLiquido + '-' + cadaLiq.descripcionSabor +'</label>';
							strGas += '</td> </tr>';
						}
						strGas += '</tbody> </table>';
                		$('#frmgaseosas').html(strGas);
				});
			}
		}
		else
		{

			var selOtrosProductos = $("input:radio[name=otros]:checked").val();
			if(selOtrosProductos == '')
			{
				alert("Antes de seleccionar la Excepción de precio, debe terminar de seleccionar el producto");
				$("#selectExcepcion").val("vacio");
				return;
			}
			else
			{
				if (selOtrosProductos != idSelExcepcion)
				{
					alert("La excepción no está relacionada con el producto seleccionado, por favor corrija su elección");
					$("#selectExcepcion").val("vacio");
					return;
				}
				else
				{
					$.getJSON(server + 'GetSaboresLiquidoExcepcion?idExcepcion=' + selExcepcion, function(data1){
	                		var strGas='';
	                		strGas = '<h2>Gaseosa</h2>';
	                		strGas += '<table class="table table-bordered">';
                			strGas += '<tbody>';
							for(var i = 0; i < data1.length;i++){
								var cadaLiq  = data1[i];
								strGas +='<tr>';
								strGas +='<td>';
								strGas += '<label><input type="radio" aria-label="..."' + '  value="'+ cadaLiq.idSaborTipoLiquido + '" id="' + cadaLiq.descripcionLiquido + '" name="liquido">' + cadaLiq.descripcionLiquido + '-' + cadaLiq.descripcionSabor +'</label>';
								strGas += '</td> </tr>';
							}
							strGas += '</tbody> </table>';
	                		$('#frmgaseosas').html(strGas);
					});
				}
			}
		}
		
	});
}




// Evento para cuando se da  CLICK EN EL BOTÓN BUSCAR
function buscarMapa() {

    // Obtenemos la dirección y la asignamos a una variable
    var direccion = $('#direccion').val();
    var municipio = $("#selectMunicipio").val();
    municipio = municipio.loLowerCase();
    direccion = direccion + " " + municipio;
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
    var direccion = dir
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
        longitud = results[0].geometry.location.lng;
        latitud = results[0].geometry.location.lat;
        map = new google.maps.Map($("#mapas").get(0), mapOptions);
        // fitBounds acercará el mapa con el zoom adecuado de acuerdo a lo buscado
        map.fitBounds(results[0].geometry.viewport);
        // Dibujamos un marcador con la ubicación del primer resultado obtenido
        var ctaLayer = new google.maps.KmlLayer({
          url: 'https://raw.githubusercontent.com/Andres-FA/KMLZonasDeReparto/master/ZonasDeRepartoTotales.kml',
          map: map,
          zoom: 13
        });
        
        var markerOptions = { position: results[0].geometry.location }
        var marker = new google.maps.Marker(markerOptions);
        marker.setMap(map);
        
    } else {
        // En caso de no haber resultados o que haya ocurrido un error
        // lanzamos un mensaje con el error
        alert("Geocoding no tuvo éxito debido a: " + status);
    }
}

function consultarPedido() 
{

	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var tienda = $("#selectTiendas").val();
	var numpedido = $("#numeropedido").val();
	var estado = $("#selectEstado").val();
	var estadoTienda = $("#selectEstadoTienda").val();
	if(fechaini == '' || fechaini == null)
	{
		alert ('La fecha inicial debe ser diferente a vacía');
		return;
	}

	if(fechafin == '' || fechafin == null)
	{
		alert ('La fecha final debe ser diferente a vacía');
		return;
	}
	if(existeFecha(fechaini))
	{
	}
	else
	{
		alert ('La fecha inicial no es correcta');
		return;
	}

	if(existeFecha(fechafin))
	{
	}
	else
	{
		alert ('La fecha final no es correcta');
		return;
	}
	if(validarFechaMenorActual(fechaini, fechafin))
	{
	}
	else
	{
		alert ('La fecha inicial es mayor a la fecha final, favor corregir');
		return;
	}
	//Incluimos validación de cantidad de días de la consulta
	if(validarDiferenciaFechas(fechaini, fechafin))
	{
	}
	else
	{
		alert ('La diferencia entre la fecha Inicial y Final no puede ser mayor a 3 días');
		return;
	}

	if (tienda == '' || tienda == null)
	{

		alert ('La tienda no puede estar vacía');
		return;
	}
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-encabezadopedido' ) ) {
    		table = $('#grid-encabezadopedido').DataTable();
    }
	$.getJSON(server + 'ConsultaIntegradaPedidosNuevaTienda?fechainicial=' + fechaini +"&fechafinal=" + fechafin + "&tienda=" + tienda +  "&numeropedido=" + numpedido + "&estado=" + estado+"&estadotienda=" + estadoTienda, function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								var cadaPedido  = data1[i];
								table.row.add(data1[i]).draw();
							}
							
					});


}


function existeFecha(fecha){
      var fechaf = fecha.split("/");
      var day = fechaf[0];
      var month = fechaf[1];
      var year = fechaf[2];
      var date = new Date(year,month,'0');
      if((day-0)>(date.getDate()-0)){
            return false;
      }
      return true;
}

function validarFechaMenorActual(date1, date2){
      var fechaini = new Date();
      var fechafin = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaini.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechafin.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      
      if (fechaini > fechafin)
        return false;
      else
        return true;
}

function validarDiferenciaFechas(date1, date2){
      var fechaini = new Date();
      var fechafin = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaini.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechafin.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      var diferencia = fechafin - fechaini;
      var dias = diferencia/(1000*60*60*24);
      if(dias > 3)
      {
      	return(false);
      }else
      {
      	return(true);
      }
}

function enviarPedidoTienda(enviarTienda){
									//Hacemos la validación de la fecha tienda
									var valFecha = validarFechaPedido();
									if(!valFecha)
									{
										$.alert('Aparentemente el pedido tiene una fecha de pedido diferente a la actual debido a esto no puede ser enviado a la tienda, por favor revisar.');
										return;
									}
									//Hacer validación de pedidos con el teléfono
									validarTelefonoPedidos(telefono.value);
									//Revisamos el tiempo del pedido
									var tiempopedido;
									//Realizamos la validacion de si se debe enviar de acuerdo al estado actual
									var estEnviadoPixel = 0;
									$.ajax({ 
										url: server + 'ObtenerEstadoEnviadoPixel?idpedido=' + idPedido , 
										dataType: 'json',
										type: 'get', 
										async: false, 
										success: function(data2){
											estEnviadoPixel = data2[0].enviadopixel;	
										} 
									});
									if(estEnviadoPixel == 1)
									{
										$.alert('Aparentemente el pedido ya fue enviado a la tienda en otro proceso, por favor refrescar la búsqueda y verificar');
										return;
									}
									$.ajax({ 
										url: server + 'CRUDTiempoPedido?idoperacion=3&idtienda=' + idTienda , 
										dataType: 'json',
										type: 'get', 
										async: false, 
										success: function(data2){
											tiempopedido = data2.tiempopedido;			    
										} 
									});
									if (tiempopedido == '' || tiempopedido == null || tiempopedido == undefined || tiempopedido == 0)
									{
										tiempopedido = 'No hay tiempo definido para la tienda';
									}
									//Rediseños para mejorar las cosas
									$.confirm({
										'title'		: 'Confirmacion de Reenvío de Pedido',
										'content'	: 'Desea confirmar el reenvío del Pedido Número ' + idPedido + '<br> El Pedido pasará a estado  Finalizado'+
										'Con la siguiente información: <br>' +
										'CLIENTE: ' + $('#nombres').val() + ' ' + '<br>' +
										'DIRECCION ' +  $('#direccion').val() + '<br>' +
										'TOTAL PEDIDO ' + $("#totalpedido").val() + '<br>' +
										'CAMBIO ' + $("#valorpago").val() + '<br>' +
										'TIENDA DEL PEDIDO ' +  '<h1>' + $("#tienda").val().toUpperCase() + '</h1> <br>'+
										'<h5> Tiempo Aproximado Pedido :  ' + tiempopedido  + ' Minutos </h5>',
										'type': 'dark',
						   				'typeAnimated': true,
										'buttons'	: {
											'Si'	: {
												'class'	: 'blue',
												'action': function(){
													//OJO var idformapago =  $("#selectformapago").val();
													var formapago =  $("#formapago").val();
													var valorformapago =  $("#valorpago").val();
													var insertado = 0;
													$.ajax({ 
								    				url: server + 'FinalizarPedidoReenvio?idpedido=' + idPedido + "&idformapago=" + idformapago + "&valortotal=" + totalpedido + "&valorformapago=" + valorformapago + "&idcliente=" + idCliente + "&insertado=" + insertado + "&tiempopedido=" + tiempopedido, 
								    				dataType: 'json', 
								    				async: false, 
								    				success: function(data){ 

															resultado = data[0];
															var resJSON = JSON.stringify(resultado);
															var urlTienda = resultado.url;
															var memcodeMar = resultado.cliente.memcode;
															//Mandamos todos los párametros para la inserción de la tienda
															//Ejecutamos el servicio para insertar en Pixel

															if(enviarTienda)
															{
																//OJO CAMBIOS PARA EL SERVICIO CON LOS PARÁMETROS Y NO SERÁ AJAX SINO JSON ES DECIR ASINCRONO
																$.ajax({ 
														    				url: urlTienda + 'FinalizarPedidoPixel' , 
														    				dataType: 'json', 
														    				type: 'post', 
								    										data: {'datos' : resJSON }, 
														    				async: false, 
														    				success: function(data1){ 
																			var resPedPixel = data1[0];
																			if(resPedPixel.numerofactura > 0)
													    					{
													    						$.ajax({ 
															    				url: server + 'ActualizarNumeroPedidoPixel?idpedido=' + resPedPixel.idpedido + '&numpedidopixel=' + resPedPixel.numerofactura +  '&creacliente=' + resPedPixel.creacliente +  '&membercode=' + resPedPixel.membercode + '&idcliente=' + resPedPixel.idcliente, 
																    				dataType: 'json', 
																    				async: false, 
																    				success: function(data){
																    					var resul =  data[0];
																    					if (resul.resultado)
																    					{
																    						$('#estadotienda').val("ENVIADO A TIENDA");
									        												$("#estadotienda").attr("disabled", true).css("background-color","#00FF00");
									        												$('#reenviarPedido').attr('disabled', true);
									        												$('#marcarPedido').attr('disabled', true);
									        												$('#numpedidotienda').val(resPedPixel.numerofactura);
									        												alert("El pedido se ha enviado satisfactoriamente a la tienda");
																    					}
																    				},
																					error: function(){
																					    alert('Se produjo error en la actualización del número del pedido de la tienda en el sistema central');
																					    //Posiblemente aca sería necesario actualizar el estado
																					 } 

																				});
													    					}
													    					else if(resPedPixel.numerofactura == -1)
													    					{
													    						$.alert('No se ha iniciado el día de facturación en la tienda, por favor comuniquese con la misma, el pedido en cuestión no fue enviado' );
													    					}
																			
																		} 
																});
															}
															else
															{
																$.ajax({ 
												    					url: server + 'ActualizarNumeroPedidoPixel?idpedido=' + idPedido + '&numpedidopixel=' + '0' +  '&creacliente=' + 'true' +  '&membercode=' + memcodeMar + '&idcliente=' + idCliente, 
													    				dataType: 'json', 
													    				async: false, 
													    				success: function(data){
													    					var resul =  data[0];
													    					if (resul.resultado)
													    					{
													    						$('#estadotienda').val("ENVIADO A TIENDA");
						        												$("#estadotienda").attr("disabled", true).css("background-color","#00FF00");
						        												$('#reenviarPedido').attr('disabled', true);
						        												$('#marcarPedido').attr('disabled', true);
						        												$('#cancelarPedido').attr('disabled', true);
						        												$('#numpedidotienda').val('0');
						        												alert("El pedido se ha marcado como enviado, pero recuerda que no fue enviado a la tienda!");
													    					}
													    				},
																		error: function(){
																		    alert('Se produjo error en la actualización del número del pedido de la tienda en el sistema central');
																		    //Posiblemente aca sería necesario actualizar el estado
																		 } 

																});
															}
														},
														error: function(){
														    alert('Se produjo un error en la inserción del Pedido, favor revisar logs y reintentar');
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
									//Incluir validación del tema de pedido programado
									if($('#programado').val() == 'SI')
									{
										//Debemos de validar si el pedido fue programado entre las 0 horas y las 10 horas del día en cuyo caso
										// es mejor validar
										//Extraemos la hora 
										var horaStr = fechaInsercion.substr(11, 2);
										var hora = parseInt(horaStr, 10);
										if(hora <= 10)
										{
											$.alert('CUIDADO!! Es un pedido programado por la APP-Tienda Virtual y adicionalmente fue programado en la siguiente hora ' + fechaInsercion + ' se debe verificar antes de enviar a la tienda.');
										}else
										{
											$.alert('CUIDADO!! Es un pedido programado por la APP-Tienda Virtual.');
										}
									}
									consultarPedido();
									
									
}

function marcarPedidoTienda()
{
	enviarPedidoTienda(false);
}


function cancelarPedido()
{
	$.ajax({ 
		url: server + 'CancelarPedido?idpedido=' + idPedido, 
		dataType: 'json', 
		async: false, 
		success: function(data){
				alert("El pedido ha sido cancelado");
				$('#cancelarPedido').attr('disabled', true);
				consultarPedido();
			
		},
		error: function(){
		    alert('Se produjo error al cancelar el pedido');
		    //Posiblemente aca sería necesario actualizar el estado
		} 

	});
}

function reenviarNotificacion()
{
	if($('#idlink').val() != '')
	{
		$.confirm({
			'title'		: 'Confirmacion de Reenvío de información de Link de Pagos',
			'content'	: 'Desea confirmar que se envíe de nuevo la información para el pago del Pedido Número ' + idPedido + '?<br> Se enviaría mensaje de texto y correo en caso de tenerlo.',
			'type': 'dark',
				'typeAnimated': true,
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
						enviarNotificacionWompi($('#idlink').val(), idCliente, "https://checkout.wompi.co/l/"+$('#idlink').val(), idformapago, idPedido);
					}
				},
				'No'	: {
					'class'	: 'gray',
					'action': function(){}	// Nothing to do in this case. You can as well omit the action property.
				}
			}
		});
		consultarPedido();
	}else
	{
		$.alert("CUIDADO! No hay idLink generado para enviar, por lo cual se debería primero recrear el Link de Pagos, por lo tanto no se ha enviado ninguna información.");
	}
	
}


function obtenerParametrosWOMPI()
{

    $.getJSON(server + 'GetParametro?parametro=WOMPIAMBIENTE' , function(data2){
        wompiAmbiente = data2.valortexto;
        if(wompiAmbiente == 'P')
        {
            $.getJSON(server + 'GetParametro?parametro=WOMPIPRODUCCIONPUB' , function(data3){
                wompiClavePublica = data3.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPIPRODUCCIONPRI' , function(data4){
                wompiClavePrivada = data4.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPIENDPOINTP' , function(data5){
                wompiEndPoint = data5.valortexto;
            });




        }else if(wompiAmbiente == 'C')
        {
            $.getJSON(server + 'GetParametro?parametro=WOMPISANDBOXPUB' , function(data3){
                wompiClavePublica = data3.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPISANDBOXPRI' , function(data4){
                wompiClavePrivada = data4.valortexto;
            });

            $.getJSON(server + 'GetParametro?parametro=WOMPIENDPOINTC' , function(data5){
                wompiEndPoint = data5.valortexto;
            });
        }
    });

}

//Método que una vez creado
function enviarNotificacionWompi(idLink, idCliNoti, linkPago, idFormaPago, idPed)
{
    $.getJSON(server + 'RealizarNotificacionWompi?idlink='+ idLink +'&idcliente='+ idCliNoti + '&linkpago=' + linkPago + '&idformapago=' + idFormaPago + '&idpedido=' + idPed , function(data1){
        var respuesta = data1[0].respuesta;
    });
}

//Método que se encargará  de recrar un link porque aparentemente se tiene problemas con el actual
function recrearLink()
{
    if(($('#fechapagovirtual').val() != '') && ($('#fechapagovirtual').val() != null))
    {
    	$.alert("No se puede recrear el link de pago, debido a que ya fue recibido el pago del pedido. CUIDADO!");
    	return;
    }
    $.confirm({
		'title'		: 'Confirmacion de Recreación de Link de Pagos',
		'content'	: 'Desea confirmar que se recree un nuevo Link de Pagos para el Pedido Número ' + idPedido + '<br> El anterior Link ya no deberá ser usado',
		'type': 'dark',
			'typeAnimated': true,
		'buttons'	: {
			'Si'	: {
				'class'	: 'blue',
				'action': function()
				{
					//Como habrá uso de WOMPI traemos las variables
					//Generamos el JSON con los valores para consumir el servicio
			        var dateExp = new Date();
			        dateExp.setDate(dateExp.getDate() + 1);
			        var expMes = dateExp.getMonth()+1;
			        if(expMes < 10)
			        {
			            expMes = "0" + expMes;
			        }
			        var expDia = dateExp.getDate();
			        if(expDia  < 10)
			        {
			            expDia = "0" + expDia;
			        }
			        var strFechaExp = dateExp.getFullYear() + "-" + expMes + "-" + expDia;
			        var jsonLinkPago = '{' +
			              '"amount_in_cents": ' + (totalpedido*100) +','+
			              '"currency": "COP",' + 
			              '"name": "Pizza Americana ' + tienda.toUpperCase() + ' ",'+
			              '"description": "Pedido #' + idPedido + '",'+
			              '"expires_at": "' + strFechaExp + 'T23:00:00.000Z",'+
			              '"redirect_url": "https://pizzaamericana.co/wompi/",'+
			              '"single_use": true,'+
			              '"sku": "' + idPedido + '",'+
			              '"collect_shipping": false'+
			            '}';
			        //Lanzamos la creación de la transacción
			        $.ajax({
			                url: wompiEndPoint + "payment_links",
			                headers: {'Authorization': "Bearer " + wompiClavePrivada},
			                dataType: 'json', 
			                type: 'post',
			                data: jsonLinkPago, 
			                contentType: "application/json; charset=utf-8",
			                async: false, 
			                success: function(dataLink){
			                    //Recuperaremos el valor del id para el envío del link al cliente
			                    var idLink = dataLink.data.id;
			                    $('#idlink').val(idLink);
			                    $('#linkparapago').val('https://checkout.wompi.co/l/' + idLink);
			                    //Vamos a refrescar el ikLink en el campo de texto
			                    enviarNotificacionWompi(idLink, idCliente, "https://checkout.wompi.co/l/"+idLink, idformapago, idPedido);
			                    consultarPedido();
			                    //alert(dataLink);
			                },
			                error: function(dataLinkError){
			                    alert('SE PRODUJO UN ERROR');
			                    console.log(dataLinkError);
			                    //process the JSON data etc
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

function validarTelefonoPedidos( telef)
{
     $.getJSON(server + 'ValidarTelefonoPedRadicado?telefono=' + telef, function(data2){
        var cantidadPedTel = data2.cantidad;
        //Si la cantidad de pedidos para el teléfono es mayor a cero  lanzaremos la alerta
        if(cantidadPedTel > 0)
        {
            $.confirm(
            {
                    'title'     : 'OJO! CON ESTE NÚMERO TELÉFONICO',
                    'content'   : 'Con este número telefónico ya hay ' + cantidadPedTel + ' pedidos registrados para hoy, por favor verifique muy bien la situación.',
                    'buttons'   : {
                        'Enterado'  : {
                            'class' : 'blue',
                            'action': function()
                            {
                            }
                        }
                    }
            });
        }
    });
}

function validarFechaPedido()
{
	var fecha = new Date()
	var dia = fecha.getDate();
	if(dia < 10)
	{
		dia = "0" + dia;
	}
	var mes = fecha.getMonth()+1;
	if(mes < 10)
	{
		mes = "0" + mes;
	}
	var fechaActual = fecha.getFullYear()+"-"+mes+"-" + dia;
	console.log(fechaActual);
	if(fechaActual == fechaPedido)
	{
		return(true);
	}else
	{
		return(false);
	}
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

function fijarCoordenadasManualmente(coordenadaMapa)
{
    latitud = coordenadaMapa.lat;
    longitud = coordenadaMapa.lng;
}

//Este método tendrá como objetivo realizar cambios en el contenido para prevenir posibles fallas en la búsqueda de la dirección
function validarNumNomen()
{
    var nuevoValor = "";
    //Para nuestra división geográfica puede ser una letra o letras o la palabra sur
    var numNom= jQuery("#numNomen").val();
    var arraySep = numNom.split(' ');
    var strTemp = "";
    var strTemp2 = "";
    var strTemp3 = "";
    if(arraySep.length > 0)
    {
        strTemp = arraySep[0];
        if(arraySep.length == 1)
        {
            //Si todo está junto no tenemos problema a excepción de que la palabra sur este pegada
            //Si el valor retornado es -1 es porque no tiene la palabra sur
            strTemp = strTemp.toUpperCase();
            var encSur = strTemp.indexOf('SUR');
            if(encSur == -1)
            {
                nuevoValor = strTemp;
            }else
            {
                //Si tiene la palabra sur pegada, debemos de separarla
                nuevoValor = strTemp.substring(0,encSur) + " " + "SUR";
            }
        }else if(arraySep.length > 1 && arraySep.length <=3)
        {
            strTemp2 = arraySep[1];
            strTemp2 = strTemp2.toUpperCase();
            var encSur2 = strTemp2.indexOf('SUR');
            if(arraySep.length == 2)
            {
                if(encSur2 >= 0)
                {
                    nuevoValor = strTemp + strTemp.substring(0 , encSur2) + " " + "SUR";
                }else
                {
                    nuevoValor = strTemp + strTemp2;
                }
                
            }else if(arraySep.length == 3)
            {
                strTemp3 = arraySep[2];
                strTemp3 = strTemp3.toUpperCase();
                var encSur3 = strTemp3.indexOf('SUR');
                if(encSur2 >= 0)
                {
                    alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 SUR, 78B SUR ");
                    return;
                }
                if(encSur3 >= 0)
                {
                    nuevoValor = strTemp + strTemp2 + " " + strTemp3;
                }else
                {
                    alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 SUR, 78B SUR ");
                    return;
                }
            }
        }else if(array.length > 3)
        {
            alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 SUR, 78B SUR ");
            return;
        }
   }else
   {
       alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 sur, 78B sur ");
       return;
   }
   jQuery("#numNomen").val(nuevoValor);
}

function validarNumNomen2()
{
    var nuevoValor = "";
    //Para nuestra división geográfica puede ser una letra o letras o la palabra sur
    var numNom= jQuery("#numNomen2").val();
    var arraySep = numNom.split(' ');
    var strTemp = "";
    var strTemp2 = "";
    var strTemp3 = "";
    if(arraySep.length > 0)
    {
        strTemp = arraySep[0];
        if(arraySep.length == 1)
        {
            //Si todo está junto no tenemos problema a excepción de que la palabra sur este pegada
            //Si el valor retornado es -1 es porque no tiene la palabra sur
            strTemp = strTemp.toUpperCase();
            var encSur = strTemp.indexOf('SUR');
            if(encSur == -1)
            {
                nuevoValor = strTemp;
            }else
            {
                //Si tiene la palabra sur pegada, debemos de separarla
                nuevoValor = strTemp.substring(0,encSur) + " " + "SUR";
            }
        }else if(arraySep.length > 1 && arraySep.length <=3)
        {
            strTemp2 = arraySep[1];
            strTemp2 = strTemp2.toUpperCase();
            var encSur2 = strTemp2.indexOf('SUR');
            if(arraySep.length == 2)
            {
                if(encSur2 >= 0)
                {
                    nuevoValor = strTemp + strTemp.substring(0 , encSur2) + " " + "SUR";
                }else
                {
                    nuevoValor = strTemp + strTemp2;
                }
                
            }else if(arraySep.length == 3)
            {
                strTemp3 = arraySep[2];
                strTemp3 = strTemp3.toUpperCase();
                var encSur3 = strTemp3.indexOf('SUR');
                if(encSur2 >= 0)
                {
                    alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 SUR, 78B SUR ");
                    return;
                }
                if(encSur3 >= 0)
                {
                    nuevoValor = strTemp + strTemp2 + " " + strTemp3;
                }else
                {
                    alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 SUR, 78B SUR ");
                    return;
                }
            }
        }else if(array.length > 3)
        {
            alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 SUR, 78B SUR ");
            return;
        }
   }else
   {
       alert("Valor incorrecto, esperamos valores por ejemplo como 46 , 67A , 78 sur, 78B sur ");
       return;
   }
   //console.log(nuevoValor);
   jQuery("#numNomen2").val(nuevoValor);
}

//Función que permite validar que se ingrese 
function validarNum3()
{
    var numero3 = jQuery("#num3").val();
    var arraySep = numero3.split(' ');
    if(arraySep.length > 0)
    {
         if(arraySep.length > 1)
        {
            alert("Valor incorrecto, esperamos valores como 46 , 103 , 02. Si deseas colocar un complemento de la dirección más adelante lo podrás hacer.");
            jQuery("#num3").val(arraySep[0]);
            return;
        }else if(arraySep.length == 1)
        {
            if(isNaN(numero3))
            {
                alert("Valor incorrecto, esperamos valores como 46 , 103 , 57 ");
                jQuery("#num3").val('');
                return;
            }
        }
    }
    else
    { 
        alert("Valor incorrecto, esperamos valores como 46 , 103 , 02 ");
        return;
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


function actualizarUbicacion()
{
	$.confirm({
		'title'		: 'Confirmacion de Actualización Dirección',
		'content'	: 'Desea confirmar que se actualice la dirección ' + $('#direccionact').val(),
		'type': 'dark',
			'typeAnimated': true,
		'buttons'	: {
			'Si'	: {
				'class'	: 'blue',
				'action': function()
				{
					var tempMunicipio = $("#selectMunicipio option:selected").val();
					var direccionEncode = encodeURIComponent(direccionact.value);
					var zonaEncode = encodeURIComponent(zonaact.value); 
					var observacionDirEncode = encodeURIComponent(observacionDiract.value);
					var nomenclatura =  $("#selectNomenclaturas option:selected").attr('id');
					var numNomenclatura1 = encodeURIComponent($("#numNomen").val());
					var numNomenclatura2 = encodeURIComponent($("#numNomen2").val());
					var num3 = encodeURIComponent($("#num3").val());
					$.ajax({ 
    				url: server + "ActualizarClienteDireccion?direccion="  + direccionEncode  +  "&zona=" + zonaEncode + "&observacion=" + observacionDirEncode + "&municipio=" + tempMunicipio + "&longitud=" + longitud + "&latitud=" + latitud + "&idcliente=" + idCliente  + "&idnomenclatura=" + nomenclatura + "&numnomenclatura1=" + numNomenclatura1 + "&numnomenclatura2=" + numNomenclatura2 +  "&num3=" + num3, 
    				dataType: 'json', 
    				async: false, 
	    				success: function(data){ 
							respuesta = data;
							if (respuesta.respuesta == 'true')
							{
								alert("Se ACTUALIZÓ CORRECTAMENTE el cliente.");
							}
							else
							{
								alert("Se tuvieron inconvenientes al momento de actualizar el cliente");
								return;
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
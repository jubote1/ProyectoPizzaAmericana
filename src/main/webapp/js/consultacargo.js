var server;
var tiendas;
var table;
var tabledetalle;
var tableAplicabilidadRappi; // Variable para el nuevo DataTable
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
var idMarcacionSel = 0;
var tableEnviadosTiendaRappiCargo;
var ARCGIS_API_KEY = window.ARCGIS_API_KEY || "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";


$(document).ready(function() {
	
	

	// Obtener la URL base de tu proyecto "ProyectoPizzaAmericana"
	const loc = window.location;
	const pathParts = loc.pathname.split('/');
	const baseFolder = "ProyectoPizzaAmericana";
	const index = pathParts.indexOf(baseFolder);

	// Reconstruir la URL base completa del proyecto
	server = `${loc.origin}/${pathParts.slice(1, index + 1).join("/")}/`;

	// Variable para almacenar la respuesta del servidor
	let respuesta = '';
	let usuario = '';

	// Validar el usuario mediante una petición AJAX síncrona
	$.ajax({
	    url: server + 'ValidarUsuarioAplicacion',
	    dataType: 'json',
	    type: 'POST',
	    async: false, // ⚠️ Sincrónico: se recomienda cambiar si puedes usar async/await
	    success: function (data) {
	        respuesta = data[0]?.respuesta || '';
	        usuario = data[0]?.nombreusuario || '';
	    },
	    error: function () {
	        console.error("Error al validar el usuario.");
	        location.href = server + "Index.html";
	    }
	});

	// Cargar el menú o redirigir según el tipo de usuario
	switch (respuesta) {
	    case 'OK': // Usuario común
	        $('#cargarMenu').load(server +"Menu.html", function () {
					    $('#usuariologin').text(usuario);
					    $('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
					});
		    administrador = 'N';
			$('#usuariologin').html(usuario);
	        break;

	    case 'OKA': // Usuario administrador		
			$('#cargarMenu').load(server + "MenuAdm.html", function () {
			    $('#usuariologin').text(usuario);
			    $('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
			});
			administrador = 'S';
			$('#usuariologin').html(usuario);
	        break;

	    case 'OKP': // Usuario PQRS
	        $('#cargarMenu').load(server +"MenuPQRS.html", function () {
					    $('#usuariologin').text(usuario);
					    $('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
					});
			$('#usuariologin').html(usuario);
	        break;

	    default: // No válido o sin sesión
	        location.href = server + "Index.html";
	        break;
	}

	//Lo primero que realizaremos es validar si está logueado

	//Llenamos arreglo con los productos
	
	//cargarMapa();
	//Cargamos los productos tipo Pizza para el menu inicial
	
	//Final cargue productos pizza


    dtpedido = $('#grid-detallepedido').DataTable( {
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
            { "mData": "aceptadorappi"  },
            { "mData": "idtienda", "visible": false },
            { "mData": "urltienda", "visible": false },
            { "mData": "stringpixel", "visible": false },
            { "mData": "idlink", "visible": false },
            { "mData": "fechafinalizacion", "visible": false },
            { "mData": "fechapagovirtual", "visible": false }
        ],
        	"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
        		if(aData.formapago == 'Tienda Virtual-Epayco')
        		{
        			$(nRow).css('background-color', '#FF974D');
        		}
    		}
    	} );
		
		
		tableEnviadosTiendaRappiCargo = $('#grid-enviados-tienda-rappicargo').DataTable({
		    "aoColumns": [
		        { "mData": "idpedidotienda" },
		        { "mData": "fechainsercion" },
		        { "mData": "estadoActual" },
		        { "mData": "minutosDesdeIngreso" },
		        { "mData": "fechaCocina" },
		        { "mData": "minutosCocina" },
		        { "mData": "nombreCompleto" },
		        { "mData": "direccion" },
		        { "mData": "telefono" },
		        { "mData": "telefonoCelular" },
		        { "mData": "totalNeto" },
		        { "mData": "idFormaPago" }
		    ],
			createdRow: function (row, data) {
			    if (esPedidoTercerizado(data)) {
			        $(row).addClass('pedido-tercerizado-cargo');
			    }
			}
		});


    // Inicialización del nuevo DataTable para ConsultarAplicabilidadPedidoRAPPICARGO
	tableAplicabilidadRappi = $('#grid-aplicabilidad-rappicargo').DataTable({

	    "aoColumns": [

	        {
	            "mData": null,
	            "className": "text-center",
	            "orderable": false,
	            "render": function(data, type, row){

	                if(row.aplicaRappiCargoDist){
	                    return "";
	                }

	                return '<i class="fa fa-info-circle text-primary informacion-rappi" ' +
	                       'title="' + row.motivoNoAplicaRappiCargo + '" ' +
	                       'style="cursor:pointer;font-size:18px;"></i>';
	            }
	        },

	        { "mData": "idpedido" },
	        { "mData": "tienda" },
	        { "mData": "totalneto" },
	        { "mData": "idcliente" },
	        { "mData": "cliente" },
	        { "mData": "estadopedido" },
	        { "mData": "estadoenviotienda" },
	        { "mData": "numposheader" },
	        { "mData": "fechapedido" },
	        { "mData": "fechainsercion" },
	        { "mData": "usuariopedido" },
	        { "mData": "direccion" },
	        { "mData": "telefono" },
	        { "mData": "formapago" },
	        { "mData": "tiempopedido" }
	    ],

	    createdRow: function(row, data){

	        if(!data.aplicaRappiCargoDist){
	            $(row).addClass("pedido-no-aplica");
	        }

	    }

	});

    // Llamada inicial al servicio del nuevo datatable al cargar la página
    consultarAplicabilidadRappiCargo();

     
    $('#grid-encabezadopedido').on('click', 'tr', function () {
        datospedido = table.row( this ).data();
		
		// Si no hay datos (por ejemplo, se dio clic en la fila "No hay datos" o en el header),
		// no continuamos para evitar el error.
		if (!datospedido) {
		    return;
		}
        //alert( 'Diste clic en  '+datos.nombre+'\'s row' );
        //$('#nombres').val(datos.nombre);
        idPedido = datospedido.idpedido;
        idOrdenComercio = datospedido.idordencomercio;
        //Realizamos validación para saber si el pedido viene por API
        if(datospedido.idordencomercio > 0)
        {
        	$('#aceptarPedidoPlat').attr('disabled', false);
        }else
        {
        	$('#aceptarPedidoPlat').attr('disabled', true);
        }
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
	                		var str = '<h5 style="font-weight:bold; color:#1f2937; text-align:center; margin-bottom:15px;">Marcaciones Pedido</h5>';
        					str += '<table class="table table-bordered">';
							str += '<tbody>';
	                		for(var i = 0; i < respuesta.length;i++)
							{
								var cadaMarcacion  = respuesta[i];
								idMarcacionSel = cadaMarcacion.idmarcacion;
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
					            str +='<td> ';
					            str += '<label>MarketPlace<input type="text" ' + '" id="txtMarket' + cadaMarcacion.marketplace + '" name= "txtMarket'  + cadaMarcacion.marketplace +'" maxlength="2" value="'+ cadaMarcacion.marketplace + '"" disabled>'  +'</label>';
					            str += '</td>';
								str += '</tr>';
								str += '<tr>';
								str +='<td colspan=5> ';
								str += '<label>'+ cadaMarcacion.log +'</label>';
								str += '</td>';
								str += '</tr>';
							}
							$('#marcacionesPedido').html(str);
					});
     

     } );
 	
	 consultarYCargarInicial();
 	
 	

	} );

	function consultarYCargarInicial() {
		// Pequeño retardo para asegurar que los selectores dependientes (como tiendas) carguen primero
		setTimeout(function() {
			consultarPedido();
		}, 300);
	}

$(function(){
	
	getListaTiendas();
	getExcepcionesPrecios();
	getPlataformas();
	setInterval('validarVigenciaLogueo()',600000);
	//Aqui realizaremos validación de si hay pedidos pendientes rappi
	setInterval('obtenerPedidosPendientesRAPPI()',120000);
	obtenerPedidosPendientesRAPPI();
});

// Función para invocar el nuevo servicio y cargar el DataTable de Aplicabilidad
function consultarAplicabilidadRappiCargo() {
	if ($.fn.dataTable.isDataTable('#grid-aplicabilidad-rappicargo')) {
		tableAplicabilidadRappi = $('#grid-aplicabilidad-rappicargo').DataTable();
	}
	
	$.getJSON(server + 'ConsultarAplicabilidadPedidoRAPPICARGO', function(response) {

	    tableAplicabilidadRappi.clear().draw();

	    if (response.error) {
			Swal.fire({
			    icon: 'error',
			    title: 'Error',
			    text: response.mensaje ,
			    customClass: {
			        popup: 'mi-swal',
			        title: 'mi-swal-title',
			        icon: 'mi-swal-icon'
			    }
			});
	        return;
	    }

	    if (!response.datos || response.datos.length === 0) {
	        return;
	    }

	    tableAplicabilidadRappi.rows.add(response.datos).draw();

		$('.informacion-rappi').tooltip();


	}).fail(function() {
		Swal.fire({
		    icon: 'error',
		    title: 'Error',
		    text: 'Error al consumir el servicio.',
		    customClass: {
		        popup: 'mi-swal',
		        title: 'mi-swal-title',
		        icon: 'mi-swal-icon'
		    }
		});
	});
}

function obtenerPedidosPendientesRAPPI(){
	$.getJSON(server + 'ConsultarPedidosPendientesRAPPI', function(data1){
			//console.log("RESULTADO " + data1.resultado);
			if(data1.resultado)
			{
				console.log("LA RESPUESTA FUE POSITIVA");
				//Aqui se realiza la notificación
				showNotification();
			}
		});
}

function showNotification() {
  if (!("Notification" in window)) {
    //Comprobar si el navegador admite notificaciones
    alert("This browser does not support desktop notification");
  } else if (Notification.permission === "granted") {
   // Comprobar si ya se han concedido los permisos de notificación;
    // si es así, crea una notificación
	CrearNotification();
  } else if (Notification.permission !== "denied") {
    // We need to ask the user for permission
    Notification.requestPermission().then((permission) => {
      // If the user accepts, let's create a notification
      if (permission === "granted") {
             // Crear una nueva notificación
			 CrearNotification();
      }
    });
  }


}

function CrearNotification() {
        var notification = new Notification('Pedido Rappi', {
          body: 'Hay un pedido pendiente para aceptar.',
		  icon: 'img/logo.jpeg',
		  image: 'img/rappi.jpeg'
        });
        
        // Reproducir el sonido de la notificación
        var sound = document.getElementById('notificationSound');
        sound.play();
	}




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
	$.getJSON(server + 'GetTiendasFuncionales', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		
		
		opciones = str + '<option value="0" id="0">Seleccionar...</option>';
		$('#ListTiendas').html(opciones).val('0');
		
		str += '<option value="TODAS" id="TODAS">TODAS</option>';
		$('#selectTiendas').html(str).val('TODAS');
		
	
	});
}


function getPlataformas(){
	$.getJSON(server + 'ObtenerMarcaciones?adm=S', function(data){
        var marcaciones = data;
		var str = '';
		for(var i = 0; i < marcaciones.length;i++){
			var cadaPlataforma  = marcaciones[i];
			str +='<option value="'+ cadaPlataforma.idmarcacion +'" id ="'+ cadaPlataforma.idmarcacion +'">' + cadaPlataforma.nombremarcacion +'</option>';
		}
		str +='<option value="'+ 'TODAS' +'" id ="0">' + 'TODAS' +'</option>';
		$('#selectPlataforma').html(str);
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

function limpiarDetallePedido()
{
	idPedido = 0;
	idOrdenComercio = 0;
	idTienda = 0;
	fechaPedido = "";
	tienda = "";
	idCliente = 0;
	idEstadoPedido = 0;
	longitud = 0;
	latitud = 0;
	urlTienda = "";
	idformapago = 0;
	totalpedido = "";
	valorformapago = "";
	stringPixel = "";
	idMarcacionSel = 0;

	$('#telefono, #telcelular, #email, #nombres, #direccion, #municipio, #zona, #tienda, #observacionDir, #observacionVirtual').val('');
	$('#NumPedido, #Cliente, #estadopedido, #estadotienda, #numpedidotienda, #totalpedido, #valorpago, #valordevolver, #formapago, #valorformapago, #descuento, #idlink, #fechafinalizacion, #fechapagovirtual, #linkparapago').val('');
	$('#estadotienda').css('background-color', '');
	$('#marcacionesPedido').empty();

	$('#aceptarPedidoPlat, #reenviarPedido, #marcarPedido, #cancelarPedido, #reenviarNotificacion, #recrearLink').attr('disabled', true);

	if ($.fn.dataTable.isDataTable('#grid-detallepedido')) {
		$('#grid-detallepedido').DataTable().clear().draw();
	}
}

function consultarPedido() 
{
	var fechaini = $("#fechainicial").val();
	var fechafin = $("#fechafinal").val();
	var tienda = $("#selectTiendas").val();
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
	if(!existeFecha(fechaini))
	{
		alert ('La fecha inicial no es correcta');
		return;
	}
	if(!existeFecha(fechafin))
	{
		alert ('La fecha final no es correcta');
		return;
	}
	if(!validarFechaMenorActual(fechaini, fechafin))
	{
		alert ('La fecha inicial es mayor a la fecha final, favor corregir');
		return;
	}
	if(!validarDiferenciaFechas(fechaini, fechafin))
	{
		alert ('La diferencia entre la fecha Inicial y Final no puede ser mayor a 3 días');
		return;
	}
	if (tienda == '' || tienda == null)
	{
		tienda = 'TODAS';
	}
	
	limpiarDetallePedido();


	if ($.fn.dataTable.isDataTable('#grid-encabezadopedido')) {
    		table = $('#grid-encabezadopedido').DataTable();
    }
    
	// Se ajusta la llamada enviando vacíos o valores predeterminados para los filtros removidos
	$.getJSON(server + 'ConsultaIntegradaPedidosRAPPICARGO?fechainicial=' + fechaini + "&fechafinal=" + fechafin + "&tienda=" + tienda + "&estadotienda=" + estadoTienda, function(data1){
		table.clear().draw();
		// Validamos si la respuesta está vacía o no trae registros
		if (!data1 || data1.length === 0) {
			$.alert({
				title: 'Sin Registros',
				content: 'No se encontraron pedidos para los filtros seleccionados.',
				type: 'orange',
				typeAnimated: true
			});
			return;
		}
		for(var i = 0; i < data1.length; i++){
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
								    				url: server + 'FinalizarPedidoReenvio?idpedido=' + idPedido + "&idformapago=" + idformapago + "&valortotal=" + totalpedido + "&valorformapago=" + valorformapago + "&idcliente=" + idCliente + "&insertado=" + insertado + "&tiempopedido=" + tiempopedido , 
								    				dataType: 'json', 
								    				async: false, 
								    				success: function(data){ 

															resultado = data[0];

															//El central puede negarse a mandar el pedido: o ya esta en la tienda,
															//o alguien mas lo esta mandando en este mismo momento. Ver
															//EnvioTiendaDAO. Sin esto la pantalla seguiria derecho y la persona
															//no sabria por que no paso nada.
															if (resultado && resultado.bloqueado)
															{
																alert(resultado.mensaje);
																return;
															}
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


// Agregar despues de inicializar tableAplicabilidadRappi en consultacargo.js.

$('#grid-aplicabilidad-rappicargo').on('click', 'tr', function () {

    var datospedido = tableAplicabilidadRappi.row(this).data();

    if (!datospedido) {
        return;
    }

    // Validar pedidos con Pago Virtual sin confirmar
	const esPagoVirtual =
	    Number(datospedido.idformapago) === 4 ||
	    (
	        datospedido.formapago &&
	        datospedido.formapago.trim().toLowerCase() === "pago virtual"
	    );

	const pagoConfirmado = !!(
	    datospedido.fechapagovirtual &&
	    datospedido.fechapagovirtual.toString().trim() !== ""
	);

    if (esPagoVirtual && !pagoConfirmado) {

        Swal.fire({
            icon: 'info',
            title: 'Pago pendiente de confirmación',
            html: `
                <div style="text-align:left">
                    Este pedido <b>sí aplica para Rappi Cargo</b>, pero su forma de pago es
                    <b>Pago Virtual</b> y aún no se ha registrado la confirmación del pago.
                    <br><br>
                    <b>No es posible crear la orden en Rappi Cargo hasta que el pago sea confirmado.</b>
                </div>
            `,
            confirmButtonText: 'Entendido',
            confirmButtonColor: '#2563eb'
        });

        return;
    }

    var advertencia = "";

    var titulo = datospedido.aplicaRappiCargoDist
        ? '¿Crear orden en Rappi Cargo?'
        : 'Pedido con advertencia';

    if (!datospedido.aplicaRappiCargoDist) {

        advertencia = `
            <div class="advertencia">

                <i class="fa fa-exclamation-triangle"></i>
                <b> Advertencia</b>

                <br><br>

                ${datospedido.motivoNoAplicaRappiCargo}

                <br><br>

                <b>Si decide continuar, el sistema intentará crear la orden en Rappi Cargo aun cuando el pedido no cumple con la validación de distancia.</b>

            </div>
        `;
    }

    Swal.fire({
        icon: datospedido.aplicaRappiCargoDist ? 'question' : 'warning',
        title: titulo,
        html: `
            <div class="rappi-info">
                <div><b>Pedido:</b> ${datospedido.idpedido}</div>
                <div><b>Cliente:</b> ${datospedido.cliente}</div>
                <div><b>Tienda:</b> ${datospedido.tienda}</div>
                <div><b>Dirección:</b> ${datospedido.direccion}</div>
                <div><b>Total:</b> $${Number(datospedido.totalneto).toLocaleString('es-CO')}</div>
                <div><b>Forma pago:</b> ${datospedido.formapago}</div>
            </div>

            ${advertencia}
        `,
        showCancelButton: true,
        confirmButtonText: datospedido.aplicaRappiCargoDist
            ? 'Crear orden'
            : 'Crear de todas formas',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#2563eb',
        cancelButtonColor: '#6b7280',
        reverseButtons: true,
        customClass: {
            popup: 'rappi-popup',
            icon: 'rappi-icon',
            title: 'rappi-title',
            actions: 'rappi-actions'
        }

    }).then((result) => {
        if (result.isConfirmed) {
            crearOrdenRappiCargo(datospedido.idpedido);
        }
    });

});


function crearOrdenRappiCargo(idpedido) {
	
	Swal.fire({
	    title: 'Creando orden...',
	    html: 'Validando dirección y coordenadas del cliente, esto puede tardar unos segundos.',
	    allowOutsideClick: false,
	    didOpen: () => {
	        Swal.showLoading();
	    }
	});
	
    $.ajax({

        url: server + 'CrearOrdenRappiCargo',
        type: 'POST',
        dataType: 'json',

        data: {
            idpedido: idpedido
        },


        success: function (data) {

            console.log("Respuesta servidor:", data);


            var respuesta = Array.isArray(data)
                    ? data[0]
                    : data;



            if (!respuesta) {

                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'El servidor no devolvió información.'
                });

                return;
            }



            if (respuesta.resultado === true) {


                Swal.fire({

                    icon: 'success',

                    title: 'Orden creada',

                    text: respuesta.mensaje,

                    timer: 2500,

                    showConfirmButton: false,

                    customClass: {
                        popup: 'mi-swal',
                        title: 'mi-swal-title',
                        icon: 'mi-swal-icon'
                    }

                });



                console.log(
                    "Cargo Order ID:",
                    respuesta.cargo_order_id
                );


                consultarPedido();

                consultarAplicabilidadRappiCargo();



            } else {


                Swal.fire({

                    icon: 'warning',

                    title: 'No fue posible crear la orden',

                    text: respuesta.mensaje,

                    customClass: {
                        popup: 'mi-swal',
                        title: 'mi-swal-title',
                        icon: 'mi-swal-icon'
                    }

                });

            }

        },


        error: function (xhr) {


            console.error(
                "Error AJAX:",
                xhr.responseText
            );


            Swal.fire({

                icon: 'error',

                title: 'Error de comunicación',

                text: 'Error HTTP ' + xhr.status,

                customClass: {
                    popup: 'mi-swal',
                    title: 'mi-swal-title',
                    icon: 'mi-swal-icon'
                }

            });

        }

    });
	
}


$('#grid-enviados-tienda-rappicargo').off('click').on('click', 'tr', function () {

    var datospedido = tableEnviadosTiendaRappiCargo.row(this).data();

    if (!datospedido) {
        return;
    }

    var tiendaSeleccionada = $('#ListTiendas option:selected').attr('id') || $('#ListTiendas').val() || 0;
    if (!datospedido.idtienda || datospedido.idtienda == 0) {
        datospedido.idtienda = parseInt(tiendaSeleccionada) || 0;
    }

    if (esPedidoTercerizado(datospedido)) {
        confirmarNotificarPedidoListoRappiCargo(datospedido);
        return;
    }

    consultaAplicabilidadCargoTienda(datospedido);
});



function consultaAplicabilidadCargoTienda(datospedido) {

    $.ajax({
        url: server + 'ConsultarAplicabilidadDeUnPedidoRAPPICARGO',
        type: 'POST',
        dataType: 'json',
        data: {
            numposheader: datospedido.idpedidotienda,
            idtienda: datospedido.idtienda
        },
        success: function (validacion) {

            if (!validacion || validacion.resultado !== true) {
                Swal.fire({
                    icon: 'warning',
                    title: 'No se puede crear la orden',
                    html: `
                        <div style="text-align:left">
                            Este pedido aparece como <b>posible candidato</b> porque en la tienda cumple condiciones básicas.
                            <br><br>
                            Pero el servidor principal indicó que <b>no se puede crear la orden en Rappi Cargo</b>.
                            <br><br>
                            <b>Pedido tienda:</b> ${datospedido.idpedidotienda}
                            <br>
                            <b>Cliente:</b> ${datospedido.nombreCompleto}
                            <br>
                            <b>Motivo:</b> ${validacion && validacion.mensaje ? validacion.mensaje : 'Sin detalle.'}
                        </div>
                    `,
                    confirmButtonText: 'Entendido',
                    confirmButtonColor: '#2563eb'
                });
                return;
            }

            verificarUltimoEstadoYContinuar(datospedido, validacion);
        },
        error: function (xhr) {
            Swal.fire({
                icon: 'error',
                title: 'Error validando aplicabilidad',
                text: 'No fue posible validar el pedido contra el servidor principal. HTTP ' + xhr.status,
                confirmButtonColor: '#2563eb'
            });
        }
    });
}

var mapaVerificacionCargo = null;
var markerTiendaCargo = null;
var markerClienteCargo = null;
var lineaRutaCargo = null;
var capaCalles = null;
var capaSatelite = null;
var capaActualTipo = 'calles';
var timerGeocodificacionInversa = null;
var pedidoCargoActual = null;
var validacionCargoActual = null;
var estadoAnteriorCargoActual = null;
var vieneSinCoordenadas = false;
var coordenadasModificadas = false;
var direccionOriginalCargo = '';
var ultimaDireccionDetectadaCargo = '';

function calcularDistanciaKm(lat1, lon1, lat2, lon2) {
    if (!lat1 || !lon1 || !lat2 || !lon2) return 0;
    var R = 6371; // Radio de la Tierra en km
    var dLat = (lat2 - lat1) * Math.PI / 180;
    var dLon = (lon2 - lon1) * Math.PI / 180;
    var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return parseFloat((R * c).toFixed(2));
}

function confirmarCrearOrdenCandidatoCargo(datospedido, validacion, estadoAnterior) {
    abrirModalVerificarCoordenadasRappiCargo(datospedido, validacion, estadoAnterior);
}

function abrirModalVerificarCoordenadasRappiCargo(datospedido, validacion, estadoAnterior) {
    pedidoCargoActual = datospedido;
    validacionCargoActual = validacion || {};
    estadoAnteriorCargoActual = estadoAnterior;
    coordenadasModificadas = false;

    var latTienda = parseFloat(validacion.latitudTienda) || 0;
    var lngTienda = parseFloat(validacion.longitudTienda) || 0;
    var latCliente = parseFloat(validacion.latitudCliente) || 0;
    var lngCliente = parseFloat(validacion.longitudCliente) || 0;

    if ((latCliente === 0 || isNaN(latCliente)) && datospedido.latitud) {
        latCliente = parseFloat(datospedido.latitud) || 0;
    }
    if ((lngCliente === 0 || isNaN(lngCliente)) && datospedido.longitud) {
        lngCliente = parseFloat(datospedido.longitud) || 0;
    }

    vieneSinCoordenadas = (!latCliente || !lngCliente || (latCliente === 0 && lngCliente === 0) || isNaN(latCliente) || isNaN(lngCliente));

    // Llenar etiquetas de información
    $('#lblCoordPedido').text(datospedido.idpedidotienda || '--');
    $('#lblCoordCliente').text(datospedido.nombreCompleto || 'Cliente');
    $('#lblCoordDireccion').text(datospedido.direccion || validacion.direccionCliente || 'Sin dirección');
    $('#lblCoordTienda').text(validacion.nombreTienda || ('Tienda ' + datospedido.idtienda));
    $('#lblCoordDireccionDetectada').html('<i class="fas fa-spinner fa-spin text-muted"></i> Consultando ubicación en mapa...');

    // Inicializar campo editable de dirección de entrega
    direccionOriginalCargo = (datospedido.direccion || validacion.direccionCliente || '').trim();
    ultimaDireccionDetectadaCargo = '';
    $('#btnCopiarDireccionDetectada').hide();
    $('#txtDireccionEntregaCargo').val(direccionOriginalCargo);
    $('#msgCambioDireccion').hide();

    // Eventos de la dirección editable
    $('#txtDireccionEntregaCargo').off('input').on('input', function () {
        var textoActual = ($(this).val() || '').trim();
        if (textoActual.toLowerCase() !== direccionOriginalCargo.toLowerCase()) {
            $('#msgCambioDireccion').slideDown(150);
        } else {
            $('#msgCambioDireccion').slideUp(150);
        }
    });

    $('#btnRestaurarDireccionOriginal').off('click').on('click', function () {
        $('#txtDireccionEntregaCargo').val(direccionOriginalCargo).trigger('input');
    });

    $('#btnCopiarDireccionDetectada').off('click').on('click', function () {
        if (ultimaDireccionDetectadaCargo) {
            $('#txtDireccionEntregaCargo').val(ultimaDireccionDetectadaCargo).trigger('input');
            Swal.fire({
                icon: 'info',
                title: 'Dirección copiada',
                text: 'Se copió la dirección detectada al campo de entrega editable.',
                timer: 1500,
                showConfirmButton: false
            });
        }
    });

    $('#btnUbicarDireccionEntrega').off('click').on('click', function () {
        var dirEntrega = ($('#txtDireccionEntregaCargo').val() || '').trim();
        if (dirEntrega) {
            $('#txtBuscarDireccionMapa').val(dirEntrega);
            buscarDireccionEnMapa();
        }
    });

    // Llenar buscador con la dirección registrada para fácil búsqueda
    var dirBusqueda = (datospedido.direccion || validacion.direccionCliente || '').trim();
    if (dirBusqueda && !dirBusqueda.toLowerCase().includes('medellin') && !dirBusqueda.toLowerCase().includes('bello')) {
        dirBusqueda += ', Antioquia';
    }
    $('#txtBuscarDireccionMapa').val(dirBusqueda);

    // Construir banner de alerta dinámico
    var alertasHtml = '';

    if (vieneSinCoordenadas) {
        alertasHtml += `
            <div class="alert-coord-box alert-coord-warning">
                <i class="fas fa-exclamation-triangle"></i>
                <div>
                    <b>⚠️ Este pedido NO tiene coordenadas registradas en el sistema.</b><br>
                    El mapa se centró provisionalmente en la tienda. <b>Debe arrastrar el marcador rojo (📍)</b> hasta la dirección de entrega del cliente para poder continuar.
                </div>
            </div>
        `;
    } else {
        alertasHtml += `
            <div class="alert-coord-box alert-coord-ok">
                <i class="fas fa-check-circle"></i>
                <div>
                    <b>Coordenadas existentes registradas.</b><br>
                    Revise que el marcador rojo coincida con la dirección <b>${datospedido.direccion || ''}</b>. Compare la <b>Dirección Registrada</b> con la <b>Dirección Detectada en Mapa</b> para confirmar exactitud.
                </div>
            </div>
        `;
    }

    if (estadoAnterior === 'canceled') {
        alertasHtml += `
            <div class="alert-coord-box alert-coord-danger">
                <i class="fas fa-ban"></i>
                <div>
                    <b>Este pedido ya había sido creado en Rappi Cargo y esa orden fue CANCELADA.</b><br>
                    Si continúa, se intentará generar una nueva orden en Rappi Cargo.
                </div>
            </div>
        `;
    }

    if (validacion && validacion.validacionDistancia === false) {
        alertasHtml += `
            <div class="alert-coord-box alert-coord-warning">
                <i class="fas fa-exclamation-circle"></i>
                <div>
                    <b>Advertencia de distancia:</b> ${validacion.mensaje || 'La distancia del pedido supera el rango habitual.'}
                </div>
            </div>
        `;
    }

    $('#alertaCoordenadasModal').html(alertasHtml);

    // Valores iniciales de coordenadas y distancia
    var distanciaInicial = validacion && validacion.distanciaKm ? validacion.distanciaKm : 0;

    if (vieneSinCoordenadas) {
        // Posicionar provisionalmente cerca de la tienda
        latCliente = (latTienda !== 0) ? (latTienda + 0.0015) : 6.2442;
        lngCliente = (lngTienda !== 0) ? (lngTienda + 0.0015) : -75.5812;
        distanciaInicial = 0;
        $('#lblCoordLatLng').text('Sin registrar');
        $('#lblCoordDistancia').text('0.0 km');
        $('#btnConfirmarCrearOrdenCargo').prop('disabled', true).html('<i class="fas fa-hand-pointer"></i> Arrastre el pin para habilitar');
    } else {
        $('#lblCoordLatLng').text(latCliente.toFixed(6) + ', ' + lngCliente.toFixed(6));
        $('#lblCoordDistancia').text(distanciaInicial + ' km');
        $('#btnConfirmarCrearOrdenCargo').prop('disabled', false).html('<i class="fas fa-motorcycle"></i> Confirmar y Crear Orden');
    }

    // Actualizar enlace a Google Maps
    $('#btnAbrirGoogleMaps').attr('href', 'https://www.google.com/maps/search/?api=1&query=' + latCliente + ',' + lngCliente);

    // Configurar eventos del toolbar
    $('#btnBuscarEnMapa').off('click').on('click', function () {
        buscarDireccionEnMapa();
    });

    $('#txtBuscarDireccionMapa').off('keypress').on('keypress', function (e) {
        if (e.which === 13) {
            e.preventDefault();
            buscarDireccionEnMapa();
        }
    });

    $('#btnCapaCalles').off('click').on('click', function () {
        cambiarCapaMapa('calles');
    });

    $('#btnCapaSatelite').off('click').on('click', function () {
        cambiarCapaMapa('satelite');
    });

    // Configurar evento de confirmación con los 3 escenarios inteligentes
    $('#btnConfirmarCrearOrdenCargo').off('click').on('click', function () {
        if (vieneSinCoordenadas && !coordenadasModificadas) {
            Swal.fire({
                icon: 'warning',
                title: 'Ubicación requerida',
                text: 'Este pedido no tiene coordenadas registradas. Debe arrastrar el marcador rojo hasta la dirección de entrega del cliente antes de continuar.',
                confirmButtonColor: '#2563eb'
            });
            return;
        }

        if (!markerClienteCargo) {
            return;
        }

        var posFinal = markerClienteCargo.getLatLng();
        var direccionFinal = ($('#txtDireccionEntregaCargo').val() || '').trim();

        if (!direccionFinal) {
            Swal.fire({
                icon: 'warning',
                title: 'Dirección requerida',
                text: 'La dirección de entrega no puede estar vacía.',
                confirmButtonColor: '#2563eb'
            });
            return;
        }

        var direccionCambiada = (direccionFinal.toLowerCase() !== direccionOriginalCargo.toLowerCase());
        var coordenadasCambiadas = (coordenadasModificadas || vieneSinCoordenadas);

        // Escenario 3: La dirección fue modificada por el operador
        if (direccionCambiada) {
            Swal.fire({
                icon: 'warning',
                title: '¿Registrar cambio de dirección?',
                html: `
                    <div style="text-align:left; font-size:13px; line-height:1.6;">
                        <p style="margin-bottom:8px; color:#475569;">Se detectó una modificación en la dirección de entrega del cliente:</p>
                        <div style="background:#fee2e2; border-left:4px solid #ef4444; padding:8px 12px; margin-bottom:8px; border-radius:4px;">
                            <span style="font-size:10.5px; color:#991b1b; font-weight:700; text-transform:uppercase;">Dirección anterior:</span><br>
                            <b style="color:#b91c1c;">${direccionOriginalCargo || 'Sin registrar'}</b>
                        </div>
                        <div style="background:#dcfce7; border-left:4px solid #22c55e; padding:8px 12px; margin-bottom:10px; border-radius:4px;">
                            <span style="font-size:10.5px; color:#166534; font-weight:700; text-transform:uppercase;">Nueva dirección a registrar:</span><br>
                            <b style="color:#15803d;">${direccionFinal}</b>
                        </div>
                        <div style="background:#f1f5f9; padding:7px 12px; border-radius:4px; font-size:11.5px; color:#334155; margin-bottom:10px;">
                            <i class="fas fa-map-marker-alt text-danger"></i> <b>Coordenadas:</b> ${posFinal.lat.toFixed(6)}, ${posFinal.lng.toFixed(6)}
                        </div>
                        <p style="margin:0; font-weight:600; color:#1e293b;">¿Desea registrar esta nueva dirección y sus coordenadas en el sistema (Call Center y Tienda) y crear la orden en Rappi Cargo?</p>
                    </div>
                `,
                showCancelButton: true,
                confirmButtonColor: '#16a34a',
                cancelButtonColor: '#64748b',
                confirmButtonText: '<i class="fas fa-check"></i> Sí, registrar y crear orden',
                cancelButtonText: '<i class="fas fa-arrow-left"></i> Cancelar y revisar',
                reverseButtons: true
            }).then((result) => {
                if (result.isConfirmed) {
                    $('#modalVerificarCoordenadasRappiCargo').modal('hide');
                    crearOrdenRappiCargoCandidatoTienda(pedidoCargoActual, posFinal.lat, posFinal.lng, direccionFinal);
                }
            });
            return;
        }

        // Escenario 2: Solo se modificaron las coordenadas (pin arrastrado o búsqueda)
        if (coordenadasCambiadas) {
            Swal.fire({
                icon: 'question',
                title: '¿Confirmar ubicación en el mapa?',
                html: `
                    <div style="text-align:left; font-size:13px; line-height:1.6;">
                        <p style="margin-bottom:8px; color:#475569;">¿Confirma que el punto marcado en el mapa es la ubicación exacta de entrega?</p>
                        <div style="background:#f8fafc; border:1px solid #e2e8f0; padding:9px 12px; border-radius:6px; margin-bottom:10px;">
                            <div style="margin-bottom:5px;">
                                <i class="fas fa-home text-primary"></i> <b>Dirección:</b> ${direccionFinal}
                            </div>
                            <div style="margin-bottom:5px;">
                                <i class="fas fa-map-marker-alt text-danger"></i> <b>Coordenadas:</b> ${posFinal.lat.toFixed(6)}, ${posFinal.lng.toFixed(6)}
                            </div>
                            <div>
                                <i class="fas fa-route text-success"></i> <b>Distancia calculada:</b> ${$('#lblCoordDistancia').text()}
                            </div>
                        </div>
                        <p style="margin:0; font-size:11.5px; color:#64748b;">Se guardarán estas coordenadas en el sistema (Call Center y Tienda) y se despachará la orden con Rappi Cargo.</p>
                    </div>
                `,
                showCancelButton: true,
                confirmButtonColor: '#2563eb',
                cancelButtonColor: '#64748b',
                confirmButtonText: '<i class="fas fa-motorcycle"></i> Sí, confirmar y crear orden',
                cancelButtonText: '<i class="fas fa-map"></i> Revisar en el mapa',
                reverseButtons: true
            }).then((result) => {
                if (result.isConfirmed) {
                    $('#modalVerificarCoordenadasRappiCargo').modal('hide');
                    crearOrdenRappiCargoCandidatoTienda(pedidoCargoActual, posFinal.lat, posFinal.lng, direccionFinal);
                }
            });
            return;
        }

        // Escenario 1: Sin cambios en dirección ni en coordenadas
        $('#modalVerificarCoordenadasRappiCargo').modal('hide');
        crearOrdenRappiCargoCandidatoTienda(pedidoCargoActual, posFinal.lat, posFinal.lng, direccionFinal);
    });

    // Abrir modal y renderizar mapa
    $('#modalVerificarCoordenadasRappiCargo').modal('show');

    $('#modalVerificarCoordenadasRappiCargo').off('shown.bs.modal').on('shown.bs.modal', function () {
        renderizarMapaCoordenadasRappiCargo(latTienda, lngTienda, latCliente, lngCliente);
    });
}

function renderizarMapaCoordenadasRappiCargo(latTienda, lngTienda, latCliente, lngCliente) {
    var contenedor = $('#mapaVerificacionCargo');
    if (!contenedor.length) return;

    if (!mapaVerificacionCargo) {
        mapaVerificacionCargo = L.map('mapaVerificacionCargo');

        capaCalles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(mapaVerificacionCargo);
    }

    // Resetear a capa seleccionada
    cambiarCapaMapa(capaActualTipo);
    mapaVerificacionCargo.invalidateSize();

    // Limpiar capas previas
    if (markerTiendaCargo) mapaVerificacionCargo.removeLayer(markerTiendaCargo);
    if (markerClienteCargo) mapaVerificacionCargo.removeLayer(markerClienteCargo);
    if (lineaRutaCargo) mapaVerificacionCargo.removeLayer(lineaRutaCargo);

    // Iconos personalizados con FontAwesome
    var iconTienda = L.divIcon({
        className: 'custom-leaflet-store-marker',
        html: '<div style="background-color:#2563eb;color:#fff;width:34px;height:34px;border-radius:50%;display:flex;align-items:center;justify-content:center;box-shadow:0 3px 8px rgba(0,0,0,0.35);border:2px solid #fff;font-size:15px;"><i class="fas fa-store"></i></div>',
        iconSize: [34, 34],
        iconAnchor: [17, 17],
        popupAnchor: [0, -18]
    });

    var iconCliente = L.divIcon({
        className: 'custom-leaflet-client-marker',
        html: '<div style="background-color:#dc2626;color:#fff;width:36px;height:36px;border-radius:50%;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 10px rgba(220,38,38,0.5);border:3px solid #fff;font-size:17px;"><i class="fas fa-map-marker-alt"></i></div>',
        iconSize: [36, 36],
        iconAnchor: [18, 18],
        popupAnchor: [0, -20]
    });

    // Marcador de tienda (fijo)
    if (latTienda !== 0 && lngTienda !== 0) {
        markerTiendaCargo = L.marker([latTienda, lngTienda], { icon: iconTienda })
            .addTo(mapaVerificacionCargo)
            .bindPopup('<b>Tienda:</b> ' + ($('#lblCoordTienda').text() || 'Punto de despacho'));
    }

    // Marcador de cliente (arrastrable)
    markerClienteCargo = L.marker([latCliente, lngCliente], {
        icon: iconCliente,
        draggable: true
    }).addTo(mapaVerificacionCargo);

    markerClienteCargo.bindPopup(
        '<b>Punto de Entrega</b><br>' +
        '<b>Dirección:</b> ' + ($('#lblCoordDireccion').text() || '')
    ).openPopup();

    // Línea de referencia tienda-cliente
    if (latTienda !== 0 && lngTienda !== 0) {
        lineaRutaCargo = L.polyline([[latTienda, lngTienda], [latCliente, lngCliente]], {
            color: '#2563eb',
            weight: 3,
            dashArray: '6, 8',
            opacity: 0.75
        }).addTo(mapaVerificacionCargo);
    }

    // Ajustar vista del mapa
    if (latTienda !== 0 && lngTienda !== 0 && (latTienda !== latCliente || lngTienda !== lngCliente)) {
        var bounds = L.latLngBounds([[latTienda, lngTienda], [latCliente, lngCliente]]);
        mapaVerificacionCargo.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
    } else {
        mapaVerificacionCargo.setView([latCliente, lngCliente], 16);
    }

    // Consultar geocodificación inversa inicial
    consultarDireccionInversa(latCliente, lngCliente);

    // Eventos de arrastre del marcador cliente
    markerClienteCargo.on('drag', function (e) {
        var pos = e.target.getLatLng();
        actualizarDatosPosicionCliente(pos.lat, pos.lng, latTienda, lngTienda);
    });

    markerClienteCargo.on('dragend', function (e) {
        var pos = e.target.getLatLng();
        coordenadasModificadas = true;
        actualizarDatosPosicionCliente(pos.lat, pos.lng, latTienda, lngTienda);

        $('#btnConfirmarCrearOrdenCargo')
            .prop('disabled', false)
            .html('<i class="fas fa-motorcycle"></i> Confirmar y Crear Orden');

        // Consultar dirección real en el punto soltado
        consultarDireccionInversa(pos.lat, pos.lng);

        if (vieneSinCoordenadas) {
            $('#alertaCoordenadasModal').html(`
                <div class="alert-coord-box alert-coord-ok">
                    <i class="fas fa-check-circle"></i>
                    <div>
                        <b>¡Ubicación asignada!</b><br>
                        Verifique que la dirección detectada coincida con la del cliente. Si todo está correcto, pulse <b>Confirmar y Crear Orden</b>.
                    </div>
                </div>
            `);
        }
    });
}

function actualizarDatosPosicionCliente(lat, lng, latTienda, lngTienda) {
    $('#lblCoordLatLng').text(lat.toFixed(6) + ', ' + lng.toFixed(6));
    $('#btnAbrirGoogleMaps').attr('href', 'https://www.google.com/maps/search/?api=1&query=' + lat + ',' + lng);

    if (latTienda !== 0 && lngTienda !== 0) {
        var distancia = calcularDistanciaKm(latTienda, lngTienda, lat, lng);
        $('#lblCoordDistancia').text(distancia + ' km');

        if (lineaRutaCargo) {
            lineaRutaCargo.setLatLngs([[latTienda, lngTienda], [lat, lng]]);
        }
    }
}

function formatearDireccionColombianaArcGIS(addr) {
    if (!addr) return { texto: '', direccionBase: '', road: '', houseNumber: '', barrio: '', municipio: '' };

    var rawAddress = (addr.Address || addr.ShortLabel || '').trim();
    var matchAddr = (addr.Match_addr || '').trim();
    var addNum = (addr.AddNum || '').trim();
    var barrio = (addr.Neighborhood || addr.District || addr.Subregion || '').trim();
    var municipio = (addr.City || 'Medellín').trim();
    var road = '';
    var houseNumber = '';
    var direccionFormateada = '';

    if (addr.Addr_type === 'POI' && (addr.PlaceName || addr.ShortLabel)) {
        direccionFormateada = (addr.PlaceName || addr.ShortLabel).trim();
        road = direccionFormateada;
    } else if (rawAddress) {
        if (rawAddress.includes('#') || rawAddress.includes('-')) {
            direccionFormateada = rawAddress;
        } else if (addNum && rawAddress.endsWith(addNum)) {
            var resto = rawAddress.substring(0, rawAddress.length - addNum.length).trim();
            var ultEspacio = resto.lastIndexOf(' ');
            if (ultEspacio > 0) {
                road = resto.substring(0, ultEspacio).trim();
                var cruce = resto.substring(ultEspacio + 1).trim();
                houseNumber = cruce + '-' + addNum;
                direccionFormateada = road + ' # ' + cruce + '-' + addNum;
            } else {
                road = resto;
                houseNumber = addNum;
                direccionFormateada = road + ' # ' + addNum;
            }
        } else {
            var matchNomen = rawAddress.match(/^((?:calle|carrera|cra|cr|cl|avenida|av|diagonal|diag|dg|transversal|trans|tv|circular|autopista)\s*\d+[a-z]?)\s+(\d+[a-z]?)\s+(\d+[a-z]?)$/i);
            if (matchNomen) {
                road = matchNomen[1];
                houseNumber = matchNomen[2] + '-' + matchNomen[3];
                direccionFormateada = road + ' # ' + houseNumber;
            } else {
                direccionFormateada = rawAddress;
                road = rawAddress;
            }
        }
    } else if (matchAddr) {
        direccionFormateada = matchAddr.split(',')[0].trim();
        road = direccionFormateada;
    }

    var textoCompleto = direccionFormateada;
    if (barrio && textoCompleto.indexOf(barrio) === -1) {
        textoCompleto += ', ' + barrio;
    }
    if (municipio && textoCompleto.indexOf(municipio) === -1) {
        textoCompleto += ', ' + municipio;
    }

    return {
        texto: textoCompleto,
        direccionBase: direccionFormateada,
        road: road,
        houseNumber: houseNumber,
        barrio: barrio,
        municipio: municipio
    };
}

function consultarDireccionInversa(lat, lng) {
    if (!lat || !lng || (lat === 0 && lng === 0)) return;

    $('#lblCoordDireccionDetectada').html('<i class="fas fa-spinner fa-spin text-muted"></i> Identificando dirección con ArcGIS...');

    if (timerGeocodificacionInversa) {
        clearTimeout(timerGeocodificacionInversa);
    }

    timerGeocodificacionInversa = setTimeout(function () {
        var urlArcGIS = 'https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode';
        $.ajax({
            url: urlArcGIS,
            type: 'GET',
            dataType: 'json',
            data: {
                location: lng + ',' + lat,
                f: 'json',
                token: ARCGIS_API_KEY
            },
            success: function (data) {
                if (data && data.address) {
                    var info = formatearDireccionColombianaArcGIS(data.address);
                    var direccionFinal = info.texto || data.address.Match_addr || '';

                    ultimaDireccionDetectadaCargo = direccionFinal;

                    $('#lblCoordDireccionDetectada').html(
                        '<i class="fas fa-check text-success"></i> <b>' + (direccionFinal || 'Vía identificada') + '</b>'
                    );

                    if (ultimaDireccionDetectadaCargo) {
                        $('#btnCopiarDireccionDetectada').show();
                    } else {
                        $('#btnCopiarDireccionDetectada').hide();
                    }

                    if (markerClienteCargo) {
                        markerClienteCargo.setPopupContent(
                            '<b>Punto de Entrega</b><br>' +
                            '<b>Registrada:</b> ' + ($('#lblCoordDireccion').text() || '') + '<br>' +
                            '<span style="color:#059669;"><b>En mapa:</b> ' + (direccionFinal || '') + '</span>'
                        );
                    }
                } else {
                    ultimaDireccionDetectadaCargo = '';
                    $('#btnCopiarDireccionDetectada').hide();
                    $('#lblCoordDireccionDetectada').html('<span class="text-muted">No fue posible identificar nombre de vía</span>');
                }
            },
            error: function () {
                $('#lblCoordDireccionDetectada').html('<span class="text-muted">Servicio de dirección no disponible</span>');
            }
        });
    }, 350);
}

function buscarDireccionEnMapa() {
    var query = ($('#txtBuscarDireccionMapa').val() || '').trim();
    if (!query || query.length < 3) {
        Swal.fire({
            icon: 'info',
            title: 'Dirección requerida',
            text: 'Escriba una dirección o barrio para ubicar en el mapa.',
            confirmButtonColor: '#2563eb'
        });
        return;
    }

    var municipio = '';
    if (pedidoCargoActual && pedidoCargoActual.municipio) {
        municipio = pedidoCargoActual.municipio;
    } else if (pedidoCargoActual && pedidoCargoActual.ciudad) {
        municipio = pedidoCargoActual.ciudad;
    } else if (validacionCargoActual && validacionCargoActual.municipio) {
        municipio = validacionCargoActual.municipio;
    }

    Swal.fire({
        title: 'Buscando con Ubicación Ctrl...',
        text: query,
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    var urlServicio = (server || '') + 'GeocodificarDireccion';

    $.ajax({
        url: urlServicio,
        type: 'GET',
        dataType: 'json',
        data: {
            direccion: query,
            municipio: municipio
        },
        success: function (data) {
            if (data && data.latitud && data.longitud && !data.error) {
                var newLat = parseFloat(data.latitud);
                var newLng = parseFloat(data.longitud);

                if (newLat !== 0 || newLng !== 0) {
                    Swal.close();

                    if (markerClienteCargo) {
                        markerClienteCargo.setLatLng([newLat, newLng]);
                    }
                    if (mapaVerificacionCargo) {
                        mapaVerificacionCargo.setView([newLat, newLng], 17);
                    }

                    var latTienda = parseFloat(validacionCargoActual.latitudTienda) || 0;
                    var lngTienda = parseFloat(validacionCargoActual.longitudTienda) || 0;

                    coordenadasModificadas = true;
                    actualizarDatosPosicionCliente(newLat, newLng, latTienda, lngTienda);

                    // Asignar dirección detectada directamente desde UbicacionCtrl
                    var dirDetectada = data.direccion || query;
                    ultimaDireccionDetectadaCargo = dirDetectada;

                    $('#lblCoordDireccionDetectada').html(
                        '<i class="fas fa-check-circle text-success"></i> <b>' + dirDetectada + '</b>'
                    );
                    $('#btnCopiarDireccionDetectada').show();

                    if (markerClienteCargo) {
                        markerClienteCargo.setPopupContent(
                            '<b>Punto de Entrega</b><br>' +
                            '<b>Registrada:</b> ' + ($('#lblCoordDireccion').text() || '') + '<br>' +
                            '<span style="color:#059669;"><b>Ubicación Ctrl:</b> ' + dirDetectada + '</span>'
                        ).openPopup();
                    }

                    $('#btnAbrirGoogleMaps').attr('href', 'https://www.google.com/maps/search/?api=1&query=' + newLat + ',' + newLng);

                    $('#btnConfirmarCrearOrdenCargo')
                        .prop('disabled', false)
                        .html('<i class="fas fa-motorcycle"></i> Confirmar y Crear Orden');

                    Swal.fire({
                        icon: 'success',
                        title: 'Dirección ubicada con exactitud',
                        text: dirDetectada,
                        timer: 2000,
                        showConfirmButton: false
                    });
                    return;
                }
            }

            // Fallback a ArcGIS si UbicacionCtrl no obtuvo coordenadas
            buscarEnMapaArcGISFallback(query);
        },
        error: function () {
            // Fallback a ArcGIS si hubo error de conexión con el servlet local
            buscarEnMapaArcGISFallback(query);
        }
    });
}

function buscarEnMapaArcGISFallback(query) {
    var consultaCompleta = query.trim();
    if (!consultaCompleta.toLowerCase().includes('colombia')) {
        consultaCompleta += ', Colombia';
    }

    var urlArcGIS = 'https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates';

    $.ajax({
        url: urlArcGIS,
        type: 'GET',
        dataType: 'json',
        data: {
            singleLine: consultaCompleta,
            f: 'json',
            countryCode: 'COL',
            maxLocations: 1,
            outFields: '*',
            token: ARCGIS_API_KEY
        },
        success: function (data) {
            Swal.close();

            if (data && data.candidates && data.candidates.length > 0) {
                var candidate = data.candidates[0];
                var newLat = parseFloat(candidate.location.y);
                var newLng = parseFloat(candidate.location.x);
                var nombreDir = candidate.address || (candidate.attributes ? candidate.attributes.Match_addr : query);

                if (markerClienteCargo) {
                    markerClienteCargo.setLatLng([newLat, newLng]);
                }
                if (mapaVerificacionCargo) {
                    mapaVerificacionCargo.setView([newLat, newLng], 17);
                }

                var latTienda = parseFloat(validacionCargoActual.latitudTienda) || 0;
                var lngTienda = parseFloat(validacionCargoActual.longitudTienda) || 0;

                coordenadasModificadas = true;
                actualizarDatosPosicionCliente(newLat, newLng, latTienda, lngTienda);
                consultarDireccionInversa(newLat, newLng);

                $('#btnConfirmarCrearOrdenCargo')
                    .prop('disabled', false)
                    .html('<i class="fas fa-motorcycle"></i> Confirmar y Crear Orden');

                Swal.fire({
                    icon: 'success',
                    title: 'Dirección ubicada con ArcGIS',
                    text: nombreDir,
                    timer: 2000,
                    showConfirmButton: false
                });
            } else {
                Swal.fire({
                    icon: 'warning',
                    title: 'No se encontró en el mapa',
                    text: 'No fue posible encontrar una coincidencia exacta para "' + query + '". Puede ajustar la búsqueda o arrastrar el marcador manualmente a la calle deseada.',
                    confirmButtonColor: '#2563eb'
                });
            }
        },
        error: function () {
            Swal.close();
            Swal.fire({
                icon: 'error',
                title: 'Error de búsqueda',
                text: 'No fue posible conectar con el servicio de mapas de ArcGIS.',
                confirmButtonColor: '#2563eb'
            });
        }
    });
}

function buscarEnMapaNominatimFallback(query) {
    return buscarEnMapaArcGISFallback(query);
}


function cambiarCapaMapa(tipo) {
    if (!mapaVerificacionCargo) return;

    if (tipo === 'satelite') {
        if (capaCalles && mapaVerificacionCargo.hasLayer(capaCalles)) {
            mapaVerificacionCargo.removeLayer(capaCalles);
        }
        if (!capaSatelite) {
            capaSatelite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                attribution: 'Tiles &copy; Esri',
                maxZoom: 19
            });
        }
        capaSatelite.addTo(mapaVerificacionCargo);
        $('#btnCapaSatelite').addClass('active');
        $('#btnCapaCalles').removeClass('active');
        capaActualTipo = 'satelite';
    } else {
        if (capaSatelite && mapaVerificacionCargo.hasLayer(capaSatelite)) {
            mapaVerificacionCargo.removeLayer(capaSatelite);
        }
        if (!capaCalles) {
            capaCalles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors',
                maxZoom: 19
            });
        }
        capaCalles.addTo(mapaVerificacionCargo);
        $('#btnCapaCalles').addClass('active');
        $('#btnCapaSatelite').removeClass('active');
        capaActualTipo = 'calles';
    }
}

function simularPedidoRappiCargoParaMapa() {
    Swal.fire({
        title: '🧪 Probar Mapa Rappi Cargo (Simulación)',
        html: `
            <div style="text-align:left; font-size:13px; line-height:1.5;">
                <p style="color:#475569;">Elija un escenario para probar la interacción con el mapa y la geocodificación ArcGIS sin guardar nada en la base de datos:</p>
                <div style="margin-bottom:8px;">
                    <b>1. Con coordenadas iniciales:</b> Carga un punto en Laureles. Podrá arrastrar el pin para ver cómo ArcGIS detecta la dirección exacta.
                </div>
                <div>
                    <b>2. Sin coordenadas iniciales:</b> El pin inicia en la tienda y debe arrastrarse al destino del cliente para habilitar el despacho.
                </div>
            </div>
        `,
        icon: 'info',
        showCancelButton: true,
        confirmButtonText: '<i class="fas fa-map-marked-alt"></i> 1. Laureles (Con coords)',
        cancelButtonText: 'Cancelar',
        showDenyButton: true,
        denyButtonText: '<i class="fas fa-hand-pointer"></i> 2. Poblado (Sin coords)',
        confirmButtonColor: '#2563eb',
        denyButtonColor: '#d97706',
        cancelButtonColor: '#64748b'
    }).then((result) => {
        if (result.isConfirmed) {
            iniciarSimulacionModalCargo({
                idpedidotienda: 'SIM-LAURELES-01',
                nombreCompleto: 'Juan Pérez (Cliente Simulado)',
                direccion: 'Circular 4 # 73-45, Laureles',
                idtienda: 1,
                telefono: '3001234567',
                latitud: 6.2443,
                longitud: -75.5925,
                tiendaLat: 6.2425,
                tiendaLng: -75.5901,
                nombreTienda: 'Tienda Laureles'
            });
        } else if (result.isDenied) {
            iniciarSimulacionModalCargo({
                idpedidotienda: 'SIM-POBLADO-02',
                nombreCompleto: 'María Gómez (Cliente Simulado)',
                direccion: 'Calle 10 # 40-20, El Poblado',
                idtienda: 2,
                telefono: '3109876543',
                latitud: 0,
                longitud: 0,
                tiendaLat: 6.2085,
                tiendaLng: -75.5680,
                nombreTienda: 'Tienda Poblado'
            });
        }
    });
}

function iniciarSimulacionModalCargo(datos) {
    var pedidoSimulado = {
        idpedidotienda: datos.idpedidotienda,
        nombreCompleto: datos.nombreCompleto,
        direccion: datos.direccion,
        idtienda: datos.idtienda,
        telefono: datos.telefono,
        latitud: datos.latitud,
        longitud: datos.longitud,
        esSimulacion: true
    };

    var validacionSimulada = {
        aplicable: true,
        distanciaKm: datos.latitud ? 1.35 : 0,
        validacionDistancia: true,
        latitudTienda: datos.tiendaLat,
        longitudTienda: datos.tiendaLng,
        latitudCliente: datos.latitud,
        longitudCliente: datos.longitud,
        direccionCliente: datos.direccion,
        nombreTienda: datos.nombreTienda
    };

    abrirModalVerificarCoordenadasRappiCargo(pedidoSimulado, validacionSimulada, null);
}

function crearOrdenRappiCargoCandidatoTienda(datospedido, latitud, longitud, direccion) {
    if (datospedido && datospedido.esSimulacion) {
        Swal.fire({
            icon: 'success',
            title: '¡Simulación Completada!',
            html: `
                <div style="text-align:left; font-size:13px; line-height:1.6;">
                    <p style="color:#15803d; font-weight:700; margin-bottom:8px;">✅ El mapa y ArcGIS funcionaron correctamente</p>
                    <div style="background:#f8fafc; border:1px solid #cbd5e1; padding:10px 12px; border-radius:6px; margin-bottom:10px;">
                        <div><i class="fas fa-map-marker-alt text-danger"></i> <b>Dirección confirmada:</b> ${direccion || datospedido.direccion}</div>
                        <div><i class="fas fa-location-arrow text-primary"></i> <b>Coordenadas:</b> ${latitud ? latitud.toFixed(6) : '0'}, ${longitud ? longitud.toFixed(6) : '0'}</div>
                        <div><i class="fas fa-route text-success"></i> <b>Distancia calculada:</b> ${$('#lblCoordDistancia').text()}</div>
                        <div><i class="fas fa-satellite text-info"></i> <b>Servicio:</b> ArcGIS World Geocoding Service</div>
                    </div>
                    <div style="background:#eff6ff; border-left:4px solid #3b82f6; padding:8px 12px; border-radius:4px; font-size:12px; color:#1e40af;">
                        <i class="fas fa-shield-alt"></i> <b>Seguridad garantizada:</b> Esta fue una simulación; NO se crearon pedidos en la base de datos ni se llamó a Rappi Cargo.
                    </div>
                </div>
            `,
            confirmButtonColor: '#2563eb'
        });
        return;
    }

    var payload = {
        numposheader: datospedido.idpedidotienda,
        idtienda: datospedido.idtienda
    };

    if (latitud && longitud) {
        payload.latitud = latitud;
        payload.longitud = longitud;
    }

    if (direccion) {
        payload.direccion = direccion;
    }

    Swal.fire({
        title: 'Creando orden en Rappi Cargo...',
        text: 'Por favor espere mientras se envía la información.',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    $.ajax({
        url: server + 'CrearOrdenRappiCargo',
        type: 'POST',
        dataType: 'json',
        data: payload,
        success: function (data) {
            Swal.close();

            var respuesta = Array.isArray(data) ? data[0] : data;

            if (!respuesta) {
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'El servidor no devolvió información.',
                    confirmButtonColor: '#2563eb'
                });
                return;
            }

            if (respuesta.resultado === true) {
                if (respuesta.marcadoTienda === true) {
                    Swal.fire({
                        icon: 'success',
                        title: 'Orden creada',
                        text: 'La orden fue creada en Rappi Cargo y el pedido quedó marcado como tercerizado en tienda.',
                        timer: 2500,
                        showConfirmButton: false
                    });
                } else if (respuesta.marcadoTienda === false && respuesta.mensajeMarcacionTienda) {
                    Swal.fire({
                        icon: 'warning',
                        title: 'Orden creada, pero falta marcar en tienda',
                        text: respuesta.mensajeMarcacionTienda,
                        confirmButtonColor: '#2563eb'
                    });
                } else {
                    Swal.fire({
                        icon: 'success',
                        title: 'Orden creada',
                        text: respuesta.mensaje || 'La orden fue creada correctamente.',
                        timer: 2500,
                        showConfirmButton: false
                    });
                }

                consultarCandidatosRappiCargoTienda();
            } else {
                Swal.fire({
                    icon: 'warning',
                    title: 'Problemas en la creación de la orden',
                    text: respuesta.mensaje || 'No se pudo crear la orden en Rappi Cargo.',
                    confirmButtonColor: '#2563eb'
                });
            }
        },
        error: function (xhr) {
            Swal.close();
            Swal.fire({
                icon: 'error',
                title: 'Error de comunicación',
                text: 'No fue posible crear la orden. HTTP ' + xhr.status,
                confirmButtonColor: '#2563eb'
            });
        }
    });
}


function consultarCandidatosRappiCargoTienda() {

	    var tienda = $('#ListTiendas option:selected').attr('id');

	    if ($.fn.dataTable.isDataTable('#grid-enviados-tienda-rappicargo')) {
	        tableEnviadosTiendaRappiCargo = $('#grid-enviados-tienda-rappicargo').DataTable();
	    }

		
		if (!tableEnviadosTiendaRappiCargo) {
		    return;
		}
		
		if (tienda == '' || tienda == null || tienda == '0') {
		    tableEnviadosTiendaRappiCargo.clear().draw();
		    return;
		}
		
	    var dsnodbc;
	    var pos;
	    var urlTienda;

	    $.ajax({
	        url: server + 'ObtenerUrlTienda?idtienda=' + tienda,
	        dataType: 'json',
	        async: false,
	        success: function(data2) {
	            urlTienda = data2[0].urltienda;
	            dsnodbc = data2[0].dsnodbc;
	            pos = data2[0].pos;
	        },
	        error: function() {
	            tableEnviadosTiendaRappiCargo.clear().draw();
	            alert('No fue posible obtener la información de la tienda');
	        }
	    });

	    if (!urlTienda || !dsnodbc || !pos) {
	        tableEnviadosTiendaRappiCargo.clear().draw();
	        return;
	    }

	    $.getJSON(
	        urlTienda + 'ConsultarCandidatosRappiCargoTienda',
	        function(data) {
	            tableEnviadosTiendaRappiCargo.clear().draw();

	            for (var i = 0; i < data.length; i++) {
	                data[i].idtienda = data[i].idtienda || parseInt(tienda) || 0;
	                tableEnviadosTiendaRappiCargo.row.add(data[i]);
	            }

	            tableEnviadosTiendaRappiCargo.draw();
	        }
	    ).fail(function() {
	        tableEnviadosTiendaRappiCargo.clear().draw();
	        alert('No fue posible consultar los candidatos RappiCargo de la tienda');
	    });
	}
	
	
	function esPedidoTercerizado(datospedido) {
	    var valor = datospedido.domicilioTercerizado || datospedido.domicilio_tercerizado || '';
	    return valor.toString().trim().toUpperCase() === 'S';
	}


	function confirmarNotificarPedidoListoRappiCargo(datospedido) {

	    Swal.fire({
	        icon: 'question',
	        title: '¿Marcar pedido listo para recoger en Rappi Cargo?',
	        html: `
	            <div class="rappi-info">
	                <div><b>Pedido tienda:</b> ${datospedido.idpedidotienda}</div>
	                <div><b>Cliente:</b> ${datospedido.nombreCompleto}</div>
	                <div><b>Dirección:</b> ${datospedido.direccion}</div>
	                <div><b>Estado:</b> ${datospedido.estadoActual}</div>
	                <div><b>Minutos cocina:</b> ${datospedido.minutosCocina}</div>
	            </div>
	        `,
	        showCancelButton: true,
	        confirmButtonText: 'Notificar listo',
	        cancelButtonText: 'Cancelar',
	        confirmButtonColor: '#7c3aed',
	        cancelButtonColor: '#6b7280',
	        reverseButtons: true
	    }).then((result) => {
	        if (result.isConfirmed) {
	            notificarPedidoListoRappiCargo(datospedido);
	        }
	    });
	}


	function notificarPedidoListoRappiCargo(datospedido) {

	    $.ajax({
	        url: server + 'NotificarPedidoListoRappiCargo',
	        type: 'POST',
	        dataType: 'json',
	        data: {
	            idPedidoTienda: datospedido.idpedidotienda,
	            idTienda: datospedido.idtienda
	        },
	        success: function (respuesta) {

	            if (respuesta.exito === true) {
	                Swal.fire({
	                    icon: 'success',
	                    title: 'Pedido notificado',
	                    text: respuesta.mensaje || 'El pedido fue marcado como listo para recoger en rappi Cargo.',
	                    timer: 2500,
	                    showConfirmButton: false
	                });

	                consultarCandidatosRappiCargoTienda();
	                return;
	            }

	            Swal.fire({
	                icon: 'warning',
	                title: 'No se pudo notificar',
	                text: respuesta.mensaje || respuesta.message || 'Rappi Cargo no aceptó la notificación.',
	                confirmButtonColor: '#2563eb'
	            });
	        },
	        error: function (xhr) {

	            var mensaje = 'No fue posible notificar el pedido listo. HTTP ' + xhr.status;

	            try {
	                var respuesta = JSON.parse(xhr.responseText);
	                mensaje = respuesta.mensaje || respuesta.message || mensaje;
	            } catch (e) {
	            }

	            Swal.fire({
	                icon: 'error',
	                title: 'Error notificando pedido',
	                text: mensaje,
	                confirmButtonColor: '#2563eb'
	            });
	        }
	    });
	}
	
	function verificarUltimoEstadoYContinuar(datospedido, validacion) {

	    $.ajax({
	        url: server + 'ConsultarUltimoEstadoRappiCargoTienda',
	        type: 'GET',
	        dataType: 'json',
	        data: {
	            idpedidotienda: datospedido.idpedidotienda,
	            idtienda: datospedido.idtienda
	        },
	        success: function (data) {
	            var estadoAnterior = data ? data.estado : null;
	            confirmarCrearOrdenCandidatoCargo(datospedido, validacion, estadoAnterior);
	        },
	        error: function () {
	            // Si falla la verificación, seguimos sin la advertencia de cancelación.
	            confirmarCrearOrdenCandidatoCargo(datospedido, validacion, null);
	        }
	    });
	}

	
	
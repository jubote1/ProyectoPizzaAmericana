	

var server;
var dtDirecciones;
var productos;
var excepciones;
var idPedido = 0;
var idCliente = 0;
var idEstadoPedido = 0;
var longitud = 0;
var latitud = 0;
var urlTienda ="";
var idformapago;
var totalpedido;
var valorformapago;
var stringPixel;
var tiendas;


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	//Inicializamos los municipios
	getListaMunicipios();
	getListaTiendas();
	getLlenarHoras();
	getPoligonos();

	    dtDirecciones = $('#grid-direcciones').DataTable( {
	    		"aoColumns": [
	    		{ "mData": "idpedido" },
	            { "mData": "direccion" },
	            { "mData": "municipio" },
	            { "mData": "idcliente" , "visible": false },
	            { "mData": "latitud" , "visible": false },
	            { "mData": "longitud" , "visible": false },
	            { "mData": "telefono" },
	            { "mData": "nombre" },
	            { "mData": "apellido" },
	            { "mData": "fechapedido" }
	          ]
	    	} );

   

     
	    $('#grid-direcciones').on('click', 'tr', function () {
	        datosDir = table.row(this ).data();
	     } );
 	
} );

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

// Método que invoca el servicio para listar las zonas de análisis a realizar
function getPoligonos(){
	$.getJSON(server + 'GetPoligonos', function(data){
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaPoligono  = data[i];
			str +='<option value="'+ cadaPoligono.ubicacionmapa +'" id ="'+ cadaPoligono.idpoligono +'">' + cadaPoligono.nombrepoligono+'</option>';
		}
		$('#selectPoligonoRevisar').html(str);
		// Realizamos cambio para que la tienda no esté seleccionada por defecto
		$("#selectPoligonoRevisar").val('');
	});
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
	$('#selectHoraInicial').html(str);
	$('#selectHoraFinal').html(str);
	$("#selectHoraInicial").val('');
	$("#selectHoraFinal").val('');
}

function validarDatosConsulta()
{
	var fechaIni = $("#fechainicial").val();
	var fechaFinal = $("#fechafinal").val();
	var selTienda = $("#selectTiendas").val();
	var horaIni = $("#selectHoraInicial").val();
	var horaFin = $("#selectHoraFinal").val();
	if(existeFecha(fechaIni))
	{

	}
	else
	{
		$.alert('La fecha inicial está incorrecta');
		return;
	}
	if(existeFecha(fechaFinal))
	{

	}
	else
	{
		$.alert('La fecha Final está incorrecta');
		return;
	}
	if((selTienda == '') || (selTienda == null))
	{
		$.alert('Debe seleccionar una Tienda para la consulta');
		return;
	}
	if((horaIni != "" || horaIni != null)&&(horaFin != "" || horaFin != null))
	{
		if((horaIni != "" || horaIni!= null)&&(horaFin == "" || horaFin == null))
		{
			$.alert('Se ingreso hora Inicial debe tambien ingresar hora final');
			return;
		}
		if((horaIni == "" || horaIni == null)&&(horaFin != "" || horaFin != null))
		{
			$.alert('Se ingreso hora Final debe tambien ingresar hora inicial');
			return;
		}
	}
	return(true);

}

function ubicarDireccionesPedidos()
{

	if(!validarDatosConsulta())
	{
		return;
	}
	if ( $.fn.dataTable.isDataTable( '#grid-direcciones' ) ) {
    	dtDirecciones = $('#grid-direcciones').DataTable();
    }
	var fechaIni = $("#fechainicial").val();
	var fechaFinal = $("#fechafinal").val();
	var idMunicipio = $("#selectMunicipio option:selected").attr('id');
	var idTienda = $("#selectTiendas option:selected").attr('id');;
	var horaIni = $("#selectHoraInicial").val();
	var horaFin = $("#selectHoraFinal").val();
	//Obtenemos el val del select que es donde está el poligono
	var ubicacionMapa = $('#selectPoligonoRevisar').val();
	// retornamos el idpoligono
	var idPoligono = $("#selectPoligonoRevisar option:selected").attr('id');
	var coordenadasPoligono;
	//Debemos de recuperar los puntos del poligono
	$.ajax({ 
	   	url: server + 'GetCoordenadasPoligono?idpoligono=' + idPoligono, 
	   	dataType: 'json',
	   	type: 'post', 
	   	async: false, 
	   	success: function(data){
			    coordenadasPoligono =  data;
		} 
	});
	//Definimos el poligono con el que trabaremos
	var poligono = new google.maps.Polygon({paths: coordenadasPoligono,
            strokeColor: "#FF0000",
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: "#FF0000",
            fillOpacity: 0.35
        });
	var longitud;
	var latitud;
	//Variables para control del total de pedidos y cuantos están dentro de la zona
	var cantidadPedidos = 0;
	var cantidadPedidosZona = 0;
	//Validamos si vamos a enviar municipio
	var consumoServicio = "";
			$.ajax({ 
			   	url: server + 'ConsultarDireccionesPedido?fechainicial=' + fechaIni + "&fechafinal="+ fechaFinal + "&idmunicipio="+idMunicipio+ "&idtienda="+idTienda+ "&horaini=" + horaIni + "&horafin="+horaFin, 
			   	dataType: 'json',
			   	type: 'post', 
			   	async: false, 
			   	success: function(data2){
			   		dtDirecciones.clear().draw();
			   		var map = new google.maps.Map($("#mapas").get(0), {
					    zoom: 7,
					    center: new google.maps.LatLng(6.22339, -75.6281),
					    mapTypeId: google.maps.MapTypeId.ROADMAP
					});
					var ctaLayer = new google.maps.KmlLayer({
				        url: ubicacionMapa,
				        map: map,
				        scrollwheel: false,
				        zoom: 17
			        });
					var infowindow = new google.maps.InfoWindow();
					var marker, i;
					var valorPedidos = 0;
					for(i = 0; i < data2.length;i++){
						cantidadPedidos = cantidadPedidos + 1; 
						var cadaDirFuera  = data2[i];
						dtDirecciones.row.add(data2[i]).draw();
						latitud = cadaDirFuera.latitud;
						longitud = cadaDirFuera.longitud;
						//Definidos la coordenada en objeto google maps
						var coordenada = new google.maps.LatLng(latitud, longitud);
						//Verificaremos que el punto esté incluido
						if(google.maps.geometry.poly.containsLocation(coordenada, poligono) == true) {

							//En este punto cuando la dirección está dentro del polígono hacemos la inserción en la tabla temporal
							$.getJSON(server + 'InsertarTmpPedidoPoligono?idcliente='+ cadaDirFuera.idcliente + '&idpedido=' + cadaDirFuera.idpedido , function(resulIns){
						    });					         
					        marker = new google.maps.Marker({
				        	position: new google.maps.LatLng(latitud, longitud),
				          	map: map,
				          	icon: 'http://maps.google.com/mapfiles/ms/icons/green-dot.png'
					        });
					        google.maps.event.addListener(marker, 'click', (function(marker, i) {
					        	return function() {
					            	infowindow.setContent("Dir Id " + cadaDirFuera.id);
					            	infowindow.open(map, marker);
					          	}
					        })(marker, i));
					        cantidadPedidosZona = cantidadPedidosZona + 1;
					        valorPedidos = valorPedidos + cadaDirFuera.valor;
					    }

						
				    }
				    //muestro el resultado del proceso
				    $.alert('CANTIDAD DE PEDIDOS ' +  cantidadPedidos + ' DE LOS CUALES EN LA ZONA ESTÁN ' + cantidadPedidosZona + '. El valor de dichos pedidos es ' + valorPedidos);
				} 
			});
	        
}


$(function(){
	
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

function initMap() {
		

		
		var map = new google.maps.Map(document.getElementById("mapas"), {
          zoom: 13,
          scrollwheel: false,
          center: {lat: 6.29139, lng: -75.53611}
        });

       
      }


function getListaMunicipios(){

	$.getJSON(server + 'CRUDMunicipio?idoperacion=5', function(data){
		
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaMunicipio  = data[i];
			str +='<option value="'+ cadaMunicipio.nombre +'" id ="'+ cadaMunicipio.idmunicipio +'">' + cadaMunicipio.nombre +'</option>';
		}
		str +='<option value="'+ "TODOS" +'" id ="'+ "TODOS" +'">' + "TODOS" +'</option>';
		$('#selectMunicipio').html(str);
		$("#selectMunicipio").val('');
	});

}
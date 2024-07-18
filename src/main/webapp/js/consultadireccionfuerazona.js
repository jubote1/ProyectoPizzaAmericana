	

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


$(document).ready(function() {

	//Obtenemos el valor de la variable server
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	//Inicializamos los municipios
	getListaMunicipios();


	    dtDirecciones = $('#grid-direcciones').DataTable( {
	    		"aoColumns": [
	    		{ "mData": "id" },
	            { "mData": "direccion" },
	            { "mData": "municipio" },
	            { "mData": "idcliente" , "visible": false },
	            { "mData": "latitud" , "visible": false },
	            { "mData": "longitud" , "visible": false },
	            { "mData": "telefono" },
	            { "mData": "nombre" },
	            { "mData": "apellido" },
	            { "mData": "fecha_ingreso" }
	          ]
	    	} );

   

     
	    $('#grid-direcciones').on('click', 'tr', function () {
	        datosDir = table.row(this ).data();
	     } );
 	
} );

function ubicarDireccionesFueraZona()
{
	if ( $.fn.dataTable.isDataTable( '#grid-direcciones' ) ) {
    	dtDirecciones = $('#grid-direcciones').DataTable();
    }
	var fechaIni = $("#fechainicial").val();
	var fechaFinal = $("#fechafinal").val();
	var municipio = $("#selectMunicipio").val();
	var longitud;
	var latitud;
			$.ajax({ 
			   	url: server + 'ConsultarDirsFueraZona?fechainicial=' + fechaIni + "&fechafinal="+ fechaFinal + "&municipio="+municipio, 
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
				        url: 'https://raw.githubusercontent.com/Andres-FA/KMLZonasDeReparto/master/PizzaAmericana-ZonasDeRepartoTotales-Ver_02.kml',
				        map: map,
				        scrollwheel: false,
				        zoom: 17
			        });
					var infowindow = new google.maps.InfoWindow();
					var marker, i;
					for(i = 0; i < data2.length;i++){
						var cadaDirFuera  = data2[i];
						dtDirecciones.row.add(data2[i]).draw();
						latitud = cadaDirFuera.latitud;
						longitud = cadaDirFuera.longitud;
						marker = new google.maps.Marker({
				        	position: new google.maps.LatLng(latitud, longitud),
				          	map: map
				        });
				        google.maps.event.addListener(marker, 'click', (function(marker, i) {
				        	return function() {
				            	infowindow.setContent("Dir Id " + cadaDirFuera.id);
				            	infowindow.open(map, marker);
				          	}
				        })(marker, i));
				    }
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
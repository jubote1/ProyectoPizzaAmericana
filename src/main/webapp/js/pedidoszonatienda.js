//Esta función recibiará latitud y lontitud
function ubicarDireccionesPedidos(latitud, longitud)
{

	
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
					    }

						
				    }
				    //muestro el resultado del proceso
				    $.alert('CANTIDAD DE PEDIDOS ' +  cantidadPedidos + ' DE LOS CUALES EN LA ZONA ESTÁN ' + cantidadPedidosZona);
				} 
			});
	        
}
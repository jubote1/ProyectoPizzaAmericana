/**
 * 
 *//**
 * 
 */
 var platform = new H.service.Platform({
  apikey: 'FVg2v7W51djQZY8TALJkAGRAJwtx7b-DLIMmhfsv_co' 
});
const defaultLayers = platform.createDefaultLayers(); 
const map = new H.Map(document.getElementById('map'), defaultLayers.vector.normal.map, {
  center: {lat:6.231516066110599, lng: -75.57780737703347},
  zoom: 12.5,

});
window.addEventListener('resize', () => map.getViewPort().resize());
const behavior = new H.mapevents.Behavior(new H.mapevents.MapEvents(map));
const ui = H.ui.UI.createDefault(map, defaultLayers,'es-ES');

var service = platform.getSearchService();  
var svgMarkup = '<svg  width="30" height="30"  xmlns="http://www.w3.org/2000/svg">' +
'<rect  fill="white" x="0" y="0" width="30" height="30" rx="30" ry="30" stroke="black" stroke-width="1"  opacity="0.9"/>' +
'<text x="50%" y="50%" alignment-baseline="middle" text-anchor="middle"  font-weight="bold"  fill="blue">${TITLE}</text></svg>';

var nombre_dom =  document.getElementById('nombre_domiciliario') ;
var direccion_dom = document.getElementById('direccion');
var subtitle = document.getElementById('subtitle');


var group = new H.map.Group();

map.addObject(group);
var xmlhttp;
var t = $('#example').DataTable({
	 language: {
        "decimal": "",
        "emptyTable": "No se encontraron registros.",
        "info": "Mostrando _START_ a _END_ de _TOTAL_ Entradas",
        "infoEmpty": "Mostrando 0 to 0 of 0 Entradas",
        "infoFiltered": "(Filtrado de _MAX_ total entradas)",
        "infoPostFix": "",
        "thousands": ",",
        "lengthMenu": "Mostrar _MENU_ Entradas",
        "loadingRecords": "Cargando...",
        "processing": "Procesando...",
        "search": "Buscar",
        "zeroRecords": "Sin resultados encontrados",
        "paginate": {
            "first": "Primero",
            "last": "Ultimo",
            "next": "Siguiente",
            "previous": "Anterior"
        }
    }
});





function AgregarMarker(nombre,clave,coordenadas){

  var iconClient = new H.map.Icon(svgMarkup.replace('${TITLE}',nombre.charAt(0))), 
  marker = new H.map.Marker(coordenadas, {
  icon:iconClient,
  volatility: true
  });
  marker.setData({"nombre":nombre,"codigo":clave});
  marker.addEventListener('tap', (ev)=> {
    service.reverseGeocode({
      at: ev.target.getGeometry()["lat"]+","+ev.target.getGeometry()["lng"]
    }, (result) => {
      result.items.forEach((item) => {
        direccion_dom.innerText = item.address.label
  
      });
    }, alert);

  nombre_dom.innerText = nombre;
  subtitle.innerHTML = "Ultima ubicacion"


  
  },false);
  
 group.addObject(marker);
  
}



function createXMLHttp(){
	var xmlhttp;
    if (window.XMLHttpRequest) {
        xmlhttp = new XMLHttpRequest();
      }

    return xmlhttp;
}

function Centrar(lat,lng){
 map.setCenter({"lat":lat,"lng":lng});
 map.setZoom(14);
}

function callback(){
	console.log("RESPUESTA " + xmlhttp.status);
	if(xmlhttp.readyState == 4 && xmlhttp.status == 200){
	
		var respuesta = JSON.parse(xmlhttp.responseText);
		console.log("RESPUESTA DENTRO" + respuesta);
		var id = respuesta[0];
		var jsonData = respuesta[1];
	     map.setCenter({'lat':6.231516066110599, 'lng': -75.57780737703347});
		 map.setZoom(11.3);
		nombre_dom.innerText = "Domiciliario";
  		subtitle.innerHTML = "Seleccione el marcador a localizar."
  		direccion_dom.innerText = ""
  		if(t.rows().count() != 0){
			 t.clear().draw();
			}
 
        group.removeAll();
		for(var j=0;j<respuesta[1].length;j++){
	
					t.row.add( [
		          		jsonData[j].fecha,
		          		jsonData[j].nombre_largo,
		          		jsonData[j].idtienda,
		          		"<button type='button' class='form btn btn-success btn-xs ' onclick='Centrar("+jsonData[j].latitud+","+jsonData[j].longitud+")'><i class='fas fa-map-marker-alt'></i></button>"
		          	        ] ).draw();
					AgregarMarker(jsonData[j].nombre_largo,jsonData[j].clave_dom,{"lat":jsonData[j].latitud,"lng": jsonData[j].longitud});
				}

	}

}


	var nombre ="";
	function sendQuestion(id,bool){
			idt = id
 			xmlhttp = createXMLHttp();
			var url = "UbicarDomiciliariosTienda?t=" + id+"&bool="+bool;
			xmlhttp.open("GET",url,true);
			xmlhttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencode');
			xmlhttp.setRequestHeader('Content-Type', 'application/xml');
			xmlhttp.onreadystatechange=callback;
			xmlhttp.send();
		
	}




  //esta funcion leera el archivo json que se defina como parametro.
function readTextFile(file, callback) {
  var rawFile = new XMLHttpRequest();
  rawFile.overrideMimeType("application/json");
  rawFile.open("GET", file, true);
  rawFile.onreadystatechange = function() {
  if (rawFile.readyState === 4 && rawFile.status == "200") {
      callback(rawFile.responseText);
  }
  }
  rawFile.send(null);
}


//leemos el archivo que contiene la informacion de   los poligonos y los dibujamos en el mapa.
readTextFile("poligonos.json", function(text){
  var data = JSON.parse(text);
  for(var i= 0;i< data.length;i++){
  var points=data[i]["coordinates"]
  var linestring = new H.geo.LineString();
  points.forEach(function(point) {
  linestring.pushPoint(point);


  });
  var yosmitePark = new H.map.Polygon(linestring, {
  style: {
  fillColor: data[i]["color"],
  lineWidth: 2,
  strokeColor:  data[i]["color"],
  }
  });
  map.addObject(yosmitePark);
  }
 
});



//leemos el archivo que contiene la ubicacion de los puntos de venta  y señalamos cada ubicacion con marcadores.
readTextFile("tiendas.json", function(text){
    var marker ;
    var data = JSON.parse(text);
    var group = new H.map.Group();
    for(var i= 0;i< data.length;i++){
    var icon = new H.map.Icon('img/pz.png'),
    points=data[i]["coordinates"],
    marker = new H.map.Marker(points, {icon: icon,min: 8});
    marker.setData(data[i]["title"]);
    group.addObject(marker)
    }
    map.addObject(group);
    
    group.addEventListener('tap', (event)=> {
      nombre_dom.innerText = "Lugar"
      subtitle.innerText = "Punto de venta"
       direccion_dom.innerText = event.target.getData()
      }, false);

});


const platform = new H.service.Platform({ apikey: 'FVg2v7W51djQZY8TALJkAGRAJwtx7b-DLIMmhfsv_co' });
const defaultLayers = platform.createDefaultLayers(); 

//inicializamos mapa con  coordenadas staticas
const map = new H.Map(document.getElementById('map'), defaultLayers.vector.normal.map, {
  center: {lat:6.231516066110599, lng: -75.57780737703347},
  zoom: 12.5,
  pixelRatio: window.devicePixelRatio || 1
});

var svgMarkup = '<svg visibility="${VISIBILITY}" width="85" height="30" xmlns="http://www.w3.org/2000/svg">'  +
'<rect  fill="${FILL}" x="1" y="1" width="85" height="30" rx="10" ry="10"  opacity="0.5"/>' +
'<text x="45" y="20" font-size="10pt" font-family="Arial" font-weight="bold" ' +
'text-anchor="middle" fill="${STROKE}">'+'${TITLE}'+'</text></svg>';

var titleIcon = new H.map.Icon(
  svgMarkup.replace('${FILL}', 'white').replace('${STROKE}', 'red').replace('${TITLE}',"Zona roja")),
  titleMarker = new H.map.Marker({lat: 6.346065450150455, lng: -75.55814003173828},
    {icon: titleIcon,min: 11});
  map.addObject(titleMarker);

var bubbleTienda;
var bubbleClient;
//al hacer clic en el mapa se cerrara la burbuja de informacion de alguno de los puntos de venta que este abierta o la del marcador del cliente.
map.addEventListener('pointerdown', function(evt) {
  ui.removeBubble(bubbleTienda)
  ui.removeBubble(bubbleClient)
});

const behavior = new H.mapevents.Behavior(new H.mapevents.MapEvents(map));
const ui = H.ui.UI.createDefault(map, defaultLayers,'es-ES');

//esta variable tendra como valor la ubicacion del cliente en un json
var ubicacion;
var coordenadas = null;
var iconClient = new H.map.Icon('markerClient.png'),coordenadas, 
markerClient = new H.map.Marker({lat:0,lng:0}, {
  icon:iconClient,
  volatility: true
  });
  toastr.options.timeOut = 5000; 

 function geocode(platform,searchText) {
      try{
        var geocoder = platform.getGeocodingService(),
        geocodingParameters = {
          searchText: searchText,
          jsonattributes : 1
        };

      geocoder.geocode(
        geocodingParameters,
        onSuccess,
        onError
      );
      }catch(error){
        console.error(error);
      }
  
  }

  //esta funcion genera una alerta en caso de que alla un error al geocodificar la direccion.
  function onError(error) {
    toastr.error('se presento un error con el servicio de geocodificacion');
  }

//esta funcion obtiene el resultado del servicio de geocodificacion.
  function onSuccess(result) {
    if(result.response.view.length > 0){
      var location = result.response.view[0].result[0].location.displayPosition;
      coordenadas =  {lat:location.latitude,lng: location.longitude};
      //la variable ubicacion toma el valor de las coordenadas de la direccion dada.
      ubicacion =  coordenadas;
      fijarCoordenadasManualmente(ubicacion);
      map.setCenter(coordenadas);
      map.setZoom(15);
      markerClient.setGeometry(coordenadas);
      //agregamos el marcador
      addDraggableMarker(map,behavior,markerClient,ui)
    }else{
      toastr.warning('verifique que la direccion sea correcta');
       toastr.error("No se encuentran coordenadas")
    
    }
  }

function inicializarMapa()
{
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
        var icon = new H.map.Icon('pz.png'),
        points=data[i]["coordinates"],
        marker = new H.map.Marker(points, {icon: icon,min: 8});
        marker.setData("<b>"+data[i]["title"]+"</b>");
        
      
        var zonaIcon = new H.map.Icon(
        svgMarkup.replace('${FILL}', 'white').replace('${STROKE}', 'red').replace('${TITLE}',data[i]["zona"])),
        zonaMarker = new H.map.Marker(data[i]["lugar_cercano"],
          {icon: zonaIcon,min: 11});
        map.addObject(zonaMarker);

        group.addObject(marker)
        }
        map.addObject(group);
        
        group.addEventListener('tap', (event)=> {
          
            //instanciamos un objeto que va a mostrar la informacion de los marcadores en  burbuja de información.
            bubbleTienda =  new H.ui.InfoBubble(event.target.getGeometry(), {
              
              content: event.target.getData()
            });
            //mostramos la burbuja de información
            ui.addBubble(bubbleTienda);
          }, false);

    });
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

$(function()
{
  window.addEventListener('resize', () => map.getViewPort().resize());
  //Inicializar Mapa para poderlo hacer en cualquier momento
  inicializarMapa();
});

function ubicarClienteExistente(latitud, longitud)
{
    coordenadas =  {lat:latitud,lng: longitud};
    //la variable ubicacion toma el valor de las coordenadas de la direccion dada.
    ubicacion =  coordenadas;
    map.setCenter(coordenadas);
    map.setZoom(15);
    markerClient.setGeometry(coordenadas);
    //agregamos el marcador
    addDraggableMarker(map,behavior,markerClient,ui);
}

//esta funcion devuelve el marcador a las coordenadas de la ultima  direccion buscada.
function EventoRecargar(markerClient){
  var recargar = document.querySelector("#recargarmapa");
    // Agregar listener
    recargar.addEventListener("click", function(){
      if(coordenadas != null){
    
        markerClient.setGeometry(coordenadas)
        map.setCenter(coordenadas);
        map.setZoom(15);
        ui.removeBubble(bubbleClient)
        //toma el valor nuevamente de las ultimas coordenadas buscadas.
        ubicacion = coordenadas;
      }
   

    });
}


//obtiene la direccion escrita en la caja de texto y se pasa como parametro a la funcion "geocode" que ejecuta el servico de geocodificacion.
function EventoBuscar(){
var  searchText;
searchText = $('#descDireccion').val();
var buscar = document.querySelector("#buscarmapa");
buscar.addEventListener("click",function(){
  searchText = $('#descDireccion').val();
  //esta funcion valida que no se obtenga un valor vacio y que en la caja de texto no se encuentren solo caracteres de espacio.
  if(searchText != null && /\S/.test(searchText)){
    geocode(platform,searchText)
  }
  });
  


}


//esta funcion maneja los eventos del marcador de la ubicacion del cliente
function addDraggableMarker(map, behavior,markerClient,ui){
    markerClient.draggable = true;
    map.addObject(markerClient);
    var service = platform.getSearchService();
//al hacer clic en el marcador se mostrara la direccion de la ubicacion en la que se encuentre.
    markerClient.addEventListener('tap', (ev)=> {
        service.reverseGeocode({
            at: ev.target.getGeometry()["lat"]+","+ev.target.getGeometry()["lng"]
          }, (result) => {
// Crea una InfoBubble en la ubicación devuelta con
            result.items.forEach((item) => {
              bubbleClient = new H.ui.InfoBubble(item.position, {
                content:"<b>"+item.address.label+"</b>"
              })
 
              ui.addBubble(bubbleClient);
            });
          }, alert);
          
    },false);

// deshabilita la capacidad de arrastre predeterminada del mapa subyacente
    // y calcular el desplazamiento entre el mouse y la posición del objetivo
    // al empezar a arrastrar un objeto marcador:
    map.addEventListener('dragstart', function(ev) {
    var target = ev.target,
    pointer = ev.currentPointer;
    if (target instanceof H.map.Marker) {
    var targetPosition = map.geoToScreen(target.getGeometry());
    target['offset'] = new H.math.Point(pointer.viewportX - targetPosition.x, pointer.viewportY - targetPosition.y);
    behavior.disable();
    }
    }, false);


// volver a habilitar la capacidad de arrastre predeterminada del mapa subyacente
    // cuando se ha completado el arrastre.

    map.addEventListener('dragend', function(ev) {
        var service = platform.getSearchService();
    var target = ev.target;
    if (target instanceof H.map.Marker) {
    behavior.enable();
    //toma el valor de las coordenadas de la ubicacion donde se arrastro el marcador.
    ubicacion = target.getGeometry();
    fijarCoordenadasManualmente(ubicacion);
    }
   

    }, false);

 // Escuche el evento de arrastre y mueva la posición del marcador
    // según sea necesario
    map.addEventListener('drag', function(ev) {

    var target = ev.target,
    pointer = ev.currentPointer;
    if (target instanceof H.map.Marker) {
    target.setGeometry(map.screenToGeo(pointer.viewportX - target['offset'].x, pointer.viewportY - target['offset'].y));
    
    }

    }, false);
}


function clarearMapa()
{
    ubicacion = {lat: 0, lng: 0};
    markerClient.setGeometry({lat: 6.25825, lng: -75.57});
    map.setCenter({lat: 6.25825, lng: -75.57});
    map.setZoom(11.4);
    markerClient.setGeometry(ubicacion);
    addDraggableMarker(map,behavior,markerClient,ui);
    ui.removeBubble(bubbleClient);
}

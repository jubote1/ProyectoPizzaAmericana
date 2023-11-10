
import { getFirestore,collection ,where,onSnapshot,query,getDocs} from "https://www.gstatic.com/firebasejs/9.6.1/firebase-firestore.js";

var platform = new H.service.Platform({
  apikey: 'FVg2v7W51djQZY8TALJkAGRAJwtx7b-DLIMmhfsv_co' 
});
const defaultLayers = platform.createDefaultLayers(); 
const map = new H.Map(document.getElementById('map'), defaultLayers.vector.normal.map, {
  center: {lat:6.231516066110599, lng: -75.57780737703347},
  zoom: 12.5,

});
//Obtenemos el valor de la variable server
var loc = window.location;
var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
window.addEventListener('resize', () => map.getViewPort().resize());
const behavior = new H.mapevents.Behavior(new H.mapevents.MapEvents(map));
const ui = H.ui.UI.createDefault(map, defaultLayers,'es-ES');
var  id_tienda = 0;
var grupos  = [];

const tiendas = [{
  "nombre": "Manrique","id": 1},{"nombre": "La Mota","id": 7},
  {"nombre": "Bello","id": 2}, {"nombre": "Envigado","id": 8},
  {"nombre": "America","id": 3}, {"nombre": "Pilarica","id": 9},
  {"nombre": "Calasanz","id": 4},{"nombre": "San antonio","id": 10}, 
  {"nombre": "Itagui","id": 5}, {"nombre": "Piloto","id": 11},
  {"nombre": "Poblado","id": 6,}]
  for (var key in tiendas) {
    if (tiendas.hasOwnProperty(key)) {
    var group = new H.map.Group();
    group.setData(tiendas[key].id);
    group.setVisibility(false);
    grupos.push(group);
    map.addObject(group);
      } 
 }

const db = getFirestore();
const q = query(collection(db, "Domiciliario"));
const unsubscribe = onSnapshot(q, (querySnapshot) => {
  querySnapshot.docChanges().forEach((change) => {
    if (change.type === "modified") {

  for(var i=0;i < grupos.length;i++){
    if(grupos[i].getData() == change.doc.data().id_tienda){
      var listMarkers = grupos[i].getObjects()
      for(var j=0;j < listMarkers.length ;j++){
        var DataMarker = listMarkers[j].getData()["codigo"];
        if(DataMarker == change.doc.data().id){
          var ubicacion = change.doc.data().ubicacion;
          listMarkers[j].setGeometry(ubicacion);
        }else{
          ejecutarConsulta(change.doc,i);
        }
      }
    }
      
  }

}
});
});

var service = platform.getSearchService();
var svgMarkup = '<svg  width="30" height="30"  xmlns="http://www.w3.org/2000/svg">' +
'<rect  fill="white" x="0" y="0" width="30" height="30" rx="30" ry="30" stroke="black" stroke-width="1"  opacity="0.9"/>' +
'<text x="50%" y="50%" alignment-baseline="middle" text-anchor="middle"  font-weight="bold"  fill="blue">${TITLE}</text></svg>';
var nombre_dom =  document.getElementById('nombre_domiciliario') ;
var direccion_dom = document.getElementById('direccion');
var subtitle = document.getElementById('subtitle');
var subtitle2 = document.getElementById('subtitle2');
var fecha_y_hora= document.getElementById('fecha_y_hora');


var index_grupo = null;
function ejecutarConsulta(doc,i){
  var nombre =  doc.data().nombre;
  var id = doc.data().id;
  var coordenadas = doc.data().ubicacion;
  var ultima_fecha_y_hora= doc.data().ultima_fecha_y_hora;

  if(coordenadas != null){
  var iconClient = new H.map.Icon(svgMarkup.replace('${TITLE}',nombre.charAt(0))), 
  marker = new H.map.Marker(coordenadas, {
  icon:iconClient,
  volatility: true
  });
  marker.setData({"nombre":nombre,"codigo":id});
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
  if(ultima_fecha_y_hora != null){
    subtitle2.innerHTML = "Fecha y hora"
    fecha_y_hora.innerText = ultima_fecha_y_hora
  }else{
    subtitle2.innerHTML = ""
    fecha_y_hora.innerText = ""
  }
  },false);

  grupos[i].addObject(marker);
  }
}
$('.dropdown-item').click(async function(){
      for(var i=0;i < grupos.length;i++){
     
        if(grupos[i].getData() == parseInt($(this).val())){
          
          if(!$(this)[0].id){
  
          const doc_Dom = query(collection(db, "Domiciliario"),where("id_tienda","==",parseInt($(this).val())));
          var querySnapshot =  await getDocs(doc_Dom);

          querySnapshot.forEach((doc) => {
            ejecutarConsulta(doc,i);

          });
            $(this)[0].id="item-"+i
          }  
          if( index_grupo != null && id_tienda != parseInt($(this).val())){
            grupos[index_grupo].setVisibility(false);
          }
          grupos[i].setVisibility(true);
          index_grupo = i;
          id_tienda = parseInt($(this).val())
        }
      }
    } );

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
    var icon = new H.map.Icon('pz.png'),
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


//----------------------------------------------
/*  const dropdown_menu = document.getElementById(tiendas[key].nombre);
       var coordenadas = doc.data().ubicacion;
       const options = document.createElement('a');
       options.setAttribute("value","hola");
       options.setAttribute("class","dropdown-item");
       options.innerText  =doc.data().nombre;
       options.addEventListener("click",function(){
        if(coordenadas != null){
          marker.setGeometry(coordenadas);
          map.setCenter(coordenadas);
         }
      // group.setData(0,"hola")
       //group.getObjects()[0].setData("hola")
       //console.log(group.getObjects()[0].data)
       });
    
       dropdown_menu.insertAdjacentElement('beforeend', options)
*/

// Obtain the default map types from the platform object


// Instantiate (and display) a map object:

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
  




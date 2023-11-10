var marker = null;
var view = null;

require(["esri/config", 
"esri/Map", "esri/views/MapView", 
"esri/layers/GraphicsLayer","esri/Graphic",
"esri/symbols/PictureMarkerSymbol" ,"esri/rest/locator",
 "esri/widgets/Search", "esri/layers/FeatureLayer"], function(esriConfig, Map, MapView,GraphicsLayer,Graphic,PictureMarkerSymbol,locator,FeatureLayer) {

    esriConfig.apiKey = "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";
    const serviceUrl = "http://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer";

    const map = new Map({
      basemap: "arcgis-topographic", // Basemap layer service,
    ground: "world-elevation"
    });

    view = new MapView({
        map: map,
        center: [-75.56359, 6.25184], // Longitude, latitude
        zoom: 13, // Zoom level
        container: "map" // Div element
      });

      
      const graphicsLayer = new GraphicsLayer();
     

      const markerSymbol = new PictureMarkerSymbol({
        url: 'pz.png',
        width: 20,  // Ancho de la imagen en píxeles
        height: 20, // Altura de la imagen en píxeles

      });

   //agregamos los poligonos que encierran las zonas

     readTextFile("poligonos2.json", function(text){
        var data = JSON.parse(text);
        for(var i= 0;i< data.length;i++){
        var points=data[i]["coordinates"]
        var color =data[i]["color"]

        polygon = {
            type: "polygon",
            rings: points
         };

             
        simpleFillSymbol = {
            type: "simple-fill",
            color: color,  // Orange, opacity 80%
            outline: {
                color: [255, 255, 255],
                width: 1
            }
        };


        polygonGraphic = new Graphic({
            geometry: polygon,
            symbol: simpleFillSymbol,
        
         });
         graphicsLayer.add(polygonGraphic);
        }

    
       
      });

      
      const text_tienda = {
        type: "text",  // autocasts as new TextSymbol()
        color: "white",
        haloColor: "black",
        haloSize: "1px",
        text: "Wish you were here",
        xoffset: 3,
        yoffset: 3,
        font: {  // autocasts as new Font()
          size: 9,
          family: "Orbitron",
          weight: "bold"
        }
      };

      //agregamos los marcadores de las tiendas
      readTextFile("tiendas.json", function(text){
        var data = JSON.parse(text);
        for(var i= 0;i< data.length;i++){
        var points=data[i]["coordinates"]
        var points2=data[i]["lugar_cercano"]

        markerGeometry = {
          type: 'point',
          longitude: points["lng"], // Longitud del marcador
          latitude: points["lat"] // Latitud del marcador
        };

        const template = {
          title:data[i]["title"]
        }

        markerGraphic = new Graphic({
          geometry: markerGeometry,
          symbol: markerSymbol,
          popupTemplate: template
      });


        graphicsLayer.add(markerGraphic);
         
        const point_tienda = {
          type: "point",  // autocasts as new Point()
          longitude: points2["lng"],
          latitude: points2["lat"]
        };
        
        text_tienda.text = data[i]["zona"]

        const Graphic_tienda = new Graphic({
          geometry: point_tienda,
          symbol: text_tienda
        });

        view.graphics.add(Graphic_tienda)
        }

       
      });

      point_client = {
        type: "point",  // autocasts as new Point()
        longitude: -75.56359,
        latitude: 6.25184
      };
      
      // Create a symbol for drawing the point
      const marker_client = new PictureMarkerSymbol({
        url: 'markerClient.png',
        width: 24,
        height: 24,
        xoffset: 0,
        yoffset: 12
      });

      
     marker =  new Graphic({
        geometry:point_client,
        symbol: marker_client
 
      });

    view.graphics.add(marker);
    map.add(graphicsLayer);

    view.on("click", (event) => {
      // Realizar una prueba de selección espacial con el clic del usuario
      view.hitTest(event).then(function(response) {
        // Obtener las gráficas que se han intersectado con el clic
        const graphics = response.results.map(function(result) {
          return result.graphic;
        });
        
        // Verificar si se hizo clic en un marcador
        const isMarkerClicked = graphics.some(function(graphic) {
          // Verificar si la gráfica tiene un símbolo de marcador
          return graphic.symbol && graphic.symbol.type === "picture-marker";
        });
        
        // Hacer algo si se hizo clic en un marcador
        if (!isMarkerClicked) {
          const params = {
            location: event.mapPoint,
            outFields: "*"
          };
          fijarCoordenadasManualmente(event.mapPoint.latitude,event.mapPoint.longitude);
          locator.locationToAddress(serviceUrl, params).then(
              function (response) {
                // Show the address found
                showAddress(response.address, event.mapPoint)
                // Show no address found
            
            }
            );
        }

      });

    });


   //configuramos la cuadro donde se muestra la direccion del marcador.
    view.popup = {
      dockEnabled: true,
      dockOptions: {
        position: "top-left",
        breakpoint: false
      }
     };
     


      //se busca la direccion escrita en el input y se se actualizan las coordenadas el marcador.
      function findAddress(){

        var address = $("#descDireccion").val();  //"Diagonal 55 #43-90, Niquia, Bello, Antioquia";
        // Definir los parámetros de la solicitud
        var params = {
          address:{"address":address},
        };          

          if(address != ""){
            locator.addressToLocations(serviceUrl,params).then(function(response){
              if(response[0] != null)
              {
                  marker.geometry =response[0].location
                  showAddress(address, marker.geometry)
                  fijarCoordenadasManualmente(response[0].location.latitude,response[0].location.longitude);
              }
            });
  
    
          }
    
  
      }
    

      $("#buscarmapa").on("click",function(){
    
        findAddress()
      });

      

});

// funcion para leer el archivo json.
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


 //se abre un  widget emergente que permite ver la direccion que se ha seleccionado con el marcador.
   function showAddress(address, location) {
      marker.geometry = location
      view.openPopup({
          title:Math.round(location.latitude * 100000) / 100000 +", " + Math.round(location.longitude * 100000) / 100000,
          content: address
     
        });

        view.goTo({
          center: [location.longitude,location.latitude]
        });
      }


  function clarearMapa()
{
    
    const params={
      longitude :-75.56359,latitude:6.25184,type:"point"
    }
    marker.geometry =params
    view.goTo({
          center:  [-75.56359, 6.25184]
        });
}
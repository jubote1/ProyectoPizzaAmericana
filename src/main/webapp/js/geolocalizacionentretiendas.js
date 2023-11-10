
var server;
var tiendas;


$(document).ready(function() {

     getListaTiendas();
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
          
function getListaTiendas(){
     $.getJSON(server + 'GetTiendas', function(data){
          tiendas = data;
          var str = '';
          for(var i = 0; i < data.length ;i++){
               var cadaTienda  = data[i];
               str +='<option value="'+ cadaTienda.nombre +'" id ="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
          }
          $('#selectTiendas').html(str);
          // Realizamos cambio para que la tienda no esté seleccionada por defecto
          $("#selectTiendas").val('');
     });
}

function revisarGeolocalizaciontiendas()
{
     var idtien = $("#selectTiendas option:selected").attr('id');
     var clientes;
     var j = 1;
     $.ajax({ 
          url: server + 'ObtenerClientesTienda?idtienda=' + idtien , 
          dataType: 'json', 
          async: false, 
          success: function(data){
                    clientes = data;
                    console.log("cantidad clientes en corrida" + clientes.length);
                    
               },
               error: function(){
                         $.alert('No se pudo recuperar los clientes para revisar la Geolocalización');
               } 

     });
                    var dirbuscar;
                    var idCliente;
                    var ciudadBuscar;
                    var idTiendaAnterior;
                    var direcc;
                    var dirSplit;
                    for(var i = 0; i < 6 ;i++)
                    {
                         
                         idCliente = clientes[i].idcliente;
                         ciudadBuscar = clientes[i].nombremunicipio;
                         idTiendaAnterior = clientes[i].idtienda;
                         direcc = clientes[i].direccion;
                         if (direcc == null || direcc == '')
                         {
                              direcc = "  -  ";
                         }
                         dirSplit = direcc.split("-");
                         // Se podría decir qeu si tenía un guion y quitaremos el último pedaza que se supone sería el del número
                         if(dirSplit.length == 2)
                         {
                              dirbuscar = dirSplit[0];
                         }
                         else
                         {
                              
                              dirSplit = direcc.split(' ');
                              for(var j = 0; j < dirSplit.length -1; j++)
                              {
                                   dirbuscar =  dirbuscar + " " + dirSplit[j];
                              }
                         }
                         dirbuscar = dirbuscar + " " + ciudadBuscar;
                         buscarMapa(dirbuscar, idTiendaAnterior, idCliente, ciudadBuscar,j, i);
                         j++;
                         if(j == 4)
                         {
                              j = 1;
                         }
                         //Creamos retardo de 3 segundos
                         
                         
                         
                    }   
                    console.log("fin");  
}


function ubicarTienda(latitud, longitud, dir, idTiendaAnterior, idCliente, ciudadBuscar ) 
{

     var americacor = [
          {lat: 6.2314567,lng: -75.5896977},
          {lat: 6.231738,lng: -75.583797},
          {lat: 6.23187,lng: -75.577617},
          {lat: 6.233419,lng: -75.577691},
          {lat: 6.234946,lng: -75.577271},
          {lat: 6.2364,lng: -75.576776},
          {lat: 6.237746,lng: -75.575938},
          {lat: 6.239314,lng: -75.575423},
          {lat: 6.238343,lng: -75.573181},
          {lat: 6.242631,lng: -75.571472},
          {lat: 6.247217,lng: -75.569764},
          {lat: 6.249926,lng: -75.577628},
          {lat: 6.250779,lng: -75.57906},
          {lat: 6.252517,lng: -75.581168},
          {lat: 6.253003,lng: -75.582496},
          {lat: 6.253104,lng: -75.583953},
          {lat: 6.253003,lng: -75.585632},
          {lat: 6.253584,lng: -75.588191},
          {lat: 6.25368,lng: -75.590476},
          {lat: 6.254341,lng: -75.591549},
          {lat: 6.255813,lng: -75.592176},
          {lat: 6.257285,lng: -75.592911},
          {lat: 6.257882,lng: -75.5937},
          {lat: 6.258138,lng: -75.594488},
          {lat: 6.258879,lng: -75.598534},
          {lat: 6.259228,lng: -75.60004},
          {lat: 6.259386,lng: -75.601492},
          {lat: 6.259072,lng: -75.602435},
          {lat: 6.258523,lng: -75.603207},
          {lat: 6.257516,lng: -75.60441},
          {lat: 6.256816,lng: -75.605312},
          {lat: 6.256276,lng: -75.606306},
          {lat: 6.256173,lng: -75.606928},
          {lat: 6.256101,lng: -75.608868},
          {lat: 6.255973,lng: -75.611134},
          {lat: 6.255002,lng: -75.612014},
          {lat: 6.255478,lng: -75.613026},
          {lat: 6.256173,lng: -75.614361},
          {lat: 6.256671,lng: -75.61384},
          {lat: 6.256929,lng: -75.614244},
          {lat: 6.257133,lng: -75.614744},
          {lat: 6.258394,lng: -75.61392},
          {lat: 6.259035,lng: -75.613513},
          {lat: 6.259297,lng: -75.613355},
          {lat: 6.25974,lng: -75.613202},
          {lat: 6.25983,lng: -75.614033},
          {lat: 6.259834,lng: -75.615074},
          {lat: 6.259944,lng: -75.616623},
          {lat: 6.25966,lng: -75.617406},
          {lat: 6.259584,lng: -75.617917},
          {lat: 6.25944,lng: -75.618252},
          {lat: 6.259399,lng: -75.619613},
          {lat: 6.259013,lng: -75.619404},
          {lat: 6.258686,lng: -75.619335},
          {lat: 6.25842,lng: -75.619401},
          {lat: 6.258906,lng: -75.621013},
          {lat: 6.257884,lng: -75.621402},
          {lat: 6.258132,lng: -75.622091},
          {lat: 6.257601,lng: -75.62213},
          {lat: 6.25685,lng: -75.622576},
          {lat: 6.257013,lng: -75.623757},
          {lat: 6.257788,lng: -75.62451},
          {lat: 6.258472,lng: -75.625188},
          {lat: 6.259071,lng: -75.626047},
          {lat: 6.257818,lng: -75.627187},
          {lat: 6.257546,lng: -75.627047},
          {lat: 6.257466,lng: -75.626686},
          {lat: 6.256997,lng: -75.626303},
          {lat: 6.256799,lng: -75.625778},
          {lat: 6.256453,lng: -75.625302},
          {lat: 6.255413,lng: -75.625779},
          {lat: 6.25494,lng: -75.624929},
          {lat: 6.254415,lng: -75.624701},
          {lat: 6.254149,lng: -75.62447},
          {lat: 6.254098,lng: -75.624151},
          {lat: 6.254016,lng: -75.623587},
          {lat: 6.254006,lng: -75.623321},
          {lat: 6.254109,lng: -75.623109},
          {lat: 6.253942,lng: -75.62265},
          {lat: 6.25392,lng: -75.621949},
          {lat: 6.254052,lng: -75.621732},
          {lat: 6.254214,lng: -75.621494},
          {lat: 6.254514,lng: -75.62138},
          {lat: 6.253922,lng: -75.621316},
          {lat: 6.253304,lng: -75.62137},
          {lat: 6.253189,lng: -75.621954},
          {lat: 6.252953,lng: -75.622077},
          {lat: 6.252584,lng: -75.622092},
          {lat: 6.252273,lng: -75.622097},
          {lat: 6.251984,lng: -75.622021},
          {lat: 6.251166,lng: -75.621686},
          {lat: 6.251743,lng: -75.620027},
          {lat: 6.252149,lng: -75.61931},
          {lat: 6.252528,lng: -75.618497},
          {lat: 6.251311,lng: -75.618603},
          {lat: 6.250396,lng: -75.618658},
          {lat: 6.249491,lng: -75.618668},
          {lat: 6.248371,lng: -75.616897},
          {lat: 6.247931,lng: -75.616167},
          {lat: 6.247249,lng: -75.615605},
          {lat: 6.246212,lng: -75.616528},
          {lat: 6.245164,lng: -75.617386},
          {lat: 6.24536,lng: -75.618208},
          {lat: 6.245535,lng: -75.6202},
          {lat: 6.245501,lng: -75.620892},
          {lat: 6.245726,lng: -75.621166},
          {lat: 6.245199,lng: -75.621646},
          {lat: 6.24465,lng: -75.622031},
          {lat: 6.243601,lng: -75.622949},
          {lat: 6.243503,lng: -75.622934},
          {lat: 6.243405,lng: -75.622592},
          {lat: 6.243246,lng: -75.622476},
          {lat: 6.243193,lng: -75.622186},
          {lat: 6.241767,lng: -75.621176},
          {lat: 6.241905,lng: -75.619619},
          {lat: 6.241971,lng: -75.618797},
          {lat: 6.241824,lng: -75.618034},
          {lat: 6.242172,lng: -75.617056},
          {lat: 6.241692,lng: -75.615903},
          {lat: 6.240911,lng: -75.61597},
          {lat: 6.236821,lng: -75.613975},
          {lat: 6.236345,lng: -75.615773},
          {lat: 6.236632,lng: -75.617486},
          {lat: 6.235592,lng: -75.617979},
          {lat: 6.235296,lng: -75.617754},
          {lat: 6.235054,lng: -75.617388},
          {lat: 6.234884,lng: -75.616556},
          {lat: 6.234672,lng: -75.616215},
          {lat: 6.234582,lng: -75.615836},
          {lat: 6.234517,lng: -75.615152},
          {lat: 6.233156,lng: -75.615729},
          {lat: 6.231742,lng: -75.616128},
          {lat: 6.231832,lng: -75.6150883},
          {lat: 6.2310569,lng: -75.6133853},
          {lat: 6.2312628,lng: -75.6065842},
          {lat: 6.2311891,lng: -75.6030216},
          {lat: 6.2310721,lng: -75.6017005},
          {lat: 6.2313497,lng: -75.5956991},
          {lat: 6.2314567,lng: -75.5896977}
];

     var lamotacor = [
          {lat: 6.1924096,lng: -75.5920955},
          {lat: 6.1913241,lng: -75.5831719},
          {lat: 6.1953394,lng: -75.581697},
          {lat: 6.1952964,lng: -75.5815601},
          {lat: 6.2019094,lng: -75.5793285},
          {lat: 6.203936,lng: -75.5795646},
          {lat: 6.206784,lng: -75.5859309},
          {lat: 6.2113224,lng: -75.5838354},
          {lat: 6.2129903,lng: -75.5868091},
          {lat: 6.2134636,lng: -75.5906412},
          {lat: 6.2084759,lng: -75.59207},
          {lat: 6.2112831,lng: -75.5942542},
          {lat: 6.2314901,lng: -75.589542},
          {lat: 6.2313612,lng: -75.5951913},
          {lat: 6.2311683,lng: -75.600154},
          {lat: 6.2310721,lng: -75.6017005},
          {lat: 6.2311891,lng: -75.6030216},
          {lat: 6.2311033,lng: -75.605739},
          {lat: 6.2312628,lng: -75.6065842},
          {lat: 6.2310756,lng: -75.6098165},
          {lat: 6.2310569,lng: -75.6133853},
          {lat: 6.2316021,lng: -75.6147137},
          {lat: 6.231832,lng: -75.6150883},
          {lat: 6.231742,lng: -75.616128},
          {lat: 6.228381,lng: -75.614128},
          {lat: 6.2246964,lng: -75.6116253},
          {lat: 6.2248578,lng: -75.6109192},
          {lat: 6.2243899,lng: -75.6098697},
          {lat: 6.224148,lng: -75.608731},
          {lat: 6.221705,lng: -75.608672},
          {lat: 6.2211436,lng: -75.611737},
          {lat: 6.2204842,lng: -75.6132747},
          {lat: 6.21968,lng: -75.613534},
          {lat: 6.2181259,lng: -75.6118935},
          {lat: 6.2193327,lng: -75.6098715},
          {lat: 6.2139953,lng: -75.607777},
          {lat: 6.2050315,lng: -75.6095019},
          {lat: 6.2022121,lng: -75.6033241},
          {lat: 6.2005251,lng: -75.600128},
          {lat: 6.1968657,lng: -75.599281},
          {lat: 6.195366,lng: -75.596978},
          {lat: 6.194196,lng: -75.595418},
          {lat: 6.193646,lng: -75.595413},
          {lat: 6.1929418,lng: -75.5920755},
          {lat: 6.1924096,lng: -75.5920955}
     ];

     var idTiendaActual = 0;
     var coordenada = new google.maps.LatLng(latitud, longitud);
     //validamos poblado
     
     var poligono1 = new google.maps.Polygon({paths: americacor,
            strokeColor: "#FF0000",
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: "#FF0000",
            fillOpacity: 0.35
        });
     

     if(google.maps.geometry.poly.containsLocation(coordenada, poligono1) == true) {
          idTiendaActual = 3;
          //console.log(idTiendaActual + " " + dir);
          //console.log(" pase america ");
          //return;
     }
     
     var poligono2 = new google.maps.Polygon({paths: lamotacor,
            strokeColor: "#FF0000",
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: "#FF0000",
            fillOpacity: 0.35
        });
     

     if(google.maps.geometry.poly.containsLocation(coordenada, poligono2) == true) {
          idTiendaActual = 7;
          //console.log(idTiendaActual + " " + dir);
          //console.log(" pase la mota ");
          //return;
     }

     // Se invoca el servicio
     //console.log("raro " + idTiendaActual);
     dir = encodeURIComponent(dir);
     $.ajax({ 
          url: server + 'InsertarClienteGeolocalizado?idtiendaactual=' + idTiendaActual + '&idtiendaanterior=' + idTiendaAnterior + "&idcliente=" + idCliente + "&direccion=" + dir + "&municipio=" + ciudadBuscar , 
          dataType: 'json', 
          async: false, 
         success: function(data){
              }
     });
     
          
}

function buscarMapa(dir, idTiendaAnterior, idCliente, ciudadBuscar, i, h) {

    // Obtenemos la dirección y la asignamos a una variable
    var direccion = dir + " Antioquia Colombia" ; 
    var resultado;
        
         $.ajax({ 
                              url:'https://maps.googleapis.com/maps/api/geocode/json?components=administrative_area:Medellin|country:Colombia&address=' + direccion +'&key=AIzaSyBH1VN3540Ux8Y92wDv61horvr8SUqNd_s' , 
                              dataType: 'json', 
                              async: false, 
                              success: function(data){ 
                                             resultado = data;
                                             // Creamos el Objeto Geocoder
                                            var geocoder = new google.maps.Geocoder();
                                            // Hacemos la petición indicando la dirección e invocamos la función
                                            // geocodeResult enviando todo el resultado obtenido
                                            

                                             geocoder.geocode({ 'address': direccion}, function(results, status){
                                                  if (status == 'OK') {
                                                     // Si hay resultados encontrados, centramos y repintamos el mapa
                                                     // esto para eliminar cualquier pin antes puesto
                                                     var mapOptions = {
                                                         center: results[0].geometry.location,
                                                         mapTypeId: google.maps.MapTypeId.ROADMAP
                                                     };
                                                     var longitud = results[0].geometry.location.lng();
                                                     var latitud = results[0].geometry.location.lat();
                                                     //console.log("Si fue ubicado y las coordenadas son " + longitud + " " + latitud);
                                                     //Luego de la ubicación en el mapa trataremos de ejecutar una función asincrona para ubicar dentro del mapa y ubicar la tienda
                                                     ubicarTienda(latitud , longitud, dir, idTiendaAnterior, idCliente, ciudadBuscar);
                                                     
                                                 } else {
                                                     // En caso de no haber resultados o que haya ocurrido un error
                                                     // lanzamos un mensaje con el error
                                                     alert("La Geolocalización no tuvo éxito debido a: " + status + " " + dir);
                                                     console.log(idCliente);
                                                 }
                                              });

                                            
                                            
                                            
                                        } 
                                   });
     
    
}


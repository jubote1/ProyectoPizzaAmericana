var marker = null;
var view = null;
var mapArcgis = null;
var ultimaDireccionDetectada = null;
var modoMoverMarcador = false;
var markerClientNormal = null;
var markerClientSeleccionado = null;
var markerHaloGraphicOuter = null;
var markerHaloGraphicInner = null;
var SimpleMarkerSymbolRef = null;
var GraphicRef = null;

const URL_VALIDACION_COBERTURA = obtenerUrlValidacionCobertura();

require([
  "esri/config",
  "esri/Map",
  "esri/views/MapView",
  "esri/layers/GraphicsLayer",
  "esri/Graphic",
  "esri/symbols/PictureMarkerSymbol",
  "esri/symbols/SimpleMarkerSymbol",
  "esri/layers/FeatureLayer"
], function (
  esriConfig,
  Map,
  MapView,
  GraphicsLayer,
  Graphic,
  PictureMarkerSymbol,
  SimpleMarkerSymbol,
  FeatureLayer
) {
  "use strict";

  SimpleMarkerSymbolRef = SimpleMarkerSymbol;
  GraphicRef = Graphic;

  esriConfig.apiKey = "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";

  const CENTRO_MAPA = {
    longitude: -75.56359,
    latitude: 6.25184,
    type: "point"
  };

  const map = new Map({
    basemap: "gray-vector",
    ground: "world-elevation"
  });
  mapArcgis = map;

  view = new MapView({
    map: map,
    center: [CENTRO_MAPA.longitude, CENTRO_MAPA.latitude],
    zoom: 13,
    container: "map",
    popup: {
      autoOpenEnabled: false
    },
    highlightOptions: {
      color: [0, 0, 0, 0],
      fillOpacity: 0
    }
  });

  const graphicsLayer = new GraphicsLayer();

  const markerSymbol = new PictureMarkerSymbol({
    url: "pz.png",
    width: 20,
    height: 20
  });

  const markerClient = new PictureMarkerSymbol({
    url: "markerClient.png",
    width: 24,
    height: 24,
    xoffset: 0,
    yoffset: 12
  });
  markerClientNormal = markerClient;

  const textTienda = {
    type: "text",
    color: "white",
    haloColor: "black",
    haloSize: "1px",
    text: "",
    xoffset: 3,
    yoffset: 3,
    font: {
      size: 8,
      family: "Orbitron",
      weight: "bold"
    }
  };

  marker = new Graphic({
    geometry: CENTRO_MAPA,
    symbol: markerClient
  });

  view.graphics.add(marker);
  map.add(graphicsLayer);

  cargarZonas(map, FeatureLayer, graphicsLayer, Graphic);
  cargarTiendas(graphicsLayer, Graphic, markerSymbol, textTienda);
  configurarPopup();
  configurarEventosMapa();
  configurarEventosFormulario();


  function cargarZonas(map, FeatureLayer, graphicsLayer, Graphic) {
    const palette = [
      [255, 99, 71, 0.22],
      [60, 179, 113, 0.22],
      [65, 105, 225, 0.22],
      [238, 130, 238, 0.22],
      [255, 165, 0, 0.22],
      [100, 149, 237, 0.22],
      [154, 205, 50, 0.22],
      [220, 20, 60, 0.22],
      [30, 144, 255, 0.22],
      [127, 255, 212, 0.22],
      [218, 112, 214, 0.22]
    ];

    var zonasCargadas = false;

    function cargarPoligonosLocales() {
      if (zonasCargadas) return;
      zonasCargadas = true;
      readTextFile("poligonos2.json", function(text) {
        try {
          var data = JSON.parse(text);
          for (var i = 0; i < data.length; i++) {
            var points = data[i]["coordinates"];
            var color = data[i]["color"] || [65, 105, 225, 0.22];
            var polygon = {
              type: "polygon",
              rings: points
            };
            var simpleFillSymbol = {
              type: "simple-fill",
              color: color,
              outline: {
                color: [160, 160, 160],
                width: 1
              }
            };
            var polygonGraphic = new Graphic({
              geometry: polygon,
              symbol: simpleFillSymbol
            });
            graphicsLayer.add(polygonGraphic);
          }
        } catch (e) {
          console.error("Error parseando poligonos2.json local:", e);
        }
      });
    }

    try {
      const zonasLayer = new FeatureLayer({
        url: "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0",
        outFields: ["nombre"],
        popupEnabled: false
      });

      zonasLayer.queryFeatures({
        where: "1=1",
        outFields: ["nombre"],
        returnGeometry: false
      }).then(function (result) {
        if (!result || !result.features || result.features.length === 0) {
          cargarPoligonosLocales();
          return;
        }

        const nombres = result.features
          .map(function (feature) {
            return feature.attributes.nombre;
          })
          .filter(Boolean);

        const nombresUnicos = Array.from(new Set(nombres));

        const uniqueValueInfos = nombresUnicos.map(function (nombre, index) {
          return {
            value: nombre,
            label: nombre,
            symbol: {
              type: "simple-fill",
              color: palette[index % palette.length],
              outline: {
                color: [160, 160, 160],
                width: 1
              }
            }
          };
        });

        zonasLayer.renderer = {
          type: "unique-value",
          field: "nombre",
          uniqueValueInfos: uniqueValueInfos
        };

        map.add(zonasLayer);
        zonasCargadas = true;
      }).catch(function (error) {
        console.warn("FeatureLayer de zonas no disponible, cargando poligonos2.json local:", error);
        cargarPoligonosLocales();
      });
    } catch (err) {
      console.warn("Error inicializando FeatureLayer, cargando poligonos locales:", err);
      cargarPoligonosLocales();
    }
  }

  function cargarTiendas(graphicsLayer, Graphic, markerSymbol, textTienda) {
    readTextFile("tiendas.json", function (text) {
      const tiendas = JSON.parse(text);

      tiendas.forEach(function (tienda) {
        const coordenadasTienda = tienda.coordinates;
        const coordenadasTexto = tienda.lugar_cercano;

        if (!coordenadasTienda || !coordenadasTexto) {
          return;
        }

        const markerGeometry = {
          type: "point",
          longitude: coordenadasTienda.lng,
          latitude: coordenadasTienda.lat
        };

        const markerGraphic = new Graphic({
          geometry: markerGeometry,
          symbol: markerSymbol,
          popupTemplate: {
            title: tienda.title
          }
        });

        graphicsLayer.add(markerGraphic);

        const textoGeometry = {
          type: "point",
          longitude: coordenadasTexto.lng,
          latitude: coordenadasTexto.lat
        };

        const textoSymbol = Object.assign({}, textTienda, {
          text: tienda.zona || ""
        });

        const textoGraphic = new Graphic({
          geometry: textoGeometry,
          symbol: textoSymbol
        });

        view.graphics.add(textoGraphic);
      });
    });
  }

  function configurarPopup() {
    view.popup = {
      autoOpenEnabled: false,
      dockEnabled: true,
      dockOptions: {
        position: "top-left",
        breakpoint: false
      }
    };
  }

  function configurarEventosMapa() {
    view.on("click", function (event) {
      view.hitTest(event).then(function (response) {
        const graphics = response.results.map(function (result) {
          return result.graphic;
        });

        // 1. Ignorar clics sobre iconos de tiendas
        const markerTiendaSeleccionado = graphics.some(function (graphic) {
          return graphic.symbol && graphic.symbol.type === "picture-marker" && graphic !== marker;
        });

        if (markerTiendaSeleccionado) {
          return;
        }

        // 2. Si se hace clic sobre el marcador del cliente o sobre su contorno brillante de selección:
        const markerClienteSeleccionado = graphics.some(function (graphic) {
          return graphic === marker ||
                 (markerHaloGraphicInner && graphic === markerHaloGraphicInner) ||
                 (markerHaloGraphicOuter && graphic === markerHaloGraphicOuter);
        });

        if (markerClienteSeleccionado) {
          // Alternar selección (si estaba apagado se prende y resalta; si estaba prendido se apaga)
          toggleModoMoverMarcador(!modoMoverMarcador);
          return;
        }

        // 3. Si NO está en modo mover marcador, IGNORAR cualquier clic en el mapa o polígonos.
        // Esto evita que al hacer clic se marque el polígono o se mueva el marcador por accidente.
        if (!modoMoverMarcador) {
          return;
        }

        // 4. Está en modo mover: ubicar en la nueva posición del mapa
        const punto = {
          type: "point",
          latitude: event.mapPoint.latitude,
          longitude: event.mapPoint.longitude
        };

        marker.geometry = punto;
        fijarCoordenadasManualmente(punto.latitude, punto.longitude);
        actualizarInfoCoordenadasUI(punto.latitude, punto.longitude);
        showAddress("Ubicación fijada en mapa", punto);
        geocodificarInversaNominatim(punto.latitude, punto.longitude);

        // Desactivar automáticamente el modo mover tras fijar la posición
        toggleModoMoverMarcador(false);
      });
    });
  }

  function configurarEventosFormulario() {
    $("#buscarmapa").on("click", function () {
      findAddress();
    });

    $("#descDireccion").on("keydown", function (event) {
      if (event.key === "Enter") {
        event.preventDefault();
        findAddress();
      }
    });
  }



});

function obtenerValor(selector) {
  const elemento = $(selector);

  if (!elemento.length) {
    return "";
  }

  return String(elemento.val() || "").trim();
}

function escaparHtml(valor) {
  return String(valor || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
function obtenerDireccionCobertura() {
  if ($("#validaDir").is(":checked")) {
    return obtenerValor("#descDireccion");
  }

  return obtenerValor("#direccion");
}

function obtenerTextoDireccionPedidoUI() {
  var dir = obtenerDireccionCobertura();
  var barrio = obtenerValor("#barrio");
  var mun = $('#selectMunicipio option:selected').text();
  if (mun === 'Seleccione...' || mun === 'Seleccione') mun = '';

  var texto = (dir || '').trim();
  if (barrio && texto && texto.toLowerCase().indexOf(barrio.toLowerCase()) === -1) {
    texto += ', ' + barrio;
  }
  if (mun && texto && texto.toLowerCase().indexOf(mun.toLowerCase()) === -1) {
    texto += ', ' + mun;
  }
  return texto;
}



function construirContenidoPopup(data, direccionMostrar) {
  const partes = [];

  partes.push("<strong>Direccion:</strong> " + escaparHtml(direccionMostrar));

if (data.proveedorGeocodificacion) {
  partes.push("<strong>Proveedor:</strong> " + escaparHtml(data.proveedorGeocodificacion));
}

  return partes.join("<br>");
}


function bloquearBusqueda(bloquear) {
  $("#buscarmapa").prop("disabled", bloquear);
  $("#loaderCobertura").toggle(bloquear);
  $("#buscarmapa").text(bloquear ? "Buscando..." : "Buscar");

  if (bloquear) {
    $("#buscarmapa").addClass("disabled");
  } else {
    $("#buscarmapa").removeClass("disabled");
  }
}



function coordenadasValidas(latitud, longitud) {
  const lat = Number(latitud);
  const lng = Number(longitud);

  return !Number.isNaN(lat) &&
    !Number.isNaN(lng) &&
    lat !== 0 &&
    lng !== 0 &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180;
}

function readTextFile(file, callback) {
  const rawFile = new XMLHttpRequest();

  rawFile.overrideMimeType("application/json");
  rawFile.open("GET", file, true);

  rawFile.onreadystatechange = function () {
    if (rawFile.readyState === 4 && rawFile.status === 200) {
      callback(rawFile.responseText);
    }
  };

  rawFile.send(null);
}

function obtenerUrlValidacionCobertura() {
  if (window.APP_CONTEXT_PATH) {
    return window.APP_CONTEXT_PATH + "/ValidacionCobertura";
  }

  return "ValidacionCobertura";
}

function mostrarMensaje(mensaje) {
  if (typeof Swal !== "undefined") {
    Swal.fire({
      toast: true,
      position: 'top',
      icon: 'warning',
      title: 'Validación de Cobertura',
      text: mensaje,
      showConfirmButton: true,
      confirmButtonText: 'Entendido',
      confirmButtonColor: '#e11d48',
      showCloseButton: true
    });
  } else if (typeof toastr !== "undefined") {
    toastr.warning(mensaje, "Validación de Cobertura", {
      timeOut: 0,
      extendedTimeOut: 0,
      closeButton: true
    });
  } else {
    alert(mensaje);
  }
}

async function findAddress(idcliente = null) {

      let coberturaRequest;

      if (idcliente) {
          coberturaRequest = { idcliente };
      } else {

          const direccion = obtenerDireccionCobertura();

          if (!direccion) {
              mostrarMensaje("Debe ingresar una dirección.");
              return;
          }

          coberturaRequest = {
              direccion,
              municipio: obtenerValor("#selectMunicipio"),
              barrio: obtenerValor("#barrio")
          };

          bloquearBusqueda(true);
      }

      try {

          const response = await fetch(URL_VALIDACION_COBERTURA, {
              method: "POST",
              headers: {
                  "Content-Type": "application/json;charset=UTF-8"
              },
              body: JSON.stringify(coberturaRequest)
          });

          const data = await response.json();
         
          if (!response.ok || !data.success) {
              mostrarMensaje(data.resultado || "No fue posible validar la cobertura.");
              var dirActual = obtenerTextoDireccionPedidoUI();
              if (dirActual) {
                  $('#mapaTxtDireccionRegistrada').text(dirActual);
              }
              $('#mapaTxtDireccionDetectada').html('<span style="color:#d97706;"><i class="fas fa-hand-pointer"></i> Haz clic en el mapa para ubicar el punto del cliente</span>');
              return;
          }

          if (!coordenadasValidas(data.latitud, data.longitud)) {
              mostrarMensaje("El servicio no devolvió coordenadas válidas.");
              return;
          }

          const punto = {
              type: "point",
              latitude: Number(data.latitud),
              longitude: Number(data.longitud)
          };

          const direccionMostrar =
              data.direccion ??
              data.direccionCorregida ??
              data.direccionOriginalNormalizada;

          showAddress(
              construirContenidoPopup(data, direccionMostrar),
              punto
          );

          fijarCoordenadasManualmente(
              punto.latitude,
              punto.longitude
          );

          actualizarInfoCoordenadasUI(punto.latitude, punto.longitude);

          // La "Dirección Registrada del Pedido" siempre debe ser la dirección digitada en el formulario del pedido
          var dirRegistrada = obtenerTextoDireccionPedidoUI();
          if (!dirRegistrada) {
              dirRegistrada = data.direccionOriginalNormalizada || (data.direccion && data.direccion.length < 90 ? data.direccion : '') || 'Sin dirección especificada';
          }
          $('#mapaTxtDireccionRegistrada').text(dirRegistrada);
          if (data.infoAdicional) {
              $('#mapaTxtTiendaCobertura').text(data.infoAdicional);
          } else {
              var tText = $('#selectTiendas option:selected').text();
              if (tText) $('#mapaTxtTiendaCobertura').text(tText);
          }
          geocodificarInversaNominatim(punto.latitude, punto.longitude);

      } catch (error) {
          console.error("Error validando cobertura:", error);
          mostrarMensaje("Error consultando el servicio de cobertura.");
      } finally {
          if (!idcliente) {
              bloquearBusqueda(false);
          }
      }
  }

  
function showAddress(address, location) {
  marker.geometry = location;
  if (markerHaloGraphicOuter) {
    markerHaloGraphicOuter.geometry = location;
  }
  if (markerHaloGraphicInner) {
    markerHaloGraphicInner.geometry = location;
  }

  // Cerrar cualquier popup flotante sobre el mapa para no tapar la vista ni duplicar coordenadas
  if (view && view.popup) {
    view.popup.close();
  }

  view.goTo({
    center: [location.longitude, location.latitude],
    zoom: 17
  }, {
    duration: 800
  });

  var mapa = document.getElementById("map");
  if (mapa) {
    mapa.scrollIntoView({
      behavior: "smooth",
      block: "center"
    });
  }
}

function redondear(numero) {
  return Math.round(Number(numero) * 100000) / 100000;
}

function clarearMapa() {
  const puntoInicial = {
    longitude: -75.56359,
    latitude: 6.25184,
    type: "point"
  };

  marker.geometry = puntoInicial;

  view.goTo({
    center: [puntoInicial.longitude, puntoInicial.latitude]
  });

  toggleModoMoverMarcador(false);
  $('#mapaTxtDireccionRegistrada').text('Sin dirección especificada');
  $('#mapaTxtDireccionDetectada').text('Haga clic en [Mover Marcador] o sobre el marcador para reubicar...');
  $('#btnCopiarDireccionMapa').hide();
  $('#mapaTxtTiendaCobertura').text('-');
  actualizarInfoCoordenadasUI(0, 0);
}

function toggleModoMoverMarcador(forzarEstado) {
  if (typeof forzarEstado === 'boolean') {
    modoMoverMarcador = forzarEstado;
  } else {
    modoMoverMarcador = !modoMoverMarcador;
  }

  var $btn = $('#btnModoMoverMarcador');
  var $txt = $('#txtBtnMoverMarcador');
  var $mapDiv = $('#map');

  if (modoMoverMarcador) {
    $btn.addClass('activo btn-warning').removeClass('btn-default');
    $txt.html('<b><i class="fas fa-crosshairs"></i> Mover activo (clic en mapa)</b>');
    $mapDiv.addClass('modo-mover-activo');

    // Resaltar sutilmente con un contorno brillante alrededor del marcador sin alterar el icono original
    if (marker && marker.geometry && view) {
      if (SimpleMarkerSymbolRef && GraphicRef) {
        // 1. Halo exterior suave tipo resplandor azul eléctrico
        var haloOuterSym = new SimpleMarkerSymbolRef({
          style: "circle",
          color: [37, 99, 235, 0.15],
          size: 32,
          outline: {
            color: [59, 130, 246, 0.55],
            width: 1.5
          },
          yoffset: 12
        });

        // 2. Borde interior nítido pegado al contorno exacto del marcador (24px)
        var haloInnerSym = new SimpleMarkerSymbolRef({
          style: "circle",
          color: [0, 0, 0, 0],
          size: 26,
          outline: {
            color: [30, 64, 175, 0.95],
            width: 2.2
          },
          yoffset: 12
        });

        if (!markerHaloGraphicOuter) {
          markerHaloGraphicOuter = new GraphicRef({
            geometry: marker.geometry,
            symbol: haloOuterSym
          });
          view.graphics.add(markerHaloGraphicOuter);
        } else {
          markerHaloGraphicOuter.geometry = marker.geometry;
          markerHaloGraphicOuter.symbol = haloOuterSym;
          markerHaloGraphicOuter.visible = true;
        }

        if (!markerHaloGraphicInner) {
          markerHaloGraphicInner = new GraphicRef({
            geometry: marker.geometry,
            symbol: haloInnerSym
          });
          view.graphics.add(markerHaloGraphicInner);
        } else {
          markerHaloGraphicInner.geometry = marker.geometry;
          markerHaloGraphicInner.symbol = haloInnerSym;
          markerHaloGraphicInner.visible = true;
        }
      }
    }
  } else {
    $btn.removeClass('activo btn-warning').addClass('btn-default');
    $txt.text('Mover Marcador');
    $mapDiv.removeClass('modo-mover-activo');

    // Ocultar contornos brillantes
    if (markerHaloGraphicOuter) {
      markerHaloGraphicOuter.visible = false;
    }
    if (markerHaloGraphicInner) {
      markerHaloGraphicInner.visible = false;
    }
  }
}

function cambiarBasemapArcgis(modo) {
  if (!mapArcgis) return;
  mapArcgis.basemap = modo;
  if (modo === 'hybrid') {
    $('#btnMapaBasemapSatelite').addClass('active');
    $('#btnMapaBasemapCalles').removeClass('active');
  } else {
    $('#btnMapaBasemapCalles').addClass('active');
    $('#btnMapaBasemapSatelite').removeClass('active');
  }
}

function actualizarInfoCoordenadasUI(lat, lng) {
  if (lat && lng && Number(lat) !== 0 && Number(lng) !== 0) {
    var latR = Number(lat).toFixed(6);
    var lngR = Number(lng).toFixed(6);
    $('#mapaTxtCoordenadas').text('Lat: ' + latR + ', Lng: ' + lngR);
    $('#btnMapaStreetView').attr('href', 'https://www.google.com/maps?q=' + lat + ',' + lng).show();
  } else {
    $('#mapaTxtCoordenadas').text('Lat: 0.000000, Lng: 0.000000');
    $('#btnMapaStreetView').hide();
  }
}

function geocodificarInversaNominatim(lat, lng) {
  var $txtDetectada = $('#mapaTxtDireccionDetectada');
  var $btnCopiar = $('#btnCopiarDireccionMapa');

  $txtDetectada.html('<span class="badge-geocodificando"><i class="fas fa-spinner fa-spin"></i> Detectando dirección en mapa...</span>');
  $btnCopiar.hide();

  var url = 'https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&zoom=18&addressdetails=1';

  fetch(url, {
    headers: { 'Accept-Language': 'es' }
  })
  .then(function(res) { return res.json(); })
  .then(function(data) {
    if (data && data.address) {
      ultimaDireccionDetectada = data;
      var addr = data.address;
      var calle = addr.road || addr.pedestrian || addr.cycleway || addr.path || '';
      var numero = addr.house_number || '';
      var barrio = addr.neighbourhood || addr.suburb || addr.residential || '';
      var municipio = addr.city || addr.town || addr.municipality || 'Medellín';

      var textoMostrar = '';
      if (calle && numero) {
        textoMostrar = calle + ' # ' + numero;
      } else if (calle) {
        textoMostrar = calle;
      } else if (data.display_name) {
        textoMostrar = data.display_name.split(',')[0];
      }

      if (barrio && textoMostrar.indexOf(barrio) === -1) {
        textoMostrar += ', ' + barrio;
      }
      if (municipio && textoMostrar.indexOf(municipio) === -1) {
        textoMostrar += ', ' + municipio;
      }

      $txtDetectada.html('<i class="fas fa-check-circle text-success"></i> ' + escaparHtml(textoMostrar));
      $btnCopiar.show();

      if (view && view.popup && view.popup.visible) {
        view.popup.content = '<strong>Punto fijado:</strong><br>' + escaparHtml(textoMostrar);
      }
    } else {
      $txtDetectada.text('Ubicación fijada en coordenadas exactas');
      $btnCopiar.hide();
    }
  })
  .catch(function(err) {
    console.warn('Error en geocodificación inversa:', err);
    $txtDetectada.text('Lat: ' + redondear(lat) + ', Lng: ' + redondear(lng));
    $btnCopiar.hide();
  });
}

function aplicarDireccionDetectadaAlFormulario() {
  if (!ultimaDireccionDetectada || !ultimaDireccionDetectada.address) {
    alert('No hay una dirección detectada en el mapa para aplicar.');
    return;
  }

  var addr = ultimaDireccionDetectada.address;
  var road = (addr.road || addr.pedestrian || addr.cycleway || addr.path || '').trim();
  var houseNumber = (addr.house_number || '').trim();
  var barrio = addr.neighbourhood || addr.suburb || addr.residential || '';
  var municipio = addr.city || addr.town || addr.municipality || '';

  // 1. Construir texto legible de la dirección detectada
  var direccionCompleta = '';
  if (road && houseNumber) {
    direccionCompleta = road + ' # ' + houseNumber;
  } else if (road) {
    direccionCompleta = road;
  } else if (ultimaDireccionDetectada.display_name) {
    direccionCompleta = ultimaDireccionDetectada.display_name.split(',').slice(0, 3).join(', ');
  }

  if (barrio && direccionCompleta.indexOf(barrio) === -1) {
    direccionCompleta += ', ' + barrio;
  }
  if (municipio && direccionCompleta.indexOf(municipio) === -1) {
    direccionCompleta += ', ' + municipio;
  }

  // 2. Asignar siempre la dirección detectada al campo de Dirección Libre (#direccion)
  if (direccionCompleta) {
    $('#direccion').val(direccionCompleta);
    $('#mapaTxtDireccionRegistrada').text(direccionCompleta);
  }

  // 3. Intentar descomponer vía: "Calle 10", "Carrera 79", etc.
  var matchVia = road.match(/^(calle|carrera|avenida|diagonal|transversal|circular|autopista)\s*(\d+[a-zA-Z]?)/i);
  if (matchVia) {
    var tipoVia = matchVia[1].toUpperCase();
    var numVia = matchVia[2];

    $('#selectNomenclaturas option').each(function() {
      var optText = $(this).text().toUpperCase().trim();
      var optVal = ($(this).val() || '').toUpperCase().trim();
      if (optText.startsWith(tipoVia) || optVal.startsWith(tipoVia)) {
        $(this).prop('selected', true);
        return false;
      }
    });

    $('#numNomen').val(numVia);
  } else if (road) {
    if (!$('#numNomen').val()) {
      $('#numNomen').val(road);
    }
  }

  // 4. Si OSM trae número de casa (ej: "40-20" o "40 # 20"), descomponer en cruce y placa
  if (houseNumber) {
    var partes = houseNumber.split(/[-#\s]+/);
    if (partes.length >= 2) {
      $('#numNomen2').val(partes[0]);
      $('#num3').val(partes[1]);
    } else if (partes.length === 1 && partes[0]) {
      $('#num3').val(partes[0]);
    }
  }
  // NOTA: Si numNomen2 o num3 ya tenían datos en el formulario, NO se borran ni se limpian.

  // 5. Asignar municipio si coincide
  if (municipio) {
    $('#selectMunicipio option').each(function() {
      if ($(this).text().toLowerCase().indexOf(municipio.toLowerCase()) !== -1) {
        $(this).prop('selected', true);
        return false;
      }
    });
  }

  // 6. Validar si los 3 componentes de nomenclatura están llenos
  var nTipo = ($('#selectNomenclaturas').val() || '').trim();
  var n1 = ($('#numNomen').val() || '').trim();
  var n2 = ($('#numNomen2').val() || '').trim();
  var n3 = ($('#num3').val() || '').trim();

  if (nTipo && n1 && n2 && n3) {
    // Nomenclatura completa -> generar descripción estructurada
    if (typeof descripcionDireccion === 'function') {
      descripcionDireccion();
    }
    $('#validaDir').prop('checked', true);

    if (typeof Swal !== 'undefined') {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'success',
        title: '¡Dirección estructurada aplicada!',
        text: $('#descDireccion').val() || direccionCompleta,
        showConfirmButton: true,
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#10b981',
        showCloseButton: true
      });
    } else if (typeof toastr !== 'undefined') {
      toastr.success('Dirección estructurada aplicada al formulario', '¡Ubicación Sincronizada!', { timeOut: 0, closeButton: true });
    }
  } else {
    // Si falta cruce o placa (ej: OSM solo conoce el nombre de la vía como "Carrera 79"):
    // Desactivamos temporalmente validaDir para que descDireccion NO quede en rojo con error
    $('#validaDir').prop('checked', false);
    $('#descDireccion').removeClass('is-invalid').attr('placeholder', 'Vía detectada: use Dirección Libre o complete Cruce y Placa');

    if (typeof Swal !== 'undefined') {
      Swal.fire({
        toast: true,
        position: 'top',
        icon: 'info',
        title: 'Dirección fijada en el pedido',
        html: '<div style="text-align:left; font-size:12.5px; line-height:1.45;">' +
              '📍 <b>Vía fijada:</b> ' + direccionCompleta + '<br>' +
              '✅ Se guardó en <b>Dirección Libre</b>.<br><br>' +
              '💡 <b>¿Deseas enviarla por nomenclatura detallada?</b><br>' +
              'Por favor escribe el <b>Cruce (#)</b> y la <b>Placa (-)</b> en las casillas de arriba.' +
              '</div>',
        showConfirmButton: true,
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#2563eb',
        showCloseButton: true
      });
    } else if (typeof toastr !== 'undefined') {
      toastr.info('Vía asignada en Dirección Libre. Complete Cruce y Placa si requiere nomenclatura.', 'Dirección Aplicada', { timeOut: 0, closeButton: true });
    }
  }
}
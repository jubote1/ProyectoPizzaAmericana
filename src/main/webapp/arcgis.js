var marker = null;
var view = null;

const URL_VALIDACION_COBERTURA = obtenerUrlValidacionCobertura();

require([
  "esri/config",
  "esri/Map",
  "esri/views/MapView",
  "esri/layers/GraphicsLayer",
  "esri/Graphic",
  "esri/symbols/PictureMarkerSymbol",
  "esri/layers/FeatureLayer"
], function (
  esriConfig,
  Map,
  MapView,
  GraphicsLayer,
  Graphic,
  PictureMarkerSymbol,
  FeatureLayer
) {
  "use strict";

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

  view = new MapView({
    map: map,
    center: [CENTRO_MAPA.longitude, CENTRO_MAPA.latitude],
    zoom: 13,
    container: "map"
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

  cargarZonas(map, FeatureLayer);
  cargarTiendas(graphicsLayer, Graphic, markerSymbol, textTienda);
  configurarPopup();
  configurarEventosMapa();
  configurarEventosFormulario();


  function cargarZonas(map, FeatureLayer) {
    const palette = [
      [255, 99, 71, 0.1],
      [60, 179, 113, 0.1],
      [65, 105, 225, 0.1],
      [238, 130, 238, 0.1],
      [255, 165, 0, 0.1],
      [100, 149, 237, 0.1],
      [154, 205, 50, 0.1],
      [220, 20, 60, 0.1],
      [30, 144, 255, 0.1],
      [127, 255, 212, 0.1],
      [218, 112, 214, 0.1]
    ];

    const zonasLayer = new FeatureLayer({
      url: "https://services1.arcgis.com/PezsEKOq8AU6Mcbj/arcgis/rest/services/zonas/FeatureServer/0",
      outFields: ["nombre"],
      popupTemplate: {
        title: "{nombre}",
        content: "Zona: {nombre}"
      }
    });

    zonasLayer.queryFeatures({
      where: "1=1",
      outFields: ["nombre"],
      returnGeometry: false
    }).then(function (result) {
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
              color: [194, 194, 194],
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
    }).catch(function (error) {
      console.error("Error cargando zonas:", error);
    });
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

        const markerTiendaSeleccionado = graphics.some(function (graphic) {
          return graphic.symbol && graphic.symbol.type === "picture-marker";
        });

        if (markerTiendaSeleccionado) {
          return;
        }

        const punto = {
          type: "point",
          latitude: event.mapPoint.latitude,
          longitude: event.mapPoint.longitude
        };

        fijarCoordenadasManualmente(punto.latitude, punto.longitude);
        showAddress("Ubicacion seleccionada manualmente", punto);
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
  alert(mensaje);
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

  view.goTo({
    center: [location.longitude, location.latitude],
    zoom: 17
  }, {
    duration: 800
  }).then(function () {
    view.openPopup({
      title: redondear(location.latitude) + ", " + redondear(location.longitude),
      content: address,
      location: location
    });
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
}
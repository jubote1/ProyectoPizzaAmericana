var marker = null;
var view = null;
var mapArcgis = null;
var ultimaDireccionDetectada = null;
var ultimaDireccionSugeridaServicio = null;
var modoMoverMarcador = false;
var markerClientNormal = null;
var markerClientSeleccionado = null;
var markerHaloGraphicOuter = null;
var markerHaloGraphicInner = null;
var SimpleMarkerSymbolRef = null;
var GraphicRef = null;

const URL_VALIDACION_COBERTURA = obtenerUrlValidacionCobertura();
const ARCGIS_API_KEY = "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";

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

  esriConfig.apiKey = ARCGIS_API_KEY;

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
        ultimaDireccionSugeridaServicio = null;
        geocodificarInversaArcGIS(punto.latitude, punto.longitude);

        // Desactivar automáticamente el modo mover tras fijar la posición
        toggleModoMoverMarcador(false);
      });
    });
  }

  function configurarEventosFormulario() {
    $("#buscarmapa, #btnBuscarDirLibre, .btn-buscar-global").on("click", function () {
      findAddress();
    });

    $("#descDireccion, #direccion").on("keydown", function (event) {
      if (event.key === "Enter") {
        event.preventDefault();
        findAddress();
      }
    });

    // Sincronizar modo visual (Libre vs Nomenclatura) al cambiar el checkbox
    $("#validaDir").on("change", function () {
      actualizarModoDireccionUI();
    });

    // Actualizar en tiempo real Card 1 con la dirección que se guardará
    $("#direccion, #zona, #descDireccion, #numNomen, #numNomen2, #num3").on("input change", function () {
      actualizarTarjetaDireccionPedidoUI();
    });

    // Detectar municipio automáticamente al escribir en Dirección Libre (ej: "bello")
    $("#direccion").on("input change", function () {
      detectarYSincronizarMunicipio($(this).val());
      actualizarTarjetaDireccionPedidoUI();
    });

    $("#selectMunicipio, #selectNomenclaturas").on("change", function () {
      actualizarTarjetaDireccionPedidoUI();
    });

    // Inicializar modo visual de direcciones
    setTimeout(function () {
      actualizarModoDireccionUI();
    }, 200);
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
function limpiarTextoDireccionUI(txt) {
  if (!txt) return "";
  return String(txt)
    .replace(/\b0\d{5}\b|\b\d{6}\b/g, "") // Remover códigos postales como 050025
    .replace(/,\s*colombia\b/gi, "")      // Remover ", Colombia"
    .replace(/\s+/g, " ")
    .replace(/,\s*,+/g, ",")
    .replace(/^[,\s-]+|[,\s-]+$/g, "")
    .trim();
}

function normalizarTextoParaComparar(txt) {
  if (!txt) return "";
  return String(txt)
    .toLowerCase()
    .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
    .replace(/[^\w\s]/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function compararDireccionesPedidoYMapa(dirPedido, dirDetectada) {
  if (!dirPedido || !dirDetectada) return { coincide: false };

  var p1 = normalizarTextoParaComparar(dirPedido);
  var p2 = normalizarTextoParaComparar(dirDetectada);

  var numsPedido = p1.match(/\d+/g) || [];
  var numsDetectada = p2.match(/\d+/g) || [];

  if (numsPedido.length >= 2 && numsDetectada.length >= 2) {
    if (numsPedido[0] === numsDetectada[0] && numsPedido[1] === numsDetectada[1]) {
      return { coincide: true, tipo: "exacto" };
    }
  }

  if (numsPedido.length >= 1 && numsDetectada.length >= 1) {
    if (numsPedido[0] === numsDetectada[0]) {
      return { coincide: true, tipo: "via" };
    }
  }

  var vias = ["calle", "carrera", "circular", "diagonal", "transversal", "avenida"];
  for (var i = 0; i < vias.length; i++) {
    var v = vias[i];
    if (p1.indexOf(v) !== -1 && p2.indexOf(v) !== -1) {
      for (var j = 0; j < numsPedido.length; j++) {
        if (numsDetectada.indexOf(numsPedido[j]) !== -1) {
          return { coincide: true, tipo: "parcial" };
        }
      }
    }
  }

  return { coincide: false, tipo: "diferente" };
}

function detectarYSincronizarMunicipio(texto) {
  if (!texto) return "";
  var municipios = [
    { nombre: 'Medellín', aliases: ['medellin'] },
    { nombre: 'Bello', aliases: ['bello'] },
    { nombre: 'Itagüí', aliases: ['itagui', 'itaguei'] },
    { nombre: 'Envigado', aliases: ['envigado'] },
    { nombre: 'Sabaneta', aliases: ['sabaneta'] },
    { nombre: 'Copacabana', aliases: ['copacabana'] },
    { nombre: 'La Estrella', aliases: ['la estrella', 'estrella'] },
    { nombre: 'Caldas', aliases: ['caldas'] },
    { nombre: 'Girardota', aliases: ['girardota'] },
    { nombre: 'Barbosa', aliases: ['barbosa'] },
    { nombre: 'Rionegro', aliases: ['rionegro'] }
  ];

  var tNorm = texto.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

  for (var i = 0; i < municipios.length; i++) {
    var item = municipios[i];
    for (var j = 0; j < item.aliases.length; j++) {
      var regex = new RegExp('\\b' + item.aliases[j] + '\\b', 'i');
      if (regex.test(tNorm)) {
        $('#selectMunicipio option').each(function() {
          var optNorm = $(this).text().toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
          var itemNorm = item.nombre.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
          if (optNorm === itemNorm || optNorm === item.aliases[0]) {
            if (!$(this).is(':selected')) {
              $(this).prop('selected', true);
            }
            return false;
          }
        });
        return item.nombre;
      }
    }
  }
  return "";
}

function obtenerDireccionCobertura() {
  if ($("#validaDir").is(":checked")) {
    return obtenerValor("#descDireccion");
  }

  return obtenerValor("#direccion");
}

function obtenerTextoDireccionPedidoUI() {
  var usaNomenclatura = $("#validaDir").is(":checked");
  var dir = "";

  if (usaNomenclatura) {
    dir = obtenerValor("#descDireccion");
    if (!dir && typeof formarDireccionEstructurada === "function") {
      dir = formarDireccionEstructurada(false) || "";
    }
  } else {
    dir = obtenerValor("#direccion");
  }

  var texto = (dir || "").trim();
  var complemento = obtenerValor("#zona") || obtenerValor("#barrio");

  // Detectar si el texto ya contiene el nombre de algún municipio (ej: "calle 20e #71-60, bello")
  var muniDetectado = detectarYSincronizarMunicipio(texto);

  if (usaNomenclatura) {
    // Si usa nomenclatura estructurada y no tiene el municipio en el texto, agregar el seleccionado
    var mun = $('#selectMunicipio option:selected').text();
    if (mun === 'Seleccione...' || mun === 'Seleccione' || mun === 'null' || !mun) {
      mun = '';
    }
    if (mun && texto && !muniDetectado && texto.toLowerCase().indexOf(mun.toLowerCase()) === -1) {
      texto += ', ' + mun;
    }
  }
  // En Dirección Libre: NO se añade ningún municipio por defecto.
  // El usuario es dueño de su texto y solo se muestra y guarda lo que escribe.

  if (complemento && texto && texto.toLowerCase().indexOf(complemento.toLowerCase()) === -1) {
    texto += ' (' + complemento + ')';
  }

  return limpiarTextoDireccionUI(texto);
}

function actualizarModoDireccionUI() {
  var usaNomenclatura = $("#validaDir").is(":checked");
  if (usaNomenclatura) {
    $("#filaDireccionNomenclatura").addClass("modo-activo").removeClass("modo-inactivo");
    $("#filaDireccionLibre").addClass("modo-inactivo").removeClass("modo-activo");
    $("#badgeModoNomenclatura").show();
    $("#badgeModoLibre").hide();
    $("#mapaBadgeOrigenDireccion").removeClass("label-warning").addClass("label-info").text("Nomenclatura");

    // Si nomenclatura está activa, el botón de buscar aparece SOLO en nomenclatura
    $("#buscarmapa").show();
    $("#btnBuscarDirLibre").hide();
  } else {
    $("#filaDireccionLibre").addClass("modo-activo").removeClass("modo-inactivo");
    $("#filaDireccionNomenclatura").addClass("modo-inactivo").removeClass("modo-activo");
    $("#badgeModoLibre").show();
    $("#badgeModoNomenclatura").hide();
    $("#mapaBadgeOrigenDireccion").removeClass("label-info").addClass("label-warning").text("Dirección Libre");

    // Si dirección libre está activa, el botón de buscar aparece SOLO en dirección libre
    $("#btnBuscarDirLibre").show();
    $("#buscarmapa").hide();
  }
  actualizarTarjetaDireccionPedidoUI();
}

function actualizarTarjetaDireccionPedidoUI() {
  var dir = obtenerTextoDireccionPedidoUI();
  if (dir) {
    $("#mapaTxtDireccionRegistrada").text(dir);
  } else {
    $("#mapaTxtDireccionRegistrada").text("Sin dirección especificada");
  }
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
  var $btns = $("#buscarmapa, #btnBuscarDirLibre, .btn-buscar-global");
  $btns.prop("disabled", bloquear);
  $("#loaderCobertura").toggle(bloquear);

  if (bloquear) {
    $btns.addClass("disabled").html('<i class="fas fa-spinner fa-spin"></i> Buscando...');
  } else {
    $btns.removeClass("disabled").html('<i class="fas fa-search"></i> Buscar');
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
              barrio: obtenerValor("#zona") || obtenerValor("#barrio")
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
              if (marker && marker.geometry && marker.geometry.latitude && marker.geometry.longitude) {
                  geocodificarInversaArcGIS(marker.geometry.latitude, marker.geometry.longitude);
              } else {
                  $('#mapaTxtDireccionDetectada').html('<span style="color:#d97706;"><i class="fas fa-hand-pointer"></i> Haz clic en el mapa para ubicar el punto del cliente</span>');
                  $('#mapaBadgeCoincidencia').hide();
              }
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

          // Si el geocodificador corrigió el municipio (ej: el usuario seleccionó Medellín pero la vía pertenece a Bello):
          if (data.municipioCorregido) {
              detectarYSincronizarMunicipio(data.municipioCorregido);
          }

          // Card 1: Dirección exacta del pedido a guardar en base de datos
          actualizarTarjetaDireccionPedidoUI();

          if (data.infoAdicional) {
              $('#mapaTxtTiendaCobertura').text(data.infoAdicional);
          } else {
              var tText = $('#selectTiendas option:selected').text();
              if (tText) $('#mapaTxtTiendaCobertura').text(tText);
          }

          // Card 2: Mostrar directamente la dirección sugerida/corregida por el servicio (HERE / Google)
          var dirServicio = data.direccionCorregida || data.direccion;
          if (dirServicio) {
              mostrarDireccionSugeridaServicio(dirServicio, data.proveedorGeocodificacion);
          } else {
              var fallbackTexto = limpiarTextoDireccionUI(obtenerTextoDireccionPedidoUI());
              geocodificarInversaArcGIS(punto.latitude, punto.longitude, fallbackTexto);
          }

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
    zoom: 16
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
  $('#mapaBadgeCoincidencia').hide();
  $('#btnCopiarDireccionMapa').hide();
  $('#mapaTxtTiendaCobertura').text('-');
  actualizarInfoCoordenadasUI(0, 0);
  actualizarModoDireccionUI();
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

function mostrarDireccionSugeridaServicio(direccion, proveedor) {
  var $txtDetectada = $('#mapaTxtDireccionDetectada');
  var $btnCopiar = $('#btnCopiarDireccionMapa');
  var $badgeCoincide = $('#mapaBadgeCoincidencia');

  var textoLimpio = limpiarTextoDireccionUI(direccion);
  ultimaDireccionSugeridaServicio = textoLimpio;
  ultimaDireccionDetectada = null;

  $txtDetectada.html('<i class="fas fa-map-marker-alt text-success"></i> ' + escaparHtml(textoLimpio));

  var dirPedido = obtenerTextoDireccionPedidoUI();
  var comp = compararDireccionesPedidoYMapa(dirPedido, textoLimpio);

  var badgeProv = proveedor ? ('<span class="label label-info" style="margin-right: 4px;"><i class="fas fa-satellite"></i> ' + escaparHtml(proveedor) + '</span>') : '';

  if (comp.coincide) {
    $badgeCoincide.html(badgeProv + '<span class="label label-success"><i class="fas fa-check-double"></i> Coincide con pedido</span>').show();
  } else {
    $badgeCoincide.html(badgeProv + '<span class="label label-warning" style="background-color: #f59e0b;"><i class="fas fa-map-pin"></i> Sugerida por servicio</span>').show();
  }

  if (dirPedido.toLowerCase().trim() !== textoLimpio.toLowerCase().trim()) {
    $btnCopiar.html('<i class="fas fa-check-circle"></i> Usar esta dirección en el pedido').show();
  } else {
    $btnCopiar.hide();
  }
}

function formatearDireccionColombianaArcGIS(addr) {
  if (!addr) return { texto: '', direccionBase: '', road: '', houseNumber: '', barrio: '', municipio: '' };

  var rawAddress = (addr.Address || addr.ShortLabel || '').trim();
  var matchAddr = (addr.Match_addr || '').trim();
  var addNum = (addr.AddNum || '').trim();
  var barrio = (addr.Neighborhood || addr.District || addr.Subregion || '').trim();
  var municipio = (addr.City || 'Medellín').trim();
  var road = '';
  var houseNumber = '';
  var direccionFormateada = '';

  if (addr.Addr_type === 'POI' && (addr.PlaceName || addr.ShortLabel)) {
    direccionFormateada = (addr.PlaceName || addr.ShortLabel).trim();
    road = direccionFormateada;
  } else if (rawAddress) {
    if (rawAddress.includes('#') || rawAddress.includes('-')) {
      direccionFormateada = rawAddress;
    } else if (addNum && rawAddress.endsWith(addNum)) {
      var resto = rawAddress.substring(0, rawAddress.length - addNum.length).trim();
      var ultEspacio = resto.lastIndexOf(' ');
      if (ultEspacio > 0) {
        road = resto.substring(0, ultEspacio).trim();
        var cruce = resto.substring(ultEspacio + 1).trim();
        houseNumber = cruce + '-' + addNum;
        direccionFormateada = road + ' # ' + cruce + '-' + addNum;
      } else {
        road = resto;
        houseNumber = addNum;
        direccionFormateada = road + ' # ' + addNum;
      }
    } else {
      var matchNomen = rawAddress.match(/^((?:calle|carrera|cra|cr|cl|avenida|av|diagonal|diag|dg|transversal|trans|tv|circular|autopista)\s*\d+[a-z]?)\s+(\d+[a-z]?)\s+(\d+[a-z]?)$/i);
      if (matchNomen) {
        road = matchNomen[1];
        houseNumber = matchNomen[2] + '-' + matchNomen[3];
        direccionFormateada = road + ' # ' + houseNumber;
      } else {
        direccionFormateada = rawAddress;
        road = rawAddress;
      }
    }
  } else if (matchAddr) {
    direccionFormateada = matchAddr.split(',')[0].trim();
    road = direccionFormateada;
  }

  var textoCompleto = direccionFormateada;
  if (barrio && textoCompleto.indexOf(barrio) === -1) {
    textoCompleto += ', ' + barrio;
  }
  if (municipio && textoCompleto.indexOf(municipio) === -1) {
    textoCompleto += ', ' + municipio;
  }

  return {
    texto: textoCompleto,
    direccionBase: direccionFormateada,
    road: road,
    houseNumber: houseNumber,
    barrio: barrio,
    municipio: municipio
  };
}

function geocodificarInversaArcGIS(lat, lng, fallbackTexto) {
  var $txtDetectada = $('#mapaTxtDireccionDetectada');
  var $btnCopiar = $('#btnCopiarDireccionMapa');
  var $badgeCoincide = $('#mapaBadgeCoincidencia');

  $txtDetectada.html('<span class="badge-geocodificando"><i class="fas fa-spinner fa-spin"></i> Detectando dirección en mapa...</span>');
  $btnCopiar.hide();
  $badgeCoincide.hide();

  var url = 'https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?location=' +
            lng + ',' + lat + '&f=json&token=' + encodeURIComponent(ARCGIS_API_KEY);

  fetch(url)
    .then(function(res) { return res.json(); })
    .then(function(data) {
      var textoMostrar = '';
      if (data && data.address) {
        var info = formatearDireccionColombianaArcGIS(data.address);
        ultimaDireccionDetectada = {
          address: data.address,
          display_name: info.texto || data.address.Match_addr,
          infoFormateada: info,
          raw: data
        };

        textoMostrar = info.texto;
        if (info.municipio) {
          detectarYSincronizarMunicipio(info.municipio);
        }
      }

      if (!textoMostrar && fallbackTexto) {
        textoMostrar = fallbackTexto;
      }

      if (textoMostrar) {
        textoMostrar = limpiarTextoDireccionUI(textoMostrar);
        $txtDetectada.html('<i class="fas fa-map-marker-alt text-danger"></i> ' + escaparHtml(textoMostrar));

        var dirPedido = obtenerTextoDireccionPedidoUI();
        var comp = compararDireccionesPedidoYMapa(dirPedido, textoMostrar);

        if (comp.coincide) {
          $badgeCoincide.html('<span class="label label-success"><i class="fas fa-check-double"></i> Coincide con pedido</span>').show();
          $btnCopiar.hide();
        } else {
          $badgeCoincide.html('<span class="label label-default" style="color: #475569; background-color: #f1f5f9; border: 1px solid #cbd5e1;"><i class="fas fa-map-marker-alt text-danger"></i> Punto en mapa</span>').show();
          $btnCopiar.show();
        }

        if (view && view.popup && view.popup.visible) {
          view.popup.content = '<strong>Punto fijado:</strong><br>' + escaparHtml(textoMostrar);
        }
      } else {
        $txtDetectada.text('Lat: ' + redondear(lat) + ', Lng: ' + redondear(lng));
        $btnCopiar.hide();
        $badgeCoincide.hide();
      }
    })
    .catch(function(err) {
      console.warn('Error en geocodificación inversa ArcGIS:', err);
      var textoRespaldo = fallbackTexto ? limpiarTextoDireccionUI(fallbackTexto) : ('Lat: ' + redondear(lat) + ', Lng: ' + redondear(lng));
      $txtDetectada.html('<i class="fas fa-map-marker-alt text-danger"></i> ' + escaparHtml(textoRespaldo));
      $btnCopiar.hide();
      $badgeCoincide.hide();
    });
}

// Alias de retrocompatibilidad
function geocodificarInversaNominatim(lat, lng, fallbackTexto) {
  return geocodificarInversaArcGIS(lat, lng, fallbackTexto);
}

function aplicarDireccionDetectadaAlFormulario() {
  var direccionAplicar = ultimaDireccionSugeridaServicio || "";
  var road = "";
  var houseNumber = "";
  var barrio = "";
  var municipio = "";

  if (ultimaDireccionDetectada) {
    if (ultimaDireccionDetectada.infoFormateada) {
      var info = ultimaDireccionDetectada.infoFormateada;
      if (!direccionAplicar) {
        direccionAplicar = info.texto || info.direccionBase;
      }
      road = info.road;
      houseNumber = info.houseNumber;
      barrio = info.barrio;
      municipio = info.municipio;
    } else if (ultimaDireccionDetectada.address) {
      var addr = ultimaDireccionDetectada.address;
      road = (addr.road || addr.pedestrian || addr.cycleway || addr.path || addr.Address || '').trim();
      houseNumber = (addr.house_number || addr.AddNum || '').trim();
      barrio = addr.neighbourhood || addr.suburb || addr.residential || addr.Neighborhood || '';
      municipio = addr.city || addr.town || addr.municipality || addr.City || '';

      if (!direccionAplicar) {
        if (road && houseNumber) {
          direccionAplicar = road + ' # ' + houseNumber;
        } else if (road) {
          direccionAplicar = road;
        } else if (ultimaDireccionDetectada.display_name) {
          direccionAplicar = ultimaDireccionDetectada.display_name.split(',').slice(0, 3).join(', ');
        }

        if (barrio && direccionAplicar.indexOf(barrio) === -1) {
          direccionAplicar += ', ' + barrio;
        }
        if (municipio && direccionAplicar.indexOf(municipio) === -1) {
          direccionAplicar += ', ' + municipio;
        }
      }
    }
  }

  if (!direccionAplicar) {
    alert('No hay una dirección sugerida o detectada en el mapa para aplicar.');
    return;
  }

  $('#direccion').val(direccionAplicar);
  detectarYSincronizarMunicipio(direccionAplicar);
  actualizarTarjetaDireccionPedidoUI();
  $('#btnCopiarDireccionMapa').hide();
  $('#mapaBadgeCoincidencia').html('<span class="label label-success"><i class="fas fa-check-double"></i> Aplicada al pedido</span>').show();

  // Descomponer vía si hay datos disponibles
  if (road) {
    var matchVia = road.match(/^(calle|carrera|cra|cr|cl|avenida|av|diagonal|diag|dg|transversal|trans|tv|circular|autopista)\s*(\d+[a-zA-Z]?)/i);
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
    } else {
      if (!$('#numNomen').val()) {
        $('#numNomen').val(road);
      }
    }

    if (houseNumber) {
      var partes = houseNumber.split(/[-#\s]+/);
      if (partes.length >= 2) {
        $('#numNomen2').val(partes[0]);
        $('#num3').val(partes[1]);
      } else if (partes.length === 1 && partes[0]) {
        $('#num3').val(partes[0]);
      }
    }
  }

  if (municipio) {
    $('#selectMunicipio option').each(function() {
      if ($(this).text().toLowerCase().indexOf(municipio.toLowerCase()) !== -1) {
        $(this).prop('selected', true);
        return false;
      }
    });
  }

  var nTipo = ($('#selectNomenclaturas').val() || '').trim();
  var n1 = ($('#numNomen').val() || '').trim();
  var n2 = ($('#numNomen2').val() || '').trim();
  var n3 = ($('#num3').val() || '').trim();

  if (nTipo && n1 && n2 && n3) {
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
        text: $('#descDireccion').val() || direccionAplicar,
        showConfirmButton: true,
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#10b981',
        showCloseButton: true
      });
    } else if (typeof toastr !== 'undefined') {
      toastr.success('Dirección estructurada aplicada al formulario', '¡Ubicación Sincronizada!', { timeOut: 0, closeButton: true });
    }
  } else {
    $('#validaDir').prop('checked', false);
    $('#descDireccion').removeClass('is-invalid').attr('placeholder', 'Vía detectada: use Dirección Libre o complete Cruce y Placa');

    if (typeof Swal !== 'undefined') {
      Swal.fire({
        toast: true,
        position: 'top',
        icon: 'info',
        title: 'Dirección fijada en el pedido',
        html: '<div style="text-align:left; font-size:12.5px; line-height:1.45;">' +
              '📍 <b>Vía fijada:</b> ' + direccionAplicar + '<br>' +
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

  actualizarModoDireccionUI();
}
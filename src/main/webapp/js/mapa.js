$(document).ready(function() {
	// Obtener la URL base de tu proyecto "ProyectoPizzaAmericana"
	const loc = window.location;
	const pathParts = loc.pathname.split('/');
	const baseFolder = "ProyectoPizzaAmericana";
	const index = pathParts.indexOf(baseFolder);

	// Reconstruir la URL base completa del proyecto
	const server = `${loc.origin}/${pathParts.slice(1, index + 1).join("/")}/`;

	// Variable para almacenar la respuesta del servidor
	let respuesta = '';
	let usuario = '';

	// Validar el usuario mediante una petición AJAX síncrona
	$.ajax({
	    url: server + 'ValidarUsuarioAplicacion',
	    dataType: 'json',
	    type: 'POST',
	    async: false, // ⚠️ Sincrónico: se recomienda cambiar si puedes usar async/await
	    success: function (data) {
	        respuesta = data[0]?.respuesta || '';
	        usuario = data[0]?.nombreusuario || '';
	    },
	    error: function () {
	        console.error("Error al validar el usuario.");
	        location.href = server + "Index.html";
	    }
	});

	// Cargar el menú o redirigir según el tipo de usuario
	switch (respuesta) {
	    case 'OK': // Usuario común
	        $('#cargarMenu').load(server +"Menu.html", function () {
					    $('#usuariologin').text(usuario);
					    $('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
					});
	        break;

	    case 'OKA': // Usuario administrador		
			$('#cargarMenu').load(server + "MenuAdm.html", function () {
			    $('#usuariologin').text(usuario);
			    $('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
			});
	        break;

	    case 'OKP': // Usuario PQRS
	        $('#cargarMenu').load(server +"MenuPQRS.html", function () {
					    $('#usuariologin').text(usuario);
					    $('#logo-img').attr("src", server + "images/logo-sin-fondo.png");
					});
	        break;

	    default: // No válido o sin sesión
	        location.href = server + "Index.html";
	        break;
	}

	const selectTienda = $("#tiendaSelect");
	const selectTiendaHist = $("#tiendaHistorial");
	const filter = $("#filter");
	const startDate = $("#startDate");
	const endDate = $("#endDate");
	var tiendas = [];
	var accessToken;
	var map;
	var map_detalle;
	let tiendaidH = 0
	let markers = {};
	const userDetails = document.getElementById("user-details");
	const infoDetails = document.getElementById("info");
	var marcadorDetalle;
	var longitudInicial = -75.5818;
	var latitudInicial = 6.2527;
	var zoomInicial = 11;

	function configurarCapasModernas(mapInstance) {
		// 1. Google Maps Calles (Ultra-limpio, fondo claro, moderno, sin tonos marrones)
		const capaGoogleCalles = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
			attribution: '&copy; Google Maps',
			maxZoom: 20
		});

		// 2. Google Maps Satélite Híbrido (Fotografía satelital con nombres de calles)
		const capaGoogleSatelite = L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
			attribution: '&copy; Google Maps',
			maxZoom: 20
		});

		// 3. Carto Voyager (Pasteles modernos de alta resolución)
		const capaCartoVoyager = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
			attribution: '&copy; CARTO &copy; OpenStreetMap',
			subdomains: 'abcd',
			maxZoom: 19
		});

		// 4. Gris Minimalista (Sleek canvas dashboard style)
		const capaGris = L.layerGroup([
			L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}', {
				attribution: 'Tiles &copy; Esri',
				maxZoom: 19
			}),
			L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Reference/MapServer/tile/{z}/{y}/{x}', {
				maxZoom: 19
			})
		]);

		// Capa por defecto: Google Maps Calles
		capaGoogleCalles.addTo(mapInstance);

		const baseMaps = {
			"🗺️ Google Calles": capaGoogleCalles,
			"🛰️ Google Satélite": capaGoogleSatelite,
			"🎨 Carto Voyager": capaCartoVoyager,
			"🏙️ Gris Minimalista": capaGris
		};

		L.control.layers(baseMaps, null, { position: 'topright' }).addTo(mapInstance);
		return { capaGoogleCalles, capaGoogleSatelite, capaCartoVoyager, capaGris };
	}

	// Llamadas optimizadas
	Promise.all([
		cargarParametro("ARCGIS-JS", "a_js"),
	]).then(() => {
		map = L.map('map').setView(
		    [latitudInicial, longitudInicial],
		    zoomInicial);
		configurarCapasModernas(map);

		map_detalle = L.map('mapa-detalle').setView(
		    [latitudInicial, longitudInicial],
		    zoomInicial);
		configurarCapasModernas(map_detalle);

		marcadorDetalle = L.marker([0, 0]).addTo(map_detalle);

		// Paleta de colores vivos y modernos por tienda para que las zonas resalten claramente
		const PALETA_ZONAS = {
			"Bello": "#2563eb",       // Azul eléctrico
			"Niquia": "#0284c7",      // Azul cielo
			"Calasanz": "#10b981",    // Verde esmeralda
			"La Mota": "#8b5cf6",     // Púrpura
			"America": "#f59e0b",     // Ámbar
			"Envigado": "#ec4899",    // Rosa magenta
			"Pilarica": "#06b6d4",    // Turquesa
			"San Antonio": "#f97316", // Naranja
			"Itagui": "#14b8a6",      // Verde azulado
			"Manrique": "#ef4444",    // Rojo vibrante
			"Poblado": "#6366f1"      // Índigo
		};
		const COLORES_FALLBACK = ["#3b82f6", "#10b981", "#f59e0b", "#ec4899", "#8b5cf6", "#06b6d4", "#ef4444", "#14b8a6"];

		fetch('zonas-polig.geojson')
		  .then(r => r.json())
		  .then(geojson => {
		    function obtenerColorZona(feature) {
		      const name = feature.properties?.name?.trim() || "";
		      const id = feature.properties?.id || 0;
		      return PALETA_ZONAS[name] || COLORES_FALLBACK[id % COLORES_FALLBACK.length];
		    }

		    const geojsonOptions = {
		      style: feature => {
		        const color = obtenerColorZona(feature);
		        return {
		          fillColor: color,
		          fillOpacity: 0.22,
		          color: color,
		          weight: 2.5,
		          opacity: 0.85
		        };
		      },
		      onEachFeature: (feature, layer) => {
		        const name = feature.properties?.name?.trim() || `Zona ${feature.properties?.id || ''}`;
		        layer.bindTooltip(`<div class="badge-zona-tooltip">🍕 <strong>${name}</strong></div>`, {
		          direction: 'center',
		          sticky: true,
		          className: 'tooltip-zona-estilizada'
		        });

		        layer.on('mouseover', function(e) {
		          layer.setStyle({ fillOpacity: 0.45, weight: 3.5 });
		        });

		        layer.on('mouseout', function(e) {
		          layer.setStyle({ fillOpacity: 0.22, weight: 2.5 });
		        });

		        layer.on('click', function(e) {
		          L.DomEvent.stopPropagation(e);
		          L.DomEvent.preventDefault(e);
		        });
		      }
		    };

		    L.geoJSON(geojson, geojsonOptions).addTo(map);
		    L.geoJSON(geojson, geojsonOptions).addTo(map_detalle);
		  })
		  .catch(err => console.error("Error cargando GeoJSON:", err));

		initializePage();
	});



	function setCurrentDate() {
		const today = new Date();
		const year = today.getFullYear();
		const month = (today.getMonth() + 1).toString().padStart(2, '0'); // +1 porque los meses empiezan en 0
		const day = today.getDate().toString().padStart(2, '0'); //  el día tenga dos dígitos
		const currentDate = `${year}-${month}-${day}`;
		// Asignar la fecha actual a los campos de fecha
		startDate.val(currentDate);
		endDate.val(currentDate);

		const tiendaId = $("#tiendaHistorial").val() || tiendaidH || 0;
		EnvioDatos({ tiendaId: tiendaId, action: "historial", startDate: currentDate, endDate: currentDate });
	}

	// Llamar inmediatamente para asegurar fechas desde la carga del DOM
	setCurrentDate();


	const map_detalleContainer = $('#mapa-detalle');




	// Inicialización de DataTable
	// Inicialización de DataTable con columnas proporcionadas y limpias
	const table = $('#table-container').DataTable({
		"language": {
			"url": "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json",
			"sInfo": "", "sInfoEmpty": "", "sInfoFiltered": "",
			"sSearch": "Buscar:",
			"sLengthMenu": "Mostrar _MENU_ registros"
		},
		"columns": [
			{ "className": "text-left align-middle" },
			{ "className": "text-center align-middle", "width": "115px" },
			{ "className": "text-center align-middle", "width": "95px" }
		],
		"responsive": true,
		"autoWidth": false,
		"pageLength": 10
	});

	const tableHistorial = $('#table-historial').DataTable({
		"language": {
			"url": "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json",
			"sInfo": "", "sInfoEmpty": "", "sInfoFiltered": ""
		},
		"responsive": true,
		"autoWidth": false,
		"createdRow": function(row, data, dataIndex) {
			const $c = $(row).find('.domi-nombre-cell');
			if ($c.length) {
				const cClave = $c.attr('data-clave');
				const cTienda = $c.attr('data-idtienda');
				const cNombre = $c.attr('data-nombre');
				const cTipo = $c.attr('data-tipo');
				const cEmpresa = $c.attr('data-empresa');
				$(row).attr('data-clave_usuario', cClave)
					  .attr('data-idtienda', cTienda)
					  .attr('data-nombre', cNombre)
					  .attr('data-tipo_repartidor', cTipo)
					  .attr('data-empresa_temporal', cEmpresa)
					  .data('clave_usuario', cClave)
					  .data('idtienda', cTienda);
			}
		}
	});

	const tiendaMap = {};


	// =========================================================================
	// GESTIÓN DE FLOTA EN TIEMPO REAL (DESDUPLICACIÓN ESTRICTA: 1 FILA POR DOMI)
	// =========================================================================
	const mapaConductores = new Map(); // clave_usuario -> { row, marker, data }
	let conductorSeleccionadoClave = null;

	function generarBadgeTipoRepartidor(tipo, empresa) {
		const tipoNorm = String(tipo || '').toUpperCase().trim();
		if (tipoNorm === 'TEMPORAL') {
			const emp = (empresa && empresa.trim()) ? ` · ${empresa.trim()}` : '';
			return `<span class="badge-tipo-domi badge-temporal" title="Personal Temporal / Tercerizado"><i class="fas fa-user-clock mr-1"></i>Temporal${emp}</span>`;
		}
		// Si es nuestro (DIRECTO) o de planta, NO mostrar etiqueta (dejar nombre limpio)
		return '';
	}

	function generarBadgeEstadoOperativo(estado, pedidosActivos = 0) {
		const est = String(estado || 'EN_TIENDA').toUpperCase().trim();
		const peds = parseInt(pedidosActivos || 0, 10);
		if (est === 'EN_RUTA') {
			const label = peds > 0 ? `En Ruta (${peds} ${peds === 1 ? 'ped' : 'peds'})` : 'En Ruta';
			return `<span class="badge-estado-domi badge-en-ruta" title="En Ruta con ${peds} pedidos asignados"><i class="fas fa-motorcycle mr-1"></i>${label}</span>`;
		}
		if (est === 'FUERA_DE_TIENDA' || est === 'SIN_PEDIDOS') {
			return `<span class="badge-estado-domi badge-fuera-tienda" title="Fuera de Tienda (Sin Pedidos Asignados)"><i class="fas fa-walking mr-1"></i>Fuera Tienda</span>`;
		}
		return `<span class="badge-estado-domi badge-en-tienda" title="En Tienda (Disponible)"><i class="fas fa-store mr-1"></i>En Tienda</span>`;
	}

	function generarBadgeBateria(bateria) {
		if (bateria === null || bateria === undefined || bateria === '') return '';
		const bat = parseInt(bateria, 10);
		if (isNaN(bat)) return '';
		let icon = 'fa-battery-full';
		let colorClass = 'text-success';
		if (bat <= 20) {
			icon = 'fa-battery-empty';
			colorClass = 'text-danger font-weight-bold';
		} else if (bat <= 50) {
			icon = 'fa-battery-half';
			colorClass = 'text-warning';
		} else if (bat <= 80) {
			icon = 'fa-battery-three-quarters';
			colorClass = 'text-info';
		}
		return `<span class="badge-bateria-cell" title="Batería del dispositivo: ${bat}%"><i class="fas ${icon} mr-1 ${colorClass}"></i>${bat}%</span>`;
	}

	function actualizarContadorActivos() {
		$('#badgeTotalDomiActivos').text(mapaConductores.size);
	}

	function restablecerFichaDetalles() {
		conductorSeleccionadoClave = null;
		$('#detalles-nombre').text('Ficha del Domiciliario');
		$('#detalles-subtitulo').html('<i class="fas fa-satellite-dish mr-1 text-muted"></i> Monitoreo Activo');
		$('#detalles-contenido').html(`
			<div class="alert alert-light border py-3 px-3 mb-0 text-center">
				<i class="fas fa-info-circle mr-1 text-primary"></i> Selecciona un domiciliario de la tabla o un marcador del mapa.
			</div>
		`);
	}

	function mostrarDetallesConductor(clave_usuario, centrar = true) {
		const claveStr = String(clave_usuario);
		const reg = mapaConductores.get(claveStr);
		if (!reg) return;

		conductorSeleccionadoClave = claveStr;

		// Resaltar visualmente la fila en la tabla
		$('#table-container tbody tr').removeClass('fila-seleccionada');
		const rowNode = reg.row.node();
		if (rowNode) {
			$(rowNode).addClass('fila-seleccionada');
		}

		// Nombre alineado a la izquierda sin tienda redundante al lado
		$('#detalles-nombre').text(reg.nombre_usuario);
		const badgeContrato = generarBadgeTipoRepartidor(reg.tipo_repartidor, reg.empresa_temporal);
		const badgeEstado = generarBadgeEstadoOperativo(reg.estado, reg.pedidos_activos);
		const badgeBat = generarBadgeBateria(reg.bateria);
		$('#detalles-subtitulo').html(`
			<div class="d-flex align-items-center mt-1 flex-wrap" style="gap: 4px;">
				<span class="text-success font-weight-bold small mr-1"><i class="fas fa-circle mr-1" style="font-size: 8px;"></i> En servicio hoy</span>
				${badgeEstado}
				${badgeBat}
				${badgeContrato}
			</div>
		`);

		const lat = reg.latitud;
		const lng = reg.longitud;

		const tipoFilaContrato = (reg.tipo_repartidor === 'TEMPORAL') ? `
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-user-clock mr-1 text-warning"></i> Contratación:</span>
				<span>${badgeContrato}</span>
			</div>
		` : `
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-shield-alt mr-1 text-primary"></i> Personal:</span>
				<span class="font-weight-bold text-dark">Nuestra Flota</span>
			</div>
		`;

		const batTexto = (reg.bateria !== null && reg.bateria !== undefined) ? `${reg.bateria}%` : 'N/D';
		const velTexto = (typeof reg.velocidad === 'number') ? `${reg.velocidad} km/h` : '0 km/h';

		const pedsCount = parseInt(reg.pedidos_activos || 0, 10);
		let filaPedidos = '';
		if (pedsCount > 0 || reg.pedidos_detalle) {
			const chips = reg.pedidos_detalle 
				? reg.pedidos_detalle.split(',').map(p => `<span class="badge-pedido-chip"><i class="fas fa-box mr-1"></i>#${p.trim()}</span>`).join(' ')
				: `<span class="badge badge-info font-weight-bold">${pedsCount} pedidos</span>`;
			filaPedidos = `
				<div class="item-dato-domi">
					<span class="text-muted"><i class="fas fa-box-open mr-1 text-primary"></i> Pedidos en Ruta:</span>
					<div class="text-right d-flex flex-wrap justify-content-end" style="max-width: 65%;">${chips}</div>
				</div>
			`;
		} else if (reg.estado === 'FUERA_DE_TIENDA') {
			filaPedidos = `
				<div class="item-dato-domi">
					<span class="text-muted"><i class="fas fa-exclamation-triangle mr-1 text-warning"></i> Pedidos en Ruta:</span>
					<span class="badge badge-warning text-dark font-weight-bold">0 pedidos (Sin asignación)</span>
				</div>
			`;
		}

		$('#detalles-contenido').html(`
			${tipoFilaContrato}
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-toggle-on mr-1 text-primary"></i> Estado Operativo:</span>
				<span>${badgeEstado}</span>
			</div>
			${filaPedidos}
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-tachometer-alt mr-1 text-info"></i> Velocidad:</span>
				<span class="font-weight-bold text-dark">${velTexto}</span>
			</div>
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-battery-three-quarters mr-1 text-success"></i> Batería Móvil:</span>
				<span class="font-weight-bold text-dark">${batTexto}</span>
			</div>
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-store mr-1 text-primary"></i> Tienda Actual:</span>
				<span class="font-weight-bold text-dark">${reg.nombreTienda}</span>
			</div>
			<div class="item-dato-domi">
				<span class="text-muted"><i class="far fa-clock mr-1 text-info"></i> Última Señal:</span>
				<span class="font-weight-bold text-dark">${reg.fecha_hora}</span>
			</div>
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-map-marker-alt mr-1 text-danger"></i> Coordenadas:</span>
				<span class="text-secondary small font-mono">${lat.toFixed(5)}, ${lng.toFixed(5)}</span>
			</div>
			<div class="item-dato-domi">
				<span class="text-muted"><i class="fas fa-map-pin mr-1 text-warning"></i> Dirección:</span>
				<span id="textoDireccionLive" class="font-weight-bold text-dark text-right" style="max-width: 65%;">Consultando dirección...</span>
			</div>
			<div class="mt-3 pt-2 border-top">
				<button type="button" class="btn btn-primary btn-sm btn-block shadow-sm font-weight-bold btn-ver-ruta-hoy" 
					data-clave="${reg.clave_usuario}" 
					data-nombre="${reg.nombre_usuario}" 
					data-idtienda="${reg.idtienda}" 
					data-tienda="${reg.nombreTienda}"
					data-tipo="${reg.tipo_repartidor}"
					data-empresa="${reg.empresa_temporal}">
					<i class="fas fa-route mr-1"></i> Ver Recorrido de Hoy en Detalle
				</button>
			</div>
		`);

		if (centrar && reg.marker) {
			map.setView([lat, lng], 17);
			reg.marker.openPopup();
		}

		reverseGeocode(lat, lng);
	}

	function reverseGeocode(latitude, longitude) {
		const token = accessToken || "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";
		const url = `https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?f=pjson&location=${longitude},${latitude}&token=${token}`;
		fetch(url)
			.then(response => response.json())
			.then(data => {
				const addr = data.address?.LongLabel || "No disponible";
				const el = document.getElementById("textoDireccionLive");
				if (el) el.textContent = addr;
			})
			.catch(() => {
				const el = document.getElementById("textoDireccionLive");
				if (el) el.textContent = "Dirección no disponible";
			});
	}

	function calcularMinutosDesdeUltimoReporte(fechaHoraStr) {
		if (!fechaHoraStr) return 999999;
		try {
			const d = new Date(fechaHoraStr.replace(' ', 'T'));
			if (isNaN(d.getTime())) return 999999;
			return Math.max(0, Math.floor((Date.now() - d.getTime()) / (1000 * 60)));
		} catch (e) {
			return 999999;
		}
	}

	// Crear icono personalizado para repartidores con radar de actividad e icono de moto FontAwesome
	function crearIconoMoto(nombre, esInactivo = false) {
		const claseInactiva = esInactivo ? 'moto-inactiva' : '';
		const pulseHtml = esInactivo ? '' : '<div class="moto-marker-pulse"></div>';
		const titleTexto = esInactivo ? `${nombre} (Inactivo / Sin señal > 2 horas)` : nombre;

		return L.divIcon({
			className: 'custom-moto-marker',
			html: `
				<div class="moto-marker-box ${claseInactiva}" title="${titleTexto}">
					${pulseHtml}
					<div class="moto-marker-icon">
						<i class="fas fa-motorcycle"></i>
					</div>
				</div>
			`,
			iconSize: [38, 38],
			iconAnchor: [19, 19],
			popupAnchor: [0, -20]
		});
	}

	// Animación de deslizamiento suave entre coordenadas (estilo Uber / Rappi)
	function animarMarcadorSuave(marker, latFinal, lngFinal, duracionMs = 1400) {
		const latInicial = marker.getLatLng().lat;
		const lngInicial = marker.getLatLng().lng;

		if ((latInicial === 0 && lngInicial === 0) || (latInicial === latFinal && lngInicial === lngFinal)) {
			marker.setLatLng([latFinal, lngFinal]);
			return;
		}

		if (marker._animacionFrame) {
			cancelAnimationFrame(marker._animacionFrame);
		}

		const inicioTiempo = performance.now();

		function frame(tiempoActual) {
			const transcurrido = tiempoActual - inicioTiempo;
			const progreso = Math.min(transcurrido / duracionMs, 1);
			const ease = 1 - Math.pow(1 - progreso, 3);

			const latActual = latInicial + (latFinal - latInicial) * ease;
			const lngActual = lngInicial + (lngFinal - lngInicial) * ease;

			marker.setLatLng([latActual, lngActual]);

			if (progreso < 1) {
				marker._animacionFrame = requestAnimationFrame(frame);
			} else {
				marker._animacionFrame = null;
			}
		}

		marker._animacionFrame = requestAnimationFrame(frame);
	}

	function aplicarFiltrosLive(resetPage = false) {
		const selectedStore = String(selectTienda.val() || "0");
		const filtroTipo = String($('#filtroTipoRepartidorLive').val() || "TODOS");
		const filtroEstado = String($('#filtroEstadoLive').val() || "TODOS");
		const soloBiometria = $('#chkSoloBiometriaLive').is(':checked');

		let visiblesCount = 0;

		mapaConductores.forEach((reg, claveStr) => {
			let visible = true;

			// 1. Filtro estricto de tienda
			const driverStore = String(reg.idtienda || "");
			if (selectedStore !== "0" && driverStore !== selectedStore) {
				visible = false;
			}

			// 2. Filtro de tipo de repartidor
			if (visible && filtroTipo !== "TODOS") {
				if (filtroTipo === "TEMPORAL" && reg.tipo_repartidor !== "TEMPORAL") visible = false;
				if (filtroTipo === "DIRECTO" && reg.tipo_repartidor === "TEMPORAL") visible = false;
			}

			// 3. Filtro de turno en biometría
			if (visible && soloBiometria) {
				if (!reg.en_turno_biometria) visible = false;
			}

			// 4. Filtro de estado operativo (En Tienda vs En Ruta vs Fuera de Tienda vs Activos vs Inactivos)
			if (visible && filtroEstado !== "TODOS") {
				if (filtroEstado === "ACTIVOS" && reg.esInactivo) visible = false;
				if (filtroEstado === "INACTIVOS" && !reg.esInactivo) visible = false;
				const estadoReg = String(reg.estado || "EN_TIENDA").toUpperCase();
				if (filtroEstado === "EN_TIENDA" && estadoReg !== "EN_TIENDA") visible = false;
				if (filtroEstado === "EN_RUTA" && estadoReg !== "EN_RUTA") visible = false;
				if (filtroEstado === "FUERA_DE_TIENDA" && estadoReg !== "FUERA_DE_TIENDA" && estadoReg !== "SIN_PEDIDOS") visible = false;
			}

			// Visibilidad en Leaflet
			if (reg.marker) {
				if (visible) {
					if (!map.hasLayer(reg.marker)) reg.marker.addTo(map);
				} else {
					if (map.hasLayer(reg.marker)) map.removeLayer(reg.marker);
				}
			}

			if (visible) visiblesCount++;
		});

		// Redibujar la tabla usando el motor nativo de DataTables (compacta registros y elimina páginas vacías)
		if (resetPage) {
			table.page('first').draw(false);
		} else {
			table.draw(false);
		}

		$('#badgeTotalDomiActivos').text(visiblesCount);
	}

	const TIENDAS_POR_ID = {
		1: { nombre: "Manrique", lat: 6.270404035083642, lng: -75.55476201695326 },
		2: { nombre: "Bello", lat: 6.31313642173263, lng: -75.5599370602694 },
		3: { nombre: "America", lat: 6.248795464621254, lng: -75.6042451558178 },
		4: { nombre: "Calasanz", lat: 6.266104483307567, lng: -75.59837327880433 },
		5: { nombre: "Itagui", lat: 6.165763998154379, lng: -75.6215936733657 },
		7: { nombre: "La Mota", lat: 6.211386771165651, lng: -75.5952875746262 },
		8: { nombre: "Envigado", lat: 6.167150830483641, lng: -75.58524990346257 },
		9: { nombre: "Pilarica", lat: 6.273129787405367, lng: -75.5854100862619 },
		10: { nombre: "San Antonio", lat: 6.168106662807738, lng: -75.64904988626188 },
		11: { nombre: "Manrique Piloto", lat: 6.263560956119886, lng: -75.55326141695326 },
		13: { nombre: "Niquia", lat: 6.337781519121857, lng: -75.5499612076178 }
	};

	function calcularDistanciaPuntosMetros(lat1, lon1, lat2, lon2) {
		const R = 6371e3;
		const toRad = x => x * Math.PI / 180;
		const dLat = toRad(lat2 - lat1);
		const dLon = toRad(lon2 - lon1);
		const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
		return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	function determinarEstadoPorTiendaAsignada(lat, lng, idtienda, pedidosActivos = 0) {
		const id = parseInt(idtienda, 10);
		const peds = parseInt(pedidosActivos || 0, 10);
		let enTienda = false;
		if (id && TIENDAS_POR_ID[id]) {
			const t = TIENDAS_POR_ID[id];
			const dist = calcularDistanciaPuntosMetros(lat, lng, t.lat, t.lng);
			enTienda = (dist <= 90);
		} else {
			// Si no tiene tienda asignada, evaluar contra cualquiera de las tiendas activas
			for (const key in TIENDAS_POR_ID) {
				const t = TIENDAS_POR_ID[key];
				if (calcularDistanciaPuntosMetros(lat, lng, t.lat, t.lng) <= 90) {
					enTienda = true;
					break;
				}
			}
		}
		if (enTienda) return "EN_TIENDA";
		return (peds > 0) ? "EN_RUTA" : "FUERA_DE_TIENDA";
	}

	function updateRowAndMarker(clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda, tipo_repartidor, empresa_temporal, en_turno_biometria, estado = null, bateria = null, velocidad = null, pedidos_activos = 0, pedidos_detalle = null, autoFilter = true) {
		if (typeof estado === 'boolean') {
			autoFilter = estado;
			estado = null;
		}

		const claveStr = String(clave_usuario || "").trim();
		if (!claveStr) return;

		const lat = parseFloat(latitude);
		const lng = parseFloat(longitude);
		if (isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) return;

		// Preservar valores si ya existían en memoria
		let finalTipo = tipo_repartidor;
		let finalEmpresa = empresa_temporal;
		let finalBiometria = en_turno_biometria;
		let finalBateria = bateria;
		let finalVelocidad = velocidad;
		let finalPedidosActivos = parseInt(pedidos_activos || 0, 10);
		let finalPedidosDetalle = pedidos_detalle;

		if (mapaConductores.has(claveStr)) {
			const regExistente = mapaConductores.get(claveStr);
			if (!finalTipo) finalTipo = regExistente.tipo_repartidor;
			if (!finalEmpresa) finalEmpresa = regExistente.empresa_temporal;
			if (finalBiometria === undefined) finalBiometria = regExistente.en_turno_biometria;
			if (finalBateria === null || finalBateria === undefined) finalBateria = regExistente.bateria;
			if (finalVelocidad === null || finalVelocidad === undefined) finalVelocidad = regExistente.velocidad;
			if (pedidos_activos === undefined || pedidos_activos === null) finalPedidosActivos = regExistente.pedidos_activos;
			if (pedidos_detalle === undefined || pedidos_detalle === null) finalPedidosDetalle = regExistente.pedidos_detalle;
		}
		if (!finalTipo) finalTipo = "OTRO";
		if (!finalEmpresa) finalEmpresa = "";
		finalBiometria = !!finalBiometria;
		if (finalVelocidad === null || finalVelocidad === undefined) finalVelocidad = 0;

		// Cálculo dinámico y certero de geocerca contra la tienda asignada y pedidos activos
		const finalEstado = determinarEstadoPorTiendaAsignada(lat, lng, idtienda, finalPedidosActivos);

		// Detección de inactividad (> 2 horas sin señal GPS)
		const minutosInactividad = calcularMinutosDesdeUltimoReporte(fecha_hora);
		const esInactivo = (minutosInactividad > 120);

		const nombreTienda = tiendaMap[idtienda] || (idtienda ? `Tienda ${idtienda}` : "Tienda desconocida");
		const horaSolo = fecha_hora ? (fecha_hora.includes(' ') ? fecha_hora.split(' ')[1] : fecha_hora) : "--:--:--";
		const badgeTipoHtml = generarBadgeTipoRepartidor(finalTipo, finalEmpresa);
		const badgeEstadoHtml = generarBadgeEstadoOperativo(finalEstado, finalPedidosActivos);
		const badgeBateriaHtml = generarBadgeBateria(finalBateria);
		const badgeBiometriaHtml = finalBiometria 
			? '<span class="badge-biometria-turno ml-1" title="Turno laboral activo en biometría"><i class="fas fa-fingerprint text-success"></i></span>' 
			: '';
		const badgeInactivoHtml = esInactivo 
			? '<span class="badge-inactivo-tag ml-1" title="Sin reporte GPS hace más de 2 horas"><i class="fas fa-moon mr-1"></i>Inactivo</span>' 
			: '';

		const colUsuario = `
			<div class="domi-nombre-cell">
				<div class="d-flex w-100 align-items-center justify-content-between">
					<span class="domi-nombre-texto ${esInactivo ? 'text-muted' : ''}">${nombre_usuario}</span>
					${badgeBateriaHtml}
				</div>
				<div class="d-flex align-items-center flex-wrap mt-1" style="gap: 4px;">
					${badgeEstadoHtml}
					${badgeInactivoHtml}
					${badgeBiometriaHtml}
					${badgeTipoHtml}
				</div>
			</div>
		`;
		const colFecha = `<span class="badge-hora-live" title="Reporte completo: ${fecha_hora}"><i class="far fa-clock mr-1 text-muted"></i>${horaSolo}</span>`;
		const colTienda = `<span class="badge-tienda-tabla">${nombreTienda}</span>`;

		if (mapaConductores.has(claveStr)) {
			// ⚡ DOMICILIARIO YA EXISTE: Actualizar sus datos in-place
			const reg = mapaConductores.get(claveStr);
			reg.latitud = lat;
			reg.longitud = lng;
			reg.fecha_hora = fecha_hora;
			reg.nombre_usuario = nombre_usuario;
			reg.idtienda = idtienda;
			reg.nombreTienda = nombreTienda;
			reg.tipo_repartidor = finalTipo;
			reg.empresa_temporal = finalEmpresa;
			reg.en_turno_biometria = finalBiometria;
			reg.estado = finalEstado;
			reg.esInactivo = esInactivo;
			reg.minutosInactividad = minutosInactividad;
			reg.bateria = finalBateria;
			reg.velocidad = finalVelocidad;
			reg.pedidos_activos = finalPedidosActivos;
			reg.pedidos_detalle = finalPedidosDetalle;

			reg.row.data([
				colUsuario,
				colFecha,
				colTienda
			]).draw(false);

			const node = reg.row.node();
			if (node) {
				node.id = `row-${claveStr}`;
				$(node).attr('data-clave', claveStr);
				$(node).data('clave_usuario', claveStr);
				$(node).data('idtienda', String(idtienda || ""));
				$(node).data('tipo_repartidor', finalTipo);
				$(node).data('empresa_temporal', finalEmpresa);
				$(node).data('en_turno_biometria', finalBiometria);
				$(node).data('estado', finalEstado);
				$(node).data('es_inactivo', esInactivo);
				$(node).data('bateria', finalBateria);
				$(node).data('velocidad', finalVelocidad);
				$(node).data('pedidos_activos', finalPedidosActivos);
				$(node).data('pedidos_detalle', finalPedidosDetalle);
				if (conductorSeleccionadoClave === claveStr) {
					$(node).addClass('fila-seleccionada');
				}
				$(node).removeClass('fila-actualizada');
				void node.offsetWidth;
				$(node).addClass('fila-actualizada');
			}

			if (reg.marker) {
				animarMarcadorSuave(reg.marker, lat, lng, 1400);
				reg.marker.setIcon(crearIconoMoto(nombre_usuario, esInactivo));
				const bioText = finalBiometria ? '<span class="badge badge-success small mb-1 mr-1"><i class="fas fa-fingerprint mr-1"></i>En Turno</span>' : '';
				const inactivoText = esInactivo ? '<span class="badge-inactivo-tag mb-1 mr-1"><i class="fas fa-moon mr-1"></i>Sin señal (>2h)</span>' : '';
				const lineaBadges = `<div class="d-flex align-items-center flex-wrap my-1" style="gap: 4px;">${badgeEstadoHtml}${inactivoText}${badgeBateriaHtml}${bioText}${badgeTipoHtml}</div>`;
				const pedidosPopup = finalPedidosDetalle ? `<div class="mt-1"><small class="text-primary font-weight-bold"><i class="fas fa-box mr-1"></i>Pedidos: ${finalPedidosDetalle}</small></div>` : '';
				reg.marker.getPopup()?.setContent(`<b>${nombre_usuario}</b><br>${lineaBadges}<span class="badge-tienda-tabla mb-1">${nombreTienda}</span>${pedidosPopup}<br><small class="text-muted"><i class="far fa-clock mr-1"></i>${fecha_hora}</small>`);
			}

			if (conductorSeleccionadoClave === claveStr) {
				mostrarDetallesConductor(claveStr, false);
			}

		} else {
			// ➕ DOMICILIARIO NUEVO: Registrar una única fila en DataTables
			const newRow = table.row.add([
				colUsuario,
				colFecha,
				colTienda
			]).draw(false);

			const node = newRow.node();
			if (node) {
				node.id = `row-${claveStr}`;
				$(node).attr('data-clave', claveStr);
				$(node).data('clave_usuario', claveStr);
				$(node).data('idtienda', String(idtienda || ""));
				$(node).data('tipo_repartidor', finalTipo);
				$(node).data('empresa_temporal', finalEmpresa);
				$(node).data('en_turno_biometria', finalBiometria);
				$(node).data('estado', finalEstado);
				$(node).data('es_inactivo', esInactivo);
				$(node).data('bateria', finalBateria);
				$(node).data('velocidad', finalVelocidad);
				$(node).data('pedidos_activos', finalPedidosActivos);
				$(node).data('pedidos_detalle', finalPedidosDetalle);
				$(node).addClass("table-row fila-actualizada");
			}

			// Crear marcador interactivo con soporte para estado inactivo
			const bioText = finalBiometria ? '<span class="badge badge-success small mb-1 mr-1"><i class="fas fa-fingerprint mr-1"></i>En Turno</span>' : '';
			const inactivoText = esInactivo ? '<span class="badge-inactivo-tag mb-1 mr-1"><i class="fas fa-moon mr-1"></i>Sin señal (>2h)</span>' : '';
			const lineaBadges = `<div class="d-flex align-items-center flex-wrap my-1" style="gap: 4px;">${badgeEstadoHtml}${inactivoText}${badgeBateriaHtml}${bioText}${badgeTipoHtml}</div>`;
			const pedidosPopup = finalPedidosDetalle ? `<div class="mt-1"><small class="text-primary font-weight-bold"><i class="fas fa-box mr-1"></i>Pedidos: ${finalPedidosDetalle}</small></div>` : '';
			const popupContent = `<b>${nombre_usuario}</b><br>${lineaBadges}<span class="badge-tienda-tabla mb-1">${nombreTienda}</span>${pedidosPopup}<br><small class="text-muted"><i class="far fa-clock mr-1"></i>${fecha_hora}</small>`;
			const marker = L.marker([lat, lng], { icon: crearIconoMoto(nombre_usuario, esInactivo) })
				.bindPopup(popupContent)
				.addTo(map);

			marker.on('click', () => {
				mostrarDetallesConductor(claveStr, false);
			});

			markers[claveStr] = marker;

			mapaConductores.set(claveStr, {
				clave_usuario: claveStr,
				nombre_usuario: nombre_usuario,
				latitud: lat,
				longitud: lng,
				fecha_hora: fecha_hora,
				idtienda: idtienda,
				nombreTienda: nombreTienda,
				tipo_repartidor: finalTipo,
				empresa_temporal: finalEmpresa,
				en_turno_biometria: finalBiometria,
				estado: finalEstado,
				bateria: finalBateria,
				velocidad: finalVelocidad,
				pedidos_activos: finalPedidosActivos,
				pedidos_detalle: finalPedidosDetalle,
				row: newRow,
				marker: marker
			});
		}

		if (autoFilter) {
			aplicarFiltrosLive(false);
		}
	}

	// Manejador de clics en las filas de la tabla en tiempo real
	$('#table-container tbody').on('click', 'tr', function() {
		const rowNode = this;
		let clave = $(rowNode).data('clave_usuario') || $(rowNode).attr('id')?.replace('row-', '');
		if (!clave) {
			const rowData = table.row(rowNode).data();
			if (rowData) {
				for (const [k, v] of mapaConductores.entries()) {
					if (v.nombre_usuario === rowData[0]) {
						clave = k;
						break;
					}
				}
			}
		}

		if (clave && mapaConductores.has(String(clave))) {
			mostrarDetallesConductor(String(clave), true);
		}
	});

	$(document).on('click', '#table-historial tbody tr', function(e) {
		const $tr = $(this);
		if ($tr.hasClass('dataTables_empty') || $tr.find('.dataTables_empty').length) return;

		const rowData = tableHistorial.row(this).data();
		if (!rowData) return;

		const $cell = $tr.find('.domi-nombre-cell');
		const claveUsuario = $cell.attr('data-clave') || $tr.attr('data-clave_usuario') || $tr.data('clave_usuario');
		const idTienda = $cell.attr('data-idtienda') || $tr.attr('data-idtienda') || $tr.data('idtienda') || 0;
		const selectedUser = $cell.attr('data-nombre') || $tr.attr('data-nombre') || $tr.find('.domi-nombre-texto').text().trim() || rowData[0];
		const selectedDate = rowData[1];
		const selectedStore = rowData[2];

		if (!claveUsuario) {
			console.warn("No se pudo obtener la clave del domiciliario en esta fila:", rowData);
			Swal.fire({
				icon: 'warning',
				title: 'Identificador no encontrado',
				text: 'No se encontró la clave de este domiciliario. Intenta presionar "Filtrar" nuevamente.'
			});
			return;
		}

		window._ultimoTipoRepartidor = $cell.attr('data-tipo') || $tr.attr('data-tipo_repartidor') || $tr.data('tipo_repartidor') || "OTRO";
		window._ultimaEmpresaTemporal = $cell.attr('data-empresa') || $tr.attr('data-empresa_temporal') || $tr.data('empresa_temporal') || "";

		window._ultimoUsuarioSeleccionado = selectedUser;
		window._ultimaTiendaSeleccionada = selectedStore;
		window._ultimaFechaSeleccionada = selectedDate;
		window._ultimaClaveSeleccionada = String(claveUsuario);

		// Feedback visual de carga inmediato
		Swal.fire({
			title: 'Cargando recorrido...',
			html: `Consultando los puntos GPS de <b>${selectedUser}</b>`,
			allowOutsideClick: false,
			didOpen: () => {
				Swal.showLoading();
			}
		});

		EnvioDatos({ tiendaId: idTienda, action: "detalle", startDate: selectedDate, claveRapida: String(claveUsuario) });
	});

	// Ver recorrido de hoy directamente desde la ficha de tiempo real
	$(document).on('click', '.btn-ver-ruta-hoy', function(e) {
		e.stopPropagation();
		const clave = $(this).data('clave');
		const nombre = $(this).data('nombre') || "Domiciliario";
		const idtienda = $(this).data('idtienda') || 0;
		const tienda = $(this).data('tienda') || "Tienda";
		const tipo = $(this).data('tipo') || "OTRO";
		const empresa = $(this).data('empresa') || "";

		// Fecha de hoy en formato local YYYY-MM-DD
		const d = new Date();
		const anio = d.getFullYear();
		const mes = String(d.getMonth() + 1).padStart(2, '0');
		const dia = String(d.getDate()).padStart(2, '0');
		const hoy = `${anio}-${mes}-${dia}`;

		window._ultimoTipoRepartidor = tipo;
		window._ultimaEmpresaTemporal = empresa;
		window._ultimoUsuarioSeleccionado = nombre;
		window._ultimaTiendaSeleccionada = tienda;
		window._ultimaFechaSeleccionada = hoy;
		window._ultimaClaveSeleccionada = String(clave);

		Swal.fire({
			title: 'Cargando recorrido...',
			html: `Consultando los puntos GPS de hoy de <b>${nombre}</b>`,
			allowOutsideClick: false,
			didOpen: () => {
				Swal.showLoading();
			}
		});

		EnvioDatos({ tiendaId: idtienda, action: "detalle", startDate: hoy, claveRapida: String(clave) });
	});

	let tiendaActualSocket = "0";

	selectTienda.change(function() {
		const tiendaId = String($(this).val() || "0");

		// 1. Limpieza visual inmediata para no dejar datos de la tienda previa
		table.clear().draw();
		mapaConductores.forEach(reg => {
			if (reg.marker) reg.marker.remove();
		});
		mapaConductores.clear();
		markers = {};
		actualizarContadorActivos();
		restablecerFichaDetalles();
		userDetails.innerHTML = `
			<h3>Detalles del Usuario</h3>
			<p>Seleccione un registro de la tabla para mostrar la información...</p>
		`;

		// 2. Gestionar salas en Socket.io
		if (socket && socket.connected) {
			if (tiendaActualSocket !== undefined) {
				socket.emit('salir_tienda', tiendaActualSocket);
			}
			tiendaActualSocket = tiendaId;
			socket.emit('unirse_tienda', tiendaId);
		} else {
			tiendaActualSocket = tiendaId;
		}

		// 3. Consultar datos en backend
		EnvioDatos({ tiendaId: tiendaId, action: "rastreo" });
	});

	// Filtros reactivos en vivo (Tipo de Repartidor y Turno Biométrico)
	$('#filtroTipoRepartidorLive').change(function() {
		aplicarFiltrosLive(true);
	});

	$('#filtroEstadoLive').change(function() {
		aplicarFiltrosLive(true);
	});

	$('#chkSoloBiometriaLive').change(function() {
		aplicarFiltrosLive(true);
	});

	// Filtro nativo de DataTables para Tiempo Real (compacta páginas, elimina huecos y páginas vacías)
	$.fn.dataTable.ext.search.push(function(settings, data, dataIndex) {
		if (settings.sTableId === 'table-container') {
			const rowNode = table.row(dataIndex).node();
			if (!rowNode) return true;

			const clave = $(rowNode).data('clave_usuario') || $(rowNode).attr('data-clave') || $(rowNode).attr('id')?.replace('row-', '');
			const reg = clave ? mapaConductores.get(clave) : null;

			const driverStore = reg ? String(reg.idtienda || "") : String($(rowNode).data('idtienda') || "");
			const driverTipo = reg ? String(reg.tipo_repartidor || "") : String($(rowNode).data('tipo_repartidor') || "");
			const driverBiometria = reg ? !!reg.en_turno_biometria : !!$(rowNode).data('en_turno_biometria');
			const driverEstado = reg ? String(reg.estado || "EN_TIENDA").toUpperCase() : String($(rowNode).data('estado') || "EN_TIENDA").toUpperCase();

			const selectedStore = String(selectTienda.val() || "0");
			const filtroTipo = String($('#filtroTipoRepartidorLive').val() || "TODOS");
			const filtroEstado = String($('#filtroEstadoLive').val() || "TODOS");
			const soloBiometria = $('#chkSoloBiometriaLive').is(':checked');

			// 1. Filtro Tienda
			if (selectedStore !== "0" && driverStore !== selectedStore) {
				return false;
			}

			// 2. Tipo de repartidor
			if (filtroTipo !== "TODOS") {
				if (filtroTipo === "TEMPORAL" && driverTipo !== "TEMPORAL") return false;
				if (filtroTipo === "DIRECTO" && driverTipo === "TEMPORAL") return false;
			}

			// 3. Biometría
			if (soloBiometria && !driverBiometria) {
				return false;
			}

			const esInactivo = reg ? !!reg.esInactivo : !!$(rowNode).data('es_inactivo');

			// 4. Estado Operativo (En Tienda vs En Ruta vs Fuera de Tienda vs Activos vs Inactivos)
			if (filtroEstado !== "TODOS") {
				if (filtroEstado === "ACTIVOS" && esInactivo) return false;
				if (filtroEstado === "INACTIVOS" && !esInactivo) return false;
				if (filtroEstado === "EN_TIENDA" && driverEstado !== "EN_TIENDA") return false;
				if (filtroEstado === "EN_RUTA" && driverEstado !== "EN_RUTA") return false;
				if (filtroEstado === "FUERA_DE_TIENDA" && driverEstado !== "FUERA_DE_TIENDA" && driverEstado !== "SIN_PEDIDOS") return false;
			}

			return true;
		}
		return true;
	});

	// Filtro por Tipo de Repartidor en Historial (Búsqueda instantánea en DataTables)
	$.fn.dataTable.ext.search.push(function(settings, data, dataIndex, rowData, counter) {
		if (settings.sTableId === 'table-historial') {
			const filtro = $('#filtroTipoRepartidorHistorial').val() || 'TODOS';
			if (filtro === 'TODOS') return true;
			const node = tableHistorial.row(dataIndex).node();
			const tipo = $(node).data('tipo_repartidor') || '';
			if (filtro === 'TEMPORAL') return tipo === 'TEMPORAL';
			if (filtro === 'DIRECTO') return tipo !== 'TEMPORAL';
		}
		return true;
	});

	$('#filtroTipoRepartidorHistorial').change(function() {
		tableHistorial.draw();
	});

	selectTiendaHist.change(function() {
		tiendaidH = $(this).val();
	});

	filter.click(() => {
		const tiendaId = $("#tiendaHistorial").val() || 0;
		const fInicio = startDate.val();
		const fFin = endDate.val();
		if (!fInicio || !fFin) {
			Swal.fire({
				icon: 'warning',
				title: 'Fechas requeridas',
				text: 'Por favor selecciona la fecha inicial y final para filtrar.'
			});
			return;
		}
		EnvioDatos({ tiendaId: tiendaId, action: "historial", startDate: fInicio, endDate: fFin });
	});


	const socket = io("http://172.19.0.25:8082", { 
		reconnection: true, 
		reconnectionAttempts: Infinity, 
		reconnectionDelay: 2000,
		reconnectionDelayMax: 10000,
		timeout: 8000
	});

	function actualizarEstadoBadgeConexion(conectado) {
		const $badge = $('.badge-live-pulse');
		if (conectado) {
			$badge.removeClass('badge-live-disconnected').html('<span class="pulse-dot"></span> EN VIVO');
		} else {
			$badge.addClass('badge-live-disconnected').html('<span class="pulse-dot-disconnected"></span> RECONECTANDO...');
		}
	}

	// Escuchar datos desde el servidor
	socket.on('updateLocation', (data) => {
		if (!data || !data.clave_usuario) return;
		const { clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda, tipo_repartidor, empresa_temporal, en_turno_biometria, estado, bateria, velocidad, pedidos_activos, pedidos_detalle } = data;
		const selectedStore = String(selectTienda.val() || "0");
		const driverStore = String(idtienda || "");

		// Si el monitor tiene una tienda seleccionada y el evento es de otra tienda, descartar inmediatamente
		if (selectedStore !== "0" && driverStore !== selectedStore) {
			return;
		}

		updateRowAndMarker(clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda, tipo_repartidor, empresa_temporal, en_turno_biometria, estado, bateria, velocidad, pedidos_activos, pedidos_detalle);
	});


	let yaMostroErrorSocket = false;

	// Cuando se conecta exitosamente (por primera vez o tras reconectar)
	socket.on("connect", () => {
		actualizarEstadoBadgeConexion(true);
		const salaActual = selectTienda.val() || tiendaActualSocket || "0";
		socket.emit('unirse_tienda', salaActual);

		if (yaMostroErrorSocket) {
			// Sincronizar datos frescos del backend por si hubo cambios durante la desconexión
			EnvioDatos({ tiendaId: salaActual, action: "rastreo" });
			Swal.fire({
				icon: 'success',
				title: 'Conexión restablecida',
				text: 'La conexión en tiempo real ha sido restablecida.',
				timer: 2500,
				showConfirmButton: false
			});
		}
		yaMostroErrorSocket = false; // Reinicia el estado
	});

	// Si se desconecta o hay error de conexión
	socket.on("disconnect", () => {
		actualizarEstadoBadgeConexion(false);
	});

	socket.on("connect_error", (err) => {
		actualizarEstadoBadgeConexion(false);
		console.warn("Error de conexión Socket.io:", err.message);

		if (!yaMostroErrorSocket) {
			yaMostroErrorSocket = true;
		}
	});



	// Paginación
	let paginaActual = 0;
	const paginas = ["#pagina-mapa", "#pagina-historial"];
	const botones = { anterior: $("#anterior"), siguiente: $("#siguiente") };

	function actualizarPaginador() {
		paginas.forEach((id, index) => {
			$(id).toggleClass("pagina-activa", index === paginaActual)
				.toggleClass("pagina-oculta", index !== paginaActual);
		});
		botones.anterior.prop("disabled", paginaActual === 0);
		botones.siguiente.prop("disabled", paginaActual === paginas.length - 1);
	}

	botones.anterior.click(() => { if (paginaActual > 0) { paginaActual--; actualizarPaginador(); } map.invalidateSize(); });
	botones.siguiente.click(() => { if (paginaActual < paginas.length - 1) { paginaActual++; actualizarPaginador(); } });

	actualizarPaginador();

	function EnvioDatos(data) {
		if (data.action === "historial") {
			$('#filter').prop('disabled', true).html('<i class="fas fa-spinner fa-spin mr-1"></i> Consultando...');
		}

		$.ajax({
			url: "/ProyectoPizzaAmericana/HistorialUbicacion",
			method: "POST",
			data: data,
			complete: function() {
				if (data.action === "historial") {
					$('#filter').prop('disabled', false).html('<i class="fas fa-filter mr-1"></i> Filtrar');
				}
			},
			success: function(response) {

				if (data.action == "rastreo") {
					table.clear();
					mapaConductores.forEach(reg => {
						if (reg.marker) reg.marker.remove();
					});
					mapaConductores.clear();
					markers = {};

					response.forEach(location => {
						const { clave_usuario, latitud, longitud, fecha, nombre_usuario, idtienda, tipo_repartidor, empresa_temporal, en_turno_biometria, estado, bateria, velocidad, pedidos_activos, pedidos_detalle } = location;
						updateRowAndMarker(clave_usuario, latitud, longitud, fecha, nombre_usuario, idtienda, tipo_repartidor, empresa_temporal, en_turno_biometria, estado, bateria, velocidad, pedidos_activos, pedidos_detalle, false);
					});

					aplicarFiltrosLive(true);
					restablecerFichaDetalles();

				} else if (data.action == "historial") {

					tableHistorial.clear();  // Limpiar la tabla

					response.forEach(function(item) {
						const badgeTipo = generarBadgeTipoRepartidor(item.tipo_repartidor, item.empresa_temporal);
						const celdaUsuario = `
							<div class="domi-nombre-cell" data-clave="${item.clave_usuario}" data-idtienda="${item.idtienda}" data-tipo="${item.tipo_repartidor}" data-empresa="${item.empresa_temporal || ''}" data-nombre="${item.nombre_usuario}">
								<span class="domi-nombre-texto">${item.nombre_usuario}</span>
								${badgeTipo}
							</div>
						`;
						const newRow = tableHistorial.row.add([
							celdaUsuario,          // Columna visible con nombre y etiqueta
							item.fecha,            // Columna visible
							item.tienda            // Columna visible
						]);
						const node = newRow.node();
						if (node) {
							$(node).attr('data-clave_usuario', item.clave_usuario)
								   .attr('data-idtienda', item.idtienda)
								   .attr('data-nombre', item.nombre_usuario)
								   .attr('data-tipo_repartidor', item.tipo_repartidor)
								   .attr('data-empresa_temporal', item.empresa_temporal || '')
								   .data('clave_usuario', item.clave_usuario)
								   .data('idtienda', item.idtienda);
						}
					});

					tableHistorial.draw(false);
				} else if (data.action == "detalle") {
					Swal.close();
					cargarDetalleHistorico(
						response,
						window._ultimoUsuarioSeleccionado || "Domiciliario",
						window._ultimaTiendaSeleccionada || "Tienda",
						window._ultimaFechaSeleccionada || ""
					);
				}
			},
			error: function(xhr, status, error) {
				console.error("Error al obtener los datos:", error);
				Swal.close();
				if (data.action === "historial") {
					Swal.fire({
						icon: 'error',
						title: 'Error de consulta',
						text: 'No fue posible consultar el historial. Por favor intenta de nuevo.'
					});
				} else if (data.action === "detalle") {
					Swal.fire({
						icon: 'error',
						title: 'Error al cargar detalle',
						text: 'No fue posible obtener el recorrido del domiciliario. Por favor verifica que el servidor esté activo.'
					});
				}
			}
		});

	}


	// =====================================================================
	// CENTRO DE TELEMETRÍA Y SIMULADOR DE RUTAS (HISTORIAL)
	// =====================================================================
	let puntosHistorico = [];
	let puntosFiltrados = [];
	let indicePuntoActual = 0;
	let timerSimulacion = null;
	let velocidadSimulacion = 1;
	let polylineRuta = null;
	let polylineGlow = null;
	let markerMotoSimulacion = null;
	let markerInicio = null;
	let markerFin = null;
	let markersParadas = [];
	let markersEntregas = [];
	let polylinesSaltos = [];
	let rutaVisible = true;
	let entregasVisibles = true;

	function actualizarVisibilidadEntregas() {
		if (entregasVisibles) {
			markersEntregas.forEach(m => {
				if (!map_detalle.hasLayer(m)) {
					m.addTo(map_detalle);
				}
			});
			$('#btnToggleEntregas')
				.removeClass('btn-outline-secondary')
				.addClass('btn-outline-success')
				.html(`<i class="fas fa-box mr-1"></i> Pedidos (${markersEntregas.length})`);
		} else {
			markersEntregas.forEach(m => {
				if (map_detalle.hasLayer(m)) {
					map_detalle.removeLayer(m);
				}
			});
			$('#btnToggleEntregas')
				.removeClass('btn-outline-success')
				.addClass('btn-outline-secondary')
				.html('<i class="fas fa-eye-slash mr-1"></i> Pedidos Ocultos');
		}
	}

	// Variables para ajuste de ruta a las calles reales (OSRM Map Matching / Routing)
	let rutaAjustadaACalles = false;
	let latLngsDirectos = [];
	let latLngsCalles = [];
	let puntosActualesDetalle = [];

	function actualizarBotonToggleRuta() {
		if (rutaVisible) {
			$('#btnToggleRuta')
				.removeClass('btn-primary')
				.addClass('btn-outline-primary')
				.html('<i class="fas fa-eye-slash mr-1"></i> Ocultar Ruta');
		} else {
			$('#btnToggleRuta')
				.removeClass('btn-outline-primary')
				.addClass('btn-primary')
				.html('<i class="fas fa-eye mr-1"></i> Mostrar Ruta');
		}
	}

	// Detecta si entre dos puntos consecutivos hubo un bache de señal o salto atípico
	function esSaltoSenal(pA, pB) {
		if (!pA || !pB || !pA.fecha || !pB.fecha) return false;
		const tA = new Date(pA.fecha).getTime();
		const tB = new Date(pB.fecha).getTime();
		const diffMin = (tB - tA) / (1000 * 60);
		const posA = L.latLng(parseFloat(pA.latitud), parseFloat(pA.longitud));
		const posB = L.latLng(parseFloat(pB.latitud), parseFloat(pB.longitud));
		const distM = posA.distanceTo(posB);

		// Se considera pérdida de señal si pasaron >= 5.5 minutos sin reporte,
		// o si en >= 2.5 minutos hubo un salto de más de 2500 metros
		return (diffMin >= 5.5) || (diffMin >= 2.5 && distM > 2500);
	}

	// Segmenta los puntos en bloques continuos confiables para no unir con calles falsas los baches de señal
	function segmentarPuntosEnBloques(puntos) {
		if (!puntos || puntos.length === 0) return { bloques: [], saltos: [] };
		const bloques = [];
		const saltos = [];
		let bloqueActual = [puntos[0]];

		for (let i = 0; i < puntos.length - 1; i++) {
			const pActual = puntos[i];
			const pSig = puntos[i + 1];

			if (esSaltoSenal(pActual, pSig)) {
				bloques.push(bloqueActual);
				const tA = new Date(pActual.fecha).getTime();
				const tB = new Date(pSig.fecha).getTime();
				const diffMin = Math.round((tB - tA) / (1000 * 60));
				const distKm = (L.latLng(parseFloat(pActual.latitud), parseFloat(pActual.longitud))
					.distanceTo(L.latLng(parseFloat(pSig.latitud), parseFloat(pSig.longitud))) / 1000).toFixed(1);

				saltos.push({
					desde: pActual,
					hasta: pSig,
					diffMin: diffMin,
					distKm: distKm
				});

				bloqueActual = [pSig];
			} else {
				bloqueActual.push(pSig);
			}
		}

		if (bloqueActual.length > 0) {
			bloques.push(bloqueActual);
		}

		return { bloques, saltos };
	}

	let idPeticionCallesActual = 0;

	async function traerRutaAjustadaACalles(puntosBase) {
		if (!puntosBase || puntosBase.length < 2) return;

		const peticionId = ++idPeticionCallesActual;
		const $btn = $('#btnAjustarCalles');
		$btn.prop('disabled', true)
			.removeClass('btn-primary btn-outline-primary btn-success')
			.addClass('btn-outline-info')
			.html('<i class="fas fa-spinner fa-spin mr-1"></i> Trazando calles...');

		// Segmentar en bloques continuos para que OSRM NUNCA invente calles sobre un bache de desconexión
		const { bloques } = segmentarPuntosEnBloques(puntosBase);

		try {
			const promesasBloques = bloques.map(async (bloque, bIdx) => {
				if (bloque.length < 2) {
					return bloque.map(p => [parseFloat(p.latitud), parseFloat(p.longitud)]);
				}

				// Dividir el bloque en trozos secuenciales de máximo 18 waypoints con 1 de solapamiento
				const CHUNK_SIZE = 18;
				const chunks = [];
				for (let i = 0; i < bloque.length - 1; i += CHUNK_SIZE) {
					const end = Math.min(bloque.length, i + CHUNK_SIZE + 1);
					chunks.push(bloque.slice(i, end));
				}

				const promesasChunks = chunks.map((chunk, idx) => {
					const coordsStr = chunk.map(p => `${parseFloat(p.longitud).toFixed(5)},${parseFloat(p.latitud).toFixed(5)}`).join(';');
					const url = `https://router.project-osrm.org/route/v1/driving/${coordsStr}?overview=full&geometries=geojson`;

					return fetch(url)
						.then(r => r.json())
						.then(res => {
							if (res && res.code === 'Ok' && res.routes && res.routes.length > 0) {
								return res.routes[0].geometry.coordinates.map(c => [c[1], c[0]]);
							}
							return chunk.map(p => [parseFloat(p.latitud), parseFloat(p.longitud)]);
						})
						.catch(err => {
							console.warn(`Chunk ${idx} del bloque ${bIdx} falló OSRM, usando directo:`, err);
							return chunk.map(p => [parseFloat(p.latitud), parseFloat(p.longitud)]);
						});
				});

				const resultadosChunks = await Promise.all(promesasChunks);
				const coordsBloque = [];
				resultadosChunks.forEach((coordsChunk, idx) => {
					if (idx === 0) {
						coordsBloque.push(...coordsChunk);
					} else {
						coordsBloque.push(...coordsChunk.slice(1));
					}
				});
				return coordsBloque;
			});

			const segmentosCalles = await Promise.all(promesasBloques);
			if (peticionId !== idPeticionCallesActual) return;

			const segmentosValidos = segmentosCalles.filter(seg => seg && seg.length > 0);

			if (segmentosValidos.length > 0) {
				latLngsCalles = segmentosValidos.length === 1 ? segmentosValidos[0] : segmentosValidos;
				rutaAjustadaACalles = true;

				if (polylineRuta && rutaVisible) {
					polylineRuta.setLatLngs(latLngsCalles);
					if (polylineGlow) polylineGlow.setLatLngs(latLngsCalles);
				}

				$btn.prop('disabled', false)
					.removeClass('btn-outline-info btn-outline-primary')
					.addClass('btn-success')
					.html('<i class="fas fa-road mr-1"></i> Calles (Activo)');
			} else {
				$btn.prop('disabled', false)
					.removeClass('btn-outline-info btn-success')
					.addClass('btn-outline-primary')
					.html('<i class="fas fa-road mr-1"></i> Ajustar a Calles');
			}
		} catch (errGral) {
			console.error("Error al obtener ruta por calles:", errGral);
			if (peticionId === idPeticionCallesActual) {
				$btn.prop('disabled', false)
					.removeClass('btn-outline-info btn-success')
					.addClass('btn-outline-primary')
					.html('<i class="fas fa-road mr-1"></i> Ajustar a Calles');
			}
		}
	}

	function alternarAjusteCallesOSRM() {
		if (!polylineRuta || !puntosActualesDetalle || puntosActualesDetalle.length < 2) {
			Swal.fire({ icon: 'info', text: 'No hay suficientes puntos de recorrido para ajustar a las calles.' });
			return;
		}

		if (rutaAjustadaACalles) {
			// Volver al trazo directo de GPS
			polylineRuta.setLatLngs(latLngsDirectos);
			if (polylineGlow) polylineGlow.setLatLngs(latLngsDirectos);
			rutaAjustadaACalles = false;
			$('#btnAjustarCalles')
				.removeClass('btn-success')
				.addClass('btn-outline-primary')
				.html('<i class="fas fa-road mr-1"></i> Ajustar a Calles');
		} else {
			// Activar trazo ajustado a calles
			if (latLngsCalles && latLngsCalles.length > 0) {
				polylineRuta.setLatLngs(latLngsCalles);
				if (polylineGlow) polylineGlow.setLatLngs(latLngsCalles);
				rutaAjustadaACalles = true;
				$('#btnAjustarCalles')
					.removeClass('btn-outline-primary')
					.addClass('btn-success')
					.html('<i class="fas fa-road mr-1"></i> Calles (Activo)');
			} else {
				traerRutaAjustadaACalles(puntosActualesDetalle);
			}
		}
	}

	function limpiarMapaDetalle() {
		pausarSimulacion();
		idPeticionCallesActual++; // Cancela peticiones pendientes de calles en vuelo
		if (polylineRuta && map_detalle.hasLayer(polylineRuta)) map_detalle.removeLayer(polylineRuta);
		if (polylineGlow && map_detalle.hasLayer(polylineGlow)) map_detalle.removeLayer(polylineGlow);
		if (markerMotoSimulacion && map_detalle.hasLayer(markerMotoSimulacion)) map_detalle.removeLayer(markerMotoSimulacion);
		if (markerInicio && map_detalle.hasLayer(markerInicio)) map_detalle.removeLayer(markerInicio);
		if (markerFin && map_detalle.hasLayer(markerFin)) map_detalle.removeLayer(markerFin);
		markersParadas.forEach(m => {
			if (map_detalle.hasLayer(m)) map_detalle.removeLayer(m);
		});
		entregasVisibles = true;
		markersEntregas.forEach(m => {
			if (map_detalle.hasLayer(m)) map_detalle.removeLayer(m);
		});
		markersEntregas = [];
		$('#btnToggleEntregas')
			.removeClass('btn-outline-secondary')
			.addClass('btn-outline-success')
			.html('<i class="fas fa-box mr-1"></i> Pedidos (0)');
		polylinesSaltos.forEach(p => {
			if (map_detalle.hasLayer(p)) map_detalle.removeLayer(p);
		});
		polylinesSaltos = [];
		polylineRuta = null;
		polylineGlow = null;
		markerMotoSimulacion = null;
		markerInicio = null;
		markerFin = null;
		rutaAjustadaACalles = false;
		latLngsDirectos = [];
		latLngsCalles = [];
		puntosActualesDetalle = [];
		$('#btnAjustarCalles')
			.prop('disabled', false)
			.removeClass('btn-primary btn-success btn-outline-info')
			.addClass('btn-outline-primary')
			.html('<i class="fas fa-road mr-1"></i> Ajustar a Calles');
	}

	function cargarDetalleHistorico(datos, usuarioNombre, tiendaNombre, fechaDia) {
		limpiarMapaDetalle();
		rutaVisible = true;
		actualizarBotonToggleRuta();

		const tipoRep = (datos && datos[0] && datos[0].tipo_repartidor) || window._ultimoTipoRepartidor || "OTRO";
		const empTemp = (datos && datos[0] && datos[0].empresa_temporal) || window._ultimaEmpresaTemporal || "";
		const badgeModal = generarBadgeTipoRepartidor(tipoRep, empTemp);

		// Actualizar encabezados del modal
		$('#modal-usuario-nombre').text(usuarioNombre);
		$('#modal-tienda-nombre').html(`<i class="fas fa-store mr-1"></i> ${tiendaNombre} <span class="ml-2">${badgeModal}</span>`);
		$('#modal-fecha-dia').html(`<i class="far fa-calendar-alt mr-1"></i> ${fechaDia}`);

		// Filtrar y ordenar puntos cronológicamente
		puntosHistorico = (datos || []).filter(item => {
			const lat = parseFloat(item.latitud);
			const lng = parseFloat(item.longitud);
			return !isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0 && item.fecha;
		}).sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime());

		if (puntosHistorico.length === 0) {
			Swal.fire({
				icon: 'info',
				title: 'Sin recorrido registrado',
				html: `El domiciliario <b>${usuarioNombre}</b> aún no registra puntos GPS para el día <b>${fechaDia}</b>.`,
				confirmButtonColor: '#007bff'
			});
			return;
		}

		puntosFiltrados = [...puntosHistorico];

		// Pre-llenar selector de horas con la hora inicial y final del día
		const pIni = puntosHistorico[0];
		const pFin = puntosHistorico[puntosHistorico.length - 1];
		const hora1 = pIni.fecha.split(' ')[1] ? pIni.fecha.split(' ')[1].substring(0, 5) : "00:00";
		const hora2 = pFin.fecha.split(' ')[1] ? pFin.fecha.split(' ')[1].substring(0, 5) : "23:59";
		$('#filtroHoraDesde').val(hora1);
		$('#filtroHoraHasta').val(hora2);
		$('#resumenFiltroHoras').text(`Jornada completa: ${hora1} a ${hora2}`);

		$('#modalDetalles').modal('show');
		try {
			renderizarRecorrido(puntosFiltrados);
		} catch (eRec) {
			console.error("Error al renderizar recorrido:", eRec);
		}

		// Consultar y renderizar despachos del domiciliario desde Datamart
		consultarYRenderizarDespachosHistorial(window._ultimaClaveSeleccionada, fechaDia, puntosHistorico);
	}

	function consultarYRenderizarDespachosHistorial(claveRapida, fechaDia, puntos) {
		$('#badgeTotalDespachos').text('0');
		$('#lista-despachos-body').html('<div class="text-center text-muted py-3 small"><i class="fas fa-spinner fa-spin mr-1"></i> Consultando despachos en Datamart...</div>');

		if (!claveRapida || !fechaDia) {
			$('#lista-despachos-body').html('<div class="text-center text-muted py-3 small">No hay información de domiciliario para consultar despachos.</div>');
			return;
		}

		$.ajax({
			url: "/ProyectoPizzaAmericana/HistorialUbicacion",
			method: "POST",
			data: {
				action: "despachos_historial",
				startDate: fechaDia,
				claveRapida: String(claveRapida)
			},
			success: function(response) {
				if (!response || !Array.isArray(response) || response.length === 0) {
					$('#badgeTotalDespachos').text('0');
					$('#lista-despachos-body').html(`
						<div class="text-center text-muted py-4 px-2">
							<i class="fas fa-box-open fa-2x mb-2 text-secondary" style="opacity: 0.5;"></i>
							<p class="small mb-0">No se registraron despachos para este domiciliario en esta fecha.</p>
						</div>
					`);
					return;
				}

				// Agrupar por despacho_id preservando orden cronológico
				const despachosMap = new Map();
				response.forEach(item => {
					const dId = item.despacho_id;
					if (!despachosMap.has(dId)) {
						despachosMap.set(dId, {
							despacho_id: dId,
							idtienda: item.idtienda,
							tienda: item.tienda || `Tienda ${item.idtienda}`,
							hora_salida: item.hora_salida || "--:--",
							hora_regreso: item.hora_regreso || null,
							pedidos: []
						});
					}
					if (item.id_pedido) {
						despachosMap.get(dId).pedidos.push({
							id_pedido: item.id_pedido,
							orden_planificada: item.orden_planificada,
							hora_entrega: item.hora_entrega
						});
					}
				});

				$('#badgeTotalDespachos').text(despachosMap.size);

				const dHoy = new Date();
				const hoyStr = `${dHoy.getFullYear()}-${String(dHoy.getMonth() + 1).padStart(2, '0')}-${String(dHoy.getDate()).padStart(2, '0')}`;
				const esFechaPasada = fechaDia && fechaDia < hoyStr;

				let htmlCards = '';
				despachosMap.forEach(despacho => {
					const estadoRegreso = despacho.hora_regreso 
						? `<span class="badge badge-success small"><i class="fas fa-check-circle mr-1"></i>Retorno: ${String(despacho.hora_regreso).replace('.0', '')}</span>`
						: (esFechaPasada 
							? `<span class="badge badge-secondary small" title="El domiciliario culminó su turno o no se registró el retorno en el POS"><i class="fas fa-flag-checkered mr-1"></i>Sin retorno registrado</span>` 
							: `<span class="badge badge-warning text-dark small"><i class="fas fa-motorcycle mr-1"></i>En ruta</span>`);

					const fechaSalida = despacho.hora_salida ? String(despacho.hora_salida).split(' ')[0] : '';
					const tienePedidosSinEntrega = despacho.pedidos.some(p => !p.hora_entrega);
					const alertaDespacho = tienePedidosSinEntrega 
						? `<span class="badge badge-warning text-dark small ml-1 border border-warning" title="Este despacho tiene pedidos que salieron pero no registran confirmación de entrega en el POS"><i class="fas fa-exclamation-triangle text-danger mr-1"></i>Revisar entrega</span>`
						: '';

					let pedidosHtml = '';
					if (despacho.pedidos.length === 0) {
						pedidosHtml = `
							<div class="text-muted small p-2 bg-light border rounded text-center">
								<i class="fas fa-info-circle mr-1 text-secondary"></i>Despacho sin pedidos asociados registrados en el POS.
							</div>
						`;
					} else {
						despacho.pedidos.forEach(p => {
							const horaTexto = p.hora_entrega ? String(p.hora_entrega).replace('.0', '').trim() : null;
							let fechaEntrega = '';
							let soloHoraEntrega = horaTexto;
							if (horaTexto && horaTexto.includes(' ')) {
								fechaEntrega = horaTexto.split(' ')[0];
								soloHoraEntrega = horaTexto.split(' ')[1];
							}

							let entregaInfo = '';
							let extraClass = '';
							if (!horaTexto) {
								extraClass = 'pedido-alerta-sin-entrega';
								entregaInfo = `<span class="badge badge-warning text-dark font-weight-bold" title="El pedido salió de tienda pero no registra fecha/hora de entrega en el POS"><i class="fas fa-exclamation-triangle text-danger mr-1"></i>Sin entrega registrada</span>`;
							} else if (fechaEntrega && fechaSalida && fechaEntrega !== fechaSalida) {
								entregaInfo = `<span class="badge badge-info text-white font-weight-bold" title="Entregado al día siguiente (${fechaEntrega})"><i class="fas fa-moon mr-1"></i>Día siguiente: ${soloHoraEntrega}</span>`;
							} else {
								entregaInfo = `<span class="text-success small font-weight-bold"><i class="fas fa-check-double mr-1"></i>Entregado: ${soloHoraEntrega || horaTexto}</span>`;
							}

							pedidosHtml += `
								<div class="d-flex align-items-center justify-content-between p-1 px-2 border-bottom bg-light rounded mb-1 pedido-item-click ${extraClass}" 
									 data-pedido="${p.id_pedido}" 
									 data-hora="${p.hora_entrega || ''}"
									 style="cursor: pointer;">
									<span class="badge-pedido-chip mb-0"><i class="fas fa-box mr-1"></i>#${p.id_pedido}</span>
									<div>${entregaInfo}</div>
								</div>
							`;

							// Si tiene hora_entrega y hay puntos GPS disponibles, colocar el pin en el mapa
							if (p.hora_entrega && puntos && puntos.length > 0) {
								ubicarMarcadorEntregaEnMapa(p, despacho, puntos);
							}
						});
					}

					htmlCards += `
						<div class="card-despacho-item mb-2 p-2 border rounded shadow-sm">
							<div class="d-flex align-items-center justify-content-between mb-1">
								<strong class="text-primary small"><i class="fas fa-shipping-fast mr-1"></i>Despacho #${despacho.despacho_id} ${alertaDespacho}</strong>
								<span class="badge badge-light border small text-muted">${despacho.tienda}</span>
							</div>
							<div class="d-flex align-items-center justify-content-between text-muted small mb-2">
								<span><i class="far fa-clock mr-1 text-info"></i>Salida: <b>${String(despacho.hora_salida).replace('.0', '')}</b></span>
								${estadoRegreso}
							</div>
							<div class="pedidos-despacho-lista">
								${pedidosHtml}
							</div>
						</div>
					`;
				});

				$('#lista-despachos-body').html(htmlCards);
				actualizarVisibilidadEntregas();
			},
			error: function(err) {
				console.error("Error consultando despachos historial:", err);
				$('#lista-despachos-body').html('<div class="text-center text-danger py-3 small"><i class="fas fa-exclamation-triangle mr-1"></i> Error al cargar despachos de Datamart.</div>');
			}
		});
	}

	function ubicarMarcadorEntregaEnMapa(pedido, despacho, puntos) {
		if (!pedido.hora_entrega || !puntos || puntos.length === 0) return;

		let tEntregaMs = NaN;
		const strEntrega = String(pedido.hora_entrega).replace('.0', '').trim();
		if (strEntrega.includes(' ')) {
			tEntregaMs = new Date(strEntrega.replace(' ', 'T')).getTime();
		} else if (despacho.hora_salida && despacho.hora_salida.includes(' ')) {
			const fBase = despacho.hora_salida.split(' ')[0];
			tEntregaMs = new Date(`${fBase}T${strEntrega}`).getTime();
		}

		if (isNaN(tEntregaMs)) return;

		let puntoMasCercano = null;
		let menorDiferenciaSeg = Infinity;

		for (let i = 0; i < puntos.length; i++) {
			const pt = puntos[i];
			if (!pt.fecha) continue;
			const tPtMs = new Date(pt.fecha.replace(' ', 'T')).getTime();
			if (isNaN(tPtMs)) continue;
			const diffSec = Math.abs(tPtMs - tEntregaMs) / 1000;

			if (diffSec < menorDiferenciaSeg) {
				menorDiferenciaSeg = diffSec;
				puntoMasCercano = pt;
				if (diffSec < 15) break; // Coincidencia de alta precisión (<15s)
			}
		}

		// Aceptar coincidencia si está dentro de 20 minutos (1200 seg) del recorrido GPS
		if (puntoMasCercano && menorDiferenciaSeg <= 1200) {
			const lat = parseFloat(puntoMasCercano.latitud);
			const lng = parseFloat(puntoMasCercano.longitud);
			if (isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) return;

			let horaEntregaTexto = strEntrega.includes(' ') ? strEntrega.split(' ')[1] : strEntrega;
			let fechaEntrega = strEntrega.includes(' ') ? strEntrega.split(' ')[0] : '';
			let esOtroDia = false;
			if (despacho.hora_salida && despacho.hora_salida.includes(' ')) {
				const fSalida = despacho.hora_salida.split(' ')[0];
				if (fechaEntrega && fSalida && fechaEntrega !== fSalida) {
					esOtroDia = true;
				}
			}

			const labelHora = esOtroDia ? `🌙 ${horaEntregaTexto}` : horaEntregaTexto;

			const iconEntrega = L.divIcon({
				className: 'custom-pin-entrega-container',
				html: `
					<div class="pin-entrega-compacto" id="pin-pedido-${pedido.id_pedido}" title="Pedido #${pedido.id_pedido}">
						<span class="pin-dot"></span>
						<span class="pin-num">#${pedido.id_pedido}</span>
					</div>
				`,
				iconSize: [66, 20],
				iconAnchor: [33, 10]
			});

			const marker = L.marker([lat, lng], { icon: iconEntrega });

			// Tooltip instantáneo en hover (muy compacto, no estorba)
			marker.bindTooltip(`
				<div class="small font-weight-bold">
					<i class="fas fa-box text-success mr-1"></i>Pedido #${pedido.id_pedido}<br>
					<span class="text-muted"><i class="far fa-clock mr-1"></i>${labelHora}</span>
				</div>
			`, { direction: 'top', offset: [0, -8], opacity: 0.95 });

			// Popup al hacer click
			marker.bindPopup(`
				<div class="p-1">
					<div class="d-flex align-items-center justify-content-between mb-1">
						<strong class="text-primary"><i class="fas fa-box-open mr-1"></i>Pedido #${pedido.id_pedido}</strong>
						<span class="badge badge-light border small text-muted">${despacho.tienda}</span>
					</div>
					<div class="small text-muted mb-1">
						<i class="far fa-clock mr-1 text-info"></i>Hora entrega: <b>${strEntrega}</b>
						${esOtroDia ? '<br><span class="badge badge-info text-white mt-1"><i class="fas fa-moon mr-1"></i>Entregado al día siguiente</span>' : ''}
					</div>
					<div class="small text-secondary">
						<i class="fas fa-shipping-fast mr-1"></i>Despacho #${despacho.despacho_id}
					</div>
				</div>
			`);

			if (entregasVisibles) {
				marker.addTo(map_detalle);
			}

			markersEntregas.push(marker);
		}
	}

	$(document).on('click', '.pedido-item-click', function() {
		const numPedido = $(this).data('pedido');
		const hora = $(this).data('hora');
		let foundMarker = null;
		markersEntregas.forEach(m => {
			const popup = m.getPopup();
			if (popup && popup.getContent() && popup.getContent().includes(`#${numPedido}`)) {
				foundMarker = m;
			}
		});

		if (foundMarker) {
			// Si los marcadores estaban ocultos, activarlos para que el usuario pueda ver el punto seleccionado
			if (!entregasVisibles) {
				entregasVisibles = true;
				actualizarVisibilidadEntregas();
			}

			map_detalle.panTo(foundMarker.getLatLng());
			foundMarker.openPopup();

			// Resaltar visualmente el pin con clase temporal
			$('.pin-entrega-compacto').removeClass('pin-highlighted');
			const elPin = $(`#pin-pedido-${numPedido}`);
			if (elPin.length) {
				elPin.addClass('pin-highlighted');
			}
		} else if (!hora) {
			Swal.fire({
				toast: true,
				position: 'top-end',
				icon: 'warning',
				title: `Pedido #${numPedido}`,
				text: 'Este pedido no cuenta con hora de entrega registrada en el sistema.',
				showConfirmButton: false,
				timer: 3500
			});
		} else if (hora && puntosHistorico && puntosHistorico.length > 0) {
			let hLimpia = String(hora).replace('.0', '').trim();
			if (hLimpia.includes(' ')) hLimpia = hLimpia.split(' ')[1];
			const partes = hLimpia.split(':');
			if (partes.length >= 2) {
				const seg = parseInt(partes[0], 10) * 3600 + parseInt(partes[1], 10) * 60 + (parseInt(partes[2] || 0, 10));
				let idxCercano = 0;
				let minDiff = Infinity;
				puntosFiltrados.forEach((p, idx) => {
					const h = p.fecha.split(' ')[1];
					if (h) {
						const ptParts = h.split(':');
						const s = parseInt(ptParts[0], 10) * 3600 + parseInt(ptParts[1], 10) * 60 + (parseInt(ptParts[2] || 0, 10));
						const d = Math.abs(s - seg);
						if (d < minDiff) {
							minDiff = d;
							idxCercano = idx;
						}
					}
				});
				moverASimulacionIndice(idxCercano, true);
			}
		}
	});

	// Filtro inteligente para eliminar ruido GPS, saltos erráticos y telarañas cuando la moto está detenida
	function filtrarPuntosRutaLimpios(puntos) {
		if (!puntos || puntos.length <= 2) return puntos;

		const filtrados = [];
		let ancla = puntos[0];
		filtrados.push(ancla);

		for (let i = 1; i < puntos.length; i++) {
			const pt = puntos[i];
			const lat = parseFloat(pt.latitud);
			const lng = parseFloat(pt.longitud);
			if (isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) continue;

			const pAncla = L.latLng(parseFloat(ancla.latitud), parseFloat(ancla.longitud));
			const pActual = L.latLng(lat, lng);
			const distAncla = pAncla.distanceTo(pActual);

			const tAncla = new Date(ancla.fecha).getTime();
			const tActual = new Date(pt.fecha).getTime();
			const deltaSec = Math.max(1, (tActual - tAncla) / 1000);
			const velocidadKmh = (distAncla / deltaSec) * 3.6;

			// 1. Descartar picos erráticos de satélite o salto de antena (Teleport glitch):
			// Si la velocidad calculada es absurda (> 90 km/h) o el punto se dispara a > 160m y luego regresa
			if (i < puntos.length - 1) {
				const pSig = L.latLng(parseFloat(puntos[i+1].latitud), parseFloat(puntos[i+1].longitud));
				const distRetorno = pAncla.distanceTo(pSig);
				if ((distAncla > 150 && distRetorno < 65) || velocidadKmh > 95) {
					continue; // Glitch descartado
				}
			}

			// 2. Filtro de ancla estacionaria (elimina 100% las telarañas / garabatos mientras la moto está detenida):
			// Si el vehículo está a menos de 35 metros del ancla, o a menos de 55 metros con velocidad < 6 km/h,
			// significa que sigue en el mismo punto de parada / entrega / restaurante.
			const esMismaParada = (distAncla < 35) || (distAncla < 55 && velocidadKmh < 6);
			if (esMismaParada && i < puntos.length - 1) {
				continue; // No agregar vértices falsos en la misma parada
			}

			// Si el vehículo realmente inició desplazamiento:
			filtrados.push(pt);
			ancla = pt;
		}

		return filtrados.length >= 2 ? filtrados : puntos;
	}

	function renderizarRecorrido(puntos) {
		limpiarMapaDetalle();

		if (!puntos || puntos.length === 0) {
			$('#puntos-recorrido-body').html('<tr><td colspan="2" class="text-center text-muted py-3">No hay puntos en este rango de horas.</td></tr>');
			return;
		}

		// 1. Cálculos de telemetría (distancia, duración, paradas)
		let distanciaKm = 0;
		for (let i = 0; i < puntos.length - 1; i++) {
			const pA = L.latLng(puntos[i].latitud, puntos[i].longitud);
			const pB = L.latLng(puntos[i+1].latitud, puntos[i+1].longitud);
			distanciaKm += pA.distanceTo(pB) / 1000;
		}

		const tInicio = new Date(puntos[0].fecha).getTime();
		const tFin = new Date(puntos[puntos.length - 1].fecha).getTime();
		const diffMin = Math.max(0, Math.round((tFin - tInicio) / (1000 * 60)));
		const horas = Math.floor(diffMin / 60);
		const minutos = diffMin % 60;
		const duracionTexto = horas > 0 ? `${horas}h ${minutos}m` : `${minutos}m`;

		// Detección de paradas (> 3 minutos sin desplazamiento significativo < 25m)
		const paradas = [];
		for (let i = 0; i < puntos.length - 1; i++) {
			const p1 = puntos[i];
			const p2 = puntos[i+1];
			const d = L.latLng(p1.latitud, p1.longitud).distanceTo(L.latLng(p2.latitud, p2.longitud));
			const diffSec = (new Date(p2.fecha).getTime() - new Date(p1.fecha).getTime()) / 1000;
			if (d < 25 && diffSec >= 180) {
				paradas.push({
					latitud: p1.latitud,
					longitud: p1.longitud,
					fechaInicio: p1.fecha.split(' ')[1] || p1.fecha,
					fechaFin: p2.fecha.split(' ')[1] || p2.fecha,
					duracionMin: Math.round(diffSec / 60)
				});
			}
		}

		// 2. Actualizar tarjetas KPI
		$('#kpi-puntos').text(puntos.length);
		$('#kpi-distancia').text(distanciaKm.toFixed(1) + ' km');
		$('#kpi-duracion').text(duracionTexto);
		$('#kpi-paradas').text(paradas.length);
		$('#badgeTotalPuntosLista').text(puntos.length + ' puntos');

		// 3. Trazar polilínea de ruta limpia (sin garabatos de deriva ni picos falsos)
		const puntosLimpios = filtrarPuntosRutaLimpios(puntos);
		const { bloques, saltos } = segmentarPuntosEnBloques(puntosLimpios);

		// Construir trazo directo por bloques independientes (MultiPolyline)
		const bloquesDirectos = bloques.map(b => b.map(p => [parseFloat(p.latitud), parseFloat(p.longitud)]));
		latLngsDirectos = bloquesDirectos.length === 1 ? bloquesDirectos[0] : bloquesDirectos;
		latLngsCalles = [];
		puntosActualesDetalle = puntosLimpios;
		rutaAjustadaACalles = false;
		$('#btnAjustarCalles')
			.removeClass('btn-primary btn-success')
			.addClass('btn-outline-primary')
			.html('<i class="fas fa-road mr-1"></i> Ajustar a Calles');

		polylineGlow = L.polyline(latLngsDirectos, {
			color: '#60a5fa',
			weight: 7,
			opacity: 0.35,
			lineCap: 'round',
			lineJoin: 'round'
		});

		polylineRuta = L.polyline(latLngsDirectos, {
			color: '#1d4ed8',
			weight: 4,
			opacity: 0.95,
			lineCap: 'round',
			lineJoin: 'round'
		});

		// Crear trazos punteados de advertencia para cada salto o desconexión detectada
		saltos.forEach(salto => {
			const latLngA = [parseFloat(salto.desde.latitud), parseFloat(salto.desde.longitud)];
			const latLngB = [parseFloat(salto.hasta.latitud), parseFloat(salto.hasta.longitud)];
			const horaA = salto.desde.fecha.split(' ')[1] || salto.desde.fecha;
			const horaB = salto.hasta.fecha.split(' ')[1] || salto.hasta.fecha;

			const polySalto = L.polyline([latLngA, latLngB], {
				color: '#f59e0b',
				weight: 3,
				dashArray: '7, 9',
				opacity: 0.9
			}).bindTooltip(`
				<div style="font-size: 11.5px; line-height: 1.4;">
					<strong class="text-warning"><i class="fas fa-exclamation-triangle mr-1"></i>Pérdida de señal / Desconexión</strong><br>
					<strong>Tiempo sin reporte:</strong> ~${salto.diffMin} min<br>
					<strong>Distancia del salto:</strong> ${salto.distKm} km<br>
					<span class="text-muted">${horaA} &rarr; ${horaB}</span>
				</div>
			`, { sticky: true });

			polylinesSaltos.push(polySalto);
		});

		if (rutaVisible) {
			polylineGlow.addTo(map_detalle);
			polylineRuta.addTo(map_detalle);
			polylinesSaltos.forEach(p => p.addTo(map_detalle));
		}

		// Iniciar de forma automática e inmediata el trazado sobre las calles de cada bloque continuo
		traerRutaAjustadaACalles(puntosLimpios);

		// Ajustar vista del mapa a los límites de la ruta
		try {
			if (polylineRuta && polylineRuta.getBounds().isValid()) {
				map_detalle.fitBounds(polylineRuta.getBounds(), { padding: [40, 40], maxZoom: 18 });
			}
		} catch (eFit) {
			console.warn("Ajuste de límites:", eFit);
		}

		// 4. Marcador de la Motocicleta en Simulación (Único icono limpio, sin banderas ni pausas invasivas)
		markerMotoSimulacion = L.marker([parseFloat(puntos[0].latitud), parseFloat(puntos[0].longitud)], {
			icon: crearIconoMoto("Moto en Simulación"),
			zIndexOffset: 1000
		}).addTo(map_detalle);

		// 8. Configurar el slider de tiempo
		const ultIdx = puntos.length - 1;
		const hInicio = (puntos[0].fecha && puntos[0].fecha.includes(' ')) ? puntos[0].fecha.split(' ')[1].substring(0, 5) : (puntos[0].fecha || "00:00");
		const hFin = (puntos[ultIdx].fecha && puntos[ultIdx].fecha.includes(' ')) ? puntos[ultIdx].fecha.split(' ')[1].substring(0, 5) : (puntos[ultIdx].fecha || "23:59");
		$('#labelHoraInicio').text(hInicio);
		$('#labelHoraFin').text(hFin);
		$('#sliderTiempoRuta').attr('max', ultIdx).val(0);

		// 9. Llenar tabla de cronología de puntos con alertas visuales para baches de señal
		const tbody = $('#puntos-recorrido-body');
		tbody.empty();
		puntos.forEach((pt, index) => {
			if (index > 0 && esSaltoSenal(puntos[index - 1], pt)) {
				const tA = new Date(puntos[index - 1].fecha).getTime();
				const tB = new Date(pt.fecha).getTime();
				const diffM = Math.round((tB - tA) / (1000 * 60));
				const dKm = (L.latLng(parseFloat(puntos[index - 1].latitud), parseFloat(puntos[index - 1].longitud))
					.distanceTo(L.latLng(parseFloat(pt.latitud), parseFloat(pt.longitud))) / 1000).toFixed(1);

				tbody.append(`
					<tr class="fila-salto-alerta" style="background-color: #fffbeb; border-top: 1px dashed #f59e0b; border-bottom: 1px dashed #f59e0b;">
						<td colspan="2" class="py-1 px-2 text-center" style="font-size: 11px; color: #b45309;">
							<i class="fas fa-exclamation-triangle mr-1 text-warning"></i>
							<strong>Pérdida de señal: ~${diffM} min</strong> (${dKm} km)
						</td>
					</tr>
				`);
			}

			const hora = pt.fecha.split(' ')[1] || pt.fecha;
			const tr = $(`
				<tr class="fila-punto-recorrido" data-idx="${index}">
					<td class="align-middle">
						<span class="small font-weight-bold text-dark"><i class="far fa-clock text-muted mr-1"></i>${hora}</span>
					</td>
					<td class="text-right align-middle">
						<button class="btn btn-xs btn-outline-primary btn-ir-punto py-0 px-2 font-weight-bold" data-idx="${index}">
							<i class="fas fa-crosshairs mr-1"></i>Ir
						</button>
					</td>
				</tr>
			`);
			tbody.append(tr);
		});

		moverASimulacionIndice(0, false);
	}

	function moverASimulacionIndice(idx, centrarMapa = false) {
		if (!puntosFiltrados || puntosFiltrados.length === 0 || idx < 0 || idx >= puntosFiltrados.length) return;
		indicePuntoActual = idx;
		const pt = puntosFiltrados[idx];
		const lat = parseFloat(pt.latitud);
		const lng = parseFloat(pt.longitud);

		let targetLat = lat;
		let targetLng = lng;

		// Si el trazado de calles está activo, posicionar la moto sobre el asfalto más cercano
		if (rutaAjustadaACalles && latLngsCalles && latLngsCalles.length > 0) {
			let menorDistancia = Infinity;
			const posGps = L.latLng(lat, lng);
			const coordsCalles = (Array.isArray(latLngsCalles[0]) && Array.isArray(latLngsCalles[0][0]))
				? latLngsCalles.flat()
				: latLngsCalles;

			for (let i = 0; i < coordsCalles.length; i++) {
				const c = coordsCalles[i];
				if (!c || c.length < 2) continue;
				const d = posGps.distanceTo(L.latLng(c[0], c[1]));
				if (d < menorDistancia) {
					menorDistancia = d;
					targetLat = c[0];
					targetLng = c[1];
					if (d < 6) break;
				}
			}
		}

		// Deslizar suavemente el marcador de la moto por la calle
		if (markerMotoSimulacion) {
			animarMarcadorSuave(markerMotoSimulacion, targetLat, targetLng, Math.max(250, 600 / velocidadSimulacion));
		}

		// Actualizar slider y badges
		$('#sliderTiempoRuta').val(idx);
		const porcentaje = Math.round((idx / (puntosFiltrados.length - 1 || 1)) * 100);
		$('#badgeProgresoRuta').text(porcentaje + '%');
		const horaStr = pt.fecha.split(' ')[1] || pt.fecha;
		$('#badgeHoraActualRuta').html(`<i class="fas fa-clock mr-1"></i> ${horaStr}`);

		// Centrar el mapa si se solicitó o si el punto sale de la pantalla
		try {
			if (centrarMapa || (map_detalle.getBounds() && !map_detalle.getBounds().contains([targetLat, targetLng]))) {
				map_detalle.panTo([targetLat, targetLng]);
			}
		} catch (ePan) {
			// Ignorar si el mapa aún se está inicializando
		}

		// Resaltar y hacer scroll automático en la lista cronológica
		$('.fila-punto-recorrido').removeClass('fila-punto-activa');
		const filaActiva = $(`.fila-punto-recorrido[data-idx="${idx}"]`);
		if (filaActiva.length) {
			filaActiva.addClass('fila-punto-activa');
			const contenedor = $('.contenedor-lista-puntos');
			if (contenedor.length) {
				const topPos = filaActiva.position().top + contenedor.scrollTop() - 80;
				contenedor.scrollTop(topPos);
			}
		}

		// Geocodificar la dirección de este punto con debounce
		clearTimeout(window._timeoutGeocodeDetalle);
		window._timeoutGeocodeDetalle = setTimeout(() => {
			reverseGeocodeDetalle(lat, lng);
		}, 350);
	}

	function reverseGeocodeDetalle(lat, lng) {
		const token = accessToken || "AAPK211b4727a21c467cab976021a4014485adqFPyZ19VbYqn4_ZnjeAgaKts7YkcKdGxdFqB_ZcyEJasSP102byhIk3tVtW_IO";
		const url = `https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?f=pjson&location=${lng},${lat}&token=${token}`;
		fetch(url)
			.then(r => r.json())
			.then(data => {
				$('#textoDireccionDetalle').text(data.address?.LongLabel || `Ubicación detectada (Lat: ${lat.toFixed(5)}, Lng: ${lng.toFixed(5)})`);
			})
			.catch(() => {
				$('#textoDireccionDetalle').text(`Lat: ${lat.toFixed(5)}, Lng: ${lng.toFixed(5)}`);
			});
	}

	// Controles del reproductor
	function iniciarSimulacion() {
		if (indicePuntoActual >= puntosFiltrados.length - 1) {
			indicePuntoActual = 0;
		}
		$('#btnPlayRuta').removeClass('btn-success').addClass('btn-warning').html('<i class="fas fa-pause mr-1"></i> Pausar');
		const intervalMs = Math.max(120, Math.round(900 / velocidadSimulacion));
		timerSimulacion = setInterval(() => {
			if (indicePuntoActual < puntosFiltrados.length - 1) {
				moverASimulacionIndice(indicePuntoActual + 1);
			} else {
				pausarSimulacion();
			}
		}, intervalMs);
	}

	function pausarSimulacion() {
		if (timerSimulacion) {
			clearInterval(timerSimulacion);
			timerSimulacion = null;
		}
		$('#btnPlayRuta').removeClass('btn-warning').addClass('btn-success').html('<i class="fas fa-play mr-1"></i> Reproducir');
	}

	$('#btnPlayRuta').click(function() {
		if (timerSimulacion) {
			pausarSimulacion();
		} else {
			iniciarSimulacion();
		}
	});

	$('#btnResetRuta').click(function() {
		pausarSimulacion();
		moverASimulacionIndice(0, true);
	});

	// Selectores de velocidad
	$(document).on('click', '.btn-vel', function() {
		$('.btn-vel').removeClass('active');
		$(this).addClass('active');
		velocidadSimulacion = parseFloat($(this).data('vel')) || 1;
		if (timerSimulacion) {
			pausarSimulacion();
			iniciarSimulacion();
		}
	});

	// Slider de tiempo interactivo (scrubber)
	$('#sliderTiempoRuta').on('input', function() {
		pausarSimulacion();
		moverASimulacionIndice(parseInt(this.value));
	});

	// Botón "Ir" en la cronología
	$(document).on('click', '.btn-ir-punto', function() {
		pausarSimulacion();
		const idx = parseInt($(this).data('idx'));
		moverASimulacionIndice(idx, true);
	});

	// Botón para alternar visibilidad de la ruta en el mapa
	$('#btnToggleRuta').click(function() {
		rutaVisible = !rutaVisible;
		actualizarBotonToggleRuta();

		if (rutaVisible) {
			if (polylineGlow && !map_detalle.hasLayer(polylineGlow)) polylineGlow.addTo(map_detalle);
			if (polylineRuta && !map_detalle.hasLayer(polylineRuta)) polylineRuta.addTo(map_detalle);
			polylinesSaltos.forEach(p => { if (!map_detalle.hasLayer(p)) p.addTo(map_detalle); });
		} else {
			if (polylineRuta && map_detalle.hasLayer(polylineRuta)) map_detalle.removeLayer(polylineRuta);
			if (polylineGlow && map_detalle.hasLayer(polylineGlow)) map_detalle.removeLayer(polylineGlow);
			polylinesSaltos.forEach(p => { if (map_detalle.hasLayer(p)) map_detalle.removeLayer(p); });
		}
	});

	// Botón para alternar trazado vial ajustado a calles (OSRM)
	$('#btnAjustarCalles').click(function() {
		alternarAjusteCallesOSRM();
	});

	// Botón para alternar visibilidad de los marcadores de pedidos en el mapa
	$('#btnToggleEntregas').click(function() {
		entregasVisibles = !entregasVisibles;
		actualizarVisibilidadEntregas();
	});

	// Filtro por rango de horas
	$('#btnAplicarFiltroHoras').click(function() {
		const desde = $('#filtroHoraDesde').val();
		const hasta = $('#filtroHoraHasta').val();
		if (!desde || !hasta) {
			Swal.fire({ icon: 'warning', text: 'Ingresa la hora inicial y final para filtrar.' });
			return;
		}
		if (desde > hasta) {
			Swal.fire({ icon: 'warning', text: 'La hora inicial no puede ser posterior a la hora final.' });
			return;
		}

		const filtrados = puntosHistorico.filter(p => {
			const hora = p.fecha.split(' ')[1] ? p.fecha.split(' ')[1].substring(0, 5) : "";
			return hora >= desde && hora <= hasta;
		});

		if (filtrados.length === 0) {
			Swal.fire({ icon: 'info', text: `No se registraron ubicaciones entre las ${desde} y las ${hasta}.` });
			return;
		}

		puntosFiltrados = filtrados;
		$('#resumenFiltroHoras').html(`Filtrado de <b>${desde}</b> a <b>${hasta}</b> (${filtrados.length} puntos)`);
		renderizarRecorrido(puntosFiltrados);
	});

	$('#btnRestablecerHoras').click(function() {
		puntosFiltrados = [...puntosHistorico];
		if (puntosHistorico.length) {
			const h1 = puntosHistorico[0].fecha.split(' ')[1]?.substring(0, 5) || "00:00";
			const h2 = puntosHistorico[puntosHistorico.length - 1].fecha.split(' ')[1]?.substring(0, 5) || "23:59";
			$('#filtroHoraDesde').val(h1);
			$('#filtroHoraHasta').val(h2);
			$('#resumenFiltroHoras').text(`Jornada completa: ${h1} a ${h2}`);
		}
		renderizarRecorrido(puntosFiltrados);
	});

	// Eventos del modal
	$('#modalDetalles').on('shown.bs.modal', function() {
		map_detalle.invalidateSize();
		if (polylineRuta) {
			map_detalle.fitBounds(polylineRuta.getBounds(), { padding: [40, 40] });
		}
	});

	$('#modalDetalles').on('hidden.bs.modal', function() {
		pausarSimulacion();
	});


	function initializePage() {
		// 1. Cargar tiendas en segundo plano
		$.ajax({
			url: "/ProyectoPizzaAmericana/GetTiendas",
			method: "GET",
			dataType: "json",
			success: function(data) {
				tiendas = data;

				[selectTienda, selectTiendaHist].forEach(select => {
					tiendas.forEach(function(tienda) {
						select.append(
							$('<option>', {
								value: tienda.id,
								text: tienda.nombre
							})
						);
					});
				});

				tiendas.forEach(tienda => tiendaMap[tienda.id] = tienda.nombre);

				// Actualizar nombres de tiendas en los registros ya cargados
				mapaConductores.forEach(reg => {
					if (reg.idtienda && tiendaMap[reg.idtienda]) {
						reg.nombreTienda = tiendaMap[reg.idtienda];
						const node = reg.row ? reg.row.node() : null;
						if (node) {
							$(node).find('.badge-tienda-tabla').text(reg.nombreTienda);
						}
					}
				});
			},
			error: function(err) {
				console.error("Error al obtener datos de tiendas:", err);
			}
		});

		// 2. Cargar inmediatamente en paralelo toda la flota del día (0 ms de retardo, sin esperar a tiendas)
		EnvioDatos({ tiendaId: 0, action: "rastreo" });
	}


	function cargarParametro(parametro, asignarVariable) {
		return $.ajax({
			url: `/ProyectoPizzaAmericana/GetParametro?parametro=${parametro}`,
			method: "GET",
			dataType: "json"
		}).done(function(data) {

			if (asignarVariable == "a_js") {

				accessToken = data.valortexto;
			}

		}).fail(function(err) {
			console.error(`Error al obtener datos para ${parametro}:`, err);
		});
	}



});


$(document).ready(function() {

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
	// Llamadas optimizadas
	Promise.all([
	    cargarParametro("ARCGIS-JS", "a_js"),
	    cargarParametro("MAPBOX-JS", "mp_js")
	]).then(() => {
		// Configuración de Mapbox

		// Inicialización de Mapbox
		 map = new mapboxgl.Map({
			container: 'map',
			style: 'mapbox://styles/mapbox/streets-v11',
			center: [longitudInicial, latitudInicial],
			zoom: zoomInicial
		});


		map_detalle = new mapboxgl.Map({
			container: 'mapa-detalle',
			style: 'mapbox://styles/mapbox/streets-v11',
			center: [longitudInicial, latitudInicial],
			zoom: zoomInicial
		});
		
		// Cargar polígonos en el mapa
		map.on('load', () => {
			map.addSource('states', { 'type': 'geojson', 'data': 'zonas-polig.geojson' });
			map.addLayer({
				'id': 'states-layer',
				'type': 'fill',
				'source': 'states',
				'paint': {
					'fill-color': ['match', ['get', 'id'], 1, 'rgba(255, 0, 0, 0.1)', 2, 'rgba(0, 255, 0, 0.1)', 3, 'rgba(0, 0, 255, 0.1)', 'rgba(200, 100, 240, 0.1)'],
					'fill-outline-color': 'rgba(0, 0, 0, 0.3)'
				}
			});
			map.addLayer({
				'id': 'states-labels',
				'type': 'symbol',
				'source': 'states',
				'layout': {
					'text-field': ['get', 'name'],
					'text-size': 12,
					'text-offset': [0, 1.5],
					'text-anchor': 'top'
				},
				'paint': {
					'text-color': '#000',
					'text-halo-color': '#fff',
					'text-halo-width': 1
				}
			});
		});

		map_detalle.on('load', () => {

			map_detalle.addSource('states-detalle', { 'type': 'geojson', 'data': 'zonas-polig.geojson' });
			map_detalle.addLayer({
				'id': 'states-layer-detalle',
				'type': 'fill',
				'source': 'states-detalle',
				'paint': {
					'fill-color': ['match', ['get', 'id'], 1, 'rgba(255, 0, 0, 0.1)', 2, 'rgba(0, 255, 0, 0.1)', 3, 'rgba(0, 0, 255, 0.1)', 'rgba(200, 100, 240, 0.1)'],
					'fill-outline-color': 'rgba(0, 0, 0, 0.3)'
				}
			});
			map_detalle.addLayer({
				'id': 'states-labels-detalle',
				'type': 'symbol',
				'source': 'states-detalle',
				'layout': {
					'text-field': ['get', 'name'],
					'text-size': 12,
					'text-offset': [0, 1.5],
					'text-anchor': 'top'
				},
				'paint': {
					'text-color': '#000',
					'text-halo-color': '#fff',
					'text-halo-width': 1
				}
			});
		});
		
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
		
		EnvioDatos({ tiendaId: tiendaidH, action: "historial", startDate: startDate.val(), endDate: endDate.val() });
	}

	// Llamar al método para establecer la fecha actual al cargar la página
	window.onload = setCurrentDate;


	const map_detalleContainer = $('#mapa-detalle');
	

	

	// Inicialización de DataTable
	const table = $('#table-container').DataTable({
		"language": {
			"url": "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json",
			"sInfo": "", "sInfoEmpty": "", "sInfoFiltered": ""
		},
		"responsive": true,
			"autoWidth": false,
	});

	const tableHistorial = $('#table-historial').DataTable({
		"language": {
			"url": "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json",
			"sInfo": "", "sInfoEmpty": "", "sInfoFiltered": ""
		},
		"responsive": true,
		"autoWidth": false

	});

	const tableDetalles = $('#table-detalles').DataTable({
		"language": {
			"url": "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json",
			"sInfo": "", "sInfoEmpty": "", "sInfoFiltered": ""
		},
		"lengthChange": false,
		"responsive": true,
		"autoWidth": false,
		"searching": false,
		"paging": false

	});


	var longitudInicial = -75.5818;
	var latitudInicial = 6.2527;
	var zoomInicial = 11;



	const tiendaMap = {};
	

	// Función de geocodificación inversa
	function updateAddress(address) {
		document.querySelector("#user-details p:last-child").innerHTML = `<strong>Dirección:</strong> ${address}`;
	}

	function reverseGeocode(latitude, longitude) {
		const url = `https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?f=pjson&location=${longitude},${latitude}&token=${accessToken}`;
		fetch(url)
			.then(response => response.json())
			.then(data => updateAddress(data.address.LongLabel || "No disponible"))
			.catch(() => updateAddress("Error al obtener dirección"));
	}


	


	// Función para actualizar o crear fila y marcador
	function updateRowAndMarker(clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda) {
		let row = document.getElementById(`row-${clave_usuario}`);
		const nombreTienda = tiendaMap[idtienda] || "Tienda desconocida";

		if (!row) {
			row = document.createElement("tr");
			row.id = `row-${clave_usuario}`;
			row.classList.add("table-row");
			row.innerHTML = `
                <td>${nombre_usuario}</td>
                <td>${fecha_hora}</td>
                <td>${nombreTienda}</td>
            `;
			table.row.add($(row)).draw(false);
		} else {
			row.children[1].textContent = fecha_hora;
			row.children[2].textContent = nombreTienda;
		}

		if (!markers[clave_usuario]) {
			const popup = new mapboxgl.Popup({ offset: 25 }).setText(nombre_usuario);
			markers[clave_usuario] = new mapboxgl.Marker().setLngLat([longitude, latitude]).setPopup(popup).addTo(map);
		} else {
			markers[clave_usuario].setLngLat([longitude, latitude]);
		}
	}

	// Manejador de clics en las filas de la tabla
	table.on('click', 'tr', function() {
		const row = $(this);
		const clave_usuario = row.attr('id').split('-')[1];
		const nombre_usuario = row.children().first().text();
		const fecha_hora = row.children().eq(1).text();
		const nombreTienda = row.children().eq(2).text();

		if (markers[clave_usuario]) {
			const lat = markers[clave_usuario].getLngLat().lat;
			const lon = markers[clave_usuario].getLngLat().lng;

			map.flyTo({ center: [lon, lat], zoom: 17, essential: true });
			userDetails.innerHTML = `
                <h3>Detalles del Usuario</h3>
                <p><strong>Nombre:</strong> ${nombre_usuario}</p>
				<p><strong>Tienda:</strong> ${nombreTienda}</p>
                <p><strong>Latitud:</strong> ${lat}</p>
                <p><strong>Longitud:</strong> ${lon}</p>
                <p><strong>Fecha y Hora:</strong> ${fecha_hora}</p>
                <p><strong>Dirección:</strong> Generando dirección...</p>
            `;
			reverseGeocode(lat, lon);
		}
	});
	var marcadorDetalle = new mapboxgl.Marker().setLngLat([0, 0]).addTo(map_detalle);
	tableHistorial.on('click', 'tr', function() {

		var rowData = tableHistorial.row(this).data();  // Obtiene los datos visibles de la fila
		var selectedUser = rowData[0];  // Nombre de usuario (columna visible)
		var selectedDate = rowData[1];  // Fecha (columna visible)
		var selectedStore = rowData[2]; // Tienda (columna visible)

		// Obtener los valores de los atributos 'data-*' desde la fila
		var claveUsuario = $(this).data('clave_usuario');
		var idTienda = $(this).data('idtienda');

		EnvioDatos({ tiendaId: idTienda, action: "detalle", startDate: selectedDate, claveRapida: claveUsuario });
		var encab_usuario = $("#header-usuario");
		var encab_tienda = $("#header-tienda");

		encab_usuario.text(selectedUser);
		encab_tienda.text(selectedStore);

		limpiarMapa();


	});




	selectTienda.change(function() {
		var tiendaId = $(this).val();
		EnvioDatos({ tiendaId: tiendaId, action: "rastreo" });
	});

	

	selectTiendaHist.change(function() {
		tiendaidH = $(this).val();

	});

	filter.click(() => {
		EnvioDatos({ tiendaId: tiendaidH, action: "historial", startDate: startDate.val(), endDate: endDate.val() });


	});


	const socket = io("http://172.19.0.25:8082", { reconnection: true, reconnectionAttempts: 10, reconnectionDelay: 1000 });

	// Escuchar datos desde el servidor
	socket.on('updateLocation', (data) => {
		const { clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda } = data;
		updateRowAndMarker(clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda);
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

	botones.anterior.click(() => { if (paginaActual > 0) { paginaActual--; actualizarPaginador(); } map.resize(); });
	botones.siguiente.click(() => { if (paginaActual < paginas.length - 1) { paginaActual++; actualizarPaginador(); } });

	actualizarPaginador();

	function EnvioDatos(data) {
		$.ajax({
			url: "/ProyectoPizzaAmericana/HistorialUbicacion",
			method: "POST",
			data: data,
			success: function(response) {
			
				if (data.action == "rastreo") {
					table.clear().draw();
					Object.keys(markers).forEach(clave_usuario => markers[clave_usuario].remove());
					markers = {};

					response.forEach(location => {
						const { clave_usuario, latitud, longitud, fecha, nombre_usuario, idtienda } = location;
						updateRowAndMarker(clave_usuario, latitud, longitud, fecha, nombre_usuario, idtienda);
					});

					userDetails.innerHTML = `
					            <h3>Detalles del Usuario</h3>
					            <p>Seleccione un registro de la tabla para mostrar la información...</p>`;

				} else if (data.action == "historial") {

					tableHistorial.clear();  // Limpiar la tabla

					response.forEach(function(item) {
						// Agregar los datos visibles a la tabla
						var rowNode = tableHistorial.row.add([
							item.nombre_usuario,  // Columna visible
							item.fecha,            // Columna visible
							item.tienda            // Columna visible
						]).node(); // Agregar la fila y obtener el nodo de la fila

						// Asegurarse de que la fila se haya agregado antes de intentar establecer los atributos
						$(rowNode).attr('data-clave_usuario', item.clave_usuario)
							.attr('data-idtienda', item.idtienda); // Atributos adicionales
					});

					tableHistorial.draw(false);
				} else if (data.action == "detalle") {
					tableDetalles.clear();  // Limpiar la tabla
					response.forEach(function(item) {
						var rowNode = tableDetalles.row.add([
							item.fecha,
							'<button class="btn btn-primary  centrar-fila">Centrar</button>',
							'<button class="btn btn-primary  seleccionar-fila">Seleccionar</button>'
						]).node();


						$(rowNode).attr('data-longitud', item.longitud)
							.attr('data-latitud', item.latitud); // Atributos adicionales
					});

					tableDetalles.draw(false);

					$('#modalDetalles').modal('show');

				}
			},
			error: function(xhr, status, error) {
				console.error("Error al obtener los datos:", error);
			}
		});

	}


	$('#modalDetalles').on('shown.bs.modal', function() {
		map_detalle.resize(); // Redimensionar el mapa de detalle
		map_detalleContainer.css('visibility', 'visible');
	});

	map_detalleContainer.css('visibility', 'hidden');
	

	$(document).on('click', '.centrar-fila', function () {
		var fila = $(this).closest('tr');
		var latitud = fila.attr('data-latitud');
		var longitud = fila.attr('data-longitud');

			if (longitud && latitud) {
				// Actualiza la posición del marcador
				marcadorDetalle.setLngLat([longitud, latitud]);
				// Centra el mapa en las coordenadas del marcador
				map_detalle.flyTo({ center: [longitud, latitud], zoom: 16 });

				const url = `https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?f=pjson&location=${longitud},${latitud}&token=${accessToken}`;
				fetch(url)
					.then(response => response.json())
					.then(data => {

						infoDetails.innerHTML = ` <h3>Dirección:</h3>
							                <p><strong>Nombre:</strong> ${data.address.LongLabel || "No disponible"}</p>`;
					})
					.catch(() => {
						infoDetails.innerHTML = ` <h3>Dirección:</h3>
																                <p><strong>Nombre:</strong> ${"Error al obtener dirección"}</p>`;
					});

			} else {
				mostrarMensajeError('Coordenadas no disponibles para esta fila.')

			}
		
	});
	// Clase CSS para resaltar filas seleccionadas
	const CLASE_SELECCIONADA = 'fila-seleccionada';

	// Arreglo para almacenar las filas seleccionadas
	var seleccionados = [];

	$(document).on('click', '.seleccionar-fila', function () {
	    var fila = $(this).closest('tr');
	    var latitud = fila.attr('data-latitud');
	    var longitud = fila.attr('data-longitud');
	    var fecha = fila.find('td:eq(0)').text();
	
	    // Si la fila ya está seleccionada
	    if (fila.hasClass(CLASE_SELECCIONADA)) {
	        // Desmarcar la fila si ya está seleccionada
	        fila.removeClass(CLASE_SELECCIONADA);
	        // Eliminar la fila del arreglo
	        seleccionados = seleccionados.filter(item => item.fila[0] !== fila[0]);

	    } else {
	        // Si ya hay 2 filas seleccionadas
	        if (seleccionados.length >= 2) {
	            // Desmarcar las filas anteriores
	            seleccionados.forEach(item => item.fila.removeClass(CLASE_SELECCIONADA));
	            // Limpiar el arreglo y agregar solo la nueva fila seleccionada
	            seleccionados = [{ fila, fecha, latitud, longitud }];
	            fila.addClass(CLASE_SELECCIONADA); // Marcar la nueva fila

	        } else {
	            // Si no hay 2 filas seleccionadas, agregar la nueva fila al arreglo
	            seleccionados.push({ fila, fecha, latitud, longitud });
				
	            fila.addClass(CLASE_SELECCIONADA); // Marcar la nueva fila
			
	        }
	    }

	    // Si hay exactamente dos filas seleccionadas, procesar la ruta
	    if (seleccionados.length === 2) {
	        obtenerCoordenadasEntreFilas(seleccionados[0], seleccionados[1]);
	    }
		
		
	});


	function obtenerCoordenadasEntreFilas(fila1, fila2) {
		ocultarMensajeError();
		limpiarMapa();
	    const fechaInicio = new Date(fila1.fecha);
	    const fechaFin = new Date(fila2.fecha);
	    const coordenadas = [];

	    // Asegurar que fechaInicio sea menor
	    const inicio = fechaInicio < fechaFin ? fechaInicio : fechaFin;
	    const fin = fechaInicio > fechaFin ? fechaInicio : fechaFin;

	    // Recorrer filas y filtrar coordenadas en el rango
	    tableDetalles.rows().every(function () {
	        const fila = this.node();
	        const fecha = new Date($(fila).find('td:eq(0)').text());
	        if (fecha >= inicio && fecha <= fin) {
	            coordenadas.push([
	                $(fila).attr('data-longitud'),
	                $(fila).attr('data-latitud'),
	            ]);
	        }
	    });

	    if (coordenadas.length > 1) {
	        enviarRutaSegmentada(coordenadas);
	    } else {
			mostrarMensajeError('Se necesitan al menos dos coordenadas para procesar la ruta.')

	    }
	}

	async function enviarRuta(coordenadas) {

	    const coordsString = coordenadas.map(coord => coord.join(',')).join(';');

	    try {
	        const response = await fetch(
				`https://api.mapbox.com/matching/v5/mapbox/walking/${coordsString}?geometries=geojson&steps=true&access_token=${mapboxgl.accessToken}`
	        );
	        const data = await response.json();

	        if (data.code === 'Ok') {
	            agregarRuta(data.matchings[0].geometry);
	        } else {
				mostrarMensajeError('No se pudo procesar la ruta. Verifica las coordenadas.')

	        }
	    } catch (error) {
			mostrarMensajeError('No se pudo procesar la ruta. se presento un error')
			 console.log('Error al solicitar la API:', error);
	    }
	}

	async function enviarRutaSegmentada(coordenadas, segmento = 50) {
	    for (let i = 0; i < coordenadas.length; i += segmento) {
	        const segmentoCoords = coordenadas.slice(i, i + segmento);
	        await enviarRuta(segmentoCoords);
	    }
	}

	
	function agregarRuta(geometry) {
		
	    map_detalle.addSource('route', {
	        type: 'geojson',
	        data: { type: 'Feature', properties: {}, geometry },
	    });

	    map_detalle.addLayer({
	        id: 'route',
	        type: 'line',
	        source: 'route',
	        layout: { 'line-join': 'round', 'line-cap': 'round' },
	        paint: { 'line-color': '#03AA46', 'line-width': 8, 'line-opacity': 0.8 },
	    });
		
		// Centrar el mapa en la ruta
		   const coordinates = geometry.coordinates; // Obtener las coordenadas de la geometría
		   const bounds = coordinates.reduce((bounds, coord) => bounds.extend(coord), new mapboxgl.LngLatBounds(coordinates[0], coordinates[0]));

		   map_detalle.fitBounds(bounds, {
		       padding: 50, // Espacio alrededor de la ruta
		       maxZoom: 18, // Nivel máximo de zoom permitido
		       duration: 1000, // Duración de la animación en milisegundos
		   });
	}


	function limpiarMapa() {
		if (map_detalle.getSource('route')) {
		    map_detalle.removeLayer('route');
		    map_detalle.removeSource('route');
		}

		infoDetails.innerHTML = ` <h3>Dirección:</h3><p>Seleccione un registro de la tabla para mostrar la información...</p></p>`;
		marcadorDetalle.remove();
		marcadorDetalle = new mapboxgl.Marker().setLngLat([0, 0]).addTo(map_detalle);
		map_detalle.flyTo({ center: [longitudInicial, latitudInicial], zoom: zoomInicial });
		ocultarMensajeError();
}



function mostrarMensajeError(mensaje) {
    const divError = document.getElementById('errorMensaje');
    divError.textContent = mensaje; // Actualiza el texto del mensaje
    divError.style.display = 'block'; // Muestra el mensaje
}

function ocultarMensajeError() {
    const divError = document.getElementById('errorMensaje');
    divError.style.display = 'none'; // Oculta el mensaje
}


function initializePage(){

	    $.ajax({
	        url: "/ProyectoPizzaAmericana/GetTiendas", 
	        method: "GET",
	        dataType: "json",
	        success: function (data) {
				tiendas =data;
				
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

	        },
	        error: function (err) {
	            console.error("Error al obtener datos:", err);
	        }
	    });
		
		EnvioDatos({ tiendaId: 0, action: "rastreo" });

}


	  function cargarParametro(parametro, asignarVariable) {
		    return $.ajax({
		        url: `/ProyectoPizzaAmericana/GetParametro?parametro=${parametro}`,
		        method: "GET",
		        dataType: "json"
		    }).done(function (data) {
				
				if(asignarVariable == "a_js"){
					
						accessToken = data.valortexto;
				}else{
						mapboxgl.accessToken =  data.valortexto;
				}
				
			
		    }).fail(function (err) {
		        console.error(`Error al obtener datos para ${parametro}:`, err);
		    });
		}
		

	
	});


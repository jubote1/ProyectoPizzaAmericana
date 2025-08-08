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

	// Llamadas optimizadas
	Promise.all([
		cargarParametro("ARCGIS-JS", "a_js"),
	]).then(() => {
		// Configuración de Mapbox

		map = L.map('map').setView(
		    [latitudInicial, longitudInicial],
		    zoomInicial);

			L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png', {
			  attribution: '&copy; <a href="https://carto.com/">CARTO</a> | &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
			  subdomains: 'abcd',
			  maxZoom: 19
			}).addTo(map);


		map_detalle = L.map('mapa-detalle').setView(
		    [latitudInicial, longitudInicial],
		    zoomInicial);

			L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png', {
					  attribution: '&copy; <a href="https://carto.com/">CARTO</a> | &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
					  subdomains: 'abcd',
					  maxZoom: 19
					}).addTo(map_detalle);




		marcadorDetalle = L.marker([0, 0]).addTo(map_detalle);

		fetch('zonas-polig.geojson')
		  .then(r => r.json())
		  .then(geojson => {
		    const colorMap = {}; // Para almacenar un color por cada ID
		    const generateColor = (key) => {
		      // Si ya tiene un color asignado, lo usamos
		      if (colorMap[key]) return colorMap[key];

		      // Si no, generamos un color aleatorio pastel y lo guardamos
		      const randomPastelColor = `hsl(${Math.floor(Math.random() * 360)}, 70%, 70%)`;
		      colorMap[key] = randomPastelColor;
		      return randomPastelColor;
		    };

		    const geojsonOptions = {
		      style: feature => ({
		        fillColor: generateColor(feature.properties.id || feature.properties.name),
		        fillOpacity: 0.1,
		        color: '#888', // borde del polígono
		        weight: 1
		      }),
		      onEachFeature: (feature, layer) => {
		        // Tooltip con nombre, si existe
		        if (feature.properties.name?.trim()) {
		          layer.bindTooltip(feature.properties.name, {
		            direction: 'top',
		            sticky: true
		          });
		        }

		        // Evitar recuadro al hacer clic
		        layer.on('click', function(e) {
		          L.DomEvent.stopPropagation(e);
		          L.DomEvent.preventDefault(e);
		        });
		      }
		    };

		    // Añadir a ambos mapas
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





	const tiendaMap = {};


	// Función de geocodificación inversa
	function updateAddress(address) {
		document.querySelector("#user-details p:last-child").innerHTML = `<strong>Dirección:</strong> ${address}`;
	}

	function reverseGeocode(latitude, longitude) {
		if (!accessToken) return updateAddress("Token no disponible");
		
		const url = `https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode?f=pjson&location=${longitude},${latitude}&token=${accessToken}`;
		fetch(url)
			.then(response => response.json())
			.then(data => updateAddress(data.address.LongLabel || "No disponible"))
			.catch(() => updateAddress("Error al obtener dirección"));
	}





	function updateRowAndMarker(clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda) {
		const nombreTienda = tiendaMap[idtienda] || "Tienda desconocida";
		const rowId = `row-${clave_usuario}`;
		const existingRow = document.getElementById(rowId);

		if (existingRow) {
			// ✅ Si la fila ya existe, actualiza los datos en DataTables
			const dataTableRow = table.row(existingRow);
			dataTableRow.data([
				nombre_usuario,
				fecha_hora,
				nombreTienda
			]).draw(false);
		} else {
			// ✅ Si no existe, crea una nueva fila
			const newRow = table.row.add([
				nombre_usuario,
				fecha_hora,
				nombreTienda
			]).draw(false).node();

			newRow.id = rowId;
			newRow.classList.add("table-row");
		}

		// Marcador
		if (!markers[clave_usuario]) {
			const popup = L.popup().setContent(nombre_usuario);
			markers[clave_usuario] = L.marker([latitude, longitude]).bindPopup(popup).addTo(map);
		} else {
			markers[clave_usuario].setLatLng([latitude, longitude]);
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

			const latlng = markers[clave_usuario].getLatLng();
			const lat = latlng.lat;
			const lon = latlng.lng;


			map.setView([lat, lon], 17);

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


	const socket = io("http://172.19.0.25:8082", { reconnection: true, reconnectionAttempts: 5, reconnectionDelay: 2000 ,  timeout: 5000});

	// Escuchar datos desde el servidor
	socket.on('updateLocation', (data) => {
		const { clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda } = data;
		updateRowAndMarker(clave_usuario, latitude, longitude, fecha_hora, nombre_usuario, idtienda);
	});


	let yaMostroErrorSocket = false;

	// Cuando se conecta exitosamente (por primera vez o tras reconectar)
	socket.on("connect", () => {
		if (yaMostroErrorSocket) {
			Swal.fire({
				icon: 'success',
				title: 'Conexión restablecida',
				text: 'La conexión en tiempo real ha sido restablecida.',
				timer: 3000,
				showConfirmButton: false
			});
		}
		yaMostroErrorSocket = false; // Reinicia el estado
	});

	// Si ocurre un error de conexión
	socket.on("connect_error", (err) => {
		console.warn("Error de conexión:", err.message);

		if (!yaMostroErrorSocket) {
			yaMostroErrorSocket = true;

			Swal.fire({
				icon: 'info',
				html: '<center><strong>Sin conexión en tiempo real </strong></center><br>No se logró la conexión con el servidor. Puede que esté apagado o fuera de horario.',
				confirmButtonText: 'Cerrar',
				customClass: {
				   popup: 'my-swal'
				 }
			});
		}
	});

	// Si definitivamente falla la reconexión
	socket.on("reconnect_failed", () => {
		console.warn("No se pudo reconectar. Desconectando...");
		socket.disconnect();
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
		map_detalle.invalidateSize();
		map_detalleContainer.css('visibility', 'visible');
	});

	map_detalleContainer.css('visibility', 'hidden');


	$(document).on('click', '.centrar-fila', function() {
		const fila = $(this).closest('tr');
		var latitud = fila.attr('data-latitud');
		var longitud = fila.attr('data-longitud');

		if (longitud && latitud) {
			// Actualiza la posición del marcador
			marcadorDetalle.setLatLng([latitud, longitud]);

			// Centra el mapa en las coordenadas del marcador
			map_detalle.setView([latitud, longitud], 16);


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

	$(document).on('click', '.seleccionar-fila', function() {
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
		tableDetalles.rows().every(function() {
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

	function agregarRutaLineal(coordenadas) {
		// Leaflet espera [lat, lng] (orden invertido)
		const latLngs = coordenadas.map(([lng, lat]) => [lat, lng]);

		const polyline = L.polyline(latLngs, {
			color: '#03AA46',
			weight: 6,
			opacity: 0.8
		}).addTo(map_detalle);

		const bounds = polyline.getBounds();
		map_detalle.fitBounds(bounds, { padding: [50, 50], maxZoom: 18 });

		// Guarda para limpiar después
		map_detalle._rutaActual = polyline;
	}

	
	function agregarRutaRealista(coordenadas) {
	    // Asegurar que están en formato [lat, lng]
	    const waypoints = coordenadas.map(([lng, lat]) => L.latLng(lat, lng));

	    // Crear la ruta con Leaflet Routing Machine y OSRM público
	    const control = L.Routing.control({
	        waypoints: waypoints,
	        router: L.Routing.osrmv1({
	            serviceUrl: 'https://router.project-osrm.org/route/v1'
	        }),
	        lineOptions: {
	            styles: [{ color: '#03AA46', weight: 6, opacity: 0.8 }]
	        },
	        createMarker: function () { return null; }, // sin marcadores
	        addWaypoints: false,
	        draggableWaypoints: false,
	        fitSelectedRoutes: true,
	        show: false
	    }).addTo(map_detalle);

	    // Guardar para poder eliminar después
	    map_detalle._rutaControl = control;
	}

	async function enviarRuta(coordenadas) {
		agregarRutaLineal(coordenadas);
	}

	async function enviarRutaSegmentada(coordenadas, segmento = 50) {
		// Puedes unir todos los segmentos en una sola línea, ya que no usamos una API externa
		agregarRutaLineal(coordenadas);
	}



	function limpiarMapa() {
		if (map_detalle._rutaActual) {
			map_detalle.removeLayer(map_detalle._rutaActual);
			delete map_detalle._rutaActual;
		}
		
		// Si ya hay una ruta previa, eliminarla
		    if (map_detalle._rutaControl) {
		        map_detalle.removeControl(map_detalle._rutaControl);
		    }


		infoDetails.innerHTML = ` <h3>Dirección:</h3><p>Seleccione un registro de la tabla para mostrar la información...</p></p>`;
		marcadorDetalle.setLatLng([0, 0]);
		map_detalle.setView([latitudInicial, longitudInicial], zoomInicial);
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


	function initializePage() {

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

			},
			error: function(err) {
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
		}).done(function(data) {

			if (asignarVariable == "a_js") {

				accessToken = data.valortexto;
			}

		}).fail(function(err) {
			console.error(`Error al obtener datos para ${parametro}:`, err);
		});
	}



});


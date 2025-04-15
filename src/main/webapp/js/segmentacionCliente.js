document.addEventListener("DOMContentLoaded", function() {
	flatpickr("#rangoFechas", {
		locale: "es",
		mode: "range",
		dateFormat: "Y-m-d",
		altInput: true,
		altFormat: "Y-m-d",
		allowInput: true,
		clickOpens: true,
		disableMobile: true,
		rangeSeparator: " - "
	});

	flatpickr("#programarEnvio", {
		enableTime: true,
		dateFormat: "Y-m-d H:i",
		time_24hr: true,
		locale: "es",
		allowInput: true,
		clickOpens: true,
		disableMobile: true,
	});

	$("#selectPromociones, #selectPlantilla, #selectTiendas").select2({
		placeholder: function() {
			return $(this).data("placeholder");
		},
		allowClear: true,
		width: "100%",
		theme: "bootstrap4"
	});

	const BASE_URL = window.location.origin + "/ProyectoPizzaAmericana";


	// Evento para seleccionar/deseleccionar todas las filas
	$(document).on("change", "#selectAll", function() {
		let checked = $(this).is(":checked");
		$(".row-checkbox").prop("checked", checked);
	});

	//	// Evento para cantidadSeleccionada: seleccionar N filas
	//	$(document).on("input change", "#cantidadSeleccionada", function() {
	//		let cantidad = parseInt($(this).val()) || 0;
	//		$(".row-checkbox").prop("checked", false); // Desmarca todos
	//		$(".row-checkbox").slice(0, cantidad).prop("checked", true); // Marca los primeros N
	//	});
	//	
	function actualizarSeleccion() {
		let cantidad = parseInt($("#cantidadSeleccionada").val()) || 0;
		let invertir = $("#invertirSeleccion").is(":checked");
		let checkboxes = $(".row-checkbox");

		checkboxes.prop("checked", false); // Desmarca todos

		if (cantidad > 0) {
			if (invertir) {
				checkboxes.slice(-cantidad).prop("checked", true);
			} else {
				checkboxes.slice(0, cantidad).prop("checked", true);
			}
		}
	}

	// Se llama esta función cuando cambia el número o el checkbox de invertir
	$(document).on("input change", "#cantidadSeleccionada, #invertirSeleccion", actualizarSeleccion);


	let tablaClientes = $("#tablaClientes").DataTable({
		paging: false,
		lengthChange: false,
		searching: false,
		autoWidth: true,
		info: false,
		data: [],
		fixedHeader: true, // Mantiene el encabezado fijo
		scrollY: "800px",  // Activa el scroll vertical
		scrollCollapse: true,
		columns: [
			{ data: null, title: "#" },
			{ data: "nombre", title: "Nombre" },
			{ data: "nombreComp", title: "Empresa" },
			{ data: "email", title: "Email" },
			{ data: "telefono", title: "Teléfono" },
			{ data: "numeropedidos", title: "Pedidos" },
			{ data: "fechamaxima", title: "Última Compra" },
			{ data: "nombretienda", title: "Tienda" },
			{
				data: null,
				title: '<input type="checkbox" id="selectAll">',
				orderable: false,
				className: "text-center", // Añadir clase para centrar
				render: function(data, type, row) {
					return `<input type="checkbox" class="row-checkbox" value="${data.email}">`;
				}
			}
		],
		responsive: true,
		language: {
			url: 'https://cdn.datatables.net/plug-ins/1.10.15/i18n/Spanish.json',
		},
		createdRow: function(row, data, index) {
			$("td:eq(0)", row).html(index + 1); // Agregar número de fila en la primera celda
		},
		drawCallback: function() {
			let totalRegistros = this.api().rows().count();
			$("#registroTotal").text(`Total de registros: ${totalRegistros}`);
		},

		initComplete: function() {
			var api = this.api();
			var selectEtiquetas = document.getElementById("selectEtiquetas");
			var parametrosSelect = document.getElementById("parametrosSelect");

			let opciones = ``;
			var totalColumns = api.columns().count(); // Obtiene el total de columnas

			api.columns().header().each(function(th, index) {
				// Ignorar la primera (index 0) y la última (index totalColumns - 1)
				if (index > 0 && index < totalColumns - 1) {
					var columnName = $(th).text(); // Obtiene el nombre de la columna
					opciones += `<option value="${index}">${columnName}</option>`;
				}
			});

			// Asegúrate de que los elementos existen antes de asignarles contenido
			if (selectEtiquetas && parametrosSelect) {
				let et = '<option value="" disabled selected>Seleccionar</option>'
				selectEtiquetas.innerHTML = et + opciones;
				selectEtiquetas.value = ""; // O puedes poner aquí un valor predeterminado si lo prefieres

				parametrosSelect.innerHTML = opciones;
				parametrosSelect.value = "1"; // Ajusta esto según el valor predeterminado que deseas
			}
		}



	});


	document.getElementById('filtrarMiembrosClub').addEventListener('change', function() {
		var camposMiembrosClub = document.getElementById('camposMiembrosClub');
		var cantidadPedidosSection = document.getElementById('cantidadPedidos').closest('.section');
		var minDiasPublicidadSection = document.getElementById('minDiasPublicidad').closest('.section');

		var selectPromocionesSection = document.getElementById('selectPromociones').closest('.section');
		var obligatorioFields = document.querySelectorAll('.obligatorio'); // Selecciona todos los spans con la clase obligatorio

		if (this.checked) {
			// Mostrar campos de miembros del club
			camposMiembrosClub.style.display = 'flex'; // Usa 'grid' si aplica

			// Ocultar los otros campos sin afectar el diseño
			cantidadPedidosSection.style.visibility = 'hidden';
			cantidadPedidosSection.style.position = 'absolute';
			minDiasPublicidadSection.style.visibility = 'hidden';
			minDiasPublicidadSection.style.position = 'absolute';
			selectPromocionesSection.style.visibility = 'hidden';
			selectPromocionesSection.style.position = 'absolute';

			// Ocultar los spans con clase "obligatorio"
			obligatorioFields.forEach(function(span) {
				span.style.display = 'none';
			});

		} else {
			// Ocultar campos de miembros del club
			camposMiembrosClub.style.display = 'none';

			// Restaurar los otros campos sin romper el CSS
			cantidadPedidosSection.style.visibility = 'visible';
			cantidadPedidosSection.style.position = 'relative';
			minDiasPublicidadSection.style.visibility = 'visible';
			minDiasPublicidadSection.style.position = 'relative';
			selectPromocionesSection.style.visibility = 'visible';
			selectPromocionesSection.style.position = 'relative';

			// Mostrar los spans con clase "obligatorio"
			obligatorioFields.forEach(function(span) {
				span.style.display = 'inline';
			});
		}
	});




	cargarPlantillas();
	getListaTiendas();
	getExcepcionesPrecio();

	async function cargarPlantillas() {
		try {
			const response = await fetch(`${BASE_URL}/ObtenerPlantillaBrevo`);
			if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

			const data = await response.json();
			let opciones = data.map(({ idplantilla, nombre }) =>
				`<option value="${idplantilla}">${nombre}</option>`
			).join("");

			document.getElementById("selectPlantilla").innerHTML = opciones;
			document.getElementById("selectPlantilla").value = "";
		} catch (error) {
			console.error("Error al cargar plantillas:", error);
		}
	}




	async function getExcepcionesPrecio() {
		try {
			const response = await fetch(`${BASE_URL}/getExcepcionesPrecio`);
			if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

			const data = await response.json();
			let opciones = ``;
			data.forEach(({ idexcepcion, descripcion }) => {
				opciones += `<option value="${idexcepcion}">${descripcion}</option>`;
			});

			document.getElementById("selectPromociones").innerHTML = opciones;
			document.getElementById("selectPromociones").value = "";
		} catch (error) {
			console.error("Error al cargar tiendas:", error);
		}
	}


	async function getListaTiendas() {
		try {
			const response = await fetch(`${BASE_URL}/GetTiendasFuncionales`);
			if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

			const data = await response.json();
			let opciones = ``;
			data.forEach(({ id, nombre }) => {
				opciones += `<option value="${id}">${nombre}</option>`;
			});

			document.getElementById("selectTiendas").innerHTML = opciones;
			document.getElementById("selectTiendas").value = "";
		} catch (error) {
			console.error("Error al cargar tiendas:", error);
		}
	}



	async function cargarClientes(data = {}, url, columns = []) {
		const btnConsultar = document.getElementById("btnConsultar");
		const loadingSpinner = document.getElementById("loading-spinner");

		let alertaLenta = null;

		try {
			btnConsultar.disabled = true;
			loadingSpinner.style.display = "flex";

			console.log("📤 Enviando datos:", data);

			const response = await fetch(`${BASE_URL}/${url}`, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(data)
			});

			const responseText = await response.text();
			let responseData;

			try {
				responseData = JSON.parse(responseText);
			} catch {
				throw new Error(`Respuesta no válida del servidor: ${responseText}`);
			}

			if (!response.ok) {
				throw new Error(responseData.error || `Error ${response.status}: ${response.statusText}`);
			}

			console.log("✅ Respuesta recibida:", responseData);

			if (responseData.length > 0) {

				if ($.fn.DataTable.isDataTable("#tablaClientes")) {
					tablaClientes.clear().destroy();
				}

				await new Promise(resolve => {
					tablaClientes = $("#tablaClientes").DataTable({
						paging: false,
						lengthChange: false,
						searching: false,
						autoWidth: true,
						info: false,
						data: responseData,
						fixedHeader: true,
						scrollY: "800px",
						scrollCollapse: true,
						columns: columns,
						responsive: true,
						language: {
							url: 'https://cdn.datatables.net/plug-ins/1.10.15/i18n/Spanish.json',
						},
						createdRow: function(row, data, index) {
							$("td:eq(0)", row).html(index + 1);
						},
						drawCallback: function() {
							let totalRegistros = this.api().rows().count();
							$("#registroTotal").text(`Total de registros: ${totalRegistros}`);
						},
						initComplete: function() {
							const api = this.api();
							const selectEtiquetas = document.getElementById("selectEtiquetas");
							const parametrosSelect = document.getElementById("parametrosSelect");

							let opciones = ``;
							const totalColumns = api.columns().count();

							api.columns().header().each(function(th, index) {
								if (index > 0 && index < totalColumns - 1) {
									const columnName = $(th).text();
									opciones += `<option value="${index}">${columnName}</option>`;
								}
							});

							if (selectEtiquetas && parametrosSelect) {
								let et = '<option value="" disabled selected>Seleccionar</option>';
								selectEtiquetas.innerHTML = et + opciones;
								selectEtiquetas.value = "";
								parametrosSelect.innerHTML = opciones;
								parametrosSelect.value = "1";
							}

							// 👇 Esto indica que terminó la carga de DataTable
							resolve();
						}
					});
				});

				if (alertaLenta) {
					Swal.close(); // cerrar mensaje de "espera"
				}

				Swal.fire({
					icon: "success",
					title: "Clientes cargados",
					text: `Se encontraron ${responseData.length} clientes.`,
					timer: 3000,
					showConfirmButton: false
				});
			} else {
				Swal.fire({
					icon: "warning",
					title: "Sin clientes",
					text: "No se encontraron clientes con los filtros seleccionados.",
					timer: 3000,
					showConfirmButton: false
				});
			}

			return responseData;

		} catch (error) {
			console.error("⚠️ Error al cargar clientes:", error.message);
			Swal.fire({
				icon: "error",
				title: "Error",
				text: error.message,
				timer: 4000,
				showConfirmButton: false
			});
			return null;
		} finally {
			loadingSpinner.style.display = "none"; // 👈 AHORA sí se oculta al final real
			btnConsultar.disabled = false;
		}
	}

	document.getElementById("btnConsultar").addEventListener("click", function() {
		let datos = {};
		const filtrarMiembrosClub = document.getElementById("filtrarMiembrosClub").checked;
		const rangoFechas = document.getElementById("rangoFechas").value.trim();
		const tiendas = Array.from(document.querySelectorAll("#selectTiendas option:checked")).map(option => option.value);

		let fechaInicio = "", fechaMaxima = "";
		if (rangoFechas) {
			const fechas = rangoFechas.split(" a ").map(fecha => fecha.trim());
			if (fechas.length === 2) {
				[fechaInicio, fechaMaxima] = fechas;
				datos.fechaInicio = fechaInicio;
				datos.fechaMaxima = fechaMaxima;
			}
		}

		if (tiendas.length > 0) {
			datos.tiendas = tiendas;
		}

		if (filtrarMiembrosClub) {
			console.log("📌 Filtrando por miembros del club");

			const estadoMiembro = document.getElementById("estadoMiembro").value;
			const correoMiembro = document.getElementById("correoMiembro").value.trim();
			const puntosMiembro = document.getElementById("puntosMiembro").value.trim();

			if (estadoMiembro) datos.estadoMiembro = estadoMiembro;
			if (correoMiembro) datos.correoMiembro = correoMiembro;
			if (puntosMiembro !== "") datos.puntosMiembro = parseInt(puntosMiembro, 10) || 0; // Si es "", no lo envía

			console.log("📤 Enviando datos de Miembros del Club:", datos);

			columns = [
				{ data: null, title: "#" }, // Agregar numeración automática},
				{ data: "nombre", title: "Nombre", defaultContent: "" },
				{ data: "nombreCompania", title: "Empresa", defaultContent: "" },
				{ data: "correo", title: "Email", defaultContent: "" },
				{ data: "telefono", title: "Teléfono", defaultContent: "" },
				{ data: "fechaVinculacion", title: "Fecha de vinculación", defaultContent: "" },
				{ data: "puntosVigentes", title: "Puntos", defaultContent: "" },
				{ data: "nombreTienda", title: "Tienda", defaultContent: "" },
				{
					data: null,
					title: '<input type="checkbox" id="selectAll">',
					orderable: false,
					className: "text-center", // Añadir clase para centrar
					render: function(data, type, row) {
						return `<input type="checkbox" class="row-checkbox" value="${data.correo}" data-idcliente="${row.idcliente}">`;
					}
				}
			];

			cargarClientes(datos, 'ObtenerMiembrosClub', columns);

		} else {
			console.log("📌 Filtrando por segmentación de clientes");

			const cantidadPedidos = document.getElementById("cantidadPedidos").value.trim();
			const minDiasPublicidad = document.getElementById("minDiasPublicidad").value.trim();
			const promocionesSeleccionadas = Array.from(document.querySelectorAll("#selectPromociones option:checked")).map(option => option.value);

			if (cantidadPedidos !== "") datos.minPedidos = parseInt(cantidadPedidos, 10) || 0;
			if (minDiasPublicidad !== "") datos.minDiasPublicidad = parseInt(minDiasPublicidad, 10) || 0;
			if (promocionesSeleccionadas.length > 0) datos.excepciones = promocionesSeleccionadas;

			if (!rangoFechas || cantidadPedidos === "") {
				Swal.fire({
					icon: "warning",
					title: "Error",
					text: "Campos obligatorios vacíos.",
					timer: 3000,
					showConfirmButton: false
				});
				return;
			}
			columns = [
				{ data: null, title: "#" },// Agregar numeración automática,
				{ data: "nombre", title: "Nombre", defaultContent: "" },
				{ data: "nombreComp", title: "Empresa", defaultContent: "" },
				{ data: "email", title: "Email", defaultContent: "" },
				{ data: "telefono", title: "Teléfono", defaultContent: "" },
				{ data: "numeropedidos", title: "Pedidos", defaultContent: "" },
				{ data: "fechamaxima", title: "Última Compra", defaultContent: "" },
				{ data: "nombretienda", title: "Tienda", defaultContent: "" },
				{
					data: null,
					title: '<input type="checkbox" id="selectAll">',
					orderable: false,
					className: "text-center", // Añadir clase para centrar
					render: function(data, type, row) {
						return `<input type="checkbox" class="row-checkbox" value="${data.email}" data-idcliente="${row.idcliente}">`;
					}
				}
			];

			console.log("📤 Enviando datos de consulta:", datos);
			cargarClientes(datos, 'ObtenerClienteSegmentado', columns);
		}
	});



	document.getElementById("btnConfirmarEnvio").addEventListener("click", function() {
		let plantilla = document.getElementById("selectPlantilla").value.trim();
		let asuntoDiv = document.getElementById("AsuntoG");
		let isEtiqueta = false;

		let data = {};

		if (!plantilla) {
			Swal.fire({
				icon: "warning",
				title: "Sin plantilla",
				text: "No se encontró una plantilla seleccionada.",
				timer: 3000,
				showConfirmButton: false
			});
			return;
		}

		data.idplantilla = plantilla;

		// Verificar si hay correos en la tabla manual
		let correosManuales = [];
		let correosInvalidos = []; // Almacena los correos inválidos
		let idsClientes = [];

		document.querySelectorAll("#correosManual tbody tr").forEach(row => {
			let email = row.cells[0].querySelector("input").value.trim();
			let name = row.cells[1].querySelector("input").value.trim();

			let emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

			if (email) {
				if (!emailRegex.test(email)) {
					correosInvalidos.push(email); // Agregar a la lista de inválidos
				} else {
					correosManuales.push({ email, name });
				}
			}

		});

		// Si hay correos inválidos, mostrar alerta
		if (correosInvalidos.length > 0) {
			Swal.fire({
				icon: "error",
				title: "Correos inválidos",
				html: `Los siguientes correos no son válidos:<br><b>${correosInvalidos.join("<br>")}</b>`,
				confirmButtonText: "Revisar"
			});

			return;
		}

		if (correosManuales.length > 0) {
			// Usar los correos manuales y borrar spans en el asunto
			data.correos = correosManuales;
			data.asunto = asuntoDiv.innerHTML
				.replace(/<span[^>]*>.*?<\/span>/g, "")
				.replace(/&nbsp;/g, " ")
				.trim();


		} else {
			// Obtener correos seleccionados de la tabla original
			let correosSeleccionados = Array.from(document.querySelectorAll(".row-checkbox:checked")).map(checkbox => {

				let fila = checkbox.closest("tr");
				let email = checkbox.value.trim();
				let idcliente = checkbox.dataset.idcliente

				let emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
				if (!emailRegex.test(email)) {
					return null;
				} else {

					if (idcliente !== undefined && idcliente !== null && idcliente !== "") {
						idsClientes.push(idcliente);
					}

				}

				let asuntoTexto = asuntoDiv.innerHTML.trim();
				let spans = asuntoDiv.querySelectorAll("span.etiqueta");
				spans.forEach(span => {
					let colIndex = parseInt(span.getAttribute("data-value"));
					let valorColumna = fila.cells[colIndex]?.textContent.trim() || "";
					asuntoTexto = asuntoTexto.replace(span.outerHTML, valorColumna);
				});

				asuntoTexto = asuntoTexto.replace(/&nbsp;/g, " ");

				let name = fila.cells[1]?.textContent.trim() || "";
				let obj = { email, name };



				if (spans.length > 0) {
					obj.subject = asuntoTexto;
					isEtiqueta = true;
				}

				let params = [];
				document.querySelectorAll("#paramTable tbody tr").forEach(row => {
					let clave = row.querySelector("input[type='text']").value.trim();
					let select = row.querySelector("select");
					let colIndex = select ? parseInt(select.value) : null;

					if (clave && colIndex != null) {
						let valorColumna = fila.cells[colIndex]?.textContent.trim() || "";
						params.push({ clave, valor: valorColumna });
					}
				});

				if (params.length > 0) {
					obj.params = params;
				}

				return obj;
			}).filter(Boolean);

			if (correosSeleccionados.length === 0) {
				Swal.fire({
					icon: "warning",
					title: "Sin selección",
					text: "Debes seleccionar al menos un cliente.",
					timer: 3000,
					showConfirmButton: false
				});
				return;
			}

			data.correos = correosSeleccionados;

			// Si no hay parámetros en la tabla, no añadir la propiedad
			if (!isEtiqueta) {
				let valorAsunt = asuntoDiv.innerText.trim();
				if (valorAsunt) {
					data.asunto = valorAsunt;
				}

			}
		}

		// Agregar parámetros generales
		let generalParams = [];
		document.querySelectorAll("#paramGeneral tbody tr").forEach(row => {
			let clave = row.querySelector("td:first-child input[type='text']").value.trim();
			let valor = row.querySelector("td:nth-child(2) input[type='text']").value.trim();
			if (clave && valor) {
				generalParams.push({ clave, valor });
			}
		});

		if (generalParams.length > 0) {
			data.paramsDefault = generalParams;
		}




		console.log("📩 Datos a enviar:", data);

		Swal.fire({
			title: "¿Estás seguro?",
			text: `Se enviará la plantilla a ${data.correos.length} clientes.`,
			icon: "question",
			showCancelButton: true,
			confirmButtonText: "Sí, enviar",
			cancelButtonText: "Cancelar",
			reverseButtons: true
		}).then(async (result) => {
			if (result.isConfirmed) {
				let mensajeFinal = "";
				let huboError = false;

				// Crear un array para almacenar las promesas
				const promesas = [];

				// Envío de correos
				const promesaEnvio = EnvioDatos(data, "EnvioCorreoBrevo", "Error en el envío de correos.");
				promesas.push(promesaEnvio); // Añadir la promesa a la lista

				// Actualización de la fecha (solo si hay clientes)
				if (idsClientes.length > 0) {
					const data_idcliente = { idsClientes };
					const promesaFecha = EnvioDatos(data_idcliente, "ClienteUltimaFechaEnvio", "Error al actualizar fecha.");
					promesas.push(promesaFecha); // Añadir la promesa a la lista
				}

				// Esperar que todas las promesas se resuelvan
				try {
					const resultados = await Promise.all(promesas);

					// Recorremos los resultados de todas las promesas
					resultados.forEach((resultado) => {
						if (resultado.success) {
							mensajeFinal += "✅ " + resultado.message + "\n";
						} else {
							mensajeFinal += "❌ " + resultado.message + "\n";
							huboError = true;
						}
					});

					// Mostrar el mensaje final
					await Swal.fire({
						icon: huboError ? "warning" : "success",
						title: huboError ? "Proceso incompleto" : "Todo correcto",
						text: mensajeFinal,
						confirmButtonText: "Aceptar"
					});

				} catch (error) {
					// Si alguna promesa falla, mostrar error
					await Swal.fire({
						icon: "error",
						title: "Error",
						text: "Ocurrió un error inesperado en uno de los procesos.",
						confirmButtonText: "Aceptar"
					});
				}
			}
		});

	});


	document.getElementById("selectEtiquetas").addEventListener("change", function() {
		var select = document.getElementById("selectEtiquetas");
		var asuntoDiv = document.getElementById("AsuntoG");

		var valor = select.value; // Obtiene el valor de la opción seleccionada

		if (valor) {
			var etiqueta = select.options[select.selectedIndex].text; // Obtiene el texto visible

			// Asegurar el foco en el div
			asuntoDiv.focus();

			// Obtener la selección actual del usuario
			var selection = window.getSelection();
			if (selection.rangeCount === 0) {
				return;
			}

			var range = selection.getRangeAt(0);
			range.deleteContents(); // Eliminar cualquier contenido seleccionado

			// Crear el span que actúa como la etiqueta no editable
			var span = document.createElement("span");
			span.classList.add("etiqueta");
			span.textContent = etiqueta;
			span.setAttribute("contenteditable", "false"); // Hacer el span no editable
			span.setAttribute("data-value", valor); // Agregar el atributo data-value

			// Crear un espacio en blanco después del span para que el cursor quede ahí
			var space = document.createTextNode("\u00A0"); // Espacio no rompible

			// Insertar el span en la posición actual del cursor
			range.insertNode(span);

			// Mover el cursor fuera del span e insertar el espacio después
			range.setStartAfter(span);
			range.insertNode(space);

			// Mover el cursor completamente después del espacio en el div editable
			var newRange = document.createRange();
			newRange.setStartAfter(space);
			newRange.setEndAfter(space);

			// Aplicar la nueva selección
			selection.removeAllRanges();
			selection.addRange(newRange);

			// Asegurar que el foco siga en el div
			asuntoDiv.focus();

			// Limpiar el select después de agregar la etiqueta
			select.value = "";
		}
	});



	function agregarFila(idTabla) {
		const table = document.getElementById(idTabla).getElementsByTagName("tbody")[0];

		if (!table) {
			console.error("Tabla no encontrada:", idTabla);
			return;
		}

		let filas = table.getElementsByTagName("tr");
		if (filas.length > 0) {
			let ultimaFila = filas[filas.length - 1]; // Obtener la última fila
			let nuevaFila = ultimaFila.cloneNode(true); // Clonar la última fila

			// Limpiar los valores de los inputs en la nueva fila
			nuevaFila.querySelectorAll("input, select").forEach((elemento) => {
				if (elemento.tagName === "INPUT") {
					elemento.value = "";
				} else if (elemento.tagName === "SELECT") {
					elemento.selectedIndex = 0;
				}
			});

			// Agregar la nueva fila a la tabla
			table.appendChild(nuevaFila);
		}
	}

	function eliminarFila(fila) {
		const table = fila.closest("table"); // Encuentra la tabla más cercana

		if (!table) {
			console.error("No se encontró la tabla.");
			return;
		}

		const tbody = table.getElementsByTagName("tbody")[0];
		if (tbody.getElementsByTagName("tr").length > 1) { // Evitar eliminar todas las filas
			fila.remove();
		}
	}

	function inicializarTabla(idTabla) {
		const table = document.getElementById(idTabla);
		if (!table) {
			console.error("Tabla no encontrada:", idTabla);
			return;
		}

		// Delegación de eventos para manejar eliminaciones en nuevas filas
		table.addEventListener("click", function(event) {
			if (event.target.classList.contains("btn-eliminar")) {
				eliminarFila(event.target.closest("tr"));
			}
		});

		// Crear botón para agregar filas y colocarlo después de la tabla
		let botonAgregar = document.createElement("button");
		botonAgregar.textContent = "Agregar Fila";
		botonAgregar.classList.add("btn", "btn-primary");
		botonAgregar.addEventListener("click", function() {
			agregarFila(idTabla);
		});

		table.parentNode.appendChild(botonAgregar);
	}

	// Llamar a la función de inicialización con el ID de la tabla que necesites
	inicializarTabla("paramTable");
	inicializarTabla("paramGeneral");
	inicializarTabla("correosManual");


	async function EnvioDatos(data = {}, url, mgs) {
		const btnConfirmarEnvio = document.getElementById("btnConfirmarEnvio");
		const loadingSpinner = document.getElementById("loading-spinner");

		try {
			// Deshabilitar el botón mientras se están cargando los datos
			btnConfirmarEnvio.disabled = true;
			loadingSpinner.style.display = "flex";

			console.log("📤 Enviando datos:", data);

			// Realizar la solicitud POST al servlet
			const response = await fetch(`${BASE_URL}/${url}`, {
				method: "POST",
				headers: {
					"Content-Type": "application/json; charset=UTF-8"
				},
				body: JSON.stringify(data)
			});

			const responseData = await response.json();

			console.log("✅ Respuesta:", responseData);
			if (!response.ok || !responseData.success) {
				let error = responseData.message || `Error ${response.status}: ${response.statusText}`;
				console.error(error);
				// Devuelvo la promesa con el error
				return Promise.reject({ success: false, message: mgs + " " + error });
			}

			console.log("✅ Respuesta recibida:", responseData);

			// Devuelvo una promesa resuelta con el mensaje de éxito
			return Promise.resolve({ success: true, message: responseData.message });

		} catch (error) {
			console.error("⚠️ Error:", error.message);
			// Devuelvo una promesa rechazada con el mensaje de error
			return Promise.reject({ success: false, message: error.message });
		} finally {
			// Este bloque se ejecutará al final de todo, independientemente de lo que pase en la promesa
			btnConfirmarEnvio.disabled = false;
			loadingSpinner.style.display = "none";
		}
	}






});

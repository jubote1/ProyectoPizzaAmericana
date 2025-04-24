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

	$("#selectPromociones, #selectPlantilla, #selectTiendas ,#selectMedio").select2({
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
			const medio = document.getElementById("selectMedio").value.trim();
			const response = await fetch(`${BASE_URL}/ObtenerPlantillaBrevo`);
			if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

			const data = await response.json();
			// Filtrar según el valor de medio

			let plantillasFiltradas = data;
			if (medio === "0") {
				plantillasFiltradas = data.filter(p => p.categoria === "C");
			} else if (medio === "1") {
				plantillasFiltradas = data.filter(p => p.categoria === "W");
			}

			const opciones = plantillasFiltradas.map(({ idplantilla, nombre }) =>
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
		const medio = document.getElementById("selectMedio").value.trim();

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
				{ data: null, title: "#" },
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

						return `<input
						             type="checkbox"
						             class="row-checkbox"
						             data-idcliente="${row.idcliente}"
									 data-telefono="${data.telefono}"
									 data-correo="${data.correo}"
						           >`;
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
						return `<input
						             type="checkbox"
						             class="row-checkbox"
						             data-idcliente="${row.idcliente}"
									 data-telefono="${data.telefono}"
									 data-correo="${data.email}"
						           >`;
					}
				}
			];

			console.log("📤 Enviando datos de consulta:", datos);
			cargarClientes(datos, 'ObtenerClienteSegmentado', columns);
		}
	});

	$("#selectMedio").on("change", function() {
		cargarPlantillas();
		const medio = $("#selectMedio").val();
		let selectEtiquetas = document.getElementById("selectEtiquetas");
		if (medio === "0") {
			selectEtiquetas.style.display = "block"; // Ocultar
		} else {
			selectEtiquetas.style.display = "none"; // Ocultar
		}
	});

	$("#selectMedio, #selectPlantilla").on("change", function() {
		const medio = $("#selectMedio").val();
		const plantilla = $("#selectPlantilla").val();
		const labelAsunt = document.getElementById("labelAsunt");
		const labelCorreos = document.getElementById("labelCorreos");

		let asuntoDiv = document.getElementById("AsuntoG");

		
		if (medio == "0") {
			document.querySelector("#ValoresManual th:first-child").textContent = "Email";
			labelCorreos.textContent = "Correos:";
			labelAsunt.textContent = "Asunto:";
			asuntoDiv.setAttribute("contenteditable", "true");
		} else {
			document.querySelector("#ValoresManual th:first-child").textContent = "Celular";
			labelCorreos.textContent = "Telefonos:";
			labelAsunt.textContent = "Mensaje:";
			if (plantilla === null || plantilla === "") {
				asuntoDiv.setAttribute("contenteditable", "true");
			} else {
				asuntoDiv.setAttribute("contenteditable", "false");
				asuntoDiv.innerHTML = "";  // Limpia todo el contenido HTML, no solo el texto

			}

		}

	});



	document.getElementById("btnConfirmarEnvio").addEventListener("click", function() {
		const plantilla = document.getElementById("selectPlantilla").value.trim();
		const asuntoDiv = document.getElementById("AsuntoG");
		const medio = document.getElementById("selectMedio").value.trim();
		let isEtiqueta = false;

		if (!medio) {
			Swal.fire({
				icon: "warning",
				title: "No selecciono el  medio",
				text: "Debe seleccionar por que medio desea enviar la información",
				timer: 3000,
				showConfirmButton: false
			});
			return;
		}


		const data = { idplantilla: plantilla };
		data.medio = medio;
		const manuales = [];
		const invalidos = [];
		const idsClientes = [];
		let title_msg = medio === "0" ? "Correos" : "Telefonos";

		if (medio === "0") {
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
		} else {
			let mg = asuntoDiv.innerText.trim();
			if (!plantilla && !mg) {
				Swal.fire({
					icon: "warning",
					title: "Campos invalidos",
					text: "Para el envio por Whatsapp debe seleccionar una plantilla o ingresar un mensaje.",
					timer: 3000,
					showConfirmButton: false
				});
				return;
			}
		}

		document.querySelectorAll("#ValoresManual tbody tr").forEach(row => {
			const valor = row.cells[0].querySelector("input").value.trim();
			const name = row.cells[1].querySelector("input").value.trim();

			const regex = medio === "0"
				? /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
				: /^\d{10,12}$/;

			if (valor) {
				if (!regex.test(valor) || !name) {
					invalidos.push(valor);
				} else {
					manuales.push(medio === "0" ? { email: valor, name } : { telefono: valor, nombre: name });
				}
			}
		});

		if (invalidos.length > 0) {
			Swal.fire({
				icon: "error",
				title: `${title_msg} Invalidos`,
				html: `Los siguientes ${title_msg} no son válidos o no se ingreso un nombre:<br><b>${invalidos.join("<br>")}</b>`,
				confirmButtonText: "Revisar"
			});
			return;
		}

		if (manuales.length > 0) {
			const contenido = asuntoDiv.innerHTML
				.replace(/<span[^>]*>.*?<\/span>/g, "")
				.replace(/&nbsp;/g, " ")
				.trim();
			data.asunto = contenido;
			if (medio === "0") {
				data.correos = manuales;
			} else {
				data.telefonos = manuales;
			}
		} else {
			const seleccionados = Array.from(document.querySelectorAll(".row-checkbox:checked"))
				.map(checkbox => {
					const fila = checkbox.closest("tr");
					const valor = (medio === "0") ? checkbox.dataset.correo : checkbox.dataset.telefono;
					const idcliente = checkbox.dataset.idcliente;

					const regex = medio === "0"
						? /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
						: /^\d{10,12}$/;

					if (!regex.test(valor)) return null;

					if (idcliente) {
						idsClientes.push(idcliente);
					}

					let asuntoTexto = asuntoDiv.innerHTML.trim();
					const spans = asuntoDiv.querySelectorAll("span.etiqueta");

					spans.forEach(span => {
						const colIndex = parseInt(span.getAttribute("data-value"));
						const valorColumna = fila.cells[colIndex]?.textContent.trim() || "";
						asuntoTexto = asuntoTexto.replace(span.outerHTML, valorColumna);
					});

					asuntoTexto = asuntoTexto.replace(/&nbsp;/g, " ");

					const name = fila.cells[1]?.textContent.trim() || "";
					let obj = {};
					if (medio === "0") {
						obj = { email: valor, name }
						if (spans.length > 0) {
							obj.subject = asuntoTexto;
							isEtiqueta = true;
						}

					} else {
						obj = { telefono: valor };
					}

					const params = [];
					document.querySelectorAll("#paramTable tbody tr").forEach(row => {
						const clave = row.querySelector("input[type='text']").value.trim();
						const select = row.querySelector("select");
						const colIndex = select ? parseInt(select.value) : null;

						if (clave && colIndex != null) {
							const valorColumna = fila.cells[colIndex]?.textContent.trim() || "";
							params.push({ clave, valor: valorColumna });
						}
					});

					if (params.length > 0) {
						obj.params = params;
					}

					return obj;
				})
				.filter(Boolean);

			if (seleccionados.length === 0) {
				Swal.fire({
					icon: "warning",
					title: "Sin selección",
					text: "Debes seleccionar al menos un cliente.",
					timer: 3000,
					showConfirmButton: false
				});
				return;
			}

			if (medio === "0") {
				data.correos = seleccionados;
			} else {
				data.telefonos = seleccionados;
			}

			if (!isEtiqueta) {
				const textoPlano = asuntoDiv.innerText.trim();
				if (textoPlano) {
					data.asunto = textoPlano;
				}
			}
		}

		// Parámetros generales
		const generalParams = [];
		document.querySelectorAll("#paramGeneral tbody tr").forEach(row => {
			const clave = row.querySelector("td:first-child input[type='text']").value.trim();
			const valor = row.querySelector("td:nth-child(2) input[type='text']").value.trim();
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
			text: `Se enviará la plantilla a ${(data.correos && data.correos.length) ? data.correos.length : (data.telefonos ? data.telefonos.length : 0)} clientes.`,
			icon: "question",
			showCancelButton: true,
			confirmButtonText: "Sí, enviar",
			cancelButtonText: "Cancelar",
			reverseButtons: true
		}).then(async (result) => {
			if (result.isConfirmed) {
				let mensajeFinal = "";
				let huboError = false;

				try {
					// Envío de correos
					const resultadoEnvio = await EnvioDatos(data, "EnvioBrevo", "Error en el envío de correos.");

					if (!resultadoEnvio.success) {
						mensajeFinal += "❌ " + resultadoEnvio.message + "\n";
						huboError = true;
					} else {
						mensajeFinal += "✅ " + resultadoEnvio.message + "\n";

						// Si hay clientes y la primera fue exitosa
						if (idsClientes.length > 0) {
							const data_idcliente = { idsClientes };
							const resultadoFecha = await EnvioDatos(data_idcliente, "ClienteUltimaFechaEnvio", "Error al actualizar fecha.");

							if (!resultadoFecha.success) {
								mensajeFinal += "❌ " + resultadoFecha.message + "\n";
								huboError = true;
							} else {
								mensajeFinal += "✅ " + resultadoFecha.message + "\n";
							}
						}
					}

					await Swal.fire({
						icon: huboError ? "warning" : "success",
						title: huboError ? "Proceso incompleto" : "Todo correcto",
						text: mensajeFinal,
						confirmButtonText: "Aceptar"
					});

				} catch (error) {
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
	inicializarTabla("ValoresManual");


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

				// Procesar errores detallados si existen
				if (responseData.errores && Array.isArray(responseData.errores)) {
					const erroresDetallados = responseData.errores.map(err => {
						if (err.error && err.telefono) {
							return `📱 Teléfono: ${err.telefono}\n❌ Error: ${err.error}`;
						} else if (err.error) {
							return `❌ Error: ${err.error}`;
						} else {
							return `⚠️ ${JSON.stringify(err)}`;
						}
					}).join("\n\n");

					error += `\n\n📋 Detalles:\n${erroresDetallados}`;
				}


				return Promise.reject({ success: false, message: mgs + " " + error });
			}

			console.log("✅ Respuesta recibida:", responseData);
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

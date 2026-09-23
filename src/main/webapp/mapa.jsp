<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page
	import="org.json.simple.JSONObject, org.json.simple.JSONArray, java.util.List"%>
<%@ page
	import="capaDAOCC.DomiciliarioPedidoDAO,capaControladorCC.ParametrosCtrl"%>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Rastreo y Historial de Domiciliarios</title>

<!-- FontAwesome para íconos de navegación y marcadores -->
<link rel="stylesheet" href="css/all.min.css">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">

<!-- Bootstrap 4 -->
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
<!-- Leaflet -->
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>



<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

<!-- DataTables CSS -->
<link rel="stylesheet"
	href="https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap4.min.css">
<script
	src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
<script
	src="https://cdn.datatables.net/1.13.6/js/dataTables.bootstrap4.min.js"></script>
<script src="https://cdn.socket.io/4.0.0/socket.io.min.js"></script>
<link
	href="https://fonts.googleapis.com/css2?family=Poppins:wght@500&display=swap"
	rel="stylesheet">

<link rel="stylesheet" href="css/mapa.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div id="cargarMenu"></div>
<div class="content-home">
	<div id="paginador" class="d-flex justify-content-between">
		<button id="anterior" class="btn btn-secondary" disabled>
			<i class="fas fa-arrow-left"></i> Anterior
		</button>
		<button id="siguiente" class="btn btn-secondary">
			Siguiente <i class="fas fa-arrow-right"></i>
		</button>
	</div>

	<div id="pagina-mapa" class="pagina-activa">
		<div class="header-monitoreo-vivo d-flex flex-wrap justify-content-between align-items-center mb-3">
			<div class="d-flex align-items-center">
				<h1 class="titulo-pagina mb-0 mr-3">Monitoreo en Tiempo Real</h1>
				<span class="badge-live-pulse"><span class="pulse-dot"></span> EN VIVO</span>
			</div>
			<div class="d-flex align-items-center mt-2 mt-md-0 flex-wrap">
				<div class="custom-control custom-switch d-inline-flex align-items-center py-1 px-3 mr-2 mb-1 bg-white border rounded shadow-sm">
					<input type="checkbox" class="custom-control-input" id="chkSoloBiometriaLive">
					<label class="custom-control-label small font-weight-bold text-dark mb-0" for="chkSoloBiometriaLive" style="cursor:pointer;">
						<i class="fas fa-fingerprint text-primary mr-1"></i> Solo en Turno (Biometría)
					</label>
				</div>
				<div class="card-contador-domis shadow-sm d-flex align-items-center px-3 py-1 mb-1 bg-white border rounded">
					<i class="fas fa-motorcycle text-primary mr-2"></i>
					<span class="small font-weight-bold text-secondary mr-2">Repartidores Visibles:</span>
					<span class="badge badge-primary font-weight-bold" id="badgeTotalDomiActivos">0</span>
				</div>
			</div>
		</div>

		<div class="row mx-0">
			<!-- Columna Izquierda: Mapa con selector de tienda y tipo integrados -->
			<div class="col-12 col-xl-7 col-lg-7 mb-3 px-1">
				<div class="card shadow-sm border-0 h-100 card-mapa-monitoreo">
					<div class="card-header bg-white py-2 d-flex justify-content-between align-items-center border-bottom">
						<div class="d-flex align-items-center">
							<i class="fas fa-map-marked-alt text-primary mr-2"></i>
							<strong class="text-dark small text-uppercase font-weight-bold">Flota en el Mapa</strong>
						</div>
						<div class="d-flex align-items-center">
							<span class="badge badge-light border text-muted small"><i class="fas fa-satellite text-primary mr-1"></i> Cobertura en Vivo</span>
						</div>
					</div>
					<!-- Barra de Filtros Espaciosa, Moderna y Sincrónica -->
					<div class="mapa-toolbar-filtros border-bottom p-2 bg-white">
						<div class="row no-gutters align-items-center">
							<div class="col-12 col-sm-4 px-1 mb-1 mb-sm-0">
								<div class="filtro-card-control shadow-none">
									<div class="filtro-icono-wrap bg-primary-soft text-primary">
										<i class="fas fa-store"></i>
									</div>
									<div class="filtro-input-group">
										<label for="tiendaSelect" class="filtro-label-micro">Tienda</label>
										<select id="tiendaSelect" class="filtro-select-clean font-weight-bold">
											<option value="0">Todas las Tiendas</option>
										</select>
									</div>
								</div>
							</div>
							<div class="col-12 col-sm-4 px-1 mb-1 mb-sm-0">
								<div class="filtro-card-control shadow-none">
									<div class="filtro-icono-wrap bg-info-soft text-info">
										<i class="fas fa-id-badge"></i>
									</div>
									<div class="filtro-input-group">
										<label for="filtroTipoRepartidorLive" class="filtro-label-micro">Tipo Repartidor</label>
										<select id="filtroTipoRepartidorLive" class="filtro-select-clean font-weight-bold">
											<option value="TODOS">Todos</option>
											<option value="DIRECTO">Solo Propios</option>
											<option value="TEMPORAL">Solo Temporales</option>
										</select>
									</div>
								</div>
							</div>
							<div class="col-12 col-sm-4 px-1">
								<div class="filtro-card-control shadow-none">
									<div class="filtro-icono-wrap bg-success-soft text-success">
										<i class="fas fa-toggle-on"></i>
									</div>
									<div class="filtro-input-group">
										<label for="filtroEstadoLive" class="filtro-label-micro">Estado Operativo</label>
										<select id="filtroEstadoLive" class="filtro-select-clean font-weight-bold">
											<option value="TODOS">Todos</option>
											<option value="ACTIVOS">⚡ Solo Activos Recientes</option>
											<option value="EN_TIENDA">🟢 En Tienda</option>
											<option value="EN_RUTA">🛵 En Ruta (Con Pedido)</option>
											<option value="FUERA_DE_TIENDA">🟡 Fuera Tienda (Sin Pedido)</option>
											<option value="INACTIVOS">⚪ Inactivos (> 2 horas)</option>
										</select>
									</div>
								</div>
							</div>
						</div>
					</div>
					<div class="card-body p-0 position-relative">
						<div id="map" style="height: 575px; width: 100%; border-radius: 0 0 8px 8px;"></div>
					</div>
				</div>
			</div>

			<!-- Columna Derecha: Tabla única por domiciliario y Ficha de Estado -->
			<div class="col-12 col-xl-5 col-lg-5 mb-3 px-1">
				<div class="card shadow-sm border-0 mb-3 card-tabla-monitoreo">
					<div class="card-header bg-white py-2 d-flex justify-content-between align-items-center border-bottom">
						<div class="d-flex align-items-center">
							<i class="fas fa-satellite-dish text-success mr-2"></i>
							<strong class="text-dark small text-uppercase font-weight-bold">Último Reporte por Repartidor</strong>
						</div>
						<span class="badge badge-light border text-muted small" id="badgeUltimaSync"><i class="fas fa-clock mr-1"></i> Hoy</span>
					</div>
					<div class="card-body p-2">
						<div class="table-responsive">
							<table id="table-container" class="table table-hover table-sm w-100 mb-0">
								<thead class="thead-light">
									<tr>
										<th>Domiciliario</th>
										<th>Última Hora</th>
										<th>Tienda</th>
									</tr>
								</thead>
								<tbody id="user-table-body"></tbody>
							</table>
						</div>
					</div>
				</div>

				<!-- Ficha de Detalles del Domiciliario Seleccionado -->
				<div id="user-details" class="card shadow-sm border-0 p-3 card-perfil-domi">
					<div class="d-flex align-items-center mb-3 pb-2 border-bottom">
						<div class="avatar-domi-perfil mr-3">
							<i class="fas fa-motorcycle"></i>
						</div>
						<div class="text-left">
							<h6 class="font-weight-bold mb-0 text-dark" id="detalles-nombre">Ficha del Domiciliario</h6>
							<small class="text-muted" id="detalles-subtitulo">Haga clic en una fila para centrar en el mapa</small>
						</div>
					</div>
					<div id="detalles-contenido" class="text-muted small">
						<div class="alert alert-light border py-3 px-3 mb-0 text-center">
							<i class="fas fa-info-circle mr-1 text-primary"></i> Selecciona un domiciliario de la tabla para ver su información en tiempo real.
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<div id="pagina-historial" class="pagina-oculta">

		<h1 class="titulo-pagina">Historial de Ubicaciones</h1>
		<!-- Filtros de Historial Modernizados -->
		<div class="card shadow-sm border-0 mb-3 p-3 bg-white">
			<div class="row align-items-center">
				<div class="col-12 col-md-3 mb-2 mb-md-0">
					<label for="tiendaHistorial" class="small font-weight-bold text-muted mb-1"><i class="fas fa-store mr-1 text-primary"></i>Tienda:</label>
					<select id="tiendaHistorial" class="form-control form-control-sm font-weight-bold">
						<option value="0">Todas</option>
					</select>
				</div>
				<div class="col-12 col-md-3 mb-2 mb-md-0">
					<label for="filtroTipoRepartidorHistorial" class="small font-weight-bold text-muted mb-1"><i class="fas fa-id-badge mr-1 text-primary"></i>Tipo Repartidor:</label>
					<select id="filtroTipoRepartidorHistorial" class="form-control form-control-sm font-weight-bold">
						<option value="TODOS">Todos</option>
						<option value="DIRECTO">Solo Propios</option>
						<option value="TEMPORAL">Solo Temporales</option>
					</select>
				</div>
				<div class="col-6 col-md-2 mb-2 mb-md-0">
					<label for="startDate" class="small font-weight-bold text-muted mb-1"><i class="far fa-calendar-alt mr-1 text-primary"></i>Desde:</label>
					<input type="date" id="startDate" class="form-control form-control-sm font-weight-bold">
				</div>
				<div class="col-6 col-md-2 mb-2 mb-md-0">
					<label for="endDate" class="small font-weight-bold text-muted mb-1"><i class="far fa-calendar-alt mr-1 text-primary"></i>Hasta:</label>
					<input type="date" id="endDate" class="form-control form-control-sm font-weight-bold">
				</div>
				<div class="col-12 col-md-2 text-md-right mt-2 mt-md-0">
					<label class="d-none d-md-block small mb-1">&nbsp;</label>
					<button id="filter" class="btn btn-primary btn-sm font-weight-bold btn-block shadow-sm">
						<i class="fas fa-filter mr-1"></i> Filtrar
					</button>
				</div>
			</div>
		</div>
		<div class="contenedor-tablas">

			<!-- Primera tabla -->
			<table id="table-historial" class="table table-bordered">
				<thead>
					<tr>
						<th colspan="3" class="centered-header">Historial</th>
					</tr>
					<tr>
						<th>Usuario</th>
						<th>Fecha</th>
						<th>Tienda</th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		</div>
	</div>

	<!-- Modal para los detalles del usuario y simulación de recorrido -->
	<div class="modal fade" id="modalDetalles" tabindex="-1"
		role="dialog" aria-labelledby="modalDetallesLabel"
		aria-hidden="true">
		<div class="modal-dialog modal-xl modal-dialog-centered" role="document">
			<div class="modal-content telemetria-modal shadow-lg">
				<div class="modal-header d-flex align-items-center justify-content-between">
					<div class="d-flex align-items-center">
						<div class="avatar-domi mr-3">
							<i class="fas fa-motorcycle"></i>
						</div>
						<div>
							<h5 class="modal-title font-weight-bold" id="modal-usuario-nombre">Detalles del Domiciliario</h5>
							<div class="modal-subinfo mt-1">
								<span class="badge badge-primary mr-2" id="modal-tienda-nombre"><i class="fas fa-store mr-1"></i> Tienda</span>
								<span class="badge badge-light border" id="modal-fecha-dia"><i class="far fa-calendar-alt mr-1"></i> Fecha</span>
							</div>
						</div>
					</div>
					<button type="button" class="close text-white" data-dismiss="modal" aria-label="Close" style="opacity: 0.9;">
						<span aria-hidden="true">&times;</span>
					</button>
				</div>

				<div class="modal-body p-3">
					<!-- Tarjetas de KPIs de Telemetría -->
					<div class="row kpis-telemetria mb-3">
						<div class="col-6 col-md-3">
							<div class="kpi-card shadow-sm">
								<div class="kpi-icon bg-soft-primary"><i class="fas fa-map-marker-alt text-primary"></i></div>
								<div class="kpi-data">
									<span class="kpi-title">Puntos GPS</span>
									<span class="kpi-value" id="kpi-puntos">0</span>
								</div>
							</div>
						</div>
						<div class="col-6 col-md-3">
							<div class="kpi-card shadow-sm">
								<div class="kpi-icon bg-soft-success"><i class="fas fa-route text-success"></i></div>
								<div class="kpi-data">
									<span class="kpi-title">Distancia Est.</span>
									<span class="kpi-value" id="kpi-distancia">0.0 km</span>
								</div>
							</div>
						</div>
						<div class="col-6 col-md-3">
							<div class="kpi-card shadow-sm">
								<div class="kpi-icon bg-soft-info"><i class="far fa-clock text-info"></i></div>
								<div class="kpi-data">
									<span class="kpi-title">Tiempo en Ruta</span>
									<span class="kpi-value" id="kpi-duracion">0m</span>
								</div>
							</div>
						</div>
						<div class="col-6 col-md-3">
							<div class="kpi-card shadow-sm">
								<div class="kpi-icon bg-soft-warning"><i class="fas fa-hand-paper text-warning"></i></div>
								<div class="kpi-data">
									<span class="kpi-title">Paradas (>3 min)</span>
									<span class="kpi-value" id="kpi-paradas">0</span>
								</div>
							</div>
						</div>
					</div>

					<!-- Barra del Reproductor de Ruta / Simulador -->
					<div class="reproductor-ruta-box shadow-sm mb-3">
						<div class="d-flex flex-wrap align-items-center justify-content-between mb-2">
							<div class="d-flex align-items-center mb-1 flex-wrap">
								<button id="btnPlayRuta" class="btn btn-success btn-sm font-weight-bold mr-2 mb-1">
									<i class="fas fa-play mr-1"></i> Reproducir
								</button>
								<button id="btnResetRuta" class="btn btn-outline-secondary btn-sm mr-3 mb-1" title="Reiniciar al inicio">
									<i class="fas fa-redo-alt"></i>
								</button>
								<div class="btn-group btn-group-sm mr-3 mb-1" role="group" id="grupoVelocidades">
									<button type="button" class="btn btn-outline-primary active btn-vel" data-vel="1">1x</button>
									<button type="button" class="btn btn-outline-primary btn-vel" data-vel="2">2x</button>
									<button type="button" class="btn btn-outline-primary btn-vel" data-vel="5">5x</button>
									<button type="button" class="btn btn-outline-primary btn-vel" data-vel="10">10x</button>
								</div>
								<button id="btnToggleRuta" class="btn btn-outline-primary btn-sm font-weight-bold mr-2 mb-1" title="Mostrar u ocultar el trazo del recorrido en el mapa">
									<i class="fas fa-eye-slash mr-1"></i> Ocultar Ruta
								</button>
								<button id="btnAjustarCalles" class="btn btn-success btn-sm font-weight-bold mr-2 mb-1" title="Ajustar y trazar el recorrido sobre las calles y carriles reales de la ciudad (vía OSRM)">
									<i class="fas fa-road mr-1"></i> Calles (Activo)
								</button>
								<button id="btnToggleEntregas" class="btn btn-outline-success btn-sm font-weight-bold mr-2 mb-1" title="Mostrar u ocultar los marcadores de pedidos en el mapa">
									<i class="fas fa-box mr-1"></i> Pedidos (Activo)
								</button>
							</div>
							<div class="d-flex align-items-center mb-1">
								<span class="badge badge-dark mr-2 p-2 shadow-sm" id="badgeHoraActualRuta"><i class="fas fa-clock mr-1"></i> --:--:--</span>
								<span class="badge badge-info p-2 shadow-sm" id="badgeProgresoRuta">0%</span>
							</div>
						</div>

						<!-- Slider de tiempo interactivo -->
						<div class="slider-tiempo-container">
							<input type="range" class="custom-range" id="sliderTiempoRuta" min="0" max="100" value="0">
							<div class="d-flex justify-content-between time-labels mt-1">
								<small id="labelHoraInicio" class="font-weight-bold text-dark">--:--</small>
								<small class="text-muted"><i class="fas fa-arrows-alt-h mr-1"></i> Arrastra para avanzar o retroceder en el tiempo</small>
								<small id="labelHoraFin" class="font-weight-bold text-dark">--:--</small>
							</div>
						</div>
					</div>

					<!-- Filtro de Rango Horario (Hora Inicial - Hora Final) -->
					<div class="filtro-rango-horas mb-3 p-2 border rounded bg-light d-flex flex-wrap align-items-center justify-content-between">
						<div class="d-flex align-items-center flex-wrap">
							<span class="font-weight-bold mr-2 text-secondary small"><i class="fas fa-filter mr-1"></i> Filtrar Tramo Horario:</span>
							<label class="mr-1 mb-0 small text-muted" for="filtroHoraDesde">Desde:</label>
							<input type="time" id="filtroHoraDesde" class="form-control form-control-sm mr-2 mb-1 mb-md-0" style="width: auto;">
							<label class="mr-1 mb-0 small text-muted" for="filtroHoraHasta">Hasta:</label>
							<input type="time" id="filtroHoraHasta" class="form-control form-control-sm mr-2 mb-1 mb-md-0" style="width: auto;">
							<button id="btnAplicarFiltroHoras" class="btn btn-primary btn-sm mr-2 mb-1 mb-md-0">
								<i class="fas fa-check mr-1"></i> Aplicar
							</button>
							<button id="btnRestablecerHoras" class="btn btn-outline-secondary btn-sm mb-1 mb-md-0">
								<i class="fas fa-undo mr-1"></i> Ver Todo el Día
							</button>
						</div>
						<div id="resumenFiltroHoras" class="small text-muted font-italic mt-1 mt-md-0"></div>
					</div>

					<!-- Layout de Dos Columnas: Cronología y Mapa -->
					<div class="row">
						<!-- Columna Izquierda: Pestañas de Despachos y Cronología GPS -->
						<div class="col-12 col-lg-4 mb-3 mb-lg-0">
							<div class="tabla-puntos-card border rounded shadow-sm p-2 bg-white h-100">
								<ul class="nav nav-pills nav-fill mb-2" id="pills-tab-recorrido" role="tablist">
									<li class="nav-item">
										<a class="nav-link active py-1 px-2 small font-weight-bold" id="tab-despachos-btn" data-toggle="pill" href="#tab-despachos" role="tab">
											<i class="fas fa-boxes mr-1"></i> Despachos (<span id="badgeTotalDespachos">0</span>)
										</a>
									</li>
									<li class="nav-item">
										<a class="nav-link py-1 px-2 small font-weight-bold" id="tab-puntos-btn" data-toggle="pill" href="#tab-puntos" role="tab">
											<i class="fas fa-list-ol mr-1"></i> Puntos GPS (<span id="badgeTotalPuntosLista">0</span>)
										</a>
									</li>
								</ul>
								<div class="tab-content" id="pills-tabContent-recorrido">
									<div class="tab-pane fade show active" id="tab-despachos" role="tabpanel">
										<div id="lista-despachos-body" class="contenedor-lista-despachos" style="max-height: 380px; overflow-y: auto;">
											<div class="text-center text-muted py-3 small"><i class="fas fa-spinner fa-spin mr-1"></i> Consultando despachos...</div>
										</div>
									</div>
									<div class="tab-pane fade" id="tab-puntos" role="tabpanel">
										<div class="contenedor-lista-puntos" style="max-height: 380px; overflow-y: auto;">
											<table id="table-detalles" class="table table-sm table-hover mb-0">
												<thead class="thead-light">
													<tr>
														<th>Hora</th>
														<th class="text-right">Ir</th>
													</tr>
												</thead>
												<tbody id="puntos-recorrido-body"></tbody>
											</table>
										</div>
									</div>
								</div>
							</div>
						</div>

						<!-- Columna Derecha: Mapa ampliado con info -->
						<div class="col-12 col-lg-8">
							<div id="mapa-detalle" class="shadow-sm" style="height: 420px; border-radius: 8px; width: 100%;"></div>
							<div class="mensaje-error mt-2" id="errorMensaje" style="display: none;"></div>
							<div id="info" class="mt-2 p-2 border rounded bg-white shadow-sm d-flex align-items-center">
								<div class="mr-3 text-primary"><i class="fas fa-map-marked-alt fa-2x"></i></div>
								<div style="flex: 1;">
									<strong class="text-secondary small text-uppercase">Dirección en este instante:</strong>
									<div id="textoDireccionDetalle" class="font-weight-bold text-dark small mt-1">Seleccione o reproduzca un punto para consultar la dirección...</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="modal-footer py-2 d-flex justify-content-between">
					<small class="text-muted"><i class="fas fa-info-circle mr-1"></i> Use los controles de velocidad o arrastre la barra para simular el trayecto.</small>
					<button type="button" class="btn btn-secondary" data-dismiss="modal">
						<i class="fas fa-times mr-1"></i> Cerrar
					</button>
				</div>
			</div>
		</div>
	</div>
		<script
			src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.16.1/umd/popper.min.js"></script>
		<script
			src="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
     <!-- SweetAlert2 CDN -->
		<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<link rel="stylesheet" href="https://unpkg.com/leaflet-routing-machine@latest/dist/leaflet-routing-machine.css" />
<script src="https://unpkg.com/leaflet-routing-machine@latest/dist/leaflet-routing-machine.js"></script>



		<script src="js/mapa.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>

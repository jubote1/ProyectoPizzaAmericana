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
<title>Mapa en Tiempo Real</title>

<!-- Bootstrap 4 -->
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
<!-- Mapbox -->
<link href="https://api.mapbox.com/mapbox-gl-js/v3.9.3/mapbox-gl.css" rel="stylesheet">
<script src="https://api.mapbox.com/mapbox-gl-js/v3.9.3/mapbox-gl.js"></script>
<script src="https://api.mapbox.com/mapbox-gl-js/plugins/mapbox-gl-directions/v4.3.1/mapbox-gl-directions.js"></script>
<link rel="stylesheet" href="https://api.mapbox.com/mapbox-gl-js/plugins/mapbox-gl-directions/v4.3.1/mapbox-gl-directions.css" type="text/css">


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

<link rel="stylesheet" href="css/mapa.css">
</head>
<body>

	<div id="paginador" class="d-flex justify-content-between">
		<button id="anterior" class="btn btn-secondary" disabled>
			<i class="fas fa-arrow-left"></i> Anterior
		</button>
		<button id="siguiente" class="btn btn-secondary">
			Siguiente <i class="fas fa-arrow-right"></i>
		</button>
	</div>

	<div id="pagina-mapa" class="pagina-activa">
		<h1 class="titulo-pagina">Mapa en Tiempo Real</h1>
		<div class="contenedor-general">

			<div class="container">

				<div id="map"></div>

				<div class="tienda-selector">
					<label for="tiendaSelect" class="mr-2">Tienda</label> <select
						id="tiendaSelect" class="form-control">
						<option value="0">Todas</option>
					</select>
				</div>

			</div>
			<div class="container">
				<table id="table-container" class="table table-bordered">
					<thead>
						<tr>
							<th colspan="3" class="centered-header">Rastreo</th>
						</tr>
						<tr>
							<th>Usuario</th>
							<th>Fecha</th>
							<th>Tienda</th>
						</tr>
					</thead>
					<tbody id="user-table-body"></tbody>
				</table>
				<div id="user-details">
					<h3>Detalles del Usuario</h3>
					<p>Seleccione un registro de la tabla para mostrar la
						información...</p>
				</div>
			</div>
		</div>
	</div>

	<div id="pagina-historial" class="pagina-oculta">

		<h1 class="titulo-pagina">Historial de Ubicaciones</h1>
		<!-- Filtros -->
		<div class="tienda-selector">
			<label for="tiendaHistorial" class="mr-2">Tienda</label> <select
				id="tiendaHistorial" class="form-control">
				<option value="0">Todas</option>
			</select>
		</div>

		<div class="date-range-selector">
			<label for="startDate" class="mr-2">Desde:</label> <input type="date"
				id="startDate" class="form-control mr-3"> <label
				for="endDate" class="mr-2">Hasta:</label> <input type="date"
				id="endDate" class="form-control mr-3">
			<button id="filter" class="btn btn-primary">Filtrar</button>
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

			<!-- Modal para los detalles del usuario -->
			<div class="modal fade" id="modalDetalles" tabindex="-1"
				role="dialog" aria-labelledby="modalDetallesLabel"
				aria-hidden="true">
				<div class="modal-dialog" role="document">
					<div class="modal-content">
						<div class="modal-header">
							<h5 class="modal-title" id="modalDetallesLabel">Detalles del
								Usuario</h5>
							<button type="button" class="close" data-dismiss="modal"
								aria-label="Close">
								<span aria-hidden="true">&times;</span>
							</button>
						</div>
						<div class="modal-body">
							<div id="detalles">
								<div class="contenedor-detalle">
									<!-- Tabla de detalles -->
											
											<div class="content-tab">	
															<table id="table-detalles" class="table table-bordered">
										<thead>
											<tr>
												<th  colspan="3" class="centered-header" id="header-usuario"></th>
											<tr>
												<th   colspan="3" class="centered-header" id="header-tienda"></th>
											</tr>
											<tr>
												<th>Fecha y Hora</th>
												<th>Centrar</th>
											    <th>Enrutar</th>
											</tr>
										</thead>
										<tbody></tbody>
									</table>
											
											</div>
								
										
								

									<!-- Mapa de detalles -->
									<div class="container-info">

										<div id="mapa-detalle"></div>
										<div class="mensaje-error"   id="errorMensaje"></div>
										<div id="info">
											<h3>Dirección</h3>
											<p>Seleccione un registro de la tabla para mostrar la
												información...</p>
										</div>

									</div>


								</div>
							</div>
							<div class="modal-footer">
								<button type="button" class="btn btn-secondary"
									data-dismiss="modal">Cerrar</button>
							</div>
						</div>
					</div>
				</div>

			</div>



		</div>
		<script
			src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.16.1/umd/popper.min.js"></script>
		<script
			src="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
     
		<script src="js/mapa.js"></script>
</body>
</html>

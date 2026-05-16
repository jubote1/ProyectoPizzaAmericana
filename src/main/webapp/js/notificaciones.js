if (!window.notificacionesCentralInicializado) {
    window.notificacionesCentralInicializado = true;
	
	
	var audioNotificacion = new Audio('sounds/notificacion.mp3');
	audioNotificacion.volume = 0.8;
	audioNotificacion.preload = 'auto';

	function iniciarNotificacionesCentral() {


		pintarToastContainer();
		prepararMenuNotificaciones();
		cargarHistorialNotificaciones();
		revisarPermisoNotificaciones();

		if (window.socketNotificacionesCentral) {
		    return;
		}
		
	    var protocolo = location.protocol === "https:" ? "wss://" : "ws://";
	    var contextPath = window.location.pathname.split("/")[1];

	    window.socketNotificacionesCentral = new WebSocket(
	        protocolo + location.host + "/" + contextPath + "/notificaciones"
	    );

		window.socketNotificacionesCentral.onmessage = function (event) {
		    var notificacion = JSON.parse(event.data);

			mostrarToastNotificacion(notificacion);
			mostrarNotificacionSistema(notificacion);
			reproducirSonidoNotificacion();
			agregarNotificacionMenu(notificacion, true);
		};


	    window.socketNotificacionesCentral.onerror = function (error) {
	        console.log("Error WebSocket notificaciones", error);
	    };

	    window.socketNotificacionesCentral.onclose = function () {
	        console.log("WebSocket notificaciones cerrado");
	        window.socketNotificacionesCentral = null;
	    };
	}

	function pintarToastContainer() {
	    if ($('#toastNotificacionesContainer').length > 0) {
	        return;
	    }

	    $('body').append(
	        '<div id="toastNotificacionesContainer" ' +
	        'style="position: fixed; top: 90px; right: 20px; z-index: 99999;"></div>'
	    );
	}
	
	function reproducirSonidoNotificacion() {

	    try {

	        audioNotificacion.pause();
	        audioNotificacion.currentTime = 0;

	        var promesa = audioNotificacion.play();

	        if (promesa !== undefined) {
	            promesa.catch(function () {
	                console.log("El navegador bloqueo el sonido");
	            });
	        }

	    } catch (e) {
	        console.log("Error reproduciendo sonido", e);
	    }
	}
	function mostrarModalPermisoNotificaciones() {
	    if ($('#modalPermisoNotificaciones').length > 0) {
	        $('#modalPermisoNotificaciones').modal('show');
	        return;
	    }

	    var html =
	        '<div class="modal fade" id="modalPermisoNotificaciones" tabindex="-1" role="dialog" aria-hidden="true">' +
	        '  <div class="modal-dialog modal-dialog-centered" role="document">' +
	        '    <div class="modal-content">' +
	        '      <div class="modal-header">' +
	        '        <h5 class="modal-title">Activar notificaciones</h5>' +
	        '        <button type="button" class="close" data-dismiss="modal" aria-label="Cerrar">' +
	        '          <span aria-hidden="true">&times;</span>' +
	        '        </button>' +
	        '      </div>' +
	        '      <div class="modal-body">' +
	        '        <p>Las notificaciones del navegador estan bloqueadas para este sitio.</p>' +
	        '        <ol class="mb-0">' +
	        '          <li>Haz clic en el icono al lado izquierdo de la direccion del navegador.</li>' +
	        '          <li>Busca la opcion <strong>Notificaciones</strong>.</li>' +
	        '          <li>Cambia el permiso a <strong>Permitir</strong>.</li>' +
	        '          <li>Recarga la pagina.</li>' +
	        '        </ol>' +
	        '      </div>' +
	        '      <div class="modal-footer">' +
	        '        <button type="button" class="btn btn-secondary" data-dismiss="modal">Entendido</button>' +
	        '      </div>' +
	        '    </div>' +
	        '  </div>' +
	        '</div>';

	    $('body').append(html);
	    $('#modalPermisoNotificaciones').modal('show');
	}


	$(document).on('click', '#btnComoActivarNotificaciones', function () {
	    mostrarModalPermisoNotificaciones();
	});
	
	$(document).on('click', '#btnCerrarAvisoNotificaciones', function () {
	    $('#avisoPermisoNotificaciones').remove();
	});

	
	function revisarPermisoNotificaciones() {
	    if (!("Notification" in window)) {
	        return;
	    }

	    if (Notification.permission === "denied") {
	        mostrarAvisoPermisoNotificaciones();
	    }
	}

	function mostrarAvisoPermisoNotificaciones() {

	    if ($('#avisoPermisoNotificaciones').length > 0) {
	        return;
	    }

	    var html =
	        '<div id="avisoPermisoNotificaciones" ' +
	        'style="' +
	        'position:fixed;' +
	        'right:18px;' +
	        'bottom:18px;' +
	        'z-index:99999;' +
	        'width:320px;' +
	        'background:#fcfdff;border:1px solid #e7ebf0;' +
	        'border-radius:14px;' +
	        'box-shadow:0 14px 34px rgba(15,23,42,.16);' +
	        'padding:14px 15px;' +
	        'font-family:Sans-Serif;' +
	        '">' +

	        '<div style="display:flex;align-items:flex-start;">' +

	        '<div style="' +
	        'width:36px;' +
	        'height:36px;' +
	        'border-radius:10px;' +
	        'background:#fff5d8;' +
	        'display:flex;' +
	        'align-items:center;' +
	        'justify-content:center;' +
	        'margin-right:12px;' +
	        'flex-shrink:0;' +
	        '">' +
	        '<i class="fas fa-bell-slash" style="color:#d9a600;font-size:15px;"></i>' +
	        '</div>' +

	        '<div style="flex:1;">' +

	        '<div style="' +
	        'font-size:14px;' +
	        'font-weight:700;' +
	        'color:#2f343a;' +
	        'margin-bottom:4px;' +
	        '">' +
	        'Notificaciones bloqueadas' +
	        '</div>' +

	        '<div style="' +
	        'font-size:12px;' +
	        'line-height:1.4;' +
	        'color:#6c757d;' +
	        '">' +
	        'Permite las notificaciones del navegador para recibir avisos.' +
	        '</div>' +

	        '<div style="margin-top:12px;display:flex;gap:8px;">' +

	        '<button type="button" id="btnComoActivarNotificaciones" ' +
	        'style="' +
	        'border:0;' +
	        'background:#202875;' +
	        'color:#fff;' +
	        'padding:7px 12px;' +
	        'border-radius:9px;' +
	        'font-size:11px;' +
	        'font-weight:600;' +
	        'cursor:pointer;' +
	        '">' +
	        'Ver como' +
	        '</button>' +

	        '<button type="button" id="btnCerrarAvisoNotificaciones" ' +
	        'style="' +
	        'border:0;' +
	        'background:#eef1f4;' +
	        'color:#5f6973;' +
	        'padding:7px 12px;' +
	        'border-radius:9px;' +
	        'font-size:11px;' +
	        'font-weight:600;' +
	        'cursor:pointer;' +
	        '">' +
	        'Cerrar' +
	        '</button>' +

	        '</div>' +
	        '</div>' +
	        '</div>' +
	        '</div>';

	    $('body').append(html);
	}

	function mostrarToastNotificacion(notificacion) {

	    var idToast = 'toastNotificacion_' + new Date().getTime();

	    var origen = (notificacion.origen || '').toUpperCase();

	    var colorBarra = '#202875';
	    var fondo = '#f4f6ff';
	    var fondoIcono = '#e7ebff';
	    var icono = 'fa-bell';

	    if (origen === 'RAPPI') {
	        colorBarra = '#dc6a32';
	        fondo = '#fff6f1';
	        fondoIcono = '#ffe8dc';
	        icono = 'fa-motorcycle';
	    }

	    if (origen === 'DIDI') {
	        colorBarra = '#d4a100';
	        fondo = '#fffbea';
	        fondoIcono = '#fff1bf';
	        icono = 'fa-store';
	    }

	    var html =
	        '<div id="' + idToast + '" ' +
	        'style="' +
	        'min-width:340px;' +
	        'max-width:420px;' +
	        'margin-bottom:14px;' +
	        'background:' + fondo + ';' +
	        'border-radius:10px 18px 18px 10px;' +
	        'overflow:hidden;' +
	        'box-shadow:0 14px 34px rgba(15,23,42,.16);' +
	        'display:flex;' +
	        'border:1px solid rgba(255,255,255,.7);' +
	        'backdrop-filter:blur(8px);' +
	        '">' +

	            '<div style="' +
	            'width:5px;' +
	            'background:' + colorBarra + ';' +
	            '"></div>' +

	            '<div style="' +
	            'padding:14px 15px;' +
	            'display:flex;' +
	            'align-items:flex-start;' +
	            'width:100%;' +
	            '">' +

	                '<div style="' +
	                'width:38px;' +
	                'height:38px;' +
	                'border-radius:11px;' +
	                'background:' + fondoIcono + ';' +
	                'display:flex;' +
	                'align-items:center;' +
	                'justify-content:center;' +
	                'margin-right:12px;' +
	                'flex-shrink:0;' +
	                '">' +

	                    '<i class="fas ' + icono + '" ' +
	                    'style="color:' + colorBarra + ';font-size:15px;"></i>' +

	                '</div>' +

	                '<div style="flex:1;">' +

	                    '<div style="' +
	                    'font-size:14px;' +
	                    'font-weight:700;' +
	                    'color:' + colorBarra + ';' +
	                    'margin-bottom:4px;' +
	                    '">' +
	                    escaparHtml(notificacion.origen || 'NOTIFICACION') +
	                    '</div>' +

	                    '<div style="' +
	                    'font-size:13px;' +
	                    'line-height:1.45;' +
	                    'color:#434a54;' +
	                    '">' +
	                    escaparHtml(notificacion.mensaje) +
	                    '</div>' +

	                '</div>' +

	            '</div>' +

	        '</div>';

	    $('#toastNotificacionesContainer').append(html);

	    $('#' + idToast)
	        .hide()
	        .fadeIn(180);

	    setTimeout(function () {
	        $('#' + idToast).fadeOut(280, function () {
	            $(this).remove();
	        });
	    }, 6500);
	}
	
	function actualizarContadorDesdeLista() {
	    var pendientes = $('.notificacion-item[data-notificado="N"]').length;

	    if (pendientes > 0) {
	        $('#contadorNotificaciones').text(pendientes).show();
	    } else {
	        $('#contadorNotificaciones').hide();
	    }
	}

	function agregarNotificacionMenu(notificacion) {
	    var notificado = notificacion.notificado || 'N';
	    var claseEstado = notificado === 'S' ? 'notificacion-leida' : 'notificacion-nueva';
	    var origenClase = obtenerClaseOrigenNotificacion(notificacion.origen);
	    var hora = obtenerHoraNotificacion(notificacion.fecha_hora);

	    var item =
	        '<a href="#" class="notificacion-item ' + claseEstado + ' ' + origenClase + '" ' +
	        'data-id="' + notificacion.id + '" data-notificado="' + notificado + '">' +
	        '<div class="notificacion-linea-superior">' +
	        '  <div class="notificacion-titulo">' + escaparHtml(notificacion.origen || 'RAPPI') + '</div>' +
	        '  <div class="notificacion-hora">' + escaparHtml(hora) + '</div>' +
	        '</div>' +
	        '<div class="notificacion-mensaje">' + escaparHtml(notificacion.mensaje) + '</div>' +
	        '</a>';

	    if ($('#listaNotificaciones .text-muted, #listaNotificaciones .notificaciones-vacio').length > 0) {
	        $('#listaNotificaciones').html('');
	    }

	    $('#listaNotificaciones').prepend(item);
	    actualizarContadorDesdeLista();
	}

	function obtenerClaseOrigenNotificacion(origen) {
	    origen = (origen || '').toUpperCase();

	    if (origen === 'DIDI') {
	        return 'notificacion-origen-didi';
	    }

	    if (origen === 'RAPPI') {
	        return 'notificacion-origen-rappi';
	    }

	    return 'notificacion-origen-default';
	}



	function obtenerIconoNotificacion(origen) {
	    origen = (origen || '').toUpperCase();

	    if (origen === 'RAPPI') {
	        return 'images/rappi.png';
	    }

	    if (origen === 'DIDI') {
	        return 'images/didi.png';
	    }

	    return 'images/logo-sin-fondo.png';
	}

	function sumarContadorNotificaciones() {
	    var actual = parseInt($('#contadorNotificaciones').text() || '0', 10);
	    var nuevo = actual + 1;

	    $('#contadorNotificaciones').text(nuevo).show();
	}

	function cargarHistorialNotificaciones() {
	    if ($('#listaNotificaciones').length === 0) {
	        return;
	    }

	    $.ajax({
	        url: 'NotificacionesServlet',
	        method: 'GET',
	        data: { accion: 'listar' },
	        success: function (respuesta) {
	            var lista = typeof respuesta === 'string' ? JSON.parse(respuesta) : respuesta;

	            $('#listaNotificaciones').html('');

	            if (!lista || lista.length === 0) {
	                $('#listaNotificaciones').html('<div class="dropdown-item text-muted">Sin notificaciones</div>');
	                $('#contadorNotificaciones').hide();
	                return;
	            }

	            var pendientes = 0;

				lista.reverse().forEach(function (n) {

				    agregarNotificacionMenu(n, false);

				    if (n.notificado === 'N') {
				        pendientes++;
				    }
				});

	            if (pendientes > 0) {
	                $('#contadorNotificaciones').text(pendientes).show();
	            } else {
	                $('#contadorNotificaciones').hide();
	            }
	        },
	        error: function (xhr) {
	            console.log("Error cargando historial de notificaciones", xhr.responseText);
	        }
	    });
	}


	function mostrarNotificacionSistema(notificacion) {
	    if (!("Notification" in window)) {
	        return;
	    }

	    if (Notification.permission === "granted") {
	        new Notification("Pedido cancelado en " + (notificacion.origen || "plataforma"), {
	            body: notificacion.mensaje,
	            icon: obtenerIconoNotificacion(notificacion.origen),
	            tag: "notificacion-" + notificacion.id,
	            requireInteraction: true
	        });
	    }
	}

	function obtenerHoraNotificacion(fechaHora) {
	    if (!fechaHora) {
	        return '';
	    }

	    var partes = fechaHora.split(' ');
	    if (partes.length < 2) {
	        return '';
	    }

	    return partes[1].substring(0, 5);
	}


	$(document).on('hidden.bs.dropdown', '#contenedorNotificaciones', function () {
	    marcarTodasNotificacionesComoVistas();
	});


	function marcarTodasNotificacionesComoVistas() {
		
		
		if ("Notification" in window && Notification.permission === "default") {
		    Notification.requestPermission();
		}

	    $('.notificacion-item')
	        .attr('data-notificado', 'S')
	        .removeClass('notificacion-nueva')
	        .addClass('notificacion-leida');

	    $('#contadorNotificaciones').hide().text('0');

	    $.ajax({
	        url: 'NotificacionesServlet',
	        method: 'POST',
	        dataType: 'json',
	        data: {
	            accion: 'marcarTodasVistas'
	        },
	        error: function (xhr) {
	            console.log("Error marcando notificaciones vistas", xhr.responseText);
	        }
	    });
	}



	function escaparHtml(texto) {
	    if (!texto) {
	        return '';
	    }

	    return texto
	        .replace(/&/g, '&amp;')
	        .replace(/</g, '&lt;')
	        .replace(/>/g, '&gt;')
	        .replace(/"/g, '&quot;')
	        .replace(/'/g, '&#039;');
	}

	function prepararMenuNotificaciones() {

	    if ($('#menuprincipal').length === 0) {
	        return;
	    }

	    if ($('#contenedorNotificaciones').length > 0) {
	        return;
	    }

		var htmlNotificaciones =
		    '<div class="dropdown mr-3" id="contenedorNotificaciones">' +
		    '  <a href="#" class="notificaciones-boton position-relative" id="btnNotificaciones" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">' +
		    '    <i class="fas fa-bell"></i>' +
		    '    <span id="contadorNotificaciones" class="notificaciones-contador" style="display: none;">0</span>' +
		    '  </a>' +
		    '  <div class="dropdown-menu dropdown-menu-right notificaciones-panel">' +
		    '    <div class="notificaciones-header">' +
		    '      <span>Notificaciones</span>' +
		    '      <small>Hoy</small>' +
		    '    </div>' +
		    '    <div id="listaNotificaciones" class="notificaciones-lista">' +
		    '      <div class="notificaciones-vacio">Sin notificaciones</div>' +
		    '    </div>' +
		    '  </div>' +
		    '</div>';


	    $('#usuariologin').before(htmlNotificaciones);
	}

	$(document).ajaxComplete(function (event, xhr, settings) {
	    if (!settings || !settings.url) {
	        return;
	    }

	    var url = settings.url.toLowerCase();

	    if (
	        url.indexOf('menu.html') !== -1 ||
	        url.indexOf('menuadm.html') !== -1 ||
	        url.indexOf('menupqrs.html') !== -1
	    ) {
	        iniciarNotificacionesCentral();
	    }
	});


	$(document).ready(function () {
	    iniciarNotificacionesCentral();
	});

}



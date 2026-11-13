/**
 * Planificador de eventos especiales.
 *
 * Dos vistas sobre los mismos datos: un calendario del mes -que es el "visual de
 * todos" que se pidio- y un listado para buscar y editar. El mes se consulta
 * completo al servidor; filtrar por estado, tipo, tienda o texto vuelve a pedirlo,
 * porque el filtro se aplica en la consulta y no en el navegador.
 */

var server;
var usuarioActivo = 'asesor_cc';
var anioVista;
var mesVista;              // 0 = enero, como en Date
var eventosDelMes = [];
var dtEventos = null;
var tiendas = [];

var MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
             'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

var SIGUIENTE_ESTADO = {
    'TENTATIVO': 'APARTADO',
    'APARTADO': 'CONFIRMADO',
    'CONFIRMADO': 'ENTREGADO'
};

$(document).ready(function () {
    var loc = window.location;
    var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
    server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

    if (typeof usuario !== 'undefined' && usuario !== '') {
        usuarioActivo = usuario;
    }
    $('#label-usuario').text(usuarioActivo);

    var hoy = new Date();
    anioVista = hoy.getFullYear();
    mesVista = hoy.getMonth();

    inicializarTabla();
    cargarTiendas();

    $('#btn-mes-anterior').on('click', function () { moverMes(-1); });
    $('#btn-mes-siguiente').on('click', function () { moverMes(1); });
    $('#btn-hoy').on('click', function () {
        var h = new Date();
        anioVista = h.getFullYear();
        mesVista = h.getMonth();
        cargar();
    });

    $('#filtro-estado, #filtro-tipo, #filtro-tienda').on('change', cargar);

    // El texto no dispara una consulta por tecla: se espera a que la persona pare.
    var reloj = null;
    $('#filtro-texto').on('keyup', function () {
        clearTimeout(reloj);
        reloj = setTimeout(cargar, 450);
    });

    $('#btn-nuevo').on('click', function () { abrirModal(null, null); });
    $('#form-evento').on('submit', function (e) { e.preventDefault(); guardar(); });

    // Sugerencia del 20% mientras se digita el valor.
    $('#ev-valor').on('input', function () {
        var v = parseFloat($(this).val()) || 0;
        $('#ev-sugerido').text(v > 0 ? 'El 20% de ' + pesos(v) + ' son ' + pesos(Math.round(v * 0.2)) : '');
    });
});

// ---------------------------------------------------------------------------
// Datos
// ---------------------------------------------------------------------------

function moverMes(paso) {
    mesVista += paso;
    if (mesVista < 0) { mesVista = 11; anioVista--; }
    if (mesVista > 11) { mesVista = 0; anioVista++; }
    cargar();
}

function diasDelMes(anio, mes) {
    return new Date(anio, mes + 1, 0).getDate();
}

function dosDigitos(n) {
    return (n < 10 ? '0' : '') + n;
}

function fechaISO(anio, mes, dia) {
    return anio + '-' + dosDigitos(mes + 1) + '-' + dosDigitos(dia);
}

function cargar() {
    $('#titulo-mes').text(MESES[mesVista] + ' ' + anioVista);

    var desde = fechaISO(anioVista, mesVista, 1);
    var hasta = fechaISO(anioVista, mesVista, diasDelMes(anioVista, mesVista));

    $('#cuerpo-calendario').html('<tr><td colspan="7" class="text-center text-muted" style="padding:30px;">'
        + '<i class="fas fa-spinner fa-spin"></i> Consultando los eventos del mes...</td></tr>');

    $.ajax({
        url: server + 'ConsultarEventosEspeciales',
        type: 'post',
        dataType: 'json',
        data: {
            fecha_desde: desde,
            fecha_hasta: hasta,
            estado: $('#filtro-estado').val(),
            tipo: $('#filtro-tipo').val(),
            idtienda: $('#filtro-tienda').val(),
            filtro: $('#filtro-texto').val()
        },
        success: function (d) {
            if (!d || d.resultado !== 'OK') {
                avisar('No se pudieron consultar los eventos', (d && d.mensaje) ? d.mensaje : 'El servicio no respondió.');
                eventosDelMes = [];
            } else {
                eventosDelMes = d.eventos || [];
                $('#kpi-eventos').text(d.total_eventos);
                $('#kpi-pizzas').text(d.total_pizzas);
                $('#kpi-valor').text(pesos(d.valor_estimado));
                $('#kpi-anticipos').text(pesos(d.total_anticipos));
            }
            pintarCalendario();
            pintarListado();
        },
        error: function () {
            avisar('Sin respuesta', 'No se pudo hablar con el servidor. Intente de nuevo en un momento.');
            eventosDelMes = [];
            pintarCalendario();
            pintarListado();
        }
    });
}

function cargarTiendas() {
    $.getJSON(server + 'GetTiendasFuncionales', function (data) {
        tiendas = data || [];
        var opciones = '';
        for (var i = 0; i < tiendas.length; i++) {
            opciones += '<option value="' + tiendas[i].id + '">' + esc(tiendas[i].nombre) + '</option>';
        }
        $('#filtro-tienda').append(opciones);
        $('#ev-tienda').append(opciones);
    }).always(function () {
        cargar();
    });
}

// ---------------------------------------------------------------------------
// Calendario
// ---------------------------------------------------------------------------

function pintarCalendario() {
    var porDia = {};
    var pizzasDia = {};
    for (var i = 0; i < eventosDelMes.length; i++) {
        var ev = eventosDelMes[i];
        var dia = parseInt(ev.fecha_evento.substring(8, 10), 10);
        if (!porDia[dia]) { porDia[dia] = []; pizzasDia[dia] = 0; }
        porDia[dia].push(ev);
        if (ev.estado !== 'CANCELADO') {
            pizzasDia[dia] += ev.cantidad_pizzas;
        }
    }

    // La semana arranca en lunes: getDay() da 0 para domingo, asi que se corre.
    var primero = new Date(anioVista, mesVista, 1).getDay();
    var desfase = (primero === 0) ? 6 : primero - 1;
    var total = diasDelMes(anioVista, mesVista);

    var hoy = new Date();
    var esMesDeHoy = (hoy.getFullYear() === anioVista && hoy.getMonth() === mesVista);

    var html = '';
    var celda = 0;
    var dia = 1;

    while (dia <= total) {
        html += '<tr>';
        for (var c = 0; c < 7; c++, celda++) {
            if (celda < desfase || dia > total) {
                html += '<td class="dia vacio"></td>';
                continue;
            }

            var clases = 'dia';
            if (c === 6) { clases += ' domingo'; }
            if (esMesDeHoy && dia === hoy.getDate()) { clases += ' hoy'; }

            html += '<td class="' + clases + '">';
            html += '<div class="numero">' + dia;
            if (pizzasDia[dia]) {
                html += '<span class="pizzas-dia" title="Pizzas comprometidas ese día">'
                      + '<i class="fas fa-pizza-slice"></i> ' + pizzasDia[dia] + '</span>';
            }
            html += '<span class="add" title="Registrar un evento este día" data-fecha="'
                  + fechaISO(anioVista, mesVista, dia) + '"><i class="fas fa-plus"></i></span>';
            html += '</div>';

            var delDia = porDia[dia] || [];
            for (var k = 0; k < delDia.length; k++) {
                var e = delDia[k];
                var quien = e.empresa ? e.empresa : e.nombre_cliente;
                html += '<span class="chip ' + esc(e.estado) + '" data-id="' + e.idevento + '" title="'
                      + esc(e.hora_evento + ' · ' + e.nombre_cliente + ' · ' + e.cantidad_pizzas + ' pizzas · ' + e.estado) + '">'
                      + '<span class="hora">' + esc(e.hora_evento) + '</span> '
                      + esc(quien) + ' <strong>(' + e.cantidad_pizzas + ')</strong></span>';
            }

            html += '</td>';
            dia++;
        }
        html += '</tr>';
    }

    $('#cuerpo-calendario').html(html);

    $('#cuerpo-calendario').off('click', '.add').on('click', '.add', function (e) {
        e.stopPropagation();
        abrirModal(null, $(this).data('fecha'));
    });
    $('#cuerpo-calendario').off('click', '.chip').on('click', '.chip', function () {
        abrirModal(buscarEvento($(this).data('id')), null);
    });
}

function buscarEvento(idevento) {
    for (var i = 0; i < eventosDelMes.length; i++) {
        if (eventosDelMes[i].idevento === idevento) {
            return eventosDelMes[i];
        }
    }
    return null;
}

// ---------------------------------------------------------------------------
// Listado
// ---------------------------------------------------------------------------

function inicializarTabla() {
    dtEventos = $('#tabla-eventos').DataTable({
        'language': { 'url': '//cdn.datatables.net/plug-ins/1.10.24/i18n/Spanish.json' },
        'pageLength': 25,
        'order': [[0, 'asc']],
        'columns': [
            {
                'data': null,
                'render': function (row, type) {
                    if (type !== 'display') { return row.fecha_evento + ' ' + row.hora_evento; }
                    return '<strong>' + esc(fechaLarga(row.fecha_evento)) + '</strong><br>'
                         + '<span style="color:#0e2963; font-weight:700;"><i class="far fa-clock"></i> '
                         + esc(row.hora_evento) + '</span>';
                }
            },
            {
                'data': null,
                'render': function (row, type) {
                    if (type !== 'display') { return row.nombre_cliente; }
                    var h = '<strong>' + esc(row.nombre_cliente) + '</strong>';
                    if (row.telefono) {
                        h += '<br><a href="tel:' + esc(row.telefono) + '" style="color:#0e2963;">'
                           + '<i class="fas fa-phone text-success"></i> ' + esc(row.telefono) + '</a>';
                    }
                    if (row.empresa) {
                        h += '<br><small class="text-muted">' + esc(row.empresa) + '</small>';
                    }
                    return h;
                }
            },
            {
                'data': null,
                'render': function (row, type) {
                    if (type !== 'display') { return row.tipo_evento; }
                    var h = '<span class="badge badge-secondary">' + esc(row.tipo_evento) + '</span>';
                    if (row.nombre_evento) { h += '<br><small>' + esc(row.nombre_evento) + '</small>'; }
                    if (row.detalle_pedido) { h += '<br><small class="text-muted">' + esc(row.detalle_pedido) + '</small>'; }
                    return h;
                }
            },
            {
                'data': 'cantidad_pizzas',
                'render': function (data, type) {
                    if (type !== 'display') { return data; }
                    return '<span class="badge" style="background:#e42528; color:#fff; font-size:12px;">'
                         + '<i class="fas fa-pizza-slice"></i> ' + data + '</span>';
                }
            },
            {
                'data': null,
                'render': function (row) {
                    var h = row.nombre_tienda ? esc(row.nombre_tienda) : '<span class="text-muted">Por definir</span>';
                    if (row.direccion_entrega) { h += '<br><small class="text-muted">' + esc(row.direccion_entrega) + '</small>'; }
                    return h;
                }
            },
            {
                'data': null,
                'render': function (row, type) {
                    if (type !== 'display') { return row.valor_estimado; }
                    var h = pesos(row.valor_estimado);
                    if (row.anticipo > 0) {
                        h += '<br><small style="color:#16704f; font-weight:700;">Abonó ' + pesos(row.anticipo) + '</small>';
                    } else {
                        h += '<br><small class="text-muted">Sin abono</small>';
                    }
                    return h;
                }
            },
            {
                'data': 'estado',
                'render': function (data, type) {
                    if (type !== 'display') { return data; }
                    return '<span class="badge-estado badge-' + esc(data) + '">' + esc(data) + '</span>';
                }
            },
            {
                'data': null,
                'orderable': false,
                'render': function (row) {
                    var h = '<button class="btn btn-sm btn-outline-primary btn-abrir" data-id="' + row.idevento
                          + '" title="Abrir el evento"><i class="fas fa-edit"></i></button>';
                    var siguiente = SIGUIENTE_ESTADO[row.estado];
                    if (siguiente) {
                        h += ' <button class="btn btn-sm btn-outline-success btn-avanzar" data-id="' + row.idevento
                           + '" data-estado="' + siguiente + '" title="Pasar a ' + siguiente + '">'
                           + '<i class="fas fa-arrow-right"></i></button>';
                    }
                    return h;
                }
            }
        ]
    });

    $('#tabla-eventos tbody').on('click', '.btn-abrir', function () {
        abrirModal(buscarEvento($(this).data('id')), null);
    });

    $('#tabla-eventos tbody').on('click', '.btn-avanzar', function () {
        var id = $(this).data('id');
        var estado = $(this).data('estado');
        var ev = buscarEvento(id);
        $.confirm({
            title: 'Pasar el evento a ' + estado,
            content: '¿Confirma que el evento de <strong>' + esc(ev ? ev.nombre_cliente : '')
                   + '</strong> pasa a <strong>' + estado + '</strong>?',
            buttons: {
                si: {
                    text: 'Sí, cambiarlo',
                    btnClass: 'btn-blue',
                    action: function () { cambiarEstado(id, estado); }
                },
                no: { text: 'Cancelar' }
            }
        });
    });
}

function pintarListado() {
    dtEventos.clear();
    dtEventos.rows.add(eventosDelMes);
    dtEventos.draw(false);
}

// ---------------------------------------------------------------------------
// Alta y edicion
// ---------------------------------------------------------------------------

function abrirModal(ev, fecha) {
    $('#form-evento')[0].reset();
    $('#ev-sugerido').text('');
    $('#ev-pie').html('');

    if (ev) {
        $('#modal-titulo').text('Evento #' + ev.idevento);
        $('#ev-idevento').val(ev.idevento);
        $('#ev-idcliente').val(ev.idcliente);
        $('#ev-idlead').val(ev.idlead);
        $('#ev-cliente').val(ev.nombre_cliente);
        $('#ev-telefono').val(ev.telefono);
        $('#ev-tipo').val(ev.tipo_evento);
        $('#ev-empresa').val(ev.empresa);
        $('#ev-email').val(ev.email);
        $('#ev-nombre').val(ev.nombre_evento);
        $('#ev-fecha').val(ev.fecha_evento);
        $('#ev-hora').val(ev.hora_evento);
        $('#ev-pizzas').val(ev.cantidad_pizzas);
        $('#ev-tienda').val(ev.idtienda ? ev.idtienda : 0);
        $('#ev-direccion').val(ev.direccion_entrega);
        $('#ev-detalle').val(ev.detalle_pedido);
        $('#ev-valor').val(ev.valor_estimado);
        $('#ev-anticipo').val(ev.anticipo);
        $('#ev-estado').val(ev.estado);
        $('#ev-observacion').val(ev.observacion);

        var pie = 'Registrado por <strong>' + esc(ev.usuario_registra) + '</strong> el ' + esc(ev.creado_en);
        if (ev.modificado_en) {
            pie += ' · Última modificación de <strong>' + esc(ev.usuario_modifica) + '</strong> el ' + esc(ev.modificado_en);
        }
        $('#ev-pie').html(pie);
    } else {
        $('#modal-titulo').text('Registrar evento');
        $('#ev-idevento').val(0);
        $('#ev-idcliente').val(0);
        $('#ev-idlead').val(0);
        $('#ev-hora').val('19:00');
        $('#ev-pizzas').val(3);
        $('#ev-valor').val(0);
        $('#ev-anticipo').val(0);
        $('#ev-tipo').val('NOVENA');
        $('#ev-estado').val('TENTATIVO');
        $('#ev-tienda').val(0);
        $('#ev-fecha').val(fecha ? fecha : fechaISO(anioVista, mesVista, 1));
    }

    $('#modal-evento').modal('show');
}

function guardar() {
    var pizzas = parseInt($('#ev-pizzas').val(), 10);
    if (!$('#ev-cliente').val().trim()) { return avisar('Falta el cliente', 'Escriba el nombre de quien responde por el evento.'); }
    if (!$('#ev-telefono').val().trim()) { return avisar('Falta el teléfono', 'Sin teléfono la tienda no puede confirmar el evento.'); }
    if (!$('#ev-fecha').val()) { return avisar('Falta la fecha', 'Un evento sin fecha no se puede planificar.'); }
    if (!pizzas || pizzas < 1) { return avisar('Faltan las pizzas', 'Registre por lo menos una pizza.'); }

    var anticipo = parseFloat($('#ev-anticipo').val()) || 0;
    var estado = $('#ev-estado').val();
    if (estado === 'APARTADO' && anticipo <= 0) {
        return avisar('Apartado sin abono',
            'Un evento APARTADO es el que ya pagó el anticipo. Registre el abono, o déjelo en TENTATIVO.');
    }

    var datos = {
        idevento: $('#ev-idevento').val(),
        idcliente: $('#ev-idcliente').val(),
        idlead: $('#ev-idlead').val(),
        nombre_cliente: $('#ev-cliente').val().trim(),
        telefono: $('#ev-telefono').val().trim(),
        tipo_evento: $('#ev-tipo').val(),
        empresa: $('#ev-empresa').val().trim(),
        email: $('#ev-email').val().trim(),
        nombre_evento: $('#ev-nombre').val().trim(),
        fecha_evento: $('#ev-fecha').val(),
        hora_evento: $('#ev-hora').val(),
        cantidad_pizzas: pizzas,
        idtienda: $('#ev-tienda').val(),
        direccion_entrega: $('#ev-direccion').val().trim(),
        detalle_pedido: $('#ev-detalle').val().trim(),
        valor_estimado: parseFloat($('#ev-valor').val()) || 0,
        anticipo: anticipo,
        estado: estado,
        observacion: $('#ev-observacion').val().trim(),
        usuario: usuarioActivo
    };

    $.ajax({
        url: server + 'GuardarEventoEspecial',
        type: 'post',
        dataType: 'json',
        data: datos,
        success: function (d) {
            if (!d || d.resultado !== 'OK') {
                avisar('No se guardó', (d && d.mensaje) ? d.mensaje : 'El servicio no respondió.');
                return;
            }
            $('#modal-evento').modal('hide');

            // Si el evento quedó en otro mes, el calendario salta a ese mes:
            // de otro modo se guarda y "desaparece", que es lo que confunde.
            var partes = datos.fecha_evento.split('-');
            anioVista = parseInt(partes[0], 10);
            mesVista = parseInt(partes[1], 10) - 1;
            cargar();

            $.alert({
                title: 'Evento guardado',
                content: 'Quedó planillado el ' + esc(fechaLarga(datos.fecha_evento)) + ' a las '
                       + esc(datos.hora_evento) + ', por ' + datos.cantidad_pizzas + ' pizzas.',
                type: 'green'
            });
        },
        error: function () {
            avisar('Sin respuesta', 'No se pudo guardar: el servidor no respondió.');
        }
    });
}

function cambiarEstado(idevento, estado) {
    $.ajax({
        url: server + 'CambiarEstadoEventoEspecial',
        type: 'post',
        dataType: 'json',
        data: { idevento: idevento, estado: estado, usuario: usuarioActivo },
        success: function (d) {
            if (!d || d.resultado !== 'OK') {
                avisar('No se cambió el estado', (d && d.mensaje) ? d.mensaje : 'El servicio no respondió.');
                return;
            }
            cargar();
        },
        error: function () {
            avisar('Sin respuesta', 'No se pudo cambiar el estado: el servidor no respondió.');
        }
    });
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

function esc(s) {
    if (s === null || s === undefined) { return ''; }
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function pesos(v) {
    var n = Math.round(parseFloat(v) || 0);
    return '$' + n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** '2026-12-18' -> '18 de diciembre' */
function fechaLarga(iso) {
    if (!iso || iso.length < 10) { return iso || ''; }
    var dia = parseInt(iso.substring(8, 10), 10);
    var mes = parseInt(iso.substring(5, 7), 10) - 1;
    return dia + ' de ' + MESES[mes];
}

function avisar(titulo, texto) {
    $.alert({ title: titulo, content: texto, type: 'red' });
}

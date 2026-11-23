/**
 * JavaScript para la Pantalla Unificada de Consulta de Pedidos (Nuevo Modelo y Multicanal).
 * Maneja filtros, KPIs en tiempo real, DataTable interactivo y modal de comparativa sombra.
 */

var tablaPedidosDT = null;

$(document).ready(function() {
    // 1. Inicializar Fechas
    $("#filtroFechaIni").datepicker($.datepicker.regional["es"]);
    $("#filtroFechaIni").datepicker('setDate', new Date());
    $("#filtroFechaFin").datepicker($.datepicker.regional["es"]);
    $("#filtroFechaFin").datepicker('setDate', new Date());

    // 2. Cargar Tiendas
    cargarTiendas();

    // 3. Inicializar DataTable
    inicializarDataTable();

    // 4. Cargar datos iniciales
    cargarPedidos();

    // Enter en caja de busqueda
    $("#filtroNumeroPedido").on('keypress', function(e) {
        if (e.which === 13) {
            cargarPedidos();
        }
    });
});

function cargarTiendas() {
    $.ajax({
        url: 'ObtenerTiendas',
        type: 'GET',
        dataType: 'json',
        success: function(tiendas) {
            var select = $('#filtroTienda');
            select.empty();
            select.append('<option value="0" selected>Todas las Tiendas</option>');
            if (tiendas && tiendas.length > 0) {
                tiendas.forEach(function(t) {
                    select.append('<option value="' + t.idtienda + '">' + t.nombre + '</option>');
                });
            }
        },
        error: function() {
            console.warn("No se pudieron cargar las tiendas.");
        }
    });
}

function inicializarDataTable() {
    tablaPedidosDT = $('#tablaPedidosUnificados').DataTable({
        language: {
            url: "//cdn.datatables.net/plug-ins/1.10.21/i18n/Spanish.json",
            emptyTable: "No se encontraron pedidos con los filtros aplicados"
        },
        order: [[0, "desc"]],
        pageLength: 25,
        columns: [
            { data: "idpedido" },
            { 
                data: "origen",
                render: function(data, type, row) {
                    return formatearBadgeCanal(row.origen, row.canal_legible);
                }
            },
            { 
                data: "idordencomercio",
                render: function(data) {
                    return data && data !== '0' ? '<strong>' + data + '</strong>' : '<span class="text-muted">-</span>';
                }
            },
            { data: "fechainsercion" },
            { data: "tienda" },
            { 
                data: "cliente",
                render: function(data, type, row) {
                    var html = '<div>' + (data || 'Sin nombre') + '</div>';
                    if (row.telefono) {
                        html += '<small class="text-muted"><i class="fas fa-phone"></i> ' + row.telefono + '</small>';
                    }
                    return html;
                }
            },
            { 
                data: "direccion",
                render: function(data, type, row) {
                    var dir = data || '-';
                    if (row.municipio) dir += ' (' + row.municipio + ')';
                    return '<span title="' + dir + '">' + (dir.length > 30 ? dir.substring(0, 30) + '...' : dir) + '</span>';
                }
            },
            { 
                data: "totalneto",
                className: "text-right font-weight-bold",
                render: function(data) {
                    return '$' + Math.round(data).toLocaleString();
                }
            },
            { 
                data: "modo_procesamiento",
                render: function(data) {
                    if (data === 'NUEVO') {
                        return '<span class="badge-modo modo-nuevo"><i class="fas fa-bolt"></i> NUEVO</span>';
                    } else if (data === 'PARALELO') {
                        return '<span class="badge-modo modo-paralelo"><i class="fas fa-clone"></i> PARALELO</span>';
                    } else {
                        return '<span class="badge-modo modo-legacy">LEGACY</span>';
                    }
                }
            },
            { 
                data: "estadopedido",
                render: function(data, type, row) {
                    if (row.idestadopedido === 2) {
                        return '<span class="badge badge-success">Finalizado</span>';
                    } else if (row.idestadopedido === 1) {
                        return '<span class="badge badge-warning">En Curso</span>';
                    } else if (row.idestadopedido === 4) {
                        return '<span class="badge badge-danger">Cancelado</span>';
                    }
                    return '<span class="badge badge-secondary">' + (data || 'Estado ' + row.idestadopedido) + '</span>';
                }
            },
            { 
                data: "enviadopixel",
                render: function(data) {
                    if (data === 1) return '<span class="text-success font-weight-bold"><i class="fas fa-check-circle"></i> Enviado</span>';
                    if (data === 0) return '<span class="text-danger font-weight-bold"><i class="fas fa-clock"></i> Pendiente</span>';
                    if (data === 2) return '<span class="text-info font-weight-bold"><i class="fas fa-credit-card"></i> Pago Virt.</span>';
                    return '<span class="text-muted">' + data + '</span>';
                }
            },
            { 
                data: "numposheader",
                render: function(data) {
                    return data > 0 ? '<span class="badge badge-dark">#' + data + '</span>' : '<span class="text-muted">-</span>';
                }
            },
            { data: "formapago" },
            { 
                data: "idpedido",
                orderable: false,
                render: function(data, type, row) {
                    var btnClass = (row.modo_procesamiento === 'PARALELO') ? 'btn-info' : 'btn-outline-dark';
                    return '<button type="button" class="btn btn-sm ' + btnClass + '" onclick="verDetallePedido(' + data + ')" title="Ver Detalle y Comparativa">' +
                           '<i class="fas fa-eye"></i> Detalle</button>';
                }
            }
        ]
    });
}

function cargarPedidos() {
    var fIni = $("#filtroFechaIni").val();
    var fFin = $("#filtroFechaFin").val();
    var canal = $("#filtroCanal").val();
    var tienda = $("#filtroTienda").val();
    var modo = $("#filtroModo").val();
    var num = $("#filtroNumeroPedido").val();

    toastr.info("Consultando pedidos multicanal...", "", { timeOut: 1500 });

    $.ajax({
        url: 'ConsultarPedidosNuevoMotor',
        type: 'GET',
        data: {
            accion: 'consultar',
            fechainicial: fIni,
            fechafinal: fFin,
            canal: canal,
            idtienda: tienda,
            modo: modo,
            numeropedido: num
        },
        dataType: 'json',
        success: function(resp) {
            if (resp && resp.pedidos) {
                // Actualizar DataTable
                tablaPedidosDT.clear();
                tablaPedidosDT.rows.add(resp.pedidos);
                tablaPedidosDT.draw();

                // Actualizar KPIs
                actualizarKpis(resp.kpis);
                toastr.success("Se encontraron " + resp.pedidos.length + " pedidos.", "", { timeOut: 2000 });
            }
        },
        error: function(xhr, status, error) {
            toastr.error("Error al consultar pedidos: " + error);
        }
    });
}

function actualizarKpis(k) {
    if (!k) return;
    $("#kpiTotalPedidos").text(k.total_pedidos.toLocaleString());
    $("#kpiTotalVentas").text('$' + Math.round(k.total_ventas).toLocaleString());
    $("#kpiTotalParalelo").text(k.modo_paralelo.toLocaleString());
    $("#kpiTotalDiferencias").text(k.con_diferencia.toLocaleString());

    // Pills de Canales
    var htmlPills = '';
    htmlPills += '<span class="pill-canal-contador canal-kuno"><i class="fas fa-globe"></i> Kuno: ' + k.kuno + '</span>';
    htmlPills += '<span class="pill-canal-contador canal-app"><i class="fas fa-mobile-alt"></i> App: ' + k.app + '</span>';
    htmlPills += '<span class="pill-canal-contador canal-bot"><i class="fas fa-robot"></i> Bot: ' + k.bot + '</span>';
    htmlPills += '<span class="pill-canal-contador canal-rappi"><i class="fas fa-motorcycle"></i> Rappi: ' + k.rappi + '</span>';
    htmlPills += '<span class="pill-canal-contador canal-didi"><i class="fas fa-car"></i> DiDi: ' + k.didi + '</span>';
    htmlPills += '<span class="pill-canal-contador canal-contact"><i class="fas fa-headset"></i> Contact: ' + k.contact + '</span>';

    $("#contenedorCanalesPills").html(htmlPills);
}

function formatearBadgeCanal(orig, nombreLegible) {
    var c = (orig || '').toUpperCase();
    var clase = 'canal-contact';
    var icono = 'fa-headset';

    if (c === 'TK' || c === 'TW') {
        clase = 'canal-kuno'; icono = 'fa-globe';
    } else if (c === 'APP') {
        clase = 'canal-app'; icono = 'fa-mobile-alt';
    } else if (c === 'BOT' || c === 'CRM') {
        clase = 'canal-bot'; icono = 'fa-robot';
    } else if (c === 'RAPPI' || c === 'RAP' || c === 'R') {
        clase = 'canal-rappi'; icono = 'fa-motorcycle';
    } else if (c === 'DIDI' || c === 'D') {
        clase = 'canal-didi'; icono = 'fa-car';
    } else if (c === 'MOTOR' || c === 'M') {
        clase = 'canal-motor'; icono = 'fa-bolt';
    }

    return '<span class="badge-canal ' + clase + '"><i class="fas ' + icono + '"></i> ' + (nombreLegible || orig) + '</span>';
}

function verDetallePedido(idPedido) {
    $.ajax({
        url: 'ConsultarPedidosNuevoMotor',
        type: 'GET',
        data: { accion: 'detalle', idpedido: idPedido },
        dataType: 'json',
        success: function(d) {
            $("#txtModalIdPedido").text(idPedido);

            // 1. Lineas Legacy
            var tbodyLeg = $("#tbodyLineasLegacy");
            tbodyLeg.empty();
            if (d.lineas_legacy && d.lineas_legacy.length > 0) {
                d.lineas_legacy.forEach(function(l, idx) {
                    var tr = $('<tr></tr>');
                    tr.append('<td>' + (idx + 1) + '</td>');
                    tr.append('<td><strong>' + l.nombreproducto + '</strong></td>');
                    tr.append('<td>' + l.cantidad + '</td>');
                    tr.append('<td>' + (l.especialidad1 || '-') + '</td>');
                    tr.append('<td>' + (l.especialidad2 || '-') + '</td>');
                    tr.append('<td>' + (l.adicion || '-') + '</td>');
                    tr.append('<td>' + (l.observacion || '-') + '</td>');
                    tr.append('<td class="text-right">$' + Math.round(l.valorunitario).toLocaleString() + '</td>');
                    tr.append('<td class="text-right font-weight-bold">$' + Math.round(l.valortotal).toLocaleString() + '</td>');
                    tbodyLeg.append(tr);
                });
            } else {
                tbodyLeg.append('<tr><td colspan="9" class="text-center text-muted">Sin líneas de producto registradas</td></tr>');
            }

            // 2. Lineas POS Central
            var tbodyPos = $("#tbodyLineasPosCentral");
            tbodyPos.empty();
            if (d.lineas_pos_central && d.lineas_pos_central.length > 0) {
                $("#navItemPosCentral").show();
                d.lineas_pos_central.forEach(function(l) {
                    var tr = $('<tr></tr>');
                    tr.append('<td>' + l.idproducto + '</td>');
                    tr.append('<td>' + l.descripcion + '</td>');
                    tr.append('<td>' + l.cantidad + '</td>');
                    tr.append('<td class="text-right">$' + Math.round(l.valorunitario).toLocaleString() + '</td>');
                    tr.append('<td class="text-right font-weight-bold">$' + Math.round(l.valortotal).toLocaleString() + '</td>');
                    tr.append('<td>' + (l.idmaster > 0 ? l.idmaster : 'Principal') + '</td>');
                    tr.append('<td>' + (l.modificador > 0 ? l.modificador : '-') + '</td>');
                    tbodyPos.append(tr);
                });
            } else {
                $("#navItemPosCentral").hide();
            }

            // 3. Auditoria Sombra / Comparativa si corrió en paralelo
            var panelSom = $("#panelComparativaSombra");
            if (d.sombra) {
                var s = d.sombra;
                var dif = Math.round(s.diferencia * 100) / 100;
                var claseBox = (Math.abs(dif) < 1.0) ? 'comparativa-ok' : 'comparativa-dif';
                var icono = (Math.abs(dif) < 1.0) ? 'fa-check-circle text-success' : 'fa-exclamation-triangle text-danger';

                var html = '<div class="comparativa-box ' + claseBox + '">';
                html += '<div class="d-flex justify-content-between align-items-center">';
                html += '<div><i class="fas ' + icono + ' fa-lg mr-2"></i><strong>Auditoría Paralela (Sombra): ' + s.estado_motor + '</strong></div>';
                html += '<div><span class="badge badge-dark">Motor ' + s.version_motor + '</span> <small class="text-muted">' + s.duracion_ms + ' ms</small></div>';
                html += '</div>';

                html += '<div class="row" style="margin-top: 10px;">';
                html += '<div class="col-md-4"><strong>Total Legacy (Viejito):</strong> $' + Math.round(s.total_viejo).toLocaleString() + '</div>';
                html += '<div class="col-md-4"><strong>Total Nuevo Motor:</strong> $' + Math.round(s.total_motor).toLocaleString() + '</div>';
                html += '<div class="col-md-4"><strong>Diferencia:</strong> <span class="font-weight-bold" style="color:' + (dif === 0 ? '#38a169' : '#e53e3e') + '">$' + dif.toLocaleString() + '</span></div>';
                html += '</div>';

                if (s.avisos_traduccion > 0) {
                    html += '<div style="margin-top: 6px; font-size: 11px; color:#c53030;"><strong>Avisos de Traducción:</strong> ' + s.avisos_traduccion + ' item(s) con discrepancia de SKU/opción.</div>';
                }
                html += '</div>';

                panelSom.html(html).show();
            } else {
                panelSom.hide();
            }

            // Abrir Modal
            $('#modalDetallePedido').modal('show');
        },
        error: function(xhr, status, error) {
            toastr.error("Error al obtener detalle del pedido: " + error);
        }
    });
}

var server;
var dtLeads = null;
var dtVentas = null;
var asesorActivo = "asesor_cc";

$(document).ready(function() {
    var loc = window.location;
    var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
    server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

    if (typeof usuario !== 'undefined' && usuario !== "") {
        asesorActivo = usuario;
    }
    $("#label-asesor").text(asesorActivo);
    $("#ventas-asesor").val(asesorActivo);

    // Fechas por defecto para ventas atribuidas (mes actual y fin de año)
    var today = new Date();
    var yyyy = today.getFullYear();
    $("#ventas-fechaini").val(yyyy + "-11-01");
    $("#ventas-fechafin").val(yyyy + "-12-31");

    // Inicializar tablas
    inicializarTablaLeads();
    inicializarTablaVentas();

    // Cargar datos iniciales
    cargarLeads();

    // Form submit de bitácora
    $("#form-bitacora").on("submit", function(e) {
        e.preventDefault();
        guardarBitacora();
    });
});

function inicializarTablaLeads() {
    dtLeads = $('#tabla-leads').DataTable({
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Spanish.json"
        },
        "pageLength": 25,
        "order": [[6, "desc"]], // Ordenar por Total Pizzas
        "columns": [
            { "data": "idlead" },
            { 
                "data": "nombre_cliente",
                "render": function(data, type, row) {
                    return "<strong>" + data + "</strong><br><small class='text-muted'>ID Cliente: " + row.idcliente + "</small>";
                }
            },
            { 
                "data": "empresa",
                "render": function(data, type, row) {
                    var emp = data && data !== 'N/A' ? data : (row.razon_social ? row.razon_social : 'Persona Natural');
                    return "<span style='font-weight:600; color:#0e2963;'>" + emp + "</span>";
                }
            },
            { 
                "data": "telefono",
                "render": function(data) {
                    if (!data) return "N/A";
                    return "<a href='tel:" + data + "' class='btn btn-xs btn-default'><i class='fas fa-phone text-success'></i> " + data + "</a>";
                }
            },
            { "data": "email" },
            { "data": "direccion" },
            { 
                "data": "total_pizzas",
                "render": function(data, type, row) {
                    var crit = row.criterio_clasificacion ? row.criterio_clasificacion : (data + " pizzas");
                    return "<span class='badge' style='background:#e42528; font-size:11px;'><i class='fas fa-pizza-slice'></i> " + crit + "</span><br><small class='text-muted'>Historial: " + data + " pizzas (" + row.total_pedidos + " ped.)</small>";
                }
            },
            { 
                "data": "factura_electronica",
                "render": function(data, type, row) {
                    if (data === "SI" || data === "SÍ") {
                        return "<span class='label label-success'>SÍ (DIAN)</span><br><small>" + (row.nit_cedula ? row.nit_cedula : '') + "</small>";
                    }
                    return "<span class='label label-default'>NO</span>";
                }
            },
            { 
                "data": "clasificacion",
                "render": function(data) {
                    if (data.indexOf("Empresa") !== -1) {
                        return "<span class='badge-empresa'><i class='fas fa-briefcase'></i> " + data + "</span>";
                    } else if (data.indexOf("Potencial") !== -1) {
                        return "<span class='badge-potencial'><i class='fas fa-building'></i> " + data + "</span>";
                    } else if (data.indexOf("Alto Volumen") !== -1) {
                        return "<span class='badge-volumen'><i class='fas fa-fire'></i> " + data + "</span>";
                    }
                    return "<span class='badge-grupal'>" + data + "</span>";
                }
            },
            { 
                "data": "estado_gestion",
                "render": function(data) {
                    if (data === "APARTADO_CON_5%") {
                        return "<span class='label label-success' style='font-size:11px;'><i class='fas fa-check-circle'></i> APARTADO 5%</span>";
                    } else if (data === "INTERESADO") {
                        return "<span class='label label-info'><i class='fas fa-star'></i> INTERESADO</span>";
                    } else if (data === "VOLVER_A_LLAMAR") {
                        return "<span class='label label-warning'><i class='fas fa-clock'></i> RELLAMAR</span>";
                    } else if (data === "NO_INTERESADO") {
                        return "<span class='label label-danger'>NO INTERESADO</span>";
                    } else if (data === "NO_CONTESTA") {
                        return "<span class='label label-default'>NO CONTESTA</span>";
                    }
                    return "<span class='label label-primary'>PENDIENTE</span>";
                }
            },
            {
                "data": null,
                "orderable": false,
                "render": function(data, type, row) {
                    return "<button class='btn btn-xs btn-primary' onclick='abrirModalBitacora(" + row.idlead + ")' style='background:#0e2963;'>" +
                           "<i class='fas fa-headset'></i> Contactar</button>";
                }
            }
        ]
    });
}

function inicializarTablaVentas() {
    dtVentas = $('#tabla-ventas').DataTable({
        "language": {
            "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Spanish.json"
        },
        "pageLength": 25,
        "columns": [
            { "data": "idpedidotienda" },
            { "data": "nombre_tienda" },
            { "data": "fecha_pedido" },
            { 
                "data": "cliente",
                "render": function(data, type, row) {
                    return "<strong>" + data + "</strong><br><small>" + row.organizacion + "</small>";
                }
            },
            { "data": "telefono" },
            { "data": "asesor" },
            { "data": "fecha_contacto" },
            { 
                "data": "total_pizzas",
                "render": function(data) {
                    return "<strong style='color:#e42528; font-size:13px;'>" + data + " pizzas</strong>";
                }
            },
            { 
                "data": "valor_neto",
                "render": function(data) {
                    return "$ " + Number(data).toLocaleString();
                }
            },
            { 
                "data": "comision",
                "render": function(data) {
                    return "<strong style='color:#38a169;'>$ " + Number(data).toLocaleString() + "</strong>";
                }
            },
            { "data": "pizzas_detalle" }
        ]
    });
}

function cargarLeads() {
    var filtro = $("#filtro-texto").val();
    var clasificacion = $("#filtro-clasificacion").val();
    var estado = $("#filtro-estado").val();

    $.ajax({
        url: server + "BuscarLeadsNovenas",
        type: "GET",
        dataType: "json",
        data: {
            filtro: filtro,
            clasificacion: clasificacion,
            estado: estado,
            limite: 300
        },
        success: function(data) {
            dtLeads.clear();
            if (data && Array.isArray(data)) {
                dtLeads.rows.add(data).draw();
                $("#contador-leads").text(data.length + " Clientes Cargados");
            } else {
                dtLeads.draw();
                $("#contador-leads").text("0 Clientes");
            }
        },
        error: function(err) {
            console.error("Error al cargar leads:", err);
            $.alert({
                title: 'Atención',
                content: 'No se pudieron cargar los leads de la campaña.',
                type: 'red'
            });
        }
    });
}

function abrirModalBitacora(idlead) {
    $.ajax({
        url: server + "BuscarLeadsNovenas",
        type: "GET",
        dataType: "json",
        data: { idlead: idlead },
        success: function(resp) {
            if (resp && resp.lead) {
                var l = resp.lead;
                $("#modal-idlead").val(l.idlead);
                $("#modal-idcliente").val(l.idcliente);
                $("#modal-nombre").text(l.nombre_cliente);
                $("#modal-empresa").text(l.empresa && l.empresa !== 'N/A' ? l.empresa : (l.razon_social ? l.razon_social : 'Persona Natural / Grupo'));
                $("#modal-direccion").text(l.direccion);
                $("#modal-telefono").text(l.telefono);
                $("#modal-email").text(l.email ? l.email : 'Sin correo registrado');
                $("#modal-historial").text(l.total_pizzas + " pizzas en " + l.total_pedidos + " pedidos (Total: $ " + Number(l.valor_total).toLocaleString() + ")");
                $("#modal-pizzas").text(l.pizzas_preferidas ? l.pizzas_preferidas : 'Variadas');
                
                if (l.factura_electronica === "SI" || l.factura_electronica === "SÍ") {
                    $("#modal-fe-badge").text("Factura Electrónica: " + (l.nit_cedula ? l.nit_cedula : 'Registrada')).show();
                } else {
                    $("#modal-fe-badge").hide();
                }

                // Renderizar historial de bitácora
                var htmlTimeline = "";
                if (resp.bitacora && resp.bitacora.length > 0) {
                    resp.bitacora.forEach(function(b) {
                        htmlTimeline += "<div class='timeline-item'>" +
                                        "<strong>" + b.fecha_contacto + "</strong> - <em>" + b.usuario_asesor + "</em> " +
                                        "<span class='label label-info'>" + b.estado_contacto + "</span> " +
                                        (b.pizzas_estimadas > 0 ? "<strong>(" + b.pizzas_estimadas + " pizzas est.)</strong>" : "") +
                                        (b.fecha_tentativa_evento ? " <small class='text-muted'>Fecha evento: " + b.fecha_tentativa_evento + "</small>" : "") +
                                        "<p style='margin:3px 0 0 0; color:#2d3748;'>" + b.observacion + "</p>" +
                                        "</div>";
                    });
                } else {
                    htmlTimeline = "<p class='text-muted'><em>No hay contactos registrados aún para este cliente.</em></p>";
                }
                $("#modal-timeline").html(htmlTimeline);

                // Limpiar campos formulario
                $("#modal-observacion").val("");
                $("#modal-fechatentativa").val("");
                $("#modal-pizzasestimadas").val("4");
                $("#modal-estado").val("INTERESADO");

                $("#modalBitacora").modal("show");
            }
        },
        error: function(err) {
            console.error("Error al obtener detalle del lead:", err);
        }
    });
}

function guardarBitacora() {
    var idlead = $("#modal-idlead").val();
    var idcliente = $("#modal-idcliente").val();
    var estado = $("#modal-estado").val();
    var fechaTentativa = $("#modal-fechatentativa").val();
    var pizzasEstimadas = $("#modal-pizzasestimadas").val();
    var observacion = $("#modal-observacion").val();

    if (!observacion || observacion.trim() === "") {
        $.alert("Por favor ingrese la observación de la llamada.");
        return;
    }

    $.ajax({
        url: server + "GuardarBitacoraNovenas",
        type: "POST",
        dataType: "json",
        data: {
            idlead: idlead,
            idcliente: idcliente,
            usuario_asesor: asesorActivo,
            estado_contacto: estado,
            fecha_tentativa: fechaTentativa,
            pizzas_estimadas: pizzasEstimadas,
            observacion: observacion
        },
        success: function(resp) {
            if (resp && resp.resultado === "OK") {
                $("#modalBitacora").modal("hide");
                $.alert({
                    title: '¡Guardado Exitoso!',
                    content: 'La llamada y nota de bitácora han sido registradas en el sistema.',
                    type: 'green'
                });
                cargarLeads();
            } else {
                $.alert({
                    title: 'Error',
                    content: resp.mensaje ? resp.mensaje : 'No se pudo guardar la bitácora.',
                    type: 'red'
                });
            }
        },
        error: function(err) {
            console.error("Error al guardar bitácora:", err);
            $.alert({
                title: 'Error de Red',
                content: 'Ocurrió un fallo en la comunicación con el servidor.',
                type: 'red'
            });
        }
    });
}

function cargarVentasAtribuidas() {
    var asesor = $("#ventas-asesor").val();
    var fechaIni = $("#ventas-fechaini").val();
    var fechaFin = $("#ventas-fechafin").val();

    $.ajax({
        url: server + "ConsultarVentasAtribuidasNovenas",
        type: "GET",
        dataType: "json",
        data: {
            asesor: asesor,
            fechainicial: fechaIni,
            fechafinal: fechaFin
        },
        success: function(data) {
            dtVentas.clear();
            if (data && data.pedidos) {
                dtVentas.rows.add(data.pedidos).draw();
                $("#kpi-pedidos").text(data.total_pedidos ? data.total_pedidos : 0);
                $("#kpi-pizzas").text(data.total_pizzas ? data.total_pizzas : 0);
                $("#kpi-ventas").text("$ " + Number(data.total_ventas ? data.total_ventas : 0).toLocaleString());
                $("#kpi-comisiones").text("$ " + Number(data.total_comisiones ? data.total_comisiones : 0).toLocaleString());
            } else {
                dtVentas.draw();
                $("#kpi-pedidos").text("0");
                $("#kpi-pizzas").text("0");
                $("#kpi-ventas").text("$ 0");
                $("#kpi-comisiones").text("$ 0");
            }
        },
        error: function(err) {
            console.error("Error al consultar ventas atribuidas:", err);
        }
    });
}

/**
 * Controlador de la Nueva Pantalla de Toma de Pedidos POS (Etapa 3 - Piloto Manrique).
 * Pizza Americana - Integracion directa con el Motor de Pedido Central.
 */

var catalogoMenu = null;
var productoEnEdicion = null;
var draftLineas = []; // [{ idproducto, cantidad, opciones: [idproducto], modificadores: [{idproducto, tipo, cantidad}] }]
var resultadoMotor = null;
var clienteActual = {
    idcliente: 0,
    memcode: 0,
    nombres: "Cliente Mostrador",
    apellidos: "",
    telefono: "3000000000",
    direccion: "Tienda Directa",
    idmunicipio: 1,
    municipio: "Medellin"
};

$(document).ready(function () {
    $("#cargarMenu").load("Menu.html", function() {
        // Cargar menu superior estandar si existe
    });

    cargarTiendas();
    cargarMenuPOS();

    // Eventos
    $("#tipoPedidoSelect").on("change", function () {
        cotizarEnVivo();
    });

    $("#tiendaSelect").on("change", function () {
        verificarTiendaPiloto();
        cotizarEnVivo();
    });

    $("#btnConfirmarPedido").on("click", function () {
        confirmarYDespacharPedido();
    });

    $("#btnAgregarProductoModal").on("click", function () {
        guardarProductoDesdeModal();
    });
});

function cargarTiendas() {
    $.getJSON("api/motor/catalogo?accion=tiendas", function (data) {
        if (data && data.tiendas) {
            var $sel = $("#tiendaSelect");
            $sel.empty();
            $.each(data.tiendas, function (i, t) {
                var isPiloto = (t.idtienda === 11); // 11 = Manrique Piloto
                var label = t.nombre + (isPiloto ? " [PILOTO DIRECTO]" : "");
                var $opt = $("<option></option>").val(t.idtienda).text(label);
                if (isPiloto) {
                    $opt.attr("selected", "selected");
                }
                $sel.append($opt);
            });
            verificarTiendaPiloto();
        }
    });
}

function verificarTiendaPiloto() {
    var id = parseInt($("#tiendaSelect").val());
    if (id === 11 || id === 1) {
        $("#badgePiloto").show();
    } else {
        $("#badgePiloto").hide();
    }
}

function cargarMenuPOS() {
    $("#pills-tab-pos").empty();
    $("#pills-tabContent-pos").empty();

    $.getJSON("api/motor/catalogo?accion=menu", function (data) {
        catalogoMenu = data;
        if (!data || !data.multimenus || data.multimenus.length === 0) {
            $("#pills-tabContent-pos").html("<p class='p-3 text-muted'>No hay categorias configuradas.</p>");
            return;
        }

        $.each(data.multimenus, function (idx, multi) {
            var tabId = "tab-multi-" + multi.idmultimenu;
            var isActive = (idx === 0) ? "active" : "";

            // Boton de pestaña
            var $navItem = $("<li></li>").addClass("nav-item");
            var $link = $("<a></a>")
                .addClass("nav-link " + isActive)
                .attr("id", "pill-" + tabId)
                .attr("data-toggle", "pill")
                .attr("href", "#" + tabId)
                .attr("role", "tab")
                .text(multi.nombre);
            $navItem.append($link);
            $("#pills-tab-pos").append($navItem);

            // Contenedor de la pestaña con la grilla de productos
            var $tabPane = $("<div></div>")
                .addClass("tab-pane fade " + (isActive ? "show active" : ""))
                .attr("id", tabId)
                .attr("role", "tabpanel");

            var $grid = $("<div></div>").addClass("grid-productos");

            if (multi.items && multi.items.length > 0) {
                $.each(multi.items, function (j, item) {
                    var $card = $("<div></div>")
                        .addClass("card-prod")
                        .attr("data-idproducto", item.idproducto)
                        .on("click", function () {
                            abrirModalProducto(item.idproducto);
                        });

                    if (item.promocion && item.promocion === "S") {
                        $card.append("<span class='badge-promo'>PROMO</span>");
                    }

                    var $title = $("<div></div>").addClass("prod-title").text(item.descripcion);
                    var $tipo = $("<div></div>").addClass("prod-tipo").text(item.tipo === "P" ? "Pizza" : (item.tipo === "B" ? "Bebida" : "Item"));
                    var $precio = $("<div></div>").addClass("prod-precio").text("$ " + formatearPesos(item.precio));

                    $card.append($title).append($tipo).append($precio);
                    $grid.append($card);
                });
            } else {
                $grid.html("<p class='text-muted p-2'>Sin productos en esta seccion.</p>");
            }

            $tabPane.append($grid);
            $("#pills-tabContent-pos").append($tabPane);
        });
    });
}

function abrirModalProducto(idProducto) {
    $("#modalProductoBody").html("<div class='text-center p-4'><i class='fas fa-spinner fa-spin fa-2x text-danger'></i><p class='mt-2'>Cargando opciones...</p></div>");
    $("#modalProductoConfig").modal("show");

    $.getJSON("api/motor/catalogo?accion=producto&id=" + idProducto, function (prod) {
        productoEnEdicion = prod;
        renderizarModalProducto(prod);
    }).fail(function () {
        $("#modalProductoBody").html("<div class='alert alert-danger'>Error al cargar ficha del producto.</div>");
    });
}

function renderizarModalProducto(prod) {
    var $c = $("<div></div>");

    var $cab = $("<div></div>").addClass("mb-3 d-flex justify-content-between align-items-center");
    $cab.append("<div><h5 class='mb-0 font-weight-bold text-dark'>" + prod.descripcion + "</h5><small class='text-muted'>" + (prod.tipo === "P" ? "Pizza" : "Producto") + "</small></div>");
    $cab.append("<div class='h4 text-danger font-weight-bold mb-0'>$ " + formatearPesos(prod.precio1) + "</div>");
    $c.append($cab);

    // Productos incluidos automaticos
    if (prod.incluidos && prod.incluidos.length > 0) {
        var $incBox = $("<div></div>").addClass("alert alert-success py-2 px-3 mb-3");
        $incBox.append("<small class='font-weight-bold d-block'><i class='fas fa-check-circle mr-1'></i>Incluye sin costo adicional:</small>");
        var incTxt = [];
        $.each(prod.incluidos, function (i, inc) {
            incTxt.push(inc.descripcion + " ($0)");
        });
        $incBox.append("<small>" + incTxt.join(", ") + "</small>");
        $c.append($incBox);
    }

    // Preguntas forzadas
    if (prod.preguntas && prod.preguntas.length > 0) {
        $.each(prod.preguntas, function (idx, preg) {
            var $pBox = $("<div></div>").addClass("pregunta-seccion").attr("data-idpregunta", preg.idpregunta);
            var minMaxTxt = (preg.minimo > 0) ? " (Elige min " + preg.minimo + " y max " + preg.maximo + ")" : "";
            $pBox.append("<div class='pregunta-titulo'><i class='fas fa-question-circle text-primary mr-1'></i>" + preg.descripcion + "<small class='text-muted ml-1'>" + minMaxTxt + "</small></div>");

            var $opGrid = $("<div></div>").addClass("opciones-grid");
            var esRadio = (preg.maximo === 1);

            $.each(preg.opciones, function (j, op) {
                var $btn = $("<div></div>")
                    .addClass("opcion-btn")
                    .attr("data-idopcion", op.idproducto)
                    .attr("data-idpregunta", preg.idpregunta)
                    .html("<span>" + op.descripcion + "</span>" + (op.precio > 0 ? "<span class='opcion-precio text-danger'>+$ " + formatearPesos(op.precio) + "</span>" : ""));

                $btn.on("click", function () {
                    if (esRadio) {
                        $opGrid.find(".opcion-btn").removeClass("selected");
                        $(this).addClass("selected");
                    } else {
                        var totalSel = $opGrid.find(".opcion-btn.selected").length;
                        if ($(this).hasClass("selected")) {
                            $(this).removeClass("selected");
                        } else {
                            if (preg.maximo > 0 && totalSel >= preg.maximo) {
                                alert("Solo puedes elegir maximo " + preg.maximo + " opciones para: " + preg.descripcion);
                                return;
                            }
                            $(this).addClass("selected");
                        }
                    }
                });

                // Preseleccionar la primera opcion si es obligatoria radio
                if (esRadio && preg.obligaEleccion === 1 && j === 0) {
                    $btn.addClass("selected");
                }

                $opGrid.append($btn);
            });

            $pBox.append($opGrid);
            $c.append($pBox);
        });
    } else {
        $c.append("<p class='text-muted'>Este producto no requiere configuracion adicional.</p>");
    }

    // Cantidad
    var $cantBox = $("<div></div>").addClass("mt-3 d-flex align-items-center justify-content-between p-2 bg-light rounded");
    $cantBox.append("<span class='font-weight-bold'>Cantidad:</span>");
    var $cantControl = $("<div class='d-flex align-items-center' style='gap:8px;'>"
        + "<button class='btn btn-sm btn-outline-secondary' type='button' id='btnMenosModal'>-</button>"
        + "<input type='number' id='cantModal' value='1' min='1' max='50' class='form-control form-control-sm text-center' style='width:60px;' readonly>"
        + "<button class='btn btn-sm btn-outline-secondary' type='button' id='btnMasModal'>+</button>"
        + "</div>");
    $cantBox.append($cantControl);
    $c.append($cantBox);

    $("#modalProductoBody").html($c);

    $("#btnMenosModal").on("click", function() {
        var v = parseInt($("#cantModal").val());
        if (v > 1) { $("#cantModal").val(v - 1); }
    });
    $("#btnMasModal").on("click", function() {
        var v = parseInt($("#cantModal").val());
        if (v < 50) { $("#cantModal").val(v + 1); }
    });
}

function guardarProductoDesdeModal() {
    if (!productoEnEdicion) return;

    var opcionesElegidas = [];
    var faltanObligatorias = false;

    if (productoEnEdicion.preguntas) {
        $.each(productoEnEdicion.preguntas, function (i, preg) {
            var $pBox = $(".pregunta-seccion[data-idpregunta='" + preg.idpregunta + "']");
            var $sel = $pBox.find(".opcion-btn.selected");
            if (preg.obligaEleccion === 1 && preg.minimo > 0 && $sel.length < preg.minimo) {
                alert("Debes seleccionar al menos " + preg.minimo + " opcion(es) para: " + preg.descripcion);
                faltanObligatorias = true;
                return false;
            }
            $sel.each(function () {
                opcionesElegidas.push(parseInt($(this).attr("data-idopcion")));
            });
        });
    }

    if (faltanObligatorias) return;

    var cant = parseInt($("#cantModal").val()) || 1;

    draftLineas.push({
        idproducto: productoEnEdicion.idproducto,
        cantidad: cant,
        opciones: opcionesElegidas,
        modificadores: []
    });

    $("#modalProductoConfig").modal("hide");
    productoEnEdicion = null;

    cotizarEnVivo();
}

function cotizarEnVivo() {
    if (draftLineas.length === 0) {
        $("#listaComanda").html("<p class='text-muted text-center py-4'>La comanda esta vacia.<br><small>Selecciona productos en el menu.</small></p>");
        $("#subtotalTxt").text("$ 0");
        $("#descuentoTxt").text("$ 0");
        $("#domicilioTxt").text("$ 0");
        $("#totalTxt").text("$ 0");
        $("#btnConfirmarPedido").prop("disabled", true);
        return;
    }

    var idTienda = parseInt($("#tiendaSelect").val()) || 11;
    var idTipoPedido = parseInt($("#tipoPedidoSelect").val()) || 1; // 1 = Domicilio

    // Armar MotorSolicitud
    var solicitud = {
        contexto: {
            idtienda: idTienda,
            canal: "T", // T = Contact Center
            idTipoPedido: idTipoPedido,
            fechaHora: new Date().toISOString().replace("T", " ").substring(0, 19)
        },
        cliente: {
            idcliente: clienteActual.idcliente,
            telefono: clienteActual.telefono,
            direccion: clienteActual.direccion
        },
        domicilio: {
            idproducto: (idTipoPedido === 1 ? 412 : 0) // 412 = Domicilio POS
        },
        lineas: draftLineas
    };

    $.ajax({
        type: "POST",
        url: "api/motor/cotizar",
        contentType: "application/json;charset=UTF-8",
        data: JSON.stringify(solicitud),
        success: function (res) {
            resultadoMotor = res;
            renderizarComanda(res);
            $("#btnConfirmarPedido").prop("disabled", false);
        },
        error: function (xhr) {
            console.error("Error cotizando pedido:", xhr.responseText);
            $("#btnConfirmarPedido").prop("disabled", true);
        }
    });
}

function renderizarComanda(res) {
    var $box = $("#listaComanda");
    $box.empty();

    if (!res || !res.lineas) return;

    // Agrupar hijas por refMaster
    var hijasMap = {};
    $.each(res.lineas, function (i, l) {
        if (l.refMaster > 0) {
            if (!hijasMap[l.refMaster]) hijasMap[l.refMaster] = [];
            hijasMap[l.refMaster].push(l);
        }
    });

    var draftIdx = 0;
    $.each(res.lineas, function (i, l) {
        if (l.refMaster === 0 && l.origen !== "DOMICILIO") {
            var itemDraftIdx = draftIdx++;
            var $item = $("<div></div>").addClass("item-comanda");

            var $m = $("<div></div>").addClass("item-comanda-maestro");
            $m.append("<span>" + l.cantidad + "x " + l.descripcion + "</span>");
            var $der = $("<span></span>").text("$ " + formatearPesos(l.valorTotal));
            var $del = $("<i class='fas fa-trash-alt btn-eliminar-item' title='Quitar producto'></i>").on("click", function () {
                eliminarItemDraft(itemDraftIdx);
            });
            $der.append($del);
            $m.append($der);
            $item.append($m);

            if (hijasMap[l.ref]) {
                $.each(hijasMap[l.ref], function (j, h) {
                    var isInc = (h.origen === "INCLUIDO");
                    var $h = $("<div></div>").addClass(isInc ? "item-comanda-incluido" : "item-comanda-hija");
                    var pref = isInc ? "[Inc] " : "• ";
                    $h.append("<span>" + pref + h.descripcion + "</span>");
                    $h.append("<span>" + (h.valorTotal > 0 ? "+$ " + formatearPesos(h.valorTotal) : "$ 0") + "</span>");
                    $item.append($h);
                });
            }

            $box.append($item);
        }
    });

    // Desglose de totales
    var domVal = 0;
    $.each(res.lineas, function (i, l) {
        if (l.origen === "DOMICILIO") domVal += l.valorTotal;
    });

    $("#subtotalTxt").text("$ " + formatearPesos(res.subtotal - domVal));
    $("#domicilioTxt").text("$ " + formatearPesos(domVal));
    $("#descuentoTxt").text("-$ " + formatearPesos(res.descuento));
    $("#totalTxt").text("$ " + formatearPesos(res.total));
}

function eliminarItemDraft(index) {
    if (index >= 0 && index < draftLineas.length) {
        draftLineas.splice(index, 1);
        cotizarEnVivo();
    }
}

function confirmarYDespacharPedido() {
    if (!resultadoMotor || draftLineas.length === 0) {
        alert("La comanda no tiene productos para despachar.");
        return;
    }

    if (!confirm("¿Deseas confirmar este pedido por un total de $ " + formatearPesos(resultadoMotor.total) + "?")) {
        return;
    }

    $("#btnConfirmarPedido").prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-1'></i>Despachando...");

    var idTienda = parseInt($("#tiendaSelect").val()) || 11;
    var idTipoPedido = parseInt($("#tipoPedidoSelect").val()) || 1;
    var idFormaPago = parseInt($("#formaPagoSelect").val()) || 1;

    var payload = {
        idTienda: idTienda,
        idCliente: clienteActual.idcliente,
        usuario: "PILOTO",
        idFormaPagoTienda: idFormaPago,
        valorFormaPago: resultadoMotor.total,
        tiempoPedido: 30,
        origen: "T",
        solicitud: {
            contexto: {
                idtienda: idTienda,
                canal: "T",
                idTipoPedido: idTipoPedido,
                fechaHora: new Date().toISOString().replace("T", " ").substring(0, 19)
            },
            cliente: {
                idcliente: clienteActual.idcliente,
                telefono: clienteActual.telefono,
                direccion: clienteActual.direccion
            },
            domicilio: {
                idproducto: (idTipoPedido === 1 ? 412 : 0)
            },
            lineas: draftLineas
        }
    };

    $.ajax({
        type: "POST",
        url: "api/motor/finalizar-pedido",
        contentType: "application/json;charset=UTF-8",
        data: JSON.stringify(payload),
        success: function (resp) {
            $("#btnConfirmarPedido").prop("disabled", false).html("<i class='fas fa-paper-plane mr-1'></i>Confirmar y Despachar a Tienda");

            if (resp.exito) {
                var msj = "¡Pedido registrado con exito en Central con id #" + resp.idPedido + "!";
                if (resp.despachoTienda && resp.despachoTienda.exito) {
                    msj += "\n\nSe despacho directamente al POS de la tienda (sin tabla de homologacion).\nComanda generada en tienda: #" + resp.despachoTienda.numeroFactura;
                } else if (resp.despachoTienda && !resp.despachoTienda.exito) {
                    msj += "\n\nAtencion en tienda: " + resp.despachoTienda.mensaje;
                }
                alert(msj);

                // Limpiar comanda
                draftLineas = [];
                cotizarEnVivo();
            } else {
                alert("Error al finalizar el pedido: " + (resp.mensaje || "Error desconocido"));
            }
        },
        error: function (xhr) {
            $("#btnConfirmarPedido").prop("disabled", false).html("<i class='fas fa-paper-plane mr-1'></i>Confirmar y Despachar a Tienda");
            alert("Error en el servidor al enviar pedido: " + xhr.responseText);
        }
    });
}

function formatearPesos(val) {
    if (!val || isNaN(val)) return "0";
    return Math.round(val).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

/**
 * Controlador de la Nueva Pantalla de Toma de Pedidos POS (Etapa 3 - Piloto Manrique).
 * Pizza Americana - Integracion directa con el Motor de Pedido Central.
 */

var catalogoMenu = null;
var productoEnEdicion = null;
var draftLineas = []; // [{ idproducto, cantidad, selecciones: [{idpregunta, mitad, idproductoOpcion, cantidad}], modificadores: [], observacion }]
var resultadoMotor = null;
var clienteUbicado = false;
var leafletMap = null;
var leafletMarker = null;
var coberturaTemporal = null;
var esAdministrador = false; // Rol verificado con ValidarUsuarioAplicacion (solo líderes y admin aplican descuentos manuales)
var tiempoEstimadoTienda = 30; // Tiempo promesa configurado en la tienda seleccionada
var idPedidoCentralActivo = 0; // Control de ID de pedido si se asigna previo al despacho definitivo

// Control de Tienda vs Cobertura de Georreferenciación
var tiendaCoberturaCliente = null; // { id: number, nombre: string }
var contingenciaTiendaForzada = false; // true si el asesor forzó el despacho a una tienda diferente a la cobertura
var tiendaPendienteConfirmacion = null; // { id: number, nombre: string } almacena temporalmente la tienda a la que se desea forzar por contingencia

// Marcaciones / Plataformas Externas (Rappi, DiDi, Bot, convenios)
var marcacionActiva = null; // { idmarcacion, nombremarcacion, observacion, marketplace, descuento, descuentoAsumido }
var catalogoMarcaciones = [];

// Formas de Pago Paramétricas (tabla pizzaamericana.forma_pago: virtual = 'S'/'N')
var catalogoFormasPago = [];

// Beneficio Precio Empleado (Colaboradores de Nómina con verificación OTP)
var empleadoPrecioEmpleadoValidado = null; // { id: number, cedula: string, nombre: string, email: string, emailEnmascarado: string, codigo: string }
var productoEmpleadoPendienteId = null;
var timerBusquedaEmpleado = null;
var empleadoSeleccionadoTemporal = null;

// Gestión de Redención y Reversa de Puntos Pizza
var idsRedencionPedido = [];
var puntosRedimidos = 0;

// Objeto de control de Descuentos & Cupones
var descuentoAplicado = {
    tipo: "", // "CUPON" o "MANUAL"
    codigoPromocional: "",
    descuentoManual: 0.0,
    motivoDescuento: "",
    justificacion: ""
};

var clienteActual = {
    idcliente: 0,
    memcode: 0,
    nombres: "Cliente Mostrador",
    apellidos: "",
    telefono: "3000000000",
    direccion: "Mostrador Directo",
    idmunicipio: 1,
    municipio: "Medellin",
    zona: "",
    email: "",
    observacion: "",
    latitud: 0.0,
    longitud: 0.0,
    distanciatienda: 0.0,
    nombreTienda: "",
    politicaDatos: "S",
    // Facturación Electrónica DIAN
    emailfact: "",
    clientesiniden: "S",
    idtipopersona: 2,
    identificacion: "222222222222"
};

$(document).ready(function () {
    $("#cargarMenu").load("Menu.html", function () {
        // Cargar menu superior si esta disponible
    });

    verificarRolUsuario();
    cargarTiendas();
    cargarMenuPOS();
    consultarTiempoTienda();
    cargarCatalogoMarcaciones();
    cargarFormasPagoParametricas();
    initValidacionPrecioEmpleado();
    initGestionPuntosPizza();

    // Eventos de Búsqueda de Cliente
    $("#btnBuscarTelefono").on("click", function () {
        ejecutarBusquedaCliente();
    });

    $("#telcelular").on("keypress", function (e) {
        if (e.which === 13) {
            e.preventDefault();
            ejecutarBusquedaCliente();
        }
    });

    $("#telcelular").on("change", function () {
        var tel = $(this).val().trim();
        if (tel.length >= 7) {
            ejecutarBusquedaCliente();
        }
    });

    // Facturación Electrónica DIAN
    $("#chkFacturaElectronica").on("change", function () {
        if ($(this).is(":checked")) {
            $("#seccionFacturaElectronica").slideDown(180);
            $("#badgeEstadoFactura").removeClass("badge-light text-muted").addClass("badge-primary text-white").html("<i class='fas fa-file-invoice-dollar mr-1'></i>Factura Electrónica (DIAN)");
            if (!$("#cliEmailFacturacion").val().trim() && $("#cliEmail").val().trim()) {
                $("#cliEmailFacturacion").val($("#cliEmail").val().trim());
            }
        } else {
            $("#seccionFacturaElectronica").slideUp(180);
            $("#badgeEstadoFactura").removeClass("badge-primary text-white").addClass("badge-light text-muted").html("<i class='fas fa-user-tag mr-1'></i>Consumidor Final (Sin Identificación)");
        }
        sincronizarDatosCliente();
    });

    $("#btnCopiarEmailCliente").on("click", function () {
        var em = $("#cliEmail").val().trim();
        if (em) {
            $("#cliEmailFacturacion").val(em);
            sincronizarDatosCliente();
        } else {
            alert("El correo principal de contacto aún no ha sido ingresado.");
            $("#cliEmail").focus();
        }
    });

    // Fidelización: Al digitar o modificar el correo electrónico
    $("#cliEmail").on("change blur", function () {
        var em = $(this).val().trim();
        consultarFidelizacionCliente(em);
    });

    // Eventos de Marcaciones / Plataformas Externas (Rappi, DiDi, Bot, convenios)
    $("#btnAbrirModalMarcacionPlataforma").on("click", function () {
        abrirModalMarcacionPlataforma();
    });

    $("#selectMarcacionPlataforma").on("change", function () {
        var idm = parseInt($(this).val()) || 0;
        if (idm > 0) {
            var sel = catalogoMarcaciones.find(function (m) { return m.idmarcacion === idm; });
            $("#grupoDetalleMarcacion").slideDown(150);
            if (sel && sel.marketplace === "S") {
                $("#chkMarketplaceMarcacion").prop("checked", true);
            } else {
                $("#chkMarketplaceMarcacion").prop("checked", false);
            }
        } else {
            $("#grupoDetalleMarcacion").slideUp(150);
        }
    });

    $("#btnGuardarMarcacionPlataforma").on("click", function () {
        guardarMarcacionPlataforma();
    });

    $("#btnRemoverMarcacion").on("click", function () {
        removerMarcacionPlataforma();
    });

    // Sincronizar inputs del cliente con el estado y la comanda
    $("#cliNombres, #cliApellidos, #cliDireccion, #cliZona, #cliMunicipio, #cliObservacion, #cliEmail, #cliTipoPersona, #cliIdentificacion, #cliEmailFacturacion").on("input change", function () {
        sincronizarDatosCliente();
    });

    // Eventos de Georreferenciación y Mapa Desacoplado
    $("#btnAbrirModalMapa, #btnOverlayAbrirMapa").on("click", function () {
        abrirModalMapa();
    });

    $("#btnEjecutarGeocodificacion").on("click", function () {
        ejecutarGeocodificacionMapa();
    });

    $("#mapaBusqDireccion").on("keypress", function (e) {
        if (e.which === 13) {
            e.preventDefault();
            ejecutarGeocodificacionMapa();
        }
    });

    $("#btnConfirmarUbicacionMapa").on("click", function () {
        confirmarUbicacionDesdeMapa();
    });

    // Eventos de Contexto
    $("#tipoPedidoSelect").on("change", function () {
        var tipo = parseInt($(this).val()) || 1;
        if (tipo !== 1) {
            // Mesa (2) o Llevar (3): Atencion presencial en tienda, el selector queda libre sin alertas de cobertura
            clienteUbicado = true;
            contingenciaTiendaForzada = false;
        } else {
            // Domicilio (1): Requiere ubicacion en mapa para cobertura y valida sincronizacion
            clienteUbicado = (clienteActual.latitud && Math.abs(clienteActual.latitud) > 0.001);
            if (tiendaCoberturaCliente && tiendaCoberturaCliente.id > 0) {
                var tiendaActualSel = parseInt($("#tiendaSelect").val()) || 0;
                if (tiendaActualSel > 0 && tiendaActualSel !== tiendaCoberturaCliente.id) {
                    contingenciaTiendaForzada = true;
                } else if (tiendaActualSel <= 0) {
                    $("#tiendaSelect").val(tiendaCoberturaCliente.id);
                    contingenciaTiendaForzada = false;
                    verificarTiendaPiloto();
                    consultarTiempoTienda(tiendaCoberturaCliente.id);
                }
            }
        }
        actualizarEstadoUbicacionUI();
        cotizarEnVivo();
    });

    $("#tiendaSelect").on("change", function () {
        var nuevoIdTienda = parseInt($(this).val()) || 0;
        var nuevaTiendaNom = $("#tiendaSelect option:selected").text().replace(/\[.*?\]/g, "").trim();
        var tipoPedido = parseInt($("#tipoPedidoSelect").val()) || 1;

        if (nuevoIdTienda <= 0) {
            contingenciaTiendaForzada = false;
            actualizarEstadoUbicacionUI();
            return;
        }

        // Control de Cobertura vs Despacho en Domicilios (Propuesta A):
        // Si el cliente tiene una tienda de cobertura asignada diferente a la seleccionada
        if (tipoPedido === 1 && tiendaCoberturaCliente && tiendaCoberturaCliente.id > 0
            && nuevoIdTienda !== tiendaCoberturaCliente.id) {

            var dirTxt = clienteActual.zona || clienteActual.direccion || "Prado Centro";
            var tiendaOficialNom = tiendaCoberturaCliente.nombre;

            // Poblar textos en el modal de advertencia de contingencia
            $("#lblDiscrepanciaDireccion").text(dirTxt);
            $("#lblDiscrepanciaTiendaOficialTxt").text(tiendaOficialNom);
            $("#lblDiscrepanciaTiendaOficial").text(tiendaOficialNom);
            $("#lblDiscrepanciaTiendaDestino").text(nuevaTiendaNom);
            $("#lblBtnMantenerTienda").text(tiendaOficialNom);
            $("#lblBtnRedirigirTienda").text(nuevaTiendaNom);

            tiendaPendienteConfirmacion = {
                id: nuevoIdTienda,
                nombre: nuevaTiendaNom
            };

            // Mostrar el modal de alerta inmediata
            $("#modalDiscrepanciaCobertura").modal("show");
            return;
        }

        contingenciaTiendaForzada = false;
        verificarTiendaPiloto();
        consultarTiempoTienda(nuevoIdTienda);
        actualizarEstadoUbicacionUI();
        cotizarEnVivo();
    });

    // Botones del Modal de Discrepancia de Cobertura
    $("#btnDiscrepanciaCancelar").on("click", function () {
        $("#modalDiscrepanciaCobertura").modal("hide");
        if (tiendaCoberturaCliente && tiendaCoberturaCliente.id > 0) {
            $("#tiendaSelect").val(tiendaCoberturaCliente.id);
            contingenciaTiendaForzada = false;
            tiendaPendienteConfirmacion = null;
            verificarTiendaPiloto();
            consultarTiempoTienda(tiendaCoberturaCliente.id);
            actualizarEstadoUbicacionUI();
            cotizarEnVivo();
        }
    });

    $("#btnDiscrepanciaConfirmar").on("click", function () {
        $("#modalDiscrepanciaCobertura").modal("hide");
        if (tiendaPendienteConfirmacion) {
            $("#tiendaSelect").val(tiendaPendienteConfirmacion.id);
            contingenciaTiendaForzada = true;
            verificarTiendaPiloto();
            consultarTiempoTienda(tiendaPendienteConfirmacion.id);
            actualizarEstadoUbicacionUI();
            cotizarEnVivo();
        }
    });

    // Eventos de Descuentos & Cupones
    $("#btnAplicarCupon").on("click", function () {
        aplicarCodigoCupon();
    });

    $("#txtCodigoCupon").on("keypress", function (e) {
        if (e.which === 13) {
            e.preventDefault();
            aplicarCodigoCupon();
        }
    });

    $("#btnAbrirModalDescuentoManual").on("click", function () {
        abrirModalDescuentoManual();
    });

    $("input[name='rdTipoDesc']").on("change", function () {
        var tipo = $("input[name='rdTipoDesc']:checked").val();
        if (tipo === "VALOR") {
            $("#addonSimboloDesc").text("$");
            $("#txtValorDescuentoManual").attr("placeholder", "Ej: 5000");
        } else {
            $("#addonSimboloDesc").text("%");
            $("#txtValorDescuentoManual").attr("placeholder", "Ej: 10");
        }
    });

    $("#btnGuardarDescuentoManual").on("click", function () {
        guardarDescuentoManualModal();
    });

    $("#btnRemoverDescuento").on("click", function () {
        removerDescuentoActivo();
    });

    // Evento Política de Datos (Habeas Data)
    $("#chkPoliticaDatos").on("change", function () {
        var marcado = $(this).is(":checked");
        var idFormaActual = parseInt($("#formaPagoSelect").val()) || 1;

        // Regla Paramétrica: Si la forma de pago activa es virtual (virtual = 'S'), la política es OBLIGATORIA
        if (!marcado && esFormaPagoVirtual(idFormaActual)) {
            $(this).prop("checked", true);
            clienteActual.politicaDatos = "S";
            alert(
                "⚠️ CONDICIONAL OBLIGATORIO: PAGO VIRTUAL\n\n" +
                "No es posible desmarcar la Política de Tratamiento de Datos mientras esté seleccionada una forma de Pago Virtual (se enviará enlace de pago vía WhatsApp / Correo).\n\n" +
                "Si el cliente no autoriza la política de tratamiento de datos, cambie primero la forma de pago a Efectivo o Datáfono."
            );
            return;
        }

        clienteActual.politicaDatos = marcado ? "S" : "N";
    });

    // Eventos de Pago y Cálculo de Devuelta
    $("#formaPagoSelect").on("change", function () {
        var idForma = parseInt($(this).val()) || 1;
        var esVirtual = esFormaPagoVirtual(idForma);

        // Regla Paramétrica: Si se selecciona pago virtual, exigir aceptación de política de datos y celular
        if (esVirtual) {
            var polAceptada = $("#chkPoliticaDatos").is(":checked") && (clienteActual.politicaDatos === "S");
            var telCelular = ($("#telcelular").val() || clienteActual.telefono || "").trim();

            if (!polAceptada) {
                var autoriza = confirm(
                    "⚠️ AUTORIZACIÓN POLÍTICA DE DATOS REQUERIDA\n\n" +
                    "La forma de pago seleccionada es VIRTUAL y requiere obligatoriamente que el cliente acepte la Política de Tratamiento de Datos (Habeas Data) para enviarle el link de pago por WhatsApp y correo electrónico.\n\n" +
                    "¿El cliente autoriza expresamente el tratamiento de sus datos personales?"
                );

                if (autoriza) {
                    $("#chkPoliticaDatos").prop("checked", true);
                    clienteActual.politicaDatos = "S";
                } else {
                    alert("CUIDADO: Si la forma de pago es virtual debe aceptar la política de manejo de datos. Se restablece la forma de pago a Efectivo.");
                    $(this).val("1");
                    ajustarFormaPagoUI();
                    return;
                }
            }

            if (!telCelular || telCelular.length < 7) {
                alert("¡Atención! Para formas de pago virtual debe registrar un número de teléfono celular válido para enviar el enlace de pago.");
                $("#telcelular").focus();
            }
        }

        ajustarFormaPagoUI();
    });

    $("input[name='radioModoPagoEfectivo']").on("change", function () {
        var modo = $(this).val();
        if (modo === "exacto") {
            if (resultadoMotor && resultadoMotor.total) {
                $("#txtValorPago").val(formatearPesos(resultadoMotor.total));
                $("#txtValorDevuelta").val("0");
            }
            calcularDevuelta("exacto");
        } else {
            $("#txtValorDevuelta").focus();
            calcularDevuelta("pago");
        }
    });

    $("#txtValorPago").on("input change", function () {
        calcularDevuelta("pago");
    });

    $("#txtValorDevuelta").on("input change", function () {
        calcularDevuelta("devuelta");
    });

    $(".btn-set-billete").on("click", function () {
        var monto = parseInt($(this).attr("data-monto")) || 0;
        $("#optPagoDevuelta").prop("checked", true);
        $("#lblModoPagoDevuelta").addClass("active").siblings().removeClass("active");
        $("#txtValorPago").val(formatearPesos(monto));
        calcularDevuelta("pago");
    });

    $(".btn-set-devuelta").on("click", function () {
        var dev = parseInt($(this).attr("data-dev")) || 0;
        $("#optPagoDevuelta").prop("checked", true);
        $("#lblModoPagoDevuelta").addClass("active").siblings().removeClass("active");
        $("#txtValorDevuelta").val(formatearPesos(dev));
        calcularDevuelta("devuelta");
    });

    $("#badgeSiguienteRedondeo").on("click", function () {
        if (resultadoMotor && resultadoMotor.total > 0) {
            var total = resultadoMotor.total;
            var redondeo = Math.ceil((total + 1) / 10000) * 10000;
            $("#optPagoDevuelta").prop("checked", true);
            $("#lblModoPagoDevuelta").addClass("active").siblings().removeClass("active");
            $("#txtValorPago").val(formatearPesos(redondeo));
            calcularDevuelta("pago");
        }
    });

    // Eventos de Pedido Programado
    $("#chkPedidoProgramado").on("change", function () {
        if ($(this).is(":checked")) {
            cargarHorariosProgramados();
            $("#wrapperHoraProgramada").fadeIn(150);
        } else {
            $("#wrapperHoraProgramada").fadeOut(150);
        }
        cotizarEnVivo();
    });

    $("#selectHoraProgramada").on("change", function () {
        cotizarEnVivo();
    });

    // Confirmación y Despacho
    $("#btnConfirmarPedido").on("click", function () {
        abrirModalConfirmacionPedido();
    });

    $("#btnDespacharPedidoDefinitivo").on("click", function () {
        ejecutarDespachoTiendaDefinitivo();
    });

    // Manual de Usuario
    $("#btnAbrirManualUsuario").on("click", function () {
        $("#modalManualUsuario").modal("show");
    });

    $("#btnAgregarProductoModal").on("click", function () {
        guardarProductoDesdeModal();
    });

    // Eventos de Reinicio de Pedido con Autenticación
    $("#btnReiniciarPedidoPOS, .btn-trigger-reiniciar-pedido").on("click", function () {
        solicitarReinicioPedidoPOS();
    });

    $("#btnConfirmarReinicioDefinitivo").on("click", function () {
        ejecutarReinicioPedidoPOS();
    });

    $("#formConfirmarReinicioPOS").on("submit", function (e) {
        e.preventDefault();
        ejecutarReinicioPedidoPOS();
    });

    // Estado inicial de ubicación y bloqueo de menú
    actualizarEstadoUbicacionUI();
    ajustarFormaPagoUI();
});

/* =========================================================================
   GESTIÓN DE CLIENTES (Muy de la mano de la pantalla actual)
   ========================================================================= */
function ejecutarBusquedaCliente() {
    var tel = $("#telcelular").val().trim();
    if (!tel || tel.length < 7) {
        return;
    }

    $("#btnBuscarTelefono").html("<i class='fas fa-spinner fa-spin'></i>");

    // Consultar alertas y marcas de seguridad / fraude asociadas a este teléfono
    consultarAlertaCliente(tel);

    $.ajax({
        type: "GET",
        url: "GetClienteTodos?telefono=" + encodeURIComponent(tel),
        dataType: "json",
        success: function (data) {
            $("#btnBuscarTelefono").html("<i class='fas fa-search'></i>");

            if (data && data.length > 0) {
                renderizarClientesEncontrados(data);
                // Autoseleccionar el primer cliente
                seleccionarClienteDeTabla(data[0]);
            } else {
                $("#contenedorTablaClientes").hide();
                // Cliente nuevo con este teléfono
                clienteActual.idcliente = 0;
                clienteActual.memcode = 0;
                clienteActual.telefono = tel;
                clienteActual.nombres = "";
                clienteActual.apellidos = "";
                clienteActual.direccion = "";
                clienteActual.latitud = 0.0;
                clienteActual.longitud = 0.0;
                clienteActual.nombreTienda = "";
                clienteUbicado = false;
                $("#cliNombres").val("").focus();
                $("#cliApellidos").val("");
                $("#cliDireccion").val("");
                actualizarEstadoUbicacionUI();
                actualizarResumenClienteComanda();
                consultarFidelizacionCliente("");
            }
        },
        error: function () {
            $("#btnBuscarTelefono").html("<i class='fas fa-search'></i>");
            $("#contenedorTablaClientes").hide();
        }
    });
}

function renderizarClientesEncontrados(lista) {
    var $tbody = $("#tbodyClientesEncontrados");
    $tbody.empty();

    if (lista.length > 1) {
        $("#contenedorTablaClientes").show();
    } else {
        $("#contenedorTablaClientes").hide();
    }

    $.each(lista, function (i, cli) {
        var $tr = $("<tr></tr>");
        $tr.append("<td>" + (cli.nombre || "") + " " + (cli.apellido || "") + "</td>");
        $tr.append("<td>" + (cli.direccion || "") + "</td>");
        $tr.append("<td>" + (cli.zona || "") + "</td>");
        $tr.append("<td>" + (cli.municipio || "Medellin") + "</td>");
        $tr.append("<td><span class='badge badge-info'>" + (cli.tienda || "General") + "</span></td>");

        var $btn = $("<button class='btn btn-xs btn-outline-primary py-0 px-2' type='button'>Elegir</button>");
        $btn.on("click", function (e) {
            e.stopPropagation();
            seleccionarClienteDeTabla(cli);
        });

        $tr.on("click", function () {
            seleccionarClienteDeTabla(cli);
        });

        $tr.append($("<td></td>").append($btn));
        $tbody.append($tr);
    });
}

function seleccionarClienteDeTabla(cli) {
    clienteActual.idcliente = cli.idCliente || 0;
    clienteActual.memcode = cli.memcode || 0;
    clienteActual.nombres = cli.nombre || "";
    clienteActual.apellidos = cli.apellido || "";
    clienteActual.telefono = cli.telefono || $("#telcelular").val().trim();
    clienteActual.direccion = cli.direccion || "";
    clienteActual.zona = cli.zona || "";
    clienteActual.municipio = cli.municipio || "Medellin";
    clienteActual.email = cli.email || "";
    clienteActual.observacion = cli.observacion || "";
    clienteActual.nombreTienda = cli.tienda || "";

    $("#cliNombres").val(clienteActual.nombres);
    $("#cliApellidos").val(clienteActual.apellidos);
    $("#cliDireccion").val(clienteActual.direccion);
    $("#cliZona").val(clienteActual.zona);
    $("#cliMunicipio").val(clienteActual.municipio);
    $("#cliEmail").val(clienteActual.email);
    $("#cliObservacion").val(clienteActual.observacion);
    $("#telcelular").val(clienteActual.telefono);

    // Consultar alertas de seguridad y estado en el plan de fidelización
    consultarAlertaCliente(clienteActual.telefono);
    consultarFidelizacionCliente(clienteActual.email);

    // Política de Tratamiento de Datos (Habeas Data)
    var tienePol = (cli.politicadatos === "S" || cli.politica_datos === "S" || cli.politicadatos === undefined);
    clienteActual.politicaDatos = tienePol ? "S" : "N";
    $("#chkPoliticaDatos").prop("checked", tienePol);

    // Si el cliente no autoriza política y actualmente está seleccionada una forma de pago virtual, revertir a Efectivo
    if (!tienePol && esFormaPagoVirtual()) {
        $("#formaPagoSelect").val("1");
        ajustarFormaPagoUI();
    }

    // Facturación Electrónica DIAN
    if (cli.identificacion && cli.identificacion !== "222222222222" && cli.identificacion !== "") {
        $("#chkFacturaElectronica").prop("checked", true).trigger("change");
        $("#cliIdentificacion").val(cli.identificacion);
        if (cli.idTipoPersona) $("#cliTipoPersona").val(cli.idTipoPersona);
        if (cli.emailFacturacion || cli.emailFact) $("#cliEmailFacturacion").val(cli.emailFacturacion || cli.emailFact);
    } else {
        $("#chkFacturaElectronica").prop("checked", false).trigger("change");
    }

    // Sincronización Automática de Tienda de Cobertura (Propuesta A)
    if (cli.tienda || cli.idtienda) {
        sincronizarTiendaConCobertura(cli.tienda, cli.idtienda);
    }

    // Verificar si ya cuenta con coordenadas GPS válidas
    var latVal = parseFloat(cli.latitud);
    var lngVal = parseFloat(cli.longitud);
    if (!isNaN(latVal) && !isNaN(lngVal) && Math.abs(latVal) > 0.001) {
        clienteActual.latitud = latVal;
        clienteActual.longitud = lngVal;
        clienteActual.distanciatienda = parseFloat(cli.distanciatienda || 0.0);
        clienteUbicado = true;
    } else {
        clienteActual.latitud = 0.0;
        clienteActual.longitud = 0.0;
        clienteUbicado = false;
    }

    actualizarEstadoUbicacionUI();
    actualizarResumenClienteComanda();
    cotizarEnVivo();
}

function establecerClienteGenerico() {
    clienteActual = {
        idcliente: 0,
        memcode: 0,
        nombres: "Cliente Mostrador",
        apellidos: "",
        telefono: "3000000000",
        direccion: "Mostrador Directo",
        idmunicipio: 1,
        municipio: "Medellin",
        zona: "Mostrador",
        email: "",
        observacion: "",
        latitud: 0.0,
        longitud: 0.0,
        distanciatienda: 0.0,
        nombreTienda: "Mostrador",
        politicaDatos: "S",
        emailfact: "",
        clientesiniden: "S",
        idtipopersona: 2,
        identificacion: "222222222222"
    };
    clienteUbicado = true;
    $("#chkPoliticaDatos").prop("checked", true);
    $("#telcelular").val("3000000000");
    $("#cliNombres").val("Cliente Mostrador");
    $("#cliApellidos").val("");
    $("#cliDireccion").val("Mostrador Directo");
    $("#cliZona").val("Mostrador");
    $("#cliMunicipio").val("Medellin");
    $("#cliEmail").val("");
    $("#cliObservacion").val("");
    $("#chkFacturaElectronica").prop("checked", false).trigger("change");
    $("#contenedorTablaClientes").hide();
    actualizarEstadoUbicacionUI();
    actualizarResumenClienteComanda();
    cotizarEnVivo();
}

/* =========================================================================
   GEORREFERENCIACIÓN Y COBERTURA EN MAPA (DESACOPLADO EN MODAL)
   ========================================================================= */
function actualizarEstadoUbicacionUI() {
    var tipo = parseInt($("#tipoPedidoSelect").val()) || 1;
    if (tipo !== 1) {
        // Pedido en Tienda (Mesa o Llevar): no requiere mapa
        $("#bloqueoMenuCliente").fadeOut(250);
        var tNom = idTiendaSeleccionada() ? $("#tiendaSelect option:selected").text() : "tienda sin seleccionar";
        var tipoStr = (tipo === 2 ? "Atención en Mesa" : "Para Llevar / Mostrador");
        $("#badgeEstadoUbicacion").removeClass("badge-ubicacion-pendiente").addClass("badge-ubicacion-ok").html(
            "<i class='fas fa-store mr-1 text-success'></i><span>" + tipoStr + " (Punto de Venta: <b>" + tNom + "</b>)</span>"
        );
        $("#btnAbrirModalMapa").removeClass("btn-pulsing-map btn-danger").addClass("btn-outline-secondary").html("<i class='fas fa-map-marker-alt mr-1'></i> Mapa Opcional");
        $("#infoCoordenadasCliente").hide();
        return;
    }

    if (clienteUbicado) {
        $("#bloqueoMenuCliente").fadeOut(250);
        var tNom = clienteActual.nombreTienda || (idTiendaSeleccionada() ? $("#tiendaSelect option:selected").text().trim() : "tienda sin seleccionar");
        var coordsTxt = (clienteActual.latitud && Math.abs(clienteActual.latitud) > 0.001) ? " (" + clienteActual.latitud.toFixed(4) + ", " + clienteActual.longitud.toFixed(4) + ")" : "";

        if (contingenciaTiendaForzada && tiendaCoberturaCliente) {
            var tDespacho = $("#tiendaSelect option:selected").text().replace(/\[.*?\]/g, "").trim();
            $("#badgeEstadoUbicacion").removeClass("badge-ubicacion-pendiente badge-ubicacion-ok").addClass("badge-warning text-dark font-weight-bold").html(
                "<i class='fas fa-exclamation-triangle mr-1 text-danger'></i><span>Ubicado en Mapa: <b>" + tiendaCoberturaCliente.nombre + "</b> ⚠️ (Redireccionado a: <b>" + tDespacho + "</b>)</span>"
            );
        } else {
            var tOficial = (tiendaCoberturaCliente && tiendaCoberturaCliente.nombre) ? tiendaCoberturaCliente.nombre : tNom;
            $("#badgeEstadoUbicacion").removeClass("badge-ubicacion-pendiente badge-warning font-weight-bold").addClass("badge-ubicacion-ok").html(
                "<i class='fas fa-check-circle mr-1 text-success'></i><span>Ubicado en Mapa: <b>" + tOficial + "</b>" + coordsTxt + "</span>"
            );
        }
        $("#btnAbrirModalMapa").removeClass("btn-pulsing-map btn-danger").addClass("btn-outline-primary").html("<i class='fas fa-map-marker-alt mr-1'></i> Reubicar / Ver Mapa");
        if (clienteActual.latitud && Math.abs(clienteActual.latitud) > 0.001) {
            $("#infoCoordenadasCliente").show();
            $("#txtCoordsClienteResumen").text("Lat: " + clienteActual.latitud.toFixed(6) + ", Lng: " + clienteActual.longitud.toFixed(6));
        } else {
            $("#infoCoordenadasCliente").hide();
        }
    } else {
        $("#bloqueoMenuCliente").fadeIn(250);
        $("#badgeEstadoUbicacion").removeClass("badge-ubicacion-ok").addClass("badge-ubicacion-pendiente").html(
            "<i class='fas fa-exclamation-triangle mr-1 text-warning'></i><span>Cliente no ubicado en mapa (Requerido para tomar pedido)</span>"
        );
        $("#btnAbrirModalMapa").removeClass("btn-outline-primary").addClass("btn-danger btn-pulsing-map").html("<i class='fas fa-map-marked-alt mr-1'></i> Ubicar en Mapa / Validar Cobertura");
        $("#infoCoordenadasCliente").hide();
    }
}

function abrirModalMapa() {
    var dir = $("#cliDireccion").val().trim();
    var mun = $("#cliMunicipio").val();
    var barrio = $("#cliZona").val().trim();

    $("#mapaBusqDireccion").val(dir);
    $("#mapaBusqMunicipio").val(mun || "Medellin");
    $("#mapaBusqBarrio").val(barrio);

    $("#modalMapaCliente").modal("show");
    inicializarMapaLeaflet();

    if (dir && dir.length >= 4) {
        ejecutarGeocodificacionMapa(dir, mun, barrio);
    }
}

function inicializarMapaLeaflet() {
    if (leafletMap !== null) {
        setTimeout(function () {
            leafletMap.invalidateSize();
        }, 300);
        return;
    }

    var centerLat = (clienteActual.latitud && Math.abs(clienteActual.latitud) > 0.001) ? clienteActual.latitud : 6.25184;
    var centerLng = (clienteActual.longitud && Math.abs(clienteActual.longitud) > 0.001) ? clienteActual.longitud : -75.56359;

    leafletMap = L.map('mapaLeaflet').setView([centerLat, centerLng], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(leafletMap);

    $('#modalMapaCliente').on('shown.bs.modal', function () {
        if (leafletMap) {
            leafletMap.invalidateSize();
        }
    });
}

function ejecutarGeocodificacionMapa(dir, mun, barrio, lat, lng) {
    dir = dir || $("#mapaBusqDireccion").val().trim();
    mun = mun || $("#mapaBusqMunicipio").val() || "Medellin";
    barrio = barrio || $("#mapaBusqBarrio").val().trim();

    if (!dir || dir.length < 3) {
        alert("Por favor ingrese una dirección para ubicar en el mapa.");
        $("#mapaBusqDireccion").focus();
        return;
    }

    $("#mapaLoaderOverlay").css("display", "flex");
    $("#btnConfirmarUbicacionMapa").prop("disabled", true);

    var reqPayload = {
        direccion: dir,
        municipio: mun,
        barrio: barrio,
        idcliente: clienteActual.idcliente || 0,
        canal: "POS"
    };
    if (lat && lng) {
        reqPayload.latitud = lat;
        reqPayload.longitud = lng;
    }

    $.ajax({
        type: "POST",
        url: "ValidacionCobertura",
        contentType: "application/json;charset=UTF-8",
        data: JSON.stringify(reqPayload),
        success: function (resp) {
            $("#mapaLoaderOverlay").hide();

            if (!resp || !resp.latitud || Math.abs(resp.latitud) < 0.001) {
                $("#panelResultadoCobertura").removeClass("alert-success alert-info").addClass("alert-warning").show();
                $("#txtResTiendaCobertura").html("<i class='fas fa-exclamation-triangle text-danger mr-1'></i>Dirección Fuera de Cobertura o No Encontrada");
                $("#badgeEstadoTienda").text("SIN COBERTURA").removeClass("badge-success").addClass("badge-danger");
                $("#txtResCoordenadas").text("Lat: 0.000000, Lng: 0.000000");
                $("#txtResDireccionEstandarizada").text("Verifique la nomenclatura o arrastre el marcador rojo en el mapa.");
                $("#txtResMensajeServidor").text(resp ? (resp.resultado || "") : "");
                return;
            }

            coberturaTemporal = resp;
            var cLat = resp.latitud;
            var cLng = resp.longitud;

            inicializarMapaLeaflet();

            if (leafletMarker) {
                leafletMarker.setLatLng([cLat, cLng]);
            } else {
                leafletMarker = L.marker([cLat, cLng], { draggable: true }).addTo(leafletMap);
                leafletMarker.on("dragend", function () {
                    var newPos = leafletMarker.getLatLng();
                    $("#txtCoordsEnVivo").text("Ajustado a Lat: " + newPos.lat.toFixed(6) + ", Lng: " + newPos.lng.toFixed(6));
                    ejecutarGeocodificacionMapa(dir, mun, barrio, newPos.lat, newPos.lng);
                });
            }

            leafletMap.setView([cLat, cLng], 16);
            setTimeout(function () {
                if (leafletMap) leafletMap.invalidateSize();
            }, 250);

            $("#panelResultadoCobertura").removeClass("alert-warning alert-info").addClass("alert-success").show();
            var tiendaNom = resp.infoAdicional || "Manrique";
            $("#txtResTiendaCobertura").html("<i class='fas fa-store mr-1 text-success'></i>Tienda Asignada: <b>" + tiendaNom + "</b>");
            var estadoT = resp.estadoTienda || "DISPONIBLE";
            $("#badgeEstadoTienda").text(estadoT).removeClass("badge-danger").addClass("badge-success");
            $("#txtResCoordenadas").text("Lat: " + cLat.toFixed(6) + ", Lng: " + cLng.toFixed(6));
            $("#txtResDireccionEstandarizada").text("Dirección estandarizada: " + (resp.direccionCorregida || resp.direccion || dir));
            $("#txtResMensajeServidor").text(resp.resultado || "");

            $("#btnConfirmarUbicacionMapa").prop("disabled", false);
        },
        error: function (xhr) {
            $("#mapaLoaderOverlay").hide();
            alert("Error consultando el servicio de cobertura: " + (xhr.responseText || "Error de red"));
        }
    });
}

function confirmarUbicacionDesdeMapa() {
    if (!coberturaTemporal || !coberturaTemporal.latitud) {
        alert("Debe validar la cobertura en el mapa antes de confirmar.");
        return;
    }

    clienteUbicado = true;
    clienteActual.latitud = coberturaTemporal.latitud;
    clienteActual.longitud = coberturaTemporal.longitud;
    clienteActual.direccion = coberturaTemporal.direccionCorregida || coberturaTemporal.direccion || $("#mapaBusqDireccion").val().trim();
    clienteActual.municipio = coberturaTemporal.municipioCorregido || $("#mapaBusqMunicipio").val();
    clienteActual.zona = coberturaTemporal.barrio || $("#mapaBusqBarrio").val().trim();
    clienteActual.nombreTienda = coberturaTemporal.infoAdicional || "";

    $("#cliDireccion").val(clienteActual.direccion);
    $("#cliMunicipio").val(clienteActual.municipio);
    $("#cliZona").val(clienteActual.zona);

    // Sincronización Automática de Tienda de Cobertura desde Mapa (Propuesta A)
    if (coberturaTemporal.infoAdicional || coberturaTemporal.idTienda) {
        sincronizarTiendaConCobertura(coberturaTemporal.infoAdicional, coberturaTemporal.idTienda);
    }

    actualizarResumenClienteComanda();
    actualizarEstadoUbicacionUI();

    $("#modalMapaCliente").modal("hide");

    if (draftLineas.length > 0) {
        cotizarEnVivo();
    }
}

function sincronizarDatosCliente() {
    clienteActual.nombres = $("#cliNombres").val().trim() || "Cliente Mostrador";
    clienteActual.apellidos = $("#cliApellidos").val().trim();
    clienteActual.telefono = $("#telcelular").val().trim() || "3000000000";
    clienteActual.direccion = $("#cliDireccion").val().trim() || "Mostrador Directo";
    clienteActual.zona = $("#cliZona").val().trim();
    clienteActual.municipio = $("#cliMunicipio").val();
    clienteActual.email = $("#cliEmail").val().trim();
    clienteActual.observacion = $("#cliObservacion").val().trim();

    // Facturación Electrónica DIAN
    if ($("#chkFacturaElectronica").is(":checked")) {
        clienteActual.clientesiniden = "N";
        clienteActual.idtipopersona = parseInt($("#cliTipoPersona").val()) || 2;
        clienteActual.identificacion = $("#cliIdentificacion").val().trim();
        clienteActual.emailfact = $("#cliEmailFacturacion").val().trim();
    } else {
        clienteActual.clientesiniden = "S";
        clienteActual.idtipopersona = 2;
        clienteActual.identificacion = "222222222222";
        clienteActual.emailfact = "";
    }

    actualizarResumenClienteComanda();
}

function actualizarResumenClienteComanda() {
    var nomCompleto = clienteActual.nombres + (clienteActual.apellidos ? " " + clienteActual.apellidos : "");
    $("#lblClienteComanda").text(nomCompleto);
    $("#lblTelComanda").html("<i class='fas fa-phone mr-1'></i>" + clienteActual.telefono);
    $("#lblDirComanda").html("<i class='fas fa-map-pin mr-1'></i>" + clienteActual.direccion + (clienteActual.zona ? " (" + clienteActual.zona + ")" : ""));
}

/* =========================================================================
   TIENDAS Y MENÚ POS
   ========================================================================= */
function cargarTiendas() {
    $.getJSON("api/motor/catalogo?accion=tiendas", function (data) {
        if (data && data.tiendas) {
            var $sel = $("#tiendaSelect");
            $sel.empty();
            // La tienda NO viene marcada: quien toma el pedido debe escogerla (o la
            // fija la busqueda del cliente). Asi un pedido no sale a la tienda de otro.
            $sel.append($("<option></option>").val("").text("-- Seleccione la tienda --"));
            $.each(data.tiendas, function (i, t) {
                var isPiloto = (t.idtienda === 11); // 11 = Manrique Piloto
                var label = t.nombre + (isPiloto ? " [PILOTO DIRECTO]" : "");
                $sel.append($("<option></option>").val(t.idtienda).text(label));
            });
            $sel.val("");
            verificarTiendaPiloto();
            consultarTiempoTienda(parseInt($("#tiendaSelect").val()));

            // Si el cliente ya estaba cargado con tienda de cobertura, sincronizarla
            if (clienteActual && (clienteActual.nombreTienda || tiendaCoberturaCliente)) {
                sincronizarTiendaConCobertura(clienteActual.nombreTienda || (tiendaCoberturaCliente ? tiendaCoberturaCliente.nombre : ""),
                                              tiendaCoberturaCliente ? tiendaCoberturaCliente.id : 0);
            }
        }
    });
}

function sincronizarTiendaConCobertura(nombreTienda, idTienda) {
    if (!nombreTienda && !idTienda) return false;
    var matchExacto = null;
    var matchParcial = null;
    var nomBusq = (nombreTienda || "").trim().toUpperCase();

    $("#tiendaSelect option").each(function () {
        var valId = parseInt($(this).val()) || 0;
        if (valId <= 0) return;
        var txtLimpio = $(this).text().replace(/\[.*?\]/g, "").trim().toUpperCase();

        if (idTienda && valId === parseInt(idTienda)) {
            matchExacto = $(this);
            return false;
        }
        if (nomBusq && txtLimpio === nomBusq) {
            matchExacto = $(this);
            return false;
        }
        if (nomBusq && (txtLimpio.indexOf(nomBusq) !== -1 || nomBusq.indexOf(txtLimpio) !== -1) && !matchParcial) {
            matchParcial = $(this);
        }
    });

    var $opt = matchExacto || matchParcial;
    if ($opt && $opt.length > 0) {
        var idSel = parseInt($opt.val()) || 0;
        var nombreLimpio = $opt.text().replace(/\[.*?\]/g, "").trim();

        tiendaCoberturaCliente = {
            id: idSel,
            nombre: nombreLimpio,
            textoOpcion: $opt.text().trim()
        };
        contingenciaTiendaForzada = false;
        $("#tiendaSelect").val(idSel);
        verificarTiendaPiloto();
        consultarTiempoTienda(idSel);
        actualizarEstadoUbicacionUI();
        return true;
    }
    return false;
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
    $("#pills-tabContent-pos").html("<div class='text-center p-4'><i class='fas fa-spinner fa-spin fa-2x text-danger'></i><p class='mt-2 text-muted'>Cargando catalogo unificado del POS...</p></div>");

    $.getJSON("api/motor/catalogo?accion=menu", function (data) {
        catalogoMenu = data;
        if (!data || !data.multimenus || data.multimenus.length === 0) {
            $("#pills-tabContent-pos").html("<div class='alert alert-warning p-3 m-3'>No se encontraron categorias de menu configuradas en pos_central.</div>");
            return;
        }

        $("#pills-tabContent-pos").empty();

        $.each(data.multimenus, function (idx, multi) {
            var tabId = "tab-multi-" + multi.idmultimenu;
            var isActive = (idx === 0) ? "active" : "";

            // Botón de pestaña
            var $navItem = $("<li></li>").addClass("nav-item");
            var $link = $("<a></a>")
                .addClass("nav-link " + isActive)
                .attr("id", "pill-" + tabId)
                .attr("data-toggle", "pill")
                .attr("href", "#" + tabId)
                .attr("role", "tab")
                .html("<i class='fas " + obtenerIconoMenu(multi.nombre) + " mr-1'></i>" + multi.nombre);
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
                            abrirModalProducto(item.idproducto, item.empleado === "S", item.precioPuntos || 0);
                        });

                    if (item.promocion && item.promocion === "S") {
                        $card.append("<span class='badge-promo'>PROMO</span>");
                    }
                    if (item.empleado && item.empleado === "S") {
                        $card.append("<span class='badge-promo' style='background: #0d6efd;'>🔒 EMPLEADO</span>");
                    }
                    if (item.precioPuntos && item.precioPuntos > 0) {
                        $card.append("<span class='badge-promo' style='background: #ffc107; color: #212529;'><i class='fas fa-gift mr-1'></i>" + Math.round(item.precioPuntos) + " PTS</span>");
                    }

                    var $title = $("<div></div>").addClass("prod-title").text(item.descripcion);
                    var $tipo = $("<div></div>").addClass("prod-tipo").text(item.tipo === "P" ? "Pizza" : (item.tipo === "B" ? "Bebida" : "Item"));
                    var precioTexto = (item.precioPuntos && item.precioPuntos > 0) ? (Math.round(item.precioPuntos) + " Puntos") : ("$ " + formatearPesos(item.precio));
                    var $precio = $("<div></div>").addClass("prod-precio").text(precioTexto);

                    $card.append($title).append($tipo).append($precio);
                    $grid.append($card);
                });
            } else {
                $grid.html("<p class='text-muted p-3'>Sin productos configurados en esta seccion.</p>");
            }

            $tabPane.append($grid);
            $("#pills-tabContent-pos").append($tabPane);
        });
    }).fail(function (xhr) {
        $("#pills-tabContent-pos").html("<div class='alert alert-danger m-3'>Error al cargar menu: " + xhr.responseText + "</div>");
    });
}

function obtenerIconoMenu(nombre) {
    if (!nombre) return "fa-utensils";
    var n = nombre.toLowerCase();
    if (n.indexOf("pizza") !== -1) return "fa-pizza-slice";
    if (n.indexOf("gaseosa") !== -1 || n.indexOf("bebida") !== -1) return "fa-wine-bottle";
    if (n.indexOf("descuento") !== -1 || n.indexOf("promo") !== -1) return "fa-tag";
    if (n.indexOf("combo") !== -1) return "fa-box-open";
    return "fa-concierge-bell";
}

/* =========================================================================
   CONFIGURADOR DE PRODUCTO (MODAL CON PREGUNTAS FORZADAS Y MITADES)
   ========================================================================= */
function abrirModalProducto(idProducto, esEmpleadoHint, precioPuntosHint) {
    if (!clienteUbicado) {
        alert("¡Atención! Es obligatorio ingresar los datos del cliente y ubicar su dirección en el mapa antes de tomar el pedido.");
        abrirModalMapa();
        return;
    }

    // Intercepción preventiva inmediata sin abrir el modal de personalización
    if (esEmpleadoHint) {
        var yaTiene = draftLineas.some(function (l) { return l.esEmpleado; });
        if (yaTiene) {
            alert("⚠️ Límite del beneficio: Solo se permite una (1) pizza a Precio Empleado por pedido.");
            return;
        }
        if (!empleadoPrecioEmpleadoValidado) {
            productoEmpleadoPendienteId = idProducto;
            abrirModalValidacionPrecioEmpleado();
            return;
        }
    }

    $.getJSON("api/motor/catalogo?accion=producto&id=" + idProducto, function (prod) {
        prod.precioPuntos = prod.precioPuntos || precioPuntosHint || 0;

        // Intercepción: Beneficio de Precio Empleado
        if (prod.empleado === "S") {
            var yaTieneEmpleadoEnComanda = draftLineas.some(function (l) { return l.esEmpleado; });
            if (yaTieneEmpleadoEnComanda) {
                alert("⚠️ Límite del beneficio: Solo se permite una (1) pizza a Precio Empleado por pedido.");
                return;
            }

            if (!empleadoPrecioEmpleadoValidado) {
                productoEmpleadoPendienteId = idProducto;
                abrirModalValidacionPrecioEmpleado();
                return;
            }
        }

        productoEnEdicion = prod;
        renderizarModalProducto(prod);
        $("#modalProductoConfig").modal("show");
    }).fail(function (xhr) {
        alert("Error al cargar opciones del producto: " + (xhr.responseText || xhr.statusText));
    });
}

function renderizarModalProducto(prod) {
    var $c = $("<div></div>");

    var $cab = $("<div></div>").addClass("mb-2 d-flex justify-content-between align-items-center p-2 rounded bg-light border");
    $cab.append("<div><h5 class='mb-0 font-weight-bold text-dark'>" + prod.descripcion + "</h5><small class='text-muted'>" + (prod.tipo === "P" ? "Pizza" : "Producto") + "</small></div>");
    var precTxt = (prod.precioPuntos && prod.precioPuntos > 0) ? (Math.round(prod.precioPuntos) + " Pts") : ("$ " + formatearPesos(prod.precio1));
    $cab.append("<div class='h4 text-danger font-weight-bold mb-0'>" + precTxt + "</div>");
    $c.append($cab);

    // Banner de Beneficio Precio Empleado si aplica
    if (prod.empleado === "S") {
        var empNom = empleadoPrecioEmpleadoValidado ? empleadoPrecioEmpleadoValidado.nombre : "Colaborador de Nómina";
        var empCod = empleadoPrecioEmpleadoValidado ? empleadoPrecioEmpleadoValidado.codigo : "";
        $c.append("<div class='alert alert-primary py-1 px-3 mb-2' style='font-size:0.83rem;'><i class='fas fa-user-shield mr-1'></i> <b>Beneficio Precio Empleado Autorizado:</b> " + empNom + " (Cód: " + empCod + ") - Máx. 1 unidad</div>");
    }

    // Banner de Redención Puntos Pizza si aplica
    if (prod.precioPuntos && prod.precioPuntos > 0) {
        $c.append("<div class='alert alert-warning py-1 px-3 mb-2' style='font-size:0.83rem;'><i class='fas fa-gift mr-1'></i> <b>Producto de Fidelización:</b> Requiere <b>" + Math.round(prod.precioPuntos) + " Puntos Pizza</b> para redimir.</div>");
    }

    // Productos incluidos automáticos ($0)
    if (prod.incluidos && prod.incluidos.length > 0) {
        var $incBox = $("<div></div>").addClass("alert alert-success py-1 px-3 mb-2");
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
            var tituloPreg = preg.titulo || preg.descripcion || ("Pregunta " + (idx + 1));
            var descPreg = (preg.descripcion && preg.descripcion !== preg.titulo) ? " - " + preg.descripcion : "";
            var minMaxTxt = (preg.minimo > 0 || preg.maximo > 1) ? " (Min: " + preg.minimo + " / Max: " + preg.maximo + ")" : "";

            $pBox.append("<div class='pregunta-titulo'><i class='fas fa-question-circle text-primary mr-1'></i>" + tituloPreg + "<small class='text-muted ml-1'>" + descPreg + minMaxTxt + "</small></div>");

            // Caso A: Pregunta que permite dividir en dos (Mitad y Mitad para Pizzas)
            if (preg.permiteDividir >= 2 && preg.maximo === 1) {
                $pBox.attr("data-permite-dividir", "2");

                var idPreg = preg.idpregunta;
                var $switchModo = $("<div class='selector-modo-mitad btn-group btn-group-toggle w-100 mb-2' data-toggle='buttons'></div>");
                var $btnEntera = $("<label class='btn btn-outline-danger btn-sm active' style='cursor:pointer;'><input type='radio' name='modo_preg_" + idPreg + "' value='entera' checked> <i class='fas fa-pizza-slice mr-1'></i> 1 Sabor Completo (100%)</label>");
                var $btnMitad = $("<label class='btn btn-outline-danger btn-sm' style='cursor:pointer;'><input type='radio' name='modo_preg_" + idPreg + "' value='mitad'> <i class='fas fa-adjust mr-1'></i> Mitad y Mitad (50% / 50%)</label>");
                $switchModo.append($btnEntera).append($btnMitad);
                $pBox.append($switchModo);

                // Contenedor 1: Entera (1 Sabor)
                var $boxEntera = $("<div></div>").addClass("box-entera").attr("id", "box-entera-" + idPreg);
                var $gridEntera = $("<div></div>").addClass("opciones-grid");
                $.each(preg.opciones, function (j, op) {
                    var $btn = $("<div></div>")
                        .addClass("opcion-btn")
                        .attr("data-idopcion", op.idproducto)
                        .attr("data-mitad", "0")
                        .html("<span>" + op.descripcion + "</span>" + (op.precio > 0 ? "<span class='opcion-precio text-danger'>+$ " + formatearPesos(op.precio) + "</span>" : ""));
                    $btn.on("click", function () {
                        $gridEntera.find(".opcion-btn").removeClass("selected");
                        $(this).addClass("selected");
                    });
                    if (preg.obligaEleccion === 1 && j === 0) {
                        $btn.addClass("selected");
                    }
                    $gridEntera.append($btn);
                });
                $boxEntera.append($gridEntera);
                $pBox.append($boxEntera);

                // Contenedor 2: Mitades (Mitad 1 y Mitad 2)
                var $boxMitades = $("<div></div>").addClass("contenedor-mitades").attr("id", "box-mitades-" + idPreg).hide();

                // Columna Mitad 1
                var $colM1 = $("<div></div>").addClass("col-mitad");
                var $titM1 = $("<div class='col-mitad-tit'></div>");
                $titM1.append("<span><i class='fas fa-adjust text-danger mr-1'></i> Mitad 1</span>");
                var $btnClonarM1 = $("<button type='button' class='btn btn-xs btn-outline-warning ml-auto py-0 px-2' style='font-size:0.75rem; font-weight:700;' title='Copiar sabor a Mitad 2 (Toda la Pizza)'><i class='fas fa-clone mr-1'></i>Toda la Pizza</button>");
                $titM1.append($btnClonarM1);
                $titM1.append("<span class='badge badge-light border ml-1'>50%</span>");
                $colM1.append($titM1);

                var $gridM1 = $("<div></div>").addClass("opciones-grid");
                var $gridM2 = $("<div></div>").addClass("opciones-grid");

                function clonarM1aM2() {
                    var $selM1 = $gridM1.find(".opcion-btn.selected");
                    if ($selM1.length > 0) {
                        var idSel = $selM1.attr("data-idopcion");
                        $gridM2.find(".opcion-btn").removeClass("selected");
                        var $m2Target = $gridM2.find(".opcion-btn[data-idopcion='" + idSel + "']");
                        if ($m2Target.length > 0) {
                            $m2Target.addClass("selected");
                            $btnClonarM1.removeClass("btn-outline-warning").addClass("btn-success").html("<i class='fas fa-check mr-1'></i>Copiado a Mitad 2");
                            setTimeout(function () {
                                $btnClonarM1.removeClass("btn-success").addClass("btn-outline-warning").html("<i class='fas fa-clone mr-1'></i>Toda la Pizza");
                            }, 1800);
                        }
                    } else {
                        var $primero = $gridM1.find(".opcion-btn").first();
                        if ($primero.length > 0) {
                            $primero.addClass("selected");
                            clonarM1aM2();
                        }
                    }
                }

                $btnClonarM1.on("click", function (e) {
                    e.preventDefault();
                    clonarM1aM2();
                });

                $.each(preg.opciones, function (j, op) {
                    var $btn = $("<div></div>")
                        .addClass("opcion-btn")
                        .attr("data-idopcion", op.idproducto)
                        .attr("data-mitad", "1")
                        .html("<span>" + op.descripcion + "</span>" + (op.precio > 0 ? "<span class='opcion-precio text-danger'>+$ " + formatearPesos(op.precio / 2) + "</span>" : ""));
                    $btn.on("click", function () {
                        $gridM1.find(".opcion-btn").removeClass("selected");
                        $(this).addClass("selected");
                    });
                    $btn.on("dblclick", function () {
                        $gridM1.find(".opcion-btn").removeClass("selected");
                        $(this).addClass("selected");
                        clonarM1aM2();
                    });
                    if (preg.obligaEleccion === 1 && j === 0) {
                        $btn.addClass("selected");
                    }
                    $gridM1.append($btn);
                });
                $colM1.append($gridM1);

                // Columna Mitad 2
                var $colM2 = $("<div></div>").addClass("col-mitad");
                $colM2.append("<div class='col-mitad-tit'><span><i class='fas fa-adjust text-primary mr-1'></i> Mitad 2</span><span class='badge badge-light border'>50%</span></div>");
                $.each(preg.opciones, function (j, op) {
                    var $btn = $("<div></div>")
                        .addClass("opcion-btn")
                        .attr("data-idopcion", op.idproducto)
                        .attr("data-mitad", "2")
                        .html("<span>" + op.descripcion + "</span>" + (op.precio > 0 ? "<span class='opcion-precio text-danger'>+$ " + formatearPesos(op.precio / 2) + "</span>" : ""));
                    $btn.on("click", function () {
                        $gridM2.find(".opcion-btn").removeClass("selected");
                        $(this).addClass("selected");
                    });
                    if (preg.obligaEleccion === 1 && j === 1) {
                        $btn.addClass("selected");
                    } else if (preg.obligaEleccion === 1 && preg.opciones.length === 1 && j === 0) {
                        $btn.addClass("selected");
                    }
                    $gridM2.append($btn);
                });
                $colM2.append($gridM2);

                $boxMitades.append($colM1).append($colM2);
                $pBox.append($boxMitades);

                // Toggle click con sincronizacion bidireccional
                $btnEntera.on("click", function () {
                    $boxEntera.show();
                    $boxMitades.hide();
                    $pBox.attr("data-modo-activo", "entera");
                    var $selM1 = $gridM1.find(".opcion-btn.selected");
                    if ($selM1.length > 0) {
                        var idM1 = $selM1.attr("data-idopcion");
                        $gridEntera.find(".opcion-btn").removeClass("selected");
                        $gridEntera.find(".opcion-btn[data-idopcion='" + idM1 + "']").addClass("selected");
                    }
                });
                $btnMitad.on("click", function () {
                    $boxEntera.hide();
                    $boxMitades.show();
                    $pBox.attr("data-modo-activo", "mitad");
                    var $selEnt = $gridEntera.find(".opcion-btn.selected");
                    if ($selEnt.length > 0) {
                        var idEnt = $selEnt.attr("data-idopcion");
                        $gridM1.find(".opcion-btn").removeClass("selected");
                        $gridM1.find(".opcion-btn[data-idopcion='" + idEnt + "']").addClass("selected");
                    }
                });
                $pBox.attr("data-modo-activo", "entera");

            } else if (preg.permiteDividir >= 2 && (preg.maximo > 1 || preg.maximo === 0)) {
                // Caso B: Adiciones divisibles de Pizza (Permite elegir Toda la Pizza, Mitad 1 o Mitad 2)
                $pBox.attr("data-permite-dividir", "2");
                $pBox.attr("data-es-adicion-divisible", "1");

                var $adicionesGrid = $("<div></div>").addClass("adiciones-divisibles-grid");

                $.each(preg.opciones, function (j, op) {
                    var $card = $("<div></div>")
                        .addClass("opcion-adicion-card")
                        .attr("data-idopcion", op.idproducto)
                        .attr("data-idpregunta", preg.idpregunta)
                        .attr("data-precio-base", op.precio)
                        .attr("data-porcion", "none");

                    var $head = $("<div class='adicion-card-head d-flex justify-content-between align-items-center'></div>");
                    $head.append("<span class='adicion-titulo font-weight-bold text-dark'>" + op.descripcion + "</span>");
                    var $badgePrecio = $("<span class='opcion-precio text-danger font-weight-bold'>+$ " + formatearPesos(op.precio) + "</span>");
                    $head.append($badgePrecio);
                    $card.append($head);

                    var $btnGroup = $("<div class='btn-group btn-group-toggle w-100 mt-1 adicion-portion-btns'></div>");
                    var $btnToda = $("<button type='button' class='btn btn-xs btn-outline-secondary py-0 flex-fill' data-val='entera' title='Adición en toda la pizza (100%)'>Toda (1.0)</button>");
                    var $btnM1 = $("<button type='button' class='btn btn-xs btn-outline-danger py-0 flex-fill' data-val='m1' title='Adición solo en Mitad 1 (50%)'>Mitad 1 (0.5)</button>");
                    var $btnM2 = $("<button type='button' class='btn btn-xs btn-outline-primary py-0 flex-fill' data-val='m2' title='Adición solo en Mitad 2 (50%)'>Mitad 2 (0.5)</button>");

                    $btnGroup.append($btnToda).append($btnM1).append($btnM2);
                    $card.append($btnGroup);

                    function actualizarEstadoCard(modo) {
                        $card.attr("data-porcion", modo);
                        $btnToda.removeClass("active btn-dark").addClass("btn-outline-secondary");
                        $btnM1.removeClass("active btn-danger").addClass("btn-outline-danger");
                        $btnM2.removeClass("active btn-primary").addClass("btn-outline-primary");
                        $card.removeClass("selected-entera selected-m1 selected-m2");

                        if (modo === "entera") {
                            $btnToda.removeClass("btn-outline-secondary").addClass("active btn-dark font-weight-bold");
                            $card.addClass("selected-entera");
                            $badgePrecio.html("+$ " + formatearPesos(op.precio) + " <small class='badge badge-dark'>Toda (100%)</small>");
                        } else if (modo === "m1") {
                            $btnM1.removeClass("btn-outline-danger").addClass("active btn-danger font-weight-bold");
                            $card.addClass("selected-m1");
                            $badgePrecio.html("+$ " + formatearPesos(op.precio / 2) + " <small class='badge badge-danger'>Mitad 1 (50%)</small>");
                        } else if (modo === "m2") {
                            $btnM2.removeClass("btn-outline-primary").addClass("active btn-primary font-weight-bold");
                            $card.addClass("selected-m2");
                            $badgePrecio.html("+$ " + formatearPesos(op.precio / 2) + " <small class='badge badge-primary'>Mitad 2 (50%)</small>");
                        } else {
                            $badgePrecio.html("+$ " + formatearPesos(op.precio));
                        }
                    }

                    $btnToda.on("click", function (e) {
                        e.stopPropagation();
                        if ($card.attr("data-porcion") === "entera") {
                            actualizarEstadoCard("none");
                        } else {
                            actualizarEstadoCard("entera");
                        }
                    });

                    $btnM1.on("click", function (e) {
                        e.stopPropagation();
                        if ($card.attr("data-porcion") === "m1") {
                            actualizarEstadoCard("none");
                        } else {
                            actualizarEstadoCard("m1");
                        }
                    });

                    $btnM2.on("click", function (e) {
                        e.stopPropagation();
                        if ($card.attr("data-porcion") === "m2") {
                            actualizarEstadoCard("none");
                        } else {
                            actualizarEstadoCard("m2");
                        }
                    });

                    $card.on("click", function (e) {
                        if ($(e.target).closest(".adicion-portion-btns").length > 0) return;
                        if ($card.attr("data-porcion") === "entera") {
                            actualizarEstadoCard("none");
                        } else {
                            actualizarEstadoCard("entera");
                        }
                    });

                    $adicionesGrid.append($card);
                });

                $pBox.append($adicionesGrid);
            } else {
                // Caso C: Pregunta estándar no divisible (Gaseosas, Bebidas, Acompañantes o Salsas)
                $pBox.attr("data-permite-dividir", "0");
                var $opGrid = $("<div></div>").addClass("opciones-grid");
                var esRadio = (preg.maximo === 1);

                $.each(preg.opciones, function (j, op) {
                    var $btn = $("<div></div>")
                        .addClass("opcion-btn")
                        .attr("data-idopcion", op.idproducto)
                        .attr("data-idpregunta", preg.idpregunta)
                        .attr("data-mitad", "0")
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
                                    alert("Solo puedes elegir maximo " + preg.maximo + " opciones para: " + tituloPreg);
                                    return;
                                }
                                $(this).addClass("selected");
                            }
                        }
                    });

                    // Preseleccionar primera opción si es radio obligatorio
                    if (esRadio && preg.obligaEleccion === 1 && j === 0) {
                        $btn.addClass("selected");
                    }

                    $opGrid.append($btn);
                });

                $pBox.append($opGrid);
            }

            $c.append($pBox);
        });
    }

    // Selector de cantidad
    var $cantBox = $("<div></div>").addClass("mt-2 d-flex align-items-center justify-content-between p-2 bg-light rounded border");
    $cantBox.append("<span class='font-weight-bold'>Cantidad:</span>");
    var $cantControl = $("<div class='d-flex align-items-center' style='gap:6px;'>"
        + "<button class='btn btn-sm btn-outline-secondary' type='button' id='btnMenosModal'><i class='fas fa-minus'></i></button>"
        + "<input type='number' id='cantModal' value='1' min='1' max='50' class='form-control form-control-sm text-center font-weight-bold' style='width:55px;' readonly>"
        + "<button class='btn btn-sm btn-outline-secondary' type='button' id='btnMasModal'><i class='fas fa-plus'></i></button>"
        + "</div>");
    $cantBox.append($cantControl);
    $c.append($cantBox);

    $("#modalProductoBody").html($c);

    $("#btnMenosModal").on("click", function () {
        var v = parseInt($("#cantModal").val());
        if (v > 1) { $("#cantModal").val(v - 1); }
    });
    $("#btnMasModal").on("click", function () {
        var v = parseInt($("#cantModal").val());
        if (v < 50) { $("#cantModal").val(v + 1); }
    });

    if (prod.empleado === "S") {
        $("#btnMenosModal, #btnMasModal").prop("disabled", true);
        $("#cantModal").val(1).prop("disabled", true);
    }
}

function guardarProductoDesdeModal() {
    if (!productoEnEdicion) return;

    var selecciones = [];
    var errorValidacion = null;

    if (productoEnEdicion.preguntas) {
        $.each(productoEnEdicion.preguntas, function (i, preg) {
            if (errorValidacion) return false;

            var $pBox = $(".pregunta-seccion[data-idpregunta='" + preg.idpregunta + "']");
            var permiteDividir = (preg.permiteDividir >= 2 && preg.maximo === 1);
            var tituloPreg = preg.titulo || preg.descripcion || ("Pregunta " + (i + 1));

            if (permiteDividir) {
                var modo = $pBox.attr("data-modo-activo") || "entera";
                if (modo === "entera") {
                    var $selEntera = $pBox.find("#box-entera-" + preg.idpregunta + " .opcion-btn.selected");
                    if (preg.obligaEleccion === 1 && $selEntera.length === 0) {
                        errorValidacion = "Debes seleccionar un sabor completo para: " + tituloPreg;
                        return false;
                    }
                    $selEntera.each(function () {
                        selecciones.push({
                            idpregunta: preg.idpregunta,
                            mitad: 0,
                            idproductoOpcion: parseInt($(this).attr("data-idopcion")),
                            cantidad: 1.0
                        });
                    });
                } else {
                    // Modo mitad y mitad
                    var $selM1 = $pBox.find("#box-mitades-" + preg.idpregunta + " .col-mitad:first .opcion-btn.selected");
                    var $selM2 = $pBox.find("#box-mitades-" + preg.idpregunta + " .col-mitad:last .opcion-btn.selected");

                    if (preg.obligaEleccion === 1) {
                        if ($selM1.length === 0) {
                            errorValidacion = "Debes seleccionar el sabor de la Mitad 1 para: " + tituloPreg;
                            return false;
                        }
                        if ($selM2.length === 0) {
                            errorValidacion = "Debes seleccionar el sabor de la Mitad 2 para: " + tituloPreg;
                            return false;
                        }
                    }

                    if ($selM1.length > 0) {
                        selecciones.push({
                            idpregunta: preg.idpregunta,
                            mitad: 1,
                            idproductoOpcion: parseInt($selM1.attr("data-idopcion")),
                            cantidad: 0.5
                        });
                    }
                    if ($selM2.length > 0) {
                        selecciones.push({
                            idpregunta: preg.idpregunta,
                            mitad: 2,
                            idproductoOpcion: parseInt($selM2.attr("data-idopcion")),
                            cantidad: 0.5
                        });
                    }
                }
            } else if (preg.permiteDividir >= 2 && (preg.maximo > 1 || preg.maximo === 0)) {
                // Adiciones divisibles de Pizza (Entera 100%, Mitad 1 50% o Mitad 2 50%)
                var $adCards = $pBox.find(".opcion-adicion-card");
                var totalSelAd = 0;
                $adCards.each(function () {
                    var porcion = $(this).attr("data-porcion");
                    var idOpcion = parseInt($(this).attr("data-idopcion"));
                    var opDesc = $(this).find(".adicion-titulo").text().trim();
                    if (porcion === "entera") {
                        selecciones.push({
                            idpregunta: preg.idpregunta,
                            mitad: 0,
                            idproductoOpcion: idOpcion,
                            cantidad: 1.0,
                            esAdicion: true,
                            descripcion: opDesc,
                            porcion: "entera"
                        });
                        totalSelAd++;
                    } else if (porcion === "m1") {
                        selecciones.push({
                            idpregunta: preg.idpregunta,
                            mitad: 1,
                            idproductoOpcion: idOpcion,
                            cantidad: 0.5,
                            esAdicion: true,
                            descripcion: opDesc,
                            porcion: "m1"
                        });
                        totalSelAd++;
                    } else if (porcion === "m2") {
                        selecciones.push({
                            idpregunta: preg.idpregunta,
                            mitad: 2,
                            idproductoOpcion: idOpcion,
                            cantidad: 0.5,
                            esAdicion: true,
                            descripcion: opDesc,
                            porcion: "m2"
                        });
                        totalSelAd++;
                    }
                });

                if (preg.obligaEleccion === 1 && preg.minimo > 0 && totalSelAd < preg.minimo) {
                    errorValidacion = "Debes seleccionar al menos " + preg.minimo + " adicion(es) para: " + tituloPreg;
                    return false;
                }
            } else {
                // Pregunta regular o adiciones no divisibles
                var $selReg = $pBox.find(".opciones-grid .opcion-btn.selected");
                if (preg.obligaEleccion === 1 && preg.minimo > 0 && $selReg.length < preg.minimo) {
                    errorValidacion = "Debes seleccionar al menos " + preg.minimo + " opcion(es) para: " + tituloPreg;
                    return false;
                }
                $selReg.each(function () {
                    selecciones.push({
                        idpregunta: preg.idpregunta,
                        mitad: 0,
                        idproductoOpcion: parseInt($(this).attr("data-idopcion")),
                        cantidad: 1.0
                    });
                });
            }
        });
    }

    if (errorValidacion) {
        alert(errorValidacion);
        return;
    }

    var cant = parseInt($("#cantModal").val()) || 1;
    var esEmp = (productoEnEdicion.empleado === "S");
    if (esEmp && cant > 1) {
        cant = 1;
    }

    draftLineas.push({
        idproducto: productoEnEdicion.idproducto,
        cantidad: cant,
        selecciones: selecciones,
        modificadores: [],
        observacion: (esEmp && empleadoPrecioEmpleadoValidado)
            ? ("[PRECIO EMPLEADO: " + empleadoPrecioEmpleadoValidado.nombre + " (Cód: " + empleadoPrecioEmpleadoValidado.codigo + ")]")
            : "",
        esEmpleado: esEmp,
        precioPuntos: (productoEnEdicion && productoEnEdicion.precioPuntos) ? productoEnEdicion.precioPuntos : 0
    });

    actualizarBadgePrecioEmpleado();

    $("#modalProductoConfig").modal("hide");
    productoEnEdicion = null;

    cotizarEnVivo();
}

/* =========================================================================
   GESTIÓN DE DESCUENTOS Y CUPONES
   ========================================================================= */
function aplicarCodigoCupon() {
    var cupon = $("#txtCodigoCupon").val().trim().toUpperCase();
    if (!cupon) {
        alert("Por favor ingrese un código de cupón o promoción.");
        $("#txtCodigoCupon").focus();
        return;
    }

    $("#btnAplicarCupon").prop("disabled", true).html("<i class='fas fa-spinner fa-spin'></i>");

    $.ajax({
        url: "GetValidarCodigoPromocional?codigopromocional=" + encodeURIComponent(cupon) + "&canal=C",
        dataType: "json",
        type: "GET",
        success: function (data) {
            $("#btnAplicarCupon").prop("disabled", false).html("<i class='fas fa-check'></i> Aplicar");
            if (data && data.respuesta === "OK") {
                if (data.utilizado === "S") {
                    alert("El código promocional '" + cupon + "' YA FUE UTILIZADO.");
                    return;
                }
                descuentoAplicado.tipo = "CUPON";
                descuentoAplicado.codigoPromocional = cupon;
                descuentoAplicado.descuentoManual = 0.0;
                descuentoAplicado.motivoDescuento = "CUPÓN: " + cupon + (data.observacion ? (" - " + data.observacion) : "");
                descuentoAplicado.justificacion = "";

                cotizarEnVivo();
                alert("¡Código promocional '" + cupon + "' validado y aplicado con éxito!");
            } else {
                var motivoError = (data && data.mensaje) ? data.mensaje : "Código promocional no existe o expiró.";
                alert("No es posible aplicar el código: " + motivoError);
            }
        },
        error: function () {
            $("#btnAplicarCupon").prop("disabled", false).html("<i class='fas fa-check'></i> Aplicar");
            // Fallback directo al motor de pedidos
            descuentoAplicado.tipo = "CUPON";
            descuentoAplicado.codigoPromocional = cupon;
            descuentoAplicado.descuentoManual = 0.0;
            descuentoAplicado.motivoDescuento = "CUPÓN: " + cupon;
            descuentoAplicado.justificacion = "";
            cotizarEnVivo();
        }
    });
}

function verificarRolUsuario() {
    $.ajax({
        url: "ValidarUsuarioAplicacion",
        dataType: "json",
        type: "POST",
        success: function (data) {
            if (data && data.length > 0) {
                var resp = data[0].respuesta;
                if (resp === "OKA") {
                    esAdministrador = true;
                    $("#btnAbrirModalDescuentoManual").show();
                } else {
                    esAdministrador = false;
                    $("#btnAbrirModalDescuentoManual").hide();
                }
            }
        },
        error: function () {
            // Por defecto en caso de error, no permitir descuentos manuales a nivel de asesor
            esAdministrador = false;
            $("#btnAbrirModalDescuentoManual").hide();
        }
    });
}

/** Id de la tienda marcada, o 0 si no hay ninguna (ya no se asume la 11). */
function idTiendaSeleccionada() {
    return parseInt($("#tiendaSelect").val()) || 0;
}

function consultarTiempoTienda(idTienda) {
    if (!idTienda) {
        idTienda = idTiendaSeleccionada();
    }
    if (!idTienda) {
        tiempoEstimadoTienda = 30;
        $("#txtTiempoTiendaFijo").val("");
        $("#txtModalTiempoEntrega").text("-");
        return;
    }
    $.ajax({
        url: "CRUDTiempoPedido?idoperacion=3&idtienda=" + idTienda + "&canal=POS",
        dataType: "json",
        type: "GET",
        success: function (data) {
            var t = 30;
            if (data && data.tiempopedido && !isNaN(parseInt(data.tiempopedido))) {
                t = parseInt(data.tiempopedido);
            }
            if (t <= 0) t = 30;
            tiempoEstimadoTienda = t;
            $("#txtTiempoTiendaFijo").val(t + " min");
            $("#txtModalTiempoEntrega").text(t + " minutos");
        },
        error: function () {
            tiempoEstimadoTienda = 30;
            $("#txtTiempoTiendaFijo").val("30 min");
            $("#txtModalTiempoEntrega").text("30 minutos");
        }
    });
}

function cargarHorariosProgramados() {
    var $sel = $("#selectHoraProgramada");
    $sel.empty();

    var horaInicio = 11;
    var minInicio = 30;
    var horaFin = 23;
    var minFin = 0;

    var ahora = new Date();
    var minActuales = ahora.getHours() * 60 + ahora.getMinutes();

    // Regla Contact Center: Se requieren al menos 60 minutos de anticipación hacia adelante.
    // Ej: Si son las 5:05 PM (17:05 = 1025 min), 6:00 PM (18:00 = 1080 min) queda excluida (faltan solo 55 min).
    // La primera franja programable disponible será 6:30 PM (18:30 = 1110 min >= 1085 min).
    var minMinimosProgramables = minActuales + 60;

    var hayOpciones = false;
    for (var m = (horaInicio * 60 + minInicio); m <= (horaFin * 60 + minFin); m += 30) {
        if (m >= minMinimosProgramables) {
            var hh = Math.floor(m / 60);
            var mm = m % 60;
            var hhStr = (hh < 10 ? "0" : "") + hh;
            var mmStr = (mm < 10 ? "0" : "") + mm;
            var val24 = hhStr + ":" + mmStr;

            var sufijo = hh >= 12 ? "PM" : "AM";
            var hh12 = hh % 12;
            if (hh12 === 0) hh12 = 12;
            var hh12Str = (hh12 < 10 ? "0" : "") + hh12;
            var texto12 = hh12Str + ":" + mmStr + " " + sufijo;

            $sel.append('<option value="' + val24 + '">' + texto12 + '</option>');
            hayOpciones = true;
        }
    }

    if (!hayOpciones) {
        $sel.append('<option value="">Sin franjas disponibles hoy</option>');
    }
}

function abrirModalDescuentoManual() {
    if (!esAdministrador) {
        alert("Los descuentos manuales solo están autorizados para líderes y administradores. Los asesores deben aplicar cupones u ofertas autorizadas.");
        return;
    }
    if (!resultadoMotor || draftLineas.length === 0) {
        alert("Debe agregar al menos un producto a la comanda antes de aplicar un descuento.");
        return;
    }
    $("#txtValorDescuentoManual").val("");
    $("#txtJustificacionDescuento").val("");
    $("#modalDescuentoManual").modal("show");
}

function guardarDescuentoManualModal() {
    var valDesc = parseFloat($("#txtValorDescuentoManual").val());
    if (isNaN(valDesc) || valDesc <= 0) {
        alert("Debe ingresar un valor de descuento válido mayor a cero.");
        $("#txtValorDescuentoManual").focus();
        return;
    }

    var tipo = $("input[name='rdTipoDesc']:checked").val();
    var motivo = $("#selectMotivoDescuento").val();
    var just = $("#txtJustificacionDescuento").val().trim();

    if (motivo === "Otro" && !just) {
        alert("Para la opción 'Otro Motivo' es obligatorio indicar la justificación.");
        $("#txtJustificacionDescuento").focus();
        return;
    }

    var subtotalActual = (resultadoMotor && resultadoMotor.subtotal) ? resultadoMotor.subtotal : 0;
    var valorPesos = 0;

    if (tipo === "VALOR") {
        valorPesos = valDesc;
    } else {
        // Porcentaje
        if (valDesc > 100) {
            alert("El porcentaje de descuento no puede ser superior al 100%.");
            return;
        }
        valorPesos = (subtotalActual * (valDesc / 100.0));
    }

    descuentoAplicado.tipo = "MANUAL";
    descuentoAplicado.codigoPromocional = "";
    descuentoAplicado.descuentoManual = valorPesos;
    descuentoAplicado.motivoDescuento = motivo + (just ? " (" + just + ")" : "");
    descuentoAplicado.justificacion = just;

    $("#modalDescuentoManual").modal("hide");
    cotizarEnVivo();
}

function removerDescuentoActivo() {
    descuentoAplicado.tipo = "";
    descuentoAplicado.codigoPromocional = "";
    descuentoAplicado.descuentoManual = 0.0;
    descuentoAplicado.motivoDescuento = "";
    descuentoAplicado.justificacion = "";
    $("#txtCodigoCupon").val("");
    $("#contenedorDescuentoActivo").hide();
    cotizarEnVivo();
}

/* =========================================================================
   PAGO Y CÁLCULO DE DEVUELTA (Paramétrico con tabla pizzaamericana.forma_pago)
   ========================================================================= */
function cargarFormasPagoParametricas() {
    $.getJSON("CRUDFormaPago?idoperacion=6&tipopago=T", function (data) {
        if (data && data.length > 0) {
            catalogoFormasPago = data;
            var $sel = $("#formaPagoSelect");
            var valorActual = $sel.val() || "1";
            $sel.empty();

            $.each(catalogoFormasPago, function (i, fp) {
                var etiqueta = fp.idformapago + " - " + fp.nombre;
                if (fp.idformapago === 2 && !fp.nombre.toLowerCase().includes("datáfono") && !fp.nombre.toLowerCase().includes("datafono")) {
                    etiqueta = fp.idformapago + " - Datáfono (" + fp.nombre + ")";
                } else if (fp.idformapago === 4 && !fp.nombre.toLowerCase().includes("wompi")) {
                    etiqueta = fp.idformapago + " - " + fp.nombre + " (Wompi)";
                }
                var $opt = $("<option></option>")
                    .val(fp.idformapago)
                    .attr("data-virtual", fp.virtual || "N")
                    .attr("data-tipo", fp.tipoformapago || "")
                    .text(etiqueta);
                $sel.append($opt);
            });

            if ($sel.find("option[value='" + valorActual + "']").length > 0) {
                $sel.val(valorActual);
            } else {
                $sel.val("1");
            }
            ajustarFormaPagoUI();
        }
    }).fail(function (err) {
        console.warn("CRUDFormaPago no disponible dinámicamente, se conserva catálogo base:", err);
    });
}

function esFormaPagoVirtual(idForma) {
    idForma = parseInt(idForma) || parseInt($("#formaPagoSelect").val()) || 1;
    if (catalogoFormasPago && catalogoFormasPago.length > 0) {
        var encontrada = catalogoFormasPago.find(function (f) {
            return (parseInt(f.idformapago) === idForma);
        });
        if (encontrada && encontrada.virtual) {
            return (encontrada.virtual.toUpperCase() === "S");
        }
    }
    var $opt = $("#formaPagoSelect option[value='" + idForma + "']");
    if ($opt.length && $opt.attr("data-virtual")) {
        return ($opt.attr("data-virtual").toUpperCase() === "S");
    }
    return (idForma === 4);
}

function ajustarFormaPagoUI() {
    var idForma = parseInt($("#formaPagoSelect").val()) || 1;
    if (idForma === 1) {
        // Efectivo
        $("#cajaDevueltaComanda").slideDown(150);
        calcularDevuelta();
    } else {
        $("#cajaDevueltaComanda").slideUp(150);
        if (resultadoMotor) {
            $("#txtValorPago").val(formatearPesos(resultadoMotor.total));
            $("#txtValorDevuelta").val("0");
        }
    }
}

function calcularDevuelta(origen) {
    if (!resultadoMotor) return;

    var idForma = parseInt($("#formaPagoSelect").val()) || 1;
    if (idForma !== 1) {
        $("#lblValorDevuelta").text("Devuelta: $ 0");
        return;
    }

    var total = resultadoMotor.total || 0;
    var esModoExacto = $("input[name='radioModoPagoEfectivo']:checked").val() === "exacto";

    if (esModoExacto || origen === "exacto") {
        $("#txtValorPago").val(formatearPesos(total)).prop("disabled", true);
        $("#txtValorDevuelta").val("0").prop("disabled", true);
        $("#lblValorDevuelta").removeClass("text-danger").addClass("text-success").text("Pago Exacto ($ " + formatearPesos(total) + ")");
        return;
    }

    $("#txtValorPago").prop("disabled", false);
    $("#txtValorDevuelta").prop("disabled", false);

    if (origen === "devuelta") {
        // El usuario digitó directamente la devuelta que desea llevar
        var devStr = ($("#txtValorDevuelta").val() || "").toString().replace(/\D/g, "");
        var devVal = parseFloat(devStr) || 0;
        var pagaCon = total + devVal;
        $("#txtValorPago").val(formatearPesos(pagaCon));
        $("#lblValorDevuelta").removeClass("text-danger").addClass("text-success").text("Llevar devuelta de $ " + formatearPesos(devVal));
    } else {
        // El usuario digitó el monto con el que paga el cliente o usó atajo de billete
        var valPagoStr = ($("#txtValorPago").val() || "").toString().replace(/\D/g, "");
        var valPago = parseFloat(valPagoStr) || 0;

        if (valPago >= total) {
            var dev = valPago - total;
            $("#txtValorDevuelta").val(formatearPesos(dev));
            $("#lblValorDevuelta").removeClass("text-danger").addClass("text-success").text("Devuelta: $ " + formatearPesos(dev));
        } else {
            var falta = total - valPago;
            $("#txtValorDevuelta").val("0");
            $("#lblValorDevuelta").removeClass("text-success").addClass("text-danger").text("Faltan $ " + formatearPesos(falta));
        }
    }
}

/* =========================================================================
   COTIZADOR EN VIVO (MOTOR DE PEDIDO SERVER-SIDE)
   ========================================================================= */
function cotizarEnVivo() {
    $("#badgeItemCount").text(draftLineas.length + " item(s)");

    if (draftLineas.length === 0) {
        $("#listaComanda").html("<p class='text-muted text-center py-4'>La comanda está vacía.<br><small>Selecciona productos en el menú.</small></p>");
        $("#subtotalTxt").text("$ 0");
        $("#descuentoTxt").text("-$ 0");
        $("#domicilioTxt").text("$ 0");
        $("#totalTxt").text("$ 0");
        $("#contenedorDescuentoActivo").hide();
        actualizarCajaPuntosComanda(0);
        $("#btnConfirmarPedido").prop("disabled", true);
        return;
    }

    var idTienda = idTiendaSeleccionada();
    if (!idTienda) {
        // Sin tienda no hay precios ni cobertura que cotizar
        $("#listaComanda").html("<p class='text-danger text-center py-4 font-weight-bold'><i class='fas fa-store mr-1'></i>Seleccione la tienda para cotizar el pedido.</p>");
        $("#btnConfirmarPedido").prop("disabled", true);
        return;
    }
    var idTipoPedido = parseInt($("#tipoPedidoSelect").val()) || 1; // 1 = Domicilio
    var esProg = $("#chkPedidoProgramado").is(":checked");
    var horaProg = esProg ? ($("#selectHoraProgramada").val() || "") : "";

    var solicitud = {
        contexto: {
            idtienda: idTienda,
            canal: "T", // T = Contact Center
            idtipopedido: idTipoPedido,
            programado: esProg,
            horaProgramado: horaProg,
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
        descuentos: {
            codigoPromocional: descuentoAplicado.codigoPromocional || "",
            redimirPuntos: false,
            descuentoManual: descuentoAplicado.descuentoManual || 0.0,
            motivoDescuento: descuentoAplicado.motivoDescuento || ""
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

/**
 * Normaliza y clasifica cada línea hija (sabor, adición, bebida o incluido)
 * para garantizar una visualización clara, destacada y diferenciada en la comanda.
 */
function formatearLineaHijaComanda(h, draftItem) {
    var isInc = (h.origen === "INCLUIDO");
    var descH = (h.descripcion || ("Opción #" + h.idproducto)).trim();

    // 1. Buscar en selecciones del draft original para rescatar la porción exacta elegida por el asesor
    var metaDraft = null;
    if (draftItem && draftItem.selecciones && draftItem.selecciones.length > 0) {
        // Coincidencia exacta por idproducto Y mitad (si h.mitad está definida)
        for (var k = 0; k < draftItem.selecciones.length; k++) {
            var sel = draftItem.selecciones[k];
            if (sel.idproductoOpcion === h.idproducto) {
                if (h.mitad > 0 && sel.mitad === h.mitad) {
                    metaDraft = sel;
                    break;
                } else if (h.mitad === 0 && (!sel.mitad || sel.mitad === 0)) {
                    metaDraft = sel;
                    break;
                }
            }
        }
        if (!metaDraft) {
            for (var k = 0; k < draftItem.selecciones.length; k++) {
                var sel = draftItem.selecciones[k];
                if (sel.idproductoOpcion === h.idproducto) {
                    metaDraft = sel;
                    break;
                }
            }
        }
    }

    var esAdicion = (metaDraft && metaDraft.esAdicion)
        || h.esAdicion
        || (descH.toLowerCase().indexOf("adici") !== -1)
        || (h.valorTotal > 0 && !isInc);

    if (esAdicion) {
        var porcionVal = (metaDraft && metaDraft.porcion) ? metaDraft.porcion : null;
        var mitadVal = (h.mitad !== undefined && h.mitad > 0)
            ? h.mitad
            : ((metaDraft && metaDraft.mitad !== undefined && metaDraft.mitad > 0) ? metaDraft.mitad : 0);
        var porcionLabel = "Toda (100%)";
        var badgeColor = "badge-dark";

        if (porcionVal === "m1" || mitadVal === 1) {
            porcionLabel = "Mitad 1 (50%)";
            badgeColor = "badge-danger";
        } else if (porcionVal === "m2" || mitadVal === 2) {
            porcionLabel = "Mitad 2 (50%)";
            badgeColor = "badge-primary";
        } else if (porcionVal === "entera" || mitadVal === 0) {
            porcionLabel = "Toda (100%)";
            badgeColor = "badge-dark";
        } else if (h.cantidad < 1.0) {
            porcionLabel = "Mitad (50%)";
            badgeColor = "badge-danger";
        }

        // Limpiar prefijo 'Adición' repetido para que quede limpio junto al badge
        var nombreLimpio = descH.replace(/^adici[oó]n\s+/i, "").trim();

        return {
            tipo: "ADICION",
            esAdicion: true,
            nombre: nombreLimpio,
            porcion: porcionLabel,
            badgeColor: badgeColor,
            precio: h.valorTotal
        };
    } else if (isInc) {
        return {
            tipo: "INCLUIDO",
            esAdicion: false,
            nombre: descH,
            porcion: "",
            badgeColor: "badge-success",
            precio: 0
        };
    } else {
        // Sabor o Bebida regular
        var mitadVal = (h.mitad !== undefined && h.mitad > 0)
            ? h.mitad
            : ((metaDraft && metaDraft.mitad !== undefined && metaDraft.mitad > 0) ? metaDraft.mitad : 0);
        var porcionLabel = "";
        var badgeColor = "";
        if (mitadVal === 1) {
            porcionLabel = "Mitad 1";
            badgeColor = "badge-danger";
        } else if (mitadVal === 2) {
            porcionLabel = "Mitad 2";
            badgeColor = "badge-primary";
        } else if (h.cantidad === 0.5) {
            porcionLabel = "Mitad";
            badgeColor = "badge-secondary";
        }
        return {
            tipo: "OPCION",
            esAdicion: false,
            nombre: descH,
            porcion: porcionLabel,
            badgeColor: badgeColor,
            precio: h.valorTotal
        };
    }
}

function renderizarComanda(res) {
    var $box = $("#listaComanda");
    $box.empty();

    if (!res || !res.lineas) return;

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
            $m.append("<span>" + l.cantidad + "x " + (l.descripcion || ("Producto #" + l.idproducto)) + "</span>");
            var $der = $("<span></span>").text("$ " + formatearPesos(l.valorTotal));
            var $del = $("<i class='fas fa-trash-alt btn-eliminar-item ml-2' title='Quitar producto'></i>").on("click", function () {
                eliminarItemDraft(itemDraftIdx);
            });
            $der.append($del);
            $m.append($der);
            $item.append($m);

            if (hijasMap[l.ref]) {
                var draftItem = draftLineas[itemDraftIdx];
                $.each(hijasMap[l.ref], function (j, h) {
                    var info = formatearLineaHijaComanda(h, draftItem);
                    if (info.tipo === "ADICION") {
                        var $h = $("<div></div>").addClass("item-comanda-adicion");
                        var $izq = $("<div></div>").addClass("d-flex align-items-center flex-wrap");
                        $izq.append("<span class='badge badge-warning text-dark mr-1 font-weight-bold' style='font-size:0.70rem;'><i class='fas fa-plus-circle text-danger mr-1'></i>Adición</span>");
                        $izq.append("<span class='badge " + info.badgeColor + " mr-1' style='font-size:0.68rem;'>" + info.porcion + "</span>");
                        $izq.append("<b class='text-dark'>" + info.nombre + "</b>");
                        $h.append($izq);
                        $h.append("<span class='text-danger font-weight-bold ml-auto'>+$ " + formatearPesos(info.precio) + "</span>");
                        $item.append($h);
                    } else if (info.tipo === "INCLUIDO") {
                        var $h = $("<div></div>").addClass("item-comanda-incluido");
                        $h.append("<span><span class='badge badge-success mr-1' style='font-size:0.68rem;'><i class='fas fa-check mr-1'></i>Incluye</span> " + info.nombre + "</span>");
                        $h.append("<span>$ 0</span>");
                        $item.append($h);
                    } else {
                        var $h = $("<div></div>").addClass("item-comanda-hija");
                        var prefBadge = info.porcion ? ("<span class='badge " + info.badgeColor + " mr-1' style='font-size:0.68rem;'>" + info.porcion + "</span>") : "<span class='text-muted mr-1'>•</span> ";
                        $h.append("<span>" + prefBadge + "<span class='text-dark font-weight-bold'>" + info.nombre + "</span></span>");
                        $h.append("<span>" + (info.precio > 0 ? "+$ " + formatearPesos(info.precio) : "$ 0") + "</span>");
                        $item.append($h);
                    }
                });
            }

            $box.append($item);
        }
    });

    var domVal = 0;
    $.each(res.lineas, function (i, l) {
        if (l.origen === "DOMICILIO") domVal += l.valorTotal;
    });

    $("#subtotalTxt").text("$ " + formatearPesos(res.subtotal - domVal));
    $("#domicilioTxt").text("$ " + formatearPesos(domVal));
    $("#descuentoTxt").text("-$ " + formatearPesos(res.descuento));
    $("#totalTxt").text("$ " + formatearPesos(res.total));

    // Estado visual del Descuento activo
    if (res.descuento > 0) {
        var textoDesc = "Descuento aplicado: -$ " + formatearPesos(res.descuento);
        if (descuentoAplicado.tipo === "CUPON") {
            textoDesc = "Cupón " + descuentoAplicado.codigoPromocional + ": -$ " + formatearPesos(res.descuento);
        } else if (descuentoAplicado.tipo === "MANUAL") {
            textoDesc = "Descuento manual: -$ " + formatearPesos(res.descuento) + " (" + (descuentoAplicado.motivoDescuento || "Auditoría") + ")";
        }
        $("#lblDetalleDescuentoActivo").html("<i class='fas fa-tag mr-1'></i>" + textoDesc);
        $("#contenedorDescuentoActivo").css("display", "flex");
    } else {
        $("#contenedorDescuentoActivo").hide();
    }

    // Actualizar valor pago por defecto si está vacío
    var valPagoStr = ($("#txtValorPago").val() || "").toString().replace(/\D/g, "");
    if (!valPagoStr || parseFloat(valPagoStr) === 0) {
        $("#txtValorPago").val(formatearPesos(res.total));
    }
    calcularDevuelta();

    // Actualizar estado de Puntos Pizza en Comanda
    var totalPtsReq = calcularPuntosRequeridosComanda();
    actualizarCajaPuntosComanda(totalPtsReq);
}

function eliminarItemDraft(index) {
    if (index >= 0 && index < draftLineas.length) {
        draftLineas.splice(index, 1);
        var quedanEmpleados = draftLineas.some(function (l) { return l.esEmpleado; });
        if (!quedanEmpleados) {
            empleadoPrecioEmpleadoValidado = null;
        }
        actualizarBadgePrecioEmpleado();
        var totalPtsReq = calcularPuntosRequeridosComanda();
        actualizarCajaPuntosComanda(totalPtsReq);
        cotizarEnVivo();
    }
}

/* =========================================================================
   MODAL DE RESUMEN Y CONFIRMACIÓN DE PEDIDO (Reemplaza confirm simple)
   ========================================================================= */
function abrirModalConfirmacionPedido() {
    if (!idTiendaSeleccionada()) {
        alert("Seleccione la tienda a la que va el pedido.");
        $("#tiendaSelect").focus();
        return;
    }
    if (!clienteUbicado) {
        alert("¡Atención! Debe ubicar al cliente en el mapa antes de despachar el pedido.");
        abrirModalMapa();
        return;
    }

    if (!resultadoMotor || draftLineas.length === 0) {
        alert("La comanda no tiene productos para despachar.");
        return;
    }

    // Regla de Puntos Pizza: validación obligatoria antes de confirmar
    var totalPtsReq = calcularPuntosRequeridosComanda();
    if (totalPtsReq > 0 && puntosRedimidos < totalPtsReq) {
        alert("⚠️ CONDICIONAL OBLIGATORIO: REDENCIÓN DE PUNTOS PIZZA\n\n"
            + "El pedido contiene productos que requieren " + totalPtsReq + " puntos de fidelización.\n"
            + "Puntos redimidos hasta el momento: " + puntosRedimidos + " pts.\n\n"
            + "Por favor realice la redención de puntos con el código enviado al cliente antes de continuar.");
        abrirModalRedencionPuntos();
        return;
    }

    sincronizarDatosCliente();

    // Validar paramétricamente Formas de Pago Virtuales (Wompi, etc.: virtual = 'S')
    var idFormaSel = parseInt($("#formaPagoSelect").val()) || 1;
    if (esFormaPagoVirtual(idFormaSel)) {
        var polAceptada = $("#chkPoliticaDatos").is(":checked") && (clienteActual.politicaDatos === "S");
        if (!polAceptada) {
            alert(
                "⚠️ CONDICIONAL OBLIGATORIO: PAGO VIRTUAL\n\n" +
                "La forma de pago seleccionada es VIRTUAL y requiere obligatoriamente que el cliente acepte la Política de Tratamiento de Datos (Habeas Data) para enviarle el link de pago por WhatsApp y correo electrónico.\n\n" +
                "Por favor active la casilla «Acepta Política de Datos» antes de confirmar."
            );
            $("#chkPoliticaDatos").focus();
            return;
        }
        var telCel = ($("#telcelular").val() || clienteActual.telefono || "").trim();
        if (!telCel || telCel.length < 7) {
            alert("⚠️ CUIDADO: Para formas de pago virtual debe diligenciar un número de teléfono celular válido para enviar el enlace de pago.");
            $("#telcelular").focus();
            return;
        }
    }

    // Validar facturación electrónica si fue solicitada
    if (clienteActual.clientesiniden === "N") {
        if (!clienteActual.identificacion || clienteActual.identificacion === "222222222222") {
            alert("Para factura electrónica DIAN debe ingresar el número de identificación (Cédula o NIT).");
            $("#cliIdentificacion").focus();
            return;
        }
        if (!clienteActual.emailfact) {
            alert("Para factura electrónica DIAN debe ingresar el correo electrónico de facturación.");
            $("#cliEmailFacturacion").focus();
            return;
        }
    }

    // 1. Cabecera y Tienda
    var nombreTiendaSel = $("#tiendaSelect option:selected").text();
    $("#txtModalTiendaDestino").html("<i class='fas fa-store mr-1'></i>" + nombreTiendaSel);

    // 2. Datos del Cliente y Entrega
    if (contingenciaTiendaForzada && tiendaCoberturaCliente) {
        $("#bannerAlertaContingenciaTienda").html(
            "<i class='fas fa-exclamation-triangle mr-2 text-warning'></i>" +
            "<span><b>Atención Contingencia:</b> Este pedido a domicilio será despachado desde <b>" +
            nombreTiendaSel + "</b>, aunque la dirección pertenece a la cobertura de <b>" +
            tiendaCoberturaCliente.nombre + "</b>.</span>"
        ).show();
    } else {
        $("#bannerAlertaContingenciaTienda").hide();
    }

    var nomCompleto = clienteActual.nombres + (clienteActual.apellidos ? " " + clienteActual.apellidos : "");
    $("#txtModalClienteNombre").text(nomCompleto);
    $("#txtModalClienteTel").text(clienteActual.telefono);
    $("#txtModalClienteDir").text(clienteActual.direccion);
    $("#txtModalClienteMun").text(clienteActual.municipio || "Medellín");
    $("#txtModalClienteBarrio").text(clienteActual.zona || "No especificado");
    $("#txtModalClienteObs").text(clienteActual.observacion || "Sin observaciones especiales");
    $("#txtModalCoords").text(clienteActual.latitud.toFixed(6) + ", " + clienteActual.longitud.toFixed(6));
    $("#txtModalDistancia").text(clienteActual.distanciatienda ? clienteActual.distanciatienda.toFixed(1) + " km" : "-");

    // 3. Facturación Electrónica DIAN
    if (clienteActual.clientesiniden === "N") {
        $("#badgeModalFacturaTipo").removeClass("badge-info").addClass("badge-primary").text("Factura Electrónica (DIAN)");
        $("#txtModalFactTipoPersona").text(clienteActual.idtipopersona === 1 ? "1 - Persona Jurídica (NIT)" : "2 - Persona Natural (Cédula)");
        $("#txtModalFactIdentificacion").text(clienteActual.identificacion);
        $("#txtModalFactEmail").text(clienteActual.emailfact);
        $("#bodyModalFactura").show();
    } else {
        $("#badgeModalFacturaTipo").removeClass("badge-primary").addClass("badge-secondary").text("Consumidor Final (Sin Identificación)");
        $("#txtModalFactTipoPersona").text("No aplica");
        $("#txtModalFactIdentificacion").text("222222222222");
        $("#txtModalFactEmail").text("No registrado (Consumidor Final)");
    }

    // 4. Condiciones Operativas
    var tipoPedTxt = $("#tipoPedidoSelect option:selected").text();
    $("#txtModalTipoPedido").text(tipoPedTxt);
    $("#txtModalTiempoEntrega").text(tiempoEstimadoTienda + " minutos");

    var esProg = $("#chkPedidoProgramado").is(":checked");
    if (esProg) {
        var hProg = $("#selectHoraProgramada option:selected").text() || "Hora no definida";
        $("#txtModalProgramacion").removeClass("badge-success").addClass("badge-warning text-dark").html("<i class='far fa-clock mr-1'></i>Programado para: <b>" + hProg + "</b>");
    } else {
        $("#txtModalProgramacion").removeClass("badge-warning text-dark").addClass("badge-success").html("<i class='fas fa-bolt mr-1'></i>Para Inmediato (Ya)");
    }

    // 5. Tabla de Comanda Detallada
    if (empleadoPrecioEmpleadoValidado) {
        $("#txtModalBeneficiarioEmpleado").text(
            empleadoPrecioEmpleadoValidado.nombre + " (CC: " + empleadoPrecioEmpleadoValidado.cedula + ") - Código: " + empleadoPrecioEmpleadoValidado.codigo
        );
        $("#boxModalPrecioEmpleado").show();
    } else {
        $("#boxModalPrecioEmpleado").hide();
    }

    var $tbody = $("#tbodyModalComanda");
    $tbody.empty();

    var hijasMap = {};
    $.each(resultadoMotor.lineas, function (i, l) {
        if (l.refMaster > 0) {
            if (!hijasMap[l.refMaster]) hijasMap[l.refMaster] = [];
            hijasMap[l.refMaster].push(l);
        }
    });

    var totalItemsCount = 0;
    $.each(resultadoMotor.lineas, function (i, l) {
        if (l.refMaster === 0 && l.origen !== "DOMICILIO") {
            totalItemsCount++;
            var $tr = $("<tr></tr>");
            $tr.append("<td class='font-weight-bold text-center'>" + l.cantidad + "x</td>");

            var $tdDesc = $("<td></td>");
            $tdDesc.append("<div class='font-weight-bold text-dark'>" + (l.descripcion || ("Producto #" + l.idproducto)) + "</div>");

            if (hijasMap[l.ref]) {
                var draftItem = (totalItemsCount - 1 < draftLineas.length) ? draftLineas[totalItemsCount - 1] : null;
                var $subList = $("<div class='pl-2 small mt-1'></div>");
                $.each(hijasMap[l.ref], function (j, h) {
                    var info = formatearLineaHijaComanda(h, draftItem);
                    if (info.tipo === "ADICION") {
                        var $rowAdi = $("<div class='my-1 p-1 rounded bg-light border-left border-warning d-flex justify-content-between align-items-center' style='border-left: 3px solid #f59e0b !important;'></div>");
                        var $izq = $("<div></div>").append("<span class='badge badge-warning text-dark mr-1 font-weight-bold' style='font-size:0.70rem;'><i class='fas fa-plus-circle text-danger mr-1'></i>Adición</span>")
                            .append("<span class='badge " + info.badgeColor + " mr-1' style='font-size:0.68rem;'>" + info.porcion + "</span>")
                            .append("<b class='text-dark'>" + info.nombre + "</b>");
                        $rowAdi.append($izq);
                        $rowAdi.append("<b class='text-danger'>+$ " + formatearPesos(info.precio) + "</b>");
                        $subList.append($rowAdi);
                    } else if (info.tipo === "INCLUIDO") {
                        $subList.append("<div class='text-success'><span class='badge badge-success mr-1' style='font-size:0.68rem;'><i class='fas fa-check mr-1'></i>Incluye</span>" + info.nombre + " ($0)</div>");
                    } else {
                        var prefBadge = info.porcion ? ("<span class='badge " + info.badgeColor + " mr-1' style='font-size:0.68rem;'>" + info.porcion + "</span>") : "• ";
                        var precioExtra = info.precio > 0 ? " (+$" + formatearPesos(info.precio) + ")" : "";
                        $subList.append("<div class='text-muted'>" + prefBadge + "<span class='text-dark'>" + info.nombre + "</span>" + precioExtra + "</div>");
                    }
                });
                $tdDesc.append($subList);
            }
            $tr.append($tdDesc);
            $tr.append("<td class='text-right font-weight-bold'>$" + formatearPesos(l.valorTotal) + "</td>");
            $tbody.append($tr);
        }
    });

    $("#badgeModalTotalItems").text(totalItemsCount + " item(s)");

    // 6. Totales Financieros
    var domVal = 0;
    $.each(resultadoMotor.lineas, function (i, l) {
        if (l.origen === "DOMICILIO") domVal += l.valorTotal;
    });

    $("#txtModalSubtotal").text("$ " + formatearPesos(resultadoMotor.subtotal - domVal));
    $("#txtModalDomicilio").text("$ " + formatearPesos(domVal));
    $("#txtModalDescuento").text("-$ " + formatearPesos(resultadoMotor.descuento));
    if (descuentoAplicado.motivoDescuento) {
        $("#lblModalDescLabel").text("Descuento (" + descuentoAplicado.motivoDescuento + "):");
    } else {
        $("#lblModalDescLabel").text("Descuento:");
    }
    $("#txtModalGranTotal").text("$ " + formatearPesos(resultadoMotor.total));

    // 7. Pago y Devuelta
    var formaPagoId = parseInt($("#formaPagoSelect").val()) || 1;
    var formaPagoNom = $("#formaPagoSelect option:selected").text();
    if (esFormaPagoVirtual(formaPagoId)) {
        formaPagoNom += " [Habeas Data OK ✓]";
    }
    $("#txtModalFormaPago").text(formaPagoNom);

    var valPagoStr = ($("#txtValorPago").val() || "").toString().replace(/\D/g, "");
    var valPago = parseFloat(valPagoStr) || resultadoMotor.total;

    if (formaPagoId === 1) {
        // Efectivo
        $("#filaModalEfectivoDevuelta").show();
        $("#txtModalPagaCon").text("$ " + formatearPesos(valPago));
        var dev = (valPago >= resultadoMotor.total) ? (valPago - resultadoMotor.total) : 0;
        $("#txtModalDevuelta").text("$ " + formatearPesos(dev));
    } else {
        $("#filaModalEfectivoDevuelta").hide();
    }

    $("#modalConfirmarPedido").modal("show");
}

function ejecutarDespachoTiendaDefinitivo() {
    var idTienda = idTiendaSeleccionada();
    if (!idTienda) {
        alert("Seleccione la tienda a la que va el pedido.");
        return;
    }
    var idTipoPedido = parseInt($("#tipoPedidoSelect").val()) || 1;
    var idFormaPago = parseInt($("#formaPagoSelect").val()) || 1;

    // Regla Paramétrica: Bloqueo de seguridad si la forma de pago es virtual sin autorización de política de datos
    if (esFormaPagoVirtual(idFormaPago)) {
        var polAceptada = (clienteActual.politicaDatos === "S");
        if (!polAceptada) {
            alert("⚠️ ERROR: No es posible despachar el pedido con forma de pago virtual si el cliente no ha autorizado la Política de Tratamiento de Datos (Habeas Data).");
            $("#btnDespacharPedidoDefinitivo").prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i>Confirmar y Despachar a Tienda");
            return;
        }
    }

    // Regla de Puntos Pizza: validación obligatoria antes de despachar
    var totalPtsReq = calcularPuntosRequeridosComanda();
    if (totalPtsReq > 0 && puntosRedimidos < totalPtsReq) {
        alert("⚠️ ERROR: El pedido requiere " + totalPtsReq + " puntos pero solo se han redimido " + puntosRedimidos + " puntos.\n\nPor favor realice la redención de puntos antes de despachar a la tienda.");
        $("#btnDespacharPedidoDefinitivo").prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i>Confirmar y Despachar a Tienda");
        abrirModalRedencionPuntos();
        return;
    }

    var tiempoPed = tiempoEstimadoTienda || 30;
    var esProg = $("#chkPedidoProgramado").is(":checked");
    var horaProg = esProg ? ($("#selectHoraProgramada").val() || "") : "";

    var valPagoStr = ($("#txtValorPago").val() || "").toString().replace(/\D/g, "");
    var valPago = parseFloat(valPagoStr) || resultadoMotor.total;

    $("#btnDespacharPedidoDefinitivo").prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-2'></i>Despachando a Tienda...");

    var payload = {
        idTienda: idTienda,
        idCliente: clienteActual.idcliente,
        // El usuario del pedido NO viaja desde aqui: el servidor lo toma de la
        // sesion de quien esta logueado (antes iba "PILOTO" quemado).
        idFormaPagoTienda: idFormaPago,
        valorFormaPago: (idFormaPago === 1) ? valPago : resultadoMotor.total,
        tiempoPedido: tiempoPed,
        origen: (marcacionActiva && marcacionActiva.codigo && marcacionActiva.codigo.trim().length <= 3) ? marcacionActiva.codigo.trim() : "C",
        fuentepedido: "CC-NUEVO",
        idPedidoAlt: (marcacionActiva && marcacionActiva.observacion) ? marcacionActiva.observacion.trim() : "",
        plataforma: (marcacionActiva && marcacionActiva.nombremarcacion) ? marcacionActiva.nombremarcacion.trim() : "",
        idEmpleadoPrecioEmpleado: empleadoPrecioEmpleadoValidado ? empleadoPrecioEmpleadoValidado.id : 0,
        codigoPrecioEmpleado: empleadoPrecioEmpleadoValidado ? empleadoPrecioEmpleadoValidado.codigo : "",
        idsRedencion: idsRedencionPedido,
        puntosRedimidos: puntosRedimidos,
        marcacion: (marcacionActiva && marcacionActiva.idmarcacion > 0) ? {
            idmarcacion: marcacionActiva.idmarcacion,
            nombremarcacion: marcacionActiva.nombremarcacion,
            observacion: marcacionActiva.observacion || "",
            domiciliopropio: marcacionActiva.domiciliopropio || "S"
        } : null,
        contingenciaTienda: contingenciaTiendaForzada ? {
            tiendaCoberturaOriginal: tiendaCoberturaCliente ? tiendaCoberturaCliente.nombre : ""
        } : null,
        cliente: {
            idcliente: clienteActual.idcliente,
            nombres: clienteActual.nombres,
            apellidos: clienteActual.apellidos,
            telefono: clienteActual.telefono,
            direccion: clienteActual.direccion,
            municipio: clienteActual.municipio,
            zona: clienteActual.zona,
            observacion: clienteActual.observacion || "",
            latitud: clienteActual.latitud,
            longitud: clienteActual.longitud,
            distanciatienda: clienteActual.distanciatienda,
            email: clienteActual.email,
            emailfact: clienteActual.emailfact,
            clientesiniden: clienteActual.clientesiniden,
            idtipopersona: clienteActual.idtipopersona,
            identificacion: clienteActual.identificacion,
            politicadatos: clienteActual.politicaDatos || "S"
        },
        solicitud: {
            contexto: {
                idtienda: idTienda,
                canal: "T",
                idtipopedido: idTipoPedido,
                programado: esProg,
                horaProgramado: horaProg,
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
            descuentos: {
                codigoPromocional: descuentoAplicado.codigoPromocional || "",
                redimirPuntos: false,
                descuentoManual: descuentoAplicado.descuentoManual || 0.0,
                motivoDescuento: descuentoAplicado.motivoDescuento || ""
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
            $("#btnDespacharPedidoDefinitivo").prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i>Confirmar y Despachar a Tienda");
            $("#modalConfirmarPedido").modal("hide");

            if (resp.exito) {
                // Si hay marcación de plataforma activa (Rappi, DiDi, Bot, convenios), asociarla al pedido
                if (marcacionActiva && marcacionActiva.idmarcacion > 0) {
                    registrarMarcacionPedido(resp.idPedido, marcacionActiva);
                }

                // CASO 1: Pedido con Pago Virtual (Wompi, etc.)
                if (resp.pagoVirtual) {
                    var msjVirtual = "🎉 ¡PEDIDO REGISTRADO: PAGO VIRTUAL!\n\n"
                        + "• Número de Pedido Central: #" + resp.idPedido + "\n"
                        + "• ID Nuevo Motor: #" + (resp.idPedidoPos || resp.idPedido) + "\n"
                        + "• Estado Operativo: PENDIENTE DE PAGO VIRTUAL (No enviado a tienda aún)\n\n";

                    if (resp.wompiSuccess && resp.urlPago) {
                        msjVirtual += "✅ Link de Pago Wompi generado exitosamente:\n" + resp.urlPago + "\n\n"
                            + "📲 Se envió automáticamente al cliente por WhatsApp y correo electrónico.\n"
                            + "⏳ El pedido quedará en espera y se despachará a tienda una vez Wompi confirme el pago.";
                    } else {
                        msjVirtual += "⚠️ Atención: " + (resp.mensaje || "Inconveniente al generar link de pago.") + "\n\n"
                            + "El pedido quedó registrado en Central. Puede verificar o reenviar el link desde la pantalla de «Monitoreo de Pagos Virtuales».";
                    }
                    alert(msjVirtual);
                    limpiarPantallaTrasDespacho();
                    return;
                }

                // CASO 2: Pedido Normal (Efectivo / Datáfono)
                var msj = "🎉 ¡Pedido registrado con éxito!\n\n"
                    + "• ID Central (Legacy): #" + resp.idPedido + "\n"
                    + "• ID Nuevo Motor: #" + (resp.idPedidoPos || resp.idPedido) + "\n";

                if (resp.despachoTienda && resp.despachoTienda.exito) {
                    msj += "\nSe despachó directamente a la tienda con éxito.\n"
                        + "• Comanda en tienda: #" + resp.despachoTienda.numeroFactura + "\n"
                        + "• Trazabilidad enviada a tienda: " + (resp.idPedidoAlt || ("NM-" + (resp.idPedidoPos || resp.idPedido)));
                } else if (resp.despachoTienda && !resp.despachoTienda.exito) {
                    msj += "\nAtención en tienda: " + resp.despachoTienda.mensaje;
                }
                alert(msj);

                // Pedido listo para el siguiente cliente: nada del anterior debe quedar
                limpiarPantallaTrasDespacho();
            } else {
                alert("Error al finalizar el pedido: " + (resp.mensaje || "Error desconocido"));
            }
        },
        error: function (xhr) {
            $("#btnDespacharPedidoDefinitivo").prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i>Confirmar y Despachar a Tienda");
            alert("Error en el servidor al enviar pedido: " + (xhr.responseText || "Error de red"));
        }
    });
}

/**
 * Deja la pantalla como recien abierta para tomar el pedido SIGUIENTE.
 *
 * Antes solo se vaciaba la comanda, el descuento y el programado: el cliente
 * (con su idcliente), la direccion, la ubicacion en el mapa, la forma de pago y
 * el valor pagado se quedaban del pedido anterior. Con la comanda ya vacia y el
 * menu desbloqueado, un pedido nuevo salia a nombre del cliente anterior.
 *
 * Se conservan a proposito la tienda y el tipo de pedido: son el contexto de
 * trabajo de quien toma pedidos, no datos del cliente.
 */
function limpiarPantallaTrasDespacho() {
    // 1. Comanda, descuento, programado, puntos pizza y marcaciones de plataforma
    draftLineas = [];
    resultadoMotor = null;
    idPedidoCentralActivo = 0;
    removerDescuentoActivo();
    removerMarcacionPlataforma();
    empleadoPrecioEmpleadoValidado = null;
    productoEmpleadoPendienteId = null;
    actualizarBadgePrecioEmpleado();
    idsRedencionPedido = [];
    puntosRedimidos = 0;
    actualizarCajaPuntosComanda(0);
    $("#txtValorDescuentoManual, #txtJustificacionDescuento").val("");
    $("#selectMotivoDescuento").prop("selectedIndex", 0);
    $("#chkPedidoProgramado").prop("checked", false);
    $("#wrapperHoraProgramada").hide();

    // 2. El cliente: objeto, casillas, ubicacion y resultados de la busqueda
    clienteActual = {
        idcliente: 0,
        memcode: 0,
        nombres: "Cliente Mostrador",
        apellidos: "",
        telefono: "3000000000",
        direccion: "Mostrador Directo",
        idmunicipio: 1,
        municipio: "Medellin",
        zona: "",
        email: "",
        observacion: "",
        latitud: 0.0,
        longitud: 0.0,
        distanciatienda: 0.0,
        nombreTienda: "",
        politicaDatos: "S",
        emailfact: "",
        clientesiniden: "S",
        idtipopersona: 2,
        identificacion: "222222222222"
    };
    clienteUbicado = false;
    coberturaTemporal = null;
    tiendaCoberturaCliente = null;
    contingenciaTiendaForzada = false;

    $("#telcelular, #cliNombres, #cliApellidos, #cliEmail, #cliDireccion, #cliZona, #cliObservacion").val("");
    $("#cliIdentificacion, #cliEmailFacturacion").val("");
    $("#cliMunicipio, #cliTipoPersona").prop("selectedIndex", 0);
    $("#mapaBusqDireccion, #mapaBusqBarrio").val("");
    $("#chkFacturaElectronica").prop("checked", false).trigger("change");
    $("#tbodyClientesEncontrados").empty();
    $("#contenedorTablaClientes").hide();

    // Alertas y Fidelización
    $("#bannerAlertaCliente").hide();
    $("#contenidoAlertaCliente").empty();
    $("#franjaFidelizacion").hide().empty();

    // 2b. La tienda vuelve a "sin marcar": la fija la busqueda del cliente o quien toma el pedido
    $("#tiendaSelect").val("");
    verificarTiendaPiloto();
    consultarTiempoTienda(0);

    // 3. El pago
    $("#formaPagoSelect").val("1");
    $("#txtValorPago").val("");
    $("#lblValorDevuelta").removeClass("text-danger").addClass("text-success").text("$ 0");
    ajustarFormaPagoUI();

    // 4. Repintar: sin ubicar el menu se bloquea hasta buscar al cliente nuevo
    actualizarResumenClienteComanda();
    actualizarEstadoUbicacionUI();
    cotizarEnVivo();
    $("#telcelular").focus();
}

/**
 * Solicita confirmación con usuario y clave para reiniciar/cancelar el pedido en curso.
 * Similar al flujo histórico en pedidos.js:3112 (GetIngresarAplicacion).
 */
function solicitarReinicioPedidoPOS() {
    var hayDatosCliente = (clienteActual && clienteActual.idcliente > 0) || ($("#telcelular").val() && $("#telcelular").val().trim() !== "");
    var hayLineas = (draftLineas && draftLineas.length > 0);
    if (!hayDatosCliente && !hayLineas) {
        alert("La pantalla ya está limpia y no hay ningún pedido en curso.");
        return;
    }

    $("#txtUsuarioReinicio").val("");
    $("#txtPasswordReinicio").val("");
    $("#alertaErrorReinicioPOS").hide().text("");
    $("#modalReiniciarPedidoPOS").modal("show");
    setTimeout(function () {
        $("#txtUsuarioReinicio").focus();
    }, 400);
}

/**
 * Valida usuario y contraseña con el servicio GetIngresarAplicacion.
 * Si las credenciales son correctas, descarta el idPedido central si existía,
 * y blanquea completamente la pantalla para iniciar un nuevo cliente.
 */
function ejecutarReinicioPedidoPOS() {
    var usuario = $("#txtUsuarioReinicio").val().trim();
    var password = $("#txtPasswordReinicio").val().trim();
    if (!usuario || !password) {
        $("#alertaErrorReinicioPOS").html("<i class='fas fa-exclamation-circle mr-1'></i> Ingrese usuario y contraseña para continuar.").show();
        return;
    }

    $("#btnConfirmarReinicioDefinitivo").prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-1'></i> Validando...");
    $("#alertaErrorReinicioPOS").hide();

    $.ajax({
        url: "GetIngresarAplicacion",
        type: "POST",
        dataType: "text",
        data: {
            "txtUsuario": usuario,
            "txtPassword": password
        },
        success: function (data) {
            $("#btnConfirmarReinicioDefinitivo").prop("disabled", false).html("<i class='fas fa-undo-alt mr-1'></i> Confirmar Reinicio");
            var resultado = (data || "").trim();
            if (resultado === "OK") {
                $("#modalReiniciarPedidoPOS").modal("hide");

                // Si se generó un pedido preliminar en el backend, se descarta
                if (typeof idPedidoCentralActivo !== "undefined" && idPedidoCentralActivo > 0) {
                    $.ajax({
                        url: "EliminarPedidoSinConfirmar?idpedido=" + idPedidoCentralActivo,
                        dataType: "json",
                        type: "GET"
                    });
                    idPedidoCentralActivo = 0;
                }

                // Reversar automáticamente redenciones de puntos si el pedido no se finalizó
                var cantRev = (idsRedencionPedido && idsRedencionPedido.length > 0) ? idsRedencionPedido.length : 0;
                reversarRedencionesActivas("Pedido cancelado o reiniciado por el usuario");

                limpiarPantallaTrasDespacho();
                var extraMsg = cantRev > 0 ? " Los puntos redimidos fueron devueltos automáticamente al cliente." : "";
                alert("✅ Pedido reiniciado exitosamente. Toda la comanda y datos del cliente han sido descartados." + extraMsg);
            } else {
                $("#alertaErrorReinicioPOS").html("<i class='fas fa-times-circle mr-1'></i> Usuario o contraseña incorrectos.").show();
                $("#txtPasswordReinicio").val("").focus();
            }
        },
        error: function (xhr) {
            $("#btnConfirmarReinicioDefinitivo").prop("disabled", false).html("<i class='fas fa-undo-alt mr-1'></i> Confirmar Reinicio");
            $("#alertaErrorReinicioPOS").html("<i class='fas fa-exclamation-triangle mr-1'></i> Error al validar credenciales: " + (xhr.statusText || "Error de red")).show();
        }
    });
}

function formatearPesos(val) {
    if (!val || isNaN(val)) return "0";
    return Math.round(val).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

/* =========================================================================
   MARCACIONES Y PLATAFORMAS EXTERNAS (Rappi, DiDi, Bot, Convenios)
   ========================================================================= */
function cargarCatalogoMarcaciones() {
    $.getJSON("ObtenerMarcaciones?adm=S", function (data) {
        if (data && data.length > 0) {
            catalogoMarcaciones = data;
            var $sel = $("#selectMarcacionPlataforma");
            $sel.empty().append("<option value='0'>-- Ninguna (Pedido Directo Tienda / Contact Center) --</option>");
            $.each(catalogoMarcaciones, function (i, m) {
                var tag = (m.marketplace === "S") ? " [Marketplace]" : "";
                $sel.append("<option value='" + m.idmarcacion + "'>" + m.nombremarcacion + tag + "</option>");
            });
        }
    }).fail(function () {
        console.warn("No se pudo cargar el catálogo de marcaciones.");
    });
}

function abrirModalMarcacionPlataforma() {
    if (marcacionActiva && marcacionActiva.idmarcacion > 0) {
        $("#selectMarcacionPlataforma").val(marcacionActiva.idmarcacion).trigger("change");
        $("#txtObsMarcacionPlataforma").val(marcacionActiva.observacion || "");
        $("#chkMarketplaceMarcacion").prop("checked", marcacionActiva.marketplace === "S");
        $("#txtDescuentoMarcacion").val(marcacionActiva.descuento || "");
    } else {
        $("#selectMarcacionPlataforma").val("0").trigger("change");
        $("#txtObsMarcacionPlataforma").val("");
        $("#chkMarketplaceMarcacion").prop("checked", true);
        $("#txtDescuentoMarcacion").val("");
    }
    $("#modalMarcacionPlataforma").modal("show");
}

function guardarMarcacionPlataforma() {
    var idm = parseInt($("#selectMarcacionPlataforma").val()) || 0;
    if (idm === 0) {
        removerMarcacionPlataforma();
        $("#modalMarcacionPlataforma").modal("hide");
        return;
    }
    var sel = catalogoMarcaciones.find(function (m) { return m.idmarcacion === idm; });
    var obs = $("#txtObsMarcacionPlataforma").val().trim();
    var domiPropio = $("#chkMarketplaceMarcacion").is(":checked") ? "S" : "N";
    var desc = parseFloat($("#txtDescuentoMarcacion").val()) || 0;

    marcacionActiva = {
        idmarcacion: idm,
        nombremarcacion: sel ? sel.nombremarcacion : ("Marcación #" + idm),
        observacion: obs,
        marketplace: domiPropio,
        descuento: desc,
        descuentoAsumido: (desc > 0 ? 1 : 0)
    };

    var modoDomi = (domiPropio === "S") ? "Domi Propio" : "Repartidor Plataforma";
    var txtBadge = marcacionActiva.nombremarcacion + (obs ? " (#" + obs + ")" : "") + " &middot; " + modoDomi;
    $("#txtDetalleMarcacionActiva").html(txtBadge);
    $("#badgeMarcacionActiva").css("display", "flex").hide().slideDown(150);
    $("#btnAbrirModalMarcacionPlataforma").removeClass("btn-outline-light").addClass("btn-warning text-dark");
    $("#modalMarcacionPlataforma").modal("hide");
}

function removerMarcacionPlataforma() {
    marcacionActiva = null;
    $("#selectMarcacionPlataforma").val("0");
    $("#txtObsMarcacionPlataforma").val("");
    $("#txtDescuentoMarcacion").val("");
    $("#grupoDetalleMarcacion").hide();
    $("#badgeMarcacionActiva").slideUp(150, function() {
        $(this).css("display", "none");
    });
    $("#btnAbrirModalMarcacionPlataforma").removeClass("btn-warning text-dark").addClass("btn-outline-light");
}

function registrarMarcacionPedido(idPedido, m) {
    if (!idPedido || !m || !m.idmarcacion) return;
    var obs = m.observacion || "";
    var desc = m.descuento || 0;
    var motivo = "";
    var mkt = m.marketplace || "S";
    var descAsumido = m.descuentoAsumido || 0;

    $.getJSON("InsertarMarcacionPedido?idpedido=" + idPedido +
        "&idmarcacion=" + m.idmarcacion +
        "&observacion=" + encodeURIComponent(obs) +
        "&descuento=" + desc +
        "&motivo=" + encodeURIComponent(motivo) +
        "&marketplace=" + mkt +
        "&descuentoasumido=" + descAsumido, function (res) {
            console.log("Marcación asociada con éxito al pedido #" + idPedido, res);
        }).fail(function (err) {
            console.error("Error al asociar marcación al pedido #" + idPedido, err);
        });
}

/* =========================================================================
   ALERTAS Y MARCAS DE SEGURIDAD / FRAUDE DEL CLIENTE
   ========================================================================= */
function consultarAlertaCliente(telefono) {
    if (!telefono || telefono.length < 7) {
        $("#bannerAlertaCliente").slideUp(150);
        $("#contenidoAlertaCliente").empty();
        return;
    }

    $.getJSON("ObtenerClienteAlerta?telefono=" + encodeURIComponent(telefono), function (data) {
        if (!data || !data.mensaje) {
            $("#bannerAlertaCliente").slideUp(150);
            $("#contenidoAlertaCliente").empty();
            return;
        }

        var marcas = data.marcas || [];
        var negativa = (data.tipo_alerta === "ALERTA_NEGATIVA");
        var positiva = (data.tipo_alerta === "ALERTA_POSITIVA");
        var texto = (typeof data.texto === "string") ? data.texto : data.mensaje;

        var html = construirHtmlAlertaCliente(marcas, texto, negativa);
        $("#contenidoAlertaCliente").html(html);

        $("#bannerAlertaCliente").removeClass("alert-danger alert-warning alert-success");
        if (negativa) {
            $("#bannerAlertaCliente").addClass("alert-danger");
        } else if (positiva) {
            $("#bannerAlertaCliente").addClass("alert-success");
        } else {
            $("#bannerAlertaCliente").addClass("alert-warning");
        }
        $("#bannerAlertaCliente").slideDown(150);
    }).fail(function () {
        $("#bannerAlertaCliente").slideUp(150);
    });
}

function construirHtmlAlertaCliente(marcas, texto, negativa) {
    function esc(t) { return String(t == null ? '' : t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }
    var html = '<div class="d-flex align-items-center flex-wrap" style="gap:6px;">';
    html += '<strong class="mr-1"><i class="fas fa-shield-alt mr-1"></i>' + (negativa ? 'ALERTA DE SEGURIDAD / FRAUDE:' : 'OBSERVACIÓN DEL CLIENTE:') + '</strong>';

    if (marcas && marcas.length > 0) {
        for (var i = 0; i < marcas.length; i++) {
            var m = marcas[i];
            var badgeClass = (m.severidad === 'N') ? 'badge-danger' : ((m.severidad === 'P') ? 'badge-success' : 'badge-warning text-dark');
            html += '<span class="badge ' + badgeClass + ' p-1 px-2" style="font-size:0.8rem;">'
                + esc(m.nombre) + (m.bloquea_cargo ? ' &middot; Bloquea Cargo' : '') + '</span>';
        }
    }
    if (texto) {
        html += '<span class="ml-1 text-dark font-weight-bold" style="font-size:0.83rem;">&ndash; ' + esc(texto) + '</span>';
    }
    html += '</div>';
    return html;
}

/* =========================================================================
   PROGRAMA DE FIDELIZACIÓN (PUNTOS PIZZA)
   ========================================================================= */
function consultarFidelizacionCliente(correo) {
    if (!correo || correo.indexOf("@") === -1) {
        $("#franjaFidelizacion").slideUp(150).empty();
        return;
    }

    $.getJSON("ServiciosClienteFidelizacion?idoperacion=1&correo=" + encodeURIComponent(correo), function (data) {
        if (data && data.respuesta) {
            // El cliente está inscrito en el plan: consultar saldo y puntos por vencer
            $.getJSON("ServiciosClienteFidelizacion?idoperacion=10&correo=" + encodeURIComponent(correo), function (r) {
                var puntos = Math.round((r && r.puntos) || 0);
                var porVencer = Math.round((r && r.porvencer) || 0);
                var dias = (r && r.dias) || 60;
                var venceStr = (r && r.vence) ? fechaVenceLegible(r.vence) : "";

                var html = '<div class="d-flex align-items-center justify-content-between flex-wrap" style="gap: 8px;">';
                html += '<div>';
                html += '<span class="badge badge-warning text-dark mr-2"><i class="fas fa-coins mr-1"></i>Puntos Pizza</span>';
                html += '<span>Tiene <b>' + puntos + '</b> puntos disponibles</span>';
                if (porVencer > 0) {
                    html += '<span class="badge badge-danger ml-2"><i class="fas fa-exclamation-triangle mr-1"></i>' + porVencer + ' vencen en ' + dias + ' días' + (venceStr ? ' (desde ' + venceStr + ')' : '') + '</span>';
                }
                html += '</div>';
                html += '<div>';
                html += '<span class="badge badge-success"><i class="fas fa-check-circle mr-1"></i>Cliente Fidelizado</span>';
                html += '</div>';
                html += '</div>';

                $("#franjaFidelizacion").html(html).removeClass("border-danger border-info border-secondary").addClass("border-primary").slideDown(150);
            });
        } else {
            // No está en el plan: verificar si el cliente manifestó no desear pertenecer
            $.getJSON("ServiciosClienteFidelizacion?idoperacion=9&correo=" + encodeURIComponent(correo), function (data2) {
                if (data2 && data2.respuesta) {
                    var htmlRechazo = '<div class="d-flex align-items-center justify-content-between flex-wrap text-danger font-weight-bold" style="gap: 8px;">';
                    htmlRechazo += '<span><i class="fas fa-ban mr-1"></i> Cliente manifestó <u>NO querer pertenecer</u> al plan de fidelización.</span>';
                    htmlRechazo += '<span class="badge badge-danger">No Desea Fidelización</span>';
                    htmlRechazo += '</div>';
                    $("#franjaFidelizacion").html(htmlRechazo).removeClass("border-primary border-info border-secondary").addClass("border-danger").slideDown(150);
                } else {
                    // Cliente no inscrito: mostrar invitación y botón de vinculación
                    var htmlInscribir = '<div class="d-flex align-items-center justify-content-between flex-wrap" style="gap: 8px;">';
                    htmlInscribir += '<span><i class="fas fa-gift text-warning mr-1"></i> El cliente <b>no está vinculado</b> al programa de fidelización Puntos Pizza.</span>';
                    htmlInscribir += '<button type="button" class="btn btn-xs btn-primary font-weight-bold py-1 px-3 shadow-sm" id="btnInscribirFidelizacion"><i class="fas fa-user-plus mr-1"></i> [ + Vincular a Puntos Pizza ]</button>';
                    htmlInscribir += '</div>';
                    $("#franjaFidelizacion").html(htmlInscribir).removeClass("border-primary border-danger border-secondary").addClass("border-info").slideDown(150);

                    $("#btnInscribirFidelizacion").off("click").on("click", function () {
                        inscribirClienteFidelizacion(correo);
                    });
                }
            });
        }
    }).fail(function () {
        $("#franjaFidelizacion").slideUp(150);
    });
}

function inscribirClienteFidelizacion(correo) {
    if (!correo) return;
    $("#btnInscribirFidelizacion").prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-1'></i> Vinculando...");
    $.getJSON("ServiciosClienteFidelizacion?idoperacion=2&correo=" + encodeURIComponent(correo) + "&canal=POS", function (resp) {
        if (resp && resp.respuesta) {
            alert("🎉 ¡El cliente fue ingresado exitosamente al programa de fidelización Puntos Pizza!");
            consultarFidelizacionCliente(correo);
        } else {
            alert("No fue posible vincular al cliente al programa de fidelización. Verifique el correo ingresado.");
            $("#btnInscribirFidelizacion").prop("disabled", false).html("<i class='fas fa-user-plus mr-1'></i> [ + Vincular a Puntos Pizza ]");
        }
    }).fail(function () {
        alert("Error de conexión al intentar vincular al cliente a fidelización.");
        $("#btnInscribirFidelizacion").prop("disabled", false).html("<i class='fas fa-user-plus mr-1'></i> [ + Vincular a Puntos Pizza ]");
    });
}

function fechaVenceLegible(fecha) {
    if (!fecha || fecha.length < 10) return '';
    var meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    var mes = parseInt(fecha.substring(5, 7), 10);
    var dia = parseInt(fecha.substring(8, 10), 10);
    if (isNaN(mes) || isNaN(dia) || mes < 1 || mes > 12) return '';
    return dia + ' de ' + meses[mes - 1];
}

/* =========================================================================
   CONTROL DE BENEFICIO - PRECIO EMPLEADO (NÓMINA & VERIFICACIÓN OTP)
   ========================================================================= */
function initValidacionPrecioEmpleado() {
    $("#btnRemoverPrecioEmpleado").on("click", function (e) {
        e.preventDefault();
        if (confirm("¿Desea quitar el beneficio de Precio Empleado de este pedido? Se removerán los productos a precio empleado de la comanda.")) {
            draftLineas = draftLineas.filter(function (l) { return !l.esEmpleado; });
            empleadoPrecioEmpleadoValidado = null;
            actualizarBadgePrecioEmpleado();
            cotizarEnVivo();
        }
    });

    $("#txtBuscarEmpleadoPrecioEmp").on("input keyup", function () {
        var q = $(this).val().trim();
        if (timerBusquedaEmpleado) clearTimeout(timerBusquedaEmpleado);
        if (q.length < 2) {
            $("#listaResultadosEmpleados").hide().empty();
            return;
        }
        timerBusquedaEmpleado = setTimeout(function () {
            buscarEmpleadosNomina(q);
        }, 280);
    });

    $("#btnCambiarEmpleadoPrecioEmp").on("click", function () {
        empleadoSeleccionadoTemporal = null;
        $("#tarjetaEmpleadoSeleccionado").hide();
        $("#seccionEnvioCodigoPrecioEmp").hide();
        $("#seccionValidarCodigoPrecioEmp").hide();
        $("#alertaPrecioEmpleado").hide().empty();
        $("#seccionBusquedaEmpleado").show();
        $("#txtBuscarEmpleadoPrecioEmp").val("").focus();
    });

    $("#btnEnviarCodigoPrecioEmp, #linkReenviarCodigoPrecioEmp").on("click", function (e) {
        e.preventDefault();
        enviarCodigoPrecioEmpleado();
    });

    $("#btnValidarCodigoPrecioEmp").on("click", function () {
        validarCodigoPrecioEmpleado();
    });

    $("#txtCodigoVerifPrecioEmp").on("keypress", function (e) {
        if (e.which === 13) {
            validarCodigoPrecioEmpleado();
        }
    });

    $("#modalValidacionPrecioEmpleado").on("hidden.bs.modal", function () {
        if (!empleadoPrecioEmpleadoValidado) {
            productoEmpleadoPendienteId = null;
        }
    });
}

function actualizarBadgePrecioEmpleado() {
    if (empleadoPrecioEmpleadoValidado) {
        $("#txtDetallePrecioEmpleadoActivo").text(
            "Precio Empleado: " + empleadoPrecioEmpleadoValidado.nombre + " (CC: " + empleadoPrecioEmpleadoValidado.cedula + ") | Cód: " + empleadoPrecioEmpleadoValidado.codigo
        );
        $("#badgePrecioEmpleadoActivo").css("display", "flex");
    } else {
        $("#badgePrecioEmpleadoActivo").hide();
    }
}

function abrirModalValidacionPrecioEmpleado() {
    empleadoSeleccionadoTemporal = null;
    $("#txtBuscarEmpleadoPrecioEmp").val("");
    $("#listaResultadosEmpleados").hide().empty();
    $("#tarjetaEmpleadoSeleccionado").hide();
    $("#seccionEnvioCodigoPrecioEmp").hide();
    $("#seccionValidarCodigoPrecioEmp").hide();
    $("#txtCodigoVerifPrecioEmp").val("");
    $("#alertaPrecioEmpleado").hide().empty();
    $("#seccionBusquedaEmpleado").show();
    $("#modalValidacionPrecioEmpleado").modal("show");
    setTimeout(function () {
        $("#txtBuscarEmpleadoPrecioEmp").focus();
    }, 400);
}

function buscarEmpleadosNomina(q) {
    $("#listaResultadosEmpleados").html("<div class='p-2 text-center text-muted small'><i class='fas fa-spinner fa-spin mr-1'></i> Buscando en nómina activa...</div>").show();
    $.getJSON("BuscarEmpleadosPrecioEmpleado?q=" + encodeURIComponent(q), function (empleados) {
        var $l = $("#listaResultadosEmpleados");
        $l.empty();
        if (!empleados || empleados.length === 0) {
            $l.html("<div class='p-2 text-muted small text-center'><i class='fas fa-info-circle mr-1'></i> No se encontraron colaboradores activos con ese criterio.</div>");
            return;
        }
        $.each(empleados, function (i, emp) {
            var $item = $("<a href='javascript:void(0)' class='list-group-item list-group-item-action py-2 px-3'></a>");
            $item.append("<div class='d-flex justify-content-between align-items-center'><strong class='text-dark'>" + emp.nombre + "</strong><span class='badge badge-secondary'>CC: " + emp.cedula + "</span></div>");
            var emailTxt = emp.tieneEmail
                ? ("<span class='text-muted small'><i class='fas fa-envelope mr-1 text-primary'></i>" + emp.emailEnmascarado + "</span>")
                : "<span class='text-danger small font-weight-bold'><i class='fas fa-exclamation-triangle mr-1'></i>Sin correo registrado en nómina</span>";
            $item.append("<div>" + emailTxt + "</div>");
            $item.on("click", function () {
                seleccionarEmpleadoNomina(emp);
            });
            $l.append($item);
        });
    }).fail(function () {
        $("#listaResultadosEmpleados").html("<div class='p-2 text-danger small text-center'><i class='fas fa-exclamation-triangle mr-1'></i> Error al consultar base de nómina.</div>");
    });
}

function seleccionarEmpleadoNomina(emp) {
    empleadoSeleccionadoTemporal = emp;
    $("#listaResultadosEmpleados").hide().empty();
    $("#seccionBusquedaEmpleado").hide();
    $("#txtNombreEmpleadoSel").text(emp.nombre);
    $("#txtCedulaEmpleadoSel").text(emp.cedula);
    $("#txtEmailEmpleadoSel").text(emp.emailEnmascarado || "Sin correo");
    $("#badgeEstadoSemanaEmpleado").html("<span class='text-muted small'><i class='fas fa-spinner fa-spin mr-1'></i> Verificando frecuencia semanal...</span>");
    $("#tarjetaEmpleadoSeleccionado").show();
    $("#alertaPrecioEmpleado").hide().empty();

    // Validar frecuencia semanal de lunes a domingo
    $.getJSON("api/precio-empleado?accion=frecuencia&idempleado=" + emp.id, function (resp) {
        if (resp.exito) {
            if (resp.yaPidio) {
                $("#badgeEstadoSemanaEmpleado").html("<span class='badge badge-danger p-1'><i class='fas fa-ban mr-1'></i> Límite semanal alcanzado: Ya redimió esta semana</span>");
                $("#seccionEnvioCodigoPrecioEmp").hide();
                mostrarAlertaPrecioEmp("danger", "⛔ <b>No es posible redimir:</b> El colaborador <b>" + emp.nombre + "</b> ya pidió su pizza de precio empleado esta semana (límite: 1 pizza por semana).");
            } else {
                $("#badgeEstadoSemanaEmpleado").html("<span class='badge badge-success p-1'><i class='fas fa-check-circle mr-1'></i> Habilitado para redimir esta semana</span>");
                if (!emp.tieneEmail) {
                    $("#seccionEnvioCodigoPrecioEmp").hide();
                    mostrarAlertaPrecioEmp("warning", "⚠️ <b>Atención:</b> Este colaborador no tiene correo electrónico registrado en nómina. Para redimir el beneficio a través del Contact Center, Talento Humano debe registrar su correo en nómina.");
                } else {
                    $("#seccionEnvioCodigoPrecioEmp").show();
                    $("#alertaPrecioEmpleado").hide();
                }
            }
        } else {
            $("#badgeEstadoSemanaEmpleado").html("<span class='badge badge-warning p-1'>Error verificando frecuencia</span>");
        }
    }).fail(function () {
        $("#badgeEstadoSemanaEmpleado").html("<span class='badge badge-danger p-1'>Error de conexión</span>");
    });
}

function enviarCodigoPrecioEmpleado() {
    if (!empleadoSeleccionadoTemporal) return;
    var $btn = $("#btnEnviarCodigoPrecioEmp");
    $btn.prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-1'></i> Generando y enviando código OTP...");
    $("#alertaPrecioEmpleado").hide();

    var tiendaId = idTiendaSeleccionada() || 1;

    $.ajax({
        type: "POST",
        url: "api/precio-empleado",
        data: {
            accion: "enviar",
            idempleado: empleadoSeleccionadoTemporal.id,
            idtienda: tiendaId,
            valor: 0
        },
        dataType: "json",
        success: function (resp) {
            $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo de Nómina");
            if (resp.exito) {
                mostrarAlertaPrecioEmp("success", "📨 <b>¡Código generado y enviado!</b> Se envió un correo a <u>" + (resp.emailEnmascarado || empleadoSeleccionadoTemporal.emailEnmascarado) + "</u>. Pídele al colaborador que revise su buzón y te brinde el código.");
                $("#seccionValidarCodigoPrecioEmp").show();
                $("#txtCodigoVerifPrecioEmp").val("").focus();
            } else {
                mostrarAlertaPrecioEmp("danger", "❌ Error: " + (resp.mensaje || "No fue posible enviar el código."));
            }
        },
        error: function (xhr) {
            $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo de Nómina");
            mostrarAlertaPrecioEmp("danger", "❌ Error de conexión al generar código: " + (xhr.statusText || "Error de red"));
        }
    });
}

function validarCodigoPrecioEmpleado() {
    if (!empleadoSeleccionadoTemporal) return;
    var codigo = ($("#txtCodigoVerifPrecioEmp").val() || "").trim().toUpperCase();
    if (!codigo) {
        mostrarAlertaPrecioEmp("warning", "Por favor ingresa el código de verificación dictado por el colaborador.");
        $("#txtCodigoVerifPrecioEmp").focus();
        return;
    }

    var $btn = $("#btnValidarCodigoPrecioEmp");
    $btn.prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-1'></i> Validando código...");

    $.ajax({
        type: "POST",
        url: "api/precio-empleado",
        data: {
            accion: "validar",
            idempleado: empleadoSeleccionadoTemporal.id,
            codigo: codigo
        },
        dataType: "json",
        success: function (resp) {
            $btn.prop("disabled", false).html("<i class='fas fa-check-circle mr-1'></i> Validar Código y Autorizar");
            if (resp.exito && resp.valido) {
                empleadoPrecioEmpleadoValidado = {
                    id: empleadoSeleccionadoTemporal.id,
                    cedula: empleadoSeleccionadoTemporal.cedula,
                    nombre: empleadoSeleccionadoTemporal.nombre,
                    email: empleadoSeleccionadoTemporal.email,
                    emailEnmascarado: empleadoSeleccionadoTemporal.emailEnmascarado,
                    codigo: resp.codigo
                };

                actualizarBadgePrecioEmpleado();

                $("#modalValidacionPrecioEmpleado").one("hidden.bs.modal", function () {
                    alert("🎉 ¡Código validado exitosamente!\n\nBeneficiario: " + empleadoPrecioEmpleadoValidado.nombre + "\nCédula: " + empleadoPrecioEmpleadoValidado.cedula + "\n\nAhora puedes personalizar la pizza del colaborador.");

                    if (productoEmpleadoPendienteId) {
                        var pid = productoEmpleadoPendienteId;
                        productoEmpleadoPendienteId = null;
                        abrirModalProducto(pid);
                    }
                });

                $("#modalValidacionPrecioEmpleado").modal("hide");
            } else {
                mostrarAlertaPrecioEmp("danger", "❌ " + (resp.mensaje || "Código inválido."));
                $("#txtCodigoVerifPrecioEmp").focus();
            }
        },
        error: function (xhr) {
            $btn.prop("disabled", false).html("<i class='fas fa-check-circle mr-1'></i> Validar Código y Autorizar");
            mostrarAlertaPrecioEmp("danger", "❌ Error al validar código: " + (xhr.statusText || "Error de red"));
        }
    });
}

function mostrarAlertaPrecioEmp(tipo, html) {
    var $a = $("#alertaPrecioEmpleado");
    $a.removeClass("alert-info alert-success alert-danger alert-warning")
        .addClass("alert-" + tipo)
        .html(html)
        .show();
}

/* =========================================================================
   GESTIÓN DE REDENCIÓN Y REVERSA DE PUNTOS PIZZA (FIDELIZACIÓN)
   ========================================================================= */
function initGestionPuntosPizza() {
    $("#btnTriggerRedimirPuntos").on("click", function (e) {
        e.preventDefault();
        abrirModalRedencionPuntos();
    });

    $("#btnEnviarCodigoRedencionPuntos").on("click", function (e) {
        e.preventDefault();
        enviarCodigoRedencionPuntos();
    });

    $("#btnValidarYConfirmarCodigoPuntos").on("click", function (e) {
        e.preventDefault();
        validarYConfirmarCodigoRedencionPuntos();
    });

    $("#linkReenviarCodigoPuntos").on("click", function (e) {
        e.preventDefault();
        enviarCodigoRedencionPuntos();
    });

    $("#txtCodigoVerifPuntos").on("keypress", function (e) {
        if (e.which === 13) {
            e.preventDefault();
            validarYConfirmarCodigoRedencionPuntos();
        }
    });
}

function calcularPuntosRequeridosComanda() {
    var pts = 0;
    if (draftLineas && draftLineas.length > 0) {
        for (var i = 0; i < draftLineas.length; i++) {
            pts += ((draftLineas[i].precioPuntos || 0) * (draftLineas[i].cantidad || 1));
        }
    }
    return Math.round(pts);
}

function actualizarCajaPuntosComanda(totalPuntosReq) {
    if (totalPuntosReq > 0) {
        $("#boxPuntosPizzaComanda").show();
        var faltantes = Math.max(0, totalPuntosReq - puntosRedimidos);
        if (puntosRedimidos >= totalPuntosReq) {
            $("#badgePuntosRequeridosComanda")
                .removeClass("badge-warning")
                .addClass("badge-success")
                .html("<i class='fas fa-check mr-1'></i>" + totalPuntosReq + " pts (Redimidos)");
            $("#txtEstadoPuntosComanda").html(
                "<span class='text-success font-weight-bold'><i class='fas fa-check-circle mr-1'></i> Redención confirmada en BD (" + puntosRedimidos + " pts)</span>"
            );
            $("#btnTriggerRedimirPuntos")
                .removeClass("btn-warning")
                .addClass("btn-outline-success")
                .html("<i class='fas fa-check mr-1'></i> Redimido");
        } else {
            $("#badgePuntosRequeridosComanda")
                .removeClass("badge-success")
                .addClass("badge-warning")
                .text(totalPuntosReq + " pts");
            $("#txtEstadoPuntosComanda").html(
                "<span class='text-danger font-weight-bold'><i class='fas fa-exclamation-triangle mr-1'></i> Pendiente (" + faltantes + " pts por redimir)</span>"
            );
            $("#btnTriggerRedimirPuntos")
                .removeClass("btn-outline-success")
                .addClass("btn-warning")
                .html("<i class='fas fa-coins mr-1'></i> Redimir Puntos");
        }
    } else {
        $("#boxPuntosPizzaComanda").hide();
        // Si no quedan productos de puntos pero se habían redimido puntos, reversarlos automáticamente
        if (idsRedencionPedido && idsRedencionPedido.length > 0) {
            reversarRedencionesActivas("Se eliminaron los productos de puntos de la comanda");
            alert("ℹ️ Se eliminaron los productos de puntos de la comanda. Los puntos redimidos fueron devueltos automáticamente al saldo del cliente.");
        }
    }
}

function abrirModalRedencionPuntos() {
    var totalReq = calcularPuntosRequeridosComanda();
    if (totalReq <= 0) {
        alert("La comanda actual no contiene productos que requieran redención de Puntos Pizza.");
        return;
    }

    var emailCli = (clienteActual && clienteActual.email ? clienteActual.email.trim() : "") || ($("#cliEmail").val() || "").trim();
    if (!emailCli) {
        alert("⚠️ ATENCIÓN: El cliente debe tener un correo electrónico registrado para enviarle el código de verificación de puntos.\n\nPor favor digite el correo en la casilla 'Correo Electrónico' del cliente.");
        $("#cliEmail").focus();
        return;
    }

    var pendientes = Math.max(0, totalReq - puntosRedimidos);
    $("#lblPuntosReqModal").text(totalReq + " pts");
    $("#lblPuntosYaRedimidosModal").text(puntosRedimidos + " pts");
    $("#lblPuntosPendientesModal").text(pendientes + " pts");
    $("#txtCorreoRedencionPuntos").val(emailCli);
    $("#txtCodigoVerifPuntos").val("");
    $("#seccionValidarCodigoPuntos").hide();
    $("#seccionEnvioCodigoPuntos").show();
    $("#alertaRedencionPuntosModal").hide().empty();
    $("#lblPuntosDisponiblesModal").html("<i class='fas fa-spinner fa-spin mr-1'></i> Consultando...");
    $("#btnEnviarCodigoRedencionPuntos").prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo");

    // Consultar saldo actual del cliente
    $.getJSON("ValidarRedencionPuntos?correo=" + encodeURIComponent(emailCli), function (data) {
        if (data && data.respuesta === "OK") {
            var saldo = Math.round(parseFloat(data.puntos) || 0);
            $("#lblPuntosDisponiblesModal").text(saldo + " pts");
            if (saldo < pendientes) {
                mostrarAlertaPuntosModal("danger", "⛔ <b>Saldo insuficiente:</b> El cliente tiene <b>" + saldo + " pts</b> y el pedido requiere redimir <b>" + pendientes + " pts</b>.");
                $("#btnEnviarCodigoRedencionPuntos").prop("disabled", true);
            }
        } else {
            $("#lblPuntosDisponiblesModal").text("No vinculado");
            mostrarAlertaPuntosModal("warning", "⚠️ <b>Cliente no vinculado:</b> Este correo no se encuentra registrado en el programa de Puntos Pizza. Utilice el botón «[ + Vincular a Puntos Pizza ]» en la franja superior.");
            $("#btnEnviarCodigoRedencionPuntos").prop("disabled", true);
        }
    }).fail(function () {
        $("#lblPuntosDisponiblesModal").text("Error consultando saldo");
    });

    $("#modalRedimirPuntosPOS").modal("show");
}

function enviarCodigoRedencionPuntos() {
    var emailCli = $("#txtCorreoRedencionPuntos").val().trim();
    if (!emailCli) {
        mostrarAlertaPuntosModal("danger", "Ingrese un correo electrónico válido.");
        return;
    }

    var totalReq = calcularPuntosRequeridosComanda();
    var puntosARedimir = Math.max(0, totalReq - puntosRedimidos);
    if (puntosARedimir <= 0) {
        mostrarAlertaPuntosModal("info", "Todos los puntos de este pedido ya han sido redimidos.");
        return;
    }

    var $btn = $("#btnEnviarCodigoRedencionPuntos");
    $btn.prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-2'></i> Enviando código...");
    $("#alertaRedencionPuntosModal").hide();

    // 1. Validar saldo
    $.getJSON("ValidarRedencionPuntos?correo=" + encodeURIComponent(emailCli), function (data1) {
        if (!data1 || data1.respuesta !== "OK") {
            $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo");
            mostrarAlertaPuntosModal("danger", "El cliente no está registrado en el programa de fidelización.");
            return;
        }

        var saldo = Math.round(parseFloat(data1.puntos) || 0);
        if (saldo < puntosARedimir) {
            $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo");
            mostrarAlertaPuntosModal("danger", "Saldo insuficiente: el cliente tiene " + saldo + " pts y requiere " + puntosARedimir + " pts.");
            return;
        }

        // 2. Generar y enviar OTP al correo
        var fechaBD = obtenerFechaFormatoBD();
        var idTienda = idTiendaSeleccionada() || 1;
        $.getJSON("CrearCodigoValidarRedencionPuntos?correo=" + encodeURIComponent(emailCli)
            + "&puntosredimir=" + puntosARedimir
            + "&fechasistema=" + encodeURIComponent(fechaBD)
            + "&idtienda=" + idTienda, function (data2) {
                if (data2) {
                    mostrarAlertaPuntosModal("success", "✅ <b>Código enviado:</b> Se ha enviado el código de verificación al correo <b>" + emailCli + "</b>. Solicítelo al cliente e ingréselo a continuación.");
                    $("#seccionValidarCodigoPuntos").slideDown();
                    $("#txtCodigoVerifPuntos").val("").focus();
                    $btn.html("<i class='fas fa-check mr-2'></i> Código Despachado");
                } else {
                    $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo");
                    mostrarAlertaPuntosModal("danger", "No se pudo generar el código de verificación. Intente nuevamente.");
                }
            }).fail(function (xhr) {
                $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo");
                mostrarAlertaPuntosModal("danger", "Error de comunicación al generar código: " + (xhr.statusText || "Error de red"));
            });
    }).fail(function (xhr) {
        $btn.prop("disabled", false).html("<i class='fas fa-paper-plane mr-2'></i> Generar y Enviar Código al Correo");
        mostrarAlertaPuntosModal("danger", "Error validando saldo del cliente: " + (xhr.statusText || "Error de red"));
    });
}

function validarYConfirmarCodigoRedencionPuntos() {
    var codigo = $("#txtCodigoVerifPuntos").val().trim();
    if (!codigo) {
        mostrarAlertaPuntosModal("warning", "Debe ingresar el código de verificación para continuar.");
        $("#txtCodigoVerifPuntos").focus();
        return;
    }

    var emailCli = $("#txtCorreoRedencionPuntos").val().trim();
    var fechaBD = obtenerFechaFormatoBD();
    var idTienda = idTiendaSeleccionada() || 1;
    var totalReq = calcularPuntosRequeridosComanda();
    var puntosARedimir = Math.max(0, totalReq - puntosRedimidos);

    var $btn = $("#btnValidarYConfirmarCodigoPuntos");
    $btn.prop("disabled", true).html("<i class='fas fa-spinner fa-spin mr-1'></i> Validando código...");
    $("#alertaRedencionPuntosModal").hide();

    // 1. Validar existencia del código
    $.getJSON("ValidarExistenciaCodigoRedencion?correo=" + encodeURIComponent(emailCli)
        + "&codigo=" + encodeURIComponent(codigo)
        + "&fechasistema=" + encodeURIComponent(fechaBD), function (data1) {
            if (data1 && data1.respuesta) {
                // Código válido: realizar la redención en base de datos
                $btn.html("<i class='fas fa-spinner fa-spin mr-1'></i> Confirmando redención...");
                $.getJSON("RealizarRedencionPuntos?correo=" + encodeURIComponent(emailCli)
                    + "&codigo=" + encodeURIComponent(codigo)
                    + "&puntosredimir=" + puntosARedimir
                    + "&idtienda=" + idTienda
                    + "&idpedido=0", function (data2) {
                        $btn.prop("disabled", false).html("<i class='fas fa-check-circle mr-1'></i> Validar Código y Confirmar Redención");
                        if (data2 && data2.respuesta === "OK") {
                            idsRedencionPedido.push(data2.idredencion);
                            puntosRedimidos += puntosARedimir;

                            $("#modalRedimirPuntosPOS").modal("hide");
                            var ptsRest = (data2.puntosrestantes != null) ? Math.round(data2.puntosrestantes) : "-";
                            alert("🎉 ¡REDENCIÓN DE PUNTOS EXITOSA!\n\n"
                                + "• Puntos redimidos: " + puntosARedimir + " pts\n"
                                + "• ID Transacción: #" + data2.idredencion + "\n"
                                + "• Saldo restante del cliente: " + ptsRest + " pts\n\n"
                                + "Ya puede continuar con el despacho del pedido.");

                            actualizarCajaPuntosComanda(calcularPuntosRequeridosComanda());
                            cotizarEnVivo();
                        } else {
                            var det = (data2 && data2.detalle) ? ("<br>" + data2.detalle) : "";
                            mostrarAlertaPuntosModal("danger", "⛔ No se pudo finalizar la redención: " + det);
                        }
                    }).fail(function (xhr) {
                        $btn.prop("disabled", false).html("<i class='fas fa-check-circle mr-1'></i> Validar Código y Confirmar Redención");
                        mostrarAlertaPuntosModal("danger", "Error de red al aplicar redención: " + (xhr.statusText || "Error"));
                    });
            } else {
                $btn.prop("disabled", false).html("<i class='fas fa-check-circle mr-1'></i> Validar Código y Confirmar Redención");
                mostrarAlertaPuntosModal("danger", "❌ Código de verificación incorrecto, ya usado o vencido.");
                $("#txtCodigoVerifPuntos").focus();
            }
        }).fail(function (xhr) {
            $btn.prop("disabled", false).html("<i class='fas fa-check-circle mr-1'></i> Validar Código y Confirmar Redención");
            mostrarAlertaPuntosModal("danger", "Error al comprobar código de verificación: " + (xhr.statusText || "Error"));
        });
}

function reversarRedencionesActivas(motivo) {
    if (!idsRedencionPedido || idsRedencionPedido.length === 0) return;
    var mot = motivo || "Pedido cancelado o reiniciado";
    for (var i = 0; i < idsRedencionPedido.length; i++) {
        $.ajax({
            url: "ReversarRedencionPedido?idredencion=" + idsRedencionPedido[i] + "&puntos=0&motivo=" + encodeURIComponent(mot),
            dataType: "json",
            async: false,
            success: function (d) {
                console.log("Redención #" + idsRedencionPedido[i] + " reversada exitosamente:", d);
            },
            error: function (e) {
                console.error("Error reversando redención #" + idsRedencionPedido[i] + ":", e);
            }
        });
    }
    idsRedencionPedido = [];
    puntosRedimidos = 0;
}

function mostrarAlertaPuntosModal(tipo, html) {
    var $a = $("#alertaRedencionPuntosModal");
    $a.removeClass("alert-info alert-success alert-danger alert-warning")
        .addClass("alert-" + tipo)
        .html(html)
        .show();
}

function obtenerFechaFormatoBD() {
    var date = new Date();
    var year = date.getFullYear();
    var month = String(date.getMonth() + 1).padStart(2, '0');
    var day = String(date.getDate()).padStart(2, '0');
    return (year + "/" + month + "/" + day);
}

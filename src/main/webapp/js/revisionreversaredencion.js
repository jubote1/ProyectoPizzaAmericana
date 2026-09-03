/*
 * Revision de solicitudes de devolucion de puntos de fidelizacion.
 *
 * Aqui llegan las devoluciones que necesitan aprobacion: pedidos que SI se
 * finalizaron y luego se anularon, o a los que se les quito el producto de
 * redencion. No se aprueban solas a proposito, porque el cliente pudo haber
 * recibido el producto y despues pedir la anulacion para recuperar los puntos.
 *
 * Los pedidos que nunca se finalizaron no pasan por esta pantalla: ahi no hubo
 * entrega, la reversa es automatica y no hay nada que decidir.
 */

var tablaSolicitudes = null;
var solicitudesCargadas = [];

function consultarSolicitudes()
{
    var estado = $('#estado').val();
    var fechaDesde = $('#fechadesde').val().trim();
    var fechaHasta = $('#fechahasta').val().trim();

    if(fechaDesde !== '' && fechaHasta !== '' && fechaDesde > fechaHasta)
    {
        $.alert('La fecha desde no puede ser mayor que la fecha hasta.');
        return;
    }

    $('#btnconsultar').prop('disabled', true).val('Consultando...');

    $.getJSON(server + 'ConsultarSolicitudesReversa?estado=' + encodeURIComponent(estado)
            + '&fechadesde=' + encodeURIComponent(fechaDesde)
            + '&fechahasta=' + encodeURIComponent(fechaHasta), function(datos){

        solicitudesCargadas = datos || [];
        pintarSolicitudes(solicitudesCargadas);
        $('#btnconsultar').prop('disabled', false).val('Consultar');

    }).fail(function(){
        $('#btnconsultar').prop('disabled', false).val('Consultar');
        $.alert('No se pudo consultar las solicitudes. Si acaba de iniciar sesion, recargue la pagina.');
    });
}

function pintarSolicitudes(datos)
{
    //DataTables no admite recargar el tbody por debajo: hay que destruirla.
    if(tablaSolicitudes !== null)
    {
        tablaSolicitudes.destroy();
        tablaSolicitudes = null;
    }
    $('#grid-solicitudes tbody').empty();

    if(datos.length === 0)
    {
        $('#panelTotales').hide();
        $('#contenedorTabla').hide();
        $('#mensajeVacio').show();
        return;
    }

    var pendientes = 0;
    var aprobadas = 0;
    var rechazadas = 0;
    var puntosPendientes = 0;
    var filas = '';

    for(var i = 0; i < datos.length; i++)
    {
        var s = datos[i];

        if(s.estado === 'PENDIENTE')
        {
            pendientes++;
            puntosPendientes += Number(s.puntossolicitados);
        }
        else if(s.estado === 'APROBADA') { aprobadas++; }
        else if(s.estado === 'RECHAZADA') { rechazadas++; }

        var claseEstado = 'est-' + String(s.estado).toLowerCase();
        var claseMotivo = (s.motivo === 'ANULACION') ? 'etiqueta-motivo mot-anulacion' : 'etiqueta-motivo';

        //La devolucion parcial se hace explicita: si no es por el total de la
        //redencion, quien aprueba tiene que verlo.
        var puntosTexto = formatearPuntos(s.puntossolicitados);
        if(Number(s.puntossolicitados) < Number(s.puntosredimidos))
        {
            puntosTexto = puntosTexto + ' <small style="font-weight:normal;color:#888;">de '
                + formatearPuntos(s.puntosredimidos) + '</small>';
        }

        var accion = '';
        if(s.estado === 'PENDIENTE')
        {
            accion = '<button class="btn btn-success btn-fila" onclick="resolver(' + s.idsolicitud + ', true)">Aprobar</button> '
                   + '<button class="btn btn-danger btn-fila" onclick="resolver(' + s.idsolicitud + ', false)">Rechazar</button>';
        }
        else
        {
            accion = '<span style="color:#999;">-</span>';
        }

        filas += '<tr>'
            + '<td>' + s.idsolicitud + '</td>'
            + '<td>' + escapar(s.fechasolicitud) + '</td>'
            + '<td>' + escapar(s.tienda) + '</td>'
            + '<td>' + s.idpedidotienda + '</td>'
            + '<td>' + escapar(s.correo) + '</td>'
            + '<td class="puntos">' + puntosTexto + '</td>'
            + '<td><span class="' + claseMotivo + '">' + escapar(s.motivo) + '</span></td>'
            + '<td>' + escapar(s.usuariosolicita) + ' <small style="color:#888;">' + escapar(s.origen) + '</small></td>'
            + '<td><span class="etiqueta-estado ' + claseEstado + '">' + escapar(s.estado) + '</span></td>'
            + '<td>' + escapar(s.usuariorevisa) + '</td>'
            + '<td>' + escapar(s.observacion) + (s.observacionrevision ? '<br><em>' + escapar(s.observacionrevision) + '</em>' : '') + '</td>'
            + '<td>' + accion + '</td>'
            + '</tr>';
    }

    $('#grid-solicitudes tbody').html(filas);

    $('#totPendientes').text(pendientes);
    $('#totPuntosPendientes').text(formatearPuntos(puntosPendientes));
    $('#totAprobadas').text(aprobadas);
    $('#totRechazadas').text(rechazadas);

    $('#mensajeVacio').hide();
    $('#panelTotales').show();
    $('#contenedorTabla').show();

    tablaSolicitudes = $('#grid-solicitudes').DataTable({
        "order": [],
        "pageLength": 25,
        "language": {
            "emptyTable": "Sin solicitudes",
            "info": "Mostrando _START_ a _END_ de _TOTAL_ solicitudes",
            "infoEmpty": "Sin solicitudes",
            "infoFiltered": "(filtrado de _MAX_)",
            "lengthMenu": "Ver _MENU_ registros",
            "search": "Buscar:",
            "zeroRecords": "No hay coincidencias",
            "paginate": { "first": "Primero", "last": "Ultimo", "next": "Siguiente", "previous": "Anterior" }
        }
    });
}

function resolver(idSolicitud, aprobar)
{
    var s = buscarSolicitud(idSolicitud);
    if(s === null)
    {
        $.alert('No se encontro la solicitud en pantalla, vuelva a consultar.');
        return;
    }

    var titulo = aprobar ? 'Aprobar la devolucion de puntos' : 'Rechazar la devolucion de puntos';
    var resumen = '<strong>Cliente:</strong> ' + escapar(s.correo) + '<br>'
        + '<strong>Tienda:</strong> ' + escapar(s.tienda) + '<br>'
        + '<strong>Pedido:</strong> ' + s.idpedidotienda + '<br>'
        + '<strong>Puntos a devolver:</strong> ' + formatearPuntos(s.puntossolicitados) + '<br>'
        + '<strong>Motivo:</strong> ' + escapar(s.motivo) + '<br>'
        + '<strong>Solicitado por:</strong> ' + escapar(s.usuariosolicita) + '<br><br>';

    var advertencia = '';
    if(aprobar && s.motivo === 'ANULACION')
    {
        //El caso de riesgo se advierte, no se bloquea: la decision es de quien revisa.
        advertencia = '<div class="alert alert-warning" style="padding:8px;font-size:12px;">'
            + 'Este pedido estaba finalizado cuando se anulo, asi que el cliente pudo haber '
            + 'recibido el producto. Verifique antes de devolver los puntos.</div>';
    }

    $.confirm({
        title: titulo,
        content: resumen + advertencia
            + '<label style="font-weight:normal;">Observacion de la revision</label>'
            + '<textarea id="obsRevision" class="form-control" rows="2" maxlength="300" '
            + 'placeholder="Por que se aprueba o se rechaza"></textarea>',
        type: aprobar ? 'green' : 'red',
        buttons: {
            confirmar: {
                text: aprobar ? 'Aprobar y devolver' : 'Rechazar',
                btnClass: aprobar ? 'btn-success' : 'btn-danger',
                action: function(){
                    var observacion = $('#obsRevision').val();
                    if(!aprobar && String(observacion).trim() === '')
                    {
                        //Rechazar sin explicacion deja al cajero sin saber que paso.
                        $.alert('Para rechazar debe escribir el motivo.');
                        return false;
                    }
                    enviarResolucion(idSolicitud, aprobar, observacion);
                }
            },
            cancelar: { text: 'Cancelar', action: function(){} }
        }
    });
}

function enviarResolucion(idSolicitud, aprobar, observacion)
{
    $.getJSON(server + 'ResolverSolicitudReversa?idsolicitud=' + idSolicitud
            + '&aprobar=' + (aprobar ? 'S' : 'N')
            + '&observacion=' + encodeURIComponent(observacion), function(data){

        if(data.respuesta === 'OK')
        {
            $.alert({
                title: aprobar ? 'Puntos devueltos' : 'Solicitud rechazada',
                content: aprobar
                    ? 'Los puntos volvieron al cliente y quedo registrado que usted lo autorizo.'
                    : 'La solicitud quedo rechazada. Los puntos no se devolvieron.',
                type: aprobar ? 'green' : 'orange'
            });
            consultarSolicitudes();
        }
        else
        {
            $.alert('No se pudo procesar la solicitud.<br><br>' + escapar(data.detalle || ''));
            consultarSolicitudes();
        }

    }).fail(function(){
        $.alert('No hubo respuesta del servidor. Vuelva a consultar antes de reintentar, '
            + 'para no aprobar dos veces la misma solicitud.');
    });
}

function buscarSolicitud(idSolicitud)
{
    for(var i = 0; i < solicitudesCargadas.length; i++)
    {
        if(Number(solicitudesCargadas[i].idsolicitud) === Number(idSolicitud))
        {
            return(solicitudesCargadas[i]);
        }
    }
    return(null);
}

function exportarSolicitudes()
{
    if(solicitudesCargadas.length === 0)
    {
        $.alert('No hay nada que exportar.');
        return;
    }
    //El BOM es para que Excel en Windows abra el archivo con los acentos bien.
    var csv = '﻿';
    csv += 'Solicitud;Fecha;Tienda;Pedido;Cliente;PuntosSolicitados;PuntosRedimidos;Motivo;'
        + 'Solicito;Origen;Estado;Reviso;FechaRevision;Observacion;ObservacionRevision\n';
    for(var i = 0; i < solicitudesCargadas.length; i++)
    {
        var s = solicitudesCargadas[i];
        csv += [ s.idsolicitud, s.fechasolicitud, s.tienda, s.idpedidotienda, s.correo,
                 s.puntossolicitados, s.puntosredimidos, s.motivo, s.usuariosolicita, s.origen,
                 s.estado, s.usuariorevisa, s.fecharevision,
                 limpiarCsv(s.observacion), limpiarCsv(s.observacionrevision) ].join(';') + '\n';
    }
    var enlace = document.createElement('a');
    enlace.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    enlace.download = 'devolucion_puntos.csv';
    document.body.appendChild(enlace);
    enlace.click();
    document.body.removeChild(enlace);
}

function limpiarCsv(texto)
{
    //El separador es punto y coma, asi que hay que sacarlo del contenido.
    return(String(texto || '').replace(/;/g, ',').replace(/[\r\n]+/g, ' '));
}

function formatearPuntos(valor)
{
    var n = Number(valor);
    if(isNaN(n)) { return('0'); }
    //Los puntos son double pero en la practica son enteros; se muestran decimales
    //solo cuando de verdad los hay.
    return((n % 1 === 0) ? String(n) : n.toFixed(2));
}

function escapar(texto)
{
    //El correo, la observacion y el usuario vienen de captura libre y se pintan
    //dentro del HTML de la tabla.
    return(String(texto === null || texto === undefined ? '' : texto)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;'));
}

function abrirAyuda()
{
    window.open('AyudaRevisionReversaRedencion.html', 'AyudaRevisionReversa',
        'width=1000,height=760,scrollbars=yes,resizable=yes');
}

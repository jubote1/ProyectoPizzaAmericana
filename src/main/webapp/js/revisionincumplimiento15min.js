/*
 * Revision de incumplimientos de la campana "15 minutos o gratis".
 *
 * Llegan aqui los pedidos de punto de venta donde la campana aplico y el
 * job de deteccion en Servicios encontro que se paso el tiempo prometido.
 * Al aprobar, el sistema NO mueve dinero: solo deja el caso con el monto ya
 * calculado para que caja/Servicio al Cliente ejecute la devolucion real.
 */

var incumplimientosCargados = [];

function consultarIncumplimientos()
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

    $.getJSON(server + 'ConsultarIncumplimientos15Min?estado=' + encodeURIComponent(estado)
            + '&fechadesde=' + encodeURIComponent(fechaDesde)
            + '&fechahasta=' + encodeURIComponent(fechaHasta), function(datos){

        incumplimientosCargados = datos || [];
        pintarIncumplimientos(incumplimientosCargados);
        $('#btnconsultar').prop('disabled', false).val('Consultar');

    }).fail(function(){
        $('#btnconsultar').prop('disabled', false).val('Consultar');
        $.alert('No se pudo consultar los casos. Si acaba de iniciar sesion, recargue la pagina.');
    });
}

function pintarIncumplimientos(datos)
{
    $('#grid-incumplimientos tbody').empty();

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
    var valorPendiente = 0;
    var filas = '';

    for(var i = 0; i < datos.length; i++)
    {
        var s = datos[i];

        if(s.estado === 'PENDIENTE')
        {
            pendientes++;
            valorPendiente += Number(s.valoradevolver);
        }
        else if(s.estado === 'APROBADA') { aprobadas++; }
        else if(s.estado === 'RECHAZADA') { rechazadas++; }

        var claseEstado = 'est-' + String(s.estado).toLowerCase();

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
            + '<td>' + escapar(s.fechadeteccion) + '</td>'
            + '<td>' + escapar(s.tienda) + '</td>'
            + '<td>' + s.idpedidotienda + '</td>'
            + '<td>' + escapar(s.fechahorainicio) + '</td>'
            + '<td class="monto">' + formatearMonto(s.valorbasepizza) + '</td>'
            + '<td class="monto">' + formatearMonto(s.retencionaplicada) + '</td>'
            + '<td class="monto">' + formatearMonto(s.valoradevolver) + '</td>'
            + '<td><span class="etiqueta-estado ' + claseEstado + '">' + escapar(s.estado) + '</span></td>'
            + '<td>' + escapar(s.usuariorevisa) + '</td>'
            + '<td>' + (s.observacionrevision ? '<em>' + escapar(s.observacionrevision) + '</em>' : '') + '</td>'
            + '<td>' + accion + '</td>'
            + '</tr>';
    }

    $('#grid-incumplimientos tbody').html(filas);

    $('#totPendientes').text(pendientes);
    $('#totValorPendiente').text(formatearMonto(valorPendiente));
    $('#totAprobadas').text(aprobadas);
    $('#totRechazadas').text(rechazadas);

    $('#mensajeVacio').hide();
    $('#panelTotales').show();
    $('#contenedorTabla').show();

    if($.fn.DataTable.isDataTable('#grid-incumplimientos'))
    {
        $('#grid-incumplimientos').DataTable().destroy();
    }
    $('#grid-incumplimientos').DataTable({
        "order": [],
        "pageLength": 25,
        "language": {
            "emptyTable": "Sin casos",
            "info": "Mostrando _START_ a _END_ de _TOTAL_ casos",
            "infoEmpty": "Sin casos",
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
    var s = buscarCaso(idSolicitud);
    if(s === null)
    {
        $.alert('No se encontro el caso en pantalla, vuelva a consultar.');
        return;
    }

    var titulo = aprobar ? 'Aprobar la devolucion' : 'Rechazar el caso';
    var resumen = '<strong>Tienda:</strong> ' + escapar(s.tienda) + '<br>'
        + '<strong>Pedido:</strong> ' + s.idpedidotienda + '<br>'
        + '<strong>Hora de inicio:</strong> ' + escapar(s.fechahorainicio) + '<br>'
        + '<strong>Valor a devolver:</strong> ' + formatearMonto(s.valoradevolver) + '<br><br>';

    var advertencia = '';
    if(aprobar)
    {
        advertencia = '<div class="alert alert-warning" style="padding:8px;font-size:12px;">'
            + 'Al aprobar, el caso queda listo con el monto calculado. La devolucion real '
            + '(datafono, QR o efectivo) la debe ejecutar caja/Servicio al Cliente por fuera de este sistema.</div>';
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
                text: aprobar ? 'Aprobar' : 'Rechazar',
                btnClass: aprobar ? 'btn-success' : 'btn-danger',
                action: function(){
                    var observacion = $('#obsRevision').val();
                    if(!aprobar && String(observacion).trim() === '')
                    {
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
    $.getJSON(server + 'ResolverIncumplimiento15Min?idsolicitud=' + idSolicitud
            + '&aprobar=' + (aprobar ? 'S' : 'N')
            + '&observacion=' + encodeURIComponent(observacion), function(data){

        if(data.respuesta === 'OK')
        {
            $.alert({
                title: aprobar ? 'Caso aprobado' : 'Caso rechazado',
                content: aprobar
                    ? 'El caso quedo aprobado con el monto calculado. Recuerde ejecutar la devolucion real.'
                    : 'El caso quedo rechazado.',
                type: aprobar ? 'green' : 'orange'
            });
            consultarIncumplimientos();
        }
        else
        {
            $.alert('No se pudo procesar el caso.<br><br>' + escapar(data.detalle || ''));
            consultarIncumplimientos();
        }

    }).fail(function(){
        $.alert('No hubo respuesta del servidor. Vuelva a consultar antes de reintentar, '
            + 'para no aprobar dos veces el mismo caso.');
    });
}

function buscarCaso(idSolicitud)
{
    for(var i = 0; i < incumplimientosCargados.length; i++)
    {
        if(Number(incumplimientosCargados[i].idsolicitud) === Number(idSolicitud))
        {
            return(incumplimientosCargados[i]);
        }
    }
    return(null);
}

function formatearMonto(valor)
{
    var n = Number(valor);
    if(isNaN(n)) { return('$0'); }
    return('$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 }));
}

function escapar(texto)
{
    return(String(texto === null || texto === undefined ? '' : texto)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;'));
}

function abrirAyuda()
{
    window.open('AyudaRevisionIncumplimiento15Min.html', 'AyudaRevisionIncumplimiento15Min',
        'width=1000,height=760,scrollbars=yes,resizable=yes');
}

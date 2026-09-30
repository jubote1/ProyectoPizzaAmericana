/*
 * Configuracion de la campana "15 minutos o gratis" en punto de venta.
 * Hoy solo se administra una campana (la primera que exista); si el usuario
 * quiere una segunda campana en el futuro, este mismo CRUD ya soporta
 * multiples filas, solo falta un selector en pantalla.
 */

var idCampanaActual = 0;

function cargarConfiguracion()
{
    $.getJSON(server + 'CRUDCampana15MinConfig?idoperacion=4', function(datos)
    {
        if(datos && datos.length > 0)
        {
            pintarConfiguracion(datos[0]);
            cargarExclusiones(datos[0].idcampana);
        }
    }).fail(function(){
        $.alert('No se pudo cargar la configuracion. Si acaba de iniciar sesion, recargue la pagina.');
    });
}

function pintarConfiguracion(c)
{
    idCampanaActual = Number(c.idcampana);
    $('#idcampana').val(c.idcampana);
    $('#nombre').val(c.nombre);
    $('#activo').prop('checked', c.activo === 'S');
    $('#mensajeoperario').val(c.mensajeoperario);
    $('#mensajefactura').val(c.mensajefactura);
    $('#fechadesde').val(c.fechadesde || '');
    $('#fechahasta').val(c.fechahasta || '');
    $('#horadesde').val(c.horadesde || '');
    $('#horahasta').val(c.horahasta || '');
    $('#minutospromesa').val(c.minutospromesa);
    $('#porcentajeretencion').val(c.porcentajeretencionmediovirtual);

    var dias = String(c.diassemana || 'SSSSSSS');
    $('.chk-dia').each(function(){
        var pos = Number($(this).data('pos'));
        $(this).prop('checked', dias.charAt(pos) === 'S');
    });
}

function guardarConfiguracion()
{
    var diasSemana = '';
    $('.chk-dia').each(function(){
        diasSemana += $(this).is(':checked') ? 'S' : 'N';
    });

    var params = $.param({
        idoperacion: 1,
        idcampana: $('#idcampana').val(),
        nombre: $('#nombre').val(),
        activo: $('#activo').is(':checked') ? 'S' : 'N',
        mensajeoperario: $('#mensajeoperario').val(),
        mensajefactura: $('#mensajefactura').val(),
        fechadesde: $('#fechadesde').val(),
        fechahasta: $('#fechahasta').val(),
        diassemana: diasSemana,
        horadesde: $('#horadesde').val(),
        horahasta: $('#horahasta').val(),
        minutospromesa: $('#minutospromesa').val(),
        porcentajeretencionmediovirtual: $('#porcentajeretencion').val()
    });

    $.getJSON(server + 'CRUDCampana15MinConfig?' + params, function(data)
    {
        if(data.respuesta === 'OK')
        {
            idCampanaActual = Number(data.idcampana);
            $('#idcampana').val(data.idcampana);
            $.alert({ title: 'Guardado', content: 'La configuracion quedo guardada.', type: 'green' });
            cargarExclusiones(idCampanaActual);
        }
        else
        {
            $.alert('No se pudo guardar la configuracion.');
        }
    }).fail(function(){
        $.alert('No hubo respuesta del servidor al guardar.');
    });
}

function cargarExclusiones(idCampana)
{
    $.getJSON(server + 'CRUDCampana15MinExclusion?idoperacion=4&idcampana=' + idCampana, function(datos)
    {
        pintarExclusiones(datos || []);
    });
}

function pintarExclusiones(datos)
{
    var html = '';
    if(datos.length === 0)
    {
        html = '<li class="text-muted">Sin exclusiones adicionales.</li>';
    }
    for(var i = 0; i < datos.length; i++)
    {
        var ex = datos[i];
        html += '<li>' + escaparTexto(ex.nombreproducto)
            + ' <button class="btn btn-default btn-xs" onclick="eliminarExclusion(' + ex.idexclusion + ')">Quitar</button></li>';
    }
    $('#listaExclusiones').html(html);
}

function agregarExclusion()
{
    if(idCampanaActual <= 0)
    {
        $.alert('Guarde primero la configuracion antes de agregar exclusiones.');
        return;
    }
    var nombre = $('#nuevaexclusion').val().trim();
    if(nombre === '')
    {
        $.alert('Escriba el nombre exacto de la especialidad o producto.');
        return;
    }
    $.getJSON(server + 'CRUDCampana15MinExclusion?idoperacion=1&idcampana=' + idCampanaActual
            + '&nombreproducto=' + encodeURIComponent(nombre), function(data)
    {
        if(data.respuesta === 'OK')
        {
            $('#nuevaexclusion').val('');
            cargarExclusiones(idCampanaActual);
        }
        else
        {
            $.alert('No se pudo agregar la exclusion.');
        }
    });
}

function eliminarExclusion(idExclusion)
{
    $.getJSON(server + 'CRUDCampana15MinExclusion?idoperacion=3&idexclusion=' + idExclusion, function(data)
    {
        cargarExclusiones(idCampanaActual);
    });
}

function escaparTexto(texto)
{
    return(String(texto === null || texto === undefined ? '' : texto)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;'));
}

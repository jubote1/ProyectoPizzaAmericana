/*
 * Configuracion de la campana "15 minutos o gratis" en punto de venta.
 * Hoy solo se administra una campana (la primera que exista); si el usuario
 * quiere una segunda campana en el futuro, este mismo CRUD ya soporta
 * multiples filas, solo falta un selector en pantalla.
 *
 * El horario NO es uniforme entre semana: cada dia se guarda por separado
 * (campana_15min_horario_dia), "todo el dia" o con una franja horaria.
 */

var idCampanaActual = 0;

var NOMBRES_DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

function cargarConfiguracion()
{
    $.getJSON(server + 'CRUDCampana15MinConfig?idoperacion=4', function(datos)
    {
        if(datos && datos.length > 0)
        {
            pintarConfiguracion(datos[0]);
            cargarExclusiones(datos[0].idcampana);
            cargarHorario(datos[0].idcampana);
        }
        else
        {
            pintarHorarioPorDefecto();
        }
    }).fail(function(){
        $.alert('No se pudo cargar la configuracion. Si acaba de iniciar sesion, recargue la pagina.');
    });
    cargarTiendasDesactivadas();
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
    $('#minutospromesa').val(c.minutospromesa);
    $('#porcentajeretencion').val(c.porcentajeretencionmediovirtual);
}

function guardarConfiguracion()
{
    var params = $.param({
        idoperacion: 1,
        idcampana: $('#idcampana').val(),
        nombre: $('#nombre').val(),
        activo: $('#activo').is(':checked') ? 'S' : 'N',
        mensajeoperario: $('#mensajeoperario').val(),
        mensajefactura: $('#mensajefactura').val(),
        fechadesde: $('#fechadesde').val(),
        fechahasta: $('#fechahasta').val(),
        minutospromesa: $('#minutospromesa').val(),
        porcentajeretencionmediovirtual: $('#porcentajeretencion').val()
    });

    $.getJSON(server + 'CRUDCampana15MinConfig?' + params, function(data)
    {
        if(data.respuesta === 'OK')
        {
            var esNueva = idCampanaActual <= 0;
            idCampanaActual = Number(data.idcampana);
            $('#idcampana').val(data.idcampana);
            $.alert({ title: 'Guardado', content: 'La configuracion quedo guardada.', type: 'green' });
            cargarExclusiones(idCampanaActual);
            if(esNueva)
            {
                pintarHorarioPorDefecto();
            }
        }
        else
        {
            $.alert('No se pudo guardar la configuracion.');
        }
    }).fail(function(){
        $.alert('No hubo respuesta del servidor al guardar.');
    });
}

function cargarHorario(idCampana)
{
    $.getJSON(server + 'CRUDCampana15MinHorario?idoperacion=4&idcampana=' + idCampana, function(datos)
    {
        if(datos && datos.length > 0)
        {
            pintarHorario(datos);
        }
        else
        {
            pintarHorarioPorDefecto();
        }
    });
}

function pintarHorarioPorDefecto()
{
    // Sin datos guardados todavia: todos los dias activos, todo el dia.
    var dias = [];
    for(var d = 1; d <= 7; d++)
    {
        dias.push({ diasemana: d, activo: 'S', todoeldia: 'S', horadesde: '', horahasta: '' });
    }
    pintarHorario(dias);
}

function pintarHorario(dias)
{
    var porDia = {};
    for(var i = 0; i < dias.length; i++)
    {
        porDia[Number(dias[i].diasemana)] = dias[i];
    }

    var filas = '';
    for(var d = 1; d <= 7; d++)
    {
        var h = porDia[d] || { activo: 'S', todoeldia: 'S', horadesde: '', horahasta: '' };
        var todoElDia = h.todoeldia === 'S';
        filas += '<tr data-dia="' + d + '">'
            + '<td><input type="checkbox" class="chk-dia-activo" ' + (h.activo === 'S' ? 'checked' : '') + '></td>'
            + '<td>' + NOMBRES_DIAS[d - 1] + '</td>'
            + '<td><input type="checkbox" class="chk-todo-el-dia" ' + (todoElDia ? 'checked' : '') + ' onchange="alternarHorasDia(this)"></td>'
            + '<td><input type="text" class="form-control input-sm txt-hora-desde" placeholder="hh:mm:ss" value="' + escaparTexto(h.horadesde || '') + '" ' + (todoElDia ? 'disabled' : '') + '></td>'
            + '<td><input type="text" class="form-control input-sm txt-hora-hasta" placeholder="hh:mm:ss" value="' + escaparTexto(h.horahasta || '') + '" ' + (todoElDia ? 'disabled' : '') + '></td>'
            + '</tr>';
    }
    $('#tablaHorario tbody').html(filas);
}

function alternarHorasDia(checkbox)
{
    var fila = $(checkbox).closest('tr');
    var deshabilitar = $(checkbox).is(':checked');
    fila.find('.txt-hora-desde, .txt-hora-hasta').prop('disabled', deshabilitar);
}

function guardarHorario()
{
    if(idCampanaActual <= 0)
    {
        $.alert('Guarde primero la configuracion antes del horario.');
        return;
    }

    var dias = [];
    $('#tablaHorario tbody tr').each(function(){
        var fila = $(this);
        dias.push({
            diasemana: Number(fila.data('dia')),
            activo: fila.find('.chk-dia-activo').is(':checked') ? 'S' : 'N',
            todoeldia: fila.find('.chk-todo-el-dia').is(':checked') ? 'S' : 'N',
            horadesde: fila.find('.txt-hora-desde').val(),
            horahasta: fila.find('.txt-hora-hasta').val()
        });
    });

    $.ajax({
        url: server + 'CRUDCampana15MinHorario?idcampana=' + idCampanaActual,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(dias),
        dataType: 'json',
        success: function(data){
            if(data.respuesta === 'OK')
            {
                $.alert({ title: 'Guardado', content: 'El horario quedo guardado.', type: 'green' });
            }
            else
            {
                $.alert('No se pudo guardar el horario.');
            }
        },
        error: function(){
            $.alert('No hubo respuesta del servidor al guardar el horario.');
        }
    });
}

function cargarTiendasDesactivadas()
{
    $.getJSON(server + 'CRUDCampana15MinConfig?idoperacion=5', function(datos)
    {
        pintarTiendasDesactivadas(datos || []);
    });
}

function pintarTiendasDesactivadas(datos)
{
    if(datos.length === 0)
    {
        $('#tablaDesactivadas tbody').html('');
        $('#sinDesactivadas').show();
        return;
    }
    $('#sinDesactivadas').hide();
    var filas = '';
    for(var i = 0; i < datos.length; i++)
    {
        var d = datos[i];
        filas += '<tr>'
            + '<td>' + escaparTexto(d.tienda) + '</td>'
            + '<td>' + escaparTexto(d.motivo) + '</td>'
            + '<td>' + escaparTexto(d.usuarioautoriza) + '</td>'
            + '<td>' + escaparTexto(d.usuariodesactiva) + '</td>'
            + '<td>' + escaparTexto(d.fechahora) + '</td>'
            + '</tr>';
    }
    $('#tablaDesactivadas tbody').html(filas);
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

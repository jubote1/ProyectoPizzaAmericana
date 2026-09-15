var server;
var table;
var rolesCache = [];
var idUsuarioEditando = 0;

$(document).ready(function() {
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	table = $('#grid-usuarios').DataTable( {
			"aoColumns": [
        { "mData": "idusuario" },
        { "mData": "nombreusuario" },
        { "mData": "nombrelargo" },
        { "mData": "roles" },
        { "mData": "accion", className: "center", defaultContent: '' }
    ]
		} );

	cargarRolesCache();
	pintarUsuarios();
	setInterval('validarVigenciaLogueo()',600000);
});

function validarVigenciaLogueo()
{
	var respuesta ='';
	$.ajax({ url: server + 'ValidarUsuarioAplicacion', dataType: 'json', type: 'post', async: false,
		success: function(data){ respuesta = data[0].respuesta; } });
	if (respuesta != 'OK' && respuesta != 'OKA') { location.href = server + "Index.html"; }
}

function cargarRolesCache()
{
	$.ajax({ url: server + 'GetRoles', dataType: 'json', async: false, success: function(data){ rolesCache = data; } });
}

function pintarUsuarios()
{
	$.getJSON(server + 'GetUsuariosConRoles', function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				table.row.add({
					"idusuario": data1[i].idusuario,
					"nombreusuario": data1[i].nombreusuario,
					"nombrelargo": data1[i].nombrelargo,
					"roles": data1[i].roles.join(', '),
					"accion":'<input type="button" onclick="editarRolesUsuario('+data1[i].idusuario +',\''+data1[i].nombreusuario+'\')" class="btn btn-default btn-xs" value="Editar"></button>'
				}).draw();
			}
		});
}

function editarRolesUsuario(idusuario, nombreUsuario)
{
	idUsuarioEditando = idusuario;
	$('#nombreUsuarioEdit').val(nombreUsuario);
	var contenedor = $('#checksRoles');
	contenedor.empty();
	for (var i = 0; i < rolesCache.length; i++) {
		if (rolesCache[i].activo != 'S') { continue; }
		contenedor.append('<div class="checkbox" style="margin-left:15px"><label><input type="checkbox" class="chk-rol" value="' + rolesCache[i].idrol + '"> ' + rolesCache[i].nombre + '</label></div>');
	}
	$.ajax({
		url: server + 'CRUDUsuarioRol?idoperacion=4&idusuario=' + idusuario, dataType: 'json', async: false,
		success: function(data){
			var idsAsignados = data[0].idsrol;
			for (var i = 0; i < idsAsignados.length; i++) {
				$('.chk-rol[value="' + idsAsignados[i] + '"]').prop('checked', true);
			}
			bootbox.dialog({
                title: 'Roles de ' + nombreUsuario,
                message: $('#userForm'),
                show: false
            })
            .on('shown.bs.modal', function() { $('#userForm').show(); })
            .on('hide.bs.modal', function(e) { $('#userForm').hide().appendTo('body'); })
            .modal('show');
		}
	});
}

function confirmarGuardarRolesUsuario()
{
	var ids = [];
	$('.chk-rol:checked').each(function(){ ids.push($(this).val()); });
	$.ajax({
		url: server + 'CRUDUsuarioRol?idoperacion=1&idusuario=' + idUsuarioEditando + '&idsrol=' + ids.join(','),
		dataType: 'json', type: 'post', async: false,
		success: function(data){
			pintarUsuarios();
			$('#userForm').parents('.bootbox').modal('hide');
			bootbox.alert('Roles actualizados');
		}
	});
}

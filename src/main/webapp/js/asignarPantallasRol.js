var server;
var modulosCache = [];

$(document).ready(function() {
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	cargarRoles();
	cargarModulos();
	setInterval('validarVigenciaLogueo()',600000);
});

function validarVigenciaLogueo()
{
	var respuesta ='';
	$.ajax({ url: server + 'ValidarUsuarioAplicacion', dataType: 'json', type: 'post', async: false,
		success: function(data){ respuesta = data[0].respuesta; } });
	if (respuesta != 'OK' && respuesta != 'OKA') { location.href = server + "Index.html"; }
}

function cargarRoles()
{
	$.getJSON(server + 'GetRoles', function(data){
		var combo = $('#idrol');
		for(var i = 0; i < data.length; i++){
			if (data[i].activo == 'S') {
				combo.append($('<option>', { value: data[i].idrol, text: data[i].nombre }));
			}
		}
		cargarPantallasDelRol();
	});
}

function cargarModulos()
{
	$.getJSON(server + 'GetModulosConPantallas', function(data){
		modulosCache = data;
		var contenedor = $('#modulos');
		contenedor.empty();
		for(var m = 0; m < data.length; m++){
			var modulo = data[m];
			var bloque = $('<div class="panel panel-default"></div>');
			bloque.append('<div class="panel-heading"><b>' + modulo.nombre + '</b></div>');
			var cuerpo = $('<div class="panel-body"></div>');
			for(var p = 0; p < modulo.pantallas.length; p++){
				var pantalla = modulo.pantallas[p];
				cuerpo.append('<div class="checkbox"><label><input type="checkbox" class="chk-pantalla" value="' + pantalla.idpantalla + '"> ' + pantalla.nombre + ' <small style="color:#999">(' + pantalla.url_html + ')</small></label></div>');
			}
			bloque.append(cuerpo);
			contenedor.append(bloque);
		}
	});
}

function cargarPantallasDelRol()
{
	var idrol = $('#idrol').val();
	if (!idrol) { return; }
	$('.chk-pantalla').prop('checked', false);
	$.ajax({
		url: server + 'CRUDRolPantalla?idoperacion=4&idrol=' + idrol, dataType: 'json', async: false,
		success: function(data){
			var idsAsignados = data[0].idspantalla;
			for (var i = 0; i < idsAsignados.length; i++) {
				$('.chk-pantalla[value="' + idsAsignados[i] + '"]').prop('checked', true);
			}
		}
	});
}

function guardarAsignacion()
{
	var idrol = $('#idrol').val();
	if (!idrol) { bootbox.alert('Seleccione un rol'); return; }
	var ids = [];
	$('.chk-pantalla:checked').each(function(){ ids.push($(this).val()); });
	$.ajax({
		url: server + 'CRUDRolPantalla?idoperacion=1&idrol=' + idrol + '&idspantalla=' + ids.join(','),
		dataType: 'json', type: 'post', async: false,
		success: function(data){ bootbox.alert('Pantallas del rol actualizadas'); }
	});
}

var server;
var table;

$(document).ready(function() {
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	table = $('#grid-roles').DataTable( {
			"aoColumns": [
        { "mData": "idrol" },
        { "mData": "nombre" },
        { "mData": "descripcion" },
        { "mData": "activo" },
        { "mData": "accion", className: "center", defaultContent: '' }
    ]
		} );

	pintarRoles();
	setInterval('validarVigenciaLogueo()',600000);
});

function validarVigenciaLogueo()
{
	var respuesta ='';
	$.ajax({
	   	url: server + 'ValidarUsuarioAplicacion',
	   	dataType: 'json', type: 'post', async: false,
	   	success: function(data){ respuesta = data[0].respuesta; }
	});
	if (respuesta != 'OK' && respuesta != 'OKA') {
		location.href = server + "Index.html";
	}
}

function pintarRoles()
{
	$.getJSON(server + 'GetRoles' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				table.row.add({
					"idrol": data1[i].idrol,
					"nombre": data1[i].nombre,
					"descripcion": data1[i].descripcion,
					"activo": data1[i].activo,
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarRol(' +data1[i].idrol + ')" value="Eliminar"></button> <input type="button" onclick="editarRol('+data1[i].idrol +')" class="btn btn-default btn-xs" value="Edicion"></button>'
				}).draw();
			}
		});
}

function guardarRol()
{
	var params = { nombre: $('#nombre').val(), descripcion: $('#descripcion').val() };
	$.ajax({
		url: server + 'CRUDRol?idoperacion=1', data: params, dataType: 'json', type: 'post', async: false,
		success: function(data){
			$('#addData').modal('hide');
			pintarRoles();
			bootbox.alert('El rol ha sido creado');
		}
	});
}

function eliminarRol(idrol)
{
	$.confirm({
			'title'		: 'Confirmacion Eliminacion Rol',
			'content'	: 'Desea confirmar la desactivacion del rol ' + idrol + '.',
			'buttons'	: {
				'Si'	: { 'class': 'blue', 'action': function(){
						$.ajax({ url: server + 'CRUDRol?idoperacion=3&idrol=' + idrol , dataType: 'json', async: false,
							success: function(data){ pintarRoles(); } });
					} },
				'No'	: { 'class': 'gray', 'action': function(){} }
			}
		});
}

function editarRol(idrol)
{
		$.ajax({
    				url: server + 'CRUDRol?idoperacion=4&idrol=' + idrol, dataType: 'json', async: false,
    				success: function(data){
						var respuesta = data[0];
				            $('#userForm')
				                .find('[name="idroledit"]').val(respuesta.idrol).end()
				                .find('[name="nombreedit"]').val(respuesta.nombre).end()
				                .find('[name="descripcionedit"]').val(respuesta.descripcion).end()

				            bootbox.dialog({
				                    title: 'Editar Rol',
				                    message: $('#userForm'),
				                    show: false
				                })
				                .on('shown.bs.modal', function() { $('#userForm').show(); })
				                .on('hide.bs.modal', function(e) { $('#userForm').hide().appendTo('body'); })
				                .modal('show');
					}
		});
}

function confirmarEditarRol()
{
	var params = {
		idrol: $('input:text[name=idroledit]').val(),
		nombre: $('input:text[name=nombreedit]').val(),
		descripcion: $('input:text[name=descripcionedit]').val()
	};
	$.ajax({
			url: server + 'CRUDRol?idoperacion=2', data: params, dataType: 'json', type: 'post', async: false,
			success: function(data){
				pintarRoles();
        		$('#userForm').parents('.bootbox').modal('hide');
        		bootbox.alert('El rol ha sido actualizado');
			}
	});
}

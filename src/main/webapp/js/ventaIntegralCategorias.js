var server;
var table;

$(document).ready(function() {

	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	table = $('#grid-categorias').DataTable( {
			"aoColumns": [
        { "mData": "idcategoria" },
        { "mData": "nombre" },
        { "mData": "tipodato" },
        { "mData": "mediciontienda" },
        { "mData": "medicioncc" },
        { "mData": "activo" },
        {
            "mData": "accion",
            className: "center",
            defaultContent: ''
        }
    ]
		} );

		pintarCategorias();
		setInterval('validarVigenciaLogueo()',600000);
});

function validarVigenciaLogueo()
{
	var respuesta ='';
	$.ajax({
	   	url: server + 'ValidarUsuarioAplicacion',
	   	dataType: 'json',
	   	type: 'post',
	   	async: false,
	   	success: function(data){
			    respuesta =  data[0].respuesta;
		}
	});
	switch(respuesta)
	{
		case 'OK':
				break;
		case 'OKA':
				break;
		default:
				location.href = server +"Index.html";
		    	break;
	}
}

function pintarCategorias()
{
	$.getJSON(server + 'GetVentaIntegralCategorias' , function(data1){
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				table.row.add({
					"idcategoria": data1[i].idcategoria,
					"nombre": data1[i].nombre,
					"tipodato": data1[i].tipodato,
					"mediciontienda": data1[i].mediciontienda,
					"medicioncc": data1[i].medicioncc,
					"activo": data1[i].activo,
					"accion":'<input type="button" class="btn btn-default btn-xs" onclick="eliminarCategoria(' +data1[i].idcategoria + ')" value="Eliminar"></button> <input type="button" onclick="editarCategoria('+data1[i].idcategoria +')" class="btn btn-default btn-xs" value="Edicion"></button>'
				}).draw();
			}
		});
}

function guardarCategoria()
{
	var params = {
		nombre: $('#nombre').val(),
		abreviatura: $('#abreviatura').val(),
		tipodato: $('#tipodato').val(),
		mediciontienda: $('#mediciontienda').val(),
		excluyeanuladostienda: $('#excluyeanuladostienda').val(),
		filtroestaciontienda: $('#filtroestaciontienda').val(),
		medicioncc: $('#medicioncc').val(),
		orden: $('#orden').val(),
		itemstienda: $('#itemstienda').val(),
		itemscontactcenter: $('#itemscontactcenter').val()
	};
	$.ajax({
		url: server + 'CRUDVentaIntegralCategoria?idoperacion=1',
		data: params,
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(data){
			$('#addData').modal('hide');
			pintarCategorias();
			bootbox.alert('La categoria ha sido creada');
		}
	});
}

function eliminarCategoria(idcategoria)
{
	$.confirm({
			'title'		: 'Confirmacion Eliminacion Categoria',
			'content'	: 'Desea confirmar la eliminacion (desactivacion) de la categoria ' + idcategoria + '.',
			'buttons'	: {
				'Si'	: {
					'class'	: 'blue',
					'action': function(){
						$.ajax({
	    				url: server + 'CRUDVentaIntegralCategoria?idoperacion=3&idcategoria=' + idcategoria ,
	    				dataType: 'json',
	    				async: false,
	    				success: function(data){
								pintarCategorias();
							}
						});
					}
				},
				'No'	: {
					'class'	: 'gray',
					'action': function(){}
				}
			}
		});
}

function editarCategoria(idcategoria)
{
		$.ajax({
    				url: server + 'CRUDVentaIntegralCategoria?idoperacion=4&idcategoria=' + idcategoria,
    				dataType: 'json',
    				async: false,
    				success: function(data){
						var respuesta = data[0];
				            $('#userForm')
				                .find('[name="idcategoriaedit"]').val(respuesta.idcategoria).end()
				                .find('[name="nombreedit"]').val(respuesta.nombre).end()
				                .find('[name="abreviaturaedit"]').val(respuesta.abreviatura).end()
				                .find('[name="ordenedit"]').val(respuesta.orden).end()
				                .find('[name="tipodatoedit"]').val(respuesta.tipodato).end()
				                .find('[name="mediciontiendaedit"]').val(respuesta.mediciontienda).end()
				                .find('[name="excluyeanuladostiendaedit"]').val(respuesta.excluyeanuladostienda).end()
				                .find('[name="filtroestaciontiendaedit"]').val(respuesta.filtroestaciontienda).end()
				                .find('[name="itemstiendaedit"]').val(respuesta.itemstienda).end()
				                .find('[name="medicionccedit"]').val(respuesta.medicioncc).end()
				                .find('[name="itemscontactcenteredit"]').val(respuesta.itemscontactcenter).end()

				            bootbox
				                .dialog({
				                    title: 'Editar Categoria de Venta Integral',
				                    message: $('#userForm'),
				                    show: false
				                })
				                .on('shown.bs.modal', function() {
				                    $('#userForm').show();
				                })
				                .on('hide.bs.modal', function(e) {
				                    $('#userForm').hide().appendTo('body');
				                })
				                .modal('show');
					}
		});
}

function confirmarEditarCategoria()
{
	var params = {
		idcategoria: $('input:text[name=idcategoriaedit]').val(),
		nombre: $('input:text[name=nombreedit]').val(),
		abreviatura: $('input:text[name=abreviaturaedit]').val(),
		orden: $('input[name=ordenedit]').val(),
		tipodato: $('select[name=tipodatoedit]').val(),
		mediciontienda: $('select[name=mediciontiendaedit]').val(),
		excluyeanuladostienda: $('select[name=excluyeanuladostiendaedit]').val(),
		filtroestaciontienda: $('input:text[name=filtroestaciontiendaedit]').val(),
		itemstienda: $('textarea[name=itemstiendaedit]').val(),
		medicioncc: $('select[name=medicionccedit]').val(),
		itemscontactcenter: $('textarea[name=itemscontactcenteredit]').val()
	};
	$.ajax({
			url: server + 'CRUDVentaIntegralCategoria?idoperacion=2',
			data: params,
			dataType: 'json',
			type: 'post',
			async: false,
			success: function(data){
				pintarCategorias();
        		$('#userForm').parents('.bootbox').modal('hide');
        		bootbox.alert('La categoria ha sido actualizada');
			}
	});
}

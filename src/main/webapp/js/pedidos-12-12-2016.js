	

var server = 'http://localhost:8080/ProyectoPizzaAmericana/';
var tiendas;


$(function(){
	console.log('hola');
	getListaTiendas();
	
	
});


function validarTelefono(){

	if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) {
    	table = $('#grid-clientes').DataTable();
    
	}
	else {
    	table = $('#grid-clientes').DataTable( {
    		"aoColumns": [
            { "mData": "idCliente" },
            { "mData": "tienda" },
            { "mData": "nombre" },
            { "mData": "direccion" },
            { "mData": "zona" },
            { "mData": "observacion" },
            { "mData": "telefono" }
        ]
    	} );
	}
	$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data1){
			console.log(data1);
			table.clear().draw();
			for(var i = 0; i < data1.length;i++){
				var cadaCliente  = data1[i];
				table.row.add(data1[i]).draw();
			}
		});
		
}

function validarTelefono4(){

	if ( $.fn.dataTable.isDataTable( '#grid-clientes' ) ) {
    table = $('#grid-clientes').DataTable();
    table.ajax.reload(true, true);
}
else {
    table = $('#grid-clientes').DataTable( {
    	"bProcessing": true,
        "bServerSide": true,
        "sAjaxSource": server + 'GetCliente?telefono=' + telefono.value,
        "sServerMethod": "GET",
        "aoColumns": [
            { "mData": null },
            { "mData": "tienda" },
            { "mData": "nombre" },
            { "mData": "direccion" },
            { "mData": "zona" },
            { "mData": "observacion" }
        ]
    	} );
}
		
}

function validarTelefono3(){

		$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data1){
			console.log(data1);
			
		});
}

function validarTelefono3(){

		$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data1){
			console.log(data1);
			
		});
}

function validarTelefono2(){

		console.log('hola');
		jQuery('#grid-clientes').jqGrid('GridUnload');
		$('#grid-clientes').jqGrid({
                url: server + 'GetCliente?telefono=' + telefono.value,
                mtype: "GET",
                datatype: "json",
                colModel: [
                    { label: 'idCliente', name: 'idCliente', key: true, width: 75 },
                    { label: 'tienda', name: 'tienda', width: 150 },
                    { label: 'nombre', name: 'nombre', width: 150 },
                    { label: 'direccion', name: 'direccion', width: 150 },
                    { label: 'zona', name: 'zona', width: 150 },
                    { label: 'observacion', name: 'observacion', width: 150 }
                ],
				page: 1,
                width: 780,
                height: 250,
                rowNum: 20,
				scrollPopUp:true,
				scrollLeftOffset: "83%",
				viewrecords: true,
                scroll: 1, // set the scroll property to 1 to enable paging with scrollbar - virtual loading of records
                emptyrecords: 'Scroll to bottom to retrieve new page', // the message will be displayed at the bottom 
                pager: "#jqGridPager",
                reloadGridOptions: { fromServer: true }
            });
		jQuery('#grid-clientes').trigger('reloadGrid');


	
}

function validarTelefono1(){

		console.log('hola');
		$.getJSON(server + 'GetCliente?telefono=' + telefono.value, function(data){
			console.log(data);
			data1 = data;
		});
		jQuery('#grid-clientes').jqGrid('clearGridData');
			$('#grid-clientes').jqGrid({
	                data: data,
	                mtype: "GET",
	                datatype: "json",
	                colModel: [
	                    { label: 'idCliente', name: 'idCliente', key: true, width: 75 },
	                    { label: 'tienda', name: 'tienda', width: 150 },
	                    { label: 'nombre', name: 'nombre', width: 150 },
	                    { label: 'direccion', name: 'direccion', width: 150 },
	                    { label: 'zona', name: 'zona', width: 150 },
	                    { label: 'observacion', name: 'observacion', width: 150 }
	                ],
					page: 1,
	                width: 780,
	                height: 250,
	                rowNum: 20,
					scrollPopUp:true,
					scrollLeftOffset: "83%",
					viewrecords: true,
	                scroll: 1, // set the scroll property to 1 to enable paging with scrollbar - virtual loading of records
	                emptyrecords: 'Scroll to bottom to retrieve new page', // the message will be displayed at the bottom 
	                pager: "#jqGridPager",
	                reloadGridOptions: { fromServer: true }
	            });
			$('#grid-clientes').jqGrid('setGridParam', {data: data}).trigger('reloadGrid');
	
}


function getListaTiendas(){
	$.getJSON(server + 'GetTiendas', function(data){
		console.log(data);
		tiendas = data;
		var str = '';
		for(var i = 0; i < data.length;i++){
			var cadaTienda  = data[i];
			str +='<option value="'+ cadaTienda.id +'">' + cadaTienda.nombre +'</option>';
		}
		$('#selectTiendas').html(str);
	});
}







	

var server;
var tiendas;
var table;
var table2;
var dtpuntos;
var administrador = "N";
var imgs;
var fulImgBox;  
var fulImg;
var div;


$(document).ready(function() {

	server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	
	//Marcamos si es administrador para tomar ciertas acciones
	if(respuesta == 'OKA')
    {
        administrador = 'S';
    }else if(respuesta == 'OK')
    {
        administrador = 'N';
    }

	//Lo primero que realizaremos es validar si está logueado

	//Llenamos arreglo con los productos
	
	//cargarMapa();
	//Cargamos los productos tipo Pizza para el menu inicial
	
	//Final cargue productos pizza


    dtpuntos = $('#grid-puntos').DataTable( {
    		"aoColumns": 
    		[
    		{ "mData": "tienda" },
    		{ "mData": "idpedidotienda" },
            { "mData": "fechatransaccion" },
            { "mData": "valorneto" },
            { "mData": "puntos" }
        	]
    	} );

  
});


$(function(){
	
	setInterval('validarVigenciaLogueo()',600000);
	
});



function validarVigenciaLogueo()
{
	var d = new Date();
	
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
		case 'OKP':
				break;	
		default:
				location.href = server +"Index.html";
		    	break;
	}
		    		
}


function consultarPuntosCliente() 
{
	var correo = $("#correo").val();
	if(correo == '' || correo == null)
	{
		alert ('El correo electrónico debe ser diferente a vacío');
		return;
	}
	
	// Si pasa a este punto es porque paso las validaciones
	if ( $.fn.dataTable.isDataTable( '#grid-puntos' ) ) {
    		table = $('#grid-puntos').DataTable();
    }
	$.getJSON(server + 'ServiciosClienteFidelizacion?idoperacion=7&correo=' + correo, function(data1){
	                		
	                		table.clear().draw();
							for(var i = 0; i < data1.length;i++){
								table.row.add({
									"tienda": data1[i].tienda,
									"idpedidotienda": data1[i].idpedidotienda,
									"fechatransaccion": data1[i].fechatransaccion,
									"valorneto": data1[i].valorneto,
									"puntos": data1[i].puntos
								}).draw();
							}
							
	});
	$.getJSON(server + 'ServiciosClienteFidelizacion?idoperacion=5&correo=' + correo, function(data2){
	                		$("#puntos").val(data2.puntos);
	                		console.log("PUNTOS " + data2.puntos);
							
	});
}


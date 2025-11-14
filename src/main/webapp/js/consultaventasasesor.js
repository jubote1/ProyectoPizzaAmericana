	

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


function consultaVentas() 
{
	var fechaInicial = $("#fechainicial").val();
	var asesor = nombreusuario;
	if(fechaInicial == '' || fechaInicial == null)
	{
		alert ('La fecha Inicial debe ser diferente a vacía');
		return;
	}

	
	if(existeFecha(fechaInicial))
	{
	}
	else
	{
		alert ('La fecha Inicial no es correcta');
		return;
	}

	//Validaremos si la fecha inicial es un lunes
	var dateInicial = $("#fechainicial").datepicker( 'getDate' );
	var dateFinal = $("#fechainicial").datepicker( 'getDate' );
	
	//Realizaremos una verificación de que sea lunes
	var dia = dateFinal.getDay();

	if(dia != 1)
	{
		alert ('La fecha seleccionada deberá ser un Lunes');
		$("#fechainicial").val('');
		return;
	}

	dateFinal.setDate(dateFinal.getDate() + 6);

	var finalMesInicial = dateInicial.getMonth()+1;
    if(finalMesInicial < 10)
    {
        finalMesInicial = "0" + finalMesInicial;
    }
    var finalDiaInicial = dateInicial.getDate();
    if(finalDiaInicial  < 10)
    {
        finalDiaInicial = "0" + finalDiaInicial;
    }
	var finalMes = dateFinal.getMonth()+1;
    if(finalMes < 10)
    {
        finalMes = "0" + finalMes;
    }
    var finalDia = dateFinal.getDate();
    if(finalDia  < 10)
    {
        finalDia = "0" + finalDia;
    }
    var fechaFinal = dateFinal.getFullYear() + "-" + finalMes + "-" + finalDia;
    fechaInicial = dateInicial.getFullYear() + "-" + finalMesInicial + "-" + finalDiaInicial; 
    var strGas = '';
    $.getJSON(server + 'ConsultarVentasAsesor?fechainicial='+fechaInicial+'&fechafinal=' + fechaFinal +'&asesor=' + asesor, function(data1){
	                		strGas += '<table class="table table-bordered">';
                        	strGas += '<tbody>';
							for(var i = 0; i < data1.length;i++){
								strGas +='<tr><td>' + data1[i].producto + '</td><td>'+ data1[i].cantidad + '</td></tr>';
							}
							strGas += '</tbody> </table>';
                        	$('#frmresultados').html(strGas);
							
	});
}


function validarFechaMenorActual(date1, date2){
      var fechaini = new Date();
      var fechafin = new Date();
      var fecha1 = date1.split("/");
      var fecha2 = date2.split("/");
      fechaini.setFullYear(fecha1[2],fecha1[1]-1,fecha1[0]);
      fechafin.setFullYear(fecha2[2],fecha2[1]-1,fecha2[0]);
      
      if (fechaini > fechafin)
        return false;
      else
        return true;
}

function existeFecha(fecha){
      var fechaf = fecha.split("/");
      var day = fechaf[0];
      var month = fechaf[1];
      var year = fechaf[2];
      var date = new Date(year,month,'0');
      if((day-0)>(date.getDate()-0)){
            return false;
      }
      return true;
}

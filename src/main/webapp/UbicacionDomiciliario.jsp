<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="capaDAOCC.DomiciliarioPedidoDAO,java.util.ArrayList,org.json.simple.JSONObject" %>

<!DOCTYPE html>
<html>
    <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>JSP Page</title>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css" integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" crossorigin="anonymous">
    <link rel="stylesheet" type="text/css" href="https://js.api.here.com/v3/3.1/mapsjs-ui.css" />
   	<!-- <script src="https://code.jquery.com/jquery-3.5.1.js"></script> -->
    <link rel="stylesheet" type="text/css" href="//cdn.datatables.net/1.10.12/css/jquery.dataTables.css">
    <link rel="icon" href="data:;base64,iVBORw0KGgo=">

    <style>
 
  .contenedor {
  margin:20px
  }
      .mapa{
      width: 800px;       
      height: 600px;
      }
     #titulo {
     background: #000000;
     color: #FFFFFF
     }

    </style>
    </head>
    <body>

	<div class="contenedor">
       <div class="dropdown show mb-2">
	  <a class="btn btn-success dropdown-toggle" href="#" role="button" id="dropdownMenuLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
	    Tienda
	  </a>
	
	  <div class="dropdown-menu" id="dropdown-menu"" aria-labelledby="dropdownMenuLink">
	    <a class='dropdown-item' value='' onclick='sendQuestion(0,false)'>Todas</a>
  </div>
	</div>
   <div class="row"> 
     
   <div class="mapa col-sm-6" id="map"></div>
   <div class="table-responsive col-sm-6" >

 
   <table id="example" class="table table-striped table-bordered" style="text-align:center;" >
    
       <thead>
         <td colspan="4" id="titulo"></td> 
            
        <tr>
          <th>Fecha</th>
          <th>Nombre</th>
          <th>Tienda</th>  
          <th>Localizar</th>
     
        </tr>
        </thead>

    </table>
   
     <div class="card-deck content" ><div class="card border-success" style="max-width: 18rem;">
        <div class="card-header"  >
        <center><h5 class="card-title" id="nombre_domiciliario">Domiciliario</h5></center></div>
        <div class="card-body text-success">
        <h6 class="card-subtitle" id="subtitle">Seleccione el marcador a localizar.</h6>
        <br>
        <p class="card-text" id="direccion"></p>
        </div>
        </div>
    </div>
    </div>
 
     </div>

</div>
    </body>
<script src="https://js.api.here.com/v3/3.1/mapsjs-core.js" type="text/javascript" charset="utf-8"></script>
<script src="https://js.api.here.com/v3/3.1/mapsjs-service.js" type="text/javascript" charset="utf-8"></script>
<script src="https://js.api.here.com/v3/3.1/mapsjs-ui.js" type="text/javascript" charset="utf-8"></script>
<script src="https://js.api.here.com/v3/3.1/mapsjs-mapevents.js"></script>
<script src='https://kit.fontawesome.com/a076d05399.js' crossorigin='anonymous'></script>
   <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.11.0/umd/popper.min.js" integrity="sha384-b/U6ypiBEHpOf/4+1nzFpr53nxSS+GLCkfwBdFNTxtclqqenISfwAzpKaMNFNmj4" crossorigin="anonymous"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js" integrity="sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl" crossorigin="anonymous"></script>
<script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.11.5/js/dataTables.bootstrap4.min.js"></script>

<script  src="js/Localizacion.js" ></script>
<script>


window.onload = function () {
	sendQuestion(-1,false);
	}	
	 <%
	 
	 DomiciliarioPedidoDAO dao = new DomiciliarioPedidoDAO();
    ArrayList<JSONObject> listaTienda = dao.ListaTiendas();
	    for (JSONObject j : listaTienda) {
	    	String nombre = (String) j.get("nombre");
	    	int idTienda =(int) j.get("id");
	    	%>
	    	var nom = {"nombre": <%='"'+nombre+'"'%>} ;
	        var bool = true;
	    	$('#dropdown-menu').append("<a  class='dropdown-item' value='"+nom["nombre"]+"' onclick='sendQuestion("+<%=idTienda%>+","+bool+")'>"+nom["nombre"]+"</a>");
	    <%
	    }
		%>
		$(".dropdown-item").on("click",function(){
			document.getElementById("titulo").innerHTML ="<strong>"+$(this).val().toUpperCase()+"</strong>";
			nombre =$(this).val(); 
		
		});

  	     </script>    
</html>
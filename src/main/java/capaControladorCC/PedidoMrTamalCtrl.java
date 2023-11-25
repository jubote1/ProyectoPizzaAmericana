package capaControladorCC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.AdicionDetallePedidoDAO;
import capaDAOCC.ClienteDAO;
import capaDAOCC.DescuentoGeneralDAO;
import capaDAOCC.DireccionFueraZonaDAO;
import capaDAOCC.DomiciliarioPedidoDAO;
import capaDAOCC.EspecialidadDAO;
import capaDAOCC.ExcepcionPrecioDAO;
import capaDAOCC.FormaPagoDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.LogEventoWompiDAO;
import capaDAOCC.LogPedidoVirtualDAO;
import capaDAOCC.LogPedidoVirtualKunoDAO;
import capaDAOCC.MarcacionPedidoDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.PedidoLinkWompiDAO;
import capaDAOCC.PedidoPagoVirtualConsolidadoDAO;
import capaDAOCC.PedidoPagoVirtualDAO;
import capaDAOCC.PedidoTiendaVirtualDAO;
import capaDAOCC.ProductoDAO;
import capaDAOCC.TiempoPedidoDAO;
import capaDAOCC.TiendaDAO;
import capaDAOCC.TiendaFranquiciaWompiDAO;
import capaDAOCC.TmpPedidosPoligonoDAO;
import capaModeloCC.AdicionTiendaVirtual;
import capaModeloCC.Cliente;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.DescuentoGeneral;
import capaModeloCC.DetallePedido;
import capaModeloCC.DetallePedidoAdicion;
import capaModeloCC.DetallePedidoPixel;
import capaModeloCC.DireccionFueraZona;
import capaModeloCC.DomiciliarioPedido;
import capaModeloCC.Especialidad;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FormaPago;
import capaModeloCC.HomologaGaseosaIncluida;
import capaModeloCC.InsertarPedidoPixel;
import capaModeloCC.LogEventoWompi;
import capaModeloCC.MarcacionPedido;
import capaModeloCC.ModificadorDetallePedido;
import capaModeloCC.NomenclaturaDireccion;
import capaModeloCC.Pedido;
import capaModeloCC.PedidoLinkWompi;
import capaModeloCC.PedidoPagoVirtual;
import capaModeloCC.Producto;
import capaModeloCC.ProductoIncluido;
import capaModeloCC.SaborLiquido;
import capaModeloCC.Tienda;
import capaModeloCC.TiendaFranquiciaWompi;
import capaDAOPOS.DetallePedidoDAO;
import utilidadesCC.ControladorEnvioCorreo;

public class PedidoMrTamalCtrl {


	public String tratarPedidoMrTamalKuno(String stringJSON, String authHeader)
	{
		//Varibles para el procesamiento del pedido
		//Variable donde quedará almacenada el idPedido creado
		String idOrdenComercio = "";
		String tipoPedido = "";
		int idTipoPedido = 1;
		long valorTotal = 0;
		//Formamos la fecha del pedido que podrá cambiar si el pedido es posfechado
		Date fechaPedido = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
		String strFechaFinal = formatoFinal.format(fechaPedido);
		
		//Realizamos la inserción de log con el JSON recibido
		LogPedidoVirtualKunoDAO.insertarLogPedidoVirtualKuno(stringJSON, authHeader);

		//Comenzaremos a parsear el JSON que nos llego
		JSONParser parser = new JSONParser();
		try
		{
			//Realizamos el parseo del primer nivel del JSON
			Object objParser = parser.parse(stringJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			//Descomponemos la información de orders que es donde está agrupado toda la info del pedido
			String ordersJSON = (String)jsonGeneral.get("orders").toString();
			Object objParserOrders = parser.parse(ordersJSON);
			JSONArray jsonOrdersArray = (JSONArray) objParserOrders;
			//Recorremos el arreglo
			for(int i = 0; i < jsonOrdersArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) jsonOrdersArray.get(i);
				//Comenzamos capturando la información del cliente del pedido
				//Capturamos el número de orden en el ecommerce
				idOrdenComercio = String.valueOf((long)objTemp.get("id"));
				valorTotal = (long)objTemp.get("total_price");
				//Continuamos con el procesamiento del pedido
				String telefono = (String)objTemp.get("client_phone");
				String telefonoCelular = "";
				//El telefono le quitaremos el indicativo del país
				if(telefono.substring(0, 3).equals(new String("+57")))
				{
					telefono = telefono.substring(3);
				}
				//Para el caso del teléfono validaremos si es un fijo
				if(telefono.trim().length() == 7)
				{
					//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
					//contact center.
					telefono = "604" + telefono;
				}else
				{
					telefonoCelular = telefono;
				}
				String nombres = (String)objTemp.get("client_first_name");
				String apellidos = (String)objTemp.get("client_last_name");
				//Tendremos un trato diferencial para la dirección
				String dirRes = "";
				String ciudad = "";
				String dirAdicional = "";
				//Trabajamos sobre la dirección resumida
				//Debemos de crear otro objeto JSON
				JSONObject infoAdiDir = (JSONObject)objTemp.get("client_address_parts");
				try
				{
					dirRes = (String)infoAdiDir.get("street");
					if(dirRes == null)
					{
						dirRes = "";
					}
				}catch(Exception e)
				{
					dirRes = "";
				}
				//Trabajamos sobre la ciudad
				try
				{
					ciudad = (String)infoAdiDir.get("city");
					if(ciudad == null)
					{
						ciudad = "";
					}
				}catch(Exception e)
				{
					ciudad = "";
				}
				//Trabajamos sobre la info adicional
				try
				{
					dirAdicional = (String)infoAdiDir.get("more_address");
					if(dirAdicional == null)
					{
						dirAdicional = "";
					}
				}catch(Exception e)
				{
					dirAdicional = "";
				}
				
				//Trabajamos sobre el campo Instrucciones
				String instrucciones = "";
				try
				{
					instrucciones = (String)objTemp.get("instructions");
					if(instrucciones == null)
					{
						instrucciones = "";
					}
				}catch(Exception e)
				{
					instrucciones = "";
				}
				
				String direccion = (String)objTemp.get("client_address");
				//Pendiente revisar como incorporaremos estas informaciones
				String zonaBarrio = "";
				String obsDireccion = "";
				//Tendremos unas reglas para conformar la dirección
				if(dirAdicional.equals(new String("")))
				{
					
				}else
				{
					direccion = dirRes + " " + ciudad;
					obsDireccion = dirAdicional;
				}
				
				
				String email  = (String)objTemp.get("client_email");
				//En ocasiones cuando no es definida la latitud ni la longitud esta llega como un String por lo
				//tanto es necesario incluirlas dentro de un try y si hay excepción llenar con cero los valores
				double latitud = 0, longitud = 0;
				try
				{
					latitud = Double.parseDouble((String)objTemp.get("latitude"));
				}catch(Exception e)
				{
					latitud = 0;
					System.out.println(e.toString());
				}
				try
				{
					longitud = Double.parseDouble((String)objTemp.get("longitude"));
				}catch(Exception e)
				{
					longitud = 0;
					System.out.println(e.toString());
				}
				tipoPedido = (String)objTemp.get("type");
				if(tipoPedido.equals(new String("delivery")))
				{
					idTipoPedido = 1;
				}else if(tipoPedido.equals(new String("pickup")))
				{
					idTipoPedido = 2;
				}
				//El servicio de Kuno permite identificar el restaurante con el campo restaurante token
				int idTienda = Integer.parseInt((String) objTemp.get("restaurant_token"));
				String nombreTienda = (String) objTemp.get("restaurant_name");
				String tokenPrivado = TiendaFranquiciaWompiDAO.obtenerTiendaFranquiciaWompiDAO(idTienda);
				
				//El servicio de Kuno permite identificar el restaurante con el campo restaurante token
				String restaurante = (String) objTemp.get("restaurant_token");
				//Vamos por la forma de pago en su homologación
				//Realizamos la captura del método de pago
				String formPagVirtual = (String)objTemp.get("payment");
				//Con la forma de pago transferencia es que haremos la opción de WOMPI - CARD_PHONE - Transferencia Bancaria
				if(formPagVirtual.equals(new String("CARD_PHONE")) || formPagVirtual.equals(new String("Transferencia Bancaria")))
				{
					Cliente clienteVirtual = new Cliente(0, telefono, nombres, direccion, zonaBarrio, obsDireccion,"", 0);
					clienteVirtual.setApellidos(apellidos);
					clienteVirtual.setEmail(email);
					clienteVirtual.setLatitud((float)latitud);
					clienteVirtual.setLontitud((float)longitud);
					clienteVirtual.setTelefonoCelular(telefonoCelular);
					clienteVirtual.setPoliticaDatos("S");
					//Realizamos el envío del mensaje de texto 
					String idLink = verificarEnvioLinkPagosMrTamal(idOrdenComercio, clienteVirtual, (valorTotal), tokenPrivado, nombreTienda);
					//Adicionamos la información en la tabla para buscar despues
					PedidoLinkWompi pedidoLink = new PedidoLinkWompi(nombreTienda, idTienda, idOrdenComercio,idLink);
					PedidoLinkWompiDAO.insertarPedidoLinkWompi(pedidoLink);
				}

			}
		}catch(Exception e)
		{
			
		}
		return("");
	}
	
	
	public String verificarEnvioLinkPagosMrTamal(String idPedidoTienda, Cliente clienteVirtual, double totalPedido, String tokenPrivado, String nombreTienda)
	{
		String idLink = "";
		//Creamos la fecha Actual
		Date dateFecha = new Date();
		Calendar calendarioActual = Calendar.getInstance();
		try
		{
			calendarioActual.add(Calendar.DAY_OF_YEAR, 1);
		}catch(Exception e)
		{
			
		}
		dateFecha = calendarioActual.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String strFechaExt = dateFormat.format(dateFecha);
		//Creamos el JSON para consumir el servicio
		String jsonLinkPago = '{' +
                "\"amount_in_cents\":"  + (int)totalPedido*100 +","+
                "\"currency\": \"COP\"," + 
                "\"name\": \"MR TAMAL"  + " " + nombreTienda + "\" ," +
                "\"description\": \"Pedido #" + idPedidoTienda + "\","+
                "\"expires_at\": \"" + strFechaExt  + "T23:00:00.000Z\","+
                "\"redirect_url\": \"https://mrtamal.com\","+
                "\"single_use\": true,"+
                "\"sku\": \"" + idPedidoTienda + "\","+
                "\"collect_shipping\": false"+
              "}";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURLWOMPI = "https://production.wompi.co/v1/payment_links";
		HttpPost request = new HttpPost(rutaURLWOMPI);
		try
		{
			//Fijamos el header con el token
			request.setHeader("Authorization", "Bearer " + tokenPrivado);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			//Fijamos los parámetros
			//pass the json string request in the entity
		    HttpEntity entity = new ByteArrayEntity(jsonLinkPago.getBytes("UTF-8"));
		    request.setEntity(entity);
			//request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			String datosJSON = retorno.toString();
			
			//Los datos vienen en un arreglo, debemos de tomar el primer valor como lo hacemos en la parte gráfica
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			String dataJSON = (String)jsonGeneral.get("data").toString();
			Object objParserData = parser.parse(dataJSON);
			JSONObject jsonData = (JSONObject) objParserData;
			idLink = (String)jsonData.get("id");
			//En la parte de arriba ya tenemos la generación del link la idea en este punto es realizar
			
			//reutilización de la lógica del resto para el envío de la notificación
			realizarNotificacionWompi(idLink, clienteVirtual, "https://checkout.wompi.co/l/"+idLink,  idPedidoTienda);
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
		return(idLink);
	}
	
	
	public String realizarNotificacionWompi(String idLink, Cliente clienteNoti , String linkPago,  String idPedido)
	{
		String observacionLog = "";
		String emailEnvio = "";
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		//Procesamos los mensajes de texto y correo electrónico
		String mensajeTexto = "Querido Cliente de MR TAMAL por favor realiza el pago de tu pedido en el siguiente link #VINCULO";
		String telefonoCelular = clienteNoti.getTelefonoCelular();
		mensajeTexto = mensajeTexto.replace("#VINCULO", linkPago);
		//Envío del mensaje de Texto
		promoCtrl.ejecutarPHPEnvioMensaje( "57"+ telefonoCelular, mensajeTexto);
		observacionLog = "Se envio mensaje de texto.";
		//Vamos a verificar si el cliente tiene correo electrónico para enviarlo si es el caso
		if(clienteNoti.getEmail() != null)
		{
			if(clienteNoti.getEmail().length()> 0)
			{
				if(clienteNoti.getEmail().contains("@"))
				{
					observacionLog = observacionLog + " Se tiene email para enviar.";
					String cuentaCorreo = "mrtamalwompi@gmail.com";
					String claveCorreo = "iyoiagfgltxldjsy";
					String imagenWompi = "https://mrtamal.com/wp-content/uploads/2022/03/WhatsApp-Image-2022-03-07-at-10.38.15-AM.jpeg";
					String mensajeCorreo = "Querido Cliente de MR TAMAL, por favor realiza el pago de tu pedido en el siguiente link #VINCULO";
					mensajeCorreo = mensajeCorreo.replace("#VINCULO", linkPago);
					Correo correo = new Correo();
					correo.setAsunto("MR TAMAL LINK DE PAGO PEDIDO # " + idPedido);
					ArrayList correos = new ArrayList();
					String correoEle = clienteNoti.getEmail();
					emailEnvio = correoEle;
					correos.add(correoEle);
					correo.setContrasena(claveCorreo);
					correo.setUsuarioCorreo(cuentaCorreo);
					String mensajeCuerpoCorreo = "Cordiar Saludo " + clienteNoti.getNombres() + clienteNoti.getApellidos() + " ." + mensajeCorreo 
							+ "\n" + "<body><a href=\"" + linkPago + "\"><img align=\" center \" src=\""+ imagenWompi +"\"></a></body>";
					correo.setMensaje(mensajeCuerpoCorreo);
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					//Agregamos control para que verifique con que método debe hacer el envío
					if(cuentaCorreo.contains("@gmail.com"))
					{
						contro.enviarCorreo();
					}else
					{
						contro.enviarCorreo();
					}
				}
			}
		}
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	public String capturarEventoPagoWompiMrTamal(String stringJSON)
	{
		// Debemos de procesar todo datos para extraer todo los datos requeridos
		JSONParser parser = new JSONParser();
		try {
			 //De acuerdo a la definición de como se envian los eventos, extraremos
			 Object objParser = parser.parse(stringJSON);
			 JSONObject jsonPedido = (JSONObject) objParser;
			 String evento = (String)jsonPedido.get("event");
			 			 
			 //Vamos extraer la informacion de DATA
			 String dataJSON = (String)jsonPedido.get("data").toString();
			 Object objParserData = parser.parse(dataJSON);
			 JSONObject jsonTransaction = (JSONObject) objParserData;
			 String dataFinal = (String)jsonTransaction.get("transaction").toString();
			 //Convertimos el JSON de Data
			 Object objParserTransaction = parser.parse(dataFinal);
			 JSONObject jsonTransaccion =(JSONObject)objParserTransaction;
			 String idLink = (String)jsonTransaccion.get("payment_link_id");
			 String estado = (String)jsonTransaccion.get("status");
			 String tipoPago = (String)jsonTransaccion.get("payment_method_type");
			 //Posteriormente de capturada la información realizaremos un guardado de log de eventos WOMPI
			 LogEventoWompi logEvento = new LogEventoWompi(idLink, evento, estado, stringJSON );
			 int idLogEvento = LogEventoWompiDAO.insertarLogEventoWompi(logEvento);
			 //Posteriormente verificaremos si es un transacción de aprobado, y verificaremos el estado del idlink
			 if(evento.equals(new String("transaction.updated")))
			 {
				 //En este punto es porque hubo el pago de una transacción
				 if(estado.equals(new String("APPROVED")))
				 {
					PedidoLinkWompi pedidoLink = PedidoLinkWompiDAO.obtenerPedidoLinkWompi(idLink);
					String cuentaCorreo = "mrtamalwompi@gmail.com";
					String claveCorreo = "iyoiagfgltxldjsy";
					String mensajeCorreo = "Se ha recibido el pago del pedido " + pedidoLink.getPedido() + " de la tienda " + pedidoLink.getTienda() + " con el link " + pedidoLink.getLink();
					Correo correo = new Correo();
					correo.setAsunto( idLink + "-" + pedidoLink.getPedido() + "-" + pedidoLink.getTienda());
					ArrayList correos = new ArrayList();
					//Recuperamos el correo de la tienda a la cual se debe de notificar
					TiendaFranquiciaWompi tienda = TiendaFranquiciaWompiDAO.obtenerTiendaFranquiciaWompi(pedidoLink.getIdTienda());
					String correoEle = tienda.getCorreo();
					correos.add(correoEle);
					correo.setContrasena(claveCorreo);
					correo.setUsuarioCorreo(cuentaCorreo);
					correo.setMensaje(mensajeCorreo);
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					//Agregamos control para que verifique con que método debe hacer el envío
					if(cuentaCorreo.contains("@gmail.com"))
					{
						contro.enviarCorreo();
					}else
					{
						contro.enviarCorreo();
					}
				 }
			 }
			 
		}catch (Exception e) {
            e.printStackTrace();
		} 
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
}



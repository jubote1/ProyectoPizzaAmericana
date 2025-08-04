package capaControladorCC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import capaModeloCC.Cliente;
import capaDAOCC.ClienteDAO;
import capaDAOCC.ClienteFidelizacionDAO;
import capaDAOCC.CodigoRedencionPuntosDAO;
import capaDAOCC.FidelizacionTransaccionDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.IntegracionCRMDAO;
import capaDAOCC.ParametrosDAO;
import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.CodigoRedencionPuntos;
import capaModeloCC.Correo;
import capaModeloCC.FidelizacionTransaccion;
import capaModeloCC.IntegracionCRM;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import utilidadesCC.ControladorEnvioCorreo;

public class FidelizacionCtrl {
	
	
	public String existeClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		ClienteFidelizacion existe = ClienteFidelizacionDAO.obtenerClienteFidelizacion(correo);
		
		if(existe != null) {
			respuesta.put("activo", existe.getActivo());
			respuesta.put("respuesta", true);
		}else {
			respuesta.put("respuesta", false);
		}
		
		return(respuesta.toJSONString());
	}
	
	public boolean existeClienteFidelizacionBoolean(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean existe = ClienteFidelizacionDAO.existeClienteFidelizacion(correo);
		return(existe);

	}
	
	public String insertarClienteFidelizacion(String correo, String canal)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.insertarClienteFidelizacion(correo, canal);
		//Enviamos correo Bienvenida
		enviarCorreoBienvenida(correo);
	    //Finalizacion
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}
	
	public void enviarCorreoBienvenida(String correo)
	{
		//Realizamos modificación para que cuando se haga matricula de programa de fidelizacion
				IntegracionCRM brevo = IntegracionCRMDAO.obtenerInformacionIntegracion("BREVO");
				//Se realiza logica para envio de correo
				OkHttpClient client = new OkHttpClient();
			    // Configuración global
			    String apiKey = brevo.getAccessToken();
			    String senderEmail = "mercadeo@pizzaamericana.com.co";
			    String senderName = "Pizza Americana";
			    String subjectDefault = "Puntos, premios y mas sabor para ti🍕";
			    int templateId = 14; // ID de la plantilla en Brevo
			    // Construcción del JSON principal
				JSONObject paramsDefault = new JSONObject();
				paramsDefault.put("nombre", "Correo");
		        JSONObject jsonRequest = new JSONObject();
		        jsonRequest.put("subject", subjectDefault);
		        JSONObject jsonObjectCorreo = new JSONObject();
		        jsonObjectCorreo.put("email", senderEmail);
		        jsonObjectCorreo.put("name", senderName);
		        jsonRequest.put("sender", jsonObjectCorreo);
		        jsonRequest.put("templateId", templateId);
		        jsonRequest.put("params", paramsDefault);
		        JSONArray messageVersions = new JSONArray();
		        JSONObject messageVersion = new JSONObject();
		        JSONObject jsonObjectDest = new JSONObject();
		        JSONArray jsonArrayCorreo = new JSONArray();
		        JSONObject jsonObjectTO = new JSONObject();
		        jsonObjectTO.put("email", correo);
		        jsonObjectTO.put("name", "Correo");
		        jsonArrayCorreo.add(jsonObjectTO);
		        JSONObject params = new JSONObject();
		        params.put("nombre", "Correo");
		        messageVersion.put("to", jsonArrayCorreo);
		        messageVersion.put("params", params);
		        messageVersion.put("subject", "Puntos, premios y mas sabor para ti🍕");  
	            messageVersions.add(messageVersion);
		        jsonRequest.put("messageVersions", messageVersions);

		        // Envío de la solicitud HTTP
		        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonRequest.toString());
		        Request request = new Request.Builder()
		                .url("https://api.brevo.com/v3/smtp/email")
		                .post(body)
		                .addHeader("Content-Type", "application/json")
		                .addHeader("Accept", "application/json")
		                .addHeader("api-key", apiKey)
		                .build();
		        // Ejecución de la solicitud
		        try (Response response = client.newCall(request).execute()) {
		            System.out.println("Response Code: " + response.code());
		            System.out.println("Response Body: " + response.body().string());
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
	}
	
	public static void main(String[] args)
	{
		FidelizacionCtrl prueba = new FidelizacionCtrl();
		prueba.enviarCorreoBienvenida("jubote1@gmail.com");
	}
	
	

	public String activarClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.activarClienteFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}

	
	
	public String desactivarClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.desactivarClienteFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}


	public String sumarPuntosClienteFidelizacion(String correo, double puntosSumar, int idTienda, int idPedidoTienda, double valorNeto)
	{
		JSONObject respuesta = new JSONObject();
		double acumula =  0;
		boolean creaTransaccion = false;
		boolean existe = FidelizacionTransaccionDAO.existeFidelizacionTransaccion(correo, idTienda, idPedidoTienda);
		if(!existe)
		{
			acumula =  ClienteFidelizacionDAO.sumarPuntosClienteFidelizacion(correo, puntosSumar);
			FidelizacionTransaccion transaccion = new FidelizacionTransaccion(correo, idTienda, idPedidoTienda, valorNeto, puntosSumar);
			creaTransaccion = FidelizacionTransaccionDAO.insertarFidelizacionTransaccion(transaccion);
			if(acumula > 0 && creaTransaccion)
			{
				respuesta.put("respuesta", true);
			}
		}else
		{
			respuesta.put("respuesta", false);
		}
		
		return(respuesta.toJSONString());
	}
	
	/**
	 * Método que se encarga de redimir los puntos del plan de fidelización
	 * @param correo
	 * @param puntosRedimir
	 * @param idTienda
	 * @param idPedidoTienda
	 * @return
	 */
	public double redimirPuntosClienteFidelizacion(String correo, double puntosRedimir)
	{
		double puntosRestantes =  0;
		puntosRestantes=  ClienteFidelizacionDAO.redimirPuntosClienteFidelizacion(correo, puntosRedimir);
		//Instalar log de la redención
		ClienteFidelizacionDAO.insertarLogRedencion(correo, puntosRedimir);
		return(puntosRestantes);
	}

	public String  obtenerFidelizacionTransacciones(String correo)
	{
		JSONArray transacciones = new JSONArray();
		JSONObject transaccion = new JSONObject();
		ArrayList<FidelizacionTransaccion> tranObj = FidelizacionTransaccionDAO.obtenerFidelizacionTransacciones(correo);
		FidelizacionTransaccion tranTemp;
		for(int i = 0; i < tranObj.size(); i++)
		{
			tranTemp = tranObj.get(i);
			transaccion = new JSONObject();
			transaccion.put("idtienda", tranTemp.getIdTienda());
			transaccion.put("tienda", tranTemp.getTienda());
			transaccion.put("idpedidotienda", tranTemp.getIdPedidoTienda());
			transaccion.put("fechatransaccion", tranTemp.getFechaTransaccion());
			transaccion.put("valorneto", tranTemp.getValorNeto());
			transaccion.put("puntos", tranTemp.getPuntos());
			transacciones.add(transaccion);
		}
		return(transacciones.toJSONString());
	}
	
	public String obtenerCantidadPuntosCliente(String correo)
	{
		JSONObject respuesta = new JSONObject();
		double puntos = ClienteFidelizacionDAO.obtenerCantidadPuntosCliente(correo);
		respuesta.put("puntos", puntos);
		return(respuesta.toJSONString());
	}
	
	
	/**
	 * Método que se encarga de validar la redención de los puntos por un cliente determinado con su correo
	 * @param correo
	 * @param puntosRedimir
	 * @return
	 */
	public String validarRedencionPuntos(String correo)
	{
		JSONObject respuesta = new JSONObject();
		ClienteFidelizacion clienteF = ClienteFidelizacionDAO.obtenerClienteFidelizacion(correo);

        if (clienteF == null) {
            // No existe en fidelización
            respuesta.put("respuesta", "NOK");
            respuesta.put("puntos", 0);
            return respuesta.toJSONString();
        }
        
        if(clienteF != null)
        {
        	respuesta.put("respuesta", "OK");
        	respuesta.put("puntos", clienteF.getPuntosVigentes());
        	
        }
		return(respuesta.toJSONString());
	}
	
	
	/**
	 * Método que crea un código y lo inserta en la tabla de codigo para redención de puntos para posteriormente validar
	 * @param codRed
	 * @return
	 */
	public String crearCodigoValidarRedencionPuntos(CodigoRedencionPuntos codRed)
	{
		JSONObject respuesta = new JSONObject();
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		String codigoRedencionGen = promoCtrl.generarCodigoRedencionPuntos();
		codRed.setCodigo(codigoRedencionGen);
		CodigoRedencionPuntosDAO.insertarCodigoRedencionPuntos(codRed);
        respuesta.put("codigo", codigoRedencionGen);
        //Realizamos envío del correo electrónico indicando el código de redención al cliente

		//En este punto deberemos de enviar el correo electrónico para lider contact center con los datos para la creación del mensaje
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		Date fecha = new Date();
		Correo correo = new Correo();
		correo.setAsunto("ENVIO CODIGO VALIDACIÓN REDENCIÓN PUNTOS PIZZA AMERICANA" + fecha.toString());
		ArrayList correos = new ArrayList();
		correos.add(codRed.getCorreo());
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		String mensajeCuerpoCorreo = "Por favor debes facilitar tu código de redención de tus puntos en la tienda " + codigoRedencionGen ;
		correo.setMensaje(mensajeCuerpoCorreo);
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
		return(respuesta.toJSONString());
	}
	
	public String validarExistenciaCodigoRedencion(String codigo, String correo, String fechaSistema)
	{
		JSONObject respuesta = new JSONObject();
		boolean existeCodigo = CodigoRedencionPuntosDAO.validarExistenciaCodigoRedencion(codigo, correo, fechaSistema);
		if(existeCodigo)
		{
			respuesta.put("respuesta", true);
		}else {
			respuesta.put("respuesta", false);
		}
		return(respuesta.toJSONString());	
	}
	
	
	public String ConsultarClienteFidelizacionWb(String correo) {
	    JSONObject respuesta = new JSONObject();
	    try {
	        ClienteFidelizacion clienteF = ClienteFidelizacionDAO.obtenerClienteFidelizacion(correo);

	        if (clienteF == null) {
	            // No existe en fidelización
	            respuesta.put("respuesta", false);
	            respuesta.put("mensaje", "no_registrado");
	            return respuesta.toJSONString();
	        }

	        Cliente existClient = ClienteDAO.obtenerClientePorCorreo(correo);

	        if (existClient == null) {
	            // Existe en fidelización, pero no en clientes
	            respuesta.put("respuesta", false);
	            respuesta.put("mensaje", "sin_datos");
	            return respuesta.toJSONString();
	        }

	        // Ahora sí, verificamos si está activo
	        if (!"S".equals(clienteF.getActivo())) {
	            respuesta.put("respuesta", false);
	            respuesta.put("mensaje", "no_activo");
	            return respuesta.toJSONString();
	        }

	        // Cliente encontrado y activo
	        respuesta.put("respuesta", true);
	        respuesta.put("mensaje", "registrado");
	        respuesta.put("nombre", existClient.getNombres());
	        respuesta.put("apellidos", existClient.getApellidos());
	        respuesta.put("puntos", clienteF.getPuntosVigentes());

	    } catch (Exception e) {
	        respuesta.put("respuesta", false);
	        respuesta.put("mensaje", "error");
	        e.printStackTrace(); // Usar Logger en producción
	    }

	    return respuesta.toJSONString();
	}

	/**
	 * Método que retorna el cliente fidelización dado un correo
	 * @param correo
	 * @return
	 */
	public ClienteFidelizacion ConsultarClienteFidelizacionOBJ(String correo) 
	{
	    ClienteFidelizacion clienteF = ClienteFidelizacionDAO.obtenerClienteFidelizacion(correo);
	    return(clienteF);
	}


	public String RegistrarClienteFidelizacionWb(String correo, String nombre, String telefono, String fechaNacimiento, String apellido, int idtienda, String politicaDatos) {
	    JSONObject respuesta = new JSONObject();

	    try {
	        ClienteFidelizacion clienteF = ClienteFidelizacionDAO.obtenerClienteFidelizacion(correo);
	        Cliente existClient = ClienteDAO.obtenerClientePorCorreo(correo);

	        // Si el cliente no existe en Cliente, registrarlo
	        if (existClient == null) {
	            if (!guardarCliente(null, correo, nombre, telefono, fechaNacimiento, apellido, idtienda, politicaDatos)) {
	                respuesta.put("respuesta", false);
	                respuesta.put("mensaje", "registro_fallido");
	                return respuesta.toJSONString();
	            }
	            existClient = ClienteDAO.obtenerClientePorCorreo(correo); // Recuperar ID asignado
	        }

	        // Si el cliente ya está en fidelización
	        if (clienteF != null) {
	            if (!guardarCliente(existClient.getIdcliente(), correo, nombre, telefono, fechaNacimiento, apellido, idtienda, politicaDatos)) {
	                respuesta.put("respuesta", false);
	                respuesta.put("mensaje", "registro_fallido");
	                return respuesta.toJSONString();
	            }

	            // Si el cliente está activo en fidelización
	            if ("S".equals(clienteF.getActivo())) {
	                respuesta.put("respuesta", false);
	                respuesta.put("mensaje", "registrado");
	            } else {
	                boolean activado = ClienteFidelizacionDAO.activarClienteFidelizacion(correo);
	                respuesta.put("respuesta", activado);
	                respuesta.put("mensaje", activado ? "registro_exitoso" : "no_activo");
	            }
	            return respuesta.toJSONString();
	        }

	        // Si el cliente NO está en fidelización, registrarlo
	        boolean inserto = ClienteFidelizacionDAO.insertarClienteFidelizacion(correo, "WEB");
	        //Enviamos correo Bienvenida
			enviarCorreoBienvenida(correo);
	        respuesta.put("respuesta", inserto);
	        respuesta.put("mensaje", inserto ? "registro_exitoso" : "registro_fallido");

	    } catch (Exception e) {
	        respuesta.put("respuesta", false);
	        respuesta.put("mensaje", "error");
	        e.printStackTrace();
	    }

	    return respuesta.toJSONString();
	}

	private boolean guardarCliente(Integer idCliente, String correo, String nombre, String telefono, 
	                               String fechaNacimiento, String apellido, int idtienda, String politicaDatos) {
	    Cliente cliente = new Cliente();
	    cliente.setIdtienda(idtienda);
	    cliente.setTelefonoCelular(telefono);
	    cliente.setTelefono(telefono);
	    cliente.setNombres(nombre);
	    cliente.setApellidos(apellido);
	    cliente.setFechaNacimiento(fechaNacimiento);
	    cliente.setEmail(correo);
	    cliente.setPoliticaDatos(politicaDatos);

	    if (idCliente == null || idCliente <= 0) {
	        int id = ClienteDAO.insertarClienteWb(cliente);
	   
	        return id > 0;
	    } else {
	        cliente.setIdcliente(idCliente);
	        boolean actualizado = ClienteDAO.actualizarClienteWb(cliente);
	  
	        return actualizado;
	    }
	}



	public String realizarRedencionPuntos(String codigo, String  correo, double puntosRedimir, int idTienda, int idPedido)
	{
		JSONObject respuesta = new JSONObject();

		boolean creaTransaccion = false;
		ClienteFidelizacion cliente = ConsultarClienteFidelizacionOBJ(correo);
		if(cliente == null)
		{
			//Cliente no existe en plan de fidelizacion
			respuesta.put("respuesta", "NOK2");
			respuesta.put("puntosrestantes", 0);
			return(respuesta.toJSONString());
		}
		if(cliente.getPuntosVigentes() < puntosRedimir)
		{
			//No hay puntos suficientes
			respuesta.put("respuesta", "NOK1");
			respuesta.put("puntosrestantes", 0);
			return(respuesta.toJSONString());
		}
		//Marcamos como validado el código
		CodigoRedencionPuntosDAO.marcarValidadoRedencionPuntos(codigo);
		//Crearemos transacción para que quede la información
		FidelizacionTransaccion transaccion = new FidelizacionTransaccion(correo, idTienda, idPedido, 0, puntosRedimir);
		creaTransaccion = FidelizacionTransaccionDAO.insertarFidelizacionTransaccion(transaccion);
		//Posteriormente hacemos la resta de los puntos
		double puntosRestantes = redimirPuntosClienteFidelizacion(correo, puntosRedimir);
		respuesta.put("respuesta", "OK");
		respuesta.put("puntosrestantes", puntosRestantes);
		return(respuesta.toJSONString());
		
	}


}

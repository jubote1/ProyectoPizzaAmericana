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
import capaDAOCC.ClienteNoFidelizacionDAO;
import capaDAOCC.CodigoRedencionPuntosDAO;
import capaDAOCC.FidelizacionRedencionDAO;
import capaDAOCC.FidelizacionReversaSolicitudDAO;
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
			int diasVigencia = ParametrosDAO.retornarValorNumerico("DIASVIGENCIAPUNTOS");
			acumula =  ClienteFidelizacionDAO.sumarPuntosClienteFidelizacion(correo, puntosSumar);
			FidelizacionTransaccion transaccion = new FidelizacionTransaccion(correo, idTienda, idPedidoTienda, valorNeto, puntosSumar);
			creaTransaccion = FidelizacionTransaccionDAO.insertarFidelizacionTransaccion(transaccion, diasVigencia);
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
	/**
	 * Version anterior, sin trazabilidad. Se conserva para no romper llamadas
	 * existentes.
	 *
	 * @deprecated usar la version que recibe tienda, pedido, usuario y origen.
	 */
	@Deprecated
	public double redimirPuntosClienteFidelizacion(String correo, double puntosRedimir)
	{
		return(redimirPuntosClienteFidelizacion(correo, puntosRedimir, 0, 0, "", ""));
	}

	/**
	 * Descuenta los puntos del cliente y deja el registro de la redencion con su
	 * trazabilidad: tienda, pedido, usuario que la proceso y canal de origen.
	 *
	 * @param correo         cliente del plan de fidelizacion
	 * @param puntosRedimir  puntos que se redimen
	 * @param idTienda       tienda donde se redimio; 0 si no aplica
	 * @param idPedidoTienda consecutivo del pedido en la tienda; 0 si no aplica
	 * @param usuario        usuario que proceso la redencion
	 * @param origen         POS o CC
	 * @return puntos que le quedan al cliente
	 */
	public double redimirPuntosClienteFidelizacion(String correo, double puntosRedimir, int idTienda,
			int idPedidoTienda, String usuario, String origen)
	{
		double puntosRestantes =  0;
		puntosRestantes=  ClienteFidelizacionDAO.redimirPuntosClienteFidelizacion(correo, puntosRedimir);
		//Instalar log de la redención
		ClienteFidelizacionDAO.insertarLogRedencion(correo, puntosRedimir, idTienda, idPedidoTienda, usuario, origen);
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
		String correoLider = ParametrosDAO.retornarValorAlfanumerico("CORREOLIDERCONTACT");
		String correoPqrs = ParametrosDAO.retornarValorAlfanumerico("CORREOPQRSCONTACT");
		Date fecha = new Date();
		Correo correo = new Correo();
		correo.setAsunto("Envio código validación redención puntos Pizza Americana" + fecha.toString());
		ArrayList correos = new ArrayList();
		correos.add(codRed.getCorreo());
		correos.add(correoLider);
		correos.add(correoPqrs);
		//Agregaremos los correos para lider y pqrs del contact center
		
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



	/**
	 * Version anterior, sin usuario ni origen. Se conserva para no romper llamadas
	 * existentes.
	 *
	 * @deprecated usar la version que recibe usuario y origen, para que la redencion
	 *             quede con trazabilidad de quien la proceso.
	 */
	@Deprecated
	public String realizarRedencionPuntos(String codigo, String  correo, double puntosRedimir, int idTienda, int idPedido)
	{
		return(realizarRedencionPuntos(codigo, correo, puntosRedimir, idTienda, idPedido, "", ""));
	}

	/**
	 * Realiza la redencion de puntos y deja constancia de quien la proceso.
	 *
	 * @param codigo        codigo de redencion que se valido
	 * @param correo        cliente del plan de fidelizacion
	 * @param puntosRedimir puntos a redimir
	 * @param idTienda      tienda donde se redime
	 * @param idPedido      consecutivo del pedido en la tienda
	 * @param usuario       usuario que procesa la redencion
	 * @param origen        POS o CC
	 * @return JSON con OK y los puntos restantes, o NOK1 si no hay puntos
	 *         suficientes, o NOK2 si el cliente no esta en el plan
	 */
	public String realizarRedencionPuntos(String codigo, String  correo, double puntosRedimir, int idTienda,
			int idPedido, String usuario, String origen)
	{
		return(realizarRedencionPuntos(codigo, correo, puntosRedimir, idTienda, idPedido, usuario, origen, false));
	}

	/**
	 * Realiza la redencion de puntos de forma atomica y deja registrado de
	 * cuales acumulaciones salio cada punto, que es lo que permite reversarla
	 * despues con exactitud.
	 *
	 * Antes esto eran cuatro operaciones en cuatro conexiones distintas, en
	 * autocommit: marcar el codigo, repartir el debito entre las acumulaciones,
	 * restar el saldo del cliente y dejar el log. Un fallo a mitad de camino
	 * dejaba las acumulaciones y el saldo descuadrados, en silencio. Ahora todo
	 * va en una transaccion dentro de FidelizacionRedencionDAO.
	 *
	 * @param reservar true deja la redencion en RESERVADA: los puntos ya estan
	 *                 descontados, pero la redencion es provisional hasta que
	 *                 el POS confirme que el pedido si quedo finalizado. false
	 *                 la deja CONFIRMADA de una vez, que es el comportamiento
	 *                 historico y el que sigue usando el POS que aun no se ha
	 *                 actualizado.
	 * @return JSON con respuesta OK, los puntos restantes y el idredencion; o
	 *         NOK1 sin puntos suficientes, NOK2 cliente fuera del plan, NOK3
	 *         error tecnico, NOK4 codigo inexistente o ya usado, NOK5 las
	 *         acumulaciones no cubren la redencion
	 */
	public String realizarRedencionPuntos(String codigo, String correo, double puntosRedimir, int idTienda,
			int idPedido, String usuario, String origen, boolean reservar)
	{
		JSONObject respuesta = new JSONObject();
		FidelizacionRedencionDAO.ResultadoRedencion resultado = FidelizacionRedencionDAO.ejecutarRedencion(codigo,
				correo, puntosRedimir, idTienda, idPedido, usuario, origen, reservar);
		if(!resultado.exitosa)
		{
			respuesta.put("respuesta", resultado.codigoError);
			respuesta.put("puntosrestantes", 0);
			respuesta.put("idredencion", 0);
			respuesta.put("detalle", resultado.detalleError);
			return(respuesta.toJSONString());
		}
		respuesta.put("respuesta", "OK");
		respuesta.put("puntosrestantes", resultado.puntosRestantes);
		respuesta.put("idredencion", resultado.idRedencion);
		respuesta.put("detalle", "");
		return(respuesta.toJSONString());
	}

	/**
	 * Confirma una redencion que quedo RESERVADA. La llama el POS cuando el
	 * pedido quedo finalizado de verdad, no cuando se abre la pantalla de pago.
	 */
	public String confirmarRedencionPuntos(int idRedencion, String usuario)
	{
		JSONObject respuesta = new JSONObject();
		boolean confirmada = FidelizacionRedencionDAO.confirmarRedencion(idRedencion, usuario);
		respuesta.put("respuesta", confirmada ? "OK" : "NOK");
		respuesta.put("detalle", confirmada ? ""
				: "No se encontro la redencion en estado RESERVADA; puede que ya estuviera confirmada o reversada");
		return(respuesta.toJSONString());
	}

	/**
	 * Devuelve los puntos de un pedido sin pasar por revision. Es para el caso
	 * en que el pedido NO se finalizo: no hubo entrega, asi que no hay nada que
	 * revisar y devolver los puntos es lo unico correcto.
	 *
	 * El caso del pedido ya finalizado que se anula NO va por aqui, va por
	 * solicitarReversaRedencion, porque ahi si hay riesgo de abuso.
	 *
	 * @param puntos puntos a devolver; 0 devuelve todo lo que quede pendiente
	 */
	public String reversarRedencionPedido(int idTienda, int idPedidoTienda, double puntos, String usuario,
			String motivo)
	{
		JSONObject respuesta = new JSONObject();
		FidelizacionRedencionDAO.RedencionPedido redencion = FidelizacionRedencionDAO
				.obtenerRedencionPorPedido(idTienda, idPedidoTienda);
		if(redencion == null)
		{
			// No es un error: la enorme mayoria de los pedidos no tienen redencion.
			respuesta.put("respuesta", "OK");
			respuesta.put("idredencion", 0);
			respuesta.put("puntosdevueltos", 0);
			respuesta.put("detalle", "El pedido no tiene una redencion pendiente por reversar");
			return(respuesta.toJSONString());
		}
		boolean reversada = FidelizacionRedencionDAO.reversarRedencion(redencion.idRedencion, puntos, usuario,
				motivo == null ? "" : motivo);
		respuesta.put("respuesta", reversada ? "OK" : "NOK");
		respuesta.put("idredencion", redencion.idRedencion);
		respuesta.put("puntosdevueltos", reversada
				? (puntos <= 0 ? redencion.puntosPendientesPorReversar() : puntos) : 0);
		respuesta.put("detalle", reversada ? "" : "No se pudo devolver los puntos de la redencion "
				+ redencion.idRedencion);
		return(respuesta.toJSONString());
	}

	/**
	 * Crea la solicitud de devolucion que pasa por aprobacion. Es el camino de
	 * los pedidos que SI se finalizaron: se anularon, o se les quito el producto
	 * de redencion. Los puntos no se mueven hasta que alguien apruebe.
	 *
	 * @param puntos puntos a devolver; si es una devolucion parcial, solo los
	 *               del producto que se quito
	 */
	public String solicitarReversaRedencion(int idTienda, int idPedidoTienda, double puntos, String motivo,
			String observacion, String usuario, String origen)
	{
		JSONObject respuesta = new JSONObject();
		FidelizacionRedencionDAO.RedencionPedido redencion = FidelizacionRedencionDAO
				.obtenerRedencionPorPedido(idTienda, idPedidoTienda);
		if(redencion == null)
		{
			respuesta.put("respuesta", "OK");
			respuesta.put("idsolicitud", 0);
			respuesta.put("detalle", "El pedido no tiene una redencion pendiente por reversar");
			return(respuesta.toJSONString());
		}
		double pendiente = redencion.puntosPendientesPorReversar();
		double solicitados = (puntos <= 0 || puntos > pendiente) ? pendiente : puntos;
		int idSolicitud = FidelizacionReversaSolicitudDAO.crearSolicitud(redencion.idRedencion, redencion.correo,
				redencion.idTienda, redencion.idPedidoTienda, solicitados, motivo, observacion, usuario, origen);
		respuesta.put("respuesta", idSolicitud > 0 ? "OK" : "NOK");
		respuesta.put("idsolicitud", idSolicitud);
		respuesta.put("puntossolicitados", solicitados);
		respuesta.put("detalle", idSolicitud > 0 ? "" : "No se pudo crear la solicitud de devolucion de puntos");
		return(respuesta.toJSONString());
	}

	/** Solicitudes de devolucion para la pantalla de revision. */
	public String obtenerSolicitudesReversa(String estado, String fechaDesde, String fechaHasta)
	{
		JSONArray solicitudes = new JSONArray();
		for(FidelizacionReversaSolicitudDAO.Solicitud s: FidelizacionReversaSolicitudDAO.obtenerSolicitudes(estado,
				fechaDesde, fechaHasta))
		{
			JSONObject fila = new JSONObject();
			fila.put("idsolicitud", s.idSolicitud);
			fila.put("idredencion", s.idRedencion);
			fila.put("correo", s.correo);
			fila.put("idtienda", s.idTienda);
			fila.put("tienda", s.nombreTienda);
			fila.put("idpedidotienda", s.idPedidoTienda);
			fila.put("puntossolicitados", s.puntosSolicitados);
			fila.put("motivo", s.motivo);
			fila.put("observacion", s.observacion);
			fila.put("usuariosolicita", s.usuarioSolicita);
			fila.put("origen", s.origen);
			fila.put("fechasolicitud", s.fechaSolicitud);
			fila.put("estado", s.estado);
			fila.put("usuariorevisa", s.usuarioRevisa);
			fila.put("fecharevision", s.fechaRevision);
			fila.put("observacionrevision", s.observacionRevision);
			fila.put("puntosredimidos", s.puntosRedimidos);
			fila.put("puntosreversados", s.puntosReversados);
			fila.put("estadoredencion", s.estadoRedencion);
			solicitudes.add(fila);
		}
		return(solicitudes.toJSONString());
	}

	/** Aprueba o rechaza una solicitud de devolucion de puntos. */
	public String resolverSolicitudReversa(int idSolicitud, boolean aprobar, String usuario, String observacion)
	{
		JSONObject respuesta = new JSONObject();
		String resultado = FidelizacionReversaSolicitudDAO.resolverSolicitud(idSolicitud, aprobar, usuario,
				observacion);
		respuesta.put("respuesta", "OK".equals(resultado) ? "OK" : "NOK");
		respuesta.put("detalle", "OK".equals(resultado) ? "" : resultado);
		return(respuesta.toJSONString());
	}

	/**
	 * Reversa directa usando el idredencion, que es el que devolvio la propia
	 * llamada de redencion. Es mas robusto que ubicarla por tienda y pedido:
	 * cuando el asesor cancela el pedido, el selector de tienda ya pudo haber
	 * cambiado, y despues de borrar el pedido no queda con que buscarla.
	 */
	public String reversarRedencionPorId(int idRedencion, double puntos, String usuario, String motivo)
	{
		JSONObject respuesta = new JSONObject();
		boolean reversada = FidelizacionRedencionDAO.reversarRedencion(idRedencion, puntos, usuario,
				motivo == null ? "" : motivo);
		respuesta.put("respuesta", reversada ? "OK" : "NOK");
		respuesta.put("idredencion", idRedencion);
		respuesta.put("detalle", reversada ? ""
				: "No se pudo devolver los puntos; puede que la redencion ya estuviera reversada");
		return(respuesta.toJSONString());
	}

	/** Cuantas solicitudes estan esperando revision. */
	public String contarSolicitudesReversaPendientes()
	{
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "OK");
		respuesta.put("pendientes", FidelizacionReversaSolicitudDAO.contarPendientes());
		return(respuesta.toJSONString());
	}


	public String insertarClienteNoFidelizacionn(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteNoFidelizacionDAO.insertarClienteNoFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
	}
	
	public String validarExistenciaClienteNoFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean existe = ClienteNoFidelizacionDAO.validarExistenciaClienteNoFidelizacion(correo);
		respuesta.put("respuesta", existe);
		return(respuesta.toJSONString());
	}
	
}

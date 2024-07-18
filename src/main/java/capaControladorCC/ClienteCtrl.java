package capaControladorCC;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.json.simple.*;
import org.json.simple.parser.*;

import capaDAOCC.ClienteAlertaDAO;
import capaDAOCC.ClienteDAO;
import capaDAOCC.MunicipioDAO;
import capaDAOCC.NomenclaturaDireccionDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.SolicitudPQRSDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Cliente;
import capaModeloCC.ClienteAlerta;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.Tienda;
import capaModeloCC.Ubicacion;
import utilidadesCC.ControladorEnvioCorreo;

/**
 * 
 * @author Juan David Botero Duque
 * Clase qeu tiene como objetivo en la capa de Controlador para todo el esquema de clientes
 */
public class ClienteCtrl {
	
	/**
	 * 
	 * @param tel recibe el valor del tel�fono para buscar al cliente en la base de datos y devolver en una estructura json
	 * las incripciones del cliente que corresponden al n�mero telef�nico entregado como par�metro.
	 * @return un valor String en formato json con el arreglo de inscripciones en la tabla cliente que responden al tel�fono
	 * enviado como par�metro
	 * NOTA: En la capa DAO validamos el estado del  cliente.
	 */
	public String obtenerCliente(String tel){
		JSONArray listJSON = new JSONArray();
		ArrayList<Cliente> clientes = ClienteDAO.obtenerCliente(tel);
		for (Cliente cliente : clientes) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("idCliente", cliente.getIdcliente());
			cadaViajeJSON.put("tienda", cliente.getTienda());
			cadaViajeJSON.put("nombre", cliente.getNombres());
			cadaViajeJSON.put("apellido", cliente.getApellidos());
			cadaViajeJSON.put("nombrecompania", cliente.getNombreCompania());
			cadaViajeJSON.put("direccion", cliente.getDireccion());
			cadaViajeJSON.put("zona", cliente.getZonaDireccion());
			cadaViajeJSON.put("observacion", cliente.getObservacion());
			cadaViajeJSON.put("telefono", cliente.getTelefono());
			cadaViajeJSON.put("municipio", cliente.getMunicipio());
			cadaViajeJSON.put("longitud", cliente.getLontitud());
			cadaViajeJSON.put("latitud", cliente.getLatitud());
			cadaViajeJSON.put("distanciatienda", cliente.getDistanciaTienda());
			cadaViajeJSON.put("memcode", cliente.getMemcode());
			cadaViajeJSON.put("idnomenclatura", cliente.getIdnomenclatura());
			cadaViajeJSON.put("numnomenclatura1", cliente.getNumNomenclatura());
			cadaViajeJSON.put("numnomenclatura2", cliente.getNumNomenclatura2());
			cadaViajeJSON.put("num3", cliente.getNum3());
			cadaViajeJSON.put("nomenclatura", cliente.getNomenclatura());
			cadaViajeJSON.put("zonatienda", cliente.getZonaTienda());
			cadaViajeJSON.put("telefonocelular", cliente.getTelefonoCelular());
			cadaViajeJSON.put("email", cliente.getEmail());
			cadaViajeJSON.put("politicadatos", cliente.getPoliticaDatos());
			cadaViajeJSON.put("fechanacimiento", cliente.getFechaNacimiento());
			cadaViajeJSON.put("clientesiniden", cliente.getClienteSinIden());
			cadaViajeJSON.put("emailfact", cliente.getEmailFacturacion());
			cadaViajeJSON.put("idtipopersona", cliente.getIdTipoPersona());
			cadaViajeJSON.put("identificacion", cliente.getIdentificacion());
			listJSON.add(cadaViajeJSON);
		}
		//String temp = listJSON.toJSONString();
		//temp = temp.substring(0,1) + "\"cliente\":[" + temp.substring(1, temp.length()-1) + "]]";
		//System.out.println(temp);
		//System.out.println(listJSON.toJSONString());
		return listJSON.toJSONString();
	}
	
	
	/**
	 * Metodo que se encargará de obtener la información de mercadeo de los clientes con base en el teléfono de los clientes que nos
	 * llaman, para así poder dar un mejor servicio comercial.
	 * @param tel
	 * @return
	 */
	public String obtenerMercadeoCliente(String tel){
		JSONObject datosMercadeo = new JSONObject();
	    String fechaPedido = PedidoDAO.obtenerFechaUltimoPedidoCliente(tel);
	    datosMercadeo.put("fechaultimopedido", fechaPedido);
	    int cantidadPedidos30 = PedidoDAO.obtenerCantidadPedidosClienteDiasAtras(tel, 30);
	    int cantidadPedidos90 = PedidoDAO.obtenerCantidadPedidosClienteDiasAtras(tel, 90);
	    int cantidadPedidos180 = PedidoDAO.obtenerCantidadPedidosClienteDiasAtras(tel, 180);
	    datosMercadeo.put("cantidadpedidos30", cantidadPedidos30);
	    datosMercadeo.put("cantidadpedidos90", cantidadPedidos90);
	    datosMercadeo.put("cantidadpedidos180", cantidadPedidos180);
	    int cantidadPQRS = SolicitudPQRSDAO.obtenerCantidadPQRS(tel);
	    datosMercadeo.put("cantidadpqrs", cantidadPQRS);
	    String especialidades = PedidoDAO.obtenerCantidadEspecialidadesCliente(tel);
	    datosMercadeo.put("especialidades", especialidades);
		return datosMercadeo.toJSONString();
	}
	
	/**
	 * M�todo en capa controlador que se encarga de retornar la info de un cliente en lo m�s actualizado posible 
	 * de su �ltimo pedido
	 * @param tel
	 * @return
	 */
	public  Cliente obtenerClienteUltimoPedido(String tel)
	{
		Cliente cliente = ClienteDAO.obtenerClienteUltimoPedido( tel);
		return(cliente);
	}
	
/**
 * M�todo que se encarga de validar si con el n�mero ingresado existen pedidos para el d�a en cuesti�n y si los hay que cantidad hay
 * @param tel
 * @return
 */
	public String validarTelefono(String tel){
		JSONObject respuesta = new JSONObject();
		int cantidadPed = PedidoDAO.validarTelefonoPedido(tel);
		respuesta.put("cantidad", cantidadPed);
		return (respuesta.toJSONString());
	}
	
	
	public String validarTelefonoPedRadicado(String tel){
		JSONObject respuesta = new JSONObject();
		int cantidadPed = PedidoDAO.validarTelefonoPedidoRadicado(tel);
		respuesta.put("cantidad", cantidadPed);
		return (respuesta.toJSONString());
	}
	
	/**
	 * 
	 * @param tel recibe el valor del tel�fono para buscar al cliente en la base de datos y devolver en una estructura json
	 * las incripciones del cliente que corresponden al n�mero telef�nico entregado como par�metro.
	 * @return un valor String en formato json con el arreglo de inscripciones en la tabla cliente que responden al tel�fono
	 * enviado como par�metro
	 * NOTA: En la capa DAO no para este en espec�fico validamos el estado del  cliente.
	 */
	public String obtenerClienteTodos(String tel){
		JSONArray listJSON = new JSONArray();
		ArrayList<Cliente> clientes = ClienteDAO.obtenerClienteTodos(tel);
		for (Cliente cliente : clientes) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("idCliente", cliente.getIdcliente());
			cadaViajeJSON.put("tienda", cliente.getTienda());
			cadaViajeJSON.put("nombre", cliente.getNombres());
			cadaViajeJSON.put("apellido", cliente.getApellidos());
			cadaViajeJSON.put("nombrecompania", cliente.getNombreCompania());
			cadaViajeJSON.put("direccion", cliente.getDireccion());
			cadaViajeJSON.put("zona", cliente.getZonaDireccion());
			cadaViajeJSON.put("observacion", cliente.getObservacion());
			cadaViajeJSON.put("telefono", cliente.getTelefono());
			cadaViajeJSON.put("municipio", cliente.getMunicipio());
			cadaViajeJSON.put("longitud", cliente.getLontitud());
			cadaViajeJSON.put("latitud", cliente.getLatitud());
			cadaViajeJSON.put("distanciatienda", cliente.getDistanciaTienda());
			cadaViajeJSON.put("memcode", cliente.getMemcode());
			cadaViajeJSON.put("idnomenclatura", cliente.getIdnomenclatura());
			cadaViajeJSON.put("numnomenclatura1", cliente.getNumNomenclatura());
			cadaViajeJSON.put("numnomenclatura2", cliente.getNumNomenclatura2());
			cadaViajeJSON.put("num3", cliente.getNum3());
			cadaViajeJSON.put("nomenclatura", cliente.getNomenclatura());
			cadaViajeJSON.put("estado", cliente.getEstado());
			cadaViajeJSON.put("telefonocelular", cliente.getTelefonoCelular());
			cadaViajeJSON.put("email", cliente.getEmail());
			cadaViajeJSON.put("politicadatos", cliente.getPoliticaDatos());
			listJSON.add(cadaViajeJSON);
		}
		//String temp = listJSON.toJSONString();
		//temp = temp.substring(0,1) + "\"cliente\":[" + temp.substring(1, temp.length()-1) + "]]";
		//System.out.println(temp);
		//System.out.println(listJSON.toJSONString());
		return listJSON.toJSONString();
	}
	
	public String inactivarCliente(int idCliente)
	{
		JSONArray listJSON = new JSONArray();
		boolean respuesta = ClienteDAO.inactivarCliente(idCliente);
		JSONObject Respuesta = new JSONObject();
		if(respuesta)
		{
			Respuesta.put("resultado", "EXITOSO");
		}else
		{
			Respuesta.put("resultado", "ERROR");
		}
		listJSON.add(Respuesta);
		return(listJSON.toString());
	}
	
	public String activarCliente(int idCliente)
	{
		JSONArray listJSON = new JSONArray();
		boolean respuesta = ClienteDAO.activarCliente(idCliente);
		JSONObject Respuesta = new JSONObject();
		if(respuesta)
		{
			Respuesta.put("resultado", "EXITOSO");
		}else
		{
			Respuesta.put("resultado", "ERROR");
		}
		listJSON.add(Respuesta);
		return(listJSON.toString());
	}
	
/**
 * 	
 * @param id Dado el id de un cliente se retorna la informaci�n del cliente
 * @return Se retorna en formato json la informaci�n del cliente que corresponde al id cliente, ingresado como par�metro.
 */
public String obtenerClienteporID(int id)
{
	Cliente clienteConsultado = ClienteDAO.obtenerClienteporID(id);
	JSONArray listJSON = new JSONArray();
	JSONObject Respuesta = new JSONObject();
	Respuesta.put("idcliente", clienteConsultado.getIdcliente());
	Respuesta.put("nombretienda", clienteConsultado.getTienda());
	Respuesta.put("nombrecliente", clienteConsultado.getNombres());
	Respuesta.put("direccion", clienteConsultado.getDireccion());
	Respuesta.put("numnomenclatura1", clienteConsultado.getNumNomenclatura());
	Respuesta.put("zona", clienteConsultado.getZonaDireccion());
	Respuesta.put("observacion", clienteConsultado.getObservacion());
	Respuesta.put("telefono", clienteConsultado.getTelefono());
	Respuesta.put("telefonocelular", clienteConsultado.getTelefonoCelular());
	Respuesta.put("email", clienteConsultado.getEmail());
	Respuesta.put("nombremunicipio", clienteConsultado.getMunicipio());
	Respuesta.put("latitud", clienteConsultado.getLatitud());
	Respuesta.put("longitud", clienteConsultado.getLontitud());
	Respuesta.put("distanciatienda", clienteConsultado.getDistanciaTienda());
	Respuesta.put("idtienda", clienteConsultado.getIdtienda());
	Respuesta.put("observacionvirtual", clienteConsultado.getObservacionVirtual());
	//Se agregan sobre la direccion
	Respuesta.put("nomenclatura", clienteConsultado.getNomenclatura());
	Respuesta.put("numnomenclatura1", clienteConsultado.getNumNomenclatura());
	Respuesta.put("numnomenclatura2", clienteConsultado.getNumNomenclatura2());
	Respuesta.put("num3", clienteConsultado.getNum3());
	listJSON.add(Respuesta);
	return(listJSON.toJSONString());
}

/**
 * M�todo que retorna el cliente en foramto objeto consultando por el id de la tabla cliente
 * @param id
 * @return
 */
public Cliente obtenerClienteporIDObj(int id)
{
	Cliente clienteConsultado = ClienteDAO.obtenerClienteporID(id);
	return(clienteConsultado);
}


/**
 * 	Este m�todo tiene como objetivo tomar los par�metros de un cliente para realizar la inserci�n
 * @param telefono par�metro de ingreso para la creaci�n de cliente
 * @param nombres par�metro de ingreso para la creaci�n de cliente
 * @param direccion par�metro de ingreso para la creaci�n de cliente
 * @param zona par�metro de ingreso para la creaci�n de cliente 
 * @param observacion par�metro de ingreso para la creaci�n de cliente
 * @param tienda par�metro de ingreso para la creaci�n de cliente
 * @return Se retorna en formato Json el id del cliente sea ingresado o actualizado.
 */
public String InsertarClientePedido(String telefono, String nombres, String direccion,  String zona,  String observacion, String tienda)
{
	//Validar si el cliente ya existe en la base de datos
	//Llamamos el m�todo ya existente para saber si el cliente con el tel�fono ya existe
	// Esta pendiente convertir el nombre tienda a tienda
	String retorno = "false";
	int idTienda = TiendaDAO.obteneridTienda(tienda);
	
	Cliente clienteInsertar = new Cliente(telefono, nombres, direccion, zona,  observacion,  tienda, idTienda);
	ArrayList<Cliente> clientes = ClienteDAO.obtenerCliente(telefono);
	boolean clienteTiendaExiste = false;
	int idRespuestaCreacion = 0;
	int idRespuestaActualizacion = 0;
	if (clientes.size() > 0)
	{
		for (Cliente cliente : clientes) 
		{
			
			if (cliente.getTienda().equals(clienteInsertar.getTienda()))
			{
				clienteTiendaExiste = true;
				idRespuestaActualizacion = ClienteDAO.actualizarCliente(clienteInsertar);
				break;
			}
			
		}
		if (clienteTiendaExiste == false)
		{
			idRespuestaCreacion = ClienteDAO.insertarCliente(clienteInsertar);
		}
	}
	else
	{
		//Deberemos insertar el cliente en la base de datos
		idRespuestaCreacion = ClienteDAO.insertarCliente(clienteInsertar);
	}
	if (idRespuestaActualizacion > 0 || idRespuestaCreacion > 0 )
	{
		retorno = "true";
	}
	JSONArray listJSON = new JSONArray();
	JSONObject Respuesta = new JSONObject();
	Respuesta.put("resultado", retorno);
	if (idRespuestaActualizacion > 0)
	{
		Respuesta.put("idcliente", idRespuestaActualizacion);
	}else
	{
		if (idRespuestaCreacion > 0)
		{
			Respuesta.put("idcliente", idRespuestaCreacion);
		}
	}
	listJSON.add(Respuesta);
	return(listJSON.toJSONString());
}

/**
 * Este m�todo recibe la informacion de un cliente y de acuerdo a los diferentes condiciones, actualiza o crea el cliente en la base de datos
 * @param idCliente 
 * @param telefono
 * @param nombres
 * @param apellidos
 * @param nombreCompania
 * @param direccion
 * @param municipio
 * @param latitud
 * @param longitud
 * @param zona
 * @param observacion
 * @param tienda
 * @param memcode
 * @return Retorna el idcliente ingresado o actualizado seg�n los datos recibidos como par�metro.
 */
public int InsertarClientePedidoEncabezado(int idCliente,String telefono, String nombres, String apellidos, String nombreCompania, String direccion, String municipio, float latitud, float longitud, double distanciaTienda, String zona,  String observacion, String tienda, int memcode, int idnomenclatura, String numNomenclatura, String numNomenclatura2, String num3, String telefonoCelular, String email, String politicaDatos, String fechaNacimiento, String clienteSinIden, String emailFact, int idTipoPersona, String identificacion )
{
	//Incluimos la l�gica para validar la latitud y longitud
	if(latitud == 0 && longitud == 0)
	{
		String geolocaliza = ParametrosDAO.retornarValorAlfanumerico("GEOLOCALIZACIONACTIVA");
		if(geolocaliza.equals(new String("S")))
		{
			UbicacionCtrl  ubicacion = new UbicacionCtrl();
			Ubicacion ubicaResp = ubicacion.ubicarDireccionEnTiendaBatch(direccion + " " + municipio);
			latitud = (float)ubicaResp.getLatitud();
			longitud = (float)ubicaResp.getLongitud();
		}
	}

	//Validar si el cliente ya existe en la base de datos
	//Llamamos el m�todo ya existente para saber si el cliente con el tel�fono ya existe
	// Esta pendiente convertir el nombre tienda a tienda
	int idTienda = TiendaDAO.obteneridTienda(tienda);
	int idMunicipio = MunicipioDAO.obteneridMunicipio(municipio);
	// Se crea el objeto cliente con todas las caracter�sticas enviadas
	Cliente clienteRevisar = new Cliente(idCliente, telefono, nombres, apellidos, nombreCompania, direccion,municipio, idMunicipio, latitud, longitud, distanciaTienda,  zona,  observacion,  tienda, idTienda, memcode, idnomenclatura, numNomenclatura, numNomenclatura2, num3, "", telefonoCelular, email, politicaDatos,fechaNacimiento,clienteSinIden, emailFact, idTipoPersona, identificacion);
	// Se inician las variables para la iniciaci�n de la creaci�n o la actualizaci�n
	int idRespuestaCreacion = 0;
	int idRespuestaActualizacion = 0;
	if ((idCliente > 0) && (memcode > 0) )
	{
		idRespuestaActualizacion = ClienteDAO.actualizarCliente(clienteRevisar);
	}else if((idCliente == 0 ) && (memcode == 0))
	{
		idRespuestaCreacion = ClienteDAO.insertarCliente(clienteRevisar);
	}else if((idCliente > 0 ) && (memcode == 0))
	{
		idRespuestaActualizacion = ClienteDAO.actualizarCliente(clienteRevisar);
	}
		
	if (idRespuestaActualizacion > 0)
	{
		return(idRespuestaActualizacion);
	}else
	{
		if (idRespuestaCreacion > 0)
		{
			return(idRespuestaCreacion);
		}
	}
	
	return(0);
}


public String InsertarClientePedidoEncabezadoJSON(int idCliente,String telefono, String nombres, String apellidos, String nombreCompania, String direccion, String municipio, float latitud, float longitud, double distanciaTienda, String zona,  String observacion, String tienda, int memcode, int idnomenclatura, String numNomenclatura, String numNomenclatura2, String num3, String telefonoCelular, String email, String politicaDatos, String fechaNacimiento, String clienteSinIden, String emailFact, int idTipoPersona, String identificacion )
{
	//Incluimos la l�gica para validar la latitud y longitud
	if(latitud == 0 && longitud == 0)
	{
		String geolocaliza = ParametrosDAO.retornarValorAlfanumerico("GEOLOCALIZACIONACTIVA");
		if(geolocaliza.equals(new String("S")))
		{
			UbicacionCtrl  ubicacion = new UbicacionCtrl();
			Ubicacion ubicaResp = ubicacion.ubicarDireccionEnTiendaBatch(direccion + " " + municipio);
			latitud = (float)ubicaResp.getLatitud();
			longitud = (float)ubicaResp.getLongitud();
		}
	}
	
	//Validar si el cliente ya existe en la base de datos
	//Llamamos el m�todo ya existente para saber si el cliente con el tel�fono ya existe
	// Esta pendiente convertir el nombre tienda a tienda
	int idTienda = TiendaDAO.obteneridTienda(tienda);
	int idMunicipio = MunicipioDAO.obteneridMunicipio(municipio);
	// Se crea el objeto cliente con todas las caracter�sticas enviadas
	Cliente clienteRevisar = new Cliente(idCliente, telefono, nombres, apellidos, nombreCompania, direccion,municipio, idMunicipio, latitud, longitud, distanciaTienda, zona,  observacion,  tienda, idTienda, memcode, idnomenclatura, numNomenclatura, numNomenclatura2, num3, "", telefonoCelular, email, politicaDatos, fechaNacimiento,clienteSinIden,emailFact, idTipoPersona, identificacion);
	// Se inician las variables para la iniciaci�n de la creaci�n o la actualizaci�n
	int idRespuestaCreacion = 0;
	int idRespuestaActualizacion = 0;
	int idClienteResp = 0;
	if ((idCliente > 0) && (memcode > 0) )
	{
		idRespuestaActualizacion = ClienteDAO.actualizarCliente(clienteRevisar);
	}else if((idCliente == 0 ) && (memcode == 0))
	{
		idRespuestaCreacion = ClienteDAO.insertarCliente(clienteRevisar);
	}else if((idCliente > 0 ) && (memcode == 0))
	{
		idRespuestaActualizacion = ClienteDAO.actualizarCliente(clienteRevisar);
	}
		
	if (idRespuestaActualizacion > 0)
	{
		idClienteResp = idRespuestaActualizacion;
	}else
	{
		if (idRespuestaCreacion > 0)
		{
			idClienteResp = idRespuestaCreacion;
		}
	}
	JSONArray listJSON = new JSONArray();
	JSONObject Respuesta = new JSONObject();
	Respuesta.put("idcliente", idClienteResp);
	listJSON.add(Respuesta);
	return(listJSON.toJSONString());
}



public String ObtenerClientesTienda(int idtienda)
{
	ArrayList <Cliente> clientesTienda = ClienteDAO.ObtenerClientesTienda(idtienda);
	JSONArray listJSON = new JSONArray();
	for(Cliente clienteConsultado: clientesTienda)
	{
		
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("idcliente", clienteConsultado.getIdcliente());
		Respuesta.put("nombretienda", clienteConsultado.getTienda());
		Respuesta.put("nombrecliente", clienteConsultado.getNombres());
		Respuesta.put("direccion", clienteConsultado.getDireccion());
		Respuesta.put("zona", clienteConsultado.getZonaDireccion());
		Respuesta.put("observacion", clienteConsultado.getObservacion());
		Respuesta.put("telefono", clienteConsultado.getTelefono());
		Respuesta.put("nombremunicipio", clienteConsultado.getMunicipio());
		Respuesta.put("latitud", clienteConsultado.getLatitud());
		Respuesta.put("longitud", clienteConsultado.getLontitud());
		Respuesta.put("idtienda", clienteConsultado.getIdtienda());
		listJSON.add(Respuesta);
	}
	return(listJSON.toJSONString());
	
}

public String InsertarClienteGeolocalizado(int idcliente,  String direccion, String municipio, int idtiendaanterior, int idtiendaactual)
{
	JSONArray listJSON = new JSONArray();
	JSONObject Respuesta = new JSONObject();
	boolean resp = ClienteDAO.InsertarClienteGeolocalizado(idcliente,  direccion, municipio, idtiendaanterior, idtiendaactual);
	Respuesta.put("resultado", resp);
	listJSON.add(Respuesta);
	return(listJSON.toJSONString());
}


public String obtenerNotificacionesCliente(int idCliente)
{
	JSONObject respuesta = new JSONObject();
	ArrayList<Integer> pqrsCliente = SolicitudPQRSDAO.obtenerPQRSCliente(idCliente);
	String[] infoPedido = PedidoDAO.obtenerUltimoPedidoCliente(idCliente);
	if(pqrsCliente.size() > 0)
	{
		String pqrs = " Ha radicado PQRS ";
		Integer cadaPQRS;
		for(int i = 0; i < pqrsCliente.size(); i++)
		{
			cadaPQRS = pqrsCliente.get(i);
			pqrs = pqrs + "," + cadaPQRS.toString();
		}
		respuesta.put("pqrs", pqrs);
	}else
	{
		respuesta.put("pqrs", "NO");
	}
	//Validamos que tenga �ltima fecha pedido
	if(infoPedido[0]== "")
	{
		
	}else
	{
		respuesta.put("idpedido", infoPedido[0]);
		respuesta.put("ultimopedido", infoPedido[1]);
		respuesta.put("formapago", infoPedido[2]);
		respuesta.put("tiempopedido", infoPedido[3]);
	}
	return(respuesta.toJSONString());
}

/**
 * M�todo que se encagar� de realizar un procesamiento completo del cliente y la informaci�n recibida del cliente que pidi� virtualmente
 * @param clienteVirtual
 */
	public int validarClienteTiendaVirtual(Cliente clienteVirtual)
	{
		int idCliente = 0;
		//Definimos la variable con las alertas de actualizacion
		String alertaActualizacion = "";
		/**
		 * 1.Regularizar la data del cliente virtual correspondiente con la direcci�n que la trae toda junta
		 */
		normalizarDireccionClienteVirtual(clienteVirtual);
		
		/**
		 * 2. A continuaci�n toda la l�gica seguida en la implementaci�n de este m�todo, comenzamos por 
		 * verificar si el cliente ya existe, pues en caso de que ya existe pues haremos una actualizaci�n 
		 * del cliente existente, en caso de que no exista, haremos la creaci�n.
		 */
		boolean esTelefonoFijo = false;
		if(clienteVirtual.getTelefono().substring(0,3).equals(new String("034")))
		{
			esTelefonoFijo = true;
		}
		Cliente clienteEncontrado = ClienteDAO.obtenerCliente(clienteVirtual.getTelefono(), clienteVirtual.getIdtienda());
		//En caso de no haber encontrado el cliente y no ser un fijo, buscamos si tenemos el cliente con celular
		if(clienteEncontrado.getIdcliente() == 0 && !esTelefonoFijo)
		{
			clienteEncontrado = ClienteDAO.obtenerClienteCelular(clienteVirtual.getTelefonoCelular(), clienteVirtual.getIdtienda());
		}
		/**
		 * 2. En caso de que el cliente exista tomaremos el idCliente y comenzaremos a realizar una actualizaci�n "inteligente, dejando un log detallado".
		 * 
		 */
		if(clienteEncontrado.getIdcliente() > 0)
		{
			clienteVirtual.setIdcliente(clienteEncontrado.getIdcliente());
			//Procederemos con la actualizaci�n dejando un rastro de lo encontrado
			alertaActualizacion = prepararActClienteTiendaVirtual(clienteVirtual, clienteEncontrado);
			//Fijamos el valor de las observaciones virtuales
			clienteVirtual.setObservacionVirtual(clienteVirtual.getObservacionVirtual() + alertaActualizacion);
			//Realizamos la actualizaci�n del cliente
			idCliente = ClienteDAO.actualizarCliente(clienteVirtual);
		}else
		{
			idCliente = ClienteDAO.insertarCliente(clienteVirtual);
		}
		return(idCliente);
	}
	
	
	
	
	/**
	 * Tenemos en tienda virtual KUNO que revisar como llegar� la direcci�n dado que no ser� est�ndar y posiblemente
	 * no hay forma de hacer la separaci�n.
	 * @param clienteVirtual
	 * @return
	 */
	public int validarClienteTiendaVirtualKuno(Cliente clienteVirtual, String instrucciones)
	{
		int idCliente = 0;
		//Definimos la variable con las alertas de actualizacion
		String alertaActualizacion = "";
		/**
		 * 1.Regularizar la data del cliente virtual correspondiente con la direcci�n que la trae toda junta
		 */
		normalizarDireccionClienteVirtualKuno(clienteVirtual);
		
		/**
		 * 2. A continuaci�n toda la l�gica seguida en la implementaci�n de este m�todo, comenzamos por 
		 * verificar si el cliente ya existe, pues en caso de que ya existe pues haremos una actualizaci�n 
		 * del cliente existente, en caso de que no exista, haremos la creaci�n.
		 */
		boolean esTelefonoFijo = false;
		if(clienteVirtual.getTelefono().substring(0,3).equals(new String("034")))
		{
			esTelefonoFijo = true;
		}
		Cliente clienteEncontrado = ClienteDAO.obtenerClienteTiendaVirtual(clienteVirtual.getTelefono(), clienteVirtual.getIdtienda());
		//En caso de no haber encontrado el cliente y no ser un fijo, buscamos si tenemos el cliente con celular
		if(clienteEncontrado.getIdcliente() == 0 && !esTelefonoFijo)
		{
			clienteEncontrado = ClienteDAO.obtenerClienteCelularTiendaVirtual(clienteVirtual.getTelefonoCelular(), clienteVirtual.getIdtienda());
		}
		/**
		 * 2. En caso de que el cliente exista tomaremos el idCliente y comenzaremos a realizar una actualizaci�n "inteligente, dejando un log detallado".
		 * 
		 */
		if(clienteEncontrado.getIdcliente() > 0)
		{
			clienteVirtual.setIdcliente(clienteEncontrado.getIdcliente());
			//Procederemos con la actualizaci�n dejando un rastro de lo encontrado
			alertaActualizacion = prepararActClienteTiendaVirtual(clienteVirtual, clienteEncontrado);
			//Fijamos el valor de las observaciones virtuales
			clienteVirtual.setObservacionVirtual(clienteVirtual.getObservacionVirtual() + alertaActualizacion + " " + instrucciones);
			//Realizamos la actualizaci�n del cliente
			idCliente = ClienteDAO.actualizarCliente(clienteVirtual);
		}else
		{
			idCliente = ClienteDAO.insertarCliente(clienteVirtual);
		}
		return(idCliente);
	}
	
	/**
	 * Este m�todo con base en un cliente existente vs el cliente que viene de la tienda virtual se encarga de comparar la informaci�n
	 * de la direcci�n y dejar anotaci�n de las igualdades y diferencias.
	 * @param clienteVirtual
	 * @param clienteExistente
	 * @return
	 */
	public String prepararActClienteTiendaVirtual(Cliente clienteVirtual, Cliente clienteExistente)
	{
		//Se tendr� este String que guardara todas aquellas anotaciones importantes de la actualizaci�n del cliente
		String alertasAct = "CLIENTE YA EXISTIA Y SE ACTUALIZ�.";
		try
		{
			//Validamos que el anterior tenga nombre compa�ia para si es el caso colocarselo al nuevo
			if(clienteExistente.getNombreCompania().length() > 0)
			{
				if(clienteVirtual.getNombreCompania().length() == 0)
				{
					clienteVirtual.setNombreCompania(clienteExistente.getNombreCompania());
					alertasAct = alertasAct + " Se encontr� que el cliente ten�a informaci�n en el campo nombre Compa�ia y se dejo dicha informaci�n.";
				}	
			}
			//Tendremos un contador de equivalencias en la direcci�n
			int equivalencias = 0;
			if(clienteVirtual.getIdnomenclatura() == clienteExistente.getIdnomenclatura())
			{
				equivalencias++;
			}
			if(clienteVirtual.getNumNomenclatura().toLowerCase().equals( new String(clienteExistente.getNumNomenclatura().toLowerCase())))
			{
				equivalencias++;
			}
			if(clienteVirtual.getNumNomenclatura2().toLowerCase().equals(new String(clienteExistente.getNumNomenclatura2().toLowerCase())))
			{
				equivalencias++;
			}
			if(clienteVirtual.getNum3().toLowerCase().equals(new String(clienteExistente.getNum3().toLowerCase())))
			{
				equivalencias++;
			}
			if(clienteVirtual.getIdMunicipio() == clienteExistente.getIdMunicipio())
			{
				equivalencias++;
			}
			if(equivalencias == 5)
			{
				alertasAct = alertasAct + " La direccion que ten�amos del cliente coincide exactamente con la que ten�amos antes";
			}else
			{
				if(clienteVirtual.getNumNomenclatura().equals(new String("0")))
				{
					alertasAct = alertasAct + " ATENCI�N!! SE DEBE REVISAR LA DIRECCI�N DEL CLIENTE Y NORMALIZARLA TIENE PROBLEMAS.";
				}else
				{
					alertasAct = alertasAct + " La direcci�n tiene diferencias, la anterior es " + clienteExistente.getDireccion() + " y la nueva es " + clienteVirtual.getDireccion();
				}	
			}
		}catch(Exception e)
		{
			alertasAct = alertasAct + " ALERTA! El cliente realiz� ingreso de la direccion en un formato diferente al esperado, se debe verificar la direcci�n antes de enviar.";
		}
		return(alertasAct);
	}
	
	/**
	 * Este m�todo se encargar� de normalizar la direcci�n para el cliente virtual que en cuyo caso la direcci�n viene toda junta
	 * y debe ser separada por campos.
	 * @param clienteVirtual
	 */
	public void normalizarDireccionClienteVirtual(Cliente clienteVirtual)
	{
		//Incluimos todo en un try/catch, con el fin de que si se presenta erorr, alertar la situaci�n
		try
		{
			//La direcci�n viene completa por lo tanto deber� ser descompuesta para ser almacenada de la forma correcta y ser validada con la existente
			//Sacamos los campos para la descomposici�n de la direcci�n
			String nomenclatura = "";
			int idNomenclatura;
			String numNomencla1="";
			String numNomencla2="";
			String num3 = "";
			String municipio = "";
			int idMunicipio;
			//Comenzamos con la direcci�n base que se trae del sitio virtual
			String direccionBase = clienteVirtual.getDireccion();
			//Vamos a extraer la nomemclatura como primer paso.
			StringTokenizer strTokenDir = new StringTokenizer(direccionBase, " ");
			while(strTokenDir.hasMoreTokens())
			{
				nomenclatura = strTokenDir.nextToken();
				break;
			}
			//Posteriormente dada la nomenclatura validamos el idNomenclatura
			idNomenclatura = NomenclaturaDireccionDAO.obtenerIdNomenclatura(nomenclatura);
			//Si la nomenclatura es cero env�amos correo
			if(idNomenclatura == 0)
			{
				//Enviaremos un correo en la etapa de piloto
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
				ArrayList correos = new ArrayList();
				correo.setAsunto("TIENDA VIRTUAL ERROR EN LA DIRECCI�N NOMENCLATURA VAC�A   " + clienteVirtual.getDireccion());
				String correoEle = "jubote1@gmail.com";
				correos.add(correoEle);
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje(" Se tuvo problema creando la orden del cliente  en la direcci�n " + clienteVirtual.getDireccion());
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
			}
			direccionBase = direccionBase.substring(nomenclatura.length());
			//Realizamos la separaci�n de los n�meros de la direcci�n
			strTokenDir = new StringTokenizer(direccionBase , "#");
			while(strTokenDir.hasMoreTokens())
			{
				numNomencla1 = strTokenDir.nextToken().trim();
				numNomencla2 = strTokenDir.nextToken().trim();
				break;
			}
			//Continuamos separando la segunda parte de los n�meros de la direcci�n
			strTokenDir = new StringTokenizer(numNomencla2, "-");
			while(strTokenDir.hasMoreTokens())
			{
				numNomencla2 = strTokenDir.nextToken().trim();
				num3 = strTokenDir.nextToken().trim();
				break;
			}
			//Terminamos la Extracci�n del �ltimo n�mero y el municipio
			strTokenDir = new StringTokenizer(num3, " ");
			while(strTokenDir.hasMoreTokens())
			{
				num3 = strTokenDir.nextToken().trim();
				municipio = strTokenDir.nextToken().trim();
				break;
			}
			//Realizamos un control para los municipios que tienen m�s de una letra
			while(strTokenDir.hasMoreTokens())
			{
				municipio = municipio + " " + strTokenDir.nextToken().trim();
			}
			idMunicipio = MunicipioDAO.obteneridMunicipio(municipio);
			if(idMunicipio == 0)
			{
				idMunicipio = 1;
				clienteVirtual.setObservacionVirtual(clienteVirtual.getObservacionVirtual() + " Se tuvo problema en la conversi�n de la direcci�n FAVOR VERIFICA LA DIRECCI�N.");
			}
			clienteVirtual.setIdnomenclatura(idNomenclatura);
			clienteVirtual.setNumNomenclatura(numNomencla1);
			clienteVirtual.setNumNomenclatura2(numNomencla2);
			clienteVirtual.setNum3(num3);
			clienteVirtual.setIdMunicipio(idMunicipio);
		}catch(Exception e)
		{
			//Si hay un inconveniente igual se reportar� el correo pero se asignar�n valores
			clienteVirtual.setIdnomenclatura(1);
			clienteVirtual.setNumNomenclatura("0");
			clienteVirtual.setNumNomenclatura2("0");
			clienteVirtual.setNum3("0");
			clienteVirtual.setIdMunicipio(1);
			//Fin de llenado por error
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
			correo.setAsunto("ERROR NORMALIZANDO CLIENTE VIRTUAL  " + clienteVirtual.getTelefono());
			ArrayList correos = new ArrayList();
			String correoEle = "jubote1@gmail.com";
			correos.add(correoEle);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje(" Se present� problema en la normalizaci�n de la direcci�n del cliente con tel�fono " + clienteVirtual.getTelefono() );
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
		}
		
			
	}
	
	
	/**
	 * La normalizaci�n de la direcci�n con KUNO es bastante diferente dado que como tal no hay normalizaci�n
	 * @param clienteVirtual
	 */
	public void normalizarDireccionClienteVirtualKuno(Cliente clienteVirtual)
	{
		//Incluimos todo en un try/catch, con el fin de que si se presenta erorr, alertar la situaci�n
		try
		{
			//La direcci�n viene completa por lo tanto deber� ser descompuesta para ser almacenada de la forma correcta y ser validada con la existente
			//Sacamos los campos para la descomposici�n de la direcci�n
			int idNomenclatura;
			String municipio = "";
			int idMunicipio;
			//Comenzamos con la direcci�n base que se trae del sitio virtual
			String direccionBase = clienteVirtual.getDireccion();
			idNomenclatura = 1;
			idMunicipio = 1;
			clienteVirtual.setIdnomenclatura(idNomenclatura);
			clienteVirtual.setIdMunicipio(idMunicipio);
		}catch(Exception e)
		{
		}
		
			
	}
	
	public void actualizarClienteCoordenadas(int idCliente, float latitud, float longitud)
	{
		ClienteDAO.actualizarClienteCoordenas(idCliente, latitud, longitud);
	}

	public String actualizarClienteDireccion(int idCliente, String direccion, String municipio, float latitud, float longitud, String zona,  String observacion, int idnomenclatura, String numNomenclatura, String numNomenclatura2, String num3 )
	{
		int idMunicipio = MunicipioDAO.obteneridMunicipio(municipio);
		ClienteDAO.actualizarClienteDireccion(idCliente, direccion, idMunicipio, latitud, longitud, zona, observacion, idnomenclatura, numNomenclatura, numNomenclatura2, num3);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "true");
		return(respuesta.toJSONString());
	}
	
	public String obtenerClienteAlerta(String telefono)
	{
		ClienteAlerta clienteAlerta = ClienteAlertaDAO.obtenerClienteAlerta(telefono);
		JSONObject respuesta = new JSONObject();
		respuesta.put("telefono", clienteAlerta.getTelefono());
		respuesta.put("tipo_alerta", clienteAlerta.getTipoAlerta());
		respuesta.put("mensaje", clienteAlerta.getMensaje());
		return(respuesta.toJSONString());
	}

}

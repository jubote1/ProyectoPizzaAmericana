package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import capaModeloCC.Cliente;
import capaDAOCC.ClienteDAO;
import capaDAOCC.ClienteFidelizacionDAO;
import capaDAOCC.FidelizacionTransaccionDAO;
import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.FidelizacionTransaccion;

public class FidelizacionCtrl {
	
	
	public String existeClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean existe = ClienteFidelizacionDAO.existeClienteFidelizacion(correo);
		respuesta.put("respuesta", existe);
		return(respuesta.toJSONString());
	}
	
	public String insertarClienteFidelizacion(String correo)
	{
		JSONObject respuesta = new JSONObject();
		boolean inserto = ClienteFidelizacionDAO.insertarClienteFidelizacion(correo);
		respuesta.put("respuesta", inserto);
		return(respuesta.toJSONString());
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
	        boolean inserto = ClienteFidelizacionDAO.insertarClienteFidelizacion(correo);
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
	        System.out.println("Cliente registrado con ID: " + id);
	        return id > 0;
	    } else {
	        cliente.setIdcliente(idCliente);
	        boolean actualizado = ClienteDAO.actualizarClienteWb(cliente);
	        System.out.println("Cliente actualizado: " + actualizado);
	        return actualizado;
	    }
	}




}

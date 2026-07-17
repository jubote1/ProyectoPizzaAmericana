package capaControladorCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.ClienteDAO;
import capaDAOCC.CodigoRedencionPuntosDAO;
import capaDAOCC.EmpleadoRemotoValeDAO;
import capaDAOCC.ExcepcionPrecioDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.LogRedencionCodigoDAO;
import capaDAOCC.OfertaClienteDAO;
import capaDAOCC.OfertaDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoPrecioEmpleadoDAO;
import capaModeloCC.Cliente;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.MensajeTexto;
import capaModeloCC.Oferta;
import capaModeloCC.OfertaCliente;
import conexionCC.ConexionBaseDatos;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import utilidadesCC.ControladorEnvioCorreo;

public class PromocionesCtrl {
	
	//ENTIDAD OFERTAS CLIENTE
	
	public String obtenerOfertasClienteGrid(int idCliente)
	{
		ArrayList<OfertaCliente> ofertas = OfertaClienteDAO.obtenerOfertasClienteGrid(idCliente);
		JSONArray listJSON = new JSONArray();
		for (OfertaCliente ofer : ofertas) 
		{
			JSONObject cadaOfertaJSON = new JSONObject();
			cadaOfertaJSON.put("idofertacliente", ofer.getIdOfertaCliente());
			cadaOfertaJSON.put("idcliente", ofer.getIdCliente());
			cadaOfertaJSON.put("idoferta", ofer.getIdOferta());
			cadaOfertaJSON.put("nombreoferta", ofer.getNombreOferta());
			cadaOfertaJSON.put("utilizada", ofer.getUtilizada());
			cadaOfertaJSON.put("ingresooferta", ofer.getIngresoOferta());
			cadaOfertaJSON.put("usooferta", ofer.getUsoOferta());
			cadaOfertaJSON.put("observacion", ofer.getObservacion());
			cadaOfertaJSON.put("pqrs", ofer.getPQRS());
			listJSON.add(cadaOfertaJSON);
		}
		return(listJSON.toJSONString());
	}
	
	public  String obtenerOfertasVigenteCliente(int idCliente)
	{
		ArrayList<OfertaCliente> ofertas = OfertaClienteDAO.obtenerOfertasVigenteCliente(idCliente);
		JSONArray listJSON = new JSONArray();
		for (OfertaCliente ofer : ofertas) 
		{
			JSONObject cadaOfertaJSON = new JSONObject();
			cadaOfertaJSON.put("idofertacliente", ofer.getIdOfertaCliente());
			cadaOfertaJSON.put("idcliente", ofer.getIdCliente());
			cadaOfertaJSON.put("idoferta", ofer.getIdOferta());
			cadaOfertaJSON.put("nombreoferta", ofer.getNombreOferta());
			cadaOfertaJSON.put("utilizada", ofer.getUtilizada());
			cadaOfertaJSON.put("ingresooferta", ofer.getIngresoOferta());
			cadaOfertaJSON.put("usooferta", ofer.getUsoOferta());
			cadaOfertaJSON.put("observacion", ofer.getObservacion());
			cadaOfertaJSON.put("pqrs", ofer.getPQRS());
			listJSON.add(cadaOfertaJSON);
		}
		return(listJSON.toJSONString());
	}
	
	/**
	 * Este m�todo debe realizar validaciones para mirar si la oferta maneja codigo promocional, en cuyo caso deber� asignar una
	 * @param ofer
	 * @return
	 */
	public String insertarOfertaCliente(OfertaCliente ofer)
	{
		//Validamos si la oferta maneja c�digo promocional
		Oferta condicionesOferta = OfertaDAO.retornarOferta(ofer.getIdOferta());
		boolean manejaCodigo;
		if(condicionesOferta.getCodigoPromocional().equals(new String("S")))
		{
			manejaCodigo = true;
		}else
		{
			manejaCodigo = false;
		}
		//boolean manejaCodigo = OfertaDAO.manejaCodigoOferta(ofer.getIdOferta());
		
		//Incluimos l�gica para verificar si el campo de saldo en la oferta debe ser llenadod
		if(condicionesOferta.getRedParcial().equals(new String("S")))
		{
			if(condicionesOferta.getDescuentoFijoValor() > 0 )
			{
				ofer.setSaldo(condicionesOferta.getDescuentoFijoValor());
			}
		}else
		{
			ofer.setSaldo(0);
		}
		
		String codigoPromocional = "";
		if(manejaCodigo)
		{
			codigoPromocional = generarCodigoPromocional();
		}
		//Fijamos el valor de codigo promocional que puede ser vac�o o contener valor
		ofer.setCodigoPromocion(codigoPromocional);
		//Validamos si la oferta maneja caducidad y como la maneja
		
		//Validamos si la oferta tiene caducidad en caso afirmativo
		if(condicionesOferta.getDiasCaducidad() > 0)
		{
			//Validamos el tipo de caducidad de la oferta si es general o particular (por el momento la general no est� implementada)
			if(condicionesOferta.getTipoCaducidad().equals(new String("P")))
			{
				//Creamos el calendario y le sumamos a la fecha actual los d�as de caducidad
				Calendar calendarioActual = Calendar.getInstance();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				calendarioActual.add(Calendar.DAY_OF_YEAR, condicionesOferta.getDiasCaducidad());
				//Extraemos la fecha caducidad
				Date datFechaCaducidad = calendarioActual.getTime();
				String fechaCaducidad = dateFormat.format(datFechaCaducidad);
				ofer.setFechaCaducidad(fechaCaducidad);
			}
		}
		JSONArray listJSON = new JSONArray();
		JSONObject ResultadoJSON = new JSONObject();
		int respuesta = OfertaClienteDAO.insertarOfertaCliente(ofer);
		ResultadoJSON.put("idofertacliente", respuesta);
		//En este punto una vez hagamos la asignaci�n de la oferta realizaremos la notificaci�n de las ofertas
		enviarMensajesOferta(ofer.getIdOferta());
		listJSON.add(ResultadoJSON);
		System.out.println(listJSON.toJSONString());
		return listJSON.toJSONString();
	}
	
	
	/**
	 * Método que inserta la oferta cliente futura para el cliente, luego de una compra
	 * @param ofer
	 * @return
	 */
	public String insertarOfertaClienteFuturo(OfertaCliente ofer)
	{
		//Validamos si la oferta maneja c�digo promocional
		Oferta condicionesOferta = OfertaDAO.retornarOferta(ofer.getIdOferta());
		//Hacemos validación si la oferta si está bien configurada y tiene los parámetros correctos
		if(condicionesOferta.getDescuentoPorcentajeFuturo() == 0 && ofer.getBaseDescuento() == 0)
		{
			//Se termina porque no hay manera hacer el proceso
			JSONArray listJSON = new JSONArray();
			JSONObject ResultadoJSON = new JSONObject();
			ResultadoJSON.put("idofertacliente", 0);
			listJSON.add(ResultadoJSON);
			return listJSON.toJSONString();
		}
		boolean manejaCodigo;
		if(condicionesOferta.getCodigoPromocional().equals(new String("S")))
		{
			manejaCodigo = true;
		}else
		{
			manejaCodigo = false;
		}
		//boolean manejaCodigo = OfertaDAO.manejaCodigoOferta(ofer.getIdOferta());
		
		//Incluimos l�gica para verificar si el campo de saldo en la oferta debe ser llenadod
		if(condicionesOferta.getRedParcial().equals(new String("S")))
		{
			if(condicionesOferta.getDescuentoFijoValor() > 0 )
			{
				ofer.setSaldo(condicionesOferta.getDescuentoFijoValor());
			}
		}else if(condicionesOferta.getDescuentoPorcentajeFuturo() > 0 && ofer.getBaseDescuento() > 0)
		{
			ofer.setSaldo(ofer.getBaseDescuento()*condicionesOferta.getDescuentoPorcentajeFuturo());
		}else
		{
			ofer.setSaldo(0);
		}
		
		String codigoPromocional = "";
		if(manejaCodigo)
		{
			codigoPromocional = generarCodigoPromocional();
		}
		//Fijamos el valor de codigo promocional que puede ser vac�o o contener valor
		ofer.setCodigoPromocion(codigoPromocional);
		//Validamos si la oferta maneja caducidad y como la maneja
		
		//Validamos si la oferta tiene caducidad en caso afirmativo
		if(condicionesOferta.getDiasCaducidad() > 0)
		{
			//Validamos el tipo de caducidad de la oferta si es general o particular (por el momento la general no est� implementada)
			if(condicionesOferta.getTipoCaducidad().equals(new String("P")))
			{
				//Creamos el calendario y le sumamos a la fecha actual los d�as de caducidad
				Calendar calendarioActual = Calendar.getInstance();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				calendarioActual.add(Calendar.DAY_OF_YEAR, condicionesOferta.getDiasCaducidad());
				//Extraemos la fecha caducidad
				Date datFechaCaducidad = calendarioActual.getTime();
				String fechaCaducidad = dateFormat.format(datFechaCaducidad);
				ofer.setFechaCaducidad(fechaCaducidad);
			}
		}
		JSONArray listJSON = new JSONArray();
		JSONObject ResultadoJSON = new JSONObject();
		int respuesta = OfertaClienteDAO.insertarOfertaCliente(ofer);
		ResultadoJSON.put("idofertacliente", respuesta);
		//En este punto una vez hagamos la asignaci�n de la oferta realizaremos la notificaci�n de las ofertas
		//enviarMensajesOferta(ofer.getIdOferta());
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	public String eliminarOfertaCliente(int idOfertaCliente)
	{
		JSONArray listJSON = new JSONArray();
		OfertaClienteDAO.eliminarOfertaCliente(idOfertaCliente);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", "exitoso");
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	public  String retornarOfertaCliente(int idOfertaCliente)
	{
		
		JSONArray listJSON = new JSONArray();
		OfertaCliente oferCliente = OfertaClienteDAO.retornarOfertaCliente(idOfertaCliente);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idofertacliente", oferCliente.getIdOfertaCliente());
		ResultadoJSON.put("idcliente", oferCliente.getIdCliente());
		ResultadoJSON.put("idoferta", oferCliente.getIdOferta());
		ResultadoJSON.put("nombreoferta", oferCliente.getNombreOferta());
		ResultadoJSON.put("utilizada", oferCliente.getUtilizada());
		ResultadoJSON.put("ingresooferta", oferCliente.getIngresoOferta());
		ResultadoJSON.put("usooferta", oferCliente.getUsoOferta());
		ResultadoJSON.put("observacion", oferCliente.getObservacion());
		ResultadoJSON.put("pqrs", oferCliente.getPQRS());
		ResultadoJSON.put("nombrecliente", oferCliente.getNombreCliente());
		ResultadoJSON.put("codigopromocion", oferCliente.getCodigoPromocion());
		ResultadoJSON.put("redparcial", oferCliente.getRedParcial());
		ResultadoJSON.put("saldo", oferCliente.getSaldo());
		listJSON.add(ResultadoJSON);
		return(listJSON.toJSONString());
	}

	public String actualizarUsoOferta(int idOfertaCliente, String usuarioUso, double descuentoSobrante, double descuento)
	{
		JSONArray listJSON = new JSONArray();
		String respuesta = OfertaClienteDAO.actualizarUsoOferta(idOfertaCliente, usuarioUso, descuentoSobrante);
		LogRedencionCodigoDAO.insertarLogRedencionCodigo(idOfertaCliente, usuarioUso, descuentoSobrante, descuento);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", "exitoso");
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	public ArrayList<OfertaCliente> obtenerOfertasNuevasSemana(String fechaSuperior, String fechaInferior)
	{
		ArrayList<OfertaCliente> ofertasNuevasSemana =  OfertaClienteDAO.obtenerOfertasNuevasSemana(fechaSuperior, fechaInferior);
		return(ofertasNuevasSemana);
	}
	
	public ArrayList<OfertaCliente> obtenerOfertasRedimidasSemana(String fechaSuperior, String fechaInferior)
	{
		ArrayList<OfertaCliente> ofertasRedSemana =  OfertaClienteDAO.obtenerOfertasRedimidasSemana(fechaSuperior, fechaInferior);
		return(ofertasRedSemana );
	}
	
	//ENTIDAD OFERTA
	
	public String obtenerOfertas()
	{
		ArrayList<Oferta> ofertas = OfertaDAO.obtenerOfertas();
		JSONArray listJSON = new JSONArray();
		for (Oferta ofer : ofertas) 
		{
			JSONObject cadaOfertaJSON = new JSONObject();
			cadaOfertaJSON.put("idoferta", ofer.getIdOferta());
			cadaOfertaJSON.put("nombreoferta", ofer.getNombreOferta());
			cadaOfertaJSON.put("idexcepcion", ofer.getIdExcepcion());
			listJSON.add(cadaOfertaJSON);
		}
		return(listJSON.toJSONString());
		
	}
	
	public  String obtenerOfertasGrid()
	{
		ArrayList<Oferta> ofertas = OfertaDAO.obtenerOfertasGrid();
		JSONArray listJSON = new JSONArray();
		for (Oferta ofer : ofertas) 
		{
			JSONObject cadaOfertaJSON = new JSONObject();
			cadaOfertaJSON.put("idoferta", ofer.getIdOferta());
			cadaOfertaJSON.put("nombreoferta", ofer.getNombreOferta());
			cadaOfertaJSON.put("idexcepcion", ofer.getIdExcepcion());
			cadaOfertaJSON.put("nombreexcepcion", ofer.getNombreExcepcion());
			listJSON.add(cadaOfertaJSON);
		}
		return(listJSON.toJSONString());
	}
	
	public  String obtenerOfertasGridContact()
	{
		ArrayList<Oferta> ofertas = OfertaDAO.obtenerOfertasGridContact();
		JSONArray listJSON = new JSONArray();
		for (Oferta ofer : ofertas) 
		{
			JSONObject cadaOfertaJSON = new JSONObject();
			cadaOfertaJSON.put("idoferta", ofer.getIdOferta());
			cadaOfertaJSON.put("nombreoferta", ofer.getNombreOferta());
			cadaOfertaJSON.put("idexcepcion", ofer.getIdExcepcion());
			cadaOfertaJSON.put("nombreexcepcion", ofer.getNombreExcepcion());
			listJSON.add(cadaOfertaJSON);
		}
		return(listJSON.toJSONString());
	}
	
	public String insertarOferta(Oferta ofer)
	{
		JSONArray listJSON = new JSONArray();
		int respuesta = OfertaDAO.insertarOferta(ofer);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idoferta", respuesta);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	public String eliminarOferta(int idOferta)
	{
		JSONArray listJSON = new JSONArray();
		OfertaDAO.eliminarOferta(idOferta);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", "exitoso");
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	public String retornarOferta(int idOferta)
	{
		JSONArray listJSON = new JSONArray();
		Oferta oferta = OfertaDAO.retornarOferta(idOferta);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idoferta", oferta.getIdOferta());
		ResultadoJSON.put("nombreoferta", oferta.getNombreOferta());
		ResultadoJSON.put("idexcepcion", oferta.getIdExcepcion());
		ResultadoJSON.put("descuentofijovalor", oferta.getDescuentoFijoValor());
		ResultadoJSON.put("descuentofijoporcentaje", oferta.getDescuentoFijoPorcentaje());
		ResultadoJSON.put("idexcepcion", oferta.getIdExcepcion());
		listJSON.add(ResultadoJSON);
		return(listJSON.toJSONString());
	}

	public String editarOferta(Oferta ofertaEdi)
	{
		JSONArray listJSON = new JSONArray();
		String resultado = OfertaDAO.editarOferta(ofertaEdi);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", resultado);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	
/**M�todo que iterativamente se va a encargar de generar el c�digo de promoci�n lo validar� y lo retornar� una vez encuentre que si puede ser generado
 * A hoy el m�todo se genera con unas caracter�sticas
 * @return
 */
	public String generarCodigoPromocional()
	{
		String codigo = "";
		//variable para controlar que si hubo de generaci�n de c�digo �nico
		boolean bandera = true;
		while(bandera)
		{
			codigo = "";
			int a;
			 for (int i = 0; i < 7; i++) 
			 {
			        if (i < 4) {    // 0,1,2,3 posiciones de numeros
			            codigo = (int) (Math.random() * 9) + "" + codigo;

			        } else {       // 4,5,6 posiciones de letras
			            do {
			                a = (int) (Math.random() * 26 + 65);///
			            } while (a == 65 || a == 69 || a == 73 || a == 79 || a == 85);

			            char letra = (char) a;
			            if (i == 4) {
			                codigo = codigo  + letra;
			            } else {
			                codigo = codigo + "" + letra;
			            }

			        }
			 }
			 //Validamos si el c�digo promocional existe, en caso de que no exista regresar� un false y saldr� del ciclo while
			 bandera = OfertaClienteDAO.validarExistenciaOfertaCliente(codigo);
		}
			 
	    System.out.println(codigo);
		return(codigo);
	}
	
	/**
	 * M�todo que desde la capa de l�gica de negocio se encarga de retornar si una oferta existe y en caso positivo devuelve
	 * la informaci�n de la oferta
	 * @param codigoPromocional
	 * @return
	 */
	public String retornarOfertaCodigoPromocional(String codigoPromocional)
	{
		//Preparamos la respuesta del JSON
		JSONObject ofertaJSON = new JSONObject();
		
		//Se validar� primero si la oferta corresponde a un c�digo promocional abierto
		Oferta ofertaAbierta = OfertaDAO.obtenerOfertaCodigoPromocional(codigoPromocional);
		if(ofertaAbierta.getIdOferta() > 0)
		{
			ofertaJSON.put("respuesta", "OK");
			ofertaJSON.put("idoferta", ofertaAbierta.getIdOferta());
			ofertaJSON.put("idofertacliente", 0);
			ofertaJSON.put("idcliente", 0);
			ofertaJSON.put("utilizado", "N");
			ofertaJSON.put("observacion", ofertaAbierta.getNombreOferta());
			ofertaJSON.put("codigopromocional", codigoPromocional);
			ofertaJSON.put("tipooferta", "A");
			ofertaJSON.put("redparcial", "N");
			ofertaJSON.put("saldo", 0);
		}else
		{
			OfertaCliente ofertaCliente = OfertaClienteDAO.retornarOfertaCodigoPromocional(codigoPromocional);
			//Variable en la que marcaremos si la oferta est� vigente
			boolean vigente = false;
			if(ofertaCliente.getIdOfertaCliente() == 0)
			{
				ofertaJSON.put("respuesta", "NOK");
			}
			else
			{
				//Recuperamos las condiciones de hora de la oferta
				Oferta ofertaHora = OfertaDAO.retornarOfertaInfoHora(ofertaCliente.getIdOferta());
				//Es necesario validar si la oferta esta vigente o no tiene fecha de caducidad o si esta caducada
				if(ofertaCliente.getFechaCaducidad().equals(null) || ofertaCliente.getFechaCaducidad().equals(new String("")))
				{
					vigente = true;
				}else
				{
					//Es porque hay fecha de caducidad entonces tendremos que transformarla y validarla
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					//Traemos la fecha Actual
					Date fechaActual = new Date();
					Date fechaCaducidad = new Date();
					try
					{
						fechaCaducidad = dateFormat.parse(ofertaCliente.getFechaCaducidad());
					}catch(Exception e)
					{
						
					}
					if(fechaActual.compareTo(fechaCaducidad) > 0)
					{
						vigente = false;
					}else
					{
						//Posteriormente incluiremos la validaci�n de la hora si es que existe
						if(ofertaHora.getControlaHora().equals(new String("N")))
						{
							vigente = true;
						}else
						{
							int horaActual = fechaActual.getHours();
							int horaInicialOferta = Integer.parseInt(ofertaHora.getHoraInicio());
							int horaFinalOferta = Integer.parseInt(ofertaHora.getHoraFin());
							if((horaActual >= horaInicialOferta) && (horaActual < horaFinalOferta))
							{
								vigente = true;
							}else
							{
								vigente = false;
							}
						}
						
					}
				}
				if(vigente && ofertaCliente.getUtilizada().equals(new String("N")))
				{
					ofertaJSON.put("respuesta", "OK");
					//En el caso de qeu la oferta este vigente y no est� actualizada retoremos un par de campos indicando el saldo y si maneja redenci�n parcial
					if(ofertaCliente.getSaldo() > 0 && ofertaHora.getRedParcial().equals(new String("S")))
					{
						ofertaJSON.put("redparcial", "S");
						ofertaJSON.put("saldo", ofertaCliente.getSaldo());
					}else
					{
						ofertaJSON.put("redparcial", "N");
						ofertaJSON.put("saldo", 0);
					}
				}else
				{
					ofertaJSON.put("redparcial", "N");
					ofertaJSON.put("saldo", 0);
					ofertaJSON.put("respuesta", "VEN");
				}
				ofertaJSON.put("idoferta", ofertaCliente.getIdOferta());
				ofertaJSON.put("idofertacliente", ofertaCliente.getIdOfertaCliente());
				ofertaJSON.put("idcliente", ofertaCliente.getIdCliente());
				ofertaJSON.put("utilizado", ofertaCliente.getUtilizada());
				ofertaJSON.put("ingresooferta", ofertaCliente.getIngresoOferta());
				ofertaJSON.put("usooferta", ofertaCliente.getUsoOferta());
				ofertaJSON.put("observacion", ofertaCliente.getObservacion());
				ofertaJSON.put("pqrs", ofertaCliente.getPQRS());
				ofertaJSON.put("codigopromocional", ofertaCliente.getCodigoPromocion());
				ofertaJSON.put("fechacaducidad", ofertaCliente.getFechaCaducidad());
				ofertaJSON.put("tipooferta", "C");
			}
		}
		return(ofertaJSON.toJSONString());
	}
	
	//M�todo para validar un c�digo de excepci�n cerrada
	public String validarExcepcionCerrada(String codigo, int idExcepcion)
	{
		//Preparamos la respuesta del JSON
		JSONObject respuestaJSON = new JSONObject();;
		String nombre = ExcepcionPrecioDAO.obtenerExcepcionCerrada(codigo, idExcepcion);
		respuestaJSON.put("nombre", nombre);
		return(respuestaJSON.toJSONString());
	}
	
	public String enviarMensajesOferta(int idOferta)
	{
		//Con el siguiente m�todo obtenemos las ofertas que tienen que enviar mensaje de texto y no lo han enviado
		//todav�a, por lo tanto se crear� un arreglo para enviarlo.
		ArrayList<MensajeTexto> mensajesTexto = OfertaClienteDAO.obtenerMensajesTextoEnviar(idOferta);
		//Posteriormente se realizar� un procesamiento del arreglo para el env�o del mensaje de texto
		String telTemp = "";
		String telCelTemp = "";
		int totalMensajesEnviados = mensajesTexto.size();
		//Tendremos una variable que nos indicar� que si se puede enviar mensaje de texto
		boolean enviarMensaje = false;
		//Varialbles Definitivas para el env�o del mensaje
		//Definimos variable con el telefono sobre el que enviaremos mensaje.
		String telEnviarMensaje = "";
		String mensaje1 = "";
		String mensaje2 = "";
		//Variable donde se almacenar� el resultado del env�o del mensaje
		String resultado = "";
		for(MensajeTexto mensaje : mensajesTexto)
		{
			telTemp = mensaje.getTelefono();
			if(telTemp == null)
			{
				telTemp = " ";
			}
			telCelTemp = mensaje.getNumeroCelular();
			if(telCelTemp == null)
			{
				telCelTemp = " ";
			}
			//Verificamos si el n�mero celular esta bien en cuanto a que el n�mero
			if(telCelTemp.length() > 1)
			{
				if ((telCelTemp.substring(0, 1).equals(new String("3"))) && (telCelTemp.length() == 10))
				{
					enviarMensaje = true;
					telEnviarMensaje = telCelTemp;
				}else if ((telTemp.substring(0, 1).equals(new String("3"))) && (telTemp.length() == 10))
				{
					enviarMensaje = true;
					telEnviarMensaje = telTemp;
				}//Sino se cumplio ninguna de las condiciones no se enviar� mensaje
			}//Sino se cumplen estas condiciones se evalua el telefono principal
			else if(telTemp.length() > 1)
			{
				if ((telTemp.substring(0, 1).equals(new String("3"))) && (telTemp.length() == 10))
				{
					enviarMensaje = true;
					telEnviarMensaje = telTemp;
				}//Sino se cumplio ninguna de las condiciones no se enviar� mensaje
			}
			
			
			if(enviarMensaje)
			{
				// Retornamos el mensaje para hacerle tratamiento mensaje 1
				mensaje1 = mensaje.getMensaje1();
				//Validamos el mensaje 1 que no sea nulo y que tenga longitud
				if((!mensaje1.equals(null)) && (mensaje1.trim().length() > 0))
				{
					//Buscamos dentro del caracter los comodines para reemplazarlos
					//#NOMBRECLIENTE  #APELLIDOCLIENTE #CODIGODESCUENTO
					mensaje1 = mensaje1.replace("#NOMBRECLIENTE", mensaje.getNombreCliente());
					mensaje1 = mensaje1.replace("#APELLIDOCLIENTE", mensaje.getApellidoCliente());
					mensaje1 = mensaje1.replace("#CODIGODESCUENTO", mensaje.getCodigoPromocion());
					//Validamos la longitud del mensaje y si cumple lo enviaremos
					if(mensaje1.length() <= 160)
					{
						//Realizar�amos el llamado al programa PHP
						resultado = ejecutarPHPEnvioMensaje( "57"+ telEnviarMensaje, mensaje1);
						//Adicionalmente por un momento enviaremos el mensaje de WhatsApp
						PedidoCtrl.enviarWhatsAppUltramsg(mensaje1, telEnviarMensaje);
					}
				}
				//Incluimos l�gica para env�o de correo electr�nico
				String email = mensaje.getEmail();
				if(email == null)
				{
					email = "";
				}
				//Intentar� enviar correo electr�nico
				if(email.trim().length() > 0)
				{
					String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
					String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
					Correo correo = new Correo();
					correo.setAsunto("Bono regalo Pizza Americana");
					ArrayList correos = new ArrayList();
					correos.add(email);
					correo.setContrasena(claveCorreo);
					correo.setUsuarioCorreo(cuentaCorreo);
					correo.setMensaje(mensaje1);
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					//Agregamos control para que verifique con que m�todo debe hacer el env�o
					if(cuentaCorreo.contains("@gmail.com"))
					{
						contro.enviarCorreo();
					}else
					{
						contro.enviarCorreo();
					}
				}
				
				// Retornamos el mensaje para hacerle tratamiento mensaje 2
				mensaje2 = mensaje.getMensaje2();
				if(mensaje2 != null)
				{
					//Validamos el mensaje 1 que no sea nulo y que tenga longitud
					if((!mensaje2.equals(null)) && (mensaje2.trim().length() > 0))
					{
						//Buscamos dentro del caracter los comodines para reemplazarlos
						//#NOMBRECLIENTE  #APELLIDOCLIENTE #CODIGODESCUENTO
						mensaje2 = mensaje2.replace("#NOMBRECLIENTE", mensaje.getNombreCliente());
						mensaje2 = mensaje2.replace("#APELLIDOCLIENTE", mensaje.getApellidoCliente());
						mensaje2 = mensaje2.replace("#CODIGODESCUENTO", mensaje.getCodigoPromocion());
						//Validamos la longitud del mensaje y si cumple lo enviaremos
						if(mensaje2.length() <= 160)
						{
							//Realizar�amos el llamado al programa PHP
							resultado =  ejecutarPHPEnvioMensaje( "57"+ telEnviarMensaje, mensaje2);
						}
					}
				}
				//Realizamos la marcaci�n de que ya se realizo el env�o del mensaje
				OfertaClienteDAO.actualizarMensajeOferta(mensaje.getIdOfertaCliente());
			}
		}
		//Si todo logro llegar  a este punto sin error, generaremos una respuesta de exitoso en un JSON
		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON.put("respuesta", "exitoso");
		respuestaJSON.put("mensajes", totalMensajesEnviados);
		return(respuestaJSON.toJSONString());
	}
	
	
	/**
	 * Método creado exclusivamente para Brevo
	 * @param idOferta
	 * @return
	 */
	public String enviarMensajesOfertaBrevo(int idOferta)
	{
		//Con el siguiente m�todo obtenemos las ofertas que tienen que enviar mensaje de texto y no lo han enviado
		//todav�a, por lo tanto se crear� un arreglo para enviarlo.
		ArrayList<MensajeTexto> mensajesTexto = OfertaClienteDAO.obtenerMensajesTextoEnviar(idOferta);
		//Posteriormente se realizar� un procesamiento del arreglo para el env�o del mensaje de texto
		String telTemp = "";
		String telCelTemp = "";
		int totalMensajesEnviados = mensajesTexto.size();
		//Tendremos una variable que nos indicar� que si se puede enviar mensaje de texto
		boolean enviarMensaje = false;
		//Varialbles Definitivas para el env�o del mensaje
		//Definimos variable con el telefono sobre el que enviaremos mensaje.
		String telEnviarMensaje = "";
		String mensaje1 = "";
		String mensaje2 = "";
		//Variable donde se almacenar� el resultado del env�o del mensaje
		String resultado = "";
		for(MensajeTexto mensaje : mensajesTexto)
		{
			telTemp = mensaje.getTelefono();
			if(telTemp == null)
			{
				telTemp = " ";
			}
			telCelTemp = mensaje.getNumeroCelular();
			if(telCelTemp == null)
			{
				telCelTemp = " ";
			}
			//Verificamos si el n�mero celular esta bien en cuanto a que el n�mero
			if(telCelTemp.length() > 1)
			{
				if ((telCelTemp.substring(0, 1).equals(new String("3"))) && (telCelTemp.length() == 10))
				{
					enviarMensaje = true;
					telEnviarMensaje = telCelTemp;
				}else if ((telTemp.substring(0, 1).equals(new String("3"))) && (telTemp.length() == 10))
				{
					enviarMensaje = true;
					telEnviarMensaje = telTemp;
				}//Sino se cumplio ninguna de las condiciones no se enviar� mensaje
			}//Sino se cumplen estas condiciones se evalua el telefono principal
			else if(telTemp.length() > 1)
			{
				if ((telTemp.substring(0, 1).equals(new String("3"))) && (telTemp.length() == 10))
				{
					enviarMensaje = true;
					telEnviarMensaje = telTemp;
				}//Sino se cumplio ninguna de las condiciones no se enviar� mensaje
			}
			
			
			if(enviarMensaje)
			{
				// Retornamos el mensaje para hacerle tratamiento mensaje 1
				mensaje1 = mensaje.getMensaje1();
				//Validamos el mensaje 1 que no sea nulo y que tenga longitud
				if((!mensaje1.equals(null)) && (mensaje1.trim().length() > 0))
				{
					//Buscamos dentro del caracter los comodines para reemplazarlos
					//#NOMBRECLIENTE  #APELLIDOCLIENTE #CODIGODESCUENTO
					mensaje1 = mensaje1.replace("#NOMBRECLIENTE", mensaje.getNombreCliente());
					mensaje1 = mensaje1.replace("#APELLIDOCLIENTE", mensaje.getApellidoCliente());
					mensaje1 = mensaje1.replace("#CODIGODESCUENTO", mensaje.getCodigoPromocion());
					//Validamos la longitud del mensaje y si cumple lo enviaremos
					if(mensaje1.length() <= 160)
					{
						//Realizar�amos el llamado al programa PHP
						ejecutarMensajeTextoBrevo(telEnviarMensaje, mensaje1);
					}
				}
				//Realizamos la marcaci�n de que ya se realizo el env�o del mensaje
				OfertaClienteDAO.actualizarMensajeOferta(mensaje.getIdOfertaCliente());
			}
		}
		//Si todo logro llegar  a este punto sin error, generaremos una respuesta de exitoso en un JSON
		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON.put("respuesta", "exitoso");
		respuestaJSON.put("mensajes", totalMensajesEnviados);
		return(respuestaJSON.toJSONString());
	}
	
	public void ejecutarMensajeTextoBrevo( String telefono, String mensaje) {
		{
			String API_URL = "https://api.brevo.com/v3/transactionalSMS/send";
			String API_KEY = ParametrosDAO.retornarValorAlfanumerico("APIKEYBREVO");
			if(API_KEY.equals(new String("")))
			{
				API_KEY = "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";
			}
		    
			// Crear el JSON con los datos del mensaje
	        JSONObject jsonPayload = new JSONObject();
	        jsonPayload.put("sender", "Americana");
	        jsonPayload.put("recipient", "+57"+telefono);
	        jsonPayload.put("content", mensaje);
	        jsonPayload.put("type", "marketing");
	        jsonPayload.put("unicodeEnabled", true);
	        jsonPayload.put("organisationPrefix", "!Pizza Americana!");
	        // Crear el cliente OkHttp
	        OkHttpClient client = new OkHttpClient();

	        Request request = new Request.Builder()
	                .url(API_URL)
	                .addHeader("accept", "application/json")
	                .addHeader("content-type", "application/json")
	                .addHeader("api-key", API_KEY)
	                .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonPayload.toString()))
	                .build();
	        // Ejecutar la solicitud
	        try (Response response = client.newCall(request).execute()) {
	            if (!response.isSuccessful()) {
	                System.out.println("Error en la solicitud: " + response.code() + " - " + response.message());
	            } else {
	                System.out.println("Mensaje enviado con éxito: " + response.body().string());
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}

	  }
	
	public String ejecutarPHPEnvioMensaje( String telefono, String mensaje) {
		StringBuilder output = new StringBuilder();
		try {
	      String line;
	      //Variable con ruta del script
	      String rutaPHPEnviarMensaje = ParametrosDAO.retornarValorAlfanumerico("RUTAPHPENVIARMENSAJE");
	      System.out.println("php " + rutaPHPEnviarMensaje + " " + telefono + " '" + mensaje + "'");
	      Process p = Runtime.getRuntime().exec(new String[]{"php",rutaPHPEnviarMensaje, telefono, mensaje});
	      //Process p = Runtime.getRuntime().exec("php " + rutaPHPEnviarMensaje + " " + telefono + " '" + mensaje + "'");
	      BufferedReader input =
	        new BufferedReader
	          (new InputStreamReader(p.getInputStream()));
	      while ((line = input.readLine()) != null) {
	          output.append(line);
	      }
	      input.close();
	    }
	    catch (Exception err) {
	      err.printStackTrace();
	    }
	    return output.toString();
	  }
	
	
	public String ejecutarPHPEnvioMensajeGet( String telefono, String mensaje) {
		StringBuilder output = new StringBuilder();
		try {
	      String line;
	      //Variable con ruta del script
	      String rutaPHPEnviarMensaje = ParametrosDAO.retornarValorAlfanumerico("RUTAPHPENVIARMENSAJE");
	      System.out.println("php " + rutaPHPEnviarMensaje + " " + telefono + " '" + mensaje + "'");
	      Process p = Runtime.getRuntime().exec("php " + rutaPHPEnviarMensaje + "?telefono=" + telefono + "&mensaje=" + mensaje + "");
	      BufferedReader input =
	        new BufferedReader
	          (new InputStreamReader(p.getInputStream()));
	      while ((line = input.readLine()) != null) {
	          output.append(line);
	      }
	      input.close();
	    }
	    catch (Exception err) {
	      err.printStackTrace();
	    }
	    return output.toString();
	  }
	
	//Hacemos la prueba de la creaci�n de un LEAD
	public static void crearLeadWhatsApp()
	{

	String jsonString = "{ " + 
			"   add: [ " + 
			"      { " + 
			"         name: \"Pencils purchase\", " + 
			"         created_at: \"1508101200\", " + 
			"         updated_at: \"1508274000\", " + 
			"         status_id: \"13670637\", " + 
			"         responsible_user_id: \"957083\", " + 
			"         sale: \"5000\", " + 
			"         tags: \"pencil, buy\", " + 
			"         contacts_id: [ " + 
			"            \"1099149\" " + 
			"            ], " + 
			"            company_id: \"1099148\", " + 
			"            custom_fields: [ " + 
			"               { " + 
			"                  id: \"4399649\", " + 
			"                  values: [ " + 
			"                     \"3691615\", " + 
			"                     \"3691616\", " + 
			"                     \"3691617\" " + 
			"                  ] " + 
			"               }, " + 
			"               { " + 
			"                  id: \"4399656\", " + 
			"                  values: [ " + 
			"                     { " + 
			"                        value: \"2017-10-26\" " + 
			"                     } " + 
			"                  ] " + 
			"               }, " + 
			"               { " + 
			"                  id: \"4399655\", " + 
			"                  values: [ " + 
			"                     { " + 
			"                        value: \"Madison st., 1\", " + 
			"                        subtype: \"address_line_1\" " + 
			"                     }, " + 
			"                     { " + 
			"                        value: \"Washington\", " + 
			"                        subtype: \"city\" " + 
			"                     }, " + 
			"                     { " + 
			"                        value: \"101010\", " + 
			"                        subtype: \"zip\" " + 
			"                     }, " + 
			"                     { " + 
			"                        value: \"US\", " + 
			"                        subtype: \"country\" " + 
			"                     } " + 
			"                  ] " + 
			"               } " + 
			"            ] " + 
			"      } " + 
			"   ] " + 
			"}";
			//Realizamos la invocaci�n mediante el uso de HTTPCLIENT
			HttpClient client = HttpClientBuilder.create().build();
			String rutaURLNotif = "https://us-east1-bottapizzaamericana.cloudfunctions.net/fnBottaWhatsAppNotification";
			HttpPost request = new HttpPost(rutaURLNotif);
			try
			{
				//Fijamos el header con el token
				//NO HAY SEGURIDAD TODAV�A
		
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json");
				//Fijamos los par�metros
				//pass the json string request in the entity
			    HttpEntity entity = new ByteArrayEntity(jsonString.getBytes("UTF-8"));
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
				String respuestaServicio = retorno.toString();
				if(respuestaServicio.equals(new String("ok : Mensaje Enviado correctamente")))
				{
					
				}else
				{
					//Recuperar la lista de distribuci�n para este correo
					ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
					Date fecha = new Date();
					Correo correo = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
					correo.setAsunto("OJO ERROR EN SERVICIO DE WHATSAPP CON BOTTA DESUSO " + fecha.toString());
					correo.setContrasena(infoCorreo.getClaveCorreo());
					correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
					correo.setMensaje("Se presenta error en servicio de API de WhatsApp.");
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					contro.enviarCorreo();
				}
				//Traemos el valor del JSON con toda la info del pedido
				String datosJSON = retorno.toString();
				System.out.println(datosJSON);
			}catch (Exception e2) {
		        e2.printStackTrace();
		        System.out.println(e2.toString());
		    }

	}
	
	public static void main(String args[])
	{
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		promoCtrl.retornarOfertaCodigoPromocional("1386HJV");
	}
	
	public String generarCodigoPrecioEmpleado()
	{
		String codigo = "";
		//variable para controlar que si hubo de generaci�n de c�digo �nico
		boolean bandera = true;
		while(bandera)
		{
			codigo = "";
			int a;
			 for (int i = 0; i < 7; i++) 
			 {
			        if (i < 4) {    // 0,1,2,3 posiciones de numeros
			            codigo = (int) (Math.random() * 9) + "" + codigo;

			        } else {       // 4,5,6 posiciones de letras
			            do {
			                a = (int) (Math.random() * 26 + 65);///
			            } while (a == 65 || a == 69 || a == 73 || a == 79 || a == 85);

			            char letra = (char) a;
			            if (i == 4) {
			                codigo = codigo  + letra;
			            } else {
			                codigo = codigo + "" + letra;
			            }

			        }
			 }
			 //Validamos si el c�digo promocional existe, en caso de que no exista regresar� un false y saldr� del ciclo while
			 bandera = PedidoPrecioEmpleadoDAO.validarExistenciaCodigo(codigo);
		}
			 
	    System.out.println(codigo);
		return(codigo);
	}
	
	public String generarCodigoRedencionPuntos()
	{
		String codigo = "";
		//variable para controlar que si hubo de generaci�n de c�digo �nico
		boolean bandera = true;
		while(bandera)
		{
			codigo = "";
			int a;
			 for (int i = 0; i < 7; i++) 
			 {
			        if (i < 4) {    // 0,1,2,3 posiciones de numeros
			            codigo = (int) (Math.random() * 9) + "" + codigo;

			        } else {       // 4,5,6 posiciones de letras
			            do {
			                a = (int) (Math.random() * 26 + 65);///
			            } while (a == 65 || a == 69 || a == 73 || a == 79 || a == 85);

			            char letra = (char) a;
			            if (i == 4) {
			                codigo = codigo  + letra;
			            } else {
			                codigo = codigo + "" + letra;
			            }

			        }
			 }
			 //Validamos si el c�digo promocional existe, en caso de que no exista regresar� un false y saldr� del ciclo while
			 bandera = CodigoRedencionPuntosDAO.validarExistenciaCodigo(codigo);
		}
		return(codigo);
	}
	
	public String generarCodigoEmpleadoRemotovale()
	{
		String codigo = "";
		//variable para controlar que si hubo de generaci�n de c�digo �nico
		boolean bandera = true;
		while(bandera)
		{
			codigo = "";
			int a;
			 for (int i = 0; i < 7; i++) 
			 {
			        if (i < 4) {    // 0,1,2,3 posiciones de numeros
			            codigo = (int) (Math.random() * 9) + "" + codigo;

			        } else {       // 4,5,6 posiciones de letras
			            do {
			                a = (int) (Math.random() * 26 + 65);///
			            } while (a == 65 || a == 69 || a == 73 || a == 79 || a == 85);

			            char letra = (char) a;
			            if (i == 4) {
			                codigo = codigo  + letra;
			            } else {
			                codigo = codigo + "" + letra;
			            }

			        }
			 }
			 //Validamos si el c�digo promocional existe, en caso de que no exista regresar� un false y saldr� del ciclo while
			 bandera = EmpleadoRemotoValeDAO.validarExistenciaCodigo(codigo);
		}
			 
	    System.out.println(codigo);
		return(codigo);
	}
	
}

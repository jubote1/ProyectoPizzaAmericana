package capaControladorCC;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import capaModeloCC.EncuestaPqrs.RespuestaEncuestaPqrs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import capaModeloCC.AreaEscalamiento;
import capaConexionPOS.ConexionBaseDatos;
import capaDAOCC.AreaEscalamientoPQRSDAO;
import capaDAOCC.ClienteDAO;
import capaDAOCC.EmpleadoEncuestaDAO;
import capaDAOCC.EscalamientoPQRSDAO;

import capaDAOCC.FocoPqrsDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.LogEncuestaServicioDAO;
import capaDAOCC.LogPedidoVirtualKunoDAO;
import capaDAOCC.MunicipioDAO;
import capaDAOCC.OrigenPqrsDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.SolicitudPQRSDAO;
import capaDAOCC.SolicitudPQRSImagenesDAO;
import capaDAOCC.TiendaDAO;
import capaDAOCC.UsuarioDAO;
import capaModeloCC.Cliente;
import capaModeloCC.ComentarioPqrs;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;

import capaModeloCC.EncuestaPqrs;
import capaModeloCC.EncuestaServicio;
import capaModeloCC.EscalamientoPQRS;

import capaModeloCC.EstadoPqrs;
import capaModeloCC.FocoPqrs;
import capaModeloCC.OrigenPqrs;
import capaModeloCC.Pedido;
import capaModeloCC.PendientePqrs;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.SolicitudPQRSImagenes;
import capaModeloCC.Tienda;
import capaModeloCC.Usuario;
import capaModeloCC.EncuestaServicio.RespuestaServicio;
import utilidadesCC.ControladorEnvioCorreo;
import capaModeloCC.ComentarioPqrs;
import capaModeloCC.MotivoPqrs;
import capaModeloCC.PrioridadPqrs;

public class SolicitudPQRSCtrl {

	/**
	 * M�todo de la capa controladora que recibe los par�metros de la entidad
	 * SolicitudPQRS se encarga de crear el objeto correspondiente e invoca la capa
	 * DAO para la inserci�n de la entidad.
	 * 
	 * @param fechaSolicitud
	 * @param tipoSolicitud
	 * @param idcliente
	 * @param idtienda
	 * @param nombres
	 * @param apellidos
	 * @param telefono
	 * @param direccion
	 * @param zona
	 * @param idmunicipio
	 * @param comentario
	 * @return Retorna un entero con el idSolicitudPQRS que retorna la base de datos
	 *         en la creaci�n del mismo.
	 */
	public String insertarSolicitudPQRS(String fechaSolicitud, String tipoSolicitud, int idcliente, int idtienda,
			String nombres, String apellidos, String telefono, String direccion, String zona, int idmunicipio,
			String comentario, int idOrigen, int idFoco, String tipo, String areaResponsable, int idpedidotienda,
			double valorPedido, double valorDescuento, int porcentajeDescuento, boolean descuentoRedimido,
			int idpedidoredencion, List<ComentarioPqrs> listaComentarios, int idusuarioRegistro, int idusuarioRedencion,
			int idestado, int idprioridad, int idmotivo, boolean ccVinculado, String correo) {
		JSONArray listJSON = new JSONArray();
		SolicitudPQRS solicitud = new SolicitudPQRS(0, fechaSolicitud, tipoSolicitud, idcliente, idtienda, nombres,
				apellidos, telefono, direccion, zona, idmunicipio, comentario, idOrigen, idFoco, tipo, areaResponsable,
				idpedidotienda, valorPedido, valorDescuento, porcentajeDescuento, descuentoRedimido, idpedidoredencion,
				idusuarioRegistro, idusuarioRedencion, idestado, idprioridad, idmotivo, ccVinculado, correo);

		// Tienda objTienda = TiendaDAO.retornarTienda(idtienda);
		// String nombreTienda = objTienda.getNombreTienda();
		// FocoPqrs foco = FocoPqrsDAO.retornarFocoPqrs(idFoco);
		int idSolPQRSIns = SolicitudPQRSDAO.insertarSolicitudPQRS(solicitud);
		boolean registroComent = SolicitudPQRSDAO.modificarComentariosPqrs(idSolPQRSIns, listaComentarios);
		/*
		 * // Se realiza env�o del correo con la solicitud Correo correo = new Correo();
		 * String cuentaCorreo =
		 * ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI"); String
		 * claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		 * //Extraemos el comentario para enviar el correo String comentarioCorreo = "";
		 * for (ComentarioPqrs comentarioTemp : listaComentarios) { comentarioCorreo =
		 * comentarioCorreo + " " + comentarioTemp.getComentario(); }
		 * correo.setAsunto("SE REGISTRO PQRS # " + idSolPQRSIns); ArrayList correos =
		 * GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		 * correo.setContrasena(claveCorreo); correo.setUsuarioCorreo(cuentaCorreo);
		 * correo.setMensaje("Se registro PQRS para el cliente " + nombres + " " +
		 * apellidos + " para la tienda " + nombreTienda +
		 * " con el siguiente comentario: " + comentario +
		 * " \n Informaci�n Adicional del cliente telefono: " + telefono +
		 * " Direccion: " + direccion +
		 * ". \n Si desea m�s informaci�n favor revisar en el sistema de Contact Center en el apartado de PQRS \n\n Foco de la PQRS: "
		 * + foco.getNombreFoco() + " \n\n Tipo: " + tipo + " , Area Responble: " +
		 * areaResponsable); ControladorEnvioCorreo contro = new
		 * ControladorEnvioCorreo(correo, correos); contro.enviarCorreo();
		 */
		if (idSolPQRSIns != 0 && registroComent) {
			CorreoPQRS confpqrs = new CorreoPQRS();
			if (correo != null && !correo.isEmpty()) {
				confpqrs.enviarConfirmacion(correo, nombres, idSolPQRSIns);
			}

		}

		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		ResultadoJSON.put("registroComentarios", registroComent);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	/**
	 * M�todo creado en la capa controladora para la actualizaci�n de una PQRS
	 * 
	 * @param idSolicitudPQRS
	 * @param fechaSolicitud
	 * @param tipoSolicitud
	 * @param idcliente
	 * @param idtienda
	 * @param nombres
	 * @param apellidos
	 * @param telefono
	 * @param direccion
	 * @param zona
	 * @param idmunicipio
	 * @param comentario
	 * @param idOrigen
	 * @param idFoco
	 * @param tipo
	 * @param areaResponsable
	 * @return
	 */
	public String actualizarSolicitudPQRS(int idSolicitudPQRS, String fechaSolicitud, String tipoSolicitud,
			int idcliente, int idtienda, String nombres, String apellidos, String telefono, String direccion,
			String zona, int idmunicipio, String comentario, int idOrigen, int idFoco, String tipo,
			String areaResponsable, int idpedidotienda, double valorPedido, double valorDescuento,
			int porcentajeDescuento, boolean descuentoRedimido, int idpedidoredencion,
			List<ComentarioPqrs> listaComentarios, int idusuarioRegistro, int idusuarioRedencion, int idestado,
			int idprioridad, int idmotivo, boolean ccVinculado, String correo_cliente, boolean envio_encuesta,String observacion_ans,boolean cambio_fecha_cierre) {

		JSONArray listJSON = new JSONArray();
		JSONObject ResultadoJSON = new JSONObject();

		// Crear y actualizar solicitud
		SolicitudPQRS solicitud = new SolicitudPQRS(idSolicitudPQRS, fechaSolicitud, tipoSolicitud, idcliente, idtienda,
				nombres, apellidos, telefono, direccion, zona, idmunicipio, comentario, idOrigen, idFoco, tipo,
				areaResponsable, idpedidotienda, valorPedido, valorDescuento, porcentajeDescuento, descuentoRedimido,
				idpedidoredencion, idusuarioRegistro, idusuarioRedencion, idestado, idprioridad, idmotivo, ccVinculado,
				correo_cliente);
		solicitud.setObservacion_ans(observacion_ans);
		solicitud.setCambio_fecha_cierre(cambio_fecha_cierre);
		String nombreTienda = TiendaDAO.retornarTienda(idtienda).getNombreTienda();
		String focoNombre = FocoPqrsDAO.retornarFocoPqrs(idFoco).getNombreFoco();

		int idSolPQRSIns = SolicitudPQRSDAO.actualizarSolicitudPQRS(solicitud);
		boolean registroComent = SolicitudPQRSDAO.modificarComentariosPqrs(idSolPQRSIns, listaComentarios);

		// Datos correo (no se envía ahora)
		Correo correo = new Correo();
		correo.setAsunto("SE ACTUALIZÓ REGISTRO PQRS # " + idSolPQRSIns);
		correo.setUsuarioCorreo(ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI"));
		correo.setContrasena(ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI"));
		correo.setMensaje("Se registró PQRS para el cliente " + nombres + " " + apellidos + " para la tienda "
				+ nombreTienda + " con el siguiente comentario: " + comentario + "\n Información Adicional: Teléfono: "
				+ telefono + ", Dirección: " + direccion + "\n Revisar en Contact Center > PQRS"
				+ "\n\n Foco de la PQRS: " + focoNombre + "\n\n Tipo: " + tipo + ", Área Responsable: "
				+ areaResponsable);
		// ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		// contro.enviarCorreo();

		// Variables de control
		boolean nuevo_envio = false;
		boolean estado_encuesta = false;
		boolean telefono_valido = true;

//		// Validar envío encuesta
//		if (idSolPQRSIns > 0 && idestado == 4 && !envio_encuesta) {
//			String telefonoLimpio = (telefono != null) ? telefono.replaceAll("\\D", "") : "";
//			telefono_valido = telefonoLimpio.matches("^3\\d{9}$");
//
//			if (telefono_valido) {
//				try {
//					org.json.JSONObject respuesta = new CorreoPQRS().enviarParsing(correo_cliente, nombres,
//							telefonoLimpio, idSolPQRSIns);
//					nuevo_envio = respuesta.getBoolean("success");
//
//					if (nuevo_envio) {
//						estado_encuesta = SolicitudPQRSDAO.actualizarEnvioEncuestaPqrs(idSolPQRSIns, true);
//					}
//				} catch (Exception e) {
//					System.out.println("Error envío encuesta PQRS: " + e.getMessage());
//					e.printStackTrace();
//				}
//			}
//		}

		// Construir respuesta
		ResultadoJSON.put("envioEncuesta", nuevo_envio);
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		ResultadoJSON.put("registroComentarios", registroComent);
		ResultadoJSON.put("estadoEncuesta", estado_encuesta);
		ResultadoJSON.put("telefono_valido", telefono_valido);

		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	public String descartarSolicitudPQRS(int idSolicitudPQRS) {
		JSONArray listJSON = new JSONArray();

		int idSolPQRSIns = SolicitudPQRSDAO.descartarSolicitudPQRS(idSolicitudPQRS);
		// Se realiza env�o del correo con la solicitud
		Correo correo = new Correo();
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		correo.setAsunto("SE DESCART� REGISTRO PQRS # " + idSolPQRSIns);
		ArrayList correos = GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		correo.setMensaje("Se descarto  registro PQRS " + idSolicitudPQRS);
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		// contro.enviarCorreo();
		//
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}

	public String ConsultaIntegradaSolicitudesPQRS(String fechainicial, String fechafinal, String tienda,
			String tipoSolicitud, boolean descuentoRedimido) {
		JSONArray listJSON = new JSONArray();
		ArrayList<SolicitudPQRS> consultaSolicitudes = SolicitudPQRSDAO.ConsultaIntegradaSolicitudesPQRS(fechainicial,
				fechafinal, tienda, tipoSolicitud, descuentoRedimido);
		for (SolicitudPQRS cadaSolicitud : consultaSolicitudes) {
			JSONObject cadaSolicitudJSON = new JSONObject();
			cadaSolicitudJSON.put("idconsultaPQRS", cadaSolicitud.getIdsolicitud());
			cadaSolicitudJSON.put("fechasolicitud", cadaSolicitud.getFechaSolicitud());
			cadaSolicitudJSON.put("tiposolicitud", cadaSolicitud.getTipoSolicitud());
			cadaSolicitudJSON.put("cliente", cadaSolicitud.getNombres() + " " + cadaSolicitud.getApellidos());
			cadaSolicitudJSON.put("nombres", cadaSolicitud.getNombres());
			cadaSolicitudJSON.put("apellidos", cadaSolicitud.getApellidos());
			cadaSolicitudJSON.put("direccion", cadaSolicitud.getDireccion());
			cadaSolicitudJSON.put("telefono", cadaSolicitud.getTelefono());
//			cadaSolicitudJSON.put("comentario", cadaSolicitud.getComentario());
			Tienda tie = TiendaDAO.retornarTienda(cadaSolicitud.getIdtienda());
			String muni = MunicipioDAO.obtenerMunicipio(cadaSolicitud.getIdmunicipio());
			cadaSolicitudJSON.put("municipio", muni);
			cadaSolicitudJSON.put("tienda", tie.getNombreTienda());
			cadaSolicitudJSON.put("idorigen", cadaSolicitud.getIdOrigen());
			cadaSolicitudJSON.put("nombreorigen", cadaSolicitud.getOrigen());
			cadaSolicitudJSON.put("nombrefoco", cadaSolicitud.getFoco());
			cadaSolicitudJSON.put("tipo", cadaSolicitud.getTipo());
			cadaSolicitudJSON.put("arearesponsable", cadaSolicitud.getAreaResponsable());
			cadaSolicitudJSON.put("imagenes", cadaSolicitud.getImagenes());
			cadaSolicitudJSON.put("idpedidotienda", cadaSolicitud.getIdpedidotienda());
			cadaSolicitudJSON.put("valorPedido", cadaSolicitud.getValorPedido());
			cadaSolicitudJSON.put("valorDescuento", cadaSolicitud.getValorDescuento());
			cadaSolicitudJSON.put("porcentajeDescuento", cadaSolicitud.getPorcentajeDescuento());
			cadaSolicitudJSON.put("descuentoRedimido", cadaSolicitud.isDescuentoRedimido());
			cadaSolicitudJSON.put("idpedidoredencion", cadaSolicitud.getIdpedidoredencion());
			cadaSolicitudJSON.put("idusuarioRegistro", cadaSolicitud.getIdusuarioRegistro());
			cadaSolicitudJSON.put("idusuarioRedencion", cadaSolicitud.getIdusuarioRedencion());
			cadaSolicitudJSON.put("idestado", cadaSolicitud.getIdestado());
			cadaSolicitudJSON.put("nombreEstado", cadaSolicitud.getNombreEstado());
			cadaSolicitudJSON.put("idprioridad", cadaSolicitud.getIdprioridad());
			cadaSolicitudJSON.put("idmotivo", cadaSolicitud.getIdmotivo());
			cadaSolicitudJSON.put("zona", cadaSolicitud.getZona());
			cadaSolicitudJSON.put("ccVinculado", cadaSolicitud.isCcVinculado());
			cadaSolicitudJSON.put("correo", cadaSolicitud.getCorreo());
			cadaSolicitudJSON.put("envio_encuesta", cadaSolicitud.isEnvio_encuesta());
			cadaSolicitudJSON.put("fecha_hora_registro",cadaSolicitud.getFecha_hora_registro());
			cadaSolicitudJSON.put("fecha_hora_cierre",cadaSolicitud.getFecha_hora_cierre());
			cadaSolicitudJSON.put("observacion_ans",cadaSolicitud.getObservacion_ans());

			try {
				cadaSolicitudJSON.put("listaComentarios", obtenerComentariosPqrs(cadaSolicitud.getIdsolicitud()));
			} catch (Exception e) {
				System.err.println("Error al obtener comentarios para solicitud " + cadaSolicitud.getIdsolicitud()
						+ ": " + e.getMessage());
				cadaSolicitudJSON.put("listaComentarios", new org.json.JSONObject()); // valor por defecto en caso de
																						// error
			}

			listJSON.add(cadaSolicitudJSON);
		}
		return listJSON.toJSONString();
	}

	public String validarPQRS(int PQRS, int idCliente) {
		JSONArray listJSON = new JSONArray();
		JSONObject respuestaJSON = new JSONObject();
		boolean respuesta = SolicitudPQRSDAO.validarPQRS(PQRS, idCliente);
		if (respuesta) {
			respuestaJSON.put("resultado", "OK");
		} else {
			respuestaJSON.put("resultado", "NOK");
		}
		listJSON.add(respuestaJSON);
		return (listJSON.toJSONString());
	}

	public String adicionarComentarioPQRS(int idSolicitudPQRS, String comentario) {
		boolean respuesta = SolicitudPQRSDAO.adicionarComentarioPQRS(idSolicitudPQRS, comentario);
		JSONArray listJSON = new JSONArray();
		JSONObject respuestaJSON = new JSONObject();
		if (respuesta) {
			respuestaJSON.put("resultado", "OK");
		} else {
			respuestaJSON.put("resultado", "NOK");
		}
		Correo correo = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOPUBLICA",
				"CLAVECORREOPUBLICA");
		correo.setAsunto("SE ACTUALIZO REGISTRO PQRS # " + idSolicitudPQRS);
		ArrayList correos = GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		correo.setMensaje("Se Actualizo PQRS con el siguiente comentario: " + comentario
				+ ". \n Si desea m�s informaci�n favor revisar en el sistema de Contact Center en el apartado de PQRS");
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		// contro.enviarCorreo();

		listJSON.add(respuestaJSON);
		return (listJSON.toJSONString());
	}

	// ESPACIO RESERVADO PARA TODO LO RELACIONADO CON ORIGEN_PQRS

	public String retornarOrigenesPqrs() {
		JSONArray listJSON = new JSONArray();
		ArrayList<OrigenPqrs> origenes = OrigenPqrsDAO.retornarOrigenesPqrs();
		for (OrigenPqrs cadaOrigen : origenes) {
			JSONObject cadaOrigenJSON = new JSONObject();
			cadaOrigenJSON.put("idorigen", cadaOrigen.getIdOrigen());
			cadaOrigenJSON.put("nombreorigen", cadaOrigen.getNombreOrigen());
			listJSON.add(cadaOrigenJSON);
		}
		return listJSON.toJSONString();
	}

	public String obtenerFocosPqrs() {
		JSONArray listJSON = new JSONArray();
		ArrayList<FocoPqrs> focosPqrs = FocoPqrsDAO.obtenerFocosPqrs();
		for (FocoPqrs cadaFoco : focosPqrs) {
			JSONObject cadaFocoJSON = new JSONObject();
			cadaFocoJSON.put("idfoco", cadaFoco.getIdFoco());
			cadaFocoJSON.put("nombrefoco", cadaFoco.getNombreFoco());
			listJSON.add(cadaFocoJSON);
		}
		return listJSON.toJSONString();
	}

	public String insertarSolicitudPQRSImagen(SolicitudPQRSImagenes solicitud) {
		int idImagen = SolicitudPQRSImagenesDAO.insertarSolicitudPQRSImagen(solicitud);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("idimagen", idImagen);
		return (resultadoJSON.toJSONString());
	}

	public String eliminarSolicitudPQRSImagen(int idimagen) {
		boolean resultado = SolicitudPQRSImagenesDAO.eliminarSolicitudPQRSImagenPorId(idimagen);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("respuesta", resultado);
		return (resultadoJSON.toJSONString());
	}

	public String consultarSolicitudPQRSImagenes(int idSolicitudPRQS) {
		ArrayList<org.json.JSONObject> imagenes = SolicitudPQRSImagenesDAO
				.consultarSolicitudPQRSImagenes(idSolicitudPRQS);
		return (imagenes.toString());
	}

	public org.json.JSONObject obtenerComentariosPqrs(int idSolicitudPqrs) {
		Map<String, List<ComentarioPqrs>> comentarios = SolicitudPQRSDAO.obtenerComentariosPqrs(idSolicitudPqrs);
		Gson gson = new Gson(); // Ya no se necesita formateador de fechas
		String jsonString = gson.toJson(comentarios); // convierte Map a JSON string
		org.json.JSONObject comentariosJson = new org.json.JSONObject(jsonString); // parsea string a JSONObject
		return comentariosJson;
	}

	public String obtenerEstadoPqrs() {
		List<EstadoPqrs> estados = SolicitudPQRSDAO.obtenerEstadoPqrs(); // esta ya retorna lista
		Gson gson = new Gson();
		return gson.toJson(estados); // convierte a JSON
	}

	public String obtenerAreasEscalamiento() {
		List<AreaEscalamiento> areas = AreaEscalamientoPQRSDAO.obtenerAreaEscalamientoPQRS(); // esta ya retorna lista
		Gson gson = new Gson();
		return gson.toJson(areas); // convierte a JSON
	}

	public String insertarPqrsWeb(String fechasolicitud, String tiposolicitud, int idtienda, String nombres,
			String apellidos, String telefono, String direccion, int idmunicipio, String comentario,
			String politicaDatos, String email) {
		String fechaFormateada = "";
		try {
			SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy");
			SimpleDateFormat formatoSalida = new SimpleDateFormat("yyyy-MM-dd");
			Date fechaDate = formatoEntrada.parse(fechasolicitud);
			fechaFormateada = formatoSalida.format(fechaDate);
		} catch (Exception e) {
			System.err.println("⚠️ Error al convertir fecha: " + fechasolicitud);
			e.printStackTrace();
		}

		ClienteCtrl clienteCtrl = new ClienteCtrl();
		Cliente cliente = clienteCtrl.obtenerClienteUltimoPedido(telefono);
		int idcliente;
		int idtiendaCliente;
		if (cliente.getIdcliente() == 0) {
			Cliente clienteObj = new Cliente();
			clienteObj.setIdtienda(idtienda);
			clienteObj.setTelefonoCelular(telefono);
			clienteObj.setTelefono(telefono);
			clienteObj.setNombres(nombres);
			clienteObj.setApellidos(apellidos);
			clienteObj.setEmail(email);
			clienteObj.setPoliticaDatos(politicaDatos);
			idcliente = ClienteDAO.insertarClienteWb(clienteObj);
			idtiendaCliente = idtienda;
		} else {
			idcliente = cliente.getIdcliente();
			idtiendaCliente = cliente.getIdtienda();
		}

		SolicitudPQRS solicitud = new SolicitudPQRS(0, fechasolicitud, tiposolicitud, idcliente, idtiendaCliente,
				nombres, apellidos, telefono, direccion, "", idmunicipio, "", 2, 2, "externa", "tienda", 0, 0.0, 0.0, 0,
				false, 0, 0, 0, 1, 2, 0, false, email);

		int idSolPQRSIns = SolicitudPQRSDAO.insertarSolicitudPQRS(solicitud);

		boolean registroComent = false;
		if (comentario != null && !comentario.trim().isEmpty()) {
			ComentarioPqrs comentarioPqrs = new ComentarioPqrs(0, 0, comentario.trim(), fechaFormateada, true);
			List<ComentarioPqrs> comentarioList = new ArrayList<>();
			comentarioList.add(comentarioPqrs);
			registroComent = SolicitudPQRSDAO.modificarComentariosPqrs(idSolPQRSIns, comentarioList);
		}

		if (idSolPQRSIns != 0 && registroComent) {
			CorreoPQRS confpqrs = new CorreoPQRS();
			if (email != null && !email.isEmpty()) {
				confpqrs.enviarConfirmacion(email, nombres, idSolPQRSIns);
			}
		}

		JSONArray listJSON = new JSONArray();
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		ResultadoJSON.put("registroComentarios", registroComent);
		listJSON.add(ResultadoJSON);

		return listJSON.toJSONString();
	}

	public String ObtenerMotivosPrioridadPqrs() {
		List<PrioridadPqrs> prioridad = SolicitudPQRSDAO.obtenerPrioridadPqrs();
		List<MotivoPqrs> motivo = SolicitudPQRSDAO.obtenerMotivosPqrs();

		Map<String, Object> respuesta = new HashMap<>();
		respuesta.put("prioridad_pqrs", prioridad);
		respuesta.put("motivo_pqrs", motivo);

		Gson gson = new Gson();
		return gson.toJson(respuesta);
	}

	public static String obtenerPendientesPqrs(String fecha) {
		List<PendientePqrs> solicitudes = SolicitudPQRSDAO.obtenerPendientePqrs(fecha); // <-- devuelve objetos Java
		Gson gson = new Gson();
		return gson.toJson(solicitudes);
	}

	public void insertarEscalamientoPQRS(EscalamientoPQRS escalamiento) {
		EscalamientoPQRSDAO.insertarEscalamientoPQRS(escalamiento);
		// Realizamos envio de correo al area encargada del escalamiento
		ArrayList correos = AreaEscalamientoPQRSDAO
				.obtenerCorreoAreaEscalamientoPQRS(escalamiento.getAreaResponsable());
		System.out.println(correos.toString());
		Correo correo = new Correo();
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		correo.setAsunto("SE LE HA ESCALADO LA PQRS # " + escalamiento.getIdSolicitudPQRS());
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		correo.setMensaje("Se le ha escalado la PQRS # " + escalamiento.getIdSolicitudPQRS()
				+ " por favor ingresar al sistema y revisar.");
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
	}

	/**
	 * Método que retorna un arreglo JSON con los escalamientos de una pqrs.
	 * 
	 * @param idSolicitudPQRS
	 * @return
	 */
	public String consultarEscalamientoPQRS(int[] idsSolicitudPQRS) {
	    ArrayList<EscalamientoPQRS> escalamientos = EscalamientoPQRSDAO.consultarEscalamientoPQRS(idsSolicitudPQRS);
	    JSONArray listJSON = new JSONArray();
	    JSONObject objectJSON;

	    for (EscalamientoPQRS escTemp : escalamientos) {
	        objectJSON = new JSONObject();
	        objectJSON.put("idescalamiento", escTemp.getIdEscalamiento());
	        objectJSON.put("idsolicitudpqrs", escTemp.getIdSolicitudPQRS());
	        objectJSON.put("arearesponsable", escTemp.getAreaResponsable());
	        objectJSON.put("fechaescalamiento", escTemp.getFechaEscalamiento());
	        objectJSON.put("fecharesolucion", escTemp.getFechaResolucion());
	        objectJSON.put("solucionado", escTemp.isSolucionado() ? "SI" : "NO");
	        listJSON.add(objectJSON);
	    }
	    return listJSON.toJSONString();
	}

	
	public String finalizarEscalamientoPQRS(int idEscalamiento)
	{
		JSONObject objectJSON = new JSONObject();
		EscalamientoPQRSDAO.finalizarEscalamientoPQRS(idEscalamiento);
		objectJSON.put("respuesta", true);
		return(objectJSON.toJSONString());
	}
	


	public String consultarEscalamientoPQRS(int idSolicitudPQRS) {
	    return consultarEscalamientoPQRS(new int[]{idSolicitudPQRS});
	}
	


	public static void main(String[] args) {

		try {
			String res = obtenerPendientesPqrs("2025-07-22");
			System.out.println(res);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

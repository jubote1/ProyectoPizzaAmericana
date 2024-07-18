package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.FocoPqrsDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.MunicipioDAO;
import capaDAOCC.OrigenPqrsDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.SolicitudPQRSDAO;
import capaDAOCC.SolicitudPQRSImagenesDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.FocoPqrs;
import capaModeloCC.OrigenPqrs;
import capaModeloCC.Pedido;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.SolicitudPQRSImagenes;
import capaModeloCC.Tienda;
import utilidadesCC.ControladorEnvioCorreo;;


public class SolicitudPQRSCtrl {
	
	/**
	 * Método de la capa controladora que recibe los parámetros de la entidad SolicitudPQRS se encarga de crear el objeto correspondiente
	 * e invoca la capa DAO para la inserción de la entidad.
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
	 * @return Retorna un entero con el idSolicitudPQRS que retorna la base de datos en la creación del mismo.
	 */
	public String insertarSolicitudPQRS(String fechaSolicitud, String tipoSolicitud, int idcliente, int idtienda,
			String nombres, String apellidos, String telefono, String direccion, String zona, int idmunicipio,
			String comentario, int idOrigen, int idFoco, String tipo, String areaResponsable)
	{
		JSONArray listJSON = new JSONArray();
		SolicitudPQRS solicitud = new SolicitudPQRS(0,fechaSolicitud, tipoSolicitud,idcliente, idtienda,
			 nombres, apellidos,  telefono,  direccion,  zona, idmunicipio,
			comentario, idOrigen, idFoco, tipo, areaResponsable);
		
		Tienda objTienda = TiendaDAO.retornarTienda(idtienda);
		String nombreTienda = objTienda.getNombreTienda();
		FocoPqrs foco = FocoPqrsDAO.retornarFocoPqrs(idFoco);
		int idSolPQRSIns = SolicitudPQRSDAO.insertarSolicitudPQRS(solicitud);
		// Se realiza envío del correo con la solicitud
		Correo correo = new Correo();
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		correo.setAsunto("SE REGISTRO PQRS # " + idSolPQRSIns);
		ArrayList correos = GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		correo.setMensaje("Se registro PQRS para el cliente " + nombres + " " + apellidos + " para la tienda " + nombreTienda + " con el siguiente comentario: " + comentario + " \n Información Adicional del cliente telefono: " + telefono + " Direccion: " + direccion + ". \n Si desea más información favor revisar en el sistema de Contact Center en el apartado de PQRS \n\n Foco de la PQRS: " + foco.getNombreFoco() + " \n\n Tipo: " + tipo + " , Area Responble: " + areaResponsable );
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
		//
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	
	/**
	 * Método creado en la capa controladora para la actualización de una PQRS
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
	public String actualizarSolicitudPQRS(int idSolicitudPQRS,String fechaSolicitud, String tipoSolicitud, int idcliente, int idtienda,
			String nombres, String apellidos, String telefono, String direccion, String zona, int idmunicipio,
			String comentario, int idOrigen, int idFoco, String tipo, String areaResponsable)
	{
		JSONArray listJSON = new JSONArray();
		SolicitudPQRS solicitud = new SolicitudPQRS(idSolicitudPQRS,fechaSolicitud, tipoSolicitud,idcliente, idtienda,
			 nombres, apellidos,  telefono,  direccion,  zona, idmunicipio,
			comentario, idOrigen, idFoco, tipo, areaResponsable);
		
		Tienda objTienda = TiendaDAO.retornarTienda(idtienda);
		String nombreTienda = objTienda.getNombreTienda();
		FocoPqrs foco = FocoPqrsDAO.retornarFocoPqrs(idFoco);
		int idSolPQRSIns = SolicitudPQRSDAO.actualizarSolicitudPQRS(solicitud);
		// Se realiza envío del correo con la solicitud
		Correo correo = new Correo();
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		correo.setAsunto("SE ACTUALIZÓ REGISTRO PQRS # " + idSolPQRSIns);
		ArrayList correos = GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		correo.setMensaje("Se registro PQRS para el cliente " + nombres + " " + apellidos + " para la tienda " + nombreTienda + " con el siguiente comentario: " + comentario + " \n Información Adicional del cliente telefono: " + telefono + " Direccion: " + direccion + ". \n Si desea más información favor revisar en el sistema de Contact Center en el apartado de PQRS \n\n Foco de la PQRS: " + foco.getNombreFoco() + " \n\n Tipo: " + tipo + " , Area Responble: " + areaResponsable );
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
		//
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	public String descartarSolicitudPQRS(int idSolicitudPQRS)
	{
		JSONArray listJSON = new JSONArray();

		int idSolPQRSIns = SolicitudPQRSDAO.descartarSolicitudPQRS(idSolicitudPQRS);
		// Se realiza envío del correo con la solicitud
		Correo correo = new Correo();
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		correo.setAsunto("SE DESCARTÓ REGISTRO PQRS # " + idSolPQRSIns);
		ArrayList correos = GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		correo.setMensaje("Se descartó  registro PQRS " + idSolicitudPQRS );
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
		//
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idSolicitudPQRS", idSolPQRSIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	public String ConsultaIntegradaSolicitudesPQRS(String fechainicial, String fechafinal, String tienda, String tipoSolicitud)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <SolicitudPQRS> consultaSolicitudes = SolicitudPQRSDAO.ConsultaIntegradaSolicitudesPQRS(fechainicial, fechafinal, tienda, tipoSolicitud);
		for (SolicitudPQRS cadaSolicitud: consultaSolicitudes){
			JSONObject cadaSolicitudJSON = new JSONObject();
			cadaSolicitudJSON.put("idconsultaPQRS", cadaSolicitud.getIdsolicitud());
			cadaSolicitudJSON.put("fechasolicitud", cadaSolicitud.getFechaSolicitud());
			cadaSolicitudJSON.put("tiposolicitud",cadaSolicitud.getTipoSolicitud());
			cadaSolicitudJSON.put("cliente", cadaSolicitud.getNombres() + " " + cadaSolicitud.getApellidos());
			cadaSolicitudJSON.put("direccion", cadaSolicitud.getDireccion());
			cadaSolicitudJSON.put("telefono", cadaSolicitud.getTelefono());
			cadaSolicitudJSON.put("comentario", cadaSolicitud.getComentario());
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
			listJSON.add(cadaSolicitudJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String validarPQRS(int PQRS, int idCliente)
	{
		JSONArray listJSON = new JSONArray();
		JSONObject respuestaJSON = new JSONObject();
		boolean respuesta = SolicitudPQRSDAO.validarPQRS(PQRS, idCliente);
		if(respuesta)
		{
			respuestaJSON.put("resultado", "OK");
		}else
		{
			respuestaJSON.put("resultado", "NOK");
		}
		listJSON.add(respuestaJSON);
		return(listJSON.toJSONString());
	}
	
	public String adicionarComentarioPQRS(int idSolicitudPQRS, String comentario)
	{
		boolean respuesta = SolicitudPQRSDAO.adicionarComentarioPQRS(idSolicitudPQRS, comentario);
		JSONArray listJSON = new JSONArray();
		JSONObject respuestaJSON = new JSONObject();
		if(respuesta)
		{
			respuestaJSON.put("resultado", "OK");
		}else
		{
			respuestaJSON.put("resultado", "NOK");
		}
		Correo correo = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOPUBLICA", "CLAVECORREOPUBLICA");
		correo.setAsunto("SE ACTUALIZÓ REGISTRO PQRS # " + idSolicitudPQRS);
		ArrayList correos = GeneralDAO.obtenerCorreosParametro("REGISTROPQRS");
		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		correo.setMensaje("Se Actualizó PQRS con el siguiente comentario: " + comentario + ". \n Si desea más información favor revisar en el sistema de Contact Center en el apartado de PQRS" );
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
		
		listJSON.add(respuestaJSON);
		return(listJSON.toJSONString());
	}
	
	//ESPACIO RESERVADO PARA TODO LO RELACIONADO CON ORIGEN_PQRS
	
	public String retornarOrigenesPqrs()
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <OrigenPqrs> origenes = OrigenPqrsDAO.retornarOrigenesPqrs();
		for (OrigenPqrs cadaOrigen: origenes){
			JSONObject cadaOrigenJSON = new JSONObject();
			cadaOrigenJSON.put("idorigen", cadaOrigen.getIdOrigen());
			cadaOrigenJSON.put("nombreorigen", cadaOrigen.getNombreOrigen());
			listJSON.add(cadaOrigenJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String obtenerFocosPqrs()
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <FocoPqrs> focosPqrs = FocoPqrsDAO.obtenerFocosPqrs();
		for (FocoPqrs cadaFoco: focosPqrs){
			JSONObject cadaFocoJSON = new JSONObject();
			cadaFocoJSON.put("idfoco", cadaFoco.getIdFoco());
			cadaFocoJSON.put("nombrefoco", cadaFoco.getNombreFoco());
			listJSON.add(cadaFocoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String insertarSolicitudPQRSImagen(SolicitudPQRSImagenes solicitud)
	{
		int idImagen = SolicitudPQRSImagenesDAO.insertarSolicitudPQRSImagen(solicitud);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("idimagen", idImagen);
		return(resultadoJSON.toJSONString());
	}
	
	public String consultarSolicitudPQRSImagenes(int idSolicitudPRQS)
	{
		ArrayList<String> imagenes = SolicitudPQRSImagenesDAO.consultarSolicitudPQRSImagenes(idSolicitudPRQS);
		JSONArray resultadoJSON = new JSONArray();
		for(int i = 0; i < imagenes.size(); i++)
		{
			JSONObject cadaImagen = new JSONObject();
			String imagen = (String) imagenes.get(i);
			cadaImagen.put("rutaimagen", imagen);
			resultadoJSON.add(cadaImagen);
			
		}
		return(resultadoJSON.toJSONString());
	}

}

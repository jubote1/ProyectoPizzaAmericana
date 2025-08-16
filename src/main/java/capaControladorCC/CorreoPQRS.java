package capaControladorCC;


import javax.mail.*;
import javax.mail.internet.*;

import org.json.JSONObject;

import com.google.gson.JsonObject;

import capaDAOCC.GeneralDAO;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import utilidadesCC.ControladorEnvioCorreo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Properties;

public class CorreoPQRS {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    private static Session configurarSesion(CorreoElectronico infoCorreo) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(infoCorreo.getCuentaCorreo(), infoCorreo.getClaveCorreo());
            }
        });
    }

    private static String cargarPlantilla(String nombreArchivo) throws Exception {
        InputStream input = CorreoPQRS.class.getClassLoader().getResourceAsStream("templates/" + nombreArchivo);
        if (input == null) throw new Exception("No se encontró la plantilla: " + nombreArchivo);
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static JSONObject enviarCorreo(String destinatario, String asunto, String contenidoHtml, CorreoElectronico infoCorreo) throws Exception {
    	JSONObject json = new JSONObject();
  	    
  	    try {
  	        Session session = configurarSesion(infoCorreo);

  	        Message message = new MimeMessage(session);
  	        message.setFrom(new InternetAddress(infoCorreo.getCuentaCorreo()));
  	        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
  	        message.setSubject(asunto);

  	        MimeBodyPart htmlPart = new MimeBodyPart();
  	        htmlPart.setContent(contenidoHtml, "text/html; charset=utf-8");

  	        Multipart multipart = new MimeMultipart();
  	        multipart.addBodyPart(htmlPart);
  	        message.setContent(multipart);

  	        Transport.send(message);
  	        
  	        // ✅ JSON de éxito
  	        json.put("success", true);
  	        json.put("message", ""); // mensaje vacío si todo fue bien
  	    }catch(Exception e) {
  	    	
  	        e.printStackTrace();
  	        // ❌ JSON de error
  	        json.put("success", false);
  	        json.put("message", "Error al enviar el correo: " + e.getMessage());
  	    }

  	    return json;

    }

    public static JSONObject enviarConfirmacion(String correo, String nombreCliente, int idpqrs) {
    	JSONObject json = new JSONObject();
        try {
        
            CorreoElectronico credenciales = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");

            String plantilla = cargarPlantilla("ConfirmacionPqrs.html");
            String contenido = plantilla
                .replace("{{NOMBRE_CLIENTE}}", nombreCliente)
                .replace("{{ID_PQRS}}", String.valueOf(idpqrs));

            String asunto = "Confirmación de PQRS - Radicado #" + idpqrs + " | Pizza Americana";
            json = enviarCorreo(correo, asunto, contenido, credenciales);

        } catch (Exception e) {
            e.printStackTrace();
  	        json.put("success", false);
  	        json.put("message", "Error al enviar el correo: " + e.getMessage());

        }
        
        return json;
    }

    public static JSONObject enviarRespuesta(String correo, String nombreCliente, int idpqrs, String respuesta) {
    	JSONObject json = new JSONObject();
    	try {
        	
            CorreoElectronico credenciales = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");

            String plantilla = cargarPlantilla("RespuestaPqrs.html");
            String contenido = plantilla
                .replace("{{NOMBRE_CLIENTE}}", nombreCliente)
                .replace("{{ID_PQRS}}", String.valueOf(idpqrs))
                .replace("{{RESPUESTA_PQRS}}", respuesta);

            String asunto = "Respuesta a su PQRS - Radicado #" + idpqrs + " | Pizza Americana";
            json = enviarCorreo(correo, asunto, contenido, credenciales);

        } catch (Exception e) {
            e.printStackTrace();
  	        json.put("success", false);
  	        json.put("message", "Error al enviar el correo: " + e.getMessage());
        }
    	return json;
    }
    
    
    public static JSONObject enviarParsing(String correoCliente, String nombreCliente,String  telefono, int idpqrs) {
    	JSONObject json = new JSONObject();
        try {
			//Recuperamos el correo para envío del parsing
			ArrayList correos = GeneralDAO.obtenerCorreosParametro("PARSERENCUESTAFINALPQRS");

					Correo correo = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
					correo.setContrasena(infoCorreo.getClaveCorreo());
					correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
					correo.setAsunto("ENCUESTA SATISFACCION PQRS # " + idpqrs);
					String mensajeCuerpoCorreo = 
						    "ID PQRS: " + idpqrs + "<br>" +
						    "Nombre cliente: " + nombreCliente + "<br>" +
						    "Número de teléfono: " + telefono + "<br>" +
						    "Correo electrónico: " + correoCliente;

					correo.setMensaje(mensajeCuerpoCorreo);ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					boolean resp = contro.enviarCorreo();
					
					if(resp) {
				        json.put("success", true);
			  	        json.put("message", ""); // mensaje vacío si todo fue bien
					}else {
				        json.put("success", false);
			  	        json.put("message", "Ocurrio un error al enviar el correo que dispara la encuesta de satisfacción.");

					}
        } catch (Exception e) {
            e.printStackTrace();
	        json.put("success", false);
  	        json.put("message", "Error al enviar el correo que dispara la encuesta de satisfacción: " + e.getMessage());
        }

        return json;
    }
}

package utilidadesCC;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import capaDAOCC.ParametrosDAO;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.Correo;

public class ControladorEnvioCorreo {

/**
 * Tiempo maximo, en milisegundos, para cada etapa del dialogo SMTP: conectar,
 * leer y escribir. Antes no se definia ninguno y el valor por defecto de
 * JavaMail es esperar de forma indefinida, asi que un Gmail lento dejaba
 * colgada la peticion que disparo el correo.
 */
private static final String MILIS_ESPERA_SMTP = "5000";

private Correo  c;
private ArrayList correos;

public ControladorEnvioCorreo(Correo co,ArrayList correosenv)
{
	this.c= co;
	this.correos = correosenv;
}

public boolean enviarCorreo()
{
	try
	{
		Properties p = new Properties();
		// Tiempos de espera del SMTP, en milisegundos. Sin estas tres propiedades
		// JavaMail espera de forma indefinida, y un Gmail lento congela la peticion
		// que disparo el correo. Ver MILIS_ESPERA_SMTP.
		p.setProperty("mail.smtp.connectiontimeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.setProperty("mail.smtp.timeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.setProperty("mail.smtp.writetimeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.put("mail.smtp.host", "smtp.gmail.com");
		p.put("mail.smtp.ssl.protocols", "TLSv1.2");
		p.setProperty("mail.smtp.starttls.enable", "true");
		p.setProperty("mail.smtp.port", "587");
		p.setProperty("mail.smtp.user", c.getUsuarioCorreo());
		p.setProperty("mail.smtp.auth", "true");
		
		Session s = Session.getDefaultInstance(p, null);
		BodyPart texto = new MimeBodyPart();
		texto.setContent(c.getMensaje(), "text/html; charset=utf-8");
		//texto.setText(c.getMensaje());
		MimeMultipart m = new MimeMultipart();
		m.addBodyPart(texto);
		MimeMessage mensaje = new MimeMessage(s);
		mensaje.setFrom(new InternetAddress(c.getUsuarioCorreo()));
		//Ponemos un control para cuando no hay destinatarios del correo y evitarse una demora en el env�o
		if(correos.size() == 0)
		{
			return(false);
		}
		for(int i = 0; i< correos.size(); i++)
		{
			mensaje.addRecipient(Message.RecipientType.TO, new InternetAddress((String)correos.get(i)));
		}
		mensaje.setSubject(c.getAsunto());
		mensaje.setContent(m);
		Transport t = s.getTransport("smtp");
		t.connect(c.getUsuarioCorreo(),c.getContrasena());
		t.sendMessage(mensaje, mensaje.getAllRecipients());
		t.close();
		return(true);
		
	}
	catch(Exception e)
	{
		Date fecha = new Date();
		System.out.println(e.toString());
		//Desde este punto enviaremos un correo para notificar problemas en env�o correo
		//Aqui daremos alcance a aquellas situaciones de problemas puntuales con la cuenta
		Correo correo = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
		ArrayList correos = new ArrayList();
		correo.setAsunto(" Ojo problemas con envio de correos " + fecha.toString());
		String correoEle = "jubote1@gmail.com";
		correos.add(correoEle);
		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		correo.setMensaje(e.toString() + " " + " Se tuvo problemas enviando correo, por favor revisar " + c.getMensaje());
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreoContingencia();
		return(false);
	}
	
}

public boolean enviarCorreoContingencia()
{
	try
	{
		Properties p = new Properties();
		// Tiempos de espera del SMTP, en milisegundos. Sin estas tres propiedades
		// JavaMail espera de forma indefinida, y un Gmail lento congela la peticion
		// que disparo el correo. Ver MILIS_ESPERA_SMTP.
		p.setProperty("mail.smtp.connectiontimeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.setProperty("mail.smtp.timeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.setProperty("mail.smtp.writetimeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.put("mail.smtp.host", "smtp.gmail.com");
		p.put("mail.smtp.ssl.protocols", "TLSv1.2");
		p.setProperty("mail.smtp.starttls.enable", "true");
		p.setProperty("mail.smtp.port", "587");
		p.setProperty("mail.smtp.user", c.getUsuarioCorreo());
		p.setProperty("mail.smtp.auth", "true");
		
		Session s = Session.getDefaultInstance(p, null);
		BodyPart texto = new MimeBodyPart();
		texto.setContent(c.getMensaje(), "text/html; charset=utf-8");
		//texto.setText(c.getMensaje());
		MimeMultipart m = new MimeMultipart();
		m.addBodyPart(texto);
		MimeMessage mensaje = new MimeMessage(s);
		mensaje.setFrom(new InternetAddress(c.getUsuarioCorreo()));
		//Ponemos un control para cuando no hay destinatarios del correo y evitarse una demora en el env�o
		if(correos.size() == 0)
		{
			return(false);
		}
		for(int i = 0; i< correos.size(); i++)
		{
			mensaje.addRecipient(Message.RecipientType.TO, new InternetAddress((String)correos.get(i)));
		}
		mensaje.setSubject(c.getAsunto());
		mensaje.setContent(m);
		Transport t = s.getTransport("smtp");
		t.connect(c.getUsuarioCorreo(),c.getContrasena());
		t.sendMessage(mensaje, mensaje.getAllRecipients());
		t.close();
		return(true);
		
	}
	catch(Exception e)
	{
		System.out.println(e.toString());
		return(false);
	}
	
}


public boolean enviarCorreoInstitucional()
{
	try
	{
		Properties p = new Properties();
		// Tiempos de espera del SMTP, en milisegundos. Sin estas tres propiedades
		// JavaMail espera de forma indefinida, y un Gmail lento congela la peticion
		// que disparo el correo. Ver MILIS_ESPERA_SMTP.
		p.setProperty("mail.smtp.connectiontimeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.setProperty("mail.smtp.timeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.setProperty("mail.smtp.writetimeout", String.valueOf(MILIS_ESPERA_SMTP));
		p.put("mail.smtp.host", "mail.pizzaamericana.co");
		//p.setProperty("mail.smtp.starttls.enable", "true");
		p.setProperty("mail.smtp.port", "587");
		p.setProperty("mail.smtp.user", c.getUsuarioCorreo());
		p.setProperty("mail.smtp.auth", "true");
		
		Session s = Session.getDefaultInstance(p, null);
		BodyPart texto = new MimeBodyPart();
		texto.setContent(c.getMensaje(), "text/html; charset=utf-8");
		//texto.setText(c.getMensaje());
		MimeMultipart m = new MimeMultipart();
		m.addBodyPart(texto);
		MimeMessage mensaje = new MimeMessage(s);
		mensaje.setFrom(new InternetAddress(c.getUsuarioCorreo()));
		//Ponemos un control para cuando no hay destinatarios del correo y evitarse una demora en el env�o
		if(correos.size() == 0)
		{
			return(false);
		}
		for(int i = 0; i< correos.size(); i++)
		{
			mensaje.addRecipient(Message.RecipientType.TO, new InternetAddress((String)correos.get(i)));
		}
		mensaje.setSubject(c.getAsunto());
		mensaje.setContent(m);
		Transport t = s.getTransport("smtp");
		t.connect(c.getUsuarioCorreo(),c.getContrasena());
		t.sendMessage(mensaje, mensaje.getAllRecipients());
		t.close();
		return(true);
		
	}
	catch(Exception e)
	{
		System.out.println(e.toString());
		return(false);
	}
	
}

public static CorreoElectronico recuperarCorreo(String variableCuenta, String variableClave)
{
	String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico(variableCuenta);
	String claveCorreo = ParametrosDAO.retornarValorAlfanumerico(variableClave);
	CorreoElectronico respuesta = new CorreoElectronico(cuentaCorreo, claveCorreo);
	return(respuesta);
}
	

/**
 * Envia el correo en un hilo aparte y retorna de inmediato.
 *
 * Para alertas operativas que no deben demorar la peticion que las origina. El
 * caso que motivo esto: el servicio de la tienda virtual mandaba el correo de
 * alerta dentro del request, el dialogo SMTP con Gmail se tomaba varios
 * segundos, y Gloria Food reintentaba el pedido por falta de respuesta, con lo
 * que se generaban intentos duplicados.
 *
 * No retorna si el envio funciono, porque cuando se llama todavia no se sabe.
 * Usar enviarCorreo() cuando el resultado importe.
 */
public void enviarCorreoAsincrono()
{
	final ControladorEnvioCorreo controlador = this;
	Thread hilo = new Thread(new Runnable()
	{
		public void run()
		{
			try
			{
				controlador.enviarCorreo();
			}
			catch(Exception e)
			{
				// Silencio deliberado: una alerta que no sale no puede tumbar nada
				System.out.println("enviarCorreoAsincrono: " + e.toString());
			}
		}
	});
	hilo.setDaemon(true);
	hilo.setName("envio-correo-asincrono");
	hilo.start();
}
}

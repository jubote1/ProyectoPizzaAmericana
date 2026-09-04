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

/**
 * Por que fallo un envio. Antes enviarCorreo solo retornaba true o false, y
 * con eso quien llamaba no podia distinguir una direccion mala de un problema
 * pasajero de red. Esa confusion tenia una consecuencia concreta: el envio del
 * link de pago marcaba email_correcto = 'N' en el cliente ante cualquier
 * fallo, asi que un Gmail lento durante cinco segundos dejaba marcado como
 * incorrecto el correo de un cliente que estaba perfecto.
 */
public enum ResultadoEnvio
{
	/** El correo salio. */
	ENVIADO,
	/** La direccion del destinatario esta mal escrita o el servidor la rechazo. */
	DIRECCION_INVALIDA,
	/** Timeout, caida de red, credenciales, limite del proveedor. Se puede reintentar. */
	FALLA_TRANSITORIA,
	/** No habia a quien enviarle. */
	SIN_DESTINATARIOS
}

/**
 * Envia el correo. Se conserva para los llamadores que solo necesitan saber si
 * salio o no; si hay que actuar distinto segun el motivo del fallo, usar
 * enviarCorreoClasificado.
 */
public boolean enviarCorreo()
{
	return (enviarCorreoClasificado() == ResultadoEnvio.ENVIADO);
}

/**
 * Envia el correo diciendo que paso. Es la version que hay que usar cuando la
 * decision de quien llama dependa del motivo del fallo, como marcar el correo
 * de un cliente por incorrecto.
 */
public ResultadoEnvio enviarCorreoClasificado()
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
			return(ResultadoEnvio.SIN_DESTINATARIOS);
		}
		for(int i = 0; i< correos.size(); i++)
		{
			//Se valida la direccion antes de conectarse al servidor. El segundo
			//parametro en true hace la validacion estricta: asi una direccion mal
			//escrita se detecta aqui como AddressException, en vez de gastarse una
			//conexion SMTP para terminar en un error que no se sabe interpretar.
			String direccion = (String)correos.get(i);
			mensaje.addRecipient(Message.RecipientType.TO,
					new InternetAddress(direccion == null ? "" : direccion.trim(), true));
		}
		mensaje.setSubject(c.getAsunto());
		mensaje.setContent(m);
		Transport t = s.getTransport("smtp");
		t.connect(c.getUsuarioCorreo(),c.getContrasena());
		t.sendMessage(mensaje, mensaje.getAllRecipients());
		t.close();
		return(ResultadoEnvio.ENVIADO);

	}
	catch(Exception e)
	{
		Date fecha = new Date();
		System.out.println(e.toString());

		//Se separa la direccion mala del problema pasajero. Es la razon de ser de
		//este metodo: quien llama tiene que poder saber si el correo del cliente
		//esta mal o si simplemente el servidor no respondio a tiempo.
		ResultadoEnvio resultado = clasificarFallo(e);

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
		return(resultado);
	}

}

/**
 * Decide si el fallo fue por la direccion del destinatario o por algo pasajero.
 *
 * Solo se considera direccion invalida cuando el propio JavaMail lo dice: o la
 * direccion no se pudo ni construir (AddressException), o el servidor la
 * rechazo de forma explicita (SendFailedException con direcciones invalidas).
 * Todo lo demas (timeout, red caida, credenciales, limite del proveedor) es
 * transitorio y no dice nada sobre el correo del cliente.
 */
private ResultadoEnvio clasificarFallo(Exception e)
{
	Throwable causa = e;
	while(causa != null)
	{
		if(causa instanceof javax.mail.internet.AddressException)
		{
			return(ResultadoEnvio.DIRECCION_INVALIDA);
		}
		if(causa instanceof javax.mail.SendFailedException)
		{
			javax.mail.SendFailedException fallo = (javax.mail.SendFailedException) causa;
			javax.mail.Address[] invalidas = fallo.getInvalidAddresses();
			if(invalidas != null && invalidas.length > 0)
			{
				return(ResultadoEnvio.DIRECCION_INVALIDA);
			}
		}
		causa = causa.getCause();
	}
	return(ResultadoEnvio.FALLA_TRANSITORIA);
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

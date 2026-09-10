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

import capaDAOCC.GeneralDAO;
import capaDAOCC.ParametrosDAO;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.Correo;

public class ControladorEnvioCorreo {

/**
 * Tiempo maximo, en milisegundos, para cada etapa del dialogo SMTP: conectar,
 * leer y escribir. Antes no se definia ninguno y el valor por defecto de
 * JavaMail es esperar de forma indefinida, asi que un Gmail lento dejaba
 * colgada la peticion que disparo el correo.
 *
 * La espera corta es la que se usa cuando hay alguien esperando del otro lado:
 * un asesor en el contact center, o el ciclo que le pide pedidos a la tienda
 * virtual. Ahi no importa tanto que el correo salga como que nadie se quede
 * bloqueado.
 */
private static final String MILIS_ESPERA_SMTP = "5000";

/**
 * Espera larga, para los reintentos que corren en segundo plano. Ahi ya nadie
 * esta esperando, asi que se le puede dar al servidor el tiempo que necesite.
 *
 * La distincion no es entre correos importantes y accesorios: es entre si hay
 * o no alguien esperando. El correo del link de pago es de los mas importantes
 * que manda el sistema, y aun asi su primer intento usa la espera corta,
 * porque se dispara con un asesor mirando la pantalla.
 */
private static final String MILIS_ESPERA_SEGUNDO_PLANO = "30000";

/** Cuanto esperar antes de cada reintento, en milisegundos. */
private static final long[] ESPERA_ENTRE_REINTENTOS = { 3000L, 10000L, 30000L };

/**
 * Minutos que se acumulan las perdidas antes de mandar UNA alerta.
 *
 * Con esto nunca sale mas de una alerta cada media hora, en vez de una por cada
 * correo perdido. Media hora es suficiente para juntar una racha y lo bastante
 * corto para enterarse el mismo dia.
 */
private static final long MINUTOS_VENTANA_ALERTA = 30L;

/**
 * Cuantas perdidas en la ventana obligan a avisar de una, sin esperar.
 *
 * Diez correos perdidos en media hora ya no es un correo que se cayo: es algo
 * roto -la cuenta bloqueada, el servidor caido, la clave cambiada- y eso no
 * puede esperar.
 */
private static final int PERDIDAS_PARA_AVISO_INMEDIATO = 10;

/** Lo perdido desde la ultima alerta. Se lee y escribe siempre bajo su candado. */
private static final ArrayList<String> PERDIDOS = new ArrayList<String>();

/** Cuando salio la ultima alerta, para no mandar dos seguidas. */
private static long ultimoAviso = 0L;

/** El reloj que vacia la lista. Uno solo para toda la aplicacion. */
private static java.util.Timer reloj = null;

private Correo  c;
private ArrayList correos;

/** Espera SMTP de esta instancia. Arranca en la corta. */
private String milisEspera = MILIS_ESPERA_SMTP;

/**
 * Si este envio avisa por correo cuando falla. Se apaga durante los reintentos
 * para que un solo correo perdido no genere un aviso por intento.
 */

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
 * Si una direccion de correo sirve para intentar un envio.
 *
 * Se valida antes de conectarse al servidor por una razon practica: intentar
 * enviarle a una direccion mal escrita gasta una conexion SMTP, se demora, y
 * termina en un error que despues genera una alarma. Y no hay nada que
 * aprender de ese error, porque la direccion ya se sabia mala desde el
 * principio.
 *
 * La regla es deliberadamente sencilla y no pretende decidir si el buzon
 * existe, que eso solo lo sabe el servidor del destinatario. Descarta lo que
 * seguro no va a funcionar: vacios, espacios en medio, sin arroba, sin dominio
 * o sin punto en el dominio. Es la misma regla del script de limpieza de
 * email_correcto, para que no digan cosas distintas.
 */
public static boolean esDireccionValida(String direccion)
{
	if(direccion == null)
	{
		return(false);
	}
	String limpia = direccion.trim();
	if(limpia.length() == 0 || limpia.contains(" "))
	{
		return(false);
	}
	int arroba = limpia.indexOf('@');
	//Una sola arroba, con algo antes y algo despues
	if(arroba <= 0 || arroba != limpia.lastIndexOf('@') || arroba == limpia.length() - 1)
	{
		return(false);
	}
	String dominio = limpia.substring(arroba + 1);
	int punto = dominio.lastIndexOf('.');
	//El dominio necesita un punto que no este al principio ni al final, y una
	//terminacion de al menos dos letras
	if(punto <= 0 || punto == dominio.length() - 1 || dominio.length() - punto - 1 < 2)
	{
		return(false);
	}
	try
	{
		//El modo estricto de JavaMail atrapa lo que la revision de arriba no ve
		new InternetAddress(limpia, true);
	}
	catch(Exception e)
	{
		return(false);
	}
	return(true);
}

/**
 * Envia el correo. Se conserva para los llamadores que solo necesitan saber si
 * salio o no; si hay que actuar distinto segun el motivo del fallo, usar
 * enviarCorreoClasificado.
 */
/**
 * Envia el correo y, si falla por algo pasajero, sigue reintentando en segundo
 * plano. Retorna si el PRIMER intento salio.
 *
 * Antes esto era un solo intento y listo: un timeout de cinco segundos dejaba el
 * correo perdido para siempre. El caso que lo destapo fue el link de pago que le
 * avisa a Kommo para mandar el WhatsApp: fallo por Read timed out y la clienta
 * nunca recibio su link.
 *
 * Se cambio aqui, en el metodo comun, y no en cada llamador: hay 57 llamados a
 * este metodo y solo tres miran el valor que retorna, ninguno de ellos para
 * intentar por otro canal. Asi que reintentar por detras no puede duplicar nada.
 *
 * Quien necesite saber POR QUE fallo -para marcar el correo de un cliente como
 * incorrecto, por ejemplo- usa enviarConReintentos, que retorna el motivo.
 */
public boolean enviarCorreo()
{
	return (enviarConReintentos() == ResultadoEnvio.ENVIADO);
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
		// que disparo el correo. La espera es la de esta instancia: corta cuando
		// hay alguien esperando, larga en los reintentos de segundo plano.
		p.setProperty("mail.smtp.connectiontimeout", this.milisEspera);
		p.setProperty("mail.smtp.timeout", this.milisEspera);
		p.setProperty("mail.smtp.writetimeout", this.milisEspera);
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

		/*
		 * Las direcciones se revisan ANTES de abrir la conexion. Si ninguna sirve
		 * no se intenta nada: no se gasta una conexion SMTP, no hay demora, y
		 * sobre todo no se produce un error que despues genere una alarma por algo
		 * que ya se sabia desde el principio.
		 *
		 * Cuando hay varias y solo algunas estan malas, se envia a las buenas.
		 * Antes una sola direccion mal escrita en la lista tumbaba el envio
		 * completo, asi que un reporte con seis destinatarios no le llegaba a
		 * ninguno por culpa de uno.
		 */
		int validas = 0;
		StringBuilder descartadas = new StringBuilder();
		for(int i = 0; i< correos.size(); i++)
		{
			String direccion = (String)correos.get(i);
			if(!esDireccionValida(direccion))
			{
				if(descartadas.length() > 0)
				{
					descartadas.append(", ");
				}
				descartadas.append(direccion);
				continue;
			}
			mensaje.addRecipient(Message.RecipientType.TO, new InternetAddress(direccion.trim(), true));
			validas++;
		}
		if(descartadas.length() > 0)
		{
			System.out.println("Correo \"" + c.getAsunto() + "\": se omiten direcciones invalidas: "
					+ descartadas.toString());
		}
		if(validas == 0)
		{
			//Se retorna desde dentro del try, asi que no pasa por el catch y no se
			//manda ninguna alarma. Una direccion mala no es un incidente del
			//servidor de correo.
			System.out.println("Correo \"" + c.getAsunto()
					+ "\": no se envia, ninguna direccion de destino es valida.");
			return(ResultadoEnvio.DIRECCION_INVALIDA);
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
		System.out.println(e.toString());

		//Se separa la direccion mala del problema pasajero. Es la razon de ser de
		//este metodo: quien llama tiene que poder saber si el correo del cliente
		//esta mal o si simplemente el servidor no respondio a tiempo.
		ResultadoEnvio resultado = clasificarFallo(e);

		//Aqui NO se avisa nada, a proposito.
		//
		//Antes salia un correo "Ojo problemas con envio de correos" por cada
		//intento fallido, con el cuerpo del correo original adentro. Eso tenia dos
		//problemas: era ruido -un timeout de cinco segundos generaba una alarma de
		//algo que casi siempre se resuelve solo- y hacia circular datos personales
		//del cliente, incluido el link de pago vivo, en una bandeja de errores.
		//
		//Ahora el unico aviso es avisarFalloDefinitivo, que suena cuando de verdad
		//se perdio el correo despues de agotar los reintentos, va agregado y no
		//lleva el cuerpo.
		return(resultado);
	}

}

/**
 * Envia el correo y, si falla por algo pasajero, sigue reintentando en segundo
 * plano hasta lograrlo o agotar los intentos.
 *
 * Retorna el resultado del PRIMER intento, que se hace con la espera corta.
 * Eso es a proposito: quien llama necesita una respuesta ya, no quedarse
 * esperando un minuto y medio. Este metodo se dispara desde el ciclo que le
 * pide pedidos a la tienda virtual y desde peticiones del contact center, y en
 * los dos casos hay alguien o algo esperando del otro lado.
 *
 * Los reintentos corren aparte, con la espera larga y sin avisar por correo en
 * cada vuelta. El aviso se manda una sola vez, y solo si se agotaron todos los
 * intentos: asi un correo que se logro en el segundo intento no genera ninguna
 * alarma, que es justamente lo que hoy sobra.
 *
 * No se reintenta una direccion invalida. Reintentar una direccion mal escrita
 * es perder tiempo tres veces por el mismo motivo.
 *
 * @return el resultado del primer intento
 */
public ResultadoEnvio enviarConReintentos()
{
	//Primer intento: espera corta y sin aviso, porque si hay reintentos el aviso
	//lo decide el hilo de segundo plano.
	final ResultadoEnvio primero = enviarCorreoClasificado();

	if(primero == ResultadoEnvio.ENVIADO || primero == ResultadoEnvio.SIN_DESTINATARIOS)
	{
		return(primero);
	}
	if(primero == ResultadoEnvio.DIRECCION_INVALIDA)
	{
		//La direccion esta mal: no hay nada que reintentar y tampoco nada que
		//avisar por correo. No es un incidente del servidor, es un dato del
		//cliente que esta mal, y eso queda registrado donde corresponde: en el
		//log del pedido y en email_correcto.
		System.out.println("Correo \"" + this.c.getAsunto()
				+ "\": direccion invalida, no se reintenta y no se genera alarma.");
		return(primero);
	}

	//Falla transitoria: los reintentos van en su propio hilo con la espera larga.
	final Correo correoReintento = this.c;
	final ArrayList correosReintento = this.correos;
	Thread hilo = new Thread(new Runnable()
	{
		public void run()
		{
			for(int intento = 0; intento < ESPERA_ENTRE_REINTENTOS.length; intento++)
			{
				try
				{
					Thread.sleep(ESPERA_ENTRE_REINTENTOS[intento]);
				}
				catch(InterruptedException ie)
				{
					Thread.currentThread().interrupt();
					return;
				}

				ControladorEnvioCorreo envio = new ControladorEnvioCorreo(correoReintento, correosReintento);
				envio.milisEspera = MILIS_ESPERA_SEGUNDO_PLANO;
				ResultadoEnvio resultado = envio.enviarCorreoClasificado();

				if(resultado == ResultadoEnvio.ENVIADO)
				{
					System.out.println("Correo enviado en el reintento " + (intento + 1) + ": "
							+ correoReintento.getAsunto());
					return;
				}
				if(resultado == ResultadoEnvio.DIRECCION_INVALIDA
						|| resultado == ResultadoEnvio.SIN_DESTINATARIOS)
				{
					//Nada que seguir intentando y nada que avisar: no es un incidente.
					System.out.println("Reintento " + (intento + 1) + " descartado por " + resultado
							+ ": " + correoReintento.getAsunto());
					return;
				}
			}

			//Se agotaron los intentos: aqui si vale avisar, porque el correo se perdio.
			ControladorEnvioCorreo avisador = new ControladorEnvioCorreo(correoReintento, correosReintento);
			avisador.avisarFalloDefinitivo("No se logro enviar despues de "
					+ (ESPERA_ENTRE_REINTENTOS.length + 1) + " intentos.");
		}
	});
	hilo.setDaemon(true);
	hilo.setName("reintento-correo");
	hilo.start();

	return(primero);
}

/**
 * Anota que un correo se perdio de forma definitiva y decide si ya toca avisar.
 *
 * Antes salia un correo de alerta por CADA fallo. Con eso, una racha de Gmail
 * lento llenaba la bandeja de veinte alertas iguales y la que importaba se
 * perdia entre las demas. Ahora se acumulan y sale UNA sola alerta por ventana,
 * con el conteo y los asuntos.
 *
 * Nunca sale mas de una alerta cada MINUTOS_VENTANA_ALERTA, y siempre sale si
 * hubo aunque sea una perdida: no se calla una perdida sola por no alcanzar un
 * umbral. Si la racha es grande -PERDIDAS_PARA_AVISO_INMEDIATO o mas- no se
 * espera la ventana, porque eso ya no es un correo perdido sino algo roto.
 *
 * A diferencia del aviso que salia antes, este NO incluye el cuerpo del correo
 * original: ese cuerpo trae el link de pago, el telefono y el correo del
 * cliente, y no tiene por que circular en una bandeja de errores.
 */
private void avisarFalloDefinitivo(String motivo)
{
	try
	{
		synchronized(ControladorEnvioCorreo.PERDIDOS)
		{
			ControladorEnvioCorreo.PERDIDOS.add(motivo + " Asunto: " + this.c.getAsunto()
					+ ". Destinatarios: " + this.correos.size() + ".");
			ControladorEnvioCorreo.programarVaciado();
			boolean rachaGrande = ControladorEnvioCorreo.PERDIDOS.size() >= PERDIDAS_PARA_AVISO_INMEDIATO;
			long desdeUltimo = System.currentTimeMillis() - ControladorEnvioCorreo.ultimoAviso;
			boolean paso = desdeUltimo >= MINUTOS_VENTANA_ALERTA * 60000L;
			if(rachaGrande || paso)
			{
				ControladorEnvioCorreo.vaciarYAvisar();
			}
		}
	}
	catch(Exception e)
	{
		System.out.println("No se pudo anotar el correo no entregado: " + e.toString());
	}
}

/**
 * Arranca, una sola vez, el reloj que vacia la lista de perdidos.
 *
 * Hace falta porque si se pierde un solo correo y despues no se pierde ninguno
 * mas, nadie volveria a pasar por avisarFalloDefinitivo y esa perdida se
 * quedaria sin avisar. El reloj es un unico hilo demonio para toda la
 * aplicacion, no uno por correo.
 */
private static synchronized void programarVaciado()
{
	if(ControladorEnvioCorreo.reloj != null)
	{
		return;
	}
	ControladorEnvioCorreo.reloj = new java.util.Timer("alerta-correos-perdidos", true);
	long cada = MINUTOS_VENTANA_ALERTA * 60000L;
	ControladorEnvioCorreo.reloj.schedule(new java.util.TimerTask()
	{
		public void run()
		{
			synchronized(ControladorEnvioCorreo.PERDIDOS)
			{
				if(!ControladorEnvioCorreo.PERDIDOS.isEmpty())
				{
					ControladorEnvioCorreo.vaciarYAvisar();
				}
			}
		}
	}, cada, cada);
}

/**
 * Manda UNA alerta con todo lo acumulado y limpia la lista.
 *
 * Se llama siempre con el candado de PERDIDOS tomado.
 *
 * Los destinatarios salen de la lista ERROR de parametros_correo, no de una
 * direccion escrita en el codigo. Si esa lista queda vacia no sale la alerta y
 * queda dicho en la salida del servidor: es la unica forma de apagar estas
 * alertas, y es a proposito que sea visible.
 */
private static void vaciarYAvisar()
{
	final int cuantos = ControladorEnvioCorreo.PERDIDOS.size();
	if(cuantos == 0)
	{
		return;
	}
	final StringBuilder detalle = new StringBuilder();
	for(int i = 0; i < ControladorEnvioCorreo.PERDIDOS.size(); i++)
	{
		detalle.append(i + 1).append(". ").append(ControladorEnvioCorreo.PERDIDOS.get(i)).append("\n<br>");
	}
	ControladorEnvioCorreo.PERDIDOS.clear();
	ControladorEnvioCorreo.ultimoAviso = System.currentTimeMillis();
	try
	{
		ArrayList destinos = GeneralDAO.obtenerCorreosParametro("ERROR");
		if(destinos == null || destinos.isEmpty())
		{
			System.out.println("Hay " + cuantos + " correos no entregados pero la lista ERROR de "
					+ "parametros_correo esta vacia, asi que no se avisa a nadie.");
			return;
		}
		Correo aviso = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR",
				"CLAVECORREOERROR");
		aviso.setAsunto(cuantos + (cuantos == 1 ? " correo no entregado" : " correos no entregados"));
		aviso.setContrasena(infoCorreo.getClaveCorreo());
		aviso.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		aviso.setMensaje("Estos correos no salieron despues de agotar los reintentos, en los ultimos "
				+ MINUTOS_VENTANA_ALERTA + " minutos:\n<br><br>" + detalle.toString()
				+ "\n<br>Fecha del aviso: " + new Date().toString());
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(aviso, destinos);
		contro.enviarCorreoContingencia();
	}
	catch(Exception e)
	{
		System.out.println("No se pudo avisar de " + cuantos + " correos no entregados: " + e.toString());
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

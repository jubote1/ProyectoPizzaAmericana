package utilidadesCC;

/**
 * Armado del correo que recibe el cliente cuando gana un premio en la ruleta de
 * la encuesta de servicio.
 *
 * Va un solo correo con todo: el agradecimiento por contestar, el premio que
 * gano, el codigo para redimirlo y las condiciones. Antes eran dos mensajes
 * separados porque la oferta se asignaba a mano en otro momento; ahora la
 * dispersion asigna la oferta y conoce el codigo al mismo tiempo, asi que
 * partirlo en dos solo le llenaria el buzon al cliente.
 *
 * Cuatro decisiones de armado que importan:
 *
 *  1. El logo se referencia por URL publica y NO en base64: Gmail descarta las
 *     imagenes embebidas asi. La URL llega como parametro para no quedar quemada,
 *     y tiene que ser publica, porque el servidor del central esta en una IP
 *     interna que el cliente no alcanza.
 *  2. El correo se sostiene sin el logo. Gmail y Outlook bloquean imagenes por
 *     defecto cuando el remitente no esta en contactos, asi que el texto
 *     alternativo va estilizado en blanco y negrita: si la imagen no carga se lee
 *     PIZZA AMERICANA sobre la franja azul, no un icono roto.
 *  3. Todo con tablas y estilos en linea. Los clientes de correo no aplican CSS
 *     de hoja aparte ni entienden flex o grid.
 *  4. La fecha de vencimiento se muestra concreta y en letras. El texto anterior
 *     decia "una semana" y las ofertas caducan a los 15 o 16 dias: asi el cliente
 *     no cuenta dias ni el texto se desactualiza si cambia la oferta.
 *
 * @author Pizza Americana
 */
public class PlantillaCorreoPremio {

	/** Azul de marca. */
	private static final String AZUL = "#20287f";
	/** Rojo de marca. */
	private static final String ROJO = "#d0201f";
	/** Amarillo de marca, para resaltar el codigo. */
	private static final String AMARILLO = "#f5c518";
	/** Gris del texto corrido. */
	private static final String GRIS = "#444444";
	/** Gris de las notas al pie. */
	private static final String GRIS_SUAVE = "#888888";

	/** Telefono del contact center, unico punto donde se define. */
	private static final String TELEFONO_CONTACTO = "604 4444553";

	/**
	 * Asunto del correo.
	 *
	 * @return el asunto
	 */
	public static String asunto() {
		return ("Tu premio Pizza Americana");
	}

	/**
	 * Cuerpo del correo en HTML.
	 *
	 * @param nombreCliente    nombre del cliente. Si viene vacio se saluda sin nombre
	 * @param premio           premio como lo vio el cliente en la ruleta
	 * @param mensajePremio    explicacion de en que consiste el premio, para los que
	 *                         por su nombre no lo dicen. Viene de
	 *                         ruleta_oferta.observacion y solo se llena para esos;
	 *                         si viene vacio no se muestra nada
	 * @param codigo           codigo promocional generado
	 * @param fechaVencimiento fecha de vencimiento en formato aaaa-mm-dd
	 * @param urlLogo          URL publica del logo. Si viene vacia se usa el
	 *                         nombre en texto, que es como se ve cuando el cliente
	 *                         de correo bloquea las imagenes
	 * @return el HTML del correo
	 */
	public static String cuerpo(final String nombreCliente, final String premio,
			final String mensajePremio, final String codigo, final String fechaVencimiento,
			final String urlLogo) {

		final String saludo = (nombreCliente == null || nombreCliente.trim().length() == 0)
				? "&iexcl;Hola!"
				: "&iexcl;Hola " + escapar(primerNombre(nombreCliente)) + "!";

		final StringBuilder h = new StringBuilder();

		h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">");
		h.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		h.append("<title>").append(asunto()).append("</title></head>");
		h.append("<body style=\"margin:0;padding:0;background:#f4f4f4;\">");

		// Tabla exterior: centra el contenido en los clientes de correo que ignoran margin auto.
		h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ");
		h.append("style=\"background:#f4f4f4;padding:24px 12px;\"><tr><td align=\"center\">");

		h.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" ");
		h.append("style=\"max-width:600px;width:100%;background:#ffffff;border-radius:10px;overflow:hidden;");
		h.append("font-family:Arial,Helvetica,sans-serif;\">");

		// Franja superior con el logo. El estilo de color y tipografia va SOBRE la
		// etiqueta img a proposito: es lo que le da formato al texto alternativo
		// cuando el cliente de correo bloquea la imagen, que es el caso por defecto
		// en Gmail y Outlook con remitentes que no estan en contactos.
		h.append("<tr><td align=\"center\" style=\"background:").append(AZUL).append(";padding:24px 30px;\">");
		if (urlLogo != null && urlLogo.trim().length() > 0) {
			h.append("<img src=\"").append(escapar(urlLogo.trim())).append("\" width=\"140\" ");
			h.append("alt=\"PIZZA AMERICANA\" style=\"display:block;margin:0 auto;border:0;");
			h.append("outline:none;text-decoration:none;height:auto;max-width:140px;");
			h.append("color:#ffffff;font-size:24px;font-weight:bold;letter-spacing:.5px;");
			h.append("font-family:Arial,Helvetica,sans-serif;\">");
		} else {
			h.append("<div style=\"color:#ffffff;font-size:24px;font-weight:bold;letter-spacing:.5px;\">");
			h.append("PIZZA AMERICANA</div>");
		}
		h.append("<div style=\"color:").append(AMARILLO).append(";font-size:13px;margin-top:12px;\">");
		h.append("Gracias por contarnos c&oacute;mo lo hicimos</div>");
		h.append("</td></tr>");

		// Saludo y agradecimiento
		h.append("<tr><td style=\"padding:30px 30px 0 30px;\">");
		h.append("<p style=\"margin:0 0 14px 0;font-size:20px;color:").append(AZUL);
		h.append(";font-weight:bold;\">").append(saludo).append("</p>");
		h.append("<p style=\"margin:0 0 20px 0;font-size:15px;line-height:1.6;color:").append(GRIS).append(";\">");
		h.append("Gracias por contestar nuestra encuesta. Lo que nos escribiste es justo lo que ");
		h.append("necesitamos para hacerlo mejor la pr&oacute;xima vez. Y como agradecimiento, ");
		h.append("<strong>te ganaste un premio</strong>.");
		h.append("</p></td></tr>");

		// El premio
		h.append("<tr><td style=\"padding:0 30px;\">");
		h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ");
		h.append("style=\"background:#fdecec;border-left:4px solid ").append(ROJO);
		h.append(";border-radius:6px;\"><tr><td style=\"padding:18px 20px;\">");
		h.append("<div style=\"font-size:12px;color:").append(GRIS_SUAVE);
		h.append(";text-transform:uppercase;letter-spacing:1px;\">Tu premio</div>");
		h.append("<div style=\"font-size:22px;font-weight:bold;color:").append(ROJO);
		h.append(";margin-top:6px;\">").append(escapar(premio)).append("</div>");
		// Hay premios cuyo nombre no dice que se gano: "Premio Sorpresa" deja al
		// cliente sin saber en que consiste. Para esos se configura el texto en
		// ruleta_oferta.observacion y se muestra aqui. En los demas queda vacio,
		// porque repetir "Pizzeta Gratis" debajo de "Pizzeta Gratis" no aporta.
		//
		// Nunca se muestra el nombre de la oferta interna: al cliente no le dice
		// nada algo como "Encueta Deditos Masa Queso - Madurito".
		if (mensajePremio != null && mensajePremio.trim().length() > 0) {
			h.append("<div style=\"font-size:15px;color:").append(GRIS);
			h.append(";margin-top:8px;line-height:1.5;\">");
			h.append(escapar(mensajePremio.trim())).append("</div>");
		}
		h.append("</td></tr></table></td></tr>");

		// El codigo
		h.append("<tr><td style=\"padding:22px 30px 0 30px;\" align=\"center\">");
		h.append("<div style=\"font-size:13px;color:").append(GRIS).append(";margin-bottom:8px;\">");
		h.append("Presenta este c&oacute;digo al momento de pagar</div>");
		h.append("<div style=\"display:inline-block;background:").append(AMARILLO);
		h.append(";border-radius:8px;padding:14px 28px;font-size:30px;font-weight:bold;");
		h.append("letter-spacing:4px;color:").append(AZUL).append(";font-family:'Courier New',Courier,monospace;\">");
		h.append(escapar(codigo)).append("</div>");
		if (fechaVencimiento != null && fechaVencimiento.trim().length() > 0) {
			h.append("<div style=\"font-size:14px;color:").append(ROJO);
			h.append(";margin-top:10px;font-weight:bold;\">");
			h.append("V&aacute;lido hasta el ").append(escapar(enLetras(fechaVencimiento))).append("</div>");
		}
		h.append("</td></tr>");

		// Como redimirlo
		h.append("<tr><td style=\"padding:26px 30px 0 30px;\">");
		h.append("<div style=\"font-size:15px;font-weight:bold;color:").append(AZUL);
		h.append(";margin-bottom:10px;\">C&oacute;mo lo reclamas</div>");
		h.append("<p style=\"margin:0;font-size:14px;line-height:1.7;color:").append(GRIS).append(";\">");
		h.append("Ll&aacute;manos al <strong>").append(TELEFONO_CONTACTO).append("</strong> o ac&eacute;rcate ");
		h.append("a cualquiera de nuestras sucursales. Cuando te atiendan, di que tienes un ");
		h.append("c&oacute;digo promocional y d&aacute;selo al momento de pagar.");
		h.append("</p></td></tr>");

		// Condiciones
		h.append("<tr><td style=\"padding:22px 30px 0 30px;\">");
		h.append("<div style=\"font-size:13px;font-weight:bold;color:").append(GRIS);
		h.append(";margin-bottom:8px;\">Ten en cuenta</div>");
		h.append("<ul style=\"margin:0;padding-left:20px;font-size:13px;line-height:1.7;color:");
		h.append(GRIS).append(";\">");
		h.append("<li>Aplica para una sola compra.</li>");
		h.append("<li>Si pides a domicilio, ten presente que tenemos zona de cobertura, ");
		h.append("y que en algunas zonas entregamos hasta las 5:00 p.m.</li>");
		h.append("<li>Si tu premio es un descuento, la compra a la que se le aplica puede ser ");
		h.append("de m&aacute;ximo $100.000.</li>");
		h.append("<li>Si es la Pizzeta gratis, no incluye gaseosa; y si la pides a domicilio, ");
		h.append("el costo del domicilio corre por tu cuenta.</li>");
		h.append("</ul></td></tr>");

		// Pie
		h.append("<tr><td style=\"padding:26px 30px 30px 30px;\">");
		h.append("<p style=\"margin:0 0 6px 0;font-size:14px;color:").append(GRIS).append(";\">");
		h.append("Gracias por participar. Nos vemos pronto.</p>");
		h.append("<p style=\"margin:0;font-size:12px;color:").append(GRIS_SUAVE).append(";\">");
		h.append("Recibes este correo porque contestaste nuestra encuesta de servicio y ");
		h.append("jugaste en la ruleta de premios.</p>");
		h.append("</td></tr>");

		h.append("</table></td></tr></table></body></html>");

		return (h.toString());
	}

	/**
	 * Primer nombre, para que el saludo no quede como "Hola JUAN CARLOS PEREZ
	 * GOMEZ". Si el nombre viene en mayusculas sostenidas se pasa a capitalizado,
	 * porque asi vienen muchos de los formularios y grita en el saludo.
	 *
	 * @param nombreCompleto nombre tal como esta guardado
	 * @return el primer nombre presentable
	 */
	private static String primerNombre(final String nombreCompleto) {
		final String limpio = nombreCompleto.trim().replaceAll("\\s+", " ");
		final int espacio = limpio.indexOf(' ');
		final String primero = (espacio > 0) ? limpio.substring(0, espacio) : limpio;
		if (primero.length() == 0) {
			return (primero);
		}
		return (primero.substring(0, 1).toUpperCase() + primero.substring(1).toLowerCase());
	}

	/**
	 * Pasa aaaa-mm-dd a algo legible como "15 de septiembre de 2026". Si el formato
	 * no es el esperado se devuelve tal cual, que es mejor que no mostrar nada.
	 *
	 * @param fecha fecha en formato aaaa-mm-dd
	 * @return la fecha en letras
	 */
	private static String enLetras(final String fecha) {
		final String[] meses = { "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
				"agosto", "septiembre", "octubre", "noviembre", "diciembre" };
		try {
			final String[] partes = fecha.trim().split("-");
			if (partes.length != 3) {
				return (fecha);
			}
			final int mes = Integer.parseInt(partes[1]);
			if (mes < 1 || mes > 12) {
				return (fecha);
			}
			return (Integer.parseInt(partes[2]) + " de " + meses[mes - 1] + " de " + partes[0]);
		} catch (final Exception e) {
			return (fecha);
		}
	}

	/**
	 * Escapa lo que romperia el HTML. El nombre y el premio vienen de datos que
	 * escribio una persona, asi que no se pueden pegar crudos en el correo.
	 *
	 * @param texto texto a escapar
	 * @return el texto seguro para HTML
	 */
	private static String escapar(final String texto) {
		if (texto == null) {
			return ("");
		}
		return (texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;"));
	}
}

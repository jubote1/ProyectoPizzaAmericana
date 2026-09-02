package utilidadesCC;

/**
 * Armado del correo con el link de pago que recibe el cliente cuando escoge Pago
 * Virtual.
 *
 * El correo anterior tenia un problema que no se notaba: el unico enlace era la
 * imagen de medios de pago. Como Gmail y Outlook bloquean imagenes por defecto
 * cuando el remitente no esta en contactos, el cliente recibia un saludo y nada
 * en que hacer clic. Aqui el enlace es un boton de texto y la URL aparece escrita
 * debajo, asi que hay como pagar incluso sin una sola imagen.
 *
 * Decisiones de armado:
 *
 *  1. El boton es una tabla con fondo, no un div ni un CSS de hoja aparte: es la
 *     unica forma de que Outlook lo pinte como boton.
 *  2. La URL va tambien en texto plano debajo. Un cliente de correo que rompa el
 *     boton, o alguien que quiera pasarla a otro dispositivo, la necesita visible.
 *  3. El logo y la imagen de medios de pago se referencian por URL publica y
 *     nunca en base64, que Gmail descarta. Las dos son opcionales: si no cargan,
 *     el correo sigue completo y con el boton funcionando.
 *  4. Todo con tablas y estilos en linea, que es lo unico que respetan los
 *     clientes de correo.
 *
 * @author Pizza Americana
 */
public class PlantillaCorreoLinkPago {

	/** Azul de marca. */
	private static final String AZUL = "#20287f";
	/** Rojo de marca, para el boton de pagar. */
	private static final String ROJO = "#d0201f";
	/** Amarillo de marca. */
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
	 * @param idPedido numero del pedido
	 * @return el asunto
	 */
	public static String asunto(final int idPedido) {
		return ("Pizza Americana - link de pago de tu pedido #" + idPedido);
	}

	/**
	 * Cuerpo del correo en HTML.
	 *
	 * @param nombreCliente  nombre del cliente. Si viene vacio se saluda sin nombre
	 * @param idPedido       numero del pedido
	 * @param linkPago       URL de pago de Wompi
	 * @param mensajeIntro   texto de presentacion. Viene de forma_pago.mensaje_correo
	 *                       para que se pueda cambiar sin desplegar; si llega vacio
	 *                       se usa uno por defecto
	 * @param urlLogo        URL publica del logo. Opcional
	 * @param urlMediosPago  URL publica de la imagen de medios de pago. Opcional
	 * @return el HTML del correo
	 */
	public static String cuerpo(final String nombreCliente, final int idPedido,
			final String linkPago, final String mensajeIntro, final String urlLogo,
			final String urlMediosPago) {

		final String saludo = (nombreCliente == null || nombreCliente.trim().length() == 0)
				? "&iexcl;Hola!"
				: "&iexcl;Hola " + escapar(primerNombre(nombreCliente)) + "!";

		final String enlace = escapar(linkPago == null ? "" : linkPago.trim());

		final StringBuilder h = new StringBuilder();

		h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">");
		h.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		h.append("<title>").append(asunto(idPedido)).append("</title></head>");
		h.append("<body style=\"margin:0;padding:0;background:#f4f4f4;\">");

		h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ");
		h.append("style=\"background:#f4f4f4;padding:24px 12px;\"><tr><td align=\"center\">");

		h.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" ");
		h.append("style=\"max-width:600px;width:100%;background:#ffffff;border-radius:10px;overflow:hidden;");
		h.append("font-family:Arial,Helvetica,sans-serif;\">");

		// Franja superior. El estilo de color y tipografia va sobre la etiqueta img
		// porque es lo que formatea el texto alternativo cuando la imagen no carga.
		h.append("<tr><td align=\"center\" style=\"background:").append(AZUL).append(";padding:24px 30px;\">");
		if (urlLogo != null && urlLogo.trim().length() > 0) {
			h.append("<img src=\"").append(escapar(urlLogo.trim())).append("\" width=\"130\" ");
			h.append("alt=\"PIZZA AMERICANA\" style=\"display:block;margin:0 auto;border:0;");
			h.append("outline:none;text-decoration:none;height:auto;max-width:130px;");
			h.append("color:#ffffff;font-size:22px;font-weight:bold;letter-spacing:.5px;");
			h.append("font-family:Arial,Helvetica,sans-serif;\">");
		} else {
			h.append("<div style=\"color:#ffffff;font-size:22px;font-weight:bold;letter-spacing:.5px;\">");
			h.append("PIZZA AMERICANA</div>");
		}
		h.append("<div style=\"color:").append(AMARILLO).append(";font-size:13px;margin-top:12px;\">");
		h.append("Tu pedido est&aacute; listo para pagar</div>");
		h.append("</td></tr>");

		// Saludo
		h.append("<tr><td style=\"padding:30px 30px 0 30px;\">");
		h.append("<p style=\"margin:0 0 12px 0;font-size:20px;color:").append(AZUL);
		h.append(";font-weight:bold;\">").append(saludo).append("</p>");
		h.append("<p style=\"margin:0 0 6px 0;font-size:15px;line-height:1.6;color:").append(GRIS).append(";\">");
		h.append("Ya tenemos tu pedido <strong>#").append(idPedido).append("</strong>. ");
		// El texto configurable va despues del numero de pedido, que es el dato que
		// el cliente busca primero. Si no hay texto en la base se usa uno propio, en
		// vez de dejar el correo a medias.
		if (mensajeIntro != null && mensajeIntro.trim().length() > 0) {
			h.append(escapar(mensajeIntro.trim()));
		} else {
			h.append("Solo falta el pago para empezar a prepararlo.");
		}
		h.append("</p></td></tr>");

		// El boton. Tabla con fondo, que es lo unico que Outlook pinta como boton.
		h.append("<tr><td align=\"center\" style=\"padding:26px 30px 0 30px;\">");
		h.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\">");
		h.append("<tr><td align=\"center\" style=\"background:").append(ROJO);
		h.append(";border-radius:8px;\">");
		h.append("<a href=\"").append(enlace).append("\" target=\"_blank\" ");
		h.append("style=\"display:inline-block;padding:16px 42px;font-size:18px;font-weight:bold;");
		h.append("color:#ffffff;text-decoration:none;font-family:Arial,Helvetica,sans-serif;\">");
		h.append("PAGAR MI PEDIDO</a>");
		h.append("</td></tr></table></td></tr>");

		// La URL escrita. Es la red de seguridad: si el boton no se pinta o el
		// cliente quiere pasar el link a otro dispositivo, aqui esta.
		h.append("<tr><td align=\"center\" style=\"padding:14px 30px 0 30px;\">");
		h.append("<div style=\"font-size:12px;color:").append(GRIS_SUAVE).append(";margin-bottom:4px;\">");
		h.append("Si el bot&oacute;n no funciona, copia este enlace en tu navegador:</div>");
		h.append("<div style=\"font-size:12px;word-break:break-all;\">");
		h.append("<a href=\"").append(enlace).append("\" target=\"_blank\" style=\"color:").append(AZUL);
		h.append(";\">").append(enlace).append("</a></div>");
		h.append("</td></tr>");

		// Medios de pago
		if (urlMediosPago != null && urlMediosPago.trim().length() > 0) {
			h.append("<tr><td align=\"center\" style=\"padding:24px 30px 0 30px;\">");
			h.append("<div style=\"font-size:12px;color:").append(GRIS_SUAVE);
			h.append(";text-transform:uppercase;letter-spacing:1px;margin-bottom:10px;\">");
			h.append("Medios de pago disponibles</div>");
			h.append("<img src=\"").append(escapar(urlMediosPago.trim())).append("\" ");
			h.append("alt=\"Tarjetas, PSE, Nequi y Daviplata\" ");
			h.append("style=\"display:block;margin:0 auto;border:0;max-width:100%;height:auto;");
			h.append("color:").append(GRIS_SUAVE).append(";font-size:12px;\">");
			h.append("</td></tr>");
		}

		// Ayuda
		h.append("<tr><td style=\"padding:26px 30px 0 30px;\">");
		h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ");
		h.append("style=\"background:#f8f9fa;border-radius:6px;\"><tr><td style=\"padding:16px 18px;\">");
		h.append("<div style=\"font-size:14px;color:").append(GRIS).append(";line-height:1.6;\">");
		h.append("El pago es seguro y lo procesa <strong>Wompi</strong>. ");
		h.append("Si algo no funciona o tienes dudas, ll&aacute;manos al ");
		h.append("<strong>").append(TELEFONO_CONTACTO).append("</strong> y te ayudamos.");
		h.append("</div></td></tr></table></td></tr>");

		// Pie
		h.append("<tr><td style=\"padding:24px 30px 30px 30px;\">");
		h.append("<p style=\"margin:0 0 6px 0;font-size:14px;color:").append(GRIS).append(";\">");
		h.append("Gracias por preferirnos. &iexcl;Buen provecho!</p>");
		h.append("<p style=\"margin:0;font-size:12px;color:").append(GRIS_SUAVE).append(";\">");
		h.append("Recibes este correo porque hiciste un pedido en Pizza Americana y ");
		h.append("elegiste pago virtual.</p>");
		h.append("</td></tr>");

		h.append("</table></td></tr></table></body></html>");

		return (h.toString());
	}

	/**
	 * Primer nombre, para que el saludo no quede como "Hola JUAN CARLOS PEREZ
	 * GOMEZ". Si viene en mayusculas sostenidas se pasa a capitalizado, porque asi
	 * llegan muchos de los formularios y grita en el saludo.
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
	 * Escapa lo que romperia el HTML. El nombre viene de datos que escribio una
	 * persona, asi que no se puede pegar crudo en el correo.
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

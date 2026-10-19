package utilidadesCC;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import capaDAOCC.ConfirmacionBotDAO.Linea;
import capaDAOCC.ConfirmacionBotDAO.Resumen;

/**
 * El correo de confirmacion de un pedido hecho por el bot.
 *
 * Es la copia, en el correo del cliente, de lo que el bot le mostro en la conversacion. Sirve para dos cosas: que el
 * cliente tenga a la mano lo que pidio, y que si el bot se equivoco o el pedido se duplico -que es lo que a veces
 * pasa con la confirmacion del bot- tenga a la vista, bien grande, a donde llamar.
 *
 * Todo en texto y con estilos en linea, para que se vea igual aunque el cliente no cargue una sola imagen. Los
 * acentos van como entidades HTML, y los textos que vienen del pedido pasan por escapar(): asi el correo se ve bien
 * sin depender de la codificacion con que se compile o se envie.
 */
public final class PlantillaCorreoConfirmacionBot {

	private static final String AZUL = "#102F6F";
	private static final String AMARILLO = "#FDC806";
	private static final String ROJO = "#E42528";
	private static final String TINTA = "#1F2430";
	private static final String GRIS = "#6C7482";
	private static final String BORDE = "#E2E6EC";
	private static final String FUENTE = "Segoe UI, Roboto, Helvetica, Arial, sans-serif";

	/** Telefono de la linea de atencion, igual que en las demas plantillas. */
	private static final String TELEFONO = "604 4444553";

	private PlantillaCorreoConfirmacionBot() {
		super();
	}

	public static String asunto(final int idPedido) {
		return (asunto(idPedido, false));
	}

	/**
	 * Cuando el pedido se paga en linea, el asunto tiene que decir que falta algo: un "Confirmacion de tu pedido"
	 * se lee como "ya esta todo" y el cliente no abre el link de pago. Sin tildes: el asunto no va en UTF-8.
	 */
	public static String asunto(final int idPedido, final boolean pagoEnLinea) {
		if (pagoEnLinea) {
			return ("Paga tu pedido #" + idPedido + " para que lo preparemos - Pizza Americana");
		}
		return ("Confirmacion de tu pedido #" + idPedido + " - Pizza Americana");
	}

	/**
	 * @param nombreCliente  el nombre que dio el cliente; si viene vacio se saluda sin nombre
	 * @param direccion      la direccion de entrega que dio el cliente ("" si es para recoger)
	 * @param resumen        el pedido tal como quedo guardado
	 * @param pagoEnLinea    si se paga con el link de pago (Wompi): se le recuerda que el pago es lo que lo pone a andar
	 */
	public static String cuerpo(final String nombreCliente, final String direccion, final Resumen resumen,
			final boolean pagoEnLinea) {
		final String saludo = (nombreCliente == null || nombreCliente.trim().length() == 0)
				? "&iexcl;Hola!"
				: "&iexcl;Hola " + escapar(primerNombre(nombreCliente)) + "!";

		final StringBuilder h = new StringBuilder();
		h.append("<div style=\"margin:0;padding:20px 0;background-color:#F4F6F8;font-family:").append(FUENTE)
				.append(";\">");
		h.append("<table cellpadding='0' cellspacing='0' border='0' align='center' width='100%'")
				.append(" style='max-width:600px;border-collapse:collapse;background-color:#FFFFFF;'>");

		h.append("<tr><td style=\"background-color:").append(AZUL).append(";padding:18px 24px;\">")
				.append("<div style=\"font-size:19px;font-weight:bold;color:#FFFFFF;\">")
				.append("PIZZA <span style=\"color:").append(AMARILLO).append(";\">AMERICANA</span></div>")
				.append("</td></tr>");
		h.append("<tr><td style=\"background-color:").append(AMARILLO)
				.append(";font-size:0;line-height:0;height:6px;\">&nbsp;</td></tr>");

		h.append("<tr><td style=\"padding:26px 24px 6px;\">")
				.append("<div style=\"font-size:13px;color:").append(GRIS)
				.append(";text-transform:uppercase;letter-spacing:.08em;font-weight:bold;\">")
				.append(saludo).append("</div>")
				.append("<div style=\"font-size:21px;font-weight:bold;color:").append(TINTA)
				.append(";line-height:1.35;padding-top:10px;\">")
				.append(pagoEnLinea ? "Recibimos tu pedido por WhatsApp, pero falta tu pago"
						: "Hiciste este pedido por WhatsApp")
				.append("</div>")
				.append("<div style=\"font-size:14.5px;color:").append(TINTA)
				.append(";line-height:1.55;padding-top:10px;\">")
				.append(pagoEnLinea
						? "Este es el resumen de tu pedido <b>#" + resumen.idPedido + "</b>. "
								+ "<b>Todav&iacute;a no lo hemos enviado a preparaci&oacute;n</b>: primero hay que pagarlo."
						: "Esta es la confirmaci&oacute;n de la informaci&oacute;n de tu pedido <b>#" + resumen.idPedido
								+ "</b>. Rev&iacute;sala y verifica que todo est&eacute; bien.")
				.append("</div></td></tr>");

		//El pago en linea: lo primero y lo mas visible del correo. Es lo unico que el cliente tiene que hacer, y si no
		//lo hace el pedido no sale a la tienda: no puede quedar como una nota al pie.
		if (pagoEnLinea) {
			h.append("<tr><td style=\"padding:14px 24px 4px;\">")
					.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;'>");
			h.append("<tr><td align='center' style=\"background-color:").append(ROJO)
					.append(";border-radius:10px 10px 0 0;padding:20px 18px 12px;\">")
					.append("<div style=\"font-size:12px;letter-spacing:.14em;color:#FFFFFF;font-weight:bold;\">")
					.append("&#9888; ATENCI&Oacute;N &#9888;</div>")
					.append("<div style=\"font-size:32px;line-height:1.15;font-weight:bold;color:").append(AMARILLO)
					.append(";padding-top:6px;letter-spacing:.02em;\">PAGA PRIMERO</div>")
					.append("<div style=\"font-size:16px;line-height:1.45;color:#FFFFFF;padding-top:8px;\">")
					.append("Tu pedido <b>NO se env&iacute;a a preparaci&oacute;n</b> hasta que realices el pago.</div>")
					.append("</td></tr>");
			h.append("<tr><td style=\"background-color:#FFF4F4;border-left:2px solid ").append(ROJO)
					.append(";border-right:2px solid ").append(ROJO).append(";padding:14px 18px 8px;\">")
					.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;'>");
			paso(h, 1, "Abre el <b>link de pago que te enviamos por WhatsApp</b> y paga el total de <b>"
					+ pesos(resumen.total) + "</b>.");
			paso(h, 2, "Cuando el pago se confirme, <b>nos llega la notificaci&oacute;n autom&aacute;ticamente</b>: "
					+ "no tienes que avisarnos.");
			paso(h, 3, "Tu pedido pasa <b>de inmediato a preparaci&oacute;n</b> en la tienda.");
			h.append("</table></td></tr>");
			h.append("<tr><td align='center' style=\"background-color:").append(AZUL)
					.append(";border-radius:0 0 10px 10px;padding:14px 18px;font-size:15px;line-height:1.4;")
					.append("font-weight:bold;color:#FFFFFF;\">SIN PAGO NO HAY PEDIDO: <span style=\"color:")
					.append(AMARILLO).append(";\">si no pagas, no lo mandamos a preparar.</span></td></tr>");
			h.append("</table></td></tr>");
		}

		//Los datos del pedido.
		h.append("<tr><td style=\"padding:14px 24px 4px;\">")
				.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;'>");
		fila(h, "Punto de venta", resumen.tienda);
		if (pagoEnLinea) {
			fila(h, "Estado", "<span style=\"color:" + ROJO + ";\">PENDIENTE DE PAGO</span>");
		}
		if (direccion != null && direccion.trim().length() > 0) {
			fila(h, "Direcci&oacute;n de entrega", escapar(direccion.trim()));
		}
		if (resumen.formaPago.length() > 0) {
			fila(h, "Forma de pago", escapar(resumen.formaPago));
		}
		fila(h, "Cu&aacute;ndo", resumen.programado && resumen.horaProgramado.length() > 0
				? "Programado para las " + escapar(resumen.horaProgramado) : "Lo m&aacute;s pronto posible");
		h.append("</table></td></tr>");

		//El detalle.
		h.append("<tr><td style=\"padding:14px 24px 4px;\">")
				.append("<div style=\"font-size:13px;color:").append(GRIS)
				.append(";text-transform:uppercase;letter-spacing:.08em;font-weight:bold;padding-bottom:6px;\">")
				.append("Lo que pediste</div>")
				.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;'>");
		if (resumen.lineas.isEmpty()) {
			h.append("<tr><td style=\"padding:8px 0;font-size:14px;color:").append(GRIS)
					.append(";\">El detalle de tu pedido se est&aacute; terminando de registrar.</td></tr>");
		}
		for (final Linea l : resumen.lineas) {
			h.append("<tr><td style=\"padding:9px 0;border-bottom:1px solid ").append(BORDE)
					.append(";font-size:14px;color:").append(TINTA).append(";line-height:1.4;\">")
					.append("<b>").append(cantidad(l.cantidad)).append(" &times; ").append(escapar(l.producto))
					.append("</b>");
			if (l.especialidades.length() > 0) {
				h.append("<div style=\"font-size:12.5px;color:").append(GRIS).append(";\">")
						.append(escapar(l.especialidades)).append("</div>");
			}
			if (l.adicion.length() > 0) {
				h.append("<div style=\"font-size:12.5px;color:").append(GRIS).append(";\">")
						.append(escapar(l.adicion)).append("</div>");
			}
			h.append("</td><td align='right' style=\"padding:9px 0 9px 12px;border-bottom:1px solid ").append(BORDE)
					.append(";font-size:14px;color:").append(TINTA).append(";white-space:nowrap;vertical-align:top;\">")
					.append(l.valorTotal > 0 ? pesos(l.valorTotal) : "Incluido").append("</td></tr>");
		}
		h.append("<tr><td style=\"padding:12px 0 4px;font-size:16px;font-weight:bold;color:").append(AZUL)
				.append(";\">Total a pagar</td><td align='right' style=\"padding:12px 0 4px;font-size:18px;")
				.append("font-weight:bold;color:").append(AZUL).append(";white-space:nowrap;\">")
				.append(pesos(resumen.total)).append("</td></tr>");
		h.append("</table></td></tr>");

		//A donde llamar si algo no esta bien: lo mas visible del correo.
		h.append("<tr><td align='center' style=\"padding:20px 24px 6px;\">")
				.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;'><tr>")
				.append("<td align='center' style=\"background-color:#FFF9E8;border:2px dashed ").append(AMARILLO)
				.append(";border-radius:8px;padding:18px;\">")
				.append("<div style=\"font-size:14.5px;color:").append(TINTA).append(";line-height:1.5;\">")
				.append(pagoEnLinea
						? "<b>&iquest;Ya pagaste y tu pedido no avanza, algo no coincide o lo recibiste dos veces?</b></div>"
						: "<b>&iquest;Algo no coincide con lo que pediste, o recibiste este pedido dos veces?</b></div>")
				.append("<div style=\"font-size:13.5px;color:").append(GRIS).append(";padding-top:4px;\">")
				.append("Comun&iacute;cate con nosotros a la l&iacute;nea de atenci&oacute;n:</div>")
				.append("<div style=\"font-size:30px;font-weight:bold;color:").append(ROJO)
				.append(";padding-top:10px;letter-spacing:.02em;\">")
				.append("<a href=\"tel:+576044444553\" style=\"color:").append(ROJO).append(";text-decoration:none;\">")
				.append(TELEFONO).append("</a></div>")
				.append("</td></tr></table></td></tr>");

		h.append("<tr><td style=\"padding:18px 24px 22px;border-top:1px solid ").append(BORDE)
				.append(";font-size:12px;color:#8A9199;line-height:1.5;\">")
				.append("Este correo es solo una confirmaci&oacute;n de tu pedido hecho por WhatsApp; no tienes ")
				.append("que responderlo. &iexcl;Gracias por pedir en Pizza Americana!")
				.append("</td></tr>");

		h.append("</table></div>");
		return (h.toString());
	}

	/** Un paso numerado del recuadro de pago. El numero va en un circulo azul para que se lea como una secuencia. */
	private static void paso(final StringBuilder h, final int numero, final String textoHtml) {
		h.append("<tr><td width='34' valign='top' style=\"padding:0 0 10px;\">")
				.append("<div style=\"width:26px;height:26px;line-height:26px;text-align:center;border-radius:13px;")
				.append("background-color:").append(AZUL).append(";color:#FFFFFF;font-weight:bold;font-size:14px;\">")
				.append(numero).append("</div></td>")
				.append("<td valign='top' style=\"padding:3px 0 10px;font-size:14.5px;line-height:1.45;color:").append(TINTA)
				.append(";\">").append(textoHtml).append("</td></tr>");
	}

	private static void fila(final StringBuilder h, final String rotulo, final String valorHtml) {
		h.append("<tr><td style=\"padding:6px 12px 6px 0;font-size:13px;color:").append(GRIS)
				.append(";vertical-align:top;white-space:nowrap;\">").append(rotulo)
				.append("</td><td style=\"padding:6px 0;font-size:14px;color:").append(TINTA)
				.append(";font-weight:bold;\">").append(valorHtml).append("</td></tr>");
	}

	private static String cantidad(final double valor) {
		return valor == Math.floor(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
	}

	private static String pesos(final double valor) {
		final DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
		simbolos.setGroupingSeparator('.');
		return new DecimalFormat("$#,##0", simbolos).format(Math.round(valor));
	}

	private static String primerNombre(final String nombre) {
		final String limpio = nombre.trim();
		final int espacio = limpio.indexOf(' ');
		return (espacio > 0 ? limpio.substring(0, espacio) : limpio);
	}

	/** Escapa HTML y convierte lo que no es ASCII en entidades numericas. */
	private static String escapar(final String valor) {
		if (valor == null) {
			return ("");
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < valor.length(); i++) {
			final char c = valor.charAt(i);
			if (c == '&') {
				sb.append("&amp;");
			} else if (c == '<') {
				sb.append("&lt;");
			} else if (c == '>') {
				sb.append("&gt;");
			} else if (c > 126) {
				sb.append("&#").append((int) c).append(';');
			} else {
				sb.append(c);
			}
		}
		return (sb.toString());
	}
}

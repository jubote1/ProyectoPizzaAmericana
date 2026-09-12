package utilidadesCC;

/**
 * El correo que recibe el cliente cuando su pedido se cancela por no pago.
 *
 * Este correo llega en el momento en que la venta se esta perdiendo, asi que
 * no puede ser solo un aviso. El anterior decia "tu pedido fue cancelado" y
 * mencionaba de pasada, al final de una frase larga, que podia llamar al
 * contact center; el unico elemento visible era una imagen, y Gmail bloquea
 * las imagenes remotas por defecto. En la practica el cliente recibia un correo
 * casi vacio que le decia que no.
 *
 * Aqui lo que se destaca es como recuperarlo: el telefono grande, y la promesa
 * de que el pedido no hay que volver a armarlo. Todo en texto y con estilos en
 * linea, para que se vea igual aunque el cliente no cargue una sola imagen.
 */
public final class PlantillaCorreoPedidoCancelado {

	private static final String AZUL = "#102F6F";
	private static final String AMARILLO = "#FDC806";
	private static final String ROJO = "#E42528";
	private static final String TINTA = "#1F2430";
	private static final String GRIS = "#6C7482";
	private static final String FUENTE = "Segoe UI, Roboto, Helvetica, Arial, sans-serif";

	/** Telefono del contact center. Mismo numero que la plantilla del link. */
	private static final String TELEFONO = "604 4444553";

	private PlantillaCorreoPedidoCancelado() {
		super();
	}

	public static String asunto(final int idPedido) {
		//Sin la palabra "cancelado" en el asunto: lo que se quiere es que lo abra
		//y llame, no que lo archive sin leerlo.
		return ("Tu pedido #" + idPedido + " quedo en espera - lo reactivamos cuando quieras");
	}

	/**
	 * @param nombreCliente nombre del cliente; si viene vacio se saluda sin nombre
	 * @param idPedido      numero del pedido
	 * @param minutos       cuantos minutos se esperó el pago
	 */
	public static String cuerpo(final String nombreCliente, final int idPedido, final int minutos) {
		final String saludo = (nombreCliente == null || nombreCliente.trim().length() == 0)
				? "&iexcl;Hola!"
				: "&iexcl;Hola " + escapar(primerNombre(nombreCliente)) + "!";

		final StringBuilder h = new StringBuilder();
		h.append("<div style=\"margin:0;padding:20px 0;background-color:#F4F6F8;font-family:")
				.append(FUENTE).append(";\">");
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
				.append("No alcanzamos a recibir tu pago y liberamos el pedido #").append(idPedido)
				.append("</div>")
				.append("<div style=\"font-size:14.5px;color:").append(TINTA)
				.append(";line-height:1.55;padding-top:10px;\">")
				.append("Esperamos ").append(minutos).append(" minutos y no nos llego la confirmaci&oacute;n, ")
				.append("as&iacute; que soltamos el pedido para no dejarte esperando sin saber.")
				.append("</div></td></tr>");

		//Lo que se quiere que haga, lo mas visible del correo.
		h.append("<tr><td align='center' style=\"padding:20px 24px 6px;\">")
				.append("<table cellpadding='0' cellspacing='0' border='0' width='100%'")
				.append(" style='border-collapse:collapse;'><tr>")
				.append("<td align='center' style=\"background-color:#FFF9E8;border:2px dashed ")
				.append(AMARILLO).append(";border-radius:8px;padding:20px 18px;\">")
				.append("<div style=\"font-size:14.5px;color:").append(TINTA).append(";\">")
				.append("<b>Tu pedido sigue guardado.</b> No tienes que volver a armarlo:</div>")
				.append("<div style=\"font-size:13px;color:").append(GRIS).append(";padding-top:4px;\">")
				.append("ll&aacute;manos y lo reactivamos en un minuto.</div>")
				.append("<div style=\"font-size:30px;font-weight:bold;color:").append(ROJO)
				.append(";padding-top:12px;letter-spacing:.02em;\">")
				.append("<a href=\"tel:+576044444553\" style=\"color:").append(ROJO)
				.append(";text-decoration:none;\">").append(TELEFONO).append("</a></div>")
				.append("</td></tr></table></td></tr>");

		h.append("<tr><td style=\"padding:16px 24px 4px;font-size:14px;color:").append(TINTA)
				.append(";line-height:1.55;\">")
				.append("Si el problema fue el pago en l&iacute;nea, cu&eacute;ntanos al llamar: ")
				.append("tambi&eacute;n puedes pagar en efectivo o con datáfono al recibir.")
				.append("</td></tr>");

		h.append("<tr><td style=\"padding:18px 24px 22px;border-top:1px solid #E2E6EC;font-size:12px;color:#8A9199;")
				.append("line-height:1.5;\">")
				.append("Si ya pagaste y te lleg&oacute; este correo, ll&aacute;manos al ").append(TELEFONO)
				.append(" y lo revisamos de inmediato.")
				.append("</td></tr>");

		h.append("</table></div>");
		return (h.toString());
	}

	private static String primerNombre(final String nombre) {
		final String limpio = nombre.trim();
		final int espacio = limpio.indexOf(' ');
		return (espacio > 0 ? limpio.substring(0, espacio) : limpio);
	}

	private static String escapar(final String valor) {
		if (valor == null) {
			return ("");
		}
		return (valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
	}
}

package utilidadesCC;

/**
 * Correo que le avisa al cliente que administracion le movio los puntos.
 *
 * Va sobrio, igual que el aviso de vencimiento: sin logo, sin imagenes, sin
 * tablas de maquetacion. El cliente no necesita mirar nada, necesita leer
 * cuantos puntos se le descontaron y cuantos le quedaron. Eso es texto, y un
 * correo de texto pasa mejor los filtros y no depende de que el cliente tenga
 * las imagenes habilitadas.
 *
 * POR QUE SE LE AVISA
 *
 * Porque el cliente no estuvo presente cuando se le movio el saldo. Cuando
 * redime en la tienda lo ve pasar; aca lo hace alguien de administracion, y si
 * el cliente se entera despues mirando su saldo, lo natural es que piense que
 * le quitaron puntos. El correo convierte un descuadre inexplicable en un
 * movimiento con fecha, motivo y saldo.
 *
 * LOS ACENTOS VAN COMO ENTIDADES HTML
 *
 * El cuerpo es HTML, asi que "regularizaci&oacute;n" llega bien escrito al
 * cliente y este archivo sigue siendo ASCII puro. Las demas plantillas de este
 * paquete escriben sin tildes para no depender de como quede codificado el
 * archivo; con entidades no hay que elegir entre las dos cosas.
 *
 * El asunto NO es HTML y ahi las entidades no sirven, asi que se redacta con
 * palabras que no llevan tilde.
 */
public final class PlantillaCorreoRedencionManual {

	private PlantillaCorreoRedencionManual() {
	}

	/**
	 * Asunto del correo.
	 *
	 * Dice lo que paso sin alarmar. "Movimiento" y no "descuento": el cliente
	 * recibio algo a cambio, no le quitaron nada.
	 */
	public static String asunto() {
		return ("Movimiento en tus puntos Pizza Americana");
	}

	/**
	 * Cuerpo del correo.
	 *
	 * @param nombre     nombre del cliente; vacio saluda sin nombre
	 * @param puntos     puntos que se descontaron
	 * @param saldo      puntos que le quedan
	 * @param motivo     por que se hizo; se le muestra al cliente
	 * @param tienda     tienda donde se entrego el producto; puede ir vacia
	 * @param fecha      fecha del movimiento, en aaaa-mm-dd
	 */
	public static String cuerpo(final String nombre, final double puntos, final double saldo,
			final String motivo, final String tienda, final String fecha) {

		final String saludo = (nombre == null || nombre.trim().length() == 0)
				? "Hola,"
				: "Hola " + escapar(nombre.trim()) + ",";

		final StringBuilder html = new StringBuilder();
		html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:15px;");
		html.append("line-height:1.55;color:#222;max-width:560px;\">");

		html.append("<p>").append(saludo).append("</p>");

		html.append("<p>Te contamos que realizamos una <strong>regularizaci&oacute;n manual</strong> ");
		html.append("de tus puntos, como parte de un proceso administrativo.</p>");

		//El bloque con las tres cifras va aparte y con fondo: es lo unico que el
		//cliente de verdad necesita leer, y tiene que encontrarlo sin buscar.
		html.append("<div style=\"background:#f4f6f8;border-left:4px solid #20287f;");
		html.append("padding:12px 16px;margin:16px 0;\">");
		html.append("<p style=\"margin:0 0 6px;\">Puntos redimidos: <strong>")
			.append(formatear(puntos)).append("</strong></p>");
		html.append("<p style=\"margin:0 0 6px;\">Saldo actual: <strong>")
			.append(formatear(saldo)).append("</strong></p>");
		html.append("<p style=\"margin:0;\">Fecha: ").append(escapar(fecha)).append("</p>");
		html.append("</div>");

		if (motivo != null && motivo.trim().length() > 0) {
			html.append("<p>Motivo: ").append(escapar(motivo.trim()));
			if (tienda != null && tienda.trim().length() > 0) {
				html.append(" (").append(escapar(tienda.trim())).append(")");
			}
			html.append(".</p>");
		}

		html.append("<p>Si algo de esto no te cuadra, responde este correo o escr&iacute;benos ");
		html.append("y lo revisamos contigo.</p>");

		html.append("<p style=\"margin-top:22px;\">Gracias por seguir con nosotros,<br>");
		html.append("<strong>Pizza Americana</strong></p>");

		html.append("<p style=\"font-size:12px;color:#777;margin-top:20px;\">");
		html.append("Este es un mensaje autom&aacute;tico sobre tu cuenta de puntos. ");
		html.append("No es publicidad.</p>");

		html.append("</div>");
		return (html.toString());
	}

	/** Los puntos se leen mejor sin decimales cuando son enteros. */
	private static String formatear(final double puntos) {
		if (puntos == Math.floor(puntos) && !Double.isInfinite(puntos)) {
			return (new java.text.DecimalFormat("###,###").format(puntos));
		}
		return (new java.text.DecimalFormat("###,###.##").format(puntos));
	}

	/**
	 * El motivo y el nombre los escribe una persona, y pueden traer un menor que
	 * o un ampersand que romperian el HTML del correo.
	 */
	private static String escapar(final String texto) {
		if (texto == null) {
			return ("");
		}
		return (texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
	}
}

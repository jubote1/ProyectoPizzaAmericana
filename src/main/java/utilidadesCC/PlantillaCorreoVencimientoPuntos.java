package utilidadesCC;

/**
 * Correo que le avisa al cliente que se le vencen puntos.
 *
 * A diferencia de los correos del link de pago y de los premios de la ruleta,
 * este va deliberadamente sobrio: sin logo, sin imagenes, sin tablas de
 * maquetacion. Son tres razones.
 *
 * La primera es de entregabilidad. Este correo sale a cientos de personas la
 * misma noche, y un correo liviano y con mucho texto propio pasa mejor los
 * filtros que uno que es basicamente una imagen. Los correos pesados en
 * imagenes son justamente el patron que Gmail asocia con publicidad masiva.
 *
 * La segunda es que el cliente no necesita mirar nada: necesita leer cuantos
 * puntos tiene y hasta cuando. Eso es texto.
 *
 * La tercera es practica: sin imagenes no hay que alojar nada, ni preocuparse
 * porque el cliente tenga las imagenes bloqueadas, que es el comportamiento por
 * defecto de Gmail y de Outlook.
 */
public class PlantillaCorreoVencimientoPuntos {

	private PlantillaCorreoVencimientoPuntos() {
	}

	/**
	 * Asunto del correo. Lleva los puntos y los dias, porque muchos clientes
	 * deciden si abren solo con el asunto.
	 */
	public static String asunto(double puntos, int dias) {
		return ("Tienes " + formatear(puntos) + " puntos que vencen en " + dias
				+ (dias == 1 ? " dia" : " dias"));
	}

	/**
	 * Cuerpo del correo.
	 *
	 * @param nombre      nombre del cliente; si viene vacio se saluda sin nombre
	 * @param puntos      puntos que se le vencen
	 * @param fechaVence  fecha del vencimiento mas proximo, en aaaa-mm-dd
	 * @param dias        dias que faltan, para el texto
	 * @param esUltimo    true para el recordatorio final, que cambia el tono
	 */
	public static String cuerpo(String nombre, double puntos, String fechaVence, int dias, boolean esUltimo) {

		final String saludo = (nombre == null || nombre.trim().length() == 0)
				? "Hola,"
				: "Hola " + escapar(nombre.trim()) + ",";

		final String entrada = esUltimo
				? "Te escribimos porque este es el ultimo aviso: tus puntos estan a punto de vencerse."
				: "Queremos avisarte con tiempo para que no pierdas lo que ya te ganaste.";

		final StringBuilder html = new StringBuilder();
		html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:15px;");
		html.append("line-height:1.55;color:#222;max-width:560px;\">");

		html.append("<p>").append(saludo).append("</p>");
		html.append("<p>").append(entrada).append("</p>");

		// El dato importante, destacado solo con texto y un borde. Sin imagenes.
		html.append("<p style=\"font-size:20px;font-weight:bold;color:#d0201f;margin:18px 0 4px 0;\">");
		html.append(formatear(puntos)).append(" puntos");
		html.append("</p>");
		html.append("<p style=\"margin:0 0 18px 0;color:#555;\">");
		html.append("se vencen el <strong>").append(formatearFecha(fechaVence)).append("</strong>");
		html.append(" (faltan ").append(dias).append(dias == 1 ? " dia" : " dias").append(").");
		html.append("</p>");

		html.append("<p>Para usarlos solo tienes que pedir en cualquiera de nuestros puntos de venta");
		html.append(" o por el call center, y decir que quieres redimir tus puntos.</p>");

		html.append("<p style=\"color:#666;font-size:13px;margin-top:24px;\">");
		html.append("Si ya los redimiste en los ultimos dias, no tengas en cuenta este mensaje.");
		html.append("</p>");

		html.append("<p style=\"color:#888;font-size:12px;margin-top:20px;");
		html.append("border-top:1px solid #ddd;padding-top:12px;\">");
		html.append("Pizza Americana &middot; Plan de fidelizacion");
		html.append("</p>");

		html.append("</div>");
		return (html.toString());
	}

	/** Los puntos se muestran sin decimales cuando son enteros. */
	private static String formatear(double puntos) {
		if (puntos == Math.rint(puntos)) {
			return (String.valueOf((long) puntos));
		}
		return (String.format("%.2f", puntos));
	}

	/**
	 * Pasa la fecha de aaaa-mm-dd a algo legible. Si no viene en ese formato se
	 * devuelve tal cual, que es preferible a mostrar un error.
	 */
	private static String formatearFecha(String fecha) {
		if (fecha == null || fecha.length() < 10) {
			return (fecha == null ? "" : fecha);
		}
		final String[] meses = { "", "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
				"agosto", "septiembre", "octubre", "noviembre", "diciembre" };
		try {
			final int anio = Integer.parseInt(fecha.substring(0, 4));
			final int mes = Integer.parseInt(fecha.substring(5, 7));
			final int dia = Integer.parseInt(fecha.substring(8, 10));
			if (mes < 1 || mes > 12) {
				return (fecha);
			}
			return (dia + " de " + meses[mes] + " de " + anio);
		} catch (final Exception e) {
			return (fecha);
		}
	}

	/** El nombre del cliente es texto de captura y se pinta dentro del HTML. */
	private static String escapar(String texto) {
		return (texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;"));
	}
}

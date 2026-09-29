package utilidadesCC;

/**
 * Correo que le confirma al cliente que redimio puntos.
 *
 * Sirve para los dos caminos y por eso hay un solo archivo: la redencion normal
 * -la que hace la tienda o el contact center cuando el cliente esta ahi- y la
 * manual -la que hace administracion cuando la tienda no pudo-. Cambian el
 * saludo y una frase; el resto es lo mismo, y tener dos plantillas casi iguales
 * termina siempre en que se corrige una y se olvida la otra.
 *
 * QUE LLEVA, Y POR QUE ESAS TRES COSAS
 *
 *   Puntos redimidos   lo que acaba de pasar.
 *   Saldo actual       lo que le queda, que es lo primero que la gente pregunta.
 *   Proximo vencimiento  porque un punto que nadie usa se pierde. Decirle al
 *                      cliente cuando se le vence lo siguiente es lo que hace
 *                      que vuelva antes de esa fecha.
 *
 * Va sobrio: sin logo, sin imagenes, sin tablas de maquetacion. El cliente no
 * necesita mirar nada, necesita leer tres numeros. Un correo de texto ademas
 * pasa mejor los filtros y no depende de que tenga las imagenes habilitadas,
 * que es el comportamiento por defecto de Gmail y de Outlook.
 *
 * LOS ACENTOS VAN COMO ENTIDADES HTML
 *
 * El cuerpo es HTML, asi que "redenci&oacute;n" llega bien escrito y este
 * archivo sigue siendo ASCII puro. Las demas plantillas de este paquete
 * escriben sin tildes para no depender de como quede codificado el archivo; con
 * entidades no hay que elegir entre las dos cosas.
 *
 * El asunto NO es HTML y ahi las entidades no sirven, asi que se redacta con
 * palabras que no llevan tilde.
 */
public final class PlantillaCorreoRedencion {

	/** Azul de marca, el mismo de los demas correos. */
	private static final String AZUL = "#20287f";

	private PlantillaCorreoRedencion() {
	}

	/**
	 * Todo lo que el correo necesita.
	 *
	 * Va como objeto y no como ocho parametros sueltos a proposito: puntos y
	 * saldo son los dos double seguidos, y cambiarlos de orden por equivocacion
	 * le mandaria al cliente un correo que dice lo contrario de lo que paso, sin
	 * que el compilador avise.
	 */
	public static class Datos {
		/** Nombre del cliente. Vacio saluda sin nombre. */
		public String nombre = "";

		/** Puntos que se acaban de redimir. */
		public double puntos;

		/** Puntos que le quedan despues de la redencion. */
		public double saldo;

		/** Tienda donde se redimio. Puede ir vacia. */
		public String tienda = "";

		/** Fecha del movimiento, en aaaa-mm-dd. */
		public String fecha = "";

		/** Fecha del proximo vencimiento, en aaaa-mm-dd. Vacia si no hay. */
		public String proximoVencimiento = "";

		/** Puntos que se vencen en esa fecha. */
		public double puntosQueVencen;

		/** true cuando la hizo administracion y no la tienda. */
		public boolean manual;

		/** Por que se hizo. Solo se muestra en las manuales. */
		public String motivo = "";
	}

	/** El asunto. En la manual se aclara, porque el cliente no estuvo presente. */
	public static String asunto(final boolean manual) {
		return (manual
				? "Movimiento en tus puntos Pizza Americana"
				: "Redimiste tus puntos Pizza Americana");
	}

	/** El cuerpo del correo. */
	public static String cuerpo(final Datos d) {

		final String saludo = (d.nombre == null || d.nombre.trim().length() == 0)
				? "Hola,"
				: "Hola " + escapar(d.nombre.trim()) + ",";

		final StringBuilder html = new StringBuilder();
		html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:15px;");
		html.append("line-height:1.55;color:#222;max-width:560px;\">");

		html.append("<p>").append(saludo).append("</p>");

		if (d.manual) {
			html.append("<p>Te contamos que realizamos una <strong>regularizaci&oacute;n manual</strong> ");
			html.append("de tus puntos, como parte de un proceso administrativo.</p>");
		} else {
			html.append("<p>Acabas de redimir tus puntos");
			if (d.tienda != null && d.tienda.trim().length() > 0) {
				html.append(" en <strong>").append(escapar(d.tienda.trim())).append("</strong>");
			}
			html.append(". Gracias por aprovecharlos.</p>");
		}

		//El bloque con las cifras va aparte y con fondo: es lo unico que el
		//cliente de verdad necesita leer, y tiene que encontrarlo sin buscar.
		html.append("<div style=\"background:#f4f6f8;border-left:4px solid ").append(AZUL);
		html.append(";padding:12px 16px;margin:16px 0;\">");
		html.append("<p style=\"margin:0 0 6px;\">Puntos redimidos: <strong>")
			.append(formatear(d.puntos)).append("</strong></p>");
		html.append("<p style=\"margin:0 0 6px;\">Saldo actual: <strong>")
			.append(formatear(d.saldo)).append("</strong></p>");
		html.append("<p style=\"margin:0;\">Fecha: ").append(escapar(d.fecha)).append("</p>");
		html.append("</div>");

		html.append(bloqueVencimiento(d));

		if (d.manual && d.motivo != null && d.motivo.trim().length() > 0) {
			html.append("<p>Motivo: ").append(escapar(d.motivo.trim()));
			if (d.tienda != null && d.tienda.trim().length() > 0) {
				html.append(" (").append(escapar(d.tienda.trim())).append(")");
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

	/**
	 * La linea del proximo vencimiento.
	 *
	 * Si no se pudo averiguar, NO se escribe nada. Poner "no tienes puntos por
	 * vencer" cuando lo que paso fue que la consulta fallo seria decirle al
	 * cliente algo que no se sabe, y el se confiaria.
	 *
	 * Si el saldo quedo en cero tampoco se escribe: no hay nada que vencer y la
	 * frase sobraria.
	 */
	private static String bloqueVencimiento(final Datos d) {
		if (d.saldo <= 0) {
			return ("");
		}
		if (d.proximoVencimiento == null || d.proximoVencimiento.trim().length() == 0) {
			return ("");
		}
		final StringBuilder html = new StringBuilder();
		html.append("<p>");
		if (d.puntosQueVencen > 0) {
			html.append("De tu saldo, <strong>").append(formatear(d.puntosQueVencen));
			html.append("</strong> puntos se vencen el <strong>");
		} else {
			html.append("Tus puntos m&aacute;s pr&oacute;ximos a vencer lo hacen el <strong>");
		}
		html.append(enLetras(d.proximoVencimiento.trim())).append("</strong>. ");
		html.append("Te avisamos para que no se te pasen.</p>");
		return (html.toString());
	}

	/**
	 * 2027-03-14 se lee "14 de marzo de 2027".
	 *
	 * Una fecha en formato de base de datos dentro de un correo a un cliente se
	 * lee como un error del sistema. Si no se entiende, se deja como vino antes
	 * que inventar una.
	 */
	private static String enLetras(final String iso) {
		final String[] meses = { "", "enero", "febrero", "marzo", "abril", "mayo", "junio",
				"julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre" };
		if (iso == null || !iso.matches("^\\d{4}-\\d{2}-\\d{2}.*$")) {
			return (escapar(iso));
		}
		try {
			final int anio = Integer.parseInt(iso.substring(0, 4));
			final int mes = Integer.parseInt(iso.substring(5, 7));
			final int dia = Integer.parseInt(iso.substring(8, 10));
			if (mes < 1 || mes > 12) {
				return (escapar(iso));
			}
			return (dia + " de " + meses[mes] + " de " + anio);
		} catch (final Exception e) {
			return (escapar(iso));
		}
	}

	/** Los puntos se leen mejor sin decimales cuando son enteros. */
	private static String formatear(final double puntos) {
		if (puntos == Math.floor(puntos) && !Double.isInfinite(puntos)) {
			return (new java.text.DecimalFormat("###,###").format(puntos));
		}
		return (new java.text.DecimalFormat("###,###.##").format(puntos));
	}

	/**
	 * El motivo, el nombre y la tienda los escribe una persona, y pueden traer
	 * un menor que o un ampersand que romperian el HTML del correo.
	 */
	private static String escapar(final String texto) {
		if (texto == null) {
			return ("");
		}
		return (texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
	}
}

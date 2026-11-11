package utilidadesCC;

/**
 * Correo que le avisa al cliente que se le adicionaron puntos de forma manual.
 *
 * Lleva lo que el cliente quiere saber: cuantos puntos se le sumaron, POR QUE (el motivo que escribio
 * quien los adiciono), cuantos tiene ahora y hasta cuando valen. Va sobrio, en texto, como el correo
 * de redencion. Los acentos van como entidades HTML para que este archivo siga siendo ASCII puro; el
 * asunto no es HTML y se redacta sin tildes.
 */
public final class PlantillaCorreoAdicionPuntos {

	private static final String AZUL = "#20287f";

	private PlantillaCorreoAdicionPuntos() {
	}

	public static class Datos {
		public String nombre = "";
		public double puntos;
		public double saldo;
		public String motivo = "";
		/** Fecha en que vencen los puntos adicionados, aaaa-mm-dd. */
		public String fechaVencimiento = "";
		public String fecha = "";
	}

	public static String asunto() {
		return ("Te sumamos puntos a tu cuenta Pizza Americana");
	}

	public static String cuerpo(final Datos d) {
		final String saludo = (d.nombre == null || d.nombre.trim().length() == 0)
				? "Hola,"
				: "Hola " + escapar(d.nombre.trim()) + ",";

		final StringBuilder html = new StringBuilder();
		html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:15px;");
		html.append("line-height:1.55;color:#222;max-width:560px;\">");
		html.append("<p>").append(saludo).append("</p>");
		html.append("<p>Te contamos que te <strong>sumamos <span style=\"color:").append(AZUL).append(";\">")
				.append(formatear(d.puntos)).append(" puntos</span></strong> a tu cuenta del Club Pizza Americana.</p>");

		if (d.motivo != null && d.motivo.trim().length() > 0) {
			html.append("<p><strong>Motivo:</strong> ").append(escapar(d.motivo.trim())).append("</p>");
		}

		html.append("<div style=\"background:#f4f6f8;border-left:4px solid ").append(AZUL);
		html.append(";padding:12px 16px;margin:16px 0;\">");
		html.append("<p style=\"margin:0 0 6px;\">Puntos sumados: <strong>").append(formatear(d.puntos)).append("</strong></p>");
		html.append("<p style=\"margin:0 0 6px;\">Saldo actual: <strong>").append(formatear(d.saldo)).append("</strong></p>");
		if (d.fechaVencimiento != null && d.fechaVencimiento.length() > 0) {
			html.append("<p style=\"margin:0 0 6px;\">Estos puntos los puedes usar hasta el <strong>")
					.append(escapar(d.fechaVencimiento)).append("</strong>.</p>");
		}
		html.append("<p style=\"margin:0;\">Fecha: ").append(escapar(d.fecha)).append("</p>");
		html.append("</div>");

		html.append("<p>Puedes redimirlos en cualquiera de nuestras tiendas. Si algo de esto no te cuadra, ");
		html.append("responde este correo o escr&iacute;benos y lo revisamos contigo.</p>");
		html.append("<p style=\"margin-top:22px;\">Gracias por seguir con nosotros,<br>");
		html.append("<strong>Pizza Americana</strong></p>");
		html.append("<p style=\"font-size:12px;color:#777;margin-top:20px;\">");
		html.append("Este es un mensaje autom&aacute;tico sobre tu cuenta de puntos. No es publicidad.</p>");
		html.append("</div>");
		return (html.toString());
	}

	private static String formatear(final double puntos) {
		if (puntos == Math.floor(puntos) && !Double.isInfinite(puntos)) {
			return (new java.text.DecimalFormat("###,###").format(puntos));
		}
		return (new java.text.DecimalFormat("###,###.##").format(puntos));
	}

	private static String escapar(final String t) {
		if (t == null) {
			return ("");
		}
		return (t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"));
	}
}

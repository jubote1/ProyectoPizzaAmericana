package utilidadesCC;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import capaDAOCC.OfertaClienteDAO.DatosCorreoOferta;

/**
 * Arma el correo con el que se le avisa al cliente que tiene una oferta.
 *
 * Este correo no existia. Al asignar una oferta se le generaba un codigo al
 * cliente y el cliente no se enteraba: dependia de que alguien lo llamara. La
 * llamada que notificaba quedo comentada el 2026-09-01 -era un mensaje de texto-
 * con la nota de que el aviso pasaba a ser por correo, pero el correo nunca se
 * escribio.
 *
 * Va con tablas y estilos en linea a proposito: Outlook no entiende CSS moderno.
 * Y va sin imagenes: Outlook y Gmail las bloquean por defecto, asi que un correo
 * hecho de imagenes le llega en blanco a medio mundo. El logo es texto y los
 * colores son fondos de celda.
 *
 * Todo el contenido sale de la definicion de la oferta. No hay nada que escribir
 * a mano en cada envio.
 */
public class CorreoOferta {

	private static final String AZUL = "#102F6F";
	private static final String AMARILLO = "#FDC806";
	private static final String AMARILLO_FONDO = "#FFF7DC";
	private static final String AMARILLO_BORDE = "#E0B400";
	private static final String AMARILLO_TEXTO = "#7A5A00";
	private static final String ROJO = "#E42528";
	private static final String TINTA = "#14181F";
	private static final String TINTA_2 = "#6E7784";
	private static final String LINEA = "#EDEFF3";
	private static final String FUENTE = "Arial,Helvetica,sans-serif";
	private static final String SITIO = "pizzaamericana.co";

	private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
		"julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

	/** Los miles con punto, sin depender de la configuracion regional del servidor. */
	private static DecimalFormat formatoMiles() {
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "CO"));
		simbolos.setGroupingSeparator('.');
		return (new DecimalFormat("###,###", simbolos));
	}

	/** El texto no viene de nosotros sino de la base: se escapa antes de meterlo al HTML. */
	private static String escapar(String valor) {
		if (valor == null) {
			return ("");
		}
		return (valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;"));
	}

	/** Solo el primer nombre, que es como se le habla a una persona. */
	private static String primerNombre(String nombreCompleto) {
		if (nombreCompleto == null) {
			return ("");
		}
		String limpio = nombreCompleto.trim();
		if (limpio.length() == 0) {
			return ("");
		}
		int espacio = limpio.indexOf(' ');
		String primero = (espacio > 0) ? limpio.substring(0, espacio) : limpio;
		//Vienen en mayuscula sostenida con frecuencia; se deja con inicial mayuscula.
		return (primero.substring(0, 1).toUpperCase() + primero.substring(1).toLowerCase());
	}

	/** De aaaa-mm-dd a "24 de septiembre de 2026". Si no se puede, se devuelve tal cual. */
	private static String fechaEnPalabras(String fechaIso) {
		if (fechaIso == null || fechaIso.length() < 10) {
			return ("");
		}
		try {
			Date fecha = new SimpleDateFormat("yyyy-MM-dd").parse(fechaIso.substring(0, 10));
			SimpleDateFormat dia = new SimpleDateFormat("d");
			SimpleDateFormat mes = new SimpleDateFormat("M");
			SimpleDateFormat anio = new SimpleDateFormat("yyyy");
			int indiceMes = Integer.parseInt(mes.format(fecha)) - 1;
			if (indiceMes < 0 || indiceMes > 11) {
				return (fechaIso);
			}
			return (dia.format(fecha) + " de " + CorreoOferta.MESES[indiceMes] + " de " + anio.format(fecha));
		} catch (Exception e) {
			return (fechaIso);
		}
	}



	/** La franja de una hora en 24: manana, tarde o noche. */
	private static String franjaDe(int hora24) {
		if (hora24 < 12) {
			return ("de la ma&ntilde;ana");
		}
		if (hora24 < 19) {
			return ("de la tarde");
		}
		return ("de la noche");
	}

	/**
	 * El rango de horas en palabras. Cuando las dos horas caen en la misma franja
	 * se nombra una sola vez: "de 2:00 a 5:00 de la tarde", no "de 2:00 de la tarde
	 * a 5:00 de la tarde".
	 */
	private static String rangoHoras(String desde24, String hasta24) {
		int desde;
		int hasta;
		try {
			desde = Integer.parseInt(desde24.trim());
			hasta = Integer.parseInt(hasta24.trim());
		} catch (Exception e) {
			return ("de " + desde24 + " a " + hasta24);
		}
		if (desde < 0 || desde > 23 || hasta < 0 || hasta > 23) {
			return ("de " + desde24 + " a " + hasta24);
		}
		String franjaDesde = CorreoOferta.franjaDe(desde);
		String franjaHasta = CorreoOferta.franjaDe(hasta);
		if (franjaDesde.equals(franjaHasta)) {
			return ("de " + CorreoOferta.soloHora(desde) + " a " + CorreoOferta.soloHora(hasta) + " "
					+ franjaHasta);
		}
		return ("de " + CorreoOferta.soloHora(desde) + " " + franjaDesde + " a "
				+ CorreoOferta.soloHora(hasta) + " " + franjaHasta);
	}

	/** La hora en formato de doce, sin la franja. */
	private static String soloHora(int hora24) {
		int docehoras = hora24 % 12;
		if (docehoras == 0) {
			docehoras = 12;
		}
		return (docehoras + ":00");
	}

	/** El beneficio en una frase corta: sirve para el asunto y para el titular. */
	private static String beneficio(DatosCorreoOferta datos) {
		if (datos.descuentoValor > 0) {
			return ("$" + CorreoOferta.formatoMiles().format(datos.descuentoValor));
		}
		if (datos.descuentoPorcentaje > 0) {
			return (datos.descuentoPorcentaje + "% de descuento");
		}
		return ("");
	}

	/**
	 * El asunto. Sin tildes a proposito: ControladorEnvioCorreo llama a setSubject sin
	 * charset, asi que el asunto se codifica con la del sistema y una tilde llegaria
	 * partida en varios clientes. El cuerpo si va en utf-8 y ahi si se usan.
	 *
	 * Con el nombre del cliente y el beneficio adelante se abre mucho
	 * mas que con un titulo en mayuscula sostenida, que es justo la forma que los
	 * filtros de correo marcan como publicidad.
	 */
	public static String armarAsunto(DatosCorreoOferta datos) {
		String nombre = CorreoOferta.primerNombre(datos.nombreCliente);
		String ben = CorreoOferta.beneficio(datos);
		StringBuilder asunto = new StringBuilder();
		if (nombre.length() > 0) {
			asunto.append(nombre).append(", ");
		}
		if (ben.length() > 0) {
			asunto.append("tienes ").append(ben).append(" para tu siguiente pedido");
		} else {
			asunto.append("tenemos una oferta para ti");
		}
		//La primera letra en mayuscula cuando no hay nombre.
		String texto = asunto.toString();
		return (texto.substring(0, 1).toUpperCase() + texto.substring(1));
	}

	/** Una fila de la tabla de condiciones. */
	private static String condicion(String rotulo, String valor, boolean fuerte, boolean ultima) {
		String borde = ultima ? "" : ("border-bottom:1px solid " + CorreoOferta.LINEA + ";");
		return ("<tr>"
				+ "<td width='36%' style='padding:9px 0;" + borde + "color:" + CorreoOferta.TINTA_2 + ";'>"
				+ rotulo + "</td>"
				+ "<td style='padding:9px 0;" + borde + (fuerte ? "font-weight:bold;" : "") + "'>"
				+ valor + "</td></tr>");
	}

	/**
	 * El cuerpo del correo.
	 *
	 * @param datos lo que devolvio OfertaClienteDAO.obtenerDatosCorreoOferta
	 */
	public static String armarCuerpo(DatosCorreoOferta datos) {
		String nombre = CorreoOferta.primerNombre(datos.nombreCliente);
		String ben = CorreoOferta.beneficio(datos);
		boolean tieneCodigo = (datos.codigoPromocion != null && datos.codigoPromocion.trim().length() > 0);
		boolean controlaHora = "S".equals(datos.controlaHora);
		boolean dejaSaldo = "S".equals(datos.redencionParcial) && datos.descuentoValor > 0;
		String fechaVence = CorreoOferta.fechaEnPalabras(datos.fechaCaducidad);

		StringBuilder m = new StringBuilder();
		m.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='max-width:600px;")
				.append("border-collapse:collapse;font-family:").append(CorreoOferta.FUENTE)
				.append(";color:").append(CorreoOferta.TINTA).append(";'>");

		//Encabezado de marca.
		m.append("<tr><td align='center' style='background-color:").append(CorreoOferta.AZUL)
				.append(";padding:18px 22px;'>")
				.append("<div style='font-size:19px;font-weight:bold;color:#FFFFFF;letter-spacing:.02em;'>")
				.append("PIZZA <span style='color:").append(CorreoOferta.AMARILLO).append(";'>AMERICANA</span>")
				.append("</div></td></tr>");
		m.append("<tr><td style='background-color:").append(CorreoOferta.AMARILLO)
				.append(";font-size:0;line-height:0;height:6px;'>&nbsp;</td></tr>");

		//Titular: el mensaje 1 de la oferta, con el beneficio destacado.
		m.append("<tr><td align='center' style='padding:26px 26px 4px;'>");
		if (nombre.length() > 0) {
			m.append("<div style='font-size:13px;color:").append(CorreoOferta.TINTA_2)
					.append(";letter-spacing:.1em;text-transform:uppercase;font-weight:bold;'>Hola ")
					.append(CorreoOferta.escapar(nombre)).append("</div>");
		}
		m.append("<div style='font-size:22px;font-weight:bold;color:").append(CorreoOferta.TINTA)
				.append(";line-height:1.3;padding-top:10px;'>");
		if (datos.mensaje1 != null && datos.mensaje1.trim().length() > 0) {
			m.append(CorreoOferta.escapar(datos.mensaje1.trim()));
		} else if (ben.length() > 0) {
			m.append("Tienes <span style='color:").append(CorreoOferta.ROJO).append(";'>").append(ben)
					.append("</span> para tu pr&oacute;ximo pedido.");
		} else {
			m.append("Tenemos algo para ti en tu pr&oacute;ximo pedido.");
		}
		m.append("</div>");
		//Si el mensaje de la oferta no menciona el valor, se pone aparte para que no se pierda.
		if (ben.length() > 0 && datos.mensaje1 != null && !datos.mensaje1.contains(ben)) {
			m.append("<div style='font-size:30px;font-weight:bold;color:").append(CorreoOferta.ROJO)
					.append(";padding-top:8px;'>").append(ben).append("</div>");
		}
		m.append("</td></tr>");

		//El codigo. Solo si la oferta genera uno.
		if (tieneCodigo) {
			m.append("<tr><td align='center' style='padding:22px 26px 6px;'>")
					.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;'><tr>")
					.append("<td align='center' style='background-color:").append(CorreoOferta.AMARILLO_FONDO)
					.append(";border:2px dashed ").append(CorreoOferta.AMARILLO_BORDE)
					.append(";border-radius:8px;padding:16px 18px;'>")
					.append("<div style='font-size:11.5px;color:").append(CorreoOferta.AMARILLO_TEXTO)
					.append(";letter-spacing:.14em;text-transform:uppercase;font-weight:bold;'>Tu c&oacute;digo</div>")
					.append("<div style='font-size:30px;font-weight:bold;color:").append(CorreoOferta.TINTA)
					.append(";letter-spacing:.14em;padding-top:6px;font-family:Consolas,\"Courier New\",monospace;'>")
					.append(CorreoOferta.escapar(datos.codigoPromocion.trim())).append("</div>")
					.append("<div style='font-size:12.5px;color:").append(CorreoOferta.AMARILLO_TEXTO)
					.append(";padding-top:7px;'>Dilo al pedir. Es solo tuyo.</div>")
					.append("</td></tr></table></td></tr>");
		}

		//Las condiciones, armadas con los parametros de la oferta.
		StringBuilder filas = new StringBuilder();
		if (fechaVence.length() > 0) {
			filas.append(CorreoOferta.condicion("Vence", "el " + fechaVence, true, false));
		}
		if (controlaHora) {
			filas.append(CorreoOferta.condicion("Horario",
					CorreoOferta.rangoHoras(datos.horaInicio, datos.horaFin), true, false));
		}
		if (dejaSaldo) {
			filas.append(CorreoOferta.condicion("Si gastas menos",
					"te queda el saldo para otra compra", false, false));
		}
		if (datos.nombreTienda != null && datos.nombreTienda.trim().length() > 0) {
			filas.append(CorreoOferta.condicion("Tu tienda",
					CorreoOferta.escapar(datos.nombreTienda.trim()), true, true));
		}
		if (filas.length() > 0) {
			m.append("<tr><td style='padding:20px 26px 6px;'>")
					.append("<table cellpadding='0' cellspacing='0' border='0' width='100%' style='border-collapse:collapse;font-size:13.5px;'>")
					.append(filas).append("</table></td></tr>");
		}

		//Llamado a la accion.
		m.append("<tr><td align='center' style='padding:18px 26px 8px;'>")
				.append("<table cellpadding='0' cellspacing='0' border='0' style='border-collapse:collapse;'><tr>")
				.append("<td align='center' style='background-color:").append(CorreoOferta.ROJO)
				.append(";border-radius:6px;padding:13px 34px;'>")
				.append("<a href='https://").append(CorreoOferta.SITIO)
				.append("' style='font-size:15.5px;font-weight:bold;color:#FFFFFF;text-decoration:none;'>Pedir ahora</a>")
				.append("</td></tr></table>")
				.append("<div style='font-size:12.5px;color:").append(CorreoOferta.TINTA_2)
				.append(";padding-top:12px;'>").append(CorreoOferta.SITIO);
		if (tieneCodigo) {
			m.append(" &nbsp;&middot;&nbsp; o ll&aacute;manos y da tu c&oacute;digo");
		}
		m.append("</div></td></tr>");

		//Letra menuda: el mensaje 2 de la oferta y el aviso de datos personales.
		m.append("<tr><td style='padding:16px 26px 22px;'>")
				.append("<div style='border-top:1px solid ").append(CorreoOferta.LINEA)
				.append(";padding-top:14px;font-size:12px;color:#8A9199;line-height:1.5;'>");
		if (datos.mensaje2 != null && datos.mensaje2.trim().length() > 0) {
			m.append(CorreoOferta.escapar(datos.mensaje2.trim())).append("<br><br>");
		}
		m.append("Recibes este correo porque autorizaste el tratamiento de tus datos con Pizza ")
				.append("Americana. Si no quieres m&aacute;s correos de ofertas, resp&oacute;ndenos y te ")
				.append("sacamos de la lista.")
				.append("</div></td></tr>");

		m.append("</table>");
		return (m.toString());
	}
}

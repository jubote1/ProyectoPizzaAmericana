package utilidadesCC;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.json.simple.JSONObject;

/**
 * El correo que reciben el empleado de una novedad de biometria y quien la reporto
 * cuando el administrador la marca como atendida.
 *
 * Dice tres cosas, en este orden: que se reporto, que se hizo con la jornada y como
 * quedo el horario del dia. Lo ultimo es lo que el empleado va a buscar, por eso es la
 * tabla de abajo y no una frase.
 *
 * Todo en texto y con estilos en linea, para que se vea igual en cualquier cliente de
 * correo. Los acentos van como entidades HTML: el asunto de este sistema no viaja en
 * UTF-8, asi que el asunto se escribe sin acentos.
 */
public final class PlantillaCorreoNovedadBiometria {

	/** A quien va dirigido el correo; cambia el saludo y la primera frase. */
	public enum Destino {
		/** Quien reporto y a quien le cambiaron el horario son la misma persona. */
		MISMA_PERSONA,
		/** Quien reporto la novedad de otro empleado. */
		REPORTA,
		/** El empleado cuyo horario se reviso, reportado por alguien mas. */
		AFECTADO
	}

	private static final String AZUL = "#102F6F";
	private static final String AMARILLO = "#FDC806";
	private static final String TINTA = "#1F2430";
	private static final String GRIS = "#6C7482";
	private static final String FUENTE = "Segoe UI, Roboto, Helvetica, Arial, sans-serif";

	private static final String[] DIAS = { "lunes", "martes", "mi&eacute;rcoles", "jueves", "viernes",
			"s&aacute;bado", "domingo" };
	private static final String[] MESES = { "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
			"agosto", "septiembre", "octubre", "noviembre", "diciembre" };
	private static final String[] MESES_ASUNTO = MESES;

	private PlantillaCorreoNovedadBiometria() {
		super();
	}

	/** Sin acentos a proposito, ver la nota de la clase. */
	public static String asunto(final String fechaJornada) {
		return ("Novedad de biometria atendida - horario del " + fechaCorta(fechaJornada));
	}

	/**
	 * @param destino  a quien va el correo
	 * @param novedad  lo que devuelve BiometriaNovedadDAO.obtenerNovedadParaCorreo
	 * @param cambios  lo que devuelve BiometriaNovedadDAO.obtenerCambiosDeNovedad
	 * @param eventos  lo que devuelve BiometriaNovedadDAO.obtenerEventosDelDia, ya con los cambios aplicados
	 */
	public static String cuerpo(final Destino destino, final JSONObject novedad, final List<JSONObject> cambios,
			final List<JSONObject> eventos) {
		final String fecha = texto(novedad, "fecha");
		final String nombre = texto(novedad, "nombre");
		final String reporta = texto(novedad, "reporta");
		final String fechaLarga = fechaLarga(fecha);

		final String saludo;
		final String intro;
		if (destino == Destino.MISMA_PERSONA) {
			saludo = "&iexcl;Hola " + escapar(primerNombre(nombre)) + "!";
			intro = "La novedad que reportaste sobre tu horario del <b>" + fechaLarga + "</b> ya fue atendida.";
		} else if (destino == Destino.REPORTA) {
			saludo = "&iexcl;Hola " + escapar(primerNombre(reporta)) + "!";
			intro = "La novedad que reportaste sobre el horario de <b>" + escapar(nombre) + "</b> del <b>"
					+ fechaLarga + "</b> ya fue atendida.";
		} else {
			saludo = "&iexcl;Hola " + escapar(primerNombre(nombre)) + "!";
			intro = "<b>" + escapar(reporta) + "</b> report&oacute; una novedad sobre tu horario del <b>"
					+ fechaLarga + "</b> y ya fue atendida.";
		}

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
				.append("<div style=\"font-size:20px;font-weight:bold;color:").append(TINTA)
				.append(";line-height:1.35;padding-top:10px;\">Novedad de biometr&iacute;a atendida</div>")
				.append("<div style=\"font-size:14.5px;color:").append(TINTA)
				.append(";line-height:1.55;padding-top:10px;\">").append(intro).append("</div></td></tr>");

		//Que se reporto
		h.append(titulo("Lo que se report&oacute;"));
		h.append("<tr><td style=\"padding:0 24px;font-size:14px;color:").append(TINTA).append(";line-height:1.55;\">");
		h.append("<b>").append(etiquetaNovedad(texto(novedad, "tiponovedad"))).append("</b>");
		if (texto(novedad, "tienda").length() > 0) {
			h.append(" &middot; ").append(escapar(texto(novedad, "tienda")));
		}
		final String observacion = texto(novedad, "observacion");
		if (observacion.length() > 0) {
			h.append("<br>&laquo;").append(escapar(observacion)).append("&raquo;");
		}
		final String horaReportada = texto(novedad, "horareportada");
		if (horaReportada.length() > 0) {
			h.append("<br>La hora real que se dijo fue <b>").append(hora(horaReportada, fecha)).append("</b>")
					.append(punto(hora(horaReportada, fecha)));
		}
		h.append("</td></tr>");

		//Que se hizo
		h.append(titulo("Lo que se hizo"));
		h.append("<tr><td style=\"padding:0 24px;font-size:14px;color:").append(TINTA).append(";line-height:1.6;\">");
		if (cambios.isEmpty()) {
			h.append("Se revis&oacute; la jornada y qued&oacute; como estaba.");
		} else {
			for (int i = 0; i < cambios.size(); i++) {
				h.append("&bull; ").append(textoCambio(cambios.get(i), fecha)).append("<br>");
			}
		}
		final String resolucion = texto(novedad, "observacionresolucion");
		if (resolucion.length() > 0) {
			h.append("<div style=\"padding-top:6px;color:").append(GRIS).append(";\">Nota de quien la atendi&oacute;: ")
					.append("&laquo;").append(escapar(resolucion)).append("&raquo;</div>");
		}
		h.append("</td></tr>");

		//Como quedo el horario
		h.append(titulo("As&iacute; qued&oacute; el horario del d&iacute;a"));
		h.append("<tr><td style=\"padding:0 24px;\">");
		if (eventos.isEmpty()) {
			h.append("<div style=\"font-size:14px;color:").append(TINTA)
					.append(";\">Ese d&iacute;a no qued&oacute; ning&uacute;n registro.</div>");
		} else {
			h.append("<table cellpadding='0' cellspacing='0' border='0' width='100%'")
					.append(" style='border-collapse:collapse;font-size:14px;'>");
			for (int i = 0; i < eventos.size(); i++) {
				final JSONObject ev = eventos.get(i);
				h.append("<tr><td style=\"padding:9px 12px;border-bottom:1px solid #E2E6EC;background-color:")
						.append(i % 2 == 0 ? "#F7F9FC" : "#FFFFFF").append(";color:").append(TINTA).append(";\"><b>")
						.append(etiquetaEvento(texto(ev, "tipo"))).append("</b></td>")
						.append("<td align='right' style=\"padding:9px 12px;border-bottom:1px solid #E2E6EC;")
						.append("background-color:").append(i % 2 == 0 ? "#F7F9FC" : "#FFFFFF")
						.append(";color:").append(TINTA).append(";\">")
						.append(hora(texto(ev, "fechahora"), fecha)).append("</td></tr>");
			}
			h.append("</table>");
			final String tiempo = tiempoEntreIngresoYSalida(eventos);
			if (tiempo.length() > 0) {
				h.append("<div style=\"padding-top:8px;font-size:13px;color:").append(GRIS).append(";\">")
						.append("Tiempo entre el ingreso y la salida: <b>").append(tiempo).append("</b></div>");
			}
		}
		h.append("<div style=\"height:14px;font-size:0;line-height:0;\">&nbsp;</div></td></tr>");

		String cierre = "";
		if (texto(novedad, "resueltopor").length() > 0) {
			cierre = "Atendida por " + escapar(texto(novedad, "resueltopor"));
			if (texto(novedad, "fecharesolucion").length() >= 16) {
				cierre += " el " + fechaCorta(texto(novedad, "fecharesolucion").substring(0, 10)) + " a las "
						+ hora(texto(novedad, "fecharesolucion"), texto(novedad, "fecharesolucion").substring(0, 10))
						+ punto(hora(texto(novedad, "fecharesolucion"),
								texto(novedad, "fecharesolucion").substring(0, 10)))
						+ " ";
			} else {
				cierre += ". ";
			}
		}
		h.append("<tr><td style=\"padding:20px 24px 22px;border-top:1px solid #E2E6EC;font-size:12px;color:#8A9199;")
				.append("line-height:1.5;\">").append(cierre)
				.append("Si algo del horario no est&aacute; bien, habla con el Administrador de tu tienda.")
				.append("</td></tr>");

		h.append("</table></div>");
		return (h.toString());
	}

	/** El punto que cierra la frase, salvo que la hora ya termine en uno ("p. m."): asi no sale "p. m..". */
	private static String punto(final String horaHtml) {
		return (horaHtml.endsWith(".") ? "" : ".");
	}

	private static String titulo(final String texto) {
		return ("<tr><td style=\"padding:20px 24px 6px;font-size:12px;color:" + GRIS
				+ ";text-transform:uppercase;letter-spacing:.08em;font-weight:bold;\">" + texto + "</td></tr>");
	}

	private static String textoCambio(final JSONObject cambio, final String fechaJornada) {
		final String accion = texto(cambio, "accion");
		final String antesTipo = texto(cambio, "antestipo");
		final String despuesTipo = texto(cambio, "despuestipo");
		final String antesHora = hora(texto(cambio, "antesfechahora"), fechaJornada);
		final String despuesHora = hora(texto(cambio, "despuesfechahora"), fechaJornada);
		if ("AGREGA".equals(accion)) {
			return ("Se agreg&oacute; " + articulo(despuesTipo) + " <b>" + etiquetaEvento(despuesTipo)
					+ "</b> de las <b>" + despuesHora + "</b>" + punto(despuesHora));
		}
		if ("ELIMINA".equals(accion)) {
			return ("Se elimin&oacute; " + articulo(antesTipo) + " <b>" + etiquetaEvento(antesTipo)
					+ "</b> de las <b>" + antesHora + "</b>" + punto(antesHora));
		}
		if (antesTipo.equals(despuesTipo)) {
			return ("Se cambi&oacute; " + articulo(antesTipo) + " <b>" + etiquetaEvento(antesTipo)
					+ "</b> de las <b>" + antesHora + "</b> a las <b>" + despuesHora + "</b>" + punto(despuesHora));
		}
		return ("Se cambi&oacute; " + articulo(antesTipo) + " <b>" + etiquetaEvento(antesTipo) + "</b> de las <b>"
				+ antesHora + "</b> por " + articulo(despuesTipo) + " <b>" + etiquetaEvento(despuesTipo)
				+ "</b> de las <b>" + despuesHora + "</b>" + punto(despuesHora));
	}

	/**
	 * Suma los tramos INGRESO -> SALIDA. Solo se muestra si todos los registros del dia
	 * forman pares completos; con un ingreso sin salida (o al reves) cualquier numero
	 * seria inventado.
	 */
	private static String tiempoEntreIngresoYSalida(final List<JSONObject> eventos) {
		long minutos = 0;
		LocalDateTime ingreso = null;
		for (int i = 0; i < eventos.size(); i++) {
			final String tipo = texto(eventos.get(i), "tipo");
			final LocalDateTime momento = momento(texto(eventos.get(i), "fechahora"));
			if (momento == null) {
				return ("");
			}
			if ("INGRESO".equals(tipo)) {
				if (ingreso != null) {
					return ("");
				}
				ingreso = momento;
			} else if ("SALIDA".equals(tipo)) {
				if (ingreso == null) {
					return ("");
				}
				minutos += Duration.between(ingreso, momento).toMinutes();
				ingreso = null;
			} else {
				return ("");
			}
		}
		if (ingreso != null || minutos <= 0) {
			return ("");
		}
		return ((minutos / 60) + " h " + (minutos % 60) + " min");
	}

	/** "INGRESO" -> "el", "SALIDA" -> "la". */
	private static String articulo(final String tipo) {
		return ("SALIDA".equals(tipo) ? "la" : "el");
	}

	private static String etiquetaEvento(final String tipo) {
		if ("INGRESO".equals(tipo)) {
			return ("Ingreso");
		}
		if ("SALIDA".equals(tipo)) {
			return ("Salida");
		}
		return (escapar(tipo));
	}

	private static String etiquetaNovedad(final String tipo) {
		if ("CORREGIR HORA".equals(tipo)) {
			return ("Corregir una hora");
		}
		if ("FALTA LA SALIDA".equals(tipo)) {
			return ("Falta la salida");
		}
		if ("FALTA EL INGRESO".equals(tipo)) {
			return ("Falta el ingreso");
		}
		return (tipo.length() == 0 ? "Novedad" : escapar(tipo.substring(0, 1) + tipo.substring(1).toLowerCase()));
	}

	/** aaaa-mm-dd hh:mm:ss[.f] -> momento, o null si no se puede leer. */
	private static LocalDateTime momento(final String valor) {
		if (valor == null || valor.length() < 19) {
			return (null);
		}
		try {
			return (LocalDateTime.parse(valor.substring(0, 19).replace(' ', 'T')));
		} catch (final Exception e) {
			return (null);
		}
	}

	/**
	 * La hora como la dice la gente, "2:09 p. m.". Si cae en un dia distinto al de la
	 * jornada (la salida de madrugada) lo aclara, para que "12:30 a. m." no parezca un error.
	 */
	private static String hora(final String valor, final String fechaJornada) {
		final LocalDateTime m = momento(valor);
		if (m == null) {
			return (escapar(valor));
		}
		final int h12 = (m.getHour() % 12 == 0) ? 12 : m.getHour() % 12;
		final String sufijo = (m.getHour() < 12) ? "a.&nbsp;m." : "p.&nbsp;m.";
		String r = h12 + ":" + (m.getMinute() < 10 ? "0" : "") + m.getMinute() + " " + sufijo;
		if (fechaJornada != null && fechaJornada.length() >= 10
				&& !m.toLocalDate().toString().equals(fechaJornada.substring(0, 10))) {
			r += " (d&iacute;a siguiente)";
		}
		return (r);
	}

	private static String fechaLarga(final String fecha) {
		try {
			final LocalDate d = LocalDate.parse(fecha.substring(0, 10));
			return (DIAS[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " de "
					+ MESES[d.getMonthValue() - 1] + " de " + d.getYear());
		} catch (final Exception e) {
			return (escapar(fecha));
		}
	}

	/** Para el asunto: sin acentos ni entidades. */
	private static String fechaCorta(final String fecha) {
		try {
			final LocalDate d = LocalDate.parse(fecha.substring(0, 10));
			return (d.getDayOfMonth() + " de " + MESES_ASUNTO[d.getMonthValue() - 1]);
		} catch (final Exception e) {
			return (fecha);
		}
	}

	private static String texto(final JSONObject obj, final String clave) {
		final Object v = obj.get(clave);
		return (v == null ? "" : String.valueOf(v));
	}

	private static String primerNombre(final String nombre) {
		final String limpio = nombre == null ? "" : nombre.trim();
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

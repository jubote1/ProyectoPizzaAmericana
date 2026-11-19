package utilidadesCC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;

/**
 * Alertas por correo de fallas en la consulta de cobertura (UbicacionCtrl).
 *
 * Antes se enviaba un correo por cada consulta fallida: si ArcGIS dejaba de responder unos minutos,
 * llegaba un correo por cada lead. Ahora:
 *
 *  - La PRIMERA falla se avisa de inmediato, con todo su detalle.
 *  - Las siguientes fallas de los proximos MINUTOS_VENTANA minutos (10) se acumulan y se envian juntas en UN correo,
 *    con una fila por cada falla (hora, servicio que fallo, direccion, lead, cliente y error).
 *    No se pierde informacion: solo se agrupa.
 *  - El envio es en segundo plano, para que la consulta del lead no espere a Gmail.
 *
 * Los datos del cliente y del lead se toman del contexto de la consulta en curso
 * (ver iniciarContexto / actualizarCliente / limpiarContexto), asi el detalle llega
 * aunque la falla ocurra en un metodo interno que no los recibe por parametro.
 */
public final class AlertaCobertura {

	/** Asunto igual al historico, para no romper filtros de correo existentes. */
	static final String ASUNTO = "TENEMOS PROBLEMA CON LA CONSULTA DE LA COBERTURA";

	private static final String[] DESTINATARIOS = { "jubote1@gmail.com", "a.desarrollosi@gmail.com" };

	/** Tope de filas por correo para no armar un correo gigante si la falla dura mucho. */
	static final int MAXIMO_DETALLES = 300;

	private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/** Envia un correo ya armado. Reemplazable en pruebas. */
	interface Enviador {
		void enviar(String asunto, String mensajeHtml);
	}

	private static final Object CANDADO = new Object();
	private static final ScheduledExecutorService EJECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "AlertaCobertura");
		t.setDaemon(true);
		return t;
	});

	/** Cada cuanto se envia, como maximo, un correo de alerta. Cambiar aqui si se quiere otro intervalo. */
	public static final int MINUTOS_VENTANA = 10;

	static volatile long ventanaMs = TimeUnit.MINUTES.toMillis(MINUTOS_VENTANA);
	static volatile Enviador enviador = AlertaCobertura::enviarPorCorreo;

	private static long ultimoEnvioMs = 0;
	private static final List<Falla> pendientes = new ArrayList<>();
	private static int omitidas = 0;
	private static ScheduledFuture<?> envioProgramado = null;

	private static final ThreadLocal<Contexto> CONTEXTO = new ThreadLocal<>();

	private AlertaCobertura() {
	}

	// ------------------------------------------------------------------------------------------
	// Contexto de la consulta en curso
	// ------------------------------------------------------------------------------------------

	/** Llamar al inicio de la consulta de cobertura. Siempre cerrar con limpiarContexto() en un finally. */
	public static void iniciarContexto(String direccion, String lead, Integer idcliente, String telefono) {
		Contexto c = new Contexto();
		c.direccion = direccion;
		c.lead = lead;
		c.idcliente = idcliente;
		c.telefono = telefono;
		CONTEXTO.set(c);
	}

	/** Completa el contexto cuando ya se cargo el cliente de la base de datos. */
	public static void actualizarCliente(Integer idcliente, String nombre, String telefono, String direccion) {
		Contexto c = CONTEXTO.get();
		if (c == null) {
			return;
		}
		if (idcliente != null && idcliente > 0) {
			c.idcliente = idcliente;
		}
		if (noVacio(nombre)) {
			c.nombreCliente = nombre.trim();
		}
		if (noVacio(telefono)) {
			c.telefono = telefono.trim();
		}
		if (noVacio(direccion)) {
			c.direccion = direccion.trim();
		}
	}

	public static void limpiarContexto() {
		CONTEXTO.remove();
	}

	// ------------------------------------------------------------------------------------------
	// Registro de fallas
	// ------------------------------------------------------------------------------------------

	/**
	 * Registra una falla. Si direccion o lead vienen vacios se toman del contexto de la consulta.
	 *
	 * @param servicio que fallo, por ejemplo "ArcGIS geocodificacion (geocode-api.arcgis.com)"
	 * @param error    mensaje de la excepcion
	 */
	public static void registrar(String servicio, String direccion, String lead, String error) {
		Falla f = new Falla();
		f.hora = LocalDateTime.now();
		f.servicio = servicio;
		f.error = error;
		Contexto c = CONTEXTO.get();
		f.direccion = noVacio(direccion) ? direccion.trim() : (c != null ? c.direccion : null);
		f.lead = noVacio(lead) ? lead.trim() : (c != null ? c.lead : null);
		if (c != null) {
			f.idcliente = c.idcliente;
			f.nombreCliente = c.nombreCliente;
			f.telefono = c.telefono;
		}

		long ahora = System.currentTimeMillis();
		synchronized (CANDADO) {
			boolean ventanaLibre = ahora - ultimoEnvioMs >= ventanaMs && pendientes.isEmpty() && envioProgramado == null;
			if (ventanaLibre) {
				// Primera falla despues de un periodo tranquilo: se avisa ya.
				ultimoEnvioMs = ahora;
				List<Falla> una = new ArrayList<>();
				una.add(f);
				EJECUTOR.execute(() -> enviarSeguro(una, 0, false));
				return;
			}
			if (pendientes.size() < MAXIMO_DETALLES) {
				pendientes.add(f);
			} else {
				omitidas++;
			}
			if (envioProgramado == null) {
				long espera = Math.max(0, ultimoEnvioMs + ventanaMs - ahora);
				envioProgramado = EJECUTOR.schedule(AlertaCobertura::enviarAcumuladas, espera, TimeUnit.MILLISECONDS);
			}
		}
	}

	private static void enviarAcumuladas() {
		List<Falla> lote;
		int sinDetalle;
		synchronized (CANDADO) {
			lote = new ArrayList<>(pendientes);
			sinDetalle = omitidas;
			pendientes.clear();
			omitidas = 0;
			envioProgramado = null;
			ultimoEnvioMs = System.currentTimeMillis();
		}
		if (!lote.isEmpty()) {
			enviarSeguro(lote, sinDetalle, true);
		}
	}

	private static void enviarSeguro(List<Falla> fallas, int sinDetalle, boolean agrupado) {
		try {
			enviador.enviar(ASUNTO, armarMensaje(fallas, sinDetalle, agrupado));
		} catch (Exception e) {
			// Una alerta que no se pudo enviar no debe tumbar nada; queda en el log del servidor.
			System.err.println("AlertaCobertura: no se pudo enviar la alerta: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------------------------------
	// Mensaje
	// ------------------------------------------------------------------------------------------

	static String armarMensaje(List<Falla> fallas, int sinDetalle, boolean agrupado) {
		StringBuilder sb = new StringBuilder();
		int total = fallas.size() + sinDetalle;
		if (agrupado) {
			sb.append("<p>Se encontraron <b>").append(total).append("</b> fallas en la consulta de cobertura")
					.append(" en los ultimos ").append(TimeUnit.MILLISECONDS.toMinutes(ventanaMs))
					.append(" minutos (despues del primer aviso).</p>");
		} else {
			sb.append("<p>Se encontro un problema al invocar uno de los servicios de Geocodificacion.</p>");
			sb.append("<p>Si siguen ocurriendo fallas, se enviaran agrupadas cada ")
					.append(TimeUnit.MILLISECONDS.toMinutes(ventanaMs)).append(" minutos.</p>");
		}

		sb.append("<table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse;font-family:Arial;font-size:12px'>");
		sb.append("<tr style='background:#eee'><th>Hora</th><th>Servicio</th><th>Direccion</th><th>Lead</th>")
				.append("<th>Cliente</th><th>Error</th></tr>");
		for (Falla f : fallas) {
			sb.append("<tr>")
					.append(celda(f.hora.format(FORMATO_HORA)))
					.append(celda(f.servicio))
					.append(celda(f.direccion))
					.append(celda(f.lead))
					.append(celda(describirCliente(f)))
					.append(celda(f.error))
					.append("</tr>");
		}
		sb.append("</table>");
		if (sinDetalle > 0) {
			sb.append("<p>Y ").append(sinDetalle)
					.append(" fallas mas sin detalle (se alcanzo el maximo de ").append(MAXIMO_DETALLES)
					.append(" filas por correo).</p>");
		}
		return sb.toString();
	}

	private static String describirCliente(Falla f) {
		StringBuilder sb = new StringBuilder();
		if (f.idcliente != null && f.idcliente > 0) {
			sb.append("Id ").append(f.idcliente);
		}
		if (noVacio(f.nombreCliente)) {
			sb.append(sb.length() > 0 ? " - " : "").append(f.nombreCliente);
		}
		if (noVacio(f.telefono)) {
			sb.append(sb.length() > 0 ? " - Tel " : "Tel ").append(f.telefono);
		}
		return sb.toString();
	}

	private static String celda(String valor) {
		return "<td>" + escaparHtml(valor) + "</td>";
	}

	/** La direccion la escribe el cliente: se escapa para que no pueda inyectar HTML en el correo. */
	static String escaparHtml(String valor) {
		if (valor == null || valor.isEmpty()) {
			return "&nbsp;";
		}
		StringBuilder sb = new StringBuilder(valor.length());
		for (char ch : valor.toCharArray()) {
			switch (ch) {
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '&' -> sb.append("&amp;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&#39;");
				default -> sb.append(ch);
			}
		}
		return sb.toString();
	}

	private static void enviarPorCorreo(String asunto, String mensajeHtml) {
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
		if (infoCorreo == null || infoCorreo.getCuentaCorreo() == null || infoCorreo.getCuentaCorreo().isBlank()) {
			System.err.println("AlertaCobertura: no se encontraron credenciales para CUENTACORREOERROR en la base de datos.");
			return;
		}
		Correo correo = new Correo();
		correo.setAsunto(asunto);
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setMensaje(mensajeHtml);
		ArrayList<String> correos = new ArrayList<>();
		for (String d : DESTINATARIOS) {
			correos.add(d);
		}
		new ControladorEnvioCorreo(correo, correos).enviarCorreo();
	}

	private static boolean noVacio(String s) {
		return s != null && !s.trim().isEmpty();
	}

	/** Solo para pruebas: deja el estado en cero. */
	static void reiniciarParaPruebas() {
		synchronized (CANDADO) {
			if (envioProgramado != null) {
				envioProgramado.cancel(false);
			}
			envioProgramado = null;
			pendientes.clear();
			omitidas = 0;
			ultimoEnvioMs = 0;
		}
	}

	static final class Falla {
		LocalDateTime hora;
		String servicio;
		String direccion;
		String lead;
		Integer idcliente;
		String nombreCliente;
		String telefono;
		String error;
	}

	private static final class Contexto {
		String direccion;
		String lead;
		Integer idcliente;
		String nombreCliente;
		String telefono;
	}
}

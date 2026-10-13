package capaControladorCC;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.CampanaDAO;
import capaDAOCC.CodigoPromoDAO;
import capaDAOCC.CompensacionTiempoDAO;
import capaDAOCC.OfertaClienteDAO;
import utilidadesCC.CorreoOferta;
import utilidadesCC.EnviadorPublicidadDirecta;

/**
 * Compensaciones por tiempo: listar los pedidos que se demoraron y mandarle una oferta de disculpa a
 * quienes se escojan.
 *
 * LO QUE ESTE CONTROLADOR NO CONFIA
 *
 * Del navegador solo recibe QUE pedidos se escogieron (tienda + numero) y con que oferta. Todo lo demas
 * -que de verdad se demoraron, que el cliente exista, a quien y con que correo se le escribe- se vuelve a
 * leer del datamart al momento de enviar. Asi un pedido que no cumplia el filtro no se cuela porque
 * alguien escribio un numero a mano en la peticion, y los minutos que quedan asentados son los reales.
 *
 * EL ENVIO ES EL DE SIEMPRE
 *
 * Una campana "COMPENSACION POR TIEMPO" (la misma para todos los dias: asi se puede medir si compensar
 * sirve), una tanda por envio, la oferta ligada a la tanda y un codigo personal por destinatario. Sale
 * por el correo directo, de a uno, por las mismas razones de reputacion del dominio de cualquier envio
 * con oferta. El correo es el de la oferta en su version de disculpa (ver CorreoOferta).
 */
public class CompensacionTiempoCtrl {

	/** La campana donde caen todas las tandas de compensacion. */
	public static final String NOMBRE_CAMPANA = "COMPENSACION POR TIEMPO";

	/** Cuantos pedidos se aceptan en una sola peticion. Con el tope del correo directo casi nunca se llega. */
	private static final int MAXIMO_SELECCION = 500;

	private CompensacionTiempoCtrl() {
	}

	// =======================================================================
	// Consultar
	// =======================================================================

	@SuppressWarnings("unchecked")
	public static String consultar(final String fecha, final int minutos, final int idTienda,
			final boolean soloPasaronPromesa) {
		final JSONObject r = new JSONObject();
		final String problema = problemaDeParametros(fecha, minutos);
		if (problema.length() > 0) {
			r.put("error", problema);
			return (r.toJSONString());
		}
		final CompensacionTiempoDAO.Resultado res = CompensacionTiempoDAO.listar(fecha, minutos, idTienda, soloPasaronPromesa);
		if (res.error.length() > 0) {
			r.put("error", res.error);
			return (r.toJSONString());
		}

		final JSONArray lista = new JSONArray();
		int puedenRecibir = 0;
		int yaCompensados = 0;
		for (final CompensacionTiempoDAO.Pedido p : res.pedidos) {
			final JSONObject o = new JSONObject();
			o.put("idtienda", p.idTienda);
			o.put("tienda", p.tienda);
			o.put("idpedido", p.idPedidoTienda);
			o.put("hora_pedido", p.horaPedido);
			o.put("hora_entrega", p.horaEntrega);
			o.put("prometido", p.prometido);
			o.put("minutos", p.minutos);
			o.put("limite", p.limite);
			o.put("exceso", p.exceso());
			o.put("cliente", p.cliente);
			o.put("email", p.email);
			o.put("puede", p.puede);
			o.put("motivo", p.motivo);
			o.put("ya_compensado", p.yaCompensado);
			if (p.puede) {
				puedenRecibir++;
			}
			if (p.yaCompensado) {
				yaCompensados++;
			}
			lista.add(o);
		}
		final JSONArray sinDatos = new JSONArray();
		sinDatos.addAll(res.tiendasSinDatos);

		r.put("pedidos", lista);
		r.put("total", res.pedidos.size());
		r.put("pueden_recibir", puedenRecibir);
		r.put("ya_compensados", yaCompensados);
		r.put("tiendas_sin_datos", sinDatos);
		r.put("truncado", res.truncado);
		r.put("tolerancia", res.tolerancia);
		r.put("tope_directo", EnviadorPublicidadDirecta.tope());
		r.put("segundos_directo", EnviadorPublicidadDirecta.segundosEntreCorreos());
		return (r.toJSONString());
	}

	// =======================================================================
	// Resumen de cumplimiento
	// =======================================================================

	/** Los acumulados de un grupo (todo, una tienda o un dia). */
	private static class Acumulado {
		int total;
		int cumplidos;
		int incumplidos;
		int sobreUmbral;
		long sumaMinutos;
		int maximo;
		String clave = "";
		String nombre = "";

		void sumar(final CompensacionTiempoDAO.FilaResumen f) {
			total += f.total;
			cumplidos += f.cumplidos;
			incumplidos += f.incumplidos;
			sobreUmbral += f.sobreUmbral;
			sumaMinutos += f.sumaMinutos;
			maximo = Math.max(maximo, f.maximo);
		}

		double porcentaje() {
			return (total == 0 ? 0 : Math.round(cumplidos * 1000.0 / total) / 10.0);
		}

		double promedio() {
			return (total == 0 ? 0 : Math.round(sumaMinutos * 10.0 / total) / 10.0);
		}

		@SuppressWarnings("unchecked")
		JSONObject aJson() {
			final JSONObject o = new JSONObject();
			o.put("clave", clave);
			o.put("nombre", nombre);
			o.put("total", total);
			o.put("cumplidos", cumplidos);
			o.put("incumplidos", incumplidos);
			o.put("sobre_umbral", sobreUmbral);
			o.put("porcentaje", porcentaje());
			o.put("promedio", promedio());
			o.put("maximo", maximo);
			return (o);
		}
	}

	/**
	 * Cuantos pedidos cumplieron la promesa de tiempo y cuantos no: en general, por tienda y -si el
	 * rango son varios dias- por dia.
	 *
	 * @param dias 1, 7 o 30: el dia escogido y los anteriores hasta completar ese numero
	 * @param umbral minutos del filtro de la lista; solo para contar cuantos lo pasaron
	 */
	@SuppressWarnings("unchecked")
	public static String resumen(final String fecha, final int dias, final int umbral) {
		final JSONObject r = new JSONObject();
		if (!fechaValida(fecha)) {
			r.put("error", "Escoja una fecha valida.");
			return (r.toJSONString());
		}
		final int rango = (dias == 7 || dias == 30) ? dias : 1;
		String desde = fecha;
		try {
			final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
			f.setLenient(false);
			final Calendar c = Calendar.getInstance();
			c.setTime(f.parse(fecha));
			c.add(Calendar.DAY_OF_YEAR, -(rango - 1));
			desde = f.format(c.getTime());
		} catch (final java.text.ParseException e) {
			r.put("error", "Escoja una fecha valida.");
			return (r.toJSONString());
		}

		final CompensacionTiempoDAO.ResultadoResumen res =
				CompensacionTiempoDAO.resumen(desde, fecha, umbral > 0 ? umbral : 0);
		if (res.error.length() > 0) {
			r.put("error", res.error);
			return (r.toJSONString());
		}

		final Acumulado general = new Acumulado();
		final Map<String, Acumulado> porTienda = new LinkedHashMap<String, Acumulado>();
		final Map<String, Acumulado> porDia = new LinkedHashMap<String, Acumulado>();
		for (final CompensacionTiempoDAO.FilaResumen f : res.filas) {
			general.sumar(f);
			Acumulado t = porTienda.get(String.valueOf(f.idTienda));
			if (t == null) {
				t = new Acumulado();
				t.clave = String.valueOf(f.idTienda);
				t.nombre = f.tienda;
				porTienda.put(t.clave, t);
			}
			t.sumar(f);
			Acumulado d = porDia.get(f.fecha);
			if (d == null) {
				d = new Acumulado();
				d.clave = f.fecha;
				d.nombre = f.fecha;
				porDia.put(f.fecha, d);
			}
			d.sumar(f);
		}

		//La peor primero: es la que hay que mirar. Con igual porcentaje, la que mas pedidos tiene.
		final List<Acumulado> tiendas = new ArrayList<Acumulado>(porTienda.values());
		Collections.sort(tiendas, new Comparator<Acumulado>() {
			public int compare(final Acumulado a, final Acumulado b) {
				final int c = Double.compare(a.porcentaje(), b.porcentaje());
				return (c != 0 ? c : Integer.compare(b.total, a.total));
			}
		});
		final JSONArray jTiendas = new JSONArray();
		for (final Acumulado a : tiendas) {
			jTiendas.add(a.aJson());
		}
		final JSONArray jDias = new JSONArray();
		if (rango > 1) {
			for (final Acumulado a : porDia.values()) {
				jDias.add(a.aJson());
			}
		}
		final JSONArray sinDatos = new JSONArray();
		sinDatos.addAll(res.tiendasSinDatos);

		r.put("desde", desde);
		r.put("hasta", fecha);
		r.put("dias", rango);
		r.put("umbral", umbral);
		r.put("general", general.aJson());
		r.put("tiendas", jTiendas);
		r.put("por_dia", jDias);
		r.put("tolerancia", res.tolerancia);
		r.put("tiendas_sin_datos", sinDatos);
		return (r.toJSONString());
	}

	// =======================================================================
	// Vista previa del correo
	// =======================================================================

	/**
	 * El correo de disculpa tal como le va a llegar a un cliente, con datos de ejemplo. Se arma con la
	 * oferta escogida -su valor y su caducidad-, para que quien envia vea EXACTAMENTE que va a recibir
	 * la gente antes de oprimir el boton.
	 */
	@SuppressWarnings("unchecked")
	public static String vistaPrevia(final int idOferta, final String fechaPedido) {
		final JSONObject r = new JSONObject();
		CodigoPromoDAO.OfertaEnviable oferta = null;
		for (final CodigoPromoDAO.OfertaEnviable e : CodigoPromoDAO.ofertasEnviables()) {
			if (e.idOferta == idOferta) {
				oferta = e;
				break;
			}
		}
		if (oferta == null) {
			r.put("error", "Esa oferta no esta disponible para enviar.");
			return (r.toJSONString());
		}
		final String fecha = fechaValida(fechaPedido) ? fechaPedido : ayer();

		final OfertaClienteDAO.DatosCorreoOferta d = new OfertaClienteDAO.DatosCorreoOferta();
		d.nombreCliente = "Maria Perez";
		d.soloNombre = "Maria";
		d.soloApellido = "Perez";
		d.codigoPromocion = "AB12CD34";
		d.descuentoValor = oferta.valor;
		d.descuentoPorcentaje = oferta.porcentaje;
		d.redencionParcial = oferta.redParcial;
		d.nombreOferta = oferta.nombre;
		final Calendar vence = Calendar.getInstance();
		vence.add(Calendar.DAY_OF_YEAR, Math.max(oferta.dias, 0));
		d.fechaCaducidad = new SimpleDateFormat("yyyy-MM-dd").format(vence.getTime());

		r.put("asunto", CorreoOferta.armarAsuntoDisculpa(d, fecha));
		r.put("html", CorreoOferta.armarCuerpoDisculpa(d, fecha));
		return (r.toJSONString());
	}

	// =======================================================================
	// Enviar
	// =======================================================================

	/**
	 * @param seleccion cada elemento "idtienda:idpedidotienda"
	 */
	@SuppressWarnings("unchecked")
	public static String enviar(final String fecha, final int minutos, final boolean soloPasaronPromesa,
			final int idOferta, final List<String> seleccion, final String usuario) {
		final JSONObject r = new JSONObject();

		final String problema = problemaDeParametros(fecha, minutos);
		if (problema.length() > 0) {
			r.put("error", problema);
			return (r.toJSONString());
		}
		if (idOferta <= 0) {
			r.put("error", "Escoja la oferta con la que se va a compensar.");
			return (r.toJSONString());
		}
		if (seleccion == null || seleccion.isEmpty()) {
			r.put("error", "Escoja al menos un pedido.");
			return (r.toJSONString());
		}
		if (seleccion.size() > MAXIMO_SELECCION) {
			r.put("error", "Son demasiados pedidos de una vez (maximo " + MAXIMO_SELECCION + ").");
			return (r.toJSONString());
		}
		final String problemaOferta = CodigoPromoDAO.problemaParaEnviar(idOferta);
		if (problemaOferta.length() > 0) {
			r.put("error", problemaOferta);
			return (r.toJSONString());
		}
		final long enCurso = CampanaDAO.envioDirectoEnCurso();
		if (enCurso != 0) {
			r.put("error", "Ya hay un envio de correo directo en curso (el " + enCurso
					+ "). Espere a que termine.");
			return (r.toJSONString());
		}

		//LO ESCOGIDO SE VUELVE A LEER. De la peticion solo se toma el numero de cada pedido.
		final CompensacionTiempoDAO.Resultado res = CompensacionTiempoDAO.listar(fecha, minutos, 0, soloPasaronPromesa);
		if (res.error.length() > 0) {
			r.put("error", res.error);
			return (r.toJSONString());
		}
		final Map<String, CompensacionTiempoDAO.Pedido> porClave =
				new HashMap<String, CompensacionTiempoDAO.Pedido>();
		for (final CompensacionTiempoDAO.Pedido p : res.pedidos) {
			porClave.put(p.idTienda + ":" + p.idPedidoTienda, p);
		}

		final Map<String, Integer> omitidos = new LinkedHashMap<String, Integer>();
		final List<CompensacionTiempoDAO.Pedido> validos = new ArrayList<CompensacionTiempoDAO.Pedido>();
		final java.util.Set<String> vistos = new java.util.HashSet<String>();
		for (final String clave : seleccion) {
			final String k = clave == null ? "" : clave.trim();
			if (!vistos.add(k)) {
				continue;
			}
			final CompensacionTiempoDAO.Pedido p = porClave.get(k);
			if (p == null) {
				contar(omitidos, "No cumple el filtro de ese dia (ya no aparece en la lista)");
			} else if (!p.puede) {
				contar(omitidos, p.motivo);
			} else {
				validos.add(p);
			}
		}
		if (validos.isEmpty()) {
			r.put("error", "Ninguno de los pedidos escogidos se puede compensar. " + resumenOmitidos(omitidos));
			return (r.toJSONString());
		}

		//UN CORREO POR PERSONA. Quien tuvo dos pedidos tarde ese dia recibe una sola disculpa; los dos
		//pedidos quedan asentados. Salen primero los que mas se demoraron.
		final Map<Long, Integer> peorMinutos = new HashMap<Long, Integer>();
		for (final CompensacionTiempoDAO.Pedido p : validos) {
			final Integer actual = peorMinutos.get(Long.valueOf(p.idPersona));
			if (actual == null || p.minutos > actual.intValue()) {
				peorMinutos.put(Long.valueOf(p.idPersona), Integer.valueOf(p.minutos));
			}
		}
		final List<Long> personas = new ArrayList<Long>(peorMinutos.keySet());
		Collections.sort(personas, new Comparator<Long>() {
			public int compare(final Long a, final Long b) {
				return (peorMinutos.get(b).compareTo(peorMinutos.get(a)));
			}
		});

		//El tope del correo directo lo pone el servidor: es proteccion del dominio, no una preferencia.
		final int tope = EnviadorPublicidadDirecta.tope();
		int quedanParaDespues = 0;
		final List<Long> enEstaTanda;
		if (personas.size() > tope) {
			enEstaTanda = new ArrayList<Long>(personas.subList(0, tope));
			quedanParaDespues = personas.size() - tope;
		} else {
			enEstaTanda = personas;
		}

		long idCampana = CampanaDAO.buscarPorNombre(NOMBRE_CAMPANA);
		if (idCampana == 0) {
			idCampana = CampanaDAO.crear(NOMBRE_CAMPANA, CampanaDAO.CANAL_DIRECTO, 0, "", "",
					"Pedidos que se demoraron: disculpa con oferta", 0, 0, usuario);
		}
		if (idCampana == 0) {
			r.put("error", "No se pudo crear la campana de compensaciones.");
			return (r.toJSONString());
		}

		final String filtros = "compensacion fecha=" + fecha + " | minutos>=" + minutos
				+ (soloPasaronPromesa ? " | solo los que pasaron la promesa" : "");
		final long idEnvio = CampanaDAO.crearEnvio(idCampana, CampanaDAO.CANAL_DIRECTO, 0,
				"Correo de la oferta " + idOferta + " (disculpa)", "", filtros, enEstaTanda.size(), 0, usuario);
		if (idEnvio == 0) {
			r.put("error", "No se pudo abrir la tanda de envio.");
			return (r.toJSONString());
		}
		CampanaDAO.asignarOferta(idEnvio, idOferta);

		final int cargados = CampanaDAO.cargarDestinatariosDePersonas(idEnvio, idCampana, enEstaTanda);
		if (cargados == 0) {
			CampanaDAO.cambiarEstado(idEnvio, "CANCELADA");
			r.put("error", "Ninguno quedo en la tanda: los correos no pasaron el filtro del envio "
					+ "(autorizacion de publicidad o direccion rebotada). " + resumenOmitidos(omitidos));
			r.put("idenvio", idEnvio);
			return (r.toJSONString());
		}

		//Se asienta ANTES de arrancar: cada correo busca aqui la fecha de su pedido para armar la disculpa.
		final Set<Long> cargadas = CompensacionTiempoDAO.personasDeEnvio(idEnvio);
		final int asentados = CompensacionTiempoDAO.registrar(idEnvio, idOferta, usuario, validos, cargadas);

		final String arranque = EnviadorPublicidadDirecta.arrancar(idEnvio);
		if (arranque.length() > 0) {
			r.put("error", arranque);
			r.put("idenvio", idEnvio);
			return (r.toJSONString());
		}

		Logger.getLogger("log_file").info("[CompensacionTiempo] " + usuario + " envio la tanda " + idEnvio
				+ " (oferta " + idOferta + ", fecha " + fecha + ", >= " + minutos + " min): " + cargados
				+ " personas, " + asentados + " pedidos asentados");

		final int sinFiltro = enEstaTanda.size() - cargados;
		final StringBuilder msg = new StringBuilder();
		msg.append("En camino a ").append(cargados).append(cargados == 1 ? " persona." : " personas.");
		if (sinFiltro > 0) {
			msg.append(" ").append(sinFiltro).append(sinFiltro == 1 ? " no entro" : " no entraron")
					.append(" a la tanda: pidio no recibir publicidad o su correo habia rebotado.");
		}
		if (quedanParaDespues > 0) {
			msg.append(" Quedaron ").append(quedanParaDespues)
					.append(" para cuando termine esta tanda (el correo directo es de a ").append(tope)
					.append("): vuelva a consultar y escoja los que siguen.");
		}
		if (!omitidos.isEmpty()) {
			msg.append(" No se incluyeron: ").append(resumenOmitidos(omitidos));
		}
		r.put("idenvio", idEnvio);
		r.put("idcampana", idCampana);
		r.put("enviadas", cargados);
		r.put("quedan", quedanParaDespues);
		r.put("mensaje", msg.toString());
		return (r.toJSONString());
	}

	// -----------------------------------------------------------------------

	private static String problemaDeParametros(final String fecha, final int minutos) {
		if (!fechaValida(fecha)) {
			return ("Escoja una fecha valida.");
		}
		if (minutos <= 0 || minutos > 600) {
			return ("Los minutos deben estar entre 1 y 600.");
		}
		return ("");
	}

	private static boolean fechaValida(final String fecha) {
		if (fecha == null || !fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return (false);
		}
		try {
			final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
			f.setLenient(false);
			f.parse(fecha);
			return (true);
		} catch (final java.text.ParseException e) {
			return (false);
		}
	}

	private static String ayer() {
		final Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		return (new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
	}

	private static void contar(final Map<String, Integer> m, final String motivo) {
		final Integer n = m.get(motivo);
		m.put(motivo, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
	}

	private static String resumenOmitidos(final Map<String, Integer> omitidos) {
		final StringBuilder t = new StringBuilder();
		for (final Map.Entry<String, Integer> e : omitidos.entrySet()) {
			if (t.length() > 0) {
				t.append("; ");
			}
			t.append(e.getValue()).append(" - ").append(e.getKey());
		}
		return (t.toString());
	}
}

package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.Campana15MinAplicadoDAO;
import capaDAOCC.Campana15MinConfigDAO;
import capaDAOCC.Campana15MinExclusionDAO;
import capaDAOCC.Campana15MinHorarioDiaDAO;
import capaDAOCC.Campana15MinIncumplimientoDAO;
import capaDAOCC.Campana15MinTiendaDesactivadaDAO;

/**
 * Campana "15 minutos o gratis" en punto de venta: configuracion, horario por
 * dia de la semana, exclusiones, registro de aplicacion desde el POS, cola de
 * revision de incumplimientos, y visibilidad de tiendas que la desactivaron
 * por hoy.
 */
public class Campana15MinCtrl {

	/** La campana activa ahora mismo, con sus exclusiones, para el POS. Vacio si no hay ninguna. */
	public String obtenerCampanaActiva() {
		Campana15MinConfigDAO.Config config = Campana15MinConfigDAO.obtenerActivaAhora();
		JSONObject respuesta = new JSONObject();
		if (config == null) {
			respuesta.put("hayCampana", false);
			return (respuesta.toJSONString());
		}
		respuesta.put("hayCampana", true);
		respuesta.put("idcampana", config.idCampana);
		respuesta.put("nombre", config.nombre);
		respuesta.put("mensajeoperario", config.mensajeOperario);
		respuesta.put("mensajefactura", config.mensajeFactura);
		respuesta.put("minutospromesa", config.minutosPromesa);
		respuesta.put("porcentajeretencionmediovirtual", config.porcentajeRetencionMedioVirtual);
		JSONArray exclusiones = new JSONArray();
		for (Campana15MinExclusionDAO.Exclusion ex : Campana15MinExclusionDAO.obtenerPorCampana(config.idCampana)) {
			exclusiones.add(ex.nombreProducto);
		}
		respuesta.put("exclusiones", exclusiones);
		return (respuesta.toJSONString());
	}

	public String registrarAplicado(int idPedidoTienda, int idTienda, int idCampana, String fechaHoraInicio,
			double valorBasePizza, String nombreCliente, String celularCliente) {
		boolean ok = Campana15MinAplicadoDAO.registrar(idPedidoTienda, idTienda, idCampana, fechaHoraInicio,
				valorBasePizza, nombreCliente, celularCliente);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", ok ? "OK" : "NOK");
		return (respuesta.toJSONString());
	}

	public String registrarFormaPago(int idPedidoTienda, int idTienda, boolean esMedioVirtual) {
		boolean ok = Campana15MinAplicadoDAO.registrarFormaPago(idPedidoTienda, idTienda, esMedioVirtual);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", ok ? "OK" : "NOK");
		return (respuesta.toJSONString());
	}

	public String obtenerConfiguraciones() {
		JSONArray lista = new JSONArray();
		for (Campana15MinConfigDAO.Config c : Campana15MinConfigDAO.obtenerTodas()) {
			lista.add(configAJson(c));
		}
		return (lista.toJSONString());
	}

	private JSONObject configAJson(Campana15MinConfigDAO.Config c) {
		JSONObject fila = new JSONObject();
		fila.put("idcampana", c.idCampana);
		fila.put("nombre", c.nombre);
		fila.put("activo", c.activo ? "S" : "N");
		fila.put("mensajeoperario", c.mensajeOperario);
		fila.put("mensajefactura", c.mensajeFactura);
		fila.put("fechadesde", c.fechaDesde);
		fila.put("fechahasta", c.fechaHasta);
		fila.put("minutospromesa", c.minutosPromesa);
		fila.put("porcentajeretencionmediovirtual", c.porcentajeRetencionMedioVirtual);
		return (fila);
	}

	public String guardarConfiguracion(int idCampana, String nombre, boolean activo, String mensajeOperario,
			String mensajeFactura, String fechaDesde, String fechaHasta, int minutosPromesa,
			double porcentajeRetencion) {
		Campana15MinConfigDAO.Config c = new Campana15MinConfigDAO.Config();
		c.idCampana = idCampana;
		c.nombre = nombre == null || nombre.trim().equals("") ? "15 MINUTOS O GRATIS" : nombre.trim();
		c.activo = activo;
		c.mensajeOperario = mensajeOperario == null ? "" : mensajeOperario;
		c.mensajeFactura = mensajeFactura == null ? "" : mensajeFactura;
		c.fechaDesde = fechaDesde;
		c.fechaHasta = fechaHasta;
		c.minutosPromesa = minutosPromesa;
		c.porcentajeRetencionMedioVirtual = porcentajeRetencion;
		String resultado = Campana15MinConfigDAO.guardar(c);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "exitoso".equals(resultado) ? "OK" : "NOK");
		respuesta.put("idcampana", c.idCampana);
		return (respuesta.toJSONString());
	}

	public String obtenerExclusiones(int idCampana) {
		JSONArray lista = new JSONArray();
		for (Campana15MinExclusionDAO.Exclusion ex : Campana15MinExclusionDAO.obtenerPorCampana(idCampana)) {
			JSONObject fila = new JSONObject();
			fila.put("idexclusion", ex.idExclusion);
			fila.put("idcampana", ex.idCampana);
			fila.put("nombreproducto", ex.nombreProducto);
			lista.add(fila);
		}
		return (lista.toJSONString());
	}

	public String agregarExclusion(int idCampana, String nombreProducto) {
		int id = Campana15MinExclusionDAO.insertar(idCampana, nombreProducto);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", id > 0 ? "OK" : "NOK");
		respuesta.put("idexclusion", id);
		return (respuesta.toJSONString());
	}

	public String eliminarExclusion(int idExclusion) {
		Campana15MinExclusionDAO.eliminar(idExclusion);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "OK");
		return (respuesta.toJSONString());
	}

	/** Los 7 dias de horario de una campana, 1=lunes..7=domingo. */
	public String obtenerHorarioSemana(int idCampana) {
		JSONArray lista = new JSONArray();
		for (Campana15MinHorarioDiaDAO.HorarioDia h : Campana15MinHorarioDiaDAO.obtenerPorCampana(idCampana)) {
			JSONObject fila = new JSONObject();
			fila.put("diasemana", h.diaSemana);
			fila.put("activo", h.activo ? "S" : "N");
			fila.put("todoeldia", h.todoElDia ? "S" : "N");
			fila.put("horadesde", h.horaDesde);
			fila.put("horahasta", h.horaHasta);
			lista.add(fila);
		}
		return (lista.toJSONString());
	}

	/**
	 * Guarda los 7 dias de la semana de una sola vez.
	 *
	 * @param dias arreglo de objetos con diasemana, activo (S/N), todoeldia (S/N),
	 *             horadesde, horahasta
	 */
	public String guardarHorarioSemana(int idCampana, JSONArray dias) {
		ArrayList<Campana15MinHorarioDiaDAO.HorarioDia> lista = new ArrayList<>();
		for (Object o : dias) {
			JSONObject item = (JSONObject) o;
			Campana15MinHorarioDiaDAO.HorarioDia h = new Campana15MinHorarioDiaDAO.HorarioDia();
			h.idCampana = idCampana;
			h.diaSemana = ((Long) item.get("diasemana")).intValue();
			h.activo = "S".equals(item.get("activo"));
			h.todoElDia = "S".equals(item.get("todoeldia"));
			h.horaDesde = (String) item.get("horadesde");
			h.horaHasta = (String) item.get("horahasta");
			lista.add(h);
		}
		String resultado = Campana15MinHorarioDiaDAO.guardarSemana(idCampana, lista);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "exitoso".equals(resultado) ? "OK" : "NOK");
		return (respuesta.toJSONString());
	}

	/**
	 * El POS reporta, best-effort, que un administrador de tienda desactivo la
	 * campana por hoy. Solo para visibilidad central; el POS ya decidio y
	 * aplico la desactivacion localmente sin esperar esta respuesta.
	 */
	public String registrarTiendaDesactivada(int idTienda, String fecha, String motivo, String usuarioAutoriza,
			String usuarioDesactiva) {
		Campana15MinTiendaDesactivadaDAO.registrar(idTienda, fecha, motivo, usuarioAutoriza, usuarioDesactiva);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "OK");
		return (respuesta.toJSONString());
	}

	/** Tiendas desactivadas hoy, para la pantalla de configuracion. */
	public String obtenerTiendasDesactivadasHoy() {
		JSONArray lista = new JSONArray();
		for (Campana15MinTiendaDesactivadaDAO.Desactivacion d : Campana15MinTiendaDesactivadaDAO.obtenerDeHoy()) {
			JSONObject fila = new JSONObject();
			fila.put("idtienda", d.idTienda);
			fila.put("tienda", d.nombreTienda);
			fila.put("motivo", d.motivo);
			fila.put("usuarioautoriza", d.usuarioAutoriza);
			fila.put("usuariodesactiva", d.usuarioDesactiva);
			fila.put("fechahora", d.fechaHora);
			lista.add(fila);
		}
		return (lista.toJSONString());
	}

	/** Cola de incumplimientos para la pantalla de revision. */
	public String obtenerIncumplimientos(String estado, String fechaDesde, String fechaHasta) {
		JSONArray lista = new JSONArray();
		for (Campana15MinIncumplimientoDAO.Incumplimiento i : Campana15MinIncumplimientoDAO.obtener(estado,
				fechaDesde, fechaHasta)) {
			JSONObject fila = new JSONObject();
			fila.put("idsolicitud", i.idSolicitud);
			fila.put("idpedidotienda", i.idPedidoTienda);
			fila.put("idtienda", i.idTienda);
			fila.put("tienda", i.nombreTienda);
			fila.put("fechahorainicio", i.fechaHoraInicio);
			fila.put("fechadeteccion", i.fechaDeteccion);
			fila.put("valorbasepizza", i.valorBasePizza);
			fila.put("retencionaplicada", i.retencionAplicada);
			fila.put("valoradevolver", i.valorADevolver);
			fila.put("estado", i.estado);
			fila.put("usuariorevisa", i.usuarioRevisa);
			fila.put("fecharevision", i.fechaRevision);
			fila.put("observacionrevision", i.observacionRevision);
			lista.add(fila);
		}
		return (lista.toJSONString());
	}

	/** Aprueba o rechaza un incumplimiento. Nunca mueve dinero, solo deja el caso calculado. */
	public String resolverIncumplimiento(int idSolicitud, boolean aprobar, String usuario, String observacion) {
		String resultado = Campana15MinIncumplimientoDAO.resolver(idSolicitud, aprobar, usuario, observacion);
		JSONObject respuesta = new JSONObject();
		respuesta.put("respuesta", "OK".equals(resultado) ? "OK" : "NOK");
		respuesta.put("detalle", "OK".equals(resultado) ? "" : resultado);
		return (respuesta.toJSONString());
	}
}

package capaControladorCC;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.AuditoriaFidelizacionDAO;
import capaDAOCC.UsuarioRolDAO;

/**
 * Arma lo que ve la pantalla de auditoria de fidelizacion.
 *
 * Dos vistas:
 *
 *   SEMANA  -> las redenciones de un rango, con el resumen de arriba.
 *   CLIENTE -> todo el historial de una persona: totales, quien le atiende,
 *              acumulaciones y redenciones.
 *
 * QUIEN PUEDE VERLA
 *
 * El rol "Auditoria de Fidelizacion" o "Superadministrador". Se revisa aqui y
 * leyendo los roles DE LA BASE, no de los que quedaron guardados en la sesion
 * al ingresar. Son dos cosas distintas y la diferencia ya costo un incidente:
 * un usuario al que se le cambio el rol siguio con el anterior hasta que
 * cerrara sesion, porque el objeto Usuario de la sesion se congela al entrar.
 *
 * Y no se delega en SeguridadFilter: en produccion
 * general.parametros.SEGURIDADFILTROMODO esta en LOG, o sea que audita pero
 * deja pasar todo. Esconder la opcion del menu no protege nada.
 */
public class AuditoriaFidelizacionCtrl {

	private static final String ROL_AUDITORIA = "Auditoria de Fidelizacion";
	private static final String ROL_SUPER = "Superadministrador";

	private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/** ¿Este usuario puede ver la pantalla? */
	public static boolean puedeVer(final int idUsuario) {
		if (idUsuario <= 0) {
			return (false);
		}
		final ArrayList<String> roles = UsuarioRolDAO.listarNombresRolPorUsuario(idUsuario);
		if (roles == null) {
			return (false);
		}
		for (int i = 0; i < roles.size(); i++) {
			final String rol = roles.get(i);
			if (ROL_AUDITORIA.equalsIgnoreCase(rol) || ROL_SUPER.equalsIgnoreCase(rol)) {
				return (true);
			}
		}
		return (false);
	}

	/**
	 * La semana pasada, de LUNES A DOMINGO.
	 *
	 * Es el corte de Pizza Americana y el mismo de los reportes semanales de
	 * Servicios: una semana que empiece en otro dia no se puede comparar
	 * contra nada de lo que ya se mira.
	 *
	 * @return {desde, hasta} en yyyy-MM-dd
	 */
	public static String[] semanaPasada() {
		final LocalDate hoy = LocalDate.now();
		final LocalDate lunesDeEstaSemana = hoy.with(DayOfWeek.MONDAY);
		final LocalDate lunes = lunesDeEstaSemana.minusDays(7);
		final LocalDate domingo = lunesDeEstaSemana.minusDays(1);
		return (new String[] {lunes.format(ISO), domingo.format(ISO)});
	}

	/** El listado de redenciones de un rango, con su resumen. */
	@SuppressWarnings("unchecked")
	public static JSONObject consultarSemana(final String fechaDesde, final String fechaHasta,
			final int idTienda, final String soloSenal) {

		final JSONObject respuesta = new JSONObject();
		final ArrayList<AuditoriaFidelizacionDAO.Redencion> redenciones =
				AuditoriaFidelizacionDAO.listarRedenciones(fechaDesde, fechaHasta, idTienda, soloSenal);

		double puntos = 0;
		int deEmpleados = 0;
		int autoRedenciones = 0;
		int reversadas = 0;
		int sinCajero = 0;
		final java.util.HashSet<String> personas = new java.util.HashSet<String>();
		final JSONArray filas = new JSONArray();

		for (int i = 0; i < redenciones.size(); i++) {
			final AuditoriaFidelizacionDAO.Redencion r = redenciones.get(i);
			if (!"REVERSADA".equals(r.estado)) {
				puntos = puntos + r.puntos;
			} else {
				reversadas++;
			}
			if (r.clienteEsEmpleado) {
				deEmpleados++;
			}
			if (r.autoRedencion) {
				autoRedenciones++;
			}
			if (r.usuario == null || r.usuario.trim().length() == 0) {
				sinCajero++;
			}
			personas.add(r.correo.toLowerCase());
			filas.add(jsonRedencion(r));
		}

		final JSONObject totales = new JSONObject();
		totales.put("redenciones", Integer.valueOf(redenciones.size()));
		totales.put("personas", Integer.valueOf(personas.size()));
		totales.put("puntos", Double.valueOf(puntos));
		totales.put("de_empleados", Integer.valueOf(deEmpleados));
		totales.put("auto_redenciones", Integer.valueOf(autoRedenciones));
		totales.put("reversadas", Integer.valueOf(reversadas));
		totales.put("sin_cajero", Integer.valueOf(sinCajero));

		respuesta.put("resultado", "OK");
		respuesta.put("desde", fechaDesde);
		respuesta.put("hasta", fechaHasta);
		respuesta.put("totales", totales);
		respuesta.put("redenciones", filas);
		return (respuesta);
	}

	/** Todo el historial de una persona. */
	@SuppressWarnings("unchecked")
	public static JSONObject consultarCliente(final String correo) {
		final JSONObject respuesta = new JSONObject();

		final AuditoriaFidelizacionDAO.ResumenCliente res =
				AuditoriaFidelizacionDAO.resumenCliente(correo);
		final ArrayList<AuditoriaFidelizacionDAO.Atencion> atienden =
				AuditoriaFidelizacionDAO.quienLoAtiende(correo);
		final ArrayList<AuditoriaFidelizacionDAO.Acumulacion> acumulaciones =
				AuditoriaFidelizacionDAO.acumulacionesCliente(correo, 300);
		final ArrayList<AuditoriaFidelizacionDAO.Redencion> redenciones =
				AuditoriaFidelizacionDAO.redencionesCliente(correo, 200);

		final JSONObject resumen = new JSONObject();
		resumen.put("correo", res.correo);
		resumen.put("nombre", res.nombre);
		resumen.put("celular", res.celular);
		resumen.put("vinculado", res.vinculado);
		resumen.put("activo", Boolean.valueOf(res.activo));
		resumen.put("puntos_vigentes", Double.valueOf(res.puntosVigentes));
		resumen.put("acumulaciones", Integer.valueOf(res.acumulaciones));
		resumen.put("puntos_acumulados", Double.valueOf(res.puntosAcumulados));
		resumen.put("valor_acumulado", Double.valueOf(res.valorAcumulado));
		resumen.put("redenciones", Integer.valueOf(res.redenciones));
		resumen.put("puntos_redimidos", Double.valueOf(res.puntosRedimidos));
		resumen.put("redenciones_reversadas", Integer.valueOf(res.redencionesReversadas));
		resumen.put("acumulaciones_con_usuario", Integer.valueOf(res.acumulacionesConUsuario));
		resumen.put("tiendas_distintas", Integer.valueOf(res.tiendasDistintas));
		resumen.put("es_empleado", Boolean.valueOf(res.esEmpleado));
		resumen.put("empleado_nombre", res.empleadoNombre);
		resumen.put("empleado_cedula", res.empleadoCedula);
		resumen.put("empleado_activo", Boolean.valueOf(res.empleadoActivo));
		resumen.put("auto_redenciones", Integer.valueOf(res.autoRedenciones));

		//La concentracion: que tanto de la atencion se la lleva una sola persona.
		//El denominador son las acumulaciones CON usuario, no todas: las de antes
		//de septiembre de 2026 no lo traen y diluirian el porcentaje.
		//El denominador son solo los pedidos atendidos por una PERSONA. Un cliente
		//que siempre pide por el contact center tendria 100% sobre CONTACT, que no
		//senala a nadie. Los canales se muestran igual, pero aparte.
		final JSONArray jsonAtienden = new JSONArray();
		int conPersona = 0;
		int porCanal = 0;
		for (int i = 0; i < atienden.size(); i++) {
			final AuditoriaFidelizacionDAO.Atencion a = atienden.get(i);
			if (a.esCanal) {
				porCanal = porCanal + a.pedidos;
			} else {
				conPersona = conPersona + a.pedidos;
			}
		}
		double concentracion = 0;
		String quienConcentra = "";
		for (int i = 0; i < atienden.size(); i++) {
			final AuditoriaFidelizacionDAO.Atencion a = atienden.get(i);
			final double pct = (!a.esCanal && conPersona > 0) ? (100.0 * a.pedidos / conPersona) : 0;
			final JSONObject o = new JSONObject();
			o.put("usuario", a.usuario);
			o.put("nombre", a.nombre.length() > 0 ? a.nombre : "(no resuelve)");
			o.put("pedidos", Integer.valueOf(a.pedidos));
			o.put("tiendas", Integer.valueOf(a.tiendas));
			o.put("porcentaje", Double.valueOf(redondear(pct)));
			o.put("es_el_mismo_cliente", Boolean.valueOf(a.esElMismoCliente));
			o.put("es_canal", Boolean.valueOf(a.esCanal));
			jsonAtienden.add(o);
			if (!a.esCanal && pct > concentracion) {
				concentracion = pct;
				quienConcentra = a.nombre.length() > 0 ? a.nombre : a.usuario;
			}
		}
		final int conUsuario = conPersona;

		final JSONObject senales = new JSONObject();
		senales.put("concentracion", Double.valueOf(redondear(concentracion)));
		senales.put("quien_concentra", quienConcentra);
		senales.put("base_concentracion", Integer.valueOf(conUsuario));
		senales.put("por_canal", Integer.valueOf(porCanal));
		senales.put("cajeros_distintos", Integer.valueOf(atienden.size()));
		resumen.put("senales", senales);

		final JSONArray jsonAcum = new JSONArray();
		for (int i = 0; i < acumulaciones.size(); i++) {
			final AuditoriaFidelizacionDAO.Acumulacion a = acumulaciones.get(i);
			final JSONObject o = new JSONObject();
			o.put("fecha", a.fecha);
			o.put("idtienda", Integer.valueOf(a.idTienda));
			o.put("tienda", a.tienda);
			o.put("idpedidotienda", Integer.valueOf(a.idPedidoTienda));
			o.put("valor_neto", Double.valueOf(a.valorNeto));
			o.put("puntos", Double.valueOf(a.puntos));
			o.put("puntos_redimidos", Double.valueOf(a.puntosRedimidos));
			o.put("usuario", a.usuario);
			o.put("usuario_nombre", a.usuarioNombre);
			o.put("vencidos", Boolean.valueOf(a.vencidos));
			jsonAcum.add(o);
		}

		final JSONArray jsonRed = new JSONArray();
		for (int i = 0; i < redenciones.size(); i++) {
			jsonRed.add(jsonRedencion(redenciones.get(i)));
		}

		respuesta.put("resultado", "OK");
		respuesta.put("resumen", resumen);
		respuesta.put("atienden", jsonAtienden);
		respuesta.put("acumulaciones", jsonAcum);
		respuesta.put("redenciones", jsonRed);
		return (respuesta);
	}

	/** Desde que porcentaje de concentracion se marca como sospechoso. */
	private static final double UMBRAL_CONCENTRACION = 70;

	/** Y con menos pedidos que esto el porcentaje es ruido: 3 de 3 da 100% y no dice nada. */
	private static final int MINIMO_PARA_JUZGAR = 5;

	/**
	 * Quien mas acumula puntos en el periodo, con las senales.
	 *
	 * LA REGLA DE SOSPECHA, dicha de frente porque de esto salen conversaciones
	 * con gente:
	 *
	 *   - Una sola PERSONA concentra 70% o mas de los pedidos que le dieron
	 *     puntos, y son al menos 5 pedidos.
	 *   - O la cuenta es de un empleado y ademas el se auto-redimio.
	 *
	 * Lo que NO cuenta: comprar mucho. Un cliente fiel compra mucho y eso es
	 * exactamente lo que queremos. Por eso el orden por puntos es solo el orden
	 * de la lista, no la senal.
	 *
	 * El porcentaje se calcula sobre los pedidos atendidos por una persona, no
	 * sobre todos: los canales (CONTACT, BOT, APP, la tienda virtual) no son
	 * nadie a quien se pueda senalar.
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject consultarAcumuladores(final String fechaDesde, final String fechaHasta,
			final int idTienda, final int minimoPedidos) {

		final JSONObject respuesta = new JSONObject();
		final ArrayList<AuditoriaFidelizacionDAO.Acumulador> top =
				AuditoriaFidelizacionDAO.topAcumuladores(fechaDesde, fechaHasta, idTienda,
						minimoPedidos > 0 ? minimoPedidos : 3, 300);

		final JSONArray filas = new JSONArray();
		int sospechosos = 0;
		int deEmpleados = 0;
		double puntosTotal = 0;

		for (int i = 0; i < top.size(); i++) {
			final AuditoriaFidelizacionDAO.Acumulador a = top.get(i);
			final double pct = a.pedidosConPersona > 0
					? (100.0 * a.topPedidos / a.pedidosConPersona) : 0;
			final boolean concentrado = pct >= UMBRAL_CONCENTRACION
					&& a.pedidosConPersona >= MINIMO_PARA_JUZGAR;
			final boolean empleadoQueSeAutoRedime = a.esEmpleado && a.autoRedenciones > 0;
			final boolean sospechoso = concentrado || empleadoQueSeAutoRedime;

			if (sospechoso) {
				sospechosos++;
			}
			if (a.esEmpleado) {
				deEmpleados++;
			}
			puntosTotal = puntosTotal + a.puntos;

			final JSONObject o = new JSONObject();
			o.put("correo", a.correo);
			o.put("nombre", a.nombre);
			o.put("pedidos", Integer.valueOf(a.pedidos));
			o.put("puntos", Double.valueOf(a.puntos));
			o.put("valor", Double.valueOf(a.valor));
			o.put("tiendas", Integer.valueOf(a.tiendas));
			o.put("cajeros", Integer.valueOf(a.cajerosDistintos));
			o.put("pedidos_con_persona", Integer.valueOf(a.pedidosConPersona));
			o.put("top_usuario", a.topUsuario);
			o.put("top_nombre", a.topNombre.length() > 0 ? a.topNombre : a.topUsuario);
			o.put("top_pedidos", Integer.valueOf(a.topPedidos));
			o.put("concentracion", Double.valueOf(redondear(pct)));
			o.put("es_empleado", Boolean.valueOf(a.esEmpleado));
			o.put("empleado_nombre", a.empleadoNombre);
			o.put("auto_redenciones", Integer.valueOf(a.autoRedenciones));
			o.put("sospechoso", Boolean.valueOf(sospechoso));
			o.put("motivo", motivo(concentrado, empleadoQueSeAutoRedime, pct, a));
			filas.add(o);
		}

		final JSONObject totales = new JSONObject();
		totales.put("personas", Integer.valueOf(top.size()));
		totales.put("puntos", Double.valueOf(puntosTotal));
		totales.put("sospechosos", Integer.valueOf(sospechosos));
		totales.put("de_empleados", Integer.valueOf(deEmpleados));
		totales.put("umbral", Double.valueOf(UMBRAL_CONCENTRACION));
		totales.put("minimo_pedidos", Integer.valueOf(MINIMO_PARA_JUZGAR));

		respuesta.put("resultado", "OK");
		respuesta.put("desde", fechaDesde);
		respuesta.put("hasta", fechaHasta);
		respuesta.put("totales", totales);
		respuesta.put("acumuladores", filas);
		return (respuesta);
	}

	/** Por que quedo marcado. Se escribe en texto para que el reporte sea explicable. */
	private static String motivo(final boolean concentrado, final boolean autoRedime,
			final double pct, final AuditoriaFidelizacionDAO.Acumulador a) {

		final StringBuilder sb = new StringBuilder();
		if (concentrado) {
			sb.append(redondear(pct)).append("% de sus pedidos con puntos (")
			  .append(a.topPedidos).append(" de ").append(a.pedidosConPersona)
			  .append(") los tomo ")
			  .append(a.topNombre.length() > 0 ? a.topNombre : a.topUsuario).append(".");
		}
		if (autoRedime) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append("La cuenta es de ").append(a.empleadoNombre)
			  .append(", que se proceso ").append(a.autoRedenciones)
			  .append(" redencion(es) el mismo.");
		}
		return (sb.toString());
	}

	@SuppressWarnings("unchecked")
	private static JSONObject jsonRedencion(final AuditoriaFidelizacionDAO.Redencion r) {
		final JSONObject o = new JSONObject();
		o.put("idredencion", Integer.valueOf(r.idRedencion));
		o.put("fecha", r.fecha);
		o.put("correo", r.correo);
		o.put("puntos", Double.valueOf(r.puntos));
		o.put("idtienda", Integer.valueOf(r.idTienda));
		o.put("tienda", r.tienda);
		o.put("idpedidotienda", Integer.valueOf(r.idPedidoTienda));
		o.put("usuario", r.usuario);
		o.put("usuario_nombre", r.usuarioNombre);
		o.put("origen", r.origen);
		o.put("estado", r.estado);
		o.put("cliente_es_empleado", Boolean.valueOf(r.clienteEsEmpleado));
		o.put("cliente_empleado_nombre", r.clienteEmpleadoNombre);
		o.put("auto_redencion", Boolean.valueOf(r.autoRedencion));
		return (o);
	}

	private static double redondear(final double valor) {
		return (Math.round(valor * 10.0) / 10.0);
	}
}

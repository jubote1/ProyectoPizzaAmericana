package capaControladorCC;

import java.time.LocalDate;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.TableroPuntosDAO;

/**
 * Tablero del plan de puntos. Une el DAO con las reglas de presentacion:
 * rangos por defecto, topes y el CSV de la lista de puntos por vencer.
 *
 * Quien puede verlo lo decide AuditoriaFidelizacionCtrl.puedeVer: mismo rol
 * que la auditoria, porque el tablero muestra nombres de clientes y
 * rankings de empleados, que es lo mismo que esa pantalla protege.
 */
public class TableroPuntosCtrl {

	/** Clientes que se consideran "activos": acumularon puntos en los ultimos N dias. */
	public static final int DIAS_ACTIVO_POR_DEFECTO = 90;

	/** Rol que da acceso solo al tablero, sin la auditoria. */
	private static final String ROL_TABLERO = "Tablero de Puntos";

	/**
	 * Quien puede ver el tablero: el rol propio del tablero, o los de la auditoria
	 * (Auditoria de Fidelizacion y Superadministrador).
	 */
	public static boolean puedeVer(final int idUsuario) {
		if (AuditoriaFidelizacionCtrl.puedeVer(idUsuario)) {
			return (true);
		}
		final java.util.ArrayList<String> roles = capaDAOCC.UsuarioRolDAO.listarNombresRolPorUsuario(idUsuario);
		if (roles == null) {
			return (false);
		}
		for (int i = 0; i < roles.size(); i++) {
			if (ROL_TABLERO.equalsIgnoreCase(roles.get(i))) {
				return (true);
			}
		}
		return (false);
	}

	/**
	 * Los dos rankings de EMPLEADOS (quien redime y quien acumula) senalan personas de la
	 * nomina, que es lo que la auditoria protege. El rol del tablero NO los trae: solo la
	 * auditoria y el Superadministrador.
	 */
	public static boolean puedeVerEmpleados(final int idUsuario) {
		return (AuditoriaFidelizacionCtrl.puedeVer(idUsuario));
	}

	/** Los ultimos 30 dias, hoy incluido. */
	public static String[] ultimos30Dias() {
		final LocalDate hoy = LocalDate.now();
		return (new String[] { hoy.minusDays(29).toString(), hoy.toString() });
	}

	public static JSONObject resumen(final int diasActivo, final String desde, final String hasta) {
		final JSONObject r = TableroPuntosDAO.resumen(diasActivo > 0 ? diasActivo : DIAS_ACTIVO_POR_DEFECTO, desde, hasta);
		r.put("desde", desde);
		r.put("hasta", hasta);
		r.put("dias_activo", Integer.valueOf(diasActivo > 0 ? diasActivo : DIAS_ACTIVO_POR_DEFECTO));
		return (r);
	}

	public static JSONObject ranking(final String tipo, final String desde, final String hasta,
			final int idTienda, final int limite) {
		final JSONObject r = TableroPuntosDAO.ranking(tipo, desde, hasta, idTienda, limite > 0 ? limite : 50);
		r.put("tipo", tipo);
		r.put("desde", desde);
		r.put("hasta", hasta);
		return (r);
	}

	public static JSONObject porVencer(final int dias, final int idTienda, final int limite) {
		final JSONObject r = TableroPuntosDAO.porVencer(dias, idTienda, limite > 0 ? limite : 100);
		r.put("dias", Integer.valueOf(dias));
		return (r);
	}

	/**
	 * La lista de puntos por vencer como CSV, para usarla en un aviso.
	 * Separador punto y coma y BOM UTF-8, igual que la descarga de segmentacion:
	 * asi Excel en espanol la abre bien, con las tildes.
	 */
	public static String csvPorVencer(final int dias, final int idTienda) {
		final JSONObject r = TableroPuntosDAO.porVencer(dias, idTienda, TableroPuntosDAO.TOPE_FILAS * 20);
		final StringBuilder sb = new StringBuilder();
		sb.append('﻿');
		sb.append("nombre;correo;celular;puntos_por_vencer;proximo_vencimiento;saldo_total;es_empleado\n");
		if (!"OK".equals(r.get("resultado"))) {
			return (sb.toString());
		}
		final JSONArray filas = (JSONArray) r.get("filas");
		for (int i = 0; i < filas.size(); i++) {
			final JSONObject o = (JSONObject) filas.get(i);
			sb.append(celda(o.get("nombre"))).append(';')
			  .append(celda(o.get("correo"))).append(';')
			  .append(celda(o.get("celular"))).append(';')
			  .append(numero(o.get("por_vencer"))).append(';')
			  .append(celda(o.get("proxima"))).append(';')
			  .append(numero(o.get("saldo"))).append(';')
			  .append(celda(o.get("es_empleado"))).append('\n');
		}
		return (sb.toString());
	}

	/** Una celda de texto: sin saltos de linea ni separadores que rompan la fila. */
	private static String celda(final Object v) {
		return (v == null ? "" : String.valueOf(v).replace(';', ',').replace('\n', ' ').replace('\r', ' ').trim());
	}

	private static String numero(final Object v) {
		if (v instanceof Number) {
			return (String.valueOf(Math.round(((Number) v).doubleValue())));
		}
		return ("0");
	}
}

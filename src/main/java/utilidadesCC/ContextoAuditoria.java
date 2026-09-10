package utilidadesCC;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Dice de donde viene un cambio, para que el log de la base lo pueda registrar.
 *
 * El problema: la ficha del cliente la escriben once metodos distintos del DAO, y
 * los llaman el CRM, el POS, el bot y los procesos por lote. Pasar el origen y el
 * usuario por parametro habria significado tocar once firmas y todos sus llamados,
 * uno de ellos con veintiseis parametros.
 *
 * La solucion: el servlet -o el proceso- deja el origen y el usuario en el hilo
 * que atiende la peticion, y el DAO los pone en la conexion como variables de
 * sesion de MySQL justo antes de escribir. El trigger de la tabla las lee.
 *
 * Uso desde un servlet:
 *
 *   try {
 *       ContextoAuditoria.fijar("CRM", usuario.getNombreUsuario());
 *       ... la operacion ...
 *   } finally {
 *       ContextoAuditoria.limpiar();
 *   }
 *
 * El limpiar en finally NO es opcional: Tomcat reutiliza los hilos, y si el valor
 * se queda pegado el siguiente cambio que pase por ese hilo se atribuye a quien no
 * fue. Por eso tambien el valor por defecto es DESCONOCIDO y no el ultimo que se
 * haya visto.
 */
public class ContextoAuditoria {

	/** Cuando nadie dijo de donde viene el cambio. Se registra igual. */
	public static final String ORIGEN_DESCONOCIDO = "DESCONOCIDO";

	private static final ThreadLocal<String> ORIGEN = new ThreadLocal<String>();
	private static final ThreadLocal<String> USUARIO = new ThreadLocal<String>();

	/** Solo estatico. */
	private ContextoAuditoria() {
	}

	/**
	 * Deja el origen y el usuario en este hilo.
	 *
	 * @param origen  CRM, POS, BOT, SERVICIO... corto, que se lea en el informe
	 * @param usuario el usuario de la aplicacion, no el de la base de datos
	 */
	public static void fijar(String origen, String usuario) {
		ContextoAuditoria.ORIGEN.set(ContextoAuditoria.recortar(origen, 30));
		ContextoAuditoria.USUARIO.set(ContextoAuditoria.recortar(usuario, 50));
	}

	/** Suelta el hilo. Va siempre en un finally. */
	public static void limpiar() {
		ContextoAuditoria.ORIGEN.remove();
		ContextoAuditoria.USUARIO.remove();
	}

	public static String origen() {
		String valor = ContextoAuditoria.ORIGEN.get();
		if (valor == null || valor.length() == 0) {
			return (ContextoAuditoria.ORIGEN_DESCONOCIDO);
		}
		return (valor);
	}

	public static String usuario() {
		String valor = ContextoAuditoria.USUARIO.get();
		return ((valor == null) ? "" : valor);
	}

	/**
	 * Pone las variables de sesion en esta conexion, para que el trigger las vea.
	 *
	 * Va justo despues de abrir la conexion y antes de escribir. Se hace con
	 * PreparedStatement no por los parametros -MySQL no acepta variables de usuario
	 * como parametros- sino escapando a mano lo poco que puede venir de afuera.
	 *
	 * Si falla no se propaga la excepcion: perder la atribucion de un cambio es
	 * malo, pero no escribir el cambio del cliente por eso seria peor. El trigger
	 * de todas formas lo registra como DESCONOCIDO.
	 */
	public static void marcar(Connection con) {
		if (con == null) {
			return;
		}
		String instruccion = "SET @pa_origen = '" + ContextoAuditoria.escapar(ContextoAuditoria.origen())
				+ "', @pa_usuario = '" + ContextoAuditoria.escapar(ContextoAuditoria.usuario()) + "'";
		try (Statement stm = con.createStatement()) {
			stm.execute(instruccion);
		} catch (Exception e) {
			System.out.println("ContextoAuditoria.marcar: " + e.toString());
		}
	}

	/**
	 * Deja el texto en algo que no puede romper la instruccion.
	 *
	 * El origen lo pone el programa y el usuario viene de la sesion, asi que no es
	 * texto libre del publico; de todas formas se limita a letras, numeros y unos
	 * pocos signos, que es todo lo que un nombre de usuario necesita.
	 */
	private static String escapar(String valor) {
		if (valor == null) {
			return ("");
		}
		StringBuilder limpio = new StringBuilder();
		for (int i = 0; i < valor.length(); i++) {
			char c = valor.charAt(i);
			boolean permitido = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
					|| c == '.' || c == '_' || c == '-' || c == '@' || c == ' ';
			if (permitido) {
				limpio.append(c);
			}
		}
		return (limpio.toString());
	}

	private static String recortar(String valor, int largo) {
		if (valor == null) {
			return ("");
		}
		String limpio = valor.trim();
		return ((limpio.length() > largo) ? limpio.substring(0, largo) : limpio);
	}
}

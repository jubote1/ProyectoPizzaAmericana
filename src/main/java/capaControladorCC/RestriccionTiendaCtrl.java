package capaControladorCC;

import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import capaDAOCC.TiendaAdministradorDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;
import capaModeloCC.Usuario;

/**
 * Quien puede ver que tienda en los reportes.
 *
 * El administrador de punto de venta ve SOLO su tienda. Todos los demas
 * -gerencia, mercadeo, operaciones, sistemas- siguen viendo las once.
 *
 * Se aplica cuando "Administrador de Tienda" es el UNICO rol del usuario. Si
 * alguien tiene ademas otro rol es porque hace otra cosa en la empresa, y
 * quitarle las tiendas por tener tambien este rol seria quitarle trabajo.
 *
 * La tienda se deduce de las horas de la semana pasada (TiendaAdministradorDAO).
 * Tres resultados posibles:
 *
 *   SIN_RESTRICCION  ve las once (no es administrador, o esta marcado supernumerario)
 *   UNA_TIENDA       ve solo esa
 *   SIN_TIENDA       no ve ninguna, porque no hay horas suyas de donde deducirla
 *
 * SIN_TIENDA es a proposito: ante la duda NO se abre. Hoy le toca a los dos
 * administradores que entraron el 2026-10-28 y todavia no tienen horas
 * registradas; en cuanto trabajen una semana les sale su tienda sola. Si se
 * necesita que vean algo antes, se les marca general.empleado.supernumerario = 1.
 *
 * El resultado se guarda en la sesion: la tienda cambia una vez por semana, no
 * tiene sentido consultarla en cada clic del reporte.
 */
public class RestriccionTiendaCtrl {

	/** El rol que se restringe. Debe ser el unico del usuario para que aplique. */
	private static final String ROL_ADMINISTRADOR_TIENDA = "Administrador de Tienda";

	/** Donde se guarda el resultado dentro de la sesion. */
	private static final String ATRIBUTO = "restriccionTienda";

	/** Nombres de tienda, para no ir a la base solo por un rotulo. */
	private static final ConcurrentHashMap<Integer, String> NOMBRES = new ConcurrentHashMap<Integer, String>();

	public static class Restriccion {
		/** false = ve las once tiendas. */
		public boolean restringido = false;
		/** La unica tienda que puede ver. 0 cuando restringido es true significa que no puede ver ninguna. */
		public int idTienda = 0;
		public String tienda = "";
		/** De donde salio la tienda: SEMANA_PASADA, ULTIMO_MES, ROTA o SIN_DATOS. */
		public String origen = "";

		/** true cuando es administrador y no se le pudo deducir tienda: no debe ver nada. */
		public boolean sinTienda() {
			return (this.restringido && this.idTienda <= 0);
		}
	}

	/** La restriccion del usuario de esta sesion. Nunca devuelve null. */
	public static Restriccion de(final HttpServletRequest request) {
		final HttpSession sesion = (request == null ? null : request.getSession(false));
		if (sesion == null) {
			return (new Restriccion());
		}
		final Object guardada = sesion.getAttribute(ATRIBUTO);
		if (guardada instanceof Restriccion) {
			return ((Restriccion) guardada);
		}
		final Object objUsuario = sesion.getAttribute("usuario");
		final Restriccion r = calcular(objUsuario instanceof Usuario ? (Usuario) objUsuario : null);
		sesion.setAttribute(ATRIBUTO, r);
		return (r);
	}

	/**
	 * La tienda a la que se le debe obligar la consulta, o el idtienda que pidio la
	 * pantalla si el usuario no esta restringido.
	 *
	 * Devuelve -1 cuando el usuario es administrador sin tienda deducida: quien llama
	 * tiene que cortar y explicar, no consultar.
	 */
	public static int tiendaForzada(final HttpServletRequest request, final int idTiendaPedida) {
		final Restriccion r = de(request);
		if (!r.restringido) {
			return (idTiendaPedida);
		}
		if (r.sinTienda()) {
			return (-1);
		}
		return (r.idTienda);
	}

	/** El mensaje que ve el administrador al que no se le pudo deducir tienda. */
	public static String mensajeSinTienda() {
		return ("{\"error\":\"Todavia no podemos saber de cual punto de venta es usted: no hay horas suyas "
				+ "registradas la semana pasada. Avisele a Gestion Humana o a Sistemas.\"}");
	}

	@SuppressWarnings("unchecked")
	public static String aJson(final Restriccion r) {
		final JSONObject o = new JSONObject();
		o.put("restringido", Boolean.valueOf(r.restringido));
		o.put("idtienda", Integer.valueOf(r.idTienda));
		o.put("tienda", r.tienda);
		o.put("origen", r.origen);
		return (o.toJSONString());
	}

	private static Restriccion calcular(final Usuario usuario) {
		final Restriccion r = new Restriccion();
		if (usuario == null || usuario.getRoles() == null) {
			return (r);
		}
		if (usuario.getRoles().size() != 1 || !ROL_ADMINISTRADOR_TIENDA.equals(usuario.getRoles().get(0))) {
			return (r);
		}
		final TiendaAdministradorDAO.Asignacion a = TiendaAdministradorDAO.asignacionDe(usuario.getId());
		r.origen = a.origen;
		if (a.rota) {
			//Supernumerario: rota entre puntos de venta, no hay una tienda suya.
			return (r);
		}
		r.restringido = true;
		r.idTienda = a.idTienda;
		r.tienda = (a.idTienda > 0 ? nombreDe(a.idTienda) : "");
		Logger.getLogger("log_file").info("restriccion de tienda: usuario " + usuario.getNombreUsuario()
				+ " -> tienda " + a.idTienda + " (" + a.origen + ")");
		return (r);
	}

	private static String nombreDe(final int idTienda) {
		final String enCache = NOMBRES.get(Integer.valueOf(idTienda));
		if (enCache != null) {
			return (enCache);
		}
		try {
			for (final Tienda t : TiendaDAO.obtenerTiendas()) {
				NOMBRES.put(Integer.valueOf(t.getIdTienda()), t.getNombreTienda());
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("No se pudo leer el nombre de la tienda " + idTienda);
		}
		final String nombre = NOMBRES.get(Integer.valueOf(idTienda));
		return (nombre == null ? "" : nombre);
	}
}

package utilidadesCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import capaDAOCC.FidelizacionTransaccionDAO;
import capaDAOCC.ParametrosDAO;
import capaModeloCC.Correo;
import conexionCC.ConexionBaseDatos;

/**
 * Le avisa al cliente que redimio puntos.
 *
 * Reune todo lo que hay que saber -el nombre, el saldo, el proximo
 * vencimiento-, arma el correo y lo manda. Quien redime solo tiene que decir
 * quien, cuantos puntos y donde.
 *
 * DOS FORMAS DE MANDARLO, Y NO ES UN DETALLE
 *
 * {@link #enviarAhora} espera a que el correo salga y devuelve si salio. Lo usa
 * la redencion manual: la hace una persona sentada frente a la pantalla, que
 * puede esperar dos segundos y que TIENE que enterarse si el correo no salio,
 * porque entonces le toca avisarle al cliente por otro lado.
 *
 * {@link #enviarLuego} devuelve de inmediato y manda el correo aparte. Lo usan
 * las redenciones normales, las del POS y el contact center. Ahi hay un cliente
 * en el mostrador esperando su pedido: hacerlo esperar a que un servidor SMTP
 * conteste -con reintentos- convertiria una cortesia en una demora en la caja.
 * Y ademas el cajero no podria hacer nada con esa falla.
 *
 * EL POOL ES FIJO Y LA COLA ES ACOTADA
 *
 * Un hilo nuevo por cada redencion es como se tumba un servidor en hora pico.
 * Son dos hilos y una cola de doscientos; si se llena, el correo se descarta y
 * queda en el log. Preferible perder un aviso de cortesia que dejar sin
 * responder a las tiendas.
 */
public final class AvisoRedencion {

	/** Dos hilos alcanzan de sobra: esto manda correos, no calcula nada. */
	private static final int HILOS = 2;

	/** Si se acumulan mas que esto es que el SMTP no esta respondiendo. */
	private static final int COLA = 200;

	private static final ThreadPoolExecutor CORREOS = crearPool();

	private AvisoRedencion() {
	}

	private static ThreadPoolExecutor crearPool() {
		final ThreadFactory fabrica = new ThreadFactory() {
			public Thread newThread(final Runnable tarea) {
				final Thread hilo = new Thread(tarea, "aviso-redencion");
				//Demonio: si el servidor se esta bajando, un correo pendiente no
				//puede ser la razon por la que no termina de apagarse.
				hilo.setDaemon(true);
				return (hilo);
			}
		};
		final RejectedExecutionHandler alLlenarse = new RejectedExecutionHandler() {
			public void rejectedExecution(final Runnable tarea, final ThreadPoolExecutor pool) {
				//Se descarta y se dice. Bloquear aqui seria bloquear la caja.
				Logger.getLogger("log_file").warn("AvisoRedencion: cola llena, se descarta un aviso");
			}
		};
		return (new ThreadPoolExecutor(HILOS, HILOS, 60, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(COLA), fabrica, alLlenarse));
	}

	/**
	 * Manda el correo y espera. Devuelve vacio si salio, o el motivo si no.
	 *
	 * Nunca lanza: un problema con el correo no puede tumbar una redencion que
	 * ya quedo hecha.
	 */
	public static String enviarAhora(final String correo, final double puntos, final String motivo,
			final int idTienda, final boolean manual) {
		try {
			return (armarYEnviar(correo, puntos, motivo, idTienda, manual));
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AvisoRedencion.enviarAhora: " + e.toString());
			return ("error tecnico enviando el correo.");
		}
	}

	/**
	 * Deja el correo encolado y devuelve de inmediato.
	 *
	 * El resultado va al log y a ningun otro lado: quien llama no puede hacer
	 * nada con el, porque ya le entrego el producto al cliente.
	 */
	public static void enviarLuego(final String correo, final double puntos, final String motivo,
			final int idTienda, final boolean manual) {
		try {
			CORREOS.execute(new Runnable() {
				public void run() {
					final String falla = enviarAhora(correo, puntos, motivo, idTienda, manual);
					if (falla.length() > 0) {
						Logger.getLogger("log_file").warn("AvisoRedencion: no salio el correo de ["
								+ correo + "]: " + falla);
					}
				}
			});
		} catch (final Exception e) {
			//Ni siquiera encolar puede romper una redencion.
			Logger.getLogger("log_file").error("AvisoRedencion.enviarLuego: " + e.toString());
		}
	}

	// =======================================================================

	private static String armarYEnviar(final String correo, final double puntos, final String motivo,
			final int idTienda, final boolean manual) {

		if (correo == null || correo.trim().length() == 0) {
			return ("no hay correo del cliente.");
		}
		if (!ControladorEnvioCorreo.esDireccionValida(correo.trim())) {
			return ("la direccion [" + correo.trim() + "] no es una direccion valida.");
		}

		final PlantillaCorreoRedencion.Datos datos = new PlantillaCorreoRedencion.Datos();
		datos.puntos = puntos;
		datos.manual = manual;
		datos.motivo = (motivo == null) ? "" : motivo;
		datos.fecha = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		datos.nombre = nombreDe(correo.trim());
		datos.saldo = saldoDe(correo.trim());
		datos.tienda = nombreTienda(idTienda);

		//El vencimiento se consulta DESPUES de la redencion, para que lo que se
		//le dice al cliente sea lo que le queda de verdad y no lo que tenia
		//antes de redimir.
		final FidelizacionTransaccionDAO.ProximoVencimiento proximo =
				FidelizacionTransaccionDAO.obtenerProximoVencimiento(correo.trim());
		datos.proximoVencimiento = proximo.fecha;
		datos.puntosQueVencen = proximo.puntos;

		//La misma cuenta del aviso de vencimiento de puntos, que es el otro
		//correo del plan de fidelizacion. Si no esta parametrizada se cae a la
		//de siempre, para que el aviso salga igual.
		String cuenta = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOVENCIMIENTO");
		String clave = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOVENCIMIENTO");
		if (cuenta == null || cuenta.trim().length() == 0) {
			cuenta = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
			clave = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		}
		if (cuenta == null || cuenta.trim().length() == 0) {
			return ("no hay una cuenta de correo parametrizada.");
		}

		final Correo mensaje = new Correo();
		mensaje.setUsuarioCorreo(cuenta);
		mensaje.setContrasena(clave);
		mensaje.setAsunto(PlantillaCorreoRedencion.asunto(manual));
		mensaje.setMensaje(PlantillaCorreoRedencion.cuerpo(datos));

		final ArrayList destinos = new ArrayList();
		destinos.add(correo.trim());

		final ControladorEnvioCorreo envio = new ControladorEnvioCorreo(mensaje, destinos);
		final ControladorEnvioCorreo.ResultadoEnvio resultado = envio.enviarConReintentos();
		if (resultado == ControladorEnvioCorreo.ResultadoEnvio.ENVIADO) {
			return ("");
		}
		return ("el servidor de correo respondio " + resultado + ".");
	}

	/**
	 * El nombre del cliente.
	 *
	 * No esta en cliente_fidelizacion -esa tabla solo tiene el correo-, asi que
	 * se busca en cliente. Puede no aparecer: hay correos del plan sin ficha.
	 * Eso no impide el correo, solo hace que salude sin nombre.
	 */
	private static String nombreDe(final String correo) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		String nombre = "";
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT nombre, apellido FROM cliente"
					+ " WHERE email = ? AND nombre IS NOT NULL AND nombre <> ''"
					+ " ORDER BY idcliente DESC LIMIT 1");
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				final String n = rs.getString("nombre") == null ? "" : rs.getString("nombre").trim();
				final String a = rs.getString("apellido") == null ? "" : rs.getString("apellido").trim();
				nombre = (n + " " + a).trim();
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AvisoRedencion.nombreDe: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (nombre);
	}

	/** El saldo despues de la redencion. */
	private static double saldoDe(final String correo) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		double saldo = 0;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT puntos_vigentes FROM cliente_fidelizacion WHERE correo = ?");
			ps.setString(1, correo);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				saldo = rs.getDouble(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AvisoRedencion.saldoDe: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (saldo);
	}

	/** El nombre de la tienda, para que el correo no diga "tienda 9". */
	private static String nombreTienda(final int idTienda) {
		if (idTienda <= 0) {
			return ("");
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		String nombre = "";
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT nombre FROM tienda WHERE idtienda = ?");
			ps.setInt(1, idTienda);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				nombre = rs.getString(1) == null ? "" : rs.getString(1).trim();
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AvisoRedencion.nombreTienda: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (nombre);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AvisoRedencion: cerrando conexion " + e.toString());
		}
	}
}

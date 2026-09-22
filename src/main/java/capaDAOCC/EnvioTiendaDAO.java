package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El turno para mandarle un pedido a la tienda.
 *
 * POR QUE EXISTE ESTO
 *
 * El 2026-09-21 en Envigado salieron duplicados los pedidos 192514/192515 y
 * 192516/192517: dos veces lo mismo en cocina, creados con UN SEGUNDO de
 * diferencia. El envio automatico y el reenvio manual de una persona salieron
 * al tiempo, y ninguno de los dos pregunto si el pedido ya estaba alla.
 *
 * Hay OCHO pantallas con boton de reenviar y TRES procesos de Servicios que
 * reenvian solos. Poner la validacion en cada una serian once sitios donde
 * olvidarla, y once sitios donde volverla a olvidar la proxima vez que se
 * agregue una pantalla. Por eso vive aqui, en el unico paso por el que todos
 * pasan obligatoriamente.
 *
 * COMO FUNCIONA: SE PIDE TURNO ANTES DE MANDAR
 *
 * Quien va a mandar un pedido a la tienda llama primero a tomarTurno. Si le
 * dicen que no, no manda. El turno se toma con UN SOLO UPDATE con condiciones,
 * no con un SELECT y despues un UPDATE: dos procesos que consulten al mismo
 * tiempo veran los dos que no hay nadie, y ese es exactamente el caso que hay
 * que evitar. Con un UPDATE condicional la base decide, y solo uno se lleva la
 * fila.
 *
 * EL TURNO SE VENCE AL MINUTO, A PROPOSITO
 *
 * Si un envio se muere a mitad de camino -se cierra el navegador, la tienda no
 * contesta- y el turno no se venciera, ese pedido no se podria reenviar nunca
 * mas y habria que arreglarlo a mano en la base. Un minuto es mucho mas de lo
 * que tarda un envio bueno y mucho menos de lo que aguanta un cliente.
 *
 * QUE CUENTA COMO "YA ESTA EN LA TIENDA"
 *
 * enviadopixel = 1 Y ademas numposheader mayor que cero. Las dos cosas, porque
 * cuando se finaliza un pedido SIN mandarlo a la tienda igual queda en
 * enviadopixel = 1 pero con numposheader en cero, y esos si hay que poder
 * mandarlos despues. Mirar solo enviadopixel dejaria pedidos legitimos sin
 * forma de llegar a la tienda, que es peor que el problema original.
 */
public class EnvioTiendaDAO {

	/**
	 * Cuanto vale un turno antes de vencerse. Ver el comentario de la clase: no
	 * es un tiempo de espera, es el seguro para que un envio muerto no deje el
	 * pedido trancado.
	 */
	private static final int SEGUNDOS_TURNO = 60;

	/** Lo que responde tomarTurno. */
	public static class Turno {
		/** true = puede mandar. false = NO mande. */
		public boolean permitido = false;
		/** OK, YA_EN_TIENDA, EN_CURSO, NO_EXISTE o ERROR. */
		public String motivo = "OK";
		/** Lo que se le muestra a la persona. Va en español y sin tecnicismos. */
		public String mensaje = "";
		/** El numero que tiene el pedido en la tienda, cuando ya llego. */
		public int numeroTienda = 0;
	}

	/**
	 * Pide el turno para mandarle este pedido a la tienda.
	 *
	 * @return permitido = true solo si de verdad se puede mandar. Quien llame
	 *         TIENE que respetar el false: este metodo no manda nada, solo dice
	 *         si se puede.
	 */
	public static Turno tomarTurno(final int idPedido) {
		final Turno t = new Turno();
		if (idPedido <= 0) {
			t.motivo = "NO_EXISTE";
			t.mensaje = "No se indico cual pedido.";
			return (t);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();

			/*
			 * UN SOLO UPDATE. Aqui esta toda la gracia.
			 *
			 * La base evalua las condiciones y escribe la marca en la misma
			 * operacion, asi que de dos envios simultaneos solo uno puede salir
			 * con una fila modificada. Partirlo en SELECT y luego UPDATE seria
			 * volver al problema: los dos verian el campo vacio.
			 *
			 * IFNULL en las dos columnas porque numposheader llega en NULL
			 * mientras la tienda no ha contestado, y una comparacion con NULL
			 * no es falsa sino desconocida: sin el IFNULL la fila no entraria y
			 * se bloquearian pedidos que si se podian mandar.
			 */
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido SET envio_tienda_en_curso = NOW()"
					+ " WHERE idpedido = ?"
					+ "   AND (IFNULL(enviadopixel,0) <> 1 OR IFNULL(numposheader,0) = 0)"
					+ "   AND (envio_tienda_en_curso IS NULL"
					+ "        OR envio_tienda_en_curso < NOW() - INTERVAL " + SEGUNDOS_TURNO + " SECOND)");
			ps.setInt(1, idPedido);
			final int filas = ps.executeUpdate();
			ps.close();

			if (filas == 1) {
				t.permitido = true;
				t.motivo = "OK";
				return (t);
			}

			//No se pudo. Ahora si se consulta, para poder decir POR QUE: un "no
			//se pudo" pelado deja a la persona sin saber si esperar o llamar.
			explicar(cn, idPedido, t);
		} catch (final Exception e) {
			/*
			 * Si no se puede pedir turno, NO se deja mandar.
			 *
			 * Es la decision incomoda pero correcta: dejar pasar ante la duda
			 * significa arriesgar un pedido duplicado en cocina, que sale caro
			 * y molesta al cliente. Bloquear significa que alguien reintenta en
			 * un minuto.
			 */
			t.permitido = false;
			t.motivo = "ERROR";
			t.mensaje = "No se pudo verificar si el pedido ya habia sido enviado, "
					+ "asi que no se envio. Intente de nuevo en un momento.";
			Logger.getLogger("log_file").error("EnvioTiendaDAO.tomarTurno pedido " + idPedido
					+ ": " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (t);
	}

	/** Averigua por que no se dio el turno y arma el mensaje. */
	private static void explicar(final Connection cn, final int idPedido, final Turno t)
			throws Exception {
		final PreparedStatement ps = cn.prepareStatement(
				"SELECT IFNULL(enviadopixel,0) enviadopixel, IFNULL(numposheader,0) numposheader,"
				+ " envio_tienda_en_curso,"
				+ " TIMESTAMPDIFF(SECOND, envio_tienda_en_curso, NOW()) hace"
				+ " FROM pedido WHERE idpedido = ?");
		ps.setInt(1, idPedido);
		final ResultSet rs = ps.executeQuery();
		if (!rs.next()) {
			t.motivo = "NO_EXISTE";
			t.mensaje = "No se encontro el pedido " + idPedido + ".";
			rs.close();
			ps.close();
			return;
		}
		final int enviado = rs.getInt("enviadopixel");
		final int numero = rs.getInt("numposheader");
		final int hace = rs.getInt("hace");
		final boolean hayMarca = rs.getString("envio_tienda_en_curso") != null;
		rs.close();
		ps.close();

		if (enviado == 1 && numero > 0) {
			t.motivo = "YA_EN_TIENDA";
			t.numeroTienda = numero;
			t.mensaje = "Este pedido YA esta en la tienda con el numero " + numero
					+ ". No se volvio a enviar para no duplicarlo en cocina. "
					+ "Si de verdad no aparece alla, avise a tecnologia.";
			return;
		}
		if (hayMarca) {
			t.motivo = "EN_CURSO";
			t.mensaje = "Este pedido se esta enviando en este momento (hace " + hace
					+ " segundos). Espere unos segundos y revise si aparecio en la tienda "
					+ "antes de volver a intentar.";
			return;
		}
		//No deberia llegar aca: el UPDATE no modifico y ninguna de las dos
		//razones aplica. Se deja dicho en vez de inventar una explicacion.
		t.motivo = "ERROR";
		t.mensaje = "No se pudo tomar el turno de envio del pedido " + idPedido + ".";
	}

	/**
	 * Suelta el turno cuando el envio no se hizo.
	 *
	 * Sirve para no dejar esperando el minuto completo a quien va a reintentar
	 * de una. No es obligatorio llamarlo -el turno se vence solo- y por eso no
	 * devuelve nada ni deja caer errores: es una cortesia, no parte del control.
	 */
	public static void soltarTurno(final int idPedido) {
		if (idPedido <= 0) {
			return;
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido SET envio_tienda_en_curso = NULL WHERE idpedido = ?");
			ps.setInt(1, idPedido);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnvioTiendaDAO.soltarTurno pedido " + idPedido
					+ ": " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnvioTiendaDAO: no cerro, " + e.toString());
		}
	}
}

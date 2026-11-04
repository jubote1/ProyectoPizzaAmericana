package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Identificacion del cliente en el mostrador, leida de la base local de una
 * tienda.
 *
 * Es la misma consulta que capaDAOPOS.PedidoDAO.obtenerIdentificacionMostrador,
 * que usa el reporte semanal de Servicios. Esta copiada aqui y NO se llama alla
 * por una razon concreta: el central no compila contra el proyecto del POS sino
 * contra libs/PizzaAmericanaCliente.jar, una foto del POS que hay que volver a
 * armar a mano. Esa foto es de noviembre de 2025 y no trae el metodo, asi que
 * llamarlo compilaba en mi verificacion -que si tenia las fuentes del POS- y
 * reventaba en Eclipse.
 *
 * Que estan duplicadas es a proposito y tiene costo: si se cambia la de alla
 * hay que cambiar esta, o el correo semanal y la pantalla dejarian de dar lo
 * mismo. La alternativa era rearmar un jar de 287 MB cada vez que el POS cambie.
 */
public class IdentificacionMostradorDAO {

	/** Lo que hizo una persona en el mostrador dentro del rango. */
	public static class Cajero {
		public String cajero = "";
		/** Pedidos que CUENTAN: no incluye los que no llevan pizza. */
		public int pedidos = 0;
		public int identificados = 0;

		/**
		 * Pedidos sin pizza y por eso NO estan en "pedidos" ni en
		 * "identificados". Se devuelven aparte para poder decir cuantos se dejaron
		 * por fuera.
		 */
		public int porcionesSolas = 0;

		/**
		 * De los NO identificados, a cuantos si se les pregunto y el cliente no
		 * dio el dato. Queda en cero donde todavia no se prendio PEDIRCLIENTEPV,
		 * que es lo correcto: ahi nadie esta preguntando.
		 */
		public int preguntadosSinDar = 0;

		/**
		 * Cuantas veces salio la pantalla de autorizacion de datos y cuantas el
		 * cliente dijo que SI. Solo aparece cuando el cliente quedo identificado
		 * y no se le ha preguntado en los ultimos 90 dias, asi que son MUCHO
		 * menores que los pedidos: no se leen como porcentaje de las ventas.
		 */
		public int autorizacionPreguntadas = 0;
		public int autorizacionSi = 0;

		public int porcentaje() {
			return (this.pedidos == 0 ? 0 : (this.identificados * 100) / this.pedidos);
		}

		/** Ni se pregunto: ni identificado, ni con motivo registrado. */
		public int sinPreguntar() {
			int resto = this.pedidos - this.identificados - this.preguntadosSinDar;
			return (resto > 0 ? resto : 0);
		}
	}

	/**
	 * Si una tabla existe en la base a la que ya estamos conectados.
	 *
	 * Para no romper el reporte en una tienda atrasada con las migraciones.
	 * Ante la duda devuelve false, que es el camino que funciona en las dos.
	 */
	private static boolean existeTabla(Connection con1, String tabla) {
		try {
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery("SELECT COUNT(*) AS hay FROM information_schema.tables"
					+ " WHERE table_schema = DATABASE() AND table_name = '" + tabla + "'");
			boolean existe = false;
			if (rs.next()) {
				existe = (rs.getInt("hay") > 0);
			}
			rs.close();
			stm.close();
			return (existe);
		} catch (Exception e) {
			return (false);
		}
	}

	/**
	 * Un pedido SIN PIZZA: no lleva ninguna linea de pizza, tenga lo que tenga.
	 * Decidido por Juan David el 4 de noviembre de 2026 para el reto de identificacion
	 * en el mostrador: las ventas que no incluyen una pizza -porciones, gaseosas
	 * solas, lecheritas, acompanantes sueltos- no piden los datos del cliente (el POS
	 * las deja con el cliente generico) y no cuentan. Desde el 27 de octubre solo se
	 * dejaban por fuera los pedidos que eran unicamente porciones.
	 *
	 * Pizza = tipo_producto = 'P' (incluye combos y promos). Las lineas anuladas no
	 * cuentan. Se escribe sobre el alias "p" de pedido.
	 *
	 * Es la misma condicion de capaDAOPOS.PedidoDAO.SQL_SIN_PIZZA: si se cambia una
	 * hay que cambiar la otra. Y es la que decide, en la caja del POS, si se pregunta
	 * por el cliente.
	 */
	private static final String SQL_SIN_PIZZA = "(NOT EXISTS (SELECT 1 FROM detalle_pedido dz"
			+ " JOIN producto pz ON pz.idproducto = dz.idproducto"
			+ " WHERE dz.idpedidotienda = p.idpedidotienda AND IFNULL(dz.idmotivoanulacion, 0) = 0"
			+ " AND pz.tipo_producto = 'P'))";

	/**
	 * @param desde         fecha inicial, inclusive, en yyyy-MM-dd
	 * @param hasta         fecha final, inclusive
	 * @param hostBD        la tienda a la que se consulta
	 * @param tipoMostrador descripcion del tipo de pedido de mostrador
	 */
	public static ArrayList<Cajero> obtenerIdentificacionMostrador(String desde, String hasta, String hostBD,
			String tipoMostrador) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Cajero> filas = new ArrayList<Cajero>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDTiendaRemota(hostBD);
		if (con1 == null) {
			logger.info("No se pudo conectar a la tienda " + hostBD + " para la identificacion.");
			return (filas);
		}
		try {
			//pedido_sin_cliente puede no existir en una tienda que todavia no
			//corrio la migracion del paso 3. Se pregunta antes en vez de dejar
			//que la consulta falle: si fallara, esa tienda desapareceria ENTERA
			//del reporte por una tabla que solo aporta una columna.
			boolean hayMotivos = existeTabla(con1, "pedido_sin_cliente");

			//Se cuenta identificado cuando el cliente del pedido tiene un celular
			//que sirve de llave. Y de los NO identificados se separan los que si
			//se preguntaron: tienen fila en pedido_sin_cliente con su motivo. La
			//condicion lleva tambien "celular is null" por si un pedido quedo
			//marcado y despues si se identifico, para que las tres cifras sumen
			//los pedidos.
			//Los pedidos SIN PIZZA no se cuentan (ver
			//SQL_SIN_PIZZA), pero se devuelven aparte en "porciones_solas" (el nombre viene de cuando solo eran porciones).
			String consulta = "SELECT IFNULL(x.usuariopedido,'(sin usuario)') AS cajero,"
					+ " SUM(x.solo_porciones = 0) AS pedidos,"
					+ " SUM(x.solo_porciones = 0 AND c.celular_norm IS NOT NULL) AS identificados,"
					+ (hayMotivos
							? " SUM(x.solo_porciones = 0 AND sc.idpedidotienda IS NOT NULL AND c.celular_norm IS NULL) AS preguntados,"
							: " 0 AS preguntados,")
					+ " SUM(x.solo_porciones = 1) AS porciones_solas"
					+ " FROM (SELECT p.idpedidotienda, p.idcliente, p.usuariopedido, "
					+ SQL_SIN_PIZZA + " AS solo_porciones"
					+ "       FROM pedido p"
					+ "       LEFT JOIN tipo_pedido t ON t.idtipopedido = p.idtipopedido"
					+ "       WHERE p.fechapedido BETWEEN '" + desde + "' AND '" + hasta + "'"
					+ "         AND t.descripcion = '" + tipoMostrador + "'"
					+ "         AND IFNULL(p.idmotivoanulacion, 0) = 0) x"
					+ " LEFT JOIN cliente c ON c.idcliente = x.idcliente"
					+ (hayMotivos
							? " LEFT JOIN pedido_sin_cliente sc ON sc.idpedidotienda = x.idpedidotienda"
							: "")
					+ " GROUP BY x.usuariopedido ORDER BY pedidos DESC";
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				Cajero fila = new Cajero();
				fila.cajero = rs.getString("cajero");
				fila.pedidos = rs.getInt("pedidos");
				fila.identificados = rs.getInt("identificados");
				fila.preguntadosSinDar = rs.getInt("preguntados");
				fila.porcionesSolas = rs.getInt("porciones_solas");
				filas.add(fila);
			}
			rs.close();
			stm.close();

			//La autorizacion de datos, en una consulta aparte sobre la misma
			//conexion. No se une a la de arriba a proposito:
			//cliente_autorizacion_datos no cuelga del pedido sino del cliente,
			//asi que unirlas multiplicaria las filas de pedido. Un cajero puede
			//aparecer aqui y no arriba -pregunto en un pedido que no era de
			//mostrador- asi que si no estaba se agrega.
			if (existeTabla(con1, "cliente_autorizacion_datos")) {
				String consultaAut = "SELECT IFNULL(usuario,'(sin usuario)') AS cajero,"
						+ " COUNT(*) AS preguntadas,"
						+ " SUM(respuesta = 'S') AS dijeron_si"
						+ " FROM cliente_autorizacion_datos"
						+ " WHERE DATE(creado_en) BETWEEN '" + desde + "' AND '" + hasta + "'"
						+ " GROUP BY usuario";
				Statement stmAut = con1.createStatement();
				ResultSet rsAut = stmAut.executeQuery(consultaAut);
				while (rsAut.next()) {
					String cajero = rsAut.getString("cajero");
					Cajero fila = null;
					for (int i = 0; i < filas.size(); i++) {
						if (filas.get(i).cajero != null && filas.get(i).cajero.equals(cajero)) {
							fila = filas.get(i);
							break;
						}
					}
					if (fila == null) {
						fila = new Cajero();
						fila.cajero = cajero;
						filas.add(fila);
					}
					fila.autorizacionPreguntadas = rsAut.getInt("preguntadas");
					fila.autorizacionSi = rsAut.getInt("dijeron_si");
				}
				rsAut.close();
				stmAut.close();
			}

			con1.close();
		} catch (Exception e) {
			//Que una tienda falle no puede tumbar el reporte de las demas.
			logger.info("Fallo la identificacion de la tienda " + hostBD + ": " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (filas);
	}

}

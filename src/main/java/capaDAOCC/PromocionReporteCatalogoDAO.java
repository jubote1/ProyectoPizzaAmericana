package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El catalogo de promociones que vigila el reporte diario.
 *
 * POR QUE EXISTE ESTA PANTALLA
 *
 * El reporte viejo traia las promociones quemadas en el codigo -trece bloques
 * copiados y pegados- y por eso se desactualizo: agregar una exigia programar,
 * y nadie lo hizo. El resultado medido fue que siete de las diez promociones
 * que vigilaba no vendian nada, y las tres mas vendidas de la compania -las
 * Insuperables, 10.578 lineas en tres meses- no aparecian.
 *
 * Mientras agregar una promocion sea una tarea de programador, va a volver a
 * pasar. Por eso el catalogo se administra desde aqui.
 *
 * LA BUSQUEDA DE PRODUCTOS VA CONTRA UNA TIENDA
 *
 * Una promocion se define por los productos de la base de CADA TIENDA, no por
 * los del central -son numeraciones distintas-. Nadie se sabe esos ids de
 * memoria, asi que la pantalla los busca por nombre en una tienda de
 * referencia, y de paso muestra cuanto vendio cada uno el ultimo mes: un
 * producto con cero ventas en treinta dias casi siempre es el que no era.
 *
 * Se puede usar cualquier tienda como referencia porque el catalogo de
 * producto esta sincronizado: se comparo la firma MD5 de los productos
 * sembrados en las once y diez dieron identico -la que faltaba estaba caida-.
 */
public class PromocionReporteCatalogoDAO {

	public static class Promocion {
		public int idPromo;
		public String nombre = "";
		public boolean plataforma;
		public boolean activo;
		public int orden;
		public ArrayList<Producto> productos = new ArrayList<Producto>();
	}

	public static class Producto {
		public int idProducto;
		public String descripcion = "";
		public double vendidosMes;
		/** Nombre de la promocion que ya lo tiene, si otra lo tiene. */
		public String yaEnPromocion = "";
	}

	// =======================================================================
	// LECTURA
	// =======================================================================

	/** Todas las promociones, activas y apagadas, con sus productos. */
	public static ArrayList<Promocion> listar() {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final LinkedHashMap<Integer, Promocion> porId = new LinkedHashMap<Integer, Promocion>();
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT p.idpromo, p.nombre, p.plataforma, p.activo, p.orden, i.idproducto"
					+ " FROM promocion_reporte p"
					+ " LEFT JOIN promocion_reporte_item i"
					+ "        ON i.idpromo = p.idpromo AND i.activo = 'S'"
					+ " ORDER BY p.orden, p.idpromo, i.idproducto");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final int id = rs.getInt("idpromo");
				Promocion promo = porId.get(Integer.valueOf(id));
				if (promo == null) {
					promo = new Promocion();
					promo.idPromo = id;
					promo.nombre = rs.getString("nombre");
					promo.plataforma = "S".equalsIgnoreCase(rs.getString("plataforma"));
					promo.activo = "S".equalsIgnoreCase(rs.getString("activo"));
					promo.orden = rs.getInt("orden");
					porId.put(Integer.valueOf(id), promo);
				}
				final int idProducto = rs.getInt("idproducto");
				if (!rs.wasNull() && idProducto > 0) {
					final Producto prod = new Producto();
					prod.idProducto = idProducto;
					promo.productos.add(prod);
				}
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("PromocionReporteCatalogoDAO.listar: " + e.toString());
		} finally {
			cerrar(cn);
		}

		final ArrayList<Promocion> lista = new ArrayList<Promocion>(porId.values());
		ponerleNombreALosProductos(lista);
		return (lista);
	}

	/**
	 * Le pone descripcion y venta del mes a los productos de todas las
	 * promociones, en UNA sola consulta a la tienda de referencia.
	 *
	 * Si la tienda no responde, los productos quedan sin nombre pero la pantalla
	 * se pinta igual con los ids: es preferible una pantalla con ids a una
	 * pantalla en blanco.
	 */
	private static void ponerleNombreALosProductos(final ArrayList<Promocion> promociones) {
		final StringBuilder ids = new StringBuilder();
		for (int i = 0; i < promociones.size(); i++) {
			final ArrayList<Producto> productos = promociones.get(i).productos;
			for (int j = 0; j < productos.size(); j++) {
				if (ids.length() > 0) {
					ids.append(",");
				}
				ids.append(productos.get(j).idProducto);
			}
		}
		if (ids.length() == 0) {
			return;
		}
		final LinkedHashMap<Integer, Producto> encontrados = consultarProductos(
				"SELECT pr.idproducto, pr.descripcion,"
				+ " IFNULL((SELECT SUM(d.cantidad) FROM detalle_pedido d, pedido pe"
				+ "          WHERE d.idproducto = pr.idproducto AND pe.idpedidotienda = d.idpedidotienda"
				+ "            AND pe.fechapedido >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)"
				+ "            AND d.idmotivoanulacion IS NULL), 0) AS vendidos"
				+ " FROM producto pr WHERE pr.idproducto IN (" + ids.toString() + ")");

		for (int i = 0; i < promociones.size(); i++) {
			final ArrayList<Producto> productos = promociones.get(i).productos;
			for (int j = 0; j < productos.size(); j++) {
				final Producto prod = productos.get(j);
				final Producto hallado = encontrados.get(Integer.valueOf(prod.idProducto));
				if (hallado != null) {
					prod.descripcion = hallado.descripcion;
					prod.vendidosMes = hallado.vendidosMes;
				} else {
					//Un producto que esta en el catalogo de promociones y ya no
					//existe en la tienda tiene que verse, no desaparecer.
					prod.descripcion = "(ya no existe en el catalogo de la tienda)";
				}
			}
		}
	}

	/**
	 * Busca productos por nombre en la tienda de referencia.
	 *
	 * Marca los que ya pertenecen a una promocion, para que no se agregue el
	 * mismo producto a dos y termine contandose dos veces.
	 */
	public static ArrayList<Producto> buscarProductos(final String texto) {
		final ArrayList<Producto> resultado = new ArrayList<Producto>();
		if (texto == null || texto.trim().length() < 2) {
			return (resultado);
		}
		//El texto lo escribe una persona, asi que se limpia antes de concatenar.
		final String limpio = texto.trim().replace("'", "").replace("\\", "")
				.replace("%", "").replace("_", "");
		if (limpio.length() < 2) {
			return (resultado);
		}

		final LinkedHashMap<Integer, Producto> hallados = consultarProductos(
				"SELECT pr.idproducto, pr.descripcion,"
				+ " IFNULL((SELECT SUM(d.cantidad) FROM detalle_pedido d, pedido pe"
				+ "          WHERE d.idproducto = pr.idproducto AND pe.idpedidotienda = d.idpedidotienda"
				+ "            AND pe.fechapedido >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)"
				+ "            AND d.idmotivoanulacion IS NULL), 0) AS vendidos"
				+ " FROM producto pr WHERE pr.descripcion LIKE '%" + limpio + "%'"
				+ " ORDER BY vendidos DESC, pr.descripcion LIMIT 40");

		final LinkedHashMap<Integer, String> yaUsados = productosYaUsados();
		final ArrayList<Producto> lista = new ArrayList<Producto>(hallados.values());
		for (int i = 0; i < lista.size(); i++) {
			final Producto p = lista.get(i);
			final String promo = yaUsados.get(Integer.valueOf(p.idProducto));
			p.yaEnPromocion = (promo == null) ? "" : promo;
			resultado.add(p);
		}
		return (resultado);
	}

	/** Que producto esta en que promocion, para avisar antes de duplicar. */
	private static LinkedHashMap<Integer, String> productosYaUsados() {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final LinkedHashMap<Integer, String> mapa = new LinkedHashMap<Integer, String>();
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT i.idproducto, p.nombre FROM promocion_reporte_item i"
					+ " JOIN promocion_reporte p ON p.idpromo = i.idpromo"
					+ " WHERE i.activo = 'S'");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				mapa.put(Integer.valueOf(rs.getInt("idproducto")), rs.getString("nombre"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO.productosYaUsados: " + e);
		} finally {
			cerrar(cn);
		}
		return (mapa);
	}

	/**
	 * Corre una consulta de productos contra la primera tienda que responda.
	 *
	 * Recorre las tiendas en orden y se queda con la primera que contesta: el
	 * dia que una este apagada, la pantalla sigue funcionando. El catalogo es
	 * el mismo en todas, asi que da igual cual conteste.
	 */
	private static LinkedHashMap<Integer, Producto> consultarProductos(final String consulta) {
		final LinkedHashMap<Integer, Producto> encontrados = new LinkedHashMap<Integer, Producto>();
		final ArrayList<String> hosts = hostsDeTienda();
		final ConexionBaseDatos con = new ConexionBaseDatos();

		for (int i = 0; i < hosts.size(); i++) {
			Connection cn = null;
			try {
				cn = con.obtenerConexionBDTiendaRemota(hosts.get(i));
				if (cn == null) {
					continue;
				}
				final Statement stm = cn.createStatement();
				final ResultSet rs = stm.executeQuery(consulta);
				while (rs.next()) {
					final Producto p = new Producto();
					p.idProducto = rs.getInt("idproducto");
					p.descripcion = rs.getString("descripcion");
					p.vendidosMes = rs.getDouble("vendidos");
					encontrados.put(Integer.valueOf(p.idProducto), p);
				}
				rs.close();
				stm.close();
				cerrar(cn);
				//Contesto una: no hace falta molestar a las otras diez.
				return (encontrados);
			} catch (final Exception e) {
				System.out.println("PromocionReporteCatalogoDAO: " + hosts.get(i)
						+ " no respondio, se intenta con la siguiente");
				cerrar(cn);
			}
		}
		return (encontrados);
	}

	private static ArrayList<String> hostsDeTienda() {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final ArrayList<String> hosts = new ArrayList<String>();
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT hosbd FROM tienda WHERE hosbd <> '' ORDER BY idtienda");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final String host = rs.getString(1);
				if (host != null && host.trim().length() > 0) {
					hosts.add(host.trim());
				}
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO.hostsDeTienda: " + e);
		} finally {
			cerrar(cn);
		}
		return (hosts);
	}

	// =======================================================================
	// ESCRITURA
	// =======================================================================

	/** @return el id de la promocion creada, o 0 si no se pudo. */
	public static int crear(final String nombre, final boolean plataforma, final int orden) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		int id = 0;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO promocion_reporte (nombre, plataforma, activo, orden)"
					+ " VALUES (?, ?, 'S', ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, nombre.trim());
			ps.setString(2, plataforma ? "S" : "N");
			ps.setInt(3, orden);
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO.crear: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	public static boolean actualizar(final int idPromo, final String nombre, final boolean plataforma,
			final boolean activo, final int orden) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		boolean listo = false;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE promocion_reporte SET nombre = ?, plataforma = ?, activo = ?, orden = ?"
					+ " WHERE idpromo = ?");
			ps.setString(1, nombre.trim());
			ps.setString(2, plataforma ? "S" : "N");
			ps.setString(3, activo ? "S" : "N");
			ps.setInt(4, orden);
			ps.setInt(5, idPromo);
			listo = (ps.executeUpdate() > 0);
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO.actualizar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (listo);
	}

	/**
	 * Agrega un producto a una promocion.
	 *
	 * Devuelve el motivo cuando no se puede, y vacio cuando quedo. No se deja
	 * agregar un producto que ya esta en otra promocion: se contaria dos veces
	 * y las dos cifras quedarian infladas sin que nada avise.
	 */
	public static String agregarProducto(final int idPromo, final int idProducto) {
		final LinkedHashMap<Integer, String> yaUsados = productosYaUsados();
		final String otra = yaUsados.get(Integer.valueOf(idProducto));
		if (otra != null) {
			return ("El producto " + idProducto + " ya esta en la promocion \"" + otra
					+ "\". Un producto en dos promociones se contaria dos veces.");
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		String error = "";
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO promocion_reporte_item (idpromo, idproducto, activo) VALUES (?, ?, 'S')"
					+ " ON DUPLICATE KEY UPDATE activo = 'S'");
			ps.setInt(1, idPromo);
			ps.setInt(2, idProducto);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO.agregarProducto: " + e);
			error = "No se pudo agregar el producto.";
		} finally {
			cerrar(cn);
		}
		return (error);
	}

	/**
	 * Quita un producto de una promocion.
	 *
	 * Se borra la fila en vez de apagarla: la historia ya guardada en
	 * datamart.promocion_dia no depende de esta tabla, asi que quitar un
	 * producto no borra ningun dato del pasado.
	 */
	public static boolean quitarProducto(final int idPromo, final int idProducto) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		boolean listo = false;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"DELETE FROM promocion_reporte_item WHERE idpromo = ? AND idproducto = ?");
			ps.setInt(1, idPromo);
			ps.setInt(2, idProducto);
			listo = (ps.executeUpdate() > 0);
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO.quitarProducto: " + e);
		} finally {
			cerrar(cn);
		}
		return (listo);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PromocionReporteCatalogoDAO: cerrando conexion " + e);
		}
	}
}

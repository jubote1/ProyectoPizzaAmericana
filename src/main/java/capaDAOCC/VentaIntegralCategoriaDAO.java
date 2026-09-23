package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.VentaIntegralCategoria;
import capaModeloCC.VentaIntegralCategoriaItem;
import conexionCC.ConexionBaseDatos;

/**
 * CRUD del catalogo parametrizable de Venta Integral: las categorias
 * (Adiciones, Deditos, Americana Premium...) y los idproducto/idespecialidad
 * que cuentan en cada una, por ambito (tienda o contact center).
 *
 * Lo usan tanto las pantallas de Monitoreo del central como el proceso
 * semanal de Servicios (que solo lee, via listarCategoriasActivas()).
 */
public class VentaIntegralCategoriaDAO {

	public static int insertarCategoria(VentaIntegralCategoria cat) {
		Logger logger = Logger.getLogger("log_file");
		int idCategoriaIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String insert = "insert into venta_integral_categoria "
					+ "(nombre, abreviatura, tipodato, medicion_tienda, excluye_anulados_tienda, "
					+ "filtro_estacion_tienda, medicion_cc, activo, orden) values ('"
					+ cat.getNombre() + "', '" + cat.getAbreviatura() + "', '" + cat.getTipoDato() + "', '"
					+ cat.getMedicionTienda() + "', '" + cat.getExcluyeAnuladosTienda() + "', '"
					+ cat.getFiltroEstacionTienda() + "', '" + cat.getMedicionCC() + "', 'S', " + cat.getOrden() + ")";
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()) {
				idCategoriaIns = rs.getInt(1);
			}
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			return (0);
		}
		if (idCategoriaIns > 0) {
			reemplazarItems(idCategoriaIns, VentaIntegralCategoriaItem.AMBITO_TIENDA, cat.getItemsTienda());
			reemplazarItems(idCategoriaIns, VentaIntegralCategoriaItem.AMBITO_CONTACTCENTER, cat.getItemsContactCenter());
		}
		return (idCategoriaIns);
	}

	public static String editarCategoria(VentaIntegralCategoria cat) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try {
			Statement stm = con1.createStatement();
			String update = "update venta_integral_categoria set nombre = '" + cat.getNombre()
					+ "', abreviatura = '" + cat.getAbreviatura() + "', tipodato = '" + cat.getTipoDato()
					+ "', medicion_tienda = '" + cat.getMedicionTienda() + "', excluye_anulados_tienda = '"
					+ cat.getExcluyeAnuladosTienda() + "', filtro_estacion_tienda = '" + cat.getFiltroEstacionTienda()
					+ "', medicion_cc = '" + cat.getMedicionCC() + "', orden = " + cat.getOrden()
					+ " where idcategoria = " + cat.getIdCategoria();
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			resultado = "error";
		}
		if ("exitoso".equals(resultado)) {
			reemplazarItems(cat.getIdCategoria(), VentaIntegralCategoriaItem.AMBITO_TIENDA, cat.getItemsTienda());
			reemplazarItems(cat.getIdCategoria(), VentaIntegralCategoriaItem.AMBITO_CONTACTCENTER,
					cat.getItemsContactCenter());
		}
		return (resultado);
	}

	/** Borrado logico: activo = 'N'. Una categoria desactivada no la usa el proceso semanal. */
	public static String eliminarCategoria(int idCategoria) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try {
			Statement stm = con1.createStatement();
			String update = "update venta_integral_categoria set activo = 'N' where idcategoria = " + idCategoria;
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			resultado = "error";
		}
		return (resultado);
	}

	public static VentaIntegralCategoria retornarCategoria(int idCategoria) {
		ArrayList<VentaIntegralCategoria> todas = listar("where idcategoria = " + idCategoria);
		if (todas.isEmpty()) {
			return (new VentaIntegralCategoria());
		}
		return (todas.get(0));
	}

	public static ArrayList<VentaIntegralCategoria> listarCategorias() {
		return (listar(""));
	}

	public static ArrayList<VentaIntegralCategoria> listarCategoriasActivas() {
		return (listar("where activo = 'S'"));
	}

	private static ArrayList<VentaIntegralCategoria> listar(String filtro) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<VentaIntegralCategoria> categorias = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select idcategoria, nombre, abreviatura, tipodato, medicion_tienda, "
					+ "excluye_anulados_tienda, filtro_estacion_tienda, medicion_cc, activo, orden "
					+ "from venta_integral_categoria " + filtro + " order by orden, idcategoria";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				VentaIntegralCategoria cat = new VentaIntegralCategoria(rs.getInt("idcategoria"),
						rs.getString("nombre"), rs.getString("abreviatura"), rs.getString("tipodato"),
						rs.getString("medicion_tienda"), rs.getString("excluye_anulados_tienda"),
						rs.getString("filtro_estacion_tienda"), rs.getString("medicion_cc"), rs.getString("activo"),
						rs.getInt("orden"));
				categorias.add(cat);
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			return (categorias);
		}
		for (VentaIntegralCategoria cat : categorias) {
			cat.setItemsTienda(listarItems(cat.getIdCategoria(), VentaIntegralCategoriaItem.AMBITO_TIENDA));
			cat.setItemsContactCenter(
					listarItems(cat.getIdCategoria(), VentaIntegralCategoriaItem.AMBITO_CONTACTCENTER));
		}
		return (categorias);
	}

	private static ArrayList<Integer> listarItems(int idCategoria, String ambito) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Integer> items = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select idvalor from venta_integral_categoria_item where idcategoria = " + idCategoria
					+ " and ambito = '" + ambito + "' and activo = 'S' order by idvalor";
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				items.add(rs.getInt("idvalor"));
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (items);
	}

	/**
	 * Reemplaza por completo los items de una categoria en un ambito: borra
	 * los que habia y deja los que vengan en la lista. Mas simple que calcular
	 * el diff, y el volumen de filas es minimo (unos pocos idproducto/idespecialidad
	 * por categoria).
	 */
	private static void reemplazarItems(int idCategoria, String ambito, ArrayList<Integer> idValores) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			stm.executeUpdate("delete from venta_integral_categoria_item where idcategoria = " + idCategoria
					+ " and ambito = '" + ambito + "'");
			if (idValores != null) {
				for (Integer idValor : idValores) {
					stm.executeUpdate("insert into venta_integral_categoria_item (idcategoria, ambito, idvalor) "
							+ "values (" + idCategoria + ", '" + ambito + "', " + idValor + ")");
				}
			}
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
	}

}

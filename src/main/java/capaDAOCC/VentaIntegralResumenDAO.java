package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import capaModeloCC.VentaIntegralCategoria;
import capaModeloCC.VentaIntegralResumenSemana;
import conexionCC.ConexionBaseDatos;

/**
 * Cierre semanal de Venta Integral: lo escribe el proceso de Servicios cada
 * lunes (insertarOActualizarResumen y consultarTotalesContactCenter) y lo lee
 * la pantalla de Monitoreo del central (consultarResumen).
 */
public class VentaIntegralResumenDAO {

	/**
	 * Upsert por (idtienda, idcategoria, semanainicio, semanafin): un reproceso
	 * de la misma semana actualiza la fila en vez de duplicarla.
	 */
	public static void insertarOActualizarResumen(VentaIntegralResumenSemana resumen) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String upsert = "insert into venta_integral_resumen_semanal "
					+ "(idtienda, idcategoria, semanainicio, semanafin, cantidad_tienda, cantidad_contactcenter, cantidad_total) "
					+ "values (" + resumen.getIdTienda() + ", " + resumen.getIdCategoria() + ", '"
					+ resumen.getSemanaInicio() + "', '" + resumen.getSemanaFin() + "', "
					+ resumen.getCantidadTienda() + ", " + resumen.getCantidadContactCenter() + ", "
					+ resumen.getCantidadTotal() + ") as nuevo on duplicate key update "
					+ "cantidad_tienda = nuevo.cantidad_tienda, cantidad_contactcenter = nuevo.cantidad_contactcenter, "
					+ "cantidad_total = nuevo.cantidad_total, fechaproceso = current_timestamp";
			logger.info(upsert);
			stm.executeUpdate(upsert);
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

	/**
	 * Totales de contact center (origen='C') por tienda, para una categoria y un
	 * rango de fechas. Se corre UNA vez por categoria contra el central (no por
	 * tienda), agrupando por idtienda.
	 *
	 * Con CONTEO_ESPECIALIDAD_CON_DEDUP replica la formula de las consultas
	 * legacy para pizzas mitad-mitad: cuenta 1/2 cuando la especialidad esta en
	 * el segundo sabor (b.idespecialidad2) o en el primero con un segundo sabor
	 * presente, y cuenta 1 completa cuando el pedido es solo de esa especialidad
	 * (idespecialidad2 = 0). Se usa UNION ALL, no UNION: al agrupar por tienda
	 * cada rama ya trae una fila por tienda, y un UNION (distinct) fundiria por
	 * error dos tiendas que por coincidencia dieran el mismo conteo parcial.
	 */
	public static HashMap<Integer, Double> consultarTotalesContactCenter(VentaIntegralCategoria categoria,
			String semanaIniISO, String semanaFinISO) {
		Logger logger = Logger.getLogger("log_file");
		HashMap<Integer, Double> totalesPorTienda = new HashMap<>();
		String listaValores = listaSeparadaPorComas(categoria.getItemsContactCenter());
		if (listaValores.isEmpty()) {
			return (totalesPorTienda);
		}
		String fechaInicial = semanaIniISO + " 00:00:00";
		String fechaFinal = semanaFinISO + " 23:59:59";
		String consulta;
		if (VentaIntegralCategoria.MEDICION_CC_CONTEO_ESPECIALIDAD_CON_DEDUP.equals(categoria.getMedicionCC())) {
			consulta = "select t.idtienda, sum(t.cnt) as total from ("
					+ "select a.idtienda as idtienda, count(*)/2 as cnt from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idespecialidad2 in (" + listaValores
					+ ") and a.numposheader > 0 group by a.idtienda "
					+ "union all "
					+ "select a.idtienda as idtienda, count(*)/2 as cnt from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idespecialidad1 in (" + listaValores
					+ ") and b.idespecialidad2 > 0 and a.numposheader > 0 group by a.idtienda "
					+ "union all "
					+ "select a.idtienda as idtienda, count(*) as cnt from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idespecialidad1 in (" + listaValores
					+ ") and b.idespecialidad2 = 0 and a.numposheader > 0 group by a.idtienda"
					+ ") t group by t.idtienda";
		} else {
			consulta = "select a.idtienda, count(*) as total from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idproducto in (" + listaValores + ") "
					+ "group by a.idtienda";
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				totalesPorTienda.put(rs.getInt("idtienda"), rs.getDouble("total"));
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
		return (totalesPorTienda);
	}

	/**
	 * Resumen agregado para la pantalla: suma las semanas del rango por tienda y
	 * categoria. idTienda en 0 trae todas las tiendas.
	 */
	public static ArrayList<VentaIntegralResumenSemana> consultarResumen(int idTienda, String semanaIniISO,
			String semanaFinISO) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<VentaIntegralResumenSemana> resumenes = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select r.idtienda, t.nombre as nombretienda, r.idcategoria, c.nombre as nombrecategoria, "
					+ "c.orden, sum(r.cantidad_tienda) as cantidad_tienda, sum(r.cantidad_contactcenter) as cantidad_contactcenter, "
					+ "sum(r.cantidad_total) as cantidad_total "
					+ "from venta_integral_resumen_semanal r, tienda t, venta_integral_categoria c "
					+ "where r.idtienda = t.idtienda and r.idcategoria = c.idcategoria "
					+ "and r.semanainicio >= '" + semanaIniISO + "' and r.semanafin <= '" + semanaFinISO + "'"
					+ (idTienda > 0 ? " and r.idtienda = " + idTienda : "")
					+ " group by r.idtienda, t.nombre, r.idcategoria, c.nombre, c.orden order by t.nombre, c.orden";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				VentaIntegralResumenSemana res = new VentaIntegralResumenSemana();
				res.setIdTienda(rs.getInt("idtienda"));
				res.setNombreTienda(rs.getString("nombretienda"));
				res.setIdCategoria(rs.getInt("idcategoria"));
				res.setNombreCategoria(rs.getString("nombrecategoria"));
				res.setCantidadTienda(rs.getDouble("cantidad_tienda"));
				res.setCantidadContactCenter(rs.getDouble("cantidad_contactcenter"));
				res.setCantidadTotal(rs.getDouble("cantidad_total"));
				resumenes.add(res);
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
		return (resumenes);
	}

	private static String listaSeparadaPorComas(ArrayList<Integer> valores) {
		if (valores == null || valores.isEmpty()) {
			return ("");
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < valores.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(valores.get(i));
		}
		return (sb.toString());
	}

}

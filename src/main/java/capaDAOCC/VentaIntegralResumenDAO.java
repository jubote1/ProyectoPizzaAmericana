package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.VentaIntegralCategoria;
import capaModeloCC.VentaIntegralResumenSemana;
import conexionCC.ConexionBaseDatos;

/**
 * Cierre semanal de Venta Integral: lo escribe el proceso de Servicios cada
 * lunes (insertarOActualizarResumen y consultarTotalContactCenter) y lo lee
 * la pantalla de Monitoreo del central (consultarResumen).
 *
 * Contact Center es un canal aparte, no una tienda: se guarda con
 * idtienda = 0 (sentinela reservado, ninguna tienda real usa 0) y SIN
 * distinguir a que tienda iba el pedido -se agrega en un solo numero por
 * categoria y semana-. Antes se sumaba dentro de cada tienda; eso mezclaba
 * algo que la tienda no ejecuto con su propio desempeno.
 */
public class VentaIntegralResumenDAO {

	/** idtienda reservado para la fila de Contact Center (ninguna tienda real usa 0). */
	public static final int IDTIENDA_CONTACTCENTER = 0;

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
	 * Total de contact center (origen='C') para una categoria y un rango de
	 * fechas, agregado en un solo numero para TODA la red -sin distinguir
	 * tienda, a proposito: el pedido lo atiende un agente central, no una
	 * tienda especifica, asi que no tiene sentido atribuirselo a una.
	 *
	 * Con CONTEO_ESPECIALIDAD_CON_DEDUP replica la formula de las consultas
	 * legacy para pizzas mitad-mitad: cuenta 1/2 cuando la especialidad esta en
	 * el segundo sabor (b.idespecialidad2) o en el primero con un segundo sabor
	 * presente, y cuenta 1 completa cuando el pedido es solo de esa especialidad
	 * (idespecialidad2 = 0).
	 */
	public static double consultarTotalContactCenter(VentaIntegralCategoria categoria, String semanaIniISO,
			String semanaFinISO) {
		Logger logger = Logger.getLogger("log_file");
		double total = 0;
		String listaValores = listaSeparadaPorComas(categoria.getItemsContactCenter());
		if (listaValores.isEmpty()) {
			return (0);
		}
		String fechaInicial = semanaIniISO + " 00:00:00";
		String fechaFinal = semanaFinISO + " 23:59:59";
		String consulta;
		if (VentaIntegralCategoria.MEDICION_CC_CONTEO_ESPECIALIDAD_CON_DEDUP.equals(categoria.getMedicionCC())) {
			consulta = "select coalesce(sum(t.cnt), 0) as total from ("
					+ "select count(*)/2 as cnt from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idespecialidad2 in (" + listaValores
					+ ") and a.numposheader > 0 "
					+ "union all "
					+ "select count(*)/2 as cnt from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idespecialidad1 in (" + listaValores
					+ ") and b.idespecialidad2 > 0 and a.numposheader > 0 "
					+ "union all "
					+ "select count(*) as cnt from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idespecialidad1 in (" + listaValores
					+ ") and b.idespecialidad2 = 0 and a.numposheader > 0"
					+ ") t";
		} else {
			consulta = "select count(*) as total from pedido a, detalle_pedido b "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedido = b.idpedido and a.origen = 'C' and b.idproducto in (" + listaValores + ")";
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			if (rs.next()) {
				total = rs.getDouble("total");
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
		return (total);
	}

	/**
	 * Resumen agregado para la pantalla: suma las semanas del rango por tienda
	 * (o Contact Center, idtienda=0) y categoria. idTienda en 0 trae todas las
	 * tiendas reales MAS la fila de Contact Center. Nunca incluye tiendas con
	 * hosbd vacio en la tabla tienda -no tienen base local que consultar, no
	 * son un punto de venta real operando-.
	 */
	public static ArrayList<VentaIntegralResumenSemana> consultarResumen(int idTienda, String semanaIniISO,
			String semanaFinISO) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<VentaIntegralResumenSemana> resumenes = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select r.idtienda, "
					+ "case when r.idtienda = " + IDTIENDA_CONTACTCENTER + " then 'Contact Center' else t.nombre end as nombretienda, "
					+ "r.idcategoria, c.nombre as nombrecategoria, c.orden, "
					+ "sum(r.cantidad_tienda) as cantidad_tienda, sum(r.cantidad_contactcenter) as cantidad_contactcenter, "
					+ "sum(r.cantidad_total) as cantidad_total "
					+ "from venta_integral_resumen_semanal r "
					+ "left join tienda t on t.idtienda = r.idtienda and r.idtienda <> " + IDTIENDA_CONTACTCENTER + " "
					+ "join venta_integral_categoria c on c.idcategoria = r.idcategoria "
					+ "where r.semanainicio >= '" + semanaIniISO + "' and r.semanafin <= '" + semanaFinISO + "' "
					+ "and (r.idtienda = " + IDTIENDA_CONTACTCENTER + " or (t.hosbd is not null and t.hosbd <> ''))"
					+ (idTienda > 0 ? " and r.idtienda = " + idTienda : "")
					+ " group by r.idtienda, nombretienda, r.idcategoria, c.nombre, c.orden order by r.idtienda = "
					+ IDTIENDA_CONTACTCENTER + ", nombretienda, c.orden";
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

	/**
	 * Tiendas (hosbd no vacio, sin contar Contact Center) cuya suma de
	 * cantidad_tienda en el rango dio cero en TODAS las categorias: alerta de
	 * "aparentemente no trajo datos" para que se evalue un reproceso. Es un
	 * indicio, no una certeza -una tienda puede legitimamente no vender nada de
	 * venta integral en una semana-, por eso "aparentemente".
	 */
	public static ArrayList<String> consultarTiendasSinDatosAparentes(String semanaIniISO, String semanaFinISO) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<String> tiendas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select t.nombre from venta_integral_resumen_semanal r "
					+ "join tienda t on t.idtienda = r.idtienda "
					+ "where r.idtienda <> " + IDTIENDA_CONTACTCENTER + " and t.hosbd is not null and t.hosbd <> '' "
					+ "and r.semanainicio >= '" + semanaIniISO + "' and r.semanafin <= '" + semanaFinISO + "' "
					+ "group by r.idtienda, t.nombre having sum(r.cantidad_tienda) = 0 order by t.nombre";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				tiendas.add(rs.getString("nombre"));
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
		return (tiendas);
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

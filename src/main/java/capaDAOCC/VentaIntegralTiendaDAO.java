package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import capaModeloCC.VentaIntegralCategoria;
import conexionCC.ConexionBaseDatos;

/**
 * Lado "tienda" de Venta Integral, calculado EN VIVO contra la base local de
 * cada tienda, para un rango de fechas cualquiera.
 *
 * El proceso semanal de Servicios (CapaDAOSer.VentaIntegralTiendaDAO) hace el
 * mismo calculo pero solo de lunes a domingo y deja el resultado guardado en
 * venta_integral_resumen_semanal. Esta copia vive en el central porque la
 * pantalla de Reportes de Mercadeo necesita rangos libres -la semana en curso,
 * del 3 al 9, un mes- que el proceso nunca cerro. El SQL es el mismo a
 * proposito: si se cambia alla hay que cambiarlo aca, o las dos pestanas
 * dejarian de dar lo mismo para una semana completa.
 *
 * Dos diferencias con la version de Servicios, las dos por ser interactivo:
 *
 * 1. Resuelve TODAS las categorias con UNA sola conexion por tienda. El proceso
 *    semanal abre una por categoria, que con 7 categorias y 11 tiendas son 77
 *    conexiones; aqui son 11. Con alguien esperando frente a la pantalla eso es
 *    la diferencia entre segundos y minutos.
 * 2. Distingue "la tienda contesto y dio cero" de "la tienda no contesto". El
 *    proceso devuelve 0 en los dos casos, que para un cierre nocturno da igual
 *    pero en pantalla seria mentir: un cero de una tienda apagada se veria como
 *    una tienda que no vendio nada.
 */
public class VentaIntegralTiendaDAO {

	/** Lo que dio una tienda, y si de verdad contesto. */
	public static class ResultadoTienda {
		public boolean respondio = false;
		public HashMap<Integer, Double> porCategoria = new HashMap<Integer, Double>();
	}

	public static ResultadoTienda consultarTotalesTienda(String hostBD, ArrayList<VentaIntegralCategoria> categorias,
			String fechaIniISO, String fechaFinISO) {
		Logger logger = Logger.getLogger("log_file");
		ResultadoTienda resultado = new ResultadoTienda();
		if (hostBD == null || hostBD.trim().isEmpty()) {
			return (resultado);
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDTiendaRemota(hostBD);
		if (con1 == null) {
			logger.info("VentaIntegralTiendaDAO: no respondio la tienda " + hostBD);
			return (resultado);
		}
		try {
			Statement stm = con1.createStatement();
			for (VentaIntegralCategoria categoria : categorias) {
				String consulta = armarConsulta(categoria, fechaIniISO, fechaFinISO);
				if (consulta.isEmpty()) {
					resultado.porCategoria.put(categoria.getIdCategoria(), 0.0);
					continue;
				}
				double total = 0;
				ResultSet rs = stm.executeQuery(consulta);
				if (rs.next()) {
					total = rs.getDouble("total");
				}
				rs.close();
				resultado.porCategoria.put(categoria.getIdCategoria(), total);
			}
			stm.close();
			//Solo aqui se marca que respondio: si reviento a mitad de las
			//categorias, lo que alcanzo a traer esta incompleto y mostrarlo
			//seria peor que decir que no contesto.
			resultado.respondio = true;
		} catch (Exception e) {
			logger.info("VentaIntegralTiendaDAO: fallo consultando " + hostBD + ": " + e);
			resultado.respondio = false;
			resultado.porCategoria.clear();
		}
		try {
			con1.close();
		} catch (Exception e1) {
		}
		return (resultado);
	}

	/** El mismo SQL del proceso semanal. Vacio si la categoria no tiene items de tienda. */
	private static String armarConsulta(VentaIntegralCategoria categoria, String fechaIniISO, String fechaFinISO) {
		String listaValores = listaSeparadaPorComas(categoria.getItemsTienda());
		if (listaValores.isEmpty()) {
			return ("");
		}
		String fechaInicial = fechaIniISO + " 00:00:00";
		String fechaFinal = fechaFinISO + " 23:59:59";
		boolean excluyeAnulados = categoria.isExcluyeAnuladosTienda();
		String filtroEstacion = categoria.getFiltroEstacionTienda();
		if (VentaIntegralCategoria.MEDICION_TIENDA_SUMA_CANTIDAD.equals(categoria.getMedicionTienda())) {
			return ("select sum(d.cantidad) as total from pedido a, detalle_pedido d "
					+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
					+ "and a.idpedidotienda = d.idpedidotienda "
					+ (excluyeAnulados ? "and d.idmotivoanulacion is null " : "")
					+ "and d.idproducto in (" + listaValores + ") and a.estacion like '" + filtroEstacion + "'");
		}
		return ("select count(*) as total from pedido a, detalle_pedido b "
				+ "where a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' "
				+ "and a.idpedidotienda = b.idpedidotienda "
				+ (excluyeAnulados ? "and b.idmotivoanulacion is null " : "")
				+ "and a.estacion like '" + filtroEstacion + "' and b.idproducto in (" + listaValores + ")");
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

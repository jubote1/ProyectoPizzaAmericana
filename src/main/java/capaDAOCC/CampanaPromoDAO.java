package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Campanas de promocion: "por la compra del producto X, en estas tiendas,
 * estos dias, preguntarle al cliente si quiere el premio".
 *
 * Reemplaza tener cada promocion de ese tipo escrita en el codigo del POS y del
 * central. El primer caso es la boleta de Cinepolis por el Combo Insuperable,
 * pero aqui no hay nada de Cinepolis: la proxima promocion son cuatro filas.
 *
 * DOS CATALOGOS DE PRODUCTO, NO UNO
 *
 * Lo que dispara la pregunta no se identifica igual en los dos lados, y esa es
 * la razon de que exista la columna ambito:
 *
 *   En la TIENDA el Combo Insuperable es un producto propio (480, 481).
 *   En el CENTRAL ese producto NO existe: es una excepcion de precio sobre la
 *   pizza base (excepcion 50 sobre Pizza XL, 51 sobre Pizza GD).
 *
 * Ver sql/campana_promo.sql.
 */
public class CampanaPromoDAO {

	public static final String AMBITO_TIENDA = "TIENDA";
	public static final String AMBITO_CONTACTCENTER = "CONTACTCENTER";

	/** Una campana con todo lo que la define. */
	public static class Campana {
		public int idCampana = 0;
		public String nombre = "";
		public boolean activo = false;
		public String fechaDesde = "";
		public String fechaHasta = "";
		public String pregunta = "";
		public int idOferta = 0;
		public boolean exigeCorreo = true;
		public ArrayList<Integer> tiendas = new ArrayList<Integer>();
		public ArrayList<Dia> dias = new ArrayList<Dia>();
		public ArrayList<Producto> productos = new ArrayList<Producto>();
	}

	/** dia_semana va 1=LUNES..7=DOMINGO, igual que la campana de 15 minutos. */
	public static class Dia {
		public int diaSemana = 0;
		public boolean activo = true;
		public boolean todoElDia = true;
		public String horaDesde = "";
		public String horaHasta = "";
	}

	/** Lo que dispara la pregunta, en el catalogo de su ambito. */
	public static class Producto {
		public int idItem = 0;
		public String ambito = AMBITO_TIENDA;
		public int idProducto = 0;
		/** Solo para CONTACTCENTER. Cero = cualquier excepcion. */
		public int idExcepcion = 0;
		public boolean activo = true;
	}

	private static String texto(String s) {
		return (s == null ? "" : s);
	}

	private static Campana mapear(ResultSet rs) throws Exception {
		Campana c = new Campana();
		c.idCampana = rs.getInt("idcampana");
		c.nombre = texto(rs.getString("nombre"));
		c.activo = "S".equals(rs.getString("activo"));
		c.fechaDesde = texto(rs.getString("fecha_desde"));
		c.fechaHasta = texto(rs.getString("fecha_hasta"));
		c.pregunta = texto(rs.getString("pregunta"));
		c.idOferta = rs.getInt("idoferta");
		c.exigeCorreo = "S".equals(rs.getString("exige_correo"));
		return (c);
	}

	/** Todas las campanas, sin sus satelites. Para el listado de la pantalla. */
	public static ArrayList<Campana> listar() {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Campana> campanas = new ArrayList<Campana>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return (campanas);
		}
		try {
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery("select * from campana_promo order by activo desc, idcampana desc");
			while (rs.next()) {
				campanas.add(mapear(rs));
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error("CampanaPromoDAO.listar: " + e);
			try { con1.close(); } catch (Exception e1) { }
		}
		return (campanas);
	}

	/** Una campana con sus tiendas, dias y productos. Null si no existe. */
	public static Campana obtener(int idCampana) {
		Logger logger = Logger.getLogger("log_file");
		Campana campana = null;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return (null);
		}
		try {
			PreparedStatement pst = con1.prepareStatement("select * from campana_promo where idcampana = ?");
			pst.setInt(1, idCampana);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				campana = mapear(rs);
			}
			rs.close();
			pst.close();

			if (campana != null) {
				PreparedStatement pt = con1.prepareStatement(
						"select idtienda from campana_promo_tienda where idcampana = ? order by idtienda");
				pt.setInt(1, idCampana);
				ResultSet rt = pt.executeQuery();
				while (rt.next()) {
					campana.tiendas.add(Integer.valueOf(rt.getInt("idtienda")));
				}
				rt.close();
				pt.close();

				PreparedStatement pd = con1.prepareStatement(
						"select * from campana_promo_dia where idcampana = ? order by dia_semana");
				pd.setInt(1, idCampana);
				ResultSet rd = pd.executeQuery();
				while (rd.next()) {
					Dia d = new Dia();
					d.diaSemana = rd.getInt("dia_semana");
					d.activo = "S".equals(rd.getString("activo"));
					d.todoElDia = "S".equals(rd.getString("todo_el_dia"));
					d.horaDesde = texto(rd.getString("hora_desde"));
					d.horaHasta = texto(rd.getString("hora_hasta"));
					campana.dias.add(d);
				}
				rd.close();
				pd.close();

				PreparedStatement pp = con1.prepareStatement(
						"select * from campana_promo_producto where idcampana = ? order by ambito, idproducto");
				pp.setInt(1, idCampana);
				ResultSet rp = pp.executeQuery();
				while (rp.next()) {
					Producto p = new Producto();
					p.idItem = rp.getInt("iditem");
					p.ambito = texto(rp.getString("ambito"));
					p.idProducto = rp.getInt("idproducto");
					p.idExcepcion = rp.getInt("idexcepcion");
					p.activo = "S".equals(rp.getString("activo"));
					campana.productos.add(p);
				}
				rp.close();
				pp.close();
			}
			con1.close();
		} catch (Exception e) {
			logger.error("CampanaPromoDAO.obtener: " + e);
			try { con1.close(); } catch (Exception e1) { }
		}
		return (campana);
	}

	/**
	 * Crea o actualiza una campana con todos sus satelites.
	 *
	 * Los satelites se borran y se vuelven a escribir en vez de compararse uno a
	 * uno: son pocas filas y asi no queda ninguna huerfana de una edicion
	 * anterior. Todo en una transaccion, porque una campana a la que se le
	 * borraron las tiendas y no se le alcanzaron a escribir las nuevas no
	 * aplicaria en ninguna parte, y eso en una promocion viva se nota tarde.
	 *
	 * @return el idcampana guardado, o 0 si fallo
	 */
	public static int guardar(Campana c) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return (0);
		}
		int idCampana = c.idCampana;
		try {
			con1.setAutoCommit(false);

			if (idCampana > 0) {
				PreparedStatement pu = con1.prepareStatement("update campana_promo set nombre = ?, activo = ?,"
						+ " fecha_desde = ?, fecha_hasta = ?, pregunta = ?, idoferta = ?, exige_correo = ?"
						+ " where idcampana = ?");
				pu.setString(1, c.nombre);
				pu.setString(2, c.activo ? "S" : "N");
				ponerFecha(pu, 3, c.fechaDesde);
				ponerFecha(pu, 4, c.fechaHasta);
				pu.setString(5, c.pregunta);
				pu.setInt(6, c.idOferta);
				pu.setString(7, c.exigeCorreo ? "S" : "N");
				pu.setInt(8, idCampana);
				pu.executeUpdate();
				pu.close();
			} else {
				PreparedStatement pi = con1.prepareStatement("insert into campana_promo (nombre, activo,"
						+ " fecha_desde, fecha_hasta, pregunta, idoferta, exige_correo) values (?,?,?,?,?,?,?)",
						Statement.RETURN_GENERATED_KEYS);
				pi.setString(1, c.nombre);
				pi.setString(2, c.activo ? "S" : "N");
				ponerFecha(pi, 3, c.fechaDesde);
				ponerFecha(pi, 4, c.fechaHasta);
				pi.setString(5, c.pregunta);
				pi.setInt(6, c.idOferta);
				pi.setString(7, c.exigeCorreo ? "S" : "N");
				pi.executeUpdate();
				ResultSet gk = pi.getGeneratedKeys();
				if (gk.next()) {
					idCampana = gk.getInt(1);
				}
				gk.close();
				pi.close();
			}

			if (idCampana <= 0) {
				con1.rollback();
				con1.close();
				return (0);
			}

			borrar(con1, "delete from campana_promo_tienda where idcampana = ?", idCampana);
			PreparedStatement pt = con1.prepareStatement(
					"insert into campana_promo_tienda (idcampana, idtienda) values (?,?)");
			for (Integer idTienda : c.tiendas) {
				pt.setInt(1, idCampana);
				pt.setInt(2, idTienda.intValue());
				pt.addBatch();
			}
			pt.executeBatch();
			pt.close();

			borrar(con1, "delete from campana_promo_dia where idcampana = ?", idCampana);
			PreparedStatement pd = con1.prepareStatement("insert into campana_promo_dia (idcampana, dia_semana,"
					+ " activo, todo_el_dia, hora_desde, hora_hasta) values (?,?,?,?,?,?)");
			for (Dia d : c.dias) {
				pd.setInt(1, idCampana);
				pd.setInt(2, d.diaSemana);
				pd.setString(3, d.activo ? "S" : "N");
				pd.setString(4, d.todoElDia ? "S" : "N");
				ponerHora(pd, 5, d.todoElDia ? "" : d.horaDesde);
				ponerHora(pd, 6, d.todoElDia ? "" : d.horaHasta);
				pd.addBatch();
			}
			pd.executeBatch();
			pd.close();

			borrar(con1, "delete from campana_promo_producto where idcampana = ?", idCampana);
			PreparedStatement pp = con1.prepareStatement("insert into campana_promo_producto (idcampana, ambito,"
					+ " idproducto, idexcepcion, activo) values (?,?,?,?,?)");
			for (Producto p : c.productos) {
				pp.setInt(1, idCampana);
				pp.setString(2, p.ambito);
				pp.setInt(3, p.idProducto);
				pp.setInt(4, p.idExcepcion);
				pp.setString(5, p.activo ? "S" : "N");
				pp.addBatch();
			}
			pp.executeBatch();
			pp.close();

			con1.commit();
			con1.setAutoCommit(true);
			con1.close();
			return (idCampana);
		} catch (Exception e) {
			logger.error("CampanaPromoDAO.guardar: " + e);
			System.out.println("CampanaPromoDAO.guardar: " + e);
			try { con1.rollback(); } catch (Exception e1) { }
			try { con1.close(); } catch (Exception e1) { }
			return (0);
		}
	}

	private static void borrar(Connection con1, String sql, int idCampana) throws Exception {
		PreparedStatement p = con1.prepareStatement(sql);
		p.setInt(1, idCampana);
		p.executeUpdate();
		p.close();
	}

	/** Una fecha vacia va como NULL, que en estas tablas significa "sin limite". */
	private static void ponerFecha(PreparedStatement pst, int indice, String fecha) throws Exception {
		if (fecha == null || fecha.trim().length() < 10) {
			pst.setNull(indice, java.sql.Types.DATE);
		} else {
			pst.setString(indice, fecha.trim());
		}
	}

	private static void ponerHora(PreparedStatement pst, int indice, String hora) throws Exception {
		if (hora == null || hora.trim().length() < 4) {
			pst.setNull(indice, java.sql.Types.TIME);
		} else {
			pst.setString(indice, hora.trim());
		}
	}

	/**
	 * Borra una campana y sus satelites.
	 *
	 * NO borra pedido_campana_promo: esas filas son el registro de lo que se le
	 * prometio a un cliente, y perderlas porque alguien limpio una campana vieja
	 * seria perder la prueba de una boleta que no se entrego.
	 */
	public static boolean eliminar(int idCampana) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return (false);
		}
		try {
			con1.setAutoCommit(false);
			borrar(con1, "delete from campana_promo_producto where idcampana = ?", idCampana);
			borrar(con1, "delete from campana_promo_dia where idcampana = ?", idCampana);
			borrar(con1, "delete from campana_promo_tienda where idcampana = ?", idCampana);
			borrar(con1, "delete from campana_promo where idcampana = ?", idCampana);
			con1.commit();
			con1.setAutoCommit(true);
			con1.close();
			return (true);
		} catch (Exception e) {
			logger.error("CampanaPromoDAO.eliminar: " + e);
			try { con1.rollback(); } catch (Exception e1) { }
			try { con1.close(); } catch (Exception e1) { }
			return (false);
		}
	}

	/** Cuantos pedidos quedaron marcados en una campana, por respuesta y estado. */
	public static ArrayList<String[]> resumenMarcados(int idCampana) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<String[]> filas = new ArrayList<String[]>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return (filas);
		}
		try {
			PreparedStatement pst = con1.prepareStatement("select solicito, estado, count(*) cuantos"
					+ " from pedido_campana_promo where idcampana = ? group by solicito, estado"
					+ " order by solicito desc, estado");
			pst.setInt(1, idCampana);
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				filas.add(new String[] { texto(rs.getString("solicito")), texto(rs.getString("estado")),
						String.valueOf(rs.getInt("cuantos")) });
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("CampanaPromoDAO.resumenMarcados: " + e);
			try { con1.close(); } catch (Exception e1) { }
		}
		return (filas);
	}

}

package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia en memoria del catalogo del POS (esquema pos_central) que necesita el motor: productos con sus
 * precios, preguntas forzadas con sus opciones, productos incluidos, horarios por dia y canal, y descuentos
 * generales. Se carga una vez y no se modifica; para refrescarlo se carga otra instancia.
 *
 * Las columnas de precio de cada opcion vienen como TEXTO ("precio10"); a veces traen saltos de linea
 * escondidos, por eso se limpian y se validan contra una lista fija antes de usarlas.
 */
public class CatalogoPos {

	public static class Producto {
		public int id;
		public String descripcion;
		public String tipo;
		public int precioPila;
		public int noVenta;
		public int idProductoDepende;
		public double precioPuntos;
		public String promocion;
		/** precio1..precio10 por nombre de columna. */
		public Map<String, Double> precios = new HashMap<String, Double>();
		/** Ids de las preguntas forzadas, en orden (0 = sin pregunta). */
		public int[] preguntas = new int[5];

		public double precio(String columna) {
			Double v = precios.get(columna);
			return v == null ? 0 : v;
		}
	}

	public static class Pregunta {
		public int id;
		public String titulo;
		public int obliga;
		public int minimo;
		public int maximo;
		public int permiteDividir;
		/** Opciones: id de producto -> nombre de la columna de precio. */
		public Map<Integer, String> opciones = new HashMap<Integer, String>();
	}

	public static class Incluido {
		/** Producto que se agrega. */
		public int idProductoQueSeAgrega;
		public double cantidad;
		public String columnaPrecio;
	}

	public static class Horario {
		public int idproducto;
		public String canal = ""; // vacio = tienda / base
		/** indexado 1..7 (1=Domingo .. 7=Sabado), compatible con Calendar.DAY_OF_WEEK */
		public boolean[] activo = new boolean[8];
		public String[] horaInicial = new String[8];
		public String[] horaFinal = new String[8];

		public boolean estaActivo(int diaCalendar, String horaHHmm) {
			if (diaCalendar < 1 || diaCalendar > 7) {
				return true;
			}
			if (!activo[diaCalendar]) {
				return false;
			}
			String ini = horaInicial[diaCalendar];
			String fin = horaFinal[diaCalendar];
			if (ini == null || fin == null) {
				return true;
			}
			ini = ini.trim();
			fin = fin.trim();
			if (ini.isEmpty() || fin.isEmpty() || (ini.equals("00:00") && fin.equals("23:59"))
					|| (ini.equals("00:00") && fin.equals("00:00"))) {
				return true;
			}
			if (horaHHmm == null || horaHHmm.trim().isEmpty()) {
				return true;
			}
			String h = horaHHmm.trim();
			return h.compareTo(ini) >= 0 && h.compareTo(fin) <= 0;
		}
	}

	public static class DescuentoGeneral {
		public int idDescuento;
		public int idProducto;
		public int valorPorcentaje;
		public double valorPesos;
		public String tipoDescuento = "P";
		public String horaInicio = "00:00";
		public String horaFinal = "00:00";

		public boolean enHorario(String horaHHmm) {
			if (horaInicio == null || horaFinal == null) {
				return true;
			}
			String ini = horaInicio.trim();
			String fin = horaFinal.trim();
			if (ini.isEmpty() || fin.isEmpty() || (ini.equals("00:00") && fin.equals("00:00"))
					|| (ini.equals("00:00") && fin.equals("23:59"))) {
				return true;
			}
			if (horaHHmm == null || horaHHmm.trim().isEmpty()) {
				return true;
			}
			String h = horaHHmm.trim();
			return h.compareTo(ini) >= 0 && h.compareTo(fin) <= 0;
		}
	}

	public final Map<Integer, Producto> productos = new HashMap<Integer, Producto>();
	public final Map<Integer, Pregunta> preguntas = new HashMap<Integer, Pregunta>();
	/** Clave: producto que se vende; valor: lo que se agrega junto con el (tabla producto_incluido). */
	public final Map<Integer, List<Incluido>> incluidos = new HashMap<Integer, List<Incluido>>();
	/** Horario por defecto (tienda). */
	public final Map<Integer, Horario> horarios = new HashMap<Integer, Horario>();
	/** Horario especifico por canal. Clave: "CANAL|idproducto". */
	public final Map<String, Horario> horariosCanal = new HashMap<String, Horario>();
	public final List<DescuentoGeneral> descuentosGenerales = new ArrayList<DescuentoGeneral>();
	public final long cargadoEn = System.currentTimeMillis();

	private static final String[] COLUMNAS = { "precio1", "precio2", "precio3", "precio4", "precio5", "precio6", "precio7",
			"precio8", "precio9", "precio10" };

	/** Deja solo una columna de precio valida ("precio3"); si no es valida devuelve "precio1". */
	public static String limpiarColumna(String texto) {
		if (texto == null) {
			return "precio1";
		}
		String t = texto.replaceAll("[\\r\\n\\t ]", "").toLowerCase();
		for (String c : COLUMNAS) {
			if (c.equals(t)) {
				return c;
			}
		}
		return "precio1";
	}

	public Horario obtenerHorario(String canal, int idProducto) {
		if (canal != null && !canal.trim().isEmpty()) {
			Horario hCanal = horariosCanal.get(canal.trim().toUpperCase() + "|" + idProducto);
			if (hCanal != null) {
				return hCanal;
			}
		}
		return horarios.get(idProducto);
	}

	public static CatalogoPos cargar(Connection con) throws Exception {
		CatalogoPos cat = new CatalogoPos();
		StringBuilder sel = new StringBuilder("select idproducto, descripcion, tipo_producto, precio_pila, no_venta, "
				+ "id_producto_depende, precio_puntos, promocion, "
				+ "idpreguntaforzada1, idpreguntaforzada2, idpreguntaforzada3, idpreguntaforzada4, idpreguntaforzada5");
		for (String c : COLUMNAS) {
			sel.append(", ").append(c);
		}
		sel.append(" from pos_central.producto");
		try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sel.toString())) {
			while (rs.next()) {
				Producto p = new Producto();
				p.id = rs.getInt(1);
				p.descripcion = rs.getString(2);
				p.tipo = rs.getString(3);
				p.precioPila = rs.getInt(4);
				p.noVenta = rs.getInt(5);
				p.idProductoDepende = rs.getInt(6);
				p.precioPuntos = rs.getDouble(7);
				p.promocion = rs.getString(8);
				for (int i = 0; i < 5; i++) {
					p.preguntas[i] = rs.getInt(9 + i);
				}
				for (int i = 0; i < COLUMNAS.length; i++) {
					p.precios.put(COLUMNAS[i], rs.getDouble(14 + i));
				}
				cat.productos.put(p.id, p);
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idpregunta, titulopregunta, obliga_eleccion, numero_minimo_eleccion, "
						+ "numero_maximo_eleccion, permite_dividir from pos_central.pregunta")) {
			while (rs.next()) {
				Pregunta q = new Pregunta();
				q.id = rs.getInt(1);
				q.titulo = rs.getString(2);
				q.obliga = rs.getInt(3);
				q.minimo = rs.getInt(4);
				q.maximo = rs.getInt(5);
				q.permiteDividir = rs.getInt(6);
				cat.preguntas.put(q.id, q);
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idpregunta, idproducto, precio from pos_central.eleccion_forzada "
						+ "where estado = 1 order by ideleccion_forzada")) {
			while (rs.next()) {
				Pregunta q = cat.preguntas.get(rs.getInt(1));
				if (q != null && !q.opciones.containsKey(rs.getInt(2))) {
					q.opciones.put(rs.getInt(2), limpiarColumna(rs.getString(3)));
				}
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idproductoincluido, idproductoincluye, cantidad, precio "
						+ "from pos_central.producto_incluido")) {
			while (rs.next()) {
				Incluido inc = new Incluido();
				inc.idProductoQueSeAgrega = rs.getInt(2);
				inc.cantidad = rs.getDouble(3);
				inc.columnaPrecio = limpiarColumna(rs.getString(4));
				List<Incluido> lista = cat.incluidos.get(rs.getInt(1));
				if (lista == null) {
					lista = new ArrayList<Incluido>();
					cat.incluidos.put(rs.getInt(1), lista);
				}
				lista.add(inc);
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idproducto, lunes, hora_inicial_l, hora_final_l, "
						+ "martes, hora_inicial_m, hora_final_m, miercoles, hora_inicial_mi, hora_final_mi, "
						+ "jueves, hora_inicial_j, hora_final_j, viernes, hora_inicial_v, hora_final_v, "
						+ "sabado, hora_inicial_s, hora_final_s, domingo, hora_inicial_d, hora_final_d "
						+ "from pos_central.producto_horario")) {
			while (rs.next()) {
				Horario h = new Horario();
				h.idproducto = rs.getInt(1);
				h.canal = "";
				leerFilaHorario(rs, h, 2);
				cat.horarios.put(h.idproducto, h);
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idproducto, canal, lunes, hora_inicial_l, hora_final_l, "
						+ "martes, hora_inicial_m, hora_final_m, miercoles, hora_inicial_mi, hora_final_mi, "
						+ "jueves, hora_inicial_j, hora_final_j, viernes, hora_inicial_v, hora_final_v, "
						+ "sabado, hora_inicial_s, hora_final_s, domingo, hora_inicial_d, hora_final_d "
						+ "from pos_central.producto_horario_canal")) {
			while (rs.next()) {
				Horario h = new Horario();
				h.idproducto = rs.getInt(1);
				h.canal = rs.getString(2) == null ? "" : rs.getString(2).trim();
				leerFilaHorario(rs, h, 3);
				cat.horariosCanal.put(h.canal.toUpperCase() + "|" + h.idproducto, h);
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select iddescuento, idproducto, valor_porcentaje, valor_pesos, "
						+ "tipo_descuento, hora_inicio, hora_final from pos_central.descuentos_generales")) {
			while (rs.next()) {
				DescuentoGeneral dg = new DescuentoGeneral();
				dg.idDescuento = rs.getInt(1);
				dg.idProducto = rs.getInt(2);
				dg.valorPorcentaje = rs.getInt(3);
				dg.valorPesos = rs.getDouble(4);
				dg.tipoDescuento = rs.getString(5) == null ? "P" : rs.getString(5).trim();
				dg.horaInicio = rs.getString(6) == null ? "00:00" : rs.getString(6).trim();
				dg.horaFinal = rs.getString(7) == null ? "00:00" : rs.getString(7).trim();
				cat.descuentosGenerales.add(dg);
			}
		}
		return cat;
	}

	private static void leerFilaHorario(ResultSet rs, Horario h, int offsetCol) throws Exception {
		int c = offsetCol;
		// lunes (Calendar.MONDAY = 2)
		h.activo[2] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[2] = rs.getString(c++);
		h.horaFinal[2] = rs.getString(c++);
		// martes (Calendar.TUESDAY = 3)
		h.activo[3] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[3] = rs.getString(c++);
		h.horaFinal[3] = rs.getString(c++);
		// miercoles (Calendar.WEDNESDAY = 4)
		h.activo[4] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[4] = rs.getString(c++);
		h.horaFinal[4] = rs.getString(c++);
		// jueves (Calendar.THURSDAY = 5)
		h.activo[5] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[5] = rs.getString(c++);
		h.horaFinal[5] = rs.getString(c++);
		// viernes (Calendar.FRIDAY = 6)
		h.activo[6] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[6] = rs.getString(c++);
		h.horaFinal[6] = rs.getString(c++);
		// sabado (Calendar.SATURDAY = 7)
		h.activo[7] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[7] = rs.getString(c++);
		h.horaFinal[7] = rs.getString(c++);
		// domingo (Calendar.SUNDAY = 1)
		h.activo[1] = "S".equalsIgnoreCase(rs.getString(c++));
		h.horaInicial[1] = rs.getString(c++);
		h.horaFinal[1] = rs.getString(c++);
	}
}

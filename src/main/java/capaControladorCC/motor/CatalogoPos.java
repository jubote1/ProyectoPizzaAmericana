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
 * precios, preguntas forzadas con sus opciones, y productos incluidos. Se carga una vez y no se modifica;
 * para refrescarlo se carga otra instancia.
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

	public final Map<Integer, Producto> productos = new HashMap<Integer, Producto>();
	public final Map<Integer, Pregunta> preguntas = new HashMap<Integer, Pregunta>();
	/** Clave: producto que se vende; valor: lo que se agrega junto con el (tabla producto_incluido). */
	public final Map<Integer, List<Incluido>> incluidos = new HashMap<Integer, List<Incluido>>();
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

	public static CatalogoPos cargar(Connection con) throws Exception {
		CatalogoPos cat = new CatalogoPos();
		StringBuilder sel = new StringBuilder("select idproducto, descripcion, tipo_producto, precio_pila, no_venta, "
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
				for (int i = 0; i < 5; i++) {
					p.preguntas[i] = rs.getInt(6 + i);
				}
				for (int i = 0; i < COLUMNAS.length; i++) {
					p.precios.put(COLUMNAS[i], rs.getDouble(11 + i));
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
		return cat;
	}
}

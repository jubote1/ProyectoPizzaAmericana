package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaModeloCC.motor.MotorSolicitud;

/**
 * Traductor del BOT (paquete 2.7): convierte el lead que arma el BOT en Kommo -texto libre: nombre del
 * combo, sabores, adicion, bebida, acompanante- en una solicitud al motor con productos del POS.
 *
 * Hace el mismo recorrido que hoy hace ConfirmarPedidoCRMBOT, pero termina en el catalogo del POS:
 *   texto del BOT --(homologacion_producto_pedidovirtual: SKU exacto)--> id viejo del central
 *   id viejo --(pos_central.equivalencia_producto)--> producto del POS.
 *
 * Lo que no se pueda traducir NO se inventa: queda en solicitud.avisosTraduccion y el renglon se omite.
 * Asi se puede medir cuanto falla la traduccion.
 *
 * Solo lee (pizzaamericana y pos_central); no escribe nada.
 */
public class TraductorBot {

	/** Producto del POS que se cobra como domicilio. */
	public static final int PRODUCTO_DOMICILIO = 412;

	private final CatalogoPos catalogo;
	/** "tipo|sku normalizado" -> idinterno. */
	private final Map<String, Integer> skus = new HashMap<String, Integer>();
	/** idexcepcion -> producto base del central. */
	private final Map<Integer, Integer> productoDeExcepcion = new HashMap<Integer, Integer>();
	/** nombre de tienda normalizado -> idtienda. */
	private final Map<String, Integer> tiendas = new HashMap<String, Integer>();
	/** Promociones que controlan la cantidad de ingredientes (los combos MAX). */
	private final java.util.Set<Integer> excControlIngredientes = new java.util.HashSet<Integer>();
	private final Map<String, Integer> eqProducto = new HashMap<String, Integer>();
	private final Map<String, Integer> eqEspecialidad = new HashMap<String, Integer>();
	private final Map<String, Integer> eqExcepcion = new HashMap<String, Integer>();
	private final Map<Integer, Integer> eqSabor = new HashMap<Integer, Integer>();

	private final Connection con;

	public TraductorBot(Connection con, CatalogoPos catalogo) throws Exception {
		this.con = con;
		this.catalogo = catalogo;
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select tipo, sku, idinterno from pizzaamericana.homologacion_producto_pedidovirtual")) {
			while (rs.next()) {
				String clave = rs.getString(1) + "|" + normalizar(rs.getString(2));
				if (!skus.containsKey(clave)) {
					skus.put(clave, rs.getInt(3));
				}
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idtienda, nombre from pizzaamericana.tienda")) {
			while (rs.next()) {
				tiendas.put(normalizar(rs.getString(2)), rs.getInt(1));
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idexcepcion, idproducto, controla_cantidad_ingredientes "
						+ "from pizzaamericana.excepcion_precio")) {
			while (rs.next()) {
				productoDeExcepcion.put(rs.getInt(1), rs.getInt(2));
				if ("S".equals(rs.getString(3))) {
					excControlIngredientes.add(rs.getInt(1));
				}
			}
		}
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select tipo, idproducto_central, idespecialidad_central, idsabor_central, "
						+ "idexcepcion_central, idproducto_pos from pos_central.equivalencia_producto")) {
			while (rs.next()) {
				String tipo = rs.getString(1);
				int pos = rs.getInt(6);
				if ("PRODUCTO".equals(tipo)) {
					eqProducto.put(String.valueOf(rs.getInt(2)), pos);
				} else if ("ESPECIALIDAD".equals(tipo)) {
					eqEspecialidad.put(rs.getInt(2) + "|" + rs.getInt(3), pos);
				} else if ("EXCEPCION".equals(tipo)) {
					eqExcepcion.put(rs.getInt(2) + "|" + rs.getInt(5), pos);
				} else if ("SABOR".equals(tipo)) {
					eqSabor.put(rs.getInt(4), pos);
				}
			}
		}
	}

	/**
	 * Igual que la comparacion de MySQL en la tabla de SKU: sin mayusculas ni acentos, de modo que lo que manda
	 * el BOT ("Pollo con champinon") encuentre el SKU guardado ("Pollo con Champinon con tilde"). Ademas el BOT
	 * manda "." o "No desea" para decir "nada": se trata como vacio.
	 */
	static String normalizar(String t) {
		if (t == null) {
			return "";
		}
		String sinAcentos = java.text.Normalizer.normalize(t.trim(), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinAcentos.replaceAll("\\s+", " ").toLowerCase();
	}

	/** Un campo del BOT que dice "nada": vacio, un punto, o "no desea". */
	private static boolean vacio(String t) {
		String n = normalizar(t);
		return n.isEmpty() || n.equals(".") || n.startsWith("no desea") || n.equals("ninguno") || n.equals("ninguna");
	}

	private int sku(String tipo, String texto) {
		if (texto == null || texto.trim().isEmpty()) {
			return 0;
		}
		Integer v = skus.get(tipo + "|" + normalizar(texto));
		return v == null ? 0 : v;
	}

	/** Campos del lead que importan, ya leidos. */
	private static class Lead {
		String combo = "", detalles = "", sabor1 = "", sabor2 = "", sabor3 = "", adicion = "", bebida = "",
				acompanamiento = "", bebida2 = "", balon = "", cantidadPizzas = "";
		String combo2 = "", detalles2 = "", sabor1_2 = "", sabor2_2 = "", adicion2 = "", bebida_2 = "", acompanamiento2 = "";
		String tienda = "", condimentos = "";
	}

	private static String valorDe(JsonObject cf) {
		JsonElement v = cf.get("values");
		if (v == null || !v.isJsonArray() || ((JsonArray) v).size() == 0) {
			return "";
		}
		JsonElement primero = ((JsonArray) v).get(0);
		if (primero.isJsonObject() && primero.getAsJsonObject().has("value")
				&& !primero.getAsJsonObject().get("value").isJsonNull()) {
			return primero.getAsJsonObject().get("value").getAsString().replace("'", " ").trim();
		}
		return "";
	}

	private Lead leer(String infoLead) {
		Lead l = new Lead();
		JsonObject raiz = JsonParser.parseString(infoLead).getAsJsonObject();
		JsonElement cfs = raiz.get("custom_fields_values");
		if (cfs == null || !cfs.isJsonArray()) {
			return l;
		}
		for (JsonElement e : cfs.getAsJsonArray()) {
			JsonObject cf = e.getAsJsonObject();
			int id = cf.has("field_id") && !cf.get("field_id").isJsonNull() ? cf.get("field_id").getAsInt() : 0;
			String v = valorDe(cf);
			switch (id) {
			case 868051: l.cantidadPizzas = v; break;
			case 861855: l.combo = v; break;
			case 862081: l.detalles = v.toLowerCase(); break;
			case 857712: l.sabor1 = v; break;
			case 857714: l.sabor2 = v; break;
			case 868957: l.sabor3 = v; break;
			case 861771: l.adicion = v; break;
			case 861775: l.bebida = v; break;
			case 861773: l.acompanamiento = v; break;
			case 862083: l.bebida2 = v; break;
			case 868055: l.combo2 = v; break;
			case 868057: l.detalles2 = v.toLowerCase(); break;
			case 868059: l.sabor1_2 = v; break;
			case 868061: l.sabor2_2 = v; break;
			case 868063: l.adicion2 = v; break;
			case 868065: l.bebida_2 = v; break;
			case 873301: l.acompanamiento2 = v; break;
			case 872191: l.balon = v; break;
			case 862153: l.tienda = v; break;
			case 867749: l.condimentos = v; break;
			default: break;
			}
		}
		return l;
	}

	/** Tienda que eligio el lead (campo "Tienda"), como idtienda; 0 si no se reconoce. */
	public int resolverTienda(String infoLead) {
		try {
			Integer id = tiendas.get(normalizar(leer(infoLead).tienda));
			return id == null ? 0 : id;
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Traduce el lead (JSON de Kommo tal como queda en log_pedido_crmbot.info_lead) a una solicitud.
	 *
	 * @param idTienda tienda del pedido si ya se conoce (el pedido existente); 0 si no, y entonces se deja sin tienda.
	 */
	public MotorSolicitud traducir(String infoLead, int idTienda) {
		MotorSolicitud s = new MotorSolicitud();
		s.contexto.canal = "BOT";
		s.contexto.idtienda = idTienda;
		s.contexto.idtipopedido = 1;
		s.contexto.esDomicilio = true;
		Lead l;
		try {
			l = leer(infoLead);
		} catch (Exception e) {
			s.avisosTraduccion.add("LEAD_ILEGIBLE: " + e);
			return s;
		}
		CatalogoPos.Producto dom = catalogo.productos.get(PRODUCTO_DOMICILIO);
		s.domicilio = new MotorSolicitud.Domicilio();
		s.domicilio.idproducto = PRODUCTO_DOMICILIO;
		s.domicilio.valor = dom == null ? 0 : dom.precio("precio1");

		// Balon (promocion del mundial): producto suelto.
		if (!vacio(l.balon)) {
			agregarSuelto(s, l.balon, "balon", false);
		}
		// Condimentos: productos sueltos sin valor ("Todos los Condimentos", "Sal de Ajo"...).
		if (!vacio(l.condimentos)) {
			for (String c : l.condimentos.split(",")) {
				if (!vacio(c)) {
					agregarSuelto(s, c.trim(), "condimento", false);
				}
			}
		}
		if (!l.combo.isEmpty()) {
			armar(s, l.combo, l.sabor1, l.sabor2, l.sabor3, l.adicion, l.bebida, l.detalles, l.acompanamiento, l.bebida2, true);
		}
		boolean segundo = "2".equals(l.cantidadPizzas) || "dos".equalsIgnoreCase(l.cantidadPizzas)
				|| (!l.combo2.isEmpty() && !"1".equals(l.cantidadPizzas));
		if (segundo && !l.combo2.isEmpty()) {
			armar(s, l.combo2, l.sabor1_2, l.sabor2_2, "", l.adicion2, l.bebida_2, l.detalles2, l.acompanamiento2, "", false);
		}
		if (s.lineas.isEmpty()) {
			s.avisosTraduccion.add("SIN_LINEAS: el lead no trajo ningun producto traducible");
		}
		return s;
	}

	private int aPos(String clave, Map<String, Integer> mapa) {
		Integer v = mapa.get(clave);
		return v == null ? 0 : v;
	}

	/** Producto suelto del POS a partir de un texto del BOT (SKU tipo P). Devuelve el id del POS o 0. */
	private int productoPos(String texto) {
		int central = sku("P", texto);
		return central == 0 ? 0 : aPos(String.valueOf(central), eqProducto);
	}

	private void agregarSuelto(MotorSolicitud s, String texto, String que, boolean conPrefijoAdicional) {
		int pos = conPrefijoAdicional ? productoPos("Producto Adicional " + texto) : 0;
		if (pos == 0) {
			pos = productoPos(texto);
		}
		if (pos == 0) {
			s.avisosTraduccion.add("NO_TRADUCIDO " + que + ": " + texto);
			return;
		}
		MotorSolicitud.Linea ln = new MotorSolicitud.Linea();
		ln.idproducto = pos;
		s.lineas.add(ln);
	}

	private void armar(MotorSolicitud s, String combo, String sabor1, String sabor2, String sabor3, String adicion,
			String bebida, String detalles, String acompanamiento, String bebida2, boolean esPrimero) {
		// 1. Producto o promocion
		int idExc = sku("EX", combo);
		boolean esPromo = idExc > 0;
		int centralProd;
		int pos;
		if (esPromo) {
			Integer base = productoDeExcepcion.get(idExc);
			centralProd = base == null ? 0 : base;
			pos = aPos(centralProd + "|" + idExc, eqExcepcion);
		} else {
			centralProd = sku("P", combo);
			pos = centralProd == 0 ? 0 : aPos(String.valueOf(centralProd), eqProducto);
		}
		if (pos == 0) {
			s.avisosTraduccion.add("NO_TRADUCIDO combo: " + combo);
			return;
		}
		MotorSolicitud.Linea ln = new MotorSolicitud.Linea();
		ln.idproducto = pos;
		s.lineas.add(ln);
		CatalogoPos.Producto prod = catalogo.productos.get(pos);

		// 2. Sabores
		String det = detalles == null ? "" : detalles.trim();
		if (det.equals("una sola especialidad") || det.equals("pizza una sola especialidad")) {
			int d = sabor(s, centralProd, sabor1);
			if (d > 0) {
				ln.selecciones.add(seleccion(d, 0.5));
				ln.selecciones.add(seleccion(d, 0.5));
			}
		} else if (det.equals("mitad y mitad") || det.equals("pizza mitad y mitad")) {
			int d1 = sabor(s, centralProd, sabor1);
			int d2 = sabor(s, centralProd, sabor2);
			if (d1 > 0) {
				ln.selecciones.add(seleccion(d1, 0.5));
			}
			if (d2 > 0) {
				ln.selecciones.add(seleccion(d2, 0.5));
			}
		} else if (det.contains("3") || !sabor3.isEmpty()) {
			s.avisosTraduccion.add("TRES_SABORES no soportado: " + sabor1 + " / " + sabor2 + " / " + sabor3);
		} else if (det.isEmpty() && esPromo) {
			// Promocion sin sabores (por ejemplo una Pizza Burger): lo que llega como "sabor" son ingredientes
			// CON, que existen como productos modificadores del producto base.
			for (String ing : new String[] { sabor1, sabor2 }) {
				if (vacio(ing)) {
					continue;
				}
				// Primero el SKU "Con <ingrediente> <EXT|GRA|MED|PIZ>" y, si no, la busqueda por nombre.
				int modCentral = centralProd == 0 ? 0 : sku("P", "Con " + ing.trim() + " " + codigoTamano(centralProd));
				if (modCentral == 0) {
					modCentral = buscarModificadorCon(centralProd, ing);
				}
				int modPos = modCentral == 0 ? 0 : aPos(String.valueOf(modCentral), eqProducto);
				if (modPos == 0) {
					s.avisosTraduccion.add("MODIFICADOR_CON no traducido: " + ing);
				} else {
					MotorSolicitud.Modificador m = new MotorSolicitud.Modificador();
					m.tipo = "CON";
					m.idproducto = modPos;
					m.cantidad = 1;
					m.sobreProducto = pos;
					ln.modificadores.add(m);
				}
			}
		}

		// 3. Adiciones. Si la pregunta de adiciones de la pizza se puede dividir, la adicion de toda la pizza se
		// registra como dos mitades de 0,5 (asi la guarda la pantalla de la tienda).
		if (!vacio(adicion)) {
			for (String a : adicion.split(",")) {
				a = a.trim();
				if (vacio(a)) {
					continue;
				}
				int p = productoPos(a);
				if (p == 0) {
					s.avisosTraduccion.add("NO_TRADUCIDO adicion: " + a);
				} else if (esPromo && excControlIngredientes.contains(idExc)) {
					// En los combos "un ingrediente" (MAX) la tienda registra la adicion entera, no por mitades.
					ln.selecciones.add(seleccion(p, 1));
				} else if (preguntaDivide(prod, p)) {
					ln.selecciones.add(seleccion(p, 0.5));
					ln.selecciones.add(seleccion(p, 0.5));
				} else {
					ln.selecciones.add(seleccion(p, 1));
				}
			}
		}

		// 4. Bebida
		if (!vacio(bebida)) {
			String clave = "Selecciona tu bebida " + bebida + " " + (esPromo ? "Extragrande (12 porciones)" : combo);
			int sab = sku("T", clave);
			int p = sab == 0 ? 0 : aPos(String.valueOf(sab), toStringMap(eqSabor));
			if (p == 0) {
				s.avisosTraduccion.add("NO_TRADUCIDO bebida: " + bebida);
			} else {
				ln.selecciones.add(seleccion(p, 1));
			}
		}

		// 5. Acompanante: eleccion de la pregunta del combo si esta ahi; si no, producto suelto.
		if (!vacio(acompanamiento)) {
			// "Producto Adicional X" es la variante incluida en un combo (valor 0); "X" a secas es la que se cobra.
			// Si el combo ofrece alguna de las dos como opcion, es parte del combo; si no, el acompanante se
			// vende aparte y se cobra completo (asi lo registran las tiendas).
			int libre = productoPos("Producto Adicional " + acompanamiento);
			int pagado = productoPos(acompanamiento);
			int p;
			if (libre != 0 && estaEnPreguntas(prod, libre)) {
				p = libre;
				ln.selecciones.add(seleccion(p, 1));
			} else if (pagado != 0 && estaEnPreguntas(prod, pagado)) {
				p = pagado;
				ln.selecciones.add(seleccion(p, 1));
			} else if (pagado != 0 || libre != 0) {
				p = pagado != 0 ? pagado : libre;
				MotorSolicitud.Linea suelto = new MotorSolicitud.Linea();
				suelto.idproducto = p;
				s.lineas.add(suelto);
			} else {
				p = 0;
				s.avisosTraduccion.add("NO_TRADUCIDO acompanante: " + acompanamiento);
			}
			// Bebida del acompanante (solo en el primer combo)
			if (esPrimero && !vacio(bebida2) && p != 0) {
				int sab2 = sku("T", "Selecciona tu bebida " + bebida2 + " Pizzeta (4 porciones)");
				int p2 = sab2 == 0 ? 0 : aPos(String.valueOf(sab2), toStringMap(eqSabor));
				if (p2 == 0) {
					s.avisosTraduccion.add("NO_TRADUCIDO bebida del acompanante: " + bebida2);
				} else {
					MotorSolicitud.Linea b = new MotorSolicitud.Linea();
					b.idproducto = p2;
					s.lineas.add(b);
				}
			}
		}
	}

	private static Map<String, Integer> toStringMap(Map<Integer, Integer> m) {
		Map<String, Integer> r = new HashMap<String, Integer>();
		for (Map.Entry<Integer, Integer> e : m.entrySet()) {
			r.put(String.valueOf(e.getKey()), e.getValue());
		}
		return r;
	}

	/** Codigo de tamano que usan los SKU de los modificadores CON: producto base del central 6, 7, 8 o 9. */
	private static String codigoTamano(int productoCentral) {
		switch (productoCentral) {
		case 6:
			return "EXT";
		case 7:
			return "GRA";
		case 8:
			return "MED";
		case 9:
			return "PIZ";
		default:
			return "";
		}
	}

	/** True si la opcion esta en una pregunta del producto que se puede dividir en mitades. */
	private boolean preguntaDivide(CatalogoPos.Producto prod, int idOpcion) {
		if (prod == null) {
			return false;
		}
		for (int idPregunta : prod.preguntas) {
			CatalogoPos.Pregunta q = catalogo.preguntas.get(idPregunta);
			if (q != null && q.opciones.containsKey(idOpcion)) {
				return q.permiteDividir >= 2;
			}
		}
		return false;
	}

	/**
	 * Modificador CON del producto base del central por nombre del ingrediente (misma consulta que hoy hace
	 * ConfirmarPedidoCRMBOT). Devuelve el id viejo del central o 0.
	 */
	private int buscarModificadorCon(int idProductoBase, String ingrediente) {
		if (idProductoBase <= 0 || ingrediente == null || ingrediente.trim().isEmpty()) {
			return 0;
		}
		String sql = "select idproducto from pizzaamericana.producto where tipo = 'MODIFICADOR CON' "
				+ "and producto_asocia_adicion = ? and (nombre like ? or descripcion like ?) and habilitado = 'S' limit 1";
		try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
			String patron = "%" + ingrediente.trim() + "%";
			ps.setInt(1, idProductoBase);
			ps.setString(2, patron);
			ps.setString(3, patron);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		} catch (Exception e) {
			return 0;
		}
	}

	private boolean estaEnPreguntas(CatalogoPos.Producto prod, int idOpcion) {
		if (prod == null) {
			return false;
		}
		for (int idPregunta : prod.preguntas) {
			CatalogoPos.Pregunta q = catalogo.preguntas.get(idPregunta);
			if (q != null && q.opciones.containsKey(idOpcion)) {
				return true;
			}
		}
		return false;
	}

	private int sabor(MotorSolicitud s, int centralProd, String sabor) {
		if (sabor == null || sabor.isEmpty()) {
			return 0;
		}
		int esp = sku("E", "Pizza " + sabor);
		if (esp == 0) {
			s.avisosTraduccion.add("NO_TRADUCIDO sabor: Pizza " + sabor);
			return 0;
		}
		int d = aPos(centralProd + "|" + esp, eqEspecialidad);
		if (d == 0) {
			s.avisosTraduccion.add("SABOR_SIN_EQUIVALENCIA para el producto " + centralProd + ": " + sabor);
		}
		return d;
	}

	private static MotorSolicitud.Seleccion seleccion(int idOpcion, double cantidad) {
		MotorSolicitud.Seleccion x = new MotorSolicitud.Seleccion();
		x.idproductoOpcion = idOpcion;
		x.cantidad = cantidad;
		return x;
	}
}

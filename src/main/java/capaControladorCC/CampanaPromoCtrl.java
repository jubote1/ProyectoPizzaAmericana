package capaControladorCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.CampanaPromoDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

/**
 * Pantalla de campanas de promocion.
 *
 * Los satelites -tiendas, dias, productos- viajan desde la pantalla como texto
 * separado por comas y no como JSON anidado. Es a proposito: el resto de los
 * servicios de este proyecto reciben parametros planos, y un JSON anidado en un
 * parametro obliga a parsearlo a mano en el servlet, que es justo donde no se
 * quiere tener logica.
 *
 *   tiendas    "5,7,8"
 *   dias       "1,2,3"                 1=LUNES .. 7=DOMINGO
 *   productos  "TIENDA:480:0,CONTACTCENTER:6:50"   ambito:producto:excepcion
 */
public class CampanaPromoCtrl {

	@SuppressWarnings("unchecked")
	public String listar() {
		JSONArray lista = new JSONArray();
		for (CampanaPromoDAO.Campana c : CampanaPromoDAO.listar()) {
			JSONObject j = new JSONObject();
			j.put("idcampana", c.idCampana);
			j.put("nombre", c.nombre);
			j.put("activo", c.activo ? "S" : "N");
			j.put("fechadesde", c.fechaDesde);
			j.put("fechahasta", c.fechaHasta);
			j.put("pregunta", c.pregunta);
			j.put("idoferta", c.idOferta);
			j.put("exigecorreo", c.exigeCorreo ? "S" : "N");
			lista.add(j);
		}
		return (lista.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String obtener(int idCampana) {
		CampanaPromoDAO.Campana c = CampanaPromoDAO.obtener(idCampana);
		if (c == null) {
			return ("{\"respuesta\":\"NOK\",\"detalle\":\"No existe la campana\"}");
		}
		JSONObject j = new JSONObject();
		j.put("idcampana", c.idCampana);
		j.put("nombre", c.nombre);
		j.put("activo", c.activo ? "S" : "N");
		j.put("fechadesde", c.fechaDesde);
		j.put("fechahasta", c.fechaHasta);
		j.put("pregunta", c.pregunta);
		j.put("idoferta", c.idOferta);
		j.put("exigecorreo", c.exigeCorreo ? "S" : "N");

		JSONArray tiendas = new JSONArray();
		for (Integer idTienda : c.tiendas) {
			tiendas.add(idTienda);
		}
		j.put("tiendas", tiendas);

		JSONArray dias = new JSONArray();
		for (CampanaPromoDAO.Dia d : c.dias) {
			if (d.activo) {
				dias.add(Integer.valueOf(d.diaSemana));
			}
		}
		j.put("dias", dias);

		JSONArray productos = new JSONArray();
		for (CampanaPromoDAO.Producto p : c.productos) {
			JSONObject jp = new JSONObject();
			jp.put("ambito", p.ambito);
			jp.put("idproducto", p.idProducto);
			jp.put("idexcepcion", p.idExcepcion);
			jp.put("nombre", this.nombreDe(p.ambito, p.idProducto, p.idExcepcion, c.tiendas));
			productos.add(jp);
		}
		j.put("productos", productos);

		JSONArray marcados = new JSONArray();
		for (String[] fila : CampanaPromoDAO.resumenMarcados(idCampana)) {
			JSONObject jm = new JSONObject();
			jm.put("solicito", fila[0]);
			jm.put("estado", fila[1]);
			jm.put("cuantos", fila[2]);
			marcados.add(jm);
		}
		j.put("marcados", marcados);
		return (j.toJSONString());
	}

	public String guardar(int idCampana, String nombre, boolean activo, String fechaDesde, String fechaHasta,
			String pregunta, int idOferta, boolean exigeCorreo, String tiendas, String dias, String productos) {
		if (nombre == null || nombre.trim().isEmpty()) {
			return ("{\"respuesta\":\"NOK\",\"detalle\":\"La campana necesita un nombre\"}");
		}
		CampanaPromoDAO.Campana c = new CampanaPromoDAO.Campana();
		c.idCampana = idCampana;
		c.nombre = nombre.trim();
		c.activo = activo;
		c.fechaDesde = fechaDesde == null ? "" : fechaDesde.trim();
		c.fechaHasta = fechaHasta == null ? "" : fechaHasta.trim();
		c.pregunta = pregunta == null ? "" : pregunta.trim();
		c.idOferta = idOferta;
		c.exigeCorreo = exigeCorreo;
		c.tiendas = this.aEnteros(tiendas);
		for (Integer dia : this.aEnteros(dias)) {
			CampanaPromoDAO.Dia d = new CampanaPromoDAO.Dia();
			d.diaSemana = dia.intValue();
			d.activo = true;
			d.todoElDia = true;
			c.dias.add(d);
		}
		c.productos = this.aProductos(productos);

		// Una campana activa sin tiendas, sin dias o sin productos no dispara
		// nunca. Se deja guardar apagada -asi se arma por partes- pero prenderla
		// incompleta es prometer una promocion que no va a salir.
		if (activo) {
			if (c.tiendas.isEmpty()) {
				return ("{\"respuesta\":\"NOK\",\"detalle\":\"Para activarla hay que decir en que tiendas aplica\"}");
			}
			if (c.dias.isEmpty()) {
				return ("{\"respuesta\":\"NOK\",\"detalle\":\"Para activarla hay que escoger al menos un dia\"}");
			}
			if (c.productos.isEmpty()) {
				return ("{\"respuesta\":\"NOK\",\"detalle\":\"Para activarla hay que decir que producto la dispara\"}");
			}
			if (c.pregunta.isEmpty()) {
				return ("{\"respuesta\":\"NOK\",\"detalle\":\"Para activarla hay que escribir la pregunta\"}");
			}
		}

		int guardado = CampanaPromoDAO.guardar(c);
		if (guardado <= 0) {
			return ("{\"respuesta\":\"NOK\",\"detalle\":\"No se pudo guardar\"}");
		}
		return ("{\"respuesta\":\"OK\",\"idcampana\":" + guardado + "}");
	}

	public String eliminar(int idCampana) {
		if (CampanaPromoDAO.eliminar(idCampana)) {
			return ("{\"respuesta\":\"OK\"}");
		}
		return ("{\"respuesta\":\"NOK\",\"detalle\":\"No se pudo eliminar\"}");
	}

	/**
	 * Como se llama lo que el usuario acaba de escribir, para que vea en
	 * pantalla si se equivoco de numero antes de guardar.
	 */
	@SuppressWarnings("unchecked")
	public String consultarNombreProducto(String ambito, int idProducto, int idExcepcion, String tiendas) {
		JSONObject j = new JSONObject();
		j.put("nombre", this.nombreDe(ambito, idProducto, idExcepcion, this.aEnteros(tiendas)));
		return (j.toJSONString());
	}

	/** Las excepciones de precio del central, para escoger de una lista. */
	@SuppressWarnings("unchecked")
	public String listarExcepciones() {
		JSONArray lista = new JSONArray();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return (lista.toJSONString());
		}
		try {
			PreparedStatement pst = con1.prepareStatement("select e.idexcepcion, e.idproducto, e.precio,"
					+ " e.descripcion, p.nombre as producto from excepcion_precio e"
					+ " left join producto p on p.idproducto = e.idproducto"
					+ " order by e.descripcion");
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("idexcepcion", rs.getInt("idexcepcion"));
				j.put("idproducto", rs.getInt("idproducto"));
				j.put("precio", rs.getDouble("precio"));
				j.put("descripcion", rs.getString("descripcion") == null ? "" : rs.getString("descripcion"));
				j.put("producto", rs.getString("producto") == null ? "" : rs.getString("producto"));
				lista.add(j);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			System.out.println("CampanaPromoCtrl.listarExcepciones: " + e);
			try { con1.close(); } catch (Exception e1) { }
		}
		return (lista.toJSONString());
	}

	/**
	 * El nombre de un producto en el catalogo que le corresponde.
	 *
	 * Para CONTACTCENTER sale del central. Para TIENDA hay que preguntarle a una
	 * tienda, porque el central no tiene ese catalogo: se usa la primera de la
	 * campana que responda. Los ids de producto son los mismos en las once
	 * (verificado el 2026-10-19 con el Combo Insuperable en Envigado, La Mota e
	 * Itagui), asi que cual conteste no cambia la respuesta.
	 */
	private String nombreDe(String ambito, int idProducto, int idExcepcion, ArrayList<Integer> tiendas) {
		if (CampanaPromoDAO.AMBITO_CONTACTCENTER.equals(ambito)) {
			return (this.nombreEnCentral(idProducto, idExcepcion));
		}
		for (Tienda t : TiendaDAO.obtenerTiendas()) {
			if (t.getHosbd() == null || t.getHosbd().trim().isEmpty()) {
				continue;
			}
			if (!tiendas.isEmpty() && !tiendas.contains(Integer.valueOf(t.getIdTienda()))) {
				continue;
			}
			String nombre = this.nombreEnTienda(t.getHosbd(), idProducto);
			if (!nombre.isEmpty()) {
				return (nombre);
			}
		}
		return ("(no se pudo consultar: ninguna tienda respondio)");
	}

	private String nombreEnCentral(int idProducto, int idExcepcion) {
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			return ("");
		}
		String nombre = "";
		try {
			if (idExcepcion > 0) {
				PreparedStatement pst = con1.prepareStatement("select e.descripcion, e.precio, p.nombre"
						+ " from excepcion_precio e left join producto p on p.idproducto = e.idproducto"
						+ " where e.idexcepcion = ? and e.idproducto = ?");
				pst.setInt(1, idExcepcion);
				pst.setInt(2, idProducto);
				ResultSet rs = pst.executeQuery();
				if (rs.next()) {
					nombre = rs.getString("descripcion") + " (sobre " + rs.getString("nombre") + ", $"
							+ Math.round(rs.getDouble("precio")) + ")";
				} else {
					nombre = "(la excepcion " + idExcepcion + " no es del producto " + idProducto + ")";
				}
				rs.close();
				pst.close();
			} else {
				PreparedStatement pst = con1.prepareStatement("select nombre from producto where idproducto = ?");
				pst.setInt(1, idProducto);
				ResultSet rs = pst.executeQuery();
				if (rs.next()) {
					nombre = rs.getString("nombre");
				} else {
					nombre = "(no existe en el central)";
				}
				rs.close();
				pst.close();
			}
			con1.close();
		} catch (Exception e) {
			System.out.println("CampanaPromoCtrl.nombreEnCentral: " + e);
			try { con1.close(); } catch (Exception e1) { }
		}
		return (nombre);
	}

	private String nombreEnTienda(String hostBD, int idProducto) {
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDTiendaRemota(hostBD);
		if (con1 == null) {
			return ("");
		}
		String nombre = "";
		try {
			PreparedStatement pst = con1.prepareStatement(
					"select descripcion, precio1 from producto where idproducto = ?");
			pst.setInt(1, idProducto);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				nombre = rs.getString("descripcion") + " ($" + Math.round(rs.getDouble("precio1")) + ")";
			} else {
				nombre = "(no existe en el catalogo de la tienda)";
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			System.out.println("CampanaPromoCtrl.nombreEnTienda " + hostBD + ": " + e);
			try { con1.close(); } catch (Exception e1) { }
		}
		return (nombre);
	}

	/**
	 * Si a este pedido hay que hacerle la pregunta, y con que texto.
	 *
	 * La pantalla de toma la llama justo antes de finalizar. Devuelve siempre un
	 * JSON con "aplica": si es false, quien toma el pedido no se entera de que
	 * existe una campana, que es lo correcto cuando no aplica.
	 *
	 * Trae tambien el correo que ya tiene el cliente, para que la pregunta no lo
	 * vuelva a pedir cuando ya esta. En mostrador solo entre el 20% y el 35% lo
	 * tienen, asi que la mayoria de las veces vendra vacio y hay que capturarlo.
	 */
	@SuppressWarnings("unchecked")
	public String consultarParaPedido(int idPedido) {
		JSONObject j = new JSONObject();
		CampanaPromoDAO.Campana c = CampanaPromoDAO.aplicaAPedidoCentral(idPedido);
		if (c == null) {
			j.put("aplica", Boolean.FALSE);
			return (j.toJSONString());
		}
		j.put("aplica", Boolean.TRUE);
		j.put("idcampana", c.idCampana);
		j.put("pregunta", c.pregunta);
		j.put("exigecorreo", c.exigeCorreo ? "S" : "N");
		j.put("nombre", c.nombre);

		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 != null) {
			try {
				PreparedStatement pst = con1.prepareStatement("select p.idtienda, p.idcliente,"
						+ " c.email, c.politica_datos, c.nombres, c.apellidos from pedido p"
						+ " left join cliente c on c.idcliente = p.idcliente where p.idpedido = ?");
				pst.setInt(1, idPedido);
				ResultSet rs = pst.executeQuery();
				if (rs.next()) {
					String correo = rs.getString("email") == null ? "" : rs.getString("email").trim();
					boolean politica = "S".equalsIgnoreCase(
							rs.getString("politica_datos") == null ? "" : rs.getString("politica_datos"));
					j.put("idtienda", rs.getInt("idtienda"));
					j.put("idcliente", rs.getInt("idcliente"));
					j.put("correo", correo);
					j.put("politica", politica ? "S" : "N");
					//Decidido aqui y no en la pantalla: la regla de quien puede
					//recibir el premio es una sola y tiene que ser la misma en el
					//contact center y en el mostrador.
					j.put("puede", (this.correoUtilizable(correo) && politica) ? "S" : "N");
					j.put("falta", this.queLeFalta(correo, politica));
					j.put("cliente", ((rs.getString("nombres") == null ? "" : rs.getString("nombres")) + " "
							+ (rs.getString("apellidos") == null ? "" : rs.getString("apellidos"))).trim());
				}
				rs.close();
				pst.close();
				con1.close();
			} catch (Exception e) {
				System.out.println("CampanaPromoCtrl.consultarParaPedido: " + e);
				try { con1.close(); } catch (Exception e1) { }
			}
		}
		return (j.toJSONString());
	}

	public String marcar(int idCampana, int idTienda, int idPedido, int idPedidoTienda, int idCliente,
			String correo, boolean solicito, String usuario, String observacion) {
		if (idCampana <= 0) {
			return ("{\"respuesta\":\"NOK\",\"detalle\":\"Falta la campana\"}");
		}
		if (CampanaPromoDAO.marcar(idCampana, idTienda, idPedido, idPedidoTienda, idCliente, correo, solicito,
				usuario, observacion)) {
			return ("{\"respuesta\":\"OK\"}");
		}
		return ("{\"respuesta\":\"NOK\",\"detalle\":\"No se pudo registrar\"}");
	}

	/**
	 * La misma vara simple de correo que usa el resto del sistema. No valida que
	 * exista, solo que sirva de direccion.
	 */
	private boolean correoUtilizable(String correo) {
		if (correo == null) {
			return (false);
		}
		String limpio = correo.trim();
		int arroba = limpio.indexOf('@');
		return (arroba > 0 && limpio.indexOf('.', arroba) > arroba + 1 && !limpio.endsWith("."));
	}

	/**
	 * Que le falta al cliente para poder recibir el premio, en palabras.
	 *
	 * Va en texto y no en banderas porque quien lo lee es la persona que esta
	 * atendiendo: "no tiene correo registrado" le dice que hacer, "correo=false"
	 * no.
	 */
	private String queLeFalta(String correo, boolean politica) {
		boolean hayCorreo = this.correoUtilizable(correo);
		if (hayCorreo && politica) {
			return ("");
		}
		if (!hayCorreo && !politica) {
			return ("no tiene correo registrado ni ha aceptado la politica de manejo de datos");
		}
		if (!hayCorreo) {
			return ("no tiene correo registrado");
		}
		return ("no ha aceptado la politica de manejo de datos");
	}

	@SuppressWarnings("unchecked")
	public String listarParaEnviar(int idCampana, boolean soloPendientes) {
		JSONArray lista = new JSONArray();
		for (CampanaPromoDAO.ParaEnviar f : CampanaPromoDAO.listarParaEnviar(idCampana, soloPendientes)) {
			JSONObject j = new JSONObject();
			j.put("idregistro", f.idRegistro);
			j.put("tienda", f.nombreTienda);
			j.put("idpedido", f.idPedido);
			j.put("idpedidotienda", f.idPedidoTienda);
			j.put("cliente", f.nombreCliente);
			j.put("correo", f.correo);
			j.put("estado", f.estado);
			j.put("codigoboleta", f.codigoBoleta);
			j.put("fechamarca", f.fechaMarca);
			j.put("fechaenvio", f.fechaEnvio);
			j.put("usuarioenvio", f.usuarioEnvio);
			lista.add(j);
		}
		return (lista.toJSONString());
	}

	public String marcarEnviada(int idRegistro, String codigoBoleta, String usuario) {
		String problema = CampanaPromoDAO.marcarEnviada(idRegistro, codigoBoleta, usuario);
		if (problema.isEmpty()) {
			return ("{\"respuesta\":\"OK\"}");
		}
		return ("{\"respuesta\":\"NOK\",\"detalle\":\"" + problema.replace("\"", "'") + "\"}");
	}

	private ArrayList<Integer> aEnteros(String csv) {
		ArrayList<Integer> valores = new ArrayList<Integer>();
		if (csv == null || csv.trim().isEmpty()) {
			return (valores);
		}
		StringTokenizer st = new StringTokenizer(csv, ",");
		while (st.hasMoreTokens()) {
			try {
				Integer valor = Integer.valueOf(st.nextToken().trim());
				if (!valores.contains(valor)) {
					valores.add(valor);
				}
			} catch (Exception e) {
				// Un token que no es numero se ignora: la pantalla los arma, y si
				// llega basura es mejor guardar lo que si sirve que perder todo.
			}
		}
		return (valores);
	}

	/** "TIENDA:480:0,CONTACTCENTER:6:50" -> lista de productos. */
	private ArrayList<CampanaPromoDAO.Producto> aProductos(String csv) {
		ArrayList<CampanaPromoDAO.Producto> productos = new ArrayList<CampanaPromoDAO.Producto>();
		if (csv == null || csv.trim().isEmpty()) {
			return (productos);
		}
		StringTokenizer st = new StringTokenizer(csv, ",");
		while (st.hasMoreTokens()) {
			String[] partes = st.nextToken().trim().split(":");
			if (partes.length < 2) {
				continue;
			}
			try {
				CampanaPromoDAO.Producto p = new CampanaPromoDAO.Producto();
				p.ambito = CampanaPromoDAO.AMBITO_CONTACTCENTER.equals(partes[0].trim())
						? CampanaPromoDAO.AMBITO_CONTACTCENTER : CampanaPromoDAO.AMBITO_TIENDA;
				p.idProducto = Integer.parseInt(partes[1].trim());
				p.idExcepcion = partes.length > 2 ? Integer.parseInt(partes[2].trim()) : 0;
				p.activo = true;
				// La excepcion solo tiene sentido en el catalogo del central. Si
				// llegara con una del lado tienda, se ignora en vez de guardar un
				// filtro que alla no significa nada.
				if (CampanaPromoDAO.AMBITO_TIENDA.equals(p.ambito)) {
					p.idExcepcion = 0;
				}
				if (p.idProducto > 0) {
					productos.add(p);
				}
			} catch (Exception e) {
				// Igual que arriba: un item mal armado no bota los demas.
			}
		}
		return (productos);
	}

}

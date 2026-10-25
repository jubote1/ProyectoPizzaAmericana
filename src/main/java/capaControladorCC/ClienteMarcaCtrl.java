package capaControladorCC;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.ClienteMarcaDAO;

/**
 * Marcas de cliente: lo que se le muestra a quien toma el pedido, la administracion de las
 * marcas y el bloqueo de Rappi Cargo. Ver ClienteMarcaDAO y sql/2026_10_25_01_cliente_marcas.sql.
 *
 * DOS CONSUMIDORES MUY DISTINTOS
 *
 *  - fichaPublica la usa ObtenerClienteAlerta, que NO exige sesion y la llaman el web de
 *    pedidos y el POS de cada tienda. Por eso devuelve las mismas claves de siempre
 *    (telefono, tipo_alerta, mensaje) y solo SUMA las nuevas: un POS que todavia no esta
 *    actualizado sigue funcionando. El "mensaje" ya trae los habilitadores escritos
 *    ("[POSIBLE FRAUDE] texto...") para que hasta el POS viejo los muestre.
 *  - El resto son operaciones de administracion; quien las llama (CRUDClienteMarca) ya
 *    verifico que el usuario sea administrador.
 *
 * Los textos que ve el usuario llevan tildes como escapes unicode: este proyecto ha tenido
 * archivos con codificaciones mezcladas y un escape no depende de con cual se guarde.
 */
public class ClienteMarcaCtrl {

	public static final String MENSAJE_FALTA_SCRIPT =
			"Falta correr el script sql/2026_10_25_01_cliente_marcas.sql en la base pizzaamericana del central.";

	// -----------------------------------------------------------------------
	// Lo que ve quien toma el pedido
	// -----------------------------------------------------------------------

	/** La alerta de un telefono para el web y el POS. Nunca falla: sin datos devuelve mensaje vacio. */
	@SuppressWarnings("unchecked")
	public String fichaPublica(final String telefono) {
		final ClienteMarcaDAO.Ficha ficha = ClienteMarcaDAO.obtenerFichaPublica(telefono);
		final JSONObject respuesta = new JSONObject();
		boolean bloquea = false;
		final JSONArray marcas = new JSONArray();
		final StringBuilder etiquetas = new StringBuilder();
		for (final ClienteMarcaDAO.Tipo t : ficha.marcas) {
			final JSONObject m = new JSONObject();
			m.put("id", t.idtipo);
			m.put("nombre", t.nombre);
			m.put("descripcion", t.descripcion);
			m.put("severidad", t.severidad);
			m.put("bloquea_cargo", "S".equals(t.bloqueaCargo));
			marcas.add(m);
			bloquea = bloquea || "S".equals(t.bloqueaCargo);
			etiquetas.append('[').append(t.nombre.toUpperCase()).append("] ");
		}
		String tipoAlerta = ficha.tipoAlerta;
		if (tipoAlerta.length() == 0 && !ficha.marcas.isEmpty()) {
			tipoAlerta = "ALERTA_INFORMATIVA";
		}
		//Sin marcas el mensaje es exactamente el de antes. Con marcas se antepone cuales son.
		final String mensaje = (etiquetas.toString() + ficha.mensaje).trim();
		respuesta.put("telefono", ficha.telefono);
		respuesta.put("tipo_alerta", tipoAlerta);
		respuesta.put("mensaje", mensaje);
		respuesta.put("texto", ficha.mensaje);
		respuesta.put("marcas", marcas);
		respuesta.put("tiene_marcas", !ficha.marcas.isEmpty());
		respuesta.put("bloquea_cargo", bloquea);
		return (respuesta.toJSONString());
	}

	// -----------------------------------------------------------------------
	// Administracion
	// -----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static String nok(final String detalle) {
		final JSONObject r = new JSONObject();
		r.put("respuesta", "NOK");
		r.put("detalle", detalle);
		return (r.toJSONString());
	}

	/** Un error de base de datos convertido en algo que se le pueda decir a quien administra. */
	private static String errorTecnico(final Exception e, final String contexto) {
		Logger.getLogger("log_file").error("ClienteMarcaCtrl." + contexto + ": " + e.toString());
		if (e instanceof SQLException) {
			final int codigo = ((SQLException) e).getErrorCode();
			//1146: la tabla no existe. 1054: la columna no existe. O sea: no se corrio el script.
			if (codigo == 1146 || codigo == 1054) {
				return (nok(MENSAJE_FALTA_SCRIPT));
			}
		}
		return (nok("No se pudo completar la operación por un error técnico. Revise el log del servidor."));
	}

	@SuppressWarnings("unchecked")
	private static JSONObject tipoJSON(final ClienteMarcaDAO.Tipo t) {
		final JSONObject o = new JSONObject();
		o.put("id", t.idtipo);
		o.put("nombre", t.nombre);
		o.put("descripcion", t.descripcion);
		o.put("severidad", t.severidad);
		o.put("bloquea_cargo", t.bloqueaCargo);
		o.put("activo", t.activo);
		o.put("orden", t.orden);
		o.put("fecha_marca", t.fechaMarca);
		o.put("usuario", t.usuario);
		return (o);
	}

	/** Todos los habilitadores, activos o no. */
	@SuppressWarnings("unchecked")
	public String tipos() {
		try {
			final JSONArray lista = new JSONArray();
			for (final ClienteMarcaDAO.Tipo t : ClienteMarcaDAO.listarTipos(false)) {
				lista.add(tipoJSON(t));
			}
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("tipos", lista);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "tipos"));
		}
	}

	/** Lo que hay de un telefono, con quienes son los clientes detras de el. */
	@SuppressWarnings("unchecked")
	public String ficha(final String telefono) {
		final String tel = ClienteMarcaDAO.normalizarTelefono(telefono);
		if (tel.length() < 7) {
			return (nok("Escriba un teléfono válido (mínimo 7 dígitos)."));
		}
		try {
			final ClienteMarcaDAO.Ficha f = ClienteMarcaDAO.obtenerFichaAdministracion(tel);
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("telefono", tel);
			r.put("existe", f.existe);
			r.put("texto", f.mensaje);
			r.put("usuario_modifica", f.usuarioModifica);
			r.put("fecha_modifica", f.fechaModifica);
			final JSONArray marcas = new JSONArray();
			for (final ClienteMarcaDAO.Tipo t : f.marcas) {
				marcas.add(tipoJSON(t));
			}
			r.put("marcas", marcas);
			final JSONArray personas = new JSONArray();
			for (final ClienteMarcaDAO.Persona p : ClienteMarcaDAO.personasDeTelefono(tel)) {
				final JSONObject o = new JSONObject();
				o.put("nombre", p.nombre);
				o.put("direccion", p.direccion);
				personas.add(o);
			}
			r.put("personas", personas);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "ficha"));
		}
	}

	/** Deja al telefono con este texto y estos habilitadores (ids separados por coma). */
	@SuppressWarnings("unchecked")
	public String guardar(final String telefono, final String texto, final String idsCsv, final String usuario) {
		final String tel = ClienteMarcaDAO.normalizarTelefono(telefono);
		if (tel.length() < 7 || tel.length() > 15) {
			return (nok("Escriba un teléfono válido (entre 7 y 15 dígitos)."));
		}
		final String t = texto == null ? "" : texto.trim();
		if (t.length() > ClienteMarcaDAO.MAXIMO_MENSAJE) {
			return (nok("El texto es demasiado largo: máximo " + ClienteMarcaDAO.MAXIMO_MENSAJE + " caracteres."));
		}
		final ArrayList<String> ids = new ArrayList<String>();
		if (idsCsv != null) {
			for (final String id : idsCsv.split(",")) {
				if (id.trim().length() > 0) {
					ids.add(id.trim());
				}
			}
		}
		try {
			final String resultado = ClienteMarcaDAO.guardar(tel, t, ids, usuario);
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("resultado", resultado);
			r.put("telefono", tel);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "guardar"));
		}
	}

	@SuppressWarnings("unchecked")
	public String quitar(final String telefono, final String usuario) {
		final String tel = ClienteMarcaDAO.normalizarTelefono(telefono);
		if (tel.length() < 7) {
			return (nok("Escriba un teléfono válido."));
		}
		try {
			final boolean habia = ClienteMarcaDAO.quitar(tel, usuario);
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("habia", habia);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "quitar"));
		}
	}

	/** Los clientes con texto o marcas, con filtro opcional por telefono/texto y por habilitador. */
	@SuppressWarnings("unchecked")
	public String listar(final String filtro, final String idtipo) {
		try {
			final JSONArray lista = new JSONArray();
			for (final ClienteMarcaDAO.Fila f : ClienteMarcaDAO.listar(filtro, idtipo, 300)) {
				final JSONObject o = new JSONObject();
				o.put("telefono", f.telefono);
				o.put("tipo_alerta", f.tipoAlerta);
				o.put("texto", f.mensaje);
				o.put("marcas", f.idsMarcas);
				o.put("usuario", f.usuario);
				o.put("fecha", f.fecha);
				lista.add(o);
			}
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("clientes", lista);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "listar"));
		}
	}

	@SuppressWarnings("unchecked")
	public String historial(final String telefono) {
		try {
			final JSONArray lista = new JSONArray();
			for (final ClienteMarcaDAO.Evento ev : ClienteMarcaDAO.historial(telefono, 60)) {
				final JSONObject o = new JSONObject();
				o.put("fecha", ev.fecha);
				o.put("usuario", ev.usuario);
				o.put("accion", ev.accion);
				o.put("detalle", ev.detalle);
				lista.add(o);
			}
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("eventos", lista);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "historial"));
		}
	}

	/** Crea o edita un habilitador. idtipo vacio = nuevo. */
	@SuppressWarnings("unchecked")
	public String guardarTipo(final String idtipo, final String nombre, final String descripcion, final String severidad,
			final String bloqueaCargo, final String activo, final String orden, final String usuario) {
		final String nom = nombre == null ? "" : nombre.trim();
		if (nom.length() < 3 || nom.length() > 80) {
			return (nok("El nombre del habilitador debe tener entre 3 y 80 caracteres."));
		}
		if (descripcion != null && descripcion.trim().length() > 300) {
			return (nok("La descripción es demasiado larga: máximo 300 caracteres."));
		}
		int ord = 10;
		try {
			ord = Integer.parseInt(orden.trim());
		} catch (final Exception e) {
			ord = 10;
		}
		try {
			final String id = ClienteMarcaDAO.guardarTipo(idtipo, nom, descripcion, severidad, bloqueaCargo, activo, ord, usuario);
			final JSONObject r = new JSONObject();
			r.put("respuesta", "OK");
			r.put("idtipo", id);
			return (r.toJSONString());
		} catch (final Exception e) {
			return (errorTecnico(e, "guardarTipo"));
		}
	}

	// -----------------------------------------------------------------------
	// Rappi Cargo
	// -----------------------------------------------------------------------

	/**
	 * Si el cliente esta marcado de manera que no se le puede enviar el pedido por Rappi Cargo,
	 * devuelve la frase que se le dice a quien intenta enviarlo. Cadena vacia = se puede.
	 *
	 * Se miran los telefonos que el cliente tenga guardados (fijo y celular) mas el que se
	 * pase, porque la marca va por telefono y el pedido puede traer cualquiera de los dos.
	 * Si no se puede consultar, devuelve vacio: ver ClienteMarcaDAO.bloqueosRappiCargo.
	 */
	public static String motivoBloqueoCargo(final int idcliente, final String telefonoExtra) {
		final ArrayList<String> tels = ClienteMarcaDAO.telefonosDeCliente(idcliente);
		if (telefonoExtra != null) {
			tels.add(telefonoExtra);
		}
		final ClienteMarcaDAO.Bloqueo b = ClienteMarcaDAO.bloqueosRappiCargo(tels);
		if (b.nombres.isEmpty()) {
			return ("");
		}
		final StringBuilder sb = new StringBuilder();
		sb.append("Este cliente está marcado como \"");
		for (int i = 0; i < b.nombres.size(); i++) {
			sb.append(i == 0 ? "" : "\" y \"").append(b.nombres.get(i));
		}
		sb.append("\": su pedido no se puede enviar por Rappi Cargo.");
		if (b.texto.length() > 0) {
			sb.append(" Nota de quien lo marcó: ").append(b.texto);
		}
		return (sb.toString());
	}
}

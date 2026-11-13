package capaControladorCC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.BancoPreguntasEmpleadoDAO;
import capaDAOCC.UsuarioRolDAO;

/**
 * Banco de preguntas a empleados: quien puede, que se valida y como se le responde a la pantalla.
 *
 * El contenido se valida AQUI (no solo en la pantalla): una pregunta sin respuesta correcta, o con una sola opcion, deja
 * al empleado frente a un cuadro que no puede contestar bien, y como el cuadro del huellero no se puede cerrar sin
 * contestar, un error de configuracion se vuelve un empleado trabado en la puerta.
 */
public class BancoPreguntasEmpleadoCtrl {

	/** Rol que da acceso a la pantalla. Se asigna desde "Asignar rol a usuario". */
	public static final String ROL = "Banco de Preguntas a Empleados";

	private static final int MIN_PREGUNTA = 10;
	private static final int MAX_PREGUNTA = 500; // pregunta_empleado.descripcion VARCHAR(500)
	private static final int MAX_OPCION = 100; // opcion_respuesta.contenido VARCHAR(100)
	private static final int MIN_OPCIONES = 2;
	private static final int MAX_OPCIONES = 6; // el cuadro del huellero muestra las opciones en tres columnas

	/** Este usuario puede usar la pantalla? Se lee de la base en cada peticion. */
	public static boolean puede(final int idUsuario) {
		if (idUsuario <= 0) {
			return (false);
		}
		final ArrayList<String> roles = UsuarioRolDAO.listarNombresRolPorUsuario(idUsuario);
		if (roles == null) {
			return (false);
		}
		for (int i = 0; i < roles.size(); i++) {
			if (ROL.equalsIgnoreCase(roles.get(i))) {
				return (true);
			}
		}
		return (false);
	}

	@SuppressWarnings("unchecked")
	public String listar(final String estado) {
		final JSONObject r = new JSONObject();
		try {
			final JSONArray cargos = new JSONArray();
			for (final BancoPreguntasEmpleadoDAO.Cargo c : BancoPreguntasEmpleadoDAO.listarCargos()) {
				final JSONObject o = new JSONObject();
				o.put("id", Integer.valueOf(c.id));
				o.put("nombre", c.nombre);
				cargos.add(o);
			}
			final JSONArray preguntas = new JSONArray();
			for (final BancoPreguntasEmpleadoDAO.Pregunta p : BancoPreguntasEmpleadoDAO.listar(estado)) {
				final JSONObject o = new JSONObject();
				o.put("id", Integer.valueOf(p.id));
				o.put("descripcion", p.descripcion);
				o.put("fechainicio", p.fechaInicio);
				o.put("fechafinal", p.fechaFinal);
				o.put("activa", Boolean.valueOf(p.activa));
				o.put("todos", Boolean.valueOf(p.todos));
				o.put("respuestas", Integer.valueOf(p.respuestas));
				o.put("empleados", Integer.valueOf(p.empleados));
				o.put("aciertos", Integer.valueOf(p.aciertos));
				final JSONArray cs = new JSONArray();
				for (final Integer c : p.cargos) {
					cs.add(c);
				}
				o.put("cargos", cs);
				final JSONArray ops = new JSONArray();
				for (final BancoPreguntasEmpleadoDAO.Opcion op : p.opciones) {
					final JSONObject oo = new JSONObject();
					oo.put("texto", op.texto);
					oo.put("correcta", Boolean.valueOf(op.correcta));
					ops.add(oo);
				}
				o.put("opciones", ops);
				preguntas.add(o);
			}
			r.put("respuesta", "OK");
			r.put("cargos", cargos);
			r.put("preguntas", preguntas);
		} catch (final Exception e) {
			r.put("respuesta", "NOK");
			r.put("detalle", "No se pudo leer el banco de preguntas: " + limpio(e.getMessage()));
		}
		return (r.toJSONString());
	}

	/**
	 * @param datos JSON: {id, descripcion, fechainicio, fechafinal, activa, todos, cargos:[ids], opciones:[{texto, correcta}]}
	 */
	@SuppressWarnings("unchecked")
	public String guardar(final String datos) {
		final JSONObject r = new JSONObject();
		try {
			final JSONObject d = (JSONObject) new JSONParser().parse(datos == null ? "{}" : datos);
			final int id = d.get("id") == null ? 0 : Integer.parseInt(String.valueOf(d.get("id")).trim());
			final String descripcion = d.get("descripcion") == null ? "" : String.valueOf(d.get("descripcion")).trim();
			final String fechaInicio = d.get("fechainicio") == null ? "" : String.valueOf(d.get("fechainicio")).trim();
			final String fechaFinal = d.get("fechafinal") == null ? "" : String.valueOf(d.get("fechafinal")).trim();
			final boolean activa = Boolean.parseBoolean(String.valueOf(d.get("activa")));
			final boolean todos = Boolean.parseBoolean(String.valueOf(d.get("todos")));

			if (descripcion.length() < MIN_PREGUNTA) {
				return (malo(r, "Escriba la pregunta completa (minimo " + MIN_PREGUNTA + " caracteres)."));
			}
			if (descripcion.length() > MAX_PREGUNTA) {
				return (malo(r, "La pregunta es muy larga: " + descripcion.length() + " caracteres, el maximo es " + MAX_PREGUNTA + "."));
			}
			final java.time.LocalDate fi;
			final java.time.LocalDate ff;
			try {
				fi = java.time.LocalDate.parse(fechaInicio);
				ff = java.time.LocalDate.parse(fechaFinal);
			} catch (final Exception e) {
				return (malo(r, "Las fechas de vigencia no son validas."));
			}
			if (ff.isBefore(fi)) {
				return (malo(r, "La fecha final no puede ser anterior a la inicial."));
			}
			if (fi.getYear() < 2020 || ff.getYear() > 2100) {
				return (malo(r, "Revise el ano de las fechas de vigencia."));
			}

			final List<Integer> cargos = new ArrayList<Integer>();
			final Object crudosCargos = d.get("cargos");
			if (crudosCargos instanceof JSONArray) {
				for (final Object c : (JSONArray) crudosCargos) {
					final int n = Integer.parseInt(String.valueOf(c).trim());
					if (n > 0 && !cargos.contains(Integer.valueOf(n))) {
						cargos.add(Integer.valueOf(n));
					}
				}
			}
			if (!todos && cargos.isEmpty()) {
				return (malo(r, "Escoja a que cargos les aplica la pregunta, o marque \"Todos los cargos\"."));
			}

			final List<BancoPreguntasEmpleadoDAO.Opcion> opciones = new ArrayList<BancoPreguntasEmpleadoDAO.Opcion>();
			final Set<String> vistas = new HashSet<String>();
			int correctas = 0;
			final Object crudasOpc = d.get("opciones");
			if (crudasOpc instanceof JSONArray) {
				for (final Object o : (JSONArray) crudasOpc) {
					final JSONObject oo = (JSONObject) o;
					final String texto = oo.get("texto") == null ? "" : String.valueOf(oo.get("texto")).trim();
					if (texto.length() == 0) {
						continue; // una fila vacia se ignora
					}
					if (texto.length() > MAX_OPCION) {
						return (malo(r, "La opcion \"" + texto.substring(0, 20) + "...\" es muy larga (maximo " + MAX_OPCION + " caracteres)."));
					}
					if (!vistas.add(texto.toLowerCase())) {
						return (malo(r, "La opcion \"" + texto + "\" esta repetida."));
					}
					final BancoPreguntasEmpleadoDAO.Opcion op = new BancoPreguntasEmpleadoDAO.Opcion();
					op.texto = texto;
					op.correcta = Boolean.parseBoolean(String.valueOf(oo.get("correcta")));
					if (op.correcta) {
						correctas++;
					}
					opciones.add(op);
				}
			}
			if (opciones.size() < MIN_OPCIONES) {
				return (malo(r, "Ponga al menos " + MIN_OPCIONES + " opciones de respuesta."));
			}
			if (opciones.size() > MAX_OPCIONES) {
				return (malo(r, "Maximo " + MAX_OPCIONES + " opciones por pregunta."));
			}
			if (correctas == 0) {
				return (malo(r, "Marque cual es la respuesta correcta."));
			}
			if (correctas == opciones.size()) {
				return (malo(r, "Todas las opciones estan marcadas como correctas: debe haber al menos una incorrecta."));
			}

			final int idGuardado = BancoPreguntasEmpleadoDAO.guardar(id, descripcion, fechaInicio, fechaFinal, activa, todos,
					cargos, opciones);
			r.put("respuesta", "OK");
			r.put("id", Integer.valueOf(idGuardado));
			r.put("detalle", "Pregunta guardada.");
		} catch (final Exception e) {
			System.out.println("BancoPreguntasEmpleadoCtrl.guardar: " + e);
			return (malo(r, "No se pudo guardar: " + limpio(e.getMessage())));
		}
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String cambiarEstado(final int idPregunta, final boolean activa) {
		final JSONObject r = new JSONObject();
		try {
			if (idPregunta <= 0 || !BancoPreguntasEmpleadoDAO.cambiarEstado(idPregunta, activa)) {
				return (malo(r, "La pregunta no existe."));
			}
			r.put("respuesta", "OK");
			r.put("detalle", activa ? "Pregunta activada." : "Pregunta inactivada.");
		} catch (final Exception e) {
			return (malo(r, "No se pudo cambiar el estado: " + limpio(e.getMessage())));
		}
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private String malo(final JSONObject r, final String detalle) {
		r.put("respuesta", "NOK");
		r.put("detalle", detalle);
		return (r.toJSONString());
	}

	private String limpio(final String texto) {
		if (texto == null) {
			return ("error desconocido");
		}
		final String t = texto.replaceAll("[\\r\\n]+", " ").trim();
		return (t.length() > 200 ? t.substring(0, 200) : t);
	}
}

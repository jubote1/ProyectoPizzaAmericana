package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.BancoPreguntasEmpleadoCtrl;
import capaModeloCC.Usuario;

/**
 * Administracion del banco de preguntas que el huellero le hace a los empleados.
 *
 * Ruta: /ProyectoPizzaAmericana/BancoPreguntasEmpleado   Metodos: GET (listar), POST (guardar, estado).
 * Tipo: Modifica datos.  Acceso: RESTRINGIDO al rol "Banco de Preguntas a Empleados".
 * Recibe GET: estado (todas | vigentes | inactivas | vencidas).
 * Recibe POST: accion = guardar (datos = JSON de la pregunta) | estado (id, activa).
 * Devuelve: application/json; charset=UTF-8; claves: respuesta (OK | NOK | NOSESION | SINPERMISO), detalle, cargos, preguntas, id.
 * Lee/Escribe: general.pregunta_empleado, pregunta_empleado_tipo, opcion_respuesta, opcion_respuesta_pregunta (lee respuesta_empleado).
 *
 * El rol se lee de la base en cada peticion y no se confia del filtro de seguridad (esta en modo LOG).
 */
@WebServlet("/BancoPreguntasEmpleado")
public class BancoPreguntasEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		if (autorizado(request, out) == null) {
			return;
		}
		String estado = request.getParameter("estado");
		if (estado == null || !estado.matches("todas|vigentes|inactivas|vencidas")) {
			estado = "todas";
		}
		out.write(new BancoPreguntasEmpleadoCtrl().listar(estado));
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		if (autorizado(request, out) == null) {
			return;
		}
		final String accion = request.getParameter("accion") == null ? "" : request.getParameter("accion").trim();
		try {
			if ("guardar".equals(accion)) {
				out.write(new BancoPreguntasEmpleadoCtrl().guardar(request.getParameter("datos")));
			} else if ("estado".equals(accion)) {
				int id = 0;
				try {
					id = Integer.parseInt(request.getParameter("id").trim());
				} catch (final Exception e) {
					id = 0;
				}
				out.write(new BancoPreguntasEmpleadoCtrl().cambiarEstado(id, "true".equals(request.getParameter("activa"))));
			} else {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Accion no reconocida\"}");
			}
		} catch (final Exception e) {
			System.out.println("BancoPreguntasEmpleado POST: " + e);
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico procesando la solicitud\"}");
		}
	}

	/** Devuelve el usuario de la sesion si tiene el rol, o null y ya escribio la negativa. */
	private Usuario autorizado(final HttpServletRequest request, final PrintWriter out) {
		final HttpSession sesion = request.getSession(false);
		final Object guardado = (sesion == null) ? null : sesion.getAttribute("usuario");
		if (!(guardado instanceof Usuario)) {
			out.write("{\"respuesta\":\"NOSESION\",\"detalle\":\"Debe iniciar sesion\"}");
			return (null);
		}
		final Usuario usuario = (Usuario) guardado;
		if (!BancoPreguntasEmpleadoCtrl.puede(usuario.getId())) {
			out.write("{\"respuesta\":\"SINPERMISO\",\"detalle\":\"No tiene permiso para esta pantalla\"}");
			return (null);
		}
		return (usuario);
	}
}

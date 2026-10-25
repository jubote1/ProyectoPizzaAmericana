package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ClienteMarcaCtrl;
import capaDAOCC.RedencionManualDAO;
import capaModeloCC.Usuario;

/**
 * Servicio de administracion de las marcas de cliente (el texto de alerta y los habilitadores
 * como "No desea Rappi Cargo" o "Posible fraude"): invoca ClienteMarcaCtrl.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDClienteMarca   Metodos: GET (consultas) y POST (cambios).
 * Tipo: Consulta y modifica datos.  Acceso: exige sesion y que el usuario sea ADMINISTRADOR
 * (usuario.administrador = 'S', preguntado a la base en cada llamado: a quien le quiten el
 * permiso lo pierde en el siguiente clic, no cuando se le venza la sesion).
 *
 * Recibe: idoperacion (integer):
 *   1 ficha de un telefono (telefono)                         GET o POST
 *   2 guardar texto y habilitadores (telefono, texto, tipos)  SOLO POST
 *   3 quitar todo de un telefono (telefono)                   SOLO POST
 *   4 listar clientes marcados (filtro, idtipo)               GET o POST
 *   5 listar los habilitadores                                GET o POST
 *   6 crear o editar un habilitador (idtipo, nombre, descripcion, severidad, bloquea_cargo,
 *     activo, orden)                                          SOLO POST
 *   7 historial de un telefono (telefono)                     GET o POST
 * Devuelve: application/json; charset=UTF-8; claves: respuesta (OK, NOK, NOSESION o SINPERMISO),
 * detalle cuando no es OK, y segun la operacion: tipos, clientes, marcas, personas, texto, eventos.
 * Lee: cliente, cliente_alerta, cliente_marca, cliente_marca_tipo, cliente_marca_log, usuario.
 * Escribe: cliente_alerta, cliente_marca, cliente_marca_tipo, cliente_marca_log.
 *
 * Los cambios exigen POST para que un enlace o una imagen en otra pagina no los pueda disparar.
 * Quien SOLO consulta la alerta de un cliente al tomar un pedido usa ObtenerClienteAlerta.
 */
@WebServlet("/CRUDClienteMarca")
public class CRUDClienteMarca extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public CRUDClienteMarca() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, false);
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, true);
	}

	private void atender(final HttpServletRequest request, final HttpServletResponse response, final boolean esPost)
			throws IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store");
		final PrintWriter out = response.getWriter();

		final Usuario usuario = autorizado(request, out);
		if (usuario == null) {
			return;
		}
		final int operacion = entero(request, "idoperacion");
		final boolean modifica = operacion == 2 || operacion == 3 || operacion == 6;
		if (modifica && !esPost) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Esta operaci\\u00f3n solo se acepta por POST.\"}");
			return;
		}
		final ClienteMarcaCtrl ctrl = new ClienteMarcaCtrl();
		final String quien = usuario.getNombreUsuario();
		try {
			switch (operacion) {
			case 1:
				out.write(ctrl.ficha(texto(request, "telefono")));
				break;
			case 2:
				out.write(ctrl.guardar(texto(request, "telefono"), texto(request, "texto"), texto(request, "tipos"), quien));
				break;
			case 3:
				out.write(ctrl.quitar(texto(request, "telefono"), quien));
				break;
			case 4:
				out.write(ctrl.listar(texto(request, "filtro"), texto(request, "idtipo")));
				break;
			case 5:
				out.write(ctrl.tipos());
				break;
			case 6:
				out.write(ctrl.guardarTipo(texto(request, "idtipo"), texto(request, "nombre"), texto(request, "descripcion"),
						texto(request, "severidad"), texto(request, "bloquea_cargo"), texto(request, "activo"),
						texto(request, "orden"), quien));
				break;
			case 7:
				out.write(ctrl.historial(texto(request, "telefono")));
				break;
			default:
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Operaci\\u00f3n no reconocida.\"}");
				break;
			}
		} catch (final Exception e) {
			System.out.println("CRUDClienteMarca op " + operacion + ": " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error t\\u00e9cnico procesando la solicitud.\"}");
		}
	}

	/** El usuario de la sesion si es administrador; si no, null y ya se escribio la negativa. */
	private Usuario autorizado(final HttpServletRequest request, final PrintWriter out) {
		final HttpSession sesion = request.getSession(false);
		final Object guardado = (sesion == null) ? null : sesion.getAttribute("usuario");
		if (!(guardado instanceof Usuario)) {
			out.write("{\"respuesta\":\"NOSESION\",\"detalle\":\"Debe iniciar sesi\\u00f3n.\"}");
			return (null);
		}
		final Usuario usuario = (Usuario) guardado;
		if (!RedencionManualDAO.esAdministrador(usuario.getId())) {
			out.write("{\"respuesta\":\"SINPERMISO\",\"detalle\":\"Solo un administrador puede marcar clientes.\"}");
			return (null);
		}
		return (usuario);
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String crudo = request.getParameter(nombre);
		return (crudo == null ? "" : crudo.trim());
	}

	private int entero(final HttpServletRequest request, final String nombre) {
		try {
			return (Integer.parseInt(request.getParameter(nombre).trim()));
		} catch (final Exception e) {
			return (0);
		}
	}
}

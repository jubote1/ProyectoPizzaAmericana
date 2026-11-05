package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import capaControladorCC.ConsultaPuntosClienteCtrl;
import capaModeloCC.Usuario;

/**
 * Consulta de puntos de un cliente para quien lo atiende (contact center y lideres).
 * Invoca ConsultaPuntosClienteCtrl.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarPuntosCliente   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: RESTRINGIDO a quien tenga, por alguno de sus roles, la pantalla
 *       ConsultaPuntosCliente.html en la matriz de roles (rol_pantalla).
 * Recibe (query/form): accion (BUSCAR o FICHA), q (correo o celular, para BUSCAR),
 *                      correo (para FICHA).
 * Devuelve: application/json; BUSCAR -> resultado, candidatos[]; FICHA -> resultado, ficha,
 *           totales, vencimientos, movimientos, comportamiento, portienda, pormes, bonos;
 *           o resultado = ERROR con mensaje.
 * Lee: cliente_fidelizacion, fidelizacion_transaccion, fidelizacion_redencion, cliente, tienda,
 *      bono_emitido, bono_campana, crm.persona_resumen.
 *
 * POR QUE EL PERMISO SE REVISA AQUI Y NO EN EL FILTRO
 *
 * general.parametros.SEGURIDADFILTROMODO esta en LOG: SeguridadFilter audita pero deja
 * pasar todo, y esconder la opcion del menu no protege nada. Esta pantalla muestra
 * datos personales de clientes (nombre, celular, correo, compras), asi que el servlet
 * exige sesion y revisa en la base, en cada peticion, que el usuario tenga la pantalla.
 * Cada consulta queda en el log con quien la hizo.
 */
@WebServlet("/ConsultarPuntosCliente")
public class ConsultarPuntosCliente extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarPuntosCliente() {
		super();
	}

	@SuppressWarnings("unchecked")
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion == null || !(sesion.getAttribute("usuario") instanceof Usuario)) {
				out.write(error("Sesion no valida. Vuelva a ingresar."));
				return;
			}
			final Usuario usuario = (Usuario) sesion.getAttribute("usuario");
			if (!ConsultaPuntosClienteCtrl.puedeVer(usuario.getId())) {
				out.write(error("No tiene permiso para ver esta informacion."));
				return;
			}

			final String accion = texto(request.getParameter("accion")).toUpperCase();
			if ("BUSCAR".equals(accion)) {
				final String q = texto(request.getParameter("q")).trim();
				if (q.length() < 4 || q.length() > 80) {
					out.write(error("Escriba el correo o el celular del cliente."));
					return;
				}
				out.write(ConsultaPuntosClienteCtrl.buscar(q).toJSONString());
				return;
			}
			if ("FICHA".equals(accion)) {
				final String correo = texto(request.getParameter("correo")).trim().toLowerCase();
				if (correo.length() < 5 || correo.length() > 50 || correo.indexOf('@') < 0) {
					out.write(error("El correo no es valido."));
					return;
				}
				Logger.getLogger("log_file").info("ConsultarPuntosCliente: " + usuario.getNombreUsuario() + " consulto " + correo);
				out.write(ConsultaPuntosClienteCtrl.ficha(correo).toJSONString());
				return;
			}
			out.write(error("Accion desconocida."));
		} catch (final Exception e) {
			out.write(error("No se pudo consultar: " + e.toString()));
		} finally {
			out.flush();
			out.close();
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@SuppressWarnings("unchecked")
	private static String error(final String mensaje) {
		final JSONObject o = new JSONObject();
		o.put("resultado", "ERROR");
		o.put("mensaje", mensaje);
		return (o.toJSONString());
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}
}

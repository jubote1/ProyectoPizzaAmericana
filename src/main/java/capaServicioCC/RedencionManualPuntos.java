package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.RedencionManualCtrl;
import capaDAOCC.RedencionManualDAO;
import capaModeloCC.Usuario;

/**
 * Servicio que modifica datos de Ofertas y fidelizacion (administracion): invoca
 * RedencionManualCtrl.redimir, RedencionManualDAO.esAdministrador.
 *
 * Ruta: /ProyectoPizzaAmericana/RedencionManualPuntos   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Exige token de tienda.
 * Devuelve: application/json; charset=UTF-8; claves: activo, correo, correoenviado, detalle,
 * idredencion, idtienda, mail.smtp.host, mail.smtp.ssl.protocols, nombre, puntos, respuesta,
 * saldo, ....
 * Lee: cliente, cliente_fidelizacion, fidelizacion_transaccion, parametros, tienda, usuario.
 * Escribe: cliente_fidelizacion (UPDATE), codigo_redencion_puntos (UPDATE), fidelizacion_redencion
 * (INSERT/UPDATE), fidelizacion_redencion_detalle (INSERT), fidelizacion_transaccion (UPDATE).
 * Sistemas externos: Correo SMTP, Gmail, Wompi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RedencionManualPuntos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RedencionManualPuntos")
public class RedencionManualPuntos extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RedencionManualPuntos() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final Usuario usuario = autorizado(request, out);
		if (usuario == null) {
			return;
		}
		try {
			final RedencionManualCtrl ctrl = new RedencionManualCtrl();
			if ("tiendas".equals(texto(request, "que"))) {
				out.write(ctrl.tiendas());
				return;
			}
			out.write(ctrl.consultar(texto(request, "correo")));
		} catch (final Exception e) {
			System.out.println("RedencionManualPuntos GET: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error consultando el cliente\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final Usuario usuario = autorizado(request, out);
		if (usuario == null) {
			return;
		}
		try {
			//Un valor que no se entiende llega como -1 y el controlador lo
			//rechaza. NO se convierte en cero: un cero aqui seria una redencion
			//de nada que igual le manda un correo al cliente.
			final double puntos = numero(request, "puntos");
			final int idTienda = entero(request, "idtienda", 0);

			out.write(new RedencionManualCtrl().redimir(
					texto(request, "correo"),
					puntos,
					idTienda,
					texto(request, "tienda"),
					texto(request, "motivo"),
					usuario.getNombreUsuario()));
		} catch (final Exception e) {
			System.out.println("RedencionManualPuntos POST: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico procesando la redencion\"}");
		}
	}

	/**
	 * Devuelve el usuario de la sesion si puede hacer esto, o null y ya escribio
	 * la negativa.
	 */
	private Usuario autorizado(final HttpServletRequest request, final PrintWriter out) {
		final HttpSession sesion = request.getSession(false);
		final Object guardado = (sesion == null) ? null : sesion.getAttribute("usuario");
		if (!(guardado instanceof Usuario)) {
			out.write("{\"respuesta\":\"NOSESION\",\"detalle\":\"Debe iniciar sesion\"}");
			return (null);
		}
		final Usuario usuario = (Usuario) guardado;
		if (!RedencionManualDAO.esAdministrador(usuario.getId())) {
			out.write("{\"respuesta\":\"SINPERMISO\",\"detalle\":"
					+ "\"Solo un administrador puede hacer redenciones manuales\"}");
			return (null);
		}
		return (usuario);
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String crudo = request.getParameter(nombre);
		return (crudo == null ? "" : crudo.trim());
	}

	private int entero(final HttpServletRequest request, final String nombre, final int siNo) {
		try {
			return (Integer.parseInt(request.getParameter(nombre).trim()));
		} catch (final Exception e) {
			return (siNo);
		}
	}

	/**
	 * Los puntos digitados, aceptando coma o punto y separadores de miles.
	 * Devuelve -1 si no se entiende.
	 */
	private double numero(final HttpServletRequest request, final String nombre) {
		final String crudo = request.getParameter(nombre);
		if (crudo == null || crudo.trim().length() == 0) {
			return (-1);
		}
		String limpio = crudo.trim().replace(" ", "");
		final int ultimaComa = limpio.lastIndexOf(',');
		final int ultimoPunto = limpio.lastIndexOf('.');
		//El separador decimal es el ultimo que aparezca; el otro es de miles.
		if (ultimaComa >= 0 && ultimaComa > ultimoPunto) {
			limpio = limpio.replace(".", "").replace(',', '.');
		} else {
			limpio = limpio.replace(",", "");
		}
		try {
			final double valor = Double.parseDouble(limpio);
			return (valor < 0 ? -1 : valor);
		} catch (final Exception e) {
			return (-1);
		}
	}
}

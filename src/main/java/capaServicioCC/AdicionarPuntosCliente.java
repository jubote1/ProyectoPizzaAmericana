package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.AdicionPuntosManualCtrl;
import capaModeloCC.Usuario;

/**
 * Adicion manual de puntos a un cliente del plan de fidelizacion.
 *
 * Ruta: /ProyectoPizzaAmericana/AdicionarPuntosCliente   Metodos: GET (consultar), POST (adicionar).
 * Tipo: Modifica datos.  Acceso: RESTRINGIDO al rol "Ajuste Manual de Puntos" (un solo miembro).
 * Devuelve: application/json; charset=UTF-8; claves: respuesta, detalle, correo, nombre, activo, puntos,
 * diasvigencia, tope, idajuste, saldoantes, saldo, vencen, correoenviado.
 * Escribe: fidelizacion_transaccion (INSERT), cliente_fidelizacion (UPDATE), fidelizacion_ajuste_manual
 * (INSERT/UPDATE). Sistemas externos: correo SMTP.
 *
 * El rol se lee de la base en cada peticion, no de la sesion, y no se confia del filtro de seguridad (esta
 * en modo LOG): quitarle el rol a alguien tiene efecto de inmediato.
 */
@WebServlet("/AdicionarPuntosCliente")
public class AdicionarPuntosCliente extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public AdicionarPuntosCliente() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		if (autorizado(request, out) == null) {
			return;
		}
		try {
			out.write(new AdicionPuntosManualCtrl().consultar(texto(request, "correo")));
		} catch (final Exception e) {
			System.out.println("AdicionarPuntosCliente GET: " + e.toString());
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
			//Un valor que no se entiende llega como -1 y el controlador lo rechaza.
			out.write(new AdicionPuntosManualCtrl().adicionar(texto(request, "correo"), numero(request, "puntos"),
					texto(request, "motivo"), usuario.getNombreUsuario()));
		} catch (final Exception e) {
			System.out.println("AdicionarPuntosCliente POST: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico procesando el ajuste\"}");
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
		if (!AdicionPuntosManualCtrl.puede(usuario.getId())) {
			out.write("{\"respuesta\":\"SINPERMISO\",\"detalle\":\"No tiene permiso para esta operacion\"}");
			return (null);
		}
		return (usuario);
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String crudo = request.getParameter(nombre);
		return (crudo == null ? "" : crudo.trim());
	}

	/** Los puntos digitados, aceptando coma o punto y separadores de miles. -1 si no se entiende. */
	private double numero(final HttpServletRequest request, final String nombre) {
		final String crudo = request.getParameter(nombre);
		if (crudo == null || crudo.trim().length() == 0) {
			return (-1);
		}
		String limpio = crudo.trim().replace(" ", "");
		final int ultimaComa = limpio.lastIndexOf(',');
		final int ultimoPunto = limpio.lastIndexOf('.');
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

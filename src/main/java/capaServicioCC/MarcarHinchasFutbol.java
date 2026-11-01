package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import capaDAOCC.HinchaFutbolDAO;
import capaModeloCC.Usuario;

/**
 * Marca a los clientes como hinchas de Nacional o del Medellin, segun en que dias
 * compraron el Combo Futbolero.
 *
 * Se dispara a mano despues de actualizar el calendario (tabla partido_futbol), no en
 * automatico: el calendario lo llena una persona y conviene que sea la misma que decide
 * cuando aplicarlo.
 *
 * Trae dos modos. Con accion = PREVISUALIZAR solo cuenta y NO escribe nada: sirve para
 * ver a cuanta gente afectaria antes de hacerlo. Con accion = MARCAR si marca.
 *
 * Ruta: /ProyectoPizzaAmericana/MarcarHinchasFutbol   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Proceso.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): desde (aaaa-mm-dd), hasta (aaaa-mm-dd), accion (PREVISUALIZAR o MARCAR).
 * Devuelve: application/json; charset=UTF-8; claves: resultado, mensaje, dias_nacional,
 * dias_medellin, dias_clasico, candidatos, ambos_equipos, marcados_nacional,
 * marcados_medellin, ya_estaban.
 * Lee: partido_futbol, datamart.pedido, datamart.detalle_pedido, pedido, cliente.
 * Escribe: cliente_marca (solo con accion = MARCAR).
 */
@WebServlet("/MarcarHinchasFutbol")
public class MarcarHinchasFutbol extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final String usuario = usuarioSesion(request);
		if (usuario.length() == 0) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Debe iniciar sesion\"}");
			return;
		}

		final String desde = fecha(request.getParameter("desde"));
		final String hasta = fecha(request.getParameter("hasta"));
		if (desde.length() == 0 || hasta.length() == 0 || desde.compareTo(hasta) > 0) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"El rango de fechas no es valido.\"}");
			return;
		}
		final boolean soloVer = !"MARCAR".equalsIgnoreCase(texto(request.getParameter("accion")));

		final HinchaFutbolDAO.Resumen resumen = new HinchaFutbolDAO.Resumen();
		final ArrayList<HinchaFutbolDAO.Candidato> candidatos =
				HinchaFutbolDAO.candidatos(desde, hasta, resumen);
		if (!soloVer && resumen.error.length() == 0) {
			HinchaFutbolDAO.marcar(candidatos, usuario, resumen);
		}

		final JSONObject r = new JSONObject();
		r.put("resultado", resumen.error.length() == 0 ? "OK" : "ERROR");
		r.put("mensaje", resumen.error.length() > 0 ? resumen.error
				: (soloVer ? "Previsualizacion: no se marco a nadie." : "Marcado terminado."));
		r.put("dias_nacional", Integer.valueOf(resumen.diasNacional));
		r.put("dias_medellin", Integer.valueOf(resumen.diasMedellin));
		r.put("dias_clasico", Integer.valueOf(resumen.diasClasico));
		r.put("candidatos", Integer.valueOf(resumen.candidatos));
		r.put("ambos_equipos", Integer.valueOf(resumen.ambosEquipos));
		r.put("marcados_nacional", Integer.valueOf(resumen.marcadosNacional));
		r.put("marcados_medellin", Integer.valueOf(resumen.marcadosMedellin));
		r.put("ya_estaban", Integer.valueOf(resumen.yaEstaban));
		out.write(r.toJSONString());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/** Acepta aaaa-mm-dd y dd/mm/aaaa; vacio si no es ninguna. */
	private static String fecha(final String valor) {
		final String f = texto(valor).trim();
		if (f.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return (f);
		}
		if (f.matches("\\d{2}/\\d{2}/\\d{4}")) {
			return (f.substring(6, 10) + "-" + f.substring(3, 5) + "-" + f.substring(0, 2));
		}
		return ("");
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}

	private static String usuarioSesion(final HttpServletRequest request) {
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion != null && sesion.getAttribute("usuario") != null) {
				final Usuario u = (Usuario) sesion.getAttribute("usuario");
				return (u.getNombreUsuario() == null ? "" : u.getNombreUsuario());
			}
		} catch (final Exception e) {
			System.out.println("MarcarHinchasFutbol, leyendo la sesion: " + e.toString());
		}
		return ("");
	}
}

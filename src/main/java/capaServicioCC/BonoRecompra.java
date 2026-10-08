package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.BonoRecompraCtrl;
import capaDAOCC.BonoRecompraDAO;
import utilidadesCC.AccesoCRM;

/**
 * Servlet implementation class BonoRecompra
 *
 * Las acciones de la pantalla de bono de recompra, distinguidas por "accion".
 * Van juntas porque comparten la misma guarda de acceso al CRM: repetirla en
 * cuatro servlets es repetir algo que algun dia se olvida en el quinto.
 */
@WebServlet("/BonoRecompra")
public class BonoRecompra extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		//Esta pantalla reparte plata: define cuanto se le devuelve a cada
		//cliente. Con mas razon se valida en el servidor y no solo en el menu.
		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		final String accion = request.getParameter("accion") == null
				? "" : request.getParameter("accion").trim();

		if ("listar".equals(accion)) {
			out.write(BonoRecompraCtrl.listar());

		} else if ("guardar".equals(accion)) {
			out.write(BonoRecompraCtrl.guardar(campanaDe(request)));

		} else if ("emisiones".equals(accion)) {
			out.write(BonoRecompraCtrl.emisiones(entero(request.getParameter("idbono")),
					entero(request.getParameter("cuantas"))));

		} else if ("previsualizar".equals(accion)) {
			out.write(BonoRecompraCtrl.previsualizar(entero(request.getParameter("idbono"))));

		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private BonoRecompraDAO.Campana campanaDe(final HttpServletRequest request) {
		final BonoRecompraDAO.Campana c = new BonoRecompraDAO.Campana();
		c.idBono = entero(request.getParameter("idbono"));
		c.nombre = texto(request.getParameter("nombre"));
		c.idOferta = entero(request.getParameter("idoferta"));
		c.compraDesde = texto(request.getParameter("compra_desde"));
		c.compraHasta = texto(request.getParameter("compra_hasta"));
		c.porcentaje = doble(request.getParameter("porcentaje"));
		c.topeBono = doble(request.getParameter("tope_bono"));
		c.baseMinima = doble(request.getParameter("base_minima"));
		c.productos = texto(request.getParameter("productos"));
		c.excluirPromociones = !"N".equals(request.getParameter("excluir_promociones"));
		c.repetible = "S".equals(request.getParameter("repetible"));
		c.estado = texto(request.getParameter("estado"));
		if (c.estado.length() == 0) {
			c.estado = "BORRADOR";
		}
		//Emitir arranca apagado a proposito: primero se mira a cuantos les
		//daria y por cuanto, y despues se prende.
		c.emitir = "S".equals(request.getParameter("emitir"));
		c.avisar = !"N".equals(request.getParameter("avisar"));
		c.abierta = !"N".equals(request.getParameter("abierta"));
		c.idEnvio = largo(request.getParameter("idenvio"));
		c.fechaEmision = texto(request.getParameter("fecha_emision"));
		c.redimeDesde = texto(request.getParameter("redime_desde"));
		c.redimeHasta = texto(request.getParameter("redime_hasta"));
		c.horaDesde = texto(request.getParameter("hora_desde"));
		c.horaHasta = texto(request.getParameter("hora_hasta"));
		c.tiposPedido = texto(request.getParameter("tipos_pedido"));
		c.usuario = AccesoCRM.usuarioEnSesion(request);
		return (c);
	}

	private String texto(final String v) {
		return (v == null ? "" : v.trim());
	}

	private int entero(final String v) {
		try {
			return (Integer.parseInt(v.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	private long largo(final String v) {
		try {
			return (Long.parseLong(v.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	/**
	 * Los valores en pesos se leen con ValorDigitado, que entiende tanto
	 * "50.000" como "50000" y "1.234,50". Escribir el tope con punto de miles y
	 * que se lea como 50 pesos seria un error caro.
	 */
	private double doble(final String v) {
		try {
			final Double d = utilidadesCC.ValorDigitado.leer(v, true);
			return (d == null ? 0 : d.doubleValue());
		} catch (final Exception e) {
			return (0);
		}
	}
}

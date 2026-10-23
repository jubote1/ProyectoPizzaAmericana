package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.CampanaPromoCtrl;

/**
 * Gestiona las campanas que le hacen una pregunta al cliente al finalizar un pedido (por ejemplo
 * "?desea la boleta de Cinepolis?") y registra que respondio.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDCampanaPromo   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Exige sesion de usuario.
 * Operaciones (operacion): 1, 2, 4, 6, 7, 8, 10, 11, 12, 13.
 * Devuelve: application/json; claves: activo, ambito, aplica, cliente, codigoboleta, correo,
 * cuantos, descripcion, detalle, dias, estado, exigecorreo, ....
 * Lee: campana_promo, campana_promo_dia, campana_promo_producto, campana_promo_tienda, cliente,
 * detalle_pedido, excepcion_precio, pedido, pedido_campana_promo, producto.
 * Escribe: campana_promo (DELETE/INSERT/UPDATE), campana_promo_dia (DELETE/INSERT),
 * campana_promo_producto (DELETE/INSERT), campana_promo_tienda (DELETE/INSERT),
 * pedido_campana_promo (INSERT/UPDATE).
 *
 * Trampas y riesgos conocidos:
 * - Corregido el 2026-10-23: las operaciones 11 y 13 guardaban en el campo de usuario el texto
 *   capaModeloCC.Usuario@... en lugar del nombre, porque la clase Usuario no tiene toString().
 *   Los registros anteriores a esa fecha tienen ese texto en pedido_campana_promo.usuario y
 *   usuario_envio.
 * - El estado de un registro que pasa de "no" a "si" quedaba en DESCARTADO (corregido el
 *   2026-10-22).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDCampanaPromo.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDCampanaPromo")
public class CRUDCampanaPromo extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public CRUDCampanaPromo() {
		super();
	}

	private int entero(final HttpServletRequest request, final String nombre) {
		try {
			return (Integer.parseInt(request.getParameter(nombre)));
		} catch (Exception e) {
			return (0);
		}
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String valor = request.getParameter(nombre);
		return (valor == null ? "" : valor);
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		// Una campana define que se le promete a un cliente y en que tiendas, asi
		// que esto no se toca sin sesion.
		final HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion\"}");
			return;
		}

		final int operacion = this.entero(request, "idoperacion");
		final CampanaPromoCtrl ctrl = new CampanaPromoCtrl();
		try {
			if (operacion == 1) {
				out.write(ctrl.guardar(this.entero(request, "idcampana"), this.texto(request, "nombre"),
						"S".equalsIgnoreCase(this.texto(request, "activo")), this.texto(request, "fechadesde"),
						this.texto(request, "fechahasta"), this.texto(request, "pregunta"),
						this.entero(request, "idoferta"),
						"S".equalsIgnoreCase(this.texto(request, "exigecorreo")), this.texto(request, "tiendas"),
						this.texto(request, "dias"), this.texto(request, "productos")));
			} else if (operacion == 2) {
				out.write(ctrl.eliminar(this.entero(request, "idcampana")));
			} else if (operacion == 4) {
				out.write(ctrl.listar());
			} else if (operacion == 6) {
				out.write(ctrl.obtener(this.entero(request, "idcampana")));
			} else if (operacion == 7) {
				out.write(ctrl.consultarNombreProducto(this.texto(request, "ambito"),
						this.entero(request, "idproducto"), this.entero(request, "idexcepcion"),
						this.texto(request, "tiendas")));
			} else if (operacion == 8) {
				out.write(ctrl.listarExcepciones());
			} else if (operacion == 10) {
				out.write(ctrl.consultarParaPedido(this.entero(request, "idpedido")));
			} else if (operacion == 11) {
				out.write(ctrl.marcar(this.entero(request, "idcampana"), this.entero(request, "idtienda"),
						this.entero(request, "idpedido"), this.entero(request, "idpedidotienda"),
						this.entero(request, "idcliente"), this.texto(request, "correo"),
						"S".equalsIgnoreCase(this.texto(request, "solicito")),
						EditarPedidoEnCurso.usuarioDeLaSesion(request), this.texto(request, "observacion")));
			} else if (operacion == 12) {
				out.write(ctrl.listarParaEnviar(this.entero(request, "idcampana"),
						"S".equalsIgnoreCase(this.texto(request, "solopendientes"))));
			} else if (operacion == 13) {
				out.write(ctrl.marcarEnviada(this.entero(request, "idregistro"),
						this.texto(request, "codigoboleta"), EditarPedidoEnCurso.usuarioDeLaSesion(request)));
			} else {
				out.write("[]");
			}
		} catch (Exception e) {
			System.out.println("CRUDCampanaPromo idoperacion " + operacion + ": " + e);
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}

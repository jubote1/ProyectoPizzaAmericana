package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.PromocionReporteCatalogoDAO;

/**
 * Lo que consume la pantalla de administracion del catalogo de promociones.
 */
public class PromocionReporteCtrl {

	@SuppressWarnings("unchecked")
	public String listar() {
		final JSONObject raiz = new JSONObject();
		final JSONArray lista = new JSONArray();
		final ArrayList<PromocionReporteCatalogoDAO.Promocion> promociones =
				PromocionReporteCatalogoDAO.listar();

		for (int i = 0; i < promociones.size(); i++) {
			final PromocionReporteCatalogoDAO.Promocion p = promociones.get(i);
			final JSONObject o = new JSONObject();
			o.put("idpromo", Integer.valueOf(p.idPromo));
			o.put("nombre", p.nombre);
			o.put("plataforma", p.plataforma ? "S" : "N");
			o.put("activo", p.activo ? "S" : "N");
			o.put("orden", Integer.valueOf(p.orden));

			final JSONArray productos = new JSONArray();
			for (int j = 0; j < p.productos.size(); j++) {
				final PromocionReporteCatalogoDAO.Producto prod = p.productos.get(j);
				final JSONObject po = new JSONObject();
				po.put("idproducto", Integer.valueOf(prod.idProducto));
				po.put("descripcion", prod.descripcion);
				po.put("vendidos", Double.valueOf(prod.vendidosMes));
				productos.add(po);
			}
			o.put("productos", productos);
			lista.add(o);
		}
		raiz.put("promociones", lista);
		raiz.put("respuesta", "OK");
		return (raiz.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String buscarProductos(final String texto) {
		final JSONObject raiz = new JSONObject();
		final JSONArray lista = new JSONArray();
		final ArrayList<PromocionReporteCatalogoDAO.Producto> productos =
				PromocionReporteCatalogoDAO.buscarProductos(texto);

		for (int i = 0; i < productos.size(); i++) {
			final PromocionReporteCatalogoDAO.Producto p = productos.get(i);
			final JSONObject o = new JSONObject();
			o.put("idproducto", Integer.valueOf(p.idProducto));
			o.put("descripcion", p.descripcion);
			o.put("vendidos", Double.valueOf(p.vendidosMes));
			o.put("yaen", p.yaEnPromocion);
			lista.add(o);
		}
		raiz.put("productos", lista);
		raiz.put("respuesta", "OK");
		return (raiz.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String crear(final String nombre, final boolean plataforma, final int orden) {
		final JSONObject raiz = new JSONObject();
		if (nombre == null || nombre.trim().length() < 3) {
			raiz.put("respuesta", "INVALIDO");
			raiz.put("detalle", "El nombre tiene que tener al menos tres letras.");
			return (raiz.toJSONString());
		}
		final int id = PromocionReporteCatalogoDAO.crear(nombre, plataforma, orden);
		if (id == 0) {
			raiz.put("respuesta", "NOK");
			//El nombre es unico en la tabla, y es el choque mas probable.
			raiz.put("detalle", "No se pudo crear. Puede que ya exista una promocion con ese nombre.");
			return (raiz.toJSONString());
		}
		raiz.put("respuesta", "OK");
		raiz.put("idpromo", Integer.valueOf(id));
		return (raiz.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String actualizar(final int idPromo, final String nombre, final boolean plataforma,
			final boolean activo, final int orden) {
		final JSONObject raiz = new JSONObject();
		if (nombre == null || nombre.trim().length() < 3) {
			raiz.put("respuesta", "INVALIDO");
			raiz.put("detalle", "El nombre tiene que tener al menos tres letras.");
			return (raiz.toJSONString());
		}
		final boolean listo = PromocionReporteCatalogoDAO.actualizar(idPromo, nombre, plataforma,
				activo, orden);
		raiz.put("respuesta", listo ? "OK" : "NOK");
		if (!listo) {
			raiz.put("detalle", "No se pudo guardar. Puede que el nombre ya lo tenga otra promocion.");
		}
		return (raiz.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String agregarProducto(final int idPromo, final int idProducto) {
		final JSONObject raiz = new JSONObject();
		if (idPromo <= 0 || idProducto <= 0) {
			raiz.put("respuesta", "INVALIDO");
			raiz.put("detalle", "Falta la promocion o el producto.");
			return (raiz.toJSONString());
		}
		final String error = PromocionReporteCatalogoDAO.agregarProducto(idPromo, idProducto);
		if (error.length() > 0) {
			raiz.put("respuesta", "NOK");
			raiz.put("detalle", error);
			return (raiz.toJSONString());
		}
		raiz.put("respuesta", "OK");
		return (raiz.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String quitarProducto(final int idPromo, final int idProducto) {
		final JSONObject raiz = new JSONObject();
		final boolean listo = PromocionReporteCatalogoDAO.quitarProducto(idPromo, idProducto);
		raiz.put("respuesta", listo ? "OK" : "NOK");
		if (!listo) {
			raiz.put("detalle", "No se encontro ese producto en la promocion.");
		}
		return (raiz.toJSONString());
	}
}

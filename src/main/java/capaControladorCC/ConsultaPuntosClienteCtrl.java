package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.ConsultaPuntosClienteDAO;
import capaDAOCC.ConsultaPuntosClienteDAO.Bono;
import capaDAOCC.ConsultaPuntosClienteDAO.Candidato;
import capaDAOCC.ConsultaPuntosClienteDAO.Comportamiento;
import capaDAOCC.ConsultaPuntosClienteDAO.Ficha;
import capaDAOCC.ConsultaPuntosClienteDAO.Movimiento;
import capaDAOCC.ConsultaPuntosClienteDAO.PorMes;
import capaDAOCC.ConsultaPuntosClienteDAO.PorTienda;
import capaDAOCC.ConsultaPuntosClienteDAO.Totales;
import capaDAOCC.ConsultaPuntosClienteDAO.Vencimiento;

/**
 * Arma las respuestas de la pantalla "Consulta de puntos cliente".
 *
 * Dos acciones: BUSCAR (por correo o celular, devuelve a quien encontro) y
 * FICHA (todo sobre una persona). Ver ConsultaPuntosClienteDAO para el detalle
 * de que se calcula y que no se muestra a proposito.
 */
public class ConsultaPuntosClienteCtrl {

	/** Ventana del aviso "puntos por vencer", en dias. La misma del aviso al cliente. */
	public static final int DIAS_AVISO = 60;

	/** Cuantas lineas de historia se devuelven. Mas que esto no se lee, se descarga. */
	private static final int MAXIMO_MOVIMIENTOS = 600;

	public static boolean puedeVer(final int idUsuario) {
		return (ConsultaPuntosClienteDAO.tienePantalla(idUsuario));
	}

	@SuppressWarnings("unchecked")
	public static JSONObject buscar(final String consulta) {
		final JSONObject r = new JSONObject();
		final ArrayList<Candidato> lista = ConsultaPuntosClienteDAO.buscar(consulta);
		final JSONArray arr = new JSONArray();
		for (int i = 0; i < lista.size(); i++) {
			final Candidato c = lista.get(i);
			final JSONObject o = new JSONObject();
			o.put("correo", c.correo);
			o.put("nombre", c.nombre);
			o.put("celular", c.celular);
			o.put("enprograma", c.enPrograma);
			arr.add(o);
		}
		r.put("resultado", "OK");
		r.put("candidatos", arr);
		return (r);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject ficha(final String correo) {
		final JSONObject r = new JSONObject();
		final Ficha f = ConsultaPuntosClienteDAO.ficha(correo);
		final Totales t = ConsultaPuntosClienteDAO.totales(correo, DIAS_AVISO);

		final JSONObject jf = new JSONObject();
		jf.put("correo", f.correo);
		jf.put("nombre", f.nombre);
		jf.put("celular", f.celular);
		jf.put("enprograma", f.enPrograma);
		jf.put("activo", f.activo);
		jf.put("vinculado", f.vinculado);
		jf.put("canal", f.canal);
		jf.put("saldoguardado", f.saldoGuardado);
		jf.put("politicadatos", f.politicaDatos);
		jf.put("haycrm", f.hayCrm);
		r.put("ficha", jf);

		final JSONObject jt = new JSONObject();
		jt.put("acumulaciones", t.acumulaciones);
		jt.put("valorcompras", t.valorCompras);
		jt.put("ganados", t.ganados);
		jt.put("redimidos", t.redimidos);
		jt.put("vencidos", t.vencidos);
		jt.put("saldo", t.saldo);
		jt.put("porvencer", t.porVencer);
		jt.put("porvencer15", t.porVencer15);
		jt.put("porvencer30", t.porVencer30);
		jt.put("diasaviso", t.diasAviso);
		jt.put("proximovencimiento", t.proximoVencimiento);
		jt.put("puntosproximovencimiento", t.puntosProximoVencimiento);
		jt.put("redencionesconfirmadas", t.redencionesConfirmadas);
		jt.put("redencionesreversadas", t.redencionesReversadas);
		jt.put("primeraacumulacion", t.primeraAcumulacion);
		jt.put("ultimaacumulacion", t.ultimaAcumulacion);
		// El saldo guardado en la ficha se desvia a veces del libro; se avisa, no se esconde.
		jt.put("saldocuadra", !f.enPrograma || Math.abs(f.saldoGuardado - t.saldo) <= 1.0);
		r.put("totales", jt);

		final JSONArray jv = new JSONArray();
		final ArrayList<Vencimiento> venc = ConsultaPuntosClienteDAO.vencimientos(correo);
		for (int i = 0; i < venc.size(); i++) {
			final JSONObject o = new JSONObject();
			o.put("fecha", venc.get(i).fecha);
			o.put("puntos", venc.get(i).puntos);
			o.put("atrasado", venc.get(i).atrasado);
			jv.add(o);
		}
		r.put("vencimientos", jv);

		final JSONArray jm = new JSONArray();
		final ArrayList<Movimiento> movs = ConsultaPuntosClienteDAO.movimientos(correo, MAXIMO_MOVIMIENTOS);
		for (int i = 0; i < movs.size(); i++) {
			final Movimiento m = movs.get(i);
			final JSONObject o = new JSONObject();
			o.put("momento", m.momento);
			o.put("tipo", m.tipo);
			o.put("tienda", m.tienda);
			o.put("pedido", m.idPedidoTienda);
			o.put("valor", m.valorNeto);
			o.put("puntos", m.puntos);
			o.put("canal", m.canal);
			o.put("estado", m.estado);
			o.put("detalle", m.detalle);
			jm.add(o);
		}
		r.put("movimientos", jm);

		final Comportamiento c = ConsultaPuntosClienteDAO.comportamiento(correo);
		final JSONObject jc = new JSONObject();
		jc.put("hay", c.hay);
		jc.put("segmento", c.segmento);
		jc.put("pedidos", c.pedidos);
		jc.put("valor", c.valor);
		jc.put("ticketpromedio", c.ticketPromedio);
		jc.put("primerpedido", c.primerPedido);
		jc.put("ultimopedido", c.ultimoPedido);
		jc.put("diassincomprar", c.diasSinComprar);
		jc.put("diasentrepedidos", c.diasEntrePedidos);
		jc.put("tiendasdistintas", c.tiendasDistintas);
		jc.put("tiendahabitual", c.tiendaHabitual);
		jc.put("familiafavorita", c.familiaFavorita);
		jc.put("pizzas12m", c.pizzas12m);
		r.put("comportamiento", jc);

		final JSONArray jtie = new JSONArray();
		final ArrayList<PorTienda> pt = ConsultaPuntosClienteDAO.porTienda(correo);
		for (int i = 0; i < pt.size(); i++) {
			final JSONObject o = new JSONObject();
			o.put("tienda", pt.get(i).tienda);
			o.put("pedidos", pt.get(i).pedidos);
			o.put("valor", pt.get(i).valor);
			o.put("puntos", pt.get(i).puntos);
			o.put("ultima", pt.get(i).ultima);
			jtie.add(o);
		}
		r.put("portienda", jtie);

		final JSONArray jmes = new JSONArray();
		final ArrayList<PorMes> pm = ConsultaPuntosClienteDAO.porMes(correo);
		for (int i = 0; i < pm.size(); i++) {
			final JSONObject o = new JSONObject();
			o.put("mes", pm.get(i).mes);
			o.put("pedidos", pm.get(i).pedidos);
			o.put("valor", pm.get(i).valor);
			o.put("puntos", pm.get(i).puntos);
			jmes.add(o);
		}
		r.put("pormes", jmes);

		final JSONArray jb = new JSONArray();
		final ArrayList<Bono> bonos = ConsultaPuntosClienteDAO.bonos(f.idPersona);
		for (int i = 0; i < bonos.size(); i++) {
			final JSONObject o = new JSONObject();
			o.put("nombre", bonos.get(i).nombre);
			o.put("codigo", bonos.get(i).codigo);
			o.put("valor", bonos.get(i).valor);
			o.put("estado", bonos.get(i).estado);
			o.put("caducidad", bonos.get(i).caducidad);
			jb.add(o);
		}
		r.put("bonos", jb);

		final JSONArray jc2 = new JSONArray();
		final ArrayList<ConsultaPuntosClienteDAO.Premio> premios = ConsultaPuntosClienteDAO.catalogoPuntos();
		for (int i = 0; i < premios.size(); i++) {
			final JSONObject o = new JSONObject();
			o.put("idproducto", premios.get(i).idProducto);
			o.put("nombre", premios.get(i).nombre);
			o.put("tipo", premios.get(i).tipo);
			o.put("puntos", premios.get(i).puntos);
			jc2.add(o);
		}
		r.put("catalogo", jc2);

		//El segmento va APARTE y no solo dentro de comportamiento, a proposito:
		//el POS recibe la ficha sin comportamiento -es historial que no se lee en
		//caja- pero si necesita saber que clase de cliente tiene al frente. Asi
		//las dos pantallas leen lo mismo del mismo lugar.
		final ConsultaPuntosClienteDAO.Segmento seg = ConsultaPuntosClienteDAO.segmento(c.segmento);
		final JSONObject js = new JSONObject();
		js.put("hay", seg.hay);
		js.put("nombre", seg.nombre);
		js.put("descripcion", seg.descripcion);
		js.put("color", seg.color);
		r.put("segmento", js);

		r.put("resultado", "OK");
		return (r);
	}
}

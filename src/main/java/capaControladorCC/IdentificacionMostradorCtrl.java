package capaControladorCC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.IdentificacionMostradorDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * Identificacion del cliente en el mostrador, por tienda y por cajero, para un
 * rango de fechas cualquiera.
 *
 * Es la misma medicion que publica por correo el proceso semanal de Servicios
 * (ServiciosSer.ReporteIdentificacionMostrador), pero a pedido y con el rango
 * que escoja quien consulta. El correo sale con los ultimos siete dias y no
 * sirve para mirar un mes, ni una quincena, ni comparar dos periodos, que es lo
 * que necesita un lider de zona.
 *
 * La consulta a cada tienda vive en capaDAOCC.IdentificacionMostradorDAO, que es
 * una copia de la del POS. Esta copiada, y no llamada, porque el central compila
 * contra una foto del POS en libs/ que esta un ano atrasada; el detalle esta en
 * el comentario de ese DAO. Las dos consultas tienen que dar lo mismo: si se
 * cambia una hay que cambiar la otra.
 *
 * Los pedidos que son solo porciones NO cuentan en "pedidos" ni en
 * "identificados" (decision del 27 de octubre de 2026); se informan aparte en
 * "porcionessolas" para poder decir cuantos se dejaron por fuera.
 *
 * Una tienda que no responda no tumba el reporte: no sale en la tabla y se
 * nombra aparte.
 */
public class IdentificacionMostradorCtrl {

	/** Como se llama el tipo de pedido de mostrador en tipo_pedido. */
	private static final String TIPO_MOSTRADOR = "Punto de Venta";

	/**
	 * Un cajero con menos pedidos que esto no se lista.
	 *
	 * Con tres pedidos un porcentaje no significa nada y solo estorba en la
	 * lista. Es el mismo corte del reporte por correo, a proposito: si los dos
	 * listaran gente distinta nadie sabria a cual creerle.
	 */
	private static final int MINIMO_PEDIDOS_CAJERO = 10;

	private static class FilaTienda {
		int idTienda = 0;
		String nombre = "";
		int pedidos = 0;
		int identificados = 0;
		int seNegaron = 0;
		int porcionesSolas = 0;
		int autPreguntadas = 0;
		int autSi = 0;
		ArrayList<IdentificacionMostradorDAO.Cajero> cajeros =
				new ArrayList<IdentificacionMostradorDAO.Cajero>();

		int porcentaje() {
			return (pedidos > 0 ? (int) Math.round((identificados * 100.0) / pedidos) : 0);
		}

		/** De los no identificados, a cuantos NO se les pregunto. */
		int sinPreguntar() {
			int sin = pedidos - identificados - seNegaron;
			return (sin > 0 ? sin : 0);
		}
	}

	@SuppressWarnings("unchecked")
	public String consultarIdentificacionMostrador(int idTienda, String desde, String hasta) {
		ArrayList<FilaTienda> filas = new ArrayList<FilaTienda>();
		ArrayList<String> sinRespuesta = new ArrayList<String>();

		for (Tienda tienda : TiendaDAO.obtenerTiendas()) {
			if (tienda.getHosbd() == null || tienda.getHosbd().trim().isEmpty()) {
				continue;
			}
			if (idTienda != 0 && idTienda != tienda.getIdTienda()) {
				continue;
			}
			ArrayList<IdentificacionMostradorDAO.Cajero> cajeros = IdentificacionMostradorDAO
					.obtenerIdentificacionMostrador(desde, hasta, tienda.getHosbd(), TIPO_MOSTRADOR);
			if (cajeros.isEmpty()) {
				//Vacio puede ser "no respondio" o "no hubo pedidos de mostrador
				//en el rango". El DAO no los distingue, asi que se nombra con
				//cuidado: se dice que no trajo datos, no que fallo.
				sinRespuesta.add(tienda.getNombreTienda());
				continue;
			}
			FilaTienda fila = new FilaTienda();
			fila.idTienda = tienda.getIdTienda();
			fila.nombre = tienda.getNombreTienda();
			for (IdentificacionMostradorDAO.Cajero c : cajeros) {
				fila.pedidos = fila.pedidos + c.pedidos;
				fila.identificados = fila.identificados + c.identificados;
				fila.seNegaron = fila.seNegaron + c.preguntadosSinDar;
				fila.porcionesSolas = fila.porcionesSolas + c.porcionesSolas;
				//La autorizacion de datos se suma siempre, tenga o no pedidos de
				//mostrador el cajero: pudo haber preguntado en un pedido de otro
				//tipo.
				fila.autPreguntadas = fila.autPreguntadas + c.autorizacionPreguntadas;
				fila.autSi = fila.autSi + c.autorizacionSi;
				if (c.pedidos >= MINIMO_PEDIDOS_CAJERO) {
					fila.cajeros.add(c);
				}
			}
			filas.add(fila);
		}

		//De mejor a peor: la gracia del reporte es poder compararse.
		Collections.sort(filas, new Comparator<FilaTienda>() {
			public int compare(FilaTienda a, FilaTienda b) {
				return (Integer.compare(b.porcentaje(), a.porcentaje()));
			}
		});

		int totalPedidos = 0;
		int totalIdentificados = 0;
		int totalPorcionesSolas = 0;
		JSONArray filasJSON = new JSONArray();
		for (FilaTienda fila : filas) {
			totalPedidos = totalPedidos + fila.pedidos;
			totalIdentificados = totalIdentificados + fila.identificados;
			totalPorcionesSolas = totalPorcionesSolas + fila.porcionesSolas;
			JSONObject filaJSON = new JSONObject();
			filaJSON.put("idtienda", fila.idTienda);
			filaJSON.put("tienda", fila.nombre);
			filaJSON.put("pedidos", fila.pedidos);
			filaJSON.put("identificados", fila.identificados);
			filaJSON.put("porcentaje", fila.porcentaje());
			filaJSON.put("senegaron", fila.seNegaron);
			filaJSON.put("porcionessolas", fila.porcionesSolas);
			filaJSON.put("sinpreguntar", fila.sinPreguntar());
			filaJSON.put("autpreguntadas", fila.autPreguntadas);
			filaJSON.put("autsi", fila.autSi);
			JSONArray cajerosJSON = new JSONArray();
			Collections.sort(fila.cajeros, new Comparator<IdentificacionMostradorDAO.Cajero>() {
				public int compare(IdentificacionMostradorDAO.Cajero a,
						IdentificacionMostradorDAO.Cajero b) {
					return (Integer.compare(b.porcentaje(), a.porcentaje()));
				}
			});
			for (IdentificacionMostradorDAO.Cajero c : fila.cajeros) {
				JSONObject cajeroJSON = new JSONObject();
				cajeroJSON.put("cajero", c.cajero);
				cajeroJSON.put("pedidos", c.pedidos);
				cajeroJSON.put("identificados", c.identificados);
				cajeroJSON.put("porcentaje", c.porcentaje());
				cajeroJSON.put("senego", c.preguntadosSinDar);
				cajeroJSON.put("sinpreguntar", c.sinPreguntar());
				cajerosJSON.add(cajeroJSON);
			}
			filaJSON.put("cajeros", cajerosJSON);
			filasJSON.add(filaJSON);
		}

		JSONArray sinRespuestaJSON = new JSONArray();
		for (String nombre : sinRespuesta) {
			sinRespuestaJSON.add(nombre);
		}

		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON.put("filas", filasJSON);
		respuestaJSON.put("tiendassindatos", sinRespuestaJSON);
		respuestaJSON.put("totalpedidos", totalPedidos);
		respuestaJSON.put("totalidentificados", totalIdentificados);
		respuestaJSON.put("totalporcionessolas", totalPorcionesSolas);
		respuestaJSON.put("totalporcentaje",
				totalPedidos > 0 ? (int) Math.round((totalIdentificados * 100.0) / totalPedidos) : 0);
		respuestaJSON.put("minimopedidoscajero", MINIMO_PEDIDOS_CAJERO);
		return (respuestaJSON.toJSONString());
	}

}

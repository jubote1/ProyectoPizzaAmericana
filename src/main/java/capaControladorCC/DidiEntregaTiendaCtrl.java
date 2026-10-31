package capaControladorCC;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import capaDAOCC.DidiEstadoPendienteDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;
import utilidadesCC.ClientesHttp;

/**
 * Le sube el estado a la tienda cuando DiDi avisa que el domiciliario recogio o
 * entrego, y deja escrito como le fue.
 *
 * POR QUE EXISTE
 *
 * Esto vivia suelto dentro de PedidoCtrl: la llamada a la tienda iba en un try
 * cuyo catch hacia solo un System.out.println. Si la tienda no contestaba, el
 * pedido se quedaba sin subir de estado, no se reintentaba y no habia donde
 * verlo. Del 28 al 30 de octubre de 2026 eso le pasaba a 1 o 2 pedidos por
 * tienda por dia, repartido parejo entre siete tiendas.
 *
 * Ahora cada cambio queda anotado en didi_estado_pendiente con su resultado, y
 * el que falle lo recoge reintentarPendientes(), que corre desde Servicios.
 * Es el mismo arreglo que ya tienen los eventos de Bold en
 * BoldEntregaTiendaCtrl.
 *
 * LOS DOS ESTADOS QUE VIAJAN
 *
 *   140 el domiciliario recogio -> DarSalidaDomicilioPlataforma, estado 5 a 7
 *   160 el domiciliario entrego -> DarEntregaDomicilio, estado 7 a 8
 *
 * El usuario con el que viaja es el 180, "Caja", que es el usuario tecnico de
 * la tienda: por eso en cambios_estado_pedido los cambios automaticos salen a
 * nombre de Caja y no de una persona.
 */
public class DidiEntregaTiendaCtrl {

	public static final int RECOGIO = 140;
	public static final int ENTREGO = 160;

	/** Despues de esto se deja de insistir: la fila queda con su ultimo_error. */
	private static final int MAX_INTENTOS = 8;
	/** Subirle el estado a un pedido de hace dias no le sirve a nadie. */
	private static final int DIAS_ATRAS = 2;
	private static final int LIMITE_POR_CORRIDA = 200;

	/**
	 * Anota el cambio y trata de entregarlo de una vez.
	 *
	 * Se anota ANTES de intentar, para que si el central se cae en mitad de la
	 * llamada el cambio no se pierda: queda pendiente y el reintento lo recoge.
	 *
	 * Si no se pudo ni anotar, se intenta la entrega igual. Perder el rastro es
	 * malo; no entregarle el estado a la tienda es peor.
	 */
	public static boolean registrarYEntregar(final BigInteger idOrdenComercio, final int deliveryStatus,
			final int idTienda, final int numPosHeader) {
		final long idPendiente = DidiEstadoPendienteDAO.registrar(idOrdenComercio, deliveryStatus,
				idTienda, numPosHeader);
		return entregar(idPendiente, idOrdenComercio, deliveryStatus, idTienda, numPosHeader);
	}

	/**
	 * Llama a la tienda y deja escrito el resultado.
	 *
	 * La marca en el central -domiciliario = 1, o entregado- se pone SOLO si la
	 * tienda contesto. Asi esa marca sigue significando lo que siempre
	 * significo: que el estado si llego.
	 */
	private static boolean entregar(final long idPendiente, final BigInteger idOrdenComercio,
			final int deliveryStatus, final int idTienda, final int numPosHeader) {
		final Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda == null || tienda.getUrl() == null || tienda.getUrl().trim().isEmpty()) {
			anotarFallo(idPendiente, "La tienda " + idTienda + " no tiene URL configurada");
			return false;
		}
		final String ruta;
		if (deliveryStatus == RECOGIO) {
			ruta = tienda.getUrl() + "DarSalidaDomicilioPlataforma?idpedido=" + numPosHeader
					+ "&idusuario=180&usuario=Caja";
		} else {
			ruta = tienda.getUrl() + "DarEntregaDomicilio?idpedidotienda=" + numPosHeader
					+ "&claveusuario=2&idtienda=" + idTienda
					+ "&observacion=PedidoEntregadoPorPlataforma";
		}

		try {
			//Contra la URL de una tienda que puede estar apagada: tiempos cortos.
			//Sin esto, una tienda sin corriente deja la peticion de DiDi colgada
			//hasta el timeout largo del cliente compartido.
			final RequestConfig config = RequestConfig.custom().setConnectTimeout(5000)
					.setConnectionRequestTimeout(3000).setSocketTimeout(10000).build();
			final HttpClient client = ClientesHttp.apache();
			final HttpGet request = new HttpGet(ruta);
			request.setConfig(config);

			final HttpResponse respuestaHttp = client.execute(request);
			final int estado = respuestaHttp.getStatusLine().getStatusCode();
			String cuerpo = "";
			try {
				if (respuestaHttp.getEntity() != null) {
					cuerpo = EntityUtils.toString(respuestaHttp.getEntity(), StandardCharsets.UTF_8);
				}
			} finally {
				EntityUtils.consumeQuietly(respuestaHttp.getEntity());
			}

			if (estado < 200 || estado >= 300) {
				anotarFallo(idPendiente, "La tienda respondio HTTP " + estado);
				return false;
			}
			//Una respuesta vacia es lo que antes se trataba como error, y asi se
			//mantiene: la tienda contesta algo cuando de verdad hizo el cambio.
			if (cuerpo == null || cuerpo.trim().isEmpty()) {
				anotarFallo(idPendiente, "La tienda contesto vacio");
				return false;
			}

			if (deliveryStatus == RECOGIO) {
				PedidoDAO.marcarDomiciliarioPlataforma(idOrdenComercio);
			} else {
				PedidoDAO.marcarEntregadoPlataforma(idOrdenComercio);
			}
			if (idPendiente > 0) {
				DidiEstadoPendienteDAO.marcarEntregado(idPendiente);
			}
			return true;
		} catch (final Exception e) {
			anotarFallo(idPendiente, e.toString());
			return false;
		}
	}

	private static void anotarFallo(final long idPendiente, final String motivo) {
		System.out.println("DidiEntregaTiendaCtrl: " + motivo);
		if (idPendiente > 0) {
			DidiEstadoPendienteDAO.marcarFallo(idPendiente, motivo);
		}
	}

	/**
	 * Vuelve a intentar los cambios que no llegaron. Corre desde Servicios.
	 *
	 * @return cuantos se lograron entregar en esta corrida
	 */
	public static int reintentarPendientes() {
		final ArrayList<DidiEstadoPendienteDAO.Pendiente> pendientes =
				DidiEstadoPendienteDAO.pendientes(MAX_INTENTOS, DIAS_ATRAS, LIMITE_POR_CORRIDA);
		int entregados = 0;
		for (final DidiEstadoPendienteDAO.Pendiente p : pendientes) {
			if (entregar(p.idPendiente, p.idOrdenComercio, p.deliveryStatus, p.idTienda, p.numPosHeader)) {
				entregados++;
			}
		}
		System.out.println("Estados DiDi pendientes: " + pendientes.size()
				+ ", entregados a su tienda: " + entregados);
		return entregados;
	}
}

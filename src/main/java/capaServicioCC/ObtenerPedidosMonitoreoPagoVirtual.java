package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * El listado del monitoreo de pagos virtuales.
 *
 * Recibe el rango de fechas. Antes no recibia nada y el rango estaba quemado en
 * la consulta como "hoy": despues de medianoche la pantalla quedaba vacia y no
 * habia forma de revisar un dia anterior.
 */
@WebServlet("/ObtenerPedidosMonitoreoPagoVirtual")
public class ObtenerPedidosMonitoreoPagoVirtual extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Cuantos dias hacia atras se deja consultar de una sola vez. */
	private static final int DIAS_MAXIMO_RANGO = 62;

	public ObtenerPedidosMonitoreoPagoVirtual() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		String hoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String fechaIni = fechaValida(request.getParameter("fechaini"), hoy);
		String fechaFin = fechaValida(request.getParameter("fechafin"), fechaIni);
		if (fechaFin.compareTo(fechaIni) < 0) {
			//Invertidas no se rechazan: se entiende lo que se quiso pedir.
			String intercambio = fechaIni;
			fechaIni = fechaFin;
			fechaFin = intercambio;
		}
		if (diasEntre(fechaIni, fechaFin) > DIAS_MAXIMO_RANGO) {
			//Un rango abierto sobre la tabla de pedidos deja la pantalla colgada y de
			//paso el servidor ocupado. Se recorta y se atiende lo que se pueda.
			fechaIni = sumarDias(fechaFin, -DIAS_MAXIMO_RANGO);
		}

		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.obtenerPedidosMonitoreoPagoVirtual(fechaIni, fechaFin);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Acepta yyyy-MM-dd y tambien dd/MM/yyyy.
	 *
	 * El resto de las pantallas del central usa el datepicker en dd/mm/yyyy y cada
	 * DAO lo voltea con substring. Aqui se admiten las dos formas para que el
	 * formato de la pantalla no sea una trampa, y cualquier otra cosa cae en el
	 * valor por defecto en vez de tumbar la consulta.
	 */
	private String fechaValida(String valor, String porDefecto) {
		if (valor == null || valor.trim().length() != 10) {
			return (porDefecto);
		}
		String fecha = valor.trim();
		if (fecha.charAt(2) == 47 && fecha.charAt(5) == 47) {
			fecha = fecha.substring(6, 10) + "-" + fecha.substring(3, 5) + "-" + fecha.substring(0, 2);
		}
		try {
			SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
			formato.setLenient(false);
			formato.parse(fecha);
			return (fecha);
		} catch (Exception e) {
			return (porDefecto);
		}
	}

	private long diasEntre(String desde, String hasta) {
		try {
			SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
			long milis = formato.parse(hasta).getTime() - formato.parse(desde).getTime();
			return (milis / (24L * 60L * 60L * 1000L));
		} catch (Exception e) {
			return (0);
		}
	}

	private String sumarDias(String fecha, int dias) {
		try {
			SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
			Date base = formato.parse(fecha);
			return (formato.format(new Date(base.getTime() + (dias * 24L * 60L * 60L * 1000L))));
		} catch (Exception e) {
			return (fecha);
		}
	}
}

package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.DashboardCargoCtrl;

/**
 * Dashboard Cargo: pedidos llevados por Rappi Cargo en un rango de fechas,
 * con cumplimiento, tiempos, ranking por tienda, y las asignaciones a Cargo
 * que se cancelaron antes de quedar en firme.
 */
@WebServlet("/ConsultarDashboardCargo")
public class ConsultarDashboardCargo extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final Logger logger = Logger.getLogger("log_file");

		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (final Exception e) {
			idTienda = 0;
		}
		final String fechaDesde = aFechaSql(request.getParameter("fechadesde"));
		final String fechaHasta = aFechaSql(request.getParameter("fechahasta"));
		logger.info("ConsultarDashboardCargo idtienda=" + idTienda + " desde=" + fechaDesde + " hasta=" + fechaHasta);

		final String respuesta = DashboardCargoCtrl.consultarDashboard(idTienda, fechaDesde, fechaHasta);
		final PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/** De dd/MM/yyyy a yyyy-MM-dd. Si ya viene en formato SQL, se deja igual. */
	private String aFechaSql(final String fecha) {
		if (fecha == null || fecha.trim().length() == 0) {
			return ("");
		}
		final String limpia = fecha.trim();
		if (limpia.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			return (limpia);
		}
		try {
			final SimpleDateFormat origen = new SimpleDateFormat("dd/MM/yyyy");
			origen.setLenient(false);
			final Date convertida = origen.parse(limpia);
			return (new SimpleDateFormat("yyyy-MM-dd").format(convertida));
		} catch (final ParseException e) {
			System.out.println("ConsultarDashboardCargo: fecha ilegible '" + limpia + "'");
			return ("");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}

package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Enrutador e Interruptor por Tienda (Etapa 3 - Piloto Manrique).
 *
 * Determina si un pedido con destino a una tienda debe despacharse directamente
 * mediante el nuevo ConectorTiendaPos (sin tabla de homologacion) o si debe
 * continuar por el flujo tradicional de PedidoPOSPMDAO.
 *
 * El parametro en general.parametros es 'MOTOR_PILOTO_TIENDAS'.
 * Puede contener una lista de ids separados por coma (ej. '11' o '11,1').
 * Si esta vacio o no existe, ninguna tienda usa el despacho nuevo.
 */
public class EnrutadorDespachoTienda {

	public static final String PARAMETRO = "MOTOR_PILOTO_TIENDAS";
	private static final long VIGENCIA_MS = 30_000L; // 30 segundos cache

	private static volatile Set<Integer> tiendasPilotoCache = new HashSet<Integer>();
	private static volatile long ultimaLectura = 0L;

	public static boolean esTiendaPiloto(Connection con, int idTienda) {
		if (idTienda <= 0) {
			return false;
		}

		long ahora = System.currentTimeMillis();
		if (ahora - ultimaLectura < VIGENCIA_MS && !tiendasPilotoCache.isEmpty()) {
			return tiendasPilotoCache.contains(idTienda);
		}

		Set<Integer> nuevasTiendas = new HashSet<Integer>();
		String sql = "select valortexto from general.parametros where valorparametro = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, PARAMETRO);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String val = rs.getString(1);
					if (val != null && !val.trim().isEmpty()) {
						String[] partes = val.split(",");
						for (String p : partes) {
							p = p.trim();
							if (!p.isEmpty()) {
								try {
									nuevasTiendas.add(Integer.parseInt(p));
								} catch (NumberFormatException ignorada) {
								}
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			// Ante cualquier fallo en base de datos, no afecta y devuelve falso
		}

		tiendasPilotoCache = nuevasTiendas;
		ultimaLectura = ahora;
		return nuevasTiendas.contains(idTienda);
	}
}

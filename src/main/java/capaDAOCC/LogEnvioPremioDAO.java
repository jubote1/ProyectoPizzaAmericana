package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Deja constancia de cada correo de premio de la ruleta: salio o no, y por que no.
 *
 * Nacio porque en octubre quedaron 31 premios con codigo y sin aviso y no hubo forma de
 * saber que les paso: ControladorEnvioCorreo si clasifica el fallo, pero RuletaCtrl
 * usaba la version booleana y lo botaba, y los fallos solo quedaban en memoria.
 *
 * Anotar NUNCA puede tumbar un envio: si esta tabla falla se escribe en el log y la
 * dispersion sigue. Perder el registro de un correo es malo; no mandarle el premio al
 * cliente por no poder registrarlo es peor.
 */
public class LogEnvioPremioDAO {

	/** Lo anota el proceso de dispersion. */
	public static final String INTENTO_DISPERSION = "DISPERSION";

	/** Lo anota el boton de reenviar correos. */
	public static final String INTENTO_REENVIO = "REENVIO";

	private static final String SQL =
			"INSERT INTO log_envio_premio_ruleta"
			+ " (idresultado, idofertacliente, correo, codigo, resultado, detalle, intento, usuario, fecha)"
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

	public static void anotar(final int idResultado, final int idOfertaCliente, final String correo,
			final String codigo, final String resultado, final String detalle, final String intento,
			final String usuario) {

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final PreparedStatement pst = con1.prepareStatement(SQL);
			pst.setInt(1, idResultado);
			pst.setInt(2, idOfertaCliente);
			pst.setString(3, recortar(correo, 200));
			pst.setString(4, recortar(codigo, 50));
			pst.setString(5, recortar(resultado, 30));
			pst.setString(6, recortar(detalle, 400));
			pst.setString(7, recortar(intento, 20));
			pst.setString(8, recortar(usuario, 50));
			pst.executeUpdate();
			pst.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("No se pudo anotar el envio del premio del resultado "
					+ idResultado + ": " + e.getMessage());
		} finally {
			try {
				if (con1 != null) {
					con1.close();
				}
			} catch (final Exception e1) {
				//Ya se reporto lo importante.
			}
		}
	}

	private static String recortar(final String texto, final int largo) {
		if (texto == null) {
			return ("");
		}
		return (texto.length() <= largo ? texto : texto.substring(0, largo));
	}
}

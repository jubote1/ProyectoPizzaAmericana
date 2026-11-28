package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.http.HttpServletRequest;

import capaConexionPOS.ConexionBaseDatos;

/**
 * Deja constancia de DESDE DONDE se envio cada encuesta: IP, navegador y hora.
 *
 * POR QUE EXISTE. Hasta el 2026-11-27 no se guardaba nada de quien envia una
 * encuesta. Cuando se encontro que un mismo telefono contesto 75 encuestas en
 * una noche cambiando el numero de pedido en la direccion del QR, lo unico con
 * que se pudo medir fue la hora. No hubo manera de saber si salieron del mismo
 * equipo. Sin registro no hay investigacion posible.
 *
 * ESTO NO BLOQUEA NADA. Solo registra. Es a proposito: primero se mira, despues
 * se decide que cerrar.
 *
 * NUNCA INTERRUMPE LA ENCUESTA. Todos los metodos se tragan cualquier error: si
 * falla el registro, la encuesta del cliente tiene que seguir su camino igual.
 * Una encuesta perdida por una tabla de auditoria seria peor que no auditar.
 */
public class EncuestaOrigenDAO {

    public static final String ENCUESTA = "ENCUESTA";
    public static final String RULETA = "RULETA";

    /** Lo que cabe en la columna; una cabecera mas larga se corta, no se pierde la fila. */
    private static final int MAX_LARGO = 255;
    private static final int MAX_IP = 45;
    private static final int MAX_IDIOMA = 60;

    private EncuestaOrigenDAO() {
    }

    /**
     * Registra el origen de un envio. Si algo falla, no pasa nada: no lanza.
     *
     * @param request  la peticion, de donde salen la IP y las cabeceras
     * @param idTienda tienda de la encuesta
     * @param idPedido pedido al que dice referirse
     * @param accion   {@link #ENCUESTA} o {@link #RULETA}
     */
    public static void registrar(final HttpServletRequest request, final int idTienda, final int idPedido,
            final String accion) {
        registrar(request, idTienda, idPedido, accion, null);
    }

    /**
     * Igual que el anterior, anotando ademas como quedo la revision de la firma
     * del QR: OK, SIN_FIRMA, INVALIDA, VENCIDA o SIN_CLAVE. Nulo cuando no se
     * pudo revisar.
     */
    public static void registrar(final HttpServletRequest request, final int idTienda, final int idPedido,
            final String accion, final String firmaEstado) {
        if (request == null) {
            return;
        }
        final String sql = "INSERT INTO encuesta_origen (idtienda, idpedido, accion, ip, ip_reenviada,"
                + " navegador, idioma, referencia, firma_estado) VALUES (?,?,?,?,?,?,?,?,?)";
        final ConexionBaseDatos con = new ConexionBaseDatos();
        try (Connection con1 = con.obtenerConexionBDPrincipal();
                PreparedStatement pstmt = con1.prepareStatement(sql)) {
            pstmt.setInt(1, idTienda);
            pstmt.setInt(2, idPedido);
            pstmt.setString(3, accion == null ? ENCUESTA : accion);
            pstmt.setString(4, recortar(request.getRemoteAddr(), MAX_IP));
            // X-Forwarded-For trae la IP real cuando la peticion paso por un proxy,
            // que es el caso de pizzaamericana.co. Se guardan las dos: la de abajo
            // puede ser siempre la del proxy, y esta la puede falsear quien arma la
            // peticion a mano, asi que ninguna sola alcanza.
            pstmt.setString(5, recortar(primeraCabecera(request, "X-Forwarded-For", "X-Real-IP"), MAX_LARGO));
            pstmt.setString(6, recortar(request.getHeader("User-Agent"), MAX_LARGO));
            pstmt.setString(7, recortar(request.getHeader("Accept-Language"), MAX_IDIOMA));
            pstmt.setString(8, recortar(request.getHeader("Referer"), MAX_LARGO));
            pstmt.setString(9, recortar(firmaEstado, 20));
            pstmt.executeUpdate();
        } catch (final Throwable t) {
            // A proposito: registrar el origen jamas puede tumbar una encuesta.
            System.out.println("EncuestaOrigenDAO: no se pudo registrar el origen - " + t);
        }
    }

    private static String primeraCabecera(final HttpServletRequest request, final String... nombres) {
        for (final String n : nombres) {
            final String v = request.getHeader(n);
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String recortar(final String v, final int largo) {
        if (v == null) {
            return null;
        }
        final String t = v.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= largo ? t : t.substring(0, largo);
    }
}

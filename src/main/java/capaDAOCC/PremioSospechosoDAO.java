package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

// La conexion propia del central (no la del POS): asi esta clase compila
// tambien desde Servicios, que es el otro que la llama.
import conexionCC.ConexionBaseDatos;

/**
 * Marca los premios de ruleta que salieron de encuestas llenadas en masa.
 *
 * POR QUE MARCAR Y NO BLOQUEAR. La dispersion ya es manual: una persona oprime
 * el boton. Entonces la marca alcanza a un humano antes de que se mueva la
 * plata, y el premio sigue siendo dispersable: quien dispersa ve el motivo al
 * lado y decide -lo entrega, o lo reporta-. Bloquear le quitaria la decision a
 * esa persona y obligaria a meterse a la base para soltarlo, y hay casos
 * legitimos que caen en la regla: una familia, un pedido corregido varias
 * veces.
 *
 * POR QUE SE MARCA EN EL MOMENTO DEL GIRO Y NO SOLO POR LOTE. Lo noto Juan
 * David el 28-nov-2026: si solo marcara el proceso nocturno, entre que la
 * persona contesta y el proceso corre hay una ventana en la que alguien puede
 * dispersar el premio, y marcarlo despues ya no sirve de nada. Por eso
 * {@link #marcarSiEsMasiva} se llama desde el mismo servicio que resuelve la
 * ruleta, apenas nace el premio. El proceso por lote queda como red de
 * seguridad y para mandar el aviso.
 *
 * CUANDO SE CRUZA EL UMBRAL SE MARCA TODO EL GRUPO, no solo el giro de ahora:
 * las encuestas 1 a 4 ya habian creado sus premios sin marca, y es al llegar la
 * quinta cuando se sabe que el grupo es masivo.
 *
 * ESTA CLASE VIVE EN EL CENTRAL y la llaman los dos lados -el servicio web y el
 * proceso de Servicios, que compila contra estas fuentes-. Escrita dos veces,
 * el lote y el servicio marcarian cosas distintas.
 *
 * NUNCA LANZA HACIA AFUERA en el camino del cliente: una encuesta no se puede
 * perder porque falle una marca de auditoria.
 */
public class PremioSospechosoDAO {

    public static final String REGLA_TELEFONO = "TELEFONO";
    public static final String REGLA_NAVEGADOR = "NAVEGADOR";

    private static final int UMBRAL_POR_DEFECTO = 5;

    private PremioSospechosoDAO() {
    }

    /** El umbral configurado en general.parametros, o 5. */
    public static int umbral(final Connection con) {
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("select valornumerico from general.parametros"
                        + " where valorparametro = 'VIGILANTEENCUESTA_UMBRAL'")) {
            if (rs.next()) {
                final int v = rs.getInt(1);
                if (v > 0) {
                    return v;
                }
            }
        } catch (final Exception ignored) {
        }
        return UMBRAL_POR_DEFECTO;
    }

    /**
     * Se llama apenas nace un premio. Mira si ese pedido viene de una tanda de
     * encuestas masivas y, si si, marca TODOS los premios pendientes de la tanda.
     * Devuelve cuantos marco. No lanza.
     */
    public static int marcarSiEsMasiva(final int idTienda, final int idPedido) {
        final ConexionBaseDatos cn = new ConexionBaseDatos();
        try (Connection con = cn.obtenerConexionBDPrincipal()) {
            return marcarSiEsMasiva(con, idTienda, idPedido);
        } catch (final Throwable t) {
            System.out.println("PremioSospechosoDAO: no se pudo revisar el premio - " + t);
            return 0;
        }
    }

    public static int marcarSiEsMasiva(final Connection con, final int idTienda, final int idPedido) {
        int marcados = 0;
        try {
            final int umbral = umbral(con);
            marcados += porTelefono(con, idTienda, idPedido, umbral);
            marcados += porNavegador(con, idTienda, idPedido, umbral);
        } catch (final Throwable t) {
            System.out.println("PremioSospechosoDAO: no se pudo revisar el premio - " + t);
        }
        return marcados;
    }

    /** El telefono y el dia de ese pedido, y cuantas encuestas lleva ese grupo. */
    private static int porTelefono(final Connection con, final int idTienda, final int idPedido,
            final int umbral) throws Exception {
        final String sqlGrupo = "select c.telefono, date(min(e.fecha_creacion))"
                + " from pizzaamericana.encuesta_servicio e"
                + " join pizzaamericana.cliente_servicio c"
                + "   on c.idpedido = e.idpedido and c.idtienda = e.idtienda"
                + " where e.idtienda = ? and e.idpedido = ? and c.telefono <> 'CLI-TIENDA'"
                + " group by c.telefono";
        String telefono = null;
        String dia = null;
        try (PreparedStatement ps = con.prepareStatement(sqlGrupo)) {
            ps.setInt(1, idTienda);
            ps.setInt(2, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    telefono = rs.getString(1);
                    dia = rs.getString(2);
                }
            }
        }
        if (telefono == null || dia == null) {
            return 0;
        }
        final String sqlCuenta = "select count(distinct e.idpedido),"
                + " timestampdiff(minute, min(e.fecha_creacion), max(e.fecha_creacion))"
                + " from pizzaamericana.encuesta_servicio e"
                + " join pizzaamericana.cliente_servicio c"
                + "   on c.idpedido = e.idpedido and c.idtienda = e.idtienda"
                + " where e.idtienda = ? and c.telefono = ? and date(e.fecha_creacion) = ?";
        int cuantas = 0;
        int minutos = 0;
        try (PreparedStatement ps = con.prepareStatement(sqlCuenta)) {
            ps.setInt(1, idTienda);
            ps.setString(2, telefono);
            ps.setString(3, dia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cuantas = rs.getInt(1);
                    minutos = rs.getInt(2);
                }
            }
        }
        if (cuantas < umbral) {
            return 0;
        }
        return marcarGrupo(con, REGLA_TELEFONO, telefono, idTienda, dia,
                motivo(cuantas, "el mismo telefono", nombreTienda(con, idTienda), dia, minutos));
    }

    /** Lo mismo pero por IP y navegador. Vacio hasta que encuesta_origen tenga datos. */
    private static int porNavegador(final Connection con, final int idTienda, final int idPedido,
            final int umbral) throws Exception {
        final String llaveSql = "concat(ifnull(o.ip,'?'),' / ',left(ifnull(o.navegador,'?'),60))";
        String llave = null;
        String dia = null;
        try (PreparedStatement ps = con.prepareStatement("select " + llaveSql + ", date(o.fecha)"
                + " from pizzaamericana.encuesta_origen o"
                + " where o.idtienda = ? and o.idpedido = ? and o.ip is not null"
                + " order by o.fecha limit 1")) {
            ps.setInt(1, idTienda);
            ps.setInt(2, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    llave = rs.getString(1);
                    dia = rs.getString(2);
                }
            }
        }
        if (llave == null || dia == null) {
            return 0;
        }
        int cuantas = 0;
        int minutos = 0;
        try (PreparedStatement ps = con.prepareStatement("select count(distinct o.idpedido),"
                + " timestampdiff(minute, min(o.fecha), max(o.fecha))"
                + " from pizzaamericana.encuesta_origen o"
                + " where o.idtienda = ? and " + llaveSql + " = ? and date(o.fecha) = ?")) {
            ps.setInt(1, idTienda);
            ps.setString(2, llave);
            ps.setString(3, dia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cuantas = rs.getInt(1);
                    minutos = rs.getInt(2);
                }
            }
        }
        if (cuantas < umbral) {
            return 0;
        }
        return marcarGrupo(con, REGLA_NAVEGADOR, llave, idTienda, dia,
                motivo(cuantas, "el mismo equipo", nombreTienda(con, idTienda), dia, minutos));
    }

    /** El texto que lee quien dispersa, al lado del premio. */
    public static String motivo(final int encuestas, final String fuente, final String tienda,
            final String dia, final int minutos) {
        return encuestas + " encuestas desde " + fuente + " en " + tienda + " el " + dia
                + (minutos > 0 ? ", en " + minutos + " minutos" : ", en pocos minutos");
    }

    private static String nombreTienda(final Connection con, final int idTienda) {
        try (PreparedStatement ps = con.prepareStatement(
                "select nombre from pizzaamericana.tienda where idtienda = ?")) {
            ps.setInt(1, idTienda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    return rs.getString(1);
                }
            }
        } catch (final Exception ignored) {
        }
        return "la tienda " + idTienda;
    }

    /**
     * Marca todos los premios PENDIENTES del grupo. Solo toca giros que de verdad
     * ganaron -de los 315 pedidos del caso real, 294 no habian ganado nada- y
     * solo los que estan en 0, nunca uno ya dispersado o ya bloqueado.
     *
     * "insert ignore" lo hace repetible: marcar dos veces no duplica ni falla.
     */
    public static int marcarGrupo(final Connection con, final String regla, final String llave,
            final int idTienda, final String dia, final String motivo) throws Exception {
        final String condicion = REGLA_TELEFONO.equals(regla)
                ? " and exists (select 1 from pizzaamericana.encuesta_servicio e"
                        + "   join pizzaamericana.cliente_servicio c"
                        + "     on c.idpedido = e.idpedido and c.idtienda = e.idtienda"
                        + "  where e.idtienda = r.idtienda and e.idpedido = r.idpedido"
                        + "    and c.telefono = ? and date(e.fecha_creacion) = ?)"
                : " and exists (select 1 from pizzaamericana.encuesta_origen o"
                        + "  where o.idtienda = r.idtienda and o.idpedido = r.idpedido"
                        + "    and concat(ifnull(o.ip,'?'),' / ',left(ifnull(o.navegador,'?'),60)) = ?"
                        + "    and date(o.fecha) = ?)";
        final String sql = "insert ignore into pizzaamericana.premio_sospechoso (idresultado, regla, motivo)"
                + " select r.idresultado, ?, ?"
                + "   from pizzaamericana.resultado_ruleta r"
                + "   join pizzaamericana.ruleta_oferta ro on ro.idopcion = r.idopcion and ro.activo = 'S'"
                + "  where r.idtienda = ? and r.idofertacliente = 0" + condicion;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, regla);
            ps.setString(2, motivo == null ? "" : (motivo.length() > 200 ? motivo.substring(0, 200) : motivo));
            ps.setInt(3, idTienda);
            ps.setString(4, llave);
            ps.setString(5, dia);
            return ps.executeUpdate();
        }
    }
}

package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;

import conexionCC.ConexionBaseDatos;

public class TercerizadoDomicilioEventoDAO {

    public static boolean guardarEvento(
            long idPedido,
            String proveedor,
            String idPedidoProveedor,
            String idPedidoLogistico,
            String estado,
            String urlSeguimiento,
            String fechaEntrega,
            String fechaCancelacion,
            String jsonEvento) {

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDPrincipal();
        PreparedStatement ps = null;

        try {

            String sql =
                    "INSERT INTO tercerizado_domicilio_evento ("
                  + "id_pedido,"
                  + "proveedor,"
                  + "id_pedido_proveedor,"
                  + "id_pedido_logistico,"
                  + "estado,"
                  + "url_seguimiento,"
                  + "fecha_entrega,"
                  + "fecha_cancelacion,"
                  + "json_evento,"
                  + "procesado,"
                  + "error_proceso"
                  + ") VALUES (?,?,?,?,?,?,?,?,?,0,NULL)";

            ps = con1.prepareStatement(sql);

            ps.setLong(1, idPedido);
            ps.setString(2, proveedor);
            ps.setString(3, idPedidoProveedor);
            ps.setString(4, idPedidoLogistico);
            ps.setString(5, estado);
            ps.setString(6, urlSeguimiento);

            // Fecha de entrega
            Timestamp tsEntrega = convertirTimestamp(fechaEntrega);

            if (tsEntrega == null) {
                ps.setNull(7, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(7, tsEntrega);
            }

            // Fecha de cancelación
            Timestamp tsCancelacion = convertirTimestamp(fechaCancelacion);

            if (tsCancelacion == null) {
                ps.setNull(8, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(8, tsCancelacion);
            }

            ps.setString(9, jsonEvento);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;

        } finally {

            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (con1 != null) {
                    con1.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Convierte diferentes formatos de fecha a Timestamp.
     *
     * Soporta:
     * 2022-09-12T21:41:39.694Z
     * 2022-09-12T21:41:39Z
     * 2022-09-12T21:41:39
     * 2022-09-12 21:41:39
     */
    private static Timestamp convertirTimestamp(String fecha) {

        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }

        fecha = fecha.trim();

        // Formato ISO-8601 con zona horaria (ej: 2022-09-12T21:41:39.694Z)
        try {
            return Timestamp.from(Instant.parse(fecha));
        } catch (Exception ignored) {
        }

        // Formato ISO-8601 sin zona horaria (ej: 2022-09-12T21:41:39)
        try {
            LocalDateTime ldt = LocalDateTime.parse(fecha);
            return Timestamp.valueOf(ldt);
        } catch (Exception ignored) {
        }

        // Formato SQL (ej: 2022-09-12 21:41:39)
        try {
            return Timestamp.valueOf(fecha);
        } catch (Exception ignored) {
        }

        System.err.println("Formato de fecha no soportado recibido de Rappi: " + fecha);

        return null;
    }

}
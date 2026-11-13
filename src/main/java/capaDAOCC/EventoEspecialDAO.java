package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import capaModeloCC.EventoEspecial;
import conexionCC.ConexionBaseDatos;

/**
 * Planificador de eventos especiales: novenas, celebraciones empresariales y
 * fiestas de fin de ano. La tabla vive en pizzaamericana, que es a donde apunta
 * obtenerConexionBDPrincipal().
 *
 * Nada de esto borra filas: un evento que se cae queda en CANCELADO, porque
 * saber que un cliente aparto y despues desistio vale tanto como saber que
 * compro.
 */
public class EventoEspecialDAO {

    private static final Logger logger = Logger.getLogger("log_file");

    /** Columnas del evento, con el nombre de la tienda pegado. */
    private static final String SELECT_BASE =
        "SELECT e.idevento, e.tipo_evento, e.nombre_evento, e.nombre_cliente, e.empresa, e.telefono, e.email, "
      + "       e.idcliente, e.idlead, DATE_FORMAT(e.fecha_evento,'%Y-%m-%d') AS fecha_evento, "
      + "       TIME_FORMAT(e.hora_evento,'%H:%i') AS hora_evento, e.cantidad_pizzas, e.detalle_pedido, "
      + "       e.idtienda, IFNULL(t.nombre,'') AS nombre_tienda, e.direccion_entrega, e.valor_estimado, "
      + "       e.anticipo, e.estado, e.observacion, e.usuario_registra, "
      + "       DATE_FORMAT(e.creado_en,'%Y-%m-%d %H:%i') AS creado_en, e.usuario_modifica, "
      + "       DATE_FORMAT(e.modificado_en,'%Y-%m-%d %H:%i') AS modificado_en "
      + "  FROM evento_especial e "
      + "  LEFT JOIN tienda t ON t.idtienda = e.idtienda ";

    /**
     * Los eventos de un rango de fechas. El planificador pide un mes completo;
     * los reportes pueden pedir la temporada entera.
     *
     * @param fechaDesde  'aaaa-mm-dd', obligatoria
     * @param fechaHasta  'aaaa-mm-dd', obligatoria
     * @param estado      TENTATIVO / APARTADO / CONFIRMADO / ENTREGADO / CANCELADO; vacio = todos menos cancelados
     * @param idtienda    0 = todas
     * @param tipo        vacio = todos
     * @param filtro      texto libre sobre cliente, empresa, telefono o nombre del evento
     */
    public static List<EventoEspecial> listar(String fechaDesde, String fechaHasta, String estado,
                                              int idtienda, String tipo, String filtro) {
        List<EventoEspecial> eventos = new ArrayList<>();
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();

        if (conn == null) {
            logger.error("EventoEspecialDAO.listar: sin conexion a la base principal");
            return eventos;
        }

        StringBuilder sql = new StringBuilder(SELECT_BASE);
        sql.append(" WHERE e.fecha_evento BETWEEN ? AND ? ");
        List<Object> params = new ArrayList<>();
        params.add(fechaDesde);
        params.add(fechaHasta);

        if (estado != null && !estado.trim().isEmpty() && !estado.equalsIgnoreCase("TODOS")) {
            sql.append(" AND e.estado = ? ");
            params.add(estado.trim().toUpperCase());
        } else {
            // Sin filtro explicito, los cancelados no estorban en el calendario.
            sql.append(" AND e.estado <> 'CANCELADO' ");
        }

        if (idtienda > 0) {
            sql.append(" AND e.idtienda = ? ");
            params.add(Integer.valueOf(idtienda));
        }

        if (tipo != null && !tipo.trim().isEmpty() && !tipo.equalsIgnoreCase("TODOS")) {
            sql.append(" AND e.tipo_evento = ? ");
            params.add(tipo.trim().toUpperCase());
        }

        if (filtro != null && !filtro.trim().isEmpty()) {
            String f = "%" + filtro.trim() + "%";
            sql.append(" AND (e.nombre_cliente LIKE ? OR e.empresa LIKE ? OR e.telefono LIKE ? OR e.nombre_evento LIKE ?) ");
            for (int i = 0; i < 4; i++) {
                params.add(f);
            }
        }

        sql.append(" ORDER BY e.fecha_evento, e.hora_evento, e.idevento ");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapear(rs));
                }
            }
        } catch (Exception e) {
            logger.error("EventoEspecialDAO.listar: " + e.getMessage(), e);
        } finally {
            cerrar(conn);
        }

        return eventos;
    }

    /** Un evento por su id, para abrirlo y editarlo. */
    public static EventoEspecial porId(int idevento) {
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " WHERE e.idevento = ? ")) {
            ps.setInt(1, idevento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (Exception e) {
            logger.error("EventoEspecialDAO.porId: " + e.getMessage(), e);
        } finally {
            cerrar(conn);
        }
        return null;
    }

    /**
     * Guarda el evento. Si trae idevento lo actualiza; si no, lo crea.
     * Devuelve el idevento, o -1 si no pudo.
     */
    public static int guardar(EventoEspecial ev) {
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) {
            logger.error("EventoEspecialDAO.guardar: sin conexion a la base principal");
            return -1;
        }

        try {
            if (ev.getIdevento() > 0) {
                String sql = "UPDATE evento_especial SET tipo_evento=?, nombre_evento=?, nombre_cliente=?, empresa=?, "
                           + " telefono=?, email=?, fecha_evento=?, hora_evento=?, cantidad_pizzas=?, detalle_pedido=?, "
                           + " idtienda=?, direccion_entrega=?, valor_estimado=?, anticipo=?, estado=?, observacion=?, "
                           + " usuario_modifica=?, modificado_en=NOW() "
                           + " WHERE idevento=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int i = llenarComunes(ps, ev);
                    ps.setString(i++, nvl(ev.getUsuarioModifica()));
                    ps.setInt(i, ev.getIdevento());
                    return ps.executeUpdate() > 0 ? ev.getIdevento() : -1;
                }
            }

            String sql = "INSERT INTO evento_especial (tipo_evento, nombre_evento, nombre_cliente, empresa, telefono, "
                       + " email, fecha_evento, hora_evento, cantidad_pizzas, detalle_pedido, idtienda, direccion_entrega, "
                       + " valor_estimado, anticipo, estado, observacion, usuario_registra, idcliente, idlead) "
                       + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = llenarComunes(ps, ev);
                ps.setString(i++, nvl(ev.getUsuarioRegistra()));
                if (ev.getIdcliente() > 0) {
                    ps.setInt(i++, ev.getIdcliente());
                } else {
                    ps.setNull(i++, java.sql.Types.INTEGER);
                }
                if (ev.getIdlead() > 0) {
                    ps.setInt(i, ev.getIdlead());
                } else {
                    ps.setNull(i, java.sql.Types.INTEGER);
                }

                if (ps.executeUpdate() == 0) {
                    return -1;
                }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                return -1;
            }
        } catch (Exception e) {
            logger.error("EventoEspecialDAO.guardar: " + e.getMessage(), e);
            return -1;
        } finally {
            cerrar(conn);
        }
    }

    /** Mueve el evento de estado y deja constancia de quien lo movio. */
    public static boolean cambiarEstado(int idevento, String estado, String usuario) {
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) {
            return false;
        }
        String sql = "UPDATE evento_especial SET estado=?, usuario_modifica=?, modificado_en=NOW() WHERE idevento=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.trim().toUpperCase());
            ps.setString(2, nvl(usuario));
            ps.setInt(3, idevento);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("EventoEspecialDAO.cambiarEstado: " + e.getMessage(), e);
            return false;
        } finally {
            cerrar(conn);
        }
    }

    // -----------------------------------------------------------------------
    // Ayudas internas
    // -----------------------------------------------------------------------

    /** Los 16 parametros que comparten el INSERT y el UPDATE, en el mismo orden. */
    private static int llenarComunes(PreparedStatement ps, EventoEspecial ev) throws Exception {
        int i = 1;
        ps.setString(i++, vacioPor(ev.getTipoEvento(), "NOVENA").toUpperCase());
        ps.setString(i++, nvl(ev.getNombreEvento()));
        ps.setString(i++, nvl(ev.getNombreCliente()));
        ps.setString(i++, nvl(ev.getEmpresa()));
        ps.setString(i++, nvl(ev.getTelefono()));
        ps.setString(i++, nvl(ev.getEmail()));
        ps.setString(i++, nvl(ev.getFechaEvento()));
        ps.setString(i++, vacioPor(ev.getHoraEvento(), "19:00"));
        ps.setInt(i++, ev.getCantidadPizzas());
        ps.setString(i++, nvl(ev.getDetallePedido()));
        if (ev.getIdtienda() > 0) {
            ps.setInt(i++, ev.getIdtienda());
        } else {
            ps.setNull(i++, java.sql.Types.INTEGER);
        }
        ps.setString(i++, nvl(ev.getDireccionEntrega()));
        ps.setDouble(i++, ev.getValorEstimado());
        ps.setDouble(i++, ev.getAnticipo());
        ps.setString(i++, vacioPor(ev.getEstado(), "TENTATIVO").toUpperCase());
        ps.setString(i++, nvl(ev.getObservacion()));
        return i;
    }

    private static EventoEspecial mapear(ResultSet rs) throws Exception {
        EventoEspecial ev = new EventoEspecial();
        ev.setIdevento(rs.getInt("idevento"));
        ev.setTipoEvento(rs.getString("tipo_evento"));
        ev.setNombreEvento(rs.getString("nombre_evento"));
        ev.setNombreCliente(rs.getString("nombre_cliente"));
        ev.setEmpresa(rs.getString("empresa"));
        ev.setTelefono(rs.getString("telefono"));
        ev.setEmail(rs.getString("email"));
        ev.setIdcliente(rs.getInt("idcliente"));
        ev.setIdlead(rs.getInt("idlead"));
        ev.setFechaEvento(rs.getString("fecha_evento"));
        ev.setHoraEvento(rs.getString("hora_evento"));
        ev.setCantidadPizzas(rs.getInt("cantidad_pizzas"));
        ev.setDetallePedido(rs.getString("detalle_pedido"));
        ev.setIdtienda(rs.getInt("idtienda"));
        ev.setNombreTienda(rs.getString("nombre_tienda"));
        ev.setDireccionEntrega(rs.getString("direccion_entrega"));
        ev.setValorEstimado(rs.getDouble("valor_estimado"));
        ev.setAnticipo(rs.getDouble("anticipo"));
        ev.setEstado(rs.getString("estado"));
        ev.setObservacion(rs.getString("observacion"));
        ev.setUsuarioRegistra(rs.getString("usuario_registra"));
        ev.setCreadoEn(rs.getString("creado_en"));
        ev.setUsuarioModifica(rs.getString("usuario_modifica"));
        ev.setModificadoEn(rs.getString("modificado_en"));
        return ev;
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private static String vacioPor(String s, String porDefecto) {
        return (s == null || s.trim().isEmpty()) ? porDefecto : s.trim();
    }

    private static void cerrar(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            logger.error("EventoEspecialDAO: no cerro la conexion - " + e.getMessage());
        }
    }
}

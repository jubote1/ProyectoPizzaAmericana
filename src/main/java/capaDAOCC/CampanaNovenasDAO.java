package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import capaModeloCC.CampanaNovenasBitacora;
import capaModeloCC.CampanaNovenasLead;
import capaModeloCC.VentaAtribuidaNovenas;
import conexionCC.ConexionBaseDatos;

/**
 * Data Access Object para la Campaña de Novenas y Venta Empresarial 25 Años.
 */
public class CampanaNovenasDAO {

    private static final Logger logger = Logger.getLogger("log_file");

    /**
     * Busca clientes precargados según filtros de texto, clasificación y estado de gestión.
     */
    public static List<CampanaNovenasLead> buscarLeads(String filtro, String clasificacion, String estado, int limite) {
        List<CampanaNovenasLead> leads = new ArrayList<>();
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();

        if (conn == null) {
            return leads;
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM campana_novenas_lead WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (filtro != null && !filtro.trim().isEmpty()) {
            String f = "%" + filtro.trim() + "%";
            sql.append(" AND (nombre_cliente LIKE ? OR empresa LIKE ? OR telefono LIKE ? OR email LIKE ? OR direccion LIKE ? OR nit_cedula LIKE ? OR razon_social LIKE ?) ");
            for (int i = 0; i < 7; i++) {
                params.add(f);
            }
        }

        if (clasificacion != null && !clasificacion.trim().isEmpty() && !clasificacion.equalsIgnoreCase("TODAS")) {
            sql.append(" AND clasificacion = ? ");
            params.add(clasificacion.trim());
        }

        if (estado != null && !estado.trim().isEmpty() && !estado.equalsIgnoreCase("TODOS")) {
            sql.append(" AND estado_gestion = ? ");
            params.add(estado.trim());
        }

        sql.append(" ORDER BY total_pizzas_historico DESC, total_comprado_historico DESC LIMIT ?");
        params.add(limite > 0 ? limite : 100);

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CampanaNovenasLead l = new CampanaNovenasLead();
                    l.setIdlead(rs.getInt("idlead"));
                    l.setIdcliente(rs.getInt("idcliente"));
                    l.setNombreCliente(rs.getString("nombre_cliente"));
                    l.setEmpresa(rs.getString("empresa"));
                    l.setTelefono(rs.getString("telefono"));
                    l.setEmail(rs.getString("email"));
                    l.setDireccion(rs.getString("direccion"));
                    l.setObservacionCliente(rs.getString("observacion_cliente"));
                    l.setClasificacion(rs.getString("clasificacion"));
                    l.setCriterioClasificacion(rs.getString("criterio_clasificacion"));
                    l.setTotalPizzasHistorico(rs.getInt("total_pizzas_historico"));
                    l.setTotalPedidosHistorico(rs.getInt("total_pedidos_historico"));
                    l.setTotalCompradoHistorico(rs.getDouble("total_comprado_historico"));
                    l.setUltimaFechaCompra(rs.getString("ultima_fecha_compra"));
                    l.setFacturaElectronica(rs.getString("factura_electronica"));
                    l.setNitCedula(rs.getString("nit_cedula"));
                    l.setRazonSocial(rs.getString("razon_social"));
                    l.setPizzasPreferidas(rs.getString("pizzas_preferidas"));
                    l.setEstadoGestion(rs.getString("estado_gestion"));
                    l.setCreadoEn(rs.getString("creado_en"));
                    leads.add(l);
                }
            }
        } catch (Exception e) {
            logger.error("Error en buscarLeads: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }

        return leads;
    }

    /**
     * Obtiene el detalle de un lead por su ID.
     */
    public static CampanaNovenasLead obtenerLeadPorId(int idlead) {
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) return null;

        String sql = "SELECT * FROM campana_novenas_lead WHERE idlead = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idlead);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CampanaNovenasLead l = new CampanaNovenasLead();
                    l.setIdlead(rs.getInt("idlead"));
                    l.setIdcliente(rs.getInt("idcliente"));
                    l.setNombreCliente(rs.getString("nombre_cliente"));
                    l.setEmpresa(rs.getString("empresa"));
                    l.setTelefono(rs.getString("telefono"));
                    l.setEmail(rs.getString("email"));
                    l.setDireccion(rs.getString("direccion"));
                    l.setObservacionCliente(rs.getString("observacion_cliente"));
                    l.setClasificacion(rs.getString("clasificacion"));
                    l.setCriterioClasificacion(rs.getString("criterio_clasificacion"));
                    l.setTotalPizzasHistorico(rs.getInt("total_pizzas_historico"));
                    l.setTotalPedidosHistorico(rs.getInt("total_pedidos_historico"));
                    l.setTotalCompradoHistorico(rs.getDouble("total_comprado_historico"));
                    l.setUltimaFechaCompra(rs.getString("ultima_fecha_compra"));
                    l.setFacturaElectronica(rs.getString("factura_electronica"));
                    l.setNitCedula(rs.getString("nit_cedula"));
                    l.setRazonSocial(rs.getString("razon_social"));
                    l.setPizzasPreferidas(rs.getString("pizzas_preferidas"));
                    l.setEstadoGestion(rs.getString("estado_gestion"));
                    l.setCreadoEn(rs.getString("creado_en"));
                    return l;
                }
            }
        } catch (Exception e) {
            logger.error("Error en obtenerLeadPorId: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Consulta el historial de notas y contactos en la bitácora para un lead.
     */
    public static List<CampanaNovenasBitacora> obtenerBitacoraPorLead(int idlead) {
        List<CampanaNovenasBitacora> bitacoras = new ArrayList<>();
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) return bitacoras;

        String sql = "SELECT * FROM campana_novenas_bitacora WHERE idlead = ? ORDER BY fecha_contacto DESC, idbitacora DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idlead);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CampanaNovenasBitacora b = new CampanaNovenasBitacora();
                    b.setIdbitacora(rs.getInt("idbitacora"));
                    b.setIdlead(rs.getInt("idlead"));
                    b.setIdcliente(rs.getInt("idcliente"));
                    b.setUsuarioAsesor(rs.getString("usuario_asesor"));
                    b.setFechaContacto(rs.getString("fecha_contacto"));
                    b.setEstadoContacto(rs.getString("estado_contacto"));
                    b.setFechaTentativaEvento(rs.getString("fecha_tentativa_evento"));
                    b.setPizzasEstimadas(rs.getInt("pizzas_estimadas"));
                    b.setObservacion(rs.getString("observacion"));
                    b.setCreadoEn(rs.getString("creado_en"));
                    bitacoras.add(b);
                }
            }
        } catch (Exception e) {
            logger.error("Error en obtenerBitacoraPorLead: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
        return bitacoras;
    }

    /**
     * Guarda una nueva entrada en la bitácora y actualiza el estado del lead.
     */
    public static boolean guardarBitacora(CampanaNovenasBitacora bitacora) {
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) return false;

        String sqlInsert = "INSERT INTO campana_novenas_bitacora "
                + "(idlead, idcliente, usuario_asesor, fecha_contacto, estado_contacto, fecha_tentativa_evento, pizzas_estimadas, observacion) "
                + "VALUES (?, ?, ?, NOW(), ?, ?, ?, ?)";

        String sqlUpdateLead = "UPDATE campana_novenas_lead SET estado_gestion = ? WHERE idlead = ?";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                ps1.setInt(1, bitacora.getIdlead());
                ps1.setInt(2, bitacora.getIdcliente());
                ps1.setString(3, bitacora.getUsuarioAsesor());
                ps1.setString(4, bitacora.getEstadoContacto());

                if (bitacora.getFechaTentativaEvento() != null && !bitacora.getFechaTentativaEvento().trim().isEmpty()) {
                    ps1.setString(5, bitacora.getFechaTentativaEvento().trim());
                } else {
                    ps1.setNull(5, java.sql.Types.DATE);
                }

                ps1.setInt(6, bitacora.getPizzasEstimadas());
                ps1.setString(7, bitacora.getObservacion());
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(sqlUpdateLead)) {
                ps2.setString(1, bitacora.getEstadoContacto());
                ps2.setInt(2, bitacora.getIdlead());
                ps2.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            logger.error("Error en guardarBitacora: " + e.getMessage(), e);
            return false;
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Consulta ventas atribuidas al asesor:
     * REGLA DE ORO DEL USUARIO:
     * "El asesor va a ver qué ventas estuvieron asociadas de acuerdo a las personas que él contactó.
     *  Le vamos a contar tres o más pizzas. Si ese cliente llamó y pidió menos de tres pizzas,
     *  NO se le tendrá en cuenta. Si pidió más de tres pizzas, SÍ se le va a tener en cuenta."
     */
    public static List<VentaAtribuidaNovenas> consultarVentasAtribuidas(String usuarioAsesor, String fechaInicial, String fechaFinal) {
        List<VentaAtribuidaNovenas> ventas = new ArrayList<>();
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection conn = con.obtenerConexionBDPrincipal();
        if (conn == null) return ventas;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("    p.idpedidotienda, ")
           .append("    p.idtienda, ")
           .append("    p.fechapedido, ")
           .append("    p.total_bruto, ")
           .append("    p.impuesto, ")
           .append("    p.total_neto, ")
           .append("    c.idcliente, ")
           .append("    TRIM(CONCAT(COALESCE(c.nombre,''), ' ', COALESCE(c.apellido,''))) AS nombre_cliente, ")
           .append("    COALESCE(NULLIF(c.nombrecompania,''), 'Persona Natural / Grupo') AS organizacion, ")
           .append("    COALESCE(NULLIF(c.telefono_celular,''), c.telefono) AS telefono, ")
           .append("    b.usuario_asesor, ")
           .append("    MIN(b.fecha_contacto) AS fecha_contacto_asesor, ")
           .append("    CAST(SUM(d.cantidad) AS UNSIGNED) AS total_pizzas, ")
           .append("    GROUP_CONCAT(DISTINCT pr.descripcion SEPARATOR ' | ') AS pizzas_detalle ")
           .append("FROM pedido p ")
           .append("JOIN cliente c ON p.idcliente = c.idcliente ")
           .append("JOIN campana_novenas_bitacora b ON p.idcliente = b.idcliente ")
           .append("JOIN detalle_pedido d ON p.idpedidotienda = d.idpedidotienda ")
           .append("JOIN producto pr ON d.idproducto = pr.idproducto ")
           .append("WHERE (pr.tipo_producto = 'P' OR pr.descripcion LIKE '%pizza%') ")
           .append("  AND p.fechapedido >= DATE(b.fecha_contacto) ") // El pedido debe ser posterior o igual al contacto
           .append("  AND p.idestado NOT IN (3, 4) "); // Excluir anulados

        List<Object> params = new ArrayList<>();

        if (usuarioAsesor != null && !usuarioAsesor.trim().isEmpty() && !usuarioAsesor.equalsIgnoreCase("TODOS")) {
            sql.append(" AND b.usuario_asesor = ? ");
            params.add(usuarioAsesor.trim());
        }

        if (fechaInicial != null && !fechaInicial.trim().isEmpty()) {
            sql.append(" AND p.fechapedido >= ? ");
            params.add(fechaInicial.trim());
        }

        if (fechaFinal != null && !fechaFinal.trim().isEmpty()) {
            sql.append(" AND p.fechapedido <= ? ");
            params.add(fechaFinal.trim());
        }

        sql.append("GROUP BY p.idpedidotienda, p.idtienda, p.fechapedido, p.total_bruto, p.impuesto, p.total_neto, c.idcliente, b.usuario_asesor ")
           .append("HAVING total_pizzas >= 3 ") // REGLA: MÍNIMO 3 PIZZAS
           .append("ORDER BY p.fechapedido DESC, total_pizzas DESC;");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VentaAtribuidaNovenas v = new VentaAtribuidaNovenas();
                    v.setIdpedidotienda(rs.getInt("idpedidotienda"));
                    v.setIdtienda(rs.getInt("idtienda"));
                    v.setNombreTienda("Tienda " + rs.getInt("idtienda"));
                    v.setIdcliente(rs.getInt("idcliente"));
                    v.setNombreCliente(rs.getString("nombre_cliente"));
                    v.setOrganizacion(rs.getString("organizacion"));
                    v.setTelefono(rs.getString("telefono"));
                    v.setFechaPedido(rs.getString("fechapedido"));
                    v.setFechaContactoAsesor(rs.getString("fecha_contacto_asesor"));
                    v.setUsuarioAsesor(rs.getString("usuario_asesor"));
                    v.setTotalPizzasPedido(rs.getInt("total_pizzas"));
                    v.setTotalBruto(rs.getDouble("total_bruto"));
                    v.setImpuesto(rs.getDouble("impuesto"));
                    double neto = rs.getDouble("total_neto");
                    v.setTotalNeto(neto);
                    v.setComisionEstimada(neto * 0.02); // 2% de comisión comercial sugerida
                    v.setPizzasDetalle(rs.getString("pizzas_detalle"));
                    v.setEstadoPedido("CONFIRMADO (>= 3 PIZZAS)");
                    ventas.add(v);
                }
            }
        } catch (Exception e) {
            logger.error("Error en consultarVentasAtribuidas: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }

        return ventas;
    }
}

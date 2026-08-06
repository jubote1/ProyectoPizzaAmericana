package capaDAOCC;

import java.lang.System.Logger;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;

import capaModeloCC.DetallePedido;
import capaModeloCC.RappiCargoOrden;
import capaModeloCC.RappiCargoOrden.RappiCargoProducto;
import conexionCC.ConexionBaseDatos;

public class TercerizadoDomicilioEventoDAO {

	

	public static RappiCargoOrden ConsultarPedidoRappiCargo(int idPedido) {

	    RappiCargoOrden orden = null;

	    String consulta =
	            "SELECT " +
	            "p.idpedido, " +
	            "p.total_neto, " +
	            "p.idtienda, " +
	            "c.nombre, " +
	            "c.apellido, " +
	            "c.telefono, " +
	            "c.telefono_celular, " +
	            "c.email, " +
	            "c.direccion, " +
	            "c.latitud, " +
	            "c.longitud, " +
	            "c.observacion, " +
	            "m.nombre AS municipio, " +
	            "f.idforma_pago " +	            
	            "FROM pedido p " +
	            "INNER JOIN cliente c ON p.idcliente = c.idcliente " +
	            "LEFT JOIN municipio m ON c.idmunicipio = m.idmunicipio " +
	            "INNER JOIN pedido_forma_pago f ON f.idpedido = p.idpedido " +
	            "WHERE p.idpedido = ?";

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();

	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {

	        ps = con1.prepareStatement(consulta);
	        ps.setInt(1, idPedido);

	        rs = ps.executeQuery();

	        if (rs.next()) {

	            orden = new RappiCargoOrden();

	            orden.setOrderId(String.valueOf(rs.getInt("idpedido")));
	            orden.setInStoreReferenceId(String.valueOf(rs.getInt("idpedido")));

	            orden.setTotalValue((int) rs.getDouble("total_neto"));
	            orden.setUserTip(0);

	            orden.setVehicleType("BIKE");
	            orden.setPaymentMethod("ONLINE");
	            
	            if(rs.getInt("idforma_pago") == 1) {
	            	  orden.setPaymentMethod("CASH");
	            }
	          

	            orden.setExternalPickingPointId(String.valueOf(rs.getInt("idtienda")));
	            orden.setInstructions("");

	            String nombre = rs.getString("nombre");
	            String apellido = rs.getString("apellido");

	            if (nombre == null) {
	                nombre = "";
	            }

	            if (apellido == null || apellido.trim().isEmpty()) {

	                nombre = nombre.trim();

	                int ultimoEspacio = nombre.lastIndexOf(" ");

	                if (ultimoEspacio > 0) {

	                    apellido = nombre.substring(ultimoEspacio + 1).trim();
	                    nombre = nombre.substring(0, ultimoEspacio).trim();

	                } else {

	                    apellido = "N/A";
	                }
	            }

	            orden.setFirstName(nombre);
	            orden.setLastName(apellido);

	            String telefono = rs.getString("telefono_celular");
	            if (telefono == null || telefono.trim().isEmpty()) {
	                telefono = rs.getString("telefono");
	            }

	            if (telefono == null) {
	                telefono = "";
	            }

	            orden.setPhone(telefono.trim());
	            orden.setPhoneCountryCode("57");

	            String email = rs.getString("email");
	            if (email == null ) {
	                email = "";
	            }

	            orden.setEmail(email);

	            orden.setAddress(rs.getString("direccion"));
	            orden.setLat(rs.getDouble("latitud"));
	            orden.setLng(rs.getDouble("longitud"));

	            orden.setComplement("");

	            String ciudad = rs.getString("municipio");
	            if (ciudad == null) {
	                ciudad = "";
	            }

	            orden.setCity(ciudad);

	            String comentarios = rs.getString("observacion");
	            if (comentarios == null) {
	                comentarios = "";
	            }

	            orden.setComments(comentarios);

	            ArrayList<DetallePedido> detalles = PedidoDAO.ConsultarDetallePedido(idPedido);

	            ArrayList<RappiCargoProducto> productos = new ArrayList<RappiCargoProducto>();

	            for (DetallePedido detalle : detalles) {

	                RappiCargoProducto producto = new RappiCargoOrden.RappiCargoProducto();

	                producto.setProductId(String.valueOf(detalle.getIdproducto()));
	                producto.setProductName(detalle.getNombreproducto());

	                StringBuilder descripcion = new StringBuilder();

	                if (detalle.getNombreespecialidad1() != null)
	                    descripcion.append(detalle.getNombreespecialidad1()).append(" ");

	                if (detalle.getNombreespecialidad2() != null)
	                    descripcion.append(detalle.getNombreespecialidad2()).append(" ");

	                if (detalle.getAdicion() != null)
	                    descripcion.append(detalle.getAdicion()).append(" ");

	                if (detalle.getObservacion() != null)
	                    descripcion.append(detalle.getObservacion());

	                producto.setProductDescription(descripcion.toString().trim());
	                producto.setUnits((int) detalle.getCantidad());
	                producto.setUnitPrice((int) detalle.getValorunitario());

	                productos.add(producto);
	            }

	            orden.setProducts(productos);
	        }

	    } catch (Exception e) {

	        e.printStackTrace();

	    } finally {

	        try {
	            if (rs != null)
	                rs.close();
	        } catch (Exception e) {
	        }

	        try {
	            if (ps != null)
	                ps.close();
	        } catch (Exception e) {
	        }

	        try {
	            if (con1 != null)
	                con1.close();
	        } catch (Exception e) {
	        }
	    }

	    return orden;
	}
    public static boolean guardarEvento(
            long idPedido,
            String proveedor,
            String idPedidoProveedor,
            String idPedidoLogistico,
            String estado,
            String urlSeguimiento,
            String fechaEntrega,
            String fechaCancelacion,
            String jsonEvento,
            String resultadoProceso) {

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
                  + "resultado_proceso"
                  + ") VALUES (?,?,?,?,?,?,?,?,?,?)";

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
            
            if (resultadoProceso == null || resultadoProceso.trim().isEmpty()) {
                ps.setNull(10, Types.VARCHAR);
            } else {
                ps.setString(10, resultadoProceso);
            }
           

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
    
    
    public static boolean actualizarResultadoProcesoPorPedido(
            long idPedido,
            boolean procesado,
            String mensaje) {

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDPrincipal();
        PreparedStatement ps = null;

        try {

            String sql =
                    "UPDATE tercerizado_domicilio_evento " +
                    "SET procesado = ?, " +
                    "resultado_proceso = ? " +
                    "WHERE id_pedido = ?";

            ps = con1.prepareStatement(sql);

            ps.setInt(1, procesado ? 1 : 0);
            ps.setString(2, mensaje);
            ps.setLong(3, idPedido);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;

        } finally {

            try {
                if (ps != null)
                    ps.close();
            } catch (Exception ignored) {
            }

            try {
                if (con1 != null)
                    con1.close();
            } catch (Exception ignored) {
            }
        }
    }

    
    
    public static boolean actualizarPedidoRappiCargo(
            int idpedido,
            BigInteger cargoOrderId) {

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDPrincipal();
        boolean respuesta = false;

        try {

            Statement stm = con1.createStatement();

            String update =
                    "UPDATE pedido " +
                    "SET domicilio_tercerizado='S', " +
                    "idordencomercio=" + cargoOrderId +
                    " WHERE idpedido=" + idpedido;

            respuesta = stm.executeUpdate(update) > 0;

            stm.close();
            con1.close();

        } catch (Exception e) {

            System.out.println(e.toString());

            try {
                con1.close();
            } catch (Exception ex) {
            }
        }

        return respuesta;
    }
}
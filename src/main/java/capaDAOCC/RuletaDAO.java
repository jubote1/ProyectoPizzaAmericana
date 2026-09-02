package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import capaConexionPOS.ConexionBaseDatos;
import capaModeloCC.EncuestaServicio;
import capaSeguridad.TokenRoulette;

public class RuletaDAO {
	
	

    
    public static List<JSONObject> ListaOpcionesRuleta() {
    	
    	List<JSONObject> listaOpciones = new ArrayList<>();
    	try {
            ConexionBaseDatos con = new ConexionBaseDatos();
            Connection con1 = con.obtenerConexionBDPrincipal(); 
            
            
            String sql = "SELECT * FROM opciones_ruleta where activo = 1";

            try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("idopcion", rs.getInt("idopcion"));
                        obj.put("premio", rs.getInt("premio"));
                        obj.put("titulo", rs.getString("titulo"));
                        obj.put("descripcion", rs.getString("descripcion"));
                        obj.put("valor", rs.getString("valor"));
                        obj.put("idtipo", rs.getInt("idtipo"));
                        obj.put("indice", rs.getInt("indice"));
                        obj.put("repeticiones", rs.getInt("repeticiones"));
                        obj.put("reintento", rs.getInt("reintento"));
                                            
                        int repeticiones = rs.getInt("repeticiones");

                        for(int i = 0; i < repeticiones; i++){
                            listaOpciones.add(obj);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

    	}catch(Exception e) {
    		System.out.println("Error al obtener opciones de la ruleta: "+e.getMessage());
    		e.printStackTrace();
    	}

        return listaOpciones;
    }

    
    public static int registrarResultadoRuletaConToken(EncuestaServicio encuesta, int idopcion, String token) {
        String sqlInsert = "INSERT INTO resultado_ruleta (idpedido, idopcion, token, correo, idtienda, telefono, nombre_cliente) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE resultado_ruleta SET token = ? WHERE idresultado = ?";
        
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;
        int idGenerado = 0;

        try {
            con1 = con.obtenerConexionBDPrincipal();
            con1.setAutoCommit(false); // 🔒 Inicia transacción manual

            // 1️⃣ INSERTAR registro
            try (PreparedStatement pstmt = con1.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, encuesta.getIdpedido());
                pstmt.setInt(2, idopcion);
                pstmt.setString(3, token); // puede ir null inicialmente
                pstmt.setString(4, encuesta.getCorreo());
                pstmt.setInt(5, encuesta.getIdtienda());
                pstmt.setString(6, encuesta.getTelefono());
                pstmt.setString(7, encuesta.getNombre_cliente());
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) idGenerado = rs.getInt(1);
                }
            }

            // 2️⃣ Generar token después de tener el ID
            String nuevoToken = TokenRoulette.generateToken(idopcion, encuesta.getIdpedido(), idGenerado);

            // 3️⃣ Actualizar token en el mismo contexto
            try (PreparedStatement pstmt = con1.prepareStatement(sqlUpdate)) {
                pstmt.setString(1, nuevoToken);
                pstmt.setInt(2, idGenerado);
                pstmt.executeUpdate();
            }

            con1.commit(); // ✅ Todo OK → confirmar transacción
            return idGenerado;

        } catch (Exception e) {
            e.printStackTrace();
            try { if (con1 != null) con1.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } // ❌ Revertir todo
            return 0;
        } finally {
            try { if (con1 != null) con1.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    




    /**
     * Premios ganados en un rango de fechas, con todo lo que la pantalla de
     * dispersion necesita: el premio, los datos del cliente, la oferta que le
     * corresponde, y si ya se disperso, si ya se le aviso y si ya redimio.
     *
     * De donde sale el dato del cliente. Hay dos origenes de encuesta y se
     * comportan al contrario de lo que parece:
     *
     *  - Encuesta de tienda: el cliente escribe sus datos en el formulario, asi
     *    que el correo viene en resultado_ruleta. Pero casi nunca cruza con un
     *    pedido del central (4 de 139 medidos).
     *  - Encuesta directa (canales no fisicos): el formulario llega con el numero
     *    de pedido y el cliente no escribe correo, queda el texto
     *    'ENCUESTA-DIRECTA'. Estos si cruzan con el central (100 de 101), y de ahi
     *    se recupera el correo real.
     *
     * Juntando los dos origenes se pasa de 139 a 219 ganadores alcanzables por
     * correo, de 240.
     *
     * Tres cuidados para no multiplicar filas, que aqui seria grave porque cada
     * fila duplicada es un premio dispersado dos veces:
     *
     *  1. log_encuesta_servicio tiene 586 claves (idtienda, numero_pedido)
     *     repetidas. Se consulta con subconsultas escalares, no con join.
     *  2. (idtienda, numposheader) NO es unico en pedido: hay 23 combinaciones
     *     repetidas, una hasta 67 veces. Por eso el pedido se resuelve con una
     *     subconsulta que elige uno solo, acotada a los 15 dias anteriores a la
     *     jugada, y el join se hace contra idpedido, que es la llave primaria.
     *  3. cliente se une por idcliente, su llave primaria. Unirlo por correo
     *     seria un error: hay 30.300 correos repetidos en esa tabla, uno de ellos
     *     37.974 veces.
     *
     * @param fechaDesde formato yyyy-MM-dd, inclusive
     * @param fechaHasta formato yyyy-MM-dd, inclusive (el dia completo)
     */
    public static List<JSONObject> obtenerPremiosParaDispersion(String fechaDesde, String fechaHasta) {

        List<JSONObject> premios = new ArrayList<>();

        String sql = "SELECT r.idresultado, DATE_FORMAT(r.fecha,'%Y-%m-%d %H:%i') AS fecha,"
                + " r.idtienda, IFNULL(t.nombre,'') AS tienda, r.idpedido,"
                + " r.idopcion, o.titulo AS premio,"
                // El correo propio manda; si no tiene forma de correo, se usa el del
                // cliente del pedido central.
                + " IFNULL(COALESCE(CASE WHEN r.correo LIKE '%@%' THEN TRIM(r.correo) END,"
                + "   CASE WHEN c.email LIKE '%@%' THEN TRIM(c.email) END),'') AS correo,"
                + " IFNULL(COALESCE(NULLIF(TRIM(r.nombre_cliente),''),"
                + "   (SELECT MAX(l.nombre_cliente) FROM log_encuesta_servicio l"
                + "     WHERE l.numero_pedido = r.idpedido AND l.idtienda = r.idtienda),"
                + "   NULLIF(TRIM(CONCAT(IFNULL(c.nombre,''),' ',IFNULL(c.apellido,''))),'')),'') AS nombre_cliente,"
                + " IFNULL(COALESCE(NULLIF(TRIM(r.telefono),''),"
                + "   (SELECT MAX(l.telefono) FROM log_encuesta_servicio l"
                + "     WHERE l.numero_pedido = r.idpedido AND l.idtienda = r.idtienda),"
                + "   NULLIF(TRIM(c.telefono_celular),''), NULLIF(TRIM(c.telefono),'')),'') AS telefono,"
                + " IFNULL(p.idcliente,0) AS idcliente, IFNULL(p.idpedido,0) AS idpedido_central,"
                + " IFNULL(p.origen,'') AS origen,"
                + " IFNULL(ro.idoferta,0) AS idoferta, IFNULL(f.nombre_oferta,'') AS nombre_oferta,"
                + " IFNULL(f.dias_caducidad,0) AS dias_caducidad,"
                + " r.idofertacliente,"
                + " IFNULL(DATE_FORMAT(r.fecha_dispersion,'%Y-%m-%d %H:%i'),'') AS fecha_dispersion,"
                + " IFNULL(r.usuario_dispersion,'') AS usuario_dispersion,"
                + " IFNULL(oc.codigo_promocion,'') AS codigo_promocion,"
                + " IFNULL(DATE_FORMAT(oc.fecha_caducidad,'%Y-%m-%d'),'') AS fecha_caducidad,"
                + " IFNULL(DATE_FORMAT(oc.fecha_mensaje,'%Y-%m-%d %H:%i'),'') AS fecha_aviso,"
                + " IFNULL(oc.utilizada,'') AS utilizada,"
                + " IFNULL(DATE_FORMAT(oc.uso_oferta,'%Y-%m-%d %H:%i'),'') AS uso_oferta,"
                + " IFNULL(oc.usuario_uso,'') AS usuario_uso"
                + " FROM resultado_ruleta r"
                + " JOIN opciones_ruleta o ON o.idopcion = r.idopcion AND o.premio = 1"
                + " LEFT JOIN tienda t ON t.idtienda = r.idtienda"
                + " LEFT JOIN ruleta_oferta ro ON ro.idopcion = r.idopcion AND ro.activo = 'S'"
                + " LEFT JOIN oferta f ON f.idoferta = ro.idoferta"
                + " LEFT JOIN oferta_cliente oc ON oc.idofertacliente = r.idofertacliente"
                // Se elige UN pedido y se une por llave primaria, para que la fila
                // no se pueda multiplicar. Usa el indice numposheader.
                + " LEFT JOIN pedido p ON p.idpedido = ("
                + "   SELECT p2.idpedido FROM pedido p2"
                + "    WHERE p2.numposheader = r.idpedido AND p2.idtienda = r.idtienda"
                + "      AND p2.fechapedido BETWEEN DATE_SUB(DATE(r.fecha), INTERVAL 15 DAY) AND DATE(r.fecha)"
                + "    ORDER BY p2.fechapedido DESC, p2.idpedido DESC LIMIT 1)"
                + " LEFT JOIN cliente c ON c.idcliente = p.idcliente"
                + " WHERE r.fecha >= ? AND r.fecha < DATE_ADD(?, INTERVAL 1 DAY)"
                + " ORDER BY r.fecha, r.idresultado";

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;
        try {
            con1 = con.obtenerConexionBDPrincipal();
            try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
                pstmt.setString(1, fechaDesde);
                pstmt.setString(2, fechaHasta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("idresultado", rs.getInt("idresultado"));
                        obj.put("fecha", rs.getString("fecha"));
                        obj.put("idtienda", rs.getInt("idtienda"));
                        obj.put("tienda", rs.getString("tienda"));
                        obj.put("idpedido", rs.getInt("idpedido"));
                        obj.put("idpedido_central", rs.getInt("idpedido_central"));
                        obj.put("origen", rs.getString("origen"));
                        obj.put("idopcion", rs.getInt("idopcion"));
                        obj.put("premio", rs.getString("premio"));
                        obj.put("correo", rs.getString("correo"));
                        obj.put("nombre_cliente", rs.getString("nombre_cliente"));
                        obj.put("telefono", rs.getString("telefono"));
                        obj.put("idcliente", rs.getInt("idcliente"));
                        obj.put("idoferta", rs.getInt("idoferta"));
                        obj.put("nombre_oferta", rs.getString("nombre_oferta"));
                        obj.put("dias_caducidad", rs.getInt("dias_caducidad"));
                        obj.put("idofertacliente", rs.getInt("idofertacliente"));
                        obj.put("fecha_dispersion", rs.getString("fecha_dispersion"));
                        obj.put("usuario_dispersion", rs.getString("usuario_dispersion"));
                        obj.put("codigo_promocion", rs.getString("codigo_promocion"));
                        obj.put("fecha_caducidad", rs.getString("fecha_caducidad"));
                        obj.put("fecha_aviso", rs.getString("fecha_aviso"));
                        obj.put("utilizada", rs.getString("utilizada"));
                        obj.put("uso_oferta", rs.getString("uso_oferta"));
                        obj.put("usuario_uso", rs.getString("usuario_uso"));
                        obj.put("estado", calcularEstado(rs.getInt("idofertacliente"),
                                rs.getInt("idoferta"), rs.getString("correo")));
                        premios.add(obj);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error al obtener premios para dispersion: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (con1 != null) { con1.close(); } } catch (Exception e1) { }
        }
        return premios;
    }

    /**
     * Estado de un premio, para que la pantalla sepa que puede hacer con el.
     * Se calcula aqui y no en la pantalla para que la lista y el boton de
     * dispersar nunca discrepen sobre que es dispersable.
     */
    private static String calcularEstado(int idOfertaCliente, int idOferta, String correo) {
        // Negativo es la marca de los premios entregados a mano antes de que
        // existiera esta pantalla. Se distinguen de los que disperso el sistema
        // porque de esos no tenemos codigo ni fecha de aviso: mostrarlos como
        // DISPERSADO haria creer que la informacion esta, y esas columnas estarian
        // vacias sin explicacion.
        if (idOfertaCliente < 0) {
            return "ENTREGADO A MANO";
        }
        if (idOfertaCliente > 0) {
            return "DISPERSADO";
        }
        if (idOferta == 0) {
            return "SIN OFERTA";
        }
        if (!correoValido(correo)) {
            return "SIN CORREO";
        }
        return "PENDIENTE";
    }

    /**
     * El correo lo escribe el cliente a mano en el formulario de la ruleta, asi
     * que llegan errores de dedo, y cuando no lo escribe queda el texto
     * 'ENCUESTA-DIRECTA'. Se valida antes de dispersar para que esos no salgan a
     * rebotar: cada rebote golpea la reputacion de la cuenta de correo.
     * No pretende verificar que el buzon exista, solo descartar lo que ni siquiera
     * tiene forma de correo.
     */
    public static boolean correoValido(String correo) {
        if (correo == null) {
            return false;
        }
        String c = correo.trim();
        if (c.length() < 6 || c.length() > 200) {
            return false;
        }
        if (c.indexOf(' ') >= 0) {
            return false;
        }
        int arroba = c.indexOf('@');
        if (arroba <= 0 || arroba != c.lastIndexOf('@')) {
            return false;
        }
        String dominio = c.substring(arroba + 1);
        int punto = dominio.indexOf('.');
        return (punto > 0 && punto < dominio.length() - 1);
    }

    /**
     * Deja constancia de que un resultado de ruleta ya fue dispersado, guardando
     * a que oferta del cliente quedo amarrado, cuando y quien lo hizo.
     *
     * El idofertacliente cumple dos papeles a la vez: es el enlace para consultar
     * el codigo, el aviso y la redencion, y es la marca de dispersado. Al ser la
     * misma columna es imposible que las dos cosas queden en desacuerdo, que es
     * lo que pasaria con una bandera aparte.
     *
     * La condicion "AND idofertacliente = 0" es la que evita dispersar dos veces:
     * si dos personas aprietan el boton al mismo tiempo, la segunda actualizacion
     * no toca ninguna fila y el metodo devuelve false, asi que el llamador sabe
     * que no debe enviar el correo.
     *
     * @param idResultado     resultado de ruleta a marcar
     * @param idOfertaCliente oferta que se le asigno al cliente
     * @param usuario         quien ejecuto la dispersion
     * @return true si esta llamada fue la que lo marco
     */
    public static boolean marcarDispersion(final int idResultado, final int idOfertaCliente,
            final String usuario) {

        final String sql = "UPDATE resultado_ruleta SET idofertacliente = ?, fecha_dispersion = NOW(),"
                + " usuario_dispersion = ? WHERE idresultado = ? AND idofertacliente = 0";

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;
        try {
            con1 = con.obtenerConexionBDPrincipal();
            try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
                pstmt.setInt(1, idOfertaCliente);
                pstmt.setString(2, usuario == null ? "" : usuario);
                pstmt.setInt(3, idResultado);
                return (pstmt.executeUpdate() == 1);
            }
        } catch (final Exception e) {
            System.out.println("Error al marcar dispersion del resultado " + idResultado + ": " + e.getMessage());
            e.printStackTrace();
            return (false);
        } finally {
            try { if (con1 != null) { con1.close(); } } catch (final Exception e1) { }
        }
    }

    /**
     * Busca el cliente activo que tenga ese telefono.
     *
     * Se elige el de idcliente mas alto, es decir el mas reciente, porque un mismo
     * telefono puede tener varios clientes y hay que quedarse con uno de forma
     * determinista: si se dejara al azar, dos dispersiones del mismo premio
     * podrian amarrar la oferta a clientes distintos.
     *
     * Se busca por telefono y no por correo a proposito: en cliente hay 30.300
     * correos repetidos, uno de ellos 37.974 veces (integration@rappi.com), asi
     * que el correo no identifica a nadie.
     *
     * @param telefono telefono a buscar
     * @return el idcliente, o 0 si no existe
     */
    public static int buscarClientePorTelefono(final String telefono) {

        if (telefono == null || telefono.trim().length() < 7) {
            return (0);
        }

        final String sql = "SELECT MAX(idcliente) AS idcliente FROM cliente"
                + " WHERE telefono = ? AND activo = 1";

        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;
        try {
            con1 = con.obtenerConexionBDPrincipal();
            try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
                pstmt.setString(1, telefono.trim());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return (rs.getInt("idcliente"));
                    }
                }
            }
        } catch (final Exception e) {
            System.out.println("Error al buscar cliente por telefono: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (con1 != null) { con1.close(); } } catch (final Exception e1) { }
        }
        return (0);
    }
}

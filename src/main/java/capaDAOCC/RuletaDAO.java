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

    



}

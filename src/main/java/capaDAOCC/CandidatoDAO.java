package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

import capaModeloCC.Candidato;
import capaModeloCC.ExperienciaCandidato;
import capaModeloCC.FormacionCandidato;
import conexionCC.ConexionBaseDatos;

public class CandidatoDAO {

	
	
	public static JSONObject  insertarCandidato(Candidato candidato) {
		JSONObject datos = new JSONObject();
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDNominaAmericana();
	    int candidatoId = 0;
	    boolean respuesta = false;
	    String mensaje = "";
	    ResultSet rs = null;

	    String insertCandidatoSQL = "INSERT INTO candidato (idvacante, nombre_completo, barrio, cedula, genero, celular, email, municipio, ciudad, fecha_nacimiento, fuente_vacante, hoja_vida, transporte, documentacion_al_dia) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	    try (
	        PreparedStatement candidatoStmt = con1.prepareStatement(insertCandidatoSQL, PreparedStatement.RETURN_GENERATED_KEYS);
	    ) {
	        con1.setAutoCommit(false); // Iniciar transacción

	        // 1. Insertar el candidato en la tabla "candidato"
	        candidatoStmt.setInt(1, candidato.getIdvacante());
	        candidatoStmt.setString(2, candidato.getNombre());
	        candidatoStmt.setString(3, candidato.getBarrio());
	        candidatoStmt.setString(4, candidato.getCedula());
	        candidatoStmt.setString(5, candidato.getGenero());
	        candidatoStmt.setString(6, candidato.getCelular());
	        candidatoStmt.setString(7, candidato.getEmail());
	        candidatoStmt.setString(8, candidato.getMunicipio());
	        candidatoStmt.setString(9, candidato.getCiudad());
	        candidatoStmt.setString(10, candidato.getFechaNacimiento());
	        candidatoStmt.setString(11, candidato.getFuenteVacante());
	        candidatoStmt.setString(12, candidato.getHojaVida());
	        candidatoStmt.setBoolean(13, candidato.isTransporte());
	        candidatoStmt.setBoolean(14, candidato.isDocumentacionTransporte());
	   

	        int affectedRows = candidatoStmt.executeUpdate();
	        if (affectedRows > 0) {
	            rs = candidatoStmt.getGeneratedKeys();
	            if (rs.next()) {
	                candidatoId = rs.getInt(1);
	            }
	        }

	        // Si no se generó un ID, retorna 0
	        if (candidatoId == 0) {
	        	mensaje="No se pudo insertar el candidato";
	        	con1.rollback(); // Deshacer la transacción
	        
	            datos.put("respuesta", respuesta);
	    	    datos.put("mensaje", mensaje);

	            return datos;
	        }

	        
	        if (!candidato.getFormacion().isEmpty()) {
	        // 2. Insertar formaciones en la tabla "formacion_candidato"
	        String insertFormacionSQL = "INSERT INTO formacion_candidato (idcandidato, nombre_institucion, programa_academico, nivel, estado, modalidad, jornada, periodicidad_academica, periodo_actual) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        try (PreparedStatement formacionStmt = con1.prepareStatement(insertFormacionSQL)) {
	            for (FormacionCandidato formacion : candidato.getFormacion()) {
	                formacionStmt.setInt(1, candidatoId);
	                formacionStmt.setString(2, formacion.getInstitucion());
	                formacionStmt.setString(3, formacion.getPrograma());
	                formacionStmt.setString(4, formacion.getNivel());
	                formacionStmt.setString(5, formacion.getEstado());
	                formacionStmt.setString(6, formacion.getModalidad());
	                formacionStmt.setString(7, formacion.getJornada());
	                formacionStmt.setString(8, formacion.getPeriodicidadAcademica());
	                formacionStmt.setString(9, formacion.getPeriodoActual());
	                formacionStmt.addBatch();
	            }
	            formacionStmt.executeBatch();
	        }
	        }


	        if (!candidato.getExperiencia().isEmpty()) {
	        String insertExperienciaSQL = "INSERT INTO experiencia_candidato (idcandidato, empresa, cargo, ciudad, descripcion, fecha_inicio, fecha_final, trabajo_actual) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	        try (PreparedStatement experienciaStmt = con1.prepareStatement(insertExperienciaSQL)) {
	            for (ExperienciaCandidato experiencia : candidato.getExperiencia()) {
	                experienciaStmt.setInt(1, candidatoId);
	                experienciaStmt.setString(2, experiencia.getNombreEmpresa());
	                experienciaStmt.setString(3, experiencia.getCargo());
	                experienciaStmt.setString(4, experiencia.getCiudad());
	                experienciaStmt.setString(5, experiencia.getDescripcion());
	                experienciaStmt.setString(6, experiencia.getFechaInicio());
	                experienciaStmt.setString(7, experiencia.getFechaFinalizacion());
	                experienciaStmt.setBoolean(8, experiencia.isTrabajoActual());
	                experienciaStmt.addBatch();
	            }
	            experienciaStmt.executeBatch();
	        }
	        
	        }

	        // 4. Confirmar la transacción
	        con1.commit();
	        respuesta= true;
	        mensaje="Exito";

	    } catch (SQLException e) {
	        if (con1 != null) {
	            try {
	                con1.rollback(); // Revertir la transacción en caso de error
	            } catch (SQLException rollbackEx) {
	            	System.out.println(rollbackEx); 
	            	mensaje =rollbackEx.getMessage();
	            }
	        }
	    	System.out.println(e);
	    	mensaje =e.getMessage();
	    	
	    } finally {
	        try {
	            if (rs != null) rs.close();
	            if (con1 != null) con1.close();
	        } catch (SQLException e) {
	        	
	        	System.out.println(e);
	        	mensaje = e.getMessage();
	        }
	    }
	    
	    datos.put("respuesta", respuesta);
	    datos.put("mensaje", mensaje);

	    return datos;
	}

}

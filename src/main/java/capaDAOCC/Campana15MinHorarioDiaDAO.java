package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Horario de la campana 15 minutos, una fila por dia de la semana (1=lunes..
 * 7=domingo). Cada dia puede estar inactivo, activo todo el dia, o activo
 * solo dentro de una franja horaria -- el horario real de la campana no es
 * uniforme entre semana y fin de semana.
 */
public class Campana15MinHorarioDiaDAO {

	public static class HorarioDia {
		public int idCampana = 0;

		public int diaSemana = 1;

		public boolean activo = true;

		public boolean todoElDia = true;

		public String horaDesde = "";

		public String horaHasta = "";
	}

	public static ArrayList<HorarioDia> obtenerPorCampana(int idCampana) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<HorarioDia> lista = new ArrayList<>();
		try {
			PreparedStatement pst = con1.prepareStatement(
					"select * from campana_15min_horario_dia where idcampana = ? order by dia_semana");
			pst.setInt(1, idCampana);
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				HorarioDia h = new HorarioDia();
				h.idCampana = rs.getInt("idcampana");
				h.diaSemana = rs.getInt("dia_semana");
				h.activo = "S".equals(rs.getString("activo"));
				h.todoElDia = "S".equals(rs.getString("todo_el_dia"));
				h.horaDesde = rs.getString("hora_desde") == null ? "" : rs.getString("hora_desde");
				h.horaHasta = rs.getString("hora_hasta") == null ? "" : rs.getString("hora_hasta");
				lista.add(h);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerPorCampana: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (lista);
	}

	/** Reemplaza las 7 filas de una campana en una sola operacion. */
	public static String guardarSemana(int idCampana, ArrayList<HorarioDia> dias) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try {
			PreparedStatement pst = con1.prepareStatement("insert into campana_15min_horario_dia"
					+ " (idcampana, dia_semana, activo, todo_el_dia, hora_desde, hora_hasta) values (?,?,?,?,?,?)"
					+ " on duplicate key update activo = values(activo), todo_el_dia = values(todo_el_dia),"
					+ " hora_desde = values(hora_desde), hora_hasta = values(hora_hasta)");
			for (HorarioDia h : dias) {
				pst.setInt(1, idCampana);
				pst.setInt(2, h.diaSemana);
				pst.setString(3, h.activo ? "S" : "N");
				pst.setString(4, h.todoElDia ? "S" : "N");
				if (h.todoElDia || h.horaDesde == null || h.horaDesde.trim().equals("")) {
					pst.setNull(5, java.sql.Types.TIME);
				} else {
					pst.setString(5, h.horaDesde);
				}
				if (h.todoElDia || h.horaHasta == null || h.horaHasta.trim().equals("")) {
					pst.setNull(6, java.sql.Types.TIME);
				} else {
					pst.setString(6, h.horaHasta);
				}
				pst.addBatch();
			}
			pst.executeBatch();
			pst.close();
			con1.close();
			resultado = "exitoso";
		} catch (Exception e) {
			logger.error("guardarSemana: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			resultado = "error";
		}
		return (resultado);
	}
}

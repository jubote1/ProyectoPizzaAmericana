package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.FormaPago;
import capaModeloCC.HorarioDia;
import conexionCC.ConexionBaseDatos;

public class HorarioDiaDAO {
	
	public static ArrayList<HorarioDia> obtenerHorariosDia()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<HorarioDia> horarios = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from horario_dia";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idDia;
			String nombre;
			String horaApertura;
			String horaCierre;
			while(rs.next()){
				idDia = rs.getInt("iddia");
				nombre = rs.getString("nombre");
				horaApertura = rs.getString("hora_apertura");
				horaCierre = rs.getString("hora_cierre");
				HorarioDia horarioTemp = new HorarioDia(idDia, nombre, horaApertura, horaCierre);
				horarios.add(horarioTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(horarios);
		
	}

}

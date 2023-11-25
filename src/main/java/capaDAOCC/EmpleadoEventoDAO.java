package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.HorarioEmpleado;
import capaConexionPOS.ConexionBaseDatos;
import capaModeloPOS.Egreso;
import capaModeloPOS.EmpleadoEncuesta;

public class EmpleadoEventoDAO {
		
	public static ArrayList<HorarioEmpleado> obtenerHorariosAdministradoresDia()
	{
		ArrayList<HorarioEmpleado> respuestas = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneralLocal();
		String select  = "SELECT a.id, b.nombre_largo, a.tipo_evento, a.fecha_hora_log, c.nombre as tienda FROM general.empleado_evento a, general.empleado b, pizzaamericana.tienda c WHERE a.id = b.id AND a.idtienda = c.idtienda AND b.administrador = 'S' AND a.fecha = CURDATE()";
		Statement stm;
		HorarioEmpleado temp;
		try
		{
			stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(select);
			int id;
			String nombre;
			String tipoEvento;
			String fechaHora;
			String tienda;
			while(rs.next()){
				id = rs.getInt("id");
				nombre = rs.getString("nombre_largo");
				tipoEvento = rs.getString("tipo_evento");
				fechaHora = rs.getString("fecha_hora_log");
				tienda = rs.getString("tienda");
				temp = new HorarioEmpleado(id,nombre, tipoEvento, fechaHora,tienda);
				respuestas.add(temp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
			System.out.println(e.toString());
			try
			{
				con1.close();
				
			}catch(Exception e1)
			{
				
			}
		}
		return(respuestas);
	}
	
}

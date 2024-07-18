	package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.EstadisticaPromocion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.Promocion;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class EstadisticaPromocionDAO {
	
	/**
	 * Método que se encarga de retornar la información de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<EstadisticaPromocion> obtenerEstadisticasPromocion(String fecha)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<EstadisticaPromocion> estadisticas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDDatamartLocal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from estadistica_promocion where fecha = '" + fecha + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idTienda = 0;
			int idPromocion = 0;
			int contact = 0;
			int tiendaVirtual = 0;
			int total = 0;
			EstadisticaPromocion est;
			while(rs.next()){
				idTienda = rs.getInt("idtienda");
				idPromocion = rs.getInt("idpromocion");
				contact = rs.getInt("contact");
				tiendaVirtual = rs.getInt("tienda_virtual");
				total = rs.getInt("total");
				est = new EstadisticaPromocion(fecha,idTienda,idPromocion,contact,tiendaVirtual,total,"");
				estadisticas.add(est);
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
		return(estadisticas);
		
	}
	
	public static ArrayList<EstadisticaPromocion> obtenerEstadisticasPromocion(String fechaInicial, String fechaFinal, boolean consolFecha, boolean consolTienda, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<EstadisticaPromocion> estadisticas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDDatamartLocal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "";
			if(consolFecha & consolTienda)
			{
				consulta = "select sum(a.tienda_virtual) as tienda_virtual, sum(a.contact) as contact , sum(a.total) as total, b.descripcion from estadistica_promocion a, promocion b where a.idpromocion = b.idpromocion and a.fecha >= '" + fechaInicial + "' and a.fecha <= '" + fechaFinal + "' group by b.descripcion";
			}else if(consolFecha & !consolTienda)
			{
				if(idTienda == 0)
				{
					consulta = "select sum(a.tienda_virtual) as tienda_virtual, sum(a.contact) as contact , sum(a.total) as total, a.idtienda, b.descripcion from estadistica_promocion a, promocion b where a.idpromocion = b.idpromocion and a.fecha >= '" + fechaInicial + "' and a.fecha <= '" + fechaFinal + "' group by a.idtienda, b.descripcion";	
				}else
				{
					consulta = "select sum(a.tienda_virtual) as tienda_virtual, sum(a.contact) as contact , sum(a.total) as total, a.idtienda, b.descripcion from estadistica_promocion a, promocion b where a.idpromocion = b.idpromocion and a.fecha >= '" + fechaInicial + "' and a.fecha <= '" + fechaFinal + "' and a.idtienda = " + idTienda + " group by a.idtienda, b.descripcion";
				}
			}if(!consolFecha & consolTienda)
			{
				consulta = "select sum(a.tienda_virtual) as tienda_virtual, sum(a.contact) as contact , sum(a.total) as total, a.fecha, b.descripcion from estadistica_promocion a, promocion b where a.idpromocion = b.idpromocion and a.fecha >= '" + fechaInicial + "' and a.fecha <= '" + fechaFinal + "' group by a.fecha, b.descripcion";
			}if(!consolFecha & !consolTienda)
			{
				if(idTienda == 0)
				{
					consulta = "select sum(a.tienda_virtual) as tienda_virtual, sum(a.contact) as contact , sum(a.total) as total, a.fecha, a.idtienda, b.descripcion from estadistica_promocion a, promocion b where a.idpromocion = b.idpromocion and a.fecha >= '" + fechaInicial + "' and a.fecha <= '" + fechaFinal + "' group by a.fecha, a.idtienda, b.descripcion";
				}else
				{
					consulta = "select sum(a.tienda_virtual) as tienda_virtual, sum(a.contact) as contact , sum(a.total) as total, a.fecha, a.idtienda, b.descripcion from estadistica_promocion a, promocion b where a.idpromocion = b.idpromocion and a.fecha >= '" + fechaInicial + "' and a.fecha <= '" + fechaFinal + "' and a.idtienda = " + idTienda +  " group by a.fecha, a.idtienda, b.descripcion";
				}
			}
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idPromocion = 0;
			int contact = 0;
			int tiendaVirtual = 0;
			int total = 0;
			String fecha = "";
			String promocion = "";
			EstadisticaPromocion est;
			while(rs.next()){
				if(consolFecha & consolTienda)
				{
					idTienda = 0;
					idPromocion = 0;
					contact = rs.getInt("contact");
					tiendaVirtual = rs.getInt("tienda_virtual");
					total = rs.getInt("total");
					fecha = "";
					promocion = rs.getString("descripcion");
				}else if(consolFecha & !consolTienda)
				{
					if(idTienda == 0)
					{
						idTienda = rs.getInt("idtienda");
						idPromocion = 0;
						contact = rs.getInt("contact");
						tiendaVirtual = rs.getInt("tienda_virtual");
						total = rs.getInt("total");
						fecha = "";
						promocion = rs.getString("descripcion");
					}else
					{
						idTienda = rs.getInt("idtienda");
						idPromocion = 0;
						contact = rs.getInt("contact");
						tiendaVirtual = rs.getInt("tienda_virtual");
						total = rs.getInt("total");
						fecha = "";
						promocion = rs.getString("descripcion");
					}
				}if(!consolFecha & consolTienda)
				{
					idTienda = 0;
					idPromocion = 0;
					contact = rs.getInt("contact");
					tiendaVirtual = rs.getInt("tienda_virtual");
					total = rs.getInt("total");
					fecha = rs.getString("fecha");
					promocion = rs.getString("descripcion");
				}if(!consolFecha & !consolTienda)
				{
					if(idTienda == 0)
					{
						idTienda = rs.getInt("idtienda");
						idPromocion = 0;
						contact = rs.getInt("contact");
						tiendaVirtual = rs.getInt("tienda_virtual");
						total = rs.getInt("total");
						fecha = rs.getString("fecha");
						promocion = rs.getString("descripcion");
					}else
					{
						idTienda = rs.getInt("idtienda");
						idPromocion = 0;
						contact = rs.getInt("contact");
						tiendaVirtual = rs.getInt("tienda_virtual");
						total = rs.getInt("total");
						fecha = rs.getString("fecha");
						promocion = rs.getString("descripcion");
					}
				}
				est = new EstadisticaPromocion(fecha,idTienda,idPromocion,contact,tiendaVirtual,total, promocion);
				estadisticas.add(est);
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
		return(estadisticas);
		
	}
	
}

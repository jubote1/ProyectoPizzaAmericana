package capaDAOCC;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import capaModeloCC.DetallePedido;
import capaModeloCC.Especialidad;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Producto;
import capaModeloCC.ProductoNoExistente;
import capaModeloCC.SaborLiquido;
import capaModeloCC.TipoLiquido;
import conexionCC.ConexionBaseDatos;

/**
 * Clase que se encarga de implementar toda la interacción con la base de datos para le entidad Producto.
 * @author JuanDavid
 *
 */
public class ProductoNoExistenteDAO {
	

	public static ArrayList<ProductoNoExistente> retornarProductosNoExistentes()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ProductoNoExistente> productos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idtienda, a.idproducto, t.nombre tienda, p.nombre producto from producto_no_existente a, producto p,  tienda t where a.idproducto = p.idproducto and a.idtienda = t.idtienda";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idproducto = rs.getInt("idproducto");
				int idtienda = rs.getInt("idtienda");
				String tienda = rs.getString("tienda");
				String nombreproducto = rs.getString("producto");
				ProductoNoExistente producto = new ProductoNoExistente(idtienda, tienda, idproducto, nombreproducto);
				productos.add(producto);
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
		return(productos);
		
	}
	
	
	public static ArrayList<ProductoNoExistente> obtenerProductosNoExistentesGrid()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ProductoNoExistente> productos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idtienda, a.idproducto, t.nombre tienda, p.nombre producto from producto_no_existente a, producto p,  tienda t where a.idproducto = p.idproducto and a.idtienda = t.idtienda";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idproducto = rs.getInt("idproducto");
				int idtienda = rs.getInt("idtienda");
				String tienda = rs.getString("tienda");
				String nombreproducto = rs.getString("producto");
				ProductoNoExistente producto = new ProductoNoExistente(idtienda, tienda, idproducto, nombreproducto);
				productos.add(producto);
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
		return(productos);
		
	}
	

	public static void insertaProductoNoExistente(ProductoNoExistente pro)
	{
		Logger logger = Logger.getLogger("log_file");
		int idProductoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into producto_no_existente (idtienda, idproducto) values (" + pro.getIdtienda() + ", " + pro.getIdproducto() + ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idProductoIns = rs.getInt(1);
				
	        }
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		
	}

	
	public static void eliminarProductoNoExistente(int idtienda, int idproducto)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from producto_no_existente  where idproducto = " + idproducto + " and idtienda = " + idtienda; 
			logger.info(delete);
			stm.executeUpdate(delete);
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		
	}


	public static ProductoNoExistente retornarProductoNoExistente(int idtienda, int idproducto)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ProductoNoExistente Pro = new ProductoNoExistente(0, "",0, "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idtienda, a.idproducto , t.nombre tienda, p.nombre producto from producto_no_existente a, producto p, tienda t where a.idproducto = p.idproducto and a.idtienda = t.idtienda and a.idtienda =" + idtienda + " and a.idproducto = " + idproducto; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idproduc = 0;
			int idtien = 0;
			String tienda ="";
			String producto = "";
			while(rs.next()){
				idproduc = rs.getInt("idproducto");
				idtien  = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				producto = rs.getString("producto");
				break;
			}
			Pro = new ProductoNoExistente(idtien,tienda, idproduc,producto);
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(Pro);
	}

	
	public static String editarProductoNoExistente(ProductoNoExistente Pro)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String update = "update producto_no_existente set idtienda =" + Pro.getIdtienda() + ", idproducto = '" + Pro.getIdproducto() + "  where idproducto = " + Pro.getIdproducto() + " and idtienda =" + Pro.getIdtienda(); 
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
			stm.close();
			con1.close();
			
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			resultado = "error";
		}
		return(resultado);
	}

}

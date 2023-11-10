package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Marcacion;
import capaModeloCC.MarcacionPedido;
import capaModeloCC.Municipio;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class MarcacionPedidoDAO {
	
	public static void InsertarMarcacionPedido(MarcacionPedido mar)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into marcacion_pedido (idpedido,idmarcacion,observacion,descuento, motivo, marketplace,descuento_asumido,descuento_plataforma,tarifa_adicional,propina,log) values ( " + mar.getIdPedido() + " , " + mar.getIdMarcacion() + " , '" + mar.getObservacion() + "' , " + mar.getDescuento() + " , '" + mar.getMotivo() + "' , '" + mar.getMarketplace() + "' , '" + mar.getDescuentoAsumido() + "' ," + mar.getDescuentoPlataforma() + " , " + mar.getTarifaAdicional() + " , " + mar.getPropina() + ", '" + mar.getLog() + "')"; 
			System.out.println(insert);
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return;
		}
		return;
	}
	
	
	public static MarcacionPedido obtenerMarcacionPedido(int idPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		MarcacionPedido marPedido = new MarcacionPedido(0,0, "", 0, "", "","", 0.0,0.0,0.0,"");
		marPedido.setNombreMarcacion("");
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.idpedido, b.idmarcacion, b.observacion, c.nombre_marcacion, b.marketplace, b.descuento_plataforma,b.tarifa_adicional, b.propina, b.log  FROM pedido a, marcacion_pedido b, marcacion c WHERE a.idpedido = b.idpedido AND b.idmarcacion = c.idmarcacion and a.idpedido = " + idPedido; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			int idMarcacion;
			String observacion, nombreMarcacion;
			String marketplace = "";
			double descuentoPlataforma,tarifaAdicional,propina;
			String log;
			while(rs.next())
			{
				idMarcacion = rs.getInt("idmarcacion");
				observacion = rs.getString("observacion");
				nombreMarcacion = rs.getString("nombre_marcacion");
				marketplace = rs.getString("marketplace");
				descuentoPlataforma = rs.getDouble("descuento_plataforma");
				tarifaAdicional = rs.getDouble("tarifa_adicional");
				propina = rs.getDouble("propina");
				log = rs.getString("log");
				marPedido = new MarcacionPedido(idPedido, idMarcacion,observacion, 0, "", marketplace,"",descuentoPlataforma, tarifaAdicional, propina,log);
				marPedido.setNombreMarcacion(nombreMarcacion);
				break;
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
		return(marPedido);
	}

	
	
}

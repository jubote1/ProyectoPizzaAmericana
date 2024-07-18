package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.TipoLiquido;
import capaModeloCC.TipoPedido;
import conexionCC.ConexionBaseDatos;	

/**
 * Clase que se encarga de implementar toda la interacción con la base de datos para la entidad tipoLiquido
 * @author JuanDavid
 *
 */
public class TipoPedidoDAO {
	
/**
 * Método que retorna todos los tipos de pedido disponible para un pedido determinado
 * @return
 */
	public static ArrayList<TipoPedido> obtenerTiposPedido()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<TipoPedido> tipos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from tipo_pedido";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idTipoPedido = rs.getInt("idtipopedido");
				String nombre = rs.getString("nombre");
				String tipoPago = rs.getString("tipo_pago");
				String validaDir = rs.getString("valida_dir");
				TipoPedido tipPedido = new TipoPedido(idTipoPedido, nombre,tipoPago, validaDir);
				tipos.add(tipPedido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			System.out.println(e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(tipos);
		
	}


}

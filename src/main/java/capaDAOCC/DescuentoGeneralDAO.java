package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;
import capaModeloCC.DescuentoGeneral;
import conexionCC.ConexionBaseDatos;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class DescuentoGeneralDAO {
	
	/**
	 * Método que se encarga de retornar la información de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<DescuentoGeneral> obtenerDescuentosGenerales( String canal)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<DescuentoGeneral> descuentos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from descuentos_generales where canal = '" + canal + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idDescuento;
			int idProducto;
			String aplicaExcepcion;
			int valorPorcentaje;
			double valorPesos;
			String tipoDescuento;
			while(rs.next()){
				idDescuento = rs.getInt("iddescuento");
				idProducto = rs.getInt("idproducto");
				aplicaExcepcion = rs.getString("aplica_excepcion");
				valorPorcentaje = rs.getInt("valor_porcentaje");
				valorPesos = rs.getDouble("valor_pesos");
				tipoDescuento = rs.getString("tipo_descuento");
				DescuentoGeneral descuento = new DescuentoGeneral(idDescuento, idProducto, aplicaExcepcion, valorPorcentaje, valorPesos, tipoDescuento);
				descuentos.add(descuento);
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
		return(descuentos);
		
	}

}

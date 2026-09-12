package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * La gestion que se le hace a un link de pago que no se ha pagado.
 *
 * Guarda quien gestiono y cuando, desde la migracion
 * 2026_09_12_02_gestion_link_trazabilidad.sql. Antes la observacion quedaba
 * suelta -sin fecha y sin autor- y ademas nunca se leia desde ninguna pantalla:
 * se escribia y se perdia.
 */
public class PedidoGestionLinkDAO {

	public static boolean ingresarObsGestionLink(int idPedido, String observacion)
	{
		return (ingresarObsGestionLink(idPedido, observacion, ""));
	}

	public static boolean ingresarObsGestionLink(int idPedido, String observacion, String usuario)
	{
		boolean resultado = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		PreparedStatement pst = null;
		try
		{
			//Con parametros y no concatenando: la observacion la escribe una persona y
			//un apostrofo -"no contesto el cliente, esta en el 5'o piso"- tumbaba el
			//insert sin que nadie se enterara.
			pst = con1.prepareStatement(
					"insert into pedido_gestion_link (idpedido, observacion, usuario) values (?, ?, ?)");
			pst.setInt(1, idPedido);
			pst.setString(2, observacion == null ? "" : observacion);
			pst.setString(3, usuario == null ? "" : usuario);
			pst.executeUpdate();
			resultado = true;
		}
		catch (Exception e){
			logger.error("ingresarObsGestionLink(" + idPedido + "): " + e.toString());
		}
		finally
		{
			try
			{
				if(pst != null) { pst.close(); }
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(resultado);
	}
}

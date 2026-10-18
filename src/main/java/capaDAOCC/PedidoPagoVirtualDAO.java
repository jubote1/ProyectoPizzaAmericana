package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.PedidoPagoVirtual;
import conexionCC.ConexionBaseDatos;

public class PedidoPagoVirtualDAO {

	/**
	 * Cuantas veces ya se le mando a esta direccion el link de ESTE pedido.
	 *
	 * POR QUE EXISTE
	 *
	 * Una direccion que no existe rebota siempre, y el rebote llega minutos
	 * despues por correo: el codigo que envia nunca se entera, porque para el
	 * servidor SMTP el envio salio bien. Sin un conteo, nada impide mandar el
	 * mismo link a la misma direccion muerta una y otra vez. El 2026-10-17 el
	 * pedido 2395879 salio unas once veces en dos minutos, todas rebotadas.
	 *
	 * Cuenta los envios automaticos y los reenvios manuales juntos, porque para
	 * el cliente son el mismo correo llegando otra vez.
	 *
	 * VA POR DIRECCION, NO SOLO POR PEDIDO, y eso es a proposito: si quien
	 * atiende escribe otra direccion, el conteo de la anterior no puede
	 * impedirle el intento. Cambiar la direccion es justamente la salida cuando
	 * la que tenemos esta mala, y el tope no puede cerrarla.
	 *
	 * Solo cuenta las filas con correo: notificarLinkPago inserta una fila por
	 * cada aviso aunque no haya salido correo -para dejar el rastro del
	 * WhatsApp-, y esas no son intentos de envio.
	 *
	 * @param idPedido pedido del link
	 * @param email    direccion por la que se pregunta
	 * @return cuantas veces ya salio; 0 si no hay registro o si la consulta falla
	 */
	public static int contarEnviosACorreo(final int idPedido, final String email)
	{
		if (email == null || email.trim().length() == 0)
		{
			return (0);
		}
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			PreparedStatement ps = con1.prepareStatement(
					"SELECT COUNT(*) FROM pedido_pago_virtual"
					+ " WHERE idpedido = ? AND TRIM(LOWER(email)) = ?");
			ps.setInt(1, idPedido);
			ps.setString(2, email.trim().toLowerCase());
			ResultSet rs = ps.executeQuery();
			int veces = 0;
			if (rs.next())
			{
				veces = rs.getInt(1);
			}
			rs.close();
			ps.close();
			con1.close();
			return (veces);
		}
		catch (Exception e)
		{
			logger.error("PedidoPagoVirtualDAO.contarEnviosACorreo(" + idPedido + "): " + e.toString());
			try
			{
				con1.close();
			} catch (Exception e1)
			{
			}
			//Ante la duda se deja pasar: es peor bloquear un link de pago legitimo
			//porque la consulta fallo, que permitir un envio de mas.
			return (0);
		}
	}

	public static int insertarPedidoPagoVirtual(PedidoPagoVirtual pedPag)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			/*
			 * Va con parametros y no concatenando. El correo llega desde la
			 * pantalla de reenvio manual, es decir desde request.getParameter: un
			 * apostrofo en la direccion -que es raro pero legal en un correo-
			 * rompia el INSERT, y el aviso se perdia sin dejar rastro justo en el
			 * registro que existe para dejar rastro. Es inyeccion autenticada, no
			 * abierta, pero no hay razon para dejarla puesta.
			 */
			String insert = "insert into pedido_pago_virtual (idpedido, email, telefono_celular, observacion)"
					+ " values (?, ?, ?, ?)";
			PreparedStatement stm = con1.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
			stm.setInt(1, pedPag.getIdPedido());
			stm.setString(2, pedPag.getEmail());
			stm.setString(3, pedPag.getTelefonoCelular());
			stm.setString(4, pedPag.getObservacion());
			logger.info(insert + " [" + pedPag.getIdPedido() + ", " + pedPag.getEmail() + ", "
					+ pedPag.getTelefonoCelular() + ", " + pedPag.getObservacion() + "]");
			stm.executeUpdate();
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id forma evento en bd " + idEventoIns);
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
			return(0);
		}
		return(idEventoIns);
	}

}

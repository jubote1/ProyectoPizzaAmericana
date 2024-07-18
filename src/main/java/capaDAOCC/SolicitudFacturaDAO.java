package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.SolicitudFactura;
import conexionCC.ConexionBaseDatos;

public class SolicitudFacturaDAO {

	public static int insertarSolicitudFactura(SolicitudFactura solFactura)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSolicitud = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitud_factura (idpedidocontact, idpedidotienda, valor, nit, correo, empresa, telefono, fechapedido, usuario) values (" + solFactura.getIdPedidoContact() + "," + solFactura.getIdPedidoTienda() + "," + solFactura.getValor() + ",'" + solFactura.getNit() + "','" + solFactura.getCorreo() + "','" + solFactura.getEmpresa() + "','" + solFactura.getTelefono() + "','" + solFactura.getFechaPedido() + "' , '" + solFactura.getUsuario() + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idSolicitud  =rs.getInt(1);
				logger.info("Id Solicitud  " + idSolicitud );
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
		return(idSolicitud );
	}
	
	public static ArrayList<SolicitudFactura> consultarSolicitudesFacturaElectronica(String fechaInicial, String fechaFinal, String estado)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SolicitudFactura> solicitudes = new ArrayList();
		SolicitudFactura solTemp;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idSolicitud;
		int idPedidoContact;
		int idPedidoTienda;
		double valor;
		String nit;
		String correo;
		String empresa;
		String telefono;
		String fechaPedido;
		String fechaSolicitud;
		String estadoConsulta;
		String usuario;
		String fechaIni = fechaInicial.substring(6, 10)+"-"+fechaInicial.substring(3, 5)+"-"+fechaInicial.substring(0, 2) + " 00:00:00";	
		String fechaFin = fechaFinal.substring(6, 10)+"-"+fechaFinal.substring(3, 5)+"-"+fechaFinal.substring(0, 2) + " 23:59:59";
		try
		{
			Statement stm = con1.createStatement();
			String select = "";
			if(estado.equals(new String("TODOS")))
			{
				select = "select * from  solicitud_factura where fechasolicitud >= '" + fechaIni +"' and fechasolicitud <= '" + fechaFin + "'";
			}else
			{
				select = "select * from  solicitud_factura where fechasolicitud >= '" + fechaIni +"' and fechasolicitud <= '" + fechaFin + "' and estado = '" + estado +"'";
			}
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while (rs.next()){
				idSolicitud = rs.getInt("idsolicitud");
				idPedidoContact = rs.getInt("idpedidocontact");
				idPedidoTienda = rs.getInt("idpedidotienda");
				valor = rs.getDouble("valor");
				nit = rs.getString("nit");
				correo = rs.getString("correo");
				empresa = rs.getString("empresa");
				telefono = rs.getString("telefono");
				fechaPedido = rs.getString("fechapedido");
				fechaSolicitud = rs.getString("fechasolicitud");
				estadoConsulta = rs.getString("estado");
				usuario = rs.getString("usuario");
				solTemp = new SolicitudFactura(idSolicitud,idPedidoContact, idPedidoTienda, valor,  nit,
						correo, empresa, telefono,fechaPedido, fechaSolicitud, estadoConsulta, usuario);
				solicitudes.add(solTemp);
	        }
			rs.close();
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
		}
		return(solicitudes);
	}
	
	
	public static void procesarSolicitudFactura(int idSolicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSolicitudPQRSImaIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update solicitud_factura set estado = 'PROCESADO' where idsolicitud = " + idSolicitud; ; 
			logger.info(update);
			stm.executeUpdate(update);
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
	
	
}

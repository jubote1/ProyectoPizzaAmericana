package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import capaModeloCC.DomiciliarioPedido;
import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.MensajeTexto;
import capaModeloCC.Oferta;
import capaModeloCC.OfertaCliente;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
/**
 * Clase que implementa todos los métodos de acceso a la base de datos para la administración de la entidad Excepcion de Precio.
 * @author JuanDavid
 *
 */
public class DomiciliarioPedidoDAO {
	
	public static void insertarDomiciliarioPedido(DomiciliarioPedido domPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{ 
			Statement stm = con1.createStatement();
			String insert = "";
			insert = "insert into domiciliario_pedido (idusuario,fecha, idtienda, cantidad) values (" + domPedido.getIdUsuario() + " , '" + domPedido.getFecha() +  "' , " + domPedido.getIdTienda() + " , " + domPedido.getCantidad() + " )";
			logger.info(insert);
			stm.executeUpdate(insert);
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
	
	public ArrayList<JSONObject> obtenerPorId(int id,Boolean bandera) throws SQLException {
		ArrayList<JSONObject> lista = new ArrayList<JSONObject>();
		try {
		
	
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String sql ="SELECT  ubi.clave_dom,ti.nombre,  ubi.latitud, ubi.longitud,t.fecha,t.nombre FROM ubicacion_domiciliario ubi JOIN (SELECT  u.clave_dom AS clave ,MAX(u.fecha)"
				+ " AS fecha ,e.nombre_largo AS nombre,MAX(u.idubicacion) AS ubicacion from ubicacion_domiciliario u  left join general.empleado  e on u.clave_dom =  e.claverapida "
				+ "COLLATE UTF8_UNICODE_CI WHERE e.claverapida IS NOT NULL  GROUP BY u.clave_dom  ,e.nombre_largo) t ON ubi.idubicacion = t.ubicacion JOIN tienda ti ON ti.idtienda = ubi.idtienda";

		if(bandera) {
			sql =sql+" WHERE ubi.idtienda = "+id;
	
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Statement statement  = con1.createStatement();
		ResultSet rs = statement.executeQuery(sql);
		while (rs.next()) {
			String fechaTexto = formatter.format(rs.getTimestamp(5));
			JSONObject js = new JSONObject();
			js.put("clave_dom",rs.getString(1) );
			js.put("idtienda",rs.getString(2));
			js.put("latitud",rs.getString(3) );
			js.put("longitud",rs.getString(4) );
			js.put("fecha",fechaTexto);
			js.put("nombre_largo",rs.getString(6));

			lista.add(js);
		}
		rs.close();
		con1.close();
		} catch (Exception e) {

			System.out.println("" + e.toString());
		}
		return lista;
	}
	
	
	public ArrayList<JSONObject> ListaTiendas() {

		ArrayList<JSONObject> lista = new ArrayList<JSONObject>();
		try {
			String sql ="select idtienda, nombre from tienda  where nombre != 'Bodega' and nombre != 'Contact Center'";
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			Statement statement  = con1.createStatement();
			ResultSet r = statement.executeQuery(sql);
			while (r.next()) {
				JSONObject jsonobject = new JSONObject();
				jsonobject.put("id",r.getInt(1) );
				jsonobject.put("nombre",r.getString(2) );
			
				lista.add(jsonobject);
			}
			r.close();
			con1.close();
		} catch (Exception e) {

			System.out.println("" + e.toString());
		}

		return lista;
	}


}




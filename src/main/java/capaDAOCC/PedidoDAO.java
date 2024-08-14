package capaDAOCC;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.Cliente;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.DetallePedido;
import capaModeloCC.DetallePedidoAdicion;
import capaModeloCC.DetallePedidoPixel;
import capaModeloCC.DireccionFueraZona;
import capaModeloCC.Especialidad;
import capaModeloCC.Estadistica;
import capaModeloCC.EstadisticaPromocion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FormaPago;
import capaModeloCC.HomologaGaseosaIncluida;
import capaModeloCC.InsertarPedidoPixel;
import capaModeloCC.MarcacionPedido;
import capaModeloCC.ModificadorDetallePedido;
import capaModeloCC.NomenclaturaDireccion;
import capaModeloCC.Pedido;
import capaModeloCC.PedidoCanceladoPlataforma;
import capaModeloCC.PedidoInfoAdicional;
import capaModeloCC.PedidoMonitoreoPagoVirtual;
import capaModeloCC.PedidoPlat;
import capaModeloCC.PedidoPlataformaMonitoreo;
import capaModeloCC.Producto;
import capaModeloCC.ProductoIncluido;
import capaModeloCC.ResumenVentaEmpresarial;
import capaModeloCC.SaborLiquido;
import capaModeloCC.Tienda;
import capaModeloCC.TipoLiquido;
import conexionCC.ConexionBaseDatos;
import pixelposCC.Main;
import utilidadesCC.ControladorEnvioCorreo;

import java.sql.ResultSet;
import java.util.Date;

public class PedidoDAO {
	
	/**
	 * M�todo que se encarga de obtener todas especialidades de Pizza definidos en el sistema.
	 * @return Se retorna un ArrayList con todas las especialidades definidas en la base de datos.
	 */
	public static ArrayList<Especialidad> obtenerEspecialidad(int idExcepcion, int idProducto)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Especialidad> especialidades = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "";
			if(idExcepcion == 0 && idProducto == 0)
			{
				consulta = "select e.idespecialidad, e.nombre, e.abreviatura from especialidad e order by nombre asc";
			}else if(idExcepcion > 0 && idProducto == 0)
			{
				consulta = "select e.idespecialidad, e.nombre, e.abreviatura from especialidad e, controla_especialidades c where e.idespecialidad = c.idespecialidad and c.idexcepcion = " + idExcepcion +" order by nombre asc";
			}else if(idExcepcion == 0 && idProducto > 0)
			{
				consulta = "select e.idespecialidad, e.nombre, e.abreviatura from especialidad e, controla_especialidades c where e.idespecialidad = c.idespecialidad and c.idproducto = " + idProducto +" order by nombre asc";
			}
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idespecialidad;
			String nombre;
			String abreviatura;
			while(rs.next()){
				idespecialidad = rs.getInt("idespecialidad");
				nombre = rs.getString("nombre");
				abreviatura = rs.getString("abreviatura");
				Especialidad espec = new Especialidad( idespecialidad, nombre, abreviatura);
				especialidades.add(espec);
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
		return(especialidades);
		
	}
	
	/**
	 * M�todo que se encarga de retornar un ArrayList con objetos de Modelo Producto que correspongan a la tipolog�a
	 * otro productos
	 * @return Se retorna ArrayList con objetos de Modelo Producto que correspongan a la tipolog�a otros productos.
	 */
	public static ArrayList<Producto> obtenerOtrosProductos()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Producto> otrosProducto = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select p.idproducto, p.idreceta, p.nombre, p.descripcion, p.impuesto, p.tipo, p.preciogeneral from producto p where tipo = 'OTROS'  and habilitado = 'S'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idProducto;
			int idReceta;
			String nombre;
			String descripcion;
			float impuesto;
			String tipo;
			double precio;
			while(rs.next()){
				idProducto = rs.getInt("idproducto");
				idReceta = rs.getInt("idreceta");
				nombre = rs.getString("nombre");
				descripcion = rs.getString("descripcion");
				impuesto = rs.getFloat("impuesto");
				tipo = rs.getString("tipo");
				precio = rs.getDouble("preciogeneral");
				Producto prod = new Producto(idProducto, idReceta, nombre, descripcion,impuesto, tipo,precio,"S");
				otrosProducto.add(prod);
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
		return(otrosProducto);
		
	}
	
	/**
	 * M�todo que se encarga de retornar los productos tipo adici�n.
	 * @return Se retorna un ArrayLista con entidades MOdelo Producto de la tipolog�a de producto Adici�n.
	 */
	public static ArrayList<Producto> obtenerAdicionProductos()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Producto> adicionProducto = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select p.idproducto, p.idreceta, p.nombre, p.descripcion, p.impuesto, p.tipo, p.preciogeneral from producto p where tipo = 'ADICION' and habilitado = 'S'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idProducto;
			int idReceta;
			String nombre;
			String descripcion;
			float impuesto;
			String tipo;
			double precio;
			while(rs.next()){
				idProducto = rs.getInt("idproducto");
				idReceta = rs.getInt("idreceta");
				nombre = rs.getString("nombre");
				descripcion = rs.getString("descripcion");
				impuesto = rs.getFloat("impuesto");
				tipo = rs.getString("tipo");
				precio = rs.getDouble("preciogeneral");
				Producto prod = new Producto(idProducto, idReceta, nombre, descripcion,impuesto, tipo,precio,"S");
				adicionProducto.add(prod);
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
		return(adicionProducto);
		
	}

	/**
	 * M�todo que se encarga de retornar todos los prodcutos definidos en el sistema
	 * @return Se retorna un arrayList con objetos Modelo Producto, todos los definidos en el sistema.
	 */
	public static ArrayList<Producto> obtenerTodosProductos() {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Producto> todosProducto = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select p.idproducto, p.idreceta, p.nombre, p.descripcion, p.impuesto, p.tipo, p.producto_asocia_adicion, p.preciogeneral, p.incluye_liquido, p.idtipo_liquido, p.manejacantidad from producto p where p.habilitado = 'S' order by nombre asc ";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idProducto;
			int idReceta;
			String nombre;
			String descripcion;
			float impuesto;
			String tipo;
			int productoasociaadicion;
			double precio;
			String incluye_liquido;
			int idtipo_liquido;
			String manejacantidad;
			while(rs.next()){
				idProducto = rs.getInt("idproducto");
				idReceta = rs.getInt("idreceta");
				nombre = rs.getString("nombre");
				descripcion = rs.getString("descripcion");
				impuesto = rs.getFloat("impuesto");
				tipo = rs.getString("tipo");
				productoasociaadicion = rs.getInt("producto_asocia_adicion");
				precio = rs.getDouble("preciogeneral");
				incluye_liquido = rs.getString("incluye_liquido");
				idtipo_liquido = rs.getInt("idtipo_liquido");
				manejacantidad = rs.getString("manejacantidad");
				Producto prod = new Producto(idProducto, idReceta, nombre, descripcion,impuesto, tipo,productoasociaadicion, precio, incluye_liquido, idtipo_liquido, manejacantidad, "S");
				todosProducto.add(prod);
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
		return(todosProducto);
		
	}
	
	/**
	 * M�todo que se encarga de retornar los sabores l�quido definidos para un producto.
	 * @param idProdu Se recibe como par�metro el id producto del cual se desean consultar los sabores tipo liquidos
	 * disponibles por parametrizaci�n.
	 * @return Se retorna un ArrayList con objetos Modelo SaborLiquido con la informaci�n seg�n los par�metros recibidos.
	 */
	public static ArrayList<SaborLiquido> ObtenerSaboresLiquidoProducto(int idProdu, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SaborLiquido> saboresLiquido = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idsabor_x_tipo_liquido, a.descripcion, b.idtipo_liquido, b.nombre, c.idproducto, c.descripcion, a.valor_adicional from sabor_x_tipo_liquido a , tipo_liquido b, producto c where a.idtipo_liquido = b.idtipo_liquido and c.idtipo_liquido = b.idtipo_liquido and c.idproducto = " + idProdu + " and a.idproducto not in (select idproducto from producto_no_existente where idtienda = " + idtienda +  ")";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idSaborTipoLiquido;
			String descripcionSabor;
			int idLiquido;
			String descripcionLiquido;
			int idProducto;
			String nombreProducto;
			double valorAdicional;
			while(rs.next())
			{
				idSaborTipoLiquido = rs.getInt("idsabor_x_tipo_liquido");
				descripcionSabor= rs.getString("descripcion");
				idLiquido= rs.getInt("idtipo_liquido");
				descripcionLiquido=rs.getString("nombre");
				idProducto = rs.getInt("idproducto");
				nombreProducto = rs.getString("descripcion");
				valorAdicional = rs.getDouble("valor_adicional");
				SaborLiquido saborLiq = new SaborLiquido(idSaborTipoLiquido, descripcionSabor,idLiquido,descripcionLiquido,idProducto,nombreProducto,valorAdicional);
				saboresLiquido.add(saborLiq);
			}
			rs.close();
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
		return(saboresLiquido);
	}
	
	/**
	 * M�todo que retorna los sabores tipo liquido que tiene disponible un excepci�n de precio.
	 * @param idExce Se recibe como par�metro un entero con el idexcepcion del cual se quiere consultar
	 * los sabores de liquido disponible
	 * @return Se retorna un ArrayList con objetos MOdelo SaborLiquido de acuerdo a la excepcion pasada como 
	 * par�metro.
	 */
	public static ArrayList<SaborLiquido> ObtenerSaboresLiquidoExcepcion(int idExce, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SaborLiquido> saboresLiquido = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idsabor_x_tipo_liquido, a.descripcion, b.idtipo_liquido, b.nombre, c.idexcepcion, c.descripcion, a.valor_adicional from sabor_x_tipo_liquido a , tipo_liquido b, excepcion_precio c where a.idtipo_liquido = b.idtipo_liquido and c.idtipoliquido = b.idtipo_liquido and c.idexcepcion = " + idExce + " and a.idproducto not in (select idproducto from producto_no_existente where idtienda = " + idtienda +  ")";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idSaborTipoLiquido;
			String descripcionSabor;
			int idLiquido;
			String descripcionLiquido;
			int idExcepcion;
			String nombreExcepcion;
			double valorAdicional;
			while(rs.next())
			{
				idSaborTipoLiquido = rs.getInt("idsabor_x_tipo_liquido");
				descripcionSabor= rs.getString("descripcion");
				idLiquido= rs.getInt("idtipo_liquido");
				descripcionLiquido=rs.getString("nombre");
				idExcepcion = rs.getInt("idexcepcion");
				nombreExcepcion = rs.getString("descripcion");
				valorAdicional = rs.getDouble("valor_adicional");
				SaborLiquido saborLiq = new SaborLiquido(idSaborTipoLiquido, descripcionSabor,idLiquido,descripcionLiquido,nombreExcepcion, idExcepcion, valorAdicional);
				saboresLiquido.add(saborLiq);
			}
			rs.close();
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
		
		return(saboresLiquido);
	}
	
	
	/**
	 * M�todo que se encarga de insertar el encabezado de un pedido.
	 * @param idtienda Se recibe la tienda a la cual pertenecer� el pedido.
	 * @param idcliente Se recibe el idcliente para el cual se tomar� el pedido
	 * @param fechaPedido Se inserta la fecha del pedido.
	 * @return Se retorna el idpedido retornado en por la base de datos en la inserci�n.
	 */
	public static int InsertarEncabezadoPedido(int idtienda, int idcliente, String fechaPedido, String user, String esProgramado, String programado, int idTipoPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idPedidoInsertado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaPedidoFinal ="";
		try
		{
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(fechaPedido);
			fechaPedidoFinal = formatoFinal.format(fechaTemporal);
			
		}catch(Exception e){
			logger.error(e.toString());
			return(0);
		}
		
		
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido (idtienda,idcliente, idestadopedido,fechapedido, usuariopedido, programado, hora_programado, idtipopedido) values (" + idtienda + ", " + idcliente + ", 1 , '" + fechaPedidoFinal  + "' , '" + user + "' , '" + esProgramado + "' , '" + programado + "' ," + idTipoPedido + ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idPedidoInsertado=rs.getInt(1);
				System.out.println(idPedidoInsertado);
	        }
	        rs.close();
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
		return(idPedidoInsertado);
	}
	
	/**
	 * Se tendr� un m�todo diferencial para la tienda virtual debido a que el campo origen ser� diferencial para distinguir
	 * si el pedido viene del contact center o viene de la tienda virtual
	 * @param idtienda
	 * @param idcliente
	 * @param fechaPedido
	 * @param user
	 * @return
	 */
	public static int InsertarEncabezadoPedidoTiendaVirtual(int idtienda, int idcliente, String fechaPedido, String user, long idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		int idPedidoInsertado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaPedidoFinal ="";
		try
		{
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(fechaPedido);
			fechaPedidoFinal = formatoFinal.format(fechaTemporal);
			
		}catch(Exception e){
			logger.error(e.toString());
			return(0);
		}
		
		
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido (idtienda,idcliente, idestadopedido,fechapedido, usuariopedido, origen, idordencomercio) values (" + idtienda + ", " + idcliente + ", 1 , '" + fechaPedidoFinal  + "' , '" + user + "' , 'T' ," + idOrdenComercio + ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idPedidoInsertado=rs.getInt(1);
				System.out.println(idPedidoInsertado);
	        }
	        rs.close();
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
		return(idPedidoInsertado);
	}
	
	
	/**
	 * Creamos un m�todo diferencial para la tienda virtual KUNO dado que ser� necesario diferenciar ciertas cosas
	 * @param idtienda
	 * @param idcliente
	 * @param fechaPedido
	 * @param user
	 * @param idOrdenComercio
	 * @param idTipoPedido
	 * @return
	 */
	public static int InsertarEncabezadoPedidoTiendaVirtualKuno(int idtienda, int idcliente, String fechaPedido, String user, long idOrdenComercio, int idTipoPedido, String origenPedido, String fuentePedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idPedidoInsertado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaPedidoFinal ="";
		try
		{
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(fechaPedido);
			fechaPedidoFinal = formatoFinal.format(fechaTemporal);
			
		}catch(Exception e){
			logger.error(e.toString());
			return(0);
		}
		
		
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido (idtienda,idcliente, idestadopedido,fechapedido, usuariopedido, origen, idordencomercio, idtipopedido, fuentepedido) values (" + idtienda + ", " + idcliente + ", 1 , '" + fechaPedidoFinal  + "' , '" + user + "' , '" + origenPedido + "' ," + idOrdenComercio + " , " + idTipoPedido + " , '" + fuentePedido +"')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idPedidoInsertado=rs.getInt(1);
				System.out.println(idPedidoInsertado);
	        }
	        rs.close();
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
		return(idPedidoInsertado);
	}
	
	public static int InsertarEncabezadoPedidoDIDI(int idtienda, int idcliente, String fechaPedido, String user, BigInteger idOrdenComercio, int idTipoPedido, String origenPedido, String fuentePedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idPedidoInsertado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaPedidoFinal ="";
		try
		{
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(fechaPedido);
			fechaPedidoFinal = formatoFinal.format(fechaTemporal);
			
		}catch(Exception e){
			logger.error(e.toString());
			return(0);
		}
		
		
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido (idtienda,idcliente, idestadopedido,fechapedido, usuariopedido, origen, idordencomercio, idtipopedido, fuentepedido) values (" + idtienda + ", " + idcliente + ", 1 , '" + fechaPedidoFinal  + "' , '" + user + "' , '" + origenPedido + "' ," + idOrdenComercio + " , " + idTipoPedido + " , '" + fuentePedido +"')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idPedidoInsertado=rs.getInt(1);
				System.out.println(idPedidoInsertado);
	        }
	        rs.close();
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
		return(idPedidoInsertado);
	}
	
	
	/**
	 * M�todo que se encarga de ir insertando uno a uno los item pedido solicitados por el cliente.
	 * @param detPedido Se recibe como par�metro en un objeto Modelo DetallePedido la informaci�n de cada detalle pedido
	 * y se va insertando en base de datos
	 * @return Con respuesta se va obteniendo un n�mero entero con el iddetallepedido
	 */
	public static int InsertarDetallePedido(DetallePedido detPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idDetallePedidoInsertado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into detalle_pedido (idproducto,idpedido,cantidad, idespecialidad1, idespecialidad2, valorUnitario, valorTotal, adicion, observacion, idsabortipoliquido, idexcepcion,modespecialidad1, modespecialidad2) values (" + detPedido.getIdproducto() + " , " + detPedido.getIdpedido() + " , " + detPedido.getCantidad() + " , " + detPedido.getIdespecialidad1() + " , " + detPedido.getIdespecialidad2() + " , " + detPedido.getValorunitario() + " , " + detPedido.getValortotal() + ", '" + detPedido.getAdicion() + "' , '"+detPedido.getObservacion() + "' , " + detPedido.getIdsabortipoliquido() + " , " + detPedido.getIdexcepcion() + " , '"+ detPedido.getModespecialidad1() + "' , '" + detPedido.getModespecialidad2() +"' )"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idDetallePedidoInsertado=rs.getInt(1);
				System.out.println(idDetallePedidoInsertado);
	        }
	        rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idDetallePedidoInsertado);
	}
	
	/**
	 * M�todo que se encarga de insertar el detalle de la adici�n en otra tabla relacional que involucra un iddetallepedido
	 * un idadici�n
	 * @param detPedidoAdicion Se recibe objeto Modelo DetallePedidoAdicion con base en el cual se realiza la inserci�n
	 * del detalle de adici�n para un producto determinado
	 * @return Se retorna el id asociado a la inserci�n realizada
	 */
	public static int InsertarDetalleAdicion(DetallePedidoAdicion detPedidoAdicion)
	{
		Logger logger = Logger.getLogger("log_file");
		int idAdicion = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert adicion_detalle_pedido (iddetallepedidopadre, iddetallepedidoadicion, idespecialidad1, idespecialidad2, cantidad1, cantidad2) "
					+ "values (" + detPedidoAdicion.getIddetallepedidopadre() + " , " + detPedidoAdicion.getIddetallepedidoadicion()+ " , "
							+  detPedidoAdicion.getIdespecialidad1() + " , " + detPedidoAdicion.getIdespecialidad2() + " , " + detPedidoAdicion.getCantidad1() + " , " + detPedidoAdicion.getCantidad2()+ ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idAdicion=rs.getInt(1);
				
	        }
	        rs.close();
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
		return(idAdicion);
	}
	
	/**
	 * M�todo que se encarga de insertar la informaci�n de un modificador CON/SIN de un detalle pedido en particular
	 * @param modDetallePedido Recibe como par�metro el objeto Modelo ModificadorDetallePedido con la informaci�n
	 * a insertar
	 * @return Retorne en un entero el idmodificador retornado luego de la inserci�n en base de datos.
	 */
	public static int InsertarModificadorDetallePedido(ModificadorDetallePedido modDetallePedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idModificador = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert modificador_detalle_pedido (iddetallepedidopadre, idproductoespecialidad1,idproductoespecialidad2,cantidad,iddetallepedidoasociado) "
					+ "values (" + modDetallePedido.getIddetallepedidopadre() + " , " + modDetallePedido.getIdproductoespecialidad1() + " , "
							+  modDetallePedido.getIdproductoespecialidad2() + " , " + modDetallePedido.getCantidad() + " , " + modDetallePedido.getIddetallepedidoasociado() + ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idModificador=rs.getInt(1);
				
	        }
	        rs.close();
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
		return(idModificador);
	}
	
	/**
	 * M�todo que permite retornar un idespecialidad, con base en el nombre especialidad pasado como par�metro
	 * @param especialidad Se recibe como par�metro un String el nombre de la especialidad.
	 * @return Se retorna el idespecialidad asociado a la especialidad enviada como par�metro.
	 */
	public static int obtenerIdEspecialidad(String especialidad)
	{
		Logger logger = Logger.getLogger("log_file");
		int idespecialidad = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idespecialidad from especialidad where abreviatura = '" + especialidad + "' " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idespecialidad = rs.getInt("idespecialidad");
				break;
			}
	        rs.close();
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
		return(idespecialidad);
	}
	
	/**
	 * M�todo que se encarga de obtener el valor total de un pedido, con base en todos los detalles asociados a un pedido
	 * en particular.
	 * @param idpedido Se recibe como par�metro el idpedido del cual se requiere obtener el total.
	 * @return Se retorna un valor doble que tiene el total del valor del pedido pasado como par�metro.
	 */
	public static double obtenerTotalPedido(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		double valorTotal = 0;
		try
		{
			
			Statement stm = con1.createStatement();
			String consulta = "select sum(valorTotal) from detalle_pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				valorTotal = rs.getDouble(1);
				break;
			}
			
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			valorTotal = 0;
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		
		return(valorTotal);
	}
	
	/**
	 * M�todo para obtener el total Neto de la tabla Pedido
	 * @param idpedido
	 * @return
	 */
	public static long obtenerTotalNetoPedido(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		double valorTotal = 0;
		try
		{
			
			Statement stm = con1.createStatement();
			String consulta = "select total_neto from pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				valorTotal = rs.getDouble(1);
				break;
			}
			
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			valorTotal = 0;
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		
		return((long)valorTotal);
	}
	
	
	public static long calcularTotalNetoPedido(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		double valorTotal = 0;
		try
		{
			
			Statement stm = con1.createStatement();
			String consulta = "select sum(valorTotal) from detalle_pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				valorTotal = rs.getDouble(1);
				break;
			}
			
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			valorTotal = 0;
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		
		return((long)valorTotal);
	}
	
	public static int obtenerIdClientePedido(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idcliente = 0;
		try
		{
			
			Statement stm = con1.createStatement();
			String consulta = "select idcliente from pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idcliente = Integer.parseInt(rs.getString("idcliente"));
				break;
			}
			
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			idcliente = 0;
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		
		return(idcliente);
	}
	
	/**
	 * M�todo que se encarga de actualizar un pedido con el n�mero de pedido el cual le fue asignado en la tienda correspondiente.
	 * @param idpedido Se recibe como par�metro el idpedido en el sistema de Contact Center.
	 * @param numPedidoPixel Se recibe el n�mero de pedido que le fue asignado al pedido en la tienda correspondiente.
	 * @return Se retorna un valor booleano que indica si la actualizaci�n fue exitosa(true) o no fue exitosa(false).
	 */
	public static boolean actualizarEstadoNumeroPedidoPixel(int idpedido, int numPedidoPixel)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set numposheader = " + numPedidoPixel + " , enviadoPixel = 1, fechaenviotienda = CURRENT_TIMESTAMP() where idpedido = "+ idpedido;
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
			return(false);
		}
		return(true);
	}
	
	public static int obtenerEstadoEnviadoPixel(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int enviadoPixel = 0;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String consulta = "select enviadopixel from pedido where idpedido = "+ idpedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				enviadoPixel = rs.getInt("enviadopixel");
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
			return(enviadoPixel);
		}
		return(enviadoPixel);
	}
	
	public static boolean cancelarPedido(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set idestadopedido = 4  where idpedido = "+ idpedido;
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
			return(false);
		}
		return(true);
	}
	
	
	/**
	 * M�todo que se encarga de en la tabla pedido almacenar el campo de idLink para los pedidos con tipo de pago virtual con WOMPI
	 * @param idpedido
	 * @param idLink
	 * @return
	 */
	public static boolean actualizarLinkPagoPedido(int idpedido, String idLink)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set idlink = '" + idLink + "'  where idpedido = "+ idpedido;
			logger.info(update);
			stm.executeUpdate(update);
			//Posteriormente guardamos en la tabla de links por pago
			String insert = "insert into pedido_idlink (idpedido, idlink) values (" + idpedido + ", '" + idLink + "')";
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
			return(false);
		}
		return(true);
	}
	
	
	/**
	 * M�todo que permite la marcaci�n de que el pedido ya fue aceptado en rappi
	 * @param idpedido
	 * @return
	 */
	public static boolean marcarEnviadoPedidoRappi(long idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set aceptado_rappi = 1  where idordencomercio = "+ idOrdenComercio;
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
			return(false);
		}
		return(true);
	}
	
	public static boolean marcarEnviadoPedidoDidi(BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set aceptado_rappi = 1  where idordencomercio = "+ idOrdenComercio;
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
			return(false);
		}
		return(true);
	}
	
	public static boolean consultarExistenciaOrdenRappi(long idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String select = "select * from  pedido where idordencomercio = "+ idOrdenComercio + " and origen = 'RAP'";
			logger.info(select);
			ResultSet rs =stm.executeQuery(select);
			while(rs.next())
			{
				respuesta = true;
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
		return(respuesta);
	}
	
	public static boolean consultarExistenciaOrdenDidi(BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String select = "select * from  pedido where idordencomercio = "+ idOrdenComercio + " and origen = 'DID'";
			logger.info(select);
			ResultSet rs =stm.executeQuery(select);
			while(rs.next())
			{
				respuesta = true;
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
		return(respuesta);
	}
	
	/**
	 * M�todo que se encarga de eliminar un pedido que no ha sido confirmado en el sistema.
	 * @param idpedido Se recibe como par�metro el idpedido que se desea eliminar.
	 * @return Se retorna valor booleano indicando si el proceso fue exitoso (true) o si no fue exitoso (false).
	 */
	public static boolean eliminarPedidoSinConfirmar(int idpedido, int idcliente, String usuario)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from modificador_detalle_pedido  where iddetallepedidopadre in (select iddetalle_pedido from detalle_pedido where idpedido = "+ idpedido +")";
			logger.info(delete);
			stm.executeUpdate(delete);
			delete = "delete from adicion_detalle_pedido  where iddetallepedidopadre in (select iddetalle_pedido from detalle_pedido where idpedido = "+ idpedido +")";
			logger.info(delete);
			stm.executeUpdate(delete);
			delete = "delete from detalle_pedido  where idpedido = "+ idpedido ;
			logger.info(delete);
			stm.executeUpdate(delete);
			delete = "delete from pedido  where idpedido = "+ idpedido ;
			logger.info(delete);
			stm.executeUpdate(delete);
			String logDelete = "insert into log_reiniciar_pedido (idpedido, idcliente, usuario) values ("+ idpedido + " , " + idcliente + " , '" + usuario + "')";
			logger.info(logDelete);
			stm.executeUpdate(logDelete);
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
			return(false);
		}
		return(true);
	}
	
	public static boolean actualizarJSONPedido(int idpedido, String resultadoJSON, String usuarioReenvio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update pedido set stringpixel = '" + resultadoJSON + "' , usuarioreenvio = '" + usuarioReenvio + "' where idpedido = "+ idpedido;
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
			return(false);
		}
		return(true);
	}
	
	/**
	 * M�todo que se encarga de cerrar un pedido, para no poder adicionarle m�s detalles, con el cual se totaliza la 
	 * informaci�n.
	 * @param idpedido El id pedido que se finalizar�.
	 * @param idformapago La forma pago que tendr� el pedido que se va finalizar.
	 * @param valorformapago Valor con el cual se pagar� el pedido.
	 * @param valortotal
	 * @param idcliente idcliente asociado al pedido.
	 * @param insertado Marcaci�n que indica si el cliente fue actualizado o insertado.
	 * @param tiempoPedido se recibe como par�metro el tiempo pedido que se le dio al cliente al momento de confirmar dicho pedido.
	 * @return Se retorna un valor booleano con el resultado del proceso exitoso (true) o no exitoso (false).
	 */
	public static InsertarPedidoPixel finalizarPedido(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado, double tiempopedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			double valorTotal = 0;
			Statement stm = con1.createStatement();
			String consulta = "select sum(valorTotal) from detalle_pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				valorTotal = rs.getDouble(1);
				break;
			}
			String update = "update pedido set total_bruto =" + valorTotal* 0.92 + " , impuesto = " + valorTotal * 0.08 + " , total_neto =" + valorTotal + " , idestadopedido = 2, tiempopedido = " + tiempopedido +  " where idpedido = " + idpedido;
			logger.info(update);
			stm.executeUpdate(update);
			String insertformapago = "insert pedido_forma_pago (idpedido, idforma_pago, valortotal, valorformapago) values (" + idpedido + " , " + idformapago + " , " + valortotal + " , " + valorformapago + ")";
			logger.info(insertformapago);
			stm.executeUpdate(insertformapago);
			rs.close();
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
		//Debemos obtener el idTienda del Pedido que vamos a finalizar
		Tienda tiendaPedido = PedidoDAO.obtenerTiendaPedido(idpedido);
		
		
		//Recuperar la formapago de la tienda homologada
		int idformapagotienda = PedidoDAO.obtenerFormaPagoHomologadaTienda(tiendaPedido.getIdTienda(), idformapago);
		
		//Llamado Inserci�n Pixel
		
				
		//La invocaci�n del pedido ya no se realizar� as�
		//Main principal = new Main();
		Cliente cliente = ClienteDAO.obtenerClienteporID(idcliente);
		boolean indicadorAct = false;
		if(insertado == 0)
		{
			indicadorAct = true;
		}
		
		// Recolectamos todos los datos para realizar la inserci�n en el POS tienda
		InsertarPedidoPixel pedidoPixel = PedidoPixelDAO.confirmarPedidoPixel(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, idformapagotienda);
		return(pedidoPixel);
	}
	
	
	public static boolean actualizarFormaPago(int idPedido, int idFormaPagoNueva, double valorFormaPagoNueva, double valorTotal, boolean virtualOrigen, boolean virtualDestino)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String borradoFormaPago = "delete from pedido_forma_pago where idpedido = " + idPedido;
			logger.info(borradoFormaPago );
			stm.executeUpdate(borradoFormaPago);
			String insertformapago = "insert pedido_forma_pago (idpedido, idforma_pago, valortotal, valorformapago) values (" + idPedido + " , " + idFormaPagoNueva + " , " + valorTotal + " , " + valorFormaPagoNueva + ")";
			logger.info(insertformapago);
			stm.executeUpdate(insertformapago);
			//Realizamos cambios adicionales si est� involucrado el pago virtual
			if(virtualOrigen)
			{
				String updatePagoVirtual = "update pedido set enviadopixel = 0 where idpedido = " + idPedido;
				logger.info(updatePagoVirtual);
				stm.executeUpdate(updatePagoVirtual);
			}
			if(virtualDestino)
			{
				String updatePagoVirtualDestino = "update pedido set enviadopixel = 2, origen = 'C' , fechainsercion = CURRENT_TIMESTAMP(), fechafinalizacion = CURRENT_TIMESTAMP() where idpedido = " + idPedido;
				logger.info(updatePagoVirtualDestino);
				stm.executeUpdate(updatePagoVirtualDestino);
			}
			//Realizamos cierre de las conexiones
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(true);
	}
	
	public static InsertarPedidoPixel finalizarPedidoPOSPM(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado, double tiempopedido, double descuento, String esProgramado, String programado)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			double valorTotal = 0;
			Statement stm = con1.createStatement();
			String consulta = "select sum(valorTotal) from detalle_pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				valorTotal = rs.getDouble(1);
				//Incluimos el valor del descuento
				valorTotal = valorTotal - descuento;
				break;
			}
			//Validamos si la forma de pago es virtual para as� al momento de cerrar el pedido en la tabla pedido debemos de hacer algo adicional
			String virtual = "";
			String consultaFormaPago = "select a.virtual from forma_pago a where a.idforma_pago =" +idformapago; 
			ResultSet rs2 = stm.executeQuery(consultaFormaPago);
			while(rs2.next())
			{
				virtual = rs2.getString("virtual").trim();
				break;
			}
			//
			String update = "";
			if(virtual.equals(new String("N")))
			{
				update = "update pedido set total_bruto =" + (valorTotal / 1.08) + " , impuesto = " + (valorTotal- (valorTotal / 1.08)) + " , total_neto =" + valorTotal + " , idestadopedido = 2, tiempopedido = " + tiempopedido + " , descuento = " + descuento +  " , fechafinalizacion = CURRENT_TIMESTAMP() , programado = '" + esProgramado + "' , hora_programado = '" + programado + "' where idpedido = " + idpedido;
			}else
			{
				update = "update pedido set total_bruto =" + (valorTotal / 1.08) + " , impuesto = " + (valorTotal- (valorTotal / 1.08)) + " , total_neto =" + valorTotal + " , idestadopedido = 2, tiempopedido = " + tiempopedido + " , descuento = " + descuento +  " , fechafinalizacion = CURRENT_TIMESTAMP() , programado = '" + esProgramado + "' , hora_programado = '" + programado + "' , enviadopixel = 2  where idpedido = " + idpedido;
			}
			logger.info(update);
			stm.executeUpdate(update);
			//Vamos a realizar una verificaci�n antes si ya tiene forma pago para no insertarla
			boolean existeFormaPago = false;
			String consultaVerificacion = "select * from pedido_forma_pago where idpedido = " + idpedido;
			ResultSet rs1 = stm.executeQuery(consultaVerificacion);
			while(rs1.next()){
				existeFormaPago = true;
				break;
			}
			if(!existeFormaPago)
			{
				String insertformapago = "insert pedido_forma_pago (idpedido, idforma_pago, valortotal, valorformapago) values (" + idpedido + " , " + idformapago + " , " + valortotal + " , " + valorformapago + ")";
				logger.info(insertformapago);
				stm.executeUpdate(insertformapago);
			}
			rs.close();
			rs1.close();
			rs2.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		//Debemos obtener el idTienda del Pedido que vamos a finalizar
		Tienda tiendaPedido = PedidoDAO.obtenerTiendaPedido(idpedido);
		
		
		//Recuperar la formapago de la tienda homologada
		int idformapagotienda = PedidoDAO.obtenerFormaPagoHomologadaTienda(tiendaPedido.getIdTienda(), idformapago);
		
		//Llamado Inserci�n Pixel
		
				
		//La invocaci�n del pedido ya no se realizar� as�
		//Main principal = new Main();
		Cliente cliente = ClienteDAO.obtenerClienteporID(idcliente);
		boolean indicadorAct = false;
		if(insertado == 0)
		{
			indicadorAct = true;
		}
		
		// Recolectamos todos los datos para realizar la inserci�n en el POS tienda
		InsertarPedidoPixel pedidoPixel = PedidoPOSPMDAO.confirmarPedidoPOSPM(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, idformapagotienda);
		return(pedidoPixel);
	}
	
	
	/**
	 * M�todo que realiza el cierre del pedido cuando es de la tienda virtual, no hace ni los preparativos ni las homologaciones
	 * teniendo en cuenta que inicialmente no va a realizar el env�o a tienda.
	 * @param idpedido
	 * @param idformapago
	 * @param idcliente
	 * @param insertado
	 * @param tiempopedido
	 * @param descuento
	 */
	public static double finalizarPedidoPOSPMTiendaVirtual(int idpedido, int idformapago, int idcliente, double tiempopedido, double descuento, double valorFormaPago, String programado, String horaProgramado, int idEstadoPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		double valorTotal = 0;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select sum(valorTotal) from detalle_pedido where idpedido = " + idpedido + " " ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				valorTotal = rs.getDouble(1);
				//Incluimos el valor del descuento
				valorTotal = valorTotal - descuento;
				break;
			}
			//Validamos si la forma de pago es virtual para as� al momento de cerrar el pedido en la tabla pedido debemos de hacer algo adicional
			String virtual = "";
			String consultaFormaPago = "select a.virtual from forma_pago a where a.idforma_pago =" +idformapago; 
			ResultSet rs2 = stm.executeQuery(consultaFormaPago);
			while(rs2.next())
			{
				virtual = rs2.getString("virtual").trim();
				break;
			}
			//
			String update = "";
			if(virtual.equals(new String("N")))
			{
				update = "update pedido set total_bruto =" + (valorTotal / 1.08) + " , impuesto = " + (valorTotal- (valorTotal / 1.08)) + " , total_neto =" + valorTotal + " , idestadopedido = " + idEstadoPedido +", tiempopedido = " + tiempopedido + " , descuento = " + descuento +  " , fechafinalizacion = CURRENT_TIMESTAMP() , programado = '" + programado + "' , hora_programado = '" + horaProgramado + "' where idpedido = " + idpedido;
			}else
			{
				update = "update pedido set total_bruto =" + (valorTotal / 1.08) + " , impuesto = " + (valorTotal- (valorTotal / 1.08)) + " , total_neto =" + valorTotal + " , idestadopedido = " + idEstadoPedido + ", tiempopedido = " + tiempopedido + " , descuento = " + descuento +  " , fechafinalizacion = CURRENT_TIMESTAMP() , enviadopixel = 2 , programado = '" + programado + "' , hora_programado = '" + horaProgramado + "'  where idpedido = " + idpedido;
			}
			logger.info(update);
			stm.executeUpdate(update);
			//Vamos a realizar una verificaci�n antes si ya tiene forma pago para no insertarla
			boolean existeFormaPago = false;
			String consultaVerificacion = "select * from pedido_forma_pago where idpedido = " + idpedido;
			ResultSet rs1 = stm.executeQuery(consultaVerificacion);
			while(rs1.next()){
				existeFormaPago = true;
				break;
			}
			if(!existeFormaPago)
			{
				if(valorFormaPago == 0)
				{
					valorFormaPago = valorTotal;
				}
				String insertformapago = "insert pedido_forma_pago (idpedido, idforma_pago, valortotal, valorformapago) values (" + idpedido + " , " + idformapago + " , " + valorTotal + " , " + valorFormaPago + ")";
				logger.info(insertformapago);
				stm.executeUpdate(insertformapago);
			}
			rs.close();
			rs1.close();
			rs2.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(valorTotal);
	}
	
	/**
	 * M�todo que se encarga de ciertas acciones propias de la confirmaci�n del pedido en el sistema tienda, dado que en el sistema
	 * de contact center ya fue confirmada la informaci�n.
	 * @param idpedido
	 * @param idformapago
	 * @param valorformapago
	 * @param valortotal
	 * @param idcliente
	 * @param insertado
	 * @return
	 */
	public static InsertarPedidoPixel finalizarPedidoReenvio(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		
		//Debemos obtener el idTienda del Pedido que vamos a finalizar
		Tienda tiendaPedido = PedidoDAO.obtenerTiendaPedido(idpedido);
		
		
		//Recuperar la formapago de la tienda homologada
		int idformapagotienda = PedidoDAO.obtenerFormaPagoHomologadaTienda(tiendaPedido.getIdTienda(), idformapago);
		Cliente cliente = ClienteDAO.obtenerClienteporID(idcliente);
		boolean indicadorAct = false;
		if(insertado == 0)
		{
			indicadorAct = true;
		}
		
		// Recolectamos todos los datos para realizar la inserci�n en el POS tienda
		InsertarPedidoPixel pedidoPixel = PedidoPixelDAO.confirmarPedidoPixel(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, idformapagotienda);
		return(pedidoPixel);
	}
	
	public static InsertarPedidoPixel finalizarPedidoReenvioPOSPM(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado, String userReenvio, boolean enviarTienda)
	{
		Logger logger = Logger.getLogger("log_file"); 
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		
		//Debemos obtener el idTienda del Pedido que vamos a finalizar
		Tienda tiendaPedido = PedidoDAO.obtenerTiendaPedido(idpedido);
		
		
		//Recuperar la formapago de la tienda homologada
		int idformapagotienda = PedidoDAO.obtenerFormaPagoHomologadaTienda(tiendaPedido.getIdTienda(), idformapago);
		//Vamos a controlar el reenv�o del mensaje de texto para alertar fraudes
		if((idformapagotienda == 4) && (!userReenvio.equals(new String(""))) && enviarTienda)
		{
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
			ArrayList correos = new ArrayList();
			correo.setAsunto("ANTIFRAUDE  " + idpedido);
			String correoEle = "jubote1@gmail.com";
			correos.add(correoEle);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje(" Cuidado hay un pedido de pago virtual WOMPI que fue reenviado " + idpedido);
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
		}
		Cliente cliente = ClienteDAO.obtenerClienteporID(idcliente);
		boolean indicadorAct = false;
		if(insertado == 0)
		{
			indicadorAct = true;
		}
		
		// Recolectamos todos los datos para realizar la inserci�n en el POS tienda
		InsertarPedidoPixel pedidoPixel = PedidoPOSPMDAO.confirmarPedidoPOSPM(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, idformapagotienda);
		return(pedidoPixel);
	}
	
	/**
	 * M�todo que se encarga de retornar la url de una tienda, dado un idpedido	
	 * @param idpedido Se recibe como par�metro  el idpedido, del cual se requiere extrear la URL de la tienda
	 * @return Se retorna un String con la URL del servicio de la tienda
	 */
	public static String obtenerUrlTienda(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idtienda = 0;
		String url = "";
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idtienda from pedido where idpedido = " + idpedido ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idtienda = rs.getInt("idtienda");
				break;
			}
			consulta = "select url from tienda where idtienda = " + idtienda;
			logger.info(consulta);
			rs = stm.executeQuery(consulta);
			while(rs.next()){
				url = rs.getString("url");
				break;
			}
			rs.close();
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
		return(url);
	}
	
	public static ArrayList<NomenclaturaDireccion> obtenerNomenclaturaDireccion()
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<NomenclaturaDireccion> nomenclaturas = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from nomenclatura_direccion" ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idnomenclatura;
			String nomenclatura;
			while(rs.next()){
				idnomenclatura = rs.getInt("idnomenclatura");
				nomenclatura = rs.getString("nomenclatura");
				NomenclaturaDireccion nomen = new NomenclaturaDireccion(idnomenclatura, nomenclatura);
				nomenclaturas.add(nomen);
			}
			rs.close();
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
		return(nomenclaturas);
	}
	
	
	public static int obtenerFormaPagoHomologadaTienda(int idtienda, int idformapago)
	{
		Logger logger = Logger.getLogger("log_file");
		int idformapagohomo = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idformapagotienda from homologacion_forma_pago where idtienda = " + idtienda + " and idforma_pago = " + idformapago ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idformapagohomo = rs.getInt("idformapagotienda");
				break;
			}
			rs.close();
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
		return(idformapagohomo);
	}
	
	/**
	 * M�todo que se encarga de retornar los tipos Liquidos existentes en el sistema.
	 * @return Se retorna un ArrayList con los objetos Modelo TipoLiquido existentes en el sistema.
	 */
	public static ArrayList<TipoLiquido> ObtenerTiposLiquido()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<TipoLiquido> tiposLiquido = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idtipo_liquido, nombre, capacidad	 from tipo_liquido " ;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idtipo_liquido;
			String nombre;
			String capacidad;
			while(rs.next())
			{
				idtipo_liquido = rs.getInt("idtipo_liquido");
				nombre= rs.getString("nombre");
				capacidad = rs.getString("capacidad");
				TipoLiquido tipliq = new TipoLiquido(idtipo_liquido,nombre, capacidad);
				tiposLiquido.add(tipliq);
			}
			rs.close();
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
		
		return(tiposLiquido);
	}
	
	/**
	 * M�todo que permite la consulta de pedidos de acuerdo a los par�metros enviados para la consulta, esta consulta es exclusiva para los 
	 * productos que son registrados dentro del sistema contact center.
	 * @param fechainicial Fecha inicial de los pedidos a consultar.
	 * @param fechafinal Fecha final de los pedidos a consultar.
	 * @param tienda nombre de la tienda que se desea filtrar para la consulta de los pedidos.
	 * @param numeropedido En caso de desearlo se puede filtrar por un n�mero de pedido en espec�fico.
	 * @return Se retorna un ArrayList con objetos de tipo pedido con la informaci�n de los pedidos consultados.
	 */
	public static ArrayList<Pedido> ConsultaIntegradaPedidos(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2);	
		//Agregamos la consulta base
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion, a.programado, a.hora_programado, a.idtipopedido, a.usuarioreenvio from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "'";//Revisaremos si es necesario agregar el filtro por tiendas
		if((tienda.length()> 0) && !(tienda.equals("TODAS")))
		{
			idtienda = TiendaDAO.obteneridTienda(tienda);
			consulta = consulta + " and a.idtienda =" + idtienda;
		}
		//Realizamos al validaci�n si se va a filtrar por n�mero de pedido
		if(numeropedido != 0)
		{
			consulta = consulta + " and a.numposheader = " + numeropedido ;
		}
		//Validamos si se incluye filtro por estado pedido
		if(idEstadoPedido != 0)
		{
			consulta = consulta +  " and a.idestadopedido = " + idEstadoPedido;
		}
		//Validamos el estado de envio a la tienda en los filtros
		if(enviadoPixel != -1)
		{
			consulta = consulta + " and enviadopixel = " + enviadoPixel;
		}
		logger.info(consulta);
		System.out.println(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			String programado;
			String horaProgramado;
			int idTipoPedido;
			String usuarioReenvio;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				programado = rs.getString("programado");
				horaProgramado = rs.getString("hora_programado");
				idTipoPedido = rs.getInt("idtipopedido");
				usuarioReenvio = rs.getString("usuarioreenvio");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				if(programado == null)
				{
					programado = "";
				}
				if(horaProgramado == null)
				{
					horaProgramado = "";
				}
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);
				cadaPedido.setProgramado(programado);
				cadaPedido.setHoraProgramado(horaProgramado);
				cadaPedido.setIdTipoPedido(idTipoPedido);
				cadaPedido.setUsuarioReenvio(usuarioReenvio);
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	/**
	 * M�todo que har� la recuperaci�n de un pedido dado un n�mero de pedido y hara el filtro con la fecha sistema, ser� una
	 * b�squeda parecida a la de consulta integrada pero con el retorno de un solo pedido.
	 * @param numeropedido
	 * @return
	 */
	public static Pedido ConsultaPedido( int numeropedido)
	{
		Logger logger = Logger.getLogger("log_file");
		Pedido consultaPedido = new Pedido();
		consultaPedido.setIdpedido(0);
		String consulta = "";
		//Agregamos la consulta base
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido = CURDATE() and a.idpedido = " + numeropedido ;
		System.out.println(consulta);
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			int idtienda;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				idtienda = rs.getInt("idtienda");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				consultaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);

			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedido);
	}
	
	public static Pedido ConsultaPedidoXOrden( BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		Pedido consultaPedido = new Pedido();
		consultaPedido.setIdpedido(0);
		String consulta = "";
		//Agregamos la consulta base
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.idordencomercio = " + idOrdenComercio ;
		System.out.println(consulta);
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			int idtienda;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				idtienda = rs.getInt("idtienda");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				consultaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);

			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedido);
	}
	
	public static ArrayList<Pedido> ConsultaIntegradaPedidosTienda(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2);	
		//Agregamos la consulta base
		consulta = "select a.idpedido, a.idordencomercio, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.origen = 'T' and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "'";//Revisaremos si es necesario agregar el filtro por tiendas
		if((tienda.length()> 0) && !(tienda.equals("TODAS")))
		{
			idtienda = TiendaDAO.obteneridTienda(tienda);
			consulta = consulta + " and a.idtienda =" + idtienda;
		}
		//Realizamos al validaci�n si se va a filtrar por n�mero de pedido
		if(numeropedido != 0)
		{
			consulta = consulta + " and a.numposheader = " + numeropedido ;
		}
		//Validamos si se incluye filtro por estado pedido
		if(idEstadoPedido != 0)
		{
			consulta = consulta +  " and a.idestadopedido = " + idEstadoPedido;
		}
		//Validamos el estado de envio a la tienda en los filtros
		if(enviadoPixel != -1)
		{
			consulta = consulta + " and enviadopixel = " + enviadoPixel;
		}
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			long idOrdenComercio;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				idOrdenComercio = rs.getLong("idordencomercio");
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);
				cadaPedido.setIdOrdenComercio(idOrdenComercio);
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	public static ArrayList<Pedido> ConsultaIntegradaPedidosNuevaTienda(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel, String origenPedido)
	{
		Logger logger = Logger.getLogger("log_file"); 
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2);	
		//Agregamos la consulta base
		if (origenPedido.equals(new String("TK")))
		{
			consulta = "select a.idpedido, a.idordencomercio, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion, b.grupo_virtual, a.programado, a.hora_programado, a.idtipopedido, a.usuarioreenvio from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.origen = 'TK' and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "'";//Revisaremos si es necesario agregar el filtro por tiendas
		}else if(origenPedido.equals(new String("APP")))
		{
			consulta = "select a.idpedido, a.idordencomercio, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion, b.grupo_virtual, a.programado, a.hora_programado, a.idtipopedido, a.usuarioreenvio from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.origen = 'APP' and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "'";//Revisaremos si es necesario agregar el filtro por tiendas
		}else if(origenPedido.equals(new String("CRM")))
		{
			consulta = "select a.idpedido, a.idordencomercio, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion, b.grupo_virtual, a.programado, a.hora_programado, a.idtipopedido, a.usuarioreenvio from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.origen = 'CRM' and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "'";//Revisaremos si es necesario agregar el filtro por tiendas
		}
		if((tienda.length()> 0) && !(tienda.equals("TODAS")))
		{
			idtienda = TiendaDAO.obteneridTienda(tienda);
			consulta = consulta + " and a.idtienda =" + idtienda;
		}
		//Realizamos al validaci�n si se va a filtrar por n�mero de pedido
		if(numeropedido != 0)
		{
			consulta = consulta + " and a.numposheader = " + numeropedido ;
		}
		//Validamos si se incluye filtro por estado pedido
		if(idEstadoPedido != 0)
		{
			consulta = consulta +  " and a.idestadopedido = " + idEstadoPedido;
		}
		//Validamos el estado de envio a la tienda en los filtros
		if(enviadoPixel != -1)
		{
			consulta = consulta + " and enviadopixel = " + enviadoPixel;
		}
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			long idOrdenComercio;
			int grupoVirtual;
			String programado;
			String horaProgramado;
			int idTipoPedido;
			String usuarioReenvio;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				programado = rs.getString("programado");
				horaProgramado = rs.getString("hora_programado");
				idTipoPedido = rs.getInt("idtipopedido");
				usuarioReenvio = rs.getString("usuarioreenvio");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				idOrdenComercio = rs.getLong("idordencomercio");
				grupoVirtual = rs.getInt("grupo_virtual");
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);
				cadaPedido.setIdOrdenComercio(idOrdenComercio);
				cadaPedido.setGrupoVirtual(grupoVirtual);
				cadaPedido.setProgramado(programado);
				cadaPedido.setHoraProgramado(horaProgramado);
				cadaPedido.setIdTipoPedido(idTipoPedido);
				cadaPedido.setUsuarioReenvio(usuarioReenvio);
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	public static ArrayList<PedidoPlat> ConsultaIntegradaPedidosDomicom(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel, int idPlataforma, int integracion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <PedidoPlat> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2);	
		//Agregamos la consulta base
		if(idPlataforma == 0)
		{
			consulta = "select a.idpedido, a.idordencomercio, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion, a.aceptado_rappi from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f, marcacion_pedido g where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.idpedido = g.idpedido and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "'";//Revisaremos si es necesario agregar el filtro por tiendas
			
		}else
		{
			consulta = "select a.idpedido, a.idordencomercio, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion, a.aceptado_rappi from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f, marcacion_pedido g where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.idpedido = g.idpedido and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "' and g.idmarcacion = " + idPlataforma;//Revisaremos si es necesario agregar el filtro por tiendas
			
		}
		if((tienda.length()> 0) && !(tienda.equals("TODAS")))
		{
			idtienda = TiendaDAO.obteneridTienda(tienda);
			consulta = consulta + " and a.idtienda =" + idtienda;
		}
		if(integracion == 1)
		{
			consulta = consulta + " and a.idordencomercio  > 0";
		}
		//Realizamos al validaci�n si se va a filtrar por n�mero de pedido
		if(numeropedido != 0)
		{
			consulta = consulta + " and a.numposheader = " + numeropedido ;
		}
		//Validamos si se incluye filtro por estado pedido
		if(idEstadoPedido != 0)
		{
			consulta = consulta +  " and a.idestadopedido = " + idEstadoPedido;
		}
		//Validamos el estado de envio a la tienda en los filtros
		if(enviadoPixel != -1)
		{
			consulta = consulta + " and enviadopixel = " + enviadoPixel;
		}
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			BigInteger idOrdenComercio;
			int aceptadoRappi;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				String strIdOrdenComercio = rs.getString("idordencomercio");
				idOrdenComercio = new BigInteger(strIdOrdenComercio);
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				PedidoPlat cadaPedido = new PedidoPlat(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);
				cadaPedido.setIdOrdenComercio(idOrdenComercio);
				aceptadoRappi = rs.getInt("aceptado_rappi");
				cadaPedido.setAceptadoRappi(aceptadoRappi);
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	public static ArrayList<Pedido> ConsultaIntegradaPedidosEnCurso(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2);	
		//Agregamos la consulta base
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion from pedido a, tienda b, cliente c, estado_pedido d where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and a.fechapedido >=  '" + fechaini +"' and a.fechapedido <= '"+ fechafin + "' and a.idestadopedido = 1 and enviadopixel = 0";//Revisaremos si es necesario agregar el filtro por tiendas
		if((tienda.length()> 0) && !(tienda.equals("TODAS")))
		{
			idtienda = TiendaDAO.obteneridTienda(tienda);
			consulta = consulta + " and a.idtienda =" + idtienda;
		}
		//Realizamos al validaci�n si se va a filtrar por n�mero de pedido
		if(numeropedido != 0)
		{
			consulta = consulta + " and a.numposheader = " + numeropedido ;
		}
		//Validamos si se incluye filtro por estado pedido
		if(idEstadoPedido != 0)
		{
			consulta = consulta +  " and a.idestadopedido = " + idEstadoPedido;
		}
		//Validamos el estado de envio a la tienda en los filtros
		if(enviadoPixel != -1)
		{
			consulta = consulta + " and enviadopixel = " + enviadoPixel;
		}
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = "";
				idformapago = 0;
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	/**
	 * M�todo que se encarga de eliminar un detalle pedido de acuerdo al par�metro recibido.
	 * @param iddetallepedido Se recibe como par�metro el iddetallepedido que se desea eliminar
	 * @return Se retorna un ArrayList de n�meros enteros qeu responden a los iddetallepedido eliminados los cuales 
	 * pueden ser varios debido a que si eliminamos un detalle pedido que tenga asociadas adiciones, tambien
	 * deberemos de eliminar las adiciones.
	 */
	public static ArrayList<Integer> EliminarDetallePedido(int iddetallepedido)
	{
		ArrayList<Integer> idDetallePedidosBorrados = new ArrayList();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta;
		try
		{
			// Debemos de validar que el item del pedido tenga o no adiciones, si tiene adiciones deber� de eliminar los dealles pedido
			//asociado a las adiciones del producto principal
			String consulta = "select iddetallepedidoadicion from adicion_detalle_pedido where iddetallepedidopadre = " + iddetallepedido;
			logger.info(consulta);
			Statement stm = con1.createStatement();
			Statement stm1 = con1.createStatement();
			Statement stm2 = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			String delete = "";
			String consultaModDetPed = "";
			while (rs.next())
			{
				//debemos borrar tambien el detalle de la adici�n
				int valorEliminar = Integer.parseInt( rs.getString("iddetallepedidoadicion"));
				delete = "delete from adicion_detalle_pedido  where iddetallepedidoadicion = " + valorEliminar;
				logger.info(delete);
				stm1.executeUpdate(delete);
				//borramos el detalle pedido asociado a ada adici�n
				delete = "delete from detalle_pedido  where iddetalle_pedido = " + valorEliminar;
				logger.info(delete);
				stm1.executeUpdate(delete);
				idDetallePedidosBorrados.add(Integer.valueOf(valorEliminar));
			}
			//borrado en detalle de adiciones en caso de que sea una adicion, en caso que solo estemos eliminando una adici�n
			delete = "delete from adicion_detalle_pedido  where iddetallepedidoadicion = " + iddetallepedido; 
			logger.info(delete);
			stm1.executeUpdate(delete);
			//borrado de la tabla de modificadores sacamos los modificadores que tienen detalle pedido porqeu tienen precio
			consultaModDetPed = "select iddetallepedidoasociado from modificador_detalle_pedido  where iddetallepedidopadre = " + iddetallepedido + " and iddetallepedidoasociado <> 0";
			logger.info(consultaModDetPed);
			rs = stm2.executeQuery(consultaModDetPed);
			while (rs.next())
			{
				//debemos borrar tambien el detalle de la adici�n
				int valorEliminarMod = Integer.parseInt( rs.getString("iddetallepedidoasociado"));
				delete = "delete from detalle_pedido  where iddetalle_pedido = " + valorEliminarMod;
				logger.info(delete);
				stm1.executeUpdate(delete);
				idDetallePedidosBorrados.add(Integer.valueOf(valorEliminarMod));
			}
			delete = "delete from modificador_detalle_pedido  where iddetallepedidopadre = " + iddetallepedido; 
			logger.info(delete);
			stm1.executeUpdate(delete);
			// Se hace la eliminaci�n final del detalle pedido pasado como parametro.
			delete = "delete from detalle_pedido  where iddetalle_pedido = " + iddetallepedido; 
			logger.info(delete);
			idDetallePedidosBorrados.add(Integer.valueOf(iddetallepedido));
			stm1.executeUpdate(delete);
			stm.close();
			stm1.close();
			stm2.close();
			con1.close();
			//Debemos de ejecutar una reconstrucci�n del campo adici�n del ippedido
			
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
		return(idDetallePedidosBorrados);
		
	}
	

	/**
	 * M�todo que se encarga de dado un iddetallepedido, retornar todas las adiciones asociados a dicho Item.
	 * @param iddetallepedido Se recibe como par�metro el iddetallepedido del cual se requiere saber las adiciones.
	 * @return Se retorna un ArrayList con objetos Modelo DetallePedidoAdicion asociados al iddetallepedido pasado
	 * como par�metro.
	 */
	public static ArrayList<DetallePedidoAdicion> ObtenerAdicionDetallePedido (int iddetallepedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<DetallePedidoAdicion> consultaAdiciones = new ArrayList();
		String consulta = "select a.iddetallepedidopadre, a.iddetallepedidoadicion, a.idespecialidad1, a.cantidad1, a.idespecialidad2, "
				+ "a.cantidad2, b.idproducto from adicion_detalle_pedido a, detalle_pedido b where a.iddetallepedidoadicion = b.iddetalle_pedido and a.iddetallepedidopadre =" + iddetallepedido; 
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int iddetallepedidopadre;
			int iddetallepedidoadicion;
			int idespecialidad1;
			double cantidad1;
			int idespecialidad2;
			double cantidad2;
			int idproducto;
			while(rs.next())
			{
				iddetallepedidopadre = Integer.parseInt(rs.getString("iddetallepedidopadre"));
				iddetallepedidoadicion = Integer.parseInt(rs.getString("iddetallepedidoadicion"));
				idespecialidad1 = Integer.parseInt(rs.getString("idespecialidad1"));
				cantidad1 = Double.parseDouble(rs.getString("cantidad1"));
				idespecialidad2 = Integer.parseInt(rs.getString("idespecialidad2"));
				cantidad2 = Double.parseDouble(rs.getString("cantidad2"));
				idproducto = Integer.parseInt(rs.getString("idproducto"));
				DetallePedidoAdicion detAdicion = new DetallePedidoAdicion(iddetallepedidopadre, iddetallepedidoadicion, idespecialidad1, idespecialidad2, cantidad1, cantidad2);
				detAdicion.setIdproducto(idproducto);
				consultaAdiciones.add(detAdicion);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(consultaAdiciones);
	}
	
	/**
	 * M�todo que se encarga de retornar todos los modificadores asociados a un iddetallepedido, enviado como par�metro.
	 * @param iddetallepedido Se recibe como par�metro un iddetallepedido con base en el cual se retornar�n todos los
	 * modificadores CON/SIN asociados al detallepedido.
	 * @return Se retorna un ArrayList con objetos tipo ModificadorDetallePedido asociados al iddetallepedido
	 * pasado como par�metro.
	 */
	public static ArrayList<ModificadorDetallePedido> ObtenerModificadorDetallePedido (int iddetallepedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ModificadorDetallePedido> consultaModificadores = new ArrayList();
		String consulta = "select a.iddetallepedidopadre,  a.idproductoespecialidad1, a.idproductoespecialidad2, "
				+ "a.cantidad, a.iddetallepedidoasociado from modificador_detalle_pedido a where a.iddetallepedidopadre =" + iddetallepedido; 
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int iddetallepedidopadre;
			int idproductoespecialidad1;
			int idproductoespecialidad2;
			int iddetallepedidoasociado;
			double cantidad;
			while(rs.next())
			{
				iddetallepedidopadre = Integer.parseInt(rs.getString("iddetallepedidopadre"));
				idproductoespecialidad1 = Integer.parseInt(rs.getString("idproductoespecialidad1"));
				idproductoespecialidad2 = Integer.parseInt(rs.getString("idproductoespecialidad2"));
				cantidad = Double.parseDouble(rs.getString("cantidad"));
				iddetallepedidoasociado = Integer.parseInt(rs.getString("iddetallepedidoasociado"));
				ModificadorDetallePedido detModificador = new ModificadorDetallePedido(0,iddetallepedidopadre,idproductoespecialidad1, idproductoespecialidad2, cantidad, iddetallepedidoasociado);
				consultaModificadores.add(detModificador);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(consultaModificadores);
	}
	
	/**
	 * M�todo que se encarga de retornar el detalle de un pedido, con base en el id pedido recibido como par�metro
	 * @param numeropedido Se recibe como par�metro del idpedido con base en el cual se retornar� la informaci�n.
	 * @return Se retorna un ArrayList con objetos tipo DetallePedido de acuerdo alos par�metros recibidos.
	 */
	public static ArrayList<DetallePedido> ConsultarDetallePedido(int numeropedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <DetallePedido> consultaDetallePedidos = new ArrayList();
		String consulta = "select a.iddetalle_pedido, a.idproducto, b.nombre, a.cantidad, a.idespecialidad1, a.idespecialidad2, concat(c.nombre , a.modespecialidad1) especialidad1, concat(d.nombre , a.modespecialidad2) especialidad2, a.modespecialidad1, a.modespecialidad2, "
				+ "a.valorUnitario, a.valorTotal, a.adicion, a.observacion, e.descripcion liquido , f.descripcion excepcion, a.idexcepcion, a.idsabortipoliquido  from detalle_pedido a left outer join especialidad "
				+ "c on a.idespecialidad1 = c.idespecialidad left outer join especialidad d on a.idespecialidad2 = d.idespecialidad"
				+ " left outer join sabor_x_tipo_liquido e on a.idsabortipoliquido = e.idsabor_x_tipo_liquido "
				+ "left outer join excepcion_precio f on a.idexcepcion = f.idexcepcion"
				+ ",producto b where a.idproducto = b.idproducto and idpedido = " + numeropedido;
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int iddetallepedido;
			int idproducto;
			String nombreproducto;
			double cantidad;
			String especialidad1;
			String modEspecialidad1;
			int idespecialidad1;
			String especialidad2;
			String modEspecialidad2;
			int idespecialidad2;
			double valorunitario;
			double valortotal;
			String adicion;
			String observacion;
			String liquido;
			String excepcion;
			int idexcepcion;
			int idsabortipoliquido;
			while(rs.next())
			{
				iddetallepedido = rs.getInt("iddetalle_pedido");
				idproducto = rs.getInt("idproducto");
				nombreproducto = rs.getString("nombre");
				cantidad = rs.getDouble("cantidad");
				especialidad1 = rs.getString("especialidad1");
				idespecialidad1 = rs.getInt("idespecialidad1");
				especialidad2 = rs.getString("especialidad2");
				idespecialidad2 = rs.getInt("idespecialidad2");
				valorunitario = rs.getDouble("valorUnitario");
				valortotal = rs.getDouble("valorTotal");
				adicion = rs.getString("adicion");
				observacion = rs.getString("observacion");
				liquido = rs.getString("liquido");
				excepcion = rs.getString("excepcion");
				idexcepcion = rs.getInt("idexcepcion");
				idsabortipoliquido = rs.getInt("idsabortipoliquido");
				modEspecialidad1 = rs.getString("modespecialidad1");
				modEspecialidad2 = rs.getString("modespecialidad2");
				DetallePedido cadaDetallePedido = new DetallePedido(iddetallepedido, nombreproducto, idproducto, cantidad,especialidad1, idespecialidad1, especialidad2, idespecialidad2,valorunitario, valortotal,adicion,observacion,liquido, excepcion, numeropedido, idexcepcion, idsabortipoliquido,"");
				cadaDetallePedido.setModespecialidad1(modEspecialidad1);
				cadaDetallePedido.setModespecialidad2(modEspecialidad2);
				consultaDetallePedidos.add(cadaDetallePedido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
			
		}
		return(consultaDetallePedidos);
	}

	
	/**
	 * M�todo que recibe como par�metro un id pedido y con base en el retorna todos lo detallespedidos asociados, sin 
	 * incluir las adiciones.
	 * @param numeropedido Se recibe como par�metro un n�mero de pedido con base en el cual se realiza la consulta.
	 * @return Se retorna un ArrayList con objetos Modelo DetallePedido asociados al pedido pasado como par�metro y excluyendo
	 * las adiciones.
	 */
	public static ArrayList<DetallePedido> ConsultarDetallePedidoSinAdiciones(int numeropedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <DetallePedido> consultaDetallePedidos = new ArrayList();
		String consulta = "select a.iddetalle_pedido, a.idproducto, b.nombre, a.cantidad, a.idespecialidad1, a.idespecialidad2, c.nombre especialidad1, d.nombre especialidad2, "
				+ "a.valorUnitario, a.valorTotal, a.adicion, a.observacion, e.descripcion liquido , f.descripcion excepcion, a.idexcepcion, a.idsabortipoliquido  from detalle_pedido a left outer join especialidad "
				+ "c on a.idespecialidad1 = c.idespecialidad left outer join especialidad d on a.idespecialidad2 = d.idespecialidad"
				+ " left outer join sabor_x_tipo_liquido e on a.idsabortipoliquido = e.idsabor_x_tipo_liquido "
				+ "left outer join excepcion_precio f on a.idexcepcion = f.idexcepcion"
				+ ",producto b where a.idproducto = b.idproducto and b.tipo in ('OTROS' , 'PIZZA') and idpedido = " + numeropedido;
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int iddetallepedido;
			int idproducto;
			String nombreproducto;
			double cantidad;
			String especialidad1;
			int idespecialidad1;
			String especialidad2;
			int idespecialidad2;
			double valorunitario;
			double valortotal;
			String adicion;
			String observacion;
			String liquido;
			String excepcion;
			int idexcepcion;
			int idsabortipoliquido;
			while(rs.next())
			{
				iddetallepedido = rs.getInt("iddetalle_pedido");
				idproducto = rs.getInt("idproducto");
				nombreproducto = rs.getString("nombre");
				cantidad = rs.getDouble("cantidad");
				especialidad1 = rs.getString("especialidad1");
				idespecialidad1 = rs.getInt("idespecialidad1");
				especialidad2 = rs.getString("especialidad2");
				idespecialidad2 = rs.getInt("idespecialidad2");
				valorunitario = rs.getDouble("valorUnitario");
				valortotal = rs.getDouble("valorTotal");
				adicion = rs.getString("adicion");
				observacion = rs.getString("observacion");
				liquido = rs.getString("liquido");
				excepcion = rs.getString("excepcion");
				idexcepcion = rs.getInt("idexcepcion");
				idsabortipoliquido = rs.getInt("idsabortipoliquido");
				DetallePedido cadaDetallePedido = new DetallePedido(iddetallepedido, nombreproducto, idproducto, cantidad,especialidad1, idespecialidad1, especialidad2, idespecialidad2,valorunitario, valortotal,adicion,observacion,liquido, excepcion, numeropedido, idexcepcion, idsabortipoliquido,"");
				consultaDetallePedidos.add(cadaDetallePedido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaDetallePedidos);
	}
	
	/**
	 * M�todo que retornar dado un idpedidopadre, retorna lo detalles de pedido asociados a este, como adiciones, productos
	 * o modificadores con precio
	 * @param iddetpedidopadre se recibe como param�tro el iddetalle pedido que deber� ser un productos de la tipolog�a
	 * PIZZA u OTROS.
	 * @return
	 */
	public static ArrayList<DetallePedido> ConsultarDetallePedidoPorPadre(int iddetpedidopadre)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <DetallePedido> consultaDetallePedidos = new ArrayList();
		String consulta = "select a.iddetalle_pedido, a.idpedido, a.idproducto, b.nombre, a.cantidad, a.idespecialidad1, a.idespecialidad2, c.nombre especialidad1, d.nombre especialidad2, "
				+ "a.valorUnitario, a.valorTotal, a.adicion, a.observacion, e.descripcion liquido , f.descripcion excepcion, a.idexcepcion, a.idsabortipoliquido, b.tipo  from detalle_pedido a left outer join especialidad "
				+ "c on a.idespecialidad1 = c.idespecialidad left outer join especialidad d on a.idespecialidad2 = d.idespecialidad"
				+ " left outer join sabor_x_tipo_liquido e on a.idsabortipoliquido = e.idsabor_x_tipo_liquido "
				+ "left outer join excepcion_precio f on a.idexcepcion = f.idexcepcion"
				+ ",producto b where a.idproducto = b.idproducto and a.iddetalle_pedido in  " 
				+ " (select iddetalle_pedido from detalle_pedido where iddetalle_pedido = " + iddetpedidopadre 
				+ " union select iddetalle_pedido from detalle_pedido where observacion = '"+ "Producto Incluido-" + iddetpedidopadre +"'"
				+ " union select iddetallepedidoadicion from adicion_detalle_pedido where iddetallepedidopadre = " + iddetpedidopadre
				+ " union select iddetallepedidoasociado from modificador_detalle_pedido where iddetallepedidopadre = " + iddetpedidopadre + ")"; 
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			int iddetallepedido;
			int idproducto;
			String nombreproducto;
			double cantidad;
			String especialidad1;
			int idespecialidad1;
			String especialidad2;
			int idespecialidad2;
			double valorunitario;
			double valortotal;
			String adicion;
			String observacion;
			String liquido;
			String excepcion;
			int idexcepcion;
			int idsabortipoliquido;
			String tipoProducto;
			while(rs.next())
			{
				iddetallepedido = rs.getInt("iddetalle_pedido");
				idpedido = rs.getInt("idpedido");
				idproducto = rs.getInt("idproducto");
				nombreproducto = rs.getString("nombre");
				cantidad = rs.getDouble("cantidad");
				especialidad1 = rs.getString("especialidad1");
				idespecialidad1 = rs.getInt("idespecialidad1");
				especialidad2 = rs.getString("especialidad2");
				idespecialidad2 = rs.getInt("idespecialidad2");
				valorunitario = rs.getDouble("valorUnitario");
				valortotal = rs.getDouble("valorTotal");
				adicion = rs.getString("adicion");
				observacion = rs.getString("observacion");
				liquido = rs.getString("liquido");
				excepcion = rs.getString("excepcion");
				idexcepcion = rs.getInt("idexcepcion");
				idsabortipoliquido = rs.getInt("idsabortipoliquido");
				tipoProducto = rs.getString("tipo");
				DetallePedido cadaDetallePedido = new DetallePedido(iddetallepedido, nombreproducto, idproducto, cantidad,especialidad1, idespecialidad1, especialidad2, idespecialidad2,valorunitario, valortotal,adicion,observacion,liquido, excepcion, idpedido , idexcepcion, idsabortipoliquido,tipoProducto);
				consultaDetallePedidos.add(cadaDetallePedido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaDetallePedidos);
	}
	
	/**
	 * mPetodo que se encarga de retornar la informaci�n de una entidad Tienda, con base en la informaci�n recibida como 
	 * par�metro.
	 * @param idpedido Se recibe como par�metro el idpedido con base en el cual se retornar� la informacion de la tienda
	 * asociada al pedido.
	 * @return Se retorna un objeto de tipo Modelo Tienda con base en el idpedido recibido como par�metro.
	 */
	public static Tienda obtenerTiendaPedido(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Tienda tienda = new Tienda();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select p.idtienda from pedido p where idpedido = " + idpedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idTienda;
			while(rs.next()){
				idTienda = rs.getInt("idtienda");
				tienda = TiendaDAO.retornarTienda(idTienda);
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
		return(tienda);
		
	}
	
	/**
	 * M�todo que se encarga de retornar la forma de pago dado un id pedido.
	 * @param idPedido Se recibe como par�metro el idpedido, del cual se retorna la forma de pago
	 * @return Se retorna un objeto Modelo FormaPago asociado al pedido pasado como par�metro.
	 */
	public static FormaPago obtenerFormaPagoPedido(int idPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		FormaPago formaPago = new FormaPago();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idpedido_forma_pago, a.idforma_pago,valortotal, a.valorformapago, b.nombre, c.descuento, b.virtual from pedido_forma_pago a, forma_pago b, pedido c where a.idforma_pago = b.idforma_pago and a.idpedido = " + idPedido + " and a.idpedido = c.idpedido ";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idpedidoformapago;
			int idformapago;
			double valortotal;
			double valorformapago;
			String nombre;
			double descuento;
			String virtual;
			while(rs.next()){
				idpedidoformapago = rs.getInt("idpedido_forma_pago");
				idformapago = rs.getInt("idforma_pago");
				valortotal = rs.getDouble("valortotal");
				valorformapago = rs.getDouble("valorformapago");
				nombre = rs.getString("nombre");
				descuento = rs.getDouble("descuento");
				virtual = rs.getString("virtual");
				formaPago = new FormaPago(idformapago,nombre,"",valortotal, valorformapago);
				formaPago.setDescuento(descuento);
				formaPago.setVirtual(virtual);
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
		return(formaPago);
	}
	
	/**
	 * M�todo que recupera las marcaciones de un pedido en un ArrayList.
	 * @param idPedido
	 * @return
	 */
	public static ArrayList<MarcacionPedido> obtenerMarcacionesPedido(int idPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<MarcacionPedido> marcacionesPedido = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select b.nombre_marcacion, a.idmarcacion, a.observacion, a.descuento, a.motivo, a.marketplace, a.descuento_plataforma, a.tarifa_adicional, a.propina, a.log from marcacion_pedido a, marcacion b where a.idmarcacion = b.idmarcacion and a.idpedido = " + idPedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String nombreMarcacion;
			String observacion;
			int idMarcacion;
			double descuento, descuentoPlataforma, tarifaAdicional, propina;
			String motivo;
			String marketplace;
			String log;
			MarcacionPedido marcacionPedido;
			while(rs.next()){
				nombreMarcacion = rs.getString("nombre_marcacion");
				idMarcacion = rs.getInt("idmarcacion");
				observacion = rs.getString("observacion");
				descuento = rs.getDouble("descuento");
				descuentoPlataforma = rs.getDouble("descuento_plataforma");
				tarifaAdicional = rs.getDouble("tarifa_adicional");
				propina = rs.getDouble("propina");
				motivo = rs.getString("motivo");
				marketplace = rs.getString("marketplace");
				log = rs.getString("log");
				marcacionPedido = new MarcacionPedido(idPedido, idMarcacion, observacion, descuento, motivo, marketplace,"",descuentoPlataforma,tarifaAdicional,propina,log);
				marcacionPedido.setNombreMarcacion(nombreMarcacion);
				marcacionesPedido.add(marcacionPedido);
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
		return(marcacionesPedido);
	}
	
	/**
	 * M�todo que se encarga de retornar un ArrayList con todos los productos incluidos en el sistema, con el objetivo
	 * que en la capa de presentaci�n, se controle la adici�n de un producto que tenga productos adicionales.
	 * @return Se retorna un arrayList con los productos incluidos parametrizados en el sistema.
	 */
	public static ArrayList<ProductoIncluido> obtenerProductosIncluidos()
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<ProductoIncluido> productosIncluidos = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idproductoincluido, a.idproductopadre, a.idproductohijo, a.cantidad, b.nombre, b.preciogeneral "
					+ "from producto_incluido a , producto b "
					+ " where a.idproductohijo = b.idproducto";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idproductoincluido;
			int idproductopadre;
			int idproductohijo;
			double cantidad;
			String nombre;
			double preciogeneral;
			while(rs.next()){
				idproductoincluido = rs.getInt("idproductoincluido");
				idproductopadre = rs.getInt("idproductopadre");
				idproductohijo = rs.getInt("idproductohijo");
				cantidad= rs.getDouble("cantidad");
				nombre = rs.getString("nombre");
				preciogeneral = rs.getDouble("preciogeneral");
				productosIncluidos.add(new ProductoIncluido(idproductoincluido,idproductopadre,idproductohijo,cantidad,nombre, preciogeneral));
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
		return(productosIncluidos);
	}
	
	public static int actualizarClienteMemcode(int idCliente, int memcode)
	{
		Logger logger = Logger.getLogger("log_file");
		int idClienteActualizado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update cliente set memcode = " + memcode + "  where idcliente = " + idCliente; 
			logger.info(update);
			stm.executeUpdate(update);
			idClienteActualizado = idCliente;
			
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
		logger.info("id cliente actualizado" + idClienteActualizado);
		return(idClienteActualizado);
	}
	
	
	/**
	 * M�todo que se encarga de validar si existen pedidos registrados con el tel�fono determinado para realizar alertamiento
	 * @param telefono
	 * @return
	 */
	public static int validarTelefonoPedido(String telefono)
	{
		Logger logger = Logger.getLogger("log_file");
		int cantidadPedidos = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String consulta = "select count(*) from pedido a, cliente b where a.idcliente = b.idcliente and b.telefono = '" + telefono +  "' and a.fechapedido = CURDATE() and a.idestadopedido = 2";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				cantidadPedidos = rs.getInt(1);
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
		return(cantidadPedidos);
	}
	
	
	/**
	 * M�todo que se encarga de validar si existen pedidos registrados con el tel�fono determinado para realizar alertamiento
	 * este a diferencia del anterior tiene en cuenta que el pedido ya est� registrado es decir ya hay uno
	 * @param telefono
	 * @return
	 */
	public static int validarTelefonoPedidoRadicado(String telefono)
	{
		Logger logger = Logger.getLogger("log_file");
		int cantidadPedidos = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String consulta = "select count(*) from pedido a, cliente b where a.idcliente = b.idcliente and b.telefono = '" + telefono +  "' and a.fechapedido = CURDATE() and a.idestadopedido = 2";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				cantidadPedidos = rs.getInt(1);
				if(cantidadPedidos > 0)
				{
					cantidadPedidos = cantidadPedidos - 1;
				}	
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
		return(cantidadPedidos);
	}
	
	/**
	 * M�todo que se encarga de retornar todas las gaseosas incluidas en productos homologadas en todas las tiendas
	 * @return Se retorna un arrayList con objetos HomologaGaseosaIncluida los cuales basicamente traen las propiedades
	 * de idtienda e idsabortipoliquido definido para las homologaciones.
	 */
	public static ArrayList<HomologaGaseosaIncluida> obtenerHomologacionGaseosaIncluida()
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<HomologaGaseosaIncluida> gaseosaHomologada = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idtienda, a.idsabortipoliquidoint "
					+ "from homologacion_producto a where a.idsabortipoliquidoint  > 0  " ;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idtienda;
			int idsabortipoliquido;
			while(rs.next()){
				idtienda = rs.getInt("idtienda");
				idsabortipoliquido = rs.getInt("idsabortipoliquidoint");
				gaseosaHomologada.add(new HomologaGaseosaIncluida(idtienda, idsabortipoliquido));
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
		return(gaseosaHomologada);
	}
	
	/**
	 * M�todo que se encarga de retornar las gaseosas como producto homologada para cada tienda
	 * @return
	 */
	public static ArrayList<HomologaGaseosaIncluida> obtenerHomologacionProductoGaseosa(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<HomologaGaseosaIncluida> gaseosaHomologada = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idtienda, a.idproductoint, b.nombre, b.preciogeneral  "
					+ "from homologacion_producto a, producto b where a.idproductoint = b.idproducto and b.tipo = 'GASEOSA' and a.idtienda = " + idTienda + " and b.idproducto not in (select idproducto from producto_no_existente where idtienda = " + idTienda +  ") order by b.nombre";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idtienda;
			int idProducto;
			String nombre;
			double precioGeneral;
			while(rs.next()){
				idtienda = rs.getInt("idtienda");
				idProducto = rs.getInt("idproductoint");
				nombre = rs.getString("nombre");
				precioGeneral = Double.parseDouble(rs.getString("preciogeneral"));
				HomologaGaseosaIncluida homGas = new HomologaGaseosaIncluida(idtienda, idProducto);
				homGas.setNombre(nombre);
				homGas.setPrecioGeneral(precioGeneral);
				gaseosaHomologada.add(homGas);
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
		return(gaseosaHomologada);
	}
	
	//M�todo que buscar� traer los �ltimos pedidos de un cliente.
	public static ArrayList<Pedido> ConsultaUltimosPedidosCliente(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.idcliente = " + idCliente + " order by fechapedido desc limit 3";
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url,0, "", "", "");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, "", "", "");
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	 	/**
	 	 * M�todo que se encargar� de obtener el Texto del �ltimo pedido de un cliente.
	 	 * @param idCliente
	 	 * @return
	 	 */
		public static String[] obtenerUltimoPedidoCliente(int idCliente)
		{
			Logger logger = Logger.getLogger("log_file");
			String[] infoPedido = new String[4];
			String consulta = "";
			consulta = "select a.idpedido, a.fechapedido, e.nombre formapago, a.tiempopedido from pedido a, forma_pago e, pedido_forma_pago f WHERE e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.idcliente = "+ idCliente + " order by fechapedido desc LIMIT 1";
			logger.info(consulta);
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			
			try
			{
				Statement stm = con1.createStatement();
				ResultSet rs = stm.executeQuery(consulta);
				String idPedido = "";
				String fechaPedido = "";
				String formaPago = "";
				String tiempoPedido = "";
				while(rs.next())
				{
					idPedido = rs.getString("idpedido");
					fechaPedido = rs.getString("fechapedido");
					formaPago = rs.getString("formapago");
					tiempoPedido = rs.getString("tiempopedido");
					break;
				}
				infoPedido[0] = idPedido;
				infoPedido[1] = fechaPedido;
				infoPedido[2] = formaPago;
				infoPedido[3] = tiempoPedido;
				rs.close();
				stm.close();
				con1.close();

			}catch(Exception e){
				logger.error(e.toString());
				try
				{
					con1.close();
				}catch(Exception e1)
				{
				}
				
			}
			return(infoPedido);
		}
		
		/**
		 * Método que nos retornará la fecha del último pedido realizado por el cliente
		 * @param telefono
		 * @return
		 */
		public static String obtenerFechaUltimoPedidoCliente(String telefono)
		{
			Logger logger = Logger.getLogger("log_file");
			String fechaPedido = "";
			String consulta = "";
			consulta = "select  a.fechapedido from pedido a, cliente b WHERE a.idcliente = b.idcliente and (b.telefono = '"+telefono +"') order by fechapedido desc LIMIT 1";
			logger.info(consulta);
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			
			try
			{
				Statement stm = con1.createStatement();
				ResultSet rs = stm.executeQuery(consulta);
				while(rs.next())
				{
					fechaPedido = rs.getString("fechapedido");
					break;
				}
				rs.close();
				stm.close();
				con1.close();

			}catch(Exception e){
				logger.error(e.toString());
				try
				{
					con1.close();
				}catch(Exception e1)
				{
				}
				
			}
			return(fechaPedido);
		}
		
		
		/**
		 * Método que nos retornará la cantidad de pedidos realizados por un cliente en un lapso o cantidad de días
		 * @param telefono
		 * @return
		 */
		public static int obtenerCantidadPedidosClienteDiasAtras(String telefono, int cantidadDias)
		{
			Logger logger = Logger.getLogger("log_file");
			int cantidadPedidos = 0;
			String consulta = "";
			consulta = "select  count(*) from pedido a, cliente b WHERE a.idcliente = b.idcliente and (b.telefono = '"+telefono +"') AND a.fechapedido >= DATE_SUB(CURDATE(), INTERVAL " + cantidadDias + " DAY) AND a.numposheader > 0";
			logger.info(consulta);
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			
			try
			{
				Statement stm = con1.createStatement();
				ResultSet rs = stm.executeQuery(consulta);
				while(rs.next())
				{
					cantidadPedidos = rs.getInt(1);
					break;
				}
				rs.close();
				stm.close();
				con1.close();

			}catch(Exception e){
				logger.error(e.toString());
				try
				{
					con1.close();
				}catch(Exception e1)
				{
				}
				
			}
			return(cantidadPedidos);
		}
		
		
		/**
		 * Método para obtener la cnatidad de especialidades que ha consumido un cliente en toda su historia, mostrará el top 4.
		 * @param telefonos
		 * @return
		 */
		public static String obtenerCantidadEspecialidadesCliente(String telefono)
		{
			Logger logger = Logger.getLogger("log_file");
			String respuesta = "";
			String consulta = "";
			consulta = "SELECT COUNT(*) AS cantidad, c.nombre  FROM pedido a, detalle_pedido b, especialidad c, cliente d WHERE a.idpedido = b.idpedido  AND b.idespecialidad1  = c.idespecialidad AND a.idcliente = d.idcliente and (d.telefono = '" + telefono + "') GROUP BY c.nombre ORDER BY cantidad DESC LIMIT 4";
			logger.info(consulta);
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			
			try
			{
				Statement stm = con1.createStatement();
				ResultSet rs = stm.executeQuery(consulta);
				int posicion = 1;
				while(rs.next())
				{
					respuesta  = respuesta + posicion + "." + rs.getString(2)+" : " + rs.getString(1) + "<br>";
					posicion = posicion + 1;
				}
				rs.close();
				stm.close();
				con1.close();

			}catch(Exception e){
				logger.error(e.toString());
				try
				{
					con1.close();
				}catch(Exception e1)
				{
				}
				
			}
			return(respuesta);
		}
	
	public static ArrayList<DireccionFueraZona> ConsultarDireccionesPedido(String fechainicial, String fechafinal, String strIdMuni, String idTienda, String horaIni, String horaFin)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <DireccionFueraZona> consultaDirs = new ArrayList();
		String consulta = "";
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2) + " 00:00:00";	
		String fechafin = fechafinal.substring(6, 10)+"-"+fechafinal.substring(3, 5)+"-"+fechafinal.substring(0, 2) + " 23:59:00";	
		//Modificamos consulta para incluir el n�mero de pedidos que tiene el cliente, para realizar un control
		//Validamos si el municipio es igual a cero es porque vamos a consultar todos los municipio, sino es as�
		// es porque la consulta deber� filtrar por municipio.
		String horaInicial = fechaini.substring(0,10) + " " + horaIni +":00";
		String horaFinal = fechafin.substring(0,10) + " " + horaFin +":00";
		if((horaIni.equals(new String("")))&&(horaFin.equals(new String(""))))
		{
			if(strIdMuni.equals(new String ("TODOS")))
			{
				consulta = "select a.idpedido id, b.direccion, c.nombre municipio, b.idcliente, b.latitud, b.longitud, b.telefono, b.nombre, b.apellido, a.fechapedido fecha_ingreso, a.total_neto from pedido a, cliente b, municipio c where a.idcliente = b.idcliente and c.idmunicipio = b.idmunicipio and  a.fechapedido >= '"+ fechaini + "' and a.fechapedido <= '" + fechafin + "' and a.idtienda = " + idTienda;
			}else
			{
				int idMunicipio;
				try
				{
					idMunicipio = Integer.parseInt(strIdMuni);
				}catch(Exception e)
				{
					idMunicipio = 0;
				}
				if(idMunicipio > 0)
				{
					consulta = "select a.idpedido id, b.direccion, c.nombre municipio, b.idcliente, b.latitud, b.longitud, b.telefono, b.nombre, b.apellido, a.fechapedido fecha_ingreso, a.total_neto from pedido a, cliente b, municipio c where a.idcliente = b.idcliente and c.idmunicipio = b.idmunicipio and  a.fechapedido >= '"+ fechaini + "' and a.fechapedido <= '" + fechafin + "' and b.idmunicipio = " + idMunicipio + " and a.idtienda = " + idTienda;
				}else
				{
					consulta = "select a.idpedido id, b.direccion, c.nombre municipio, b.idcliente, b.latitud, b.longitud, b.telefono, b.nombre, b.apellido, a.fechapedido fecha_ingreso, a.total_neto from pedido a, cliente b, municipio c where a.idcliente = b.idcliente and c.idmunicipio = b.idmunicipio and  a.fechapedido >= '"+ fechaini + "' and a.fechapedido <= '" + fechafin + "' and a.idtienda = " + idTienda;
				}
			}
		}else
		{
			if(strIdMuni.equals(new String ("TODOS")))
			{
				consulta = "select a.idpedido id, b.direccion, c.nombre municipio, b.idcliente, b.latitud, b.longitud, b.telefono, b.nombre, b.apellido, a.fechapedido fecha_ingreso, a.total_neto from pedido a, cliente b, municipio c where a.idcliente = b.idcliente and c.idmunicipio = b.idmunicipio and  a.fechapedido >= '"+ fechaini + "' and a.fechapedido <= '" + fechafin + "' and a.idtienda = " + idTienda + " and fechainsercion >= '" + horaInicial + "' and fechainsercion <= '" + horaFinal +"'";
			}else
			{
				int idMunicipio;
				try
				{
					idMunicipio = Integer.parseInt(strIdMuni);
				}catch(Exception e)
				{
					idMunicipio = 0;
				}
				if(idMunicipio > 0)
				{
					consulta = "select a.idpedido id, b.direccion, c.nombre municipio, b.idcliente, b.latitud, b.longitud, b.telefono, b.nombre, b.apellido, a.fechapedido fecha_ingreso, a.total_neto from pedido a, cliente b, municipio c where a.idcliente = b.idcliente and c.idmunicipio = b.idmunicipio and  a.fechapedido >= '"+ fechaini + "' and a.fechapedido <= '" + fechafin + "' and b.idmunicipio = " + idMunicipio + " and a.idtienda = " + idTienda + " and fechainsercion >= '" + horaInicial + "' and fechainsercion <= '" + horaFinal +"'";
				}else
				{
					consulta = "select a.idpedido id, b.direccion, c.nombre municipio, b.idcliente, b.latitud, b.longitud, b.telefono, b.nombre, b.apellido, a.fechapedido fecha_ingreso, a.total_neto from pedido a, cliente b, municipio c where a.idcliente = b.idcliente and c.idmunicipio = b.idmunicipio and  a.fechapedido >= '"+ fechaini + "' and a.fechapedido <= '" + fechafin + "' and a.idtienda = " + idTienda + " and fechainsercion >= '" + horaInicial + "' and fechainsercion <= '" + horaFinal +"'";
				}
			}
		}
		
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int id = 0;
			String direccion = "";
			String municipio = "";
			int idCliente = 0;
			double latitud = 0;
			double longitud = 0;
			String telefono = "";
			String nombre = "";
			String apellido = "";
			String fechaIngreso ="";
			double valor;
			while(rs.next())
			{
				id = rs.getInt("id");
				direccion = rs.getString("direccion");
				municipio = rs.getString("municipio");
				idCliente = rs.getInt("idcliente");
				latitud = rs.getDouble("latitud");
				longitud = rs.getDouble("longitud");
				telefono = rs.getString("telefono");
				nombre = rs.getString("nombre");
				apellido = rs.getString("apellido");
				fechaIngreso = rs.getString("fecha_ingreso");
				valor = rs.getDouble("total_neto");
				//Luego de tomada la informaci�n de la cantidad de pedidos, validamos que los pedidos llevados al cliente seran 0 o 1.
				DireccionFueraZona dirFuera = new DireccionFueraZona(id, direccion, municipio, idCliente, latitud, longitud, telefono, nombre, apellido, valor);
				dirFuera.setFechaIngreso(fechaIngreso);
				consultaDirs.add(dirFuera);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaDirs);
	}
	
	
	/**
	 * M�todo que se encargar� de consultar los pedidos pendientes dada una fecha determinada, con el fin de alertar posteriormente en correo electr�nico
	 * @param fechaPed
	 * @return
	 */
	public static ArrayList<Pedido> ConsultarPedidosPendientes(String fechaPed)
	{
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido = '" + fechaPed + "' and a.idestadopedido = 2 and a.enviadopixel = 0 AND TIMESTAMPDIFF(MINUTE, a.fechainsercion, NOW()) > 5 and b.alertarpedidos = 1";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, null, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, "", "", "");
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	public static ArrayList<Pedido> ConsultarPedidosVentasCorporativas(String fechaIni, String fechaFin)
	{
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = ""; 
		String fechaInicial = fechaIni.substring(6, 10)+"-"+fechaIni.substring(3, 5)+"-"+fechaIni.substring(0, 2) + " 00:00:00";	
		String fechaFinal = fechaFin.substring(6, 10)+"-"+fechaFin.substring(3, 5)+"-"+fechaFin.substring(0, 2) + " 23:59:59";
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, c.nombrecompania from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' and  a.venta_corporativa = 'S' order by a.usuariopedido, a.fechapedido";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String telefono;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String nombreCompania;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				nombreCompania = rs.getString("nombrecompania");
				Pedido cadaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, null, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, "", "", "");
				cadaPedido.setNombreCompania(nombreCompania);
				consultaPedidos.add(cadaPedido);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	public static ArrayList<ResumenVentaEmpresarial> ConsultarResumenVentasCorporativas(String fechaIni, String fechaFin)
	{
		ArrayList <ResumenVentaEmpresarial> resumenes = new ArrayList();
		String consulta = ""; 
		String fechaInicial = fechaIni.substring(6, 10)+"-"+fechaIni.substring(3, 5)+"-"+fechaIni.substring(0, 2) + " 00:00:00";	
		String fechaFinal = fechaFin.substring(6, 10)+"-"+fechaFin.substring(3, 5)+"-"+fechaFin.substring(0, 2) + " 23:59:59";
		consulta = "select sum(a.total_neto) as ventatotal, (sum(a.total_neto)*0.03) as comision, g.nombre_largo as asesor  from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f, usuario g where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.usuariopedido = g.nombre and a.fechapedido >= '" + fechaInicial + "' and a.fechapedido <= '" + fechaFinal + "' and  a.venta_corporativa = 'S' group by asesor";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			String asesor;
			double ventaTotal;
			double comision;
			while(rs.next())
			{
				asesor = rs.getString("asesor");
				ventaTotal = rs.getDouble("ventatotal");
				comision = rs.getDouble("comision");
				ResumenVentaEmpresarial resumenTemp = new ResumenVentaEmpresarial(asesor,ventaTotal,comision);
				resumenes.add(resumenTemp);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(resumenes);
	}
	
	public static boolean seDebeReportar(int idpedido ,int maxAlertas)
	{
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean seDebeReportar = false;
		boolean existeRegistro = false;
		int vecesReportado = 0;
		try
		{
			Statement stm = con1.createStatement();
			Statement stm1 = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String select = "select veces_reportado from pedidos_pendiente where idpedido =" + idpedido;
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				vecesReportado = rs.getInt("veces_reportado");
				if(vecesReportado == maxAlertas)
				{
					seDebeReportar = false;
				}
				else
				{
					seDebeReportar = true;
					vecesReportado++;
					String update = "update pedidos_pendiente set veces_reportado = " + vecesReportado + " where idpedido = " + idpedido;
					System.out.println("update " + update);
					stm1.executeUpdate(update);
				}
				existeRegistro = true;
			}
			if(!existeRegistro)
			{
				seDebeReportar = true;
				vecesReportado = 1;
				String insert = "insert into pedidos_pendiente (idpedido, veces_reportado) values(" + idpedido + "," + vecesReportado + ")";
				System.out.println("insert " + insert);
				stm1.executeUpdate(insert);
			}
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.getMessage());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(false);
		}
		return(seDebeReportar);
	}
	
	/**
	 * M�todo que se encarga de recibir el idLink que se pago con su forma de pago y realizar la correspondiente actualizaci�n
	 * para que posteriormente los procesos autom�ticos detecten estos pagos y realicen el env�o del pedido a la tienda.
	 * @param idLink
	 * @param tipoPago
	 * @return
	 */
	public static int actualizarFechaPagoVirtual(String idLink, String tipoPago)
	{
		boolean respuesta = false;
		int idPedido = 0;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set fechapagovirtual = CURRENT_TIMESTAMP(), tipopago = '" + tipoPago + "'   where idlink = '"+ idLink + "'";
			logger.info(update);
			int idPedidoAct = 0;
			int actualizados = stm.executeUpdate(update);
			if(actualizados > 0)
			{
				respuesta = true;
				String consulta = "select idpedido from pedido where idlink = '" + idLink +"'";
				ResultSet rs = stm.executeQuery(consulta);
				while(rs.next())
				{
					idPedido = rs.getInt(1);
					break;
				}
			}
			//Preguntamos por el valor de respuesta para saber si debemos de buscar en la tabla pedido_idlink
			if(!respuesta)
			{
				//Realizamos la consulta en la tabla de links para tomar el pedido a que corresponde
				String consulta = "select idpedido from pedido_idlink where idlink = '" + idLink +"'";
				ResultSet rs = stm.executeQuery(consulta);
				while(rs.next())
				{
					idPedidoAct = rs.getInt(1);
				}
				rs.close();
				//Validamos si recuperamos algo de idPedido, si este es mayor a cero
				if(idPedidoAct > 0)
				{
					//Realizamos la actualizaci�n y validamos si hubo o no actualizaci�n para realizar la notificaci�n
					update = "update pedido set fechapagovirtual = CURRENT_TIMESTAMP(), tipopago = '" + tipoPago + "'   where idpedido = " + idPedidoAct ;
					actualizados = stm.executeUpdate(update);
					if(actualizados > 0)
					{
						respuesta = true;
						idPedido = idPedidoAct;
					}
				}
			}
			//Realizamos validaci�n de si el pedido estaba cancelado en cuyo caso deberemos de alertar
			String consultaPedido = "";
			int idEstadoPedido = 0;
			//Armamos la consulta seg�n como se pueda en la tabla pedido y como se haya realizado la actualizaci�n del pedido
			if(idPedidoAct > 0)
			{
				consultaPedido = "select idestadopedido from pedido where idpedido = " + idPedidoAct;
			}else
			{
				consultaPedido = "select idestadopedido from pedido where idlink = '" + idLink + "'";
			}
			ResultSet rs1 = stm.executeQuery(consultaPedido);
			if(rs1.next())
			{
				idEstadoPedido = rs1.getInt(1);
			}
			//Validamos el resultado final del estado pedido para realizar el alertamiento o no
			if(idEstadoPedido == 4)
			{
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOWOMPI", "CLAVECORREOWOMPI");
				ArrayList correos = new ArrayList();
				correo.setAsunto("ATENCI�N SE REGISTRO PAGO WOMPI DE PEDIDO CANCELADO");
				correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje(" Se realiz� pago con el idLink en cuesti�n " + idLink + ", sin embargo el pedido ya hab�a sido cancelado, por favor notificar al cliente de esta situaci�n y tomar una decisi�n.");
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
			}
			rs1.close();
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
		return(idPedido);
	}
	
	//Consultas elaboradas para extraer reporte semanal de WOMPI
	
	/**
	 * M�todo que me retorna total de pagos virtuales entre las fechas pasadas como par�metro
	 * @param fechaAnterior
	 * @param fechaActual
	 * @return
	 */
	public static double consultarTotalPedidosVirtualRealizados(String fechaAnterior, String fechaActual)
	{
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		consulta = "select sum(a.total_neto) from pedido a, forma_pago e, pedido_forma_pago f where e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idestadopedido = 2 and a.enviadopixel = 1 and a.fechapagovirtual IS NOT NULL and e.virtual = 'S'";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		double totalPedidos = 0;
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				totalPedidos = rs.getDouble(1);
				break;
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			totalPedidos = 0;
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(totalPedidos);
	}
	
	/**
	 * M�todo que me retorna total de pagos virtuales Epayco entre las fechas pasadas como par�metro
	 * @param fechaAnterior
	 * @param fechaActual
	 * @return
	 */
	public static double consultarTotalPedidosEpaycoRealizados(String fechaAnterior, String fechaActual)
	{
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		consulta = "select sum(a.total_neto) from pedido a, forma_pago e, pedido_forma_pago f where e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idestadopedido = 2 and a.enviadopixel = 1 and a.numposheader > 0 and e.idforma_pago = 6";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		double totalPedidos = 0;
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				totalPedidos = rs.getDouble(1);
				break;
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			totalPedidos = 0;
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(totalPedidos);
	}
	
	
	/**
	 * M�todo que trae la informaci�n de los totales vendidos de pedidos virtuales por tienda y su total de la semana
	 * @param fechaAnterior
	 * @param fechaActual
	 * @return
	 */
	public static ArrayList consultarPedidosVirtualTiendaSemana(String fechaAnterior, String fechaActual)
	{
		ArrayList totalSemanaTienda = new ArrayList();
		String consulta = "";
		consulta = "select sum(a.total_neto), b.nombre from pedido a, tienda b,  forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idestadopedido = 2 and a.enviadopixel = 1 and a.fechapagovirtual IS NOT NULL and e.virtual = 'S' group by b.nombre order by b.nombre";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				totalSemanaTienda.add(fila);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(totalSemanaTienda);
	}
	
	public static ArrayList consultarPedidosEpaycoTiendaSemana(String fechaAnterior, String fechaActual)
	{
		ArrayList totalSemanaTienda = new ArrayList();
		String consulta = "";
		consulta = "select sum(a.total_neto), b.nombre from pedido a, tienda b,  forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idestadopedido = 2 and a.enviadopixel = 1 and a.numposheader > 0  and e.idforma_pago = 6 group by b.nombre order by b.nombre";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				totalSemanaTienda.add(fila);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(totalSemanaTienda);
	}
	
	public static ArrayList consultarPedidosVirtualTiendaDiaSemana(String fechaAnterior, String fechaActual)
	{
		ArrayList totalDiaSemanaTienda = new ArrayList();
		String consulta = "";
		consulta = "select sum(a.total_neto), b.nombre, a.fechapedido from pedido a, tienda b,  forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idestadopedido = 2 and a.enviadopixel = 1 and a.fechapagovirtual IS NOT NULL and e.virtual = 'S' group by b.nombre, a.fechapedido order by b.nombre, a.fechapedido";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				totalDiaSemanaTienda.add(fila);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(totalDiaSemanaTienda);
	}
	
	public static ArrayList consultarPedidosEpaycoTiendaDiaSemana(String fechaAnterior, String fechaActual)
	{
		ArrayList totalDiaSemanaTienda = new ArrayList();
		String consulta = "";
		consulta = "select sum(a.total_neto), b.nombre, a.fechapedido from pedido a, tienda b,  forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idestadopedido = 2 and a.enviadopixel = 1 and a.numposheader > 0 and e.idforma_pago = 6 group by b.nombre, a.fechapedido order by b.nombre, a.fechapedido";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				totalDiaSemanaTienda.add(fila);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(totalDiaSemanaTienda);
	}
	
	/**
	 * M�todo que retorna los pedidos tomados por tienda para la forma de pago Wompi pago virtual
	 * @param fechaAnterior
	 * @param fechaActual
	 * @param idTienda
	 * @return
	 */
	public static ArrayList<Pedido> consultarPedidosVirtualRealizadosTienda(String fechaAnterior, String fechaActual, int idTienda)
	{
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		consulta = "select b.nombre, a.idpedido, a.numposheader ,  a.total_neto, a.fechapedido, a.tipopago from pedido a, tienda b, pedido_forma_pago f where a.idtienda = b.idtienda and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <='" + fechaActual + "' and a.fechapagovirtual IS NOT NULL  and f.idforma_pago = 4 "; 
				
		if (idTienda > 0)
		{
			consulta = consulta + " and a.idtienda = " + idTienda;
		}
		//Se realiza la uni�n de la otra consulta con la uni�n de los pagos virtuales que se realiza desde la tienda
		consulta = consulta + " UNION SELECT 'tienda' AS nombre, 9000 as idpedido, idpedidotienda AS numposheader, valor_pedido AS total_neto, DATE_FORMAT(fechapagovirtual, '%Y-%m-%d') AS fechapedido ,tipopago FROM pedido_pago_virtual_consolidado " + 
				" WHERE estado = 'PAGADO' AND DATE_FORMAT(fechapagovirtual, '%Y-%m-%d') >= '" + fechaAnterior + "' and DATE_FORMAT(fechapagovirtual, '%Y-%m-%d') <= '" + fechaActual + "'";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		Pedido pedTemp;
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			String nombreTienda = "";
			int idPedido = 0;
			int idPedidoTienda = 0;
			double totalPedido = 0;
			String fechaPedido = "";
			String tipoPago = "";
			while(rs.next())
			{
				pedTemp = new Pedido();
				nombreTienda = rs.getString("nombre");
				pedTemp.setNombretienda(nombreTienda);
				idPedido = rs.getInt("idpedido");
				pedTemp.setIdpedido(idPedido);
				idPedidoTienda = rs.getInt("numposheader");
				pedTemp.setNumposheader(idPedidoTienda);
				totalPedido = rs.getDouble("total_neto");
				pedTemp.setTotal_neto(totalPedido);
				fechaPedido = rs.getString("fechapedido");
				pedTemp.setFechapedido(fechaPedido);
				tipoPago = rs.getString("tipopago");
				pedTemp.setTipoPago(tipoPago);
				consultaPedidos.add(pedTemp);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	/**
	 * M�todo que retorna los pagos realizados para la forma de pago Epayco tienda virtual
	 * @param fechaAnterior
	 * @param fechaActual
	 * @param idTienda
	 * @return
	 */
	public static ArrayList<Pedido> consultarPedidosEpaycoRealizadosTienda(String fechaAnterior, String fechaActual, int idTienda)
	{
		ArrayList <Pedido> consultaPedidos = new ArrayList();
		int idtienda = 0;
		String consulta = "";
		consulta = "select b.nombre, a.idpedido, a.numposheader ,  a.total_neto, a.fechapedido, a.tipopago from pedido a, tienda b, pedido_forma_pago f where a.idtienda = b.idtienda and f.idpedido = a.idpedido and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <='" + fechaActual + "' and a.numposheader > 0 and f.idforma_pago = 6";
		if (idTienda > 0)
		{
			consulta = consulta + " and a.idtienda = " + idTienda;
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		Pedido pedTemp;
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			String nombreTienda = "";
			int idPedido = 0;
			int idPedidoTienda = 0;
			double totalPedido = 0;
			String fechaPedido = "";
			String tipoPago = "";
			while(rs.next())
			{
				pedTemp = new Pedido();
				nombreTienda = rs.getString("nombre");
				pedTemp.setNombretienda(nombreTienda);
				idPedido = rs.getInt("idpedido");
				pedTemp.setIdpedido(idPedido);
				idPedidoTienda = rs.getInt("numposheader");
				pedTemp.setNumposheader(idPedidoTienda);
				totalPedido = rs.getDouble("total_neto");
				pedTemp.setTotal_neto(totalPedido);
				fechaPedido = rs.getString("fechapedido");
				pedTemp.setFechapedido(fechaPedido);
				tipoPago = rs.getString("tipopago");
				pedTemp.setTipoPago(tipoPago);
				consultaPedidos.add(pedTemp);
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedidos);
	}
	
	
	/**
	 * M�todo que se encargar� de retornar el origen de un pedido determinado que nos ayudar� para otros fines.
	 * @param idpedido
	 * @return
	 */
	public static PedidoInfoAdicional obtenerOrigenPedido(int idpedido)
	{
		PedidoInfoAdicional pedInfoAdicional = new PedidoInfoAdicional("","", "",0,0);
		Logger logger = Logger.getLogger("log_file");
		String origen = "";
		String programado = "";
		String horaProgramado = "";
		double descuento = 0;
		int idTipoPedido = 1;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select origen, programado, hora_programado, descuento, idtipopedido from pedido where idpedido = " + idpedido ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				origen = rs.getString("origen");
				programado = rs.getString("programado");
				horaProgramado = rs.getString("hora_programado");
				descuento = rs.getDouble("descuento");
				idTipoPedido = rs.getInt("idtipopedido");
				pedInfoAdicional = new PedidoInfoAdicional(origen, programado, horaProgramado,descuento, idTipoPedido);
				break;
			}
			rs.close();
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
		return(pedInfoAdicional);
	}
	
	/**
	 * M�todo en la capa controladora que se encarga del cambio de un pedido en cuanto al cambio de tienda
	 * @param idTienda
	 * @param idPedido
	 * @param idCliente
	 * @return
	 */
	public static boolean realizarCambioPedido(int idTienda, int idpedido, int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos primero el pedido con la tienda correspondiente
			String update1 = "update pedido set idTienda = " +idTienda + "  where idpedido = "+ idpedido;
			logger.info(update1);
			stm.executeUpdate(update1);
			// Posteriormente realizamos la actualizaci�n del cliente
			String update2 = "update cliente set idtienda = " + idTienda +  " where idcliente = " + idCliente;
			System.out.println(update2);
			logger.info(update2);
			stm.executeUpdate(update2);
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
			return(false);
		}
		return(true);
	}
	
	public static ArrayList obtenerPedidosPlataformasTienda(int idRazon,String fechaAnterior, String fechaActual, int idMarcacion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList pedidosDomCOMTiendas = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select b.nombre, sum(a.total_neto) as total from pedido a, tienda b, razon_x_tienda e, marcacion_pedido f, pedido_forma_pago g "
					+"where a.idtienda = b.idtienda and b.idtienda = e.idtienda and e.idrazon = " + idRazon +"  and a.idpedido = f.idpedido "
					+"and f.idmarcacion = " + idMarcacion + " and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idpedido = g.idpedido group by b.nombre";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String[] resTemp = new String[2];
			while(rs.next()){
				resTemp = new String[9];
				resTemp[0] = rs.getString("nombre");
				resTemp[1] = Double.toString(rs.getDouble("total"));
				pedidosDomCOMTiendas.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de domicilios.com " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosDomCOMTiendas);
	}
	
	
	public static ArrayList obtenerPedidosPlataformasTiendaFull(int idRazon,String fechaAnterior, String fechaActual, int idMarcacion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList pedidosDomCOMTiendas = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select b.idtienda, b.nombre, sum(a.total_neto) + sum(f.descuento) as total, sum(f.descuento) as descuento from pedido a, tienda b, razon_x_tienda e, marcacion_pedido f, pedido_forma_pago g "
					+"where a.idtienda = b.idtienda and b.idtienda = e.idtienda and e.idrazon = " + idRazon +"  and a.idpedido = f.idpedido "
					+"and f.idmarcacion = " + idMarcacion + " and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idpedido = g.idpedido group by b.idtienda, b.nombre";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String[] resTemp = new String[4];
			while(rs.next()){
				resTemp = new String[4];
				resTemp[0] = rs.getString("idtienda");
				resTemp[1] = rs.getString("nombre");
				resTemp[2] = Double.toString(rs.getDouble("total"));
				resTemp[3] = Double.toString(rs.getDouble("descuento"));
				pedidosDomCOMTiendas.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de domicilios.com " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosDomCOMTiendas);
	}
	
	
	/**
	 * Metodo que retorna los pedidos de plataforma, con su total correspondiente, descuento y marcaci�n de marketplace
	 * @param idRazon
	 * @param fechaAnterior
	 * @param fechaActual
	 * @param idMarcacion
	 * @param idTienda
	 * @return
	 */
	public static ArrayList obtenerPedidosPlataformasTiendaDetallada(int idRazon,String fechaAnterior, String fechaActual, int idMarcacion, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList pedidosDomCOMTiendas = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.total_neto , f.descuento, f.marketplace, f.descuento_asumido, f.descuento_plataforma, f.tarifa_adicional, f.propina from pedido a, tienda b, razon_x_tienda e, marcacion_pedido f, pedido_forma_pago g "
					+"where a.idtienda = b.idtienda and b.idtienda = e.idtienda and e.idrazon = " + idRazon +"  and a.idpedido = f.idpedido "
					+"and f.idmarcacion = " + idMarcacion + " and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idpedido = g.idpedido and a.idtienda = " + idTienda;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String[] resTemp = new String[7];
			while(rs.next()){
				resTemp = new String[7];
				resTemp[0] = rs.getString("total_neto");
				resTemp[1] = rs.getString("descuento");
				resTemp[2] = rs.getString("marketplace");
				resTemp[3] = rs.getString("descuento_asumido");
				resTemp[4] = rs.getString("descuento_plataforma");
				resTemp[5] = rs.getString("tarifa_adicional");
				resTemp[6] = rs.getString("propina");
				pedidosDomCOMTiendas.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de plataformas " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosDomCOMTiendas);
	}
	
	public static ArrayList obtenerPedidosPlataformas(int idRazon,String fechaAnterior, String fechaActual, int idMarcacion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList pedidosDomCOM = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select b.nombre, a.total_neto, a.fechapedido, a.idpedido, a.numposheader, f.observacion, g.idforma_pago, f.descuento, f.motivo from pedido a, tienda b, razon_x_tienda e, marcacion_pedido f, pedido_forma_pago g "
					+"where a.idtienda = b.idtienda and b.idtienda = e.idtienda and e.idrazon = " + idRazon +"  and a.idpedido = f.idpedido "
					+"and f.idmarcacion = " + idMarcacion + " and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idpedido = g.idpedido order by b.nombre, fechapedido";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String[] resTemp = new String[9];
			while(rs.next()){
				resTemp = new String[9];
				resTemp[0] = rs.getString("nombre");
				resTemp[1] = rs.getString("total_neto");
				resTemp[2] = rs.getString("fechapedido");
				resTemp[3] = rs.getString("idpedido");
				resTemp[4] = rs.getString("numposheader");
				resTemp[5] = rs.getString("observacion");
				resTemp[6] = rs.getString("idforma_pago");
				resTemp[7] = rs.getString("descuento");
				resTemp[8] = rs.getString("motivo");
				pedidosDomCOM.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de domicilios.com " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosDomCOM);
	}
	
	/**
	 * M�todo que nos retorna en un rango de fecha el total de pedidos con forma de pago ONLINE para resumir por semana
	 * @param idRazon
	 * @param fechaAnterior
	 * @param fechaActual
	 * @return
	 */
	public static ArrayList obtenerPedidosPlataformasONLINETienda(int idRazon,String fechaAnterior, String fechaActual, int idMarcacion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList pedidosDomCOMTiendas = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select b.nombre, sum(a.total_neto) as total, 'ONLINE' forma, b.idtienda  from pedido a, tienda b, razon_x_tienda e, marcacion_pedido f, pedido_forma_pago g "
					+"where a.idtienda = b.idtienda and b.idtienda = e.idtienda and e.idrazon = " + idRazon +"  and a.idpedido = f.idpedido "
					+"and f.idmarcacion = " + idMarcacion + " and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idpedido = g.idpedido and g.idforma_pago = 3 group by b.nombre, b.idtienda";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String[] resTemp = new String[4];
			while(rs.next()){
				resTemp = new String[4];
				resTemp[0] = rs.getString("nombre");
				resTemp[1] = Double.toString(rs.getDouble("total"));
				resTemp[2] =  rs.getString("forma");
				resTemp[3] =  rs.getString("idtienda");
				pedidosDomCOMTiendas.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de domicilios.com " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosDomCOMTiendas);
	}
	
	
	/**
	 * M�todo que permite obtener los descuentos por tienda en un rango de fechas
	 * @param idRazon
	 * @param fechaAnterior
	 * @param fechaActual
	 * @return
	 */
	public static ArrayList obtenerDescuentosPlataformasTienda(int idRazon,String fechaAnterior, String fechaActual, int idMarcacion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList pedidosDomCOM = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select b.nombre, sum(f.descuento) as descuento from pedido a, tienda b, razon_x_tienda e, marcacion_pedido f, pedido_forma_pago g "
					+"where a.idtienda = b.idtienda and b.idtienda = e.idtienda and e.idrazon = " + idRazon +"  and a.idpedido = f.idpedido "
					+"and f.idmarcacion = " + idMarcacion + " and a.fechapedido >= '" + fechaAnterior + "' and a.fechapedido <= '" + fechaActual + "' and a.idpedido = g.idpedido group by b.nombre";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String[] resTemp = new String[2];
			while(rs.next()){
				resTemp = new String[2];
				resTemp[0] = rs.getString("nombre");
				resTemp[1] = Double.toString(rs.getDouble("descuento"));
				pedidosDomCOM.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de domicilios.com " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosDomCOM);
	}
	
	public static void actualizarProgramadoFinalizado(long idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		int idClienteActualizado = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Para actualizar el cliente el idcliente debe ser diferente de vac�o.
			Statement stm = con1.createStatement();
			String update = "update pedido set idestadopedido = 2  where idordencomercio = " + idOrdenComercio; 
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
	
	public static ArrayList obtenerPedidosProgramadosTienda(int idTienda)
	{
		ArrayList pedidosProgramados = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idpedido, a.total_neto, a.hora_programado, a.numposheader from pedido a "
					+"where a.idtienda = " + idTienda 
					+" and a.fechapedido = CURDATE() and a.programado = 'S' AND HOUR(NOW()) <= (CAST(SUBSTR(a.hora_programado, 1,2) AS UNSIGNED)) and a.numposheader > 0";
			ResultSet rs = stm.executeQuery(consulta);
			System.out.println(consulta);
			String[] resTemp = new String[9];
			while(rs.next()){
				resTemp = new String[4];
				resTemp[0] = rs.getString("idpedido");
				resTemp[1] = rs.getString("numposheader");
				resTemp[2] = rs.getString("total_neto");
				resTemp[3] = rs.getString("hora_programado");
				pedidosProgramados.add(resTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			System.out.println("falle lanzando la consulta de pedidos posfechados");
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosProgramados);
	}
	
	public static ArrayList<Estadistica> obtenerEstadisticasPlataforma(String fechaInicial, String fechaFinal)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Estadistica> estadisticas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "";
			consulta = "SELECT a.origen AS fuente, COUNT(*) AS cantidad, SUM(a.total_neto) AS total FROM pedido a WHERE a.fechapedido >= '"+fechaInicial+"' AND a.fechapedido <= '"+fechaFinal+"' AND a.idpedido NOT IN(SELECT idpedido FROM marcacion_pedido) GROUP BY a.origen "
					+ " union "
					+ " SELECT d.nombre_marcacion AS fuente, COUNT(*) AS cantidad, SUM(b.total_neto) AS total  FROM pedido b , marcacion_pedido c, marcacion d WHERE b.idpedido = c.idpedido AND c.idmarcacion = d.idmarcacion AND b.fechapedido >= '"+fechaInicial+"' AND b.fechapedido <= '"+fechaFinal+"' GROUP BY d.nombre_marcacion";
			logger.info(consulta);
			System.out.println(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String estadistica;
			long cantidad;
			double total;
			Estadistica est;
			while(rs.next()){
				estadistica = rs.getString("fuente");
				cantidad = rs.getLong("cantidad");
				total = rs.getDouble("total");
				est = new Estadistica(estadistica,cantidad,total);
				estadisticas.add(est);
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
		return(estadisticas);
		
	}
	
	
	public static boolean consultarPedidosPendientesRAPPI(String fechaPed)
	{
		boolean respuesta = false;
		String consulta = "";
		//consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, f.valorformapago, a.descuento, c.memcode, a.idtienda, a.origen from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido = '" + fechaPed + "' and a.idestadopedido = 2 and a.origen IN ('C','TK') and a.enviadopixel = 0 AND TIMESTAMPDIFF(MINUTE, a.fechainsercion, NOW()) > 5 and b.alertarpedidos = 1";
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, f.valorformapago, a.descuento, c.memcode, a.idtienda, a.hora_programado, a.origen from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido = '" + fechaPed + "' and a.idestadopedido = 2 and a.origen = 'RAP' and a.enviadopixel = 0 AND TIMESTAMPDIFF(MINUTE, a.fechainsercion, NOW()) > 2";
		ConexionBaseDatos con = new ConexionBaseDatos();
		//Llamamos metodo de conexi�n asumiendo que corremos en el servidor de aplicaciones de manera local
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			
			while(rs.next())
			{
				respuesta = true;
				break;
			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(respuesta);
	}
	
	public static ArrayList obtenerCorreosEnvioMasivoCorreo()
	{
		ArrayList correos = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String correo;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "SELECT DISTINCT(b.email) as correo FROM pedido a, cliente b WHERE a.idcliente = b.idcliente AND b.politica_datos = 'S' AND (b.email != '' OR b.email IS NOT NULL) AND a.fechapedido >= '2023-01-01';"; 
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				correo = rs.getString(1);
				correos.add(correo);
				}
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(correos);
	}
	
	/**
	 * M�todo que nos permitir� consultar un pedido por telefono y fecha
	 * @param telefono
	 * @param fecha
	 * @return
	 */
	public static Pedido ConsultaPedido(String telefono)
	{
		Logger logger = Logger.getLogger("log_file");
		Pedido consultaPedido = new Pedido();
		consultaPedido.setIdpedido(0);
		String consulta = "";
		//Agregamos la consulta base
		consulta = "select a.idpedido, b.nombre, a.total_bruto, a.impuesto, a.total_neto, concat (c.nombre , '-' , c.apellido) nombrecliente, c.direccion, c.telefono, d.descripcion, a.fechapedido, c.idcliente, a.enviadopixel, a.numposheader, b.idtienda, b.url, a.stringpixel, a.fechainsercion, a.usuariopedido, e.nombre formapago, e.idforma_pago, a.tiempopedido, a.idlink, a.fechapagovirtual, a.fechafinalizacion from pedido a, tienda b, cliente c, estado_pedido d, forma_pago e, pedido_forma_pago f where a.idtienda = b.idtienda and a.idcliente = c.idcliente and a.idestadopedido = d.idestadopedido and e.idforma_pago = f.idforma_pago and f.idpedido = a.idpedido and a.fechapedido = CURDATE() and (c.telefono = '" + telefono +"' or c.telefono_celular = '" + telefono + "')";
		System.out.println(consulta);
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idpedido;
			int idtienda;
			String nombreTienda;
			double totalBruto;
			double impuesto;
			double totalNeto;
			String nombreCliente;
			String estadoPedido;
			String fechaPedido;
			int idcliente;
			int enviadopixel;
			int numposheader;
			String url;
			String stringpixel;
			String fechainsercion;
			String usuariopedido;
			String direccion;
			String formapago;
			int idformapago;
			double tiempopedido;
			String idLink;
			String fechaPagoVirtual;
			String fechaFinalizacion;
			while(rs.next())
			{
				idpedido = rs.getInt("idpedido");
				idtienda = rs.getInt("idtienda");
				nombreTienda = rs.getString("nombre");
				totalBruto = rs.getDouble("total_bruto");
				impuesto = rs.getDouble("impuesto");
				totalNeto = rs.getDouble("total_neto");
				nombreCliente = rs.getString("nombrecliente");
				estadoPedido = rs.getString("descripcion");
				fechaPedido = rs.getString("fechapedido");
				idcliente = rs.getInt("idcliente");
				enviadopixel = rs.getInt("enviadopixel");
				numposheader = rs.getInt("numposheader");
				stringpixel = rs.getString("stringpixel");
				fechainsercion = rs.getString("fechainsercion");
				usuariopedido = rs.getString("usuariopedido");
				direccion = rs.getString("direccion");
				telefono = rs.getString("telefono");
				url = rs.getString("url");
				formapago = rs.getString("formapago");
				idformapago = rs.getInt("idforma_pago");
				tiempopedido = rs.getDouble("tiempopedido");
				idLink = rs.getString("idlink");
				if(idLink == null)
				{
					idLink = "";
				}
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				if(fechaPagoVirtual == null)
				{
					fechaPagoVirtual = "";
				}
				fechaFinalizacion = rs.getString("fechafinalizacion");
				if(fechaFinalizacion == null)
				{
					fechaFinalizacion = "";
				}
				Tienda tiendapedido = new Tienda(idtienda, nombreTienda, "", url, 0, "", "", "");
				consultaPedido = new Pedido(idpedido,  nombreTienda,totalBruto, impuesto, totalNeto,
						estadoPedido, fechaPedido, nombreCliente, idcliente, enviadopixel,numposheader, tiendapedido, stringpixel, fechainsercion, usuariopedido, direccion, telefono, formapago, idformapago, tiempopedido, idLink, fechaPagoVirtual, fechaFinalizacion);

			}
			rs.close();
			stm.close();
			con1.close();

		}catch(Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			
		}
		return(consultaPedido);
	}
	
	
	/**
	 * M�todo que retorna un ArrayList con objetos de tipo Pedido Pago virtual monitoreo con el fin de saber el estado de los pagos virtuales
	 * @return
	 */
	public static ArrayList<PedidoMonitoreoPagoVirtual> obtenerPedidosMonitoreoPagoVirtual()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<PedidoMonitoreoPagoVirtual> pedidosMonitoreo = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "SELECT a.idpedido, b.nombre AS tienda, concat(c.nombre,' ', c.apellido) AS nombre, c.telefono, c.telefono_celular, c.email, a.total_neto, a.idlink, a.fechainsercion,TIMESTAMPDIFF(MINUTE,a.fechainsercion,CURTIME()) AS minutos, a.idcliente, d.idforma_pago FROM pedido a, tienda b, cliente c, pedido_forma_pago d WHERE a.idtienda = b.idtienda AND a.idcliente = c.idcliente and a.idpedido = d.idpedido and a.fechapedido = CURDATE() AND a.idestadopedido = 2 AND a.enviadopixel = 2 ";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idPedido;
			String tienda;
			String nombre;
			String telefono;
			String telefonoCelular;
			String email;
			String totalNeto;
			String idLink;
			String fechaInsercion;
			int minutos;
			int idCliente;
			int idFormaPago;
			PedidoMonitoreoPagoVirtual pedTemp;
			while(rs.next()){
				idPedido = rs.getInt("idpedido");
				tienda = rs.getString("tienda");
				nombre = rs.getString("nombre");
				telefono = rs.getString("telefono");
				telefonoCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				totalNeto = rs.getString("total_neto");
				idLink = rs.getString("idlink");
				fechaInsercion = rs.getString("fechainsercion");
				minutos = rs.getInt("minutos");
				idCliente = rs.getInt("idcliente");
				idFormaPago = rs.getInt("idforma_pago");
				pedTemp = new PedidoMonitoreoPagoVirtual(idPedido,tienda, nombre, telefono, telefonoCelular, email, totalNeto, idLink,fechaInsercion,minutos,idFormaPago, idCliente);
				pedidosMonitoreo.add(pedTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			System.out.println("falle lanzando la consulta de domicilios.com " + e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.info(e1.toString());
				System.out.println("falle cerrando la conexion");
			}
		}
		return(pedidosMonitoreo);
	}
	
	public static boolean marcarPedidoEmpresarial(int idpedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido set venta_corporativa  = 'S'  where idpedido = "+ idpedido;
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
			return(false);
		}
		return(true);
	}
	
	/**
	 * Método que se encarga de retornar un booleano que nos indicará si una orden se encuentra cancelada en true o sino en false.
	 * @param idOrdenComercio
	 * @return
	 */
	public static boolean validarCancelacionPlataforma(BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String select = "select * from  pedido  where idordencomercio = "+ idOrdenComercio + " and cancelado = 1";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				respuesta = true;
				break;
			}
			rs.close();
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
		return(respuesta);
	}
	
	/**
	 * Método que se encargará de marcar como cancelado un pedido de plataforma pasado como parámetro
	 * @param idOrdenComercio
	 * @return
	 */
	public static boolean marcarCancelacionPlataforma(BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update  pedido set cancelado = 1, fecha_cancelacion = CURRENT_TIMESTAMP()  where idordencomercio = "+ idOrdenComercio;
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
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
		return(respuesta);
	}
	
	/**
	 * Método que se encargará de marcar como salido con el domiciliario un pedido de plataforma pasado como parámetro
	 * @param idOrdenComercio
	 * @return
	 */
	public static boolean marcarDomiciliarioPlataforma(BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update  pedido set domiciliario = 1, fecha_domiciliario = CURRENT_TIMESTAMP()  where idordencomercio = "+ idOrdenComercio;
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
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
		return(respuesta);
	}
	
	/**
	 * Método que se encargará de marcar como salido con el domiciliario un pedido de plataforma pasado como parámetro
	 * @param idOrdenComercio
	 * @return
	 */
	public static boolean marcarEntregadoPlataforma(BigInteger idOrdenComercio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update  pedido set entregado = 1, fecha_entregado = CURRENT_TIMESTAMP()  where idordencomercio = "+ idOrdenComercio;
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
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
		return(respuesta);
	}
	
	/**
	 * Método que retorna los pedidos cancelados por plataforma según una fecha determinada
	 * @param fecha
	 * @return
	 */
	public static ArrayList<PedidoCanceladoPlataforma> ObtenerPedidosCancelados (String fechainicial)
	{
		Logger logger = Logger.getLogger("log_file");
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		ArrayList<PedidoCanceladoPlataforma> pedidosCanc = new ArrayList();
		String consulta = "SELECT idpedido,numposheader, idordencomercio, fechainsercion, fecha_cancelacion, fecha_domiciliario, fecha_entregado FROM pedido  WHERE  fechapedido = '" + fechaini + "' AND cancelado = 1"; 
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idPedido;
			int idPedidoTienda;
			String idOrdenComercio;
			String fechaIngreso;
			String fechaDomiciliario;
			String fechaEntregado;
			String fechaCancelacion;
			while(rs.next())
			{
				idPedido = rs.getInt("idpedido");
				idPedidoTienda = rs.getInt("numposheader");
				idOrdenComercio = rs.getString("idordencomercio");
				fechaIngreso = rs.getString("fechainsercion");
				fechaDomiciliario = rs.getString("fecha_domiciliario");
				fechaEntregado = rs.getString("fecha_entregado");
				fechaCancelacion = rs.getString("fecha_cancelacion");
				PedidoCanceladoPlataforma pedido = new PedidoCanceladoPlataforma(idPedido,idPedidoTienda,idOrdenComercio,fechaIngreso,fechaDomiciliario, fechaEntregado,fechaCancelacion);
				pedidosCanc.add(pedido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(pedidosCanc);
	}
	
	
	/**
	 * Método que trae todos los pedidos de la plataforma dado una fecha determinada
	 * @param fechainicial
	 * @return
	 */
	public static ArrayList<PedidoPlataformaMonitoreo> obtenerPedidosPlataforma (String fechainicial)
	{
		Logger logger = Logger.getLogger("log_file");
		String fechaini = fechainicial.substring(6, 10)+"-"+fechainicial.substring(3, 5)+"-"+fechainicial.substring(0, 2);	
		ArrayList<PedidoPlataformaMonitoreo> pedidos = new ArrayList();
		String consulta = "SELECT a.idpedido, a.numposheader, a.idordencomercio, a.fechainsercion, IFNULL(a.fecha_cancelacion,'') AS fecha_cancelacion, IFNULL(a.fecha_domiciliario,'') AS fecha_domiciliario, IFNULL(a.fecha_entregado, '') AS fecha_entregado, IFNULL(TIMESTAMPDIFF(MINUTE, a.fechainsercion, a.fecha_domiciliario),0) AS tiemposalidadomiciliario, IFNULL(TIMESTAMPDIFF(MINUTE, a.fecha_domiciliario, a.fecha_entregado),0) AS tiempodomiciliopedido, IFNULL(fecha_domiciliario,TIMESTAMPDIFF(MINUTE, a.fechainsercion, NOW())) AS tiempopedido FROM pedido a, marcacion_pedido b WHERE a.idpedido = b.idpedido AND a.fechapedido = '" + fechaini + "' AND b.idmarcacion = 1 ORDER BY a.idpedido asc"; 
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int idPedido;
			int idPedidoTienda;
			String idOrdenComercio;
			String fechaIngreso;
			String fechaDomiciliario;
			String fechaEntregado;
			String fechaCancelacion;
			int tiempoSalidaDomiciliario;
			int tiempoDomicilioPedido;
			int tiempoPedido;
			while(rs.next())
			{
				idPedido = rs.getInt("idpedido");
				idPedidoTienda = rs.getInt("numposheader");
				idOrdenComercio = rs.getString("idordencomercio");
				fechaIngreso = rs.getString("fechainsercion");
				fechaDomiciliario = rs.getString("fecha_domiciliario");
				fechaEntregado = rs.getString("fecha_entregado");
				fechaCancelacion = rs.getString("fecha_cancelacion");
				tiempoSalidaDomiciliario = rs.getInt("tiemposalidadomiciliario");
				tiempoDomicilioPedido = rs.getInt("tiempoDomicilioPedido");
				try {
					tiempoPedido = rs.getInt("tiempopedido");
				}catch(Exception e)
				{
					tiempoPedido = 0;
				}
				PedidoPlataformaMonitoreo pedido = new PedidoPlataformaMonitoreo(idPedido,idPedidoTienda,idOrdenComercio,fechaIngreso,tiempoPedido,fechaDomiciliario,tiempoSalidaDomiciliario, fechaEntregado, tiempoDomicilioPedido,fechaCancelacion);
				pedidos.add(pedido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(pedidos);
	}

}

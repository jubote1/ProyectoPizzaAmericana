package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.Cliente;
import capaModeloCC.DetallePedido;
import capaModeloCC.DetallePedidoAdicion;
import capaModeloCC.DetallePedidoPixel;
import capaModeloCC.InsertarPedidoPixel;
import capaModeloCC.ModificadorDetallePedido;
import capaModeloCC.PedidoInfoAdicional;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

public class PedidoPOSPMDAO {
	
		
	public static InsertarPedidoPixel confirmarPedidoPOSPM(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado,int idformapagotienda)
	{
		int numFactura = 0;
		//Debemos obtener el idTienda del Pedido que vamos a finalizar
				Tienda tiendaPedido = PedidoDAO.obtenerTiendaPedido(idpedido);
				
				//Llamado Inserciï¿½n Pixel
				//En este punto es donde debemos intervenir para la holmologación
				ArrayList <DetallePedidoPixel> envioPixel = PedidoPOSPMDAO.InsertarPedidoPOSPM(idpedido, tiendaPedido.getIdTienda());
				
				//Obtenemos el origen del pedido y otra
				PedidoInfoAdicional pediInfoAdicional = PedidoDAO.obtenerOrigenPedido(idpedido);
				
				//La invocación del pedido ya no se realizará así
				//OJO
				//Main principal = new Main();
				Cliente cliente = ClienteDAO.obtenerClienteporID(idcliente);
				boolean indicadorAct = false;
				if(insertado == 0)
				{
					indicadorAct = true;
				}
				
				//Si memcode = 0 es porque hay que crear el cliente
				//Si memcode <> 0 y indicador igual a true hay que actualizar
				//Si memcode <> 0 y indicador igual a false hay que actualizar
				int memcode = cliente.getMemcode();
				InsertarPedidoPixel pedidoPixel = new InsertarPedidoPixel(envioPixel,tiendaPedido.getDsnTienda(),cliente.getMemcode(),cliente,indicadorAct,valorformapago, idformapagotienda);
				pedidoPixel.setOrigen(pediInfoAdicional.getOrigen());
				pedidoPixel.setProgramado(pediInfoAdicional.getProgramado());
				pedidoPixel.setHoraProgramado(pediInfoAdicional.getHoraProgramado());
				pedidoPixel.setDescuento(pediInfoAdicional.getDescuento());
				pedidoPixel.setIdTipoPedido(pediInfoAdicional.getIdTipoPedido());
				return(pedidoPixel);
				
	}
	
	
		
		
	/**
	 * Método que se encarga de generar la homologación para los productos en el sistema POS de PIzza Americana, porl o visto 
	 * es demasiado similar a como se maneja con el sistema POS Pixel
	 * @param idpedido
	 * @param idtienda
	 * @return
	 */
	public static ArrayList<DetallePedidoPixel> InsertarPedidoPOSPM(int idpedido, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		logger.info("Se inicia la homologaciï¿½n para base de datos POS Pizza Americana, para el pedido " + idpedido);
		//Tener en cuenta que tenemos en homologaciï¿½n el producto 10000 interno que harï¿½ el simil al producto de mensaje en Pixel
		ArrayList <DetallePedido> pedidoPOSPM = PedidoPOSPMDAO.ConsultarDetallePedidoSinAdiciones(idpedido);
		ArrayList <DetallePedidoPixel> pedidoDefinitivoPOSMP = new ArrayList();
		double cantidadPixel;
		int idproductoext;
		int idproductomaestroext;
		int idproductoextsep;
		int idSaborTipoLiquido;
		//extraemos el cï¿½digo producto pixel de la gaseosa para las que van dentro de los combos
		int idproductogasext;
		double valor = 0;
		ArrayList<DetallePedidoAdicion> adicionDetallePedido = new ArrayList();
		ArrayList<ModificadorDetallePedido> modificadoresDetallePedido = new ArrayList();
		int idDetalleMaster;
		int idDetalleModificador = 1;
		for (DetallePedido cadaDetallePedido: pedidoPOSPM)
		{
			adicionDetallePedido = PedidoPOSPMDAO.ObtenerAdicionDetallePedido(cadaDetallePedido.getIddetallepedido());
			modificadoresDetallePedido = PedidoPOSPMDAO.ObtenerModificadorDetallePedido(cadaDetallePedido.getIddetallepedido());
			//Aqui tendremos la lï¿½gica para generar un array list
			//Definimos la cantidad del item que se va a pasar al otro sistema
			int cantidad = (int) cadaDetallePedido.getCantidad();
			//Tomamos el idDetalleMaster que está procesando
			idDetalleMaster = cadaDetallePedido.getIddetallepedido();
			// Este caso se puede dar o para otro producto, o para pizzas un solo ingrediente
			if(cadaDetallePedido.getIdespecialidad1() == 0 && cadaDetallePedido.getIdespecialidad2() == 0)
			{
				cantidadPixel = cadaDetallePedido.getCantidad();
				idSaborTipoLiquido = cadaDetallePedido.getIdsabortipoliquido();
				//Realizamos cambio para soportar tema 
				idproductoext = PedidoPOSPMDAO.RetornarIdproductoMaestroExterno(cadaDetallePedido.getIdproducto(), cadaDetallePedido.getIdexcepcion(), idtienda);
				valor = cadaDetallePedido.getValortotal();
				pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cantidadPixel,true,0,0,idDetalleMaster,valor));
				logger.debug(idproductoext + " , " + cantidadPixel);
				
				//Recorremos los modificadores
				
				for(ModificadorDetallePedido cadaModificador: modificadoresDetallePedido)
				{
					if(cadaModificador.getIdproductoespecialidad1()>0)
					{
						idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaModificador.getIdproductoespecialidad1(), 0, idtienda);
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaModificador.getCantidad(), false,idDetalleMaster,idDetalleMaster,0));
						logger.debug(idproductoext + " , " + cadaModificador.getCantidad());
					}
					
					if(cadaModificador.getIdproductoespecialidad2()>0)
					{
						idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaModificador.getIdproductoespecialidad2(), 0, idtienda);
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaModificador.getCantidad(), false,idDetalleMaster,idDetalleMaster,0));
						logger.debug(idproductoext + " , " + cadaModificador.getCantidad());
					}
					
				}
				
				//Recorremos las adiciones
				for(DetallePedidoAdicion cadaAdicion: adicionDetallePedido)
				{
					if(cadaAdicion.getCantidad1() > 0)
					{
						idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaAdicion.getIdproducto(), 0, idtienda);
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaAdicion.getCantidad1(),false,idDetalleMaster,0,0));
						logger.debug(idproductoext + " , " + cadaAdicion.getCantidad1());
					}
					else if(cadaAdicion.getCantidad2() > 0)
					{
						idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaAdicion.getIdproducto(), 0, idtienda);
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaAdicion.getCantidad2(),false,idDetalleMaster,0,0));
						logger.debug(idproductoext + " , " + cadaAdicion.getCantidad2());
					}
					
				}
				
				//Debemos revisar si el producto lleva gaseosa, en cuyo caso en el detalle del pedido debe ir un valor diferente a cero
				if (idSaborTipoLiquido > 0)
				{
					idproductoext = PedidoPOSPMDAO.RetornarIdprodGaseosaPromo(idSaborTipoLiquido, idtienda);
					pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cantidadPixel,false,idDetalleMaster,0,0));
					logger.debug(idproductoext + " , " + cantidadPixel);
					
				}
				//Poner separador 0,0
				//Decidimos no separar lo productos
				//pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(0, 0));
				//logger.debug(0 + " , " + 0);
				
			}
			else
			{
				//Requerimos de un ciclo for para dividir la pizza si es el caso en varias
				for(int i = 1; i <= cantidad; i++)
				{
					if((cadaDetallePedido.getIdespecialidad1() > 0)&&(cadaDetallePedido.getIdespecialidad2() == 0))
					{
						//En las pizzas y con base en la pizza que se estï¿½ facturando, al principio debo de tener un master item
						//Master ITEM Se harï¿½ la homologaciï¿½n entre el cï¿½digo producto y la excepciï¿½n de precio
						idproductomaestroext = PedidoPOSPMDAO.RetornarIdproductoMaestroExterno(cadaDetallePedido.getIdproducto(), cadaDetallePedido.getIdexcepcion(), idtienda);
						idDetalleMaster = cadaDetallePedido.getIddetallepedido();
						int idproductopizza = 0;
						cantidadPixel = 0.5;
						idSaborTipoLiquido = cadaDetallePedido.getIdsabortipoliquido();
						//Adicionamos el producto maestro con cantidad 1
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductomaestroext, 1,true,0,0,idDetalleMaster));
						logger.debug(idproductomaestroext + " , " + cantidadPixel);
						
						idproductoextsep = PedidoPOSPMDAO.RetornarIdproductoExterno(10000, 0, idtienda);
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoextsep, cantidadPixel));
						logger.debug(idproductoextsep + " , " + cantidadPixel);
						
						idproductopizza = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaDetallePedido.getIdproducto(), cadaDetallePedido.getIdespecialidad1(), idtienda);
						idDetalleModificador = idproductopizza;
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductopizza, cantidadPixel,false,idDetalleMaster,0,0));
						logger.debug(idproductopizza + " , " + cantidadPixel);
						
						//Aqui se ingresan la adiciones y los modificadores
						for(DetallePedidoAdicion cadaAdicion: adicionDetallePedido)
						{
							if(cadaAdicion.getIdespecialidad1() > 0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaAdicion.getIdproducto(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaAdicion.getCantidad1(),false,idDetalleMaster,0,0));
								logger.debug(idproductoext + " , " + cadaAdicion.getCantidad1());
							}
						}
						
						//Recorremos los modificadores
						
						for(ModificadorDetallePedido cadaModificador: modificadoresDetallePedido)
						{
							if(cadaModificador.getIdproductoespecialidad1()>0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaModificador.getIdproductoespecialidad1(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaModificador.getCantidad()/2, false, idDetalleMaster,idDetalleModificador,0));
								logger.debug(idproductoext + " , " + cadaModificador.getCantidad()/2);
							}
							
						}
						
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoextsep, cantidadPixel));
						logger.debug(idproductoextsep + " , " + cantidadPixel);
						
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductopizza, cantidadPixel,false,idDetalleMaster,0,0));
						logger.debug(idproductopizza + " , " + cantidadPixel);
						
						for(DetallePedidoAdicion cadaAdicion: adicionDetallePedido)
						{
							if(cadaAdicion.getIdespecialidad2() > 0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaAdicion.getIdproducto(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaAdicion.getCantidad2(),false,idDetalleMaster,0,0));
								logger.debug(idproductoext + " , " + cadaAdicion.getCantidad2());
							}
						}
						
						for(ModificadorDetallePedido cadaModificador: modificadoresDetallePedido)
						{
							if(cadaModificador.getIdproductoespecialidad1()>0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaModificador.getIdproductoespecialidad1(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaModificador.getCantidad()/2, false, idDetalleMaster,idDetalleModificador,0));
								logger.debug(idproductoext + " , " + cadaModificador.getCantidad()/2);
							}
							
						}
						
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoextsep, cantidadPixel));
						logger.debug(idproductoextsep + " , " + cantidadPixel);
						
						//Debemos revisar si el producto lleva gaseosa, en cuyo caso en el detalle del pedido debe ir un valor diferente a cero
						if (idSaborTipoLiquido > 0)
						{
							idproductoext = PedidoPOSPMDAO.RetornarIdprodGaseosaPromo(idSaborTipoLiquido, idtienda);
							pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, 1,false, idDetalleMaster, 0, 0));
							logger.debug(idproductoext + " , " + cantidadPixel);
							
						}
						//Poner separador 0,0
						//Decidimos no separar lo productos
						//pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(0, 0));
						//logger.debug(0 + " , " + 0);
						
					}else
					{
						//En las pizzas y con base en la pizza que se estï¿½ facturando, al principio debo de tener un master item
						//Master ITEM Se harï¿½ la homologaciï¿½n entre el cï¿½digo producto y la excepciï¿½n de precio
						idproductomaestroext = PedidoPOSPMDAO.RetornarIdproductoMaestroExterno(cadaDetallePedido.getIdproducto(), cadaDetallePedido.getIdexcepcion(), idtienda);
						cantidadPixel = 0.5;
						//Tomamos el idDetalleMaster para agregarlo a lo items detalle hijos
						idDetalleMaster = cadaDetallePedido.getIddetallepedido();
						idSaborTipoLiquido = cadaDetallePedido.getIdsabortipoliquido();
						//Adicionamos el producto maestro con cantidad 1
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductomaestroext, 1,true,0,0,idDetalleMaster));
						logger.debug(idproductomaestroext + " , " + cantidadPixel);
						
						idproductoextsep = PedidoPOSPMDAO.RetornarIdproductoExterno(10000, 0, idtienda);
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoextsep, cantidadPixel));
						logger.debug(idproductoextsep + " , " + cantidadPixel);
						
						idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaDetallePedido.getIdproducto(), cadaDetallePedido.getIdespecialidad1(), idtienda);
						idDetalleModificador = idproductoext;
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cantidadPixel, false, idDetalleMaster,0,0));
						logger.debug(idproductoext + " , " + cantidadPixel);
						
						for(DetallePedidoAdicion cadaAdicion: adicionDetallePedido)
						{
							if(cadaAdicion.getIdespecialidad1() > 0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaAdicion.getIdproducto(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaAdicion.getCantidad1(),false,idDetalleMaster,0,0));
								logger.debug(idproductoext + " , " + cadaAdicion.getCantidad1());
							}
						}
						
						for(ModificadorDetallePedido cadaModificador: modificadoresDetallePedido)
						{
							if(cadaModificador.getIdproductoespecialidad1()>0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaModificador.getIdproductoespecialidad1(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaModificador.getCantidad()/2, false,idDetalleMaster,idDetalleModificador,0));
								logger.debug(idproductoext + " , " + cadaModificador.getCantidad()/2);
							}
							
						}
						
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoextsep, cantidadPixel));
						logger.debug(idproductoextsep + " , " + cantidadPixel);
						
						idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaDetallePedido.getIdproducto(), cadaDetallePedido.getIdespecialidad2(), idtienda);
						idDetalleModificador = idproductoext;
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cantidadPixel, false,idDetalleMaster,0,0));
						logger.debug(idproductoext + " , " + cantidadPixel);
						
						for(DetallePedidoAdicion cadaAdicion: adicionDetallePedido)
						{
							if(cadaAdicion.getIdespecialidad2() > 0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaAdicion.getIdproducto(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaAdicion.getCantidad2(),false,idDetalleMaster,0,0));
								logger.debug(idproductoext + " , " + cadaAdicion.getCantidad2());
							}
						}
						
						for(ModificadorDetallePedido cadaModificador: modificadoresDetallePedido)
						{
							if(cadaModificador.getIdproductoespecialidad2()>0)
							{
								idproductoext = PedidoPOSPMDAO.RetornarIdproductoExterno(cadaModificador.getIdproductoespecialidad2(), 0, idtienda);
								pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, cadaModificador.getCantidad()/2, false,idDetalleMaster,idDetalleModificador,0));
								logger.debug(idproductoext + " , " + cadaModificador.getCantidad()/2);
							}
							
						}
						
						pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoextsep, cantidadPixel));
						logger.debug(idproductoextsep + " , " + cantidadPixel);
						
						//Debemos revisar si el producto lleva gaseosa, en cuyo caso en el detalle del pedido debe ir un valor diferente a cero
						if (idSaborTipoLiquido > 0)
						{
							idproductoext = PedidoPOSPMDAO.RetornarIdprodGaseosaPromo(idSaborTipoLiquido, idtienda);
							pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(idproductoext, 1,false,idDetalleMaster,0,0));
							logger.debug(idproductoext + " , " + cantidadPixel);
							
						}
						//Poner separador 0,0
						//Decidimos no separar lo productos
						//pedidoDefinitivoPOSMP.add(new DetallePedidoPixel(0, 0));
						//logger.debug(0 + " , " + 0);
						
					}

				}
			}	
		}
		return(pedidoDefinitivoPOSMP);
	}
	

		
	public static ArrayList<DetallePedido> ConsultarDetallePedidoSinAdiciones(int numeropedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList <DetallePedido> consultaDetallePedidos = new ArrayList();
		String consulta = "select a.iddetalle_pedido, a.idproducto, b.nombre, a.cantidad, a.idespecialidad1, a.idespecialidad2, c.nombre especialidad1, d.nombre especialidad2, "
				+ "a.valorUnitario, a.valorTotal, a.adicion, a.observacion, e.descripcion liquido , f.descripcion excepcion, a.idexcepcion, a.idsabortipoliquido  from detalle_pedido a left outer join especialidad "
				+ "c on a.idespecialidad1 = c.idespecialidad left outer join especialidad d on a.idespecialidad2 = d.idespecialidad"
				+ " left outer join sabor_x_tipo_liquido e on a.idsabortipoliquido = e.idsabor_x_tipo_liquido "
				+ "left outer join excepcion_precio f on a.idexcepcion = f.idexcepcion"
				+ ",producto b where a.idproducto = b.idproducto and b.tipo in ('OTROS' , 'PIZZA' , 'ADICIONALES' , 'GASEOSA') and idpedido = " + numeropedido;
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
				DetallePedido cadaDetallePedido = new DetallePedido(iddetallepedido, nombreproducto, idproducto, cantidad,especialidad1, idespecialidad1, especialidad2, idespecialidad2,valorunitario, valortotal,adicion,observacion,liquido, excepcion, numeropedido, idexcepcion, idsabortipoliquido);
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
	
	
	public static ArrayList<ModificadorDetallePedido> ObtenerModificadorDetallePedido (int iddetallepedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ModificadorDetallePedido> consultaModificadores = new ArrayList();
		String consulta = "select a.iddetallepedidopadre,  a.idproductoespecialidad1, a.idproductoespecialidad2, "
				+ "a.cantidad from modificador_detalle_pedido a where a.iddetallepedidopadre =" + iddetallepedido; 
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
			double cantidad;
			while(rs.next())
			{
				iddetallepedidopadre = Integer.parseInt(rs.getString("iddetallepedidopadre"));
				idproductoespecialidad1 = Integer.parseInt(rs.getString("idproductoespecialidad1"));
				idproductoespecialidad2 = Integer.parseInt(rs.getString("idproductoespecialidad2"));
				cantidad = Double.parseDouble(rs.getString("cantidad"));
				ModificadorDetallePedido detModificador = new ModificadorDetallePedido(0,iddetallepedidopadre,idproductoespecialidad1, idproductoespecialidad2, cantidad);
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
	
	
	public static int RetornarIdproductoExterno(int idproductoint, int idespecialidadint, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		int idProductoExt = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idproductoext from homologacion_producto where idproductoint = " + idproductoint + " and  idespecialidadint = " + idespecialidadint + " and idtienda =" + idtienda ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idProductoExt = rs.getInt("idproductoext");
				break;
			}
	        rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
			logger.error(e.toString());
			return(-1);
		}
		return(idProductoExt);
	}
	
	
	public static int RetornarIdproductoMaestroExterno(int idproductoint, int idexcepcion, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		int idProductoExt = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idproductoext from homologacion_producto where idproductoint = " + idproductoint + " and  idexcepcion = " + idexcepcion + " and idtienda =" + idtienda + " and idespecialidadint = 0" ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idProductoExt = rs.getInt("idproductoext");
				break;
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
			return(-1);
		}
		return(idProductoExt);
	}
	
	public static String RetornarPrecioProductoMaestroExterno(int idproductoint, int idexcepcion, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		String precio = "";
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select precio from homologacion_producto where idproductoint = " + idproductoint + " and  idexcepcion = " + idexcepcion + " and idtienda =" + idtienda + " and idespecialidadint = 0" ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				precio = rs.getString("precio");
				break;
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
			return("");
		}
		return(precio);
	}
	
	public static int RetornarIdprodGaseosaPromo(int idsabortipoliquidoint, int idtienda)
	{
		Logger logger = Logger.getLogger("log_file");
		int idProductoExt = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idproductoext from homologacion_producto where idsabortipoliquidoint = " + idsabortipoliquidoint  + " and idtienda =" + idtienda ; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idProductoExt = rs.getInt("idproductoext");
				break;
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
			return(-1);
		}
		return(idProductoExt);
	}
	
	

}

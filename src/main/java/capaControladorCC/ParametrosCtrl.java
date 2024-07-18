package capaControladorCC;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.DireccionFueraZonaDAO;
import capaDAOCC.EspecialidadDAO;
import capaDAOCC.EstadoPedidoDAO;
import capaDAOCC.ExcepcionPrecioDAO;
import capaDAOCC.FormaPagoDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.HorarioDiaDAO;
import capaDAOCC.IntegracionCRMDAO;
import capaDAOCC.MarcacionDAO;
import capaDAOCC.MunicipioDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.ProductoDAO;
import capaDAOCC.ProductoNoExistenteDAO;
import capaDAOCC.SaborTipoLiquidoDAO;
import capaDAOCC.TiempoPedidoDAO;
import capaDAOCC.TiendaDAO;
import capaDAOCC.TipoLiquidoDAO;
import capaDAOCC.TipoPedidoDAO;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.DireccionFueraZona;
import capaModeloCC.Especialidad;
import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FormaPago;
import capaModeloCC.HorarioDia;
import capaModeloCC.IntegracionCRM;
import capaModeloCC.Marcacion;
import capaModeloCC.Municipio;
import capaModeloCC.Parametro;
import capaModeloCC.Producto;
import capaModeloCC.ProductoNoExistente;
import capaModeloCC.SaborLiquido;
import capaModeloCC.TiempoPedido;
import capaModeloCC.Tienda;
import capaModeloCC.TipoLiquido;
import capaModeloCC.TipoPedido;
import utilidadesCC.ControladorEnvioCorreo;
/**
 * 
 * @author Juan David Botero Duque
 * Esta clase est� en la capa controlador y se encarga de todos los par�metros de la soluci�n
 *
 */
public class ParametrosCtrl {
	
	//FORMAPAGO
	
	/**
	 * M�todo en la capa controlador que se encarga de retornar la informaci�n de las formas de pago en formato JSON
	 * @return
	 */
	public String retornarFormasPago(String tipoPago){
		JSONArray listJSON = new JSONArray();
		ArrayList<FormaPago> formasPago = FormaPagoDAO.obtenerFormasPago(tipoPago);
		for (FormaPago forma : formasPago) 
		{
			JSONObject cadaFormaPagoJSON = new JSONObject();
			cadaFormaPagoJSON.put("idformapago", forma.getIdformapago());
			cadaFormaPagoJSON.put("nombre", forma.getNombre());
			cadaFormaPagoJSON.put("tipoformapago", forma.getTipoforma());
			cadaFormaPagoJSON.put("virtual", forma.getVirtual());
			cadaFormaPagoJSON.put("mensajetexto", forma.getMensajeTexto());
			cadaFormaPagoJSON.put("mensajecorreo", forma.getMensajeCorreo());
			listJSON.add(cadaFormaPagoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String retornarFormasPagoContact(String tipoPago){
		JSONArray listJSON = new JSONArray();
		ArrayList<FormaPago> formasPago = FormaPagoDAO.obtenerFormasPagoContact(tipoPago);
		for (FormaPago forma : formasPago) 
		{
			JSONObject cadaFormaPagoJSON = new JSONObject();
			cadaFormaPagoJSON.put("idformapago", forma.getIdformapago());
			cadaFormaPagoJSON.put("nombre", forma.getNombre());
			cadaFormaPagoJSON.put("tipoformapago", forma.getTipoforma());
			cadaFormaPagoJSON.put("virtual", forma.getVirtual());
			cadaFormaPagoJSON.put("mensajetexto", forma.getMensajeTexto());
			cadaFormaPagoJSON.put("mensajecorreo", forma.getMensajeCorreo());
			listJSON.add(cadaFormaPagoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public int realizarHomologacionFormaPagoTiendaVirtual(String codTiendaVirtual)
	{
		int idFormaPago = FormaPagoDAO.realizarHomologacionFormaPagoTiendaVirtual(codTiendaVirtual);
		return(idFormaPago);
	}
	
	//ESPECIALIDAD
	
	/**
	 * M�todo en la capa controlador que recibe par�metros desde la capa de Presentaci�n para la creaci�n de especialidades
	 * @param nombre Nombre de la especialidad
	 * @param abreviatura Abreviatura de la especialidad
	 * @return Retorna el id de la especialidad creado y devuelvo por la base de datos
	 */
	public String insertarEspecialidad(String nombre, String abreviatura)
	{
		JSONArray listJSON = new JSONArray();
		Especialidad Espe = new Especialidad(nombre, abreviatura);
		int idEspIns = EspecialidadDAO.insertarEspecialidad(Espe);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idespecialidad", idEspIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * 
	 * @param idespecialidad Recibe como par�metro el idespecialidad que se va  a consultar
	 * @return Retorna en formato JSON la informaci�n de la especialidad de acuerdo al par�metro de idespecialidad.
	 */
	public String retornarEspecialidad(int idespecialidad)
	{
		JSONArray listJSON = new JSONArray();
		Especialidad Espe = EspecialidadDAO.retornarEspecialidad(idespecialidad);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idespecialidad", Espe.getIdespecialidad());
		ResultadoJSON.put("nombre", Espe.getNombre());
		ResultadoJSON.put("abreviatura", Espe.getAbreviatura());
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * 
	 * @param idespecialidad Se pasa como par�metro el idespecialidad que desea se eliminado
	 * @return Se retorna el valor de exitoso en caso de que se elimine de manera exitosa la especialidad.
	 */
	public String eliminarEspecialidad(int idespecialidad)
	{
		JSONArray listJSON = new JSONArray();
		EspecialidadDAO.eliminarEspecialidad(idespecialidad);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", "exitoso");
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * 
	 * @param idespecialidad Par�metro con base en el cual se edita la especialidad.
	 * @param nombre Valor de nombre de la especialidad que va a ser editado.
	 * @param abreviatura  Valor abreviatura de la especialidad que va a  ser editado.
	 * @return Se retorna el valor resultado.
	 */
	public String editarEspecialidad(int idespecialidad , String nombre, String abreviatura)
	{
		JSONArray listJSON = new JSONArray();
		Especialidad Espe = new Especialidad(idespecialidad, nombre, abreviatura);
		String resultado = EspecialidadDAO.editarEspecialidad(Espe);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", resultado);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	
	//ESTADO PEDIDO
	
	/**
	 * M�todo para la inserci�n de estado pedido en la capa de controlador
	 * @param descripcion Se recibe como par�metro el valor de la descripci�n del estado.
	 * @return Se devuelve el idestado insertado desde base de datos.
	 */
	public String insertarEstadoPedido(String descripcion)
	{
		JSONArray listJSON = new JSONArray();
		EstadoPedido Est = new EstadoPedido(descripcion);
		int idEstIns = EstadoPedidoDAO.insertarEstadoPedido(Est);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idestadopedido", idEstIns);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * M�todo que se encarga de retornar la informaci�n de un estado pedido determinado de acuerdo al par�metro idestadopedido
	 * @param idestadopedido Se recibe como par�metro el idestadopedido a consultar.
	 * @return Se retona en formato JSON la informaci�n del estado pedido a consutar.
	 */
	public String retornarEstadoPedido(int idestadopedido)
	{
		JSONArray listJSON = new JSONArray();
		EstadoPedido Est = EstadoPedidoDAO.retornarEstadoPedido(idestadopedido);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("idestadopedido", Est.getIdestadopedido());
		ResultadoJSON.put("descripcion", Est.getDescripcion());
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	
	/**
	 * M�todo que retorna en formato JSON los estados pedidos creados en la base de datos en capa controlador
	 * @return M�todo que retorna en formato JSON los estados pedidos creados en la base de datos en capa controlador
	 */
	public String retornarEstadosPedido(){
		JSONArray listJSON = new JSONArray();
		ArrayList<EstadoPedido> estados = EstadoPedidoDAO.obtenerEstadosPedido();
		for (EstadoPedido est : estados) 
		{
			JSONObject cadaEstadoJSON = new JSONObject();
			cadaEstadoJSON.put("idestadopedido", est.getIdestadopedido());
			cadaEstadoJSON.put("descripcion", est.getDescripcion());
			listJSON.add(cadaEstadoJSON);
		}
		return listJSON.toJSONString();
	}
	
	
	/**
	 * M�todo en capa controlador que recide el idestadopedido a eliminar y se encarga de llamar al m�todo en la capa DAO, retona el valor exitoso en caso de finalizar bien.
	 * @param idestadopedido Recibe el idestadopedido que va a ser eliminado.
	 * @return Se retorna el resultado del proceso (exitoso)
	 */
	public String eliminarEstadoPedido(int idestadopedido)
	{
		JSONArray listJSON = new JSONArray();
		EstadoPedidoDAO.eliminarEstadoPedido(idestadopedido);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", "exitoso");
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * Met�do en capa controlador que recibe los valores de estado a pedido a editar y se encarga de llamar al m�todo correspondiente en la capa DAO.
	 * @param idestadopedido Se recibe el idestadopedido a editar.
	 * @param descripcion Se recibe el valor de la descripci�n del estado pedido que se va a editar.
	 * @return Se retorna el valor del resultado de la edici�n.
	 */
	public String editarEstadoPedido(int idestadopedido , String descripcion)
	{
		JSONArray listJSON = new JSONArray();
		EstadoPedido Est = new EstadoPedido(idestadopedido,descripcion);
		String resultado = EstadoPedidoDAO.editarEstadoPedido(Est);
		JSONObject ResultadoJSON = new JSONObject();
		ResultadoJSON.put("resultado", resultado);
		listJSON.add(ResultadoJSON);
		return listJSON.toJSONString();
	}
	
	//EXCEPCION PRECIO
	/**
	 * Para la entidad Excepci�n Precio se reciben los par�metros para inserci�n y se invoca al m�todo correspondiente en la capa DAO.
	 * @param idproducto Se recibe como par�metro idproducto asociado a la excepci�n de precio 
	 * @param precio Se recibe el valor del precio de la excepci�n
	 * @param descripcion Se recibe la descripcion de la excepci�n.
	 * @param incluye_liquido Se recibe si la excepci�n de precio incluye o no l�quido.
	 * @param idtipoliquido En caso de incluir l�quido se recibe par�metro del tipo liquido que incluye.
	 * @return Se retorna el id excepci�n creado seg�n los par�metros enviados.
	 */
		public String insertarExcepcionPrecio(ExcepcionPrecio exc)
		{
			JSONArray listJSON = new JSONArray();
			int idEscIns = ExcepcionPrecioDAO.insertarExcepcionPrecio(exc);
			JSONObject ResultadoJSON = new JSONObject();
			ResultadoJSON.put("idexcepcion", idEscIns);
			listJSON.add(ResultadoJSON);
			return listJSON.toJSONString();
		}
		
		
		
		/**
		 * M�todo en la capa controlador que se encarga de recibir el id excepci�n a consultar y de invocar el m�todo en clase DAO que ejecuta la acci�n.
		 * @param idexcepcion Se recibe el valor del idexcepci�n con base en el cual se retonar� en formato JSON la entidad Excepci�n Precio.
		 * @return Se retorna en formato JSON la informaci�n de la entidad Excepcion Precio a consultar seg�n el par�metro entregado.
		 */
		public String retornarExcepcionPrecio(int idexcepcion)
		{
			JSONArray listJSON = new JSONArray();
			ExcepcionPrecio Esc = ExcepcionPrecioDAO.retornarExcepcionPrecio(idexcepcion);
			JSONObject ResultadoJSON = new JSONObject();
			ResultadoJSON.put("idexcepcion", Esc.getIdExcepcion());
			ResultadoJSON.put("idproducto", Esc.getIdProducto());
			ResultadoJSON.put("precio", Esc.getPrecio());
			ResultadoJSON.put("descripcion", Esc.getDescripcion());
			ResultadoJSON.put("incluye_liquido", Esc.getIncluyeliquido());
			ResultadoJSON.put("idtipoliquido", Esc.getIdtipoliquido());
			ResultadoJSON.put("habilitado", Esc.getHabilitado());
			ResultadoJSON.put("horainicial", Esc.getHoraInicial());
			ResultadoJSON.put("horafinal", Esc.getHoraFinal());
			ResultadoJSON.put("lunes", Esc.getLunes());
			ResultadoJSON.put("martes", Esc.getMartes());
			ResultadoJSON.put("miercoles", Esc.getMiercoles());
			ResultadoJSON.put("jueves", Esc.getJueves());
			ResultadoJSON.put("viernes", Esc.getViernes());
			ResultadoJSON.put("sabado", Esc.getSabado());
			ResultadoJSON.put("domingo", Esc.getDomingo());
			listJSON.add(ResultadoJSON);
			return listJSON.toJSONString();
		}
		
		
		/**
		 * M�todo en capa controlador que se encarga de retonar en formato JSON la informaci�n de las entidades Excepci�n Precio, luego del llamado a la capa DAO.
		 * @return retorna en formato JSON las excepciones de precio que se tienen en base de datos.
		 */
		public String retornarExcecionesPrecio(){
			JSONArray listJSON = new JSONArray();
			ArrayList<ExcepcionPrecio> excepciones = ExcepcionPrecioDAO.obtenerExcepcionesPrecio();
			for (ExcepcionPrecio Esc : excepciones) 
			{
				JSONObject cadaExcepcionJSON = new JSONObject();
				cadaExcepcionJSON.put("idexcepcion", Esc.getIdExcepcion());
				cadaExcepcionJSON.put("idproducto", Esc.getIdProducto());
				cadaExcepcionJSON.put("precio", Esc.getPrecio());
				cadaExcepcionJSON.put("descripcion", Esc.getDescripcion());
				cadaExcepcionJSON.put("incluye_liquido", Esc.getIncluyeliquido());
				cadaExcepcionJSON.put("idtipoliquido", Esc.getIdtipoliquido());
				cadaExcepcionJSON.put("habilitado", Esc.getHabilitado());
				cadaExcepcionJSON.put("horainicial", Esc.getHoraInicial());
				cadaExcepcionJSON.put("horafinal", Esc.getHoraFinal());
				cadaExcepcionJSON.put("lunes", Esc.getLunes());
				cadaExcepcionJSON.put("martes", Esc.getMartes());
				cadaExcepcionJSON.put("miercoles", Esc.getMiercoles());
				cadaExcepcionJSON.put("jueves", Esc.getJueves());
				cadaExcepcionJSON.put("viernes", Esc.getViernes());
				cadaExcepcionJSON.put("sabado", Esc.getSabado());
				cadaExcepcionJSON.put("domingo", Esc.getDomingo());
				listJSON.add(cadaExcepcionJSON);
			}
			return listJSON.toJSONString();
		}
		
		/**
		 * M�todo en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocaci�n al m�todo en la capa DAO.
		 * @return M�todo en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocaci�n al m�todo en la capa DAO.
		 */
		public String ObtenerTiposLiquido(){
			JSONArray listJSON = new JSONArray();
			ArrayList<TipoLiquido> tipliquidos = PedidoDAO.ObtenerTiposLiquido();
			for (TipoLiquido tipliq : tipliquidos) 
			{
				JSONObject cadaTipoLiquidoJSON = new JSONObject();
				cadaTipoLiquidoJSON.put("idtipo_liquido", tipliq.getIdtipo_liquido());
				cadaTipoLiquidoJSON.put("nombre", tipliq.getNombre());
				cadaTipoLiquidoJSON.put("capacidad", tipliq.getCapacidad());
				listJSON.add(cadaTipoLiquidoJSON);
			}
			return listJSON.toJSONString();
		}
		
		public String obtenerMarcaciones(String adm){
			JSONArray listJSON = new JSONArray();
			ArrayList<Marcacion> marcaciones = MarcacionDAO.obtenerMarcaciones(adm);
			for (Marcacion mar : marcaciones) 
			{
				JSONObject cadaMarcacionJSON = new JSONObject();
				cadaMarcacionJSON.put("idmarcacion", mar.getIdMarcacion());
				cadaMarcacionJSON.put("nombremarcacion", mar.getNombreMarcacion());
				cadaMarcacionJSON.put("marketplace", mar.getMarketplace());
				listJSON.add(cadaMarcacionJSON);
			}
			return listJSON.toJSONString();
		}
		
		/**
		 * M�todo en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocaci�n al m�todo en la capa DAO.
		 * @return M�todo en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocaci�n al m�todo en la capa DAO.
		 */
		public String retornarExcecionesPrecioGrid(){
			JSONArray listJSON = new JSONArray();
			ArrayList<ExcepcionPrecio> excepciones = ExcepcionPrecioDAO.obtenerExcepcionesPrecioGrid();
			for (ExcepcionPrecio Esc : excepciones) 
			{
				JSONObject cadaExcepcionJSON = new JSONObject();
				cadaExcepcionJSON.put("idexcepcion", Esc.getIdExcepcion());
				cadaExcepcionJSON.put("idproducto", Esc.getIdProducto());
				cadaExcepcionJSON.put("nombreproducto", Esc.getNombreProducto());
				cadaExcepcionJSON.put("precio", Esc.getPrecio());
				cadaExcepcionJSON.put("descripcion", Esc.getDescripcion());
				cadaExcepcionJSON.put("incluye_liquido", Esc.getIncluyeliquido());
				cadaExcepcionJSON.put("idtipoliquido", Esc.getIdtipoliquido());
				cadaExcepcionJSON.put("nombreliquido", Esc.getNombreLiquido());
				cadaExcepcionJSON.put("horainicial", Esc.getHoraInicial());
				cadaExcepcionJSON.put("horafinal", Esc.getHoraFinal());
				cadaExcepcionJSON.put("habilitado", Esc.getHabilitado());
				cadaExcepcionJSON.put("lunes", Esc.getLunes());
				cadaExcepcionJSON.put("martes", Esc.getMartes());
				cadaExcepcionJSON.put("miercoles", Esc.getMiercoles());
				cadaExcepcionJSON.put("jueves", Esc.getJueves());
				cadaExcepcionJSON.put("viernes", Esc.getViernes());
				cadaExcepcionJSON.put("sabado", Esc.getSabado());
				cadaExcepcionJSON.put("domingo", Esc.getDomingo());
				listJSON.add(cadaExcepcionJSON);
			}
			return listJSON.toJSONString();
		}
		
		/**
		 * M�todo que se encarga de recibir el idexcepcion Precio a eliminar, invocar la capa DAO y retonar el resultado del proceso.
		 * @param idexcepcion Se recibe el idexcepcion que ser� eliminado.
		 * @return Se retorna el resultado de la eliminaci�n.
		 */
		public String eliminarExcepcionPrecio(int idexcepcion)
		{
			
			JSONArray listJSON = new JSONArray();
			JSONObject ResultadoJSON;
			boolean tieneRegs = ExcepcionPrecioDAO.validarEliminarExcepcionPrecio(idexcepcion);
			if(!tieneRegs)
			{
				ExcepcionPrecioDAO.eliminarExcepcionPrecio(idexcepcion);
				ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", "exitoso");
			}
			else
			{
				ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", "error");
			}
			listJSON.add(ResultadoJSON);
			return listJSON.toJSONString();
		}
		
		
		/**
		 * M�todo en la capa controlador que se encarga de editar la excepci�n, se reciben los par�metros y se invoca la capa DAO.
		 * @param idexcepcion Valor de idexcepcion a editar.
		 * @param idproducto idproducto valor a editar.
		 * @param precio precio valor a editar
		 * @param descripcion descripcion valor a editar.
		 * @param incluye_liquido valor incluye_liquido valor a editar
		 * @param idtipoliquido idtipoliquido valor a  editar
		 * @return se retorna el resultado del proceso de la edici�n.
		 */
		public String editarExcepcionPrecio(ExcepcionPrecio  exc)
		{
			JSONArray listJSON = new JSONArray();
			String resultado = ExcepcionPrecioDAO.editarExcepcionPrecio(exc);
			JSONObject ResultadoJSON = new JSONObject();
			ResultadoJSON.put("resultado", resultado);
			listJSON.add(ResultadoJSON);
			return listJSON.toJSONString();
		}
		
		//PRODUCTO
		
			
		/**
		 * Se reciben los par�metros para la inserci�n del producto, se invoca a la capa DAO y se retorna el idproducto insertado
		 * @param idreceta Par�metro la inserci�n del producto, id receta asociado al producto.
		 * @param nombre Nombre del producto a insertar.
		 * @param descripcion Descripci�n del producto a insertar.
		 * @param impuesto Impuesto que manejar� el producto a insertar.
		 * @param tipo Clasificaci�n asociado al producto a insertar, podr� ser PIZZAS, OTROS, MODIFICADORES CON etc
		 * @param preciogeneral Precio General que manejar� al producto
		 * @param incluye_liquido Se recibe el par�metro si el producto manejar�  o no liquido
		 * @param idtipo_liquido Dependiendo de si el producto incluye liquido o no, se parametriza el tipo liquido que manera el producto.
		 * @return Se retorna el idproducto creado.
		 */
			public String insertaProducto(int idreceta,String nombre, String descripcion, float impuesto, String tipo, double preciogeneral, String incluye_liquido, int idtipo_liquido, String habilitado)
			{
				JSONArray listJSON = new JSONArray();
				Producto Pro = new Producto(0,idreceta,nombre, descripcion, impuesto, tipo, 0, preciogeneral,  incluye_liquido, idtipo_liquido,"",habilitado);
				int idProIns = ProductoDAO.insertarProducto(Pro);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idproducto", idProIns);
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa controladora que recibe un id producto y retorna en formato JSON la informaci�n del producto, luego de invocar la capa DAO.
			 * @param idproducto Se recibe informaci�n del idproducto que desea consultar.
			 * @return Se retorna en formato JSON la informaci�n de la entidad producto consultada.
			 */
			public String retornarProducto(int idproducto)
			{
				JSONArray listJSON = new JSONArray();
				Producto Pro = ProductoDAO.retornarProducto(idproducto);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idproducto", Pro.getIdProducto());
				ResultadoJSON.put("idreceta", Pro.getIdReceta());
				ResultadoJSON.put("nombre", Pro.getNombre());
				ResultadoJSON.put("descripcion", Pro.getDescripcion());
				ResultadoJSON.put("impuesto", Pro.getImpuesto());
				ResultadoJSON.put("tipo", Pro.getTipo());
				ResultadoJSON.put("preciogeneral", Pro.getPreciogeneral());
				ResultadoJSON.put("incluye_liquido", Pro.getIncluye_liquido());
				ResultadoJSON.put("idtipo_liquido", Pro.getIdtipo_liquido());
				ResultadoJSON.put("habilitado", Pro.getHabilitado());
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			
			/**
			 * M�todo de la capa controladora que retorna en formato JSON todos los productos creados en la base de datos.
			 * @return Retorna en formato JSON todos los productos creados en la base de datos.
			 */
			public String retornarProductos(){
				JSONArray listJSON = new JSONArray();
				ArrayList<Producto> productos = ProductoDAO.obtenerProductos();
				for (Producto Pro : productos) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idproducto", Pro.getIdProducto());
					ResultadoJSON.put("idreceta", Pro.getIdReceta());
					ResultadoJSON.put("nombre", Pro.getNombre());
					ResultadoJSON.put("descripcion", Pro.getDescripcion());
					ResultadoJSON.put("impuesto", Pro.getImpuesto());
					ResultadoJSON.put("tipo", Pro.getTipo());
					ResultadoJSON.put("preciogeneral", Pro.getPreciogeneral());
					ResultadoJSON.put("incluye_liquido", Pro.getIncluye_liquido());
					ResultadoJSON.put("idtipo_liquido", Pro.getIdtipo_liquido());
					ResultadoJSON.put("habilitado", Pro.getHabilitado());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa Controladora que retorna en formato JSON todos los productos creados en la base de datos, con el objetivo de alimentar el GRID que soporta el CRUD de productos.		
			 * @return Retorna en formato JSON todos los productos creados en la base de datos.
			 */
			public String retornarProductosGrid(){
				JSONArray listJSON = new JSONArray();
				ArrayList<Producto> productos = ProductoDAO.obtenerProductosGrid();
				for (Producto Pro : productos) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idproducto", Pro.getIdProducto());
					ResultadoJSON.put("idreceta", Pro.getIdReceta());
					ResultadoJSON.put("nombrereceta", Pro.getNombrereceta());
					ResultadoJSON.put("nombre", Pro.getNombre());
					ResultadoJSON.put("descripcion", Pro.getDescripcion());
					ResultadoJSON.put("impuesto", Pro.getImpuesto());
					ResultadoJSON.put("tipo", Pro.getTipo());
					ResultadoJSON.put("preciogeneral", Pro.getPreciogeneral());
					ResultadoJSON.put("incluye_liquido", Pro.getIncluye_liquido());
					ResultadoJSON.put("idtipo_liquido", Pro.getIdtipo_liquido());
					ResultadoJSON.put("nombreliquido", Pro.getNombreliquido());
					ResultadoJSON.put("habilitado", Pro.getHabilitado());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa controladora que se encarga de recibir un idproducto y con el llamado a la capa DAO elimina un producto
			 * @param idproducto Se recibe como par�metro el idproducto que se desea eliminar.
			 * @return Retorna el resultado de la eliminaci�n del producto.
			 */
			public String eliminarProducto(int idproducto)
			{
				JSONArray listJSON = new JSONArray();
				ProductoDAO.eliminarProducto(idproducto);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", "exitoso");
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo que se encarga de modificar la informaci�n de un producto con base en el idproducto recibido como par�metro.
			 * @param idproducto Valor con base en el cual se realiza la modificaci�n.
			 * @param idreceta Valor de idreceta a modificar.
			 * @param nombre Valor de Nombre del producto a modificar.
			 * @param descripcion Valor de descripci�n  del producto a modificar.
			 * @param impuesto Valor de impuesto del producto a modificar.
			 * @param tipo Valro del tipo de producto a modificar.
			 * @param preciogeneral Valor del precio general del producto a modificar
			 * @param incluye_liquido 
			 * @param idtipo_liquido
			 * @return Se retorna el valor del resultado del producto.
			 */
			public String editarProducto(int idproducto,int idreceta,String nombre, String descripcion, float impuesto, String tipo, double preciogeneral, String incluye_liquido, int idtipo_liquido,String habilitado)
			{
				JSONArray listJSON = new JSONArray();
				Producto Pro = new Producto(idproducto,idreceta,nombre,descripcion, impuesto, tipo,0, preciogeneral, incluye_liquido, idtipo_liquido,"",habilitado);
				String resultado =ProductoDAO.editarProducto(Pro);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", resultado);
				listJSON.add(ResultadoJSON);
				System.out.println(listJSON.toJSONString());
				return listJSON.toJSONString();
			}
			
			//SABOR LIQUIDO
			
			/**
			 * Metodo de la capa Controladora que se encarga de Insertar un sabor Liquido con base en los par�metros recibidos.
			 * @param descripcion Valor de descripci�n del sabor liquido a insertar.
			 * @param idtipo_liquido Valor de tipo liquido asociado al sabor liquido a insertar.
			 * @return Retorna el idsabortipoliquido creado.
			 */
			public String insertarSaborLiquido(String descripcion, int idtipo_liquido)
			{
				JSONArray listJSON = new JSONArray();
				SaborLiquido Sab = new SaborLiquido(0,descripcion,idtipo_liquido);
				int idSabIns = SaborTipoLiquidoDAO.insertarSaborTipoLiquido(Sab);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idsabortipoliquido", idSabIns);
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa controladora que se encarga de retornar en formato JSON la entidad sator tipo liquido consultado con base en el valor del id sabor tipo liquido enviado.
			 * @param idsabortipoliquido Valor idsabortipoliquido con base en el cual se consulta.
			 * @return Retorna en formato JSON la entidad sator tipo liquido consultado con base en el valor del id sabor tipo liquido enviado.
			 */
			public String retornarSaborLiquido(int idsabortipoliquido)
			{
				JSONArray listJSON = new JSONArray();
				SaborLiquido Sab = SaborTipoLiquidoDAO.retornarSaborTipoLiquido(idsabortipoliquido);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idsabortipoliquido", Sab.getIdSaborTipoLiquido());
				ResultadoJSON.put("descripcion", Sab.getDescripcionSabor());
				ResultadoJSON.put("idtipoliquido", Sab.getIdLiquido());
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			
			/**
			 * M�todo de la capa controlador qeu se encarga de retonar en formato JSON los sabores liquidos creados en el sistema.
			 * @return Retonar en formato JSON los sabores liquidos creados en el sistema.
			 */
			public String retornarSaborLiquidos(){
				JSONArray listJSON = new JSONArray();
				ArrayList<SaborLiquido> sabores = SaborTipoLiquidoDAO.obtenerSaborLiquidos();
				for (SaborLiquido Sab : sabores) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idsabortipoliquido", Sab.getIdSaborTipoLiquido());
					ResultadoJSON.put("descripcion", Sab.getDescripcionSabor());
					ResultadoJSON.put("idtipoliquido", Sab.getIdLiquido());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
			/**
			 * Este m�todo de la capa controlador retorna la informaci�n en formato de JSON de los Sabores Tipo Liquido con el fin de ser desplegado en el CRUD de este par�metro.			
			 * @return Retorna la informaci�n en formato de JSON de los Sabores Tipo Liquido con el fin de ser desplegado en el CRUD de este par�metro.			
			 */
			public String retornarSaborLiquidosGrid(){
				JSONArray listJSON = new JSONArray();
				ArrayList<SaborLiquido> sabores = SaborTipoLiquidoDAO.obtenerSaborLiquidosGrid();
				for (SaborLiquido Sab : sabores) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idsabortipoliquido", Sab.getIdSaborTipoLiquido());
					ResultadoJSON.put("descripcion", Sab.getDescripcionSabor());
					ResultadoJSON.put("idtipoliquido", Sab.getIdLiquido());
					ResultadoJSON.put("nombreliquido", Sab.getDescripcionLiquido());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa controlador que se encarga de la eliminaci�n de un sabor tipo liquido con base en la identificaci�n
			 * @param idsaborxtipoliquido Recibe como par�metro el id sabor tipo liquido con base en el cual se realiza la eliminaci�n.
			 * @return Se retorna el resultado del proceso de eliminaci�n de sabor tipo liquido
			 */
			public String eliminarSaborLiquido(int idsaborxtipoliquido)
			{
				JSONArray listJSON = new JSONArray();
				SaborTipoLiquidoDAO.eliminarSaborTipoLiquido(idsaborxtipoliquido);;
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", "exitoso");
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo en la capa controlador que se encarga de recibir los par�metros para la edici�n de sabores tipo liquido, invocar la capa DAO y retornar el resultado del proceso.
			 * @param idsaborxtipoliquido Valor del id del sabor tipo liquido que se va a editar
			 * @param descripcion Valor de la descripci�n que se va  editar.
			 * @param idtipo_liquido id tipo liquido asociado al sabor tipo liquido que va a ser editado.
			 * @return Se retorna el resultado del proceso.
			 */
			public String editarSaborLiquido(int idsaborxtipoliquido, String descripcion, int idtipo_liquido)
			{
				JSONArray listJSON = new JSONArray();
				SaborLiquido Sab = new SaborLiquido(idsaborxtipoliquido,descripcion,idtipo_liquido);
				String resultado =SaborTipoLiquidoDAO.editarSaborTipoLiquido(Sab);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", resultado);
				listJSON.add(ResultadoJSON);
				System.out.println(listJSON.toJSONString());
				return listJSON.toJSONString();
			}	
			
//TIENDA
			
		
//TIPO LIQUIDO
			
			/**
			 * M�todo de la capa controlador que se encarga de recibir los par�metro para la inserci�n de la entidad tipo liquido, se encarga de invocar la capa DAO.
			 * @param nombre Par�metro con el valor del nombre tipo liquido
			 * @param capacidad Par�metro de la capacidad del tipo liquido.
			 * @return Se retorna en formato JSON el valor de id tipo liquido insertado.
			 */
			public String insertarTipoLiquido(String nombre, String capacidad)
			{
				JSONArray listJSON = new JSONArray();
				TipoLiquido Tipo = new TipoLiquido(0,nombre,capacidad);
				int idTipoIns = TipoLiquidoDAO.insertarTipoLiquido(Tipo);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idtipoliquido", idTipoIns);
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa controlador que se encarga 
			 * @param idtipo_liquido  par�metro con base en el cual se retorna el tipo liquido.
			 * @return Se retorna en formato JSON el tipo liquido con base en el id tipo liquido pasado como par�metro.
			 */
			public String retornarTipoLiquido(int idtipo_liquido)
			{
				JSONArray listJSON = new JSONArray();
				TipoLiquido Tipo = TipoLiquidoDAO.retornarTipoProducto(idtipo_liquido);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idtipoliquido", Tipo.getIdtipo_liquido());
				ResultadoJSON.put("nombre", Tipo.getNombre());
				ResultadoJSON.put("capacidad", Tipo.getCapacidad());
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			
			/**
			 * M�todo de la capa controladora que se encarga de retornar en formato JSON los tipos liquidos de la base de datos.
			 * @return Se retorna en formato JSON los tipos liquidos definidos en le sistema.
			 */
			public String retornarTiposLiquidos(){
				JSONArray listJSON = new JSONArray();
				ArrayList<TipoLiquido> tipos = TipoLiquidoDAO.obtenerTiposLiquido();
				for (TipoLiquido Tipo : tipos) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idtipoliquido", Tipo.getIdtipo_liquido());
					ResultadoJSON.put("nombre", Tipo.getNombre());
					ResultadoJSON.put("capacidad", Tipo.getCapacidad());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la capa controlador qeu se encarga de eliminar un tipo liquido invocando la capa DAO.			
			 * @param idtipo_liquido Se recibe como par�metro del id tipo liquido con base en el cual se realiza la eliminaci�n
			 * @return Retorna en formato JSON el resultado del proceso de eliminaci�n.
			 */
			public String eliminarTipodLiquido(int idtipo_liquido)
			{
				JSONArray listJSON = new JSONArray();
				TipoLiquidoDAO.eliminarTipoLiquido(idtipo_liquido);;
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", "exitoso");
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			/**
			 * M�todo de la clase controladora que se encarga de editar un tipo liquido invocando la capa DAO con los par�metros.
			 * @param idtipo_liquido Par�metro con base en el cual se realiza la edici�n del tipo liquido
			 * @param nombre Par�metro del nombre del tipo liquido a modificar.
			 * @param capacidad Par�metro de la capacidad del tipo liquido a modificar.
			 * @returnS Se retorna en formato JSON el resultado del proceso.
			 */
			public String editarTipoLiquido(int idtipo_liquido, String nombre, String capacidad)
			{
				JSONArray listJSON = new JSONArray();
				TipoLiquido Tipo = new TipoLiquido(idtipo_liquido,nombre,capacidad);
				String resultado =TipoLiquidoDAO.editarTipoLiquido(Tipo);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", resultado);
				listJSON.add(ResultadoJSON);
				System.out.println(listJSON.toJSONString());
				return listJSON.toJSONString();
			}
			
			//MUNICIPIOS
			
			/**
			 * M�todo de la capa controladora que se encarga mediante la invocaci�n de la capa DAO de retornar las entidades municipio creadas en el sistema.
			 * @return Retorna en formato DAO los municipios creados en el sistema.
			 */
			public String retornarMunicipios(){
				JSONArray listJSON = new JSONArray();
				ArrayList<Municipio> municipios = MunicipioDAO.obtenerMunicipios();
				for (Municipio municipio : municipios) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idmunicipio", municipio.getIdmunicipio());
					ResultadoJSON.put("nombre",municipio.getNombre());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
			//PRODUCTOS NO EXISTENTES
			
			public String insertaProductoNoExistente(int idtienda, int idproducto)
			{
				JSONArray listJSON = new JSONArray();
				ProductoNoExistente Pro = new ProductoNoExistente(idtienda, "" , idproducto, "");
				ProductoNoExistenteDAO.insertaProductoNoExistente(Pro);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("insertado", "true");
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			
			public String retornarProductoNoExistente(int idtienda, int idproducto)
			{
				JSONArray listJSON = new JSONArray();
				ProductoNoExistente Pro = ProductoNoExistenteDAO.retornarProductoNoExistente(idtienda, idproducto);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("idtienda", Pro.getIdtienda());
				ResultadoJSON.put("tienda", Pro.getTienda());
				ResultadoJSON.put("idproducto", Pro.getIdproducto());
				ResultadoJSON.put("producto", Pro.getProducto());
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			
			
			public String retornarProductosNoExistentes(){
				JSONArray listJSON = new JSONArray();
				ArrayList<ProductoNoExistente> productos = ProductoNoExistenteDAO.retornarProductosNoExistentes();
				for (ProductoNoExistente Pro : productos) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idtienda", Pro.getIdtienda());
					ResultadoJSON.put("tienda", Pro.getTienda());
					ResultadoJSON.put("idproducto", Pro.getIdproducto());
					ResultadoJSON.put("producto", Pro.getProducto());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
		
			public String retornarProductosNoExistentesGrid(){
				JSONArray listJSON = new JSONArray();
				ArrayList<ProductoNoExistente> productos = ProductoNoExistenteDAO.obtenerProductosNoExistentesGrid();
				for (ProductoNoExistente Pro : productos) 
				{
					JSONObject ResultadoJSON = new JSONObject();
					ResultadoJSON.put("idtienda", Pro.getIdtienda());
					ResultadoJSON.put("tienda", Pro.getTienda());
					ResultadoJSON.put("idproducto", Pro.getIdproducto());
					ResultadoJSON.put("producto", Pro.getProducto());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
			}
			
		
			public String eliminarProductoNoExistente(int idtienda, int idproducto)
			{
				JSONArray listJSON = new JSONArray();
				ProductoNoExistenteDAO.eliminarProductoNoExistente(idtienda,idproducto);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", "exitoso");
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			
			public String editarProductoNoExistente(int idtienda, int idproducto)
			{
				JSONArray listJSON = new JSONArray();
				ProductoNoExistente Pro = new ProductoNoExistente(idtienda,"",idproducto, "");
				String resultado =ProductoNoExistenteDAO.editarProductoNoExistente(Pro);
				JSONObject ResultadoJSON = new JSONObject();
				ResultadoJSON.put("resultado", resultado);
				listJSON.add(ResultadoJSON);
				return listJSON.toJSONString();
			}
			
			public String retornarTiempoPedido()
			{
				JSONArray listJSON = new JSONArray();
				ArrayList <TiempoPedido> tiempos = TiempoPedidoDAO.retornarTiemposPedidos();
				for (TiempoPedido tiempo : tiempos) 
				{
					JSONObject Respuesta = new JSONObject();
					Respuesta.put("tiempopedido", tiempo.getMinutosPedido());
					Respuesta.put("tienda", tiempo.getTienda());
					Respuesta.put("idtienda", tiempo.getIdtienda());
					Respuesta.put("estado", tiempo.getEstado());
					listJSON.add(Respuesta);
				}
				return(listJSON.toString());
			}
			
			public String actualizarTiempoPedido(int nuevotiempo, int idtienda, String user)
			{
				JSONArray listJSON = new JSONArray();
				JSONObject Respuesta = new JSONObject();
				boolean respues = TiempoPedidoDAO.actualizarTiempoPedido(nuevotiempo, idtienda, user);
				//Realizaremos la validaci�n para env�o de correo
				String respuestaHTML = "";
				if(respues)
				{
					if (nuevotiempo > 70)
					{
						Date fechaActual = new Date();
						Correo correo = new Correo();
						CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOPUBLICA", "CLAVECORREOPUBLICA");
						correo.setAsunto("ALERTA TIEMPOS PEDIDO " + fechaActual);
						ArrayList correos = GeneralDAO.obtenerCorreosParametro("TIEMPOPEDIDO");
						correo.setContrasena(infoCorreo.getClaveCorreo());
						correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
						Tienda tienda = TiendaDAO.retornarTienda(idtienda);
						correo.setMensaje("La tienda " + tienda.getNombreTienda() + " est� aumentando el tiempo de entrega a " + nuevotiempo + " minutos");
						ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
						contro.enviarCorreo();
					}
					respuestaHTML = "<html><b> <h1 align='center'><i>EL TIEMPO SE HA ACTUALIZADO CORRECTAMENTE EN EL CONTACT CENTER";
				}
				else
				{
					Correo correo = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOPUBLICA", "CLAVECORREOPUBLICA");
					correo.setAsunto("ERROR ACTUALIZACION TIEMPO PEDIDO");
					ArrayList correos = GeneralDAO.obtenerCorreosParametro("TIEMPOPEDIDOERROR");
					correo.setContrasena(infoCorreo.getClaveCorreo());
					correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
					Tienda tienda = TiendaDAO.retornarTienda(idtienda);
					correo.setMensaje("La tienda " + tienda.getNombreTienda() + " TUVO UN ERROR CUANDO ESTABA aumentando el tiempo de entrega a " + nuevotiempo + " minutos");
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					contro.enviarCorreo();
				}
				Respuesta.put("resultado", respuestaHTML);
				listJSON.add(Respuesta);
				return(Respuesta.toString());
			}
			
			public String retornarTiempoPedidoTienda(int idtienda)
			{
				JSONArray listJSON = new JSONArray();
				JSONObject Respuesta = new JSONObject();
				int respues = TiempoPedidoDAO.retornarTiempoPedidoTienda( idtienda);
				Respuesta.put("tiempopedido", respues);
				listJSON.add(Respuesta);
				return(Respuesta.toString());
			}
			
			public String insertarDirFueraZona(String direccion, String municipio, int idCliente, double latitud, double longitud, String telefono, String nombre, String apellido)
			{
				DireccionFueraZona dirFuera = new DireccionFueraZona(0, direccion, municipio, idCliente, latitud, longitud, telefono, nombre, apellido,0);
				int id = DireccionFueraZonaDAO.insertarDirFueraZona(dirFuera);
				JSONArray listJSON = new JSONArray();
				JSONObject Respuesta = new JSONObject();
				Respuesta.put("id", id);
				listJSON.add(Respuesta);
				return(Respuesta.toString());
			}
			
			public String ConsultaDirFueraZona(String fechainicial, String fechafinal, String municipio)
			{
				ArrayList <DireccionFueraZona> dirsFuera = DireccionFueraZonaDAO.ConsultaDirFueraZona(fechainicial, fechafinal, municipio);
				JSONArray listJSON = new JSONArray();
				JSONObject ResultadoJSON = new JSONObject();
				for (DireccionFueraZona dirTemp : dirsFuera) 
				{
					ResultadoJSON = new JSONObject();
					ResultadoJSON.put("id", dirTemp.getId());
					ResultadoJSON.put("direccion", dirTemp.getDireccion());
					ResultadoJSON.put("municipio", dirTemp.getMunicipio());
					ResultadoJSON.put("idcliente", dirTemp.getIdCliente());
					ResultadoJSON.put("latitud", dirTemp.getLatitud());
					ResultadoJSON.put("longitud", dirTemp.getLongitud());
					ResultadoJSON.put("telefono", dirTemp.getTelefono());
					ResultadoJSON.put("nombre", dirTemp.getNombre());
					ResultadoJSON.put("apellido", dirTemp.getApellido());
					ResultadoJSON.put("fecha_ingreso", dirTemp.getFechaIngreso());
					listJSON.add(ResultadoJSON);
				}
				return listJSON.toJSONString();
				
			}
			
			//Manejo de tema de par�metros
			public boolean EditarParametro(Parametro parametro)
			{
				boolean respuesta = ParametrosDAO.EditarParametro(parametro);
				return(respuesta);
			}
			
			public boolean eliminarParametro(String valorParametro)
			{
				boolean respuesta = ParametrosDAO.eliminarParametro(valorParametro);
				return(respuesta);
			}
			
			public boolean insertarParametro(Parametro parametro)
			{
				boolean respuesta = ParametrosDAO.insertarParametro(parametro);
				return(respuesta);
			}
			
			public String obtenerParametro(String valorParametro)
			{
				JSONObject ResultadoJSON = new JSONObject();
				Parametro parametro = ParametrosDAO.obtenerParametro(valorParametro);
				ResultadoJSON.put("valortexto", parametro.getValorTexto());
				ResultadoJSON.put("valornumerico", parametro.getValorNumerico());
				System.out.println(ResultadoJSON.toJSONString());
				return ResultadoJSON.toJSONString();
			}
			
			public ArrayList obtenerParametros()
			{
				ArrayList parametros = ParametrosDAO.obtenerParametros();
				return parametros;
			}
			
			/**
			 * M�todo que se encarga de realizar la homologaci�n desde capa de controlador de el SKU y su correspondiente producto.
			 * @param sku
			 * @return
			 */
			public int homologarProductoTiendaVirtual(String sku)
			{
				int idProducto = ProductoDAO.homologarProductoTiendaVirtual(sku);
				return(idProducto);
			}
			
			/**
			 * M�todo que se encarga de realizar la homologaci�n desde capa de controlador de el SKU y su correspondiente especialidad.
			 * @param sku
			 * @return
			 */
			public int homologarEspecialidadTiendaVirtual(String sku)
			{
				int idEspecialidad = EspecialidadDAO.homologarEspecialidadTiendaVirtual(sku);
				return(idEspecialidad);
			}
			
			public boolean esPromocionTiendaVirtual(String sku)
			{
				boolean respuesta = EspecialidadDAO.esPromocionTiendaVirtual(sku);
				return(respuesta);
			}
			
			/**
			 * M�todo que se encarga de realizar la homologaci�n desde capa de controlador de el SKU y su correspondiente excepci�n de precio.
			 * @param sku
			 * @return
			 */
			public ExcepcionPrecio homologarExcepcionTiendaVirtual(String sku)
			{
				ExcepcionPrecio excepcion= ExcepcionPrecioDAO.homologarExcepcionTiendaVirtual(sku);
				return(excepcion);
			}
			
			/**
			 * M�todo que se encarga de realizar la homologaci�n desde capa de controlador de el SKU y su correspondiente sabor tipo liquido con el tipo de producto T TIPO LIQUIDO.
			 * @param sku
			 * @return
			 */
			public int homologarLiquidoTiendaVirtual(String sku)
			{
				int idSabor = SaborTipoLiquidoDAO.homologarLiquidoTiendaVirtual(sku);
				return(idSabor);
			}
			
			/**
			 * M�todo que tendr� como objetivo realizar la devoluci�n de los posibles hoarios, dada la hora actual, y el d�a en que nos
			 * encontramos con base en los horarios que se manejan para dicho d�a.
			 * @return
			 */
			public String obtenerHorarioProgramacionPizza()
			{
				//El retorno ser� un JSON con las posibles horas a partir de las cuales se puede programar un pedido.
				JSONArray listJSON = new JSONArray();
				ArrayList<HorarioDia> horariosDia = HorarioDiaDAO.obtenerHorariosDia();
				//Posteriormente fijamos el calendario para la fecha en la que estamos
				Calendar calendarioActual = Calendar.getInstance();
				int diaActual = calendarioActual.get(Calendar.DAY_OF_WEEK);
				for(int i = 0; i < horariosDia.size(); i++)
				{
					HorarioDia horarioDiaTemp = horariosDia.get(i);
					if(horarioDiaTemp.getIdDia() == diaActual)
					{
						//En este punto ya estaremos ubicados en el d�a del cual queremos sacar los horarios
						//para programaci�n
						String horaApertura = horarioDiaTemp.getHoraApertura();
						String horaCierre = horarioDiaTemp.getHoraCierre();
						int horaAperturaDep =  Integer.parseInt((new StringTokenizer(horaApertura, ":")).nextToken());
						int horaCierreDep =  Integer.parseInt((new StringTokenizer(horaCierre, ":")).nextToken());
						Date fechaActual = new Date();
						int horaActual = fechaActual.getHours();
						int minutosActual = fechaActual.getMinutes();
						JSONObject cadaOpcionHorarioJSON = new JSONObject();
						//Tendremos un valor booleano que ser� una marca para saber como nos fue con la primera hora
						boolean primeraHora = false;
						while(horaActual < horaCierreDep)
						{
							//Haremos la validaci�n si la hora actual es menor a la fecha de apertura
							if((horaActual < horaAperturaDep))
							{
								primeraHora = true;
								horaActual = horaAperturaDep  +1;
								cadaOpcionHorarioJSON = new JSONObject();
								cadaOpcionHorarioJSON.put("hora", horaActual+":"+"00");
								listJSON.add(cadaOpcionHorarioJSON);
								cadaOpcionHorarioJSON = new JSONObject();
								cadaOpcionHorarioJSON.put("hora", horaActual+":"+"30");
								listJSON.add(cadaOpcionHorarioJSON);
							}else if((horaActual >= horaAperturaDep) && ((horaActual+1) < horaCierreDep))
							{
								horaActual = horaActual  +1;
								if(!primeraHora)
								{
									//validaremos los minutos para ver que podemos hacer en cuanto al tiempo
									if(minutosActual > 30)
									{

									}else
									{
										cadaOpcionHorarioJSON = new JSONObject();
										cadaOpcionHorarioJSON.put("hora", horaActual+":"+"30");
										listJSON.add(cadaOpcionHorarioJSON);
									}
									primeraHora = true;
								}else
								{
									cadaOpcionHorarioJSON = new JSONObject();
									cadaOpcionHorarioJSON.put("hora", horaActual+":"+"00");
									listJSON.add(cadaOpcionHorarioJSON);
									cadaOpcionHorarioJSON = new JSONObject();
									cadaOpcionHorarioJSON.put("hora", horaActual+":"+"30");
									listJSON.add(cadaOpcionHorarioJSON);
								}	
							}else if(horaActual +1 == horaCierreDep)
							{
								horaActual = horaActual +1;
							}
							
						}
						break;	
					}
				}
				System.out.println(listJSON.toJSONString());
				return(listJSON.toJSONString());
			}
			
			public static void main(String args[])
			{
				ParametrosCtrl parCtrl = new ParametrosCtrl();
				parCtrl.obtenerHorarioProgramacionPizza();
			}
			
			
			public static String obtenerTiposPedido()
			{
				JSONArray listJSON = new JSONArray();
				ArrayList<TipoPedido> tiposPedido = new ArrayList();
				tiposPedido = TipoPedidoDAO.obtenerTiposPedido();
				for (TipoPedido tipPedido : tiposPedido) 
				{
					JSONObject cadaTipoJSON = new JSONObject();
					cadaTipoJSON.put("idtipopedido", tipPedido.getIdTipoPedido());
					cadaTipoJSON.put("nombre", tipPedido.getNombre());
					cadaTipoJSON.put("tipopago", tipPedido.getTipoPago());
					cadaTipoJSON.put("validadir", tipPedido.getValidaDir());
					listJSON.add(cadaTipoJSON);
				}
				return listJSON.toJSONString();
			}
			
			public String obtenerInformacionIntegracion(String crm)
			{
				IntegracionCRM integra = IntegracionCRMDAO.obtenerInformacionIntegracion(crm);
				JSONObject respuestaJSON = new JSONObject();
				respuestaJSON.put("accesstoken", integra.getAccessToken());
				respuestaJSON.put("clientid", integra.getClientID());
				respuestaJSON.put("freshtoken", integra.getFreshToken());
				return(respuestaJSON.toJSONString());
			}
}

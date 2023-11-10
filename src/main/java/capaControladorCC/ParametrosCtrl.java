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
 * Esta clase está en la capa controlador y se encarga de todos los parámetros de la solución
 *
 */
public class ParametrosCtrl {
	
	//FORMAPAGO
	
	/**
	 * Método en la capa controlador que se encarga de retornar la información de las formas de pago en formato JSON
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
	 * Método en la capa controlador que recibe parámetros desde la capa de Presentación para la creación de especialidades
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
	 * @param idespecialidad Recibe como parámetro el idespecialidad que se va  a consultar
	 * @return Retorna en formato JSON la información de la especialidad de acuerdo al parámetro de idespecialidad.
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
	 * @param idespecialidad Se pasa como parámetro el idespecialidad que desea se eliminado
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
	 * @param idespecialidad Parámetro con base en el cual se edita la especialidad.
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
	 * Método para la inserción de estado pedido en la capa de controlador
	 * @param descripcion Se recibe como parámetro el valor de la descripción del estado.
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
	 * Método que se encarga de retornar la información de un estado pedido determinado de acuerdo al parámetro idestadopedido
	 * @param idestadopedido Se recibe como parámetro el idestadopedido a consultar.
	 * @return Se retona en formato JSON la información del estado pedido a consutar.
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
	 * Método que retorna en formato JSON los estados pedidos creados en la base de datos en capa controlador
	 * @return Método que retorna en formato JSON los estados pedidos creados en la base de datos en capa controlador
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
	 * Método en capa controlador que recide el idestadopedido a eliminar y se encarga de llamar al método en la capa DAO, retona el valor exitoso en caso de finalizar bien.
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
	 * Metódo en capa controlador que recibe los valores de estado a pedido a editar y se encarga de llamar al método correspondiente en la capa DAO.
	 * @param idestadopedido Se recibe el idestadopedido a editar.
	 * @param descripcion Se recibe el valor de la descripción del estado pedido que se va a editar.
	 * @return Se retorna el valor del resultado de la edición.
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
	 * Para la entidad Excepción Precio se reciben los parámetros para inserción y se invoca al método correspondiente en la capa DAO.
	 * @param idproducto Se recibe como parámetro idproducto asociado a la excepción de precio 
	 * @param precio Se recibe el valor del precio de la excepción
	 * @param descripcion Se recibe la descripcion de la excepción.
	 * @param incluye_liquido Se recibe si la excepción de precio incluye o no líquido.
	 * @param idtipoliquido En caso de incluir líquido se recibe parámetro del tipo liquido que incluye.
	 * @return Se retorna el id excepción creado según los parámetros enviados.
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
		 * Método en la capa controlador que se encarga de recibir el id excepción a consultar y de invocar el método en clase DAO que ejecuta la acción.
		 * @param idexcepcion Se recibe el valor del idexcepción con base en el cual se retonará en formato JSON la entidad Excepción Precio.
		 * @return Se retorna en formato JSON la información de la entidad Excepcion Precio a consultar según el parámetro entregado.
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
		 * Método en capa controlador que se encarga de retonar en formato JSON la información de las entidades Excepción Precio, luego del llamado a la capa DAO.
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
		 * Método en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocación al método en la capa DAO.
		 * @return Método en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocación al método en la capa DAO.
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
		 * Método en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocación al método en la capa DAO.
		 * @return Método en capa controlador  que se encarga de retonar en formato JSON los tipos liquidos de la base de datos, con base en la invocación al método en la capa DAO.
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
		 * Método que se encarga de recibir el idexcepcion Precio a eliminar, invocar la capa DAO y retonar el resultado del proceso.
		 * @param idexcepcion Se recibe el idexcepcion que será eliminado.
		 * @return Se retorna el resultado de la eliminación.
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
		 * Método en la capa controlador que se encarga de editar la excepción, se reciben los parámetros y se invoca la capa DAO.
		 * @param idexcepcion Valor de idexcepcion a editar.
		 * @param idproducto idproducto valor a editar.
		 * @param precio precio valor a editar
		 * @param descripcion descripcion valor a editar.
		 * @param incluye_liquido valor incluye_liquido valor a editar
		 * @param idtipoliquido idtipoliquido valor a  editar
		 * @return se retorna el resultado del proceso de la edición.
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
		 * Se reciben los parámetros para la inserción del producto, se invoca a la capa DAO y se retorna el idproducto insertado
		 * @param idreceta Parámetro la inserción del producto, id receta asociado al producto.
		 * @param nombre Nombre del producto a insertar.
		 * @param descripcion Descripción del producto a insertar.
		 * @param impuesto Impuesto que manejará el producto a insertar.
		 * @param tipo Clasificación asociado al producto a insertar, podrá ser PIZZAS, OTROS, MODIFICADORES CON etc
		 * @param preciogeneral Precio General que manejará al producto
		 * @param incluye_liquido Se recibe el parámetro si el producto manejará  o no liquido
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
			 * Método de la capa controladora que recibe un id producto y retorna en formato JSON la información del producto, luego de invocar la capa DAO.
			 * @param idproducto Se recibe información del idproducto que desea consultar.
			 * @return Se retorna en formato JSON la información de la entidad producto consultada.
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
			 * Método de la capa controladora que retorna en formato JSON todos los productos creados en la base de datos.
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
			 * Método de la capa Controladora que retorna en formato JSON todos los productos creados en la base de datos, con el objetivo de alimentar el GRID que soporta el CRUD de productos.		
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
			 * Método de la capa controladora que se encarga de recibir un idproducto y con el llamado a la capa DAO elimina un producto
			 * @param idproducto Se recibe como parámetro el idproducto que se desea eliminar.
			 * @return Retorna el resultado de la eliminación del producto.
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
			 * Método que se encarga de modificar la información de un producto con base en el idproducto recibido como parámetro.
			 * @param idproducto Valor con base en el cual se realiza la modificación.
			 * @param idreceta Valor de idreceta a modificar.
			 * @param nombre Valor de Nombre del producto a modificar.
			 * @param descripcion Valor de descripción  del producto a modificar.
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
			 * Metodo de la capa Controladora que se encarga de Insertar un sabor Liquido con base en los parámetros recibidos.
			 * @param descripcion Valor de descripción del sabor liquido a insertar.
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
			 * Método de la capa controladora que se encarga de retornar en formato JSON la entidad sator tipo liquido consultado con base en el valor del id sabor tipo liquido enviado.
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
			 * Método de la capa controlador qeu se encarga de retonar en formato JSON los sabores liquidos creados en el sistema.
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
			 * Este método de la capa controlador retorna la información en formato de JSON de los Sabores Tipo Liquido con el fin de ser desplegado en el CRUD de este parámetro.			
			 * @return Retorna la información en formato de JSON de los Sabores Tipo Liquido con el fin de ser desplegado en el CRUD de este parámetro.			
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
			 * Método de la capa controlador que se encarga de la eliminación de un sabor tipo liquido con base en la identificación
			 * @param idsaborxtipoliquido Recibe como parámetro el id sabor tipo liquido con base en el cual se realiza la eliminación.
			 * @return Se retorna el resultado del proceso de eliminación de sabor tipo liquido
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
			 * Método en la capa controlador que se encarga de recibir los parámetros para la edición de sabores tipo liquido, invocar la capa DAO y retornar el resultado del proceso.
			 * @param idsaborxtipoliquido Valor del id del sabor tipo liquido que se va a editar
			 * @param descripcion Valor de la descripción que se va  editar.
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
			 * Método de la capa controlador que se encarga de recibir los parámetro para la inserción de la entidad tipo liquido, se encarga de invocar la capa DAO.
			 * @param nombre Parámetro con el valor del nombre tipo liquido
			 * @param capacidad Parámetro de la capacidad del tipo liquido.
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
			 * Método de la capa controlador que se encarga 
			 * @param idtipo_liquido  parámetro con base en el cual se retorna el tipo liquido.
			 * @return Se retorna en formato JSON el tipo liquido con base en el id tipo liquido pasado como parámetro.
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
			 * Método de la capa controladora que se encarga de retornar en formato JSON los tipos liquidos de la base de datos.
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
			 * Método de la capa controlador qeu se encarga de eliminar un tipo liquido invocando la capa DAO.			
			 * @param idtipo_liquido Se recibe como parámetro del id tipo liquido con base en el cual se realiza la eliminación
			 * @return Retorna en formato JSON el resultado del proceso de eliminación.
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
			 * Método de la clase controladora que se encarga de editar un tipo liquido invocando la capa DAO con los parámetros.
			 * @param idtipo_liquido Parámetro con base en el cual se realiza la edición del tipo liquido
			 * @param nombre Parámetro del nombre del tipo liquido a modificar.
			 * @param capacidad Parámetro de la capacidad del tipo liquido a modificar.
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
			 * Método de la capa controladora que se encarga mediante la invocación de la capa DAO de retornar las entidades municipio creadas en el sistema.
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
				//Realizaremos la validación para envío de correo
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
						correo.setMensaje("La tienda " + tienda.getNombreTienda() + " está aumentando el tiempo de entrega a " + nuevotiempo + " minutos");
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
			
			//Manejo de tema de parámetros
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
			 * Método que se encarga de realizar la homologación desde capa de controlador de el SKU y su correspondiente producto.
			 * @param sku
			 * @return
			 */
			public int homologarProductoTiendaVirtual(String sku)
			{
				int idProducto = ProductoDAO.homologarProductoTiendaVirtual(sku);
				return(idProducto);
			}
			
			/**
			 * Método que se encarga de realizar la homologación desde capa de controlador de el SKU y su correspondiente especialidad.
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
			 * Método que se encarga de realizar la homologación desde capa de controlador de el SKU y su correspondiente excepción de precio.
			 * @param sku
			 * @return
			 */
			public ExcepcionPrecio homologarExcepcionTiendaVirtual(String sku)
			{
				ExcepcionPrecio excepcion= ExcepcionPrecioDAO.homologarExcepcionTiendaVirtual(sku);
				return(excepcion);
			}
			
			/**
			 * Método que se encarga de realizar la homologación desde capa de controlador de el SKU y su correspondiente sabor tipo liquido con el tipo de producto T TIPO LIQUIDO.
			 * @param sku
			 * @return
			 */
			public int homologarLiquidoTiendaVirtual(String sku)
			{
				int idSabor = SaborTipoLiquidoDAO.homologarLiquidoTiendaVirtual(sku);
				return(idSabor);
			}
			
			/**
			 * Método que tendrá como objetivo realizar la devolución de los posibles hoarios, dada la hora actual, y el día en que nos
			 * encontramos con base en los horarios que se manejan para dicho día.
			 * @return
			 */
			public String obtenerHorarioProgramacionPizza()
			{
				//El retorno será un JSON con las posibles horas a partir de las cuales se puede programar un pedido.
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
						//En este punto ya estaremos ubicados en el día del cual queremos sacar los horarios
						//para programación
						String horaApertura = horarioDiaTemp.getHoraApertura();
						String horaCierre = horarioDiaTemp.getHoraCierre();
						int horaAperturaDep =  Integer.parseInt((new StringTokenizer(horaApertura, ":")).nextToken());
						int horaCierreDep =  Integer.parseInt((new StringTokenizer(horaCierre, ":")).nextToken());
						Date fechaActual = new Date();
						int horaActual = fechaActual.getHours();
						int minutosActual = fechaActual.getMinutes();
						JSONObject cadaOpcionHorarioJSON = new JSONObject();
						//Tendremos un valor booleano que será una marca para saber como nos fue con la primera hora
						boolean primeraHora = false;
						while(horaActual < horaCierreDep)
						{
							//Haremos la validación si la hora actual es menor a la fecha de apertura
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
}

package capaControladorCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.JOptionPane;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import okio.Buffer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaDAOCC.AdicionDetallePedidoDAO;
import capaDAOCC.ClienteDAO;
import capaDAOCC.DescuentoGeneralDAO;
import capaDAOCC.DomiciliarioPedidoDAO;
import capaDAOCC.EmpleadoRemotoValeDAO;
import capaDAOCC.EspecialidadDAO;
import capaDAOCC.EstadisticaPromocionDAO;
import capaDAOCC.ExcepcionPrecioDAO;
import capaDAOCC.FormaPagoDAO;
import capaDAOCC.GeneralDAO;
import capaDAOCC.HomologacionProductoRappiDAO;
import capaDAOCC.HomologacionTiendaTokenDAO;
import capaDAOCC.IntegracionCRMDAO;
import capaDAOCC.IntegracionCRMHomologaAsesorDAO;
import capaDAOCC.LogEventoWompiDAO;
import capaDAOCC.LogPedidoVirtualDAO;
import capaDAOCC.LogPedidoVirtualKunoDAO;
import capaDAOCC.MarcacionPedidoDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.PedidoGestionLinkDAO;
import capaDAOCC.PedidoPagoVirtualConsolidadoDAO;
import capaDAOCC.PedidoPagoVirtualDAO;
import capaDAOCC.PedidoPrecioEmpleadoDAO;
import capaDAOCC.PedidoTiendaVirtualDAO;
import capaDAOCC.ProductoDAO;
import capaDAOCC.PromocionDAO;
import capaDAOCC.SolicitudFacturaDAO;
import capaDAOCC.SolicitudFacturaImagenesDAO;
import capaDAOCC.TiempoPedidoDAO;
import capaDAOCC.TiendaDAO;
import capaDAOCC.TmpPedidosPoligonoDAO;
import capaModeloCC.AdicionTiendaVirtual;
import capaModeloCC.Cliente;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.DescuentoGeneral;
import capaModeloCC.DetallePedido;
import capaModeloCC.DetallePedidoAdicion;
import capaModeloCC.DetallePedidoPixel;
import capaModeloCC.DireccionFueraZona;
import capaModeloCC.DomiciliarioPedido;
import capaModeloCC.Especialidad;
import capaModeloCC.Estadistica;
import capaModeloCC.EstadisticaPromocion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FormaPago;
import capaModeloCC.HomologaGaseosaIncluida;
import capaModeloCC.HomologacionProductoRappi;
import capaModeloCC.HomologacionTiendaToken;
import capaModeloCC.InsertarPedidoPixel;
import capaModeloCC.IntegracionCRM;
import capaModeloCC.LogEventoWompi;
import capaModeloCC.MarcacionPedido;
import capaModeloCC.ModificadorDetallePedido;
import capaModeloCC.NomenclaturaDireccion;
import capaModeloCC.Pedido;
import capaModeloCC.PedidoMonitoreoPagoVirtual;
import capaModeloCC.PedidoPagoVirtual;
import capaModeloCC.PedidoPlat;
import capaModeloCC.PedidoPrecioEmpleado;
import capaModeloCC.Producto;
import capaModeloCC.ProductoIncluido;
import capaModeloCC.Promocion;
import capaModeloCC.Resultado;
import capaModeloCC.ResumenVentaEmpresarial;
import capaModeloCC.SaborLiquido;
import capaModeloCC.SolicitudFactura;
import capaModeloCC.SolicitudFacturaImagenes;
import capaModeloCC.SolicitudPQRSImagenes;
import capaModeloCC.TiempoPedido;
import capaModeloCC.Tienda;
import capaModeloCC.Ubicacion;
import capaControladorPOS.BiometriaCtrl;
import capaControladorPOS.EmpleadoCtrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import utilidadesCC.ControladorEnvioCorreo;

public class PedidoCtrl {

	boolean cobroVariosDomicilios = false; 
	
	public String obtenerEspecialidades(int idExcepcion, int idProducto){
		JSONArray listJSON = new JSONArray();
		ArrayList<Especialidad> especialidades = PedidoDAO.obtenerEspecialidad(idExcepcion, idProducto);
		for (Especialidad espe : especialidades) {
			JSONObject cadaViajeJSON = new JSONObject();
			cadaViajeJSON.put("idespecialidad", espe.getIdespecialidad());
			cadaViajeJSON.put("nombre", espe.getNombre());
			cadaViajeJSON.put("abreviatura", espe.getAbreviatura());
			listJSON.add(cadaViajeJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	
	public String obtenerNomenclaturaDireccion(){
		JSONArray listJSON = new JSONArray();
		ArrayList<NomenclaturaDireccion> nomenclaturas = PedidoDAO.obtenerNomenclaturaDireccion();
		for (NomenclaturaDireccion nomen : nomenclaturas) {
			JSONObject cadaNomenJSON = new JSONObject();
			cadaNomenJSON.put("idnomenclatura", nomen.getIdnomemclatura());
			cadaNomenJSON.put("nomenclatura", nomen.getNomenclatura());
			listJSON.add(cadaNomenJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	public String actualizarEstadoNumeroPedidoPixel(int idpedido, int numPedidoPixel, boolean creaCliente, int membercode, int idcliente)
	{
		JSONArray listJSON = new JSONArray();
		boolean resp = PedidoDAO.actualizarEstadoNumeroPedidoPixel(idpedido, numPedidoPixel);
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("resultado", resp);
		listJSON.add(Respuesta);
		if (creaCliente)
		{
			PedidoDAO.actualizarClienteMemcode(idcliente, membercode);
		}
		return(listJSON.toJSONString());
		
	}
	
	public String obtenerEstadoEnviadoPixel(int idpedido)
	{
		JSONArray listJSON = new JSONArray();
		int enviadoPixel = PedidoDAO.obtenerEstadoEnviadoPixel(idpedido);
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("enviadopixel", enviadoPixel);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
		
	}
	
	public String cancelarPedido(int idpedido)
	{
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		boolean resp = PedidoDAO.cancelarPedido(idpedido);
		Respuesta.put("resultado", resp);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String actualizarMemcode(int membercode, int idcliente)
	{
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		PedidoDAO.actualizarClienteMemcode(idcliente, membercode);
		Respuesta.put("resultado", "OK");
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
		
	}
	
	
	public String obtenerTodosProductos(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Producto> otrosProductos = PedidoDAO.obtenerTodosProductos();
		for (Producto produ : otrosProductos) {
			JSONObject cadaProductoJSON = new JSONObject();
			cadaProductoJSON.put("idproducto", produ.getIdProducto());
			cadaProductoJSON.put("idreceta", produ.getIdReceta());
			cadaProductoJSON.put("nombre", produ.getNombre());
			cadaProductoJSON.put("descripcion", produ.getDescripcion());
			cadaProductoJSON.put("impuesto", produ.getImpuesto());
			cadaProductoJSON.put("tipo", produ.getTipo());
			cadaProductoJSON.put("productoasociaadicion", produ.getProductoasociaadicion());
			cadaProductoJSON.put("preciogeneral", produ.getPreciogeneral());
			cadaProductoJSON.put("incluye_liquido", produ.getIncluye_liquido());
			cadaProductoJSON.put("idtipo_liquido", produ.getIdtipo_liquido());
			cadaProductoJSON.put("manejacantidad", produ.getManejacantidad());
			listJSON.add(cadaProductoJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	public String GetProductosTienda(int idtienda){
		JSONArray listJSON = new JSONArray();
		ArrayList<Producto> otrosProductos = ProductoDAO.GetProductosTienda(idtienda);
		for (Producto produ : otrosProductos) {
			JSONObject cadaProductoJSON = new JSONObject();
			cadaProductoJSON.put("idproducto", produ.getIdProducto());
			cadaProductoJSON.put("idreceta", produ.getIdReceta());
			cadaProductoJSON.put("nombre", produ.getNombre());
			cadaProductoJSON.put("descripcion", produ.getDescripcion());
			cadaProductoJSON.put("impuesto", produ.getImpuesto());
			cadaProductoJSON.put("tipo", produ.getTipo());
			cadaProductoJSON.put("productoasociaadicion", produ.getProductoasociaadicion());
			cadaProductoJSON.put("preciogeneral", produ.getPreciogeneral());
			cadaProductoJSON.put("incluye_liquido", produ.getIncluye_liquido());
			cadaProductoJSON.put("idtipo_liquido", produ.getIdtipo_liquido());
			cadaProductoJSON.put("manejacantidad", produ.getManejacantidad());
			cadaProductoJSON.put("controlaespecialidades", produ.getControlaEspecialidades());
			listJSON.add(cadaProductoJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que retorna los productos de una tienda teniendo en cuenta o no productos de plataforma
	 * @param idtienda
	 * @param plataforma
	 * @return
	 */
	public String GetProductosTiendaPlat(int idtienda, String plataforma){
		JSONArray listJSON = new JSONArray();
		ArrayList<Producto> otrosProductos = ProductoDAO.GetProductosTiendaPlat(idtienda, plataforma);
		for (Producto produ : otrosProductos) {
			JSONObject cadaProductoJSON = new JSONObject();
			cadaProductoJSON.put("idproducto", produ.getIdProducto());
			cadaProductoJSON.put("idreceta", produ.getIdReceta());
			cadaProductoJSON.put("nombre", produ.getNombre());
			cadaProductoJSON.put("descripcion", produ.getDescripcion());
			cadaProductoJSON.put("impuesto", produ.getImpuesto());
			cadaProductoJSON.put("tipo", produ.getTipo());
			cadaProductoJSON.put("productoasociaadicion", produ.getProductoasociaadicion());
			cadaProductoJSON.put("preciogeneral", produ.getPreciogeneral());
			cadaProductoJSON.put("incluye_liquido", produ.getIncluye_liquido());
			cadaProductoJSON.put("idtipo_liquido", produ.getIdtipo_liquido());
			cadaProductoJSON.put("manejacantidad", produ.getManejacantidad());
			cadaProductoJSON.put("controlaespecialidades", produ.getControlaEspecialidades());
			listJSON.add(cadaProductoJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	public String  obtenerHomologacionGaseosaIncluida()
	{
		JSONArray listJSON = new JSONArray();
		ArrayList<HomologaGaseosaIncluida> gaseosaHomologada = PedidoDAO.obtenerHomologacionGaseosaIncluida();
		for (HomologaGaseosaIncluida homologa : gaseosaHomologada) {
			JSONObject cadaGaseosaJSON = new JSONObject();
			cadaGaseosaJSON.put("idtienda", homologa.getIdtienda());
			cadaGaseosaJSON.put("idsabortipoliquido", homologa.getIdsabortipoliquido());
			listJSON.add(cadaGaseosaJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que se encarga de retornar las gaseosas como producto homologada para cada tienda
	 * @return
	 */
	public String obtenerHomologacionProductoGaseosa(int idtienda)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList<HomologaGaseosaIncluida> gaseosaHomologada = PedidoDAO.obtenerHomologacionProductoGaseosa(idtienda);
		for (HomologaGaseosaIncluida homologa : gaseosaHomologada) {
			JSONObject cadaGaseosaJSON = new JSONObject();
			cadaGaseosaJSON.put("idtienda", homologa.getIdtienda());
			cadaGaseosaJSON.put("idproducto", homologa.getIdsabortipoliquido());
			cadaGaseosaJSON.put("nombre", homologa.getNombre());
			cadaGaseosaJSON.put("preciogeneral", homologa.getPrecioGeneral());
			listJSON.add(cadaGaseosaJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	/**
	 * Método en la capa controladora que se encarga de invocar a la capa DAO y obtener todos los productos incluidos
	 * posteriormente los formatea en el JSON y se los envía al servicio para ser retornados
	 * @return Retorna un String en formato JSON con los productos incluidos parametrizados en el sistema.
	 */
	public String obtenerProductosIncluidos(){
		JSONArray listJSON = new JSONArray();
		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();
		for (ProductoIncluido produ : productosIncluidos) {
			JSONObject cadaProductoJSON = new JSONObject();
			cadaProductoJSON.put("idproductoincluido", produ.getIdproductoincluido());
			cadaProductoJSON.put("idproductopadre", produ.getIdproductopadre());
			cadaProductoJSON.put("idproductohijo", produ.getIdproductohijo());
			cadaProductoJSON.put("cantidad", produ.getCantidad());
			cadaProductoJSON.put("nombre", produ.getNombre());
			cadaProductoJSON.put("preciogeneral", produ.getPreciogeneral());
			listJSON.add(cadaProductoJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	public String InsertarEncabezadoPedido(String tienda,int idcliente, String fechaPedido, String user, String esProgramado, String programado, int idTipoPedido){
		int idtienda = TiendaDAO.obteneridTienda(tienda);
		JSONArray listJSON = new JSONArray();
		int resultado = 0;
		if (idcliente != 0)
		{
			resultado = PedidoDAO.InsertarEncabezadoPedido(idtienda, idcliente, fechaPedido, user, esProgramado, programado, idTipoPedido);
		}
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("idpedido", resultado);
		Respuesta.put("idcliente", idcliente);
		Respuesta.put("idestadopedido", "1");
		Respuesta.put("descripcionestadopedido","En curso");
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String InsertarDetallePedido(int idproducto,int idpedido,double cantidad, String especialidad1, String especialidad2, double valorunitario, double valortotal, String adicion, String observacion, int idsabortipoliquido, int idexcepcion, String modespecialidad1, String modespecialidad2){
		JSONArray listJSON = new JSONArray();
		//int idespecialidad1 = PedidoDAO.obtenerIdEspecialidad(especialidad1);
		//int idespecialidad2 = PedidoDAO.obtenerIdEspecialidad(especialidad2);
		int idespecialidad1;
		int idespecialidad2;
		try
        {
			idespecialidad1 = Integer.parseInt(especialidad1);
        }catch (Exception e)
        {
        	idespecialidad1 = 0;
        }
		try
        {
			idespecialidad2 = Integer.parseInt(especialidad2);
        }catch (Exception e)
        {
        	idespecialidad2 = 0;
        }
		DetallePedido detPedido = new DetallePedido(idproducto,idpedido,cantidad,idespecialidad1,idespecialidad2,valorunitario,valortotal, adicion, observacion, idsabortipoliquido, idexcepcion, modespecialidad1, modespecialidad2);
		int iddetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("iddetallepedido", iddetallepedido);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String InsertarDetalleAdicion(int iddetallepedidopadre, int iddetallepedidoadicion, int idespecialidad1, int idespecialidad2, double cantidad1, double cantidad2){
		JSONArray listJSON = new JSONArray();
		DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(iddetallepedidopadre, iddetallepedidoadicion, idespecialidad1, idespecialidad2, cantidad1,cantidad2);
		int idadicion = PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("idadicion", idadicion);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String InsertarModificadorDetallePedido(int iddetallepedidopadre, int idproductoespecialidad1, int idproductoespecialidad2, double cantidad, int iddetallepedido){
		JSONArray listJSON = new JSONArray();
		ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,iddetallepedidopadre, idproductoespecialidad1, idproductoespecialidad2, cantidad, iddetallepedido);
		int idmodificador = PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("idmodificador", idmodificador);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String InsertarMarcacionPedido(MarcacionPedido marPedido){
		MarcacionPedidoDAO.InsertarMarcacionPedido(marPedido);
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("resultado", "OK");
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String obtenerTodosExcepciones(){
		JSONArray listJSON = new JSONArray();
		ArrayList<ExcepcionPrecio> excepcionesPrecio = ExcepcionPrecioDAO.obtenerExcepcionesPrecio();
		for (ExcepcionPrecio exc : excepcionesPrecio) {
			JSONObject cadaExcepcionJSON = new JSONObject();
			cadaExcepcionJSON.put("idexcepcion", exc.getIdExcepcion());
			cadaExcepcionJSON.put("idproducto", exc.getIdProducto());
			cadaExcepcionJSON.put("precio", exc.getPrecio());
			cadaExcepcionJSON.put("descripcion", exc.getDescripcion());
			cadaExcepcionJSON.put("controlacantidadingredientes", exc.getControlaCantidadIngredientes());
			cadaExcepcionJSON.put("cantidadingredientes", exc.getCantidadIngrediantes());
			cadaExcepcionJSON.put("partiradiciones", exc.getPartiradiciones());
			cadaExcepcionJSON.put("horainicial", exc.getHoraInicial());
			cadaExcepcionJSON.put("horafinal", exc.getHoraFinal());
			cadaExcepcionJSON.put("lunes", exc.getLunes());
			cadaExcepcionJSON.put("martes", exc.getMartes());
			cadaExcepcionJSON.put("miercoles", exc.getMiercoles());
			cadaExcepcionJSON.put("jueves", exc.getJueves());
			cadaExcepcionJSON.put("viernes", exc.getViernes());
			cadaExcepcionJSON.put("sabado", exc.getSabado());
			cadaExcepcionJSON.put("domingo", exc.getDomingo());
			cadaExcepcionJSON.put("controlaespecialidades", exc.getControlaEspecialidades());
			cadaExcepcionJSON.put("ofertaabierta", exc.getOfertaAbierta());
			cadaExcepcionJSON.put("manejaoferta", exc.getManejaOferta());
			listJSON.add(cadaExcepcionJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	
	public String obtenerAdicionProductos(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Producto> adicionProductos = PedidoDAO.obtenerAdicionProductos();
		for (Producto produ : adicionProductos) {
			JSONObject cadaProductoJSON = new JSONObject();
			cadaProductoJSON.put("idproducto", produ.getIdProducto());
			cadaProductoJSON.put("idreceta", produ.getIdReceta());
			cadaProductoJSON.put("nombre", produ.getNombre());
			cadaProductoJSON.put("descripcion", produ.getDescripcion());
			cadaProductoJSON.put("impuesto", produ.getImpuesto());
			cadaProductoJSON.put("tipo", produ.getTipo());
			cadaProductoJSON.put("preciogeneral", produ.getPreciogeneral());
			listJSON.add(cadaProductoJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	
	public String ObtenerSaboresLiquidoProducto(int idProdu, int idtienda)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList<SaborLiquido> saboresLiquidos = PedidoDAO.ObtenerSaboresLiquidoProducto(idProdu, idtienda);
		for (SaborLiquido sabor: saboresLiquidos){
			JSONObject cadaSaborJSON = new JSONObject();
			cadaSaborJSON.put("idSaborTipoLiquido", sabor.getIdSaborTipoLiquido());
			cadaSaborJSON.put("descripcionSabor", sabor.getDescripcionSabor());
			cadaSaborJSON.put("idLiquido", sabor.getIdLiquido());
			cadaSaborJSON.put("descripcionLiquido", sabor.getDescripcionLiquido());
			cadaSaborJSON.put("idProducto", sabor.getIdProducto());
			cadaSaborJSON.put("nombreProducto", sabor.getNombreProducto());
			cadaSaborJSON.put("valorAdicional", sabor.getValorAdicional());
			listJSON.add(cadaSaborJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String ObtenerSaboresLiquidoExcepcion(int idExcep, int idtienda)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList<SaborLiquido> saboresLiquidos = PedidoDAO.ObtenerSaboresLiquidoExcepcion(idExcep, idtienda);
		for (SaborLiquido sabor: saboresLiquidos){
			JSONObject cadaSaborJSON = new JSONObject();
			cadaSaborJSON.put("idSaborTipoLiquido", sabor.getIdSaborTipoLiquido());
			cadaSaborJSON.put("descripcionSabor", sabor.getDescripcionSabor());
			cadaSaborJSON.put("idLiquido", sabor.getIdLiquido());
			cadaSaborJSON.put("descripcionLiquido", sabor.getDescripcionLiquido());
			cadaSaborJSON.put("idExcepcion", sabor.getIdExcepcion());
			cadaSaborJSON.put("descripcionExcepcion", sabor.getDescripcionExcepcion());
			cadaSaborJSON.put("valorAdicional", sabor.getValorAdicional());
			listJSON.add(cadaSaborJSON);
		}
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que se encarga de realizar las acciones para finalizar el pedido en el sistema de Contact Center, secuencialmente
	 * se finaliza el pedido en la BD de contact center, se totaliza el valor del pedido y se cambia el estado del pedido, adicionalmente
	 * se forma un json con la informaciónd del pedido, con la cual se invocará el servicio de tienda con los datos necesarios del pedido.
	 * @param idpedido
	 * @param idformapago
	 * @param valorformapago
	 * @param valortotal
	 * @param idcliente
	 * @param insertado
	 * @param tiempopedido
	 * @return Se retorna un string en formato JSON con todos los valores que se pasarán al servicio tienda para la creación del pedido en la 
	 * tienda.
	 */
	public String FinalizarPedido(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado, double tiempopedido, String validaDir, double descuento, String motivoDescuento, String esProgramado, String programado)
	{
		Tienda tienda = PedidoDAO.obtenerTiendaPedido(idpedido);
		String tiendaPixel = tienda.getUrl();
		//Capturamos el parámetro de para que POS irá el destino del pedido, con base en esto se formará la información para enviar
		int pos = tienda.getPos();
		//Validamos si el Pos es igual a 2 el envío será para PIXEL, en caso contrario será para POS Pizza Americana
		InsertarPedidoPixel pedidoPixel = new InsertarPedidoPixel();
		if(pos == 2)
		{
			pedidoPixel = PedidoDAO.finalizarPedido(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, tiempopedido);
		}else if (pos == 1)
		{
			pedidoPixel = PedidoDAO.finalizarPedidoPOSPM(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, tiempopedido, descuento, esProgramado, programado);
		}
		JSONArray listJSON = new JSONArray();
		JSONArray listJSONCliente = new JSONArray();
		JSONObject respuesta = new JSONObject();
		respuesta.put("idpedido", idpedido);
		respuesta.put("valortotal", valortotal);
		respuesta.put("descuento", descuento);
		respuesta.put("motivodescuento", motivoDescuento);
		respuesta.put("insertado", "true");
		respuesta.put("url", tiendaPixel);
		respuesta.put("dsntienda", pedidoPixel.getDsnTienda());
		respuesta.put("memcode", pedidoPixel.getMemcode());
		respuesta.put("indicadoract", pedidoPixel.getIndicadorAct());
		respuesta.put("valorformapago", pedidoPixel.getValorformapago());
		respuesta.put("idformapagotienda", pedidoPixel.getIdformapagotienda());
		respuesta.put("pos", pos);
		respuesta.put("usuariopedido", "CONTACT");
		respuesta.put("tiempopedido", tiempopedido);
		respuesta.put("origen", pedidoPixel.getOrigen());
		respuesta.put("programado", pedidoPixel.getProgramado());
		respuesta.put("horaprogramado", pedidoPixel.getHoraProgramado());
		respuesta.put("idtipopedido", pedidoPixel.getIdTipoPedido());
		//Tomamos la información de la marcación del pedido
		MarcacionPedido marPedido = MarcacionPedidoDAO.obtenerMarcacionPedido(idpedido);
		if(marPedido.getMarketplace().equals(new String("")))
		{
			respuesta.put("plataforma", marPedido.getNombreMarcacion());
			respuesta.put("idpedidoalt", marPedido.getObservacion());
		}else if(marPedido.getMarketplace().equals(new String("S")))
		{
			respuesta.put("plataforma", marPedido.getNombreMarcacion()+" DOMI-Prop");
			respuesta.put("idpedidoalt", marPedido.getObservacion());
		}else if(marPedido.getMarketplace().equals(new String("N")))
		{
			respuesta.put("plataforma", marPedido.getNombreMarcacion()+" DOMI-Plat");
			respuesta.put("idpedidoalt", marPedido.getObservacion());
		}
		
		ArrayList<DetallePedidoPixel> detPedPOS = new ArrayList();
		detPedPOS = pedidoPixel.getEnvioPixel();
		//Se realiza un ciclo For para obtener y formatear en json cada uno de los detalles pedidos
		int contador = 1;
		//Validamos si que pos es para saber que datos enviamos
		if(pos == 2)
		{
			for(DetallePedidoPixel detPed: detPedPOS)
			{
				
				respuesta.put("idproductoext" + contador, detPed.getIdproductoext() );
				respuesta.put("cantidad" + contador, detPed.getCantidad());
				//System.out.println("idproductoext " + detPed.getIdproductoext() + " cantidad " + detPed.getCantidad() );
				contador++;
				
			}
		}else if (pos == 1)
		{
			for(DetallePedidoPixel detPed: detPedPOS)
			{
				
				respuesta.put("idproductoext" + contador, detPed.getIdproductoext() );
				respuesta.put("cantidad" + contador, detPed.getCantidad());
				respuesta.put("esmaster" + contador, detPed.isEsMaster());
				respuesta.put("idmaster" + contador, detPed.getIdMaster());
				respuesta.put("idmodificador" + contador, detPed.getIdModificador());
				respuesta.put("iddetalle" + contador, detPed.getIdDetallePedido());
				//System.out.println("idproductoext " + detPed.getIdproductoext() + " cantidad " + detPed.getCantidad() + " iddetalle " + detPed.getIdDetallePedido());
				contador++;
				
			}
		}
		contador--;
		respuesta.put("cantidaditempedido", contador);
		JSONObject clienteJSON = new JSONObject();
		// El objeto cliente del cual se extraen cada uno de los parámetros y se formatea en JSON.
		String apellidosTemp = pedidoPixel.getCliente().getApellidos();
		if(apellidosTemp == null)
		{
			apellidosTemp = "";
		}
		clienteJSON.put("apellidos", apellidosTemp);
		String nombresTemp = pedidoPixel.getCliente().getNombres();
		if(nombresTemp == null)
		{
			nombresTemp = "";
		}
		clienteJSON.put("nombres", nombresTemp);
		clienteJSON.put("nombrecompania", pedidoPixel.getCliente().getNombreCompania());
		if (validaDir.equals(new String("S")))
		{
			clienteJSON.put("direccion", pedidoPixel.getCliente().getNomenclatura() + " " + pedidoPixel.getCliente().getNumNomenclatura() + " # " + pedidoPixel.getCliente().getNumNomenclatura2() + " - " + pedidoPixel.getCliente().getNum3() );
		}else
		{
			clienteJSON.put("direccion", pedidoPixel.getCliente().getDireccion() );
		}
		clienteJSON.put("telefono", pedidoPixel.getCliente().getTelefono());
		clienteJSON.put("idcliente", pedidoPixel.getCliente().getIdcliente());
		clienteJSON.put("memcode", pedidoPixel.getCliente().getMemcode());
		clienteJSON.put("zonadireccion", pedidoPixel.getCliente().getZonaDireccion());
		clienteJSON.put("observacion", pedidoPixel.getCliente().getObservacion());
		clienteJSON.put("memcode", pedidoPixel.getCliente().getMemcode());
		clienteJSON.put("idnomenclatura", pedidoPixel.getCliente().getIdnomenclatura());
		clienteJSON.put("nomenclatura", pedidoPixel.getCliente().getNomenclatura());
		clienteJSON.put("numnomenclatura", pedidoPixel.getCliente().getNumNomenclatura());
		clienteJSON.put("numnomenclatura2", pedidoPixel.getCliente().getNumNomenclatura2());
		clienteJSON.put("num3", pedidoPixel.getCliente().getNum3());
		clienteJSON.put("municipio", pedidoPixel.getCliente().getMunicipio());
		clienteJSON.put("idmunicipio", pedidoPixel.getCliente().getIdMunicipio());
		clienteJSON.put("latitud", pedidoPixel.getCliente().getLatitud());
		clienteJSON.put("longitud", pedidoPixel.getCliente().getLontitud());
		clienteJSON.put("distanciatienda", pedidoPixel.getCliente().getDistanciaTienda());
		clienteJSON.put("telefonocelular", pedidoPixel.getCliente().getTelefonoCelular());
		clienteJSON.put("email", pedidoPixel.getCliente().getEmail());
		clienteJSON.put("politicadatos", pedidoPixel.getCliente().getPoliticaDatos());
		listJSONCliente.add(clienteJSON);
		respuesta.put("cliente", listJSONCliente.toString());
		listJSON.add(respuesta);
		String respuestaJson = listJSON.toJSONString();
		PedidoDAO.actualizarJSONPedido(idpedido, respuestaJson, "");
		return(respuestaJson);
	}
	
	
	public void FinalizarPedidoTiendaVirtual(int idpedido, int idformapago, int idcliente,  double tiempopedido, String validaDir, double descuento, String motivoDescuento, double valorFormaPago, String esProgramado, String horaProgramado, int idEstadoPedido)
	{

		InsertarPedidoPixel pedidoPixel = new InsertarPedidoPixel();
		PedidoDAO.finalizarPedidoPOSPMTiendaVirtual(idpedido, idformapago, idcliente, tiempopedido, descuento, valorFormaPago, esProgramado, horaProgramado, idEstadoPedido);

	}
	
	/**
	 * Método que se encarga de finalizar un pedido en el modo reenvío, teniendo en cuenta que la información del pedido en el
	 * sistema contact center ya fue confirmado pero faltaría confirmarlo en el sistema de la tienda
	 * @param idpedido
	 * @param idformapago
	 * @param valorformapago
	 * @param valortotal
	 * @param idcliente
	 * @param insertado
	 * @return
	 */
	public String FinalizarPedidoReenvio(int idpedido, int idformapago, double valorformapago, double valortotal, int idcliente, int insertado, double tiempoPedido, String userReenvio, boolean enviarTienda)
	{
		
		Tienda tienda = PedidoDAO.obtenerTiendaPedido(idpedido);
		String tiendaPixel = tienda.getUrl();
		//Capturamos el parámetro de para que POS irá el destino del pedido, con base en esto se formará la información para enviar
		int pos = tienda.getPos();
		//Validamos si el Pos es igual a 2 el envío será para PIXEL, en caso contrario será para POS Pizza Americana
		InsertarPedidoPixel pedidoPixel = new InsertarPedidoPixel();
		if(pos == 2)
		{
			pedidoPixel = PedidoDAO.finalizarPedidoReenvio(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado);
		}else if (pos == 1)
		{
			pedidoPixel = PedidoDAO.finalizarPedidoReenvioPOSPM(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, userReenvio, enviarTienda);
		}
		
		
		JSONArray listJSON = new JSONArray();
		JSONArray listJSONCliente = new JSONArray();
		JSONObject respuesta = new JSONObject();
		respuesta.put("idpedido", idpedido);
		respuesta.put("valortotal", valortotal);
		respuesta.put("descuento", pedidoPixel.getDescuento());
		respuesta.put("insertado", "true");
		respuesta.put("url", tiendaPixel);
		respuesta.put("dsntienda", pedidoPixel.getDsnTienda());
		respuesta.put("memcode", pedidoPixel.getMemcode());
		respuesta.put("indicadoract", pedidoPixel.getIndicadorAct());
		respuesta.put("valorformapago", pedidoPixel.getValorformapago());
		respuesta.put("idformapagotienda", pedidoPixel.getIdformapagotienda());
		respuesta.put("pos", pos);
		respuesta.put("usuariopedido", "CONTACT");
		respuesta.put("tiempopedido", tiempoPedido);
		respuesta.put("origen", pedidoPixel.getOrigen());
		respuesta.put("programado", pedidoPixel.getProgramado());
		respuesta.put("horaprogramado", pedidoPixel.getHoraProgramado());
		respuesta.put("idtipopedido", pedidoPixel.getIdTipoPedido());
		//Tomamos la información de la marcación del pedido
		MarcacionPedido marPedido = MarcacionPedidoDAO.obtenerMarcacionPedido(idpedido);
		if(marPedido.getMarketplace().equals(new String("")))
		{
			respuesta.put("plataforma", marPedido.getNombreMarcacion());
			respuesta.put("idpedidoalt", marPedido.getObservacion());
		}else if(marPedido.getMarketplace().equals(new String("S")))
		{
			respuesta.put("plataforma", marPedido.getNombreMarcacion()+" DOMI-Prop");
			respuesta.put("idpedidoalt", marPedido.getObservacion());
		}else if(marPedido.getMarketplace().equals(new String("N")))
		{
			respuesta.put("plataforma", marPedido.getNombreMarcacion()+" DOMI-Plat");
			respuesta.put("idpedidoalt", marPedido.getObservacion());
		}
		
		
		ArrayList<DetallePedidoPixel> detPedPixel = pedidoPixel.getEnvioPixel();
		//Se realiza un ciclo For para obtener y formatear en json cada uno de los detalles pedidos
		int contador = 1;
		if(pos == 2)
		{
			for(DetallePedidoPixel detPed: detPedPixel)
			{
				
				respuesta.put("idproductoext" + contador, detPed.getIdproductoext() );
				respuesta.put("cantidad" + contador, detPed.getCantidad());
				respuesta.put("valor" + contador, detPed.getValor());
				contador++;
				
			}
		}else if (pos == 1)
		{
			for(DetallePedidoPixel detPed: detPedPixel)
			{
				
				respuesta.put("idproductoext" + contador, detPed.getIdproductoext() );
				respuesta.put("cantidad" + contador, detPed.getCantidad());
				respuesta.put("esmaster" + contador, detPed.isEsMaster());
				respuesta.put("idmaster" + contador, detPed.getIdMaster());
				respuesta.put("idmodificador" + contador, detPed.getIdModificador());
				respuesta.put("iddetalle" + contador, detPed.getIdDetallePedido());
				respuesta.put("valor" + contador, detPed.getValor());
				//System.out.println("idproductoext " + detPed.getIdproductoext() + " cantidad " + detPed.getCantidad() + " iddetalle " + detPed.getIdDetallePedido());
				contador++;
				
			}
		}
		contador--;
		respuesta.put("cantidaditempedido", contador);
		JSONObject clienteJSON = new JSONObject();
		// El objeto cliente del cual se extraen cada uno de los parámetros y se formatea en JSON.
		clienteJSON.put("apellidos", pedidoPixel.getCliente().getApellidos());
		clienteJSON.put("nombres", pedidoPixel.getCliente().getNombres());
		clienteJSON.put("nombrecompania", pedidoPixel.getCliente().getNombreCompania());
		clienteJSON.put("direccion", pedidoPixel.getCliente().getDireccion());
		clienteJSON.put("telefono", pedidoPixel.getCliente().getTelefono());
		clienteJSON.put("apellidos", pedidoPixel.getCliente().getApellidos());
		clienteJSON.put("idcliente", pedidoPixel.getCliente().getIdcliente());
		clienteJSON.put("memcode", pedidoPixel.getCliente().getMemcode());
		clienteJSON.put("zonadireccion", pedidoPixel.getCliente().getZonaDireccion());
		clienteJSON.put("observacion", pedidoPixel.getCliente().getObservacion());
		clienteJSON.put("memcode", pedidoPixel.getCliente().getMemcode());
		clienteJSON.put("idnomenclatura", pedidoPixel.getCliente().getIdnomenclatura());
		clienteJSON.put("nomenclatura", pedidoPixel.getCliente().getNomenclatura());
		clienteJSON.put("numnomenclatura", pedidoPixel.getCliente().getNumNomenclatura());
		clienteJSON.put("numnomenclatura2", pedidoPixel.getCliente().getNumNomenclatura2());
		clienteJSON.put("num3", pedidoPixel.getCliente().getNum3());
		clienteJSON.put("municipio", pedidoPixel.getCliente().getMunicipio());
		clienteJSON.put("idmunicipio", pedidoPixel.getCliente().getIdMunicipio());
		clienteJSON.put("latitud", pedidoPixel.getCliente().getLatitud());
		clienteJSON.put("longitud", pedidoPixel.getCliente().getLontitud());
		clienteJSON.put("distanciatienda", pedidoPixel.getCliente().getDistanciaTienda());
		clienteJSON.put("telefonocelular", pedidoPixel.getCliente().getTelefonoCelular());
		clienteJSON.put("email", pedidoPixel.getCliente().getEmail());
		clienteJSON.put("politicadatos", pedidoPixel.getCliente().getPoliticaDatos());
		listJSONCliente.add(clienteJSON);
		respuesta.put("cliente", listJSONCliente.toString());
		listJSON.add(respuesta);
		String respuestaJson = listJSON.toJSONString();
		PedidoDAO.actualizarJSONPedido(idpedido, respuestaJson, userReenvio);
		return(respuestaJson);
	}
	
	public String eliminarPedidoSinConfirmar(int idpedido, String usuario)
	{
		int idcliente = PedidoDAO.obtenerIdClientePedido(idpedido);
		boolean resultado = PedidoDAO.eliminarPedidoSinConfirmar(idpedido, idcliente, usuario);
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("respuesta", resultado);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	
	public String obtenerTotalPedido(int idpedido)
	{
		double valorTotal = PedidoDAO.obtenerTotalPedido(idpedido);
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("valortotal", valorTotal);
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String obtenerFormaPagoPedido(int idpedido)
	{
		FormaPago formapago = PedidoDAO.obtenerFormaPagoPedido(idpedido);
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		Respuesta.put("idformapago", formapago.getIdformapago());
		Respuesta.put("valortotal", formapago.getValortotal());
		Respuesta.put("valorformapago", formapago.getValorformapago());
		Respuesta.put("nombre", formapago.getNombre());
		Respuesta.put("descuento", formapago.getDescuento());
		Respuesta.put("virtual", formapago.getVirtual());
		listJSON.add(Respuesta);
		return(listJSON.toJSONString());
	}
	
	public String obtenerMarcacionesPedido(int idpedido)
	{
		ArrayList<MarcacionPedido> marcacionesPedido = PedidoDAO.obtenerMarcacionesPedido(idpedido);
		JSONArray listJSON = new JSONArray();
		JSONObject Respuesta = new JSONObject();
		for(MarcacionPedido cadaMarcacion: marcacionesPedido)
		{
			Respuesta.put("idpedido", cadaMarcacion.getIdPedido());
			Respuesta.put("idmarcacion", cadaMarcacion.getIdMarcacion() );
			Respuesta.put("nombremarcacion", cadaMarcacion.getNombreMarcacion() );
			Respuesta.put("observacion", cadaMarcacion.getObservacion());
			Respuesta.put("descuento", cadaMarcacion.getDescuento());
			Respuesta.put("motivo", cadaMarcacion.getMotivo() );
			Respuesta.put("marketplace", cadaMarcacion.getMarketplace());
			Respuesta.put("descuentoplataforma", cadaMarcacion.getDescuentoPlataforma());
			Respuesta.put("tarifaadicional", cadaMarcacion.getTarifaAdicional());
			Respuesta.put("propina", cadaMarcacion.getPropina());
			Respuesta.put("log", cadaMarcacion.getLog());
			listJSON.add(Respuesta);
		}
		
		return(listJSON.toJSONString());
	}
	
	
	public String ConsultaIntegradaPedidos(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <Pedido> consultaPedidos = PedidoDAO.ConsultaIntegradaPedidos(fechainicial, fechafinal, tienda, numeropedido, idEstadoPedido, enviadoPixel);
		for (Pedido cadaPedido: consultaPedidos){
			JSONObject cadaPedidoJSON = new JSONObject();
			cadaPedidoJSON.put("idpedido", cadaPedido.getIdpedido());
			cadaPedidoJSON.put("tienda", cadaPedido.getNombretienda());
			cadaPedidoJSON.put("totalneto",cadaPedido.getTotal_neto());
			cadaPedidoJSON.put("idcliente", cadaPedido.getIdcliente());
			cadaPedidoJSON.put("cliente", cadaPedido.getNombrecliente());
			cadaPedidoJSON.put("estadopedido", cadaPedido.getEstadopedido());
			cadaPedidoJSON.put("enviadopixel", cadaPedido.getEnviadoPixel());
			if(cadaPedido.getEnviadoPixel() == 1)
			{
				cadaPedidoJSON.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else if(cadaPedido.getEnviadoPixel() == 0)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE TIENDA");
			}else if(cadaPedido.getEnviadoPixel() == 2)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE PAGO VIRTUAL");
			}
			cadaPedidoJSON.put("numposheader", cadaPedido.getNumposheader());
			cadaPedidoJSON.put("idtienda", cadaPedido.getTienda().getIdTienda());
			cadaPedidoJSON.put("urltienda", cadaPedido.getTienda().getUrl());
			cadaPedidoJSON.put("stringpixel", cadaPedido.getStringpixel());
			cadaPedidoJSON.put("fechainsercion", cadaPedido.getFechainsercion());
			cadaPedidoJSON.put("fechapedido", cadaPedido.getFechapedido());
			cadaPedidoJSON.put("usuariopedido", cadaPedido.getUsuariopedido());
			cadaPedidoJSON.put("direccion", cadaPedido.getDireccion());
			cadaPedidoJSON.put("telefono", cadaPedido.getTelefono());
			cadaPedidoJSON.put("formapago", cadaPedido.getFormapago());
			cadaPedidoJSON.put("idformapago", cadaPedido.getIdformapago());
			cadaPedidoJSON.put("tiempopedido", cadaPedido.getTiempopedido());
			cadaPedidoJSON.put("idlink", cadaPedido.getIdLink());
			cadaPedidoJSON.put("fechapagovirtual", cadaPedido.getFechaPagoVirtual());
			cadaPedidoJSON.put("fechafinalizacion", cadaPedido.getFechaFinalizacion());
			cadaPedidoJSON.put("programado", cadaPedido.getProgramado());
			cadaPedidoJSON.put("horaprogramado", cadaPedido.getHoraProgramado());
			cadaPedidoJSON.put("idtipopedido", cadaPedido.getIdTipoPedido());
			cadaPedidoJSON.put("usuarioreenvio", cadaPedido.getUsuarioReenvio());
			listJSON.add(cadaPedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que retornará la información de un pedido dado el número de pedido del contact center
	 * @param numeropedido
	 * @return
	 */
	public String ConsultaPedido(int numeropedido)
	{
		JSONObject respuesta = new JSONObject();
		Pedido consultaPedido = PedidoDAO.ConsultaPedido(numeropedido);
		if(consultaPedido.getIdpedido() == 0)
		{
			respuesta.put("idpedido", 0);
		}else
		{
			respuesta.put("idpedido", consultaPedido.getIdpedido());
			respuesta.put("tienda", consultaPedido.getNombretienda());
			respuesta.put("totalneto",consultaPedido.getTotal_neto());
			respuesta.put("idcliente", consultaPedido.getIdcliente());
			respuesta.put("cliente", consultaPedido.getNombrecliente());
			respuesta.put("estadopedido", consultaPedido.getEstadopedido());
			respuesta.put("enviadopixel", consultaPedido.getEnviadoPixel());
			if(consultaPedido.getEnviadoPixel() == 1)
			{
				respuesta.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else if(consultaPedido.getEnviadoPixel() == 0)
			{
				respuesta.put("estadoenviotienda", "PENDIENTE TIENDA");
			}else if(consultaPedido.getEnviadoPixel() == 2)
			{
				respuesta.put("estadoenviotienda", "PENDIENTE PAGO VIRTUAL");
			}
			respuesta.put("numposheader", consultaPedido.getNumposheader());
			respuesta.put("idtienda", consultaPedido.getTienda().getIdTienda());
			respuesta.put("urltienda", consultaPedido.getTienda().getUrl());
			respuesta.put("stringpixel", consultaPedido.getStringpixel());
			respuesta.put("fechainsercion", consultaPedido.getFechainsercion());
			respuesta.put("usuariopedido", consultaPedido.getUsuariopedido());
			respuesta.put("direccion", consultaPedido.getDireccion());
			respuesta.put("telefono", consultaPedido.getTelefono());
			respuesta.put("formapago", consultaPedido.getFormapago());
			respuesta.put("idformapago", consultaPedido.getIdformapago());
			respuesta.put("tiempopedido", consultaPedido.getTiempopedido());
			respuesta.put("idlink", consultaPedido.getIdLink());
			respuesta.put("fechapagovirtual", consultaPedido.getFechaPagoVirtual());
			respuesta.put("fechafinalizacion", consultaPedido.getFechaFinalizacion());

		}
		return respuesta.toJSONString();
	}
	
	public String ConsultaIntegradaPedidosTienda(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <Pedido> consultaPedidos = PedidoDAO.ConsultaIntegradaPedidosTienda(fechainicial, fechafinal, tienda, numeropedido, idEstadoPedido, enviadoPixel);
		for (Pedido cadaPedido: consultaPedidos){
			JSONObject cadaPedidoJSON = new JSONObject();
			cadaPedidoJSON.put("idpedido", cadaPedido.getIdpedido());
			cadaPedidoJSON.put("tienda", cadaPedido.getNombretienda());
			cadaPedidoJSON.put("totalneto",cadaPedido.getTotal_neto());
			cadaPedidoJSON.put("idcliente", cadaPedido.getIdcliente());
			cadaPedidoJSON.put("cliente", cadaPedido.getNombrecliente());
			cadaPedidoJSON.put("estadopedido", cadaPedido.getEstadopedido());
			cadaPedidoJSON.put("enviadopixel", cadaPedido.getEnviadoPixel());
			if(cadaPedido.getEnviadoPixel() == 1)
			{
				cadaPedidoJSON.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else if(cadaPedido.getEnviadoPixel() == 0)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE TIENDA");
			}else if(cadaPedido.getEnviadoPixel() == 2)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE PAGO VIRTUAL");
			}
			cadaPedidoJSON.put("numposheader", cadaPedido.getNumposheader());
			cadaPedidoJSON.put("idtienda", cadaPedido.getTienda().getIdTienda());
			cadaPedidoJSON.put("urltienda", cadaPedido.getTienda().getUrl());
			cadaPedidoJSON.put("stringpixel", cadaPedido.getStringpixel());
			cadaPedidoJSON.put("fechainsercion", cadaPedido.getFechainsercion());
			cadaPedidoJSON.put("usuariopedido", cadaPedido.getUsuariopedido());
			cadaPedidoJSON.put("direccion", cadaPedido.getDireccion());
			cadaPedidoJSON.put("telefono", cadaPedido.getTelefono());
			cadaPedidoJSON.put("formapago", cadaPedido.getFormapago());
			cadaPedidoJSON.put("idformapago", cadaPedido.getIdformapago());
			cadaPedidoJSON.put("tiempopedido", cadaPedido.getTiempopedido());
			cadaPedidoJSON.put("idlink", cadaPedido.getIdLink());
			cadaPedidoJSON.put("fechapagovirtual", cadaPedido.getFechaPagoVirtual());
			cadaPedidoJSON.put("fechafinalizacion", cadaPedido.getFechaFinalizacion());
			cadaPedidoJSON.put("idordencomercio", cadaPedido.getIdOrdenComercio());
			listJSON.add(cadaPedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String ConsultaIntegradaPedidosNuevaTienda(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel, String origenPedido)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <Pedido> consultaPedidos = PedidoDAO.ConsultaIntegradaPedidosNuevaTienda(fechainicial, fechafinal, tienda, numeropedido, idEstadoPedido, enviadoPixel, origenPedido);
		for (Pedido cadaPedido: consultaPedidos){
			JSONObject cadaPedidoJSON = new JSONObject();
			cadaPedidoJSON.put("idpedido", cadaPedido.getIdpedido());
			cadaPedidoJSON.put("tienda", cadaPedido.getNombretienda());
			cadaPedidoJSON.put("totalneto",cadaPedido.getTotal_neto());
			cadaPedidoJSON.put("idcliente", cadaPedido.getIdcliente());
			cadaPedidoJSON.put("cliente", cadaPedido.getNombrecliente());
			cadaPedidoJSON.put("estadopedido", cadaPedido.getEstadopedido());
			cadaPedidoJSON.put("enviadopixel", cadaPedido.getEnviadoPixel());
			if(cadaPedido.getEnviadoPixel() == 1)
			{
				cadaPedidoJSON.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else if(cadaPedido.getEnviadoPixel() == 0)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE TIENDA");
			}else if(cadaPedido.getEnviadoPixel() == 2)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE PAGO VIRTUAL");
			}
			cadaPedidoJSON.put("numposheader", cadaPedido.getNumposheader());
			cadaPedidoJSON.put("idtienda", cadaPedido.getTienda().getIdTienda());
			cadaPedidoJSON.put("urltienda", cadaPedido.getTienda().getUrl());
			cadaPedidoJSON.put("stringpixel", cadaPedido.getStringpixel());
			cadaPedidoJSON.put("fechainsercion", cadaPedido.getFechainsercion());
			cadaPedidoJSON.put("fechapedido", cadaPedido.getFechapedido());
			cadaPedidoJSON.put("usuariopedido", cadaPedido.getUsuariopedido());
			cadaPedidoJSON.put("direccion", cadaPedido.getDireccion());
			cadaPedidoJSON.put("telefono", cadaPedido.getTelefono());
			cadaPedidoJSON.put("formapago", cadaPedido.getFormapago());
			cadaPedidoJSON.put("idformapago", cadaPedido.getIdformapago());
			cadaPedidoJSON.put("tiempopedido", cadaPedido.getTiempopedido());
			cadaPedidoJSON.put("idlink", cadaPedido.getIdLink());
			cadaPedidoJSON.put("fechapagovirtual", cadaPedido.getFechaPagoVirtual());
			cadaPedidoJSON.put("fechafinalizacion", cadaPedido.getFechaFinalizacion());
			cadaPedidoJSON.put("idordencomercio", cadaPedido.getIdOrdenComercio());
			cadaPedidoJSON.put("grupovirtual", cadaPedido.getGrupoVirtual());
			cadaPedidoJSON.put("programado", cadaPedido.getProgramado());
			cadaPedidoJSON.put("horaprogramado", cadaPedido.getHoraProgramado());
			cadaPedidoJSON.put("idtipopedido", cadaPedido.getIdTipoPedido());
			cadaPedidoJSON.put("usuarioreenvio", cadaPedido.getUsuarioReenvio());
			listJSON.add(cadaPedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String ConsultaIntegradaPedidosDomicom(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel , int idPlataforma, int integracion)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <PedidoPlat> consultaPedidos = PedidoDAO.ConsultaIntegradaPedidosDomicom(fechainicial, fechafinal, tienda, numeropedido, idEstadoPedido, enviadoPixel, idPlataforma, integracion);
		for (PedidoPlat cadaPedido: consultaPedidos){
			JSONObject cadaPedidoJSON = new JSONObject();
			cadaPedidoJSON.put("idpedido", cadaPedido.getIdpedido());
			cadaPedidoJSON.put("tienda", cadaPedido.getNombretienda());
			cadaPedidoJSON.put("totalneto",cadaPedido.getTotal_neto());
			cadaPedidoJSON.put("idcliente", cadaPedido.getIdcliente());
			cadaPedidoJSON.put("cliente", cadaPedido.getNombrecliente());
			cadaPedidoJSON.put("estadopedido", cadaPedido.getEstadopedido());
			cadaPedidoJSON.put("enviadopixel", cadaPedido.getEnviadoPixel());
			if(cadaPedido.getEnviadoPixel() == 1)
			{
				cadaPedidoJSON.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else if(cadaPedido.getEnviadoPixel() == 0)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE TIENDA");
			}else if(cadaPedido.getEnviadoPixel() == 2)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE PAGO VIRTUAL");
			}
			cadaPedidoJSON.put("numposheader", cadaPedido.getNumposheader());
			cadaPedidoJSON.put("idtienda", cadaPedido.getTienda().getIdTienda());
			cadaPedidoJSON.put("urltienda", cadaPedido.getTienda().getUrl());
			cadaPedidoJSON.put("stringpixel", cadaPedido.getStringpixel());
			cadaPedidoJSON.put("fechainsercion", cadaPedido.getFechainsercion());
			cadaPedidoJSON.put("fechapedido", cadaPedido.getFechapedido());
			cadaPedidoJSON.put("usuariopedido", cadaPedido.getUsuariopedido());
			cadaPedidoJSON.put("direccion", cadaPedido.getDireccion());
			cadaPedidoJSON.put("telefono", cadaPedido.getTelefono());
			cadaPedidoJSON.put("formapago", cadaPedido.getFormapago());
			cadaPedidoJSON.put("idformapago", cadaPedido.getIdformapago());
			cadaPedidoJSON.put("tiempopedido", cadaPedido.getTiempopedido());
			cadaPedidoJSON.put("idlink", cadaPedido.getIdLink());
			cadaPedidoJSON.put("fechapagovirtual", cadaPedido.getFechaPagoVirtual());
			cadaPedidoJSON.put("fechafinalizacion", cadaPedido.getFechaFinalizacion());
			cadaPedidoJSON.put("idordencomercio", cadaPedido.getIdOrdenComercio().toString());
			cadaPedidoJSON.put("aceptadorappi", cadaPedido.getAceptadoRappi());
			listJSON.add(cadaPedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String ConsultaIntegradaPedidosEnCurso(String fechainicial, String fechafinal, String tienda, int numeropedido, int idEstadoPedido, int enviadoPixel)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <Pedido> consultaPedidos = PedidoDAO.ConsultaIntegradaPedidosEnCurso(fechainicial, fechafinal, tienda, numeropedido, idEstadoPedido, enviadoPixel);
		for (Pedido cadaPedido: consultaPedidos){
			JSONObject cadaPedidoJSON = new JSONObject();
			cadaPedidoJSON.put("idpedido", cadaPedido.getIdpedido());
			cadaPedidoJSON.put("tienda", cadaPedido.getNombretienda());
			cadaPedidoJSON.put("totalneto",cadaPedido.getTotal_neto());
			cadaPedidoJSON.put("idcliente", cadaPedido.getIdcliente());
			cadaPedidoJSON.put("cliente", cadaPedido.getNombrecliente());
			cadaPedidoJSON.put("estadopedido", cadaPedido.getEstadopedido());
			cadaPedidoJSON.put("enviadopixel", cadaPedido.getEnviadoPixel());
			if(cadaPedido.getEnviadoPixel() == 1)
			{
				cadaPedidoJSON.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else if(cadaPedido.getEnviadoPixel() == 0)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE TIENDA");
			}else if(cadaPedido.getEnviadoPixel() == 2)
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE PAGO VIRTUAL");
			}
			cadaPedidoJSON.put("numposheader", cadaPedido.getNumposheader());
			cadaPedidoJSON.put("idtienda", cadaPedido.getTienda().getIdTienda());
			cadaPedidoJSON.put("urltienda", cadaPedido.getTienda().getUrl());
			cadaPedidoJSON.put("stringpixel", cadaPedido.getStringpixel());
			cadaPedidoJSON.put("fechainsercion", cadaPedido.getFechainsercion());
			cadaPedidoJSON.put("usuariopedido", cadaPedido.getUsuariopedido());
			cadaPedidoJSON.put("direccion", cadaPedido.getDireccion());
			cadaPedidoJSON.put("telefono", cadaPedido.getTelefono());
			cadaPedidoJSON.put("formapago", cadaPedido.getFormapago());
			cadaPedidoJSON.put("idformapago", cadaPedido.getIdformapago());
			cadaPedidoJSON.put("tiempopedido", cadaPedido.getTiempopedido());
			cadaPedidoJSON.put("idlink", cadaPedido.getIdLink());
			cadaPedidoJSON.put("fechapagovirtual", cadaPedido.getFechaPagoVirtual());
			cadaPedidoJSON.put("fechafinalizacion", cadaPedido.getFechaFinalizacion());
			listJSON.add(cadaPedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	
	/**
	 * Método que responde desde la capa controladora para la consulta del encabezado de los pedidos de un cliente.
	 * @param idCliente, se recibe como parámetro el identificador del cliente para la consulta
	 * @return
	 */
	public String ConsultaUltimosPedidosCliente(int idCliente)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <Pedido> consultaPedidos = PedidoDAO.ConsultaUltimosPedidosCliente(idCliente);
		for (Pedido cadaPedido: consultaPedidos){
			JSONObject cadaPedidoJSON = new JSONObject();
			cadaPedidoJSON.put("idpedido", cadaPedido.getIdpedido());
			cadaPedidoJSON.put("tienda", cadaPedido.getNombretienda());
			cadaPedidoJSON.put("totalneto",cadaPedido.getTotal_neto());
			cadaPedidoJSON.put("idcliente", cadaPedido.getIdcliente());
			cadaPedidoJSON.put("cliente", cadaPedido.getNombrecliente());
			cadaPedidoJSON.put("estadopedido", cadaPedido.getEstadopedido());
			cadaPedidoJSON.put("enviadopixel", cadaPedido.getEnviadoPixel());
			if(cadaPedido.getEnviadoPixel() == 1)
			{
				cadaPedidoJSON.put("estadoenviotienda", "ENVIADO A TIENDA");
			}
			else
			{
				cadaPedidoJSON.put("estadoenviotienda", "PENDIENTE TIENDA");
			}
			cadaPedidoJSON.put("numposheader", cadaPedido.getNumposheader());
			cadaPedidoJSON.put("idtienda", cadaPedido.getTienda().getIdTienda());
			cadaPedidoJSON.put("urltienda", cadaPedido.getTienda().getUrl());
			cadaPedidoJSON.put("stringpixel", cadaPedido.getStringpixel());
			cadaPedidoJSON.put("fechainsercion", cadaPedido.getFechainsercion());
			cadaPedidoJSON.put("usuariopedido", cadaPedido.getUsuariopedido());
			cadaPedidoJSON.put("direccion", cadaPedido.getDireccion());
			cadaPedidoJSON.put("telefono", cadaPedido.getTelefono());
			cadaPedidoJSON.put("formapago", cadaPedido.getFormapago());
			cadaPedidoJSON.put("idformapago", cadaPedido.getIdformapago());
			cadaPedidoJSON.put("tiempopedido", cadaPedido.getTiempopedido());
			listJSON.add(cadaPedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String ConsultarDetallePedido(int numeropedido)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList <DetallePedido> consultaDetallePedido = PedidoDAO.ConsultarDetallePedido(numeropedido);
		for (DetallePedido cadaDetallePedido: consultaDetallePedido){
			JSONObject cadaDetallePedidoJSON = new JSONObject();
			cadaDetallePedidoJSON.put("iddetallepedido", cadaDetallePedido.getIddetallepedido());
			cadaDetallePedidoJSON.put("nombreproducto", cadaDetallePedido.getNombreproducto());
			cadaDetallePedidoJSON.put("cantidad", cadaDetallePedido.getCantidad());
			cadaDetallePedidoJSON.put("especialidad1", cadaDetallePedido.getNombreespecialidad1());
			cadaDetallePedidoJSON.put("modespecialidad1", cadaDetallePedido.getModespecialidad1());
			cadaDetallePedidoJSON.put("especialidad2",cadaDetallePedido.getNombreespecialidad2());
			cadaDetallePedidoJSON.put("modespecialidad2", cadaDetallePedido.getModespecialidad2());
			cadaDetallePedidoJSON.put("valorunitario", cadaDetallePedido.getValorunitario());
			cadaDetallePedidoJSON.put("valortotal", cadaDetallePedido.getValortotal());
			cadaDetallePedidoJSON.put("adicion", cadaDetallePedido.getAdicion());
			cadaDetallePedidoJSON.put("observacion", cadaDetallePedido.getObservacion());
			cadaDetallePedidoJSON.put("liquido", cadaDetallePedido.getLiquido());
			cadaDetallePedidoJSON.put("excepcion", cadaDetallePedido.getExcepcion());
			listJSON.add(cadaDetallePedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	public String EliminarDetallePedido(int iddetallepedido)
	{
		JSONArray listJSON = new JSONArray();
		ArrayList<Integer> idPedidos = PedidoDAO.EliminarDetallePedido(iddetallepedido);
		for(Integer cadaInteger: idPedidos)
		{
			JSONObject cadaIdDetallePedidoJSON = new JSONObject();
			cadaIdDetallePedidoJSON.put("iddetallepedido", cadaInteger.intValue() );
			listJSON.add(cadaIdDetallePedidoJSON);
		}
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que se encarga de permitir duplicar un iddetallepedido, validando todo lo que tiene el id pedido padre
	 * y si es posible duplicar todo lo asociado a este.
	 * @param iddetallepedido Se recibe como parámetro el iddetallepedido asociado a la duplicación, desde la capa de
	 * presentación se permite realizar la duplicación de productos diferentes a adiciones y modificadores.
	 * @return Se retorna un String en formato JSON con la respuesta de cada uno de los detalles pedidos insertados
	 * y que se tomarán como bases para pintar el DATATABLE.
	 */
	public String DuplicarDetallePedido(int iddetallepedido)
	{
				//Utilizamos método que se encarga de retonar todos los detalles pedido asignado a un grupo de pedido
				//donde podría tenerse modificadores y adiciones, retornados en un ArrayList.
				ArrayList<DetallePedido> detallesPedido = PedidoDAO.ConsultarDetallePedidoPorPadre(iddetallepedido);
				//Se instancia ArrayList en donde se insertaran los nuevos detalles pedidos productos de la duplicación
				// de los iniciales.
				ArrayList<DetallePedido> detallesPedidoNuevos = new ArrayList();
				int idDetPedIns;
				//Se lleva el control del que será el detalle pedido padre, esto se detecta por el tipo de pedido.
				boolean bandDetPedido = false;
				int idDetPedidoPadre = 0;
				// Se define arreglo donde se llevaran lo iddetallepedidos nuevo y viejos
				int[][] idDetsPeds = new int[20][2];
				int contDetPeds = 0;
				//Se recorren uno a uno los detalle de pedido para insertarse como duplicado
				for (DetallePedido detPed : detallesPedido) {
					// Se realiza la inserción de de pedido, no existe inconveniente debido a que no se toma el iddetallepedido
					idDetsPeds[contDetPeds][0] = detPed.getIddetallepedido();
					//Antes de realizar la inserción del detalle pedido controlaremos si es un producto incluido para cambiar la información
					if (detPed.getObservacion().equals(new String("Producto Incluido-"+ iddetallepedido)))
					{
						detPed.setObservacion("Producto Incluido-"+ idDetPedidoPadre);
					}
					// Se realiza la inserción del detallepedido
					idDetPedIns = PedidoDAO.InsertarDetallePedido(detPed);
					// Se lleva registro en el array del iddetallepedido recién insertado.
					idDetsPeds[contDetPeds][1] = idDetPedIns;
					// Se lleva control de si es el detalle pedido principal con la marcación de que sea sola una vez, sea tipo de prodcutos OTROS o PIZZA y adicionalmente no sea un producto incluido
					if (bandDetPedido == false && (detPed.getTipoProducto().equals(new String("OTROS")) || detPed.getTipoProducto().equals(new String("PIZZA"))) && !(detPed.getObservacion().equals(new String("Producto Incluido-"+ iddetallepedido))) )
					{
						idDetPedidoPadre = idDetPedIns;
						bandDetPedido = true;
					}
					// Se clona el objeto para este ser editado y adicionarlo al ArrayList que se responderá.					
					DetallePedido detPedTemp = (DetallePedido) detPed.clone();
					detPedTemp.setIddetallepedido(idDetPedIns);
					// Se agrega el detallePedido al ArrayList de respuesta
					detallesPedidoNuevos.add(detPedTemp);
					// Se lleva contador de detalles pedidos adiciones/duplicados
					contDetPeds++;
				}
				//Obtenemos ArrayList con las adiciones dado un detallepedido
				ArrayList<DetallePedidoAdicion> adicionDetallePedido = PedidoDAO.ObtenerAdicionDetallePedido(iddetallepedido);
				//Recorremos los detalles de las adiciones
				for(DetallePedidoAdicion detPedAdi: adicionDetallePedido)
				{
					//Nos traemos el detalle de pedido padre que significa el producto detalle pedido al cual
					// esta asociada la adición.
					int idDetPedAdiAnt =  detPedAdi.getIddetallepedidoadicion();
					for (int i= 0; i < contDetPeds; i++)
					{
						//buscamos uno a uno el arreglo donde se guardan los detalles pedidos
						if (idDetsPeds[i][0] == idDetPedAdiAnt)
						{
							// Se realizan los cambios en el objeto para dejar la información correcta
							detPedAdi.setIddetallepedidoadicion(idDetsPeds[i][1]);
							detPedAdi.setIddetallepedidopadre(idDetPedidoPadre);
							// Se realiza la inserción del detalle pedido adición.
							PedidoDAO.InsertarDetalleAdicion(detPedAdi);
						}
						
					}
				}
				// Se extrae un Array List con los modificadores de detalle pedido 
				ArrayList<ModificadorDetallePedido> modificadorDetallePedido = PedidoDAO.ObtenerModificadorDetallePedido(iddetallepedido);
				// Se recorren uno a uno los modificadores detalle pedido.
				for(ModificadorDetallePedido modDetPed: modificadorDetallePedido)
				{
					int idDetPedPadAnt =  modDetPed.getIddetallepedidopadre();
					for (int i= 0; i < contDetPeds; i++)
					{
						if (idDetsPeds[i][0] == idDetPedPadAnt)
						{
							modDetPed.setIddetallepedidopadre(idDetPedidoPadre);
							// Cuando se tiene un modificador detalle pedido que en el campo iddetallepedidoasociado
							// es diferente de cero se tiene que asociar el iddetallepedido insertado, esto se da
							// cuando hay modificadores que traen un precio adicional para la pizza.
							if(modDetPed.getIddetallepedidoasociado() != 0)
							{
								int idDetPedAso = modDetPed.getIddetallepedidoasociado();
								for (int j= 0; j < contDetPeds; j++)
								{
									if (idDetsPeds[j][0] == idDetPedAso)
									{
										modDetPed.setIddetallepedidoasociado(idDetsPeds[j][1]);;
									}
								}
							}
							PedidoDAO.InsertarModificadorDetallePedido(modDetPed);
						}
						
					}
				}		
				JSONArray listJSON = new JSONArray();
				// Se realiza el formateo en JSON para la respuesta de los detalles pedidos duplicados para que sean 
				// pintados en la capa de presentación.
				for (DetallePedido cadaDetallePedido: detallesPedidoNuevos){
					JSONObject cadaDetallePedidoJSON = new JSONObject();
					cadaDetallePedidoJSON.put("iddetallepedido", cadaDetallePedido.getIddetallepedido());
					if(cadaDetallePedido.getTipoProducto().equals(new String("PIZZA")))
					{
						cadaDetallePedidoJSON.put("pizza", cadaDetallePedido.getNombreproducto());
					}else
					{
						cadaDetallePedidoJSON.put("otroprod", cadaDetallePedido.getNombreproducto());
					}
					cadaDetallePedidoJSON.put("cantidad", cadaDetallePedido.getCantidad());
					cadaDetallePedidoJSON.put("especialidad1", cadaDetallePedido.getNombreespecialidad1());
					cadaDetallePedidoJSON.put("especialidad2", cadaDetallePedido.getNombreespecialidad2());
					cadaDetallePedidoJSON.put("liquido", cadaDetallePedido.getLiquido());
					cadaDetallePedidoJSON.put("observacion", cadaDetallePedido.getObservacion());
					cadaDetallePedidoJSON.put("adicion", cadaDetallePedido.getAdicion());
					cadaDetallePedidoJSON.put("valorunitario", cadaDetallePedido.getValorunitario());
					cadaDetallePedidoJSON.put("valortotal", cadaDetallePedido.getValortotal());
					cadaDetallePedidoJSON.put("tipo", cadaDetallePedido.getTipoProducto());
					listJSON.add(cadaDetallePedidoJSON);
				}
				return(listJSON.toJSONString());
	}
	
	/**
	 * Método de la capa controladora que se encarga de retornar en formato JSON el precio asociado a una excepción de
	 * precio de especialidad y producto.
	 * @param idespecialidad Se recibe como parámetro el idespecialidad asociado a la excepción de precio especialidad.
	 * @param idproducto Se recibe como parámetro el idproducto asociado a la excepción de precio especialidad.
	 * @return Se retorna en un String formato json el precio asociado según los parámetros de entrada|
	 */
	public String obtenerPrecioExcepcionEspecialidad(int idespecialidad, int idproducto)
	{
		JSONArray listJSON = new JSONArray();
		double precio = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idespecialidad, idproducto);
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("precio", precio );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
		
	}
	
	public String ConsultarDireccionesPedido(String fechainicial, String fechafinal, String idMunicipio, String idTienda, String horaIni, String horaFin)
	{
		ArrayList <DireccionFueraZona> dirsPedido = PedidoDAO.ConsultarDireccionesPedido(fechainicial, fechafinal, idMunicipio, idTienda, horaIni, horaFin);
		JSONArray listJSON = new JSONArray();
		JSONObject ResultadoJSON = new JSONObject();
		for (DireccionFueraZona dirTemp : dirsPedido) 
		{
			ResultadoJSON = new JSONObject();
			ResultadoJSON.put("idpedido", dirTemp.getId());
			ResultadoJSON.put("direccion", dirTemp.getDireccion());
			ResultadoJSON.put("municipio", dirTemp.getMunicipio());
			ResultadoJSON.put("idcliente", dirTemp.getIdCliente());
			ResultadoJSON.put("latitud", dirTemp.getLatitud());
			ResultadoJSON.put("longitud", dirTemp.getLongitud());
			ResultadoJSON.put("telefono", dirTemp.getTelefono());
			ResultadoJSON.put("nombre", dirTemp.getNombre());
			ResultadoJSON.put("apellido", dirTemp.getApellido());
			ResultadoJSON.put("fechapedido", dirTemp.getFechaIngreso());
			ResultadoJSON.put("valor", dirTemp.getValor());
			listJSON.add(ResultadoJSON);
		}
		//Tendremos una tabla temporal para reportes clientes y pedidos dentro de un poligono
		TmpPedidosPoligonoDAO.limpiarTabla();
		return listJSON.toJSONString();
		
	}
	
	public String insertarTmpPedidoPoligono(int idPedido, int idCliente)
	{
		int respuesta = TmpPedidosPoligonoDAO.insertarTmpPedidoPoligono(idPedido, idCliente);
		JSONObject ResultadoJSON  = new JSONObject();
		if(respuesta > 0)
		{
			ResultadoJSON.put("resultado", "OK");
		}else
		{
			ResultadoJSON.put("resultado", "NOK");
		}
		return(ResultadoJSON.toJSONString());
	}
	
	
	public ArrayList<Pedido> ConsultarPedidosPendientes(String fechaPed)
	{
		ArrayList<Pedido> pedidosPendientes = PedidoDAO.ConsultarPedidosPendientes(fechaPed);
		return(pedidosPendientes);
		
	}
	
	public String realizarNotificacionWompi(String idLink, int idCliente, String linkPago, int idFormaPago, int idPedido)
	{
		Date date = new Date();   // given date
		Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
		int horaActual = calendar.get(Calendar.HOUR_OF_DAY); 
		calendar.setTime(date);
		String observacionLog = "";
		String emailEnvio = "";
		//Comenzamos por consultar al cliente para recuperar la información de correo electrónico y número celular
		Cliente clienteNoti = ClienteDAO.obtenerClienteporID(idCliente);
		//Obtenemos la forma de pago
		FormaPago formaPagoNoti = FormaPagoDAO.retornarFormaPago(idFormaPago);
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		//Procesamos los mensajes de texto y correo electrónico
		String mensajeTexto = formaPagoNoti.getMensajeTexto();
		String telefonoCelular = clienteNoti.getTelefonoCelular();
		mensajeTexto = mensajeTexto.replace("#VINCULO", linkPago);
		//Envío del mensaje de Texto
		promoCtrl.ejecutarPHPEnvioMensaje( "57"+ telefonoCelular, mensajeTexto);
		observacionLog = "Se envio mensaje de texto.";
		//Vamos a verificar si el cliente tiene correo electrónico para enviarlo si es el caso
		if(clienteNoti.getEmail() != null)
		{
			if(clienteNoti.getEmail().length()> 0)
			{
				if(clienteNoti.getEmail().contains("@"))
				{
					observacionLog = observacionLog + " Se tiene email para enviar.";
					String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
					String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
					String imagenWompi = ParametrosDAO.retornarValorAlfanumerico("IMAGENPAGOWOMPI");
					String mensajeCorreo = formaPagoNoti.getMensajeCorreo();
					mensajeCorreo = mensajeCorreo.replace("#VINCULO", linkPago);
					Correo correo = new Correo();
					correo.setAsunto("PIZZA AMERICANA LINK DE PAGO PEDIDO # " + idPedido);
					ArrayList correos = new ArrayList();
					String correoEle = clienteNoti.getEmail();
					emailEnvio = correoEle;
					correos.add(correoEle);
					correo.setContrasena(claveCorreo);
					correo.setUsuarioCorreo(cuentaCorreo);
					String mensajeCuerpoCorreo = "Cordiar Saludo " + clienteNoti.getNombres() + " " + clienteNoti.getApellidos() + " ." + mensajeCorreo 
							+ "\n" + "<body><a href=\"" + linkPago + "\"><img align=\" center \" src=\""+ imagenWompi +"\"></a></body>";
					correo.setMensaje(mensajeCuerpoCorreo);
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					//Agregamos control para que verifique con que método debe hacer el envío
					if(cuentaCorreo.contains("@gmail.com"))
					{
						contro.enviarCorreo();
					}else
					{
						contro.enviarCorreo();
					}
					observacionLog = observacionLog + " Se intento realizar el envío del correo electrónico.";
				}
			}
		}
		//Verificar el mensaje de WhatsApp
//		if(horaActual >= 18 && horaActual<=20)
//		{
//			notificarWhatsApp(clienteNoti.getNombres() + " " + clienteNoti.getApellidos(), idPedido, idCliente, linkPago);
//		}
		notificarWhatsAppUltramsg(clienteNoti.getNombres() + " " + clienteNoti.getApellidos(), idPedido, idCliente, linkPago);
		PedidoPagoVirtual pedPagVirtual = new PedidoPagoVirtual(idPedido, emailEnvio, telefonoCelular, observacionLog);
		PedidoPagoVirtualDAO.insertarPedidoPagoVirtual(pedPagVirtual);
		//Al pedido le adicionamos el campo de idLink para el pago
		PedidoDAO.actualizarLinkPagoPedido(idPedido, idLink);
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que tendrá como objetivo realizar una renotificación al cliente despues de 20 minutos con el fin de recordar a este el pago
	 * @param idLink
	 * @param idCliente
	 * @param linkPago
	 * @param idFormaPago
	 * @param idPedido
	 * @return
	 */
	public String realizarRenotificacionWompi(String idLink, int idCliente, String linkPago, int idPedido)
	{
		String observacionLog = "";
		String emailEnvio = "";
		//Comenzamos por consultar al cliente para recuperar la información de correo electrónico y número celular
		Cliente clienteNoti = ClienteDAO.obtenerClienteporID(idCliente);
		//Obtenemos la forma de pago
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		//Procesamos los mensajes de texto y correo electrónico
		String mensajeTexto = "Querido Cliente han pasado 20 minutos y no registramos tu pago, tendras 20 minutos mas o tu pedido sera cancelado. Tu link es " + linkPago;
		String telefonoCelular = clienteNoti.getTelefonoCelular();
		//Envío del mensaje de Texto
		promoCtrl.ejecutarPHPEnvioMensaje( "57"+ telefonoCelular, mensajeTexto);
		
		//ENVIAREMOS MENSAJE DE WHATSAPP
		OkHttpClient client = new OkHttpClient();

		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
		String mensajeEvidencia = "token=tjjy9tki646vwazi&to=+57"+ telefonoCelular + "&body=" + mensajeTexto +" &priority=1&referenceId=";
		RequestBody body = RequestBody.create(mediaType, "token=tjjy9tki646vwazi&to=+57"+ telefonoCelular + "&body=" + mensajeTexto +" &priority=1&referenceId=");
		Request request = new Request.Builder()
		  .post(body)
		  .addHeader("content-type", "application/x-www-form-urlencoded")
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
		}catch(Exception e)
		{
			System.out.println("ERROR " + e.toString());
			//Recuperar la lista de distribución para este correo
			ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
			Date fecha = new Date();
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
			correo.setAsunto("OJO ERROR EN SERVICIO DE WHATSAPP  " + fecha.toString());
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje("Se presenta error en servicio de API de WhatsApp. " + e.toString() + mensajeEvidencia );
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
		}
		//
		
		observacionLog = "Se envio mensaje de texto.";
		//Vamos a verificar si el cliente tiene correo electrónico para enviarlo si es el caso
		if(clienteNoti.getEmail() != null)
		{
			if(clienteNoti.getEmail().length()> 0)
			{
				observacionLog = observacionLog + " Se tiene email para enviar.";
				String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
				String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
				String imagenWompi = ParametrosDAO.retornarValorAlfanumerico("IMAGENWOMPI20MINUTOS");
				String mensajeCorreo = "Querido Cliente han pasado 20 minutos y no registramos tu pago, tendrás 10 minutos más o tu pedido será cancelado. Tu link es " + linkPago;
				Correo correo = new Correo();
				correo.setAsunto("No hemos registrado tu pago Pedido # " + idPedido);
				ArrayList correos = new ArrayList();
				String correoEle = clienteNoti.getEmail();
				emailEnvio = correoEle;
				correos.add(correoEle);
				correo.setContrasena(claveCorreo);
				correo.setUsuarioCorreo(cuentaCorreo);
				String mensajeCuerpoCorreo = "Cordiar Saludo " + clienteNoti.getNombres() + clienteNoti.getApellidos() + " ." + mensajeCorreo 
						+ "\n" + "<body><a href=\"" + linkPago + "\"><img align=\" center \" src=\""+ imagenWompi +"\"></a></body>";;
				correo.setMensaje(mensajeCuerpoCorreo);
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				//Agregamos control para que verifique con que método debe hacer el envío
				if(cuentaCorreo.contains("@gmail.com"))
				{
					contro.enviarCorreo();
				}else
				{
					contro.enviarCorreo();
				}
				observacionLog = observacionLog + " Se intento realizar el envío del correo electrónico.";
			}
		}
		PedidoPagoVirtual pedPagVirtual = new PedidoPagoVirtual(idPedido, emailEnvio, telefonoCelular, observacionLog);
		PedidoPagoVirtualDAO.insertarPedidoPagoVirtual(pedPagVirtual);
		//Al pedido le adicionamos el campo de idLink para el pago
		PedidoDAO.actualizarLinkPagoPedido(idPedido, idLink);
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	public boolean notificarWhatsAppPagorVirtual()
	{
		return(true);
	}
	
	public String realizarCancelacionWompi(int idCliente, int idPedido)
	{
		String observacionLog = "";
		String emailEnvio = "";
		//Comenzamos por consultar al cliente para recuperar la información de correo electrónico y número celular
		Cliente clienteNoti = ClienteDAO.obtenerClienteporID(idCliente);
		//Obtenemos la forma de pago
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		//Procesamos los mensajes de texto y correo electrónico
		String mensajeTexto = "Querido Cliente han pasado mas de 40 minutos y no registramos tu pago, tu pedido # " + idPedido + " fue cancelado.";
		String telefonoCelular = clienteNoti.getTelefonoCelular();
		//Envío del mensaje de Texto
		promoCtrl.ejecutarPHPEnvioMensaje( "57"+ telefonoCelular, mensajeTexto);
		observacionLog = "Se envio mensaje de texto.";
		//Vamos a verificar si el cliente tiene correo electrónico para enviarlo si es el caso
		if(clienteNoti.getEmail() != null)
		{
			if(clienteNoti.getEmail().length()> 0)
			{
				observacionLog = observacionLog + " Se tiene email para enviar.";
				String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
				String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
				String imagenWompi = ParametrosDAO.retornarValorAlfanumerico("IMAGENCANCELACIONWOMPI");
				String mensajeCorreo = "Querido Cliente han pasado más de 40 minutos y no registramos tu pago, tu pedido # " + idPedido + " fue cancelado. Si deseas podrás llamar de nuevo a nuestro Contact Center y reactivar tu pedido.";
				Correo correo = new Correo();
				correo.setAsunto("Cancelación de tu pago Pedido # " + idPedido);
				ArrayList correos = new ArrayList();
				String correoEle = clienteNoti.getEmail();
				emailEnvio = correoEle;
				correos.add(correoEle);
				correo.setContrasena(claveCorreo);
				correo.setUsuarioCorreo(cuentaCorreo);
				String mensajeCuerpoCorreo = "Cordiar Saludo " + clienteNoti.getNombres() + clienteNoti.getApellidos() + " ." + mensajeCorreo 
						+ "\n" + "<body><img align=\" center \" src=\""+ imagenWompi +"\"></a></body>";;
				correo.setMensaje(mensajeCuerpoCorreo);
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				//Agregamos control para que verifique con que método debe hacer el envío
				if(cuentaCorreo.contains("@gmail.com"))
				{
					contro.enviarCorreo();
				}else
				{
					contro.enviarCorreo();
				}
				observacionLog = observacionLog + " Se intento realizar el envío del correo electrónico.";
			}
		}
		PedidoPagoVirtual pedPagVirtual = new PedidoPagoVirtual(idPedido, emailEnvio, telefonoCelular, observacionLog);
		PedidoPagoVirtualDAO.insertarPedidoPagoVirtual(pedPagVirtual);
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	public String capturarEventoPagoWompi(String stringJSON)
	{
		// Debemos de procesar todo datos para extraer todo los datos requeridos
		JSONParser parser = new JSONParser();
		try {
			 //De acuerdo a la definición de como se envian los eventos, extraremos
			 Object objParser = parser.parse(stringJSON);
			 JSONObject jsonPedido = (JSONObject) objParser;
			 String evento = (String)jsonPedido.get("event");
			 			 
			 //Vamos extraer la informacion de DATA
			 String dataJSON = (String)jsonPedido.get("data").toString();
			 Object objParserData = parser.parse(dataJSON);
			 JSONObject jsonTransaction = (JSONObject) objParserData;
			 String dataFinal = (String)jsonTransaction.get("transaction").toString();
			 //Convertimos el JSON de Data
			 Object objParserTransaction = parser.parse(dataFinal);
			 JSONObject jsonTransaccion =(JSONObject)objParserTransaction;
			 String idLink = (String)jsonTransaccion.get("payment_link_id");
			 String estado = (String)jsonTransaccion.get("status");
			 String tipoPago = (String)jsonTransaccion.get("payment_method_type");
			 //Posteriormente de capturada la información realizaremos un guardado de log de eventos WOMPI
			 if(stringJSON.length() > 5000)
			 {
				 stringJSON = stringJSON.substring(0,5000);
			 }
			 LogEventoWompi logEvento = new LogEventoWompi(idLink, evento, estado, stringJSON);
			 int idLogEvento = LogEventoWompiDAO.insertarLogEventoWompi(logEvento);
			 //Posteriormente verificaremos si es un transacción de aprobado, y verificaremos el estado del idlink
			 if(evento.equals(new String("transaction.updated")))
			 {
				 //En este punto es porque hubo el pago de una transacción
				 if(estado.equals(new String("APPROVED")))
				 {
					 
					 //Haremos una validación de que el pago viene del datáfono virtual de WOMPI
					 if(idLink.equals(new String("VPOS_MmLnoT")))
					 {
						 long valorPagoCent =  (long)jsonTransaccion.get("amount_in_cents");
						 double valorPago = ((double)valorPagoCent)/100;
						 //Realizaremos la inserción del registro de lo que se pago en el datáfono virtual
						 PedidoPagoVirtualConsolidadoDAO.insertarPedidoPagoVirtualPagado(131313, 13, valorPago, valorPago, idLink, tipoPago);
					 }
					 //Si el pedido asociado está en enviadoPixel = 2 es decir PENDIENTE PAGO VIRTUAL, realizaremos envío del pedido a la tienda
					 //y actualizaremos el estado a 1 que significa ENVIADO A TIENDA.
					 
					//Realizaremos el llenado del campo de fechapagovirtual
					int idPedido = PedidoDAO.actualizarFechaPagoVirtual(idLink, tipoPago);
					Correo correo = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOWOMPI", "CLAVECORREOWOMPI");
					ArrayList correos = new ArrayList();
					if(idPedido > 0)
					{
						//Realizamos labor de envío de notificación por WhatsApp para un número de pedido
						Pedido pedPagado = PedidoDAO.ConsultaPedido(idPedido);
						//Enviamos mensaje ultramsg
						if(pedPagado.getTelefono()!= null)
						{
							if(!pedPagado.getTelefono().equals(new String("")))
							{
								PedidoCtrl.enviarWhatsAppUltramsg("Querido " + pedPagado.getNombrecliente() + " hemos acabado de recibir la notificación de tu pago, a partir de este momento tu pedido será enviado a elaboración en una de nuestras tiendas!", pedPagado.getTelefono());
							}
						}
						//Enviaremos un correo en la etapa de piloto
						correo.setAsunto("PAGO IDLINK  " + idLink);
						String correoEle = "jubote1@gmail.com";
						correos.add(correoEle);
						correo.setContrasena(infoCorreo.getClaveCorreo());
						correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
						correo.setMensaje(" Se realizó pago con el inLink en cuestión " + idLink);
						//ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
						//contro.enviarCorreo();
					}else
					{
						//En este punto sabemos que el pago no es del contact center posiblemente, por lo tanto posiblemente es de tienda
						boolean actualizaTienda = PedidoPagoVirtualConsolidadoDAO.validarPagoWompiTienda(idLink, tipoPago);
						if(!actualizaTienda)
						{
							//Controlamos el caso de que no encontró un idLink para actualizar debería de notificarlo
							//Enviaremos un correo en la etapa de piloto
							correo.setAsunto("ATENCIÓN PAGO IDLINK  " + idLink + " dicho link no aparece en un pedido");
							correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
							correo.setContrasena(infoCorreo.getClaveCorreo());
							correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
							correo.setMensaje(" Se realizó pago con el inLink en cuestión " + idLink + ", sin embargo en estos momentos no hay un pedido con dicho link por favor revisar.");
							ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
							contro.enviarCorreo();
						}	
					}
					
					 
					 //En este punto vamos a revisar el tema de como enviar el pedido a la tienda y actualizar la trazabilidad del pedido DE PAGO VIRTUAL
					 
				 }
			 }
			 
		}catch (Exception e) {
            e.printStackTrace();
		} 
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	public String capturarEventoPagoEpayco(int idOrdenComercio, int codRespuesta , String tipoPago)
	{
		//Dejamos un log de la operación
		LogPedidoVirtualDAO.insertarLogPedidoEpayco("idOrdenComercio " + idOrdenComercio  + " codRespuesta " + codRespuesta + " tipoPago " + tipoPago);
		// Debemos de procesar todo datos para extraer todo los datos requeridos
		JSONParser parser = new JSONParser();
		try {
			  
		}catch (Exception e) {
            e.printStackTrace();
		} 
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("respuesta", "OK" );
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	
	/**
	 * Método que se encargará de tener toda la lógica para la descomposición del JSON enviado para tomar el pedido desde el sistema de 
	 * tienda virtual de KUNO.
	 * @param stringJSON
	 * @return
	 */
	public String insertarPedidoTiendaVirtualKuno(String stringJSON, String authHeader)
	{
		//Realizamos la inserción de log con el JSON recibido
		LogPedidoVirtualKunoDAO.insertarLogPedidoVirtualKuno(stringJSON, authHeader);
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			AutenticacionCtrl autCtrl = new AutenticacionCtrl();
			boolean respuesta = autCtrl.validarAuthKuno(authHeader);
			if(!respuesta)
			{
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
				ArrayList correos = new ArrayList();
				correo.setAsunto("ATENCIÓN POSIBLE ATAQUE A SERVICIO DE KUNO  " + authHeader);
				String correoEle = "jubote1@gmail.com";
				correos.add(correoEle);
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje(" Se tiene una invocación al servicio con Authorization no autorizado, el cual es  " + authHeader);
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
				return("");
			}
		}
		
		//Varibles para el procesamiento del pedido
		//Variable donde quedará almacenada el idPedido creado
		int idPedidoCreado = 0;
		long idOrdenComercio = 0;
		//Variable que le asignará un id único al pedido de tienda virtual que llega
		int idInterno = 0;
		//Variable donde almacenaremos el idCliente
		int idCliente = 0;
		//Variable para almacenar el precio total
		long valorTotal = 0;
		long valorTotalContact = 0;
		String estadoPedido = "";
		boolean esProgramado = false;
		//Variable donde se almacenará la hora de programado del pedido con S o N y la hora de programado
		//Inicializamos ambas variables con los valores 
		String programado = "N";
		String horaProgramado = "AHORA";
		//Variable donde almacenaremos el tipoPedido
		String tipoPedido = "";
		int idTipoPedido = 1;
		//Formamos la fecha del pedido que podrá cambiar si el pedido es posfechado
		Date fechaPedido = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
		String strFechaFinal = formatoFinal.format(fechaPedido);
		
		//Comenzaremos a parsear el JSON que nos llego
		JSONParser parser = new JSONParser();
		try
		{
			//Realizamos el parseo del primer nivel del JSON
			Object objParser = parser.parse(stringJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			//Descomponemos la información de orders que es donde está agrupado toda la info del pedido
			String ordersJSON = (String)jsonGeneral.get("orders").toString();
			Object objParserOrders = parser.parse(ordersJSON);
			JSONArray jsonOrdersArray = (JSONArray) objParserOrders;
			//Recorremos el arreglo
			for(int i = 0; i < jsonOrdersArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) jsonOrdersArray.get(i);
				//Comenzamos capturando la información del cliente del pedido
				//Capturamos el número de orden en el ecommerce
				idOrdenComercio = (long)objTemp.get("id");
				valorTotal = (long)objTemp.get("total_price");
				//Agregamos análisis para programación de pedidos  y si el pedido está aceptado
				esProgramado = (boolean)objTemp.get("for_later");
				estadoPedido = (String)objTemp.get("status");
				if(estadoPedido.equals(new String("pending")) && !esProgramado)
				{
					
				}
				else
				{
					//Definimos variable que nos indicará para que estado va el pedido la inicializamos el 2
					int idEstadoPedido = 2;
					//Insertamos la traza inicial del pedido y retornamos el id
					idInterno = PedidoTiendaVirtualDAO.insertarPedidoTiendaVirtual(((Long)idOrdenComercio).intValue());
					//Controlaremos si idInterno es cero para no continuar con la inserción
					if(idInterno == 0 && !esProgramado )
					{
						Correo correo = new Correo();
						CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
						ArrayList correos = new ArrayList();
						correo.setAsunto("TIENDA VIRTUAL KUNO INTENTO PEDIDO DUPLICADO   " + idOrdenComercio);
						String correoEle = "jubote1@gmail.com";
						correos.add(correoEle);
						correo.setContrasena(infoCorreo.getClaveCorreo());
						correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
						correo.setMensaje(" Se tuvo problema creando el pedido   " + idOrdenComercio + " dado que ya esta creado y se estaba intentando duplicar.");
						ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
						contro.enviarCorreo();
					}else if(idInterno == 0 && esProgramado && estadoPedido.equals(new String("accepted")))
					{
						//Deberíamos de incluir la lógica para decir que es un pedido programado y fue aceptado
						PedidoDAO.actualizarProgramadoFinalizado((int)idOrdenComercio);
					}
					else
					{
						//Vamos a revisar si el pedido es programado
						if(esProgramado)
						{
							idEstadoPedido = 5;
							programado = "S";
							//En este punto vamos a tener la lógica para extraer la fecha del pedido programado requerimos tomar
							//para cuando está programado el pedido
							//Tenemos en cuenta que esto viene según la fecha ZULU
							String strFechaPreparacionZulu = (String)objTemp.get("fulfill_at");
							//otra prueba
							DateFormat utcFormato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
							utcFormato.setTimeZone(TimeZone.getTimeZone("UTC"));
							Date fechaUTC = utcFormato.parse(strFechaPreparacionZulu);
							DateFormat localFormato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
							Calendar calendar = Calendar.getInstance(); 
							localFormato.setTimeZone(calendar.getTimeZone());
							String strFechaLocal = localFormato.format(fechaUTC);
							Date fechaLocal = localFormato.parse(strFechaLocal);
							//Ya tenemos la fecha para la cual es el pedido vamos a sacar la hora con minutos
							calendar.setTime(fechaLocal);
							int hora = calendar.get(Calendar.HOUR_OF_DAY);
							int minutos = calendar.get(Calendar.MINUTE);
							if(hora < 10)
							{
								horaProgramado = "0" + Integer.toString(hora);
							}else
							{
								horaProgramado = Integer.toString(hora);
							}
							if(minutos < 10)
							{
								horaProgramado = horaProgramado + ":" +"0" + Integer.toString(minutos);
							}else
							{
								horaProgramado = horaProgramado + ":" +Integer.toString(minutos);
							}
							int mes = calendar.get(Calendar.MONTH) + 1;
							int ano = calendar.get(Calendar.YEAR);
							int dia = calendar.get(Calendar.DAY_OF_MONTH);
							String fechaPedidoPro = "";
							if(dia < 10)
							{
								fechaPedidoPro = "0" + Integer.toString(dia);
							}else
							{
								fechaPedidoPro = Integer.toString(dia);
							}
							if(mes < 10)
							{
								fechaPedidoPro = fechaPedidoPro + "/"+ "0" + Integer.toString(mes);
							}else
							{
								fechaPedidoPro = fechaPedidoPro +  "/"+ Integer.toString(mes);
							}
							fechaPedidoPro = fechaPedidoPro + "/" + Integer.toString(ano);
							
							//Se hace la diferenciación que la fecha final será la del pedido programado
							strFechaFinal = fechaPedidoPro;
						}
						
						//Continuamos con el procesamiento del pedido
						String telefono = (String)objTemp.get("client_phone");
						String telefonoCelular = "";
						//El telefono le quitaremos el indicativo del país
						if(telefono.substring(0, 3).equals(new String("+57")))
						{
							telefono = telefono.substring(3);
						}
						//Para el caso del teléfono validaremos si es un fijo
						if(telefono.trim().length() == 7)
						{
							//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
							//contact center.
							telefono = "604" + telefono;
						}else
						{
							telefonoCelular = telefono;
						}
						String nombres;
						try {
							nombres = (String)objTemp.get("client_first_name");
							nombres = nombres.replaceAll("'", " ");
						}catch(Exception enombre)
						{
							nombres = "NO SE PUDO EXTRAR EL NOMBRE";
						}
						String apellidos;
						try {
							apellidos = (String)objTemp.get("client_last_name");
							apellidos = apellidos.replaceAll("'", " ");
						}catch(Exception enombre)
						{
							apellidos = "NO SE PUDO EXTRAR EL APELLIDO";
						}
						//Tendremos un trato diferencial para la dirección
						String dirRes = "";
						String ciudad = "";
						String dirAdicional = "";
						//Trabajamos sobre la dirección resumida
						//Debemos de crear otro objeto JSON
						JSONObject infoAdiDir = (JSONObject)objTemp.get("client_address_parts");
						try
						{
							dirRes = (String)infoAdiDir.get("street");
							dirRes = dirRes.replaceAll("'", " ");
							if(dirRes == null)
							{
								dirRes = "";
							}
						}catch(Exception e)
						{
							dirRes = "";
						}
						//Trabajamos sobre la ciudad
						try
						{
							ciudad = (String)infoAdiDir.get("city");
							ciudad = ciudad.replaceAll("'", " ");
							if(ciudad == null)
							{
								ciudad = "";
							}
						}catch(Exception e)
						{
							ciudad = "";
						}
						//Trabajamos sobre la info adicional
						try
						{
							dirAdicional = (String)infoAdiDir.get("more_address");
							dirAdicional = dirAdicional.replaceAll("'", " ");
							if(dirAdicional == null)
							{
								dirAdicional = "";
							}
						}catch(Exception e)
						{
							dirAdicional = "";
						}
						
						//Trabajamos sobre el campo Instrucciones
						String instrucciones = "";
						try
						{
							instrucciones = (String)objTemp.get("instructions");
							instrucciones = instrucciones.replaceAll("'", " ");
							if(instrucciones == null)
							{
								instrucciones = "";
							}
						}catch(Exception e)
						{
							instrucciones = "";
						}
						
						String direccion = (String)objTemp.get("client_address");
						direccion = direccion.replaceAll("'", " ");
						//Pendiente revisar como incorporaremos estas informaciones
						String zonaBarrio = "";
						String obsDireccion = "";
						//Tendremos unas reglas para conformar la dirección
						if(dirAdicional.equals(new String("")))
						{
							
						}else
						{
							direccion = dirRes + " " + ciudad;
							obsDireccion = dirAdicional;
						}
						
						
						String email  = (String)objTemp.get("client_email");
						//En ocasiones cuando no es definida la latitud ni la longitud esta llega como un String por lo
						//tanto es necesario incluirlas dentro de un try y si hay excepción llenar con cero los valores
						double latitud = 0, longitud = 0;
						try
						{
							latitud = Double.parseDouble((String)objTemp.get("latitude"));
						}catch(Exception e)
						{
							latitud = 0;
							System.out.println(e.toString());
						}
						try
						{
							longitud = Double.parseDouble((String)objTemp.get("longitude"));
						}catch(Exception e)
						{
							longitud = 0;
							System.out.println(e.toString());
						}
						//Realizamos la intervención para tratar en el momentoen que la ubicación viene en cero
						if(latitud== 0 && longitud == 0)
						{
							//Cambiamos para el llamado del método el resume
							UbicacionCtrl  ubicacion = new UbicacionCtrl();
							Ubicacion ubicaResp = ubicacion.ubicarDireccionEnTiendaBatch(direccion + " " + ciudad);
							latitud = ubicaResp.getLatitud();
							longitud = ubicaResp.getLongitud();
							
//							//Realizaremos el intengo de realizar el llamado para la dirección
//							try
//							{
//							String resultado = "";
//					        String apikey ="GsNtuVVEgGawtZDJwrcB_YcNs0lQ0JcMa5UXYQZN3wU";
//					        if(ciudad.equals(new String("")))
//					        {
//					        	ciudad = "Medellín";
//					        }
//					        String dirBuscar= direccion+",Colombia,"+ ciudad +",Antioquia";
//					        String connstr = "https://geocoder.ls.hereapi.com/6.2/geocode.json?apiKey="+apikey+"&searchtext="+ URLEncoder.encode(dirBuscar,"UTF-8");
//					        //Realizamos la invocación mediante el uso de HTTPCLIENT
//							HttpClient client = HttpClientBuilder.create().build();
//							HttpGet request = new HttpGet(connstr);
//							
//								StringBuffer retorno = new StringBuffer();
//								HttpResponse responseFinPed = client.execute(request);
//								BufferedReader rd = new BufferedReader
//									    (new InputStreamReader(
//									    		responseFinPed.getEntity().getContent()));
//								String line = "";
//								while ((line = rd.readLine()) != null) {
//									    retorno.append(line);
//									}
//								resultado = retorno.toString();
//								//Posteriormente realizamos la conversión del objeto JSON para tener la latitud y la longitud
//								Object objParserServicio = parser.parse(resultado);
//								JSONObject jsonObjectGeojsonGeneral = (JSONObject) objParserServicio;
//								String strResponse = jsonObjectGeojsonGeneral.get("Response").toString();
//								objParserServicio = parser.parse(strResponse);
//								jsonObjectGeojsonGeneral = (JSONObject) objParserServicio;
//								String strView = (String)jsonObjectGeojsonGeneral.get("View").toString();
//								objParserServicio = parser.parse(strView);
//								JSONArray View = (JSONArray) objParserServicio;
//					            JSONObject coodernadas =  new JSONObject();
//					            if (View.toString().length() > 0) {
//					            	JSONObject objTempServicio = (JSONObject) View.get(0);
//					            	String strResult = (String)objTempServicio.get("Result").toString();
//					            	objParserServicio = parser.parse(strResult);
//					            	JSONArray result = (JSONArray) objParserServicio;
//					            	objTempServicio = (JSONObject) result.get(0);
//					            	JSONObject location = (JSONObject) objTempServicio.get("Location");
//					            	String strNavigationPosition = (String)location.get("NavigationPosition").toString();
//					            	objParserServicio = parser.parse(strNavigationPosition);
//					            	JSONArray navigationPosition  = (JSONArray) objParserServicio;
//					            	objTempServicio = (JSONObject) navigationPosition.get(0);
//					            	latitud = (Double)objTempServicio.get("Latitude");
//					            	longitud = (Double)objTempServicio.get("Longitude");
//					            	//coodernadas =  View.getJSONObject(0).getJSONArray("Result").getJSONObject(0).getJSONObject("Location").getJSONArray("NavigationPosition").getJSONObject(0);
//					            } 
//
//							}catch (Exception e2) {
//						        e2.printStackTrace();
//						        System.out.println("ERROR " + e2.toString());
//						    }
						}
						tipoPedido = (String)objTemp.get("type");
						if(tipoPedido.equals(new String("delivery")))
						{
							idTipoPedido = 1;
						}else if(tipoPedido.equals(new String("pickup")))
						{
							idTipoPedido = 2;
						}
						//El servicio de Kuno permite identificar el restaurante con el campo restaurante token
						
						//Capturamos el token del restaurante para conocer la tienda y el origen
						int token = Integer.parseInt((String) objTemp.get("restaurant_token"));
						HomologacionTiendaToken homoTiendaToken = HomologacionTiendaTokenDAO.obtenerHomologacionTiendaToken(token);
						int idTienda = homoTiendaToken.getIdtienda();
						String origenPedido = homoTiendaToken.getOrigen();
	
						
						//Vamos por la forma de pago en su homologación
						//Realizamos la captura del método de pago
						String formPagVirtual = (String)objTemp.get("payment");
						//Realizamos la homologación de la formad de pago
						ParametrosCtrl parCtrl = new ParametrosCtrl();
						int idFormaPago = parCtrl.realizarHomologacionFormaPagoTiendaVirtual(formPagVirtual);				
						//Realizamos la creación de cliente y su procesamiento
						//Creamos un objeto de la clase cliente y creamos un método de se encargue de procesar el cliente
						Cliente clienteVirtual = new Cliente(0, telefono, nombres, direccion, zonaBarrio, obsDireccion,"", idTienda);
						clienteVirtual.setApellidos(apellidos);
						clienteVirtual.setEmail(email);
						clienteVirtual.setIdtienda(idTienda);
						clienteVirtual.setLatitud((float)latitud);
						clienteVirtual.setLontitud((float)longitud);
						clienteVirtual.setTelefonoCelular(telefonoCelular);
						clienteVirtual.setPoliticaDatos("S");
						
						//Realizamos la validación del cliente con toda la lógica en la capa Controladora de Cliente
						ClienteCtrl clienteCtrl = new ClienteCtrl();
						//Aprovecharemos que los objetos se pasan como valores por referencia por lo tanto las modificaciones realizadas al objeto tendrán mucho que ver
						idCliente = clienteCtrl.validarClienteTiendaVirtualKuno(clienteVirtual, instrucciones);
						if(idCliente == 0)
						{
							Correo correo = new Correo();
							CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
							ArrayList correos = new ArrayList();
							correo.setAsunto("ERROR GRAVE CLIENTE NO CREADO ACTUALIZADO  " + idOrdenComercio);
							String correoEle = "jubote1@gmail.com";
							correos.add(correoEle);
							correos.add("lidercontactcenter@pizzaamericana.com.co");
							correo.setContrasena(infoCorreo.getClaveCorreo());
							correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
							correo.setMensaje(" Se tiene un problema creando el pedido duplicado de RAPPI número  " + idOrdenComercio);
							ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
							contro.enviarCorreo();
						}
						//Vamos a proceder a realizar el procesamiento del pedido
	
						//Continuamos con el procesamiento de la orden con los productos
						String ItemsJSON = (String)objTemp.get("items").toString();
						Object objParserLineItems = parser.parse(ItemsJSON);
						JSONArray jsonItemsArray = (JSONArray) objParserLineItems;
						
						//Vamos a proceser la fuente del pedido
						String fuentePedido = (String)objTemp.get("source");
						
						//Tenemos que crear un nuevo método para el procesamiento según como se recibe información de KUNO
						//Creamos un arreglo para traer los descuentos de cupones
						ArrayList<Double> cupones = new ArrayList();
						idPedidoCreado = ingresarPedidoTiendaVirtualKuno(jsonItemsArray, idTienda, idCliente, strFechaFinal,idOrdenComercio, idTipoPedido,cupones,origenPedido, fuentePedido);
						//Calculamos el descuento del pedido luego de ingresado
						Double descuentoPedido = obtenerDescuentoDiarioPedidoVal(idPedidoCreado, "T");
						//En caso de que no se tenga descuento por % en lo ya establecido, revisamos si hay descuento con cupón
						if(descuentoPedido == 0)
						{
							if(cupones.size()>0)
							{
								for(int z = 0; z < cupones.size(); z++)
								{
									descuentoPedido =  descuentoPedido + (double)cupones.get(z);
								}
							}	
						}
						//Posteriormente realizamos los pasos para la finalización del pedido
						int tiempoPedido = TiempoPedidoDAO.retornarTiempoPedidoTienda(idTienda);
						//Consultaremos el tiempo que la tienda está dando en el momento
						valorTotalContact = PedidoDAO.calcularTotalNetoPedido(idPedidoCreado);
						long longDescuento = descuentoPedido.longValue();
						valorTotalContact = valorTotalContact - longDescuento;
						//Realizamos un cambio temporal para evitar las diferencias pero igual seguirán llegando los correos
						FinalizarPedidoTiendaVirtual(idPedidoCreado, idFormaPago, idCliente, tiempoPedido, "S", descuentoPedido, "DESCUENTOS GENERALES DIARIOS", (valorTotalContact), programado, horaProgramado, idEstadoPedido);
						
						
						if(valorTotalContact != valorTotal)
						{
							//Realizamos un control para verificar si la diferencia proviene de una diferencia del valor del domicilio
							if(!cobroVariosDomicilios)
							{
								//Realizamos la división por el valor del domicilio y si da un número entero
								Correo correo = new Correo();
								CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
								ArrayList correos = new ArrayList();
								correo.setAsunto("OJO TOTAL PEDIDO TIENDA VIRTUAL TIENE ERRORES EN LOS TOTALES  " + idOrdenComercio + " PEDIDO "  + idPedidoCreado);
								String correoEle = "jubote1@gmail.com";
								correos.add(correoEle);
								correo.setContrasena(infoCorreo.getClaveCorreo());
								correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
								correo.setMensaje(" Se tiene un problema con el pedido   " + idOrdenComercio + " dado que los totales no coinciden entre lo que venía en la tienda virtual y lo que arrojo la creación en el sistema de contact center.");
								ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
								contro.enviarCorreo();
							}	
						}
						
						//Intervenimos cuando el idFormaPago es igual a 4 es porque es WOMPI y realizaremos el envío del link del pedido para pago al cliente
						if(idFormaPago == 4)
						{
							verificarEnvioLinkPagos(idPedidoCreado, clienteVirtual, (valorTotal), idTienda);
						}
						
						//Actualizamos la fecha de procesamiento
						PedidoTiendaVirtualDAO.actualizarPedidoTiendaVirtual(idInterno, "PRO");
						
					}
				}
			}
		}catch(Exception e)
		{
			System.out.println(e.toString());
			PedidoTiendaVirtualDAO.actualizarPedidoTiendaVirtual(idInterno, "ERR");
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
			ArrayList correos = new ArrayList();
			correo.setAsunto("TIENDA VIRTUAL ERROR CREANDO ORDEN   " + idOrdenComercio);
			String correoEle = "jubote1@gmail.com";
			correos.add(correoEle);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje(" Se tuvo problema creando la orden del cliente  " + idOrdenComercio);
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
		}
		
		return("");
	}
	
	/**
	 * Método base para la descomposición del JSON con el objetivo de extraer todos los datos necesarios para traspasar el pedido
	 * desde el ecommerce hacia el sistema de pedidos del contact center.
	 * @param stringJSON
	 * @return
	 */
	public String insertarPedidoTiendaVirtual(String stringJSON)
	{
		//Formamos la fecha del pedido
		Date fechaPedido = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
		String strFechaFinal = formatoFinal.format(fechaPedido);
		
		//Realizamos la inserción de log con el JSON recibido
		LogPedidoVirtualDAO.insertarLogPedidoVirtual(stringJSON);
		JSONParser parser = new JSONParser();
		//Variable que le asignará un id único al pedido de tienda virtual que llega
		int idInterno = 0;
		//Variable donde almacenaremos el idCliente
		int idCliente = 0;
		//Variable donde quedará almacenada el idPedido creado
		int idPedidoCreado = 0;
		long idOrdenComercio = 0;
		try
		{
			//Realizamos el parseo del primer nivel del JSON
			Object objParser = parser.parse(stringJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			
			//Capturamos el número de orden en el ecommerce
			idOrdenComercio = (long)jsonGeneral.get("id");
			
			//Insertamos la traza inicial del pedido y retornamos el id
			idInterno = PedidoTiendaVirtualDAO.insertarPedidoTiendaVirtual(((Long)idOrdenComercio).intValue());
			
			//Comenzamos la captura de información del cliente que está agrupada en billing
			String billingJSON = (String)jsonGeneral.get("billing").toString();
			Object objParserBilling = parser.parse(billingJSON);
			JSONObject jsonBilling = (JSONObject) objParserBilling;
			String telefono = (String)jsonBilling.get("phone");
			//Para el caso del teléfono validaremos si es un fijo
			if(telefono.trim().length() == 7)
			{
				//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
				//contact center.
				telefono = "604" + telefono;
			}
			String nombres = (String)jsonBilling.get("first_name");
			String apellidos = (String)jsonBilling.get("last_name");
			String direccion = (String)jsonBilling.get("address_1");
			String email  = (String)jsonBilling.get("email");
			
			//Realizamos la extracción de la tienda o sitio donde se realizó la venta
			String tiendaVenta = "";
			String zonaBarrio = "";
			String obsDireccion = "";
			String strTiempoPedido = "";
			String metaDataJSON = (String)jsonGeneral.get("meta_data").toString();
			Object objParserMetaData = parser.parse(metaDataJSON);
			JSONArray jsonMetaData = (JSONArray) objParserMetaData;
			//Variable donde se almacenará como se va a pagar el pedido
			double valorFormaPago = 0;
			for(int i = 0; i < jsonMetaData.size(); i++)
			{
				JSONObject objTemp = (JSONObject) jsonMetaData.get(i);
				String key = (String)objTemp.get("key");
				if(key.equals(new String("_billing_sede")))
				{
					tiendaVenta = (String) objTemp.get("value");
				}
				if(key.equals(new String("additional_Zona/Barrio")))
				{
					zonaBarrio = (String) objTemp.get("value");
				}
				if(key.equals(new String("additional_Observación_dirección")) || key.equals(new String("additional_Observacion_direccion")))
				{
					obsDireccion = (String) objTemp.get("value");
				}
				if(key.equals(new String("_billing_cantidad_pago")))
				{
					try
					{
						valorFormaPago = Double.parseDouble((String) objTemp.get("value"));
					}catch (Exception e)
					{
						valorFormaPago = 0;
					}
					zonaBarrio = (String) objTemp.get("value");
				}
				if(key.equals(new String("_billing_tiempo")))
				{
					try {
						strTiempoPedido = (String) objTemp.get("value");
					}catch(Exception e)
					{
						strTiempoPedido = "50 minutos";
					}
				}
			}
			
			//Realizamos una serie de validaciones para poder revisar las acciones a tomar
			if(zonaBarrio.length() > 100)
			{
				obsDireccion = obsDireccion + " " + zonaBarrio;
				zonaBarrio = zonaBarrio.substring(0,100);
			}
			
			if(obsDireccion.length() > 200)
			{
				obsDireccion = obsDireccion.substring(0,200);
			}
			
			//Realizamos la captura del método de pago
			String formPagVirtual = (String)jsonGeneral.get("payment_method");
			
			//Realizamos la homologación de la formad de pago
			ParametrosCtrl parCtrl = new ParametrosCtrl();
			int idFormaPago = parCtrl.realizarHomologacionFormaPagoTiendaVirtual(formPagVirtual);
			
			//Realizamos la homologación de la tienda
			TiendaCtrl tiendaCtrl = new TiendaCtrl();
			int idTiendaContact = tiendaCtrl.realizarHomologacionTiendaVirtual(tiendaVenta);
			
			//Creamos un objeto de la clase cliente y creamos un método de se encargue de procesar el cliente
			Cliente clienteVirtual = new Cliente(0, telefono, nombres, direccion, zonaBarrio, obsDireccion,"", idTiendaContact );
			clienteVirtual.setApellidos(apellidos);
			clienteVirtual.setEmail(email);
			clienteVirtual.setIdtienda(idTiendaContact);
			clienteVirtual.setPoliticaDatos("S");
			//Controlaremos que el teléfono tenga +57
			if(telefono.substring(0, 3).equals(new String("+57")))
			{
				telefono = telefono.substring(3);	
			}
			//Controlaremos que el teléfono tenga 57
			if(telefono.substring(0, 3).equals(new String("57")))
			{
				telefono = telefono.substring(2);	
			}
			
			//Realizamos modificacion para ver si podemos fijar el numero celular
			if(telefono.substring(0, 1).equals(new String("3")))
			{
				clienteVirtual.setTelefonoCelular(telefono);
			}
			
			//Vamos a realizar la notificación del correo electrónico al cliente indicando que tomamos su pedido
			notificarCorreoCliente(email, nombres + " " + apellidos, direccion , zonaBarrio, obsDireccion , telefono,idOrdenComercio);
			
			//Realizamos la validación del cliente con toda la lógica en la capa Controladora de Cliente
			ClienteCtrl clienteCtrl = new ClienteCtrl();
			//Aprovecharemos que los objetos se pasan como valores por referencia por lo tanto las modificaciones realizadas al objeto tendrán mucho que ver
			idCliente = clienteCtrl.validarClienteTiendaVirtual(clienteVirtual);
			
			//Continuamos con el procesamiento de la orden con los productos
			String lineItemsJSON = (String)jsonGeneral.get("line_items").toString();
			//Reazliamos la conversion se shipping_lines para extraer la info del domicilio
			String shippingLinesJSON = (String)jsonGeneral.get("shipping_lines").toString();
			Object objParserLineItems = parser.parse(lineItemsJSON);
			Object objParserShippingLines = parser.parse(shippingLinesJSON);
			JSONArray jsonLineItems = (JSONArray) objParserLineItems;
			JSONArray jsonShippingLines = (JSONArray) objParserShippingLines;
			idPedidoCreado = ingresarPedidoTiendaVirtual(jsonLineItems, idTiendaContact, idCliente, strFechaFinal,jsonShippingLines, (int)idOrdenComercio);
			//Calculamos el descuento del pedido luego de ingresado
			Double descuentoPedido = obtenerDescuentoDiarioPedidoVal(idPedidoCreado, "T");
			//Posteriormente realizamos los pasos para la finalización del pedido
			//Realizamos el procesamiento del tiempo pedido para enviarlo como parámetro
			StringTokenizer strTokenTiempo = new StringTokenizer(strTiempoPedido, " ");
			Double tiempoPedido = 0.0;
			while(strTokenTiempo.hasMoreTokens())
			{
				try
				{
					strTiempoPedido = strTokenTiempo.nextToken();
					break;
				}catch(Exception e)
				{
					strTiempoPedido = "55";
				}
				
			}
			try
			{
				tiempoPedido = Double.parseDouble(strTiempoPedido);
			}catch(Exception ex)
			{
				tiempoPedido = 55.0;
			}
			FinalizarPedidoTiendaVirtual(idPedidoCreado, idFormaPago, idCliente, tiempoPedido, "S", descuentoPedido, "DESCUENTOS GENERALES DIARIOS", valorFormaPago, "N", "AHORA",2);
		
			//Actualizamos la fecha de procesamiento
			PedidoTiendaVirtualDAO.actualizarPedidoTiendaVirtual(idInterno, "PRO");
			
		}catch(Exception e)
		{
			
			System.out.println(e.toString());
			//Actualizamos la fecha de procesamiento
			PedidoTiendaVirtualDAO.actualizarPedidoTiendaVirtual(idInterno, "ERR");
			//En fase de piloto envíamos un correo electrónico porque hubo error
			//Enviaremos un correo en la etapa de piloto
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
			ArrayList correos = new ArrayList();
			correo.setAsunto("TIENDA VIRTUAL ERROR CREANDO ORDEN   " + idOrdenComercio);
			String correoEle = "jubote1@gmail.com";
			correos.add(correoEle);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje(" Se tuvo problema creando la orden del cliente  " + idOrdenComercio);
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
		}
		

		
		JSONArray listJSON = new JSONArray();
		JSONObject precioJSON = new JSONObject();
		precioJSON.put("idpedido", idPedidoCreado);
		listJSON.add(precioJSON);
		return listJSON.toJSONString();
	}
	
	/**
	 * Método que se encargará de todo el proceso de inserción del pedido y sus detalles de lo que viene del sistema de tienda virtual.
	 * @param detallePedido
	 * @param idTienda
	 * @param idCliente
	 * @param fechaPedido
	 * @return
	 */
	public int ingresarPedidoTiendaVirtual(JSONArray detallePedido, int idTienda, int idCliente, String fechaPedido, JSONArray infoCobroDomi, int idOrdenComercio)
	{
		//Manejaremos una variable que nos indicará si ya cobramos domicilio para cobrarlo una sola vez
		boolean cobradoDomicilio = false;
		ParametrosCtrl parCtrl = new ParametrosCtrl();
		//Procesaremos inicial el cobro o no del domicilio
		//Con la siguiente variable deberemos de validar si se debe cobrar domicilio y cual sería el producto para este fin.
		int idProductoDomicilio = 0;
		String strHomDomicilio = "";
		long valorDomicilio = 0;
		// 1. Debemos extraer la información de el cobro del domicilio para el pedido
		for(int i = 0; i < infoCobroDomi.size(); i++) 
		{
			JSONObject infoDomicilioTemp = (JSONObject) infoCobroDomi.get(i);
			//Extraremos el total del item
			strHomDomicilio = (String)infoDomicilioTemp.get("method_title");
			if(strHomDomicilio.contains("Valor del domicilio"))
			{
				//Sabiendo que estamos en el campo que queremos conocer, verificamos y traemos el valor de domicilio
				valorDomicilio = Long.parseLong((String)infoDomicilioTemp.get("total"));
				//Si el valor de domicilio es mayor a cero deberemos de recuperar el producto para agregarlo posteriormente finalizando el pedido
				if(valorDomicilio > 0)
				{
					idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual(strHomDomicilio); 
				}
				break;
			}
		}
		
		
		// 2. Traemos los productos incluidos para validación
		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();
		//IdPedido valor a devolver con la creación del pedido
		int idPedido = 0;
		//3. Realizaremos la inserción del ENCABEZADO PEDIDO
		idPedido = PedidoDAO.InsertarEncabezadoPedidoTiendaVirtual(idTienda, idCliente, fechaPedido, "TIENDA VIRTUAL", idOrdenComercio);
		//Realizamos el recorrido de uno a uno los detalles del pedido
		//Tendremos un valor para el idPedido que crearemos una vez
		
		long valorTotalItemJSON = 0;
		//Definimos las variables que nos serviran para obtener el código de producto y las especialidades
		int idProducto = 0;
		//Variable donde almacenaremos el idAdicion
		int idProductoAdicion = 0;
		//Variable para almacenar el tipo de liquido
		int idSaborTipoLiquido = 0;
		//Variable para almacenar el producto condimento
		int idProductoCond = 0;
		//Variable para almacenar el producto con
		int idProductoCon = 0;
		//Variable donde almacenamos el String de las adiciones
		String strAdiciones = "";
		//Variable donde almacenamos el String de modificador con
		String strModCon = "";
		int idEspecialidad = 0;
		//Agregamos la especialidad2 en caso de ser mitad y mitad
		int idEspecialidad2 = 0;
		//Tendremos una homologación del excepción de precio
		int idExcepcion = 0;
		double valorExcepcion = 0;
		//Tendremos dos variables para controlar cual mitad es la que estamos procesando
		boolean mitad1, mitad2;
		boolean sel1, sel2;
		double valorUnitario = 0;
		//Variable donde extraeremos la cantidad de lo que estamos pidiendo
		double cantidad = 0;
		String sku = "";
		//Variable del detalle Pedido insertado
		int idDetInser = 0;
		//Definimos el idDetallePadre 
		int idDetallepedido = 0;
		//Esta variable es para el tema del tamaño
		String tamanoPizza = "";
		for(int i = 0; i < detallePedido.size(); i++) 
		{
			strAdiciones = "";
			strModCon = "";
			//Iniciamos la variable de control de mitades
			mitad1 = true;
			mitad2 = false;
			sel1 = true;
			sel2 = false;
			//Tendremos un arreglo con las adiciones para este line_items
			ArrayList<AdicionTiendaVirtual> adiciones = new ArrayList();
			//Tendremos un arreglo con los condimentos
			ArrayList<AdicionTiendaVirtual> modificadoresCon = new ArrayList();
			//Creamos objeto de AdicionTiendaVirtual temporal
			AdicionTiendaVirtual adiTemp = new AdicionTiendaVirtual();
			//Extraemos uno a uno los items del pedido
			JSONObject detallePedidoTemp = (JSONObject) detallePedido.get(i);
			//Extraremos el total del item
			valorTotalItemJSON = Long.parseLong((String)detallePedidoTemp.get("subtotal"));
			//Extreamos la cantidad
			long tmp = (long)detallePedidoTemp.get("quantity");
			Long lngCantidad = Long.valueOf(tmp);
			cantidad = lngCantidad.doubleValue();
			//Extraemos el código SKU que nos dará pistas de lo que tiene el item
			sku = (String) detallePedidoTemp.get("sku");
			//Una vez obtenido el SKU podemos detectar varias cosas
			//Si la longitud es igual a 3 significa que es un producto de los solos
			//Llevamos a cero ambos valores
			idProducto = 0;
			idEspecialidad = 0;
			idEspecialidad2 = 0;
			idExcepcion = 0;
			valorExcepcion = 0;
			if(sku.length() == 3)
			{
				idProducto = parCtrl.homologarProductoTiendaVirtual(sku);
				//En este caso es una pizza de tamaño y especialidad
			}else if(sku.length()>= 6 && sku.length()<= 9)
			{
				if(sku.length() == 6) 
				{
					tamanoPizza = sku.substring(3,6);
					idProducto = parCtrl.homologarProductoTiendaVirtual(tamanoPizza);
					idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(sku.substring(0,3));
				}else if(sku.length() == 9)
				{
					tamanoPizza = sku.substring(6,9);
					idProducto = parCtrl.homologarProductoTiendaVirtual(tamanoPizza);
					idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(sku.substring(0,6));
					//Vamos para el caso en donde será una promoción, estas tendrán 11 caracteres
				}
			}
			else if(sku.length() == 11)
			{
				if(sku.substring(0,5).equals(new String("PROMO")))
				{
					//Traemos la excepción de precio con el SKU recuperado
					ExcepcionPrecio excepcion = parCtrl.homologarExcepcionTiendaVirtual(sku);
					//Recuperamos el idExcepción y el idProducto
					idExcepcion = excepcion.getIdExcepcion();
					idProducto = excepcion.getIdProducto();
					valorExcepcion = excepcion.getPrecio();
				}
			}
			
			//7.	En este punto ya tenemos un producto por llamarlo encabezado, inicialmente pensamos en tener el producto principal y 
			//la especialidad, necesitamos conocer las adiciones y la gaseosa con la que va la pizza
			//Debemos de procesar la metadata que viene del pedido con sus adiciones, selección de bebida y otras cosas
			JSONArray metaDataDetalle = (JSONArray) detallePedidoTemp.get("meta_data");
			String keyDetPedido = "";
			String valueDetPedido = "";
			idSaborTipoLiquido = 0;
			for(int j = 0; j < metaDataDetalle.size(); j++)
			{
				//Clareamos las variables para llevar las variables
				idProductoAdicion = 0;
				idProductoCond = 0;
				idProductoCon = 0;
				//Extraemos cada una de las meta data del detalle del pedido
				JSONObject metaDataTemp = (JSONObject) metaDataDetalle.get(j);
				keyDetPedido = (String) metaDataTemp.get("key");
				valueDetPedido = (String) metaDataTemp.get("value");
				//Ya comenzamos a tratar por cada uno, validamos si es tamaño en cuyo caso
				//Validaremos si es tamaño, si es gaseosa o si es adicion
				if(keyDetPedido.equals(new String("pa_tamanos")))
				{
					//El tamaño como tal no nos presta ningún valor, 
					//por lo tanto no hacemos nada.
				}else if(keyDetPedido.contains("Adiciones") || keyDetPedido.contains("bebida") || keyDetPedido.contains("Condimentos") || keyDetPedido.contains("Mitad y Mitad") || keyDetPedido.contains("Con") || keyDetPedido.contains("Elige la especialidad") || keyDetPedido.contains("Producto adicional") || keyDetPedido.contains("Selecciona la especialidad 1") || keyDetPedido.contains("Selecciona la especialidad 2"))
				{
					/**
					 * Para el caso de la adición deberemos de tener una lógica determinada
					 * en donde deberemos de conformar la adición en texto y posteriormente hacer la homologación
					 * y realizar la adición en el arreglo.
					 */
					StringTokenizer keyTemp = new StringTokenizer(keyDetPedido, "(");
					//Realizamos la separación de la primera parte del String siempre y cuando tenga información adicional
					while(keyTemp.hasMoreTokens())
					{
						//Sacamos la parte inicial del String.
						keyDetPedido = keyTemp.nextToken();
						//Realizamos un trim para limpiar el inicial y final del String.
						keyDetPedido = keyDetPedido.trim();
						break;
					}
					keyDetPedido = keyDetPedido + " " + valueDetPedido;
					//Con el Key formado validamos el tipo de key que tenemos
					if(keyDetPedido.contains("Adiciones"))
					{
						idProductoAdicion = parCtrl.homologarProductoTiendaVirtual(keyDetPedido);
						strAdiciones = strAdiciones + " " + idProductoAdicion + "-" + keyDetPedido;
						/**
						 * Realizamos la inserción y cobro de la adición
						 * 
						 */
						valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
						DetallePedido detPedidoAdicion = new DetallePedido(idProductoAdicion,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/, "", "");
						idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoAdicion);
						adiTemp = new AdicionTiendaVirtual();
						adiTemp.setCantidad(cantidad);
						adiTemp.setIdProductoAdicion(idProductoAdicion);
						adiTemp.setIdDetallePedido(idDetInser);
						adiciones.add(adiTemp);
						
					}else if(keyDetPedido.contains("bebida"))
					{
						if(tamanoPizza.equals(new String("")))
						{
							if(sku.substring(0,5).equals(new String("PROMO")))
							{
								idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(keyDetPedido + " " + "PROMO");
							}else if(keyDetPedido.equals(new String("LAS")))
							{
								idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(keyDetPedido + " " + "PIZ");
							}
						}else
						{
							idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(keyDetPedido + " " + tamanoPizza);
						}
					}else if(keyDetPedido.contains("Condimentos") || keyDetPedido.contains("Producto adicional"))
					{
						idProductoCond = parCtrl.homologarProductoTiendaVirtual(keyDetPedido);
						/*
						 * Realizamos la inserción de los condimentos
						 */
						valorUnitario = ProductoDAO.retornarProducto(idProductoCond).getPreciogeneral();
						DetallePedido detPedidoCond = new DetallePedido(idProductoCond,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/, "", "");
						int idDetalleAdicional = PedidoDAO.InsertarDetallePedido(detPedidoCond);
						//Revisaremos si hay productos incluidos para agregar
						for(int z = 0; z < productosIncluidos.size(); z++)
						{
							ProductoIncluido proIncTemp = productosIncluidos.get(z);
							if(proIncTemp.getIdproductopadre() == idProductoCond)
							{
								DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad(),0,0,0,0, "" , "Producto Incluido-"+idDetalleAdicional, 0, 0 /*idexcepcion*/, "", "");
								PedidoDAO.InsertarDetallePedido(detPedidoInc);
							}
						}
						
					}else if(keyDetPedido.contains("Mitad y Mitad") || keyDetPedido.contains("Elige la especialidad"))
					{
						//Si se tiene Elige la especialidad que es la que viene en promociones vamos a hacer el cambio por mitad y mitad
						keyDetPedido = keyDetPedido.replace("Elige la especialidad", "Mitad y Mitad");
						//Preguntamos si es la mitad1 o la mitad2, para saber cual especialidad deberemos de homologar
						if(mitad1)
						{
							idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
							mitad1 = false;
							mitad2 = true;
						}else if(mitad2)
						{
							idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
						}
					}else if(keyDetPedido.contains("Selecciona la especialidad 1") || keyDetPedido.contains("Selecciona la especialidad 2"))
					{
						//Si se tiene Elige la especialidad que es la que viene en promociones vamos a hacer el cambio por mitad y mitad
						keyDetPedido = keyDetPedido.replace("Selecciona la especialidad 1", "Mitad y Mitad");
						keyDetPedido = keyDetPedido.replace("Selecciona la especialidad 2", "Mitad y Mitad");
						//Preguntamos si es la mitad1 o la mitad2, para saber cual especialidad deberemos de homologar
						if(sel1)
						{
							idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
							sel1 = false;
							sel2 = true;
						}else if(sel2)
						{
							idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
						}
					}
					else if(keyDetPedido.contains("Con "))
					{
						//Le agregamos el tamaño al tema del Con + ingrediente + tamaño de Pizza para buscar la homologación
						keyDetPedido = keyDetPedido + " " + tamanoPizza;
						idProductoCon = parCtrl.homologarProductoTiendaVirtual(keyDetPedido);
						strModCon = strModCon + " " + idProductoCon + "-" + keyDetPedido + " / ";
						//Capturamos el valor unitario asociado al ModificadorCon
						valorUnitario = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
						valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
						idDetInser = 0;
						//Controlamos si el valor Unitario es mayor a cero hacemos la inserción del detalle pedido
						if(valorUnitario > 0)
						{
							DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
							idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
						}
						adiTemp = new AdicionTiendaVirtual();
						adiTemp.setCantidad(1);
						adiTemp.setIdProductoAdicion(idProductoCon);
						adiTemp.setIdDetallePedido(idDetInser);
						modificadoresCon.add(adiTemp);
					}
				}
				
			}
			//Luego de procesar la meta_data 
			//En este punto comenzamos a realizar la inserción del detalle del pedido
			//Validamos en caso de que no sea excepción es el precio full
			if (idExcepcion == 0)
			{
				valorUnitario = ProductoDAO.retornarProducto(idProducto).getPreciogeneral();
			}else
			{
				//En el caso contrario el precio será el de la excepción de precio
				valorUnitario = valorExcepcion;
			}
			
			//Deberemos revisar si la especialidad en cuestión tiene un sobrecosto
			double valorAdicional = 0;
			double valorAdicional2 = 0;
			
			//Recordar que los valores adicionales se dan si no estan dentro de promociones
			if(idExcepcion == 0)
			{
				if(idEspecialidad > 0  && idEspecialidad2 == 0)
				{
					valorAdicional = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto);
				}else if(idEspecialidad > 0 && idEspecialidad2 > 0)
				{
					valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto))/2;
					valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto))/2;
				}	
				valorUnitario = valorUnitario + valorAdicional + valorAdicional2;
			}
			//EN este punto vamos a realizar una diferenciación de PROMODOS, dado que tendremos que hacer dos inserciones
			if((sku.contains("PROMODOS"))||(sku.contains("PROMOPIZ2X1")))
			{
				DetallePedido detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad,0,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, strModCon, "");
				idDetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
				detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad2,0,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, 0, idExcepcion, strModCon, "");
				idDetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
			}else
			{
				DetallePedido detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad,idEspecialidad2,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, strModCon, "");
				idDetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
			}
			//Revisaremos si hay productos incluidos para agregar
			for(int j = 0; j < productosIncluidos.size(); j++)
			{
				ProductoIncluido proIncTemp = productosIncluidos.get(j);
				if(proIncTemp.getIdproductopadre() == idProducto)
				{
					//Realizamos una modificación para que la cantidad a incluir se debe multiplicar por la cantidad de productos adicionados
					DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad()*cantidad,0,0,0,0, "" , "Producto Incluido-"+idDetallepedido, 0, 0 /*idexcepcion*/, "", "");
					PedidoDAO.InsertarDetallePedido(detPedidoInc);
				}
			}
			//Posteriormente realizamos la inserción de las adiciones
			for(int j = 0; j < adiciones.size(); j++)
			{
				AdicionTiendaVirtual adiTempIns = adiciones.get(j);
				//Creamos el objeto DetalleAdicion y hacemos la inserción
				//Validamos si es una sola mitad
				if(idEspecialidad  >0 && idEspecialidad2 == 0) 
				{
					DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), idEspecialidad, 0, 0.5 , 0);
					PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
					detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), 0, idEspecialidad, 0 , 0.5);
					PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
				}else
				{
					//Este sería el caso de que es mitad y mitad
					DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), idEspecialidad, 0, 0.5 , 0);
					PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
					detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), 0, idEspecialidad2, 0 , 0.5);
					PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
				}	
			}
			//Posteriormente realizamos la inserción de los modificadores
			for(int j = 0; j < modificadoresCon.size(); j++)
			{
				AdicionTiendaVirtual modTempIns = modificadoresCon.get(j);
				ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetallepedido, modTempIns.getIdProductoAdicion(), 0, modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
				PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);	
			}
			//Agregamos el domicilio si es el caso que hay que agregar y verificamos
			//que no hayamos insertado el domicilio antes
			if(idProductoDomicilio > 0 && !cobradoDomicilio)
			{
				DetallePedido detPedidoDomi = new DetallePedido(idProductoDomicilio,idPedido,1,0,0,1500,1500*1, "" , "" /*observacion*/, 0, 0, "", "");
				PedidoDAO.InsertarDetallePedido(detPedidoDomi);
				//Prendemos el indicador de que se cobro el domicilio
				cobradoDomicilio = true;
			}
		}
		//Realizamos ahora el retorno del id del pedido que ya es el identificador único del pedido en el sistema
		//de contact center.
		return(idPedido);
	}
	
	public int ingresarPedidoTiendaVirtualKuno(JSONArray producto, int idTienda, int idCliente, String fechaPedido, long idOrdenComercio, int idTipoPedido, ArrayList<Double> cupones, String origenPedido, String fuentePedido)
	{
		ParametrosCtrl parCtrl = new ParametrosCtrl();
		int idPedido = 0;
		
		// 1. Información de los domicilios
		int idProductoDomicilio = 0;
		long valorDomicilio = 0;
		boolean cobradoDomicilio = false;
		// 2. Traemos los productos incluidos para validación
		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();
		//IdPedido valor a devolver con la creación del pedido
		String usuarioPedido = "";
		if(fuentePedido.contains("web"))
		{
			usuarioPedido = "TIENDA VIRTUAL";
		}else
		{
			usuarioPedido = "APP";
		}
		//3. Realizaremos la inserción del ENCABEZADO PEDIDO
		idPedido = PedidoDAO.InsertarEncabezadoPedidoTiendaVirtualKuno(idTienda, idCliente, fechaPedido, usuarioPedido, idOrdenComercio, idTipoPedido, origenPedido, fuentePedido);
		//Realizamos el recorrido de uno a uno los detalles del pedido
		//Tendremos un valor para el idPedido que crearemos una vez
		long valorTotalItemJSON = 0;
		//Definimos las variables que nos serviran para obtener el código de producto y las especialidades
		int idProducto = 0;
		//Variable donde almacenaremos el idAdicion
		int idProductoAdicion = 0;
		//Variable para almacenar el tipo de liquido
		int idSaborTipoLiquido = 0;
		//Variable para almacenar el producto condimento
		int idProductoCond = 0;
		//Variable para almacenar el producto con
		int idProductoCon = 0;
		//Variable donde almacenamos el String de las adiciones
		String strAdiciones = "";
		String strAdicionesSegunda = "";
		//Variable donde almacenamos el String de modificador con
		String strModCon = "";
		String strModConSegunda = "";
		int idEspecialidad = 0;
		//Agregamos la especialidad2 en caso de ser mitad y mitad
		int idEspecialidad2 = 0;
		//Tendremos una homologación del excepción de precio
		int idExcepcion = 0;
		double valorExcepcion = 0;
		String controlaEspecialidadExc = "N";
		//Tendremos dos variables para controlar cual mitad es la que estamos procesando
		boolean mitad1, mitad2;
		boolean sel1, sel2;
		double valorUnitario = 0;
		double valorUnitario2 = 0;
		//Variable donde extraeremos la cantidad de lo que estamos pidiendo
		double cantidad = 0;
		String sku = "";
		//Variable del detalle Pedido insertado
		int idDetInser = 0;
		//Definimos el idDetallePadre 
		int idDetallepedido = 0;
		int idDetPedido1 = 0;
		int idDetPedido2 = 0;
		//Esta variable es para el tema del tamaño
		String tamanoPizza = "";
		//Variable que indicará si es promoción y la cantidad del producto se deberá tomar de allí
		//por ejemplo si es una promoción del 20%
		boolean esPromocion = false;
		boolean indPrimerCon = false;
		boolean indSegundoCon = false;
		for(int i = 0; i < producto.size(); i++) 
		{
			// 1. Extraemos uno a uno los items generales del pedido
			JSONObject productoTemp = (JSONObject) producto.get(i);
			
			strAdiciones = "";
			strAdicionesSegunda = "";
			strModCon = "";
			strModConSegunda = "";
			//Iniciamos la variable de control de mitades
			mitad1 = true;
			mitad2 = false;
			sel1 = true;
			sel2 = false;
			idProductoDomicilio = 0;
			tamanoPizza = "";
			//Tendremos un arreglo con las adiciones para este line_items
			ArrayList<AdicionTiendaVirtual> adiciones = new ArrayList();
			//Tendremos un arreglo con los condimentos
			ArrayList<AdicionTiendaVirtual> modificadoresCon = new ArrayList();
			//Creamos objeto de AdicionTiendaVirtual temporal
			AdicionTiendaVirtual adiTemp = new AdicionTiendaVirtual();
			//Extraemos uno a uno los items del pedido

			//Hacemos una validacion específica si es el caso de un descuento de acompañante gratis
			String tipo = (String)productoTemp.get("type");
			String cupon = "";
			Double descuento = 0.0;
			if(tipo.equals(new String("promo_item")))
			{
				try
				{
					cupon = (String)productoTemp.get("coupon");
					long tmp = (long)productoTemp.get("item_discount");
					Long longDesc = Long.valueOf(tmp);
					descuento = longDesc.doubleValue();
				}catch(Exception e)
				{
					cupon = "";
					descuento = 0.0;
					
				}
				Double tmpDouble = Double.valueOf(descuento);
				cupones.add(tmpDouble);
				if(cupon.equals(new String ("FESTIVAL"))||cupon.equals(new String ("40000GRACIAS"))||cupon.equals(new String ("DIADEPIZZA20")))
				{
					continue;
				}
			}
			
			//Extraremos el total del item
			valorTotalItemJSON = (long)productoTemp.get("total_item_price");
			if(esPromocion)
			{
				//Conservaremos la cantidad como venía en la anterior interacción y apagaremos la variable
				esPromocion = false;
			}else
			{
				//Extreamos la cantidad
				Long tmp = (long)productoTemp.get("quantity");
				Long lngCantidad = Long.valueOf(tmp);
				cantidad = lngCantidad.doubleValue();
			}
			//Extraemos el código SKU que nos dará pistas de lo que tiene el item
			sku = (String) productoTemp.get("name");
			
			//Validamos si es un SKU de promoción
			esPromocion = parCtrl.esPromocionTiendaVirtual(sku);
			ExcepcionPrecio excepcionPrecioTemp = parCtrl.homologarExcepcionTiendaVirtual(sku);
			//Si lo es iremos al siguiente item de iteración dado que solo nos interesará la cantidad
			if(esPromocion)
			{
				continue;
			}
			
			//Incluiremos una lógica para procesar en caso de que sea domicilio
			if(sku.equals(new String("DELIVERY_FEE")))
			{
				idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual("Valor del domicilio");
				valorDomicilio = (Long) productoTemp.get("price");
				DetallePedido detPedidoDomi = new DetallePedido(idProductoDomicilio,idPedido,1,0,0,valorDomicilio,valorDomicilio*1, "" , "" /*observacion*/, 0, 0, "", "");
				PedidoDAO.InsertarDetallePedido(detPedidoDomi);
				idProductoDomicilio = 0;
				valorDomicilio = 0;
			}
			
			//Extraeremos el valor del tamano de la pizza
			JSONArray metaDataDetalle = (JSONArray) productoTemp.get("options");
			//Realizamos un recorrido para tomar el tamaño si existe
			for(int j = 0; j < metaDataDetalle.size();j++)
			{
				JSONObject metaDataTemp = (JSONObject) metaDataDetalle.get(j);
				if(((String)metaDataTemp.get("group_name")).trim().equals(new String("Tamaño"))||((String)metaDataTemp.get("group_name")).trim().equals(new String("Size")))
				{
					tamanoPizza = (String)metaDataTemp.get("name");
					break;
				}
			}
			
			//Valores inicializados para trabajar
			idProducto = 0;
			idEspecialidad = 0;
			idEspecialidad2 = 0;
			idExcepcion = 0;
			valorExcepcion = 0;
			controlaEspecialidadExc = "N";
			indPrimerCon = false;
			indSegundoCon = false;
			//En caso de que tamanoPizza tenga un valor diferente a vacío significa que fue un producto del menú
			if(tamanoPizza.length() > 0 && excepcionPrecioTemp.getIdExcepcion() == 0 )
			{
				idProducto = parCtrl.homologarProductoTiendaVirtual(tamanoPizza);
				idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(sku);
				//Tendremos una marcación especial cuando la pizza es mitad y mitad
				if(idEspecialidad == -1)
				{
					String strmitad1 = ""; 
					String strmitad2 = "";
					for(int j = 0; j < metaDataDetalle.size();j++)
					{
						JSONObject metaDataTemp = (JSONObject) metaDataDetalle.get(j);
						if(((String)metaDataTemp.get("group_name")).trim().equals(new String("Elige uno o dos sabor de tu pizza")))
						{
							if(strmitad1.equals(new String("")))
							{
								strmitad1 = sku + " " + (String)metaDataTemp.get("name");
								idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(strmitad1);
							}else if(strmitad2.equals(new String("")))
							{
								strmitad2 = sku + " " + (String)metaDataTemp.get("name");
								idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual(strmitad2);
								break;
							}
						}
					}
					//Quiere decir que la pizza es mitad y mitad.
				}
			}else //Este será el caso o de una promoción o de un producto incluido
			{
				//Intentaremos primero como un producto acompañante
				idProducto = parCtrl.homologarProductoTiendaVirtual(sku);
				//En caso de que el idProducto sea cero es porque no encontró homologación.
				if(idProducto == 0)
				{
					ExcepcionPrecio excepcion = parCtrl.homologarExcepcionTiendaVirtual(sku);
					//Recuperamos el idExcepción y el idProducto
					idExcepcion = excepcion.getIdExcepcion();
					idProducto = excepcion.getIdProducto();
					valorExcepcion = excepcion.getPrecio();
					controlaEspecialidadExc = excepcion.getControlaEspecialidades();
					if(idExcepcion > 0)
					{
						sku = sku + " " + "PROMO";
					}
				}
			}
			
			//La idea es procesar los adicionales de la pizza que estarían dentro de los detalles y estos serían
			//las adiciones, los condimentos y la gaseosa.
			String keyDetPedido = "";
			String valueDetPedido = "";
			idSaborTipoLiquido = 0;
			for(int j = 0; j < metaDataDetalle.size(); j++)
			{
				//Extraemos cada una de las meta data del detalle del pedido
				JSONObject metaDataTemp = (JSONObject) metaDataDetalle.get(j);
				//Clareamos las variables para llevar las variables
				idProductoAdicion = 0;
				idProductoCond = 0;
				idProductoCon = 0;
				keyDetPedido = (String) metaDataTemp.get("group_name");
				valueDetPedido = (String) metaDataTemp.get("name");
				//Ya comenzamos a tratar por cada uno, validamos si es tamaño en cuyo caso
				//Validaremos si es tamaño, si es gaseosa o si es adicion
				//Realizaremos un replace con el fin de evitar para las pizzas un solo ingrediente realizar una homologación
				keyDetPedido = keyDetPedido.replace("Elige 1 ingrediente Primera Pizza", "Elige hasta 3 ingredientes");
				keyDetPedido = keyDetPedido.replace("Elige 1 ingrediente Segunda Pizza", "Elige hasta 3 ingredientes");
				keyDetPedido = keyDetPedido.replace("Elige 1 ingrediente Segunda Mitad", "Elige hasta 3 ingredientes");
				keyDetPedido = keyDetPedido.replace("Elige 1 ingrediente Primera Mitad", "Elige hasta 3 ingredientes");
				keyDetPedido = keyDetPedido.replace("Elige 1 ingrediente", "Elige hasta 3 ingredientes");
				keyDetPedido = keyDetPedido.replace("Elige hasta 2 ingredientes", "Elige hasta 3 ingredientes");
				if(keyDetPedido.contains("Adicionar") || keyDetPedido.contains("bebida") || keyDetPedido.contains("Condimentos") || keyDetPedido.contains("Mitad y Mitad") || keyDetPedido.contains("Elige hasta 3 ingredientes") || keyDetPedido.contains("Elige la especialidad") || keyDetPedido.contains("Producto Adicional") || keyDetPedido.contains("Elige uno o dos sabores para tu promoción") || keyDetPedido.contains("Envío (obligatorio)") || keyDetPedido.contains("Selecciona la especialidad 1") || keyDetPedido.contains("Selecciona la especialidad 2"))
				{

					keyDetPedido = keyDetPedido + " " + valueDetPedido;
					//Con el Key formado validamos el tipo de key que tenemos
					if(keyDetPedido.contains("Adicionar"))
					{
						//Agregamos tomar la cantidad de la adición dado que puede ser diferente de 1
						double cantidadAdicion = 0;
						long tmp = (long)metaDataTemp.get("quantity");
						Long lngCantidadAdi = Long.valueOf(tmp);
						cantidadAdicion = lngCantidadAdi.doubleValue();
						//En este punto vamos a multiplicar las adiciones para hacerlo de la manera correcta
						cantidadAdicion = cantidadAdicion * cantidad;
						
						//En este punto realizaremos un tema comportamiento diferencial para la promoción SUPER COMBO MEDIANAS
						if(sku.contains("Super Combo Medianas"))
						{
							int posicionPizza = 1;
							String keyDetPedidoTemp = "";
							if(indPrimerCon && !indSegundoCon)
							{
								keyDetPedidoTemp = keyDetPedido.replaceFirst("Adicionar a tu Mediana 1", "Adicionar a tu Mediana");
							}else if(indPrimerCon && indSegundoCon)
							{
								keyDetPedidoTemp = keyDetPedido.replaceFirst("Adicionar a tu Mediana 2", "Adicionar a tu Mediana");
							}
							idProductoAdicion = parCtrl.homologarProductoTiendaVirtual(keyDetPedidoTemp);
							if(indPrimerCon && !indSegundoCon)
							{
								strAdiciones = strAdiciones + " " + idProductoAdicion + "-" + keyDetPedido;
								posicionPizza = 1;
							}else if(indPrimerCon && indSegundoCon)
							{
								strAdicionesSegunda = strAdicionesSegunda + " " + idProductoAdicion + "-" + keyDetPedido;
								posicionPizza = 2;
							}
							valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
							DetallePedido detPedidoAdicion = new DetallePedido(idProductoAdicion,idPedido,cantidadAdicion,0,0,valorUnitario,valorUnitario*cantidadAdicion, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/, "", "");
							idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoAdicion);
							adiTemp = new AdicionTiendaVirtual();
							adiTemp.setCantidad(cantidadAdicion);
							adiTemp.setIdProductoAdicion(idProductoAdicion);
							adiTemp.setIdDetallePedido(idDetInser);
							adiTemp.setPosicionPizza(posicionPizza);
							adiciones.add(adiTemp);
						}else
						{
							idProductoAdicion = parCtrl.homologarProductoTiendaVirtual(keyDetPedido);
							strAdiciones = strAdiciones + " " + idProductoAdicion + "-" + keyDetPedido;
							/**
							 * Realizamos la inserción y cobro de la adición
							 * 
							 */
							valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
							//La idea es adicionar la adición del ingrediente cuantas veces esté
							int cantAdicionCiclo = (int) cantidadAdicion;
							for(int z = 1; z <= cantAdicionCiclo; z++)
							{
								DetallePedido detPedidoAdicion = new DetallePedido(idProductoAdicion,idPedido,1,0,0,valorUnitario,valorUnitario, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/, "", "");
								idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoAdicion);
								adiTemp = new AdicionTiendaVirtual();
								adiTemp.setCantidad(1);
								adiTemp.setIdProductoAdicion(idProductoAdicion);
								adiTemp.setIdDetallePedido(idDetInser);
								adiciones.add(adiTemp);
							}	
						}	
					}else if(keyDetPedido.contains("bebida"))
					{
						if(tamanoPizza.equals(new String("")))
						{
							if(sku.contains(new String("PROMO")))
							{
								idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(keyDetPedido + " " + "PROMO");
							}else if(sku.contains(new String("LASAGNA")))
							{
								idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(keyDetPedido + " " + "Pizzeta (4 porciones)");
							}
						}else
						{
							idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(keyDetPedido + " " + tamanoPizza);
						}
					}else if(keyDetPedido.contains("Condimentos") || keyDetPedido.contains("Producto Adicional"))
					{
						idProductoCond = parCtrl.homologarProductoTiendaVirtual(keyDetPedido);
						/*
						 * Realizamos la inserción de los condimentos
						 */
						valorUnitario = ProductoDAO.retornarProducto(idProductoCond).getPreciogeneral();
						DetallePedido detPedidoCond = new DetallePedido(idProductoCond,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/, "", "");
						int idDetalleAdicional = PedidoDAO.InsertarDetallePedido(detPedidoCond);
						//Revisaremos si hay productos incluidos para agregar
						for(int z = 0; z < productosIncluidos.size(); z++)
						{
							ProductoIncluido proIncTemp = productosIncluidos.get(z);
							if(proIncTemp.getIdproductopadre() == idProductoCond)
							{
								DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad(),0,0,0,0, "" , "Producto Incluido-"+idDetalleAdicional, 0, 0 /*idexcepcion*/, "", "");
								PedidoDAO.InsertarDetallePedido(detPedidoInc);
							}
						}
						
					}else if(keyDetPedido.contains("Elige hasta 3 ingredientes"))
					{
						
						//CONTROLAMOS QUE SI NO TIENE TAMAÑO SE LO PONEMOS PERO DEBERÍA YA DE VENIR
						//Le agregamos el tamaño al tema del Con + ingrediente + tamaño de Pizza para buscar la homologación
						//Colocaremos un condicional para poder manejar la promoción de 1 ingrediente que manejamos
						if(tamanoPizza.equals(new String("")))
						{
							if(sku.contains("Super Combo Medianas"))
							{
								tamanoPizza = "Mediana (6 porciones)";
							}else
							{
								// Para promoción de Grande 1 Ingrediente 19900
								tamanoPizza = "Grande (8 porciones)";
							}
							
						}
						
						//Se realiza la identificación del CON						
						keyDetPedido = keyDetPedido + " " + tamanoPizza;
						idProductoCon = parCtrl.homologarProductoTiendaVirtual(keyDetPedido);
						//Hacemos diferenciación si es una promoción o no
						if(idExcepcion > 0)
						{
							if(sku.contains("Super Combo Medianas"))
							{
								if(!indPrimerCon && !indSegundoCon)
								{
									strModCon = strModCon + " " + idProductoCon + "-" + keyDetPedido + " / ";
									indPrimerCon = true;
									//Capturamos el valor unitario asociado al ModificadorCon
									valorUnitario = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
									//2021-02-27 Comentamos siguiente linea pues no tiene sentido si estamos hablando es del idproductoCon
									//valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
									idDetInser = 0;
									//Controlamos si el valor Unitario es mayor a cero hacemos la inserción del detalle pedido
									if(valorUnitario > 0)
									{
										DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
										idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
									}
									adiTemp = new AdicionTiendaVirtual();
									adiTemp.setCantidad(1);
									adiTemp.setIdProductoAdicion(idProductoCon);
									adiTemp.setIdDetallePedido(idDetInser);
									adiTemp.setPosicionPizza(1);
									modificadoresCon.add(adiTemp);
								}else if(indPrimerCon && !indSegundoCon)
								{
									strModConSegunda = strModConSegunda + " " + idProductoCon + "-" + keyDetPedido + " / ";
									indSegundoCon = true;
									//Capturamos el valor unitario asociado al ModificadorCon
									valorUnitario = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
									//2021-02-27 Comentamos siguiente linea pues no tiene sentido si estamos hablando es del idproductoCon
									//valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
									idDetInser = 0;
									//Controlamos si el valor Unitario es mayor a cero hacemos la inserción del detalle pedido
									if(valorUnitario > 0)
									{
										DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
										idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
									}
									adiTemp = new AdicionTiendaVirtual();
									adiTemp.setCantidad(1);
									adiTemp.setIdProductoAdicion(idProductoCon);
									adiTemp.setIdDetallePedido(idDetInser);
									adiTemp.setPosicionPizza(2);
									modificadoresCon.add(adiTemp);
								}
							}else
							{
								if(!indPrimerCon && !indSegundoCon)
								{
									strModCon = strModCon + " " + idProductoCon + "-" + keyDetPedido + " / ";
									indPrimerCon = true;
									//Capturamos el valor unitario asociado al ModificadorCon
									valorUnitario = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
									//2021-02-27 Comentamos siguiente linea pues no tiene sentido si estamos hablando es del idproductoCon
									//valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
									idDetInser = 0;
									//Controlamos si el valor Unitario es mayor a cero hacemos la inserción del detalle pedido
									if(valorUnitario > 0)
									{
										DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
										idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
									}
									adiTemp = new AdicionTiendaVirtual();
									adiTemp.setCantidad(1);
									adiTemp.setIdProductoAdicion(idProductoCon);
									adiTemp.setIdDetallePedido(idDetInser);
									adiTemp.setPosicionPizza(1);
									modificadoresCon.add(adiTemp);
								}else if(indPrimerCon && !indSegundoCon)
								{
									strModCon = strModCon + " " + idProductoCon + "-" + keyDetPedido;
									indSegundoCon = true;
									//Capturamos el valor unitario asociado al ModificadorCon
									valorUnitario = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
									//2021-02-27 Comentamos siguiente linea pues no tiene sentido si estamos hablando es del idproductoCon
									//valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
									idDetInser = 0;
									//Controlamos si el valor Unitario es mayor a cero hacemos la inserción del detalle pedido
									if(valorUnitario > 0)
									{
										DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
										idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
									}
									adiTemp = new AdicionTiendaVirtual();
									adiTemp.setCantidad(1);
									adiTemp.setIdProductoAdicion(idProductoCon);
									adiTemp.setIdDetallePedido(idDetInser);
									adiTemp.setPosicionPizza(2);
									modificadoresCon.add(adiTemp);
								}
							}
						}else if(idExcepcion == 0)
						{
							//Aqui debería venir la lógica cuando la pizza es ARMA TU PIZZA
							strModCon = strModCon + " " + idProductoCon + "-" + keyDetPedido;
							//Capturamos el valor unitario asociado al ModificadorCon
							valorUnitario = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
							//2021-02-27 Comentamos siguiente linea pues no tiene sentido si estamos hablando es del idproductoCon
							//valorUnitario = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
							idDetInser = 0;
							//Controlamos si el valor Unitario es mayor a cero hacemos la inserción del detalle pedido
							if(valorUnitario > 0)
							{
								DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitario,valorUnitario*cantidad, keyDetPedido , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
								idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
							}
							adiTemp = new AdicionTiendaVirtual();
							adiTemp.setCantidad(1);
							adiTemp.setIdProductoAdicion(idProductoCon);
							adiTemp.setIdDetallePedido(idDetInser);
							modificadoresCon.add(adiTemp);
						}
						
						
					}else if(keyDetPedido.contains("Elige uno o dos sabores para tu promoción"))
					{
						//Si se tiene Elige la especialidad que es la que viene en promociones vamos a hacer el cambio por mitad y mitad
						keyDetPedido = keyDetPedido.replace("Elige uno o dos sabores para tu promoción", "Mitad y Mitad");
						//Preguntamos si es la mitad1 o la mitad2, para saber cual especialidad deberemos de homologar
						if(mitad1)
						{
							idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
							mitad1 = false;
							mitad2 = true;
						}else if(mitad2)
						{
							idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
						}
					}else if(keyDetPedido.contains("Envío (obligatorio)"))
					{
						if(cantidad > 1)
						{
							cobroVariosDomicilios = true;
						}
						valorDomicilio = 2000;
						if(valorDomicilio > 0)
						{
							idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual("Valor del domicilio"); 
						}
						break;
					}else if(keyDetPedido.contains("Selecciona la especialidad 1") || keyDetPedido.contains("Selecciona la especialidad 2"))
					{
						//Si se tiene Elige la especialidad que es la que viene en promociones vamos a hacer el cambio por mitad y mitad
						keyDetPedido = keyDetPedido.replace("Selecciona la especialidad 1", "Mitad y Mitad");
						keyDetPedido = keyDetPedido.replace("Selecciona la especialidad 2", "Mitad y Mitad");
						//Preguntamos si es la mitad1 o la mitad2, para saber cual especialidad deberemos de homologar
						if(sel1)
						{
							idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
							sel1 = false;
							sel2 = true;
						}else if(sel2)
						{
							idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual(keyDetPedido);
						}
					}
				}
				
			}
			//Luego de procesar la meta_data 
			//En este punto comenzamos a realizar la inserción del detalle del pedido
			//Validamos en caso de que no sea excepción es el precio full
			if (idExcepcion == 0)
			{
				valorUnitario = ProductoDAO.retornarProducto(idProducto).getPreciogeneral();
			}else
			{
				//En el caso contrario el precio será el de la excepción de precio
				valorUnitario = valorExcepcion;
				valorUnitario2 = valorExcepcion;
			}
			
			//Deberemos revisar si la especialidad en cuestión tiene un sobrecosto
			double valorAdicional = 0;
			double valorAdicional2 = 0;
			
			//Recordar que los valores adicionales se dan si no estan dentro de promociones
			if(idExcepcion == 0)
			{
				if(idEspecialidad > 0  && idEspecialidad2 == 0)
				{
					valorAdicional = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto);
				}else if(idEspecialidad > 0 && idEspecialidad2 > 0)
				{
					valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto))/2;
					valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto))/2;
				}	
				valorUnitario = valorUnitario + valorAdicional + valorAdicional2;
			}else if(idExcepcion > 0)
			{
				if(idEspecialidad > 0  && idEspecialidad2 == 0)
				{
					valorAdicional = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto);
					valorUnitario = valorUnitario + valorAdicional;
				}else if(idEspecialidad > 0 && idEspecialidad2 > 0)
				{
					if((sku.contains("Combo 2 Medianas"))||(sku.contains("Pizzeta 2 x 1")))
					{
						valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto));
						valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto));
						valorUnitario = valorUnitario + valorAdicional;
						valorUnitario2 = valorUnitario2 + valorAdicional2;
					}else
					{
						valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto))/2;
						valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto))/2;
						valorUnitario = valorUnitario + valorAdicional + valorAdicional2;
					}	
				}	
				
			}
			//EN este punto vamos a realizar una diferenciación de PROMODOS, dado que tendremos que hacer dos inserciones
			if((sku.contains("Combo 2 Medianas"))||(sku.contains("Pizzeta 2 x 1")))
			{
				if(sku.contains("Combo 2 Medianas"))
				{
					DetallePedido detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad,0,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, strModCon, "");
					idDetPedido1 = PedidoDAO.InsertarDetallePedido(detPedido);
					detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad2,0,valorUnitario2,valorUnitario2*cantidad, strAdicionesSegunda , "" /*observacion*/, 0, idExcepcion, strModConSegunda, "");
					idDetPedido2 = PedidoDAO.InsertarDetallePedido(detPedido);
				}else if((sku.contains("Pizzeta 2 x 1")))
				{
					DetallePedido detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad,0,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, strModCon, "");
					idDetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
					detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad2,0,valorUnitario2,valorUnitario2*cantidad, strAdiciones , "" /*observacion*/, 0, idExcepcion, strModCon, "");
					idDetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
				}
			}else
			{
				DetallePedido detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad,idEspecialidad2,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, strModCon, "");
				idDetallepedido = PedidoDAO.InsertarDetallePedido(detPedido);
			}
			//Revisaremos si hay productos incluidos para agregar
			for(int j = 0; j < productosIncluidos.size(); j++)
			{
				ProductoIncluido proIncTemp = productosIncluidos.get(j);
				if(proIncTemp.getIdproductopadre() == idProducto)
				{
					//Realizamos una modificación para que la cantidad a incluir se debe multiplicar por la cantidad de productos adicionados
					DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad()*cantidad,0,0,0,0, "" , "Producto Incluido-"+idDetallepedido, 0, 0 /*idexcepcion*/, "", "");
					PedidoDAO.InsertarDetallePedido(detPedidoInc);
				}
			}
			//Posteriormente realizamos la inserción de los modificadores
			for(int j = 0; j < modificadoresCon.size(); j++)
			{
				if((indPrimerCon && indSegundoCon) || (indPrimerCon && !indSegundoCon))
				{
					AdicionTiendaVirtual modTempIns = modificadoresCon.get(j);
					if(idDetPedido1 != 0 && idDetPedido2 != 0)
					{
						if(modTempIns.getPosicionPizza() == 1)
						{
							ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetPedido1, modTempIns.getIdProductoAdicion(), 0, modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
							PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);
						}else if(modTempIns.getPosicionPizza() == 2)
						{
							ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetPedido2, modTempIns.getIdProductoAdicion(), 0, modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
							PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);
						}
					}else
					{
						if(modTempIns.getPosicionPizza() == 1)
						{
							ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetallepedido, modTempIns.getIdProductoAdicion(), 0, modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
							PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);
						}else if(modTempIns.getPosicionPizza() == 2)
						{
							ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetallepedido, 0, modTempIns.getIdProductoAdicion(), modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
							PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);
						}
					}
					
				}else
				{
					AdicionTiendaVirtual modTempIns = modificadoresCon.get(j);
					ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetallepedido, modTempIns.getIdProductoAdicion(), 0, modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
					PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);	
				}
				
			}
			//Posteriormente realizamos la inserción de las adiciones
			for(int j = 0; j < adiciones.size(); j++)
			{
				AdicionTiendaVirtual adiTempIns = adiciones.get(j);
				if((indPrimerCon && indSegundoCon) ||(indPrimerCon && !indSegundoCon))
				{
					if(idDetPedido1 != 0 && idDetPedido2 != 0)
					{
						if(adiTempIns.getPosicionPizza() == 1)
						{
							DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetPedido1, adiTempIns.getIdDetallePedido(), 0, 0, 1 , 0);
							PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
						}else if(adiTempIns.getPosicionPizza() == 2)
						{
							DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetPedido2, adiTempIns.getIdDetallePedido(), 0, 0, 1 , 0);
							PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
						}
					}else
					{
						DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), 0, 0, 1 , 0);
						PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
					}
					
				}
				else
				{
					//Creamos el objeto DetalleAdicion y hacemos la inserción
					//Validamos si es una sola mitad
					if(idEspecialidad  >0 && idEspecialidad2 == 0) 
					{
						DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), idEspecialidad, 0, 0.5 , 0);
						PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
						detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), 0, idEspecialidad, 0 , 0.5);
						PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
					}else if(idEspecialidad  >0 && idEspecialidad2 > 0)
					{
						//Este sería el caso de que es mitad y mitad
						DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), idEspecialidad, 0, 0.5 , 0);
						PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
						detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), 0, idEspecialidad2, 0 , 0.5);
						PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
					}else if(idEspecialidad  == 0 && idEspecialidad2 == 0)
					{
						DetallePedidoAdicion detPedidoAdicion = new DetallePedidoAdicion(idDetallepedido, adiTempIns.getIdDetallePedido(), 0, 0, 1 , 0);
						PedidoDAO.InsertarDetalleAdicion(detPedidoAdicion);
					}
				}	
			}
			//Agregamos el domicilio si es el caso que hay que agregar y verificamos
			//que no hayamos insertado el domicilio antes
			if(idProductoDomicilio > 0 && !cobradoDomicilio)
			{
				DetallePedido detPedidoDomi = new DetallePedido(idProductoDomicilio,idPedido,1,0,0,valorDomicilio,valorDomicilio*1, "" , "" /*observacion*/, 0, 0, "", "");
				PedidoDAO.InsertarDetallePedido(detPedidoDomi);
				//Prendemos el indicador de que se cobro el domicilio
				cobradoDomicilio = true;
			}else if(idProductoDomicilio > 0 && cobradoDomicilio)
			{
				cobroVariosDomicilios = true;
			}
		}
		return(idPedido);
	}
	
	//Método para la generación y envío de link de pago con tienda virtual
	/**
	 * Método que se encargará de la creación del link de pago y del envío de la notificaciókn al correo y mensaje para notificar al cliente
	 * @param idPedidoTienda
	 * @param clienteVirtual
	 * @param totalPedido
	 * @param idTienda
	 */
	public void verificarEnvioLinkPagos(int idPedidoTienda, Cliente clienteVirtual, double totalPedido, int idTienda)
	{
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		//Se debe hacer la creación del link y la inserción en la tabla
		//Obtenemos la tienda
		Tienda tienda = TiendaDAO.retornarTienda(idTienda);
		//Creamos la fecha Actual
		Date dateFecha = new Date();
		Calendar calendarioActual = Calendar.getInstance();
		try
		{
			calendarioActual.add(Calendar.DAY_OF_YEAR, 1);
		}catch(Exception e)
		{
			
		}
		dateFecha = calendarioActual.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String strFechaExt = dateFormat.format(dateFecha);
		//Creamos el JSON para consumir el servicio
		String jsonLinkPago = '{' +
                "\"amount_in_cents\":"  + (int)totalPedido*100 +","+
                "\"currency\": \"COP\"," + 
                "\"taxes\": [" +
                 "      {" +
                            "\"type\": \"CONSUMPTION\", "+
                            "\"amount_in_cents\":" + (int)((totalPedido*100)*0.08/1.08) + 
                          "}" +
                        "]," +
                "\"name\": \"Pizza Americana"  + " " + tienda.getNombreTienda() + "\" ," +
                "\"description\": \"Pedido #" + idPedidoTienda + "\","+
                "\"expires_at\": \"" + strFechaExt  + "T23:00:00.000Z\","+
                "\"redirect_url\": \"https://pizzaamericana.co\","+
                "\"single_use\": false,"+
                "\"sku\": \"" + idPedidoTienda + "\","+
                "\"collect_shipping\": false"+
              "}";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURLWOMPI = "https://production.wompi.co/v1/payment_links";
		HttpPost request = new HttpPost(rutaURLWOMPI);
		try
		{
			//Fijamos el header con el token
			request.setHeader("Authorization", "Bearer " + "prv_prod_Qdb2HcV6AkbkvCKr9UWbhFs6L73IFCkT");
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			//Fijamos los parámetros
			//pass the json string request in the entity
		    HttpEntity entity = new ByteArrayEntity(jsonLinkPago.getBytes("UTF-8"));
		    request.setEntity(entity);
			//request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			String datosJSON = retorno.toString();
			
			//Los datos vienen en un arreglo, debemos de tomar el primer valor como lo hacemos en la parte gráfica
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			String dataJSON = (String)jsonGeneral.get("data").toString();
			Object objParserData = parser.parse(dataJSON);
			JSONObject jsonData = (JSONObject) objParserData;
			String idLink = (String)jsonData.get("id");
			//En la parte de arriba ya tenemos la generación del link la idea en este punto es realizar
			
			//reutilización de la lógica del resto para el envío de la notificación
			realizarNotificacionWompi(idLink, clienteVirtual.getIdcliente(), "https://checkout.wompi.co/l/"+idLink, 4, idPedidoTienda);
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
	}
	
	
	public void verificarEnvioLinkPagosProcesoWompi(int idPedidoTienda, int idCliente, double totalPedido, int idTienda)
	{
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		//Se debe hacer la creación del link y la inserción en la tabla
		//Obtenemos la tienda
		Tienda tienda = TiendaDAO.retornarTienda(idTienda);
		//Creamos la fecha Actual
		Date dateFecha = new Date();
		Calendar calendarioActual = Calendar.getInstance();
		try
		{
			calendarioActual.add(Calendar.DAY_OF_YEAR, 1);
		}catch(Exception e)
		{
			
		}
		dateFecha = calendarioActual.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String strFechaExt = dateFormat.format(dateFecha);
		//Creamos el JSON para consumir el servicio
		String jsonLinkPago = '{' +
                "\"amount_in_cents\":"  + (int)totalPedido*100 +","+
                "\"currency\": \"COP\"," + 
                "\"currency\": \"COP\"," + 
                "\"taxes\": [" +
                 "      {" +
                            "\"type\": \"CONSUMPTION\", "+
                            "\"amount_in_cents\":" + (int)((totalPedido*100)*0.08/1.08) + 
                          "}" +
                        "]," +
                "\"name\": \"Pizza Americana"  + " " + tienda.getNombreTienda() + "\" ," +
                "\"description\": \"Pedido #" + idPedidoTienda + "\","+
                "\"expires_at\": \"" + strFechaExt  + "T23:00:00.000Z\","+
                "\"redirect_url\": \"https://pizzaamericana.co\","+
                "\"single_use\": false,"+
                "\"sku\": \"" + idPedidoTienda + "\","+
                "\"collect_shipping\": false"+
              "}";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURLWOMPI = "https://production.wompi.co/v1/payment_links";
		HttpPost request = new HttpPost(rutaURLWOMPI);
		try
		{
			//Fijamos el header con el token
			request.setHeader("Authorization", "Bearer " + "prv_prod_Qdb2HcV6AkbkvCKr9UWbhFs6L73IFCkT");
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			//Fijamos los parámetros
			//pass the json string request in the entity
		    HttpEntity entity = new ByteArrayEntity(jsonLinkPago.getBytes("UTF-8"));
		    request.setEntity(entity);
			//request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			String datosJSON = retorno.toString();
			
			//Los datos vienen en un arreglo, debemos de tomar el primer valor como lo hacemos en la parte gráfica
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			String dataJSON = (String)jsonGeneral.get("data").toString();
			Object objParserData = parser.parse(dataJSON);
			JSONObject jsonData = (JSONObject) objParserData;
			String idLink = (String)jsonData.get("id");
			//En la parte de arriba ya tenemos la generación del link la idea en este punto es realizar
			
			//reutilización de la lógica del resto para el envío de la notificación
			realizarNotificacionWompi(idLink, idCliente, "https://checkout.wompi.co/l/"+idLink, 4, idPedidoTienda);
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
	}
	
	public void notificarCorreoCliente(String email, String nombres, String direccion , String zona, String observacionAdicional, String telefono, long idOrdenComercio)
	{
		Correo correo = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOVIRTUAL", "CLAVECORREOVIRTUAL");
		ArrayList correos = new ArrayList();
		correo.setAsunto("HEMOS RECIBIDO TU PEDIDO #   " + idOrdenComercio);
		correos.add(email);
		correo.setContrasena(infoCorreo.getClaveCorreo());
		correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		String mensaje = obtenerStrCreacionPedido(nombres, direccion, zona, observacionAdicional, telefono, (int)idOrdenComercio);
		//correo.setMensaje(" Estimado Cliente  " + nombres + " hemos tenido el gusto de recibir tu pedido para ser entregado en " + direccion + " en la zona " + zona + " con la información adicional " + observacionAdicional + ". El teléfono de contacto que has dejado es el " + telefono + ". Por favor si alguno de estos datos es incorrecto por favor comunicate con nosotros a los números 4444553-4442525-4804660. \n Recuerda si tu pago es en linea y no has realizado el pago estaremos esperando que el pago se refleje para iniciar la elaboración de tu pedido.");
		correo.setMensaje(mensaje);
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreo();
	}
	
	public String obtenerStrCreacionPedido(String nombres, String direccion, String zona, String observacionAdicional, String telefonoContacto, int idOrdenComercio)
	{
		String htmlTemplate = "<!DOCTYPE html\n" + 
				"	PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional //EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" + 
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\"\n" + 
				"	xmlns:o=\"urn:schemas-microsoft-com:office:office\">\n" + 
				"\n" + 
				"<head>\n" + 
				"	<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" + 
				"	<meta name=\"viewport\" content=\"width=device-width\">\n" + 
				"	<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" + 
				"	<title></title>\n" + 
				"	<link\n" + 
				"		href=\"https://fonts.googleapis.com/css2?family=Montserrat:wght@100;200;300;400;500;600;700;800;900&display=swap\"\n" + 
				"		rel=\"stylesheet\" type=\"text/css\">\n" + 
				"	<style type=\"text/css\">\n" + 
				"		body {\n" + 
				"			margin: 0;\n" + 
				"			padding: 0;\n" + 
				"		}\n" + 
				"\n" + 
				"		table,\n" + 
				"		td,\n" + 
				"		tr {\n" + 
				"			vertical-align: top;\n" + 
				"			border-collapse: collapse;\n" + 
				"		}\n" + 
				"\n" + 
				"		* {\n" + 
				"			line-height: inherit;\n" + 
				"		}\n" + 
				"\n" + 
				"		a[x-apple-data-detectors=true] {\n" + 
				"			color: inherit !important;\n" + 
				"			text-decoration: none !important;\n" + 
				"		}\n" + 
				"\n" + 
				"		.tioSam {\n" + 
				"			float: left;\n" + 
				"		}\n" + 
				"\n" + 
				"		.estadoPedido {\n" + 
				"			margin-top: -10px;\n" + 
				"		}\n" + 
				"\n" + 
				"		.contDirTel {\n" + 
				"			border: 1px solid #909090;\n" + 
				"		}\n" + 
				"\n" + 
				"		.conTel {\n" + 
				"			padding-top: 100px;\n" + 
				"		}\n" + 
				"\n" + 
				"		.icoLeft {\n" + 
				"			float: left;\n" + 
				"			width: 6%;\n" + 
				"			padding: 2%;\n" + 
				"		}\n" + 
				"\n" + 
				"		.icoRight {\n" + 
				"			float: left;\n" + 
				"			width: 86%;\n" + 
				"			margin-top: 3px;\n" + 
				"			padding: 15px 0 10px 15px;\n" + 
				"			font-size: 12px;\n" + 
				"			font-family: 'Montserrat', sans-serif;\n" + 
				"			color: #555555;\n" + 
				"		}\n" + 
				"\n" + 
				"		.txtInfoLineas {\n" + 
				"			text-align: justify;\n" + 
				"		}\n" + 
				"\n" + 
				"		.contPolitica {\n" + 
				"			text-align: center;\n" + 
				"			margin-top: 10px;\n" + 
				"		}\n" + 
				"\n" + 
				"		.btnPolitica {\n" + 
				"			border: 1px solid #909090;\n" + 
				"			cursor: pointer;\n" + 
				"			font-size: 16px;\n" + 
				"			padding: 5px 10px 5px 10px;\n" + 
				"			text-decoration: none;\n" + 
				"			color: #505050;\n" + 
				"		}\n" + 
				"\n" + 
				"		.btnPolitica:hover {\n" + 
				"			background-color: hsl(0, 0%, 87%);\n" + 
				"\n" + 
				"\n" + 
				"		}\n" + 
				"\n" + 
				"		.footer {\n" + 
				"			margin-top: 40px;\n" + 
				"		}\n" + 
				"\n" + 
				"		@media only screen and (max-width: 560px) {\n" + 
				"			.btnPolitica {\n" + 
				"				font-size: 12px;\n" + 
				"			}\n" + 
				"		}\n" + 
				"\n" + 
				"		@media only screen and (max-width: 420px) {\n" + 
				"			.btnPolitica {\n" + 
				"				font-size: 10px;\n" + 
				"			}\n" + 
				"		}\n" + 
				"\n" + 
				"		@media only screen and (max-width: 360px) {\n" + 
				"			.btnPolitica {\n" + 
				"				font-size: 8px;\n" + 
				"			}\n" + 
				"		}\n" + 
				"	</style>\n" + 
				"	<style type=\"text/css\" id=\"media-query\">\n" + 
				"		@media (max-width: 620px) {\n" + 
				"\n" + 
				"			.block-grid,\n" + 
				"			.col {\n" + 
				"				min-width: 320px !important;\n" + 
				"				max-width: 100% !important;\n" + 
				"				display: block !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.block-grid {\n" + 
				"				width: 100% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.col {\n" + 
				"				width: 100% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.col>div {\n" + 
				"				margin: 0 auto;\n" + 
				"			}\n" + 
				"\n" + 
				"			img.fullwidth,\n" + 
				"			img.fullwidthOnMobile {\n" + 
				"				max-width: 100% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col {\n" + 
				"				min-width: 0 !important;\n" + 
				"				display: table-cell !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack.two-up .col {\n" + 
				"				width: 50% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col.num4 {\n" + 
				"				width: 33% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col.num8 {\n" + 
				"				width: 66% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col.num4 {\n" + 
				"				width: 33% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col.num3 {\n" + 
				"				width: 25% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col.num6 {\n" + 
				"				width: 50% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.no-stack .col.num9 {\n" + 
				"				width: 75% !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.video-block {\n" + 
				"				max-width: none !important;\n" + 
				"			}\n" + 
				"\n" + 
				"			.mobile_hide {\n" + 
				"				min-height: 0px;\n" + 
				"				max-height: 0px;\n" + 
				"				max-width: 0px;\n" + 
				"				display: none;\n" + 
				"				overflow: hidden;\n" + 
				"				font-size: 0px;\n" + 
				"			}\n" + 
				"\n" + 
				"			.desktop_hide {\n" + 
				"				display: block !important;\n" + 
				"				max-height: none !important;\n" + 
				"			}\n" + 
				"		}\n" + 
				"	</style>\n" + 
				"</head>\n" + 
				"\n" + 
				"<body class=\"clean-body\" style=\"margin: 0; padding: 0; -webkit-text-size-adjust: 100%; background-color: #ffffff;\">\n" + 
				"	<table class=\"nl-container\"\n" + 
				"		style=\"table-layout: fixed; vertical-align: top; min-width: 320px; Margin: 0 auto; border-spacing: 0; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #ffffff; width: 100%;\"\n" + 
				"		cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" width=\"100%\" bgcolor=\"#ffffff\" valign=\"top\">\n" + 
				"		<tbody>\n" + 
				"			<tr style=\"vertical-align: top;\" valign=\"top\">\n" + 
				"				<td style=\"word-break: break-word; vertical-align: top;\" valign=\"top\">\n" + 
				"\n" + 
				"					<!--Header-->\n" + 
				"					<div style=\"background-color:#db2824;\">\n" + 
				"						<div class=\"block-grid \"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: transparent;\">\n" + 
				"							<div\n" + 
				"								style=\"border-collapse: collapse;display: table;width: 100%;background-color:transparent;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"min-width: 320px; max-width: 600px; display: table-cell; vertical-align: top; width: 600px;\">\n" + 
				"									<div style=\"width:100% !important;\">\n" + 
				"										<div\n" + 
				"											style=\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:10px; padding-bottom:6px; padding-right: 0px; padding-left: 0px;\">\n" + 
				"											<div class=\"img-container center fixedwidth\" align=\"center\"\n" + 
				"												style=\"padding-right: 0px;padding-left: 0px;\">\n" + 
				"												<a href=\"https://pizzaamericana.co/\"><img class=\"center fixedwidth\"\n" + 
				"														align=\"center\" border=\"0\"\n" + 
				"														src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-3-8.png\"\n" + 
				"														alt=\"\"\n" + 
				"														style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 180px; display: block;\"\n" + 
				"														width=\"120\"></a>\n" + 
				"											</div>\n" + 
				"										</div>\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"\n" + 
				"\n" + 
				"					<div style=\"background-color:#ffffff;\">\n" + 
				"						<div class=\"block-grid \"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: transparent;\">\n" + 
				"							<div\n" + 
				"								style=\"border-collapse: collapse;display: table;width: 100%;background-color:transparent;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"min-width: 320px; max-width: 600px; display: table-cell; vertical-align: top; width: 600px;\">\n" + 
				"									<div style=\"width:100% !important;\">\n" + 
				"										<div\n" + 
				"											style=\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\">\n" + 
				"											<table class=\"divider\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"\n" + 
				"												width=\"100%\"\n" + 
				"												style=\"table-layout: fixed; vertical-align: top; border-spacing: 0; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; min-width: 100%; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;\"\n" + 
				"												role=\"presentation\" valign=\"top\">\n" + 
				"												<tbody>\n" + 
				"													<tr style=\"vertical-align: top;\" valign=\"top\">\n" + 
				"														<td class=\"divider_inner\"\n" + 
				"															style=\"word-break: break-word; vertical-align: top; min-width: 100%; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%; padding-top: 0px; padding-right: 0px; padding-bottom: 0px; padding-left: 0px;\"\n" + 
				"															valign=\"top\">\n" + 
				"															<table class=\"divider_content\" border=\"0\" cellpadding=\"0\"\n" + 
				"																cellspacing=\"0\" width=\"100%\"\n" + 
				"																style=\"table-layout: fixed; vertical-align: top; border-spacing: 0; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-top: 0px solid transparent; height: 15px; width: 100%;\"\n" + 
				"																align=\"center\" role=\"presentation\" height=\"15\"\n" + 
				"																valign=\"top\">\n" + 
				"																<tbody>\n" + 
				"																	<tr style=\"vertical-align: top;\" valign=\"top\">\n" + 
				"																		<td style=\"word-break: break-word; vertical-align: top; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;\"\n" + 
				"																			height=\"15\" valign=\"top\"><span></span></td>\n" + 
				"																	</tr>\n" + 
				"																</tbody>\n" + 
				"															</table>\n" + 
				"														</td>\n" + 
				"													</tr>\n" + 
				"												</tbody>\n" + 
				"											</table>\n" + 
				"										</div>\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"					<!--Seccion 2-->\n" + 
				"					<div style=\"background-color:transparent;\">\n" + 
				"						<div class=\"block-grid mixed-two-up\"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: transparent;\">\n" + 
				"							<div\n" + 
				"								style=\"border-collapse: collapse;display: table;width: 100%;background-color:transparent;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"display: table-cell; vertical-align: top; min-width: 320px; max-width: 400px; width: 400px;\">\n" + 
				"									<div style=\"width:100% !important;\">\n" + 
				"										<div\n" + 
				"											style=\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:5px; padding-right: 0px; padding-left: 0px;\">\n" + 
				"											<div\n" + 
				"												style=\"color:#000000;font-family: 'Montserrat', sans-serif; line-height:1.5;padding-top:13px;padding-right:3px;padding-left:3px;\">\n" + 
				"												<div class=\"tioSam\">\n" + 
				"													<img src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-4-8.png\"\n" + 
				"														alt=\"\" title=\"Alternate text\"\n" + 
				"														style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 155px; display: block;\"\n" + 
				"														width=\"140\">\n" + 
				"												</div>\n" + 
				"												<div class=\"contSeccion2\"\n" + 
				"													style=\"text-align:center; line-height: 1.5; font-size: 12px; color: #000000; font-family: 'Montserrat', sans-serif; mso-line-height-alt: 18px;\">\n" + 
				"\n" + 
				"													<p\n" + 
				"														style=\"text-align: center; line-height: 1.5; word-break: break-word; font-size: 22px; mso-line-height-alt: 33px; margin: 0;\">\n" + 
				"													<p style=\"font-size: 30px; margin-bottom: -6px;\">\n" + 
				"														<strong>Hola,</strong></p>\n" + 
				"													</p>\n" + 
				"													<a\n" + 
				"														style=\"text-decoration:none;color:#ffffff;background-color:#1f376d;border-radius:6px;-webkit-border-radius:6px;-moz-border-radius:6px;width:auto; width:auto; \n" + 
				"														padding:10px 90px 10px 90px;font-family: 'Montserrat', sans-serif; mso-border-alt:none;word-break:keep-all; font-size: 18px;\">"+ nombres +"</a>\n" + 
				"												</div>\n" + 
				"											</div>\n" + 
				"										</div>\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"					<!--Hemos recibido tu pedido-->\n" + 
				"					<div style=\"background-color:transparent;\">\n" + 
				"						<div class=\"block-grid \"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: #ffffff;\">\n" + 
				"							<div style=\"border-collapse: collapse;display: table;width: 100%;background-color:#ffffff;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"min-width: 320px; max-width: 600px; display: table-cell; vertical-align: top; width: 600px;\">\n" + 
				"									<div style=\"width:100% !important;\">\n" + 
				"\n" + 
				"										<div\n" + 
				"											style=\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\">\n" + 
				"											<div\n" + 
				"												style=\"color:#555555;font-family: 'Montserrat', sans-serif; line-height:1.5;padding-top:0px;padding-right:40px;padding-left:15px;\">\n" + 
				"												<div class=\"estadoPedido\"\n" + 
				"													style=\"font-size: 14px; line-height: 1.5; font-family: 'Montserrat', sans-serif; color: #555555; mso-line-height-alt: 21px;\">\n" + 
				"													<p\n" + 
				"														style=\"font-size: 16px; line-height: 1.5; word-break: break-word; text-align: center; font-family: 'Montserrat', sans-serif; mso-line-height-alt: 24px; margin: 0;\">\n" + 
				"														<span style=\"font-size: 16px;\"><strong>Hemos recibido tu pedido\n" + 
				"																# " + idOrdenComercio +"</strong></span></p>\n" + 
				"												</div>\n" + 
				"											</div>\n" + 
				"\n" + 
				"											<table class=\"divider\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"\n" + 
				"												width=\"100%\"\n" + 
				"												style=\"table-layout: fixed; vertical-align: top; border-spacing: 0; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; min-width: 100%; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;\"\n" + 
				"												role=\"presentation\" valign=\"top\">\n" + 
				"												<tbody>\n" + 
				"													<tr style=\"vertical-align: top;\" valign=\"top\">\n" + 
				"														<td class=\"divider_inner\"\n" + 
				"															style=\"word-break: break-word; vertical-align: top; min-width: 100%; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%; padding-top: 10px; padding-right: 15px; padding-bottom: 10px; padding-left: 15px;\"\n" + 
				"															valign=\"top\">\n" + 
				"															<table class=\"divider_content\" border=\"0\" cellpadding=\"0\"\n" + 
				"																cellspacing=\"0\" width=\"100%\"\n" + 
				"																style=\"table-layout: fixed; vertical-align: top; border-spacing: 0; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-top: 1px solid #E9EBEB; width: 100%;\"\n" + 
				"																align=\"center\" role=\"presentation\" valign=\"top\">\n" + 
				"																<tbody>\n" + 
				"																	<tr style=\"vertical-align: top;\" valign=\"top\">\n" + 
				"																		<td style=\"word-break: break-word; vertical-align: top; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;\"\n" + 
				"																			valign=\"top\"><span></span></td>\n" + 
				"																	</tr>\n" + 
				"																</tbody>\n" + 
				"															</table>\n" + 
				"														</td>\n" + 
				"													</tr>\n" + 
				"												</tbody>\n" + 
				"											</table>\n" + 
				"\n" + 
				"										</div>\n" + 
				"\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"					<!--Contacto Direcciones y telefono-->\n" + 
				"					<div style=\"background-color:transparent;\">\n" + 
				"						<div class=\"block-grid mixed-two-up\"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: transparent;\">\n" + 
				"							<div class=\"contDirTel\"\n" + 
				"								style=\"border-collapse: collapse;display: table;width: 100%;background-color:transparent;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"display: table-cell; vertical-align: top; min-width: 320px; max-width: 400px; width: 400px;\">\n" + 
				"									<div>\n" + 
				"										<div>\n" + 
				"											<div class=\"icoLeft\">\n" + 
				"												<img class=\"center autowidth\" align=\"center\" border=\"0\"\n" + 
				"													src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-9-8.png\"\n" + 
				"													alt=\"\"\n" + 
				"													style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 17px;\"\n" + 
				"													width=\"17\">\n" + 
				"											</div>\n" + 
				"											<div class=\"icoRight\">\n" + 
				"												<p\n" + 
				"													style=\"font-size: 16px; line-height: 1.2; text-align: left; word-break: break-word; font-family: 'Montserrat', sans-serif; mso-line-height-alt: 19px; margin: 0;\">\n" + 
				"													Para ser entregado en " + direccion +  " En la zona\n" + 
				"													 " + zona +".</p>\n" + 
				"												<p\n" + 
				"													style=\"font-size: 16px; line-height: 1.2; text-align: left; word-break: break-word; font-family: 'Montserrat', sans-serif; mso-line-height-alt: 19px; margin: 0;\">\n" + 
				"													Con la información adicional " + observacionAdicional + ".</p>\n" + 
				"											</div>\n" + 
				"										</div>\n" + 
				"										<div class=\"conTel\">\n" + 
				"											<div class=\"icoLeft\">\n" + 
				"												<img class=\"center autowidth\" align=\"center\" border=\"0\"\n" + 
				"													src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-10-8.png\"\n" + 
				"													alt=\"\"\n" + 
				"													style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 17px;\"\n" + 
				"													width=\"17\">\n" + 
				"											</div>\n" + 
				"											<div class=\"icoRight\"\">\n" + 
				"												<p\n" + 
				"													style=\" font-size: 16px; line-height: 1.2; text-align: left; word-break: break-word;\n" + 
				"												font-family: 'Montserrat' , sans-serif; mso-line-height-alt: 19px;\n" + 
				"												margin: 0;\">\n" + 
				"												El teléfono que has dejado es " + telefonoContacto + ".</p>\n" + 
				"											</div>\n" + 
				"										</div>\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"					</div>\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"					<!--Informacion de lineas telefonicas-->\n" + 
				"					<div style=\"background-color:transparent;\">\n" + 
				"						<div class=\"block-grid \"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: transparent;\">\n" + 
				"							<div\n" + 
				"								style=\"border-collapse: collapse;display: table;width: 100%;background-color:transparent;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"min-width: 320px; max-width: 600px; display: table-cell; vertical-align: top; width: 600px;\">\n" + 
				"									<div style=\"width:100% !important;\">\n" + 
				"										<div\n" + 
				"											style=\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:20px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\">\n" + 
				"											<div\n" + 
				"												style=\"color:#555555;font-family: 'Montserrat', sans-serif; line-height:1.5;padding-top:0px;padding-right:0px;padding-bottom:0px;padding-left:0px;\">\n" + 
				"												<div\n" + 
				"													style=\"font-size: 14px; line-height: 1.5; font-family: 'Montserrat', sans-serif; color: #555555; mso-line-height-alt: 21px;\">\n" + 
				"													<p class=\"txtInfoLineas\"\n" + 
				"														style=\"font-size: 14px; line-height: 1.5; word-break: break-word; font-family: 'Montserrat', sans-serif; mso-line-height-alt: 21px; margin: 0;\">\n" + 
				"														Por favor si alguno de estos datos es incorrecto comunícate con\n" + 
				"														nosotros a los números 4444553 - 4442525 -4804660. Recuerda si\n" + 
				"														tu pago es en línea y no has realizado el pago estaremos\n" + 
				"														esperando que el pago se refleje para iniciar la elaboración de\n" + 
				"														tu pedido.</p>\n" + 
				"												</div>\n" + 
				"											</div>\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"											<!--Boton de politicas de tratamiento de datos-->\n" + 
				"											<div\n" + 
				"												style=\"color:#555555;font-family: 'Montserrat', sans-serif; line-height:1.5;padding-top:20px;padding-right:10px;padding-bottom:0px;padding-left:10px;\">\n" + 
				"												<div class=\"contPolitica\"\n" + 
				"													style=\"font-size: 14px; line-height: 1.5; color: #555555; font-family: 'Montserrat', sans-serif; mso-line-height-alt: 21px;\">\n" + 
				"													<a href=\"https://pizzaamericana.co/politica-de-tratamiento-de-datos\"\n" + 
				"														class=\"btnPolitica\">Conoce nuestras Políticas de tratamientos de\n" + 
				"														datos personales</a>\n" + 
				"												</div>\n" + 
				"											</div>\n" + 
				"										</div>\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"\n" + 
				"					<!--Footer-->\n" + 
				"					<div class=\"footer\" style=\"background-color:#db2824;\">\n" + 
				"						<div class=\"block-grid\"\n" + 
				"							style=\"Margin: 0 auto; min-width: 320px; max-width: 600px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; background-color: transparent;\">\n" + 
				"							<div\n" + 
				"								style=\"border-collapse: collapse;display: table;width: 100%;background-color:transparent;\">\n" + 
				"								<div class=\"col num12\"\n" + 
				"									style=\"min-width: 320px; max-width: 600px; display: table-cell; vertical-align: top; width: 600px;\">\n" + 
				"									<div style=\"width:100% !important;\">\n" + 
				"										<div\n" + 
				"											style=\"border-top:0px solid transparent; border-left:0px solid transparent; border-bottom:0px solid transparent; border-right:0px solid transparent; padding-top:20px; padding-bottom:20px; padding-right: 0px; padding-left: 0px;\">\n" + 
				"											<div\n" + 
				"												style=\"color:#555555;font-family: 'Montserrat', sans-serif; line-height:1.2;padding-top:10px;padding-right:10px;padding-bottom:10px;padding-left:10px;\">\n" + 
				"												<div\n" + 
				"													style=\"line-height: 1; font-size: 12px; font-family: 'Montserrat', sans-serif; color: #555555; mso-line-height-alt: 14px;\">\n" + 
				"													<p\n" + 
				"														style=\"font-size: 18px; line-height: 1; text-align: center; font-family: 'Montserrat', sans-serif; word-break: break-word; mso-line-height-alt: 22px; margin: 0;\">\n" + 
				"														<span style=\"font-size: 24px; color: #ffffff;\">¡Gracias por\n" + 
				"															comprar en PizzaAmericana.co!</span></p>\n" + 
				"												</div>\n" + 
				"											</div>\n" + 
				"											<div class=\"img-container center autowidth\" align=\"center\"\n" + 
				"												style=\"text-align:center; padding-right: 0px;padding-left: 0px;\">\n" + 
				"												<p>\n" + 
				"													<a href=\"https://www.facebook.com/pizzaamericana.co\">\n" + 
				"														<img class=\"center autowidth\" align=\"center\" border=\"0\"\n" + 
				"															src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-8-8.png\"\n" + 
				"															alt=\"\" title=\"\"\n" + 
				"															style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 25px;\"\n" + 
				"															width=\"26\"></a>\n" + 
				"													<a href=\"https://www.instagram.com/pizzaamericana.co/\"><img\n" + 
				"															class=\"center autowidth\" align=\"center\" border=\"0\"\n" + 
				"															src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-7-8.png\"\n" + 
				"															alt=\"\" title=\"\"\n" + 
				"															style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 27px;\"\n" + 
				"															width=\"26\"></a>\n" + 
				"													<a href=\"https://pizzaamericana.co/\"><img class=\"center autowidth\"\n" + 
				"															align=\"center\" border=\"0\"\n" + 
				"															src=\"https://pizzaamericana.co/wp-content/uploads/2020/08/Asset-5-8.png\"\n" + 
				"															alt=\"\" title=\"\"\n" + 
				"															style=\"text-decoration: none; -ms-interpolation-mode: bicubic; height: auto; border: 0; width: 100%; max-width: 26px;\"\n" + 
				"															width=\"26\"></a>\n" + 
				"													<a href=\"https://pizzaamericana.co/\"\n" + 
				"														style=\"font-family: 'Montserrat', sans-serif; color: #ffffff; font-size: 18px; text-decoration: none;\">PizzaAmericana.co</a>\n" + 
				"												</p>\n" + 
				"											</div>\n" + 
				"										</div>\n" + 
				"									</div>\n" + 
				"								</div>\n" + 
				"							</div>\n" + 
				"						</div>\n" + 
				"					</div>\n" + 
				"				</td>\n" + 
				"			</tr>\n" + 
				"		</tbody>\n" + 
				"	</table>\n" + 
				"</body>\n" + 
				"\n" + 
				"</html>";
		return(htmlTemplate);
	}
	
	
	/**
	 * Método en la capa controladora que se encarga del cambio de un pedido en cuanto al cambio de tienda
	 * @param idTienda
	 * @param idPedido
	 * @param idCliente
	 * @return
	 */
	public String realizarCambioPedido(int idTienda, int idPedido, int idCliente)
	{
		JSONObject respuesta = new JSONObject();
		boolean resultado = PedidoDAO.realizarCambioPedido(idTienda, idPedido, idCliente);
		if(resultado)
		{
			respuesta.put("resultado", "OK");
		}else
		{
			respuesta.put("resultado", "NOK");
		}
		return(respuesta.toJSONString());
	}
	
	public String actualizarFormaPago(int idPedido, int idFormaPagoNueva, double valorFormaPagoNueva, double valorTotal, boolean virtualOrigen, boolean virtualDestino)
	{
		JSONObject respuesta = new JSONObject();
		boolean resultado = PedidoDAO.actualizarFormaPago(idPedido, idFormaPagoNueva, valorFormaPagoNueva, valorTotal, virtualOrigen, virtualDestino);
		if(resultado)
		{
			respuesta.put("resultado", "OK");
		}else
		{
			respuesta.put("resultado", "NOK");
		}
		return(respuesta.toJSONString());
	}
	
	public void registrarPagoVirtualTienda(int idPedidoTienda, int idTienda, double valorPagoVirtual, double valorPedido, String idLink)
	{
		PedidoPagoVirtualConsolidadoDAO.insertarPedidoPagoVirtual(idPedidoTienda, idTienda, valorPagoVirtual, valorPedido, idLink);
	}
	
	public String obtenerDescuentoDiarioPedido(int idPedido)
	{
		JSONObject respuesta = new JSONObject();
		//Definimos la variable donde iremos almacenando el descuento
		double valorDescuento = obtenerDescuentoDiarioPedidoVal(idPedido, "C");
		respuesta.put("descuento", valorDescuento);
		respuesta.put("motivodescuento", "DESCUENTOS GENERALES DIARIOS");
		return(respuesta.toJSONString());
	}
	
	public double obtenerDescuentoDiarioPedidoVal(int idPedido, String canal)
	{
		double valorDescuento = 0;
		double descuentoTemp = 0;
		//Recuperamos los descuentos que aplican para el día en cuestión como un arrayList
		ArrayList<DescuentoGeneral> descuentos = DescuentoGeneralDAO.obtenerDescuentosGenerales(canal);
		//Recuperamos el detalle pedido del pedido en cuestión
		ArrayList<DetallePedido> detallesPedido = capaDAOCC.DetallePedidoDAO.obtenerDetallesPedidoResumido(idPedido);
		//Comenzamos a recorrer cada uno de los descuentos y recorriendo cada uno de los detalle pedido
		for(int i = 0; i < descuentos.size(); i++)
		{
			DescuentoGeneral descTemp = descuentos.get(i);
			//Colocamos condicional si el descuento es tipo producto
			if(descTemp.getTipoDescuento().equals(new String("P")))
			{
				for(int j = 0; j < detallesPedido.size(); j++)
				{
					DetallePedido detPedidoTemp = detallesPedido.get(j);
					//Validamos si el producto es sujeto a descuento
					if(detPedidoTemp.getIdproducto() == descTemp.getIdProducto())
					{
						//En caso de que coincidan los productos tendremos dos situaciones en las que aplica el descuento 
						//Si el idExcepción es diferente de cero y aplica Excepcion es S o idExcepcion es cero 
						if((detPedidoTemp.getIdexcepcion() > 0 && descTemp.getAplicaExcepcion().equals(new String("S"))) ||(detPedidoTemp.getIdexcepcion() == 0))
						{
							//Verificamos si el descuento es en pesos o en caso contrario si es en porcentaje.	
							if(descTemp.getValorPesos() > 0)
							{
								descuentoTemp = (descTemp.getValorPesos());
							}else
							{
								double totalDetallePedido = capaDAOCC.DetallePedidoDAO.obtenerTotalDetallePedidoPadre(detPedidoTemp.getIdpedido(), detPedidoTemp.getIddetallepedido());
								descuentoTemp = totalDetallePedido*(((double)descTemp.getValorPorcentaje())/100);
							}
							//acumulamos el valor de descuento en la variable dispuesta para tal fin
							valorDescuento = valorDescuento + descuentoTemp;
						}
					}
				}
			}else if(descTemp.getTipoDescuento().equals(new String("I")))
			{
				//Cuando es por ingredientes la idea es que el descuento se maneja en pesos
				double descuentoDisponible = 0;
				double valorMaximoDescuento = descTemp.getValorPesos();
				//La lógica consiste en mirar cada detalle pedido ver si el producto le aplica la promoción de Ingredientes
				//Si le aplica ir y revisar las adiciones y sumar el valor su costo y hacerle el descuento pertinente
				for(int j = 0; j < detallesPedido.size(); j++)
				{
					DetallePedido detPedidoTemp = detallesPedido.get(j);
					if(detPedidoTemp.getIdproducto() == descTemp.getIdProducto())
					{
						//En caso de que coincidan los productos tendremos dos situaciones en las que aplica el descuento 
						//Si el idExcepción es diferente de cero y aplica Excepcion es S o idExcepcion es cero 
						if((detPedidoTemp.getIdexcepcion() > 0 && descTemp.getAplicaExcepcion().equals(new String("S"))) ||(detPedidoTemp.getIdexcepcion() == 0))
						{
							descuentoDisponible = valorMaximoDescuento;
							//Validamos que la oferta si tenga un descuento disponible
							if(valorMaximoDescuento > 0)
							{
								//Recuperamos los idDetallePedido que corresponden a las adiciones del producto principal y vamos a realizar el cálculo
								ArrayList<Integer> adicionesDetalle = AdicionDetallePedidoDAO.ObtenerIdAdicionDetallePedido(detPedidoTemp.getIddetallepedido());
								for(int z = 0; z < adicionesDetalle.size(); z++)
								{
									//Comenzamos a procesar las adiciones que tiene el detalle Pedido
									Integer detPedido  = (Integer)adicionesDetalle.get(z);
									DetallePedido detPedAdi = capaDAOCC.DetallePedidoDAO.obtenerDetallePedido(detPedido.intValue());
									if((descuentoDisponible - detPedAdi.getValortotal())>0)
									{
										valorDescuento  = valorDescuento + detPedAdi.getValortotal();
										descuentoDisponible = descuentoDisponible - detPedAdi.getValortotal();
									}else
									{
										valorDescuento  = valorDescuento + descuentoDisponible;
										descuentoDisponible = 0;
										break;
									}
								}
							}
						}
					}
				}
			}
			
		}
		return(valorDescuento);
	}
	
	
//	public static void main(String args[])
//	{
//		PedidoCtrl PedidoCtrl = new PedidoCtrl();
//		PedidoCtrl.verificarEnvioLinkPagosProcesoWompi(435653, 275623, 10000, 1);
//	}
	
	
	//TIENDA VIRTUAL WORDPRESS
//	public static void main(String[] args)
//	{
//		String strInicial = "{\"id\":15294,\"parent_id\":0,\"number\":\"15294\",\"order_key\":\"wc_order_63kFblzevMDiC\",\"created_via\":\"checkout\",\"version\":\"4.0.1\",\"status\":\"processing\",\"currency\":\"COP\",\"date_created\":\"2020-11-09T14:11:55\",\"date_created_gmt\":\"2020-11-09T19:11:55\",\"date_modified\":\"2020-11-09T14:11:55\",\"date_modified_gmt\":\"2020-11-09T19:11:55\",\"discount_total\":\"0\",\"discount_tax\":\"0\",\"shipping_total\":\"0\",\"shipping_tax\":\"0\",\"cart_tax\":\"0\",\"total\":\"30000\",\"total_tax\":\"0\",\"prices_include_tax\":false,\"customer_id\":6,\"customer_ip_address\":\"200.122.197.98\",\"customer_user_agent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36\",\"customer_note\":\"\",\"billing\":{\"first_name\":\"Juan David\",\"last_name\":\"Botero\",\"company\":\"\",\"address_1\":\"Calle 63A # 47 - 27 Medellin\",\"address_2\":\"\",\"city\":\"\",\"state\":\"\",\"postcode\":\"\",\"country\":\"CO\",\"email\":\"jubote1@gmail.com\",\"phone\":\"3148807777\"},\"shipping\":{\"first_name\":\"Juan David\",\"last_name\":\"Botero\",\"company\":\"\",\"address_1\":\"\",\"address_2\":\"\",\"city\":\"\",\"state\":\"\",\"postcode\":\"\",\"country\":\"CO\"},\"payment_method\":\"cod\",\"payment_method_title\":\"Pago en efectivo\",\"transaction_id\":\"\",\"date_paid\":null,\"date_paid_gmt\":null,\"date_completed\":null,\"date_completed_gmt\":null,\"cart_hash\":\"c1461c74dbe59f9fa33fd33a1113cd23\",\"meta_data\":[{\"id\":540163,\"key\":\"_billing_Confirmar_Direcciu00f3n\",\"value\":\"\"},{\"id\":540164,\"key\":\"_billing_cantidad_pago\",\"value\":\"50000\"},{\"id\":540165,\"key\":\"_billing_sede\",\"value\":\"Sede Manrique Piloto\"},{\"id\":540166,\"key\":\"_billing_tiempo\",\"value\":\"30 minutos\"},{\"id\":540167,\"key\":\"is_vat_exempt\",\"value\":\"no\"},{\"id\":540168,\"key\":\"additional_Zona/Barrio\",\"value\":\"prueba\"},{\"id\":540169,\"key\":\"additional_Observaciu00f3n_direcciu00f3n\",\"value\":\"prueba\"},{\"id\":540170,\"key\":\"_wc_facebook_for_woocommerce_order_placed\",\"value\":\"yes\"},{\"id\":540175,\"key\":\"_wc_facebook_for_woocommerce_purchase_tracked\",\"value\":\"yes\"}],\"line_items\":[{\"id\":24157,\"name\":\"Mitad Mitad!\",\"product_id\":3638,\"variation_id\":3641,\"quantity\":1,\"tax_class\":\"\",\"subtotal\":\"30000\",\"subtotal_tax\":\"0\",\"total\":\"30000\",\"total_tax\":\"0\",\"taxes\":[],\"meta_data\":[{\"id\":238495,\"key\":\"pa_tamanos\",\"value\":\"grande\"},{\"id\":238496,\"key\":\"Mitad y Mitad\",\"value\":\"Paisa\"},{\"id\":238497,\"key\":\"Mitad y Mitad\",\"value\":\"Mexicana\"},{\"id\":238498,\"key\":\"Adiciones de Grande (&#36;1.600)\",\"value\":\"Mau00edz\"},{\"id\":238499,\"key\":\"Adiciones de Grande (&#36;1.600)\",\"value\":\"Salamy\"},{\"id\":238500,\"key\":\"Selecciona tu bebida\",\"value\":\"Colombiana\"}],\"sku\":\"MITMITGRA\",\"price\":30000}],\"tax_lines\":[],\"shipping_lines\":[{\"id\":24158,\"method_title\":\"Valor del domicilio\",\"method_id\":\"flat_rate\",\"instance_id\":\"4\",\"total\":\"0\",\"total_tax\":\"0\",\"taxes\":[],\"meta_data\":[{\"id\":238506,\"key\":\"Artu00edculos\",\"value\":\"Mitad Mitad! &times; 1\"}]}],\"fee_lines\":[],\"coupon_lines\":[],\"refunds\":[],\"currency_symbol\":\"$\",\"_links\":{\"self\":[{\"href\":\"https://pizzaamericana.co/sede/wp-json/wc/v3/orders/15294\"}],\"collection\":[{\"href\":\"https://pizzaamericana.co/sede/wp-json/wc/v3/orders\"}],\"customer\":[{\"href\":\"https://pizzaamericana.co/sede/wp-json/wc/v3/customers/6\"}]}}";
//		byte[] byteText = null;
//		try {
//			byteText = strInicial.getBytes("UTF-8");
//		} catch (UnsupportedEncodingException e1) {
//			e1.printStackTrace();
//		}
//		//To get original string from byte.
//		String strJSON = "";
//		try {
//			strJSON = new String(byteText , "UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		PedidoCtrl pedCtrl = new PedidoCtrl();
//		pedCtrl.insertarPedidoTiendaVirtual(strJSON);
//	}
	
	//TIENDA VIRTUAL KUNO
//	public static void main(String[] args)
//	{
//		String strInicial = "{\"count\":1,\"orders\":[{\"instructions\":\"\",\"coupons\":[],\"tax_list\":[],\"missed_reason\":null,\"billing_details\":null,\"fulfillment_option\":null,\"table_number\":null,\"ready\":false,\"updated_at\":\"2022-10-10T17:53:25.000Z\",\"id\":581411611,\"total_price\":46500,\"sub_total_price\":44500,\"tax_value\":0,\"persons\":0,\"latitude\":\"6.259071354182328\",\"longitude\":\"-75.55976914907379\",\"client_first_name\":\"Juan David\",\"client_last_name\":\"Botero Duque\",\"client_email\":\"jubote1@gmail.com\",\"client_phone\":\"+573148807773\",\"restaurant_name\":\"Pizza Americana Manrique Piloto\",\"currency\":\"COP\",\"type\":\"delivery\",\"status\":\"pending\",\"source\":\"website\",\"pin_skipped\":false,\"accepted_at\":null,\"tax_type\":\"GROSS\",\"tax_name\":\"Sales Tax\",\"fulfill_at\":\"2022-10-10T21:30:00.000Z\",\"client_language\":\"es\",\"integration_payment_provider\":null,\"integration_payment_amount\":0,\"reference\":null,\"restaurant_id\":267607,\"client_id\":13094867,\"restaurant_phone\":\"+5744444553\",\"restaurant_timezone\":\"America/Bogota\",\"card_type\":null,\"used_payment_methods\":[\"CASH\"],\"company_account_id\":993823,\"pos_system_id\":29888,\"restaurant_key\":\"r1RkYFNQZzaCk9yxTgqOdQjHsJiFnPTbR\",\"restaurant_country\":\"Colombia\",\"restaurant_city\":\"Medellin\",\"restaurant_zipcode\":\"050011\",\"restaurant_street\":\"Calle 68 #43-05\",\"restaurant_latitude\":\"6.263416100000011\",\"restaurant_longitude\":\"-75.55329222209016\",\"client_marketing_consent\":true,\"restaurant_token\":\"11\",\"gateway_transaction_id\":null,\"gateway_type\":null,\"api_version\":2,\"payment\":\"CASH\",\"for_later\":true,\"client_address\":\"Calle 63 a # 47 - 27, Apt.101, Medellín Prado Centro\",\"client_address_parts\":{\"street\":\"Calle 63 a # 47 - 27\",\"city\":\"Medellín Prado Centro\",\"more_address\":\"Apt.101\"},\"items\":[{\"id\":779245101,\"name\":\"DELIVERY_FEE\",\"total_item_price\":2000,\"price\":2000,\"quantity\":1,\"instructions\":null,\"type\":\"delivery_fee\",\"type_id\":565857,\"tax_rate\":0,\"tax_value\":0,\"parent_id\":null,\"item_discount\":0,\"cart_discount_rate\":0,\"cart_discount\":0,\"tax_type\":\"GROSS\",\"options\":[]},{\"id\":779252085,\"name\":\"PIZZA ESTOFADA\",\"total_item_price\":44500,\"price\":38000,\"quantity\":1,\"instructions\":\"\",\"type\":\"item\",\"type_id\":15002995,\"tax_rate\":0,\"tax_value\":0,\"parent_id\":null,\"item_discount\":0,\"cart_discount_rate\":0,\"cart_discount\":0,\"tax_type\":\"GROSS\",\"options\":[{\"id\":696388974,\"name\":\"Grande Estofada (8 porciones)\",\"price\":5000,\"group_name\":\"Tamaño\",\"quantity\":1,\"type\":\"size\",\"type_id\":7416293},{\"id\":696388975,\"name\":\"Peperoni y Queso\",\"price\":1500,\"group_name\":\"Selecciona la especialidad 1\",\"quantity\":1,\"type\":\"option\",\"type_id\":17331806},{\"id\":696388976,\"name\":\"Sal de Ajo\",\"price\":0,\"group_name\":\"Condimentos\",\"quantity\":1,\"type\":\"option\",\"type_id\":8680744},{\"id\":696388977,\"name\":\"Pepsi\",\"price\":0,\"group_name\":\"Selecciona tu bebida\",\"quantity\":1,\"type\":\"option\",\"type_id\":8680739}]}]}]}";
//		byte[] byteText = null;
//		try {
//			byteText = strInicial.getBytes("UTF-8");
//		} catch (UnsupportedEncodingException e1) {
//			e1.printStackTrace();
//		}
//		//To get original string from byte.
//		String strJSON = "";
//		try {
//			strJSON = new String(byteText , "UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		PedidoCtrl pedCtrl = new PedidoCtrl();
//		pedCtrl.insertarPedidoTiendaVirtualKuno(strJSON, "PRUEBA");
//	}
	
//	public static void main(String[] args)
//	{
//		String respuesta = "{\n" + 
//				"  \"event\": \"transaction.updated\",\n" + 
//				"  \"data\": {\n" + 
//				"    \"transaction\": {\n" + 
//				"        \"id\": \"1234-1610641025-49201\",\n" + 
//				"        \"amount_in_cents\": 4490000,\n" + 
//				"        \"reference\": \"MZQ3X2DE2SMX\",\n" + 
//				"        \"customer_email\": \"juan.perez@gmail.com\",\n" + 
//				"        \"currency\": \"COP\",\n" + 
//				"        \"payment_method_type\": \"NEQUI\",\n" + 
//				"        \"redirect_url\": \"https://mitienda.com.co/pagos/redireccion\",\n" + 
//				"        \"status\": \"APPROVED\",\n" + 
//				"        \"shipping_address\": null,\n" + 
//				"        \"payment_link_id\": \"VPOS_MmLnoT\",\n" + 
//				"        \"payment_source_id\": null\n" + 
//				"      }\n" + 
//				"  },\n" + 
//				"  \"environment\": \"prod\",\n" + 
//				"  \"signature\": {\n" + 
//				"    \"properties\": [\n" + 
//				"      \"transaction.id\",\n" + 
//				"      \"transaction.status\",\n" + 
//				"      \"transaction.amount_in_cents\"\n" + 
//				"    ],\n" + 
//				"    \"checksum\": \"3476DDA50F64CD7CBD160689640506FEBEA93239BC524FC0469B2C68A3CC8BD0\"\n" + 
//				"  },\n" + 
//				"  \"timestamp\": 1530291411,\n" + 
//				"  \"sent_at\":  \"2018-07-20T16:45:05.000Z\"\n" + 
//				"}";
//		PedidoCtrl pedCtrl = new PedidoCtrl();
//		pedCtrl.capturarEventoPagoWompi(respuesta);
//	}
	
	public String avanzarEstadoPedidoDomicilioLog(int idPedidoTienda, int idTienda, String claveUsuario, String observacion)
	{
		String respuesta = "";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		//Recuperamos la tienda que requerimos trabajar con el servicio
		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda != null)
		{
			//Realizar invocación de servicio en tienda
			String rutaURL = tienda.getUrl() + "DarEntregaDomicilio?idpedidotienda=" + idPedidoTienda + "&claveusuario=" + claveUsuario + "&idtienda=" + idTienda + "&observacion=" + observacion;
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				StringBuffer retornoTienda = new StringBuffer();
				//Se realiza la ejecución del servicio de finalizar pedido
				HttpResponse responseFinPed = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    		responseFinPed.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				System.out.println(retorno);
				respuesta = retorno.toString();
			}catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
		if(respuesta.equals(new String("")))
		{
			JSONObject resultado = new JSONObject();
			resultado.put("resultado", "error");
			resultado.put("tipo_error", "error servicio tienda");
		}
		return(respuesta);
	}
	
	//CambiarFormaPagoPedidoApp
	public String cambiarFormaPagoPedidoApp(int idPedidoTienda, int idTienda, String claveUsuario, String observacion)
	{
		String respuesta = "";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		//Recuperamos la tienda que requerimos trabajar con el servicio
		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda != null)
		{
			//Realizar invocación de servicio en tienda
			String rutaURL = tienda.getUrl() + "CambiarFormaPagoPedidoApp?idpedidotienda=" + idPedidoTienda + "&claveusuario=" + claveUsuario + "&idtienda=" + idTienda + "&observacion=" + observacion;
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				StringBuffer retornoTienda = new StringBuffer();
				//Se realiza la ejecución del servicio de finalizar pedido
				HttpResponse responseFinPed = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    		responseFinPed.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				System.out.println(retorno);
				respuesta = retorno.toString();
			}catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
		if(respuesta.equals(new String("")))
		{
			JSONObject resultado = new JSONObject();
			resultado.put("resultado", "error");
			resultado.put("tipo_error", "error servicio tienda");
		}
		return(respuesta);
	}
	
	//CambiarFormaPagoPedidoApp
		public String obtenerResumenDomiciliarioApp(int idTienda, String claveUsuario)
		{
			String respuesta = "";
			//Realizamos la invocación mediante el uso de HTTPCLIENT
			HttpClient client = HttpClientBuilder.create().build();
			//Recuperamos la tienda que requerimos trabajar con el servicio
			Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
			if (tienda != null)
			{
				//Realizar invocación de servicio en tienda
				String rutaURL = tienda.getUrl() + "ObtenerResumenDomiciliarioApp?claveusuario=" + claveUsuario + "&idtienda=" + idTienda;
				HttpGet request = new HttpGet(rutaURL);
				try
				{
					StringBuffer retorno = new StringBuffer();
					StringBuffer retornoTienda = new StringBuffer();
					//Se realiza la ejecución del servicio de finalizar pedido
					HttpResponse responseFinPed = client.execute(request);
					BufferedReader rd = new BufferedReader
						    (new InputStreamReader(
						    		responseFinPed.getEntity().getContent()));
					String line = "";
					while ((line = rd.readLine()) != null) {
						    retorno.append(line);
						}
					System.out.println(retorno);
					respuesta = retorno.toString();
				}catch(Exception e)
				{
					System.out.println(e.toString());
				}
			}
			if(respuesta.equals(new String("")))
			{
				JSONObject resultado = new JSONObject();
				resultado.put("resultado", "error");
				resultado.put("tipo_error", "error servicio tienda");
			}
			return(respuesta);
		}
		
		public String obtenerResumenDomiciliarioAppV2(int idTienda, String claveUsuario)
		{
			String respuesta = "";
			//Realizamos la invocación mediante el uso de HTTPCLIENT
			HttpClient client = HttpClientBuilder.create().build();
			//Recuperamos la tienda que requerimos trabajar con el servicio
			Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
			if (tienda != null)
			{
				//Realizar invocación de servicio en tienda
				String rutaURL = tienda.getUrl() + "ObtenerResumenDomiciliarioAppV2?claveusuario=" + claveUsuario + "&idtienda=" + idTienda;
				HttpGet request = new HttpGet(rutaURL);
				try
				{
					StringBuffer retorno = new StringBuffer();
					StringBuffer retornoTienda = new StringBuffer();
					//Se realiza la ejecución del servicio de finalizar pedido
					HttpResponse responseFinPed = client.execute(request);
					BufferedReader rd = new BufferedReader
						    (new InputStreamReader(
						    		responseFinPed.getEntity().getContent()));
					String line = "";
					while ((line = rd.readLine()) != null) {
						    retorno.append(line);
						}
					System.out.println(retorno);
					respuesta = retorno.toString();
				}catch(Exception e)
				{
					System.out.println(e.toString());
				}
			}
			if(respuesta.equals(new String("")))
			{
				JSONObject resultado = new JSONObject();
				resultado.put("resultado", "error");
				resultado.put("tipo_error", "error servicio tienda");
			}
			return(respuesta);
		}
		
	
	public void insertarDomiciliarioPedido(DomiciliarioPedido domPedido)
	{
		DomiciliarioPedidoDAO.insertarDomiciliarioPedido(domPedido);
	}
	
	public static void notificarWhatsAppUltramsg(String nombre, int idPedido, int idCliente, String linkPago)
	{
		String telefonoCelular = "";
		String respuestaServicio = "";
		Cliente clienteNotif = ClienteDAO.obtenerClienteporID(idCliente);
		//Revisamos la lógica para obtener el telefono
		if(clienteNotif.getTelefonoCelular()!= null)
		{
			if(!clienteNotif.getTelefonoCelular().equals(new String("")))
			{
				if(clienteNotif.getTelefonoCelular().length() == 10)
				{
					if(clienteNotif.getTelefonoCelular().substring(0,1).equals(new String("3")))
					{
						telefonoCelular = clienteNotif.getTelefonoCelular();
					}
				}
			}
		}
		
		if(telefonoCelular.equals(new String("")))
		{
			if(clienteNotif.getTelefono()!= null)
			{
				if(!clienteNotif.getTelefono().equals(new String("")))
				{
					if(clienteNotif.getTelefono().length() == 10)
					{
						if(clienteNotif.getTelefono().substring(0,1).equals(new String("3")))
						{
							telefonoCelular = clienteNotif.getTelefono();
						}
					}
				}
			}
		}
		
		//Validaremos que el telefono celular si se hubiese podido tomar
		if(!telefonoCelular.equals(new String("")))
		{
			OkHttpClient client = new OkHttpClient();
			IntegracionCRM intWhat = IntegracionCRMDAO.obtenerInformacionIntegracion("ULTRAMSG");
			okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
			String mensajeEvidencia = "token="+ intWhat.getAccessToken() +"&to=+57"+ telefonoCelular + "&body=Estimado " + nombre +", este es tu link de pago " + linkPago + " . Ingresa y realiza el proceso de pago. Una vez efectuado el pago,iniciaremos la elaboración de tu pedido. ¡Para que el link sea habilitado en este mensaje, deberás guardar este contacto, pero ten en cuenta que a este número no deberás escribir dado que no tendrás respuesta! &priority=1&referenceId=";
			RequestBody body = RequestBody.create(mediaType, "token="+ intWhat.getAccessToken() +"&to=+57"+ telefonoCelular + "&body=Estimado " + nombre +", este es tu link de pago " + linkPago + " . Ingresa y realiza el proceso de pago. Una vez efectuado el pago,iniciaremos la elaboración de tu pedido. ¡Para que el link sea habilitado en este mensaje, deberás guardar este contacto, pero ten en cuenta que a este número no deberás escribir dado que no tendrás respuesta! &priority=1&referenceId=");
			Request request = new Request.Builder()
			  .url("https://api.ultramsg.com/"+ intWhat.getClientID() +"/messages/chat")
			  .post(body)
			  .addHeader("content-type", "application/x-www-form-urlencoded")
			  .build();
			try
			{
				okhttp3.Response response = client.newCall(request).execute();
				String resultado = response.toString();
				System.out.println(resultado);
			}catch(Exception e)
			{
				System.out.println("ERROR " + e.toString());
				//Recuperar la lista de distribución para este correo
				ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
				Date fecha = new Date();
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
				correo.setAsunto("OJO ERROR EN SERVICIO DE WHATSAPP  " + fecha.toString());
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje("Se presenta error en servicio de API de WhatsApp. " + e.toString() + mensajeEvidencia);
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
			}
			
			
		}	
	}
	
	
	/**
	 * MÉTODO EN DESUSO PRODUCTO DE QUE NO TENEMOS RELACIÓN CON BOTTA SOFTWARE
	 * @param nombre
	 * @param idPedido
	 * @param idCliente
	 * @param linkPago
	 */
	public static void notificarWhatsApp(String nombre, int idPedido, int idCliente, String linkPago)
	{
		String telefonoCelular = "";
		String respuestaServicio = "";
		Cliente clienteNotif = ClienteDAO.obtenerClienteporID(idCliente);
		//Revisamos la lógica para obtener el telefono
		if(clienteNotif.getTelefonoCelular()!= null)
		{
			if(!clienteNotif.getTelefonoCelular().equals(new String("")))
			{
				if(clienteNotif.getTelefonoCelular().length() == 10)
				{
					if(clienteNotif.getTelefonoCelular().substring(0,1).equals(new String("3")))
					{
						telefonoCelular = clienteNotif.getTelefonoCelular();
					}
				}
			}
		}
		
		if(telefonoCelular.equals(new String("")))
		{
			if(clienteNotif.getTelefono()!= null)
			{
				if(!clienteNotif.getTelefono().equals(new String("")))
				{
					if(clienteNotif.getTelefono().length() == 10)
					{
						if(clienteNotif.getTelefono().substring(0,1).equals(new String("3")))
						{
							telefonoCelular = clienteNotif.getTelefono();
						}
					}
				}
			}
		}
		
		//Validaremos que el telefono celular si se hubiese podido tomar
		if(!telefonoCelular.equals(new String("")))
		{
			String jsonString = "{" +
		            "\"templateId\": \"link_pago\","+
		            "\"customerNumber\": \"whatsapp:+57" + telefonoCelular +"\"," + 
		            "\"inputs\": [\""+ nombre+" - "+ idPedido +"\" , \""+ linkPago +"\"]" +
		          "}";
					//Realizamos la invocación mediante el uso de HTTPCLIENT
					HttpClient client = HttpClientBuilder.create().build();
					String rutaURLNotif = "https://us-east1-bottapizzaamericana.cloudfunctions.net/fnBottaWhatsAppNotification";
					HttpPost request = new HttpPost(rutaURLNotif);
					try
					{
						//Fijamos el header con el token
						//NO HAY SEGURIDAD TODAVÍA
						//request.setHeader("Authorization", "Bearer " + "prv_prod_Qdb2HcV6AkbkvCKr9UWbhFs6L73IFCkT");
						request.setHeader("Accept", "application/json");
						request.setHeader("Content-type", "application/json");
						//Fijamos los parámetros
						//pass the json string request in the entity
					    HttpEntity entity = new ByteArrayEntity(jsonString.getBytes("UTF-8"));
					    request.setEntity(entity);
						//request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
						StringBuffer retorno = new StringBuffer();
						HttpResponse responseFinPed = client.execute(request);
						BufferedReader rd = new BufferedReader
							    (new InputStreamReader(
							    		responseFinPed.getEntity().getContent()));
						String line = "";
						while ((line = rd.readLine()) != null) {
							    retorno.append(line);
							}
						respuestaServicio = retorno.toString();
						if(respuestaServicio.equals(new String("ok : Mensaje Enviado correctamente")))
						{
							
						}else
						{
							//Recuperar la lista de distribución para este correo
							ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
							Date fecha = new Date();
							Correo correo = new Correo();
							CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
							correo.setAsunto("OJO ERROR EN SERVICIO DE WHATSAPP CON BOTTA OJO DESUSO " + fecha.toString());
							correo.setContrasena(infoCorreo.getClaveCorreo());
							correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
							correo.setMensaje("Se presenta error en servicio de API de WhatsApp.");
							ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
							contro.enviarCorreo();
						}
						//Traemos el valor del JSON con toda la info del pedido
						String datosJSON = retorno.toString();
						System.out.println(datosJSON);
					}catch (Exception e2) {
				        e2.printStackTrace();
				        System.out.println(e2.toString());
				    }
		}	
	}
	
	public static void enviarWhatsAppUltramsg(String mensaje, String telefonoCelular)
	{
		
		//Validaremos que el telefono celular si se hubiese podido tomar
		if(!telefonoCelular.equals(new String("")))
		{
			OkHttpClient client = new OkHttpClient();
			IntegracionCRM intWhat = IntegracionCRMDAO.obtenerInformacionIntegracion("ULTRAMSG");
			okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
			String mensajeEvidencia = "token="+ intWhat.getAccessToken() +"&to=+57"+ telefonoCelular + "&body=" + mensaje +"&priority=1&referenceId=";
			RequestBody body = RequestBody.create(mediaType, "token="+ intWhat.getAccessToken() +"&to=+57"+ telefonoCelular + "&body=" + mensaje +"&priority=1&referenceId=");
			Request request = new Request.Builder()
			  .url("https://api.ultramsg.com/"+ intWhat.getClientID() +"/messages/chat")
			  .post(body)
			  .addHeader("content-type", "application/x-www-form-urlencoded")
			  .build();
			try
			{
				okhttp3.Response response = client.newCall(request).execute();
			}catch(Exception e)
			{
				System.out.println("ERROR " + e.toString());
				//Recuperar la lista de distribución para este correo
				ArrayList correos = GeneralDAO.obtenerCorreosParametro("ERROR");
				Date fecha = new Date();
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
				correo.setAsunto("OJO ERROR EN SERVICIO DE WHATSAPP  " + fecha.toString() + mensajeEvidencia);
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje("Se presenta error en servicio de API de WhatsApp." + e.toString());
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
			}	
		}	
	}
	
	public String obtenerPromociones(){
		JSONArray listJSON = new JSONArray();
		ArrayList<Promocion> promociones = PromocionDAO.obtenerPromociones();
		for (Promocion pro : promociones) {
			JSONObject cadaPromoJSON = new JSONObject();
			cadaPromoJSON.put("idpromocion", pro.getIdPromocion());
			cadaPromoJSON.put("descripcion", pro.getDescripcion());
			listJSON.add(cadaPromoJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	
	public String obtenerEstadisticasPromocion(String fecha){
		JSONArray listJSON = new JSONArray();
		ArrayList<EstadisticaPromocion> estadisticas = EstadisticaPromocionDAO.obtenerEstadisticasPromocion(fecha);
		for (EstadisticaPromocion est : estadisticas) {
			JSONObject cadaEstJSON = new JSONObject();
			cadaEstJSON.put("fecha", est.getFecha());
			cadaEstJSON.put("idtienda", est.getIdTienda());
			cadaEstJSON.put("idpromocion", est.getIdPromocion());
			cadaEstJSON.put("contact", est.getContact());
			cadaEstJSON.put("tiendavirtual", est.getTiendaVirtual());
			cadaEstJSON.put("total", est.getTotal());
			listJSON.add(cadaEstJSON);
		}
		
		return listJSON.toJSONString();
	}
	
	public static void probarHere()
	{
		try
		{
		String result = "";
        String apikey ="GsNtuVVEgGawtZDJwrcB_YcNs0lQ0JcMa5UXYQZN3wU";
        String searchtext= "Cra. 76 #96-50 a 96-102,Colombia,Medellín,Antioquia";
        String connstr = "https://geocoder.ls.hereapi.com/6.2/geocode.json?apiKey="+apikey+"&searchtext="+ URLEncoder.encode(searchtext,"UTF-8");
        //Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(connstr);
		
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			String datosJSON = retorno.toString();
			System.out.println(datosJSON);
		}catch (Exception e2) {
	        e2.printStackTrace();
	        System.out.println("ERROR " + e2.toString());
	    }
		      
	}
	
	public static void crearTareaTookan()
	{
		Client client = ClientBuilder.newClient();
		Entity payload = Entity.json("{  \"api_key\": \"53606981f44403105f187a7e121077471ae2c0ff28d97d365f1808c6\",  \"order_id\": \"654321\",  \"job_description\": \"groceries delivery\",  \"job_pickup_phone\": \"+1201555555\",  \"job_pickup_name\": \"7 Eleven Store\",  \"job_pickup_email\": \"\",  \"job_pickup_address\": \"114, sansome street, San Francisco\",  \"job_pickup_latitude\": \"30.7188978\",  \"job_pickup_longitude\": \"76.810296\",  \"job_pickup_datetime\": \"2022-09-30 13:00:00\",  \"pickup_custom_field_template\": \"Template_1\",  \"pickup_meta_data\": [    {      \"label\": \"Price\",      \"data\": \"100\"    },    {      \"label\": \"Quantity\",      \"data\": \"100\"    }  ],  \"team_id\": \"\",  \"auto_assignment\": \"0\",  \"has_pickup\": \"1\",  \"has_delivery\": \"0\",  \"layout_type\": \"0\",  \"tracking_link\": 1,  \"timezone\": \"+300\",  \"fleet_id\": \"\",  \"p_ref_images\": [    \"http://tookanapp.com/wp-content/uploads/2015/11/logo_dark.png\"  ],  \"notify\": 1,  \"tags\": \"\",  \"geofence\": 0}");
		Response response = client.target("https://api.tookanapp.com/v2/create_task")
		  .request(MediaType.APPLICATION_JSON_TYPE)
		  .post(payload);

		System.out.println("status: " + response.getStatus());
		System.out.println("headers: " + response.getHeaders());
		System.out.println("body:" + response.readEntity(String.class));
	}
	
//	public static void main(String args[])
//	{
//		//crearTareaTookan();
//	}
	
	public String insertarPedidoPrecioEmpleado(PedidoPrecioEmpleado pedPrecio)
	{
		JSONObject respuestaJSON = new JSONObject();
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		String codigo = promoCtrl.generarCodigoPrecioEmpleado();
		pedPrecio.setCodigo(codigo);
		//Incluimos la validación del código para asignar
		PedidoPrecioEmpleadoDAO.insertarPedidoPrecioEmpleado(pedPrecio);
		respuestaJSON.put("codigo", codigo);
		return(codigo);
	}
	
	public String generarCodigoValeEmpleadoRemoto(int idEmpleado, String fecha)
	{
		JSONObject respuestaJSON = new JSONObject();
		PromocionesCtrl promoCtrl = new PromocionesCtrl();
		String codigo = promoCtrl.generarCodigoEmpleadoRemotovale();
		//Incluimos la validación del código para asignar
		EmpleadoRemotoValeDAO.insertarEmpleadoRemotoVale(idEmpleado,fecha,codigo);
		respuestaJSON.put("codigo", codigo);
		return(codigo);
	}
	
	public boolean validarFrecuenciaPedidoPrecioEmpleado(int idEmpleado)
	{
		boolean respuesta = false;
		String fechaActual = "";
		//Variables donde manejaremos la fecha anerior con el fin realizar los cálculos de ventas
		Date datFechaAnterior;
		String fechaAnterior = "";
		Calendar calendarioActual = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try
		{
			fechaActual = dateFormat.format(calendarioActual.getTime());
		}catch(Exception exc)
		{
			System.out.println(exc.toString());
		}
		int diaActual = calendarioActual.get(Calendar.DAY_OF_WEEK);
		
		//Domingo
		if(diaActual == 1)
		{
			calendarioActual.add(Calendar.DAY_OF_YEAR, -6);
		}
		else if(diaActual == 2)
		{
			calendarioActual.add(Calendar.DAY_OF_YEAR, -0);
		}
		else if(diaActual == 3)
		{
			//Si es martes se resta uno solo
			calendarioActual.add(Calendar.DAY_OF_YEAR, -1);
		}
		else if(diaActual == 4)
		{
			//Si es miercoles se resta dos
			calendarioActual.add(Calendar.DAY_OF_YEAR, -2);
		}
		else if(diaActual == 5)
		{
			//Si es jueves se resta tres
			calendarioActual.add(Calendar.DAY_OF_YEAR, -3);
		}
		else if(diaActual == 6)
		{
			//Si es viernes se resta cuatro
			calendarioActual.add(Calendar.DAY_OF_YEAR, -4);
		}
		else if(diaActual == 7)
		{
			//Si es sabado se resta cinco
			calendarioActual.add(Calendar.DAY_OF_YEAR, -5);
		}
		//Llevamos a un string la fecha anterior para el cálculo de la venta
		datFechaAnterior = calendarioActual.getTime();
		fechaAnterior = dateFormat.format(datFechaAnterior);
		//Fija las fechas tenemos un método para validar cuantas pizzas tiene el empleado para dicha semana
		int cantPedidos = PedidoPrecioEmpleadoDAO.validarFrecuenciaPedidoPrecioEmpleado(idEmpleado, fechaAnterior, fechaActual);
		if(cantPedidos > 0)
		{
			respuesta = true;
		}
		return(respuesta);
	}
	
	public void marcarAutorizadoPedidoPrecioEmpleado(String codigo)
	{
		PedidoPrecioEmpleadoDAO.marcarAutorizadoPedidoPrecioEmpleado(codigo);

	}
	
	public Ubicacion obtenerUbicacionPedido(String direccion, String ciudad)
	{
		Ubicacion ubicaResp = new Ubicacion(0,0);
		JSONParser parser = new JSONParser();
		//Realizaremos el intengo de realizar el llamado para la dirección
		try
		{
			String resultado = "";
		    String apikey ="GsNtuVVEgGawtZDJwrcB_YcNs0lQ0JcMa5UXYQZN3wU";
		    if(ciudad.equals(new String("")))
		    {
		    	ciudad = "Medellín";
		    }
		    String dirBuscar= direccion+",Colombia,"+ ciudad +",Antioquia";
		    String connstr = "https://geocoder.ls.hereapi.com/6.2/geocode.json?apiKey="+apikey+"&searchtext="+ URLEncoder.encode(dirBuscar,"UTF-8");
		    //Realizamos la invocación mediante el uso de HTTPCLIENT
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(connstr);
			
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			resultado = retorno.toString();
			//Posteriormente realizamos la conversión del objeto JSON para tener la latitud y la longitud
			Object objParserServicio = parser.parse(resultado);
			JSONObject jsonObjectGeojsonGeneral = (JSONObject) objParserServicio;
			String strResponse = jsonObjectGeojsonGeneral.get("Response").toString();
			objParserServicio = parser.parse(strResponse);
			jsonObjectGeojsonGeneral = (JSONObject) objParserServicio;
			String strView = (String)jsonObjectGeojsonGeneral.get("View").toString();
			objParserServicio = parser.parse(strView);
			JSONArray View = (JSONArray) objParserServicio;
	        JSONObject coodernadas =  new JSONObject();
	        if (View.toString().length() > 0) {
	        	JSONObject objTempServicio = (JSONObject) View.get(0);
	        	String strResult = (String)objTempServicio.get("Result").toString();
	        	objParserServicio = parser.parse(strResult);
	        	JSONArray result = (JSONArray) objParserServicio;
	        	objTempServicio = (JSONObject) result.get(0);
	        	JSONObject location = (JSONObject) objTempServicio.get("Location");
	        	String strNavigationPosition = (String)location.get("NavigationPosition").toString();
	        	objParserServicio = parser.parse(strNavigationPosition);
	        	JSONArray navigationPosition  = (JSONArray) objParserServicio;
	        	objTempServicio = (JSONObject) navigationPosition.get(0);
	        	ubicaResp.setLatitud((Double)objTempServicio.get("Latitude"));
	        	ubicaResp.setLongitud((Double)objTempServicio.get("Longitude"));
	        	//coodernadas =  View.getJSONObject(0).getJSONArray("Result").getJSONObject(0).getJSONObject("Location").getJSONArray("NavigationPosition").getJSONObject(0);
	        } 
	
		}catch (Exception e2) {
	        e2.printStackTrace();
	        System.out.println("ERROR " + e2.toString());
	    }
		return(ubicaResp);
	}
	
	/**
	 * Método que desde la capa controladora se encarga de procesar la solicitud de una factura electrónica
	 * @param solFactura
	 * @return
	 */
	public String insertarSolicitudFactura(SolicitudFactura solFactura)
	{
		int idsolicitud = SolicitudFacturaDAO.insertarSolicitudFactura(solFactura);
		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON.put("idsolicitud", idsolicitud);
		if(idsolicitud > 0)
		{
			//Mandaremos mensaje de WhatsApp con la generación de la factura
			notificarWhatsAppUltramsgSolFactura(solFactura);
		}
		return(respuestaJSON.toJSONString());
	}
	
	public String consultarSolicitudesFacturaElectronica(String fechaInicial, String fechaFinal, String estado)
	{
		ArrayList<SolicitudFactura> solicitudes = SolicitudFacturaDAO.consultarSolicitudesFacturaElectronica(fechaInicial, fechaFinal, estado);
		JSONObject cadaRespuestaJSON = new JSONObject();
		JSONArray respuestaJSON = new JSONArray();
		System.out.println("longitud del arreglo  " + solicitudes.size());
		for(SolicitudFactura solTemp : solicitudes)
		{
			cadaRespuestaJSON = new JSONObject();
			cadaRespuestaJSON.put("idsolicitud", solTemp.getIdSolicitud());
			cadaRespuestaJSON.put("idpedidocontact", solTemp.getIdPedidoContact());
			cadaRespuestaJSON.put("idpedidotienda", solTemp.getIdPedidoTienda());
			cadaRespuestaJSON.put("valor", solTemp.getValor());
			cadaRespuestaJSON.put("nit", solTemp.getNit());
			cadaRespuestaJSON.put("correo", solTemp.getCorreo());
			cadaRespuestaJSON.put("empresa", solTemp.getEmpresa());
			cadaRespuestaJSON.put("telefono", solTemp.getTelefono());
			cadaRespuestaJSON.put("fechapedido", solTemp.getFechaPedido());
			cadaRespuestaJSON.put("fechasolicitud", solTemp.getFechaSolicitud());
			cadaRespuestaJSON.put("usuario", solTemp.getUsuario());
			cadaRespuestaJSON.put("estado", solTemp.getEstado());
			respuestaJSON.add(cadaRespuestaJSON);
		}

		return(respuestaJSON.toString());
	}
	
	public String ConsultarPedidosVentasCorporativas(String fechaIni, String fechaFin)
	{
		ArrayList<Pedido> pedidos = PedidoDAO.ConsultarPedidosVentasCorporativas(fechaIni, fechaFin);
		JSONObject cadaRespuestaJSON = new JSONObject();
		JSONArray respuestaJSON = new JSONArray();
		for(Pedido pedTemp : pedidos)
		{
			cadaRespuestaJSON = new JSONObject();
			cadaRespuestaJSON.put("idpedido", pedTemp.getIdpedido());
			cadaRespuestaJSON.put("idpedidotienda", pedTemp.getNumposheader());
			cadaRespuestaJSON.put("valor", pedTemp.getTotal_neto());
			cadaRespuestaJSON.put("cliente", pedTemp.getNombrecliente());
			cadaRespuestaJSON.put("fecha", pedTemp.getFechapedido());
			cadaRespuestaJSON.put("asesor", pedTemp.getUsuariopedido());
			cadaRespuestaJSON.put("nombrecompania", pedTemp.getNombreCompania());
			respuestaJSON.add(cadaRespuestaJSON);
		}

		return(respuestaJSON.toString());
	}
	
	public String ConsultarResumenVentasCorporativas(String fechaIni, String fechaFin)
	{
		ArrayList <ResumenVentaEmpresarial> resumenes = PedidoDAO.ConsultarResumenVentasCorporativas(fechaIni, fechaFin);
		JSONObject cadaRespuestaJSON = new JSONObject();
		JSONArray respuestaJSON = new JSONArray();
		for(ResumenVentaEmpresarial resTemp : resumenes)
		{
			cadaRespuestaJSON = new JSONObject();
			cadaRespuestaJSON.put("asesor", resTemp.getAsesor());
			cadaRespuestaJSON.put("totalventa", resTemp.getTotalVenta());
			cadaRespuestaJSON.put("comision", resTemp.getComision());
			respuestaJSON.add(cadaRespuestaJSON);
		}

		return(respuestaJSON.toString());
	}
	
	public void notificarWhatsAppUltramsgSolFactura(SolicitudFactura solFactura)
	{
		OkHttpClient client = new OkHttpClient();
		IntegracionCRM intWhat = IntegracionCRMDAO.obtenerInformacionIntegracion("ULTRAMSG");
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
		String mensajeEvidencia = "token="+ intWhat.getAccessToken() +"&to=+57"+ "3148807773" + "&body=*SE HA RADICADO UNA NUEVA SOLICITUD DE FACTURA ELECTRÓNICA*, # Pedido Contact " + solFactura.getIdPedidoContact() +", # pedido tienda " + solFactura.getIdPedidoTienda() + " , por un valor de  $" + solFactura.getValor() + ", para la empresa " + solFactura.getEmpresa() + ", de una fecha de pedido " + solFactura.getFechaPedido() + " ¡No te tardes en generarla.! &priority=1&referenceId=";
		RequestBody body = RequestBody.create(mediaType, "token="+ intWhat.getAccessToken() +"&to=+57"+ "3148807773" + "&body=*SE HA RADICADO UNA NUEVA SOLICITUD DE FACTURA ELECTRÓNICA*, # Pedido Contact " + solFactura.getIdPedidoContact() +", # pedido tienda " + solFactura.getIdPedidoTienda() + " , por un valor de  $" + solFactura.getValor() + ", para la empresa " + solFactura.getEmpresa() + ", de una fecha de pedido " + solFactura.getFechaPedido() + " ¡No te tardes en generarla.! &priority=1&referenceId=");
		Request request = new Request.Builder()
		  .url("https://api.ultramsg.com/"+ intWhat.getClientID() +"/messages/chat")
		  .post(body)
		  .addHeader("content-type", "application/x-www-form-urlencoded")
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
		}catch(Exception e)
		{
			System.out.println("ERROR " + e.toString());
			//Recuperar la lista de distribución para este correo
			ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEVIRTUALSINPAGO");
			Date fecha = new Date();
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
			correo.setAsunto("OJO ERROR EN SERVICIO DE WHATSAPP  " + fecha.toString() + mensajeEvidencia);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje("Se presenta error en servicio de API de WhatsApp." + e.toString());
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
		}	
	}
	
	public String generarStringHTMLReporteOperacionVenta()
	{
		//Requerimos primero que todo obtener el rango de fechas con el fin de tener dicho rango para las consultas
		//Definimos el formato como manejaremos las fechas
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat dateFormatHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		DecimalFormat formatea = new DecimalFormat("###,###");
		//Traemos la fechaActual en Blanco
		String fechaActual = "";
		//Traemos la fecha actual en un date
		Date datFechaActual = new Date();
		//Comenzamos a traer la fecha actual como un String
		fechaActual = dateFormat.format(datFechaActual);
		//Con la fecha actual vamos a realizar la consulta de los pedidos
		
		//Necesitamos obtener la fecha hora actual menos una hora en el formato para consultar en mysql
		Calendar calendario = Calendar.getInstance();
		calendario.setTime(datFechaActual); 
		calendario.add(Calendar.HOUR, -1);
		Date datFechaMenosHora = calendario.getTime();
		String fechaActualMenosHora = dateFormatHora.format(datFechaMenosHora);
		
		
		//Realizamos la extracción de los tiempos pedidos
		String respuesta = "";
		boolean indicadorCorreo = false;
		//Recuperaremos las tiendas y empezaremos a ir consultando una a una las tiendas para extraer la información
		ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendas();
		String cantMinutos = "";
		String strPedidosProg = "";
		for(Tienda tien : tiendas)
		{
			strPedidosProg = "(";
			if(!tien.getHosbd().equals(new String("")))
			{
				//Vamos a agregar los pedidos programados
				respuesta = respuesta + "<table border='2'><tr><td colspan='4'>" + tien.getNombreTienda() + "</td></tr>";
				respuesta = respuesta + "<tr>"
						+  "<td><strong>PEDIDO</strong></td>"
						+  "<td><strong>FACTURA TIENDA</strong></td>"
						+  "<td><strong>VALOR PEDIDO</strong></td>"
						+  "<td><strong>HORA PROGRAMADO</strong></td>"
						+  "</tr>";
				ArrayList pedidosProgramados = PedidoDAO.obtenerPedidosProgramadosTienda(tien.getIdTienda());
				for(int i = 0; i < pedidosProgramados.size(); i++)
				{
					String[] pedTemp = (String[]) pedidosProgramados.get(i);
					respuesta = respuesta + "<tr>"
					+  "<td>" + pedTemp[0] + "</td>"
					+  "<td>" + pedTemp[1] + "</td>"
					+  "<td>" + formatea.format(Double.parseDouble(pedTemp[2]))  + "</td>"
					+  "<td>" + pedTemp[3] + "</td>"
					+  "</tr>";
					if(i == 0)
					{
						strPedidosProg = strPedidosProg + pedTemp[0];
					}else
					{
						strPedidosProg = strPedidosProg + " , " + pedTemp[0];
					}
					
				}
				strPedidosProg = strPedidosProg + ")";
				System.out.println(strPedidosProg);
				respuesta = respuesta + "</table> <br/>";
				
				//Generamos una segunda tabla por tienda
				respuesta = respuesta + "<table border='2'><tr><td colspan='3'>" + tien.getNombreTienda() + "</td></tr>";
				respuesta = respuesta + "<tr>"
						+  "<td><strong>VENTA</strong></td>"
						+  "<td><strong>Cant Domiciliario Interno</strong></td>"
						+  "<td><strong>Cant Domiciliario Externos</strong></td>"
						+  "</tr>";
				//En este punto verificamos vendido de la tienda hasta el momento
				capaControladorPOS.PedidoCtrl pedCtrl = new capaControladorPOS.PedidoCtrl(false);
				double totalVentaDia = pedCtrl.obtenerTotalesPedidosDia(fechaActual, tien.getHosbd());
				BiometriaCtrl bioCtrl = new BiometriaCtrl(false);
				int cantDomInt = bioCtrl.cantidadEmpleadoDomiciliario(fechaActual, tien.getIdTienda());
				EmpleadoCtrl empCtrl  = new EmpleadoCtrl(false);
				int cantDomExt = empCtrl.consultarCantEmpleadoTempDia(fechaActual, tien.getHosbd());
				respuesta = respuesta + "<tr>"
						+  "<td>" + formatea.format(totalVentaDia) + "</td>"
						+  "<td>" + cantDomInt + "</td>"
						+  "<td>" + cantDomExt + "</td>"
						+  "</tr>";
				respuesta = respuesta + "</table> <br/>";
					
			}
		}
		return(respuesta);
	}
	
	public String generarStringHTMLReporteOperacion()
	{
		//Requerimos primero que todo obtener el rango de fechas con el fin de tener dicho rango para las consultas
		//Definimos el formato como manejaremos las fechas
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat dateFormatHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		DecimalFormat formatea = new DecimalFormat("###,###");
		//Traemos la fechaActual en Blanco
		String fechaActual = "";
		//Traemos la fecha actual en un date
		Date datFechaActual = new Date();
		//Comenzamos a traer la fecha actual como un String
		fechaActual = dateFormat.format(datFechaActual);
		//Con la fecha actual vamos a realizar la consulta de los pedidos
		
		//Necesitamos obtener la fecha hora actual menos una hora en el formato para consultar en mysql
		Calendar calendario = Calendar.getInstance();
		calendario.setTime(datFechaActual); 
		calendario.add(Calendar.HOUR, -1);
		Date datFechaMenosHora = calendario.getTime();
		String fechaActualMenosHora = dateFormatHora.format(datFechaMenosHora);
		
		
		//Realizamos la extracción de los tiempos pedidos
		String respuesta = "";
		boolean indicadorCorreo = false;
		//Recuperaremos las tiendas y empezaremos a ir consultando una a una las tiendas para extraer la información
		ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendas();
		//Vamos a recuperar de manera centralizada los valores de las variables de pedido en espera y pedido en ruta
		int pedidoEmpacado = ParametrosDAO.retornarValorNumerico("EMPACADODOMICILIO");
		int pedidoEnRuta = ParametrosDAO.retornarValorNumerico("ENRUTADOMICILIO");
		int tipoPedidoDomicilio = ParametrosDAO.retornarValorNumerico("TIPOPEDIDODOMICILIO");
		//Con los valores recuperados con anterioridad se realizará la consulta a cada una de las tiendas
		int cantPedCoc = 0;
		int cantPedEmp = 0;
		int cantPedPen = 0;
		int cantPedHoraDom = 0;
		int cantPedHoraNoDom = 0;
		String cantMinutos = "";
		String strPedidosProg = "";
		for(Tienda tien : tiendas)
		{
			strPedidosProg = "(";
			if(!tien.getHosbd().equals(new String("")))
			{
				respuesta = respuesta + "<table border='2'><tr><td colspan='5'>" + tien.getNombreTienda() + "</td></tr>";
				respuesta = respuesta + "<tr>"
						+  "<td><strong>Pedidos en COCINA</strong></td>"
						+  "<td><strong>Ped Pend Salir Tienda</strong></td>"
						+  "<td><strong>Cant de Ped Últ Hora Domicilio</strong></td>"
						+  "<td><strong>Cant de Ped Últ Hora No Domicilio</strong></td>"
						+  "<td><strong>Tiempo último Ped Pend</strong></td>"
						+  "</tr>";
				//Comenzamos a validar los parámetros de cada tienda 
				// LA MEJOR ESTRATEGIA SERÍA TENER UN SOLO MÉTODO PARA MEJORAR EL PERFORMANCE
				//Cantidad de pedidos en Cocina
				cantPedCoc = capaDAOPOS.PedidoDAO.obtenerCantidadPedidoCocina(fechaActual, tien.getHosbd());
				//Cantidad de pedidos pendientes por salir de la tienda
				cantPedEmp = capaDAOPOS.PedidoDAO.obtenerCantidadPedidoPorEstado(fechaActual, pedidoEmpacado, tien.getHosbd());
				//Cantidad de pedidos pendientes de la tienda
				cantPedPen =  cantPedEmp + capaDAOPOS.PedidoDAO.obtenerCantidadPedidoPorEstado(fechaActual, pedidoEnRuta, tien.getHosbd());
				//Cantidad de pedidos de la última hora Domicilio
				cantPedHoraDom = capaDAOPOS.PedidoDAO.obtenerCantidadPedidoDespuesHoraDomicilio(fechaActual, fechaActualMenosHora, tien.getHosbd(),tipoPedidoDomicilio );
				//Cantidad de pedidos de la última hora Domicilio
				cantPedHoraNoDom = capaDAOPOS.PedidoDAO.obtenerCantidadPedidoDespuesHoraNoDomicilio(fechaActual, fechaActualMenosHora, tien.getHosbd(),tipoPedidoDomicilio );
				//Tiempo del último pedimo por salir
				cantMinutos = capaDAOPOS.PedidoDAO.obtenerTiempoUltimoPedidoEstado(fechaActual, pedidoEmpacado, strPedidosProg,  tien.getHosbd());
				//Luego de obtenidos los datos pintamos el html
				respuesta = respuesta + "<tr>"
						+  "<td>" + cantPedCoc + "</td>"
						+  "<td>" + cantPedEmp + "</td>"
						+  "<td>" + cantPedHoraDom + "</td>"
						+  "<td>" + cantPedHoraNoDom + "</td>"
						+  "<td>" + cantMinutos + "</td>"
						+  "</tr>";
				respuesta = respuesta + "</table> <br/>";					
			}
		}
		return(respuesta);
	}
	
	
	public void actualizarTokenIntegracionCRM(String crm) throws IOException
	{
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion(crm);
		String strBody = "{\"client_id\": \"9a76a134-0966-4642-a16b-f953b40dfae9\",\n"
				+ "\"client_secret\": \"1Pp5phe3j9TbLInhGL39rrKDj0y3ZdbOzQPki86XD1IOOgC9HT2FAVc58086QqwU\",\n"
				+ "  \"grant_type\": \"refresh_token\",\n"
				+ "  \"refresh_token\": \"" + intCRM.getFreshToken() +"\",\n"
				+ "  \"redirect_uri\": \"https://tiendapizzaamericana.co/ProyectoPizzaAmericana/InsertarPedidoCRMBOT\"\n"
				+ "}";
		OkHttpClient client = new OkHttpClient();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, strBody );
		Request request = new Request.Builder()
		  .url("https://pizzaamericana.kommo.com/oauth2/access_token")
		  .post(body)
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
			String respuestaJSON = response.body().string();
			System.out.println(response.toString());
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(respuestaJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			String refreshToken = (String)jsonGeneral.get("refresh_token").toString();
			String accessToken = (String)jsonGeneral.get("access_token").toString();
			IntegracionCRMDAO.actualizarTokenIntegracionCRM(crm, accessToken, refreshToken);
		}catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	public static Map<String, String> separarURL(String query) throws UnsupportedEncodingException {
	    Map<String, String> valores = new LinkedHashMap<String, String>();
	    String[] pairs = query.split("&");
	    for (String pair : pairs) {
	        int idx = pair.indexOf("=");
	        valores.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
	    }
	    return valores;
	}
	
	
	
	public String insertarPedidoCRMBOT(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		String infLead = obtenerInformacionLeadCRM(lead);
		//System.out.println("información " + infLead);
		LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead,"I");
		procesarPedidoBOTCRM(infLead,lead, idLog);
		return(respuesta);
	}
	
	/**
	 * Método para atender desde el Servlet la consulta del estado de un pedido en el BOT CRM
	 * @param datos
	 * @param authHeader
	 * @return
	 * @throws IOException
	 */
	public String consultarPedidoCRMBOT(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		String infLead = obtenerInformacionLeadCRM(lead);
		//System.out.println("información " + infLead);
		LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead, "C");
		consultarPedidoCRMBOT(infLead,lead, idLog);
		return(respuesta);
	}
	
	/**
	 * Método para atender desde el Servlet la consulta del cobertura en el BOT CRM
	 * @param datos
	 * @param authHeader
	 * @return
	 * @throws IOException
	 */
	public String consultarCoberturaCRMBOT(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//ArcGISRuntimeEnvironment.setInstallDirectory("C:\\Program Files\\POSPM\\arcgis-runtime-sdk-java-100.15.0");
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		System.out.println("1. REVISIÓN LEAD " + lead);
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		String infLead = obtenerInformacionLeadCRM(lead);
		System.out.println("2. OBTUVIMOS EL LEAD  " + infLead);
		//System.out.println("información " + infLead);
		LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead, "T");
		consultarCoberturaCRMBOT(infLead,lead, idLog);
		return(respuesta);
	}
	
	/**
	 * Método que ejecuta el servicio para validar la cobertura de un pedido.
	 * @param datosJSON
	 * @param lead
	 * @param idLog
	 */
	public void consultarCoberturaCRMBOT(String datosJSON, String lead, int idLog)
	{
		String resultadoProceso = "";
		String direccion = "";
		String barrioMunicipio = "";
		String tipo_cliente ="";
		String referencia ="";
		//Para realizar el último parseo
		JSONParser parserFinal = new JSONParser();
		Object objParserFinal;
		//Aqui estamos verificando la utilización del valor
		JSONObject valor = new JSONObject();
		JSONArray valorArreglo = new JSONArray();
		String strValor = "";
		try
		{
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			JSONArray customFieldsArray = new JSONArray();
			try
			{
				String customFieldsValues = (String)jsonGeneral.get("custom_fields_values").toString();
				Object objcustomFieldsValues = parser.parse(customFieldsValues);
				customFieldsArray = (JSONArray) objcustomFieldsValues;
			}catch(Exception e1)
			{
				resultadoProceso = resultadoProceso + " Se tiene error dado que el LEAD no tiene los datos de pedido, posiblemente no es un LEAD de pedido de BOT o no están llenos los campos.";
				//Tratar problema de no tener campos adicionales
			}
			//Continuamos con la recolección de la información para el pedido
			for(int i = 0; i < customFieldsArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);
				String clave = objTemp.get("field_name").toString().toLowerCase();
				String values = objTemp.get("values").toString();
				Object objValues = parser.parse(values);
				JSONArray valuesArray = (JSONArray) objValues;
				objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
				valor = (JSONObject) objParserFinal;
				strValor = valor.get("value").toString();
				strValor = strValor.replaceAll("'", " ");
				//Dependiendo del campos se tendrá la recuperación del mismo
				if(clave.equals(new String("dirección de envío")) || clave.contains("direcci"))
				{
					direccion = strValor;
				}else if(clave.equals(new String("barrio y municipio")))
				{
					barrioMunicipio = strValor;
				}
				else if(clave.equals(new String("tipo de cliente")))
				{
					tipo_cliente = strValor;
				}
				else if(clave.equals(new String("referencia")))
				{
					referencia = strValor;
				}
			}
		}catch(Exception e)
		{
			
		}
		//
		UbicacionCtrl ubicaCtrl = new UbicacionCtrl();
	    String txtdirecc = direccion + " , " + barrioMunicipio+","+referencia+",Antioquia,Colombia";
		Resultado resultado = ubicaCtrl.ubicarDireccionEnTienda(txtdirecc,tipo_cliente);
		System.out.println("3. RESULTADO DEL PROCESO " +  resultado);
		actualizarCoberturaLeadCRMBOT(lead, resultado,tipo_cliente);
		System.out.println("6. TERMINO DEL PROCESO ");
	}
	
	public String insertarPedidoCRMBOTPoblado(String datos, String authHeader) throws IOException
	{
		String respuesta = ""; 
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		String infLead = obtenerInformacionLeadCRM(lead);
		//System.out.println("información " + infLead);
		LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead, "I");
		procesarPedidoBOTCRMPoblado(infLead,lead, idLog);
		return(respuesta);
	}
	
	
	
	public String limpiarLeadCRMBOT(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		limpiarLeadCRM(lead);
		return(respuesta);
	}
	
	
	public String insertarPQRSCRMBOT(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		String infLead = obtenerInformacionLeadCRM(lead);
		LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead, "P");
		procesarPQRSBOTCRM(infLead,lead, idLog);
		return(respuesta);
	}
	
	public String insertarFACCRMBOT(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader);
		//Vamos a realizar la extracción del parámetro
		String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());
		Map parSep = separarURL(parametrosDecode);
		String lead = (String)parSep.get("leads[status][0][id]");
		//Ya tenemos la información del LEAD, por lo tanto realizaremos la consulta de la información
		String infLead = obtenerInformacionLeadCRM(lead);
		LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead, "F");
		procesarFACBOTCRM(infLead,lead, idLog);
		return(respuesta);
	}
	
	
	/**
	 * Método creado para tomar los datos del JSON y crear el pedido en el sistema proveniente de RAPPI.
	 * @param datos
	 * @param authHeader
	 * @return
	 * @throws IOException
	 */
	public String insertarPedidoRAPPI(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogRAPPI(datos, authHeader);
		//Realizamos el procesamiento del Pedido
		insertarPedidoRAPPI(datos, idLog);
		return(respuesta);
	}
	
	public String insertarPedidoDIDI(String datos, String authHeader) throws IOException
	{
		String respuesta = "";
		//El primer paso a validar es la autorización para la utilización del servicio
		if(authHeader.equals(new String("PRUEBA")))
		{
			//Si viene el valor de prueba omitimos la validación
		}else
		{
			
		}
		//Realizamos la inserción de log con el JSON recibido
		int idLog = LogPedidoVirtualKunoDAO.insertarLogDIDI(datos, authHeader);
		//Realizamos el procesamiento del Pedido
		insertarPedidoDIDI(datos, idLog);
		return(respuesta);
	}
	
	public String obtenerInformacionLeadCRM(String lead) throws IOException
	{
		String datosLead = "";
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("KOMMO");
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURL = "https://pizzaamericana.kommo.com/api/v4/leads/"+lead;
		HttpGet request = new HttpGet(rutaURL);
		try
		{
			//Fijamos el header con el token
			request.setHeader("Authorization", "Bearer " + intCRM.getAccessToken());
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			datosLead = retorno.toString();
			
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
		return(datosLead);
	}
	
	public String obtenerInfoCampoLeadCRM(String idcampo) throws IOException
	{
		String datosLead = "";
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("KOMMO");
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURL = "https://pizzaamericana.kommo.com/api/v4/leads/custom_fields/"+idcampo;
		HttpGet request = new HttpGet(rutaURL);
		try
		{
			//Fijamos el header con el token
			request.setHeader("Authorization", "Bearer " + intCRM.getAccessToken());
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			datosLead = retorno.toString();
			
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
		return(datosLead);
	}
	
	public String limpiarLeadCRM(String lead) throws IOException
	{
		String datosLead = "";
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("KOMMO");
		
		//Para revisar
		String mensaje = "[\n"
				+ "    {   \"id\": " + lead + ",\n"
				+ "        \"custom_fields_values\": [\n"
				+ "        {\n"
				+ "            \"field_id\": 861855,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862081,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 857712,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 861771,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 861775,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862087,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 858274,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 858276,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862091,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862089,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 858272,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 857714,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862847,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862901,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862155,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"enum_id\": null\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862153,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"enum_id\": null\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 861773,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"field_id\": 862083,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "                {\n"
				+ "            \"field_id\": 864379,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "                {\n"
				+ "            \"field_id\": 863191,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "                {\n"
				+ "            \"field_id\": 863427,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "                {\n"
				+ "            \"field_id\": 865067,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        },\n"
				+ "                {\n"
				+ "            \"field_id\": 865069,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \"\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        }\n"
				+ "    ]\n"
				+ "    }\n"
				+ "]";
		OkHttpClient client = new OkHttpClient();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, mensaje );
		Request request = new Request.Builder()
		  .url("https://pizzaamericana.kommo.com/api/v4/leads")
		  .patch(body)
		  .addHeader("Authorization", "Bearer " + intCRM.getAccessToken())
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
			String respuestaJSON = response.body().string();
			System.out.println("1 " + response.toString());
			datosLead = respuestaJSON;
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
		return(datosLead);
	}
	
	/**
	 * Método que recibe la información capturada en el CRM BOT y realiza una inserción del pedido, teniendo las características
	 * propias del CRM en donde solo hay un producto y un posible acompañante.
	 * @param idPedido
	 * @param nombreDelCombo
	 * @param sabor1
	 * @param sabor2
	 * @param adicion
	 * @param bebida
	 * @param acompanamiento
	 * @param bebida2
	 * @param detalle
	 */
	public String insertarProductoBOTCRM(int idPedido, String nombreDelCombo, String sabor1, String sabor2, String adicion, String bebida, String acompanamiento, String bebida2, String detalle, int idTipoPedido)
	{
		ParametrosCtrl parCtrl = new ParametrosCtrl();
		//Información del domicilio
		int idProductoDomicilio = 0;
		long valorDomicilio = 0;
		int idEspecialidad = 0;
		int idEspecialidad2 = 0;
		int idSaborTipoLiquido = 0;
		int idSaborTipoLiquido2 = 0;
		//Realizamos la inserción del domicilio en caso de que el tipo pedido sea domicilio
		if(idTipoPedido == 1)
		{
			idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual("Valor del domicilio");
			valorDomicilio = 3000;
			DetallePedido detPedidoDomi = new DetallePedido(idProductoDomicilio,idPedido,1,0,0,valorDomicilio,valorDomicilio*1, "" , "" /*observacion*/, 0, 0, "", "");
			PedidoDAO.InsertarDetallePedido(detPedidoDomi);
		}
		//Tendremos un arreglo con las adiciones para este line_items
		ArrayList<AdicionTiendaVirtual> adiciones = new ArrayList();
		//Tendremos un arreglo con los condimentos
		ArrayList<AdicionTiendaVirtual> modificadoresCon = new ArrayList();
		//Creamos objeto de AdicionTiendaVirtual temporal
		AdicionTiendaVirtual adiTemp = new AdicionTiendaVirtual();
		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();
		//Procesamos nombre del combo
		int idProducto = 0;
		int idProductoCon = 0;
		String strCON = "";
		int idProductoAcompa = 0;
		int cantidad = 1;
		boolean esPromocion = false;
		int idProductoAdicion = 0;
		String strAdiciones = "";
		int idDetInser = 0;
		double valorUnitarioAdicion = 0;
		double valorUnitarioCon = 0;
		double valorUnitario = 0;
		double valorUnitario2 = 0;
		double valorAdicional = 0;
		double valorAdicional2 = 0;
		int idDetallePedido = 0;
		ExcepcionPrecio excepcionPrecioTemp = parCtrl.homologarExcepcionTiendaVirtual(nombreDelCombo);
		int idExcepcion = excepcionPrecioTemp.getIdExcepcion();
		double valorExcepcion = excepcionPrecioTemp.getPrecio();
		String log = "";
		//Hacemos la validación de si no se logró identificar dentro de los productos de precio menú 
		if(excepcionPrecioTemp.getIdExcepcion() != 0)
		{
			idProducto = excepcionPrecioTemp.getIdProducto();
			valorUnitario = valorExcepcion;
			esPromocion = true;
		}else
		{
			idProducto = parCtrl.homologarProductoTiendaVirtual(nombreDelCombo);
			valorUnitario = ProductoDAO.retornarProducto(idProducto).getPreciogeneral();
		}
		if(detalle.equals(new String("")))
		{
			
		}else if(detalle.equals(new String("una sola especialidad"))||detalle.equals(new String("pizza una sola especialidad")))
		{
			idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual("Pizza "+ sabor1);
			idEspecialidad2 = idEspecialidad;
			if(!sabor1.equals(new String("")))
			{
				if(idEspecialidad == 0) 
				{
					log = log + " Problema al homologar " + "Pizza " + sabor1 + "-";
				}
			}
		}else if(detalle.equals(new String("mitad y mitad"))||detalle.equals(new String("pizza mitad y mitad")))
		{
			idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual("Pizza "+ sabor1);
			idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual("Pizza "+ sabor2);
			if(!sabor1.equals(new String("")))
			{
				if(idEspecialidad == 0) 
				{
					log = log + " Problema al homologar " + "Pizza " + sabor1 + "-";
				}
			}
			if(!sabor2.equals(new String("")))
			{
				if(idEspecialidad2 == 0) 
				{
					log = log + " Problema al homologar " + "Pizza " + sabor1 + "-";
				}
			}
		}
		//Procesamos las adiciones
		if(!adicion.equals(new String("")))
		{
			idProductoAdicion = parCtrl.homologarProductoTiendaVirtual(adicion);
			strAdiciones = strAdiciones + " " + idProductoAdicion + "-" + adicion;
			valorUnitarioAdicion = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
			DetallePedido detPedidoAdicion = new DetallePedido(idProductoAdicion,idPedido,1,0,0,valorUnitarioAdicion,valorUnitarioAdicion, adicion , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/, "", "");
			idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoAdicion);
			adiTemp = new AdicionTiendaVirtual();
			adiTemp.setCantidad(1);
			adiTemp.setIdProductoAdicion(idProductoAdicion);
			adiTemp.setIdDetallePedido(idDetInser);
			adiciones.add(adiTemp);
			if(!adicion.equals(new String("")))
			{
				if(idProductoAdicion == 0) 
				{
					log = log + " Problema al homologar " + adicion + "-";
				}
			}
		}
		//Procesamos los modificadores CON solo se hará en promociones, dado que en precio menú no tenemos arma tu pizza
		if(esPromocion && idEspecialidad == 0)
		{
			String comTamano = "";
			if(nombreDelCombo.equals(new String("Pizzeta (4 porciones)")))
			{
				comTamano = "PIZ";
			}else if(nombreDelCombo.equals(new String("Mediana (6 porciones)")))
			{
				comTamano = "MED";
			}else if(nombreDelCombo.equals(new String("Grande (8 porciones)")))
			{
				comTamano = "GRA";
			}else if(nombreDelCombo.equals(new String("Extragrande (12 porciones)")))
			{
				comTamano = "EXT";
			}else if(nombreDelCombo.equals(new String("COMBO DELI GRANDE")))
			{
				comTamano = "GRA";
			}else if(nombreDelCombo.equals(new String("PROMO MEDIANA PLUS")))
			{
				comTamano = "MED";
			}else if(nombreDelCombo.equals(new String("COMBO PARA TODOS")))
			{
				comTamano = "EXT";
			}else if(nombreDelCombo.equals(new String("Pizzeta promo poblado")))
			{
				comTamano = "PIZ";
			}
			//Realizamos la labor con el Sabor1
			idProductoCon = parCtrl.homologarProductoTiendaVirtual("CON " + sabor1 + " " + comTamano);
			if(idProductoCon !=0)
			{
				strCON = strCON + " " + "CON " + sabor1;
				valorUnitarioCon = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
				if(valorUnitarioCon > 0)
				{
					DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitarioCon,valorUnitarioCon*cantidad, "CON " + sabor1 , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
					idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
				}
				adiTemp = new AdicionTiendaVirtual();
				adiTemp.setCantidad(1);
				adiTemp.setIdProductoAdicion(idProductoCon);
				adiTemp.setIdDetallePedido(idDetInser);
				adiTemp.setPosicionPizza(1);
				modificadoresCon.add(adiTemp);
			}
			//Realizamos la labor cn el Sabor 2
			idProductoCon = parCtrl.homologarProductoTiendaVirtual("CON " + sabor2 + " " + comTamano);
			if(idProductoCon !=0)
			{
				strCON = strCON + " " + "CON " + sabor2;
				valorUnitarioCon = ProductoDAO.retornarProducto(idProductoCon).getPreciogeneral();
				if(valorUnitarioCon > 0)
				{
					DetallePedido detPedidoCon = new DetallePedido(idProductoCon,idPedido,cantidad,0,0,valorUnitarioCon,valorUnitarioCon*cantidad, "CON " + sabor2 , "" /*observacion*/,  0 /*idSaborTipoLiquido*/, 0 /*idexcepcion*/,"", "");
					idDetInser = PedidoDAO.InsertarDetallePedido(detPedidoCon);
				}
				adiTemp = new AdicionTiendaVirtual();
				adiTemp.setCantidad(1);
				adiTemp.setIdProductoAdicion(idProductoCon);
				adiTemp.setIdDetallePedido(idDetInser);
				adiTemp.setPosicionPizza(1);
				modificadoresCon.add(adiTemp);
			}
		}
		//Recordar que los valores adicionales se dan si no estan dentro de promociones
		if(idExcepcion == 0)
		{
			if(idEspecialidad > 0  && idEspecialidad2 == 0)
			{
				valorAdicional = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto);
			}else if(idEspecialidad > 0 && idEspecialidad2 > 0)
			{
				valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto))/2;
				valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto))/2;
			}	
			valorUnitario = valorUnitario + valorAdicional + valorAdicional2;
		}else if(idExcepcion > 0)
		{
			if(idEspecialidad > 0  && idEspecialidad2 == 0)
			{
				valorAdicional = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto);
				valorUnitario = valorUnitario + valorAdicional;
			}else if(idEspecialidad > 0 && idEspecialidad2 > 0)
			{
				
				valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto))/2;
				valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto))/2;
				valorUnitario = valorUnitario + valorAdicional + valorAdicional2;	
			}	
		}
		//Homologamos la bebida
		if(esPromocion)
		{
			idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual("Selecciona tu bebida " + bebida + " Extragrande (12 porciones)");
		}
		else
		{
			idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual("Selecciona tu bebida " + bebida + " " + nombreDelCombo);
		}
		DetallePedido detPedido = new DetallePedido(idProducto,idPedido,cantidad,idEspecialidad,idEspecialidad2,valorUnitario,valorUnitario*cantidad, strAdiciones , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, strCON, "");
		idDetallePedido = PedidoDAO.InsertarDetallePedido(detPedido);
		//Revisaremos si hay productos incluidos para agregar
		for(int j = 0; j < productosIncluidos.size(); j++)
		{
			ProductoIncluido proIncTemp = productosIncluidos.get(j);
			if(proIncTemp.getIdproductopadre() == idProducto)
			{
				//Realizamos una modificación para que la cantidad a incluir se debe multiplicar por la cantidad de productos adicionados
				DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad()*cantidad,0,0,0,0, "" , "Producto Incluido-"+idDetallePedido, 0, 0 /*idexcepcion*/, "", "");
				PedidoDAO.InsertarDetallePedido(detPedidoInc);
			}
		}
		//Revisaremos los modificadores con
		for(int j = 0; j < modificadoresCon.size(); j++)
		{
			AdicionTiendaVirtual modTempIns = modificadoresCon.get(j);
			ModificadorDetallePedido modDetPedido = new ModificadorDetallePedido(0,idDetallePedido, modTempIns.getIdProductoAdicion(), 0, modTempIns.getCantidad(), modTempIns.getIdDetallePedido());
			PedidoDAO.InsertarModificadorDetallePedido(modDetPedido);
		}
		//Revisaremos las adiciones
		for(int j = 0; j < adiciones.size(); j++)
		{
			if(idEspecialidad > 0 && idEspecialidad2 >0)
			{
				AdicionTiendaVirtual adiTempIns = adiciones.get(j);
				DetallePedidoAdicion detPedidoAdiTemp = new DetallePedidoAdicion(idDetallePedido, adiTempIns.getIdDetallePedido(), idEspecialidad, 0, 0.5 , 0);
				PedidoDAO.InsertarDetalleAdicion(detPedidoAdiTemp);
				detPedidoAdiTemp = new DetallePedidoAdicion(idDetallePedido, adiTempIns.getIdDetallePedido(), 0 , idEspecialidad2, 0 , 0.5);
				PedidoDAO.InsertarDetalleAdicion(detPedidoAdiTemp);
			}else
			{
				AdicionTiendaVirtual adiTempIns = adiciones.get(j);
				DetallePedidoAdicion detPedidoAdiTemp = new DetallePedidoAdicion(idDetallePedido, adiTempIns.getIdDetallePedido(), 0, 0, 1 , 0);
				PedidoDAO.InsertarDetalleAdicion(detPedidoAdiTemp);
			}
		}
		//Verificamos el estado del acompañante si se pidió
		if(acompanamiento.equals(new String("")))
		{
			
		}else
		{
			//No necesariamente si solo es promoción también se debe validar si es combo para todos
			if(esPromocion && nombreDelCombo.equals(new String("COMBO PARA TODOS")))
			{
				idProductoAcompa = parCtrl.homologarProductoTiendaVirtual("Producto Adicional " + acompanamiento);
			}else
			{
				idProductoAcompa = parCtrl.homologarProductoTiendaVirtual(acompanamiento);
			}
			if(idProductoAcompa != 0)
			{
				double valorUnitarioAco = ProductoDAO.retornarProducto(idProductoAcompa).getPreciogeneral();
				idSaborTipoLiquido2 = parCtrl.homologarLiquidoTiendaVirtual("Selecciona tu bebida " + bebida2 + " Pizzeta (4 porciones)");
				DetallePedido detPedidoAcom = new DetallePedido(idProductoAcompa,idPedido,1,0,0,valorUnitarioAco,valorUnitarioAco, "" , "" /*observacion*/, idSaborTipoLiquido2, 0, "", "");
				idDetallePedido = PedidoDAO.InsertarDetallePedido(detPedidoAcom);
				//Realizamos ciclo de productos incluidos
				for(int j = 0; j < productosIncluidos.size(); j++)
				{
					ProductoIncluido proIncTemp = productosIncluidos.get(j);
					if(proIncTemp.getIdproductopadre() == idProductoAcompa)
					{
						//Realizamos una modificación para que la cantidad a incluir se debe multiplicar por la cantidad de productos adicionados
						DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad(),0,0,0,0, "" , "Producto Incluido-"+idDetallePedido, 0, 0 /*idexcepcion*/, "", "");
						PedidoDAO.InsertarDetallePedido(detPedidoInc);
					}
				}
			}
		}
		return(log);
	}
	
	/**
	 * Método para realizar la inserción del pedido en el sistem proveniente del CRM
	 * @param datosJSON
	 * @param lead
	 * @param idLog
	 */
	public void procesarPedidoBOTCRM(String datosJSON, String lead, int idLog)
	{
		String resultadoProceso = "";
		String obserProceso = "";
		String asesor = "";
		String nombreCliente = "";
		String telefono = "";
		String correo = "";
		String nombreDelCombo = "";
		String detalles = "";
		String sabor1 = "";
		String sabor2 = "";
		String adicion = "";
		String bebida = "";
		String acompanamiento = "";
		String bebida2 = "";
		String formaPago ="";
		String direccion ="";
		String referencia ="";
		String tienda ="";
		int idTienda = 0;
		String horaPedido = "";
		String fechaProgramado = "";
		//Para realizar el último parseo
		JSONParser parserFinal = new JSONParser();
		Object objParserFinal;
		//Aqui estamos verificando la utilización del valor
		JSONObject valor = new JSONObject();
		JSONArray valorArreglo = new JSONArray();
		String strValor = "";
		try
		{
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			JSONArray customFieldsArray = new JSONArray();
			try
			{
				String customFieldsValues = (String)jsonGeneral.get("custom_fields_values").toString();
				Object objcustomFieldsValues = parser.parse(customFieldsValues);
				customFieldsArray = (JSONArray) objcustomFieldsValues;
			}catch(Exception e1)
			{
				resultadoProceso = resultadoProceso + " Se tiene error dado que el LEAD no tiene los datos de pedido, posiblemente no es un LEAD de pedido de BOT o no están llenos los campos.";
				//Tratar problema de no tener campos adicionales
			}
			//Continuamos con la recolección de la información para el pedido
			for(int i = 0; i < customFieldsArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);
				String clave = objTemp.get("field_name").toString().toLowerCase();
				String values = objTemp.get("values").toString();
				Object objValues = parser.parse(values);
				JSONArray valuesArray = (JSONArray) objValues;
				if(clave.equals(new String("adicion")))
				{
					objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
					//Necesitamos probar una pedido con varias adiciones
					//valorArreglo = (JSONArray) objParserFinal;
					valor = (JSONObject) objParserFinal;
				}else
				{
					objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
					valor = (JSONObject) objParserFinal;
				}
				strValor = valor.get("value").toString();
				strValor = strValor.replaceAll("'", " ");
				//Dependiendo del campos se tendrá la recuperación del mismo
				if(clave.equals(new String("asesor que atiende")))
				{
					asesor = strValor;
					asesor = IntegracionCRMHomologaAsesorDAO.obtenerHomologacionAsesorCRM(asesor);
				}else if(clave.equals(new String("nombre cliente"))||clave.equals(new String("Nombre del cliente")))
				{
					nombreCliente = strValor;
				}else if(clave.equals(new String("numero de teléfono")))
				{
					telefono  = strValor;
				}else if(clave.equals(new String("correo electrónico")) || clave.equals(new String("correo electrónico pqrs")))
				{
					correo = strValor;
				}else if(clave.equals(new String("nombre del combo")))
				{
					nombreDelCombo = strValor;
				}else if(clave.equals(new String("detalles")))
				{
					detalles = strValor.toLowerCase();
				}else if(clave.equals(new String("sabor 1")))
				{
					sabor1  = strValor;
				}else if(clave.equals(new String("sabor 2")))
				{
					sabor2 = strValor;
				}else if(clave.equals(new String("adicion")))
				{
					//En este punto si podrán ser varios
					adicion = strValor;
					
				}else if(clave.equals(new String("bebida")))
				{
					bebida = strValor;
				}else if(clave.equals(new String("acompañamiento")))
				{
					acompanamiento = strValor;
				}else if(clave.equals(new String("bebida 2")))
				{
					bebida2 = strValor;
				}else if(clave.equals(new String("forma de pago")))
				{
					formaPago = strValor;
				}else if(clave.equals(new String("dirección de envío")))
				{
					direccion = strValor;
				}else if(clave.equals(new String("referencia")))
				{
					referencia = strValor;
				}else if(clave.equals(new String("tienda")))
				{
					tienda = strValor;
					TiendaCtrl tiendaCtrl = new TiendaCtrl();
					idTienda = TiendaDAO.obteneridTienda(tienda);
					
				}else if(clave.equals(new String("hora pedido")))
				{
					horaPedido = strValor;
				}else if(clave.equals(new String("fecha pedido")))
				{
					fechaProgramado = strValor;
				}
			}
			if(idTienda == 0)
			{
				idTienda = 1;
			}
			//Realizamos procesamiento de telefono
			telefono = telefono.replace(" ", "");
			telefono = telefono.replace("-", "");
			//Procedemos a crear el pedido en el sistema con base en como lo hemos creado para la tienda virtual
			String telefonoCelular = "";
			//El telefono le quitaremos el indicativo del país
			if(telefono.substring(0, 3).equals(new String("+57")))
			{
				telefono = telefono.substring(3);
			}
			//Para el caso del teléfono validaremos si es un fijo
			if(telefono.trim().length() == 7)
			{
				//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
				//contact center.
				telefono = "604" + telefono;
			}else
			{
				telefonoCelular = telefono;
			}
			double latitud = 0, longitud = 0;
			if(latitud== 0 && longitud == 0)
			{
				UbicacionCtrl  ubicacion = new UbicacionCtrl();
				Ubicacion ubicaResp = ubicacion.ubicarDireccionEnTiendaBatch(direccion + " " + referencia + " Medellín");
				latitud = ubicaResp.getLatitud();
				longitud = ubicaResp.getLongitud();
			}
			ParametrosCtrl parCtrl = new ParametrosCtrl();
			int idFormaPago = parCtrl.realizarHomologacionFormaPagoTiendaVirtual(formaPago);
			if(idFormaPago == 0)
			{
				idFormaPago = 1;
				obserProceso = obserProceso + " " + "No se logro homologar la forma de pago del bot, por favor revisar";
			}
			Cliente clienteVirtual = new Cliente(0, telefono, nombreCliente, direccion, "", referencia,"", idTienda);
			clienteVirtual.setEmail(correo);
			clienteVirtual.setIdtienda(idTienda);
			clienteVirtual.setLatitud((float)latitud);
			clienteVirtual.setLontitud((float)longitud);
			clienteVirtual.setTelefonoCelular(telefonoCelular);
			clienteVirtual.setPoliticaDatos("S");
			
			//Realizamos la validación del cliente con toda la lógica en la capa Controladora de Cliente
			ClienteCtrl clienteCtrl = new ClienteCtrl();
			//Aprovecharemos que los objetos se pasan como valores por referencia por lo tanto las modificaciones realizadas al objeto tendrán mucho que ver
			int idCliente = clienteCtrl.validarClienteTiendaVirtualKuno(clienteVirtual, "");
			if(idCliente == 0)
			{
				idCliente = 1;
				obserProceso = obserProceso + " " + "No se logro crear o actualizar el cliente, se debe revisar el pedido para asignar el cliente correctamente.";
			}
			//Vamos a proceser la fuente del pedido
			String fuentePedido = "CRM-BOT";
			//Validamos la fecha del pedido si es programado
			Date fechaPedido = new Date();
			DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
			String strFechaFinal = formatoFinal.format(fechaPedido);
			fechaPedido = formatoFinal.parse(strFechaFinal);
			//Validamos la fecha del pedido si es programado
			if(!fechaProgramado.equals(new String("")))
			{
				//Realizar modificaciones o validaciones de la fecha
				boolean errorFecha = false;
				fechaProgramado = reemplazarFechaProgCRMBOT(fechaProgramado);
				Date datFechPro = new Date();
				//Realizar validacion que la fecha si se pueda convertir
				try {
					//Validamos si se puede hacer la conversión
					datFechPro = formatoFinal.parse(fechaProgramado);
				}catch(Exception e)
				{
					//En caso de error prenderemos el indicador 
					errorFecha = true;
				}
				//Validaremos que la fecha de programación no sea menor a la fecha actual
				if(datFechPro.before(fechaPedido))
				{
					errorFecha = true;
				}
				if(!errorFecha)
				{
					strFechaFinal = fechaProgramado;
				}else
				{
					//Se envia correo con la notificacion de que se tuvo error con la fecha y que igual el pedido
					//se enviará y con fecha actual
					Correo correoError = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
					ArrayList correos = new ArrayList();
					correoError.setAsunto("Tuvimos Novedad en la Creación de tu Pedido  ");
					String correoEle = "jubote1@gmail.com";
					correos.add(correoEle);
					correos.add("pqrs@pizzaamericana.com.co");
					correos.add("lidercontactcenter@pizzaamericana.com.co");
					correos.add(correo);
					correoError.setContrasena(infoCorreo.getClaveCorreo());
					correoError.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
					correoError.setMensaje(" Querido Cliente " + nombreCliente + " tuvimos una novedad en la creación de tu pedido, no entendimos la fecha ingresada, por lo tanto creamos el pedido para el día de hoy, en caso de que no sea así por favor comunicate con nosotros a la linea 604 4444553.");
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correoError, correos);
					contro.enviarCorreo();
				}
				
			}
			int idTipoPedido = 1;
			int idPedido = PedidoDAO.InsertarEncabezadoPedidoTiendaVirtualKuno(idTienda, idCliente, strFechaFinal, asesor, Integer.parseInt(lead), idTipoPedido, "CRM", fuentePedido);
			//Realizamos la inserción del producto ordenado
			String log = insertarProductoBOTCRM(idPedido,nombreDelCombo, sabor1, sabor2, adicion, bebida, acompanamiento,  bebida2, detalles, idTipoPedido);
			LogPedidoVirtualKunoDAO.actualizarLogCRMBOTInfLog(idLog, log);
			//Luego de insertar el pedido haremos las últimas validaciones
			//Posteriormente realizamos los pasos para la finalización del pedido
			int tiempoPedido = TiempoPedidoDAO.retornarTiempoPedidoTienda(idTienda);
			//Consultaremos el tiempo que la tienda está dando en el momento
			long valorTotalContact = PedidoDAO.calcularTotalNetoPedido(idPedido);
			String horaProgramado = "AHORA";
			String pedidoProgramado = "N";
			if(!horaPedido.equals(new String("")))
			{
				horaProgramado = horaPedido;
				pedidoProgramado = "S";
			}
			int idEstadoPedido = 2;
			//Realizamos un cambio temporal para evitar las diferencias pero igual seguirán llegando los correos
			FinalizarPedidoTiendaVirtual(idPedido, idFormaPago, idCliente, tiempoPedido, "S", 0, "DESCUENTOS GENERALES DIARIOS", (valorTotalContact), pedidoProgramado, horaProgramado, idEstadoPedido);
			//Intervenimos cuando el idFormaPago es igual a 4 es porque es WOMPI y realizaremos el envío del link del pedido para pago al cliente
			if(idFormaPago == 4)
			{
				verificarEnvioLinkPagos(idPedido, clienteVirtual, valorTotalContact, idTienda);
			}
		}catch(Exception e)
		{
			//Trabajar excepción de que no se pudo obtener información del LEAD y no se pudo crear
			resultadoProceso = resultadoProceso + " Se tiene error dado que el LEAD no tiene los datos de pedido, posiblemente no es un LEAD de pedido de BOT o no están llenos los campos.";
		}
	}
	
	/**
	 * Método inicial que es llamado por el servicio, obtiene la información del lead y trae la información del pedido
	 * @param datosJSON
	 * @param lead
	 * @param idLog
	 */
	public void consultarPedidoCRMBOT(String datosJSON, String lead, int idLog)
	{
		String resultadoProceso = "";
		String telefono = "";
		//Para realizar el último parseo
		JSONParser parserFinal = new JSONParser();
		Object objParserFinal;
		//Aqui estamos verificando la utilización del valor
		JSONObject valor = new JSONObject();
		JSONArray valorArreglo = new JSONArray();
		String strValor = "";
		try
		{
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			JSONArray customFieldsArray = new JSONArray();
			try
			{
				String customFieldsValues = (String)jsonGeneral.get("custom_fields_values").toString();
				Object objcustomFieldsValues = parser.parse(customFieldsValues);
				customFieldsArray = (JSONArray) objcustomFieldsValues;
			}catch(Exception e1)
			{
				resultadoProceso = resultadoProceso + " Se tiene error dado que el LEAD no tiene los datos de pedido, posiblemente no es un LEAD de pedido de BOT o no están llenos los campos.";
				//Tratar problema de no tener campos adicionales
			}
			//Continuamos con la recolección de la información para el pedido
			for(int i = 0; i < customFieldsArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);
				String clave = objTemp.get("field_name").toString().toLowerCase();
				String values = objTemp.get("values").toString();
				Object objValues = parser.parse(values);
				JSONArray valuesArray = (JSONArray) objValues;
				objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
				valor = (JSONObject) objParserFinal;
				strValor = valor.get("value").toString();
				strValor = strValor.replaceAll("'", " ");
				//Dependiendo del campos se tendrá la recuperación del mismo
				if(clave.equals(new String("numero de teléfono")) || clave.contains("numero de tel"))
				{
					telefono = strValor;
				}
			}
		}catch(Exception e)
		{
			
		}
		//
		String mensaje = obtenerMensajePedidoBOTCRM(telefono);
		actualizarEstadoPedidoLeadCRMBOT(lead, mensaje);
	}
	
	/**
	 * Método que con el télefono se encargará de devolver el texto indicando el estado del pedido que se viene consultando.
	 * @param telefono
	 * @return
	 */
	public String obtenerMensajePedidoBOTCRM(String telefono)
	{
		String mensaje = "";
		//Una vez recuperado el teléfono realizamos las acciones correspondientes
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		//Con el número de teléfono y fecha realizamos la consulta del pedido
		int cantPedidos = PedidoDAO.validarTelefonoPedido(telefono);
		if(cantPedidos > 1)
		{
			mensaje = "Para el día de hoy, hay más de un pedido registrado con este número.";
		}else if(cantPedidos == 0)
		{

			mensaje = "Estimado Cliente! Para el día de hoy, no hay ningún pedido registrado con este número teléfonico (" + telefono +")";

		}else if(cantPedidos == 1)
		{
			//Realizamos la consulta del pedido para saber de que tienda
			//Deberemos de conocer si es pago virtual por las diferentes condiciones
			boolean esPagoVirtual = false;
			Pedido pedConsultado = PedidoDAO.ConsultaPedido(telefono);
			if(pedConsultado.getIdformapago() == 4)
			{
				esPagoVirtual = true;
			}
			//Teniendo la información del pedido deberemos de consultar el estado del pedido para lo cual vamos a consumir servicio a la tienda
			//Recuperamos la tienda que requerimos trabajar con el servicio
			HttpClient client = HttpClientBuilder.create().build();
			if(pedConsultado.getNumposheader() > 0)
			{
				Tienda tienda = TiendaDAO.obtenerTienda(pedConsultado.getTienda().getIdTienda());
				if (tienda != null)
				{
					//Realizar invocación de servicio en tienda
					String rutaURL = tienda.getUrl() + "ConsultarEstadoPedido?idpedidotienda=" + pedConsultado.getNumposheader();
					HttpGet request = new HttpGet(rutaURL);
					try
					{
						StringBuffer retorno = new StringBuffer();
						StringBuffer retornoTienda = new StringBuffer();
						//Se realiza la ejecución del servicio de finalizar pedido
						HttpResponse responseFinPed = client.execute(request);
						BufferedReader rd = new BufferedReader
							    (new InputStreamReader(
							    		responseFinPed.getEntity().getContent()));
						String line = "";
						while ((line = rd.readLine()) != null) {
							    retorno.append(line);
							}
						System.out.println(retorno.toString());
						String strRetorno = retorno.toString();
						JSONParser parser = new JSONParser();
						Object objParser = parser.parse(strRetorno);
						JSONObject jsonResServicio= (JSONObject) objParser;
						String estado = (String)jsonResServicio.get("estadopedido");
						if(estado.contains("En Espera"))
						{
							estado = "ya está listo y en espera de un domiciliario.";
						}else if(estado.contains("En Elaboración"))
						{
							estado = "ya fue aceptado en tienda y en unos minutos ingresará a nuestros hornos.";
						}else if(estado.contains("En Ruta"))
						{
							estado = "ya está con tu domiciliario y se encuentra en ruta.";
						}else if(estado.contains("Entregado"))
						{
							estado = "ya fue entregado en la dirección " + pedConsultado.getDireccion() ;
						}
						String fechaDesde = (String)jsonResServicio.get("fechadesde");
						//Cuando el mensaje esta vacio en ambos es porque no encontró
						if(estado.equals(new String("")) && fechaDesde.equals(new String("")))
						{

							mensaje = "No se encontrá información del pedido, por favor llama a nuestras lineas de atención 604 4444553";
						}else
						{
							if(estado.contains("En Espera"))
							{
								mensaje = "*Su pedido número " + pedConsultado.getIdpedido() + " a nombre de " + pedConsultado.getNombrecliente() + ". " + estado + ".*";
							}else
							{
								mensaje = "*Su pedido número " + pedConsultado.getIdpedido() + " a nombre de " + pedConsultado.getNombrecliente() + ". " + estado + " desde la siguiente fecha y hora " + fechaDesde + ".*";
							}

						}	
					}catch(Exception e)
					{
						mensaje = "No se encontró información para su solicitud por favor contactar a un asesor.";
						System.out.println(e.toString());
					}
				}
			}
			//Validamos si el pago es virtual para incluir más información al mensaje
			String mensajePagoVirtual = "";
			if(esPagoVirtual)
			{
				mensajePagoVirtual = " Recuerde que su forma de pago es PAGO VIRTUAL y su pedido es enviado a elaborar una vez recibimos notificación automática de su pago. Su pedido ingresó a las " + pedConsultado.getFechainsercion();
				if(pedConsultado.getFechaPagoVirtual() == null || pedConsultado.getFechaPagoVirtual().equals(new String("")) ||pedConsultado.getFechaPagoVirtual().equals(new String("null")))
				{
					mensajePagoVirtual = mensajePagoVirtual + " y aún no ha sido pagado.";
				}else
				{
					mensajePagoVirtual = mensajePagoVirtual + " y fue pagado a las " + pedConsultado.getFechaPagoVirtual() + " y enviado a elaborar a las " + pedConsultado.getFechaFinalizacion();
				}
				mensaje = mensaje + mensajePagoVirtual;
			}
		}
		
		return(mensaje);
	}
	
	
	/**
	 * Método que se encarga de realizar la actualización de la información de los datos adicionales del lead para mostrar
	 * en el bot el resultado de la información
	 * @param lead
	 * @param mensaje
	 * @throws IOException
	 */
	public void actualizarEstadoPedidoLeadCRMBOT(String lead, String mensaje)
	{
		String datosLead = "";
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("KOMMO");
		//Para revisar
		String datos = "[\n"
				+ "    {   \"id\": "+ lead +",\n"
				+ "        \"custom_fields_values\": [\n"
				+ "                {\n"
				+ "            \"field_id\":864379,\n"
				+ "            \"values\": [\n"
				+ "                {\n"
				+ "                    \"value\": \" " + mensaje + "\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        }\n"
				+ "    ]\n"
				+ "    }\n"
				+ "]";
		OkHttpClient client = new OkHttpClient();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, datos );
		Request request = new Request.Builder()
		  .url("https://pizzaamericana.kommo.com/api/v4/leads")
		  .patch(body)
		  .addHeader("Authorization", "Bearer " + intCRM.getAccessToken())
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
			String respuestaJSON = response.body().string();
			System.out.println("1 " + response.toString());
			datosLead = respuestaJSON;
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
			
	}
	
	
	public void actualizarCoberturaLeadCRMBOT(String lead, Resultado mensaje,String tipo_cliente) {
		String datosLead = "";
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("KOMMO");
		String datos="";
		String txtmensaje=mensaje.getResultado();
		
		if(tipo_cliente.toLowerCase().equals("programado")) {
			String  idcampo_asesor = obtenerCampoSeleccionCRM("862155","PROGRAMADO BOT SAM");
			String  idcampo_tienda = obtenerCampoSeleccionCRM("862153",mensaje.getInfoAdicional());
			
			if(mensaje.getResultado() == null) {
				txtmensaje="";
			}

			datos = "[\r\n"
					+ "    {   \"id\":"+ lead +",\r\n"
					+ "        \"custom_fields_values\": [\r\n"
					+ "        {\r\n"
					+ "            \"field_id\": 863191,\r\n"
					+ "            \"values\": [\r\n"
					+ "                {\r\n"
					+ "                    \"value\":\""+ mensaje.getResultado() +"\"\r\n"
					+ "                }\r\n"
					+ "            ]\r\n"
					+ "        },\r\n"
					+ "        {\r\n"
					+ "            \"field_id\": 862155,\r\n"
					+ "            \"values\": [\r\n"
					+ "                {\r\n"
					+ "                    \"enum_id\":"+idcampo_asesor+"\r\n"
					+ "                }\r\n"
					+ "            ]\r\n"
					+ "        },\r\n"
					+ "             {\r\n"
					+ "            \"field_id\": 862153,\r\n"
					+ "            \"values\": [\r\n"
					+ "                {\r\n"
					+ "                    \"enum_id\":"+idcampo_tienda+"\r\n"
					+ "                }\r\n"
					+ "            ]\r\n"
					+ "        }\r\n"
					+ "        ]\r\n"
					+ "    }\r\n"
					+ "    \r\n"
					+ "]";
		}else {
			//Para revisar
			String  idcampo_tienda = obtenerCampoSeleccionCRM("862153",mensaje.getInfoAdicional());
			 
			 datos = "[{\"id\":"+ lead +",\n"
			 		+ "  \"custom_fields_values\": [\n"
			 		+ "    {\"field_id\": 863191,\n"
			 		+ "      \"values\": [\n"
			 		+ "        {\"value\":\""+ mensaje.getResultado() +"\"}\n"
			 		+ "      ]\n"
			 		+ "    },\n"
			 		+ "    {\"field_id\": 862153,\n"
			 		+ "      \"values\": [\n"
			 		+ "        {\"enum_id\":"+idcampo_tienda+"}\n"
			 		+ "      ]\n"
			 		+ "    }\n"
			 		+ "  ]\n"
			 		+ "}]";
			}
			
			
		OkHttpClient client = new OkHttpClient();
		System.out.println("4. LOS DATOS ENVIADOS PARA ACTUALIZACIÓN  " + datos);
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json; charset=utf-8");
		//byte[] datosBytes = datos.getBytes(StandardCharsets.UTF_8);
		RequestBody body = RequestBody.create(mediaType,datos);
			System.out.println("5. Clave  " + intCRM.getAccessToken());
			System.out.println("5.1. body  " + body);



		System.out.println("Request Body: " + bodyToString(body));
		Request request = new Request.Builder()
		  .url("https://pizzaamericana.kommo.com/api/v4/leads")
		  .patch(body)
		  .addHeader("Authorization", "Bearer " + intCRM.getAccessToken())
		  .build();
		try
		{
				okhttp3.Response response = client.newCall(request).execute();
				System.out.println("Response Code: " + response.code());
				System.out.println("Response Headers: " + response.headers());
				String respuestaJSON = response.body().string();
				System.out.println("Response Body: " + respuestaJSON);
				datosLead = respuestaJSON;
		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
			
	}
	
	private static String bodyToString(final RequestBody request) {
	    try {
	        final Buffer buffer = new Buffer();
	        if (request != null) {
	            request.writeTo(buffer);
	        } else {
	            return "";
	        }
	        return buffer.readUtf8();
	    } catch (final IOException e) {
	        return "Couldn't convert request body to string";
	    }
	}
	
	public String  obtenerCampoSeleccionCRM(String idcampo,String valorDeseado ){
		String id = "";
		try {
			String infoCamp = obtenerInfoCampoLeadCRM(idcampo);
            ObjectMapper objectMapper = new ObjectMapper();
            // Convertir el string JSON a un objeto JsonNode
            JsonNode jsonNode = objectMapper.readTree(infoCamp);
            // Obtener el valor de "enums"
            if (jsonNode.has("enums")) {
            JsonNode enumsNode = jsonNode.get("enums");
            // Verificar si "enums" no es nulo
            if (enumsNode != null) {
                // Imprimir el valor de "enums"
                for (JsonNode enumNode : enumsNode) {
                    // Obtener el valor de "value"
                    String value = enumNode.get("value").asText();

                    if (value.toLowerCase().equals(valorDeseado.toLowerCase())) {
                    	 id= enumNode.get("id").asText();
                    	
                        break; // Puedes salir del bucle si encuentras la coincidencia
                    }
                }
            } 
            }

		
		} catch (IOException e) {
			  System.out.println(e.toString());
			e.printStackTrace();
		}
		
		return id;
		
		
		
	}
	
	
	
	/**
	 * Método para la inserción directa del pedido proveneinte del CRM con la integración particular de Poblado
	 * @param datosJSON
	 * @param lead
	 * @param idLog
	 */
	public void procesarPedidoBOTCRMPoblado(String datosJSON, String lead, int idLog)
	{
		DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
		String resultadoProceso = "";
		String obserProceso = "";
		String asesor = "";
		String nombreCliente = "";
		String telefono = "";
		String correo = "";
		String nombreDelCombo = "";
		String detalles = "";
		String sabor1 = "";
		String sabor2 = "";
		String adicion = "";
		String bebida = "";
		String acompanamiento = "";
		String bebida2 = "";
		String formaPago ="";
		String direccion ="";
		String referencia ="";
		String tienda ="";
		int idTienda = 0;
		String horaPedido = "";
		String fechaProgramado = "";
		//Para realizar el último parseo
		JSONParser parserFinal = new JSONParser();
		Object objParserFinal;
		//Aqui estamos verificando la utilización del valor
		JSONObject valor = new JSONObject();
		JSONArray valorArreglo = new JSONArray();
		String strValor = "";
		try
		{
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			JSONArray customFieldsArray = new JSONArray();
			try
			{
				String customFieldsValues = (String)jsonGeneral.get("custom_fields_values").toString();
				Object objcustomFieldsValues = parser.parse(customFieldsValues);
				customFieldsArray = (JSONArray) objcustomFieldsValues;
			}catch(Exception e1)
			{
				resultadoProceso = resultadoProceso + " Se tiene error dado que el LEAD no tiene los datos de pedido, posiblemente no es un LEAD de pedido de BOT o no están llenos los campos.";
				//Tratar problema de no tener campos adicionales
			}
			//Continuamos con la recolección de la información para el pedido
			for(int i = 0; i < customFieldsArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);
				String clave = objTemp.get("field_name").toString().toLowerCase();
				String values = objTemp.get("values").toString();
				Object objValues = parser.parse(values);
				JSONArray valuesArray = (JSONArray) objValues;
				if(clave.equals(new String("adicion")))
				{
					objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
					//Necesitamos probar una pedido con varias adiciones
					//valorArreglo = (JSONArray) objParserFinal;
					valor = (JSONObject) objParserFinal;
				}else
				{
					objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
					valor = (JSONObject) objParserFinal;
				}
				strValor = valor.get("value").toString();
				strValor = strValor.replaceAll("'", " ");
				//Dependiendo del campos se tendrá la recuperación del mismo
				if(clave.equals(new String("asesor que atiende")))
				{
					asesor = strValor;
					//asesor = IntegracionCRMHomologaAsesorDAO.obtenerHomologacionAsesorCRM(asesor);
				}else if(clave.equals(new String("nombre cliente"))||clave.equals(new String("Nombre del cliente")))
				{
					nombreCliente = strValor;
				}else if(clave.equals(new String("numero de teléfono")))
				{
					telefono  = strValor;
				}else if(clave.equals(new String("correo electrónico")) || clave.equals(new String("correo electrónico pqrs")))
				{
					correo = strValor;
				}else if(clave.equals(new String("nombre del combo")))
				{
					nombreDelCombo = strValor;
				}else if(clave.equals(new String("detalles")))
				{
					detalles = strValor.toLowerCase();
				}else if(clave.equals(new String("sabor 1")))
				{
					sabor1  = strValor;
				}else if(clave.equals(new String("sabor 2")))
				{
					sabor2 = strValor;
				}else if(clave.equals(new String("adicion")))
				{
					//En este punto si podrán ser varios
					adicion = strValor;
					
				}else if(clave.equals(new String("bebida")))
				{
					bebida = strValor;
				}else if(clave.equals(new String("acompañamiento")))
				{
					acompanamiento = strValor;
				}else if(clave.equals(new String("bebida 2")))
				{
					bebida2 = strValor;
				}else if(clave.equals(new String("forma de pago")))
				{
					formaPago = strValor;
				}else if(clave.equals(new String("dirección de envío")))
				{
					direccion = strValor;
				}else if(clave.equals(new String("referencia")))
				{
					referencia = strValor;
				}else if(clave.equals(new String("tienda")))
				{
					tienda = strValor;
					TiendaCtrl tiendaCtrl = new TiendaCtrl();
					idTienda = TiendaDAO.obteneridTienda(tienda);
					
				}else if(clave.equals(new String("hora pedido")))
				{
					horaPedido = strValor;
				}else if(clave.equals(new String("fecha pedido")))
				{
					fechaProgramado = strValor;
					fechaProgramado = fechaProgramado.toLowerCase();
				}
			}
			if(idTienda == 0)
			{
				//Fijamos la tienda del poblado
				idTienda = 6;
			}
			//Procedemos a crear el pedido en el sistema con base en como lo hemos creado para la tienda virtual
			String telefonoCelular = "";
			//El telefono le quitaremos el indicativo del país
			if(telefono.substring(0, 3).equals(new String("+57")))
			{
				telefono = telefono.substring(3);
			}
			//Para el caso del teléfono validaremos si es un fijo
			if(telefono.trim().length() == 7)
			{
				//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
				//contact center.
				telefono = "604" + telefono;
			}else
			{
				telefonoCelular = telefono;
			}

			//INCLUSIÓN DE LÓGICA
			//Validamos la hora del pedido para poder realizar la validación del mismo 
			Date fechaActual = new Date();
			//Requerimos saber si es viernes o Sábado
			Calendar calendarioActual = Calendar.getInstance();
			int diaActual = calendarioActual.get(Calendar.DAY_OF_WEEK);
			//Solo dejaremos pedir si es un jueves, viernes o sabado
			if((diaActual == 5)||(diaActual == 6)||(diaActual == 7))
			{
				//Se podría continuar con el pedido
			}
			else
			{
				//En caso de que se esté pidiendo un día diferente se enviará un mensaje al WhatsApp y al correo informando esta anomlia
				String mensaje = "Querido Cliente " + nombreCliente + " el pedido programado para Poblado solo se puede realizar los días jueves (para viernes y sabado), viernes (para viernes y sabado) y sabado (para sabado)";
				enviarWhatsAppUltramsg( mensaje , telefono);
				envioCorreoNotificacion(correo,"INCONVENIENTE FECHA PEDIDO-PEDIDO RECHAZADO", mensaje);
				return;
			}
			//Realizamos validación de si es sabado no se programe para el viernes
			if(diaActual == 7 && fechaProgramado.equals(new String("viernes")))
			{
				//En caso de que se esté pidiendo un día sabado para día viernes al WhatsApp y al correo informando esta anomlia
				String mensaje = "Querido Cliente " + nombreCliente + " el pedido programado para Poblado solo se puede realizar los días jueves (para viernes y sabado), viernes (para viernes y sabado) y sabado (para sabado),en tu caso estamos a día sabado y estas intentando programarlo para el día viernes.";
				enviarWhatsAppUltramsg( mensaje , telefono);
				envioCorreoNotificacion(correo,"INCONVENIENTE FECHA PEDIDO-PEDIDO RECHAZADO", mensaje);
				return;
			}
			//Por último hacemos validaciones de las horas de los pedidos vs la fecha del pedido
			LocalDateTime locaDate = LocalDateTime.now();
			int hora  = locaDate.getHour();
			int minutos = locaDate.getMinute();
			if(diaActual == 6 && fechaProgramado.equals(new String("viernes")))
			{
				if(hora > 16 || (hora== 16 && minutos > 45))
				{
					String mensaje = "Querido Cliente " + nombreCliente + " el pedido programado para Poblado para hoy viernes ya no se puede programar, dado que lo puedes programar máximo para las 4:45pm.";
					enviarWhatsAppUltramsg( mensaje , telefono);
					envioCorreoNotificacion(correo,"INCONVENIENTE FECHA PEDIDO-PEDIDO RECHAZADO", mensaje);
					return;
				}
			}
			if(diaActual == 7 && fechaProgramado.equals(new String("sabado")))
			{
				if(hora > 15 || (hora== 15 && minutos > 45))
				{
					String mensaje = "Querido Cliente " + nombreCliente + " el pedido programado para Poblado para hoy sabado ya no se puede programar, dado que lo puedes programar máximo para las 3:45pm.";
					enviarWhatsAppUltramsg( mensaje , telefono);
					envioCorreoNotificacion(correo,"INCONVENIENTE FECHA PEDIDO-PEDIDO RECHAZADO", mensaje);
					return;
				}
			}
			
			
			double latitud = 0, longitud = 0;
			ParametrosCtrl parCtrl = new ParametrosCtrl();
			int idFormaPago = parCtrl.realizarHomologacionFormaPagoTiendaVirtual(formaPago);
			if(idFormaPago == 0)
			{
				idFormaPago = 1;
				obserProceso = obserProceso + " " + "No se logro homologar la forma de pago del bot, por favor revisar";
			}
			Cliente clienteVirtual = new Cliente(0, telefono, nombreCliente, direccion, "", referencia,"", idTienda);
			clienteVirtual.setEmail(correo);
			clienteVirtual.setIdtienda(idTienda);
			clienteVirtual.setLatitud((float)latitud);
			clienteVirtual.setLontitud((float)longitud);
			clienteVirtual.setTelefonoCelular(telefonoCelular);
			clienteVirtual.setPoliticaDatos("S");
			
			//Realizamos la validación del cliente con toda la lógica en la capa Controladora de Cliente
			ClienteCtrl clienteCtrl = new ClienteCtrl();
			//Aprovecharemos que los objetos se pasan como valores por referencia por lo tanto las modificaciones realizadas al objeto tendrán mucho que ver
			int idCliente = clienteCtrl.validarClienteTiendaVirtualKuno(clienteVirtual, "");
			if(idCliente == 0)
			{
				idCliente = 1;
				obserProceso = obserProceso + " " + "No se logro crear o actualizar el cliente, se debe revisar el pedido para asignar el cliente correctamente.";
			}
			//Vamos a proceser la fuente del pedido
			String fuentePedido = "CRM-BOT";
			//Validamos la fecha del pedido si es programado
			LocalDateTime fechaHoy = LocalDateTime.now(); 
			
			//Validamos la fecha del pedido si es programado
			if(!fechaProgramado.equals(new String("")))
			{
				if(diaActual == 5 && fechaProgramado.equals(new String("viernes")))
				{
					fechaHoy = fechaHoy.plusDays(1);
				}else if(diaActual == 5 && fechaProgramado.equals(new String("sabado")))
				{
					fechaHoy = fechaHoy.plusDays(2);
				}else if(diaActual == 6 && fechaProgramado.equals(new String("sabado")))
				{
					fechaHoy = fechaHoy.plusDays(1);
				}
			} 
			Date fechaFinalPedido =  Date.from(fechaHoy.atZone(ZoneId.systemDefault()).toInstant());
			String strFechaFinal = formatoFinal.format(fechaFinalPedido);
			int idTipoPedido = 4;
			int idPedido = PedidoDAO.InsertarEncabezadoPedidoTiendaVirtualKuno(idTienda, idCliente, strFechaFinal, asesor, Integer.parseInt(lead), idTipoPedido, "CRM", fuentePedido); 
			//Realizamos la inserción del producto ordenado
			String log = insertarProductoBOTCRM(idPedido,nombreDelCombo, sabor1, sabor2, adicion, bebida, acompanamiento,  bebida2, detalles, idTipoPedido );
			LogPedidoVirtualKunoDAO.actualizarLogCRMBOTInfLog(idLog, log);
			//Luego de insertar el pedido haremos las últimas validaciones
			//Posteriormente realizamos los pasos para la finalización del pedido
			int tiempoPedido = TiempoPedidoDAO.retornarTiempoPedidoTienda(idTienda);
			//Consultaremos el tiempo que la tienda está dando en el momento
			long valorTotalContact = PedidoDAO.calcularTotalNetoPedido(idPedido);
			String horaProgramado = "AHORA";
			String pedidoProgramado = "N";
			if(!horaPedido.equals(new String("")))
			{
				horaProgramado = horaPedido;
				pedidoProgramado = "S";
			}
			int idEstadoPedido = 2;
			//Realizamos un cambio temporal para evitar las diferencias pero igual seguirán llegando los correos
			FinalizarPedidoTiendaVirtual(idPedido, idFormaPago, idCliente, tiempoPedido, "S", 0, "DESCUENTOS GENERALES DIARIOS", (valorTotalContact), pedidoProgramado, horaProgramado, idEstadoPedido);
			//Intervenimos cuando el idFormaPago es igual a 4 es porque es WOMPI y realizaremos el envío del link del pedido para pago al cliente
			if(idFormaPago == 4)
			{
				verificarEnvioLinkPagos(idPedido, clienteVirtual, valorTotalContact, idTienda);
			}
			try
			{
				//Enviamos notificación de creación del pedido
				ArrayList correos = new ArrayList();
				String correoEle = "jubote1@gmail.com";
				correos.add(correoEle);
				correos.add(correo);
				Correo correoNoti = new Correo(); 
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
				correoNoti.setAsunto("CREACIÓN DE PEDIDO BOT   # " + idPedido);
				correoNoti.setContrasena(infoCorreo.getClaveCorreo());
				correoNoti.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				//Construimos el mensaje
				String mensaje =  "<table border='2'> <tr> DATOS DE CREACIÓN DEL PEDIDO </tr>";
				mensaje = mensaje 
						+  "<tr><td><strong>Cliente</strong></td><td>"+ nombreCliente +"</td></tr>"
						+  "<tr><td><strong>Telefono</strong></td><td>"+ telefono +"</td></tr>"
						+  "<tr><td><strong>Fecha Pedido</strong></td><td>"+ strFechaFinal +"</td></tr>" 
						+  "<tr><td><strong>Hora Programado</strong></td><td>"+ horaProgramado +"</td></tr>"
						+  "<tr><td><strong>IMPORTANTE</strong></td><td>DEBE REALIZAR EL PAGO PARA QUE SU PEDIDO QUEDE PROGRAMADO</td></tr>";
				
				correoNoti.setMensaje(mensaje);
				//correo.setMensaje("Se ha radicado una pqrs por intermedio del BOT con los siguientes datos. Tipo Atención:  " + tipoAtencion + " Punto de Venta PQRS: " + puntoVentaPQRS + " Nombre del Cliente: " + nombreCliente + " Telefono: " + telefono + " descrición del problema: " + descripcionProblema  );
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correoNoti, correos);
				contro.enviarCorreo(); 
				
			}catch(Exception e)
			{
				
			}
			
		}catch(Exception e)
		{
			//Trabajar excepción de que no se pudo obtener información del LEAD y no se pudo crear
			resultadoProceso = resultadoProceso + " Se tiene error dado que el LEAD no tiene los datos de pedido, posiblemente no es un LEAD de pedido de BOT o no están llenos los campos.";
		}
	}
	
	public void procesarPQRSBOTCRM(String datosJSON, String lead, int idLog)
	{
		String tipoAtencion = "";
		String puntoVentaPQRS = "";
		String nombreCliente = "";
		String telefono = "";
		String descripcionProblema = "";
		//Para realizar el último parseo
		JSONParser parserFinal = new JSONParser();
		Object objParserFinal;
		//Aqui estamos verificando la utilización del valor
		JSONObject valor = new JSONObject();
		JSONArray valorArreglo = new JSONArray();
		String strValor = "";
		ClienteCtrl clienteCtrl = new ClienteCtrl();
		try
		{
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			JSONArray customFieldsArray = new JSONArray();
			try
			{
				String customFieldsValues = (String)jsonGeneral.get("custom_fields_values").toString();
				Object objcustomFieldsValues = parser.parse(customFieldsValues);
				customFieldsArray = (JSONArray) objcustomFieldsValues;
			}catch(Exception e1)
			{
			}
			//Continuamos con la recolección de la información para el pedido
			for(int i = 0; i < customFieldsArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);
				String clave = objTemp.get("field_name").toString().toLowerCase();
				String values = objTemp.get("values").toString();
				Object objValues = parser.parse(values);
				JSONArray valuesArray = (JSONArray) objValues;
				objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
				valor = (JSONObject) objParserFinal;
				strValor = valor.get("value").toString();
				strValor = strValor.replaceAll("'", " ");
				//Dependiendo del campos se tendrá la recuperación del mismo
				if(clave.equals(new String("tipo de atención")))
				{
					tipoAtencion = strValor;
				}else if(clave.equals(new String("punto de venta pqrs")))
				{
					puntoVentaPQRS = strValor;
				}else if(clave.equals(new String("nombre cliente pqrs")))
				{
					nombreCliente  = strValor;
				}else if(clave.equals(new String("teléfono cliente pqrs")))
				{
					telefono = strValor.trim();
					telefono = telefono.replaceAll(" ", "");
				}else if(clave.equals(new String("descripción del problema")))
				{
					descripcionProblema = strValor;
				}
			}
			//Realizamos envío de CORREO
			//Recuperar la lista de distribución para este correo
			ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEPQRSBOT");
			Date fecha = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			String strFechaSol = dateFormat.format(fecha);
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
			correo.setAsunto("RADICIÓN DE PQRS POR BOT  " + lead);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			//Construimos el mensaje
			String mensaje =  "<table border='2'> <tr> RADICACIÓN DE PQRS POR SISTEMA CRM FUENTE BOT </tr>";
			mensaje = mensaje 
					+  "<tr><td><strong>Tipo Atención</strong></td><td>"+ tipoAtencion +"</td></tr>"
					+  "<tr><td><strong>Punto Venta PQRS</strong></td><td>"+ puntoVentaPQRS +"</td></tr>"
					+  "<tr><td><strong>Nombre Cliente</strong></td><td>"+ nombreCliente +"</td></tr>"
					+  "<tr><td><strong>Teléfono</strong></td><td>"+ telefono +"</td></tr>"
					+  "<tr><td><strong>Descripción problema</strong></td><td>"+ descripcionProblema +"</td></tr>";
			correo.setMensaje(mensaje);
			//correo.setMensaje("Se ha radicado una pqrs por intermedio del BOT con los siguientes datos. Tipo Atención:  " + tipoAtencion + " Punto de Venta PQRS: " + puntoVentaPQRS + " Nombre del Cliente: " + nombreCliente + " Telefono: " + telefono + " descrición del problema: " + descripcionProblema  );
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
			
			//Posteriormente realizaremos un tratamiento para crear la PQRS y que esta sea creada en nuestro sistema y se notifique a 
			//Isabel y Mónica
			Cliente cliente = clienteCtrl.obtenerClienteUltimoPedido(telefono);
			//Validamos si el idcliente es cero significa que el cliente no existe
			if(cliente.getIdcliente()== 0)
			{
				cliente = clienteCtrl.obtenerClienteporIDObj(1);
			}
			SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
			String respuesta = solicitudCtrl.insertarSolicitudPQRS(strFechaSol, "peticion", cliente.getIdcliente(), cliente.getIdtienda(), nombreCliente, "", telefono, cliente.getDireccion(), cliente.getZonaDireccion(), cliente.getIdMunicipio(), descripcionProblema, 3, 2 , "externa", "tienda");
			//Realizar notificación WhatsApp
			String notificacion = "Ha ingreasado una PQRS por el BOT Pizza Americana, por favor revisar con el LEAD # " + lead;
			//Recuperaremos los celulares para notificar de la situación de la queja
			ArrayList telNotifica = GeneralDAO.obtenerCorreosParametro("NOTIFICAPQRSBOT");
			for(int i = 0; i  < telNotifica.size(); i++)
			{
				String telTemp = (String) telNotifica.get(i);
				notificarWhatsAppUltramsg(telTemp, notificacion);
			}
		}catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	
	/**
	 * Método para enviar de manera general un mensaje de WhatsApp con el celular y el texto.
	 * @param telefono
	 * @param mensaje
	 */
	public void notificarWhatsAppUltramsg(String telefono, String mensaje)
	{
		OkHttpClient client = new OkHttpClient();
		IntegracionCRM intWhat = IntegracionCRMDAO.obtenerInformacionIntegracion("ULTRAMSG");
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType, "token="+ intWhat.getAccessToken() +"&to=+57"+ telefono + "&body=*" + mensaje +"* &priority=1&referenceId=");
		Request request = new Request.Builder()
		  .url("https://api.ultramsg.com/"+ intWhat.getClientID() +"/messages/chat")
		  .post(body)
		  .addHeader("content-type", "application/x-www-form-urlencoded")
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
		}catch(Exception e)
		{
		}	
	}
	
	/**
	 * Método que realiza el procesamiento de la solicitud de factura electrónica
	 * @param datosJSON
	 * @param lead
	 * @param idLog
	 */
	public void procesarFACBOTCRM(String datosJSON, String lead, int idLog)
	{
		String tipoClienteFAC = "";
		String facturaVenta = "";
		String identificacion = "";
		String nombreCliente = "";
		String telefono = "";
		String correoElec = "";
		//Para realizar el último parseo
		JSONParser parserFinal = new JSONParser();
		Object objParserFinal;
		//Aqui estamos verificando la utilización del valor
		JSONObject valor = new JSONObject();
		JSONArray valorArreglo = new JSONArray();
		String strValor = "";
		try
		{
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			JSONArray customFieldsArray = new JSONArray();
			try
			{
				String customFieldsValues = (String)jsonGeneral.get("custom_fields_values").toString();
				Object objcustomFieldsValues = parser.parse(customFieldsValues);
				customFieldsArray = (JSONArray) objcustomFieldsValues;
			}catch(Exception e1)
			{
			}
			//Continuamos con la recolección de la información para el pedido
			for(int i = 0; i < customFieldsArray.size(); i++)
			{
				//Tomamos el elemento para procesar
				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);
				String clave = objTemp.get("field_name").toString().toLowerCase();
				String values = objTemp.get("values").toString();
				Object objValues = parser.parse(values);
				JSONArray valuesArray = (JSONArray) objValues;
				objParserFinal = parserFinal.parse(valuesArray.get(0).toString());
				valor = (JSONObject) objParserFinal;
				strValor = valor.get("value").toString();
				strValor = strValor.replaceAll("'", " ");
				//Dependiendo del campos se tendrá la recuperación del mismo
				if(clave.equals(new String("# tipo de cliente fac")))
				{
					tipoClienteFAC = strValor;
				}else if(clave.equals(new String("# factura de venta")))
				{
					facturaVenta = strValor;
				}else if(clave.equals(new String("# nit o cc")))
				{
					identificacion  = strValor;
				}else if(clave.equals(new String("# nombre empresa o cliente")))
				{
					nombreCliente = strValor;
				}else if(clave.equals(new String("# teléfono fac")))
				{
					telefono = strValor;
				}else if(clave.equals(new String("# correo electrónico fac")))
				{
					correoElec = strValor;
				}
			}
			//Realizamos envío de CORREO
			//Recuperar la lista de distribución para este correo
			ArrayList correos = GeneralDAO.obtenerCorreosParametro("REPORTEPQRSBOT");
			Date fecha = new Date();
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
			correo.setAsunto("SOLICITUD FACTURA ELECTRÓNICA  " + lead);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			//Construimos el mensaje
			String mensaje =  "<table border='2'> <tr> SOLICITUD FACTURA ELECTRÓNICA </tr>";
			mensaje = mensaje 
					+  "<tr><td><strong>Tipo de Cliente FAC</strong></td><td>"+ tipoClienteFAC +"</td></tr>"
					+  "<tr><td><strong>Factura de venta</strong></td><td>"+ facturaVenta +"</td></tr>"
					+  "<tr><td><strong>NIT o CC</strong></td><td>"+ identificacion +"</td></tr>"
					+  "<tr><td><strong>Nombre Empresa o Cliente</strong></td><td>"+ nombreCliente +"</td></tr>"
					+  "<tr><td><strong>Teléfono FAC</strong></td><td>"+ telefono +"</td></tr>"
					+  "<tr><td><strong>Correo electrónico FAC</strong></td><td>"+ correoElec +"</td></tr>";
			correo.setMensaje(mensaje);
			//correo.setMensaje("Se ha radicado una pqrs por intermedio del BOT con los siguientes datos. Tipo Atención:  " + tipoAtencion + " Punto de Venta PQRS: " + puntoVentaPQRS + " Nombre del Cliente: " + nombreCliente + " Telefono: " + telefono + " descrición del problema: " + descripcionProblema  );
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
			//REVISAR TEMA DE GENERAR LA SOLICITUD DE FACTURA ELECTRÓNICA EN NUESTRO SISTEMA
			Date fechaActual = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String strFechaAct = dateFormat.format(fechaActual);
			//Realizamos un pequeño tratamiento al telefono
			telefono = telefono.replace("+57", "");
			telefono = telefono.replace(" ", "");
			int intFacturaVenta = 0;
			try
			{
				intFacturaVenta = Integer.parseInt(facturaVenta);
			}catch(Exception e)
			{
				
			}
			SolicitudFactura solFactura = new SolicitudFactura(0,intFacturaVenta,intFacturaVenta, 0.0, identificacion, correoElec, nombreCliente, telefono, strFechaAct,"","","BOT");
	        PedidoCtrl pedCtrl = new PedidoCtrl();
	        String respuesta  = pedCtrl.insertarSolicitudFactura(solFactura);
		}catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}

	/**
	 * MAIN PARA PROBAR DEVOPS
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException
	{
		PedidoCtrl PedidoCtrl = new PedidoCtrl();

		PedidoCtrl.verificacionExistenciaClienteSalesManago("jubote1@gmail.com");
		
		PedidoCtrl.consultarCoberturaCRMBOT("{\"id\":21100107,\"name\":\"Lead #21100107\",\"price\":0,\"responsible_user_id\":7785881,\"group_id\":0,\"status_id\":58812344,\"pipeline_id\":5421266,\"loss_reason_id\":null,\"created_by\":0,\"updated_by\":0,\"created_at\":1697835183,\"updated_at\":1697835320,\"closed_at\":null,\"closest_task_at\":null,\"is_deleted\":false,\"custom_fields_values\":[{\"field_id\":858274,\"field_name\":\"Dirección de envío\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"calle 63a # 47 -27\"}]},{\"field_id\":863427,\"field_name\":\"Barrio y Municipio\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"prado centro , medellin\"}]}],\"score\":null,\"account_id\":29918165,\"labor_cost\":null,\"_links\":{\"self\":{\"href\":\"https://pizzaamericana.kommo.com/api/v4/leads/21100107?page=1&limit=250\"}},\"_embedded\":{\"tags\":[],\"companies\":[]}}", "21100107", 0);

		//PedidoCtrl.insertarPedidoDIDI("{\"app_id\":5764607613466051220,\"app_shop_id\":\"2\",\"type\":\"orderNew\",\"timestamp\":1694821682,\"data\":{\"order_id\":5764625991884408029,\"order_info\":{\"order_id\":5764625991884408029,\"status\":100,\"order_index\":162005,\"remark\":\"\",\"country\":\"CO\",\"city_id\":57010100,\"timezone\":\"America/Bogota\",\"pay_type\":2,\"pay_method\":2,\"pay_channel\":153,\"delivery_type\":1,\"delivery_eta\":0,\"expected_cook_eta\":0,\"expected_arrived_eta\":1694824858,\"create_time\":1694821681,\"pay_time\":1694821681,\"complete_time\":0,\"cancel_time\":0,\"shop_confirm_time\":0,\"price\":{\"order_price\":2446200,\"items_discount\":1306200,\"delivery_discount\":0,\"shop_paid_money\":0,\"refund_price\":0},\"shop\":{\"shop_id\":5764607591205045162,\"app_shop_id\":\"2\",\"shop_addr\":\"Carrera 53 #23-102 bello antioquia\",\"shop_name\":\"Pizza Americana - Bello\",\"shop_phone\":[{\"calling_code\":57,\"phone\":6044444553,\"type\":\"1\"}]},\"receive_address\":{\"uid\":0,\"name\":\"privacy protection\",\"first_name\":\"privacy protection\",\"last_name\":\"ud835udcf8ud835udcfcud835udcf9ud835udcf2ud835udcf7ud835udcea\",\"calling_code\":\"+57\",\"phone\":\"322***2741\",\"city\":\"Medellu00edn\",\"country_code\":\"CO\",\"poi_address\":\"privacy protection\",\"house_number\":\"privacy protection\",\"poi_lat\":6,\"poi_lng\":-76,\"coordinate_type\":\"wgs84\",\"poi_display_name\":\"privacy protection\"},\"order_items\":[{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Combo Pizzeta + Gaseosa 400 ml\",\"total_price\":2446200,\"sku_price\":2446200,\"amount\":1,\"remark\":\"\",\"sub_item_list\":[{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Manzana 400 ml\",\"total_price\":0,\"sku_price\":0,\"amount\":1,\"app_content_id\":\"\",\"content_app_external_id\":\"\",\"sub_item_list\":[]},{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Hawaiana\",\"total_price\":0,\"sku_price\":0,\"amount\":1,\"app_content_id\":\"\",\"content_app_external_id\":\"\",\"sub_item_list\":[]}],\"promo_type\":2,\"real_price\":1590000,\"promotion_detail\":{\"promo_type\":2,\"promo_discount\":856200,\"shop_subside_price\":642200},\"promo_list\":[{\"promo_type\":2,\"promo_discount\":856200,\"shop_subside_price\":642200}]}],\"promotions\":[{\"promo_type\":2,\"promo_discount\":856200,\"shop_subside_price\":642200},{\"promo_type\":11,\"promo_discount\":450000,\"shop_subside_price\":0}]}}}", "revisar");
		//PedidoCtrl.procesarFACBOTCRM("{\"id\":20617795,\"name\":\"Pizza Americana Manrique Piloto - Order #744528864 confirmed\",\"price\":34,\"responsible_user_id\":7785881,\"group_id\":0,\"status_id\":59471960,\"pipeline_id\":5421266,\"loss_reason_id\":null,\"created_by\":0,\"updated_by\":0,\"created_at\":1692742678,\"updated_at\":1694608827,\"closed_at\":null,\"closest_task_at\":null,\"is_deleted\":false,\"custom_fields_values\":[{\"field_id\":491608,\"field_name\":\"Tienda web:\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Pizza Americana Manrique Piloto\"}]},{\"field_id\":491706,\"field_name\":\"Tipo:\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"DELIVERY\"}]},{\"field_id\":492800,\"field_name\":\"Metodo de Pago\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"CASH\"}]},{\"field_id\":849448,\"field_name\":\"Llego\",\"field_code\":null,\"field_type\":\"multiselect\",\"values\":[{\"value\":\"Si\",\"enum_id\":537026,\"enum_code\":null}]},{\"field_id\":849440,\"field_name\":\"Conforme con producto\",\"field_code\":null,\"field_type\":\"numeric\",\"values\":[{\"value\":\"0\"}]},{\"field_id\":863599,\"field_name\":\"# Factura de venta\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"192993\"}]},{\"field_id\":863607,\"field_name\":\"# Tipo de cliente FAC\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Empresa\"}]},{\"field_id\":863601,\"field_name\":\"# NIT o CC\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"901290745\"}]},{\"field_id\":863603,\"field_name\":\"# Nombre empresa o cliente\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Pizza Americana SAS\"}]},{\"field_id\":863609,\"field_name\":\"# Correo electrónico FAC\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"jubote1@gmail.com\"}]},{\"field_id\":863605,\"field_name\":\"# Teléfono FAC\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"3148807773\"}]},{\"field_id\":863701,\"field_name\":\"# Documento FAC\",\"field_code\":null,\"field_type\":\"file\",\"values\":[{\"value\":{\"file_uuid\":\"044ffe1f-dd92-46d0-b866-f0c52b51d804\",\"version_uuid\":\"9afb3f07-7eb7-4510-aab3-aecbb1dc5c6e\",\"file_name\":\"_SOLICITUD PIEZAS GRÁFICAS junio 2023.pdf\",\"file_size\":529070,\"is_deleted\":false}}]}],\"score\":null,\"account_id\":29918165,\"labor_cost\":null,\"_links\":{\"self\":{\"href\":\"https://pizzaamericana.kommo.com/api/v4/leads/20617795?page=1&limit=250\"}},\"_embedded\":{\"tags\":[{\"id\":6000,\"name\":\"VENTA ONLINE\",\"color\":null}],\"companies\":[]}}", "20617795", 0);
		int idOrdenRappi = (int)(Math.random()*10000 + 1);
		PedidoCtrl.insertarPedidoRAPPI("{\"order_detail\":{\"discounts\":[{\"value\":4500.0,\"description\":\"Aprovecha envío GRATIS cerca a ti\",\"title\":\"DESCUENTOS CERCANOS\",\"product_id\":null,\"type\":\"free_shipping\",\"raw_value\":100,\"value_type\":\"percentage\",\"max_value\":171000.0,\"includes_toppings\":false,\"percentage_by_rappi\":100.0,\"percentage_by_partners\":0.0,\"amount_by_rappi\":4500.0,\"amount_by_partner\":0.0,\"discount_product_units\":0,\"discount_product_unit_value\":null,\"sku\":null}],\"order_id\":\""+idOrdenRappi+"\",\"cooking_time\":20,\"min_cooking_time\":14,\"max_cooking_time\":26,\"created_at\":\"2023-08-22 16:18:11\",\"delivery_method\":\"marketplace\",\"payment_method\":\"nequi\",\"billing_information\":null,\"delivery_information\":{\"complementary_street_without_meter\":\"43 A \",\"complete_main_street_number\":\"48 C SUR\",\"main_street_number_letter\":\"C\",\"complementary_street_quadrant\":null,\"city\":\"Envigado\",\"meter\":\"50\",\"complete_address\":\"CL 48 C SUR # 43 A 50\",\"complementary_street_prefix\":null,\"complete_main_street\":\"CL 48 C SUR\",\"main_street_type\":\"CL\",\"main_street_number_or_name\":\"48\",\"complementary_street_letter\":\"A\",\"main_street_prefix_letter\":null,\"main_street_prefix\":null,\"complete_complementary_street\":\"43 A 50\",\"complementary_street_number\":\"43\",\"complementary_street_prefix_letter\":null,\"neighborhood\":\"Primavera\",\"complement\":\"casa 170 cuidadela real\",\"postal_code\":\"055422\",\"main_street_quadrant\":\"SUR\"},\"totals\":{\"total_products\":46000.0,\"total_discounts\":4500.0,\"total_products_with_discount\":46000,\"total_products_without_discount\":46000,\"total_other_discounts\":0,\"total_order\":50500,\"total_to_pay\":0,\"discount_by_support\":0.0,\"total_discount_by_partner\":0.0,\"charges\":{\"shipping\":4500},\"other_totals\":{\"total_rappi_credits\":0,\"total_rappi_pay\":0,\"tip\":0}},\"items\":[{\"price\":46000,\"sku\":\"2135092234\",\"id\":\"2095543663\",\"name\":\"Pizza Hawaiana Extra Grande\",\"type\":\"product\",\"comments\":null,\"unit_price_with_discount\":46000,\"unit_price_without_discount\":46000,\"percentage_discount\":0,\"quantity\":1,\"subitems\":[{\"price\":0,\"sku\":\"2135092232\",\"id\":\"21584967\",\"name\":\"Colombiana 1.5 l\",\"type\":\"topping\",\"comments\":null,\"unit_price_with_discount\":0,\"unit_price_without_discount\":0,\"percentage_discount\":0,\"quantity\":1,\"subitems\":[]}]}],\"delivery_discount\":{\"total_percentage_discount\":0.0,\"total_value_discount\":0.0}},\"customer\":{\"first_name\":\"Pablo\",\"last_name\":\"Hernández \",\"phone_number\":\"3238060575\",\"email\":\"integration@rappi.com\",\"document_type\":\"1\",\"document_number\":\"1020404113\"},\"store\":{\"internal_id\":\"900171988\",\"external_id\":\"900171988\",\"name\":\"Pizza Americana Envigado  (Solo maleta grande)\"}}", "");
		int idOrdenDidi = (int)(Math.random()*10000 + 1);
		PedidoCtrl.insertarPedidoDIDI("{\"app_id\":5764607613466051220,\"app_shop_id\":\"11\",\"type\":\"orderNew\",\"timestamp\":1692580650,\"data\":{\"order_id\":"+ idOrdenDidi +",\"order_info\":{\"order_id\":"+ idOrdenDidi +",\"status\":100,\"order_index\":938010,\"remark\":\"\",\"country\":\"CO\",\"city_id\":57010100,\"timezone\":\"America/Bogota\",\"pay_type\":2,\"pay_method\":2,\"pay_channel\":153,\"delivery_type\":1,\"delivery_eta\":0,\"expected_cook_eta\":0,\"expected_arrived_eta\":1692583124,\"create_time\":1692580650,\"pay_time\":1692580650,\"complete_time\":0,\"cancel_time\":0,\"shop_confirm_time\":0,\"price\":{\"order_price\":1900000,\"items_discount\":0,\"delivery_discount\":0,\"shop_paid_money\":0,\"refund_price\":0},\"shop\":{\"shop_id\":5764607772705162938,\"app_shop_id\":\"11\",\"shop_addr\":\"Calle 68 # 43 u2013 05 medellin, Medellu00edn, Antioquia, Colombia\",\"shop_name\":\"Pizza Americana - Manrique Piloto\",\"shop_phone\":[{\"calling_code\":57,\"phone\":6044444553,\"type\":\"1\"}]},\"receive_address\":{\"uid\":0,\"name\":\"privacy protection\",\"first_name\":\"privacy protection\",\"last_name\":\"\",\"calling_code\":\"+57\",\"phone\":\"310***3910\",\"city\":\"Medellu00edn\",\"country_code\":\"CO\",\"poi_address\":\"privacy protection\",\"house_number\":\"privacy protection\",\"poi_lat\":6,\"poi_lng\":-76,\"coordinate_type\":\"wgs84\",\"poi_display_name\":\"privacy protection\"},\"order_items\":[{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Pizzeta por Mitades\",\"total_price\":1900000,\"sku_price\":1900000,\"amount\":1,\"remark\":\"\",\"sub_item_list\":[{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Manzana 400ml\",\"total_price\":0,\"sku_price\":0,\"amount\":1,\"app_content_id\":\"\",\"content_app_external_id\":\"\",\"sub_item_list\":[]},{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Americana\",\"total_price\":0,\"sku_price\":0,\"amount\":1,\"app_content_id\":\"\",\"content_app_external_id\":\"\",\"sub_item_list\":[]},{\"app_item_id\":\"\",\"app_external_id\":\"\",\"name\":\"Hawaiana\",\"total_price\":0,\"sku_price\":0,\"amount\":1,\"app_content_id\":\"\",\"content_app_external_id\":\"\",\"sub_item_list\":[]}],\"promo_type\":0,\"real_price\":1900000,\"promotion_detail\":{\"promo_type\":0,\"promo_discount\":0,\"shop_subside_price\":0}}]}}}", "revisar");
		
		//OTROS TEMAS DE PRUEBAS
		
		//PedidoCtrl.consultarCoberturaCRMBOT("leads%5Bstatus%5D%5B0%5D%5Bid%5D=20424793&leads%5Bstatus%5D%5B0%5D%5Bstatus_id%5D=58822804&leads%5Bstatus%5D%5B0%5D%5Bpipeline_id%5D=5421266&leads%5Bstatus%5D%5B0%5D%5Bold_status_id%5D=58812344&leads%5Bstatus%5D%5B0%5D%5Bold_pipeline_id%5D=5421266&account%5Bid%5D=29918165&account%5Bsubdomain%5D=pizzaamericana", "HTTP Authorization header: No authorization header");
		//PedidoCtrl.obtenerMensajePedidoBOTCRM("3003861204");
		//PedidoCtrl.capturarEventoPagoWompi("{\"event\":\"transaction.updated\",\"data\":{\"transaction\":{\"id\":\"17415-1690508372-39946\",\"created_at\":\"2023-07-28T01:39:32.482Z\",\"finalized_at\":\"2023-07-28T02:24:43.627Z\",\"amount_in_cents\":4100000,\"reference\":\"o7uauu_1690508299_xwdlTld0Y\",\"customer_email\":\"erikazapata7193@gmail.com\",\"currency\":\"COP\",\"payment_method_type\":\"NEQUI\",\"payment_method\":{\"type\":\"NEQUI\",\"extra\":{\"is_three_ds\":false,\"transaction_id\":\"350-123-200774-1690508373TD3e\",\"external_identifier\":\"1690508373TD3e\",\"nequi_transaction_id\":\"350-123-200774-1690508373TD3e\"},\"phone_number\":\"3012630615\"},\"status\":\"APPROVED\",\"status_message\":\"La transacción caducó\",\"shipping_address\":null,\"redirect_url\":\"https://pizzaamericana.co\",\"payment_source_id\":null,\"payment_link_id\":\"o7uauu\",\"customer_data\":{\"full_name\":\"Erika Zapata \",\"phone_number\":\"+573012630615\"},\"billing_data\":null}},\"sent_at\":\"2023-07-28T02:24:43.676Z\",\"timestamp\":1690511083,\"signature\":{\"checksum\":\"e9575a0dfbb7ded7b8bf30e028458fcf459e2426e4624259a1b7f700b2776527\",\"properties\":[\"transaction.id\",\"transaction.status\",\"transaction.amount_in_cents\"]},\"environment\":\"prod\"}");
		//PedidoCtrl.procesarPedidoBOTCRM("{\"id\":20288899,\"name\":\"Lead #20288899\",\"price\":0,\"responsible_user_id\":7785881,\"group_id\":0,\"status_id\":58725736,\"pipeline_id\":7052584,\"loss_reason_id\":null,\"created_by\":0,\"updated_by\":0,\"created_at\":1689875327,\"updated_at\":1690466036,\"closed_at\":null,\"closest_task_at\":null,\"is_deleted\":false,\"custom_fields_values\":[{\"field_id\":862089,\"field_name\":\"numero de teléfono\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"3185020068\"}]},{\"field_id\":861855,\"field_name\":\"Nombre del combo\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Grande (8 porciones)\"}]},{\"field_id\":862081,\"field_name\":\"Detalles\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Pizza Mitad y Mitad\"}]},{\"field_id\":857712,\"field_name\":\"Sabor 1\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Paisa\"}]},{\"field_id\":857714,\"field_name\":\"Sabor 2\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Mexicana\"}]},{\"field_id\":861771,\"field_name\":\"Adicion\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Adiciones de Grande Peperoni\"}]},{\"field_id\":861775,\"field_name\":\"Bebida\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Pepsi\"}]},{\"field_id\":861773,\"field_name\":\"Acompañamiento\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Deditos de masa con queso\"}]},{\"field_id\":862087,\"field_name\":\"nombre cliente\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Sebastian\"}]},{\"field_id\":862901,\"field_name\":\"FECHA PEDIDO\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"27/07/2023\"}]},{\"field_id\":862847,\"field_name\":\"HORA PEDIDO\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"16:30\"}]},{\"field_id\":858274,\"field_name\":\"Dirección de envío\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Calle 67a # 31 AE - 39\"}]},{\"field_id\":858276,\"field_name\":\"Referencia\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Torre 4\"}]},{\"field_id\":862091,\"field_name\":\"correo electrónico\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Sbotero0601@gmail.com\"}]},{\"field_id\":858272,\"field_name\":\"Forma de Pago\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Pago Virtual Link Wompi\"}]}],\"score\":null,\"account_id\":29918165,\"labor_cost\":null,\"_links\":{\"self\":{\"href\":\"https://pizzaamericana.kommo.com/api/v4/leads/20288899?page=1&limit=250\"}},\"_embedded\":{\"tags\":[{\"id\":93039,\"name\":\"PROGRAMADO\",\"color\":null}],\"companies\":[]}}", "20288899", 0);
		//PedidoCtrl.consultarPedidoCRMBOT("leads%5Bstatus%5D%5B0%5D%5Bid%5D=20017019&leads%5Bstatus%5D%5B0%5D%5Bstatus_id%5D=53955335&leads%5Bstatus%5D%5B0%5D%5Bpipeline_id%5D=5064734&leads%5Bstatus%5D%5B0%5D%5Bold_status_id%5D=142&leads%5Bstatus%5D%5B0%5D%5Bold_pipeline_id%5D=5064734&account%5Bid%5D=29918165&account%5Bsubdomain%5D=pizzaamericana","prueba");
		//PedidoCtrl.limpiarLeadCRMBOT("leads%5Bstatus%5D%5B0%5D%5Bid%5D=19895821&leads%5Bstatus%5D%5B0%5D%5Bstatus_id%5D=47301113&leads%5Bstatus%5D%5B0%5D%5Bpipeline_id%5D=5010344&leads%5Bstatus%5D%5B0%5D%5Bold_status_id%5D=53365371&leads%5Bstatus%5D%5B0%5D%5Bold_pipeline_id%5D=5010344&account%5Bid%5D=29918165&account%5Bsubdomain%5D=pizzaamericana", "");
		//PedidoCtrl.procesarPQRSBOTCRM("{\"id\":20780873,\"name\":\"Lead #20780873\",\"price\":0,\"responsible_user_id\":7785881,\"group_id\":0,\"status_id\":58728028,\"pipeline_id\":5421266,\"loss_reason_id\":null,\"created_by\":0,\"updated_by\":0,\"created_at\":1694391557,\"updated_at\":1694391802,\"closed_at\":null,\"closest_task_at\":null,\"is_deleted\":false,\"custom_fields_values\":[{\"field_id\":862681,\"field_name\":\"Tipo de atención\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Punto de venta\"}]},{\"field_id\":862683,\"field_name\":\"punto de venta pqrs\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Envigado\"}]},{\"field_id\":862673,\"field_name\":\"Nombre cliente PQRS\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"Arley Eduardo Palacio Cardona\"}]},{\"field_id\":862675,\"field_name\":\"Teléfono cliente PQRS\",\"field_code\":null,\"field_type\":\"text\",\"values\":[{\"value\":\"312 7038619\"}]},{\"field_id\":862677,\"field_name\":\"descripción del problema\",\"field_code\":null,\"field_type\":\"textarea\",\"values\":[{\"value\":\"Pedí unos palitos con queso y arequipe y no me enviaron el arequipe\"}]}],\"score\":null,\"account_id\":29918165,\"labor_cost\":null,\"_links\":{\"self\":{\"href\":\"https://pizzaamericana.kommo.com/api/v4/leads/20780873?page=1&limit=250\"}},\"_embedded\":{\"tags\":[],\"companies\":[]}}", "20780873", 0);
		//PedidoCtrl.envioCorreoTarjetaRegalo();
	}
	
	/**
	 * MAIN PARA PROBAR LA CREACIÓN DE UN PEDIDO DE RAPPI
	 * @param args
	 * @throws IOException
	 */
//	public static void main(String args[]) throws IOException
//	{
//		PedidoCtrl PedidoCtrl = new PedidoCtrl();
//		//PedidoCtrl.aceptarPedidoIntegracionRAPPI(357382552);
//		PedidoCtrl.insertarPedidoRAPPI("{"order_detail":{"discounts":[{"value":4500.0,"description":null,"title":null,"product_id":null,"type":"prime_discount_shipping","raw_value":0.0,"value_type":"percentage","max_value":null,"includes_toppings":false,"percentage_by_rappi":100,"percentage_by_partners":0,"amount_by_rappi":4500.0,"amount_by_partner":0.0,"discount_product_units":0,"discount_product_unit_value":null,"sku":null}],"order_id":"2155573457","cooking_time":20,"min_cooking_time":14,"max_cooking_time":26,"created_at":"2023-08-14 18:56:42","delivery_method":"marketplace","payment_method":"cc","billing_information":null,"delivery_information":{"complementary_street_without_meter":"74 ","complete_main_street_number":"31 ","main_street_number_letter":"","complementary_street_quadrant":null,"city":"Medellín","meter":"19","complete_address":"Calle 31 # 74 19","complementary_street_prefix":null,"complete_main_street":"Calle 31 ","main_street_type":"Calle","main_street_number_or_name":"31","complementary_street_letter":"","main_street_prefix_letter":null,"main_street_prefix":null,"complete_complementary_street":"74 19","complementary_street_number":"74","complementary_street_prefix_letter":null,"neighborhood":"Belén","complement":"","postal_code":"050030","main_street_quadrant":null},"totals":{"total_products":35000.0,"total_discounts":4500.0,"total_products_with_discount":35000,"total_products_without_discount":35000,"total_other_discounts":0,"total_order":39500,"total_to_pay":0,"discount_by_support":0.0,"total_discount_by_partner":0.0,"charges":{"shipping":4500},"other_totals":{"total_rappi_credits":0,"total_rappi_pay":0,"tip":0}},"items":[{"price":35000,"sku":"2135092188","id":"2095543677","name":"Pizza Americana Mediana","type":"product","comments":null,"unit_price_with_discount":35000,"unit_price_without_discount":35000,"percentage_discount":0,"quantity":1,"subitems":[{"price":0,"sku":"2135092185","id":"21585022","name":"Manzana 1.5 l","type":"topping","comments":null,"unit_price_with_discount":0,"unit_price_without_discount":0,"percentage_discount":0,"quantity":1,"subitems":[]}]}],"delivery_discount":{"total_percentage_discount":0.0,"total_value_discount":0.0}},"customer":{"first_name":"Alejandro","last_name":"Arbeláez","phone_number":"3054316759","email":"integration@rappi.com","document_type":"1","document_number":"1017270642"},"store":{"internal_id":"900171987","external_id":"900171987","name":"Pizza Americana La América"}}", "PRUEBA");
//	}
	
	public String insertarSolicitudFacturaImagen(SolicitudFacturaImagenes solicitud)
	{
		int idImagen = SolicitudFacturaImagenesDAO.insertarSolicitudFacturaImagen(solicitud);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("idimagen", idImagen);
		return(resultadoJSON.toJSONString());
	}
	
	
	public String procesarSolicitudFactura(int idSolicitud)
	{
		SolicitudFacturaDAO.procesarSolicitudFactura(idSolicitud);
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("respuesta", "exitoso");
		return(resultadoJSON.toJSONString());
	}
	
	public String consultarSolicitudFacturaImagenes(int idSolicitud)
	{
		ArrayList<String> imagenes = SolicitudFacturaImagenesDAO.consultarSolicitudFacturaImagenes(idSolicitud);
		JSONArray resultadoJSON = new JSONArray();
		for(int i = 0; i < imagenes.size(); i++)
		{
			JSONObject cadaImagen = new JSONObject();
			String imagen = (String) imagenes.get(i);
			cadaImagen.put("rutaimagen", imagen);
			resultadoJSON.add(cadaImagen);
			
		}
		return(resultadoJSON.toJSONString());
	}
	
	
	public String consultarEstadosPedidoTienda(int idTienda)
	{
		String respuesta = "";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		//Recuperamos la tienda que requerimos trabajar con el servicio
		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda != null)
		{
			//Realizar invocación de servicio en tienda
			String rutaURL = tienda.getUrl() + "ConsultarEstadosPedidoTienda?dsnodbc=" + tienda.getDsnTienda() + "&pos=" + tienda.getPos();
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				StringBuffer retornoTienda = new StringBuffer();
				//Se realiza la ejecución del servicio de finalizar pedido
				HttpResponse responseFinPed = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    		responseFinPed.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				System.out.println(retorno);
				respuesta = retorno.toString();
			}catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
		if(respuesta.equals(new String("")))
		{
			JSONObject resultado = new JSONObject();
			resultado.put("resultado", "error");
			resultado.put("tipo_error", "error servicio tienda");
		}
		return(respuesta);
	}
	
	public String obtenerEgresosServicio(int idTienda, String fecha)
	{
		String respuesta = "";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		//Recuperamos la tienda que requerimos trabajar con el servicio
		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda != null)
		{
			//Realizar invocación de servicio en tienda
			String rutaURL = tienda.getUrl() + "ObtenerEgresosServicio?fecha=" + fecha;
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				StringBuffer retornoTienda = new StringBuffer();
				//Se realiza la ejecución del servicio de finalizar pedido
				HttpResponse responseFinPed = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    		responseFinPed.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				System.out.println(retorno);
				respuesta = retorno.toString();
			}catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
		if(respuesta.equals(new String("")))
		{
			JSONObject resultado = new JSONObject();
			resultado.put("resultado", "error");
			resultado.put("tipo_error", "error servicio tienda");
		}
		return(respuesta);
	}
	public String consultaResumidaEstadoTienda(int idTienda, String fecha)
	{
		String respuesta = "";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		//Recuperamos la tienda que requerimos trabajar con el servicio
		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda != null)
		{
			//Realizar invocación de servicio en tienda
			String rutaURL = tienda.getUrl() + "ConsultaResumidaEstadoTienda?fecha=" + fecha;
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				StringBuffer retornoTienda = new StringBuffer();
				//Se realiza la ejecución del servicio de finalizar pedido
				HttpResponse responseFinPed = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    		responseFinPed.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				System.out.println(retorno);
				respuesta = retorno.toString();
			}catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
		if(respuesta.equals(new String("")))
		{
			JSONObject resultado = new JSONObject();
			resultado.put("resultado", "error");
			resultado.put("tipo_error", "error servicio tienda");
		}
		return(respuesta);
	}
	
	public String aprobarEgresoServicio(int idTienda, int idEgreso)
	{
		String respuesta = "";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		//Recuperamos la tienda que requerimos trabajar con el servicio
		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda != null)
		{
			//Realizar invocación de servicio en tienda
			String rutaURL = tienda.getUrl() + "AprobarEgresoServicio?idegreso=" + idEgreso;
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				StringBuffer retornoTienda = new StringBuffer();
				//Se realiza la ejecución del servicio de finalizar pedido
				HttpResponse responseFinPed = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    		responseFinPed.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				System.out.println(retorno);
				respuesta = retorno.toString();
			}catch(Exception e)
			{
				System.out.println(e.toString());
			}
		}
		if(respuesta.equals(new String("")))
		{
			JSONObject resultado = new JSONObject();
			resultado.put("resultado", "error");
			resultado.put("tipo_error", "error servicio tienda");
		}
		return(respuesta);
	}
	
	
	/**
	 * Método que se encarga de realizar la actualización cada semana del token de actualización para la plataformad de RAPPI
	 * @param plataforma
	 * @throws IOException
	 */
	public void actualizarTokenIntegracionRAPPI(String plataforma) throws IOException
	{
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion(plataforma);
		String strBody = "{\"client_id\": \""+ intCRM.getClientID() +"\",\n"
				+ "\"client_secret\": \"" + intCRM.getFreshToken() + "\",\n"
				+ "  \"audience\": \"https://services.rappi.com/api/v2/restaurants-integrations-public-api\",\n"
				+ "  \"grant_type\": \"client_credentials\"}";
		OkHttpClient client = new OkHttpClient();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, strBody );
		Request request = new Request.Builder()
		  .url("https://rests-integrations.auth0.com/oauth/token")
		  .post(body)
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
			String respuestaJSON = response.body().string();
			System.out.println("1 " + response.toString());
			System.out.println("2 " + response.body().toString());
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(respuestaJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			String accessToken = (String)jsonGeneral.get("access_token").toString();
			IntegracionCRMDAO.actualizarTokenIntegracionCRM(plataforma, accessToken, intCRM.getFreshToken());
		}catch(Exception e)
		{
			System.out.println(e.toString());
		}
	}
	
	/**
	 * Método construido para el propósito de descomponer el JSON con la información del pedido de RAPPI
	 * @param stringJSON
	 * @param idLog
	 * @return
	 */
	public String insertarPedidoRAPPI(String stringJSON, int idLog)
	{
		String log = "";
		int idPedidoCreado = 0;
		long idOrdenComercio = 0;
		//Variable que le asignará un id único al pedido de tienda virtual que llega
		int idInterno = 0;
		//Variable donde almacenaremos el idCliente
		int idCliente = 0;
		//Variable para almacenar el precio total
		long valorTotal = 0;
		long valorTotalContact = 0;
		String estadoPedido = "";
		boolean esProgramado = false;
		//Variable donde se almacenará la hora de programado del pedido con S o N y la hora de programado
		//Inicializamos ambas variables con los valores 
		String programado = "N";
		String horaProgramado = "AHORA";
		String pedidoProgramado = "N";
		//Variable donde almacenaremos el tipoPedido
		String tipoPedido = "";
		int idTipoPedido = 1;
		//Formamos la fecha del pedido que podrá cambiar si el pedido es posfechado
		Date fechaPedido = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
		String strFechaFinal = formatoFinal.format(fechaPedido);
		double propina = 0;
		double rappiCreditos = 0;
		double rappiPay = 0;
		double tarifaServicio = 0;
		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();
		//Comenzaremos a parsear el JSON que nos llego
		JSONParser parser = new JSONParser();
		try
		{
			//Realizamos el parseo del primer nivel del JSON
			Object objParser = parser.parse(stringJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			//Descomponemos la información en order_detail,customer,store
			String orderDetailJSON = (String)jsonGeneral.get("order_detail").toString();
			String customerJSON = (String)jsonGeneral.get("customer").toString();
			String storeJSON = (String)jsonGeneral.get("store").toString();
			Object objParserOrderDetail = parser.parse(orderDetailJSON);
			JSONObject jsonOrder = (JSONObject) objParserOrderDetail;
			//Comenzamos a extraer la informacion que allí viene
			idOrdenComercio = Long.parseLong((String)jsonOrder.get("order_id"));
			//Incluiremos validación para que no cree pedidos dobles
			boolean yaExisteOrden = PedidoDAO.consultarExistenciaOrdenRappi(idOrdenComercio);
			if(yaExisteOrden)
			{
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
				ArrayList correos = new ArrayList();
				correo.setAsunto("RAPPI ERROR CREANDO PEDIDO DUPLICADO RAPPI  " + idOrdenComercio);
				String correoEle = "jubote1@gmail.com";
				correos.add(correoEle);
				correos.add("lidercontactcenter@pizzaamericana.com.co");
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje(" Se tiene un problema creando el pedido duplicado de RAPPI número  " + idOrdenComercio);
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
				return("");
			}
			
			tipoPedido = (String)jsonOrder.get("delivery_method");
			if(tipoPedido.equals(new String("marketplace")))
			{
				idTipoPedido = 1;
			}else if(tipoPedido.equals(new String("pickup")) || tipoPedido.equals(new String("delivery")))
			{
				idTipoPedido = 2;
				log = log + " " + "El pedido de RAPPI no llego para ser llevado por nosotros, por favor revisar.";
			}
			
			//Obtenemos el valor de la propina y la tarifa de servicio
			JSONObject jsonTotals  = (JSONObject)jsonOrder.get("totals");
			JSONObject jsonCharges  = (JSONObject)jsonTotals.get("charges");
			try
			{
				tarifaServicio = (double)jsonCharges.get("service_fee");
			}catch(Exception e)
			{
				try
				{
					tarifaServicio = ((Long)jsonCharges.get("service_fee")).doubleValue();
				}catch(Exception e1)
				{
					tarifaServicio = 0;
				}
			}
			JSONObject jsonOtherTotals  = (JSONObject)jsonTotals.get("other_totals");
			try
			{
				propina = (double)jsonOtherTotals.get("tip");
			}catch(Exception e)
			{
				try
				{
					propina = ((Long)jsonOtherTotals.get("tip")).doubleValue();
				}catch(Exception e1)
				{
					propina = 0;
				}
			}
			try
			{
				rappiCreditos = (double)jsonOtherTotals.get("total_rappi_credits");
			}catch(Exception e)
			{
				try
				{
					rappiCreditos = ((Long)jsonOtherTotals.get("total_rappi_credits")).doubleValue();
				}catch(Exception e1)
				{
					rappiCreditos = 0;
				}
			}
			try
			{
				rappiPay = (double)jsonOtherTotals.get("total_rappi_pay");
			}catch(Exception e)
			{
				try
				{
					rappiPay = ((Long)jsonOtherTotals.get("total_rappi_pay")).doubleValue();
				}catch(Exception e1)
				{
					rappiPay = 0;
				}
			}
		
			//Trabajamos con la extracción y homologación de la forma de pago
			String formPago = (String)jsonOrder.get("payment_method");
			//Realizamos la homologación de la formad de pago
			ParametrosCtrl parCtrl = new ParametrosCtrl();
			int idFormaPago = parCtrl.realizarHomologacionFormaPagoTiendaVirtual(formPago);
			if(idFormaPago == 0)
			{
				idFormaPago = 3;
				log = log + " " + "No se pudo homologar la forma de pago fue puesto en PAGO-ONLINE, por favor revisar.";
			}
			//Procesamos la información de Ubicación del domicilio
			JSONObject jsonDelivInf  = (JSONObject)jsonOrder.get("delivery_information");
			//Tendremos un trato diferencial para la dirección
			String dirRes = "";
			String ciudad = "";
			String dirAdicional = "";
			String complementoDir = "";
			try
			{
				dirRes = (String)jsonDelivInf.get("complete_address");
				if(dirRes == null)
				{
					//Se deberá probar a armar la dirección manualmente
					dirRes = "";
					dirRes = (String)jsonDelivInf.get("complete_main_street_number") + " " + (String)jsonDelivInf.get("complete_complementary_street");
				}
				dirRes = dirRes.replaceAll("'", " ");
			}catch(Exception e)
			{
				dirRes = "";
			}
			//Trabajamos sobre la ciudad
			try
			{
				ciudad = (String)jsonDelivInf.get("city");
				ciudad = ciudad.replaceAll("'", " ");
				if(ciudad == null)
				{
					ciudad = "";
				}
			}catch(Exception e)
			{
				ciudad = "";
			}
			//Trabajamos sobre la info adicional
			try
			{
				dirAdicional = (String)jsonDelivInf.get("neighborhood");
				if(dirAdicional == null)
				{
					dirAdicional = "";
				}
				dirAdicional = dirAdicional.replaceAll("'", " ");
			}catch(Exception e)
			{
				dirAdicional = "";
			}
			//Trabajamos sobre el complemento de la dirección
			try
			{
				complementoDir = (String)jsonDelivInf.get("complement");
				if(complementoDir == null)
				{
					complementoDir = "";
				}
				complementoDir = complementoDir.replaceAll("'", " ");
			}catch(Exception e)
			{
				complementoDir = "";
			}
			//Juntamos lo que se tenga en dirAdicional
			dirAdicional = dirAdicional + " " + complementoDir;
			if(dirAdicional.length() > 200)
			{
				dirAdicional = dirAdicional.substring(0,200);
			}
			
			
			String direccion = "";
			String obsDireccion = "";
			direccion = direccion.replaceAll("'", " ");
			//Tendremos unas reglas para conformar la dirección
			if(dirAdicional.equals(new String("")))
			{
				direccion = dirRes + " " + ciudad;
				obsDireccion = "";
			}else
			{
				direccion = dirRes + " " + ciudad;
				obsDireccion = dirAdicional;
			}
			double latitud = 0, longitud = 0;
			//Realizamos la intervención para tratar en el momentoen que la ubicación viene en cero
			if(latitud== 0 && longitud == 0)
			{
				UbicacionCtrl  ubicacion = new UbicacionCtrl();
				Ubicacion ubicaResp = ubicacion.ubicarDireccionEnTiendaBatch(direccion + " " + ciudad);
				latitud = ubicaResp.getLatitud();
				longitud = ubicaResp.getLongitud();
			}
			
			
			//REALIZAMOS PROCESAMIENTO DE LA INFORMACIÓN DEL CLIENTE
			Object objParserCustomer = parser.parse(customerJSON);
			JSONObject jsonCustomer = (JSONObject) objParserCustomer;
			//Continuamos con el procesamiento del pedido
			String telefono = (String)jsonCustomer.get("phone_number");
			String telefonoCelular = "";
			//El telefono le quitaremos el indicativo del país
			if(telefono.substring(0, 3).equals(new String("+57")))
			{
				telefono = telefono.substring(3);
			}
			//Para el caso del teléfono validaremos si es un fijo
			if(telefono.trim().length() == 7)
			{
				//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
				//contact center.
				telefono = "604" + telefono;
			}else
			{
				telefonoCelular = telefono;
			}
			String nombres;
			try {
				nombres = (String)jsonCustomer.get("first_name");
				nombres = nombres.replaceAll("'", " ");
			}catch(Exception enombre)
			{
				nombres = "NO SE PUDO EXTRAR EL NOMBRE";
			}
			String apellidos;
			try {
				apellidos = (String)jsonCustomer.get("last_name");
				apellidos = apellidos.replaceAll("'", " ");
			}catch(Exception enombre)
			{
				apellidos = "NO SE PUDO EXTRAR EL APELLIDO";
			}
			String email  = (String)jsonCustomer.get("email");
			
			//REALIZAMOS PROCESAMIENTO DE LA INFORMACIÓN DE LA TIENDA
			Object objParserStore = parser.parse(storeJSON);
			JSONObject jsonStore = (JSONObject) objParserStore;
			Long internalIDTienda =Long.parseLong((String)jsonStore.get("internal_id"));
			int token = internalIDTienda.intValue();
			HomologacionTiendaToken homoTiendaToken = HomologacionTiendaTokenDAO.obtenerHomologacionTiendaToken(token);
			int idTienda = homoTiendaToken.getIdtienda();
			String origenPedido = homoTiendaToken.getOrigen();
			if(idTienda == 0)
			{
				idTienda = 1;
				log = log + " " + "No fue posible homologar la tienda por defecto se puso tienda de Manrique.";
			}
			if(origenPedido.equals(new String("")))
			{
				origenPedido = "RAP";
			}
			//REALIZAMOS PROCESAMIENTO DEL CLIENTE CON SU DIRECCIÓN
			Cliente clienteVirtual = new Cliente(0, telefono, nombres, direccion, "", obsDireccion,"", idTienda);
			clienteVirtual.setApellidos(apellidos);
			clienteVirtual.setEmail(email);
			clienteVirtual.setIdtienda(idTienda);
			clienteVirtual.setLatitud((float)latitud);
			clienteVirtual.setLontitud((float)longitud);
			clienteVirtual.setTelefonoCelular(telefonoCelular);
			clienteVirtual.setPoliticaDatos("S");
			//Realizamos la validación del cliente con toda la lógica en la capa Controladora de Cliente
			ClienteCtrl clienteCtrl = new ClienteCtrl();
			//Aprovecharemos que los objetos se pasan como valores por referencia por lo tanto las modificaciones realizadas al objeto tendrán mucho que ver
			idCliente = clienteCtrl.validarClienteTiendaVirtualKuno(clienteVirtual, "");
			if(idCliente == 0)
			{
				Correo correo = new Correo();
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
				ArrayList correos = new ArrayList();
				correo.setAsunto("ERROR GRAVE CLIENTE NO CREADO ACTUALIZADO  " + idOrdenComercio);
				String correoEle = "jubote1@gmail.com";
				correos.add(correoEle);
				correos.add("lidercontactcenter@pizzaamericana.com.co");
				correo.setContrasena(infoCorreo.getClaveCorreo());
				correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correo.setMensaje(" Se tiene un problema creando el pedido duplicado de RAPPI número  " + idOrdenComercio);
				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
				contro.enviarCorreo();
			}
			
			//REALIZAMOS PROCESAMIENTO DE LOS PRODUCTOS QUE INCLUYE LA ORDEN
			JSONArray detalleOrdenArray = (JSONArray)jsonOrder.get("items");
			int idProducto;
			int idEspecialidad;
			int idExcepcion;
			//Insertamos el encabezado del pedido
			int idPedido = PedidoDAO.InsertarEncabezadoPedidoTiendaVirtualKuno(idTienda, idCliente, strFechaFinal, "RAPPI",idOrdenComercio, 1, "RAP", "RAPPI");
			int idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual("Valor del domicilio plataforma");
			double valorDomicilio = ProductoDAO.retornarProducto(idProductoDomicilio).getPreciogeneral();
			DetallePedido detPedidoDomi = new DetallePedido(idProductoDomicilio,idPedido,1,0,0,valorDomicilio,valorDomicilio*1, "" , "" /*observacion*/, 0, 0, "", "");
			PedidoDAO.InsertarDetallePedido(detPedidoDomi);
			//Ingresamos los valores de propina y tarifa de servicio
			int idProductoTarifaServicio = 0;
			if(tarifaServicio > 0)
			{
				idProductoTarifaServicio = parCtrl.homologarProductoTiendaVirtual("Tarifa Servicio");
				DetallePedido detPedidoTarifa = new DetallePedido(idProductoTarifaServicio,idPedido,1,0,0,tarifaServicio,tarifaServicio*1, "" , "" /*observacion*/, 0, 0, "", "");
				PedidoDAO.InsertarDetallePedido(detPedidoTarifa);
			}
			int idProductoPropina = 0;
			if(propina > 0)
			{
				idProductoPropina = parCtrl.homologarProductoTiendaVirtual("Propina");
				DetallePedido detPedidoPropina = new DetallePedido(idProductoPropina,idPedido,1,0,0,propina,propina*1, "" , "" /*observacion*/, 0, 0, "", "");
				PedidoDAO.InsertarDetallePedido(detPedidoPropina);
			}
			for(int i = 0; i < detalleOrdenArray.size(); i++)
			{
				//Obtenemos cada uno de los items de PEDIDO
				JSONObject productoTemp = (JSONObject) detalleOrdenArray.get(i);
				//Tomamos el nombre del producto
				String nombreProducto = (String) productoTemp.get("name");
				//Buscaremos la manera de homologar con el nombre en donde homologaríamos producto, sabor y es promoción, crearemos una tabla para este fin.
				HomologacionProductoRappi homProducto = HomologacionProductoRappiDAO.obtenerHomologacionProductoRappi(nombreProducto);
				idProducto = homProducto.getIdProducto();
				idEspecialidad = homProducto.getIdEspecialidad();
				//Calcular el valor unitario
				double valorUnitario = homProducto.getPrecio();
				int cantidad = new Long((long)productoTemp.get("quantity")).intValue();
				double douCantidad = (double)cantidad;
				//Necesitamos extraer la información de la gaseosa
				JSONArray subItemsOrdenArray = (JSONArray)productoTemp.get("subitems");
				int idSaborTipoLiquido = 0;
				for(int j = 0; j < subItemsOrdenArray.size(); j++)
				{
					JSONObject subProductoTemp = (JSONObject) subItemsOrdenArray.get(j);
					String nombreGaseosa = (String)subProductoTemp.get("name");
					idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(nombreGaseosa);
				}
				DetallePedido detPedido = new DetallePedido(idProducto,idPedido,douCantidad,idEspecialidad,idEspecialidad,valorUnitario,valorUnitario*cantidad, "" /*strAdiciones*/ , "" /*observacion*/, idSaborTipoLiquido, 0/*idExcepcion*/, "" /*strCON*/, "");
				int idDetallePedido = PedidoDAO.InsertarDetallePedido(detPedido);
				//Revisamos el tema de los de productos incluidos
				if(idDetallePedido > 0)
				{
					for(int j = 0; j < productosIncluidos.size(); j++)
					{
						ProductoIncluido proIncTemp = productosIncluidos.get(j);
						if(proIncTemp.getIdproductopadre() == detPedido.getIdproducto())
						{
							//Realizamos una modificación para que la cantidad a incluir se debe multiplicar por la cantidad de productos adicionados
							DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad()*cantidad,0,0,0,0, "" , "Producto Incluido-"+idDetallePedido, 0, 0 /*idexcepcion*/, "", "");
							PedidoDAO.InsertarDetallePedido(detPedidoInc);
						}
					}
				}
				
			}
			int idEstadoPedido = 2;
			//Posteriormente realizamos los pasos para la finalización del pedido
			int tiempoPedido = TiempoPedidoDAO.retornarTiempoPedidoTienda(idTienda);
			
			//VALIDAMOS LA INFORMACIÓN DEL DESCUENTO
			double descuentoPedido = 0;
			double descuentoPropio = 0;
			double descuentoPlataforma = 0;
			String descuentoAsumido = "N";
			String marketPlace = "S";
			String motivoDescuento = "";
			JSONArray descuentosArray = (JSONArray)jsonOrder.get("discounts");
			//Realizamos modificación
			for(int k = 0; k < descuentosArray.size(); k++)
			{
				JSONObject descuentoTemp = (JSONObject) descuentosArray.get(k);
				//Convertimos el valor del descuento 
				Double desTemp = 0.0;
				String descripcion = "";
				try {
					descripcion = (String)descuentoTemp.get("description");
				}catch(Exception e)
				{
					descripcion = "";
				}
				if(descripcion == null)
				{
					descripcion = "";
				}
				try {
					desTemp = (double)descuentoTemp.get("value");
				}catch(Exception e)
				{
					try {
						desTemp = ((Long)descuentoTemp.get("value")).doubleValue();
						
					}catch(Exception e1)
					{
						desTemp = 0.0;
					}
	
				}
				//Convertimos el valor del porcentajeRappi
				double porcentajeRappi = 0.0;
				try {
					porcentajeRappi = ((Long)descuentoTemp.get("percentage_by_rappi")).doubleValue();
				}catch(Exception e)
				{
					try {
						porcentajeRappi = (double)descuentoTemp.get("percentage_by_rappi");
						
					}catch(Exception e1)
					{
						porcentajeRappi = 0.0;
					}
	
				}
				//Convertimos el valor del porcentajePropio
				double porcentajePropio = 0.0;
				try {
					porcentajePropio = ((Long)descuentoTemp.get("percentage_by_partners")).doubleValue();
				}catch(Exception e)
				{
					try {
						porcentajePropio = (double)descuentoTemp.get("percentage_by_partners");
						
					}catch(Exception e1)
					{
						porcentajePropio = 0.0;
					}
	
				}
				//Realizamos validación para que si el descuento viene del service fee y es PAGO-ONLINE no lo tenga en cuenta
				if(descripcion.equals(new String("30%OFF Service Fee")) && idFormaPago == 3)
				{
					
				}else
				{
					descuentoPedido = descuentoPedido + desTemp;
					descuentoPropio = descuentoPropio  +  (desTemp*porcentajePropio)/100;
					descuentoPlataforma = descuentoPlataforma  +  (desTemp*porcentajeRappi)/100;
				}
			}
			//Hacemos validación de rappiCreditos con los descuentos
			if(rappiCreditos > 0)
			{
				descuentoPlataforma = descuentoPlataforma + rappiCreditos;
				descuentoPedido = descuentoPedido + rappiCreditos;
			}
			//Debemos insertar la marcación del pedido RAPPI
			MarcacionPedido marPedido = new MarcacionPedido(idPedido, 2, Long.toString(idOrdenComercio), descuentoPropio, motivoDescuento, marketPlace, descuentoAsumido, descuentoPlataforma,tarifaServicio,propina,log);
			MarcacionPedidoDAO.InsertarMarcacionPedido(marPedido);
			//Realizamos un cambio temporal para evitar las diferencias pero igual seguirán llegando los correos
			FinalizarPedidoTiendaVirtual(idPedido, idFormaPago, idCliente, tiempoPedido, "S", descuentoPedido, "DESCUENTOS GENERALES DIARIOS", (valorTotalContact), pedidoProgramado, horaProgramado, idEstadoPedido);
		}catch(Exception e)
		{
			Correo correo = new Correo();
			CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
			ArrayList correos = new ArrayList();
			correo.setAsunto("RAPPI ERROR CREANDO PEDIDO RAPPI  " + idOrdenComercio);
			String correoEle = "jubote1@gmail.com";
			correos.add(correoEle);
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje(" Se tiene un problema creando el pedido de RAPPI número  " + idOrdenComercio + " " + e.toString());
			ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
			System.out.println(e.toString());
		}
		return("");
	}
	
	public String insertarPedidoDIDI(String stringJSON, int idLog)
	{
		String log = "";
		int idPedidoCreado = 0;
		BigInteger idOrdenComercio = new BigInteger("0");
		long numeroPedidoDiario = 0;
		long tipoDelivery = 0;
		String appId = "";
		String appShopId = "";
		int idTienda = 0;
		//Variable que le asignará un id único al pedido de tienda virtual que llega
		int idInterno = 0;
		//Variable donde almacenaremos el idCliente
		int idCliente = 0;
		//Variable para almacenar el precio total
		long valorTotal = 0;
		long valorTotalContact = 0;
		String estadoPedido = "";
		boolean esProgramado = false;
		//Variable donde se almacenará la hora de programado del pedido con S o N y la hora de programado
		//Inicializamos ambas variables con los valores 
		String programado = "N";
		String horaProgramado = "AHORA";
		String pedidoProgramado = "N";
		//Variable donde almacenaremos el tipoPedido
		String tipoPedido = "";
		int idTipoPedido = 1;
		//Formamos la fecha del pedido que podrá cambiar si el pedido es posfechado
		Date fechaPedido = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("dd/MM/yyyy");
		String strFechaFinal = formatoFinal.format(fechaPedido);
		double propina = 0;
		double rappiCreditos = 0;
		double rappiPay = 0;
		double tarifaServicio = 0;
		double descuentoDomicilio = 0;
		//Comenzaremos a parsear el JSON que nos llego
		JSONParser parser = new JSONParser();
		ParametrosCtrl parCtrl = new ParametrosCtrl();
		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();
		try
		{
			//Realizamos el parseo del primer nivel del JSON
			Object objParser = parser.parse(stringJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			//Descomponemos el tipo de orden para ver si es un tema de un pedido nuevo
			String tipoOrden = (String)jsonGeneral.get("type").toString();
			appShopId = (String)jsonGeneral.get("app_shop_id");
			if(tipoOrden.equals(new String("orderNew")))
			{
				//Descomponemos la información en order_detail,customer,store
				String data = (String)jsonGeneral.get("data").toString();
				Object objParserData = parser.parse(data);
				JSONObject jsonData = (JSONObject) objParserData;
				//Comenzamos a extraer la informacion que allí viene
				idOrdenComercio = new BigInteger((String)(jsonData.get("order_id").toString()));
				boolean yaExisteOrden = PedidoDAO.consultarExistenciaOrdenDidi(idOrdenComercio);
				if(yaExisteOrden)
				{
					Correo correo = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
					ArrayList correos = new ArrayList();
					correo.setAsunto("DIDI ERROR CREANDO PEDIDO DUPLICADO DIDI  " + idOrdenComercio);
					String correoEle = "jubote1@gmail.com";
					correos.add(correoEle);
					correos.add("lidercontactcenter@pizzaamericana.com.co");
					correo.setContrasena(infoCorreo.getClaveCorreo());
					correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
					correo.setMensaje(" Se tiene un problema creando el pedido duplicado de DIDI número  " + idOrdenComercio);
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					//contro.enviarCorreo();
					return("");
				}
				appId = ((Long)jsonGeneral.get("app_id")).toString();
				//Obtenemos la tienda de la cual proviene la integración
				idTienda = IntegracionCRMDAO.obtenerIdTiendaIntegracion(appShopId);
				String orderInfo = (String)jsonData.get("order_info").toString();
				Object objParserOrderInfo = parser.parse(orderInfo);
				JSONObject jsonOrderInfo = (JSONObject) objParserOrderInfo;
				String origenPedido = "DID";
				//Hacemos el procesamiento de la información del cliente
				//Continuamos con el procesamiento del pedido
				JSONObject jsonReceiveAddress = (JSONObject) jsonOrderInfo.get("receive_address");
				numeroPedidoDiario = (long) jsonOrderInfo.get("order_index");
				try
				{
					tipoDelivery = (long) jsonOrderInfo.get("delivery_type");
				}catch(Exception e)
				{
					tipoDelivery = 1;
				}
				
				//PROCESAMIENTO DE DATOS PERSONALES
				String telefono = "";
				
				
				//El telefono le quitaremos el indicativo del país
				if(tipoDelivery == 1)
				{
					telefono = numeroPedidoDiario + "4444";
				}else
				{
					telefono =(String)jsonReceiveAddress.get("phone");
				}
				String telefonoCelular = "";
				if(telefono.substring(0, 3).equals(new String("+57")))
				{
					telefono = telefono.substring(3);
				}
				//Para el caso del teléfono validaremos si es un fijo
				if(telefono.trim().length() == 7)
				{
					//En este caso al ser un número fijo, le agregaremos el 604 que es como es almacenado en el sistema de 
					//contact center.
					telefono = "604" + telefono;
				}else
				{
					telefonoCelular = telefono;
				}
				String nombres;
				try {
					if(tipoDelivery == 1)
					{
						nombres = numeroPedidoDiario + (String)jsonReceiveAddress.get("first_name");
					}else
					{
						nombres = (String)jsonReceiveAddress.get("first_name");
					}
					nombres = nombres.replaceAll("'", " ");
				}catch(Exception enombre)
				{
					nombres = "NO SE PUDO EXTRAR EL NOMBRE";
				}
				String apellidos;
				try {
					apellidos = (String)jsonReceiveAddress.get("last_name");
					apellidos = apellidos.replaceAll("'", " ");
					if(apellidos.length() > 30)
					{
						apellidos = "SIN APELLIDOS";
					}
				}catch(Exception enombre)
				{
					apellidos = "NO SE PUDO EXTRAR EL APELLIDO";
				}
				String email  = (String)jsonReceiveAddress.get("email");
				if(email == null)
				{
					email = "";
				}
				
				//PROCESAMIENTO DE DATOS DE UBICACIÓN
				String dirRes = "";
				String ciudad = "";
				try
				{
					dirRes = (String)jsonReceiveAddress.get("poi_address");
					dirRes = dirRes.replaceAll("'", " ");
				}catch(Exception e)
				{
					dirRes = "";
				}
				//Trabajamos sobre la ciudad
				try
				{
					ciudad = (String)jsonReceiveAddress.get("city");
					ciudad = ciudad.replaceAll("'", " ");
					if(ciudad == null)
					{
						ciudad = "";
					}
				}catch(Exception e)
				{
					ciudad = "";
				}
				if(dirRes.length() > 200)
				{
					dirRes = dirRes.substring(0,200);
				}
				
				
				String direccion = "";
				String obsDireccion = "";
				String dirAdicional = "";
				direccion = direccion.replaceAll("'", " ");
				//Tendremos unas reglas para conformar la dirección
				if(dirAdicional.equals(new String("")))
				{
					direccion = numeroPedidoDiario + "-" + dirRes + " " + ciudad;
					obsDireccion = "";
				}else
				{
					direccion = numeroPedidoDiario + "-" + dirRes + " " + ciudad;
					obsDireccion = dirAdicional;
				}
				//Validamos si la dirección nos da
				if(direccion.length() > 200)
				{
					direccion = direccion.substring(200);
				}
				
				double latitud = 0, longitud = 0;
				try
				{
					latitud = (double)jsonReceiveAddress.get("poi_lat");
				}catch(Exception e1)
				{
					latitud = 0;
				}
				try
				{
					longitud = (double)jsonReceiveAddress.get("poi_lng");
				}catch(Exception e1)
				{
					longitud = 0;
				}
				//Realizamos la intervención para tratar en el momentoen que la ubicación viene en cero
				if(tipoDelivery != 1)
				{
					if(latitud== 0 && longitud == 0)
					{
						UbicacionCtrl  ubicacion = new UbicacionCtrl();
						Ubicacion ubicaResp = ubicacion.ubicarDireccionEnTiendaBatch(direccion + " " + ciudad);
						latitud = ubicaResp.getLatitud();
						longitud = ubicaResp.getLongitud();
					}
				}
				
				//INCLUSIÓN Y PROCESAMIENTO DE LA INFORMACIÓN DEL CLIENTE
				//REALIZAMOS PROCESAMIENTO DEL CLIENTE CON SU DIRECCIÓN
				Cliente clienteVirtual = new Cliente(0, telefono, nombres, direccion, "", obsDireccion,"", idTienda);
				clienteVirtual.setApellidos(apellidos);
				clienteVirtual.setEmail(email);
				clienteVirtual.setIdtienda(idTienda);
				clienteVirtual.setLatitud((float)latitud);
				clienteVirtual.setLontitud((float)longitud);
				clienteVirtual.setTelefonoCelular(telefonoCelular);
				clienteVirtual.setPoliticaDatos("S");
				//Realizamos la validación del cliente con toda la lógica en la capa Controladora de Cliente
				ClienteCtrl clienteCtrl = new ClienteCtrl();
				//Aprovecharemos que los objetos se pasan como valores por referencia por lo tanto las modificaciones realizadas al objeto tendrán mucho que ver
				idCliente = clienteCtrl.validarClienteTiendaVirtualKuno(clienteVirtual, "");
				if(idCliente == 0)
				{
					Correo correo = new Correo();
					CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
					ArrayList correos = new ArrayList();
					correo.setAsunto("ERROR GRAVE CLIENTE NO CREADO ACTUALIZADO  " + idOrdenComercio);
					String correoEle = "jubote1@gmail.com";
					correos.add(correoEle);
					correos.add("lidercontactcenter@pizzaamericana.com.co");
					correo.setContrasena(infoCorreo.getClaveCorreo());
					correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
					correo.setMensaje(" Se tiene un problema creando actualizando cliente DIDI número  " + idOrdenComercio);
					ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
					contro.enviarCorreo();
				}
				
				//PROCESAMIENTO DEL DETALLE DEL PEDIDO
				JSONArray detalleOrdenArray = (JSONArray)jsonOrderInfo.get("order_items");
				int idProducto = 0;
				int idEspecialidad = 0;
				int idEspecialidad2= 0;
				int idExcepcion = 0;
				//Insertamos el encabezado del pedido y del valor del domicilio si ES EL CASO
				int idPedido = PedidoDAO.InsertarEncabezadoPedidoDIDI(idTienda, idCliente, strFechaFinal, "DIDI",idOrdenComercio, 1, "DID", "DIDI");
				if(tipoDelivery != 1)
				{
					int idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual("Valor del domicilio plataforma");
					double valorDomicilio = ProductoDAO.retornarProducto(idProductoDomicilio).getPreciogeneral();
					DetallePedido detPedidoDomi = new DetallePedido(idProductoDomicilio,idPedido,1,0,0,valorDomicilio,valorDomicilio*1, "" , "" /*observacion*/, 0, 0, "", "");
					PedidoDAO.InsertarDetallePedido(detPedidoDomi);
				}
				for(int i = 0; i < detalleOrdenArray.size(); i++)
				{
					idEspecialidad = 0;
					idEspecialidad2 = 0;
					JSONObject productoTemp = (JSONObject) detalleOrdenArray.get(i);
					//Tomamos el nombre del producto
					String nombreProducto = (String) productoTemp.get("name");
					//Buscaremos la manera de homologar con el nombre en donde homologaríamos producto, sabor y es promoción, crearemos una tabla para este fin.
					HomologacionProductoRappi homProducto = HomologacionProductoRappiDAO.obtenerHomologacionProductoRappi(nombreProducto);
					idProducto = homProducto.getIdProducto();
					idEspecialidad = homProducto.getIdEspecialidad();
					if(homProducto.getIdExcepcion() > 0)
					{
						idExcepcion = homProducto.getIdExcepcion();
					}else
					{
						idExcepcion = 0;
					}
					//Calcular el valor unitario
					double valorUnitario = homProducto.getPrecio();
					int cantidad = new Long((long)productoTemp.get("amount")).intValue();
					double douCantidad = (double)cantidad;
					//Necesitamos extraer la información de la gaseosa
					JSONArray subItemsOrdenArray = (JSONArray)productoTemp.get("sub_item_list");
					//En caso de no encontrar el producto es porque es Mitad y Mitad
					if(idProducto == 0)
					{
						boolean primerEspecialidad = false, segundaEspecialidad = false;
						HomologacionProductoRappi homProductoMitad;
						for(int j = 0; j < subItemsOrdenArray.size(); j++)
						{
							JSONObject subProductoTemp = (JSONObject) subItemsOrdenArray.get(j);
							String nombreEspecialidad = (String)subProductoTemp.get("name");
							homProductoMitad = HomologacionProductoRappiDAO.obtenerHomologacionProductoRappi(nombreProducto + " " + nombreEspecialidad);
							if(homProductoMitad.getIdProducto() > 0)
							{
								if(!primerEspecialidad)
								{
									idProducto = homProductoMitad.getIdProducto();
									idEspecialidad = homProductoMitad.getIdEspecialidad();
									//Calcular el valor unitario
									valorUnitario = homProductoMitad.getPrecio();
									primerEspecialidad = true;
								}else if(!segundaEspecialidad)
								{
									idEspecialidad2 = homProductoMitad.getIdEspecialidad();
									//Calcular el valor unitario
									segundaEspecialidad = true;
								}
							}
						}
					}
					//Deberemos de tener una variante para promociones que son agrupadas en una sola opción
					//Con el nombre de la promoción tendremos el idproducto, el idexcepción y en idespecialidad un -1 que nos 
					//indicará que se debe procesar las especialidades
					if(idProducto > 0 && idEspecialidad == -1 )
					{
						boolean primerEspecialidad = false, segundaEspecialidad = false;
						HomologacionProductoRappi homProductoMitad;
						for(int j = 0; j < subItemsOrdenArray.size(); j++)
						{
							JSONObject subProductoTemp = (JSONObject) subItemsOrdenArray.get(j);
							String nombreEspecialidad = (String)subProductoTemp.get("name");
							homProductoMitad = HomologacionProductoRappiDAO.obtenerHomologacionProductoRappi(nombreEspecialidad);
							if(homProductoMitad.getIdEspecialidad() > 0)
							{
								if(!primerEspecialidad)
								{
						
									idEspecialidad = homProductoMitad.getIdEspecialidad();
									primerEspecialidad = true;
								}else if(!segundaEspecialidad)
								{
									idEspecialidad2 = homProductoMitad.getIdEspecialidad();
									segundaEspecialidad = true;
								}
							}
						}
					}
					int idSaborTipoLiquido = 0;
					for(int j = 0; j < subItemsOrdenArray.size(); j++)
					{
						JSONObject subProductoTemp = (JSONObject) subItemsOrdenArray.get(j);
						String nombreGaseosa = (String)subProductoTemp.get("name");
						idSaborTipoLiquido = parCtrl.homologarLiquidoTiendaVirtual(nombreGaseosa);
						if(idSaborTipoLiquido != 0)
						{
							break;
						}
					}
					DetallePedido detPedido = new DetallePedido();
					if(idEspecialidad > 0 && idEspecialidad2 == 0)
					{
						detPedido = new DetallePedido(idProducto,idPedido,douCantidad,idEspecialidad,idEspecialidad,valorUnitario,valorUnitario*cantidad, "" /*strAdiciones*/ , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, "" /*strCON*/, "");
					}else if(idEspecialidad > 0 && idEspecialidad2 >0)
					{
						//Cuando la pizza es mitad y mitad hay que validar si tiene un valor extra
						double valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto))/2;
						double valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto))/2;
						valorUnitario = valorUnitario + valorAdicional + valorAdicional2;
						detPedido = new DetallePedido(idProducto,idPedido,douCantidad,idEspecialidad,idEspecialidad2,valorUnitario,valorUnitario*cantidad, "" /*strAdiciones*/ , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, "" /*strCON*/, "");
					}else if(idEspecialidad == 0 && idEspecialidad2 ==0)
					{
						detPedido = new DetallePedido(idProducto,idPedido,douCantidad,0,0,valorUnitario,valorUnitario*cantidad, "" /*strAdiciones*/ , "" /*observacion*/, idSaborTipoLiquido, idExcepcion, "" /*strCON*/, "");
					}
					int idDetallePedido = 0;
					if(detPedido.getIdproducto() == 0)
					{
						Correo correo = new Correo();
						CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
						ArrayList correos = new ArrayList();
						correo.setAsunto("ERROR GRAVE HOMOLOGACIÓN PRODUCTO DIDI EN PEDIDO  " + idOrdenComercio);
						String correoEle = "jubote1@gmail.com";
						correos.add(correoEle);
						correos.add("lidercontactcenter@pizzaamericana.com.co");
						correo.setContrasena(infoCorreo.getClaveCorreo());
						correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
						correo.setMensaje(" Se tiene un problema creando el pedido duplicado de DIDI número  " + idOrdenComercio + " en la homologación de producto.");
						ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
						contro.enviarCorreo();
					}else
					{
						idDetallePedido = PedidoDAO.InsertarDetallePedido(detPedido);
					}
					//Realizamos modificación para agregar los acompañantes adicionales de productos incluidos
					if(idDetallePedido > 0)
					{
						for(int j = 0; j < productosIncluidos.size(); j++)
						{
							ProductoIncluido proIncTemp = productosIncluidos.get(j);
							if(proIncTemp.getIdproductopadre() == detPedido.getIdproducto())
							{
								//Realizamos una modificación para que la cantidad a incluir se debe multiplicar por la cantidad de productos adicionados
								DetallePedido detPedidoInc = new DetallePedido(proIncTemp.getIdproductohijo(),idPedido,proIncTemp.getCantidad()*cantidad,0,0,0,0, "" , "Producto Incluido-"+idDetallePedido, 0, 0 /*idexcepcion*/, "", "");
								PedidoDAO.InsertarDetallePedido(detPedidoInc);
							}
						}
					}
				}
				
				
				//PROCESAMIENTO DE OTROS CAMPOS
				//PROPINA
				JSONObject price = (JSONObject)jsonOrderInfo.get("price");
				//Tratamos de detectar si hay descuento de domicilio y lo quitamos en la factura, dado que si lo dan es llevandolo Didi
				try
				{
					descuentoDomicilio = (long)price.get("delivery_discount")/100;
				}catch(Exception e)
				{
					descuentoDomicilio = 0;
				}
				JSONObject other_fees = (JSONObject)price.get("others_fees");
				try
				{
					propina = ((long)other_fees.get("tip")/100);
				}catch(Exception e)
				{
					propina = 0;
				}
				int idProductoPropina = 0;
				if(propina > 0)
				{
					idProductoPropina = parCtrl.homologarProductoTiendaVirtual("Propina");
					DetallePedido detPedidoPropina = new DetallePedido(idProductoPropina,idPedido,1,0,0,propina,propina*1, "" , "" /*observacion*/, 0, 0, "", "");
					PedidoDAO.InsertarDetallePedido(detPedidoPropina);
				}
				//FORMA DE PAGO
				//Trabajamos con la extracción y homologación de la forma de pago
				int idFormaPago = 0;
				String formPago = "";
				if(tipoDelivery == 1)
				{
					formPago = "1-DIDI";
				}else
				{
					formPago = ((Long)jsonOrderInfo.get("pay_type")).toString()+"-DIDI";
				}
				idFormaPago = parCtrl.realizarHomologacionFormaPagoTiendaVirtual(formPago);
				if(idFormaPago == 0)
				{
					idFormaPago = 3;
					log = log + " " + "No se pudo homologar la forma de pago fue puesto en PAGO-ONLINE, por favor revisar.";
				}
				//PROCESAMIENTO FINAL DEL PEDIDO
				int idEstadoPedido = 2;
				//Posteriormente realizamos los pasos para la finalización del pedido
				int tiempoPedido = TiempoPedidoDAO.retornarTiempoPedidoTienda(idTienda);
				double descuentoPedido = 0;
				double descuentoPropio = 0;
				double descuentoPlataforma = 0;
				String descuentoAsumido = "N";
				String marketPlace;
				if(tipoDelivery == 1)
				{
					marketPlace = "N";
				}else
				{
					marketPlace = "S";
				}
				String motivoDescuento = "";
				JSONArray descuentosArray = (JSONArray)jsonOrderInfo.get("promotions");
				//Realizamos Programación para temas de descuentos
				if(descuentosArray == null)
				{
					
				}else
				{
					for(int k = 0; k < descuentosArray.size(); k++)
					{
						JSONObject descuentoTemp = (JSONObject) descuentosArray.get(k);
						//Convertimos el valor del descuento 
						Double desTemp = 0.0;
						try {
							desTemp = (double)((long)descuentoTemp.get("promo_discount")/100);
						}catch(Exception e)
						{
							desTemp = 0.0;
						}
						//Traemos el valor del descuento subsidiado por la tienda
						Double desSubTienda = 0.0;
						try {
							desSubTienda = (double)((long)descuentoTemp.get("shop_subside_price")/100);
						}catch(Exception e)
						{
							desSubTienda = 0.0;
						}
						descuentoPedido = descuentoPedido + desTemp;
						descuentoPropio = descuentoPropio  +  desSubTienda;
						descuentoPlataforma = descuentoPlataforma  +  (desTemp - desSubTienda);
					}
				}
				//Le quitaremos si hay descuento de domicilio
				descuentoPedido = descuentoPedido - descuentoDomicilio;
				//Le quitaremos el descuento de domicilio si lo hay
				descuentoPlataforma = descuentoPlataforma - descuentoDomicilio;
				//Debemos insertar la marcación del pedido RAPPI
				MarcacionPedido marPedido = new MarcacionPedido(idPedido, 1, idOrdenComercio.toString(), descuentoPropio, motivoDescuento, marketPlace, descuentoAsumido, descuentoPlataforma,tarifaServicio,propina,log);
				MarcacionPedidoDAO.InsertarMarcacionPedido(marPedido);
				//Realizamos un cambio temporal para evitar las diferencias pero igual seguirán llegando los correos
				FinalizarPedidoTiendaVirtual(idPedido, idFormaPago, idCliente, tiempoPedido, "S", descuentoPedido, "DESCUENTOS GENERALES DIARIOS", (valorTotalContact), pedidoProgramado, horaProgramado, idEstadoPedido);
				
			}else if(tipoOrden.equals(new String("deliveryStatus")))
			{
				//Descomponemos la información en order_detail,customer,store
				String data = (String)jsonGeneral.get("data").toString();
				Object objParserData = parser.parse(data);
				JSONObject jsonData = (JSONObject) objParserData;
				//Comenzamos a extraer la informacion que allí viene
				idOrdenComercio = new BigInteger((String)(jsonData.get("order_id").toString()));
				long deliveryStatus = (long)(jsonData.get("delivery_status"));
				String nombreDomi = "";
				String telDomi = "";
				//&& (appShopId.equals(new String("5")) || appShopId.equals(new String("2")))
				if(deliveryStatus == 140 )
				{
					nombreDomi = (String)(jsonData.get("rider_name").toString());
					telDomi = (String)(jsonData.get("rider_phone").toString());
					Pedido pedEvento = PedidoDAO.ConsultaPedidoXOrden(idOrdenComercio);
					//Información para hacer el llamado al servicio en la tienda
					String respuesta = "";
					//Realizamos la invocación mediante el uso de HTTPCLIENT
					HttpClient client = HttpClientBuilder.create().build();
					//Recuperamos la tienda que requerimos trabajar con el servicio
					Tienda tienda = TiendaDAO.obtenerTienda(pedEvento.getTienda().getIdTienda());
					if (tienda != null)
					{
						//Realizar invocación de servicio en tienda
						String rutaURL = tienda.getUrl() + "DarSalidaDomicilioPlataforma?idpedido=" + pedEvento.getNumposheader() + "&idusuario=180&usuario=Caja";
						HttpGet request = new HttpGet(rutaURL);
						try
						{
							StringBuffer retorno = new StringBuffer();
							StringBuffer retornoTienda = new StringBuffer();
							//Se realiza la ejecución del servicio de finalizar pedido
							HttpResponse responseFinPed = client.execute(request);
							BufferedReader rd = new BufferedReader
								    (new InputStreamReader(
								    		responseFinPed.getEntity().getContent()));
							String line = "";
							while ((line = rd.readLine()) != null) {
								    retorno.append(line);
								}
							System.out.println(retorno);
							respuesta = retorno.toString();
						}catch(Exception e)
						{
							System.out.println(e.toString());
						}
					}
					if(respuesta.equals(new String("")))
					{
						JSONObject resultado = new JSONObject();
						resultado.put("resultado", "error");
						resultado.put("tipo_error", "error servicio tienda");
					} //&& (appShopId.equals(new String("5")) || appShopId.equals(new String("2")))
				}else if(deliveryStatus == 160 )
				{
					nombreDomi = (String)(jsonData.get("rider_name").toString());
					telDomi = (String)(jsonData.get("rider_phone").toString());
					Pedido pedEvento = PedidoDAO.ConsultaPedidoXOrden(idOrdenComercio);
					//Información para hacer el llamado al servicio en la tienda
					String respuesta = "";
					//Realizamos la invocación mediante el uso de HTTPCLIENT
					HttpClient client = HttpClientBuilder.create().build();
					//Recuperamos la tienda que requerimos trabajar con el servicio
					Tienda tienda = TiendaDAO.obtenerTienda(pedEvento.getTienda().getIdTienda());
					if (tienda != null)
					{
						//Realizar invocación de servicio en tienda
						String rutaURL = tienda.getUrl() + "DarEntregaDomicilio?idpedidotienda=" + pedEvento.getNumposheader() + "&claveusuario=2&idtienda=" + tienda.getIdTienda() + "&observacion=PedidoEntregadoPorPlataforma";
						HttpGet request = new HttpGet(rutaURL);
						try
						{
							StringBuffer retorno = new StringBuffer();
							StringBuffer retornoTienda = new StringBuffer();
							//Se realiza la ejecución del servicio de finalizar pedido
							HttpResponse responseFinPed = client.execute(request);
							BufferedReader rd = new BufferedReader
								    (new InputStreamReader(
								    		responseFinPed.getEntity().getContent()));
							String line = "";
							while ((line = rd.readLine()) != null) {
								    retorno.append(line);
								}
							System.out.println(retorno);
							respuesta = retorno.toString();
						}catch(Exception e)
						{
							System.out.println(e.toString());
						}
					}
					if(respuesta.equals(new String("")))
					{
						JSONObject resultado = new JSONObject();
						resultado.put("resultado", "error");
						resultado.put("tipo_error", "error servicio tienda");
					}
				}
			}else if(tipoOrden.equals(new String("orderCancel")))
			{
				//Descomponemos la información en order_detail,customer,store
				String data = (String)jsonGeneral.get("data").toString();
				Object objParserData = parser.parse(data);
				JSONObject jsonData = (JSONObject) objParserData;
				//Comenzamos a extraer la informacion que allí viene
				idOrdenComercio = new BigInteger((String)(jsonData.get("order_id").toString()));
				String notificacion = "*CUIDADO DIDI se ha cancelado la orden número  " + idOrdenComercio + " por favor revisar el estado de la orden.*" ;
				//Recuperaremos los celulares para notificar de la situación de la queja
				ArrayList telNotifica = GeneralDAO.obtenerCorreosParametro("NOTIFICAPQRSBOT");
				for(int i = 0; i  < telNotifica.size(); i++)
				{
					String telTemp = (String) telNotifica.get(i);
					notificarWhatsAppUltramsg(telTemp, notificacion);
				}
			}
		}catch(Exception e)
		{
			System.out.println("Se presentaron problemas en la descomposión del JSON inicial" + e.toString());
		}
		//Preparamos la respuesta para que DIDI no mande más eventos de Pedidos
		JSONObject respJSON  = new JSONObject();
		respJSON.put("errno", 0);
		respJSON.put("errmsg", "ok");
		System.out.println(respJSON.toJSONString());
		return(respJSON.toJSONString());
	}
	
	public  String obtenerEstadisticasPromocion(String fechaInicial, String fechaFinal, boolean consolFecha, boolean consolTienda, int idTienda)
	{
		ArrayList<EstadisticaPromocion> estadisticas = EstadisticaPromocionDAO.obtenerEstadisticasPromocion(fechaInicial, fechaFinal, consolFecha, consolTienda, idTienda);
		JSONObject tempJSON = new JSONObject();
		JSONArray respuesta = new JSONArray();
		for(EstadisticaPromocion estTemp: estadisticas)
		{
			tempJSON = new JSONObject();
			tempJSON.put("fecha", estTemp.getFecha());
			tempJSON.put("idtienda", estTemp.getIdTienda());
			tempJSON.put("idpromocion", estTemp.getIdPromocion());
			tempJSON.put("tiendavirtual", estTemp.getTiendaVirtual());
			tempJSON.put("contact", estTemp.getContact());
			tempJSON.put("total", estTemp.getTotal());
			tempJSON.put("promocion", estTemp.getPromocion());
			respuesta.add(tempJSON);
		}
		return(respuesta.toJSONString());
	}
	
	public  String obtenerEstadisticasPlataformas(String fechaInicial, String fechaFinal)
	{
		ArrayList<Estadistica> estadisticas = PedidoDAO.obtenerEstadisticasPlataforma(fechaInicial, fechaFinal);
		JSONObject tempJSON = new JSONObject();
		JSONArray respuesta = new JSONArray();
		for(Estadistica estTemp: estadisticas)
		{
			tempJSON = new JSONObject();
			tempJSON.put("fuente", estTemp.getEstadistica());
			tempJSON.put("cantidadpedidos", estTemp.getCantidad());
			tempJSON.put("totalventa", estTemp.getTotal());
			respuesta.add(tempJSON);
		}
		return(respuesta.toJSONString());
	}
	
	public String reemplazarFechaProgCRMBOT(String fecha)
	{
		fecha = fecha.replace(".", "/");
		fecha = fecha.replace("-", "/");
		return(fecha);
	}
	
	
	public String aceptarPedidoIntegracionRAPPI(long idOrdenComercio) throws IOException
	{
		JSONObject respJSON = new JSONObject();
		boolean respuesta = false;
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("RAPPI");
		String strBody = "";
		OkHttpClient client = new OkHttpClient();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, strBody );
		Request request = new Request.Builder()
		  .url("https://services.rappi.com/api/v2/restaurants-integrations-public-api/orders/"+ idOrdenComercio +"/take")
		  .put(body)
		  .addHeader("x-authorization", "bearer " + intCRM.getAccessToken())
		  .build();
		try
		{
			okhttp3.Response response = client.newCall(request).execute();
			String respuestaJSON = response.body().string();
			System.out.println("1 " + response.toString());
			String respFinal = response.toString().replace("Response", "");
			System.out.println("2 " + respFinal);
			respJSON.put("respuesta", "OK");
			//Marcamos en el sistema el pedido como ACEPTADO EN RAPPI
			PedidoDAO.marcarEnviadoPedidoRappi(idOrdenComercio);
			
//			JSONParser parser = new JSONParser();
//			
//			Object objParser = parser.parse(respFinal);
//			JSONObject jsonGeneral = (JSONObject) objParser;
//			long codigoRespuesta = (long)jsonGeneral.get("code");
//			if(codigoRespuesta == 200)
//			{
//				respuesta = true;
//			}
		}catch(Exception e)
		{
			System.out.println(e.toString());
			respJSON.put("respuesta", "NOK");
		}
		return(respJSON.toJSONString());
	}
	
	/**
	 * Método que se encarga de aceptar el pedido en la plataforma de DIDI, una vez ingresa por la API
	 * @param idOrdenComercio
	 * @return
	 * @throws IOException
	 */
	public String aceptarPedidoIntegracionDIDI(BigInteger idOrdenComercio) throws IOException
	{
		JSONObject respJSON = new JSONObject();
		boolean respuesta = false;
		//Obtejemos la información del Pedido
		Pedido pedCons = PedidoDAO.ConsultaPedidoXOrden(idOrdenComercio);
		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracionXTienda("DIDI", pedCons.getTienda().getIdTienda());
		//Creamos el JSON para consumir el servicio
		String jsonData = "{  \"auth_token\": \""  + intCRM.getAccessToken() + "\",\n"
				+ "  \"order_id\": "+ idOrdenComercio +"\n"
				+ "}";
		//Realizamos la invocación mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURLDIDI = "https://openapi.didi-food.com/v1/order/order/confirm";
		HttpPost request = new HttpPost(rutaURLDIDI);
		try
		{
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			//Fijamos los parámetros
			//pass the json string request in the entity
		    HttpEntity entity = new ByteArrayEntity(jsonData.getBytes("UTF-8"));
		    request.setEntity(entity);
			//request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			String datosJSON = retorno.toString();
			System.out.println(datosJSON);
			
			//Los datos vienen en un arreglo, debemos de tomar el primer valor como lo hacemos en la parte gráfica
			JSONParser parser = new JSONParser();
			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;
			boolean resultado = (boolean)jsonGeneral.get("data");
			if(resultado)
			{
				PedidoDAO.marcarEnviadoPedidoDidi(idOrdenComercio);
				respJSON.put("respuesta", "OK");
			}else
			{
				respJSON.put("respuesta", "NOK");
			}
		}catch(Exception e)
		{
			respJSON.put("respuesta", "NOK");
			System.out.println(e.toString());
		}
		return(respJSON.toJSONString());
	}
	
	public String consultarPedidosPendientesRAPPI(String fecha)
	{
		boolean respuesta = PedidoDAO.consultarPedidosPendientesRAPPI(fecha);
		JSONObject respJSON = new JSONObject();
		respJSON.put("resultado", respuesta);
		return(respJSON.toJSONString());
	}
	
	public void envioCorreoNotificacion(String correo, String asunto, String mensaje)
	{
		Correo correoError = new Correo();
		CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
		ArrayList correos = new ArrayList();
		correoError.setAsunto(asunto);
		correos.add(correo);
		correoError.setContrasena(infoCorreo.getClaveCorreo());
		correoError.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
		correoError.setMensaje(mensaje);
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correoError, correos);
		contro.enviarCorreo();
	}
	
	/**
	 * Método que nos servirá como base para el envío del correo de la tarjeta regalo
	 */
	public void envioCorreoTarjetaRegalo()
	{
		String cuentaCorreo = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
		String claveCorreo = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
		String imagenWompi = ParametrosDAO.retornarValorAlfanumerico("IMAGENPAGOWOMPI");
		Correo correo = new Correo();
		correo.setAsunto("PIZZA AMERICANA - NUEVA TARJETA REGALO");
		correo.setContrasena(claveCorreo);
		correo.setUsuarioCorreo(cuentaCorreo);
		String mensajeCuerpoCorreo = "" 
				+ "<!DOCTYPE html>\n"
				+ "<html lang=\"en\">\n"
				+ "\n"
				+ "<head>\n"
				+ "  <meta charset=\"UTF-8\">\n"
				+ "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
				+ "  <title>Pizza Americana | Tarjeta de regalo</title>\n"
				+ "  <link rel=\"shortcut icon\" href=\"https://pizzaamericana.co/wp-content/uploads/2023/07/logo.png\">\n"
				+ "</head>\n"
				+ "\n"
				+ "<body>\n"
				+ "  <div  style=\"background-color:#dadada\">\n"
				+ "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"\n"
				+ "    style=\"border-collapse:collapse;border-spacing:0px;width:100%;height:100%;background:repeat center top;margin:0;padding:0;\">\n"
				+ "    <tbody>\n"
				+ "      <tr>\n"
				+ "        <td valign=\"top\" style=\"margin:0;padding:0\">\n"
				+ "\n"
				+ "          <table cellspacing=\"0\" cellpadding=\"0\" align=\"center\"\n"
				+ "            style=\"border-collapse:collapse;border-spacing:0px;table-layout:fixed!important;width:100%\">\n"
				+ "            <tbody>\n"
				+ "              <tr>\n"
				+ "                <td align=\"center\" style=\"margin:0;padding:0\">\n"
				+ "                  <table cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#fff\" align=\"center\"\n"
				+ "                    style=\"border-collapse:collapse;border-spacing:0px;width:700px\">\n"
				+ "                    <tbody>\n"
				+ "                      <tr>\n"
				+ "                        <td align=\"left\" style=\"margin:0;padding:0\">\n"
				+ "                          <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" role=\"presentation\"\n"
				+ "                            style=\"border-collapse:collapse;border-spacing:0px\">\n"
				+ "                            <tbody>\n"
				+ "                              <tr>\n"
				+ "                                <td align=\"center\" style=\"font-size:0px;margin:0;padding:0\"><a href=\"\"\n"
				+ "                                    style=\"text-decoration:underline;color:#2cb543;font-size:14px\" target=\"_blank\">\n"
				+ "                                    <img src=\"https://pizzaamericana.co/wp-content/uploads/2023/07/head.jpeg\" alt=\"\"\n"
				+ "                                      style=\"display:block;outline:none;text-decoration:none;border:0\" width=\"700\"\n"
				+ "                                      title=\"Descubre nuestras marcas\"></a></td>\n"
				+ "                              </tr>\n"
				+ "                            </tbody>\n"
				+ "                          </table>\n"
				+ "                        </td>\n"
				+ "                      </tr>\n"
				+ "                      <tr>\n"
				+ "                        <td align=\"center\" bgcolor=\"#00bdaf\"\n"
				+ "                          style=\"background-color:#12296f;margin:0;padding:0px 0px 0px 0px\"><img src=\"https://pizzaamericana.co/wp-content/uploads/2023/07/centro.jpeg\"\n"
				+ "                            alt=\"\" style=\"display:block;outline:none;text-decoration:none;border:0\" width=\"695\"\n"
				+ "                            >\n"
				+ "                          <table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\"\n"
				+ "                            style=\"border-collapse:collapse;border-spacing:0px\">\n"
				+ "                            <tbody>\n"
				+ "                              <tr>\n"
				+ "                                <td align=\"left\" style=\"width:650px;margin:0;padding:0\">\n"
				+ "                                  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" role=\"presentation\"\n"
				+ "                                    style=\"border-collapse:collapse;border-spacing:0px\">\n"
				+ "\n"
				+ "                              </tr>\n"
				+ "                          </table>\n"
				+ "                        </td>\n"
				+ "                      </tr>\n"
				+ "                    </tbody>\n"
				+ "                  </table>\n"
				+ "                </td>\n"
				+ "              </tr>\n"
				+ "\n"
				+ "              <tr>\n"
				+ "                <td align=\"left\" bgcolor=\"#fff\" style=\"margin:0;padding:0 0 50px\">\n"
				+ "                  <table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\"\n"
				+ "                    style=\"border-collapse:collapse;border-spacing:0px\">\n"
				+ "                    <tbody>\n"
				+ "                      <tr>\n"
				+ "                        <td align=\"center\" style=\"width:700px;margin:0;padding:0\">\n"
				+ "                          <table  cellspacing=\"0\" cellpadding=\"0\" role=\"presentation\"\n"
				+ "                            style=\"border-collapse:collapse;border-spacing:0px\">\n"
				+ "                            <tbody>\n"
				+ "                              <tr>\n"
				+ "                                <td align=\"left\" bgcolor=\"#D00000\"\n"
				+ "                                  style=\"background-color:#12296f;padding:20px 20px 60px 20px;\">\n"
				+ "\n"
				+ "                                  <table cellspacing=\"0\" cellpadding=\"0\" align=\"left\"\n"
				+ "                                    style=\"border-collapse:collapse;border-spacing:0px;float:left\">\n"
				+ "                                    <tbody>\n"
				+ "                                      <tr>\n"
				+ "                                        <td align=\"left\" style=\"width:315px;margin:0;padding:0\">\n"
				+ "                                          <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#D00000\"\n"
				+ "                                            style=\"border-collapse:separate;border-spacing:0px;border-radius:8px\"\n"
				+ "                                            role=\"presentation\">\n"
				+ "                                            <tbody>\n"
				+ "                                              <tr>\n"
				+ "                                                <td align=\"left\" style=\"margin:0;padding:10px 0\">\n"
				+ "                                                  <table width=\"100%\"\n"
				+ "                                                    style=\"border-collapse:collapse;border-spacing:0px;border-radius:8px\"\n"
				+ "                                                    role=\"presentation\">\n"
				+ "                                                    <tbody>\n"
				+ "                                                      <tr>\n"
				+ "                                                        <td style=\"margin:0;padding:20px 20px 20px 0px\" align=\"center\">\n"
				+ "                                                          <img src=\"https://pizzaamericana.co/wp-content/uploads/2023/07/disponible.png\" alt=\"10%\"\n"
				+ "                                                            style=\"display:block;outline:none;text-decoration:none;font-size:12px;border:0\"\n"
				+ "                                                            width=\"300\" title=\"10%\" ></td>\n"
				+ "\n"
				+ "                                                      </tr>\n"
				+ "                                                    </tbody>\n"
				+ "                                                  </table>\n"
				+ "                                                </td>\n"
				+ "                                              </tr>\n"
				+ "                                              <tr>\n"
				+ "                                                <td align=\"left\" style=\"margin:0;padding:0 10px\">\n"
				+ "                                                  <table width=\"100%\"\n"
				+ "                                                    style=\"border-collapse:collapse;border-spacing:0px;border-radius:8px\"\n"
				+ "                                                    role=\"presentation\">\n"
				+ "                                                    <tbody>\n"
				+ "                                                      <tr>\n"
				+ "                                                        <td style=\"color:#ffffff;margin:0;padding:0\" align=\"center\">\n"
				+ "                                                          <table width=\"100%\"\n"
				+ "                                                            style=\"border-collapse:collapse;border-spacing:0px;border-radius:8px\"\n"
				+ "                                                            role=\"presentation\" bgcolor=\"#D00000\">\n"
				+ "                                                            <tbody>\n"
				+ "                                                              <tr>\n"
				+ "\n"
				+ "                                                              </tr>\n"
				+ "                                                            </tbody>\n"
				+ "                                                          </table>\n"
				+ "                                                        </td>\n"
				+ "                                                      </tr>\n"
				+ "                                                    </tbody>\n"
				+ "                                                  </table>\n"
				+ "                                                </td>\n"
				+ "                                              </tr>\n"
				+ "                                              <tr>\n"
				+ "\n"
				+ "                                              </tr>\n"
				+ "                                            </tbody>\n"
				+ "                                          </table>\n"
				+ "                                        </td>\n"
				+ "                                      </tr>\n"
				+ "                                    </tbody>\n"
				+ "                                  </table>\n"
				+ "\n"
				+ "                                  <table cellpadding=\"0\" cellspacing=\"0\" align=\"right\"\n"
				+ "                                    style=\"border-collapse:collapse;border-spacing:0px;float:right\">\n"
				+ "                                    <tbody>\n"
				+ "                                      <tr>\n"
				+ "                                        <td align=\"left \" style=\"width:315px;margin:0;padding:0\">\n"
				+ "                                          <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#D00000\"\n"
				+ "                                            style=\"border-collapse:separate;border-spacing:0px;border-radius:8px\"\n"
				+ "                                            role=\"presentation\">\n"
				+ "                                            <tbody>\n"
				+ "                                              <tr>\n"
				+ "                                                <td align=\"left\" style=\"margin:0;padding:0px\">\n"
				+ "                                                  <table width=\"100%\"\n"
				+ "                                                    style=\"border-collapse:collapse;border-spacing:0px;border-radius:8px\"\n"
				+ "                                                    role=\"presentation\">\n"
				+ "                                                    <tbody>\n"
				+ "                                                      <tr>\n"
				+ "                                                        <td style=\"margin:0;padding:0px 0px 0px 0px\" width=\"90px;\"\n"
				+ "                                                          align=\"center\"><img src=\"https://pizzaamericana.co/wp-content/uploads/2023/07/invitacion.png\" alt=\"10%\"\n"
				+ "                                                            style=\"display:block;outline:none;text-decoration:none;font-size:12px;border:0\"\n"
				+ "                                                            width=\"300\" title=\"10%\" ></td>\n"
				+ "\n"
				+ "                                                      </tr>\n"
				+ "                                                    </tbody>\n"
				+ "                                                  </table>\n"
				+ "                                                </td>\n"
				+ "                                              </tr>\n"
				+ "                                              <tr>\n"
				+ "                                                <td align=\"left\" style=\"margin:0;padding:0 10px\">\n"
				+ "                                                  <table width=\"100%\"\n"
				+ "                                                    style=\"border-collapse:collapse;border-spacing:0px;border-radius:8px\"\n"
				+ "                                                    role=\"presentation\">\n"
				+ "                                                    <tbody>\n"
				+ "                                                      <tr>\n"
				+ "                                                        <td style=\"color:#ffffff;margin:0;padding:0\" align=\"center\">\n"
				+ "                                                          <table width=\"100%\"\n"
				+ "                                                            style=\"border-collapse:collapse;border-spacing:0px;border-radius:8px\"\n"
				+ "                                                            role=\"presentation\" bgcolor=\"#333333\">\n"
				+ "                                                            <tbody>\n"
				+ "                                                              <tr>\n"
				+ "\n"
				+ "                                                              </tr>\n"
				+ "                                                            </tbody>\n"
				+ "                                                          </table>\n"
				+ "                                                        </td>\n"
				+ "                                                      </tr>\n"
				+ "                                                    </tbody>\n"
				+ "                                                  </table>\n"
				+ "                                                </td>\n"
				+ "                                              </tr>\n"
				+ "                                              <tr>\n"
				+ "\n"
				+ "                                              </tr>\n"
				+ "                                            </tbody>\n"
				+ "                                          </table>\n"
				+ "                                        </td>\n"
				+ "                                      </tr>\n"
				+ "                                    </tbody>\n"
				+ "                                  </table>\n"
				+ "\n"
				+ "                                </td>\n"
				+ "                              </tr>\n"
				+ "                              <tr>\n"
				+ "                                <td align=\"center\" style=\"font-size:0px;margin:0;padding:0\"><a href=\"\"\n"
				+ "                                    style=\"text-decoration:underline;color:#2cb543;font-size:14px\" target=\"_blank\">\n"
				+ "                                    <img src=\"https://pizzaamericana.co/wp-content/uploads/2023/07/tarjeta.png\" alt=\"\"\n"
				+ "                                      style=\"display:block;outline:none;text-decoration:none;border:0\" width=\"700\"\n"
				+ "                                      ></a></td>\n"
				+ "                              </tr>\n"
				+ "                            </tbody>\n"
				+ "                          </table>\n"
				+ "                        </td>\n"
				+ "                      </tr>\n"
				+ "                    </tbody>\n"
				+ "                  </table>\n"
				+ "                </td>\n"
				+ "              </tr>\n"
				+ "\n"
				+ "\n"
				+ "            </tbody>\n"
				+ "          </table>\n"
				+ "        </td>\n"
				+ "      </tr>\n"
				+ "    </tbody>\n"
				+ "  </table>\n"
				+ "  <table cellspacing=\"0\" cellpadding=\"0\" align=\"center\"\n"
				+ "    style=\"border-collapse:collapse;border-spacing:0px;table-layout:fixed!important;width:100%;background:repeat center top\"\n"
				+ "    bgcolor=\"transparent\">\n"
				+ "    <tbody>\n"
				+ "      <tr>\n"
				+ "        <td align=\"center\" style=\"margin:0;padding:0\">\n"
				+ "          <table cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#ffffff\" align=\"center\"\n"
				+ "            style=\"border-collapse:collapse;border-spacing:0px;width:700px\">\n"
				+ "            <tbody>\n"
				+ "\n"
				+ "              <tr>\n"
				+ "                <td\n"
				+ "                  style=\"border-bottom-width:1px;border-bottom-color:#d4d4d4;border-bottom-style:solid;height:1px;width:100%;margin:0px;padding:0\">\n"
				+ "                </td>\n"
				+ "              </tr>\n"
				+ "              <tr>\n"
				+ "                <td align=\"left\" style=\"margin:0;padding:25px\">\n"
				+ "                  <table cellpadding=\"0\" cellspacing=\"0\" align=\"right\"\n"
				+ "                    style=\"border-collapse:collapse;border-spacing:0px;float:right\">\n"
				+ "                    <tbody>\n"
				+ "                      <tr>\n"
				+ "                        <td align=\"center\" style=\"margin:0px;padding:0\"><a href=\"https://www.facebook.com/pizzaamericana.co/\"\n"
				+ "                            style=\"text-decoration:underline;color:#ffffff;font-size:14px\" target=\"_blank\"\n"
				+ "                            data-saferedirecturl=\"https://www.google.com/url?q=https://clicks.dafiti.com.br/f/a/rf0LPIGXdtGETFTSXh6zNQ~~/AAQRxQA~/RgRmh8RPP0QoaHR0cHM6Ly93d3cuZmFjZWJvb2suY29tL0RhZml0aUNvbG9tYmlhL1cDc3BjQgpknFA_pWRr8JDKUhNpc2Fnb21leDFAZ21haWwuY29tWAQAAAAX&amp;source=gmail&amp;ust=1688823890066000&amp;usg=AOvVaw1ocBcnSwRNwIiUe1Lh0zkz\"><img\n"
				+ "                              src=\"https://ci3.googleusercontent.com/proxy/mu-6DMoAJnxMFkDL9kaIk8CRcttJbOlzsgzSUhJMCXIS9Ilh4oNh0qiLhtSVTFcs0LCGV9sRCq7t7Eh70sKwG-eEKFwyHOrqrUVLyRwEnJ6vbZccSreMu71nAYX_-u7voJsg7oDYPt5YwsmhA0NnQb9GR5svs63X-Y8W2qyrHAGLA9ZtoCw1Qh1jNxY=s0-d-e1-ft#https://braze-images.com/appboy/communication/assets/image_assets/images/63852800a6e49e028b8a226f/original.png?1669670912\"\n"
				+ "                              alt=\"\" style=\"display:block;outline:none;text-decoration:none;border:0\" width=\"35\"\n"
				+ "                              ></a></td>\n"
				+ "                        <td align=\"center\" style=\"margin:0;padding:0\"><a href=\"https://www.instagram.com/pizzaamericana.co/\"\n"
				+ "                            style=\"text-decoration:underline;color:#ffffff;font-size:14px\" target=\"_blank\"\n"
				+ "                            data-saferedirecturl=\"https://www.google.com/url?q=https://clicks.dafiti.com.br/f/a/vEeFigdToJ0a3DRMKx3abA~~/AAQRxQA~/RgRmh8RPP0QqaHR0cHM6Ly93d3cuaW5zdGFncmFtLmNvbS9kYWZpdGlfY29sb21iaWEvVwNzcGNCCmScUD-lZGvwkMpSE2lzYWdvbWV4MUBnbWFpbC5jb21YBAAAABc~&amp;source=gmail&amp;ust=1688823890066000&amp;usg=AOvVaw05bTRB_XKRN_E7FbsBGze0\"><img\n"
				+ "                              src=\"https://ci3.googleusercontent.com/proxy/w2vXwr7Z5-cC_q9e-cb0fBvZBGifjsSqMO9silKMWsDZUroVShWW4a6_QNltUVYTnPSqe9Qh_yUAW6x4UsnDkW9GgrTsgMhxY3iexyzFY0A99Rcq0y1Y0Q6btTVG5YZaHsKLEMKSFB0LHycmqKLMNsGS64UkXkxODxziFGc9R7PXIzepfWK7WbEcMAs=s0-d-e1-ft#https://braze-images.com/appboy/communication/assets/image_assets/images/6385280009c09932b81ed700/original.png?1669670912\"\n"
				+ "                              alt=\"\" style=\"display:block;outline:none;text-decoration:none;border:0\" width=\"35\"\n"
				+ "                              ></a></td>\n"
				+ "\n"
				+ "                      </tr>\n"
				+ "                    </tbody>\n"
				+ "                  </table>\n"
				+ "\n"
				+ "                </td>\n"
				+ "              </tr>\n"
				+ "              <tr>\n"
				+ "                <td align=\"center\" bgcolor=\"#000\"\n"
				+ "                  style=\"margin:0;padding:10px 25px;color:white;font-family:arial,'helvetica neue',helvetica,sans-serif;font-size:15px\">\n"
				+ "                  <strong>CONDICIONES Y RESTRICCIONES</strong></td>\n"
				+ "              </tr>\n"
				+ "              <tr>\n"
				+ "                <td align=\"left\" style=\"margin:0;padding:25px;background-color:#ffd53d;\">\n"
				+ "                  <table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\"\n"
				+ "                    style=\"border-collapse:collapse;border-spacing:0px\">\n"
				+ "                    <tbody>\n"
				+ "                      <tr>\n"
				+ "                        <td align=\"left\"\n"
				+ "                          style=\"color:#1d1c1c;margin:0;padding:0;font-family:arial,'helvetica neue',helvetica,sans-serif;font-size:18px\">\n"
				+ "\n"
				+ "                          <ul>\n"
				+ "                            <li>La tarjeta válida al portador.</li>\n"
				+ "                            <li>Puedes redimir nuestra tarjeta de regalo en nuestro Contact Center <strong>(604 4444\n"
				+ "                                553)</strong> o a través de nuestras tiendas físicas.</li>\n"
				+ "                            <li>Al momento de realizar la compra debes indicar que la forma de pago será nuestra Tarjeta\n"
				+ "                              Pizza Americana.</li>\n"
				+ "                            <li>La tarjeta tendrá una validez de 1 año, a partir de la fecha de compra.</li>\n"
				+ "                            <li>Si tu pedido es a domicilio asegúrese de indicar esta forma de pago dado que su tarjeta\n"
				+ "                              deberá ser validada en el datáfono.</li>\n"
				+ "                            <li>Puedes recargar esta tarjeta nuevamente en cualquiera de nuestras tiendas fisicas,\n"
				+ "                              cuando el saldo inicial se acabe.</li>\n"
				+ "                          </ul>\n"
				+ "                        </td>\n"
				+ "                      </tr>\n"
				+ "                    </tbody>\n"
				+ "\n"
				+ "                  </table>\n"
				+ "                </td>\n"
				+ "              </tr>\n"
				+ "            </tbody>\n"
				+ "          </table>\n"
				+ "        </td>\n"
				+ "      </tr>\n"
				+ "    </tbody>\n"
				+ "  </table>\n"
				+ "  </td>\n"
				+ "  </tr>\n"
				+ "  </tbody>\n"
				+ "  </table>\n"
				+ "</div>\n"
				+ "</body>\n"
				+ "\n"
				+ "</html>";
		correo.setMensaje(mensajeCuerpoCorreo);
		//FIJACIÓN DE ENVÍO DE CORREO
		ArrayList correos = new ArrayList();
		//String correoEle = "leidyjtorog@gmail.com";
		ArrayList correosEnviar = PedidoDAO.obtenerCorreosEnvioMasivoCorreo();
		String correoTemporal = "";
		ControladorEnvioCorreo contro;
		for(int i = 0; i < correosEnviar.size(); i++)
		{
			correoTemporal = (String)correosEnviar.get(i);
			correos = new ArrayList();
			correos.add(correoTemporal);
			contro = new ControladorEnvioCorreo(correo, correos);
			contro.enviarCorreo();
			//Realizar un delay de 15 segundos para no hacer un envío masivo de correos
			try {
	            //Ponemos a "Dormir" el programa durante los ms que queremos
	            Thread.sleep(15*1000);
	         } catch (Exception e) {
	            System.out.println(e);
	         }
		}
	}
	
	/**
	 * Métod que se encargará de refrescar el auth_token en la plataforma de DIDI, este método se realiza mediante el llamado
	 * GET por lo tanto los parámetros van en la URL, se realizan 2 llamados el primero de refrescar el token y el segundo de tomar el auth_token y almacenarlo
	 * @param plataforma
	 * @throws IOException
	 */
	public void actualizarTokenIntegracionDIDI(String plataforma) throws IOException
	{
		ArrayList<IntegracionCRM> integraciones = IntegracionCRMDAO.obtenerInformacionIntegracionMultiple(plataforma);
		IntegracionCRM intCRM;
		for(int i = 0; i < integraciones.size(); i++)
		{
			intCRM = integraciones.get(i);
			String rutaURL = "https://openapi.didi-food.com/v1/auth/authtoken/refresh?app_id="+ intCRM.getClientID() +"&app_shop_id=" + intCRM.getAppShopID() + "&app_secret=" + intCRM.getFreshToken();
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(rutaURL);
			try
			{
				StringBuffer retorno = new StringBuffer();
				HttpResponse response = client.execute(request);
				BufferedReader rd = new BufferedReader
					    (new InputStreamReader(
					    response.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					    retorno.append(line);
					}
				String respuestaJSON = retorno.toString();
				JSONParser parser = new JSONParser();
				Object objParser = parser.parse(respuestaJSON);
				JSONObject jsonGeneral = (JSONObject) objParser;
				String error = (String)jsonGeneral.get("errno").toString();
				String authToken = "";
				//Si se cumple esta condición es porque no hubo error refrescando el token
				if(error.equals(new String("0")))
				{
					rutaURL = "https://openapi.didi-food.com/v1/auth/authtoken/get?app_id="+ intCRM.getClientID() +"&app_shop_id=" + intCRM.getAppShopID() + "&app_secret=" + intCRM.getFreshToken();;
					client = HttpClientBuilder.create().build();
					HttpGet request2 = new HttpGet(rutaURL);
					try
					{
						StringBuffer retorno2 = new StringBuffer();
						HttpResponse response2 = client.execute(request2);
						BufferedReader rd2 = new BufferedReader
							    (new InputStreamReader(
							    response2.getEntity().getContent()));
						String line2 = "";
						while ((line2 = rd2.readLine()) != null) {
							    retorno2.append(line2);
							}
						String respuestaJSON2 = retorno2.toString();
						objParser = parser.parse(respuestaJSON2);
						jsonGeneral = (JSONObject) objParser;
						String data = (String)jsonGeneral.get("data").toString();
						objParser = parser.parse(data);
						JSONObject dataJSON = (JSONObject) objParser;
						authToken =(String)dataJSON.get("auth_token");
					}catch(Exception e2)
					{
						e2.printStackTrace();
						System.out.println("Hubo un problema obteniendo el token de actualización de DiDi");
					}
					IntegracionCRMDAO.actualizarTokenIntegracionCRMMultiple(plataforma,intCRM.getIdTienda(), authToken, intCRM.getFreshToken());
				}
			}catch(Exception e)
			{
				e.printStackTrace();
				System.out.println("Hubo un problema refrescando el Token de DIDI");
			
		}
		}
	
	}
	
	public String obtenerPedidosMonitoreoPagoVirtual()
	{
		JSONArray pedRespuesta = new JSONArray();
		JSONObject pedResTemp = new JSONObject();
		ArrayList<PedidoMonitoreoPagoVirtual> pedidosMonitoreo = PedidoDAO.obtenerPedidosMonitoreoPagoVirtual();
		PedidoMonitoreoPagoVirtual pedTemp;
		for(int i = 0; i < pedidosMonitoreo.size();i++)
		{
			pedTemp = pedidosMonitoreo.get(i);
			pedResTemp = new JSONObject();
			pedResTemp.put("idpedido", pedTemp.getIdPedido());
			pedResTemp.put("tienda", pedTemp.getTienda());
			pedResTemp.put("nombre", pedTemp.getNombre());
			pedResTemp.put("telefono", pedTemp.getTelefono());
			pedResTemp.put("telefonocelular", pedTemp.getTelefonoCelular());
			pedResTemp.put("email", pedTemp.getEmail());
			pedResTemp.put("totalneto", pedTemp.getTotalNeto());
			pedResTemp.put("idlink", pedTemp.getIdLink());
			pedResTemp.put("fechainsercion", pedTemp.getFechaInsercion());
			pedResTemp.put("minutos", pedTemp.getMinutos());
			pedResTemp.put("idcliente", pedTemp.getIdCliente());
			pedResTemp.put("idformapago", pedTemp.getIdFormaPago());
			pedRespuesta.add(pedResTemp);
		}
		
		return(pedRespuesta.toJSONString());
	}
	
	public  String consultarLogEventoWompi(String idLink)
	{
		JSONArray eventosJSON = new JSONArray();
		JSONObject eventoTemp = new JSONObject();
		ArrayList<LogEventoWompi> eventos = LogEventoWompiDAO.consultarLogEventoWompi(idLink);
		LogEventoWompi logEventoTemp;
		for(int i = 0; i < eventos.size();i++)
		{
			logEventoTemp = eventos.get(i);
			eventoTemp = new JSONObject();
			eventoTemp.put("idlink", logEventoTemp.getIndLink());
			eventoTemp.put("fechahora", logEventoTemp.getFechaHora());
			eventoTemp.put("evento", logEventoTemp.getEvento());
			eventoTemp.put("estado", logEventoTemp.getEstado());
			eventosJSON.add(eventoTemp);
		}
		return(eventosJSON.toJSONString());
	}
	
	public String ingresarObsGestionLink(int idPedido, String observacion)
	{
		PedidoGestionLinkDAO.ingresarObsGestionLink(idPedido, observacion);
		JSONObject res = new JSONObject();
		res.put("resultado", "OK");
		return(res.toJSONString());
	}
	
	public String marcarPedidoEmpresarial(int idpedido)
	{
		JSONObject respuesta = new JSONObject();
		boolean resp = PedidoDAO.marcarPedidoEmpresarial(idpedido);
		respuesta.put("resultado", resp);
		return(respuesta.toJSONString());
	}
	
	public void verificacionExistenciaClienteSalesManago(String correoCliente)
	{
		String jsonInfo = "{"
				+ "  \"clientId\": \"8bff4c80ffe4b78a\","
				+ "  \"apiKey\": \"jfHf8cOqtwcgYW2r4Top\","
				+ "  \"requestTime\":  1704218302,\r\n"
				+ "  \"sha\": \"c8ed0c44ee57ee5fc1eee36de659db051c7a0f66\","
				+ "  \"owner\": \"tecnologia@pizzaamericana.com.co\","
				+ "    \"email\": \""+ correoCliente +"\""
				+ "}";
		//Realizamos la invocacion mediante el uso de HTTPCLIENT
		HttpClient client = HttpClientBuilder.create().build();
		String rutaURLSales = "https://app2.salesmanago.pl/api/contact/hasContact ";
		HttpPost request = new HttpPost( rutaURLSales);
		try
		{
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json;charset=UTF-8");
			//Fijamos los parametros
			//pass the json string request in the entity
		    HttpEntity entity = new ByteArrayEntity(jsonInfo.getBytes("UTF-8"));
		    request.setEntity(entity);
			//request.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			StringBuffer retorno = new StringBuffer();
			HttpResponse responseFinPed = client.execute(request);
			BufferedReader rd = new BufferedReader
				    (new InputStreamReader(
				    		responseFinPed.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				    retorno.append(line);
				}
			//Traemos el valor del JSON con toda la info del pedido
			String datosJSON = retorno.toString();
			System.out.println("RESULTADO RESPUESTA " + datosJSON);
			//Los datos vienen en un arreglo, debemos de tomar el primer valor como lo hacemos en la parte grafica
//			JSONParser parser = new JSONParser();
//			Object objParser = parser.parse(datosJSON);
//			JSONObject jsonGeneral = (JSONObject) objParser;
//			String dataJSON = (String)jsonGeneral.get("data").toString();
//			Object objParserData = parser.parse(dataJSON);
//			JSONObject jsonData = (JSONObject) objParserData;
//			String idLink = (String)jsonData.get("id");

		}catch (Exception e2) {
            e2.printStackTrace();
            System.out.println(e2.toString());
        }
	}

}

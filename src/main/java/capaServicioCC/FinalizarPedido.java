package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * Cierra el pedido en el central y arma el JSON que la tienda necesita para recibirlo.
 *
 * Ruta: /ProyectoPizzaAmericana/FinalizarPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer), programado (string), validadir (string),
 * motivodescuento (string), idformapago (integer), valorformapago (number), valortotal (number),
 * idcliente (integer), insertado (integer), tiempopedido (number), descuento (number), tiendakuno
 * (string).
 * Devuelve: application/json;charset=UTF-8; claves: apellidos, apiKey, bloqueado, cantidad,
 * cantidaditempedido, clientId, cliente, clientesiniden, contactEvent, contactExtEventType, date,
 * descuento, ....
 * Lee: adicion_detalle_pedido, cliente, detalle_pedido, especialidad, excepcion_precio,
 * forma_pago, homologacion_forma_pago, homologacion_producto, integracion_crm, marcacion.
 * Escribe: pedido (UPDATE).
 * Sistemas externos: GloriaFood/Kuno, Rappi, SALESmanago.
 *
 * Trampas y riesgos conocidos:
 * - El usuariopedido que viaja a la tienda es siempre CONTACT, no el usuario real.
 * - pedido_forma_pago.valortotal es el que mando el cliente de la API, mientras que
 *   pedido.total_neto es la suma menos descuento: pueden no coincidir (es una de las causas de
 *   descuadres de forma de pago).
 * - No valida que el pedido este en estado 1, que tenga renglones ni que el descuento sea menor
 *   que el total (un descuento mayor deja un total negativo).
 * - Si validadir falta, el servicio falla con una excepcion (usa validaDir.equals) DESPUES de
 *   haber cerrado el pedido en la base: el pedido queda finalizado pero el navegador recibe un
 *   error y no tiene el JSON para la tienda.
 * - No es idempotente por el lado del cliente, pero el turno de envio protege del doble envio.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha FinalizarPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/FinalizarPedido")
public class FinalizarPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FinalizarPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocar� el m�todo de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		int idpedido = Integer.parseInt(request.getParameter("idpedido"));
		//Un pedido a medio tomar que se retomo en esta pantalla (Pedidos.html?reanudar=NNN) esta retenido a
		//nombre de quien lo trabaja; el primer paso de FinalizarPedido pide turno de envio y ese turno se
		//niega a los retenidos. Se suelta SOLO si la retencion es de edicion y es de esta misma sesion: la de
		//otra persona se respeta, y un pedido nuevo -sin retencion- pasa de largo sin cambios.
		try {
			final capaDAOCC.RetencionPedidoDAO.Estado retencion = capaDAOCC.RetencionPedidoDAO.consultar(idpedido);
			final String quien = EditarPedidoEnCurso.usuarioDeLaSesion(request);
			if (retencion.retenido && capaDAOCC.RetencionPedidoDAO.POR_EDICION.equals(retencion.motivo)
					&& quien.length() > 0 && quien.equals(retencion.por == null ? "" : retencion.por.trim())) {
				capaDAOCC.RetencionPedidoDAO.liberar(idpedido);
			}
		} catch (Exception e) {
			//Si no se pudo mirar, se sigue como siempre: el turno de envio decide.
		}
		int idformapago;
		int idcliente;
		int insertado;
		double valorformapago;
		double valortotal;
		double tiempopedido;
		double descuento = 0;
		String validaDir;
		String motivoDescuento = "";
		String programado = request.getParameter("programado");
		if(programado == null)
		{
			programado = "";
		}
        String esProgramado = "N";
        if(programado.equals(new String("AHORA")))
        {
        	esProgramado = "N";
        }else if(programado.equals(new String("")))
        {
        	esProgramado = "N";
        }else
        {
        	esProgramado = "S";
        }
		validaDir = request.getParameter("validadir");
		motivoDescuento = request.getParameter("motivodescuento");
		try
		{
			idformapago = Integer.parseInt(request.getParameter("idformapago"));
		}catch(Exception e)
		{
			idformapago = 0;
		}
		try{
			valorformapago = Double.parseDouble(request.getParameter("valorformapago"));
		}catch(Exception e)
		{
			valorformapago = 0;
		}
		try{
			valortotal = Double.parseDouble(request.getParameter("valortotal"));
		}catch(Exception e)
		{
			valortotal = 0;
		}
		try{
			idcliente = Integer.parseInt(request.getParameter("idcliente"));
		}catch(Exception e)
		{
			idcliente = 0;
		}
		try{
			insertado = Integer.parseInt(request.getParameter("insertado"));
		}catch(Exception e)
		{
			insertado = 0;
		}
		try{
			tiempopedido = Double.parseDouble(request.getParameter("tiempopedido"));
		}catch(Exception e)
		{
			tiempopedido = 0;
		}
		try{
			descuento = Double.parseDouble(request.getParameter("descuento"));
		}catch(Exception e)
		{
			descuento = 0;
		}
		//Validamos si es tiendaKuno
		String tiendaKuno = request.getParameter("tiendakuno");
		if(tiendaKuno == null)
		{
			tiendaKuno = "";
		}
		if(tiendaKuno.equals(new String("S")))
		{
			validaDir = "N";
		}
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.FinalizarPedido(idpedido, idformapago, valorformapago, valortotal, idcliente, insertado, tiempopedido, validaDir, descuento, motivoDescuento, esProgramado, programado);
        PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

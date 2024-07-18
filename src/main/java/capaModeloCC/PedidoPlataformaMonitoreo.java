package capaModeloCC;

import java.math.BigInteger;

public class PedidoPlataformaMonitoreo {
	
	private int idPedido;
	private int idPedidoTienda;
	private String idOrdenComercio;
	private String fechaIngreso;
	private int tiempoPedido;
	private String fechaDomiciliario;
	private int tiempoSalidaDomiciliario;
	private String fechaEntregado;
	private int tiempoDomicilioPedido;
	private String fechaCancelacion;
	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	
	public int getIdPedidoTienda() {
		return idPedidoTienda;
	}
	public void setIdPedidoTienda(int idPedidoTienda) {
		this.idPedidoTienda = idPedidoTienda;
	}
	public String getFechaIngreso() {
		return fechaIngreso;
	}
	public void setFechaIngreso(String fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	
	public int getTiempoPedido() {
		return tiempoPedido;
	}
	public void setTiempoPedido(int tiempoPedido) {
		this.tiempoPedido = tiempoPedido;
	}
	public String getFechaDomiciliario() {
		return fechaDomiciliario;
	}
	public void setFechaDomiciliario(String fechaDomiciliario) {
		this.fechaDomiciliario = fechaDomiciliario;
	}
	public String getFechaEntregado() {
		return fechaEntregado;
	}
	public void setFechaEntregado(String fechaEntregado) {
		this.fechaEntregado = fechaEntregado;
	}
	public String getFechaCancelacion() {
		return fechaCancelacion;
	}
	public void setFechaCancelacion(String fechaCancelacion) {
		this.fechaCancelacion = fechaCancelacion;
	}
	public String getIdOrdenComercio() {
		return idOrdenComercio;
	}
	public void setIdOrdenComercio(String idOrdenComercio) {
		this.idOrdenComercio = idOrdenComercio;
	}
	public int getTiempoSalidaDomiciliario() {
		return tiempoSalidaDomiciliario;
	}
	public void setTiempoSalidaDomiciliario(int tiempoSalidaDomiciliario) {
		this.tiempoSalidaDomiciliario = tiempoSalidaDomiciliario;
	}
	public int getTiempoDomicilioPedido() {
		return tiempoDomicilioPedido;
	}
	public void setTiempoDomicilioPedido(int tiempoDomicilioPedido) {
		this.tiempoDomicilioPedido = tiempoDomicilioPedido;
	}
	public PedidoPlataformaMonitoreo(int idPedido, int idPedidoTienda, String idOrdenComercio, String fechaIngreso,
			int tiempoPedido, String fechaDomiciliario, int tiempoSalidaDomiciliario, String fechaEntregado,
			int tiempoDomicilioPedido, String fechaCancelacion) {
		super();
		this.idPedido = idPedido;
		this.idPedidoTienda = idPedidoTienda;
		this.idOrdenComercio = idOrdenComercio;
		this.fechaIngreso = fechaIngreso;
		this.tiempoPedido = tiempoPedido;
		this.fechaDomiciliario = fechaDomiciliario;
		this.tiempoSalidaDomiciliario = tiempoSalidaDomiciliario;
		this.fechaEntregado = fechaEntregado;
		this.tiempoDomicilioPedido = tiempoDomicilioPedido;
		this.fechaCancelacion = fechaCancelacion;
	}

	
}

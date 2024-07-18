package capaModeloCC;

import java.math.BigInteger;

public class PedidoCanceladoPlataforma {
	
	private int idPedido;
	private int idPedidoTienda;
	private String idOrdenComercio;
	private String fechaIngreso;
	private String fechaDomiciliario;
	private String fechaEntregado;
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
	public PedidoCanceladoPlataforma(int idPedido, int idPedidoTienda, String idOrdenComercio, String fechaIngreso,
			String fechaDomiciliario, String fechaEntregado, String fechaCancelacion) {
		super();
		this.idPedido = idPedido;
		this.idPedidoTienda = idPedidoTienda;
		this.idOrdenComercio = idOrdenComercio;
		this.fechaIngreso = fechaIngreso;
		this.fechaDomiciliario = fechaDomiciliario;
		this.fechaEntregado = fechaEntregado;
		this.fechaCancelacion = fechaCancelacion;
	}
	

}

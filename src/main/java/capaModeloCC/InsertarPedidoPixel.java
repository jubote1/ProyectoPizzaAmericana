package capaModeloCC;

import java.util.ArrayList;

public class InsertarPedidoPixel {
	
	private ArrayList<DetallePedidoPixel> envioPixel;
	private String dsnTienda;
	private int memcode;
	private Cliente cliente;
	private boolean indicadorAct;
	private double valorformapago;
	private int idformapagotienda;
	private String origen;
	private String programado;
	private String horaProgramado;
	private double descuento;
	private int idTipoPedido;
	
	public int getIdTipoPedido() {
		return idTipoPedido;
	}
	public void setIdTipoPedido(int idTipoPedido) {
		this.idTipoPedido = idTipoPedido;
	}
	public double getDescuento() {
		return descuento;
	}
	public void setDescuento(double descuento) {
		this.descuento = descuento;
	}
	public String getProgramado() {
		return programado;
	}
	public void setProgramado(String programado) {
		this.programado = programado;
	}
	public String getHoraProgramado() {
		return horaProgramado;
	}
	public void setHoraProgramado(String horaProgramado) {
		this.horaProgramado = horaProgramado;
	}
	public String getOrigen() {
		return origen;
	}
	public void setOrigen(String origen) {
		this.origen = origen;
	}
	public int getIdformapagotienda() {
		return idformapagotienda;
	}
	public void setIdformapagotienda(int idformapagotienda) {
		this.idformapagotienda = idformapagotienda;
	}
	public ArrayList<DetallePedidoPixel> getEnvioPixel() {
		return envioPixel;
	}
	public void setEnvioPixel(ArrayList<DetallePedidoPixel> envioPixel) {
		this.envioPixel = envioPixel;
	}
	public String getDsnTienda() {
		return dsnTienda;
	}
	public void setDsnTienda(String dsnTienda) {
		this.dsnTienda = dsnTienda;
	}
	public int getMemcode() {
		return memcode;
	}
	public void setMemcode(int memcode) {
		this.memcode = memcode;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	public boolean getIndicadorAct() {
		return indicadorAct;
	}
	public void setIndicadorAct(boolean indicadorAct) {
		this.indicadorAct = indicadorAct;
	}
	public double getValorformapago() {
		return valorformapago;
	}
	public void setValorformapago(double valorformapago) {
		this.valorformapago = valorformapago;
	}
	public InsertarPedidoPixel(ArrayList<DetallePedidoPixel> envioPixel, String dsnTienda, int memcode, Cliente cliente,
			boolean indicadorAct, double valorformapago, int idformapagotienda) {
		super();
		this.envioPixel = envioPixel;
		this.dsnTienda = dsnTienda;
		this.memcode = memcode;
		this.cliente = cliente;
		this.indicadorAct = indicadorAct;
		this.valorformapago = valorformapago;
		this.idformapagotienda = idformapagotienda;
	}
	
	public InsertarPedidoPixel()
	{
		
	}
	
}

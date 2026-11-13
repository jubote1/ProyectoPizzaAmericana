package capaModeloCC;

import java.io.Serializable;

/**
 * Modelo para las ventas corporativas/novenas atribuidas a un asesor del Contact Center.
 * Regla de Oro: Solo califica si el pedido contiene 3 o más pizzas.
 */
public class VentaAtribuidaNovenas implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idpedido;
    private int idpedidotienda;
    private int idtienda;
    private String nombreTienda;
    private int idcliente;
    private String nombreCliente;
    private String organizacion;
    private String telefono;
    private String fechaPedido;
    private String fechaContactoAsesor;
    private String usuarioAsesor;
    private int totalPizzasPedido;
    private double totalBruto;
    private double impuesto;
    private double totalNeto;
    private double comisionEstimada; // Ej. 2% o según escala comercial
    private String pizzasDetalle;
    private String estadoPedido;

    public VentaAtribuidaNovenas() {
    }

    public int getIdpedido() {
        return idpedido;
    }

    public void setIdpedido(int idpedido) {
        this.idpedido = idpedido;
    }

    public int getIdpedidotienda() {
        return idpedidotienda;
    }

    public void setIdpedidotienda(int idpedidotienda) {
        this.idpedidotienda = idpedidotienda;
    }

    public int getIdtienda() {
        return idtienda;
    }

    public void setIdtienda(int idtienda) {
        this.idtienda = idtienda;
    }

    public String getNombreTienda() {
        return nombreTienda;
    }

    public void setNombreTienda(String nombreTienda) {
        this.nombreTienda = nombreTienda;
    }

    public int getIdcliente() {
        return idcliente;
    }

    public void setIdcliente(int idcliente) {
        this.idcliente = idcliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getOrganizacion() {
        return organizacion;
    }

    public void setOrganizacion(String organizacion) {
        this.organizacion = organizacion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(String fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public String getFechaContactoAsesor() {
        return fechaContactoAsesor;
    }

    public void setFechaContactoAsesor(String fechaContactoAsesor) {
        this.fechaContactoAsesor = fechaContactoAsesor;
    }

    public String getUsuarioAsesor() {
        return usuarioAsesor;
    }

    public void setUsuarioAsesor(String usuarioAsesor) {
        this.usuarioAsesor = usuarioAsesor;
    }

    public int getTotalPizzasPedido() {
        return totalPizzasPedido;
    }

    public void setTotalPizzasPedido(int totalPizzasPedido) {
        this.totalPizzasPedido = totalPizzasPedido;
    }

    public double getTotalBruto() {
        return totalBruto;
    }

    public void setTotalBruto(double totalBruto) {
        this.totalBruto = totalBruto;
    }

    public double getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(double impuesto) {
        this.impuesto = impuesto;
    }

    public double getTotalNeto() {
        return totalNeto;
    }

    public void setTotalNeto(double totalNeto) {
        this.totalNeto = totalNeto;
    }

    public double getComisionEstimada() {
        return comisionEstimada;
    }

    public void setComisionEstimada(double comisionEstimada) {
        this.comisionEstimada = comisionEstimada;
    }

    public String getPizzasDetalle() {
        return pizzasDetalle;
    }

    public void setPizzasDetalle(String pizzasDetalle) {
        this.pizzasDetalle = pizzasDetalle;
    }

    public String getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(String estadoPedido) {
        this.estadoPedido = estadoPedido;
    }
}

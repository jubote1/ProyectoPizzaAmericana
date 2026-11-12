package capaModeloCC;

import java.io.Serializable;

/**
 * Modelo para los clientes precargados en la Campaña de Novenas y Venta Empresarial 25 Años.
 */
public class CampanaNovenasLead implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idlead;
    private int idcliente;
    private String nombreCliente;
    private String empresa;
    private String telefono;
    private String email;
    private String direccion;
    private String observacionCliente;
    private String clasificacion;
    private String criterioClasificacion;
    private int totalPizzasHistorico;
    private int totalPedidosHistorico;
    private double totalCompradoHistorico;
    private String ultimaFechaCompra;
    private String facturaElectronica;
    private String nitCedula;
    private String razonSocial;
    private String pizzasPreferidas;
    private String estadoGestion;
    private String creadoEn;

    public CampanaNovenasLead() {
    }

    public int getIdlead() {
        return idlead;
    }

    public void setIdlead(int idlead) {
        this.idlead = idlead;
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

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getObservacionCliente() {
        return observacionCliente;
    }

    public void setObservacionCliente(String observacionCliente) {
        this.observacionCliente = observacionCliente;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }

    public String getCriterioClasificacion() {
        return criterioClasificacion;
    }

    public void setCriterioClasificacion(String criterioClasificacion) {
        this.criterioClasificacion = criterioClasificacion;
    }

    public int getTotalPizzasHistorico() {
        return totalPizzasHistorico;
    }

    public void setTotalPizzasHistorico(int totalPizzasHistorico) {
        this.totalPizzasHistorico = totalPizzasHistorico;
    }

    public int getTotalPedidosHistorico() {
        return totalPedidosHistorico;
    }

    public void setTotalPedidosHistorico(int totalPedidosHistorico) {
        this.totalPedidosHistorico = totalPedidosHistorico;
    }

    public double getTotalCompradoHistorico() {
        return totalCompradoHistorico;
    }

    public void setTotalCompradoHistorico(double totalCompradoHistorico) {
        this.totalCompradoHistorico = totalCompradoHistorico;
    }

    public String getUltimaFechaCompra() {
        return ultimaFechaCompra;
    }

    public void setUltimaFechaCompra(String ultimaFechaCompra) {
        this.ultimaFechaCompra = ultimaFechaCompra;
    }

    public String getFacturaElectronica() {
        return facturaElectronica;
    }

    public void setFacturaElectronica(String facturaElectronica) {
        this.facturaElectronica = facturaElectronica;
    }

    public String getNitCedula() {
        return nitCedula;
    }

    public void setNitCedula(String nitCedula) {
        this.nitCedula = nitCedula;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getPizzasPreferidas() {
        return pizzasPreferidas;
    }

    public void setPizzasPreferidas(String pizzasPreferidas) {
        this.pizzasPreferidas = pizzasPreferidas;
    }

    public String getEstadoGestion() {
        return estadoGestion;
    }

    public void setEstadoGestion(String estadoGestion) {
        this.estadoGestion = estadoGestion;
    }

    public String getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(String creadoEn) {
        this.creadoEn = creadoEn;
    }
}

package capaModeloCC;

/**
 * Un evento especial planillado: una novena, un almuerzo de fin de ano, una
 * celebracion empresarial. Es lo que el Contact Center aparta con el cliente y
 * lo que la tienda tiene que tener listo ese dia a esa hora.
 *
 * Las fechas y horas se manejan como texto ('aaaa-mm-dd' y 'HH:mm') a proposito:
 * es lo que mandan los <input type="date"> / <input type="time"> de la pantalla y
 * lo que entiende MySQL sin conversiones de zona horaria de por medio.
 */
public class EventoEspecial {

    private int idevento;
    private String tipoEvento;
    private String nombreEvento;
    private String nombreCliente;
    private String empresa;
    private String telefono;
    private String email;
    private int idcliente;
    private int idlead;
    private String fechaEvento;
    private String horaEvento;
    private int cantidadPizzas;
    private String detallePedido;
    private int idtienda;
    private String nombreTienda;
    private String direccionEntrega;
    private double valorEstimado;
    private double anticipo;
    private String estado;
    private String observacion;
    private String usuarioRegistra;
    private String creadoEn;
    private String usuarioModifica;
    private String modificadoEn;

    public int getIdevento() { return idevento; }
    public void setIdevento(int idevento) { this.idevento = idevento; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getNombreEvento() { return nombreEvento; }
    public void setNombreEvento(String nombreEvento) { this.nombreEvento = nombreEvento; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getIdcliente() { return idcliente; }
    public void setIdcliente(int idcliente) { this.idcliente = idcliente; }

    public int getIdlead() { return idlead; }
    public void setIdlead(int idlead) { this.idlead = idlead; }

    public String getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(String fechaEvento) { this.fechaEvento = fechaEvento; }

    public String getHoraEvento() { return horaEvento; }
    public void setHoraEvento(String horaEvento) { this.horaEvento = horaEvento; }

    public int getCantidadPizzas() { return cantidadPizzas; }
    public void setCantidadPizzas(int cantidadPizzas) { this.cantidadPizzas = cantidadPizzas; }

    public String getDetallePedido() { return detallePedido; }
    public void setDetallePedido(String detallePedido) { this.detallePedido = detallePedido; }

    public int getIdtienda() { return idtienda; }
    public void setIdtienda(int idtienda) { this.idtienda = idtienda; }

    public String getNombreTienda() { return nombreTienda; }
    public void setNombreTienda(String nombreTienda) { this.nombreTienda = nombreTienda; }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public double getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(double valorEstimado) { this.valorEstimado = valorEstimado; }

    public double getAnticipo() { return anticipo; }
    public void setAnticipo(double anticipo) { this.anticipo = anticipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getUsuarioRegistra() { return usuarioRegistra; }
    public void setUsuarioRegistra(String usuarioRegistra) { this.usuarioRegistra = usuarioRegistra; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    public String getUsuarioModifica() { return usuarioModifica; }
    public void setUsuarioModifica(String usuarioModifica) { this.usuarioModifica = usuarioModifica; }

    public String getModificadoEn() { return modificadoEn; }
    public void setModificadoEn(String modificadoEn) { this.modificadoEn = modificadoEn; }
}

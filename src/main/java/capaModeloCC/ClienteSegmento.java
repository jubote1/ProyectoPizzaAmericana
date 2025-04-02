package capaModeloCC;


import java.util.Date;

public class ClienteSegmento {
    private int idCliente;
    private String nombre;
    private String telefono;
    private String nombreTienda;
    private int numeroPedidos;
    private Date fechaMaxima;
    private String email;
    private String nombreComp;

    public String getNombreComp() {
		return nombreComp;
	}
	public void setNombreComp(String nombreComp) {
		this.nombreComp = nombreComp;
	}
	// Getters y Setters
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNombreTienda() { return nombreTienda; }
    public void setNombreTienda(String nombreTienda) { this.nombreTienda = nombreTienda; }

    public int getNumeroPedidos() { return numeroPedidos; }
    public void setNumeroPedidos(int numeroPedidos) { this.numeroPedidos = numeroPedidos; }

    public Date getFechaMaxima() { return fechaMaxima; }
    public void setFechaMaxima(Date fechaMaxima) { this.fechaMaxima = fechaMaxima; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public ClienteSegmento() {
    	
    }
}

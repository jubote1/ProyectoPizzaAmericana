package capaModeloCC;

public class AlertaEntregaDom {

    private int idPedido;
    private String clave_dom;
    private String descripcion;
    private boolean error;
    private double latDom;
    private double longDom;
    private double latCli;
    private double longCli;
    private int idTienda;

    // Constructor
    public AlertaEntregaDom(int idPedido, String clave_dom, String descripcion, boolean error,
                            double latDom, double longDom, double latCli, double longCli, int idTienda) {
        this.idPedido = idPedido;
        this.clave_dom = clave_dom;
        this.descripcion = descripcion;
        this.error = error;
        this.latDom = latDom;
        this.longDom = longDom;
        this.latCli = latCli;
        this.longCli = longCli;
        this.idTienda = idTienda;
    }

    public int getIdTienda() {
		return idTienda;
	}

	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}

	// Getters y Setters
    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public String getClaveDom() {
        return clave_dom;
    }

    public void setClaveDom(String clave_dom) {
        this.clave_dom = clave_dom;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public double getLatDom() {
        return latDom;
    }

    public void setLatDom(double latDom) {
        this.latDom = latDom;
    }

    public double getLongDom() {
        return longDom;
    }

    public void setLongDom(double longDom) {
        this.longDom = longDom;
    }

    public double getLatCli() {
        return latCli;
    }

    public void setLatCli(double latCli) {
        this.latCli = latCli;
    }

    public double getLongCli() {
        return longCli;
    }

    public void setLongCli(double longCli) {
        this.longCli = longCli;
    }
}

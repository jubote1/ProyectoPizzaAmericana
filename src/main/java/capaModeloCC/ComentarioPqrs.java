package capaModeloCC;

public class ComentarioPqrs {
    private int id;
    private int idSolicitud;
    private String comentario;
    private String  fecha;
    private boolean  estado = true;

    // Constructor
    public ComentarioPqrs(int id, int idSolicitud, String comentario, String  fecha, boolean  estado) {
        this.id = id;
        this.idSolicitud = idSolicitud;
        this.comentario = comentario;
        this.fecha = fecha;
        this.estado = estado;
    }

    public ComentarioPqrs(int id, int idSolicitud, String comentario, String  fecha) {
        this.id = id;
        this.idSolicitud = idSolicitud;
        this.comentario = comentario;
        this.fecha = fecha;
    }
    
    public ComentarioPqrs() {
    	
    }

    // Getters y Setters
    public int getId() { return id; }
    public int getIdSolicitud() { return idSolicitud; }
    public String getComentario() { return comentario; }
    public String  getFecha() { return fecha; }

    public boolean isEstado() {
		return estado;
	}

	public void setEstado(boolean estado) {
		this.estado = estado;
	}

	public void setId(int id) { this.id = id; }
    public void setIdSolicitud(int idSolicitud) { this.idSolicitud = idSolicitud; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public void setFecha(String  fecha) { this.fecha = fecha; }
}

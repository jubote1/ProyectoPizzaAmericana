package capaModeloCC;

public class Lugar {
    private int id;
    private String descripcion;
    private int cantidad_vacantes; 

    public int getCantidad_vacantes() {
		return cantidad_vacantes;
	}

	public void setCantidad_vacantes(int cantidad_vacantes) {
		this.cantidad_vacantes = cantidad_vacantes;
	}

	// Constructor
    public Lugar(int id, String descripcion,int cantidad_vacantes) {
        this.id = id;
        this.descripcion = descripcion;
        this.cantidad_vacantes = cantidad_vacantes;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
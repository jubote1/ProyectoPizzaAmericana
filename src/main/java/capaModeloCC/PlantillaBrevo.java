package capaModeloCC;


public class PlantillaBrevo {
    private int idPlantilla;
    private String nombre;
    private String categoria;

    public String getCategoria() {
		return categoria;
	}
	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}
	public PlantillaBrevo(int idPlantilla, String nombre) {
        this.idPlantilla = idPlantilla;
        this.nombre = nombre;
    }
    public PlantillaBrevo() {
       
    }

    public int getIdPlantilla() {
        return idPlantilla;
    }

    public void setIdPlantilla(int idPlantilla) {
        this.idPlantilla = idPlantilla;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}

package capaModeloCC;

public class ExperienciaCandidato {
    private String empresa;
    private String cargo;
    private String ciudad;
    private String descripcion;
    private String fecha_inicio;
    private String fecha_final;
    private boolean trabajo_actual;

    // Getters y Setters

    public String getNombreEmpresa() {
        return empresa;
    }

    public void setNombreEmpresa(String nombreEmpresa) {
        this.empresa = nombreEmpresa;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaInicio() {
        return fecha_inicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fecha_inicio = fechaInicio;
    }

    public String getFechaFinalizacion() {
        return fecha_final;
    }

    public void setFechaFinalizacion(String fechaFinalizacion) {
        this.fecha_final = fechaFinalizacion;
    }

    public boolean isTrabajoActual() {
        return trabajo_actual;
    }

    public void setTrabajoActual(boolean trabajoActual) {
        this.trabajo_actual = trabajoActual;
    }
}

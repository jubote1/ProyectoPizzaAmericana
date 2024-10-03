package capaModeloCC;

import java.util.List;

public class Candidato {
    private int idvacante;
    private String nombre;
    private String barrio;
    private String cedula;
    private String genero;
    private String celular;
    private String municipio;
    private String ciudad;
    private String fecha_nacimiento;
    private String fuente_vacante;
    private boolean transporte;
    private boolean documentacion;
    private boolean sin_experiencia;
    private String hoja_vida;
    private String email;

    // Listas para formación y experiencia
    private List<FormacionCandidato> formacion;
    private List<ExperienciaCandidato> experiencia;

    // Getters y Setters

    public int getIdvacante() {
        return idvacante;
    }

    public void setIdvacante(int idvacante) {
        this.idvacante = idvacante;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getFechaNacimiento() {
        return fecha_nacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fecha_nacimiento = fechaNacimiento;
    }

    public String getFuenteVacante() {
        return fuente_vacante;
    }

    public void setFuenteVacante(String fuenteVacante) {
        this.fuente_vacante = fuenteVacante;
    }

    public boolean isTransporte() {
        return transporte;
    }

    public void setTransporte(boolean transporte) {
        this.transporte = transporte;
    }

    public boolean isDocumentacionTransporte() {
        return documentacion;
    }

    public void setDocumentacionTransporte(boolean documentacionTransporte) {
        this.documentacion = documentacionTransporte;
    }

    public boolean isSinExperiencia() {
        return sin_experiencia;
    }

    public void setSinExperiencia(boolean sinExperiencia) {
        this.sin_experiencia = sinExperiencia;
    }

    public String getHojaVida() {
        return hoja_vida;
    }

    public void setHojaVida(String hojaVida) {
        this.hoja_vida = hojaVida;
    }

    public List<FormacionCandidato> getFormacion() {
        return formacion;
    }

    public void setFormacion(List<FormacionCandidato> formacion) {
        this.formacion = formacion;
    }

    public List<ExperienciaCandidato> getExperiencia() {
        return experiencia;
    }

    public void setExperiencia(List<ExperienciaCandidato> experiencia) {
        this.experiencia = experiencia;
    }
}

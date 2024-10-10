package capaModeloCC;

import java.util.List;

public class Vacante {
    private int id;
    private String perfil;
    private String descripcion;
    private boolean estado; // Cambiado a boolean
    private String imagen;
    private String mision;
    private String salario;
    private String contrato;
    private boolean requiere_transporte;
    
    public String getContrato() {
		return contrato;
	}

	public void setContrato(String contrato) {
		this.contrato = contrato;
	}

	public boolean isRequiere_transporte() {
		return requiere_transporte;
	}

	public void setRequiere_transporte(boolean requiere_transporte) {
		this.requiere_transporte = requiere_transporte;
	}

	// Relacionados con otras tablas
    private List<Lugar>  lugares;
    private String categoria;
    private List<HorarioVacante> HorarioVacantes;
    private List<BeneficioVacante> BeneficioVacantes;

    // Constructor
    public Vacante(int id, String perfil, String descripcion, boolean estado, String imagen, String mision, String salario,String categoria,String contrato) {
        this.id = id;
        this.perfil = perfil;
        this.descripcion = descripcion;
        this.estado = estado;
        this.imagen = imagen;
        this.mision = mision;
        this.salario = salario;
        this.categoria = categoria;
        this.contrato = contrato;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

 


    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getMision() {
        return mision;
    }

    public void setMision(String mision) {
        this.mision = mision;
    }

    public String getSalario() {
        return salario;
    }

    public void setSalario(String salario) {
        this.salario = salario;
    }

    public void setLugares(List<Lugar> lugares) {
        this.lugares = lugares;
    }

    public List<Lugar> getLugares() {
        return lugares;
    }
    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Vacante() {
		super();
		// TODO Auto-generated constructor stub
	}

	public List<HorarioVacante> getHorarioVacante() {
        return HorarioVacantes;
    }

    public void setHorarioVacante(List<HorarioVacante> HorarioVacantes) {
        this.HorarioVacantes = HorarioVacantes;
    }

    public List<BeneficioVacante> getBeneficioVacante() {
        return BeneficioVacantes;
    }

    public void setBeneficioVacante(List<BeneficioVacante> BeneficioVacantes) {
        this.BeneficioVacantes = BeneficioVacantes;
    }
}

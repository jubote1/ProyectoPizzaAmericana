package capaModeloCC;

import java.io.Serializable;

/**
 * Modelo para las notas y bitácora de llamadas de los asesores del Contact Center.
 */
public class CampanaNovenasBitacora implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idbitacora;
    private int idlead;
    private int idcliente;
    private String usuarioAsesor;
    private String fechaContacto;
    private String estadoContacto; // INTERESADO, APARTADO_5%, VOLVER_A_LLAMAR, NO_INTERESADO, NO_CONTESTA
    private String fechaTentativaEvento;
    private int pizzasEstimadas;
    private String observacion;
    private String creadoEn;

    public CampanaNovenasBitacora() {
    }

    public int getIdbitacora() {
        return idbitacora;
    }

    public void setIdbitacora(int idbitacora) {
        this.idbitacora = idbitacora;
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

    public String getUsuarioAsesor() {
        return usuarioAsesor;
    }

    public void setUsuarioAsesor(String usuarioAsesor) {
        this.usuarioAsesor = usuarioAsesor;
    }

    public String getFechaContacto() {
        return fechaContacto;
    }

    public void setFechaContacto(String fechaContacto) {
        this.fechaContacto = fechaContacto;
    }

    public String getEstadoContacto() {
        return estadoContacto;
    }

    public void setEstadoContacto(String estadoContacto) {
        this.estadoContacto = estadoContacto;
    }

    public String getFechaTentativaEvento() {
        return fechaTentativaEvento;
    }

    public void setFechaTentativaEvento(String fechaTentativaEvento) {
        this.fechaTentativaEvento = fechaTentativaEvento;
    }

    public int getPizzasEstimadas() {
        return pizzasEstimadas;
    }

    public void setPizzasEstimadas(int pizzasEstimadas) {
        this.pizzasEstimadas = pizzasEstimadas;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(String creadoEn) {
        this.creadoEn = creadoEn;
    }
}

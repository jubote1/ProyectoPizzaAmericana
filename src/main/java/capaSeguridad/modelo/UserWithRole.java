package capaSeguridad.modelo;

public class UserWithRole {
    private int id;
    private String rol;

    public UserWithRole(int id, String rol) {
        this.id = id;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public String toString() {
        return "UsuarioConRol{id=" + id + ", rol='" + rol + "'}";
    }
}

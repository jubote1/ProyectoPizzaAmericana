package conexionCC;

import java.sql.*;

/**
 * Clase que se encarga de la implementación de la conexión a las bases de datos del sistema contact center y la
 * base de datos de cada tienda.
 * 
 * @author JuanDavid
 */
public class ConexionBaseDatos {

    public static void main(String args[]) {
        ConexionBaseDatos cn = new ConexionBaseDatos();
        cn.obtenerConexionBDTienda("PixelSqlbase");
    }

    /**
     * Método que implementa la conexión a la base de datos del sistema principal de contact center
     * 
     * @return Connection a la base de datos principal
     */
    public Connection obtenerConexionBDPrincipal() {
  
//	            "jdbc:mysql://172.19.0.25/pizzaamericana?"
//	            + "user=root&password=4m32017");
        return obtenerConexion("jdbc:mysql://localhost/pizzaamericana?", "root", "4m32017");
    }

    public Connection obtenerConexionBDGeolocalizacion() {
        return obtenerConexion("jdbc:mysql://localhost/geolocalizacion?", "root", "4m32017");
    }

    public Connection obtenerConexionBDGeneral() {
        return obtenerConexion("jdbc:mysql://localhost/general?", "root", "4m32017");
    }

    public Connection obtenerConexionBDInventario() {
        return obtenerConexion("jdbc:mysql://localhost/inventarioamericana?", "root", "4m32017");
    }

    /**
     * Método que se encarga de implementar la conexión a la base de datos de cada tienda
     * 
     * @param dsn Recibe como parámetro el valor del Datasource Name
     * @return Se retorna un objeto de la clase conexión.
     */
    public Connection obtenerConexionBDTienda(String dsn) {
        Connection con = null;
        try {
            DriverManager.registerDriver((Driver) Class.forName("sybase.jdbc.sqlanywhere.IDriver").getDeclaredConstructor().newInstance());
            con = DriverManager.getConnection("jdbc:sqlanywhere:dsn=" + dsn + ";uid=admin;pwd=xxx"); // SystemPos
        } catch (Exception ex) {
            System.out.println("SQLException: " + ex.toString());
        }
        return con;
    }

    public Connection obtenerConexionBDDatamartLocal() {
        return obtenerConexion("jdbc:mysql://localhost/datamart?", "root", "4m32017");
    }

    public Connection obtenerConexionBDNominaAmericana() {
        return obtenerConexion("jdbc:mysql://localhost/nominaamericana?", "root", "4m32017");
    }

    public Connection obtenerConexionBDSeguridad() {
        return obtenerConexion("jdbc:mysql://localhost/seguridad?", "root", "4m32017");
    }

    /**
     * Método auxiliar para obtener la conexión a la base de datos.
     *
     * @param url  La URL de conexión de la base de datos
     * @param user El nombre de usuario para la conexión
     * @param password La contraseña para la conexión
     * @return Connection a la base de datos
     */
    private Connection obtenerConexion(String url, String user, String password) {
        Connection con = null;
        try {
            // Se registra el driver de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Se realiza la creación de la conexión a la base de datos
            con = DriverManager.getConnection(url + "user=" + user + "&password=" + password + "&serverTimezone=UTC");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found: " + e.toString());
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }
        return con;
    }
}

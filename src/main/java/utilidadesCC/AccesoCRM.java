package utilidadesCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaModeloCC.Usuario;
import conexionCC.ConexionBaseDatos;

/**
 * Quien puede entrar al area de CRM.
 *
 * POR QUE ESTO NO ES SOLO ESCONDER LA OPCION DEL MENU
 *
 * El CRM muestra datos personales de 450 mil personas: nombre, celular, correo,
 * direcciones, que compran y cuanto gastan. Esconder el boton no protege nada,
 * porque la URL se escribe a mano y los servicios responden JSON sin pasar por
 * la pantalla. Por eso cada servicio del CRM pregunta aqui antes de contestar.
 *
 * LA MARCA VIVE EN pizzaamericana.usuario
 *
 * No en general.empleado. El central autentica contra la tabla usuario
 * -capaDAOCC.UsuarioDAO consulta "FROM usuario" con obtenerConexionBDPrincipal-;
 * general.empleado es el maestro de personal, que usa el POS, y ni siquiera
 * tiene los mismos registros: 217 empleados contra 127 usuarios. La primera
 * version de esta clase miraba la tabla equivocada y habria dejado a todo el
 * mundo por fuera.
 *
 * LA MARCA ES PROPIA Y NO EL CAMPO administrador
 *
 * usuario.acceso_crm es una columna aparte, sembrada con los 11 administradores
 * porque fue lo que se pidio. Teniendo marca propia se le puede quitar el CRM a
 * alguien sin quitarle el perfil de administrador, que le sirve para otras
 * pantallas.
 *
 * NO SE MIRA usuario.activo, y es a proposito: el login no lo mira tampoco
 * -"select * from usuario where nombre = ? and password = ?"-, y hoy 8 de los
 * 11 administradores estan en activo = 0 y entran igual. Exigirlo aqui dejaria
 * sin CRM a quien si lo usa. Que una columna "activo" no impida entrar es un
 * problema aparte, y esta anotado.
 *
 * ARRANCA CERRADO: la columna nace en 'N', asi que un usuario nuevo no queda
 * con acceso por descuido.
 */
public class AccesoCRM {

	/** Nadie construye esto. */
	private AccesoCRM() {
	}

	/**
	 * @return true solo si hay sesion viva Y ese usuario tiene acceso_crm = 'S'
	 */
	public static boolean puede(final HttpServletRequest request) {
		final String usuario = usuarioEnSesion(request);
		if (usuario == null) {
			return (false);
		}
		return (tieneAcceso(usuario));
	}

	/** El nombre de usuario de la sesion, o null si no hay sesion. */
	public static String usuarioEnSesion(final HttpServletRequest request) {
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion == null) {
				return (null);
			}
			final Usuario usuario = (Usuario) sesion.getAttribute("usuario");
			if (usuario == null || usuario.getNombreUsuario() == null
					|| usuario.getNombreUsuario().trim().length() == 0) {
				return (null);
			}
			return (usuario.getNombreUsuario().trim());
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AccesoCRM.usuarioEnSesion: " + e.toString());
			return (null);
		}
	}

	/**
	 * Se consulta cada vez y no se guarda en la sesion a proposito: si a alguien
	 * le quitan el acceso, deja de entrar en la siguiente pantalla y no cuando
	 * vuelva a iniciar sesion manana.
	 */
	public static boolean tieneAcceso(final String nombreUsuario) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		boolean puede = false;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT acceso_crm FROM usuario WHERE nombre = ?");
			ps.setString(1, nombreUsuario);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				puede = "S".equalsIgnoreCase(rs.getString("acceso_crm"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			//Si no se puede preguntar, NO se deja entrar. Ante la duda, cerrado.
			Logger.getLogger("log_file").error("AccesoCRM.tieneAcceso: " + e.toString());
			puede = false;
		} finally {
			try {
				if (cn != null) {
					cn.close();
				}
			} catch (final Exception e) {
				Logger.getLogger("log_file").error("AccesoCRM: no cerro la conexion, " + e.toString());
			}
		}
		return (puede);
	}

	/**
	 * La respuesta que devuelve un servicio del CRM cuando el usuario no tiene
	 * acceso. Se dice explicitamente para que la pantalla pueda sacar un
	 * mensaje claro en vez de quedarse cargando.
	 */
	public static String negado() {
		return ("{\"error\":\"SINACCESO\",\"mensaje\":\"Su usuario no tiene acceso al CRM.\"}");
	}
}

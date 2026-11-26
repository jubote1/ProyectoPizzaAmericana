package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import capaModeloCC.PedidoPrecioEmpleado;
import conexionCC.ConexionBaseDatos;
import utilidadesCC.ControladorEnvioCorreo;

/**
 * Servicio para gestion de codigos de precio empleado en el Contact Center:
 * - Validacion de frecuencia semanal
 * - Generacion y envio seguro de codigo OTP al correo registrado en nomina
 * - Validacion previa del codigo ingresado por el asesor
 *
 * Rutas: /GestionCodigoPrecioEmpleado, /api/precio-empleado
 */
@WebServlet({ "/GestionCodigoPrecioEmpleado", "/api/precio-empleado" })
public class GestionCodigoPrecioEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Gson GSON = new Gson();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		procesar(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		procesar(request, response);
	}

	private void procesar(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");

		String accion = request.getParameter("accion");
		if (accion == null || accion.trim().isEmpty()) {
			accion = "validar";
		}

		Map<String, Object> resp = new HashMap<String, Object>();

		try {
			if ("frecuencia".equalsIgnoreCase(accion)) {
				// 1. Validar si ya redimio en la semana
				int idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
				PedidoCtrl pedCtrl = new PedidoCtrl();
				boolean yaPidio = pedCtrl.validarFrecuenciaPedidoPrecioEmpleado(idEmpleado);

				resp.put("exito", true);
				resp.put("yaPidio", yaPidio);
				resp.put("mensaje", yaPidio
						? "El empleado ya pidió la pizza de esta semana (límite: 1 por semana)."
						: "Empleado habilitado para redimir precio empleado esta semana.");

			} else if ("enviar".equalsIgnoreCase(accion)) {
				// 2. Generar y enviar codigo OTP al correo de nomina
				int idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
				int idTienda = 1;
				try {
					String tStr = request.getParameter("idtienda");
					if (tStr != null && !tStr.trim().isEmpty()) {
						idTienda = Integer.parseInt(tStr.trim());
					}
				} catch (Exception ignorada) {
				}

				double valor = 0.0;
				try {
					String vStr = request.getParameter("valor");
					if (vStr != null && !vStr.trim().isEmpty()) {
						valor = Double.parseDouble(vStr.trim());
					}
				} catch (Exception ignorada) {
				}

				// Consultar datos del empleado
				String nombreEmpleado = "";
				String emailEmpleado = "";
				ConexionBaseDatos cnx = new ConexionBaseDatos();
				try (Connection conGen = cnx.obtenerConexionBDGeneral()) {
					String sqlEmp = "SELECT nombre_largo, IFNULL(email, '') as email FROM general.empleado WHERE id = ? AND activo = 1";
					try (PreparedStatement ps = conGen.prepareStatement(sqlEmp)) {
						ps.setInt(1, idEmpleado);
						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								nombreEmpleado = rs.getString("nombre_largo");
								emailEmpleado = rs.getString("email").trim();
							}
						}
					}
				}

				if (nombreEmpleado.isEmpty()) {
					resp.put("exito", false);
					resp.put("mensaje", "Empleado no encontrado o inactivo en nómina.");
					response.getWriter().write(GSON.toJson(resp));
					return;
				}

				if (emailEmpleado.isEmpty() || !ControladorEnvioCorreo.esDireccionValida(emailEmpleado)) {
					resp.put("exito", false);
					resp.put("mensaje", "El empleado no tiene correo electrónico válido registrado en nómina. No es posible enviar el código.");
					response.getWriter().write(GSON.toJson(resp));
					return;
				}

				// Validar frecuencia semanal antes de generar
				PedidoCtrl pedCtrl = new PedidoCtrl();
				boolean yaPidio = pedCtrl.validarFrecuenciaPedidoPrecioEmpleado(idEmpleado);
				if (yaPidio) {
					resp.put("exito", false);
					resp.put("motivo", "YA_PIDIO");
					resp.put("mensaje", "NO SE PUEDE PEDIR: El empleado ya redimió su pizza de precio empleado esta semana.");
					response.getWriter().write(GSON.toJson(resp));
					return;
				}

				// Generar e insertar codigo
				String fechaHoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
				PedidoPrecioEmpleado pedPrecio = new PedidoPrecioEmpleado(idEmpleado, fechaHoy, idTienda, valor, 0, "N", "");
				String codigoGenerado = pedCtrl.insertarPedidoPrecioEmpleado(pedPrecio);

				if (codigoGenerado == null || codigoGenerado.trim().isEmpty()) {
					resp.put("exito", false);
					resp.put("mensaje", "Error generando código en base de datos.");
					response.getWriter().write(GSON.toJson(resp));
					return;
				}

				// Enviar correo electronico
				boolean correoEnviado = enviarCorreoCodigo(emailEmpleado, nombreEmpleado, codigoGenerado);

				resp.put("exito", true);
				resp.put("codigoGenerado", codigoGenerado);
				resp.put("correoEnviado", correoEnviado);
				resp.put("emailEnmascarado", BuscarEmpleadosPrecioEmpleado.enmascararEmail(emailEmpleado));
				resp.put("mensaje", "Código de verificación generado y enviado exitosamente a "
						+ BuscarEmpleadosPrecioEmpleado.enmascararEmail(emailEmpleado)
						+ ". Pídele al empleado que revise su buzón de correo y te brinde el código.");

			} else if ("validar".equalsIgnoreCase(accion)) {
				// 3. Validar codigo ingresado por el asesor
				int idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
				String codigoIngresado = request.getParameter("codigo");
				if (codigoIngresado == null || codigoIngresado.trim().isEmpty()) {
					resp.put("exito", false);
					resp.put("valido", false);
					resp.put("mensaje", "Debe ingresar el código de verificación.");
					response.getWriter().write(GSON.toJson(resp));
					return;
				}

				codigoIngresado = codigoIngresado.trim().toUpperCase();

				boolean existe = false;
				String autorizado = "N";

				ConexionBaseDatos cnx = new ConexionBaseDatos();
				try (Connection conGen = cnx.obtenerConexionBDGeneral()) {
					String sqlCheck = "SELECT autorizado, fecha_insercion FROM general.pedido_precio_empleado "
							+ "WHERE idempleado = ? AND UPPER(TRIM(codigo)) = ? "
							+ "ORDER BY fecha_insercion DESC LIMIT 1";

					try (PreparedStatement ps = conGen.prepareStatement(sqlCheck)) {
						ps.setInt(1, idEmpleado);
						ps.setString(2, codigoIngresado);

						try (ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								existe = true;
								autorizado = rs.getString("autorizado");
							}
						}
					}
				}

				if (!existe) {
					resp.put("exito", false);
					resp.put("valido", false);
					resp.put("mensaje", "El código ingresado NO es válido para este empleado.");
				} else if ("S".equalsIgnoreCase(autorizado)) {
					resp.put("exito", false);
					resp.put("valido", false);
					resp.put("mensaje", "Este código ya fue utilizado y redimido anteriormente.");
				} else {
					resp.put("exito", true);
					resp.put("valido", true);
					resp.put("codigo", codigoIngresado);
					resp.put("mensaje", "¡Código validado exitosamente! El beneficio de precio empleado ha sido autorizado.");
				}
			} else {
				resp.put("exito", false);
				resp.put("mensaje", "Acción no reconocida: " + accion);
			}
		} catch (Throwable t) {
			resp.put("exito", false);
			resp.put("mensaje", "Error procesando solicitud: " + t.getMessage());
			t.printStackTrace();
		}

		PrintWriter out = response.getWriter();
		out.write(GSON.toJson(resp));
	}

	private boolean enviarCorreoCodigo(String emailDestino, String nombreEmpleado, String codigo) {
		try {
			CorreoElectronico cuenta = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
			if (cuenta == null || cuenta.getCuentaCorreo() == null || cuenta.getCuentaCorreo().trim().isEmpty()) {
				cuenta = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREONOMINA", "CLAVECORREONOMINA");
			}
			if (cuenta == null || cuenta.getCuentaCorreo() == null || cuenta.getCuentaCorreo().trim().isEmpty()) {
				cuenta = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOPUBLICA", "CLAVECORREOPUBLICA");
			}

			if (cuenta == null || cuenta.getCuentaCorreo() == null || cuenta.getCuentaCorreo().trim().isEmpty()) {
				System.err.println("GestionCodigoPrecioEmpleado: No se encontro cuenta de correo configurada en general.parametros.");
				return false;
			}

			String cuerpoHtml = armarHtmlCorreo(nombreEmpleado, codigo);

			Correo correo = new Correo();
			correo.setAsunto("CÓDIGO DE PRECIO EMPLEADO - CONTACT CENTER PIZZA AMERICANA");
			correo.setUsuarioCorreo(cuenta.getCuentaCorreo());
			correo.setContrasena(cuenta.getClaveCorreo());
			correo.setMensaje(cuerpoHtml);

			ArrayList<String> destinatarios = new ArrayList<String>();
			destinatarios.add(emailDestino.trim());

			ControladorEnvioCorreo envio = new ControladorEnvioCorreo(correo, destinatarios);
			envio.enviarCorreoAsincrono();
			return true;
		} catch (Throwable t) {
			System.err.println("Error enviando correo de precio empleado a " + emailDestino + ": " + t.getMessage());
			t.printStackTrace();
			return false;
		}
	}

	private String armarHtmlCorreo(String nombreEmpleado, String codigo) {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>");
		sb.append("<html><head><meta charset='UTF-8'>");
		sb.append("<style>");
		sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f7f7f7; margin: 0; padding: 20px; color: #333; }");
		sb.append(".container { max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border-top: 5px solid #c8102e; }");
		sb.append(".header { background: #222222; text-align: center; padding: 20px; }");
		sb.append(".header img { max-height: 50px; }");
		sb.append(".content { padding: 30px 25px; text-align: center; }");
		sb.append(".badge-title { font-size: 13px; font-weight: 700; color: #c8102e; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }");
		sb.append("h2 { color: #111; margin-top: 0; font-size: 22px; }");
		sb.append("p { font-size: 15px; line-height: 1.5; color: #555; text-align: left; }");
		sb.append(".box-codigo { background: #fff5f5; border: 2px dashed #c8102e; border-radius: 8px; padding: 18px; margin: 25px 0; text-align: center; }");
		sb.append(".codigo { font-size: 32px; font-weight: 800; color: #c8102e; letter-spacing: 6px; font-family: monospace; }");
		sb.append(".aviso { font-size: 12px; color: #888; margin-top: 20px; border-top: 1px solid #eee; padding-top: 15px; }");
		sb.append(".footer { background: #fafafa; padding: 15px; text-align: center; font-size: 12px; color: #999; border-top: 1px solid #eee; }");
		sb.append("</style></head><body>");
		sb.append("<div class='container'>");
		sb.append("<div class='header'>");
		sb.append("<img src='https://pizzaamericana.co/wp-content/uploads/2026/09/logo-sin-fondo.png' alt='Pizza Americana'>");
		sb.append("</div>");
		sb.append("<div class='content'>");
		sb.append("<div class='badge-title'>Beneficio Corporativo</div>");
		sb.append("<h2>Código de Validación Precio Empleado</h2>");
		sb.append("<p>Hola <strong>").append(nombreEmpleado).append("</strong>,</p>");
		sb.append("<p>Has solicitado adquirir un producto a <strong>Precio Empleado</strong> a través de nuestro Contact Center.</p>");
		sb.append("<p>Por favor facilita este código de verificación al asesor telefónico para autorizar tu orden:</p>");
		sb.append("<div class='box-codigo'>");
		sb.append("<div style='font-size:11px; color:#c8102e; font-weight:700; margin-bottom:4px;'>TU CÓDIGO DE VERIFICACIÓN</div>");
		sb.append("<div class='codigo'>").append(codigo).append("</div>");
		sb.append("</div>");
		sb.append("<p class='aviso'>⚠️ Este código es <strong>personal, intransferible y de uso único</strong>. Si tú no realizaste esta solicitud, por favor comunícalo al área de tecnología o talento humano.</p>");
		sb.append("</div>");
		sb.append("<div class='footer'>");
		sb.append("Pizza Americana &copy; ").append(new SimpleDateFormat("yyyy").format(new Date())).append(" - Sistema Contact Center");
		sb.append("</div></div></body></html>");
		return sb.toString();
	}
}

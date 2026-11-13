package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EventoEspecialCtrl;
import capaModeloCC.EventoEspecial;

/**
 * Planilla un evento especial: lo crea, o lo actualiza si viene idevento.
 *
 * Ruta: /ProyectoPizzaAmericana/GuardarEventoEspecial
 * Solo POST: es una mutacion. Un GET devuelve 405 para que no quede un
 * "guardar" alcanzable desde la barra de direcciones.
 *
 * Obligatorios: nombre_cliente, telefono, fecha_evento (aaaa-mm-dd), cantidad_pizzas (>= 1)
 * Opcionales:   idevento, tipo_evento, nombre_evento, empresa, email, hora_evento,
 *               detalle_pedido, idtienda, direccion_entrega, valor_estimado, anticipo,
 *               estado, observacion, usuario, idcliente, idlead
 */
@WebServlet("/GuardarEventoEspecial")
public class GuardarEventoEspecial extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public GuardarEventoEspecial() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        String nombreCliente = texto(request.getParameter("nombre_cliente"));
        String telefono = texto(request.getParameter("telefono"));
        String fechaEvento = texto(request.getParameter("fecha_evento"));

        if (nombreCliente.isEmpty()) {
            out.write(error("Falta el nombre del cliente."));
            return;
        }
        if (telefono.isEmpty()) {
            out.write(error("Falta el telefono: sin telefono la tienda no puede confirmar el evento."));
            return;
        }
        if (!fechaEvento.matches("\\d{4}-\\d{2}-\\d{2}")) {
            out.write(error("La fecha del evento debe venir como aaaa-mm-dd."));
            return;
        }

        int cantidadPizzas;
        try {
            cantidadPizzas = Integer.parseInt(texto(request.getParameter("cantidad_pizzas")));
        } catch (NumberFormatException e) {
            out.write(error("La cantidad de pizzas tiene que ser un numero."));
            return;
        }
        if (cantidadPizzas < 1) {
            out.write(error("La cantidad de pizzas tiene que ser por lo menos 1."));
            return;
        }

        // Los largos de la tabla, validados aqui para que el error salga en
        // castellano y no como un "Data too long for column" del driver.
        if (nombreCliente.length() > 150 || telefono.length() > 50) {
            out.write(error("El nombre del cliente (max 150) o el telefono (max 50) se pasan de largo."));
            return;
        }

        EventoEspecial ev = new EventoEspecial();
        ev.setIdevento(entero(request.getParameter("idevento")));
        ev.setTipoEvento(texto(request.getParameter("tipo_evento")));
        ev.setNombreEvento(recortar(texto(request.getParameter("nombre_evento")), 150));
        ev.setNombreCliente(nombreCliente);
        ev.setEmpresa(recortar(texto(request.getParameter("empresa")), 150));
        ev.setTelefono(telefono);
        ev.setEmail(recortar(texto(request.getParameter("email")), 100));
        ev.setFechaEvento(fechaEvento);
        ev.setHoraEvento(texto(request.getParameter("hora_evento")));
        ev.setCantidadPizzas(cantidadPizzas);
        ev.setDetallePedido(texto(request.getParameter("detalle_pedido")));
        ev.setIdtienda(entero(request.getParameter("idtienda")));
        ev.setDireccionEntrega(recortar(texto(request.getParameter("direccion_entrega")), 250));
        ev.setValorEstimado(decimal(request.getParameter("valor_estimado")));
        ev.setAnticipo(decimal(request.getParameter("anticipo")));
        ev.setEstado(texto(request.getParameter("estado")));
        ev.setObservacion(texto(request.getParameter("observacion")));
        ev.setIdcliente(entero(request.getParameter("idcliente")));
        ev.setIdlead(entero(request.getParameter("idlead")));

        String usuario = texto(request.getParameter("usuario"));
        if (usuario.isEmpty()) {
            usuario = "asesor_cc";
        }
        ev.setUsuarioRegistra(usuario);
        ev.setUsuarioModifica(usuario);

        EventoEspecialCtrl ctrl = new EventoEspecialCtrl();
        out.write(ctrl.guardarJson(ev));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().write("{\"resultado\":\"ERROR\",\"mensaje\":\"Este servicio solo acepta POST.\"}");
    }

    private static String error(String mensaje) {
        return "{\"resultado\":\"ERROR\",\"mensaje\":\"" + mensaje.replace("\"", "'") + "\"}";
    }

    private static String texto(String s) {
        return s == null ? "" : s.trim();
    }

    private static String recortar(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static int entero(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double decimal(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? 0 : Double.parseDouble(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

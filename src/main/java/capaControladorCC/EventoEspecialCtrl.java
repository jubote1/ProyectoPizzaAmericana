package capaControladorCC;

import java.util.List;

import capaDAOCC.EventoEspecialDAO;
import capaModeloCC.EventoEspecial;

/**
 * Controlador del planificador de eventos especiales. Arma el JSON a mano, como
 * el resto del central, para no meter dependencias nuevas.
 */
public class EventoEspecialCtrl {

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Un evento como objeto JSON. */
    private static void anexar(StringBuilder sb, EventoEspecial e) {
        sb.append("{")
          .append("\"idevento\":").append(e.getIdevento()).append(",")
          .append("\"tipo_evento\":\"").append(escapeJson(e.getTipoEvento())).append("\",")
          .append("\"nombre_evento\":\"").append(escapeJson(e.getNombreEvento())).append("\",")
          .append("\"nombre_cliente\":\"").append(escapeJson(e.getNombreCliente())).append("\",")
          .append("\"empresa\":\"").append(escapeJson(e.getEmpresa())).append("\",")
          .append("\"telefono\":\"").append(escapeJson(e.getTelefono())).append("\",")
          .append("\"email\":\"").append(escapeJson(e.getEmail())).append("\",")
          .append("\"idcliente\":").append(e.getIdcliente()).append(",")
          .append("\"idlead\":").append(e.getIdlead()).append(",")
          .append("\"fecha_evento\":\"").append(escapeJson(e.getFechaEvento())).append("\",")
          .append("\"hora_evento\":\"").append(escapeJson(e.getHoraEvento())).append("\",")
          .append("\"cantidad_pizzas\":").append(e.getCantidadPizzas()).append(",")
          .append("\"detalle_pedido\":\"").append(escapeJson(e.getDetallePedido())).append("\",")
          .append("\"idtienda\":").append(e.getIdtienda()).append(",")
          .append("\"nombre_tienda\":\"").append(escapeJson(e.getNombreTienda())).append("\",")
          .append("\"direccion_entrega\":\"").append(escapeJson(e.getDireccionEntrega())).append("\",")
          .append("\"valor_estimado\":").append(e.getValorEstimado()).append(",")
          .append("\"anticipo\":").append(e.getAnticipo()).append(",")
          .append("\"estado\":\"").append(escapeJson(e.getEstado())).append("\",")
          .append("\"observacion\":\"").append(escapeJson(e.getObservacion())).append("\",")
          .append("\"usuario_registra\":\"").append(escapeJson(e.getUsuarioRegistra())).append("\",")
          .append("\"creado_en\":\"").append(escapeJson(e.getCreadoEn())).append("\",")
          .append("\"usuario_modifica\":\"").append(escapeJson(e.getUsuarioModifica())).append("\",")
          .append("\"modificado_en\":\"").append(escapeJson(e.getModificadoEn())).append("\"")
          .append("}");
    }

    /**
     * Los eventos del rango, con el resumen que la pantalla pinta arriba:
     * cuantos eventos, cuantas pizzas y cuanta plata, contando solo los que
     * siguen vivos (los cancelados no suman).
     */
    public String listarJson(String fechaDesde, String fechaHasta, String estado,
                             int idtienda, String tipo, String filtro) {
        List<EventoEspecial> eventos = EventoEspecialDAO.listar(fechaDesde, fechaHasta, estado, idtienda, tipo, filtro);

        int pizzas = 0;
        double valor = 0;
        double anticipos = 0;
        int apartados = 0;
        for (EventoEspecial e : eventos) {
            if ("CANCELADO".equalsIgnoreCase(e.getEstado())) {
                continue;
            }
            pizzas += e.getCantidadPizzas();
            valor += e.getValorEstimado();
            anticipos += e.getAnticipo();
            if ("APARTADO".equalsIgnoreCase(e.getEstado()) || "CONFIRMADO".equalsIgnoreCase(e.getEstado())) {
                apartados++;
            }
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"resultado\":\"OK\",")
          .append("\"total_eventos\":").append(eventos.size()).append(",")
          .append("\"total_pizzas\":").append(pizzas).append(",")
          .append("\"valor_estimado\":").append(valor).append(",")
          .append("\"total_anticipos\":").append(anticipos).append(",")
          .append("\"apartados\":").append(apartados).append(",")
          .append("\"eventos\":[");
        for (int i = 0; i < eventos.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            anexar(sb, eventos.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Guarda (crea o actualiza) y devuelve el evento ya guardado. */
    public String guardarJson(EventoEspecial ev) {
        int id = EventoEspecialDAO.guardar(ev);
        if (id <= 0) {
            return "{\"resultado\":\"ERROR\",\"mensaje\":\"No se pudo guardar el evento. Revise el log del servidor.\"}";
        }
        EventoEspecial guardado = EventoEspecialDAO.porId(id);
        StringBuilder sb = new StringBuilder("{\"resultado\":\"OK\",\"idevento\":");
        sb.append(id).append(",\"evento\":");
        if (guardado != null) {
            anexar(sb, guardado);
        } else {
            sb.append("null");
        }
        sb.append("}");
        return sb.toString();
    }

    public String cambiarEstadoJson(int idevento, String estado, String usuario) {
        boolean ok = EventoEspecialDAO.cambiarEstado(idevento, estado, usuario);
        if (!ok) {
            return "{\"resultado\":\"ERROR\",\"mensaje\":\"No se pudo cambiar el estado del evento " + idevento + ".\"}";
        }
        return "{\"resultado\":\"OK\",\"idevento\":" + idevento + ",\"estado\":\"" + escapeJson(estado.toUpperCase()) + "\"}";
    }
}

package capaControladorCC;

import java.util.List;

import capaDAOCC.CampanaNovenasDAO;
import capaModeloCC.CampanaNovenasBitacora;
import capaModeloCC.CampanaNovenasLead;
import capaModeloCC.VentaAtribuidaNovenas;

/**
 * Controlador para la Campaña de Novenas y Venta Empresarial 25 Años.
 * Proporciona métodos de serialización JSON libres de dependencias externas.
 */
public class CampanaNovenasCtrl {

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public String buscarLeadsJson(String filtro, String clasificacion, String estado, int limite) {
        List<CampanaNovenasLead> leads = CampanaNovenasDAO.buscarLeads(filtro, clasificacion, estado, limite);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < leads.size(); i++) {
            CampanaNovenasLead l = leads.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"idlead\":").append(l.getIdlead()).append(",")
              .append("\"idcliente\":").append(l.getIdcliente()).append(",")
              .append("\"nombre_cliente\":\"").append(escapeJson(l.getNombreCliente())).append("\",")
              .append("\"empresa\":\"").append(escapeJson(l.getEmpresa())).append("\",")
              .append("\"telefono\":\"").append(escapeJson(l.getTelefono())).append("\",")
              .append("\"email\":\"").append(escapeJson(l.getEmail())).append("\",")
              .append("\"direccion\":\"").append(escapeJson(l.getDireccion())).append("\",")
              .append("\"observacion_cliente\":\"").append(escapeJson(l.getObservacionCliente())).append("\",")
              .append("\"clasificacion\":\"").append(escapeJson(l.getClasificacion())).append("\",")
              .append("\"criterio_clasificacion\":\"").append(escapeJson(l.getCriterioClasificacion())).append("\",")
              .append("\"total_pizzas\":").append(l.getTotalPizzasHistorico()).append(",")
              .append("\"total_pedidos\":").append(l.getTotalPedidosHistorico()).append(",")
              .append("\"valor_total\":").append(l.getTotalCompradoHistorico()).append(",")
              .append("\"ultima_fecha\":\"").append(escapeJson(l.getUltimaFechaCompra())).append("\",")
              .append("\"factura_electronica\":\"").append(escapeJson(l.getFacturaElectronica())).append("\",")
              .append("\"nit_cedula\":\"").append(escapeJson(l.getNitCedula())).append("\",")
              .append("\"razon_social\":\"").append(escapeJson(l.getRazonSocial())).append("\",")
              .append("\"pizzas_preferidas\":\"").append(escapeJson(l.getPizzasPreferidas())).append("\",")
              .append("\"estado_gestion\":\"").append(escapeJson(l.getEstadoGestion())).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public String obtenerLeadConBitacoraJson(int idlead) {
        CampanaNovenasLead lead = CampanaNovenasDAO.obtenerLeadPorId(idlead);
        if (lead == null) return "{\"error\":\"Lead no encontrado\"}";

        List<CampanaNovenasBitacora> bitacoras = CampanaNovenasDAO.obtenerBitacoraPorLead(idlead);

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"lead\":{")
          .append("\"idlead\":").append(lead.getIdlead()).append(",")
          .append("\"idcliente\":").append(lead.getIdcliente()).append(",")
          .append("\"nombre_cliente\":\"").append(escapeJson(lead.getNombreCliente())).append("\",")
          .append("\"empresa\":\"").append(escapeJson(lead.getEmpresa())).append("\",")
          .append("\"telefono\":\"").append(escapeJson(lead.getTelefono())).append("\",")
          .append("\"email\":\"").append(escapeJson(lead.getEmail())).append("\",")
          .append("\"direccion\":\"").append(escapeJson(lead.getDireccion())).append("\",")
          .append("\"observacion_cliente\":\"").append(escapeJson(lead.getObservacionCliente())).append("\",")
          .append("\"clasificacion\":\"").append(escapeJson(lead.getClasificacion())).append("\",")
          .append("\"criterio_clasificacion\":\"").append(escapeJson(lead.getCriterioClasificacion())).append("\",")
          .append("\"total_pizzas\":").append(lead.getTotalPizzasHistorico()).append(",")
          .append("\"total_pedidos\":").append(lead.getTotalPedidosHistorico()).append(",")
          .append("\"valor_total\":").append(lead.getTotalCompradoHistorico()).append(",")
          .append("\"ultima_fecha\":\"").append(escapeJson(lead.getUltimaFechaCompra())).append("\",")
          .append("\"factura_electronica\":\"").append(escapeJson(lead.getFacturaElectronica())).append("\",")
          .append("\"nit_cedula\":\"").append(escapeJson(lead.getNitCedula())).append("\",")
          .append("\"razon_social\":\"").append(escapeJson(lead.getRazonSocial())).append("\",")
          .append("\"pizzas_preferidas\":\"").append(escapeJson(lead.getPizzasPreferidas())).append("\",")
          .append("\"estado_gestion\":\"").append(escapeJson(lead.getEstadoGestion())).append("\"")
          .append("},\"bitacora\":[");

        for (int i = 0; i < bitacoras.size(); i++) {
            CampanaNovenasBitacora b = bitacoras.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"idbitacora\":").append(b.getIdbitacora()).append(",")
              .append("\"usuario_asesor\":\"").append(escapeJson(b.getUsuarioAsesor())).append("\",")
              .append("\"fecha_contacto\":\"").append(escapeJson(b.getFechaContacto())).append("\",")
              .append("\"estado_contacto\":\"").append(escapeJson(b.getEstadoContacto())).append("\",")
              .append("\"fecha_tentativa_evento\":\"").append(escapeJson(b.getFechaTentativaEvento())).append("\",")
              .append("\"pizzas_estimadas\":").append(b.getPizzasEstimadas()).append(",")
              .append("\"observacion\":\"").append(escapeJson(b.getObservacion())).append("\"")
              .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public String guardarBitacoraJson(int idlead, int idcliente, String usuarioAsesor, String estadoContacto, String fechaTentativa, int pizzasEstimadas, String observacion) {
        CampanaNovenasBitacora b = new CampanaNovenasBitacora();
        b.setIdlead(idlead);
        b.setIdcliente(idcliente);
        // Las dos columnas son VARCHAR(50): un valor mas largo hace fallar el INSERT completo.
        b.setUsuarioAsesor(usuarioAsesor != null && usuarioAsesor.length() > 50 ? usuarioAsesor.substring(0, 50) : usuarioAsesor);
        b.setEstadoContacto(estadoContacto != null && estadoContacto.length() > 50 ? estadoContacto.substring(0, 50) : estadoContacto);
        b.setFechaTentativaEvento(fechaTentativa);
        b.setPizzasEstimadas(pizzasEstimadas);
        b.setObservacion(observacion);

        // La fecha tentativa se valida aqui: un campo de fecha del navegador deja escribir anos de 5 o 6
        // digitos, y la base rechaza el INSERT entero con un error que el asesor no entiende.
        if (fechaTentativa != null && !fechaTentativa.trim().isEmpty()) {
            try {
                java.time.LocalDate f = java.time.LocalDate.parse(fechaTentativa.trim());
                if (f.getYear() < 2020 || f.getYear() > 2100) {
                    return "{\"resultado\":\"ERROR\",\"mensaje\":\"La fecha tentativa del evento no es valida: revise el ano.\"}";
                }
            } catch (Exception e) {
                return "{\"resultado\":\"ERROR\",\"mensaje\":\"La fecha tentativa del evento no es valida. Use el calendario o deje el campo vacio.\"}";
            }
        }

        String motivo = CampanaNovenasDAO.guardarBitacoraDetalle(b);
        if (motivo.length() == 0) {
            return "{\"resultado\":\"OK\",\"mensaje\":\"Contacto y bitácora guardados exitosamente\"}";
        } else {
            String limpio = motivo.replace("\\", "/").replace("\"", "'");
            return "{\"resultado\":\"ERROR\",\"mensaje\":\"No se pudo guardar la bitacora: " + limpio + "\"}";
        }
    }

    public String consultarVentasAtribuidasJson(String usuarioAsesor, String fechaInicial, String fechaFinal) {
        List<VentaAtribuidaNovenas> ventas = CampanaNovenasDAO.consultarVentasAtribuidas(usuarioAsesor, fechaInicial, fechaFinal);

        double sumaNeto = 0;
        int sumaPizzas = 0;
        double sumaComision = 0;

        StringBuilder sbVentas = new StringBuilder("[");
        for (int i = 0; i < ventas.size(); i++) {
            VentaAtribuidaNovenas v = ventas.get(i);
            sumaNeto += v.getTotalNeto();
            sumaPizzas += v.getTotalPizzasPedido();
            sumaComision += v.getComisionEstimada();

            if (i > 0) sbVentas.append(",");
            sbVentas.append("{")
                    .append("\"idpedidotienda\":").append(v.getIdpedidotienda()).append(",")
                    .append("\"idtienda\":").append(v.getIdtienda()).append(",")
                    .append("\"nombre_tienda\":\"").append(escapeJson(v.getNombreTienda())).append("\",")
                    .append("\"idcliente\":").append(v.getIdcliente()).append(",")
                    .append("\"cliente\":\"").append(escapeJson(v.getNombreCliente())).append("\",")
                    .append("\"organizacion\":\"").append(escapeJson(v.getOrganizacion())).append("\",")
                    .append("\"telefono\":\"").append(escapeJson(v.getTelefono())).append("\",")
                    .append("\"fecha_pedido\":\"").append(escapeJson(v.getFechaPedido())).append("\",")
                    .append("\"fecha_contacto\":\"").append(escapeJson(v.getFechaContactoAsesor())).append("\",")
                    .append("\"asesor\":\"").append(escapeJson(v.getUsuarioAsesor())).append("\",")
                    .append("\"total_pizzas\":").append(v.getTotalPizzasPedido()).append(",")
                    .append("\"valor_neto\":").append(v.getTotalNeto()).append(",")
                    .append("\"comision\":").append(v.getComisionEstimada()).append(",")
                    .append("\"pizzas_detalle\":\"").append(escapeJson(v.getPizzasDetalle())).append("\",")
                    .append("\"estado\":\"").append(escapeJson(v.getEstadoPedido())).append("\"")
                    .append("}");
        }
        sbVentas.append("]");

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"total_pedidos\":").append(ventas.size()).append(",")
          .append("\"total_pizzas\":").append(sumaPizzas).append(",")
          .append("\"total_ventas\":").append(sumaNeto).append(",")
          .append("\"total_comisiones\":").append(sumaComision).append(",")
          .append("\"pedidos\":").append(sbVentas.toString())
          .append("}");

        return sb.toString();
    }
}

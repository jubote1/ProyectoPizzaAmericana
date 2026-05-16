package capaServicioCC;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import capaModeloCC.Notificaciones;

@ServerEndpoint("/notificaciones")
public class NotificacionSocket {

    private static final Set<Session> sesiones = new CopyOnWriteArraySet<>();

    @OnOpen
    public void abrir(Session session) {
        sesiones.add(session);
        System.out.println("Cliente conectado a notificaciones");
    }

    @OnClose
    public void cerrar(Session session) {
        sesiones.remove(session);
        System.out.println("Cliente desconectado de notificaciones");
    }

    public static void enviarATodos(Notificaciones notificacion) {

    	String json = "{"
    	        + "\"id\":" + notificacion.getId() + ","
    	        + "\"mensaje\":\"" + escapar(notificacion.getMensaje()) + "\","
    	        + "\"origen\":\"" + escapar(notificacion.getOrigen()) + "\","
    	        + "\"idpedido\":" + notificacion.getIdpedido() + ","
    	        + "\"fecha_hora\":\"" + escapar(notificacion.getFechaHora()) + "\""
    	        + "}";
        for (Session session : sesiones) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", " ");
    }

}

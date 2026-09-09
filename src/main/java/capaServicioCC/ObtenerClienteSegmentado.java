package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import capaControladorCC.SegmentacionClienteCtrl;
import capaModeloCC.FiltroSegmentacion;

@WebServlet("/ObtenerClienteSegmentado")
public class ObtenerClienteSegmentado extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            //Todos los filtros van a un objeto. Son catorce, y con parametros
            //sueltos cada filtro nuevo obligaba a tocar el servlet, el controlador
            //y el DAO solo para pasar el valor de la mano.
            FiltroSegmentacion filtro = new FiltroSegmentacion();
            filtro.setFechaInicio(texto(jsonObject, "fechaInicio"));
            filtro.setFechaMaxima(texto(jsonObject, "fechaMaxima"));
            filtro.setMinPedidos(entero(jsonObject, "minPedidos", -1));
            filtro.setMaxPedidos(entero(jsonObject, "maxPedidos", 0));
            filtro.setDiasMinimosSinPublicidad(entero(jsonObject, "minDiasPublicidad", 0));
            filtro.setDiasUltimaCompraDesde(entero(jsonObject, "diasUltimaCompraDesde", 0));
            filtro.setDiasUltimaCompraHasta(entero(jsonObject, "diasUltimaCompraHasta", 0));
            filtro.setTiendas(enteros(jsonObject, "tiendas"));
            filtro.setExcepciones(enteros(jsonObject, "excepciones"));
            filtro.setProductos(enteros(jsonObject, "productos"));
            filtro.setEspecialidades(enteros(jsonObject, "especialidades"));
            filtro.setCanales(textos(jsonObject, "canales"));
            filtro.setTiposCliente(textos(jsonObject, "tiposcliente"));

            //Compatibilidad: si alguien sigue mandando canal o tipocliente en
            //singular, se respeta. La pantalla nueva manda las listas.
            if (filtro.getCanales().isEmpty() && texto(jsonObject, "canal").length() > 0) {
                filtro.getCanales().add(texto(jsonObject, "canal"));
            }
            if (filtro.getTiposCliente().isEmpty() && texto(jsonObject, "tipocliente").length() > 0) {
                filtro.getTiposCliente().add(texto(jsonObject, "tipocliente"));
            }

            //Por defecto se dejan fuera los clientes de plataforma: sus datos de
            //contacto no son nuestros y encabezan cualquier conteo.
            if (jsonObject.has("incluirPlataformas") && !jsonObject.get("incluirPlataformas").isJsonNull()) {
                filtro.setExcluirPlataformas(!jsonObject.get("incluirPlataformas").getAsBoolean());
            }

            if (filtro.getFechaInicio().isEmpty() || filtro.getFechaMaxima().isEmpty()
                    || filtro.getMinPedidos() < 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(
                        "{\"error\": \"Faltan parametros obligatorios o los valores no son validos\"}");
                return;
            }
            if (filtro.getMaxPedidos() > 0 && filtro.getMaxPedidos() < filtro.getMinPedidos()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(
                        "{\"error\": \"El maximo de pedidos no puede ser menor que el minimo\"}");
                return;
            }
            if (filtro.getDiasUltimaCompraDesde() > 0 && filtro.getDiasUltimaCompraHasta() > 0
                    && filtro.getDiasUltimaCompraDesde() > filtro.getDiasUltimaCompraHasta()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(
                        "{\"error\": \"En la ultima compra, el desde no puede ser mayor que el hasta\"}");
                return;
            }

            String respuesta = SegmentacionClienteCtrl.obtenerClientesFiltrados(filtro);
            
 

            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
    		out.write(respuesta);

        } catch (JsonSyntaxException e) {
            System.err.println("Error de JSON: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Formato JSON inválido\"}");
        } catch (Exception e) {
            System.err.println("Error interno: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Error interno del servidor\"}");
        }
    }

    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

    /** Texto del JSON, o cadena vacia si no viene o viene nulo. */
    private String texto(JsonObject json, String campo) {
        if (!json.has(campo) || json.get(campo).isJsonNull()) {
            return "";
        }
        return json.get(campo).getAsString().trim();
    }

    /** Entero del JSON, o el valor por defecto si no viene, viene nulo o no es numero. */
    private int entero(JsonObject json, String campo, int porDefecto) {
        if (!json.has(campo) || json.get(campo).isJsonNull()) {
            return porDefecto;
        }
        try {
            return json.get(campo).getAsInt();
        } catch (Exception e) {
            return porDefecto;
        }
    }

    /** Lista de enteros del JSON. Nunca devuelve nulo. */
    private List<Integer> enteros(JsonObject json, String campo) {
        List<Integer> valores = new ArrayList<>();
        if (json.has(campo) && json.get(campo).isJsonArray()) {
            for (JsonElement elem : json.get(campo).getAsJsonArray()) {
                try {
                    valores.add(elem.getAsInt());
                } catch (Exception e) {
                    //Un valor que no es numero se ignora en vez de tumbar la consulta.
                }
            }
        }
        return valores;
    }

    /** Lista de textos del JSON, sin vacios. Nunca devuelve nulo. */
    private List<String> textos(JsonObject json, String campo) {
        List<String> valores = new ArrayList<>();
        if (json.has(campo) && json.get(campo).isJsonArray()) {
            for (JsonElement elem : json.get(campo).getAsJsonArray()) {
                if (elem.isJsonNull()) {
                    continue;
                }
                String valor = elem.getAsString().trim();
                if (valor.length() > 0 && !"TODOS".equalsIgnoreCase(valor)) {
                    valores.add(valor);
                }
            }
        }
        return valores;
    }

}

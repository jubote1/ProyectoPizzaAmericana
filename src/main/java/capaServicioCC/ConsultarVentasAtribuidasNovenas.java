package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CampanaNovenasCtrl;

/**
 * Servicio para consultar las ventas corporativas/novenas atribuidas a un asesor o equipo.
 * 
 * Ruta: /ProyectoPizzaAmericana/ConsultarVentasAtribuidasNovenas
 * Parámetros: asesor (string), fechainicial (string), fechafinal (string)
 * 
 * Regla de Oro: Solo se devuelven pedidos cerrados donde el cliente haya comprado 3 o más pizzas.
 */
@WebServlet("/ConsultarVentasAtribuidasNovenas")
public class ConsultarVentasAtribuidasNovenas extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ConsultarVentasAtribuidasNovenas() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");

        String asesor = request.getParameter("asesor");
        String fechaInicial = request.getParameter("fechainicial");
        String fechaFinal = request.getParameter("fechafinal");

        CampanaNovenasCtrl ctrl = new CampanaNovenasCtrl();
        String jsonRespuesta = ctrl.consultarVentasAtribuidasJson(asesor, fechaInicial, fechaFinal);

        PrintWriter out = response.getWriter();
        out.write(jsonRespuesta);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}

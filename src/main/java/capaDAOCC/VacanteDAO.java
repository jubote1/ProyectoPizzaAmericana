package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import capaModeloCC.BeneficioVacante;
import capaModeloCC.HorarioVacante;
import capaModeloCC.Lugar;
import capaModeloCC.Vacante;
import conexionCC.ConexionBaseDatos;

public class VacanteDAO {
    
    public static List<Vacante> obtenerVacantesActivas() {
        List<Vacante> vacantes = new ArrayList<>();
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDNominaAmericana();

        String query = "SELECT v.id, v.perfil, v.descripcion, v.cantidad, c.descripcion AS categoria, " +
                       "l.id AS lugar_id, l.descripcion AS lugar_descripcion, vl.cantidad_vacantes " +
                       "FROM vacante v " +
                       "JOIN categoria c ON v.idcategoria = c.id " +
                       "LEFT JOIN vacante_lugar vl ON v.id = vl.idvacante " +
                       "LEFT JOIN lugar l ON vl.idlugar = l.id " +
                       "WHERE v.estado = 1"; // Estado activo

        try (PreparedStatement preparedStatement = con1.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            Map<Integer, Vacante> vacanteMap = new HashMap<>();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String perfil = resultSet.getString("perfil");
                String descripcion = resultSet.getString("descripcion");
                int cantidad = resultSet.getInt("cantidad");
                String categoria = resultSet.getString("categoria");

                // Si la vacante no está en el mapa, la agregamos
                Vacante vacante = vacanteMap.get(id);
                if (vacante == null) {
                    vacante = new Vacante();
                    vacante.setId(id);
                    vacante.setPerfil(perfil);
                    vacante.setDescripcion(descripcion);
                    vacante.setCantidad(cantidad);
                    vacante.setCategoria(categoria);
                    vacante.setLugares(new ArrayList<>()); // Inicializar la lista de lugares
                    vacanteMap.put(id, vacante);
                }

                // Agregar el lugar, si existe y no es nulo o cero
                Integer lugarId = resultSet.getObject("lugar_id", Integer.class);
   
                if (lugarId != null && lugarId > 0 ) {
                    String lugarDescripcion = resultSet.getString("lugar_descripcion");
                    int cantidadVacantes= resultSet.getInt("cantidad_vacantes");
                    Lugar lugar = new Lugar(lugarId, lugarDescripcion,cantidadVacantes);
                    vacante.getLugares().add(lugar); // Agregar lugar a la lista
                }
            }

            // Agregar todas las vacantes del mapa a la lista final
            vacantes.addAll(vacanteMap.values());

        } catch (SQLException e) {
            e.printStackTrace(); // Manejo de excepciones
        } finally {
            // Cerrar la conexión si no es nula y no se ha cerrado aún
            if (con1 != null) {
                try {
                    con1.close();
                } catch (SQLException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return vacantes;
    }

    public static Vacante obtenerVacanteDetalle(int idvacante) {
        Vacante vacante = null;
        
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDNominaAmericana();
        
        String sql = "SELECT v.id, v.perfil, v.descripcion, v.cantidad, c.descripcion AS categoria, ct.descripcion AS contrato, v.imagen, "
                   + "v.mision, v.salario, "
                   + "(SELECT JSON_ARRAYAGG(JSON_OBJECT('id', l.id, 'descripcion', l.descripcion)) FROM lugar l "
                   + "JOIN vacante_lugar vl ON l.id = vl.idlugar WHERE vl.idvacante = v.id) AS lugares, "
                   + "(SELECT JSON_ARRAYAGG(JSON_OBJECT('id', b.id, 'descripcion', b.descripcion)) FROM beneficio b "
                   + "JOIN vacante_beneficio vb ON b.id = vb.idbeneficio WHERE vb.idvacante = v.id) AS beneficios, "
                   + "(SELECT JSON_ARRAYAGG(JSON_OBJECT('id', h.id, 'descripcion', h.descripcion)) FROM horario h "
                   + "JOIN vacante_horario vh ON h.id = vh.idhorario WHERE vh.idvacante = v.id) AS horarios "
                   + "FROM vacante v "
                   + "JOIN categoria c ON v.idcategoria = c.id "
                   + "JOIN contrato ct ON v.idcontrato= ct.id "
                   + "WHERE v.estado = 1 AND v.id = ?";

        try (PreparedStatement preparedStatement = con1.prepareStatement(sql)) {
            preparedStatement.setInt(1, idvacante);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    vacante = new Vacante();
                    vacante.setId(rs.getInt("id"));
                    vacante.setPerfil(rs.getString("perfil"));
                    vacante.setDescripcion(rs.getString("descripcion"));
                    vacante.setCantidad(rs.getInt("cantidad"));
                    vacante.setCategoria(rs.getString("categoria"));
                    vacante.setMision(rs.getString("mision"));
                    vacante.setSalario(rs.getString("salario"));
                    vacante.setContrato(rs.getString("contrato"));
                    vacante.setImagen(rs.getString("imagen"));;
                    Gson gson = new Gson();
                    // Procesar lugares
                    String lugaresJson = rs.getString("lugares");
                    if (lugaresJson != null) {
                        // Usa Gson o Jackson para procesar JSON
                   
                        Lugar[] lugares = gson.fromJson(lugaresJson, Lugar[].class);
                        vacante.setLugares(Arrays.asList(lugares));
                    }

                    // Procesar beneficios
                    String beneficiosJson = rs.getString("beneficios");
                    if (beneficiosJson != null) {
                        // Usa Gson o Jackson para procesar JSON
                        BeneficioVacante[] beneficios = gson.fromJson(beneficiosJson, BeneficioVacante[].class);
                        vacante.setBeneficioVacante(Arrays.asList(beneficios));
                    }

                    // Procesar horarios
                    String horariosJson = rs.getString("horarios");
                    if (horariosJson != null) {
                        // Usa Gson o Jackson para procesar JSON
                        HorarioVacante[] horarios = gson.fromJson(horariosJson, HorarioVacante[].class);
                        vacante.setHorarioVacante(Arrays.asList(horarios));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            // Cerrar la conexión si no es nula
            if (con1 != null) {
                try {
                    con1.close();
                } catch (SQLException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return vacante;
    }

/*
 * public static Vacante obtenerVacanteDetalle(int idvacante) {
    Vacante vacante = null;
    List<Lugar> lugares = new ArrayList<>();
    List<BeneficioVacante> beneficios = new ArrayList<>();
    List<HorarioVacante> horarios = new ArrayList<>();

    ConexionBaseDatos con = new ConexionBaseDatos();
    Connection con1 = con.obtenerConexionBDNominaAmericana();
    String sql = "SELECT v.id, v.perfil, v.descripcion, v.cantidad, c.descripcion AS categoria, "
               + "v.mision, v.salario, l.id AS lugar_id, l.descripcion AS lugar_descripcion, "
               + "b.id AS beneficio_id, b.descripcion AS beneficio_descripcion, "
               + "h.id AS horario_id, h.descripcion AS horario_descripcion "
               + "FROM vacante v "
               + "JOIN categoria c ON v.idcategoria = c.id "
               + "LEFT JOIN vacante_lugar vl ON v.id = vl.idvacante "
               + "LEFT JOIN lugar l ON vl.idlugar = l.id "
               + "LEFT JOIN vacante_beneficio vb ON v.id = vb.idvacante "
               + "LEFT JOIN beneficio b ON vb.idbeneficio = b.id "
               + "LEFT JOIN vacante_horario vh ON v.id = vh.idvacante "
               + "LEFT JOIN horario h ON vh.idhorario = h.id "
               + "WHERE v.estado = 1 AND v.id = ?";

    try (PreparedStatement preparedStatement = con1.prepareStatement(sql)) {
        // Set the parameter before executing the query
        preparedStatement.setInt(1, idvacante);
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                // Initialize the Vacante object if it's null
                if (vacante == null) {
                    vacante = new Vacante();
                    vacante.setId(rs.getInt("id"));
                    vacante.setPerfil(rs.getString("perfil"));
                    vacante.setDescripcion(rs.getString("descripcion"));
                    vacante.setCantidad(rs.getInt("cantidad"));
                    vacante.setCategoria(rs.getString("categoria"));
                    vacante.setMision(rs.getString("mision"));
                    vacante.setSalario(rs.getString("salario"));
                }

                // Add location if it exists and is not already added
                Integer lugarId = rs.getObject("lugar_id", Integer.class);
                if (lugarId != null && lugarId > 0) {
                    Lugar lugar = new Lugar(lugarId, rs.getString("lugar_descripcion"));
                    if (!lugares.contains(lugar)) { // Assuming Lugar has equals and hashCode methods overridden
                        lugares.add(lugar);
                    }
                }

                // Add benefit if it exists and is not already added
                Integer beneficioId = rs.getObject("beneficio_id", Integer.class);
                if (beneficioId != null && beneficioId > 0) {
                    BeneficioVacante beneficio = new BeneficioVacante(beneficioId, rs.getString("beneficio_descripcion"));
                    if (!beneficios.contains(beneficio)) { // Assuming BeneficioVacante has equals and hashCode methods overridden
                        beneficios.add(beneficio);
                    }
                }

                // Add schedule if it exists and is not already added
                Integer horarioId = rs.getObject("horario_id", Integer.class);
                if (horarioId != null && horarioId > 0) {
                    HorarioVacante horario = new HorarioVacante(horarioId, rs.getString("horario_descripcion"));
                    if (!horarios.contains(horario)) { // Assuming HorarioVacante has equals and hashCode methods overridden
                        horarios.add(horario);
                    }
                }
            }
        }
    } catch (SQLException e) {
        System.err.println("Error: " + e.getMessage());
    } finally {
        // Close the connection if it's not null and hasn't been closed yet
        if (con1 != null) {
            try {
                con1.close();
            } catch (SQLException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    // Assign the lists to the vacancy
    if (vacante != null) {
        vacante.setLugares(lugares);
        vacante.setBeneficioVacante(beneficios);
        vacante.setHorarioVacante(horarios);
    }

    return vacante;
}

 * 
 * 
 * */
    
    public static Vacante obtenerVacantePorId(int idVacante) {
        Vacante vacante = null;
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = con.obtenerConexionBDNominaAmericana();

        String query = "SELECT v.id, v.perfil, v.descripcion, v.cantidad, v.requiere_transporte, c.descripcion AS categoria, " +
                       "l.id AS lugar_id, l.descripcion AS lugar_descripcion ,vl.cantidad_vacantes " +
                       "FROM vacante v " +
                       "JOIN categoria c ON v.idcategoria = c.id " +
                       "LEFT JOIN vacante_lugar vl ON v.id = vl.idvacante " +
                       "LEFT JOIN lugar l ON vl.idlugar = l.id " +
                       "WHERE v.estado = 1 AND v.id = ?"; // Estado activo y el id de la vacante

        try (PreparedStatement preparedStatement = con1.prepareStatement(query)) {
            // Set the parameter before executing the query
            preparedStatement.setInt(1, idVacante);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String perfil = resultSet.getString("perfil");
                    String descripcion = resultSet.getString("descripcion");
                    int cantidad = resultSet.getInt("cantidad");
                    String categoria = resultSet.getString("categoria");
                    boolean requiere_transporte = resultSet.getBoolean("requiere_transporte");
                    
                    vacante = new Vacante();
                    vacante.setId(id);
                    vacante.setPerfil(perfil);
                    vacante.setDescripcion(descripcion);
                    vacante.setCantidad(cantidad);
                    vacante.setCategoria(categoria);
                    vacante.setLugares(new ArrayList<>()); // Inicializar la lista de lugares
                    vacante.setRequiere_transporte(requiere_transporte);

                    
                    Integer lugarId = resultSet.getObject("lugar_id", Integer.class);
                    
                    if (lugarId != null && lugarId > 0 ) {
                        String lugarDescripcion = resultSet.getString("lugar_descripcion");
                        int cantidadVacantes= resultSet.getInt("cantidad_vacantes");
                        Lugar lugar = new Lugar(lugarId, lugarDescripcion,cantidadVacantes);
                        
                        vacante.getLugares().add(lugar); // Agregar lugar a la lista
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Manejo de excepciones
        } finally {
            // Cerrar la conexión si no es nula y no se ha cerrado aún
            if (con1 != null) {
                try {
                    con1.close();
                } catch (SQLException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return vacante;
    }


    public static void main(final String[] args) {
        List<Vacante> uno = obtenerVacantesActivas();
        Vacante dos = obtenerVacanteDetalle(1);
        
        // Iterating over 'uno' list of Vacante
        if (uno != null) {
            for (Vacante vacante : uno) {
                System.out.println("Vacante ID: " + vacante.getId());
                System.out.println("Perfil: " + vacante.getPerfil());
                System.out.println("Descripcion: " + vacante.getDescripcion());
                System.out.println("Cantidad: " + vacante.getCantidad());
                System.out.println("Categoria: " + vacante.getCategoria());
                System.out.println("Mision: " + vacante.getMision());
                System.out.println("Salario: " + vacante.getSalario());
                // If you have lists like lugares, beneficios, or horarios in Vacante, iterate through them as well
                System.out.println("Lugares: " + vacante.getLugares());
                System.out.println("Beneficios: " + vacante.getBeneficioVacante());
                System.out.println("Horarios: " + vacante.getHorarioVacante());
                System.out.println("---------");
            }
        }

        // Printing details of 'dos' object
        if (dos != null) {
            System.out.println("Vacante (dos) ID: " + dos.getId());
            System.out.println("Perfil: " + dos.getPerfil());
            System.out.println("Descripcion: " + dos.getDescripcion());
            System.out.println("Cantidad: " + dos.getCantidad());
            System.out.println("Categoria: " + dos.getCategoria());
            System.out.println("Mision: " + dos.getMision());
            System.out.println("Salario: " + dos.getSalario());
            System.out.println("Lugares: " + dos.getLugares());
            System.out.println("Beneficios: " + dos.getBeneficioVacante());
            System.out.println("Horarios: " + dos.getHorarioVacante());
        } else {
            System.out.println("No details available for Vacante 'dos'.");
        }
        
        Vacante vacanteEspecifica = obtenerVacantePorId(1);
        if (vacanteEspecifica != null) {
            System.out.println("Vacante ID: " + vacanteEspecifica.getId());
            // Imprimir otros detalles de la vacante
        } else {
            System.out.println("No se encontró la vacante con el ID especificado.");
        }
    }
}

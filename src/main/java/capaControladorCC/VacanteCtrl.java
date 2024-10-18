package capaControladorCC;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import capaDAOCC.VacanteDAO;
import capaModeloCC.Candidato;
import capaModeloCC.Vacante;

public class VacanteCtrl {

    
    
    public List<Vacante>  obtenerVacantesActivas() {
    	return  VacanteDAO.obtenerVacantesActivas();

    }

    public Vacante  obtenerVacanteDetalle(int idvacante) {
        return VacanteDAO.obtenerVacanteDetalle(idvacante);
    }

    public Vacante  obtenerVacantePorId(int idvacante) {
        return VacanteDAO.obtenerVacantePorId(idvacante);
    }
    
    

    public static void main(String[] args) {
    	// Create the refresh token expiration time
    	long JWT_EXPIRATION_REFRESH_TOKEN = 1000L * 60 * 60 * 24 * 7;  // 7 days
    	long expirationLongRefreshToken = System.currentTimeMillis() + JWT_EXPIRATION_REFRESH_TOKEN;

    	System.out.println(expirationLongRefreshToken);
    	  long timestamp = 1728656422394L; // tu marca de tiempo  
          Date date = new Date(timestamp); // convierte el timestamp a Date  

          System.out.println("Fecha: "+date);
 

  }
  
    
}

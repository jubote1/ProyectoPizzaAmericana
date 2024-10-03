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

  }
  
    
}

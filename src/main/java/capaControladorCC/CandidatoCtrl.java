package capaControladorCC;

import org.json.JSONObject;

import capaDAOCC.CandidatoDAO;
import capaModeloCC.Candidato;

public class CandidatoCtrl {

	public String insertarCandidato(Candidato candidato) {
	    JSONObject respuesta = CandidatoDAO.insertarCandidato(candidato);
	    return respuesta.toString();
	}

}

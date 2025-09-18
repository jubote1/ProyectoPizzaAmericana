package capaModeloCC;

import java.util.List;



public class EncuestaPqrs {

	private int idpqrs;
	private List<RespuestaEncuestaPqrs> respuesta;
	
	public int getIdpqrs() {
		return idpqrs;
	}



	public void setIdpqrs(int idpqrs) {
		this.idpqrs = idpqrs;
	}



	public List<RespuestaEncuestaPqrs> getRespuesta() {
		return respuesta;
	}



	public void setRespuesta(List<RespuestaEncuestaPqrs> respuesta) {
		this.respuesta = respuesta;
	}

	
	
	public static class RespuestaEncuestaPqrs{
		private String descripcion;
		private String respuesta ;
		private int idpregunta;
		
		
		public int getIdpregunta() {
			return idpregunta;
		}
		public void setIdpregunta(int idpregunta) {
			this.idpregunta = idpregunta;
		}
		public String getDescripcion() {
			return descripcion;
		}
		public void setDescripcion(String descripcion) {
			this.descripcion = descripcion;
		}

		public String getRespuesta() {
			return respuesta;
		}
		public void setRespuesta(String respuesta) {
			this.respuesta = respuesta;
		}
		public RespuestaEncuestaPqrs(int idpregunta,String respuesta,String descripcion ) {
			this.descripcion = descripcion;
			this.respuesta = respuesta;
			this.idpregunta = idpregunta;


		}
	}
		
	
}

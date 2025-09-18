package capaModeloCC;

import java.util.List;



public class EncuestaGenerica {

	private int idpedido;
	private List<RespuestaEncuestaGen> respuesta;
	
	public int getIdpedido() {
		return idpedido;
	}



	public void setIdpedido(int idpedido) {
		this.idpedido = idpedido;
	}



	public List<RespuestaEncuestaGen> getRespuesta() {
		return respuesta;
	}



	public void setRespuesta(List<RespuestaEncuestaGen> respuesta) {
		this.respuesta = respuesta;
	}

	
	
	public static class RespuestaEncuestaGen{
		private String titulo;
		private String respuesta ;
		private int idpregunta;
		
		
		public int getIdpregunta() {
			return idpregunta;
		}
		public void setIdpregunta(int idpregunta) {
			this.idpregunta = idpregunta;
		}
		public String getTitulo() {
			return titulo;
		}
		public void setTitulo(String titulo) {
			this.titulo = titulo;
		}

		public String getRespuesta() {
			return respuesta;
		}
		public void setRespuesta(String respuesta) {
			this.respuesta = respuesta;
		}
		public RespuestaEncuestaGen(int idpregunta ,String respuesta,String titulo) {
			this.idpregunta = idpregunta;
			this.respuesta = respuesta;
			this.titulo = titulo;


		}
	}
		
	
}

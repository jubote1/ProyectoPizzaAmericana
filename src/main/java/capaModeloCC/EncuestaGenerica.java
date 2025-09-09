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
		private String descripcion;
		private String respuesta ;
		
		
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
		public RespuestaEncuestaGen(String descripcion ,String respuesta) {
			this.descripcion = descripcion;
			this.respuesta = respuesta;


		}
	}
		
	
}

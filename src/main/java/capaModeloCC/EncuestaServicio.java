package capaModeloCC;

import java.util.List;

public class EncuestaServicio {


    private int idpedido;
    private String tipo_atencion;
    public String getTipo_atencion() {
		return tipo_atencion;
	}
	public void setTipo_atencion(String tipo_atencion) {
		this.tipo_atencion = tipo_atencion;
	}

	private List<RespuestaServicio> respuesta;
    
	public List<RespuestaServicio> getRespuesta() {
		return respuesta;
	}
	public void setRespuesta(List<RespuestaServicio> respuesta) {
		this.respuesta = respuesta;
	}
	public int getIdpedido() {
		return idpedido;
	}
	public void setIdpedido(int idpedido) {
		this.idpedido = idpedido;
	}


	
	public EncuestaServicio() {
		
	}
	
	public EncuestaServicio(int idpedido,  List<RespuestaServicio> respuesta,String tipo_atencion) {
		this.idpedido = idpedido;
		this.respuesta = respuesta;
		this.tipo_atencion = tipo_atencion;

	}
	
	
		
		 public static class RespuestaServicio {
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
				public RespuestaServicio(String descripcion ,String respuesta) {
					this.descripcion = descripcion;
					this.respuesta = respuesta;


				}
				
				
	}
	
}

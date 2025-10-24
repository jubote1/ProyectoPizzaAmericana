package capaModeloCC;

import java.util.List;

public class EncuestaServicio {


    private int idpedido;
    private String tipo_atencion;
    private String nombre_cliente;
    private String telefono;
    private int idtienda;
    private String correo; 
    public String getCorreo() {
		return correo;
	}
	public void setCorreo(String correo) {
		this.correo = correo;
	}
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
	public String getNombre_cliente() {
		return nombre_cliente;
	}
	public void setNombre_cliente(String nombre_cliente) {
		this.nombre_cliente = nombre_cliente;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
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
				public RespuestaServicio(int idpregunta,String respuesta,String titulo ) {
					this.titulo = titulo;
					this.respuesta = respuesta;
					this.idpregunta = idpregunta;


				}
				
				
	}
	
}

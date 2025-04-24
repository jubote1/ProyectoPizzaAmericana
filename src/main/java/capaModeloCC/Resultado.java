package capaModeloCC;

public class Resultado {
	
	private String resultado;
	private String infoAdicional;
	private String estadoTienda;
	private boolean success;
	
	

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getEstadoTienda() {
		return estadoTienda;
	}

	public void setEstadoTienda(String estadoTienda) {
		this.estadoTienda = estadoTienda;
	}

	public String getResultado() {
		return resultado;
	}
	
	public String getInfoAdicional() {
		return infoAdicional;
	}
	
	public void setInfoAdicional(String infoAdicional) {
		this.infoAdicional =infoAdicional;
	}


	public void setResultado(String resultado) {
		this.resultado = resultado;
	}
	

}
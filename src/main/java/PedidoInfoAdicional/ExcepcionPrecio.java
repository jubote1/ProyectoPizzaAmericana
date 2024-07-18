package PedidoInfoAdicional;

/**
 * Clase que implementa la entidad Excepción Precio.
 * @author JuanDavid
 *
 */
public class ExcepcionPrecio {
	private int idExcepcion;
	private int idProducto;
	private String nombreProducto;
	private double precio;
	private String descripcion;
	private String incluyeliquido;
	private int idtipoliquido;
	private String nombreLiquido;
	private String controlaCantidadIngredientes;
	private int cantidadIngrediantes;
	private String partiradiciones;
	private String habilitado;
	private String horaInicial;
	private String horaFinal;
	private String lunes;
	private String martes;
	private String miercoles;
	private String jueves;
	private String viernes;
	private String sabado;
	private String domingo;
	private String controlaEspecialidades;
	private String ofertaAbierta;
	
	public String getOfertaAbierta() {
		return ofertaAbierta;
	}

	public void setOfertaAbierta(String ofertaAbierta) {
		this.ofertaAbierta = ofertaAbierta;
	}

	public String getControlaEspecialidades() {
		return controlaEspecialidades;
	}

	public void setControlaEspecialidades(String controlaEspecialidades) {
		this.controlaEspecialidades = controlaEspecialidades;
	}

	public String getLunes() {
		return lunes;
	}

	public void setLunes(String lunes) {
		this.lunes = lunes;
	}

	public String getMartes() {
		return martes;
	}

	public void setMartes(String martes) {
		this.martes = martes;
	}

	public String getMiercoles() {
		return miercoles;
	}

	public void setMiercoles(String miercoles) {
		this.miercoles = miercoles;
	}

	public String getJueves() {
		return jueves;
	}

	public void setJueves(String jueves) {
		this.jueves = jueves;
	}

	public String getViernes() {
		return viernes;
	}

	public void setViernes(String viernes) {
		this.viernes = viernes;
	}

	public String getSabado() {
		return sabado;
	}

	public void setSabado(String sabado) {
		this.sabado = sabado;
	}

	public String getDomingo() {
		return domingo;
	}

	public void setDomingo(String domingo) {
		this.domingo = domingo;
	}

	public String getHoraInicial() {
		return horaInicial;
	}

	public void setHoraInicial(String horaInicial) {
		this.horaInicial = horaInicial;
	}

	public String getHoraFinal() {
		return horaFinal;
	}

	public void setHoraFinal(String horaFinal) {
		this.horaFinal = horaFinal;
	}

	public String getHabilitado() {
		return habilitado;
	}

	public void setHabilitado(String habilitado) {
		this.habilitado = habilitado;
	}

	public String getPartiradiciones() {
		return partiradiciones;
	}

	public void setPartiradiciones(String partiradiciones) {
		this.partiradiciones = partiradiciones;
	}
	
	public ExcepcionPrecio(int idExcepcion, int idProducto, double precio, String descripcion, String incluyeliquido,
			int idtipoliquido, String habilitado, String horaInicial, String horaFinal, String lunes, String martes, String miercoles, String jueves, String viernes, String sabado, String domingo) {
		super();
		this.idExcepcion = idExcepcion;
		this.idProducto = idProducto;
		this.precio = precio;
		this.descripcion = descripcion;
		this.incluyeliquido = incluyeliquido;
		this.idtipoliquido = idtipoliquido;
		this.habilitado = habilitado;
		this.horaInicial = horaInicial;
		this.horaFinal = horaFinal;
		this.lunes = lunes;
		this.martes = martes;
		this.miercoles = miercoles;
		this.jueves = jueves;
		this.viernes = viernes;
		this.sabado = sabado;
		this.domingo = domingo;
		
	}


//Constructor elaborado principalmente para Grid donde se muetra la informacion de las excepciones precio
	public ExcepcionPrecio(int idExcepcion, int idProducto, String nombreProducto, double precio, String descripcion,
			String incluyeliquido, int idtipoliquido, String nombreLiquido, String habilitado, String horaInicial, String horaFinal, String lunes, String martes, String miercoles, String jueves, String viernes, String sabado, String domingo) {
		super();
		this.idExcepcion = idExcepcion;
		this.idProducto = idProducto;
		this.nombreProducto = nombreProducto;
		this.precio = precio;
		this.descripcion = descripcion;
		this.incluyeliquido = incluyeliquido;
		this.idtipoliquido = idtipoliquido;
		this.nombreLiquido = nombreLiquido;
		this.habilitado = habilitado;
		this.horaInicial = horaInicial;
		this.horaFinal = horaFinal;
		this.lunes = lunes;
		this.martes = martes;
		this.miercoles = miercoles;
		this.jueves = jueves;
		this.viernes = viernes;
		this.sabado = sabado;
		this.domingo = domingo;
	}




	public String getControlaCantidadIngredientes() {
		return controlaCantidadIngredientes;
	}




	public void setControlaCantidadIngredientes(String controlaCantidadIngredientes) {
		this.controlaCantidadIngredientes = controlaCantidadIngredientes;
	}




	public int getCantidadIngrediantes() {
		return cantidadIngrediantes;
	}




	public void setCantidadIngrediantes(int cantidadIngrediantes) {
		this.cantidadIngrediantes = cantidadIngrediantes;
	}




	public String getNombreProducto() {
		return nombreProducto;
	}




	public void setNombreProducto(String nombreProducto) {
		this.nombreProducto = nombreProducto;
	}




	public String getNombreLiquido() {
		return nombreLiquido;
	}




	public void setNombreLiquido(String nombreLiquido) {
		this.nombreLiquido = nombreLiquido;
	}




	public String getIncluyeliquido() {
		return incluyeliquido;
	}
	public void setIncluyeliquido(String incluyeliquido) {
		this.incluyeliquido = incluyeliquido;
	}
	public int getIdtipoliquido() {
		return idtipoliquido;
	}
	public void setIdtipoliquido(int idtipoliquido) {
		this.idtipoliquido = idtipoliquido;
	}
	public int getIdExcepcion() {
		return idExcepcion;
	}
	public void setIdExcepcion(int idExcepcion) {
		this.idExcepcion = idExcepcion;
	}
	public int getIdProducto() {
		return idProducto;
	}
	public void setIdProducto(int idProducto) {
		this.idProducto = idProducto;
	}
	public double getPrecio() {
		return precio;
	}
	public void setPrecio(double precio) {
		this.precio = precio;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public ExcepcionPrecio(int idExcepcion, int idProducto, double precio, String descripcion, String controlaCantidadIngredientes, int cantidadIngredientes,String incluyeliquido,
			int idtipoliquido, String partiradiciones , String horaInicial, String horaFinal, String lunes, String martes, String miercoles, String jueves, String viernes, String sabado, String domingo, String controlaEspecialidades ) {
		super();
		this.idExcepcion = idExcepcion;
		this.idProducto = idProducto;
		this.precio = precio;
		this.descripcion = descripcion;
		this.controlaCantidadIngredientes = controlaCantidadIngredientes;
		this.cantidadIngrediantes = cantidadIngredientes;
		this.partiradiciones = partiradiciones;
		this.horaInicial = horaInicial;
		this.horaFinal = horaFinal;
		this.lunes = lunes;
		this.martes = martes;
		this.miercoles = miercoles;
		this.jueves = jueves;
		this.viernes = viernes;
		this.sabado = sabado;
		this.domingo = domingo;
		this.controlaEspecialidades = controlaEspecialidades;
	}
	
	
	
	

}

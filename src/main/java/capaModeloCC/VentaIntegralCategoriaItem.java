package capaModeloCC;

/**
 * Un item (idproducto o idespecialidad, segun VentaIntegralCategoria.tipoDato)
 * asociado a una categoria de Venta Integral, en un ambito especifico.
 *
 * El ambito es obligatorio: el mismo concepto puede tener codigos distintos en
 * tienda y en contact center (Estofadas: 469/470 en tienda, 311/312 en CC), asi
 * que nunca se puede asumir que un idvalor sirve para los dos lados.
 */
public class VentaIntegralCategoriaItem {

	public static final String AMBITO_TIENDA = "TIENDA";
	public static final String AMBITO_CONTACTCENTER = "CONTACTCENTER";

	private int idItem;
	private int idCategoria;
	private String ambito;
	private int idValor;
	private String activo;

	public VentaIntegralCategoriaItem() {
	}

	public VentaIntegralCategoriaItem(int idCategoria, String ambito, int idValor) {
		this.idCategoria = idCategoria;
		this.ambito = ambito;
		this.idValor = idValor;
	}

	public int getIdItem() {
		return idItem;
	}

	public void setIdItem(int idItem) {
		this.idItem = idItem;
	}

	public int getIdCategoria() {
		return idCategoria;
	}

	public void setIdCategoria(int idCategoria) {
		this.idCategoria = idCategoria;
	}

	public String getAmbito() {
		return ambito;
	}

	public void setAmbito(String ambito) {
		this.ambito = ambito;
	}

	public int getIdValor() {
		return idValor;
	}

	public void setIdValor(int idValor) {
		this.idValor = idValor;
	}

	public String getActivo() {
		return activo;
	}

	public void setActivo(String activo) {
		this.activo = activo;
	}

}

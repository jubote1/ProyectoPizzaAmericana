package capaModeloCC;

import java.util.ArrayList;

/**
 * Entidad Venta Integral: una categoria parametrizable (Adiciones, Deditos,
 * Americana Premium...) con la forma en que se mide en tienda y en contact
 * center. Reemplaza las categorias que antes vivian hardcodeadas en consultas
 * SQL sueltas.
 */
public class VentaIntegralCategoria {

	public static final String TIPODATO_PRODUCTO = "PRODUCTO";
	public static final String TIPODATO_ESPECIALIDAD = "ESPECIALIDAD";

	public static final String MEDICION_TIENDA_SUMA_CANTIDAD = "SUMA_CANTIDAD";
	public static final String MEDICION_TIENDA_CONTEO_FILAS = "CONTEO_FILAS";

	public static final String MEDICION_CC_CONTEO_PRODUCTO = "CONTEO_PRODUCTO";
	public static final String MEDICION_CC_CONTEO_ESPECIALIDAD_CON_DEDUP = "CONTEO_ESPECIALIDAD_CON_DEDUP";

	private int idCategoria;
	private String nombre;
	private String abreviatura;
	private String tipoDato;
	private String medicionTienda;
	private String excluyeAnuladosTienda;
	private String filtroEstacionTienda;
	private String medicionCC;
	private String activo;
	private int orden;
	private String fechaCreacion;
	private ArrayList<Integer> itemsTienda = new ArrayList<>();
	private ArrayList<Integer> itemsContactCenter = new ArrayList<>();

	public VentaIntegralCategoria() {
	}

	public VentaIntegralCategoria(int idCategoria, String nombre, String abreviatura, String tipoDato,
			String medicionTienda, String excluyeAnuladosTienda, String filtroEstacionTienda, String medicionCC,
			String activo, int orden) {
		this.idCategoria = idCategoria;
		this.nombre = nombre;
		this.abreviatura = abreviatura;
		this.tipoDato = tipoDato;
		this.medicionTienda = medicionTienda;
		this.excluyeAnuladosTienda = excluyeAnuladosTienda;
		this.filtroEstacionTienda = filtroEstacionTienda;
		this.medicionCC = medicionCC;
		this.activo = activo;
		this.orden = orden;
	}

	public int getIdCategoria() {
		return idCategoria;
	}

	public void setIdCategoria(int idCategoria) {
		this.idCategoria = idCategoria;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getAbreviatura() {
		return abreviatura;
	}

	public void setAbreviatura(String abreviatura) {
		this.abreviatura = abreviatura;
	}

	public String getTipoDato() {
		return tipoDato;
	}

	public void setTipoDato(String tipoDato) {
		this.tipoDato = tipoDato;
	}

	public String getMedicionTienda() {
		return medicionTienda;
	}

	public void setMedicionTienda(String medicionTienda) {
		this.medicionTienda = medicionTienda;
	}

	public String getExcluyeAnuladosTienda() {
		return excluyeAnuladosTienda;
	}

	public void setExcluyeAnuladosTienda(String excluyeAnuladosTienda) {
		this.excluyeAnuladosTienda = excluyeAnuladosTienda;
	}

	public boolean isExcluyeAnuladosTienda() {
		return "S".equals(this.excluyeAnuladosTienda);
	}

	public String getFiltroEstacionTienda() {
		return filtroEstacionTienda;
	}

	public void setFiltroEstacionTienda(String filtroEstacionTienda) {
		this.filtroEstacionTienda = filtroEstacionTienda;
	}

	public String getMedicionCC() {
		return medicionCC;
	}

	public void setMedicionCC(String medicionCC) {
		this.medicionCC = medicionCC;
	}

	public String getActivo() {
		return activo;
	}

	public void setActivo(String activo) {
		this.activo = activo;
	}

	public int getOrden() {
		return orden;
	}

	public void setOrden(int orden) {
		this.orden = orden;
	}

	public String getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(String fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public ArrayList<Integer> getItemsTienda() {
		return itemsTienda;
	}

	public void setItemsTienda(ArrayList<Integer> itemsTienda) {
		this.itemsTienda = itemsTienda;
	}

	public ArrayList<Integer> getItemsContactCenter() {
		return itemsContactCenter;
	}

	public void setItemsContactCenter(ArrayList<Integer> itemsContactCenter) {
		this.itemsContactCenter = itemsContactCenter;
	}

}

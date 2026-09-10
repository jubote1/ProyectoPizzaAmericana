package capaModeloCC;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros de la pantalla de segmentacion de clientes.
 *
 * Van en un objeto y no como parametros suelos porque ya son catorce; con la
 * firma anterior cualquier filtro nuevo obligaba a tocar el DAO, el controlador
 * y el servlet solo para pasar el valor de la mano.
 *
 * Todas las listas arrancan vacias, nunca nulas: el DAO pregunta por isEmpty y
 * asi no hay que revisar nulos en cada filtro.
 */
public class FiltroSegmentacion {

	private String fechaInicio = "";
	private String fechaMaxima = "";
	private int minPedidos = 0;
	/** 0 quiere decir sin tope. */
	private int maxPedidos = 0;
	private int diasMinimosSinPublicidad = 0;
	/** 0 quiere decir sin filtro de recencia. */
	private int diasUltimaCompraDesde = 0;
	private int diasUltimaCompraHasta = 0;
	private List<Integer> tiendas = new ArrayList<Integer>();
	private List<Integer> excepciones = new ArrayList<Integer>();
	private List<Integer> productos = new ArrayList<Integer>();
	private List<Integer> especialidades = new ArrayList<Integer>();
	private List<String> canales = new ArrayList<String>();
	private List<String> tiposCliente = new ArrayList<String>();
	/** Deja fuera a los clientes de plataforma, cuyos datos de contacto no son nuestros. */
	private boolean excluirPlataformas = true;

	public String getFechaInicio() { return(this.fechaInicio); }
	public void setFechaInicio(final String v) { this.fechaInicio = (v == null) ? "" : v.trim(); }
	public String getFechaMaxima() { return(this.fechaMaxima); }
	public void setFechaMaxima(final String v) { this.fechaMaxima = (v == null) ? "" : v.trim(); }
	public int getMinPedidos() { return(this.minPedidos); }
	public void setMinPedidos(final int v) { this.minPedidos = v; }
	public int getMaxPedidos() { return(this.maxPedidos); }
	public void setMaxPedidos(final int v) { this.maxPedidos = v; }
	public int getDiasMinimosSinPublicidad() { return(this.diasMinimosSinPublicidad); }
	public void setDiasMinimosSinPublicidad(final int v) { this.diasMinimosSinPublicidad = v; }
	public int getDiasUltimaCompraDesde() { return(this.diasUltimaCompraDesde); }
	public void setDiasUltimaCompraDesde(final int v) { this.diasUltimaCompraDesde = v; }
	public int getDiasUltimaCompraHasta() { return(this.diasUltimaCompraHasta); }
	public void setDiasUltimaCompraHasta(final int v) { this.diasUltimaCompraHasta = v; }
	public List<Integer> getTiendas() { return(this.tiendas); }
	public void setTiendas(final List<Integer> v) { this.tiendas = (v == null) ? new ArrayList<Integer>() : v; }
	public List<Integer> getExcepciones() { return(this.excepciones); }
	public void setExcepciones(final List<Integer> v) { this.excepciones = (v == null) ? new ArrayList<Integer>() : v; }
	public List<Integer> getProductos() { return(this.productos); }
	public void setProductos(final List<Integer> v) { this.productos = (v == null) ? new ArrayList<Integer>() : v; }
	public List<Integer> getEspecialidades() { return(this.especialidades); }
	public void setEspecialidades(final List<Integer> v) { this.especialidades = (v == null) ? new ArrayList<Integer>() : v; }
	public List<String> getCanales() { return(this.canales); }
	public void setCanales(final List<String> v) { this.canales = (v == null) ? new ArrayList<String>() : v; }
	public List<String> getTiposCliente() { return(this.tiposCliente); }
	public void setTiposCliente(final List<String> v) { this.tiposCliente = (v == null) ? new ArrayList<String>() : v; }
	public boolean isExcluirPlataformas() { return(this.excluirPlataformas); }
	public void setExcluirPlataformas(final boolean v) { this.excluirPlataformas = v; }
}

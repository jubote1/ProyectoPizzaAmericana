package capaModeloCC;

import java.util.ArrayList;

/**
 * Un modulo de menu (Funciones, Parametrizacion, Monitoreo...). Con
 * idModuloPadre permite submenus, aunque hoy se use solo un nivel.
 */
public class MenuModulo {

	private int idModulo;
	private String nombre;
	private int orden;
	private Integer idModuloPadre;
	private ArrayList<Pantalla> pantallas = new ArrayList<>();

	public MenuModulo() {
	}

	public MenuModulo(int idModulo, String nombre, int orden, Integer idModuloPadre) {
		this.idModulo = idModulo;
		this.nombre = nombre;
		this.orden = orden;
		this.idModuloPadre = idModuloPadre;
	}

	public int getIdModulo() {
		return idModulo;
	}

	public void setIdModulo(int idModulo) {
		this.idModulo = idModulo;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getOrden() {
		return orden;
	}

	public void setOrden(int orden) {
		this.orden = orden;
	}

	public Integer getIdModuloPadre() {
		return idModuloPadre;
	}

	public void setIdModuloPadre(Integer idModuloPadre) {
		this.idModuloPadre = idModuloPadre;
	}

	public ArrayList<Pantalla> getPantallas() {
		return pantallas;
	}

	public void setPantallas(ArrayList<Pantalla> pantallas) {
		this.pantallas = pantallas;
	}

}

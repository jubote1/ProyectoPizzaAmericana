package PedidoInfoAdicional;

/**
 * Clase que implementa la entidad Detalle Pedido
 * @author JuanDavid
 *
 */
public class DetallePedido implements Cloneable {
	
	private int iddetallepedido;
	private int idproducto;
	private String nombreproducto;
	private int idpedido;
	private double cantidad;
	private String nombreespecialidad1;
	private String nombreespecialidad2;
	private int idespecialidad1;
	private int idespecialidad2;
	private double valorunitario;
	private double valortotal;
	private String adicion;
	private String observacion;
	private int idsabortipoliquido;
	private String liquido;
	private int idexcepcion;
	private String excepcion;
	private String modespecialidad1;
	private String modespecialidad2;
	private String tipoProducto;
	
	
	
	public Object clone(){
        Object obj=null;
        try{
            obj=super.clone();
        }catch(CloneNotSupportedException ex){
            System.out.println(" no se puede duplicar");
        }
        return obj;
    }
	
	public String getTipoProducto() {
		return tipoProducto;
	}
	public void setTipoProducto(String tipoProducto) {
		this.tipoProducto = tipoProducto;
	}
	public String getModespecialidad1() {
		return modespecialidad1;
	}
	public void setModespecialidad1(String modespecialidad1) {
		this.modespecialidad1 = modespecialidad1;
	}
	public String getModespecialidad2() {
		return modespecialidad2;
	}
	public void setModespecialidad2(String modespecialidad2) {
		this.modespecialidad2 = modespecialidad2;
	}
	public String getNombreproducto() {
		return nombreproducto;
	}
	public void setNombreproducto(String nombreproducto) {
		this.nombreproducto = nombreproducto;
	}
	public String getNombreespecialidad1() {
		return nombreespecialidad1;
	}
	public void setNombreespecialidad1(String nombreespecialidad1) {
		this.nombreespecialidad1 = nombreespecialidad1;
	}
	public String getNombreespecialidad2() {
		return nombreespecialidad2;
	}
	public void setNombreespecialidad2(String nombreespecialidad2) {
		this.nombreespecialidad2 = nombreespecialidad2;
	}
	public String getLiquido() {
		return liquido;
	}
	public void setLiquido(String liquido) {
		this.liquido = liquido;
	}
	public String getExcepcion() {
		return excepcion;
	}
	public void setExcepcion(String excepcion) {
		this.excepcion = excepcion;
	}
	public int getIdexcepcion() {
		return idexcepcion;
	}
	public void setIdexcepcion(int idexcepcion) {
		this.idexcepcion = idexcepcion;
	}
	public int getIdsabortipoliquido() {
		return idsabortipoliquido;
	}
	public void setIdsabortipoliquido(int idsabortipoliquido) {
		this.idsabortipoliquido = idsabortipoliquido;
	}
	public String getAdicion() {
		return adicion;
	}
	public void setAdicion(String adicion) {
		this.adicion = adicion;
	}
	public String getObservacion() {
		return observacion;
	}
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}
	public int getIddetallepedido() {
		return iddetallepedido;
	}
	public void setIddetallepedido(int iddetallepedido) {
		this.iddetallepedido = iddetallepedido;
	}
	public int getIdproducto() {
		return idproducto;
	}
	public void setIdproducto(int idproducto) {
		this.idproducto = idproducto;
	}
	public int getIdpedido() {
		return idpedido;
	}
	public void setIdpedido(int idpedido) {
		this.idpedido = idpedido;
	}
	public double getCantidad() {
		return cantidad;
	}
	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
	public int getIdespecialidad1() {
		return idespecialidad1;
	}
	public void setIdespecialidad1(int idespecialidad1) {
		this.idespecialidad1 = idespecialidad1;
	}
	public int getIdespecialidad2() {
		return idespecialidad2;
	}
	public void setIdespecialidad2(int idespecialidad2) {
		this.idespecialidad2 = idespecialidad2;
	}
	public double getValorunitario() {
		return valorunitario;
	}
	public void setValorunitario(double valorunitario) {
		this.valorunitario = valorunitario;
	}
	public double getValortotal() {
		return valortotal;
	}
	public void setValortotal(double valortotal) {
		this.valortotal = valortotal;
	}
	public DetallePedido(int iddetallepedido, int idproducto, int idpedido, double cantidad, int idespecialidad1,
			int idespecialidad2, double valorunitario, double valortotal) {
		super();
		this.iddetallepedido = iddetallepedido;
		this.idproducto = idproducto;
		this.idpedido = idpedido;
		this.cantidad = cantidad;
		this.idespecialidad1 = idespecialidad1;
		this.idespecialidad2 = idespecialidad2;
		this.valorunitario = valorunitario;
		this.valortotal = valortotal;
	}
	public DetallePedido(int idproducto, int idpedido, double cantidad, int idespecialidad1, int idespecialidad2,
			double valorunitario, double valortotal, String adicion, String observacion, int idsabortipoliquido,
			int idexcepcion, String modespecialidad1, String modespecialidad2 ) {
		super();
		this.idproducto = idproducto;
		this.idpedido = idpedido;
		this.cantidad = cantidad;
		this.idespecialidad1 = idespecialidad1;
		this.idespecialidad2 = idespecialidad2;
		this.valorunitario = valorunitario;
		this.valortotal = valortotal;
		this.adicion = adicion;
		this.observacion = observacion;
		this.idsabortipoliquido = idsabortipoliquido;
		this.idexcepcion = idexcepcion;
		this.modespecialidad1 = modespecialidad1;
		this.modespecialidad2 = modespecialidad2;
	}
	public DetallePedido(int iddetallepedido, String nombreproducto, int idproducto, double cantidad, String nombreespecialidad1, int idespecialidad1,
			String nombreespecialidad2, int idespecialidad2, double valorunitario, double valortotal, String adicion, String observacion,
			String liquido, String excepcion, int idpedido, int idexcepcion, int idsabortipoliquido, String tipoProducto) {
		super();
		this.iddetallepedido = iddetallepedido;
		this.nombreproducto = nombreproducto;
		this.idproducto = idproducto;
		this.cantidad = cantidad;
		this.nombreespecialidad1 = nombreespecialidad1;
		this.idespecialidad1 = idespecialidad1;
		this.nombreespecialidad2 = nombreespecialidad2;
		this.idespecialidad2 = idespecialidad2;
		this.valorunitario = valorunitario;
		this.valortotal = valortotal;
		this.adicion = adicion;
		this.observacion = observacion;
		this.liquido = liquido;
		this.excepcion = excepcion;
		this.idpedido = idpedido;
		this.idexcepcion = idexcepcion; 
		this.idsabortipoliquido = idsabortipoliquido;
		this.tipoProducto =  tipoProducto;
	}
	
	public DetallePedido(int iddetallepedido, String nombreproducto, int idproducto, double cantidad, String nombreespecialidad1, int idespecialidad1,
			String nombreespecialidad2, int idespecialidad2, double valorunitario, double valortotal, String adicion, String observacion,
			String liquido, String excepcion, int idpedido, int idexcepcion, int idsabortipoliquido) {
		super();
		this.iddetallepedido = iddetallepedido;
		this.nombreproducto = nombreproducto;
		this.idproducto = idproducto;
		this.cantidad = cantidad;
		this.nombreespecialidad1 = nombreespecialidad1;
		this.idespecialidad1 = idespecialidad1;
		this.nombreespecialidad2 = nombreespecialidad2;
		this.idespecialidad2 = idespecialidad2;
		this.valorunitario = valorunitario;
		this.valortotal = valortotal;
		this.adicion = adicion;
		this.observacion = observacion;
		this.liquido = liquido;
		this.excepcion = excepcion;
		this.idpedido = idpedido;
		this.idexcepcion = idexcepcion; 
		this.idsabortipoliquido = idsabortipoliquido;
	}
	
	
	

}

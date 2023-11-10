package pixelposCC;
import java.sql.Date;
import java.sql.Timestamp;

public class PosDetailPixel {
	
	private int idposdetail;
	private int numfactura;
	private int idproductoext;
	private int whooder;
	private int whoauth;
	private double costeach;
	private double cantidad;
	private Timestamp timeord;
	private int recpos;
	private int prodtype;
	private int applytax1;
	private int storenum;
	private java.sql.Date opendate;
	private String linedes; 
	private int masteritem;
	private int questionid;
	private double origcostech;
	private double netcosteach;
		
	public int getIdposdetail() {
		return idposdetail;
	}
	public void setIdposdetail(int idposdetail) {
		this.idposdetail = idposdetail;
	}
	public int getNumfactura() {
		return numfactura;
	}
	public void setNumfactura(int numfactura) {
		this.numfactura = numfactura;
	}
	public int getIdproductoext() {
		return idproductoext;
	}
	public void setIdproductoext(int idproductoext) {
		this.idproductoext = idproductoext;
	}
	public int getWhooder() {
		return whooder;
	}
	public void setWhooder(int whooder) {
		this.whooder = whooder;
	}
	public int getWhoauth() {
		return whoauth;
	}
	public void setWhoauth(int whoauth) {
		this.whoauth = whoauth;
	}
	public double getCosteach() {
		return costeach;
	}
	public void setCosteach(double costeach) {
		this.costeach = costeach;
	}
	public double getCantidad() {
		return cantidad;
	}
	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
	public Timestamp getTimeord() {
		return timeord;
	}
	public void setTimeord(Timestamp timeord) {
		this.timeord = timeord;
	}
	public int getRecpos() {
		return recpos;
	}
	public void setRecpos(int recpos) {
		this.recpos = recpos;
	}
	public int getProdtype() {
		return prodtype;
	}
	public void setProdtype(int prodtype) {
		this.prodtype = prodtype;
	}
	public int getApplytax1() {
		return applytax1;
	}
	public void setApplytax1(int applytax1) {
		this.applytax1 = applytax1;
	}
	public int getStorenum() {
		return storenum;
	}
	public void setStorenum(int storenum) {
		this.storenum = storenum;
	}
	public java.sql.Date getOpendate() {
		return opendate;
	}
	public void setOpendate(java.sql.Date opendate) {
		this.opendate = opendate;
	}
	public String getLinedes() {
		return linedes;
	}
	public void setLinedes(String linedes) {
		this.linedes = linedes;
	}	
	public int getMasteritem() {
		return masteritem;
	}
	public void setMasteritem(int masteritem) {
		this.masteritem = masteritem;
	}
	public int getQuestionid() {
		return questionid;
	}
	public void setQuestionid(int questionid) {
		this.questionid = questionid;
	}
	public double getOrigcostech() {
		return origcostech;
	}
	public void setOrigcostech(double origcostech) {
		this.origcostech = origcostech;
	}
	public double getNetcosteach() {
		return netcosteach;
	}
	public void setNetcosteach(double netcosteach) {
		this.netcosteach = netcosteach;
	}
	public PosDetailPixel(int idposdetail, int numfactura, int idproductoext, int whooder, int whoauth, double costeach,
			double cantidad, Timestamp timeord, int recpos, int prodtype, int applytax1, int storenum, Date opendate,
			String linedes, int masteritem, int questionid, double origcostech, double netcosteach) {
		super();
		this.idposdetail = idposdetail;
		this.numfactura = numfactura;
		this.idproductoext = idproductoext;
		this.whooder = whooder;
		this.whoauth = whoauth;
		this.costeach = costeach;
		this.cantidad = cantidad;
		this.timeord = timeord;
		this.recpos = recpos;
		this.prodtype = prodtype;
		this.applytax1 = applytax1;
		this.storenum = storenum;
		this.opendate = opendate;
		this.linedes = linedes;
		this.masteritem = masteritem;
		this.questionid = questionid;
		this.origcostech = origcostech;
		this.netcosteach = netcosteach;
	}
}

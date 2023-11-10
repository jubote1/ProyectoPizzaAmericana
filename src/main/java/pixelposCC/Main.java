package pixelposCC;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import capaModeloCC.Cliente;
import conexionCC.ConexionBaseDatos;
import pixelposCC.DetallePedidoPixel;

import java.text.*;

public class Main {
	
	public static ArrayList<PosDetailPixel> listPosdetail = new ArrayList<PosDetailPixel>();
	public static double Total = 0,NetTotal = 0,Tax1 = 0;
	
	public static String PriceLetter(int a){
		String Prices[]={"PRICEMODE","PRICEA","PRICEB","PRICEC","PRICED","PRICEE","PRICEF","PRICEG","PRICEH","PRICEI","PRICEJ"};
		String Letter = null;
		
		if(a >=0 && a < Prices.length){
			Letter = Prices[a];
		}else{
			Letter = Prices[1];
		}
		return Letter;
	}
	
	public static void PosDetail(Connection con,ArrayList<capaModeloCC.DetallePedidoPixel> pruebaPedido,int idEmployee,int NumFactura,java.sql.Date OpenDate){
		
		try {
			double Tax1 = 0, Quan=0, CostEach=0, OrigCostEach=0, NetCostEach=0;
			int i=0,j=0,l=0;
			int ApplyTax1 = 0,idPosDetail = 0 ,prodnum=0,recpos = 0,MasterItem = 0,QuestionId = -1, ProdType=0, StoreNum=0;
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			PreparedStatement Tax = null,StorenumAndPrice = null,NetandOrigCost = null;
			CallableStatement AutoInc = null;
			ResultSet rsTax = null,rsStorenumAndPrice = null,rsNetandOrigCost = null;
			PosDetailPixel temporal = null;
			String LineDes = null;
			String Question[] = {"QUESTION1","QUESTION2","QUESTION3","QUESTION4","QUESTION5"};
			String SqlPrice = "SELECT "+PriceLetter(0)+" FROM dba.Product WHERE PRODNUM = ?";
			String SqlStorenumAndPrice = "SELECT  a.pricemode, a.OPTIONINDEX FROM DBA.forcedchoices a , dba.questions b , dba.product c "
					+ "where a.optionindex = b.optionindex and a.choice = c.prodnum and a.optionindex = (SELECT "+Question[0]+" FROM dba.Product "
					+ "WHERE ISACTIVE = 1 AND "+Question[0]+" > 0 AND PRODNUM = ? ) AND c.PRODNUM = ? AND a.IsActive = 1 order by a.sequence asc";
			
			
			for(capaModeloCC.DetallePedidoPixel cadaDetallePedido:pruebaPedido){
				ts = new Timestamp(System.currentTimeMillis());
				prodnum = cadaDetallePedido.getIdproductoext();
				Quan = cadaDetallePedido.getCantidad();
				
				if(prodnum != 0){
					//Obteniendo idPosDetail
					AutoInc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
					AutoInc.registerOutParameter(1, Types.INTEGER);
					AutoInc.setString(2, "GETNEXT_POSDETAIL");
					AutoInc.execute();
					idPosDetail = AutoInc.getInt(1);
					AutoInc.close();
					
					//Impuesto
					Tax = con.prepareStatement("SELECT USEITEMCAT FROM dba.Product WHERE PRODNUM = ?");
					Tax.setInt(1, prodnum);
					rsTax = Tax.executeQuery();
					rsTax.next();
					ApplyTax1 = rsTax.getInt(1);
					Tax.close();
					rsTax.close();
				}
				
				if(recpos == 0){
					MasterItem = idPosDetail;
				}else if (prodnum == 0) {
					MasterItem = 0;
				}else if (MasterItem == 0 && recpos != 0) {
					MasterItem = idPosDetail;
				}
				
				if (idPosDetail == MasterItem) {
					QuestionId = 0;
				}else if (prodnum == 2002) {
					QuestionId = MasterItem;
				}
				
				if(prodnum == 2002){
					if (l == 0) {
						LineDes = "Dividir 1 de 2";
						l++;
					}else if (l == 1) {
						LineDes = "Dividir 1 de 2";
						l++;
					}else if (l == 2) {
						LineDes = "Fin de División";
						l = 0;
					}
				}
							
				listPosdetail.add(new PosDetailPixel(idPosDetail,NumFactura,prodnum,idEmployee,idEmployee,CostEach,
						Quan,ts,recpos,ProdType,ApplyTax1,StoreNum,OpenDate,LineDes,MasterItem,QuestionId,OrigCostEach,NetCostEach));
				
				if(prodnum != 0){
					recpos++;				
				}
				
				QuestionId = -1;
				
			}
			
			for (i=0 ; i < listPosdetail.size() ;i++) {
				if (listPosdetail.get(i).getIdproductoext() != 0 & listPosdetail.get(i).getIdproductoext() != 2002) {
					for (j = i+1; j < listPosdetail.size() ;j++) {
						if(listPosdetail.get(j).getIdproductoext() == 0) {
							j=listPosdetail.size();
						}else if (listPosdetail.get(j).getIdproductoext() != 2002) {
							for (l = 0; l < Question.length; l++) {
								SqlStorenumAndPrice = "SELECT  a.pricemode, a.OPTIONINDEX FROM DBA.forcedchoices a , dba.questions b , dba.product c "
										+ "where a.optionindex = b.optionindex and a.choice = c.prodnum and a.optionindex = (SELECT "+Question[l]+" FROM dba.Product "
										+ "WHERE ISACTIVE = 1 AND "+Question[l]+" > 0 AND PRODNUM = ? ) AND c.PRODNUM = ? AND a.IsActive = 1 order by a.sequence asc";
								StorenumAndPrice = con.prepareStatement(SqlStorenumAndPrice);
								StorenumAndPrice.setInt(1, listPosdetail.get(i).getIdproductoext());
								StorenumAndPrice.setInt(2, listPosdetail.get(j).getIdproductoext());
								rsStorenumAndPrice = StorenumAndPrice.executeQuery();
								
								while (rsStorenumAndPrice.next()) {
									SqlPrice = "SELECT "+PriceLetter(rsStorenumAndPrice.getInt(1))+" FROM dba.Product WHERE PRODNUM = ?";
									NetandOrigCost = con.prepareStatement(SqlPrice);
									NetandOrigCost.setInt(1, listPosdetail.get(j).getIdproductoext());
									rsNetandOrigCost = NetandOrigCost.executeQuery();
									rsNetandOrigCost.next();
									NetCostEach = rsNetandOrigCost.getDouble(1);
									OrigCostEach = NetCostEach;
									NetandOrigCost.close();
									rsNetandOrigCost.close();
									
									temporal = listPosdetail.get(j);
									temporal.setStorenum(rsStorenumAndPrice.getInt(2));
									temporal.setNetcosteach(NetCostEach);
									temporal.setOrigcostech(OrigCostEach);
									listPosdetail.set(j, temporal);
									
									NetCostEach = 0;
									OrigCostEach = 0;
									
								}
								StorenumAndPrice.close();
								rsStorenumAndPrice.close();
							}
						}
					}
					if (listPosdetail.get(i).getStorenum() == 0 ) {
						SqlPrice = "SELECT PRICEA FROM dba.Product WHERE PRODNUM = ?";
						NetandOrigCost = con.prepareStatement(SqlPrice);
						NetandOrigCost.setInt(1, listPosdetail.get(i).getIdproductoext());
						rsNetandOrigCost = NetandOrigCost.executeQuery();
						rsNetandOrigCost.next();
						NetCostEach = rsNetandOrigCost.getDouble(1);
						OrigCostEach = NetCostEach;
						NetandOrigCost.close();
						rsNetandOrigCost.close();
						
						temporal = listPosdetail.get(i);
						temporal.setNetcosteach(NetCostEach);
						temporal.setOrigcostech(OrigCostEach);
						listPosdetail.set(i, temporal);
						
						NetCostEach = 0;
						OrigCostEach = 0;
					}
				}
			}
			
			for (i=0 ; i < listPosdetail.size() ;i++) {
				if(listPosdetail.get(i).getIdproductoext() != 0 & listPosdetail.get(i).getIdproductoext() != 2002){
					if(listPosdetail.get(i).getApplytax1() == 1){
						Tax1 = 0.08;
					}else{
						Tax1 = 0;
					}
					CostEach = listPosdetail.get(i).getOrigcostech()/(1+Tax1);
					temporal = listPosdetail.get(i);
					temporal.setCosteach(CostEach);
					listPosdetail.set(i, temporal);
					
					CostEach = 0;
				}
			}						
		} catch (SQLException e) {
			e.getMessage();
		}	
	}
	
	public static void PosHeader(Connection con,int idEmployee,int NumFactura,int MemCode,int MethodNum,double Change,
			java.sql.Timestamp Inits,java.sql.Date OpenDate) {
		try {
			int i=0,intPunchIndex=0,idHowpaid=0;
			double FinalTotal = 0,Tax1Able = 0,NetTotal=0,Tax1=0;
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			CallableStatement proc = null;
			PreparedStatement PosHeader = null,PunchIndex = null,Howpaid = null;
			ResultSet rs = null;
			String sqlPosHeader =  "INSERT INTO DBA.POSHEADER (TRANSACT,TABLENUM,TIMESTART,TIMEEND,NUMCUST,TAX1,TAX2,TAX3,TAX4,TAX5,"
					 +"TAX1ABLE,TAX2ABLE,TAX3ABLE,TAX4ABLE,TAX5ABLE,NETTOTAL,WHOSTART,WHOCLOSE,ISSPLIT,SALETYPEINDEX,EXP,STATNUM,STATUS,FINALTOTAL,"
					 +"PUNCHINDEX,Gratuity,OPENDATE,MemCode,TotalPoints,PointsApplied,UpdateStatus,ISDelivery,ScheduleDate,Tax1Exempt,Tax2Exempt,"
					 +"Tax3Exempt,Tax4Exempt,Tax5Exempt,MEMRATE,MealTime,IsInternet,RevCenter,PunchIdxStart,StatNumStart,SecNum,GratAmount,ShipTo, EnforcedGrat) "
					 +"VALUES(?,30001,?,?,1,?,0,0,0,0,?,0,0,0,0,?,?,?,1,1005,1,1,3,?,?,0,?,?,0,0,1,1,?,0,0,0,0,0,1,1,0,999,?,1,0,0,0,0)";
			String SqlHowpaid = "INSERT INTO DBA.Howpaid(HowPaidLink,TRANSDATE,EMPNUM,TENDER,METHODNUM,CHANGE,AUTHORIZED,MEMCODE,ExchangeRate,"
					 +"TRANSACT,PayType,OPENDATE,PUNCHINDEX,UpdateStatus,Settled,Status,Approved,STATNUM,IsPayInOut,MealTime,RevCenter,Voided,VoidedLink)"
					 +"VALUES(?,?,?,?,?,?,199,?,1,?,114,?,?,1,1,3,1,1,0,1,999,0,0)";
			
			//Obteniendo PunchIndex
			PunchIndex = con.prepareStatement("SELECT a.PunchIndex FROM DBA.PUNCHCLOCK a WHERE  a.EmpNUM = 2007 AND a.OPENDATE = ?");
			PunchIndex.setDate(1, OpenDate);
			rs = PunchIndex.executeQuery();
			rs.next();
			intPunchIndex = rs.getInt(1);
			PunchIndex.close();
			rs.close();
			 
			for (i=0 ; i < listPosdetail.size() ; i++) {
				if (listPosdetail.get(i).getIdproductoext() != 0 & listPosdetail.get(i).getIdproductoext() != 2002) {
					FinalTotal += listPosdetail.get(i).getOrigcostech()*listPosdetail.get(i).getCantidad();
					NetTotal += listPosdetail.get(i).getCosteach()*listPosdetail.get(i).getCantidad();
					if (listPosdetail.get(i).getApplytax1() == 1) {
						Tax1Able += listPosdetail.get(i).getCosteach()*listPosdetail.get(i).getCantidad();
					}
				}
			}
			
			NetTotal = Math.round(NetTotal);
			Tax1 = FinalTotal-NetTotal;
			
			PosHeader = con.prepareStatement(sqlPosHeader);
			PosHeader.setInt(1, NumFactura);//TRANSACT
			PosHeader.setTimestamp(2, ts);//TIMESTART
			PosHeader.setTimestamp(3, ts);//TIMEEND
			PosHeader.setDouble(4, Tax1);//TAX1
			PosHeader.setDouble(5, Tax1Able);//TAX1ABLE
			PosHeader.setDouble(6, NetTotal);//NETTOTTAL
			PosHeader.setInt(7, idEmployee);//WHOSTART
			PosHeader.setInt(8, idEmployee);//WHOCLOSE
			PosHeader.setDouble(9, FinalTotal);//FINALTOTAL 
			PosHeader.setInt(10, intPunchIndex);//PUNCHINDEX
			PosHeader.setDate(11, OpenDate);//OPENDATE
			PosHeader.setInt(12, MemCode);//Memcode
			PosHeader.setTimestamp(13, Inits);//ScheduleDate
			PosHeader.setInt(14, intPunchIndex);//PunchIdxStart
			 
			PosHeader.execute();
			PosHeader.close();
			
			//Insertando nueva transacción en DBA.Howpaid
			ts = new Timestamp(System.currentTimeMillis());
			 
			proc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
			proc.registerOutParameter(1, Types.INTEGER);
			proc.setString(2, "GETNEXT_HowPaid");
			proc.execute();
			idHowpaid = proc.getInt(1);
			proc.close();
			 
			Howpaid = con.prepareStatement(SqlHowpaid);
			Howpaid.setInt(1, idHowpaid);//HowPaidLink
			Howpaid.setTimestamp(2, ts);//TRANSDATE
			Howpaid.setInt(3, idEmployee);//EMPNUM
			Howpaid.setDouble(4, FinalTotal);//TENDER
			Howpaid.setInt(5, MethodNum);//METHODNUM
			Howpaid.setDouble(6, Change-FinalTotal);//CHANGE
			Howpaid.setInt(7, MemCode);//MEMCODE
			Howpaid.setInt(8, NumFactura);//TRANSACT
			Howpaid.setDate(9, OpenDate);//OPENDATE
			Howpaid.setInt(10, intPunchIndex);//PUNCHINDEX
			 
			Howpaid.execute();
			Howpaid.close();
			 
		} catch (SQLException e) {
			e.getMessage();			
		}
	}

	public static void RunPosDetail(Connection con) {
		try {
			PreparedStatement AdPosDetail = null;
			String SqlPosDetail = "INSERT INTO DBA.POSDETAIL(UNIQUEID,TRANSACT,PRODNUM,WHOORDER,WHOAUTH,"
					+"COSTEACH,QUAN,TIMEORD,PRINTLOC,SEATNUM,Minutes,NOTAX,HOWORDERED,STATUS,NEXTPOS,"
					+"PRIORPOS,RECPOS,PRODTYPE,ApplyTax1,Applytax2,Applytax3,Applytax4,Applytax5,"
					+"ReduceInventory,StoreNum,STATNUM,RecipeCostEach,OpenDate,"
					+"MealTime,LineDes,REVCENTER,MasterItem,QuestionId,OrigCostEach,NetCostEach,UpdateStatus) "
					+"VALUES (?,?,?,?,?,"
					+"?,?,?,1,0,0,0,16,5000000,0,"
					+"0,?,?,?,0,0,0,0,"
					+"1,?,1,0,?,"
					+"1,?,999,?,?,?,?,1)";
			
			for(PosDetailPixel RunList: listPosdetail){
				if (RunList.getIdproductoext() != 0) {
					//Insertando en tabla PosDetail
					AdPosDetail = con.prepareStatement(SqlPosDetail);
					AdPosDetail.setInt(1, RunList.getIdposdetail());//UNIQUEID
					AdPosDetail.setInt(2, RunList.getNumfactura());//TRANSACT
					AdPosDetail.setInt(3, RunList.getIdproductoext());//PRODNUM
					AdPosDetail.setInt(4, RunList.getWhooder());//WHOORDER
					AdPosDetail.setInt(5, RunList.getWhoauth());//WHOAUTH
					AdPosDetail.setDouble(6, RunList.getCosteach());//COSTEACH
					AdPosDetail.setDouble(7, RunList.getCantidad());//QUAN
					AdPosDetail.setTimestamp(8, RunList.getTimeord());//TIMEORD
					AdPosDetail.setInt(9, RunList.getRecpos());//RECPOS
					AdPosDetail.setInt(10, RunList.getProdtype());//PRODTYPE
					AdPosDetail.setInt(11, RunList.getApplytax1());//ApplyTax1
					AdPosDetail.setInt(12, RunList.getStorenum());//StoreNum
					AdPosDetail.setDate(13, RunList.getOpendate());//OpenDate
					AdPosDetail.setString(14, RunList.getLinedes());//LineDes
					AdPosDetail.setInt(15, RunList.getMasteritem());//MasterItem
					AdPosDetail.setInt(16, RunList.getQuestionid());//QuestionId
					AdPosDetail.setDouble(17, RunList.getOrigcostech());//OrigCostEach
					AdPosDetail.setDouble(18, RunList.getNetcosteach());//NetCostEach
					
					AdPosDetail.execute();
					AdPosDetail.close();
				}
			}
			
			
		} catch (SQLException e) {
			e.getMessage();
		}
	}
	
	public static void Recipe(Connection con,ArrayList<capaModeloCC.DetallePedidoPixel> pruebaPedido,int NumFactura){
		try {
			int ProdNum=0,InvenNum=0,idAdjustInventory=0;
			double Quan=0,CalcQuan=0,PosUsed=0,Units=0;
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			PreparedStatement Receta = null,StockLevels=null,UpdateStock=null,AdjustInventory=null;
			CallableStatement AutoInc = null;
			ResultSet rs = null,rsStockLevels=null;
			//Simplificar Querys
			String SqlStockLevels = "SELECT INVENNUM,UNITS,POSUSED,StartInv FROM DBA.StockLevels WHERE INVENNUM = ?";
			String SqlUpdateStock = "UPDATE dba.StockLevels SET UNITS = ? , POSUSED = ? WHERE INVENNUM = ?";
			String RecetaSQL = "SELECT a.UNIQUEID,a.PRODNUM,b.DESCRIPT,c.INVENNUM,c.DESCRIPT,a.USAGE,c.UNITDES "
					+ "FROM DBA.RECIPE a ,dba.Product b , dba.inventory c WHERE  a.PRODNUM = b.PRODNUM "
					+ "AND  a.INVENNUM = c.INVENNUM AND a.IsActive = 1 AND a.PRODNUM = ? ORDER BY a.INVENNUM asc";
			String SqlAdjustInventory = "INSERT INTO DBA.AdjustInventory (ADJUSTNUM,INVENNUM,ADJUSTUNITS,WAREHOUSENUM,"
					+ "WHOADJUST,EMPREF,Reason,AdjustType,SupplyNum,AdjustTime,WareHouseTO,STATE,PurPrice,ADJUSTCOST,"
					+ "PURID,TransNum) VALUES (?,?,?,1001,0,0,'POS Used',1,0,?,1001,1,0,0,1005,?)";
			
			for (capaModeloCC.DetallePedidoPixel cadaDetallePedido:pruebaPedido) {
				ProdNum = cadaDetallePedido.getIdproductoext();
				Quan = cadaDetallePedido.getCantidad();
				if (ProdNum != 0 & ProdNum != 2002) {
					Receta = con.prepareStatement(RecetaSQL);
					Receta.setInt(1, ProdNum);
					rs = Receta.executeQuery();
					while(rs.next()){
						InvenNum = rs.getInt(4);
						CalcQuan = Quan*rs.getDouble(6);
						StockLevels = con.prepareStatement(SqlStockLevels);
						StockLevels.setInt(1, InvenNum);
						rsStockLevels = StockLevels.executeQuery();
						while (rsStockLevels.next()) {
							PosUsed = CalcQuan+rsStockLevels.getDouble(3);
							Units = rsStockLevels.getDouble(4)-PosUsed;
							UpdateStock = con.prepareStatement(SqlUpdateStock);
							UpdateStock.setDouble(1, Units);
							UpdateStock.setDouble(2, PosUsed);
							UpdateStock.setInt(3, InvenNum);
							UpdateStock.executeQuery();
							UpdateStock.close();
							
							//Obteniendo idAdjustInventory
							ts = new Timestamp(System.currentTimeMillis());
							AutoInc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
							AutoInc.registerOutParameter(1, Types.INTEGER);
							AutoInc.setString(2, "GETNEXT_StockLevel");
							AutoInc.execute();
							idAdjustInventory = AutoInc.getInt(1);
							AutoInc.close();
							
							AdjustInventory = con.prepareStatement(SqlAdjustInventory);
							AdjustInventory.setInt(1, idAdjustInventory);
							AdjustInventory.setInt(2, InvenNum);
							AdjustInventory.setDouble(3, CalcQuan);
							AdjustInventory.setTimestamp(4, ts);
							AdjustInventory.setInt(5, NumFactura);
							
							AdjustInventory.execute();
							AdjustInventory.close();
						}
						StockLevels.close();
						rsStockLevels.close();
					}
					Receta.close();
					rs.close();
					CalcQuan = 0;
				}
				ProdNum = 0;
				Quan = 0;
			}
		} catch (SQLException e) {
			e.getMessage();
		}
	}
	
	public static void Invoice(Connection con,int Numfactura,int idEmployee,int MemCode) {
		try {
			int Invoice1 = 0,idCloseTran = 0;
			String SqlStrInvoice = "Select DBA.StrInvoice(?,?,?,?)",SqlStrCloseTran = "Select DBA.StrCloseTran(?)";
			String SqlInvoice = "INSERT INTO DBA.MsgMgr(MsgNum,MsgTime,MsgType,MsgPrm,Data) VALUES (?,?,5,2002,?)";
			String SqlCloseTrans = "INSERT INTO DBA.MsgMgr(MsgNum,MsgTime,MsgType,MsgPrm,Data) VALUES (?,?,8,0,?)";
			String DeleteMsgNum = "DELETE FROM DBA.MsgMgr WHERE MsgNum = ?";
			String StrInvoice = "",StrCloseTran = "",StrCloseTran1 = "CLOSETRAN\r\n1,"+Integer.toString(Numfactura)+",1005\r\n1001";
			String Central = null;
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			PreparedStatement Invoice = null,CloseTran = null,MsgMgr = null;
			CallableStatement proc = null;
			ResultSet rsInvoice = null,rsCloseTran = null;
			
			Central = CentralZone(con);
			
			//String Invoice
			Invoice = con.prepareStatement(SqlStrInvoice);
			Invoice.setInt(1, Numfactura);
			Invoice.setInt(2, idEmployee);
			Invoice.setInt(3, MemCode);
			Invoice.setString(4, Central);
			
			rsInvoice = Invoice.executeQuery();
			rsInvoice.next();
			StrInvoice = rsInvoice.getString(1);
			
			System.out.println(StrInvoice);
			Invoice.close();
			rsInvoice.close();
			
			//Insertando Factura
			proc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
			proc.registerOutParameter(1, Types.INTEGER);
			proc.setString(2, "GetNext_MsgMgr");
			proc.execute();
			Invoice1 = proc.getInt(1);
			proc.close();
			
			ts = new Timestamp(System.currentTimeMillis());
			MsgMgr = con.prepareStatement(SqlInvoice);
			MsgMgr.setInt(1, Invoice1);
			MsgMgr.setTimestamp(2, ts);
			MsgMgr.setString(3, StrInvoice);
			
			MsgMgr.execute();
			MsgMgr.close();
			
			//String CloseTran
			CloseTran = con.prepareStatement(SqlStrCloseTran);
			CloseTran.setInt(1, Numfactura);
			
			rsCloseTran = Invoice.executeQuery();
			rsCloseTran.next();
			StrCloseTran = rsCloseTran.getString(1);
			
			System.out.println(StrCloseTran);
			CloseTran.close();
			rsCloseTran.close();
			
			//Insertando CloseTran
			proc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
			proc.registerOutParameter(1, Types.INTEGER);
			proc.setString(2, "GetNext_MsgMgr");
			proc.execute();
			idCloseTran = proc.getInt(1);
			proc.close();
			
			ts = new Timestamp(System.currentTimeMillis());
			MsgMgr = con.prepareStatement(SqlCloseTrans);
			MsgMgr.setInt(1, Numfactura);
			MsgMgr.setTimestamp(2, ts);
			MsgMgr.setString(3, StrCloseTran);
			
			MsgMgr.execute();
			MsgMgr.close();
			
			//Eliminando Factura
			MsgMgr = con.prepareStatement(DeleteMsgNum);
			MsgMgr.setInt(1, Invoice1);
			
			MsgMgr.execute();
			MsgMgr.close();
			
			//Eliminando CloseTran
			MsgMgr = con.prepareStatement(DeleteMsgNum);
			MsgMgr.setInt(1, idCloseTran);
			
			MsgMgr.execute();
			MsgMgr.close();
			
		} catch (SQLException e) {
			e.getMessage();
		}
	}

	public static String CentralZone(Connection con){
		int i=0,j=0,PrintZero=0;
		double Quan = 0,Price=0;
		String CentralZone = "",SqlCentral = "SELECT DESCRIPT,PrintZero FROM DBA.Product WHERE PRODNUM = ?";
		String Specialty = "";
		NumberFormat Formatter = NumberFormat.getInstance(Locale.ENGLISH);
		StringBuilder StrPrice = null,StrQuan = null,TempChain = new StringBuilder(),FinalChain = new StringBuilder();
		PreparedStatement Central = null;
		ResultSet rsCentral = null;
		
		for (i=0 ; i < listPosdetail.size() ; i++) {
			if (listPosdetail.get(i).getIdproductoext() != 0 & listPosdetail.get(i).getIdproductoext() != 2002) {
				try {
					Central = con.prepareStatement(SqlCentral);
					Central.setInt(1, listPosdetail.get(i).getIdproductoext());
					rsCentral = Central.executeQuery();
					rsCentral.next();
					Specialty = rsCentral.getString(1);
					PrintZero = rsCentral.getInt(2);
					
					if (PrintZero == 1 || listPosdetail.get(i).getOrigcostech() > 0) {
						Price = Math.round(listPosdetail.get(i).getOrigcostech()*listPosdetail.get(i).getCantidad());
						Quan = listPosdetail.get(i).getCantidad();
						
						Formatter.setMinimumFractionDigits(0);
						StrPrice = new StringBuilder(Formatter.format(Price));
						StrPrice.insert(0, "$ ");
						
						if ((Quan - Math.floor(Quan)) == 0) {
							Formatter.setMinimumFractionDigits(0);
						}else {
							Formatter.setMinimumFractionDigits(2);
						}
						
						StrQuan = new StringBuilder(Formatter.format(Quan));
						
						for (j = StrQuan.length(); j < 5; j++) {
							StrQuan = StrQuan.insert(0, " ");
						}
						
						if (Specialty.length() > 21) {
							Specialty = Specialty.substring(0, 21); 
						}
						
						TempChain = TempChain.append(StrQuan+" "+Specialty);
						
						for (j = TempChain.length(); j < (40 - StrPrice.length()); j++) {
							TempChain.insert(j, " ");
						}
						
						FinalChain.append(TempChain);
						FinalChain.append(StrPrice + "\r\n");
						TempChain = new StringBuilder();
					}
					Central.close();
					rsCentral.close();
				} catch (SQLException e) {
					e.getMessage();
				}
			}
		}
		
		CentralZone = CentralZone + FinalChain.toString();
		CentralZone = CentralZone + "^L\r\n";
		
		return CentralZone;
	}
	
	public void main(ArrayList<capaModeloCC.DetallePedidoPixel> pruebaPedido, String dsnODBC, int MemCode, Cliente MiembroEnviado, boolean indicadorAct) {
				
		 try
		 {
			 //ESTO LO HEMOS UTILIZADO PARA LAS PRUEBAS UNITARIOS
			 
			 //Class.forName("sybase.jdbc.sqlanywhere.IDriver");
			 //Connection con = DriverManager.getConnection("jdbc:sqlanywhere:dsn=PixelPC;uid=admin;pwd=xxx");//local
			 
			 // ESTO LO ESTABAMOS USANDO EN LAS PRUEBAS INTEGRALES
			 //DriverManager.registerDriver( (Driver)
			 //		 Class.forName( "sybase.jdbc.sqlanywhere.IDriver" ).newInstance() );
			 //Connection con = DriverManager.getConnection("jdbc:sqlanywhere:dsn=PixelSqlbase;uid=admin;pwd=xxx");//SystemPos
			 
			 
			 // ESTA ES LA FORMA PAR�METRICA
			 ConexionBaseDatos conexion = new ConexionBaseDatos();
			 Connection con = conexion.obtenerConexionBDTienda(dsnODBC);
			 
			 //Obteniendo el dia de apertura
			 Statement state = con.createStatement();
			 String consulta = "select DBA.PixOpenDate() as Opendate";
			 ResultSet rs = state.executeQuery(consulta);
			 rs.next();
			 java.sql.Date DiaApertura = rs.getDate("OpenDate");
			 rs.close();
			 System.out.println("Día De Apertura: "+DiaApertura);
			 
			 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
			 Date parsedDate = dateFormat.parse("1899-12-30 00:00:00.000");
			 Timestamp Inicialts = new java.sql.Timestamp(parsedDate.getTime());
			 System.out.println("Fecha Inicial Sistema: "+Inicialts);
			 
			 Timestamp ts = new Timestamp(System.currentTimeMillis());
			 System.out.println("Timestamp: "+ts);

			 
			 //Obteniendo el numero de factura
			 CallableStatement proc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
			 proc.registerOutParameter(1, Types.INTEGER);
			 proc.setString(2, "GETNEXT_POSHEADER");
			 proc.execute();
			 int NumFactura = proc.getInt(1);
			 proc.close();
			 System.out.println("NumFactura: "+NumFactura);
			 
				//MemCode: Codigo del miembro o cliente
//				Cliente(int idcliente, String telefono, String nombres, String apellidos, String nombreCompania,
//						String direccion, String municipio, int idMunicipio, float latitud, float lontitud, String zonaDireccion,
//						String observacion, String tienda, int idtienda) 
				
				PreparedStatement Member = null;
				String SqlInsertMember = "INSERT INTO DBA.Member(MEMCODE,FIRSTNAME,LASTNAME,ADRESS1,ADRESS2,CITY,PROV,STARTDATE,"
						+"EXPDATE,ANNIVER,HOMETELE,LASTVISIT,MEMDIS,CARDNUM,GROUPNUM,ISACTIVE,Country,"
						+"UpdateStatus,Directions,CompanyName,Type,HasBioReg,NoSolicit)"
						+"VALUES (?,?,?,?,?,?,'Antioquia',?,?,?,?,?,0,?,0,1,'',1,?,?,0,0,0)";
				String SqlUpdateMember = "UPDATE DBA.Member SET FIRSTNAME=?,LASTNAME=?,ADRESS1=?,ADRESS2=?,CITY=?,"
						+ "Directions=?,CompanyName=? WHERE MEMCODE=?";
				
				
				
				//Si existe Memcode>0 y Bool = False **no hacer nada
				//Si existe Memcode>0 y Bool = True **Actualizar
				//No existe Memcode = 0 y ignora Bool **Crea
				
				if (MemCode == 0) {
					parsedDate = dateFormat.parse("2040-12-31 00:00:00.000");
					Timestamp LastDayTS = new java.sql.Timestamp(parsedDate.getTime());
									
					proc = con.prepareCall("{call DBA.GetNextAutoInc(?,?)}");
					proc.registerOutParameter(1, Types.INTEGER);
					proc.setString(2, "GETNEXT_MEMBER");
					proc.execute();
					MemCode = proc.getInt(1);
					proc.close();
					
					Member = con.prepareStatement(SqlInsertMember);
					Member.setInt(1, MemCode);
					Member.setString(2, MiembroEnviado.getNombres());
					Member.setString(3, MiembroEnviado.getApellidos());
					Member.setString(4, MiembroEnviado.getDireccion());
					Member.setString(5, MiembroEnviado.getZonaDireccion());//revisar
					Member.setString(6, MiembroEnviado.getMunicipio());
					Member.setTimestamp(7, ts);
					Member.setTimestamp(8, LastDayTS);
					Member.setTimestamp(9, ts);
					Member.setString(10, MiembroEnviado.getTelefono());
					Member.setTimestamp(11, ts);
					Member.setString(12, Integer.toString(MemCode));
					Member.setString(13, MiembroEnviado.getObservacion());
					Member.setString(14, MiembroEnviado.getNombreCompania());
					
					Member.execute();
					Member.close();
					
					MiembroEnviado.setMemcode(MemCode);
					
				}else if (indicadorAct == true) {
					Member = con.prepareStatement(SqlUpdateMember);
					Member.setString(1, MiembroEnviado.getNombres());
					Member.setString(2, MiembroEnviado.getApellidos());
					Member.setString(3, MiembroEnviado.getDireccion());
					Member.setString(4, MiembroEnviado.getZonaDireccion());//revisar
					Member.setString(5, MiembroEnviado.getMunicipio());
					Member.setString(6, MiembroEnviado.getObservacion());
					Member.setString(7, MiembroEnviado.getNombreCompania());
					Member.setInt(8, MemCode);
					
					Member.execute();
					Member.close();
				}
			 
			 
			 
			 //MethodNum: Efectivo o Tarjeta
			 int MethodNum = 1001;
			 
			 //Cambio
			 double Change = 100000;
			 
			 
			 
			 //Llenado de ArrayList Pedido de prueba
			 /*
			 ArrayList<DetallePedidoPixel> pruebaPedido = new ArrayList<DetallePedidoPixel>();
			 pruebaPedido.add(new DetallePedidoPixel(2341,1));
			 pruebaPedido.add(new DetallePedidoPixel(2002,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2024,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2229,1));
			 pruebaPedido.add(new DetallePedidoPixel(2273,1));
			 pruebaPedido.add(new DetallePedidoPixel(2130,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2126,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2002,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2022,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2401,1));
			 pruebaPedido.add(new DetallePedidoPixel(2249,1));
			 pruebaPedido.add(new DetallePedidoPixel(2177,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2002,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2110,1));
			 pruebaPedido.add(new DetallePedidoPixel(2421,1));
			 pruebaPedido.add(new DetallePedidoPixel(0,0));
			 pruebaPedido.add(new DetallePedidoPixel(2372,1));
			 pruebaPedido.add(new DetallePedidoPixel(2374,2));
			 pruebaPedido.add(new DetallePedidoPixel(0,0));
			 pruebaPedido.add(new DetallePedidoPixel(2107,1));
			 pruebaPedido.add(new DetallePedidoPixel(0,0));
			 pruebaPedido.add(new DetallePedidoPixel(2389,1));
			 pruebaPedido.add(new DetallePedidoPixel(2430,1));
			 pruebaPedido.add(new DetallePedidoPixel(2116,1));
			 pruebaPedido.add(new DetallePedidoPixel(0,0));
			 pruebaPedido.add(new DetallePedidoPixel(2003,1));
			 pruebaPedido.add(new DetallePedidoPixel(2002,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2067,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2002,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2061,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2002,0.5));
			 pruebaPedido.add(new DetallePedidoPixel(2116,1));
			 pruebaPedido.add(new DetallePedidoPixel(0,0)); 
			 */
			 //Preparando ArraList PosDetail
			 PosDetail(con,pruebaPedido,2007,NumFactura,DiaApertura);
			 
			 //Insertando nueva transacción en dba.PosHeader y en DBA.Howpaid
			 PosHeader(con,2007,NumFactura,MemCode,MethodNum,Change,Inicialts,DiaApertura);
			 
			 //Insertando nueva transacción en dba.Tabinfo
			 ts = new Timestamp(System.currentTimeMillis());
			 System.out.println("Timestamp #2: "+ts);
			 
			 String SqlTabInfo = "INSERT INTO DBA.TABINFO(TRANSACT,EMPNUM,TIMESTART,TABLENUM,InUse,Course)"+
					 "VALUES (?,?,?,?,?,?)";
			 
			 PreparedStatement TabInfo = con.prepareStatement(SqlTabInfo);
			 TabInfo.setInt(1, NumFactura);
			 TabInfo.setInt(2, 2007);
			 TabInfo.setTimestamp(3, ts);
			 TabInfo.setInt(4, 30001);
			 TabInfo.setInt(5, 1);
			 TabInfo.setInt(6, 0);
			 
			 TabInfo.execute();
			 TabInfo.close();
			 
			 //Insertando nueva transacción en dba.PosHDelivery
			 ts = new Timestamp(System.currentTimeMillis());
			 System.out.println("Timestamp #3: "+ts);
			 
			 String SqlPosHDelivery = "INSERT INTO DBA.PosHDelivery(Transact,MemCode,OpenDate,DeliveryStatus,UpdateStatus,SNum)"
					 +"VALUES (?,?,?,?,?,?)";
			 
			 PreparedStatement PosHDelivery = con.prepareStatement(SqlPosHDelivery);
			 PosHDelivery.setInt(1, NumFactura);
			 PosHDelivery.setInt(2, MemCode);
			 PosHDelivery.setDate(3, DiaApertura);
			 PosHDelivery.setInt(4, 0);
			 PosHDelivery.setInt(5, 1);
			 PosHDelivery.setInt(6, -1);
			 
			 PosHDelivery.execute();
			 PosHDelivery.close();
			 
			 //Insertando nueva transacción en DBA.POSDETAIL
			 RunPosDetail(con);
			 
			 //Borrando registro en dba.Tabinfo
			 PreparedStatement BorrarTabInfo = con.prepareStatement("DELETE FROM DBA.TABINFO WHERE TRANSACT = ?");
			 BorrarTabInfo.setInt(1, NumFactura);
			 
			 BorrarTabInfo.execute();
			 BorrarTabInfo.close();
			 
			 //Actualizando registro en dba.Member 
			 ts = new Timestamp(System.currentTimeMillis());
			 System.out.println("Timestamp #5: "+ts);
			 String NumDeliverys = "SELECT a.LastTrans,a.LastTrans2,(SELECT COUNT(*) FROM dba.PosHDelivery h WHERE a.memcode = h.memcode) "
					 + "FROM dba.Member a WHERE a.MEMCODE = ?";
			 
			 PreparedStatement RegisInMember = con.prepareStatement(NumDeliverys);
			 RegisInMember.setInt(1, MemCode);
			 rs = RegisInMember.executeQuery();
			 rs.next();
			 int LastTrans2 = rs.getInt(1);
			 int LastTrans3 = rs.getInt(2);
			 int NumberDeliverys = rs.getInt(3);
			 RegisInMember.close();
			 rs.close();
			 
			 RegisInMember = con.prepareStatement("UPDATE dba.Member SET LastTrans=?,LastTrans2=?,LastTrans3=?,NumDeliverys=?,LastOrderDate=? WHERE MemCode =?");
			 RegisInMember.setInt(1, NumFactura);
			 RegisInMember.setInt(2, LastTrans2);
			 RegisInMember.setInt(3, LastTrans3);
			 RegisInMember.setInt(4, NumberDeliverys);
			 RegisInMember.setTimestamp(5, ts);
			 RegisInMember.setInt(6, MemCode);
			 RegisInMember.executeQuery();
			 RegisInMember.close();
			 
			 //Imprimiendo Facturas
			 Invoice(con,NumFactura,2007,MemCode);
			 		 
			 //Receta y Ajuste de Inventario
			 Recipe(con,pruebaPedido,NumFactura);
			 
			 ts = new Timestamp(System.currentTimeMillis());
			 System.out.println("Timestamp Final: "+ts);
			 System.out.println("=====Listo=====");
			 
			 listPosdetail.clear();
			 con.close();
			 
		 }catch(Exception e){
			 System.out.println("Tuvimos un fallo " + e.getMessage());
		 }
	}

}

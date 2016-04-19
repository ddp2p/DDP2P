package net.ddp2p.widgets.private_org;
import static net.ddp2p.common.util.Util.__;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_OrgDistribution;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.table.org_distribution;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.TableUpdater;
@SuppressWarnings("serial")
public class PrivateOrgModel extends AbstractTableModel implements TableModel, DBListener{
	private static final boolean _DEBUG = true;
	private static final int TABLE_COL_PEER_NAME = 0;
	private static final int TABLE_COL_PEER_EMAIL = 1;
	public static boolean DEBUG = false;
	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={__("Peers To Share Organization"),__("Emails")};//_("Organization"),_("Peer")};
	ArrayList<D_OrgDistribution> data = new ArrayList<D_OrgDistribution>(); 
	private PrivateOrgPanel panel;
	private long curOrg;
	private Component mytable;
	public PrivateOrgModel(DBInterface _db, PrivateOrgPanel _panel) { 
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.org_distribution.TNAME)), null);
	}
	public void setMyTable(Component _mytable){
		mytable = _mytable;
	}
	@Override
	public int getRowCount() {
		if(data==null) return 0;
		return data.size();
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("PrivateOrgModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return Util.getString(columnNames[col]);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		return String.class;
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_OrgDistribution crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_PEER_NAME: return data.get(rowIndex).peer_name;
		case TABLE_COL_PEER_EMAIL: return data.get(rowIndex).emails;
		}
		return null;
	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("PrivateOrgModel:setValueAt: r="+row +", c="+col+" val="+aValue);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_OrgDistribution crt = data.get(row);
		if(DEBUG) System.out.println("PrivateOrgModel:setValueAt: old crt="+crt);
		fireTableCellUpdated(row, col);
	}
	@Override
	public void addTableModelListener(TableModelListener l) {
	}
	@Override
	public void removeTableModelListener(TableModelListener l) {
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("PrivateOrgModel: update: start: table="+Util.concat(table, " ", "null"));
		if(curOrg<=0){
			data =  new ArrayList<D_OrgDistribution>();
			if(DEBUG) System.out.println("PrivateOrgModel: update: done no org");
			return;
		}
		data = D_OrgDistribution.get_Org_Distribution_byOrgID(curOrg+"");
		new TableUpdater(this, mytable, null);
		if(DEBUG) System.out.println("PrivateOrgModel: update: done");
	}
    private PrivateOrgPanel getPanel() {
		return panel;
	}
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}
	public void setTable(PrivateOrgTable privateOrgTable) {
		tables.add(privateOrgTable);
	}
	public long get_PrivateOrgID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).od_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public long get_PeerID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).peer_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public String get_Emails(int row) {
		if(row<0) return "";
		try{
			return data.get(row).emails;
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	public long get_OrgID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).organization_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public void setOrg(D_Organization org) {
		if(org==null){
			this.curOrg = -1;
		}else
			this.curOrg = org.getLID();
		update(null, null);
	}
}

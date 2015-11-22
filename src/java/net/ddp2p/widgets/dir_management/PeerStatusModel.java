package net.ddp2p.widgets.dir_management;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.ddp2p.common.hds.DirMessage;
import net.ddp2p.common.hds.DirectoryMessageStorage;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public class PeerStatusModel extends AbstractTableModel implements TableModel, DBListener{
	public static final int TABLE_COL_NAME = 0;   
	public static final int TABLE_COL_INSTANCE = 1; 
	public static final int TABLE_COL_ADDRESS = 2;  
	public static final int TABLE_COL_TERMS = 3; 
	public static final int TABLE_COL_STATUS = 4; 
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	PeerStatusTable peerStatusTable;
	String columnNames[]={__("Peer Name"), __("Instance"), __("Peer Address"), __("Terms"), __("Status")};
	DirMessage[] data;
	private DirPanel panel;
	public PeerStatusModel(DBInterface _db, DirPanel _panel) { 
		panel = _panel;
	}
	@Override
	public int getRowCount() {
		if(data==null) return 0;
		return data.length;
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("PeerStatusModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return Util.getString(columnNames[col]);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		return getValueAt(0, col).getClass();
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex<0) || (rowIndex>=this.getRowCount())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		DirMessage crt = data[rowIndex];
		if(crt==null) return null;
		switch(columnIndex){
			case TABLE_COL_NAME:
				return crt.initiatorGIDhash;
			case TABLE_COL_INSTANCE:
				return crt.sourceInstance;
			case TABLE_COL_ADDRESS:
				return crt.sourceIP;
			case TABLE_COL_TERMS:
				String terms="";
				if(crt.requestTerms!=null)
		       		for(int i=0; i<crt.requestTerms.length; i++){
		       			terms+="\n  terms["+i+"]\n"+ crt.requestTerms[i];
		            }
				else terms="nothing";
				return terms;
			case TABLE_COL_STATUS:
				return crt.status;
		}
		return null;
	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
	}
	@Override
	public void addTableModelListener(TableModelListener l) {
	}
	@Override
	public void removeTableModelListener(TableModelListener l) {
	}
	public String getGID_by_row(int row){
		if(data==null ){
			System.out.println("getGID_by_row():Error in data[] array");
			return null;
		}
		return data[row].sourceGID;
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(DEBUG ) System.out.println("PeerStatusModel: update: start:");
		if( DirectoryMessageStorage.latestRequest_storage !=null ){
			Object[] arr = DirectoryMessageStorage.latestRequest_storage.values().toArray();
			data = new DirMessage[arr.length];
			for(int i=0; i<arr.length; i++)
				data[i]=(DirMessage) arr[i];
		}
		this.fireTableDataChanged();
		DEBUG=false;
	}
	public void setTable(PeerStatusTable peerStatusTable) {
		this.peerStatusTable = peerStatusTable;
	}
}

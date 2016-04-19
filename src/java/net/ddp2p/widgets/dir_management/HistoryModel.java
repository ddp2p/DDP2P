package net.ddp2p.widgets.dir_management;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Date;
import java.util.Calendar;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JComboBox;
import javax.swing.DefaultCellEditor;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_DirectoryServerSubscriberInfo;
import net.ddp2p.common.hds.DirMessage;
import net.ddp2p.common.hds.DirectoryMessageStorage;
import net.ddp2p.common.table.subscriber;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public class HistoryModel extends AbstractTableModel implements TableModel, DBListener{
	public static final int TABLE_COL_USED_IP = 0; 
	public static final int TABLE_COL_MSG_TYPE = 1;
    public static final int TABLE_COL_REQUESTED_PEER = 2;
    public static final int TABLE_COL_TIMESTAMP = 3;
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	HistoryTable historyTable;
	String columnNames[]={__("Peer Used IP"),__("Message Type"),__("Requested Peer"),__("Timestamp")};
	ArrayList<DirMessage> data = new ArrayList<DirMessage>();
	String GID_history; 
	private DirPanel panel;
	public HistoryModel(DBInterface _db, DirPanel _panel) { 
		panel = _panel;
	}
	@Override
	public int getRowCount() {
		return data.size();
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("HistoryModel:getColumnName: col Header["+col+"]="+columnNames[col]);
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
		DirMessage crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
			case TABLE_COL_USED_IP:
				return crt.sourceIP;
			case TABLE_COL_MSG_TYPE:
				return crt.MsgType;
			case TABLE_COL_REQUESTED_PEER:
				return crt.requestedPeerGIDhash;
			case TABLE_COL_TIMESTAMP:
				return crt.timestamp;
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
	public void update(String GID_history) {
		this.GID_history = GID_history;
		update(null, null);
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(GID_history==null) return;
		if(DEBUG ) System.out.println("PeerStatusModel: update: start:");
		ArrayList<DirMessage> announcementList=null;
		ArrayList<DirMessage> pingList = null;
		ArrayList<DirMessage> nopingList = null;
		if (DirectoryMessageStorage.announcement_storage != null) {
			announcementList = DirectoryMessageStorage.announcement_storage.get(GID_history);
		} else {
			if (_DEBUG) System.out.println("HistoryModel:update: Why am I here");
		}
		if (DirectoryMessageStorage.ping_storage != null)
			pingList = DirectoryMessageStorage.ping_storage.get(GID_history);
		if (DirectoryMessageStorage.noping_storage != null)
			nopingList = DirectoryMessageStorage.noping_storage.get(GID_history);	
		if (announcementList != null) {
			data.clear();
			for (int i = 0; i < announcementList.size(); i++)
				data.add(announcementList.get(i));
		}
		if(pingList!=null) {
			for(int i=0; i<pingList.size(); i++)
				data.add(pingList.get(i));
		}
		if(nopingList != null) {
			for(int i=0; i<nopingList.size(); i++)
				data.add(nopingList.get(i));
		}
		this.fireTableDataChanged();
		DEBUG=false;
	}
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}
	public void setTable(HistoryTable historyTable) {
		this.historyTable = historyTable;
	}
	public DirMessage getObject(int model_row) {
		return data.get(model_row);
	}
}

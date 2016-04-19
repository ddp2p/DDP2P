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
import net.ddp2p.common.table.peer;
import net.ddp2p.common.table.subscriber;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public class ConnPeerModel extends AbstractTableModel implements TableModel, DBListener{
	public static final int TABLE_COL_IP = 0;
	public static final int TABLE_COL_STATUS = 1;
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	ConnPeerTable connPeerTable;
	String columnNames[]={__("Peer IP"),__("Status")};
	ArrayList<D_DirectoryServerSubscriberInfo> data = new ArrayList<D_DirectoryServerSubscriberInfo>();
	private DirPanel panel;
	public ConnPeerModel(DBInterface _db, DirPanel _panel) { 
		if(_db==null){
			System.err.println("ConnPeerModel:<init>: no directory database");
			panel = _panel;
			return;
		}
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.subscriber.TNAME,net.ddp2p.common.table.peer.TNAME)), null);
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
		if(DEBUG) System.out.println("ConnPeerModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return Util.getString(columnNames[col]);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		return getValueAt(0, col).getClass();
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
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
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
	}
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}
	public void setTable(ConnPeerTable connPeerTable) {
		this.connPeerTable = connPeerTable;
	}
}

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
	public static final int TABLE_COL_NAME = 0;   // Peer name
	public static final int TABLE_COL_INSTANCE = 1; // peer's instance
	public static final int TABLE_COL_ADDRESS = 2;  // all registered addresses for the peer
	public static final int TABLE_COL_TERMS = 3; // last negotiated tersms between a peer and the directory 
	public static final int TABLE_COL_STATUS = 4; // accepted or rejected service
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	
	public DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	PeerStatusTable peerStatusTable;
	String columnNames[]={__("Peer Name"), __("Instance"), __("Peer Address"), __("Terms"), __("Status")};
	DirMessage[] data;
	private DirPanel panel;
	
	public PeerStatusModel(DBInterface _db, DirPanel _panel) { // constructor with dataSource -> DBInterface _db
//		if(_db==null){
//			System.err.println("PeerStatusModel:<init>: no directory database");
//			panel = _panel;
//			return;
//		}
//		db = _db;
		panel = _panel;
//		db.addListener(this, new ArrayList<String>(Arrays.asList(table.Subscriber.TNAME,table.peer.TNAME)), null);
//		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
//	//	update(null, null);
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
	//	if(col == TABLE_COL_IP || col==TABLE_COL_STATUS) return getValueAt(0, col).getClass();// ComboBoxRenderer.class; // dropdown list
	//	if(col == TABLE_COL_EXPIRATION) return String.class;
		return getValueAt(0, col).getClass();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
//		switch(columnIndex){
//		case TABLE_COL_PRIORITY:
			return false;
//		}
//		return true;
	}

	//@SuppressWarnings({"rawtypes", "unchecked" })
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
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
				return terms;//crt.requestTerms.toString();//crt.get(registered.);
			case TABLE_COL_STATUS:
				return crt.status;//crt.get(registered.global_peer_ID_hash);
		}
		return null;
	}
//		@Override
//	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
//		if((rowIndex<0) || (rowIndex>=data.size())) return null;
//		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
//		ArrayList<Object> crt = data.get(rowIndex);
//		if(crt==null) return null;
//		switch(columnIndex){
//			case TABLE_COL_NAME:
//				D_PeerAddress peerAdd = D_PeerAddress.getPeerByGID((String)crt.get(registered.REG_GID), false);
//				return peerAdd.component_basic_data.name;
//			case TABLE_COL_INSTANCE:
//				return crt.get(registered.REG_INSTANCE);
//			case TABLE_COL_ADDRESS:
//				return crt.get(registered.REG_ADDR);
//			case TABLE_COL_TERMS:
//				return "terms";//crt.get(registered.);
//			case TABLE_COL_STATUS:
//				return "status";//crt.get(registered.global_peer_ID_hash);
//		}
//		return null;
//	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
//		if(_DEBUG) System.out.println("PeerStatusModel:setValueAt: r="+row +", c="+col+" val="+aValue);
//		if((row<0) || (row>=data.size())) return;
//		if((col<0) || (col>this.getColumnCount())) return;
//		D_SubscriberInfo crt = data.get(row);
//		if(_DEBUG) System.out.println("PeerStatusModel:setValueAt: old crt="+crt);
//		switch(col) {
//		case TABLE_COL_TOPIC:
//			crt.topic = ((Boolean) aValue).booleanValue();
//			try {
//				crt.storeNoSync("update");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			break;
//		case TABLE_COL_AD:
//			crt.ad = ((Boolean) aValue).booleanValue();
//			try {
//				crt.storeNoSync("update");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			break;
//		case TABLE_COL_PLAINTEXT:
//			crt.plaintext = ((Boolean) aValue).booleanValue();
//			try {
//				crt.storeNoSync("update");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			break;
//		case TABLE_COL_EXPIRATION:
//			crt.expiration = Calendar.getInstance();//
//			try{
//				crt.expiration.setTime(new Date(Util.getString(aValue)));
//			}
//			catch(IllegalArgumentException e){	   
//			   crt.expiration =null;
//			   if(Util.getString(aValue)!=null && !Util.getString(aValue).trim().equals(""))
//			   		Application.warning(Util.getString(aValue)+ " is not a correct date format" , "Date format");	
//			}
//			
//			// crt.expiration.s Util.getCalendar(Util.getString(aValue));
////			System.out.println("crt.expiration: "+crt.expiration);
////			System.out.println("Util.getString(aValue): "+Util.getString(aValue));
//			try {
//				crt.storeNoSync("update");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			break;
//		case TABLE_COL_NAME:
//			if (aValue instanceof CBoxItem){
//				crt.GID = ((CBoxItem)aValue).GID;
//				crt.name = Util.getString( aValue);
//				try {
//					crt.storeNoSync("update");
//				} catch (P2PDDSQLException e) {
//					e.printStackTrace();
//				}
//			}
//			break;
//		case TABLE_COL_MODE:
//			crt.mode = Util.getString( aValue);
//			try {
//				crt.storeNoSync("update");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			break;
//		case TABLE_COL_PAYMENT:
//			if(_DEBUG) System.out.println("PeerStatusModel:setValueAt: PAYMENT");
//			crt.payment = ((Boolean) aValue).booleanValue();
//			try {
//				crt.storeNoSync("update");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			break;
//		}
//		fireTableCellUpdated(row, col);
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

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
		//DEBUG=true;
		if(DEBUG ) System.out.println("PeerStatusModel: update: start:"/*+table*/);
		if( DirectoryMessageStorage.latestRequest_storage !=null ){
			Object[] arr = DirectoryMessageStorage.latestRequest_storage.values().toArray();
			data = new DirMessage[arr.length];
			for(int i=0; i<arr.length; i++)
				data[i]=(DirMessage) arr[i];
		}
		this.fireTableDataChanged();
		DEBUG=false;
	}
	
//		@Override
//	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
//		//DEBUG=true;
//		if(DEBUG ) System.out.println("PeerStatusModel: update: start:"/*+table*/);
//		//dirTable.init();
//		//getComboBox(null);
//		//dirTable.repaint();
//	   // namesCBox.removeAllItems() ;
//		String sql =
//				"SELECT "+
//						registered.fields+
//				" FROM "+registered.TNAME+
//				" GROUP BY "+registered.global_peer_ID +", " +registered.instance 	+";";
//				
//		String[]params = new String[]{};// where clause?
//	//	ArrayList<ArrayList<Object>> u;
//		try {
//			data = db.select(sql, params, DEBUG);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//			return;
//		}
////		data = new ArrayList<D_SubscriberInfo>();
////		for(ArrayList<Object> _u :u){
////			D_SubscriberInfo ui = new D_SubscriberInfo(_u, db);
////			if(DEBUG) System.out.println("PeerStatusModel: update: add: "+ui);
////			data.add(ui); // add a new item to data list (rows)
////		}
////		if(dirTable!=null)
////			dirTable.getColumnModel().getColumn(TABLE_COL_NAME).setCellEditor(new DefaultCellEditor(getComboBox(null)));
//		this.fireTableDataChanged();
//		DEBUG=false;
//	}
	
//	public void refresh() {
//		data.removeAll(data);
//		update(null, null);
//	}

	public void setTable(PeerStatusTable peerStatusTable) {
		//tables.add(dirTable);
		this.peerStatusTable = peerStatusTable;
	}
/*	
	@Override
	public void actionPerformed(ActionEvent e) {
		TableJButton bb =(TableJButton)e.getSource();
		QualitesTable q = new QualitesTable(data.get(bb.rowNo));
		JPanel p = new JPanel(new BorderLayout());
		p.add(q.getScrollPane());
		final JFrame frame = new JFrame();
		frame.setContentPane(p);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.pack();
		frame.setSize(600,300);
		frame.setVisible(true);
        JButton okBt = new JButton("   OK   ");
        okBt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            frame.hide();
           }
        });

		p.add(okBt,BorderLayout.SOUTH);
//		JOptionPane.showMessageDialog(null,p);
  //      JOptionPane.showMessageDialog(null,p,"Test Qualities Info", JOptionPane.DEFAULT_OPTION, null);
        

	}
*/
/*	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
			JPanel test = new JPanel();
			//frame.add(test);
			test.setLayout(new BorderLayout());
			UpdatesTable t = new UpdatesTable(Application.db);
			//t.getColumnModel().getColumn(TABLE_COL_QOT_ROT).setCellRenderer(new ComboBoxRenderer());
			test.add(t);
			//PeersTest newContentPane = new PeersTest(db);
			//newContentPane.setOpaque(true);
			//frame.setContentPane(t.getScrollPane());
			test.add(t.getTableHeader(),BorderLayout.NORTH);
			frame.setContentPane(test);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
*/

}

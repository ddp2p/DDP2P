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
	
	public ConnPeerModel(DBInterface _db, DirPanel _panel) { // constructor with dataSource -> DBInterface _db
		if(_db==null){
			System.err.println("ConnPeerModel:<init>: no directory database");
			panel = _panel;
			return;
		}
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.subscriber.TNAME,net.ddp2p.common.table.peer.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
	//	update(null, null);
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
	//	if(col == TABLE_COL_IP || col==TABLE_COL_STATUS) return getValueAt(0, col).getClass();// ComboBoxRenderer.class; // dropdown list
	//	if(col == TABLE_COL_EXPIRATION) return String.class;
		return getValueAt(0, col).getClass();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
//		switch(columnIndex){
//		case TABLE_COL_PRIORITY:
//			return false;
//		}
		return true;
	}

	//@SuppressWarnings({"rawtypes", "unchecked" })
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
//		if((rowIndex<0) || (rowIndex>=data.size())) return null;
//		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
//		D_SubscriberInfo crt = data.get(rowIndex);
//		if(crt==null) return null;
//		switch(columnIndex){
//		case TABLE_COL_TOPIC:
//			return data.get(rowIndex).topic;
//		case TABLE_COL_AD:
//			return data.get(rowIndex).ad;
//		case TABLE_COL_PLAINTEXT:
//			return data.get(rowIndex).plaintext;
//		case TABLE_COL_PAYMENT:
//			return data.get(rowIndex).payment;
//		case TABLE_COL_EXPIRATION:
//			 if(data.get(rowIndex).expiration != null)
//				return data.get(rowIndex).expiration.getTime().toGMTString();
//			 return null;
//		case TABLE_COL_NAME:
//			    //getComboBox(null);
//				return	data.get(rowIndex).name;	 // list of all peers names excluding the selected ones.
//		case TABLE_COL_MODE:
//			return  data.get(rowIndex).mode; // list of all peers names excluding the selected ones.
//		}
		return null;
	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
//		if(_DEBUG) System.out.println("ConnPeerModel:setValueAt: r="+row +", c="+col+" val="+aValue);
//		if((row<0) || (row>=data.size())) return;
//		if((col<0) || (col>this.getColumnCount())) return;
//		D_SubscriberInfo crt = data.get(row);
//		if(_DEBUG) System.out.println("ConnPeerModel:setValueAt: old crt="+crt);
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
//			if(_DEBUG) System.out.println("ConnPeerModel:setValueAt: PAYMENT");
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

	
	
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
//		//DEBUG=true;
//		if(DEBUG ) System.out.println("ConnPeerModel: update: start:"/*+table*/);
//		//dirTable.init();
//		getComboBox(null);
//		dirTable.repaint();
//	   // namesCBox.removeAllItems() ;
//		String sql =
//				"SELECT "+
//						Subscriber.fields_subscribers+
//				" FROM "+Subscriber.TNAME+";";
//		String[]params = new String[]{};// where clause?
//		ArrayList<ArrayList<Object>> u;
//		try {
//			u = db.select(sql, params, DEBUG);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//			return;
//		}
//		data = new ArrayList<D_SubscriberInfo>();
//		for(ArrayList<Object> _u :u){
//			D_SubscriberInfo ui = new D_SubscriberInfo(_u, db);
//			if(DEBUG) System.out.println("ConnPeerModel: update: add: "+ui);
//			data.add(ui); // add a new item to data list (rows)
//		}
////		if(dirTable!=null)
////			dirTable.getColumnModel().getColumn(TABLE_COL_NAME).setCellEditor(new DefaultCellEditor(getComboBox(null)));
//		this.fireTableDataChanged();
//		DEBUG=false;
	}
	
	
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}

	public void setTable(ConnPeerTable connPeerTable) {
		//tables.add(dirTable);
		this.connPeerTable = connPeerTable;
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

package widgets.private_org;

import static util.Util._;

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

import util.P2PDDSQLException;

import config.Application;
import data.D_OrgDistribution;
import data.D_Organization;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.components.TableUpdater;
import table.org_distribution;

@SuppressWarnings("serial")
public class PrivateOrgModel extends AbstractTableModel implements TableModel, DBListener{
	private static final boolean _DEBUG = true;
	private static final int TABLE_COL_PEER_NAME = 0;
	private static final int TABLE_COL_PEER_EMAIL = 1;
	public static boolean DEBUG = false;
	


	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={_("Peers To Share Organization"),_("Emails")};//_("Organization"),_("Peer")};

	//ArrayList<D_PrivateOrgInfo> data = new ArrayList<D_PrivateOrgInfo>(); // rows of type D_PrivateOrgInfo -> Bean
	ArrayList<D_OrgDistribution> data = new ArrayList<D_OrgDistribution>(); // rows of type D_PrivateOrgInfo -> Bean
	private PrivateOrgPanel panel;
	private long curOrg;
	private Component mytable;
	
	public PrivateOrgModel(DBInterface _db, PrivateOrgPanel _panel) { // constructor with dataSource -> DBInterface _db
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.org_distribution.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
	//	update(null, null);
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
//		if(col == TABLE_COL_SERVICE || col == TABLE_COL_PRIORITY_TYPE) return getValueAt(0, col).getClass();
//		if(col == TABLE_COL_PRIORITY) return String.class;
//		return Long.class;// Boolean.class;
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
//		switch(columnIndex){
//		case TABLE_COL_PRIORITY:
//			return false;
//		case TABLE_COL_PAYMENT:
//			return false;
//		}
		return false;
	}

	//@SuppressWarnings({"rawtypes", "unchecked" })
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_OrgDistribution crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		//case TABLE_COL_ID: return data.get(rowIndex).peer_distribution_ID;
		//case TABLE_COL_ORGANIZATION_ID: return data.get(rowIndex).organization_ID;
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
		/*
		switch(col) {
		case TABLE_COL_PEER_ID:
			crt.peer_ID = ((Long) aValue).longValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		}
		*/
		fireTableCellUpdated(row, col);
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
		if(DEBUG) System.out.println("PrivateOrgModel: update: start: table="+Util.concat(table, " ", "null"));
		if(curOrg<=0){
			data =  new ArrayList<D_OrgDistribution>();
			if(DEBUG) System.out.println("PrivateOrgModel: update: done no org");
			return;
		}
		data = D_OrgDistribution.get_Org_Distribution_byOrgID(curOrg+"");
		new TableUpdater(this, mytable);
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
			this.curOrg = org._organization_ID;
		update(null, null);
	}
}
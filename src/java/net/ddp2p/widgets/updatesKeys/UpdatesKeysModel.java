package net.ddp2p.widgets.updatesKeys;

import static net.ddp2p.common.util.Util.__;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Tester;
import net.ddp2p.common.table.tester;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.updates.PanelRenderer;
import net.ddp2p.widgets.updates.TableJButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class UpdatesKeysModel extends AbstractTableModel implements TableModel, DBListener {
	public static final int TABLE_COL_NAME = 0; // orginal or preferred name
	public static final int TABLE_COL_WEIGHT = 1; // selecting wight value
	public static final int TABLE_COL_REFERENCE = 2; // required tester
	public static final int TABLE_COL_TRUSTED_TESTER = 3; // mirror url 
	public static final int TABLE_COL_TRUSTED_MIRROR = 4; // set true if it is trusted as mirror
	private static final boolean _DEBUG = true;

	public static boolean DEBUG = false;

	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={__("Name/ID"),__("Weight"),__("Reference"),__("Trusted as tester"),__("Trusted as mirror")};

	
	public ArrayList<D_Tester> data = new ArrayList<D_Tester>(); // rows of type D_UpdateInfo -> Bean
   
	public UpdatesKeysModel(DBInterface _db) { // constructor get dataSource -> DBInterface _db
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.tester.TNAME, net.ddp2p.common.table.tester.TNAME, net.ddp2p.common.table.mirror.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
		update(null, null);
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
		if(DEBUG) System.out.println("UpdatesKeysModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		//if(col == this.TABLE_COL_WEIGHT) return Float.class;
		if(col == this.TABLE_COL_TRUSTED_MIRROR) return Boolean.class;
		if(col == this.TABLE_COL_TRUSTED_TESTER) return Boolean.class;
		if(col == this.TABLE_COL_REFERENCE) return Boolean.class;
		if(col == this.TABLE_COL_NAME) return String.class;//TesterNameCellPanel.class;//PanelRenderer.class;
		
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_NAME:
		case TABLE_COL_WEIGHT:
		case TABLE_COL_REFERENCE:
		case TABLE_COL_TRUSTED_TESTER:
			try {
				return !DD.getAppBoolean(DD.AUTOMATIC_TESTERS_RATING_BY_SYSTEM);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
    final String prefixID="ID:";
    
    
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_Tester crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_NAME:
			String result = null;
			result = data.get(rowIndex).my_name;
			if(result == null) result = data.get(rowIndex).name;
			if(result == null) result = prefixID+data.get(rowIndex).testerGIDH;
//			JTextField nameTxt = new JTextField(result);
//			nameTxt.setPreferredSize(new Dimension(40,30));
//			TableJButton b = new TableJButton("...",rowIndex);
//			b.addActionListener(this); 
//	    	b.setPreferredSize(new Dimension(20,30));
//			testerNameCellPanel = new TesterNameCellPanel(nameTxt, b);
			return result; //testerNameCellPanel;
		case TABLE_COL_REFERENCE:
			return data.get(rowIndex).referenceTester;
		case TABLE_COL_WEIGHT:
			return data.get(rowIndex).trustWeight;
		case TABLE_COL_TRUSTED_MIRROR:
			return data.get(rowIndex).trustedAsMirror;
		case TABLE_COL_TRUSTED_TESTER:
			return data.get(rowIndex).trustedAsTester; 
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("setVlaueAt"+row +", "+col);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_Tester crt = data.get(row);
		switch(col) {
		case TABLE_COL_NAME:
			System.out.println("setValueAt:txtField"+ aValue);
			crt.my_name = Util.getString(aValue);
			System.out.println("crt.my_name:"+ aValue+":");
			if(crt.my_name!=null) crt.my_name = crt.my_name.trim();
			if(crt.my_name!=null && ("".equals(crt.my_name) || crt.my_name.startsWith(prefixID))) crt.my_name = null;
			try {
				data.get(row).store();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_WEIGHT: // use float value for validation
			crt.trustWeight = Util.getString(Float.valueOf(Util.getString(aValue)).floatValue());
			try {
				data.get(row).store();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_REFERENCE:
			data.get(row).referenceTester = ((Boolean)aValue).booleanValue(); 
			try {
				data.get(row).store();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_TRUSTED_MIRROR:
			data.get(row).trustedAsMirror = ((Boolean)aValue).booleanValue(); 
			try {
				data.get(row).store();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_TRUSTED_TESTER:
			if(data.get(row).trustWeight==null && ((Boolean)aValue).booleanValue()){
				Application_GUI.warning(Util.__("Note: the tester you trusted has no weight, It will be assigned 0 value, you can change it later!"), Util.__("Tester has no weight"));
				data.get(row).trustWeight = ""+0;
			}
			data.get(row).trustedAsTester = ((Boolean)aValue).booleanValue(); 
			try {
				data.get(row).store();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		}
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
		if(DEBUG) System.out.println("UpdatesKeysModel:update: start: "+table);
		String sql = "SELECT "+tester.fields_tester+" FROM "+tester.TNAME+";";
		String[]params = new String[]{};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		data = new ArrayList<D_Tester>();
		for(ArrayList<Object> _u :u){
			D_Tester ui = new D_Tester(_u);
			if(DEBUG)System.out.println("UpdatesKeysModel:update: adding : id= "+ui.tester_ID);
			data.add(ui); // add a new item to data list (rows)
		}
		this.fireTableDataChanged();
	}
	 public void refresh() {
		data.removeAll(data);
		update(null, null);
	}

	public void setTable(UpdatesKeysTable l) {
		tables.add(l);
	}
	
//		@Override
//	public void actionPerformed(ActionEvent e) {
//		TableJButton bb =(TableJButton)e.getSource();
//		D_Tester uKey =data.get(bb.rowNo);
// 		TesterInfoPanel testerPanel= new TesterInfoPanel(D_Tester.getTesterInfoByGID(uKey.testerGID, false, null, null)); 
//		JPanel p = new JPanel(new BorderLayout());
//		p.setBackground(Color.BLUE);
//        p.setMinimumSize(new Dimension(200,200));
//		p.add(new JButton("hi"));
//		JOptionPane.showMessageDialog(null,testerPanel,"Tester Info", JOptionPane.DEFAULT_OPTION, null);
//	}

	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
			JPanel test = new JPanel();
			//frame.add(test);
			test.setLayout(new BorderLayout());
			UpdatesKeysTable t = new UpdatesKeysTable();
			test.add(t);
			//PeersTest newContentPane = new PeersTest(db);
			//newContentPane.setOpaque(true);
			//frame.setContentPane(t.getScrollPane());
			test.add(t.getTableHeader(),BorderLayout.NORTH);
			frame.setContentPane(test);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long get_UpdatesKeysID(int row) {
		try{
			if(row<0) return -1;
			return data.get(row).tester_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}

	public D_Tester get_UpdatesKeysInfo(int row) {
		try{
			return data.get(row);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}

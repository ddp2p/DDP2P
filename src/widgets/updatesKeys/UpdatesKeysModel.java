package widgets.updatesKeys;

import static util.Util._;

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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import util.P2PDDSQLException;

import config.Application;

import data.D_UpdatesKeysInfo;
import data.D_TesterDefinition;


import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import table.updatesKeys;
import widgets.updates.TableJButton;
import widgets.updates.PanelRenderer;

public class UpdatesKeysModel extends AbstractTableModel implements TableModel, DBListener, ActionListener {
	public static final int TABLE_COL_NAME = 0; // orginal or preferred name
	public static final int TABLE_COL_WEIGHT = 1; // selecting wight value
	public static final int TABLE_COL_REFERENCE = 2; // required tester
	public static final int TABLE_COL_TRUSTED_TESTER = 3; // mirror url 
	public static final int TABLE_COL_TRUSTED_MIRROR = 4; // set true if it is trusted as mirror
	private static final boolean _DEBUG = true;

	public static boolean DEBUG = false;

	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={_("Name"),_("Weight"),_("Reference"),_("Trusted as tester"),_("Trusted as mirror")};

	ArrayList<D_UpdatesKeysInfo> data = new ArrayList<D_UpdatesKeysInfo>(); // rows of type D_UpdateInfo -> Bean
	
	public UpdatesKeysModel(DBInterface _db) { // constructor get dataSource -> DBInterface _db
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.updatesKeys.TNAME, table.tester.TNAME, table.updates.TNAME)), null);
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
		if(col == this.TABLE_COL_WEIGHT) return Float.class;
		if(col == this.TABLE_COL_TRUSTED_MIRROR) return Boolean.class;
		if(col == this.TABLE_COL_TRUSTED_TESTER) return Boolean.class;
		if(col == this.TABLE_COL_REFERENCE) return Boolean.class;
		if(col == this.TABLE_COL_NAME) return JPanel.class;//PanelRenderer.class;
		
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
//		switch(columnIndex){
//		case TABLE_COL_NAME:
//		case TABLE_COL_TRUSTED_MIRROR:
//		case TABLE_COL_WEIGHT:
//		case TABLE_COL_TRUSTED_TESTER:
//			return true;
//		}
		return true;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_UpdatesKeysInfo crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_NAME:
			String result = null;
			result = data.get(rowIndex).my_tester_name;
			if(result == null) result = data.get(rowIndex).original_tester_name;
			JTextField nameTxt = new JTextField(result);
			JPanel p = new JPanel(new BorderLayout());
			p.setPreferredSize(new Dimension(60,40));
		    p.add(nameTxt);
	        TableJButton b = new TableJButton("...",rowIndex );
	        b.addActionListener(this);
	       // System.out.println(b.rowNo); 
	        b.setPreferredSize(new Dimension(20,30));
	        nameTxt.setPreferredSize(new Dimension(40,30));
	        p.add(b, BorderLayout.EAST);
			return p;
		case TABLE_COL_REFERENCE:
			return data.get(rowIndex).reference;
		case TABLE_COL_WEIGHT:
			return data.get(rowIndex).weight;
		case TABLE_COL_TRUSTED_MIRROR:
			return data.get(rowIndex).trusted_as_mirror;
		case TABLE_COL_TRUSTED_TESTER:
			return data.get(rowIndex).trusted_as_tester; 
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("setVlaueAt"+row +", "+col);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_UpdatesKeysInfo crt = data.get(row);
		switch(col) {
		case TABLE_COL_NAME:
			System.out.println("txtField"+ aValue);
			crt.my_tester_name = Util.getString(aValue);
			if(crt.my_tester_name!=null) crt.my_tester_name = crt.my_tester_name.trim();
			if("".equals(crt.my_tester_name)) crt.my_tester_name = null;
			try {
				data.get(row).store("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_WEIGHT: 
			crt.weight = Float.valueOf(Util.getString(aValue)).floatValue();
			try {
				data.get(row).store("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_REFERENCE:
			data.get(row).reference = ((Boolean)aValue).booleanValue(); 
			try {
				data.get(row).store("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_TRUSTED_MIRROR:
			data.get(row).trusted_as_mirror = ((Boolean)aValue).booleanValue(); 
			try {
				data.get(row).store("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_TRUSTED_TESTER:
			data.get(row).trusted_as_tester = ((Boolean)aValue).booleanValue(); 
			try {
				data.get(row).store("update");
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
		String sql = "SELECT "+updatesKeys.fields_updates_keys+" FROM "+updatesKeys.TNAME+";";
		String[]params = new String[]{};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		data = new ArrayList<D_UpdatesKeysInfo>();
		for(ArrayList<Object> _u :u){
			D_UpdatesKeysInfo ui = new D_UpdatesKeysInfo(_u);
			if(DEBUG)System.out.println("UpdatesKeysModel:update: adding : id= "+ui.updates_keys_ID);
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
	
		@Override
	public void actionPerformed(ActionEvent e) {
		TableJButton bb =(TableJButton)e.getSource();
		D_UpdatesKeysInfo uKey =data.get(bb.rowNo);
	    TesterInfoPanel testerPanel= new TesterInfoPanel(D_TesterDefinition.retriveTesterInfo(uKey.original_tester_name, uKey.public_key ));
//		QualitesTable q = new QualitesTable(data.get(bb.rowNo));
		JPanel p = new JPanel(new BorderLayout());
//		p.add(q.getScrollPane());
//		JFrame frame = new JFrame();
//		frame.setContentPane(p);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.pack();
//		frame.setSize(800,300);
//		frame.setVisible(true);
		
		p.setBackground(Color.BLUE);
        p.setMinimumSize(new Dimension(200,200));
		p.add(new JButton("hi"));
		JOptionPane.showMessageDialog(null,testerPanel,"Tester Info", JOptionPane.DEFAULT_OPTION, null);

	}

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
			return data.get(row).updates_keys_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}

	public D_UpdatesKeysInfo get_UpdatesKeysInfo(int row) {
		try{
			return data.get(row);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}

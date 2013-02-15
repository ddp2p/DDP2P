package widgets.updates;

import static util.Util._;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import com.almworks.sqlite4java.SQLiteException;

import config.Application;

import data.D_UpdatesInfo;


import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import table.updates;

public class UpdatesModel extends AbstractTableModel implements TableModel, DBListener , ActionListener{
	public static final int TABLE_COL_NAME = 0; // orginal or preferred name
	public static final int TABLE_COL_URL = 1; // mirror url 
	public static final int TABLE_COL_LAST_VERSION = 2; // downloaded info
	public static final int TABLE_COL_USED = 3; // set true if has been used for update
	public static final int TABLE_COL_QOT_ROT = 4; // set true if has been used for update
	public static final int TABLE_COL_DATE = 5; // Last contact date
	public static final int TABLE_COL_ACTIVITY = 6; // connection status
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	JComboBox comboBox;

	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={_("Name"),_("URL"),_("Last Version"),_("Use"),_("Tester QoT & RoT"), _("Last Contact"),_("Activity")};

	ArrayList<D_UpdatesInfo> data = new ArrayList<D_UpdatesInfo>(); // rows of type D_UpdateInfo -> Bean
	
	public UpdatesModel(DBInterface _db) { // constructor with dataSource -> DBInterface _db
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.updates.TNAME)), null);
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
		if(DEBUG) System.out.println("UpdatesModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_ACTIVITY) return Boolean.class;
		if(col == TABLE_COL_USED) return Boolean.class;
		if(col == TABLE_COL_QOT_ROT) return PanelRenderer.class;//ComboBoxRenderer.class;
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_NAME:
		case TABLE_COL_USED:
		case TABLE_COL_URL:
		case TABLE_COL_QOT_ROT:
			return true;
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_UpdatesInfo crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_NAME:
			String result = null;
			result = data.get(rowIndex).my_mirror_name;
			if(result == null) result = data.get(rowIndex).original_mirror_name;
			return result;
		case TABLE_COL_URL:
			return data.get(rowIndex).url;
		case TABLE_COL_LAST_VERSION:
			return data.get(rowIndex).last_version;
		case TABLE_COL_USED:
			return data.get(rowIndex).used; 
		case TABLE_COL_QOT_ROT:
			if(DEBUG)System.out.println("UpdatesModel:getValueAt:TABLE_COL_QOT_ROT");
			//			         //TableColumn testerColumn = table.getColumnModel().getColumn(TABLE_COL_QOT_ROT);
			if(data.get(rowIndex).testerInfo==null) return null;
			comboBox = new JComboBox();
			for(int i=0; i<data.get(rowIndex).testerInfo.length; i++)
				comboBox.addItem(data.get(rowIndex).testerInfo[i].name);
			JPanel p = new JPanel(new BorderLayout());
			p.add(comboBox);
			TableJButton b = new TableJButton("...",rowIndex );
			b.addActionListener(this);
			if(DEBUG)System.out.println("UpdatesModel:getValueAt:TABLE_COL_QOT_ROT: row"+b.rowNo); 
			b.setPreferredSize(new Dimension(20,30));
			p.add(b, BorderLayout.EAST);
			//			         comboBox.addItem("Ali");
			//			         comboBox.addItem("Ahmed");
			//			         comboBox.setSelectedIndex(1);
			//                  ComboBoxRenderer c = new ComboBoxRenderer();
			//                  c.addItem("Khalid");
			//                    ArrayList<String> a = new ArrayList<String>();
			//                    a.add("Ahmed");
			//                    a.add("Ali");
			return p;
			//			return new ComboBoxRenderer();////comboBox;//
		case TABLE_COL_DATE:
			if(crt.last_contact_date == null) return null;
			return Util.getString(crt.last_contact_date.getTime());
//		case TABLE_COL_ACTIVITY:
//			return data.get(rowIndex).activity;
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("setVlaueAt"+row +", "+col);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_UpdatesInfo crt = data.get(row);
		switch(col) {
		case TABLE_COL_NAME:
			crt.my_mirror_name = Util.getString(aValue);
			if(crt.my_mirror_name!=null) crt.my_mirror_name = crt.my_mirror_name.trim();
			if("".equals(crt.my_mirror_name)) crt.my_mirror_name = null;
			try {
				data.get(row).store("update");
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_URL:
			data.get(row).url = Util.getString(aValue);
			try {
				data.get(row).store("update");
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_USED:
			data.get(row).used = ((Boolean) aValue).booleanValue();
			try {
				data.get(row).store("update");
			} catch (SQLiteException e) {
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
		if(DEBUG) System.out.println("UpdatesModel: update: start:"+table);
		String sql = "SELECT "+updates.fields_updates+" FROM "+updates.TNAME+";";
		String[]params = new String[]{};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return;
		}
		data = new ArrayList<D_UpdatesInfo>();
		for(ArrayList<Object> _u :u){
			D_UpdatesInfo ui = new D_UpdatesInfo(_u);
			if(DEBUG) System.out.println("UpdatesModel: update: add: "+ui);
			data.add(ui); // add a new item to data list (rows)
		}
		this.fireTableDataChanged();
	}
	   
    public void refresh() {
		data.removeAll(data);
		update(null, null);
	}

	public void setTable(UpdatesTable updatesTable) {
		tables.add(updatesTable);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		TableJButton bb =(TableJButton)e.getSource();
		QualitesTable q = new QualitesTable(data.get(bb.rowNo));
		JPanel p = new JPanel(new BorderLayout());
		p.add(q.getScrollPane());
	//	p.setSize(400,300);
		final JFrame frame = new JFrame();
		frame.setContentPane(p);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.pack();
		frame.setSize(600,300);
		frame.setVisible(true);
		
//		p.setBackground(Color.BLUE);
  //      p.setSize(new Dimension(200,200));
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
	public static void main(String args[]) {
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
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long get_UpdatesID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).updates_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
}

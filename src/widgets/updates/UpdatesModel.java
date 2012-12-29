/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Khalid Alhamed and Marius Silaghi
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package widgets.updates;

import static util.Util._;

import java.awt.BorderLayout;
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
	private static final boolean DEBUG = false;
	JComboBox<String> comboBox;

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
		case TABLE_COL_QOT_ROT:System.out.println("TABLE_COL_QOT_ROT");
//			         //TableColumn testerColumn = table.getColumnModel().getColumn(TABLE_COL_QOT_ROT);
			         if(data.get(rowIndex).testerInfo==null) return null;
			         comboBox = new JComboBox<String>();
			         for(int i=0; i<data.get(rowIndex).testerInfo.length; i++)
			            comboBox.addItem(data.get(rowIndex).testerInfo[i].name);
			         JPanel p = new JPanel(new BorderLayout());
		             p.add(comboBox);
		             TableJButton b = new TableJButton("...",rowIndex );
		             b.addActionListener(this);
		             System.out.println(b.rowNo); 
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
				data.get(row).store();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_URL:
			data.get(row).url = Util.getString(aValue);
			try {
				data.get(row).store();
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
		if(DEBUG) System.out.println("UpdatesModel: update: start");
		try{
		_update(table, info);
		}catch(Exception e){
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("UpdatesModel: update: done");
		this.fireTableDataChanged();
	}
	public void _update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("UpdatesModel: update: start");
		String sql = "SELECT "+updates.fields_updates+" FROM "+updates.TNAME+";";
		String[]params = new String[]{};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return;
		}
		for(ArrayList<Object> _u :u){
			if(DEBUG) System.out.println("UpdatesModel: update: "+_u);
			D_UpdatesInfo ui = new D_UpdatesInfo(_u);
			if(DEBUG) System.out.println("UpdatesModel: update: will add");
			data.add(ui); // add a new item to data list (rows)
			if(DEBUG) System.out.println("UpdatesModel: update: added");
		}
	}

	public void setTable(UpdatesTable updatesTable) {
		tables.add(updatesTable);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		TableJButton bb =(TableJButton)e.getSource();
		QualitesTable q = new QualitesTable(data.get(bb.rowNo));
		JOptionPane.showConfirmDialog(null,q);

	}
	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
			JPanel test = new JPanel();
			//frame.add(test);
			test.setLayout(new BorderLayout());
			UpdatesTable t = new UpdatesTable();
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
}

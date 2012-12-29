/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Khalid Alhamed
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
package widgets.updatesKeys;

import static util.Util._;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JButton;
import com.almworks.sqlite4java.SQLiteException;

import config.Application;

import data.D_UpdatesKeysInfo;


import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import table.updatesKeys;

public class UpdatesKeysModel extends AbstractTableModel implements TableModel, DBListener {
	public static final int TABLE_COL_NAME = 0; // orginal or preferred name
	public static final int TABLE_COL_URL = 1; // mirror url 
	public static final int TABLE_COL_LAST_VERSION = 2; // downloaded info
	public static final int TABLE_COL_USED = 3; // set true if has been used for update
	public static final int TABLE_COL_WEIGHT = 4; // selecting wight value
	public static final int TABLE_COL_REFERENCE = 5; // required distebutor
	public static final int TABLE_COL_ACTIVITY = 6; // connection status
	public static final int TABLE_COL_DATE = 6; // Last contact date
	private static final boolean DEBUG = true;

	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={_("Name"),_("URL"),_("Last Version"),_("Used"),_("Weight"),_("Reference"),_("Activity"), _("Last Contact")};

	ArrayList<D_UpdatesKeysInfo> data = new ArrayList<D_UpdatesKeysInfo>(); // rows of type D_UpdateInfo -> Bean
	
	public UpdatesKeysModel(DBInterface _db) { // constructor with dataSource -> DBInterface _db
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.updatesKeys.TNAME)), null);
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
		if(col == TABLE_COL_ACTIVITY) return Boolean.class;
		if(col == this.TABLE_COL_WEIGHT) return Integer.class;
		if(col == this.TABLE_COL_USED) return Boolean.class;
		if(col == this.TABLE_COL_REFERENCE) return Boolean.class;
		
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_NAME:
		case TABLE_COL_USED:
		case TABLE_COL_WEIGHT:
		case TABLE_COL_URL:
			return true;
		}
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		switch(columnIndex){
		case TABLE_COL_NAME:
			String result = null;
			result = data.get(rowIndex).my_tester_name;
			if(result == null) result = data.get(rowIndex).original_tester_name;
			return result;
		case TABLE_COL_URL: return data.get(rowIndex).url;
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("setVlaueAt"+row +", "+col);
		if((row<0) || (row>data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		switch(col) {
		case TABLE_COL_NAME:
			data.get(row).my_tester_name = Util.getString(aValue);
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
		String sql = "SELECT "+updatesKeys.fields_updates_keys+" FROM "+updatesKeys.TNAME+";";
		String[]params = new String[]{};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return;
		}
		for(ArrayList<Object> _u :u){
			D_UpdatesKeysInfo ui = new D_UpdatesKeysInfo(_u);
			data.add(ui); // add a new item to data list (rows)
		}
		this.fireTableDataChanged();
	}

	public void setTable(UpdatesKeysTable l) {
		tables.add(l);
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
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

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
package widgets.updates;

import static util.Util._;

import java.awt.BorderLayout;
import java.awt.GridLayout;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import com.almworks.sqlite4java.SQLiteException;

import config.Application;

import data.D_UpdatesInfo;
import data.D_TesterInfo;
import data.D_ReleaseQuality;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import table.updates;


public class QualitiesModel extends AbstractTableModel implements TableModel{
	public static final int TABLE_COL_TESTERS = 0;
	public static final int TABLE_COL_SECURITY = 1; // orginal or preferred name
	public static final int TABLE_COL_PLATFORM = 2; // mirror url 
	public static final int TABLE_COL_USEABILITY = 3; // downloaded info
	public static final int TABLE_COL_TOTAL = 4;
	private static final boolean DEBUG = true;
	String columnNames[];//={_("   "),_("Security"),_("Platform"),_("Useability"), _("Total")};
	
	D_ReleaseQuality[] releaseQoT ; // It should be part of D_UpdatesInfo

	D_TesterInfo[] data;
	JPanel cellContent;
	
	public QualitiesModel(D_UpdatesInfo u) { // constructor with dataSource -> DBInterface _db
        data = u.testerInfo;
        this.releaseQoT = u.releaseQoT;
        columnNames = new String[releaseQoT.length+2]; //first+last
        columnNames[0]= _("   ");
        System.out.println("length = "+ columnNames.length);
        columnNames[columnNames.length-1]= _("Total");
        for(int i=1; i<columnNames.length-1; i++)
        	columnNames[i] = releaseQoT[i-1].getQualityName();
        //build columns here
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("QualitiesModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_TESTERS || col == TABLE_COL_TOTAL ) return String.class;
//		if(col == TABLE_COL_USED) return Boolean.class;
//		if(col == TABLE_COL_QOT_ROT) return ComboBoxRenderer.class;
		return PanelRenderer.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
    public JPanel buildPanel(String[] d){
    	JPanel p = new JPanel(new GridLayout(1,d.length));
    	JLabel l=null;
    	for(int i =0; i<d.length; i++){
    		l = new JLabel(d[i]);
    		l.setSize(15,10);
    		p.add(l);
    	}
    	return p; 
    }
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>=data.length+1)) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_TesterInfo crt = data[rowIndex];
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_TESTERS:
			if(rowIndex==0) return "";
			return crt.name;
		case TABLE_COL_TOTAL:
			if(rowIndex==0) return "";
			return 5; // sum(crt);

		default :
			 if(rowIndex==0){
			 	for(int i=1; i<columnNames.length-1; i++)
        	      if(columnNames[columnIndex] == releaseQoT[i-1].getQualityName()){
        	      	return buildPanel(releaseQoT[i-1].subQualities);
        	      }
			 }
			 for(int i=1; i<columnNames.length-1; i++)
        	      if(columnNames[columnIndex] == releaseQoT[i-1].getQualityName()){
        	      	return buildPanel(new String[]{"0.5"}); // crt.RoT[i-1];
        	      }
			 
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	
	public void update() {
		this.fireTableDataChanged();
	}

	
	
}

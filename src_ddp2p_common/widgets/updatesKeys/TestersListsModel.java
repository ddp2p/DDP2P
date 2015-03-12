package widgets.updatesKeys;

import static util.Util.__;

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
import config.DD;

import data.D_Tester;


import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import recommendationTesters.TesterAndScore;
import table.tester;
import widgets.updates.TableJButton;
import widgets.updates.PanelRenderer;

public class TestersListsModel extends AbstractTableModel implements TableModel{
	public static final int TABLE_COL_NAME = 0; // orginal or preferred name
	public static final int TABLE_COL_WEIGHT = 1; // selecting wight value
	private static final boolean _DEBUG = true;

	public static boolean DEBUG = false;
	
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={__("Name/ID"),__("Weight")};

	
	public TesterAndScore[] data ;
   
	public TestersListsModel(TesterAndScore[] testersRatingList) { 
		data = testersRatingList;
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
		if(DEBUG) System.out.println("UpdatesKeysModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		//if(col == this.TABLE_COL_WEIGHT) return Float.class;
		//if(col == this.TABLE_COL_NAME) return String.class;//TesterNameCellPanel.class;//PanelRenderer.class;
		
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
    
    
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>data.length)) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		TesterAndScore crt = data[rowIndex];
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_NAME:
			String result = null;
			result = Util.getStringID(data[rowIndex].testerID);// get it from DB
			return result;
		case TABLE_COL_WEIGHT:
			return Util.getString(crt.score);//.score ;//Util.getString(new Integer(data[rowIndex].score)); 
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("setVlaueAt"+row +", "+col);
		if((row<0) || (row>=data.length)) return;
		if((col<0) || (col>this.getColumnCount())) return;
		TesterAndScore crt = data[row];
		switch(col) {
		case TABLE_COL_NAME:
			break;
		case TABLE_COL_WEIGHT: // use float value for validation
		}
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	
	public void setTable(TestersListsTable l) {
		tables.add(l);
	}
	

	
}

package widgets.updates;

import static util.Util._;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.awt.Dimension;
import javax.swing.SwingConstants;
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
import util.P2PDDSQLException;

import config.Application;

import data.D_MirrorInfo;
import data.D_TesterInfo;
import data.D_ReleaseQuality;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import table.mirror;


public class QualitiesModel extends AbstractTableModel implements TableModel{
	public static final int TABLE_COL_TESTERS = 0;
	public static final int TABLE_COL_SECURITY = 1; // orginal or preferred name
	public static final int TABLE_COL_PLATFORM = 2; // mirror url 
	public static final int TABLE_COL_USEABILITY = 3; // downloaded info
	public static final int TABLE_COL_TOTAL = 4;
	private static final boolean DEBUG = false;
	String columnNames[];//={_("   "),_("Security"),_("Platform"),_("Useability"), _("Total")};
	
	D_ReleaseQuality[] releaseQoT ; // It should be part of D_MirrorInfo

	D_TesterInfo[] data;
	JPanel cellContent;
	
	public QualitiesModel(D_MirrorInfo u) { // constructor with dataSource -> DBInterface _db
        data = u.testerInfo;
        this.releaseQoT = u.releaseQoT;
        if(DEBUG) System.out.println("QualitiesModel(D_MirrorInfo u):   u.releaseQoT = "+ u.releaseQoT);
        columnNames = new String[releaseQoT.length+2]; //first+last
        columnNames[0]= _("Tester name");
        System.out.println("length = "+ columnNames.length);
        columnNames[columnNames.length-1]= _("Total");
        for(int i=1; i<columnNames.length-1; i++)
        	columnNames[i] = releaseQoT[i-1].getQualityName();//.substring(releaseQoT[i-1].getQualityName().indexOf("."));
        //build columns here
        update();
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
//		if(col == TABLE_COL_TESTERS || col == TABLE_COL_TOTAL ) return String.class;
//		if(col == TABLE_COL_USED) return Boolean.class;
//		if(col == TABLE_COL_QOT_ROT) return ComboBoxRenderer.class;
		return String.class;//PanelRenderer.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

    public JPanel buildPanel(String[] d){
    	JPanel p = new JPanel(new GridLayout(1,d.length));
    	JLabel l=null;
    	for(int i =0; i<d.length; i++){
    		l = new JLabel("<html><center>"+d[i]+"</center></html>");
    		l.setSize(15,10);
    		l.setHorizontalAlignment(SwingConstants.CENTER);
    		p.add(l);
    	}
    	return p; 
    }
    public int sumQoT(D_TesterInfo t){
    	int sum=0;
    	if(t.tester_QoT!= null)
    		for(int i=0; i< t.tester_QoT.length;i++)
    			if(t.tester_QoT[i]!=0) sum++;
    	
    	return sum;
    }
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>=data.length)) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_TesterInfo crt=null;
		crt = data[rowIndex];
		//System.out.println("RowInfo (crt): "+ rowIndex +"  "+ crt.name);
		if( crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_TESTERS:
		//	if(rowIndex==0) return "";
			return crt.name;
		case TABLE_COL_TOTAL:
		//	if(rowIndex==0) return "";
			return  sumQoT(crt);

		default :
//			 if(rowIndex+1 ){
//			 	for(int i=1; i<columnNames.length-1; i++)
//        	      if(columnNames[columnIndex] == releaseQoT[i-1].getQualityName()){
//        	      	return buildPanel(releaseQoT[i-1].subQualities);
//        	      }
//			 }
			 for(int i=1; i<columnNames.length-1; i++)
        	      if(columnNames[columnIndex].equals( releaseQoT[i-1].getQualityName())){
        	      	//return buildPanel(new String[]{"0.5"}); // crt.RoT[i-1];
        	      if(crt.tester_QoT[i-1]==0) return null;
        	      	return "QoT = " + crt.tester_QoT[i-1] + ", RoT = "+ crt.tester_RoT[i-1];
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

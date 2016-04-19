package net.ddp2p.widgets.updates;
import static net.ddp2p.common.util.Util.__;
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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_MirrorInfo;
import net.ddp2p.common.data.D_ReleaseQuality;
import net.ddp2p.common.data.D_SoftwareUpdatesReleaseInfoByTester;
import net.ddp2p.common.table.mirror;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
public class QualitiesModel extends AbstractTableModel implements TableModel{
	public static final int TABLE_COL_TESTERS = 0;
	public static final int TABLE_COL_SECURITY = 1; 
	public static final int TABLE_COL_PLATFORM = 2; 
	public static final int TABLE_COL_USEABILITY = 3; 
	public static final int TABLE_COL_TOTAL = 4;
	private static final boolean DEBUG = false;
	String columnNames[];
	D_ReleaseQuality[] releaseQoT ; 
	D_SoftwareUpdatesReleaseInfoByTester[] data;
	JPanel cellContent;
	public QualitiesModel(D_MirrorInfo u) { 
        data = u.testerInfo;
        this.releaseQoT = u.releaseQoT;
        if(DEBUG) System.out.println("QualitiesModel(D_MirrorInfo u):   u.releaseQoT = "+ u.releaseQoT);
        columnNames = new String[releaseQoT.length+2]; 
        columnNames[0]= __("Tester name");
        System.out.println("length = "+ columnNames.length);
        columnNames[columnNames.length-1]= __("Total");
        for(int i=1; i<columnNames.length-1; i++)
        	columnNames[i] = releaseQoT[i-1].getQualityName();
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
		return String.class;
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
    public int sumQoT(D_SoftwareUpdatesReleaseInfoByTester t){
    	int sum=0;
    	if(t.tester_QoT!= null)
    		for(int i=0; i< t.tester_QoT.length;i++)
    			if(t.tester_QoT[i]!=0) sum++;
    	return sum;
    }
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex<0) || (rowIndex>=data.length)) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_SoftwareUpdatesReleaseInfoByTester crt=null;
		crt = data[rowIndex];
		if( crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_TESTERS:
			return crt.name;
		case TABLE_COL_TOTAL:
			return  sumQoT(crt);
		default :
			 for(int i=1; i<columnNames.length-1; i++)
        	      if(columnNames[columnIndex].equals( releaseQoT[i-1].getQualityName())){
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
	}
	@Override
	public void removeTableModelListener(TableModelListener l) {
	}
	public void update() {
		this.fireTableDataChanged();
	}
}

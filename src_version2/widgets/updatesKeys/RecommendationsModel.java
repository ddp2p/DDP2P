package widgets.updatesKeys;

import static util.Util.__;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import util.Util;

public class RecommendationsModel extends AbstractTableModel implements TableModel{
	public static final int TABLE_COL_PEER = 0; // orginal or preferred name
	//public static final int TABLE_COL_WEIGHT = 1; // selecting wight value
	private static final boolean _DEBUG = true;

	public static boolean DEBUG = false;
	
	Float[][] scoreMatrix;
	Long[] sourcePeers;
	Long[] receivedTesters;
	
	String columnNames[];

   
	public RecommendationsModel(Float[][] scoreMatrix, Long[] sourcePeers,
			Long[] receivedTesters) { 
		this.scoreMatrix = scoreMatrix;
		this.sourcePeers = sourcePeers;
		this.receivedTesters = receivedTesters;
		columnNames = new String[receivedTesters.length+1];
		columnNames[0] = "     ";
		for(int i=0; i<receivedTesters.length; i++)
			columnNames[i+1] = "T"+receivedTesters[i];
		if(!DEBUG) System.out.println("scoreMatrix.length:"+ scoreMatrix.length);
	}

	@Override
	public int getRowCount() {
		return scoreMatrix.length;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("RecommendationsModel:getColumnName: col Header["+col+"]="+columnNames[col]);
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
		if((rowIndex<0) || (rowIndex>scoreMatrix.length)) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		if(DEBUG) System.out.println("rowIndex:"+ rowIndex +" columnIndex" +columnIndex);
		switch(columnIndex){
		case TABLE_COL_PEER:
			String result = null;
			result = "P"+Util.getStringID(this.sourcePeers[rowIndex]);
			return result;
		}
		return Util.getString(scoreMatrix[rowIndex ][columnIndex-1]);
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
	
}

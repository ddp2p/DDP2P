package widgets.components;

import java.awt.Component;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class TableUpdater implements Runnable{
	private static final boolean DEBUG = false;
	AbstractTableModel model;
	Component mytable;

	public TableUpdater(AbstractTableModel _model, Component mytable2){
		model = _model;
		mytable = mytable2;
		SwingUtilities.invokeLater(this);
	}
	public void run() {
		if(DEBUG) System.out.println("PrivateOrgModel: update: run start");
		try{
			model.fireTableDataChanged();
		}catch(Exception e){}
		if(mytable!=null) mytable.repaint();
		if(DEBUG) System.out.println("PrivateOrgModel: update: run done "+mytable);
	}
}
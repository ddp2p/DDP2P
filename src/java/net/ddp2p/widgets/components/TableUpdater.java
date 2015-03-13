package net.ddp2p.widgets.components;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class TableUpdater implements Runnable{
	private static final boolean DEBUG = false;
	AbstractTableModel model;
	Component mytable;
	ArrayList<Component> mytables;

	/**
	 * 
	 * @param _model
	 * @param mytable2 (a table, may be null)
	 */
	public TableUpdater(AbstractTableModel _model, Component mytable2, ArrayList<Component> tables){
		model = _model;
		mytable = mytable2;
		mytables = tables;
		SwingUtilities.invokeLater(this);
	}
	public void run() {
		if(DEBUG) System.out.println("PrivateOrgModel: update: run start");
		try{
			if(model!=null)model.fireTableDataChanged();
		}catch(Exception e){}
		if(mytable!=null) mytable.repaint();
		if(mytables!=null)
			for(Component _t : mytables) _t.repaint();
		if(DEBUG) System.out.println("PrivateOrgModel: update: run done "+mytable);
	}
}
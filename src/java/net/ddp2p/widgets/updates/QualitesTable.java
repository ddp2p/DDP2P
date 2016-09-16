/**
 * @(#)QualitesTable.java
 *
 *
 * @author 
 * @version 1.00 2012/12/23
 */

package net.ddp2p.widgets.updates;

import static net.ddp2p.common.util.Util.__;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_MirrorInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.widgets.components.BulletRenderer;

public class  QualitesTable extends JTable{
	
	private static final boolean DEBUG = false;
//	private static final int DIM_X = 400;
//	private static final int DIM_Y = 300;
	BulletRenderer bulletRenderer = new BulletRenderer();
	//private ColorRenderer colorRenderer;
	private DefaultTableCellRenderer centerRenderer;


	public QualitesTable(D_MirrorInfo updateInfo) {
		super(new QualitiesModel(updateInfo));
		init();
	}
	
	public QualitiesModel getModel(){
		return (QualitiesModel)super.getModel();
	}
	void init(){
		if(DEBUG)System.out.println("QualitiesModel:init:start");
		//getModel().setTable(this);
		//addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
//		for(int i=1; i< getModel().getColumnCount()-1; i++ ){		
//	    	getColumnModel().getColumn(i).setCellRenderer(new PanelRenderer());
//	    	getColumnModel().getColumn(i).setCellEditor(new MyPanelEditor());
//		}
		//getModel().update();
		if(DEBUG)System.out.println("UpdateTable:init:done");
		//his.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		//this.setFillsViewportHeight(true);
		return scrollPane;
	}
//	public TableCellRenderer getCellRenderer(int row, int column) {
//		//if ((column == QualitiesModel.TABLE_COL_NAME)) return colorRenderer;
//		if ((column == QualitiesModel.TABLE_COL_ACTIVITY)) return bulletRenderer;// when successfully connect the server
//		return super.getCellRenderer(row, column);
//	}


	private void initColumnSizes() {
	//	this.rowHeight=20;
        QualitiesModel model = (QualitiesModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < model.getColumnCount(); i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 this, getValueAt(r, i),
                                 false, false, 0, i);
            	cellWidth = Math.max(comp.getPreferredSize().width, cellWidth);
            }
            if (DEBUG) {
                System.out.println("Initializing width of column "
                                   + i + ". "
                                   + "headerWidth = " + headerWidth
                                   + "; cellWidth = " + cellWidth);
            }
 
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
            //column.setWidth(Math.max(headerWidth, cellWidth));
        }
    }
}

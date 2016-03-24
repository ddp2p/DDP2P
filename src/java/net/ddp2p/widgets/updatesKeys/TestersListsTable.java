package net.ddp2p.widgets.updatesKeys;

import static net.ddp2p.common.util.Util.__;

import java.awt.Component;
import java.awt.Dimension;




import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.ddp2p.widgets.updates.UpdatesModel;


public class TestersListsTable extends JTable {
	public static boolean DEBUG = false;
	private DefaultTableCellRenderer centerRenderer;
	
	public TestersListsTable(TestersListsModel dm) {
		super(dm);
		init();
	}
	
	void init(){
		if(DEBUG) System.out.println("TestersListsTable:init:start");
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		
		for(int i=0; i<this.getModel().getColumnCount(); i++)
			getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);	
		if(DEBUG) System.out.println("UpdateTable:init:done");
		//initColumnSizes();
		
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
        this.setPreferredScrollableViewportSize(new Dimension(200,200));
		this.setFillsViewportHeight(true);
        scrollPane.setViewportView(this);
		return scrollPane;
	}
	private void initColumnSizes() {
		this.rowHeight=20;
		TestersListsModel model = (TestersListsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
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
        }
    }
}

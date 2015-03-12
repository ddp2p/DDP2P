/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
package widgets.threads;

import static util.Util._;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import widgets.app.ThreadsAccounting;
import widgets.components.BulletRenderer;
import widgets.components.XTableColumnModel;

@SuppressWarnings("serial")
public class ThreadsView  extends JTable implements MouseListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;
	public static final int HOT_SEC = 10;
	BulletRenderer bulletRenderer = new BulletRenderer();
	private XTableColumnModel yourColumnModel;
	
	public ThreadsView() {
		super(ThreadsAccounting.instance());
		if(DEBUG) System.out.println("ThreadsView: constr from model");
		init();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(DIM_X, DIM_Y));
        jp.add(scrollPane, BorderLayout.CENTER);
		return jp;
    }

	public TableCellRenderer getCellRenderer(int row, int _column) {
		int column = this.convertColumnIndexToModel(_column);
		if ((column == ThreadsAccounting.TABLE_COL_HOT))
			return bulletRenderer;
		return super.getCellRenderer(row, _column);
	}
	protected String[] columnToolTips = {
			_("A name regsitered for thread"),
			_("State at last ping"),
			_("Ping in less than:")+""+HOT_SEC,
			_("Creation date"),
			_("Date of Last Ping")};
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= columnToolTips.length) return null;
				return columnToolTips[realIndex];
            }
        };
    }
	
	public ThreadsAccounting getModel(){
		return (ThreadsAccounting) super.getModel();
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		
		yourColumnModel = new widgets.components.XTableColumnModel();
		setColumnModel(yourColumnModel); 
		createDefaultColumnsFromModel(); 
		
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		////this.setAutoCreateRowSorter(true);
		//this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
	}
	
	/**
	 * Call this to remove a current column
	 * @param crt_col
	 */
	public void removeColumn(int crt_col){
		TableColumn column  = this.yourColumnModel.getColumn(crt_col);
		yourColumnModel.setColumnVisible(column, false);
	}
	public void addColumn(int crt_col){
		TableColumn column  = this.yourColumnModel.getColumnByModelIndex(crt_col);
		yourColumnModel.setColumnVisible(column, true);
	}
	void initColumnSizes() {
        TableModel model = (TableModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < this.getColumnCount(); i++)
        {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(this.convertColumnIndexToModel(i))).
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

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}

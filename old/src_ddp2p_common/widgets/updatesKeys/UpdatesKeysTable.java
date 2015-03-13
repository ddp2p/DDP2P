package widgets.updatesKeys;

import static util.Util.__;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Point;
import java.awt.event.KeyEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import util.DBInterface;
import util.P2PDDSQLException;
import widgets.app.DDIcons;
import widgets.components.BulletRenderer;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import widgets.updates.PanelRenderer;
import widgets.updates.MyPanelEditor;
import widgets.updates.UpdateCustomAction;

public class UpdatesKeysTable extends JTable implements MouseListener{
	
	private static final boolean DEBUG = false;
	private static final int DIM_X = 400;
	private static final int DIM_Y = 300;
	BulletRenderer bulletRenderer = new BulletRenderer();
	//private ColorRenderer colorRenderer;
	private DefaultTableCellRenderer centerRenderer;

	public UpdatesKeysTable() {
		super(new UpdatesKeysModel(Application.db));
		init();
	}
	public UpdatesKeysTable(DBInterface _db) {
		super(new UpdatesKeysModel(_db));
		init();
	}
	public UpdatesKeysTable(UpdatesKeysModel dm) {
		super(dm);
		init();
	}
	public UpdatesKeysModel getModel(){
		return (UpdatesKeysModel)super.getModel();
	}
	void init(){
		if(DEBUG) System.out.println("UpdateKeysTable:init:start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
	//	initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		
		//getColumnModel().getColumn(UpdatesKeysModel.TABLE_COL_NAME).setCellRenderer(new TesterNameCellPanel());
		//getColumnModel().getColumn(UpdatesKeysModel.TABLE_COL_NAME).setCellEditor(new TesterNameCellEditor());
		getColumnModel().getColumn(UpdatesKeysModel.TABLE_COL_NAME).setCellRenderer(new TesterNameColumnRenderer(this.getDefaultRenderer(String.class)));
		getColumnModel().getColumn(UpdatesKeysModel.TABLE_COL_NAME).setCellEditor(new TesterNameCellEditor(this.getDefaultEditor(String.class)));
		
		if(DEBUG) System.out.println("UpdateKeysTable:init:done");
		//his.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		//this.setMinimumSize(new Dimension(400,200));
		//scrollPane.setMinimumSize(new Dimension(400,200));
		return scrollPane;
	}
//	public TableCellRenderer getCellRenderer(int row, int column) {
//		//if ((column == UpdatesKeysModel.TABLE_COL_NAME)) return colorRenderer;
//		if ((column == UpdatesKeysModel.TABLE_COL_ACTIVITY)) return bulletRenderer;// when successfully connect the server
//		return super.getCellRenderer(row, column);
//	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		//jtableMouseReleased(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
	private void initColumnSizes() {
        UpdatesKeysModel model = (UpdatesKeysModel)this.getModel();
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
        }
    }
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	if(!evt.isPopupTrigger()) return;
    	boolean autoSeleced =false;
    	try {
    		autoSeleced = DD.getAppBoolean(DD.AUTOMATIC_TESTERS_RATING_BY_SYSTEM);
    	} catch (P2PDDSQLException e) {
    		e.printStackTrace();
    	} 
    	if(autoSeleced){
    		Application_GUI.warning("you need to select manual rating mode to use this option", "Manual Rating");
    		return;
    	}
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    //	getModel().update(null, null);
    }
    JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	ImageIcon importicon = DDIcons.getImpImageIcon(__("import information"));
    	JPopupMenu popup = new JPopupMenu();
    	UpdateKeysCustomAction aAction;
    	//System.out.println(importicon.toString());
    	
    	
    	aAction = new UpdateKeysCustomAction(this, __("Import!"), importicon,__("Import Mirrors"), __("Import"),KeyEvent.VK_I, UpdateKeysCustomAction.M_IMPORT);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	
    	aAction = new UpdateKeysCustomAction(this, __("Delete"), delicon,__("Delete Mirror"), __("Delete"),KeyEvent.VK_D, UpdateKeysCustomAction.M_DELETE);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	    
    	return popup;
	}
	public void setColumnBackgroundColor(Color gray) {
		// TODO Auto-generated method stub
		
	}
}

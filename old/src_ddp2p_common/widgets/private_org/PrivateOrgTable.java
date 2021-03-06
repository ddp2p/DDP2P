package widgets.private_org;

import static util.Util.__;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Point;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.table.TableColumn;

import util.DBInterface;
import widgets.app.DDIcons;
import widgets.components.BulletRenderer;
import widgets.components.DebateDecideAction;
import config.Application;
import config.Identity;
import data.D_Organization;
import util.P2PDDSQLException;

//import widgets.updatesKeys.UpdatesKeysTable;


public class PrivateOrgTable extends JTable implements MouseListener{
	
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	//private static final int DIM_X = 400;
	//private static final int DIM_Y = 300;
	//BulletRenderer bulletRenderer = new BulletRenderer();
	//private ColorRenderer colorRenderer;
	private DefaultTableCellRenderer centerRenderer;
	private PrivateOrgPanel privateOrgPanel;

	public PrivateOrgModel getPModel(){
		return (PrivateOrgModel)super.getModel();
	}
	public PrivateOrgTable(PrivateOrgPanel privateOrgPanel) {
		super(new PrivateOrgModel(Application.db, privateOrgPanel));
		this.privateOrgPanel = privateOrgPanel;
		this.getPModel().setMyTable(this);
		init();
	}
	public PrivateOrgTable(DBInterface _db, PrivateOrgPanel privateOrgPanel) {
		super(new PrivateOrgModel(_db, privateOrgPanel));
		this.getPModel().setMyTable(this);
		init();
	}
	public PrivateOrgTable(PrivateOrgModel dm) {
		super(dm);
		this.getPModel().setMyTable(this);
		init();
	}
	public PrivateOrgModel getModel(){
		return (PrivateOrgModel)super.getModel();
	}
	void init(){
		if(DEBUG) System.out.println("PrivateOrgTable:init:start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
	/*	this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);*/
//		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellRenderer(new ComboBoxRenderer());
//		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellEditor(new MyComboBoxEditor(new String[]{"khalis"}));
//		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellRenderer(new PanelRenderer());
//		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellEditor(new MyPanelEditor());
//		getColumnModel().getColumn(TermsModel.TABLE_COL_SERVICE).setCellRenderer(new DefaultTableCellRenderer());
//		getColumnModel().getColumn(TermsModel.TABLE_COL_SERVICE).setCellEditor(new DefaultCellEditor(getModel().getServiceComboBox()));
//		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY_TYPE).setCellRenderer(new DefaultTableCellRenderer());
//		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY_TYPE).setCellEditor(new DefaultCellEditor(getModel().getPriorityTypeComboBox()));		
//		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY).setWidth(0);
//		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY).setMaxWidth(0);
//		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY).setMinWidth(0);
		getModel().update(null,null);
		if(DEBUG) System.out.println("PrivateOrgTable:init:done");
		//his.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
        //scrollPane.setPreferredSize(new Dimension(400,100));
        //scrollPane.show(false);
		this.setFillsViewportHeight(true);
		//this.setMinimumSize(new Dimension(400,200));
		//scrollPane.setMinimumSize(new Dimension(400,200));
		return scrollPane;
	}
/*	public TableCellRenderer getCellRenderer(int row, int column) {
		//if ((column == UpdatesModel.TABLE_COL_NAME)) return colorRenderer;
		if ((column == UpdatesModel.TABLE_COL_ACTIVITY)) return bulletRenderer;// when successfully connect the server
		return super.getCellRenderer(row, column);
	}
*/

	@Override
	public void mouseClicked(MouseEvent e) {
		jtableMouseReleased(e);
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
		jtableMouseReleased(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
	private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
		if(DEBUG) System.out.println("PrivateOrgTable:jtableMouseReleased: mouserelease");
    	int _row; //=this.getSelectedRow();
    	int model_row = -1;
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()){
    		if(DEBUG) System.out.println("PrivateOrgTable:jtableMouseReleased: not popup");
    		return;
    	}
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        _row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if(_row>=0) model_row = this.convertRowIndexToModel(_row);
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup == null){
    		if(DEBUG) System.out.println("PrivateOrgTable:jtableMouseReleased: popup null");
    		return;
    	}
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    //	getModel().update(null, null);
    }
    JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	JPopupMenu popup = new JPopupMenu();
    	PrivateOrgCustomAction aAction;
    	
    	aAction = new PrivateOrgCustomAction(this, __("Stop"), delicon,__("Stop Peer Distribution"), __("Delete"),KeyEvent.VK_D, PrivateOrgCustomAction.M_DELETE);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	
    
    	return popup;
	}
	
	private void initColumnSizes() {
		this.rowHeight=20;
        PrivateOrgModel model = (PrivateOrgModel)this.getModel();
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
/*	public JTable getSubTable() {
		if(termsPanel==null) return null;
		return updatesPanel.getTestersTable();
	}
*/
	public void setOrg(D_Organization org) {
		this.getModel().setOrg(org);
	}
}


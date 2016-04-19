package net.ddp2p.widgets.private_org;
import static net.ddp2p.common.util.Util.__;
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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.components.DebateDecideAction;
public class PrivateOrgTable extends JTable implements MouseListener{
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	private DefaultTableCellRenderer centerRenderer;
	private PrivateOrgPanel privateOrgPanel;
	public PrivateOrgModel getPModel(){
		return (PrivateOrgModel)super.getModel();
	}
	public PrivateOrgTable(PrivateOrgPanel privateOrgPanel) {
		super(new PrivateOrgModel(Application.getDB(), privateOrgPanel));
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
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		getModel().update(null,null);
		if(DEBUG) System.out.println("PrivateOrgTable:init:done");
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
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
    	int _row; 
    	int model_row = -1;
    	int col; 
    	if(!evt.isPopupTrigger()){
    		if(DEBUG) System.out.println("PrivateOrgTable:jtableMouseReleased: not popup");
    		return;
    	}
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
	public void setOrg(D_Organization org) {
		this.getModel().setOrg(org);
	}
}

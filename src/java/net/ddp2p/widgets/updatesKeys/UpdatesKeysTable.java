package net.ddp2p.widgets.updatesKeys;
import static net.ddp2p.common.util.Util.__;
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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.updates.MyPanelEditor;
import net.ddp2p.widgets.updates.PanelRenderer;
import net.ddp2p.widgets.updates.UpdateCustomAction;
public class UpdatesKeysTable extends JTable implements MouseListener{
	private static final boolean DEBUG = false;
	private static final int DIM_X = 400;
	private static final int DIM_Y = 300;
	BulletRenderer bulletRenderer = new BulletRenderer();
	private DefaultTableCellRenderer centerRenderer;
	public UpdatesKeysTable() {
		super(new UpdatesKeysModel(Application.getDB()));
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
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		getColumnModel().getColumn(UpdatesKeysModel.TABLE_COL_NAME).setCellRenderer(new TesterNameColumnRenderer(this.getDefaultRenderer(String.class)));
		getColumnModel().getColumn(UpdatesKeysModel.TABLE_COL_NAME).setCellEditor(new TesterNameCellEditor(this.getDefaultEditor(String.class)));
		if(DEBUG) System.out.println("UpdateKeysTable:init:done");
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
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
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	private void initColumnSizes() {
        UpdatesKeysModel model = (UpdatesKeysModel)this.getModel();
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
    	int row; 
    	int col; 
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
    JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	ImageIcon importicon = DDIcons.getImpImageIcon(__("import information"));
    	JPopupMenu popup = new JPopupMenu();
    	UpdateKeysCustomAction aAction;
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
	}
}

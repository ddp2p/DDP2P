package net.ddp2p.widgets.updates;
import static net.ddp2p.common.util.Util.__;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.Point;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.BulletRenderer;
public class UpdatesTable extends JTable implements MouseListener{
	public static boolean DEBUG = false;
	private static final int DIM_X = 400;
	private static final int DIM_Y = 300;
	BulletRenderer bulletRenderer = new BulletRenderer();
	private DefaultTableCellRenderer centerRenderer;
	private UpdatesPanel updatesPanel;
	public UpdatesTable(UpdatesPanel updatesPanel) {
		super(new UpdatesModel(Application.getDB()));
		this.updatesPanel = updatesPanel;
		init();
	}
	public UpdatesTable(DBInterface _db) {
		super(new UpdatesModel(_db));
		init();
	}
	public UpdatesTable(UpdatesModel dm) {
		super(dm);
		init();
	}
	public UpdatesModel getModel(){
		return (UpdatesModel)super.getModel();
	}
	void init(){
		if(DEBUG) System.out.println("UpdateTable:init:start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellRenderer(new PanelRenderer());
		getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellEditor(new MyPanelEditor());
		getModel().update(null,null);
		if(DEBUG) System.out.println("UpdateTable:init:done");
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == UpdatesModel.TABLE_COL_ACTIVITY)) return bulletRenderer;
		return super.getCellRenderer(row, column);
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
		if(DEBUG) System.out.println("UpdatesTable:jtableMouseReleased: mouserelease");
    	int _row; 
    	int model_row = -1;
    	int col; 
    	if(!evt.isPopupTrigger()){
    		if(DEBUG) System.out.println("UpdatesTable:jtableMouseReleased: not popup");
    		return;
    	}
    	Point point = evt.getPoint();
        _row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if(_row>=0) model_row = this.convertRowIndexToModel(_row);
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup == null){
    		if(DEBUG) System.out.println("UpdatesTable:jtableMouseReleased: popup null");
    		return;
    	}
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
    JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	ImageIcon importicon = DDIcons.getImpImageIcon(__("import information"));
    	ImageIcon useUpdateIcon = DDIcons.getImpImageIcon(__("use update"));
    	JPopupMenu popup = new JPopupMenu();
    	UpdateCustomAction aAction;
    	aAction = new UpdateCustomAction(this, __("Import!"), importicon,__("Import Mirrors"), __("Import"),KeyEvent.VK_I, UpdateCustomAction.M_IMPORT);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	aAction = new UpdateCustomAction(this, __("Delete"), delicon,__("Delete Mirror"), __("Delete"),KeyEvent.VK_D, UpdateCustomAction.M_DELETE);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	aAction = new UpdateCustomAction(this, __("Use This Update"), useUpdateIcon,__("Use This Update"), __("Use This Update"),KeyEvent.VK_U, UpdateCustomAction.USE_UPDATE);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	return popup;
	}
	private void initColumnSizes() {
		this.rowHeight=20;
        UpdatesModel model = (UpdatesModel)this.getModel();
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
	public JTable getSubTable() {
		if(updatesPanel==null) return null;
		return updatesPanel.getTestersTable();
	}
}

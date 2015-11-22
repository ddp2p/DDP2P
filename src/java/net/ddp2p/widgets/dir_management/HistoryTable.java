package net.ddp2p.widgets.dir_management;
import static net.ddp2p.common.util.Util.__;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Point;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTextField;
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
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.hds.DirMessage;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.components.DebateDecideAction;
public class HistoryTable extends JTable implements MouseListener{
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	private DefaultTableCellRenderer centerRenderer;
	private DirPanel dirPanel;
	public HistoryTable(DirPanel dirPanel) {
		super(new HistoryModel(Application.getDB(), dirPanel));
		this.dirPanel = dirPanel;
		init();
	}
	public HistoryTable(DBInterface _db, DirPanel dirPanel) {
		super(new HistoryModel(_db, dirPanel));
		this.dirPanel = dirPanel;
		init();
	}
	public HistoryTable(HistoryModel dm) {
		super(dm);
		init();
	}
	public HistoryModel getModel(){
		return (HistoryModel)super.getModel();
	}
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);
                return tip;
            }
	public void init(){
		if(DEBUG) System.out.println("HistoryTable:init:start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		getColumnModel().getColumn(HistoryModel.TABLE_COL_USED_IP).setCellRenderer(renderer);
		getColumnModel().getColumn(HistoryModel.TABLE_COL_MSG_TYPE).setCellRenderer(renderer);
		getColumnModel().getColumn(HistoryModel.TABLE_COL_REQUESTED_PEER).setCellRenderer(renderer);
		getColumnModel().getColumn(HistoryModel.TABLE_COL_TIMESTAMP).setCellRenderer(renderer);
		getModel().update(null,null);
		if(DEBUG) System.out.println("HistoryTable:init:done");
  	}
  	public JScrollPane scrollPane;
	public JScrollPane getScrollPane(){
        scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		jtableMouseReleased(e);
		viewMessage(e);
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
		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: mouserelease");
    	int _row; 
    	int model_row = -1;
    	int col; 
    	if(!evt.isPopupTrigger()){
    		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: not popup");
    		return;
    	}
    	Point point = evt.getPoint();
        _row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if(_row>=0) model_row = this.convertRowIndexToModel(_row);
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup == null){
    		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: popup null");
    		return;
    	}
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
    JPopupMenu getPopup(int row, int col){
    	JPopupMenu popup = new JPopupMenu();
    	return popup;
	}
	private void viewMessage(MouseEvent e) {
		if(DEBUG) System.out.println("HistoryTable:jtableMouseReleased: mouserelease");
    	int _row; 
    	int model_row = -1;
    	int col; 
    	Point point = e.getPoint();
        _row = this.rowAtPoint(point);
        col = this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if (_row>=0) model_row = this.convertRowIndexToModel(_row);
        DirMessage msg = getModel().getObject(model_row);
        String _msg = msg+"";
        if (msg != null) _msg = "MSG:"+msg.MsgType+": "+msg.msg;
        Application_GUI.warning(_msg, "Message");
	}
	private void initColumnSizes() {
		this.rowHeight=20;
        HistoryModel model = (HistoryModel)this.getModel();
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

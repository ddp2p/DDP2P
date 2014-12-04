/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
package widgets.justifications;

import static util.Util.__;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import util.P2PDDSQLException;
import util.DBInterface;
import util.Util;
import widgets.app.DDIcons;
import widgets.app.MainFrame;
import widgets.components.BulletRenderer;
import widgets.components.DebateDecideAction;
//import widgets.org.ColorRenderer;
import widgets.components.DocumentTitleRenderer;
import widgets.justifications.JustificationsModel;
import config.Application;
import config.JustificationsListener;
//import config.DDIcons;
import data.D_Document_Title;
import data.D_Justification;


@SuppressWarnings("serial")
public class Justifications extends JTable implements MouseListener, JustificationsListener  {
	private int DIM_X = 1000;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = 4;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private DocumentTitleRenderer titleRenderer;
	DefaultTableCellRenderer centerRenderer;
	BulletRenderer hotRenderer;
	public Justifications(int dim_x) {
		super(new JustificationsModel(Application.db));
		DIM_X = dim_x;
		//getModel().columnNames = getModel().getCrtColumns();
		init();
		//getModel().setCrtChoice(null);
	}
	public Justifications() {
		super(new JustificationsModel(Application.db));
		//getModel().columnNames = getModel().getCrtColumns();
		init();
		//getModel().setCrtChoice(null);
	}
	public Justifications(DBInterface _db) {
		super(new JustificationsModel(_db));
		//getModel().columnNames = getModel().getCrtColumns();
		init();
		//getModel().setCrtChoice(null);
	}
	public Justifications(JustificationsModel dm) {
		super(dm);
		//dm.columnNames = dm.getCrtColumns();
		init();
		//getModel().setCrtChoice(null);
	}
	/**
	 * Connecting this widget to the status.
	 * This will be told about motions and justification changes.
	 * 
	 * If an editor is incorporated, it will also be announced of changes in justifications and motions.
	 */
	public void connectWidget() {
		getModel().connectWidget();
		
		MainFrame.status.addMotionStatusListener(this.getModel());
    	if (_jedit != null) {
    		MainFrame.status.addMotionStatusListener(_jedit);
    		MainFrame.status.addJustificationStatusListener(_jedit);
    		MainFrame.status.addConstituentMeStatusListener(_jedit);
    	}
    	MainFrame.status.addJustificationStatusListener(this);
	}
	/**
	 * Disconnect listeners from status (for this model, and any editor)
	 */
	public void disconnectWidget() {
		getModel().disconnectWidget();

		MainFrame.status.removeMotListener(this.getModel());
    	if (_jedit != null) {
    	 	MainFrame.status.removeConstituentMeListener(_jedit);
    		MainFrame.status.removeMotListener(_jedit);
    		MainFrame.status.removeJustificationListener(_jedit);
    	}
    	MainFrame.status.removeJustificationListener(this);
	}
	JustificationEditor _jedit = null;
	//JPanel
	Component just_panel = null;
	public Component getComboPanel() {
		if (just_panel != null) return just_panel;
		if (DEBUG) System.out.println("createAndShowGUI: added justif");
    	if (_jedit == null) _jedit = new JustificationEditor();
		if (DEBUG) System.out.println("createAndShowGUI: added justif editor");
    	just_panel = MainFrame.makeJustificationPanel(_jedit, this);
    	//javax.swing.JScrollPane jscj = new javax.swing.JScrollPane(just_panel);
		//tabbedPane.addTab("Justifications", jscj);
    	//JScrollPane just = justifications.getScrollPane();
		//tabbedPane.addTab("Justifications", just);
		//tabbedPane.addTab("Justification", _jedit.getScrollPane());
		if(DEBUG) System.out.println("createAndShowGUI: justif pane");
		this.addListener(MainFrame.status);
		MainFrame.status.addJustificationStatusListener(_jedit);
    	return just_panel;//jscj;
	}
	void init() {
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		titleRenderer = new DocumentTitleRenderer();
		centerRenderer = new DefaultTableCellRenderer();
		hotRenderer = new BulletRenderer(
				DDIcons.getHotImageIcon("Hot"), DDIcons.getHotGImageIcon("Hot"),
				null, __("Recently Contacted"),  __("Not Recently Contacted"), null);
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		//this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		

		Comparator<D_Document_Title> documentTitleComparator = new java.util.Comparator<D_Document_Title>() {

			//@Override
			public int _compare(Object o1, Object o2) {
				if (o1 == null) return 1;
				if (o2 == null) return -1;
				String s1 = Util.getString(o1), s2 = Util.getString(o2);
				if (o1 instanceof data.D_Document_Title) s1 = ((data.D_Document_Title)o1).title_document.getDocumentUTFString();
				if (o2 instanceof data.D_Document_Title) s2 = ((data.D_Document_Title)o2).title_document.getDocumentUTFString();
				return s1.compareTo(s2);
			}

			@Override
			public int compare(D_Document_Title arg0, D_Document_Title arg1) {
				return _compare(arg0, arg1);
			}
		};
		TableRowSorter<JustificationsModel> sorter = new TableRowSorter<JustificationsModel>();
		this.setRowSorter(sorter);
		sorter.setModel(getModel());
		sorter.setComparator(getModel().TABLE_COL_NAME, documentTitleComparator);
		this.getRowSorter().toggleSortOrder(getModel().TABLE_COL_ARRIVAL_DATE);
		this.getRowSorter().toggleSortOrder(getModel().TABLE_COL_ARRIVAL_DATE);

		DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel headerLabel = (JLabel)
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Icon icon = Justifications.this.getModel().getIcon(column);
				if(icon != null)  headerLabel.setText(null);
				headerLabel.setIcon(icon);
			    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			    setHorizontalAlignment(JLabel.CENTER);
			    return headerLabel;
			}
		};
		
		//getTableHeader().setDefaultRenderer(rend);
		for (int col_index = 0; col_index < getModel().getColumnCount(); col_index++) {
			if (getModel().getIcon(col_index) != null) {
				getTableHeader().getColumnModel().getColumn(col_index).setHeaderRenderer(rend);
			}
		}
		
//    	try{
//    		if (Identity.getCurrentIdentity().identity_id!=null) {
//    			//long id = new Integer(Identity.current.identity_id).longValue();
//    			long orgID = Identity.getDefaultOrgID();
//    			this.setCurrentJust(orgID);
//    			int row =this.getSelectedRow();
//     			this.fireListener(row, A_NON_FORCE_COL, true);
//    		}
//    	}catch(Exception e){e.printStackTrace();}
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        //scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setPreferredSize(new Dimension(DIM_X, DIM_Y));
        jp.add(scrollPane, BorderLayout.CENTER);
		return jp;
    }

    DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();
	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == getModel().TABLE_COL_NAME)) return titleRenderer;
		if ((column == getModel().TABLE_COL_VOTERS_NB)) return centerRenderer;

		
		if (column == getModel().TABLE_COL_RECENT) //return super.getCellRenderer(row, column);
			return hotRenderer;
		if (column == getModel().TABLE_COL_BROADCASTED) return super.getCellRenderer(row, column);
		if (column == getModel().TABLE_COL_BLOCKED) return super.getCellRenderer(row, column);
		if (column == getModel().TABLE_COL_TMP) return super.getCellRenderer(row, column);
		if (column == getModel().TABLE_COL_GID_VALID) return super.getCellRenderer(row, column);
		if (column == getModel().TABLE_COL_SIGN_VALID) return super.getCellRenderer(row, column);
		if (column == getModel().TABLE_COL_HIDDEN) return super.getCellRenderer(row, column);
		//return super.getCellRenderer(row, column);
		//if ((column == JustificationsModel.TABLE_COL_CONNECTION)) return bulletRenderer;
//		if (column >= JustificationsModel.TABLE_COL_PLUGINS) {
//			int plug = column-JustificationsModel.TABLE_COL_PLUGINS;
//			if(plug < plugins.size()) {
//				String pluginID= plugins.get(plug);
//				return plugin_applets.get(pluginID).renderer;
//			}
//		}
		TableCellRenderer result = defaultTableCellRenderer;//super.getCellRenderer(row, column);
        if(DEBUG) System.out.println("Motions:getCellRenderer default="+result);
		return result;
		//return super.getCellRenderer(row, column);
	}
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= getModel().columnToolTipsCount()) return null;
				return getModel().columnToolTipsEntry(realIndex);
            }
        };
    }
	public JustificationsModel getModel(){
		return (JustificationsModel) super.getModel();
	}
	public void initColumnSizes() {
        JustificationsModel model = (JustificationsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int col_model = 0; col_model < model.getColumnCount(); col_model++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
            column = this.getColumnModel().getColumn(col_model);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r = 0; r < this.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(col_model)).
                             getTableCellRendererComponent(
                                 this, getValueAt(r, this.convertColumnIndexToView(col_model)),
                                 false, false, 0, col_model);
            	cellWidth = Math.max(comp.getPreferredSize().width, cellWidth);
            }
            if (DEBUG) {
                System.out.println("Initializing width of column "
                                   + col_model + ". "
                                   + "headerWidth = " + headerWidth
                                   + "; cellWidth = " + cellWidth);
            }
 
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }

	ArrayList<JustificationsListener> listeners=new ArrayList<JustificationsListener>();
	public void fireForceEdit(String orgID) {		
		if(DEBUG) System.out.println("Justifications:fireForceEdit: row="+orgID);
		for(JustificationsListener l: listeners){
			if(DEBUG) System.out.println("Justifications:fireForceEdit: l="+l);
			try{
				if(orgID==null) ;//l.forceEdit(orgID);
				else l.forceJustificationEdit(orgID);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	void fireListener(int row, int col, boolean db_sync) {
		if(DEBUG) System.out.println("Justifications:fireListener:choice="+getModel().crt_choice+"  row="+row);
		String id;
		if (row < 0) id = null;
		else id = Util.getString(this.getModel().getJustificationID(this.convertRowIndexToModel(row)));
		fireListener(id, col, db_sync);
		if(DEBUG) System.out.println("Justifications:fireListener:choice="+getModel().crt_choice+"  id="+id);
	}
	/**
	 * Announce all listeners
	 * @param lid
	 * @param col
	 * @param db_sync
	 */
	void fireListener(String lid, int col, boolean db_sync) {
   	   	if(DEBUG) System.out.println("Justifications: fire justID="+lid);
   	   	D_Justification just = null;
   	   	if (lid != null)
   	   		just = D_Justification.getJustByLID(lid, true, false);
		for ( JustificationsListener l: listeners){
			if(DEBUG) System.out.println("Justifications:fireListener: l="+l);
			try {
				l.justUpdate(lid, col, db_sync, just);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addListener(JustificationsListener l) {
		listeners.add(l);
		int row = this.getSelectedRow();
		if (row >= 0)
			l.justUpdate(Util.getString(this.getModel().getJustificationID(this.convertRowIndexToModel(row))),A_NON_FORCE_COL, false, null);
	}
	public void removeListener(JustificationsListener l) {
		listeners.remove(l);
	}

	/**
	 * Should be called from Swing thread.
	 * Sets the selection in all views.
	 * @param just_id
	 */
	public void setCurrentJust(long just_id) {
   		if(DEBUG) System.out.println("Justifications:setCurrentJust: got id="+just_id);
   		
		getModel().setCurrentJust(just_id);
	}
	public long getConstituentIDMyself() {
		return getModel().getConstituentIDMyself();
	}
	public String getConstituentGIDMyself() {
		return  getModel().getConstituentGIDMyself();
	}
	public String getOrganizationID() {
		return  getModel().getOrganizationID();
	}
	@Override
	public void mouseClicked(MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	//if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        if((row<0)||(col<0)) return;
        
    	JustificationsModel model = (JustificationsModel)getModel();
 		int model_row=convertRowIndexToModel(row);
   	   	if(model_row>=0) {
   	   		String orgID = model.getJustificationID(model_row); //Util.getString(model._justifications[model_row]);
   	   		try {
   	   			long oID = Util.lval(orgID);
   	   			model.setCurrentJust(oID);
   	   		}catch(Exception e){};
   	   	}
        
   	   	if (DEBUG) System.out.println("Justifications: mouse click row="+row);
        fireListener(row,col, false);
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
	}
	JPopupMenu getPopup(int model_row, int col){
		JMenuItem menuItem;
    	//ImageIcon addicon = Util.createImageIcon(DDIcons.I_ADD,_("add an item"));
    	//ImageIcon reseticon = Util.createImageIcon(DDIcons.I_RES,_("reset item"));
    	//ImageIcon delicon = Util.createImageIcon(DDIcons.I_DEL,_("delete an item"));
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	
    	JPopupMenu popup = new JPopupMenu();
    	JustificationsCustomAction aAction;
    	
    	aAction = new JustificationsCustomAction(this, __("Answer!"), addicon,__("Answer this."), __("Answer"),KeyEvent.VK_A, JustificationsCustomAction.J_ANSWER);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, __("Delete!"), delicon,__("Delete this."), __("Delete"),KeyEvent.VK_D, JustificationsCustomAction.J_DEL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, __("Remove Answering Filter!"), delicon,__("Remove Answering Filter."), __("Remove Answering Filter"),KeyEvent.VK_R, JustificationsCustomAction.J_REM_ANSWER);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, __("Filter by Answering This!"), delicon,__("Filter by Answering This."), __("Filter by Answering This"),KeyEvent.VK_T, JustificationsCustomAction.J_ANSWER_THIS);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	
    	aAction = new JustificationsCustomAction(this, __("Delete Partial!"), delicon,__("Delete partial."), __("Delete partial"),KeyEvent.VK_P, JustificationsCustomAction.J_DEL_PARTIAL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	
    	aAction = new JustificationsCustomAction(this, __("Refresh Cache!"), delicon,__("Refresh Cache."), __("Refresh Cache"),KeyEvent.VK_F, JustificationsCustomAction.J_REFRESH);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	return popup;
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	if (! evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row = this.rowAtPoint(point);
        col = this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if (row >= 0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if (popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
    /**
     * 
     * @param justID
     * @param col
     * @param db_sync
     * @param just
     */
	public void justificationSetCurrent(String justID, int col, boolean db_sync,
			D_Justification just) {
		justUpdate( justID, col, db_sync, just);
		// The next call is worth issue only if the function is called from outside the status. 
		// If redundant it should simply end due to lack of change (since the status is the listener and it check)
		this.fireListener(justID, col, db_sync);
	}
    /**
     * called from DD.status backward/forward.
     * If need to call from outside the status.fireListener, you should use: justificationSetCurrent
     */
	@Override
	public void justUpdate(String justID, int col, boolean db_sync,
			D_Justification just) {
		if (justID == null) return;
		int model_row = getModel().getRow(justID); //getRowByID(Util.lval(justID));
		if (model_row < 0) return;
		SwingUtilities.invokeLater(new util.DDP2P_ServiceRunnable(__("Set Selection of Justification"), false, false, Integer.valueOf(model_row)) {
			// may be daemon
			@Override
			public void _run() {
				try {
					int model_row = (Integer)ctx;
					int view_row = Justifications.this.convertRowIndexToView(model_row);
					Justifications.this.setRowSelectionInterval(view_row, view_row);
				}
				catch (java.lang.ArrayIndexOutOfBoundsException e){}
				catch (Exception e){e.printStackTrace();}
			}
			
		});
	}
	@Override
	public void forceJustificationEdit(String justID) {
		// TODO Auto-generated method stub
		
	}
}
@SuppressWarnings("serial")
class JustificationsCustomAction extends DebateDecideAction {
	public static final int J_ADD = 1;
    public static final int J_ANSWER = 2;
    public static final int J_DEL = 3;
    public static final int J_DEL_PARTIAL = 4;
    public static final int J_ANSWER_THIS = 5;
	public static final int J_REM_ANSWER = 6;
	public static final int J_REFRESH = 7;
	
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Justifications tree; ImageIcon icon; int cmd;
    public JustificationsCustomAction(Justifications tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("JustificationsCAction: start");
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		//org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
    		//org_id = tree.getModel().org_id;
    		//System.err.println("Row selected: " + row);
    	}
    	JustificationsModel model = (JustificationsModel)tree.getModel();
    	
    	if(DEBUG) System.out.println("JustificationsCAction: row = "+row);
    	//do_cmd(row, cmd);
    	if (cmd == J_ANSWER_THIS) {
    		model.setCrtAnswered(model.getJustificationID(row));
    	}
        if (cmd == J_REM_ANSWER) {
    		model.setCrtAnswered(null);        	
        }
        if (cmd == J_REFRESH) {
        	new util.DDP2P_ServiceThread(__("Justifications Refresh"), true, model) {

				@Override
				public void _run() {
					JustificationsModel model = (JustificationsModel) ctx;
					model.refreshCache();
					model._update(null, null, true);
				}
        		
        	}.start();
    		//model.setCrtAnswered(null);        	
        }
        if (cmd == J_DEL) {
    		String _j_ID = model.getJustificationID(row);
    		if(_j_ID == null) return;
    		try {
				Application.db.delete(table.justification.TNAME,
						new String[]{table.justification.justification_ID},
						new String[]{_j_ID}, DEBUG);
				Application.db.delete(table.signature.TNAME,
						new String[]{table.signature.justification_ID},
						new String[]{_j_ID}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    	if(cmd == J_DEL_PARTIAL) {
    		try {
				Application.db.delete(
						"DELETE FROM "+table.justification.TNAME+
						" WHERE "+table.justification.signature+" IS NULL OR "+table.justification.global_justification_ID+" IS NULL",
						new String[]{}, DEBUG);
				Application.db.delete(
						"DELETE FROM "+table.signature.TNAME+
						" WHERE "+table.signature.signature+" IS NULL OR "+table.signature.global_signature_ID+" IS NULL",
						new String[]{}, DEBUG);
				Application.db.sync(new ArrayList<String>(Arrays.asList(table.justification.TNAME,table.signature.TNAME)));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
       	if (cmd == J_ANSWER) {
        	if (DEBUG) System.out.println("JustificationsCAction: start ANSWER "+model.crt_choice);
    		long cID = tree.getConstituentIDMyself();
    		if (cID <= 0) return;
    		String jLIDstr = model.getJustificationID(row);
        	long j_ID = Util.lval(jLIDstr, -1);

        	//D_Justification.createAnswerTo(tree, jLIDstr);
        	new util.DDP2P_ServiceThread(__("Answer Justification"), false, model.getJustificationID(row)) {
    			@Override
    			public void _run() {
        			//Justifications tree = (Justifications) ctx;
        			//JustificationsModel model = tree.getModel();
    	        	long j_ID = Util.lval(ctx, -1);

		        	D_Justification answered_justification = D_Justification.getJustByLID(j_ID, true, false);
		        		
		        	
		        	D_Justification answering_justification = D_Justification.getAnsweringDefaultTemporaryStoredRegistered(answered_justification);
		        			//D_Justification.getEmpty();
		        	long nID = answering_justification.getLID();
		        	/*
		        	answering_justification.loadRemote(answered_justification, null, null, null);
		        	answering_justification.changeToDefaultAnswer();
		        	//answering_justification.setConstituentLIDstr(Util.getStringID(cID));
		        	//answering_justification.setConstituentGID(tree.getConstituentGIDMyself());
		        	
		        	answering_justification.setTemporary(true);
		        	answering_justification.setGID(null);
		        	answering_justification.setLIDstr(null);
		         	// nID = answering_justification.storeLinkNewTemporary(); //storeVerified();
		        	nID = answering_justification.storeSynchronouslyNoException();
		        	//answering_justification.releaseReference();
		        	*/
		        		
		        	if (DEBUG) System.out.println("JustificationsCAction: got id="+nID+" for " + answering_justification);
		        	SwingUtilities.invokeLater(new util.DDP2P_ServiceRunnable(__("Select Answering"), false, false, Long.valueOf(nID)) {
		        		public void _run () {
		        			Long nID = (Long) ctx;
		           			JustificationsModel model = tree.getModel();
		        			if (model.crt_choice == null) tree.setCurrentJust(nID); // will be visible only if everything is visible
		        		}
		        	});
		        	if (DEBUG) System.out.println("JustificationsCAction: fire=" + nID);
		        	
		        	tree.fireListener(Util.getStringID(nID), 0, false);
        		}
        	}.start();
        	//DD.tabbedPane.setSelectedComponent(DD.jscj);
        	MainFrame.tabbedPane.setSelectedIndex(MainFrame.TAB_JUSTS_);
       	}
    }
}


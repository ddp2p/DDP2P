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

import static util.Util._;

import hds.DebateDecideAction;

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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import util.P2PDDSQLException;

import util.DBInterface;
import util.Util;
//import widgets.org.ColorRenderer;
import widgets.components.DocumentTitleRenderer;
import widgets.justifications.JustificationsModel;
import widgets.motions.Motions;
import widgets.motions.MotionsModel;
import config.Application;
import config.DD;
import config.DDIcons;
//import config.DDIcons;
import config.Identity;
import data.D_Document_Title;
import data.D_Justification;
import data.D_Motion;


@SuppressWarnings("serial")
public class Justifications extends JTable implements MouseListener, JustificationsListener  {
	private int DIM_X = 1000;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = 4;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private DocumentTitleRenderer titleRenderer;
	DefaultTableCellRenderer centerRenderer;
	public Justifications(int dim_x) {
		super(new JustificationsModel(Application.db));
		DIM_X = dim_x;
		init();
	}
	public Justifications() {
		super(new JustificationsModel(Application.db));
		init();
	}
	public Justifications(DBInterface _db) {
		super(new JustificationsModel(_db));
		init();
	}
	public Justifications(JustificationsModel dm) {
		super(dm);
		init();
	}
	public void connectWidget() {
		getModel().connectWidget();
		
		DD.status.addMotionStatusListener(this.getModel());
    	if(_jedit != null) DD.status.addJustificationStatusListener(_jedit);
    	DD.status.addJustificationStatusListener(this);
	}
	public void disconnectWidget() {
		getModel().disconnectWidget();

		DD.status.removeMotListener(this.getModel());
    	if(_jedit != null) DD.status.removeJustificationListener(_jedit);
    	DD.status.removeJustificationListener(this);
	}
	JustificationEditor _jedit = null;
	//JPanel
	Component just_panel = null;
	public Component getComboPanel() {
		if(just_panel != null) return just_panel;
		if(DEBUG) System.out.println("createAndShowGUI: added justif");
    	if(_jedit == null) _jedit = new JustificationEditor();
		if(DEBUG) System.out.println("createAndShowGUI: added justif editor");
    	just_panel = DD.makeJustificationPanel(_jedit, this);
    	//javax.swing.JScrollPane jscj = new javax.swing.JScrollPane(just_panel);
		//tabbedPane.addTab("Justifications", jscj);
    	//JScrollPane just = justifications.getScrollPane();
		//tabbedPane.addTab("Justifications", just);
		//tabbedPane.addTab("Justification", _jedit.getScrollPane());
		if(DEBUG) System.out.println("createAndShowGUI: justif pane");
		this.addListener(DD.status);
		DD.status.addJustificationStatusListener(_jedit);
    	return just_panel;//jscj;
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		titleRenderer = new DocumentTitleRenderer();
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		//this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		

		Comparator<D_Document_Title> documentTitleComparator = new java.util.Comparator<D_Document_Title>() {

			//@Override
			public int _compare(Object o1, Object o2) {
				if(o1==null) return 1;
				if(o2==null) return -1;
				String s1 = Util.getString(o1), s2 = Util.getString(o2);
				if(o1 instanceof data.D_Document_Title) s1 = ((data.D_Document_Title)o1).title_document.getDocumentUTFString();
				if(o2 instanceof data.D_Document_Title) s2 = ((data.D_Document_Title)o2).title_document.getDocumentUTFString();
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

		
    	try{
    		if (Identity.getCurrentIdentity().identity_id!=null) {
    			//long id = new Integer(Identity.current.identity_id).longValue();
    			long orgID = Identity.getDefaultOrgID();
    			this.setCurrentJust(orgID);
    			int row =this.getSelectedRow();
     			this.fireListener(row, A_NON_FORCE_COL, true);
    		}
    	}catch(Exception e){e.printStackTrace();}
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

	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == getModel().TABLE_COL_NAME)) return titleRenderer;
		if ((column == getModel().TABLE_COL_VOTERS_NB)) return centerRenderer;
		//if ((column == JustificationsModel.TABLE_COL_CONNECTION)) return bulletRenderer;
//		if (column >= JustificationsModel.TABLE_COL_PLUGINS) {
//			int plug = column-JustificationsModel.TABLE_COL_PLUGINS;
//			if(plug < plugins.size()) {
//				String pluginID= plugins.get(plug);
//				return plugin_applets.get(pluginID).renderer;
//			}
//		}
		return super.getCellRenderer(row, column);
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
		if(row<0) id = null;
		else id = Util.getString(this.getModel()._justifications[this.convertRowIndexToModel(row)]);
		fireListener(id, col, db_sync);
		if(DEBUG) System.out.println("Justifications:fireListener:choice="+getModel().crt_choice+"  id="+id);
	}
	void fireListener(String id, int col, boolean db_sync) {
   	   	if(DEBUG) System.out.println("Justifications: fire justID="+id);
   	   	D_Justification just = null;
   	   	if(id != null)
			try {
				just = new D_Justification(Util.lval(id));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		for(JustificationsListener l: listeners){
			if(DEBUG) System.out.println("Justifications:fireListener: l="+l);
			try{
				l.justUpdate(id, col, db_sync, just);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addListener(JustificationsListener l){
		listeners.add(l);
		int row =this.getSelectedRow();
		if(row>=0)
			l.justUpdate(Util.getString(this.getModel()._justifications[this.convertRowIndexToModel(row)]),A_NON_FORCE_COL, false, null);
	}
	public void removeListener(JustificationsListener l){
		listeners.remove(l);
	}

	public void setCurrentJust(long just_id) {
   		if(DEBUG) System.out.println("Justifications:setCurrentJust: got id="+just_id);
		getModel().setCurrentJust(just_id);
	}
	public long getConstituentIDMyself(){
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
   	   		String orgID = Util.getString(model._justifications[model_row]);
   	   		try{
   	   			long oID = new Integer(orgID).longValue();
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
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	
    	JPopupMenu popup = new JPopupMenu();
    	JustificationsCustomAction aAction;
    	
    	aAction = new JustificationsCustomAction(this, _("Answer!"), addicon,_("Answer this."), _("Answer"),KeyEvent.VK_A, JustificationsCustomAction.J_ANSWER);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, _("Delete!"), delicon,_("Delete this."), _("Delete"),KeyEvent.VK_D, JustificationsCustomAction.J_DEL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, _("Remove Answering Filter!"), delicon,_("Remove Answering Filter."), _("Remove Answering Filter"),KeyEvent.VK_R, JustificationsCustomAction.J_REM_ANSWER);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, _("Filter by Answering This!"), delicon,_("Filter by Answering This."), _("Filter by Answering This"),KeyEvent.VK_T, JustificationsCustomAction.J_ANSWER_THIS);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new JustificationsCustomAction(this, _("Delete Partial!"), delicon,_("Delete partial."), _("Delete partial"),KeyEvent.VK_P, JustificationsCustomAction.J_DEL_PARTIAL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	return popup;
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
    /**
     * called from DD.status backward/forward
     */
	@Override
	public void justUpdate(String justID, int col, boolean db_sync,
			D_Justification just) {
		if(justID==null) return;
		int model_row = getModel().getRow(justID); //getRowByID(Util.lval(justID));
		if(model_row<0) return;
		int view_row = this.convertRowIndexToView(model_row);
		this.setRowSelectionInterval(view_row, view_row);
		
		this.fireListener(justID, col, db_sync);
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
    	if(cmd == J_ANSWER_THIS) {
    		model.setCrtAnswered(model.getJustificationID(row));
    	}
        if(cmd == J_REM_ANSWER) {
    		model.setCrtAnswered(null);        	
        }
        if(cmd == J_DEL) {
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
       	if(cmd == J_ANSWER) {
        	if(DEBUG) System.out.println("JustificationsCAction: start ANSWER "+model.crt_choice);
    		long cID = tree.getConstituentIDMyself();
    		if(cID<=0) return;
    		D_Justification n_justification;
        	long j_ID = Util.lval(model.getJustificationID(row), -1);
        	try {
        		n_justification = new D_Justification(j_ID);
        		n_justification.changeToDefaultAnswer();
        		//n_justification.motion_title.title_document.setDocumentString(_("Newly added justification"));
        		n_justification.constituent_ID = Util.getStringID(cID);
         		n_justification.global_constituent_ID = tree.getConstituentGIDMyself();
         		//n_justification.organization_ID = tree.getOrganizationID();
        		//n_justification.creation_date = Util.CalendargetInstance();
        		long nID;
        		nID = n_justification.storeVerified();
        		if(DEBUG) System.out.println("JustificationsCAction: got id="+nID);
        		if(model.crt_choice==null)tree.setCurrentJust(nID); // will be visible only if everything is visible
        		if(DEBUG) System.out.println("JustificationsCAction: fire="+nID);
        		tree.fireListener(Util.getStringID(nID), 0, false);
        		//DD.tabbedPane.setSelectedComponent(DD.jscj);
        		DD.tabbedPane.setSelectedIndex(DD.TAB_JUSTS_);
			} catch (P2PDDSQLException e2) {
				e2.printStackTrace();
			}
    	}
    }
}


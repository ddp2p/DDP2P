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
package widgets.motions;

import static util.Util._;

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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import util.P2PDDSQLException;
import streaming.RequestData;
import util.DBInterface;
import util.Util;
import widgets.app.DDIcons;
import widgets.app.MainFrame;
import widgets.components.BulletRenderer;
import widgets.components.DebateDecideAction;
//import widgets.org.ColorRenderer;
import widgets.components.DocumentTitleRenderer;
import widgets.motions.MotionsModel;
import widgets.org.Orgs;
import wireless.BroadcastClient;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.MotionsListener;
//import config.DDIcons;
import config.Identity;
import data.D_Document_Title;
import data.D_Justification;
import data.D_Motion;
@SuppressWarnings("serial")
public class Motions extends JTable implements MouseListener, MotionsListener  {
	private static final int DIM_X = 1000;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = 4;
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private DocumentTitleRenderer titleRenderer;
	BulletRenderer hotRenderer;
	DefaultTableCellRenderer centerRenderer;
	public Motions(DBInterface _db) {
		super(new MotionsModel(_db));
		init();
	}
	public Motions(MotionsModel dm) {
		super(dm);
		init();
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
	public Motions() {
		super(new MotionsModel(Application.db));
		init();
	}
	void init(){
		if(DEBUG) System.out.println("Motions: init: start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		titleRenderer = new DocumentTitleRenderer();
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		hotRenderer = new BulletRenderer(
				DDIcons.getHotImageIcon("Hot"), DDIcons.getHotGImageIcon("Hot"),
				null, _("Recently Contacted"),  _("Not Recently Contacted"), null);
		if(DEBUG) System.out.println("ThreadsView: constr from model");
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));

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
		TableRowSorter<MotionsModel> sorter = new TableRowSorter<MotionsModel>();
		this.setRowSorter(sorter);
		sorter.setModel(getModel());
		sorter.setComparator(MotionsModel.TABLE_COL_NAME, documentTitleComparator);
		this.getRowSorter().toggleSortOrder(MotionsModel.TABLE_COL_ARRIVAL_DATE);
		this.getRowSorter().toggleSortOrder(MotionsModel.TABLE_COL_ARRIVAL_DATE);
		
		DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel headerLabel = (JLabel)
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Icon icon = Motions.this.getModel().getIcon(column);
				if(icon != null)  headerLabel.setText(null);
				headerLabel.setIcon(icon);
			    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			    setHorizontalAlignment(JLabel.CENTER);
			    return headerLabel;
			}
		};
		
		//getTableHeader().setDefaultRenderer(rend);
		for(int col_index = 0; col_index < getModel().getColumnCount(); col_index++) {
			if(getModel().getIcon(col_index) != null)
				getTableHeader().getColumnModel().getColumn(col_index).setHeaderRenderer(rend);
		}
		
    	try{
    		if (Identity.getCurrentIdentity().identity_id!=null) {
    			//long id = new Integer(Identity.current.identity_id).longValue();
    			long orgID = Identity.getDefaultOrgID();
    			this.setCurrent(orgID);
    			int row =this.getSelectedRow();
     			this.fireListener(row, A_NON_FORCE_COL);
    		}
    	}catch(Exception e){e.printStackTrace();}
		if(DEBUG) System.out.println("Motions: init: End");
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
		//boolean DEBUG=true;
        if(DEBUG) System.out.println("Motions:getCellRenderer row="+row+" col="+column);

		switch(column){
		case MotionsModel.TABLE_COL_NAME: //))
			{
				//if(DEBUG) System.out.println("Motions:getCellRenderer ="+titleRenderer);
				return titleRenderer;
			}
		
		// Integers
		case MotionsModel.TABLE_COL_VOTERS_NB: // )) return centerRenderer;
		case MotionsModel.TABLE_COL_ACTIVITY: // )) return centerRenderer;
		case MotionsModel.TABLE_COL_NEWS: // )) return centerRenderer;
			return centerRenderer;
		case MotionsModel.TABLE_COL_RECENT: // return Boolean.class;
			return hotRenderer;
		// BOOLEANS
		//case MotionsModel.TABLE_COL_VOTERS_NB: // return Integer.class;
		//case MotionsModel.TABLE_COL_ACTIVITY: // return Integer.class;
		//case MotionsModel.TABLE_COL_RECENT: // return Boolean.class;
		//case MotionsModel.TABLE_COL_NEWS: // return Integer.class;
		case MotionsModel.TABLE_COL_BROADCASTED: // ) return Boolean.class;
		case MotionsModel.TABLE_COL_BLOCKED: // ) return Boolean.class;
		case MotionsModel.TABLE_COL_TMP: // ) return Boolean.class;
		case MotionsModel.TABLE_COL_GID_VALID: // ) return Boolean.class;
		case MotionsModel.TABLE_COL_SIGN_VALID: // ) return Boolean.class;
		case MotionsModel.TABLE_COL_HIDDEN: //) return Boolean.class;
			return super.getCellRenderer(row, column);
			
		case MotionsModel.TABLE_COL_CREATOR: // )) return defaultTableCellRenderer;
		case MotionsModel.TABLE_COL_CATEGORY: // )) return defaultTableCellRenderer;
		case MotionsModel.TABLE_COL_CREATION_DATE: // )) return defaultTableCellRenderer;
		default:
			TableCellRenderer result = defaultTableCellRenderer;//super.getCellRenderer(row, column);
	        if(DEBUG) System.out.println("Motions:getCellRenderer default="+result);
			return result;
		}
		
		//if ((column == MotionsModel.TABLE_COL_CONNECTION)) return bulletRenderer;
//		if (column >= MotionsModel.TABLE_COL_PLUGINS) {
//			int plug = column-MotionsModel.TABLE_COL_PLUGINS;
//			if(plug < plugins.size()) {
//				String pluginID= plugins.get(plug);
//				return plugin_applets.get(pluginID).renderer;
//			}
//		}		
	}
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= MotionsModel.columnToolTips.length) return null;
				return MotionsModel.columnToolTips[realIndex];
            }
        };
    }
	public MotionsModel getModel(){
		return (MotionsModel) super.getModel();
	}
	public void initColumnSizes() {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("Motions:initColumnSizes started");
        MotionsModel model = (MotionsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        int columns = model.getColumnCount();
        //if(DEBUG) System.out.println("Motions:initColumnSizes cnt="+columns);
        for (int i = 0; i < columns; i++) {
            //if(DEBUG) System.out.println("Motions:initColumnSizes i="+i);
        	headerWidth = 0;
        	cellWidth = 0;
            column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            int rows = model.getRowCount();
            for(int r=0; r<rows; r++) {
                //if(DEBUG) System.out.println("Motions:initColumnSizes r="+r);
            	Object val = getValueAt(r, i);
                //if(DEBUG) System.out.println("Motions:initColumnSizes val="+val);
                Class<?> o = model.getColumnClass(i);
                //if(DEBUG) System.out.println("Motions:initColumnSizes class="+o);
                TableCellRenderer rend = this.getDefaultRenderer(o);
                //if(DEBUG) System.out.println("Motions:initColumnSizes rend="+rend);
            	comp = rend.getTableCellRendererComponent(
                                 this, val,
                                 false, false, 0, i);
                //if(DEBUG) System.out.println("Motions:initColumnSizes comp="+comp);
            	int this_width = comp.getPreferredSize().width;
                //if(DEBUG) System.out.println("Motions:initColumnSizes w="+this_width);
            	cellWidth = Math.max(this_width, cellWidth);
        		if(DEBUG) System.out.println("Motions:iniColSz:"+i+" row="+cellWidth+" this="+cellWidth+" val="+val);
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

	ArrayList<MotionsListener> listeners=new ArrayList<MotionsListener>();
	public void fireForceEdit(String orgID) {		
		ArrayList<MotionsListener> _listeners = listeners;
		if(DEBUG) System.out.println("Motions:fireForceEdit: row="+orgID);
		for(MotionsListener l: _listeners){
			if(DEBUG) System.out.println("Motions:fireForceEdit: l="+l);
			try{
				if(orgID==null) ;//l.forceEdit(orgID);
				else l.motion_forceEdit(orgID);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	void fireListener(D_Motion crt_mot, String motion_ID, int col) {
		ArrayList<MotionsListener> _listeners = listeners;
		for(MotionsListener l: _listeners){
			if(DEBUG) System.out.println("Motions:fireListener: l="+l);
			try{
				l.motion_update(motion_ID, col, crt_mot);
			}catch(Exception e){e.printStackTrace();}
		}
		if(DEBUG) System.out.println("\n************\nMotions:fireListener: Done");
	}
	void fireListener(int table_row, int col) {
		if(DEBUG) System.out.println("Motions:fireListener: row="+table_row);
		String motion_ID;
		D_Motion crt_mot;
		if(table_row<0) {
			motion_ID = null;
			crt_mot = null;
		}else{
			motion_ID = this.getModel().getMotionID(this.convertRowIndexToModel(table_row));
			long _motion_ID = new Integer(motion_ID).longValue();
			try {
				crt_mot = new D_Motion(_motion_ID);
			} catch (Exception e) {
				e.printStackTrace();
				motion_ID = null;
				crt_mot = null;				
			}
		}
		fireListener(crt_mot, motion_ID, col);
		if(DEBUG) System.out.println("\n************\nMotions:fireListener: Done");
	}
	public void addListener(MotionsListener l){
		if(DEBUG) System.out.println("\n************\nMotions:addListener: start");
		if(listeners.contains(l)) return;
		listeners = new ArrayList<MotionsListener>(listeners);
		listeners.add(l);
		int row =this.getSelectedRow();
		if(row>=0) {
			if(DEBUG) System.out.println("Motions:addListener: row="+row);
			l.motion_update(this.getModel().getMotionID(this.convertRowIndexToModel(row)),A_NON_FORCE_COL, null);
		}
		if(DEBUG) System.out.println("\n************\nMotions:addListener: Done");
	}
	public void removeListener(MotionsListener l){
		if(!listeners.contains(l)) return;
		listeners = new ArrayList<MotionsListener>(listeners);
		listeners.remove(l);
	}

	public void setCurrent(long motion_id) {
		getModel().setCurrent(motion_id);
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
        
    	MotionsModel model = (MotionsModel)getModel();
 		int model_row=convertRowIndexToModel(row);
   	   	if(model_row>=0) {
   	   		String motID = Util.getString(model._motions[model_row]);
   	   		try{
   	   			long mID = new Integer(motID).longValue();
   	   			model.setCurrent(mID);
   	   		}catch(Exception e){};
   	   	}
        
        fireListener(row,col);
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
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));

    	JPopupMenu popup = new JPopupMenu();
    	MotionCustomAction aAction;
    	
    	aAction = new MotionCustomAction(this, _("Add!"), addicon,_("Add new motion."), _("Add"),KeyEvent.VK_A, MotionCustomAction.M_ADD);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);

    	aAction = new MotionCustomAction(this, _("Advertise!"), addicon,_("Advertise this."), _("Advertise"),KeyEvent.VK_S, MotionCustomAction.M_ADVERTISE);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	

    	if(MotionsModel.hide)
    		aAction = new MotionCustomAction(this, _("Show Hidden!"), addicon,_("Show Hidden."), _("Show Hidden"),KeyEvent.VK_H, MotionCustomAction.M_TOGGLE_HIDE);
    	else
    		aAction = new MotionCustomAction(this, _("Hide!"), addicon,_("Hide this."), _("Hide"),KeyEvent.VK_H, MotionCustomAction.M_TOGGLE_HIDE);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	

    	aAction = new MotionCustomAction(this, _("WLAN_Request!"), addicon,_("Request this on WLAN."), _("Request"),KeyEvent.VK_R, MotionCustomAction.M_WLAN_REQUEST);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	

    	aAction = new MotionCustomAction(this, _("Enhance!"), addicon,_("Enhance this."), _("Enhance"),KeyEvent.VK_E, MotionCustomAction.M_ENHANCE);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	
    	aAction = new MotionCustomAction(this, _("Delete!"), delicon,_("Delete this."), _("Delete"),KeyEvent.VK_D, MotionCustomAction.M_DEL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, _("Remove Enhancing Filter!"), delicon,_("Remove Enhancing Filter."), _("Remove Enhancing Filter"),KeyEvent.VK_R, MotionCustomAction.M_REM_ENHANCING);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, _("Filter by Enhancing This!"), delicon,_("Filter by Enhancing This."), _("Filter by Enhancing This"),KeyEvent.VK_T, MotionCustomAction.M_ENHANCING_THIS);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, _("Delete Partial!"), delicon,_("Delete partial."), _("Delete partial"),KeyEvent.VK_P, MotionCustomAction.M_DEL_PARTIAL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, _("Adjust Column Size!"), delicon,_("Adjust Column Size."), _("Adjust Column Size"),KeyEvent.VK_C, MotionCustomAction.M_REDRAW);
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
    Component _medit = null;
    Component motion_panel = null;
    TemporaryMotionsEditor tmpeditor = null;
	public void connectWidget() {
		getModel().connectWidget();
	    MainFrame.status.addOrgStatusListener(getModel());
	    MainFrame.status.addMotionStatusListener(this);
	}
	public void disconnectWidget() {
		getModel().disconnectWidget();
		MainFrame.status.removeOrgListener(this.getModel());
		MainFrame.status.removeMotListener(this);
	}
	public Component getComboPanel() {
		Component editor = _medit;
		if(motion_panel != null) return motion_panel;
	   	// Initialize widgets
			
		Motions motions = this; //new widgets.motions.Motions();
		if(DEBUG) System.out.println("Motions.getComboPanel: motions located");
			
	    //orgsPane.addOrgListener(motions.getModel());
	    if(_medit == null) {
			if(DEBUG) System.out.println("Motions.getComboPanel: created Editor");
	    	//editor = _medit = new MotionEditor();
	    	//motions.addListener((MotionsListener)_medit);
	    	
	    	editor = new JPanel();
	    	
	    	if(tmpeditor == null) tmpeditor = new TemporaryMotionsEditor(motions);
	    	motions.addListener(tmpeditor);
			if(DEBUG) System.out.println("Motions:getComboPanel: added mot tmpeditor");
			
			
	    }else{
	    	motions.addListener((MotionsListener)_medit);
			if(DEBUG) System.out.println("Motions.getComboPanel: motion editor listens to me");
	    }
	    //motions.addListener(justifications.getModel());
	    //motions.addListener(_jbc);
	    motions.addListener(MainFrame.status);
		if(DEBUG) System.out.println("Motions.getComboPanel: status listens to me");
	    	
	    	   	
	    motion_panel = MainFrame.makeMotionPanel(editor, motions);
	    //DD.jscm = motion_panel; //new javax.swing.JScrollPane(motion_panel);
	    	//tabbedPane.addTab("Motions", jscm);
		if(DEBUG) System.out.println("Motions.getComboPanel: added motions panel");
		
		return motion_panel; //DD.jscm;
	}
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if(motID==null) return;
		int model_row = getModel().getRow(motID);
		if(model_row<0) return;
		int view_row = this.convertRowIndexToView(model_row);
		this.setRowSelectionInterval(view_row, view_row);
		
		this.fireListener(d_motion, motID, col);
	}
	@Override
	public void motion_forceEdit(String motID) {
		// TODO Auto-generated method stub
		
	}
}
@SuppressWarnings("serial")
class MotionCustomAction extends DebateDecideAction {
	public static final int M_ADD = 1;
    public static final int M_DEL = 2;
	public static final int M_REM_ENHANCING = 3;
	public static final int M_ENHANCING_THIS = 4;
	public static final int M_DEL_PARTIAL = 5;
	public static final int M_ENHANCE = 6;
	public static final int M_ADVERTISE = 7;
	public static final int M_WLAN_REQUEST = 8;
	public static final int M_TOGGLE_HIDE = 9;
	public static final int M_REDRAW = 10;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Motions tree; ImageIcon icon; int cmd;
    public MotionCustomAction(Motions tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("MotionCAction: start");
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
    	MotionsModel model = (MotionsModel)tree.getModel();
    	
    	if(DEBUG) System.out.println("MotionCAction: row = "+row);
    	//do_cmd(row, cmd);
    	if(cmd == M_WLAN_REQUEST) {
    		if(DEBUG) System.out.println("Motions:MotionCustomAction:WLANRequest: start");
    		String _m_GID = model.getMotionGID(row);
    		if(_m_GID==null){
    			if(DEBUG) System.out.println("Morions:MotionCustomAction:WLANRequest: null GID");
        		return;
    		}
    		if(DEBUG) System.out.println("Morions:MotionCustomAction:WLANRequest: GID: "+_m_GID);
    		RequestData rq = new RequestData();;
    		
			try {
				String interests = DD.getAppText(DD.WLAN_INTERESTS);
				if(interests != null){
					byte[] wlan_interests = Util.byteSignatureFromString(interests);
					rq = rq.decode(new ASN1.Decoder(wlan_interests));
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if(!rq.moti.contains(_m_GID)) {
				rq.moti.add(_m_GID);
				if(BroadcastClient.msgs == null){
					System.out.println("Motions:MotionCustomAction:WLANRequest: empty messages queue!");
				}else
					BroadcastClient.msgs.registerRequest(rq);
				if(DEBUG) System.out.println("Morions:MotionCustomAction:WLANRequest: added GID: "+_m_GID);
			}
			
			byte[] intr = rq.getEncoder().getBytes();
			try {
				DD.setAppText(DD.WLAN_INTERESTS, Util.stringSignatureFromByte(intr));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			if(DEBUG) System.out.println("Motions:MotionCustomAction:WLANRequest: done ");
    	}
    	if(cmd == M_REDRAW){
    		tree.initColumnSizes();
    	}
        if(cmd == M_DEL) {
    		String _m_ID = model.getMotionID(row);
    		if(_m_ID == null) return;
    		try {
				Application.db.delete(table.justification.TNAME,
						new String[]{table.justification.motion_ID},
						new String[]{_m_ID}, DEBUG);
				Application.db.delete(table.signature.TNAME,
						new String[]{table.signature.motion_ID},
						new String[]{_m_ID}, DEBUG);
				Application.db.delete(table.motion.TNAME,
						new String[]{table.motion.motion_ID},
						new String[]{_m_ID}, DEBUG);
				Application.db.delete(table.news.TNAME,
						new String[]{table.news.motion_ID},
						new String[]{_m_ID}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    	if(cmd == M_DEL_PARTIAL) {
    		try {
				Application.db.delete(
						"DELETE FROM "+table.motion.TNAME+
						" WHERE "+table.motion.signature+" IS NULL OR "+table.motion.global_motion_ID+" IS NULL",
						new String[]{}, DEBUG);
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
    	long constID = tree.getConstituentIDMyself();
    	String gID = tree.getConstituentGIDMyself();
     	if((constID<=0)||(gID==null)){
    			Application_GUI.warning(_("No constituent selected. First select a constituent for this org"), _("First register a constituent!"));
    			return;
    	}
   	
    	if(cmd == M_ADD){
        	if(DEBUG) System.out.println("MotionCAction: start ADD");
        	D_Motion n_motion = new D_Motion();
    		n_motion.motion_title.title_document.setDocumentString(_("Newly added motion"));
    		long cID = constID;
    		n_motion.global_constituent_ID = gID;
    		n_motion.constituent_ID = Util.getStringID(cID);
    		n_motion.organization_ID = tree.getOrganizationID();
    		n_motion.creation_date = Util.CalendargetInstance();
    		long nID;
			try {
				nID = n_motion.storeVerified();
	        	if(DEBUG) System.out.println("MotionCAction: got id="+nID);
				tree.setCurrent(nID);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    	if(cmd == M_ADVERTISE) {
    		model.advertise(row);
    	}
    	if(cmd == M_TOGGLE_HIDE) {
    		MotionsModel.hide = !MotionsModel.hide;
    	}
    	if(cmd == M_ENHANCING_THIS) {
    		model.setCrtEnhanced(model.getMotionID(row));
    	}
        if(cmd == M_REM_ENHANCING) {
    		model.setCrtEnhanced(null);        	
        }
       	if(cmd == M_ENHANCE) {
        	if(DEBUG) System.out.println("MotionsCAction: start ANSWER "+model.crt_enhanced);
    		long cID = tree.getConstituentIDMyself();
    		if(cID<=0) return;
    		D_Motion n_motion;
        	long m_ID = Util.lval(model.getMotionID(row), -1);
        	try {
        		n_motion = new D_Motion(m_ID);
        		n_motion.changeToDefaultEnhancement();
        		//n_justification.motion_title.title_document.setDocumentString(_("Newly added justification"));
        		n_motion.constituent_ID = Util.getStringID(cID);
         		n_motion.global_constituent_ID = tree.getConstituentGIDMyself();
         		//n_justification.organization_ID = tree.getOrganizationID();
        		//n_justification.creation_date = Util.CalendargetInstance();
        		long nID;
        		nID = n_motion.storeVerified();
        		if(DEBUG) System.out.println("MotionCAction: got id="+nID);
        		model.crt_enhanced=null;
        		tree.setCurrent(nID);
        		if(DEBUG) System.out.println("MotionCAction: fire="+nID);
        		tree.fireListener(n_motion, ""+nID, 0);
 			} catch (P2PDDSQLException e2) {
				e2.printStackTrace();
			}
    	}
    }
}

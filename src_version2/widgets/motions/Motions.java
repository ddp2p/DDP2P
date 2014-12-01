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

import static util.Util.__;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import ciphersuits.KeyManagement;
import ciphersuits.PK;
import ciphersuits.SK;
import util.P2PDDSQLException;
import streaming.RequestData;
import util.DBInterface;
import util.DD_SK;
import util.Util;
import widgets.app.DDIcons;
import widgets.app.MainFrame;
import widgets.app.Util_GUI;
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
import data.D_Constituent;
import data.D_Document_Title;
import data.D_Justification;
import data.D_Motion;
import data.D_Neighborhood;
import data.D_Organization;
import data.D_Peer;
import data.D_Vote;
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
				null, __("Recently Contacted"),  __("Not Recently Contacted"), null);
		if(DEBUG) System.out.println("ThreadsView: constr from model");
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
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
		for (int col_index = 0; col_index < getModel().getColumnCount(); col_index ++) {
			if (getModel().getIcon(col_index) != null)
				getTableHeader().getColumnModel().getColumn(col_index).setHeaderRenderer(rend);
		}
		
//    	try{
//    		if (Identity.getCurrentIdentity().identity_id!=null) {
//    			//long id = new Integer(Identity.current.identity_id).longValue();
//    			long orgID = Identity.getDefaultOrgID();
//    			this.setCurrent(orgID);
//    			int row =this.getSelectedRow();
//     			this.fireListener(row, A_NON_FORCE_COL);
//    		}
//    	}catch(Exception e){e.printStackTrace();}
		if (DEBUG) System.out.println("Motions: init: End");
	}
	JScrollPane scrollPane = null;
	public JScrollPane getScrollPane() {
        scrollPane = new JScrollPane(this);
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
		for (MotionsListener l: _listeners){
			if (DEBUG) System.out.println("Motions:fireListener: l="+l);
			try {
				l.motion_update(motion_ID, col, crt_mot);
			} catch(Exception e){e.printStackTrace();}
		}
		if (DEBUG) System.out.println("\n************\nMotions:fireListener: Done");
	}
	/**
	 * Received view_row
	 * @param table_row
	 * @param col
	 */
	void fireListener(int table_row, int col) {
		if (DEBUG) System.out.println("Motions:fireListener2: row="+table_row);
		String motion_ID;
		D_Motion crt_mot;
		if (table_row < 0) {
			motion_ID = null;
			crt_mot = null;
		} else {
			motion_ID = this.getModel().getMotionIDstr(this.convertRowIndexToModel(table_row));
			crt_mot = D_Motion.getMotiByLID(motion_ID, true, false);
			if (crt_mot == null)
				motion_ID = null;
		}
		fireListener(crt_mot, motion_ID, col);
		if (DEBUG) System.out.println("\n************\nMotions:fireListener2: Done");
	}
	public void addListener(MotionsListener l){
		if(DEBUG) System.out.println("\n************\nMotions:addListener: start");
		if(listeners.contains(l)) return;
		listeners = new ArrayList<MotionsListener>(listeners);
		listeners.add(l);
		int row =this.getSelectedRow();
		if(row>=0) {
			if(DEBUG) System.out.println("Motions:addListener: row="+row);
			l.motion_update(this.getModel().getMotionIDstr(this.convertRowIndexToModel(row)),A_NON_FORCE_COL, null);
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
		// boolean DEBUG = true;
    	int view_row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	//if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        view_row = this.rowAtPoint(point);
        col = this.columnAtPoint(point);
        if ((view_row < 0) || (col < 0 )) return;
        
		if (DEBUG) System.out.println("Motions: mouseClicked: row=" + view_row);
    	MotionsModel model = (MotionsModel)getModel();
 		int model_row = convertRowIndexToModel(view_row);
 		if (DEBUG) System.out.println("Motions: mouseClicked: model_row="+model_row);
   	   	if (model_row >= 0) {
   	   		long motID = model.getMotionID(model_row); //Util.getString(model._motions[model_row]);
   			if (DEBUG) System.out.println("Motions: mouseClicked: mot ID =" + motID);
  	   		try {
   	   			//long mID = Util.lval(motID);
   	   			model.setCurrent(motID);
   	   		} catch(Exception e){};
   	   	}
        
        fireListener(view_row,col);
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
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));

    	JPopupMenu popup = new JPopupMenu();
    	MotionCustomAction aAction;
    	
    	aAction = new MotionCustomAction(this, __("Add!"), addicon,__("Add new motion."), __("Add"),KeyEvent.VK_A, MotionCustomAction.M_ADD);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);

    	aAction = new MotionCustomAction(this, __("Advertise!"), addicon,__("Advertise this."), __("Advertise"),KeyEvent.VK_S, MotionCustomAction.M_ADVERTISE);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	

    	aAction = new MotionCustomAction(this, __("Export!"), addicon,__("Export."), __("Export"), KeyEvent.VK_X, MotionCustomAction.M_EXPORT);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	

    	if(MotionsModel.hide)
    		aAction = new MotionCustomAction(this, __("Show Hidden!"), addicon,__("Show Hidden."), __("Show Hidden"),KeyEvent.VK_H, MotionCustomAction.M_TOGGLE_HIDE);
    	else
    		aAction = new MotionCustomAction(this, __("Hide!"), addicon,__("Hide this."), __("Hide"),KeyEvent.VK_H, MotionCustomAction.M_TOGGLE_HIDE);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	

    	aAction = new MotionCustomAction(this, __("WLAN_Request!"), addicon,__("Request this on WLAN."), __("Request"),KeyEvent.VK_R, MotionCustomAction.M_WLAN_REQUEST);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	

    	aAction = new MotionCustomAction(this, __("Enhance!"), addicon,__("Enhance this."), __("Enhance"),KeyEvent.VK_E, MotionCustomAction.M_ENHANCE);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	
    	aAction = new MotionCustomAction(this, __("Delete!"), delicon,__("Delete this."), __("Delete"),KeyEvent.VK_D, MotionCustomAction.M_DEL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, __("Remove Enhancing Filter!"), delicon,__("Remove Enhancing Filter."), __("Remove Enhancing Filter"),KeyEvent.VK_R, MotionCustomAction.M_REM_ENHANCING);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, __("Filter by Enhancing This!"), delicon,__("Filter by Enhancing This."), __("Filter by Enhancing This"),KeyEvent.VK_T, MotionCustomAction.M_ENHANCING_THIS);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
    	
    	aAction = new MotionCustomAction(this, __("Delete Partial!"), delicon,__("Delete partial."), __("Delete partial"),KeyEvent.VK_P, MotionCustomAction.M_DEL_PARTIAL);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	
    	aAction = new MotionCustomAction(this, __("Adjust Column Size!"), delicon,__("Adjust Column Size."), __("Adjust Column Size"),KeyEvent.VK_C, MotionCustomAction.M_REDRAW);
    	aAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);    	
       	
    	aAction = new MotionCustomAction(this, __("Refresh Cache!"), delicon,__("Refresh Cache."), __("Refresh Cache"),KeyEvent.VK_F, MotionCustomAction.M_REFRESH);
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
		//boolean DEBUG = true;
		Component editor = _medit;
		if (DEBUG) {
			System.out.println("Motions.getComboPanel: start");
			Util.printCallPath("Test");
		}
		
		if (motion_panel != null) {
			if (DEBUG) System.out.println("Motions.getComboPanel: return existing panel");
			return motion_panel;
		}
	   	// Initialize widgets
			
		Motions motions = this; //new widgets.motions.Motions();
		if (DEBUG) System.out.println("Motions.getComboPanel: motions located");
			
	    //orgsPane.addOrgListener(motions.getModel());
	    if (_medit == null) {
			if (DEBUG) System.out.println("Motions.getComboPanel: created Editor");
	    	//editor = _medit = new MotionEditor();
	    	//motions.addListener((MotionsListener)_medit);
	    	
	    	editor = new JPanel();
	    	
	    	if (tmpeditor == null) tmpeditor = new TemporaryMotionsEditor(motions);
	    	motions.addListener(tmpeditor);
			if (DEBUG) System.out.println("Motions:getComboPanel: added mot tmpeditor");
			
			
	    } else {
	    	motions.addListener((MotionsListener)_medit);
			if (DEBUG) System.out.println("Motions.getComboPanel: motion editor listens to me");
	    }
	    //motions.addListener(justifications.getModel());
	    //motions.addListener(_jbc);
	    motions.addListener(MainFrame.status);
		if (DEBUG) System.out.println("Motions.getComboPanel: status listens to me");
	    	
	    	   	
	    motion_panel = MainFrame.makeMotionPanel(editor, motions);
	    //DD.jscm = motion_panel; //new javax.swing.JScrollPane(motion_panel);
	    	//tabbedPane.addTab("Motions", jscm);
		if (DEBUG) System.out.println("Motions.getComboPanel: added motions panel");
		
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
	public void showSelected() {
		if (DEBUG) System.out.println("Motions.showSelected");
		if (!(this.getParent() instanceof JViewport)) {
			if (_DEBUG) System.out.println("Motions.showSelected bad parent");
			return;
		}
		int row = this.getSelectedRow();
		if (row <= 0) {
			if (DEBUG) System.out.println("Motions.showSelected no row: "+row);
			return;
		}
		JViewport viewport = (JViewport) this.getParent();
		Point pt = viewport.getViewPosition();
	    Rectangle rect = this.getCellRect(row, 0, true);
	    //rect.setLocation(rect.x-pt.x, rect.y-pt.y);
		this.scrollRectToVisible(rect);
		//if (scrollPane != null) scrollPane.scrollRectToVisible(rect);
		if (DEBUG) System.out.println("Motions.showSelected: done rect="+rect+" vs pt="+pt);
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
	public static final int M_EXPORT = 11;
	public static final int M_REFRESH = 12;
	
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    
	public static final JFileChooser filterUpdates = new JFileChooser();
    
	Motions tree; ImageIcon icon; int cmd;
    public MotionCustomAction(Motions tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if (DEBUG) System.out.println("MotionCAction: start");
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row = -1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		//org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row = tree.getSelectedRow();
       		row = tree.convertRowIndexToModel(row);
    		//org_id = tree.getModel().org_id;
    		//System.err.println("Row selected: " + row);
    	}
    	MotionsModel model = (MotionsModel)tree.getModel();
    	
    	if (DEBUG) System.out.println("MotionCAction: row = "+row);
    	//do_cmd(row, cmd);
    	if (cmd == M_WLAN_REQUEST) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction:WLANRequest: start");
    		String _m_GID = model.getMotionGID(row);
    		if (_m_GID==null) {
    			if (DEBUG) System.out.println("Morions: MotionCustomAction: WLANRequest: null GID");
        		return;
    		}
    		if (DEBUG) System.out.println("Morions: MotionCustomAction: WLANRequest: GID: "+_m_GID);
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
			if (! rq.moti.contains(_m_GID)) {
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
    	if (cmd == M_REDRAW){
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Redraw: start");
    		tree.initColumnSizes();
    	}
    	if (cmd == M_REFRESH){
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Redraw: start");
    		new util.DDP2P_ServiceThread(__("Motions Refresh"), true, tree) {

				@Override
				public void _run() {
					Motions tree = (Motions) ctx; //getContext();
		    		tree.getModel().refresh();
				}
    			
    		}.start();
    	}
        if (cmd == M_DEL) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Del: start");
    		String _m_ID = model.getMotionIDstr(row);
    		if(_m_ID == null) return;
    		D_Motion.zapp(_m_ID);
     	}
    	if (cmd == M_DEL_PARTIAL) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Del Partial: start");
    		D_Motion.zappPartial();
    	}

    	if (cmd == M_ADD) {
        	long constID = tree.getConstituentIDMyself();
        	String gID = tree.getConstituentGIDMyself();
         	if ((constID <= 0) || (gID == null)) {
        			Application_GUI.warning(__("No constituent selected. First select a constituent for this org"), __("First register a constituent!"));
        			return;
        	}
    		
        	if (DEBUG) System.out.println("MotionCAction: start ADD");
        	D_Motion n_motion = D_Motion.getEmpty();
    		n_motion.getMotionTitle().title_document.setDocumentString(__("Newly added motion"));
    		long cID = constID;
    		n_motion.setConstituentGID(gID);
    		n_motion.setConstituentLIDstr(Util.getStringID(cID));
    		n_motion.setOrganizationLIDstr(tree.getOrganizationID());
    		n_motion.setCreationDate(Util.CalendargetInstance());
    		n_motion.setTemporary();
    		long nID;

    		nID = n_motion.storeLinkNewTemporary();
    		if(DEBUG) System.out.println("MotionCAction: got id="+nID);
    		tree.setCurrent(nID);
    	}
    	if (cmd == M_ADVERTISE) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Advertise: start");
    		model.advertise(row);
    	}
    	if (cmd == M_TOGGLE_HIDE) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Toggle hide: start");
    		MotionsModel.hide = ! MotionsModel.hide;
    		new util.DDP2P_ServiceThread("Motions Hide", true, model) {

				@Override
				public void _run() {
					((MotionsModel) ctx).update(null, null);
				}
    		}.start();
    		//model.fireTableDataChanged();
    	}
    	if (cmd == M_ENHANCING_THIS) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Enhance This: start");
    		model.setCrtEnhanced(model.getMotionIDstr(row));
    	}
        if (cmd == M_REM_ENHANCING) {
    		if (DEBUG) System.out.println("Motions:MotionCustomAction: Rem Enhancing: start");
    		model.setCrtEnhanced(null);        	
        }
       	if (cmd == M_ENHANCE) {
        	if (DEBUG) System.out.println("MotionsCAction: start Enhance "+model.crt_enhanced);
    		long cID = tree.getConstituentIDMyself();
    		if (cID <= 0) return;
    		D_Motion n_motion;
        	long m_ID = Util.lval(model.getMotionIDstr(row), -1);
        	
        	D_Motion old_motion = D_Motion.getMotiByLID(m_ID, true, false);
        	n_motion = D_Motion.getEmpty();
        	n_motion.loadRemote(old_motion, null, null, null);
        	n_motion.changeToDefaultEnhancement();
        	//n_justification.motion_title.title_document.setDocumentString(_("Newly added justification"));
        	n_motion.setConstituentLIDstr(Util.getStringID(cID));
        	n_motion.setConstituentGID(tree.getConstituentGIDMyself());
        	//n_justification.organization_ID = tree.getOrganizationID();
        	//n_justification.creation_date = Util.CalendargetInstance();
        	long nID;
        	n_motion.setGID(null);
        	n_motion.setTemporary();
        	nID = n_motion.storeLinkNewTemporary();
        	//nID = n_motion.storeVerified();
        	if (DEBUG) System.out.println("MotionCAction: got id="+nID);
        	model.crt_enhanced = null;
        	tree.setCurrent(nID);
        	if (DEBUG) System.out.println("MotionCAction: fire="+nID);
        	tree.fireListener(n_motion, ""+nID, 0);
     	}
       	if (cmd == M_EXPORT) {
        	if (DEBUG) System.out.println("MotionsCAction: start Export");
			//String lid = model.getMotionIDstr(row); //.getGID(row);
			D_Motion motion = model.getMotion(row); // D_Motion.getMotiByLID(lid, true, false);
			
			if (motion.isTemporary()) {
				Application_GUI.warning(__("Cannot Export Temporary Motions!"), __("Temporary Motion"));
				return;
			}
			
			long declared_motion_author_lid = motion.getConstituentLID();
			D_Constituent declared_motion_author = null;
			if (declared_motion_author_lid > 0) declared_motion_author = D_Constituent.getConstByLID(declared_motion_author_lid, true, false);
			long myself_constituent_lid = model.getConstituentIDMyself();
			D_Constituent myself_constituent = null;
			if (myself_constituent_lid > 0) myself_constituent = D_Constituent.getConstByLID(myself_constituent_lid, true, false);
			
			D_Vote vote = D_Vote.getOneBroadcastedSupportForMotion(motion, myself_constituent, motion.getSupportChoice());
			if (vote == null) {
				Application_GUI.warning(__("Cannot Export Unsupported Motions!"), __("Unsupported Motion"));
				return;
			}
			D_Motion enhanced = motion.getEnhancedMotion();
			D_Constituent cons = vote.getConstituent();
			D_Justification just = vote.getJustificationFromObjOrLID();
			D_Organization org = motion.getOrganization();//D_Organization.getOrgByLID(m.getOrganizationLIDstr(), true, false);
			
			filterUpdates.setFileFilter(new widgets.components.StegoFilterKey());
			filterUpdates.setName(__("Select Secret Trusted Key"));
			//filterUpdates.setSelectedFile(null);
			Util_GUI.cleanFileSelector(filterUpdates);
			int returnVal = filterUpdates.showDialog(tree,__("Specify Motion File"));
			if (returnVal != JFileChooser.APPROVE_OPTION)  return;
			File fileTrustedSK = filterUpdates.getSelectedFile();
			//SK sk;
			//PK pk;
			if (fileTrustedSK.exists()) {
				int _c = Application_GUI.ask(__("Existing file. Overwrite: "+fileTrustedSK+"?"), __("Overwrite file?"), JOptionPane.OK_CANCEL_OPTION);
				if (_c != 0) return;
			}
			try {
				boolean result = false;
				//result = KeyManagement.saveSecretKey(gid, fileTrustedSK.getCanonicalPath());
				DD_SK dsk =  new DD_SK(); 
				dsk.org.add(org);
				dsk.moti.add(motion);
				if (enhanced != null) dsk.moti.add(enhanced);
				if (cons != null) {
					cons.loadNeighborhoods(D_Constituent.EXPAND_ALL);
					dsk.constit.add(cons);
					if (cons.getNeighborhood() != null) {
						for (int k = 0; k < cons.getNeighborhood().length; k++) {
							dsk.neigh.add(cons.getNeighborhood()[k]);
						}
					}
				}
				if (declared_motion_author != null) {
					declared_motion_author.loadNeighborhoods(D_Constituent.EXPAND_ALL);
					dsk.constit.add(declared_motion_author);
					if (declared_motion_author.getNeighborhood() != null) {
						for (int k = 0; k < declared_motion_author.getNeighborhood().length; k++) {
							dsk.neigh.add(declared_motion_author.getNeighborhood()[k]);
						}
					}
				}
				if (just != null) dsk.just.add(just);
				if (vote != null) dsk.vote.add(vote);
				D_Peer org_creat = org.getCreator();
				if (org_creat != null) dsk.peer.add(org_creat);
				
				dsk.sign_and_set_sender(data.HandlingMyself_Peer.get_myself_or_null()); //D_Peer.getPeerByGID_or_GIDhash_NoCreate(gid, null, true, false);
				if (_DEBUG) System.out.println("MotionsCAction: PeersRowAction: actionPerformed: export: will encode: "+dsk);
					
				String []explain = new String[1];
				result = DD.embedPeerInBMP(fileTrustedSK, explain, dsk);
						
				if (result) return;
			} catch (Exception e3) {
				Application_GUI.warning(__("Failed to save motion: "+e3.getMessage()), __("Failed to save motion"));
				e3.printStackTrace();
				return;
			}
       	}
    }
}

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

import util.P2PDDSQLException;

import streaming.RequestData;
import util.DBInterface;
import util.Util;
//import widgets.org.ColorRenderer;
import widgets.components.DocumentTitleRenderer;
import widgets.motions.MotionsModel;
import widgets.org.Orgs;
import wireless.BroadcastClient;
import config.Application;
import config.DD;
import config.DDIcons;
//import config.DDIcons;
import config.Identity;
import data.D_Justification;
import data.D_Motion;

@SuppressWarnings("serial")
public class Motions extends JTable implements MouseListener  {
	private static final int DIM_X = 1000;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = 4;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private DocumentTitleRenderer titleRenderer;
	DefaultTableCellRenderer centerRenderer;
	public Motions() {
		super(new MotionsModel(Application.db));
		init();
	}
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
	void init(){
		if(DEBUG) System.out.println("Motions: init: start");
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
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		
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

	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == MotionsModel.TABLE_COL_NAME)) return titleRenderer;
		if ((column == getModel().TABLE_COL_VOTERS_NB)) return centerRenderer;
		if ((column == getModel().TABLE_COL_ACTIVITY)) return centerRenderer;
		if ((column == getModel().TABLE_COL_NEWS)) return centerRenderer;

		//if ((column == MotionsModel.TABLE_COL_CONNECTION)) return bulletRenderer;
//		if (column >= MotionsModel.TABLE_COL_PLUGINS) {
//			int plug = column-MotionsModel.TABLE_COL_PLUGINS;
//			if(plug < plugins.size()) {
//				String pluginID= plugins.get(plug);
//				return plugin_applets.get(pluginID).renderer;
//			}
//		}
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {null,null,_("A name you provide")};
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= columnToolTips.length) return null;
				return columnToolTips[realIndex];
            }
        };
    }
	public MotionsModel getModel(){
		return (MotionsModel) super.getModel();
	}
	public void initColumnSizes() {
        MotionsModel model = (MotionsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < model.getColumnCount(); i++) {
        	headerWidth = 0;
        	cellWidth = 0;
            column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	Object val = getValueAt(r, i);
            	comp = this.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 this, val,
                                 false, false, 0, i);
            	int this_width = comp.getPreferredSize().width;
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
		if(DEBUG) System.out.println("Motions:fireForceEdit: row="+orgID);
		for(MotionsListener l: listeners){
			if(DEBUG) System.out.println("Motions:fireForceEdit: l="+l);
			try{
				if(orgID==null) ;//l.forceEdit(orgID);
				else l.news_forceEdit(orgID);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	void fireListener(D_Motion crt_mot, String motion_ID, int col) {
		
		for(MotionsListener l: listeners){
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
		listeners.add(l);
		int row =this.getSelectedRow();
		if(row>=0) {
			if(DEBUG) System.out.println("Motions:addListener: row="+row);
			l.motion_update(this.getModel().getMotionID(this.convertRowIndexToModel(row)),A_NON_FORCE_COL, null);
		}
		if(DEBUG) System.out.println("\n************\nMotions:addListener: Done");
	}
	public void removeListener(MotionsListener l){
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
    			Application.warning(_("No constituent selected. First select a constituent for this org"), _("First register a constituent!"));
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

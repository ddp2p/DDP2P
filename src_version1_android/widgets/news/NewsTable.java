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
package widgets.news;

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

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import util.P2PDDSQLException;
import util.DBInterface;
import util.Util;
import widgets.app.DDIcons;
import widgets.app.MainFrame;
import widgets.components.DebateDecideAction;
import widgets.components.DocumentTitleRenderer;
import widgets.motions.Motions;
import widgets.motions.MotionsModel;
import widgets.news.NewsListener;
import widgets.news.NewsModel;
import config.Application;
import config.Identity;
import data.D_Motion;
import data.D_News;

public class NewsTable extends JTable implements MouseListener {
	private static final int DIM_X = 1000;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = 4;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private DocumentTitleRenderer titleRenderer;
	public NewsTable() {
		super(new NewsModel(Application.db));
		init();
	}
	public NewsTable(DBInterface _db) {
		super(new NewsModel(_db));
		init();
	}
	public NewsTable(NewsModel dm) {
		super(dm);
		init();
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		titleRenderer = new DocumentTitleRenderer();
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
		//if ((column == NewsModel.TABLE_COL_CONNECTION)) return bulletRenderer;
//		if (column >= NewsModel.TABLE_COL_PLUGINS) {
//			int plug = column-NewsModel.TABLE_COL_PLUGINS;
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
	public NewsModel getModel(){
		return (NewsModel) super.getModel();
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
	public String getMotionID() {
		return  getModel().getMotionID();
	}
	private void initColumnSizes() {
        NewsModel model = (NewsModel)this.getModel();
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

	ArrayList<NewsListener> listeners=new ArrayList<NewsListener>();
	public void fireForceEdit(String orgID) {		
		if(DEBUG) System.out.println("NewsTable:fireForceEdit: row="+orgID);
		for(NewsListener l: listeners){
			if(DEBUG) System.out.println("NewsTable:fireForceEdit: l="+l);
			try{
				if(orgID==null) ;//l.forceEdit(orgID);
				else l.motion_forceEdit(orgID);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	void fireListener(int row, int col) {
		if(DEBUG) System.out.println("NewsTable:fireListener: row="+row);
		for(NewsListener l: listeners){
			if(DEBUG) System.out.println("NewsTable:fireListener: l="+l);
			try{
				if(row<0) l.newsUpdate(null, col);
				else l.newsUpdate(Util.getString(this.getModel()._news[this.convertRowIndexToModel(row)]), col);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addListener(NewsListener l){
		listeners.add(l);
		int row =this.getSelectedRow();
		if(row>=0)
			l.newsUpdate(Util.getString(this.getModel()._news[this.convertRowIndexToModel(row)]),A_NON_FORCE_COL);
	}
	public void removeListener(NewsListener l){
		listeners.remove(l);
	}

	public void setCurrent(long org_id) {
		getModel().setCurrent(org_id);
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
        
    	NewsModel model = (NewsModel)getModel();
 		int model_row=convertRowIndexToModel(row);
   	   	if(model_row>=0) {
   	   		String orgID = Util.getString(model._news[model_row]);
   	   		try{
   	   			long oID = new Integer(orgID).longValue();
   	   			model.setCurrent(oID);
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
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	NewsCustomAction aAction;
    	
    	aAction = new NewsCustomAction(this, _("Add!"), delicon,_("Add news."), _("Add"),KeyEvent.VK_A, NewsCustomAction.M_ADD);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	
    	aAction = new NewsCustomAction(this, _("Delete Organization Constraint!"), delicon,_("Delete Organization Constraint."), _("Delete Organization Constraint"),KeyEvent.VK_O, NewsCustomAction.M_DELORG);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);

    	aAction = new NewsCustomAction(this, _("Delete Motion Constraint!"), delicon,_("Delete Motion Constraint."), _("Delete Motion Constraint"),KeyEvent.VK_M, NewsCustomAction.M_DELMOT);
    	aAction.putValue("row", new Integer(row));
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
	public void connectWidget() {
		getModel().connectWidget();
		
    	MainFrame.status.addOrgStatusListener(getModel());
    	MainFrame.status.addMotionStatusListener(getModel());
    	if(_nedit != null) {
    		MainFrame.status.addOrgStatusListener(_nedit);
    		MainFrame.status.addMotionStatusListener(_nedit);
    	}
	}
	public void disconnectWidget() {
		getModel().disconnectWidget();
		
		MainFrame.status.removeOrgListener(getModel());
		MainFrame.status.removeMotListener(getModel());
		if(_nedit != null) {
			MainFrame.status.removeOrgListener(_nedit);
			MainFrame.status.removeMotListener(_nedit);
		}
	}
	NewsEditor _nedit = null;
	Component news_panel = null;
	public Component getComboPanel() {
		if(news_panel != null) return news_panel;
	   	widgets.news.NewsTable news = this; //new widgets.news.NewsTable();
	    if(_nedit == null) _nedit = new NewsEditor();
    	//orgsPane.addOrgListener(news.getModel());
	    news.addListener(_nedit);
    	//orgsPane.addOrgListener(_nedit);
		if(DEBUG) System.out.println("createAndShowGUI: some news");
    	news_panel = MainFrame.makeNewsPanel(_nedit, news);
    	javax.swing.JScrollPane jscn = new javax.swing.JScrollPane(news_panel);
		//tabbedPane.addTab("News", jscn);
		return jscn;
	}
}

@SuppressWarnings("serial")
class NewsCustomAction extends DebateDecideAction {
    public static final int M_DELORG = 2;
	public static final int M_DELMOT = 3;
	public static final int M_ADD = 1;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	NewsTable tree; ImageIcon icon; int cmd;
    public NewsCustomAction(NewsTable tree,
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
    	NewsModel model = (NewsModel)tree.getModel();
    	
    	if(DEBUG) System.out.println("NewsCAction: row = "+row);
    	//do_cmd(row, cmd);
    	if(cmd == M_DELORG) {
    		model.orgUpdate(null, 0, null);
    		model.motion_update(null, 0, null);
    	}
        if(cmd == M_DELMOT) {
    		model.motion_update(null, 0, null);        	
        }
        if(cmd == M_ADD) {
        	if(DEBUG) System.out.println("NewsCAction: start ADD");
        	D_News n_news = new D_News();
    		n_news.title.title_document.setDocumentString(_("Newly added news"));
    		long cID = tree.getConstituentIDMyself();
    		if(cID<=0) return;
    		n_news.constituent_ID = Util.getStringID(cID);
    		n_news.organization_ID = tree.getOrganizationID();
    		n_news.global_constituent_ID = tree.getConstituentGIDMyself();
    		n_news.creation_date = Util.CalendargetInstance();
    		n_news.motion_ID = tree.getMotionID();
    		long nID;
			try {
				nID = n_news.storeVerified();
	        	if(DEBUG) System.out.println("NewsCAction: got id="+nID);
				tree.setCurrent(nID);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    }
}


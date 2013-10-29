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
package widgets.peers;
import hds.ASNPluginInfo;
import hds.ControlPane;
import hds.DebateDecideAction;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

import plugin_data.PeerPlugin;
import plugin_data.PeerPluginAction;
import plugin_data.PeerPluginEditor;
import plugin_data.PeerPluginMenuItem;
import plugin_data.PeerPluginRenderer;
import plugin_data.PluginMenus;
import plugin_data.PluginRegistration;
import plugin_data.PluginRequest;

import ciphersuits.KeyManagement;
import ciphersuits.PK;
import ciphersuits.SK;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import config.DDIcons;
import config.Identity;
import data.D_OrgDistribution;
import data.D_PeerAddress;
import data.D_PluginInfo;
import data.D_PluginData;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.Util;
import widgets.components.BulletRenderer;
import widgets.components.TableUpdater;
import widgets.dir_fw_terms.TermsPanel;
import widgets.private_org.PrivateOrgPanel;
import widgets.private_org.PrivateOrgTable;
import static util.Util._;

class NoPluginRenderer implements TableCellRenderer{

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		return new JLabel("Absent");
	}
	
}

class MyComboBoxRenderer extends JLabel  implements TableCellRenderer {
//	class MyComboBoxRenderer extends DefaultTableCellRenderer  implements TableCellRenderer {
  public MyComboBoxRenderer() {
    super();
	setOpaque(true);
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
	  if(value instanceof JComboBox){
		  int cnt = ((JComboBox) value).getItemCount();
		  String text;
		  if(cnt <= 0) text = "#0";
		  text = "/#"+cnt+": "+Util.getString(((JComboBox) value).getItemAt(0));
		  if(cnt > 1) this.setBackground(Color.YELLOW);
		  else this.setBackground(Color.LIGHT_GRAY);
		  //if(cnt>1) text = text;
		  setText(text);
	  }else{
			setBackground(Color.WHITE);
		  setText(Util.getString(value));
	  }
    return this;
  }
}

@SuppressWarnings("serial")
class MyCComboBoxRenderer extends JComboBox<Object> implements TableCellRenderer {
  public MyCComboBoxRenderer() {
    super();
  }

  @SuppressWarnings("unchecked")
public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
	  this.removeAllItems();
	  if(value instanceof JComboBox){
		  for(int k=0; k<((JComboBox<Object>) value).getItemCount(); k++)
			  this.addItem(((JComboBox<Object>) value).getItemAt(k));
		  return this;
	  }
	  this.addItem(value);
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }
    setSelectedItem(value);
    return this;
  }
}

@SuppressWarnings("serial")
public class Peers extends JTable implements MouseListener {
	// Different icons should be displayed for each state... for now just on/off
	public static final int STATE_CONNECTION_FAIL =0;
	public static final int STATE_CONNECTION_TCP = 1;
	public static final int STATE_CONNECTION_UDP = 2;
	public static final int STATE_CONNECTION_UDP_NAT = 3;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int COMMAND_MENU_SET_MYSELF = 0;
	static final int COMMAND_MENU_NEW_MYSELF = 1;
	static final int COMMAND_MENU_NEW_PLUGINS = 2;
	public static final int COMMAND_REFRESH_VIEWS = 3;
	public static final int COMMAND_MENU_SAVE_SK = 4;
	public static final int COMMAND_MENU_LOAD_SK = 5;
	public static final int COMMAND_MENU_SET_NAME = 6;
	public static final int COMMAND_MENU_SET_SLOGAN = 7;
	public static final int COMMAND_TOUCH_CLIENT = 8;
	public static final int COMMAND_MENU_SET_TERMS = 9;
	public static final int COMMAND_MENU_SHARE_ORG = 10;
	static final int COMMAND_MENU_SET_EMAILS = 11;
	public static final int COMMAND_MENU_REFRESH = 12;
	public static final int COMMAND_MENU_BLOCK = 13;

	
	BulletRenderer bulletRenderer = new BulletRenderer();
	MyComboBoxRenderer myComboBoxRenderer;
	ColorRenderer colorRenderer;
	NoPluginRenderer noPluginRenderer = new NoPluginRenderer();
	protected String[] columnToolTips = {null,null,_("A name you provide")};
	ArrayList<PeerListener> listeners = new ArrayList<PeerListener>();
	public TermsPanel termsPanel;
	public PrivateOrgPanel privateOrgPanel; // to listen to Orgs
	private JMenuItem refresh = new JMenuItem();
	private JMenuItem myself_new = refresh;
	private JMenuItem use_stop = refresh;
	private JMenuItem use_start = refresh;
	private JMenuItem block_stop = refresh;
	private JMenuItem block_start = refresh;
	private JMenuItem filter_stop = refresh;
	private JMenuItem filter_start = refresh;
	private JMenuItem serving_stop = refresh;
	private JMenuItem serving_start = refresh;
	private JMenuItem serve_default = refresh;
	private JMenuItem share_org = refresh;
	private JMenuItem myself_set = refresh;
	private JMenuItem name_set = refresh;
	private JMenuItem slogan_set = refresh;
	private JMenuItem emails_set = refresh;
	private JMenuItem terms_set = refresh;
	private JMenuItem export_SK = refresh;
	private JMenuItem import_SK = refresh;
	private JMenuItem reset_peer = refresh;
	private JMenuItem delete_row = refresh;
	private JMenuItem wake_up = refresh;
	private JMenuItem refresh_plugins = refresh;
	private JMenuItem refresh_model = refresh;
	
	
	public Peers() {
		super(new PeersModel(Application.db));
		this.initPopupItems();
		init();
	}
	public Peers(DBInterface _db) {
		super(new PeersModel(_db));
		this.initPopupItems();
		init();
	}
	public Peers(PeersModel dm) {
		super(dm);
		this.initPopupItems();
		init();
	}
	/**
	 * 
	 * @param plugin_ID : unique string
	 * @param plugin_name : descriptive name
	 * @param column : column in widget table where the menu item appears
	 * @param plugin_menuItem : item
	 * @return
	 */
	public boolean registerPluginMenu(String plugin_ID, String plugin_name, int column, Action plugin_menuItem){
		return true;
	}
	/**
	 * 
	 * @param plugin_ID : unique string
	 * @param plugin_name : descriptive name
	 * @param column : column in widget table where the menu item appears
	 * @param plugin_menu_item
	 * @return
	 */
	public boolean registerPluginMenu(String plugin_ID, String plugin_name, int column, JMenuItem plugin_menu_item){
		return true;
	}
	/**
	 * 
	 * @param plugin_ID
	 * @param column : the column freed
	 * @return
	 */
	public boolean deregisterPluginMenu(String plugin_ID, int column){
		return true;
	}
	/**
	 * Does it need to be executed on Dispatcher Thread or not?
	 * @param plugin_GID
	 * @param plugin_name
	 * @param plugin_info
	 * @param plugin_url
	 * @param renderer
	 * @param editor
	 * @return
	 */
	public boolean registerPlugin(
			String plugin_GID,
			String plugin_name,
			String plugin_info,
			String plugin_url,
			TableCellRenderer renderer,
			TableCellEditor editor
			//,PeerPlugin plugin
			) {
		int columns = this.getModel().columnNames.length;//this.getColumnModel().getColumnCount();
		TableColumnModel cm = this.getColumnModel();
		int model_columns = cm.getColumnCount();
		if(columns>model_columns){
			System.err.println("Peers:registerPlugin: Less columns than desired: "+model_columns+" < "+columns);
		}
		int crt = D_PluginInfo.plugins.size()+PeersModel.TABLE_COL_PLUGINS-1;
		
		String colName = plugin_name;
		if(colName==null) colName=_("Plugin");
		colName = colName.substring(0, 5)+" "+(crt-columns+2);
		if(DEBUG)System.out.println("Peers:registerPlugin: adding plugin column val="+ crt+" = "+colName+" column="+columns+" mc="+model_columns);
		if(crt > model_columns) {
			if(_DEBUG)System.out.println("Peers:registerPlugin: fail adding plugin column val="+ crt+" = "+colName);
			return false;
		}
		if(crt < model_columns) {
			if(crt == columns-1) {
				String cl = this.getModel().getColumnName(crt);
				if(DEBUG)System.out.println("Peers:registerPlugin: adding plugin column val="+ crt+" = "+colName+" cl="+cl);
				try{
					TableColumn c = this.getColumn(cl); // Plugin not found
					c.setHeaderValue(colName);
				}catch(Exception e){e.printStackTrace();}
				//this.getColumnModel().addColumn(c);
				this.getModel().addColumn(crt, colName);
			}else{
				if(_DEBUG)System.out.println("Peers:registerPlugin: fail adding plugin column val="+ crt+" = "+colName+" m="+model_columns);
				return false;
			}
		}
		if(crt == model_columns) {
			TableColumn c = new TableColumn(crt);
	        c.setHeaderValue(colName);
			this.getColumnModel().addColumn(c);
			this.getModel().addColumn(crt, colName);
			if(DEBUG)System.out.println("Peers:registerPlugin: adding plugin column="+ crt+" = "+colName);
			//crt = columns-1;
		}		
		TableColumn appletColumn = this.getColumnModel().getColumn(crt);
		try{
			if(editor != null) appletColumn.setCellEditor(editor);
			if(renderer != null) appletColumn.setCellRenderer(renderer);
		}catch(Exception e){e.printStackTrace(); return false;}
		return true;
	}
	/**
	 * 
	 * @param plugin_ID
	 * @return
	 */
	public boolean deregisterPlugin(String plugin_GID){
		return true;
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		colorRenderer = new ColorRenderer(getModel());
		myComboBoxRenderer = new MyComboBoxRenderer();
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	if(DEBUG) System.out.println("Peers:getPanel: start");
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setMinimumSize(new Dimension(100,50));
        
        //jp.add(scrollPane, BorderLayout.CENTER);
        Application.peer_contacts = new PeerContacts();
        //jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, new JScrollPane(Application.peer)), BorderLayout.CENTER);
        JPanel p = new JPanel(new BorderLayout());
	//	p.add(new TermsPanel(1, "khalid"));
	//	p.add(Application.peer);
//        scrollPane.setPreferredSize(new Dimension(400, 200));
        termsPanel = new TermsPanel();
        this.addListener(termsPanel);
    	if(DEBUG) System.out.println("Peers:getPanel: added termsPanel");
    	JScrollPane scrollPane2 = new JScrollPane(termsPanel);
        scrollPane2.setMinimumSize(new Dimension(100,100));
        //p.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane2 ,new JScrollPane(Application.peer)));
       // p.add(new JScrollPane(new JScrollPane(Application.peer)) );

        privateOrgPanel = new PrivateOrgPanel();
        JScrollPane scrollPane3 = new JScrollPane(privateOrgPanel);
        scrollPane3.setMinimumSize(new Dimension(100,100));
        p.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		new JSplitPane(JSplitPane.VERTICAL_SPLIT,scrollPane2,scrollPane3),
        		new JScrollPane(Application.peer_contacts)));
        
        jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, p ), BorderLayout.CENTER);
		return jp;
    }

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
	   Object value = super.getValueAt(row, column);
	   if(value != null) {
	      if(value instanceof JComboBox) {
	           return new DefaultCellEditor((JComboBox)value);
	      }
	            return getDefaultEditor(value.getClass());
	   }
	   return super.getCellEditor(row, column);
	}
	public TableCellRenderer getCellRenderer(int row, int column) {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("Peers:getCellRenderer: start row="+row);
		//if ((column == PeersModel.COL_UDP_ON)) return bulletRenderer;
		//if ((column == PeersModel.COL_TCP_ON)) return bulletRenderer;
		//if ((column == DirectoriesModel.COL_NAT)) return bulletRenderer;
		if ((column == PeersModel.TABLE_COL_SERVING)) return myComboBoxRenderer;
		if ((column == PeersModel.TABLE_COL_NAME)) return colorRenderer;
		if ((column == PeersModel.TABLE_COL_SLOGAN)) return colorRenderer;
		if ((column == PeersModel.TABLE_COL_CONNECTION)) return bulletRenderer;
		if(DEBUG) System.out.println("Peers:getCellRenderer: start col="+column);
		if (column >= PeersModel.TABLE_COL_PLUGINS) {
			if(DEBUG) System.out.println("Peers:getCellRenderer: start plugin col="+column);
			
			
			int plug = column-PeersModel.TABLE_COL_PLUGINS;
			if(plug < D_PluginInfo.plugins.size()) {
				
				int model_row = this.convertRowIndexToModel(row);
				
				String pluginGID = D_PluginInfo.plugins.get(plug);
				//if(!presentPlugin(this.getModel(), model_row, pluginGID)) return noPluginRenderer;
				
				if(DEBUG) System.out.println("Peers:getCellRenderer: contained or myself");
				D_PluginInfo p = plugin_data.PluginRegistration.plugin_applets.get(pluginGID);
				//if(p!=null)return p.renderer;
				
				return new PluginPresenceRenderer(p.renderer, noPluginRenderer, super.getCellRenderer(row, column));
			}else{
				if(DEBUG) System.out.println("Peers:getCellRenderer: outside column");				
			}
		}
		return super.getCellRenderer(row, column);
	}
	public static boolean presentPlugin(PeersModel peersModel, int model_row, String pluginGID) {
		String myselfGID = peersModel.getMyselfPeerGID();
		String peer_GID = peersModel.getGID(model_row);
		if((peer_GID!=null)&&(peer_GID.equals(myselfGID))){
			return true;// plugin_data.PluginRegistration.plugin_applets.get(pluginID).renderer;
		}
		if((peer_GID==null) || (pinfo==null)) return false;
		ASNPluginInfo[] info = pinfo.get(peer_GID);
		if(info == null){
			if(DEBUG) System.out.println("Peers:getCellRenderer: start empty plugin");
			return false;//super.getCellRenderer(row, column); // Empty renderer?
		}
		if(!D_PluginInfo.contains(info, pluginGID)){
			if(DEBUG) System.out.println("Peers:getCellRenderer: start absent plugin");
			return false;
		}
		return true;
	}
	public String getPluginGID(int column) {
		if(column < PeersModel.TABLE_COL_PLUGINS) return null;
		int plug = column-PeersModel.TABLE_COL_PLUGINS;
		if(plug >= D_PluginInfo.plugins.size()) return null;
		return D_PluginInfo.plugins.get(plug);
	}
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
	public void setConnectionState(String peerID, int state){
		((PeersModel) this.getModel()).setConnectionState(peerID, state);
	}
    /*
	public void setUDPOn(String address, Boolean on){
		((PeersModel) this.getModel()).setUDPOn(address, on);
	}
	public void setTCPOn(String address, Boolean on){
		((PeersModel) this.getModel()).setTCPOn(address, on);
	}
	public void setNATOn(String address, Boolean on){
		((PeersModel) this.getModel()).setNATOn(address, on);
	}
	*/
	public PeersModel getModel(){
		return (PeersModel) super.getModel();
	}
	private void initColumnSizes() {
        PeersModel model = (PeersModel)this.getModel();
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
            	/*comp = this.getDefaultRenderer(model.getColumnClass(i)).
                        getTableCellRendererComponent(
                            this, getValueAt(r, i),
                            false, false, 0, i);*/
            	comp = this.getCellRenderer(r,i).
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
	/**
	 * @param args
	 * @throws P2PDDSQLException 
	 */
	public static void main(String[] args) throws P2PDDSQLException {
		String dfname = Application.DELIBERATION_FILE;
		Application.db = new DBInterface(dfname);
		PeersTest.createAndShowGUI(Application.db);
	}
	@Override
	public void mouseClicked(MouseEvent arg0) {
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	@Override
	public void mousePressed(MouseEvent arg0) {
    	jtableMouseReleased(arg0, false);
	}
	@Override
	public void mouseReleased(MouseEvent arg0) {
    	jtableMouseReleased(arg0, true);
	}

	void initPopupItems(){
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	//
    	refresh = new JMenuItem(new PeersRowAction(this, _("Refresh!"), reseticon,_("Refresh."),
    			_("Refresh!"),KeyEvent.VK_R, Peers.COMMAND_MENU_REFRESH));
       	
        	//
//    	myself_new = new JMenuItem(new PeersRowAction(this, _("New Myself!"), reseticon,_("Create new myself peer."),
//        			_("New Myself!"),KeyEvent.VK_N, Peers.COMMAND_MENU_NEW_MYSELF));
       	use_stop = new JMenuItem(new PeersUseAction(this, _("No pull from it!"),addicon,_("Stop pulling from it."),_("Will not be used to synchronize."),KeyEvent.VK_A));
       	use_start = new JMenuItem(new PeersUseAction(this, _("Pull from it!"),addicon,_("Pull from it."),_("Will be used to synchronize."),KeyEvent.VK_A));
    	//
       	block_stop = new JMenuItem(new PeersRowAction(this, _("Unblock!"),addicon,_("Stop blocking it."),_("Will not be blocked to synchronize."),KeyEvent.VK_A,  Peers.COMMAND_MENU_BLOCK));
       	block_start = new JMenuItem(new PeersRowAction(this, _("Block!"),addicon,_("Block it."),_("Will be blocked to synchronize."),KeyEvent.VK_A,  Peers.COMMAND_MENU_BLOCK));
    	//
    	filter_stop = new JMenuItem(new PeersFilterAction(this, _("UnFilter!"),addicon,_("Get anything."),_("Will ask for anything new."),KeyEvent.VK_F));
    	filter_start = new JMenuItem(new PeersFilterAction(this, _("Filter!"),addicon,_("Request only specified orgs."),_("Will only ask for organizations marked as requested."),KeyEvent.VK_F));
    	//
    	serving_stop  = new JMenuItem(new PeersBroadcastAction(this, _("Stop Serving!"),addicon,_("Stop serving."),_("Will not be sent at synchronization."),KeyEvent.VK_B, false));
    	serving_start = new JMenuItem(new PeersBroadcastAction(this, _("Serve!"),addicon,_("Serve it."),_("Will be sent at synchronize."),KeyEvent.VK_B, false));
    	//
    	serve_default = new JMenuItem(new PeersBroadcastAction(this, _("Peer-Defined Serve!"),addicon,_("Default serving."),_("Will let peer decide serving."),KeyEvent.VK_D, true));
    	//
    	share_org = new JMenuItem(new PeersRowAction(this, _("Share Org!"), reseticon,_("Share Organization."),
    			_("Share Organization!"),KeyEvent.VK_H, Peers.COMMAND_MENU_SHARE_ORG));
    	//
    	myself_set = new JMenuItem(new PeersRowAction(this, _("Set as Myself!"), reseticon,_("Set this as myself peer."),
    			_("Set as Myself!"),KeyEvent.VK_M, Peers.COMMAND_MENU_SET_MYSELF));
    	//
    	name_set = new JMenuItem(new PeersRowAction(this, _("Set New Name!"), reseticon,_("Set New Name."),
    			_("Set New Name!"),KeyEvent.VK_N, Peers.COMMAND_MENU_SET_NAME));
    	//
    	slogan_set = new JMenuItem(new PeersRowAction(this, _("Set Slogan!"), reseticon,_("Set New Slogan."),
    			_("Set Slogan!"),KeyEvent.VK_M, Peers.COMMAND_MENU_SET_SLOGAN));
    	//
    	emails_set = new JMenuItem(new PeersRowAction(this, _("Set Emails!"), reseticon,_("Set New Emails."),
    			_("Set Emails!"),KeyEvent.VK_E, Peers.COMMAND_MENU_SET_EMAILS));
    	//
    	terms_set = new JMenuItem(new PeersRowAction(this, _("Set Terms"), reseticon,_("Set New Terms."),
    			_("Set Terms"),KeyEvent.VK_M, Peers.COMMAND_MENU_SET_TERMS));
    	//
    	export_SK = new JMenuItem(new PeersRowAction(this, _("Export Secret keys!"), reseticon,_("Export Secret keys."),
    			_("Export Secret Keys!"),KeyEvent.VK_E, Peers.COMMAND_MENU_SAVE_SK));
    	//
    	import_SK = new JMenuItem(new PeersRowAction(this, _("Import Secret keys!"), reseticon,_("Import Secret keys."),
    			_("Import Secret Keys!"),KeyEvent.VK_E, Peers.COMMAND_MENU_LOAD_SK));
    	//
    	reset_peer = new JMenuItem(new PeersResetAction(this, _("Reset!"), reseticon,_("Bring again all data from this."), _("Go restart!"),KeyEvent.VK_R));
    	//
    	delete_row = new JMenuItem(new PeersDeleteAction(this, _("Delete!"), delicon,_("Delete all data about this."), _("Reget"),KeyEvent.VK_D));
    	//
    	wake_up = new JMenuItem(new PeersRowAction(this, _("Wake Up Communication!"), delicon,_("Wake Up Communication."),
    			_("Wake Up"),KeyEvent.VK_W, Peers.COMMAND_TOUCH_CLIENT));
    	//
    	// General actions
    	myself_new = new JMenuItem(new PeersRowAction(this, _("New Myself!"), reseticon,_("Create new myself peer."),
    			_("New Myself!"),KeyEvent.VK_N, Peers.COMMAND_MENU_NEW_MYSELF));
       	
    	// General refresh plugins
    	refresh_plugins = new JMenuItem(new PeersRowAction(this, _("RefreshPlugins!"), reseticon,_("Refresh installed Plugins."),
    			_("Refresh Plugins!"),KeyEvent.VK_P, Peers.COMMAND_MENU_NEW_PLUGINS));
       	
    	// General refresh plugins
    	refresh_model = new JMenuItem(new PeersRowAction(this, _("RefreshModel!"), reseticon,_("Refresh Model."),
    			_("Refresh Model!"),KeyEvent.VK_R, Peers.COMMAND_REFRESH_VIEWS));
	}

	JMenuItem setRow(JMenuItem item, Integer row){
		item.getAction().putValue("row", row);
    	return item;
	}
	
	JPopupMenu getPopup(int model_row, int col){
    	
    	JPopupMenu popup = new JPopupMenu();
    	Integer _model_row = new Integer(model_row);

    	//
    	popup.add(setRow(this.refresh,_model_row));
       	
       	if(model_row<0) {
        	//
        	popup.add(setRow(this.myself_new,_model_row));      		
       		return popup;
       	}
       	
       	// Variable Fields and toggles
    	if(this.getModel().used(model_row)){
        	popup.add(setRow(this.use_stop,_model_row));
    	}else{
        	popup.add(setRow(this.use_start,_model_row));
    	}
    	//blocked
    	if(this.getModel().blocked(model_row)){
        	popup.add(setRow(this.block_stop,_model_row));
    	}else{
        	popup.add(setRow(this.block_start,_model_row));
    	}
    	//
    	if(this.getModel().broadcastable(model_row)){
        	popup.add(setRow(this.serving_stop,_model_row));
    	}else{
        	popup.add(setRow(this.serving_start,_model_row));
    	}
    	//
    	if(this.getModel().filtered(model_row)){
        	popup.add(setRow(this.filter_stop,_model_row));
    	}else{
        	popup.add(setRow(this.filter_start,_model_row));
    	}
    	//
    	if(!this.getModel().broadcastable_default(model_row)) {
        	popup.add(setRow(this.serve_default,_model_row));
    	}
    	//
    	popup.addSeparator();
    	//
    	popup.add(setRow(this.share_org,_model_row));
    	//
    	popup.add(setRow(this.terms_set,_model_row));
    	//
    	popup.add(setRow(this.export_SK,_model_row));
    	//
    	popup.add(setRow(this.import_SK,_model_row));
    	//
    	popup.add(setRow(this.reset_peer,_model_row));
    	//
    	popup.addSeparator();
    	//
    	popup.add(setRow(this.delete_row,_model_row));
    	//
    	popup.addSeparator();
    	//
    	popup.add(setRow(this.wake_up,_model_row));
    	//
    	popup.add(setRow(this.myself_set,_model_row));
    	//
    	popup.addSeparator();
    	//Actions_Myself
    	//
    	popup.add(setRow(this.slogan_set,_model_row));
    	//
    	popup.add(setRow(this.emails_set,_model_row));
    	//
    	//
    	popup.add(setRow(this.name_set,_model_row));
    	popup.add(setRow(this.myself_new,_model_row));

    	// General actions
    	// General refresh plugins
    	popup.add(setRow(this.refresh_plugins,_model_row));
    	// General refresh model
    	popup.add(setRow(this.refresh_model,_model_row));

    	// Plugin Actions
    	popup.addSeparator();
    	// Add Plugin-related menus
    	Hashtable<String, PluginMenus> mn = new Hashtable<String, PluginMenus>();
    	Hashtable<String, PluginMenus> mn1 = plugin_data.PluginRegistration.plugin_menus.get(new Integer(col));
    	Hashtable<String, PluginMenus> mn2 = plugin_data.PluginRegistration.plugin_menus.get(new Integer(PluginMenus.COLUMN_MYSELF));
    	Hashtable<String, PluginMenus> mn3 = plugin_data.PluginRegistration.plugin_menus.get(new Integer(PluginMenus.COLUMN_ALL));
     	String pluginGID = this.getPluginGID(col);
    	if(mn1 != null) mn.putAll(mn1);
    	if((mn2 != null)&&(pluginGID!=null)) mn = PluginMenus.merge(mn, mn2, pluginGID);
    	if(mn3 != null) mn = PluginMenus.merge(mn, mn3, null);
    	
    	
    	addPluginPopupMenusColumn(popup, model_row, mn);
    	return popup;
	}
	private void addPluginPopupMenusColumn(JPopupMenu popup, int model_row, Hashtable<String, PluginMenus> mn) {
    	//Hashtable<String, PluginMenus> mn = plugin_data.PluginRegistration.plugin_menus.get(new Integer(col));
    	if(mn == null) return;
     	Object peer_gid = this.getModel().getGID(model_row);
    	Object peer_alias = this.getModel().getAlias(model_row);
    	Object peer_name = this.getModel().getName(model_row);
    	Object peer_slogan = this.getModel().getSlogan(model_row);
    	boolean myPeer = false, otherAbsent = false, otherPresent = false;
    	
    	Object my_peer_GID = this.getModel().getMyselfPeerGID();
    	myPeer = (my_peer_GID!=null) && my_peer_GID.equals(peer_gid);
   	
    	for(String plug_gid : mn.keySet()){
    		if(!myPeer) {
    			if(presentPlugin(this.getModel(), model_row, plug_gid))
    				otherPresent = true;
    			else
    				otherAbsent = true;
    		}
    		popup.addSeparator();
    		D_PluginInfo i = D_PluginInfo.getPluginInfo(plug_gid);
    		String sep, name= null;
    		if (i!=null) name = i.plugin_name;
    		if(name != null) sep = name;
    		else sep = plug_gid;
    		popup.add(new JMenuItem("Plugin: "+sep));
    		PluginMenus pm = mn.get(plug_gid);
     		for(Action pa: pm.plugin_menu_action){
     			
     			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
     			if(myPeer && (pa.getValue(PluginMenus.ROW_MYPEER)!=null)) continue;
     			if(otherAbsent && (pa.getValue(PluginMenus.ROW_OTHERS_ABSENT)!=null)) continue;
     			if(otherPresent && (pa.getValue(PluginMenus.ROW_OTHERS_PRESENT)!=null)) continue;
     			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: -act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
     			
    			pa.putValue(PluginMenus.GID, peer_gid);
    			pa.putValue(PluginMenus.NAME, peer_name);
    			pa.putValue(PluginMenus.ALIAS, peer_alias);
    			pa.putValue(PluginMenus.SLOGAN, peer_slogan);
    			popup.add(pa);
    		}
    		for(JMenuItem ma: pm.plugin_menu_item){
    			Action pa = ma.getAction();
     			if(pa!=null){
         			
         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: mi.act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
        			if(myPeer && (pa.getValue(PluginMenus.ROW_MYPEER)!=null)) continue;
         			if(otherAbsent && (pa.getValue(PluginMenus.ROW_OTHERS_ABSENT)!=null)) continue;
         			if(otherPresent && (pa.getValue(PluginMenus.ROW_OTHERS_PRESENT)!=null)) continue;
         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: -mi.act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
         			
    				pa.putValue(PluginMenus.GID, peer_gid);
        			pa.putValue(PluginMenus.NAME, peer_name);
        			pa.putValue(PluginMenus.ALIAS, peer_alias);
        			pa.putValue(PluginMenus.SLOGAN, peer_slogan);
    			}else{

         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: mi myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
         			if(myPeer && (ma.getClientProperty(PluginMenus.ROW_MYPEER)!=null)) continue;
         			if(otherAbsent && (ma.getClientProperty(PluginMenus.ROW_OTHERS_ABSENT)!=null)) continue;
         			if(otherPresent && (ma.getClientProperty(PluginMenus.ROW_OTHERS_PRESENT)!=null)) continue;
         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: -mi myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
        			    				
    				ma.putClientProperty(PluginMenus.GID, peer_gid);
        			ma.putClientProperty(PluginMenus.NAME, peer_name);
        			ma.putClientProperty(PluginMenus.ALIAS, peer_alias);
        			ma.putClientProperty(PluginMenus.SLOGAN, peer_slogan);
    			}
    			popup.add(ma);
    		}
    	}
	}
	class DispatchPeer extends Thread{
		Peers peers;
		int model_row;
		public DispatchPeer(Peers peers, int model_row) {
			this.peers = peers;
			this.model_row = model_row;
		}

		public void run(){
			if(DEBUG) System.out.println("DispatchPeer:run:start");
			PeersModel model = peers.getModel();
			long id =-1;
			D_PeerAddress peer = null;
			String my_peer_name = null;
			if(model_row>=0){
				id = Util.lval(model.getID(model_row), -1);
			
				try {
					peer = new D_PeerAddress(id, false, true, true);
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
					return;
				}
				my_peer_name = Util.getString(model.getValueAt(model_row, PeersModel.TABLE_COL_NAME));
			}
			if(DEBUG) System.out.println("DispatchPeer:run:listeners=#"+listeners.size());
			for(PeerListener l : listeners) {
				try{l.update(peer, my_peer_name);}catch(Exception e){e.printStackTrace();}
			}
		}
		
	}
	void dispatchToListeners(int model_row){
		new DispatchPeer(this, model_row).start();
	}
//	void updateTerms(int model_row){
//		if(model_row!=-1){
//			termsPanel.setPeerID(Integer.parseInt(this.getModel().getID(model_row)));
//			this.repaint();
//		}
//	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt, boolean release) {
    	int row; //=this.getSelectedRow();
    	int model_row=-1; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        if(DEBUG) System.out.println("Peers:jTableMouseRelease: row="+row);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0)
        	model_row=this.convertRowIndexToModel(row);
        //updateTerms(model_row);
    	if(!evt.isPopupTrigger()){
    		if(release){
    			if(getModel().updates_requested){
    				getModel().updates_requested = false;
    				getModel().__update(null,null);
    				initColumnSizes();
    			}	
    			return;
    		}else{
    			if(DEBUG) System.out.println("Peer:jtableMouseRelease: press");
    	        dispatchToListeners(model_row);
    			return;
    		}
    	}
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup != null)
    		popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
        dispatchToListeners(model_row);
		if(DEBUG) System.out.println("Peer:jtabkeMouseRelease: end");
   }
	public static String getPeerID(String global_peer_ID) throws P2PDDSQLException {
		String result = "-1";
		ArrayList<ArrayList<Object>> dt=Application.db.select("SELECT "+table.peer.peer_ID+" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+" = ?;",
				new String[]{""+global_peer_ID});
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			result = dt.get(0).get(0).toString();
		}
		return result;
	}
	/**
	 * Mark available plugins for this peer in the table, for display 
	 * (only if I also have that plugin)
	 * @param plugins
	 * @param _global_peer_ID
	 * @param _peer_ID
	 */
	public void setPluginsInfo(ASNPluginInfo[] plugins, String _global_peer_ID, String _peer_ID) {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nPeers: setPluginsInfo: learning about available plugins from pID="+_peer_ID +" pGID="+Util.trimmed(_global_peer_ID));
		if(_global_peer_ID == null) {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nPeers: setPluginsInfo: no source peerGID, drop!");
			//Util.printCallPath("setting plugin info from null");
			return;
		}
		pinfo.put(_global_peer_ID, plugins);
		int row;
		row = this.getModel().getRowForPeerID(_peer_ID);
		for (int col = PeersModel.TABLE_COL_PLUGINS; col < this.getModel().getColumnCount(); col++) {
			this.tableChanged(new TableModelEvent(this.getModel(), row, row, col, col));
		}
	}
	static Hashtable<String,ASNPluginInfo[]> pinfo = new Hashtable<String,ASNPluginInfo[]>();


	public void addListener(PeerListener l) {
		if(!listeners.contains(l)) listeners.add(l);
	}
	public void removeListener(PeerListener l) {
		listeners.remove(l);
	}
}
@SuppressWarnings("serial")
class PeersResetAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersResetAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG) System.err.println("PeersResetAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
   		if(DEBUG) System.err.println("PeersResetAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
     	
     	
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	D_PeerAddress.reset(peerID, null, Util.CalendargetInstance());
    }
}
@SuppressWarnings("serial")
class PeersDeleteAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersDeleteAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG)System.err.println("Peers:PeersDeleteAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
       		if(DEBUG)System.err.println("Peers:PeersDeleteAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	int d = Application.ask(_("Are you willing to delete peer:")+" #"+peerID+":"+model.getName(row),
    			_("It is not recommended to delete peers"), JOptionPane.YES_NO_OPTION);
    	if(d!=0) return;
    	try {
			Application.db.deleteNoSync(table.peer_address.TNAME, new String[]{table.peer_address.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer_my_data.TNAME, new String[]{table.peer_my_data.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.org_distribution.TNAME, new String[]{table.org_distribution.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer_org.TNAME, new String[]{table.peer_org.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer_plugin.TNAME, new String[]{table.peer_plugin.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer.TNAME, new String[]{table.peer.peer_ID}, new String[]{peerID}, DEBUG); // never loaded
			Application.db.sync(new ArrayList<String>(Arrays.asList(table.peer.TNAME, table.peer_my_data.TNAME, table.org_distribution.TNAME,
					table.peer_org.TNAME,table.peer_plugin.TNAME, table.peer.TNAME))); //table never loaded
    	} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class PeersUseAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersUseAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG)System.err.println("PeersUseAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
    		if(DEBUG)System.err.println("PeersUseAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	String used="1";
    	if(model.used(row)) used="0";
    	try {
			Application.db.update(table.peer.TNAME, new String[]{table.peer.used}, new String[]{table.peer.peer_ID},
					new String[]{used, peerID}, DEBUG);
			DD.touchClient();
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class PeersFilterAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersFilterAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            System.err.println("PeersUseAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
    		System.err.println("PeersUseAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	String filtered="1";
    	if(model.filtered(row)) filtered="0";
    	try {
			Application.db.update(table.peer.TNAME, new String[]{table.peer.filtered}, new String[]{table.peer.peer_ID},
					new String[]{filtered, peerID}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
class PeerUpdateThread extends Thread {
	PeersModel model;
	PeerUpdateThread(PeersModel _model){
		model = _model;
		start();
	}
	public void run(){
		model.__update(null, null);
	}
}
@SuppressWarnings("serial")
class PeersRowAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon; int command;
    public PeersRowAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;
        this.icon = icon;
        this.command = command;
    }
	public final JFileChooser filterUpdates = new JFileChooser();
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	String peerID;
        if(DEBUG)System.out.println("PeersRowAction:command property: " + command);
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG)System.out.println("PeersRowAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
    		if(DEBUG)System.out.println("PeersRowAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
    	ArrayList<ArrayList<Object>> a;
		switch(command){
		case Peers.COMMAND_MENU_BLOCK:
	    	//PeersModel model = (PeersModel)tree.getModel();
	     	if(row<0) return;
	    	peerID = Util.getString(model.getPeers().get(row).get(0));
	    	String blocked="1";
	    	if(model.blocked(row)) blocked="0";
	    	try {
				Application.db.update(table.peer.TNAME, new String[]{table.peer.blocked}, new String[]{table.peer.peer_ID},
						new String[]{blocked, peerID}, DEBUG);
				DD.touchClient();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			break;
		case Peers.COMMAND_MENU_REFRESH:
			new PeerUpdateThread(model);
			break;
		case Peers.COMMAND_MENU_SHARE_ORG:
			String peer_ID = model.getID(row);
			String org_ID = Application.peers.privateOrgPanel.get_organizationID();
			if(org_ID == null){
				Application.warning(_("No Organization selected"), _("Failure Adding Peer"));
				break;
			}
			try {
				D_OrgDistribution.add(org_ID, peer_ID);
				//((PrivateOrgTable)Application.peers.privateOrgPanel.getTable()).getPModel().update(null,null);
				//((PrivateOrgTable)Application.peers.privateOrgPanel.getTable()).getPModel().fireTableDataChanged();
				//Application.peers.privateOrgPanel.getTable().repaint();
				//Application.peers.privateOrgPanel.privateOrgTableScroll.repaint();
				//Application.peers.privateOrgPanel.repaint();
				//Application.peers.privateOrgPanel.getTable().repaint();
			} catch (P2PDDSQLException e5) {
				e5.printStackTrace();
			}
			break;
		case Peers.COMMAND_TOUCH_CLIENT:
			try {
				DD.touchClient();
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application.warning(_("Error trying to restart comunication:")+"\n "+e4.getLocalizedMessage(), _("Error trying to restart communication!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_NAME:
			try {
				D_PeerAddress.changeMyPeerName(tree);
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application.warning(_("Error trying to change names:")+"\n "+e4.getLocalizedMessage(), _("Error trying to change names!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_SLOGAN:
			try {
				D_PeerAddress.changeMyPeerSlogan(tree);
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application.warning(_("Error trying to change slogan:")+"\n "+e4.getLocalizedMessage(), _("Error trying to change slogan!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_EMAILS:
			try {
				if(_DEBUG)System.out.println("Peer:PeerRowAction: changing emails");
				D_PeerAddress.changeMyPeerEmails(tree);
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application.warning(_("Error trying to change emails:")+"\n "+e4.getLocalizedMessage(), _("Error trying to change emails!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_TERMS:
			TermsPanel termsPanel = new TermsPanel(Integer.parseInt(tree.getModel().getID(row)), tree.getModel().getName(row).toString());
			termsPanel.showJFrame();
//			try {
//			//	D_PeerAddress.changeMyPeerSlogan(tree);
//			} catch (P2PDDSQLException e4) {
//			//	e4.printStackTrace();
//			//	Application.warning(_("Error trying to change slogan:")+"\n "+e4.getLocalizedMessage(), _("Error trying to change slogan!"));
//			}
			break;
		case Peers.COMMAND_MENU_SAVE_SK:
			
			filterUpdates.setFileFilter(new hds.UpdatesFilterKey());
			filterUpdates.setName(_("Select Secret Trusted Key"));
			//filterUpdates.setSelectedFile(null);
			Util.cleanFileSelector(filterUpdates);
			int returnVal = filterUpdates.showDialog(tree,_("Specify Trusted Secret Key File"));
			if (returnVal != JFileChooser.APPROVE_OPTION)  return;
			File fileTrustedSK = filterUpdates.getSelectedFile();
			SK sk;
			PK pk;
			if(fileTrustedSK.exists()) {
				int c = Application.ask(_("Existing file. Overwrite: "+fileTrustedSK+"?"), _("Overwrite file?"), JOptionPane.OK_CANCEL_OPTION);
				if(c!=0) break;
			}
			String gid = model.getGID(row);
			try {
				boolean result = KeyManagement.saveSecretKey(gid, fileTrustedSK.getCanonicalPath());
				if(result) break;
			} catch (P2PDDSQLException e3) {
				Application.warning(_("Failed to save key: "+e3.getMessage()), _("Failed to save key"));
				e3.printStackTrace();
				break;
			} catch (IOException e3) {
				Application.warning(_("Failed to save key: "+e3.getMessage()), _("Failed to save key"));
				e3.printStackTrace();
				break;
			}
			Application.warning(_("Failed to save key: absent"), _("Failed to save key"));
//			String file_dest=JOptionPane.showInputDialog(tree,
//					_("Enter the name of the file"),
//					_("File to save secret key"),
//					JOptionPane.QUESTION_MESSAGE);
			break;
		case Peers.COMMAND_MENU_LOAD_SK:
			filterUpdates.setFileFilter(new hds.UpdatesFilterKey());
			filterUpdates.setName(_("Select Secret Trusted Key"));
			//filterUpdates.setSelectedFile(null);
			Util.cleanFileSelector(filterUpdates);
			int loadVal = filterUpdates.showDialog(tree,_("Specify Trusted Secret Key File"));
			if (loadVal != JFileChooser.APPROVE_OPTION)  return;
			File fileLoadSK = filterUpdates.getSelectedFile();
			if(!fileLoadSK.exists()) {
				Application.warning(_("Inexisting file: "+fileLoadSK.getPath()), _("Inexisting file!"));
				break;
			}
			try{
				String []__pk = new String[1];
				SK new_sk = KeyManagement.loadSecretKey(fileLoadSK.getCanonicalPath(), __pk);
				String old_gid = model.getGID(row);
				String _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
				D_PeerAddress peer = new D_PeerAddress(old_gid);
				peer.component_basic_data.globalID = _pk;
				peer.component_basic_data.globalIDhash=null;
				peer._peer_ID = -1;
				peer.peer_ID = null;
				peer.sign(new_sk);
				peer.storeVerified();
			}catch(Exception e2){}
			break;
    	case Peers.COMMAND_MENU_NEW_MYSELF:
    		//
    		try {
				D_PeerAddress.createMyPeerID();
			} catch (P2PDDSQLException e2) {
				e2.printStackTrace();
			}
			break;
    	case Peers.COMMAND_REFRESH_VIEWS:
			model.fireTableDataChanged();
    		break;
    	case Peers.COMMAND_MENU_NEW_PLUGINS:
    		//PluginRegistration.removePlugins();
    		new Thread() {
    			public void run(){
    				PluginRegistration.loadNewPlugins();
    			}
    		}.start();
    		break;
    	case Peers.COMMAND_MENU_SET_MYSELF:
    		if(row<0) return;
    		peerID = Util.getString(model.getPeers().get(row).get(0));
    		try {
				D_PeerAddress.setMyself(peerID);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				Application.warning(_("Failure to set new ID:")+"\n"+e1.getLocalizedMessage(), _("Failure in message!"));
			}
    		break;
    	default:
    	}
    	/*
    	String filtered="1";
    	if(model.filtered(row)) filtered="0";
    	try {
			Application.db.update(table.peer.TNAME, new String[]{table.peer.filtered}, new String[]{table.peer.peer_ID},
					new String[]{filtered, peerID}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		*/
    }
}
@SuppressWarnings("serial")
class PeersBroadcastAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon; boolean _default;
	/**
	 * _default se if we want to use peers.broadcastable vaue
	 * @param tree
	 * @param text
	 * @param icon
	 * @param desc
	 * @param whatis
	 * @param mnemonic
	 * @param _default
	 */
    public PeersBroadcastAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, boolean _default) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this._default=_default;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG) System.err.println("PeersBroadcastAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
   		if(DEBUG) System.err.println("PeersBroadcastAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	String broadcastable="1";
    	if(_default) broadcastable=null;
    	else if(model.broadcastable(row)) broadcastable="0";
    	try {
    		String sql="SELECT "+table.peer_my_data.broadcastable+" FROM "+table.peer_my_data.TNAME+" WHERE "+table.peer_my_data.peer_ID+"=?;";
    		ArrayList<ArrayList<Object>> m = Application.db.select(sql,new String[]{peerID}, DEBUG);
    		if(m.size()>0)
    			Application.db.update(table.peer_my_data.TNAME, new String[]{table.peer_my_data.broadcastable}, new String[]{table.peer_my_data.peer_ID},
    					new String[]{broadcastable, peerID}, DEBUG);
    		else
    			Application.db.insert(table.peer_my_data.TNAME, new String[]{table.peer_my_data.peer_ID,table.peer_my_data.broadcastable},
    					new String[]{peerID, broadcastable}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class ColorRenderer extends JLabel implements TableCellRenderer {
	PeersModel model;
	public ColorRenderer(PeersModel _model) {
		super();
		model = _model;
		setOpaque(true);
	}
	@Override
	public Component getTableCellRendererComponent(JTable table, Object obj,
			boolean isSelected, boolean hasFocus, int row, int column) {
		row=table.convertRowIndexToModel(row);
		if(model.broadcastable(row)) {
			setBackground(Color.WHITE);
		}else{
			setBackground(Color.LIGHT_GRAY);		
		}
		
		if(model.used(row)) {
			//setBackground(Color.WHITE);
			this.setForeground(Color.green);
			this.setText(Util.getString(obj));
			//System.out.println("Row="+row+" Col="+column+"  val="+obj);
		}else{
			//setBackground(Color.WHITE);
			this.setForeground(Color.BLACK);		
			this.setText(Util.getString(obj));
		}
		//setToolTipText("");
		return this;
	}	
}
/**
 * 
 */
@SuppressWarnings("serial")
class PeersModel extends AbstractTableModel implements TableModel, DBListener {
	static final int COL_NAT = 3;
	static final int COL_UDP_ON = 5;
	static final int COL_TCP_ON = 6;

	static int idx=0;
	public static final int TABLE_COL_NAME = 0;//idx++;
	public static final int TABLE_COL_VERIF_NAME = 1;//idx++;
	public static final int TABLE_COL_SERVING = 2;//idx++;
	public static final int TABLE_COL_CONNECTION = 3;//idx++;
	public static final int TABLE_COL_BLOCKED = 4;//idx++;
	public static final int TABLE_COL_HIDDEN = 5;//idx++;
	public static final int TABLE_COL_VALID = 6;//idx++;
	public static final int TABLE_COL_TEMP = 7;//idx++;
	public static final int TABLE_COL_REVOKED = 8;//idx++;
	public static final int TABLE_COL_EMAIL = 9;//idx++;
	public static final int TABLE_COL_VERIF_EMAIL = 10;//idx++;
	public static final int TABLE_COL_CATEGORY = 11;//idx++;
	public static final int TABLE_COL_SLOGAN = 12;//idx++;
	public static final int TABLE_COL_PROVIDER = 13;//idx++;
	public static final int TABLE_COL_LAST = 14;//idx++;
	public static final int TABLE_COL_PLUGINS = 15;//idx++;

	static final int SELECT_COL_ID = 0;
	static final int SELECT_COL_NAME = 1;
	static final int SELECT_COL_SLOGAN = 6;
	static final int SELECT_COL_USED = 7;
	static final int SELECT_COL_PICTURE = 8;
	static final int SELECT_COL_BLOCKED = 9;
	static final int SELECT_COL_BROADCASTABLE = 2;
	private static final int SELECT_COL_FILTERED = 12;
	static final int SELECT_COL_M_ID = 13;
	static final int SELECT_COL_M_NAME = 14;
	static final int SELECT_COL_M_SLOGAN = 15;
	static final int SELECT_COL_M_PICTURE = 16;
	static final int SELECT_COL_M_BROADCASTABLE = 17;
	static final int SELECT_COL_GID = 18;
	static final int SELECT_COL_M_TOPIC = 19;
	static final int SELECT_COL_HIDDEN = 20;
	static final int SELECT_COL_REVOKED = 21;
	static final int SELECT_COL_REVOKATION_INSTR = 22;
	static final int SELECT_COL_EMAIL_VERIF = 23;
	static final int SELECT_COL_NAME_VERIF = 24;
	static final int SELECT_COL_CATEG = 25;
	static final int SELECT_COL_EMAIL = 26;
	static final int SELECT_COL_SIGN = 27;
	static final int SELECT_COL_PROVIDER = 28;
	static final int SELECT_COL_LAST = 29;

	static final String select_fields =
		" p."+table.peer.peer_ID+
		", p."+table.peer.name+
		", p."+table.peer.broadcastable+
		", p."+table.peer.last_sync_date+
		", p."+table.peer.arrival_date+
		", p."+table.peer.creation_date+
		", p."+table.peer.slogan+
		", p."+table.peer.used+
		", p."+table.peer.picture+
		", p."+table.peer.blocked+
		", p."+table.peer.exp_avg+
		", p."+table.peer.experience+
		", p."+table.peer.filtered+
		", m."+table.peer_my_data.peer_ID+
		", m."+table.peer_my_data.name+
		", m."+table.peer_my_data.slogan+
		", m."+table.peer_my_data.picture+
		", m."+table.peer_my_data.broadcastable+
		", p."+table.peer.global_peer_ID+
		", m."+table.peer_my_data.my_topic+
		", p."+table.peer.hidden+
		", p."+table.peer.revoked+
		", p."+table.peer.revokation_instructions+
		", p."+table.peer.email_verified+
		", p."+table.peer.name_verified+
		", p."+table.peer.category+
		", p."+table.peer.emails+
		", p."+table.peer.signature+
		", p."+table.peer.first_provider_peer+
		", p."+table.peer.last_sync_date
		;

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final Object peers_monitor = new Object();
	Hashtable<String,Boolean> natByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Boolean> onUDPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Boolean> onTCPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Integer> rowByIPport = new Hashtable<String,Integer>();
	Hashtable<String,Integer> __rowByPeerID = new Hashtable<String,Integer>();
	Hashtable<String,Integer> connTCPByID = new Hashtable<String,Integer>();
	DBInterface db;
	//String ld;	//-
	//String _ld[];  //-
	ArrayList<ArrayList<Object>> __peers;
	
	String columnNames[]={"Peer Data","V","Serving","Connection","B","H","S","T","R","Email","V","Categ","Slogan","Provider","LastSync","Pluggins"};
	Hashtable<Integer,String> columnNamesHash=new Hashtable<Integer,String>();
	int plugin_applets = 0;
	int columns = columnNames.length-1 + plugin_applets;
	ArrayList<TableCellRenderer> appletRenderers;
	ArrayList<TableCellEditor> appletEditors;
	Peers p_table;
	public boolean updates_requested;
	//private String[] _peers_ID;
	public void setTable(Peers _table){
		p_table = _table;
	}
	public int getRowForPeerID(String _peer_ID) {
		Integer _rowByPeerID = getRowByPeerID().get(_peer_ID);
		if(_rowByPeerID == null) return -1;
		return _rowByPeerID.intValue();
	}
	public String getMyselfPeerGID() {
		return DD.getMyPeerGIDFromIdentity();
	}
	public String getMyselfPeerID() {
		try {
			return table.peer.getLocalPeerID(DD.getMyPeerGIDFromIdentity());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public String getID(int model_row) {
		return Util.getString(getPeers().get(model_row).get(SELECT_COL_ID));
	}
	public Object getSlogan(int model_row) {
		return Util.getString(getPeers().get(model_row).get(SELECT_COL_SLOGAN));
	}
	public Object getName(int model_row) {
		return Util.getString(getPeers().get(model_row).get(SELECT_COL_NAME));
	}
	public Object getAlias(int model_row) {
		return Util.getString(getPeers().get(model_row).get(SELECT_COL_M_NAME));
	}
	/**
	 * Get GID for plugins
	 * @param model_row
	 * @return
	 */
	public String getGID(int model_row) {
		return Util.getString(getPeers().get(model_row).get(SELECT_COL_GID));
	}
	public boolean broadcastable_default(int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:broadcastable: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_M_BROADCASTABLE){
			if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE cols>="+ao.size());
			return true;
		}
		boolean result = true;
		Object _broadcastable_m = ao.get(SELECT_COL_M_BROADCASTABLE);
		if ("1".equals(Util.getString(_broadcastable_m))) result = false;
		else if ("0".equals(Util.getString(_broadcastable_m))) result = false;
		return result;
	}
	/**
	 * Checks both peers_my_data and peers
	 * @param row
	 * @return
	 */
	public boolean broadcastable(int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor){
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:broadcastable: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if((ao.size()<=SELECT_COL_BROADCASTABLE) && (ao.size()<=SELECT_COL_M_BROADCASTABLE)){
			if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE cols>="+ao.size());
			return false;
		}
		boolean result;
		Object _broadcastable_m = ao.get(SELECT_COL_M_BROADCASTABLE);
		if ("1".equals(Util.getString(_broadcastable_m))) result = true;
		else if ("0".equals(Util.getString(_broadcastable_m))) result = false;
		else {
			Object _broadcastable = ao.get(SELECT_COL_BROADCASTABLE);
			if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE val=\""+_broadcastable+"\"");
			result = "1".equals(Util.getString(_broadcastable));
		}
		if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE val="+result);
		return result;
	}
	public boolean filtered(int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:filtered: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_FILTERED){
			if(DEBUG) System.out.println("PeersModel:filtered: FILTERED cols>="+ao.size());
			return false;
		}
		Object _filtered = ao.get(SELECT_COL_FILTERED);
		if(DEBUG) System.out.println("PeersModel:filtered: FILTERED val=\""+_filtered+"\"");
		boolean result = "1".equals(Util.getString(_filtered));
		if(DEBUG) System.out.println("PeersModel:filtered: FILTERED val="+result);
		return result;
	}
	public boolean blocked(int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:used: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_BLOCKED){
			if(DEBUG) System.out.println("PeersModel:used: USED cols>="+ao.size());
			return false;
		}
		Object _used = ao.get(SELECT_COL_BLOCKED);
		if(DEBUG) System.out.println("PeersModel:used: USED val=\""+_used+"\"");
		boolean result = Util.stringInt2bool(_used, false);
		if(DEBUG) System.out.println("PeersModel:used: USED val="+result);
		return result;
	}
	public boolean used(int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:used: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_USED){
			if(DEBUG) System.out.println("PeersModel:used: USED cols>="+ao.size());
			return false;
		}
		Object _used = ao.get(SELECT_COL_USED);
		if(DEBUG) System.out.println("PeersModel:used: USED val=\""+_used+"\"");
		boolean result = "1".equals(Util.getString(_used));
		if(DEBUG) System.out.println("PeersModel:used: USED val="+result);
		return result;
	}
	PeersModel(DBInterface _db) {
		db = _db;
		
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.peer.TNAME, table.peer_address.TNAME, table.peer_my_data.TNAME, table.peer_org.TNAME)),
				DBSelector.getHashTable(table.peer.TNAME, table.peer.used, "0"));
		__update(null, null);
	}
	@Override
	public int getColumnCount() {
		if(DEBUG) System.out.println("PeersModel:getColumnCount:columns="+columnNames.length);
		if(p_table == null) return columnNames.length;
		return columnNames.length+D_PluginInfo.plugins.size()-1;
	}
	public void addColumn(int col, String string) {
		if(DEBUG) System.out.println("PeersModel:addColumn: ["+col+"]="+string);
		columnNamesHash.put(new Integer(""+col), string);
	}
	@Override
	public String getColumnName(int col) {
		String result = null;
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]=?");
		if(columnNames.length-1<=col){
			result = columnNamesHash.get(new Integer(""+col));
			if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"/"+columnNames.length+"]="+result);
		}
		if((result == null)&&(col<columnNames.length)){
			result = columnNames[col].toString();
			if(DEBUG) System.out.println("PeersModel:getColumnName: colNames Header["+col+"/"+columnNames.length+"]="+result);
		}
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]="+result);
		return result;
	}
	private String getPluginGIDcol(int col) {
		int plugin_col = col - columnNames.length+1;
		if(plugin_col<0) return null;
		if(plugin_col>=D_PluginInfo.plugins.size()) return null;
		return D_PluginInfo.plugins.get(plugin_col);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == this.TABLE_COL_BLOCKED) return Boolean.class;
		if(col == this.TABLE_COL_HIDDEN) return Boolean.class;
		if(col == this.TABLE_COL_REVOKED) return Boolean.class;
		if(col == this.TABLE_COL_VERIF_EMAIL) return Boolean.class;
		if(col == this.TABLE_COL_VERIF_NAME) return Boolean.class;
		if(col == this.TABLE_COL_TEMP) return Boolean.class;
		if(col == this.TABLE_COL_VALID) return Boolean.class;
			return String.class;
	}		
	@Override
	public int getRowCount() {
		synchronized(this.peers_monitor){
			if(getPeers()==null){
				if(DEBUG) System.out.println("PeersModel:getRowCount:rows=00");
				return 0;
			}
			if(DEBUG) System.out.println("PeersModel:getRowCount:rows="+getPeers().size());
			return getPeers().size();
		}
	}
	@Override
	public Object getValueAt(int row, int col) {
		String peerID;
		ArrayList<Object> ao;
		synchronized(this.peers_monitor){
			if((getPeers()==null)||(getPeers().size() <= row)){
				if(DEBUG) System.out.println("PeersModel:getValueAt:No peer at: row="+row);
				return null;
			}
			ao = getPeers().get(row);
		}		
		if(col>=TABLE_COL_PLUGINS){
			peerID = Util.getString(ao.get(0));
			//return peerID;
			if(Peers.presentPlugin(this, row, this.getPluginGIDcol(col))) return peerID;
			return null;
		}
		//if((col==0)&&(row==0))  Util.printCallPath("No");
		switch(col){
		case TABLE_COL_LAST:
			peerID = Util.getString(ao.get(0));
			return Util.getString(ao.get(this.SELECT_COL_LAST));
		case TABLE_COL_NAME:
			if((ao.size() > SELECT_COL_M_NAME) &&
					(ao.get(SELECT_COL_M_NAME)!=null) &&
					!"".equals(ao.get(SELECT_COL_M_NAME)) )
				col = SELECT_COL_M_NAME;
			else col = SELECT_COL_NAME;
			break;
		case TABLE_COL_SLOGAN:
			if((ao.size() > SELECT_COL_M_SLOGAN) && 
					(ao.get(SELECT_COL_M_SLOGAN)!=null) &&
					!"".equals(ao.get(SELECT_COL_M_SLOGAN)) )
				col = SELECT_COL_M_SLOGAN;
			else col = SELECT_COL_SLOGAN;
			break;
		case TABLE_COL_SERVING:
			peerID = Util.getString(ao.get(0));
			String[] params = new String[]{peerID};
			ArrayList<ArrayList<Object>> orgs;
			try {
				orgs = Application.db.select("SELECT p."+table.peer_org.organization_ID+", o."+table.organization.name +
						" FROM "+table.peer_org.TNAME+" AS p " +
						" LEFT JOIN "+table.organization.TNAME+" AS o ON (p."+table.peer_org.organization_ID+" = o."+table.organization.organization_ID+") " +
						" WHERE p."+table.peer_org.peer_ID+" = ? AND o."+table.organization.name+" IS NOT NULL;", params, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return null;
			}
			if ((orgs == null) || (orgs.size()==0)) return _("Nothing");
			if(orgs.size()==1) return orgs.get(0).get(1);
			JComboBox<Object> ed = new JComboBox<Object>(Util.getColumn(orgs,1).toArray());
			return ed;//orgs.get(0).get(1)+" (/#"+orgs.size()+")";
			//break;
		case TABLE_COL_CONNECTION:
			peerID = Util.getString(ao.get(0));
			return this.connTCPByID.get(peerID);
		case TABLE_COL_BLOCKED:
			peerID = Util.getString(ao.get(0));
			return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_BLOCKED), false));
		case TABLE_COL_HIDDEN:
			peerID = Util.getString(ao.get(0));
			return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_HIDDEN), false));
		case TABLE_COL_PROVIDER:
			peerID = Util.getString(ao.get(0));
			return D_PeerAddress.getDisplayName(Util.lval(ao.get(this.SELECT_COL_PROVIDER)));
		case TABLE_COL_VALID:
			peerID = Util.getString(ao.get(0));
			return new Boolean(D_PeerAddress.checkValid(Util.lval(peerID)));
		case TABLE_COL_TEMP:
			peerID = Util.getString(ao.get(0));
			return new Boolean(ao.get(this.SELECT_COL_SIGN)==null);
		case TABLE_COL_REVOKED:
			peerID = Util.getString(ao.get(0));
			return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_REVOKED), false));
		case TABLE_COL_VERIF_EMAIL:
			peerID = Util.getString(ao.get(0));
			return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_EMAIL_VERIF), false));
		case TABLE_COL_VERIF_NAME:
			peerID = Util.getString(ao.get(0));
			return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_NAME_VERIF), false));
		case TABLE_COL_EMAIL:
			peerID = Util.getString(ao.get(0));
			return ao.get(this.SELECT_COL_EMAIL);
		case TABLE_COL_CATEGORY:
			peerID = Util.getString(ao.get(0));
			return ao.get(this.SELECT_COL_CATEG);
		default:
			return null;	
		}
		/*
		if(col == COL_UDP_ON)return this.onUDPByIPport.get(this.ipPort(row));
		if(col == COL_TCP_ON)return this.onTCPByIPport.get(this.ipPort(row));
		if(col == COL_NAT) return this.natByIPport.get(this.ipPort(row));
		*/
		if(ao.size() <= col){
			if(DEBUG) System.out.println("PeersModel:getValueAt:No peer at: row="+row+" col="+col);
			return null;
		}
		//boolean used = "1".equals(Util.getString(peers.get(row).get(SELECT_COL_USED)));
		String el = Util.getString(ao.get(col));
		//if((el!=null) && (used)) el = "<html><font color='green'>"+el+"</font></html>";
		if(DEBUG) System.out.println("PeersModel:getValueAt:Peer at: row="+row+" col="+col+" val="+el);		
		return el;
	}
	@Override
	public boolean isCellEditable(int row, int col) {
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_CATEGORY:
		case TABLE_COL_SLOGAN:
		case TABLE_COL_BLOCKED:
		case TABLE_COL_HIDDEN:
		case TABLE_COL_VERIF_NAME:
		case TABLE_COL_VERIF_EMAIL:
		case TABLE_COL_TEMP:
		case TABLE_COL_VALID:
		case TABLE_COL_SERVING:
			return true;
		}
		return false;
	}
	public void set_my_data(String field, int SELECT_COL_M, String value, int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if(row >= getPeers().size()) return;
			if(SELECT_COL_ID >= getPeers().get(row).size()) return;
			ao = getPeers().get(row);
		}
		String peer_ID = Util.getString(ao.get(SELECT_COL_ID));
		if((SELECT_COL_M_ID >= ao.size())||(ao.get(SELECT_COL_M_ID)==null)) {
				try {
					db.insert(table.peer_my_data.TNAME, new String[]{table.peer_my_data.peer_ID,field},
							new String[]{peer_ID, value}, DEBUG);
					ao.set(SELECT_COL_M_ID, peer_ID);
					ao.set(SELECT_COL_M, value);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
		} else {
				try {
					db.update(table.peer_my_data.TNAME, new String[]{field}, new String[]{table.peer_my_data.peer_ID},
							new String[]{value, peer_ID}, DEBUG);
					ao.set(SELECT_COL_M, Util.getString(value));
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}					
		}		
	}
	public void set_data(String field, int SELECT_COL, String value, int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if(row >= getPeers().size()) return;
			if(SELECT_COL_ID >= getPeers().get(row).size()) return;
			ao = getPeers().get(row);
		}
		String peer_ID = Util.getString(ao.get(SELECT_COL_ID));
		try {
			db.update(table.peer.TNAME, new String[]{field},
					new String[]{table.peer.peer_ID},
					new String[]{value, peer_ID}, DEBUG);
			ao.set(SELECT_COL, Util.getString(value));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}					
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		switch(col) {
		case TABLE_COL_NAME:
			set_my_data(table.peer_my_data.name, SELECT_COL_M_NAME, Util.getString(value), row);
			break;
		case TABLE_COL_BLOCKED:
			set_data(table.peer.blocked, SELECT_COL_BLOCKED, Util.getString(value), row);
			break;
		case TABLE_COL_CATEGORY:
			set_data(table.peer.category, SELECT_COL_CATEG, Util.getString(value), row);
			break;
		case TABLE_COL_HIDDEN:
			set_data(table.peer.hidden, SELECT_COL_HIDDEN, Util.getString(value), row);
			break;
		case TABLE_COL_TEMP:
			if(0 == Application.ask(_("Do you really want to drop signature for:"+" "+getName(row)),
					_("Are you sure?"), JOptionPane.OK_CANCEL_OPTION)){
				String _value = null;
				set_data(table.peer.signature, SELECT_COL_SIGN, _value, row);
			}
			break;
		case TABLE_COL_VALID:
			if(0 == Application.ask(_("Do you really want to attempt re-signature for:"+" "+getName(row)),
					_("Are you sure?"), JOptionPane.OK_CANCEL_OPTION)){
				long ID = Util.lval(getID(row));
				try {
					D_PeerAddress.readSignSave(ID, ID, true);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
			break;
		case TABLE_COL_VERIF_NAME:
			set_data(table.peer.name_verified, SELECT_COL_NAME_VERIF, Util.getString(value), row);
			break;
		case TABLE_COL_SLOGAN:
			set_my_data(table.peer_my_data.slogan, SELECT_COL_M_SLOGAN, Util.getString(value), row);
			break;
		}
		/*
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP) >= 0) return; 
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_SEP) >= 0) return; 
		String el[] = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		String result="";
		for(int k=0; k<columns; k++) {
			if(k > 0) result = result + DD.APP_LISTING_DIRECTORIES_ELEM_SEP;
			if(k==col) result = result + value;
			else if(k<el.length) result = result + el[k];
			else result = result+"";
		}
		_ld[row] = result;
		try {
			String dirs = Util.concat(_ld, DD.APP_LISTING_DIRECTORIES_SEP);
			if(DEBUG)System.out.println("Directories: setValueAt: Setting "+dirs);
			DD.setAppTextNoSync(DD.APP_LISTING_DIRECTORIES, dirs);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		*/
		fireTableCellUpdated(row, col);
	}
	public void setConnectionState(String peerID, int state) {
		this.connTCPByID.put(peerID, new Integer(state));
	}
	/*
	String ipPort(int k) {
		if((_ld==null)||(_ld.length<=k)) return null;
		int id1 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		if(id1<0) return null;
		int id2 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP,id1+1);
		if(id2<0) return _ld[k];//id2 = _ld[k].length();
		return _ld[k].substring(0, id2);
	}
	void setUDPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setUDPOn:"+address+" at "+on);
		onUDPByIPport.put(address, on);
		int row = this.rowByIPport.get(address).intValue();
		this.fireTableCellUpdated(row, COL_UDP_ON);
	}
	void setTCPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setTCPOn:"+address+" at "+on);
		onTCPByIPport.put(address, on);
		int row = this.rowByIPport.get(address).intValue();
		this.fireTableCellUpdated(row, COL_TCP_ON);
	}
	void setNATOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setNATOn:"+address+" at "+on);
		natByIPport.put(address, on);
		int row = this.rowByIPport.get(address).intValue();
		this.fireTableCellUpdated(row, COL_NAT);
	}
	*/
	@Override
	public void update(ArrayList<String> a_table, Hashtable<String,DBInfo> info) {
		if(DEBUG) System.out.println("Peers:update:peers start");
		updates_requested = true;
	}
	public void __update(ArrayList<String> a_table, Hashtable<String,DBInfo> info) {
		if(DEBUG) System.out.println("Peers:update:peers start");
		ArrayList<ArrayList<Object>> _peers;
		try {
			String sql = "SELECT "+select_fields+ 
					" FROM "+table.peer.TNAME+" AS p " +
					" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON(p."+table.peer.peer_ID+"=m."+table.peer_my_data.peer_ID+") "+ 
					" ORDER BY "+ table.peer.used+" DESC, "+table.peer.last_sync_date+" DESC; ";
			String[] params = new String[0];
			_peers = Application.db.select(sql, params, DEBUG);
/*			
			if(ld!=null) {
				_ld=ld.split(DD.APP_LISTING_DIRECTORIES_SEP);
				for(int k=0; k<_ld.length; k++) {
					if(DEBUG)System.out.println("Directories:update:"+k+" is "+_ld[k]);
					String ipPort = ipPort(k);
					if(ipPort == null) continue;
					this.rowByIPport.put(ipPort, new Integer(k));
				}
			}
			*/
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		Hashtable<String,Integer> _rowByPeerID = new Hashtable<String,Integer>();
		if(DEBUG) System.out.println("Peers:update:peers#="+_peers.size());
		//_peers_ID = new String[peers.size()];
		for(int i=0; i<_peers.size(); i++) {
			_rowByPeerID.put(Util.getString(_peers.get(i).get(SELECT_COL_ID)), new Integer(i));
			//_peers_ID[i] = Util.getString(peers.get(i).get(SELECT_COL_ID));
		}
		synchronized(this.peers_monitor){
			__peers = _peers;
			__rowByPeerID = _rowByPeerID;
		}
		//this.fireTableStructureChanged();
		try{
			this.fireTableDataChanged();
		}catch(Exception e){if(DEBUG)e.printStackTrace();}
		new TableUpdater(this, p_table, null);
		//this.fireTableDataChanged();
		if(DEBUG) System.out.println("Peers:update:peers done");
	}
	ArrayList<ArrayList<Object>> getPeers(){
		synchronized(this.peers_monitor) {
			return __peers;
		}
	}
	Hashtable<String, Integer> getRowByPeerID(){
		synchronized(this.peers_monitor) {
			return __rowByPeerID;
		}
	}
/*
	@Override
	public void addTableModelListener(TableModelListener arg0) {		
	}


	@Override
	public void removeTableModelListener(TableModelListener arg0) {
		// 
	}
	*/
}
class PeersTest extends JPanel {
    Peers tree;
    public PeersTest(DBInterface db) {
    	super(new BorderLayout());
    	tree = new Peers( new PeersModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        add(scrollPane, BorderLayout.CENTER);
		//JScrollPane scrollPane = new JScrollPane(dirs);
		tree.setFillsViewportHeight(true);
    }
    public static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Directories Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PeersTest newContentPane = new PeersTest(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
}

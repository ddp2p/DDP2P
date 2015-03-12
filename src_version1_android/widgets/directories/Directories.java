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
package widgets.directories;

import hds.Address;
import hds.ClientSync;
import hds.Connections;
import hds.DirectoryAnswer;
import hds.DirectoryAnswerInstance;
import hds.DirectoryAnswerMultipleIdentities;
import hds.DirectoryRequest;
import hds.DirectoryServer;
import hds.Server;
import hds.UDPServer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import ASN1.Decoder;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Directories_View;
import config.Identity;
import data.D_Peer;
import data.HandlingMyself_Peer;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.DirectoryAddress;
import util.Util;
import widgets.app.DDIcons;
import widgets.app.ThreadsAccounting;
import widgets.components.BulletRenderer;
import widgets.components.DebateDecideAction;
import widgets.components.XTableColumnModel;
import static java.lang.System.out;
import static util.Util._;
@SuppressWarnings("serial")
class PathCellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(
                        JTable table, Object value,
                        boolean isSelected, boolean hasFocus,
                        int row, int column) {
        JLabel c = (JLabel)super.getTableCellRendererComponent( table, value,
                isSelected, hasFocus, row, column );
        String pathValue = Util.getString(value);
        c.setToolTipText(pathValue);
        // ...OR this probably works in your case:
        //c.setToolTipText(c.getText());
        return c;
    }
}

@SuppressWarnings("serial")
public class Directories extends JTable implements MouseListener, Directories_View {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	PathCellRenderer pathRenderer = new PathCellRenderer();
	BulletRenderer bulletRenderer = new BulletRenderer();
	MyDirComboBoxRenderer myDirComboBoxRenderer;
	private XTableColumnModel yourColumnModel;
	public Directories() {
		super(new DirectoriesModel(Application.db));
		getModel().setTable(this);
		init();
	}
	public Directories(DBInterface _db) {
		super(new DirectoriesModel(_db));
		getModel().setTable(this);
		init();
	}
	public Directories(DirectoriesModel dm) {
		super(dm);
		init();
	}
	
	//Implement table cell tool tips.
    public String getToolTipText(MouseEvent e) {
        String tip = null;
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex < 0) return null;
        int colIndex = columnAtPoint(p);
        int realColumnIndex = -1;
        int realRowIndex = -1;
        try {
	        realColumnIndex = convertColumnIndexToModel(colIndex);
	        realRowIndex = convertRowIndexToModel(rowIndex);
        } catch (Exception ex) {
        	System.out.println("Directories.toToolTipText: "+rowIndex+","+colIndex);
        	ex.printStackTrace();
        	return null;
        }

        if (realColumnIndex == DirectoriesModel.COL_TCP_ON) {
        	tip = getModel().getTCP_ON_Tip(realRowIndex, realColumnIndex);
        }
        if (realColumnIndex == DirectoriesModel.COL_ADDRESS) {
            //JComboBox<Object> j = (JComboBox<Object>) getModel().getValueAt(realRowIndex, realColumnIndex);
            //if (j == null) return null;
            tip = Util.getString(getModel().getAddressValueAt(realRowIndex, realColumnIndex));
        }
        if (
        	    (realColumnIndex == DirectoriesModel.COL_IP)
        	    || (realColumnIndex == DirectoriesModel.COL_DATE)
        	    || (realColumnIndex == DirectoriesModel.COL_NAME)
        	    || (realColumnIndex == DirectoriesModel.COL_BRANCH)
        	    || (realColumnIndex == DirectoriesModel.COL_VERSION)
        	    || (realColumnIndex == DirectoriesModel.COL_PORT_TCP)
        		)
        { //Sport column
            // tip = Util.getString(getValueAt(rowIndex, colIndex));
            tip = Util.getString(getModel().getValueAt(realRowIndex, realColumnIndex));
        }
        return tip;
    }
	
	void init() {
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		myDirComboBoxRenderer = new MyDirComboBoxRenderer();

		yourColumnModel = new widgets.components.XTableColumnModel();
		setColumnModel(yourColumnModel); 
		createDefaultColumnsFromModel(); 

		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);	
		this.addMouseListener(this);
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 200));
        //jp.add(scrollPane, BorderLayout.CENTER);
        Application.directoriesData = new DirectoriesData();
        //jp.add(Application.directoriesData, BorderLayout.SOUTH);
        jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, ((DirectoriesData)Application.directoriesData)), BorderLayout.CENTER);
		return jp;
    }

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
	   Object value = super.getValueAt(row, column);
	   if (value != null) {
	      if(value instanceof JComboBox) {
	           return new DefaultCellEditor((JComboBox)value);
	      }
	            return getDefaultEditor(value.getClass());
	   }
	   return super.getCellEditor(row, column);
	}
	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == DirectoriesModel.COL_UDP_ON)) return bulletRenderer;
		if ((column == DirectoriesModel.COL_TCP_ON)) return bulletRenderer;
		if ((column == DirectoriesModel.COL_ADDRESS)) return this.myDirComboBoxRenderer;//pathRenderer;
		if ((column == DirectoriesModel.COL_DATE)) return pathRenderer;
		//if ((column == DirectoriesModel.COL_NAT)) return bulletRenderer;
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {null,null,_("A name you provide"),
			_("Branch"), _("Latest Supported Version"),
			_("Is it behind a NAT?"), _("Does it offer relay service"),
			_("UDP answered"), _("TCP answered"), _("UDP answered"),
			_("Served address"), _("Date last answer"), _("Use?")};
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
	public void setUDPOn(String address, Boolean on){
		if (DEBUG) System.out.println("Directories:setUDPOn:"+address+" "+on);
		((DirectoriesModel) this.getModel()).setUDPOn(address, on);
	}
	public void setTCPOn(String address, Boolean on, Exception e){
		if (DEBUG) System.out.println("Directories:setTCPOn:"+address+" "+on);
		((DirectoriesModel) this.getModel()).setTCPOn(address, on, e);
	}
	public void setNATOn(String address, Boolean on){
		((DirectoriesModel) this.getModel()).setNATOn(address, on);
	}
	public DirectoriesModel getModel(){
		return (DirectoriesModel) super.getModel();
	}
	/**
	 * Call this to remove a current column
	 * @param crt_col
	 */
	public void removeColumn(int crt_col){
		TableColumn column  = this.yourColumnModel.getColumn(crt_col);
		yourColumnModel.setColumnVisible(column, false);
	}
	public void addColumn(int crt_col){
		TableColumn column  = this.yourColumnModel.getColumnByModelIndex(crt_col);
		yourColumnModel.setColumnVisible(column, true);
	}
	void initColumnSizes() {
        DirectoriesModel model = (DirectoriesModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
        
        TableColumnModel cm = this.getColumnModel();
        int cnt = cm.getColumnCount(); // model.getColumnCount();
        for (int i = 0; i < cnt; i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
            column = cm.getColumn(i);
 
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
	/**
	 * @param args
	 * @throws P2PDDSQLException 
	 */
	public static void main(String[] args) throws P2PDDSQLException {
		String dfname = Application.DELIBERATION_FILE;
		Application.db = new DBInterface(dfname);
		//DirectoriesTest dT = new DirectoriesTest(Application.db);
		//DirectoriesModel dirsM = new DirectoriesModel(Application.db);
		//Directories dirs = new Directories(dirsM);
		//JScrollPane scrollPane = new JScrollPane(dirs);
		//dirs.setFillsViewportHeight(true);
		DirectoriesTest.createAndShowGUI(Application.db);
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
	public void mousePressed(MouseEvent e) {
		jMouse(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jMouse(e);
	}
	private void jMouse(MouseEvent evt) {
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
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	DirectoriesModel model = getModel();
    	DirectoriesCustomAction cAction;
    	
    	cAction = new DirectoriesCustomAction(this, _("Refresh!"), addicon,_("Refresh."), _("Refresh"),KeyEvent.VK_R, DirectoriesCustomAction.C_REFRESH);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new DirectoriesCustomAction(this, _("Remove Column!"), delicon,_("Remove Column."), _("Remove Column"), KeyEvent.VK_C, DirectoriesCustomAction.C_RCOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new DirectoriesCustomAction(this, _("Add Column!"), addicon,_("Add Column."), _("Add Column"), KeyEvent.VK_A, DirectoriesCustomAction.C_ACOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));

    	cAction = new DirectoriesCustomAction(this, _("Add Directory!"), addicon,_("Add Directory."), _("Add Directory"), KeyEvent.VK_Y, DirectoriesCustomAction.C_ADIR);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));

    	cAction = new DirectoriesCustomAction(this, _("Del Directory!"), delicon,_("Del Directory."), _("Del Directory"), KeyEvent.VK_D, DirectoriesCustomAction.C_DDIR);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));

    	cAction = new DirectoriesCustomAction(this, _("Resize Columns!"), delicon,_("Resize."), _("Resize"), KeyEvent.VK_W, DirectoriesCustomAction.C_RESIZE);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));

    	cAction = new DirectoriesCustomAction(this, _("Announce Myself!"), delicon,_("Announce."), _("Announce"), KeyEvent.VK_M, DirectoriesCustomAction.C_ANNOUNCE_MYSELF);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	return popup;
	}
}

@SuppressWarnings("serial")
class DirectoriesCustomAction extends DebateDecideAction {
	public static final int C_REFRESH = 1;
    public static final int C_HIDE = 2;
    public static final int C_UNHIDE = 3;
    public static final int C_DELETE = 4;
    public static final int C_PEER = 5;
	public static final int C_ACOLUMN = 6;
	public static final int C_RCOLUMN = 7;
	public static final int C_ADIR = 8;
	public static final int C_DDIR = 9;
	public static final int C_RESIZE = 10;
	public static final int C_ANNOUNCE_MYSELF = 11;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    Directories tree; ImageIcon icon;
	int command;
    public DirectoriesCustomAction(Directories tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
        this.command = command;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    	} else {
    		row = tree.getSelectedRow();
       		row = tree.convertRowIndexToModel(row);
    	}
    	DirectoriesModel model = (DirectoriesModel)tree.getModel();
    	if (command == C_REFRESH) {
    		model.update(null, null);
    		tree.initColumnSizes();
        } else	if (command == C_RCOLUMN) {
        	tree.removeColumn(row);
    		tree.initColumnSizes();
        } else	if (command == C_RESIZE) {
    		tree.initColumnSizes();
        } else	if (command == C_ANNOUNCE_MYSELF) {
        	new util.DDP2P_ServiceThread("Directory Announcement Reset", false) {
        		public void _run() {
		    		UDPServer.announceMyselfToDirectoriesReset();
		    		Application_GUI.warning(_("Announcing:")+UDPServer.directoryAnnouncement, _("Announcement to Listing Directories"));
        		}
        	};
        } else	if (command == C_ACOLUMN) {
        	int col = Application_GUI.ask(_("Add"), _("Columns"), Arrays.copyOf(DirectoriesModel.columnNames, DirectoriesModel.columnNames.length, new Object[]{}.getClass()), null, null);
        	if (col == JOptionPane.CLOSED_OPTION) return;
        	if (col < 0) return;
       		tree.addColumn(col);
    		tree.initColumnSizes();
        } else  if (command == C_ADIR) {
        	String adr = Application_GUI.input(_("Directory Address, such as:\n\"DIR%B%0.5.6://10.0.0.1:25123:24123:Name\""), _("New Directory!"), JOptionPane.QUESTION_MESSAGE);
           	if (DEBUG) System.out.println("DirectoriesAction: adir adr="+adr);
        	Address a = new Address(adr);
           	if (DEBUG) System.out.println("DirectoriesAction: adir a="+a.toLongString());
           	util.DirectoryAddress da = new util.DirectoryAddress(a); //have names for addresses
           	if (DEBUG) System.out.println("DirectoriesAction: adir da="+da);
        	new util.DirectoriesSaverThread(da).start();
        } else  if (command == C_DDIR) {
        	if (_DEBUG) System.out.println("DirectoriesAction:delete C_DDIR: row="+row);
        	//util.DirectoryAddress da = new util.DirectoryAddress(a); //have names for addresses
        	//	new util.DirectoriesSaverThread(da).start();
        	if (row < 0) return;
        	if (0 == Application_GUI.ask(_("Do you want to delete directory?"), _("Delete Directory?"), JOptionPane.OK_CANCEL_OPTION))
        		model.deleteDirectory(row);
        }
    }
}
class DirectoryPing extends util.DDP2P_ServiceThread {
	private static final long WAIT_TIME_MS = 10000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static final String ADDRESS_SEP = ";";
	private DirectoriesModel m;
	private Directories t;
	boolean stop = false;
	private boolean first_warning_done_dir_answer;
	DirectoryPing(DirectoriesModel m, Directories t){
		super ("Directories Polling", true);
		this.m = m;
		this.t = t;
		//this.setDaemon(true); 
		start();
	}
	public void _run() {
		Hashtable<String, InetSocketAddress> addr = new Hashtable<String, InetSocketAddress>();
		while (!stop) {
			try {
				synchronized(this){
					this.wait(WAIT_TIME_MS);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//GID = Identity.getMyPeerGID();
			String id = null;
			String GID = null;
			String GIDH = null;
			D_Peer dpa;
			try {
				dpa = HandlingMyself_Peer.get_myself_or_null();
				if (dpa != null) {
					id = dpa.peer_ID;
					if (DEBUG) System.out.println("Directories: _run my id="+id);
					GID = dpa.getGID();
					GIDH = dpa.getGIDH();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (id == null) {
				try {
					synchronized(this){
						this.wait(WAIT_TIME_MS);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			//String[] ld = m._ld;
			DirectoryAddress[] _addr = m._addr;
			if (_addr == null) {
				ThreadsAccounting.ping("No Directories");
			} else {
				ThreadsAccounting.ping("Directories #"+_addr.length);
				for (int k = 0; k < _addr.length; k++) {
					if (!_addr[k].active) continue;
					Address dir_a = new Address(_addr[k]);//DirectoriesModel.getAddress(ld, k);
					if (DEBUG) System.out.println("Directories:_run: _addr="+_addr[k].toLongString());
					if (DEBUG) System.out.println("Directories:_run: dir_a="+dir_a.toLongString());
					String iport = dir_a.ipPort(); //m.ipPort(ld, k);
					String ip = dir_a.getIP();//ld, k);
					int port = dir_a.getTCPPort();//ld,k);
					if (DEBUG) System.out.println("Directories:ping: k="+k+" ip="+iport+" ld0="+ip+" ld1="+port);
					InetSocketAddress s = addr.get(iport);
					if (s == null) {
						InetAddress ia = Util.getHostIA(ip);
						if (ia == null) {
							if (DEBUG) System.out.println("Directories:ping: got iport="+iport+" a=unr");
							m.addressByIPport.put(iport, _("Unreachable"));
							m.dateByIPport.put(iport, Util.getGeneralizedTime());
							m.fireTableRowsUpdated(k, k);
							continue;
						}
						s = new InetSocketAddress(ia, port);
						addr.put(iport, s);
					}
					ArrayList<Address> a = askAddress(s, GID, GIDH, id, dir_a);
					String _a = Util.concat(a, ADDRESS_SEP, null);
					if (DEBUG) System.out.println("Directories:ping: got iport="+iport+" a="+_a);
					if (_a != null) m.addressByIPport.put(iport, _a);
					else m.addressByIPport.remove(iport);
					m.dateByIPport.put(iport, Util.getGeneralizedTime());
					//if(t!=null) t.initColumnSizes();
					
					m.fireTableRowsUpdated(k, k);
				}
			}
			try {
				synchronized(this){
					this.wait(WAIT_TIME_MS);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	ArrayList<Address> askAddress(InetSocketAddress sock_addr, String global_peer_ID, String GIDH,
			String peer_ID, Address dir_address){
		//final boolean DEBUG = true;
		//final boolean _DEBUG = true;
		if (DEBUG) System.out.println("Directories:askAddress: prepared dir request "+sock_addr+"/"+dir_address.toLongString());
		if (global_peer_ID == null) {
			if (_DEBUG) System.out.println("Directories:askAddress: prepared dir request "+sock_addr+"/"+dir_address.toLongString());
			if (_DEBUG) System.out.println("Directories:askAddress: null GID for GIDH"+GIDH+" LID="+peer_ID);
			Util.printCallPath("");
			return new ArrayList<Address>();
		}
		Socket socket = new Socket();
		try {
			//if(DEBUG) out.println("Directories: askAddress:  Sending to Directory Server: connected:"+sock_addr);
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			DirectoryRequest dr =
					new DirectoryRequest(global_peer_ID,
							Identity.getMyPeerInstance(),
					Identity.getMyPeerGID(),
					Identity.getMyPeerInstance(),
					Identity.udp_server_port, 
					peer_ID,
					dir_address);
			if (DEBUG) System.out.println("Directories:askAddress: prepared dir request dr="+dr);
			byte[] msg = dr.encode();
			if (DEBUG) {
				DirectoryRequest drt = new DirectoryRequest(new Decoder(msg));
				if (DEBUG) System.out.println("Directories:askAddress: prepared dec request "+drt);
			}
			for (;;) {
				socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
				socket.getOutputStream().write(msg);
				if (DEBUG) out.println("Directories: askAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
				Decoder dec_da = Util.readASN1Decoder(socket.getInputStream(), DD.SIZE_DA_PREFERRED, DD.SIZE_DA_MAX);
				if (DEBUG) System.out.println("Directories:askAddress: got answer");
				DirectoryAnswerMultipleIdentities da;
				try {
					da = new DirectoryAnswerMultipleIdentities(dec_da);
					if (DEBUG) System.out.println("Directories:askAddress: gets "+da);
				} catch (Exception e) {
					if (!first_warning_done_dir_answer) {
						first_warning_done_dir_answer = true;
						Application_GUI.warning(_("Unsupported Directory Answer message: ")+
								e.getLocalizedMessage()+"\n"+sock_addr,
								_("Directory Answer fail"));
					}
					e.printStackTrace();
					socket.close();
					return null;
				}
				//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, da, null);
				if (da.instances.size() <= 0) {
					if (_DEBUG) System.out.println("Directories:askAddress:"+da);
					socket.close();
					return null;
				}
				if (da.instances.size() > 1) {
					if (_DEBUG) System.out.println("Directories:askAddress:"+da);
					socket.close();
					return null;
				}
				DirectoryAnswerInstance dia =  da.instances.get(0);
				if ((dia.instance_terms != null) && (dia.instance_terms.length != 0)) {
					dr.terms_default = dr.updateTerms(dia.instance_terms, peer_ID, global_peer_ID, dir_address, dr.terms_default);
					if (dr != null) {
						continue;
						/*
						msg = dr.encode();
						socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
						socket.getOutputStream().write(msg);
						if (_DEBUG) out.println("Directories: askAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
						da = new DirectoryAnswer(socket.getInputStream());
						*/
					}
				}
				
				if ((dia.addresses == null) || (dia.addresses.size() == 0)) {
					//if(DEBUG) out.println("Directories: askAddress:  Got no addresses! da="+da+" for:"+_pc.name);
					if(_DEBUG) out.println("Directories: askAddress:  Got empty addresses! "+da+" \n\tfor:"+dr+" \n\tsa="+sock_addr+"/"+dir_address.toLongString());
					socket.close();
					return null;
				}
				if (DEBUG) out.println("Directories: askAddress: Dir Answer: "+da);
				socket.close();
				return dia.addresses;
				//InetSocketAddress s= da.address.get(0);
				//return s.getHostName()+":"+s.getPort();
			}
		} catch (SocketTimeoutException e) {
			if (DEBUG) {
				out.println("Directories:askAddresses:  fail: timeout "+e+"  DIR addr="+dir_address);
				//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
				e.printStackTrace();
			}
			return askAddressUDP(sock_addr, global_peer_ID, GIDH, peer_ID, dir_address);
		} catch (IOException e) {
			if (DEBUG) out.println("Directories:askAddresses:  fail:  DIR addr="+dir_address+" -> "+dir_address.toLongString()+" "+e.getLocalizedMessage());
			//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			//e.printStackTrace();
			//Directories.setUDPOn(dir_address, new Boolean(false));
			return askAddressUDP(sock_addr, global_peer_ID, GIDH, peer_ID, dir_address);
		} catch (Exception e) {
			if (_DEBUG) out.println("Directories:askAddresses:  fail: "+e+"  DIR addr="+dir_address);
			//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * This function is queried when the TCP request fails. 
	 * It sends a UDP request but does not wait for answer (retrieving the last available in Connections.
	 * @param sock_addr
	 * @param global_peer_ID
	 * @param peer_ID
	 * @param dir_address
	 * @return
	 */
	ArrayList<Address> askAddressUDP(InetSocketAddress _sock_addr, String global_peer_ID, String GIDH, 
			String peer_ID, Address dir_address) {
		if (DEBUG) System.out.println("Directories:askAddressUDP: enter dir_address = "+dir_address);
		if ((Application.aus == null) || (UDPServer.ds == null)) return null;
		int udp_port = dir_address.udp_port;
		if (udp_port <= 0) Util.printCallPath("udp_port="+dir_address.toLongString()); 
		if (udp_port <= 0) udp_port = dir_address.getTCPPort();
		if (udp_port <= 0) return null; 
		DirectoryRequest dr =
				new DirectoryRequest(global_peer_ID,
						Identity.getMyPeerInstance(),
				Identity.getMyPeerGID(),
				Identity.getMyPeerInstance(),
				Identity.udp_server_port, 
				peer_ID,
				dir_address);
		if (DEBUG) System.out.println("Directories:askAddressUDP: prepared dir request dr="+dr);
		//if (DEBUG) System.out.println("Directories:askAddressUDP: dir_address = "+dir_address+" -> "+dir_address.toLongString());
		//if (DEBUG) System.out.println("Directories:askAddressUDP: sock_address="+_sock_addr.getAddress()+" port="+udp_port);
		InetSocketAddress sock_addr = new InetSocketAddress(_sock_addr.getAddress(), udp_port);
		byte[] msg = dr.encode();
		if (DEBUG) {
			DirectoryRequest drt = new DirectoryRequest(new Decoder(msg));
			if (DEBUG) System.out.println("Directories:askAddressUDP: prepared dec request "+drt);
		}
		try {
			DatagramPacket dp = new DatagramPacket(msg, msg.length, sock_addr);
			//if (DEBUG) System.out.println("Directories:askAddressUDP: prepared msg = "+msg.length);
			//if (DEBUG) System.out.println("Directories:askAddressUDP: prepared sa = "+sock_addr);
			//if (DEBUG) System.out.println("Directories:askAddressUDP: prepared ds = "+UDPServer.ds);
			//if (DEBUG) System.out.println("Directories:askAddressUDP: prepared dp = "+dp);
			UDPServer.ds.send(dp);
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
		}
		return Connections.getKnownDirectoryAddresses(dir_address, GIDH, Identity.getMyPeerInstance());
	}
}
/**
 * 
 */
class DirectoriesModel extends AbstractTableModel implements TableModel, DBListener {
	private static final long serialVersionUID = 1L;
	static final int COL_IP = 0;
	static final int COL_PORT_TCP = 1;
	static final int COL_NAME = 2;
	static final int COL_BRANCH = 3;
	static final int COL_VERSION = 4;
	static final int COL_NAT = 5;
	static final int COL_RELAY = 6; // does this directory relay?
	static final int COL_UDP_ON = 7;
	static final int COL_TCP_ON = 8;
	static final int COL_ADDRESS = 9;
	static final int COL_DATE = 10;
	static final int COL_ACTIVE = 11;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	Hashtable<String,Boolean> natByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,String> natAddrByIPport = new Hashtable<String,String>();
	Hashtable<String,Boolean> onUDPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Boolean> onTCPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Exception> onTCPByIPportEx = new Hashtable<String,Exception>();
	Hashtable<String,String> addressByIPport = new Hashtable<String,String>();
	Hashtable<String,String> dateByIPport = new Hashtable<String,String>();
	Hashtable<String,Integer> rowByIPport = new Hashtable<String,Integer>();
	DBInterface db;
	String ld;
	String __ld[];
	DirectoryAddress _addr[];
	static String columnNames[]={_("IP"),_("Port"),_("Name"), _("Branch"),_("Version"),
			_("NAT Piercing"),_("Relay"),
			_("UDP"),_("TCP"),
			_("My address"), _("Date"), _("Active")};
	 /**
	  *  only store the number of columns already stored, anyhow (storage is in application table)
	  */
	int columns_storable = columnNames.length-4;
	private DirectoryPing dp;
	private Directories _table;
	DirectoriesModel(DBInterface _db) {
		db = _db;
		db.addListener(
				this,
				new ArrayList<String>(Arrays.asList(table.directory_address.TNAME)), null);
				
//				new ArrayList<String>(Arrays.asList(table.application.TNAME)),
//				DBSelector.getHashTable(table.application.TNAME, table.application.field, DD.APP_LISTING_DIRECTORIES));
		update(null, null);
		dp = new DirectoryPing(this, getTable());
	}
	public void deleteDirectory(int row) {
		if (_DEBUG) System.out.println("Directories: delete: Setting "+row+"/"+_addr.length);
		_addr[row].delete();
		/*
		try {
			//Address[] addr = _addr.clone();
			String[] ld = __ld.clone();
			if (_DEBUG) System.out.println("Directories: delete: drop "+ld[row]);
			ld[row] = null;
			String dirs = buildDirectoryEntry(ld,	DD.APP_LISTING_DIRECTORIES_SEP);
			String _dirs = buildDirectoryEntry(__ld,	DD.APP_LISTING_DIRECTORIES_SEP);
			if (_DEBUG) System.out.println("Directories: delete: Change "+_dirs+"\nto"+dirs);
			DD.setAppText(DD.APP_LISTING_DIRECTORIES, dirs);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		*/
	}
	public void setTable(Directories _table) {
		this._table = _table;
	}
	private Directories getTable() {
		return _table;
	}
	/*
	public int getPort(String[] ld2, int k) {
		if (ld2 == null) return -1;
		if (k < 0) return -1;
		if (k >= ld2.length) return -1;
		//String[] l = ld2[k].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		//if (l.length<2) return -1;
		try {
			Address adrAddress = new Address(ld2[k]);
			return adrAddress.getTCPPort();
			//return Integer.parseInt(l[1]);
		} catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	
	public String getIP(String[] ld2, int k) {
		if(ld2 == null) return null;
		if(k<0) return null;
		if(k>=ld2.length) return null;
		//String[] l = ld2[k].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		//if(l.length<1) return null;
		//return l[0];
		Address adrAddress = new Address(ld2[k]);
		return adrAddress.getIP();

	}
	*/
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	void setUDPOn(String address, Boolean on){
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("DirectoriesModel:setUDPOn:"+address+" at "+on);
		onUDPByIPport.put(address, on);
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(_DEBUG){
				System.out.println("DirectoriesModel:setUDPOn: No row for directory: "+address);
				for (String a: onUDPByIPport.keySet()){
					System.out.println("DirectoriesModel: existing addresses are:"+a);
				}
			}
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_UDP_ON);
	}
	@Override
	public int getRowCount() {
		if(_addr==null) return 0;
		return _addr.length;
	}
	public Object getAddressValueAt(int row, int col) {
		if ((_addr==null)||(_addr.length<=row)) return null;
		//DirectoryAddress address = _addr[row]; //new Address(_ld[row]);
		String addr = this.addressByIPport.get(this.ipPort(row));
		return addr;
	}
	@Override
	public Object getValueAt(int row, int col) {
		if ((_addr==null)||(_addr.length<=row)) return null;
		
		try {
			DirectoryAddress address = _addr[row]; //new Address(_ld[row]);
			if (col == COL_UDP_ON)return this.onUDPByIPport.get(this.ipPort(row));
			if (col == COL_TCP_ON)return this.onTCPByIPport.get(this.ipPort(row));
			if (col == COL_NAT) return this.natByIPport.get(this.ipPort(row));
			if (col == COL_ADDRESS) {
				String addr = this.addressByIPport.get(this.ipPort(row));
				if (addr == null) return null;
				return new JComboBox<Object>(addr.split(Pattern.quote(DirectoryPing.ADDRESS_SEP)));
			}
			if (col == COL_DATE){
				String o = this.dateByIPport.get(this.ipPort(row));
				if (o == null) return Util.getGeneralizedTime();
				return o;
			}
		
			//String[] el = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
			//if(el.length<=col) return null;
			//return el[col];
			//TypedAddress t_address = TypedAddress.parseStringDirectoryAddress(_ld[row], DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
			if (DEBUG) System.out.println("Directories: getValue: from="+__ld[row]+" -> a="+address);
			if (col == COL_IP) return address.domain;
			if (col == COL_PORT_TCP) return ""+address.tcp_port;
			if (col == COL_NAME) return address.name;
			if (col == COL_BRANCH) return address.branch;
			if (col == COL_VERSION) return address.agent_version;
			if (col == COL_ACTIVE) return address.active;
			return null;
		}catch(Exception e){e.printStackTrace(); return null;}
	}
	public String getTCP_ON_Tip(int row, int col) {
		if (this.onTCPByIPport.get(this.ipPort(row)) == null) return null;
		if (this.onTCPByIPport.get(this.ipPort(row))) return _("Success");
		else return Util.getString(this.onTCPByIPportEx.get(this.ipPort(row)));
		//return null;
	}
	@Override
	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		switch (col) {
		case COL_NAME:
		case COL_IP:
		case COL_PORT_TCP:
		case COL_BRANCH:
		case COL_VERSION:
		case COL_ACTIVE:
		case COL_ADDRESS:
			return true;
		default:
			return false;
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		if ((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP) >= 0) return; 
		if ((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_SEP) >= 0) return; 
		DirectoryAddress adr = _addr[row]; //new Address(_ld[row]);
		//adr.setPureProtocol(Address.DIR);
		if (DEBUG) System.out.println("Directories:setVal "+adr);
		/*
		String el[] = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		String result="";
		for(int k = 0; k < columns_storable; k ++) {
			if(k > 0) result = result + DD.APP_LISTING_DIRECTORIES_ELEM_SEP;
			if(k==col) result = result + value;
			else if(k<el.length) result = result + el[k];
			else result = result+"";
		}
		_ld[row] = result;
		*/
		int ok;
		switch (col) {
		case COL_IP:
			ok = Application_GUI.ask(_("Are you sure you want to change the domain?"), _("Change IP"), JOptionPane.OK_CANCEL_OPTION);
			if (ok == 0) adr.setDomain(Util.getString(value));
			break;
		case COL_PORT_TCP:
			ok = Application_GUI.ask(_("Are you sure you want to change the port?"), _("Change Port"), JOptionPane.OK_CANCEL_OPTION);
			if (ok == 0) adr.setBothPorts(Util.getString(value));
			break;
		case COL_NAME:
			adr.setName(Util.getString(value));
			break;
		case COL_BRANCH:
			adr.setBranch(Util.getString(value));
			break;
		case COL_VERSION:
			adr.setAgentVersion(Util.getString(value));
			break;
		case COL_ACTIVE:
			adr.setActive((Boolean)value);
			if (DEBUG) System.out.println("Directories:setVal:got "+adr+" active="+value);
			break;
		}
		adr.store();
		/*
		__ld[row] = adr.toDirActivityString();
		if (_DEBUG) System.out.println("Directories:setVal is="+row+" => "+__ld[row]);
		try {
			String dirs = buildDirectoryEntry(__ld,
					//DirectoryServer.ADDR_SEP);// 
					DD.APP_LISTING_DIRECTORIES_SEP);
			if (_DEBUG) System.out.println("Directories: setValueAt: Setting "+dirs);
			DD.setAppTextNoSync(DD.APP_LISTING_DIRECTORIES, dirs);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		fireTableCellUpdated(row, col);
		*/
	}
	/*
	private static String buildDirectoryEntry(String[] _addr, String appSep) {
		if (_addr == null) return null;
		return Util.concatNonNull(_addr, appSep, null);
	}
	private static String buildDirectoryEntry(Address[] _addr, String appSep) {
		if (_addr == null) return null;
		
//		Address a[] = new Address[_ld.length];
//		for (int k=0; k<a.length; k++) {
//			a[k] = new Address(_ld[k]);
//		}
//		return Util.concat(a, appSep, null); 
		
		return Util.concatNonNull(_addr, appSep, null);
	}
	*/
	String ipPort(int k) {
		if (_addr[k] == null) return null;
		return _addr[k].ipPort(); //ipPort(_ld, k);
	}
	static Address getAddress(String[]_ld, int k) {
		if ((_ld == null) || (_ld.length <= k)) return null;	
		Address adr = new Address(_ld[k]);
		return adr;
	}
	/*
	static String ipPort(String[]_ld, int k) {
		Address adr = getAddress(_ld, k);
		return adr.ipPort();
		
		
//		int id1 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
//		if(id1<0) return null;
//		int id2 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP,id1+1);
//		if(id2<0) return _ld[k];//id2 = _ld[k].length();
//		return _ld[k].substring(0, id2);
		
	}
	*/
	void setTCPOn(String address, Boolean on, Exception e){
		if(DEBUG) System.out.println("DirectoriesModel:setTCPOn:"+address+" at "+on);
		onTCPByIPport.put(address, on);
		if (e != null) onTCPByIPportEx.put(address, e);
		else onTCPByIPportEx.remove(address);
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(DEBUG) System.out.println("DirectoriesModel:setTCPOn: No row for directory: "+address);
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_TCP_ON);
	}
	void setNATOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setNATOn:"+address+" at "+on);
		natByIPport.put(address, on);
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(DEBUG) System.out.println("DirectoriesModel:setNATOn: No row for directory: "+address);
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_NAT);
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String,DBInfo> info) {
		_addr = DirectoryAddress.getDirectoryAddresses();
		if (DEBUG) System.out.println("Directories:update:"+Util.concat(_addr, ","));
		this.rowByIPport = new Hashtable<String,Integer>();
		for (int k = 0; k<_addr.length; k++) {
			this.rowByIPport.put(_addr[k].ipPort(), new Integer(k));
		}
		/*
		try {
			ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
			if (_DEBUG) System.out.println("Directories:update:"+ld);
			this.rowByIPport = new Hashtable<String,Integer>();
			if (ld != null) {
				__ld=ld.split(DD.APP_LISTING_DIRECTORIES_SEP);
				_addr = new Address[__ld.length];
				for (int k = 0; k<__ld.length; k++) {
					if(DEBUG) System.out.println("Directories:update:"+k+" is "+__ld[k]);
					_addr[k] = DirectoriesModel.getAddress(__ld, k);
					if (_addr[k] == null) {
						if(_DEBUG) System.out.println("Directories:update: fail k=:"+k+" is "+__ld[k]+" from ld="+__ld);
						Util.printCallPath("");
						continue;
					}
					String ipPort = _addr[k].ipPort();
					if (ipPort == null) continue;
					this.rowByIPport.put(ipPort, new Integer(k));
				}
			} else {
				__ld = new String[0];
				_addr = new Address[0];
				this.rowByIPport = new Hashtable<String,Integer>();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		*/
		this.fireTableDataChanged();
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == COL_NAT) return Boolean.class;
		if(col == COL_ACTIVE) return Boolean.class;
		if(col == COL_ADDRESS) return JComboBox.class;
		return String.class;
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
class MyDirComboBoxRenderer extends JLabel  implements TableCellRenderer {
//	class MyDirComboBoxRenderer extends DefaultTableCellRenderer  implements TableCellRenderer {
  public MyDirComboBoxRenderer() {
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
class MyCDirComboBoxRenderer extends JComboBox<Object> implements TableCellRenderer {
  public MyCDirComboBoxRenderer() {
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

class DirectoriesTest extends JPanel {
    Directories tree;
    public DirectoriesTest(DBInterface db) {
    	super(new BorderLayout());
    	tree = new Directories( new DirectoriesModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        add(scrollPane, BorderLayout.CENTER);
		//JScrollPane scrollPane = new JScrollPane(dirs);
		tree.setFillsViewportHeight(true);
    }
    public static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Directories Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DirectoriesTest newContentPane = new DirectoriesTest(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
}

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
import hds.DirectoryAnswer;
import hds.DirectoryRequest;
import hds.Server;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import config.DDIcons;
import config.Identity;
import data.D_PeerAddress;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.Util;
import widgets.components.BulletRenderer;
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
public class Directories extends JTable {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	PathCellRenderer pathRenderer = new PathCellRenderer();
	BulletRenderer bulletRenderer = new BulletRenderer();
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
	void init(){
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
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
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 200));
        //jp.add(scrollPane, BorderLayout.CENTER);
        Application.directoriesData = new DirectoriesData();
        //jp.add(Application.directoriesData, BorderLayout.SOUTH);
        jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, Application.directoriesData), BorderLayout.CENTER);
		return jp;
    }

	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == DirectoriesModel.COL_UDP_ON)) return bulletRenderer;
		if ((column == DirectoriesModel.COL_TCP_ON)) return bulletRenderer;
		if ((column == DirectoriesModel.COL_ADDRESS)) return pathRenderer;
		if ((column == DirectoriesModel.COL_DATE)) return pathRenderer;
		//if ((column == DirectoriesModel.COL_NAT)) return bulletRenderer;
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {null,null,_("A name you provide"),
			_("Is it behind a NAT?"), _("Does it offer relay service"),
			_("UDP answered"), _("TCP answered"), _("UDP answered"),
			_("Served address"), _("Date last answer")};
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
		((DirectoriesModel) this.getModel()).setUDPOn(address, on);
	}
	public void setTCPOn(String address, Boolean on){
		((DirectoriesModel) this.getModel()).setTCPOn(address, on);
	}
	public void setNATOn(String address, Boolean on){
		((DirectoriesModel) this.getModel()).setNATOn(address, on);
	}
	public DirectoriesModel getModel(){
		return (DirectoriesModel) super.getModel();
	}
	void initColumnSizes() {
        DirectoriesModel model = (DirectoriesModel)this.getModel();
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

}

class DirectoryPing extends Thread{
	private static final long WAIT_TIME_MS = 10000;
	private static final boolean DEBUG = false;
	private DirectoriesModel m;
	private Directories t;
	boolean stop = false;
	String GID;
	DirectoryPing(DirectoriesModel m, Directories t){
		this.m = m;
		this.t = t;
		this.setDaemon(true); 
		start();
	}
	public void run(){
		try{
			_run();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	void _run() {
		Hashtable<String, InetSocketAddress> addr = new Hashtable<String, InetSocketAddress>();
		while(!stop){
			GID = Identity.getMyPeerGID();
			String id = null;
			try {
				id = D_PeerAddress.get_myself(GID).peer_ID;
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			String[] ld = m._ld;
			for (int k = 0; k<ld.length; k++){
				String iport = m.ipPort(ld, k);
				String ip = m.getIP(ld, k);
				int port = m.getPort(ld,k);
				//if(DEBUG)System.out.println("Dirs: k="+k+" ip="+iport+" ld0="+ip+" ld1="+port);
				InetSocketAddress s = addr.get(iport);
				if(s == null) {
					InetAddress ia = Util.getHostIA(ip);
					if(ia==null){
						m.addressByIPport.put(iport, _("Unreachable"));
						m.dateByIPport.put(iport, Util.getGeneralizedTime());
						m.fireTableRowsUpdated(k, k);
						continue;
					}
					s = new InetSocketAddress(ia, port);
					addr.put(iport, s);
				}
				ArrayList<Address> a = askAddress(s, GID, id, iport);
				String _a = Util.concat(a, ";", null);
				if(_a!=null) m.addressByIPport.put(iport, _a);
				else m.addressByIPport.remove(iport);
				m.dateByIPport.put(iport, Util.getGeneralizedTime());
				//if(t!=null) t.initColumnSizes();
				
				m.fireTableRowsUpdated(k, k);
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
	ArrayList<Address> askAddress(InetSocketAddress sock_addr, String global_peer_ID, 
			String peer_ID, String dir_address){
		final boolean DEBUG = false;
		final boolean _DEBUG = false;
		Socket socket = new Socket();
		try {
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			//if(DEBUG) out.println("Directories: askAddress:  Sending to Directory Server: connected:"+sock_addr);
			DirectoryRequest dr = new DirectoryRequest(global_peer_ID,
					Identity.getMyPeerGID(),
					Identity.udp_server_port, 
					peer_ID,
					dir_address);
			byte[] msg = dr.encode();
			socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
			socket.getOutputStream().write(msg);
			if(DEBUG) out.println("Directories: askAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
			DirectoryAnswer da = new DirectoryAnswer(socket.getInputStream());
			//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, da, null);

			if((da.terms != null) && (da.terms.length != 0)) {
				dr.terms = dr.updateTerms(da.terms, peer_ID, global_peer_ID, dir_address, dr.terms);
				if(dr!=null){
					msg = dr.encode();
					socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
					socket.getOutputStream().write(msg);
					if(DEBUG) out.println("Directories: askAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
					da = new DirectoryAnswer(socket.getInputStream());
				}
			}
			
			if(da.addresses.size()==0){
				//if(DEBUG) out.println("Directories: askAddress:  Got no addresses! da="+da+" for:"+_pc.name);
				socket.close();
				return null;
			}
			if(DEBUG) out.println("Directories: askAddress: Dir Answer: "+da);
			socket.close();
			if(da.addresses==null){
				if(_DEBUG) out.println("Directories: askAddress:  Got empty addresses!");
				return null;
			}
			return da.addresses;
			//InetSocketAddress s= da.address.get(0);
			//return s.getHostName()+":"+s.getPort();
		}catch (IOException e) {
			//if(DEBUG) out.println("Connections: getDirAddress:  fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			//e.printStackTrace();
			//Directories.setUDPOn(dir_address, new Boolean(false));
		} catch (Exception e) {
			//if(DEBUG) out.println("Connections: getDirAddress:  fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			//ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}
}
/**
 * 
 */
class DirectoriesModel extends AbstractTableModel implements TableModel, DBListener {
	private static final long serialVersionUID = 1L;
	private static final int COL_NAME = 2;
	static final int COL_NAT = 3;
	static final int COL_UDP_ON = 5;
	static final int COL_TCP_ON = 6;
	static final int COL_ADDRESS = 7;
	static final int COL_DATE = 8;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	Hashtable<String,Boolean> natByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,String> natAddrByIPport = new Hashtable<String,String>();
	Hashtable<String,Boolean> onUDPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Boolean> onTCPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,String> addressByIPport = new Hashtable<String,String>();
	Hashtable<String,String> dateByIPport = new Hashtable<String,String>();
	Hashtable<String,Integer> rowByIPport = new Hashtable<String,Integer>();
	DBInterface db;
	String ld;
	String _ld[];
	String columnNames[]={_("IP"),_("Port"),_("Name"),
			_("NAT Piercing"),_("Relay"),
			_("UDP"),_("TCP"),
			_("My address"), _("Date")};
	 /**
	  *  only store the number of columns already stored, anyhow (storage is in application table)
	  */
	int columns_storable = columnNames.length-4;
	private DirectoryPing dp;
	private Directories _table;
	DirectoriesModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.application.TNAME)),
				DBSelector.getHashTable(table.application.TNAME, table.application.field, DD.APP_LISTING_DIRECTORIES));
		update(null, null);
		dp = new DirectoryPing(this, getTable());
	}
	public void setTable(Directories _table) {
		this._table = _table;
	}
	private Directories getTable() {
		return _table;
	}
	public int getPort(String[] ld2, int k) {
		if(ld2 == null) return -1;
		if(k<0) return -1;
		if(k>=ld2.length) return -1;
		String[] l = ld2[k].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		if(l.length<2) return -1;
		try{
			return Integer.parseInt(l[1]);
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public String getIP(String[] ld2, int k) {
		if(ld2 == null) return null;
		if(k<0) return null;
		if(k>=ld2.length) return null;
		String[] l = ld2[k].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		if(l.length<1) return null;
		return l[0];
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public int getRowCount() {
		if(_ld==null) return 0;
		return _ld.length;
	}
	@Override
	public Object getValueAt(int row, int col) {
		if((_ld==null)||(_ld.length<=row)) return null;
		try{
			if(col == COL_UDP_ON)return this.onUDPByIPport.get(this.ipPort(row));
			if(col == COL_TCP_ON)return this.onTCPByIPport.get(this.ipPort(row));
			if(col == COL_NAT) return this.natByIPport.get(this.ipPort(row));
			if(col == COL_ADDRESS) return this.addressByIPport.get(this.ipPort(row));
			if(col == COL_DATE){
				String o = this.dateByIPport.get(this.ipPort(row));
				if(o==null) return Util.getGeneralizedTime();
				return o;
			}
		
			String[] el = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
			if(el.length<=col) return null;
			return el[col];
		}catch(Exception e){e.printStackTrace(); return null;}
	}
	@Override
	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col==COL_NAME;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP) >= 0) return; 
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_SEP) >= 0) return; 
		String el[] = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		String result="";
		for(int k=0; k<columns_storable; k++) {
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
		fireTableCellUpdated(row, col);
	}
	String ipPort(int k) {
		return ipPort(_ld, k);
	}
	String ipPort(String[]_ld, int k) {
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
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(DEBUG) System.out.println("DirectoriesModel:setUDPOn: No row for directory: "+address);
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_UDP_ON);
	}
	void setTCPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setTCPOn:"+address+" at "+on);
		onTCPByIPport.put(address, on);
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
		try {
			ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
			if(DEBUG)System.out.println("Directories:update:"+ld);
			if(ld!=null) {
				_ld=ld.split(DD.APP_LISTING_DIRECTORIES_SEP);
				for(int k=0; k<_ld.length; k++) {
					if(DEBUG)System.out.println("Directories:update:"+k+" is "+_ld[k]);
					String ipPort = ipPort(k);
					if(ipPort == null) continue;
					this.rowByIPport.put(ipPort, new Integer(k));
				}
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}		
		this.fireTableDataChanged();
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == COL_NAT) return Boolean.class;
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

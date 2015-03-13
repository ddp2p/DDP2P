/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Osamah Dhannoon
		Authors: Osamah Dhanoon: odhanoon2011@my.fit.edu
				 Marius Silaghi: msilaghi@fit.edu
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
package net.ddp2p.widgets.wireless;
/**
 * This class ...
 */

import javax.swing.JTable;

import static net.ddp2p.common.util.Util.__;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.DBSelector;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastServer;
import net.ddp2p.common.wireless.Detect_interface;
import net.ddp2p.common.wireless.Refresh;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.peers.Peers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.BorderLayout;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;


@SuppressWarnings("serial")
public class WLAN_widget extends JTable implements ActionListener, MouseListener {
	
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int COL_SELECTED = 3;
	public static final int COL_INTERF = 0;
	public static final int COL_IP = 1;
	public static final int COL_SSID = 2;
	public WLAN_widget(WlanModel dm) {
		super(dm);
		init();
	}
	public WLAN_widget(DBInterface db) {
		super(new WlanModel(db));
		init();	
	}
	public void connectWidget() {
		getModel().connectWidget();
	}
	public void disconnectWidget() {
		getModel().disconnectWidget();
	}
	public Component getComboPanel() {
		return MainFrame.makeWLanPanel(this);
	}    
	
	void init(){
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);	
		addMouseListener(this);
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
        jp.add(scrollPane, BorderLayout.CENTER);
		return jp;
    }
    
	/*
    public TableCellRenderer getCellRenderer(int row, int col) {
    	if(col==2)  return cR;
    	 return super.getCellRenderer(row, col);
	}
*/
    protected String[] columnToolTips = {null,null,__("A name you provide")};
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
	public WlanModel getModel(){
		return (WlanModel) super.getModel();
	}
	private void initColumnSizes() {
        WlanModel model = (WlanModel)this.getModel();
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
	
	public void actionPerformed(ActionEvent event) {
		
	}
	public void update() {
		WlanModel model = getModel();
		model.update(null, null);
	}
	
	public static void main(String[] args) throws P2PDDSQLException {
		/*
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows 7");
		os_Names.add("Linux");
		String osName= System.getProperty("os.name");
		
		int ch=0;
		if(osName.compareTo(os_Names.get(0))==0) ch=1;
		else if(osName.compareTo(os_Names.get(1))==0) ch=2;
		
		switch(ch){
		case 1:{ Application.db=new DBInterface("deliberation-app.db");
				break;}
		case 2: { Application.db=new DBInterface("deliberation-app.db");
				break;}
			default: { System.out.println("Unable to detect OS"); break;}
		}
		*/
		Interfaces.createAndShowGUI(Application.db);
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
    	jtableMouseReleased(arg0);		
	}
	@Override
	public void mouseReleased(MouseEvent arg0) {
    	jtableMouseReleased(arg0);
	}
	
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        if(DEBUG) System.out.println("WLAN_widget:jTableMouseRelease: row="+row);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row=this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	JPopupMenu getPopup(int model_row, int col){
		JMenuItem menuItem;
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
       	WirelessCustomAction rAction ;
       	rAction = new WirelessCustomAction(this, __("Refresh!"),addicon,__("Refresh."),__("Refresh the data."), KeyEvent.VK_A, false, WirelessCustomAction.REFRESH);
     	rAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(rAction);
    	popup.add(menuItem);
    	
       	rAction = new WirelessCustomAction(this, __("Unselect"),addicon,__("Unselect."),__("Unselect."), KeyEvent.VK_U, false, WirelessCustomAction.UNSELECT);
     	rAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(rAction);
    	popup.add(menuItem);
    	
       	rAction = new WirelessCustomAction(this, __("DisConfigure"),addicon,__("DisConfigure (Linux: restart manager, Windows: dhcp)."),__("Disconfigure."), KeyEvent.VK_D, false, WirelessCustomAction.DISCONFIGURE);
     	rAction.putValue("row", new Integer(model_row));
    	menuItem = new JMenuItem(rAction);
    	popup.add(menuItem);

    	return popup;
	}
}
@SuppressWarnings("serial")
class WirelessCustomAction extends DebateDecideAction {
    public static final int DISCONFIGURE = 3;
	public static final int UNSELECT = 2;
	public static final int REFRESH = 1;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    WLAN_widget tree; ImageIcon icon; boolean _default;
    int cmd = 0;
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
    public WirelessCustomAction(WLAN_widget tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, boolean _default, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this._default=_default; this.cmd=cmd;
    }
	public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1, row_model=-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row_model = ((Integer)act.getValue("row")).intValue();
            if(DEBUG) System.err.println("WirelessCustomActionrow property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row_model=tree.convertRowIndexToModel(row);
   		if(DEBUG) System.err.println("WirelessCustomAction:Row selected: " + row);
    	}
    	WlanModel model = (WlanModel)tree.getModel();
     	//if(row<0) return;
    	switch(cmd) {
    	case REFRESH:
    		WlanModel.refresh();
    		break;
    	case UNSELECT:
    		model.unselect(row_model);
    		break;
    	case DISCONFIGURE:
    		model.disconfigure(row_model);
    		break;
    	}
    }
}

class WlanModel  extends AbstractTableModel implements TableModel, DBListener {
	/**
	 * 
	 */
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final long serialVersionUID = 1L;
	DBInterface db;
	String ld;
	//String _ld[];
	//String columnNames[]={"Interface Name","IP Address",SSID,"Select Interface"};
	String columnNames[]={__("Interface Name"),__("Current IP"),__("SSID"),__("Select Interface")};
	int columns = columnNames.length;
	private String[][] _table = new String[0][];
	WlanModel(DBInterface _db) {
		db = _db;
		connectWidget();
		update(null, null);
	}
	public void connectWidget() {
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.application.TNAME)),
				DBSelector.getHashTable(net.ddp2p.common.table.application.TNAME, net.ddp2p.common.table.application.field, DD.APP_NET_INTERFACES));
	}
	public void disconnectWidget() {
		db.delListener(this);
	}
	public static boolean refresh() {
		new DDP2P_ServiceThread("WLAN: detect", true) {
			public void _run() {
				String _wireless;
				synchronized(Refresh.wlanmonitor){
					try {
						_wireless = net.ddp2p.common.wireless.Detect_interface.detect_wlan();
						DD.setAppText(DD.APP_NET_INTERFACES, _wireless);
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		return false;
	}
	public static void refreshRepeatedly() {
		new DDP2P_ServiceThread("WLAN: refresh", true) {
			public void _run(){
				int delay = 500; 
				for(int k=0; k<10; k++) {
					if(DEBUG)System.out.println("WLAN_widget:refreshRepeatedly:"+k);
					if(refresh()) break;
					try {
						Thread.sleep(delay);
						if(k==3) delay = 1000;
						if(k==6) delay = 1500;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public int getRowCount() {
		return _table.length;
		//if(_ld==null) return 0;
		//return _ld.length;
	}
	// TODO err0r if column 3, no 3 columns
	@Override
	public Object getValueAt(int row, int col) {
		//boolean DEBUG = true;
		Object result;
		if((row<0)||(col<0)) return null;
		if((row>=_table.length)||(col>=_table[row].length)) return null;
		if(col==WLAN_widget.COL_SELECTED){
			result = new Boolean(Util.stringInt2bool(_table[row][col], false));
			if(DEBUG) System.out.println("WLAN_widget:getValueAt():row="+row+":col="+col+"="+result);
			return result;
		}
		return _table[row][col];
//		if((_ld==null)||(_ld.length<=row)) return null;
//		if(col==3){
//			String el[]=_ld[row].split(":");
//			if(el.length>3)
//				return new Boolean ("1".equals(el[3]));
//			return null;
//		}
//		String[] el = _ld[row].split(":");
//		if(el.length<=col) return null;
//		return el[col];
	}
	
	@Override
	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	@Override
    public boolean isCellEditable(int row, int col)
    { 
    return col==3;	
    }
	
	class InterfaceDown extends Thread {
		private Object interf;

		public InterfaceDown(Object interf) {
			this.interf = interf;
		}

		public void run() {
			try {
				WirelessSetup.DisconnectInterface(interf);
			} catch (IOException e) {
				e.printStackTrace();
			}
			catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			catch (InterruptedException e2) {
				e2.printStackTrace();
			}

		}
	}
	class InterfaceUp extends Thread {
		private Object interf;
		boolean configured;

		public InterfaceUp(Object interf, boolean _configured) {
			this.interf = interf;
			configured = _configured;
		}

		public void run() {
			try {
				WirelessSetup.interfaceUp(interf, configured);
			} catch (IOException e) {
				e.printStackTrace();
			}
			catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			catch (InterruptedException e2) {
				e2.printStackTrace();
			}

		}
	}
	public void disconfigure(int row_model) {
		int col = WLAN_widget.COL_SELECTED;
		setValueAt(new Boolean(false), row_model, col, true);
	}
	public void unselect(int row_model) {
		int col = WLAN_widget.COL_SELECTED;
		setValueAt(new Boolean(false), row_model, col, false);
	}   
	@Override
	public void setValueAt(Object value, int row, int col) {
		setValueAt(value, row, col, true);
	}
	public void setValueAt(Object value, int row, int col, boolean _disconfigure) {
		boolean DEBUG = true;
		if((row<0)||(col<0)) return;
		if((row>=_table.length)||(col>=_table[row].length)) return;
		if(col!=WLAN_widget.COL_SELECTED) return;
		if(!(value instanceof Boolean)) return;
		if(DEBUG)System.out.println("wlan_widget: setValueAt: row="+row+" col="+col);
		Object interf = _table[row][WLAN_widget.COL_INTERF];
		HashSet<String> selected = new HashSet<String>();
		try {
			selected = Detect_interface.getSelectedInterfaces();
			//_table[row][WLAN_widget.COL_SELECTED] = value;
			boolean changed = false;
			if(((Boolean)value).booleanValue()) {
				if(DEBUG)System.out.println("wlan_widget: setValueAt: Setting true");
				changed = selected.add((String) interf); 
				if(changed){
					if(DEBUG)System.out.println("wlan_widget: setValueAt: Setting changed");
					Detect_interface.setSelectedInterfaces(selected);
				}
				boolean configured=false;
				if(DD.DD_SSID.equals(_table[row][WLAN_widget.COL_SSID]) &&
						(Util.ip_compatible_with_network_mask(_table[row][WLAN_widget.COL_IP],DD.WIRELESS_ADHOC_DD_NET_IP_BASE,DD.WIRELESS_ADHOC_DD_NET_MASK)))
					configured = true;
				new InterfaceUp(interf, configured).start();
			}else{
				if(DEBUG)System.out.println("wlan_widget: setValueAt: Setting false");
				changed = selected.remove(interf);
				if(changed){
					if(DEBUG)System.out.println("wlan_widget: setValueAt: Setting changed");
					Detect_interface.setSelectedInterfaces(selected, false);
				}
				if(_disconfigure) {
					new InterfaceDown(interf).start();
					//	WirelessSetup.interfaceDown(interf);
				}
			}
			_table[row][col] = Util.bool2StringInt(((Boolean)value).booleanValue());
			String dirs = buildInterfacesDescriptionString(_table);
			if(DEBUG)System.out.println("wlan_widget: setValueAt: Setting "+dirs);
			DD.setAppTextNoSync(DD.APP_NET_INTERFACES, dirs);
		} catch (P2PDDSQLException e) {
			if(_DEBUG)System.out.println("wlan_widget: setValueAt: Setting error");
			if(_DEBUG)e.printStackTrace();
		}
		fireTableCellUpdated(row, col);
		if(DEBUG)System.out.println("wlan_widget: setValueAt: done: disconf"+_disconfigure);
		
//		
//		Object interf = new String(_ld[row].substring(0,_ld[row].indexOf(":")));
//		String el[] = _ld[row].split(":");
//		String result="";
//		for(int k=0; k<columns; k++) {
//			if(k > 0) result = result + ":";
//			if(k==col) result = result + (new Boolean(true).equals(value)?"1":"0");
//			else if(k<el.length) result = result + el[k];
//			else result = result+"";
//		}
//		_ld[row] = result;
//		try {
//			String dirs = Util.concat(_ld,",");
//			if(DEBUG)System.out.println("wlan_widget: setValueAt: Setting "+dirs);
//			DD.setAppTextNoSync(DD.APP_NET_INTERFACES, dirs);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
	}
	private String buildInterfacesDescriptionString(String[][] _table2) {
		String[]rows = new String[_table2.length];
		for(int i=0;i<_table2.length; i++) {
			rows[i] = Util.concat(_table2[i], ":");
		}
		return Util.concat(rows, ",");
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String,DBInfo> info) {
		//boolean DEBUG = true;
		//Util.printCallPath("update");
		if(DEBUG)System.out.println("wlan_widget:update: start");
		try {
			ld = DD.getAppText(DD.APP_NET_INTERFACES);
			if(ld!=null){
				String[][] __table = BroadcastServer.parseInterfaceDescription(ld);
				if(__table!=null) _table =__table;
			}
			if(DEBUG)System.out.println("wlan_widget:update:"+ld);
			/*
			if(ld!=null) {
				_ld=ld.split(",");
				for(int k=0; k<_ld.length; k++) {
					if(DEBUG)System.out.println("wlan_widget:update:"+k+" is "+_ld[k]);
				}
			}
			*/
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}		
		this.fireTableDataChanged();
	}
	
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == 3) return Boolean.class;
		return String.class;	
	}
}
/*
@SuppressWarnings("serial")
class Interfaces extends JPanel {
	WLAN_widget tree;
    public Interfaces(DBInterface db) {
    	super(new BorderLayout());
    	tree = new WLAN_widget( new WlanModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        add(scrollPane, BorderLayout.CENTER);
		tree.setFillsViewportHeight(true);
    }
    public static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Wlan Widget");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Interfaces newContentPane = new Interfaces(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
}
*/
class checkR implements TableCellRenderer{
public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {

	JCheckBox rendererComponent = new JCheckBox();
	if(value==null)  rendererComponent.setSelected(false);
	boolean marked = (Boolean) value;
	
	if (marked) {
		rendererComponent.setSelected(true);
	}
	return rendererComponent;
}
}

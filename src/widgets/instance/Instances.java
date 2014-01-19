/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
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
package widgets.instance;

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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import util.P2PDDSQLException;
import util.Util;
import widgets.components.BulletRenderer;
import widgets.components.CalendarRenderer;
import widgets.components.XTableColumnModel;
import widgets.peers.PeerListener;
import widgets.peers.Peers;
import config.Application;
import config.DDIcons;
import data.D_PeerAddress;
import data.D_PeerInstance;

@SuppressWarnings("serial")
public 
class Instances extends JTable implements MouseListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;
	public static final int HOT_SEC = 10;
	BulletRenderer hotRenderer;
	CalendarRenderer calendarRenderer = new CalendarRenderer();
	private XTableColumnModel yourColumnModel;
	
	public Instances() {
		super(new InstancesModel());
		hotRenderer = new BulletRenderer(
				DDIcons.getHotImageIcon("Hot"), DDIcons.getHotGImageIcon("Hot"),
				null, _("Recently Contacted"),  _("Not Recently Contacted"), null);
		if(DEBUG) System.out.println("ThreadsView: constr from model");
		init();
		this.getTableHeader().addMouseListener(this);
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(DIM_X, DIM_Y));
        jp.add(scrollPane, BorderLayout.CENTER);
		return jp;
    }

	public TableCellRenderer getCellRenderer(int row, int _column) {
		int column = this.convertColumnIndexToModel(_column);
		switch (column) {
		case InstancesModel.TABLE_COL_HOT:
			return hotRenderer;
		case InstancesModel.TABLE_COL_CONTACT:
		case InstancesModel.TABLE_COL_LAST:
		case InstancesModel.TABLE_COL_RESET:
			return calendarRenderer;
		}
		return super.getCellRenderer(row, _column);
	}

	@SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realIndex = 
						columnModel.getColumn(index).getModelIndex();
				if (realIndex >= InstancesModel.columnToolTips.length) return null;
				return InstancesModel.columnToolTips[realIndex];
			}
		};
	}
	public InstancesModel getModel(){
		return (InstancesModel) super.getModel();
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        if (DEBUG) System.out.println("Instances:mouse: row="+row+" col="+col);
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
    	InstancesModel model = getModel();
    	InstancesCustomAction cAction;
    	
    	cAction = new InstancesCustomAction(this, _("Refresh!"), reseticon,_("Refresh."), _("Refresh"),KeyEvent.VK_R, InstancesCustomAction.C_REFRESH);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new InstancesCustomAction(this, _("Remove Column!"), delicon,_("Remove Column."), _("Remove Column"), KeyEvent.VK_C, InstancesCustomAction.C_RCOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new InstancesCustomAction(this, _("Add Column!"), addicon,_("Add Column."), _("Add Column"), KeyEvent.VK_A, InstancesCustomAction.C_ACOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new InstancesCustomAction(this, _("Add new instance!"), addicon,_("Add new instance."), _("Add New"),KeyEvent.VK_I, InstancesCustomAction.C_INSTANCE);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	if(row>=0){
	    	popup.addSeparator();

	    	cAction = new InstancesCustomAction(this, _("Set Myself!"), addicon,_("Set Myself."), _("Set Myself"),KeyEvent.VK_M, InstancesCustomAction.C_SET_MYSELF);
	    	cAction.putValue("row", new Integer(row));
	    	popup.add(new JMenuItem(cAction));
	    	
	    	cAction = new InstancesCustomAction(this, _("Delete instance!"), delicon,_("Delete instance."), _("Delete"),KeyEvent.VK_D, InstancesCustomAction.C_DELETE);
	    	cAction.putValue("row", new Integer(row));
	    	popup.add(new JMenuItem(cAction));
    	}
    	return popup;
	}
	void init() {
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		
		yourColumnModel = new widgets.components.XTableColumnModel();
		setColumnModel(yourColumnModel); 
		createDefaultColumnsFromModel(); 
		
		initColumnSizes();
		//this.getTableHeader().setToolTipText(_("Click to sort; Shift-Click to sort in reverse order"));
		////this.setAutoCreateRowSorter(true);
		//this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		
		//icon_register = config.DDIcons.getRegistrationImageIcon("Register");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		//icon_org = config.DDIcons.getOrgImageIcon("Org");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");

		
		DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel headerLabel = (JLabel)
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Icon icon = Instances.this.getModel().getIcon(column);
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
        TableModel model = (TableModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < this.getColumnCount(); i++)
        {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(this.convertColumnIndexToModel(i))).
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
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);		
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);		
	}
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}

}
@SuppressWarnings("serial")
class InstancesModel extends AbstractTableModel implements TableModel, PeerListener {
	private static final boolean DEBUG = false;
	private static final long HOT_SEC = 30*60;
	static String columnNames[]={_("Instance"),_("Hot"),_("Local"),_("Reset"),_("Contact"),_("Sync")};
	protected static String[] columnToolTips = {
			_("A name for the instance"),
			_("Ping in less seconds than:")+""+HOT_SEC,
			_("Created on this installation"),
			_("Date of Reset Sent"),
			_("Date of Last Contact Established"),
			_("Date of Synchronization from this Instance")
			};
	public static final int TABLE_COL_INSTANCE = 0;
	public static final int TABLE_COL_HOT = 1;
	public static final int TABLE_COL_LOCAL = 2;
	public static final int TABLE_COL_RESET = 3;
	public static final int TABLE_COL_CONTACT = 4;
	public static final int TABLE_COL_LAST = 5;
	public static final int TABLE_COL_HIDDEN = 6;
	/**
	 * 30 minutes
	 */
	private static final long HOT_DELAY = HOT_SEC * 1000;
	ArrayList<Instances> tables = new ArrayList<Instances>();
	ArrayList<D_PeerInstance> data = new ArrayList<D_PeerInstance>();

	D_PeerAddress current;
	public InstancesModel() {
		
	}
	public void setTable(Instances threadsView) {
		tables.add(threadsView);
	}
	public Icon getIcon(int column) {
		if (column == TABLE_COL_HIDDEN){ 
			return DDIcons.getHideImageIcon("Hidden");
		}
		if (column == TABLE_COL_INSTANCE){ 
			return DDIcons.getIdentitiesImageIcon("Heads");
		}
		if (column == TABLE_COL_HOT){ 
			return DDIcons.getHotImageIcon("Fire");
		}
		return null;
	}
	@Override
	public int getRowCount() {
		if(current == null) return 0;
		//return current.instances.size();
		return data.size();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		if (col == InstancesModel.TABLE_COL_HIDDEN) return Boolean.class;
		if (col == InstancesModel.TABLE_COL_HOT) return Boolean.class;
		if (col == InstancesModel.TABLE_COL_LOCAL) return Boolean.class;
			return String.class;
	}		
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public String getColumnName(int col) {
		try {
			return columnNames[col];
		} catch (Exception e) {
			e.printStackTrace();return null;
		}
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		D_PeerInstance crt;
		Object result = null;
		try {
			crt = data.get(rowIndex);
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		switch (columnIndex) {
		case TABLE_COL_INSTANCE:
			result = crt.peer_instance;
			break;
		case TABLE_COL_HOT:
			if(crt.last_contact_date == null) {
				result = Boolean.FALSE;
			} else {
				result = new Boolean(Util.CalendargetInstance().getTimeInMillis() - crt.last_contact_date.getTimeInMillis()
						< HOT_DELAY);
			}
			break;
		case TABLE_COL_LOCAL:
			result = new Boolean(crt.createdLocally);
			break;
		case TABLE_COL_RESET:
			result = crt.last_reset;
			break;
			//return crt._last_reset;
		case TABLE_COL_CONTACT:
			result = crt.last_contact_date;
			break;
			//return crt._last_contact_date;
		case TABLE_COL_LAST:
			result = crt.last_sync_date;
			break;
			//return crt._last_sync_date;
		}
		//System.out.println("InstancesModel:getAt:"+rowIndex+":"+columnIndex+"Result="+result);
		return result;
	}

	public void update() {
		if (current == null) {
			this.data = new ArrayList<D_PeerInstance>();
		} else {
			current.loadInstances();
			D_PeerInstance __data[] = current.instances.values().toArray(new D_PeerInstance[0]);
			Arrays.sort(__data, new Comparator<D_PeerInstance>(){

				@Override
				public int compare(D_PeerInstance o1, D_PeerInstance o2) {
					if (o1 == o2) return 0;
					if (o1 == null) return -1;
					if (o2 == null) return 1;
					if (o1.peer_instance == o2.peer_instance) return 0;
					if (o1.peer_instance == null) return -1;
					if (o2.peer_instance == null) return 1;
					return o1.peer_instance.compareTo(o2.peer_instance);
				}});
			this.data = new ArrayList<D_PeerInstance>(Arrays.asList(__data));
			/*
			ArrayList<D_PeerInstance> _data = new ArrayList<D_PeerInstance>(current.instances.size());
			//ArrayList<D_PeerInstance> _data = new ArrayList<D_PeerInstance>(current.instances.values());
			for (D_PeerInstance dpi : current.instances.values()) {
				for (int k = 0; k < _data.size(); k++) {
					if (dpi.peer_instance.compareTo(_data.get(k).peer_instance) < 0) {
						_data.add(k, dpi);
						break;
					}
				}
			}
			this.data = _data;
			*/
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fireTableDataChanged();
			}
		});
	}

	public void delete(int row) {
		current.deleteInstance(data.get(row).peer_instance);
		update();
	}
	public void setMyself(int row) {
		if (current == null) return;
		String instance = data.get(row).peer_instance;
		try {
			current.setCurrentInstance(instance);
			D_PeerAddress.setMyself(current);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	public void createInstance(int row) {
		String instance = Application.input(_("Provide an instance name (empty for current time)"),  _("New Instance"), JOptionPane.QUESTION_MESSAGE);
		if ((instance == null) || instance.trim().equals(""))
			instance = Util.getGeneralizedTime();
		current.addInstance(instance, true);
		update();
	}
	@Override
	public void update_peer(D_PeerAddress peer, String my_peer_name,
			boolean me, boolean selected) {
		if (!selected) return;
		if (!D_PeerAddress.samePeers(peer, this.current)) {
			this.current = peer;
			update();
		}
	}	
}
@SuppressWarnings("serial")
class InstancesCustomAction extends DebateDecideAction {
	public static final int C_REFRESH = 1;
    public static final int C_HIDE = 2;
    public static final int C_UNHIDE = 3;
    public static final int C_DELETE = 4;
    public static final int C_INSTANCE = 5;
	public static final int C_ACOLUMN = 6;
	public static final int C_RCOLUMN = 7;
	public static final int C_SET_MYSELF = 8;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Instances tree; ImageIcon icon;
	int command;
    public InstancesCustomAction(Instances tree,
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
    	InstancesModel model = (InstancesModel)tree.getModel();
    	if(command == C_REFRESH) {
    		model.update();
    		tree.initColumnSizes();
    		model.fireTableDataChanged();
//    	}else if(command == C_HIDE) {
//    		model.hide = true;
//    		model.update(null, null);
//    	}else if(command == C_UNHIDE) {
//        	model.hide = false;
//        	model.update(null, null);
        }else	if(command == C_DELETE) {
        	model.delete(row);
        }else	if(command == C_INSTANCE) {
        	model.createInstance(row);
        }else	if(command == C_RCOLUMN) {
        	int col = row; 
        	tree.removeColumn(col);
    		tree.initColumnSizes();
        }else if (command == C_SET_MYSELF) {
        	model.setMyself(row);
        } else	if (command == C_ACOLUMN) {
        	int col = Application.ask(_("Add"), _("Columns"), Arrays.copyOf(InstancesModel.columnNames, InstancesModel.columnNames.length, new Object[]{}.getClass()), null, null);
        	if(col == JOptionPane.CLOSED_OPTION) return;
        	if(col < 0) return;
       		tree.addColumn(col);
    		tree.initColumnSizes();
        }
    }
}

package net.ddp2p.widgets.keys;

import static net.ddp2p.common.util.Util.__;

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
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
//import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
//import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.components.TableUpdater;
//import widgets.org.ColorRenderer;

@SuppressWarnings("serial")
public class Keys extends JTable implements MouseListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;

	public Keys() {
		super(new KeysModel(Application.db));
		if(DEBUG) System.out.println("Orgs: constr from db");
		init();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		//this.setMinimumSize(new Dimension(400,200));
		//scrollPane.setMinimumSize(new Dimension(400,200));
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

	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		//centerRenderer = new DefaultTableCellRenderer();
		//centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
	}

	@Override
	public TableCellEditor getCellEditor(int row, int columnIndex) {
		int column = getModel().globalColumn(columnIndex);
		if(column == KeysModel.TABLE_COL_CONS){
			Object value = super.getValueAt(row, column);
			if((value != null)&&(!"".equals(value))) {
				int row_m = this.convertRowIndexToModel(row);
				value = getModel().getConstituentsValue(row_m);
				if(value instanceof JComboBox) {
					return new DefaultCellEditor((JComboBox)value);
				}
				if(value!=null) return getDefaultEditor(value.getClass());
			}
		}
		return super.getCellEditor(row, column);
	}
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
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
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	KeysModel model = getModel();
    	KeysCustomAction cAction;
    	
    	cAction = new KeysCustomAction(this, __("Refresh!"), addicon,__("Refresh."), __("Refresh"),KeyEvent.VK_R, KeysCustomAction.C_REFRESH);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new KeysCustomAction(this, __("Remove Column!"), delicon,__("Remove Column."), __("Remove Column"), KeyEvent.VK_C, KeysCustomAction.C_RCOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new KeysCustomAction(this, __("Add Column!"), addicon,__("Add Column."), __("Add Column"), KeyEvent.VK_A, KeysCustomAction.C_ACOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	if(model.hide){
    		cAction = new KeysCustomAction(this, __("UnHide!"), addicon,__("UnHide."), __("UnHide"),KeyEvent.VK_H, KeysCustomAction.C_UNHIDE);
    	}else{
    		cAction = new KeysCustomAction(this, __("Hide!"), addicon,__("Hide."), __("Hide"),KeyEvent.VK_H, KeysCustomAction.C_HIDE);
    	}
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	if(row>=0){
	    	popup.addSeparator();

	    	if(model.getPeer(row)==null){
	    		cAction = new KeysCustomAction(this, __("Create Peer!"), addicon,__("Create Peer."), __("Create Peer"),KeyEvent.VK_P, KeysCustomAction.C_PEER);
	    		cAction.putValue("row", new Integer(row));
	    		popup.add(new JMenuItem(cAction));
	    	}
	    	
	    	cAction = new KeysCustomAction(this, __("Delete!"), delicon,__("Delete."), __("Delete"),KeyEvent.VK_D, KeysCustomAction.C_DELETE);
	    	cAction.putValue("row", new Integer(row));
	    	popup.add(new JMenuItem(cAction));
    	}
    	return popup;
	}
	public KeysModel getModel(){
		return (KeysModel) super.getModel();
	}
	void initColumnSizes() {
        KeysModel model = (KeysModel)this.getModel();
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
}

@SuppressWarnings("serial")
class KeysCustomAction extends DebateDecideAction {
	public static final int C_REFRESH = 1;
    public static final int C_HIDE = 2;
    public static final int C_UNHIDE = 3;
    public static final int C_DELETE = 4;
    public static final int C_PEER = 5;
	public static final int C_ACOLUMN = 6;
	public static final int C_RCOLUMN = 7;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Keys tree; ImageIcon icon;
	int command;
    public KeysCustomAction(Keys tree,
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
    	KeysModel model = (KeysModel)tree.getModel();
    	if(command == C_REFRESH) {
    		model.update(null, null);
    		tree.initColumnSizes();
    	}else if(command == C_HIDE) {
    		model.hide = true;
    		model.update(null, null);
    	}else if(command == C_UNHIDE) {
        	model.hide = false;
        	model.update(null, null);
        }else	if(command == C_DELETE) {
        	model.delete(row);
        }else	if(command == C_PEER) {
        	model.createPeer(row);
        }else	if(command == C_RCOLUMN) {
        	model.removeColumn(row);
    		tree.initColumnSizes();
        }else	if(command == C_ACOLUMN) {
        	int col = Application_GUI.ask(__("Add"), __("Columns"), Arrays.copyOf(KeysModel.columnNames, KeysModel.columnNames.length, new Object[]{}.getClass()), null, null);
        	if(col == JOptionPane.CLOSED_OPTION) return;
        	if(col < 0) return;
       		model.addColumn(col);
    		tree.initColumnSizes();
        }
    }
}

@SuppressWarnings("serial")
class KeysModel  extends AbstractTableModel implements TableModel, DBListener {
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_TYPE = 1;
	public static final int TABLE_COL_PREF_DATE = 2;
	public static final int TABLE_COL_CONS = 3;
	public static final int TABLE_COL_PEER = 4;
	public static final int TABLE_COL_ORG = 5;
	public static final int TABLE_COL_HIDE = 6;
	public static final int TABLE_COL_ID = 7;
	public static final int TABLE_COL_CREA_DATE = 8;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	
	static public boolean hide = false;
	static public boolean inited_statics = false;
	static String columnNames[];
	static ArrayList<Integer> _ColumnNames;

	static public ArrayList<Integer> prep(){
		ArrayList<Integer> _ColumnNames = new ArrayList<Integer>();
		for(int i=0; i<columnNames.length; i++) _ColumnNames.add(new Integer(i));
		return _ColumnNames;
	}
	static public void init_statics(){
		if(inited_statics) return;
		inited_statics = true;
		columnNames = new String[]{__("Name"),__("Type"),__("P_Date"),__("Constituents"),__("Peer"),__("Organization"),__("Hide"),__("ID"),__("C_Date")};
		_ColumnNames = prep();
	}
	
	/**
	 * Call this to remove a current column
	 * @param crt_col
	 */
	public void removeColumn(int crt_col){
		_ColumnNames.remove(crt_col);
		this.fireTableStructureChanged();
	}
	/**
	 * Call this to add a global column index
	 * @param global_col
	 */
	public void addColumn(int global_col){
		if(_ColumnNames.contains(new Integer(global_col)))
			return;
		_ColumnNames.add(new Integer(global_col));
		this.fireTableStructureChanged();
	}
	
	ArrayList<ArrayList<Object>> _keydata = new ArrayList<ArrayList<Object>>();
	ArrayList<Component> tables= new ArrayList<Component>();
	private DBInterface db;
	private Object monitor_keydata = new Object();
	public KeysModel(DBInterface _db) {
		init_statics();
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.key.TNAME)), null);
		update(null, null);
	}
	public void createPeer(int row) {
		D_Peer peer =
				D_Peer.getPeerByGID_or_GIDhash(Util.getString(_keydata.get(row).get(net.ddp2p.common.table.key.COL_PK)), null, true, true, true, null);
		if (peer.getLID() > 0) {
			int v = Application_GUI.ask(__("Peer already exists, do you want to change its name?"), __("Existing Peer!"), JOptionPane.OK_CANCEL_OPTION);
			if (v != 0) {
				peer.releaseReference();
				return;
			}
		}
		String name  = Application_GUI.input(__("What name do you want for the peer"), __("Creating peer"),
				JOptionPane.QUESTION_MESSAGE);
		peer.component_basic_data.name = name;
		peer.setCreationDate();//.component_basic_data.creation_date = Util.CalendargetInstance();
		SK sk = Cipher.getSK(Util.getString(_keydata.get(row).get(net.ddp2p.common.table.key.COL_SK)));
		peer.sign(sk);
		peer.storeRequest();
		peer.releaseReference();
	}
	public Object getPeer(int row) {
		if (row < 0) return null;
		String pk = Util.getString(_keydata.get(row).get(net.ddp2p.common.table.key.COL_PK));
		D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(pk, null, true, false, false, null);
		if (peer == null) return null;
		return peer.getName_MyOrDefault();
		//return _keydata.get(row).get(SELECT_COL_PEER);
	}
	public void delete(int row) {
		String ID = Util.getString(_keydata.get(row).get(net.ddp2p.common.table.key.COL_ID));
		int v = Application_GUI.ask(__("Are you sure that you want to delete key: "+ID), __("Delete Key"), JOptionPane.OK_CANCEL_OPTION);
		if(v != 0) return;
		try {
			db.delete(net.ddp2p.common.table.key.TNAME,
					new String[]{net.ddp2p.common.table.key.key_ID},
					new String[]{ID},
					DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void setTable(Keys keys) {
		tables.add(keys);
	}
	/*
	static String _sql_nohide = 
			"SELECT "+Util.setDatabaseAlias(table.key.fields_list,"k")+
			",p."+table.peer.name+
			",o."+table.organization.name+
			",COUNT(c."+table.constituent.constituent_ID+")"+
			",c."+table.constituent.forename+
			",c."+table.constituent.name+
			",oc."+table.organization.name+
			//",c."+table.constituent.constituent_ID+
			//",SUM(CASE c."+table.constituent.constituent_ID+" WHEN NULL 0 ELSE 1 END)"+
			" FROM "+table.key.TNAME+" AS k"+
			" LEFT JOIN "+table.peer.TNAME+" AS p ON (p."+table.peer.global_peer_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.global_organization_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON (c."+table.constituent.global_constituent_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS oc ON (c."+table.constituent.organization_ID+"=oc."+table.organization.organization_ID+")"+
			" GROUP BY k."+table.key.key_ID+", k."+table.key.hide
			;
	*/
	static String sql_nohide = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.key.fields_list,"k")+
			",COUNT(c."+net.ddp2p.common.table.constituent.constituent_ID+")"+
			",c."+net.ddp2p.common.table.constituent.forename+
			",c."+net.ddp2p.common.table.constituent.name+
			",c."+net.ddp2p.common.table.constituent.organization_ID+//",oc."+table.organization.name+
			//",c."+table.constituent.constituent_ID+
			//",SUM(CASE c."+table.constituent.constituent_ID+" WHEN NULL 0 ELSE 1 END)"+
			" FROM "+net.ddp2p.common.table.key.TNAME+" AS k"+
			" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON (c."+net.ddp2p.common.table.constituent.global_constituent_ID+"=k."+net.ddp2p.common.table.key.public_key+")"+
			//" LEFT JOIN "+table.organization.TNAME+" AS oc ON (c."+table.constituent.organization_ID+"=oc."+table.organization.organization_ID+")"+
			" GROUP BY k."+net.ddp2p.common.table.key.key_ID+", k."+net.ddp2p.common.table.key.hide
			;
	static String sql_hide = sql_nohide+
			" HAVING (k."+net.ddp2p.common.table.key.hide+" != '1') OR (k."+net.ddp2p.common.table.key.hide+" IS NULL)";
	//static int SELECT_COL_PEER = table.key.FIELDS;
	//static int SELECT_COL_ORG = table.key.FIELDS+1;
	static int SELECT_COL_CONST_NB = net.ddp2p.common.table.key.FIELDS+0;
	static int SELECT_COL_CONST_FORENAME = net.ddp2p.common.table.key.FIELDS+1;
	static int SELECT_COL_CONST_NAME = net.ddp2p.common.table.key.FIELDS+2;
	static int SELECT_COL_CONST_ORG_ID = net.ddp2p.common.table.key.FIELDS+3;
	//static int SELECT_COL_CONST_ID = table.key.FIELDS+3;
	//static int SELECT_COL_CONST_NB2 = table.key.FIELDS+3;
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		
		ArrayList<ArrayList<Object>> keydata;
		try {
			if(hide)
				keydata = db.select(sql_hide, new String[]{}, DEBUG);
			else
				keydata = db.select(sql_nohide, new String[]{}, DEBUG);
			
			synchronized(monitor_keydata){
				_keydata = keydata;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		//this.fireTableDataChanged();
		new TableUpdater(this, null, tables);
	}
	public boolean isCellEditable(int row, int columnIndex) {
		int col=globalColumn(columnIndex);
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_HIDE:
		case TABLE_COL_CONS:
			return true;
		}
		return false;
	}

	@Override
	public int getColumnCount() {
		//return columnNames.length;
		return _ColumnNames.size();
	}
	@Override
	public Class<?> getColumnClass(int column) {
		int col = globalColumn(column);
		if(col == TABLE_COL_HIDE) return Boolean.class;
		if(col == TABLE_COL_CONS) return Integer.class;
		if(col == TABLE_COL_ID) return Integer.class;
		
		return String.class;
	}
	@Override
	public int getRowCount() {
		return _keydata.size();
	}

	public Object getConstituentsValue(int row) {
		long consts = Util.lval(_keydata.get(row).get(SELECT_COL_CONST_NB));
		if(consts <= 1) return getValueAt(row, TABLE_COL_CONS);
		ArrayList<D_Constituent> cs = D_Constituent.getAllConstsByGID(Util.getString(_keydata.get(row).get(net.ddp2p.common.table.key.COL_PK)));
		ArrayList<String> list = new ArrayList<String>();
		for (D_Constituent c : cs) {
			list.add(c.getOrganization().getOrgNameOrMy() +":"+ c.getNameOrMy());
		}
		return new JComboBox<Object>(list.toArray());
		/*
		int S_COL_CONST_NAME = 0;
		int S_COL_CONST_FORENAME = 1;
		int S_COL_CONST_ORG = 2;
		String sql =
				"SELECT "+
						" c."+table.constituent.name+
						",c."+table.constituent.forename+
						",o."+table.organization.name+
						" FROM "+table.constituent.TNAME+" AS c"+
						" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+") "+
						" WHERE c."+table.constituent.global_constituent_ID+"=?;"
						;
		try {
			ArrayList<ArrayList<Object>> r = db.select(sql, new String[]{Util.getString(_keydata.get(row).get(table.key.COL_PK))}, DEBUG);
			ArrayList<String> list = new ArrayList<String>();
			for(ArrayList<Object> o:r){
				Object forename = o.get(S_COL_CONST_FORENAME);
				if(forename!=null) forename = ", "+forename;
				else forename="";
				list.add(
				o.get(S_COL_CONST_ORG)+":"+
				o.get(S_COL_CONST_NAME)+forename);
			}
			return new JComboBox<Object>(list.toArray());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
		*/
	}
	int globalColumn(int crt_col){
		return _ColumnNames.get(crt_col).intValue();
		// return __column.get(_columnNames.get(crt_col)).intValue();
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		synchronized(monitor_keydata){
			return s_getValueAt(rowIndex, columnIndex);
		}
	}
	private Object s_getValueAt(int rowIndex, int columnIndex) {
		if(rowIndex>=_keydata.size()) return null;
		//switch(columnIndex){
		switch(globalColumn(columnIndex)){
		case TABLE_COL_NAME:
			return _keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_NAME);
		case TABLE_COL_TYPE:
			return _keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_TYPE);
		case TABLE_COL_PREF_DATE:
			return _keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_PREF_DATE);
		case TABLE_COL_CREA_DATE:
			return _keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_CREATION_DATE);
		case TABLE_COL_HIDE:
			Object h = _keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_HIDE);
			return new Boolean(Util.stringInt2bool(h, false));
			
		case TABLE_COL_PEER:
		{
				String pk = Util.getString(_keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_PK));
				D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(pk, null, true, false, false, null);
				if (peer == null) return null;
				return peer.getName_MyOrDefault();
		}
			//return _keydata.get(rowIndex).get(SELECT_COL_PEER);
		case TABLE_COL_ORG:
		{
			String pk = Util.getString(_keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_PK));
			D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(pk, null, true, false);
			if (org == null) return null;
			return org.getOrgNameOrMy();
		}
			//return _keydata.get(rowIndex).get(SELECT_COL_ORG);
		case TABLE_COL_CONS:
			//if(consts == 1)	consts = (_keydata.get(rowIndex).get(SELECT_COL_CONST_ID)==null)?0:1;
			long consts = Util.lval(_keydata.get(rowIndex).get(SELECT_COL_CONST_NB));
			if(consts<=0) return "";//"#"+consts;
			Object forename = _keydata.get(rowIndex).get(SELECT_COL_CONST_FORENAME);
			if(forename!=null) forename = ", "+forename;
			else forename="";
			
			String orgID = Util.getString(_keydata.get(rowIndex).get(SELECT_COL_CONST_ORG_ID));
			String orgName= "";
			D_Organization org = D_Organization.getOrgByLID_NoKeep(orgID, true);
			if (org != null) orgName = org.getOrgNameOrMy();
			
			return "#"+consts+":/"+
			orgName+":"+
			_keydata.get(rowIndex).get(SELECT_COL_CONST_NAME)+forename;
			//return new Integer((int)consts);
		case TABLE_COL_ID:
			long id = Util.lval(_keydata.get(rowIndex).get(net.ddp2p.common.table.key.COL_ID));
			return new Integer((int)id);
		}
		return null;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		synchronized(monitor_keydata){
			s_setValueAt(value, row, col);
		}
	}
	private void s_setValueAt(Object value, int row, int col) {
		//switch(col) {
		switch(globalColumn(col)){
		case TABLE_COL_NAME:
			set_my_data(net.ddp2p.common.table.key.name, Util.getString(value), row);
			//set_my_data(table.key.preference_date, Util.getGeneralizedTime(), row);
			break;
		case TABLE_COL_HIDE:
			String val = Util.stringInt2bool(value, false)?"1":"0";
			set_my_data(net.ddp2p.common.table.key.hide, val, row);
			//set_my_data(table.key.preference_date, Util.getGeneralizedTime(), row);
			break;
		}
		//fireTableCellUpdated(row, col);
		//fireTableCellUpdated(row, TABLE_COL_DATE);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _keydata.size()) return;
		if("".equals(value)) value = null;
		if(_DEBUG)System.out.println("Set value =\""+value+"\"");
		String _ID = Util.getString(_keydata.get(row).get(net.ddp2p.common.table.key.COL_ID));
		try {
			db.update(net.ddp2p.common.table.key.TNAME, new String[]{field_name, net.ddp2p.common.table.key.preference_date},
					new String[]{net.ddp2p.common.table.key.key_ID}, new String[]{value, Util.getGeneralizedTime(), _ID}, _DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public String getColumnName(int crt_col) {
		String result = columnNames[globalColumn(crt_col)].toString();
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+crt_col+"]="+result);
		//return columnNames[crt_col].toString();
		//return _columnNames.get(crt_col).toString();
		return result;
	}
}

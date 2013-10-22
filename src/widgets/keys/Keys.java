package widgets.keys;

import static util.Util._;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
//import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
//import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import config.Application;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.P2PDDSQLException;
import util.Util;
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
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
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
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	public KeysModel getModel(){
		return (KeysModel) super.getModel();
	}
	private void initColumnSizes() {
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
class KeysModel  extends AbstractTableModel implements TableModel, DBListener {
	String columnNames[]={_("Name"),_("Type"),_("Date"),_("Constituents"),_("Peer"),_("Organization"),_("Hide")};
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_TYPE = 1;
	public static final int TABLE_COL_DATE = 2;
	public static final int TABLE_COL_CONS = 3;
	public static final int TABLE_COL_PEER = 4;
	public static final int TABLE_COL_ORG = 5;
	public static final int TABLE_COL_HIDE = 6;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public boolean hide = false;
	ArrayList<ArrayList<Object>> _keydata = new ArrayList<ArrayList<Object>>();
	ArrayList<Keys> tables= new ArrayList<Keys>();
	private DBInterface db;
	public KeysModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.key.TNAME)), null);
		update(null, null);
	}
	public void setTable(Keys keys) {
		tables.add(keys);
	}
	static String sql_nohide = 
			"SELECT "+Util.setDatabaseAlias(table.key.fields_list,"k")+
			",p."+table.peer.name+
			",o."+table.organization.name+
			",COUNT(c."+table.constituent.constituent_ID+")"+
			",c."+table.constituent.constituent_ID+
			" FROM "+table.key.TNAME+" AS k"+
			" LEFT JOIN "+table.peer.TNAME+" AS p ON (p."+table.peer.global_peer_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.global_organization_ID+"=k."+table.key.public_key+")"+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON (c."+table.constituent.global_constituent_ID+"=k."+table.key.public_key+")"+
			" GROUP BY k."+table.key.key_ID
			;
	static String sql_hide = sql_nohide+
			" HAVING k."+table.key.hide+"='1'";
	static int SELECT_COL_PEER = table.key.FIELDS;
	static int SELECT_COL_ORG = table.key.FIELDS+1;
	static int SELECT_COL_CONST_NB = table.key.FIELDS+2;
	static int SELECT_COL_CONST_ID = table.key.FIELDS+3;
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		
		try {
			if(hide)
				_keydata = db.select(sql_hide, new String[]{}, DEBUG);
			else
				_keydata = db.select(sql_nohide, new String[]{}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		this.fireTableDataChanged();
	}
	public boolean isCellEditable(int row, int col) {
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_HIDE:
			return true;
		}
		return false;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_HIDE) return Boolean.class;
		if(col == TABLE_COL_CONS) return Integer.class;
		
		return String.class;
	}
	@Override
	public int getRowCount() {
		return _keydata.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_NAME:
			return _keydata.get(rowIndex).get(table.key.COL_NAME);
		case TABLE_COL_TYPE:
			return _keydata.get(rowIndex).get(table.key.COL_TYPE);
		case TABLE_COL_DATE:
			return _keydata.get(rowIndex).get(table.key.COL_DATE);
		case TABLE_COL_HIDE:
			Object h = _keydata.get(rowIndex).get(table.key.COL_HIDE);
			return new Boolean(Util.stringInt2bool(h, false));
			
		case TABLE_COL_PEER:
			return _keydata.get(rowIndex).get(SELECT_COL_PEER);
		case TABLE_COL_ORG:
			return _keydata.get(rowIndex).get(SELECT_COL_ORG);
		case TABLE_COL_CONS:
			//long consts = 1;
		
			long consts = Util.lval(_keydata.get(rowIndex).get(SELECT_COL_CONST_NB));
			if(consts == 1)
				consts = (_keydata.get(rowIndex).get(SELECT_COL_CONST_ID)==null)?0:1;

			return new Integer((int)consts);
		}
		return null;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		switch(col) {
		case TABLE_COL_NAME:
			set_my_data(table.key.name, Util.getString(value), row);
			//set_my_data(table.key.preference_date, Util.getGeneralizedTime(), row);
			break;
		case TABLE_COL_HIDE:
			String val = Util.stringInt2bool(value, false)?"1":"0";
			set_my_data(table.key.hide, val, row);
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
		String _ID = Util.getString(_keydata.get(row).get(table.key.COL_ID));
		try {
			db.update(table.key.TNAME, new String[]{field_name, table.key.preference_date},
					new String[]{table.key.key_ID}, new String[]{value, Util.getGeneralizedTime(), _ID}, _DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}
}

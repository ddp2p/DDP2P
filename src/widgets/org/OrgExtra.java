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
package widgets.org;

import static util.Util._;

import hds.DebateDecideAction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DDIcons;

import data.D_OrgParam;
import data.D_OrgParams;


import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.components.DDCountrySelector;
import widgets.components.LVEditor;
@SuppressWarnings("serial")
public class OrgExtra extends JTable implements MouseListener, ActionListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;
	public SpinEditor spin;
	public LVEditor lvEditor;
	public OIDComboBox comboBox;
	// private ColorRenderer colorRenderer;
	public OrgExtra() {
		super(new OrgExtraModel(Application.db));
		init();
	}
	public OrgExtra(DBInterface _db) {
		super(new OrgExtraModel(_db));
		init();
	}
	public OrgExtra(OrgExtraModel dm) {
		super(dm);
		init();
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		//colorRenderer = new ColorRenderer(getModel());
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		
		TableColumn oidColumn = this.getColumnModel().getColumn(OrgExtraModel.TABLE_COL_OID);
		comboBox = getModel().getOIDComboBox();//new OIDComboBox();
		comboBox.addActionListener(this);
		oidColumn.setCellEditor(new DefaultCellEditor(comboBox));
		
		TableColumn neighColumn = this.getColumnModel().getColumn(OrgExtraModel.TABLE_COL_NEIGHB);
		spin = new SpinEditor();
		neighColumn.setCellEditor(spin);

		TableColumn listColumn = this.getColumnModel().getColumn(OrgExtraModel.TABLE_COL_VALUES);
		lvEditor = new LVEditor();
		listColumn.setCellEditor(lvEditor);
		
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        //scrollPane.setPreferredSize(new Dimension(400, 100));
        jp.add(scrollPane, BorderLayout.CENTER);
		return jp;
    }
	public TableCellRenderer getCellRenderer(int row, int column) {
		//if ((column == OrgExtraModel.TABLE_COL_OID)) return colorRenderer;
		//if ((column == OrgExtraModel.TABLE_COL_NEIGHB))column.setCellRenderer(new SpinnerRenderer(values));
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {_("Label"),_("List of Values separated by ';'. Type to add new, select to delete"),_("A name you provide"),_("Neighborhood Level"),
			_("Required"),_("Can be provided later"),_("Certified"),_("ToolTip for registration"),_("OID")};
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
	public OrgExtraModel getModel(){
		return (OrgExtraModel) super.getModel();
	}
	private void initColumnSizes() {
        OrgExtraModel model = (OrgExtraModel)this.getModel();
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
	@Override
	public void mouseReleased(MouseEvent arg0) {
		jtableMouseReleased(arg0);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}
	/**
	 * This sets the current organization to _orgID
	 * @param _orgID
	 */
	public void setCurrent(String _orgID) {
		lvEditor.editing_stopped(this);
		spin.editing_stopped(this);
		getModel().setCurrent(_orgID);
	}
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	//OrgExtraUpAction uAction;
    	//OrgExtraDownAction prAction;
    	OrgExtraDeleteAction pdAction;
    	OrgExtraAddAction aAction;
    	// uAction = new PeersUseAction(this, _("Toggle"),addicon,_("Toggle it."),_("Will be used to synchronize."),KeyEvent.VK_A);
    	aAction = new OrgExtraAddAction(this, _("Add!"), delicon,_("Add new field."), _("Add"),KeyEvent.VK_A);
    	aAction.putValue("row", new Integer(row));
    	aAction.putValue("org", this.getModel().org_id);
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	if(row<0) return popup;
    	
       	//uAction = new OrgExtraUpAction(this, _("Use!"),addicon,_("Use it."),_("Will be used to synchronize."),KeyEvent.VK_A);
    	//uAction.putValue("row", new Integer(row));
    	//menuItem = new JMenuItem(uAction);
    	//popup.add(menuItem);
    	//
    	//prAction = new OrgExtraDownAction(this, _("Reset!"), reseticon,_("Bring again all data from this."), _("Go restart!"),KeyEvent.VK_R);
    	//prAction.putValue("row", new Integer(row));
    	//popup.add(new JMenuItem(prAction));
    	//
    	pdAction = new OrgExtraDeleteAction(this, _("Delete!"), delicon,_("Delete all data about this."), _("Delete"),KeyEvent.VK_D);
    	pdAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(pdAction));
    	/*
    	popup.addSeparator();
    	Hashtable<String, PluginMenus> mn = this.plugin_menus.get(new Integer(col));
    	if(mn == null) return popup;
    	for(String a : mn.keySet()){
    		PluginMenus pm = mn.get(a);
    		for(PeerPluginAction pa: pm.plugin_menu_action) popup.add(pa);
    		for(PeerPluginMenuItem ma: pm.plugin_menu_item) popup.add(ma);
    	}
    	*/
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
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	@Override
	public void actionPerformed(ActionEvent e) {
		if(DEBUG) System.out.println("OrgExtra:action:: "+e);
		if(e.getSource()==comboBox) {
			comboBox.edit(e);
		}
		//super.actionPerformed(e);
	}
	/**
	 * This sets the highlighted extra to extra_ID
	 * @param extra_ID
	 */
	public void setCurrent(long extra_ID) {
		if(DEBUG)System.out.println("OrgExtraTable:setCurrent:long:"+extra_ID);
		getModel().setCurrent(extra_ID);
	}
}

@SuppressWarnings("serial")
class OrgExtraDeleteAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	OrgExtra tree; ImageIcon icon;
    public OrgExtraDeleteAction(OrgExtra tree,
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
            if(DEBUG)System.err.println("row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		if(DEBUG)System.err.println("Row selected: " + row);
    	}
    	OrgExtraModel model = (OrgExtraModel)tree.getModel();
     	if(row<0) return;
    	String extraID = Util.getString(model.m_extras.get(row).get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
    	try {
			Application.db.delete(table.field_extra.TNAME, new String[]{table.field_extra.field_extra_ID}, new String[]{extraID}, DEBUG);
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class OrgExtraAddAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	OrgExtra tree; ImageIcon icon;
    public OrgExtraAddAction(OrgExtra tree,
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
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row=tree.getSelectedRow();
    		org_id = tree.getModel().org_id;
    		//System.err.println("Row selected: " + row);
    	}
    	OrgExtraModel model = (OrgExtraModel)tree.getModel();
     	String new_global_extra = null;//data.D_OrgParam.makeGID(null);
		if(row<0){
     		try {
     			long extra_ID = Application.db.insert(table.field_extra.TNAME,
						new String[]{table.field_extra.organization_ID, table.field_extra.global_field_extra_ID},
						new String[]{org_id, new_global_extra });
     			tree.setCurrent(extra_ID);
			} catch (SQLiteException e1) {e1.printStackTrace();}
     		return;
     	}
    	//String extraID = Util.getString(model.m_extras.get(row).get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
    	try {
			long extra_ID = Application.db.insert(table.field_extra.TNAME,
					new String[]{table.field_extra.organization_ID, table.field_extra.global_field_extra_ID, table.field_extra.partNeigh},
					new String[]{org_id, new_global_extra,
						Util.getString(model.m_extras.get(row).get(table.field_extra.OPARAM_NEIGH))
					}, DEBUG);
			tree.setCurrent(extra_ID);
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class OrgExtraModel extends AbstractTableModel implements TableModel, DBListener {
	static final int TABLE_COL_LABEL = 0;
	static final int TABLE_COL_VALUES = 1;
	static final int TABLE_COL_DEFAULT = 2;
	static final int TABLE_COL_NEIGHB = 3;
	static final int TABLE_COL_REQUIRED = 4;
	static final int TABLE_COL_LAZY = 5;
	static final int TABLE_COL_CERTIFIED = 6;
	static final int TABLE_COL_TIP = 7;
	static final int TABLE_COL_OID = 8;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DBInterface db;
	//Object _orgs[]=new Object[0];
	String org_id = null;
	String columnNames[]={_("Label"),_("Values"),_("Default"),_("Level"),_("Req"),
			_("Lazy"),_("Cert"),_("Tip"),_("OID")};
	ArrayList<OrgExtra> tables= new ArrayList<OrgExtra>();
	ArrayList<ArrayList<Object>> m_extras;
	public void setTable(OrgExtra orgExtra) {
		tables.add(orgExtra);
	}
	OIDComboBox oidComboBox = new OIDComboBox();
	public OIDComboBox getOIDComboBox() {
		return oidComboBox;
	}
	/**
	 * Set highlight on extra_ID
	 * @param extra_ID
	 */
	public void setCurrent(long extra_ID) {
		if(DEBUG)System.out.println("OrgExtra:setCurrent:long:"+extra_ID);
		//this.fireTableDataChanged();
		for(int k=0;k<this.m_extras.size();k++){
			ArrayList<Object> e = m_extras.get(k);
			Object i = e.get(table.field_extra.OPARAM_EXTRA_FIELD_ID);
			if(i instanceof Integer){
				Integer id = (Integer)i;
				if(id.longValue()==extra_ID) {
					if(DEBUG)System.out.println("OrgExtra:setCurrent:long: found k="+k);
					for(OrgExtra o: tables){
						int tk = o.convertRowIndexToView(k);
						o.setRowSelectionAllowed(true);
						ListSelectionModel selectionModel = o.getSelectionModel();
						selectionModel.setSelectionInterval(tk, tk);
						o.scrollRectToVisible(o.getCellRect(tk, 0, true));
						//o.fireListener(k, 0);
					}
					break;
				}
			}
		}
	}
	public OrgExtraModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.field_extra.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
		update(null, null);
	}
	final static int COL_OID_ID = table.field_extra.org_field_extras+0; 
	final static int COL_OID_NAME = table.field_extra.org_field_extras+1; 
	final static int COL_OID_EXPL = table.field_extra.org_field_extras+2; 
	/**
	 * Sets the organization _orgID as current
	 * @param _orgID
	 */
	public void setCurrent(String _orgID) {
		org_id = _orgID;
		if(org_id==null){
			m_extras = null; return;
		}
		String sql = "SELECT "+Util.setDatabaseAlias(table.field_extra.org_field_extra,"e")+
		",o."+table.oid.oid_ID+",o."+table.oid.OID_name+",o."+table.oid.explanation+
		" FROM "+table.field_extra.TNAME+" AS e " +
				" LEFT JOIN "+table.oid.TNAME+" AS o ON(o."+table.oid.sequence+"=e."+table.field_extra.oid+") " +
						" WHERE "+table.field_extra.organization_ID+"=? GROUP BY e."+table.field_extra.field_extra_ID;
		try {
			m_extras = Application.db.select(sql, new String[]{org_id},DEBUG);
			this.fireTableDataChanged();
			//this.fireTableStructureChanged();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_REQUIRED) return Boolean.class;
		if(col == TABLE_COL_LAZY) return Boolean.class;
		if(col == TABLE_COL_CERTIFIED) return Boolean.class;
		if(col == TABLE_COL_NEIGHB) return Integer.class;		
		return String.class;
	}
	@Override
	public int getRowCount() {
		if(m_extras == null) return 0;
		return m_extras.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		if(DEBUG) System.out.println("OrgExtraModel:getValueAt: "+row+" col="+col);
		if(m_extras==null){
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: null extras");
			return null;
		}
		Object result = null;
		if(row >= this.m_extras.size()){
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: row>="+m_extras.size());
			return result;
		}
		ArrayList<Object> field = this.m_extras.get(row);
		if((field == null) || field.size() <= col){
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: fields="+field);
			return null;
		}
		// String fieldID = Util.getString(field.get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
		switch(col) {
		case TABLE_COL_LABEL: result = field.get(table.field_extra.OPARAM_LABEL); break;
		case TABLE_COL_LAZY: result = field.get(table.field_extra.OPARAM_LATER); break;
		case TABLE_COL_CERTIFIED: result = field.get(table.field_extra.OPARAM_CERT); break;
		case TABLE_COL_REQUIRED: result = field.get(table.field_extra.OPARAM_REQ); break;
		case TABLE_COL_NEIGHB:
			try{result = (Integer)field.get(table.field_extra.OPARAM_NEIGH);}
			catch(Exception e){result = new Integer(table.field_extra.partNeigh_non_neighborhood_indicator+"");}break;
		case TABLE_COL_VALUES: result = field.get(table.field_extra.OPARAM_LIST_VAL); break;
		case TABLE_COL_DEFAULT: result = field.get(table.field_extra.OPARAM_DEFAULT); break;
		case TABLE_COL_TIP: result = field.get(table.field_extra.OPARAM_TIP); break;
		case TABLE_COL_OID:{
			String seq = Util.getString(field.get(table.field_extra.OPARAM_OID));
			result = oidComboBox.getOIDItem(seq);
			/*
			result = new OIDItem(
					field.get(COL_OID_NAME),
					field.get(COL_OID_ID),
					field.get(COL_OID_EXPL),
					seq);
			*/
			break;
		}
		default:
			if(DEBUG) System.out.println("OrgExtraModel:setValueAt: unknown col="+col);
		}
		if(DEBUG) System.out.println("OrgExtraModel:getValueAt:str: "+result);
		switch(col){
		case TABLE_COL_LAZY:
		case TABLE_COL_CERTIFIED:
		case TABLE_COL_REQUIRED:
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt:bool: "+result);
			try{result = new Boolean(("1".equals(result+""))||("true".equals(result+"")));
				if(DEBUG) System.out.println("OrgExtraModel:getValueAt:Bool: "+result);
			}catch(Exception e) {
				e.printStackTrace();
				result = new Boolean(false);
				if(DEBUG) System.out.println("OrgExtraModel:getValueAt:Boolean: "+result);
			}
			break;
		}
		if(DEBUG) System.out.println("OrgExtraModel:getValueAt: "+result);
		return result;
	}
	private int selectByCol(int col){
		int result = -1;
		switch(col) {
		case TABLE_COL_LABEL: result = table.field_extra.OPARAM_LABEL; break;
		case TABLE_COL_LAZY: result = table.field_extra.OPARAM_LATER; break;
		case TABLE_COL_CERTIFIED: result = table.field_extra.OPARAM_CERT; break;
		case TABLE_COL_REQUIRED: result = table.field_extra.OPARAM_REQ; break;
		case TABLE_COL_NEIGHB: result = table.field_extra.OPARAM_NEIGH; break;
		case TABLE_COL_VALUES: result = table.field_extra.OPARAM_LIST_VAL; break;
		case TABLE_COL_DEFAULT: result = table.field_extra.OPARAM_DEFAULT; break;
		case TABLE_COL_TIP: result = table.field_extra.OPARAM_TIP; break;
		case TABLE_COL_OID: result = table.field_extra.OPARAM_OID; break;
		}
		return result;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		if(DEBUG) System.out.println("OrgExtraModel:setValueAt: "+row+" val="+value+" col="+col);
		boolean r=false;
		if(m_extras==null){
			if(_DEBUG) System.out.println("OrgExtraModel:setValueAt: null extras");
			return;
		}
		if(row >= this.m_extras.size()){
			if(_DEBUG) System.out.println("OrgExtraModel:setValueAt: row>="+m_extras.size());
			return;
		}
		ArrayList<Object> field = this.m_extras.get(row);
		if((field == null) || field.size() <= col){
			if(_DEBUG) System.out.println("OrgExtraModel:setValueAt: fields="+field);
			return;
		}
		Object o;
		String s=null;
		switch(col) { // Store strings in s; prepare strings for boolean values
		case TABLE_COL_NEIGHB:
			o=value; break;
		case TABLE_COL_LAZY:
		case TABLE_COL_CERTIFIED:
		case TABLE_COL_REQUIRED:
			o=s=Util.getIntStringBool(value); break;
		case TABLE_COL_OID:
			if (value!=null) {
				if(value instanceof OIDItem) {
					o = s  = ((OIDItem)value).sequence;
				}else{
					String seq = OIDComboBox.saveOID(s);
					o = s = seq;
				}
			}else{
				o = s = null;
			}
			break;
		case TABLE_COL_LABEL:
		case TABLE_COL_VALUES:
		case TABLE_COL_DEFAULT:
		case TABLE_COL_TIP:
		default:
			o = s = Util.getString(value); break;
		}
		field.set(selectByCol(col), o); // store objects in fields, since we do not call sync on database update
		switch(col) { // store in database without sync
		case TABLE_COL_LABEL:r=set_my_data(table.field_extra.label, s, row); break;
		case TABLE_COL_LAZY:r=set_my_data(table.field_extra.can_be_provided_later, s, row); break;
		case TABLE_COL_CERTIFIED:r=set_my_data(table.field_extra.certificated, s, row); break;
		case TABLE_COL_REQUIRED:r=set_my_data(table.field_extra.required, s, row); break;
		case TABLE_COL_NEIGHB:r=set_my_data(table.field_extra.partNeigh, Util.getString(o), row); break;
		case TABLE_COL_VALUES:r=set_my_data(table.field_extra.list_of_values, s, row); break;
		case TABLE_COL_DEFAULT:r=set_my_data(table.field_extra.default_val, s, row); break;
		case TABLE_COL_TIP:r=set_my_data(table.field_extra.tip, s, row); break;
		case TABLE_COL_OID:r=set_my_data(table.field_extra.oid, s, row);break;
		default:
			if(DEBUG) System.out.println("OrgExtraModel:setValueAt: unknown col="+col);
			
		}
		fireTableCellUpdated(row, col);
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("OrgExtraModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}
	@Override
	public boolean isCellEditable(int row, int col) {
		/*
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_SLOGAN:
			return true;
		}
		*/
		return true;
	}
	private boolean set_my_data(String field_name, String value, int row) {
		if(DEBUG)System.out.println("OrgExtraModel:set_my_data: field="+field_name);
		ArrayList<Object> field = this.m_extras.get(row);
		String fieldID = Util.getString(field.get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
		try {
			db.updateNoSync(table.field_extra.TNAME,
					new String[]{field_name}, new String[]{table.field_extra.field_extra_ID}, 
					new String[]{value,fieldID},
					DEBUG);
			
			if(table.field_extra.label.equals(field_name)) {
				D_OrgParam dop = new D_OrgParam(fieldID);
				String hash = data.D_OrgParam.makeGID(value, dop.list_of_values, dop.version);
				db.update(table.field_extra.TNAME, new String[]{table.field_extra.global_field_extra_ID},
					new String[]{table.field_extra.field_extra_ID}, new String[]{hash,fieldID}, DEBUG);
			}
			if(table.field_extra.list_of_values.equals(field_name)) {
				D_OrgParam dop = new D_OrgParam(fieldID);
				String hash = data.D_OrgParam.makeGID(dop.label, dop.list_of_values, dop.version);
				db.update(table.field_extra.TNAME, new String[]{table.field_extra.global_field_extra_ID},
					new String[]{table.field_extra.field_extra_ID}, new String[]{hash,fieldID}, DEBUG);
			}
		
		} catch (SQLiteException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		this.setCurrent(org_id);
	}
}

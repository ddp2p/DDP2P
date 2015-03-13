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

import static util.Util.__;

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
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import config.Application;
import data.D_OID;
import data.D_OrgParam;
import data.D_OrgParams;
import data.D_Organization;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.app.DDIcons;
import widgets.components.DDCountrySelector;
import widgets.components.DebateDecideAction;
import widgets.components.LVEditor;
import widgets.components.LVRenderer;
import widgets.news.NewsModel;

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
        __("Click to sort; Shift-Click to sort in reverse order"));
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
		if ((column == OrgExtraModel.TABLE_COL_VALUES)) return new LVRenderer();
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {__("Label"),__("List of Values separated by ';'. Type to add new, select to delete"),__("A name you provide"),__("Neighborhood Level"),
			__("Required"),__("Can be provided later"),__("Certified"),__("ToolTip for registration"),__("OID")};
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
 
            for (int r = 0; r < model.getRowCount(); r ++) {
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
	public void setCurrentOrg(String _orgID) {
		if (DEBUG) System.out.println("OrgExtra: setCurrentOrg: start " + _orgID);
		lvEditor.editing_stopped(this);
		spin.editing_stopped(this);
		getModel().setCurrentOrg(_orgID);
	}
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	//OrgExtraUpAction uAction;
    	//OrgExtraDownAction prAction;
    	OrgExtraDeleteAction pdAction;
    	OrgExtraAddAction aAction;
    	// uAction = new PeersUseAction(this, _("Toggle"),addicon,_("Toggle it."),_("Will be used to synchronize."),KeyEvent.VK_A);
    	aAction = new OrgExtraAddAction(this, __("Add!"), addicon,__("Add new field."), __("Add"),KeyEvent.VK_A);
    	aAction.putValue("row", new Integer(row));
    	//aAction.putValue("org", this.getModel().org_id);
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	if (row < 0) return popup;
    	
       	//uAction = new OrgExtraUpAction(this, _("Use!"),addicon,_("Use it."),_("Will be used to synchronize."),KeyEvent.VK_A);
    	//uAction.putValue("row", new Integer(row));
    	//menuItem = new JMenuItem(uAction);
    	//popup.add(menuItem);
    	//
    	//prAction = new OrgExtraDownAction(this, _("Reset!"), reseticon,_("Bring again all data from this."), _("Go restart!"),KeyEvent.VK_R);
    	//prAction.putValue("row", new Integer(row));
    	//popup.add(new JMenuItem(prAction));
    	//
    	pdAction = new OrgExtraDeleteAction(this, __("Delete!"), delicon,__("Delete all data about this."), __("Delete"),KeyEvent.VK_D);
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
	public void setCurrentField(long extra_ID) {
		if(DEBUG)System.out.println("OrgExtraTable:setCurrentField:long: eID"+extra_ID);
		getModel().setCurrentField(extra_ID);
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
    	int row = -1;
    	if (src instanceof JMenuItem) {
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if (DEBUG) System.err.println("OrgExtra:del:row property: " + row);
    	} else {
    		row = tree.getSelectedRow();
    		if (DEBUG) System.err.println("OrgExtra:del:Row selected: " + row);
    	}
    	OrgExtraModel model = (OrgExtraModel)tree.getModel();
    	model.deleteRow(row);
    	/*		
    			Util.getString(model.m_extras.get(row).get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
    	try {
			Application.db.delete(table.field_extra.TNAME, new String[]{table.field_extra.field_extra_ID}, new String[]{extraID}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    	*/
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
    	//String org_id=null;
    	
    	if (src instanceof JMenuItem) {
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		//org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row = tree.getSelectedRow();
    		//org_id = tree.getModel().getOrgLID();
    		//System.err.println("Row selected: " + row);
    	}
    	class OrgExtra_SP {
    		public int row;
    		public long id;
    		public OrgExtra tree;
    		OrgExtra_SP (int _row, OrgExtra _tree) {row = _row; tree = _tree;}
    		//OrgExtra_SP (int _row, long _id, OrgExtra _tree) {row = _row; id = _id; tree = _tree;}
    	}
    	new util.DDP2P_ServiceThread("OrgExtraRows", true, new OrgExtra_SP(row, tree)) {

			@Override
			public void _run() {
				OrgExtra_SP tree_sp = (OrgExtra_SP) ctx;
				OrgExtraModel model = (OrgExtraModel) tree_sp.tree.getModel();
		    	long extra_ID = model.addRow(tree_sp.row);
		    	if (extra_ID >= 0) {
					int cnt = model.getRowCount();
		    		tree_sp.row = cnt - 1;
		    		tree_sp.id = extra_ID;
		    		SwingUtilities.invokeLater(new util.DDP2P_ServiceRunnable("OrgExtra_SW", false, false, tree_sp) {
		    			// may be set daemon
						@Override
						public void _run() {
							OrgExtra_SP tree_sp = (OrgExtra_SP) ctx;
							OrgExtraModel model = (OrgExtraModel) tree_sp.tree.getModel();
							model.fireTableRowsInserted(tree_sp.row,  tree_sp.row);
				    		tree.setCurrentField(tree_sp.id);
						}
		    			
		    		});
		    	}
				
			}}.start();
  		return;
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
	//String org_id = null;
	D_Organization org;
	String columnNames[]={__("Label"),__("Values"),__("Default"),__("Level"),__("Req"),
			__("Lazy"),__("Cert"),__("Tip"),__("OID")};
	ArrayList<OrgExtra> tables= new ArrayList<OrgExtra>();
	//ArrayList<ArrayList<Object>> m_extras;
	public void setTable(OrgExtra orgExtra) {
		tables.add(orgExtra);
	}
	public void deleteRow(int row) {
    	if (org == null) {
    		if (DEBUG) System.err.println("OrgExtra:del: no org");
    		return;
    	}
     	if (row < 0) return;
    	//String extraID = Util.getStringID(model.org.getOrgParam(row).field_LID);
     	org = D_Organization.getOrgByOrg_Keep(org);
     	org.deleteOrgParam(row);
     	fireTableRowsDeleted(row, row);
		if (org.dirty_any()) org.storeRequest();
    	org.releaseReference();
	}
	public long addRow(int row) {
     	if (org == null) {
    		System.err.println("OrgExtraModel:addRow: no org for row: " + row);
    		return -1;
    	}
     	//String new_global_extra = null; // data.D_OrgParam.makeGID(null);
    	org = D_Organization.getOrgByOrg_Keep(org);
    	String new_level = table.field_extra.partNeigh_non_neighborhood_indicator+"";
		if (row >= 0) {
			new_level = org.getOrgParam(row).partNeigh+"";
     	}
		//System.err.println("OrgExtraModel:act: orig org: \n" + org);
		long extra_ID = org.addEmptyOrgExtraParam(new_level);
		//System.err.println("OrgExtraModel:act: final org: row=" +extra_ID+" cnt="+this.getRowCount()+"\n" + org);
		//fireTableRowsInserted(getRowCount()-1,  getRowCount()-1);
		if (org.dirty_any()) org.storeRequest();
		org.releaseReference();
		return extra_ID;
	}
	OIDComboBox oidComboBox = new OIDComboBox();
	public OIDComboBox getOIDComboBox() {
		return oidComboBox;
	}
	/**
	 * Set highlight on extra_ID
	 * @param extra_ID
	 */
	public void setCurrentField(long extra_ID) {
		D_Organization _org = this.org;
		if (DEBUG) System.out.println("OrgExtraModel:setCurrentField:long: eID"+extra_ID);
		// Util.printCallPath("Org");
		if (_org == null) {
			if (DEBUG) System.out.println("OrgExtraModel:setCurrent:long: no org");
			return;
		}
		//this.fireTableDataChanged();
		for (int k = 0; k < _org.getOrgParamsLen(); k ++) {
			//ArrayList<Object> e = m_extras.get(k);
			//Object i = e.get(table.field_extra.OPARAM_EXTRA_FIELD_ID);
			D_OrgParam op = _org.getOrgParam(k);
			if (op == null) {
				if (DEBUG) System.out.println("OrgExtraModel: setCurrent: null param at k = " + k);
				Util.printCallPath("Org: "+_org);
				continue;
			}
			if (op.field_LID <= 0) {
				if (DEBUG) System.out.println("OrgExtraModel: setCurrent: null param at k = " + k+" id="+op.field_LID);
				continue;
			}
			
			if (op.field_LID == extra_ID) {
				if (DEBUG) System.out.println("OrgExtraModel: setCurrent:long: found k = " + k);
				for (OrgExtra o: tables) {
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
	public OrgExtraModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.field_extra.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
		load_OIDs();
		update(null, null);
	}
	final static int COL_OID_ID = table.field_extra.org_field_extras+0; 
	final static int COL_OID_NAME = table.field_extra.org_field_extras+1; 
	final static int COL_OID_EXPL = table.field_extra.org_field_extras+2; 
	/**
	 * Sets the organization _orgID as current
	 * @param _orgID
	 */
	public void setCurrentOrg (String _orgID) {
		//boolean DEBUG = true;
		//org_id = _orgID;
		if (DEBUG) System.out.println ("OrgExtraModel: setCurrentOrg: orgID="+_orgID);
		if (_orgID == null) {
			//m_extras = null;
			org = null;
			if (DEBUG) System.out.println ("OrgExtraModel: setCurrentOrg: null orgID="+_orgID);
//			this.fireTableDataChanged();
			SwingUtilities.invokeLater(new util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
				// daemon?
				@Override
				public void _run() {
					((OrgExtraModel)ctx).fireTableDataChanged();
				}
			});
			return;
		}
		
		org = D_Organization.getOrgByLID_NoKeep (_orgID, true);
		//this.fireTableDataChanged();
		
		SwingUtilities.invokeLater(new util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
			// daemon?
			@Override
			public void _run() {
				((OrgExtraModel)ctx).fireTableDataChanged();
			}
		});
		
		/*
		String sql = "SELECT "+Util.setDatabaseAlias(table.field_extra.org_field_extra,"e")+
		",o."+table.oid.oid_ID+",o."+table.oid.OID_name+",o."+table.oid.explanation+
		" FROM "+table.field_extra.TNAME+" AS e " +
				" LEFT JOIN "+table.oid.TNAME+" AS o ON(o."+table.oid.sequence+"=e."+table.field_extra.oid+") " +
						" WHERE "+table.field_extra.organization_ID+"=? GROUP BY e."+table.field_extra.field_extra_ID;
		try {
			m_extras = Application.db.select(sql, new String[]{org_id},DEBUG);
			this.fireTableDataChanged();
			//this.fireTableStructureChanged();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		*/
		
	}
	public void load_OIDs() {
		if (D_OID._load_OIDs ());
		this.fireTableDataChanged();
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
		D_Organization _org = org;
		if (_org == null) return 0;
		return _org.getOrgParamsLen();
	}

	@Override
	public Object getValueAt(int row, int col) {
		//boolean DEBUG = false;
		if (DEBUG) System.out.println("OrgExtraModel:getValueAt: "+row+" col="+col);
		if (org == null) {
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: null extras");
			return null;
		}
		D_OrgParam[] params = org.getOrgParams();
		if (org == null) {
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: null extras");
			return null;
		}
		
		Object result = null;
		if (row >= params.length){
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: row>="+params.length);
			return result;
		}
		/*
		ArrayList<Object> field = this.m_extras.get(row);
		if ((field == null) || field.size() <= col){
			if(DEBUG) System.out.println("OrgExtraModel:getValueAt: fields="+field);
			return null;
		}
		*/
		D_OrgParam param = params[row];
		// String fieldID = Util.getString(field.get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
		switch (col) {
		case TABLE_COL_LABEL: result = param.label; //field.get(table.field_extra.OPARAM_LABEL); 
			break;
		case TABLE_COL_LAZY: result = new Boolean(param.can_be_provided_later);//field.get(table.field_extra.OPARAM_LATER);
			break;
		case TABLE_COL_CERTIFIED: result = new Boolean(param.certificated); //field.get(table.field_extra.OPARAM_CERT);
			break;
		case TABLE_COL_REQUIRED: result = new Boolean(param.required); // field.get(table.field_extra.OPARAM_REQ);
			break;
		case TABLE_COL_NEIGHB:
			result = new Integer (param.partNeigh);
			/*
			try {
				Object o = field.get(table.field_extra.OPARAM_NEIGH);
				if (o == null) 
					result = new Integer(table.field_extra.partNeigh_non_neighborhood_indicator+"");
				else	if(o instanceof Integer) result = (Integer)o;
				else	result = new Integer(Util.ival(o, table.field_extra.partNeigh_non_neighborhood_indicator));
			} catch(Exception e){result = new Integer(table.field_extra.partNeigh_non_neighborhood_indicator+"");}
			*/
			break;
		case TABLE_COL_VALUES: result = param.list_of_values;// Util.concat(param.list_of_values,table.organization.ORG_VAL_SEP, null); //field.get(table.field_extra.OPARAM_LIST_VAL);
			 //System.out.println("OrgExtra: getValueAt: "+Util.concat(param.list_of_values, " ; "));
		     break;
		case TABLE_COL_DEFAULT: result = param.default_value; //field.get(table.field_extra.OPARAM_DEFAULT);
			break;
		case TABLE_COL_TIP: result = param.tip; //field.get(table.field_extra.OPARAM_TIP);
				break;
		case TABLE_COL_OID:{
			String seq = Util.BNOID2String(param.oid);//Util.getString(field.get(table.field_extra.OPARAM_OID));
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
			if (_DEBUG) System.out.println("OrgExtraModel:getValueAt: unknown col="+col);
		}
		if(DEBUG) System.out.println("OrgExtraModel:getValueAt:str: "+result);
		switch (col) {
		case TABLE_COL_LAZY:
		case TABLE_COL_CERTIFIED:
		case TABLE_COL_REQUIRED:
			if (result instanceof Boolean) break;
			if (DEBUG) System.out.println("OrgExtraModel:getValueAt:bool: "+result);
			try {result = new Boolean((Util.stringInt2bool(result+"", false)) || ("true".equals(result+"")));
				if(DEBUG) System.out.println("OrgExtraModel:getValueAt:Bool: "+result);
			} catch(Exception e) {
				e.printStackTrace();
				result = new Boolean(false);
				if (DEBUG) System.out.println("OrgExtraModel:getValueAt:Boolean: "+result);
			}
			break;
		}
		if (DEBUG) System.out.println("OrgExtraModel:getValueAt: "+result);
		return result;
	}
	/*
	private int selectByCol(int col) {
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
	*/
	@Override
	public void setValueAt(Object value, int row, int col) {
		if(DEBUG) System.out.println("OrgExtraModel:setValueAt: "+row+" val="+value+" col="+col);
		if ((org == null)) {
			if (_DEBUG) System.out.println("OrgExtraModel:setValueAt: null org");
			return;
		}
		org = D_Organization.getOrgByOrg_Keep(org);
		_setValueAt(value, row, col);
		if (org.dirty_any()) org.storeRequest();
		org.releaseReference();
	}
	public void _setValueAt(Object value, int row, int col) {
		D_OrgParam[] params = org.getOrgParams();
		boolean r; // = false;
		if ((params == null)) {
			if (_DEBUG) System.out.println("OrgExtraModel:setValueAt: null extra params");
			return;
		}
		if (row >= params.length){
			if (_DEBUG) System.out.println("OrgExtraModel:setValueAt: row>="+params.length);
			return;
		}
		/*
		ArrayList<Object> field = this.m_extras.get(row);
		if((field == null) || field.size() <= col){
			if(_DEBUG) System.out.println("OrgExtraModel:setValueAt: fields="+field);
			return;
		}
		*/
		D_OrgParam param = params[row];
		Object o;
		String s=null;
		switch (col) { // Store strings in s; prepare strings for boolean values
		case TABLE_COL_NEIGHB:
			param.partNeigh = Util.ival(value, table.field_extra.partNeigh_non_neighborhood_indicator);
			o=value; 
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_LAZY:
			param.can_be_provided_later = ((Boolean)value).booleanValue();
			o=s=Util.getIntStringBool(value); 
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_CERTIFIED:
			param.certificated = ((Boolean)value).booleanValue();
			o=s=Util.getIntStringBool(value); 
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_REQUIRED:
			param.required = ((Boolean)value).booleanValue();
			o=s=Util.getIntStringBool(value); 
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_OID:
			if (value != null) {
				if (value instanceof OIDItem) {
					o = s  = ((OIDItem)value).sequence;
				}else{
					String seq = OIDComboBox.saveOID(s);
					o = s = seq;
				}
			}else{
				o = s = null;
			}
			param.oid = Util.string2BIOID(s);
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_LABEL:
			o = s = Util.getString(value);
			param.label = s;
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_VALUES:
			o = s = Util.getString(value);
			 try{param.list_of_values =s.split(Pattern.quote(table.organization.ORG_VAL_SEP));}catch(Exception e){}
				params[row].dirty = true;
				org.dirty_params = true;
			break;
		case TABLE_COL_DEFAULT:
			o = s = Util.getString(value);
			param.default_value = s;
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		case TABLE_COL_TIP:
			o = s = Util.getString(value);
			param.tip = s;
			params[row].dirty = true;
			org.dirty_params = true;
			break;
		default:
			o = s = Util.getString(value);
			if (_DEBUG) System.out.println("OrgExtraModel:setValueAt: unknown col="+col);
			break;
		}
		//field.set(selectByCol(col), o); // store objects in fields, since we do not call sync on database update
		/*
		switch (col) { // store in database without sync
		case TABLE_COL_LABEL:    r=D_Organization.set_my_data(table.field_extra.label, s, param); break;
		case TABLE_COL_LAZY:     r=D_Organization.set_my_data(table.field_extra.can_be_provided_later, s, param); break;
		case TABLE_COL_CERTIFIED:r=D_Organization.set_my_data(table.field_extra.certificated, s, param); break;
		case TABLE_COL_REQUIRED: r=D_Organization.set_my_data(table.field_extra.required, s, param); break;
		case TABLE_COL_NEIGHB:   r=D_Organization.set_my_data(table.field_extra.partNeigh, Util.getString(o), param); break;
		case TABLE_COL_VALUES:   r=D_Organization.set_my_data(table.field_extra.list_of_values, s, param); break;
		case TABLE_COL_DEFAULT:  r=D_Organization.set_my_data(table.field_extra.default_val, s, param); break;
		case TABLE_COL_TIP:      r=D_Organization.set_my_data(table.field_extra.tip, s, param); break;
		case TABLE_COL_OID:      r=D_Organization.set_my_data(table.field_extra.oid, s, param);break;
		default:
			if (DEBUG) System.out.println("OrgExtraModel:setValueAt: unknown col="+col);
		}
		*/
		fireTableCellUpdated(row, col);
	}
	@Override
	public String getColumnName(int col) {
		if (DEBUG) System.out.println("OrgExtraModel:getColumnName: col Header["+col+"]="+columnNames[col]);
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
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if (DEBUG) System.out.println("D_Organization: update: start tables: "+Util.concat(table, ",", "[NULL]"));
		if (org == null) { this.setCurrentField(-1);
		} else {
			String lID = org.getLIDstr_forced();
			if (DEBUG) System.out.println("D_Organization: update: oldID: "+lID);
			this.setCurrentOrg(lID);
		}
	}
}

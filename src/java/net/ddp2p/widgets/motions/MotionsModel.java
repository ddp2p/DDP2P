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
package net.ddp2p.widgets.motions;
import static net.ddp2p.common.util.Util.__;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Document_Title;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.WB_Messages;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.DDP2PColoredItem;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.DDP2PColoredItem.DDP2PColorPair;
import net.ddp2p.widgets.justifications.JustificationsModel;
@SuppressWarnings("serial")
public class MotionsModel extends AbstractTableModel implements TableModel, DBListener, OrgListener, DDP2PColoredItem {
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_CREATOR = 1; 
	public static final int TABLE_COL_CATEGORY = 2; 
	public static final int TABLE_COL_VOTERS_NB = 3;
	public static final int TABLE_COL_ACTIVITY = 4; 
	public static final int TABLE_COL_RECENT = 5; 
	public static final int TABLE_COL_NEWS = 6; 
	public static final int TABLE_COL_PROVIDER = 7; 
	public static final int TABLE_COL_BROADCASTED = 8; 
	public static final int TABLE_COL_BLOCKED = 9; 
	public static final int TABLE_COL_TMP = 10; 
	public static final int TABLE_COL_GID_VALID = 11; 
	public static final int TABLE_COL_SIGN_VALID = 12; 
	public static final int TABLE_COL_HIDDEN = 13; 
	public static final int TABLE_COL_ARRIVAL_DATE = 14; 
	public static final int TABLE_COL_PREFERENCES_DATE = 15; 
	public static final int TABLE_COL_CREATION_DATE = 16; 
	public static int RECENT_DAYS_OLD = 10;
	public static int RECENT_NEWS_DAYS_OLD = 10;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static boolean hide = true;
	DBInterface db;
	D_Motion _motion[]=new D_Motion[0];
	String crt_enhanced=null;
	public final static String[] columnToolTips = {
		__("Short title of the item!"),__("Name of the initiator of the item, if not anonymous!"),
		__("A category for classification!"),
		__("How many constituents select the first choice?"),
		__("How many constituents submit any choice?"),
		__("Recently created, within a number of days, here")+" "+RECENT_DAYS_OLD,
		__("Number of news items linked to this motion"),
		__("Provider"),
		__("Should this be disseminated to others?"),
		__("Should votes and justifications about this be stored when received?"),
		__("Is this a temporary item under editing (not yet finalized?"),
		__("Does this item have a Global Identifier (without which cannot be disseminated)?"),
		__("Is this item signed by an initiator?"),
		__("Hide this item from the view of this user?"),
		__("Date when the last version of this item was received!"),
		__("Date when the local preferences about this item were last modified!"),
		__("Creation date of the item declared by its initiator!"),
		};
	String columnNames[]={
			__("Title"),__("Initiator"),__("Category"),
			__("Support"),__("Voters"),
			__("Hot"),__("News"),__("Provider"), __("^"), __("X"),
			__("T"), __("G"), __("S"), __("H"), __("Arrival"), __("Preferences")
			};
	ArrayList<Motions> tables= new ArrayList<Motions>();
	Hashtable<Long, Integer> rowByID =  new Hashtable<Long, Integer>();
	private String crt_orgID;
	private D_Constituent constituent;
	private D_Organization organization;
	private D_Motion crt_motion;
	public Icon getIcon(int column) {
		switch (column) {
			case TABLE_COL_HIDDEN: 
				return DDIcons.getHideImageIcon("Hidden");
			case TABLE_COL_TMP: 
				return DDIcons.getTmpImageIcon("TMP");
			case TABLE_COL_GID_VALID: 
				return DDIcons.getGIDImageIcon("GID");
			case TABLE_COL_BLOCKED: 
				return DDIcons.getBlockImageIcon("Block");
			case TABLE_COL_BROADCASTED: 
				return DDIcons.getBroadcastImageIcon("Broadcast");
			case TABLE_COL_SIGN_VALID: 
				return DDIcons.getSignedImageIcon("Signed");
			case TABLE_COL_RECENT: 
				return DDIcons.getHotImageIcon("Hot");
			case TABLE_COL_NEWS:
				return DDIcons.getNewsImageIcon("News");
			case TABLE_COL_VOTERS_NB:
				return DDIcons.getSigImageIcon("Support");
			case TABLE_COL_ACTIVITY:
				return DDIcons.getConImageIcon("Voters");	
			case TABLE_COL_CREATOR:
				return DDIcons.getCreatorImageIcon("Creator");	
			case TABLE_COL_PROVIDER:
				return DDIcons.getMailImageIcon("DHL");	
			case TABLE_COL_ARRIVAL_DATE:
				return DDIcons.getLandingImageIcon("Arrival");	
		}
		return null;
	}
	public void setCrtEnhanced(String enhanced) {
		crt_enhanced = enhanced;
		this.update(null, null);
	}
	public MotionsModel(DBInterface _db) {
		db = _db;
		connectWidget();
		update(null, null);
	}
	public void connectWidget() {
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.motion.TNAME,net.ddp2p.common.table.signature.TNAME,net.ddp2p.common.table.my_motion_data.TNAME,net.ddp2p.common.table.constituent.TNAME)), null);
	}
	public void disconnectWidget() {
		db.delListener(this);
	}
	public void setCrtOrg(String orgID){
		crt_orgID = orgID;
		update(null, null);
	}
	public D_Motion getMotion(int row) {
		D_Motion[] _motions = _motion;
		if (row >= _motions.length) return null;
		if (row < 0) return null;
		return _motions[row];
	}
	public boolean isBlocked(int row) {
		D_Motion[] _motions = _motion;
		if (row >= _motions.length) return false;
		if (row < 0) return false;
		return _motions[row].isBlocked();
	}
	public boolean isBroadcasted(int row) {
		D_Motion[] _motions = _motion;
		if(row >= _motions.length) return false;
		if (row < 0) return false;
		return _motions[row].isBroadcasted();
	}
	public boolean isRequested(int row) {
		D_Motion[] _motions = _motion;
		if (row >= _motions.length) return false;
		if (row < 0) return false;
		return _motions[row].isRequested();
	}
	public boolean isServing(int row) {
		if(DEBUG) System.out.println("\n************\nMotionsModel:isServing: row="+row);
		return isBroadcasted(row);
	}
	public boolean toggleServing(int row) {
		if(DEBUG) System.out.println("\n************\nMotionsModel:Model:toggleServing: row="+row);
		D_Motion m = this.getMotion(row);
		if (m != null) return m.toggleServing();
		return false;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == MotionsModel.TABLE_COL_VOTERS_NB) return Integer.class;
		if(col == MotionsModel.TABLE_COL_ACTIVITY) return Integer.class;
		if(col == MotionsModel.TABLE_COL_RECENT) return Boolean.class;
		if(col == MotionsModel.TABLE_COL_NEWS) return Integer.class;
		if(col == MotionsModel.TABLE_COL_BROADCASTED) return Boolean.class;
		if(col == MotionsModel.TABLE_COL_BLOCKED) return Boolean.class;
		if(col == MotionsModel.TABLE_COL_TMP) return Boolean.class;
		if(col == MotionsModel.TABLE_COL_GID_VALID) return Boolean.class;
		if(col == MotionsModel.TABLE_COL_SIGN_VALID) return Boolean.class;
		if(col == MotionsModel.TABLE_COL_HIDDEN) return Boolean.class;
		if ((col == MotionsModel.TABLE_COL_NAME)) return String.class;
		if ((col == MotionsModel.TABLE_COL_CREATOR)) return String.class;
		if ((col == MotionsModel.TABLE_COL_CATEGORY)) return String.class;
		if ((col == MotionsModel.TABLE_COL_CREATION_DATE)) return String.class;
		return super.getColumnClass(col); 
	}
	@Override
	public int getRowCount() {
		return _motion.length;
	}
	public void refresh() {
		final boolean refresh = true;
		for (int row = 0; row < getRowCount(); row ++) {
			 D_Motion _m = this.getMotion(row);
			 _m.getMotionSupport_WithCache(0, refresh);
			 _m.getActivity_WithCache(0, refresh);
			 _m.getCountNews_WithCache(0, refresh);
			 _m.getActivity_WithCache(RECENT_DAYS_OLD, refresh);
			 _m.getCountNews_WithCache(RECENT_DAYS_OLD, refresh);
			 _m.getCountNews_WithCache(-RECENT_NEWS_DAYS_OLD, refresh);
			 _m.getGIDValid_WithMemory(refresh);
			 _m.getSignValid_WithMemory(refresh);
		}
		SwingUtilities.invokeLater (new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("Refresh Motions"), false, false, this) {
			@Override
			public void _run() {
				MotionsModel m = (MotionsModel) this.getContext();
				m.fireTableDataChanged();
			}
		});
	}
	@Override
	public Object getValueAt(int row, int col) {
		return _getValueAt (row, col, false);
	}
	public Object _getValueAt(int row, int col, boolean refresh) {
		Object result = null;
		D_Motion _m = this.getMotion(row);
		if ( _m == null ) return null;
		switch (col) {
		case TABLE_COL_NAME:
			result = _m.getTitleOrMy();
			break;
		case TABLE_COL_HIDDEN:
			result = new Boolean(_m.isHidden());
			break;
		case TABLE_COL_TMP:
			result = new Boolean(_m.isTemporary());
			break;
		case TABLE_COL_BROADCASTED:
			result = _m.isBroadcasted();
			break;
		case TABLE_COL_BLOCKED:
			result = _m.isBlocked();
			break;
		case TABLE_COL_CREATION_DATE:
			result = _m.getCreationDateStr();
			break;
		case TABLE_COL_ARRIVAL_DATE:
			result = _m.getArrivalDateStr();
			break;
		case TABLE_COL_PREFERENCES_DATE:
			result = _m.getPreferencesDateStr();
			break;
		case TABLE_COL_PROVIDER:
			D_Peer r = _m.getProvider();
			if (r == null) result = "";// __("Empty");
			else result = r.getName_MyOrDefault(); 
			break;
		case TABLE_COL_CREATOR:
			result = _m.getCreatorOrMy();
			if (result == null) result = "";// __("Empty");
			break;
		case TABLE_COL_CATEGORY:
			result = _m.getCategoryOrMy();
			break;
		case TABLE_COL_VOTERS_NB:
			result = _m.getMotionSupport_WithCache(0, refresh).getCnt();
			break;
		case TABLE_COL_ACTIVITY: 
			result = new Integer("" + (_m.getActivity_WithCache(0, refresh) + _m.getCountNews_WithCache(0, refresh)));
			break;
		case TABLE_COL_RECENT: 
			result = new Boolean((_m.getActivity_WithCache(RECENT_DAYS_OLD, refresh) + _m.getCountNews_WithCache(RECENT_DAYS_OLD, refresh)) > 0);
			break;
		case TABLE_COL_NEWS: 
			result = new Integer(_m.getCountNews_WithCache(-RECENT_NEWS_DAYS_OLD, refresh)+"");
			break;
		case TABLE_COL_GID_VALID:
			result = _m.getGIDValid_WithMemory(refresh);
			break;
		case TABLE_COL_SIGN_VALID:
			result = _m.getSignValid_WithMemory(refresh);
			break;
		default:
		}
		return result;
	}	
	public boolean isCellEditable(int row, int col) {
		switch (col) {
		case TABLE_COL_NAME:
		case TABLE_COL_CREATOR:
		case TABLE_COL_CATEGORY:
		case TABLE_COL_BROADCASTED:
		case TABLE_COL_BLOCKED:
		case TABLE_COL_TMP:
		case TABLE_COL_HIDDEN:
		case TABLE_COL_GID_VALID:
		case TABLE_COL_SIGN_VALID:
			return true;
		}
		return false;
	}
	public void setTable(Motions motions) {
		tables.add(motions);
	}
	public void setCurrent(long motion_id) {
		if (DEBUG) System.out.println("MotionsModel:setCurrent: id=" + motion_id);
		if (motion_id < 0) {
			for (Motions o: tables) {
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(-1, -1);
				o.fireListener(-1, 0);
			}	
			if (DEBUG) System.out.println("MotionsModel:setCurrent: Done -1");
			return;
		}
		D_Motion[] __motions;
		Integer K;
		synchronized(this) {
			__motions = _motion;
			K = this.rowByID.get(motion_id);
		}
		if (K == null) {
			if (DEBUG) System.out.println("MotionsModel:setCurrent: quit k="+K+" mot_ID="+motion_id);
			return;
		}
		D_Motion i = __motions[K];
		if (DEBUG) System.out.println("MotionsModel:setCurrent: row k="+K+" mot_ID="+i);
		if (i == null) return;
		Long id = i.getLID();
		if ((id != null) && (id.longValue() == motion_id)) {
			for (Motions o: tables) {
				int tk = o.convertRowIndexToView(K);
				if (DEBUG) System.out.println("MotionsModel:setCurrent: loop sel motions tk = "+tk);
				o.setRowSelectionAllowed(true);
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(tk, tk);
				o.scrollRectToVisible(o.getCellRect(tk, 0, true));
				if (DEBUG) System.out.println("MotionsModel:setCurrent: loop sel fire="+K);
				o.fireListener(K, 0);
			}
		}
		this.setCurrentMotion(Util.getStringID(motion_id), null);
		if (DEBUG) System.out.println("MotionsModel:setCurrent: Done");
	}
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update table= "+_table+": info= "+info);
		if (crt_orgID == null) return;
		if (_table != null && !_table.contains(net.ddp2p.common.table.motion.TNAME)) {
			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
				@Override
				public void _run() {
					((MotionsModel)ctx).fireTableDataChanged();
				}
			});
			return;
		}
		D_Motion[] __motions;
		Hashtable<Long, Integer> __rowByID = new Hashtable<Long, Integer>();
		{
			if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update pselect");
			ArrayList<ArrayList<Object>> moti = D_Motion.getAllMotions(crt_orgID, hide, crt_enhanced);
			if (moti.size() == _motion.length) {
				boolean different = false;
				for (int k = 0; k < _motion.length; k++) {
					if (_motion[k].getLID() != Util.lval(moti.get(k).get(D_Motion.SELECT_ALL_MOTI_LID))) {
						different = true;
						break;
					}
				}
				if (! different) {
					SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
						@Override
						public void _run() {
							((MotionsModel)ctx).fireTableDataChanged();
						}
					});
					return;
				}
			}
			__motions = new D_Motion[moti.size()];
			if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update part1");
			for (int k = 0; k < __motions.length; k ++) {
				ArrayList<Object> m = moti.get(k);
				String id = Util.getString(m.get(D_Motion.SELECT_ALL_MOTI_LID));
				__motions[k] = D_Motion.getMotiByLID(id, true, false);
				__rowByID.put(Util.Lval(id), new Integer(k));
				if (DEBUG) System.out.println("widgets.org.Motions: update: put id="+id+" -> k="+k);
			}
			if (DEBUG) System.out.println("widgets.org.Motions: A total of: "+_motion.length);
		}
		Long old_sel_LID[] = new Long[tables.size()];
		synchronized (this) {
			{
				for (int old_view_idx = 0; old_view_idx < old_sel_LID.length; old_view_idx ++) {
					Motions tree = tables.get(old_view_idx);
					int sel_view = tree.getSelectedRow();
					if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update oldselLID["+old_view_idx+"] <- row_view="+sel_view);
					if ((sel_view >= 0) && (sel_view < _motion.length)) {
						int sel_model = tree.convertRowIndexToModel(sel_view);
						if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update old model oldselLID["+old_view_idx+"] <- row_model="+sel_model);
						old_sel_LID[old_view_idx] = _motion[sel_model].getLID();
					}
					if (DEBUG) System.out.println("widgets.motions.MotionsModel: update oldselLID = "+old_sel_LID[old_view_idx]);
				}
			}
			if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update oldsel passed");
			_motion = __motions;
			rowByID = __rowByID;
			if (DEBUG) System.out.println("widgets.org.MotionsModel: data changed: mot");
			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
				@Override
				public void _run() {
					((MotionsModel)ctx).fireTableDataChanged();
				}
			});
			if (DEBUG) System.out.println("widgets.org.MotionsModel: update data changed: mot");
			{
				if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update part");
				for (int crt_view_idx = 0; crt_view_idx < old_sel_LID.length; crt_view_idx ++) {
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update k="+crt_view_idx);
					Motions crt_view = tables.get(crt_view_idx);
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update old selected LID="+old_sel_LID[crt_view_idx]);
					int row_view = -1;
					Long lid = old_sel_LID[crt_view_idx];
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update view idx "+crt_view_idx+" osl="+old_sel_LID+" this="+this);
					if (lid != null && lid.longValue() >= 0) {
						if (DEBUG) System.out.println("widgets.org.MotionsModel: update view idx "+crt_view_idx);
						row_view = findTableRow(crt_view, lid.longValue());
						if (row_view < 0)
							if (_DEBUG) System.out.println("widgets.org.MotionsModel: update selected new row view: "+row_view+
									" from LID=" + old_sel_LID[crt_view_idx]);
					}
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update selected new row view: "+row_view);
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update selected view: "+row_view);
					class O {int row_view; Motions crt_view; O(int _row, Motions _view){row_view = _row; crt_view = _view;}}
					SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("MM invoke swing"), false, false, new O(row_view,crt_view)) {
						@Override
						public void _run() {
							O o = (O)ctx;
							if ((o.row_view >= 0) && (o.row_view < o.crt_view.getRowCount())) o.crt_view.setRowSelectionInterval(o.row_view, o.row_view);
							o.crt_view.initColumnSizes();
						}
					});
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update inited cols");
					crt_view.fireListener(row_view, Motions.A_NON_FORCE_COL);
					if (DEBUG) System.out.println("widgets.org.MotionsModel: update fired Listener (motion)");
					if (DEBUG) Util.printCallPath("");
				}		
				if (DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update done");
			}
		}
	}
	/**
	 * Computes the view (table) row with a given ID
	 * @param crt_view
	 * @param id
	 * @return
	 */
	private int findTableRow(Motions crt_view, Long id) {
		if (DEBUG) System.out.println("widgets.org.Motions: findTableRow1 id = " + id);
		int modelRow = findModelRow(id);
		if (modelRow < 0) return modelRow;
		if (crt_view==null) return -1;
		return crt_view.convertRowIndexToView(modelRow);
	}
	/**
	 * Searches the model row with the ID
	 * @param id
	 * @return
	 */
	private int findModelRow(Long id) {
		if (DEBUG) System.out.println("widgets.org.Motions: findTableRow id = " + id+" #="+this.rowByID.size());
		if (id == null) return -1;
		Integer I = this.rowByID.get(id);
		if (DEBUG) System.out.println("widgets.org.Motions: findTableRow get = " + I);
		if (I == null) return -1;
		return I;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		D_Motion _m = this.getMotion(row);
		if (_m == null) return;
		_m = D_Motion.getMotiByMoti_Keep(_m);
		if (_m == null) return;
		String _value;
		if (_DEBUG) System.out.println("MotionsModel: setValueAt r="+row+" m = " + _m);
		switch (col) {
		case TABLE_COL_NAME:
			if (_DEBUG) System.out.println("MotionsModel: setValueAt name init: "+value);
			if (value instanceof D_Document_Title) {
				if (_DEBUG) System.out.println("MotionsModel: setValueAt name DocumentTitle: "+value);
				D_Document_Title __value = (D_Document_Title) value;
				if (__value.title_document != null)  {
					String format  = __value.title_document.getFormatString();
					if ((format == null) || D_Document.TXT_FORMAT.equals(format))
						value = __value.title_document.getDocumentUTFString();
				}
			}
			if (_DEBUG) System.out.println("MotionsModel: setValueAt name obj: "+value);
			_value = Util.getString(value);
			if (_DEBUG) System.out.println("MotionsModel: setValueAt name str: "+_value);
			if ("".equals(_value)) _value = null;
			if (_DEBUG) System.out.println("MotionsModel: setValueAt name nulled: "+_value);
			if (_m.getNameMy() == null && _value == null) break;
			if (_m.getNameMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudotitle?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			if (_DEBUG) System.out.println("MotionsModel: setValueAt name my: " + _value);
			_m.setNameMy(_value);
			break;
		case TABLE_COL_CREATOR:
			if (_DEBUG) System.out.println("MotionsModel: setValueAt cre obj: "+value);
			_value = Util.getString(value);
			if (_DEBUG) System.out.println("MotionsModel: setValueAt cre str: "+_value);
			if ("".equals(_value)) _value = null;
			if (_DEBUG) System.out.println("MotionsModel: setValueAt cre nulled: "+_value);
			if (_m.getCreatorMy() == null && _value == null) break;
			if (_m.getCreatorMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudocreator?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			_m.setCreatorMy(_value);
			break;
		case TABLE_COL_CATEGORY:
			if (_DEBUG) System.out.println("MotionsModel:setValueAt cat obj: "+value);
			_value = Util.getString(value);
			if (_DEBUG) System.out.println("MotionsModel:setValueAt cat str: "+_value);
			if ("".equals(_value)) _value = null;
			if (_DEBUG) System.out.println("MotionsModel:setValueAt cat nulled: "+_value);
			if (_m.getCategoryMy() == null && _value == null) break;
			if (_m.getCategoryMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudocategory?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			_m.setCategoryMy(_value);
			break;
		case TABLE_COL_BROADCASTED:
			if(!(value instanceof Boolean)) break;
			boolean val1 = ((Boolean)value).booleanValue();
			_m.setBroadcasted(val1);
			break;
		case TABLE_COL_BLOCKED:
			if(!(value instanceof Boolean)) break;
			boolean val2 = ((Boolean)value).booleanValue();
			_m.setBlocked(val2);
			break;
		case TABLE_COL_HIDDEN:
			if(!(value instanceof Boolean)) break;
			boolean val3 = ((Boolean)value).booleanValue();
			_m.setHidden(val3);
			_m.storeRequest();
			_m.releaseReference();
			this.fireTableDataChanged();
			return;
		case TABLE_COL_TMP:
			if (!(value instanceof Boolean)) break;
			boolean val4 = ((Boolean)value).booleanValue();
			int q = 1;
			if (val4) {
				q = Application_GUI.ask(__("Are you sure you want to force setting this to true (stop dissemination)? \nNo recomputes default!s"),
						__("Set temporary to true?"), JOptionPane.YES_NO_CANCEL_OPTION);
			} else {
				q = Application_GUI.ask(__("Are you sure you want to set this to false (disseminate)? \nNo recomputes default!"),
						__("Set temporary to false?"), JOptionPane.YES_NO_CANCEL_OPTION);
			}
			if (0 == q) {
				_m.setTemporary(val4);
			}
			if (1 == q) {
				if (_m.getGID() != null && _m.verifySignature()) {
					_m.setTemporary(false);
				} else {
					_m.setTemporary(true);
				}
			}
			break;
		case TABLE_COL_GID_VALID:
			if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID for "+_m);
			if (!(value instanceof Boolean)) {
				if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID quit not boolean: "+value);
				break;
			}
			boolean val5 = ((Boolean)value).booleanValue();
			int qq;
			if (val5) {
				qq = Application_GUI.ask(__("Are you sure you want to recompute this GID?"),
						__("Set GID valid?"), JOptionPane.OK_CANCEL_OPTION);
				if (qq == 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID user set new ");
					if (
							(_m.getConstituentLIDstr() != null)
							&& (MainFrame.status != null)
							&& (MainFrame.status.getMeConstituent() != null)
							&& (_m.getConstituentLID() == MainFrame.status.getMeConstituent().getLID())
						) {
						if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID motion mine");
					} else {
						if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID motion maybe not mine");
						if (_m.getConstituentLIDstr() != null) {
							if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID motion not mine");
							break;
						}
					}
					_m.setGID(_m.make_ID());
					if (_m.getConstituentLIDstr() != null) {
						if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID motion set non-anonymous");
						_m.setSignature(_m.sign());
					}
				} else {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID user not set new ");
				}
			} else {
				qq = Application_GUI.ask(__("Are you sure you want to remove this GID?"),
						__("Set GID valid?"), JOptionPane.OK_CANCEL_OPTION);
				if (qq == 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt: setGID user set null ");
					_m.setGID(null);
					_m.setTemporary();
				}
			}
			break;
		}
		if (_m.dirty_any()) _m.storeRequest();
		_m.releaseReference();
		this.fireTableRowsUpdated(row, row);
	}
	public boolean isMine(int row) {
		D_Motion _m = getMotion(row);
		if (_m == null) return false;
		String id  = _m.getConstituentLIDstr();
		if (id == null) return false;
		D_Constituent c = D_Constituent.getConstByLID(id, true, false);
		if (!c.isExternal() && (c.getSK() != null))
			return true; 
		return false; 
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("Motions:isNotReady: row="+row);
		D_Motion _m = getMotion(row);
		if (_m == null) return false;
		if (_m.getGID() == null) {
			if (DEBUG) System.out.println("Motions:isNotReady: gid false");
			return true;
		}
		if(DEBUG) System.out.println("Motions:isNotReady: exit false");
		return false;
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if(DEBUG) System.out.println("MotionsModel:orgUpdate start");
		if ((crt_orgID==null) || (!crt_orgID.equals(orgID))){
			synchronized(this) {
				this._motion = new D_Motion[0]; 
				this.rowByID = new Hashtable<Long, Integer>();
			}
			if(DEBUG) System.out.println("MotionsModel:orgUpdate setOrg");
			this.setCrtOrg(orgID);
		}
		this.organization = org;
		if(DEBUG) System.out.println("MotionsModel:orgUpdate done");
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		orgUpdate(orgID, 0, org);
		return;
	}
	public long getConstituentIDMyself() {
		if(GUI_Swing.constituents==null) return -1;
		if(GUI_Swing.constituents.tree==null) return -1;
		if(GUI_Swing.constituents.tree.getModel()==null) return -1;
		return  GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
	}
	public String getConstituentGIDMyself() {
		if(GUI_Swing.constituents==null) return null;
		if(GUI_Swing.constituents.tree==null) return null;
		if(GUI_Swing.constituents.tree.getModel()==null) return null;
		return  GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
	}
	public String getOrganizationID() {
		return  this.crt_orgID;
	}
	public String getMotionIDstr(int row) {
		D_Motion _m = this.getMotion(row);
		if (_m == null) return null;
		return _m.getLIDstr(); 
	}
	public long getMotionID(int row) {
		D_Motion _m = this.getMotion(row);
		if (_m == null) return -1;
		return _m.getLID(); 
	}
	public String getMotionGID(int row) {
		D_Motion _m = this.getMotion(row);
		if (_m == null) return null;
		return _m.getGID(); 
	}
	/**
	 * Add hashes of motion, my_vote, my_used_justification to payload_fix
	 * Not adding whole items to payload (payload only used for an instance of sending)
	 * @param row
	 */
	public void advertise(int row) {
		D_Motion _m = this.getMotion(row);
		String hash = _m.getGID(); 
		String org_hash = this.organization.getGIDH();
		ClientSync.addToPayloadFix(RequestData.MOTI, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
		if (ClientSync.payload.requested == null)ClientSync.payload.requested = new WB_Messages();
		try {
			D_Motion m = D_Motion.getMotiByGID(hash, true, false, Util.lval(this.getOrganizationID()));
			if (ClientSync.USE_PAYLOAD_REQUESTED) ClientSync.payload.requested.moti.add(m);
			D_Vote vote = D_Vote.getOpinionForMotion(m.getLIDstr(), this.getConstituentIDMyself());
			if ((vote != null) && (vote.readyToSend())) {
				ClientSync.addToPayloadFix(RequestData.SIGN, vote.getGID(), org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
				if (ClientSync.USE_PAYLOAD_REQUESTED) ClientSync.payload.requested.sign.add(vote);
				if (vote.getJustificationLIDstr() != null) {
					D_Justification just = D_Justification.getJustByLID(vote.getJustificationLIDstr(), true, false);
					if ((just!=null)&&(just.readyToSend())){
						ClientSync.addToPayloadFix(RequestData.JUST, just.getGID(), org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
						if (ClientSync.USE_PAYLOAD_REQUESTED) ClientSync.payload.requested.just.add(just);
					}
				}
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public int getRow(String motID) {
		Integer row = rowByID.get(Util.Lval(motID));
		if ( row == null ) return -1;
		return row.intValue();
	}
	@Override
	public DDP2PColorPair getColors(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row_view, int column_view,
			Component component) {
		boolean crt_is_answered = false, crt_is_answering = false;
		if (DEBUG) System.out.println("MotionsModel: getColors: r="+row_view+" c="+column_view+" o="+value);
		int m_row = table.convertRowIndexToModel(row_view);
		int m_col = table.convertColumnIndexToModel(column_view);
		if (m_col == MotionsModel.TABLE_COL_NAME) {
			D_Motion crt = this.getMotion(m_row);
			if (crt != null && this.crt_motion != null) {
				if (crt.getLID() == crt_motion.getEnhancedMotionLID()) {
					crt_is_answered = true;
				}
				if (crt.getEnhancedMotionLID() == crt_motion.getLID()) {
					crt_is_answering = true;
				}
			}
		}
		DDP2PColorPair result;
		if (value instanceof String) {
			if (crt_is_answered) result = new DDP2PColorPair(Color.RED.brighter(), Color.WHITE, net.ddp2p.widgets.app.DDIcons.getAnsweredImageIcon("justs"));
			else if (crt_is_answering) result = new DDP2PColorPair(Color.BLUE.brighter(), Color.WHITE, net.ddp2p.widgets.app.DDIcons.getAnsweringImageIcon("justs"));
			else result = new DDP2PColorPair(Color.GREEN.darker(), Color.WHITE, null);
		} else {
			if (crt_is_answered) result = new DDP2PColorPair(Color.RED.darker(), Color.WHITE, net.ddp2p.widgets.app.DDIcons.getAnsweredImageIcon("justs"));
			else if (crt_is_answering) result = new DDP2PColorPair(Color.BLUE.darker(), Color.WHITE, net.ddp2p.widgets.app.DDIcons.getAnsweringImageIcon("justs"));
			else result = new DDP2PColorPair(Color.BLACK, Color.WHITE, null); 
		}
		if (DEBUG) System.out.println("JustificationsModel: getColors: answered="+crt_is_answered+" ing="+crt_is_answering+ " r="+result);
		if (isSelected) {
			if (component != null && component instanceof JLabel) {
				JLabel c = (JLabel) component;
				result.setBackground(c.getBackground());
				result.setForeground(c.getForeground());
			} else {
				result.setBackground(Color.BLUE);
			}
		}
		return result;
	}
	public void setCurrentMotion(String motID, D_Motion d_motion) {
		if (d_motion != null || motID == null) {
			crt_motion = d_motion;
		} else {
			D_Motion m = D_Motion.getMotiByLID(motID, true, false);
			crt_motion = m;
		}
    	SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("Motion Answering Colors"), false, false, this) {
    		public void _run () {
    			MotionsModel jm = (MotionsModel) ctx;
    			if (jm.getRowCount() > 100) {
    				jm.fireTableDataChanged();
    				return;
    			}
				for (int crt_row = 0; crt_row < jm.getRowCount(); crt_row ++ ) {
					jm.fireTableCellUpdated(crt_row, MotionsModel.TABLE_COL_NAME);
				}
    		}
    	});
		return;
	}
}

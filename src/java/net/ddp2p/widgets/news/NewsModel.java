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
package net.ddp2p.widgets.news;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Document_Title;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.table.my_news_data;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.motions.Motions;
import net.ddp2p.widgets.motions.MotionsModel;
import net.ddp2p.widgets.news.NewsTable;
@SuppressWarnings("serial")
public class NewsModel extends AbstractTableModel implements OrgListener, TableModel, DBListener, MotionsListener {
	public  int TABLE_COL_NAME = -2;
	public  int TABLE_COL_CREATOR = -2; 
	public  int TABLE_COL_VOTERS_NB = -2;
	public  int TABLE_COL_CREATION_DATE = -2; 
	public  int TABLE_COL_ACTIVITY = -4; 
	public  int TABLE_COL_RECENT = -5; 
	public  int TABLE_COL_NEWS = -6; 
	public  int TABLE_COL_CATEGORY = -7; 
	public boolean show_name = true;
	public boolean show_creator = true;
	public boolean show_voters = false;
	public boolean show_creation_date = true;
	public boolean show_activity = false;
	public boolean show_recent = false;
	public boolean show_news = false;
	public boolean show_category = false;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static final  Object monitor_update = new Object(); 
	DBInterface db;
	Object _news[]=new Object[0];
	Object _hash[]=new Object[0];
	Object _crea[]=new Object[0];
	Object _crea_date[]=new Object[0];
	boolean[] _gid=new boolean[0];
	boolean[] _blo=new boolean[0]; 
	boolean[] _req=new boolean[0]; 
	boolean[] _bro=new boolean[0]; 
	String columnNames[] = getCrtColumns();
	private String[] getCrtColumns() {
		int crt = 0;
		ArrayList<String> cols = new ArrayList<String>();
		if(show_name){ cols.add(__("Name")); TABLE_COL_NAME = crt++;}
		if(show_creator){ cols.add(__("Initiator")); TABLE_COL_CREATOR = crt++;}
		if(show_voters){ cols.add(__("Voters")); TABLE_COL_VOTERS_NB = crt++;}
		if(show_creation_date){ cols.add(__("Date")); TABLE_COL_CREATION_DATE = crt++;}
		if(show_activity){ cols.add(__("Activity")); TABLE_COL_ACTIVITY = crt++;}
		if(show_recent){ cols.add(__("Hot")); TABLE_COL_RECENT = crt++;}
		if(show_news){ cols.add(__("News")); TABLE_COL_NEWS = crt++;}
		if(show_category){ cols.add(__("Category")); TABLE_COL_CATEGORY = crt++;}
		return cols.toArray(new String[0]);
	}
	ArrayList<NewsTable> tables= new ArrayList<NewsTable>();
	private String crt_motionID;
	private String crt_choice;
	private String crt_orgID;
	private D_Organization organization;
	public NewsModel(DBInterface _db) {
		db = _db;
		connectWidget();
		update(null, null);
	}
	public void connectWidget() {
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.news.TNAME)), null);
	}
	public void disconnectWidget() {
		db.delListener(this);
	}
	public void setCrtChoice(String choice){
		if(DEBUG) System.out.println("\n************\nNewsModel:setCrtChoice: choice="+choice);
		crt_choice=choice;
		update(null, null);
		if(DEBUG) System.out.println("\n************\nNewsModel:setCrtChoice: Done");
	}
	public void setCrtMotion(String motionID){
		if(DEBUG) System.out.println("\n************\nNewsModel:setCrtMotion: mID="+motionID);
		crt_motionID=motionID;
		update(null, null);
		if(DEBUG) System.out.println("\n************\nNewsModel:setCrtMotion: Done");
	}
	public void setCrtOrg(String orgID){
		if(DEBUG) System.out.println("\n************\nNewsModel:setCrtMotion: mID="+orgID);
		crt_orgID=orgID;
		update(null, null);
		if(DEBUG) System.out.println("\n************\nNewsModel:setCrtMotion: Done");
	}
	public boolean isBlocked(int row) {
		if(row>=_blo.length) return false;
		return _blo[row];
	}
	public boolean isBroadcasted(int row) {
		if(row>=_bro.length) return false;
		return _bro[row];
	}
	public boolean isRequested(int row) {
		if(row>=_req.length) return false;
		return _req[row];
	}
	public boolean isServing(int row) {
		if(DEBUG) System.out.println("\n************\nNewsModel:isServing: row="+row);
		return isBroadcasted(row);
	}
	public boolean toggleServing(int row) {
		if(DEBUG) System.out.println("\n************\nNewsModel:Model:toggleServing: row="+row);
		String news_ID = Util.getString(this._news[row]);
		boolean result = toggleServing(news_ID, true, false);
		if(DEBUG) System.out.println("NewsModel:Model:toggleServing: result="+result+"\n************\n");
		return result;
	}
	/**
	 * Sets serving both in peer.served_orgs and in organization.broadcasted
	 * has to sign the peer again because of served_orgs changes
	 * @param organization_ID
	 * @param toggle
	 * @param val
	 * @return
	 */
	public static boolean toggleServing(String organization_ID, boolean toggle, boolean val) {
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
		if(col == this.TABLE_COL_RECENT) return Boolean.class;
		if(col == this.TABLE_COL_ACTIVITY) return Integer.class;
		if(col == this.TABLE_COL_VOTERS_NB) return Integer.class;
		return String.class;
	}
	@Override
	public int getRowCount() {
		return _news.length;
	}
	@Override
	public Object getValueAt(int row, int col) {
		Object result = null;
		String motID = Util.getString(this._news[row]);
		if (col == TABLE_COL_CREATION_DATE) {
			if((row>=0) && (row<this._crea_date.length))result = this._crea_date[row];
			if(DEBUG) System.out.println("NewsModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_NAME) {
			String sql =
				"SELECT o."+net.ddp2p.common.table.news.title + 
				", m."+net.ddp2p.common.table.my_news_data.name +
				", o."+net.ddp2p.common.table.news.title_type +
					" FROM "+net.ddp2p.common.table.news.TNAME+" AS o" +
					" LEFT JOIN "+net.ddp2p.common.table.my_news_data.TNAME+" AS m " +
					" ON (o."+net.ddp2p.common.table.news.news_ID+" = m."+net.ddp2p.common.table.my_news_data.news_ID+")" +
					" WHERE o."+net.ddp2p.common.table.news.news_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql, new String[]{motID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("NewsModel:Got my="+result);
					}
					else{
						D_Document_Title dt = new D_Document_Title();
						dt.title_document.setFormatString(Util.getString(orgs.get(0).get(2)));
						dt.title_document.setDocumentString(Util.getString(orgs.get(0).get(0)));
						result = dt;
						if(DEBUG)System.out.println("NewsModel:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if (col == TABLE_COL_CREATOR) {
			String sql_cr = "SELECT o."+net.ddp2p.common.table.news.constituent_ID+", m."+net.ddp2p.common.table.my_news_data.creator+//", c."+table.constituent.name+
			" FROM "+net.ddp2p.common.table.news.TNAME+" AS o " +
			" LEFT JOIN "+net.ddp2p.common.table.my_news_data.TNAME+" AS m " + " ON(o."+net.ddp2p.common.table.news.news_ID+"=m."+net.ddp2p.common.table.my_news_data.news_ID+")"+
			" WHERE o."+net.ddp2p.common.table.news.news_ID+" = ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cr, new String[]{motID});
				if (orgs.size() > 0)
					if (orgs.get(0).get(1) != null) {
						result = Util.getString(orgs.get(0).get(1));
						if(DEBUG)System.out.println("News:Got my="+result);
					}
					else {
						String cID = Util.getString(orgs.get(0).get(0));
						D_Constituent c = D_Constituent.getConstByLID(cID, true, false);
						if (c != null) result = c.getNameOrMy();
						if(DEBUG)System.out.println("NewsModel:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	public boolean isCellEditable(int row, int col) {
		if (col == TABLE_COL_NAME) return true;
		if (col == TABLE_COL_CREATOR) return true;
		if (col == TABLE_COL_CATEGORY) return true;
		return false;
	}
	public void setTable(NewsTable news) {
		tables.add(news);
	}
	public void setCurrent(long just_id) {
		if(DEBUG) System.out.println("NewsModel:setCurrent: id="+just_id);
		if(just_id<0){
			for(NewsTable o: tables){
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(-1, -1);
				o.fireListener(-1, 0);
			}	
			if(DEBUG) System.out.println("NewsModel:setCurrent: Done -1");
			return;
		}
		for(int k=0;k<_news.length;k++){
			Object i = _news[k];
			if(DEBUG) System.out.println("NewsModel:setCurrent: k="+k+" row_just_ID="+i);
			Long id = Util.Lval(i);
			if((id != null) && (id.longValue()==just_id)) {
					for(NewsTable o: tables){
						int tk = o.convertRowIndexToView(k);
						o.setRowSelectionAllowed(true);
						ListSelectionModel selectionModel = o.getSelectionModel();
						selectionModel.setSelectionInterval(tk, tk);
						o.scrollRectToVisible(o.getCellRect(tk, 0, true));
						o.fireListener(k, 0);
					}
					break;
			}
		}
		if(DEBUG) System.out.println("NewsModel:setCurrent: Done");
	}
	private static String sql_news = 
			"SELECT n."+net.ddp2p.common.table.news.news_ID+",n."+net.ddp2p.common.table.news.creation_date+",n."
			+net.ddp2p.common.table.news.global_news_ID+",n."+net.ddp2p.common.table.news.constituent_ID+
			","+net.ddp2p.common.table.news.blocked+
			",n."+net.ddp2p.common.table.news.broadcasted+
			",n."+net.ddp2p.common.table.news.requested
			+" FROM "+net.ddp2p.common.table.news.TNAME+" AS n "
			+" WHERE n."+net.ddp2p.common.table.news.news_ID+" IS NOT NULL ";
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("\nwidgets.news.NewsModel: update table= "+_table+": info= "+info);
		if(DEBUG) System.out.println("\nwidgets.news.NewsModel: crt_motion_id = "+this.crt_motionID);
		String sql = sql_news;
		ArrayList<String> params = new ArrayList<String>(); 
		if (this.crt_motionID != null) {
			sql += " AND n."+net.ddp2p.common.table.news.motion_ID + "=? ";
			params.add(this.crt_motionID);
		}
		if (this.crt_orgID != null) {
			sql += " AND n."+net.ddp2p.common.table.news.organization_ID + "=? ";
			params.add(this.crt_orgID);
		}
		Object old_sel[] = new Object[tables.size()];
		synchronized(monitor_update) {
			try {
				for (int old_view_idx = 0; old_view_idx < old_sel.length; old_view_idx ++) {
					NewsTable old_view = tables.get(old_view_idx);
					int sel_view = old_view.getSelectedRow();
					if ((sel_view >= 0) && (sel_view < _news.length)) {
						int sel_model = old_view.convertRowIndexToModel(sel_view);
						old_sel[old_view_idx] = _news[sel_model];
					}
				}
				ArrayList<ArrayList<Object>> news_data;
				news_data = db.select(sql, params.toArray(new String[0]), DEBUG);
				_news = new Object[news_data.size()];
				_hash = new Object[news_data.size()];
				_crea = new Object[news_data.size()];
				_crea_date = new Object[news_data.size()];
				_gid = new boolean[news_data.size()]; 
				_blo = new boolean[news_data.size()];
				_bro = new boolean[news_data.size()];
				_req = new boolean[news_data.size()];
				for (int k = 0; k < _news.length; k ++) {
					_news[k] = news_data.get(k).get(0);
					_crea_date[k] = news_data.get(k).get(1);
					_hash[k] = news_data.get(k).get(2);
					_crea[k] = news_data.get(k).get(3);
					_gid[k] = (news_data.get(k).get(3) != null);
					_blo[k] = "1".equals(news_data.get(k).get(4));
					_bro[k] = "1".equals(news_data.get(k).get(5));
					_req[k] = "1".equals(news_data.get(k).get(6));
				}
				if(DEBUG) System.out.println("widgets.org.News: A total of: "+_news.length);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
			@Override
			public void _run() {
				((NewsModel)ctx).fireTableDataChanged();
			}
		});
		for ( int crt_view_idx = 0; crt_view_idx < old_sel.length; crt_view_idx ++) {
			NewsTable crt_view = tables.get(crt_view_idx);
			int row_model = findModelRow(old_sel[crt_view_idx]);
			if(DEBUG) System.out.println("widgets.org.News: selected row: "+row_model);
			class O {int row_model; NewsTable crt_view; O(int _row, NewsTable _view) {row_model = _row; crt_view = _view;}}
			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, new O(row_model,crt_view)) {
				@Override
				public void _run() {
					O o = (O)ctx;
					if ((o.row_model >= 0) && (o.row_model < o.crt_view.getModel().getRowCount())) {
						int row_view = o.crt_view.convertRowIndexToView(o.row_model);
						o.crt_view.setRowSelectionInterval(row_view, row_view);
					}
					o.crt_view.initColumnSizes();
				}
			});
			crt_view.fireListener(row_model, NewsTable.A_NON_FORCE_COL);
		}		
		if(DEBUG) System.out.println("widgets.org.News: Done");
	}
	private int findModelRow(Object id) {
		if (id == null) return -1;
		for (int k = 0; k < _news.length; k ++) if (id.equals(_news[k])) return k;
		return -1;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		if(col == TABLE_COL_NAME)
			set_my_data(net.ddp2p.common.table.my_news_data.name, Util.getString(value), row);
		if(col == TABLE_COL_CREATOR)
			set_my_data(net.ddp2p.common.table.my_news_data.creator, Util.getString(value), row);
		fireTableCellUpdated(row, col);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _news.length) return;
		if("".equals(value)) value = null;
		if(DEBUG)System.out.println("Set value =\""+value+"\"");
		String news_ID = Util.getString(_news[row]);
		try {
			String sql = "SELECT "+field_name+" FROM "+net.ddp2p.common.table.my_news_data.TNAME+" WHERE "+net.ddp2p.common.table.my_news_data.news_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{news_ID});
			if(orgs.size()>0){
				db.update(net.ddp2p.common.table.my_news_data.TNAME, new String[]{field_name},
						new String[]{net.ddp2p.common.table.my_news_data.news_ID}, new String[]{value, news_ID});
			}else{
				if(value==null) return;
				db.insert(net.ddp2p.common.table.my_news_data.TNAME,
						new String[]{field_name,net.ddp2p.common.table.my_news_data.news_ID},
						new String[]{value, news_ID});
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Is the creator of this news, myself?
	 * @param row
	 * @return
	 */
	public boolean isMine(int row) {
		if(row >= _news.length) return false;
		D_Constituent c = D_Constituent.getConstByLID(Util.getString(_crea[row]), true, false);
		if (!c.isExternal() && (c.getSK() != null))
			return true; 
		return false; 
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("News:isNotReady: row="+row);
		if(row >= _news.length) {
			if(DEBUG) System.out.println("News:isNotReady: row>"+_news.length);
			return false;
		}
		if(!_gid[row]){
			if(DEBUG) System.out.println("News:isNotReady: gid false");
			return true;
		}
		String cID=Util.getString(_crea[row]);
		if(cID == null){
			if(DEBUG) System.out.println("News:isNotReady: cID null");
			return true;
		}
		if(DEBUG) System.out.println("News:isNotReady: exit false");
		return false;
	}
	public static void setBlocking(String newsID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		try {
			Application.getDB().update(net.ddp2p.common.table.news.TNAME,
					new String[]{net.ddp2p.common.table.news.blocked},
					new String[]{net.ddp2p.common.table.news.news_ID},
					new String[]{val?"1":"0", newsID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * change org.broadcasted Better change with toggleServing which sets also peer.served_orgs
	 * @param orgID
	 * @param val
	 */
	public static void setBroadcasting(String newsID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+newsID);
		try {
			Application.getDB().update(net.ddp2p.common.table.news.TNAME,
					new String[]{net.ddp2p.common.table.news.broadcasted},
					new String[]{net.ddp2p.common.table.news.news_ID},
					new String[]{val?"1":"0", newsID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}
	public static void setRequested(String newsID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setRequested: set="+val);
		try {
			Application.getDB().update(net.ddp2p.common.table.news.TNAME,
					new String[]{net.ddp2p.common.table.news.requested},
					new String[]{net.ddp2p.common.table.news.news_ID},
					new String[]{val?"1":"0", newsID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void motion_forceEdit(String motionID) {
		return;
	}
	@Override
	public void motion_update(String motionID, int col, D_Motion d_motion) {
		if(DEBUG) System.out.println("\n************\nNewsModel:motUpdate:motID="+motionID);
		if ((crt_motionID==null) || (!crt_motionID.equals(motionID))){
			if(DEBUG) System.out.println("NewsModel:motUpdate: new justs");
			this._news= new Object[0]; 
			this.setCrtMotion(motionID);
		}
		if(DEBUG) System.out.println("\n************\nNewsModel:motUpdate: Done");
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if(DEBUG) System.out.println("\n************\nNewsModel:orgUpdate:orgID="+orgID);
		this.organization = org;
		this.setCrtOrg(orgID);
	}
	public long getConstituentIDMyself() {
		return  GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
	}
	public String getConstituentGIDMyself() {
		return  GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
	}
	public String getOrganizationID() {
		return  this.crt_orgID;
	}
	public String getMotionID() {
		return this.crt_motionID;
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
	}
}

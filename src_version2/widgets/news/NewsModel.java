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
package widgets.news;

import static util.Util.__;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import util.P2PDDSQLException;
import config.Application;
import config.Identity;
import config.MotionsListener;
import config.OrgListener;
import data.D_Constituent;
import data.D_Document_Title;
import data.D_Motion;
import data.D_Organization;
import table.my_news_data;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.components.GUI_Swing;
import widgets.news.NewsTable;

@SuppressWarnings("serial")
public class NewsModel extends AbstractTableModel implements OrgListener, TableModel, DBListener, MotionsListener {
	public  int TABLE_COL_NAME = -2;
	public  int TABLE_COL_CREATOR = -2; // certified by trusted?
	public  int TABLE_COL_VOTERS_NB = -2;
	public  int TABLE_COL_CREATION_DATE = -2; // any activity in the last x days?
	public  int TABLE_COL_ACTIVITY = -4; // number of votes + news
	public  int TABLE_COL_RECENT = -5; // any activity in the last x days?
	public  int TABLE_COL_NEWS = -6; // unread news?
	public  int TABLE_COL_CATEGORY = -7; // certified by trusted?
	//public static final int TABLE_COL_PLUGINS = -8;
	
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
	DBInterface db;
	Object _news[]=new Object[0];
	//Object _meth[]=new Object[0];
	Object _hash[]=new Object[0];
	Object _crea[]=new Object[0];
	Object _crea_date[]=new Object[0];
	//Object _votes[]=new Object[0];
	boolean[] _gid=new boolean[0];
	boolean[] _blo=new boolean[0]; // block
	boolean[] _req=new boolean[0]; // request
	boolean[] _bro=new boolean[0]; // broadcast
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
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.news.TNAME)), null);
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
	//@Override
	//public void addTableModelListener(TableModelListener arg0) {}

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
		// if(col == this.TABLE_COL_NEWS) return Integer.class;
		
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
			//if(!this.show_creation_date) break;
			if((row>=0) && (row<this._crea_date.length))result = this._crea_date[row];
			if(DEBUG) System.out.println("NewsModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_NAME) {
			//if(!this.show_name) break;
			String sql =
				"SELECT o."+table.news.title + 
				", m."+table.my_news_data.name +
				", o."+table.news.title_type +
					" FROM "+table.news.TNAME+" AS o" +
					" LEFT JOIN "+table.my_news_data.TNAME+" AS m " +
					" ON (o."+table.news.news_ID+" = m."+table.my_news_data.news_ID+")" +
					" WHERE o."+table.news.news_ID+"= ? LIMIT 1;";
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
			// if(!this.show_creator) break;
			String sql_cr = "SELECT o."+table.news.constituent_ID+", m."+table.my_news_data.creator+//", c."+table.constituent.name+
			" FROM "+table.news.TNAME+" AS o " +
			" LEFT JOIN "+table.my_news_data.TNAME+" AS m " + " ON(o."+table.news.news_ID+"=m."+table.my_news_data.news_ID+")"+
			//" LEFT JOIN "+table.constituent.TNAME+" AS c " +" ON(o."+table.news.constituent_ID+"=c."+table.constituent.constituent_ID+")"+
			" WHERE o."+table.news.news_ID+" = ? LIMIT 1;";
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
						//result = Util.getString(orgs.get(0).get(2));
						if(DEBUG)System.out.println("NewsModel:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		/*
		if (col == TABLE_COL_CATEGORY) {
			String sql_cat = "SELECT o."+table.news.category + ", m."+table.my_news_data.category+
					" FROM "+table.news.TNAME+" AS o" +
					" LEFT JOIN "+table.my_news_data.TNAME+" AS m " +
							" ON (o."+table.news.news_ID+" = m."+table.my_news_data.news_ID+")" +
					" WHERE o."+table.news.news_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cat, new String[]{motID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("News:Got my="+result);
					}
					else{
						result = orgs.get(0).get(0);
						if(DEBUG)System.out.println("News:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		
		if (col == TABLE_COL_VOTERS_NB) {
			if((row>=0) && (row<this._votes.length)){
				result = this._votes[row];
				return result;
			}
			String sql_co = "SELECT count(*) FROM "+table.signature.TNAME+
			" WHERE "+table.signature.news_ID+" = ? AND "+table.signature.choice+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_co, new String[]{motID, "y"});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
			
		if (col == TABLE_COL_ACTIVITY) { // number of all votes + news
			String sql_ac = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE "+table.signature.news_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac, new String[]{motID});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return result;
			}
			
			String sql_new = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE "+table.news.news_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new, new String[]{motID});
				if(orgs.size()>0) result = new Integer(""+(((Integer)result).longValue()+((Integer)orgs.get(0).get(0)).longValue()));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
			
		if (col == TABLE_COL_RECENT) { // any activity in the last x days?
			int DAYS_OLD2 = 10;
			String sql_ac2 = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE s."+table.signature.news_ID+" = ? AND s."+table.signature.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac2, new String[]{motID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return result;
			}
			
			String sql_new2 = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.news_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new2, new String[]{motID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0){
					int result_int = new Integer(""+(((Integer)result).longValue()+((Integer)orgs.get(0).get(0)).longValue()));
					if(result_int>0) result = new Boolean(true); else result = new Boolean(false);
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
			
		case TABLE_COL_NEWS: // unread news?
			int DAYS_OLD = 10;
			String sql_news = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.news_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_news, new String[]{motID,Util.getGeneralizedDate(DAYS_OLD)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
			*/
		//default:
		//}
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

	//@Override
	//public void removeTableModelListener(TableModelListener arg0) {}

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
		//this.fireTableDataChanged();
		for(int k=0;k<_news.length;k++){
			Object i = _news[k];
			if(DEBUG) System.out.println("NewsModel:setCurrent: k="+k+" row_just_ID="+i);
			Long id = Util.Lval(i);
			if((id != null) && (id.longValue()==just_id)) {
					/*
					try {
						//long constituent_ID = 
						if(DEBUG) System.out.println("NewsModel:setCurrent: will set current just");
						//Identity.setCurrentOrg(mot_id);
						
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
					*/
					for(NewsTable o: tables){
						int tk = o.convertRowIndexToView(k);
						o.setRowSelectionAllowed(true);
						ListSelectionModel selectionModel = o.getSelectionModel();
						selectionModel.setSelectionInterval(tk, tk);
						//o.requestFocus();
						o.scrollRectToVisible(o.getCellRect(tk, 0, true));
						//o.setEditingRow(k);
						//o.setRowSelectionInterval(k, k);
						o.fireListener(k, 0);
					}
					break;
			}
		}
		if(DEBUG) System.out.println("NewsModel:setCurrent: Done");
	}
	private static String sql_news = 
			"SELECT n."+table.news.news_ID+",n."+table.news.creation_date+",n."
			+table.news.global_news_ID+",n."+table.news.constituent_ID+
			","+table.news.blocked+
			",n."+table.news.broadcasted+
			",n."+table.news.requested
			//", count(*) AS cnt "
			+" FROM "+table.news.TNAME+" AS n "
			+" WHERE n."+table.news.news_ID+" IS NOT NULL ";
			//" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.news_ID+"=n."+table.news.news_ID+")"+
			//" GROUP BY n."+table.news.news_ID+
			//" ORDER BY cnt DESC"+
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("\nwidgets.news.NewsModel: update table= "+_table+": info= "+info);
		if(DEBUG) System.out.println("\nwidgets.news.NewsModel: crt_motion_id = "+this.crt_motionID);
		Object old_sel[] = new Object[tables.size()];
		for(int i=0; i<old_sel.length; i++){
			int sel = tables.get(i).getSelectedRow();
			if((sel >= 0) && (sel < _news.length)) old_sel[i] = _news[sel];
		}
		String sql = sql_news;
		ArrayList<String> params = new ArrayList<String>(); 
		if(this.crt_motionID != null) {
			sql += " AND n."+table.news.motion_ID + "=? ";
			params.add(this.crt_motionID);
		}
		if(this.crt_orgID != null) {
			sql += " AND n."+table.news.organization_ID + "=? ";
			params.add(this.crt_orgID);
		}
		try {
			ArrayList<ArrayList<Object>> news_data;
			news_data = db.select(sql, params.toArray(new String[0]), DEBUG);
			_news = new Object[news_data.size()];
			//_meth = new Object[orgs.size()];
			_hash = new Object[news_data.size()];
			_crea = new Object[news_data.size()];
			_crea_date = new Object[news_data.size()];
			//_votes = new Object[news_data.size()];
			_gid = new boolean[news_data.size()]; // has creatoe
			_blo = new boolean[news_data.size()];
			_bro = new boolean[news_data.size()];
			_req = new boolean[news_data.size()];
			for(int k=0; k<_news.length; k++) {
				_news[k] = news_data.get(k).get(0);
				_crea_date[k] = news_data.get(k).get(1);
				//_meth[k] = orgs.get(k).get(1);
				_hash[k] = news_data.get(k).get(2);
				_crea[k] = news_data.get(k).get(3);
				_gid[k] = (news_data.get(k).get(3) != null);
				_blo[k] = "1".equals(news_data.get(k).get(4));
				_bro[k] = "1".equals(news_data.get(k).get(5));
				_req[k] = "1".equals(news_data.get(k).get(6));
				//_votes[k] = news_data.get(k).get(7);
			}
			if(DEBUG) System.out.println("widgets.org.News: A total of: "+_news.length);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		for(int k=0; k<old_sel.length; k++){
			NewsTable i = tables.get(k);
			//int row = i.getSelectedRow();
			int row = findRow(old_sel[k]);
			if(DEBUG) System.out.println("widgets.org.News: selected row: "+row);
			//i.revalidate();
			this.fireTableDataChanged();
			if((row >= 0)&&(row<_news.length)) i.setRowSelectionInterval(row, row);
			i.fireListener(row, NewsTable.A_NON_FORCE_COL);
		}		
		if(DEBUG) System.out.println("widgets.org.News: Done");
	}

	private int findRow(Object id) {
		if(id==null) return -1;
		for(int k=0; k < _news.length; k++) if(id.equals(_news[k])) return k;
		return -1;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		if(col == TABLE_COL_NAME)
			set_my_data(table.my_news_data.name, Util.getString(value), row);
			
		if(col == TABLE_COL_CREATOR)
			set_my_data(table.my_news_data.creator, Util.getString(value), row);
			/*
		case TABLE_COL_CATEGORY:
			set_my_data(table.my_news_data.category, Util.getString(value), row);
			break;
			*/
		//}
		fireTableCellUpdated(row, col);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _news.length) return;
		if("".equals(value)) value = null;
		if(DEBUG)System.out.println("Set value =\""+value+"\"");
		String news_ID = Util.getString(_news[row]);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_news_data.TNAME+" WHERE "+table.my_news_data.news_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{news_ID});
			if(orgs.size()>0){
				db.update(table.my_news_data.TNAME, new String[]{field_name},
						new String[]{table.my_news_data.news_ID}, new String[]{value, news_ID});
			}else{
				if(value==null) return;
				db.insert(table.my_news_data.TNAME,
						new String[]{field_name,table.my_news_data.news_ID},
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
		/*
		String sql =
			"SELECT p."+table.constituent.name+
			" FROM "+table.constituent.TNAME +" AS p JOIN "+table.key.TNAME+" AS k "+
			" ON ("+table.constituent.global_constituent_ID_hash+"=k."+table.key.ID_hash+") " +
					" WHERE "+table.constituent.constituent_ID +"=?;";
		String cID=Util.getString(_crea[row]);
		
		if(cID == null) return true; // Unknown creator? probably just not set => editable
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql, new String[]{cID});
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		*/
		D_Constituent c = D_Constituent.getConstByLID(Util.getString(_crea[row]), true, false);
		if (!c.isExternal() && (c.getSK() != null))
			return true; // I have the key => editable
		return false; // I do not have the key => not editable;
//		if(a.size()>0) return true; // I have the key => editable
//		return false; // I do not have the key => not editable;
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("News:isNotReady: row="+row);
		//Util.printCallPath("Orgs:isNotReady: signals test");
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
			Application.db.update(table.news.TNAME,
					new String[]{table.news.blocked},
					new String[]{table.news.news_ID},
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
			Application.db.update(table.news.TNAME,
					new String[]{table.news.broadcasted},
					new String[]{table.news.news_ID},
					new String[]{val?"1":"0", newsID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}
	public static void setRequested(String newsID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setRequested: set="+val);
		try {
			Application.db.update(table.news.TNAME,
					new String[]{table.news.requested},
					new String[]{table.news.news_ID},
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
			this._news= new Object[0]; // do not remember current selections
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
		return  this.crt_orgID;//Application.constituents.tree.getModel().getOrganizationID();
	}
	public String getMotionID() {
		return this.crt_motionID;
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		// TODO Auto-generated method stub
		
	}
}

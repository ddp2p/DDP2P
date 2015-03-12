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
package widgets.justifications;

import static util.Util._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import util.P2PDDSQLException;

import config.Application;
import config.Identity;
import data.D_Document_Title;
import data.D_Motion;

import table.my_justification_data;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.motions.MotionsListener;
import widgets.org.OrgListener;

@SuppressWarnings("serial")
public class JustificationsModel extends AbstractTableModel implements TableModel, DBListener, MotionsListener {
	public  int TABLE_COL_NAME = -2;
	public  int TABLE_COL_CREATOR = -2; // certified by trusted?
	public  int TABLE_COL_VOTERS_NB = -2;
	//public static final int TABLE_COL_VOTES = 0;
	public  int TABLE_COL_CREATION_DATE = -2; // any activity in the last x days?
	public  int TABLE_COL_ARRIVAL_DATE = -2; // any activity in the last x days?
	public  int TABLE_COL_PREFERENCES_DATE = -2; // any activity in the last x days?
	public  int TABLE_COL_ACTIVITY = -4; // number of votes + news
	public  int TABLE_COL_RECENT = -5; // any activity in the last x days?
	public  int TABLE_COL_NEWS = -6; // unread news?
	public  int TABLE_COL_CATEGORY = -7; // certified by trusted?
	//public static final int TABLE_COL_PLUGINS = -8;
	int RECENT_DAYS_OLD = 10;
	
	public boolean show_name = true;
	public boolean show_creator = true;
	public boolean show_voters = true;
	public boolean show_creation_date = true;
	public boolean show_arrival_date = true;
	public boolean show_preferences_date = false;
	public boolean show_activity = false;
	public boolean show_recent = false;
	public boolean show_news = false;
	public boolean show_category = false;
	
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DBInterface db;
	Object _justifications[]=new Object[0];
	//Object _meth[]=new Object[0];
	Object _hash[]=new Object[0];
	Object _crea[]=new Object[0];
	Object _crea_date[]=new Object[0];
	Object _arrival_date[]=new Object[0];
	Object _preferences_date[]=new Object[0];
	Object _votes[]=new Object[0];
	boolean[] _gid=new boolean[0];
	boolean[] _blo=new boolean[0]; // block
	boolean[] _req=new boolean[0]; // request
	boolean[] _bro=new boolean[0]; // broadcast
	String columnNames[] = getCrtColumns();
	Hashtable<String, Integer> rowByID =  new Hashtable<String, Integer>();
	
	D_Motion crt_motion;
	private String[] getCrtColumns() {
		int crt = 0;
		ArrayList<String> cols = new ArrayList<String>();
		if(show_name){ cols.add(_("Title")); TABLE_COL_NAME = crt++;}
		if(show_creator){ cols.add(_("Initiator")); TABLE_COL_CREATOR = crt++;}
		if(show_voters){ cols.add(_("Voters")); TABLE_COL_VOTERS_NB = crt++;}
		if(show_creation_date){ cols.add(_("Date")); TABLE_COL_CREATION_DATE = crt++;}
		if(show_arrival_date){ cols.add(_("Arrival")); TABLE_COL_ARRIVAL_DATE = crt++;}
		if(show_preferences_date){ cols.add(_("Preference")); TABLE_COL_PREFERENCES_DATE = crt++;}
		if(show_activity){ cols.add(_("Activity")); TABLE_COL_ACTIVITY = crt++;}
		if(show_recent){ cols.add(_("Hot")); TABLE_COL_RECENT = crt++;}
		if(show_news){ cols.add(_("News")); TABLE_COL_NEWS = crt++;}
		if(show_category){ cols.add(_("Category")); TABLE_COL_CATEGORY = crt++;}
		return cols.toArray(new String[0]);
	}
	//String[] columnToolTips = {null,null,_("A name you provide")};
	public int columnToolTipsCount() {
		return this.getColumnCount();
	}
	public String columnToolTipsEntry(int realIndex) {
		if(realIndex == TABLE_COL_NAME) return _("Short title of the justification!");
		if(realIndex == TABLE_COL_CREATOR) return _("Initiator of this version of the justification!");
		if(realIndex == TABLE_COL_VOTERS_NB) return _("Number of constituents refering this justification with the first choice!");
		if(realIndex == TABLE_COL_CREATION_DATE) return _("Creation Date!");
		if(realIndex == TABLE_COL_ARRIVAL_DATE) return _("Arrival Date!");
		if(realIndex == TABLE_COL_PREFERENCES_DATE) return _("Preferences Date!");
		if(realIndex == TABLE_COL_NEWS) return _("Number of News related to this justification");
		if(realIndex == TABLE_COL_RECENT) return _("This justification is newer than a number of days:")+" "+RECENT_DAYS_OLD;
		if(realIndex == TABLE_COL_CATEGORY) return _("A category name for classifying the justification!");
		if(realIndex == TABLE_COL_ACTIVITY) return _("Total number of constituents plus news related to this justification!");
		return null;
	}
	
	ArrayList<Justifications> tables= new ArrayList<Justifications>();
	private String crt_motionID;
	public String crt_choice=null;
	public String crt_answered=null;

	public JustificationsModel(DBInterface _db) {
		db = _db;
		connectWidget();
		update(null, null);
	}
	public void connectWidget() {
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.justification.TNAME,table.my_justification_data.TNAME,table.signature.TNAME)), null);
	}
	public void disconnectWidget() {
		db.delListener(this);
	}
	public void setCrtChoice(String choice){
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtChoice: choice="+choice);
		crt_choice=choice;
		update(null, null);
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtChoice: Done");
	}
	public void setCrtAnswered(String answered){
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtAnswered: answered="+answered);
		crt_answered = answered;
		update(null, null);
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtAnswered: Done");
	}
	public void setCrtMotion(String motionID, D_Motion crt_motion){
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtMotion: mID="+motionID);
		crt_motionID=motionID;
		if((crt_motionID != null)&&(crt_motion == null)){ // never expected to happen
			long cm = new Integer(crt_motionID).longValue();
			if(cm>0)
				try {
					crt_motion = new D_Motion(cm);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		update(null, null);
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtMotion: Done");
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
		if(DEBUG) System.out.println("\n************\nJustificationsModel:isServing: row="+row);
		return isBroadcasted(row);
	}
	public boolean toggleServing(int row) {
		if(DEBUG) System.out.println("\n************\nJustificationsModel:Model:toggleServing: row="+row);
		String justification_ID = Util.getString(this._justifications[row]);
		boolean result = toggleServing(justification_ID, true, false);
		if(DEBUG) System.out.println("JustificationsModel:Model:toggleServing: result="+result+"\n************\n");
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
		return _justifications.length;
	}
	@Override
	public Object getValueAt(int row, int col) {
		Object result = null;
		String motID = Util.getString(this._justifications[row]);
		if (col == TABLE_COL_CREATION_DATE) {
			//if(!this.show_creation_date) break;
			if((row>=0) && (row<this._crea_date.length))result = this._crea_date[row];
			if(DEBUG) System.out.println("JustifModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_ARRIVAL_DATE) {
			//if(!this.show_arrival_date) break;
			if((row>=0) && (row<this._arrival_date.length))result = this._arrival_date[row];
			if(DEBUG) System.out.println("JustifModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_PREFERENCES_DATE) {
			//if(!this.show_preferences_date) break;
			if((row>=0) && (row<this._preferences_date.length))result = this._preferences_date[row];
			if(DEBUG) System.out.println("JustifModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_NAME) {
			//if(!this.show_name) break;
			String sql =
				"SELECT o."+table.justification.justification_title + 
				", m."+table.my_justification_data.name +
				", o."+table.justification.justification_title_format +
					" FROM "+table.justification.TNAME+" AS o" +
					" LEFT JOIN "+table.my_justification_data.TNAME+" AS m " +
					" ON (o."+table.justification.justification_ID+" = m."+table.my_justification_data.justification_ID+")" +
					" WHERE o."+table.justification.justification_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql, new String[]{motID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("Justifications:Got my="+result);
					}
					else{
						D_Document_Title dt = new D_Document_Title();
						dt.title_document.setFormatString(Util.getString(orgs.get(0).get(2)));
						dt.title_document.setDocumentString(Util.getString(orgs.get(0).get(0)));
						result = dt;
						if(DEBUG)System.out.println("Justifications:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if (col == TABLE_COL_CREATOR) {
			// if(!this.show_creator) break;
			String sql_cr = "SELECT o."+table.justification.constituent_ID+", m."+table.my_justification_data.creator+", c."+table.constituent.name+
			" FROM "+table.justification.TNAME+" AS o " +
			" LEFT JOIN "+table.my_justification_data.TNAME+" AS m " + " ON(o."+table.justification.justification_ID+"=m."+table.my_justification_data.justification_ID+")"+
			" LEFT JOIN "+table.constituent.TNAME+" AS c " +" ON(o."+table.justification.constituent_ID+"=c."+table.constituent.constituent_ID+")"+
			" WHERE o."+table.justification.justification_ID+" = ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cr, new String[]{motID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = Util.getString(orgs.get(0).get(1));
						if(DEBUG)System.out.println("Justifications:Got my="+result);
					}
					else{
						result = Util.getString(orgs.get(0).get(2));
						if(DEBUG)System.out.println("Justifications:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		/*
		if (col == TABLE_COL_CATEGORY) {
			String sql_cat = "SELECT o."+table.justification.category + ", m."+table.my_justification_data.category+
					" FROM "+table.justification.TNAME+" AS o" +
					" LEFT JOIN "+table.my_justification_data.TNAME+" AS m " +
							" ON (o."+table.justification.justification_ID+" = m."+table.my_justification_data.justification_ID+")" +
					" WHERE o."+table.justification.justification_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cat, new String[]{motID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("Justifications:Got my="+result);
					}
					else{
						result = orgs.get(0).get(0);
						if(DEBUG)System.out.println("Justifications:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		*/
		if (col == TABLE_COL_VOTERS_NB) {
			if((row>=0) && (row<this._votes.length)){
				result = this._votes[row];
				return result;
			}
			
			String sql_co = "SELECT count(*) FROM "+table.signature.TNAME+
			" WHERE "+table.signature.justification_ID+" = ? AND "+table.signature.choice+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_co, new String[]{motID, "y"}, _DEBUG);
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
			
		if (col == TABLE_COL_ACTIVITY) { // number of all votes + news
			String sql_ac = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE "+table.signature.justification_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac, new String[]{motID});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return result;
			}
			
			String sql_new = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE "+table.news.justification_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new, new String[]{motID});
				if(orgs.size()>0)
					result = new Integer(""+Util.lval(result,0)+Util.lval(orgs.get(0).get(0),0));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
			
		if (col == TABLE_COL_RECENT) { // any activity in the last x days?
			String sql_ac2 = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE s."+table.signature.justification_ID+" = ? AND s."+table.signature.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac2, new String[]{motID,Util.getGeneralizedDate(RECENT_DAYS_OLD)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return result;
			}
			
			String sql_new2 = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.justification_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new2, new String[]{motID,Util.getGeneralizedDate(RECENT_DAYS_OLD)});
				if(orgs.size()>0){
					int result_int = new Integer(""+Util.lval(result,0)+Util.lval(orgs.get(0).get(0),0));
					if(result_int>0) result = new Boolean(true); else result = new Boolean(false);
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
			/*
		case TABLE_COL_NEWS: // unread news?
			int DAYS_OLD = 10;
			String sql_news = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.justification_ID+" = ? AND n."+table.news.arrival_date+">?;";
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

	public void setTable(Justifications justifications) {
		tables.add(justifications);
	}

	//@Override
	//public void removeTableModelListener(TableModelListener arg0) {}
	public int getRowByID(long just_id){
		for(int k=0;k<_justifications.length;k++){
			Object i = _justifications[k];
			if(DEBUG) System.out.println("JustificationsModel:setCurrent: k="+k+" row_just_ID="+i);
			Long id = Util.Lval(i);
			if((id!=null) && (id.longValue()==just_id)) {
				return k;
			}
		}
		return -1;
	}
					/*
		//this.fireTableDataChanged();
					try {
						//long constituent_ID = 
						if(DEBUG) System.out.println("JustificationsModel:setCurrent: will set current just");
						//Identity.setCurrentOrg(mot_id);
						
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
					*/
	public void setCurrentJust(long just_id) {
		if(DEBUG) System.out.println("JustificationsModel:setCurrent: choice="+crt_choice+"  id="+just_id);
		if(just_id<0) {
			for(Justifications o: tables){
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(-1, -1);
				o.fireListener(-1, 0, false);
			}	
			if(DEBUG) System.out.println("JustificationsModel:setCurrent: choice="+crt_choice+"  Done -1");
			return;
		}
		int k = this.getRowByID(just_id);
		if(k>=0) {
			for(Justifications o: tables){
				int tk = o.convertRowIndexToView(k);
				o.setRowSelectionAllowed(true);
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(tk, tk);
				//o.requestFocus();
				o.scrollRectToVisible(o.getCellRect(tk, 0, true));
				//o.setEditingRow(k);
				//o.setRowSelectionInterval(k, k);
				o.fireListener(k, 0, false);
				if(DEBUG) System.out.println("JustificationsModel:setCurentJust: choice="+crt_choice+" fireListener: k="+k);
			}
		}
		if(DEBUG) System.out.println("JustificationsModel:setCurrent:choice="+crt_choice+"  Done");
	}
	private static String sql_no_choice_no_answer = 
		"SELECT j."+table.justification.justification_ID+",j."+table.justification.creation_date+
		",j."+table.justification.global_justification_ID+",j."+table.justification.constituent_ID+
		","+table.justification.blocked+
		",j."+table.justification.broadcasted+
		",j."+table.justification.requested+
		", count(*) AS cnt "+
		",j."+table.justification.arrival_date+",j."+table.justification.preferences_date+
		", max(s."+table.signature.justification_ID+") AS maxsignID "
		+" FROM "+table.justification.TNAME+" AS j "
		+" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE j."+table.justification.motion_ID + "=?"+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	private static String sql_choice_no_answer = 
		"SELECT j."+table.justification.justification_ID+",j."+table.justification.creation_date+",j."
		+table.justification.global_justification_ID+",j."+table.justification.constituent_ID+
		","+table.justification.blocked+
		",j."+table.justification.broadcasted+
		",j."+table.justification.requested+
		", count(*) AS cnt "
		+",j."+table.justification.arrival_date+",j."+table.justification.preferences_date
		+" FROM "+table.justification.TNAME+" AS j "
		+" JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE s."+table.signature.choice+"=? AND j."+table.justification.motion_ID + "=?"+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	private static String sql_no_choice_answer = 
		"SELECT j."+table.justification.justification_ID+",j."+table.justification.creation_date+",j."
		+table.justification.global_justification_ID+",j."+table.justification.constituent_ID+
		","+table.justification.blocked+
		",j."+table.justification.broadcasted+
		",j."+table.justification.requested+
		", count(*) AS cnt "
		+",j."+table.justification.arrival_date+",j."+table.justification.preferences_date
		+" FROM "+table.justification.TNAME+" AS j "
		+" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE j."+table.justification.motion_ID + "=? AND j."+table.justification.answerTo_ID+"=? "+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	private static String sql_choice_answer = 
		"SELECT j."+table.justification.justification_ID+",j."+table.justification.creation_date+",j."
		+table.justification.global_justification_ID+",j."+table.justification.constituent_ID+
		","+table.justification.blocked+
		",j."+table.justification.broadcasted+
		",j."+table.justification.requested+
		", count(*) AS cnt "
		+",j."+table.justification.arrival_date+",j."+table.justification.preferences_date
		+" FROM "+table.justification.TNAME+" AS j "
		+" JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE s."+table.signature.choice+"=? AND j."+table.justification.motion_ID + "=? AND j."+table.justification.answerTo_ID+"=? "+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	public String getJustificationID(int row) {
		if((row<0) || (row>=this._justifications.length)) return null;
		return Util.getString(this._justifications[row]);
	}
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		final int SELECT_ID = 0;
		final int SELECT_CREATION_DATE = 1;
		final int SELECT_GID = 2;
		final int SELECT_CONST_ID = 3;
		final int SELECT_BLOCKED = 4;
		final int SELECT_BROADCAST = 5;
		final int SELECT_REQUESTED = 6;
		final int SELECT_CNT = 7;
		final int SELECT_ARRIVAL_DATE = 8;
		final int SELECT_PREFERENCES_DATE = 9;
		final int SELECT_MAX_JUST_ID_FOR_SIGN = 10;// only for: sql_no_choice_no_answer
		//boolean DEBUG=true;
		if(DEBUG) System.out.println("\nwidgets.justifications.JustificationsModel: update table= "+_table+": info= "+info);
		if(crt_motionID==null){
			if(DEBUG) System.out.println("\nwidgets.justifications.JustificationsModel: null crt_motion_id");
			return;
		}
		Object old_sel[] = new Object[tables.size()];
		for(int i=0; i<old_sel.length; i++){
			int sel = tables.get(i).getSelectedRow();
			if((sel >= 0) && (sel < _justifications.length)) old_sel[i] = _justifications[sel];
		}
		try {
			ArrayList<ArrayList<Object>> justi=null;
			if((crt_choice == null)&&(crt_answered==null)) justi = db.select(sql_no_choice_no_answer, new String[]{crt_motionID}, DEBUG);
			if((crt_choice != null)&&(crt_answered==null)) justi = db.select(sql_choice_no_answer, new String[]{crt_choice, crt_motionID}, DEBUG);
			if((crt_choice == null)&&(crt_answered!=null)) justi = db.select(sql_no_choice_answer, new String[]{crt_motionID, crt_answered}, DEBUG);
			if((crt_choice != null)&&(crt_answered!=null)) justi = db.select(sql_choice_answer, new String[]{crt_choice, crt_motionID, crt_answered}, DEBUG);
			_justifications = new Object[justi.size()];
			//_meth = new Object[orgs.size()];
			_hash = new Object[justi.size()];
			_crea = new Object[justi.size()];
			_crea_date = new Object[justi.size()];
			_arrival_date = new Object[justi.size()];
			_preferences_date = new Object[justi.size()];
			_votes = new Object[justi.size()];
			_gid = new boolean[justi.size()];
			_blo = new boolean[justi.size()];
			_bro = new boolean[justi.size()];
			_req = new boolean[justi.size()];
			rowByID = new Hashtable<String, Integer>();
			for(int k=0; k<_justifications.length; k++) {
				ArrayList<Object> j = justi.get(k);
				_justifications[k] = j.get(SELECT_ID);
				rowByID.put(Util.getString(_justifications[k]), new Integer(k));
				//_meth[k] = orgs.get(k).get(SELECT_CREATION_DATE);
				_hash[k] = j.get(SELECT_GID);
				_crea[k] = j.get(SELECT_CONST_ID);
				_gid[k] = (j.get(SELECT_CONST_ID) != null);
				_blo[k] = Util.stringInt2bool(j.get(SELECT_BLOCKED), false);
				_bro[k] = Util.stringInt2bool(j.get(SELECT_BROADCAST), false);
				_req[k] = Util.stringInt2bool(j.get(SELECT_REQUESTED), false);
				_votes[k] = j.get(SELECT_CNT);
				if(Util.stringInt2bool(_votes[k], false))
					if((j.size()>SELECT_MAX_JUST_ID_FOR_SIGN)&&(j.get(SELECT_MAX_JUST_ID_FOR_SIGN)==null)) _votes[k]="0"; 
				_crea_date[k] = j.get(SELECT_CREATION_DATE);
				_arrival_date[k] = j.get(SELECT_ARRIVAL_DATE);
				_preferences_date[k] = j.get(SELECT_PREFERENCES_DATE);
			}
			if(DEBUG) System.out.println("widgets.org.Justifications: A total of: "+_justifications.length);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		for(int k=0; k<old_sel.length; k++){
			Justifications i = tables.get(k);
			//int row = i.getSelectedRow();
			int row = findRow(old_sel[k]);
			if(DEBUG) System.out.println("widgets.org.Justifications: selected row: "+row);
			//i.revalidate();
			this.fireTableDataChanged();
			if((row >= 0)&&(row<_justifications.length)) i.setRowSelectionInterval(row, row);
			i.fireListener(row, Justifications.A_NON_FORCE_COL, true); // no need to tell listeners of an update (except if telling the cause)
			//TODO the next is probably slowing down the system. May be run only on request
			i.initColumnSizes();
		}		
		if(DEBUG) System.out.println("widgets.org.Justifications: Done");
	}

	private int findRow(Object id) {
		if(id==null) return -1;
		for(int k=0; k < _justifications.length; k++) if(id.equals(_justifications[k])) return k;
		return -1;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		if(col == TABLE_COL_NAME)
			set_my_data(table.my_justification_data.name, Util.getString(value), row);
			
		if(col == TABLE_COL_CREATOR)
			set_my_data(table.my_justification_data.creator, Util.getString(value), row);
			/*
		case TABLE_COL_CATEGORY:
			set_my_data(table.my_justification_data.category, Util.getString(value), row);
			break;
			*/
		//}
		fireTableCellUpdated(row, col);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _justifications.length) return;
		if("".equals(value)) value = null;
		if(DEBUG)System.out.println("Set value =\""+value+"\"");
		String justification_ID = Util.getString(_justifications[row]);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_justification_data.TNAME+" WHERE "+table.my_justification_data.justification_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{justification_ID});
			if(orgs.size()>0){
				db.update(table.my_justification_data.TNAME, new String[]{field_name},
						new String[]{table.my_justification_data.justification_ID}, new String[]{value, justification_ID});
			}else{
				if(value==null) return;
				db.insert(table.my_justification_data.TNAME,
						new String[]{field_name,table.my_justification_data.justification_ID},
						new String[]{value, justification_ID});
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Is the creator of this justification, myself?
	 * @param row
	 * @return
	 */
	public boolean isMine(int row) {
		if(row >= _justifications.length) return false;
		
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
		if(a.size()>0) return true; // I have the key => editable
		return false; // I do not have the key => not editable;
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("Justifications:isNotReady: row="+row);
		//Util.printCallPath("Orgs:isNotReady: signals test");
		if(row >= _justifications.length) {
			if(DEBUG) System.out.println("Justifications:isNotReady: row>"+_justifications.length);
			return false;
		}
		if(!_gid[row]){
			if(DEBUG) System.out.println("Justifications:isNotReady: gid false");
			return true;
		}
		String cID=Util.getString(_crea[row]);
		if(cID == null){
			if(DEBUG) System.out.println("Justifications:isNotReady: cID null");
			return true;
		}
		if(DEBUG) System.out.println("Justifications:isNotReady: exit false");
		return false;
	}
	public static void setBlocking(String justificationID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		try {
			Application.db.update(table.justification.TNAME,
					new String[]{table.justification.blocked},
					new String[]{table.justification.justification_ID},
					new String[]{val?"1":"0", justificationID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * change org.broadcasted Better change with toggleServing which sets also peer.served_orgs
	 * @param orgID
	 * @param val
	 */
	static void setBroadcasting(String justificationID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+justificationID);
		try {
			Application.db.update(table.justification.TNAME,
					new String[]{table.justification.broadcasted},
					new String[]{table.justification.justification_ID},
					new String[]{val?"1":"0", justificationID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}
	public static void setRequested(String justificationID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setRequested: set="+val);
		try {
			Application.db.update(table.justification.TNAME,
					new String[]{table.justification.requested},
					new String[]{table.justification.justification_ID},
					new String[]{val?"1":"0", justificationID}, DEBUG);
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
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("\n************\nJustificationsModel:motUpdate:choice="+crt_choice+":motID="+motionID);
		if ((crt_motionID==null) || (!crt_motionID.equals(motionID))){
			if(DEBUG) System.out.println("JustificationsModel:motUpdate: new justs");
			this._justifications= new Object[0]; // do not remember current selections
			this.setCrtMotion(motionID, d_motion);
		}
		if(DEBUG) System.out.println("\n************\nJustificationsModel:motUpdate:choice="+crt_choice+": Done");
	}
	public long getConstituentIDMyself() {
		return  Application.constituents.tree.getModel().getConstituentIDMyself();

	}
	public String getConstituentGIDMyself() {
		return  Application.constituents.tree.getModel().getConstituentGIDMyself();
	}
	public String getOrganizationID() {
		if(crt_motion == null) return null;
		return  crt_motion.organization_ID;
	}
	public String getMotionID() {
		return this.crt_motionID;
	}
	public int getRow(String justID) {
		Integer row = rowByID.get(justID);
		if(row==null) return -1;
		return row.intValue();
	}
}

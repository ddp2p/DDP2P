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
package widgets.motions;

import static util.Util._;

import hds.ClientSync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import util.P2PDDSQLException;

import config.Application;
import config.Identity;
import data.D_Constituent;
import data.D_Document;
import data.D_Document_Title;
import data.D_Justification;
import data.D_Motion;
import data.D_Organization;
import data.D_Vote;

import streaming.RequestData;
import streaming.WB_Messages;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.org.OrgListener;

@SuppressWarnings("serial")
public class MotionsModel extends AbstractTableModel implements TableModel, DBListener, OrgListener {
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_CREATOR = 1; // certified by trusted?
	public static final int TABLE_COL_CATEGORY = 2; // certified by trusted?
	public static final int TABLE_COL_VOTERS_NB = 3;
	public static final int TABLE_COL_ACTIVITY = 4; // number of votes + news
	public static final int TABLE_COL_RECENT = 5; // any activity in the last x days?
	public static final int TABLE_COL_NEWS = 6; // unread news?
	public static final int TABLE_COL_CREATION_DATE = 7; // unread news?
	//public static final int TABLE_COL_PLUGINS = 7;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DBInterface db;
	Object _motions[]=new Object[0];
	//Object _meth[]=new Object[0];
	Object _hash[]=new Object[0];
	Object _crea[]=new Object[0];
	Object _crea_date[]=new Object[0];
	Object _votes[]=new Object[0];
	boolean[] _gid=new boolean[0];
	boolean[] _blo=new boolean[0]; // block
	boolean[] _req=new boolean[0]; // request
	boolean[] _bro=new boolean[0]; // broadcast
	
	String crt_enhanced=null;
	
	String columnNames[]={
			_("Name"),_("Initiator"),_("Category"),
			_("Voters"),_("Activity"),
			_("Hot"),_("News"),_("Date")
			};
	ArrayList<Motions> tables= new ArrayList<Motions>();
	private String crt_orgID;
	private D_Constituent constituent;
	private D_Organization organization;
	public void setCrtEnhanced(String enhanced) {
		crt_enhanced = enhanced;
		this.update(null, null);
	}

	public MotionsModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.motion.TNAME,table.signature.TNAME,table.my_motion_data.TNAME,table.constituent.TNAME)), null);
		update(null, null);
	}
	public void setCrtOrg(String orgID){
		crt_orgID=orgID;
		update(null, null);
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
		if(DEBUG) System.out.println("\n************\nMotionsModel:isServing: row="+row);
		return isBroadcasted(row);
	}
	public boolean toggleServing(int row) {
		if(DEBUG) System.out.println("\n************\nMotionsModel:Model:toggleServing: row="+row);
		String motion_ID = Util.getString(this._motions[row]);
		boolean result = toggleServing(motion_ID, true, false);
		if(DEBUG) System.out.println("MotionsModel:Model:toggleServing: result="+result+"\n************\n");
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
		if(col == this.TABLE_COL_NEWS) return Integer.class;
		
		return String.class;
	}
	@Override
	public int getRowCount() {
		return _motions.length;
	}
	@Override
	public Object getValueAt(int row, int col) {
		Object result = null;
		String motID = Util.getString(this._motions[row]);
		switch(col) {
		case TABLE_COL_CREATION_DATE:
			if((row>=0) && (row<this._crea_date.length))result = this._crea_date[row];
			break;
		case TABLE_COL_NAME:
			String sql = "SELECT o."+table.motion.motion_title + ", m."+table.my_motion_data.name+", o."+table.motion.format_title_type+
					" FROM "+table.motion.TNAME+" AS o" +
					" LEFT JOIN "+table.my_motion_data.TNAME+" AS m " +
							" ON (o."+table.motion.motion_ID+" = m."+table.my_motion_data.motion_ID+")" +
					" WHERE o."+table.motion.motion_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql, new String[]{motID});
				if(orgs.size()>0){
					ArrayList<Object> o = orgs.get(0);
					if(o.get(1)!=null){
						String s = Util.getString(o.get(1));
						if(s!=null){
							s=s.trim();
							if(s.startsWith(D_Document_Title.TD)){
								D_Document_Title dt = new D_Document_Title();
								dt.decode(s);
								result = dt;
							}else{
								result = s;
							}
						}
						if(DEBUG)System.out.println("Motions:Got my="+result);
					}
					else{
						D_Document_Title dt = new D_Document_Title();
						dt.title_document.setFormatString(Util.getString(o.get(2)));
						dt.title_document.setDocumentString(Util.getString(o.get(0)));
						result = dt;
						if(DEBUG)System.out.println("Motions:Got my="+result);
					}
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CREATOR:
			result = _("Empty");
			String sql_cr = "SELECT o."+table.motion.constituent_ID+", m."+table.my_motion_data.creator+", c."+table.constituent.name+
			" FROM "+table.motion.TNAME+" AS o " +
			" LEFT JOIN "+table.my_motion_data.TNAME+" AS m " + " ON(o."+table.motion.motion_ID+"=m."+table.my_motion_data.motion_ID+")"+
			" LEFT JOIN "+table.constituent.TNAME+" AS c " +" ON(o."+table.motion.constituent_ID+"=c."+table.constituent.constituent_ID+")"+
			" WHERE o."+table.motion.motion_ID+" = ? LIMIT 1;";
			//" WHERE m."+table.motion.constituent_ID+">0 o."+table.motion.motion_ID+" = ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cr, new String[]{motID}, DEBUG);
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = Util.getString(orgs.get(0).get(1));
						if(DEBUG)System.out.println("Motions:Got my="+result);
					}
					else{
						result = Util.getString(orgs.get(0).get(2));
						if(DEBUG)System.out.println("Motions:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CATEGORY:
			String sql_cat = "SELECT o."+table.motion.category + ", m."+table.my_motion_data.category+
					" FROM "+table.motion.TNAME+" AS o" +
					" LEFT JOIN "+table.my_motion_data.TNAME+" AS m " +
							" ON (o."+table.motion.motion_ID+" = m."+table.my_motion_data.motion_ID+")" +
					" WHERE o."+table.motion.motion_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cat, new String[]{motID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("Motions:Got my="+result);
					}
					else{
						result = orgs.get(0).get(0);
						if(DEBUG)System.out.println("Motions:Got my="+result);
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_VOTERS_NB:
			String sql_co = "SELECT count(*) FROM "+table.signature.TNAME+
			" WHERE "+table.signature.motion_ID+" = ? AND "+table.signature.choice+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_co, new String[]{motID, D_Vote.DEFAULT_YES_COUNTED_LABEL});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_ACTIVITY: // number of all votes + news
			String sql_ac = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE "+table.signature.motion_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac, new String[]{motID});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				break;
			}
			
			String sql_new = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE "+table.news.motion_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new, new String[]{motID});
				if(orgs.size()>0) result = new Integer(""+((Util.get_long(result))+(Util.get_long(orgs.get(0).get(0)))));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_RECENT: // any activity in the last x days?
			int DAYS_OLD2 = 10;
			String sql_ac2 = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE s."+table.signature.motion_ID+" = ? AND s."+table.signature.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac2, new String[]{motID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				break;
			}
			
			String sql_new2 = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.motion_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new2, new String[]{motID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0){
					int result_int = new Integer(""+((Util.get_long(result))+(Util.get_long(orgs.get(0).get(0)))));
					if(result_int>0) result = new Boolean(true); else result = new Boolean(false);
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_NEWS: // unread news?
			int DAYS_OLD = 10;
			String sql_news = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.motion_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_news, new String[]{motID,Util.getGeneralizedDate(DAYS_OLD)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		default:
		}
		return result;
	}
	public boolean isCellEditable(int row, int col) {
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_CREATOR:
		case TABLE_COL_CATEGORY:
			return true;
		}
		return false;
	}

	public void setTable(Motions motions) {
		tables.add(motions);
	}

	//@Override
	//public void removeTableModelListener(TableModelListener arg0) {}

	public void setCurrent(long motion_id) {
		if(DEBUG) System.out.println("MotionsModel:setCurrent: id="+motion_id);
		if(motion_id<0){
			for(Motions o: tables){
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(-1, -1);
				o.fireListener(-1, 0);
			}	
			if(DEBUG) System.out.println("MotionsModel:setCurrent: Done -1");
			return;
		}
		//this.fireTableDataChanged();
		for(int k=0;k<_motions.length;k++){
			Object i = _motions[k];
			if(DEBUG) System.out.println("MotionsModel:setCurrent: k="+k+" row_org_ID="+i);
			if(i == null) continue;
			Long id = Util.Lval(i);
			if((id != null) && (id.longValue()==motion_id)) {
					/*
					try {
						//long constituent_ID = 
						if(DEBUG) System.out.println("MotionsModel:setCurrent: will set current org");
						Identity.setCurrentOrg(motion_id);
						
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
					*/
					for(Motions o: tables){
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
		if(DEBUG) System.out.println("MotionsModel:setCurrent: Done");
		
	}
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("\nwidgets.motions.MotionsModel: update table= "+_table+": info= "+info);
		if(crt_orgID==null) return;
		String sql = 
			"SELECT "+table.motion.motion_ID+","+table.motion.creation_date+","
			+table.motion.global_motion_ID+","+table.motion.constituent_ID+
			","+table.motion.blocked+","+table.motion.broadcasted+","+table.motion.requested+
			","+table.motion.creation_date
			+" FROM "+table.motion.TNAME+
			" WHERE "+table.motion.organization_ID + "=? ";
		Object old_sel[] = new Object[tables.size()];
		for(int i=0; i<old_sel.length; i++){
			int sel = tables.get(i).getSelectedRow();
			if((sel >= 0) && (sel < _motions.length)) old_sel[i] = _motions[sel];
		}
		try {
			ArrayList<ArrayList<Object>> moti;
			if(crt_enhanced!=null){
				sql += " AND "+table.motion.enhances_ID+" = ?;";
				moti = db.select(sql, new String[]{crt_orgID, crt_enhanced});
			}
			else{
				moti = db.select(sql+";", new String[]{crt_orgID});
			}
			_motions = new Object[moti.size()];
			//_meth = new Object[orgs.size()];
			_hash = new Object[moti.size()];
			_crea = new Object[moti.size()];
			_crea_date = new Object[moti.size()];
			_gid = new boolean[moti.size()];
			_blo = new boolean[moti.size()];
			_bro = new boolean[moti.size()];
			_req = new boolean[moti.size()];
			for(int k=0; k<_motions.length; k++) {
				_motions[k] = moti.get(k).get(0);
				//_meth[k] = orgs.get(k).get(1);
				_hash[k] = moti.get(k).get(2);
				_crea[k] = moti.get(k).get(3);
				_gid[k] = (moti.get(k).get(3) != null);
				_blo[k] = "1".equals(moti.get(k).get(4));
				_bro[k] = "1".equals(moti.get(k).get(5));
				_req[k] = "1".equals(moti.get(k).get(6));
				_crea_date[k] = moti.get(k).get(7);
			}
			if(DEBUG) System.out.println("widgets.org.Motions: A total of: "+_motions.length);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		for(int k=0; k<old_sel.length; k++){
			Motions i = tables.get(k);
			//int row = i.getSelectedRow();
			int row = findTableRow(i,old_sel[k]);
			if(DEBUG) System.out.println("widgets.org.Motions: selected row: "+row);
			//i.revalidate();
			this.fireTableDataChanged();
			if((row >= 0)&&(row<_motions.length)) i.setRowSelectionInterval(row, row);
			i.fireListener(row, Motions.A_NON_FORCE_COL);
			i.initColumnSizes();
		}		
	}
	private int findTableRow(Motions i, Object id) {
		int modelRow = findModelRow(id);
		if(modelRow < 0) return modelRow;
		if(i==null) return -1;
		return i.convertRowIndexToView(modelRow);
	}
	private int findModelRow(Object id) {
		if(id==null) return -1;
		for(int k=0; k < _motions.length; k++) if(id.equals(_motions[k])) return k;
		return -1;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		switch(col) {
		case TABLE_COL_NAME:
			if(value instanceof D_Document_Title){
				D_Document_Title _value = (D_Document_Title) value;
				if(_value.title_document!=null)  {
					String format  = _value.title_document.getFormatString();
					if((format==null) || D_Document.TXT_FORMAT.equals(format))
						value = _value.title_document.getDocumentUTFString();
				}
			}
			set_my_data(table.my_motion_data.name, Util.getString(value), row);
			break;
		case TABLE_COL_CREATOR:
			set_my_data(table.my_motion_data.creator, Util.getString(value), row);
			break;
		case TABLE_COL_CATEGORY:
			set_my_data(table.my_motion_data.category, Util.getString(value), row);
			break;
		}
		fireTableCellUpdated(row, col);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _motions.length) return;
		if("".equals(value)) value = null;
		if(DEBUG)System.out.println("Set value =\""+value+"\"");
		String motion_ID = Util.getString(_motions[row]);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_motion_data.TNAME+" WHERE "+table.my_motion_data.motion_ID+"=?;";
			ArrayList<ArrayList<Object>> mots = db.select(sql,new String[]{motion_ID});
			if(mots.size()>0){
				db.update(table.my_motion_data.TNAME, new String[]{field_name},
						new String[]{table.my_motion_data.motion_ID}, new String[]{value, motion_ID});
			}else{
				if(value==null) return;
				db.insert(table.my_motion_data.TNAME,
						new String[]{field_name,table.my_motion_data.motion_ID},
						new String[]{value, motion_ID});
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public boolean isMine(int row) {
		if(row >= _motions.length) return false;
		
		String sql = "SELECT p."+table.constituent.name+" FROM "+table.constituent.TNAME +" AS p JOIN "+table.key.TNAME+" AS k"+
		" ON ("+table.constituent.global_constituent_ID_hash+"=k."+table.key.ID_hash+") WHERE "+table.constituent.constituent_ID +"=?;";
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
		if(DEBUG) System.out.println("Motions:isNotReady: row="+row);
		//Util.printCallPath("Orgs:isNotReady: signals test");
		if(row >= _motions.length) {
			if(DEBUG) System.out.println("Motions:isNotReady: row>"+_motions.length);
			return false;
		}
		if(!_gid[row]){
			if(DEBUG) System.out.println("Motions:isNotReady: gid false");
			return true;
		}
		String cID=Util.getString(_crea[row]);
		if(cID == null){
			if(DEBUG) System.out.println("Motions:isNotReady: cID null");
			return true;
		}
		if(DEBUG) System.out.println("Motions:isNotReady: exit false");
		return false;
	}
	public static void setBlocking(String motionID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		try {
			Application.db.update(table.motion.TNAME,
					new String[]{table.motion.blocked},
					new String[]{table.motion.motion_ID},
					new String[]{val?"1":"0", motionID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * change org.broadcasted Better change with toggleServing which sets also peer.served_orgs
	 * @param orgID
	 * @param val
	 */
	public static void setBroadcasting(String motionID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+motionID);
		try {
			Application.db.update(table.motion.TNAME,
					new String[]{table.motion.broadcasted},
					new String[]{table.motion.motion_ID},
					new String[]{val?"1":"0", motionID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}
	public static void setRequested(String motionID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setRequested: set="+val);
		try {
			Application.db.update(table.motion.TNAME,
					new String[]{table.motion.requested},
					new String[]{table.motion.motion_ID},
					new String[]{val?"1":"0", motionID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if ((crt_orgID==null) || (!crt_orgID.equals(orgID))){
			this._motions= new Object[0]; // do not remember current selections
			this.setCrtOrg(orgID);
		}
		this.organization = org;
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		orgUpdate(orgID, 0, org);
		return;
	}
	public long getConstituentIDMyself() {
		if(Application.constituents==null) return -1;
		if(Application.constituents.tree==null) return -1;
		if(Application.constituents.tree.getModel()==null) return -1;
		return  Application.constituents.tree.getModel().getConstituentIDMyself();

	}
	public String getConstituentGIDMyself() {
		if(Application.constituents==null) return null;
		if(Application.constituents.tree==null) return null;
		if(Application.constituents.tree.getModel()==null) return null;
		return  Application.constituents.tree.getModel().getConstituentGIDMyself();
	}
	
	public String getOrganizationID() {
		return  this.crt_orgID;//Application.constituents.tree.getModel().getOrganizationID();
	}
	
	public String getMotionID(int row) {
		return Util.getString(this._motions[row]);
	}
	
	public String getMotionGID(int row) {
		return Util.getString(this._hash[row]);
	}

	public void advertise(int row) {
		String hash = Util.getString(this._hash[row]);
		String org_hash = this.organization.global_organization_IDhash;
		ClientSync.addToPayloadFix(RequestData.MOTI, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
		ClientSync.payload.requested = new WB_Messages();
		try {
			D_Motion m;
			ClientSync.payload.requested.moti.add(m = new D_Motion(hash));
			D_Vote vote = D_Vote.getMyVoteForMotion(m.motionID);
			if(vote != null) {
				ClientSync.addToPayloadFix(RequestData.SIGN, vote.global_vote_ID, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
				ClientSync.payload.requested.sign.add(vote);
				if(vote.justification_ID!=null){
					D_Justification just = new D_Justification(vote.justification_ID);
					if(just!=null){
						ClientSync.addToPayloadFix(RequestData.JUST, just.global_justificationID, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
						ClientSync.payload.requested.just.add(just);
					}
				}
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
}

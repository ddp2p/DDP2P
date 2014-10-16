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

import static util.Util.__;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import util.P2PDDSQLException;
import config.Application;
import config.Identity;
import config.MotionsListener;
import config.OrgListener;
import data.D_Constituent;
import data.D_Document;
import data.D_Document_Title;
import data.D_Justification;
import data.D_Motion;
import data.D_News;
import data.D_Peer;
import table.my_justification_data;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.app.DDIcons;
import widgets.components.GUI_Swing;
//import widgets.motions.MotionsModel;

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
	public int TABLE_COL_PROVIDER = -2;
	public int TABLE_COL_BROADCASTED = -2;
	public int TABLE_COL_BLOCKED = -2;
	public int TABLE_COL_HIDDEN = -2;
	public int TABLE_COL_TMP = -2;
	public int TABLE_COL_GID_VALID = -2;
	public int TABLE_COL_SIGN_VALID = -2;
	//public static final int TABLE_COL_PLUGINS = -8;
	int RECENT_DAYS_OLD = 10;
	
	public boolean show_name = true;
	public boolean show_creator = true;
	public boolean show_voters = true;
	public boolean show_creation_date = false;
	public boolean show_arrival_date = true;
	public boolean show_preferences_date = true;
	public boolean show_activity = false;
	public boolean show_recent = true;
	public boolean show_news = false;
	public boolean show_category = false;
	public boolean show_provider = true;
	public boolean show_tmp = true;
	public boolean show_hidden = true;
	public boolean show_broadcasted = true;
	public boolean show_blocked = true;
	public boolean show_gid = true;
	public boolean show_sign = true;
	
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DBInterface db;
	Object _justifications[] = new Object[0];
	D_Justification _justification[] = new D_Justification[0];
	//Object _votes[] = new Object[0];
	//Object _meth[]=new Object[0];
//	Object _hash[]=new Object[0];
//	Object _crea[]=new Object[0];
//	Object _crea_date[]=new Object[0];
//	Object _arrival_date[]=new Object[0];
//	Object _preferences_date[]=new Object[0];
//	boolean[] _gid=new boolean[0];
//	boolean[] _blo=new boolean[0]; // block
//	boolean[] _req=new boolean[0]; // request
//	boolean[] _bro=new boolean[0]; // broadcast
	String columnNames[] = getCrtColumns();
	Hashtable<String, Integer> rowByID =  new Hashtable<String, Integer>();
	
	D_Motion crt_motion;
	String[] getCrtColumns() {
		int crt = 0;
		boolean d = false;//DEBUG;
		ArrayList<String> cols = new ArrayList<String>();
		if(show_name){ cols.add(__("Title")); if(d)System.out.println("n="+crt); TABLE_COL_NAME = crt++;}
		if(show_voters){ cols.add(__("Voters")); if(d)System.out.println("V="+crt); TABLE_COL_VOTERS_NB = crt++;}
		if(show_creator){ cols.add(__("Initiator")); if(d)System.out.println("I="+crt); TABLE_COL_CREATOR = crt++;}
		if(show_provider){ cols.add(__("Provider")); if(d)System.out.println("P="+crt); TABLE_COL_PROVIDER = crt++;}
		if(show_activity){ cols.add(__("Activity")); if(d)System.out.println("A="+crt); TABLE_COL_ACTIVITY = crt++;}
		if(show_recent){ cols.add(__("Hot")); if(d)System.out.println("O="+crt); TABLE_COL_RECENT = crt++;}
		if(show_tmp){ cols.add(__("T")); if(d)System.out.println("T="+crt); TABLE_COL_TMP = crt++;}
		if(show_hidden){ cols.add(__("H")); if(d)System.out.println("H="+crt); TABLE_COL_HIDDEN = crt++;}
		if(show_broadcasted){ cols.add(__("^")); if(d)System.out.println("^="+crt); TABLE_COL_BROADCASTED = crt++;}
		if(show_blocked){ cols.add(__("_")); if(d)System.out.println("_="+crt); TABLE_COL_BLOCKED = crt++;}
		if(show_gid){ cols.add(__("G")); if(d)System.out.println("G="+crt); TABLE_COL_GID_VALID = crt++;}
		if(show_sign){ cols.add(__("S")); if(d)System.out.println("S="+crt); TABLE_COL_SIGN_VALID = crt++;}
		if(show_news){ cols.add(__("News")); if(d)System.out.println("N="+crt); TABLE_COL_NEWS = crt++;}
		if(show_category){ cols.add(__("Category")); if(d)System.out.println("c="+crt); TABLE_COL_CATEGORY = crt++;}
		if(show_creation_date){ cols.add(__("Creation")); if(d)System.out.println("C="+crt); TABLE_COL_CREATION_DATE = crt++;}
		if(show_preferences_date){ cols.add(__("Preference")); if(d)System.out.println("p="+crt); TABLE_COL_PREFERENCES_DATE = crt++;}
		if(show_arrival_date){ cols.add(__("Arrival")); if(d)System.out.println("a="+crt); TABLE_COL_ARRIVAL_DATE = crt++;}
		return cols.toArray(new String[0]);
	}
	//String[] columnToolTips = {null,null,_("A name you provide")};
	public int columnToolTipsCount() {
		return this.getColumnCount();
	}
	public String columnToolTipsEntry(int realIndex) {
		if(realIndex == TABLE_COL_NAME) return __("Short title of the justification!");
		if(realIndex == TABLE_COL_CREATOR) return __("Initiator of this version of the justification!");
		if(realIndex == TABLE_COL_VOTERS_NB) return __("Number of constituents refering this justification with the first choice!");
		if(realIndex == TABLE_COL_CREATION_DATE) return __("Creation Date!");
		if(realIndex == TABLE_COL_ARRIVAL_DATE) return __("Arrival Date!");
		if(realIndex == TABLE_COL_PREFERENCES_DATE) return __("Preferences Date!");
		if(realIndex == TABLE_COL_NEWS) return __("Number of News related to this justification");
		if(realIndex == TABLE_COL_RECENT) return __("This justification is newer than a number of days:")+" "+RECENT_DAYS_OLD;
		if(realIndex == TABLE_COL_CATEGORY) return __("A category name for classifying the justification!");
		if(realIndex == TABLE_COL_ACTIVITY) return __("Total number of constituents plus news related to this justification!");
		if(realIndex == TABLE_COL_PROVIDER) return __("The peer that passed me this!");
		if(realIndex == TABLE_COL_TMP) return __("Is this under editing or not fully received!");
		if(realIndex == TABLE_COL_GID_VALID) return __("Is the GID valid!");
		if(realIndex == TABLE_COL_SIGN_VALID) return __("Is the signature valid!");
		if(realIndex == TABLE_COL_BROADCASTED) return __("Do I broadcast this!");
		if(realIndex == TABLE_COL_BLOCKED) return __("Do I accept votes for this!");
		if(realIndex == TABLE_COL_HIDDEN) return __("Do I want to see this!");
		return null;
	}
	public Icon getIcon(int column) {
		//if (column == TABLE_COL_CREATOR) return null;
		if (column == TABLE_COL_HIDDEN) 
			return DDIcons.getHideImageIcon("Hidden");
		if (column == TABLE_COL_TMP)
				return DDIcons.getTmpImageIcon("TMP");
		if (column == TABLE_COL_GID_VALID)
				return DDIcons.getGIDImageIcon("GID");
		if (column == TABLE_COL_BLOCKED)
				return DDIcons.getBlockImageIcon("Block");
		if (column == TABLE_COL_BROADCASTED) 
			return DDIcons.getBroadcastImageIcon("Broadcast");
			//if (DEBUG) System.out.println("Justification: getIcon: broadcast col? = "+column);
		if (column == TABLE_COL_SIGN_VALID)
				return DDIcons.getSignedImageIcon("Signed");
		if (column == TABLE_COL_RECENT)
				return DDIcons.getHotImageIcon("Hot");
		if (column == TABLE_COL_NEWS)
				return DDIcons.getNewsImageIcon("News");
		if (column == TABLE_COL_VOTERS_NB)
				return DDIcons.getSigImageIcon("Support");
		if (column == TABLE_COL_ACTIVITY)
			return DDIcons.getConImageIcon("Voters");	
		if (column == TABLE_COL_CREATOR)
			return DDIcons.getCreatorImageIcon("Creator");	
		if (column == TABLE_COL_PROVIDER)
			return DDIcons.getMailImageIcon("DHL");	
		if (column == TABLE_COL_ARRIVAL_DATE)
			return DDIcons.getLandingImageIcon("Arrival");	

		//if (DEBUG) System.out.println("Justification: getIcon: col? = "+column);
		//Util.printCallPath("");
		if (column == TABLE_COL_NAME) return null;
		if (column == TABLE_COL_PREFERENCES_DATE) return null;
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
		if (DEBUG) System.out.println("\n************\nJustificationsModel:setCrtChoice: choice="+choice);
		crt_choice = choice;
		
		if (crt_choice == null) {
			this.show_provider = true;
			this.show_gid = true;
			this.show_hidden = true;
			this.show_tmp = true;
			this.show_broadcasted = true;
			this.show_blocked = true;
			this.show_sign = true;
			this.show_preferences_date = true;
		} else {
			this.show_provider = false;
			this.show_gid = false;
			this.show_hidden = false;
			this.show_tmp = false;
			this.show_broadcasted = false;
			this.show_blocked = false;
			this.show_sign = false;
			this.show_preferences_date = false;
		}
		columnNames = getCrtColumns();
		
		update(null, null);
		this.fireTableStructureChanged();
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtChoice: Done");
	}
	public void setCrtAnswered(String answered){
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtAnswered: answered="+answered);
		crt_answered = answered;
		update(null, null);
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtAnswered: Done");
	}
	public void setCrtMotion(String motionID, D_Motion crt_motion){
		if (DEBUG) System.out.println("\n************\nJustificationsModel:setCrtMotion: mID="+motionID);
		crt_motionID = motionID;
		if ((crt_motionID != null) && (crt_motion == null)) { // never expected to happen
//			long cm = new Integer(crt_motionID).longValue();
//			if (cm > 0)
//				try {
			crt_motion = D_Motion.getMotiByLID(crt_motionID, true, false);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
		}
		update(null, null);
		if(DEBUG) System.out.println("\n************\nJustificationsModel:setCrtMotion: Done");
	}
	D_Justification getJustification(int row) {
		D_Justification[] __justification = this._justification;
		if (row < 0) return null;
		if (row >= __justification.length) return null;
		if (__justification != null)
			return __justification[row];
		return __justification[row] = D_Justification.getJustByLID(Util.lval(this.getJustificationID(row)), true, false);
	}
	public boolean isBlocked(int row) {
		D_Justification j = this.getJustification(row);
		if (j == null) return false;
		return j.isBlocked();
	}
	public boolean isBroadcasted(int row) {
		D_Justification j = this.getJustification(row);
		if (j == null) return false;
		return j.isBroadcasted();
	}
	public boolean isRequested(int row) {
		D_Justification j = this.getJustification(row);
		if (j == null) return false;
		return j.isRequested();
	}
	public boolean isServing(int row) {
		if(DEBUG) System.out.println("\n************\nJustificationsModel:isServing: row="+row);
		return isBroadcasted(row);
	}
	public boolean toggleServing(int row) {
		if(DEBUG) System.out.println("\n************\nJustificationModel:Model:toggleServing: row="+row);
		D_Justification m = this.getJustification(row);
		if (m != null) return m.toggleServing();
		return false;
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
		//if (DEBUG) System.out.println("Justifications: getColumnClass: col="+col +" "+TABLE_COL_RECENT+" "+TABLE_COL_TMP);
		if(col == this.TABLE_COL_RECENT) return Boolean.class;
		if(col == this.TABLE_COL_TMP) return Boolean.class;
		if(col == this.TABLE_COL_GID_VALID) return Boolean.class;
		if(col == this.TABLE_COL_SIGN_VALID) return Boolean.class;
		if(col == this.TABLE_COL_BROADCASTED) return Boolean.class;
		if(col == this.TABLE_COL_BLOCKED) return Boolean.class;
		if(col == this.TABLE_COL_HIDDEN) return Boolean.class;
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
		D_Justification j = this.getJustification(row); //D_Justification.getJustByLID(justID, true, false);
		if (j == null) return null;
		//String justID = j.getLIDstr(); //Util.getString(this._justifications[row]);
		if (col == TABLE_COL_CREATION_DATE) {
			result = j.getCreationDateStr();
			if(DEBUG) System.out.println("JustifModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_ARRIVAL_DATE) {
			result = j.getArrivalDateStr();
			if(DEBUG) System.out.println("JustifModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_PREFERENCES_DATE) {
			result = j.getPreferencesDateStr();
			if(DEBUG) System.out.println("JustifModel:getValueAt:date="+result+" row="+row);
		}
		if (col == TABLE_COL_NAME) {
			result = j.getTitleOrMy();
		}
		if (col == TABLE_COL_CREATOR) {
			result = j.getCreatorOrMy();
		}
		if (col == TABLE_COL_PROVIDER) {
			D_Peer r = j.getProvider();
			if (r == null) result = "";// __("Empty");
			else result = r.getName_MyOrDefault(); 
			//break;
		}
		if (col == TABLE_COL_BROADCASTED) {
			result = j.isBroadcasted();
			//break;
		}
		if (col == TABLE_COL_BLOCKED) {
			result = j.isBlocked();
			//break;
		}
		if (col == TABLE_COL_HIDDEN) {
			result = new Boolean(j.isHidden());
		}
		if (col == TABLE_COL_TMP) {
			result = new Boolean(j.isTemporary());
//			String newGID = j.make_ID();
//			if (newGID == null) result = new Boolean(false);
//			else result = new Boolean(j.isTemporary() || !newGID.equals(j.getGID()));
		}
		if (col == TABLE_COL_GID_VALID) {
			String newGID = j.make_ID();
			if (newGID == null) result = new Boolean(false);
			else result = new Boolean(newGID.equals(j.getGID()));
		}
		if (col == TABLE_COL_SIGN_VALID) {
			byte[] sgn = j.getSignature();
			if (sgn == null) result = new Boolean(false);
			else result = new Boolean(sgn.length>0);
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
			return j.getVotes();			
		}
			
		if (col == TABLE_COL_ACTIVITY) { // number of all votes + news
			result = new Integer(""+j.getActivity(0)+ D_News.getCountJust(j, 0));
		}
			
		if (col == TABLE_COL_RECENT) { // any activity in the last x days?
			result = new Boolean((j.getActivity(RECENT_DAYS_OLD)+ D_News.getCountJust(j, RECENT_DAYS_OLD)) > 0);

//			try {
//				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac2, new String[]{motID,Util.getGeneralizedDate(RECENT_DAYS_OLD)});
//				if(orgs.size()>0) result = orgs.get(0).get(0);
//				else result = new Integer("0");
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//				return result;
//			}
//			
//			try {
//				ArrayList<ArrayList<Object>> orgs = db.select(sql_new2, new String[]{motID,Util.getGeneralizedDate(RECENT_DAYS_OLD)});
//				if(orgs.size()>0){
//					int result_int = new Integer(""+Util.lval(result,0)+Util.lval(orgs.get(0).get(0),0));
//					if(result_int>0) result = new Boolean(true); else result = new Boolean(false);
//				}
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
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
		if (col == TABLE_COL_TMP) return true;
		if (col == TABLE_COL_HIDDEN) return true;
		if (col == TABLE_COL_BROADCASTED) return true;
		if (col == TABLE_COL_BLOCKED) return true;
		if (col == TABLE_COL_GID_VALID) return true;
		if (col == TABLE_COL_SIGN_VALID) return true;
		return false;
	}

	public void setTable(Justifications justifications) {
		if (! tables.contains(justifications)) tables.add(justifications);
	}

	//@Override
	//public void removeTableModelListener(TableModelListener arg0) {}
//	public int getRowByID(long just_id){
//		for(int k=0;k<_justifications.length;k++){
//			Object i = _justifications[k];
//			if(DEBUG) System.out.println("JustificationsModel:setCurrent: k="+k+" row_just_ID="+i);
//			Long id = Util.Lval(i);
//			if((id!=null) && (id.longValue()==just_id)) {
//				return k;
//			}
//		}
//		return -1;
//	}
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
		int k = this.findRow(just_id);
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
		"SELECT "
		+ "j." + table.justification.justification_ID 
//		+ ",j."+table.justification.creation_date 
//		+ ",j."+table.justification.global_justification_ID
//		+ ",j."+table.justification.constituent_ID
//		+ ",j."+table.justification.blocked 
//		+ ",j."+table.justification.broadcasted
//		+ ",j."+table.justification.requested
		+ ", count(*) AS cnt "
//		+ ",j."+table.justification.arrival_date 
//		+ ",j."+table.justification.preferences_date
		+ ", max(s."+table.signature.justification_ID+") AS maxsignID "
		+" FROM "+table.justification.TNAME+" AS j "
		+" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE j."+table.justification.motion_ID + "=?"+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	private static String sql_choice_no_answer = 
		"SELECT "
		+ "j."+table.justification.justification_ID 
//		+ ",j."+table.justification.creation_date
//		+ ",j." + table.justification.global_justification_ID 
//		+ ",j."+table.justification.constituent_ID
//		+ ",j."+table.justification.blocked
//		+ ",j."+table.justification.broadcasted
//		+ ",j."+table.justification.requested
		+ ", count(*) AS cnt "
//		+ ",j."+table.justification.arrival_date
//		+ ",j."+table.justification.preferences_date
		+" FROM "+table.justification.TNAME+" AS j "
		+" JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE s."+table.signature.choice+"=? AND j."+table.justification.motion_ID + "=?"+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	private static String sql_no_choice_answer = 
		"SELECT "
		+ "j."+table.justification.justification_ID
//		+ ",j."+table.justification.creation_date
//		+ ",j."+table.justification.global_justification_ID
//		+ ",j."+table.justification.constituent_ID
//		+ ",j."+table.justification.blocked
//		+ ",j."+table.justification.broadcasted
//		+ ",j."+table.justification.requested
		+ ", count(*) AS cnt "
//		+ ",j."+table.justification.arrival_date
//		+ ",j."+table.justification.preferences_date
		+" FROM "+table.justification.TNAME+" AS j "
		+" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE j."+table.justification.motion_ID + "=? AND j."+table.justification.answerTo_ID+"=? "+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	private static String sql_choice_answer = 
		"SELECT "
		+ "j."+table.justification.justification_ID
//		+ ",j."+table.justification.creation_date
//		+ ",j."+table.justification.global_justification_ID
//		+ ",j."+table.justification.constituent_ID
//		+ ",j."+table.justification.blocked
//		+ ",j."+table.justification.broadcasted
//		+ ",j."+table.justification.requested
		+ ", count(*) AS cnt "
//		+ ",j."+table.justification.arrival_date
//		+ ",j."+table.justification.preferences_date
		+" FROM "+table.justification.TNAME+" AS j "
		+" JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.justification_ID+"=j."+table.justification.justification_ID+")"+
		" WHERE s."+table.signature.choice+"=? AND j."+table.justification.motion_ID + "=? AND j."+table.justification.answerTo_ID+"=? "+
		" GROUP BY j."+table.justification.justification_ID+
		" ORDER BY cnt DESC"+
		";";
	public String getJustificationID(int row) {
		Object[] __justifications = _justifications;
		if ((row < 0) || (row >= __justifications.length)) return null;
		return Util.getString(__justifications[row]);
	}
	
	static final int SELECT_ID = 0;
//		final int SELECT_CREATION_DATE = 1;
//		final int SELECT_GID = 2;
//		final int SELECT_CONST_ID = 3;
//		final int SELECT_BLOCKED = 4;
//		final int SELECT_BROADCAST = 5;
//		final int SELECT_REQUESTED = 6;
	static final int SELECT_CNT = 1; //7;
//		final int SELECT_ARRIVAL_DATE = 8;
//		final int SELECT_PREFERENCES_DATE = 9;
	static final int SELECT_MAX_JUST_ID_FOR_SIGN = 2; //10;// only for: sql_no_choice_no_answer
	
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		//boolean DEBUG=true;
		if(DEBUG) System.out.println("\nwidgets.justifications.JustificationsModel: update table= "+_table+": info= "+info);
		if (crt_motionID == null) {
			if(DEBUG) System.out.println("\nwidgets.justifications.JustificationsModel: null crt_motion_id");
			return;
		}
		Object old_sel[] = new Object[tables.size()];
		{
			Object[] __justifications = _justifications;
			for(int i=0; i<old_sel.length; i++) {
				int sel = tables.get(i).getSelectedRow();
				if ((sel >= 0) && (sel < __justifications.length)) old_sel[i] = __justifications[sel];
			}
		}
		
		Object[] _t__justifications;
		D_Justification[] _t__justification;
		Object[] _t__votes;
		Hashtable<String, Integer> _t_rowByID;
		
		try {
			ArrayList<ArrayList<Object>> justi=null;
			if((crt_choice == null)&&(crt_answered==null)) justi = db.select(sql_no_choice_no_answer, new String[]{crt_motionID}, DEBUG);
			if((crt_choice != null)&&(crt_answered==null)) justi = db.select(sql_choice_no_answer, new String[]{crt_choice, crt_motionID}, DEBUG);
			if((crt_choice == null)&&(crt_answered!=null)) justi = db.select(sql_no_choice_answer, new String[]{crt_motionID, crt_answered}, DEBUG);
			if((crt_choice != null)&&(crt_answered!=null)) justi = db.select(sql_choice_answer, new String[]{crt_choice, crt_motionID, crt_answered}, DEBUG);
			_t__justifications = new Object[justi.size()];
			_t__justification = new D_Justification[justi.size()];
			_t__votes = new Object[justi.size()];
			//_meth = new Object[orgs.size()];
//			_hash = new Object[justi.size()];
//			_crea = new Object[justi.size()];
//			_crea_date = new Object[justi.size()];
//			_arrival_date = new Object[justi.size()];
//			_preferences_date = new Object[justi.size()];
//			_gid = new boolean[justi.size()];
//			_blo = new boolean[justi.size()];
//			_bro = new boolean[justi.size()];
//			_req = new boolean[justi.size()];
			_t_rowByID = new Hashtable<String, Integer>();
			for(int k = 0; k<_t__justifications.length; k++) {
				ArrayList<Object> j = justi.get(k);
				_t__justifications[k] = j.get(SELECT_ID);
				_t__justification[k] = D_Justification.getJustByLID(Util.Lval(_t__justifications[k]), true, false);
				_t_rowByID.put(Util.getString(_t__justifications[k]), new Integer(k));
				//_meth[k] = orgs.get(k).get(SELECT_CREATION_DATE);
//				_hash[k] = j.get(SELECT_GID);
//				_crea[k] = j.get(SELECT_CONST_ID);
//				_gid[k] = (j.get(SELECT_CONST_ID) != null);
//				_blo[k] = Util.stringInt2bool(j.get(SELECT_BLOCKED), false);
//				_bro[k] = Util.stringInt2bool(j.get(SELECT_BROADCAST), false);
//				_req[k] = Util.stringInt2bool(j.get(SELECT_REQUESTED), false);
				_t__votes[k] = j.get(SELECT_CNT);
				if (Util.stringInt2bool(_t__votes[k], false))
					if (
							(j.size() > SELECT_MAX_JUST_ID_FOR_SIGN)
							&& (j.get(SELECT_MAX_JUST_ID_FOR_SIGN) == null)
						) _t__votes[k] = "0"; 
//				_crea_date[k] = j.get(SELECT_CREATION_DATE);
//				_arrival_date[k] = j.get(SELECT_ARRIVAL_DATE);
//				_preferences_date[k] = j.get(SELECT_PREFERENCES_DATE);
				
				_t__justification[k].setVotes(_t__votes[k]);
			}
			if (DEBUG) System.out.println("widgets.org.Justifications: A total of: "+_t__justifications.length);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}

		synchronized (this) {
			this._justification = _t__justification;
			this._justifications = _t__justifications;
			//this._votes = _t__votes;
			this.rowByID = _t_rowByID;
		}
		
		for (int k = 0; k < old_sel.length; k ++){
			Justifications i = tables.get(k);
			//int row = i.getSelectedRow();
			int row = findRow(old_sel[k]);
			if(DEBUG) System.out.println("widgets.org.Justifications: selected row: "+row);
			//i.revalidate();
			this.fireTableDataChanged();
			if ((row >= 0) && (row < _justifications.length)) i.setRowSelectionInterval(row, row);
			i.fireListener(row, Justifications.A_NON_FORCE_COL, true); // no need to tell listeners of an update (except if telling the cause)
			//TODO the next is probably slowing down the system. May be run only on request
			i.initColumnSizes();
		}		
		if(DEBUG) System.out.println("widgets.org.Justifications: Done");
	}

	private int findRow(Object id) {
		if (id == null) return -1;
		Integer row = this.rowByID.get(id);
		if (row == null) return -1;
		return row;
//		for(int k=0; k < _justifications.length; k++) if(id.equals(_justifications[k])) return k;
//		return -1;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		D_Justification _m = this.getJustification(row);
		if (_m == null) return;
		_m = D_Justification.getJustByJust_Keep(_m);
		if (_m == null) return;

		if (col == TABLE_COL_NAME)
			if(value instanceof D_Document_Title){
				D_Document_Title _value = (D_Document_Title) value;
				if (_value.title_document != null)  {
					String format  = _value.title_document.getFormatString();
					if((format==null) || D_Document.TXT_FORMAT.equals(format))
						value = _value.title_document.getDocumentUTFString();
				}
			}
			_m.setNameMy(Util.getString(value));
			//set_my_data(table.my_justification_data.name, Util.getString(value), row);
			
		if (col == TABLE_COL_CREATOR)
			_m.setCreatorMy(Util.getString(value));
			//set_my_data(table.my_justification_data.creator, Util.getString(value), row);
			/*
		case TABLE_COL_CATEGORY:
			set_my_data(table.my_justification_data.category, Util.getString(value), row);
			break;
			*/
		//}
		_m.storeRequest();
		_m.releaseReference();
		fireTableCellUpdated(row, col);
	}
//	private void set_my_data(String field_name, String value, int row) {
//		if(row >= _justifications.length) return;
//		if("".equals(value)) value = null;
//		if(DEBUG)System.out.println("Set value =\""+value+"\"");
//		String justification_ID = Util.getString(_justifications[row]);
//		try {
//			String sql = "SELECT "+field_name+" FROM "+table.my_justification_data.TNAME+" WHERE "+table.my_justification_data.justification_ID+"=?;";
//			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{justification_ID});
//			if(orgs.size()>0){
//				db.update(table.my_justification_data.TNAME, new String[]{field_name},
//						new String[]{table.my_justification_data.justification_ID}, new String[]{value, justification_ID});
//			}else{
//				if(value==null) return;
//				db.insert(table.my_justification_data.TNAME,
//						new String[]{field_name,table.my_justification_data.justification_ID},
//						new String[]{value, justification_ID});
//			}
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//	}
	/**
	 * Is the creator of this justification, myself?
	 * @param row
	 * @return
	 */
	public boolean isMine(int row) {
		D_Justification j = this.getJustification(row);
		if (j == null) return false;
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
		D_Constituent c = D_Constituent.getConstByLID(j.getConstituentLIDstr(), true, false);
		if (!c.isExternal() && (c.getSK() != null))
			return true; // I have the key => editable
		return false; // I do not have the key => not editable;
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("Justifications:isNotReady: row="+row);
		D_Justification j = this.getJustification(row);
		if (j == null) {
			if(DEBUG) System.out.println("Justifications:isNotReady: row>"+_justifications.length);
			return false;
		}
		if (j.getGID() == null) {
			if(DEBUG) System.out.println("Justifications:isNotReady: gid false");
			return true;
		}
//		String cID = j.getConstituentGID(); //Util.getString(_crea[row]);
//		if(cID == null){
//			if(DEBUG) System.out.println("Justifications:isNotReady: cID null");
//			return true;
//		}
		if(DEBUG) System.out.println("Justifications:isNotReady: exit false");
		return false;
	}
//	public static void setBlocking(String justificationID, boolean val) {
//		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
//		try {
//			Application.db.update(table.justification.TNAME,
//					new String[]{table.justification.blocked},
//					new String[]{table.justification.justification_ID},
//					new String[]{val?"1":"0", justificationID}, DEBUG);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//	}
//	/**
//	 * change org.broadcasted Better change with toggleServing which sets also peer.served_orgs
//	 * @param orgID
//	 * @param val
//	 */
//	static void setBroadcasting(String justificationID, boolean val) {
//		if(DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+justificationID);
//		try {
//			Application.db.update(table.justification.TNAME,
//					new String[]{table.justification.broadcasted},
//					new String[]{table.justification.justification_ID},
//					new String[]{val?"1":"0", justificationID}, DEBUG);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
//	}
//	public static void setRequested(String justificationID, boolean val) {
//		if(DEBUG) System.out.println("Orgs:setRequested: set="+val);
//		try {
//			Application.db.update(table.justification.TNAME,
//					new String[]{table.justification.requested},
//					new String[]{table.justification.justification_ID},
//					new String[]{val?"1":"0", justificationID}, DEBUG);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//	}
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
			cleanMemory();
			this.setCrtMotion(motionID, d_motion);
		}
		if(DEBUG) System.out.println("\n************\nJustificationsModel:motUpdate:choice="+crt_choice+": Done");
	}
	void cleanMemory() {
		synchronized(this) {
			this._justifications= new Object[0]; // do not remember current selections
			this._justification = new D_Justification[0];
			this.rowByID = new Hashtable<String, Integer>();
		}
	}
	public long getConstituentIDMyself() {
		return  GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();

	}
	public String getConstituentGIDMyself() {
		return  GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
	}
	public String getOrganizationID() {
		if(crt_motion == null) return null;
		return  crt_motion.getOrganizationLIDstr();
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

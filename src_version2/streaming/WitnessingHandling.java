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

package streaming;

import static java.lang.System.out;
import hds.ASNSyncRequest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import ASN1.Encoder;
import util.P2PDDSQLException;
import config.Application;
import config.DD;
import data.D_Organization;
import data.D_Peer;
import data.D_Witness;
import util.DBInterface;
import util.Util;

public class WitnessingHandling {

	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;

	static String sql_get_hashes=
		"SELECT w."+table.witness.global_witness_ID
		+" FROM "+table.witness.TNAME+" AS w "+
		//" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
		" JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
		//" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
		//" JOIN "+table.organization.TNAME+" AS o ON(s."+table.constituent.organization_ID+"=o."+table.organization.organization_ID+") "+
		" WHERE "
		+" s."+table.constituent.organization_ID+"=? "
		+" AND w."+table.witness.arrival_date+">? " 
		+" AND w."+table.witness.arrival_date+"<=? "
		//+" AND o."+table.organization.broadcasted+" <> '0' "
		+" AND s."+table.constituent.broadcasted+" <> '0' " 
		+" AND w."+table.witness.signature +" IS NOT NULL "
		+" AND w."+table.witness.global_witness_ID +" IS NOT NULL "
		+" ORDER BY w."+table.witness.arrival_date
		;
	public static ArrayList<String> getWitnessingHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}
	
	/**
 * Return the date allowing approx LIMIT=5 witness messages
 * @param last_sync_date
 * @param _maxDate
 * @param ofi
 * @param orgs
 * @param limitWitnessLow 
 * @return
 * @throws P2PDDSQLException
 */
	public static String getNextWitnessingDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitWitnessLow) throws P2PDDSQLException {
		if(DEBUG) out.println("WitnessingHandling:getNextWitnessingDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if(ofi==null){
			String sql=
				"SELECT w."+table.witness.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.witness.TNAME+" AS w "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+"=o."+table.organization.organization_ID+") "
				+" WHERE " +
				" w."+table.witness.signature +" IS NOT NULL "+
				 " AND o."+table.organization.broadcasted+" <> '0' " +
				 " AND c."+table.constituent.broadcasted+" <> '0' " +
						" AND w."+table.witness.arrival_date+">? " +
				((_maxDate!=null)?" AND w."+table.witness.arrival_date+"<=? ":"")
				+" ORDER BY w."+table.witness.arrival_date
						+" LIMIT "+(limitWitnessLow+1)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.db.select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				String orgID = Util.getString(s.get(1));
				orgs.add(orgID);
				if(DEBUG) out.println("WitnessingHandling:getNextWitnessingDate: present full data in orgID = "+orgID);
			}
			
		}else{
			String sql=
				"SELECT w."+table.witness.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.witness.TNAME+" AS w "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+"=o."+table.organization.organization_ID+") "
				+" WHERE " +
				" w."+table.witness.signature +" IS NOT NULL "+
				 " AND o."+table.organization.broadcasted+" <> '0' " +
				 " AND c."+table.constituent.broadcasted+" <> '0' " +
						" AND w."+table.witness.arrival_date+">? " +
				((_maxDate!=null)?" AND w."+table.witness.arrival_date+"<=? ":"")
				+" AND c."+table.constituent.organization_ID+"=? "
				+" ORDER BY w."+table.witness.arrival_date
				+" LIMIT "+(limitWitnessLow+1)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.db.select(sql, params, DEBUG);
			if(w.size()>0) {
				orgs.add(Util.getString(orgID));
				if(DEBUG) out.println("WitnessingHandling:getNextWitnessingDate: present ofi data in orgID = "+orgID);
			}
		}
		if(w.size()>limitWitnessLow){
			_maxDate = Util.getString(w.get(limitWitnessLow-1).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("WitnessingHandling: getNextWitnessingDate: limits reached: "+limitWitnessLow+" date="+_maxDate);
		}
		
		if(DEBUG) out.println("WitnessingHandling:getNextWitnessingDate: end: "+_maxDate);
		return _maxDate;
	}

	public static D_Witness[] getWitnessingData(ASNSyncRequest asr,
			String last_sync_date, String org_gid, String org_id,
			String[] __maxDate) throws P2PDDSQLException {
		
		D_Witness[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("WitnessingHandling:getWitnessingData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql=
				"SELECT "+Util.setDatabaseAlias(table.witness.witness_fields, "w")
				//+", n."+table.neighborhood.global_neighborhood_ID
				//+", t."+table.constituent.global_constituent_ID
				//+", s."+table.constituent.global_constituent_ID
				//+", o."+table.organization.global_organization_ID//", w."+table.witness.source_ID+
				//+", o."+table.organization.organization_ID
				+" FROM "+table.witness.TNAME+" AS w "+
				//+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				//" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
				" LEFT JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
				//" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
				" LEFT JOIN "+table.organization.TNAME+" AS o ON(s."+table.constituent.organization_ID+"=o."+table.organization.organization_ID+") "+
				" WHERE " +
				" w."+table.witness.signature +" IS NOT NULL "+
				 " AND o."+table.organization.broadcasted+" <> '0' " +
				 " AND s."+table.constituent.broadcasted+" <> '0' " +
				" AND w."+table.witness.arrival_date+">? " +
				((_maxDate!=null)?" AND w."+table.witness.arrival_date+"<=? ":"")
				+" AND s."+table.constituent.organization_ID+"=? "
				+" ORDER BY w."+table.witness.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.db.select(sql, params, DEBUG);

		if (w.size() > 0) {
			result = new D_Witness[w.size()];
			for (int k = 0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = D_Witness.getWitness(s);
				result[k].global_organization_ID = null; // available
			}
		}
		
		if(DEBUG) out.println("WitnessingHandling:getWitnessingData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}

	public static boolean integrateNewData(D_Witness[] witnesses, String orgGID,
			String org_local_ID, String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if(DEBUG) out.println("WitnessHandling:integrateNewData: start oID="+org_local_ID+" gid="+orgGID);
		if(witnesses==null) return false;
		for(int k=0; k<witnesses.length; k++) {
			if(DEBUG) out.println("WitnessHandling:integrateNewData: store w="+witnesses[k]);
			witnesses[k].organization_ID = org_local_ID;
			witnesses[k].global_organization_ID = orgGID;
			long id=witnesses[k].store(sol_rq, new_rq, __peer);
			if(DEBUG) out.println("WitnessHandling:integrateNewData: store got id="+id);
		}
		return witnesses.length>0;
	}

	static public void main(String[] arg) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			_main(arg);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	static public void _main(String[] arg) throws P2PDDSQLException {
		HashSet<String> a = new HashSet<String>();
		String start = arg[0];
		String end=null;
		if(arg.length>1) end = arg[1];
		/*
		if(start ==null) start = "0";
		String date = getNextWitnessingDate(start, end, null, a );
		System.out.println("Got date: "+date);
		for(String o: a){
			System.out.println("Got org: "+o);
		}
		*/
		String org_gid = D_Organization.getGIDbyLIDstr("1");
		D_Witness[] w = getWitnessingData(null, start, org_gid, "1", new String[]{end});
		for(D_Witness W:w) {
			System.out.println("\n***\nw="+W);
		}
		Calendar now=Util.CalendargetInstance();
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		integrateNewData(w, org_gid, "1", Encoder.getGeneralizedTime(now), null, sol_rq, new_rq, null);
	}
}

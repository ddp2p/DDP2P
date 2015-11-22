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
package net.ddp2p.common.streaming;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class WitnessingHandling {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;
	static String sql_get_hashes=
		"SELECT w."+net.ddp2p.common.table.witness.global_witness_ID
		+" FROM "+net.ddp2p.common.table.witness.TNAME+" AS w "+
		" JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS s ON(s."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.source_ID+") "+
		" WHERE "
		+" s."+net.ddp2p.common.table.constituent.organization_ID+"=? "
		+" AND w."+net.ddp2p.common.table.witness.arrival_date+">? " 
		+" AND w."+net.ddp2p.common.table.witness.arrival_date+"<=? "
		+" AND s."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' " 
		+" AND w."+net.ddp2p.common.table.witness.signature +" IS NOT NULL "
		+" AND w."+net.ddp2p.common.table.witness.global_witness_ID +" IS NOT NULL "
		+" ORDER BY w."+net.ddp2p.common.table.witness.arrival_date
		;
	public static ArrayList<String> getWitnessingHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.getDB().select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
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
				"SELECT w."+net.ddp2p.common.table.witness.arrival_date+", c."+net.ddp2p.common.table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+net.ddp2p.common.table.witness.TNAME+" AS w "
				+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.source_ID+")"
				+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(c."+net.ddp2p.common.table.constituent.organization_ID+"=o."+net.ddp2p.common.table.organization.organization_ID+") "
				+" WHERE " +
				" w."+net.ddp2p.common.table.witness.signature +" IS NOT NULL "+
				 " AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
				 " AND c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' " +
						" AND w."+net.ddp2p.common.table.witness.arrival_date+">? " +
				((_maxDate!=null)?" AND w."+net.ddp2p.common.table.witness.arrival_date+"<=? ":"")
				+" ORDER BY w."+net.ddp2p.common.table.witness.arrival_date
						+" LIMIT "+(limitWitnessLow+1)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			w = Application.getDB().select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				String orgID = Util.getString(s.get(1));
				orgs.add(orgID);
				if(DEBUG) out.println("WitnessingHandling:getNextWitnessingDate: present full data in orgID = "+orgID);
			}
		}else{
			String sql=
				"SELECT w."+net.ddp2p.common.table.witness.arrival_date+", c."+net.ddp2p.common.table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+net.ddp2p.common.table.witness.TNAME+" AS w "
				+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.source_ID+")"
				+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(c."+net.ddp2p.common.table.constituent.organization_ID+"=o."+net.ddp2p.common.table.organization.organization_ID+") "
				+" WHERE " +
				" w."+net.ddp2p.common.table.witness.signature +" IS NOT NULL "+
				 " AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
				 " AND c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' " +
						" AND w."+net.ddp2p.common.table.witness.arrival_date+">? " +
				((_maxDate!=null)?" AND w."+net.ddp2p.common.table.witness.arrival_date+"<=? ":"")
				+" AND c."+net.ddp2p.common.table.constituent.organization_ID+"=? "
				+" ORDER BY w."+net.ddp2p.common.table.witness.arrival_date
				+" LIMIT "+(limitWitnessLow+1)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.getDB().select(sql, params, DEBUG);
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
				"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.witness.witness_fields, "w")
				+" FROM "+net.ddp2p.common.table.witness.TNAME+" AS w "+
				" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS s ON(s."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.source_ID+") "+
				" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(s."+net.ddp2p.common.table.constituent.organization_ID+"=o."+net.ddp2p.common.table.organization.organization_ID+") "+
				" WHERE " +
				" w."+net.ddp2p.common.table.witness.signature +" IS NOT NULL "+
				 " AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
				 " AND s."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' " +
				" AND w."+net.ddp2p.common.table.witness.arrival_date+">? " +
				((_maxDate!=null)?" AND w."+net.ddp2p.common.table.witness.arrival_date+"<=? ":"")
				+" AND s."+net.ddp2p.common.table.constituent.organization_ID+"=? "
				+" ORDER BY w."+net.ddp2p.common.table.witness.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.getDB().select(sql, params, DEBUG);
		if (w.size() > 0) {
			result = new D_Witness[w.size()];
			for (int k = 0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = D_Witness.getWitness(s);
				result[k].global_organization_ID = null; 
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
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
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

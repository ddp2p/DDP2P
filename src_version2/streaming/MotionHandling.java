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
import java.util.HashSet;

import util.P2PDDSQLException;
import config.Application;
import config.DD;
import data.D_Organization;
import data.D_Motion;
import data.D_Peer;
import data.D_Witness;
import util.Util;

public class MotionHandling {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;

	public static boolean integrateNewData(D_Motion[] motions, String orgGID,
			String org_local_ID, String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if (motions == null) return false;
		for (int k = 0; k < motions.length; k ++) {
			motions[k].setOrganizationGID(orgGID);
			motions[k].setOrganizationLIDstr(org_local_ID);
			// motions[k].store(sol_rq, new_rq);
			D_Motion m = D_Motion.getMotiByGID(motions[k].getGID(), true, true, true, __peer, Util.lval(org_local_ID));
			m.loadRemote(motions[k], sol_rq, new_rq, __peer);
			m.storeRequest();
			m.releaseReference();
		}
		return motions.length>0;
	}
	
/**
 * Return the date allowing approx LIMIT=5 witness messages
 * @param last_sync_date
 * @param _maxDate
 * @param ofi
 * @param orgs
 * @param limitMotionLow 
 * @return
 * @throws P2PDDSQLException
 */
	public static String getNextMotionDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitMotionLow) throws P2PDDSQLException {
		if(DEBUG) out.println("MotionHandling:getNextMotionDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if(ofi==null){
			String sql=
				"SELECT m."+table.motion.arrival_date+", m."+table.motion.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.motion.TNAME+" AS m "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"
				+" WHERE " +
				" m."+table.motion.signature+" IS NOT NULL "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
						" AND m."+table.motion.arrival_date+">? " +
				((_maxDate!=null)?" AND m."+table.motion.arrival_date+"<=? ":"")
				+" ORDER BY m."+table.motion.arrival_date
						+" LIMIT "+(1+limitMotionLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.db.select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
			
		}else{
			String sql=
				"SELECT m."+table.motion.arrival_date+", m."+table.motion.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.motion.TNAME+" AS m "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"
				//+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				+" WHERE " +
				" m."+table.motion.signature+" IS NOT NULL "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
						" AND m."+table.motion.arrival_date+">? " +
				((_maxDate!=null)?" AND m."+table.motion.arrival_date+"<=? ":"")
				+" AND m."+table.motion.organization_ID+"=? "
				+" ORDER BY m."+table.motion.arrival_date
				+" LIMIT "+(1+limitMotionLow)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.db.select(sql, params, DEBUG);
			if(w.size()>0)
				orgs.add(Util.getString(orgID));
		}
		if(w.size()>limitMotionLow){
			_maxDate = Util.getString(w.get(limitMotionLow-1).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("MotionHandling: getNextMotionDate: limits reached: "+limitMotionLow+" date="+_maxDate);
		}
		
		if(DEBUG) out.println("MotionHandling:getNextMotionDate: end: "+_maxDate);
		return _maxDate;
	}

	public static D_Motion[] getMotionData(ASNSyncRequest asr,
			String last_sync_date, String org_gid, String org_id,
			String[] __maxDate) throws P2PDDSQLException {
		
		D_Motion[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("MotionHandling:getMotionData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql=
			"SELECT "+Util.setDatabaseAlias(table.motion.fields,"m")+
			", c."+table.constituent.global_constituent_ID+
			", e."+table.motion.global_motion_ID+
			", o."+table.organization.global_organization_ID+
			" FROM "+table.motion.TNAME+" AS m "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS e ON(e."+table.motion.motion_ID+"=m."+table.motion.enhances_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
				" WHERE " +
				" m."+table.motion.signature+" IS NOT NULL "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
				" AND m."+table.motion.arrival_date+">? " +
				((_maxDate!=null)?" AND m."+table.motion.arrival_date+"<=? ":"")
				+" AND m."+table.motion.organization_ID+"=? "
				+" ORDER BY m."+table.motion.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.db.select(sql, params, DEBUG);

		if(w.size()>0) {
			result = new D_Motion[w.size()];
			for(int k=0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = D_Motion.getMotiByLID(Util.getString(s.get(table.motion.M_MOTION_ID)), true, false); //new D_Motion(s);
			}
		}
		
		if(DEBUG) out.println("MotionHandling:getMotionData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}
	static String sql_get_hashes=
		"SELECT m."+table.motion.global_motion_ID+
		" FROM "+table.motion.TNAME+" AS m "+
		" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"+
		//" LEFT JOIN "+table.motion.TNAME+" AS e ON(e."+table.motion.motion_ID+"=m."+table.motion.enhances_ID+")"+
		//" JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE " +
			" m."+table.motion.organization_ID+"=? "+
			" AND m."+table.motion.organization_ID+"= c."+table.constituent.organization_ID+
			" AND m."+table.motion.arrival_date+">? " +
			" AND m."+table.motion.arrival_date+"<=? "+
			//" AND o."+table.organization.broadcasted+" <> '0' "+
			" AND c."+table.constituent.broadcasted+" <> '0' "+
			" AND m."+table.motion.broadcasted+" <> '0' "+
			" AND m."+table.motion.signature+" IS NOT NULL "+
			" AND m."+table.motion.global_motion_ID+" IS NOT NULL "+
			" ORDER BY m."+table.motion.arrival_date
		;
	public static ArrayList<String> getMotionHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		
		ArrayList<String> r = new ArrayList<String>();
		/*
		if(_DEBUG&&result.size()>0){
			if(_DEBUG)System.out.println("MotionHandling:getMH:got 1="+result.size());
			result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
					new String[]{org_id, last_sync_date, maxDate}, _DEBUG);
			if(_DEBUG)System.out.println("MotionHandling:getMH:got 2="+result.size());
			r = Util.AL_AL_O_2_AL_S(result);
			if(_DEBUG)System.out.println("MotionHandling:getMH:got="+Util.concat(r, "\n", "null"));
		}
		*/
		r = Util.AL_AL_O_2_AL_S(result);
		return r;
	}


}

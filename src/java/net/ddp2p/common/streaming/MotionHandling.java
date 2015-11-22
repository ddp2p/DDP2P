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
import java.util.HashSet;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
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
			D_Motion m = D_Motion.getMotiByGID(motions[k].getGID(), true, true, true, __peer, Util.lval(org_local_ID), null);
			m.loadRemote(motions[k], sol_rq, new_rq, __peer);
			m.storeRequest();
			m.releaseReference();
		}
		return motions.length>0;
	}
	private static final String sql_motions = 
			"SELECT m."+net.ddp2p.common.table.motion.arrival_date+", m."+net.ddp2p.common.table.motion.organization_ID//", w."+table.witness.source_ID+
			+" FROM "+net.ddp2p.common.table.motion.TNAME+" AS m "
			+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=m."+net.ddp2p.common.table.motion.constituent_ID+")"
			+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=m."+net.ddp2p.common.table.motion.organization_ID+")"
			+" WHERE " +
			" m."+net.ddp2p.common.table.motion.temporary+" = '0' "+
			" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
			" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
			" AND ( c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
					" AND m."+net.ddp2p.common.table.motion.arrival_date+">? ";
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
		if (ofi == null) {
			String sql = sql_motions +
				((_maxDate!=null)?" AND m."+net.ddp2p.common.table.motion.arrival_date+"<=? ":"")
				+" ORDER BY m."+net.ddp2p.common.table.motion.arrival_date
						+" LIMIT "+(1+limitMotionLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			w = Application.getDB().select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
		}else{
			String sql = sql_motions +
				((_maxDate!=null)?" AND m."+net.ddp2p.common.table.motion.arrival_date+"<=? ":"")
				+" AND m."+net.ddp2p.common.table.motion.organization_ID+"=? "
				+" ORDER BY m."+net.ddp2p.common.table.motion.arrival_date
				+" LIMIT "+(1+limitMotionLow)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.getDB().select(sql, params, DEBUG);
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
	private static final String sql_motion_data=
			"SELECT "+
					"m."+net.ddp2p.common.table.motion.motion_ID+
			" FROM "+net.ddp2p.common.table.motion.TNAME+" AS m "+
			" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=m."+net.ddp2p.common.table.motion.constituent_ID+")"+
			" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=m."+net.ddp2p.common.table.motion.organization_ID+")"+
				" WHERE " +
				" m."+net.ddp2p.common.table.motion.temporary+" == '0' "+
				" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
				" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
				" AND ( c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0'  OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
				" AND m."+net.ddp2p.common.table.motion.arrival_date+">? ";
	public static D_Motion[] getMotionData(ASNSyncRequest asr,
			String last_sync_date, String org_gid, String org_id,
			String[] __maxDate) throws P2PDDSQLException {
		if (DEBUG) System.out.println("MotionHandling: getMotData: start");
		D_Motion[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("MotionHandling:getMotionData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		String sql= sql_motion_data +
				((_maxDate!=null)?" AND m."+net.ddp2p.common.table.motion.arrival_date+"<=? ":"")
				+" AND m."+net.ddp2p.common.table.motion.organization_ID+"=? "
				+" ORDER BY m."+net.ddp2p.common.table.motion.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		if (_maxDate == null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.getDB().select(sql, params, DEBUG);
		if (w.size() > 0) {
			result = new D_Motion[w.size()];
			for (int k = 0; k < w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = D_Motion.getMotiByLID(Util.getString(s.get(0)), true, false); 
			}
		}
		if(DEBUG) out.println("MotionHandling:getMotionData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}
	private static final String sql_get_hashes=
		"SELECT m."+net.ddp2p.common.table.motion.global_motion_ID+
		" FROM "+net.ddp2p.common.table.motion.TNAME+" AS m "+
		" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=m."+net.ddp2p.common.table.motion.constituent_ID+")"+
			" WHERE " +
			" m."+net.ddp2p.common.table.motion.organization_ID+"=? "+
			" AND ( m."+net.ddp2p.common.table.motion.organization_ID+"= c."+net.ddp2p.common.table.constituent.organization_ID + " OR  c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
			" AND m."+net.ddp2p.common.table.motion.arrival_date+">? " +
			" AND m."+net.ddp2p.common.table.motion.arrival_date+"<=? "+
			" AND ( c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0'  OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
			" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
			" AND m."+net.ddp2p.common.table.motion.temporary+" == '0' "+
			" AND m."+net.ddp2p.common.table.motion.global_motion_ID+" IS NOT NULL "+
			" ORDER BY m."+net.ddp2p.common.table.motion.arrival_date
		;
	public static ArrayList<String> getMotionHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if (DEBUG) System.out.println("MotionHandling: getMotHashes: start");
		if ((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.getDB().select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		ArrayList<String> r = new ArrayList<String>();
		r = Util.AL_AL_O_2_AL_S(result);
		if (DEBUG) System.out.println("MotionHandling: getMotHashes: return "+r.size());
		return r;
	}
}

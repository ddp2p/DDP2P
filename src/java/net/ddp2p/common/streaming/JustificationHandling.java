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

package net.ddp2p.common.streaming;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.HashSet;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class JustificationHandling {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;
	
	private static final String sql_motions =
			"SELECT j."+net.ddp2p.common.table.justification.arrival_date+", c."+net.ddp2p.common.table.constituent.organization_ID//", w."+table.witness.source_ID+
			+" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "
			+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=j."+net.ddp2p.common.table.justification.constituent_ID+")"
			+" LEFT JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(m."+net.ddp2p.common.table.motion.motion_ID+"=j."+net.ddp2p.common.table.justification.motion_ID+")"
			+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=c."+net.ddp2p.common.table.constituent.organization_ID+")"
			+" WHERE " +
			//" j."+table.justification.signature+" IS NOT NULL "+
			" j."+net.ddp2p.common.table.justification.temporary+" == '0' "+
			" AND j."+net.ddp2p.common.table.justification.broadcasted+" <> '0' "+
			" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
			" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
			" AND ( c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0'  OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
					" AND j."+net.ddp2p.common.table.justification.arrival_date+">? ";

	public static String getNextJustificationDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitJustLow) throws P2PDDSQLException {
		if(DEBUG) out.println("JustificationHandling:getNextJustificationDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if (ofi == null) {
			String sql = sql_motions +
				((_maxDate!=null)?" AND j."+net.ddp2p.common.table.justification.arrival_date+"<=? ":"")
				+" ORDER BY j."+net.ddp2p.common.table.justification.arrival_date
						+" LIMIT "+(1+limitJustLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.getDB().select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
			
		} else {
			String sql = sql_motions +
//				"SELECT j."+table.justification.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
//				+" FROM "+table.justification.TNAME+" AS j "
//				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"
//				+" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"
//				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+")"
//				+" WHERE " +
//				" j."+table.justification.signature+" IS NOT NULL "+
//				" AND j."+table.justification.broadcasted+" <> '0' "+
//				" AND m."+table.motion.broadcasted+" <> '0' "+
//				" AND o."+table.organization.broadcasted+" <> '0' "+
//				" AND c."+table.constituent.broadcasted+" <> '0' "+
//						" AND j."+table.justification.arrival_date+">? " +
				((_maxDate!=null)?" AND j."+net.ddp2p.common.table.justification.arrival_date+"<=? ":"")
				+" AND c."+net.ddp2p.common.table.constituent.organization_ID+"=? "
				+" ORDER BY j."+net.ddp2p.common.table.justification.arrival_date
				+" LIMIT "+(1+limitJustLow)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.getDB().select(sql, params, DEBUG);
			if(w.size()>0)
				orgs.add(Util.getString(orgID));
		}
		if(w.size() > limitJustLow) {
			_maxDate = Util.getString(w.get(limitJustLow-1).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("JustificationHandling: getNextJustificationDate: limits reached: "+limitJustLow+" date="+_maxDate);
		}
		
		if(DEBUG) out.println("JustificationHandling:getNextJustificationDate: end: "+_maxDate);
		return _maxDate;
	}
	private static final String sql_get_hashes=
		"SELECT j."+net.ddp2p.common.table.justification.global_justification_ID+
		" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "+
		" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=j."+net.ddp2p.common.table.justification.constituent_ID+")"+
		" JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(m."+net.ddp2p.common.table.motion.motion_ID+"=j."+net.ddp2p.common.table.justification.motion_ID+")"+
		//" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
		//" JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE " +
			" m."+net.ddp2p.common.table.motion.organization_ID+"=? "+
			" AND ( m."+net.ddp2p.common.table.motion.organization_ID+"= c."+net.ddp2p.common.table.constituent.organization_ID+"  OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
			" AND j."+net.ddp2p.common.table.justification.arrival_date+">? " +
			" AND j."+net.ddp2p.common.table.justification.arrival_date+"<=? "+
			//" AND o."+table.organization.broadcasted+" <> '0' "+
			" AND ( c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0'  OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
			" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
			" AND j."+net.ddp2p.common.table.justification.broadcasted+" <> '0' "+
			//" AND j."+table.justification.signature+" IS NOT NULL "+
			" AND j."+net.ddp2p.common.table.justification.temporary+" == '0' "+
			" AND j."+net.ddp2p.common.table.justification.global_justification_ID+" IS NOT NULL "+
			" ORDER BY j."+net.ddp2p.common.table.justification.arrival_date
		;
	public static ArrayList<String> getJustificationHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if (DEBUG) System.out.println("JustificationHandling: getJustHashes: start");
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.getDB().select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}

	private static final String sql_justification_data=
			"SELECT "+
					//Util.setDatabaseAlias(table.justification.fields,"j")+
					"j."+net.ddp2p.common.table.justification.justification_ID +
			//", c."+table.constituent.global_constituent_ID+
			//", m."+table.motion.global_motion_ID+
//			", a."+table.justification.global_justification_ID+
			//", o."+table.organization.global_organization_ID+
			//", o."+table.organization.organization_ID+
			" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "+
			" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=j."+net.ddp2p.common.table.justification.constituent_ID+")"+
			" LEFT JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(m."+net.ddp2p.common.table.motion.motion_ID+"=j."+net.ddp2p.common.table.justification.motion_ID+")"+
//			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
				" WHERE " +
				//" j."+table.justification.signature+" IS NOT NULL "+
				" j."+net.ddp2p.common.table.justification.temporary+" == '0' "+
				" AND j."+net.ddp2p.common.table.justification.broadcasted+" <> '0' "+
				" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
				" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
				" AND ( c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0'  OR c."+net.ddp2p.common.table.constituent.constituent_ID+" ISNULL ) "+
				" AND j."+net.ddp2p.common.table.justification.arrival_date+">? ";
	
	public static D_Justification[] getJustificationData(
			ASNSyncRequest asr, String last_sync_date, String org_gid,
			String org_id, String[] __maxDate) throws P2PDDSQLException {
		
		D_Justification[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("JustificationHandling:getJustificationData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql= sql_justification_data +
				((_maxDate!=null)?" AND j."+net.ddp2p.common.table.justification.arrival_date+"<=? ":"")
				+" AND m."+net.ddp2p.common.table.motion.organization_ID+"=? "
				+" ORDER BY j."+net.ddp2p.common.table.justification.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.getDB().select(sql, params, DEBUG);

		if(w.size()>0) {
			result = new D_Justification[w.size()];
			for(int k=0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				//result[k] = D_Justification.getJustByLID(Util.getString(s.get(table.justification.J_ID)), true, false);
				result[k] = D_Justification.getJustByLID(Util.getString(s.get(0)), true, false);
			}
		}
		
		if(DEBUG) out.println("JustificationHandling:getJustificationData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}

	public static boolean integrateNewData(D_Justification[] justifications,
			String orgGID, String org_local_ID, String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if (justifications == null) return false;
		long p_mLID = -1;
		for (int k = 0; k < justifications.length; k ++) {
			justifications[k].setOrgGID(orgGID);
			justifications[k].setOrganizationLIDstr(org_local_ID);
			D_Justification j = D_Justification.getJustByGID(justifications[k].getGID(), true, true, true, __peer, Util.lval(org_local_ID), p_mLID, justifications[k]);
			j.loadRemote(justifications[k], sol_rq, new_rq, __peer);
			j.storeRequest();
			j.releaseReference();
			//justifications[k].store(sol_rq, new_rq);
		}
		return justifications.length>0;
	}

//	public static String getJustificationLocalID(String global_justification_ID) throws P2PDDSQLException {
//		String sql = "SELECT "+table.justification.justification_ID+" FROM "+table.justification.TNAME+
//		" WHERE "+table.justification.global_justification_ID+"=?;";
//		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_justification_ID}, DEBUG);
//		if(o.size()==0) return null;
//		return Util.getString(o.get(0).get(0));
//	}

}

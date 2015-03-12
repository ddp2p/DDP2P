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
import data.D_Justification;

import util.Util;

public class JustificationHandling {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;

	public static String getNextJustificationDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitJustLow) throws P2PDDSQLException {
		if(DEBUG) out.println("JustificationHandling:getNextJustificationDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if(ofi==null){
			String sql=
				"SELECT j."+table.justification.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.justification.TNAME+" AS j "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"
				+" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+")"
				+" WHERE " +
				" j."+table.justification.signature+" IS NOT NULL "+
				" AND j."+table.justification.broadcasted+" <> '0' "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
						" AND j."+table.justification.arrival_date+">? " +
				((_maxDate!=null)?" AND j."+table.justification.arrival_date+"<=? ":"")
				+" ORDER BY j."+table.justification.arrival_date
						+" LIMIT "+(1+limitJustLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.db.select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
			
		}else{
			String sql=
				"SELECT j."+table.justification.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.justification.TNAME+" AS j "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"
				+" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+")"
				+" WHERE " +
				" j."+table.justification.signature+" IS NOT NULL "+
				" AND j."+table.justification.broadcasted+" <> '0' "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
						" AND j."+table.justification.arrival_date+">? " +
				((_maxDate!=null)?" AND j."+table.justification.arrival_date+"<=? ":"")
				+" AND c."+table.constituent.organization_ID+"=? "
				+" ORDER BY j."+table.justification.arrival_date
				+" LIMIT "+(1+limitJustLow)
						+";";
			String orgID = ""+D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash);
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.db.select(sql, params, DEBUG);
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
	static String sql_get_hashes=
		"SELECT j."+table.justification.global_justification_ID+
		" FROM "+table.justification.TNAME+" AS j "+
		" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"+
		" JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"+
		//" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
		//" JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE " +
			" m."+table.motion.organization_ID+"=? "+
			" AND m."+table.motion.organization_ID+"= c."+table.constituent.organization_ID+
			" AND j."+table.justification.arrival_date+">? " +
			" AND j."+table.justification.arrival_date+"<=? "+
			//" AND o."+table.organization.broadcasted+" <> '0' "+
			" AND c."+table.constituent.broadcasted+" <> '0' "+
			" AND m."+table.motion.broadcasted+" <> '0' "+
			" AND j."+table.justification.broadcasted+" <> '0' "+
			" AND j."+table.justification.signature+" IS NOT NULL "+
			" AND j."+table.justification.global_justification_ID+" IS NOT NULL "+
			" ORDER BY j."+table.justification.arrival_date
		;
	public static ArrayList<String> getJustificationHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}

	public static D_Justification[] getJustificationData(
			ASNSyncRequest asr, String last_sync_date, String org_gid,
			String org_id, String[] __maxDate) throws P2PDDSQLException {
		
		D_Justification[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("JustificationHandling:getJustificationData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql=
			"SELECT "+Util.setDatabaseAlias(table.justification.fields,"j")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", a."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.justification.TNAME+" AS j "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
				" WHERE " +
				" j."+table.justification.signature+" IS NOT NULL "+
				" AND j."+table.justification.broadcasted+" <> '0' "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
				" AND j."+table.justification.arrival_date+">? " +
				((_maxDate!=null)?" AND j."+table.justification.arrival_date+"<=? ":"")
				+" AND m."+table.motion.organization_ID+"=? "
				+" ORDER BY j."+table.justification.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.db.select(sql, params, DEBUG);

		if(w.size()>0) {
			result = new D_Justification[w.size()];
			for(int k=0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = new D_Justification(s);
			}
		}
		
		if(DEBUG) out.println("JustificationHandling:getJustificationData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}

	public static boolean integrateNewData(D_Justification[] justifications,
			String orgGID, String org_local_ID, String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		if(justifications==null) return false;
		for(int k=0; k<justifications.length; k++) {
			justifications[k].global_organization_ID = orgGID;
			justifications[k].organization_ID = org_local_ID;
			justifications[k].store(sol_rq, new_rq);
		}
		return justifications.length>0;
	}

	public static String getJustificationLocalID(String global_justification_ID) throws P2PDDSQLException {
		String sql = "SELECT "+table.justification.justification_ID+" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.global_justification_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_justification_ID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}

}

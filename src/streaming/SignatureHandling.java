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
import data.D_Vote;
import data.D_Justification;

import util.Util;

public class SignatureHandling {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;

	public static String getNextSignatureDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitVoteLow) throws P2PDDSQLException {
		if(DEBUG) out.println("SignatureHandling:getNextSignatureDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if(ofi==null){
			String sql=
				"SELECT v."+table.signature.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.signature.TNAME+" AS v "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"
				+" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"
				+" WHERE " +
				" v."+table.signature.signature+" IS NOT NULL "+
//				" AND j."+table.justification.broadcasted+" <> '0' "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
						" AND v."+table.signature.arrival_date+">? " +
				((_maxDate!=null)?" AND v."+table.signature.arrival_date+"<=? ":"")
				+" ORDER BY v."+table.signature.arrival_date
						+" LIMIT "+(1+limitVoteLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.db.select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
			
		}else{
			String sql=
				"SELECT v."+table.signature.arrival_date+", c."+table.constituent.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.signature.TNAME+" AS v "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"
				+" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"
				+" WHERE " +
				" v."+table.signature.signature+" IS NOT NULL "+
//				" AND j."+table.justification.broadcasted+" <> '0' "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
						" AND v."+table.signature.arrival_date+">? " +
				((_maxDate!=null)?" AND v."+table.signature.arrival_date+"<=? ":"")
				+" AND c."+table.constituent.organization_ID+"=? "
				+" ORDER BY v."+table.signature.arrival_date
				+" LIMIT "+(1+limitVoteLow)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.db.select(sql, params, DEBUG);
			if(w.size()>0)
				orgs.add(Util.getString(orgID));
		}
		if(w.size() > limitVoteLow) {
			_maxDate = Util.getString(w.get(limitVoteLow-1).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("SignatureHandling: getNextSignatureDate: limits reached: "+limitVoteLow+" date="+_maxDate);
		}
		
		if(DEBUG) out.println("SignatureHandling:getNextSignatureDate: end: "+_maxDate);
		return _maxDate;
	}
	static String sql_get_hashes=
		"SELECT v."+table.signature.global_signature_ID+
		" FROM "+table.signature.TNAME+" AS v "+
		" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
		" JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
		//" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
		//" JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE " +
			" c."+table.constituent.organization_ID+"=? "+
			" AND c."+table.constituent.organization_ID+"= m."+table.motion.organization_ID+
			" AND v."+table.signature.arrival_date+">? " +
			" AND v."+table.signature.arrival_date+"<=? "+
			//" AND o."+table.organization.broadcasted+" <> '0' "+
			" AND c."+table.constituent.broadcasted+" <> '0' "+
			" AND m."+table.motion.broadcasted+" <> '0' "+
			" AND v."+table.signature.signature+" IS NOT NULL "+
			" AND v."+table.signature.global_signature_ID+" IS NOT NULL "+
//			" AND j."+table.justification.broadcasted+" <> '0' "+
			" ORDER BY v."+table.signature.arrival_date
		;
	public static ArrayList<String> getSignatureHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}

	public static D_Vote[] getSignaturesData(ASNSyncRequest asr,
			String last_sync_date, String org_gid, String org_id,
			String[] __maxDate) throws P2PDDSQLException {
		
		D_Vote[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("SignatureHandling:getSignatureData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql=
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", j."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
				" WHERE " +
				" v."+table.signature.signature+" IS NOT NULL "+
//				" AND j."+table.justification.broadcasted+" <> '0' "+
				" AND m."+table.motion.broadcasted+" <> '0' "+
				" AND o."+table.organization.broadcasted+" <> '0' "+
				" AND c."+table.constituent.broadcasted+" <> '0' "+
				" AND v."+table.signature.arrival_date+">? " +
				((_maxDate!=null)?" AND v."+table.signature.arrival_date+"<=? ":"")
				+" AND c."+table.constituent.organization_ID+"=? "
				+" ORDER BY v."+table.signature.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.db.select(sql, params, DEBUG);

		if(w.size()>0) {
			result = new D_Vote[w.size()];
			for(int k=0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = new D_Vote(s);
			}
		}
		
		if(DEBUG) out.println("SignatureHandling:getSignatureData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}

	public static boolean integrateNewData(D_Vote[] signatures, String org_GID,
			String org_local_ID, String arrival_time, D_Organization orgData, RequestData rq) throws P2PDDSQLException {
		if(DEBUG) out.println("SignatureHandling:integrateNewData: start: #"+signatures);
		if(signatures==null) {
			if(DEBUG) out.println("SignatureHandling:integrateNewData: none available");
			return false;
		}
		for(int k=0; k<signatures.length; k++) {
			if(DEBUG) out.println("SignatureHandling:integrateNewData: doing["+k+"]: #"+signatures[k]);
			signatures[k].global_organization_ID = org_GID;
			signatures[k].organization_ID = org_local_ID;
			signatures[k].store(rq);
		}
		if(DEBUG) out.println("SignatureHandling:integrateNewData: done for:"+signatures.length);
		return signatures.length>0;
	}
}

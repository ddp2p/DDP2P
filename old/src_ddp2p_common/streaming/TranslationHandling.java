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
import data.D_Peer;
import data.D_Vote;
import data.D_Translations;
import util.Util;

public class TranslationHandling {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;

	public static boolean integrateNewData(D_Translations[] translations,
			String global_organization_ID, String org_local_ID,
			String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if(translations==null) return false;
		for(int k=0; k<translations.length; k++) {
			translations[k].global_organization_ID = global_organization_ID;
			translations[k].organization_ID = org_local_ID;
			translations[k].store(sol_rq, new_rq, __peer);
		}
		return translations.length>0;
	}

	static String sql_get_hashes=
		"SELECT t."+table.translation.global_translation_ID
		+" FROM "+table.translation.TNAME+" AS t "+
		" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=t."+table.translation.submitter_ID+") "+
		//" JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=t."+table.translation.organization_ID+") "+
			" WHERE " +
			" t."+table.translation.organization_ID+"=? "+
			" AND t."+table.translation.organization_ID+"= c."+table.constituent.organization_ID+
			" AND t."+table.translation.arrival_date+">? " +
			" AND t."+table.translation.arrival_date+"<=? "+
			//" AND o."+table.organization.broadcasted+" <> '0' " +
			" AND c."+table.constituent.broadcasted+" <> '0' " +
			" AND t."+table.translation.signature+" IS NOT NULL "+
			" AND t."+table.translation.global_translation_ID+" IS NOT NULL "+
			" ORDER BY t."+table.translation.arrival_date
		;
	public static ArrayList<String> getTranslationHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}

	public static D_Translations[] getTranslationData(ASNSyncRequest asr,
			String last_sync_date, String org_gid, String org_id,
			String[] __maxDate) throws P2PDDSQLException {
		
		D_Translations[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("TranslationHandling:getTranslationData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql=
			"SELECT "
			+ Util.setDatabaseAlias(table.translation.fields, "t")+" "
			+", c."+table.constituent.global_constituent_ID
			+", o."+table.organization.global_organization_ID
			+" FROM "+table.translation.TNAME+" AS t "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=t."+table.translation.submitter_ID+") "+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=t."+table.translation.organization_ID+") "+
				" WHERE t."+table.translation.arrival_date+">? " +
				 " AND c."+table.constituent.broadcasted+" <> '0' " +
				 " AND o."+table.organization.broadcasted+" <> '0' " +
				((_maxDate!=null)?" AND t."+table.translation.arrival_date+"<=? ":"")
				+" AND t."+table.translation.organization_ID+"=? "
				+" AND t."+table.translation.signature+" IS NOT NULL "
				+" ORDER BY t."+table.translation.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.db.select(sql, params, DEBUG);

		if(w.size()>0) {
			result = new D_Translations[w.size()];
			for(int k=0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = new D_Translations(s);
			}
		}
		
		if(DEBUG) out.println("TranslationHandling:getTranslationData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}

	public static String getNextTranslationDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitTransLow) throws P2PDDSQLException {
		if(DEBUG) out.println("TranslationHandling:getNextTranslationDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if(ofi==null){
			String sql=
				"SELECT t."+table.translation.arrival_date+", t."+table.translation.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.translation.TNAME+" AS t "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=t."+table.translation.submitter_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=t."+table.translation.organization_ID+")"
				+" WHERE t."+table.translation.arrival_date+">? " +
				 " AND c."+table.constituent.broadcasted+" <> '0' " +
				 " AND o."+table.organization.broadcasted+" <> '0' " +
				((_maxDate!=null)?" AND t."+table.translation.arrival_date+"<=? ":"")
				+" ORDER BY t."+table.translation.arrival_date
						+" LIMIT "+(1+limitTransLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.db.select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
			
		}else{
			String sql=
				"SELECT t."+table.translation.arrival_date+", t."+table.translation.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.translation.TNAME+" AS t "
				+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=t."+table.translation.submitter_ID+")"
				+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=t."+table.translation.organization_ID+")"
				//+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				+" WHERE t."+table.translation.arrival_date+">? " +
				 " AND c."+table.constituent.broadcasted+" <> '0' " +
				 " AND o."+table.organization.broadcasted+" <> '0' " +
				((_maxDate!=null)?" AND t."+table.translation.arrival_date+"<=? ":"")
				+" AND t."+table.translation.organization_ID+"=? "
				+" ORDER BY t."+table.translation.arrival_date
				+" LIMIT "+(1+limitTransLow)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.db.select(sql, params, DEBUG);
			if(w.size()>0)
				orgs.add(Util.getString(orgID));
		}
		if(w.size() > limitTransLow){
			_maxDate = Util.getString(w.get(limitTransLow-1).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("TranslationHandling: getNextTranslationDate: limits reached: "+limitTransLow+" date="+_maxDate);
		}
		
		if(DEBUG) out.println("TranslationHandling:getNextTranslationDate: end: "+_maxDate);
		return _maxDate;
	}

}

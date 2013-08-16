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
import data.D_Vote;
import data.D_News;

import util.Util;

public
class NewsHandling {
	
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT = 5;
	private static final int BIG_LIMIT = 500;
	
	public static void main(String[] args) {
		System.out.println("Got: "+args.length);
		System.out.println("Got 1: "+args[0]);
		System.out.println("Got 2: "+args[1]);
		byte[] b0 = util.Util.byteSignatureFromString(args[0]);
		System.out.println(util.Util.byteToHexDump(b0, 16));
		byte[] b1 = util.Util.byteSignatureFromString(args[1]);
		System.out.println(util.Util.byteToHexDump(b1, 16));
		if(b0.length!=b1.length) return;
		
		byte[] result = new byte[b0.length];
		for(int k=0; k<b0.length; k++) result[k] = (byte)(b0[k]^b1[k]);
		System.out.println(util.Util.byteToHexDump(result, 16));
		System.out.println(util.Util.byteToHex(result, ""));
	}

	public static boolean integrateNewData(D_News[] news,
			String global_organization_ID, String org_local_ID,
			String arrival_time, D_Organization orgData, RequestData rq) throws P2PDDSQLException {
		if(news==null) return false;
		for(int k=0; k<news.length; k++) {
			news[k].global_organization_ID = global_organization_ID;
			news[k].organization_ID = org_local_ID;
			news[k].store(rq);
		}
		return news.length>0;
	}

	public static String getNextNewsDate(String last_sync_date, String _maxDate,
			OrgFilter ofi, HashSet<String> orgs, int limitNewsLow) throws P2PDDSQLException {
		if(DEBUG) out.println("TranslationHandling:getNextTranslationDate: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		if(ofi==null){
			String sql=
				"SELECT n."+table.news.arrival_date+", n."+table.news.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.news.TNAME+" AS n "
				//+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				+" WHERE n."+table.news.arrival_date+">? " +
				 " AND n."+table.news.blocked+" <> '1' " +
				((_maxDate!=null)?" AND n."+table.news.arrival_date+"<=? ":"")
				+" ORDER BY n."+table.news.arrival_date
						+" LIMIT "+(1+limitNewsLow)+
						";";
			if(_maxDate==null) params = new String[]{last_sync_date};
			else params = new String[]{last_sync_date, _maxDate};
			
			w = Application.db.select(sql, params, DEBUG);
			for(ArrayList<Object> s: w) {
				orgs.add(Util.getString(s.get(1)));
			}
			
		}else{
			String sql=
				"SELECT n."+table.news.arrival_date+", n."+table.news.organization_ID//", w."+table.witness.source_ID+
				+" FROM "+table.news.TNAME+" AS n "
				//+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+")"
				+" WHERE n."+table.news.arrival_date+">? " +
				 " AND n."+table.news.blocked+" <> '1' " +
				((_maxDate!=null)?" AND n."+table.news.arrival_date+"<=? ":"")
				+" AND n."+table.news.organization_ID+"=? "
				+" ORDER BY n."+table.news.arrival_date
				+" LIMIT "+(1+limitNewsLow)
						+";";
			String orgID = Util.getStringID(D_Organization.getLocalOrgID(ofi.orgGID, ofi.orgGID_hash));
			if(_maxDate==null) params = new String[]{last_sync_date, orgID};
			else params = new String[]{last_sync_date, _maxDate, orgID};
			w = Application.db.select(sql, params, DEBUG);
			if(w.size()>0)
				orgs.add(Util.getString(orgID));
		}
		if(w.size() > limitNewsLow) {
			_maxDate = Util.getString(w.get(limitNewsLow-1).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("NewsHandling: getNextNewsDate: limits reached: "+limitNewsLow+" date="+_maxDate);
		}
		
		if(DEBUG) out.println("TranslationHandling:getNextTranslationDate: end: "+_maxDate);
		return _maxDate;
	}
	static String sql_get_hashes=
		"SELECT n."+table.news.global_news_ID+
		" FROM "+table.news.TNAME+" AS n "+
		" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=n."+table.news.constituent_ID+")"+
		//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.news.organization_ID+")"+
		" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=n."+table.news.motion_ID+")"+
			" WHERE " +
			" n."+table.news.organization_ID+"=? "+
			" AND n."+table.news.organization_ID+"= c."+table.constituent.organization_ID+
			" AND n."+table.news.arrival_date+">? " +
			" AND n."+table.news.arrival_date+"<=? "+
			//" AND o."+table.organization.blocked+" <> '1' " +
			" AND c."+table.constituent.blocked+" <> '1' " +
			//" AND m."+table.motion.blocked+" <> '1' " +
			" AND n."+table.news.blocked+" <> '1' " +
			" AND n."+table.news.signature+" IS NOT NULL " +
			" AND n."+table.news.global_news_ID+" IS NOT NULL " +
			" ORDER BY n."+table.news.arrival_date
		;
	public static ArrayList<String> getNewsHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws P2PDDSQLException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else { maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}

	public static D_News[] getNewsData(ASNSyncRequest asr,
			String last_sync_date, String org_gid, String org_id,
			String[] __maxDate) throws P2PDDSQLException {
		
		D_News[] result=null;
		String _maxDate = __maxDate[0];
		if(DEBUG) out.println("MotionHandling:getMotionData: start: between: "+last_sync_date+" : "+_maxDate);
		ArrayList<ArrayList<Object>> w;
		String[] params;
		
		String sql=
			"SELECT "+Util.setDatabaseAlias(table.news.fields,"n")+
			", c."+table.constituent.global_constituent_ID+
			", o."+table.organization.global_organization_ID+
			", m."+table.motion.global_motion_ID+
			" FROM "+table.news.TNAME+" AS n "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=n."+table.news.constituent_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.news.organization_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=n."+table.news.motion_ID+")"+
				" WHERE n."+table.news.arrival_date+">? " +
				 " AND n."+table.news.blocked+" <> '1' " +
				((_maxDate!=null)?" AND n."+table.news.arrival_date+"<=? ":"")
				+" AND n."+table.news.organization_ID+"=? "+
				" AND n."+table.news.signature+" IS NOT NULL " 
				+" ORDER BY n."+table.news.arrival_date
				+" LIMIT "+BIG_LIMIT
						+";";
		
		if(_maxDate==null) params = new String[]{last_sync_date, org_id};
		else params = new String[]{last_sync_date, _maxDate, org_id};
		w = Application.db.select(sql, params, DEBUG);

		if(w.size()>0) {
			result = new D_News[w.size()];
			for(int k=0; k<w.size(); k++) {
				ArrayList<Object> s = w.get(k);
				result[k] = new D_News(s);
			}
		}
		
		if(DEBUG) out.println("MotionHandling:getMotionData: found#= "+((result!=null)?result.length:"null"));
		return result;
	}
}
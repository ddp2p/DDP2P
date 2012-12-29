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

import registration.ASNNeighborhoodOP;

import ASN1.Encoder;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import data.D_Organization;
import data.D_Constituent;
import data.D_Neighborhood;

import util.Util;

public class NeighborhoodHandling {
	static String sql_max=
		"SELECT n."+table.neighborhood.arrival_date+
		", o."+table.organization.global_organization_ID +", n."+table.neighborhood.organization_ID +
		" FROM "+table.neighborhood.TNAME +" AS n "+
		" LEFT JOIN  "+table.organization.TNAME+" as o ON (o."+table.organization.organization_ID+" = n."+table.neighborhood.organization_ID+")  "+
		" WHERE n."+table.neighborhood.signature+" IS NOT NULL " +
		 " AND o."+table.organization.broadcasted+" <> '0' " +
				" AND n."
		+table.neighborhood.arrival_date+">? AND n."+table.neighborhood.arrival_date+"<=?" +
		" ORDER BY n."+table.neighborhood.arrival_date;
	static String sql_nomax=
		"SELECT n."+table.neighborhood.arrival_date+
		", o."+table.organization.global_organization_ID +", n."+table.neighborhood.organization_ID +
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" LEFT JOIN  "+table.organization.TNAME+" as o ON (o."+table.organization.organization_ID+" = n."+table.neighborhood.organization_ID+")  "+
		" WHERE n."+table.neighborhood.signature+" IS NOT NULL " +
		 " AND o."+table.organization.broadcasted+" <> '0' " +
				" AND n."
		+table.neighborhood.arrival_date+">?" +
		" ORDER BY n."+table.neighborhood.arrival_date;

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	public static String getNextNeighborhoodDate(String last_sync_date,
			String _maxDate, OrgFilter ofi, HashSet<String> orgs, int limitNeighLow) throws SQLiteException {
		if(DEBUG) out.println("OrgHandling:getNextNeighborhoodDate: start: between: "+last_sync_date+" : "+_maxDate);
		String sql = (_maxDate==null)?sql_nomax:sql_max;
		String[] param=(_maxDate==null)?new String[]{last_sync_date}:new String[]{last_sync_date, _maxDate};
		ArrayList<ArrayList<Object>> n = Application.db.select(sql+" ASC LIMIT "+(1+limitNeighLow), param, DEBUG);

		for (ArrayList<Object> ae : n) {
			orgs.add(Util.getString(ae.get(2)));
		}
		if(n.size() <= limitNeighLow) return _maxDate;



		_maxDate = Util.getString(n.get(n.size()-1).get(0));
		if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("NeighborhoodHandling: getNeighborhoodOPsDate: limits reached: "+limitNeighLow+" date="+_maxDate);
		return _maxDate;
	}
	static String sql_get_hashes=
		"SELECT n."+table.neighborhood.global_neighborhood_ID+
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" JOIN "+table.constituent.TNAME+" AS c ON(n."+table.neighborhood.submitter_ID+"=c."+table.constituent.constituent_ID+") "+
		//" LEFT JOIN "+table.neighborhood.TNAME+" AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "+
		//" JOIN "+table.organization.TNAME+" AS o ON(n."+table.neighborhood.organization_ID+"=o."+table.organization.organization_ID+") "+
		" WHERE " +
		" n."+table.neighborhood.organization_ID+"=? "+ 
		" AND n."+table.neighborhood.organization_ID+"= c."+table.constituent.organization_ID 
		+" AND n."+table.neighborhood.arrival_date+">? "
		+" AND n."+table.neighborhood.arrival_date+"<=? "
		//+" AND o."+table.organization.broadcasted+" <> '0' "
		+" AND c."+table.constituent.broadcasted+" <> '0' "
		+" AND n."+table.neighborhood.signature+" IS NOT NULL "
		+" AND n."+table.neighborhood.global_neighborhood_ID+" IS NOT NULL "
		+" ORDER BY n."+table.neighborhood.arrival_date
		;
	public static ArrayList<String> getNeighborhoodHashes(String last_sync_date, String org_id, String[] _maxDate, int BIG_LIMIT) throws SQLiteException {
		String maxDate;
		if((_maxDate==null)||(_maxDate.length<1)||(_maxDate[0]==null)) maxDate = Util.getGeneralizedTime();
		else{ maxDate = _maxDate[0]; if((_maxDate!=null)&&(_maxDate.length>0)) _maxDate[0] = maxDate;}
		ArrayList<ArrayList<Object>> result = Application.db.select(sql_get_hashes+" LIMIT "+BIG_LIMIT+";",
				new String[]{org_id, last_sync_date, maxDate}, DEBUG);
		return Util.AL_AL_O_2_AL_S(result);
	}

	public static ASNNeighborhoodOP[] getNeighborhoodOPs(ASNSyncRequest asr,
			String last_sync_date, String gid, String org_id, String[] _maxDate) throws SQLiteException {
		if(DEBUG) out.println("NeighborhoodHandling:getNeighborhoodOPs: from="+last_sync_date+" to="+_maxDate[0]+" for orgID="+org_id+" orgGID="+gid);
		ASNNeighborhoodOP[] result = null;
		ArrayList<ArrayList<Object>> a;
		String _sql= "SELECT "+table.neighborhood.global_neighborhood_ID+","+table.neighborhood.name+","+table.neighborhood.address+
		","+table.neighborhood.creation_date+","+table.neighborhood.names_subdivisions+","+table.neighborhood.signature+
		","+table.neighborhood.submitter_ID+","+table.neighborhood.name_division+","+table.neighborhood.fields_neighborhoods+
		","+table.neighborhood.parent_nID+
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" LEFT JOIN "+table.constituent.TNAME+" AS c ON(n."+table.neighborhood.submitter_ID+"=c."+table.constituent.constituent_ID+") "+
		" WHERE n."+table.neighborhood.organization_ID+"=? AND n."+table.neighborhood.arrival_date+">? "+
		 " AND o."+table.organization.broadcasted+" <> '0' "+
		 " AND c."+table.constituent.broadcasted+" <> '0' "
		;
		String sql = "SELECT  "+Util.setDatabaseAlias(table.neighborhood.fields_neighborhoods, "n")+
		", p."+table.neighborhood.global_neighborhood_ID+
		", c."+table.constituent.global_constituent_ID+
		", o."+table.organization.global_organization_ID+
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" LEFT JOIN "+table.constituent.TNAME+" AS c ON(n."+table.neighborhood.submitter_ID+"=c."+table.constituent.constituent_ID+") "+
		" LEFT JOIN "+table.neighborhood.TNAME+" AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "+
		" LEFT JOIN "+table.organization.TNAME+" AS o ON(n."+table.neighborhood.organization_ID+"=o."+table.organization.organization_ID+") "+
		" WHERE n."+table.neighborhood.organization_ID+"=? AND n."+table.neighborhood.arrival_date+">? "+
		 " AND o."+table.organization.broadcasted+" <> '0' "+
		 " AND c."+table.constituent.broadcasted+" <> '0' "
			+" AND n."+table.neighborhood.signature+" IS NOT NULL "
			+" AND n."+table.neighborhood.global_neighborhood_ID+" IS NOT NULL "
		;

		String order = " ORDER BY n."+table.neighborhood.arrival_date+";";
		if (_maxDate[0]==null) {
			a = Application.db.select(sql+order, new String[]{org_id,last_sync_date}, DEBUG);
		} else {
			String cond = " AND n."+table.neighborhood.arrival_date+"<=? ";
			a = Application.db.select(sql+cond+order, new String[]{org_id,last_sync_date, _maxDate[0]}, DEBUG);			
		}
		if(a.size()==0) return null;

		result = new ASNNeighborhoodOP[a.size()];
		for (int k=0; k<result.length; k++) 
			result[k]=getNeighborhoodOP(a.get(k));
		if(DEBUG) out.println("NeighborhoodHandling:getNeighborhoodOPs: neighborhoods="+((result!=null)?result.length:"null"));
		return result;
	}

	private static ASNNeighborhoodOP getNeighborhoodOP(ArrayList<Object> N) {
		ASNNeighborhoodOP result = new ASNNeighborhoodOP();
		result.neighborhood = D_Neighborhood.getNeighborhood(N);
		return result;
	}
	

	public static boolean integrateNewData(ASNNeighborhoodOP[] neighborhoods,
			String orgGID, String org_local_ID, String arrival_time, D_Organization orgData, RequestData rq) throws SQLiteException {
		if(neighborhoods == null) return false;
		boolean result = false;
		for(int k=0; k<neighborhoods.length; k++) {
			String submit_ID;
			boolean existingConstituentSigned[] = new boolean[]{false};
			if(neighborhoods[k].neighborhood.submitter_ID !=null) submit_ID = neighborhoods[k].neighborhood.submitter_ID;
			else submit_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(neighborhoods[k].neighborhood.submitter_global_ID,existingConstituentSigned);

			if(submit_ID==null){
				submit_ID = ""+D_Constituent.insertTemporaryConstituentGID(neighborhoods[k].neighborhood.submitter_global_ID, org_local_ID);
				rq.cons.put(neighborhoods[k].neighborhood.submitter_global_ID, DD.EMPTYDATE);
			}else{
				if(rq.cons.contains(neighborhoods[k].neighborhood.submitter_global_ID)) {
					if((existingConstituentSigned[0]) || (1==D_Constituent.isGID_or_Hash_available(neighborhoods[k].neighborhood.submitter_global_ID, DEBUG)))//not temporary
						rq.cons.remove(neighborhoods[k].neighborhood.submitter_global_ID);
				}
			}
			neighborhoods[k].neighborhood.submitter_ID = submit_ID;

			
			if(neighborhoods[k].neighborhood.parent_global_ID!=null) {
				boolean existingNeighborhoodSigned[] = new boolean[]{false};
				String parent_neighborhood_ID;
				String parent_neighborhood_GID = neighborhoods[k].neighborhood.parent_global_ID;
				if(neighborhoods[k].neighborhood.parent_ID != null) parent_neighborhood_ID = neighborhoods[k].neighborhood.parent_ID;
				else parent_neighborhood_ID = D_Neighborhood.getNeighborhoodLocalID(parent_neighborhood_GID, existingNeighborhoodSigned);

				if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: no local parent: \n "+neighborhoods[k].neighborhood);

				if(parent_neighborhood_ID == null) {
					if(parent_neighborhood_GID != null) {
						if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: will temp neigh: \n "+neighborhoods[k].neighborhood);
						parent_neighborhood_ID = D_Neighborhood.insertTemporaryNeighborhoodGID(parent_neighborhood_GID, org_local_ID);
						rq.neig.add(parent_neighborhood_GID);
					}
				}else{
					if(rq.neig.contains(neighborhoods[k].neighborhood.parent_global_ID)) {
						if((existingNeighborhoodSigned[0]) || (1==D_Neighborhood.isGIDavailable(neighborhoods[k].neighborhood.parent_global_ID, DEBUG))){						
							if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: available: \n "+neighborhoods[k].neighborhood);
							rq.neig.remove(neighborhoods[k].neighborhood.parent_global_ID);
						}else{
							if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: un-available: \n "+neighborhoods[k].neighborhood);							
						}
					}else{
						if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: needed???: \n "+neighborhoods[k].neighborhood);						
					}
				}
				neighborhoods[k].neighborhood.parent_ID = parent_neighborhood_ID;
			}else{
				if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: no parent for: \n "+neighborhoods[k].neighborhood);
			}

			if(DEBUG) System.out.println("\nNeighborhoodHandling: integrateNewData: will integrate: \n "+neighborhoods[k].neighborhood);						
			rq.neig.remove(neighborhoods[k].neighborhood.global_neighborhood_ID);
			result |= integrateNewNeighborhoodOPData(neighborhoods[k],
					orgGID, org_local_ID, arrival_time, orgData);
		}
		return result;
	}

	private static boolean integrateNewNeighborhoodOPData(
			ASNNeighborhoodOP neighborhood, String orgGID,
			String org_local_ID, String arrival_time, D_Organization orgData) throws SQLiteException {
		if(DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewNeighborhoodData: start on "+neighborhood);
		if(neighborhood == null) return false;
		boolean result = false;
		D_Neighborhood wn = neighborhood.neighborhood;
		wn.global_organization_ID = orgGID;
		wn.organization_ID = Util.lval(org_local_ID,-1);
		result = null!= D_Neighborhood.integrateNewNeighborhoodData(wn, orgGID, org_local_ID, arrival_time, orgData);
		if(DEBUG) System.out.println("NeighborhoodHandling:integrateNewNeighborhoodData: exit");
		return result;
	}

}

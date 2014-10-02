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

import static util.Util.__;
import static java.lang.System.out;
import hds.ASNSyncPayload;
import hds.ASNSyncRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import util.Util;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import data.D_OrgConcepts;
import data.D_OrgDistribution;
import data.D_Organization;
import data.D_OrgParams;
import data.D_Peer;
public class OrgHandling {
	
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	private static int LIMIT_CONSTITUENT_LOW = 5;
	private static int LIMIT_CONSTITUENT_MAX = 200;
	private static int LIMIT_NEIGH_LOW = 10;
	private static int LIMIT_WITNESS_LOW = 10;
	private static int LIMIT_MOTION_LOW = 10;
	private static int LIMIT_JUST_LOW = 10;
	private static int LIMIT_SIGN_LOW = 10;
	private static int LIMIT_VOTE_LOW = 10;
	private static int LIMIT_NEWS_LOW = 10;
	private static int LIMIT_TRANS_LOW = 10;
	private static int LIMIT_HASH_LOW = 100;
	public static final String ORG_PEERS = "-20"; // this is added to HashSet orgs to tell that there are "peers" to add in the time segment
	public static final String ORG_NEWS = "-21";  // this is added to HashSet orgs to tell that there are "news" to add in the time segment
	public static boolean SERVE_DIRECTLY_DATA = false;
	/**
	 * prepare GIDhash
	 * Integrate newly arrived data for one organization
	 * organizationID expected to be already set if org is not blocked
	 * 
	 * Will removed hashes/hash-dates of newly fully received items from _rq
	 * @param od
	 * @param _orgID
	 * @param arrival_time
	 * @return
	 * @throws P2PDDSQLException
	 */
	public	static boolean updateOrg(ASNSyncPayload asa, D_Organization od, String[] _orgID, String arrival_time,
			RequestData _sol_rq, RequestData _new_rq, D_Peer peer) throws P2PDDSQLException {
		boolean DEBUG = true;
		boolean changed=false;
		//String id_hash;
		if (DEBUG) out.println("OrgHandling:updateOrg: enter");
		
		/*
		if((od.signature==null)||(od.signature.length==0)) {
			if(DEBUG) out.println("OrgHandling:updateOrg: no signature, will integrate other data");
			long local_org_ID = D_Organization.getLocalOrgID(od.global_organization_ID);
			if(local_org_ID==-1){
				rq.orgs.add(od.global_organization_ID);
				//local_org_ID = OrgHandling.insertTemporaryOrganizationGID(od.global_organization_ID);
				if(DEBUG) out.println("OrgHandling:updateOrg: fail because the organization is unknown");
				return false;
			}
			integrateOtherOrgData(od, Util.getStringID(local_org_ID), arrival_time, false, rq);
			return false;
		}
		*/
		
		if (od.global_organization_IDhash == null)
			od.global_organization_IDhash = od.getOrgGIDHashFromGID();
		
		boolean _changed[] = new boolean[1];
		if (od.creator != null) {
			if (DEBUG) out.println("OrgHandling:updateOrg: store peer");
			//D_Peer.storeReceived(od.creator, Util.getCalendar(arrival_time), arrival_time);
			//od.creator.setArrivalDate(null, arrival_time);
			D_Peer.storeReceived(od.creator, true, false, arrival_time);
			//if(DEBUG) out.println("OrgHandling:updateOrg: stored peer");
			if (
					(od.creator.component_basic_data.globalID != null)
					&&
					od.creator.component_basic_data.globalID.equals(od.params.creator_global_ID)) {
				od.creator_ID = od.creator.getLIDstr();
				if (DEBUG) out.println("OrgHandling:updateOrg: creator LID="+od.creator_ID);
			}
		}
		long id = -1;
		if (DEBUG) out.println("OrgHandling:updateOrg: decide org storage");
		boolean verified = od.verifySignature();
		boolean anonymous = od.isAnonymous();
		if (
				(verified)
				&&
				(
						(! anonymous)
						||
						DD.ANONYMOUS_ORG_ACCEPTED
				)
			)
		{
			if (DEBUG) out.println("OrgHandling:updateOrg: will store received");
			id = D_Organization.storeRemote(od, _changed, _sol_rq, _new_rq, peer); // should set blocking new orgs
			if (DEBUG || DD.DEBUG_PRIVATE_ORGS)System.out.println("OrgHandling:updateOrg: org changed: ch="+_changed[0]+
					" br="+od.broadcast_rule+" id="+id+" creat="+od.creator_ID);
			if (
					(od.broadcast_rule == false) &&
					(id > 0) &&
					(od.creator_ID != null) &&
					(_changed[0])
					)
			{
				if (DEBUG || DD.DEBUG_PRIVATE_ORGS) System.out.println("OrgHandling:updateOrg: private org changed: auto="+DD.AUTOMATE_PRIVATE_ORG_SHARING);
				if (DD.AUTOMATE_PRIVATE_ORG_SHARING == 0) {
					int r = Application_GUI.ask(__("In this session, do you want to share data of private orgs\n to all those sending it to you?"),
							__("Share private orgs with your providers"), Application_GUI.YES_NO_OPTION);
					if (r == 0) DD.AUTOMATE_PRIVATE_ORG_SHARING = 1;
					else DD.AUTOMATE_PRIVATE_ORG_SHARING = -1;
					if (DEBUG || DD.DEBUG_PRIVATE_ORGS) System.out.println("OrgHandling:updateOrg: sharing: auto:="+DD.AUTOMATE_PRIVATE_ORG_SHARING);
				}
				if (DD.AUTOMATE_PRIVATE_ORG_SHARING == 1)
				{
					D_OrgDistribution.add(Util.getStringID(id), od.creator_ID);
				}
			}
		}
		
		if(DEBUG)System.out.println("OrgHandling:updateOrg: org id="+id+" name="+od.name);
		if (id <= 0) {
			id = D_Organization.getLIDbyGID(od.global_organization_ID);
			if (DEBUG)System.out.println("OrgHandling:updateOrg: org id(GID)="+id+" name="+od.name);
			if (id <= 0) {
				if (od.signature != null) {
					od.blocked = DD.BLOCK_NEW_ARRIVING_ORGS_WITH_BAD_SIGNATURE;
					if(DD.WARN_OF_FAILING_SIGNATURE_ONRECEPTION) {
						String peer_name = __("Unknown");
						if(peer != null){
							peer_name = peer.component_basic_data.name;
							if(peer_name==null) peer_name = Util.trimmed(peer.component_basic_data.globalID);
							if(peer_name == null) peer_name = __("Unknown");
						}
						Application_GUI.warning(
								__("Verification/Storage failure for incoming org:")+od.name+"\n"+
										__("Verified:")+verified+"\n"+
										__("Anonymous:")+anonymous+"\n"+
										__("Arriving from peer:")+peer_name+"\n"+
										__("You can disable such warnings from Control Panel, GUI"),
										__("Failure signature organization"));
						// might have been created while waiting for user
						id = D_Organization.getLIDbyGID(od.global_organization_ID);
					}
				}
				// created blocked on bad signature
				String name = od.name;
				if (id <= 0)
					id = D_Organization.insertTemporaryGID(od.global_organization_ID, od.global_organization_IDhash, od.blocked, od.name, peer);
				if(DEBUG)System.out.println("OrgHandling:updateOrg: tmp id="+id+" name="+name);
			}
			od.setLID(id);
		}
		//if(id>0) od.organization_ID = Util.getStringID(id);
		changed = _changed[0];
		_orgID[0] = od.getLIDstr();
		//changed = (id>0);
		
		if((id > 0)&&(!od.blocked))
			integrateOtherOrgData(od, _orgID[0], arrival_time, changed, _sol_rq, _new_rq, peer);
		if(DEBUG) out.println("OrgHandling:updateOrg: return="+changed);
		return changed;
	}
	private static boolean integrateOtherOrgData(D_Organization od, String org_local_ID,
			String arrival_time, boolean recent_org_data,
			RequestData _sol_rq, RequestData new_rq, D_Peer peer) throws P2PDDSQLException {
		boolean result = false;
		if(DEBUG) out.println("OrgHandling:integrateOtherOrgData: changed start="+result);
		result |= streaming.ConstituentHandling.integrateNewData(od.constituents, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq, peer);
		if(DEBUG) out.println("OrgHandling:integrateOtherOrgData: changed cons="+result);
		result |= streaming.NeighborhoodHandling.integrateNewData(od.neighborhoods, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq, peer);
		if(DEBUG) out.println("OrgHandling:integrateOtherOrgData: changed neig="+result);
		result |= streaming.WitnessingHandling.integrateNewData(od.witnesses, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq, peer);
		if(DEBUG) out.println("OrgHandling:integrateOtherOrgData: changed witn="+result);
		result |= streaming.MotionHandling.integrateNewData(od.motions, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq, peer);
		if(DEBUG) out.println("OrgHandling:integrateOtherOrgData: changed moti="+result);
		result |= streaming.JustificationHandling.integrateNewData(od.justifications, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq, peer);
		result |= streaming.SignatureHandling.integrateNewData(od.signatures, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq);
		result |= streaming.TranslationHandling.integrateNewData(od.translations, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq, peer);
		result |= streaming.NewsHandling.integrateNewData(od.news, od.global_organization_ID, org_local_ID, arrival_time, recent_org_data?od:null, _sol_rq, new_rq);
		return result;
	}
	/**
	 * Refines _maxDate to have a single organization or a single operation
	 * 
	 * if _maxDate ==null search from last_sync_date unlimited, 
	 * else search an organization between last_sync_date and _maxDate
	 * // Should integrate search for organization, constituents, etc...
	 * 
	 * @param last_sync_date
	 * @param _maxDate
	 * @param limitOrgMax 
	 * @param limitOrgLow 
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getNextOrgDate(String last_sync_date, String _maxDate, HashSet<String> orgs, int limitOrgLow) throws P2PDDSQLException {
		//boolean DEBUG = true;
		
		if (DEBUG) out.println("OrgHandling:getNextOrgDate: start: between: "+last_sync_date+" : "+_maxDate);
		String sql, result=null;
		if (DEBUG) System.out.println("OrgHandling:getNextOrgDate: enter="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		
		int _LIMIT_CONSTITUENT_LOW;
		int _LIMIT_NEIGH_LOW;
		int _LIMIT_WITNESS_LOW;
		int _LIMIT_MOTION_LOW;
		int _LIMIT_JUST_LOW;
		int _LIMIT_VOTE_LOW;
		int _LIMIT_TRANS_LOW;
		int _LIMIT_NEWS_LOW;
		if (SERVE_DIRECTLY_DATA ) {
			 _LIMIT_CONSTITUENT_LOW=LIMIT_CONSTITUENT_LOW;
			 _LIMIT_NEIGH_LOW=LIMIT_NEIGH_LOW;
			 _LIMIT_WITNESS_LOW=LIMIT_WITNESS_LOW;
			 _LIMIT_MOTION_LOW=LIMIT_MOTION_LOW;
			 _LIMIT_JUST_LOW=LIMIT_JUST_LOW;
			 _LIMIT_VOTE_LOW=LIMIT_VOTE_LOW;
			 _LIMIT_TRANS_LOW=LIMIT_TRANS_LOW;
			 _LIMIT_NEWS_LOW=LIMIT_NEWS_LOW;			
		}else{
			_LIMIT_CONSTITUENT_LOW = LIMIT_HASH_LOW;
			_LIMIT_NEIGH_LOW = LIMIT_HASH_LOW;
			_LIMIT_WITNESS_LOW = LIMIT_HASH_LOW;
			_LIMIT_MOTION_LOW = LIMIT_HASH_LOW;
			_LIMIT_JUST_LOW = LIMIT_HASH_LOW;
			_LIMIT_VOTE_LOW = LIMIT_HASH_LOW;
			_LIMIT_TRANS_LOW = LIMIT_HASH_LOW;
			_LIMIT_NEWS_LOW = LIMIT_HASH_LOW;			
		}
		
		
		ArrayList<ArrayList<Object>>p_data;
		if(_maxDate == null) {
			sql = "SELECT "+table.organization.arrival_date+", COUNT(*), " + table.organization.global_organization_ID+","+ table.organization.organization_ID+
					" FROM "+table.organization.TNAME + " AS o "+
			" WHERE "+table.organization.arrival_date+" > ? " +
			" AND " +table.organization.signature+ " IS NOT NULL "+
			 " AND o."+table.organization.broadcasted+" <> '0' " +
					" GROUP BY "+table.organization.arrival_date+","+table.organization.global_organization_ID+" " +
							" ORDER BY "+table.organization.arrival_date+" LIMIT "+(1+limitOrgLow)+";";
			p_data = Application.db.select(sql, new String[]{last_sync_date}, DEBUG);
		}else{
			sql = "SELECT "+table.organization.arrival_date+", COUNT(*), " + table.organization.global_organization_ID+","+ table.organization.organization_ID+
					" FROM "+table.organization.TNAME + " AS o "+
				" WHERE "+table.organization.arrival_date+" > ? AND "+table.organization.arrival_date+" <= ? " +
				" AND " +table.organization.signature+ " IS NOT NULL "+
				 " AND o."+table.organization.broadcasted+" <> '0' " +
						" GROUP BY "+table.organization.arrival_date+","+table.organization.global_organization_ID+" " +
								" ORDER BY "+table.organization.arrival_date+" LIMIT "+(1+limitOrgLow)+";";
			p_data = Application.db.select(sql, new String[]{last_sync_date,_maxDate},DEBUG);
		}
		if(p_data.size()<=0) result = null;
		else{
			if(DEBUG) out.println("OrgHandling:getNextOrgDate: sz=: "+p_data.size());
			if(p_data.size()>limitOrgLow) {
				result = Util.getString(p_data.get(limitOrgLow-1).get(0));
				if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("TranslationHandling: getNextTranslationDate: limits reached: "+limitOrgLow+" date="+result);
				if(DEBUG) out.println("OrgHandling:getNextOrgDate:>limitOrgLow="+limitOrgLow);
			}
			for (ArrayList<Object> a : p_data) {
				orgs.add(Util.getString(a.get(3)));
			}
			if(DEBUG) out.println("OrgHandling:getNextOrgDate:>limitOrgLow In  getNextOrgDate orgs: "+Util.concat(orgs.toArray(),","));
		}
		if (result == null){
			if(DEBUG) out.println("OrgHandling:getNextOrgDate: In  getNextOrgDate result: "+result);
			result = _maxDate;
		}
		
		// There should be at most LIMIT_CONSTITUENTS new constituents per organization transmitted;
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs pre="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = ConstituentHandling.getConstituentOPsDate(last_sync_date, result, null, orgs, _LIMIT_CONSTITUENT_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs con="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = NeighborhoodHandling.getNextNeighborhoodDate(last_sync_date, result, null, orgs, _LIMIT_NEIGH_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs nei="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = WitnessingHandling.getNextWitnessingDate(last_sync_date, result, null, orgs, _LIMIT_WITNESS_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs wit="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = MotionHandling.getNextMotionDate(last_sync_date, result, null, orgs, _LIMIT_MOTION_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs mot="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = JustificationHandling.getNextJustificationDate(last_sync_date, result, null, orgs, _LIMIT_JUST_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs jus="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = SignatureHandling.getNextSignatureDate(last_sync_date, result, null, orgs, _LIMIT_VOTE_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs sig="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = TranslationHandling.getNextTranslationDate(last_sync_date, result, null, orgs, _LIMIT_TRANS_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs tra="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		result = NewsHandling.getNextNewsDate(last_sync_date, result, null, orgs, _LIMIT_NEWS_LOW);
		if(DEBUG) System.out.println("OrgHandling:getNextOrgDate: orgs news="+result+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		if(DEBUG) out.println("OrgHandling:getNextOrgDate: exit result: "+result);
		if(DEBUG) out.println("**************");
		return result;
	}

	private static String getNextFilteredOrgDate(String last_sync_date, OrgFilter ofi, HashSet<String> orgs) throws P2PDDSQLException {
		if(DEBUG) out.println("\n************\nOrgHandling:getNextFilteredOrgDate filter "+ofi+": "+last_sync_date+" ;");
		String sql, result=null;
		ArrayList<ArrayList<Object>>p_data;
		sql = "SELECT "+table.organization.arrival_date+", COUNT(*) FROM "+table.organization.TNAME + " AS o "+
			" WHERE ( ( "+table.organization.global_organization_ID+" = ? ) OR ( "+table.organization.global_organization_ID_hash+" = ? ) ) " +
			 " AND o."+table.organization.broadcasted+" <> '0' " +
					" AND "+table.organization.arrival_date+" > ? " +
					" GROUP BY "+table.organization.arrival_date+" LIMIT 1;";
		p_data = Application.db.select(sql, new String[]{ofi.orgGID, ofi.orgGID_hash, last_sync_date}, DEBUG);
		if(p_data.size()<=0) result = null;
		else result = Util.getString(p_data.get(0).get(0));
		//if (result == null) result = _maxDate;
		// There should be at most LIMIT_CONSTITUENTS new constituents per organization transmitted;
		result = ConstituentHandling.getConstituentOPsDate(last_sync_date, result, ofi, orgs, LIMIT_CONSTITUENT_LOW);
		result = NeighborhoodHandling.getNextNeighborhoodDate(last_sync_date, result, ofi, orgs, LIMIT_NEIGH_LOW);
		result = WitnessingHandling.getNextWitnessingDate(last_sync_date, result, ofi, orgs, LIMIT_WITNESS_LOW);
		result = MotionHandling.getNextMotionDate(last_sync_date, result, ofi, orgs, LIMIT_MOTION_LOW);
		result = JustificationHandling.getNextJustificationDate(last_sync_date, result, ofi, orgs, LIMIT_JUST_LOW);
		result = SignatureHandling.getNextSignatureDate(last_sync_date, result, ofi, orgs, LIMIT_VOTE_LOW);
		result = TranslationHandling.getNextTranslationDate(last_sync_date, result, ofi, orgs, LIMIT_TRANS_LOW);
		result = NewsHandling.getNextNewsDate(last_sync_date, result, ofi, orgs, LIMIT_NEWS_LOW);
		if(DEBUG) out.println("OrgHandling:getNextFilteredOrgDate filtered result: "+result);
		if(DEBUG) out.println("**************");
		return result;
	}
	
	@Deprecated
	static D_Organization _signableOrgData(String ofi_orgID, String local_id, ArrayList<Object> row) throws P2PDDSQLException {
			if(DEBUG) out.println("\n****************\nOrgHandling:signOrgData: start organization orgID="+ofi_orgID);
		//return new OrgData(row);
		D_Organization od = D_Organization.getEmpty();
		od.global_organization_ID = ofi_orgID;
		od.name = Util.getString(row.get(table.organization.ORG_COL_NAME),null);
		od.params = new D_OrgParams();
		//String languages_l = Util.getString(row.get(table.organization.ORG_COL_LANG));
		//languages_l.split(table.organization.ORG_LANG_SEP);
		//if(languages_l != null) 
		od.params.languages = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_LANG)));
		//else od.params.languages = new String[0];
		od.params.instructions_registration = Util.getString(row.get(table.organization.ORG_COL_INSTRUC_REGIS),null);
		od.params.instructions_new_motions = Util.getString(row.get(table.organization.ORG_COL_INSTRUC_MOTION),null);
		od.params.description = Util.getString(row.get(table.organization.ORG_COL_DESCRIPTION),null);
		//String scoring_l = Util.getString(row.get(table.organization.ORG_COL_SCORES));
		//if(scoring_l!=null)od.params.default_scoring_options = scoring_l.split(table.organization.ORG_SCORE_SEP);
		//else 
		od.params.default_scoring_options = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_SCORES),null));//new String[0];//null;
		od.params.category = Util.getString(row.get(table.organization.ORG_COL_CATEG),null);
		
		try{od.params.certifMethods = Integer.parseInt(Util.getString(row.get(table.organization.ORG_COL_CERTIF_METHODS)));}catch(NumberFormatException e){od.params.certifMethods = 0;}
		od.params.hash_org_alg = Util.getString(row.get(table.organization.ORG_COL_HASH_ALG),null);
		//try{od.params.hash_org = Util.hexToBytes(Util.getString(row.get(table.organization.ORG_COL_HASH)).split(table.organization.ORG_HASH_BYTE_SEP));}catch(Exception e){}
		od.params.creation_time = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_CREATION_DATE)), Util.CalendargetInstance());
		od.params.certificate = Util.byteSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_CERTIF_DATA),null));
		String creator_ID=Util.getString(row.get(table.organization.ORG_COL_CREATOR_ID));
		D_Peer creator = D_Peer.getPeerByLID_NoKeep(creator_ID, true); // for creator one alse needs _served
		if(creator == null) {
			if(DEBUG)Util.printCallPath("No creator:"+creator_ID);
			Application_GUI.warning(Util.__("Missing organization creator, or may have incomplete data. You should create a new one!"), Util.__("Missing organization creator."));
			return null;
		}
		od.params.creator_global_ID = creator.component_basic_data.globalID;
		od.params.orgParam = D_Organization.getOrgParams(local_id);
		
		od.concepts = new D_OrgConcepts();
		/*
		int languages=1;
		if(od.params.languages!=null) {
			languages = od.params.languages.length;
			if(languages < 1) languages =1;
		}
		*/
		od.concepts.name_organization = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_ORG),null));
		//new String[languages];
		od.concepts.name_forum = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_FORUM),null));
		od.concepts.name_motion = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_MOTION),null));
		od.concepts.name_justification = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_JUST),null));
		
		//if(od.concepts.name_organization.length!= od.params.languages);
		//od.concepts.name_forum[0] = Util.getString(row.get(table.organization.ORG_COL_NAME_FORUM),null);
		//od.concepts.name_motion[0] = Util.getString(row.get(table.organization.ORG_COL_NAME_MOTION),null);
		//od.concepts.name_justification[0] = Util.getString(row.get(table.organization.ORG_COL_NAME_JUST),null);
		if(DEBUG) out.println("OrgHandling:signOrgData: result="+od);
		if(DEBUG) out.println("**************");
		return od;
	}	
	
		/*
		od.constituents = ConstituentHandling.getConstituentsModifs(local_id, last_sync_date);
		
		od.translations = new ASNTranslation[0];
		od.witnesses = new ASNWitness[0];
		od.news = new ASNNews[0];
		od.motions = new ASNMotion[0];
		od.justifications = new ASNJustificationSets[0];
		od.signatures = new ASNSignature[0];
		*/
	/*
	static D_Organization getOrgData(String last_sync_date, String ofi_orgGID, ArrayList<Object> row) throws P2PDDSQLException {
		if(DEBUG) out.println("\n****************\nOrgHandling:getOrgData: start organization orgGID="+ofi_orgGID+" from date="+last_sync_date);
		String local_id = Util.getString(row.get(table.organization.ORG_COL_ID),null);
		D_Organization od = D_Organization.signableOrgData(ofi_orgGID, local_id, row);
		if(DD.VERIFY_SENT_SIGNATURES && od.creator!=null){
			if(!od.creator.verifySignature()) {
				SK sk = Util.getStoredSK(od.creator.component_basic_data.globalID);
				if(sk!=null) {
					int a = Application_GUI.ask(
							_("Signature fails for peer:")+od.creator.component_basic_data.name+"\n"+
							_("Do you want to sign it now (YES)?, to not send the peer (NO)?")+"\n"+
							_("or to send it with bad signature (Cancel)?"),
							_("Verifying sent organization Initiator Signature"),
							Application_GUI.YES_NO_CANCEL_OPTION);
					switch(a) {
					case 0: od.creator.sign(sk);od.creator.storeRequest();break;
					case 1: od.creator = null; break;
					default:
					}
				}else{
					int a = Application_GUI.ask(
							_("Signature fails for peer:")+od.creator.component_basic_data.name+"\n"+
							_("Do you want to drop  peer (OK)?")+"\n"+
							_("or to send it with bad signature (Cancel)?"),
							_("Verifying sent organization Initiator Signature"),
							Application_GUI.OK_CANCEL_OPTION);
					switch(a) {
					case 0: od.creator = null; break;
					default:
					}
				}
			}
		}
		od.signature = D_Organization.getSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_SIGN),null));
		od.signature_initiator = D_Organization.getSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_SIGN_INITIATOR),null));
		
		if(DD.VERIFY_SENT_SIGNATURES){
			if(!od.verifySignature()){
				Application_GUI.warning(_("Organization signature fails for:")+od.name, _("Verifying sent organization fails"));
			}
		}
		
		if(DEBUG) System.out.println("OrgHandling:getOrgData: exit with orgData="+od);
		if(DEBUG) out.println("**************");
		return od;
	}
*/
/**
 * @throws P2PDDSQLException 
 * @throws  
 * @throws P2PDDSQLException 
 *  Get the OrgData starting last_sync_data and up to _maxData[0], or find _maxDate[]
 *  such that the data to send is not too large
 * @param asr
 * @param last_sync_date
 * @param _maxDate[0]     output if justData = true; else input
 * @param justDate
 * @param limitOrgMax 
 * @param limitOrgLow 
 * @return data, if justDate id false, else null
 * @throws  
 */
	public static D_Organization[] getOrgData(ASNSyncRequest asr, String last_sync_date, String[] _maxDate, 
			boolean justDate, HashSet<String> orgIDs,
			int limitOrgLow, int limitOrgMax) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(DEBUG) out.println("\n**************\nOrgHandling:getOrgData':start: "+_maxDate[0]+" justDate:"+justDate+" orgs="+orgIDs.size());//+" asr="+asr);
		String maxDate=null;
		if(!justDate) maxDate = _maxDate[0];
		D_Organization[] orgData=null;
		if(asr.orgFilter==null) {
			if(DEBUG) out.println("OrgHandling:getOrgData: No filter!");
			if (justDate) {
				String max_Date = OrgHandling.getNextOrgDate(last_sync_date, _maxDate[0], orgIDs, limitOrgLow);
				if (DEBUG) System.out.println("OrgHandling:getOrgData: orgs :"+Util.nullDiscrimArray(orgIDs.toArray(new String[0])," : "));
				if (max_Date != null) _maxDate[0] = max_Date;
				if (DEBUG) out.println("OrgHandling:getOrgData: Date (after org no filter): "+_maxDate[0]);
				return null;
			}
			
			/*
			 * Next we check which org parameters are in the expected range, and add them to  orgGID_data and orgID_data
			 */
			
			String sql = "SELECT " + table.organization.field_list +
				" FROM "+table.organization.TNAME+ " AS o " +
				" WHERE "+table.organization.arrival_date+" > ? "+
					((maxDate!=null)?("AND "+table.organization.arrival_date+" <= ? "):"") +
					" AND " +table.organization.global_organization_ID+ " IS NOT NULL "+
					 " AND o."+table.organization.broadcasted+" <> '0' " +
				//	" AND " +table.organization.creator_ID+ " IS NOT NULL "+
					" AND " +table.organization.signature+ " IS NOT NULL "+
				" ORDER BY "+table.organization.arrival_date+" LIMIT "+limitOrgMax+";";
			ArrayList<ArrayList<Object>> p_data =
				Application.db.select(sql,
						((maxDate!=null)?(new String[]{last_sync_date, maxDate}):new String[]{last_sync_date}),
						DEBUG);
			if(DEBUG)out.println("OrgHandling:getOrgData: Valid Organizations for sending="+p_data.size());
			
			if ((orgIDs.size()==0) && (p_data.size()==0)){
				if(DEBUG)out.println("OrgHandling:getOrgData: No new data from orgs.");
				return new D_Organization[0];
			}
			Hashtable<String,Integer> orgGID_data = new Hashtable<String, Integer>();
			Hashtable<String,Integer> orgID_data = new Hashtable<String, Integer>();
			if(DEBUG) System.out.println("OrgHandling:getOrgData: orgs pre org="+Util.nullDiscrimArray(orgIDs.toArray(new String[0])," : "));
			for(int k=0; k<p_data.size(); k++) {
				ArrayList<Object> a = p_data.get(k);
				String gid = Util.getString(a.get(table.organization.ORG_COL_GID));
				orgGID_data.put(gid, new Integer(k));
				if(DEBUG) System.out.println("OrgHandling:getOrgData: orgs place gid="+gid+" "+orgGID_data.get(gid));
				String id = Util.getString(a.get(table.organization.ORG_COL_ID));
				orgID_data.put(id, new Integer(k));
				orgIDs.add(id); // add to orgs in strange case it was nor already
			}
			if(DEBUG) System.out.println("OrgHandling:getOrgData: orgs post org="+Util.nullDiscrimArray(orgIDs.toArray(new String[0])," : "));
			
			/*
			 * Orgs with new parameters (ie in orgGID_data) will have all parameters sent,
			 *  others just GID and certif_methods (to reconstruct hash)
			 *  K stores the index in the sql result
			 */
			
			//orgData = new D_Organization[orgIDs.size()]; //p_data.size()];
			ArrayList<D_Organization> _orgData = new ArrayList<D_Organization>();
			//int of = 0;
			Iterator<String> orgIDs_iter = orgIDs.iterator();
			do{ //int of=0; of<orgData.length; of++) {
				D_Organization crt_org;
				String org_id =orgIDs_iter.next();
				if(DEBUG) System.out.println("OrgHandling:getOrgData: orgs try ID="+org_id);
				if(org_id==null) continue;
				/*
				long l_org_id = D_Organization.getLocalOrgID(org_gid);
				String org_id = Util.getStringID(l_org_id);
				*/
//				boolean serving_peer = serving_org_to_peer(org_id, asr.address);
//				if(!serving_peer && privateOrg(org_id)){
//					if(_DEBUG) System.out.println("OrgHandling:getOrgData: not serving="+org_id+" to="+asr.address);
//					continue;
//				}else{
//					if(serving_peer){
//						if(_DEBUG) System.out.println("OrgHandling:getOrgData: serving private orgID="+org_id);
//					}else{
//						if(_DEBUG) System.out.println("OrgHandling:getOrgData: serving non-private orgID="+org_id);
//					}
//				}
				if(!OrgHandling.serving(asr, org_id)) continue;
				String org_gid = D_Organization.getGIDbyLIDstr(org_id);
				if(DEBUG) System.out.println("OrgHandling:getOrgData: orgs try GID="+org_gid);
				Integer K = null;
				if(org_gid!=null) K = orgGID_data.get(org_gid);
				else{
					Util.printCallPath("OrgHandling: null GID for ID:"+org_id);
				}
								
				if(DEBUG) System.out.println("OrgHandling:getOrgData: orgs try K="+K);
				if (K != null) {
					int k = K.intValue();
					//String signature = Util.getString(p_data.get(k).get(table.organization.ORG_COL_SIGN)); // only authoritarian
					//String org_gid = Util.getString(p_data.get(k).get(table.organization.ORG_COL_GID));
					//String org_id = Util.getString(p_data.get(k).get(table.organization.ORG_COL_ID));
					
					crt_org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(org_gid, null, true, false); //OrgHandling.getOrgData(last_sync_date, org_gid, p_data.get(k));
					_orgData.add(crt_org);//[of]
					if(crt_org == null) continue;
					if (DD.STREAM_SEND_ALL_ORG_CREATOR && (crt_org!=null)) {
						try{crt_org.creator = D_Peer.getPeerByGID_or_GIDhash(crt_org.params.creator_global_ID, null, true, false, false, null);}
						catch(Exception e){e.printStackTrace();}
					}
					if(DEBUG) out.println("OrgHandling:getOrgData: Prepared org: "+crt_org);
				}else{
					D_Organization all;
					all = D_Organization.getOrgByGID_or_GIDhash_NoCreate(org_gid, null, true, false); //new D_Organization(org_gid, null, true, false); //false to not load extras
					if(all == null) continue;
					if (!DD.STREAM_SEND_ALL_FUTURE_ORG || all.arrival_date.before(Util.getCalendar(last_sync_date))){
						crt_org = D_Organization.getEmpty();
						_orgData.add(crt_org);//[of]
						crt_org.global_organization_ID = org_gid;
						crt_org.global_organization_IDhash = all.global_organization_IDhash;
						crt_org.params.certifMethods = all.params.certifMethods;
					}else{
						crt_org = all;
						_orgData.add(all);
						if(DD.STREAM_SEND_ALL_ORG_CREATOR) {
							try{all.creator = D_Peer.getPeerByGID_or_GIDhash(all.params.creator_global_ID, null, true, false, false, null);}
							catch(Exception e){e.printStackTrace();}
						}
					}
					//orgData[of].version = table.organization.arrival_date;
				}
				
				if (SERVE_DIRECTLY_DATA) {
					if(DEBUG) out.println("OrgHandling:getOrgData: SERVE_DIRECTLY");
					crt_org.constituents = ConstituentHandling.getConstituentData(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.neighborhoods = NeighborhoodHandling.getNeighborhoodOPs(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.witnesses = WitnessingHandling.getWitnessingData(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.motions = MotionHandling.getMotionData(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.justifications = JustificationHandling.getJustificationData(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.signatures = SignatureHandling.getSignaturesData(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.translations = TranslationHandling.getTranslationData(asr,last_sync_date, org_gid, org_id, _maxDate);
					crt_org.news = NewsHandling.getNewsData(asr,last_sync_date, org_gid, org_id, _maxDate);
				} else {
					//boolean DEBUG = true;
					if(DEBUG) out.println("OrgHandling:getOrgData: SERVE_INDIRECTLY:"+_maxDate[0]);
					//SpecificRequest availableHashes = new SpecificRequest();
					RequestData rq = new RequestData();
					rq.global_organization_ID_hash = crt_org.global_organization_IDhash;
					if(rq.global_organization_ID_hash==null) rq.global_organization_ID_hash = D_Organization.getOrgGIDHashGuess(org_gid);
					int BIG_LIMIT = 300;
					if(DEBUG) out.println("\n\b******OrgHandling:getOrgData: get indirectly");
					rq.cons = ConstituentHandling.getConstituentHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					if(DEBUG) out.println("OrgHandling:getOrgData: got constituents: "+_maxDate[0]+" "+Util.concat(rq.cons,";","null"));
					rq.neig = NeighborhoodHandling.getNeighborhoodHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					if(DEBUG) out.println("OrgHandling:getOrgData: got neighborhoods: "+_maxDate[0]+" "+Util.concat(rq.neig,";","null"));
					rq.witn = WitnessingHandling.getWitnessingHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					if(DEBUG) out.println("OrgHandling:getOrgData: got witnessing: "+_maxDate[0]+" "+Util.concat(rq.witn,";","null"));
					rq.moti = MotionHandling.getMotionHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					rq.just = JustificationHandling.getJustificationHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					rq.sign = SignatureHandling.getSignatureHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					rq.tran = TranslationHandling.getTranslationHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					rq.news = NewsHandling.getNewsHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
					if(DEBUG) out.println("OrgHandling:getOrgData: got advertising: "+_maxDate[0]+" "+rq);
					crt_org.availableHashes = rq;
					maxDate = _maxDate[0];
				}
				crt_org.setLastSyncDate(maxDate);

				if(DEBUG) out.println("OrgHandling:getOrgData: Prepared org components: "+crt_org);
				
				//of++;
			}while(orgIDs_iter.hasNext());
			try{
				_orgData.removeAll(Arrays.asList(new D_Organization[]{null}));
			}catch(Exception e){e.printStackTrace();}
			orgData = _orgData.toArray(new D_Organization[0]);
		}else{
			if(DEBUG)out.println("OrgHandling:getOrgData: Filter length: "+asr.orgFilter.length);
			/*if(justDate){
				_maxDate[0] = OrgHandling.getNextOrgDate(last_sync_date, _maxDate[0]);
				if(DEBUG) out.println("Date after filter: "+_maxDate[0]); return null;
			}*/

			ArrayList<D_Organization> _orgData = new ArrayList<D_Organization>();
			for(int of=0; of<asr.orgFilter.length; of++) {
				OrgFilter ofi = asr.orgFilter[of];
				String max_Date = OrgHandling.getNextFilteredOrgDate(last_sync_date, ofi, orgIDs);
				if(max_Date == null) continue;
				if(_maxDate[0] == null) _maxDate[0] = last_sync_date;
				
				String sql = "SELECT " + table.organization.field_list +
						" FROM "+table.organization.TNAME +
						" WHERE ( "+table.organization.global_organization_ID+" = ? OR "+table.organization.global_organization_ID_hash+" = ? ) " +
								" ORDER BY "+table.organization.arrival_date+" LIMIT 100;";
						//" AND arrival_time > ? AND arrival_time <= ? ORDER BY arrival_time LIMIT 100;";
				
				ArrayList<ArrayList<Object>>p_data =
					Application.db.select(sql, new String[]{ofi.orgGID, ofi.orgGID_hash/*, last_sync_date, max_Date*/}, DEBUG);
				if(p_data.size()>=1) {
					//if(max_Date.equals(last_sync_date) && !Util.getString(p_data.get(0).get(ORG_COL_ARRIVAL)).compareTo(last_sync_date)) continue;
					D_Organization od = //OrgHandling.getOrgData(last_sync_date, ofi.orgGID, p_data.get(0));
							D_Organization.getOrgByGID_or_GIDhash_NoCreate(ofi.orgGID, null, true, false);
					if (od == null) continue;
					od.setLastSyncDate(max_Date);
					_orgData.add(od);
					String org_id = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));

					if(!OrgHandling.serving(asr, org_id)) continue;

					if(SERVE_DIRECTLY_DATA) {
						if(DEBUG) out.println("OrgHandling:getOrgData: SERVE_DIRECTLY");
						od.constituents = ConstituentHandling.getConstituentOPs(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.neighborhoods = NeighborhoodHandling.getNeighborhoodOPs(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.witnesses = WitnessingHandling.getWitnessingData(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.motions = MotionHandling.getMotionData(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.justifications = JustificationHandling.getJustificationData(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.signatures = SignatureHandling.getSignaturesData(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.translations = TranslationHandling.getTranslationData(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.news = NewsHandling.getNewsData(asr,last_sync_date, ofi.orgGID, org_id, _maxDate);
						od.setLastSyncDate(max_Date);
					}else{
						//boolean DEBUG = true;
						if(DEBUG) out.println("OrgHandling:getOrgData: SERVE_INDIRECTLY:"+_maxDate[0]);
						//SpecificRequest availableHashes = new SpecificRequest();
						RequestData rq = new RequestData();
						rq.global_organization_ID_hash = ofi.orgGID_hash; //;orgData[of].global_organization_IDhash;
						if(rq.global_organization_ID_hash==null) rq.global_organization_ID_hash = D_Organization.getOrgGIDHashGuess(ofi.orgGID);
						int BIG_LIMIT = 300;
						if(DEBUG) out.println("\n\b******OrgHandling:getOrgData: get indirectly");
						rq.cons = ConstituentHandling.getConstituentHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						if(DEBUG) out.println("OrgHandling:getOrgData: got constituents: "+_maxDate[0]+" "+Util.concat(rq.cons,";","null"));
						rq.neig = NeighborhoodHandling.getNeighborhoodHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						if(DEBUG) out.println("OrgHandling:getOrgData: got neighborhoods: "+_maxDate[0]+" "+Util.concat(rq.neig,";","null"));
						rq.witn = WitnessingHandling.getWitnessingHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						if(DEBUG) out.println("OrgHandling:getOrgData: got witnessing: "+_maxDate[0]+" "+Util.concat(rq.witn,";","null"));
						rq.moti = MotionHandling.getMotionHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						rq.just = JustificationHandling.getJustificationHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						rq.sign = SignatureHandling.getSignatureHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						rq.tran = TranslationHandling.getTranslationHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						rq.news = NewsHandling.getNewsHashes(last_sync_date, org_id, _maxDate, BIG_LIMIT);
						if(DEBUG) out.println("OrgHandling:getOrgData: got advertising: "+_maxDate[0]+" "+rq);
						od.availableHashes = rq;
						maxDate = _maxDate[0];
					}
				}
			}
			try{
				_orgData.removeAll(Arrays.asList(new D_Organization[]{null}));
			}catch(Exception e){e.printStackTrace();}
			orgData = _orgData.toArray(new D_Organization[0]);
		}
		if(DEBUG) out.println("OrgHandling:getOrgData':exit: "+orgData);
		if(DEBUG) out.println("***************");
		return orgData;
	}
	static boolean privateOrg(String org_id) {
		D_Organization dorg;
		try {
			dorg = D_Organization.getOrgByLID_NoKeep(org_id, true);
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
		return !dorg.broadcast_rule;
	}
	static boolean serving_org_to_peer(String org_id, D_Peer address) {
		D_Organization dorg;
		ArrayList<D_OrgDistribution> b = D_OrgDistribution.get_Org_Distribution_byPeerID(address.getLIDstr());

		if((address.component_basic_data.emails!=null)&&(address.isEmailVerified())){
			try {
				dorg = D_Organization.getOrgByLID_NoKeep(org_id, true);
			} catch (Exception e) {
				e.printStackTrace();
				return true;
			}
			HashSet<String> emails = D_Organization.getEmailsSet(address.component_basic_data.emails);
			for(String email : emails)
				if(dorg.preapproved.contains(email)) return true;
		}
		return D_OrgDistribution.contains_org(b, Util.lval(org_id, -1));
	}
	void test(int of, long l_org_id, String org_gid, D_Organization[] orgData){
		
		if (orgData[of].constituents != null)
			for(int k = 0; k<orgData[of].constituents.length; k++)
				if (!orgData[of].constituents[k].constituent.verifySignature())  {
					if(DEBUG) System.err.println("OrgHandling:getOrgData: fail verifying const["+k+"]="+orgData[of].constituents[k].constituent);
					Util.printCallPath("Failure signature const");
					throw new RuntimeException("Failure signature const");
				}
		
		if(orgData[of].neighborhoods!=null)
			for(int k=0; k<orgData[of].neighborhoods.length; k++) {
				//orgData[of].neighborhoods[k].neighborhood.organization_ID = l_org_id;
				orgData[of].neighborhoods[k].neighborhood.setOrgIDs(org_gid, l_org_id);
				if(!orgData[of].neighborhoods[k].neighborhood.verifySignature()) {
					if(DEBUG) System.err.println("OrgHandling:getOrgData: fail verifying neigh["+k+"]="+orgData[of].neighborhoods[k].neighborhood);
					Util.printCallPath("Failure signature neigh");
					throw new RuntimeException("Failure signature neigh");
				}
			}
		
		if(orgData[of].witnesses!=null)
			for(int k=0; k<orgData[of].witnesses.length; k++)
				if(!orgData[of].witnesses[k].verifySignature())  {
					if(DEBUG) System.err.println("OrgHandling:getOrgData: fail verifying witn["+k+"]="+orgData[of].witnesses[k]);
					Util.printCallPath("Failure signature witn");
					throw new RuntimeException("Failure signature witness");
				}	
	}
	/**
	 * Do we serve org_id to the peer generating asr.
	 * @param asr
	 * @param org_id
	 * @return
	 */
	static boolean serving(ASNSyncRequest asr, String org_id){
		if(org_id == null){
			System.err.println("OrgHandling:serving: cannot check null organization!");
			return false;
		}
		boolean serving_peer = serving_org_to_peer(org_id, asr.address);
		if(!serving_peer && privateOrg(org_id)){
			if(DEBUG||DD.DEBUG_PRIVATE_ORGS) System.out.println("OrgHandling:getOrgData: not serving="+org_id+" to="+asr.address);
			return false;
		}else{
			if(serving_peer){
				if(DEBUG||DD.DEBUG_PRIVATE_ORGS) System.out.println("OrgHandling:getOrgData: serving private orgID="+org_id);
			}else{
				if(DEBUG||DD.DEBUG_PRIVATE_ORGS) System.out.println("OrgHandling:getOrgData: serving non-private orgID="+org_id);
			}
		}
		return true;
	}
}


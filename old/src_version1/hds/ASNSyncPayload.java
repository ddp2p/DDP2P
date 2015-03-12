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

package hds;
import java.util.ArrayList;
import java.util.Calendar;

import streaming.RequestData;
import streaming.SpecificRequest;
import streaming.WB_Messages;
import util.Summary;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;
import data.ASNNeighborhoodOP;
import data.D_Constituent;
import data.D_FieldValue;
import data.D_Justification;
import data.D_Motion;
import data.D_Neighborhood;
import data.D_News;
import data.D_OrgParam;
import data.D_Organization;
import data.D_PeerAddress;
import data.D_PeerOrgs;
import data.D_PluginData;
import data.D_Translations;
import data.D_Vote;
import data.D_Witness;
/**
ASNSyncPayload := IMPLICIT [APPLICATION 8] SEQUENCE {
	version UTF8String,
	upToDate GeneralizedTime OPTIONAL,
	tables [APPLICATION 0] ASNDatabase OPTIONAL,
	orgData [APPLICATION 1] SEQUENCE OF D_Organization OPTIONAL,
	orgCRL [APPLICATION 2] OrgCRL OPTIONAL,
	responderID PrintableString OPTIONAL,
	plugins [APPLICATION 11] SEQUENCE OF ASNPluginInfo OPTIONAL,
	plugin_data_set [APPLICATION 12] D_PluginData OPTIONAL,
	requested [APPLICATION 13] WB_Messages OPTIONAL,
	advertised [APPLICATION 14] SpecificRequest OPTIONAL,
	advertised_orgs_hash [APPLICATION 15] SEQUENCE OF OrgsData_Hash OPTIONAL,
	advertised_orgs [APPLICATION 16] SEQUENCE OF OrgInfo OPTIONAL,
	dictionary_GIDs [APPLICATION 17] SEQUENCE OF PrintableString OPTIONAL,
	changed_orgs [APPLICATION 18] SEQUENCE OF ResetOrgInfo OPTIONAL,
}
*/
public class ASNSyncPayload extends ASNObj{
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static boolean STREAMING_SEND_CREATOR_IN_PEER = true; // if not sent, then receivers won't have it in time for more requests
	public static boolean STREAMING_SEND_NEIGHBORHOOD_IN_CONSTITUENT = false;
	public String version="1";
	public Calendar upToDate; // OPTIONAL
	public ASNDatabase tables; //AC0 OPTIONAL
	public D_Organization orgData[]; //AC1 OPTIONAL
	public OrgCRL orgCRL;   //AC2 OPTIONAL
	public String responderID; // OPTIONAL
	public String peer_instance; // the agent instance (not yet implemented)
	public ASNPluginInfo plugins[]; //AC11 OPTIONAL
	//ASNPluginData plugin_data[]; //AC12 OPTIONAL
	public D_PluginData plugin_data_set; //AC12 OPTIONAL
	public WB_Messages requested;
	public ArrayList<String> dictionary_GIDs =  new ArrayList<String>();
	
	public SpecificRequest advertised;   // the GIDhash of all data that I have upToDate (from last_sync_Date), grouped by org
	public ArrayList<OrgInfo> advertised_orgs; // the GID of new orgs, and their name
	public ArrayList<ResetOrgInfo> changed_orgs; // orgs enabled backwards
	
	// hash of all data I have, to avoid resending it if the hash is the same
	// this currently does not work for votes, as their hash is not containing the actual vote, justification and creation date 
	public ArrayList<OrgsData_Hash> advertised_orgs_hash;

	public boolean empty() {
		if((advertised!=null) && !advertised.empty()) return true;
		return true;
	}
	
	/**
	 * Called before encoding
	 */
	private void prepareDictionary() {
		try{_prepareDictionary();}catch(Exception e){e.printStackTrace();}
	}
	private void _prepareDictionary() {
		// start with peerGIDs, orgGIDs, constGIDs,,,,, ince these are large
		
		if(this.responderID!=null)responderID = addToDictionaryGetIdxS(dictionary_GIDs, responderID);
		
		preparePeersDictionary();
		prepareOrgsDictionary();
		
		if(this.requested!=null) {
			if(this.requested.cons !=null){
				for(D_Constituent c : this.requested.cons)	this.prepareConstDictionary(c);
				for(D_Organization o : this.requested.orgs)	this.prepareOrgDictionary(o);
				for(D_Neighborhood n : this.requested.neig)	this.prepareNeigDictionary(n);
				for(D_Witness w : this.requested.witn)	this.prepareWitnDictionary(w);
				for(D_Motion m : this.requested.moti)	this.prepareMotiDictionary(m);
				for(D_Vote v : this.requested.sign)	this.prepareVoteDictionary(v);
				for(D_Justification j : this.requested.just) this.prepareJustDictionary(j);
				for(D_News e : this.requested.news) this.prepareNewsDictionary(e);
				for(D_Translations e : this.requested.tran) this.prepareTranDictionary(e);
			}
		}
	}
	private void expandDictionariesAtDecoding() {
		try{_expandDictionariesAtDecoding();}catch(Exception e){e.printStackTrace();}
	}
	/**
	 * Called after decoding
	 */
	private void _expandDictionariesAtDecoding() {
		if(this.responderID!=null)responderID = getDictionaryValueOrKeep((responderID));

		expandPeersDictionariesAtDecoding();
		expandOrgsDictionariesAtDecoding();
		
		if(this.requested!=null) {
			if(this.requested.cons !=null){
				for(D_Constituent c : this.requested.cons)	this.expandConstDictionariesAtDecoding(c);
				for(D_Organization o : this.requested.orgs)	this.expandOrgDictionariesAtDecoding(o);
				for(D_Neighborhood n : this.requested.neig)	this.expandNeigDictionariesAtDecoding(n);
				for(D_Witness w : this.requested.witn)	this.expandWitnDictionariesAtDecoding(w);
				for(D_Motion m : this.requested.moti)	this.expandMotiDictionariesAtDecoding(m);
				for(D_Vote v : this.requested.sign)	this.expandVoteDictionariesAtDecoding(v);
				for(D_Justification j : this.requested.just) this.expandJustDictionariesAtDecoding(j);
				for(D_News e : this.requested.news) this.expandNewsDictionariesAtDecoding(e);
				for(D_Translations e : this.requested.tran) this.expandTranDictionariesAtDecoding(e);
			}
		}
	}
	
	private void prepareOrgsDictionary(){
		if(orgData!=null) for(D_Organization o: orgData)  prepareOrgDictionary(o);
	}
	private void expandOrgsDictionariesAtDecoding() {
		if(orgData!=null) for(D_Organization o: orgData) expandOrgDictionariesAtDecoding(o);
	}
	
	private void prepareOrgDictionary(D_Organization o) {
		prepareOrgDictionary(o, dictionary_GIDs);
		
		if(o.constituents != null)
			for(data.ASNConstituentOP c : o.constituents) {
				if(c.constituent==null) continue;
				c.constituent.global_organization_ID = null;
				if(!STREAMING_SEND_NEIGHBORHOOD_IN_CONSTITUENT) c.constituent.neighborhood = null;
				prepareConstDictionary(c.constituent);
			}
		if(o.neighborhoods != null)
			for(ASNNeighborhoodOP n : o.neighborhoods) {
				if(n.neighborhood==null) continue;
				n.neighborhood.global_organization_ID = null;
				prepareNeigDictionary(n.neighborhood);
			}
		if(o.witnesses!=null) for(D_Witness w : o.witnesses) {w.global_organization_ID = null;prepareWitnDictionary(w);}
		if(o.motions!=null) for(D_Motion m : o.motions) {m.global_organization_ID = null;prepareMotiDictionary(m);}
		if(o.signatures!=null) for(D_Vote v : o.signatures) {v.global_organization_ID = null;prepareVoteDictionary(v);}
		if(o.justifications!=null) for(D_Justification j : o.justifications) {j.global_organization_ID = null;prepareJustDictionary(j);}
		if(o.news!=null) for(D_News e : o.news) {e.global_organization_ID = null;prepareNewsDictionary(e);}
		if(o.translations!=null) for(D_Translations t : o.translations) {t.global_organization_ID = null;prepareTranDictionary(t);}
	}
	public static void prepareOrgDictionary(D_Organization o, ArrayList<String> dictionary_GIDs) {
		if(o.global_organization_ID!=null) o.global_organization_IDhash=null; // drop hash
		if(o.creator!=null){
			if(STREAMING_SEND_CREATOR_IN_PEER) {
				preparePeerDictionary(o.creator, dictionary_GIDs);
			}else{
				System.err.println("AsnSyncPayload:prepareOrgsDictionary: will drop org creator = "+o.creator);
				o.creator=null;
			}
		}

		if(o.global_organization_ID != null) 
			o.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, o.global_organization_ID);
		if(o.global_organization_IDhash != null) 
			o.global_organization_IDhash = addToDictionaryGetIdxS(dictionary_GIDs, o.global_organization_IDhash);
		if((o.params != null) && (o.params.creator_global_ID != null))
			o.params.creator_global_ID = addToDictionaryGetIdxS(dictionary_GIDs, o.params.creator_global_ID);

		if((o.params != null) && (o.params.orgParam!=null))
			for(int p=0; p< o.params.orgParam.length; p++) prepareFieldsExtraDictionary(o.params.orgParam[p], dictionary_GIDs);
		
	}
	private void expandOrgDictionariesAtDecoding(D_Organization o) {
		expandOrgDictionariesAtDecoding(o, dictionary_GIDs);

		if(o.constituents != null)
			for(data.ASNConstituentOP c : o.constituents) {
				if(c.constituent==null) continue;
				expandConstDictionariesAtDecoding(c.constituent);
				c.constituent.global_organization_ID = o.global_organization_ID;
				//c.constituent.neighborhood = ;// cannot reconstruct
			}
		if(o.neighborhoods != null)
			for(ASNNeighborhoodOP n : o.neighborhoods) {
				if(n.neighborhood==null) continue;
				expandNeigDictionariesAtDecoding(n.neighborhood);
				n.neighborhood.global_organization_ID = o.global_organization_ID;
			}
		if(o.witnesses!=null) 
			for(D_Witness w : o.witnesses) {
				expandWitnDictionariesAtDecoding(w);
				w.global_organization_ID = o.global_organization_ID;
			}
		if(o.motions!=null)
			for(D_Motion m : o.motions) {
				expandMotiDictionariesAtDecoding(m);
				m.global_organization_ID = o.global_organization_ID;
			}
		if(o.signatures!=null)
			for(D_Vote v : o.signatures) {
				expandVoteDictionariesAtDecoding(v);
				v.global_organization_ID = o.global_organization_ID;
			}
		if(o.justifications!=null)
			for(D_Justification j : o.justifications) {
				expandJustDictionariesAtDecoding(j);
				j.global_organization_ID = o.global_organization_ID;
			}
		if(o.news!=null)
			for(D_News e : o.news) {
				expandNewsDictionariesAtDecoding(e);
				e.global_organization_ID = o.global_organization_ID;
			}
		if(o.translations!=null)
			for(D_Translations t : o.translations) {
				expandTranDictionariesAtDecoding(t);
				t.global_organization_ID = o.global_organization_ID;
			}
	}
	public static void expandOrgDictionariesAtDecoding(D_Organization o, ArrayList<String> dictionary_GIDs) {
		if(o.creator!=null){
			expandPeerDictionariesAtDecoding(o.creator, dictionary_GIDs);
		}
		if(o.global_organization_ID != null) 
			o.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (o.global_organization_ID));
		if(o.global_organization_IDhash != null) 
			o.global_organization_IDhash = getDictionaryValueOrKeep(dictionary_GIDs, (o.global_organization_IDhash));
		if((o.params != null) && (o.params.creator_global_ID != null))
			o.params.creator_global_ID = getDictionaryValueOrKeep(dictionary_GIDs, (o.params.creator_global_ID));

		// reconstruct hash
		if(o.global_organization_ID!=null){
			o.global_organization_IDhash = o.getOrgGIDHashFromGID();
			if(DEBUG){
				String guess = D_Organization.getOrgGIDHashGuess(o.global_organization_ID);
				String fix = o.global_organization_IDhash;
				if(guess.compareTo(fix)!=0){
					Util.printCallPath("assumption failed: probably the org method is not passed");
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: guess is="+guess);
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: fix is="+fix);
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: orig is="+o.global_organization_ID);
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: org is="+o);
				}
			}
		}
		if((o.params != null) && (o.params.orgParam!=null))
			for(int p=0; p< o.params.orgParam.length; p++) expandFieldsExtraDictionary(o.params.orgParam[p], dictionary_GIDs);
	}

	public void prepareFieldsExtraDictionary(D_OrgParam p) {
		prepareFieldsExtraDictionary(p, this.dictionary_GIDs);
	}
	public static void prepareFieldsExtraDictionary(D_OrgParam p, ArrayList<String> dictionary_GIDs) {
		if(p.global_field_extra_ID!=null) p.global_field_extra_ID = addToDictionaryGetIdxS(dictionary_GIDs, p.global_field_extra_ID);
	}
	private void expandFieldsExtraDictionary(D_OrgParam p) {
		expandFieldsExtraDictionary(p, dictionary_GIDs);
	}
	private static void expandFieldsExtraDictionary(D_OrgParam p,
			ArrayList<String> dictionary_GIDs) {
		if(p.global_field_extra_ID!=null) p.global_field_extra_ID = getDictionaryValueOrKeep(dictionary_GIDs, (p.global_field_extra_ID));
	}
	
	public static void prepareServedDictionary(D_PeerOrgs s,
			ArrayList<String> dictionary_GIDs) {
		if(s.global_organization_ID!=null) s.global_organization_IDhash = null;
		if(s.global_organization_ID!=null) s.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, s.global_organization_ID);
		if(s.global_organization_IDhash!=null) s.global_organization_IDhash = addToDictionaryGetIdxS(dictionary_GIDs, s.global_organization_IDhash);
	}
	private void prepareServedDictionary(D_PeerOrgs s) {
		prepareServedDictionary(s, dictionary_GIDs);
	}
	public void expandServedDictionariesAtDecoding(D_PeerOrgs s) {
		expandServedDictionariesAtDecoding(s, dictionary_GIDs);
	}
	public static void expandServedDictionariesAtDecoding(D_PeerOrgs s,
			ArrayList<String> dictionary_GIDs) {
		if(s.global_organization_ID!=null) s.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (s.global_organization_ID));
		if(s.global_organization_IDhash!=null) s.global_organization_IDhash = getDictionaryValueOrKeep(dictionary_GIDs, (s.global_organization_IDhash));
		if(s.global_organization_ID!=null) s.global_organization_IDhash = D_Organization.getOrgGIDHashGuess(s.global_organization_ID);
	}

	/*
	if(o.creator.globalID!=null)o.creator.globalIDhash=null;
	if(o.creator.globalID!=null)o.creator.globalID = addToDictionaryGetIdxS(dictionary_GIDs, o.creator.globalID);
	if(o.creator.globalIDhash!=null)o.creator.globalIDhash = addToDictionaryGetIdxS(dictionary_GIDs, o.creator.globalIDhash);
	if(o.creator.served_orgs!=null) for(D_PeerOrgs s: o.creator.served_orgs) prepareServedDictionary(s);
	*/
	public static void preparePeerDictionary(D_PeerAddress p,
			ArrayList<String> dictionary_GIDs) {
		if(p.component_basic_data.globalID!=null) p.component_basic_data.globalIDhash = null;
		if(p.component_basic_data.globalID!=null) p.component_basic_data.globalID = addToDictionaryGetIdxS(dictionary_GIDs, p.component_basic_data.globalID);
		if(p.component_basic_data.globalIDhash!=null) p.component_basic_data.globalIDhash = addToDictionaryGetIdxS(dictionary_GIDs, p.component_basic_data.globalIDhash);
		if(p.served_orgs!=null)
			for(D_PeerOrgs _p : p.served_orgs) {
				prepareServedDictionary(_p, dictionary_GIDs);
				/*
				if(_p.global_organization_ID!=null) _p.global_organization_IDhash = null;
				if(_p.global_organization_ID!=null) _p.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, _p.global_organization_ID);
				if(_p.global_organization_IDhash!=null) _p.global_organization_IDhash = addToDictionaryGetIdxS(dictionary_GIDs, _p.global_organization_IDhash);
				*/
			}
	}
	/*
	if(o.creator.globalID!=null)o.creator.globalID = getDictionaryValueOrKeep(dictionary_GIDs, (o.creator.globalID));
	if(o.creator.globalIDhash!=null)o.creator.globalIDhash = getDictionaryValueOrKeep(dictionary_GIDs, (o.creator.globalIDhash));
	if(o.creator.globalID!=null) o.creator.globalIDhash = D_PeerAddress.getGIDHashFromGID(o.creator.globalID);
	if(o.creator.served_orgs!=null) for(D_PeerOrgs s: o.creator.served_orgs) expandServedDictionariesAtDecoding(s);
	*/
	public static void expandPeerDictionariesAtDecoding(D_PeerAddress p,
			ArrayList<String> dictionary_GIDs) {
		if(p.component_basic_data.globalID!=null) p.component_basic_data.globalID = getDictionaryValueOrKeep(dictionary_GIDs, p.component_basic_data.globalID);
		if(p.component_basic_data.globalIDhash!=null) p.component_basic_data.globalIDhash = getDictionaryValueOrKeep(dictionary_GIDs, p.component_basic_data.globalIDhash);
		if(p.component_basic_data.globalID!=null) p.component_basic_data.globalIDhash = D_PeerAddress.getGIDHashFromGID(p.component_basic_data.globalID);
		if(p.served_orgs!=null)
			for(D_PeerOrgs _p : p.served_orgs) {
				expandServedDictionariesAtDecoding(_p, dictionary_GIDs);
				/*
				if(_p.global_organization_ID!=null) _p.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, _p.global_organization_ID);
				if(_p.global_organization_IDhash!=null) _p.global_organization_IDhash = getDictionaryValueOrKeep(dictionary_GIDs, _p.global_organization_IDhash);
				*/
			}
	}
	/**
	 * global_translation_ID need not be extracted as it is not referred
	 * @param e
	 */
	public static void prepareTranDictionary(D_Translations e, ArrayList<String> dictionary_GIDs) {
		if(e.global_organization_ID!=null) e.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_organization_ID);
		if(e.global_constituent_ID!=null) e.global_constituent_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_constituent_ID);
		//if(e.global_translation_ID!=null) e.global_translation_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_translation_ID);
		if(e.organization!=null) prepareOrgDictionary(e.organization, dictionary_GIDs);
		if(e.constituent!=null) prepareConstDictionary(e.constituent, dictionary_GIDs);
	}
	private void prepareTranDictionary(D_Translations e) {
		prepareTranDictionary(e, dictionary_GIDs);
	}
	private void expandTranDictionariesAtDecoding(D_Translations e) {
		expandTranDictionariesAtDecoding(e, dictionary_GIDs);
	}
	public static void expandTranDictionariesAtDecoding(D_Translations e, ArrayList<String> dictionary_GIDs) {
		if(e.global_organization_ID!=null) e.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (e.global_organization_ID));
		if(e.global_constituent_ID!=null) e.global_constituent_ID = getDictionaryValueOrKeep(dictionary_GIDs, (e.global_constituent_ID));
		//if(e.global_translation_ID!=null) e.global_translation_ID = getDictionaryValueOrKeep((e.global_translation_ID));
		if(e.organization!=null) expandOrgDictionariesAtDecoding(e.organization, dictionary_GIDs);
		if(e.constituent!=null) expandConstDictionariesAtDecoding(e.constituent, dictionary_GIDs);
	}
	
	public static void prepareJustDictionary(D_Justification j, ArrayList<String> dictionary_GIDs) {
		if(j.global_organization_ID!=null) j.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, j.global_organization_ID);
		if(j.global_constituent_ID!=null) j.global_constituent_ID = addToDictionaryGetIdxS(dictionary_GIDs, j.global_constituent_ID);
		if(j.global_motionID!=null) j.global_motionID = addToDictionaryGetIdxS(dictionary_GIDs, j.global_motionID);
		if(j.global_justificationID!=null) j.global_justificationID = addToDictionaryGetIdxS(dictionary_GIDs, j.global_justificationID);
		if(j.global_answerTo_ID!=null) j.global_answerTo_ID = addToDictionaryGetIdxS(dictionary_GIDs, j.global_answerTo_ID);
		
		if(j.constituent!=null) prepareConstDictionary(j.constituent, dictionary_GIDs);
		if(j.motion!=null) prepareMotiDictionary(j.motion, dictionary_GIDs);
	}
	private void prepareJustDictionary(D_Justification j) {
		prepareJustDictionary(j, dictionary_GIDs);
	}
	private void expandJustDictionariesAtDecoding(D_Justification j) {
		expandJustDictionariesAtDecoding(j, dictionary_GIDs);
	}
	public static void expandJustDictionariesAtDecoding(D_Justification j, ArrayList<String> dictionary_GIDs) {
		if(j.global_organization_ID!=null) j.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (j.global_organization_ID));
		if(j.global_constituent_ID!=null) j.global_constituent_ID = getDictionaryValueOrKeep(dictionary_GIDs, (j.global_constituent_ID));
		if(j.global_motionID!=null) j.global_motionID = getDictionaryValueOrKeep(dictionary_GIDs, (j.global_motionID));
		if(j.global_justificationID!=null) j.global_justificationID = getDictionaryValueOrKeep(dictionary_GIDs, (j.global_justificationID));
		if(j.global_answerTo_ID!=null) j.global_answerTo_ID = getDictionaryValueOrKeep(dictionary_GIDs, (j.global_answerTo_ID));
		
		if(j.constituent!=null) expandConstDictionariesAtDecoding(j.constituent, dictionary_GIDs);
		if(j.motion!=null) expandMotiDictionariesAtDecoding(j.motion, dictionary_GIDs);
	}
	/**
	 * VoteID is not extracted as it is not typically referred anywhere. Need not even be sent at all here, except as checksums
	 * @param v
	 * @param dictionary_GIDs
	 */

	public static void prepareVoteDictionary(D_Vote v, ArrayList<String> dictionary_GIDs) {
		if(v.global_organization_ID!=null) v.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_organization_ID);
		if(v.global_constituent_ID!=null) v.global_constituent_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_constituent_ID);
		if(v.global_motion_ID!=null) v.global_motion_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_motion_ID);
		if(v.global_justification_ID!=null) v.global_justification_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_justification_ID);
		//v.global_vote_ID = null;
		//if(v.global_vote_ID!=null) v.global_vote_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_vote_ID);
		if(v.constituent!=null) ASNSyncPayload.prepareConstDictionary(v.constituent, dictionary_GIDs);
		if(v.motion!=null) ASNSyncPayload.prepareMotiDictionary(v.motion, dictionary_GIDs);
		if(v.justification!=null) ASNSyncPayload.prepareJustDictionary(v.justification, dictionary_GIDs);
	}
	private void prepareVoteDictionary(D_Vote v) {
		prepareVoteDictionary(v, dictionary_GIDs);
	}
	private void expandVoteDictionariesAtDecoding(D_Vote v) {
		expandVoteDictionariesAtDecoding(v, dictionary_GIDs);
	}
	public static void expandVoteDictionariesAtDecoding(D_Vote v, ArrayList<String> dictionary_GIDs) {
		if(v.global_organization_ID!=null) v.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs,(v.global_organization_ID));
		if(v.global_constituent_ID!=null) v.global_constituent_ID = getDictionaryValueOrKeep(dictionary_GIDs,(v.global_constituent_ID));
		if(v.global_motion_ID!=null) v.global_motion_ID = getDictionaryValueOrKeep(dictionary_GIDs,(v.global_motion_ID));
		if(v.global_justification_ID!=null) v.global_justification_ID = getDictionaryValueOrKeep(dictionary_GIDs,(v.global_justification_ID));
		//v.global_vote_ID = v.make_ID();
		//if(v.global_vote_ID!=null) v.global_vote_ID = getDictionaryValueOrKeep(dictionary_GIDs,(v.global_vote_ID));
		if(v.constituent!=null) ASNSyncPayload.expandConstDictionariesAtDecoding(v.constituent, dictionary_GIDs);
		if(v.motion!=null) ASNSyncPayload.expandMotiDictionariesAtDecoding(v.motion, dictionary_GIDs);
		if(v.justification!=null) ASNSyncPayload.expandJustDictionariesAtDecoding(v.justification, dictionary_GIDs);
	}
	/**
	 * NewsID is not extracted as it is not referred
	 * @param e
	 */
	public static void prepareNewsDictionary(D_News e, ArrayList<String> dictionary_GIDs) {
		if(e.global_organization_ID!=null) e.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_organization_ID);
		if(e.global_constituent_ID!=null) e.global_constituent_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_constituent_ID);
		if(e.global_motion_ID!=null) e.global_motion_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_motion_ID);
		//if(e.global_news_ID!=null) e.global_news_ID = addToDictionaryGetIdxS(dictionary_GIDs, e.global_news_ID);
		if(e.constituent!=null) prepareConstDictionary(e.constituent, dictionary_GIDs);
		if(e.motion!=null) prepareMotiDictionary(e.motion, dictionary_GIDs);
		if(e.organization!=null) prepareOrgDictionary(e.organization, dictionary_GIDs);
	}
	private void prepareNewsDictionary(D_News e) {
		prepareNewsDictionary(e, dictionary_GIDs);
	}
	private void expandNewsDictionariesAtDecoding(D_News e) {
		expandNewsDictionariesAtDecoding(e, dictionary_GIDs);
	}
	public static void expandNewsDictionariesAtDecoding(D_News e, ArrayList<String> dictionary_GIDs) {
		if(e.global_organization_ID!=null) e.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (e.global_organization_ID));
		if(e.global_constituent_ID!=null) e.global_constituent_ID = getDictionaryValueOrKeep(dictionary_GIDs, (e.global_constituent_ID));
		if(e.global_motion_ID!=null) e.global_motion_ID = getDictionaryValueOrKeep(dictionary_GIDs, (e.global_motion_ID));
		//if(e.global_news_ID!=null) e.global_news_ID = getDictionaryValueOrKeep(dictionary_GIDs, (e.global_news_ID));
		if(e.constituent!=null) expandConstDictionariesAtDecoding(e.constituent, dictionary_GIDs);
		if(e.motion!=null) expandMotiDictionariesAtDecoding(e.motion, dictionary_GIDs);
		if(e.organization!=null) expandOrgDictionariesAtDecoding(e.organization, dictionary_GIDs);
	}

	public static void prepareMotiDictionary(D_Motion m, ArrayList<String> dictionary_GIDs) {
		if(m.global_organization_ID!=null) m.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, m.global_organization_ID);
		if(m.global_constituent_ID!=null) m.global_constituent_ID = addToDictionaryGetIdxS(dictionary_GIDs, m.global_constituent_ID);
		if(m.global_motionID!=null) m.global_motionID = addToDictionaryGetIdxS(dictionary_GIDs, m.global_motionID);
		if(m.global_enhanced_motionID!=null) m.global_enhanced_motionID = addToDictionaryGetIdxS(dictionary_GIDs, m.global_enhanced_motionID);
		
		if(m.constituent!=null) prepareConstDictionary(m.constituent, dictionary_GIDs);
	}
	private void prepareMotiDictionary(D_Motion m) {
		prepareMotiDictionary(m, this.dictionary_GIDs);
	}
	private void expandMotiDictionariesAtDecoding(D_Motion m) {
		expandMotiDictionariesAtDecoding(m, dictionary_GIDs);
	}
	public static void expandMotiDictionariesAtDecoding(D_Motion m, ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+m);
		if(m.global_organization_ID!=null) m.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (m.global_organization_ID));
		if(m.global_constituent_ID!=null) m.global_constituent_ID = getDictionaryValueOrKeep(dictionary_GIDs, (m.global_constituent_ID));
		if(m.global_motionID!=null) m.global_motionID = getDictionaryValueOrKeep(dictionary_GIDs, (m.global_motionID));
		if(m.global_enhanced_motionID!=null) m.global_enhanced_motionID = getDictionaryValueOrKeep(dictionary_GIDs, (m.global_enhanced_motionID));
		
		if(m.constituent!=null) expandConstDictionariesAtDecoding(m.constituent, dictionary_GIDs);
	}
	/**
	 * WitnessID needs not be extracts as it is not referred
	 * @param w
	 * @param dictionary_GIDs
	 */
	public static void prepareWitnDictionary(D_Witness w,
			ArrayList<String> dictionary_GIDs) {
		if(w.global_organization_ID!=null) w.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, w.global_organization_ID);
		//if(w.global_witness_ID!=null) w.global_witness_ID = addToDictionaryGetIdxS(dictionary_GIDs, w.global_witness_ID);
		if(w.witnessed_global_neighborhoodID!=null) w.witnessed_global_neighborhoodID = addToDictionaryGetIdxS(dictionary_GIDs, w.witnessed_global_neighborhoodID);
		if(w.witnessing_global_constituentID!=null) w.witnessing_global_constituentID = addToDictionaryGetIdxS(dictionary_GIDs, w.witnessing_global_constituentID);
		if(w.witnessed_global_constituentID!=null) w.witnessed_global_constituentID = addToDictionaryGetIdxS(dictionary_GIDs, w.witnessed_global_constituentID);
		
		if(w.witnessed!=null) prepareConstDictionary(w.witnessed, dictionary_GIDs);
		if(w.witnessing!=null) prepareConstDictionary(w.witnessing, dictionary_GIDs);
	}
	private void prepareWitnDictionary(D_Witness w) {
		prepareWitnDictionary(w, this.dictionary_GIDs);
	}
	private void expandWitnDictionariesAtDecoding(D_Witness w) {
		expandWitnDictionariesAtDecoding(w, dictionary_GIDs);
	}
	public static void expandWitnDictionariesAtDecoding(D_Witness w,
			ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+w);
		if(w.global_organization_ID!=null) w.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (w.global_organization_ID));
		//if(w.global_witness_ID!=null) w.global_witness_ID = getDictionaryValueOrKeep(dictionary_GIDs, (w.global_witness_ID));
		if(w.witnessed_global_neighborhoodID!=null) w.witnessed_global_neighborhoodID = getDictionaryValueOrKeep(dictionary_GIDs, (w.witnessed_global_neighborhoodID));
		if(w.witnessing_global_constituentID!=null) w.witnessing_global_constituentID = getDictionaryValueOrKeep(dictionary_GIDs, (w.witnessing_global_constituentID));
		if(w.witnessed_global_constituentID!=null) w.witnessed_global_constituentID = getDictionaryValueOrKeep(dictionary_GIDs, (w.witnessed_global_constituentID));
		
		if(w.witnessed!=null) expandConstDictionariesAtDecoding(w.witnessed, dictionary_GIDs);
		if(w.witnessing!=null) expandConstDictionariesAtDecoding(w.witnessing, dictionary_GIDs);
	}

	public static void prepareNeigDictionary(D_Neighborhood n,
			ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+n);
		if(n==null) return;
		if(n.global_organization_ID!=null) n.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, n.global_organization_ID);
		if(n.global_neighborhood_ID!=null) n.global_neighborhood_ID = addToDictionaryGetIdxS(dictionary_GIDs, n.global_neighborhood_ID);
		if(n.submitter_global_ID!=null) n.submitter_global_ID = addToDictionaryGetIdxS(dictionary_GIDs, n.submitter_global_ID);
		if(n.parent_global_ID!=null) n.parent_global_ID = addToDictionaryGetIdxS(dictionary_GIDs, n.parent_global_ID);
		
		if(n.parent!=null) prepareNeigDictionary(n.parent, dictionary_GIDs);
		if(n.submitter!=null) prepareConstDictionary(n.submitter, dictionary_GIDs);
	}
	private void prepareNeigDictionary(D_Neighborhood n) {
		prepareNeigDictionary(n, dictionary_GIDs);
	}
	private void expandNeigDictionariesAtDecoding(D_Neighborhood n) {
		expandNeigDictionariesAtDecoding(n, dictionary_GIDs);
	}
	public static void expandNeigDictionariesAtDecoding(
			D_Neighborhood n, ArrayList<String> dictionary_GIDs) {
		if(n==null) return;
		if(n.global_organization_ID!=null) n.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (n.global_organization_ID));
		if(n.global_neighborhood_ID!=null) n.global_neighborhood_ID = getDictionaryValueOrKeep(dictionary_GIDs, (n.global_neighborhood_ID));
		if(n.submitter_global_ID!=null) n.submitter_global_ID = getDictionaryValueOrKeep(dictionary_GIDs, (n.submitter_global_ID));
		if(n.parent_global_ID!=null) n.parent_global_ID = getDictionaryValueOrKeep(dictionary_GIDs, (n.parent_global_ID));
		
		if(n.parent!=null) expandNeigDictionariesAtDecoding(n.parent, dictionary_GIDs);
		if(n.submitter!=null) expandConstDictionariesAtDecoding(n.submitter, dictionary_GIDs);
	}

	public static void prepareConstDictionary(D_Constituent c,
			ArrayList<String> dictionary_GIDs) {
		if(c==null)return;
		if(c.global_constituent_id!=null)c.global_constituent_id_hash=null;
		if(c.global_organization_ID!=null) c.global_organization_ID = addToDictionaryGetIdxS(dictionary_GIDs, c.global_organization_ID);
		if(c.global_neighborhood_ID!=null) c.global_neighborhood_ID = addToDictionaryGetIdxS(dictionary_GIDs, c.global_neighborhood_ID);
		if(c.global_constituent_id!=null) c.global_constituent_id = addToDictionaryGetIdxS(dictionary_GIDs, c.global_constituent_id);
		if(c.global_constituent_id_hash!=null) c.global_constituent_id_hash = addToDictionaryGetIdxS(dictionary_GIDs, c.global_constituent_id_hash);
		if(c.global_submitter_id!=null) c.global_submitter_id = addToDictionaryGetIdxS(dictionary_GIDs, c.global_submitter_id);
		if(c.address!=null) for(int k =0; k<c.address.length; k++) prepareValueDictionaries(c.address[k], dictionary_GIDs);
		
		if(c.neighborhood!=null) for(int k=0; k<c.neighborhood.length; k++) prepareNeigDictionary(c.neighborhood[k], dictionary_GIDs);
		if(c.submitter!=null) prepareConstDictionary(c.submitter, dictionary_GIDs);
		if(DEBUG)System.out.println("ASNSyncPayload:Prepared: "+c);
	}
	private void prepareConstDictionary(D_Constituent c) {
		prepareConstDictionary(c, dictionary_GIDs);
	}
	private void expandConstDictionariesAtDecoding(D_Constituent c) {
		expandConstDictionariesAtDecoding(c, dictionary_GIDs);
	}
	public static void expandConstDictionariesAtDecoding(
			D_Constituent c, ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+c);
		if(c==null) return;
		if(c.global_organization_ID!=null) c.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (c.global_organization_ID));
		if(c.global_neighborhood_ID!=null) c.global_neighborhood_ID = getDictionaryValueOrKeep(dictionary_GIDs, (c.global_neighborhood_ID));
		if(c.global_constituent_id!=null) c.global_constituent_id = getDictionaryValueOrKeep(dictionary_GIDs, (c.global_constituent_id));
		if(c.global_constituent_id_hash!=null) c.global_constituent_id_hash = getDictionaryValueOrKeep(dictionary_GIDs, (c.global_constituent_id_hash));
		if(c.global_submitter_id!=null) c.global_submitter_id = getDictionaryValueOrKeep(dictionary_GIDs, (c.global_submitter_id));
		if(c.address!=null) for(int k =0; k<c.address.length; k++) expandValuedictionaries(c.address[k], dictionary_GIDs);
		if(c.global_constituent_id!=null)c.global_constituent_id_hash=D_Constituent.getGIDHashFromGID(c.global_constituent_id);
		
		if(c.neighborhood!=null) for(int k=0; k<c.neighborhood.length; k++) expandNeigDictionariesAtDecoding(c.neighborhood[k], dictionary_GIDs);
		if(c.submitter!=null) expandConstDictionariesAtDecoding(c.submitter, dictionary_GIDs);
	}

	private static void prepareValueDictionaries(D_FieldValue v, ArrayList<String> dictionary_GIDs) {
		if(v.global_neighborhood_ID!=null) v.global_neighborhood_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_neighborhood_ID);
		if(v.field_extra_GID!=null) v.field_extra_GID = addToDictionaryGetIdxS(dictionary_GIDs, v.field_extra_GID);
		if(v.field_GID_above!=null) v.field_GID_above = addToDictionaryGetIdxS(dictionary_GIDs, v.field_GID_above);
		if(v.field_GID_default_next!=null) v.field_GID_default_next = addToDictionaryGetIdxS(dictionary_GIDs, v.field_GID_default_next);
		if(DEBUG)System.out.println("ASNSyncPayload:Prepared: "+v);
	}
	public void prepareValueDictionaries(D_FieldValue v) {
		prepareValueDictionaries(v, dictionary_GIDs);
	}
	private void expandValuedictionaries(D_FieldValue v) {
		expandValuedictionaries(v, dictionary_GIDs);
	}
	public static void expandValuedictionaries(D_FieldValue v,
			ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+v);
		if(v.global_neighborhood_ID!=null) v.global_neighborhood_ID = getDictionaryValueOrKeep(dictionary_GIDs, (v.global_neighborhood_ID));
		if(v.field_extra_GID!=null) v.field_extra_GID = getDictionaryValueOrKeep(dictionary_GIDs, (v.field_extra_GID));
		if(v.field_GID_above!=null) v.field_GID_above = getDictionaryValueOrKeep(dictionary_GIDs, (v.field_GID_above));
		if(v.field_GID_default_next!=null) v.field_GID_default_next = getDictionaryValueOrKeep(dictionary_GIDs, (v.field_GID_default_next));
		
	}
	private void preparePeersDictionary(){
		if(tables!=null){
			for (Table t: tables.tables) {
				if(t.name.equals(table.peer.G_TNAME)) {
					for(byte[][] row : t.rows) {
						//row[0]; // peerGID
						//row[5]; // orgs
						if(row[0]!=null){
							String peerGID = Util.getBString(row[0]);
							row[0] = new Encoder(addToDictionaryGetIdx(dictionary_GIDs, peerGID)).getBytes();
						}
						if(row[5]!=null){
							String global_organizationIDs = Util.getBString(row[5]);
							D_PeerOrgs[] served_orgs = table.peer_org.peerOrgsFromString(global_organizationIDs);
							if(served_orgs!=null){
								for(D_PeerOrgs s:served_orgs) prepareServedDictionary(s);
								String orgs = table.peer_org.stringFromPeerOrgs(served_orgs);
								row[5] = streaming.UpdatePeersTable.getFieldData(orgs);
							}
						}
					}
				}
			}
		}
	}
	
	private void expandPeersDictionariesAtDecoding() {
		if(tables!=null){
			for (Table t: tables.tables) {
				if(t.name.equals(table.peer.G_TNAME)) {
					for(byte[][] row : t.rows) {
						//row[0]; // peerGID
						//row[5]; // orgs
						if(row[0]!=null){
							int peerID = new Decoder(row[0]).getInteger().intValue();
							String pGID = dictionary_GIDs.get(peerID);
							if(pGID==null){Util.printCallPath("Unexpected case");continue;}
							row[0] = streaming.UpdatePeersTable.getFieldData(pGID);
						}
						if(row[5]!=null){
							String global_organizationIDs = Util.getBString(row[5]);
							D_PeerOrgs[] served_orgs = table.peer_org.peerOrgsFromString(global_organizationIDs);
							if(served_orgs!=null){
								for(D_PeerOrgs s:served_orgs) expandServedDictionariesAtDecoding(s);
								String orgs = table.peer_org.stringFromPeerOrgs(served_orgs);
								row[5] = streaming.UpdatePeersTable.getFieldData(orgs);
							}
						}						
					}
				}
			}
		}
	}
	public ASNSyncPayload(){}
	public ASNSyncPayload(Calendar _upToDate, String globalID){
		upToDate = _upToDate;
		responderID = globalID;
	}
	public ASNSyncPayload(String last_sync_date) {
		upToDate = Util.getCalendar(last_sync_date);
	}
	public String toString(){
		String result = "[SyncAnswer v="+version+"] "+" upTo:"+Encoder.getGeneralizedTime(upToDate)+
		" tables="+tables;
		result += "\n orgData= ["+Util.concat(orgData," ")+"]";
		result += "\n orgCRL="+orgCRL;
		result += "\n responderID="+Util.trimmed(responderID);
		result += "\n plugins="+Util.trimmed(""+Util.nullDiscrimArray(plugins,":"), 400);
		//result += "\n plugin_data="+Util.trimmed(plugin_data+"");
		result += "\n plugin_data_set="+Util.trimmed(plugin_data_set+"", 400);
		result += "\n requested="+requested;
		if(advertised_orgs_hash!=null) result += "\n advertised_orgs_hash="+Util.nullDiscrimArray(advertised_orgs_hash.toArray(new OrgsData_Hash[0]),"--");
		if(changed_orgs!=null) result += "\n changed_org="+Util.nullDiscrimArray(changed_orgs.toArray(new ResetOrgInfo[0]),"--");
		result += "\n advertised="+advertised+"";
		if(advertised_orgs!=null)result += "\n advertised_orgs="+Util.nullDiscrimArray(advertised_orgs.toArray(new OrgInfo[0]),"--");
		if(this.dictionary_GIDs!=null)result += "\n dictionay_GIDs="+Util.nullDiscrimArrayNumbered(dictionary_GIDs.toArray(new String[0]));
		return result;
	}
	public String toSummaryString(){
		String result = "\n[SyncAnswer v="+version+"] "+" upTo:"+Encoder.getGeneralizedTime(upToDate);
		if(tables!=null) result += "\n  tables="+tables.toSummaryString();
		if((orgData!=null) && (orgData.length>0))result += "\n  orgData#"+orgData.length+"= ["+Util.concatSummary(orgData," ",null)+"]";
		if(orgCRL!=null) result += "\n orgCRL="+orgCRL;
		result += "\n responderID="+Util.trimmed(responderID);
		if(plugins!=null) result += "\n plugins="+Util.trimmed(""+Util.nullDiscrimArray(plugins,":"), 400);
		//result += "\n plugin_data="+Util.trimmed(plugin_data+"");
		if((plugin_data_set!=null) && !plugin_data_set.empty()) result += "\n plugin_data_set="+Util.trimmed(plugin_data_set+"", 400);
		if(changed_orgs!=null) result += "\n ch_org="+Util.nullDiscrimArraySummary(changed_orgs.toArray(new Summary[0]),"--");
		if((requested!=null)&&(!requested.empty()))result += "\n requested="+requested.toSummaryString();
		//if(advertised_orgs_hash!=null) result += "\n advertised_orgs_hash="+Util.nullDiscrimArray(advertised_orgs_hash.toArray(new OrgsData_Hash[0]),"--");
		if(advertised!=null)result += "\n advertised="+advertised.toSummaryString()+"";
		//if(advertised_orgs!=null)result += "\n advertised_orgs="+Util.nullDiscrimArray(advertised_orgs.toArray(new OrgInfo[0]),"--");
		//if(this.dictionary_GIDs!=null)result += "\n dictionay_GIDs="+Util.nullDiscrimArrayNumbered(dictionary_GIDs.toArray(new String[0]));
		return result;
	}
	/**
ASNSyncPayload := IMPLICIT [APPLICATION 8] SEQUENCE {
	version UTF8String,
	upToDate GeneralizedTime OPTIONAL,
	tables [APPLICATION 0] ASNDatabase OPTIONAL,
	orgData [APPLICATION 1] SEQUENCE OF D_Organization OPTIONAL,
	orgCRL [APPLICATION 2] OrgCRL OPTIONAL,
	responderID PrintableString OPTIONAL,
	plugins [APPLICATION 11] SEQUENCE OF ASNPluginInfo OPTIONAL,
	plugin_data_set [APPLICATION 12] D_PluginData OPTIONAL,
	requested [APPLICATION 13] WB_Messages OPTIONAL,
	advertised [APPLICATION 14] SpecificRequest OPTIONAL,
	advertised_orgs_hash [APPLICATION 15] SEQUENCE OF OrgsData_Hash OPTIONAL,
	advertised_orgs [APPLICATION 16] SEQUENCE OF OrgInfo OPTIONAL,
	dictionary_GIDs [APPLICATION 17] SEQUENCE OF PrintableString OPTIONAL,
	changed_orgs [APPLICATION 18] SEQUENCE OF ResetOrgInfo OPTIONAL,
}
	 */
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding SyncAnswer");
		
		prepareDictionary();
		
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if(upToDate!=null) enc.addToSequence(new Encoder(upToDate));
		if(tables!=null) enc.addToSequence(tables.getEncoder().setASN1Type(DD.TAG_AC0));
		 
		//if(orgData!=null) enc.addToSequence(orgData.getEncoder().setASN1Type(DD.TAG_AC1));
		if(orgData!=null) enc.addToSequence(Encoder.getEncoder(orgData).setASN1Type(DD.TAG_AC1));
		if(orgCRL!=null) enc.addToSequence(orgCRL.getEncoder().setASN1Type(DD.TAG_AC2));
		//enc.addToSequence(new Encoder(true));
		if(responderID!=null) enc.addToSequence(new Encoder(responderID,false));
		if(plugins!=null) enc.addToSequence(Encoder.getEncoder(plugins).setASN1Type(DD.TAG_AC11));
		//if(plugin_data!=null) enc.addToSequence(Encoder.getEncoder(plugin_data).setASN1Type(DD.TAG_AC12));
		if(plugin_data_set!=null) enc.addToSequence(plugin_data_set.getEncoder().setASN1Type(DD.TAG_AC12));
		if(requested!=null) enc.addToSequence(requested.getEncoder().setASN1Type(DD.TAG_AC13));
		if(advertised!=null) enc.addToSequence(advertised.getEncoder().setASN1Type(DD.TAG_AC14));
		if(advertised_orgs_hash!=null) enc.addToSequence(Encoder.getEncoder(advertised_orgs_hash.toArray(new OrgsData_Hash[0])).setASN1Type(DD.TAG_AC15));
		if(advertised_orgs!=null) enc.addToSequence(Encoder.getEncoder(advertised_orgs.toArray(new OrgInfo[0])).setASN1Type(DD.TAG_AC16));
		if(dictionary_GIDs!=null) enc.addToSequence(Encoder.getStringEncoder(dictionary_GIDs.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC17));
		if(changed_orgs!=null){
			ResetOrgInfo[] ch_orgs = changed_orgs.toArray(new ResetOrgInfo[0]);
			enc.addToSequence(Encoder.getEncoder(ch_orgs).setASN1Type(DD.TAG_AC18));
		}
		enc.setASN1Type(getASN1Type());
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded SyncAnswer");
		return enc;
	}
	/**
	 * Returns DD.TAG_AC8
	 * @return
	 */
	public static byte getASN1Type(){
		return DD.TAG_AC8;
	}

	public ASNSyncPayload decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding SyncAnswer: "+" \nfrom: "+decoder.dumpHex());
		Decoder dec = decoder.getContent();
		if(ASNSyncRequest.DEBUG)System.out.println("DEncodes SA tag: "+dec.getTypeByte());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncodes SA: "+dec.dumpHex());
		version = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime)
			upToDate = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		if(dec.getTypeByte()==DD.TAG_AC0) {
			if(ASNSyncRequest.DEBUG)System.out.println("DEncoding tables");
			tables=new ASNDatabase();
			Decoder deco = dec.getFirstObject(true);
			if(ASNSyncRequest.DEBUG)System.out.println("tables msg="+deco.dumpHex());
			tables.decode(deco);
		}
		
		//if(dec.getTypeByte()==DD.TAG_AC1) orgData=new OrgData().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC1) orgData=(D_Organization[]) dec.getFirstObject(true).getSequenceOf(DD.TYPE_ORG_DATA, new D_Organization[0], new D_Organization());
		if(dec.getTypeByte()==DD.TAG_AC2) orgCRL=new OrgCRL().decode(dec.getFirstObject(true));
		//dec.getFirstObject(true,Encoder.TAG_BOOLEAN).getBoolean();
		//if(dec.getFirstObject(false)!=null) 
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)  responderID=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC11) plugins = (ASNPluginInfo[]) dec.getFirstObject(true).getSequenceOf(ASNPluginInfo.getASN1Type(), new ASNPluginInfo[0], new ASNPluginInfo());
		//if(dec.getTypeByte()==DD.TAG_AC12) plugin_data = (ASNPluginData[]) dec.getFirstObject(true).getSequenceOf(ASNPluginData.getASN1Type(), new ASNPluginData[0], new ASNPluginData());
		if(dec.getTypeByte()==DD.TAG_AC12) plugin_data_set = new D_PluginData().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC13) requested = new WB_Messages().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC14) advertised=new SpecificRequest().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC15) advertised_orgs_hash=dec.getFirstObject(true).getSequenceOfAL(OrgsData_Hash.getASN1Type(), new OrgsData_Hash());
		if(dec.getTypeByte()==DD.TAG_AC16) advertised_orgs=dec.getFirstObject(true).getSequenceOfAL(OrgInfo.getASN1Type(),new OrgInfo());
		if(dec.getTypeByte()==DD.TAG_AC17) dictionary_GIDs=dec.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC18) changed_orgs=dec.getFirstObject(true).getSequenceOfAL(ResetOrgInfo.getASN1Type(),new ResetOrgInfo());

		if(DEBUG)System.out.println("\n\n***********ASNSyncPayload:Decoded SyncAnswer: raw "+this+"\n");
		
		expandDictionariesAtDecoding();
		if(DEBUG)System.out.println("\n\n*******************ASNSyncPayload:Decoded SyncAnswer: expanded "+this+"\n");
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		return this;
	}
	/**
	 * Return an int to which "" should be added to make a string
	 * @param dictionary_GIDs
	 * @param gid
	 * @return
	 */
	public static int addToDictionaryGetIdx(
			ArrayList<String> dictionary_GIDs, String gid) {
		int idx;
		if(gid.length()<=3){
			try{
				int id = Integer.parseInt(gid);
				throw new RuntimeException("Impossible happened: Packing already packed:"+gid);
				//return id;
			}catch(NumberFormatException e){}
		}
		idx = dictionary_GIDs.indexOf(gid);
		if(idx<0) {
			idx = dictionary_GIDs.size();
			dictionary_GIDs.add(gid);
		}
		// slower alternative
		//if(!dictionary_GIDs.contains(gid)) dictionary_GIDs.add(gid);
		//idx = dictionary_GIDs.indexOf(gid);
		if(idx<0) throw new RuntimeException("Impossible happened: Added and still not there:"+gid);
		return idx;
	}
	public static String addToDictionaryGetIdxS(
			ArrayList<String> dictionary_GIDs, String gid){
		return ""+addToDictionaryGetIdx(dictionary_GIDs, gid);
	}
	/**
	 * May fail if data was modified by an external procedure (e.g. called in a decode function, such are recomputing a hash)
	 * @param o
	 * @return
	 */
	public static int getDictionaryIdx(String o) {
		try{
			return new Integer(o).intValue();
		}catch(Exception e) {
			if(DEBUG)System.err.println("\nASNSyncPayload:getDictionaryIdx: probably unpacked: failed searching dictionary for: "+o);
			//e.printStackTrace();
			return -1;
		}
	}
	public String getDictionaryValueOrKeep(String o) {
		return getDictionaryValueOrKeep(dictionary_GIDs, o);
	}
	public static String getDictionaryValueOrKeep(ArrayList<String>dictionary_GIDs, String o) {
		int idx = getDictionaryIdx(o);
		if(idx<0) return o;
		String a;
		try{
			a = dictionary_GIDs.get(idx);
		}catch (Exception e){Util.printCallPath("Payload Dict: gid="+o+" val="+e.getLocalizedMessage());return o;}
		return a;
	}

}

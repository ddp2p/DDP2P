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

package net.ddp2p.common.hds;
import java.util.ArrayList;
import java.util.Calendar;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.ASNNeighborhoodOP;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_FieldValue;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.data.D_PeerOrgs;
import net.ddp2p.common.data.D_PluginData;
import net.ddp2p.common.data.D_Translations;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.SpecificRequest;
import net.ddp2p.common.streaming.WB_Messages;
import net.ddp2p.common.util.Summary;
import net.ddp2p.common.util.Util;
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
	peer_instance [APPLICATION 10] PrintableString OPTIONAL,
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
	public String responderGID; // OPTIONAL
	public D_PeerInstance peer_instance; // the agent instance
	public ASNPluginInfo plugins[]; //AC11 OPTIONAL
	//ASNPluginData plugin_data[]; //AC12 OPTIONAL
	public D_PluginData plugin_data_set; //AC12 OPTIONAL
	public WB_Messages requested; // the data requested in a previous ASNSyncRequest
	public ArrayList<String> dictionary_GIDs =  new ArrayList<String>();
	
	public SpecificRequest advertised;   // the GIDhash of all data that I have upToDate (from last_sync_Date), grouped by org
	public ArrayList<OrgInfo> advertised_orgs; // the GID of new orgs, and their name
	public ArrayList<ResetOrgInfo> changed_orgs; // orgs enabled backwards
	
	// hash of all data I have, to avoid resending it if the hash is the same
	// this currently does not work for votes, as their hash is not containing the actual vote, justification and creation date 
	public ArrayList<OrgsData_Hash> advertised_orgs_hash;
	public byte[] recommendation_testers;

	public boolean empty() {
		if((advertised!=null) && !advertised.empty()) return true;
		return true;
	}
	
	/**
	 * Called before encoding.
	 * Simultaneously adds new GIDs to dictionary and replaces them with their index.
	 * Should not replace them in structures that are permanent: D_Peer, D_PeerOrgs
	 */
	private void prepareDictionary() {
		try{_prepareDictionary();}catch(Exception e){e.printStackTrace();}
	}
	private void _prepareDictionary() {
		// start with peerGIDs, orgGIDs, constGIDs,,,,, ince these are large
		
		/**
		 * I think responderGID can be safely replaced since it is not part of a cached object 
		 */
		if (this.responderGID != null) responderGID = addToDictionaryGetIdxS(dictionary_GIDs, responderGID);
		
		preparePeersDictionary();
		prepareOrgsDictionary();
		
		if (this.requested != null) {
			if (this.requested.cons != null) {
				for (D_Peer p : this.requested.peers)	this.preparePeerDictionary(p);
				for (D_Constituent c : this.requested.cons)	this.prepareConstDictionary(c);
				for (D_Organization o : this.requested.orgs)	this.prepareOrgDictionary(o);
				for (D_Neighborhood n : this.requested.neig)	this.prepareNeigDictionary(n);
				for (D_Witness w : this.requested.witn)	this.prepareWitnDictionary(w);
				for (D_Motion m : this.requested.moti)	this.prepareMotiDictionary(m);
				for (D_Vote v : this.requested.sign)	this.prepareVoteDictionary(v);
				for (D_Justification j : this.requested.just) this.prepareJustDictionary(j);
				for (D_News e : this.requested.news) this.prepareNewsDictionary(e);
				for (D_Translations e : this.requested.tran) this.prepareTranDictionary(e);
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
		if (this.responderGID != null) responderGID = getDictionaryValueOrKeep((responderGID));

		expandPeersDictionariesAtDecoding();
		expandOrgsDictionariesAtDecoding();
		
		if (this.requested != null) {
			if (this.requested.cons !=null) {
				for(D_Peer p : this.requested.peers)	this.expandPeerDictionariesAtDecoding(p);
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
		
		if (o.constituents != null)
			for (net.ddp2p.common.data.ASNConstituentOP c : o.constituents) {
				if (c.constituent==null) continue;
				// the next must be commented since ORG_GID is compressed (or eliminated in the objec's encoder)
				// but should certainly not change cached objects!
				//c.constituent.global_organization_ID = null; 
				if (!STREAMING_SEND_NEIGHBORHOOD_IN_CONSTITUENT) c.constituent.setNeighborhood(null);
				prepareConstDictionary(c.constituent);
			}
		if(o.neighborhoods != null)
			for(ASNNeighborhoodOP n : o.neighborhoods) {
				if(n.neighborhood==null) continue;
				// the next must be commented since ORG_GID is compressed (or eliminated in the objec's encoder)
				//n.neighborhood.setOrgGID(null);
				prepareNeigDictionary(n.neighborhood);
			}
		if(o.witnesses!=null)
			for(D_Witness w : o.witnesses) {
				//w.global_organization_ID = null;
				prepareWitnDictionary(w);}
		if(o.motions!=null)
			for(D_Motion m : o.motions)
			{//m.setOrganizationGID(null);
				prepareMotiDictionary(m);
			}
		if(o.signatures!=null)
			for(D_Vote v : o.signatures) {//v.global_organization_ID = null;
				prepareVoteDictionary(v);
			}
		if(o.justifications!=null)
			for(D_Justification j : o.justifications) {//j.setOrgGID(null);
				prepareJustDictionary(j);
			}
		if(o.news!=null)
			for(D_News e : o.news) {
				//e.global_organization_ID = null;
				prepareNewsDictionary(e);}
		if(o.translations!=null)
			for(D_Translations t : o.translations) {//t.global_organization_ID = null;
				prepareTranDictionary(t);
			}
	}
	public static void prepareOrgDictionary(D_Organization o, ArrayList<String> dictionary_GIDs) {
		//if(o.global_organization_ID!=null) o.global_organization_IDhash=null; // drop hash
		if(o.creator!=null){
			if(STREAMING_SEND_CREATOR_IN_PEER) {
				preparePeerDictionary(o.creator, dictionary_GIDs);
			}else{
				System.err.println("AsnSyncPayload:prepareOrgsDictionary: will drop org creator = "+o.creator);
				//should br done in encoder
				//o.creator=null;
			}
		}

		if(o.getGID() != null) 
			//o.global_organization_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, o.getGID());
		//if(o.global_organization_IDhash != null) 
		//	o.global_organization_IDhash = addToDictionaryGetIdxS(dictionary_GIDs, o.global_organization_IDhash);
		if((o.params != null) && (o.params.creator_global_ID != null))
			//o.params.creator_global_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, o.params.creator_global_ID);

		if((o.params != null) && (o.params.orgParam!=null))
			for(int p=0; p< o.params.orgParam.length; p++) prepareFieldsExtraDictionary(o.params.orgParam[p], dictionary_GIDs);
		
	}
	private void expandOrgDictionariesAtDecoding(D_Organization o) {
		expandOrgDictionariesAtDecoding(o, dictionary_GIDs);

		if(o.constituents != null)
			for(net.ddp2p.common.data.ASNConstituentOP c : o.constituents) {
				if(c.constituent==null) continue;
				expandConstDictionariesAtDecoding(c.constituent);
				c.constituent.setOrganizationGID(o.getGID());
				//c.constituent.neighborhood = ;// cannot reconstruct
			}
		if(o.neighborhoods != null)
			for(ASNNeighborhoodOP n : o.neighborhoods) {
				if(n.neighborhood==null) continue;
				expandNeigDictionariesAtDecoding(n.neighborhood);
				n.neighborhood.setOrgGID(o.getGID());
			}
		if(o.witnesses!=null) 
			for(D_Witness w : o.witnesses) {
				expandWitnDictionariesAtDecoding(w);
				w.global_organization_ID = o.getGID();
			}
		if(o.motions!=null)
			for(D_Motion m : o.motions) {
				expandMotiDictionariesAtDecoding(m);
				m.setOrganizationGID(o.getGID());
			}
		if(o.signatures!=null)
			for(D_Vote v : o.signatures) {
				expandVoteDictionariesAtDecoding(v);
				v.setOrganizationGID(o.getGID());
			}
		if(o.justifications!=null)
			for(D_Justification j : o.justifications) {
				expandJustDictionariesAtDecoding(j);
				j.setOrgGID(o.getGID());
			}
		if(o.news!=null)
			for(D_News e : o.news) {
				expandNewsDictionariesAtDecoding(e);
				e.global_organization_ID = o.getGID();
			}
		if(o.translations!=null)
			for(D_Translations t : o.translations) {
				expandTranDictionariesAtDecoding(t);
				t.global_organization_ID = o.getGID();
			}
	}
	public static void expandOrgDictionariesAtDecoding(D_Organization o, ArrayList<String> dictionary_GIDs) {
		if(o.creator!=null){
			expandPeerDictionariesAtDecoding(o.creator, dictionary_GIDs);
		}
		if(o.getGID() != null) 
			o.setGID(getDictionaryValueOrKeep(dictionary_GIDs, (o.getGID())), null);
		if(o.getGIDH() != null) 
			o.setGIDH(getDictionaryValueOrKeep(dictionary_GIDs, (o.getGIDH())));
		if((o.params != null) && (o.params.creator_global_ID != null))
			o.params.creator_global_ID = getDictionaryValueOrKeep(dictionary_GIDs, (o.params.creator_global_ID));

		// reconstruct hash
		if(o.getGID()!=null){
			o.setGIDH(o.getOrgGIDHashFromGID());
			if(DEBUG){
				String guess = D_Organization.getOrgGIDHashGuess(o.getGID());
				String fix = o.getGIDH();
				if(guess.compareTo(fix)!=0){
					Util.printCallPath("assumption failed: probably the org method is not passed");
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: guess is="+guess);
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: fix is="+fix);
					System.err.println("ASNSyncPayload:expandOrgDictionariesAtDecoding: orig is="+o.getGID());
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
		if (p.global_field_extra_ID != null)
			//p.global_field_extra_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, p.global_field_extra_ID);
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
		/// if (s.global_organization_ID != null) s.setOrgGIDH(null);
		if (s.global_organization_ID != null) // s.global_organization_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, s.global_organization_ID);
		if (s.getOrgGIDH_Or_Null() != null) //s.setOrgGIDH(
			addToDictionaryGetIdxS(dictionary_GIDs, s.getOrgGIDH_Or_Null());//);
	}
	private void prepareServedDictionary(D_PeerOrgs s) {
		prepareServedDictionary(s, dictionary_GIDs);
	}
	public static void prepareServedTableDictionary(D_PeerOrgs s,
			ArrayList<String> dictionary_GIDs) {
		//if (s.global_organization_ID != null) s.setOrgGIDH(null);
		if (s.global_organization_ID != null) //s.global_organization_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, s.global_organization_ID);
		if (s.getOrgGIDH_Or_Null() != null) //s.setOrgGIDH(
			addToDictionaryGetIdxS(dictionary_GIDs, s.getOrgGIDH_Or_Null());
	}
	private void prepareServedTableDictionary(D_PeerOrgs s) {
		prepareServedTableDictionary(s, dictionary_GIDs);
	}
	public void expandServedDictionariesAtDecoding(D_PeerOrgs s) {
		expandServedDictionariesAtDecoding(s, dictionary_GIDs);
	}
	public static void expandServedDictionariesAtDecoding(D_PeerOrgs s,
			ArrayList<String> dictionary_GIDs) {
		if(s.global_organization_ID!=null) s.global_organization_ID = getDictionaryValueOrKeep(dictionary_GIDs, (s.global_organization_ID));
		if(s.getOrgGIDH_Or_Null() != null) s.setOrgGIDH(getDictionaryValueOrKeep(dictionary_GIDs, (s.getOrgGIDH_Or_Null())));
		if(s.global_organization_ID!=null) s.setOrgGIDH(D_Organization.getOrgGIDHashGuess(s.global_organization_ID));
	}

	/*
	if(o.creator.globalID!=null)o.creator.globalIDhash=null;
	if(o.creator.globalID!=null)o.creator.globalID = addToDictionaryGetIdxS(dictionary_GIDs, o.creator.globalID);
	if(o.creator.globalIDhash!=null)o.creator.globalIDhash = addToDictionaryGetIdxS(dictionary_GIDs, o.creator.globalIDhash);
	if(o.creator.served_orgs!=null) for(D_PeerOrgs s: o.creator.served_orgs) prepareServedDictionary(s);
	*/
	public static void preparePeerDictionary(D_Peer p,
			ArrayList<String> dictionary_GIDs) {
		///if (p.component_basic_data.globalID != null) p.component_basic_data.globalIDhash = null;
		if (p.component_basic_data.globalID != null)  // p.component_basic_data.globalID = 
			addToDictionaryGetIdxS(dictionary_GIDs, p.component_basic_data.globalID);
		// no need to compact GIDH since anyhow it is not encoded
		//if (p.component_basic_data.globalIDhash != null) // p.component_basic_data.globalIDhash = 
		//	addToDictionaryGetIdxS(dictionary_GIDs, p.component_basic_data.globalIDhash);
		if (p.served_orgs != null)
			for (D_PeerOrgs _p : p.served_orgs) {
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
	public static void expandPeerDictionariesAtDecoding(D_Peer p,
			ArrayList<String> dictionary_GIDs) {
		if(p.component_basic_data.globalID!=null) p.component_basic_data.globalID = getDictionaryValueOrKeep(dictionary_GIDs, p.component_basic_data.globalID);
		if(p.component_basic_data.globalIDhash!=null) p.component_basic_data.globalIDhash = getDictionaryValueOrKeep(dictionary_GIDs, p.component_basic_data.globalIDhash);
		if(p.component_basic_data.globalID!=null) p.component_basic_data.globalIDhash = D_Peer.getGIDHashFromGID(p.component_basic_data.globalID);
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
	 * TODO: I believe that the translation objects are not yet implemented with caching (and their encoder is not using yet the dictionary).
	 *  Therefore the result of addToDictionaryGetIdxS should still be saved in  GIDs (like e.global_organization_ID), in order for the messages
	 *  to be compressed. I remove that compression only because I am afraid that whoever will implement caching forD_ Translation (as I will
	 *  likely no longer have time for it) will forget to remove these lines and would get in bit troubles debugging afterwards.
	 *  
	 *  TODO: Do not forget! One should implement aversion of the ASN getEncoder() for D_Translation, D_News, D_Justification, and D_Witness
	 *  (the not yet cached objects) such that it takes a dictionary as parameters (as already done for D_Peer, D_COnstituent, ....)
	 *  Then, in the encoder of objects in a ASNPayload, when calling getEncoder on a sub-object of the 4 types above, one must make 
	 *  sure to also pass the dictionary received in parameter (as done for the already cached objects of types D_Peer, etc...).
	 * 
	 * global_translation_ID need not be extracted as it is not referred anywhere else (so its compression with our algorithm is useless)
	 * @param e
	 */
	public static void prepareTranDictionary(D_Translations e, ArrayList<String> dictionary_GIDs) {
		if(e.global_organization_ID!=null) //e.global_organization_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, e.global_organization_ID);
		if(e.global_constituent_ID!=null) //e.global_constituent_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, e.global_constituent_ID);
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
		if(j.getOrgGID()!=null) //j.setOrgGID(
				addToDictionaryGetIdxS(dictionary_GIDs, j.getOrgGID());
		if(j.getConstituentGID()!=null) //j.setConstituentGID(
			addToDictionaryGetIdxS(dictionary_GIDs, j.getConstituentGID());
		if(j.getMotionGID()!=null) //j.setMotionGID(
			addToDictionaryGetIdxS(dictionary_GIDs, j.getMotionGID());
		if(j.getGID()!=null) //j.setGID(
			addToDictionaryGetIdxS(dictionary_GIDs, j.getGID());
		if(j.getAnswerTo_GID()!=null) //j.setAnswerTo_GID(
			addToDictionaryGetIdxS(dictionary_GIDs, j.getAnswerTo_GID());
		
		if (j.getConstituent() != null) prepareConstDictionary(j.getConstituent(), dictionary_GIDs);
		if (j.getMotion() != null) prepareMotiDictionary(j.getMotion(), dictionary_GIDs);
		if (j.getAnswerTo() != null) prepareJustDictionary(j.getAnswerTo(), dictionary_GIDs);
	}
	private void prepareJustDictionary(D_Justification j) {
		prepareJustDictionary(j, dictionary_GIDs);
	}
	private void expandJustDictionariesAtDecoding(D_Justification j) {
		expandJustDictionariesAtDecoding(j, dictionary_GIDs);
	}
	public static void expandJustDictionariesAtDecoding(D_Justification j, ArrayList<String> dictionary_GIDs) {
		if(j.getOrgGID()!=null) j.setOrgGID(getDictionaryValueOrKeep(dictionary_GIDs, (j.getOrgGID())));
		if(j.getConstituentGID()!=null) j.setConstituentGID(getDictionaryValueOrKeep(dictionary_GIDs, (j.getConstituentGID())));
		if(j.getMotionGID()!=null) j.setMotionGID(getDictionaryValueOrKeep(dictionary_GIDs, (j.getMotionGID())));
		if(j.getGID()!=null) j.setGID(getDictionaryValueOrKeep(dictionary_GIDs, (j.getGID()))); // removed registration
		if(j.getAnswerTo_GID()!=null) j.setAnswerTo_GID(getDictionaryValueOrKeep(dictionary_GIDs, (j.getAnswerTo_GID())));
		
		if(j.getConstituent()!=null) expandConstDictionariesAtDecoding(j.getConstituent(), dictionary_GIDs);
		if(j.getMotion()!=null) expandMotiDictionariesAtDecoding(j.getMotion(), dictionary_GIDs);
	}
	/**
	 * TODO: as for the comment to D_Translations, votes are not yet implemented with caching and their encoding do not use dictionarius.
	 *  So compression is not yet done. I comment compression  (assignment to GIDs) to avoid troubles for others
	 * 
	 * VoteID is not extracted as it is not typically referred anywhere. Need not even be sent at all here, except as checksums
	 * @param v
	 * @param dictionary_GIDs
	 */

	public static void prepareVoteDictionary(D_Vote v, ArrayList<String> dictionary_GIDs) {
		if(v.getOrganizationGID()!=null) // v.global_organization_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, v.getOrganizationGID());
		if(v.getConstituentGID()!=null) //v.global_constituent_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, v.getConstituentGID());
		if(v.getMotionGID() != null) //v.global_motion_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, v.getMotionGID());
		if(v.getJustificationGID()!=null) //v.global_justification_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, v.getJustificationGID());
		//v.global_vote_ID = null;
		//if(v.global_vote_ID!=null) v.global_vote_ID = addToDictionaryGetIdxS(dictionary_GIDs, v.global_vote_ID);
		if(v.getConstituent_force()!=null) ASNSyncPayload.prepareConstDictionary(v.getConstituent_force(), dictionary_GIDs);
		if(v.getMotionFromObjOrLID()!=null) ASNSyncPayload.prepareMotiDictionary(v.getMotionFromObjOrLID(), dictionary_GIDs);
		if(v.getJustificationFromObjOrLID()!=null) ASNSyncPayload.prepareJustDictionary(v.getJustificationFromObjOrLID(), dictionary_GIDs);
	}
	private void prepareVoteDictionary(D_Vote v) {
		prepareVoteDictionary(v, dictionary_GIDs);
	}
	private void expandVoteDictionariesAtDecoding(D_Vote v) {
		expandVoteDictionariesAtDecoding(v, dictionary_GIDs);
	}
	public static void expandVoteDictionariesAtDecoding(D_Vote v, ArrayList<String> dictionary_GIDs) {
		if(v.getOrganizationGID()!=null) v.setOrganizationGID(getDictionaryValueOrKeep(dictionary_GIDs,(v.getOrganizationGID())));
		if(v.getConstituentGID()!=null) v.setConstituentGID(getDictionaryValueOrKeep(dictionary_GIDs,(v.getConstituentGID())));
		if(v.getMotionGID()!=null) v.setMotionGID(getDictionaryValueOrKeep(dictionary_GIDs,(v.getMotionGID())));
		if(v.getJustificationGID()!=null) v.setJustificationGID(getDictionaryValueOrKeep(dictionary_GIDs,(v.getJustificationGID())));
		//v.global_vote_ID = v.make_ID();
		//if(v.global_vote_ID!=null) v.global_vote_ID = getDictionaryValueOrKeep(dictionary_GIDs,(v.global_vote_ID));
		if(v.getConstituent_force()!=null) ASNSyncPayload.expandConstDictionariesAtDecoding(v.getConstituent_force(), dictionary_GIDs);
		if(v.getMotionFromObjOrLID()!=null) ASNSyncPayload.expandMotiDictionariesAtDecoding(v.getMotionFromObjOrLID(), dictionary_GIDs);
		if(v.getJustificationFromObjOrLID()!=null) ASNSyncPayload.expandJustDictionariesAtDecoding(v.getJustificationFromObjOrLID(), dictionary_GIDs);
	}
	/**
	 * TODO, just as with the Translations with Votes
	 * 
	 *
	 * 
	 * NewsID is not extracted as it is not referred
	 * @param e
	 */
	public static void prepareNewsDictionary(D_News e, ArrayList<String> dictionary_GIDs) {
		if(e.global_organization_ID!=null) //e.global_organization_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, e.global_organization_ID);
		if(e.global_constituent_ID!=null) //e.global_constituent_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, e.global_constituent_ID);
		if(e.global_motion_ID!=null) //e.global_motion_ID = 
				addToDictionaryGetIdxS(dictionary_GIDs, e.global_motion_ID);
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
		if (m.getOrganizationGID_force()!=null) //m.setOrganizationGID(
				addToDictionaryGetIdxS(dictionary_GIDs, m.getOrganizationGID_force());
		if (m.getConstituentGID()!=null) //m.setConstituentGID(
			addToDictionaryGetIdxS(dictionary_GIDs, m.getConstituentGID()); //);
		if (m.getGID() != null) //m.setGID(
				addToDictionaryGetIdxS(dictionary_GIDs, m.getGID());
		if (m.getEnhancedMotionGID()!=null) //m.setEnhancedMotionGID(
			addToDictionaryGetIdxS(dictionary_GIDs, m.getEnhancedMotionGID());
		
		if (m.getConstituent()!=null) prepareConstDictionary(m.getConstituent(), dictionary_GIDs);
	}
	private void prepareMotiDictionary(D_Motion m) {
		prepareMotiDictionary(m, this.dictionary_GIDs);
	}
	private void expandMotiDictionariesAtDecoding(D_Motion m) {
		expandMotiDictionariesAtDecoding(m, dictionary_GIDs);
	}
	public static void expandMotiDictionariesAtDecoding(D_Motion m, ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+m);
		if(m.getOrganizationGID_force()!=null) m.setOrganizationGID(getDictionaryValueOrKeep(dictionary_GIDs, (m.getOrganizationGID_force())));
		if(m.getConstituentGID()!=null) m.setConstituentGID(getDictionaryValueOrKeep(dictionary_GIDs, (m.getConstituentGID())));
		if(m.getGID()!=null) m._setGID(getDictionaryValueOrKeep(dictionary_GIDs, (m.getGID())));
		if(m.getEnhancedMotionGID()!=null) m.setEnhancedMotionGID(getDictionaryValueOrKeep(dictionary_GIDs, (m.getEnhancedMotionGID())));
		
		if(m.getConstituent()!=null) expandConstDictionariesAtDecoding(m.getConstituent(), dictionary_GIDs);
	}
	/**
	 * TODO: Just as for News, Votes and Translations
	 * 
	 * WitnessID needs not be extracts as it is not referred
	 * @param w
	 * @param dictionary_GIDs
	 */
	public static void prepareWitnDictionary(D_Witness w,
			ArrayList<String> dictionary_GIDs) {
		if(w.global_organization_ID!=null) //w.global_organization_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, w.global_organization_ID);
		//if(w.global_witness_ID!=null) w.global_witness_ID = addToDictionaryGetIdxS(dictionary_GIDs, w.global_witness_ID);
		if(w.witnessed_global_neighborhoodID!=null) //w.witnessed_global_neighborhoodID = 
			addToDictionaryGetIdxS(dictionary_GIDs, w.witnessed_global_neighborhoodID);
		if(w.witnessing_global_constituentID!=null) //w.witnessing_global_constituentID = 
			addToDictionaryGetIdxS(dictionary_GIDs, w.witnessing_global_constituentID);
		if(w.witnessed_global_constituentID!=null) //w.witnessed_global_constituentID = 
			addToDictionaryGetIdxS(dictionary_GIDs, w.witnessed_global_constituentID);
		
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
		if(n.getOrgGID()!=null) // n.setOrgGID(
				addToDictionaryGetIdxS(dictionary_GIDs, n.getOrgGID()); //);
		if(n.getGID()!=null) // n.setGID(
			addToDictionaryGetIdxS(dictionary_GIDs, n.getGID()); //);
		if(n.getSubmitter_GID()!=null) // n.setSubmitter_GID(
			addToDictionaryGetIdxS(dictionary_GIDs, n.getSubmitter_GID()); //);
		if(n.getParent_GID()!=null) // n.setParent_GID(
			addToDictionaryGetIdxS(dictionary_GIDs, n.getParent_GID()); //);
		
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
		if(n.getOrgGID()!=null) n.setOrgGID(getDictionaryValueOrKeep(dictionary_GIDs, (n.getOrgGID())));
		if(n.getGID()!=null) n.setGID(getDictionaryValueOrKeep(dictionary_GIDs, (n.getGID())));
		if(n.getSubmitter_GID()!=null) n.setSubmitter_GID(getDictionaryValueOrKeep(dictionary_GIDs, (n.getSubmitter_GID())));
		if(n.getParent_GID()!=null) n.setParent_GID(getDictionaryValueOrKeep(dictionary_GIDs, (n.getParent_GID())));
		
		if(n.parent!=null) expandNeigDictionariesAtDecoding(n.parent, dictionary_GIDs);
		if(n.submitter!=null) expandConstDictionariesAtDecoding(n.submitter, dictionary_GIDs);
	}

	public static void prepareConstDictionary(D_Constituent c, ArrayList<String> dictionary_GIDs) {
		if(c==null)return;
		//if(c.global_constituent_id!=null) c.global_constituent_id_hash=null;
		if(c.getOrganizationGID()!=null) //c.global_organization_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, c.getOrganizationGID());
		if(c.getNeighborhoodGID()!=null) //c.global_neighborhood_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, c.getNeighborhoodGID());
		if(c.getGID()!=null) //c.global_constituent_id =
			addToDictionaryGetIdxS(dictionary_GIDs, c.getGID());
		//if(c.global_constituent_id_hash!=null) c.global_constituent_id_hash = addToDictionaryGetIdxS(dictionary_GIDs, c.global_constituent_id_hash);
		if(c.getSubmitterGID()!=null) //c.global_submitter_id = 
			addToDictionaryGetIdxS(dictionary_GIDs, c.getSubmitterGID());
		if(c.address!=null) for(int k =0; k<c.address.length; k++) prepareValueDictionaries(c.address[k], dictionary_GIDs);
		
		if(c.getNeighborhood()!=null) for(int k=0; k<c.getNeighborhood().length; k++) prepareNeigDictionary(c.getNeighborhood()[k], dictionary_GIDs);
		if(c.getSubmitter()!=null) prepareConstDictionary(c.getSubmitter(), dictionary_GIDs);
		if(DEBUG)System.out.println("ASNSyncPayload:Prepared: "+c);
	}
	private void prepareConstDictionary(D_Constituent c) {
		prepareConstDictionary(c, dictionary_GIDs);
	}
	private void preparePeerDictionary(D_Peer p) {
		preparePeerDictionary(p, dictionary_GIDs);
	}
	private void expandConstDictionariesAtDecoding(D_Constituent c) {
		expandConstDictionariesAtDecoding(c, dictionary_GIDs);
	}
	private void expandPeerDictionariesAtDecoding(D_Peer p) {
		expandPeerDictionariesAtDecoding(p, dictionary_GIDs);
	}
	public static void expandConstDictionariesAtDecoding(
			D_Constituent c, ArrayList<String> dictionary_GIDs) {
		if(DEBUG)System.out.println("ASNSyncPayload:Expanding: "+c);
		if(c==null) return;
		if(c.getOrganizationGID()!=null) c.setOrganizationGID(getDictionaryValueOrKeep(dictionary_GIDs, (c.getOrganizationGID())));
		if(c.getNeighborhoodGID()!=null) c.setNeighborhoodGID(getDictionaryValueOrKeep(dictionary_GIDs, (c.getNeighborhoodGID())));
		if(c.getGID()!=null) c._set_GID(getDictionaryValueOrKeep(dictionary_GIDs, (c.getGID())));
		if(c.getGIDH()!=null) c.setGIDH(getDictionaryValueOrKeep(dictionary_GIDs, (c.getGIDH())));
		if(c.getSubmitterGID()!=null) c.setSubmitterGID(getDictionaryValueOrKeep(dictionary_GIDs, (c.getSubmitterGID())));
		if(c.address!=null) for(int k =0; k<c.address.length; k++) expandValuedictionaries(c.address[k], dictionary_GIDs);
		if(c.getGID()!=null)c.setGIDH(D_Constituent.getGIDHashFromGID(c.getGID()));
		
		if(c.getNeighborhood()!=null) for(int k=0; k<c.getNeighborhood().length; k++) expandNeigDictionariesAtDecoding(c.getNeighborhood()[k], dictionary_GIDs);
		if(c.getSubmitter()!=null) expandConstDictionariesAtDecoding(c.getSubmitter(), dictionary_GIDs);
	}

	private static void prepareValueDictionaries(D_FieldValue v, ArrayList<String> dictionary_GIDs) {
		if(v.global_neighborhood_ID!=null) //v.global_neighborhood_ID = 
			addToDictionaryGetIdxS(dictionary_GIDs, v.global_neighborhood_ID);
		if(v.field_extra_GID!=null) //v.field_extra_GID = 
			addToDictionaryGetIdxS(dictionary_GIDs, v.field_extra_GID);
		if(v.field_GID_above!=null) //v.field_GID_above = 
			addToDictionaryGetIdxS(dictionary_GIDs, v.field_GID_above);
		if(v.field_GID_default_next!=null) //v.field_GID_default_next = 
			addToDictionaryGetIdxS(dictionary_GIDs, v.field_GID_default_next);
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
		if (tables != null) {
			for (Table t: tables.tables) {
				if (t.name.equals(net.ddp2p.common.table.peer.G_TNAME)) {
					for (byte[][] row : t.rows) {
						//row[0]; // peerGID
						//row[5]; // orgs
						if (row[0] != null) { // replace GID with appropriate
							String peerGID = Util.getBString(row[0]);
							row[0] = new Encoder(addToDictionaryGetIdx(dictionary_GIDs, peerGID)).getBytes();
						}
						if (row[5] != null) { // replace orgs
							String global_organizationIDs = Util.getBString(row[5]);
							D_PeerOrgs[] served_orgs = net.ddp2p.common.table.peer_org.peerOrgsFromString(global_organizationIDs);
							if (served_orgs != null) {
								for (D_PeerOrgs s:served_orgs) prepareServedTableDictionary(s);
								String orgs = net.ddp2p.common.table.peer_org.stringFromPeerOrgs(served_orgs);
								row[5] = net.ddp2p.common.streaming.UpdatePeersTable.getFieldData(orgs);
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
				if(t.name.equals(net.ddp2p.common.table.peer.G_TNAME)) {
					for(byte[][] row : t.rows) {
						//row[0]; // peerGID
						//row[5]; // orgs
						if(row[0]!=null){
							int peerID = new Decoder(row[0]).getInteger().intValue();
							String pGID = dictionary_GIDs.get(peerID);
							if(pGID==null){Util.printCallPath("Unexpected case");continue;}
							row[0] = net.ddp2p.common.streaming.UpdatePeersTable.getFieldData(pGID);
						}
						if(row[5]!=null){
							String global_organizationIDs = Util.getBString(row[5]);
							D_PeerOrgs[] served_orgs = net.ddp2p.common.table.peer_org.peerOrgsFromString(global_organizationIDs);
							if(served_orgs!=null){
								for(D_PeerOrgs s:served_orgs) expandServedDictionariesAtDecoding(s);
								String orgs = net.ddp2p.common.table.peer_org.stringFromPeerOrgs(served_orgs);
								row[5] = net.ddp2p.common.streaming.UpdatePeersTable.getFieldData(orgs);
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
		responderGID = globalID;
	}
	public ASNSyncPayload(String last_sync_date) {
		upToDate = Util.getCalendar(last_sync_date);
	}
	public String toString(){
		String result = "[SyncAnswer v="+version+"] "+" upTo:"+Encoder.getGeneralizedTime(upToDate)+
		" tables="+tables;
		result += "\n orgData= ["+Util.concat(orgData," ")+"]";
		result += "\n orgCRL="+orgCRL;
		result += "\n responderID="+Util.trimmed(responderGID);
		result += "\n plugins="+Util.trimmed(""+Util.nullDiscrimArray(plugins,":"), 400);
		//result += "\n plugin_data="+Util.trimmed(plugin_data+"");
		result += "\n plugin_msg="+Util.trimmed(plugin_data_set+"", 400);
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
		result += "\n responderID="+Util.trimmed(responderGID);
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
	@Override
	public Encoder getEncoder() {
//		return getEncoder(new ArrayList<String>());
//	}
//	@Override
//	public Encoder getEncoder(ArrayList<String> _dictionary_GIDs) {
		if (ASNSyncRequest.DEBUG) System.out.println("Encoding SyncAnswer");
		
		prepareDictionary();
		
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if (upToDate != null) enc.addToSequence(new Encoder(upToDate));
		if (tables != null) enc.addToSequence(tables.getEncoder().setASN1Type(DD.TAG_AC0));
		 
		//if(orgData!=null) enc.addToSequence(orgData.getEncoder().setASN1Type(DD.TAG_AC1));
		if (orgData != null) enc.addToSequence(Encoder.getEncoder(orgData, this.dictionary_GIDs).setASN1Type(DD.TAG_AC1));
		if (orgCRL != null) enc.addToSequence(orgCRL.getEncoder().setASN1Type(DD.TAG_AC2));
		//enc.addToSequence(new Encoder(true));
		if (responderGID != null) enc.addToSequence(new Encoder(responderGID,false));
		if (plugins != null) enc.addToSequence(Encoder.getEncoder(plugins).setASN1Type(DD.TAG_AC11));
		//if(plugin_data!=null) enc.addToSequence(Encoder.getEncoder(plugin_data).setASN1Type(DD.TAG_AC12));
		if (plugin_data_set != null) enc.addToSequence(plugin_data_set.getEncoder().setASN1Type(DD.TAG_AC12));
		if (requested != null) enc.addToSequence(requested.getEncoder(this.dictionary_GIDs).setASN1Type(DD.TAG_AC13));
		if (advertised != null) enc.addToSequence(advertised.getEncoder().setASN1Type(DD.TAG_AC14));
		if (advertised_orgs_hash != null) enc.addToSequence(Encoder.getEncoder(advertised_orgs_hash.toArray(new OrgsData_Hash[0])).setASN1Type(DD.TAG_AC15));
		if (advertised_orgs != null) enc.addToSequence(Encoder.getEncoder(advertised_orgs.toArray(new OrgInfo[0])).setASN1Type(DD.TAG_AC16));
		if (this.dictionary_GIDs != null) {
			if (ASNSyncRequest.DEBUG) System.out.println("\n\nEncoded SyncAnswer: dicts: #" + this.dictionary_GIDs.size());
			enc.addToSequence(Encoder.getStringEncoder(this.dictionary_GIDs.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC17));
		}
		if (changed_orgs != null) {
			ResetOrgInfo[] ch_orgs = changed_orgs.toArray(new ResetOrgInfo[0]);
			enc.addToSequence(Encoder.getEncoder(ch_orgs).setASN1Type(DD.TAG_AC18));
		}
		if (peer_instance != null) enc.addToSequence(peer_instance.getEncoder().setASN1Type(DD.TAG_AC10));
		
		if (recommendation_testers != null) enc.addToSequence(new Encoder(recommendation_testers).setASN1Type(DD.TAG_AC20)); 
		
		enc.setASN1Type(getASN1Type());
		if (ASNSyncRequest.DEBUG) System.out.println("\n\nEncoded SyncAnswer: "+this);
		//this.expandDictionariesAtDecoding(); // redundant now!
		//if (true || ASNSyncRequest.DEBUG) System.out.println("\n\nEncoded SyncAnswer2: "+this);
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
		if(dec.getTypeByte()==DD.TAG_AC1) orgData=(D_Organization[]) dec.getFirstObject(true).getSequenceOf(D_Organization.getASN1Type(), new D_Organization[0], D_Organization.getEmpty());
		if(dec.getTypeByte()==DD.TAG_AC2) orgCRL=new OrgCRL().decode(dec.getFirstObject(true));
		//dec.getFirstObject(true,Encoder.TAG_BOOLEAN).getBoolean();
		//if(dec.getFirstObject(false)!=null) 
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)  responderGID=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC11) plugins = (ASNPluginInfo[]) dec.getFirstObject(true).getSequenceOf(ASNPluginInfo.getASN1Type(), new ASNPluginInfo[0], new ASNPluginInfo());
		//if(dec.getTypeByte()==DD.TAG_AC12) plugin_data = (ASNPluginData[]) dec.getFirstObject(true).getSequenceOf(ASNPluginData.getASN1Type(), new ASNPluginData[0], new ASNPluginData());
		if(dec.getTypeByte()==DD.TAG_AC12) plugin_data_set = new D_PluginData().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC13) {
			if(DEBUG)System.out.println("***********ASNSyncPayload:Decoded SyncAnswer: Found req ");
			requested = new WB_Messages().decode(dec.getFirstObject(true));
		}
		if(dec.getTypeByte()==DD.TAG_AC14) {
			if(DEBUG)System.out.println("***********ASNSyncPayload:Decoded SyncAnswer: Found adv ");
			advertised=new SpecificRequest().decode(dec.getFirstObject(true));
		}
		if(dec.getTypeByte()==DD.TAG_AC15) { // none?
			if(DEBUG)System.out.println("***********ASNSyncPayload:Decoded SyncAnswer: Found adv_orgh ");
			advertised_orgs_hash=dec.getFirstObject(true).getSequenceOfAL(OrgsData_Hash.getASN1Type(), new OrgsData_Hash());
		}
		if(dec.getTypeByte()==DD.TAG_AC16) { // none?
			if(DEBUG)System.out.println("***********ASNSyncPayload:Decoded SyncAnswer: Found adv_org ");
			advertised_orgs=dec.getFirstObject(true).getSequenceOfAL(OrgInfo.getASN1Type(),new OrgInfo());
		}
		if(dec.getTypeByte()==DD.TAG_AC17) {
			if(DEBUG)System.out.println("***********ASNSyncPayload:Decoded SyncAnswer: Found dict ");
			dictionary_GIDs=dec.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		}
		if(dec.getTypeByte()==DD.TAG_AC18) {
			if(DEBUG)System.out.println("***********ASNSyncPayload:Decoded SyncAnswer: Found changed ");
			changed_orgs=dec.getFirstObject(true).getSequenceOfAL(ResetOrgInfo.getASN1Type(),new ResetOrgInfo());
		}
		if(dec.getTypeByte()==DD.TAG_AC10) peer_instance=new D_PeerInstance().decode(dec.getFirstObject(true)); //(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC20) recommendation_testers = dec.getFirstObject(true).getBytes(DD.TAG_AC20);

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
	public static int getIdx(
			ArrayList<String> dictionary_GIDs, String gid) {
		int idx;
		if (gid.length() <= 3) {
			try{
				int id = Integer.parseInt(gid);
				throw new RuntimeException("Impossible happened: Packing already packed:"+gid);
				//return id;
			} catch(NumberFormatException e){}
		}
		idx = dictionary_GIDs.indexOf(gid);
		return idx;
	}
	/**
	 * Returns gid if not in dictionary, alse return index
	 * @param dictionary_GIDs
	 * @param gid
	 * @return
	 */
	public static String getIdxS(
			ArrayList<String> dictionary_GIDs, String gid){
		if (gid == null) return null;
		int r = getIdx(dictionary_GIDs, gid);
		if (r < 0) return gid;
		return "" + r;
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
		if (idx < 0) return o;
		String a;
		try {
			a = dictionary_GIDs.get(idx);
		} catch (Exception e) { Util.printCallPath("Payload Dict: gid="+o+" val="+e.getLocalizedMessage());return o;}
		return a;
	}
	public String get_peer_instance() {
		if (peer_instance == null) return null;
		return peer_instance.peer_instance;
	}

}

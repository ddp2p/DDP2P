/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Ossama Dhanoon
		Author: Ossama Dhanoon : odhanoon2011@my.fit.edu
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

package data;



import handling_wb.ReceivedBroadcastableMessages;
import hds.ASNSyncPayload;

import java.util.ArrayList;
import java.util.Calendar;

import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
WB_Message ::= SEQUENCE {
	sender WB_Peer,
	interest WB_INTEREST [0] OPTIONAL,
	organization WB_ORGANIZATION,
	motion WB_MOTION [1] OPTIONAL,
	constituent WB_Constituent,
	vote WB_VOTE [2] OPTIONAL,
	signature OCTET_STRING
}
 */

public class D_Message extends ASNObj {

	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public D_Peer sender;
	public ArrayList<String> recent_senders;
	public D_Peer Peer;
	public WB_ASN_Interest interest;
	public D_Organization organization;
	public D_Constituent constituent;
	public D_Witness witness;
	public D_Motion motion;
	public D_Vote vote;
	public D_News news; // TODO
	public D_Translations translations[]; // TODO
	public D_PluginData plugins; // TODO
	public D_Neighborhood neighborhoods[];
	public String signature; //OCT STR
	
	public ArrayList<String> dictionary_GIDs =  new ArrayList<String>();
	
	/**
	 * Called before encoding
	 */
	public void prepareDictionary() {
		try{_prepareDictionary();}catch(Exception e){e.printStackTrace();}
	}
	public void _prepareDictionary() {
		if(sender!=null) ASNSyncPayload.preparePeerDictionary(sender, dictionary_GIDs);
		if(recent_senders != null)
			for(int k=0; k<this.recent_senders.size(); k++) {
				String gid = this.recent_senders.get(k);
				recent_senders.set(k, ""+ASNSyncPayload.addToDictionaryGetIdx(dictionary_GIDs, gid));	
			}
		if(Peer!=null) ASNSyncPayload.preparePeerDictionary(Peer, dictionary_GIDs);
		if(organization!=null) ASNSyncPayload.prepareOrgDictionary(organization, dictionary_GIDs);
		if(constituent!=null)ASNSyncPayload.prepareConstDictionary(constituent, dictionary_GIDs);
		if(neighborhoods!=null) {
			for(int k=0; k<neighborhoods.length; k++)
				ASNSyncPayload.prepareNeigDictionary(neighborhoods[k], dictionary_GIDs);
		}
		if(witness!=null) ASNSyncPayload.prepareWitnDictionary(witness, dictionary_GIDs);
		if(motion!=null)ASNSyncPayload.prepareMotiDictionary(motion, dictionary_GIDs);
		if(vote!=null)ASNSyncPayload.prepareVoteDictionary(vote, dictionary_GIDs);
		//this.prepareJustDictionary(j, dictionary_GIDs);
		//this.prepareNewsDictionary(e, dictionary_GIDs);
		//this.prepareTranDictionary(e, dictionary_GIDs);
	}
	public void expandDictionariesAtDecoding() {
		try{_expandDictionariesAtDecoding();}catch(Exception e){e.printStackTrace();}
	}
	public void _expandDictionariesAtDecoding() {
		if(sender!=null) ASNSyncPayload.expandPeerDictionariesAtDecoding(sender, dictionary_GIDs);
		if(recent_senders != null)
			for(int k=0; k<this.recent_senders.size(); k++) {
				String gid = this.recent_senders.get(k);
				recent_senders.set(k, ""+ASNSyncPayload.getDictionaryValueOrKeep(dictionary_GIDs, gid));	
			}
		if(Peer!=null) ASNSyncPayload.expandPeerDictionariesAtDecoding(Peer, dictionary_GIDs);
		if(organization!=null)ASNSyncPayload.expandOrgDictionariesAtDecoding(organization, dictionary_GIDs);
		if(constituent!=null)ASNSyncPayload.expandConstDictionariesAtDecoding(constituent, dictionary_GIDs);
		if(neighborhoods!=null) {
			for(int k=0; k<neighborhoods.length; k++)
				ASNSyncPayload.expandNeigDictionariesAtDecoding(neighborhoods[k], dictionary_GIDs);
		}
		if(witness!=null)ASNSyncPayload.expandWitnDictionariesAtDecoding(witness, dictionary_GIDs);
		if(motion!=null)ASNSyncPayload.expandMotiDictionariesAtDecoding(motion, dictionary_GIDs);
		if(vote!=null)ASNSyncPayload.expandVoteDictionariesAtDecoding(vote, dictionary_GIDs);
		//this.expandJustDictionariesAtDecoding(j, dictionary_GIDs);
		//this.expandNewsDictionariesAtDecoding(e, dictionary_GIDs);
		//this.expandTranDictionariesAtDecoding(e, dictionary_GIDs);
	}

	public D_Message(){
		recent_senders = ReceivedBroadcastableMessages.my_bag_of_peers;
	}

	@Override
	public Encoder getEncoder() {
		int dependants = 10; // could be ASNObj.DEPENDANTS_ALL, but risks being too big;
		
		if (DD.ADHOC_MESSAGES_USE_DICTIONARIES){
			if (dictionary_GIDs.size() > 0) {
				System.err.println("D_Message:getEncoder: compacting dictionaries: prior dicts were not empty");
				dictionary_GIDs = new ArrayList<String>();
			}
			this.prepareDictionary();
			if (ASNSyncPayload.DEBUG || DEBUG) System.out.println("D_Message:getEncoder:compacted dictionaries:"+this);
		} else {
			if (dictionary_GIDs.size() > 0) System.err.println("D_Message:getEncoder: not compacting dictionaries: prior dicts were not empty");
			dictionary_GIDs = new ArrayList<String>();
			if (_DEBUG) System.out.println("D_Message:getEncoder: not compacting dictionaries");
		}
		
		Encoder enc = new Encoder().initSequence();
		if (sender != null) enc.addToSequence(sender.getEncoder(dictionary_GIDs).setASN1Type(DD.TAG_AC0));
		if (Peer != null) enc.addToSequence(Peer.getEncoder(dictionary_GIDs).setASN1Type(DD.TAG_AC1));
		if (interest != null) enc.addToSequence(interest.getEncoder(dictionary_GIDs).setASN1Type(DD.TAG_AC2));

		// encoded with dependants
		if (organization != null) enc.addToSequence(organization.getEncoder(dictionary_GIDs, dependants).setASN1Type(DD.TAG_AC3));
		if (motion!=null) enc.addToSequence(motion.getEncoder(dictionary_GIDs, dependants).setASN1Type(DD.TAG_AC4));
		if (constituent!=null) enc.addToSequence(constituent.getEncoder(dictionary_GIDs, dependants).setASN1Type(DD.TAG_AC5));
		if (witness!=null) enc.addToSequence(witness.getEncoder(dictionary_GIDs, dependants).setASN1Type(DD.TAG_AC6));
		if (vote!=null)enc.addToSequence(vote.getEncoder(dictionary_GIDs, dependants).setASN1Type(DD.TAG_AC7));
		
		if (signature != null) enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));

		// encoded with dependants
		if (neighborhoods != null) enc.addToSequence(Encoder.getEncoder(neighborhoods, dictionary_GIDs, dependants).setASN1Type(DD.TAG_AC9));

		if (recent_senders != null) {
			String[] senders = recent_senders.toArray(new String[0]);
			enc.addToSequence(Encoder.getStringEncoder(senders, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		}
		
		if (dictionary_GIDs != null) enc.addToSequence(Encoder.getStringEncoder(dictionary_GIDs.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC17));

		if (DD.ADHOC_MESSAGES_USE_DICTIONARIES)  this.expandDictionariesAtDecoding();
		if (dictionary_GIDs != null) dictionary_GIDs = new ArrayList<String>();
		
		return enc;
	}

	@Override
	public D_Message decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0) sender = 
				//D_Peer.getPeerIfValid(dec.getFirstObject(true));//
				D_Peer.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC1)Peer = //D_Peer.getPeerIfValid(dec.getFirstObject(true));//
				D_Peer.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC2) interest = new WB_ASN_Interest().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC3)organization = D_Organization.getOrgFromDecoder(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC4)motion = D_Motion.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC5)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC6)witness = new D_Witness().decode(dec.getFirstObject(true)); 
		if(dec.getTypeByte()==DD.TAG_AC7)vote = new D_Vote().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC8)signature = dec.getFirstObject(true).getString(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)neighborhoods = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Neighborhood[]{}, D_Neighborhood.getEmpty());
		if(dec.getTypeByte()==DD.TAG_AC10)recent_senders = dec.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);

		if(dec.getTypeByte()==DD.TAG_AC17) dictionary_GIDs=dec.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);

		if(//DD.ADHOC_MESSAGES_USE_DICTIONARIES || 
				( (dictionary_GIDs!=null) && (dictionary_GIDs.size()>0))){
			if(ASNSyncPayload.DEBUG||DEBUG) System.out.println("D_Message:decode:compacted dictionaries:"+this);
			expandDictionariesAtDecoding();
		}
		
		return this;
	}
}

/**
WB_INTEREST ::= SEQUENCE {
	organizations [0] SEQUENCE OF ORGID OPTIONAL,
	motions [1] SEQUENCE OF MOTIONID OPTIONAL,
	constituents [2] SEQUENCE OF CONSTITUENTID OPTIONAL,
	justifications [3] SEQUENCE OF JUSTIFICATIONID OPTIONAL
}
ORGID ::= PrintableString
MOTIONID ::= PrintableString
CONSTITUENTID ::= PrintableString
JUSTIFICATIONID ::= PrintableString

 */

class WB_ASN_Interest extends ASNObj{

	String organizations[];//Printable
	String motions[];//Printable
	String constituents[];//Printable
	String justifications[];//Printable
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(organizations!=null) enc.addToSequence(Encoder.getStringEncoder(organizations, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(motions!=null) enc.addToSequence(Encoder.getStringEncoder(motions, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(constituents!=null) enc.addToSequence(Encoder.getStringEncoder(constituents, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(justifications!=null) enc.addToSequence(Encoder.getStringEncoder(justifications, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		return enc;
	}

	@Override
	public WB_ASN_Interest decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec=decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0) organizations =dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC1) motions = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC2) constituents = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC3) justifications = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		return this;
	}

}



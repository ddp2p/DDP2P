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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Summary;
import net.ddp2p.common.util.Util;

/**
 * Structure encapsulating an ArrayList of RequestData.
 * Each request data contains the data to be requested-from/advertised-to
 * a given peer, with respect to an organization.
 * @author msilaghi
 *
 */
public class SpecificRequest extends ASNObj implements Summary{
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	public ArrayList<RequestData> rd = new ArrayList<RequestData>();
	public Hashtable<String,String> peers = new Hashtable<String,String>(); // these are not part of some organization
	public ArrayList<String> news = new ArrayList<String>(); // these are not part of some organization
	public ArrayList<String> tran = new ArrayList<String>(); // these are not part of some organization

	public boolean empty() {
//		if (peers.size() > 0) return false;
//		if (news.size() > 0) return false;
//		if (tran.size() > 0) return false;
		if (rd.isEmpty()) return true;
		for (RequestData a : rd) if ( ! a.empty() ) return false;
		return true;
	}	
	public boolean emptyGlobal() {
		if (peers.size() > 0) return false;
		if (news.size() > 0) return false;
		if (tran.size() > 0) return false;
//		if (rd.isEmpty()) return true;
//		for (RequestData a : rd) if ( ! a.empty() ) return false;
		return true;
	}	
	/*
	String[] org_GIDs;
	String[] constituent_GIDs;
	String[] neighborhood_GIDs;
	//String[] witnessing_GIDs;
	String[] motion_GIDs;
	String[] justification_GIDs;
	*/
	public SpecificRequest () {}
	public SpecificRequest(RequestData rq) {
		/*
		org_GIDs = rq.orgs.toArray(new String[0]);
		constituent_GIDs = rq.cons.toArray(new String[0]);
		motion_GIDs = rq.moti.toArray(new String[0]);
		justification_GIDs = rq.just.toArray(new String[0]);
		*/
		rd.add(rq);
	}
	@SuppressWarnings("unchecked")
	public SpecificRequest clone() {
		SpecificRequest result = new SpecificRequest();
		result.peers = (Hashtable<String,String>) peers.clone();
		result.news = (ArrayList<String>) news.clone();
		for (RequestData a: result.rd) {
			result.rd.add(a.clone());
		}
		return result;
	}
	public String toSummaryString() {
		return "SpecificRequest["+Util.concatSummary(rd.toArray(new RequestData[0]), ",", null)+
				",\n p="+Util.concat(peers, ",", null)+
				", "+Util.concatSummary(news.toArray(new String[0]), ",", null)+
				", "+Util.concatSummary(tran.toArray(new String[0]), ",", null)+
				"]";
	}
	public String toString() {
		return "SpecificRequest["+Util.concat(rd.toArray(new RequestData[0]), ",")+
				",\n peers="+Util.concat(peers, ",", null)+
				",\n news="+Util.concat(news.toArray(new String[0]), ",")+
				",\n tran="+Util.concat(tran.toArray(new String[0]), ",")+
				"]";
	}
	public static final String sql_orgs_inferred_queried_for_peer =
			"SELECT DISTINCT "
			//+" o."+table.organization.specific_requests 
			//+ ", "
			//+"o."+table.organization.global_organization_ID+
			//", "
			+ "o."+net.ddp2p.common.table.organization.global_organization_ID_hash+
			" FROM "+net.ddp2p.common.table.organization.TNAME+" AS o "+
			" JOIN "+net.ddp2p.common.table.peer_org_inferred.TNAME+" AS p ON(p."+net.ddp2p.common.table.peer_org_inferred.organization_ID+"=o."+net.ddp2p.common.table.organization.organization_ID+") "+
			" WHERE "+net.ddp2p.common.table.peer_org_inferred.peer_ID+"=? AND o."+net.ddp2p.common.table.organization.specific_requests+" IS NOT NULL";
	public static final String sql_orgs_queried_for_peer =
			"SELECT DISTINCT "
			//+" o."+table.organization.specific_requests 
			//+ ", "
			//+"o."+table.organization.global_organization_ID+
			//", "
			+ "o."+net.ddp2p.common.table.organization.global_organization_ID_hash+
			" FROM "+net.ddp2p.common.table.organization.TNAME+" AS o "+
			" JOIN "+net.ddp2p.common.table.peer_org.TNAME+" AS p ON(p."+net.ddp2p.common.table.peer_org.organization_ID+"=o."+net.ddp2p.common.table.organization.organization_ID+") "+
			" WHERE "+net.ddp2p.common.table.peer_org.peer_ID+"=? AND o."+net.ddp2p.common.table.organization.specific_requests+" IS NOT NULL";
	/**
	 * // for each org served by this peer, gather the requested data
	 * @param peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static SpecificRequest getPeerRequest(String peer_ID) throws P2PDDSQLException {
		//boolean DEBUG = true;
		String sql_orgs_all_queried = sql_orgs_inferred_queried_for_peer+" UNION "+sql_orgs_queried_for_peer;
		if (DEBUG) System.out.println("SpecificRequest: getPeerRequest: start "+peer_ID);
		long _peer_ID = Util.lval(peer_ID, -1);
		if (_peer_ID == -1) return null;
		SpecificRequest result = new SpecificRequest();
		ArrayList<ArrayList<Object>> s = Application.db.select(sql_orgs_all_queried, new String[]{peer_ID, peer_ID}, DEBUG);
		
		//if(s.size()==0) return null; // comment since there are also some global ones...
		//RequestData rq = new RequestData();
		for (ArrayList<Object> o :s) {
			//String rd = Util.getString(o.get(0));
			String oGIDhash = Util.getString(o.get(0));
			D_Organization or = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, oGIDhash, true, false);
			if (or == null) continue;
			OrgPeerDataHashes no = or.getSpecificRequest();//new OrgPeerDataHashes(rd, oGIDhash);
			if (DEBUG) System.out.println("SpecificRequest: getPeerRequest: add "+no);
			RequestData n = no.getRequestData(_peer_ID);
			
			//RequestData n = new RequestData(rd, oGIDhash);
			if (n.empty()) {
				if (DEBUG) System.out.println("SpecificRequest: getPeerRequest: empty");
				continue;
			}
			n.global_organization_ID_hash = oGIDhash;
		
			
			if (DEBUG) System.out.println("SpecificRequest:getPeerRequest: add "+n);
			result.rd.add(n);
		}
		
		GlobalClaimedDataHashes gcdh = GlobalClaimedDataHashes.get();
		if (gcdh != null) {
			if (DEBUG) System.out.println("SpecificRequest:getPeerRequest: obtained "+gcdh+"\npeers");
			result.peers = OrgPeerDataHashes.addFromPeer(gcdh.peers, _peer_ID, result.peers);
			if (DEBUG) System.out.println("SpecificRequest: getPeerRequest: news");
			result.news = OrgPeerDataHashes.addFromPeer(gcdh.news, _peer_ID, result.news);
			if (DEBUG) System.out.println("SpecificRequest: getPeerRequest: tran");
			result.tran = OrgPeerDataHashes.addFromPeer(gcdh.tran, _peer_ID, result.tran);
		} else {
			if (DEBUG) System.out.println("SpecificRequest:getPeerRequest: null global");
		}
		/*
		if (false) {
			String missing_news = DD.getExactAppText(DD.MISSING_NEWS);
			if (missing_news != null) {
				byte data[] = Util.byteSignatureFromString(missing_news);
				Decoder d = new Decoder(data);
				Hashtable<String, Hashtable<Long, String>> _news = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
				result.news = OrgPeerDataHashes.addFromPeer(_news, _peer_ID, result.news);
			}
			
			String missing_peers = DD.getExactAppText(DD.MISSING_PEERS);
			if (missing_peers != null) {
				byte data[] = Util.byteSignatureFromString(missing_peers);
				Decoder d = new Decoder(data);
				Hashtable<String, Hashtable<Long, String>> _peers = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
				result.peers = OrgPeerDataHashes.addFromPeer(_peers, _peer_ID, result.peers);
			}
		}	
		*/
		if (DEBUG) System.out.println("SpecificRequest:getPeerRequest: return "+result);
		return result;
	}

	/**
	 * SpecificRequest ::= SEQUENCE {
	 * rd SEQUENCE OF REquestData
	 * }
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(Encoder.getEncoder(this.rd.toArray(new RequestData[0])));
		if (peers.size() > 0)
			//enc.addToSequence(Encoder.getStringEncoder(peers.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
			enc.addToSequence(Encoder.getHashStringEncoder(peers, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if (news.size() > 0) enc.addToSequence(Encoder.getStringEncoder(news.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if (tran.size() > 0) enc.addToSequence(Encoder.getStringEncoder(tran.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		/*
		enc.addToSequence(Encoder.getStringEncoder(org_GIDs, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(constituent_GIDs, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(neighborhood_GIDs, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(motion_GIDs, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(justification_GIDs, Encoder.TAG_PrintableString));
		*/
		
		return enc;
	}

	@Override
	public SpecificRequest decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		rd = d.getFirstObject(true).getSequenceOfAL(RequestData.getASN1Tag(), new RequestData());
		if (d.getTypeByte() == DD.TAG_AC1) peers = d.getFirstObject(true).getSequenceOfHSS(Encoder.TAG_PrintableString, false);
		if (d.getTypeByte() == DD.TAG_AC2) news = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		if (d.getTypeByte() == DD.TAG_AC3) tran = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		/*
		org_GIDs = d.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		constituent_GIDs = d.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		neighborhood_GIDs = d.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		motion_GIDs = d.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		justification_GIDs = d.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		*/
		return this;
	}
	public void add(SpecificRequest a) {
		for(RequestData r : a.rd) {
			add(r);
		}
	}
	public void add(RequestData a) {
		for (RequestData r : rd) {
			if ( (r == null) || (r.global_organization_ID_hash == null) ) {
				Util.printCallPath("Something is null!"+this);
				return;
			}
			if (r.global_organization_ID_hash.equals(a.global_organization_ID_hash)) {
				r.add(a);
				return;
			}
		}
		rd.add(a);
	}
	public void add(int type, String hash, String org_hash, int MAX_ITEM) {
		if ( (org_hash == null) || (hash == null) ) {
			Util.printCallPath("Something is null!\n"+this+"\n hash ="+hash+"\norg_hash="+org_hash);
			return;
		}
		RequestData tmp = new RequestData();
		tmp.global_organization_ID_hash = org_hash;
		tmp.addHashIfNewTo(hash, type, MAX_ITEM);
		add(tmp);
	}
	public void add(int type, String hash, Calendar date, String org_hash, int MAX_ITEM) {
		if ( (org_hash == null) || (hash == null) ) {
			Util.printCallPath("Something is null!\n"+this+"\n hash ="+hash+"\norg_hash="+org_hash);
			return;
		}
		RequestData tmp = new RequestData();
		tmp.global_organization_ID_hash = org_hash;
		tmp.addHashIfNewTo(hash, Encoder.getGeneralizedTime(date), type, MAX_ITEM);
		add(tmp);
	}
}
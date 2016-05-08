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
package net.ddp2p.common.streaming;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
/**
 * For each piece of data store {data_hash:{peerID:date_claim_received}}
 * 
 * Currently we only use the data_hash, as we send it in specific requests in sync requests.
 * The idea is to eventually use rest of it to evaluate peers if they claim to have data they do not have,
 * or to mark that a peer does not have it and should not  be disturbed again for it.
 * 
 * It is stored in attribute "specific_request" of table organization.
 * 
 * TODO:
 * For hashes of constituents, this should ideally also store the claimed creation date, to know when it was really received.
 * 		
 * ... probably should be something like {data_hash:{peerID:[date_claim_received,creation_data_claimed]}}
 *  
 * @author msilaghi
 *
 */
public class OrgPeerDataHashes extends ASNObj{ 
	/**
	 * TODO Not yet implemented (only date_first_claim_received is used!).
	 * Should ASN encode and decode all components
	 * At implementation one should check that all the below operations work right!
	 * @author msilaghi
	 *
	 */
	public static class PeerData extends ASNObj {
		String date_first_claim_received;
		String date_last_rejection_received; 
		String creation_data_claimed;
		public String toString() {
			return date_first_claim_received;
		}
		@Override
		public Encoder getEncoder() {
			return new Encoder(date_first_claim_received);
		}
		@Override
		public PeerData decode(Decoder dec) throws ASN1DecoderFail {
			date_first_claim_received = dec.getString();
			return this;
		}
		public String toLongString() {
			return "PeerData:"+creation_data_claimed+" c="+date_first_claim_received+" r="+date_last_rejection_received;
		}
	}
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int version = 2;
	public Hashtable<String,Hashtable<Long,Object>> cons=new Hashtable<String,Hashtable<Long,Object>>();
	public Hashtable<String,Hashtable<Long,String>> witn=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> neig=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> moti=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> just=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> sign=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> news=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> tran=new Hashtable<String,Hashtable<Long,String>>();
	private String global_organization_ID_hash; 
	private String getStringRepresentationOfHashes(
			Hashtable<String, Hashtable<Long, String>> data_peers) {
		String result ="[";
		if(data_peers == null) return "[null]";
		for(String dHash : data_peers.keySet()){
			result += "\n     GIDH="+dHash+"{";
			Hashtable<Long, String> dta = data_peers.get(dHash);
			for(Long id : dta.keySet()) {
				result += "(pLID="+id+":"+dta.get(id)+");";
			}
			result+="}";
		}
		return result+"]";
	}
	private String getStringRepresentationOfHashesObj(
			Hashtable<String, Hashtable<Long, Object>> data_peers) {
		String result ="[";
		if(data_peers == null) return "[null]";
		for(String dHash : data_peers.keySet()){
			result += "\n     GIDH="+dHash+"{";
			Hashtable<Long, Object> dta = data_peers.get(dHash);
			for(Long id : dta.keySet()) {
				result += "(pLID="+id+":"+dta.get(id)+");";
			}
			result+="}";
		}
		return result+"]";
	}
	public String toString(){
		String result = "OrgPeerDataHashes: [\n";
		result += "    orgGIDhash = "+getOrganizationGIDH();
		if((cons!=null)&&(cons.size()>0)) result += "\n  cons = "+getStringRepresentationOfHashesObj(cons);
		if((witn!=null)&&(witn.size()>0)) result += "\n  witn = "+getStringRepresentationOfHashes(witn);
		if((neig!=null)&&(neig.size()>0)) result += "\n  neig = "+getStringRepresentationOfHashes(neig);
		if((moti!=null)&&(moti.size()>0)) result += "\n  moti = "+getStringRepresentationOfHashes(moti);
		if((just!=null)&&(just.size()>0)) result += "\n  just = "+getStringRepresentationOfHashes(just);
		if((sign!=null)&&(sign.size()>0)) result += "\n  sign = "+getStringRepresentationOfHashes(sign);
		if((news!=null)&&(news.size()>0)) result += "\n  news = "+getStringRepresentationOfHashes(news);
		if((tran!=null)&&(tran.size()>0)) result += "\n  tran = "+getStringRepresentationOfHashes(tran);
		result += "/n]";
		return result;
	}
	public OrgPeerDataHashes() {}
	public OrgPeerDataHashes(Decoder dec) throws ASN1DecoderFail {this.decode(dec);}
	public static OrgPeerDataHashes get(long orgID) {
		if (orgID <= 0) {
			System.err.println("OrgPeerDataHashes: get: null "+orgID);
			return null;
		}
		D_Organization org = D_Organization.getOrgByLID_NoKeep(orgID, true);
		if (org == null) {
			System.err.println("OrgPeerDataHashes: get: null for "+orgID);
			return null;
		}
		OrgPeerDataHashes requests = org.getSpecificRequest();
		if (DEBUG) System.out.println("\nOrgPeerDataHashes: init: got "+requests);
		return requests;
	}
	/**
	 * in database we do not store the orgGID
	 * @param orgID
	 * @param peer_ID 
	 * @throws P2PDDSQLException
	 */
	public void save(long orgID, long peer_ID, D_Peer peer) throws P2PDDSQLException {
		if (DEBUG) System.out.println("\nOrgPeerDataHashes: save: saving oLID="+orgID+" pLID="+peer_ID+" "+this);
		if (this.getOrganizationGIDH() == null) Util.printCallPath("Try");
		D_Organization or = D_Organization.getOrgByLID(orgID, true, true);
		if (or.getSpecificRequest() != this) {
			if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("\nOrgPeerDataHashes: save: object is == different \n"+this+"\nvs "+or.getSpecificRequest());			
		}
		or.setSpecificRequest(this);
		or.dirty_locals = true;
		or.storeRequest();
		or.releaseReference();
		if ((peer != null) && (peer.servesOrgEntryExists(orgID))) {
			if (DEBUG) System.out.println("\nOrgPeerDataHashes: save: exists");
			return;
		} else {
			if (DEBUG) System.out.println("\nOrgPeerDataHashes: save: not existing");
		}
		if (DEBUG) out.println("OrgPeerDataHashes: save: end orgID="+orgID + " peer=" + peer);
		peer.setServingOrgInferred(orgID, true);
	}
	/**
	 * Extracts the keys i.e., hashes (and sometimes creation_dates) from the stored
	 * claims in OrgPeerData
	 * @return
	 */
	RequestData getRequestData() {
		RequestData r = new RequestData();
		r.global_organization_ID_hash = this.getOrganizationGIDH();
		r.cons = new Hashtable<String, String>(Util.getHSS(this.cons.keySet()));
		r.witn = new ArrayList<String>(this.witn.keySet());
		r.neig = new ArrayList<String>(this.neig.keySet());
		r.moti = new ArrayList<String>(this.moti.keySet());
		r.just = new ArrayList<String>(this.just.keySet());
		r.sign = new Hashtable<String,String>(Util.getHSS(this.sign.keySet()));
		r.news = new ArrayList<String>(this.news.keySet());
		r.tran = new ArrayList<String>(this.tran.keySet());
		return r;
	}
	/**
	 * Extract hashes provided by _peer_ID into result (if not yet there)
	 * @param hashes    {data_hash:{peerID:date_claim_received}}
	 * @param _peer_ID
	 * @param result
	 * @return
	 */
	public static ArrayList<String> addFromPeer(Hashtable <String, Hashtable<Long,String>> hashes, Long _peer_ID, ArrayList<String> result) {
		if (DEBUG) System.out.println("\nOrgPeerDataHashes:addFromPeer:  pID="+_peer_ID+" adding to"+Util.concat(result, ";", "null"));
		for (String h : hashes.keySet()) { 
			Hashtable <Long, String> p = hashes.get(h); 
			if (_peer_ID == null || p.containsKey(_peer_ID)) 
				if ( ! result.contains(h) )
					result.add(h);  
		}
		if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: Got "+hashes+"\n");
		return result;
	}
	/**
	 * This version should return both the desired GIDH and the desired date. Since dates are not stored internally for some types, add EMPTYDATE ("")
	 * as date.
	 * @param hashes
	 * @param _peer_ID
	 * @param result
	 * @return
	 */
	public static Hashtable<String, String> addFromPeer(
			Hashtable<String, Hashtable<Long, String>> hashes, Long _peer_ID,
			Hashtable<String, String> result) {
		if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: "+_peer_ID);
		for (String h : hashes.keySet()) {
			if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: evaluate " + h);
			Hashtable<Long, String> p = hashes.get(h);
			if (_peer_ID == null || p.containsKey(_peer_ID)) {
				if (! result.containsKey(h)) {
					result.put(h,DD.EMPTYDATE); 
					if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: result contained in peer " + _peer_ID);
				} else {
					if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: result not contained in " + _peer_ID);
				}
			} else {
				if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: hash not containing " + _peer_ID);
			}
		}
		if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: Got "+hashes+"\n");
		return result;
	}
	public static Hashtable<String, String> addFromPeerObj(
			Hashtable<String, Hashtable<Long, Object>> hashes, Long _peer_ID,
			Hashtable<String, String> result) {
		if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: "+_peer_ID);
		for (String h : hashes.keySet()) {
			if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: evaluate " + h);
			Hashtable<Long, Object> p = hashes.get(h);
			if (_peer_ID == null || p.containsKey(_peer_ID)) {
				if (! result.containsKey(h)) {
					result.put(h,DD.EMPTYDATE); 
					if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: result contained in peer " + _peer_ID);
				} else {
					if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: result not contained in " + _peer_ID);
				}
			} else {
				if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: hash not containing " + _peer_ID);
			}
		}
		if (DEBUG) System.out.println("OrgPeerDataHashes:addFromPeer: Got "+hashes+"\n");
		return result;
	}
	public RequestData getRequestData(long peer_ID) {
		Long _peer_ID = new Long(peer_ID);
		RequestData r = new RequestData();
		r.global_organization_ID_hash = this.getOrganizationGIDH();
		r.cons = addFromPeerObj(cons, _peer_ID, r.cons);
		r.witn = addFromPeer(witn, _peer_ID, r.witn);
		r.neig = addFromPeer(neig, _peer_ID, r.neig);
		r.moti = addFromPeer(moti, _peer_ID, r.moti);
		r.just = addFromPeer(just, _peer_ID, r.just);
		r.sign = addFromPeer(sign, _peer_ID, r.sign);
		r.news = addFromPeer(news, _peer_ID, r.news);
		r.tran = addFromPeer(tran, _peer_ID, r.tran);
		return r;
	}
	public void add(RequestData n, long _peer_ID, String generalizedTime) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:add: add "+n+" to "+this);
		synchronized(cons){
			neig = appendSet(neig, n.neig, _peer_ID, generalizedTime);
			cons = appendHashObj(cons, n.cons, _peer_ID, generalizedTime);
			witn = appendSet(witn, n.witn, _peer_ID, generalizedTime);
			moti = appendSet(moti, n.moti, _peer_ID, generalizedTime);
			just = appendSet(just, n.just, _peer_ID, generalizedTime);
			sign = appendHash(sign, n.sign, _peer_ID, generalizedTime);
			tran = appendSet(tran, n.tran, _peer_ID, generalizedTime);
			news = appendSet(news, n.news, _peer_ID, generalizedTime);
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:add: Got "+this);
	}
/**
 * ads to "to" {s , {peer_ID, generalizedTime}}
 * @param to
 * @param s
 * @param peer_ID
 * @param generalizedTime
 */
	static void add(Hashtable<String, Hashtable<Long, String>> to, String s, long peer_ID, String generalizedTime) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:add: static peer="+peer_ID+" hash="+s+"\n   t="+generalizedTime+"\n    to:" +to);
		Hashtable<Long, String> peers = to.get(s);
		if(peers == null) peers = new Hashtable<Long, String>();
		peers.put(new Long(peer_ID), generalizedTime);
		if(!to.containsKey(s)) to.put(s, peers);		
		if(DEBUG)System.out.println("OrgPeerDataHashes:add: static Got "+to);
	}
	static void addObj(Hashtable<String, Hashtable<Long, Object>> to, String s, long peer_ID, String generalizedTime) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:add: static peer="+peer_ID+" hash="+s+"\n   t="+generalizedTime+"\n    to:" +to);
		Hashtable<Long, Object> peers = to.get(s);
		if(peers == null) peers = new Hashtable<Long, Object>();
		peers.put(new Long(peer_ID), generalizedTime);
		if(!to.containsKey(s)) to.put(s, peers);		
		if(DEBUG)System.out.println("OrgPeerDataHashes:add: static Got "+to);
	}
	/**
	 * 
	 * @param to
	 * @param from
	 * @param peer_ID
	 * @param generalizedTime : arrival date of claim (not used now, // using creation time in from)
	 * @return
	 */
	public static Hashtable<String, Hashtable<Long, String>> appendHash(
			Hashtable<String, Hashtable<Long, String>> to,
			Hashtable<String, String> from, long peer_ID,
			String generalizedTime) {
		for (String s : from.keySet()) {
			add(to, s, peer_ID, from.get(s));
		}
		if (DEBUG) System.out.println("OrgPeerDataHashes:appendSet: Got "+to);
		return to;
	}
	public static Hashtable<String, Hashtable<Long, Object>> appendHashObj(
			Hashtable<String, Hashtable<Long, Object>> to,
			Hashtable<String, String> from, long peer_ID,
			String generalizedTime) {
		for (String s : from.keySet()) {
			addObj(to, s, peer_ID, from.get(s));
		}
		if (DEBUG) System.out.println("OrgPeerDataHashes:appendSet: Got "+to);
		return to;
	}
	public static Hashtable<String, Hashtable<Long, String>> appendSet(
			Hashtable<String, Hashtable<Long, String>> to,
			ArrayList<String> from,
			long peer_ID, String generalizedTime) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:appendSet: adding peerID="+peer_ID+" t="+generalizedTime+" new set="+Util.concat(from, ";", "null"));
		for(String s : from){
			add(to, s, peer_ID, generalizedTime);
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:appendSet: Got "+to);
		return to;
	}
	@Override
	public OrgPeerDataHashes decode(Decoder dec) throws ASN1DecoderFail {
		if (dec == null || dec.isEmpty()) return this;
		if (dec.getTypeByte() != DD.TAG_AC15) {
			Util.printCallPath("wrong TAG "+dec.getTypeByte());
			return this;
		}
		Decoder d = dec.getContent();
		cons = decodeDataObj(d.getFirstObject(true));
		witn = decodeData(d.getFirstObject(true));
		neig = decodeData(d.getFirstObject(true));
		moti = decodeData(d.getFirstObject(true));
		just = decodeData(d.getFirstObject(true));
		sign = decodeData(d.getFirstObject(true));
		news = decodeData(d.getFirstObject(true));
		tran = decodeData(d.getFirstObject(true));
		return this;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(getDataEncoderObj(cons));
		enc.addToSequence(getDataEncoder(witn));
		enc.addToSequence(getDataEncoder(neig));
		enc.addToSequence(getDataEncoder(moti));
		enc.addToSequence(getDataEncoder(just));
		enc.addToSequence(getDataEncoder(sign));
		enc.addToSequence(getDataEncoder(news));
		enc.addToSequence(getDataEncoder(tran));
		enc.setASN1Type(DD.TAG_AC15);
		return enc;
	}
	/**
	 *
	 * @param dec
	 * @return   {data_hash:{peerID:date_claim_received}}
	 */
	static public Hashtable<String, Hashtable<Long, String>> decodeData(Decoder dec) {
		Hashtable<String, Hashtable<Long, String>> result =
			new Hashtable<String, Hashtable<Long, String>>();
		if(dec==null) return result;
		if(DEBUG) dec.printHex("\nOrgPeerDataHashes:decodeData: decoding dec");
		if(dec.getTypeByte() != DD.TAG_AC14){
			Util.printCallPath("wrong Data tag");
			return result;
		}
		Decoder _d_data;
		try {
			_d_data = dec.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return result;
		}
		if(DEBUG) _d_data.printHex("OrgPeerDataHashes:decodeData: decoding _d_data content");
		for(;;) {
			Decoder d_item_hash = _d_data.getFirstObject(true);
			if(d_item_hash==null) break;
			if(DEBUG) d_item_hash.printHex("OrgPeerDataHashes:decodeData: decoding d_item_hash");
			if(d_item_hash.getTypeByte() != DD.TAG_AC16){
				Util.printCallPath("wrong data item tag");
				break;
			}
			Decoder d_item_hash_content;
			try {
				d_item_hash_content = d_item_hash.getContent();
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
				break;
			}
			if(DEBUG) d_item_hash_content.printHex("OrgPeerDataHashes:decodeData: decoding d_item_hash_content");
			Decoder _d_hash = d_item_hash_content.getFirstObject(true);
			if(_d_hash==null) break;
			if(DEBUG) _d_hash.printHex("OrgPeerDataHashes:decodeData: decoding _d_hash");
			String hash = _d_hash.getString();
			if(DEBUG) System.out.println("OrgPeerDataHashes: decodeData: peer hash/GID="+hash);
			if(DEBUG) d_item_hash_content.printHex("OrgPeerDataHashes:decodeData: decoding d_item_hash_content after hash");
			Decoder _d_peers = d_item_hash_content.getFirstObject(true);
			if(DEBUG) d_item_hash_content.printHex("OrgPeerDataHashes:decodeData: decoding _d_peers");
			if(_d_peers==null){
				Util.printCallPath("Extra peer/hash in requested data");
				break;
			}
			Hashtable<Long, String> peers = decodePeers(_d_peers);
			result.put(hash, peers);
		}
		return result;
	}
	/**
	 * TODO: here one should detect if the data is a String or an instance of PeerData!
	 * @param dec
	 * @return
	 */
	static public Hashtable<String, Hashtable<Long, Object>> decodeDataObj(Decoder dec) {
		Hashtable<String, Hashtable<Long, Object>> result =
			new Hashtable<String, Hashtable<Long, Object>>();
		if(dec==null) return result;
		if(DEBUG) dec.printHex("\nOrgPeerDataHashes:decodeData: decoding dec");
		if(dec.getTypeByte() != DD.TAG_AC14){
			Util.printCallPath("wrong Data tag");
			return result;
		}
		Decoder _d_data;
		try {
			_d_data = dec.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return result;
		}
		if(DEBUG) _d_data.printHex("OrgPeerDataHashes:decodeData: decoding _d_data content");
		for(;;) {
			Decoder d_item_hash = _d_data.getFirstObject(true);
			if(d_item_hash==null) break;
			if(DEBUG) d_item_hash.printHex("OrgPeerDataHashes:decodeData: decoding d_item_hash");
			if(d_item_hash.getTypeByte() != DD.TAG_AC16){
				Util.printCallPath("wrong data item tag");
				break;
			}
			Decoder d_item_hash_content;
			try {
				d_item_hash_content = d_item_hash.getContent();
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
				break;
			}
			if(DEBUG) d_item_hash_content.printHex("OrgPeerDataHashes:decodeData: decoding d_item_hash_content");
			Decoder _d_hash = d_item_hash_content.getFirstObject(true);
			if(_d_hash==null) break;
			if(DEBUG) _d_hash.printHex("OrgPeerDataHashes:decodeData: decoding _d_hash");
			String hash = _d_hash.getString();
			if(DEBUG) System.out.println("OrgPeerDataHashes: decodeData: peer hash/GID="+hash);
			if(DEBUG) d_item_hash_content.printHex("OrgPeerDataHashes:decodeData: decoding d_item_hash_content after hash");
			Decoder _d_peers = d_item_hash_content.getFirstObject(true);
			if(DEBUG) d_item_hash_content.printHex("OrgPeerDataHashes:decodeData: decoding _d_peers");
			if(_d_peers==null){
				Util.printCallPath("Extra peer/hash in requested data");
				break;
			}
			Hashtable<Long, Object> peers = decodePeersObj(_d_peers);
			result.put(hash, peers);
		}
		return result;
	}
	static public Encoder getDataEncoder(Hashtable<String, Hashtable<Long, String>> d) {
		Encoder enc = new Encoder().initSequence();
		for(String hash: d.keySet()) {
			Encoder enc_item = new Encoder().initSequence();
			Hashtable<Long, String> peers = d.get(hash);
			enc_item.addToSequence(new Encoder(hash));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getDataEncoder: hash="+hash);
			enc_item.addToSequence(getPeers(peers));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getDataEncoder: peers");
			enc_item.setASN1Type(DD.TAG_AC16);
			enc.addToSequence(enc_item);
		}
		enc.setASN1Type(DD.TAG_AC14);
		return enc;
	}
	static public Encoder getDataEncoderObj(Hashtable<String, Hashtable<Long, Object>> d) {
		Encoder enc = new Encoder().initSequence();
		for(String hash: d.keySet()) {
			Encoder enc_item = new Encoder().initSequence();
			Hashtable<Long, Object> peers = d.get(hash);
			enc_item.addToSequence(new Encoder(hash));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getDataEncoder: hash="+hash);
			enc_item.addToSequence(getPeersObj(peers));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getDataEncoder: peers");
			enc_item.setASN1Type(DD.TAG_AC16);
			enc.addToSequence(enc_item);
		}
		enc.setASN1Type(DD.TAG_AC14);
		return enc;
	}
	static private Hashtable<Long, String> decodePeers(Decoder _d_hash) {
		Hashtable<Long, String> result = new Hashtable<Long, String>();
		if(_d_hash==null) return result;
		if(DEBUG) _d_hash.printHex("O\nrgPeerDataHashes:decodePeers: decoding data");
		if(_d_hash.getTypeByte() != DD.TAG_AC13){
			Util.printCallPath("wrong Peers tag");
			return result;
		}
		Decoder d;
		try {
			d = _d_hash.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return result;
		}
		for(;;) {
			Decoder d_peer = d.getFirstObject(true);
			if(d_peer == null) break;
			long peer = d_peer.getInteger().longValue();
			if(DEBUG) System.out.println("OrgPeerDataHashes: decodePeers: peer="+peer);
			Decoder d_date = d.getFirstObject(true);
			if(d_date==null) break;
			String date = d_date.getString();
			result.put(new Long(peer), date);
			if(DEBUG) System.out.println("OrgPeerDataHashes: decodePeers: date="+date);
		}
		return result;
	}
	/**
	 * TODO should check that the data is String or PeerData...
	 * @param _d_hash
	 * @return
	 */
	static private Hashtable<Long, Object> decodePeersObj(Decoder _d_hash) {
		Hashtable<Long, Object> result = new Hashtable<Long, Object>();
		if(_d_hash==null) return result;
		if(DEBUG) _d_hash.printHex("O\nrgPeerDataHashes:decodePeers: decoding data");
		if(_d_hash.getTypeByte() != DD.TAG_AC13){
			Util.printCallPath("wrong Peers tag");
			return result;
		}
		Decoder d;
		try {
			d = _d_hash.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return result;
		}
		for(;;) {
			Decoder d_peer = d.getFirstObject(true);
			if (d_peer == null) break;
			long peer = d_peer.getInteger().longValue();
			if(DEBUG) System.out.println("OrgPeerDataHashes: decodePeers: peer="+peer);
			Decoder d_date = d.getFirstObject(true);
			if (d_date == null) break;
			try {
				PeerData pd = new PeerData().decode(d_date);
				result.put(new Long(peer), pd);
			}catch (Exception e){
				String date = d_date.getString();
				result.put(new Long(peer), date);
				if(DEBUG) System.out.println("OrgPeerDataHashes: decodePeers: date="+date);
			}
		}
		return result;
	}
	static private Encoder getPeers(Hashtable<Long, String> peers) {
		Encoder enc = new Encoder().initSequence();
		for(Long peer: peers.keySet()) {
			String data = peers.get(peer);
			enc.addToSequence(new Encoder(peer.longValue()));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getPeers: peer="+peer);
			enc.addToSequence(new Encoder(data));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getPeers: data="+data);
		}
		enc.setASN1Type(DD.TAG_AC13);
		return enc;
	}
	static private Encoder getPeersObj(Hashtable<Long, Object> peers) {
		Encoder enc = new Encoder().initSequence();
		for(Long peer: peers.keySet()) {
			Object data = peers.get(peer);
			enc.addToSequence(new Encoder(peer.longValue()));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getPeers: peer="+peer);
			if (data instanceof PeerData) enc.addToSequence(((PeerData)data).getEncoder());
			else if (data instanceof String) enc.addToSequence(new Encoder((String)data));
			if(DEBUG) System.out.println("OrgPeerDataHashes: getPeers: data="+data);
		}
		enc.setASN1Type(DD.TAG_AC13);
		return enc;
	}
	/**
	 * removed what is in obtained
	 * @param obtained
	 */
	public void purge(RequestData obtained) {
		if(obtained==null) return;
		if(DEBUG)System.out.println("RequestData:purge: Will purge "+this+" with "+obtained);
		synchronized(cons){
			for(String s : obtained.cons.keySet()){
				cons.remove(s);
				cons.remove(D_Constituent.getGIDHashFromGID(s));
			}
			for(String s : obtained.neig) neig.remove(s);
			for(String s : obtained.witn) witn.remove(s);
			for(String s : obtained.moti) moti.remove(s);
			for(String s : obtained.just) just.remove(s);
			for(String s : obtained.sign.keySet()) sign.remove(s);
			for(String s : obtained.tran) tran.remove(s);
			for(String s : obtained.news) news.remove(s);
		}
		if(DEBUG)System.out.println("RequestData:purge: Got "+this);
	}
	/**
	 * Add each obtained key to this, as coming from peer_ID at crt_date.
	 * Then remove from this keys that were not in "obtained"
	 * 
	 * @param orig_rq : not used
	 * @param sol_rq : remove these, after adding new
	 * @param new_rq : add those from this parameter
	 * @param peer_ID
	 * @param crt_date
	 */
	public void updateAfterChanges(RequestData orig_rq, RequestData sol_rq, RequestData new_rq, long peer_ID, String crt_date) {
		if(DEBUG)System.out.println("RequestData:updateAfterChanges: Will updateAfterChanges by "+peer_ID+" on \n"+this+
				" with \n new="+new_rq+"\nsolved="+sol_rq);
			for(String s : new_rq.cons.keySet()) addObj(cons, s, peer_ID, crt_date);
			for(String s : new_rq.neig) add(neig, s, peer_ID, crt_date);
			for(String s : new_rq.witn) add(witn, s, peer_ID, crt_date);
			for(String s : new_rq.moti) add(moti, s, peer_ID, crt_date);
			for(String s : new_rq.just) add(just, s, peer_ID, crt_date);
			for(String s : new_rq.sign.keySet()) add(sign, s, peer_ID, crt_date);
			for(String s : new_rq.tran) add(tran, s, peer_ID, crt_date);
			for(String s : new_rq.news) add(news, s, peer_ID, crt_date);
			setIntersectionFirstFromSecondObj(this.cons, sol_rq.cons);
			setIntersectionFirstFromSecond(this.neig, sol_rq.neig);
			setIntersectionFirstFromSecond(this.witn, sol_rq.witn);
			setIntersectionFirstFromSecond(this.moti, sol_rq.moti);
			setIntersectionFirstFromSecond(this.just, sol_rq.just);
			setIntersectionFirstFromSecond(this.sign, sol_rq.sign);
			setIntersectionFirstFromSecond(this.tran, sol_rq.tran);
			setIntersectionFirstFromSecond(this.news, sol_rq.news);
		if(DEBUG)System.out.println("RequestData:updateAfterChanges: Got "+this);
	}
	/**
	 * Keep in first only what is also in second
	 * @param first
	 * @param second
	 */
	public static void setIntersectionFirstFromSecond(
			Hashtable<String, Hashtable<Long, String>> first,
			ArrayList<String> second) {
		ArrayList<String>  itemsToRemove = new ArrayList<String>();
		for(String s : first.keySet()) if(second.contains(s)) itemsToRemove.add(s);
		synchronized (first) {
			for(String s : itemsToRemove) first.remove(s);
		}
	}
	/**
	 * Keep in first only what is also in second
	 * @param first
	 * @param second
	 */
	public static void setIntersectionFirstFromSecond(
			Hashtable<String, Hashtable<Long, String>> first,
			Hashtable<String,String> second) {
		ArrayList<String>  itemsToRemove = new ArrayList<String>();
		for(String s : first.keySet()) if(!second.containsKey(s)) itemsToRemove.add(s);
		for(String s : itemsToRemove) first.remove(s);		
	}
	/**
	 * TODO
	 * @param first
	 * @param second
	 */
	public static void setIntersectionFirstFromSecondObj(
			Hashtable<String, Hashtable<Long, Object>> first,
			Hashtable<String,String> second) {
		ArrayList<String>  itemsToRemove = new ArrayList<String>();
		for(String s : first.keySet()) if(!second.containsKey(s)) itemsToRemove.add(s);
		for(String s : itemsToRemove) first.remove(s);		
	}
	public boolean empty() {
		return 0==
				neig.size()+
				cons.size()+
				moti.size()+
				just.size()+
				witn.size()+
				sign.size()+
				tran.size()+
				news.size();
	}
	public String getOrganizationGIDH() {
		return global_organization_ID_hash;
	}
	public void setOrganizationGIDH(String global_organization_ID_hash) {
		this.global_organization_ID_hash = global_organization_ID_hash;
	}
}

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

import java.util.ArrayList;
import java.util.Hashtable;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import data.D_Constituent;
import data.D_Organization;
import data.D_PeerAddress;
/**
 * For each piece of data store {data_hash:{peerID:date_claim_received}}
 * 
 * Currently we only use the data_hash, as we send it in specific requests in sync requests.
 * The idea is to eventually use rest of it to evaluate peers if they claim to have data they do not have.
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
public class OrgPeerDataHashes extends ASNObj{ // data_hash, peerID, date_claimed  
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public Hashtable<String,Hashtable<Long,String>> cons=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> witn=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> neig=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> moti=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> just=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> sign=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> news=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> tran=new Hashtable<String,Hashtable<Long,String>>();
	String global_organization_ID_hash;
	
	private String getStringRepresentationOfHashes(
			Hashtable<String, Hashtable<Long, String>> data_peers) {
		String result ="[";
		if(data_peers == null) return "[null]";
		for(String peer : data_peers.keySet()){
			result += "\n     peer="+peer+"{";
			Hashtable<Long, String> dta = data_peers.get(peer);
			for(Long id : dta.keySet()) {
				result += "(id="+id+":"+dta.get(id)+");";
			}
			result+="}";
		}
		return result+"]";
	}
	
	public String toString(){
		String result = "OrgPeerDataHashes: [\n";
		result += "    GIDhash = "+global_organization_ID_hash;
		if((cons!=null)&&(cons.size()>0)) result += "\n  cons = "+getStringRepresentationOfHashes(cons);
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

	public OrgPeerDataHashes(){}
	public OrgPeerDataHashes(long orgID) throws P2PDDSQLException {
		if(orgID<=0) return;
		String sql =
			"SELECT "+table.organization.specific_requests+
			","+table.organization.global_organization_ID_hash+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> r = Application.db.select(sql, new String[]{Util.getStringID(orgID)}, DEBUG);
		if(r.size()==0) return;
		String s = Util.getString(r.get(0).get(0));
		this.global_organization_ID_hash = Util.getString(r.get(0).get(1));
		try {
			init(s);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("\nOrgPeerDataHashes: init: got "+this);
	}
	public OrgPeerDataHashes(String rd, String _org_hash) {
		this.global_organization_ID_hash = _org_hash;
		try {
			init(rd);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
	/**
	 * in database we do not store the orgGID
	 * @param orgID
	 * @param peer_ID 
	 * @throws P2PDDSQLException
	 */
	public void save(long orgID, long peer_ID, D_PeerAddress peer) throws P2PDDSQLException {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes: save: saving "+this);
		//Util.printCallPath("Try");
		String old = this.global_organization_ID_hash;
		this.global_organization_ID_hash = null;
		Encoder enc = this.getEncoder();
		this.global_organization_ID_hash = old;
		byte[]msg = enc.getBytes();
		String s = Util.stringSignatureFromByte(msg);
		Application.db.update(table.organization.TNAME, new String[]{table.organization.specific_requests},
				new String[]{table.organization.organization_ID}, new String[]{s, Util.getStringID(orgID)}, DEBUG);
		
		if ((peer!=null) && (peer.servesOrgEntryExists(orgID))) return;
		String sql =
				"SELECT "+table.peer_org.peer_org_ID+
				" FROM "+table.peer_org.TNAME+
				" WHERE "+table.peer_org.peer_ID+"=? AND "+table.peer_org.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql,
				new String[]{Util.getStringID(peer_ID), Util.getStringID(orgID)}, DEBUG);
		if(o.size()==0){
			String _peer_ID = Util.getStringID(peer_ID);
			String _org_ID = Util.getStringID(orgID);
			if((_peer_ID==null)||(_org_ID==null)){
				System.err.println("\nOrgPeerDataHashes: save: error saving "+this);
				Util.printCallPath("Assumption failed: oID="+orgID+" p="+peer_ID);
				return;
			}
			Application.db.insertNoSync(table.peer_org.TNAME,
					new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID, table.peer_org.served,table.peer_org.last_sync_date},
					new String[]{_peer_ID, _org_ID, "0", Util.getGeneralizedTime()}, DEBUG);
		}
	}

	private void init(String rd) throws ASN1DecoderFail {
		byte data[] = Util.byteSignatureFromString(rd);
		if(data==null) return;
		Decoder d = new Decoder(data);
		this.decode(d);
	}
	
	RequestData getRequestData() {
		RequestData r = new RequestData();
		r.global_organization_ID_hash = this.global_organization_ID_hash;
		r.cons = new Hashtable<String, String>(Util.getHSS(this.cons.keySet()));
		r.witn = new ArrayList<String>(this.witn.keySet());
		r.neig = new ArrayList<String>(this.neig.keySet());
		r.moti = new ArrayList<String>(this.moti.keySet());
		r.just = new ArrayList<String>(this.just.keySet());
		r.sign = new ArrayList<String>(this.sign.keySet());
		r.news = new ArrayList<String>(this.news.keySet());
		r.tran = new ArrayList<String>(this.tran.keySet());
		return r;
	}
	public static ArrayList<String> addFromPeer(Hashtable <String, Hashtable<Long,String>> hashes, Long _peer_ID, ArrayList<String> result) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:addFromPeer:  pID="+_peer_ID+" adding to"+Util.concat(result, ";", "null"));
		for(String h : hashes.keySet()){
			Hashtable<Long, String> p = hashes.get(h);
			if(p.containsKey(_peer_ID))
				if(!result.contains(h))result.add(h);
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:addFromPeer: Got "+hashes+"\n");
		return result;
	}
	public static Hashtable<String, String> addFromPeer(
			Hashtable<String, Hashtable<Long, String>> hashes, Long _peer_ID,
			Hashtable<String, String> result) {
		
		for(String h : hashes.keySet()){
			Hashtable<Long, String> p = hashes.get(h);
			if(p.containsKey(_peer_ID))
				if(!result.contains(h))result.put(h,DD.EMPTYDATE);
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:addFromPeer: Got "+hashes+"\n");
		return result;
	}
	public RequestData getRequestData(long peer_ID) {
		Long _peer_ID = new Long(peer_ID);
		RequestData r = new RequestData();
		r.global_organization_ID_hash = this.global_organization_ID_hash;
		r.cons = addFromPeer(cons, _peer_ID, r.cons);
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
		//orgs = appendSet(orgs, n.orgs, _peer_ID, generalizedTime);
		neig = appendSet(neig, n.neig, _peer_ID, generalizedTime);
		cons = appendHash(cons, n.cons, _peer_ID, generalizedTime);
		witn = appendSet(witn, n.witn, _peer_ID, generalizedTime);
		moti = appendSet(moti, n.moti, _peer_ID, generalizedTime);
		just = appendSet(just, n.just, _peer_ID, generalizedTime);
		sign = appendSet(sign, n.sign, _peer_ID, generalizedTime);
		tran = appendSet(tran, n.tran, _peer_ID, generalizedTime);
		news = appendSet(news, n.news, _peer_ID, generalizedTime);
		if(DEBUG)System.out.println("OrgPeerDataHashes:add: Got "+this);
	}

	private static void add(Hashtable<String, Hashtable<Long, String>> to, String s, long peer_ID, String generalizedTime) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:add: static peer="+peer_ID+" hash="+s+"\n   t="+generalizedTime+"\n    to:" +to);
		Hashtable<Long, String> peers = to.get(s);
		if(peers == null) peers = new Hashtable<Long, String>();
		peers.put(new Long(peer_ID), generalizedTime);
		if(!to.contains(s)) to.put(s, peers);		
		if(DEBUG)System.out.println("OrgPeerDataHashes:add: static Got "+to);
	}
	public static Hashtable<String, Hashtable<Long, String>> appendHash(
			Hashtable<String, Hashtable<Long, String>> to,
			Hashtable<String, String> from, long peer_ID,
			String generalizedTime) {
		for(String s : from.keySet()) {
			add(to, s, peer_ID, generalizedTime);
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:appendSet: Got "+to);
		return to;
	}
	public static Hashtable<String, Hashtable<Long, String>> appendSet(
			Hashtable<String, Hashtable<Long, String>> to,
			ArrayList<String> from,
			long peer_ID, String generalizedTime) {
		if(DEBUG)System.out.println("\nOrgPeerDataHashes:appendSet: adding peerID="+peer_ID+" t="+generalizedTime+" new set="+Util.concat(from, ";", "null"));
		for(String s : from){
			add(to, s, peer_ID, generalizedTime);
			/*
			Hashtable<Long, String> peers = to.get(s);
			if(peers == null) peers = new Hashtable<Long, String>();
			peers.put(new Long(peer_ID), generalizedTime);
			if(!to.contains(s)) to.put(s, peers);
			*/
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:appendSet: Got "+to);
		return to;
	}
	
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		if(dec==null) return this;
		if(dec.getTypeByte()!=DD.TAG_AC15) {
			Util.printCallPath("wrong TAG");
			return this;
		}
		Decoder d = dec.getContent();
		cons = decodeData(d.getFirstObject(true));
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
		enc.addToSequence(getDataEncoder(cons));
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
	
	private Hashtable<String, Hashtable<Long, String>> decodeData(Decoder dec) {
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
	private Encoder getDataEncoder(Hashtable<String, Hashtable<Long, String>> d) {
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
	private Hashtable<Long, String> decodePeers(Decoder _d_hash) {
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
	private Encoder getPeers(Hashtable<Long, String> peers) {
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

	/**
	 * removed what is in obtained
	 * @param obtained
	 */
	public void purge(RequestData obtained) {
		if(obtained==null) return;
		if(DEBUG)System.out.println("RequestData:purge: Will purge "+this+" with "+obtained);
		//if(.empty()) return;
		for(String s : obtained.cons.keySet()){
			cons.remove(s);
			cons.remove(D_Constituent.getGIDHashFromGID(s));
		}
		for(String s : obtained.neig) neig.remove(s);
		for(String s : obtained.witn) witn.remove(s);
		for(String s : obtained.moti) moti.remove(s);
		for(String s : obtained.just) just.remove(s);
		for(String s : obtained.sign) sign.remove(s);
		for(String s : obtained.tran) tran.remove(s);
		for(String s : obtained.news) news.remove(s);
		if(DEBUG)System.out.println("RequestData:purge: Got "+this);
	}
	public void updateAfterChanges(RequestData obtained, long peer_ID, String crt_date) {
		if(DEBUG)System.out.println("RequestData:updateAfterChanges: Will updateAfterChanges by "+peer_ID+" on \n"+this+" with \n"+obtained);
		for(String s : obtained.cons.keySet()) add(cons, s, peer_ID, crt_date);
		for(String s : obtained.neig) add(neig, s, peer_ID, crt_date);
		for(String s : obtained.witn) add(witn, s, peer_ID, crt_date);
		for(String s : obtained.moti) add(moti, s, peer_ID, crt_date);
		for(String s : obtained.just) add(just, s, peer_ID, crt_date);
		for(String s : obtained.sign) add(sign, s, peer_ID, crt_date);
		for(String s : obtained.tran) add(tran, s, peer_ID, crt_date);
		for(String s : obtained.news) add(news, s, peer_ID, crt_date);
		
		for(String s : this.cons.keySet()) if(!obtained.cons.contains(s)) cons.remove(s);
		for(String s : this.neig.keySet()) if(!obtained.neig.contains(s)) neig.remove(s);
		for(String s : this.witn.keySet()) if(!obtained.witn.contains(s)) witn.remove(s);
		for(String s : this.moti.keySet()) if(!obtained.moti.contains(s)) moti.remove(s);
		for(String s : this.just.keySet()) if(!obtained.just.contains(s)) just.remove(s);
		for(String s : this.sign.keySet()) if(!obtained.sign.contains(s)) sign.remove(s);
		for(String s : this.tran.keySet()) if(!obtained.tran.contains(s)) tran.remove(s);
		for(String s : this.news.keySet()) if(!obtained.news.contains(s)) news.remove(s);
		if(DEBUG)System.out.println("RequestData:updateAfterChanges: Got "+this);
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
}
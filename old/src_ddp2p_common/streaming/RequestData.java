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

import util.P2PDDSQLException;
import config.Application;
import config.DD;
import data.D_Organization;
import data.D_Constituent;
import data.D_Peer;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import util.Summary;
import util.Util;

/**
 * Contains ArrayLists of GIDhash-es. An ordered hashtable for constituents (with date)
 * Probably should be the same for authoritarian orgs! and for signatures!
 * @author msilaghi
 *
 */
public class RequestData extends ASNObj implements Summary{
	public static final boolean DEBUG = false;
	public static final int ORGS = 0;
	public static final int NEIG = 1;
	public static final int CONS = 2;
	public static final int WITN = 3;
	public static final int MOTI = 4;
	public static final int JUST = 5;
	public static final int SIGN = 6;
	public static final int TRAN = 7;
	public static final int NEWS = 8;
	public static final int PEERS = 9;
	public Hashtable<String,String> peers = new Hashtable<String,String>(); // not encoded and decoded (in transmission and storage used outside)
	public ArrayList<String> orgs = new ArrayList<String>();
	public Hashtable<String,String> cons = new Hashtable<String,String>();
	public ArrayList<String> neig = new ArrayList<String>();
	public ArrayList<String> witn = new ArrayList<String>();
	public ArrayList<String> moti = new ArrayList<String>();
	public ArrayList<String> just = new ArrayList<String>();
	public Hashtable<String,String> sign = new Hashtable<String,String>();
	public ArrayList<String> tran = new ArrayList<String>();
	public ArrayList<String> news = new ArrayList<String>();
	public String global_organization_ID_hash;
	public int version = 2; // From version 2 on, sign is a Hashtable
	public RequestData() {}
	public RequestData(String GIDH) {global_organization_ID_hash = GIDH;}
	/**
	 * No longer used, since the specific requests now store OrgPeerDataHashes
	 * @param orgID
	 * @throws P2PDDSQLException
	 */
	@Deprecated
	private RequestData(long orgID) throws P2PDDSQLException {
		String sql =
			"SELECT "+table.organization.specific_requests+
			","+table.organization.global_organization_ID_hash+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> r =
			Application.db.select(sql, new String[]{Util.getStringID(orgID)}, DEBUG);
		if(r.size()==0) return;
		String s = Util.getString(r.get(0).get(0));
		this.global_organization_ID_hash = Util.getString(r.get(0).get(1));
		try {
			init(s);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
	public RequestData(String rd, String _global_organization_ID_hash) {
		global_organization_ID_hash = _global_organization_ID_hash;
		try {
			init(rd);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
	public RequestData clone() {
		RequestData result = new RequestData();
		result.orgs = new ArrayList<String>(orgs);
		result.neig = new ArrayList<String>(neig);
		result.peers = new Hashtable<String,String>(peers);
		result.cons = new Hashtable<String,String>(cons);
		result.witn = new ArrayList<String>(witn);
		result.moti = new ArrayList<String>(moti);
		result.just = new ArrayList<String>(just);
		result.sign = new Hashtable<String,String>(sign);
		result.tran = new ArrayList<String>(tran);
		result.news = new ArrayList<String>(news);
		return result;
	}
	public boolean addHashIfNewTo(String hash, String date, int type, int MAX_ITEM) {
		switch(type) {
		case PEERS: return addIfNewToHash(hash, date, peers, MAX_ITEM);
		case CONS: return addIfNewToHash(hash, date, cons, MAX_ITEM);
		case SIGN: return addIfNewToHash(hash, date, sign, MAX_ITEM);
		default:
			Util.printCallPath("Unknown data type: "+type);
		}
		return false;
	}
	public boolean addHashIfNewTo(String hash, int type, int MAX_ITEM) {
		switch(type) {
		case ORGS: return addIfNewToArray(hash, orgs, MAX_ITEM);
		case NEIG: return addIfNewToArray(hash, neig, MAX_ITEM);
		case WITN: return addIfNewToArray(hash, witn, MAX_ITEM);
		case MOTI: return addIfNewToArray(hash, moti, MAX_ITEM);
		case JUST: return addIfNewToArray(hash, just, MAX_ITEM);
		case SIGN: //return addIfNewToArray(hash, sign, MAX_ITEM);
			Util.printCallPath("Signatures are not arrays");
			return addHashIfNewTo(hash, DD.EMPTYDATE, type, MAX_ITEM);
			//throw new RuntimeException("Never come here!");
		case TRAN: return addIfNewToArray(hash, tran, MAX_ITEM);
		case NEWS: return addIfNewToArray(hash, news, MAX_ITEM);
		default:
			Util.printCallPath("Unknown data type: "+type);
		}
		return false;
	}
	private static boolean addIfNewToHash(String hash, String date, Hashtable<String,String> set, int MAX) {
		if(set.containsKey(hash)) return false;
		if((MAX>0)&&(set.size()>=MAX)) {
			String remove = set.keys().nextElement();
			set.remove(remove);
		}
		set.put(hash, date);
		return true;
	}
	private static boolean addIfNewToArray(String hash, ArrayList<String> set, int MAX) {
		if(set.contains(hash)) return false;
		set.add(hash);
		if(set.size()>MAX) set.remove(0);
		return true;
	}
	public void init(String rd) throws ASN1DecoderFail {
		byte[] req = Util.byteSignatureFromString(rd);
		if(rd==null) return;
		Decoder d = new Decoder(req).getContent();
		if(d.isFirstObjectTagByte(Encoder.TAG_SEQUENCE)) {
			if(DEBUG)System.out.println("Old version on the other side");
			version  = 1;
		}else{
			if(d.isFirstObjectTagByte(Encoder.TAG_INTEGER)) {
				version = d.getFirstObject(true).getInteger().intValue();
			}
		}
		orgs = d.getSequenceOfAL(Encoder.TAG_PrintableString);
		neig = d.getSequenceOfAL(Encoder.TAG_PrintableString);
		//peers = d.getSequenceOfHSS(Encoder.TAG_PrintableString, false);
		cons = d.getSequenceOfHSS(Encoder.TAG_PrintableString, false);
		witn = d.getSequenceOfAL(Encoder.TAG_PrintableString);
		moti = d.getSequenceOfAL(Encoder.TAG_PrintableString);
		just = d.getSequenceOfAL(Encoder.TAG_PrintableString);		
		sign = d.getSequenceOfHSS(Encoder.TAG_PrintableString, (version<2));
		tran = d.getSequenceOfAL(Encoder.TAG_PrintableString);		
		news = d.getSequenceOfAL(Encoder.TAG_PrintableString);		
	}
	/**
	 * in database we do not store the orgGID
	 * @param orgID
	 * @throws P2PDDSQLException
	 */
	/*
	@Deprecated
	public void save(long orgID) throws P2PDDSQLException {
		String old = this.global_organization_ID_hash;
		this.global_organization_ID_hash = null;
		Encoder enc = this.getEncoder();
		this.global_organization_ID_hash = old;
		byte[]msg = enc.getBytes();
		String s = Util.stringSignatureFromByte(msg);
		Application.db.update(table.organization.TNAME, new String[]{table.organization.specific_requests},
				new String[]{table.organization.organization_ID}, new String[]{s, Util.getStringID(orgID)}, DEBUG);
	}
	*/
	public void add(RequestData n) {
		orgs = appendSet(orgs, n.orgs);
		neig = appendSet(neig, n.neig);
		peers = appendHash(peers, n.peers);
		cons = appendHash(cons, n.cons);
		witn = appendSet(witn, n.witn);
		moti = appendSet(moti, n.moti);
		just = appendSet(just, n.just);
		sign = appendHash(sign, n.sign);
		tran = appendSet(tran, n.tran);
		news = appendSet(news, n.news);
	}
	public static ArrayList<String> appendSet(ArrayList<String> to,
			ArrayList<String> from) {
		for(String s : from){
			if(!to.contains(s))to.add(s);
		}
		return to;
	}
	public static Hashtable<String, String> appendHash(Hashtable<String, String> to,
			Hashtable<String, String> from) {
		for(String s : from.keySet()){
			//if(!to.containsKey(s))
			to.put(s, from.get(s));
		}
		return to;
	}
	public void add(RequestData n, String _peer_ID, String generalizedTime) {
		orgs = appendSet(orgs, n.orgs, _peer_ID, generalizedTime);
		neig = appendSet(neig, n.neig, _peer_ID, generalizedTime);
		peers = appendHash(peers, n.peers, _peer_ID, generalizedTime);
		cons = appendHash(cons, n.cons, _peer_ID, generalizedTime);
		witn = appendSet(witn, n.witn, _peer_ID, generalizedTime);
		moti = appendSet(moti, n.moti, _peer_ID, generalizedTime);
		just = appendSet(just, n.just, _peer_ID, generalizedTime);
		sign = appendHash(sign, n.sign, _peer_ID, generalizedTime);
		tran = appendSet(tran, n.tran, _peer_ID, generalizedTime);
		news = appendSet(news, n.news, _peer_ID, generalizedTime);
	}
	public static ArrayList<String> appendSet(ArrayList<String> to,
			ArrayList<String> from, String _peer_ID, String generalizedTime) {
		for(String s : from){
			if(!to.contains(s)) to.add(s);
		}
		return to;
	}
	public static Hashtable<String, String> appendHash(Hashtable<String, String> to,
			Hashtable<String, String> from, String _peer_ID, String generalizedTime) {
		for(String s : from.keySet()){
			//if(!to.contains(s)) 
			to.put(s, from.get(s));
		}
		return to;
	}
	@Override
	public Encoder getEncoder() {
		if(version >= 2) return getEncoder_2();
		else return getEncoder_1();
	}
	/**
	 * HSS_Elem ::= SEQUENCE {
			key [PrintableString] IMPLICIT UTF8String,
			value [PrintableString] IMPLICIT UTF8String OPTIONAL
		}
	 * // for version 2
	 * RequestData ::= SEQUENCE {
	 *   orgs [AC15] IMPLICIT SEQUENCE OF PrintableString,
	 *   neig SEQUENCE OF PrintableString,
	 *   cons [AC13] IMPLICIT SEQUENCE OF HSS_Elem,
	 *   witn SEQUENCE OF PrintableString,
	 *   moti SEQUENCE OF PrintableString,
	 *   just SEQUENCE OF PrintableString,
	 *   sign [AC13] IMPLICIT SEQUENCE OF HSS_Elem,
	 *   tran SEQUENCE OF PrintableString,
	 *   news SEQUENCE OF PrintableString,
	 *   global_organization_ID_hash PrintableString,
	 * }
	 * @return
	 */
	public Encoder getEncoder_2() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(Encoder.getStringEncoder(orgs.toArray(new String[0]), Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC15));
		enc.addToSequence(Encoder.getStringEncoder(neig.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getHashStringEncoder(cons, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC13));
		enc.addToSequence(Encoder.getStringEncoder(witn.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(moti.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(just.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getHashStringEncoder(sign, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC13));
		enc.addToSequence(Encoder.getStringEncoder(tran.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(news.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(this.global_organization_ID_hash, false));
		return enc;
	}
	public Encoder getEncoder_1() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(Encoder.getStringEncoder(orgs.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(neig.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getHashStringEncoder(cons, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(witn.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(moti.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(just.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getKeysStringEncoder(sign, Encoder.TAG_PrintableString));			
		enc.addToSequence(Encoder.getStringEncoder(tran.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringEncoder(news.toArray(new String[0]), Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(this.global_organization_ID_hash, false));
		return enc;
	}
	@Override
	public RequestData decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if(d.isFirstObjectTagByte(Encoder.TAG_SEQUENCE)) {
			if(DEBUG)System.out.println("Old version on the other side");
			version  = 1;
		}else{
			if(d.isFirstObjectTagByte(Encoder.TAG_INTEGER)) {
				version = d.getFirstObject(true).getInteger().intValue();
			}
		}
		orgs = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		neig = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		cons = d.getFirstObject(true).getSequenceOfHSS(Encoder.TAG_PrintableString, false);
		witn = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		moti = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		just = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		sign = d.getFirstObject(true).getSequenceOfHSS(Encoder.TAG_PrintableString, (version<2));
		tran = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		news = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		this.global_organization_ID_hash = d.getFirstObject(true).getString();
		return this;
	}
	public static byte getASN1Tag() {
		return Encoder.TAG_SEQUENCE;
	}
	public RequestData instance() {
		return new RequestData();
	}
	public boolean empty() {
		try{
			return 0==orgs.size()+
					neig.size()+
					peers.size()+
					cons.size()+
					moti.size()+
					just.size()+
					witn.size()+
					sign.size()+
					tran.size()+
					news.size();
		}catch(Exception e){
			e.printStackTrace();
//			System.err.println("RequestData: empty: this="+this);
//			System.err.println("RequestData: empty: orgs="+orgs);
//			System.err.println("RequestData: empty: neig="+neig);
//			System.err.println("RequestData: empty: cons="+cons);
//			System.err.println("RequestData: empty: witn="+witn);
//			System.err.println("RequestData: empty: moti="+moti);
			return false;
		}
	}
	public String toString() {
		String result = "\n RequestData [ org_hash="+this.global_organization_ID_hash;
		if((orgs!=null)&&(orgs.size()>0)) result += "\n  orgs="+Util.concat(orgs, ", ", "NULL");
		if((neig!=null)&&(neig.size()>0)) result += "\n  neig="+Util.concat(neig, ", ", "NULL");
		if((peers!=null)&&(peers.size()>0)) result += "\n  peers="+Util.concat(peers, ", ", "NULL");
		if((cons!=null)&&(cons.size()>0)) result += "\n  cons="+Util.concat(cons, ", ", "NULL");
		if((witn!=null)&&(witn.size()>0)) result += "\n  witn="+Util.concat(witn, ", ", "NULL");
		if((moti!=null)&&(moti.size()>0)) result += "\n  moti="+Util.concat(moti, ", ", "NULL");
		if((just!=null)&&(just.size()>0)) result += "\n  just="+Util.concat(just, ", ", "NULL");
		if((sign!=null)&&(sign.size()>0)) result += "\n  vote="+Util.concat(sign, ", ", "NULL");
		if((tran!=null)&&(tran.size()>0)) result += "\n  tran="+Util.concat(tran, ", ", "NULL");
		if((news!=null)&&(news.size()>0)) result += "\n  news="+Util.concat(news, ", ", "NULL");
		result += "]";
		return result;
	}
	public String toSummaryString() {
		String result = "\n RequestData [ org_hash="+this.global_organization_ID_hash;
		if((orgs!=null)&&(orgs.size()>0)) result += "\n  orgs=["+orgs.size()+"]="+Util.concat(orgs, ", ", "NULL");
		if((neig!=null)&&(neig.size()>0)) result += "\n  neig="+Util.concat(neig, ", ", "NULL");
		if((peers!=null)&&(peers.size()>0)) result += "\n  peers="+Util.concat(peers, ", ", "NULL");
		if((cons!=null)&&(cons.size()>0)) result += "\n  cons="+Util.concat(cons, ", ", "NULL");
		if((witn!=null)&&(witn.size()>0)) result += "\n  witn="+Util.concat(witn, ", ", "NULL");
		if((moti!=null)&&(moti.size()>0)) result += "\n  moti="+Util.concat(moti, ", ", "NULL");
		if((just!=null)&&(just.size()>0)) result += "\n  just="+Util.concat(just, ", ", "NULL");
		if((sign!=null)&&(sign.size()>0)) result += "\n  vote="+Util.concat(sign, ", ", "NULL");
		if((tran!=null)&&(tran.size()>0)) result += "\n  tran="+Util.concat(tran, ", ", "NULL");
		if((news!=null)&&(news.size()>0)) result += "\n  news="+Util.concat(news, ", ", "NULL");
		result += "]";
		return result;
	}
	/**
	 * removed what is in obtained
	 * @param obtained
	 */
	public void purge(RequestData obtained) {
		if(obtained==null) return;
		if(DEBUG)System.out.println("RequestData:purge: Will purge "+this+" with "+obtained);
		//if(obtained.empty()) return;
		for(String s : obtained.peers.keySet()) {
			if (this.peers.containsKey(s)) {
				if (!Util.newerDateStr(this.peers.get(s), obtained.peers.get(s)))
					peers.remove(s);				
			}
			String gidh =  D_Peer.getGIDHashFromGID(s);
			if (this.peers.containsKey(gidh)) {
				if (!Util.newerDateStr(this.peers.get(gidh), obtained.peers.get(gidh)))
					peers.remove(gidh);
			}
		}
		for(String s : obtained.orgs) {
			orgs.remove(s);
			orgs.remove(D_Organization.getOrgGIDHashGuess(s));
		}
		for(String s : obtained.cons.keySet()){
			if (this.cons.containsKey(s)) {
				if (!Util.newerDateStr(this.cons.get(s), obtained.cons.get(s)))
					cons.remove(s);				
			}
			String gidh =  D_Constituent.getGIDHashFromGID(s);
			if (this.cons.containsKey(gidh)) {
				if (!Util.newerDateStr(this.cons.get(gidh), obtained.cons.get(gidh)))
					cons.remove(gidh);
			}
		}
		for(String s : obtained.neig) neig.remove(s);
		for(String s : obtained.witn) witn.remove(s);
		for(String s : obtained.moti) moti.remove(s);
		for(String s : obtained.just) just.remove(s);
		for(String s : obtained.sign.keySet()) {
			if (this.sign.containsKey(s)) {
				if (!Util.newerDateStr(this.sign.get(s), obtained.sign.get(s)))
					sign.remove(s);				
			}
		}
		for(String s : obtained.tran) tran.remove(s);
		for(String s : obtained.news) news.remove(s);
		if(DEBUG)System.out.println("RequestData:purge: Got "+this);
	}
	public void update(RequestData sol_rq, RequestData new_rq) {
		for (String s : new_rq.peers.keySet()) {
			if (! this.peers.containsKey(s)) this.peers.put(s, new_rq.peers.get(s));//DD.EMPTYDATE);
			else if (Util.newerDateStr(new_rq.peers.get(s), this.peers.get(s))) this.peers.put(s, new_rq.peers.get(s));
		}
		for(String s : new_rq.cons.keySet()) {
			if (! this.cons.containsKey(s)) this.cons.put(s, new_rq.cons.get(s));//DD.EMPTYDATE);
			else if (Util.newerDateStr(new_rq.cons.get(s), this.cons.get(s))) this.cons.put(s, new_rq.cons.get(s));
		}
		for(String s : new_rq.orgs) if(!this.orgs.contains(s)) this.orgs.add(s);
		for(String s : new_rq.neig) if(!this.neig.contains(s)) this.neig.add(s);
		for(String s : new_rq.witn) if(!this.witn.contains(s)) this.witn.add(s);
		for(String s : new_rq.moti) if(!this.moti.contains(s)) this.moti.add(s);
		for(String s : new_rq.just) if(!this.just.contains(s)) this.just.add(s);
		for(String s : new_rq.sign.keySet()) {
			if (! this.sign.containsKey(s)) this.sign.put(s, new_rq.sign.get(s));//DD.EMPTYDATE);
			else if (Util.newerDateStr(new_rq.sign.get(s), this.sign.get(s))) this.sign.put(s, new_rq.sign.get(s));
		}
		for(String s : new_rq.tran) if(!this.tran.contains(s)) this.tran.add(s);
		for(String s : new_rq.news) if(!this.news.contains(s)) this.news.add(s);
		
		for(String s : sol_rq.peers.keySet()) {
			if (this.peers.containsKey(s)) {
				if (! Util.newerDateStr(this.peers.get(s), sol_rq.peers.get(s)))
					this.peers.remove(s);
			}
		}
		for(String s : sol_rq.cons.keySet()) {
			if (this.cons.containsKey(s)) {
				if (! Util.newerDateStr(this.cons.get(s), sol_rq.cons.get(s)))
					this.cons.remove(s);
			}
		}
		for(String s : sol_rq.orgs) this.orgs.remove(s);
		for(String s : sol_rq.neig) this.neig.remove(s);
		for(String s : sol_rq.witn) this.witn.remove(s);
		for(String s : sol_rq.moti) this.moti.remove(s);
		for(String s : sol_rq.just) this.just.remove(s);
		for(String s : sol_rq.sign.keySet()) {
			if (this.sign.containsKey(s)) {
				if (! Util.newerDateStr(this.sign.get(s), sol_rq.sign.get(s)))
					this.sign.remove(s);
			}
		}
		for(String s : sol_rq.tran) this.tran.remove(s);
		for(String s : sol_rq.news) this.news.remove(s);		
	}
}

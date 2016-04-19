package net.ddp2p.common.streaming;
import java.util.ArrayList;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
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
 * The idea is to eventually use rest of it to evaluate peers if they claim to have data they do not have.
 * TODO
 * For hashes of peers, this should ideally also store the claimed creation date, to know when it was really received.
 * 		
 * ... probably should be something like {data_hash:{peerID:[date_claim_received,creation_data_claimed]}}
 * 
 * should also send authoritarian organization
 * 
 * @author msilaghi
 *
 */
public class GlobalClaimedDataHashes extends ASNObj {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static public GlobalClaimedDataHashes globalClaimedDataHashes = null;
	int version = 2;
	public Hashtable<String,Hashtable<Long,String>> peers=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> orgs=new Hashtable<String,Hashtable<Long,String>>(); 
	public Hashtable<String,Hashtable<Long,String>> news=new Hashtable<String,Hashtable<Long,String>>();
	public Hashtable<String,Hashtable<Long,String>> tran=new Hashtable<String,Hashtable<Long,String>>();
	private GlobalClaimedDataHashes() {}
	public GlobalClaimedDataHashes(Decoder dec) throws ASN1DecoderFail {this.decode(dec);}
	public static GlobalClaimedDataHashes get() {
		if (globalClaimedDataHashes == null) {
			try {
				byte []data = Util.byteSignatureFromString(DD.getAppText(DD.APP_CLAIMED_DATA_HASHES));
				if (data != null) {
					globalClaimedDataHashes = new GlobalClaimedDataHashes(new Decoder(data));
				}
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if (globalClaimedDataHashes == null) {
			globalClaimedDataHashes = new GlobalClaimedDataHashes();
		}
		if (DEBUG) System.out.println("GlobalClaimedDataHashes: get: "+globalClaimedDataHashes);
		return globalClaimedDataHashes;
	}	
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(OrgPeerDataHashes.getDataEncoder(peers));
		enc.addToSequence(OrgPeerDataHashes.getDataEncoder(news));
		enc.addToSequence(OrgPeerDataHashes.getDataEncoder(tran));
		enc.addToSequence(OrgPeerDataHashes.getDataEncoder(orgs));
		enc.setASN1Type(DD.TAG_AC15);
		return enc;
	}
	@Override
	public GlobalClaimedDataHashes decode(Decoder dec) throws ASN1DecoderFail {
		if (dec == null) return this;
		if (dec.getTypeByte() != DD.TAG_AC15) {
			Util.printCallPath("wrong TAG");
			return this;
		}
		Decoder d = dec.getContent();
		peers = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
		news = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
		tran = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
		if (d.getTypeByte() != 0) orgs = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
		return this;
	}
	public void save( 
			) throws P2PDDSQLException {
		if (DEBUG)System.out.println("\nGlobalClaimedDataHashes: save: saving "+this);
		byte[] data = this.encode();
		DD.setAppText(DD.APP_CLAIMED_DATA_HASHES, Util.stringSignatureFromByte(data));
	}	
	public String toString(){
		String result = "GlobalClaimedDataHashes: [\n";
		if((peers!=null)&&(peers.size()>0)) result += "\n  peers = "+getStringRepresentationOfHashes(peers);
		if((orgs!=null)&&(orgs.size()>0)) result += "\n  orgs = "+getStringRepresentationOfHashes(orgs);
		if((news!=null)&&(news.size()>0)) result += "\n  news = "+getStringRepresentationOfHashes(news);
		if((tran!=null)&&(tran.size()>0)) result += "\n  tran = "+getStringRepresentationOfHashes(tran);
		result += "]";
		return result;
	}
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
	public RequestData getRequestData(long peer_ID) {
		Long _peer_ID = new Long(peer_ID);
		RequestData r = new RequestData();
		r.peers = OrgPeerDataHashes.addFromPeer(peers, _peer_ID, r.peers);
		r.orgs = OrgPeerDataHashes.addFromPeer(orgs, _peer_ID, r.orgs);
		r.news = OrgPeerDataHashes.addFromPeer(news, _peer_ID, r.news);
		r.tran = OrgPeerDataHashes.addFromPeer(tran, _peer_ID, r.tran);
		return r;
	}
	public void add(SpecificRequest n, long _peer_ID, String generalizedTime) {
		if (DEBUG) System.out.println("\nOrgPeerDataHashes:add: add "+n+" to "+this);
		synchronized (peers) {
			peers = OrgPeerDataHashes.appendHash(peers, n.peers, _peer_ID, generalizedTime);
			orgs = OrgPeerDataHashes.appendHash(orgs, n.orgs, _peer_ID, generalizedTime);
			tran = OrgPeerDataHashes.appendSet(tran, n.tran, _peer_ID, generalizedTime);
			news = OrgPeerDataHashes.appendSet(news, n.news, _peer_ID, generalizedTime);
		}
		if(DEBUG)System.out.println("OrgPeerDataHashes:add: Got "+this);
	}
	/**
	 * 
	 * @param n
	 * @param _peer_ID
	 * @param generalizedTime
	 */
	public void addPeers(Hashtable<String, String> n, long _peer_ID, String generalizedTime) {
		if (DEBUG) System.out.println("\nGlobalClaimedDataHashes: addPeers: add "+n+" to "+this);
		synchronized (peers) {
			peers = OrgPeerDataHashes.appendHash(peers, n, _peer_ID, generalizedTime);
		}
		if (DEBUG) System.out.println("GlobalClaimedDataHashes: addPeers: Got "+this);
	}
	public void addNews(ArrayList<String> n, long _peer_ID, String generalizedTime) {
		if (DEBUG) System.out.println("\nGlobalClaimedDataHashes: addNews: add "+n+" to "+this);
		synchronized (peers) {
			peers = OrgPeerDataHashes.appendSet(news, n, _peer_ID, generalizedTime);
		}
		if (DEBUG) System.out.println("GlobalClaimedDataHashes: addNews: Got "+this);
	}
	public void addTran(ArrayList<String> n, long _peer_ID, String generalizedTime) {
		if (DEBUG) System.out.println("\nGlobalClaimedDataHashes: addTran: add "+n+" to "+this);
		synchronized (peers) {
			peers = OrgPeerDataHashes.appendSet(news, n, _peer_ID, generalizedTime);
		}
		if (DEBUG) System.out.println("GlobalClaimedDataHashes: addTran: Got "+this);
	}
	public void purgeDated (Hashtable<String,String> obtained, Hashtable<String, Hashtable<Long, String>> local) {
		for (String s : obtained.keySet()){
			if (DEBUG) System.out.println("RequestData: purge: At purge "+s);
			if (local.containsKey(s)) {
				for (Long k: local.get(s).keySet()) {
					if (! Util.newerDateStr(local.get(s).get(k), obtained.get(s))) {
						if (DEBUG) System.out.println("RequestData: purge: Will purge "+s+" at "+k+":"+local.get(s).get(k));
						local.get(s).remove(k);
					} else
						if (DEBUG) System.out.println("RequestData: purge: Will not purge "+s+" at "+k+":"+local.get(s).get(k));
				}
				if (local.get(s).size() <= 0) local.remove(s);
			}
			try {
				String gidh = D_Peer.getGIDHashFromGID(s);
				if (gidh != null) {
					if (local.containsKey(gidh)) {
						if (DEBUG) System.out.println("RequestData: purge: At purge gidh "+gidh);
						for (Long k: local.get(gidh).keySet()) {
							if (! Util.newerDateStr(local.get(gidh).get(k), obtained.get(gidh))) {
								if (DEBUG) System.out.println("RequestData: purge: Will purge "+gidh+" at "+k+":"+local.get(gidh).get(k));
								local.get(gidh).remove(k);
							} else
								if (DEBUG) System.out.println("RequestData: purge: Will not purge "+gidh+" at "+k+":"+local.get(gidh).get(k));
						}
						if (local.get(gidh).size() <= 0) local.remove(gidh);
					}
				}
			} catch (Exception e){e.printStackTrace();}
		}
	}
	/**
	 * removed what is in obtained
	 * @param obtained
	 */
	public void purge(RequestData obtained) {
		if (obtained == null) return;
		if (DEBUG) System.out.println("RequestData: purge: Will purge "+this+" with "+obtained);
		synchronized (peers) {
			purgeDated(obtained.peers, peers);
			purgeDated(obtained.orgs_auth, orgs);
			for(String s : obtained.tran) tran.remove(s);
			for(String s : obtained.news) news.remove(s);
		}
		if(DEBUG)System.out.println("RequestData:purge: Got "+this);
	}
	/**
	 * Add each obtained key to this, as coming from peer_ID at crt_date.
	 * Then remove from this keys that were not in "obtained"
	 * @param obtained
	 * @param peer_ID
	 * @param crt_date
	 */
	public void updateAfterChanges(RequestData orig_rq, RequestData sol_rq, RequestData new_rq, long peer_ID, String crt_date) {
		if(DEBUG)System.out.println("RequestData:updateAfterChanges: Will updateAfterChanges by "+peer_ID+" on \n"+this+
				" with \n new="+new_rq+"\nsolved="+sol_rq);
		for(String s : new_rq.peers.keySet()) OrgPeerDataHashes.add(peers, s, peer_ID, crt_date);
		for(String s : new_rq.orgs_auth.keySet()) OrgPeerDataHashes.add(orgs, s, peer_ID, crt_date);
			for(String s : new_rq.tran) OrgPeerDataHashes.add(tran, s, peer_ID, crt_date);
			for(String s : new_rq.news) OrgPeerDataHashes.add(news, s, peer_ID, crt_date);
			OrgPeerDataHashes.setIntersectionFirstFromSecond(this.peers, sol_rq.peers);
			OrgPeerDataHashes.setIntersectionFirstFromSecond(this.tran, sol_rq.tran);
			OrgPeerDataHashes.setIntersectionFirstFromSecond(this.news, sol_rq.news);
		if(DEBUG)System.out.println("RequestData:updateAfterChanges: Got "+this);
	}
	public boolean empty() {
		return 0 ==
				peers.size()+
				orgs.size()+
				tran.size()+
				news.size();
	}
}

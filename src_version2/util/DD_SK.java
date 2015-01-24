package util;

import static util.Util.__;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import streaming.RequestData;
import streaming.WB_Messages;
import config.Application;
import config.Application_GUI;
import config.DD;
import ciphersuits.PK;
import data.D_Constituent;
import data.D_Justification;
import data.D_Motion;
import data.D_Neighborhood;
import data.D_News;
import data.D_Organization;
import data.D_Peer;
import data.D_Translations;
import data.D_Vote;
import data.D_Witness;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

class SK_SaverThread extends Thread {
	private static final boolean DEBUG = false;
	DD_SK ds;
	SK_SaverThread(DD_SK ds) {
		this.ds = ds;
	}
	public void run() {
		try {
			_run();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void _run() throws P2PDDSQLException{
    	for (DD_SK_Entry d : ds.sk) {
    		try {
	    		String params[] = new String[table.key.fields.length];
	    		PK pk = d.key.getPK();
	    		params[table.key.COL_SK] = Util.stringSignatureFromByte(d.key.encode());
	    		params[table.key.COL_NAME] = d.name;
	    		params[table.key.COL_TYPE] = d.type;
	    		params[table.key.COL_CREATION_DATE] = Encoder.getGeneralizedTime(d.creation);
	       		params[table.key.COL_PK] = Util.stringSignatureFromByte(pk.encode());
	       		params[table.key.COL_IDHASH] = Util.getGIDhash((String)params[table.key.COL_PK]);
	       		Application.db.insert(table.key.TNAME, table.key.fields, params, DEBUG);
    		} catch (Exception e) {
    			Application_GUI.warning(__("Error saving key:")+d+"\n"+e.getLocalizedMessage(), __("Error saving key"));
    			e.printStackTrace();
    		}
     	}
    	/*
    	for (D_Peer p : ds.peer) {
			if (DEBUG) System.out.println("WB_Messages: store: handle peer: "+p);
			if (! p.verifySignature()) {
				if (DEBUG) System.out.println("WB_Messages: store: failed signature for: "+p);
				continue;
			}
			p.fillLocals(null, null);
			if (DEBUG) System.out.println("WB_Messages: store: nou="+p);
			D_Peer old = D_Peer.getPeerByGID_or_GIDhash(p.getGID(), null, true, true, true, ds.sender);
			if (DEBUG) System.out.println("WB_Messages: store: old="+old);
			if (old.loadRemote(p, null, null)) {
				if (old.dirty_any()) {
					old.setArrivalDate();
					old.storeRequest();
					
					config.Application_GUI.inform_arrival(old, null);
				}
			}
			old.releaseReference();
    	}
    	*/
    	WB_Messages wm = new WB_Messages();
    	wm.peers.addAll(ds.peer);
    	if (ds.sender != null) wm.peers.add(0, ds.sender);
    	wm.orgs.addAll(ds.org);
    	wm.neig.addAll(ds.neigh);
    	wm.cons.addAll(ds.constit);
    	wm.witn.addAll(ds.witn);
    	wm.moti.addAll(ds.moti);
       	wm.just.addAll(ds.just);
      	wm.sign.addAll(ds.vote);
      	wm.news.addAll(ds.news);
      	wm.tran.addAll(ds.tran);
      	
		Hashtable<String, RequestData> missing_sr = new Hashtable<String, RequestData>();
		Hashtable<String, RequestData> obtained_sr = new Hashtable<String, RequestData>();
		HashSet<String> orgs = new HashSet<String>();
		String dbg_msg = __("Importing manually!");
		D_Peer myself = ds.sender; //data.HandlingMyself_Peer.get_myself_or_null();
		WB_Messages.store(null, myself, wm, missing_sr, obtained_sr, orgs, dbg_msg);
	}
}

public class DD_SK extends ASNObj implements StegoStructure {
	private static final boolean DEBUG = false;
	public final int V0 = 0;
	int version = V0;
	
	public D_Peer sender;
	
	public ArrayList<DD_SK_Entry> sk = new ArrayList<DD_SK_Entry>();
	public ArrayList<D_Peer> peer = new ArrayList<D_Peer>();
	public ArrayList<D_Organization> org = new ArrayList<D_Organization>();
	public ArrayList<D_Neighborhood> neigh = new ArrayList<D_Neighborhood>();
	public ArrayList<D_Constituent> constit = new ArrayList<D_Constituent>();
	public ArrayList<D_Witness> witn = new ArrayList<D_Witness>();
	public ArrayList<D_Motion> moti = new ArrayList<D_Motion>();
	public ArrayList<D_Justification> just = new ArrayList<D_Justification>();
	public ArrayList<D_Vote> vote = new ArrayList<D_Vote>();
	public ArrayList<D_News> news = new ArrayList<D_News>();
	public ArrayList<D_Translations> tran = new ArrayList<D_Translations>();
	private byte[] signature;
	
	public boolean empty() {
		return sk.size() == 0 
				&& peer.size() == 0
				&& org.size() == 0
				&& neigh.size() == 0
				&& constit.size() == 0
				&& witn.size() == 0
				&& moti.size() == 0
				&& just.size() == 0
				&& vote.size() == 0
				&& news.size() == 0
				&& tran.size() == 0
				&& sender == null;
		// return false;
	}
	
	public String toString() {
		String result = "";
		result += "SK[: sk="+Util.concat(sk, ";;;", "NULL");
		if (peer.size() > 0) result += " peers="+Util.concat(peer, ",,,", "NULL");
		if (org.size() > 0) result += " org="+Util.concat(org, ",,,", "NULL");
		if (constit.size() > 0) result += " constit="+Util.concat(constit, ",,,", "NULL");
		if (witn.size() > 0) result += " witn="+Util.concat(witn, ",,,", "NULL");
		if (moti.size() > 0) result += " moti="+Util.concat(moti, ",,,", "NULL");
		if (just.size() > 0) result += " just="+Util.concat(just, ",,,", "NULL");
		if (vote.size() > 0) result += " vote="+Util.concat(vote, ",,,", "NULL");
		if (news.size() > 0) result += " news="+Util.concat(news, ",,,", "NULL");
		if (tran.size() > 0) result += " tran="+Util.concat(tran, ",,,", "NULL");
		result += "]";
		return result;
	}

	@Override
	public void save() throws P2PDDSQLException {
		if (DEBUG) System.out.println("DD_Testers:save");
		new SK_SaverThread(this).start();
    	if(true){
    		Application_GUI.warning(__("Adding testers:")+" \n"+this.toString(), __("Import"));
    		//return;
    	}
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_Testers:decode "+asn1.length);
		decode(new Decoder(asn1));
		if (this.empty()) throw new ASN1.ASNLenRuntimeException("Empty object!");
		if(DEBUG)System.out.println("DD_Testers:decoded");
	}

	@Override
	public byte[] getBytes() {
		return encode();
	}

	@Override
	public String getNiceDescription() {
		return this.toString();
	}

	@Override
	public String getString() {
		String result = "";
		result += "SK[: sk="+Util.concat(sk, ";;;", "NULL");
		if (peer.size() > 0) result += " peers="+Util.concat(peer, ",,,", "NULL");
		if (org.size() > 0) result += " org="+Util.concat(org, ",,,", "NULL");
		if (constit.size() > 0) result += " constit="+Util.concat(constit, ",,,", "NULL");
		if (witn.size() > 0) result += " witn="+Util.concat(witn, ",,,", "NULL");
		if (moti.size() > 0) result += " moti="+Util.concat(moti, ",,,", "NULL");
		if (just.size() > 0) result += " just="+Util.concat(just, ",,,", "NULL");
		if (vote.size() > 0) result += " vote="+Util.concat(vote, ",,,", "NULL");
		if (news.size() > 0) result += " news="+Util.concat(news, ",,,", "NULL");
		if (tran.size() > 0) result += " tran="+Util.concat(tran, ",,,", "NULL");
		result += "]";
		return result;
	}

	@Override
	public boolean parseAddress(String content) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public short getSignShort() {
		if(DEBUG) System.out.println("DD_SK: get_sign=:"+DD.STEGO_SK);
		return DD.STEGO_SK;
	}
	public static BigInteger getASN1Tag() {
		return new BigInteger(DD.getPositive(DD.STEGO_SK)+"");
	}

	/**
	 * sender has its own signature, so needs not be signed. Its PK has to match anyhow.
	 * @return
	 */
	public Encoder getSignEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AP0));
		if (sk.size() > 0) enc.addToSequence(Encoder.getEncoder(sk).setASN1Type(DD.TAG_AC0));
		if (peer.size() > 0) enc.addToSequence(Encoder.getEncoder(peer).setASN1Type(DD.TAG_AC1));
		if (org.size() > 0) enc.addToSequence(Encoder.getEncoder(org).setASN1Type(DD.TAG_AC2));
		if (neigh.size() > 0) enc.addToSequence(Encoder.getEncoder(neigh).setASN1Type(DD.TAG_AC3));
		if (constit.size() > 0) enc.addToSequence(Encoder.getEncoder(constit).setASN1Type(DD.TAG_AC4));
		if (witn.size() > 0) enc.addToSequence(Encoder.getEncoder(witn).setASN1Type(DD.TAG_AC5));
		if (moti.size() > 0) enc.addToSequence(Encoder.getEncoder(moti).setASN1Type(DD.TAG_AC6));
		if (just.size() > 0) enc.addToSequence(Encoder.getEncoder(just).setASN1Type(DD.TAG_AC7));
		if (vote.size() > 0) enc.addToSequence(Encoder.getEncoder(vote).setASN1Type(DD.TAG_AC8));
		if (news.size() > 0) enc.addToSequence(Encoder.getEncoder(news).setASN1Type(DD.TAG_AC9));
		if (tran.size() > 0) enc.addToSequence(Encoder.getEncoder(tran).setASN1Type(DD.TAG_AC10));
		return enc;
}

	@Override
	public Encoder getEncoder() {
		Encoder enc = getSignEncoder();
		if (sender != null) enc.addToSequence(sender.getEncoder().setASN1Type(DD.TAG_AP1));
		if (signature != null) enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AP2));	
		enc.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, getASN1Tag());
		return enc;
	}
	@Override
	public DD_SK decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if (d.getTypeByte() == DD.TAG_AP0) version = d.getFirstObject(true).getInteger().intValue();
		if (d.getTypeByte() == DD.TAG_AC0)
			sk =  d.getFirstObject(true).getSequenceOfAL(DD_SK_Entry.getASN1Tag(), new DD_SK_Entry());
		if (d.getTypeByte() == DD.TAG_AC1)
			peer =  d.getFirstObject(true).getSequenceOfAL(D_Peer.getASN1Type(), D_Peer.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC2)
			org =  d.getFirstObject(true).getSequenceOfAL(D_Organization.getASN1Type(), D_Organization.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC3)
			neigh =  d.getFirstObject(true).getSequenceOfAL(D_Neighborhood.getASN1Type(), D_Neighborhood.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC4)
			constit =  d.getFirstObject(true).getSequenceOfAL(D_Constituent.getASN1Type(), D_Constituent.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC5)
			witn =  d.getFirstObject(true).getSequenceOfAL(D_Witness.getASN1Type(), D_Witness.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC6)
			moti =  d.getFirstObject(true).getSequenceOfAL(D_Motion.getASN1Type(), D_Motion.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC7)
			just =  d.getFirstObject(true).getSequenceOfAL(D_Justification.getASN1Type(), D_Justification.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC8)
			vote =  d.getFirstObject(true).getSequenceOfAL(D_Vote.getASN1Type(), D_Vote.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC10)
			tran =  d.getFirstObject(true).getSequenceOfAL(D_Translations.getASN1Type(), D_Translations.getEmpty());
		if (d.getTypeByte() == DD.TAG_AP1) sender = D_Peer.getEmpty().decode(d.getFirstObject(true));
		if (d.getTypeByte() == DD.TAG_AP2) signature = d.getFirstObject(true).getBytes(DD.TAG_AP2);
		return this;
	}
	/**
	 * Sets the sender and computes the signature
	 * @param me
	 */
	public void sign_and_set_sender(D_Peer me) {
		if (me == null) {
			sender = null;
			signature = null;
			return;
		}
		//signature = null;
		byte[] msg = this.getSignEncoder().getBytes();
		signature = Util.sign(msg, me.getSK());
		this.sender = me;
	}
	/**
	 * Verifies the signature.
	 * Always false if there is no sender.
	 * @return
	 */
	public boolean verify() {
		if (sender == null) return false;
		byte[] msg = this.getSignEncoder().getBytes();
		return Util.verifySign(msg, sender.getPK(), signature);
	}
}
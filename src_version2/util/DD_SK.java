package util;

import static util.Util.__;

import java.util.ArrayList;

import config.Application;
import config.Application_GUI;
import config.DD;
import ciphersuits.PK;
import data.D_Constituent;
import data.D_Organization;
import data.D_Peer;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

class SK_SaverThread extends Thread {
	private static final boolean DEBUG = false;
	DD_SK ds;
	SK_SaverThread(DD_SK ds){
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
    	for (D_Peer p : ds.peer) {
			if (DEBUG) System.out.println("WB_Messages: store: handle peer: "+p);
			if (! p.verifySignature()) {
				if (DEBUG) System.out.println("WB_Messages: store: failed signature for: "+p);
				continue;
			}
			p.fillLocals(null, null);
			if (DEBUG) System.out.println("WB_Messages: store: nou="+p);
			D_Peer old = D_Peer.getPeerByGID_or_GIDhash(p.getGID(), null, true, true, true, null);
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
	}
}

public class DD_SK extends ASNObj implements StegoStructure {
	private static final boolean DEBUG = false;
	public final int V0 = 0;
	int version = V0;
	
	public ArrayList<DD_SK_Entry> sk = new ArrayList<DD_SK_Entry>();
	public ArrayList<D_Peer> peer = new ArrayList<D_Peer>();
	public ArrayList<D_Organization> org = new ArrayList<D_Organization>();
	public ArrayList<D_Constituent> constit = new ArrayList<D_Constituent>();
	
	public String toString() {
		return "SK[: sk="+Util.concat(sk, ";;;", "NULL")
				+ " peers="+Util.concat(peer, ",,,", "NULL")
				+" org="+Util.concat(org, ",,,", "NULL")
				+" constit="+Util.concat(constit, ",,,", "NULL")
				+ "]";
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
		return "SK[: sk="+Util.concat(sk, ";;;", "NULL")
				+" peers="+Util.concat(peer, ",,,", "NULL")
				+" org="+Util.concat(org, ",,,", "NULL")
				+" constit="+Util.concat(constit, ",,,", "NULL")
				+"]";
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

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AP0));
		if (sk.size() > 0) enc.addToSequence(Encoder.getEncoder(sk).setASN1Type(DD.TAG_AC0));
		if (peer.size() > 0) enc.addToSequence(Encoder.getEncoder(peer).setASN1Type(DD.TAG_AC1));
		if (org.size() > 0) enc.addToSequence(Encoder.getEncoder(org).setASN1Type(DD.TAG_AC2));
		if (constit.size() > 0) enc.addToSequence(Encoder.getEncoder(constit).setASN1Type(DD.TAG_AC3));
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
			org =  d.getFirstObject(true).getSequenceOfAL(D_Constituent.getASN1Type(), D_Organization.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC3)
			constit =  d.getFirstObject(true).getSequenceOfAL(D_Constituent.getASN1Type(), D_Constituent.getEmpty());
		return this;
	}
}
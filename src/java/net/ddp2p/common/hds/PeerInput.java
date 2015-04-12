package net.ddp2p.common.hds;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.CipherSuit;
import net.ddp2p.common.data.D_Peer;

/**
 * Data communicated by a CreatePeer Dialog
 * @author msilaghi
 *
 */
public
class PeerInput extends ASNObj {
	public String name;
	public String slogan;
	public CipherSuit cipherSuite;
	public boolean valid;
	public String email;
	public String instance;
	public int version = 1;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(name));
		enc.addToSequence(new Encoder(slogan));
		enc.addToSequence(new Encoder(email));
		enc.addToSequence(new Encoder(instance));
		enc.addToSequence(new Encoder(valid));
		enc.addToSequence(new Encoder(cipherSuite != null));
		enc.addToSequence(new Encoder(cipherSuite.cipher));
		enc.addToSequence(new Encoder(cipherSuite.hash_alg));
		enc.addToSequence(new Encoder(cipherSuite.ciphersize));
		return enc;
	}
	@Override
	public PeerInput decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		name = d.getFirstObject(true).getString();
		slogan = d.getFirstObject(true).getString();
		email = d.getFirstObject(true).getString();
		instance = d.getFirstObject(true).getString();
		valid = d.getFirstObject(true).getBoolean();
		boolean hasCS = d.getFirstObject(true).getBoolean();
		if (hasCS) {
			cipherSuite = new CipherSuit();
			cipherSuite.cipher = d.getFirstObject(true).getString();
			cipherSuite.hash_alg = d.getFirstObject(true).getString();
			cipherSuite.ciphersize = d.getFirstObject(true).getInteger().intValue();
		}
		return this;
	}
	public PeerInput() {
		
	}
	public PeerInput(String name2, String slogan2, String email2) {
		name = name2;
		email = email2;
		slogan = slogan2;
	}
	public PeerInput(String name2, String slogan2, String email2, CipherSuit object) {
		name = name2;
		email = email2;
		slogan = slogan2;
		if(object != null) {
			object = new CipherSuit(null);
		}
		this.cipherSuite = object;
	}
	public String toString() {
		String result = "";
		result += "\n\tname="+name;
		result += "\n\tslogan="+slogan;
		result += "\n\tvalid="+valid;
		result += "\n\temail="+email;
		result += "\n\tinstance="+instance;
		result += "\n\tciphersuit=["+cipherSuite+"]";
		return result;
	}
	public static PeerInput getPeerInput(D_Peer This) {
		PeerInput result = new PeerInput();
		result.email = This.getEmail();
		result.slogan = This.getSlogan();
		result.name = This.getName();
		result.instance = This.getInstance();
		result.cipherSuite = This.getCipherSuite();
		return result;
	}
	public void incName() {
		name = name+" (2)";
	}
}
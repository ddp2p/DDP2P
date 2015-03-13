package net.ddp2p.common.hds;

import java.math.BigInteger;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;

/**
 * @author msilaghi
 *
 */
public
class ASNUDPPing extends ASNObj{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	boolean senderIsPeer = false;
	boolean senderIsInitiator = false;
	String peer_globalID; // contacted peer ID
	String peer_instance; // contacted peer ID
	int peer_port;		 // ping sender port
	//String peer_domains[]; // ping sender domains
	String peer_domain; // ping sender domains

	String initiator_globalID; // initiator ID
	public String initiator_instance;
	int initiator_port;		 // ping initiator port
	public String initiator_domain;
	
	public static byte getASN1Tag() {
		return DD.TAG_AC13;
	}
	/**
 * ASNUDPPing = IMPLICIT [APPLICATION 13] SEQUENCE {
 * 		senderIsPeer BOOLEAN,
 * 		senderIsInitiator BOOLEAN,
 * 		peer_port INTEGER,
 * 		initiator_port INTEGER,
 * 		peer_domain UTF8String,
 * 		initiator_domain UTF8String,
 * 		peer_globalID	PrintableString OPTIONAL
 * 		initiator_globalID	PrintableString OPTIONAL
 * 		peer_instance	[AP9] UTF8String OPTIONAL
 * 		initiator_instance [AP10]	UTF8String OPTIONAL
 * }
	 * 
	 */
	@Override
	public Encoder getEncoder() {
		if (ASNSyncRequest.DEBUG) System.out.println("Encoding ASNUDPPing");
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(senderIsPeer));
		enc.addToSequence(new Encoder(senderIsInitiator));
		enc.addToSequence(new Encoder(new BigInteger(""+peer_port)));
		enc.addToSequence(new Encoder(new BigInteger(""+initiator_port)));
		//enc.addToSequence(Encoder.getStringEncoder(peer_domains, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(peer_domain));
		//enc.addToSequence(Encoder.getStringEncoder(initiator_domains, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(initiator_domain));
		if (peer_globalID != null) enc.addToSequence(new Encoder(peer_globalID, false).setASN1Type(DD.TAG_AP7));
		if (initiator_globalID != null) enc.addToSequence(new Encoder(initiator_globalID, false).setASN1Type(DD.TAG_AP8));
		if (peer_instance != null) enc.addToSequence(new Encoder(peer_instance, false).setASN1Type(DD.TAG_AP9));
		if (initiator_instance != null) enc.addToSequence(new Encoder(initiator_instance, false).setASN1Type(DD.TAG_AP10));
		enc.setASN1Type(getASN1Tag());
		
		try {
			if (DEBUG) System.out.println("\n\n\nEncode: "+this+"\n\nTo:"+new ASNUDPPing().decode(new Decoder(enc.getBytes()))+"\n\n");
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		return enc;
	}
	@Override
	public ASNUDPPing decode(Decoder dec) throws ASN1DecoderFail {
		try {
			Decoder d = dec.getContent();
			senderIsPeer = d.getFirstObject(true).getBoolean();
			senderIsInitiator = d.getFirstObject(true).getBoolean();
			peer_port = d.getFirstObject(true).getInteger().intValue();
			initiator_port = d.getFirstObject(true).getInteger().intValue();
			//peer_domains = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
			peer_domain = d.getFirstObject(true).getString();
			//initiator_domains = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
			initiator_domain = d.getFirstObject(true).getString();
			if (d.isFirstObjectTagByte(DD.TAG_AP7)
					//||(d.getTypeByte()!=0)
					) peer_globalID = d.getFirstObject(true).getString(DD.TAG_AP7);
			if (d.isFirstObjectTagByte(DD.TAG_AP8)
					//||(d.getTypeByte()!=0)
				) initiator_globalID = d.getFirstObject(true).getString(DD.TAG_AP8);
			if (d.isFirstObjectTagByte(DD.TAG_AP9)) peer_instance = d.getFirstObject(true).getString(DD.TAG_AP9);
			if (d.isFirstObjectTagByte(DD.TAG_AP10)) initiator_instance = d.getFirstObject(true).getString(DD.TAG_AP10);
			if (DEBUG) System.out.println("DeEncode: "+this);
		} catch (RuntimeException e) {
			//e.printStackTrace();
			//System.out.println(e+"\n: "+dec);
			throw new ASN1DecoderFail(e+"");
		}
		return this;
	}
	public String toString() {
		return "ASNUDPPing: fromPeer:"+senderIsPeer+" fromInitiator:"+senderIsInitiator
		//+" peer port:"+peer_port+"; domains:"+Util.concat(peer_domains, ",")+"; ID="+peer_globalID
		+"\n peer port:"+peer_port+";\n    domains:"+peer_domain+";\n    ID="+Util.trimmed(peer_globalID)+":"+peer_instance
		//+" initiator port:"+initiator_port+"; domains:"+Util.concat(initiator_domains, ",")+"; ID="+initiator_globalID;
		+"\n initiator port:"+initiator_port+";\n     domains:"+initiator_domain+";\n     ID="+Util.trimmed(initiator_globalID)+":"+initiator_instance;
	}
}
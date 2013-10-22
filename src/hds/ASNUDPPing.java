package hds;

import java.math.BigInteger;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

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
 * }
 * @author msilaghi
 *
 */
public
class ASNUDPPing extends ASNObj{
	boolean senderIsPeer=false;
	boolean senderIsInitiator=false;
	String peer_globalID; // contacted peer ID
	int peer_port;		 // ping sender port
	//String peer_domains[]; // ping sender domains
	String peer_domain; // ping sender domains
	String initiator_globalID; // initiator ID
	int initiator_port;		 // ping initiator port
	public String initiator_domain;
	
	@Override
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding ASNUDPPing");
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(senderIsPeer));
		enc.addToSequence(new Encoder(senderIsInitiator));
		enc.addToSequence(new Encoder(new BigInteger(""+peer_port)));
		enc.addToSequence(new Encoder(new BigInteger(""+initiator_port)));
		//enc.addToSequence(Encoder.getStringEncoder(peer_domains, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(peer_domain));
		//enc.addToSequence(Encoder.getStringEncoder(initiator_domains, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(initiator_domain));
		if(peer_globalID!=null) enc.addToSequence(new Encoder(peer_globalID, false));
		if(initiator_globalID!=null) enc.addToSequence(new Encoder(initiator_globalID,false));
		enc.setASN1Type(DD.TAG_AC13);
		return enc;
	}
	@Override
	public ASNUDPPing decode(Decoder dec) throws ASN1DecoderFail {
		try{
		Decoder d = dec.getContent();
		senderIsPeer = d.getFirstObject(true).getBoolean();
		senderIsInitiator = d.getFirstObject(true).getBoolean();
		peer_port = d.getFirstObject(true).getInteger().intValue();
		initiator_port = d.getFirstObject(true).getInteger().intValue();
		//peer_domains = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		peer_domain = d.getFirstObject(true).getString();
		//initiator_domains = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		initiator_domain = d.getFirstObject(true).getString();
		peer_globalID = d.getFirstObject(true).getString();
		initiator_globalID = d.getFirstObject(true).getString();
		}catch(RuntimeException e){
			//e.printStackTrace();
			//System.out.println(e+"\n: "+dec);
			throw new ASN1DecoderFail(e+"");
		}
		return this;
	}
	public String toString() {
		return "ASNUDPPing: fromPeer:"+senderIsPeer+" fromInitiator:"+senderIsInitiator
		//+" peer port:"+peer_port+"; domains:"+Util.concat(peer_domains, ",")+"; ID="+peer_globalID
		+"\n peer port:"+peer_port+";\n    domains:"+peer_domain+";\n    ID="+Util.trimmed(peer_globalID)
		//+" initiator port:"+initiator_port+"; domains:"+Util.concat(initiator_domains, ",")+"; ID="+initiator_globalID;
		+"\n initiator port:"+initiator_port+";\n     domains:"+initiator_domain+";\n     ID="+Util.trimmed(initiator_globalID);
	}
}
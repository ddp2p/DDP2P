package registration;

import hds.TypedAddress;

import java.util.Calendar;

import config.DD;

import util.Util;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import data.D_PeerAddress;
import data.D_PeerOrgs;

/**
WB_Peer ::= SEQUENCE {
	id PrintableString,
	name UTF8String,
	slogan UTF8String [0] OPTIONAL,
	address UTF8String [1] OPTIONAL,
	date GeneralizedDate,
	signature OCTET_STRING [2] OPTIONAL	
}
 */
/**
 * Please use PeerAddress instead
 */
@Deprecated
public class WB_ASN_Peer extends ASNObj{
	
	public String global_peer_id; //Printable
	public String name; //UTF8
	public String slogan; //UTF8
	public hds.TypedAddress[] address; //UTF8
	public Calendar creation_date;
	public byte[] signature; //OCT STR
	public String global_peer_ID_hash;
	public Boolean broadcastable;
	public String[] hash_alg;
	public String version = hds.DDAddress.V0;
	public byte[] picture = null;
	public D_PeerOrgs[] served_orgs = null; //OPT
	//public String type; 
	public WB_ASN_Peer() {
	}
	public WB_ASN_Peer(D_PeerAddress pa) {
		global_peer_id = pa.globalID; //Printable
		name = pa.name; //UTF8
		slogan = pa.slogan; //UTF8
		address = pa.address; //UTF8
		creation_date = pa.creation_date;
		signature = pa.signature; //OCT STR
		global_peer_ID_hash =Util.getGIDhash(pa.globalID);
		broadcastable = pa.broadcastable;
		hash_alg = pa.signature_alg;
		version = pa.version;
		picture = pa.picture;
		served_orgs = pa.served_orgs; //OPT
		//type = pa.type;
	}
	@Override
	public Encoder getEncoder() {
	Encoder enc = new Encoder().initSequence();
	if(global_peer_id!=null) enc.addToSequence(new Encoder(global_peer_id).setASN1Type(Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
	if(name!=null) enc.addToSequence(new Encoder(name).setASN1Type(Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
	if(slogan!=null)enc.addToSequence(new Encoder(slogan).setASN1Type(Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC2));
	if(address!=null)enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC3));
	if(creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
	if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC5));
	if(global_peer_ID_hash!=null) enc.addToSequence(new Encoder(global_peer_ID_hash).setASN1Type(Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
	if(hash_alg!=null) enc.addToSequence(Encoder.getStringEncoder(hash_alg,Encoder.TAG_PrintableString)).setASN1Type(DD.TAG_AC7);
	if(broadcastable!=null) enc.addToSequence(new Encoder(broadcastable).setASN1Type(Encoder.TAG_BOOLEAN).setASN1Type(DD.TAG_AC8));
	return enc;
	}
	@Override
	public WB_ASN_Peer decode(Decoder decoder) throws ASN1DecoderFail {
	Decoder dec = decoder.getContent();
	if(dec.getTypeByte()==DD.TAG_AC0)global_peer_id = dec.getFirstObject(true).getString(DD.TAG_AC0);
	if(dec.getTypeByte()==DD.TAG_AC1) name = dec.getFirstObject(true).getString(DD.TAG_AC1);
	if(dec.getTypeByte()==DD.TAG_AC2) slogan = dec.getFirstObject(true).getString(DD.TAG_AC2);
	if(dec.getTypeByte() == DD.TAG_AC3) address = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE,
			new TypedAddress[]{}, new TypedAddress());
	else address=null;
	//if(dec.getTypeByte()==DD.TAG_AC3) address = dec.getFirstObject(true).getString(DD.TAG_AC3);
	if(dec.getTypeByte()==DD.TAG_AC4)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC4);
	if(dec.getTypeByte()==DD.TAG_AC5) signature = dec.getFirstObject(true).getBytes(DD.TAG_AC5);
	if(dec.getTypeByte()==DD.TAG_AC6) global_peer_ID_hash = dec.getFirstObject(true).getString(DD.TAG_AC6);
	if(dec.getTypeByte()==DD.TAG_AC7) hash_alg = dec.getFirstObject(true).getSequenceOf(DD.TAG_AC7);
	if(dec.getTypeByte()==DD.TAG_AC8) broadcastable = dec.getFirstObject(true).getBoolean();
	return this;
	}
	
}

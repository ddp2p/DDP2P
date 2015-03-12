package hds;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

public class DIR_Terms_Requested extends ASNObj {
	public int version=0;
	public String topic;
	public DIR_Payment_Request payment;
	public byte[] services_available;
	public int ad=-1, plaintext=-1;
	@Override
	public ASNObj instance() throws CloneNotSupportedException{return (ASNObj) new DIR_Terms_Requested();}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(version != 0) enc.addToSequence(new Encoder(version));
		if(topic != null) enc.addToSequence(new Encoder(topic).setASN1Type(DD.TAG_AP1));
		if(payment != null) enc.addToSequence(payment.getEncoder().setASN1Type(DD.TAG_AC2));
		if(services_available != null) enc.addToSequence(new Encoder(services_available).setASN1Type(DD.TAG_AP3));
		if(ad > 0) enc.addToSequence(new Encoder(ad).setASN1Type(DD.TAG_AP4));
		if(plaintext > 0) enc.addToSequence(new Encoder(plaintext).setASN1Type(DD.TAG_AP5));
		return enc.setASN1Type(getASN1Type());
	}
	@Override
	public DIR_Terms_Requested decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent(); version = 0;
		if(d.getTypeByte()==Encoder.TAG_INTEGER) version = d.getFirstObject(true).getInteger().intValue();
		if(d.getTypeByte()==DD.TAG_AP1) topic = d.getFirstObject(true).getString();
		if(d.getTypeByte()==DD.TAG_AC2) payment = new DIR_Payment_Request().decode(d.getFirstObject(true));
		if(d.getTypeByte()==DD.TAG_AP3) services_available = d.getFirstObject(true).getBytesAnyType();
		if(d.getTypeByte()==DD.TAG_AP4) ad = d.getFirstObject(true).getInteger().intValue();
		if(d.getTypeByte()==DD.TAG_AP5) plaintext = d.getFirstObject(true).getInteger().intValue();
		return this;
	}
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
}
package util;

import java.util.Calendar;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import ciphersuits.Cipher;
import ciphersuits.SK;
import config.DD;

public
class DD_SK_Entry extends ASNObj {
	int version = 0;
	public SK key;
	public String name, type;
	public Calendar creation;
	
	public String toString() {
		return "DD_SK_Entry: [ name="+name+" type="+type+"]";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AP0));
		enc.addToSequence(key.getEncoder().setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AP1));
		enc.addToSequence(new Encoder(type).setASN1Type(DD.TAG_AP2));
		enc.addToSequence(new Encoder(creation).setASN1Type(DD.TAG_AP3));
		enc.setASN1Type(getASN1Tag());
		return enc;
	}
	@Override
	public DD_SK_Entry decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if (d.isFirstObjectTagByte(DD.TAG_AP0)) version = d.getFirstObject(true).getInteger(DD.TAG_AP0).intValue();
		if (d.isFirstObjectTagByte(DD.TAG_AC0)) key = Cipher.getSK(d.getFirstObject(true));
		if (d.isFirstObjectTagByte(DD.TAG_AP1)) name = d.getFirstObject(true).getString(DD.TAG_AP1);
		if (d.isFirstObjectTagByte(DD.TAG_AP2)) type = d.getFirstObject(true).getString(DD.TAG_AP2);
		if (d.isFirstObjectTagByte(DD.TAG_AP3)) creation = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		return this;
	}
	public DD_SK_Entry instance() {return new DD_SK_Entry();}
	public static byte getASN1Tag() {
		return DD.TAG_AC10;
	}
	
}
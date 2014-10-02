package registration;

import java.util.Calendar;

import data.D_FieldValue;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
@Deprecated
public class ASNConstituent extends ASNObj{
	public String id; // Print
	public String forename; // UTF
	public String surname; //UTF
	public byte[] gificon; // OCT STR OPT
	public String neighID; // Print
	public String email; // Print
	public Calendar date;
	public String slogan;//UTF
	public String languages;// Print
	//public ASNLocationItem[] postalAddress;
	public D_FieldValue[] postalAddress;
	public String hash_alg;
	public byte[] hash;
	public byte[] certificate;
	public Object cerReq;
	public byte[] signature;
	public Object cert_hash_alg;
	public boolean external;
	public String toString() {
		return "ASNConstituent: "+
		"; id="+id+
		"; forename="+forename+
		"; surname="+surname+
		"; gificon="+gificon+
		"; neighID="+neighID+
		"; email="+email+
		"; date="+date+
		"; slogan="+slogan+
		"; languages="+languages+
		"; postalAddress="+Util.concat(postalAddress, ":")+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id, false));
		enc.addToSequence(new Encoder(forename, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(surname, Encoder.TAG_UTF8String));
		if(gificon!=null) enc.addToSequence(new Encoder(gificon));
		enc.addToSequence(new Encoder(neighID, false));
		enc.addToSequence(new Encoder(email, Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(date));
		enc.addToSequence(new Encoder(slogan, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(languages, Encoder.TAG_PrintableString));
		if (postalAddress != null) enc.addToSequence(Encoder.getEncoder(postalAddress));
		return enc;
	}

	@Override
	public ASNConstituent decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec=decoder.getContent();
		id = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		forename = dec.getFirstObject(true).getString();
		surname = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING) gificon = dec.getFirstObject(true).getBytes();
		neighID = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		email = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		slogan = dec.getFirstObject(true).getString();
		languages = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		//postalAddress = dec.getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNLocationItem[]{}, new ASNLocationItem());
		postalAddress = dec.getSequenceOf(Encoder.TYPE_SEQUENCE, new D_FieldValue[]{}, new D_FieldValue());
		if(dec.getTypeByte()!=Encoder.TAG_EOC) throw new ASN1DecoderFail("Redundant objects in ASNConstituent");
		return this;
	}
}

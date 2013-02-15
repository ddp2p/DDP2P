
package registration;

import java.util.Calendar;

import config.DD;
import data.D_FieldExtra;


import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
WB_ORGANIZATION ::= SEQUENCE {
	global_organizationID PrintableString,
	name UTF8String,
	description UTF8String,
	details SEQUENCE OF WB_FieldExtra [0] OPTIONAL,
	creator WB_Peer [1] OPTIONAL
	date GeneralizedDate,
	signature OCTET_STRING
}
 */
@Deprecated
public class WB_ASN_Organization extends ASNObj{

	public String global_organizationID;//Printable
	public String name;//UTF8
	public String description;//UTF8
	public D_FieldExtra details;
	public WB_ASN_Peer creator;//UTF8
	public String signature; //OCT STR
	public String global_organization_ID_hash; 
	public Calendar creation_date;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(global_organizationID!=null) enc.addToSequence(new Encoder(global_organizationID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(name!=null) enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(description!=null) enc.addToSequence(new Encoder(description,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC2));
		if(details!=null)enc.addToSequence(details.getEncoder().setASN1Type(DD.TAG_AC3));
		if(creator!=null)enc.addToSequence(creator.getEncoder().setASN1Type(DD.TAG_AC4));
		if(creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC5));
		if(signature!=null) enc.addToSequence(new Encoder(signature).setASN1Type(Encoder.TAG_OCTET_STRING).setASN1Type(DD.TAG_AC6));
		if(global_organization_ID_hash!=null) enc.addToSequence(new Encoder(global_organization_ID_hash).setASN1Type(DD.TAG_AC7));
		//if(creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		return enc;
	}

	@Override
	public WB_ASN_Organization decode(Decoder decoder) throws ASN1DecoderFail {

		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)global_organizationID = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)name = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)description = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3) details= new D_FieldExtra().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC4) creator = new WB_ASN_Peer().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC5)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC5);
		if(dec.getTypeByte()==DD.TAG_AC6)signature = dec.getFirstObject(true).getString(DD.TAG_AC6);
		if(dec.getTypeByte()==DD.TAG_AC7)global_organization_ID_hash = dec.getFirstObject(true).getString(DD.TAG_AC7);
		//if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("More data available: "+dec.getTypeByte()); 
		return this;
	}
	
}

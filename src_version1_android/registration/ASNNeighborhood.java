package registration;

import hds.ASNPoint;
import hds.ASNSyncRequest;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
@Deprecated
public 
class ASNNeighborhood extends ASNObj{
	public String name; //UTF
	public String description; // UTF
	public String global_neighborhood_ID;
	public ASNPoint[] boundary;
	public String name_lang;
	public String name_division;
	public String[] names_subdivisions;
	public String parent_global_ID;
	public String submitter_global_ID;
	public byte[] picture;
	public byte[] signature;
	public String toString() {
		return "ASNNeighborhood: "+
		"; name="+name+
		"; description="+description+
		"; global_neighborhood_ID="+global_neighborhood_ID+
		"; boundary=["+Util.concat(boundary, ":")+"]"+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(name));
		if(description!=null)enc.addToSequence(new Encoder(description));
		enc.addToSequence(new Encoder(global_neighborhood_ID, false));
		if(boundary!=null) enc.addToSequence(Encoder.getEncoder(boundary));
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded ASNNeighborhood");
		return enc;
	}
	@Override
	public ASNNeighborhood decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		name = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==Encoder.TAG_UTF8String)
			description = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		global_neighborhood_ID = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==Encoder.TAG_SEQUENCE)
			boundary = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNPoint[]{}, new ASNPoint());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoded ASNNeighborhood");
		return this;
	}
}

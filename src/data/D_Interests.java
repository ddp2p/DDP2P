package data;

import java.util.ArrayList;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

//public D_Interests interests;
public class D_Interests extends ASNObj{
	public ArrayList<String> org_ID_hashes;
	public ArrayList<String> const_ID_hashes;
	public ArrayList<String> motion_ID;
	public ArrayList<String> neighborhood_ID;
	
	double latitude, longitude, altitude;
	double bearing, speed;
	long timeGPS; // time from satellite
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(Encoder.getStringsEncoder(org_ID_hashes, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringsEncoder(const_ID_hashes, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringsEncoder(motion_ID, Encoder.TAG_PrintableString));
		enc.addToSequence(Encoder.getStringsEncoder(neighborhood_ID, Encoder.TAG_PrintableString));
		return enc;
	}

	@Override
	public D_Interests decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		org_ID_hashes = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		const_ID_hashes = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		motion_ID = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		neighborhood_ID = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
		return this;
	}
	
}
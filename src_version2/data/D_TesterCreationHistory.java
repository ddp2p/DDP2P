package data;

import java.util.Calendar;

import util.Util;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_TesterCreationHistory extends ASNObj  {
	public String creatorPeerGID;//// the peer who has introduced the tester in the recommendation 
	public String weight;  // the initial weight given by the creator peer; encoded in ASN1 as a String
	public float _weight;  // encoded in ASN1 as a String
	public Calendar creation_date; //date set by the creator when introduced the tester first time
	public byte[] signature; //signature of date,weight and tester_GID (from tester table if exist??)
	@Override
	public D_TesterCreationHistory instance() {
		return new D_TesterCreationHistory();
	}
	
	public String toString() {
		return this.toTXT();
	}
	public String toTXT() {
		String result ="";
		result += this.creatorPeerGID+"\r\n";
		result += this.weight+"\r\n";
		result += this.creation_date+"\r\n";
		return result;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(creatorPeerGID));
		enc.addToSequence(new Encoder(weight));
		enc.addToSequence(new Encoder(creation_date));
		enc.addToSequence(new Encoder(signature));
		enc.setASN1Type(getASNType());
		return enc;
	}
	
	static byte getASNType() { 
		return DD.TAG_AC17;
	}
		@Override
	public D_TesterCreationHistory decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		creatorPeerGID = d.getFirstObject(true).getString();
		_weight = Util.fval(weight = d.getFirstObject(true).getString(), 0.0f);
//		creation_date = d.getFirstObject(true).getString();
//		signature = d.getFirstObject(true).getString();
		return this;
	}
	
}
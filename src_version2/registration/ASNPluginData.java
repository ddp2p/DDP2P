package registration;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

/**
PluginData ::= SEQUENCE {
	  id PrintableString,
	  data OCTET STRING
	}
*/
@Deprecated
public
class ASNPluginData extends ASNObj{
	private static final byte TAG = DD.TAG_AC12;
	String id;
	byte[] data;
	public ASNPluginData instance() {return new ASNPluginData();}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder();
		enc.addToSequence(new Encoder(id, false));
		enc.addToSequence(new Encoder(data));		
		enc.setASN1Type(TAG);
		return enc;
	}
	@Override
	public ASNPluginData decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		id = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		data = dec.getFirstObject(true).getBytes();		
		return this;
	}
	public Decoder getPayload() {
		return new Decoder(data);
	}
	public ASNObj getDecodedPayload(ASNObj dat) throws ASN1DecoderFail {
		return (ASNObj)dat.decode(new Decoder(data));
	}
	public void setData(ASNObj dat) {
		data = dat.encode();
	}
	public static byte getASN1Type() {
		return TAG;
	}
	public static ASNPluginData[] getData(String peerID) {
		// TODO Auto-generated method stub
		return null;
	}
}